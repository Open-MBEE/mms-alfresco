#!/bin/sh

${TOMCAT_HOME}/bin/set_properties.sh

# Start Alfresco
su - tomcat -c "${TOMCAT_HOME}/bin/catalina.sh run"
