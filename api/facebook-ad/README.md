# Facebook Ad Extractor [![GitHub license](https://img.shields.io/github/license/dafiti/causalimpact.svg)](https://bitbucket.org/dafiti/bi_dafiti_group_nick/src/master/license)
### Extrator de dados de Ad do Facebook (https://developers.facebook.com/docs/marketing-api/reference/ad-campaign-group)

## How it works

O **Facebook Ad Extractor** possibilita a extração de dados de AdCampaigns, AdSets, Ads e AdsInsights.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **facebook-ad** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **facebook-ad.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as seguintes informações sobre seu acesso ao serviço de SRO da ECT, este será o seu **credentials file**:

```
{
	"client-secret":"<client-secret>",
	"token":"<token>"
}
```

## Utilização

```bash
java -jar facebook-ad.jar  \
	--credentials=<Credentials file>  \
	--object=<Post authorization numbers divided by + or in a file> \
	--output=<Output full qualified file name> \
	--account=<Facebook account, divided by + if has more than one> \
	--report=<Identifies the report to extract. Possible values: AdCampaigns, AdSets, Ads e AdsInsights> \
	--start_date=<Start date, today as default> \
	--end_date=<End date, today as default> \
	--partition=<(Optional)  Partition field> \
	--key=<(Optional) Unique key> 
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
