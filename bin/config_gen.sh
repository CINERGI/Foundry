#!/bin/bash
mvn -X -q -f ../common/pom.xml exec:java -Dexec.mainClass="org.neuinfo.foundry.common.config.ConfigGenerator" -Dexec.args="$*"
