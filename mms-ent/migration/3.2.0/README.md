# D2 Migration Scripts

1. You should have Python 2.7 or higher installed on your server
2. Run `sudo pip install psycopg2`
3. Run `sudo pip install elasticsearch`

MMS 3.1 should be up as scripts are run as currently scripts 2 and 3 utilize the rest interface.

Scripts should be run preferably on the mms machine

Scripts must be run the following order:


### Database Update

Remove edges and nodes in project dbs that are no longer relevant (global holding bin, org holding bin, org).

1. Run `update_database.py [your_database_name] [your_db_host_name] [your_database_user_name]`
2. Connect to Alfresco's database and run these commands in order:
```
delete from alf_node_aspects where qname_id in (select id from alf_qname where ns_id = (select id from alf_namespace where uri = 'http://jpl.nasa.gov/model/view/1.0'));
delete from alf_node_properties where qname_id in (select id from alf_qname where ns_id = (select id from alf_namespace where uri = 'http://jpl.nasa.gov/model/view/1.0'));
delete from alf_qname where ns_id = (select id from alf_namespace where uri = 'http://jpl.nasa.gov/model/view/1.0');
delete from alf_namespace where uri = 'http://jpl.nasa.gov/model/view/1.0';
```

### Updating Commit, Project and Ref Elastic Obejcts with the _projectId key

2. Run `update_commits.py [elastic hostname] [mms host] [your_admin_username_for_alfresco]`

### Converting indexes from 'mms' to 'projectIds'

3. Run the template mapping script without the delete all indexes.  Make sure not to delete indexes if it's the same server. (in repo-amp/src/main/java/gov/nasa/jpl/view_repo/db, update the host if needed)
4. Run `convert_indexes.py [mms host] [your_admin_username_for_alfresco] [src elastic hostname] [destination_elastich_hostname]`

### Notes on Failure

Update the base_url in scripts if necessary.

If the converting_indexes.py script fails you'll either have to remove all the indexes and start over or remove the last project it ran and edit the script to start there.  To remove a project run `curl -XDELETE 'http://[hostname]:9200/[projectId]'` where projectId is the lowercased project id.

If a timeout error occurs increase the timeout.


