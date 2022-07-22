

# Google Directory API extractor
### Extrator de dados do Google Admin para usuários e grupos de uma conta.

## How it works

Através da API do _Google Directory_ é possível gerenciar recursos atrelados ao Google Workspace domain, como usuários, grupos etc. O Google Directory API extractor tem como objetivo extrair todos os dados relacionados a usuários.


## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Uso da API Admin SDK API habilitado no Google Cloud: [Link](https://developers.google.com/admin-sdk/directory/v1/quickstart/java?hl=pt-br)
- Arquivo de credencias

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **google-directory** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **google-directory.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* É necessário gerar um **credentials file** através do Console do Google Cloud nas opções de APIs e serviços, para esse extrator é necessário utilizar a opção oAuth 2.0 e armazenar o arquivo .json na estação de execução, Documentação para auxiliar nessa criação: https://developers.google.com/workspace/guides/create-credentials?hl=pt-br


## Utilização

```bash
java -jar google-directory.jar  \
	--credentials=<Credentials file>  \
	--output=<Output file> \
	--field=<Fields to be retrieved from an endpoint in JsonPath fashion> \
	--show_deleted=<(Optional) If set to 'true', retrieves the list of deleted users. (Default: 'false')> \
	--max_results=<(Optional) Maximum number of results to return in a call. (Default: 50)> \
	--customer=<(Optional) The unique ID for the customer's Google Workspace account. (Default: my_customer)> \
	--query=<(Optional) Query string for searching user fields> \
	--partition=<(Optional) Partition, divided by + if has more than one field>
	--key=<(Optional) Unique key, divided by + if has more than one field>
```

* Caso haja necessidade de montar uma query específica, consultar o seguinte documenta para auxílio: https://developers.google.com/admin-sdk/directory/v1/guides/search-users
* Caso haja interesse, a documentação a seguir lista alguns exemplos do uso da API: https://developers.google.com/admin-sdk/directory/v1/guides/manage-users

#### Exemplo

Retornar usuários que foram deletados nos últimos dias.


```bash
java -jar google-directory.jar \
  --credentials="/credentials_path/google_directory_api.json" \
  --output="/tmp/google_directory/users/users.csv" \
  --field="customerId+id+name.fullName+creationTime+deletionTime+kind+lastLoginTime+primaryEmail" \
  --show_deleted="true" \
  --partition="" \
  --key="id"
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
