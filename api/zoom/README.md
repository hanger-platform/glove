
# Zoom extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados de account statement do Zoom. 

## How it works

O **Zoom extractor** é um crawler que permite a extração de dados do extrato do anunciante na plataforma do zoom (http://anunciante.zoom.com.br/). 

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **zoom** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **zoom.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com o token gerado do site da braze, este será o seu **credentials file**:

```
{
{
	"username":"<username>",
	"password":"<password>",
	"account":"<Merchant ID>"
}
}
```

## Utilização

```bash
java -jar zoom.jar  \
	--credentials=<Arquivo de credenciais>  \
	--output=<Caminho onde arquivo será salvo> \
	--start_date=<Data inicial do relatório no formato YYYY-MM-DD> \	
	--end_date=<Data final do relatório no formato YYYY-MM-DD> \
	--field=<Campos que serão gerados no arquivo de saída> \
	--partition=<(Opcional) Partição, dividos por + quando for mais de um> \	
	--key=<(Opcional) Chave única, dividos por + quando for mais de um>
```

## Exemplos

```bash
java -jar /<path>/zoom.jar \
	--credentials="/<path>/zoom.json"  
	--output="/tmp/zoom/zoom.csv" 
	--field="Data+Crédito+Saldo" 
	--start_date="2021-01-01" 
	--end_date="2021-03-20"
```

Os campos disponíveis para extração são os seguintes:
|Field                          |Description		|
|-------------------------------|-----------------------|
|Data				|Data da operação 	|
|Crédito			|Valor de crédito 	|
|Débito				|Valor de débito 	|
|Observações			|Observação.		|
|Saldo				|Valor de saldo		|

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
