#!/bin/sh
mvn install:install-file -Dfile=$PWD/lib/prov-json-0.5.1-SNAPSHOT.jar -DgroupId=org.openprovenance.prov -DartifactId=prov-json -Dversion=0.5.1-SNAPSHOT -Dpackaging=jar

