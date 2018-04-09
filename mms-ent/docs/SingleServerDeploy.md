Install MMS on CentOS 7.x
===

### Update OS and install OpenJDK
1. Update OS as root:
    * `yum update -y`

2. Install OpenJDK:
    * `yum install -y java-1.8.0-openjdk.x86_64 java-1.8.0-openjdk-devel.x86_64`

### Install and configure ElasticSearch
1. Run the following commands as root:
    * `rpm --import https://artifacts.elastic.co/GPG-KEY-elasticsearch`
    * Create file /etc/yum.repos.d/elasticsearch.repo as follows
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
    * `yum install -y elasticsearch`
    
2. Edit elasticsearch config located at /etc/elasticsearch/elasticsearch.yml.  
    * Set and uncomment a cluster.name (same for all) and a node.name (unique for each cluster).  
    * Set path.data to the location where you mount the volume ES, (i.e. /mnt/elasticsearch this must be set in etc/fstab and mounted.
    * `chown -R elasticsearch:elasticsearch /mnt/elasticsearch/`
    
3. Enable the elasticsearch service
    * `systemctl enable elasticsearch.service`
    
4. Start the elasticsearch service
    * `systemctl start elasticsearch.service`
    
5. Check the state of the elasticsearch service
    * `curl -XGET 'http://localhost:9200/_cluster/state?pretty'`

## Install and configure Postgresql 9.3.x (Optional if using the Alfresco installer which includes Postgresql)
1. Run the following commands as root:
    * `yum -y https://yum.postgresql.org/9.3/redhat/rhel-7-x86_64/pgdg-centos93-9.3-3.noarch.rpm`
    * `yum -y install postgresql93 postgresql93-server postgresql93-contrib postgresql93-libs`
    * `systemctl enable postgresql-9.3`
    * `postgresql93-setup initdb`
    * `systemctl start postgresql-9.3`
    * Connect to the PostgreSQL server and:
    * Create a `mms` user (referenced by pg.user in your `mms-ent/mms.properties` file)
       * Ensure you set a password (referenced by pg.pass)
    * Create a `mms` database ( referenced by pg.name)

## Upload Schemas for ElasticSearch and Postgres on VM
1.  Run `mms_mappings.sh`  on each ElasticSearch instance
    * Execute `mms-ent/repo-amp/src/main/java/gov/nasa/jpl/view_repo/db/mms_mappings.sh`
       * e.g.: `sh mms-ent/repo-amp/src/main/java/gov/nasa/jpl/view_repo/db/mms_mappings.sh`

2.  Run `mms.sql` on your instance of Postgres:
    * Execute `mms-ent/repo-amp/src/main/java/gov/nasa/jpl/view_repo/db/mms.sql`
       * e.g.: `psql -h localhost -p 5432 -U mms -d mms -v schema=public < mms-ent/repo-amp/src/main/java/gov/nasa/jpl/view_repo/db/mms.sql`
       
## Install ActiveMQ (Optional, depending on MDK version)
1. Get activemq binaries:
    * `wget http://www.us.apache.org/dist/activemq/5.12.1/apache-activemq-5.12.1-bin.tar.gz`
    * `tar -zxvf apache-activemq*.tar.gz`
    * `mv apache-activemq-5.12.1 /opt`
    * `ln -s /opt/apache-activemq-5.12.1 /opt/activemq`
    * `adduser --system activemq`
    * `chown -R activemq: /opt/activemq`
    
2. Start activemq:
    * `/opt/activemq/bin/activemq`
    
3. You may want to run activemq as a service.
       
## Installing Alfresco on Tomcat

See: [Alfresco Documentation on Installation](https://docs.alfresco.com/5.2/concepts/master-ch-install.html)

## Installing MMS
1. Grab the latest mms-amp and mms-share-amp from the github release page:
    * `https://github.com/Open-MBEE/mms/releases`
2. Install MMS AMPs for Alfresco repository and share
    * Change to your tomcat webapps directory, where alfresco.war and share.war is located
        * `java -jar ../bin/alfresco-mmt.jar install $YOUR_PATH/mms-amp.amp alfresco.war -force`
        * `java -jar ../bin/alfresco-mmt.jar install $YOUR_PATH/mms-share-amp.amp share.war -force`
        
    * Create and edit the mms.properties file in the $TOMCAT_HOME/shared/classes directory (You can copy mms-ent/mms.properties.example)
    
3. Start tomcat
    * Run either one of these
        * `systemctl start tomcat`
        
        OR
        * `systemctl start alfresco` (If installed with the Alfresco Installer)
    
