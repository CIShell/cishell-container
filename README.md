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

## Build Infrastructure

Build infrastructure for CIShell and Sci2 generously provided by the following open source patrons:

[![GitHub](https://cishell.github.io/images/GitHub_Logo.png)](https://github.com/CIShell/)
[![Travis CI](https://cishell.github.io/images/TravisCI-Full-Color.png)](https://travis-ci.com/CIShell/)
[![JFrog Artifactory](https://cishell.github.io/images/Powered-by-artifactory_03.png)](https://cishell.jfrog.io)
