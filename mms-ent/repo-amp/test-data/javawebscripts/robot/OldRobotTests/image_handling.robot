*** Settings ***
Documentation    Permissions Testing
Resource        ../resources.robot

*** Test Cases ***
PostArtifact
	[Documentation]		"Regression Test: 350. Post artifact to the master branch"
	[Tags]				350
	${post_json} =		Get File	 ../JsonData/x.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/europa/artifacts/folder1/folder2/xartifact		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetArtifact
	[Documentation]		"Regression Test: 360. Get artifact from the master branch"
	[Tags]				360
	${result} =			Get		url=${ROOT}/workspaces/master/artifacts/xartifact?extension=svg&cs=3463563326
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		url
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
