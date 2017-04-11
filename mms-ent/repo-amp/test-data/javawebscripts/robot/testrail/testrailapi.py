#!/usr/bin/env python
'''
TestRail API
To expand the capabilities of the API, create a parser then add necessary functions or logic.
Author: Dan Karlsson
'''
import os
import json
import libs.testrail as testrail
import robottestparser
import awsreportparser

os.chdir("../")
project_dir = os.getcwd()
src_dir = project_dir + "/src"
api_keys_dir = project_dir + "/api-keys"
testrail_api_key = "s2IRLTC8I9huNJxkr/Xz-fkXkS5dC9FFaQLCSlkrC"
# Jenkinstestrail Production App Acount API Key
testrail_production_api_key = "ji/pRl4EQoJZkWrRe/th-4tSGV4ASllnIWEqB4SFE"


class TestRailAPI:
    def __init__(self, server_name=None, user=None, password=None, production=True):
        # Configure the TestRail client
        self.test_suite = None
        self.use_junit_test_output = False
        self.use_aws_report_output = False
        self.test_sections = []
        self.test_section_map = {}
        if server_name is None and production is False:
            self.client = testrail.APIClient('https://cae-testrail-uat.jpl.nasa.gov/testrail/')
        elif production is True:
            self.client = testrail.APIClient('https://cae-testrail.jpl.nasa.gov/testrail/')
            password = testrail_production_api_key
        else:
            self.client = testrail.APIClient(str(server_name))

        if user is None:
            self.client.user = 'jenkinstestrail@jpl.nasa.gov'
        else:
            self.client.user = str(user)
        if password is None:
            self.client.password = testrail_api_key
        else:
            self.client.password = password

    def load_robot_tests(self, xml_file=None):
        # This will need to be modified so it searches for the robot test results.
        if xml_file is None:
            self.test_suite = robottestparser.RobotXML('output.xml')
        else:
            os.chdir(str(xml_file).replace('output.xml', ''))
            self.test_suite = robottestparser.RobotXML('output.xml')
        self.test_sections = self.test_suite.sections
        return self.test_suite

    def load_aws_reports(self, aws_file=None):
        self.use_aws_report_output = True
        if aws_file is None:
            self.test_suite = awsreportparser.AWSReportJSON('aws.json')
        else:
            os.chdir(str(aws_file).replace('aws.json', ''))
            self.test_suite = awsreportparser.KarmaJUnitXML('aws.json')
        self.test_sections = self.test_suite.sections
        return self.test_suite

    # Test Suite Methods
    def get_test_suites(self, project_id):
        return self.client.send_get("get_suites/{0}".format(project_id))

    def get_test_suites_ids(self, project_id):
        ids = []
        suites = self.get_test_suites(project_id)
        for suite in suites:
            ids.append(suite["id"])
        return ids

    # Test Case Methods
    def get_test_cases(self, project_id, test_suite_id, test_section_id):
        return self.client.send_get(
            "get_cases/{0}&suite_id={1}&section_id={2}".format(project_id, test_suite_id, test_section_id))

    def get_test_cases_titles(self, project_id, suite_id, section_id):
        results = self.get_test_cases(project_id, suite_id, section_id)
        titles = []
        for test in results:
            titles.append(test["title"])
        return titles

    def get_all_project_test_cases_ids(self, project_id):
        test_cases = self.get_all_project_test_cases(project_id)
        # print(test_cases)
        ids = []
        for test_case in test_cases:
            for test in test_case:
                ids.append(test["id"])

        return ids

    def get_all_project_test_cases(self, project_id):
        """
        Method finds all test cases from a project and returns a JSON object of each test case.
        :param project_id:
        :return:
        """
        suite_ids = self.get_test_suites_ids(project_id)
        test_cases = []

        for suite_id in suite_ids:
            section_ids = self.get_section_ids(project_id, suite_id)
            for id in section_ids:
                test_cases.append(self.get_test_cases(project_id, suite_id, id))
        return test_cases

    def get_all_project_test_cases_titles(self, project_id):
        """
        Method finds all test cases that a project contains and returns the titles of these test cases.
        :param project_id:
        :return:
        """
        test_cases = self.get_all_project_test_cases(project_id)
        # print(test_cases)
        titles = []
        for test_case in test_cases:
            for test in test_case:
                titles.append(str(test["title"]))

        return titles

    # Test Suite Section Methods
    def get_sections(self, project_id, suite_id):
        return self.client.send_get("get_sections/{0}&suite_id={1}".format(project_id, suite_id))

    def get_section_ids(self, project_id, suite_id):
        sections = self.get_sections(project_id, suite_id)
        section_ids = []
        for section in sections:
            section_ids.append(section["id"])
        return section_ids

    def compare_project_to_robot_tests(self, project_id, suite_id, section_id, robot_xml_file=None):
        """
        This function will compare a project to a robot output.xml file and see what tests exist underneath a project
        :param project_id:
        :param suite_id:
        :param section_id:
        :param robot_xml_file:
        :return:
        """
        self.load_robot_tests(robot_xml_file)
        missing_test_cases = []
        project_tests = self.get_test_cases_titles(project_id, suite_id, section_id)
        for test in self.test_suite.tests:
            if test.name not in project_tests:
                missing_test_cases.append(test.name)

        return missing_test_cases

    def compare_project_to_tests(self, project_id, suite_id, section_id=None):  # , junit_xml_file=None):
        """
        This function will compare a project to a robot output.xml file and see what tests exist underneath a project
        :param project_id:
        :param suite_id:
        :param section_id:
        :param junit_xml_file:
        :return:
        """
        # self.load_junit_tests(junit_xml_file)
        missing_test_cases = []
        project_tests = []
        if section_id is not None:
            project_tests = self.get_test_cases_titles(project_id, suite_id, section_id)
            for test in self.test_suite.tests:
                if test.name not in project_tests:
                    missing_test_cases.append(test.name)
        else:
            for map_section_id in self.test_section_map.values():
                test_case_titles = self.get_test_cases_titles(project_id, suite_id, map_section_id)
                # print(test_case_titles)
                for test in test_case_titles:
                    if test not in project_tests:
                        project_tests.append(test.rstrip())
            project_tests.sort()
            for test in self.test_suite.tests:
                if test.name not in project_tests:
                    missing_test_cases.append(test.name)

        missing_test_cases.sort()
        print("")
        print("Missing Test Cases length {}".format(len(missing_test_cases)))
        print("Missing tests {}".format(missing_test_cases))
        return missing_test_cases

    def add_new_case(self, section_id, case):
        result = self.client.send_post(
            'add_case/{0}'.format(section_id),
            {"title": case}
        )
        return result

    def update_test_cases(self, project_id, suite_id, section_id=None):
        """
        Method will first compare the parsed test output xml file, then given the project, suite and section id it will
        compare tests the testrail server has and the tests the new output xml file has.
        :param project_id: ID of the project
        :param suite_id:  ID of the suite
        :param section_id: ID of the section
        :return:
        """
        results = self.compare_project_to_tests(project_id, suite_id, section_id)

        for case in results:
            if section_id is None:
                # print(self.test_suite.tests)
                section_name = self.test_suite.get_test_section_name(case)
                new_id = self.test_section_map[section_name]
                print(self.add_new_case(new_id, str(case)))
            else:
                print(self.add_new_case(section_id, str(case)))

    def add_test_run(self, project_id, suite_id, test_run_name, include_all_tests=True, description=None,
                     case_ids=None):
        """
        Method will add a test run to a test suite.
        :param project_id: Project ID, Required
        :param test_run_name: Name of the test run to add
        :param include_all_tests: Boolean value that determines whether or not to use all test cases in the test suite
        :param description: Description of the test run [Optional]
        :param suite_id: Suite ID which to add the test run to [Optional]
        :param case_ids: If Include All Tests is False, the IDs of which test cases to add into the test run is required
        :return: Results of the post
        """
        test_run = {
            "name": str(test_run_name),
            "suite_id": suite_id,
            "include_all": include_all_tests,
        }
        if include_all_tests is False:
            if case_ids is None:
                print("An array of case IDs are required ")
                return
            else:
                test_run["case_ids"] = case_ids

        if description is not None:
            test_run["description"] = description

        print(test_run)
        result = self.client.send_post(
            'add_run/{0}'.format(project_id),
            test_run
        )
        print(result)
        return result

    # ID Functions
    def get_id_by_name(self, project_id, suite_id, section_id, name):
        """
        Gets the names of the tests within a project then checks to see if the name of the test is in the project.
        If it finds the test it will return the ID of the test
        :param project_id:
        :param suite_id:
        :param section_id:
        :param name:
        :return: returns the ID
        """
        project_tests = self.get_test_cases(project_id, suite_id, section_id)
        test_ids = []
        for test in project_tests:
            if test["title"] in name:
                return test["id"]

    # Test Run Methods
    def get_project_test_runs(self, project_id):
        """
        Method makes a call to the API to retrieve the Test Runs on the server
        :param project_id:
        :return: Returns a Test Run Object
        """
        return self.client.send_get('get_runs/{0}'.format(project_id))

    # Result Methods
    def add_result(self, test_result):
        result_json = {
            "comment": test_result.description
        }
        if "PASS" in str(test_result.status) or "1" == str(test_result.status):
            result_json["status_id"] = "1"
        elif "FAIL" in str(test_result.status) or "5" == str(test_result.status):
            result_json["status_id"] = "5"
        elif "4" == str(test_result.status):
            result_json["status_id"] = "4"
        else:
            result_json["status_id"] = "6"

        result = self.client.send_post(
            'add_result/{0}'.format(test_result.id),

            result_json
        )

        return result

    def add_ids_to_tests(self, project_id, suite_id, section_ids):
        if len(section_ids) > 1:
            for section_id in section_ids:
                remote_test_cases = self.get_test_cases(project_id, suite_id, section_id)
        else:
            remote_test_cases = self.get_test_cases(project_id, suite_id, section_ids)

        for test_case in remote_test_cases:
            for test in self.test_suite.tests:
                if test_case["title"] in test.name:
                    test.id = test_case['id']

    def add_ids_to_tests_from_run(self, run_id):
        remote_test_cases = self.client.send_get('get_tests/{0}'.format(run_id))
        for test_case in remote_test_cases:
            for test in self.test_suite.tests:
                if test_case["title"] in test.name:
                    test.id = test_case['id']

    def add_section(self, section_name, project_id, suite_id, parent_id=None):
        section = {
            "name": section_name,
            "suite_id": suite_id,
        }
        if parent_id is not None:
            section["parent_id"] = parent_id

        return self.client.send_post(
            'add_section/{0}'.format(project_id),
            section
        )

    def map_section_names_to_ids(self, project_id, suite_id):
        sections = self.get_sections(project_id=project_id, suite_id=suite_id)
        print(sections)
        for section in sections:
            self.test_section_map[section['name']] = section['id']

        return self.test_section_map

    def update_suite_sections(self, project_id, suite_id):
        sections = self.get_sections(project_id=project_id, suite_id=suite_id)
        testrail_section_names = []
        to_update_section = []

        for section in sections:
            testrail_section_names.append(section['name'])

        for section_name in self.test_suite.sections:
            if section_name not in testrail_section_names:
                to_update_section.append(section_name)

        for section_name in to_update_section:
            print(self.add_section(section_name, project_id, suite_id))

        return self.map_section_names_to_ids(project_id, suite_id)


# TODO: There is an issue with loading the key from the JSON because the APIClient doesn't recognize it correctly
def load_api_key():
    global testrail_api_key
    json_file = open(api_keys_dir + "/testrail-uat-key.json")
    data = json.load(json_file)
    testrail_api_key = json.dumps(data["key"])


if __name__ == "__main__":
    """
    This is only for testing!
    """
    api = TestRailAPI()

    robot_reports = api.load_robot_tests("output/output.xml")

    # aws_reports = api.load_aws_reports()

    api.map_section_names_to_ids(24, 1185)
    api.compare_project_to_tests(24, 1185)
    api.update_suite_sections(24, 1185)
    api.update_test_cases(24, 1185)
    test_run = api.add_test_run(24, 1185, api.test_suite.test_name)
    api.add_ids_to_tests_from_run(test_run['id'])
    for test in api.test_suite.tests:
        api.add_result(test)
