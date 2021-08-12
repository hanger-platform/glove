

# INMET API extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados da API do Instituto Nacional de Meteorologia. 

## How it works

A API do INMET possibilita a extração de dados meteorológicos fornecidos pelo INMET, sendo suportados os endpoints fornecidos no seguinte manual: https://portal.inmet.gov.br/manual.

> A API PREVISÃO DO TEMPO não é suportada nesta versão.  

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **inmet** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **inmet.jar** será gerado no subdiretório **_target_**.


## Utilização

```bash
java -jar inmet.jar  \
	--output=<Output file> \
	--field=<Fields to be retrieved in a JsonPath fashion> \
	--endpoint=<Endpoint name> \
	--partition=<(Optional) Partition, divided by + if has more than one field> \
	--key=<(Optional) Unique key, divided by + if has more than one field>
```

Para recuperação de dados da API de **condições de tempo registrado nas capitais** no dia **2021-08-11**, podemos utilizar a seguinte configuração: 

## JSON

É importante conhecer a estrutura do dado que será extraído, esta informação pode ser obtida na URL: https://portal.inmet.gov.br/manual selecionando a API desejada. 

```javascript
[
   {
      "CAPITAL":"ARACAJU",
      "TMIN18":"21.2*",
      "TMAX18":"28.3*",
      "UMIN18":"*",
      "PMAX12":"0*"
   },...
]
```

## Extractor

```bash
java -jar inmet.jar  \
	--output="/tmp/inmet/capitais.csv" \
	--field="CAPITAL+TMIN18+TMAX18+UMIN18+PMAX12+data::Fixed(20210811)" \
	--endpoint="condicao/capitais/2021-08-11"
```
	
> É importante observar dois pontos. 
> 1. A data faz parte do endpoint.
> 2. A API não retorna a data no JSON, desta forma, caso seja necessário, é importante que ela seja incluída utilizando as funções do MITT.


## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
