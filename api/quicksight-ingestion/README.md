# Quicksight Ingestion
### SPICE ingestion for a dataset

## How it works

Quicksight is a Business Intelligence tool that is inside AWS Cloud. It delivers an API who can manage resources inside the tool. This API is powerfull and allows to make almost every actions of AWS Quicksight. (Documentation: https://docs.aws.amazon.com/pt_br/quicksight/latest/APIReference/Welcome.html).
Quicksight ingestion is a program that creates and starts a new SPICE ingestion for a dataset. 

## Installation

##### REQUIREMENTS

- Java 8 +
- Maven
- Git
- CLI AWS
- Permission in the AWS Quicksight resources.

##### CONSTRUCTION

Using [Maven](https://maven.apache.org/):

- Go to directory that has **quicksight-ingestion** source codes.
- Type command _**mvn package**_.
- The file **quicksight-ingestion.jar** will be created in the subdirectory **_target_**.

##### CONFIGURAÇÃO

* AWS CLI should be installed and configured with keys **AWS Secret Access Key** and **AWS Access Key**, the can be found in AWS console.

## Using

```bash
java -jar quicksight-ingestion.jar  \
	--account=<The Amazon Web Services account ID> \
	--dataset=<The ID of the dataset used in the ingestion> \
	--type=<(Optional) The type of ingestion that you want to create, available: INCREMENTAL_REFRESH and FULL_REFRESH (default)> \
	--sleep=<(Optional) Sleep time in seconds at one request and another; 10 is default>
```

##### EXAMPLE

In this example, we run a dataset with incremental refresh.

```bash
java -jar quicksight.jar \
	--account='276421814571' \
	--dataset='2c8j37a6-3f5e-4c41-a850-9b3se466b116' \
	--type='INCREMENTAL_REFRESH'
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
