#!/bin/sh

# Configure alfresco-global.properties and mms.properties
if [[ -z "$PG_HOST" ]];then
    echo "Postgres host not set, using default localhost on port 5432"
else
    sed -i'' -e "s/db.host=localhost/db.host=${PG_HOST}/" /usr/local/tomcat/shared/classes/alfresco-global.properties
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
else
    sed -i'' -e "s/pg.name=POSTGRESDBNAME/pg.name=${PG_DB_NAME}/" /usr/local/tomcat/shared/classes/mms.properties
fi
if [[ -z "$PG_DB_USER" ]];then
    echo "Postgres Database User not set"
else
    sed -i'' -e "s/db.username=alfresco/db.username=${PG_DB_USER}/" /usr/local/tomcat/shared/classes/alfresco-global.properties
    sed -i'' -e "s/pg.user=POSTGRESUSERNAME/pg.user=${PG_DB_USER}/" /usr/local/tomcat/shared/classes/mms.properties
fi
if [[ -z "$PG_DB_PASS" ]];then
    echo "Postgres Database User password not set"
else
    sed -i'' -e "s/db.password=alfresco/db.password=${PG_DB_PASS}/" /usr/local/tomcat/shared/classes/alfresco-global.properties
    sed -i'' -e "s/pg.pass=POSTGRESPASSWORD/pg.pass=${PG_DB_PASS}/" /usr/local/tomcat/shared/classes/mms.properties
fi
if [[ -z "$ES_HOST" ]];then
    echo "Elasticsearch host not set"
else
    if [[ -z "$ES_PORT" ]];then
        echo "Elasticsearch port not set, using default 9200"
        sed -i'' -e "s/elastic.host=http:\/\/localhost/elastic.host=http:\/\/${ES_HOST}/" /usr/local/tomcat/shared/classes/mms.properties
    else
        sed -i'' -e "s/elastic.host=http:\/\/localhost:9200/elastic.host=http:\/\/${ES_HOST}:${ES_PORT}/" /usr/local/tomcat/shared/classes/mms.properties
    fi
fi
if [[ -z "$APP_USER" ]];then
    echo "Application User not set, using default username 'admin'"
else
    sed -i'' -e "s/app.user=admin/app.user=${APP_USER}/" /usr/local/tomcat/shared/classes/mms.properties
fi
if [[ -z "$APP_PASS" ]];then
    echo "Application Password not set, using default password 'admin'"
else
    sed -i'' -e "s/app.pass=admin/app.pass=${APP_USER}/" /usr/local/tomcat/shared/classes/mms.properties
fi

# Run these unconditionally
sed -i'' -e "s/pandoc.exec=\/usr\/local\/bin\/pandoc/pandoc.exec=\/usr\/local\/pandoc\/bin\/pandoc/" /usr/local/tomcat/shared/classes/mms.properties
export CATALINA_TMPDIR=/mnt/alf_data/temp
