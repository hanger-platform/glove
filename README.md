# Glove [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### A modular platform designed to automate data integration processes

## How it works
Glove's main goal is to automate data integration processes in a modular fashion. 

## Instalation
Glove depends on some third-party softwares, that need to be installed to have it up and running: 

* Linux

* Open JDK 8

* Git 

* Tomcat 8 +

* Google Cloud SDK

* AWS Cli

* Jenkins ( https://jenkins.io/ )

* Java 8 +

* Pentaho Data Integration 8 +

* Parallel ( https://www.gnu.org/software/parallel/ )

* jq (https://stedolan.github.io/jq/)

* pigz ( https://zlib.net/pigz/ )

## Configuration

* Clone the GLOVE project.

* Creates a ~/.kettle/kettle.properties file with the following content:

```
# Core
GLOVE_HOME=<Glove files folder>
GLOVE_TEMP=<temporary files folder>
GLOVE_METADATA=<glove metadata files folder>
GLOVE_NAMED_QUERIES=<Glove named queries files folder>

# Redshift
REDSHIFT_JDBC_HOST=<Host>
REDSHIFT_JDBC_PORT=<Port>
REDSHIFT_JDBC_DB=<Database>
REDSHIFT_JDBC_USER_NAME=<User>
REDSHIFT_JDBC_PASSWORD=<Password>
 
# Redshift Spectrum
SPECTRUM_ROLE=<Role>
 
# Google Cloud SDK
GOOGLE_TOOLS_HOME=<Google Cloud SDK path>
```

* Create a .kettle/connection.properties in the home directory with the following structure:

```
CONNECTION_NAME;JDBC_URL;JDBC_DRIVER_CLASS;DB_HOST;DB_PORT;DB_DATABASE;DB_USER;DB_PASSWORD;DATABASE_TYPE
```

## Contributing, Bugs, Questions
Contributions are more than welcome! If you want to propose new changes, fix bugs or improve something feel free to fork the repository and send us a Pull Request. You can also open new `Issues` for reporting bugs and general problems.
