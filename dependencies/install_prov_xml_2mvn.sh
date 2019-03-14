#!/bin/sh
mvn install:install-file -Dfile=$PWD/lib/prov-xml-0.5.1-SNAPSHOT.jar -DgroupId=org.openprovenance.prov -DartifactId=prov-xml -Dversion=0.5.1-SNAPSHOT -Dpackaging=jar

