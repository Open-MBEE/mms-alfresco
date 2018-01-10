#!/bin/sh

${TOMCAT_DIR}/bin/set_properties.sh

# Start Alfresco
su - tomcat -c "${TOMCAT_DIR}/bin/catalina.sh run"
