#!/usr/bin/env python

# MMS Test Library
import regression_test_harness, subprocess

# from docutils.nodes import option_string

OUTPUT_FILENAME = "regression_test_suite.robot"


def set_test_suite_settings(file_action):
    # Add the settings for the test case
    # Setting Headers
    with open(OUTPUT_FILENAME, file_action) as file_object:
        file_object.write("*** Settings ***\n")

        # Libraries to be added
        # TODO: Dynamically adding Libraries to be used
        file_object.write("Library\t\t" + "OperatingSystem\n")
        file_object.write("Library\t\t" + "requests\n")
        file_object.write("Library\t\t" + "Collections\n")
        file_object.write("Library\t\t" + "json\n")
        file_object.write(
            "Library\t\t" + "${CURDIR}/../robot_lib.py\n")
        file_object.write("\n")


def set_test_suite_variables(file_action):
    with open(OUTPUT_FILENAME, file_action) as file_object:
        # Variable Declarations
        file_object.write("*** Variables ***\n")
        file_object.writelines("${AUTH}\t\t\tadmin:admin\n")
        file_object.writelines("${SERVER}\t\tlocalhost:8080\n")
        file_object.writelines("${ROOT}\t\t\thttp://${AUTH}@${SERVER}/alfresco/service\n")
        file_object.writelines("#Notice the & instead of $, it represents a dictionary object\n")
        file_object.writelines("&{REQ_HEADER}\t\tcontent-type=application/json\n")
        file_object.write("\n")


def set_test_suite_keywords(file_action):
    with open(OUTPUT_FILENAME, file_action) as file_object:
        file_object.write("*** Keywords ***\n")

        file_object.write("Should Match Baseline\n")
        file_object.writelines("\t[Arguments]\t\t\t${result}\n")
        file_object.writelines("\tShould Be True\t\t${result}\n\n")

        file_object.write("Generate JSON\n")
        file_object.writelines("\t[Arguments]\t\t\t${test_name}\t\t${json}\t\t${filter_list}\n")
        file_object.writelines("\twrite json to file\t\t\t${json}\t\t${test_name}\n")
        file_object.writelines("\tfilter json file\t\t\t${test_name}\t\t${filter_list}\n")
        file_object.writelines("\tReturn From Keyword\t\t\t${json}\n\n")


def generate_full_robot_test_suite(file_action):
    with open(OUTPUT_FILENAME, file_action) as file_object:
        generated_keywords = []
        # Test Case Header
        file_object.write("*** Test Cases ***\n")

        # For each test within tests generate robot formatted test case output
        for test in regression_test_harness.tests:
            # Write the name of the test
            file_object.write(test[1] + "\n")

            test_spec_length = len(test)

            # Documentation (Description)
            file_object.write(
                "\t[Documentation]\t\t" + "\"Regression Test: " + str(test[0]) + ". " + str(test[2]) + "\"\n")

            file_object.write(
                "\t[Tags]\t\t\t\t" + str(test[0]) + "\n")

            curl_cmd = test[3]
            # print(curl_cmd.split(' '))
            using_json_data = False
            call_type = ""
            if "GET" in curl_cmd:
                call_type = "Get"
            elif "POST" in curl_cmd:
                call_type = "Post"
                if '.json' in curl_cmd:
                    post_data = curl_cmd.split('.json')[0]
                    if "@JsonData" in post_data:
                        post_data = post_data.split('@JsonData')[1]
                    else:
                        post_data = post_data.split('--data')[1]
                    using_json_data = True
                    post_data = post_data.replace('/', '') + ".json"
                    print(post_data)
                    file_object.writelines(
                        "\t${post_json} =\t\tGet File\t ../JsonData/" + post_data.replace(' ', '') + "\n")
            elif "DELETE" in curl_cmd:
                call_type = "Delete"

            # print(curl_cmd)
            api_url = curl_cmd.split('alfresco/service/')[1].split(' ')[0].split('"')[0]
            # print(api_url)
            if call_type == "Post":
                if using_json_data:
                    file_object.writelines("\t${result} =\t\t\t" + call_type + "\t\turl=${ROOT}/" + api_url)
                    file_object.writelines("\t\tdata=${post_json}\t\theaders=&{REQ_HEADER}")
                else:
                    file_object.writelines("\t${result} =\t\t\t" + call_type + "\t\t${ROOT}/" + api_url)

            else:
                file_object.writelines("\t${result} =\t\t\t" + call_type + "\t\turl=${ROOT}/" + api_url + "")

            file_object.writelines("\n")
            file_object.writelines("\tShould Be Equal\t\t${result.status_code}\t\t${200}\n")
            # file_object.writelines("\t${json} =\t\t\tSet Variable\t${result.json()}\n")

            stripped_filters = []
            filter_string = ""

            if test[5] is not None:
                for sfilter in test[5]:
                    stripped_filters.append(str(sfilter).replace('"', ''))

                # print("Filters {}".format(stripped_filters))
                filter_string = str(stripped_filters).replace("[", "").replace("]", "").replace("'", "").replace(',',
                                                                                                                 '\t\t')
            file_object.writelines("\t${filter} =\t\t\tCreate List\t\t" + filter_string + "\n")
            file_object.writelines("\tGenerate JSON\t\t\t${TEST_NAME}\t\t${result.json()}\t\t${filter}\n")

            file_object.write('\tSleep\t\t\t\t.5s\n')
            file_object.writelines(
                "\t${compare_result} =\t\tCompare JSON\t\t${TEST_NAME}\n")
            file_object.writelines("\tShould Match Baseline\t\t${compare_result}")

            file_object.write("\n")
            file_object.write("\n")

            # file_object.write("Regression\t\t" + str(test[0]) + "\n")


def set_full_regression_suite():
    with open(OUTPUT_FILENAME, 'a') as file_object:
        file_object.write("\nRegression Suite\n")
        file_object.write(
            "\t[Documentation]\t\t" + "This will execute the entire regression suite to generate the output required for the following tests to compare.\n")
        file_object.write("\t${output}= \t\tOperatingSystem.Run\t\t" + "./regress.sh -g develop\n")
        file_object.write("\tlog\t\t ${output}\n")
        file_object.write("\tlog to console\t\t ${output}\n")
        file_object.write("\tShould Not Contain\t\t${output}\t\tFAIL:\n")
        file_object.write("\tShould Contain\t\t${output}\t\tPASS:\n")


def generate_test_case(test_num=None, test_name=None):
    with open(OUTPUT_FILENAME, 'a') as file_object:
        for test in regression_test_harness.tests:
            if test[0] == test_num or test[1] == test_name:
                # Documentation (Description)
                file_object.write(test[1])
                file_object.write(
                    "\t[Documentation]\t\t" + "\"Regression Test: " + str(test[0]) + ". " + str(test[2]) + "\"\n")
                file_object.write(
                    "\t${output}= \t\tOperatingSystem.Run\t\t" + "./regress.sh -g develop -t " + test[0] + "\n")
                file_object.write("\tlog\t\t ${output}\n")
                file_object.write("\tlog to console\t\t ${output}\n")
                file_object.write("\tShould Not Contain\t\t${output}\t\tFAIL:\n")
                file_object.write("\tShould Contain\t\t${output}\t\tPASS:\n")


def generate_tests(test_names=None, test_numbers=None):
    if test_names is not None:
        for test in test_names:
            generate_test_case(test_name=test[1])
    if test_numbers is not None:
        for test in test_numbers:
            generate_test_case(test_num=test[0])


if __name__ == '__main__':
    # Automatically generate the Robot Framework Test Suite for the MMS when executing the python file.
    set_test_suite_settings('w')
    set_test_suite_variables('a')
    generate_full_robot_test_suite('a')
    set_test_suite_keywords('a')
