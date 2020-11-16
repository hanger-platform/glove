# ECT Object Tracking [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados armazenados em servidores SFTP 

## How it works

O **ECT Object Tracking** permite a extração de dados o rastreamento de objetos da ECT.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **ect-object-tracking** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **ect-object-tracking.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as seguintes informações sobre seu acesso ao serviço de SRO da ECT, este será o seu **credentials file**:

```
{
	"user":"<username>",
	"password":"<password>"
}
```

## Utilização

```bash
java -jar ect-object-tracking.jar  \
	--credentials=<Credentials file>  \
	--object=<Post authorization numbers divided by + or in a file (should by prefixed with file: and have an object per line)> \
	--output=<Output path> \
	--type=<(Optional) L: list of objects. O server will make the query individual of each informed identifier or F: range of objects; L as default> \
	--result=<(Optional) L: All will be returned the events of the object or U: will be returned only object's last event; L as default> \
	--language=<(Optional) 101: Will be returned all events in the Portuguese language 102: Will be returned all events in the English language; 101 as default>
	--partition=<(Optional)  Partition field; ::dateformat(event_date,dd/MM/yyyy HH:mm,yyyyMM) as default> \
	--key=<(Optional) Unique key; ::concat([number,event_type,::concat([number,event_type,event_code,event_status,::dateformat(event_date,dd/MM/yyyy HH:mm,yyyyMMddHHmm)],|) as default> \
	--thread=<(Optional) Threads; 5 as default> \
	--chunk=<(Optional) Objects to be retrieved in each thread; 1000 as default>
```

## Metadata

A api sempre retorna um arquivo csv contendo os seguintes campos:

| Field | Description |
|--|--|
| partition_field | Campo interno |
| custom_primary_key | Campo interno |
| etl_load_date | Data de geração dos dados |
| number | Número do objeto |
| event_type | Tipo do evento |
| event_code | CEP da unidade ECT |
| event_status | Status do evento |
| event_description | Descrição do evento |
| event_date | Data e hora do evento |
| initials | Sigla do objeto solicitado |
| name | Nome do objeto solicitado |
| event_city | Cidade onde ocorreu o evento |
| event_place | Local onde ocorreu o evento |
| event_state | Unidade da Federação |

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
