FROM openjdk:8-jdk-alpine

ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH
WORKDIR /usr/local

ENV TOMCAT_MAJOR 7
ENV TOMCAT_VERSION 7.0.68
ENV TOMCAT_TGZ_URL https://www.apache.org/dist/tomcat/tomcat-$TOMCAT_MAJOR/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz

RUN pwd

RUN apk add --update curl

RUN curl -fSL "$TOMCAT_TGZ_URL" -o tomcat.tar.gz \
    && ls -la \
    && echo "tar -xzf tomcat.tar.gz" \
    && tar -xzf tomcat.tar.gz \
    && rm tomcat.tar.gz* \
    && echo "mv apache-tomcat-7.0.68 tomcat" \
    && mv apache-tomcat-7.0.68 tomcat


RUN echo "ls -la" && ls -la \
    && echo "Current Dir:$PWD" \
    && ls -la bin \
    && ls -la tomcat

WORKDIR $CATALINA_HOME

EXPOSE 8080
CMD ["catalina.sh", "run"]
