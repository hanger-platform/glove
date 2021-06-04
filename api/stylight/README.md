# Stylight API Extractor
### Extrator de dados da API Stylight Partner Center.

## How it works

O Stylight API Extractor possibilita a extração de dados da API do Stylight Partner Center que disponibiliza o relatório de **cliques e comissões**, onde pode ser monitorado a performance de cliques e comissão (custo) na plataforma.
A explicação completa da api está disponível no link: https://partner.stylight.net/docs/stylight-partner-center-api-documentation.html

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **stylight** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **stylight.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÃO

* Crie um arquivo com as credencias usadas para acessar o portal de parceiros da Stylight, este será o seu **credentials file**:

```
{
	"username":"<username>",
	"password":"<password>"
}
```

## Utilização

```bash
java -jar stylight.jar  \
--credentials=<Arquivo com as credenciais> \
--output=<Caminho onde o arquivo será salvo> \
--field=<Campos que serão gerados no arquivo de saída> \
--endpoint=<Endpoint que será extraído, exemplo: https://partner.stylight.net/api/v1/report?from_date=2021-05-26&to_date=2021-05-26> \
--key=<(Opcional) Chave única, dividos por + quando for mais de um> \
--partition=<(Opcional) Partição, dividos por + quando for mais de um>
```

##### EXEMPLO 1

```bash
java -jar stylight.jar  \
	--credentials="/home/etl/credentials/stylight.json" \
	--output="/tmp/stylight/commision_report.csv" \
	--field="vertical+device_type+date+ppc_clickout_count+average_ppc_price+estimated_overall_commission_per_overall_clickout+ppc_commission" \
	--endpoint="https://partner.stylight.net/api/v1/report?from_date=2021-05-26&to_date=2021-05-26&group_by=device_type&group_by=date&group_by=vertical&currency=BRL" \
	--key="::checksum()" \
	--partition="::dateformat(date,yyyy-MM-dd,yyyyMM)"
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
