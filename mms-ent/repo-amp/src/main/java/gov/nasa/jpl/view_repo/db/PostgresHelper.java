package gov.nasa.jpl.view_repo.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpl.view_repo.util.Sjm;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import org.postgresql.util.PSQLException;

public class PostgresHelper implements GraphInterface {
    static Logger logger = Logger.getLogger(PostgresHelper.class);

    private Connection conn;
    private Connection configConn;
    private String project;
    private Map<String, String> projectProperties = new HashMap<>();
    private String workspaceId;
    private Savepoint savePoint;


    public PostgresHelper() {
        setWorkspace("master");
    }

    public PostgresHelper(EmsScriptNode ref) {
        setWorkspace(ref);
    }

    public PostgresHelper(String refId) {
        setWorkspace(refId);
    }

    public void connect() {
        try {
            if (this.conn == null || this.conn.isClosed()) {
                this.conn = PostgresPool
                    .getInstance(this.projectProperties.get("location"), this.projectProperties.get("dbname"))
                    .getConnection();
            }
        } catch (SQLException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public void connectConfig() {
        try {
            if (this.configConn == null || this.configConn.isClosed()) {
                this.configConn =
                    PostgresPool.getInstance(EmsConfig.get("pg.host"), EmsConfig.get("pg.name")).getConnection();
            }
        } catch (SQLException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public void closeConfig() {
        try {
            if (configConn != null) {
                configConn.close();
            }
        } catch (SQLException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    private void getProjectProperties() {
        projectProperties.put("location", EmsConfig.get("pg.host"));
        projectProperties.put("dbname", "_" + project);
        connectConfig();
        try {
            PreparedStatement query =
                this.configConn.prepareStatement("SELECT location FROM projects WHERE projectId = ?");
            query.setString(1, project);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                if (!rs.getString(1).isEmpty()) {
                    projectProperties.put("location", rs.getString(1));
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }
    }

    public void setWorkspace(EmsScriptNode workspace) {
        String workspaceName = workspace == null ? "" : workspace.getId();
        setWorkspace(workspaceName);
    }

    public void setWorkspace(String workspaceId) {
        if (workspaceId == null || workspaceId.equals("master") || workspaceId.equals("null")) {
            this.workspaceId = "";
        } else {
            this.workspaceId = "";
            workspaceId = sanitizeRefId(workspaceId);
            try {
                // Try to check for either workspaceName or workspaceId
                connect();
                PreparedStatement query = this.conn.prepareStatement("SELECT refId FROM refs WHERE refId = ?");
                query.setString(1, workspaceId);
                ResultSet rs = query.executeQuery();
                if (rs.next()) {
                    this.workspaceId = rs.getString(1);
                }
            } catch (Exception e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            } finally {
                close();
            }

            if (this.workspaceId.equals("")) {
                try {
                    connect();
                    PreparedStatement nquery = this.conn.prepareStatement("SELECT refId FROM refs WHERE refName = ?");
                    nquery.setString(1, workspaceId);
                    ResultSet nrs = nquery.executeQuery();
                    if (nrs.next()) {
                        this.workspaceId = workspaceId;
                    }
                } catch (Exception e) {
                    logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                } finally {
                    close();
                }
            }
        }
    }

    public void setProject(String project) {
        this.project = project;
        getProjectProperties();
    }

    public Savepoint startTransaction() throws SQLException {
        return startTransaction(null);
    }

    public Savepoint startTransaction(String savePointName) throws SQLException {
        connect();
        this.conn.setAutoCommit(false);
        logger.debug("Starting transaction");
        if (savePointName != null) {
            this.savePoint = this.conn.setSavepoint(savePointName);
        } else {
            this.savePoint = this.conn.setSavepoint();
        }

        return this.savePoint;
    }

    public void commitTransaction() throws SQLException {
        try {
            if (!this.conn.getAutoCommit()) {
                logger.debug("Committing transaction");
                this.conn.commit();
            } else {
                logger.debug("Cannot commit, no transaction");
            }
            logger.debug("Transaction finished");
        } catch (SQLException e) {
            if (this.savePoint != null) {
                this.conn.rollback(this.savePoint);
                logger.warn(String.format("Transaction has been rolled back to savePoint: %s", this.savePoint));
            } else {
                this.conn.rollback();
                logger.warn("Transaction has been rolled back to save point");
            }
            if (e.iterator().hasNext()) {
                throw new SQLException(e.iterator().next());
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public void rollBackToSavepoint(Savepoint savepoint) throws SQLException {
        this.conn.rollback(savepoint);
    }

    public void execUpdate(String query) throws SQLException {
        logger.debug(String.format("Query: %s", query));
        connect();
        try {
            this.conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
    }

    public int execUpdateWithCount(String query) throws SQLException {
        logger.debug(String.format("Query: %s", query));
        int count = 0;

        connect();
        try {
            count = this.conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }

        return count;
    }

    public ResultSet execQuery(String query) throws SQLException {
        logger.debug(String.format("Query: %s", query));
        connect();
        ResultSet rs = null;
        try {
            rs = this.conn.createStatement().executeQuery(query);
        } catch (SQLException e) {
            if (logger.isDebugEnabled()) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
        return rs;
    }

    public int insert(String table, Map<String, String> values) throws SQLException {

        StringBuilder columns = new StringBuilder();
        StringBuilder vals = new StringBuilder();

        try {
            for (String col : values.keySet()) {
                columns.append(col).append(",");

                if (values.get(col) != null) {
                    vals.append("'").append(values.get(col)).append("',");
                } else {
                    vals.append(values.get(col)).append(",");
                }
            }

            columns.setLength(columns.length() - 1);
            vals.setLength(vals.length() - 1);

            String query = String
                .format("INSERT INTO \"%s\" (%s) VALUES (%s) RETURNING id", table, columns.toString(), vals.toString());

            logger.debug(String.format("Query: %s", query));
            execQuery(query);
            return 1;
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return -1;
    }

    public void updateBySysmlIds(String table, String column, String value, List<String> sysmlIds) throws SQLException {
        if (sysmlIds == null || sysmlIds.isEmpty()) {
            return;
        }
        int limit = Integer.parseInt(EmsConfig.get("pg.limit.insert"));
        List<String> queries = new ArrayList<>();
        String queryStarter =
            String.format("UPDATE \"%s\" SET %s = '%s' WHERE sysmlId IN ('", table + workspaceId, column, value);
        String query = queryStarter;
        for (int i = 0; i < sysmlIds.size(); i++) {
            query += sysmlIds.get(i) + "','";
            if (((i + 1) % limit) == 0 || i == (sysmlIds.size() - 1)) {
                query = query.substring(0, query.length() - 2) + ");";
                queries.add(query);
                query = queryStarter;
            }
        }
        runBulkQueries(queries, false);
    }

    public void runBulkQueries(List<String> queries, boolean withResults) throws SQLException {
        int limit = Integer.parseInt(EmsConfig.get("pg.limit.insert"));
        String queryCache = "";
        for (int i = 0; i < queries.size(); i++) {
            queryCache += queries.get(i);
            if (((i + 1) % limit) == 0 || i == (queries.size() - 1)) {
                String storedInsert = String.format("%s", queryCache);
                logger.debug(String.format("Query: %s", storedInsert));
                if (withResults) {
                    boolean rs = this.conn.createStatement().execute(storedInsert);
                } else {
                    this.conn.createStatement().executeUpdate(storedInsert);
                }
                queryCache = "";
            }
        }
    }

    public void runBulkQueries(List<Map<String, String>> queries, String type) throws SQLException {
        int limit = Integer.parseInt(EmsConfig.get("pg.limit.insert"));
        List<Map<String, String>> queryCache = new ArrayList<>();
        for (int i = 0; i < queries.size(); i++) {
            queryCache.add(queries.get(i));
            String storedInsert = "";
            if (((i + 1) % limit) == 0 || i == (queries.size() - 1)) {
                if (type.contains("nodes")) {
                    storedInsert = createInsertNodeQuery(queryCache);
                    logger.debug(String.format("Query: %s", storedInsert));
                } else if (type.contains("updates")) {
                    storedInsert = createUpdateNodeQuery(queryCache);
                    logger.debug(String.format("Query: %s", storedInsert));
                    this.conn.createStatement().executeUpdate(storedInsert);
                } else if (type.contains("edges")) {
                    storedInsert = createInsertEdgeQuery(queryCache);
                    logger.debug(String.format("Query: %s", storedInsert));
                }
                this.conn.createStatement().executeUpdate(storedInsert);
                queryCache = new ArrayList<>();
            }
        }
    }

    public List<EdgeTypes> getEdgeTypes() {
        List<EdgeTypes> result = new ArrayList<>();
        try {
            ResultSet rs = execQuery("SELECT * FROM edgeTypes");

            while (rs.next()) {
                result.add(new EdgeTypes(rs.getInt(1), rs.getString(2)));
            }

        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    private Node resultSetToNode(ResultSet rs) throws SQLException {
        return new Node(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6),
            rs.getBoolean(7));
    }

    public List<Map<String, String>> getOrganizations(String orgId) {
        List<Map<String, String>> result = new ArrayList<>();
        String query;
        if (orgId == null) {
            query = "SELECT id, orgId, orgName FROM organizations";
        } else {
            query = String.format("SELECT id, orgId, orgName FROM organizations WHERE orgId = '%s'", orgId);
        }
        try {
            connectConfig();
            ResultSet rs = this.configConn.createStatement().executeQuery(query);
            while (rs.next()) {
                Map<String, String> org = new HashMap<>();
                org.put("id", Integer.toString(rs.getInt(1)));
                org.put("orgId", rs.getString(2));
                org.put("orgName", rs.getString(3));
                result.add(org);
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        return result;
    }

    public String getOrganizationFromProject(String projectId) {
        try {
            connectConfig();
            PreparedStatement query = this.configConn.prepareStatement(
                "SELECT organizations.orgId FROM projects JOIN organizations ON projects.orgId = organizations.id WHERE projects.projectId = ?");
            query.setString(1, projectId);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        return null;
    }

    public List<Node> getSites(boolean sites, boolean sitepackages) {
        List<Node> result = new ArrayList<>();

        try {
            ResultSet rs = null;

            if (sites) {
                rs = execQuery("SELECT * FROM \"nodes" + workspaceId
                    + "\" WHERE nodetype = (SELECT id FROM nodetypes WHERE name = \'site\') AND deleted = false");

                while (rs.next()) {
                    result.add(resultSetToNode(rs));
                }
            }
            if (sitepackages) {
                rs = execQuery("SELECT * FROM \"nodes" + workspaceId
                    + "\" WHERE nodetype = (SELECT id FROM nodetypes WHERE name = \'siteandpackage\') AND deleted = false");

                while (rs.next()) {
                    result.add(resultSetToNode(rs));
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return result;
    }

    public List<Map<String, Object>> getProjects() {
        return getProjects(null);
    }

    public List<Map<String, Object>> getProjects(String orgId) {
        List<Map<String, Object>> result = new ArrayList<>();

        connectConfig();
        try {
            PreparedStatement query;
            if (orgId != null) {
                query = this.configConn.prepareStatement(
                    "SELECT projects.id, projectId, name, organizations.orgId FROM projects JOIN organizations ON organizations.id = projects.orgId WHERE projects.orgId = (SELECT id FROM organizations where orgId = ?)");
                query.setString(1, orgId);
            } else {
                query = this.configConn.prepareStatement(
                    "SELECT projects.id, projectId, name, organizations.orgId FROM projects JOIN organizations ON organizations.id = projects.orgId");
            }

            ResultSet rs = query.executeQuery();

            while (rs.next()) {
                Map<String, Object> project = new HashMap<>();
                project.put(Sjm.SYSMLID, rs.getString(2));
                project.put(Sjm.NAME, rs.getString(3));
                project.put("orgId", rs.getString(4));
                result.add(project);
            }

        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        return result;
    }

    public Map<String, Object> getProject(String projectId) {

        Map<String, Object> result = new HashMap<>();

        connectConfig();
        try {
            PreparedStatement query = this.configConn.prepareStatement(
                "SELECT projects.id, projectId, name, organizations.orgId FROM projects JOIN organizations ON organizations.id = projects.orgId WHERE projectId = ?");
            query.setString(1, projectId);

            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                result.put(Sjm.SYSMLID, rs.getString(2));
                result.put(Sjm.NAME, rs.getString(3));
                result.put("orgId", rs.getString(4));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        return result;
    }

    public List<Node> getNodesByType(DbNodeTypes type) {
        List<Node> result = new ArrayList<>();

        try {
            connect();
            PreparedStatement query =
                this.conn.prepareStatement("SELECT * FROM \"nodes" + workspaceId + "\" WHERE nodetype = ?");
            query.setInt(1, type.getValue());
            ResultSet rs = query.executeQuery();
            while (rs.next()) {
                result.add(resultSetToNode(rs));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return result;
    }

    public List<Node> getAllNodes() {
        List<Node> result = new ArrayList<>();

        try {
            connect();
            PreparedStatement query = this.conn
                .prepareStatement("SELECT * FROM \"nodes" + workspaceId + "\" WHERE initialcommit IS NOT NULL");
            ResultSet rs = query.executeQuery();
            while (rs.next()) {
                result.add(resultSetToNode(rs));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return result;
    }

    /**
     * Returns a modified version of all the nodes the database along with the timestamp of the last commit.
     *
     * @return List of Maps
     */
    public List<Map<String, Object>> getAllNodesWithLastCommitTimestamp() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            connect();
            ResultSet rs = execQuery(String.format(
                "SELECT nodes%1$s.id, nodes%1$s.elasticid, nodes%1$s.nodetype, nodes%1$s.sysmlid, "
                    + "nodes%1$s.lastcommit, nodes%1$s.initialcommit, nodes%1$s.deleted, commits.timestamp "
                    + "FROM nodes%1$s JOIN commits ON nodes%1$s.lastcommit = commits.elasticid "
                    + "WHERE initialcommit IS NOT NULL ORDER BY commits.timestamp;", workspaceId));

            while (rs.next()) {
                Map<String, Object> node = new HashMap<>();
                node.put(Sjm.ELASTICID, rs.getString(2));
                node.put(Sjm.SYSMLID, rs.getString(4));
                node.put(LASTCOMMIT, rs.getString(5));
                node.put(INITIALCOMMIT, rs.getString(6));
                node.put(Sjm.TIMESTAMP, rs.getTimestamp(8));
                result.add(node);
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return result;
    }

    public boolean isMoved(String sysmlid, String owner) {
        try {
            Set<Pair<String, String>> dbowner = getImmediateParents(sysmlid, DbEdgeTypes.CONTAINMENT);
            if (dbowner.size() == 0) {
                return false;
            }
            assert (dbowner.size() == 1);
            return !(new ArrayList<>(dbowner).get(0).first.equals(owner));
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return false;
    }

    public boolean isDeleted(String sysmlid) {
        try {
            connect();
            PreparedStatement query = this.conn
                .prepareStatement("SELECT id FROM \"nodes" + workspaceId + "\" WHERE sysmlid = ? AND deleted = true");
            query.setString(1, sysmlid);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return false;
    }

    public boolean sysmlIdExists(String sysmlid) {
        try {
            connect();
            PreparedStatement query =
                this.conn.prepareStatement("SELECT id FROM \"nodes" + workspaceId + "\" WHERE sysmlid = ?");
            query.setString(1, sysmlid);
            ResultSet rs = query.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return false;
    }

    public boolean edgeExists(String parent, String child, DbEdgeTypes dbet) {
        try {
            connect();
            PreparedStatement query = this.conn.prepareStatement(
                "SELECT id FROM \"edges" + workspaceId + "\" WHERE parent = (SELECT id FROM \"nodes" + workspaceId
                    + "\" WHERE sysmlid = ?) AND child = (SELECT id FROM \"nodes" + workspaceId
                    + "\" WHERE sysmlid = ?) AND edgetype = ?");
            query.setString(1, parent);
            query.setString(2, child);
            query.setInt(3, dbet.getValue());
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return false;
    }

    public Node getNodeFromElasticId(String elasticId) {
        try {
            ResultSet rs =
                execQuery("SELECT * FROM \"nodes" + workspaceId + "\" WHERE elasticId = '" + elasticId + "'");

            if (rs.next()) {
                return new Node(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getBoolean(7));
            } else
                return null;
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return null;
    }

    public List<String> getElasticIdsFromSysmlIds(List<String> sysmlids) {
        return getElasticIdsFromSysmlIds(sysmlids, false);
    }

    public List<String> getElasticIdsFromSysmlIds(List<String> sysmlids, boolean withDeleted) {
        List<String> elasticIds = new ArrayList<>();
        if (sysmlids == null || sysmlids.isEmpty())
            return elasticIds;

        try {
            String query = String.format("SELECT elasticid FROM \"nodes%s\" WHERE sysmlid IN (%s)", workspaceId,
                "'" + String.join("','", sysmlids) + "'");
            if (!withDeleted) {
                query += "AND deleted = false";
            }
            ResultSet rs = execQuery(query);
            while (rs.next()) {
                elasticIds.add(rs.getString(1));
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return elasticIds;
    }

    public Node getNode(int id) {
        try {
            ResultSet rs =
                execQuery("SELECT * FROM \"nodes" + workspaceId + "\" WHERE id = " + id + " AND deleted = false");
            if (rs.next()) {
                return new Node(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getBoolean(7));
            } else
                return null;
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return null;
    }

    public Node getNodeFromSysmlId(String sysmlId) {
        return getNodeFromSysmlId(sysmlId, false);
    }

    public Node getNodeFromSysmlId(String sysmlId, boolean withDeleted) {
        try {
            connect();
            PreparedStatement query;
            if (withDeleted) {
                query = this.conn.prepareStatement("SELECT * FROM \"nodes" + workspaceId + "\" WHERE sysmlId = ?");
                query.setString(1, sysmlId);
            } else {
                query = this.conn
                    .prepareStatement("SELECT * FROM \"nodes" + workspaceId + "\" WHERE sysmlId = ? AND deleted = ?");
                query.setString(1, sysmlId);
                query.setBoolean(2, false);
            }

            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                return new Node(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getBoolean(7));
            } else {
                return null;
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return null;
    }

    public Set<String> getElasticIds() {
        return getElasticIds(false);
    }

    public Set<String> getElasticIds(boolean withDeleted) {
        Set<String> elasticIds = new HashSet<>();
        try {
            String query;
            if (withDeleted) {
                query = String.format("SELECT elasticid FROM \"nodes%s\"", workspaceId);
            } else {
                query = String.format("SELECT elasticid FROM \"nodes%s\" WHERE deleted = false", workspaceId);
            }
            ResultSet rs = execQuery(query);
            while (rs.next()) {
                elasticIds.add(rs.getString(1));
            }
            elasticIds.remove("holding_bin"); //??
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return elasticIds;
    }

    public String getElasticIdForCommit(String commitId) {
        try {
            ResultSet rs = execQuery("SELECT elasticId FROM commits WHERE id = '" + commitId + "'");
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return null;
    }

    public String getElasticIdFromSysmlId(String sysmlId) {
        if (logger.isDebugEnabled())
            logger.debug("Getting ElasticId for: " + sysmlId);
        Node node = getNodeFromSysmlId(sysmlId);
        if (node != null) {
            return node.getElasticId();
        }

        return null;
    }

    public String insertCommit(String elasticId, DbCommitTypes type, String creator) {
        try {
            Map<String, String> map = new HashMap<>();
            // we can hard code the commit type here....but we should still store the integer value
            // from the DB in memory
            int parentId = getHeadCommit();
            map.put("elasticId", elasticId);
            map.put("commitType", Integer.toString(type.getValue()));
            map.put("refId", workspaceId);
            map.put("creator", creator);
            insert("commits", map);
            if (parentId > 0) {
                int childId = getHeadCommit();
                execUpdate(
                    String.format("INSERT INTO commitParent (child, parent) VALUES (%d, %d);", childId, parentId));
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return elasticId;
    }

    public int getHeadCommit() {
        try {
            ResultSet rs = execQuery(String
                .format("SELECT id FROM commits WHERE refId = '%s' ORDER BY timestamp DESC LIMIT 1", workspaceId));
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                rs = execQuery(String.format("SELECT parentcommit FROM refs WHERE refId = '%s'", workspaceId));
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return 0;
    }

    public String getHeadCommitString() {
        try {
            ResultSet rs = execQuery(String
                .format("SELECT elasticId FROM commits WHERE refId = '%s' ORDER BY timestamp DESC LIMIT 1",
                    workspaceId));
            if (rs.next()) {
                return rs.getString(1);
            } else {
                rs = this.conn.prepareStatement(String.format(
                    "SELECT commits.elasticid FROM refs LEFT JOIN commits ON refs.parentcommit = commits.id WHERE refs.refid = '%s'",
                    workspaceId)).executeQuery();
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return null;
    }

    // insert commit and insert commit edges as well
    public void insertNode(String elasticId, String sysmlId, DbNodeTypes type) {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("elasticId", elasticId);
            map.put("sysmlId", sysmlId);
            map.put("nodeType", Integer.toString(type.getValue()));
            insert("nodes" + workspaceId, map);
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public String createInsertNodeQuery(List<Map<String, String>> nodes) {
        String query = String
            .format("INSERT INTO \"nodes%s\" (elasticId, sysmlId, lastcommit, initialcommit, nodeType) VALUES ",
                workspaceId);
        for (Map<String, String> node : nodes) {
            query += String.format("('%s', '%s', '%s', '%s', '%s'),", node.get(Sjm.ELASTICID), node.get(Sjm.SYSMLID),
                node.get("lastcommit"), node.get(Sjm.ELASTICID), node.get("nodetype"));
        }
        query = query.substring(0, query.length() - 1) + ";";
        return query;
    }

    public String createUpdateNodeQuery(List<Map<String, String>> nodes) {
        String query = "";
        for (Map<String, String> node : nodes) {
            query += String.format(
                "UPDATE \"nodes%s\" SET elasticId = '%s', sysmlId = '%s', lastcommit = '%s', nodeType = '%s', deleted = %b WHERE sysmlId = '%s';",
                workspaceId, node.get(Sjm.ELASTICID), node.get(Sjm.SYSMLID), node.get("lastcommit"),
                node.get("nodetype"), Boolean.parseBoolean(node.get("deleted")), node.get(Sjm.SYSMLID));
        }
        return query;
    }

    public String createInsertEdgeQuery(List<Map<String, String>> edges) {
        int limit = Integer.parseInt(EmsConfig.get("pg.limit.select"));
        String query = String.format("INSERT INTO \"edges%s\" (parent, child, edgeType) VALUES ", workspaceId);
        List<String> values = new ArrayList<>();
        for (Map<String, String> edge : edges) {
            values.add("((SELECT id FROM \"nodes" + workspaceId + "\" WHERE sysmlid = '" + edge.get("parent")
                + "'), (SELECT id FROM \"nodes" + workspaceId + "\" WHERE sysmlid = '" + edge.get("child") + "'), "
                + edge.get("edgetype") + ")");
        }
        query += StringUtils.join(values, ",") + ";";
        return query;
    }

    public String createInsertEdgePropertyQuery(String parentSysmlId, String childSysmlId, DbEdgeTypes type,
        Map<String, String> properties) {
        String query = "";

        if (!properties.isEmpty()) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                query += String.format("SELECT insert_edge_property('%s', '%s', '%s', %d, '%s', '%s');", parentSysmlId,
                    childSysmlId, workspaceId, type.getValue(), entry.getKey(), entry.getValue());
            }
        }
        return query;
    }

    public int updateNode(String sysmlid, Map<String, String> values) {

        StringBuilder vals = new StringBuilder();

        try {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                vals.append(key).append(" = ");

                if (key.equals("nodetype")) {
                    vals.append(value).append(",");
                } else if (value != null) {
                    vals.append("'").append(value).append("',");
                } else {
                    vals.append(value).append(",");
                }
            }

            vals.setLength(vals.length() - 1);

            String query = String
                .format("UPDATE \"%s\" SET %s WHERE sysmlid = '%s'", "nodes" + workspaceId, vals.toString(), sysmlid);

            if (logger.isDebugEnabled()) {
                logger.debug("Query: " + query);
            }

            return execUpdateWithCount(query);
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return -1;
    }

    public void deleteNode(String sysmlId) {
        try {
            connect();
            PreparedStatement query =
                this.conn.prepareStatement("UPDATE \"nodes" + workspaceId + "\" SET deleted = ? WHERE sysmlid = ?");
            query.setBoolean(1, true);
            query.setString(2, sysmlId);
            query.execute();
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public void insertEdge(String parentSysmlId, String childSysmlId, DbEdgeTypes edgeType) {

        if (parentSysmlId == null || childSysmlId == null || parentSysmlId.isEmpty() || childSysmlId.isEmpty()) {
            logger.warn("Parent or child not found");
            logger.warn("parentSysmlId: " + parentSysmlId);
            logger.warn("childSysmlId: " + childSysmlId);
            return;
        }

        try {
            execQuery(
                "INSERT INTO \"edges" + workspaceId + "\" (parent, child, edgeType) VALUES ((SELECT id FROM \"nodes"
                    + workspaceId + "\" WHERE sysmlId = '" + parentSysmlId + "')," + "(SELECT id FROM \"nodes"
                    + workspaceId + "\" WHERE sysmlId = '" + childSysmlId + "'), " + edgeType.getValue()
                    + ") RETURNING id");
        } catch (Exception e) {
            if (e.getMessage().contains("duplicate key")) {
                logger.info(String.format("%s", LogUtil.getStackTrace(e)));
            } else {
                logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
            }
        } finally {
            close();
        }
    }

    public Map<String, String> getCommitAndTimestamp(String lookUp, String value) {
        Map<String, String> commit = new HashMap<>();
        try {
            String query = "SELECT elasticId, timestamp FROM commits WHERE %s = '%s';";
            ResultSet rs = execQuery(String.format(query, lookUp, value));

            if (rs.next()) {
                commit.put(Sjm.COMMITID, rs.getString(1));
                commit.put(Sjm.TIMESTAMP, rs.getString(2));
                return commit;
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return null;
    }

    public Long getTimestamp(String lookUp, String value) {
        Long timestamp;
        try {
            String query = "SELECT timestamp FROM commits WHERE %s = '%s';";
            ResultSet rs = execQuery(String.format(query, lookUp, value));

            if (rs.next()) {
                timestamp = rs.getTimestamp(1).getTime();
                return timestamp;
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return null;
    }

    public Map<String, Object> getCommit(String commitId) {
        try {
            connect();
            PreparedStatement query = this.conn.prepareStatement(
                "SELECT commits.id, commits.elasticId, commits.refid, commits.timestamp, committype.name, creator FROM commits JOIN committype ON commits.committype = committype.id WHERE elasticId = ?");
            query.setString(1, commitId);
            ResultSet rs = query.executeQuery();

            if (rs.next()) {
                Map<String, Object> result = new HashMap<>();
                String refId = rs.getString(3).isEmpty() || rs.getString(3).equals("") ? "master" : rs.getString(3);
                result.put(Sjm.SYSMLID, rs.getInt(1));
                result.put(Sjm.ELASTICID, rs.getString(2));
                result.put(Sjm.REFID, refId);
                result.put(Sjm.TIMESTAMP, rs.getTimestamp(4));
                result.put("commitType", rs.getString(5));
                result.put(Sjm.CREATOR, rs.getString(6));
                return result;
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return null;
    }

    public List<Map<String, String>> getAllCommits() {
        List<Map<String, String>> commits = new ArrayList<>();

        try {
            String query = "SELECT elasticId, refId, timestamp FROM commits ORDER BY timestamp DESC";
            ResultSet rs = execQuery(query);

            while (rs.next()) {
                Map<String, String> commit = new HashMap<>();
                commit.put("commitId", rs.getString(1));
                commit.put("refId", rs.getString(2));
                commit.put("timestamp", rs.getString(3));
                commits.add(commit);
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return commits;
    }

    public boolean isInitialCommit() {
        boolean isInitial = false;
        try {
            String query = "SELECT count(elasticId) FROM commits";
            ResultSet rs = execQuery(query);
            isInitial = !(rs.next() && rs.getInt(1) > 0);
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return isInitial;
    }

    public LinkedList<String> getRootParents(String sysmlId, DbEdgeTypes et) {
        LinkedList<String> result = new LinkedList<>();
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null)
                return result;

            String query = "SELECT * FROM get_root_parents(%s, %d, '%s')";
            ResultSet rs = execQuery(String.format(query, n.getId(), et.getValue(), workspaceId));

            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    /**
     * Returns a set of immediate parents of sysmlid to elasticid
     *
     * @param sysmlId
     * @param et
     * @return
     */
    public Set<Pair<String, String>> getImmediateParents(String sysmlId, DbEdgeTypes et) {
        Set<Pair<String, String>> result = new HashSet<>();
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null)
                return result;

            String query = "SELECT * FROM get_immediate_parents(%s, %d, '%s')";
            ResultSet rs = execQuery(String.format(query, n.getId(), et.getValue(), workspaceId));

            while (rs.next()) {
                result.add(new Pair<>(rs.getString(1), rs.getString(2)));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public Map<String, Set<String>> getImmediateParentRoots(String sysmlId, DbEdgeTypes et) {
        Map<String, Set<String>> result = new HashMap<>();
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null)
                return result;

            String query = "SELECT * FROM get_immediate_parent_roots(%s, %d, '%s')";
            ResultSet rs = execQuery(String.format(query, n.getId(), et.getValue(), workspaceId));

            while (rs.next()) {
                String rootId = rs.getString(2);
                String immediateID = rs.getString(1);
                if (!result.containsKey(rootId)) {
                    result.put(rootId, new HashSet<>());
                }
                result.get(rootId).add(immediateID);
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public String getImmediateParentOfType(String sysmlId, DbEdgeTypes et, Set<DbNodeTypes> dnts) {
        String result = null;

        Set<Pair<String, String>> immediateParents = getImmediateParents(sysmlId, et);
        while (immediateParents.size() > 0) {
            String parentId = null;
            for (Pair<String, String> immediateParent : immediateParents) {
                parentId = immediateParent.first;
                try {
                    String query = "SELECT nodetype FROM nodes%s WHERE sysmlid='%s'";
                    ResultSet rs = execQuery(String.format(query, workspaceId, parentId));
                    while (rs.next()) {
                        for (DbNodeTypes dnt : dnts) {
                            if (dnt.getValue() == rs.getLong(1)) {
                                return parentId;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
                } finally {
                    close();
                }
            }
            if (parentId != null) {
                immediateParents = getImmediateParents(parentId, et);
            }
        }

        return result;
    }

    public Set<String> getRootParents(String sysmlId, DbEdgeTypes et, int height) {
        Set<String> result = new HashSet<>();
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null)
                return result;

            String query = "SELECT * FROM get_root_parents(%d, %d, '%s')";
            ResultSet rs = execQuery(String.format(query, n.getId(), et.getValue(), workspaceId));

            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public Set<String> getBranchParents(String refId) {
        Set<String> result = new HashSet<>();
        try {

            String query = "SELECT parent FROM refs WHERE refid=%s";
            ResultSet rs = execQuery(String.format(query, refId));

            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    /**
     * Returns in order of height from sysmlID up for containment only
     *
     * @param sysmlId
     * @param height
     * @return
     */
    public List<Pair<String, String>> getContainmentParents(String sysmlId, int height) {
        List<Pair<String, String>> result = new ArrayList<>();
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null)
                return result;

            String query = "SELECT N.sysmlid, N.elasticid FROM \"nodes%s\" N JOIN "
                + "(SELECT * FROM get_parents(%s, %d, '%s')) P ON N.id = P.id ORDER BY P.height";
            ResultSet rs = execQuery(
                String.format(query, workspaceId, n.getId(), DbEdgeTypes.CONTAINMENT.getValue(), workspaceId));

            while (rs.next()) {
                result.add(new Pair<>(rs.getString(1), rs.getString(2)));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public Set<Pair<String, Integer>> getParentsOfType(String sysmlId, DbEdgeTypes dbet) {
        Set<Pair<String, Integer>> result = new HashSet<>();
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null) {
                return result;
            }

            String query = "SELECT N.sysmlid, N.nodetype FROM \"nodes%s\" N JOIN "
                + "(SELECT * FROM get_parents(%s, %d, '%s')) P ON N.id = P.id ORDER BY P.height";
            ResultSet rs = execQuery(String.format(query, workspaceId, n.getId(), dbet.getValue(), workspaceId));

            result.add(new Pair<>(n.getSysmlId(), n.getNodeType()));
            while (rs.next()) {
                result.add(new Pair<>(rs.getString(1), rs.getInt(2)));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return result;
    }

    /**
     * Returns the containment group for element
     *
     * @param sysmlId
     * @return
     */
    public String getGroup(String sysmlId) {
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null)
                return null;

            String query = "SELECT N.sysmlid, N.elasticid, N.nodetype FROM \"nodes%s\" N JOIN "
                + "(SELECT * FROM get_parents(%s, %d, '%s')) P ON N.id = P.id ORDER BY P.height";
            ResultSet rs = execQuery(
                String.format(query, workspaceId, n.getId(), DbEdgeTypes.CONTAINMENT.getValue(), workspaceId));

            while (rs.next()) {
                if (rs.getInt(3) == DbNodeTypes.SITEANDPACKAGE.getValue()) {
                    return rs.getString(1);
                } else if (rs.getInt(3) == DbNodeTypes.SITE.getValue()) {
                    return null;
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return null;
    }

    // returns list of elasticId
    public List<Pair<String, String>> getChildren(String sysmlId, DbEdgeTypes et, int depth) {
        List<Pair<String, String>> result = new ArrayList<>();
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null)
                return result;

            ResultSet rs = execQuery(
                "SELECT sysmlId, elasticId FROM \"nodes" + workspaceId + "\" WHERE id IN (SELECT id FROM get_children("
                    + n.getId() + ", " + et.getValue() + ", '" + workspaceId + "', " + depth + "))");

            while (rs.next()) {
                result.add(new Pair<>(rs.getString(1), rs.getString(2)));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public void deleteEdgesForNode(String sysmlId) {
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null)
                return;

            execUpdate(
                "DELETE FROM \"edges" + workspaceId + "\" WHERE child = " + n.getId() + " OR parent = " + n.getId());
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public void deleteEdgesForNode(String sysmlId, boolean child, DbEdgeTypes edgeType) {
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null)
                return;
            String column = child ? "child" : "parent";
            execUpdate(
                "DELETE FROM \"edges" + workspaceId + "\" WHERE " + column + " = " + n.getId() + " AND edgeType = "
                    + edgeType.getValue());
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public void deleteEdges(String parentSysmlId, String childSysmlId, DbEdgeTypes dbet) {
        try {
            Node pn = getNodeFromSysmlId(parentSysmlId);
            Node cn = getNodeFromSysmlId(childSysmlId);

            if (pn == null || cn == null)
                return;

            execUpdate(
                "DELETE FROM edges WHERE parent = " + pn.getId() + " AND child = " + cn.getId() + " AND edgetype = "
                    + dbet.getValue());
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public List<String> findNullParents() {
        List<String> nullParents = new ArrayList<>();
        try {
            ResultSet rs = execQuery(
                "SELECT n.elasticId FROM nodes n INNER JOIN (SELECT * FROM edges WHERE edgeType = 1 and parent IS NULL) as e ON (n.id = e.child);");
            if (rs == null) {
                return nullParents;
            }
            while (rs.next()) {
                nullParents.add(rs.getString(1));
            }
        } catch (NullPointerException npe) {
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return nullParents;
    }

    public void cleanEdges() {
        try {
            String nullParents = "UPDATE \"edges" + workspaceId + "\" SET parent = nodes.id FROM \"nodes" + workspaceId
                + "\" nodes WHERE parent IS NULL AND edgeType = 1 AND nodes.sysmlid = 'holding_bin_" + project + "'";
            execUpdate(nullParents);
            String query = "DELETE FROM \"edges" + workspaceId + "\" WHERE parent IS NULL OR child IS NULL";
            execUpdate(query);
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public int createOrganization(String orgId, String orgName) {
        int recordId = 0;
        try {
            connectConfig();
            PreparedStatement query =
                this.configConn.prepareStatement("SELECT count(id) FROM organizations WHERE orgId = ?");
            query.setString(1, orgId);
            if (query.execute()) {
                PreparedStatement insertOrg = this.configConn
                    .prepareStatement("INSERT INTO organizations (orgId, orgName) VALUES (?,?) RETURNING ID");
                insertOrg.setString(1, orgId);
                insertOrg.setString(2, orgName);
                ResultSet rs = insertOrg.executeQuery();
                if (rs.next()) {
                    recordId = rs.getInt(1);
                }
            }
        } catch (PSQLException pe) {
            // Do nothing for duplicate found
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        return recordId;
    }

    public void createProjectDatabase(String projectId, String orgId, String name, String location) {
        int organizationId = 0;
        try {
            connectConfig();
            PreparedStatement query = this.configConn.prepareStatement("SELECT id FROM organizations WHERE orgId = ?");
            query.setString(1, orgId);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                organizationId = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        try {
            if (location == null || location.isEmpty()) {
                location = EmsConfig.get("pg.host");
            }
            connectConfig();
            if (organizationId > 0) {
                PreparedStatement insertProject = this.configConn
                    .prepareStatement("INSERT INTO projects (projectId, name, orgId, location) VALUES (?,?,?,?)");
                insertProject.setString(1, projectId);
                insertProject.setString(2, name);
                insertProject.setInt(3, organizationId);
                insertProject.setString(4, location);
                insertProject.execute();
            }
        } catch (PSQLException e) {
            // Do nothing
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }
        try {
            setProject(projectId);
            this.projectProperties.put("dbname", "postgres");
            connect();
            this.conn.createStatement().execute(String.format("CREATE DATABASE \"_%s\";", projectId));
            this.conn.createStatement().execute(
                String.format("GRANT ALL PRIVILEGES ON DATABASE \"_%s\" TO %s;", projectId, EmsConfig.get("pg.user")));
            this.conn.createStatement().execute(
                String.format("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO %s;", EmsConfig.get("pg.user")));
        } catch (SQLException se) {
            // Catch Duplicate error and do nothing
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        createProjectTables(projectId);
    }

    private void createProjectTables(String projectId) {

        try {
            setProject(projectId);
            // Test if tables exist already
            ResultSet exists = execQuery(
                "SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c WHERE c.relname = 'nodetypes' AND c.relkind = 'r')");
            if (exists.next()) {
                if (exists.getBoolean(1)) {
                    return;
                }
            }

            execUpdate("CREATE TABLE nodeTypes (id bigserial primary key, name text not null);");
            execUpdate("CREATE TABLE edgeTypes (id bigserial primary key, name text not null);");

            execUpdate(
                "CREATE TABLE nodes(id bigserial primary key, elasticId text not null unique, nodeType integer REFERENCES nodeTypes(id) not null, sysmlId text not null unique, lastCommit text, initialCommit text, deleted boolean default false);");
            execUpdate("CREATE INDEX nodeIndex on nodes(id);");
            execUpdate("CREATE INDEX sysmlIndex on nodes(sysmlId);");

            execUpdate(
                "CREATE TABLE edges(id bigserial primary key, parent integer REFERENCES nodes(id), child integer references nodes(id), edgeType integer references edgeTypes(id) not null, constraint unique_edges unique (parent, child, edgeType));");
            execUpdate("CREATE INDEX edgeIndex on edges(id);");
            execUpdate("CREATE INDEX childIndex on edges(child);");
            execUpdate("CREATE INDEX parentIndex on edges(parent);");

            execUpdate(
                "CREATE TABLE edgeProperties(edgeId integer REFERENCES edges(id) ON UPDATE NO ACTION ON DELETE CASCADE not null, key text not null, value text not null, CONSTRAINT unique_edgeproperties UNIQUE (edgeId, key));");

            execUpdate("CREATE TABLE commitType(id bigserial primary key, name text not null);");
            execUpdate("CREATE INDEX commitTypeIndex on commitType(id);");

            execUpdate(
                "CREATE TABLE commits(id bigserial primary key, elasticId text not null unique, refId text not null, timestamp timestamp default current_timestamp, commitType bigserial, creator text, FOREIGN KEY(commitType) REFERENCES commitType (id) ON DELETE CASCADE);");
            execUpdate("CREATE INDEX commitIndex on commits(id);");
            execUpdate("CREATE INDEX commitElasticIdIndex on commits(elasticId);");

            execUpdate(
                "CREATE TABLE commitParent(id bigserial primary key, child integer NOT NULL, parent integer not null, FOREIGN KEY(child) REFERENCES commits(id) ON DELETE CASCADE, FOREIGN KEY(parent) REFERENCES commits(id) ON DELETE CASCADE, constraint unique_parents unique(child, parent));");
            execUpdate("CREATE INDEX commitParentIndex on commitParent(id)");

            execUpdate(
                "CREATE TABLE refs(id bigserial primary key, parent text not null, refId text not null unique, refName text not null, parentCommit integer, elasticId text, tag boolean DEFAULT false, timestamp timestamp DEFAULT current_timestamp, deleted boolean DEFAULT false);");
            execUpdate("CREATE INDEX refsIndex on refs(id)");

            execUpdate(
                "CREATE OR REPLACE FUNCTION insert_edge(text, text, text, integer)\n" + "  returns integer as $$\n"
                    + "  begin\n" + "    execute '\n"
                    + "      insert into ' || (format('edges%s', $3)) || ' (parent, child, edgeType) values((select id from ' || format('nodes%s',$3) || ' where sysmlId = ''' || $1 || '''), (select id from ' || format('nodes%s', $3) || ' where sysmlId = ''' || $2 || '''), ' || $4 || ');';\n"
                    + "      return 1;\n" + "    exception\n" + "      when unique_violation then\n"
                    + "        return -1;\n" + "      when not_null_violation then\n" + "        return -1;\n"
                    + "  end;\n" + "$$ language plpgsql;");

            execUpdate("CREATE OR REPLACE FUNCTION insert_edge_property(text, text, text, integer, text, text)\n"
                + "  returns integer as $$\n" + "  begin\n" + "  execute '\n"
                + "      insert into ' || (format('edgeProperties%s', $3)) || ' (edgeId, key, value) values((select id from ' || format('edges%s', $3) || ' where parent = (select id from ' || format('nodes%s', $3) || ' where sysmlId = ''' || $1 || ''') and child = (select id from ' || format('nodes%s', $3) || ' where sysmlId = ''' || $2 || ''') and edgeType = ' || $4 || '), ''' || $5 || ''', ''' || $6 || ''');';\n"
                + "      return 1;\n" + "    exception\n" + "      when unique_violation then\n"
                + "        return -1;\n" + "      when not_null_violation then\n" + "        return -1;\n" + "  end;\n"
                + "$$ language plpgsql;");

            execUpdate("CREATE OR REPLACE FUNCTION get_edge_properties(edge integer, text)\n"
                + "  returns table(key text, value text) as $$\n" + "  begin\n" + "    return query\n"
                + "    execute '\n"
                + "      select key, value from ' || (format('edgeproperties%s', $1)) || ' where edgeid = ' || edge;\n"
                + "  end;\n" + "$$ language plpgsql;");

            execUpdate("CREATE OR REPLACE FUNCTION get_children(integer, integer, text, integer)\n"
                + "  returns table(id bigint) as $$\n" + "  begin\n" + "    return query\n" + "    execute '\n"
                + "    with recursive children(depth, nid, path, cycle, deleted) as (\n"
                + "      select 0 as depth, node.id, ARRAY[node.id], false, node.deleted from ' || format('nodes%s', $3) || '\n"
                + "        node where node.id = ' || $1 || ' union\n"
                + "      select (c.depth + 1) as depth, edge.child as nid, path || cast(edge.child as bigint) as path, edge.child = ANY(path) as cycle, node.deleted as deleted \n"
                + "        from ' || format('edges%s', $3) || ' edge, children c, ' || format('nodes%s', $3) || ' node where edge.parent = nid and node.id = edge.child and node.deleted = false and \n"
                + "        edge.edgeType = ' || $2 || ' and not cycle and depth < ' || $4 || ' \n" + "      )\n"
                + "      select distinct nid from children;';\n" + "  end;\n" + "$$ language plpgsql;");

            execUpdate("CREATE OR REPLACE FUNCTION get_childviews(integer, text)\n"
                + "  returns table(sysmlid text, aggregation text) as $$\n" + "  begin\n" + "    return query\n"
                + "    execute '\n" + "    with childviews(sysmlid, aggregation) as (\n" + "        (\n"
                + "        select typeid.value as sysmlid, aggregation.value as aggregation\n"
                + "          from ' || format('edges%s', $2) || ' as edges\n"
                + "          join ' || format('edgeproperties%s', $2) || ' as ordering on edges.id = ordering.edgeid and ordering.key = ''order''\n"
                + "          join ' || format('edgeproperties%s', $2) || ' as aggregation on edges.id = aggregation.edgeid and aggregation.key = ''aggregation''\n"
                + "          join ' || format('edgeproperties%s', $2) || ' as typeid on edges.id = typeid.edgeid and typeid.key = ''typeId''\n"
                + "          join ' || format('nodes%s', $2) || ' as child on typeid.value = child.sysmlid and (child.nodetype = 4 or child.nodetype = 12)\n"
                + "          where edges.parent = ' || $1 || '\n" + "          order by ordering.value::integer ASC\n"
                + "        )\n" + "      )\n" + "      select sysmlid, aggregation from childviews;';\n" + "  end;\n"
                + "$$ language plpgsql;");

            execUpdate("CREATE OR REPLACE FUNCTION get_parents(integer, integer, text)\n"
                + "  returns table(id bigint, height integer, root boolean) as $$\n" + "  begin\n"
                + "    return query\n" + "    execute '\n"
                + "    with recursive parents(height, nid, path, cycle) as (\n"
                + "    select 0, node.id, ARRAY[node.id], false from ' || format('nodes%s', $3) || ' node where node.id = ' || $1 || '\n"
                + "    union\n" + "      select (c.height + 1), edge.parent, path || cast(edge.parent as bigint),\n"
                + "        edge.parent = ANY(path) from ' || format('edges%s', $3) || '\n"
                + "        edge, parents c where edge.child = nid and edge.edgeType = ' || $2 || '\n"
                + "        and not cycle\n" + "      )\n"
                + "      select nid,height,(not exists (select true from edges where child = nid and edgetype = ' || $2 || '))\n"
                + "        from parents order by height desc;';\n" + "  end;\n" + "$$ language plpgsql;");

            execUpdate("CREATE OR REPLACE FUNCTION get_immediate_parents(integer, integer, text)\n"
                + "  returns table(sysmlid text, elasticid text) as $$\n" + "  begin\n" + "    return query\n"
                + "    execute '\n" + "    select sysmlid, elasticid from nodes' || $3 || ' where id in\n"
                + "      (select id from get_parents(' || $1 || ',' || $2 || ',''' || format('%s',$3) ||\n"
                + "      ''') where height = 1);';\n" + "  end;\n" + "$$ language plpgsql;");

            execUpdate("CREATE OR REPLACE FUNCTION get_root_parents(integer, integer, text)\n"
                + "  returns table(sysmlid text) as $$\n" + "  begin\n" + "    return query\n" + "    execute '\n"
                + "    select sysmlid from nodes' || $3 || ' where id in\n"
                + "      (select id from get_parents(' || $1 || ',' || $2 || ',''' || format('%s',$3) ||\n"
                + "      ''') where root = true);';\n" + "  end;\n" + "$$ language plpgsql;");

            execUpdate("CREATE OR REPLACE FUNCTION get_immediate_parent_roots(integer, integer, text)\n"
                + "  returns table(ip text, rp text) as $$\n" + "  declare\n" + "    s text;\n" + "    l text;\n"
                + "  begin\n" + "    FOR s in select sysmlid from get_immediate_parents($1,$2,$3) LOOP\n"
                + "      return query select s,sysmlid from get_root_parents(cast((select id from nodes where sysmlid=s) as int), $2, $3);\n"
                + "    end Loop;\n" + "    RETURN;\n" + " end;\n" + "$$ language plpgsql;");

            execUpdate("CREATE TYPE return_type as (pstart integer, pend integer, path integer[]);\n");

            execUpdate("CREATE OR REPLACE FUNCTION get_paths_to_node(integer, integer, text)\n"
                + "  returns setof return_type as $$\n" + "  begin\n" + "    return query\n" + "    execute '\n"
                + "    with recursive node_graph as (\n" + "      select parent as path_start, child as path_end,\n"
                + "             array[parent, child] as path\n"
                + "      from ' || format('edges%s', $3) || ' where edgeType = ' || $2 || '\n" + "      union all\n"
                + "      select ng.path_start, nr.child as path_end,\n" + "           ng.path || nr.child as path\n"
                + "      from node_graph ng\n"
                + "      join edges nr ON ng.path_end = nr.parent where nr.edgeType = ' || $2 || '\n" + "    )\n"
                + "    select * from node_graph where path_end = ' || $1 || ' order by path_start, array_length(path,1)';\n"
                + "  end;\n" + "$$ language plpgsql;");

            execUpdate(
                "CREATE AGGREGATE array_agg_mult(anyarray) (\n" + "    SFUNC = array_cat,\n" + "    STYPE = anyarray,\n"
                    + "    INITCOND = '{}'\n" + ");\n");

            execUpdate(
                "CREATE OR REPLACE FUNCTION array_sort_unique (anyarray)\n" + "  returns anyarray\n" + "  as $body$\n"
                    + "    select array(\n" + "      select distinct $1[s.i]\n"
                    + "      from generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)\n"
                    + "      order by 1\n" + "    );\n" + "  $body$\n" + "language sql;");

            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (1, 'element');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (2, 'site');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (3, 'project');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (4, 'document');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (5, 'comment');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (6, 'constraint');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (7, 'instancespecification');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (8, 'operation');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (9, 'package');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (10, 'property');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (11, 'parameter');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (12, 'view');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (13, 'viewpoint');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (14, 'siteandpackage');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (15, 'holdingbin');");
            execUpdate("INSERT INTO nodeTypes(id, name) VALUES (16, 'mount');");

            execUpdate("INSERT INTO edgeTypes(id, name) VALUES (1, 'containment');");
            execUpdate("INSERT INTO edgeTypes(id, name) VALUES (2, 'view');");
            execUpdate("INSERT INTO edgeTypes(id, name) VALUES (3, 'transclusion');");
            execUpdate("INSERT INTO edgeTypes(id, name) VALUES (4, 'childview');");

            execUpdate("INSERT INTO commitType(id, name) VALUES (1, 'commit');");
            execUpdate("INSERT INTO commitType(id, name) VALUES (2, 'branch');");
            execUpdate("INSERT INTO commitType(id, name) VALUES (3, 'merge');");

            execUpdate(String.format("GRANT USAGE, SELECT ON SEQUENCE nodes_id_seq TO %s;", EmsConfig.get("pg.user")));
            execUpdate(String.format("GRANT USAGE, SELECT ON SEQUENCE refs_id_seq TO %s;", EmsConfig.get("pg.user")));

        } catch (PSQLException pe) {
            // Do Nothing
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public void addProjectMount(String projectId, String mountId) {
        try {
            connectConfig();
            if ((this.configConn.createStatement()
                .execute(String.format("SELECT id FROM projects WHERE projectId = '%s'", projectId)))
                && (this.configConn.createStatement()
                .execute(String.format("SELECT id FROM projects WHERE projectId = '%s'", mountId)))) {
                this.configConn.createStatement().execute(String
                    .format("INSERT INTO projectMounts (projectId, mountId) VALUES " + "('%s','%s')", projectId,
                        mountId));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }
    }

    public void createBranchFromWorkspace(String childWorkspaceName, String workspaceName, String elasticId,
        String commitId, boolean isTag) {
        if (childWorkspaceName == null || childWorkspaceName.length() == 0 || childWorkspaceName.equals("master")) {
            return;
        }

        try {
            // make sure that foreign key constraints match mms.sql
            String childWorkspaceNameSanitized = sanitizeRefId(childWorkspaceName);

            // insert record into workspace table

            execUpdate(String.format(
                "CREATE TABLE nodes%s (LIKE nodes%s INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES)",
                childWorkspaceNameSanitized, workspaceId));

            execUpdate(String.format(
                "CREATE TABLE edges%s (LIKE edges%s INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES)",
                childWorkspaceNameSanitized, workspaceId));

            int commit = 0;
            if (commitId != null && !commitId.isEmpty()) {
                Map<String, Object> commitObject = getCommit(commitId);
                if (commitObject != null && commitObject.containsKey(Sjm.SYSMLID)) {
                    commit = (int) commitObject.get(Sjm.SYSMLID);
                }
            } else {
                commit = getHeadCommit();
            }

            insertRef(childWorkspaceNameSanitized, workspaceName, commit, elasticId, isTag);
            copyTable("nodes", childWorkspaceNameSanitized, workspaceId);

            if (commitId != null && !commitId.isEmpty()) {
                execUpdate(String.format("UPDATE nodes%s SET deleted = true WHERE initialcommit IS NOT NULL",
                    childWorkspaceNameSanitized));
            } else {
                copyTable("edges", childWorkspaceNameSanitized, workspaceId);
            }

            // add constraints last otherwise they won't hold
            execUpdate(String.format(
                "ALTER TABLE ONLY nodes%s ADD CONSTRAINT nodes%s_nodetype_fkey FOREIGN KEY (nodetype) REFERENCES nodetypes(id)",
                childWorkspaceNameSanitized, childWorkspaceNameSanitized));
            execUpdate(String.format(
                "ALTER TABLE ONLY edges%s ADD CONSTRAINT edges%s_child_fkey FOREIGN KEY (child) REFERENCES nodes%s(id)",
                childWorkspaceNameSanitized, childWorkspaceNameSanitized, childWorkspaceNameSanitized));
            execUpdate(String.format(
                "ALTER TABLE ONLY edges%s ADD CONSTRAINT edges%s_parent_fkey FOREIGN KEY (parent) REFERENCES nodes%s(id)",
                childWorkspaceNameSanitized, childWorkspaceNameSanitized, childWorkspaceNameSanitized));
            execUpdate(String.format(
                "ALTER TABLE ONLY edges%s ADD CONSTRAINT edges%s_edgetype_fkey FOREIGN KEY (edgetype) REFERENCES edgetypes(id)",
                childWorkspaceNameSanitized, childWorkspaceNameSanitized));

            if (isTag) {
                execUpdate(String.format("REVOKE INSERT, UPDATE, DELETE ON nodes%1$s, edges%1$s FROM %2$s",
                    childWorkspaceNameSanitized, EmsConfig.get("pg.user")));
            }

        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    private void copyTable(String name, String toRef, String fromRef) {
        try {
            execUpdate(String.format("INSERT INTO %1$s%2$s SELECT * FROM %1$s%3$s", name, toRef, fromRef));
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public boolean isTag(String refId) {
        try {
            ResultSet rs = execQuery(String
                .format("SELECT tag FROM refs WHERE (refId = '%1$s' OR refName = '%1$s') AND deleted = false", refId));
            if (rs.next()) {
                return rs.getBoolean(1);
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return false;
    }

    public void updateTag(String name, String elasticId, String id) {
        try {
            execUpdate(String
                .format("UPDATE tags SET timestamp = now(),name = '%s',elasticId = '%s' WHERE id = '%s'", name,
                    elasticId, id));
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public void deleteRef(String id) {
        try {
            execUpdate(String.format("UPDATE refs SET deleted = true WHERE refId = '%s'", id));
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public void deleteRefTables(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        try {
            execUpdate("DROP TABLE IF EXISTS \"nodes" + id + "\"");
            execUpdate("DROP TABLE IF EXISTS \"edges" + id + "\"");
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public Pair<String, String> getRefElastic(String refId) {
        if (refId == null || refId.isEmpty()) {
            refId = "master";
        }
        refId = sanitizeRefId(refId);
        try {
            ResultSet rs = execQuery(
                String.format("SELECT refId, elasticId FROM refs WHERE deleted = false AND refId = '%s'", refId));

            if (rs.next()) {
                return new Pair<>(rs.getString(1), rs.getString(2));
            }

        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return null;
    }

    public Pair<String, Long> getParentRef(String refId) {
        if (refId.equals("master")) {
            return null;
        }
        refId = sanitizeRefId(refId);
        try {
            ResultSet rs = execQuery(
                String.format("SELECT parent, timestamp FROM refs WHERE deleted = false AND refId = '%s'", refId));

            if (rs.next()) {
                String checkForMaster =
                    (rs.getString(1).equals("") && !refId.equals("master")) ? "master" : rs.getString(1);
                return new Pair<>(checkForMaster, rs.getTimestamp(2).getTime());
            }

        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return null;
    }

    public List<Pair<String, String>> getRefsElastic() {
        List<Pair<String, String>> result = new ArrayList<>();
        try {
            ResultSet rs = execQuery("SELECT refId, elasticId FROM refs WHERE deleted = false");

            while (rs.next()) {
                result.add(new Pair<>(rs.getString(1), rs.getString(2)));
            }

        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public List<Map<String, Object>> getRefsCommits(String refId) {
        return getRefsCommits(refId, 0);
    }

    public List<Map<String, Object>> getRefsCommits(String refId, int commitId) {

        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String refIdString = sanitizeRefId(refId);
            if (refId.equals("master")) {
                refId = "";
            }
            if (refIdString.equals("")) {
                refIdString = "master";
            }
            String query = String.format(
                "SELECT elasticId, creator, timestamp, refId, commitType.name FROM commits JOIN commitType ON commitType.id = commits.commitType WHERE (refId = '%s' OR refId = '%s')",
                refId, refIdString);

            if (commitId != 0) {
                query += String.format(" AND timestamp <= (SELECT timestamp FROM commits WHERE id = %s)", commitId);
            }
            query += " ORDER BY timestamp DESC";

            ResultSet rs = execQuery(query);

            while (rs.next()) {
                Map<String, Object> commit = new HashMap<>();
                commit.put(Sjm.SYSMLID, rs.getString(1));
                commit.put(Sjm.CREATOR, rs.getString(2));
                commit.put(Sjm.CREATED, rs.getTimestamp(3));
                commit.put("refId", rs.getString(4));
                commit.put("commitType", rs.getString(5));
                result.add(commit);
            }

            rs = execQuery(String
                .format("SELECT parent, parentCommit FROM refs WHERE refId = '%s' OR refId = '%s'", refId,
                    refIdString));
            if (rs.next() && rs.getInt(2) != 0) {
                String nextRefId = rs.getString(1);
                result.addAll(getRefsCommits(nextRefId, rs.getInt(2)));
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public List<Pair<String, String>> getTags() {
        List<Pair<String, String>> result = new ArrayList<>();
        try {
            ResultSet rs = execQuery("SELECT refId, elasticId FROM refs WHERE tag = true AND deleted = false");

            while (rs.next()) {
                result.add(new Pair<>(rs.getString(1), rs.getString(2)));
            }

        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public void insertRef(String newWorkspaceId, String newWorkspaceName, int headCommit, String elasticId,
        boolean isTag) {
        String parent;
        if (workspaceId.equals("") && !newWorkspaceId.equals("master")) {
            parent = "master";
        } else {
            parent = workspaceId;
        }
        try {
            Map<String, String> map = new HashMap<>();
            map.put("refId", newWorkspaceId);
            map.put("refName", newWorkspaceName);
            map.put("parent", parent);
            map.put("parentCommit", Integer.toString(headCommit));
            map.put("elasticId", elasticId);
            map.put("tag", Boolean.toString(isTag));
            insert("refs", map);
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public void updateRef(String refId, String refName, String elasticId, boolean isTag) {
        try {
            execUpdate(String
                .format("UPDATE refs SET refName = '%s', elasticId = '%s', tag = '%b' WHERE refId = '%s'", refName,
                    elasticId, isTag, refId));
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public void insertTag(String workspaceName, String workspaceId) {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("refName", workspaceName);
            map.put("refId", workspaceId);
            map.put("tag", "true");
            insert("refs", map);
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }

    }

    public List<String> filterNodesWithElastic(List<String> elasticrefs) {
        return filterNodesByWorkspaceWithElastic(elasticrefs, workspaceId);
    }

    public List<String> filterNodesByWorkspaceWithElastic(List<String> elasticrefs, String workspace) {
        return filterNodesByWorkspace(elasticrefs, workspace, "elasticid");
    }

    public List<String> filterNodesWithSysmlid(List<String> sysmlids) {
        return filterNodesByWorkspaceWithSysmlid(sysmlids, workspaceId);
    }

    public List<String> filterNodesByWorkspaceWithSysmlid(List<String> sysmlids, String workspace) {
        return filterNodesByWorkspace(sysmlids, workspace, "sysmlid");
    }

    public List<String> filterNodesByWorkspace(List<String> sysmlids, String workspace, String column) {
        List<String> result = new ArrayList<>();
        List<String> selectCache = new ArrayList<>();
        int limit = Integer.parseInt(EmsConfig.get("pg.limit.select"));
        try {
            for (int i = 0; i < sysmlids.size(); i++) {
                selectCache.add(sysmlids.get(i));
                if (((i + 1) % limit) == 0 || i == (sysmlids.size() - 1)) {
                    String query = "SELECT " + column + " FROM \"nodes" + workspace + "\" WHERE " + column + " IN ('";
                    query += StringUtils.join(selectCache, "','");
                    query += "');";

                    logger.debug(String.format("Query: %s", query));

                    ResultSet rs = execQuery(query);

                    while (rs.next()) {
                        result.add(rs.getString(1));
                    }
                    selectCache = new ArrayList<>();
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return result;
    }

    public boolean orgExists(String orgId) {
        try {
            connectConfig();
            PreparedStatement query =
                this.configConn.prepareStatement("SELECT count(id) FROM organizations WHERE orgId = ?");
            query.setString(1, orgId);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }
        return false;
    }

    public boolean siteExists(String siteName) {
        try {
            connect();
            PreparedStatement query = this.conn.prepareStatement("SELECT count(*) FROM \"nodes" + workspaceId
                + "\" WHERE (nodetype = (SELECT id FROM nodetypes WHERE name = 'site') OR nodetype = (SELECT id FROM nodetypes WHERE name = 'siteandpackage')) AND sysmlid = ?");
            query.setString(1, siteName);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return false;
    }

    public boolean refExists(String refId) {
        String currentWorkspace = this.workspaceId;
        this.workspaceId = "";
        refId = sanitizeRefId(refId);
        try {
            connect();
            PreparedStatement query = this.conn.prepareStatement("SELECT count(id) FROM refs WHERE refId = ?");
            query.setString(1, refId);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        this.workspaceId = currentWorkspace;
        return false;
    }

    /**
     * Will alter the connection limit to the database to be 0 then kill any processes connected to it. Finally it will
     * drop the database. If any connections persist or it fails to change the connection limit then the database
     * will not be dropped.
     *
     * @param databaseName
     */
    public void dropDatabase(String databaseName) {

        String query = "ALTER DATABASE  \"_" + databaseName + "\" CONNECTION LIMIT 0";
        String query2 =
            "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = \'_" + databaseName + "\'";
        String query3 = "DROP DATABASE \"_" + databaseName + "\";";

        connectConfig();

        try {
            this.configConn.createStatement().executeUpdate(query);
            this.configConn.prepareCall(query2).execute();
            this.configConn.createStatement().executeUpdate(query3);

            // Should only try to remove the connection from postgres if the queries succeed.
            PostgresPool.removeConnection(EmsConfig.get("pg.host"), databaseName);
        } catch (SQLException e) {

            logger.error(String.format("%s", LogUtil.getStackTrace(e)));

            // If any of the queries fail, reset the connection limit of the database
            query = "ALTER DATABASE  \"_" + databaseName + "\" CONNECTION LIMIT -1";
            try {
                this.configConn.createStatement().executeUpdate(query);
            } catch (SQLException e2) {
                logger.warn(String.format("%s", LogUtil.getStackTrace(e2)));
            }
        } finally {
            closeConfig();
        }
    }

    /**
     * Deletes the project from the project table based on the projectId provided.
     *
     * @param projectId
     */
    public void deleteProjectFromProjectsTable(String projectId) {
        connectConfig();
        try {
            String query = "DELETE FROM projects WHERE projectid = \'" + projectId + "\'";
            this.configConn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        closeConfig();
    }

    private String sanitizeRefId(String refId) {
        return refId.replace("-", "_").replaceAll("\\s+", "");
    }

    public boolean isLocked() {
        try {
            String query = "SELECT id FROM queue WHERE refId = ";
            ResultSet rs = execQuery(query);
            while(rs.next()) {
                if (rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return false;
    }
}
