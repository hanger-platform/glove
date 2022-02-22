
# Splitter [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Splitter é uma ferramenta que auxilia na criação de arquivos csv particionados.

## How it works

A ferramenta **Splitter** basicamente faz a leitura de arquivo no formato csv, identifica a coluna de particionamento, e transforma esse arquivo em arquivo(s) csv particionado(s).

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **splitter** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **splitter.jar** será gerado no subdiretório **_target_**.

## Utilização

#### Parâmetros
##### Explicação dos parâmetros disponíveis na ferramenta.

```bash
java -jar splitter.jar  \
	--folder=<Folder where the files to be splitted are located> \
	--filename=<(Optional) Filename, with wildcard if necessary, to be converted; default is "*.csv"> \
	--header=<(Optional) Identifies the csv file has a header; default is false> \
	--replace=<(Optional) Identifies if csv files will be replaced by partitioned files; default is false> \
	--thread=<(Optional) Limit of thread; default is 1> \
	--delimiter=<(Optional) Delimiter of csv files; default is ;> \
	--quote=<(Optional) Identifies the quote character; default is \"> \
	--escape=<(Optional) Identifies the quote escape character; default is \"> \
	--partition=<(Optional) Partition column; default is 0> \
	--splitStrategy=<(Optional) Identifies if split strategy is FAST or SECURE; default is SECURE> \
	--readable=<(Optional) Identifies if partition name should be readable at runtime; default is false>

```             

#### Exemplos
##### Arquivo de entrada:

    /home/user/Documents/customer/customer.csv

```bash
partition_field;custom_primary_key;id;first_name;last_name
20220101;100;100;John;Smiths
20220101;200;200;Ted;Thompson
20220102;303;303;Paul;Reed
20220101;405;405;Rose;Williams
20220102;502;502;Bertha;Blood
```
#### Exemplo 1
```bash
java -jar splitter.jar  \
	--folder=/home/user/Documents/customer/ \
	--header=true
```

##### Arquivos de saída:

    /home/user/Documents/customer/20220101/57956570-2a91-4053-9024-25601ef132b7.csv

```bash
20220101;100;100;John;Smiths
20220101;200;200;Ted;Thompson
20220101;405;405;Rose;Williams
```

    /home/user/Documents/customer/20220102/26d76e78-daf8-4f68-8638-dd2b6273168a.csv

```bash
20220102;303;303;Paul;Reed
20220102;502;502;Bertha;Blood
```

#### Exemplo 2
O parâmetro readable habilitado (true), serve para gerar arquivo com nome legível, como no exemplo abaixo.

```bash
java -jar splitter.jar  \
	--folder=/home/user/Documents/customer/ \
	--header=true \
	--readable=true

```

##### Arquivos de saída:

    /home/user/Documents/customer/20220101.csv

```bash
20220101;100;100;John;Smiths
20220101;200;200;Ted;Thompson
20220101;405;405;Rose;Williams
```

    /home/user/Documents/customer/20220102.csv

```bash
20220102;303;303;Paul;Reed
20220102;502;502;Bertha;Blood
```

## Informações adicionais

**Parâmetro splitStrategy:** Identifica a estratégia utilizada para o particionamento dos dados sendo: 
 - FAST - os dados de entrada que serão processados são confiáveis e, não contém caracteres especiais ou quebra de linhas. Modo rápido;
 - SECURE - os dados de entrada não são confiáveis e devem passar por um tratamento. Modo um pouco mais lento;

**Observação:** A ferramenta só aceita arquivo de entrada no formato **csv**.

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.