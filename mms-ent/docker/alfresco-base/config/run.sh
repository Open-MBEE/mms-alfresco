#!/bin/sh

s=
sed -i'' -e "s/db.host=/db.host=${POSTGRES_HOST}/" /usr/local/tomcat/shared/classes/alfresco-global.properties

# Start Alfresco
/usr/local/tomcat/bin/catalina.sh run
