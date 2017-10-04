# D2 Migration Scripts

1. You should have Python 2.7 or higher installed on your server
2. Run `sudo pip install psycopg2`
3. Run `sudo pip install elasticsearch`

Scripts must be run the following order:

### Database Update

We had to remove some of the edges being tracked in the graph.

1. Run `update_database.py [your_database_name] [your_ip_host_name] [your_database_user_name]`

### Updating Commit, Project and Ref Obejct with the _ProjectId key

2. Run `update_commits.py [your_admin_username_for_alfresco] [your_ip_host_name]`

### Converting indexes from 'mms' to 'projectIds'

3. Run the template mapping script without the delete all indexes.  Make sure not to delete indexes if it's the same server.
4. Run `convert_indexes.py [src_hostname] [your_admin_username_for_alfresco] [destination_ElasticSearch_hostname]`

### Notes on Failure

If the converting_indexes.py script fails you'll either have to remove all the indexes and start over or remove the last project it ran and edit the script to start there.  To remove a project run `curl -XDELETE 'http://[hostname]:9200/[projectId]'`.
