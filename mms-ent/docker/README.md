# MMS Docker README

Build Docker image
```
docker build . -t openmbee/mms
```

To run the container:
- For persistence, we need to specify the mount directory
```
--mount source=mmsvol,target=/mnt/alf_data
```
    
- expose the 8080 port
```
-p 8080:8080
```

Set the PostgreSQL info:

- Host address.
```
-e PG_HOST=${PG_HOST}
```
Formatted as `[ip|hostname]:port`, e.g. `127.0.0.1:5432` or `postgres.openmbee.org:5432`

- PostgreSQL User
```
-e PG_DB_USER=${PG_DB_USER}
```

- User Password
```
-e PG_DB_PASS=${PG_DB_PASS} 
```

- Elasticsearch Host
```
-e ES_HOST=${ES_HOST}
```

Example running the container
```
docker run --mount source=mmsvol,target=/mnt/alf_data -p 8080:8080 -e PG_HOST=${PG_HOST} -e PG_DB_NAME=mms -e PG_DB_USER=${PG_DB_USER} -e PG_DB_PASS=${PG_DB_PASS} -e ES_HOST=${ES_HOST} -d openmbee/mms
```

Example for initializing PostgreSQL in Docker

```
docker run -d --name postgres-docker --publish=5432:5432 -e POSTGRES_USER=${PG_DB_USER} -e POSTGRES_PASSWORD=${PG_DB_PASS} postgres:9.4-alpine
docker exec -it postgres-docker psql -h localhost -U postgres -c "ALTER ROLE ${PG_DB_USER} CREATEDB"
docker exec -it postgres-docker createdb -h localhost -U ${PG_DB_USER} alfresco
docker exec -it postgres-docker createdb -h localhost -U ${PG_DB_USER} mms
docker exec -it postgres-docker psql -h localhost -U ${PG_DB_USER} -d mms -c "create table if not exists organizations (   id bigserial primary key,   orgId text not null,   orgName text not null,   constraint unique_organizations unique(orgId, orgName) ); create index orgId on organizations(orgId);  create table projects (   id bigserial primary key,   projectId text not null,   orgId integer references organizations(id),   name text not null,   location text not null,   constraint unique_projects unique(orgId, projectId) ); create index projectIdIndex on projects(projectid);"
```

Example for initializing Elasticsearch in Docker
```
docker run -d --name elasticsearch-docker --publish=9200:9200 elasticsearch:5.5-alpine
curl -XPUT http://localhost:9200/_template/template -d @repo-amp/src/main/resources/mapping_template.json
```
