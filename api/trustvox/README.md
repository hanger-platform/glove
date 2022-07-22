

# Trustvox API extractor
### Extrator de dados de coleta e exibição de reviews dos clientes.

## How it works

Com a API da Trustvox é possível consumir dados através de uma REST API e efetuar a captura de dados como revisões de produtos, revisões da empresa etc. Os endpoints disponíveis na API estão na seguinte documentação: https://developers.trustvox.com.br/#intro

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Token gerado na plataforma da trustvox: [Link](https://developers.trustvox.com.br/#041e60ee-476e-4252-89eb-bdec10591c0b)

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **trustvox** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **trustvox.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com o token fornecido pela Trustvox, este será o seu **credentials file**:

```
{
	"token":"<token>"
}
```

## Utilização

```bash
java -jar trustvox.jar  \
	--credentials=<Credentials file>  \
	--output=<Output file> 
	--field=<Fields to be retrieved from an endpoint in JsonPath fashion>
	--endpoint=<Endpoint uri>
	--parameters=<(Optional) Endpoint parameters>
	--object=<(Optional) Json object>
	--paginate <Identifies if the endpoint has pagination>
	--partition=<(Optional) Partition, divided by + if has more than one field>
	--key=<(Optional) Unique key, divided by + if has more than one field>
```

#### Exemplos

Retornar dados de avaliações de produtos pelos clientes, endpoint: https://developers.trustvox.com.br/#b3b92acb-91ff-4d82-be24-3e084b35af32

```bash

java -jar /home/etl/glove/extractor/lib/trustvox.jar  \
	--credentials="/credentials_path/trustvox.json" \
	--output="/tmp/trustvox/reviews/reviews.csv" \
	--field="id+rate+text+recommends+created_at+product.id+product.name+product.price+client.email+order.delivery_date+order.order_id" \
	--endpoint="stores/{store_id}/opinions" \
	--object='items' \
	--parameters='{"by_min_created_at":"2022-06-01", "by_max_created_at":"2022-06-02"}' \
	--partition="::dateformat(created_at,yyyy-MM-dd,yyyyMMdd)" \
	--key="id"
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
