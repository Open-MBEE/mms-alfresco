*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot

*** Test Cases ***
UpdateMasterBranchJson
    [Documentation]     "Send an update to master branch JSON"
    [Tags]              critical    workspaces      010201
    log to console      merp
	${post_json} =		Get File		${CURDIR}/../../JsonData/UpdateMasterBranchObject.json
    ${result} =         Post    url=${ROOT}/projects/PA/refs        data=${post_json}        headers=&{REQ_HEADER}
	Sleep				${POST_DELAY_INDEXING}
	Should Be Equal		${result.status_code}		${200}
    ${result} =         Get     url=${ROOT}/projects/PA/refs/master
	Should Be Equal     ${result.json()['refs'][0]['name']}        merp
	${post_json} =		Get File		${CURDIR}/../../JsonData/RevertMasterBranchObject.json
	${result}           Post        url=${ROOT}/projects/PA/refs     data=${post_json}         headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
    ${result} =         Get     url=${ROOT}/projects/PA/refs/master
	Should Be Equal     ${result.json()['refs'][0]['name']}        master

