# ENV container docker
# ------------------------------------------------------------
# Basic updates and install
# ------------------------------------------------------------
FROM dank/alpine-jdk-tomcat:0.1

MAINTAINER Dan Karlsson <dan.e.karlsson@jpl.nasa.gov>

# Debugging and environment set up
RUN echo $JAVA_HOME


# Set the working directory to be the tomcat home dir
WORKDIR /usr/local/tomcat


# Copy AMP files over
VOLUME view-repo-5.0/alf_data_dev

# The alfresco-tomcat Directory : opt/alfresco-5.0.d/tomcat/webapps/
# RUN rm /opt/alfresco-5.0.d/tomcat/webapps/alfresco/WEB-INF/classes/alfresco-global.properties

RUN mkdir -p /usr/local/tomat/shared/classes \
    && rm -rf /usr/local/tomcat/webapps \
    && mkdir -p /usr/local/tomcat/webapps

COPY classes/* /usr/local/tomcat/shared/classes/
RUN ls /usr/local/tomcat/shared/classes/

COPY properties/catalina.properties /usr/local/tomcat/conf/
RUN ls /usr/local/tomcat/conf

# Basic Alfresco war files
COPY wars/alfresco/ROOT.war /usr/local/tomcat/webapps
COPY wars/alfresco/alfresco.war /usr/local/tomcat/webapps
COPY wars/alfresco/solr4.war /usr/local/tomcat/webapps
RUN ls /usr/local/tomcat/webapps

# Custom MMS Repo and Share war files
COPY wars/mms/repo.3-31-16.war /usr/local/tomcat/webapps/repo.war
COPY wars/mms/share.3-31-16.war /usr/local/tomcat/webapps/share.war
RUN ls /usr/local/tomcat/webapps

COPY jars/postgresql-9.4-1201-jdbc41.jar /user/local/tomcat/lib/postgresql-9.4-1201-jdbc41.jar
COPY jars/h2-1.4.191.jar /user/local/tomcat/lib/h2-1.4.191.jar


# Don't think these commands are needed at the moment.
# RUN mkdir -p /usr/local/tomcat/webapps/alfresco-solr4 \
#     && mkdir -p /usr/local/tomcat/webapps/repo \
#     && mkdir -p /usr/local/tomcat/webapps/share
