#!/bin/bash

# Google Tools.
export PATH=$PATH:${GOOGLE_TOOLS_HOME}:${GOOGLE_TOOLS_HOME}/bin

# Entity.
SCHEMA="${SCHEMA_NAME}"
TABLE="${TABLE_NAME}"

# Caminho do bucket.
STORAGE_QUEUE_PATH="gs://${GOOGLE_CLOUD_BUCKET}/${SCHEMA}/${TABLE}/rawfile/queue/"

# Arquivo de dados. 
DATA_FILE="${SCHEMA}_${TABLE}"

# Cria uma tabela no Redshift.
table_check()
{
	bq mk --project_id=${BIG_QUERY_PROJECT_ID} ${CUSTOM_SCHEMA}${SCHEMA_NAME}

	if [ "${#TIME_PARTITIONING_FIELD}" -gt "0" ] && [ "${#TIME_PARTITIONING_TYPE}" -gt "0" ] ; then
		echo "Preparing partitioned table by ${TIME_PARTITIONING_FIELD} of type ${TIME_PARTITIONING_TYPE}"	
		bq mk --table \
			--project_id=${BIG_QUERY_PROJECT_ID} \
			--time_partitioning_field ${TIME_PARTITIONING_FIELD} \
	  		--time_partitioning_type ${TIME_PARTITIONING_TYPE} \
	  		--schema ${METADATA_JSON_FILE} \
			 ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE}
			 
	elif [ "${#TIME_PARTITIONING_FIELD}" -gt "0" ] && [ "${#TIME_PARTITIONING_TYPE}" -gt "0" ] && [ "${#CLUSTER_COLUMNS}" -gt "0" ] ; then
		echo "Preparing partitioned table by ${TIME_PARTITIONING_FIELD} of type ${TIME_PARTITIONING_TYPE} clusterized by ${CLUSTER_COLUMNS}"	
		bq mk --table \
			--project_id=${BIG_QUERY_PROJECT_ID} \
			--time_partitioning_field ${TIME_PARTITIONING_FIELD} \
	  		--time_partitioning_type ${TIME_PARTITIONING_TYPE} \
	  		--clustering_fields ${CLUSTER_COLUMNS} \
	  		--schema ${METADATA_JSON_FILE} \
			 ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE}
			 
	elif [ "${#CLUSTER_COLUMNS}" -gt "0" ] ; then
		echo "Preparing clusterized table by ${CLUSTER_COLUMNS}"	
		bq mk --table \
			--project_id=${BIG_QUERY_PROJECT_ID} \
			--clustering_fields ${CLUSTER_COLUMNS} \
	  		--schema ${METADATA_JSON_FILE} \
			 ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE}
	else
		echo "Preparing table"
		bq mk --table \
			--project_id=${BIG_QUERY_PROJECT_ID} \
	  		--schema ${METADATA_JSON_FILE} \
			 ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE}
	fi
	
	bq show --project_id=${BIG_QUERY_PROJECT_ID} ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE}
	error_check
}

# Executa a carga full.
full_load()
{
	echo "Running full load!"
    cd ${RAWFILE_QUEUE_PATH}

	echo "Uploading files to ${STORAGE_QUEUE_PATH} from ${RAWFILE_QUEUE_PATH}"
	gsutil -q -m rm ${STORAGE_QUEUE_PATH}*
	gsutil -q -m cp ${RAWFILE_QUEUE_PATH}* ${STORAGE_QUEUE_PATH}
	error_check
	
	echo "Loading table ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE} from ${STORAGE_QUEUE_PATH}"
	bq rm --project_id=${BIG_QUERY_PROJECT_ID} -f -t ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE} 
	bq load --project_id=${BIG_QUERY_PROJECT_ID} --field_delimiter="${DELIMITER}" ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE} ${STORAGE_QUEUE_PATH}* ${METADATA_JSON_FILE}
	error_check	
	
	echo "Updating from ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE}"
	bq query --project_id=${BIG_QUERY_PROJECT_ID} --use_legacy_sql=false  << EOF
BEGIN TRANSACTION;	
	TRUNCATE TABLE ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE};		
	MERGE ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE} t USING ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE} s ON t.custom_primary_key = s.custom_primary_key WHEN NOT MATCHED THEN INSERT ROW;
	COMMIT TRANSACTION;	
EOF
	error_check	
	
    # Remove os arquivos temporários.
    if [ ${DEBUG} = 0 ] ; then
		clean_up
    fi
}

# Executa a carga delta.
delta_load()
{
	cd ${RAWFILE_QUEUE_PATH}
		
	echo "Uploading files to ${STORAGE_QUEUE_PATH} from ${RAWFILE_QUEUE_PATH}"
	gsutil -q -m rm ${STORAGE_QUEUE_PATH}*
	gsutil -q -m cp ${RAWFILE_QUEUE_PATH}* ${STORAGE_QUEUE_PATH}
	error_check
	
	echo "Loading table ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE} from ${STORAGE_QUEUE_PATH}"
	bq rm --project_id=${BIG_QUERY_PROJECT_ID} -f -t ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE} 
	bq load --project_id=${BIG_QUERY_PROJECT_ID} --field_delimiter="${DELIMITER}" ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE} ${STORAGE_QUEUE_PATH}* ${METADATA_JSON_FILE}
	error_check
	
	echo "Updating from ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE}"
	bq query --project_id=${BIG_QUERY_PROJECT_ID} --use_legacy_sql=false  << EOF
BEGIN TRANSACTION;	
	DELETE FROM ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE} t WHERE custom_primary_key IN (SELECT custom_primary_key FROM ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE});		
	MERGE ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE} t USING ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE} s ON t.custom_primary_key = s.custom_primary_key WHEN NOT MATCHED THEN INSERT ROW;
	COMMIT TRANSACTION;	
EOF
	error_check	

    # Remove os arquivos temporários.
    if [ ${DEBUG} = 0 ] ; then
		clean_up
    fi	
}

# Realiza a limpeza dos arquivos temporários.
clean_up()
{
	bq rm --project_id=${BIG_QUERY_PROJECT_ID} -f -t ${CUSTOM_SCHEMA}${SCHEMA_NAME}.tmp_${TABLE} 
	gsutil -q -m rm ${STORAGE_QUEUE_PATH}*

    if [ ${MODULE} != "file" ]; then
        echo "Removing temporary folder ${QUEUE_PATH}"
        cd ${GLOVE_TEMP}
        rm -rf ${QUEUE_PATH}
    fi
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
if [ ${IS_RECREATE} = 1 ]; then
	# Dropa a tabela para que possa ser recriada.
	echo "Removing table ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE}!"
	bq rm --project_id=${BIG_QUERY_PROJECT_ID} -f -t ${CUSTOM_SCHEMA}${SCHEMA_NAME}.${TABLE} 
fi

# Identifica a quantidade de arquivos a serem processados.
QUEUE_FILE_COUNT=`ls ${RAWFILE_QUEUE_PATH}*${DATA_FILE}* |wc -l`

# Identifica se o arquivo contém ao menos uma linha.
if [ ${QUEUE_FOLDER_SIZE} -lt 1000 ]; then
    QUEUE_FILE_COUNT=`cat ${RAWFILE_QUEUE_PATH}*${DATA_FILE}* |wc -l`
fi

# Executa o processo de carga e criação de entidades.
if [ ${QUEUE_FILE_COUNT} -gt 0 ]; then
	cd ${RAWFILE_QUEUE_PATH}

	echo "Work area:"
	echo "${RAWFILE_QUEUE_PATH}"
	
	table_check

	echo "Splitting csv file!"
	split -l ${PARTITION_LENGTH} -a 4 --numeric-suffixes=1 --additional-suffix=.csv ${RAWFILE_QUEUE_FILE} ${DATA_FILE}_
	error_check

	echo "Removing file ${RAWFILE_QUEUE_FILE}!"
	rm -f ${RAWFILE_QUEUE_FILE}
	error_check	

    if [ ${MODULE} == "query" ] || [ ${MODULE} == "file" ]; then
    	echo "Removing header from ${DATA_FILE}_0001.csv!"
    	tail -n +2 ${DATA_FILE}_0001.csv > ${DATA_FILE}_0001.tmp
		error_check

    	mv -f ${DATA_FILE}_0001.tmp ${DATA_FILE}_0001.csv;
		error_check
    fi

	echo "Compacting csv files!"
	for i in `ls *.csv`
	do
		pigz $i
		error_check
	done

    # Identifica o tipo de carga que será realizado.
	if [ ${MODULE} == "file" ] && [ ${FILE_OUTPUT_MODE} == "append" ]; then
		delta_load	
	elif [ ${MODULE} == "query" ] && [ "${#PARTITION_FIELD}" -gt "0" ]; then
		delta_load
	else
        if [ "${#DELTA_FIELD_IN_METADATA}" -gt "0" ]; then
            delta_load
        else
            full_load
        fi
    fi
else
	echo "Nothing to load from ${RAWFILE_QUEUE_PATH} :/"
fi
