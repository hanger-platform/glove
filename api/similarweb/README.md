

# SimilarWeb API Extractor
### Extrator de dados da API SimilarWeb.

## How it works

O SimilarWeb API Extractor permite a extração de dados da API Rest disponibilizada pelo SimilarWeb. O extrator é baseado na V1 da API, e a documentação dos endpoints e campos estão disponíveis neste documento: https://documenter.getpostman.com/view/5388671/RzfcNs8W

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **similarweb** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **similarweb.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as chaves fornecidas pela API SimilarWeb, este será o seu **credentials file**:

```
{
	"api_key":"<apiKey>"
}
```

## Utilização

```bash
java -jar similarweb.jar  \
	--credentials=<Arquivo com as credenciais> \
	--output=<Caminho onde o arquivo será salvo> \
	--field=<Campos que serão gerados no arquivo de saída> \
	--endpoint=<Endpoint que será extraído, exemplo: "/v1/website/bbc.com/traffic-sources/paid-search"> \
	--object=<Objeto JSON, exemplo: "search"> \
	--parameters=<(Opcional) JSON com os parâmetros, exemplo: {"country":"br","start_date":"2021-01","end_date":"2021-03"}> \
	--partition=<(Opcional) Partição, dividos por + quando for mais de um> \
	--key=<(Opcional) Chave única, dividos por + quando for mais de um>

```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
