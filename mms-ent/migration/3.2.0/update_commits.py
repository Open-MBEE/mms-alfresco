import urllib2
import json
import sys
import getpass
import util
from elasticsearch import Elasticsearch
from elasticsearch import helpers
from datetime import datetime

'''
Created on Sept 13, 2017

@author: Laura Mann

Updates the commits, projects and ref objects with the correct project Id key

'''


def main(args):
    es = Elasticsearch([{'host': args[1], 'port': 9200}], timeout=300)
    util.base_url = args[2]
    print(util.base_url)
    username = args[3]
    password = getpass.getpass()
    # need auth to get projects from the rest API
    util.auth_key = {"username": username, "password": password}
    util.ticket = util.get_ticket()
    # Get all the projects
    projectsIds = None
    project_elastic = []
    try:
        projects, project_elastic = get_projects(util.base_url, util.ticket)
        projectIds = list(projects)
    except urllib2.HTTPError as err:
        print(err.code)
        print("The script stopped here")
        sys.exit(0)
    project_actions = []
    for key, value in project_elastic.iteritems():
        project_actions.append(add_actions(key, value, 'element'))
    helpers.bulk(es, project_actions)
    refs = {}
    ref_elastic = []
    try:
        refs, ref_elastic = get_refs_by_project(util.base_url, util.ticket, projectIds)
    except urllib2.HTTPError as err:
        print(err.code)
        print("The script stopped here")
        sys.exit(0)
    ref_actions = []
    for key, value in ref_elastic.iteritems():
        for ref in value:
            ref_actions.append(add_actions(key, ref, 'element'))
            print(key)
            print(ref)
    helpers.bulk(es, ref_actions)

    commits_by_project = {}
    for key, value in refs.iteritems():
        try:
            commits = get_commits_by_ref(util.base_url, util.ticket, key, value)
            commits_by_project[key] = list(commits)
        except urllib2.HTTPError as err:
            print(err.code)
            print("The script stopped here")
            sys.exit(0)
    actions = []
    print("start")
    print(datetime.now().time())
    for key, value in commits_by_project.iteritems():
        for commitId in value:
            actions.append(add_actions(key, commitId, 'commit'))
    helpers.bulk(es, actions)
    # This logic chunks the actions if the ES machine is too small
    # chunks = [actions[x:x+80] for x in xrange(0, len(actions), 80)]
    # total = len(chunks)
    # print("number of chunks is:")
    # print(total)
    # for chunk in chunks:
    #    helpers.bulk(es, chunk)
    #    if total%20 == 0:
    #        time.sleep(120)
    #    total = total - 1
    #    print("This many left: ")
    #    print(total)
    print("end")
    print(datetime.now().time())


def get_refs_by_project(base_url, ticket, projects):
    project_refs = {}
    refs_elastic_ids = {}
    for projectId in projects:
        url = base_url + "/alfresco/service/projects/%s/refs?alf_ticket=%s" % (projectId, ticket)
        headers = {"Content-Type": "application/json"}
        refs = json.load(util.make_request(url, headers)).get('refs')
        refIds = []
        refsElastic = []
        for ref in refs:
            refId = ref['id'].replace('-', '_')
            refIds.append(refId)
            refsElastic.append(ref['_elasticId'])
        project_refs[projectId] = refIds
        refs_elastic_ids[projectId] = (refsElastic)
    return project_refs, refs_elastic_ids


def get_commits_by_ref(base_url, ticket, projectId, refs):
    # http://localhost:8080/alfresco/service/projects/PA/refs/master/history
    commits = []
    for ref in refs:
        url = base_url + "/alfresco/service/projects/%s/refs/%s/history?alf_ticket=%s" % (projectId, ref, ticket)
        headers = {"Content-Type": "application/json"}
        # :TODO check for project or ref object and add elasticId instead
        for commit in json.load(util.make_request(url, headers)).get('commits'):
            commits.append(commit['id'])
    # set should guarantee uniqueness
    return set(commits)


def add_actions(projectId, id, type):
    update = {}
    params = {}
    script = {}
    update['_op_type'] = 'update'
    update['_index'] = 'mms'
    update['_type'] = type
    update['_id'] = id
    script['inline'] = "ctx._source._projectId = params._projectId"
    params['_projectId'] = projectId
    script['params'] = params
    update['script'] = script

    return update


def get_projects(base_url, ticket):
    url = base_url + "/alfresco/service/projects?alf_ticket=%s" % (ticket)
    print(url)
    headers = {"Content-Type": "application/json"}
    projects = json.load(util.make_request(url, headers)).get('projects')
    project_elastic = {}
    project_ids = []
    for project in projects:
        projectId = project['id']
        project_elastic[project['id']] = project['_elasticId']
        project_ids.append(projectId)
    return set(project_ids), project_elastic


if __name__ == '__main__':
    if len(sys.argv) < 4:
        print "You need to pass the elastic host, mms host, and mms admin username"
    else:
        main(sys.argv)
