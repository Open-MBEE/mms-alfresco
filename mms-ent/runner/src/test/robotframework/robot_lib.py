#!/usr/bin/env python
import json
import argparse
import collections
import platform
import os
import shutil
import subprocess
import requests

'''
Example of GetSites JSON converted to Robot Framework

Get Sites
    [Documentation]     Get Sites
    [Tags]              Get   Sites
    ${result} =         Get  ${ROOT}/refs/master/sites
    Should Be Equal     ${result.status_code}  ${200}
    ${json} =           Set Variable  ${result.json()}
    ${some_stuff} =     Create List    ${COMMON_FILTERS}
    ${result} =         Compare JSON        test_name=${TEST_NAME}    json_object=${json}   filters=${some_stuff}   create_baseline=${CREATE_BASELINE}
    Should Match Baseline   ${result}
'''

# Global Variables
develop_baseline_dir = '../developBaselineDir/'
debug_test = False
ROOT_RETURN_ELEMENTS = ['elements', 'refs', 'configurations', 'products', 'sites', 'views', 'ownersNotFound',
                        'message', 'workspace1', 'workspace2', 'documents']
if "/runner/src/test/robotframework" not in os.getcwd():
    robot_dir = os.getcwd() + '/runner/src/test/robotframework'
else:
    robot_dir = os.getcwd()
output_dir = os.listdir(robot_dir + '/output')
if "baseline" not in output_dir:
    os.mkdir(robot_dir + '/output/baseline')

if "results" not in output_dir:
    os.mkdir(robot_dir + '/output/results')

if "original" not in output_dir:
    os.mkdir(robot_dir + '/output/original')


def sanitize_json_files(unsanitized_json_file):
    """
    Takes a JSON file and removes the last 200 line from the file. This may become unnecessary if we change what is
    written to the JSON file.
    :param unsanitized_json_file: JSON File containing a response code on the last line.
    :return: Nothing
    """
    with open(develop_baseline_dir + unsanitized_json_file) as f:
        lines = f.readlines()

        with open('sanitizedJson/sanitized_{}'.format(unsanitized_json_file), 'w') as w:
            w.writelines([item for item in lines[:-1]])


def write_json_to_file(json_object, filename):
    """
    Take a given json object and write it the specified filename
    :param json_object: JSON String, will attempt to serialize this string before writing it to the file.
    :param filename: Name of the file to write object to
    :return: Nothing
    """
    try:
        # Try to serialize it before writing
        json_object = json.dumps(json_object)
    except TypeError:
        print("Failed to serialize the object")
        try:
            json_object = json.loads(json_object)
            json_object = json.dumps(json_object)
        except TypeError:
            print("Failed secondary serialization of json object")

    json_file = robot_dir + "/output/original/{}_orig.json".format(filename.replace(' ', ''))
    with open(json_file, 'w') as json_orig_file:
        json_orig_file.writelines(json_object)


def delete_keys_from_dict(dict_del, the_keys):
    """
    Delete the keys present in the lst_keys from the dictionary.
    Loops recursively over nested dictionaries.
    """
    # make sure the_keys is a set to get O(1) lookups
    if type(the_keys) is not set:
        the_keys = set(the_keys)
    for k, v in dict_del.items():
        if k in the_keys:
            del dict_del[k]
        if isinstance(v, dict):
            delete_keys_from_dict(v, the_keys)
        if isinstance(v, list):
            for item in v:
                if isinstance(item, dict):
                    delete_keys_from_dict(item, the_keys)
    return dict_del


def filter_json_file(filename, filters_list):
    """
    Function will take a json file, open it and generate a filtered JSON file based on the filters in the list
    :param filename: JSON File to be filtered
    :param filters_list: List of filters that will be stripped out of the original file
    :return: Filtered JSON and Original Unfiltered JSON
    """
    filename = filename.replace(" ", "")
    print("Filters List {}".format(filters_list))
    if isinstance(filters_list, list) is False:
        filters_list = [filters_list]
    with open(robot_dir + "/output/original/{}_orig.json".format(filename), "r") as file_orig:
        # Get the root Json Object
        json_root_object = json.load(file_orig)

        if "commitId" in json_root_object:
            del json_root_object["commitId"]

        # Store the original json object
        orig_json_object = json.dumps(json_root_object)
        root_key = json_root_object.keys()[0]

        if root_key == "message" and len(json_root_object.keys()) > 1:
            root_key = json_root_object.keys()[1]

        # Get the array of objects under the root
        try:
            if len(filters_list) > 0:
                json_root_object = delete_keys_from_dict(json_root_object, filters_list)
        except:
            print('Failed to delete keys from json_root_object before iteration')

        if root_key in ROOT_RETURN_ELEMENTS:
            json_array = json_root_object[root_key]

            if isinstance(json_array, list):
                # Clear the original array
                json_root_object[root_key] = []

                # For each object remove any of the keys that are specified in the filter list then put the object back into
                #   the array
                for json_object in json_array:
                    if len(filters_list) > 0:
                        json_object = delete_keys_from_dict(json_object, filters_list)
                    json_root_object[root_key].append(json_object)
                    # json_root_object = sorted(json_root_object)
            elif isinstance(json_array, str):
                json_root_object = json_array

        # Serialize the JSON object before writing it to the file
        json_object = json.dumps(json_root_object, sort_keys=True, indent=4, separators=(',', ': '))
    # Write to result .json file:
    filtered_filename = robot_dir + "/output/results/{}.json".format(filename)
    filtered_file = open(filtered_filename, "w")
    # print(json_object)
    filtered_file.writelines(json_object)
    filtered_file.close()

    # return filtered_output, orig_output
    return json_object, orig_json_object


def create_baseline_json(baseline_json_file):
    # TODO: Update this... Technically shouldn't need to sort it again since it was sorted when written to the results
    # directory
    """
    Take a JSON object and write the object to the specified baseline JSON file name.
    :param baseline_json_file:
    :return:
    """
    baseline_json_file = baseline_json_file.replace(' ', '')
    if 'json' not in baseline_json_file:
        baseline_json_file += ".json"
    with open(baseline_json_file, 'r') as json_file:
        try:
            json_object = json.load(json_file)
        except:
            print("Failed to open {}".format(json_file))
            return

            # cmd = "cat output/results/" + baseline_json_file + " | jq --sort-keys 'if .sysmlid? != null then sort_by(.sysmlId)? else . end' > output/baseline/" + baseline_json_file.replace(
            #     '_orig', '').replace('.json', '') + "_sorted.json"
            # print(commands.getoutput(cmd))


def create_result_json(json_object, result_json_file):
    """
    Take a JSON object and write the object to the specified result json file name.
    :param json_object:
    :param result_json_file:
    :return:
    """
    write_json_to_file(json_object, result_json_file)


def compare_json(test_name):  # , json_object, filters):
    """
    Takes a JSON object and looks for the baseline json that is specified by test_name.
    :param test_name:
    :param json_object:
    :param filters:
    :param create_baseline:
    :return:
    """
    baseline_json = None
    result_json = None
    try:
        with open(robot_dir + "/output/baseline/{}.json".format(test_name.replace(' ', ''))) as baseline_file:
            try:
                with open(robot_dir + "/output/results/{}.json".format(test_name.replace(' ', ''))) as result_file:
                    baseline_json = json.load(baseline_file)
                    result_json = json.load(result_file)
            except:
                print("Failed to open the results json")
    except:
        print("Failed to open the baseline json")
        return False
    return ordered(baseline_json) == ordered(result_json)

def compare_json_to_json(json1, json2):  # , json_object, filters):
    """
    Compare 2 JSON objects against each other.
    :param json1:
    :param json2:
    :return:
    """
    return ordered(json1) == ordered(json2)

def id_exists(test_name):
    """
    Takes a JSON object and looks for the baseline json that is specified by test_name.
    :param test_name:
    :param json_object:
    :param filters:
    :param create_baseline:
    :return:
    """
    result_json = None
    try:
        with open(robot_dir + "/output/results/{}.json".format(test_name.replace(' ', ''))) as result_file:
            result_json = json.load(result_file)
    except:
        print("Failed to open the result json")
        return False
    #look for values NEW_ASSOC, NEW_PROP1, NEW_PROP2
    print(result_json)
    if 6 == 6:
        return True
    return "Length is not 6"
def get_last_commit_id(commits):
    """
    finds the list commit in a list
    :param commits:
    :return:
    """
    print(commits)
    if bool(commits):
        return commits[-1].get('id')
    return "no commits"

def commit_naught(commits):
    """
    finds the list commit in a list
    :param commits:
    :return:
    """
    print(type(commits.get('commits')[0].get('id')))
    # print(commits.get('commits'))
    # print(commits.get('commits').get('id'))
    #print(commits[0].get('commits').get('id'))

    if bool(commits):
        return commits.get('commits')[0].get('id')
    return "no commits"

def clear_results_dir():
    try:
        shutil.rmtree('output/results')
        os.mkdir('output/results')
    except:
        print("Failed to clear the results directory")


def change_property_name(app_directory, old_property, new_property):
    """
    Function is used to change the properties inside of a json file. This is useful for when the JSON structure changes
    :param app_directory: Directory of the JSON files
    :param old_property: Name of the old property
    :param new_property: Name of the new property
    :return:
    """
    os.chdir(app_directory)
    dir_content = os.listdir(app_directory)
    for f in dir_content:
        print(f)
        subprocess.check_call(["sed", "-i", "''", 's/\{}/{}/g'.format(old_property, new_property), f])


def ordered(obj):
    if isinstance(obj, dict):
        return sorted((k, ordered(v)) for k, v in obj.items())
    if isinstance(obj, list):
        return sorted(ordered(x) for x in obj)
    else:
        return obj


def get_hostname():
    return platform.node().replace('-origin', '')


def get_sysmlid_from_dict(json_object):
    """
    :param json_object: JSON Dictionary objecet to be searched.
    :return:
    """
    sysmlId = ""
    for k, v in json_object.items():
        if k in "sysmlId":
            sysmlId = v
        if isinstance(v, dict):
            get_sysmlid_from_dict(v)
    return sysmlId


def get_id_from_workspace(workspace_json):
    """
    Method will grab the ID out of the json object. Typically used for the the refs tests when branching or
    creating refs based off of tags.
    :param workspace_json: Name of the JSON object that contains the id needed for the test
    :return: ID found in the JSON object
    """
    # workspace_object = open('output/original/{}_orig.json'.format(workspace_json))
    # workspace_object = json.load(workspace_object)
    # return workspace_object['refs'][0]['id']
    return get_id_from_json(workspace_json, 'refs')


def get_id_from_json(test_name, root_key):
    """
    Method will get the id from the generated JSON response from the given json object in the directory containing the
    original JSON responses.
    :param test_name: Name of the JSON file to grab
    :param root_key: Specify what type of root key
    :return: Return the ID
    """
    json_object = open(robot_dir + '/output/original/{}_orig.json'.format(test_name))
    json_object = json.load(json_object)
    print(json_object[str(root_key)][0]['id'])
    return json_object[root_key][0]['id']
    # return json_object


def get_copy_time_from_json(test_name, root_key, exclude_origin=False):
    json_file = ''
    if exclude_origin:
        json_file = test_name
    else:
        json_file = test_name + "_orig"

    json_object = open(robot_dir + '/output/original/{}.json'.format(json_file))
    json_object = json.load(json_object)
    created = ''
    # print(json_object[str(root_key)][0]['created'])
    if json_object[str(root_key)][0].has_key('created'):
        created = json_object[str(root_key)][0]['created']
    elif json_object[str(root_key)][0].has_key('_created'):
        created = json_object[str(root_key)][0]['_created']
    return json.dumps(created)


def check_if_elements_is_empty(json_object):
    """
    Checks to see that there are elements in the json object
    :param json_object:
    :return: bool false if not empty, true if empty
    """
    try:
        if len(json_object) > 0:
            is_empty = len(json_object['elements']) == 0
        else:
            is_empty = True
    except KeyError:
        print("TypeError [" + str(TypeError) + " ]")
        return True
    return is_empty


def is_empty_json(json_object):
    """
    Checks to see if the JSON object is empty
    :param json_object:
    :return: Boolean if the object is empty
    """
    try:
        obj = json.load(json_object)
    except:
        obj = json.dumps(json_object)
        print(TypeError.message)
    return len(obj) == 0


def create_workspace_json(ws_name, ws_description="Some workspace", ws_parent="master"):
    workspace_json = {
        "refs": [{
            "name": str(ws_name),
            "description": "<p>{}</p>\n".format(ws_description),
            "permission": "read",
            "parent": ws_parent
        }]
    }
    json_file = open('NewJsonData/CreateWorkspace_{}.json'.format(str(ws_name).replace(' ', '')), 'w')
    json_file.writelines(json.dumps(workspace_json))
    return workspace_json

def get_elements_from_elasticsearch(sysmlId, index="mms", elasticHost="localhost"):
    """
    Method will return an array of elements based on the sysmlid provided. It will be the entire history of the element.
    :param sysmlid: string
    :param index: ElasticSearch Index
    :param elastichost: Hostname of ElasticSearch
    :return:
    """
    query = {
        "query":{
            "term":{
                "id":sysmlId
            }
        }
    }
    res = requests.post("http://{}:9200/{}/_search".format(elasticHost,index), data=json.dumps(query))
    return res.json()["hits"]["hits"]


def find_element_by_commit(sysmlId, commitId):
        """
        Returns an element at a specific commit.
        :param sysmlid:
        :param commitId:
        :return:
        """
        elementList = get_elements_from_elasticsearch(sysmlId)
        for element in elementList:
            if element["_source"]["_commitId"] == commitId:
                return element["_source"]

def get_element_commit_ids(sysmlId):
    """
    Returns a list of commit ids based on the element sysmlid provided.
    :param sysmlid:
    :return:
    """
    elements = get_elements_from_elasticsearch(sysmlId=sysmlId)
    commits = []
    for element in elements:
        commits.append(element["_source"]["_commitId"])
    return commits

def element_exists_in_commit(sysmlId, commitId):
    return find_element_by_commit(sysmlId=sysmlId,commitId=commitId) > 0

def get_commit_timestamp(commitId, index="mms", elasticHost="localhost"):
    query = {
        "query":{
            "term":{
                "_commitId":commitId
            }
        }
    }
    res = requests.post("http://{}:9200/{}/_search".format(elasticHost,index), data=json.dumps(query))
    return res.json()["hits"]["hits"][0]["_source"]["_created"]

def get_commit_from_json(jsonObject):
    return jsonObject["elements"][0]["_commitId"]

def get_all_commits(index="mms", elasticHost="localhost", excludeCommits=[]):
    """
    Returns all commits in elasticsearch. This is only useful when there is a small number of elements and commits in
    elasticsearch. If this is used a model that is large then the method may return more commits than desired.
    :param index:
    :param elasticHost:
    :return:
    """
    query = {
        "query":{
            "match_all":{}
        },
        "size":"30"
    }
    res = requests.post("http://{}:9200/{}/commit/_search".format(elasticHost,index), data=json.dumps(query))
    commitObjectList = {}
    for hit in res.json()["hits"]["hits"]:
        if(hit['_source']['_elasticId']) not in excludeCommits:
            commitObjectList[hit['_source']['_created']] = hit['_source']['_elasticId']
    return commitObjectList

def get_all_project_commits(projectId, index="mms", elasticHost="localhost"):
    query = {
        "query": {
            "term":{
                "_projectId": projectId
            }
        }
    }
    res = requests.post("http://{}:9200/{}/_search".format(elasticHost, index), data=json.dumps(query))
    commitObjectList = []
    for hit in res.json()["hits"]["hits"]:
        if hit["_source"]["_commitId"] not in commitObjectList:
            commitObjectList.append(hit["_source"]["_commitId"])
    return commitObjectList

def get_number_of_commits(project_id, host="localhost", ref="master", user="admin", password="admin"):
    return len((requests.get("http://{}:{}@{}:8080/alfresco/service/projects/{}/refs/{}/history"
                             .format(user, password, host, project_id, ref)).json())["commits"])

def get_last_commit():
    commits = get_all_commits()
    sorted_list = sorted(commits)
    return commits[sorted_list[-1]]

def get_commit_in_between_latest_and_element(sysmlId, projectId):
    element_commits = get_element_commit_ids(sysmlId)
    commits = get_all_commits(excludeCommits=element_commits)
    sorted_list = sorted(commits)
    project_commits = get_all_project_commits(projectId)
    for c in sorted_list:
        if commits[c] not in element_commits and commits[c] in project_commits:
            return commits[c]

# Main Application when running from the commandline
if __name__ == "__main__":
    cur_dir = os.listdir('.')
    if robot_dir + '/output' not in cur_dir:
        os.mkdir(robot_dir + "/output")

    parser = argparse.ArgumentParser(description="Generate the keyword test files based on JSON baseline.")
    parser.add_argument("--sanitize", help="Sanitize the baseline json to remove the 200 status code at the end.",
                        action="store_true", default=False)

    args = parser.parse_args()
    if args.sanitize:
        baseline_files = os.listdir(develop_baseline_dir)
        baseline_files = collections.deque(baseline_files)
        # print(baseline_files)
        while len(baseline_files) > 0:
            unsanitized_json_file = baseline_files.pop()
            print(unsanitized_json_file)
            if unsanitized_json_file != '.gitignore' and 'sorted' not in unsanitized_json_file:
                sanitize_json_files(unsanitized_json_file)
    else:
        parser.add_argument("testname", help="Name of the test to generate")
        parser.add_argument("json", help="Name of the JSON file to parse and generating Robot Keywords")
        args = parser.parse_args()

        # write_test(args.testname, args.json)
