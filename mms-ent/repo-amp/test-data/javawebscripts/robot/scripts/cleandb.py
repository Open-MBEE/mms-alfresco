import psycopg2


def clean_db_projects():
    mms_conn = psycopg2.connect(dbname='mms', user='mmsuser', host='/tmp')
    cur = mms_conn.cursor()

    cur.execute("SELECT projectid FROM projects")
    projects = cur.fetchall()[0]

    mms_conn.set_isolation_level(0)

    for project_id in projects:
        try:
            cur.execute("DROP DATABASE _" + str(project_id))
        except:
            print("Unable to drop database with id {}".format(project_id))
    mms_conn.commit()
    cur.close()
    mms_conn.close()


if __name__ == "__main__":
    clean_db_projects()
