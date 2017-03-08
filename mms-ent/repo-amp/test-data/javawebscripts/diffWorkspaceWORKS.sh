#!/bin/bash
#Script for running all of the curl commands.  Put into one script so that you can
# easily change the server/user preferences.

mkdir -p outputWorkspaces

failedTest=0

export CURL_STATUS='-w \n%{http_code}\n'
export CURL_POST_FLAGS_NO_DATA="-X POST"
export CURL_POST_FLAGS='-X POST -H Content-Type:application/json --data'
export CURL_PUT_FLAGS="-X PUT"
export CURL_GET_FLAGS="-X GET"

#export CURL_SECURITY=" -k -3"

#if [true]; then
       export CURL_USER=" -u admin:admin"
       export CURL_FLAGS=$CURL_STATUS$CURL_USER
       export SERVICE_URL="http://localhost:8080/alfresco/service/"
       export BASE_URL="http://localhost:8080/alfresco/service/workspaces/master/"
#else
#        export CURL_USER=" -u shatkhin"
#        export CURL_FLAGS=$CURL_STATUS$CURL_USER$CURL_SECURITY
#        export SERVICE_URL="https://europaems-dev-staging-a/alfresco/service/" 
#       export BASE_URL="http://europaems-dev-staging-a:8443/alfresco/service/javawebscripts/"
#        export BASE_URL="https://europaems-dev-staging-a/alfresco/service/javawebscripts/"
#fi


### ADDED CURL COMMANDS

echo 'testPost1'
# create project and site
echo curl $CURL_FLAGS $CURL_POST_FLAGS '{"name":"CY Test"}' $BASE_URL"sites/europa/projects/123456?createSite=true" 
curl $CURL_FLAGS $CURL_POST_FLAGS '{"name":"CY Test"}' $BASE_URL"sites/europa/projects/123456?createSite=true" > outputWorkspaces/post1.json
DIFF=$(diff baselineWorkspaces/post1.json outputWorkspaces/post1.json)
#if [ "$DIFF" != "" ];then
#        failedTest=1
#        echo "DIFF"
#        echo "$DIFF"
#fi
echo
echo

echo 'testGET1'
# get project - should just return 200
#echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europaTest/projects/123456\""
#curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europaTest/projects/123456"
echo
echo



exit $failedTest