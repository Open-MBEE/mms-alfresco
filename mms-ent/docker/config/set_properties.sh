#!/bin/sh

# Configure alfresco-global.properties
if [[ -z "$PG_HOST" ]];then
    echo "Postgres host not set"
    exit 1
fi
if [[ -z "$PG_DB_NAME" ]];then
    echo "Postgres Database name not set"
    exit 1
fi
if [[ -z "$PG_DB_USER" ]];then
    echo "Postgres Database User not set"
    exit 1
fi
if [[ -z "$PG_DB_PASS" ]];then
    echo "Postgres Database User password not set"
    exit 1
fi
if [[ -z "$ES_HOST" ]];then
    echo "ElasticSearch host not set"
    exit 1
fi

sed -i'' -e "s/db.host=/db.host=${PG_HOST}/" /usr/local/tomcat/shared/classes/alfresco-global.properties

# Configure mms.properties file
sed -i'' -e "s/pg.host=jdbc:postgresql:\/\/127.0.0.1\//pg.host=jdbc:postgresql:\/\/${PG_HOST}\//" /usr/local/tomcat/shared/classes/mms.properties
sed -i'' -e "s/pg.name=/pg.name=${PG_DB_NAME}/" /usr/local/tomcat/shared/classes/mms.properties
sed -i'' -e "s/pg.user=/pg.user=${PG_DB_USER}/" /usr/local/tomcat/shared/classes/mms.properties
sed -i'' -e "s/pg.pass=/pg.pass=${PG_DB_PASS}/" /usr/local/tomcat/shared/classes/mms.properties

sed -i'' -e "s/elastic.host=http:\/\/localhost/elastic.host=http:\/\/${ES_HOST}/" /usr/local/tomcat/shared/classes/mms.properties
