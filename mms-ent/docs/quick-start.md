### Setup MMS and VE on a single machine

#### Prerequisites

- familiarity of commandline 
- a machine running CentOS 7.x with root access
- 16 GB of RAM or more

#### 1. Update OS and install OpenJDK
1. Update OS as root:

    `yum update -y`

1. Install OpenJDK:

    `yum install -y java-1.8.0-openjdk.x86_64 java-1.8.0-openjdk-devel.x86_64`

#### 2. Install and configure ElasticSearch
1. Run the following commands as root:
    
    `rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch`
    
    Create file /etc/yum.repos.d/elasticsearch.repo as follows
    ```
    [elasticsearch-5.x]
    name=Elasticsearch repository for 5.x packages
    baseurl=https://artifacts.elastic.co/packages/5.x/yum
    gpgcheck=1
    gpgkey=https://artifacts.elastic.co/GPG-KEY-elasticsearch
    enabled=1
    autorefresh=1
    type=rpm-md
    ```

    `yum install -y elasticsearch`
    
2. Edit elasticsearch config located at `/etc/elasticsearch/elasticsearch.yml`.  
    * Set and uncomment a cluster.name (same for all) and a node.name (unique for each cluster).  
    * Set path.data to the location where you mount the volume ES, (i.e. /mnt/elasticsearch this must be set in etc/fstab and mounted.
    * `chown -R elasticsearch:elasticsearch /mnt/elasticsearch/`
    
3. Enable the elasticsearch service

    `systemctl enable elasticsearch.service`
    
4. Start the elasticsearch service

    `systemctl start elasticsearch.service`
    
5. Check the state of the elasticsearch service
    
    `curl -XGET 'http://localhost:9200/_cluster/state?pretty'`

#### 3. Installing Alfresco on Tomcat

See: [Alfresco Documentation on Installation](https://docs.alfresco.com/5.1/concepts/master-ch-install.html)
* To use the simple installer, get the installer from [here](https://community.alfresco.com/docs/DOC-6296-community-file-list-201605-ga/)
* The alfresco installer will handle all configurations that need to be set via command prompts, which simplifies installation.

#### 4. Configure Postgresql
* Use the full path for psql
* Connect to the PostgreSQL server and:
    * Create a `mms` user (will be referenced by pg.user in `mms.properties` file) with role of `CREATEDB`
       * Ensure you set a password (will be referenced by pg.pass)
    * Create a `mms` database (will be referenced by pg.name)

#### 5. Upload Schemas for ElasticSearch and Postgres
1. download [mapping_template.json](https://raw.githubusercontent.com/Open-MBEE/mms-alfresco/develop/mms-ent/repo-amp/src/main/resources/mapping_template.json), [mms_mappings.sh](https://raw.githubusercontent.com/Open-MBEE/mms-alfresco/develop/mms-ent/repo-amp/src/main/resources/mms_mappings.sh) and [mms.sql](https://raw.githubusercontent.com/Open-MBEE/mms-alfresco/develop/mms-ent/repo-amp/src/main/resources/mms.sql) and save all to the same directory
1.  Run `mms_mappings.sh`

    `bash mms_mappings.sh`

1.  Run `mms.sql` on your instance of Postgres:
    
    `psql -h localhost -p 5432 -U mms -d mms -v schema=public < mms.sql`
       
#### 6. Install ActiveMQ (Optional, only used for VE realtime updates)
1. Get activemq binaries:
    
    `wget http://www.us.apache.org/dist/activemq/5.12.1/apache-activemq-5.12.1-bin.tar.gz`

    `tar -zxvf apache-activemq*.tar.gz`

    `mv apache-activemq-5.12.1 /opt`

    `ln -s /opt/apache-activemq-5.12.1 /opt/activemq`

    `adduser --system activemq`

    `chown -R activemq: /opt/activemq`
    
1. Start activemq:

    `/opt/activemq/bin/activemq`
    
1. You may want to run activemq as a service.
       
#### 7. Installing MMS
1. Grab the latest [mms-amp](https://github.com/Open-MBEE/mms-alfresco/releases/download/3.4.2/mms-amp-3.4.2.amp) and [mms-share-amp](https://github.com/Open-MBEE/mms-alfresco/releases/download/3.4.2/mms-share-amp-3.4.2.amp) from the [github release page](https://github.com/Open-MBEE/mms/releases):

1. Change to the tomcat webapps directory, where alfresco.war and share.war is located, then do

    `java -jar ../bin/alfresco-mmt.jar install $YOUR_PATH/mms-amp.amp alfresco.war -force`

    `java -jar ../bin/alfresco-mmt.jar install $YOUR_PATH/mms-share-amp.amp share.war -force`
        
1. Create and edit the mms.properties file in the $TOMCAT_HOME/shared/classes directory (You can copy [mms.properties.example](https://raw.githubusercontent.com/Open-MBEE/mms-alfresco/develop/mms-ent/mms.properties.example) and update as appropriate)
        
    * app.user and app.pass should be set to the alfresco admin user that you set up
    
#### 8. Install VE
1. Get the latest VE release [zip](https://github.com/Open-MBEE/ve/releases/download/3.6.1/ve-3.6.1.zip) from [github release page](https://github.com/Open-MBEE/ve/releases)

1. unzip the release into the tomcat alfresco webapp directory as a folder called `ve`
    
    `$TOMCAT_HOME/webapps/alfresco/ve/**`

#### 9. Start tomcat

`systemctl start alfresco`

After Alfresco is started, the API can be used to create orgs and projects.

The ve login url would be [http://localhost:8080/alfresco/ve/mms.html](http://localhost:8080/alfresco/ve/mms.html)
