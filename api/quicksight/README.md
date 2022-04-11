# Quicksight Extractor
### Extrator de dados da API AWS Quicksight.

## How it works

Quicksight é uma ferramenta de Business Intelligence na nuvem da AWS. Ela disponibiliza uma API para consumir os dados gerados na ferramenta. É possível efetuar todas as operações da ferramenta através da API (Documentação: https://docs.aws.amazon.com/pt_br/quicksight/latest/APIReference/Welcome.html).

A ferramenta **Quicksight Extractor** é responsável por extrair diversos recursos dessa api e gerar um arquivo _csv_ com o resultado obtido. Os seguintes recursos podem ser extraídos:
* **User**: Lista de todos os usuários do Amazon QuickSight pertencentes a uma conta.
* **Group**: Lista todos os grupos de usuários no Amazon QuickSight.
* **GroupMembership**: Lista usuários-membro em um grupo.
* **Dashboard**: Lista painéis em uma conta da AWS.
* **DashboardPermissions**: Lista permissões de leitura e gravação para um painel.
* **DataSet**: Lista todos os conjuntos de dados de uma conta da AWS em uma região.
* **DataSetPermissions**: Lista as permissões em todos os conjunto de dados.
* **DataSource**: Lista as fontes de dados em um região da AWS e que pertencem a uma conta da AWS.


## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- CLI AWS
- Permissões nos recursos do AWS Quicksight

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **quicksight** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **quicksight.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÃO

* O AWS CLI deve estar instalado a configurado com as chaves **AWS Secret Access Key** e **AWS Access Key**, elas podem ser encontradas no console da AWS.

## Utilização

```bash
java -jar quicksight.jar  \
	--output=<Caminho onde o arquivo será salvo> \
	--resource=<Recurso que deseja extrair> \	
	--region=<Região aws> \
	--account=<ID da conta da AWS> \
	--namespace=<Namespace do AWS quicksight> \
	--partition=<(Opcional) Partição, dividos por + quando for mais de um> \
	--key=<(Opcional) Chave única, dividos por + quando for mais de um>
```

Os valores para o parâmetro **_resource_** são:
* user
* group
* group_membership
* dashboard
* dashboard_permissions
* dataset
* dataset_permissions
* datasource

##### EXEMPLO

Neste exemplo, extraímos informações sobre grupos do AWS Quicksight.

```bash
java -jar quicksight.jar \
	--output="/tmp/quicksight/default/group/group.csv" \
	--region="us-east-1" \
	--account="296025910508" \
	--namespace="default" \
	--resource="group" \
	--key="::checksum()" \
	--partition="::fixed(default)"

```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
