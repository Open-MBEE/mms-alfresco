*** Settings ***
Library		OperatingSystem
Library		requests
Library		Collections
Library		json
Library		/Users/dank/git/mms-all-in-one/mms-ent/repo-amp/test-data/javawebscripts/robot/robot_lib.py

*** Variables ***
${AUTH}			admin:admin
${SERVER}		localhost:8080
${ROOT}			http://${AUTH}@${SERVER}/alfresco/service
#Notice the & instead of $, it represents a dictionary object
&{REQ_HEADER}		content-type=application/json
