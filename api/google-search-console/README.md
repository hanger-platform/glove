# Google Search Console Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados do Google Search Console. 

## How it works

O **Google Search Console Extractor** é uma ferramenta que possibilita extrair as informações do Google Search Console. 

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Criação de uma conta de serviço no _Google Cloud Platform_ caso não exista.
- Ativar biblioteca Google Search Console:
    - Acessar [https://console.cloud.google.com](https://console.cloud.google.com/)
    - _APIs_ e serviços
    - Biblioteca
    - Buscar por '_Google search console API_**'**
    - Se _API_ não estiver ativada, ative-a.

- Criar credenciais de acesso para _API_
    - Acessar [https://console.cloud.google.com](https://console.cloud.google.com/)
    - _APIs_ e serviços
    - Credenciais
    - Criar credenciais
    - Chave de conta de serviço
    - Escolher tipo _JSON_
    - Guardar arquivo _JSON_ gerado

- Adicionar permissão de acesso para conta de serviço
    - Acessar [https://search.google.com/](https://search.google.com/u/1/search-console/users?resource_id=https%3A%2F%2Fwww.dafiti.com.br%2F&hl=pt-BR)
    - Escolher domínio desejado
    - Configurações
    - Adicionar usuário
    - Colocar o e-mail do usuário de serviço

##### CONSTRUÇÃO

- Utilizando o [Maven](https://maven.apache.org/): 
    - Acesse o diretório no qual os fontes do **_Google Search Console Extractor_ **se localizam.
    - Digite o comando _**mvn package**_.
    - O arquivo **google-search-console.jar** será gerado no subdiretório **_target_**.

## Utilização

```bash
java -jar google-search-console.jar \
	--json_key_path=<Identifica o caminho onde o arquivo secreto com as credenciais está localizado> \
	--application_name=<Identifica o nome da aplicação> \
	--output=<Identifica o caminho e nome do arquivo que será gerado> \
	--site=<	Identifica o nome do site a ser buscado> \
	--start_date=<Identifica a data de início para buscar os dados> \
	--end_date=<Identifica a data final para buscar os dados> \
	--dimension=<Identifica as dimensões que serão extraídas> \
	--key=<Identifica a chave primária> \
	--partition=<Identifica o campo que será utilizado para particionamento dos dados> \
	--device=<Identifica para quais dispositivos os dados devem ser gerados> \
	--type=<(Opcional) Identifica o tipo de busca, padrão é web>
	--quote =<(Opcional) Identifica o separador, padrão é aspas>
	--delimiter =<(Opcional) Identifica o delimitador, padrão é vírgula>
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
