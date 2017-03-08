#!/bin/bash
# Bash file containing CURL commands generated from MMSCurlTestsGenerator.py. Based on curl.tests.sh and diff2.sh.

mkdir -p TestsOutput
passedTest=0

export CURL_STATUS='-w \n%{http_code}\n'
export CURL_POST_FLAGS_NO_DATA="-X POST"
export CURL_POST_FLAGS='-X POST -H Content-Type:application/json --data'
export CURL_PUT_FLAGS="-X PUT"
export CURL_GET_FLAGS="-X GET"

export CURL_USER=" -u admin:admin"
export CURL_FLAGS=$CURL_STATUS$CURL_USER
export SERVICE_URL="http://localhost:8080/alfresco/service/"



######################################## GET REQUESTS ########################################
echo
echo GET REQUESTS:

# get checklogin
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/checklogin\\"

# get demo
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/demo\\"

# get java_query
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/java_query\\"

# get info
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/logout/info\\"

# get sites
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/rest/sites\\"

# get comments?recurse={recurse?}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/rest/views/Test_viewid/comments?recurse=true,false\\"

# get {viewid}?recurse={recurse?}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/rest/views/Test_viewid?recurse=true,false\\"

# get {site}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/ve/configurations/Test_site\\"

# get {artifactId}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/workspaces/master/artifacts/Test_artifactId\\"

# get sites
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/workspaces/master/sites\\"

# get {productId}
echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL"/workspaces/master/sites/Regression_Test_Site/products/Test_productId\\"
echo

######################################## POST REQUESTS ########################################
echo
echo POST REQUESTS:

# Post {path}?cs={cs?}&amp;extension={extension?}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/artifacts/Test_path?cs=Random_CS_ID&amp;extension=Random_Extension_ID\\"

# Post demo
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/demo\\"

# Post committed
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/rest/comments/committed\\"

# Post {docid}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/rest/projects/document/Test_docid\\"

# Post delete
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/rest/projects/document/Test_docid/delete\\"

# Post delete
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/rest/projects/volume/Test_volid/delete\\"

# Post {projectid}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/rest/projects/Test_projectid\\"

# Post comments?recurse={recurse?}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/rest/views/Test_viewid/comments?recurse=true,false\\"

# Post hierarchy
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/rest/views/Test_viewid/hierarchy\\"

# Post {viewid}?force={force?}&amp;recurse={recurse?}&amp;doc={doc?}&amp;product={product?}&amp;user={user}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/rest/views/Test_viewid?force=true,false&amp;recurse=true,false&amp;doc=true,false&amp;product=true,false&amp;user=Test_user\\"

# Post {artifactId}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/workspaces/master/sites/Regression_Test_Site/artifacts/Test_artifactId\\"

# Post {productId}
echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] "/workspaces/master/sites/Regression_Test_Site/products/Test_productId\\"
echo

######################################## DELETE REQUESTS ########################################
echo
echo DELETE REQUESTS:
echo