
# Facebook Ad Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados de Ad do Facebook (https://developers.facebook.com/docs/marketing-api/reference/v9.0)

## How it works

O **Facebook Ad Extractor** possibilita a extração de dados de AdCampaigns, AdSets, Ads, AdsCreative e AdsInsights.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **facebook-ad** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **facebook-ad.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as seguintes informações sobre seu acesso à API de Marketing do Facebook, este será o seu **credentials file**:

```
{
	"client-secret":"<client-secret>",
	"token":"<token>"
}
```

## Utilização

```bash
java -jar facebook-ad.jar \
	--credentials=<Credentials file> \
	--output=<Output full qualified file name> \
	--account=<Facebook accounts divided by +> \
	--report=<Identifies the report to extract. Possible values: AdCampaigns, AdSets, Ads e AdsInsights> \
	--start_date=<Start date, today as default> \
	--end_date=<End date, today as default> \
	--partition=<(Optional)  Partition field> \
	--key=<(Optional) Unique key> \
	--attribute=<(Optional) Facebook Report fields, divided by + if has more than one field> \
	--breakdowns=<(Optional) Breakdowns of report, divided by + if has more than one field> \
	--filtering=<(Optional) Report filters in the format: [{'field':'<field>','operator':'<operator>','value':<value>}]> \
	--field=<(Optional) Custom fields where MITT functions are welcome, divided by + if has more than one field>
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
