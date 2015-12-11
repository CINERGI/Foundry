#!/bin/bash

mvn -f ../consumers/pom.xml exec:java -Dexec.mainClass="org.neuinfo.foundry.consumers.common.ProvenanceUploader" -Dexec.args="$*"

