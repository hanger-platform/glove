

# Google Pagespeed Insights extractor
### Extrator de dados da ferramenta Google PageSpeed Insights.

## How it works

O PageSpeed Insights informa o desempenho real de uma página para dispositivos móveis e computadores. Além disso, ele sugere maneiras de aprimorar a página. Esse extrator de dados tem como objetivo extrair as informações dessa ferramenta através da geração de uma arquivo csv como saída.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Geração da chave no Google Cloud (https://developers.google.com/speed/docs/insights/v5/get-started?hl=pt-br)

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **google-pagespeed-insights** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **google-pagespeed-insights.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com a chavesfornecidas pela Google Cloud, este será o seu **credentials file** :

```
{
	"key":"<key>",
}

```

## Utilização

```bash
java -jar google-pagespeed-insights.jar  \
	--credentials=<Credentials file>  \
	--output=<Output file> 
	--field=<Fields to be retrieved in JsonPath fashion>
	--url=<The URL to fetch and analyze>
	--parameters=<(Optional) Endpoint parameters>
	--object=<(Optional) Json object>
	--partition=<(Optional) Partition, divided by + if has more than one field>
	--key=<(Optional) Unique key, divided by + if has more than one field>	
```

Os parametros disponíveis para utilização podem ser consultados no link: https://developers.google.com/speed/docs/insights/rest/v5/pagespeedapi/runpagespeed?apix_params=%7B%22url%22%3A%22http%3A%2F%2Fdafiti.com.br%22%2C%22category%22%3A%5B%22SEO%22%5D%2C%22strategy%22%3A%22DESKTOP%22%7D

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
