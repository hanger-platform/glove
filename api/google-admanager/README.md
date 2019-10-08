# Google Admanager Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados do Google Admanager. 

## How it works

O **Google Admanager Extractor** é uma ferramenta que possibilita extrair as informações de seus anúncios gerenciados pela ferramenta Google Ad Manager.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Conta criada na plataforma AD Manager.
- Criação de uma conta de serviço no Google Cloud Platform.
- Geração do arquivo secreto (JSON) a partir da conta de serviço.
- Acesso a API liberado na plataforma AD Manager.
- Conta de serviço devidamente cadastrada na plataforma AD Manager.
	- Documentação do Google para auxiliar na geração dos últimos 5 tópicos: https://developers.google.com/ad-manager/api/start

##### CONSTRUÇÃO

- Utilizando o [Maven](https://maven.apache.org/): 
    - Acesse o diretório no qual os fontes do **_Google Admanager Extractor_ **se localizam.
    - Digite o comando _**mvn package**_.
    - O arquivo **google-admanager.jar** será gerado no subdiretório **_target_**.

## Utilização

```bash
java -jar google-admanager.jar \
	--credentials=<Identifica o caminho onde o arquivo secreto com as credenciais está localizado> \
	--application_name=<Identifica o nome da aplicação> \
	--output=<Identifica o caminho e nome do arquivo que será gerado> \
	--network_code=<Identifica o código da rede do Ad Manager que o processo será executado. Encontre essa informação na sua conta do Ad Manager, na aba de administrador, configurações globais.> \
	--start_date=<Identifica a data de início para buscar os dados.	> \
	--end_date=<Identifica a data final para buscar os dados.	> \
	--dimensions=<Identifica as dimensões que serão extraídas> \
	--columns=<Identifica as métricas que serão extraídas> \
	--dimensions_attributes=<(Opcional) Identica os atributos das dimensões que serão extraídos> \
	--filters=<(Opcional) Identifica os filtros que serão utilizados na extração de dados> \
	--time_zone=<Zona de extração, exemplo: America/Sao_Paulo> \
	--key=<Identifica a chave primária> \
	--partition=<Identifica o campo que será utilizado para particionamento dos dados> \
	--quote =<(Opcional) Identifica o separador, padrão é aspas> \
	--delimiter =<(Opcional) Identifica o delimitador, padrão é vírgula>
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
