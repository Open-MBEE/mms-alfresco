#!/bin/sh

# Configure alfresco-global.properties
sed -i'' -e "s/db.host=/db.host=${PG_HOST}/" /usr/local/tomcat/shared/classes/alfresco-global.properties

# Configure mms.properties file
sed -i'' -e "s/db.host=jdbc:postgresql:\/\/127.0.0.1\//db.host=jdbc:postgresql:\/\/${PG_HOST}\//" /usr/local/tomcat/shared/classes/mms.properties
sed -i'' -e "s/db.name=/db.name=${PG_DB_NAME}/" /usr/local/tomcat/shared/classes/mms.properties
sed -i'' -e "s/db.user=/db.user=${PG_DB_USER}/" /usr/local/tomcat/shared/classes/mms.properties
sed -i'' -e "s/db.pass=/db.pass=${PG_DB_PASS}/" /usr/local/tomcat/shared/classes/mms.properties

sed -i'' -e "s/elastic.host=/elastic.host=${ES_HOST}/" /usr/local/tomcat/shared/classes/mms.properties


# Start Alfresco
/usr/local/tomcat/bin/catalina.sh run
