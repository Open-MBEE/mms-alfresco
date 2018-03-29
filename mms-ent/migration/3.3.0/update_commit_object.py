import urllib2
import json
import sys
import getpass
import util
from elasticsearch import Elasticsearch
from elasticsearch import helpers
from datetime import datetime

'''
Created on March 26, 2018

@author: Laura Mann

Searches through all commit objects, checks for duplicate ids, removes and transforms those records.
'''


def main(args):
    es = Elasticsearch([{'host': 'internal-opencae-elasticsearch-test-lb-1895477701.us-gov-west-1.elb.amazonaws.com', 'port': '9200'}], timeout=300)
    util.base_url = 'https://opencae-test-origin.jpl.nasa.gov'
    print(util.base_url)
    username = 'lauram'
    password = getpass.getpass()
    # need auth to get projects from the rest API
    util.auth_key = {"username": username, "password": password}
    util.ticket = util.get_ticket()
    # Get all the projects
    projectsIds = None

    try:
        projects = get_projects(util.base_url, util.ticket)
        projectIds = list(projects)
    except urllib2.HTTPError as err:
        print(err.code)
        print("The script stopped here")
        sys.exit(0)
    print(projectIds)
    dupes = []
    for index in es.indices.get('*'):
        first_page = es.search(
            index=index,
            doc_type='commit',
            scroll='2m',
            size=1)
        s_id = first_page['_scroll_id']
        scroll_size = first_page['hits']['total']
        iterate = es.scroll(scroll_id=s_id, scroll="10m")
        dupes = iterate_scroll(es, s_id)

def iterate_scroll(es, scroll_id):
    iterate= es.scroll(scroll_id=scroll_id, scroll= "10m")
    scroll_id = iterate['_scroll_id']
    hits = iterate.get('hits').get('hits')
    result = find_dupes(hits)
    if hits:
        return result + iterate_scroll(scroll_id)
    else:
        return result

def find_dupes(hits):
    for hit in hits:
        id = hit.get('_id')
        added = hit.get('_source').get('added')
        ids = []
        if 'source' in entry:
            if hit.get('source').lower() == 'magicdraw':
                continue
        else:
            for entry in added:
                ids.append(id + "" + entry.get('id'))
    return list(set([x for x in ids if ids.count(x) > 1]))



# def add_actions(projectId, id, type):
#     update = {}
#     params = {}
#     script = {}
#     update['_op_type'] = 'update'
#     update['_index'] = 'mms'
#     update['_type'] = type
#     update['_id'] = id
#     script['inline'] = "ctx._source._projectId = params._projectId"
#     params['_projectId'] = projectId
#     script['params'] = params
#     update['script'] = script
#
#     return update
#
#
def get_projects(base_url, ticket):
    url = base_url + "/alfresco/service/projects?alf_ticket=%s" % (ticket)
    print(url)
    headers = {"Content-Type": "application/json"}
    projects = json.load(util.make_request(url, headers)).get('projects')
    project_ids = []
    for project in projects:
        projectId = project['id']
        project_ids.append(projectId.lower())
    return set(project_ids)


if __name__ == '__main__':
    main(sys.argv)
    # if len(sys.argv) < 4:
    #     print "You need to pass the elastic host, mms host, and mms admin username"
    # else:
    #     main(sys.argv)
