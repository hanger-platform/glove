# ECT Reverse Logistics Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados armazenados em servidores SFTP 

## How it works

O **ECT Reverse Logistics per date Extractor** permite a extração de dados sobre logística reversa dos correios por data. 

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **ect-reverse-logistics** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **ect-reverse-logistics-per-date.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as seguintes informações sobre seu acesso ao serviço de logística reversa da ECT, este será o seu **credentials file**:

```
{
	"user":"<username>",
	"password":"<password>",
	"administrativeCode":"<administrativeCode>"
}
```

## Utilização

```bash
java -jar ect-reverse-logistics.jar  \
	--credentials=<Credentials file>  \
	--start_date=<Start date> \
	--start_date=<End date> \
	--output=<Output path> \
	--searchType=<(Optional) Search Type: A or C; A as default> \
	--partition=<(Optional)  Partition field; ::dateformat(history_update_date,dd-MM-yyyy,yyyyMM) as default> \
	--key=<(Optional) Unique key; ::concat([administrative_code,order_type,order_number,history_status],|) as default> \
	--thread=<(Optional) Threads; 5 as default> \
	--chunk=<(Optional) Authorization to be retrieved in each thread; 1000 as default>
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
