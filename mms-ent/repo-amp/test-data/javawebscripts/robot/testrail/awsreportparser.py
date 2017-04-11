#!/usr/bin/env python
'''
Author: Dan Karlsson
'''

import json
import testcase
import time
import datetime
import sys


class AWSReportJSON:
    def __init__(self, json_file):

        self.json_file = open(json_file)
        self.report = json.load(self.json_file)
        self.sections = []
        self.tests = []
        ts = time.time()
        st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')
        self.test_name = str(json_file).replace('aws', '').replace('.json', '') + "Amazon EC2 Report: " + st

        self.description = 'Used Instances = Running Instances\n' \
                           'Extra = RI Count-Used Instances\n\n' \
                           'Pass (Green) = Machine has extra instances\n' \
                           'Retest (Yellow) = Machine has no extra instances\n' \
                           'Fail (Red) = Machine has on demand instances\n'

        # separate zone into section head
        for zone in self.report.keys():
            # Check the properties of each server instance type
            self.sections.append(zone)
            for instance_type in self.report[zone]:
                new_test = testcase.TestCase()
                new_test.name = instance_type
                print("Test Name {}".format(new_test.name))
                new_test.section = zone

                # CREATE DESCRIPTION
                # Iterate through each property and create the description text for the test
                property_keys = self.report[zone][instance_type].keys()
                for instance_property in property_keys:
                    new_test.description += instance_property + ":"
                    # Check if the property is instances and iterate through each instance
                    if instance_property == "RunningInstances" or instance_property == "StoppedInstances" \
                            or instance_property == "Instances":
                        for server_instance in self.report[zone][instance_type][instance_property]:
                            new_test.description += "\n\t" + server_instance
                    else:
                        new_test.description += str(self.report[zone][instance_type][instance_property])

                    new_test.description += "\n"

                extra_instances = self.report[zone][instance_type]['Extra']

                if extra_instances > 0:
                    new_test.status = 1
                elif extra_instances == 0:
                    new_test.status = 4
                else:
                    new_test.status = 5

                self.tests.append(new_test)

        print("---------- Done Parsing ----------")

    def print_tests(self):
        print("- Done -")
        for section in self.sections:

            print('========================')
            print(section)
            print('------------------------')
            for test in self.tests:
                print(test.description)

    def get_test_section_name(self, test_name):
        for test in self.tests:
            if test_name in test.name:
                return test.section


if __name__ == "__main__":
    awsreport = AWSReportJSON('testfiles/aws.cae.json')
    awsreport.print_tests()
