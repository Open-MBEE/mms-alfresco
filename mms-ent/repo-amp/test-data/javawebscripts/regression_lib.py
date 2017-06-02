#
# Library methods for the regression harness.
#

import os
import commands
import re
import time
import subprocess
import sys
import optparse
import glob
import json
import datetime
# CURL_STATUS = '-w "\\n%{http_code}\\ntime_total:%{time_total}\\n"'
CURL_STATUS = '-w "\\n%{http_code}\\n"'
CURL_POST_FLAGS_NO_DATA = "-X POST"
CURL_POST_FLAGS = '-X POST -H "Content-Type:application/json" --data'
CURL_POST_FLAGS_K = '-X POST -H "Content-Type:application/k" --data'
CURL_PUT_FLAGS = "-X PUT"
CURL_GET_FLAGS = "-X GET"
CURL_DELETE_FLAGS = "-X DELETE"
CURL_USER = " -u admin:admin"
CURL_FLAGS = CURL_STATUS + CURL_USER
HOST = "localhost:8080"
SERVICE_URL = "http://%s/alfresco/service/" % HOST
BASE_URL_WS_NOBS = SERVICE_URL + "workspaces"
BASE_URL_WS = BASE_URL_WS_NOBS + "/"

failed_tests = 0
errs = []
passed_tests = 0
result_dir = ""
baseline_dir = ""
display_width = 100
test_dir_path = "test-data/javawebscripts"
test_nums = []
test_names = []
create_baselines = False
common_filters = ['"nodeRefId"', '"versionedRefId"', '"created"','"read"','"lastModified"','"modified"','"siteCharacterizationId"','"elasticId"','time_total']
cmd_git_branch = None

tests = []
service_flags = []

# Some global variables for lambda functions in tests
gv1 = None
gv2 = None
gv3 = None
gv4 = None
gv5 = None
gv6 = None
gv7 = None

# These capture the curl output for any teardown functions
orig_output = None
filtered_output = None
orig_json = None
filtered_json = None
filter_output = None

def set_gv1( v ):
    global gv1
    gv1 = v
def set_gv2( v ):
    global gv2
    gv2 = v
def set_gv3( v ):
    global gv3
    gv3 = v
def set_gv4( v ):
    global gv4
    gv4 = v
def set_gv5( v ):
    global gv5
    gv5 = v
def set_gv6( v ):
    global gv6
    gv6 = v
def set_gv7( v ):
    global gv7
    gv7 = v

import re

def get_json_output_no_status(output=None):
    '''Gets the output json and removes the status code from it for use in the next test'''

    json_output = ""
    #print 'orig_output=' + str(orig_output)
    output_good = output != None and len(str(output)) > 5
    original_good = orig_output != None and len(str(orig_output)) > 5
    if output_good or original_good:
        my_output = output if output_good else orig_output
        # find the status code at the end of the string and remove it
        # comput i as the index to the start of the status code
        # walk backwards over whitespace
        i=len(my_output)-1
        while i >= 0:
            if (my_output[i] == '}'):
                i = i + 1
                break
            i = i - 1
        # set json_output to my_output without the ststus code
        if i > 0:
            json_output = my_output[0:i]
#         json_output = re.sub(r'^[2][0-9][0-9]', r'', my_output, 1)
#         #json_output = re.sub("^$", "", json_output)
        #print 'json_output=' + str(json_output)

    return json_output

def do20():
    ''' Gets the "modified" date out of the json output and sets gv4 to it.'''
    modDate = None
    json_output = get_json_output_no_status()
    if json_output:
        j = json.loads(json_output)
        #print "j=" + str(j)
        if j and j['workspace2'] and j['workspace2']['updatedElements'] and \
        len(j['workspace2']['updatedElements']) > 0:
            modDate = j['workspace2']['updatedElements'][0]['modified']
    set_gv4(modDate)

def removeCmNames():
    global filter_output
    #print("filter_output before: " + str(filter_output))
    rrr = re.sub("cm_[a-zA-Z0-9-]*", "some_cm_name", str(filter_output), 0)
    filter_output = str(rrr)
    #print("filter_output after: " + filter_output)

def set_json_output_to_gv(gv):
    '''Sets the json output to gv variable'''
    if gv == 1:
        set_gv1(get_json_output_no_status().replace("\n",""))
    elif gv == 2:
        set_gv2(get_json_output_no_status().replace("\n",""))
    elif gv == 3:
        set_gv3(get_json_output_no_status().replace("\n",""))
    elif gv == 4:
        set_gv4(get_json_output_no_status().replace("\n",""))
    elif gv == 5:
        set_gv5(get_json_output_no_status().replace("\n",""))
    elif gv == 6:
        set_gv6(get_json_output_no_status().replace("\n",""))

def set_json_output_to_gv1():
    '''Sets the json output to gv1 variable'''
    set_json_output_to_gv(1)

def set_json_output_to_gv2():
    '''Sets the json output to gv2 variable'''
    set_json_output_to_gv(2)

def set_json_output_to_gv3():
    '''Sets the json output to gv3 variable'''
    set_json_output_to_gv(3)

def do176():
    '''Get the json output, modifies the description and name keys,
       and set to gv1'''
    json_output = get_json_output_no_status()
    j = json.loads(json_output)

    if j and j['workspaces'] and j['workspaces'][0]:
        j["workspaces"][0]["description"] = "modified the workspace name and desc"
        j["workspaces"][0]["name"] = "modifiedWorkspaceName"

    set_gv3(json.dumps(j))

def set_wsid_to_gv(gv):
    '''Get the json output, and sets gvi to the that workspace id'''
    json_output = get_json_output_no_status()
    j = json.loads(json_output)

    if j and 'workspaces' in j and len(j['workspaces']) > 0: # and j['workspaces'][0]['id']:
        if gv == 1:
            set_gv1(j["workspaces"][0]["id"])
        elif gv == 2:
            set_gv2(j["workspaces"][0]["id"])
        elif gv == 3:
            set_gv3(j["workspaces"][0]["id"])
        elif gv == 4:
            set_gv4(j["workspaces"][0]["id"])
        elif gv == 5:
            set_gv5(j["workspaces"][0]["id"])
        elif gv == 6:
            set_gv6(j["workspaces"][0]["id"])
        elif gv == 7:
            set_gv7(j["workspaces"][0]["id"])

def set_wsid_to_gv1():
    '''Get the json output, and sets gv1 to the that workspace id'''
    set_wsid_to_gv(1)

def set_wsid_to_gv2():
    '''Get the json output, and sets gv2 to the that workspace id'''
    set_wsid_to_gv(2)

def set_wsid_to_gv3():
    '''Get the json output, and sets gv3 to the that workspace id'''
    set_wsid_to_gv(3)

def set_wsid_to_gv4():
    '''Get the json output, and sets gv4 to the that workspace id'''
    set_wsid_to_gv(4)

def set_wsid_to_gv5():
    '''Get the json output, and sets gv5 to the that workspace id'''
    set_wsid_to_gv(5)

def set_wsid_to_gv6():
    '''Get the json output, and sets gv6 to the that workspace id'''
    set_wsid_to_gv(6)

def set_read_to_gv(gv, idx=0):
    '''Get the json output, and sets gvi to the read time'''
    json_output = get_json_output_no_status()
    #print("json_output=" + str(json_output))
    j = json.loads(json_output)
    #print("j=" + str(j))

    if j:
        if gv == 1:
            set_gv1(j["elements"][idx]["read"])
        elif gv == 2:
            set_gv2(j["elements"][idx]["read"])
        elif gv == 3:
            set_gv3(j["elements"][idx]["read"])
        elif gv == 4:
            set_gv4(j["elements"][idx]["read"])
        elif gv == 5:
            set_gv5(j["elements"][idx]["read"])
        elif gv == 6:
            set_gv6(j["elements"][idx]["read"])
        elif gv == 7:
            set_gv7(j["elements"][idx]["read"])

def set_read_to_gv1():
    '''Get the json output, and sets gv1 to the read time'''
    set_read_to_gv(1)

def set_read_to_gv2():
    '''Get the json output, and sets gv2 to the read time'''
    set_read_to_gv(2)

def set_read_to_gv3():
    '''Get the json output, and sets gv3 to the read time'''
    set_read_to_gv(3)

def set_read_to_gv4():
    '''Get the json output, and sets gv4 to the read time'''
    set_read_to_gv(4)

def set_read_to_gv5():
    '''Get the json output, and sets gv5 to the read time'''
    set_read_to_gv(5)

def set_read_to_gv6():
    '''Get the json output, and sets gv6 to the read time'''
    set_read_to_gv(6)

def set_read_to_gv7():
    '''Get the json output, and sets gv7 to the read time'''
    set_read_to_gv(7)

def set_last_read_to_gv1():
    '''Get the json output, and sets gv1 to the latest read time'''
    set_read_to_gv(1,idx=-1)

def set_last_read_to_gv2():
    '''Get the json output, and sets gv2 to the latest read time'''
    set_read_to_gv(2,idx=-1)

def set_last_read_to_gv3():
    '''Get the json output, and sets gv3 to the latest read time'''
    set_read_to_gv(3,idx=-1)

def set_last_read_to_gv4():
    '''Get the json output, and sets gv4 to the latest read time'''
    set_read_to_gv(4,idx=-1)

def set_last_read_to_gv5():
    '''Get the json output, and sets gv5 to the latest read time'''
    set_read_to_gv(5,idx=-1)

def set_last_read_to_gv6():
    '''Get the json output, and sets gv6 to the latest read time'''
    set_read_to_gv(6,idx=-1)

def set_last_read_to_gv7():
    '''Get the json output, and sets gv7 to the latest read time'''
    set_read_to_gv(7,idx=-1)

def set_read_to_gv6_delta_gv7(delta=3):
    '''Get the json output, and sets gv6 to the read time'''
    global gv6,gv7

    set_read_to_gv(6)
    if gv6 and "." in gv6:
     date = datetime.datetime.strptime(gv6.split(".")[0], "%Y-%m-%dT%H:%M:%S")
     date = date + datetime.timedelta(seconds=delta)
     gv7 = date.strftime("%Y-%m-%dT%H:%M:%S.000")

def set_gv1_to_current_time():
    '''Gets the current time'''
    global gv1
    gv1 = get_current_time(0)
    print "set gv1 to current time " + gv1
    return gv1

def get_current_time(delay=3):
    '''Gets the current time and adds delay mins b/c it lags behind'''
    a = datetime.datetime.now() + datetime.timedelta(minutes=delay)
    return a.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]

def set_read_delta_to_gv1(delta=7):
    '''Get the json output, and sets gv1 to the read time - delta secs'''
    global gv1

    set_read_to_gv1()
    if gv1 and "." in gv1:
        date = datetime.datetime.strptime(gv1.split(".")[0], "%Y-%m-%dT%H:%M:%S")
        date = date - datetime.timedelta(seconds=delta)
        gv1 = date.strftime("%Y-%m-%dT%H:%M:%S.000")

def set_read_delta_to_gv2(delta=10):
    '''Get the json output, and sets gv1 to the read time + delta secs'''
    global gv2

    set_read_to_gv2()
    if gv2 and "." in gv2:
        date = datetime.datetime.strptime(gv2.split(".")[0], "%Y-%m-%dT%H:%M:%S")
        date = date + datetime.timedelta(seconds=delta)
        gv2 = date.strftime("%Y-%m-%dT%H:%M:%S.000")

def create_command_line_options():

    '''Create all the command line options for this application

    Returns
    --------
    An optparse.OptionParser object for parsing the command line arguments fed to this application'''

    usageText = '''
    python regression_test_harness.py [-t <TESTNUMS> -n <TESTNAMES> -b -g <GITBRANCH>]

    To run all tests for the branch:

    python regression_test_harness.py

    To run test numbers 1,2,11,5-9:

    python regression_test_harness.py -t 1,2,11,5-9

    To create baselines of all tests for the branch:

    python regression_test_harness.py -b

    To run tests with names "test1" and "test2"

    python regression_test_harness.py -n test1,test2

    After generating the baselines you will need to copy them from testBaselineDir
    into the desired folder for that branch, ie workspacesBaselineDir, if you like the results.
    This is because when running this script outside of jenkins, the script will output to testBaselineDir.
    Alternatively, change the branch name used using the -g command line arg when creating the baselines,
    to output to the correct baseline folder.

    When all tests are ran, it runs all the tests mapped to the current branch, which is specified in
    the tests table.  The current branch is determined via the GIT_BRANCH environment variable, or the
    -g command line argument.

    The -t option can take test numbers in any order, and it will run them in the order specified.
    Similarly for -n option.
    '''

    versionText = 'Version 1 (9_16_2014)'

    parser = optparse.OptionParser(usage=usageText,version=versionText)

    parser.add_option("-t","--testNums",action="callback",type="string",metavar="TESTNUMS",callback=parse_test_nums,
                      help='''Specify the test numbers to run or create baselines for, ie "1,2,5-9,11".  Can only supply this if not supplying -n also.  (Optional)''')
    parser.add_option("-n","--testNames",action="callback",type="string",metavar="TESTNAMES",callback=parse_test_names,
                      help='''Specify the test names to run or create baselines for, ie "test1,test2".  Can only supply this if not supplying -t also.  (Optional)''')
    parser.add_option("-b","--createBaselines",action="store_true",dest="create_baselines",
                      help='''Supply this option if you want to create the baseline files for the tests (Optional)''')
    parser.add_option("-g","--gitBranch",action="callback",type="string",metavar="GITBRANCH",callback=parse_git_branch,
                      help='''Specify the branch to use, otherwise uses the value of $GIT_BRANCH, and if that env variable is not defined uses 'test'. (Optional)''')
    parser.add_option("-v","--evaluate",action="store_true",dest="evaluate_only",
                      help='''Do not execute the tests. Just evaluate the existing output. (Optional)''')

    return parser

def parse_command_line():
    '''Parse the command line options given to this application'''

    global test_nums, create_baselines, evaluate_only, cmd_git_branch, test_names

    parser = create_command_line_options()

    parser.test_nums = None
    parser.cmd_git_branch = None
    parser.test_names = None

    (_options,_args) = parser.parse_args()

    test_nums = parser.test_nums
    test_names = parser.test_names
    create_baselines = _options.create_baselines
    evaluate_only = _options.evaluate_only
    cmd_git_branch = parser.cmd_git_branch

    if test_nums and test_names:
        print "ERROR: Cannot supply both the -t and -n options!  Please remove one of them."
        sys.exit(1)

def parse_git_branch(option, opt, value, parser):
    '''
    Parses the GIT_BRANCH command line arg
    '''

    if value is not None:
        parser.cmd_git_branch = value.strip()

def parse_test_nums(option, opt, value, parser):
    '''
    Parses out the section numbers ran from the passed string and creates
    a list of the corresponding section numbers, ie "1,3-5,7" will
    create [1,3,4,5,7].  Assigns this to list to parser.test_nums
    '''

    def parse_range(str):

        myList = []
        keyListBounds = str.split('-')
        bound1 = float(keyListBounds[0])
        bound2 = float(keyListBounds[1])

        mult = 1 if bound2 >= bound1 else -1
        upperBound = (bound2 if mult == 1 else bound1)
        lowerBound = (bound1 if mult == 1 else bound2)

        #testArray = [].extend(tests)#tests if mult == 1 else tests.reverse()
        #for test in testArray:
        for i in range(0,len(tests)):
            j = i if mult == 1 else len(tests) - i - 1
            test = tests[j]
            testNum = test[0]
            if testNum >= lowerBound and testNum <= upperBound:
                myList.append(testNum)
#         for key in range(bound1,bound2+mult,mult):
#             myList.append(key)

        return myList

    keyList = []

    if value is not None:
        value = value.strip()

        # value is comma separated ie 1,3-5,7:
        if value.find(',') != -1:
            keyListArray = value.split(',')

            for keyStr in keyListArray:

                # testKey is a range ie 1-5:
                if keyStr.find('-') != -1:
                    keyList += parse_range(keyStr)

                # It was a single key:
                else:
                    keyList.append(float(keyStr))

        # value is just a range ie 1-3:
        elif value.find('-') != -1:
            keyList += parse_range(value)

        #value was just a single key:
        else:
            keyList = [float(value)]

    parser.test_nums = keyList

def parse_test_names(option, opt, value, parser):
    '''
    Parses out the test names from the command line arg.  Assigns this to list to parser.test_names
    '''

    keyList = []

    if value is not None:
        value = value.strip()

        # value is comma separated:
        if value.find(',') != -1:
            keyList = value.split(',')

        #value was just a single key:
        else:
            keyList = [value]

    parser.test_names = keyList

def thick_divider():
    print "\n"+"="*display_width+"\n"

def thin_divider():
    print "-"*display_width

def print_pass(msg):
    global passed_tests
    passed_tests += 1
    print "\nPASS: "+str(msg)
    print "\n# passed: "+ str(passed_tests) + ",  # failed: " + str(failed_tests)

def print_error(msg, outpt):
    global failed_tests
    failed_tests += 1
    errs.append(msg)
    print "\nFAIL: "+str(msg)
    print str(outpt)
    print "\n# passed: "+ str(passed_tests) + ",  # failed: " + str(failed_tests)

def mbee_util_jar_path():
    path = "../../../../.m2/repository/gov/nasa/jpl/mbee/util/mbee_util/"
    pathList = glob.glob(path+"*SNAPSHOT/*SNAPSHOT.jar")
    if pathList:
        return pathList[0]
    else:
        return path+"0.0.16/mbee_util-0.0.16.jar"

def mbee_util_jar_path2():
    path = "../../src/main/amp/web/WEB-INF/lib/mbee-util.jar"
    return path

def mbee_util_jar_path3():
    path = "../../../util/mbee_util.jar"
    return path

def mbee_util_jar_path4():
    path = "../../target/mms-repo-war/WEB-INF/lib/mbee_util.jar"
    return path

def run_curl_test(test_num, test_name, test_desc, curl_cmd, use_json_diff=False, filters=None,
                  setupFcn=None, postProcessFcn=None, teardownFcn=None, delay=None):
    '''
    Runs the curl test and diffs against the baseline if create_baselines is false, otherwise
    runs the curl command and creates the baseline .json file.

    test_num: The unique test number for this test
    test_name: The name of the test
    test_desc: The test description
    curl_cmd: The curl command to send
    use_json_diff: Set to True to use a JsonDiff when comparing to the baseline
    filters: A list of strings that should be removed from the post output, ie ['"modified"']
    delay: Delay time in seconds before running the test
    '''

    global orig_output
    global filtered_output
    global filter_output
    global orig_json
    global filtered_json
    global gv1, gv2, gv3, gv4, gv5, gv6, gv7

    result_json = "%s/%s.json"%(result_dir,test_name)
    result_sorted_json = "%s/sorted_%s.json"%(result_dir,test_name)
    result_orig_json = "%s/%s_orig.json"%(result_dir,test_name)
    baseline_json = "%s/%s.json"%(baseline_dir,test_name)
    baseline_sorted_json = "%s/sorted_%s.json"%(baseline_dir,test_name)
    baseline_orig_json = "%s/%s_orig.json"%(baseline_dir,test_name)

    thick_divider()
    if create_baselines and not evaluate_only:
        print "CREATING BASELINE FOR TEST %s (%s)"%(test_num, test_name)
        orig_json = baseline_orig_json
        filtered_json = baseline_json
    else:
        print "TEST %s (%s) %s"%(test_num, test_name, datetime.datetime.now())
        orig_json = result_orig_json
        filtered_json = result_json

    if delay and not evaluate_only:
        print "Delaying %s seconds before running the test"%delay
        time.sleep(delay)

    print "TEST DESCRIPTION: "+test_desc

    if setupFcn: #and not evaluate_only:
        print "calling setup function"
        setupFcn()

    #replace gv variable references in curl command
    curl_cmd = str(curl_cmd).replace("$gv1", str(gv1))
    curl_cmd = str(curl_cmd).replace("$gv2", str(gv2))
    curl_cmd = str(curl_cmd).replace("$gv3", str(gv3))
    curl_cmd = str(curl_cmd).replace("$gv4", str(gv4))
    curl_cmd = str(curl_cmd).replace("$gv5", str(gv5))
    curl_cmd = str(curl_cmd).replace("$gv6", str(gv6))
    curl_cmd = str(curl_cmd).replace("$gv7", str(gv7))

    foo=""
    if (evaluate_only):
        foo="Not "
    print foo + "Executing curl cmd: \n"+str(curl_cmd)

    status = 0
    if (not evaluate_only):
        (status,output) = commands.getstatusoutput(curl_cmd+"> "+orig_json)

    if status == 0:

        file_orig = open(orig_json, "r")

        # Apply filters to output of curl cmd (not using output b/c getstatusoutput pipes stderr to stdout):
        orig_output = ""
        filter_output = ""
        if filters:
            for line in file_orig:
                filterFnd = False
                for filter in filters:
                    # If the contains the filter:
                    if re.search(filter,line):
                        filterFnd = True
                        break

                # Add line if it does not contain the filter:
                if not filterFnd:
                    filter_output += (line)

                # Always add lines to orig_output
                orig_output += (line)
        else:
            stuffRead = file_orig.read()
            filter_output = stuffRead
            orig_output = stuffRead

        if postProcessFcn: #and not evaluate_only:
            print "calling post-process function"
            postProcessFcn()
            #print("filter_output after post process = " + str(filter_output))

        # Write to result .json file:
        file = open(filtered_json, "w")
        file.write(filter_output)
        file.close()
        file_orig.close()

        if teardownFcn: #and not evaluate_only:
            print "calling teardown function"
            teardownFcn()

        if create_baselines and not evaluate_only:

            print "Filtered output of curl command:\n"+filter_output

        else:
            # Perform diff:
            if use_json_diff:
                # pull from the maven created classpath, if not available, revert to hardcoded search
                #cmd = 'grep "classes \-classpath" ../../runserver.log | cut -d " " -f5'
                #cmd = 'grep "classes \-classpath" ../../runserver.log | sed -e "s/.*classes \-classpath \\(.*\\) \-sourcepath.*$/\\1/"'
                #print cmd
                #p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                #cp, err = p.communicate()
                #if len(cp) == 0:
                #cp = ".:%s:%s:%s:%s:../../target/mms-amp/lib/json-20140107.jar:../../target/mms-repo-ent-war/WEB-INF/lib/json-20140107.jar:../../target/mms-repo-war/WEB-INF/lib/json-20140107.jar:../../target/mms-repo-war/WEB-INF/lib/json-20090211.jar:../../target/mms-amp/lib/mbee_util-2.2.0-SNAPSHOT.jar:../../target/classes"%(mbee_util_jar_path(),mbee_util_jar_path2(),mbee_util_jar_path3(),mbee_util_jar_path4())
                #diff_cmd = 'java -cp "%s" gov.nasa.jpl.view_repo.util.JsonDiff' % cp.strip()

                #commands.getoutput("head -n -1 " + baseline_json + " | jq --sort-keys '[.elements[] | select(.name != null)] | sort_by(.name, .sysmlid)' > " + baseline_sorted_json)
                #commands.getoutput("head -n -1 " + result_json + " | jq --sort-keys '[.elements[]] | select(.name != null)] | sort_by(.name, .sysmlid)' > " + result_sorted_json)
                #json1 = commands.getoutput("head -n -1 " + baseline_json + " | jq --sort-keys '.' > " + baseline_sorted_json)
                #json2 = commands.getoutput("head -n -1 " + result_json + " | jq --sort-keys '.' > " + result_sorted_json)
                diff_cmd = "diff"
            else:
                diff_cmd = "diff"

            print "baseline: " + baseline_json
            commands.getoutput("head -n -1 " + baseline_json + " | jq --sort-keys '[.elements[]?,.sites[]?,.views[]?,.ownersNotFound[]?,.status? | select(.sysmlid != null)] | sort_by(.sysmlid)' > " + baseline_sorted_json)
            commands.getoutput("head -n -1 " + result_json + " | jq --sort-keys '[.elements[]?,.sites[]?,.views[]?,.ownersNotFound[]?,.status? | select(.sysmlid != null)] | sort_by(.sysmlid)' > " + result_sorted_json)

            diff_cmd2 = "%s %s %s"%(diff_cmd,baseline_sorted_json,result_sorted_json)
            #print diff_cmd2
            (status_diff,output_diff) = commands.getstatusoutput(diff_cmd2)

            if output_diff:
                print_error("Test number %s (%s) failed!"%(test_num,test_name), "  Diff returned bad status or diffs found in the filtered .json files (%s,%s), status: %s, output: \n'%s'"%(baseline_json,result_json,status_diff,output_diff))
            else:
                print_pass("Test number %s (%s) passed!  No differences in the filtered .json files (%s,%s)"%(test_num,test_name,baseline_json,result_json))
    else:
        print_error("Test number %s (%s) failed!"%(test_num,test_name), "Curl command return a bad status and output doesnt start with json: %s, output: '%s'"%(status,output))

    thick_divider()

def run_test(test):
    '''
    Runs the curl test specified the passed test list
    '''

    run_curl_test(test_num=test[0],test_name=test[1],
                  test_desc=test[2],curl_cmd=test[3],
                  use_json_diff=test[4],filters=test[5],
                  setupFcn=test[7] if (len(test) > 7) else None,
                  postProcessFcn=test[8] if (len(test) > 8) else None,
                  teardownFcn=test[9] if (len(test) > 9) else None,
                  delay=test[10] if (len(test) > 10) else None)


def create_curl_cmd(type, data="", base_url=BASE_URL_WS, post_type="elements", branch="master/",
                    project_post=False):
    '''
    Helper method to create curl commands.  Returns the curl cmd (string).

    type: POST, GET, DELETE
    data: Data to post in JsonData ie elementsNew.json, or the key/value pair when making a project ie "'{"name":"JW_TEST"}'",
          or the data to get ie views/301 or data to delete ie workspaces/master/elements/771
    base_url:  What base url to use, ie %s
    post_type: "elements", "views", "products"
    branch: The workspace branch, ie "master/", or the project/site to use to ie "sites/europa/projects/123456/"
    project_post: Set to True if creating a project
    post_no_data: Set to True if posting with no data
    '''%BASE_URL_WS

    cmd = ""

    if type == "POST":
        post_flags = CURL_POST_FLAGS
        if data and data[-2:] == ".k":
            post_flags = CURL_POST_FLAGS_K
        if project_post:
            cmd = 'curl %s %s %s "%s%s"'%(CURL_FLAGS, post_flags, data, base_url, branch)
        elif data:
            cmd = 'curl %s %s @JsonData/%s "%s%s%s"'%(CURL_FLAGS, post_flags, data, base_url, branch, post_type)
        else:
            cmd = 'curl %s %s "%s%s%s"'%(CURL_FLAGS, CURL_POST_FLAGS_NO_DATA, base_url, branch, post_type)

    elif type == "GET":
        cmd = 'curl %s %s "%s%s%s"'%(CURL_FLAGS, CURL_GET_FLAGS, base_url, branch, data)

    elif type == "DELETE":
        cmd = 'curl %s %s "%s%s%s"'%(CURL_FLAGS, CURL_DELETE_FLAGS, base_url, branch, data)

    return cmd

def kill_server():
    (status,output) = commands.getstatusoutput("pkill -fn 'integration-test'")


def startup_server():

    print "KILLING SERVER IF ONE IS RUNNING"
    kill_server()
    time.sleep(1)
    print "STARTING UP SERVER"
    #subprocess.call("./runserver_regression.sh")
    # Is this inheriting the correct environment variables?
    #p = subprocess.Popen("./runserver_regression.sh", shell=True) # still not working, eventually hangs and server doesn't come up
    #commands.getstatusoutput("./runserver_regression.sh")
    #time.sleep(30)

    '''fnd_line = wait_on_server()

    if fnd_line:
        print "SERVER CONNECTED"
    else:
        print "ERROR: SERVER TIME-OUT"
        kill_server()
        sys.exit(1)
    '''


def wait_on_server(filename="runserver.log", lineToCheck="Starting ProtocolHandler"):
    print "POLLING SERVER"
    server_log = open(filename,"r")
    seek = 0
    fnd_line = False
    for timeout in range(0,600):
        server_log.seek(seek)
        for line in server_log:
            if lineToCheck in line:
                fnd_line = True
                break

        if fnd_line:
            break

        seek = server_log.tell()
        time.sleep(1)

        if timeout%10 == 0:
            print ".."

    return fnd_line


def set_curl_user(user):

    global CURL_USER, CURL_FLAGS
    CURL_USER = " -u " + user
    CURL_FLAGS = CURL_STATUS+CURL_USER


def run(testArray):
    '''
    Main function to run the regression harness
    '''

    global result_dir, baseline_dir, tests

    tests = testArray

    # Parse the command line arguments:
    parse_command_line()

    # this is not working yet, so assumption for now is that this will be called
    # by the bash script which will start up the server
    #startup_server()

    # Change directories to where we are used to sending curl cmds:
    if not os.path.exists(test_dir_path):
        print "ERROR: Test directory path '%s' does not exists!\n"%test_dir_path
        sys.exit(1)

    os.chdir(test_dir_path)

    # Determine the branch to use based on the command line arg if supplied, otherwise
    # use the environment variable:
    if cmd_git_branch:
        git_branch = cmd_git_branch
    else:
        git_branch = os.getenv("GIT_BRANCH", "test")

    # Make the directories if needed:
    if "/" in git_branch:
        git_branch = git_branch.split("/")[1]

    result_dir = "%sResultDir"%git_branch
    baseline_dir = "%sBaselineDir"%git_branch

    if not os.path.exists(result_dir):
        os.makedirs(result_dir)

    if not os.path.exists(baseline_dir):
        os.makedirs(baseline_dir)

    print "\nUSING BASELINE DIR: '%s'\nOUTPUT DIR: '%s'\n"%(baseline_dir, result_dir)

    # Run tests or create baselines:
    # If there were test numbers specified:
    if test_nums:
        for test_num in test_nums:
            # If it is a valid test number then run the test:
            for test in tests:
                if test_num == test[0]: # TODO --  and git_branch in test[6]:
                    run_test(test)

    # If there were test names specified:
    elif test_names:
        for test_name in test_names:
                # If it is a valid test number then run the test:
                for test in tests:
                    if test_name == test[1]:
                        run_test(test)

    # Otherwise, run all the tests for the branch:
    else:
        for test in tests:
            if git_branch in test[6]:
                run_test(test)

    # uncomment once startup_server() works
#     print "KILLING SERVER"
#     kill_server()

    if not create_baselines:
        print "\nNUMBER OF PASSED TESTS: "+str(passed_tests)
        print "NUMBER OF FAILED TESTS: "+str(failed_tests)+"\n"
        if failed_tests > 0:
            print "FAILED TESTS:"
            for item in errs[:]:
                print item
            print "\n"

    sys.exit(failed_tests)

# This will attempt to turn a flag -ON- the MMS server
def turn_on_mms_flag(mmsFlag, VERBOSE=True):
    curl_cmd = create_curl_cmd("GET", data="flags/" + mmsFlag + "?on", base_url=SERVICE_URL, branch="")
    result = commands.getoutput(curl_cmd)
    if VERBOSE : print result

# This will attempt to turn a flag -OFF- the MMS server
def turn_off_mms_flag(mmsFlag, VERBOSE=True):
    curl_cmd = create_curl_cmd("GET", data="flags/" + mmsFlag + "?off", base_url=SERVICE_URL, branch="")
    result = commands.getoutput(curl_cmd)
    if VERBOSE : print result

# Retrieves the status of a flag on the MMS server, and will return a boolean value if it is on (true) or off ( false)
def get_mms_flag_status(mmsFlag, VERBOSE=True):
    curl_cmd = create_curl_cmd("GET", data="flags/" + mmsFlag + "?ison", base_url=SERVICE_URL, branch="")
    output = commands.getoutput(curl_cmd)

    # Regular Expression to check for the patter is off, or is on, Python compiles then searches for it
    # The variables will contain None if the pattern is not found in the string.
    off = re.compile('is off')
    on = re.compile('is on')
    isOff = off.search(output)
    isOn = on.search(output)

    # Compares the values of isOff and isOn against None to see whether or not they are in the string
    # Technically if one is true the other must be false so it could be an if else, but I figure it is safer
    # to check both when dealing with memory locations
    if isOn is not None:
        if VERBOSE : print "The MMS flag : " + mmsFlag + " is on, RETURN : True"
        return True
    if isOff is not None:
        if VERBOSE : print "The MMS flag : " + mmsFlag + " is off, RETURN : False"
        return False

# Toggles a flag on the MMS server on or off, based on its current status
def toggle_mms_flag(mmsFlag, VERBOSE=True):

    # Gets the current status of the mms flag
    flag_status = get_mms_flag_status(mmsFlag)
    # if flag is currently on, turn off flag
    if flag_status is True:
        toggleParameter = "?off"
        if VERBOSE : print "The MMS flag : " + mmsFlag + " is turning off.."
    # else if off, turn on
    elif flag_status is False:
        toggleParameter = "?on"
        if VERBOSE : print "The MMS flag :" + mmsFlag + " is turning on.."

    # Create curl command with either ?on or ?off then execute curl command
    curl_cmd = create_curl_cmd("GET", data="flags/" + mmsFlag + toggleParameter, base_url=SERVICE_URL, branch="")
    output = commands.getoutput(curl_cmd)
    if VERBOSE : print str(output)

# Turns on the MMS Service Flag Testing for checking MMS Versions
def turn_on_check_mms_version_flag():
    isOn = get_mms_flag_status("checkMmsVersion")
    if not isOn:
       turn_on_mms_flag("checkMmsVersion")


def turn_off_check_mms_version_flag():
    isOn = get_mms_flag_status("checkMmsVersion")
    if isOn:
       turn_off_mms_flag("checkMmsVersion")


def turn_on_service_flags():
    for flag in service_flags:
        print "Turning on : " + flag
        turn_on_mms_flag(flag)

def turn_off_service_flags():
    for flag in service_flags:
        print "Turning off : " + flag
        turn_off_mms_flag(flag)
