#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo -n "creating element mappings: "
curl -XPUT http://localhost:9200/_template/template -d @$DIR/mapping_template.json
echo
