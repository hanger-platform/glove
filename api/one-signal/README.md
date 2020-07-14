
# OneSignal Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados da api da plataforma Onesginal

## How it works

O **Onesignal Extractor** permite a extração de dados da API Rest disponibilizada pela OneSignal.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Chave gerada no site da OneSignal (https://documentation.onesignal.com/docs/accounts-and-keys)

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **OneSignal** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **one-signal.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com a chave gerada do site da Onesignal, este será o seu **credentials file**:

```
{
	"authorization":"<Basic key>"
}
```

## Utilização

```bash
java -jar one-signal.jar  \
	--credentials=<Arquivo de credenciais>  \
	--output=<Caminho onde arquivo será salvo> \
	--service=<Nome do serviço a ser consumido> \
	--apps=<Id do app que deseja que as informações sejam extraídas, dividos por + quando for mais de um> \
	--field=<(Opcional) Campos que serão gerados no arquivo de saída> \
	--partition=<(Opcional) Partição, dividos por + quando for mais de um> \	
	--key=<(Opcional) Chave única, dividos por + quando for mais de um> \
	--delimiter=<(Opcional) Delimitador. ';' é o padrão> \
	--sleep=<(Opcional) Tempo de espera entre uma chamada de outra. '0' é o padrão> \	
```

## Serviços disponíveis

| Serviço        | Descrição                                                 | Documentação                                                       |
|----------------|-----------------------------------------------------------|--------------------------------------------------------------------|
| users          | Exportação de todos os seus dados atuais do usuário       | https://documentation.onesignal.com/reference/csv-export           |
| notifications  | Extração dos detalhes de várias notificações              | https://documentation.onesignal.com/reference/view-notifications   |


## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
