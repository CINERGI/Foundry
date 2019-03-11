#!/bin/bash
today=$(date +"%b_%d_%Y")
log_file="/data/logs/dispatcher_nohup_${today}.log"
echo $log_file
nohup mvn -q -f ../dispatcher/pom.xml exec:java -Dexec.mainClass="org.neuinfo.foundry.jms.producer.PipelineMessageDispatcher"  &> $log_file

#nohup mvn -q -f ../dispatcher/pom.xml exec:java -Dexec.mainClass="org.neuinfo.foundry.jms.producer.PipelineMessageDispatcher" -Dexec.args="-c cinergi-dispatcher-cfg-pipe-stage.xml" &> $log_file &

#nohup mvn -f ../dispatcher/pom.xml exec:java -Dexec.mainClass="org.neuinfo.foundry.jms.producer.OplogMessageDispatcher" -Dexec.args="-c cinergi-dispatcher-cfg-pipe-stage.xml" &> $log_file &


#java -cp foundry-dispatcher-1.0-SNAPSHOT-prod.jar org.neuinfo.foundry.jms.producer.OplogMessageDispatcher -c cinergi-dispatcher-cfg-pipe-stage.xml  $*
#nohup java -cp foundry-dispatcher-1.0-SNAPSHOT-prod.jar org.neuinfo.foundry.jms.producer.OplogMessageDispatcher -c cinergi-dispatcher-cfg-pipe-stage.xml &> $log_file &

