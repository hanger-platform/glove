# Google Sheets Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados do Google Sheets. 

## How it works

O **Google Sheets Extractor** é uma ferramenta que possibilita extrair as informações de suas planilhas no Google Sheets.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Criação de uma conta de serviço no _Google Cloud Platform_ caso não exista.
- Ativar biblioteca Google Sheets:
    - Acessar [https://console.cloud.google.com](https://console.cloud.google.com/)
    - _APIs_ e serviços
    - Biblioteca
    - Buscar por '_Google Sheets API_**'**
    - Se _API_ não estiver ativada, ative-a.

- Criar credenciais de acesso para _API_
    - Acessar [https://developers.google.com/sheets/api/quickstart](https://developers.google.com/sheets/api/quickstart)
    - Clique no botão Enable the Google Sheets API
    - Cliquei em DOWNLOAD CLIENT CONFIGURATION
    - Guardar arquivo _JSON_ gerado

##### CONSTRUÇÃO

- Utilizando o [Maven](https://maven.apache.org/): 
    - Acesse o diretório no qual os fontes do **_Google Sheets Extractor_ **se localizam.
    - Digite o comando _**mvn package**_.
    - O arquivo **google-sheets.jar** será gerado no subdiretório **_target_**.

## Utilização

```bash
java -jar google-sheets.jar \
	--credentials=<Identifica o caminho onde o arquivo secreto com as credenciais está localizado> \
	--application_name=<Identifica o nome da aplicação> \
	--output=<Identifica o caminho e nome do arquivo que será gerado> \
	--spreadsheet=<Identifica o ID da sheets que será extraída> \
	--key=<Identifica a chave primária> \
	--partition=<Identifica o campo que será utilizado para particionamento dos dados> \
	--sheets=<(Opcional) Identifica quais sheets serão extraídas, caso não seja informado extrairá todas as planilhas> \
	--field=<(Opcional) Identifica os campos das planilhas que serão extraídos, senão for informado extrairá todos os campos> \
	--quote =<(Opcional) Identifica o separador, padrão é aspas> \
	--delimiter =<(Opcional) Identifica o delimitador, padrão é vírgula>
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
