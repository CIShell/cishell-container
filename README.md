# cishell-container
Container library for CIShell

## Build Instructions

This repository is built using maven. To build this repository, just run:
```
mvn clean install
```

## container
The container module starts up the felix framework with methods for returning the CIShell algorithms and services.

## examples
This module has 2 examples
- cishell : This has all the plugins required to start the cishell container
- sci2 : This has all the plugins required to start the sci2 container

## Starting the container
You can start the container by running the jar (org.cishell.container) inside the target folder by providing the relative path of plugins folder.

```
mvn clean install
cd examples/cishell/target
java -jar org.cishell.container-1.0.0-SNAPSHOT.jar
```
