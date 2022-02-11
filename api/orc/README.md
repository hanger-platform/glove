# Orc [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Ferramenta que possibilita converter arquivos csv em ORC

## How it works

A ferramenta **orc** permite converter arquivos cujo formato é _csv_ para o formato colunar [orc](https://orc.apache.org/), a estrutura do arquivo orc gerado se baseia em um _avro schema_ que deve ser informado nos parâmetros de execução. Também é possível efetuar a mescla de um arquivo existente no S3 com um arquivo na estação no qual a ferramenta está sendo executada.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- AWS Cli (para casos de mescla de arquivo do s3)

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **orc** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **orc.jar** será gerado no subdiretório **_target_**.

## Utilização

#### Parâmetros
##### Explicação dos parâmetros disponíveis na ferramenta.

```bash
java -jar orc.jar  \
	--folder=<Folder where the files to be converted to orc are> \
	--schema=<Avro schema file to be used on conversion> \
	--filename=<(Optional) Filename, with wildcard if necessary, to be converted> \
	--header=<(Optional) Identifies if the csv file has a header> \
	--replace=<(Optional) Identifies if csv files will be replaced to orc files> \
	--thread=<(Optional) Limit of threads, be careful with it not to overload the workstation memory; default is 1> \
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
##### Conversão de csv para orc.

```bash
java -jar orc.jar  \
	--folder="/home/user/csv_files/" \
	--schema=/home/user/metadata/avro_json.json \
	--filename="*.csv" \
	--delimiter=; \
	--compression=snappy \
	--thread=4 \
```

Neste exemplo, todos os arquivo que tiverem a extensão _.csv_ serão convertidos para _orc_ baseado no parâmetro schema (avro schema).

O arquivo **avro_json.json** deve ter a seguinte estrutura:

```json
[
	{"name": "campo_string","type":["null", "string"], "default": null},
	{"name": "campo_long","type":["null", "long"] , "default": null}
]
```

Os arquivos _orc_ serão gerados na mesma pasta dos arquivos _csv_ e com o mesmo nome:

Estrutura da pasta antes da execução:
```bash
	- home	
		- user
			-csv_files
				- csv_0001.csv
				- csv_0002.csv
```

Estrutura da pasta depois da execução:
```bash

	- home	
		- user
			-csv_files
				- csv_0001.csv
				- csv_0001.snappy.orc
				- csv_0002.csv
				- csv_0002.snappy.orc
```

Caso desejar excluir os arquivos _csvs_ após a transformações em orc, basta utilizar o parâmetro _replace_:

```bash
--replace='true'
```

#### Exemplo 2
##### Conversão de csv para orc com merge de um arquivo do S3 com um arquivo local.

```bash
java -jar orc.jar  \
	--folder="/home/user/csv_files/" \
	--filename=* \
	--delimiter=; \
	--schema=/home/user/metadata/avro_json.json \
	--thread=4 \
	--duplicated=0 \
	--fieldkey=1 \
	--merge=1 \
	--bucket=s3://bucket/orc_files/ \
	--mode=virtual
```

Neste exemplo após a conversão dos arquivos _csvs_ em _orc_, o processo irá fazer o _download_ do arquivos no bucket informado e irá mesclar os arquivos _orcs_ locais com os arquivos baixados. O _log_ da aplicação vai mostrar a atualização após a finalização do processo.

Exemplo de log:
```bash
Converting CSV to ORC: /home/user/csv_files
[20220210.snappy.orc] records: 16, Delta: 16, ( Updated: 16, Inserted: 0, Duplicated:0 ) Final: 16
```

No caso acima, o arquivo _orc_ local foi mesclado com o arquivo _orc_ baixado do S3, no exemplo acima, 16 registros foram atualizados, 0 foram inseridos e não foi encontrado nenhum registro duplicado.

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
