#
# Initializes cache data by doing a recursive get on all model elements in
# all workspaces
#
# TODO:
#    Do we want the curl output files?  Should we delete them at the end? 
#    Should we place them in a dedicated folder?
#

from regression_lib import *

##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
            
    thick_divider()

    # Changing curl user/password, ie "admin:admin":
    if len(sys.argv) > 2 and sys.argv[1] == "-user":
        set_curl_user(sys.argv[2])
    
    #     1.) Get all the workspaces
    #     2.) For each workspace get all the projects
    #     3.) For each workspace get all the elements using the project as the root
    
    print "Getting all workspaces \n"
    
    output_file_wsget = "initialize_cache_wsget"
    curl_cmd = create_curl_cmd(type="GET",base_url=BASE_URL_WS_NOBS,branch="")
    (status,out) = commands.getstatusoutput(curl_cmd+"> "+output_file_wsget)
    wsIds = []
    if status == 0 and out:

        file_orig = open(output_file_wsget, "r")
        out = file_orig.read()
        file_orig.close()
        
        output = get_json_output_no_status(out)
        
        if output:
            j = json.loads(output)
    
            if j and 'workspaces' in j and j['workspaces']:
                wsJson = j['workspaces']
                for ws in wsJson:
                    if ws['id']:
                        wsIds.append(ws['id'])
    else:
        print "!!! ERROR getting workspaces!\n"
                        
    print "Found workspaces: %s\n"%wsIds
    
    wsidCount = 0
    for wsid in wsIds:
        thin_divider()
        wsidCount += 1
        print "= Getting projects for workspace: %s (%d of %d)\n" % (wsid, wsidCount, len(wsIds))
        
        output_file_projectsget_base = "initialize_cache_projectsget"
        output_file_projectsget = output_file_projectsget_base+"_"+wsid
        curl_cmd = create_curl_cmd(type="GET",data="projects",base_url=BASE_URL_WS,
                                   branch="%s/"%wsid)
        (status,out) = commands.getstatusoutput(curl_cmd+"> "+output_file_projectsget)
        projectIds = []
        if status == 0 and out:
    
            file_orig = open(output_file_projectsget, "r")
            out = file_orig.read()
            file_orig.close()
            
            output = get_json_output_no_status(out)
            
            if output:
                j = json.loads(output)
        
                if j and 'elements' in j and j['elements']:
                    projectJson = j['elements']
                    for project in projectJson:
                        if project['sysmlid']:
                            projectIds.append(project['sysmlid'])
              
                    print "Found projects: %s\n"%projectIds

                    projectidCount = 0
                    for projectid in projectIds:
                        projectidCount += 1
                        print "=== Getting all elements for workspace: %s (%d of %d), project: %s (%d of %d)\n"%(wsid,wsidCount,len(wsIds), projectid,projectidCount,len(projectIds))
                        
                        curl_cmd = create_curl_cmd(type="GET",data="elements/%s?recurse=true"%projectid,
                                                   base_url=BASE_URL_WS,branch="%s/"%wsid)
                        print "Sending curl cmd: %s\n"%curl_cmd
                        (status,out) = commands.getstatusoutput(curl_cmd)
                        
                        if status != 0:
                            print "!!! ERROR sending curl cmd: %s\n"%curl_cmd
                            
        else:
            print "!!! ERROR getting projects for workspace: %s\n"%wsid
    
    thick_divider()

