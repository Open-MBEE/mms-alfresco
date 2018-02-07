# Docker MMS README

Build container
```
docker build . -t mms-container
```

To run the container:
- we need to specify the mount directory 
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
-e PG_HOST={PG_ID_ADDR} 
```

- PostgreSQL User
```
-e PG_DB_USER=${PG_USER}
```

- User Password
```
-e PG_DB_PASS={PG_PASS} 
```

- ElasticSearch Host
```
-e ES_HOST={PG_ID_ADDR}
```

Example running the container
```
docker run --mount source=mmsvol,target=/mnt/alf_data -p 8080:8080 -e PG_HOST={PG_ID_ADDR} -e PG_DB_NAME=mms -e PG_DB_USER=${PG_USER} -e PG_DB_PASS={PG_PASS} -e ES_HOST={PG_ID_ADDR} -d mms-container
```
