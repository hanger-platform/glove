# Email attachment extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados provenientes de anexos de e-mail.

## How it works

O Email attachment extractor possibilita a extração de dados proveniente de anexos de e-mails.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **email-attachment** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **email-attachment.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÃO

* Crie um arquivo contendo suas credenciais de e-mail e as propriedades de conexão, este será o seu **credentials file**:

```
{
	"email":"<email>",
	"password":"<password>",
	"protocol":"<protocol>",
	"connection":[
		{"<property>":"<value>"},
		{"<property>":"<value>"}...
	]
}
```

* As propriedades de conexão para cada protocolo são diferentes:

**imap** -> https://javaee.github.io/javamail/docs/api/com/sun/mail/imap/package-summary.html

**pop3** -> https://javaee.github.io/javamail/docs/api/com/sun/mail/pop3/package-summary.html

**smtp** -> https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html

## Utilização

```bash
java -jar email-attachment.jar  \
	--credentials=<Credentials file> \
	--output=<Output file> \
	--field=<Fields to be retrieved from e-mail attachment, concatenated by +> \	
	--folder=<(Optional) E-mail folder, default: INBOX> \	
	--start_date=<(Optional) E-mail received date since as YYYYMMDD> \	
	--start_time=<(Optional) E-mail received time since, default: 00:00:00> \	
	--end_date=<(Optional) E-mail received date to as YYYYMMDD> \	
	--end_time=<(Optional) E-mail received time to, default: 23:59:59> \	
	--from=<(Optional) E-mail from condition> \		
	--subject=<(Optional) E-mail subject condition> \		
	--pattern=<(Optional) Attachment file name pattern in a RegExp fashion, default: .csv|.xls|.xlsx|.avro|.gz|.zip> \	
	--partition=<(Optional)  Partition field, concatenated by +> \	
	--key=<(Optional) Unique key field, concatenated by +> \	
	--delimiter=<(Optional) File delimiter, default ;> \	
	--no_header=<(Optional) File has no header, default false> \
	--properties=<(Optional) MITT reader properties> \	
	--backup=<(Optional) Original attachment backup folder> 
```

##### EXEMPLO 1

Para recuperar o anexo de uma mensagem de e-mail do Gmail que foi recebida no dia **20/07/2021** e cujo assunto contém a palavra **export**, uma das alternativas seria: 

###### Credentials file:

O Gmail suporta o protocolo IMAP, e a conexão com o servidor poderia ser feita utilizando a seguinte configuração:

```javascript
{
	"email":"valdiney.gomes@dafiti.com.br",
	"password":"<password>",
	"protocol":"imap",
	"connection":[
		{"mail.imap.host":"imap.gmail.com"},
		{"mail.imap.port":"993"},
		{"mail.imap.socketFactory.class":"javax.net.ssl.SSLSocketFactory"},
		{"mail.imap.socketFactory.fallback":"false"}
	]
}
```

>Para o correto funcionamento do extrator utilizando o Gmail, é necessário que sua conta esteja habilitada para suportar a conexão de "Apps menos seguros": https://support.google.com/accounts/answer/6010255?hl=pt

###### Script

Tento o arquivo de credenciais configurado e estando apto para estabelecer uma conexão com o servidor de e-mail, o extrator seria configurado da seguinte forma:

```bash
java -jar email-attachment.jar \
	--credentials="/home/valdiney.gomes/credentials/gmail.json" \
	--output="/tmp/export.csv" \
	--field="id+name" \
	--start_date="2021-07-20" \
	--end_date="2021-07-20" \
	--subject="export"
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
