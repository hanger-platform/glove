# Google Drive Manager [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Manages and extracts files from Google Drive. 

## How it works

The **Google Drive Manager** is a tool that allows the user to manage and extract files from Google Drive. There are 3 actions available in this tool, they are:
- **Copy**: Copy one file to another, if source file is shared with some people, these permissions will be copied either.
- **Import**: Import files from google drive and turn them into a csv file processed by mitt.
- **Upload**: Upload a local file into Google Drive.

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

##### Parameters

```bash
java -jar google-drive-manager.jar \
	--credentials=<Path where secret file with credentials are stored> \
	--id=<file ID> \
	--title=<(Optional)  New file title, Required for COPY> \
	--folder=<(Optional) Folder id, if null save file in my drive> \
	--action=<(Optional) Action on Google Drive; COPY is default> \
	--output=<(Optional) Output file; Required for IMPORT> \
	--properties=<(Optional) Reader properties.> \
	--field=<(Optional) Fields to be extracted from the file; Required for IMPORT> \
	--partition=<(Optional)  Partition, divided by + if has more than one field> \
	--key=<(Optional) Unique key, divided by + if has more than one field> \
	--input=<(Optional) Input file; Required for UPLOAD> \
  	--notification=<(Optional) Send notification email; COPY only; FALSE is default>
```

##### COPY
This action will copy the whole file from google drive to another, you can use this action to make a backup of your file.

```bash
java -jar /home/user_name/glove/extractor/lib/google-drive-manager.jar \
  --credentials=/home/user_name/credentials/google_drive.json \
  --action="COPY" \
  --id="<id of the google drive file to be copied>" \
  --title="this_title_will_be_the_name_of_copied_file" \
  --folder="if_you_want_to_copy_the_file_to_a_folder_put_the_folder_id_here"  
```

>If you want users to be notified by email, use the "notification=true" parameter.

##### IMPORT
This action will download one file from Google Drive and will turn it into a csv file, the gain here is that the file is processed on mitt framework, then, you can use it's transformations (https://github.com/dafiti-group/glove/tree/master/api/mitt)

```bash
java -jar /home/user_name/glove/extractor/lib/google-drive-manager.jar \
  --credentials=/home/user_name/credentials/google_drive.json \
  --id="<id of the google drive file to be copied, it can be a xls, csv or txt file.>" \
  --action="IMPORT" \
  --output="/tmp/anything/file_name.csv" \
  --field="field1+field2+field3+fieldN" \
  --partition="::fixed(2019)" \
  --key="::md5([[field1,field2]])" \
  --properties="{\"skip\":\"1\",\"sheet\":\"sheet_name\"}"
```

* **Properties** parameter is used for xls files when you need to skip some lines or need to specify the sheet to be extracted.
* **Field** parameter is used to specify the fields of the output csv file.

##### UPLOAD
This action will upload one local file to Google Drive, if folder parameter is empty, upload to My drive directory.

```bash
java -jar /home/user_name/glove/extractor/lib/google-drive-manager.jar \
  --credentials=/home/user_name/credentials/google_drive.json \
  --action="UPLOAD" \
  --folder="if_you_want_to_upload_the_file_to_a_folder_put_the_folder_id_here" \
  --input="/tmp/anything/input_file_name.any_extesion" \
  --title="title_of_uploaded_file.any_extension"
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
