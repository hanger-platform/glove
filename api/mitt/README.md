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
- Suporte para arquivos csv, zip, gz, avro ou xls/xlsx.

## Instalação

##### REQUISITOS

- Java 8 +
- Maven
- Git

##### CONSTRUÇÃO

O MITT deve ser importado como dependência nos projetos de extratores para o Glove. 

## Utilização

O MITT trabalha com dois tipos de entrada, _stream_ ou _file_, estes são explicados adiante. 

#### Stream

Nessa opção é possível efetuar a gravação de forma iterativa através de um _loop_.

**Exemplo**: Neste caso, é apresentada a gravação de dados de stream em uma aplicação de linha de comando. 

```java
public static void main(String[] args){       
		Mitt mitt = new Mitt();
		
		//Defines the command line interface expected parameters. 
		mitt.getConfiguration().addParameter("p", "pais", "Pais", "brasil");
		
		//Gets a instance of MITT CommandLineInterface class. 
		CommandLineInterface cli = mitt.getCommandLineInterface(args);
		
		//Defines the output file. 
		mitt.setOutputFile("/tmp/mitt.csv");

		//Defines default output fields. 
		mitt.getConfiguration()
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

No exemplo acima o arquivo de saída seria:

```
0;nome do 0;brasil;2021-07-27 10:54:56
1;nome do 1;brasil;2021-07-27 10:54:56
2;nome do 2;brasil;2021-07-27 10:54:56
3;nome do 3;brasil;2021-07-27 10:54:56
4;nome do 4;brasil;2021-07-27 10:54:56
5;nome do 5;brasil;2021-07-27 10:54:56
6;nome do 6;brasil;2021-07-27 10:54:56
7;nome do 7;brasil;2021-07-27 10:54:56
8;nome do 8;brasil;2021-07-27 10:54:56
9;nome do 9;brasil;2021-07-27 10:54:56
```

#### File

Nessa opção é possível efetuar a gravação do arquivo de saída no padrão Glove a partir de um arquivo de entrada. Tipos de arquivos aceitos são:
* csv 
* zip 
* gz 
* avro 
* xls
* xlsx

**Exemplo**: Neste caso, é apresentado um arquivo csv de entrada e a gravação no arquivo de saída é feito em uma aplicação de linha de comando. 

```java
public static void main(String[] args) throws DuplicateEntityException, IOException {
        //Write an input csv file.
        FileWriter inputFile = new FileWriter("/tmp/mitt_test/input_file.csv");
        inputFile.write("id;name;birthday\n");
        inputFile.write("1;helio;1990-11-27\n");
        inputFile.write("2;val;1984-02-17\n");
        inputFile.write("3;saga;1987-03-15\n");
        inputFile.close();

        Mitt mitt = new Mitt();

        //Defines the output file. 
        mitt.setOutputFile("/tmp/output_file.csv");

        //Defines default output fields. 
        mitt.getConfiguration()
                .addField("hash::checksum()")
                .addField("year::dateformat(birthday,yyyy-MM-dd,yyyy)")
                .addField("id")
                .addField("name")
                .addField("birthday");

        //Defines custom fields, based on transformations. 
        mitt.getConfiguration().addCustomField("etl_load_date", new Now());

        //Writes output file.
        mitt.write(new File("/tmp/mitt_test/"));

        mitt.close();
    }
```

Arquivo de entrada:

```
id;name;birthday
1;helio;1990-11-27
2;val;1984-02-17
3;saga;1987-03-15
```

Arquivo de saída:

```
hash;year;id;name;birthday;etl_load_date
1E6B9BBEA2C6F69C9E642AB2639CAA6F;1990;1;helio;1990-11-27;2021-07-27 11:52:21
FD9353CCE431917E26D3D3F76017DA0D;1984;2;val;1984-02-17;2021-07-27 11:52:21
7E23C75C9A6E0A02F26A32F1AB910B04;1987;3;saga;1987-03-15;2021-07-27 11:52:21

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
| **FileLastModified**| Retorna a data de modificação do arquivo, quando o input for um arquivo  | ::fileLastModified() |
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
| **At**| Retorna o valor em uma posição específica de um campo original iniciando da posição 0 | ::At(<posisão\>) |
| **FileSize**| Retorna o tamanho do arquivo em bytes, quando o input for um arquivo  | ::fileSize() |
| **RowNumber**| Retorna o número da linha | ::RowNumber() ou ::RowNumber([<campo\>,<campo\>,..]), quando um ou mais campos forem informados a função retornará o mesmo número de linha para o mesmo valor de campo]) |
| **NumberFormat**| Retorna um número a partir de uma representação em string em um formato diferente de americano | ::numberFormat(<campo\>,<idioma\>,<país\>). Ex.: Para transforma a string -1.066,68 em um número, devemos usar ::numberFormat(value,pt,BR). Para maior parte dos casos, pode ser utilizada uma desta configurações: (123 456) fr FR, (345 987,246) fr FR, (123.456) de DE, (345.987,246) de DE, (123,456) en US e (345,987.246) en US|

##### RESERVED CHARACTERES

Quando utilizamos uma transformação no MITT, muitas vezes é necessário informar não um único campo, mas uma lista de campos e, em outras situações específica, alguns valores não podem ser considerados pelo processador do MITT. Para estas situações temos alguns caracteres reservados que indicam ao processador do MITT como a informação passada deve ser processada. 

| Type| Example |
|--|--|
| **List** | **[[**<campo\>,<campo\>**]]** |
| **Unparsable** | **\*\***<expressão\>**\*\*** |
| **Rename** | <field\>**\>\>**<alias\> |

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
