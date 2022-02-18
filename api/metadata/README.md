# Metadata [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Metadata é uma ferramenta auxiliar do GLOVE para geração de metadados.

## How it works

A ferramenta **Metadata** atua na geração de arquivos de metadados que serão utilizados pelo processo de ingestão do GLOVE, no total, essa ferramena gera quatro arquivos:
- **<schema>_<tabela>_columns.csv**: Lista com todos os campos, um campo por linha.
- **<schema>_<tabela>_fields.csv**: Lista de todos os campos com o tipo de dados.
- **<schema>_<tabela>.json**: Possui formato json com os campos e os seus respectivos tipos, formato para ser usados nos conversores de orc e parquet.
- **<schema>_<tabela>_metadata.csv**: Possui formato json com os campos e os seus respectivos tipos.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **metadata** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **metadata.jar** será gerado no subdiretório **_target_**.

## Utilização

#### Parâmetros
##### Explicação dos parâmetros disponíveis na ferramenta.

```bash
java -jar metadata.jar  \
	--folder=<Folder where the sample file is> \
	--dialect=<(Optional) dialect; default is spectrum> \
	--sample=<(Optional) Define the data sample to be analized at metadata extraction process; default is 100000> \
	--delimiter=<(Optional) Delimiter of csv files; default is ;> \
	--quote=<(Optional) Identifies the quote escape character; default is \""> \
	--escape=<(Optional) Identifies the quote escape character; default is \"> \
	--thread=<(Optional) Limit of threads, be careful with it not to overload the workstation memory; default is 1> \
	--field=<(Optional) Identifies the header fields of a csv file> \
	--metadata=<(Optional) Identifies the csv field metadata> \
	--output=<(Optional) Identifies the output path> \
	--filename=<(Optional) Filename, with wildcard if necessary> \
	--reservedWords=<(Optional) Identifies the reserved words file list>	
```             

#### Exemplo 1
##### Gerar metadados a partir de um arquivo csv.

```bash
java -jar metadata.jar  \
	--folder=/home/user/Documents/sample_file/ \
	--output=/home/user/Documents/output/ \
	--metadata="" \
	--field="" \
	--sample=100000 \
	--delimiter=";" \
	--filename=spc_staging_dev_sales_order_simplified.csv \
	--dialect=spectrum \
	--reservedWords=/home/user/Documents/reserved_words/spectrum.txt
```

O diretório informado no parâmetro _folder_ contem um único arquivo com o nome: _spc_staging_dev_sales_order_simplified.csv_.

Esse arquivo contém o seguinte conteúdo:

```bash
partition_field;custom_primary_key;id_sales_order;customer_first_name;customer_last_name;customer_email;order_nr;original_grand_total;payment_method;created_at;updated_at
2022-02-13;138563627;138563627;LEGACY;LEGACY;email_xxx@icloud.com;4512319139;251.98;debitcard;2022-02-13 00:00:05.0;2022-02-13 00:16:59.0
2022-02-13;136365829;138563629;LEGACY;LEGACY;email_yyy@gmail.com;4548519123;374.9;braspag_cc;2022-02-13 00:00:07.0;2022-02-13 00:17:51.0
2022-02-13;138541831;138563631;LEGACY;LEGACY;email_zzz@gmail.com;454812343;69.98;braspag_cc;2022-02-13 00:00:07.0;2022-02-13 00:00:07.0
2022-02-13;138541636;138563633;LEGACY;LEGACY;email_www@hotmail.com;4548519145;618.65;braspag_cc;2022-02-13 00:00:09.0;2022-02-13 00:17:52.0
2022-02-13;138636835;138563635;LEGACY;LEGACY;email_aaa@gmail.com;4548512347;99.9;braspag_cc;2022-02-13 00:00:10.0;2022-02-13 00:17:52.0
....
```

Após a execução do programa, o diretório output irá conter os seguintes arquivos:

- spc_staging_dev_sales_order_simplified_columns.csv
- spc_staging_dev_sales_order_simplified_fields.csv
- spc_staging_dev_sales_order_simplified.json
- spc_staging_dev_sales_order_simplified_metadata.csv

**spc_staging_dev_sales_order_simplified_columns.csv**
		
```bash
partition_field
custom_primary_key
id_sales_order
customer_first_name
customer_last_name
customer_email
order_nr
original_grand_total
payment_method
created_at
updated_at
```	

**spc_staging_dev_sales_order_simplified_fields.csv**

```bash
partition_field varchar(20),custom_primary_key bigint,id_sales_order bigint,customer_first_name varchar(12),customer_last_name varchar(12),customer_email varchar(58),order_nr bigint,original_grand_total double precision,payment_method varchar(28),created_at varchar(42),updated_at varchar(42)
```

**spc_staging_dev_sales_order_simplified.json**
		
```bash
[{"name":"partition_field","type":["null","string"],"default":null},
{"name":"custom_primary_key","type":["null","long"],"default":null},
{"name":"id_sales_order","type":["null","long"],"default":null},
{"name":"customer_first_name","type":["null","string"],"default":null},
{"name":"customer_last_name","type":["null","string"],"default":null},
{"name":"customer_email","type":["null","string"],"default":null},
{"name":"order_nr","type":["null","long"],"default":null},
{"name":"original_grand_total","type":["null","double"],"default":null},
{"name":"payment_method","type":["null","string"],"default":null},
{"name":"created_at","type":["null","string"],"default":null},
{"name":"updated_at","type":["null","string"],"default":null}]
```

**spc_staging_dev_sales_order_simplified_metadata.csv**
		
```bash
[{"field":"partition_field","type":"string","length":20},
{"field":"custom_primary_key","type":"integer"},
{"field":"id_sales_order","type":"integer"},
{"field":"customer_first_name","type":"string","length":12},
{"field":"customer_last_name","type":"string","length":12},
{"field":"customer_email","type":"string","length":58},
{"field":"order_nr","type":"integer"},
{"field":"original_grand_total","type":"number"},
{"field":"payment_method","type":"string","length":28},
{"field":"created_at","type":"string","length":42},
{"field":"updated_at","type":"string","length":42}]
```
		
		

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
