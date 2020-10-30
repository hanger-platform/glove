
# Facebook Ad Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados da API Analytics da Criteo.

## How it works

O **Criteo Analytics** possibilita a extração de dados da API de Analytics da Criteo,as possíveis combinações de dimensões e métrica podem ser consultadas na documentação da API, disponível neste link: [Statistics v1](https://developers.criteo.com/marketing-solutions/v2020/docs/statistics-v1)

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **criteo-analytics** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **criteo-analytics.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as seguintes informações sobre seu acesso à API de Marketing do Facebook, este será o seu **credentials file**:

```
{
	"client_id":"<client_id>",
	"client_secret":"<client_secret>"
}
```

## Utilização

```bash
java -jar facebook-ad.jar \
	--credentials="<Credentials file>" \
	--output="<Output file>" \
	--account="<Advertiser account ID>" \
	--start_date="<Start date>" \
	--end_date="<End date>" \
	--dimensions=<Report dimensions, divided by + if has more than one field> \
	--metrics=<Report metrics, divided by + if has more than one field> \
	--field=<Fields to be extracted> \
	--currency="[(Optional) Currency code.  Default is BRL]" \
	--partition=[(Optional)  Partition field] \
	--key=[(Optional) Unique key] \
	--debug
```

> Para saber os campos que são exportados pela API e que podem ser utilizados no parâmetro fields, recomendamos o extrator seja executado em modo de debug inicialmente. 

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
