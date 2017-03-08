#!/bin/bash
#Script for running all of the curl commands.  Put into one script so that you can
# easily change the server/user preferences.

mkdir -p output

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
       export BASE_URL="http://localhost:8080/alfresco/service/javawebscripts/"
#else
#        export CURL_USER=" -u shatkhin"
#        export CURL_FLAGS=$CURL_STATUS$CURL_USER$CURL_SECURITY
#        export SERVICE_URL="https://europaems-dev-staging-a/alfresco/service/" 
#       export BASE_URL="http://europaems-dev-staging-a:8443/alfresco/service/javawebscripts/"
#        export BASE_URL="https://europaems-dev-staging-a/alfresco/service/javawebscripts/"
#fi

###################################    POST CURL COMMANDS   ############################################

echo
echo 'testPost1'
# create project and site
echo curl $CURL_FLAGS $CURL_POST_FLAGS '{"name":"CY Test"}' $BASE_URL"sites/europa/projects/123456?fix=true&createSite=true" 
curl $CURL_FLAGS $CURL_POST_FLAGS '{"name":"CY Test"}' $BASE_URL"sites/europa/projects/123456?fix=true&createSite=true" > output/post1.json
DIFF=$(diff baselineoutput/post1.json output/post1.json)
if [ "$DIFF" != "" ];then
    failedTest=1
    echo "$DIFF"
fi
echo
echo

echo 'testPost 2'
#post elements to project
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/elementsNew.json $BASE_URL"sites/europa/projects/123456/elements" 
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/elements.json $BASE_URL"sites/europa/projects/123456/elements" | grep -v '"read":'| grep -v '"lastModified"' > output/post2.json
java -cp .:../../src/main/amp/web/WEB-INF/lib/mbee_util.jar:../../target/mms-repo-war/WEB-INF/lib/json-20090211.jar:../../target/classes gov.nasa.jpl.view_repo.util.JsonDiff baselineoutput/post2.json output/post2.json  | grep -v '"sysmlid"' | grep -v '"author"'| grep -v '}' | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+"
echo
echo

echo 'testPost 3'
# post views
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/views.json $BASE_URL"views" 
echo
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/views.json $BASE_URL"views" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > output/post3.json
DIFF=$(diff baselineoutput/post3.json output/post3.json)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testPost 4'
# post comments (can only add these to a particular view - though view isn't really checked at the moment)
echo
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/comments.json $BASE_URL"sites/europa/projects/123456/elements"  
echo
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/comments.json $BASE_URL"sites/europa/projects/123456/elements"  | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"'| grep -v '"author"' > output/post4.json
DIFF=$(diff -I 'author' baselineoutput/post4.json output/post4.json | grep -v '"author"')
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testPost 5'
# post products
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/products.json $BASE_URL"products" 
echo
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/products.json $BASE_URL"products" | grep -v '"read":'| grep -v '"lastModified"'| grep -v '"sysmlid"' > output/post5.json
DIFF=$(diff baselineoutput/post5.json output/post5.json)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

####################################         GET CURL COMMANDS                ###########################################

echo 'testGET1'
# get project - should just return 200
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/projects/123456\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"sites/europa/projects/123456"
echo
echo

echo 'testGET2'
# get elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/123456?recurse=true\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/123456?recurse=true" | grep -v '"read":'| grep -v '"lastModified"' > output/get2.jsonjava 
java -cp .:../../src/main/amp/web/WEB-INF/lib/mbee_util.jar:../../target/mms-repo-war/WEB-INF/lib/json-20090211.jar:../../target/classes gov.nasa.jpl.view_repo.util.JsonDiff baselineoutput/post2.json output/post2.json  | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' | grep -v '"author"' | grep -v '"sysmlid":'
echo
echo

echo 'testGET3'
# get views
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301_pkg\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301_pkg" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > output/get3.json
DIFF=$(diff baselineoutput/get3.json output/get3.json | grep -v '"author"' | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+")
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi 
echo 
echo

echo 'testGET4'
# get view elements
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301/elements\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"views/301/elements" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > output/get4.json
DIFF=$(diff baselineoutput/get4.json output/get4.json | grep -v '"author"' | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+")
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi 
echo
echo

echo 'testGET5'
# get comments for element
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/303/comments\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/303/comments"  | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > output/get5.json
DIFF=$(diff baselineoutput/get5.json output/get5.json)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo 
echo

echo 'testGET6'
# get product
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"products/301\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"products/301"  | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > output/get6.json
DIFF=$(diff baselineoutput/get6.json output/get6.json | grep -v '"author"' | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+")
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi 
echo
echo

echo 'testGET7'
# get moaproducts
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/products/301?format=json\""
curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/products/301?format=json" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > output/get7.json
DIFF=$(diff baselineoutput/get7.json output/get7.json | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+")
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testGET8'
# get product list
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/documents/europa?format=json\""
curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"ve/documents/europa?format=json" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > output/get8.json
DIFF=$(diff baselineoutput/get8.json output/get8.json | grep -ve '---' | egrep -v "[0-9]+[c|a|d][0-9]+")
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testGET9'
# get search
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"element/search?keyword=some*\""
#curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"element/search?keyword=some*" | grep -v '"read":'| grep -v '"lastModified"' | grep -v '"sysmlid"' > output/get9.json
#DIFF=$(diff baselineoutput/get9.json output/get9.json)
#if [ "$DIFF" != "" ];then
#        failedTest=1
#        echo "$DIFF"
#fi
echo
echo



####################################        POST CHANGE CURL COMMANDS        ###########################################

echo 'testPOSTCHANGE1'
# post changes to directed relationships only (without owners)
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/directedrelationships.json $BASE_URL"sites/europa/projects/123456/elements\""
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/directedrelationships.json $BASE_URL"sites/europa/projects/123456/elements" | grep -v '"read":' | grep -v '"lastModified"' | grep -v '"sysmlid"' > output/postChange1.json
DIFF=$(diff baselineoutput/postChange1.json output/postChange1.json | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' |grep -v '"author"')
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

echo 'testPOSTCHANGE2'
# get changed element to see if source/target changed
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/401?recurse=true\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"elements/401?recurse=true" | grep -v '"read":' | grep -v '"lastModified"' > output/postChange2.json
DIFF=$(diff baselineoutput/postChange2.json output/postChange2.json | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' | grep -v '"author"')
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
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/snapshot.html $SERVICE_URL"ui/views/301/snapshot" | grep -v '"read":' | grep -v '"lastModified"' > output/snap.json
#grep out other key patterns that might cause necessary diffs
#then diff the grepped files
grep -vE '"id":*' output/snap.json | grep -vE '"url": "/alfresco/service/snapshots/*' | grep -vE '"created":' > baselineoutput/tempSnap2.json
grep -vE '"id":*' baselineoutput/snap.json | grep -vE '"url": "/alfresco/service/snapshots/*' | grep -vE '"created":' > baselineoutput/tempSnap1.json
DIFF=$(diff baselineoutput/tempSnap1.json baselineoutput/tempSnap2.json | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' | grep -v '"creator"' | grep -vi 'time' | grep -v 'WebScriptException' | grep -v 'snapshot does not map to a')
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo

#update the configurations
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"configurations/europa" > baselineoutput/config2.json
echo
sleep 3s


####################################           CONFIGURATIONS CURL COMMANDS     ###########################################


# post configuration
echo 'testCONFIG1'
echo curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/configuration.json $BASE_URL"configurations/europa\""
curl $CURL_FLAGS $CURL_POST_FLAGS @JsonData/configuration.json $BASE_URL"configurations/europa" | grep -v '"read":' | grep -v '"lastModified"' | grep -v '"sysmlid"' > output/config1.json
DIFF=$(diff baselineoutput/config1.json output/config1.json | egrep -v "[0-9]+[c|a|d][0-9]+" | grep -ve '---' | grep -v 'time')
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo


# get configurations
echo 'testCONFIG2'
echo curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"configurations/europa\""
curl $CURL_FLAGS $CURL_GET_FLAGS $BASE_URL"configurations/europa" | grep -v '"read":' | grep -v '"modified"' | grep -v '"sysmlid"' > output/config2.json

#as long as outputs/baselines match these regex - output is conceptually correct
grep -vE '"id":*' output/config2.json | grep -vE '"url": "/alfresco/service/snapshots/*' | grep -vE '"created":'  > baselineoutput/tempConfig2_1.json
grep -vE '"read":' baselineoutput/config2.json  | grep -v '"modified"' | grep -v '"sysmlid"' | grep -vE '"id":*'| grep -vE '"url": "/alfresco/service/snapshots/*' | grep -vE '"created":'  > baselineoutput/tempConfig2_2.json
DIFF=$(diff baselineoutput/tempConfig2_2.json baselineoutput/tempConfig2_1.json)
if [ "$DIFF" != "" ];then
        failedTest=1
        echo "$DIFF"
fi
echo
echo

exit $failedTest
