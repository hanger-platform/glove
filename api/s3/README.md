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

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **s3** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **s3.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* O AWS CLI deve estar instalado a configurado com as chaves AWS Secret Access Key e AWS Access Key, elas podem ser encontradas no console da AWS.

## Utilização

```bash
java -jar s3.jar  \
	--output=<Output path> \
	--bucket=<AWS S3 Bucket> \
	--prefix=<Files path, no need to write bucket here> \
	--start_date=<Start date> \
	--end_date=<End date>  \ 
	--field=<Fields of ouput file> \
	--partition=<(Optional)  Partition, divided by + if has more than one field> \
	--key=<(Optional) Unique key, divided by + if has more than one field> \
	--delimiter=<(Optional) File delimiter; ';' as default>
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
