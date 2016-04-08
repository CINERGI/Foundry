#!/bin/sh
mvn install:install-file -Dfile=$PWD/lib/bnlpkit-cinergi-models-0.2.jar -DgroupId=bnlp -DartifactId=bnlpkit-cinergi-models -Dversion=0.2 -Dpackaging=jar

