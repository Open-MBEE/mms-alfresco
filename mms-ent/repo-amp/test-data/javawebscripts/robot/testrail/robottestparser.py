import xml.etree.ElementTree as ET
import testcase


class RobotXML:
    def __init__(self, xml_file, ):
        self.debug = True
        self.tree = ET.parse(str(xml_file))
        self.root = self.tree.getroot()
        self.tests = []
        self.sections = []
        self.test_name = "Test Run" + '-' + self.root.attrib['generated'].split(' ')[0] \
                         + '-' + self.root.attrib['generated'].split(' ')[1]

        root_suite = self.root.find('suite')

        for suite in root_suite:
            section_name = ""
            if suite.tag != "status":
                section_name = suite.attrib['name']
                sub_suites = suite.find('suite')
                section_name += " " + sub_suites.attrib['name']
                print("====================================")
                print("Section Name : {}".format(section_name))
                self.sections.append(section_name)
                for test in sub_suites:
                    newTest = testcase.TestCase()
                    if test.tag == "test":
                        newTest.name = test.attrib['name']
                        newTest.section = section_name
                        newTest.status = test.find('status').attrib['status']
                        print("------------------------------------")
                        print("Test name : {}\nTest Section : {}\nTest Status : {}".format(newTest.name,
                                                                                           newTest.section,
                                                                                           newTest.status))
                        self.tests.append(newTest)

    def print_tests(self):
        for test in self.tests:
            print(test.name)

    def get_test_section_name(self, test_name):
        for test in self.tests:
            if test_name in test.name:
                return test.section


if __name__ == "__main__":
    robot = RobotXML("../output/output.xml")
