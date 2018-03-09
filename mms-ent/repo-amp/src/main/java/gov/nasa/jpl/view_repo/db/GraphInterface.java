package gov.nasa.jpl.view_repo.db;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.postgresql.util.PSQLException;

public interface GraphInterface {
    String LASTCOMMIT = "lastCommit";
    String INITIALCOMMIT = "initialCommit";
    String DELETED = "deleted";

    void connect();

    void close();

    void setWorkspace(EmsScriptNode workspace);

    void setWorkspace(String workspaceId);

    void setProject(String project);

    Savepoint startTransaction() throws SQLException;

    Savepoint startTransaction(String savePointName) throws SQLException;

    void commitTransaction() throws SQLException;

    void rollBackToSavepoint(Savepoint savepoint) throws SQLException;

    void execUpdate(String query) throws SQLException;

    ResultSet execQuery(String query) throws SQLException;

    int insert(String table, Map<String, Object> values) throws SQLException;

    List<Map<String, String>> getOrganizations(String orgId);

    String getOrganizationFromProject(String projectId);

    List<Node> getSites(boolean sites, boolean sitepackages);

    List<Map<String, Object>> getProjects();

    List<Map<String, Object>> getProjects(String orgId);

    Map<String, Object> getProject(String projectId);

    List<Node> getNodesByType(DbNodeTypes type);

    List<Map<String, Object>> getAllNodesWithLastCommitTimestamp();

    boolean isDeleted(String sysmlid);

    boolean sysmlIdExists(String sysmlid);

    boolean edgeExists(String parent, String child, DbEdgeTypes dbet);

    List<String> getElasticIdsFromSysmlIds(List<String> sysmlids, boolean withDeleted);

    Node getNodeFromSysmlId(String sysmlId);

    Node getNodeFromSysmlId(String sysmlId, boolean withDeleted);

    Set<String> getElasticIdsNodes();

    Set<String> getElasticIdsNodes(boolean withDeleted);

    String getElasticIdFromSysmlId(String sysmlId);

    String insertCommit(String elasticId, DbCommitTypes type, String creator);

    int getHeadCommit();

    String getHeadCommitString();

    void insertNode(String elasticId, String sysmlId, DbNodeTypes type);

    void deleteNode(String sysmlId);

    void insertEdge(String parentSysmlId, String childSysmlId, DbEdgeTypes edgeType);

    Map<String, String> getCommitAndTimestamp(String lookUp, String value);

    Long getTimestamp(String lookUp, String value);

    Map<String, Object> getCommit(String commitId);

    List<Map<String, String>> getAllCommits();

    Set<Pair<String, String>> getImmediateParents(String sysmlId, DbEdgeTypes et);

    String getImmediateParentOfType(String sysmlId, DbEdgeTypes et, Set<DbNodeTypes> dnts);

    Set<Pair<String, Integer>> getParentsOfType(String sysmlId, DbEdgeTypes dbet);

    String getGroup(String sysmlId);

    List<Pair<String, String>> getChildren(String sysmlId, DbEdgeTypes et, int depth);

    List<String> getGroupDocuments(String sysmlId, DbEdgeTypes et, int depth, DbNodeTypes nt);

    void deleteEdgesForNode(String sysmlId);

    void deleteEdgesForNode(String sysmlId, boolean child, DbEdgeTypes edgeType);

    int createOrganization(String orgId, String orgName) throws PSQLException;

    void createProjectDatabase(String projectId, String orgId, String name, String location);

    void createBranchFromWorkspace(String childWorkspaceName, String workspaceName, String elasticId, String commitId, boolean isTag);

    boolean isTag(String refId);

    void setAsTag(String refId);

    void deleteRef(String id);

    Pair<String, String> getRefElastic(String refId);

    Pair<String, Long> getParentRef(String refId);

    List<Pair<String, String>> getRefsElastic();

    List<Map<String, Object>> getRefsCommits(String refId);

    List<Map<String, Object>> getRefsCommits(String refId, int commitId);

    List<Pair<String, String>> getTags();

    void insertRef(String newWorkspaceId, String newWorkspaceName, int headCommit, String elasticId, boolean isTag);

    void updateRef(String refId, String refName, String elasticId, boolean isTag);

    boolean orgExists(String orgId);

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
