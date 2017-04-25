#!/bin/bash
# Downloads the spring-loaded lib if not existing and runs the full all-in-one
# (Alfresco + Share + Solr) using the runner project

if [ -f runserver.log ]; then
    cp runserver.log runserver.last.log
fi

if [ -f runserver.out ]; then
    cp runserver.out runserver.last.out
fi

if [ "$1" == "" ]; then
    pom=pom.xml
else
    pom=$1
fi

if [ -z "$JREBEL_JAR" ]; then
  echo "running without jrebel"
  MAVEN_OPTS="-Xms256m -Xmx8G -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n" mvn install -f $pom -P run 2>runserver.err 2>&1 | tee runserver.log | tee runserver.out;
else
  echo "running with jrebel"
  MAVEN_OPTS="-Xms256m -Xmx8G -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n -agentpath:$JREBEL_JAR" mvn install -f $pom -P run 2>runserver.err 2>&1 | tee runserver.log | tee runserver.out
fi
