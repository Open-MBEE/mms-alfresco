*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot

*** Test Cases ***
#Create new branch from PA
# Add elements
# Compare
GetAllElementsByProjectAndRef
    [Documentation]     "Compare returned models"
    [Tags]              critical    workspaces      010301
    ${resultA} =         Get     url=${ROOT}/projects/PA/refs/master/elements
    ${resultB} =         Get     url=${ROOT}/projects/PA/refs/master/elements/PA?depth=-1
	Should Be Equal     ${resultA.json()}        ${resultB.json()}
