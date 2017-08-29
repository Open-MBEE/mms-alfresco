#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# delete if previously exists and then create
echo -n "deleting db: "
curl -XDELETE 'http://localhost:9200/mms/'
echo

echo -n "creating shard mappings: "
curl -XPUT 'http://localhost:9200/mms/' -d @$DIR/mms_shard_mappings.json
echo

# create element type
#"_id" : { "path" : "sysmlid", "store" : true, "index" : "not_analyzed"},
echo -n "creating element mappings: "
curl -XPUT http://localhost:9200/_template/template -d @$DIR/mapping_template.json
echo

#echo -n "creating commit mappings: "
#curl -XPUT http://localhost:9200/mms/_mapping/commit -d @$DIR/mms_commit_mappings.json
#echo

#$ curl -XPUT 'http://localhost:9200/my_index/my_type/_mapping' -d '
# curl -XPUT 'http://localhost:9200/mms'
# {
#   "mappings": {
#     "element": {
#       "dynamic_templates": [
#         {
#           "index_new_ids": {
#             "match_mapping_type": "string",
#             "match_pattern":   "*Id",
#             "mapping": {
#               "type": "keyword"
#             }
#           }
#         }
#       ]
#     }
#   }
# }

