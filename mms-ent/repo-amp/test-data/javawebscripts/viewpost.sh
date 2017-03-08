#!/bin/bash
curl -w "%{http_code}\n" -X POST -u admin:admin -H "Content-Type:application/json" --data @$1 "http://localhost:8080/view-repo/service/javawebscripts/views"
