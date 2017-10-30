#!/bin/sh

s=
sed -i'' -e "s/db.host=/db.host=${POSTGRES_HOST}/" /usr/local/tomcat/shared/classes/alfresco-global.properties

# Configure mms
sed -i'' -e "s/pg.name=/db.host=${PG_DB_NAME}/" /usr/local/tomcat/shared/classes/mms.properties
sed -i'' -e "s/pg.user=/db.host=${PG_DB_USER}/" /usr/local/tomcat/shared/classes/mms.properties
sed -i'' -e "s/pg.pass=/db.host=${PG_DB_PASS}/" /usr/local/tomcat/shared/classes/mms.properties


# Start Alfresco
/usr/local/tomcat/bin/catalina.sh run
