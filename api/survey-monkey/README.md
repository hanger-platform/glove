# SurveyMonkey Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados da SurveyMonkey. 

## How it works

A empresa **SurveyMonkey** tem como objetivo efetuar o desenvolvimento de pesquisas online, com essa plataforma de pesquisa, fica fácil medir e entender feedback de clientes a fim de poder impulsionar o crescimento e a inovação dos negócios.

O **SurveyMonkey Extractor** é uma ferramenta desenvolvida utilizando o framework _mitt_ e possibilita extrair dados da _api_ do _surveymonkey_ (https://developer.surveymonkey.com/). Com ela é possível extrair diversos dados disponíveis na api de maneira bem simplificada (como por exemplo _surveys_ disponíves, detalhes de uma campanha, respostas do clientes em uma campanha etc.), gerando no final um arquivo csv simplificado com o conteúdo desejado.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Criação de um app na seção My apps (detalhes em https://developer.surveymonkey.com/api/v3/)
- Deploy

##### CONSTRUÇÃO

- Utilizando o [Maven](https://maven.apache.org/): 
    - Acesse o diretório no qual os fontes do **_survey-monkey_ **se localizam.
    - Digite o comando _**mvn package**_.
    - O arquivo **survey-monkey.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as credencias para acessar a _api_ através do _access token_ do _app_ criado, este será o seu **credentials file**:

```
{
	"authorization":"bearer <access token>"
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

```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
