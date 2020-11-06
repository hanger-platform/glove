
# RTB House Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados armazenados em servidores da RTB House

## How it works

O **RTB House Extractor** permite a extração de dados da API Rest disponibilizada pela RTB House (https://panel.rtbhouse.com/api/docs).

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Usuário e senha administrador no site da RTB House (https://panel.rtbhouse.com/)

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **rtb-house** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **rtb-house.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as credenciais de acesso, este será o seu **credentials file**:

```
{
  "login": "<<e-mail do usuário administrador>>",
  "password": "<<senha>>",
  "url": "<<Servidor onde seus dados estão armazenados, verificar em: https://panel.rtbhouse.com/api/docs>>"
}

```

## Utilização

```bash
java -jar rtb-house.jar  \
	--credentials=<Arquivo de credenciais>  \
	--output=<Caminho onde arquivo será salvo> \
	--advertiser=<Nome do advertiser, dividos por + quando for mais de um. Exemplo: "BR_Dafiti+BR_Dafiti_InApp"> \
	--field=<Campos que serão gerados no arquivo de saída> \
	--start_date=<Data início> \
	--end_date=<Data fim>  \
	--partition=<(Opcional) Partição, dividos por + quando for mais de um> \
	--key=<(Opcional) Chave única, dividos por + quando for mais de um> \
	--uri_filter=<URI do endpoint que deseja que  os dados sejam extraídos, as nomenclaturas <<>>, <<>>, <<>> serão macro substituídas pelo advertiser informado no parâmetro acima 'advertiser', data início pelo 'start_date' e data fim pelo 'end_date' respectivamente. As opções <<>> não são obrigatórias. Exemplo: '/advertisers/<<advertiser>>/summary-stats?dayFrom=<<start_date>>&dayTo=<<end_date>>&groupBy=subcampaign&metrics=campaignCost'">
```


## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
