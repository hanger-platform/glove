# Google Analytics Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados armazenados em servidores SFTP 

## How it works

O **Google Analytics Extractor** permite a extração de dados do google analytics. Todas as dimensões e métricas que podem ser usadas para geração de gelatórios estão disponíveis em (https://ga-dev-tools.appspot.com/dimensions-metrics-explorer/).

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **google-analytics** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **google-analytics.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* A configuração deve ser realizada de acordo com o seguinte tutorial: https://developers.google.com/analytics/devguides/reporting/core/v4/quickstart/installed-java?hl=pt-br

## Utilização

```bash
java -jar google-analytics.jar  \
	--client_secret=<Client secrets json file path>  \
	--view_id=<Google analytics project view ID. HowTo: (https://www.chatnox.com/faq-items/find-google-analytics-view-id/)> \
	--start_date=<Report start date> \
	--start_date=<Report end date> \
	--dimensions=<Dimensions, divided by + if has more than one> \
	--metrics=<Metrics, divided by + if has more than one> \
	--output=<Output file> \
	--partition=<(Optional)  Partition field, divided by + if has more than one> \
	--key=<(Optional) Unique key, divided by + if has more than one> 
```

## Exemplo

```bash
java -jar google-analytics.jar  \
	--client_secret=/home/glove/.google-analytics/client_secret.json \
	--view_id=123456789 \
	--start_date=2019-08-24 \
	--end_date=2019-08-25 \
	--dimensions=ga:userType \
	--metrics=ga:users+ga:sessionsPerUser \
	--output=/tmp/V4.csv
```

* Na primeira utilização será necessário acessar a URL exibida no console para autorizar a aplicação. Após a autenticação, será gerado um arquivo no diretório $USER_HOME/.store/google_analytics_reporting/ que será utilizado para novos acessos à API, sem a necessidade de nova autenticação.

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
