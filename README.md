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
* Open Project
* Add git submodule to VCS Project Root

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
            *   Enter `run, enterprise, mbee-dev` for Profiles textbox
            *   Select Maven 3.X.X (whatever you chose to setup in step 2) for Maven Runtime
    *   At **JRE** tab
            *   Select **Java 8** for JRE.
            *   If it's not installed, download and install Java 8. Afterward, return to here and select Java 8.

## 4. Install and Configure Elastic Search
*   Download Elasticsearch 2.0.0
*   Install Elasticsearch
*   Start Elasticsearch then run `mms-elastic.sh`

# Running Alfresco
1. Select file menu **Run** > **Run Configurations**
2. Expand **Maven Build**
3. Select **mms-all-in-one**
    1. Click **Run** button
    * If you get error:-Dmaven.multiModuleProjectDirectory system propery is not set. Check $M2_HOME environment variable and mvn script match. Goto **Window -> Preference -> Java -> Installed JREs -> Edit -> Default VM arguments**  _set -Dmaven.multiModuleProjectDirectory=$M2_HOME_

# Testing Alfresco
1. Enter http://localhost:8080/share/ at a browser's url address textbox.
2. Enter **admin** for user name
3. Enter **admin** for password

# Setting up elastic

-----

## MMS using ElaticSearch and PostgreSQL

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

## Configuration Schema
* Global PG database called 'mms' holds configuration information per project
* Contains Org, Project, and DB Location information

## Graph
* Each project has it's own database configured in 'mms' database
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

## Graph Schema
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

## Graph
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


## NoSQL
Currently we are using ElasticSearch 2.0.0 (ES) for storing our node information
All elements are stored in a single index
All commits are saved in a different index (only for commits)

* We can combine them into one also. Currently no strong opinions either way

The mapping is stored in mms_mapping.json where we specify what all fields for elements are

* Certain fields are not to be “analyzed” by ES, or certain fields are to be treated differently etc. etc.

All access to ES is done via the ElasticHelper.java
This currently uses the connection factory that ES library JEST provides

* We can switch to a different library, which should be simple enough in the code. But, the issue may arise when dealing with Alfresco and the POM.XML file, and whether it will work with all the other depencencies etc.
The document for each node is slightly different now. It has two root objects: metadata and element, these are combined into one when returning to ViewEditor etc.

* __TODO__: consider changing ViewEditor and other clients to actually consume this directly for performance and simplicity/consisitency

### Document example in ElasticSearch
Metadata object introduced by the MMS middle layer to store information.

```json
{
   "metadata":{
      "creator":"admin",
      "qualifiedId":"europa/JW_TEST/dlam",
      "created":"2016-02-12T09:43:07.735-0800",
      "qualifiedName":"europa/123456/dlam",
      "modifier":"admin",
      "modified":"2016-02-12T09:43:07.735-0800"
   },
   "element":{
      "owner":"123456",
      "documentation":"to make compatible with value specs (this is just a Element)",
      "name":"dlam",
      "sysmlid":"dlam"
   }
}
```

Actual element data given to the mms, completely untouched.

```json
{
   "metadata":{
      "creator":"admin",
      "qualifiedId":"europa/JW_TEST/dlam",
      "created":"2016-02-12T09:43:07.735-0800",
      "qualifiedName":"europa/123456/dlam",
      "modifier":"admin",
      "modified":"2016-02-12T09:43:07.735-0800"
   },
   "element":{
      "owner":"123456",
      "documentation":"to make compatible with value specs (this is just a Element)",
      "name":"dlam",
      "sysmlid":"dlam"
   }
}
```
## Example webscript in new world: modelpost

Create instances of ElasticHelper and PostgresHelper
Get all other relevant information, validation, etc. form request
Figure out if the elements are to go to the holding bin or not
If owner is not found, we have a problem – BAIL
Calculate the qualified name and ID information for each node
Add metadata for each element
Store elements in ES
Update graph
Create JSON response and return


## Example webscript in new world: modelget

Create instances of ElasticHelper and PostgresHelper
Get all other relevant information, validation, etc. form request
Access PG to get the ElasticSearch IDs for all documents that we are interested in
Access ES via ElasticHelper and get those IDs
Create JSON response and return


## General Pattern for WebScripts
Create instances of ElasticHelper and PostgresHelper
Get all other relevant information, validation, etc. form request
Understand how to:

* Add information to ES
* Update the graph with the corresponding information from ES

> All the work is to be done here for each webscript. The pre and post should always be the same.

Create JSON response and return


## Search Using Elastic
Everything is a search in ES
All depends on the query that you provide
The query DSL is fully documented online
What we have used so far:

* Filtered queries
* Match queries
* Must queries
ES only provides a certain number of results, so you have to iterate through them

* Potentially may have to use streaming results in the future

For getting all elements, we actually break the input list and issue multiple queries!

## Elastic query dsl usage in mms example

```json
{
    "query": {
        "filtered": {
            "filter": {
                "term": {
                    "element.sysmlid": "%s"
                } } } },
    "sort": [
        {
            "metadata.modified": {
                "order": "desc"
            } } ] }

```

We use a filtered query to find the element that we are interested in
Note that the nested object “element” in each document can be accessed using dot notation
We further sort the results using the metadata.modified field in descending order so the most recent result is received first

* Metadata is the other object in the document


```json
{
    "query": {
        "terms": {
            "_id": [
                "%s"
            ]
        }
    }
}
```

A term query to get an array of elements where we know the specific Elastic ID


```json
{
    "query": {
        "query_string": {
            "query": "%s"
        }
    }
}
```
A basic search on all strings in the document database

This should be modified to include other filters etc.

### Some Finer points
ES supports types, which we are currently using
For each configuration/tag/workspace, we essentially copy the current state of the nodes,commits,edges tables

* Can this get expensive? I don’t suspect too bad. But depends.
* For example: a million nodes and edges cost us roughly 30 MB to store in the database. lets go conservative and say 100 MB. So we can save 10 configurations/tags/workspaces per 1 GB. Which seems very tractable…

## How to do commits
Store commit data in ES
Store reference in commits table for the workspace
Special commit types

* Branch: a marker to see who the parent branch is
* Merge: a marker to indicate that a different branch was merged in

Commit data contains: added, deleted, moved, updated
For each commit and state of graph, need to know how to reverse process

## How to do branching in past time
Give a state of workspace W, SW
Given a series of commits C0 … Cn
For each commit, reverse process state of workspace w.r.t. commit
At the end of the commits, you have a new graph that reflects the workspace at the time in point you intended
Now get data from ES etc.

## Test infrastructure
The test infrastructure should be changed so that it is not performing weak diffs for the test cases.
Instead using something like jq or a stronger more semantic  JSON diff would be better

* Does not depend on the order of the keys
* Does not depend on spaces etc. etc.

This should not take too long

This needs to be thought out a bit, but should be manageable

## Initializing Organizations, Projects, and test elements

Use the following curl commands to post an initial organization + project:
```
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin --data '{"elements": [{"sysmlId": "vetest", "name": "vetest"}]}' -X POST "http://localhost:8080/alfresco/service/orgs"
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin --data '{"elements": [{"sysmlId": "123456","name": "vetest","type": "Project"}]}' -X POST "http://localhost:8080/alfresco/service/orgs/vetest/projects"
```

Then you can post some elements. For convenience, there is a json file in repo-amp/test-data/javawebscripts/JsonData. Using the project from above:
```
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin --data @JsonData/elementsNew.json -X POST "http://localhost:8080/alfresco/service/projects/123456/refs/master/elements"
```

Make sure the elements went in:
```
curl -w "\n%{http_code}\n" -H "Content-Type: application/json" -u admin:admin -X GET "http://localhost:8080/alfresco/service/projects/123456/refs/master/elements/123456?depth=-1"
```

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
