# JDBC Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados de bancos que suportem JDBC.

## How it works

O JDBC Extractor possibilita a extração de dados de bancos de dados que suportem a JDBC; 

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **jdbc** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **jdbc.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÃO

* Crie um arquivo contendo suas credenciais e as propriedades de conexão, este será o seu **credentials file**:

```javascript
{
	"username":"<username>",
	"password":"<password>",
	"JDBCDriverFile":"<JDBC driver file path>",
	"JDBCDriverClass":"<JDBC driver class>",
	"JDBCUrl":"<JDBC url>"
}
```

## Utilização

```bash
java -jar jdbc.jar  \
	--credentials=<Credentials file> \
	--output=<Output file> \
	--delimiter=<(Optional)  Output delimiter, default as ;> \
	--field=<(Optional) Fields to be retrieved, concatenated by +> \	
	--catalog=<(Optional) Catalog name> \	
	--schema=<(Optional) Schema name> \	
	--table=<(Optional) Table name> \	
	--filter=<(Optional) SQL filter condition> \	
	--sql=<(Optional) SQL SELECT statement. Its possible to read the SELECT statement from a file using: file://<.sql file path>> \	
	--partition=<(Optional)  Partition field, concatenated by +> \	
	--key=<(Optional) Unique key field, concatenated by +> \
	--parameter=<(Optional) Credentials and SQL SELECT statement replacement variable, in a bash fashion> \
	--no_header=<Identifies if output file should have a header>
```

###### Credentials file:

Para utilizar o extrator de dados com o MySQL, a seguinte configuração poderia ser utilizada:

```javascript
{
	"username":"<username>",
	"password":"<password>",
	"JDBCDriverFile":"/home/valdiney.gomes/Classes/mysql-connector-java-8.0.26.jar",
	"JDBCDriverClass":"com.mysql.cj.jdbc.Driver",
	"JDBCUrl":"jdbc:mysql://127.0.0.1:3306/company"
}
```

> Observe que o driver JDBC foi baixado do site do fornecedor do banco de dados e disponibilizado em um diretório local para ser usado pelo extrator. Consulte o manual do fornecedor do banco de dados para identificar a JDBCDriverClass e a forma correta de configurar a JDBCUrl. 

##### EXEMPLO 1

Para recuperar todos os dados da tabela **customers** do schema **bob** de um banco de dados **Mysql**, uma das alternativas seria: 

###### Script

Tendo o arquivo de credenciais configurado, para alcançar o resultado esperado o extrator poderia ser configurado da seguinte forma:

```bash
java -jar jdbc.jar \
	--credentials="/home/valdiney.gomes/credentials/mysql.json" \
	--output="/tmp/jdbc/customers.csv" \
	--field="id+nome+::checksum()" \
	--schema="bob" \
	--table="customers"
```

##### EXEMPLO 2

Em situações mais complexas, nas quais é necessário cruzar duas ou mais tabelas, uma alternativa seria:

###### Script

```bash
java -jar jdbc.jar \
	--credentials="/home/valdiney.gomes/credentials/mysql.json" \
	--output="/tmp/jdbc/customers.csv" \
	--sql="SELECT * FROM dafiti.customers"
```

Ou para ler a query de um arquivo com a extensão .sql

```bash
java -jar jdbc.jar \
	--credentials="/home/valdiney.gomes/credentials/mysql.json" \
	--output="/tmp/jdbc/customers.csv" \
	--fields=id+nome \
	--sql="file:///home/valdiney.gomes/queries/query.sql"
```

Em qualquer situação, é possivel definir variáveis tanto no arquivo de credencial quanto no arquivo .SQL a serem substituídas em tempo de execução:

###### SQL

```sql
	SELECT * FROM dafiti.customers WHERE created_at > '${date}'
```

A variável poderia ser informada utilizando a seguinte configuração: 

```bash
java -jar jdbc.jar \
	--credentials="/home/valdiney.gomes/credentials/mysql.json" \
	--output="/tmp/jdbc/customers.csv" \
	--fields=id+nome \
	--sql="file:///home/valdiney.gomes/queries/query.sql" \
	--parameter='{"date":"2021-08-02"}'
```

Para definir valores _default_ para qualquer variável, pode ser utilizado o delimitador **":-"** da seguinte forma:

###### SQL

```sql
	SELECT * FROM dafiti.customers WHERE created_at > '${date:-2021-08-02}'
```

> Quando o parametro não for informado as transformações do MITT não poderão ser utilizadas, porém todos os campos da tabela ou consulta customizadas serão incluídas automaticamente. Caso os campos sejam informados no parâmetro field, as transformações poderão ser usadas normalmente.

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
