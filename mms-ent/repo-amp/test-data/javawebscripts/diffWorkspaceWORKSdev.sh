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

echo
echo 'testPost1'
# create project and site
echo curl $CURL_FLAGS $CURL_POST_FLAGS '{"name":"CY Test"}' $BASE_URL"sites/europa/projects/123456?createSite=true" 
curl $CURL_FLAGS $CURL_POST_FLAGS '{"name":"CY Test"}' $BASE_URL"sites/europa/projects/123456?createSite=true" > outputWorkspaces/post1.json
DIFF=$((diff baselineWorkspaces/post1.json outputWorkspaces/post1.json) 2>&1)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testPost 2'
pwd
#post elements to project
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/elementsNew.json $BASE_URL"elements" 
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/elements.json $BASE_URL"elements" | grep -v '"read":'| grep -v '"lastModified"' > outputWorkspaces/post2.json
DIFF=$((java -cp .:../../../../.m2/repository/gov/nasa/jpl/mbee/util/mbee_util/0.0.16/mbee_util-0.0.16.jar:../../target/mms-repo-war/WEB-INF/lib/json-20090211.jar:../../target/classes gov.nasa.jpl.view_repo.util.JsonDiff baselineWorkspaces/post2.json outputWorkspaces/post2.json  | grep -v '"sysmlid"' | grep -v '"author"'| grep -v '}' | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -v '"modified":' | grep -v '"qualifiedId”') 2>&1)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo


echo 'testGET1'
# get project - should just return 200
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/projects/123456\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/projects/123456" > outputWorkspaces/get1.json
DIFF=$((diff baselineWorkspaces/get1.json outputWorkspaces/get1.json) 2>&1)
if [ "$DIFF" != "" ];then
    failedTest=1
    echo "$DIFF"
fi
echo
echo

echo 'testGET8'
# get product list
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/documents/europa?format=json\""
curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/documents/europa?format=json" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/get8.json
DIFF=$((java -cp .:../../../../.m2/repository/gov/nasa/jpl/mbee/util/mbee_util/0.0.16/mbee_util-0.0.16.jar:../../target/mms-repo-war/WEB-INF/lib/json-20090211.jar:../../target/classes gov.nasa.jpl.view_repo.util.JsonDiff baselineWorkspaces/get8.json outputWorkspaces/get8.json  | grep -v '"sysmlid"' | grep -v '"author"'| grep -v '}' | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -v '"modified":' | grep -v '"qualifiedId”') 2>&1)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo


echo 'testPOSTCHANGE1'
# post changes to directed relationships only (without owners)
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/directedrelationships.json $BASE_URL"sites/europa/projects/123456\""
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/directedrelationships.json $BASE_URL"sites/europa/projects/123456" | grep -v '"read":' | grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/postChange1.json
DIFF=$((diff baselineWorkspaces/postChange1.json outputWorkspaces/postChange1.json | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' |grep -v '"author"') 2>&1)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo



exit $failedTest
