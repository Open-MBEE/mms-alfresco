#!/bin/bash

if [[ "$#" -le 4 ]]; then
        echo "Not enough arguments"
        echo "Usage: ./test-post.sh [hostname] [user] [pass] [suiteId] (option) [planId]"
fi

export TESTRAIL_HOST=$1
export TESTRAIL_USER=$2
export TESTRAIL_PASS=$3
export TESTRAIL_SUITE_ID=$4
export TESTRAIL_PLAN_ID=$5

#mvn -Prun,robotframework,testrail install
mvn -Ptestrail install
