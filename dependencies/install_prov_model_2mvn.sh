#!/bin/sh
mvn install:install-file -Dfile=$PWD/lib/prov-model-0.5.1-SNAPSHOT.jar -DgroupId=org.openprovenance.prov -DartifactId=prov-model -Dversion=0.5.1-SNAPSHOT -Dpackaging=jar

