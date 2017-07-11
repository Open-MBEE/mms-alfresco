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
       export BASE_URL="http://localhost:8080/alfresco/service/refs/master/"
#else
#        export CURL_USER=" -u shatkhin"
#        export CURL_FLAGS=$CURL_STATUS$CURL_USER$CURL_SECURITY
#        export SERVICE_URL="https://europaems-dev-staging-a/alfresco/service/"
#       export BASE_URL="http://europaems-dev-staging-a:8443/alfresco/service/javawebscripts/"
#        export BASE_URL="https://europaems-dev-staging-a/alfresco/service/javawebscripts/"
#fi

###################################    POST CURL COMMANDS   ############################################

echo 'testPost 3'
# post views
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/viewsNew.json $BASE_URL"views"
echo
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/viewsNew.json $BASE_URL"views" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/post3.json
DIFF=$(diff baselineWorkspaces/post3.json outputWorkspaces/post3.json)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testPost 5'
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/productsNew.json $BASE_URL"sites/europa/products"
echo
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/productsNew.json $BASE_URL"sites/europa/products" | grep -v '"read":'| grep -v '"lastModified"'| grep -v '"sysmlid"' > outputWorkspaces/post5.json
DIFF=$(diff baselineWorkspaces/post5.json outputWorkspaces/post5.json)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

####################################         GET CURL COMMANDS                ###########################################

echo 'testGET2'
# get elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/300?recurse=true\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/300?recurse=true" | grep -v '"read":'| grep -v '"lastModified"' > outputWorkspaces/get2.json
java -cp .:../../src/main/amp/web/WEB-INF/lib/mbee_util.jar:../../target/mms-repo-war/WEB-INF/lib/json-20090211.jar:../../target/classes gov.nasa.jpl.view_repo.util.JsonDiff baselineWorkspaces/post2.json outputWorkspaces/post2.json  | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' | grep -v '"author"' | grep -v '"sysmlid":' | grep -v '"modified":' | grep -v '"qualifiedId"'
echo
echo

echo 'testGET3'
# get views
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/get3.json
DIFF=$(diff baselineWorkspaces/get3.json outputWorkspaces/get3.json | grep -v '"author"' | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+")
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testGET4'
# get view elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301/elements\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301/elements" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/get4.json
DIFF=$(diff baselineWorkspaces/get4.json outputWorkspaces/get4.json | grep -v '"author"' | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+")
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testGET5'
# get comments for element
#echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/303/comments\""
#curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/303/comments"  | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/get5.json
#DIFF=$(diff baselineWorkspaces/get5.json outputWorkspaces/get5.json)
#if [ "$DIFF" != "" ];then
#        failedTest=1
#        echo "$DIFF"
#fi
echo "COMMENTS TEST OMITTED FOR NOW"
echo

echo 'testGET6'
# get product
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/products/301\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/products/301"  | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/get6.json
DIFF=$(diff baselineWorkspaces/get6.json outputWorkspaces/get6.json | grep -v '"author"' | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+")
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testGET7'
# get moaproducts
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/products/3301?format=json\""
curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/products/3301?format=json" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > outputWorkspaces/get7.json
DIFF=$(diff baselineWorkspaces/get7.json outputWorkspaces/get7.json | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+")
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo


####################################        POST CHANGE CURL COMMANDS        ###########################################


echo 'testPOSTCHANGE2'
# get changed element to see if source/target changed
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/400\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/400" | grep -v '"read":' | grep -v '"lastModified"' > outputWorkspaces/postChange2.json
DIFF=$(diff baselineWorkspaces/postChange2.json outputWorkspaces/postChange2.json | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' | grep -v '"author"')
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo


####################################           SNAPSHOT CURL COMMANDS           ###########################################

# post snapshot
echo 'testSNAP1'
echo  curl -w "%{http_code}" -u admin:admin -X POST -H "Content-Type:text/html" --data @JsonData/snapshot.html http://localhost:8080/alfresco/service/ui/views/301/snapshot
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/snapshot.html $SERVICE_URL"ui/views/301/snapshot" | grep -v '"read":' | grep -v '"lastModified"' > outputWorkspaces/snap.json
#grep out other key patterns that might cause necessary diffs
#then diff the grepped files
grep -vE '"id":*' outputWorkspaces/snap.json | grep -vE '"url": "/alfresco/service/snapshots/*' | grep -vE '"created":' > baselineWorkspaces/tempSnap2.json
grep -vE '"id":*' baselineWorkspaces/snap.json | grep -vE '"url": "/alfresco/service/snapshots/*' | grep -vE '"created":' > baselineWorkspaces/tempSnap1.json
DIFF=$(diff baselineWorkspaces/tempSnap1.json baselineWorkspaces/tempSnap2.json | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' | grep -v '"creator"' | grep -vi 'time' | grep -v 'WebScriptException' | grep -v 'snapshot does not map to a')
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo

#update the configurations
#curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/configurations" > baselineWorkspaces/config2.json
#echo
#sleep 3s


####################################           ADDED CURL COMMANDS     ###########################################




exit $failedTest
