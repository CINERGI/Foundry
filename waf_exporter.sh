#!/bin/bash

mvn -f ../ingestor/pom.xml exec:java -Dexec.mainClass="org.neuinfo.foundry.ingestor.WAFExporterCLI" -Dexec.args="$*"

#java -cp foundry-ingestor-1.0-SNAPSHOT-prod.jar org.neuinfo.foundry.ingestor.SourceIngestorCLI  $*
