

# Glove [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Plataforma modular para automatização de processos de integração de dados

## Instalação

##### REQUISITOS

- Linux
- Open JDK 8
- Git 
- Tomcat 8 +
- Google Cloud SDK
- AWS Cli
- Jenkins ( [https://jenkins.io/](https://jenkins.io/) )
- Pentaho Data Integration
- Parallel ( [https://www.gnu.org/software/parallel/](https://www.gnu.org/software/parallel/) )
- jq ([https://stedolan.github.io/jq/](https://stedolan.github.io/jq/))
- pigz ( [https://zlib.net/pigz/](https://zlib.net/pigz/) )

##### CONFIGURAÇÃO

- Clone o projeto do Glove.
- Crie o arquivo ~/.kettle/kettle.properties, com o seguinte conteúdo:
 
```
# Core
GLOVE_HOME=<Glove home directory>
GLOVE_TEMP=<Temp files directory>
GLOVE_METADATA=<Glove metadata directory>
GLOVE_NAMED_QUERIES=<.sql files>
GLOVE_CLEANUP_HOUR=<Hora do dia, no formato HH que os arquivos temporários serão analisados e removidos>
  
# Redshift
REDSHIFT_JDBC_HOST=<Host>
REDSHIFT_JDBC_PORT=<Port>
REDSHIFT_JDBC_DB=<Database>
REDSHIFT_JDBC_USER_NAME=<User>
REDSHIFT_JDBC_PASSWORD=<Password>
 
# Redshift Spectrum
SPECTRUM_ROLE=<Role>

# Redshift Unload and Copy credentials. 
REDSHIFT_UNLOAD_COPY_AUTHENTICATION=<CREDENTIALS\ \'aws_access_key_id=XXX;aws_secret_access_key=YYY\' ou IAM_ROLE\ \'arn:aws:iam::XXX:role/YYY\' >
 
# Google Cloud SDK
GOOGLE_TOOLS_HOME=<Google Cloud SDK path>
 
# Sisense
GLOVE_SISENSE_URL=<Sisense URL>
GLOVE_SISENSE_TOKEN=<Sisense API Token>    

# Support buckets
GLOVE_STORARE_BUCKET_STAGING=<S3 bucket para dados temporários>
GLOVE_STORARE_BUCKET_DISASTER_RECOVERY=<S3 bucket para disaster recovery>
```

Para configuração do Spectrum Role, consulte esta documentação:  [Utilização do Amazon Redshift Spectrum para consultar dados externos](https://docs.aws.amazon.com/pt_br/redshift/latest/dg/c-using-spectrum.html)   

- Crie o arquivo ~/.kettle/connection.properties, inserindo o cabeçalho e uma nova linha para cada conexão:  

    - **CONNECTION_NAME**: Nome da conexão. 
    - **JDBC_URL**: JDBC URL da conexão.
    - **JDBC_DRIVER_CLASS**: Classe do driver de conexão.
    - **DB_HOST**: Endereço do banco de dados .
    - **DB_DATABASE**: Database.
    - **DB_USER**: Usuário do banco de dados.
    - **DB_PASSWORD**: Senha do usuário do banco de dados. 
    - **DATABASE_TYPE**: Tipo do banco de dados, sendo suportado os seguintes valores: **REDSHIFT, ATHENA, MYSQL, POSTGRES **e** SAP_HANA**. 

##### TECNOLOGIAS SUPORTADAS

As fontes de dados suportadas são as seguintes:

*  MySQL
*  PostgreSQL
*  Hana
*  Redshift
*  Athena
*  Sisense
*  SQL Server 

O destino de dados suportados são os seguintes:

*  Redshift Spectrum
*  Redshift
*  Google Big Query

##### LIMITAÇÕES CONHECIDAS

1. Os campos do tipo _Timestamp_ e _Date_ são sempre convertidos para _String_. 
2. O particionamento dos arquivos Parquet e Orc é sempre realizado pela coluna partition_field e, quando utilizada partição real, a filtragem dos registros deve ser realizada pela coluna partition_value. 
3. Named queries com tipo de carga script é suportado apenas pelo banco de dados destino Redshift.
4. Para destino Big Query, somente as partições com formatos yyyyMMdd, yyyyMM ou yyyy serão aceitos. Para geração de tabelas particionadas no Big query, utilizamos o conceito de tabelas fragmentadas ([Documentação](https://cloud.google.com/bigquery/docs/partitioned-tables?hl=pt-br#dt_partition_shard)), então a geração da tabela será schema.tabela_yyyyMMdd, onde para partição por mês o dia sempre será 01 e para partição ano o mês e dia será 0101.

## Módulos

##### DATABASE MODULE

Permite a extração de dados de bancos de dados relacionais, garantindo que os dados serão integrados entre a origem e o destino e que a estrutura das tabelas sejam mantidas idênticas. 

Campos do tipo **Date** e **Timestamp** serão sempre criadas como **String** na tabela de destino. 

Quando utilizamos o Spectrum como destino, os dados extraídos da origem são convertidos para Parquet ou Orc e armazenados no S3. Se não for utilizado nenhum campo para particionamento dos dados, será gerado um único arquivo no S3 e a atualização das informações do arquivo se tornarão mais lentas. Quando definido o PARTITION_FIELD, o conteúdo deste campo será utilizado para definir como os arquivos serão criados e, posteriormente, atualizados. Sendo assim, deve ser definido como PARTITION_FIELD um campo cujos valores não sofram atualização. Ex.: created_at

###### UTILIZAÇÃO

```
cd <HOME>/data-integration
bash kitchen.sh -file=<GLOVE_HOME>/extractor/glove.kjb \
	-param:MODULE=database \
	-param:CONNECTION_NAME=raw_human_resources \
	-param:INPUT_TABLE_SCHEMA=employee \
	-param:INPUT_TABLE_NAME=salary \
	-param:DELTA_FIELD=updated_at \
	-param:PARTITION_FIELD=created_at \
	-param:TARGET=spectrum \
	-param:DATASET_NAME=production \
	-param:STORAGE_BUCKET=glove-data-lake \
	-param:OUTPUT_FORMAT=parquet \
	-param:OUTPUT_COMPRESSION=snappy
```

###### PARÂMETROS 

| Tecnologia | Parâmetro           | Descrição                                                                                                                                                                                                               |
|------------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|            | MODULE              | Identifica o módulo que será utilizado                                                                                                                                                                                  |
|            | CONNECTION_NAME     | Identifica o nome de uma conexão configurada no arquivo de conexões.  O arquivo de conexões pode ser encontrado em ~/.kettle/connections.properties                                                                     |
|            | INPUT_TABLE_NAME    | Identifica a tabela de origem.                                                                                                                                                                                          |
|            | INPUT_TABLE_SCHEMA  | Identifica o schema de origem                                                                                                                                                                                           |
|            | DELTA_FIELD         | Identifica o campo de delta.  Por padrão é esperado um campo do tipo timestamp como delta. Para utilizar outros tipos de campos, consulte a seção de parâmetros adicionais.                                             |
| SPECTRUM   | PARTITION_FIELD     | Identifica o campo que será utilizado para particionamento dos dados. Por padrão é esperado um campo do tipo timestamp como partition. Para utilizar outros tipos de campos, consulte a seção de parâmetros adicionais. |
|            | TARGET              | Identifica a tecnologia de destino.                                                                                                                                                                                     |
|            | DATASET_NAME        | Identifica o dataset de destino.                                                                                                                                                                                        |
|            | STORAGE_BUCKET      | Identifica o bucket do storage que será utilizado para transferência de arquivos.                                                                                                                                       |
| SPECTRUM   | OUTPUT_FORMAT       | Identifica o formato de arquivo de saída.                                                                                                                                                                               |
|            | OUTPUT_TABLE_SCHEMA | Identifica o nome do schema de saída.  Quando esta parâmetro não for informado o nome do schema de entrada será o mesmo de saída.                                                                                       |


##### FILE MODULE

Permite a extração de dados de arquivos .csv contendo cabeçalho, a inferência de tipos e a criação de  tabela no destino para recepção dos dados. 

###### UTILIZAÇÃO

```
cd <HOME>/data-integration
bash kitchen.sh -file=<GLOVE_HOME>/extractor/glove.kjb \
	-param:MODULE=file \
	-param:FILE_INPUT_BUCKET=s3://marketing-bucket/facebook/campaigns_costs/brazil/queue/ \
	-param:FILE_DONE_BUCKET=s3://marketing-bucket/facebook/campaigns_costs/brazil/done/ \
	-param:FILE_INPUT_DELIMITER=";" \
	-param:FILE_OUTPUT_SCHEMA=raw_facebook \
	-param:FILE_OUTPUT_TABLE=campaigns_costs \
	-param:TARGET=spectrum \
	-param:DATASET_NAME=production \
	-param:STORAGE_BUCKET=glove-data-lake \
	-param:OUTPUT_FORMAT=parquet \
	-param:OUTPUT_COMPRESSION=snappy 
```

###### PARÂMETROS 

| Tecnologia | Parâmetro            | Descrição                                                                         |
|------------|----------------------|-----------------------------------------------------------------------------------|
|            | MODULE               | Identifica o módulo que será utilizado                                            |
|            | FILE_INPUT_BUCKET    | Identifica o caminho do arquivo de origem.                                        |
|            | FILE_DONE_BUCKET     | Identifica o caminho para movimentação do arquivo de origem após o processamento. |
|            | FILE_INPUT_DELIMITER | Identifica o delimitador do arquivo de origem.                                    |
|            | FILE_OUTPUT_SCHEMA   | Identifica o schema de destino.                                                   |
|            | FILE_OUTPUT_TABLE    | Identifica a tabela de destino.                                                   |
|            | FILE_INPUT_PARTITONED| Identifica se o arquivo é particionado.                                           |
|            | FILE_INPUT_MANIFEST  | Identifica o arquivo de manifesto para definição do tipo de campos                |
|            | TARGET               | Identifica a tecnologia de destino.                                               |
|            | DATASET_NAME         | Identifica o dataset de destino.                                                  |
|            | STORAGE_BUCKET       | Identifica o bucket do storage que será utilizado para transferência de arquivos. |
|            | OUTPUT_FORMAT        | Identifica o formato de arquivo de saída.                                         |
|            | OUTPUT_COMPRESSION   | Identifica o tipo de compressão que será aplicado no arquivo de saída.            |

##### NAMED QUERY MODULE

Permite a construção de processos de extração de dados compostos por um ou mais _steps_ com fontes de dados distintas e executados de forma sequencial.

###### EXEMPLO

```
cd <HOME>/data-integration
bash kitchen.sh -file=<GLOVE_HOME>/extractor/glove.kjb \
	-param:MODULE=query \
	-param:NAMED_QUERY=sales_funnel \
	-param:TARGET=spectrum \
	-param:DATASET_NAME=production \
	-param:STORAGE_BUCKET=glove-data-lake \
	-param:OUTPUT_FORMAT=parquet \
	-param:OUTPUT_COMPRESSION=snappy 
```

###### PARÂMETROS 

| Tecnologia | Parâmetro             | Descrição                                                                                                                                                                                                                            |
|------------|-----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|            | MODULE                | Identifica o módulo que será utilizado                                                                                                                                                                                               |
|            | NAMED_QUERY           | Identifica o diretório dos arquivos .sql que devem ser executados. Os arquivos .sql podem ser organizados livremente em diretórios e subdiretórios, no entanto, todos os steps de uma named query devem estar abaixo do mesmo nível. |
|            | NAMED_QUERY_DIRECTORY | Identifica o diretório do servidor no qual a named query deve ser procurada.  Este parâmetro deve ser informado apenas quando o valor atribuído ao NAMED_QUERY_DIRECTORY for diferente do valor da configuração                      |
|            | TARGET               | Identifica a tecnologia de destino.                                               |
|            | DATASET_NAME         | Identifica o dataset de destino.                                                  |
|            | STORAGE_BUCKET       | Identifica o bucket do storage que será utilizado para transferência de arquivos. |
|            | OUTPUT_FORMAT        | Identifica o formato de arquivo de saída.                                         |
|            | OUTPUT_COMPRESSION   | Identifica o tipo de compressão que será aplicado no arquivo de saída.            |

###### CONSTRUÇÃO

Uma _named query_ é composta por um ou mais arquivos [.sql](https://dafiti.jira.com/wiki/spaces/BIDG/pages/807698514/GLOVE#GLOVE-sql) e, quando necessário, um arquivo [.manifest](https://dafiti.jira.com/wiki/spaces/BIDG/pages/807698514/GLOVE#GLOVE-manifest) que devem ser organizados dentro um diretório da seguinte forma:

- Folder
    - 1.schema.table.connection.mode.sql
    - 2.schema.table.connection.mode.sql
    - ...
    - folder.manifest

###### .SQL

Permite definir a instrução que será realizada pelo _step_, sendo que cada atributo do nome do arquivo (separados por ponto) deve ser definido de acordo com a seguinte regra:

| Parâmetro | Valor                                                                                                                                              |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| 1         | Ordem de execução do step.                                                                                                                         |
| 2         | Nome do schema que será criado no destino.                                                                                                         |
| 3         | Nome da tabela que será criado no destino.                                                                                                         |
| 4         | Identifica o nome de uma conexão configurada no arquivo de conexões. O arquivo de conexões pode ser encontrado em ~/.kettle/connections.properties |
| 5         | Tipo de carga   

São suportados os seguintes tipos de carga:

| Tipo                                | Descrição                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FULL                                | Será executada uma carga full e será gerado um único arquivo de dados no storage.                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| PARTITIONED, PARTITION, INCREMENTAL | Será executada uma carga particionada, e será gerado um arquivo de dados para cada valor distinto do primeiro campo do resultset.  Para isto é necessário que o primeiro campo do resultset contenha o valor utilizado para o particionamento e a segunda coluna a chave única do registro. Quando o TARGET for Redshift, será realizado o UPSERT entre o conteúdo da tabela existente e os novos dados sendo carregados, para este destino é obrigatório que o campo custom_primary_key exista na query e que seu conteúdo seja único |
| STATICPARTITIONED, STATICPARTITION  | O comportamento será o mesmo do parâmetro PARTITIONED, porém os arquivos de cada partição serão substituídos ao invés de serem atualizados.                                                                                                                                                                                                                                                                                                                                                                                            |
| SCRIPT                              | Será executada uma instrução sql no database de destino sem que seja retornado nenhum valor.                                                                                                                                                                                                                                                                                                                                                                                                                                           |


###### EXEMPLO (Materialização)

**1.integration_layer.product_costs.redshift.full.sql**

```
SELECT <fields> FROM <schema>.<table>
```

Neste exemplo: 

**1** é a ordem no qual este step seria executado. 
**integration_layer** é o schema resultante da execução.
**product_costs** é a tabela resultante da execução.
**redshift** é a conexão na qual a consulta será executada. 
**full** é o tipo da carga, cuja descrição está na tabela anterior. 

###### EXEMPLO (Script)

**1.business_layer.product_costs.redshift.script.sql**

```
BEGIN;
    CREATE TABLE #TMP AS SELECT <fields> FROM <schema>.<table>;
    INSERT INTO business_layer.dim_product_config(<fields>)
        SELECT <fields> FROM #TMP;
END;
```

Neste exemplo: 

**1** é a ordem no qual este step seria executado. 
**business_layer** é apenas informativo e pode ser usado para identificar qual schema resultante do script, caso haja uma.
**product_costs** é apenas informativo e pode ser usado para identificar qual tabela resultante do script, caso haja uma.
**redshift** é a conexão na qual o script será executado. 
**script** é o tipo da carga, cuja descrição está na tabela anterior. 

###### .MANIFEST

Permite parametrizar cada **step** individualmente, de modo que além dos atributos obrigatórios contidos no nome do arquivo é possível individualizar alguns parâmetros como o tipo de campo, por meio do atributo METADATA, e passar parâmetros diferentes para cada step, por meio do parâmetro PARAMETER.

| Atributo           | Valor                                                                                                                                                                                                                                                                                                                                    | Escope      |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------|
| ORDER              | Identifica o step que será parametrizado.                                                                                                                                                                                                                                                                                                | OBRIGATÓRIO |
| METADATA           | Identifica o metadado da tabela.                                                                                                                                                                                                                                                                                                         | OPCIONAL    |
| PARAMETER          | Identifica os parâmetros que serão passados para a named query, no formato: parameter:"PARAMETER:VALUE". É possível utilizar macros para substituição de datas, respeitando o seguinte padrão: "PARAMETER:#D-1,yyyy-MM-dd", onde #D identifica que é uma macro de data e -1 qual operação será realizada e o formato de data é opcional. | OPCIONAL    |
| EXPORT_BUCKET      | Identifica o bucket para exportação de dados.                                                                                                                                                                                                                                                                                            | OPCIONAL    |
| EXPORT_SPREADSHEET | Identifica o Id da spreadsheet.                                                                                                                                                                                                                                                                                                          | OPCIONAL    |
| EXPORT_SHEET       | Identifica o nome da página (guia) da spreadsheet.                                                                                                                                                                                                                                                                                       | OPCIONAL    |
| IS_EXPORT          | Identifica se será gerado o export do resultset.                                                                                                                                                                                                                                                                                         | OPCIONAL    |
| ONLY_EXPORT        | Identifica se o processo apenas exporta o resultado sem materializar uma tabela.                                                                                                                                                                                                                                                         | OPCIONAL    |

O metadado possui os seguintes atributos obrigatórios:

| Atributo | Valor                                             |
|----------|---------------------------------------------------| 
| FIELD    | Nome do campo                                     |
| TYPE     | string, integer, number, timestamp, date, boolean |
| LENGTH   | Tamanho                                           |
| DECIMAL  | Decimal                                           |

###### EXEMPLO (Metadado)

```
[ 
   { 
      "order":1,
      "parameter":"DATE_FROM:#D-1|DATE_TO:#D"
      "metadata":[ 
         { 
            "field":"id",
            "type":"integer"
         },
         {
            "field":"name",
            "type":"string",
            "length":10
         },
         {
            "field":"gtv",
            "type":"number"
         }
      ]
   }
]
```

###### EXEMPLO (Export s3)

```
[ 
   { 
      "order":0,
      "export_bucket":"s3://nome-do-bucket/caminho-de-export/"
   },
   { 
      "order":1,
      "export_bucket":"s3://nome-do-bucket-2/caminho-de-export-2/"
   }
]
```

###### EXEMPLO (Export Google Sheets)

```
[ 
	{ 
		"order": 1, 
		"export_sheet": "nome_aba_1", 
		"export_spreadsheet": "id_spreadsheet" 
	}, 
	{ 
		"order": 2, 
		"export_sheet": "nome_aba_2", 
		"export_spreadsheet": "id_spreadsheet_2" 
	} 
]

```

* **PS**: Os exemplos acima podem ser utilizados tanto no arquivo de .manifest quanto no parâmetro MANIFEST (Explicação na seção Parâmetros Adicionais).



##### PARÂMETROS ADICIONAIS

| Tecnologia | Parâmetro                  | Descrição                                                                                                                                                                                                                                                                                                                                                                         | Valor padrão |
|------------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| SPECTRUM   | ALLOW_DUPLICATED           | Identifica se campos com custom primary key duplicadas serão inseridas na tabela.                                                                                                                                                                                                                                                                                                 | 0            |
| SPECTRUM   | ALLOW_RECREATE             | Identifica se a tabela destino pode ser recriada quando ocorre alteração da estrutura da tabela origem.                                                                                                                                                                                                                                                                           | 1            |
| BIGQUERY   | BIG_QUERY_PROJECT_ID       | Identifica o projeto do Google Cloud Plataform que será utilizado.                                                                                                                                                                                                                                                                                                                |              |
|            | CONNECTION_NAME            | Identifica o nome de uma conexão configurada no arquivo de conexões. O arquivo de conexões pode ser encontrado em ~/.kettle/connections.properties                                                                                                                                                                                                                                |              |
|            | CUSTOM_PRIMARY_KEY         | Identifica a chave primária da tabela origem. Este parâmetro deve ser informado apenas se a tabela de origem não possuir chave primária. Caso contrário o GLOVE fará o preenchimento automaticamente.                                                                                                                                                                             |              |
| BIGQUERY   | CLUSTER_COLUMNS            | Identifica uma lista separada por vírgulas de até quatro colunas de clustering. A lista não pode conter espaços.                                                                                                                                                                                                                                                                  |              |
|            | DEBUG                      | Identifica se o processo será executado em modo de debug. Neste modo, os arquivos gerados no servidor e as tabelas temporárias geradas no banco de dados não são apagados ao final do processo.                                                                                                                                                                                   | 0            |
|            | DELTA_DELAY                | Identifica o tempo em minutos que deve ser considerado na gravação do delta para evitar que itens sendo comitados não sejam extraídos.                                                                                                                                                                                                                                            | 60           |
|            | DELTA_FIELD                | Identifica o campo de delta.                                                                                                                                                                                                                                                                                                                                                      |              |
|            | DELTA_VALUE                | Identifica o valor do delta.                                                                                                                                                                                                                                                                                                                                                      |              |
| REDSHIFT   | DISTKEY                    | Identifica a chave de distribuição da tabela. (http://docs.aws.amazon.com/pt_br/redshift/latest/dg/c_best-practices-best-dist-key.html)                                                                                                                                                                                                                                           |              |
| REDSHIFT   | DISTSTYLE                  | Identifica o estilo de distribuição da tabela. (http://docs.aws.amazon.com/pt_br/redshift/latest/dg/c_best-practices-sort-key.html)                                                                                                                                                                                                                                               | none         |
| REDSHIFT   | ENCODE                     | Identifica o tipo de compressão aplicado nas colunas de tabelas do redshift.                                                                                                                                                                                                                                                                                                      | zstd         |
| SPECTRUM   | EXPORT_BUCKET_DEFAULT      | Identifica um ou mais buckets, separados por vírgula, para exportação de dados. Quando o export é originado de uma named query com escopo FULL, o nome do arquivo de saída pode ser incluído. Caso seja originado de uma named query com escopo PARTITIONED, somente o diretório de saída deve ser informado, uma vez que seja gerado um arquivo para cada partição identificada. |              |
| SPECTRUM   | EXPORT_PROFILE             | Identifica qual [profile](https://docs.aws.amazon.com/credref/latest/refdocs/creds-config-files.html) de configuração do S3 deve ser usado no processo de exportação.                                                                                                                                                                                                             | default      |
| SPECTRUM   | EXPORT_SHEETS_METHOD       | Identifica o tipo de atualização da planilha [0: FULL, 1:APPEND]                                                                                                                                                                                                                                                                                                                  | 0            |
| SPECTRUM   | EXPORT_SHEET_DEFAULT       | Identifica o nome da página (guia).                                                                                                                                                                                                                                                                                                                                               |              |
| SPECTRUM   | EXPORT_SPREADSHEET_DEFAULT | Identifica o Id da Google spreadsheet.                                                                                                                                                                                                                                                                                                                                            |              |
| SPECTRUM   | EXPORT_TYPE                | Identifica o tipo de arquivo que será gerado no processo de exportação. Onde: gz para gzip, zip para zip e csv para csv.                                                                                                                                                                                                                                                          | gz           |
| SPECTRUM   | FIELD_HAS_PREFIX           | Identifica se o prefixo dos campos das tabelas do HANA devem ser mantidos.                                                                                                                                                                                                                                                                                                        | 0            |
|            | FILE_DONE_BUCKET           | Identifica o caminho para movimentação do arquivo de origem após o processamento.                                                                                                                                                                                                                                                                                                 |              |
|            | FILE_INPUT_BUCKET          | Identifica o diretório de entrada de arquivos a serem processados.                                                                                                                                                                                                                                                                                                                |              |
|            | FILE_INPUT_DELIMITER       | Identifica o delimitador do arquivo de origem.                                                                                                                                                                                                                                                                                                                                    | ;            |
|            | FILE_INPUT_EXTENSION       | Identifica o tipo de arquivos de entrada, valor padrão é csv.                                                                                                                                                                                                                                                                                                                     | csv          |
|            | FILE_INPUT_MANIFEST        | Identifica o arquivo de manifesto para definição do tipo de campos                                                                                                                                                                                                                                                                                                                |              |
|            | FILE_INPUT_PARTITONED      | Identifica se o arquivo é particionado.                                                                                                                                                                                                                                                                                                                                           | 0            |
|            | FILE_OUTPUT_MODE           | Identifica o mode de atualização da tabela,                                                                                                                                                                                                                                                                                                                                       | append       |
|            | FILE_OUTPUT_SCHEMA         | Identifica o schema de destino.                                                                                                                                                                                                                                                                                                                                                   |              |
|            | FILE_OUTPUT_TABLE          | Identifica a tabela de destino.                                                                                                                                                                                                                                                                                                                                                   |              |
|            | GENERIC_PARAMETER          | Identifica os parâmetros que serão passados para uma named query. Parâmetros de data podem utilizar macros (#), no seguinte formato: :#D-1, :#D-1,yyyyMMdd                                                                                                                                                                                                                        |              |
| BIGQUERY   | GOOGLE_CLOUD_BUCKET        | Identifica o bucket a ser utilizado no Google Cloud.                                                                                                                                                                                                                                                                                                                              |              |
| SPECTRUM   | HAS_ATHENA                 | Identifica se o AWS Athena está disponível.                                                                                                                                                                                                                                                                                                                                       | 0            |
|            | INPUT_TABLE_NAME           | Identifica a tabela de origem.                                                                                                                                                                                                                                                                                                                                                    |              |
|            | INPUT_TABLE_SCHEMA         | Identifica o schema de origem                                                                                                                                                                                                                                                                                                                                                     |              |
| SPECTRUM   | IS_EXPORT                  | Identifica se será gerada exportação do resultset para o bucket informado no parâmetro STORAGE_BUCKET.                                                                                                                                                                                                                                                                            | 0            |
| SPECTRUM   | IS_PARALLEL                | Identifica se será utilizado processamento paralelo para particionamento de arquivos.                                                                                                                                                                                                                                                                                             | 0            |
|            | IS_RECREATE                | Identifica se a tabela de destino deve ser recriada e todos os dados recarregados. Sempre que o IS_RECREATE é utilizado, uma cópia dos dados da tabela é copiada para o bucket de disaster recovery. [0: Inativo, 1: Ativo], 0 é o padrão                                                                                                                                         | 0            |
|            | IS_RELOAD                  | Identifica se todos os dados devem ser recarregados sem que a tabela de destino seja recriada. Não é criado disaster recovery.                                                                                                                                                                                                                                                    | 0            |
|            | IS_SCHEMA_EVOLUTION        | Identifica se deve manter os dados em caso de mudanças no metadado, esse parâmetro quando passado o valor 1, deve ser usado em conjunto com o parâmetro IS_RECREATE com o valor 1                                                                                                                                                                                                 | 0            |
|            | IS_SPECTRUM                | Identifica se o AWS Redshift Spectrum está configurado.                                                                                                                                                                                                                                                                                                                           | 1            |
|            | MANIFEST                   | Identifica se o manifest será passado por parâmetro. (Disponível para módulo file e query)                                                                                                                                                                                                                                                                                        |              |
|            | METADATA_BLACKLIST         | Identifica a lista de campos a serem excluídos (apenas para database module). Para mais de um campo, utilizar vírgula.                                                                                                                                                                                                                                                            |              |
|            | MODULE                     | Identifica o módulo que será utilizado                                                                                                                                                                                                                                                                                                                                            | database     |
|            | NAMED_QUERY                | Identifica o diretório contendo osstepsde umanamed query.                                                                                                                                                                                                                                                                                                                         |              |
|            | NAMED_QUERY_DIRECTORY      | Identifica o diretório de origem dasnamed queries. Por padrão, asnamed queriessão procuradas no diretório /home/etl/named_query                                                                                                                                                                                                                                                   |              |
|            | NAMED_QUERY_IGNORE_STEP    | Identifica os passos que devem ser ignorados na execução de uma named query, caso haja mais de um step separar vírgula, exemplo: 1,2,3                                                                                                                                                                                                                                            |              |
|            | NAMED_QUERY_INCLUDE_STEP   | Identifica os passos que devem ser considerados na execução de umanamed query, caso haja mais de um step separar vírgula, exemplo: 1,2,3                                                                                                                                                                                                                                          |              |
| SPECTRUM   | ONLY_EXPORT                | Identifica se o processo é apenas de exportação, sem que nenhuma tabela seja materializada.                                                                                                                                                                                                                                                                                       | 0            |
| SPECTRUM   | OUTPUT_COMPRESSION         | Identifica o tipo de compressão dos arquivos de saída.O valor padrão para arquivos parquet é o .gzip e para os arquivos .orc o zlib.                                                                                                                                                                                                                                              | gzip         |
| SPECTRUM   | OUTPUT_FORMAT              | Identifica o formato de arquivo de saída.                                                                                                                                                                                                                                                                                                                                         | parquet      |
|            | OUTPUT_TABLE_NAME          | Identifica o nome da tabela de saída.                                                                                                                                                                                                                                                                                                                                             |              |
|            | OUTPUT_TABLE_SCHEMA        | Identifica o nome do schema de saída.                                                                                                                                                                                                                                                                                                                                             |              |
|            | PARTITION_EAGER            | Identifica se deve extrair todo o dado, quando 1, ou uma partição por vez, quando 0.                                                                                                                                                                                                                                                                                              | 1            |
|            | PARTITION_FIELD            | Identifica o campo de negócio que será utilizado para o particinamento dos dados, para o BigQuery deve ser um campo do tipo [INT, TIMESTAMP, DATETIME ou DATE].                                                                                                                                                                                                                   |              |
|            | PARTITION_FORMAT           | Formato da partição quando o particionamento for feito por data, podem ser utilizado os seguintes formatos YYYY,  YYYYMM, YYYYMMDD e YYYYWW.                                                                                                                                                                                                                                      | yyyymm       |
|            | PARTITION_HAS_PREFIX       | Identifica se o campo de partição da tabela do SAP utiliza o caracter 0 como prefixo.                                                                                                                                                                                                                                                                                             | 1            |
|            | PARTITION_LENGTH           | Quantidade de registros de cada partição quando o particionamento for feito por ID. É importante que as mesma recomendações aplicadas para o PARTITION_FORMAT sejam seguidas para a escolha do PARTITION_LENGTH.                                                                                                                                                                  | 1000000      |
| SPECTRUM   | PARTITION_MERGE            | Identifica se deve ser realizadomergedos dados da partição. Quando este parâmetro recebe o valor 0, a partição processada é substituída.                                                                                                                                                                                                                                          | 1            |
|            | PARTITION_MINIMUN          | Identifica o valor mínimo para definição de range de partição de dada.                                                                                                                                                                                                                                                                                                            | 2015         |
| SPECTRUM   | PARTITION_MODE             | Tipo de particionamento que será utilizado. Quando utilizado o tipo virtual, os arquivos serão particionados dentro do diretório do S3, mas não será criada o particionamento no catálogo do Athena. Quando utilizado o particionamento real, os arquivos são particionados no S3 e é criado o registro do particionamento no catálogo do Athena.                                 | virtual      |
|            | PARTITION_TYPE             | Identifica o tipo da partição [id, date, timestamp], para o BigQuery [DAY, HOUR, MONTH ou YEAR].                                                                                                                                                                                                                                                                                  |              |
| BIGQUERY   | PARTITION_EXPIRATION       | Identifica a vida útil padrão (em segundos) para as partições da tabela. Não há valor mínimo. O prazo de validade é avaliado para a data da partição acrescida deste valor.                                                                                                                                                                                                       | 0            |
|            | QUEUE_FILES_SIZE_LIMIT     | Identifica o tamanho máximo em MB dos arquivos temporários.                                                                                                                                                                                                                                                                                                                       | 100000000    |
| REDSHIFT   | QUOTE                      | Identifica se o unload de dados do Redshift devem conter aspas duplas, sendo 0 para não e 1 para sim.                                                                                                                                                                                                                                                                             | 1            |
|            | QUOTE_ESCAPE               | Identifica o caracter utilizado para escape de aspas duplas, padrão é "                                                                                                                                                                                                                                                                                                           | "            |
|            | SAMPLE                     | Identifica o samplede dados que é analisado para a inferência do metadado da tabela de destino.                                                                                                                                                                                                                                                                                   | 100000       |
|            | SISENSE_CUBE               | Identifica o cubo do sisense no qual as queries serão realizadas.                                                                                                                                                                                                                                                                                                                 |              |
| REDSHIFT   | SORTKEY                    | Identifica a chave de distribuição do Redshift.                                                                                                                                                                                                                                                                                                                                   |              |
| SPECTRUM   | SPLIT_STRATEGY             | Identifica a estratégia utilizada para o particionamento dos dados sendo: FAST, os dados sendo processados são confiáveis e não contém caracteres especiais ou quebra de linas. SECURE, os dados não são confiáveis.                                                                                                                                                              | SECURE       |
|            | STAGING_SCHEMA             | Identifica o schema utilizado como staging.                                                                                                                                                                                                                                                                                                                                       | transient    |
| SPECTRUM   | STORAGE_BUCKET             | Identifica o bucketdo storage que será utilizado para transferência de arquivos.                                                                                                                                                                                                                                                                                                  |              |
|            | STORAGE_BUCKET_BACKUP      | Identifica o bucket do storage que será utilizado para backup de arquivos.                                                                                                                                                                                                                                                                                                        |              |
|            | TARGET                     | Identifica a tecnologia de destino. [spectrum, redshift, bigquery]                                                                                                                                                                                                                                                                                                                | spectrum     |
| SPECTRUM   | THREAD                     | Identifica o número de threads que serão usadas para conversão de arquivos.                                                                                                                                                                                                                                                                                                       | 4            |
| BIGQUERY   | TIMEZONE_OFFSET            | Identifica o timezone offset para os campos do tipo timestamp.                                                                                                                                                                                                                                                                                                                    |              |
|            | WHERE_CONDITION_TO_DELTA   | Condição para carga delta.                                                                                                                                                                                                                                                                                                                                                        |              |
|            | WHERE_CONDITION_TO_RECOVER | Identifica a condição para recuperação de desastres.                                                                                                                                                                                                                                                                                                                              | 1=1          |

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
