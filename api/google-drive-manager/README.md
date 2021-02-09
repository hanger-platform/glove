# Google Drive Manager [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Manager files in Google Drive. 

## How it works

The **Google Drive Manager** is a tool that allows to manager files from Google Drive. 
By default, you can copy a file to a new folder.

## Install

##### REQUIREMENTS

- Java 8 +
- Maven
- Git
- You must have an _Google Cloud Platform_ account.
- Activate the Google Drive library:
    - Access [https://console.cloud.google.com](https://console.cloud.google.com/)
    - _APIs_ and services
    - Library
    - Search for '_Google Drive API_**'**
    - Se _API_ não estiver ativada, ative-a.

- Create access credentials to the _API_:
    - Access the Google Developer Console API [https://console.developers.google.com/apis](https://console.developers.google.com/apis)
    - Click on **Credentials** menu.
    - Click on **Create credentials**. 
    - Click on create **OAuth client ID**.
    - Select the option **Others**
    - Define a **name** and click on **create**.
    - Download the JSON file.     

##### CONSTRUCTION

- Using [Maven](https://maven.apache.org/): 
    - Access the directory where the ***Google Drive Manager*** sources are located.
    - Type the _**mvn package**_ command.
    - The **google-drive-manager.jar** file will be generated on **_target_** subdirectory.

## Utilization

```bash
java -jar google-drive-manager.jar \
	--credentials=<Identifica o caminho onde o arquivo secreto com as credenciais está localizado> \
	--id=<Identifies the file ID that will be copied> \
	--title=<Identifies the new file name> \
	--folder=<Identifies the folder ID where the file will be copied> \
	--action=<Identifies the syste action. By default, the action is COPY> \
	--output=<Identifies the output file. It is required to use IMPORT action> \
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
