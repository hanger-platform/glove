
# MITT Framework [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### O MITT é um framework utilizado para criação de conectores para o Glove.

## How it works

O **MITT** é reponsável por encapsular a maioria das tarefas repetitivas envolvidas na criação de extratores de dados para o módulo de arquivos do Glove. Dentre estas tarefas estão:

- Identificação automática de charset dos dados.
- Geração de arquivos como UTF 8. 
- Transformação de dados de forma simples com uma série de transformadores embarcados. 
- Geração de *command line interface* simplificada.
- Definição de valores default para os parâmetros passados para a *command line interface*. 
- Passagem de função como parâmetro na chamada do *command line interface*.
- Suporte para arquivos csv, gz, zip ou avro.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

O MITT deve ser importado como dependência nos projetos de extratores para o Glove. 

## Utilização

1. Neste exemplo, é apresentada a gravação de dados de stream em uma aplicação de linha de comando. 

```java
 public static void main(String[] args){       
	    Mitt mitt = new Mitt();
		
		//Defines the command line interface expected parameters. 
        mitt
	        .getConfiguration()
		        .addParameter("p", "pais", "Pais", "brasil");
		
		//Gets a instance of MITT CommandLineInterface class. 
        CommandLineInterface cli = mitt.getCommandLineInterface(args);
		
		//Defines the output file. 
		mitt.setOutputFile("/tmp/mitt.csv");

        //Defines default output fields. 
        mitt
	        .getConfiguration()
			    .addField("id")
			    .addField("nome")
				.addField("pais");
		
		//Defines custom fields, based on a transformation. 
        mitt.getConfiguration().addCustomField("etl_load_date", new Now());

		//Writes to the output file. 
        for (int i = 0; i < 10; i++) {
            List data = new ArrayList();
            data.add(i);
            data.add("nome do " + i );
			data.add(cli.getParameter("pais"));
			
            mitt.write(data);
        }
        mitt.close();
}
```

## Transformations

Uma das principais características do MITT é a possibilidade de transformação dos dados. Desta forma, o usuário de um extrator construído com o *framework* pode aplicar uma das várias transformações disponíveis em qualquer campo que estiver sendo gerado por um extrator, bem como adicionar novos campos a partir de campos existentes ou simplesmente criar um campo a partir de uma transformação. 

##### EMBEDDED TRANSFORMATIONS

| Transformation| Description| Example |
|--|--|--|
| **Concat** | Concatena dois ou mais campos | ::concat([[<campo\>,<campo\>]])|
| **DateFormat**| Formata um campo do tipo data | ::dateformat(<campo\>,[formato_de_entrada, para formato de entrada Unix Timestamp informar neste parâmetro UNIXTIME e caso o formato esteja em milissegundos informar UNIXTIMEMILLIS],<formato_de_saída\>)|
| **Eval**| Transforma um campo usando funções do JavaScript | ::eval(\*\*<campo\>.replace('A','B')\*\*) |
| **FarmFingerprint**| Aplica a função de hash FarmFingerprint em um ou mais campos | ::farmfingerprint([[<campo\>,<campo\>]]) |
| **FileName**| Retorna o nome do arquivo que está sendo processado, quando o input for um arquivo  | ::filename() |
| **Fixed**| Retorna um valor fixo | ::fixed(oi) |
| **MD5**| Aplica a função de hash MD5 em um ou mais campos | ::md5([[<campo\>,<campo\>]])|
| **Now**| Retorna a data e hora corrente | ::now() |
| **RegExp**| Extrai parte da informação de um campo usando RegExp | ::regexp(<campo\>,<regex\>) |
| **Checksum**| Gera um hash utilizando MD5, SHA1 ou FARM_FINGERPRINT para a combinação de todos os campos originais de um registro | ::checksum(<algorítimo\>) |
| **Upper**| Retorna um valor em caixa alta | ::upper(<campo\>) |
| **Lower**| Retorna um valor em caixa baixa | ::lower(<campo\>) |
| **SplitPart**| Divide uma string no delimitador informado e retorna a parte na posição especificada | ::splitpart(<campo\>,<delimitador\>,<posição\>) |
| **Replace**| Substitui todas as ocorrências de um conjunto de caracteres em uma string existente por outros caracteres especificados | ::replace(<campo\>,<valor a substituir\>,<novo valor\>) |
| **Trim**| Remove os espaços em branco iniciais e finais, ou pode remover apenas os iniciais ou apenas os finais | ::trim(<campo\>,[(Opcional) Para limpar apenas os iniciais informar LTRIM, para limpar apenas os finais informar RTRIM]) |
| **JsonPath**| Retorna o valor para o par de valor:chave referenciado por um JSON | ::jsonpath(<campo\>,<valor:chave, exemplo: $['book']['title']\>) ou em alguns casos pode estourar erro no console ao parsear algum campo, senão quiser apresentar o erro no console pode usar a função da seguinte maneira: ::jsonpath(<campo\>,<valor:chave, exemplo: $['book']['title']\>,<apresenta erros no console, true sim, false não; exemplo: false>) |


##### RESERVED CHARACTERES

Quando utilizamos uma transformação no MITT, muitas vezes é necessário informar não um único campo, mas uma lista de campos e, em outras situações específica, alguns valores não podem ser considerados pelo processador do MITT. Para estas situações temos alguns caracteres reservados que indicam ao processador do MITT como a informação passada deve ser processada. 

| Type| Example |
|--|--|
| **List** | **[[**<campo\>,<campo\>**]]** |
| **Unparsable** | **\*\***<expressão\>**\*\*** |

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
