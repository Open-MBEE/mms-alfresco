*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot


*** Test Cases ***

SearchIncludingType
    [Documentation]     "Search for an element and provide flag checkType. Should return owner if they element type a property or grandowner if is a slot. Test will try to search for element with sysmlid 302, and it should return element with sysmlid 301."
    [Tags]              search      critical        0701
    ${post_json}        Get File        ${CURDIR}/../../JsonData/SearchQueryWithType.json
    ${result}           Post            url=${ROOT}/projects/PA/refs/master/search?checkType=true      data=${post_json}       headers=&{REQ_HEADER}
    Should Be Equal     ${result.status_code}       ${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
    Generate JSON           ${TEST_NAME}        ${result.json()}        ${filter}
    ${compare_result} =     Compare JSON        ${TEST_NAME}
    Should Match Baseline       ${compare_result}

SearchLiteral
    [Documentation]     "Search for an element and return literal json from elastic."
    [Tags]              search      critical        0702
    ${post_json}        Get File        ${CURDIR}/../../JsonData/SearchQueryLiteral.json
    ${result}           Post            url=${ROOT}/projects/PA/refs/master/search?literal=true      data=${post_json}       headers=&{REQ_HEADER}
    Should Be Equal     ${result.status_code}       ${400}
	${filter} =			Create List	 _id    _shards     _score  sort    took    _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
    Generate JSON           ${TEST_NAME}        ${result.json()}        ${filter}
    ${compare_result} =     Compare JSON        ${TEST_NAME}
    Should Match Baseline       ${compare_result}

SearchWithAgg
    [Documentation]     "Search for an element with aggregations."
    [Tags]              search      critical        0703
    ${post_json}        Get File        ${CURDIR}/../../JsonData/SearchQueryWithAgg.json
    ${result}           Post            url=${ROOT}/projects/PA/refs/master/search      data=${post_json}       headers=&{REQ_HEADER}
    Should Be Equal     ${result.status_code}       ${200}
	${filter} =			Create List	 _id    _shards     _score  sort    took    _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
    Generate JSON           ${TEST_NAME}        ${result.json()}        ${filter}
    ${compare_result} =     Compare JSON        ${TEST_NAME}
    Should Match Baseline       ${compare_result}
