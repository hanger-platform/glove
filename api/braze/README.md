
# Braze Extractor [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Extrator de dados armazenados em buckets do S3 

## How it works

O **Braze Extractor** permite a extração de dados da API Rest disponibilizada pela Braze.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git
- Token gerado no site da braze (https://www.braze.com/docs/api/basics/#what-is-a-rest-api)

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **braze** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **braze.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com o token gerado do site da braze, este será o seu **credentials file**:

```
{
	"authorization":"<Bearer token>"
}
```

## Utilização

```bash
java -jar braze.jar  \
	--credentials=<Arquivo de credenciais>  \
	--output=<Caminho onde arquivo será salvo> \
	--endpoint=<Nome do serviço a ser consumido, exemplo: campaigns> \
	--endpoint_list=<URL que retorna a lista a ser percorrida, exemplo:https://rest.iad-03.braze.com/campaigns/list?include_archived=true&page=<<page>> > \
	--endpoint_detail=<URL que retorna o detalhe de cada item da lista, exemplo:https://rest.iad-03.braze.com/campaigns/details?campaign_id=<<id>> > \
	--field=<Campos que serão gerados no arquivo de saída> \
	--partition=<(Opcional) Partição, dividos por + quando for mais de um> \	
	--key=<(Opcional) Chave única, dividos por + quando for mais de um> \
	--delimiter=<(Opcional) Delimitador. ';' é o padrão> \
	--sleep=<(Opcional) Tempo de espera entre uma chamada de outra. '0' é o padrão> \
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
