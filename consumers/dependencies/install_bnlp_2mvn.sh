#!/bin/sh
mvn install:install-file -Dfile=$HOME/Foundry/consumers/dependencies/lib/bnlpkit-0.5.11.jar -DgroupId=bnlp -DartifactId=bnlpkit -Dversion=0.5.11 -Dpackaging=jar
