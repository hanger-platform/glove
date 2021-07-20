

# Tiktok Ads Extractor
### Extrator de dados de anúncios automatizados do TikTok Ads Manager.

## How it works

A TikTok Marketing API permite interagir diretamente com a plataforma TiTok Ads Manager para gerencimento e análise de anúncios automatizados. A documentação da API que fornece inclusive os endpoints(path), métricas e dimensões estão disponíveis neste documento: https://ads.tiktok.com/marketing_api/docs

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **tiktok-ads** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **tiktok-ads.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com o token fornecido pelo plataforma TikTok Marketing API, este será o seu **credentials file**:

```
{
	"access_token":"<token>"
}
```

Caso o token não esteja mais ativo, é necessário gerar um novo token. Siga as instruções deste documento: https://ads.tiktok.com/marketing_api/docs?rid=y3cel02pcw&id=1701890912382977

## Utilização

```bash
java -jar tiktok-ads.jar  \
	--credentials=<Credentials file>  \
	--output=<Output file> 
	--field=<Fields to be retrieved from an endpoint in JsonPath fashion>
	--path=<Request address>
	--parameters=<Request parameters. Json format>
	--partition=<(Optional) Partition, divided by + if has more than one field>
	--key=<(Optional) Unique key, divided by + if has more than one field>
```

##### EXEMPLO

Ao fazer uma chamada os dados de retorno são sempre no formato json:

```javascript
{
    "message": "OK",
    "code": 0,
    "data": {
        "page_info": {
            "total_number": 2,
            "page": 1,
            "page_size": 200,
            "total_page": 1
        },
        "list": [
            {
                "metrics": {
                    "ad_name": "20200923012039",
                    "clicks": "116.0",
                    "spend": "76.73",
                    "impressions": "10505.0",
                    "ctr": "1.1"
                },
                "dimensions": {
                    "stat_time_day": "2020-10-17 00:00:00",
                    "ad_id": 1678604629756978
                }
            },
            {
                "metrics": {
                    "ad_name": "20200923012039",
                    "clicks": "134.0",
                    "spend": "106.4",
                    "impressions": "12003.0",
                    "ctr": "1.12"
                },
                "dimensions": {
                    "stat_time_day": "2020-10-16 00:00:00",
                    "ad_id": 1678604629756978
                }
            }
        ]
    },
    "request_id": "202011250924260101151531911200759C"
}
```
Nesta situação, este seria uma configuração possível:
```bash
java -jar tiktok-ads.jar  \
	--credentials="/home/etl/credentials/tiktok.json"  \
	--output="/tmp/tiktok/ad.csv" \
	--path="/open_api/v1.2/reports/integrated/get/" \
	--field="dimensions.stat_time_day+dimensions.ad_id+metrics.ad_name+metrics.clicks+metrics.spend+metrics.impressions+metrics.ctr" \
	--parameters='{"metrics":["ad_name","clicks","spend","impressions","ctr"],"data_level":"AUCTION_AD","start_date":"2020-10-10","end_date":"2020-10-20","advertiser_id":"xxx","service_type":"AUCTION","report_type":"BASIC","dimensions":["ad_id","stat_time_day"]}' \
	--partition="::dateformat(dimensions.stat_time_day,yyyy-MM-dd HH:mm:ss,yyyyMMdd)" \
	--key="::checksum()"
```
Note que no parâmetro **field**, é necessário informar o campo utilizando a sintaxe JSONPath, ou seja, montando o caminho dos campos separados por ponto: Ex.: **dimensions.ad_id**

> **SEMPRE** deve ser retornado apenas um valor por campo especificado no parâmetro field, qualquer sintaxe diferente de ponto resultará em erro.

Como pode ser observado, qualquer parâmetro da API pode ser informado pelo parâmetro **parameters** do extrator no qual deve ser em formato json.

Não é necessário informar o parâmetro **page** da API, pois o extrator utiliza esse parâmetro de uma forma dinâmica para iteração de todas páginas encontradas.

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.