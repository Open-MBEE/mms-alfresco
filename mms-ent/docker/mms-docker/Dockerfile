FROM dank/alfresco-base:0.1

MAINTAINER Dan Karlsson <dan.e.karlsson@jpl.nasa.gov>

ENV FILES_DIR /usr/local/files
ENV TOMCAT_DIR /usr/local/tomcat
COPY files /usr/local/files


RUN java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${FILES_DIR}/mms-repo.amp ${TOMCAT_DIR}/webapps/alfresco.war -force \
    && java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${FILES_DIR}/mms-share.amp ${TOMCAT_DIR}/webapps/share.war -force \
    && cd ${TOMCAT_DIR}/webapps \
    && rm -rf alfresco share \
    && mkdir alfresco share \
    && cd alfresco \
    && jar xf ../alfresco.war \
    && cd ../share \
    && jar xf ../share.war \
    && mv ${FILES_DIR}/mms.properties.example ${TOMCAT_DIR}/shared/classes/mms.properties \
    && rm -rf ${FILES_DIR}

EXPOSE 8080:8080
EXPOSE 5432:5432
EXPOSE 9200:9200
