import urllib2
from datetime import datetime
import json
import sys
import util
from elasticsearch import Elasticsearch
from elasticsearch import helpers
from elasticsearch import ElasticsearchException
import logging

'''
Created on March 26, 2018

@author: Laura Mann

Searches through all commit objects, checks for duplicate ids, removes and transforms those records.
'''


def main(args):
    commits_missing_info = []
    es = Elasticsearch([{'host': 'localhost', 'port': '9200'}], timeout=300)
    # Get every dupe that isn't a intial commit
    dupes = {}
    for index in es.indices.get('*'):
        print("We working on dis index  " + index)
        first_page = es.search(
            index=index,
            doc_type='commit',
            scroll='2m',
            size=1000)
        dupes.update(find_dupes(first_page.get('hits').get('hits')))
        s_id = first_page['_scroll_id']
        # print(str(type(iterate_scroll(es, s_id))) +"   "+str(len(iterate_scroll(es, s_id))))
        iterate_scroll(es, s_id, dupes)
        print('Starting to remove dupes for project ' + index + ' There are this many dupes: ' + str(len(dupes)))
    print(str(len(dupes)) + " There are this many dupes in this org.")
    # print(dupes)
    # TODO: Count the actual length
    # count = sum(len(v) for v in dupes.itervalues())
    # print(count)
    if len(dupes) > 10000:
        print('The number of dupes was too large to update at once')
        sys.exit(0)
    # This updates all the projects with dupes for the entire org
    project_actions = []
    for commitId, dup in dupes.iteritems():
        new_added = find_correct_id(es, dup, commitId)
        # if new_added is None:
        #     commits_missing_info.append(commitId)
        #     continue
        # for d in dup:
        #     try:
        #         search_for_ids = es.get(
        #             index=d[0],
        #             doc_type='commit',
        #             id=commitId)
        #         added = search_for_ids['_source']['added']
        #         # Add entries that are not dupes
        #         for entry in added:
        #             if entry['id'] == d[1] and entry['_elasticId'] == d[2]:
        #                 continue
        #             else:
        #                 new_added.append(entry)
        #         # add the dupe once
        #         new_added.append({'id': d['id'], '_elasticId': d['_elasticId']})
        #     except ElasticsearchException as e:
        #         print("curl -X GET \'localhost:9200/"+d[0]+"/commit/"+commitId+"\'")
    #     project_actions.append(add_actions(new_added, commitId, 'commit', dup[0]))
    # helpers.bulk(es, project_actions)
    # # if some commits were skipped print them
    # if len(commits_missing_info) > 0:
    #     print_errors(commits_missing_info)
    # print('The number dupes after is :')
    # print(len(check_all(es)))


def print_errors(commits_missing):
    logging.basicConfig(filename=str(datetime.now().time()) + 'commit.log', level=logging.ERROR)
    for commit in commits_missing:
        logging.error('This dupe is missing info ' + commit[0] + ' in project ' + commit[1])


def iterate_scroll(es, scroll_id, dupes):
    iterate = es.scroll(scroll_id=scroll_id, scroll="2m")
    s_id = iterate['_scroll_id']
    hits = iterate.get('hits').get('hits')
    dupes.update(find_dupes(hits))
    if hits:
        iterate_scroll(es, s_id, dupes)
        # if temp is not None:
        #     dupes.update(temp)


def find_dupes(hits):
    dup_by_commit = {}
    for hit in hits:
        id = hit['_id']
        added = hit['_source']['added']
        if 'source' in hit['_source']:
            if hit['_source']['source'].lower() == 'magicdraw':
                continue
        elif len(added) > 5000:
            continue
        else:
            ids = []
            for entry in added:
                projectId = hit['_source']['_projectId'].lower()
                ids.append((projectId, entry['id'], entry['_elasticId']))
            dups = list(set([x for x in ids if ids.count(x) > 1]))
            if dups and dups is not None:
                dup_by_commit[id] = dups
    return dup_by_commit


def find_correct_id(es, d, commitId):
    update_added = []
    for dup in d:
        try:
            search_for_ids = es.get(
                index=dup[0],
                doc_type='element',
                id=dup[2])
            source = search_for_ids['_source']
            ass_obj, association_obj = get_association(es, d, source, commitId)
            app_obj = get_stereotype(es, d, source, commitId)
            if association_obj is not None:
                owned_obj = get_owned_end(es, d, source, association_obj, commitId)
            else:
                return None
            if ass_obj is not None and association_obj is not None:
                update_added.append(ass_obj)
            else:
                return None
            if app_obj is not None:
                update_added.append(app_obj)
            else:
                return None
            if owned_obj is not None:
                update_added.append(owned_obj)
            else:
                return None
        except ElasticsearchException as e:
            print(e)
            print("curl -X GET \'localhost:9200/"+dup[0]+"/element/"+dup[2]+"\'")
            sys.exit(0)
    return update_added


def get_association(es, d, source, commitId):
    association_obj = es.search(
        index=d[0],
        doc_type='element',
        body=define_query(commitId, source.get('associationId')))
    if association_obj is not None and len(association_obj.get('hits').get('hits')) > 0:
        return {'id': association_obj.get('hits').get('hits')[0].get('_source').get('id'),
                '_elasticId': association_obj.get('hits').get('hits')[0].get('_source').get(
                    '_elasticId')}, association_obj
    else:
        return None, None


def get_stereotype(es, d, source, commitId):
    appliedStereotypeInstanceId_obj = es.search(
        index=d[0],
        doc_type='element',
        body=define_query(commitId, source.get('appliedStereotypeInstanceId')))
    if appliedStereotypeInstanceId_obj is not None and len(appliedStereotypeInstanceId_obj.get('hits').get('hits')) > 0:
        return {'id': appliedStereotypeInstanceId_obj.get('hits').get('hits')[0].get('_source').get('id'),
                '_elasticId': appliedStereotypeInstanceId_obj.get('hits').get('hits')[0].get('_source').get(
                    '_elasticId')}
    else:
        return None


def get_owned_end(es, d, source, association_obj, commitId):
    ownedEndId_obj = es.search(
        index=d[0],
        doc_type='element',
        body=define_query(commitId, association_obj.get('hits').get('hits')[0].get('_source').get('ownedEndIds')[0]))
    if ownedEndId_obj is not None and len(ownedEndId_obj.get('hits').get('hits')) > 0:
        return {'id': ownedEndId_obj.get('hits').get('hits')[0].get('_source').get('id'),
                '_elasticId': ownedEndId_obj.get('hits').get('hits')[0].get('_source').get('_elasticId')}
    else:
        return None


def define_query(commitId, sysmlid):
    return {
        "query": {
            "bool": {
                "must": [
                    {"term": {"id": sysmlid}},
                    {"term": {"_commitId": commitId}}
                ]
            }
        }
    }


def add_actions(edit, id, type, index):
    update = {}
    added = {}
    update['_op_type'] = 'update'
    update['_index'] = index
    update['_type'] = type
    update['_id'] = id
    added['added'] = edit
    update['doc'] = added
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


def check_all(es):
    dupes = []
    for index in es.indices.get('*'):
        first_page = es.search(
            index=index,
            doc_type='commit',
            scroll='2m',
            size=1000)
        s_id = first_page['_scroll_id']
        iterate = es.scroll(scroll_id=s_id, scroll="2m")
        dupes = double_check(es, s_id)
    return dupes


def double_check(es, s_id):
    iterate = es.scroll(scroll_id=s_id, scroll="2m")
    s_id = iterate['_scroll_id']
    hits = iterate.get('hits').get('hits')
    result = find_dupes(hits)
    if hits:
        return result.update(double_check(es, s_id))
    else:
        return result


if __name__ == '__main__':
    main(sys.argv)
    # if len(sys.argv) < 4:
    #     print "You need to pass the elastic host, mms host, and mms admin username"
    # else:
    #     main(sys.argv)


# curl -XGET 'localhost:9200/_search?pretty' -H 'Content-Type: application/json' -d'
# {
#     "query": {
#         "bool": {
#             "must": [
#                 {"term": {"id": "blah"}},
#                 {"term": {"_commitId": "blah"}}
#             ]
#         }
#     }
# }'

# curl -X POST 'localhost:9200/project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63/commit/669964e1-9f47-4d90-8512-798b44cd1bd3/_update' -H 'Content-Type: application/json' -d '{"doc" : {"added" : []}}'

# curl -X GET 'localhost:9200/project-411c7459-4e09-4dda-b534-4b1fa7aaa140/commit/361169dc-25ec-416a-9cb8-d1fae420266f'
# print(json.dumps(ownedEndId_obj, indent=4, sort_keys=True))
# print('!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!')
# first_page = es.search(
#     index='project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63',
#     doc_type='commit',
#     scroll='2m',
#     size=10000)
# dupes = find_dupes(first_page.get('hits').get('hits'))
# s_id = first_page['_scroll_id']
# dupes = dupes + iterate_scroll(es, s_id)
# print(dupes)
# dupes = [('669964e1-9f47-4d90-8512-798b44cd1bd3', 'project-id_9_25_13_4_05_00_pm_52a679e5_1415678a941_56c_sscae_cmr_128_149_130_63', 'MMS_1519155626332_5bb79ad0-f61c-4210-9072-d94485061592', '118e64f3-c4af-4933-8f7e-c27efdb4d194')]
