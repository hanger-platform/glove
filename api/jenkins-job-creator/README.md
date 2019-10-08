# Jenkins job creator [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Cria um jobs no jenkins utilizando como base um template pré determinado.

## How it works

O **Jenkins job creator** permite a criação em lote de jobs no Jenkins a partir de um template. 

## Requisitos

- Java 8 +
- Maven
- Git

## Construção

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **_Jenkins job creator_ ** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo jenkins-job-creator.jar será gerado no subdiretório _target_.

## Utilização

```bash
java -jar jenkins-job-creator.jar \
	--url=<Jenkins URL> \
	--user=<Jenkins user> \
	--password=<Jenkins password> \
	--template=<Template job in Jenkins> \
	--file=<JSON parameter file> \
```

## Exemplo

Crie um job no Jenkins que será utilizado como template e informe os valores que serão substituídos utilizados o seguinte formato ${}.

```bash
cd /home/etl/data-integration
bash kitchen.sh -file=/home/etl/glove/extractor/glove.kjb \
    -param:CONNECTION_NAME=${connection_name} \
    -param:INPUT_TABLE_NAME=${input_table_name} \
    -param:INPUT_TABLE_SCHEMA=${input_table_schema} \
    -param:TARGET=${target} \
    -param:DATASET_NAME=${dataset_name} \
    -param:OUTPUT_TABLE_SCHEMA=${output_table_schema} \
    -param:DELTA_FIELD=${delta_field} \
    -param:PARTITION_FIELD=${partition_field} \
    -param:STORAGE_BUCKET=${storage_bucket} \
    -param:OUTPUT_FORMAT=parquet \
    -param:OUTPUT_COMPRESSION=snappy 
```

Crie um arquivo json, contendo uma lista de objetos cujos valores serão utilizados para substituir as variáveis do job:

```json
[
    	{"name":"GLOVE_raw_campaign_factory_dafiti_ar_app_and_miac","connection_name":"RAW_CAMPAIGN_FACTORY","input_table_name":"dafiti_ar_app_and_miac","input_table_schema":"campaign_factory","target":"spectrum","dataset_name":"dftdwh","storage_bucket":"bi-bucket","output_table_schema":"spc_raw_campaign_factory","delta_field":"","partition_field":""},
...
]
```

Execute o Jenkins job creator:

```bash
java -jar jenkins-job-creator.jar \
	--url=http://172.18.10.204:8080/jenkins \
	--user=vgomes \
	--password=xxx \
	--template=GLOVE_template \
	--file=/home/valdiney/parameters.json
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
