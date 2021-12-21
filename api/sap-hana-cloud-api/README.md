# SAP HANA CLOUD API Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados da api do SAP HANA CLOUD.

## How it works

O **SAP HANA CLOUD API Extractor** é uma ferramenta que possibilita extrair dados via api do SAP Hana Cloud por meio do formato oData.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

- Utilizando o [Maven](https://maven.apache.org/): 
    - Acesse o diretório no qual os fontes do **_SAP HANA CLOUD API_ **se localizam.
    - Digite o comando _**mvn package**_.
    - O arquivo **sap-hana-cloud-api.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as credencias para acessar a api, este será o seu **credentials file**:

```
{
	"user":"<Usuário>",
	"passwd":"<Senha>",
}

```

## Utilização

```bash
java -jar sap-hana-cloud-api.jar \
  --credentials="<Identifica o caminho onde o arquivo secreto com as credenciais está localizado>" \
  --output="<Identifica o caminho e nome do arquivo que será gerado>" \
  --field="<Identifica o nome dos campos que serão extraídos>" \
  --uri="<URI a ser chamada, pode ser passado o filtro pela URI>" \
  --object="<Opcional, Objeto json para varrer os valores de retorno>" \
  --partition="<Opcional, Identifica o campo que será utilizado para particionamento dos dados>" \
  --key="<Opcional, Identifica a chave primária>"
```                 

## Exemplo

```bash
java -jar /home/etl/lib/sap-hana-cloud-api.jar \
	--credentials="/<credentials_path>/<credentials_file>.json" \ 
	--output=/tmp/sap_hana_cloud_api/getintegracao.csv \
	--field="CD_FLUXO+CD_PAIS+CD_CHAVE_EXTERNA+CD_CORRELATION_FLUXO+CD_CHAVE_INTEGRACAO+CD_ETAPA+DT_HORA_PROXIMA_EXECUCAO+DT_HORA_INICIO_EXECUCAO+DT_HORA_FIM_EXECUCAO" \
	--uri="getIntegracaoSCP?=DT_HORA_INICIO_EXECUCAO ge 2021-12-15T00:00:00.00Z and DT_HORA_INICIO_EXECUCAO le 2021-12-15T23:59:59.00Z" \
	--key=::checksum() \
	--partition=::dateformat(DT_HORA_INICIO_EXECUCAO,yyyyMMdd,yyyyMM)  
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
