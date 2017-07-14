*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot
Suite Setup     Purge Results Directory

*** Test Cases ***
#PostNewImage
#	[Documentation]		"Post a image to PA."
#	[Tags]				I1
#	${post_json} =		Get File	    JsonData/PostNewElements.json
#	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
#	Should Be Equal		${result.status_code}		${200}
#	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
#	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
#	Sleep				${POST_DELAY_INDEXING}
#	${compare_result} =		Compare JSON		${TEST_NAME}
#	Should Match Baseline		${compare_result}
