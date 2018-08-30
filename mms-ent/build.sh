#!/bin/bash
# Downloads the spring-loaded lib if not existing and runs the full all-in-one
# (Alfresco + Share + Solr) using the runner project


if [ "$1" == "" ]; then
    MAVEN_OPTS="-Xms256m -Xmx8G -Xdebug" ./mvnw clean package -f pom.xml -P build -Ddependency.surf.version=6.3 -Dmaven.test.skip=true -U -Dmaven.repo.local=$HOME/.m2/repository;
else
    ./mvnw versions:set -DnewVersion=$1 -DgenerateBackupPoms=false -Ddependency.surf.version=6.3
    rm -f ../.circleci/config.yml
    sed "s/\${version}/$1/" ../.circleci/config.yml.template > ../.circleci/config.yml
fi
