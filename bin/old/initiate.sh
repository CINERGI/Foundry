#!/bin/bash

./bit_bucket_consumer.sh foundry.new
./bit_bucket_consumer.sh foundry.indexCheckpoint
./bit_bucket_consumer.sh foundry.index

./mongo_utils.cli.sh -rf -s


./ingest_src_cli.sh -p nlx_999999.properties
