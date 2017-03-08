#!/usr/bin/env python

#must be places in test-data/javawebscripts directory
import os
import sys
import re
import optparse
import commands
from regression_lib import create_curl_cmd
from __builtin__ import True, False

#test_dir_path = "git/mms-all-in-one/mms-ent/repo-war/test-data/javawebscripts"
HOST = "localhost:8080"
SERVICE_URL = "http://%s/alfresco/service/"%HOST
BASE_URL_WS_NOBS = SERVICE_URL + "workspaces"
BASE_URL_WS = BASE_URL_WS_NOBS + "/"
common_filters = ['created','read','lastModified','modified','siteCharacterizationId','time_total']

#######################################

usageText = '''
    To create a workspace:
    
    ./record_curl.py -n CreateParentWorkspaceUnderMaster --description "Create theParentWorkspace under master" -g develop -t POST -p "" -w theParentWorkspace?sourceWorkspace=master --runBranches "develop" -f "branched,created,id,qualifiedId" -j
    Post parent workspace under master by supplying correct workspace branch
    Use filters "branched, "created", "id", "qualifiedId"
    -p is empty string so it doesn't add elements at the end of the URL
        
    To post an element to master:
    
    ./record_curl.py -n PostAToMaster --description "Post element a to master" -g develop -t POST -d a.json --runBranches "develop" -j --teardown set_read_to_gv1 
    Creates name of PostAToMaster with description in the developBaselineDir and puts it at the end of the harness
    Curl command arguments POST, data a.json file, under the workspace master, run regression with develop, compare using json diff
    Adds set_read_to_gv1 at the end of the table
    
    To post an element to workspace1:
    
    ./record_curl.py -n CreateTestData1InWorkspace1 --description "Create a parent folder in workspace1" -g develop -t POST -d '{"elements":[{"sysmlid":"testData1","name":"testData1","owner":"123456"}]}' -w "workspace1/" --runBranches "test,develop" -j
    Creates name and description in developBaselineDir
    Curl command uses POST and supplies a string of data to post in workspace1, run regression with test and develop, compare with json diff
    
    To get an element a from master:
    
    ./record_curl.py -n GetAInMaster --description "Get element a in master" -g develop -t GET -d elements/a --runBranches "test,develop" -j 
    
    To compare workspaces:
    
    ./record_curl.py -n CompareMasterAToLatest --description "Compare master to itself between post time of a and latest" -g develop -t GET -u SERVICE_URL -w "diff/master/master/2015-08-24T08:46:58.156-0700/latest" --runBranches "develop" -f "id,qualifiedId" -j
    Substitute URL with SERVICE_URL rather than BASE_URL_WS
    Supply the diff branch and timestamp 
    Using filters "id" and "qualifiedId" in output json
    
    Supplying -n <TESTNAME> and -t <TYPE> are mandatory because a baseline/regression table cannot be created without the name and curl command requires some type of HTTP call
    Adding setup, post process, teardown, and time delay functions to the regression table is possible but will not be executed when record_curl is run
    '''
    
parser = optparse.OptionParser(usage = usageText)

parser.add_option("-n", "--testName", help="Mandatory option: test name to create the baseline json")
parser.add_option("-g", "--gitBranch", default=os.getenv("GIT_BRANCH", "test"), help="Specify the branch to use or uses the $GIT_BRANCH value using 'test' if it doesn't exist")

parser.add_option("--host", default=HOST, help="DEFAULT: " + HOST)
parser.add_option("-t", "--type", default="", help="Type of curl command: POST, GET, DELETE")
parser.add_option("-d", "--data", default="", help="Data to post in json")
parser.add_option("-u", "--url", default=BASE_URL_WS, help="Base URL to use DEFAULT: " + BASE_URL_WS)
parser.add_option("-p", "--post", default="elements", help="Post-type: elements, views, products DEFAULT: elements")
parser.add_option("-w", "--workspace", default="master/", help="The workspace branch DEFAULT: master/")
parser.add_option("-o", "--project", dest="project", action="store_true", default=False, help="Set True if creating a project DEFAULT: False")
parser.add_option("-f", "--filter", default="", help="A string of comma separated values to be removed from the output i.e. \"filter1,filter2,filter3...\" (no spaces)")

#options to add test to the regression test harness
parser.add_option("--description", help="Test description")
parser.add_option("-j", "--jsonDiff", dest="jsonDiff", action="store_true", default=False, help="Set True if using json diff DEFAULT: False")
parser.add_option("--runBranches", default="", help="A string of comma separated branch names that will run this test by default")

parser.add_option("--setup", default=" ", help="Set up function (optional)")
parser.add_option("--postProcess", default=" ", help="Post process function (optional)")
parser.add_option("--teardown", default=" ", help="Tear down function (optional)")
parser.add_option("--timeDelay", default=" ", help="Delay in seconds before running the test (optional)")

options, args = parser.parse_args()

if options.host != "":
    HOST = options.host

SERVICE_URL = "http://%s/alfresco/service/"%HOST
BASE_URL_WS_NOBS = SERVICE_URL + "workspaces"
BASE_URL_WS = BASE_URL_WS_NOBS + "/"

#######################################
#Error Messages

#need a test name in order to create the baseline
if options.testName is None:
    parser.error("Test name needed to create baseline")

#if there is no -c input, the other six must be present
if options.type is None:
    parser.error("Type of curl command is required to create curl command")

#######################################
#finding the git branch
if "/" in options.gitBranch:
    options.gitBranch = options.gitBranch.split("/")[1]

#creating the baseline and directory names
baseline_dir = "%sBaselineDir"%options.gitBranch
baseline_json = "%s/%s.json"%(baseline_dir, options.testName)
baseline_orig_json = "%s/%s_orig.json"%(baseline_dir, options.testName)

curl_base_url = ""
curl_data = ""
#create curl command
if options.url == "BASE_URL_WS":
    curl_base_url = BASE_URL_WS
elif options.url == "SERVICE_URL":
    curl_base_url = SERVICE_URL
elif options.url == "BASE_URL_WS_NOBS":
    curl_base_url = BASE_URL_WS_NOBS
else:
    curl_base_url = options.url

if options.data and options.data[0] == "{" and options.data[-1] == "}":
    curl_data = "'" + options.data + "'"
else:
    curl_data = options.data

curl_cmd = create_curl_cmd(type=options.type, data=curl_data, base_url=curl_base_url, post_type=options.post, branch=options.workspace, project_post=options.project)

print "\n" + curl_cmd
user = raw_input("Is this the desired curl command? (y/n) ")

if user != "y":
    sys.exit()

#######################################

#os.chdir(test_dir_path)

#making the baseline directory if it doesn't exist
if not os.path.exists(baseline_dir):
    os.makedirs(baseline_dir)

#######################################

print "Executing curl command\n"
#returns the status and output of executing command in a shell
(status, output) = commands.getstatusoutput(curl_cmd + "> " + baseline_orig_json)
print output + "\n"
print "Creating baseline %s.json in %s"%(options.testName, baseline_dir)

useCommonFilters = False
if status == 0:
    file_orig = open(baseline_orig_json, "r")
    useCommonFilters = True
    #apply filters to output of curl cmd (not using output b/c getstatusoutput pipes stderr to stdout):
    orig_output = ""
    filter_output = ""
    if options.filter is not "":
        filters = options.filter.split(",")
        if useCommonFilters:
            filters.extend(common_filters)
        for line in file_orig:
            filterFnd = False
            for filter in filters:
                if filter != "time_total":
                    filter = '"' + filter + '"'
                #if the output contains the filter:
                if re.search(filter, line):
                    filterFnd = True
                    break

            #add line if it does not contain the filter:
            if not filterFnd:
                filter_output += (line)

            #always add lines to orig_output
            orig_output += (line)

    else:
        fileRead = file_orig.read()
        filter_output = fileRead
        orig_output = fileRead
    #write the baseline file with the output json file with filters
    file = open(baseline_json, "w")
    file.write(filter_output)
    file.close()
    file_orig.close()

def isTestNumber(testNum):
    try:
        int(testNum)
        return True
    except ValueError:
        return False
    
print "Adding test case into regression test harness"
file = open("regression_test_harness.py", "r")
lines = file.readlines()
file.close()

latestTest = 0
#check the file for the latest test case number
for line in lines:
    if isTestNumber(line[:-2]):
        currentNumber = int(line[:-2])
        if currentNumber > latestTest:
            latestTest = currentNumber

#creates table for the test harness
listOfFilters = "common_filters"
listOfBranches = ""

def createListOfValues(values, needSingleQuote):
#splits string up into individual values to concatenate into a string appropriate to the test harness
    values = values.split(",")
    listOfValues = '['
    for value in values:
        entry = '"' + value + '",'
        if needSingleQuote:
            entry = '\'"' + value + '"\','
        listOfValues += entry
    listOfValues = listOfValues[:-1] + ']'
    return listOfValues

if options.filter != "":
    listOfFilters += '+'
    listOfFilters += createListOfValues(options.filter, True)
if options.runBranches != "":
    listOfBranches = createListOfValues(options.runBranches, False)

optionalArguments = []
listOfOptionalFunctions = ""
#look through all of the optional arguments and see if one of them exists
if options.setup != " " or options.postProcess != " " or options.teardown != " " or options.timeDelay != " ":
    #compile all optional arguments into one list
    optionalArguments.append(options.setup)
    optionalArguments.append(options.postProcess)
    optionalArguments.append(options.teardown)
    optionalArguments.append(options.timeDelay)
    #replace all the default values to None
    optionalArguments = [argument.replace(" ", "None") for argument in optionalArguments]
    #reverse list so that all the None strings at the end are removed
    optionalArguments.reverse()
    i = 0
    while i == 0:
        if optionalArguments.index("None") == 0:
            optionalArguments.remove("None")
        else:
            i += 1
    #list still has elements so join the list with ,\n to make correct table
    if len(optionalArguments) != 0:
        optionalArguments.reverse()
        listOfOptionalFunctions += ',\n'
        compiledStringOfArguments = ',\n'.join(optionalArguments)
        listOfOptionalFunctions += compiledStringOfArguments          
 
if options.data and curl_data[0] == "'" and curl_data[-1] == "'":
    curl_data = "'\\\'" + options.data + "\\\''"
else:
    curl_data = "\"" + curl_data + "\""      
value = "[\n" + str(latestTest + 1) + ',\n"' + options.testName + '",\n"' + options.description + \
        '",\n' + 'create_curl_cmd(type="' + options.type + '", data=' + curl_data + ', base_url="' + \
        curl_base_url + '", post_type="' + options.post + '", branch="' + options.workspace + '", project_post=' + \
        str(options.project) + '),\n' + str(options.jsonDiff) + ',\n' + listOfFilters + ',\n' + listOfBranches + \
        listOfOptionalFunctions + '\n],\n'

#insert the brace and leave room so that the next test can be input
i = lines.index("]\n")
lines.insert(i, value + "\n")
lines = "".join(lines)

file = open("regression_test_harness.py", "w")
file.write(lines)
file.close()


