# ENV container docker
# ------------------------------------------------------------
# Basic updates and install
# ------------------------------------------------------------
FROM dank/alpine-jdk-tomcat:0.1

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

RUN cp ${CONFIG_DIR}/alfresco-global.properties tomcat/shared/classes/ \
    && cp ${CONFIG_DIR}/catalina.properties tomcat/conf/ \
    && cp ${CONFIG_DIR}/solr4-context.xml tomcat/conf/Catalina/localhost/solr4.xml \
    && cp ${CONFIG_DIR}/tomcat-server.xml tomcat/conf/server.xml

RUN addgroup -S tomcat \
    && adduser -S -g tomcat tomcat \
    && chown -R tomcat:tomcat /usr/local/tomcat \
    && mkdir -p /mnt/alf_data/solr4 \
    && chown -R tomcat:tomcat /mnt/alf_data

EXPOSE 8080
CMD ["tomcat/bin/run.sh"]
