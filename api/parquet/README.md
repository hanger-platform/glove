# Parquet [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Ferramenta que possibilita converter arquivos csv em parquet

## How it works

A ferramenta **parquet** permite converter arquivos cujo formato é _csv_ para o formato colunar [parquet](https://parquet.apache.org/), a estrutura do arquivo parquet gerado se baseia em um _avro schema_ que deve ser informado nos parâmetros de execução. Também é possível efetuar a mescla de um arquivo existente no S3 com um arquivo na estação no qual a ferramenta está sendo executada.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- AWS Cli (para casos de mescla de arquivo do s3)

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **parquet** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **parquet.jar** será gerado no subdiretório **_target_**.

## Utilização

#### Parâmetros
##### Explicação dos parâmetros disponívels na ferramenta.

```bash
java -jar parquet.jar  \
	--folder=<Folder where the files to be converted to parquet are> \
	--schema=<Avro schema file to be used on conversion> \
	--filename=<(Optional) Filename, with wildcard if necessary, to be converted> \
	--header=<(Optional) Identifies if the csv file has a header> \
	--replace=<(Optional) File delimiter; ';' as default> \
	--thread=<(Optional) Limit of threads; default is 1> \
	--compression=<(Optional) Identifies the compression to be applied; default is gzip> \
	--delimiter=<(Optional) Delimiter of csv files; default is ;> \
	--quote=<(Optional) Identifies the quote escape character; default is \""> \
	--escape=<(Optional) Identifies the quote escape character; default is \"> \
	--fieldkey=<(Optional) Unique key field; default is -1> \
	--duplicated=<(Optional) Identifies if duplicated is allowed; default is 0> \
	--merge=<(Optional) Identifies if should merge existing files; default is 0> \
	--debug=<(Optional) Show full log messages; default is 0"> \
	--bucket=<(Optional) Identifies the storage bucket> \
	--mode=<(Optional) Identifies the partition mode>
```             

#### Exemplo 1
##### Conversão de csv para parquet.

```bash
java -jar parquet.jar  \
	--folder="/home/user/csv_files/" \
	--schema=/home/user/metadata/avro_json.json \
	--filename="*.csv" \
	--delimiter=; \
	--compression=snappy \
	--thread=4 \
```

Neste exemplo, todos os arquivo que tiverem a extensão _.csv_ serão convertidos para _parquet_.



#### Exemplo 2
##### Conversão de csv para parquet com merge de um arquivo do S3 com um arquivo local.

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
