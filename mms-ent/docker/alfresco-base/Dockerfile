# ENV container docker
# ------------------------------------------------------------
# Basic updates and install
# ------------------------------------------------------------
FROM dank/alpine-jdk-tomcat

MAINTAINER Dan Karlsson <dan.e.karlsson@jpl.nasa.gov>

ENV POSTGRES_HOST localhost
ENV ALFRESCO_HOME /usr/local/tomcat
ENV ALF_DATA_DIR /mnt/alf_data
ENV CONFIG_DIR /usr/local/config


# Set the working directory to be the tomcat home dir
WORKDIR /usr/local

COPY config /usr/local/config
COPY config/config_alfresco.sh .
COPY config/run.sh tomcat/bin

# Download the community zip files
RUN wget --no-check-certificate "https://download.alfresco.com/release/community/201605-build-00010/alfresco-community-distribution-201605.zip" \
    && chmod +x config_alfresco.sh \
    && chmod +x tomcat/bin/run.sh \
    && ./config_alfresco.sh

RUN cp ${CONFIG_DIR}/alfresco-global.properties tomcat/shared/classes/alfresco-global.properties \
    && cp ${CONFIG_DIR}/catalina.properties tomcat/conf/catalina.properties \
    && cp ${CONFIG_DIR}/solr4-context.xml tomcat/conf/Catalina/localhost/solr4.xml \
    && cp ${CONFIG_DIR}/solr4-context.xml tomcat/solr4/context.xml \
    && cp ${CONFIG_DIR}/tomcat-server.xml tomcat/conf/server.xml \
    && cp ${CONFIG_DIR}/archive-solrcore.properties tomcat/solr4/archive-SpacesStore/conf/solrcore.properties \
    && cp ${CONFIG_DIR}/workspace-solrcore.properties tomcat/solr4/workspace-SpacesStore/conf/solrcore.properties

EXPOSE 8080
CMD ["tomcat/bin/run.sh"]
