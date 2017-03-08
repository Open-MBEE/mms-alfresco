#!/bin/bash

curl -w "%{http_code}\n" -X POST -u admin:admin -H "Content-Type:application/json" --data '{"name":"Test Project"}' "http://localhost:8080/view-repo/service/javawebscripts/sites/europa/projects/$1?fix=true&createSite=true"
