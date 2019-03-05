# Model Management System
[![Language Grade: Java](https://img.shields.io/lgtm/grade/java/g/Open-MBEE/mms.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/Open-MBEE/mms/context:java) [![CircleCI](https://circleci.com/gh/Open-MBEE/mms.svg?style=svg)](https://circleci.com/gh/Open-MBEE/mms)

The MMS AMP is a hosted application run atop the [community version of an Alfresco Enterprise Content Management Server](https://docs.alfresco.com/community/concepts/welcome-infocenter_community.html).  

**NOTE 1: AMPs for Alfresco as built from this maven project are meant to be run by [Alfresco Community Edition v5.1.g (AKA 201605-GA)](https://download.alfresco.com/release/community/201605-build-00010/alfresco-community-distribution-201605.zip)**

Per [ Alfresco's documentation on Modules](https://docs.alfresco.com/5.0/concepts/dev-extensions-modules-intro.html), Alfresco Module Packages (AMPs) are installed using the Module Management Tool [MMT](https://docs.alfresco.com/5.0/concepts/dev-extensions-modules-management-tool.html) jar file.  The latest version of the two AMPs you will be "exploding" into the "alfresco" and "share" WARs resident within the embedded Tomcat server are available via the download links leading to the two interdependent portions named "mms-repo.amp" and "mms-share.amp", relating to "alfresco.war" and "share.war", respectively.

[ ![Download](https://api.bintray.com/packages/openmbee/maven/mms-amp/images/download.svg) ](https://bintray.com/openmbee/maven/mms-amp/_latestVersion) of the alfresco WAR related portion of the "MMS" Alfresco module.

[ ![Download](https://api.bintray.com/packages/openmbee/maven/mms-share-amp/images/download.svg)](https://bintray.com/openmbee/maven/mms-share-amp/_latestVersion) of the share WAR related portion of the "MMS" Alfresco Module.

In tandem with the EMS-Webapp (colloquially known as the View Editor), and the Magicdraw Development Kit (MDK) for MagicDraw client and Teamwork Cloud users; this github repo serves as a one-stop shop to set up the Model Management Server per the 
[MMS-MDK-VE Compatibility Matrix](https://github.com/Open-MBEE/open-mbee.github.io/blob/master/compat%20matrix.pdf).



## Developer Setup

### Dependencies
* ElasticSearch 5.x (Up to 5.5)
* PostgreSQL 9.x (Up to 9.4 is using PostgreSQL for Alfresco)

### Optional Dependencies
* ActiveMQ 5.X

### 1a. Using Intellij
* Open Project with root of 'mms'
* Import Maven Project
* Open Project Structure and Import Module "mms-ent" and set to find projects recursively

### 1b. Import Project from git repo to Eclipse
*  **Eclipse** > **File** > **Import** > **General** > **Existing Projects into Workspace**
*  Set 'Root Directory' as the path to mms e.g. **Browse** to `$HOME/workspace/mms`
*  In the 'Projects' list, you should see all poms. Click **Finish**

### 1c. Import Maven Project into Eclipse
* **Eclipse** > **File** > **Import** > **Maven** > **Existing Maven Projects**
* Set 'Root Directory' as the path to mms e.g. **Browse** to `$HOME/workspace/mms/mms-ent`
* In the 'Projects' list, you should see all poms. Click **Finish**

### 2. Configure Eclipse to use Maven 3.X.X
*   **Eclipse** > **Window** > **Preferences** > **Maven** > **Installation**
 *  Toggle Maven 3.X.X
    *   If Maven 3.X.X is not listed, download and install it.
    *   On a Mac, install it at /usr/local/Cellar/maven.
    *   On a Linux, anywhere in your $PATH.
*   Return to **Eclipse** > **Window** > **Preferences** > **Maven** > **Installation**
*   Choose **Add...**
    *   Browse and select Maven 3.X.X installed location.
    *  Location is the maven home that you can get by running the newly installed maven, with mvn -V

### 3. Configure Run Configuration**
*   Select **mms-ent** project
*   From menu bar, choose **Run** > **Run Configurations**
*   Right-click **Maven Build** > **New**
    *   Enter `mms` for Name textbox
    *   At **Main** tab
        *   Enter `${project_loc}` or `${workspace_loc}` for Base Directory textbox
        *   Enter `install` for Goals textbox
        *   Enter `run` for Profiles textbox
        *   Select Maven 3.X.X (whatever you chose to setup in step 2) for Maven Runtime
    *   At **JRE** tab
        *   Select **Java 8** for JRE.
        *   If it's not installed, download and install Java 8. Afterward, return to here and select Java 8.

## Install Dependencies
### 1. Install and Configure ElasticSearch
*   Download ElasticSearch 5.X
*   Install ElasticSearch
*   Start ElasticSearch then run `mms-ent/repo-amp/src/main/resources/mms-mappings.sh`

### 2. Install and Configure PostgreSQL
*   Download PostgreSQL 9.x
    * If using PostgreSQL as the database for Alfresco, PostgreSQL 9.4 is the latest supported version
*   Install PostgreSQL
*   Start PostgreSQL server
*   Connect to the PostgreSQL server and:
    *  Create a `mms` user (referenced by pg.user in your `mms-ent/mms.properties` file)
       *  Ensure you set a password (referenced by pg.pass)
    *  Create a `mms` database ( referenced by pg.name)
*   Execute `mms-ent/repo-amp/src/main/resources/mms.sql`
    * windows CMD e.g.: `psql -h localhost -p 5432 -U mms -d mms -v schema=public < C:\path\to\mms\repo\mms.sql`

### 3. Install and Configure ActiveMQ
*   Download ActiveMQ 5.X
*   Install ActiveMQ
*   Start ActiveMQ service

## Running
### 1a. Running Alfresco
1. Select file menu **Run** > **Run Configurations**
2. Expand **Maven Build**
3. Select **mms**
    1. Click **Run** button
    * If you get error:-Dmaven.multiModuleProjectDirectory system property is not set. Check $M2_HOME environment variable and mvn script match. Goto **Window -> Preference -> Java -> Installed JREs -> Edit -> Default VM arguments**  _set -Dmaven.multiModuleProjectDirectory=$M2_HOME_

### 1b. Running Alfresco
1. From mms-ent directory, either run `clean-run.sh`, `run.sh`, or `./mvnw install -Prun -Ddependency.surf.version=6.3`

### 2. Testing Alfresco
1. Enter http://localhost:8080/share/ at a browser's url address textbox.
2. Enter **admin** for user name
3. Enter **admin** for password

## Design Documentation
### 1. MMS using ElasticSearch and PostgreSQL

-----

General Design

    +----------------+   \
    | REST API Layer |    \
    |----------------|     \
    |   WebScripts   |      MMS
    |----------------|     /
    |Storage | Layer |    /
    +----------------+   /
      /\         /\
      ||         ||
      \/         \/
    +-----+    +----+
    | RDB |    | ES |
    +-----+    +----+
    (Graph)    (Data)

### 2. Configuration Schema
* Global PG database called 'mms' holds configuration information per project
* Contains Org, Project, and DB Location information

### 3. Graph Database
* Each project has it's own database configured in the `mms` database
* All graph related information stored in relational database
* Schema defined in mms.sql
    * Nodes
    * Edges
    * Edge types
    * Node types
    * Commits
    * Refs
* Functions defined that are recursive for getting parents, children etc.

* All access to the graph is done via the PostgresHelper that contains functions to interface with the database

* The graph should never be manipulated directly from anywhere else


The nodes in the graph contain pointers to ElasticSearch documents which contain the real information
    * Same goes for other things such as configurations or commits

Each of these pointers is the “latest one”
    * Because for a given SYSMLID there can be multiple documents for each version of that node

The history of each node can be retrieved via a query to ElasticSearch (see ElasticHelper.java)

### 4. Graph Schema
Nodes and edges both have a type associated with them
The type must exist in the NodeTypes and EdgeTypes tables

* Correspondingly, the types also exist in the PostgresHelper code
* Each node can be assigned a particular type, by default it is just element
* Each edge can be assigned a particular type, containment being the basic type

Some assertions about the graph:

* It is always the case that each node in the graph has a single containment parent
    * If you have multiple, something wrong happened!
* Multiple root parents are only possible for not containment type edges
* There should never be any orphan nodes in the graph
    * Always have either children or parents
* All elastic references in the graph must exist in ES

### 5. Graph
Structure of the graph for each "project"

               +---------+
            +--| Commits |
    +===+   |  +---------+          +-------------+    +----------+
    | P |   |                   +---| Holding bin |----| Elements |
    | R |   |  +-------+        |   +-------------+    +----------+
    | O |   +--| Nodes |--------|
    | J |   |  +-------+        |   +----------+
    | E |   |                   +---| Elements |
    | C | --|  +-------+            +----------+
    | T |   +--| Edges |
    |   |   |  +-------+
    |   |   |
    |   |   |  +----------------+
    +===+   +--| Refs           |
               +----------------+

### 6. Example webscript in new world: modelpost

Create instance of EmsNodeUtil
Get all other relevant information, validation, etc. form request
Figure out if the elements are to go to the holding bin or not
Calculate the qualified name and ID information for each node
Add metadata for each element
Store elements in ES
Update graph
Create JSON response and return


### 7. Example webscript in new world: modelget

Create instance of EmsNodeUtil
Get all other relevant information, validation, etc. form request
Access PG to get the ElasticSearch IDs for all documents that we are interested in
Access ES via ElasticHelper and get those IDs
Create JSON response and return


### 8. General Pattern for WebScripts
Create instance of EmsNodeUtil
Get all other relevant information, validation, etc. form request
Understand how to:

* Add information to ES
* Update the graph with the corresponding information from ES

> All the work is to be done here for each webscript. The pre and post should always be the same.

Create JSON response and return

## Testing
### Initializing Organizations, Projects, and test elements

Use the following curl commands to post an initial organization + project:
```
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin --data '{"orgs": [{"id": "vetest", "name": "vetest"}]}' -X POST "http://localhost:8080/alfresco/service/orgs"
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin --data '{"projects": [{"id": "123456","name": "vetest","type": "Project"}]}' -X POST "http://localhost:8080/alfresco/service/orgs/vetest/projects"
```

Then you can post some elements. For convenience, there is a json file in `runner/src/test/robotframework/JsonData`. Using the project from above:
```
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin --data @JsonData/PostNewElements.json -X POST "http://localhost:8080/alfresco/service/projects/123456/refs/master/elements"
```

Make sure the elements went in:
```
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin -X GET "http://localhost:8080/alfresco/service/projects/123456/refs/master/elements/123456?depth=-1"
```

### Robotframework test suite
Robot tests can be run with the following maven profiles in the mms-ent directory:
```
./mvnw install -Prun,robot-tests
```
Please note that tests should be run on a clean instance, therefore, it may be helpful to run clean.sh before running the tests

The Robotframework tests require the 'requests' and 'robotframework-requests' python modules. Install it as follows:
```
pip install --target=runner/src/test/robotframework/libraries requests
pip install --target=runner/src/test/robotframework/libraries robotframework-requests
```
OR:
```
pip install --target=$HOME/.m2/repository/org/robotframework/robotframework/{ROBOTPLUGINVERSION}/Lib requests
pip install --target=$HOME/.m2/repository/org/robotframework/robotframework/{ROBOTPLUGINVERSION}/Lib robotframework-requests
```

### Changing debug levels on the fly

If you need to change debug levels on the fly, use the following endpoint.

```
alfresco/service/loglevel
```

It takes as input JSON that specifies the classes and the log levels. For example:

```
[
  {
    "classname": "gov.nasa.jpl.view_repo.webscripts.ModelGet",
    "loglevel": "DEBUG"
  }
]
```

## API Documentation
API Documentation is located at the following endpoints:

Swagger CodeGen:
```
alfresco/mms/index.html
```

Swagger UI:
```
alfresco/mms/swagger-ui/index.html
```

Swagger YAML file:
```
alfresco/mms/mms.swagger.yaml
```

## Migrations after 3.2
For versions after 3.2, most notably 3.3.0, an automigration step has been included to run necessary migrations automatically during the initial startup of alfresco after the upgrade. Should this migration fail for any reason, you can trigger the migration manually by accessing the following endpoint:
```
alfresco/service/migrate/{targetVersion}
```

where {targetVersion} is the version that you are upgrading to. Example:
```
alfresco/service/migrate/3.3.0
```

This operation is idempotent and can be safely run multiple times.
