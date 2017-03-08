#!/bin/bash

flag=$1
arg=$2

if [ "$arg" == "" ]; then
  arg="ison"
fi

if [ "$flag" == "" ]; then
  flag="debug"
fi

echo curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/flags/$flag?$arg"
curl -w "\n%{http_code}\n" -X GET -u admin:admin "http://localhost:8080/alfresco/service/flags/$flag?$arg"

