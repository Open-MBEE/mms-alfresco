FROM openjdk:8-jdk-alpine

ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH

ENV TOMCAT_MAJOR 7
ENV TOMCAT_VERSION 7.0.82
ENV TOMCAT_TGZ_URL "http://mirrors.sonic.net/apache/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz"
ENV POSTGRES_HOST localhost
ENV ALF_DATA_DIR /mnt/alf_data
ENV CONFIG_DIR /usr/local/config
ENV FILES_DIR /usr/local/files
ENV TOMCAT_DIR /usr/local/tomcat

RUN apk add --update curl wget unzip

# ---- Tomcat ----
RUN curl -fSL "$TOMCAT_TGZ_URL" -o tomcat.tar.gz \
    && ls -la \
    && echo "tar -xzf tomcat.tar.gz" \
    && tar -xzf tomcat.tar.gz \
    && rm tomcat.tar.gz* \
    && echo "mv apache-tomcat-${TOMCAT_VERSION} tomcat" \
    && mv apache-tomcat-${TOMCAT_VERSION} ${TOMCAT_DIR}


# ---- Alfresco ----
WORKDIR /usr/local
COPY config /usr/local/config
COPY config/config_alfresco.sh .
COPY config/run.sh tomcat/bin/run.sh


# Download the community zip files
RUN wget --no-check-certificate "https://download.alfresco.com/release/community/201605-build-00010/alfresco-community-distribution-201605.zip"

RUN chmod +x config_alfresco.sh \
    && chmod +x tomcat/bin/run.sh \
    && ./config_alfresco.sh

RUN cp ${CONFIG_DIR}/alfresco-global.properties tomcat/shared/classes/alfresco-global.properties \
    && cp ${CONFIG_DIR}/catalina.properties tomcat/conf/catalina.properties \
    && cp ${CONFIG_DIR}/solr4-context.xml tomcat/conf/Catalina/localhost/solr4.xml \
    && cp ${CONFIG_DIR}/solr4-context.xml tomcat/solr4/context.xml \
    && cp ${CONFIG_DIR}/tomcat-server.xml tomcat/conf/server.xml \
    && cp ${CONFIG_DIR}/archive-solrcore.properties tomcat/solr4/archive-SpacesStore/conf/solrcore.properties \
    && cp ${CONFIG_DIR}/workspace-solrcore.properties tomcat/solr4/workspace-SpacesStore/conf/solrcore.properties

# ---- MMS -----
COPY files ${FILES_DIR}
RUN ls ${FILES_DIR}
RUN java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${FILES_DIR}/mms-amp*.amp ${TOMCAT_DIR}/webapps/alfresco.war -force \
    && java -jar ${TOMCAT_DIR}/bin/alfresco-mmt.jar install ${FILES_DIR}/mms-share-amp*.amp ${TOMCAT_DIR}/webapps/share.war -force \
    && cd ${TOMCAT_DIR}/webapps \
    && rm -rf alfresco share \
    && mkdir alfresco share \
    && cd alfresco \
    && jar xf ../alfresco.war \
    && cd ../share \
    && jar xf ../share.war \
    && mv ${FILES_DIR}/mms.properties.example ${TOMCAT_DIR}/shared/classes/mms.properties \
    && cp ${FILES_DIR}/web.xml ${TOMCAT_DIR}/webapps/alfresco/WEB-INF/web.xml \
    && rm -rf ${FILES_DIR}


# Add tomcat user and config
RUN addgroup -S tomcat \
    && adduser -G tomcat -g "Tomcat User" -s /bin/ash -D tomcat \
    && mkdir -p ${ALF_DARA_DIR}/solr4 \
    && chown -R tomcat:tomcat ${TOMCAT_DIR} \
    && chown -R tomcat:tomcat ${ALF_DARA_DIR}
    
# Alfresco
EXPOSE 8080:8080
# Postgres
EXPOSE 5432:5432
# ElasticSearch
EXPOSE 9200:9200
