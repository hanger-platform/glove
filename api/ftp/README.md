# FTP Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados armazenados em servidores FTP 

## How it works

O **FTP Extractor** permite a extração de dados de arquivos armazendos em servidores FTP.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **ftp** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **ftp.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as seguintes informações sobre seu acesso ao servidor FTP, este será o seu **credentials file**:

```
{
	"user":"<username>",
	"password":"<password>",
	"host":"<host>",
	"port":"<port>"
}
```

## Utilização

```bash
java -jar ftp.jar  \
	--credentials=<Credentials file>  \
	--output=<Output path> \
	--directory=<FTP directory> \
	--output=<Output file> \
	--start_date=<Start date>
	--end_date=<End date>  \
	--delimiter=<(Optional) File delimiter; ';' as default> \
	--pattern=<(Optional) FTP file pattern; *.csv as default> \
	--partition=<(Optional)  Partition, divided by + if has more than one field> \
	--key=<(Optional) Unique key, divided by + if has more than one field> \
	--field=<Fields to extracted from raw file, divided by +> \
	--passive=<(Optional) Define the connection mode (passive or active). Default is true (passive)>
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
