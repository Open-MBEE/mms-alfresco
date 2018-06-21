#!/bin/sh

count=`ls -1 ./files/*.amp 2>/dev/null | wc -l`
if [[ ${count} < 2 ]];then
    (cd ../; ./mvnw clean -Ppurge && ./mvnw install -Pbuild)
    cp ../repo-amp/target/*.amp ./files/
    cp ../share-amp/target/*.amp ./files/
fi

if [ ! -f ./files/mms.properties.example ];then
    cp ../mms.properties.example ./files/
fi

docker build . -t mms-image
