
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

- Acesse o diretório no qual os fontes do **blue** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **blue.jar** será gerado no subdiretório **_target_**.

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
	--app=<App name supplied by Blue> \
	--output=<Output file> \
	--field=<Fields to be retrieved from an endpoint in JsonPath fashion> \
	--campaign=<(Optional) Campaign ID> \
	--date=<(Optional) Campaign Date> \
	--parameters=<(Optional) Endpoint parameters in a JSON fashion> \
	--partition=<(Optional) Partition, divided by + if has more than one field> 
```

## Exemplos

A API fornece apenas 3 endpoints, sendo eles:

##### Lista de campanhas

- http://getblue.io/rest/report/api/campaigns

```javascript
[
    {
        "CAMPANIGNID": 0,
        "TRACKING": "blue",
        "TYPE": "REV",
        "CAMPAIGN": "Dafiti"
    },...
]
```

##### Resultado das campanhas

- http://getblue.io/rest/report/api/campaign/<campaignID\>
- http://getblue.io/rest/report/api/campaign/<campaignID\>?date=YYYY-MM-DD

```javascript
[
    {
        "DATE": "2021-08-12",
        "MEDIACOST": 0.00,
        "CAMPANIGNID": 0,
        "ACTIONS": 0,
        "RESULTSURL": "http://getblue.io/rest/report/api/campaign/0",
        "CLICKS": 0,
        "IMPRESSIONS": 0
    },...
]
```

Desta forma, para extração de todas as campanhas, a seguinte configuração poderia ser utilizada: 

```bash
java -jar /<path>/blue.jar \
  --credentials="/home/etl/credentials/blue.json"  \
  --app="REPORTS_API" \
  --output="/tmp/blue/campaign.csv" \
  --field="CAMPANIGNID+TRACKING+TYPE+CAMPAIGN" 
```

Para obter os resultados de uma campanha:

```bash
java -jar /<path>/blue.jar \
  --credentials="/home/etl/credentials/blue.json"  \
  --app="REPORTS_API" \
  --campaign="0" \
  --output="/tmp/blue/campaign.csv" \
  --field="CAMPANIGNID+DATE+MEDIACOST+ACTIONS+CLICKS+IMPRESSIONS" 
```

Para obter o resultado de uma campanha para uma data específica:


```bash
java -jar /<path>/blue.jar \
  --credentials="/home/etl/credentials/blue.json"  \
  --app="REPORTS_API" \
  --campaign="0" \
  --date="2021-07-22" \
  --output="/tmp/blue/campaign.csv" \
  --field="CAMPANIGNID+DATE+MEDIACOST+ACTIONS+CLICKS+IMPRESSIONS" 
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
