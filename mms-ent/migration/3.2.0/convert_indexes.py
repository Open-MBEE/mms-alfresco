import urllib2
import json
import sys
import getpass
import util
from elasticsearch import Elasticsearch
from elasticsearch import helpers
from datetime import datetime

'''
Created on August 30, 2017

@author: Laura Mann

Grabs all the data from the current index and re-indexes by project

'''


def main(args):
    util.base_url = args[1]
    print(util.base_url)
    username = args[2]
    password = getpass.getpass()
    # need auth to get projects from the rest API
    util.auth_key = {"username": username, "password": password}
    util.ticket = util.get_ticket(util.base_url, util.auth_key)
    es = Elasticsearch([{'host': args[3], 'port': 9200}], timeout=300)
    remoteElasticsearch = Elasticsearch([{'host': args[4], 'port': 9200}], timeout=300)

    # Get all the projects
    projects = None
    try:
        get_response = get_projects(util.base_url, util.ticket)
        projects = json.load(get_response).get('projects')
    except urllib2.HTTPError as err:
        print(err.code)
        print("The script stopped here")
        sys.exit(0)

    # get all documents in each project
    projectIds = get_project_ids(projects)
    print("start")
    print(datetime.now().time())
    numberOfProjects = len(projectIds)
    print("total projects")
    print(numberOfProjects)

    # create default index
    create_index(remoteElasticsearch, "mms")

    for id in projectIds:
        # post all documents to new index
        create_index(remoteElasticsearch, id.lower().replace(" ", ""))
        print("preparing to post project: ")
        print(id)
        query = {"query": {"term": {"_projectId": id}}}
        index = id.lower().replace(" ", "")
        # bulk post to index
        helpers.reindex(es, "mms", index, query=query, target_client=remoteElasticsearch, scroll=u'10m',
                        scan_kwargs={"doc_type": "element"}, bulk_kwargs={"doc_type": "element"})
        helpers.reindex(es, "mms", index, query=query, target_client=remoteElasticsearch, scroll=u'10m',
                        scan_kwargs={"doc_type": "commit"}, bulk_kwargs={"doc_type": "commit"})
        print("finished posting project: ")
        print(id)
        if numberOfProjects > 0:
            numberOfProjects = numberOfProjects - 1
        print("projects left:   ")
        print(numberOfProjects)

    print("finish")
    print(datetime.now().time())


def get_project_ids(projectsJson):
    projectIds = []
    for project in projectsJson:
        projectId = project.get('_projectId')
        if projectId is not None:
            projectIds.append(projectId)
    return projectIds


def create_index(es, projectId):
    res = es.indices.create(index=projectId)
    # :TODO should check that the key is true, otherwise exit
    print(res['acknowledged'])


def get_projects(base_url, ticket):
    url = base_url + "/alfresco/service/projects?alf_ticket=%s" % (ticket)
    print(url)
    headers = {"Content-Type": "application/json"}
    return util.make_request(url, headers)


def add_actions(projectId, type, id, doc):
    # {
    #     '_index': 'index-name',
    #     '_type': 'document',
    #     '_id': 42,
    #     '_parent': 5,
    #     'pipeline': 'my-ingest-pipeline',
    #     '_source': {
    #         "title": "Hello World!",
    #         "body": "..."
    #     }
    # }
    index = {}
    index['_index'] = projectId
    index['_type'] = type
    index['_id'] = id
    index['_source'] = doc
    return index


if __name__ == '__main__':
    if len(sys.argv) < 5:
        print "Not enough arguments.  Needs the mms host, Alfresco admin username, src elastic host, and hostname of destination elasticsearch server. "
    else:
        main(sys.argv)
