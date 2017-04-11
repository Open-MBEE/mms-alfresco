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
        }
    },
    "mappings": {
        "element": {
            "dynamic_templates": [
            {
                "id_as_keywords": {
                    "match_mapping_type": "string",
                    "match_pattern": "regex",
                    "match": ".*(Id|Ids)",
                    "mapping": {
                        "type": "keyword"
                    }
                }
            },
            {
                "id_and_type": {
                    "match_mapping_type": "string",
                    "match_pattern": "regex",
                    "match": "(id|ids|type|uri|URI)",
                    "mapping": {
                        "type": "keyword"
                    }
                }
            },
            {
                "boolean": {
                    "match_mapping_type": "*",
                    "match_pattern": "regex",
                    "match": "is[A-Z].*",
                    "mapping": {
                        "type": "boolean"
                    }
                }
            },
            {
                "text": {
                    "match_mapping_type": "string",
                    "match_pattern": "regex",
                    "match": "(body|documentation)",
                    "mapping": {
                        "type": "text"
                    }
                }
            },
            {
                "value": {
                    "match_mapping_type": "*",
                    "path_unmatch": "value",
                    "match": "value",
                    "mapping": {
                        "type": "text",
                        "fields": {
                            "keyword": {
                                "type": "keyword",
                                "ignore_above": 256
                            }
                        }
                    }
                }
            }
            ]
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

