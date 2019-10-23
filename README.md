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
- Jenkins ( [https://jenkins.io/](https://jenkins.io/) )
- Pentaho Data Integration
- Parallel ( [https://www.gnu.org/software/parallel/](https://www.gnu.org/software/parallel/) )
- jq ([https://stedolan.github.io/jq/](https://stedolan.github.io/jq/))
- pigz ( [https://zlib.net/pigz/](https://zlib.net/pigz/) )

##### CONFIGURAÇÃO

- Clone o projeto do Glove.
- Crie o arquivo ~/.kettle/kettle.properties, com o seguinte conteúdo:
 
```
# Core
GLOVE_HOME=<Glove home directory>
GLOVE_TEMP=<Temp files directory>
GLOVE_METADATA=<Glove metadata directory>
GLOVE_NAMED_QUERIES=<.sql files>
  
# Redshift
REDSHIFT_JDBC_HOST=<Host>
REDSHIFT_JDBC_PORT=<Port>
REDSHIFT_JDBC_DB=<Database>
REDSHIFT_JDBC_USER_NAME=<User>
REDSHIFT_JDBC_PASSWORD=<Password>
 
# Redshift Spectrum
SPECTRUM_ROLE=<Role>
 
# Google Cloud SDK
GOOGLE_TOOLS_HOME=<Google Cloud SDK path>
 
# Sisense
GLOVE_SISENSE_URL=<Sisense URL>
GLOVE_SISENSE_TOKEN=<Sisense API Token>    
```

Para configuração do Spectrum Role, consulte esta documentação:  [Utilização do Amazon Redshift Spectrum para consultar dados externos](https://docs.aws.amazon.com/pt_br/redshift/latest/dg/c-using-spectrum.html)   

- Crie o arquivo ~/.kettle/connection.properties, inserindo o cabeçalho e uma nova linha para cada conexão:  

    - **CONNECTION_NAME**: Nome da conexão. 
    - **JDBC_URL**: JDBC URL da conexão.
    - **JDBC_DRIVER_CLASS**: Classe do driver de conexão.
    - **DB_HOST**: Endereço do banco de dados .
    - **DB_DATABASE**: Database.
    - **DB_USER**: Usuário do banco de dados.
    - **DB_PASSWORD**: Senha do usuário do banco de dados. 
    - **DATABASE_TYPE**: Tipo do banco de dados, sendo suportado os seguintes valores: **REDSHIFT, ATHENA, MYSQL, POSTGRES **e** SAP_HANA**. 

##### TECNOLOGIAS SUPORTADAS

As fontes de dados suportadas são as seguintes:

*  MySQL
*  PostgreSQL
*  Hana
*  Redshift
*  Athena
*  Sisense

O destino de dados suportados são os seguintes:

*  Redshift Spectrum
*  Redshift
*  Google Big Query

##### LIMITAÇÕES CONHECIDAS

1. Os campos do tipo _Timestamp_ e _Date_ são sempre convertidos para _String_. 
2. O particionamento dos arquivos Parquet e Orc é sempre realizado pela coluna partition_field e, quando utilizada partição real, a filtragem dos registros deve ser realizada pela coluna partition_value. 

## Módulos

##### DATABASE MODULE

Permite a extração de dados de bancos de dados relacionais, garantindo que os dados serão integrados entre a origem e o destino e que a estrutura das tabelas sejam mantidas idênticas. 

Campos do tipo **Date** e **Timestamp** serão sempre criadas como **String** na tabela de destino. 

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

Permite a extração de dados de arquivos .csv contendo cabeçalho, a inferência de tipos e a criação de  tabela no destino para recepção dos dados. 

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
|            | TARGET               | Identifica a tecnologia de destino.                                               |
|            | DATASET_NAME         | Identifica o dataset de destino.                                                  |
|            | STORAGE_BUCKET       | Identifica o bucket do storage que será utilizado para transferência de arquivos. |
|            | OUTPUT_FORMAT        | Identifica o formato de arquivo de saída.                                         |
|            | OUTPUT_COMPRESSION   | Identifica o tipo de compressão que será aplicado no arquivo de saída.            |

##### NAMED QUERY MODULE

Permite a construção de processos de extração de dados compostos por um ou mais _steps_ com fontes de dados distintas e executados de forma sequencial.

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

Uma _named query_ é composta por um ou mais arquivos [.sql](https://dafiti.jira.com/wiki/spaces/BIDG/pages/807698514/GLOVE#GLOVE-sql) e, quando necessário, um arquivo [.manifest](https://dafiti.jira.com/wiki/spaces/BIDG/pages/807698514/GLOVE#GLOVE-manifest) que devem ser organizados dentro um diretório da seguinte forma:

- Folder
    - 1.schema.table.connection.mode.sql
    - 2.schema.table.connection.mode.sql
    - ...
    - folder.manifest

###### .SQL

Permite definir a instrução que será realizada pelo _step_, sendo que cada atributo do nome do arquivo (separados por ponto) deve ser definido de acordo com a seguinte regra:

| Parâmetro | Valor                                                                                                                                              |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| 1         | Ordem de execução do step.                                                                                                                         |
| 2         | Nome do schema que será criado no destino.                                                                                                         |
| 3         | Nome da tabela que será criado no destino.                                                                                                         |
| 4         | Identifica o nome de uma conexão configurada no arquivo de conexões. O arquivo de conexões pode ser encontrado em ~/.kettle/connections.properties |
| 5         | Tipo de carga   

São suportados os seguintes tipos de carga:

| Tipo              | Descrição                                                                                                                                                                                                                                                                                   |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FULL              | Será executada uma carga full e será gerado um único arquivo de dados no storage.                                                                                                                                                                                                           |
| PARTITIONED       | Será executada uma carga particionada, e será gerado um arquivo de dados para cada valor distinto do primeiro campo do resultset.  Para isto é necessário que o primeiro campo do resultset contenha o valor utilizado para o particionamento e a segunda coluna a chave única do registro. |
| STATICPARTITIONED | O comportamento será o mesmo do parâmetro PARTITIONED, porém os arquivos de cada partição serão substituídos ao invés de serem atualizados.                                                                                                                                                 |
| SCRIPT            | Será executada uma instrução sql no database de destino sem que seja retornado nenhum valor.                                                                                                                                                                                                |

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

Permite parametrizar cada **step** individualmente, de modo que além dos atributos obrigatórios contidos no nome do arquivo é possível individualizar alguns parâmetros como o tipo de campo, por meio do atributo METADATA, e passar parâmetros diferentes para cada step, por meio do parâmetro PARAMETER.

| Atributo  | Valor                                                                                                                                                                                                                                                                                                                                    |   |   | Escope      |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---|---|-------------|
| ORDER     | Identifica o step que será parametrizado.                                                                                                                                                                                                                                                                                                |   |   | OBRIGATÓRIO |
| METADATA  | Identifica o metadado da tabela.                                                                                                                                                                                                                                                                                                         |   |   | OPCIONAL    |
| PARAMETER | Identifica os parâmetros que serão passados para a named query, no formato: parameter:"PARAMETER:VALUE". É possível utilizar macros para substituição de datas, respeitando o seguinte padrão: "PARAMETER:#D-1,yyyy-MM-dd", onde #D identifica que é uma macro de data e -1 qual operação será realizada e o formato de data é opcional. |   |   | OPCIONA     |

O metadado possui os seguintes atributos obrigatórios:

| Atributo | Valor                                            |
|----------|--------------------------------------------------|
| FIELD    | Nome do campo                                    |
| TYPE     | string, integer, number, timestamp,date, boolean |
| LENGTH   | Tamanho                                          |
| DECIMAL  | Decimal                                          |

###### EXEMPLO (Metadado)

```
[ 
   { 
      "order":1,
      "parameter":"DATE_FROM:#D-1|DATE_TO:#D"
      "metadata":[ 
         { 
            "field":"id",
            "type":"Integer",
            "length":9,
            "decimal":0
         }
      ]
   }
]
```

##### PARÂMETROS ADICIONAIS

| Tecnologia         | Parâmetro                | Descrição                                                                                                                                                                                                                                                                                                                                         |
|--------------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|                    | MODULE                   | Identifica o módulo que será utilizado                                                                                                                                                                                                                                                                                                            |
|                    | ALLOW_DUPLICATED         | Identifica se permite chaves única duplicadas em um arquivo paraquet ou orc.                                                                                                                                                                                                                                                                      |
| SPECTRUM           | ALLOW_RECREATE           | Identifica se a tabela destino pode ser recriada quando ocorre alteração da estrutura da tabela origem.                                                                                                                                                                                                                                           |
|                    | CONNECTION_NAME          | Identifica o nome de uma conexão configurada no arquivo de conexões. O arquivo de conexões pode ser encontrado em ~/.kettle/connections.properties                                                                                                                                                                                                |
|                    | CUSTOM_PRIMARY_KEY       | Identifica a chave primária da tabela origem. Este parâmetro deve ser informado apenas se a tabela de origem não possuir chave primária. Caso contrário o GLOVE fará o preenchimento automaticamente.                                                                                                                                             |
|                    | DEBUG                    | Identifica se o processo será executado em modo dedebug. Neste modo, os arquivos gerados no servidor e as tabelas temporárias geradas no banco de dados não são apagados ao final do processo.                                                                                                                                                    |
|                    | DELTA_DELAY              | Identifica o tempo em minutos que deve ser considerado na gravação do delta para evitar que itens sendo comitados não sejam extraídos.                                                                                                                                                                                                            |
|                    | DELTA_FIELD              | Identifica o campo de delta.                                                                                                                                                                                                                                                                                                                      |
|                    | DELTA_VALUE              | Identifica o valor do delta.                                                                                                                                                                                                                                                                                                                      |
| REDSHIFT           | DISTKEY                  | Identifica a chave de distribuição da tabela. (http://docs.aws.amazon.com/pt_br/redshift/latest/dg/c_best-practices-best-dist-key.html)                                                                                                                                                                                                           |
| REDSHIFT           | DISTSTYLE                | Identifica o estilo de distribuição da tabela. (http://docs.aws.amazon.com/pt_br/redshift/latest/dg/c_best-practices-sort-key.html)                                                                                                                                                                                                               |
|                    | EXPORT_BUCKET_DEFAULT    | Identifica o bucket para exportação de dados.                                                                                                                                                                                                                                                                                                     |
|                    | GENERIC_PARAMETER        | Identifica os parâmetros que serão passados para uma named query. Parâmetros de data podem utilizar macros (#), no seguinte formato: <parameter>:#D-1, <parameter>:#D-1,yyyyMMdd                                                                                                                                                                  |
| REDSHIFT           | IS_EXPORT                | Identifica se será gerada exportação doresultsetpara obucketinformado no parâmetro STORAGE_BUCKET.                                                                                                                                                                                                                                                |
| REDSHIFT, BIGQUERY | ONLY_EXPORT              | Identifica se o processo é apenas de exportação, sem que nenhuma tabela seja materializada.                                                                                                                                                                                                                                                       |
|                    | IS_RECREATE              | Identifica se a tabela de destino deve ser recriada e todos os dados recarregados.                                                                                                                                                                                                                                                                |
|                    | IS_RELOAD                | Identifica se todos os dados devem ser recarregados sem que a tabela de destino seja recriada.                                                                                                                                                                                                                                                    |
|                    | NAMED_QUERY              | Identifica o diretório contendo osstepsde umanamed query.                                                                                                                                                                                                                                                                                         |
|                    | NAMED_QUERY_DIRECTORY    | Identifica o diretório de origem dasnamed queries. Por padrão, asnamed queriessão procuradas no diretório /home/etl/named_query                                                                                                                                                                                                                   |
|                    | NAMED_QUERY_IGNORE_STEP  | Identifica os passos que devem ser ignorados na execução de umanamed query.                                                                                                                                                                                                                                                                       |
|                    | OUTPUT_FORMAT            | Identifica o formato de arquivo de saída.                                                                                                                                                                                                                                                                                                         |
|                    | OUTPUT_COMPRESSION       | Identifica o tipo de compressão dos arquivos de saída, sendo suportado gzip e snappy.                                                                                                                                                                                                                                                             |
|                    | WHERE_CONDITION_TO_DELTA | Condição para carga delta.                                                                                                                                                                                                                                                                                                                        |
|                    | PARTITION_FIELD          | Identifica o campo que será utilizado para particionamento dos dados.                                                                                                                                                                                                                                                                             |
|                    | PARTITION_TYPE           | Tipo do campo utilizado como partição, podem ser utilizados os tipostimestamp,datee id.                                                                                                                                                                                                                                                           |
|                    | PARTITION_FORMAT         | Formato da partição quando o particionamento for feito por data, podem ser utilizado os seguintes formatos YYYY,  YYYYMM, YYYYMMDD e YYYYWW.                                                                                                                                                                                                      |
|                    | PARTITION_LENGTH         | Quantidade de registros de cada partição quando o particionamento for feito por ID. É importante que as mesma recomendações aplicadas para o PARTITION_FORMAT sejam seguidas para a escolha do PARTITION_LENGTH.                                                                                                                                  |
|                    | PARTITION_MODE           | Tipo de particionamento que será utilizado. Quando utilizado o tipo virtual, os arquivos serão particionados dentro do diretório do S3, mas não será criada o particionamento no catálogo do Athena. Quando utilizado o particionamento real, os arquivos são particionados no S3 e é criado o registro do particionamento no catálogo do Athena. |
|                    | PARTITION_HAS_PREFIX     | Identifica se o campo de partição da tabela do SAP utiliza o caracter 0 como prefixo.                                                                                                                                                                                                                                                             |
|                    | PARTITION_LAZY           | Identifica se deve particionar os dados apenas no momento da carga para o storage. Caso o valor do parâmetro seja definido como 1, os dados serão particionados no momento em que são extraídos da fonte.                                                                                                                                         |
| SPECTRUM           | PARTITION_MERGE          | Identifica se deve ser realizadomergedos dados da partição. Quando este parâmetro recebe o valor 0, a partição processada é substituída.                                                                                                                                                                                                          |
| SPECTRUM           | SAMPLE                   | Identifica osamplede dados que é analisado para a inferência do metadado da tabela de destino.                                                                                                                                                                                                                                                    |
| SPECTRUM           | STORAGE_BUCKET           | Identifica obucketdo storage que será utilizado para transferência de arquivos.                                                                                                                                                                                                                                                                   |
| SPECTRUM           | OUTPUT_FORMAT            | Identifica o formato de arquivo de saída.                                                                                                                                                                                                                                                                                                         |
| SPECTRUM           | OUTPUT_COMPRESSION       | Identifica o tipo de compressão dos arquivos de saída.O valor padrão para arquivos parquet é o .gzip e para os arquivos .orc o zlib.                                                                                                                                                                                                              |
|                    | OUTPUT_TABLE_NAME        | Identifica o nome da tabela de saída.                                                                                                                                                                                                                                                                                                             |
|                    | OUTPUT_TABLE_SCHEMA      | Identifica o nome do schema de saída.                                                                                                                                                                                                                                                                                                             |
| BIGQUERY           | TIMEZONE_OFFSET          | Identifica otimezone offsetpara os campos do tipotimestamp.                                                                                                                                                                                                                                                                                       |
| BIGQUERY           | PROJECT_ID               | Identifica o projeto do Google Cloud Plataform que será utilizado.                                                                                                                                                                                                                                                                                |
| SPECTRUM           | THREAD                   | Identifica o número de threads que serão usadas para conversão de arquivos.                                                                                                                                                                                                                                                                       |
| SPECTRUM           | ALLOW_DUPLICATED         | Identifica se campos comcustom primary keyduplicadas serão inseridas na tabela.                                                                                                                                                                                                                                                                   |
|                    | FILE_DONE_BUCKET         | Identifica o caminho para movimentação do arquivo de origem após o processamento.                                                                                                                                                                                                                                                                 |
|                    | FILE_INPUT_BUCKET        | Identifica o diretório de entrada de arquivos a serem processados.                                                                                                                                                                                                                                                                                |
|                    | FILE_INPUT_DELIMITER     | Identifica o delimitador do arquivo de origem.                                                                                                                                                                                                                                                                                                    |
|                    | FILE_OUTPUT_SCHEMA       | Identifica o schema de destino.                                                                                                                                                                                                                                                                                                                   |
|                    | FILE_OUTPUT_TABLE        | Identifica a tabela de destino.                                                                                                                                                                                                                                                                                                                   |
| SPECTRUM           | STATISTICS_UPDATE        | Identifica se as estatísticas da tabela devem ser atualizadas.                                                                                                                                                                                                                                                                                    |
| SPECTRUM           | HAS_ATHENA               | Identifica se o AWS Athena está disponível                                                                                                                                                                                                                                                                                                        |
| SPECTRUM           | FIELD_HAS_PREFIX         | Identifica se o prefixo dos campos das tabelas do HANA devem ser mantidos.                                                                                                                                                                                                                                                                        |


## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
