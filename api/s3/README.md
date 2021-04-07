
# S3 Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados armazenados em buckets do S3 

## How it works

O **S3 Extractor** permite a extração de dados de arquivos armazendos em buckets do S3.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- AWS CLI

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **s3** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **s3.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* O AWS CLI deve estar instalado a configurado com as chaves AWS Secret Access Key e AWS Access Key, elas podem ser encontradas no console da AWS.

## Utilização

```bash
java -jar s3.jar  \
	--output=<Output path> \
	--bucket=<AWS S3 Bucket name> \
	--prefix=<(Optional) Object prefix> \
	--field=<Fields of ouput file> \
	--start_date=<(Optional) Object modified date since as YYYY-MM-DD> \
	--end_date=<(Optional) Object modified date to as YYYY-MM-DD>  \ 
	--filter=<(Optional) Regexp expression to filter to select desired objects> \ 
	--partition=<(Optional) Partition, divided by + if has more than one field> \
	--key=<(Optional) Unique key, divided by + if has more than one field> \
	--delimiter=<(Optional) File delimiter. ';' as default> \
	[--no_header] <(Optional) Identifies if the input file has header>
```

Considerando que um bucket tenha a seguinte estrutura:
```bash		
bucket-name
	folder
		subfolder
			object_1.csv
			object_2.csv
```
Caso queira fazer download de todos os arquivos, os parâmetros **bucket** e **prefix** devem ser configurados da seguinte forma:

```bash
java -jar s3.jar  \
	--output="..." \
	--bucket="bucket-name" \
	--prefix="folder/subfolder/" \
	--start_date="..." \
	--end_date="..."  \ 
	--field="..." \
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
