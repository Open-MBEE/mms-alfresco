#!/bin/sh
CUR_DIR=`echo $PWD`
ROOT_DIR=alfresco-community-distribution-201605
TOMCAT_DIR=/usr/local/tomcat
unzip -q alfresco-community-distribution-201605.zip
ls -la
mv ${ROOT_DIR}/alf_data /mnt
echo "cp -r ${ROOT_DIR}/web-server/webapps/alfresco.war ./tomcat/webapps/"
cp -r ${ROOT_DIR}/web-server/webapps/alfresco.war ./tomcat/webapps/
echo "cp -r ${ROOT_DIR}/web-server/webapps/share.war ./tomcat/webapps/"
cp -r ${ROOT_DIR}/web-server/webapps/share.war ./tomcat/webapps/
echo "cp -r ${ROOT_DIR}/web-server/webapps/solr4.war ./tomcat/webapps/"
cp -r ${ROOT_DIR}/web-server/webapps/solr4.war ./tomcat/webapps/

echo "cp -r ${ROOT_DIR}/web-server/conf/Catalina tomcat/conf/"
cp -r ${ROOT_DIR}/web-server/conf/Catalina tomcat/conf/

# Copy postgres driver
echo "cp -r ${ROOT_DIR}/web-server/lib/postgresql-9.4-1201-jdbc41.jar tomcat/lib/"
cp -r ${ROOT_DIR}/web-server/lib/postgresql-9.4-1201-jdbc41.jar tomcat/lib/

echo "cp -r ${ROOT_DIR}/web-server/shared tomcat/shared"
cp -r ${ROOT_DIR}/web-server/shared tomcat/
mkdir tomcat/shared/lib
cp -r ${ROOT_DIR}/web-server/lib/postgresql-9.4-1201-jdbc41.jar tomcat/shared/lib/


echo "cp -r ${ROOT_DIR}/amps tomcat/"
cp -r ${ROOT_DIR}/amps tomcat/

echo "cp -r ${ROOT_DIR}/solr4 tomcat/"
cp -r ${ROOT_DIR}/solr4 tomcat/

echo "cp -r ${ROOT_DIR}/amps tomcat/"
cp -r ${ROOT_DIR}/amps tomcat/

echo "cp -r ${ROOT_DIR}/amps_share tomcat/"
cp -r ${ROOT_DIR}/amps_share tomcat/

echo "cp -r ${ROOT_DIR}/licenses tomcat/"
cp -r ${ROOT_DIR}/licenses tomcat/

echo "cp -r ${ROOT_DIR}/bin tomcat/"
cp -r ${ROOT_DIR}/bin tomcat/

# Install the share service amp into the war before exploding the war.
echo "java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${TOMCAT_DIR}/amps/alfresco-share-services.amp ${TOMCAT_DIR}/webapps/share.war -force"
java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${TOMCAT_DIR}/amps/alfresco-share-services.amp ${TOMCAT_DIR}/webapps/share.war -force

# Explode wars into directories
cd tomcat/webapps
rm -rf alfresco share solr4
mkdir alfresco share solr4
cd alfresco
jar xf ../alfresco.war
cd ../share
jar xf ../share.war
cd ../solr4
jar xf ../solr4.war

# Remove the extra files to reduce image size.
cd ${CUR_DIR}
rm -rf alfresco-community-distribution-201605.zip alfresco-community-distribution-201605

# Install the share service amp AGAIN into the war before exploding the war.

echo "java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${TOMCAT_DIR}/amps/alfresco-share-services.amp ${TOMCAT_DIR}/webapps/share.war -force"
java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${TOMCAT_DIR}/amps/alfresco-share-services.amp ${TOMCAT_DIR}/webapps/share.war -force
