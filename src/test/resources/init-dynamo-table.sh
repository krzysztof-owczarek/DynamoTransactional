#!/usr/bin/env bash
set -x
awslocal dynamodb create-table \
--table-name TestTable \
--attribute-definitions \
    AttributeName=partitionKey,AttributeType=S \
--key-schema \
    AttributeName=partitionKey,KeyType=HASH \
--provisioned-throughput \
    ReadCapacityUnits=10,WriteCapacityUnits=10