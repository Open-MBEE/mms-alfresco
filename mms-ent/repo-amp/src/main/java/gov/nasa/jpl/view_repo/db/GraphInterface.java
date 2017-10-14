package gov.nasa.jpl.view_repo.db;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.postgresql.util.PSQLException;

public interface GraphInterface {
    String LASTCOMMIT = "lastCommit";
    String INITIALCOMMIT = "initialCommit";

    void connect();

    void connectConfig();

    void close();

    void closeConfig();

    void setWorkspace(EmsScriptNode workspace);

    void setWorkspace(String workspaceId);

    void setProject(String project);

    Savepoint startTransaction() throws SQLException;

    Savepoint startTransaction(String savePointName) throws SQLException;

    void commitTransaction() throws SQLException;

    void rollBackToSavepoint(Savepoint savepoint) throws SQLException;

    void execUpdate(String query) throws SQLException;

    int execUpdateWithCount(String query) throws SQLException;

    ResultSet execQuery(String query) throws SQLException;

    int insert(String table, Map<String, String> values) throws SQLException;

    void updateBySysmlIds(String table, String column, String value, List<String> sysmlIds) throws SQLException;

    void runBulkQueries(List<String> queries, boolean withResults) throws SQLException;

    void runBulkQueries(List<Map<String, String>> queries, String type) throws SQLException;

    List<EdgeTypes> getEdgeTypes();

    List<Map<String, String>> getOrganizations(String orgId);

    String getOrganizationFromProject(String projectId);

    List<Node> getSites(boolean sites, boolean sitepackages);

    List<Map<String, Object>> getProjects();

    List<Map<String, Object>> getProjects(String orgId);

    Map<String, Object> getProject(String projectId);

    List<Node> getNodesByType(DbNodeTypes type);

    List<Node> getAllNodes();

    List<Map<String, Object>> getAllNodesWithLastCommitTimestamp();

    boolean isMoved(String sysmlid, String owner);

    boolean isDeleted(String sysmlid);

    boolean sysmlIdExists(String sysmlid);

    boolean edgeExists(String parent, String child, DbEdgeTypes dbet);

    Node getNodeFromElasticId(String elasticId);

    List<String> getElasticIdsFromSysmlIds(List<String> sysmlids);

    List<String> getElasticIdsFromSysmlIds(List<String> sysmlids, boolean withDeleted);

    Node getNode(int id);

    Node getNodeFromSysmlId(String sysmlId);

    Node getNodeFromSysmlId(String sysmlId, boolean withDeleted);

    Set<String> getElasticIds();

    Set<String> getElasticIds(boolean withDeleted);

    String getElasticIdForCommit(String commitId);

    String getElasticIdFromSysmlId(String sysmlId);

    String insertCommit(String elasticId, DbCommitTypes type, String creator);

    int getHeadCommit();

    String getHeadCommitString();

    void insertNode(String elasticId, String sysmlId, DbNodeTypes type);

    String createInsertNodeQuery(List<Map<String, String>> nodes);

    String createUpdateNodeQuery(List<Map<String, String>> nodes);

    String createInsertEdgeQuery(List<Map<String, String>> edges);

    String createInsertEdgePropertyQuery(String parentSysmlId, String childSysmlId, DbEdgeTypes type, Map<String, String> properties);

    int updateNode(String sysmlid, Map<String, String> values);

    void deleteNode(String sysmlId);

    void insertEdge(String parentSysmlId, String childSysmlId, DbEdgeTypes edgeType);

    Map<String, String> getCommitAndTimestamp(String lookUp, String value);

    Long getTimestamp(String lookUp, String value);

    Map<String, Object> getCommit(String commitId);

    List<Map<String, String>> getAllCommits();

    boolean isInitialCommit();

    LinkedList<String> getRootParents(String sysmlId, DbEdgeTypes et);

    Set<Pair<String, String>> getImmediateParents(String sysmlId, DbEdgeTypes et);

    Map<String, Set<String>> getImmediateParentRoots(String sysmlId, DbEdgeTypes et);

    String getImmediateParentOfType(String sysmlId, DbEdgeTypes et, Set<DbNodeTypes> dnts);

    Set<String> getRootParents(String sysmlId, DbEdgeTypes et, int height);

    Set<String> getBranchParents(String refId);

    List<Pair<String, String>> getContainmentParents(String sysmlId, int height);

    Set<Pair<String, Integer>> getParentsOfType(String sysmlId, DbEdgeTypes dbet);

    String getGroup(String sysmlId);

    List<Pair<String, String>> getChildren(String sysmlId, DbEdgeTypes et, int depth);

    void deleteEdgesForNode(String sysmlId);

    void deleteEdgesForNode(String sysmlId, boolean child, DbEdgeTypes edgeType);

    void deleteEdges(String parentSysmlId, String childSysmlId, DbEdgeTypes dbet);

    int createOrganization(String orgId, String orgName) throws PSQLException;

    void createProjectDatabase(String projectId, String orgId, String name, String location);

    void addProjectMount(String projectId, String mountId);

    void createBranchFromWorkspace(String childWorkspaceName, String workspaceName, String elasticId, String commitId, boolean isTag);

    boolean isTag(String refId);

    void setAsTag(String refId);

    void deleteRef(String id);

    void deleteRefTables(String id);

    Pair<String, String> getRefElastic(String refId);

    Pair<String, Long> getParentRef(String refId);

    List<Pair<String, String>> getRefsElastic();

    List<Map<String, Object>> getRefsCommits(String refId);

    List<Map<String, Object>> getRefsCommits(String refId, int commitId);

    List<Pair<String, String>> getTags();

    void insertRef(String newWorkspaceId, String newWorkspaceName, int headCommit, String elasticId, boolean isTag);

    void updateRef(String refId, String refName, String elasticId, boolean isTag);

    void insertTag(String workspaceName, String workspaceId);

    List<String> filterNodesWithElastic(List<String> elasticrefs);

    List<String> filterNodesByWorkspaceWithElastic(List<String> elasticrefs, String workspace);

    List<String> filterNodesWithSysmlid(List<String> sysmlids);

    List<String> filterNodesByWorkspaceWithSysmlid(List<String> sysmlids, String workspace);

    List<String> filterNodesByWorkspace(List<String> sysmlids, String workspace, String column);

    boolean orgExists(String orgId);

    boolean siteExists(String siteName);

    boolean refExists(String refId);

    void dropDatabase(String databaseName);

    void deleteProjectFromProjectsTable(String projectId);

    enum DbEdgeTypes {
        CONTAINMENT(1), VIEW(2), TRANSCLUSION(3), CHILDVIEW(4);

        private final int id;

        DbEdgeTypes(int id) {
            this.id = id;
        }

        public int getValue() {
            return id;
        }
    }

    enum DbNodeTypes {
        ELEMENT(1), SITE(2), PROJECT(3), DOCUMENT(4), COMMENT(5), CONSTRAINT(6), INSTANCESPECIFICATION(7), OPERATION(
            8), PACKAGE(9), PROPERTY(10), PARAMETER(11), VIEW(12), VIEWPOINT(13), SITEANDPACKAGE(14), HOLDINGBIN(
            15), MOUNT(16);

        private final int id;

        public int getValue() {
            return id;
        }

        DbNodeTypes(int id) {
            this.id = id;
        }
    }

    enum DbCommitTypes {
        COMMIT(1), BRANCH(2), MERGE(3);

        private final int id;

        public int getValue() {
            return id;
        }

        DbCommitTypes(int id) {
            this.id = id;
        }
    }
}
