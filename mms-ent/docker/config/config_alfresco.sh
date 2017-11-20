#!/bin/sh

cd /usr/local
CUR_DIR=`echo $PWD`
ROOT_DIR=/usr/local/alfresco-community-distribution-201605
TOMCAT_DIR=/usr/local/tomcat
unzip -q alfresco-community-distribution-201605.zip

ls -la
ls -la ${CUR_DIR}
ls -la ${TOMCAT_DIR}

echo "cp -r ${ROOT_DIR}/alf_data /mnt/"
cp -r ${ROOT_DIR}/alf_data /mnt/

echo "cp ${ROOT_DIR}/web-server/webapps/alfresco.war ${TOMCAT_DIR}/webapps"
mv ${ROOT_DIR}/web-server/webapps/alfresco.war ${TOMCAT_DIR}/webapps/alfresco.war

echo "cp -r ${ROOT_DIR}/web-server/webapps/share.war ./tomcat/webapps/share.war"
mv ${ROOT_DIR}/web-server/webapps/share.war ${TOMCAT_DIR}/webapps/share.war

echo "cp -r ${ROOT_DIR}/web-server/webapps/solr4.war ./tomcat/webapps/solr4.war"
mv ${ROOT_DIR}/web-server/webapps/solr4.war ${TOMCAT_DIR}/webapps/solr4.war

echo "cp -r ${ROOT_DIR}/web-server/conf/Catalina tomcat/conf/"
cp -r ${ROOT_DIR}/web-server/conf/Catalina ${TOMCAT_DIR}/conf/

# Copy postgres driver
echo "cp -r ${ROOT_DIR}/web-server/lib/postgresql-9.4-1201-jdbc41.jar tomcat/lib/"
cp ${ROOT_DIR}/web-server/lib/postgresql-9.4-1201-jdbc41.jar ${TOMCAT_DIR}/lib/postgresql-9.4-1201-jdbc41.jar

echo "cp -r ${ROOT_DIR}/web-server/shared tomcat/shared"
cp -r ${ROOT_DIR}/web-server/shared ${TOMCAT_DIR}/
mkdir ${TOMCAT_DIR}/shared/lib
mv ${ROOT_DIR}/web-server/lib/postgresql-9.4-1201-jdbc41.jar ${TOMCAT_DIR}/shared/lib/

echo "cp -r ${ROOT_DIR}/amps tomcat/"
cp -r ${ROOT_DIR}/amps ${TOMCAT_DIR}/

echo "cp -r ${ROOT_DIR}/solr4 tomcat/"
cp -r ${ROOT_DIR}/solr4 ${TOMCAT_DIR}/

echo "cp -r ${ROOT_DIR}/amps tomcat/"
cp -r ${ROOT_DIR}/amps ${TOMCAT_DIR}/

echo "cp -r ${ROOT_DIR}/amps_share tomcat/"
cp -r ${ROOT_DIR}/amps_share ${TOMCAT_DIR}/

echo "cp -r ${ROOT_DIR}/licenses tomcat/"
cp -r ${ROOT_DIR}/licenses ${TOMCAT_DIR}/

echo "cp -r ${ROOT_DIR}/bin tomcat/"
cp -r ${ROOT_DIR}/bin ${TOMCAT_DIR}/

# Install the share service amp into the war before exploding the war.
echo "java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${TOMCAT_DIR}/amps/alfresco-share-services.amp ${TOMCAT_DIR}/webapps/repo.war -force"
java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${TOMCAT_DIR}/amps/alfresco-share-services.amp ${TOMCAT_DIR}/webapps/alfresco.war -force

# Explode wars into directories
cd ${TOMCAT_DIR}/webapps
rm -rf alfresco share solr4
mkdir alfresco share solr4
cd alfresco
jar xf ../alfresco.war
cd ../share
jar xf ../share.war
cd ../solr4
jar xf ../solr4.war

# Remove extra backup files
cd ${TOMCAT_DIR}/webapps
rm -rf *.bak

# Remove the extra files to reduce image size.
cd ${CUR_DIR}
rm -rf alfresco-community-distribution-201605.zip alfresco-community-distribution-201605

