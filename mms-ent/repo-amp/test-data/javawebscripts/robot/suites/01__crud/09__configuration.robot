*** Settings ***
Documentation    Testing CRUD Operations on Configurations
Resource        ../resources.robot

*** Test Cases ***
#TODO: CONFIGS FAIL
PostConfig
	[Documentation]		"Regression Test: 140. Post configuration"
	[Tags]				140
	${post_json} =		Get File	 ../JsonData/configuration.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/europa/configurations		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 id
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetConfig
	[Documentation]		"Regression Test: 150. Get configurations"
	[Tags]				150
	${result} =			Get		url=${ROOT}/workspaces/master/sites/europa/configurations
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 id
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostConfigAgain
	[Documentation]		"Regression Test: 154. Post same configuration again"
	[Tags]				154
	${post_json} =		Get File	 ../JsonData/configuration.json
	${result} =			Post		url=${ROOT}/workspaces/master/configurations		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 id
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetConfigAgain
	[Documentation]		"Regression Test: 155. Get configurations"
	[Tags]				155
	${result} =			Get		url=${ROOT}/workspaces/master/sites/europa/configurations
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 id
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

