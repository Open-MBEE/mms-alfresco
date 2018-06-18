FROM openjdk:8-jdk-alpine

ENV BUILD_DEPS \
    alpine-sdk \
    cabextract \
    coreutils \
    curl \
    ghc \
    gmp \
    libffi \
    linux-headers \
    musl-dev \
    wget \
    zlib-dev

ENV PERSIST_DEPS \
    fontconfig \
    graphviz \
    msttcorefonts-installer \
    python \
    py2-pip \
    sed \
    ttf-droid \
    ttf-droid-nonlatin \
    unzip

ENV EDGE_DEPS cabal

ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH

ENV TOMCAT_MAJOR 7
ENV TOMCAT_VERSION 7.0.88
ENV TOMCAT_TGZ_URL "http://mirrors.sonic.net/apache/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz"
ENV POSTGRES_HOST localhost
ENV ALF_DATA_DIR /mnt/alf_data
ENV CONFIG_DIR /usr/local/config
ENV FILES_DIR /usr/local/files
ENV TOMCAT_DIR /usr/local/tomcat

ENV PLANTUML_VERSION 1.2017.18
ENV PLANTUML_DOWNLOAD_URL https://sourceforge.net/projects/plantuml/files/plantuml.$PLANTUML_VERSION.jar/download

ENV PANDOC_VERSION 1.19.2.4
ENV PANDOC_DOWNLOAD_URL https://hackage.haskell.org/package/pandoc-$PANDOC_VERSION/pandoc-$PANDOC_VERSION.tar.gz
ENV PANDOC_ROOT /usr/local/pandoc


# ---- Install Dependencies ----
RUN apk upgrade --update \
    && apk add --no-cache --virtual .build-deps $BUILD_DEPS \
    && apk add --no-cache --virtual .persistent-deps $PERSIST_DEPS \
    && update-ms-fonts \
    && fc-cache -f \
    && curl -fsSL "$PLANTUML_DOWNLOAD_URL" -o /usr/local/plantuml.jar \
    && apk add --no-cache --virtual .edge-deps $EDGE_DEPS -X http://dl-cdn.alpinelinux.org/alpine/edge/community \
    && curl -fsSL "$PANDOC_DOWNLOAD_URL" | tar -xzf - \
    && ( cd pandoc-$PANDOC_VERSION && cabal update && cabal install --only-dependencies \
    && cabal configure --prefix=$PANDOC_ROOT \
    && cabal build \
    && cabal copy \
    && cd .. ) \
    && rm -Rf pandoc-$PANDOC_VERSION/ \
    && rm -Rf /root/.cabal/ /root/.ghc/ \
    && rmdir /pandoc-build \
    && set -x; addgroup -g 82 -S www-data; adduser -u 82 -D -S -G www-data www-data \
    && mkdir -p /var/docs


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
COPY config ${CONFIG_DIR}
COPY config/config_alfresco.sh .
COPY config/run.sh ${TOMCAT_DIR}/bin/run.sh
COPY config/set_properties.sh ${TOMCAT_DIR}/bin/set_properties.sh


# Download the community zip files
RUN wget --no-check-certificate -S "https://download.alfresco.com/release/community/201605-build-00010/alfresco-community-distribution-201605.zip" \
    && chmod +x config_alfresco.sh \
    && chmod +x ${TOMCAT_DIR}/bin/run.sh \
    && chmod +x ${TOMCAT_DIR}/bin/set_properties.sh \
    && ./config_alfresco.sh

RUN cp ${CONFIG_DIR}/alfresco-global.properties ${TOMCAT_DIR}/shared/classes/alfresco-global.properties \
    && cp ${CONFIG_DIR}/catalina.properties ${TOMCAT_DIR}/conf/catalina.properties \
    && cp ${CONFIG_DIR}/solr4-context.xml ${TOMCAT_DIR}/conf/Catalina/localhost/solr4.xml \
    && cp ${CONFIG_DIR}/tomcat-server.xml ${TOMCAT_DIR}/conf/server.xml \
    && cp ${CONFIG_DIR}/archive-solrcore.properties ${TOMCAT_DIR}/solr4/archive-SpacesStore/conf/solrcore.properties \
    && cp ${CONFIG_DIR}/workspace-solrcore.properties ${TOMCAT_DIR}/solr4/workspace-SpacesStore/conf/solrcore.properties


# ---- MMS -----
COPY files ${FILES_DIR}
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
    && rm -rf ${FILES_DIR} \
    && cd ${TOMCAT_DIR}/webapps \
    && rm *.bak


# Add tomcat user and config
RUN addgroup -S tomcat \
    && adduser -G tomcat -g "Tomcat User" -s /bin/ash -D tomcat \
    && mkdir -p ${ALF_DATA_DIR}/solr4 \
    && chown -R tomcat:tomcat ${TOMCAT_DIR} \
    && chown -R tomcat:tomcat ${ALF_DATA_DIR}

RUN apk del .build-deps


# Alfresco
EXPOSE 8080:8080
# Postgres
#EXPOSE 5432:5432
# ElasticSearch
#EXPOSE 9200:9200

ENTRYPOINT ["/usr/local/tomcat/bin/run.sh"]
