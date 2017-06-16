@echo off
rem This script expects PATH variable to contain the curl bin directory

SETLOCAL enabledelayedexpansion
TITLE mms_mappings

SET HOSTNAME=%COMPUTERNAME%


SET MY_DIR=%~dp0

CALL curl > NUL 2>&1

if %ERRORLEVEL% geq 2 (
   echo curl binary not found. Please install it and update your PATH variable.
   echo MMS Update failed.
   EXIT /B 2
)



rem delete if previously exists and then create
printf "\ndeleting db: "
curl -XDELETE "http://localhost:9200/mms/"

printf "\ncreating shards: "
curl -XPUT "http://localhost:9200/mms/" -d @!MY_DIR!/mms_shard_mappings.json


rem create element type
rem "_id" : { "path" : "sysmlid", "store" : true, "index" : "not_analyzed"},
printf "\ncreating element mappings: "
curl -XPUT "http://localhost:9200/mms/_mapping/element" -d @!MY_DIR!/mms_element_mappings.json

printf "\ncreating commit mappings: "
curl -XPUT "http://localhost:9200/mms/_mapping/commit" -d @!MY_DIR!/mms_commit_mappings.json


rem $ curl -XPUT 'http://localhost:9200/my_index/my_type/_mapping' -d '
rem  curl -XPUT 'http://localhost:9200/mms'
rem  {
rem    "mappings": {
rem      "element": {
rem       "dynamic_templates": [
rem          {
rem            "index_new_ids": {
rem              "match_mapping_type": "string",
rem              "match_pattern":   "*Id",
rem              "mapping": {
rem                "type": "keyword"
rem              }
rem            }
rem          }
rem        ]
rem      }
rem    }
rem  }

ENDLOCAL
