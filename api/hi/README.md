

# Hi Platform API extractor
### Extrator de dados de interações em redes sociais da API da Hi Platform. 

## How it works

A API da Hi Platform possibilita a extração de dados sobre monitoramento e atendimento de clientes nas redes sociais, fornecendo insights para tomar decisões de marketing e negócios. O extrator é baseado na V3 da API, e a documentação dos endpoins e campos estão disponíveis neste documento: https://plataforma.seekr.com.br/api_doc/v3#projects. 

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **hi** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **hi.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as chaves fornecidas pela HI Platform, este será o seu **credentials file**:

```
{
	"key":"<key>",
	"secretKey":"<secretKey>"
}

```

## Utilização

```bash
java -jar hi.jar  \
	--credentials=<Credentials file>  \
	--output=<Output file> 
	--field=<Fields to be retrieved from an endpoint in JsonPath fashion>
	--endpoint=<Endpoint name>
	--parameters=<(Optional) Endpoint parameters>
	--object=<(Optional) Json object>
	--paginate <Identifies if the endpoint has pagination>
	--partition=<(Optional) Partition, divided by + if has more than one field>
	--key=<(Optional) Unique key, divided by + if has more than one field>
```

Em algumas situações é necessário utilizar configurações avançadas para extrair os dados corretamente. Considerando o endpoint **search_results**, é necessário que sejam passados parâmetros para o endpoint e, posteriormente, selecionados valores aninhados no JSON retornado pela API, como é o caso dos dados de **locale**:

```javascript
  {
  "response":  {
	  "status":  "200 OK",
	  "code":  200
  },
  "total":  166,
  "search_results":  [
  {
  "id":  33221,
...
  "locale":  {
	  "latitude":  1.23456,
	  "longitude":  -2.34567,
	  "country":  "Brazil",
	  "country_short":  "BR",
	  "state":  "São Paulo",
	  "state_short":  "SP",
	  "city":  "São Paulo",
	  "country_id":  1,
	  "state_id":  2,
	  "city_id":  3
  }...]
```
Nesta situação, este seria uma configuração possível:
```bash
java -jar hi.jar  \
	--credentials="/home/etl/credentials/hi.json"  \
	--output="/tmp/hi/search_results.csv"
	--field="id+locale.latitude+locale.longitude"
	--endpoint="search_results"
	--parameters='{"project_id":"46117", "from_date":"2021-01-10", "to_date":"2021-01-10"}'
	--object="search_results"
	--paginate
```
Para recuperar campos aninhados no JSON de retorno é necessário utilizar a sintaxe do JSONPath, ou seja, montando o caminho dos campos separados por ponto: Ex.: **locale.latitude**

> **SEMPRE** deve ser retornado apenas um valor por campo especificado no parâmetro field, qualquer sintaxe diferente de ponto resultará em erro.

Como pode ser observado, qualquer parâmetro pode ser fornecido para API por meio do parâmetro **parameters** do extrator, que, como pode ser observado, recebe um JSON contendo os parâmetros desejados. 

> A única forma de identificar se um endpoint possui paginação é verificando se na documentação do endpoint existe o parâmetro page, nestes caso sempre deve ser fornecido o parâmetro --paginate para o extrator, caso contrário ele deve ser removido. 

Em outras situações, como no caso do endpoint **tickets/health**, o object (ou lista contendo os valores desejados) não é disponibilizado no JSON:

```javascript
  {
	  "response":  {
	  "status":  "200 OK",
	  "code":  200
  },
	  "positive":  79,
	  "neutral":  79,
	  "negative":  41,
	  "total":  199,
	  "health":  0.7429467084639498
  }
```

Neste caso, deve ser informado o valor * no parâmetro **object** do extrator:

```bash
java -jar hi.jar  \
	--credentials="/home/etl/credentials/hi.json"  \
	--output="/tmp/hi/tickets_health.csv"
	--field="positive+neutral+negative+total+health"
	--endpoint="tickets/health"
	--parameters='{"project_id":"46117"}'
	--object="*"
```

> Não é possível extrair dados de endpoint que recebam ID como parâmetro diretamente no nome. 

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
