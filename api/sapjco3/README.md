# SAPJCO3 Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados de sistemas SAP. 

## How it works

O **SAPJCO3 Extractor** é uma ferramenta que possibilita extrair dados de sistemas SAP (BW, ECC, EWM) no padrão do MITT. Esse extrator utiliza a biblioteca SAPJCO3 que tem como recurso o protocolo RFC.
Para informações mais detalhadas, consulte: https://sap.github.io/cloud-sdk/docs/java/features/bapi-and-rfc/bapi-and-rfc-overview/

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Download da biblioteca SAPJCO3 direto do site da SAP, em caso de dúvidas consulte: https://sap.github.io/cloud-sdk/docs/java/features/bapi-and-rfc/bapi-and-rfc-overview/
- A biblioteca deve estar instalada na classpath do java
	- Linux:
		- Arquivo libsapjco3.so 
	- Windows:
		- Arquivo sapjco3.dll


##### CONSTRUÇÃO

- Utilizando o [Maven](https://maven.apache.org/): 
    - Acesse o diretório no qual os fontes do **_SAPJCO3_ **se localizam.
    - Digite o comando _**mvn package**_.
    - O arquivo **sapjco3.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as credencias para acessar o ambiente SAP, este será o seu **credentials file**:

```
{
	"host":"<IP do servidor SAP>",
	"sysnr":"<Número do sistema para se conectar>",
	"client":"<ID Mandante do ambiente>",
	"user":"<Usuário>",
	"passwd":"<Senha>",
	"lang":"<Idioma do sistemas. exemplo: PT>"
}

```

## Utilização

```bash
java -jar sapjco3.jar \
  --credentials="<Identifica o caminho onde o arquivo secreto com as credenciais está localizado>" \
  --output="<Identifica o caminho e nome do arquivo que será gerado>" \
  --function="<Identifica o nome da função a ser chamada no sistema SAP, exemplo: RFC_READ_TABLE>" \
  --table="<Nome da tabela a ser retornada>" \
  --field="<Opcional, Identifica o nome dos campos que serão extraídos, senão for passado o processo tentará pegar os campos automaticamente>" \
  --where="<Opcional, Identifca a condição where>" \
  --partition="<Opcional, Identifica o campo que será utilizado para particionamento dos dados>" \
  --key="<Opcional, Identifica a chave primária>" \
  --input_delimiter="<Opcional, Delimitador do resultado vindo da função chamada; '|' é o padrão>" \
  --row_count="<Opcional, quantidade de registros que serão trazidos por vez; '0' é o padrão e significar trazer tudo de uma vez>" \
  --row_skips="<Opcional, Começa a trazer registros a partir de qual índice; '0' é o padrão>" \
  --delimiter="<Opcional, esse é o delimitador usado no mitt; '|' é o padrão>" \
  --debug="Opcional, Identifica se é modo debug ou não; 'false' é o padrão>"  
```

## Exemplos

##### Chamada simples

```bash
java -jar /home/etl/lib/sapjco3.jar \
  --credentials="/<credentials_path>/<credentials_file>.json" \
  --output="/tmp/sapjco3/bw/ZBW000016/ZBW000016.csv" \
  --function="ZRFC_READ_TABLE" \
  --table="ZBW000016" \
  --key='::checksum()' \
  --partition='::fixed(FULL)'
```
* No exemplo acima não especificamos os campos que desejamos de retorno, desta maneira, todos os campos serão extraídos para o arquivo de saída.

#### Chamada com filtro e especificação de campos

```bash
java -jar /home/etl/lib/sapjco3.jar \
  --credentials="/<credentials_path>/<credentials_file>.json" \
  --output="/tmp/sapjco3/bw/ZBW000029/ZBW000029.csv" \
  --function="ZRFC_READ_TABLE" \
  --field="MANDT+DELIV_NUMB+AMOUNT+CURRENCY+MOTIVO+/BIC/ZUPDATED" \
  --table="ZBW000029" \
  --where='AMOUNT > 1 OR DELIV_NUMB = '"'"'8043143930'"'"'' \
  --key='::checksum()' \
  --partition='::fixed(FULL)'
```
* No exemplo acima especificamos os campos que queremos de retorno no parâmetro _field_ e colocamos uma **condição** para a extração dos dados no parâmetro _where_

#### Chamada com filtro e quantidade de linhas por 'lote'

```bash
java -jar /home/etl/lib/sapjco3.jar \
  --credentials="/<credentials_path>/<credentials_file>.json" \
  --output="/tmp/sapjco3/bw/B1H/ASD_D1100/BI0_ASD_D1100.csv" \
  --function="ZRFC_READ_TABLE" \
  --table="/B1H/ASD_D1100" \
  --key='::checksum()' \
  --where=' /BIC/ZUPDATED >= '"'"'020210601000000'"'"' ' \
  --partition="::dateformat(/BIC/ZUPDATED,yyyyMMdd,yyyy)" \
  --debug="true" \
  --row_count=300000
```

* No exemplo acima especificamos uma **condição** para a extração dos dados no parâmetro _where_ e no parâmetro **row_count** colocamos o número 300.000 que será responsável por fazer a extração dos dados por partes de 300.000, ou seja, irá trazer os dados de 0 a 300.000, depois de 300.000 até 600.000 até que atinja o tamanho total de registros.
* Também colocamos o parâmetro **debug** com o valor true para efetuarmos um acompanhamento do que está acontecendo no processo, dessa maneira conseguimos traquear onde o processo está através de um log bem detalhado. Esse log apresenta a quantidade de registros por chamada e também é possível acompanhar o tempo de chamada de ida e vinda do servidor SAP e também é possível acompanhar o tempo de escrita do mitt.

#### Exemplo de Log da aplicação com o modo _debug_ ativado

```javascript
2021-06-09 09:39:04 INFO  GLOVE - SAPJCO3 extractor started
Jun 09, 2021 9:39:04 AM br.com.dafiti.mitt.Mitt <init>
INFO: MITT v1.0.7
2021-06-09 09:39:38 DEBUG Before ZRFC_GET_TABLE_COUNT execute.
2021-06-09 09:39:40 DEBUG After ZRFC_GET_TABLE_COUNT execute.
2021-06-09 09:39:40 INFO  Table /B1H/ASD_D1100 has 1328376 records [WHERE CONDITION:  /BIC/ZUPDATED >= '020210601000000' ].
2021-06-09 09:39:40 DEBUG Before ZRFC_READ_TABLE execute
2021-06-09 09:45:55 DEBUG After ZRFC_READ_TABLE execute
2021-06-09 09:45:55 INFO  This request returned 300000 rows [ROWCOUNT: 300000, ROWSKIPS: 0].
2021-06-09 09:45:55 DEBUG Before mitt write.
2021-06-09 09:46:15 DEBUG After mitt write.
2021-06-09 09:46:15 DEBUG Before ZRFC_READ_TABLE execute
2021-06-09 09:50:09 DEBUG After ZRFC_READ_TABLE execute
2021-06-09 09:50:09 INFO  This request returned 300000 rows [ROWCOUNT: 300000, ROWSKIPS: 300000].
2021-06-09 09:50:09 DEBUG Before mitt write.
2021-06-09 09:50:29 DEBUG After mitt write.
2021-06-09 09:50:29 DEBUG Before ZRFC_READ_TABLE execute
2021-06-09 09:55:22 DEBUG After ZRFC_READ_TABLE execute
2021-06-09 09:55:22 INFO  This request returned 300000 rows [ROWCOUNT: 300000, ROWSKIPS: 600000].
2021-06-09 09:55:22 DEBUG Before mitt write.
2021-06-09 09:55:39 DEBUG After mitt write.
2021-06-09 09:55:39 DEBUG Before ZRFC_READ_TABLE execute
2021-06-09 09:58:57 DEBUG After ZRFC_READ_TABLE execute
2021-06-09 09:58:57 INFO  This request returned 300000 rows [ROWCOUNT: 300000, ROWSKIPS: 900000].
2021-06-09 09:58:57 DEBUG Before mitt write.
2021-06-09 09:59:14 DEBUG After mitt write.
2021-06-09 09:59:14 DEBUG Before ZRFC_READ_TABLE execute
2021-06-09 10:01:17 DEBUG After ZRFC_READ_TABLE execute
2021-06-09 10:01:17 INFO  This request returned 134061 rows [ROWCOUNT: 300000, ROWSKIPS: 1200000].
2021-06-09 10:01:17 DEBUG Before mitt write.
2021-06-09 10:01:22 DEBUG After mitt write.
2021-06-09 10:01:22 INFO  GLOVE - SAPJCO3 extractor finalized
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
