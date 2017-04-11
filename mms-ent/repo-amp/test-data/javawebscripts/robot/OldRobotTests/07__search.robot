*** Settings ***
Documentation    Testing Search Features
Resource        ../resources.robot

*** Test Cases ***
GetSearchSlotProperty
	[Documentation]		"Regression Test: 671. Searching for the property "real" having value 5.39 (slot property)"
	[Tags]				671
	${result} =			Get		url=${ROOT}/workspaces/master/search?keyword=5.39&filters=value&propertyName=real
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSearchSlotPropertyOffNom
	[Documentation]		"Regression Test: 672. Searching for the property "foo" having value 5.39 (slot property).  This should fail"
	[Tags]				672
	${result} =			Get		url=${ROOT}/workspaces/master/search?keyword=5.39&filters=value&propertyName=foo
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSearchNonSlotProperty
	[Documentation]		"Regression Test: 673. Searching for the property "real55" having value 34.5 (non-slot property)"
	[Tags]				673
	${result} =			Get		url=${ROOT}/workspaces/master/search?keyword=34.5&filters=value&propertyName=real55
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSearchNonSlotPropertyOffNom
	[Documentation]		"Regression Test: 674. Searching for the property "real55" having value 34.5 (non-slot property).  This should fail."
	[Tags]				674
	${result} =			Get		url=${ROOT}/workspaces/master/search?keyword=34.5&filters=value&propertyName=gg
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSearchElementWithProperty
	[Documentation]		"Regression Test: 675. Searching for element that owns a Property"
	[Tags]				675
	${result} =			Get		url=${ROOT}/workspaces/master/search?keyword=steroetyped&filters=name
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSearchDocumentation
	[Documentation]		"Regression Test: 10000. Get search documentation"
	[Tags]				10000
	${result} =			Get		url=${ROOT}/workspaces/master/search?keyword=some*&filters=documentation
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSearchAspects
	[Documentation]		"Regression Test: 10001. Get search aspects"
	[Tags]				10001
	${result} =			Get		url=${ROOT}/workspaces/master/search?keyword=View&filters=type
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSearchId
	[Documentation]		"Regression Test: 10002. Get search id"
	[Tags]				10002
	${result} =			Get		url=${ROOT}/workspaces/master/search?keyword=300&filters=sysmlId
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSearchValue
	[Documentation]		"Regression Test: 10003. Get search value"
	[Tags]				10003
	${result} =			Get		url=${ROOT}/workspaces/master/search?keyword=dlam_string&filters=value
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 qualifiedId		 sysmlid
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

