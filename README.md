# mms-all-in-one
**Alfresco version 5.0.1**

# Initial Setup
## 1a. Import Project from git repo to Eclipse
*  **Eclipse** > **File** > **Import** > **General** > **Existing Projects into Workspace**
*  Set 'Root Directory' as the path to mms-all-in-one e.g. **Browse** to `$HOME/workspace/mms-all-in-one`
*  In the 'Projects' list, you should see all poms. Click **Finish**

## 1b. Import Maven Project into Eclipse
* **Eclipse** > **File** > **Import** > **Maven** > **Existing Maven Projects**
* Set 'Root Directory' as the path to mms-all-in-one e.g. **Browse** to `$HOME/workspace/mms-all-in-one/mms-ent`
* In the 'Projects' list, you should see all poms. Click **Finish**

## 1c. Using Intellij
* Open Project with root of 'mms-ent'
* Import Maven Project

## 2. Configure Eclipse to use Maven 3.X.X
*   **Eclipse** > **Window** > **Preferences** > **Maven** > **Installation**
 *  Toggle Maven 3.X.X
    *   If Maven 3.X.X is not listed, download and install it.
    *   On a Mac, install it at /usr/local/Cellar/maven.
    *   On a Linux, anywhere in your $PATH.
*   Return to **Eclipse** > **Window** > **Preferences** > **Maven** > **Installation**
*   Choose **Add...**
    *   Browse and select Maven 3.X.X installed location.
    *  Location is the maven home that you can get by running the newly installed maven, with mvn -V

## 3. Configure Run Configuration**
*   Select **mms-ent** project
*   From menu bar, choose **Run** > **Run Configurations**
*   Right-click **Maven Build** > **New**
    *   Enter `mms-all-in-one` for Name textbox
    *   At **Main** tab
            *   Enter `${project_loc}` or `${workspace_loc}` for Base Directory textbox
            *   Enter `install` for Goals textbox
            *   Enter `run` for Profiles textbox
            *   Select Maven 3.X.X (whatever you chose to setup in step 2) for Maven Runtime
    *   At **JRE** tab
            *   Select **Java 8** for JRE.
            *   If it's not installed, download and install Java 8. Afterward, return to here and select Java 8.

## 4. Install and Configure Elastic Search
*   Download Elasticsearch 5.X
*   Install Elasticsearch
*   Start Elasticsearch then run `mms-mappings.sh`

# Running
## 1. Running Alfresco
1. Select file menu **Run** > **Run Configurations**
2. Expand **Maven Build**
3. Select **mms-all-in-one**
    1. Click **Run** button
    * If you get error:-Dmaven.multiModuleProjectDirectory system propery is not set. Check $M2_HOME environment variable and mvn script match. Goto **Window -> Preference -> Java -> Installed JREs -> Edit -> Default VM arguments**  _set -Dmaven.multiModuleProjectDirectory=$M2_HOME_

## 2. Testing Alfresco
1. Enter http://localhost:8080/share/ at a browser's url address textbox.
2. Enter **admin** for user name
3. Enter **admin** for password

# Design Documentation
## 1. MMS using ElaticSearch and PostgreSQL

-----

General Design

    +----------------+   \
    | REST API Layer |    \
    |----------------|      \
    |   WebScripts   |       MMS
    |----------------|       /
    |Storage | Layer |     /
    +----------------+   /
      /\         /\
      ||         ||
      \/         \/
    +-----+    +----+
    | RDB |    | ES |
    +-----+    +----+
    (Graph)    (Data)

## 2. Configuration Schema
* Global PG database called 'mms' holds configuration information per project
* Contains Org, Project, and DB Location information

## 3. Graph Database
* Each project has it's own database configured in the `mms` database
* All graph related information stored in relational database
* Schema defined in mms.sql
    * Nodes
    * Edges
    * Edge types
    * Node types
    * Commits
    * Configurations
* Functions defined that are recursive for getting parents, children etc.

* All access to the graph is done via the PostgresHelper that contains functions to interface with the database

* The graph should never be manipulated directly from anywhere else


The nodes in the graph contain pointers to ElasticSearch documents which contain the real information
    * Same goes for other things such as configurations or commits

Each of these pointers is the “latest one”
    * Because for a given SYSMLID there can be multiple documents for each version of that node

The history of each node can be retrieved via a query to ElasticSearch (see ElasticHelper.java)

## 4. Graph Schema
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

## 5. Graph
Structure of the graph for each “workspace”

               +---------+
            +--| Commits |
    +===+   |  +---------+          +-------------+    +----------+
    | W |   |                   +---| Holding bin |----| Elements |
    | O |   |  +-------+        |   +-------------+    +----------+
    | R |   +--| Nodes |--------|
    | K |   |  +-------+        |   +----------+
    | S |   |                   +---| Elements |
    | P | --|  +-------+            +----------+
    | A |   +--| Edges |
    | A |   |  +-------+
    | C |   |
    | E |   |  +----------------+
    +===+   +--| Configurations |
               +----------------+

## 6. Example webscript in new world: modelpost

Create instance of EmsNodeUtil
Get all other relevant information, validation, etc. form request
Figure out if the elements are to go to the holding bin or not
Calculate the qualified name and ID information for each node
Add metadata for each element
Store elements in ES
Update graph
Create JSON response and return


## 7. Example webscript in new world: modelget

Create instance of EmsNodeUtil
Get all other relevant information, validation, etc. form request
Access PG to get the ElasticSearch IDs for all documents that we are interested in
Access ES via ElasticHelper and get those IDs
Create JSON response and return


## 8. General Pattern for WebScripts
Create instance of EmsNodeUtil
Get all other relevant information, validation, etc. form request
Understand how to:

* Add information to ES
* Update the graph with the corresponding information from ES

> All the work is to be done here for each webscript. The pre and post should always be the same.

Create JSON response and return

# Testing
## Initializing Organizations, Projects, and test elements

Use the following curl commands to post an initial organization + project:
```
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin --data '{"orgs": [{"id": "vetest", "name": "vetest"}]}' -X POST "http://localhost:8080/alfresco/service/orgs"
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin --data '{"projects": [{"id": "123456","name": "vetest","type": "Project"}]}' -X POST "http://localhost:8080/alfresco/service/orgs/vetest/projects"
```

Then you can post some elements. For convenience, there is a json file in repo-amp/test-data/javawebscripts/JsonData. Using the project from above:
```
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin --data @JsonData/elementsNew.json -X POST "http://localhost:8080/alfresco/service/projects/123456/refs/master/elements"
```

Make sure the elements went in:
```
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin -X GET "http://localhost:8080/alfresco/service/projects/123456/refs/master/elements/123456?depth=-1"
```

For more testing, see repo-amp/test-data/javawebscripts/robot

## Changing debug levels on the fly

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
