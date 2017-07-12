#!/bin/bash

export CURL_STATUS="-w \"\\n%{http_code}\\n\""
export CURL_POST_FLAGS_NO_DATA="-X POST"
export CURL_POST_FLAGS="-X POST -H \"Content-Type:application/json\" --data"
export CURL_PUT_FLAGS="-X PUT"
export CURL_GET_FLAGS="-X GET"

export CURL_SECURITY=" -k -3"

#if [true]; then
    export CURL_USER=" -u admin:admin"
    export CURL_FLAGS=$CURL_STATUS$CURL_USER
    export SERVICE_URL="\"http://localhost:8080/alfresco/service/"
    export BASE_URL="\"http://localhost:8080/alfresco/service/javawebscripts/"
#else
#    export CURL_USER=" -u cinyoung"
#    export CURL_FLAGS=$CURL_STATUS$CURL_USER$CURL_SECURITY
#    export SERVICE_URL="\"http://europaems-dev-staging-a:8443/alfresco/service/"
#    export BASE_URL="\"http://europaems-dev-staging-a:8443/alfresco/service/javawebscripts/"
#    export BASE_URL="\"https://europaems-dev-staging-a:8443/alfresco/service/javawebscripts/"
#fi

echo ""
echo SNAPSHOTS

# post snapshot
echo  curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/snapshot.html $SERVICE_URL"refs/master/sites/europa/products/301/snapshots\""

# get snapshots - this currently doesn't work
#echo  curl -w "%{http_code}" -u admin:admin -X GET http://localhost:8080/alfresco/service/snapshots/301

echo ""
echo CONFIGURATIONS

# post configuration
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/configuration.json $SERVICE_URL"refs/master/sites/europa/configurations\""

# get configurations
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"refs/master/sites/europa/configurations\""

# post snapshots
echo ""
echo SNAPSHOTS
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/snapshot.html $SERVICE_URL"refs/master/sites/europa/products/301/snapshots\""
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/config.snapshots.json $SERVICEURL"refs/master/sites/europa/configurations/4ca5dac8-85a7-4598-8897-d8ab4ee38b24/snapshots"


# images
echo ""
echo ARTIFACTS
echo curl $CURL_FLAGS $CURL_POST_FLAGS_NO_DATA $SERVICE_URL"refs/master/sites/europa/artifacts/screenshot1?extension=.png&cs=200\"" -F content=@/Users/cinyoung/Desktop/screenshot1.png
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"refs/master/artifacts/screenshot1?extension=.png&timestamp=2014-06-18T10:22:32.999-0700\""
