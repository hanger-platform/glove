
# Kestraa How Much Extractor
### Estrator de dados do serviço How much da Kestraa

## How it works

How much é o serviço da Kestraa responsável por calcular o rateio dos embarques, esses rateios são feitos com base em 5 documentos:
- Shipment
- Invoice
- Purchase Order
- Import Declaration
- Payment Document

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **kestraa** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **kestraa.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com o token gerado do site da braze, este será o seu **credentials file**:

```
{"email": "<email>", "password": "<password>"}

```

## Utilização

```bash
java -jar kestraa.jar  \
	--credentials=<Arquivo de credenciais>  \
	--output=<Caminho onde os arquivos serão salvos> 
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
