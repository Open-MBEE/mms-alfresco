#!/usr/bin/env python

import testrailapi
import argparse

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Pushing Robot Test Results to TestRail Server.')
    parser.add_argument('projectid', help="ID of the project to push to")
    parser.add_argument('suiteid', help="ID of the suite to push to")
    parser.add_argument('--sectionid', help="ID of the section to push to")

    # Optional Arguments
    parser.add_argument('--file', '-f', help="Robot test result XML file to parse")
    parser.add_argument('--user', '-u', help="User to post the test results as")
    parser.add_argument('--apikey', '-k', help="API Key of the user to post as")
    parser.add_argument('--prod', '-p', help="Use production server endpoint instead of UAT", action="store_true",
                        default=False)

    args = parser.parse_args()
    suite_id = args.suiteid
    project_id = args.projectid
    section_id = args.sectionid

    user = args.user
    api_key = args.apikey
    xml_file = args.file

    api = testrailapi.TestRailAPI(user=user, password=api_key, production=args.prod)

    robot_tests = api.load_robot_tests(xml_file)
    api.compare_project_to_robot_tests(project_id, suite_id, section_id)

    # After loading the robot output.xml it will compare the testrail server to see if the tests exists, if they don't
    #   they will be generated underneath the specified test suite
    if args.sectionid is not None:
        api.update_test_cases(project_id, suite_id, section_id)
    else:
        api.update_test_cases(project_id, suite_id)

    # Get the name of the robot test that was ran which includes the timestamp
    test_run = api.add_test_run(project_id, suite_id, api.test_suite.test_name)
    # Add the IDs from the test run to the tests
    api.add_ids_to_tests_from_run(test_run['id'])

    # Add the test results to the test run on testrail server
    for test in api.test_suite.tests:
        api.add_result(test)
