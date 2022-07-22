#!/bin/bash

export PGPASSWORD=${REDSHIFT_JDBC_PASSWORD};

# Sprectrum variables from kettle.properties.
REDSHIFT_URL=${REDSHIFT_JDBC_HOST}
REDSHIFT_USER=${REDSHIFT_JDBC_USER_NAME}
REDSHIFT_PORT=${REDSHIFT_JDBC_PORT}
REDSHIFT_DATASET=${REDSHIFT_JDBC_DB}

# Entity.
SCHEMA="${SCHEMA_NAME}"
TABLE="${TABLE_NAME}"

# Hash.
ENTITY_HASH=`echo -n ${TABLE} | md5sum | awk '{print substr($1,1,6)}'`
SCHEMA_HASH=`echo -n ${SCHEMA} | md5sum | awk '{print substr($1,1,6)}'`

# Caminho do bucket.
STORAGE_QUEUE_PATH="s3://${STORAGE_BUCKET}/${SCHEMA_HASH}_${SCHEMA}/${ENTITY_HASH}_${TABLE}/rawfile/queue/"
STORAGE_DISASTER_RECOVERY_QUEUE_PATH="s3://${GLOVE_STORARE_BUCKET_DISASTER_RECOVERY}/disaster_recovery/${SCHEMA_HASH}_${SCHEMA}/${ENTITY_HASH}_${TABLE}/rawfile/queue/"
STORAGE_STAGING_QUEUE_PATH="s3://${GLOVE_STORARE_BUCKET_STAGING}/staging"
STORAGE_EXPORT_QUEUE_PATH="s3://${GLOVE_STORARE_BUCKET_STAGING}/export"

# Arquivo de dados.
DATA_FILE="${SCHEMA}_${TABLE}"

# Cria um schema.
schema_check(){
    if [ ${IS_SPECTRUM} = 1 ] ; then
        echo "Running database check at ${SCHEMA}"

        # Cria o schema.
        psql -h ${REDSHIFT_URL} -U ${REDSHIFT_USER} -w -d ${REDSHIFT_DATASET} -p ${REDSHIFT_PORT} << EOF
            CREATE EXTERNAL SCHEMA IF NOT EXISTS ${SCHEMA}
            FROM DATA CATALOG
            DATABASE '${SCHEMA}'
            IAM_ROLE '${SPECTRUM_ROLE}'
            CREATE EXTERNAL DATABASE IF NOT EXISTS;
EOF
        error_check
    else
    	echo "Running database check at ${SCHEMA}"

        # Cria o database.
        run_on_athena "CREATE DATABASE IF NOT EXISTS ${SCHEMA};"
    fi
}

# Cria uma tabela no spectrum.
# o parâmetro TABLE_STRATEGY pode ser PARTITIONED ou SIMPLE. 
table_check(){
    TABLE_STRATEGY=$1
    echo "Preparing ${TABLE_STRATEGY} table to store ${OUTPUT_FORMAT} files!"
    if [ ${IS_SPECTRUM} = 1 ] && [ ${HAS_ATHENA} = 0 ]; then
        # Verifica se a tabela existe.
        TABLE_EXISTS=`psql -h ${REDSHIFT_URL} -U ${REDSHIFT_USER} -w -d ${REDSHIFT_DATASET} -p ${REDSHIFT_PORT} -c "SELECT SUM(N) FROM ( SELECT DISTINCT 1 AS N FROM SVV_EXTERNAL_TABLES WHERE SCHEMANAME='${SCHEMA}' AND TABLENAME='${TABLE}' UNION ALL SELECT 0 AS N);" | sed '1,2d' | head -n 1`
        # Cria a tabela e o schema.
        if [ ${TABLE_EXISTS} -eq 0 ]; then
            schema_check
            # Identifica a estratégia para criação da tabela no Spectrum. 
            if [ ${TABLE_STRATEGY} == "partitioned" ]; then
                psql -h ${REDSHIFT_URL} -U ${REDSHIFT_USER} -w -d ${REDSHIFT_DATASET} -p ${REDSHIFT_PORT} << EOF
                    CREATE EXTERNAL TABLE "${SCHEMA}"."${TABLE}"
                    (  ${FIELD_NAME_AND_TYPE_LIST}  )
                    PARTITIONED BY ( PARTITION_VALUE INT )
                    STORED AS ${OUTPUT_FORMAT}
                    LOCATION '${STORAGE_QUEUE_PATH}';
EOF
            else
                psql -h ${REDSHIFT_URL} -U ${REDSHIFT_USER} -w -d ${REDSHIFT_DATASET} -p ${REDSHIFT_PORT} << EOF
                    CREATE EXTERNAL TABLE "${SCHEMA}"."${TABLE}"
                    (${FIELD_NAME_AND_TYPE_LIST})
                    STORED AS ${OUTPUT_FORMAT}
                    LOCATION '${STORAGE_QUEUE_PATH}';
EOF
            fi
        fi
        error_check
    else
        # Cria o database.
        schema_check
        # Cria a tabela.
        if [ ${TABLE_STRATEGY} -eq "partitioned" ]; then
            run_on_athena "CREATE EXTERNAL TABLE IF NOT EXISTS ${SCHEMA}.${TABLE} (${FIELD_NAME_AND_TYPE_LIST}) PARTITIONED BY ( partition_value int ) STORED AS ${OUTPUT_FORMAT} LOCATION '${STORAGE_QUEUE_PATH}';"
        else
            run_on_athena "CREATE EXTERNAL TABLE IF NOT EXISTS ${SCHEMA}.${TABLE} (${FIELD_NAME_AND_TYPE_LIST}) STORED AS ${OUTPUT_FORMAT} LOCATION '${STORAGE_QUEUE_PATH}';"
        fi
    fi
}

# Executa carga particionada.
partition_load(){
	echo "Running partition load!"
	cd ${RAWFILE_QUEUE_PATH}

    if [ ${MODULE} == "query" ] || [ ${MODULE} == "file" ]; then
		split_file "*.csv" ${THREAD} "true" "false"
    else
		split_file "*.csv" ${THREAD} "false" "false"
    fi

	# Identifica se será realizado merge.
	if [ ${PARTITION_MERGE} -gt 0 ]; then
		echo "PARTITION_MERGE ACTIVED!"
    else
        echo "PARTITION_MERGE DISABLED!"
	fi

    # Converte os arquivos das partições para formato colunar.
	convert "*" "1" ${PARTITION_MERGE} ${STORAGE_QUEUE_PATH} ${PARTITION_MODE} "false"

	# Remove os arquivos utilizados no merge.
	rm -f *.original.${OUTPUT_FORMAT}
	rm -f *.transient.${OUTPUT_FORMAT}

    # Identifica se o particionamento é real ou virtual.
    if [ ${PARTITION_MODE} == "real" ]; then
        # Cria o diretório para cada partição respeitando o padrão de particionamento do HIVE.
        for i in `ls *.${OUTPUT_FORMAT}`
        do
            PARTITION=`echo $i | cut -d '.' -f 1`
            mkdir -p ${RAWFILE_QUEUE_PATH}partition_value=${PARTITION}
            rm -f ${RAWFILE_QUEUE_PATH}partition_value=${PARTITION}/*
            mv -f $i ${RAWFILE_QUEUE_PATH}partition_value=${PARTITION}
            error_check
        done
    fi

	# Envia os arquivo para o storage.
	echo "Uploading ${OUTPUT_FORMAT} files!"
	aws s3 cp ${RAWFILE_QUEUE_PATH} ${STORAGE_QUEUE_PATH} --recursive --exclude "*" --include "*.${OUTPUT_FORMAT}" --only-show-errors
	error_check

    # Adiciona as partições na tabela.
	if [ ${PARTITION_MODE} == "real" ]; then
        # Cria o diretório para cada partição respeitando o padrão de particionamento do HIVE.
        for i in `ls`
        do
            PARTITION=`echo $i | cut -d '=' -f 2`

			echo "Adding partition ${PARTITION} to table ${SCHEMA}.${TABLE}!"
            run_on_athena "ALTER TABLE ${SCHEMA}.${TABLE} ADD IF NOT EXISTS PARTITION ( partition_value = '${PARTITION}');"
        done
	fi

    # Remove os arquivos temporários.
    if [ ${DEBUG} = 0 ] ; then
        clean_up
    fi
}

# Executa a carga delta.
delta_load(){
	echo "Running delta load!"
	cd ${RAWFILE_QUEUE_PATH}

    # Identifica se o arquivo existe no storage.
	FILE_EXISTS=`aws s3 ls ${STORAGE_QUEUE_PATH} | grep ${DATA_FILE}. | wc -l`

    if [ ${FILE_EXISTS} -gt 0 ]; then
        # Recupera o arquivo de dados.
        echo "Getting hot file!"
		aws s3 cp ${STORAGE_QUEUE_PATH} ${RAWFILE_QUEUE_PATH} --recursive --exclude "*" --include "${DATA_FILE}.*" --only-show-errors
        error_check
    fi

    # Converte o arquivo final para o formato colunar.
	convert "${DATA_FILE}.csv" "0" ${PARTITION_MERGE} ${STORAGE_QUEUE_PATH} "" "false"

    # Remove os arquivos antigos.
    aws s3 rm ${STORAGE_QUEUE_PATH} --recursive --exclude "*" --include "${DATA_FILE}.*" --only-show-errors

    # Remove os arquivos utilizados no merge.
    rm -f *.original.${OUTPUT_FORMAT}

    # Envia o arquivo para o storage.
    echo "Uploading ${OUTPUT_FORMAT} files to S3 "
    aws s3 cp ${RAWFILE_QUEUE_PATH} ${STORAGE_QUEUE_PATH} --recursive --exclude "*" --include "*.${OUTPUT_FORMAT}" --only-show-errors
    error_check

    # Remove os arquivos temporários.
    if [ ${DEBUG} = 0 ] ; then
        clean_up
    fi
}

# Executa carga full.
full_load(){
	echo "Running full load!"
  	cd ${RAWFILE_QUEUE_PATH}

	# Identifica se o arquivo pode ser quebrado.
	SPLIT=1

	# Identifica se o APPEND está habilitado no módulo de arquivo.
	if [ ${MODULE} == "file" ] && [ ${FILE_OUTPUT_MODE} == "append" ]; then
		SPLIT=0
	fi

	if [ ${SPLIT} == 1 ]; then
		# Quebra o arquivo original em blocos menores.
		echo "Splitting csv file!"
		split -l ${PARTITION_LENGTH} -a 4 --numeric-suffixes=1 --additional-suffix=.csv ${RAWFILE_QUEUE_FILE} ${DATA_FILE}_
		error_check

		# Remove o arquivo original.
		echo "Removing file ${RAWFILE_QUEUE_FILE}!"
		rm -f ${RAWFILE_QUEUE_FILE}
		error_check
	fi

	if [ ${MODULE} == "file" ]; then
		# Converte o arquivo de dados para formato colunar.
		convert "*.csv" "-1" "0" "" "" "true"

		if [ ${FILE_OUTPUT_MODE} == "append" ]; then
			echo "APPEND mode ON!"
		else
			# Remove os arquivos antigos do storage.
			echo "Removing files from ${STORAGE_QUEUE_PATH}"
			aws s3 rm ${STORAGE_QUEUE_PATH} --recursive --only-show-errors
		fi
	else
		# Converte o arquivo de dados para formato colunar.
		convert "*.csv" "-1" "0" "" "" "false"

		# Remove os arquivos antigo do storage.
		echo "Removing files from ${STORAGE_QUEUE_PATH}"
		aws s3 rm ${STORAGE_QUEUE_PATH} --recursive --only-show-errors
	fi

    # Envia o arquivo para o storage.
    echo "Uploading ${OUTPUT_FORMAT} files to S3 "
    aws s3 cp ${RAWFILE_QUEUE_PATH} ${STORAGE_QUEUE_PATH} --recursive --exclude "*" --include "*.${OUTPUT_FORMAT}" --only-show-errors
    error_check

    # Remove os arquivos temporários.
    if [ ${DEBUG} = 0 ] ; then
        clean_up
    fi
}

#Executa uma query no Athena.
run_on_athena()
{
    QUERY=$1

    if [ ${DEBUG} = 1 ] ; then
        echo ${QUERY}
    fi

    # Cria o database.
    ATHENA_QUERY_ID=`aws athena start-query-execution \
            --query-string "${QUERY}" \
            --output text \
            --result-configuration OutputLocation=${STORAGE_STAGING_QUEUE_PATH}`

    # Identifica o status de execução.
    while true
    do
        ATHENA_QUERY_STATUS=`aws athena get-query-execution --query-execution-id $ATHENA_QUERY_ID | jq '.QueryExecution.Status.State' | sed 's/\"//g'`
        error_check

        if [[ "$ATHENA_QUERY_STATUS" == "SUCCEEDED" ]]; then
            break
        elif [[ "$ATHENA_QUERY_STATUS" == "FAILED" || "$ATHENA_QUERY_STATUS" == "CANCELLED" ]]; then
            echo "Fail running ${QUERY} :/"
            exit 1
        else
            # Verifica o status da instrução enquanto diferente de SUCCEEDED, FAILED ou CANCELLED.
            sleep 1
        fi
    done

	# Remove os arquivos antigos.
	if [ ${DEBUG} = 0 ] ; then
		echo "Removing staging files of ${STORAGE_STAGING_QUEUE_PATH}/${ATHENA_QUERY_ID}*"
		aws s3 rm ${STORAGE_STAGING_QUEUE_PATH} --recursive --exclude "*" --include "*${ATHENA_QUERY_ID}*" --only-show-errors
	fi
}

# Realiza a limpeza dos arquivos temporários.
clean_up()
{
    if [ ${MODULE} != "file" ]; then
        echo "Removing temporary folder ${QUEUE_PATH}"
        cd ${GLOVE_TEMP}
        rm -rf ${QUEUE_PATH}
    fi
}

# Realiza o backup dos arquivos brutos.
backup()
{
    #Identifica a data e hora do backup.
    YEAR=`date '+%Y'`
    MONTH=`date '+%m'`
    DAY=`date '+%d'`
    HOUR=`date '+%H'`
    MINUTE=`date '+%M'`

    #Identifica o bucket que receberá os dados do backup.
    STORAGE_BACKUP_METADATA_PATH="s3://${STORAGE_BUCKET_BACKUP}/backup/${SCHEMA}/${TABLE}/metadata/year=${YEAR}/month=${MONTH}/day=${DAY}/hour=${HOUR}/minute=${MINUTE}/"
    STORAGE_BACKUP_QUEUE_PATH="s3://${STORAGE_BUCKET_BACKUP}/backup/${SCHEMA}/${TABLE}/rawfile/year=${YEAR}/month=${MONTH}/day=${DAY}/hour=${HOUR}/minute=${MINUTE}/"

    # Envia o arquivo do metadado para o storage.
    echo "Backing up metadata files to ${STORAGE_BACKUP_METADATA_PATH}"
    aws s3 cp ${METADATA_PATH} ${STORAGE_BACKUP_METADATA_PATH} --recursive --only-show-errors
    error_check

    # Envia o arquivo de dados para o storage.
    echo "Backing up data files to ${STORAGE_BACKUP_QUEUE_PATH}"
    cd ${RAWFILE_QUEUE_PATH}
    pigz -c ${RAWFILE_QUEUE_FILE} > ${RAWFILE_QUEUE_FILE}.gz
    aws s3 cp ${RAWFILE_QUEUE_PATH} ${STORAGE_BACKUP_QUEUE_PATH} --recursive --exclude "*" --include "*.gz" --only-show-errors
    rm -f ${RAWFILE_QUEUE_FILE}.gz
    error_check
}

#Send e-mail for valid recipients.
send_email()
{
    # Transforma string de destinatários em um vetor.
    RECIPIENTS=(`echo ${EXPORT_SHEETS_RECIPIENTS} | cut -d ","  --output-delimiter=" " -f 1-`)

    # Lista de e-mails válidos.
    ALLOWED_EMAILS=""

    # Varre os destinatários.
    for RECIPIENT in ${RECIPIENTS[@]}; do 
        IS_ALLOWED=true

        # Identifica se existe lista de e-mails válidos.
        if [ "${#GLOVE_EXPORT_EMAIL_WHITELIST}" -gt "0" ]; then
            IS_ALLOWED=false

            # Varre lista de e-mails/domínios válidos.
            for EMAIL_PATTERN in $(cat ${GLOVE_EXPORT_EMAIL_WHITELIST}); do

                # Identifica se destinatário é válido.
                if [[ $RECIPIENT =~ $EMAIL_PATTERN ]] ; then
                    IS_ALLOWED=true
                    break
                fi
            done
        fi

        # Adiciona destinatários permitidos na lista de envio de e-mails.
        if $IS_ALLOWED ; then
            ALLOWED_EMAILS+="$RECIPIENT,"
        else
            echo "Recipient ${RECIPIENT} is not allowed to receive this export."
        fi
    done
    
	echo "Sending e-mail with presign link to recipients: ${ALLOWED_EMAILS::-1}"					

	echo $1 | mutt -s "${EXPORT_SHEETS_SUBJECT}" -b ${ALLOWED_EMAILS::-1}
}

# Efetua a conversão dos arquivos de dados para formato colunares (Orc ou parquet)
convert()
{
	FILENAME=$1 # File name or Wildcard
	FIELDKEY=$2 # Field key
	IS_MERGE=$3 # Identify if it is a merge
	BUCKET=$4 # Bucket with file to merge
	MODE=$5 # Identify partition mode
	HEADER=$6 # Identify if the file has header

	echo "Generating ${OUTPUT_FORMAT} files!"
	if [ ${DEBUG} = 1 ] ; then
	 	echo "DEBUG:java -jar ${GLOVE_HOME}/extractor/lib/${OUTPUT_FORMAT}.jar \
			--folder=${RAWFILE_QUEUE_PATH} \
			--filename=${FILENAME} \
			--delimiter=${DELIMITER} \
			--schema=${METADATA_JSON_FILE} \
			--compression=${OUTPUT_COMPRESSION} \
			--thread=${THREAD} \
			--duplicated=${ALLOW_DUPLICATED} \
			--fieldkey=${FIELDKEY} \
			--merge=${IS_MERGE} \
			--bucket=${BUCKET} \
			--mode=${MODE} \
			--escape=${QUOTE_ESCAPE} \
			--replace='true' \
			--header=${HEADER} \
			--debug=${DEBUG}"
	fi

	java -jar ${GLOVE_HOME}/extractor/lib/${OUTPUT_FORMAT}.jar \
		--folder=${RAWFILE_QUEUE_PATH} \
		--filename=${FILENAME} \
		--delimiter=${DELIMITER} \
		--schema=${METADATA_JSON_FILE} \
		--compression=${OUTPUT_COMPRESSION} \
		--thread=${THREAD} \
		--duplicated=${ALLOW_DUPLICATED} \
		--fieldkey=${FIELDKEY} \
		--merge=${IS_MERGE} \
		--bucket=${BUCKET} \
		--mode=${MODE} \
		--escape=${QUOTE_ESCAPE} \
		--replace='true' \
		--header=${HEADER} \
		--debug=${DEBUG}
	error_check
}

#Particiona o arquivo contendo os dados.
split_file()
{
	FILENAME=$1 # File name or Wildcard
	THREADS=$2 # Use how many threads.
	HEADER=$3 # Identify if the file has header
	READABLE=$4 # Identify if it is a merge

	echo "Partitioning data file delimited by ${DELIMITER}!"
	if [ ${DEBUG} = 1 ] ; then
		echo "DEBUG:java -jar ${GLOVE_HOME}/extractor/lib/splitter.jar \
			--folder=${RAWFILE_QUEUE_PATH} \
			--filename=${FILENAME} \
			--delimiter=${DELIMITER} \
			--splitStrategy=${SPLIT_STRATEGY} \
			--thread=${THREADS} \
			--escape=${QUOTE_ESCAPE} \
			--header=${HEADER} \
			--replace='true'
			--readable=${READABLE}"
	fi

	java -jar ${GLOVE_HOME}/extractor/lib/splitter.jar \
		--folder=${RAWFILE_QUEUE_PATH} \
		--filename=${FILENAME} \
		--delimiter=${DELIMITER} \
		--splitStrategy=${SPLIT_STRATEGY} \
		--thread=${THREADS} \
		--escape=${QUOTE_ESCAPE} \
		--header=${HEADER} \
		--replace='true' \
		--readable=${READABLE}
	error_check
}

# Identifica a ocorrência de erros e interrompe processo.
error_check()
{
	if [ $? -gt 0 ]; then
        echo "An error has occurred :_("

        # Remove os arquivos temporários.
        if [ ${DEBUG} = 0 ] ; then
            clean_up
        fi

		exit 1
	fi
}

# Identifica o tamanho do diretório de trabalho.
QUEUE_FOLDER_SIZE=`du -s ${RAWFILE_QUEUE_PATH} | cut -f1`

# Limita o volume de dados.
if [ ${QUEUE_FOLDER_SIZE} -gt ${QUEUE_FILES_SIZE_LIMIT} ]; then
	echo "You are trying processing ${QUEUE_FOLDER_SIZE} KB :0. Please, reduce the amount of data!"

	# Remove os arquivos temporários.
	if [ ${DEBUG} = 0 ] ; then
		clean_up
	fi

	exit 1
else
    echo "${QUEUE_FOLDER_SIZE} KB of data will be processed ( ${QUEUE_FILES_SIZE_LIMIT} KB is the limit )!"
fi

# Identifica se deve recriar a tabela.
if [ ${IS_RECREATE} = 1 -o ${IS_RELOAD} = 1 ]; then

    DATE=`date '+%Y%m%d%H%M%S'`

	# Cria o arquivo de recuperação a partir dos arquivos do processo.
	if [ ${IS_RECREATE} = 1 ]; then

		# Identifica se deve manter os arquivos caso houver mudanças no metadado.
		if [ ${IS_SCHEMA_EVOLUTION} = 1 ]; then
			echo "IS_RECREATE ACTIVED with SCHEMA_EVOLUTION!"
			echo "Copying files to recovery folder ${STORAGE_DISASTER_RECOVERY_QUEUE_PATH}!"
			aws s3 cp ${STORAGE_QUEUE_PATH} ${STORAGE_DISASTER_RECOVERY_QUEUE_PATH}${DATE}/ --recursive --only-show-errors
		else
			echo "IS_RECREATE ACTIVED!"			
			echo "Moving files to recovery folder ${STORAGE_DISASTER_RECOVERY_QUEUE_PATH}!"
			aws s3 mv ${STORAGE_QUEUE_PATH} ${STORAGE_DISASTER_RECOVERY_QUEUE_PATH}${DATE}/ --recursive --only-show-errors
		fi
	fi

	if [ ${IS_RELOAD} = 0 ]; then
		# Dropa a tabela para que possa ser recriada.
        if [ ${IS_SPECTRUM} = 1 ] && [ ${HAS_ATHENA} = 0 ]; then
            echo "Dropping table ${SCHEMA}.${TABLE} from Spectrum!"
            psql -h ${REDSHIFT_URL} -U ${REDSHIFT_USER} -w -d ${REDSHIFT_DATASET} -p ${REDSHIFT_PORT} << EOF
			     drop table "${SCHEMA}"."${TABLE}";
EOF
        else
            echo "Dropping table ${SCHEMA}.${TABLE} from Athena!"
            run_on_athena "DROP TABLE IF EXISTS ${SCHEMA}.${TABLE};"
        fi

		# Realiza a verificação da estrutura das tabelas.
		if [ "${#PARTITION_FIELD}" -gt "0" ] || [ ${FILE_INPUT_PARTITONED} -eq 1 ]; then
			if [ ${PARTITION_MODE} == "real" ]; then
				table_check "partitioned"
			else
				table_check "simple"
			fi
		else
			table_check "simple"
		fi
	else
		echo "IS_RELOAD ACTIVED!"
		echo "Removing files from ${STORAGE_QUEUE_PATH}"
		aws s3 rm ${STORAGE_QUEUE_PATH} --recursive --only-show-errors
	fi
fi

# Identifica a quantidade de arquivos a serem processados.
QUEUE_FILE_COUNT=`ls ${RAWFILE_QUEUE_PATH}*${DATA_FILE}* |wc -l`

# Identifica se o arquivo contém ao menos uma linha.
if [ ${QUEUE_FOLDER_SIZE} -lt 1000 ]; then
    QUEUE_FILE_COUNT=`cat ${RAWFILE_QUEUE_PATH}*${DATA_FILE}* |wc -l`
fi

# Executa o processo de carga e criação de entidades.
if [ ${QUEUE_FILE_COUNT} -gt 0 ]; then
	echo "Work area:"
	echo "${RAWFILE_QUEUE_PATH}"
	echo "${STORAGE_QUEUE_PATH}"

    if [ ${IS_SPECTRUM} = 1 ] ; then
        echo "Credentials:"
        echo "URL ${REDSHIFT_URL}"
        echo "User ${REDSHIFT_USER}"
        echo "Dataset ${REDSHIFT_DATASET}"
        echo "Port ${REDSHIFT_PORT}"
        echo "Password provided by kettle.properties parameter"
    fi

    # Identifica se será feito backup dos arquivos de dados.
    if [ "${#STORAGE_BUCKET_BACKUP}" -gt "0" ]; then
        backup
    fi

    # Identifica se é uma named query.
    if [ ${MODULE} == "query" ]; then

		# Identifica se deve exportar o resultset intermediário.
		if [ ${IS_EXPORT} = 1 ]; then

			# Define o storage de exportação.
			if [ "${#EXPORT_BUCKET}" -gt "0" ]; then
				# Particiona o arquivo de entrada. 
				if [ "${#PARTITION_FIELD}" -gt "0" ]; then
					FILE_INDEX=0

					echo "Merging files!"

					# Une os dados em um único arquivo.  
					for i in `ls ${RAWFILE_QUEUE_PATH}*`
					do
						if [ ${FILE_INDEX} = 0 ]; then	
							cat ${i}	>> ${RAWFILE_QUEUE_PATH}merged.csv
							error_check
						else
							sed '1d' ${i} >> ${RAWFILE_QUEUE_PATH}merged.csv
							error_check	
						fi

						FILE_INDEX=$(( $FILE_INDEX + 1 ))
					done

					echo "Partitioning data file delimited by ${DELIMITER}!"

					# Particiona o arquivo em single thread (thread=1) para preservar os dados e nome das partições.  
					# TODO - A geração de um único arquivo de saída deve ser suportada pelo conversor de dados nativamente sem a necessidade do merge anterior. 
					split_file "merged.csv" "1" "true" "true"
				fi

				# Identifica cada bucket para o qual o export deve ser enviado.
				BUCKETS=(`echo ${EXPORT_BUCKET} | tr -d ' ' | tr ',' ' '`)

				# Identifica se deve compactar o arquivo a ser exportado.
				if [ ${EXPORT_TYPE} == "gz" ]  || [ ${EXPORT_TYPE} == "zip" ]; then
					echo "Compacting files at ${RAWFILE_QUEUE_PATH} to ${EXPORT_TYPE}!"

					if [ ${EXPORT_TYPE} == "gz" ]; then
						pigz -k ${RAWFILE_QUEUE_PATH}*
					else
						find ${RAWFILE_QUEUE_PATH} -type f -not -name "${DATA_FILE}*" -execdir zip '{}.zip' '{}' \;
					fi 	

					for index in "${!BUCKETS[@]}"
					do
						echo "Exporting resultset to ${BUCKETS[index]} using profile ${EXPORT_PROFILE}!"

						if [ "${#PARTITION_FIELD}" -gt "0" ]; then
							aws s3 cp ${RAWFILE_QUEUE_PATH} ${BUCKETS[index]} --profile ${EXPORT_PROFILE} --recursive --exclude "${DATA_FILE}*" --exclude "*.csv" --only-show-errors --acl bucket-owner-full-control
						else
							aws s3 cp ${RAWFILE_QUEUE_FILE}.${EXPORT_TYPE} ${BUCKETS[index]} --profile ${EXPORT_PROFILE} --only-show-errors --acl bucket-owner-full-control
						fi
						error_check
					done
				else
					for index in "${!BUCKETS[@]}"
					do
						echo "Exporting resultset to ${BUCKETS[index]} using profile ${EXPORT_PROFILE}!"
						
						if [ "${#PARTITION_FIELD}" -gt "0" ]; then
							aws s3 cp ${RAWFILE_QUEUE_PATH} ${BUCKETS[index]} --profile ${EXPORT_PROFILE} --recursive --exclude "${DATA_FILE}*" --only-show-errors --acl bucket-owner-full-control
						else
							aws s3 cp ${RAWFILE_QUEUE_FILE} ${BUCKETS[index]} --profile ${EXPORT_PROFILE} --only-show-errors --acl bucket-owner-full-control
						fi
						error_check
					done
				fi

				# Remove os arquivos temporários. 	
				find ${RAWFILE_QUEUE_PATH} -not -name "${DATA_FILE}*.csv" -delete
			elif [ "${#EXPORT_SPREADSHEET}" -gt "0" ]; then
				if [ ${DEBUG} = 1 ] ; then
					echo "DEBUG:java -jar ${GLOVE_HOME}/extractor/lib/google-sheets-export.jar \
					--credentials=GLOVE_SPREADSHEET_CREDENTIALS \
					--spreadsheet=${EXPORT_SPREADSHEET} \
					--input=${RAWFILE_QUEUE_FILE} \
					--sheet=${EXPORT_SHEET} \
					--method=${EXPORT_SHEETS_METHOD} \
					--debug=${DEBUG} \
					--delimiter=${DELIMITER}"
				fi

				# Exporta resultset para uma planilha do Google Sheets.
				java -jar ${GLOVE_HOME}/extractor/lib/google-sheets-export.jar \
					--credentials=${GLOVE_SPREADSHEET_CREDENTIALS} \
					--spreadsheet=${EXPORT_SPREADSHEET} \
					--input=${RAWFILE_QUEUE_FILE} \
					--sheet=${EXPORT_SHEET} \
					--method=${EXPORT_SHEETS_METHOD} \
					--debug=${DEBUG} \
					--delimiter=${DELIMITER}
				error_check
				
				# Identifica se deve exportar a spreadsheet para xls.
				if [ "${#EXPORT_SHEETS_RECIPIENTS}" -gt "0" ] && [ "${#EXPORT_SHEETS_SUBJECT}" -gt "0" ]; then

					# Data e hora atual.
					NOW=`date '+%Y%m%d%H%M%S'`
					
					# Local para armazenar arquivo de exportação.
					RAWFILE_QUEUE_PATH_EXPORT="${RAWFILE_QUEUE_PATH}export"
					
					# Nome do arquivo de exportação.
					EXPORT_FILE_NAME="${EXPORT_SPREADSHEET}.xls"
					
					# Local temporário no S3 onde o arquivo será colocado.
					STORAGE_EXPORT_FILE_PATH="${STORAGE_EXPORT_QUEUE_PATH}/${EXPORT_SPREADSHEET}/${NOW}/"

					# Cria diretório temporário de exportação.
					mkdir ${RAWFILE_QUEUE_PATH_EXPORT}

					if [ ${DEBUG} = 1 ] ; then
						echo "DEBUG:java -jar ${GLOVE_HOME}/extractor/lib/google-drive-manager.jar \
						--credentials=${GLOVE_GOOGLE_DRIVE_CREDENTIALS} \
						--action='export' \
						--id=${EXPORT_SPREADSHEET} \
						--output=${RAWFILE_QUEUE_PATH_EXPORT}/${EXPORT_FILE_NAME}"
					fi

					# Exporta a spreadsheet para formato xls.
					java -jar ${GLOVE_HOME}/extractor/lib/google-drive-manager.jar \
						--credentials=${GLOVE_GOOGLE_DRIVE_CREDENTIALS} \
						--action='export' \
						--id=${EXPORT_SPREADSHEET} \
						--output=${RAWFILE_QUEUE_PATH_EXPORT}/${EXPORT_FILE_NAME}
					error_check

					# Sobe o arquivo exportado para o S3.
					aws s3 cp ${RAWFILE_QUEUE_PATH_EXPORT}/${EXPORT_FILE_NAME} ${STORAGE_EXPORT_FILE_PATH}
					
					# Gera link presign a ser enviado por e-mail.
					LINK_PRESIGN=`aws s3 presign ${STORAGE_EXPORT_FILE_PATH}${EXPORT_FILE_NAME} --expires-in 604800`

					echo 'Presign link expires in 604800 seconds:'
					echo $LINK_PRESIGN	

					send_email "Your file is available for download for 7 days: ${LINK_PRESIGN}. Generated by Glove, a modular data integration platform."
				fi				
			else
				echo "EXPORT_BUCKET_DEFAULT or EXPORT_SPREADSHEET_DEFAULT was not defined!"
			fi

			# Finaliza o processo de exportação.
			if [ ${ONLY_EXPORT} = 1 ]; then
				echo "Exporting finished!"
				exit 0
			fi
		fi
    fi

	# Identifica se o schema e tabela existem. 
	if [ "${#PARTITION_FIELD}" -gt "0" ]; then
		table_check "partitioned"
	else
		table_check "simple"
	fi

    # Identifica se a fonte é arquivo.
    if [ ${MODULE} == "file" ]; then
		# Identifica o tipo de carga que será realizado.
		if [ ${FILE_INPUT_PARTITONED} == 1 ]; then
			partition_load
		else
			full_load
		fi
    else
	    # Identifica o tipo de carga que será realizado.
	    if [ "${#PARTITION_FIELD}" -gt "0" ]; then
		    partition_load
	    else
            if [ "${#DELTA_FIELD_IN_METADATA}" -gt "0" ]; then
			    delta_load
		    else
                # Remove o header do csv intermediário.
                if [ ${MODULE} == "query" ]; then
                    echo "Removing header from ${RAWFILE_QUEUE_FILE}!"
                    tail -n +2 ${RAWFILE_QUEUE_FILE} > ${RAWFILE_QUEUE_FILE}.tmp
                    mv -f ${RAWFILE_QUEUE_FILE}.tmp ${RAWFILE_QUEUE_FILE};
                fi

			    full_load
		    fi
	    fi
    fi

	# Identifica se deve recarregar as partições da tabela.
	if [ ${IS_RECREATE} = 1 ] && [ ${IS_SCHEMA_EVOLUTION} = 1 ] && [ "${#PARTITION_FIELD}" -gt "0" ] && [ ${PARTITION_MODE} == "real" ]; then
		echo "Repairing table partitions"
		run_on_athena "MSCK REPAIR TABLE ${SCHEMA}.${TABLE};"
	fi

	# Identifica a quantidade de registros na tabela.
    ATHENA_QUERY_ID=`aws athena start-query-execution --query-string "select count(1) from ${SCHEMA}.${TABLE};" --output text --result-configuration OutputLocation=${STORAGE_STAGING_QUEUE_PATH}`
    error_check

    while true
    do
        # Identifica o status da query.
        ATHENA_QUERY_STATUS=`aws athena get-query-execution --query-execution-id $ATHENA_QUERY_ID | jq '.QueryExecution.Status.State' | sed 's/\"//g'`
        error_check

        if [[ "$ATHENA_QUERY_STATUS" == "SUCCEEDED" ]]; then
            ROW_COUNT=`aws athena get-query-results --query-execution-id=$ATHENA_QUERY_ID --output=text --query=ResultSet.Rows | sed '1d'| grep -E '[0-9]' | tr -d '[:alpha:][:blank:]'`
            error_check

            # Atualiza as estatísticas da entidade.
            if [ ${IS_SPECTRUM} = 1 ] ; then
                if [ ${ROW_COUNT} -gt 0 ]; then
                    echo "Updating table properties: ${ROW_COUNT} records!"
                    psql -h ${REDSHIFT_URL} -U ${REDSHIFT_USER} -w -d ${REDSHIFT_DATASET} -p ${REDSHIFT_PORT} << EOF
                      ALTER TABLE "${SCHEMA}"."${TABLE}" SET TABLE PROPERTIES ('numRows'='${ROW_COUNT}');
EOF
                fi

                echo "Removing old table versions from glue!"
                TABLE_VERSION=`aws glue get-table-versions --max-items=100 --database-name ${SCHEMA} --table-name ${TABLE} | jq --compact-output "[.TableVersions[].VersionId]" | sed -e 's/\,/ /g;s/\[/ /g;s/\]/ /g;s/\"//g'`
                aws glue batch-delete-table-version --database-name ${SCHEMA} --table-name ${TABLE} --version-ids $TABLE_VERSION
            else
                echo "${ROW_COUNT} records in the table \"${SCHEMA}\".\"${TABLE}\""
            fi

			# Remove os arquivos antigos.
			if [ ${DEBUG} = 0 ] ; then
				echo "Removing staging files of ${STORAGE_STAGING_QUEUE_PATH}/${ATHENA_QUERY_ID}*"
				aws s3 rm ${STORAGE_STAGING_QUEUE_PATH} --recursive --exclude "*" --include "*${ATHENA_QUERY_ID}*" --only-show-errors
			fi

            break
        elif [[ "$ATHENA_QUERY_STATUS" == "FAILED" || "$ATHENA_QUERY_STATUS" == "CANCELLED" ]]; then
            echo "Table properties not updated :/"
            break
        else
            sleep 5
        fi
    done
else
	echo "Nothing to load from ${RAWFILE_QUEUE_PATH} :/"
fi
