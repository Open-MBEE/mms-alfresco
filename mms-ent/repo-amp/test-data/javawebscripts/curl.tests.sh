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
#    export SERVICE_URL="\"https://ems-test.jpl.nasa.gov/alfresco/service/"
#    export BASE_URL="\"https://ems-test.jpl.nasa.gov/alfresco/service/javawebscripts/"
#    export CURL_USER=" -u shatkhin"
#    export CURL_FLAGS=$CURL_STATUS$CURL_USER$CURL_SECURITY
#    export SERVICE_URL="\"http://europaems-dev-staging-a:8443/alfresco/service/"
#    export BASE_URL="\"http://europaems-dev-staging-a:8443/alfresco/service/javawebscripts/"
#fi

# TODO: CURL commands aren't executed from bash using environment variables
echo POSTS
# create project and site
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/project.json $SERVICE_URL"workspaces/master/sites/europa/projects?createSite=true\""

# post elements to project
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/elementsNew.json $SERVICE_URL"workspaces/master/sites/europa/elements\""

# post views
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/views.json $BASE_URL"views\""

# post products
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/products.json $BASE_URL"products\""


echo ""
echo GET
# get project - should just return 200
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/projects/123456\""

# get elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/123456?recurse=true\""

# get views
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301\""

# get view elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301/elements\""

# get product
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"products/301\""

# get moaproducts
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/products/301?format=json\""

# get product list
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/documents/europa?format=json\""

# TODO - fix these URLs? change search to just search
# get search
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"element/search?keyword=some*\""

# get multiple elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"workspaces/master/elements\"" -d @JsonData/elementsGet.json -H "\"Content-Type:application/json\""

echo ""
echo DELETE
# post new element
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/elementsNewTest.json $SERVICE_URL"workspaces/master/elements\""
# get elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"workspaces/master/elements/771\""
# delete elements
echo curl $CURL_FLAGS -X DELETE $SERVICE_URL"workspaces/master/elements/771\""
# get elements (should get 404)
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"workspaces/master/elements/771\""


echo ""
echo POST changes

# post changes to directed relationships only (without owners)
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/directedrelationships.json $BASE_URL"sites/europa/projects/123456/elements\""

# get changed element to see if source/target changed
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/400\""

echo ""
echo SNAPSHOTS

# post snapshot
echo curl $CURL_FLAGS $CURL_POST_FLAGS_NO_DATA $SERVICE_URL"workspaces/master/sites/europa/products/_17_0_5_1_407019f_1402422683509_36078_16169/snapshots\""
#echo  curl -w "%{http_code}" -u admin:admin -X POST -H "Content-Type:text/html" --data @JsonData/snapshot.html http://localhost:8080/alfresco/service/ui/views/_17_0_5_1_407019f_1402422683509_36078_16169/snapshot
echo curl $CURL_FLAGS $CURL_POST_FLAGS_NO_DATA $SERVICE_URL"workspaces/master/sites/undefined/products/_17_0_5_1_407019f_1402422683509_36078_16169/snapshots\""
#echo  curl -w "%{http_code}" -u admin:admin -X POST -H "Content-Type:application/json" --data @JsonData/snapshots.json http://localhost:8080/alfresco/service/workspaces/master/sites/europa/configurations/_17_0_5_1_407019f_1402422683509_36078_16169/snapshots
echo curl $CURL_FLAGS $CURL_POST_FLAGS_NO_DATA $SERVICE_URL"workspaces/master/sites/europa/products/_17_0_5_1_407019f_1402422683509_36078_16169\""
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/configuration.json $SERVICE_URL"workspaces/master/sites/europa/configurations\""
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/configuration.products.json $SERVICE_URL"workspaces/master/sites/europa/configurations/[CONFIGURATION_ID]/products\""
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"workspaces/master/sites/europa/configurations/[CONFIGURATION_ID]/snapshots\""

# get snapshots - this currently doesn't work
#echo  curl -w "%{http_code}" -u admin:admin -X GET http://localhost:8080/alfresco/service/snapshots/301

echo ""
echo CONFIGURATIONS

# post configuration
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/configuration.json $BASE_URL"configurations/europa\""

# get configurations
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"configurations/europa\""

echo ""
echo WORKSPACES
echo curl $CURL_FLAGS -X POST $SERVICE_URL"workspaces/wsA?sourceWorkspace=master\""

echo curl $CURL_FLAGS -X POST $SERVICE_URL"workspaces/wsB?sourceWorkspace=wsA\""

echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"workspaces\""

