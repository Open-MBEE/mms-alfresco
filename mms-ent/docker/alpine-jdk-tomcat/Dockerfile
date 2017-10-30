FROM openjdk:8-jdk-alpine

ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH
WORKDIR /usr/local

ENV TOMCAT_MAJOR 7
ENV TOMCAT_VERSION 7.0.82
ENV TOMCAT_TGZ_URL "http://mirrors.sonic.net/apache/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz"

RUN pwd

RUN apk add --update curl wget unzip

RUN curl -fSL "$TOMCAT_TGZ_URL" -o tomcat.tar.gz \
    && ls -la \
    && echo "tar -xzf tomcat.tar.gz" \
    && tar -xzf tomcat.tar.gz \
    && rm tomcat.tar.gz* \
    && echo "mv apache-tomcat-${TOMCAT_VERSION} tomcat" \
    && mv apache-tomcat-${TOMCAT_VERSION} tomcat \
    &&


RUN echo "ls -la" && ls -la \
    && echo "Current Dir:$PWD" \
    && ls -la bin \
    && ls -la tomcat

WORKDIR $CATALINA_HOME

EXPOSE 8080
CMD ["catalina.sh", "run"]
