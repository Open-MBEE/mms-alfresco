import psycopg2
import sys
import getpass

'''
Created on Sept 25, 2017

@author: Laura Mann

Removes the edges of holding bin and orgs.

'''


def main(args):  # dbname, host, user
    print("You're gonna need the database password!")
    password = getpass.getpass()
    projects = get_projects(args[1], args[2], args[3], password)
    for project in projects:
        delete_orgs(project[1], project[0], args[3], password)


def get_projects(dbname, host, user, password):
    projects = []
    conn = None
    try:
        conn = psycopg2.connect("dbname=" + dbname + " user=" + user + "host=" + host + " password=" + password)
        print("you're connected to ")
        print(dbname)
        cur = conn.cursor()
        cur.execute("""select location, projectid from projects""")
        rows = cur.fetchall()
        for row in rows:
            suffix = row[0]
            projects.append((suffix[18:-6], "_" + row[1]))
    except psycopg2.DatabaseError, e:
        print('Error %s' % e)
        print("I'm sorry Dave I'm afraid I can't do that.  I am unable to connect to the database.")
        sys.exit(1)
    finally:
        if conn is not None:
            conn.close()
    return projects


def delete_orgs(dbname, host, user, password):
    # CASE 1: {orgid}      (nodetype is 2)
    orgNames = None
    orgId = None
    conn = None
    try:
        conn = psycopg2.connect("dbname=" + dbname + " user=" + user + "host=" + host + " password=" + password)
        print("you're connected to ")
        print(dbname)
        cur = conn.cursor()
        cur.execute("""select id, sysmlid from nodes where nodetype = 2""")
        rows = cur.fetchall()
        for row in rows:
            orgId = str(row[0])
            orgNames = str(row[1])
        if orgNames is not None:
            print(orgId)
            print(orgNames)
            cur.execute('DELETE FROM edges WHERE parent = %s', [orgId])
        # CASE 2: holding_bin or holding_bin_{orgid}
        holding_bins = [orgNames, 'holding_bin', 'holding_bin_' + orgNames]
        for bin in holding_bins:
            cur.execute('DELETE FROM nodes WHERE sysmlid = %s', [bin])
        conn.commit()
    except psycopg2.DatabaseError, e:
        if conn is not None:
            conn.rollback()
        print("I'm sorry Dave I'm afraid I can't do that.  I am unable to connect to the database.")
        print('Error %s' % e)
        sys.exit(1)
    finally:
        if conn is not None:
            conn.close()


if __name__ == '__main__':
    if len(sys.argv) < 4:
        print "Not enough arguments, need dbname, host, dbuser"
    else:
        main(sys.argv)
