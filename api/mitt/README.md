# MITT Framework [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### O MITT é um framework qua facilita a geração de arquivos de texto para serem consumidos pelo glove.

## How it works

O **MITT** é reponsável por encapsular a maioria das tarefas repetitivas envolvidas na criação de extratores de dados para o módulo de arquivos do Glove. Dentre estas tarefas estão:

- Identificação automática de charset dos dados.
- Geração de arquivos como UTF 8. 
- Transformação de dados de forma simples com uma série de transformadores embarcados. 
- Geração de cli simplificada.
- Definição de valores default para os parâmetros passados para o cli. 
- Passagem de função como parâmetro na chamada do cli.
- Entre muitas outras coisas.


## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

O mitt deve ser importado como dependência nos projetos de extratores para o Glove. 

## Utilização

1. Gravação de dados de stream em uma aplicação de linha de comando. 

```java
 public static void main(String[] args){       
       //Define uma instância do Mitt. 
	   Mitt mitt = new Mitt();
	   mitt.setOutput("/tmp/mitt.csv");

        //Define os parâmetros que serão recebidos pelo cli. Neste exemplo o valor default para o parâmetro país será brasil. 
        mitt.getConfiguration().addParameter("p", "pais", "Pais", "brasil");

        CommandLineInterface cli = mitt.getCommandLineInterface(parameter.toArray(new String[0]));

        //Define a lista de campos do arquivo de saída.
        mitt.getConfiguration().addField("id");
        mitt.getConfiguration().addField("nome");
		mitt.getConfiguration().addField("pais");
		
		//Define um campo customizadado que será resultado de uma transformação.
        mitt.getConfiguration().addCustomField("etl_load_date", new Now());

		//Grava os dados no arquivo. 
        for (int i = 0; i < 10; i++) {
            List data = new ArrayList();
            data.add(i);
            data.add("nome do " + i );
			//Recupera o valor passado por parâmetro, não faz sentido neste caso mas serve para exemplificar a recuperação do valor de um parâmetro. 
			data.add(cli.getParameter("pais"));

			//Escreve os dados para o arquivo de saída. 
            mitt.write(data);
        }

		//Fecha o arquivo de saída. 
        mitt.close();
}
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
