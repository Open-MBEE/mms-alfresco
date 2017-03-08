*** Settings ***
Documentation    Testing Projects
Resource        ../resources.robot

*** Test Cases ***
PostSitePackage
	[Documentation]		"Regression Test: 430. Create a site package"
	[Tags]				430
	${post_json} =		Get File	 ../JsonData/SitePackage.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/europa/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${400}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostElementSitePackage
	[Documentation]		"Regression Test: 440. Post a product to a site package"
	[Tags]				440
	${post_json} =		Get File	 ../JsonData/ElementSitePackage.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/site_package/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 message
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSitePackageProducts
	[Documentation]		"Regression Test: 450. Get site package products"
	[Tags]				450
	${result} =			Get		url=${ROOT}/workspaces/master/sites/site_package/products
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

SitePackageBugTest1
	[Documentation]		"Regression Test: 451. Create packages A2, B2, and C2, where A2/B2 are site packages for CMED-871 testing"
	[Tags]				451
	${post_json} =		Get File	 ../JsonData/SitePkgBugTest1.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/europa/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

SitePackageBugTest2
	[Documentation]		"Regression Test: 452. Moves package B2 under package C2 for CMED-871 testing"
	[Tags]				452
	${post_json} =		Get File	 ../JsonData/SitePkgBugTest2.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/europa/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

SitePackageBugTest3
	[Documentation]		"Regression Test: 453. Makes package C2 a site package for CMED-871 testing"
	[Tags]				453
	${post_json} =		Get File	 ../JsonData/SitePkgBugTest3.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/europa/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
