# Google Ads Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados da api do Google Ads

## How it works

O **Google Ads Extractor** permite a extração de dados diretamente da plataforma do Google Ads através de contas indivíduais ou todas as contas abaixo de uma **Ads manager account**. Os dados que podem ser retornados através desse extrator, bem como métricas e atributos, podem ser encontrados na documentação (https://developers.google.com/google-ads/api/fields/v6/overview).

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **google-ads** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **google-ads.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Se você não possui um client ID e secret para o Ads, crie um projeto no console do desenvolvedor. Consulte o link a seguir para obter mais informações: (https://developers.google.com/google-ads/api/docs/first-call/overview)

* Crie um arquivo com as seguintes informações sobre seu acesso ao serviço, este será o seu **credentials file**:

```
api.googleads.refreshToken=<refreshToken>
api.googleads.clientId=<clientId>
api.googleads.clientSecret=<clientSecret>
api.googleads.clientCustomerId=<AdWords manager account ID>
api.googleads.developerToken=<developerToken>
api.googleads.isPartialFailure=false
```

## Utilização

```bash
java -jar google-adwords.jar  \
	--credentials=<Credentials file>  \
	--type=<Ads resource type> \
	--output=<Output file or path> \
	--field=<Resource fields> \
	--manager=<Manager account> \	
	--customer=<Customer ID of the customer being queried> \
	--filter=<(Optional) Filter of query> \
	--partition=<(Optional) Partition, divided by + if has more than one> \
	--key=<(Optional) Unique key, divided by + if has more than one> \	
	--debug=<(Optional) Debug mode. false as default> \
```

- Para saber os resources disponíveis, consulte (https://developers.google.com/google-ads/api/fields/v6/overview)
- Em caso de dúvidas sobre filtros ou campos, pode utilizar a seguinte ferramenta como apoio (https://developers.google.com/google-ads/api/docs/query/interactive-gaql-builder?hl=en)

## Exemplo

```bash
java -jar google-ads.jar 
	--credentials="/home/helio.leal/.google/google_ads_api/ads.properties" \
	--output="/tmp/google_ads/campaign/" \
	--manager="12345678" \ 
	--field="campaign.id,campaign.name,campaign.status,segments.date,segments.device,metrics.impressions,metrics.clicks,metrics.ctr,metrics.average_cpc,metrics.cost_micros" \
	--key="::checksum()" \
	--partition="::fixed(FULL)" \
	--type="campaign" \
	--filter="segments.date DURING LAST_14_DAYS" \
	--customer="12345678"	
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
