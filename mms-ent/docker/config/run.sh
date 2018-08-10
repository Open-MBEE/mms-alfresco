#!/bin/sh

${TOMCAT_HOME}/bin/set_properties.sh

# Check if this is an all-in-one image
if [ -f "/usr/local/bin/docker-entrypoint.sh" ];then
    /usr/local/bin/docker-entrypoint.sh elasticsearch &&
    POSTGRES_USER=$PG_DB_USER POSTGRES_PASSWORD=$PG_DB_PASS /usr/local/bin/docker-entrypoint.sh postgres &&
    su - tomcat -c "${TOMCAT_HOME}/bin/catalina.sh run";
else
    #Just start tomcat
    su - tomcat -c "${TOMCAT_HOME}/bin/catalina.sh run";
fi
