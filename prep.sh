#!/bin/sh

mvn -Pprod clean compile assembly:single
cp dispatcher/target/foundry-dispatcher-1.0-SNAPSHOT-prod.jar bin
cp consumers/target/foundry-consumers-1.0-SNAPSHOT-prod.jar bin/
cp ingestor/target/foundry-ingestor-1.0-SNAPSHOT-prod.jar bin/
cp common/target/foundry-common-1.0-SNAPSHOT-prod.jar bin/

