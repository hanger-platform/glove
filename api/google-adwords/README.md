# Google Adwords Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados da api do Google Adwords 

## How it works

O **Google Adwords Extractor** permite a extração de relatórios do Google Adwords de todas as contas abaixo de uma **AdWords manager account**. Os relatórios suportado, bem como métricas e atributos estão disponíveis em (https://developers.google.com/adwords/api/docs/appendix/reports#available-reports).

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **google-adwords** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **google-adwords.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Se você não possui um client ID e secret para o Adwords, crie um projeto no console do desenvolvedor. Consulte o link a seguir para obter mais informações:: https://github.com/googleads/googleads-java-lib/wiki/Using-OAuth2.0

* Crie um arquivo com as seguintes informações sobre seu acesso ao serviço, este será o seu **credentials file**:

```
api.adwords.refreshToken=<refreshToken>
api.adwords.clientId=<clientId>
api.adwords.clientSecret=<clientSecret>
api.adwords.clientCustomerId=<AdWords manager account ID>
api.adwords.developerToken=<developerToken>
api.adwords.isPartialFailure=false
```

## Utilização

```bash
java -jar google-adwords.jar  \
	--credentials=<Credentials file>  \
	--type=<Adwords report type> 
	--field=<Report fields>
	--start_date=<Start date formated as YYYYMMDD> 
	--end_date=<End date formated as YYYYMMDD> 
	--output=<Output file>
	--zero_impression=<(Optional) Include Zero Impressions. false as default>
	--threads=<Optional)  Number customer reports being generated in parallel. 5 as default>
	--page_size=<Page size. 500 as default>
	--partition=<(Optional)  Partition, divided by + if has more than one>
	--key=<(Optional) Unique key, divided by + if has more than one>
```

## Exemplo

```bash
java -jar google-adwords.jar  \
	--credentials="/home/valdiney/ads.properties" 
	--type="ADGROUP_PERFORMANCE_REPORT" 
	--field="CampaignId+AdGroupId+Impressions+Clicks+Cost" 
	--start_date="20190921" 
	--end_date="20190921" 
	--output="/tmp/ADGROUP_PERFORMANCE_REPORT.csv"
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
