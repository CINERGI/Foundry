#!/bin/bash
mvn -f ../dispatcher/pom.xml exec:java -Dexec.mainClass="org.neuinfo.foundry.jms.producer.ManagementService" -Dexec.args="-c cinergi-dispatcher-cfg.xml $*"

#java -cp foundry-dispatcher-1.0-SNAPSHOT-prod.jar org.neuinfo.foundry.jms.producer.ManagementService $*
