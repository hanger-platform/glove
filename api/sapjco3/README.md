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

##### Usando a função RFC_READ_TABLE

```bash
java -jar /home/etl/lib/sapjco3.jar \
  --credentials="/caminho/bw.json" \
  --output="/tmp/ZBW000029/ZBW000029.csv" \
  --function="RFC_READ_TABLE" \
  --field="MANDT+DELIV_NUMB+AMOUNT+CURRENCY+MOTIVO+/BIC/ZUPDATED" \
  --import='{"QUERY_TABLE":"ZBW000029","DELIMITER":"|"}' \
  --tables='[{"TABLE":"OPTIONS","VALUES": [{"TEXT":"AMOUNT > 1 OR DELIV_NUMB = '"'"'8043143930'"'"' "}]},{"TABLE":"FIELDS","VALUES":[{"FIELDNAME":"MANDT"},{"FIELDNAME":"DELIV_NUMB"},{"FIELDNAME":"AMOUNT"},{"FIELDNAME":"CURRENCY"},{"FIELDNAME":"MOTIVO"},{"FIELDNAME":"/BIC/ZUPDATED"}]}]' \
  --key='::checksum()' \
  --partition='::fixed(FULL)'
```
* No exemplo acima, estamos usando a função **RFC_READ_TABLE**
* Ela possui vários parãmetros de importação, nesse exemplo usamos o **QUERY_TABLE** que recebe o nome da tabela a ser extraída e **DELIMITER**, que receber qual o delimitador do _resultset_.
* Ela possui vários parâmetros de tabelas, nesse exemplos usamos o **OPTIONS** para fazer um filtro (_WHERE_) na consulta e **FIELDS** para trazer somente alguns determinados campos.


## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
