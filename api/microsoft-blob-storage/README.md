# Microsoft Blob Storage Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados Microsoft Azure

## How it works

O **Microsoft Blob Storage Extractor** permite a extração de dados armazenados no Azure da Microsoft.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **microsoft-blob-storage** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **microsoft-blob-storage.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÃO

* Crie um arquivo (json) com as seguintes informações sobre seu acesso ao servidor Microsoft Azure, este será o seu **credentials file**:

```
{
	"accountName":"<accountName>",
	"accountKey":"<accountKey>"
}
```

## Utilização

```bash
java -jar microsoft-blob-storage.jar  \
	--credentials=<credentials file>  \
	--container=<blob container> \
	--output=<output path> \
	--field=<fields to extracted from raw file, divided by +> \
	--start_date=<start date> \
	--end_date=<end date>  \
	--delimiter=<(optional) file delimiter; ';' as default> \
	--prefix=<(optional) prefix; null as default> \
	--partition=<(optional) partition, divided by + if has more than one field> \
	--key=<(optional) unique key, divided by + if has more than one field> \
	--timeout=<(optional) API timeout in minutes; '60' as default> \
	--encode=<(optional) encode file> \
	--properties=<(optional) reader properties>
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
