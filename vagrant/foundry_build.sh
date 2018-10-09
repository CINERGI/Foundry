#!/usr/bin/env bash

cd $HOME/Foundry
mvn clean compile assembly:single
cp dispatcher/target/foundry-dispatcher-1.0-SNAPSHOT-prod.jar $HOME/Foundry/bin/
cp consumers/target/foundry-consumers-1.0-SNAPSHOT-prod.jar $HOME/Foundry/bin/
cp ingestor/target/foundry-ingestor-1.0-SNAPSHOT-prod.jar $HOME/Foundry/bin/
mvn -Pdev clean install