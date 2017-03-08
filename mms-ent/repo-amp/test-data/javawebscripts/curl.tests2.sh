#!/bin/bash

export CURL_STATUS="-w \"\\n%{http_code}\\n\""
export CURL_POST_FLAGS_NO_DATA="-X POST"
export CURL_POST_FLAGS="-X POST -H \"Content-Type:application/json\" --data"
export CURL_PUT_FLAGS="-X PUT"
export CURL_GET_FLAGS="-X GET"

export CURL_SECURITY=" -k -3"

export CURL_USER=" -u admin:admin"
export CURL_FLAGS=$CURL_STATUS$CURL_USER
export SERVICE_URL="\"http://localhost:8080/alfresco/service/"
export BASE_URL="\"http://localhost:8080/alfresco/service/workspaces/master/"

echo POSTS

# post elements to project
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/elements.json $BASE_URL"elements\""

echo GET

# get project - should just return 200
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/303\""


