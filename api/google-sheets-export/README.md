# Google Sheets Export [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Exportação de dados para Google Sheets. 

## How it works

O **Google Sheets Export** é uma ferramenta que possibilita efetuar a exportação de dados para uma planilha do Google Sheets. Somente é aceito arquivo de entrada do tipo csv.

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
    - Acesse o Google Developer Console API [https://console.developers.google.com/apis](https://console.developers.google.com/apis)
    - Clique no menu **Credenciais**.
    - Clique em **Criar credenciais**. 
    - Clique em criar **ID do cliente do OAuth**.
    - Selecione a opção **Outros**
    - Defina um **nome** e clique em **criar**.
    - Faça download do arquivo JSON relacionado com a credencial.     

##### CONSTRUÇÃO

- Utilizando o [Maven](https://maven.apache.org/): 
    - Acesse o diretório no qual os fontes do **_Google Sheets Export_ **se localizam.
    - Digite o comando _**mvn package**_.
    - O arquivo **google-sheets-export.jar** será gerado no subdiretório **_target_**.

## Utilização

```bash
java -jar google-sheets-export.jar \
	--credentials=<Identifica o caminho onde o arquivo secreto com as credenciais está localizado> \
	--spreadsheet=<Identifica o ID da spreadsheet que receberá os dados (PS: este id podde ser encontrado na URL entre /d/ e /edit/> \
	--input=<Identifica o caminho e nome do arquivo de entrada, estre arquivo deve ser do tipo CSV> \
	--sheet=<Identifica qual a aba que os dados serão exportados> \
	--debug=<Identifica se o extrator deve rodar em modo debug>
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
