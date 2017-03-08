#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# delete if previously exists and then create
curl -XDELETE 'http://localhost:9200/mms/'
echo
echo "deleted db"

curl -XPUT 'http://localhost:9200/mms/' -d '{
"settings" : {
        "index" : {
            "number_of_shards" : 3
        },
        "analysis": {
            "analyzer": {
                "include_hyphen": {
                    "type": "pattern",
                    "pattern": "[^-_a-zA-Z0-9]+",
                    "lowercase": true
                }
            }
        }
    }
}'
echo
echo "created shards"
# create element type
#"_id" : { "path" : "sysmlid", "store" : true, "index" : "not_analyzed"},
curl -XPUT http://localhost:9200/mms/_mapping/element -d @$DIR/mms_mappings.json
echo
echo "element mapping"
curl -XPUT http://localhost:9200/mms/_mapping/commit -d @$DIR/mms_commit_mappings.json
echo
echo "commit mapping"
#$ curl -XPUT 'http://localhost:9200/my_index/my_type/_mapping' -d '
# create master holding bin
curl -XPOST 'http://localhost:9200/mms/element/holding_bin' -d '{
"element":{"name":"holding_bin","sysmlid":"holding_bin", "creator":"admin", "created":"2013-01-25T02:40:31.075-0800"}
}'
echo "holding bin"
