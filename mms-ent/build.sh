#!/bin/bash
# Downloads the spring-loaded lib if not existing and runs the full all-in-one
# (Alfresco + Share + Solr) using the runner project


if [ "$1" == "" ]; then
    MAVEN_OPTS="-Xms256m -Xmx8G -Xdebug" mvn clean package -f pom.xml -Dmaven.test.skip=true -U -Dmaven.repo.local=$HOME/.m2/repository;
else
    mvn versions:set -DnewVersion=$1 -DgenerateBackupPoms=false
fi

