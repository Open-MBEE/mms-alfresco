#!/bin/sh

# Configure alfresco-global.properties and mms.properties
if [[ -z "$PG_HOST" ]];then
    echo "Postgres host not set"
    exit 1
else
    sed -i'' -e "s/db.host=/db.host=${PG_HOST}/" /usr/local/tomcat/shared/classes/alfresco-global.properties
    if [[ -z "$PG_PORT" ]];then
        echo "Postgres port not set, using default 5432"
        sed -i'' -e "s/pg.host=jdbc:postgresql:\/\/127.0.0.1\//pg.host=jdbc:postgresql:\/\/${PG_HOST}\//" /usr/local/tomcat/shared/classes/mms.properties
    else
        sed -i'' -e "s/db.port=5432/db.port=${PG_PORT}/" /usr/local/tomcat/shared/classes/alfresco-global.properties
        sed -i'' -e "s/pg.host=jdbc:postgresql:\/\/127.0.0.1\//pg.host=jdbc:postgresql:\/\/${PG_HOST}:${PG_PORT}\//" /usr/local/tomcat/shared/classes/mms.properties
    fi
fi
if [[ -z "$PG_DB_NAME" ]];then
    echo "Postgres Database name not set"
    exit 1
else
    sed -i'' -e "s/pg.name=/pg.name=${PG_DB_NAME}/" /usr/local/tomcat/shared/classes/mms.properties
fi
if [[ -z "$PG_DB_USER" ]];then
    echo "Postgres Database User not set"
    exit 1
else
    sed -i'' -e "s/db.username=/db.username=${PG_DB_USER}/" /usr/local/tomcat/shared/classes/alfresco-global.properties
    sed -i'' -e "s/pg.user=/pg.user=${PG_DB_USER}/" /usr/local/tomcat/shared/classes/mms.properties
fi
if [[ -z "$PG_DB_PASS" ]];then
    echo "Postgres Database User password not set"
    exit 1
else
    sed -i'' -e "s/db.password=/db.password=${PG_DB_PASS}/" /usr/local/tomcat/shared/classes/alfresco-global.properties
    sed -i'' -e "s/pg.pass=/pg.pass=${PG_DB_PASS}/" /usr/local/tomcat/shared/classes/mms.properties
fi
if [[ -z "$ES_HOST" ]];then
    echo "Elasticsearch host not set"
    exit 1
else
    if [[ -z "$ES_PORT" ]];then
        echo "Elasticsearch port not set, using default 9200"
        sed -i'' -e "s/elastic.host=http:\/\/localhost/elastic.host=http:\/\/${ES_HOST}/" /usr/local/tomcat/shared/classes/mms.properties
    else
        sed -i'' -e "s/elastic.host=http:\/\/localhost:9200/elastic.host=http:\/\/${ES_HOST}:${ES_PORT}/" /usr/local/tomcat/shared/classes/mms.properties
    fi
fi
