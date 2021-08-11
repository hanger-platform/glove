
# Blue API Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados de campanhas da API da Blue 

## How it works

O **Blue API Extractor** permite a extração de dados de campanhas da API disponibilizada pela Blue.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **braze** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **braze.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com o tokenUuid disponibilizado pela Blue, este será o seu **credentials file**:

```
{
	"tokenUuid":"<token>"
}
```

## Utilização

```bash
java -jar blue.jar  \
	--credentials=<Credentials file>  \
	--output=<Output file> \
	--field=<Fields to be retrieved from an endpoint in JsonPath fashion> \
  --app=<App name> \
  --parameters=<(Optional) Endpoint parameters> \
  --paginate=<(Optional) Identifies if the endpoint has pagination> \
	--partition=<(Optional) Partition, divided by + if has more than one field> 
```

## Exemplos

##### Exemplo de tipo 'list detail' (Padrão)

```bash
java -jar /<path>/blue.jar \
  --credentials="/home/etl/credentials/blue.json"  \
  --output=/tmp/blue/search_results.csv \
  --field="CAMPANIGNID+TRACKING+TYPE+CAMPAIGN" \
  --app="REPORTS_API" \
  --parameters=<(Optional) Endpoint parameters> \
  --partition=<(Optional) Partition, divided by + if has more than one field> 
  --paginate 
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
