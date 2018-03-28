import urllib2
import json
import sys
import getpass
import util
from elasticsearch import Elasticsearch

'''
Created on March 26, 2018

@author: Laura Mann

Searches through all commit objects, checks for duplicate ids, removes and transforms those records.
'''


def main(args):
    es = Elasticsearch([{'host': 'internal-opencae-elasticsearch-test-lb-1895477701.us-gov-west-1.elb.amazonaws.com', 'port': '9200'}], timeout=300)
    # util.base_url = 'https://opencae-test-origin.jpl.nasa.gov'
    # print(util.base_url)
    # username = 'lauram'
    # password = getpass.getpass()
    # # need auth to get projects from the rest API
    # util.auth_key = {"username": username, "password": password}
    # util.ticket = util.get_ticket()
    # # Get all the projects
    # projectsIds = None
    #
    # try:
    #     projects = get_projects(util.base_url, util.ticket)
    #     projectIds = list(projects)
    # except urllib2.HTTPError as err:
    #     print(err.code)
    #     print("The script stopped here")
    #     sys.exit(0)
    # print(projectIds)
    # dupes = []
    # for index in es.indices.get('*'):
    #     first_page = es.search(
    #         index=index,
    #         doc_type='commit',
    #         scroll='2m',
    #         size=1)
    #     s_id = first_page['_scroll_id']
    #     scroll_size = first_page['hits']['total']
    #     iterate = es.scroll(scroll_id=s_id, scroll="10m")
    #     dupes = iterate_scroll(es, s_id)
    search_for_ids = es.get(
        index='project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63',
        doc_type='element',
        id='7d7e07f2-2a81-4b21-98c5-e1a7ccead848')
    source = search_for_ids['_source']
    print(source.get('associationId'))
    print(source.get('appliedStereotypeInstanceId'))
    #print(search_for_ids['_source']['associationId'])
    #print(search_for_ids['_source']['appliedStereotypeInstanceId'])


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
                ids.append((id,entry.get('id'), entry.get('elasticId')))
    return list(set([x for x in ids if ids.count(x) > 1]))

def delete_duplicates(dupes):
    return ' '
#     POST test/_doc/1/_update
# {
#     "script" : {
#         "source": "if (ctx._source.tags.contains(params.tag)) { ctx.op = 'delete' } else { ctx.op = 'none' }",
#         "lang": "painless",
#         "params" : {
#             "tag" : "green"
#         }
#     }
# }
#
# POST test/_doc/1/_update
# {
#     "script" : "ctx._source.remove('new_field')"
# }

#project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63/element/b3e98746-93be-4909-b5d5-d11fa1d731c6
def find_correct_ids(es, dupes, index):
    updates = []
    for d in dupes:
        search_for_ids = es.get(
            index=index,
            doc_type='element',
            id=d[2])
        source = search_for_ids['_source']
        association_obj = es.get(
            index=index,
            doc_type='element',
            id=source.get('associationId'))
        appliedStereotypeInstanceId_obj = es.get(
            index=index,
            doc_type='element',
            id=source.get('appliedStereotypeInstanceId'))
        ownedEndId_obj = es.get(
            index=index,
            doc_type='element',
            id=appliedStereotypeInstanceId_obj.get('ownedEndIds')[0])

        o = {'associationId': association_obj.get('_source').get('_elasticId'),
             'appliedStereotypeInstanceId': appliedStereotypeInstanceId_obj.get('_source').get('_elasticId'),
             'ownedEndId': ownedEndId_obj.get('_source').get('_elasticId'),
             'commitId': d[0],
             'sysmlid':d[1],
             'elasticId':d[2]}
        updates.append(o)
    return updates

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
