# Sisense Toolbox [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Conjunto de ferramenta para integração com o Sisense.

## How it works

O **Sisense Toolbox** facilita a execução de tarefas relacionadas com a administração e monitoramento do sisense. 

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

Utilizando o [Maven](https://maven.apache.org/):

- Acesse o diretório no qual os fontes do **sisense** se localizam.
- Digite o comando _**mvn package**_.
- O arquivo **sisense.jar** será gerado no subdiretório **_target_**.

##### CONFIGURAÇÂO

* Crie um arquivo com as seguintes informações sobre seu acesso ao servidor SFTP, este será o seu **credentials file**:

```
{
	"token":"<token>",
}
```

## Utilização

```bash
java -jar sisense.jar  \
	--credentials=<Credentials file>  \
	--server=<Sisense server> \
	--cube=<Output path> \
	--action=<Sisense API Action, accepted values: startBuild> 
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
