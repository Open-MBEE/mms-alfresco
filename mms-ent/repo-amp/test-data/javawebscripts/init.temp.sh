#!/bin/bash

echo "curl -w '\n%{http_code}\n' -u admin:admin -X POST -H 'Content-Type:application/json' --data '{'elements':[{'sysmlid':'PROJECT-5c79ccb6-68a2-4411-be7b-339a05b9e779','name':'HierarchyTest','specialization':{'type':'Project'}}]}' 'http://localhost:8080/alfresco/service/workspaces/master/sites/vetest/projects?createSite=true'"

curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" --data '{"elements":[{"sysmlid":"PROJECT-5c79ccb6-68a2-4411-be7b-339a05b9e779","name":"HierarchyTest","specialization":{"type":"Project"}}]}' "http://localhost:8080/alfresco/service/workspaces/master/sites/vetest/projects?createSite=true"
 
echo "./projectpost hierarchyproject.json"
./projectpost hierarchyproject.json

echo "./modelpost hierarchypretty.json"
./modelpost hierarchypretty.json
