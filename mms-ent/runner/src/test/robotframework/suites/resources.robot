*** Settings ***
Documentation       Suite for testing the permissions features of the MMS
Library             OperatingSystem
Library             Collections
Library             json
Library             requests
Library             ${CURDIR}/../robot_lib.py

*** Variables ***
${AUTH}         admin:admin
${SERVER}       localhost:8080
${ROOT}         http://${AUTH}@${SERVER}/alfresco/service
#Notice the & instead of $, it represents a dictionary object
&{REQ_HEADER}       content-type=application/json
&{PNG_HEADER}       content-type=image/png
&{JPG_HEADER}       content-type=image/jpg
&{SVG_HEADER}       content-type=image/svg
&{PNG_GET_HEADER}       Accept=image/png
&{JPG_GET_HEADER}       Accept=image/jpg
&{SVG_GET_HEADER}       Accept=image/svg
# GLOBAL DELAYS TO ALLOW FOR INDEXING IN SOLR
${POST_DELAY_INDEXING}      2s
${DELETE_DELAY_INDEXING}    10s

# UNCOMMENT TO TEST KEYWORDS -- This will make all other tests fail!
#*** Test Cases ***
#Test Create Workspace
#    [Documentation]     Test to create a workspace
#    ${json_result} =        Create Workspace JSON   robotTest
#    Log To Console          ${json_result}
#    ${json_result} =        Create Workspace        robotTest
#
#
#Test Create SubWorkspace
#    ${json_result} =        Create SubWorkspace     merp
#    Log To Console      ${json_result}


*** Keywords ***
Purge Results Directory
    Remove Directory        ${CURDIR}/../output/results/         recurse=True
    Create Directory        ${CURDIR}/../output/results/

Should Match Baseline
    [Arguments]         ${result}
    Run Keyword If      ${result} == False      Fail        Result did not match baseline.
#   Should Be True      ${result}

Generate JSON
    [Arguments]                 ${name}     ${json}     ${filter_list}
    write json to file          ${json}     ${name}
    filter json file            ${name}     ${filter_list}
    Return From Keyword         ${json}

Create Workspace
    [Documentation]     Create a workspace
    [Arguments]         ${workspace_id}
    ${post_json} =      Get File     newJsonData/CreateWorkspace_${workspace_id}.json
    ${result} =         Post        url=${ROOT}/workspaces/wsA?sourceWorkspace=${workspace_id}       data=${post_json}      headers=&{REQ_HEADER}
    Should Be Equal     ${result.status_code}       ${200}
    ${filter} =         Create List        commitId     nodeRefId        versionedRefId      _created        read        lastModified        _modified       siteCharacterizationId      time_total      _elasticId      branched        _created        sysmlId         qualifiedId        commitId        created         id      modified        qualifiedName
    Generate JSON           ${TEST_NAME}        ${result.json()}        ${filter}

Create Subworkspace
    [Documentation]     Create workspace inside a workspace
    [Arguments]         ${workspace_id}         ${name}        ${filter_list}=
    ${result} =         Post        url=${ROOT}/workspaces/subworkspace1?sourceWorkspace=${workspace_id}      data='{}'       headers=&{REQ_HEADER}
    Should Be Equal     ${result.status_code}       ${200}
    Generate JSON       Subworkspace_${name}.json       ${result.json()}        ${filter_list}
    Return From Keyword     ${result.json()}

Create Subworkspace With CopyTime
    [Documentation]     Create workspace inside a workspace
    [Arguments]         ${workspace_id}     ${copyTime}     ${filter_list}=
    ${result} =         Post        url=${ROOT}/workspaces/subworkspace1?sourceWorkspace=${workspace_id}&copyTime=${copyTime}      data='{}'       headers=&{REQ_HEADER}
    Should Be Equal     ${result.status_code}       ${200}
    Generate JSON       Subworkspace_${workspace_id}.json       ${result.json()}        ${filter_list}
    Return From Keyword     ${result.json()}

Delete Project
    [Documentation]     Deletes project with given projectId
    [Arguments]         ${projectId}
    ${result} =         Delete      url=${ROOT}/projects/${projectId}
    Should Be Equal     ${result.status_code}       ${200}
    Return From Keyword     ${result.json()}

Clean Database
    [Documentation]  Deletes the PA, PB, PC, and PD projects from the MMS and ElasticSearch
    Delete Project  PA
    Delete Project  PB
    Delete Project  PC
    Delete Project  PD
