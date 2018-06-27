FROM openjdk:8-jdk-alpine

ENV BUILD_DEPS \
    bison \
    cabextract \
    ca-certificates \
    coreutils \
    curl \
    dpkg-dev dpkg \
    flex \
    gcc \
    ghc \
    gmp \
    gnupg \
    libc-dev \
    libedit-dev \
    libxml2-dev \
    libxslt-dev \
    libffi \
    linux-headers \
    make \
    musl-dev \
    openssl-dev \
    perl-utils \
    perl-ipc-run \
    tar \
    util-linux-dev \
    wget \
    zlib-dev

ENV PERSIST_DEPS \
    bash \
    fontconfig \
    graphviz \
    imagemagick \
    msttcorefonts-installer \
    python \
    py2-pip \
    sed \
    su-exec \
    ttf-droid \
    ttf-droid-nonlatin \
    tzdata \
    unzip

ENV EDGE_DEPS \
    cabal

ENV LANG en_US.utf8

ENV USR_LOCAL /usr/local

ENV ELASTICSEARCH_VERSION 5.5.2
ENV ELASTICSEARCH_TARBALL "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz"
ENV ELASTICSEARCH_TARBALL_ASC "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${ELASTICSEARCH_VERSION}.tar.gz.asc"
ENV ELASTICSEARCH_TARBALL_SHA1 "91b3b3c823fafce54609ed5c9075d9cf50b2edff"
ENV ELASTICSEARCH_HOME $USR_LOCAL/elasticsearch

ENV PG_MAJOR 9.4
ENV PG_VERSION 9.4.18
ENV PG_SHA256 428337f2b2f5e3ea21b8a44f88eb89c99a07a324559b99aebe777c9abdf4c4c0

ENV TOMCAT_MAJOR 7
ENV TOMCAT_VERSION 7.0.88
ENV TOMCAT_TGZ_URL "http://mirrors.sonic.net/apache/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz"
ENV TOMCAT_HOME $USR_LOCAL/tomcat

ENV PLANTUML_VERSION 1.2017.18
ENV PLANTUML_DOWNLOAD_URL https://sourceforge.net/projects/plantuml/files/plantuml.$PLANTUML_VERSION.jar/download

ENV PANDOC_VERSION 1.19.2.4
ENV PANDOC_DOWNLOAD_URL https://hackage.haskell.org/package/pandoc-$PANDOC_VERSION/pandoc-$PANDOC_VERSION.tar.gz
ENV PANDOC_HOME $USR_LOCAL/pandoc

ENV ALF_DATA_DIR /mnt/alf_data
ENV CONFIG_DIR $USR_LOCAL/config
ENV FILES_DIR $USR_LOCAL/files

ENV PGDATA /var/lib/postgresql/data

ENV PATH $USR_LOCAL/bin:$TOMCAT_HOME/bin:$ELASTICSEARCH_HOME/bin:$PANDOC_HOME/bin:$PATH

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
        && cabal configure --prefix=$PANDOC_HOME \
        && cabal build \
        && cabal copy \
        && cd .. ) \
    && rm -Rf pandoc-$PANDOC_VERSION/ /root/.cabal/ /root/.ghc/ /pandoc-build \
    && mkdir -p /var/docs


# ---- Postgres ----

RUN set -ex; \
	postgresHome="$(getent passwd postgres)"; \
	postgresHome="$(echo "$postgresHome" | cut -d: -f6)"; \
	[ "$postgresHome" = '/var/lib/postgresql' ]; \
	mkdir -p "$postgresHome"; \
	chown -R postgres:postgres "$postgresHome"

RUN set -ex; \
    wget -O postgresql.tar.bz2 "https://ftp.postgresql.org/pub/source/v$PG_VERSION/postgresql-$PG_VERSION.tar.bz2" \
        && echo "$PG_SHA256 *postgresql.tar.bz2" | sha256sum -c - \
        && mkdir -p /usr/src/postgresql \
        && tar \
            --extract \
            --file postgresql.tar.bz2 \
            --directory /usr/src/postgresql \
            --strip-components 1 \
        && rm postgresql.tar.bz2 \
        \
        && cd /usr/src/postgresql \
        && awk '$1 == "#define" && $2 == "DEFAULT_PGSOCKET_DIR" && $3 == "\"/tmp\"" { $3 = "\"/var/run/postgresql\""; print; next } { print }' src/include/pg_config_manual.h > src/include/pg_config_manual.h.new \
        && grep '/var/run/postgresql' src/include/pg_config_manual.h.new \
        && mv src/include/pg_config_manual.h.new src/include/pg_config_manual.h \
        && gnuArch="$(dpkg-architecture --query DEB_BUILD_GNU_TYPE)" \
        && wget -O config/config.guess 'https://git.savannah.gnu.org/cgit/config.git/plain/config.guess?id=7d3d27baf8107b630586c962c057e22149653deb' \
        && wget -O config/config.sub 'https://git.savannah.gnu.org/cgit/config.git/plain/config.sub?id=7d3d27baf8107b630586c962c057e22149653deb' \
        && ./configure \
            --build="$gnuArch" \
            --enable-integer-datetimes \
            --enable-thread-safety \
            --enable-tap-tests \
            --disable-rpath \
            --with-uuid=e2fs \
            --with-gnu-ld \
            --with-pgport=5432 \
            --with-system-tzdata=/usr/share/zoneinfo \
            --prefix=/usr/local \
            --with-includes=/usr/local/include \
            --with-libraries=/usr/local/lib \
            \
            --with-openssl \
            --with-libxml \
            --with-libxslt \
        && make -j "$(nproc)" world \
        && make install-world \
        && make -C contrib install \
        \
        && runDeps="$( \
            scanelf --needed --nobanner --format '%n#p' --recursive /usr/local/share/postgresql \
                | tr ',' '\n' \
                | sort -u \
                | awk 'system("[ -e /usr/local/lib/" $1 " ]") == 0 { next } { print "so:" $1 }' \
        )" \
        && apk add --no-cache --virtual .persistent-deps \
            $runDeps \
        && cd / \
        && rm -rf \
            /usr/src/postgresql \
            /usr/local/share/doc \
            /usr/local/share/man \
        && find /usr/local -name '*.a' -delete

RUN sed -ri "s!^#?(listen_addresses)\s*=\s*\S+.*!\1 = '*'!" /usr/local/share/postgresql/postgresql.conf.sample
RUN mkdir -p /var/run/postgresql && chown -R postgres:postgres /var/run/postgresql && chmod 2777 /var/run/postgresql
RUN mkdir -p "$PGDATA" && chown -R postgres:postgres "$PGDATA" && chmod 777 "$PGDATA"


# ---- Elasticsearch ----

RUN addgroup -S elasticsearch && adduser -S -G elasticsearch elasticsearch
WORKDIR /usr/share/elasticsearch

RUN set -ex; \
    wget -O elasticsearch.tar.gz "$ELASTICSEARCH_TARBALL" \
    && echo "$ELASTICSEARCH_TARBALL_SHA1 *elasticsearch.tar.gz" | sha1sum -c - \
    && wget -O elasticsearch.tar.gz.asc "$ELASTICSEARCH_TARBALL_ASC" \
    && export GNUPGHOME="$(mktemp -d)" \
    && gpg --keyserver ha.pool.sks-keyservers.net --recv-keys "$GPG_KEY" \
    && gpg --batch --verify elasticsearch.tar.gz.asc elasticsearch.tar.gz \
    && rm -rf "$GNUPGHOME" elasticsearch.tar.gz.asc \
    && tar -xf elasticsearch.tar.gz --strip-components=1 \
    && rm elasticsearch.tar.gz \
    && mkdir -p ./plugins; \
    for path in \
        ./data \
        ./logs \
        ./config \
        ./config/scripts \
    ; do \
        mkdir -p "$path"; \
        chown -R elasticsearch:elasticsearch "$path"; \
    done; \
    export ES_JAVA_OPTS='-Xms32m -Xmx32m';

COPY config ./config

# ---- Tomcat ----
RUN curl -fSL "$TOMCAT_TGZ_URL" -o tomcat.tar.gz \
    && ls -la \
    && echo "tar -xzf tomcat.tar.gz" \
    && tar -xzf tomcat.tar.gz \
    && rm tomcat.tar.gz* \
    && echo "mv apache-tomcat-${TOMCAT_VERSION} tomcat" \
    && mv apache-tomcat-${TOMCAT_VERSION} ${TOMCAT_HOME}


# ---- Alfresco ----
WORKDIR /usr/local
COPY config ${CONFIG_DIR}
COPY config/config_alfresco.sh .
COPY config/run.sh ${TOMCAT_HOME}/bin/run.sh
COPY config/set_properties.sh ${TOMCAT_HOME}/bin/set_properties.sh
COPY all-in-one/docker-entrypoint.sh /usr/local/bin/


# Download the community zip files
RUN wget --no-check-certificate -S "https://download.alfresco.com/release/community/201605-build-00010/alfresco-community-distribution-201605.zip" \
    && chmod +x ${USR_LOCAL}/bin/docker-entrypoint.sh \
    && chmod +x config_alfresco.sh \
    && chmod +x ${TOMCAT_HOME}/bin/run.sh \
    && chmod +x ${TOMCAT_HOME}/bin/set_properties.sh \
    && ./config_alfresco.sh

RUN cp ${CONFIG_DIR}/alfresco-global.properties ${TOMCAT_HOME}/shared/classes/alfresco-global.properties \
    && cp ${CONFIG_DIR}/catalina.properties ${TOMCAT_HOME}/conf/catalina.properties \
    && cp ${CONFIG_DIR}/solr4-context.xml ${TOMCAT_HOME}/conf/Catalina/localhost/solr4.xml \
    && cp ${CONFIG_DIR}/tomcat-server.xml ${TOMCAT_HOME}/conf/server.xml \
    && cp ${CONFIG_DIR}/tomcat-users.xml ${TOMCAT_HOME}/conf/tomcat-users.xml \
    && cp ${CONFIG_DIR}/archive-solrcore.properties ${TOMCAT_HOME}/solr4/archive-SpacesStore/conf/solrcore.properties \
    && cp ${CONFIG_DIR}/workspace-solrcore.properties ${TOMCAT_HOME}/solr4/workspace-SpacesStore/conf/solrcore.properties


# ---- MMS -----
COPY files ${FILES_DIR}
RUN java -jar ${TOMCAT_HOME}/bin/alfresco-mmt.jar install ${FILES_DIR}/mms-amp*.amp ${TOMCAT_HOME}/webapps/alfresco.war -force \
    && java -jar ${TOMCAT_HOME}/bin/alfresco-mmt.jar install ${FILES_DIR}/mms-share-amp*.amp ${TOMCAT_HOME}/webapps/share.war -force \
    && cd ${TOMCAT_HOME}/webapps \
    && rm -rf alfresco share \
    && mkdir alfresco share \
    && cd alfresco \
    && jar xf ../alfresco.war \
    && cd ../share \
    && jar xf ../share.war \
    && mv ${FILES_DIR}/mms.properties.example ${TOMCAT_HOME}/shared/classes/mms.properties \
    && cp ${FILES_DIR}/web.xml ${TOMCAT_HOME}/webapps/alfresco/WEB-INF/web.xml \
    && rm -rf ${FILES_DIR} \
    && cd ${TOMCAT_HOME}/webapps \
    && rm *.bak


# Add tomcat user and config
RUN addgroup -S tomcat \
    && adduser -G tomcat -g "Tomcat User" -s /bin/ash -D tomcat \
    && mkdir -p ${ALF_DATA_DIR}/solr4 \
    && chown -R tomcat:tomcat ${TOMCAT_HOME} \
    && chown -R tomcat:tomcat ${ALF_DATA_DIR}

RUN apk del .build-deps

# Alfresco
EXPOSE 8080:8080
# Postgres
EXPOSE 5432:5432
# ElasticSearch
EXPOSE 9200:9200

ENTRYPOINT ["/usr/local/tomcat/bin/run.sh"]
