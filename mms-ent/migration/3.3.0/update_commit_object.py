import urllib2
import json
import sys
import getpass
import util
from elasticsearch import Elasticsearch
from elasticsearch import helpers

'''
Created on March 26, 2018

@author: Laura Mann

Searches through all commit objects, checks for duplicate ids, removes and transforms those records.
'''


def main(args):
    es = Elasticsearch([{'host': 'localhost', 'port': '9200'}], timeout=300)
    # util.base_url = 'https://opencae.jpl.nasa.gov'
    # print(util.base_url)
    # username = 'lauram'
    # password = getpass.getpass()
    # # need auth to get projects from the rest API
    # util.auth_key = {"username": username, "password": password}
    # util.ticket = util.get_ticket()
    # Get all the projects
    projectsIds = None
    #:TODO have multiple examples, run a removal script
    # try:
    #     projects = get_projects(util.base_url, util.ticket)
    #     projectIds = list(projects)
    # except urllib2.HTTPError as err:
    #     print(err.code)
    #     print("The script stopped here")
    #     sys.exit(0)
    # print(projectIds)
    dupes = []
    # for index in es.indices.get('*'):
    #     print("We working on dis index  " + index)
    #     first_page = es.search(
    #         index=index,
    #         doc_type='commit',
    #         scroll='2m',
    #         size=1000)
    #     s_id = first_page['_scroll_id']
    #     #scroll_size = first_page['hits']['total']
    #     #iterate = es.scroll(scroll_id=s_id, scroll="2m")
    #     #scroll_id = iterate['_scroll_id']
    #     dupes = dupes + iterate_scroll(es, s_id)

    first_page = es.search(
        index='project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63',
        doc_type='commit',
        scroll='2m',
        size=10000)
    dupes = find_dupes(first_page.get('hits').get('hits'))
    s_id = first_page['_scroll_id']
        #scroll_size = first_page['hits']['total']
        #iterate = es.scroll(scroll_id=s_id, scroll="2m")
        #scroll_id = iterate['_scroll_id']
    dupes = dupes + iterate_scroll(es, s_id)

    # search_for_ids = es.get(
    #     index='project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63',
    #     doc_type='commit',
    #     id='7d7e07f2-2a81-4b21-98c5-e1a7ccead848')
    #
    # source = search_for_ids['_source']
    # new_ids = find_correct(dupes)
    #print(dupes)
    #print(dupes)
    project_actions = []
    for d in dupes:
        new_added = find_correct_id(es, d)
        search_for_ids = es.get(
            index=d[1],
            doc_type='commit',
            id=d[0])
        added = search_for_ids['_source'].get('added')
        for entry in added:
            if entry['id'] == d[2] and entry['_elasticId'] == d[3]:
                continue
            else:
                new_added.append(entry)
        project_actions.append(add_actions(new_added, d[0], 'commit'))
    helpers.bulk(es, project_actions)


def iterate_scroll(es, scroll_id):
    iterate= es.scroll(scroll_id=scroll_id, scroll= "2m")
    s_id = iterate['_scroll_id']
    hits = iterate.get('hits').get('hits')
    result = find_dupes(hits)
    if hits:
        return result + iterate_scroll(es, s_id)
    else:
        return result

def find_dupes(hits):
    ids = []
    for hit in hits:
        id = hit['_id']
        added = hit['_source']['added']
        #projectId = hit.get('_source').get('_projectId').lower()
        # if hit.get('_source').get('_projectId') is None:
        #     print(hit)
        if 'source' in hit:
            if hit['source'].lower() == 'magicdraw':
                continue
        elif len(added) > 5000:
            continue
        else:
            for entry in added:
                projectId = hit['_source']['_projectId'].lower()
                ids.append((id, projectId, entry['id'], entry['_elasticId']))
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
#int total = 0; for (int i = 0; i < doc['added'].length; ++i) { total += doc['goals'][i]; } return total;
# POST test/_doc/1/_update
# {
#     "script" : "ctx._source.remove('new_field')"
# }
# GET project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63/commit/7d7e07f2-2a81-4b21-98c5-e1a7ccead848
# {
#     "script" : "def total = new ArrayList(); for (int i = 0; i < doc['added'].length; ++i) { total.add(i); } return total"
# }
#curl -X GET http://localhost:9200/project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63/commit/cbeb265b-9b73-4144-931b-de29c99155b4?pretty -d '{"script": {"inline": "def total = new ArrayList(); for (int i = 0; i < doc['added'].length; ++i) { total.add(i); } return total"}}'

#curl -X GET http://localhost:9200/project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63/commit/cbeb265b-9b73-4144-931b-de29c99155b4/_source?_source_include=added -d '{"query": {"script": {"script": {"inline": "return doc['_source']"}}}}'

#curl -XGET 'localhost:9200/twitter/tweet/1/_source?_source_include=*.id&_source_exclude=entities'&pretty'


# def find_correct_ids(es, dupes):
#     updates = []
#     for d in dupes:
#         search_for_ids = es.get(
#             index=d[1],
#             doc_type='element',
#             id=d[3])
#         source = search_for_ids['_source']
#         association_obj = es.search(
#             index=d[1],
#             doc_type='element',
#             body= define_query(d[0], source.get('associationId')))
#         appliedStereotypeInstanceId_obj = es.search(
#             index=d[1],
#             doc_type='element',
#             body=define_query(d[0], source.get('appliedStereotypeInstanceId')))
#         ownedEndId_obj = es.search(
#             index=d[1],
#             doc_type='element',
#             body=define_query(d[0], appliedStereotypeInstanceId_obj.get('ownedEndIds')[0]))
#
#         o = {'associationId': association_obj.get('_source').get('_elasticId'),
#              'appliedStereotypeInstanceId': appliedStereotypeInstanceId_obj.get('_source').get('_elasticId'),
#              'ownedEndId': ownedEndId_obj.get('_source').get('_elasticId'),
#              'commitId': d[0],
#              'sysmlid':d[2],
#              'elasticId':d[3]}
#         updates.append(o)
#     return updates

def find_correct_id(es, d):
    update_added = []
    search_for_ids = es.get(
        index=d[1],
        doc_type='element',
        id=d[3])
    source = search_for_ids['_source']
    association_obj = es.search(
        index=d[1],
        doc_type='element',
        body= define_query(d[0], source.get('associationId')))
    ass_obj = {'id': association_obj.get('_source').get('id'), '_elasticId': association_obj.get('_source').get('_elasticId')}
    update_added.append(ass_obj)
    appliedStereotypeInstanceId_obj = es.search(
        index=d[1],
        doc_type='element',
        body=define_query(d[0], source.get('appliedStereotypeInstanceId')))
    app_obj = {'id': appliedStereotypeInstanceId_obj.get('_source').get('id') ,'_elasticId': appliedStereotypeInstanceId_obj.get('_source').get('_elasticId')}
    update_added.append(app_obj)
    ownedEndId_obj = es.search(
        index=d[1],
        doc_type='element',
        body=define_query(d[0], appliedStereotypeInstanceId_obj.get('ownedEndIds')[0]))
    owned_obj = {'id': ownedEndId_obj.get('_source').get('id'), '_elasticId': ownedEndId_obj.get('_source').get('_elasticId')}
    update_added.append(owned_obj)
    return update_added

def define_query(commitId, sysmlid):
    return {
        "query": {
            "bool": {
                "must": [
                    {
                        "term": {
                            "id": {
                                "value": sysmlid,
                            }
                        }
                    },
                    {
                        "term": {
                            "_commitId": commitId
                        }
                    }
                ]
            }
        }
    }

def add_actions(added, id, type):
    update = {}
    params = {}
    script = {}
    update['_op_type'] = 'update'
    update['_index'] = 'mms'
    update['_type'] = type
    update['_id'] = id
    script['inline'] = "ctx._source.added = params.added"
    params['added'] = added
    script['params'] = params
    update['script'] = script

    return update

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
