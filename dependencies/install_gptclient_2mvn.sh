#!/bin/sh
mvn install:install-file -Dfile=$PWD/lib/geoportal-harvester-cli-2.6.1-SNAPSHOT.jar -DgroupId=com.esri.geoportal -DartifactId=geoportal-harvester-cli -Dversion=2.6.2-SNAPSHOT -Dpackaging=jar