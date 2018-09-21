#!/bin/bash
DOCKER_CHECK=$( which docker )
if [ ! $DOCKER_CHECK ];then
    echo "Docker not found in PATH."
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

POSTGRES_INSTANCE=$( nc -z -v 127.0.0.1 5432 2>&1 | grep open | wc -l )
POSTGRES_USER=mmsuser
POSTGRES_PASS=test123
DB_SCHEMA=$( cat $DIR/repo-amp/src/main/resources/mms.sql | tail -n +2 | tr -d "\n" )

ELASTIC_INSTANCE=$( nc -z -v 127.0.0.1 9200 2>&1 | grep open | wc -l )
ELASTIC_MAPPING=$DIR/repo-amp/src/main/resources/mapping_template.json

if [ $POSTGRES_INSTANCE -eq 0 ];then
    docker run -d --name postgres-docker --publish=5432:5432 -e POSTGRES_USER=${POSTGRES_USER} -e POSTGRES_PASSWORD=${POSTGRES_PASS} postgres:9.4-alpine
else
    echo 'Existing database found.'
    docker rm -f postgres-docker
    docker run -d --name postgres-docker --publish=5432:5432 -e POSTGRES_USER=${POSTGRES_USER} -e POSTGRES_PASSWORD=${POSTGRES_PASS} postgres:9.4-alpine
fi

if [ $ELASTIC_INSTANCE -eq 0 ];then
    docker run -d --name elasticsearch-docker --publish=9200:9200 elasticsearch:5.5-alpine
else
    echo 'Existing elasticsearch server found.'
    docker rm -f elasticsearch-docker
    docker run -d --name elasticsearch-docker --publish=9200:9200 elasticsearch:5.5-alpine
fi

while [ $( docker exec -it postgres-docker ps aux | grep postgres | uniq | wc -l ) -lt 6 ] || [ $( curl -v --silent localhost:9200/_cat/health 2>&1 | grep 'green' | wc -l ) -lt 1 ];do
    sleep 1;
done

docker exec -it postgres-docker psql -h localhost -U postgres -c "ALTER ROLE ${POSTGRES_USER} CREATEDB"
docker exec -it postgres-docker createdb -h localhost -U ${POSTGRES_USER} mms
docker exec -it postgres-docker psql -h localhost -U ${POSTGRES_USER} -d mms -c "${DB_SCHEMA}"
echo 'Postgresql initialized'

curl -s -o /dev/null -d @${ELASTIC_MAPPING} -XPUT http://localhost:9200/_template/template
echo 'Elasticsearch mappings applied'

rm -rf $DIR/alf_data_dev

./mvnw clean -Ppurge && ./mvnw install -Prun,robot-tests
