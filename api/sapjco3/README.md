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
  --field="<Opcional, Identifica o nome dos campos que serão extraídos, senão for passado o processo tentará pegar os campos automaticamente>" \
  --import='conteúdo json que representa os parâmetros de importação da função desejada' \
  --tables='conteúdo json que representa os parâmetros de tabelas da função desejada ' \
  --key=<Identifica a chave primária> \
  --partition=<Identifica o campo que será utilizado para particionamento dos dados>
```

* o parâmetro _tables_ deve seguir sempre a seguinte estrutura para suas funções:
```json
[
	{
		"TABLE":"<Nome do parâmetro de tabelas 1>",
		"VALUES":[{
			"<Tipo de referência - Componente 1>":"<Valor>"
		}]
	},
	{
		"TABLE":"<Nome do parâmetro de tabelas 2>",
		"VALUES":[
			{"<Tipo de referência - Componente 1>":"<Valor 1>"},
			{"<Tipo de referência - Componente 2>":"<Valor 2>"}
		]
	}
]
```


* o parâmetro _import_ deve seguir sempre a seguinte estrutura para suas funções:
```json
{
	"<NOME_DO_PARÂMETRO 1>":"<VALOR 1>",
	"<NOME_DO_PARÂMETRO_2>":"<VALOR 2>"
}
```

* Para saber quais são os parâmetros da função (**importação ou tabelas**), deve se consultar a Transação **SE37 (Módulos de função ABAP)** dentro do **SAP GUI**.

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
