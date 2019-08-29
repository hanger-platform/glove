# Google Play Store Extractor [![GitHub license](https://img.shields.io/github/license/dafiti/causalimpact.svg)](https://bitbucket.org/dafiti/bi_dafiti_group_nick/src/master/license)
### Extrai informações da API do Google Play Store

## How it works

O **Google Play Store Extractor** possibilita a extração de dados dos relatórios mensais do Google Play Store para um arquivo csv no formato aceito pelo Glove.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Criação de uma conta de serviço no _Google Cloud Platform_ caso não exista.
- Ativar o recurso Storage: 
    - Acessar [https://console.cloud.google.com](https://console.cloud.google.com/)
    - Menu de navegação
    - APIs e serviços
    - Biblioteca
    - Buscar por _Cloud Storage_
    - Ativar API.

- Criar credenciais de acesso para _API_
    - Acessar [https://console.cloud.google.com](https://console.cloud.google.com/)
    - _APIs_ e serviços
    - Credenciais
    - Criar credenciais
    - Chave de conta de serviço
    - Escolher tipo _JSON_
    - Guardar arquivo _JSON_ gerado

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **_Google Play Store Extractor_ **se localizam.
- Digite o comando _**mvn package**_.
- O arquivo google-play-store.jar será gerado no subdiretório _target_.

## Utilização

```bash
java -jar google-play-store.jar \
	--json_key_path=<O caminho onde o arquivo secreto com as credenciais está localizado> \
	--output=<O caminho e nome do arquivo que será gerado> \
	--bucket=<Identificador da conta do Google Cloud Storage> \
	--path=<Caminho de extração dos dados dentro do Google Cloud Storage> \
	--package_name=<Identifica o nome do pacote a ser buscado> \
	--start_date=<Data inicial do período de data, filtrando conforme a data de modificação do arquivo no Google Cloud Storage> \
	--end_date=<Data final do período de data, filtrando conforme a data de modificação do arquivo no Google Cloud Storage> \
	--field=<Campos a serem extraídos, separados por +> \
	--partition=<Identifica o campo que será utilizado para particionamento dos dados> \
	--dimension=<Identifica a dimensão que será extraída>
```

## Referências

1. Play Console Help: [https://support.google.com/googleplay/android-developer/answer/6135870?hl=en](https://support.google.com/googleplay/android-developer/answer/6135870?hl=en)

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
