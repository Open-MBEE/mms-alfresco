package gov.nasa.jpl.view_repo.db;

import com.google.gson.JsonPrimitive;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

public class PostgresHelper implements GraphInterface {
    static Logger logger = Logger.getLogger(PostgresHelper.class);

    private Map<String, Connection> connMap = new HashMap<>();
    private String project;
    private Map<String, String> projectProperties = new HashMap<>();
    private String workspaceId;
    private Savepoint savePoint;
    private static final String pgHost = EmsConfig.get("pg.host");
    private static final String pgName = EmsConfig.get("pg.name");

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
        connect(false);
    }

    public void connect(boolean config) {
        try {
            if (config) {
                if (!this.connMap.containsKey("config") || this.connMap.get("config") == null || this.connMap
                    .get("config").isClosed()) {
                    this.connMap.put("config", PostgresPool.getInstance(pgHost, pgName).getConnection());
                }
            } else if (!this.connMap.containsKey(project) || this.connMap.get(project) == null || this.connMap
                .get(project).isClosed()) {
                this.connMap.put(project, PostgresPool
                    .getInstance(this.projectProperties.get("location"), this.projectProperties.get("dbname"))
                    .getConnection());
            }
        } catch (SQLException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public Connection getConn() {
        return getConn(null);
    }

    public Connection getConn(String type) {
        if (type == null) {
            connect();
            return this.connMap.getOrDefault(project, null);
        } else {
            switch (type) {
                case "config":
                    connect(true);
                    return this.connMap.get("config");

                default:
                    connect();
                    return this.connMap.get(project);
            }
        }
    }

    public void close() {
        closeConnection(project);
    }

    public void closeConfig() {
        closeConnection("config");
    }

    private void closeConnection(String key)
    {
        try {
            Connection connection = this.connMap.remove(key);
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    private void getProjectProperties() {
        projectProperties.put("location", pgHost);
        projectProperties.put("dbname", "_" + project);
        try (PreparedStatement query = getConn("config")
            .prepareStatement("SELECT location FROM projects WHERE projectId = ?");) {
            query.setString(1, project);
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next() && !rs.getString(1).isEmpty()) {
                    projectProperties.put("location", rs.getString(1));
                }
            } catch (SQLException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
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
            try (PreparedStatement query = prepareStatement("SELECT refId FROM refs WHERE refId = ?");) {
                query.setString(1, workspaceId);
                try (ResultSet rs = query.executeQuery()) {
                    if (rs.next()) {
                        this.workspaceId = rs.getString(1);
                    }
                } catch (SQLException e) {
                    logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                }
            } catch (SQLException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            } finally {
                close();
            }

            if (this.workspaceId.equals("")) {
                try (PreparedStatement nquery = getConn()
                    .prepareStatement("SELECT refId FROM refs WHERE refName = ?")) {
                    nquery.setString(1, workspaceId);
                    try (ResultSet nrs = nquery.executeQuery()) {
                        if (nrs.next()) {
                            this.workspaceId = workspaceId;
                        }
                    } catch (SQLException e) {
                        logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                    }
                } catch (SQLException e) {
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
        getConn().setAutoCommit(false);
        logger.info("Starting transaction");
        if (savePointName != null) {
            this.savePoint = getConn().setSavepoint(savePointName);
        } else {
            this.savePoint = getConn().setSavepoint();
        }

        return this.savePoint;
    }

    public void commitTransaction() throws SQLException {
        try {
            if (!getConn().getAutoCommit()) {
                logger.warn("Committing transaction");
                getConn().commit();
            } else {
                logger.warn("Cannot commit, no transaction");
            }
            logger.info("Transaction finished");
        } catch (SQLException e) {
            if (this.savePoint != null) {
                getConn().rollback(this.savePoint);
                logger.warn(String.format("Transaction has been rolled back to savePoint: %s", this.savePoint));
            } else {
                getConn().rollback();
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
        getConn().rollback(savepoint);
    }

    public int execUpdate(String query) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("execUpdate: %s", query));
        }
        try {
            return getConn().createStatement().executeUpdate(query);
        } catch (SQLException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
        return 0;
    }

    public ResultSet execQuery(String query) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("execQuery: %s", query));
        }
        try {
            return getConn().createStatement().executeQuery(query);
        } catch (SQLException e) {
            if (logger.isDebugEnabled()) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
        return null;
    }

    public PreparedStatement prepareStatement(String query) {
        try {
            return getConn().prepareStatement(query);
        } catch (SQLException e) {
            if (logger.isDebugEnabled()) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }

        return null;
    }

    public int insert(String table, Map<String, Object> values) {

        StringBuilder columns = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        List<String> columnList = new ArrayList<>();

        try {
            for (String col : values.keySet()) {
                columns.append(col).append(',');
                columnList.add(col);
                vals.append('?').append(',');
            }

            columns.setLength(columns.length() - 1);
            vals.setLength(vals.length() - 1);

            String query = String
                .format("INSERT INTO \"%s\" (%s) VALUES (%s) RETURNING id", table, columns.toString(), vals.toString());

            try (PreparedStatement statement = prepareStatement(query)) {
                for (int i = 0; i < columnList.size(); i++) {
                    statementSetter(statement, values.getOrDefault(columnList.get(i), null), i);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Query: %s", query));
                }
                statement.execute();
                return 1;
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return -1;
    }

    public void updateLastCommitsNodes(String value, List<String> sysmlIds) {
        if (sysmlIds == null || sysmlIds.isEmpty()) {
            return;
        }
        updateLastCommits(value, sysmlIds, "nodes");
    }

    public void updateLastCommitsArtifacts(String value, List<String> sysmlIds) {
        if (sysmlIds == null || sysmlIds.isEmpty()) {
            return;
        }
        updateLastCommits(value, sysmlIds, "artifacts");
    }

    public void updateLastCommits(String value, List<String> sysmlIds, String type) {
        String starter = String.format("UPDATE \"%s%s\" SET lastcommit = ? WHERE sysmlId IN (", type, workspaceId);
        int limit = Integer.parseInt(EmsConfig.get("pg.limit.insert"));
        StringBuilder query = new StringBuilder(starter);
        int count = 0;
        int total = sysmlIds.size();
        for (int i = 0; i < total; i++) {
            query.append("?,");
            count++;
            if (((i + 1) % limit) == 0 || i == (total - 1)) {
                query.setLength(query.length() - 1);
                query.append(")");
                List<Object> single = new LinkedList<>();
                single.add(0, value);
                for (int j = 0; j < count; j++) {
                    single.add(j + 1, sysmlIds.remove(0));
                }
                List<List<Object>> values = new ArrayList<>();
                values.add(single);
                executeBulkStatements(query.toString(), values);
                query = new StringBuilder(starter);
                count = 0;
            }
        }
    }

    public void runBatchQueries(List<Map<String, Object>> rows, String type) {
        String query = null;
        List<List<Object>> values = new LinkedList<>();

        switch (type) {
            case "nodes":
                query = String.format(
                    "INSERT INTO \"nodes%s\" (elasticId, sysmlId, lastcommit, initialcommit, nodeType) VALUES (?, ?, ?, ?, ?)",
                    workspaceId);
                for (Map<String, Object> node : rows) {
                    List<Object> single = new LinkedList<>();
                    single.add(0, node.get(Sjm.ELASTICID));
                    single.add(1, node.get(Sjm.SYSMLID));
                    single.add(2, node.get("lastcommit"));
                    single.add(3,
                        node.containsKey("initialcommit") ? node.get("initialcommit") : node.get(Sjm.ELASTICID));
                    single.add(4, node.get("nodetype"));
                    values.add(single);
                }
                break;
            case "artifacts":
                query = String.format(
                    "INSERT INTO \"artifacts%s\" (elasticId, sysmlId, lastcommit, initialcommit) VALUES (?, ?, ?, ?)",
                    workspaceId);
                for (Map<String, Object> node : rows) {
                    List<Object> single = new LinkedList<>();
                    single.add(0, node.get(Sjm.ELASTICID));
                    single.add(1, node.get(Sjm.SYSMLID));
                    single.add(2, node.get("lastcommit"));
                    single.add(3,
                        node.containsKey("initialcommit") ? node.get("initialcommit") : node.get(Sjm.ELASTICID));
                    values.add(single);
                }
                break;
            case "artifactUpdates":
                query = String
                    .format("UPDATE \"artifacts%s\" SET elasticId = ?, lastcommit = ?, deleted = ? WHERE sysmlId = ?",
                        workspaceId);
                for (Map<String, Object> node : rows) {
                    List<Object> single = new LinkedList<>();
                    single.add(0, node.get(Sjm.ELASTICID));
                    single.add(1, node.get("lastcommit"));
                    single.add(2, node.get("deleted"));
                    single.add(3, node.get(Sjm.SYSMLID));
                    values.add(single);
                }
                break;
            case "updates":
                query = String.format(
                    "UPDATE \"nodes%s\" SET elasticId = ?, lastcommit = ?, nodeType = ?, deleted = ? WHERE sysmlId = ?",
                    workspaceId);
                for (Map<String, Object> node : rows) {
                    List<Object> single = new LinkedList<>();
                    single.add(0, node.get(Sjm.ELASTICID));
                    single.add(1, node.get("lastcommit"));
                    single.add(2, node.get("nodetype"));
                    single.add(3, node.get("deleted"));
                    single.add(4, node.get(Sjm.SYSMLID));
                    values.add(single);
                }
                break;
            case "edges":
                query = String.format(
                    "INSERT INTO \"edges%1$s\" (parent, child, edgeType) VALUES ((SELECT id FROM \"nodes%1$s\" WHERE sysmlid = ?), (SELECT id FROM \"nodes%1$s\" WHERE sysmlid = ?), ?)",
                    workspaceId);
                for (Map<String, Object> node : rows) {
                    List<Object> single = new LinkedList<>();
                    single.add(0, node.get("parent"));
                    single.add(1, node.get("child"));
                    single.add(2, node.get("edgetype"));
                    values.add(single);
                }
                break;
            default:
                break;
        }

        if (query != null && !values.isEmpty()) {
            executeBulkStatements(query, values);
        }
    }

    private Node resultSetToNode(ResultSet rs) throws SQLException {
        return new Node(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6),
            rs.getBoolean(7));
    }

    public List<Map<String, String>> getOrganizations(String orgId) {
        List<Map<String, String>> result = new ArrayList<>();
        PreparedStatement statement = null;
        try {
            if (orgId == null) {
                statement = getConn("config").prepareStatement("SELECT id, orgId, orgName FROM organizations");
            } else {
                statement =
                    getConn("config").prepareStatement("SELECT id, orgId, orgName FROM organizations WHERE orgId = ?");
                statement.setString(1, orgId);
            }
            ResultSet rs = statement.executeQuery();
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
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
                }
            }
            closeConfig();
        }

        return result;
    }

    public String getOrganizationFromProject(String projectId) {
        try (PreparedStatement query = getConn("config").prepareStatement(
            "SELECT organizations.orgId FROM projects JOIN organizations ON projects.orgId = organizations.id WHERE projects.projectId = ?")) {
            query.setString(1, projectId);
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
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
        PreparedStatement query = null;

        try (Connection connection = getConn("config")) {
            if (orgId != null) {
                query = connection.prepareStatement(
                    "SELECT projects.id, projectId, name, organizations.orgId FROM projects JOIN organizations ON organizations.id = projects.orgId WHERE projects.orgId = (SELECT id FROM organizations where orgId = ?)");
                query.setString(1, orgId);
            } else {
                query = connection.prepareStatement(
                    "SELECT projects.id, projectId, name, organizations.orgId FROM projects JOIN organizations ON organizations.id = projects.orgId");
            }

            try (ResultSet rs = query.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> project = new HashMap<>();
                    project.put(Sjm.SYSMLID, rs.getString(2));
                    project.put(Sjm.NAME, rs.getString(3));
                    project.put("orgId", rs.getString(4));
                    result.add(project);
                }
            }

        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
            if (query != null) {
                try {
                    query.close();
                } catch (SQLException e) {
                    logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
                }
            }
        }

        return result;
    }

    public Map<String, Object> getProject(String projectId) {

        Map<String, Object> result = new HashMap<>();

        try (PreparedStatement query = getConn("config").prepareStatement(
            "SELECT projects.id, projectId, name, organizations.orgId FROM projects JOIN organizations ON organizations.id = projects.orgId WHERE projectId = ?")) {
            query.setString(1, projectId);

            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
                    result.put(Sjm.SYSMLID, rs.getString(2));
                    result.put(Sjm.NAME, rs.getString(3));
                    result.put("orgId", rs.getString(4));
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        return result;
    }

    public List<Node> getNodesByType(DbNodeTypes type) {
        return getNodesByType(type, false);
    }

    public List<Node> getNodesByType(DbNodeTypes type, boolean withDeleted) {
        List<Node> result = new ArrayList<>();

        StringBuilder query = new StringBuilder("SELECT * FROM \"nodes" + workspaceId + "\" WHERE nodetype = ?");
        if (!withDeleted) {
            query.append(" AND deleted = false");
        }

        try (PreparedStatement statement = prepareStatement(query.toString())) {
            statement.setInt(1, type.getValue());

            try (ResultSet rs = statement.executeQuery()) {
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

    public List<String> getAllNodes(boolean sysmlid) {
        List<Node> result = new ArrayList<>();
        List<String> ids = new ArrayList<>();

        StringBuilder query =
            new StringBuilder("SELECT * FROM \"nodes" + workspaceId + "\" WHERE deleted = false ORDER BY id");
        try (PreparedStatement statement = prepareStatement(query.toString())) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(resultSetToNode(rs));
                }
            }
            if (sysmlid) {
                ids = result.stream().map(Node::getSysmlId).collect(Collectors.toList());
            } else {
                ids = result.stream().map(Node::getElasticId).collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return ids;
    }

    /**
     * Returns a modified version of all the nodes the database along with the timestamp of the last commit.
     *
     * @return List of Maps
     */
    public List<Map<String, Object>> getAllNodesWithLastCommitTimestamp() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
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
                node.put(DELETED, rs.getBoolean(7));
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

    public List<Map<String, Object>> getAllArtifactsWithLastCommitTimestamp() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            ResultSet rs = execQuery(String.format(
                "SELECT artifacts%1$s.id, artifacts%1$s.elasticid, artifacts%1$s.sysmlid, "
                    + "artifacts%1$s.lastcommit, artifacts%1$s.initialcommit, artifacts%1$s.deleted, commits.timestamp "
                    + "FROM artifacts%1$s JOIN commits ON artifacts%1$s.lastcommit = commits.elasticid "
                    + "WHERE initialcommit IS NOT NULL ORDER BY commits.timestamp;", workspaceId));

            while (rs.next()) {
                Map<String, Object> artifact = new HashMap<>();
                artifact.put(Sjm.ELASTICID, rs.getString(2));
                artifact.put(Sjm.SYSMLID, rs.getString(3));
                artifact.put(LASTCOMMIT, rs.getString(4));
                artifact.put(INITIALCOMMIT, rs.getString(5));
                artifact.put(DELETED, rs.getBoolean(6));
                artifact.put(Sjm.TIMESTAMP, rs.getTimestamp(7));
                result.add(artifact);
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return result;
    }

    public List<Map<String, Object>> getAllNodesWithoutLastCommitTimestamp() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            ResultSet rs = execQuery(String.format(
                "SELECT nodes%1$s.id, nodes%1$s.elasticid, nodes%1$s.sysmlid "
                    + "FROM nodes%1$s "
                    + "WHERE nodes%1$s.lastcommit IS NULL AND deleted IS NOT TRUE;", workspaceId));

            while (rs.next()) {
                Map<String, Object> node = new HashMap<>();
                node.put(Sjm.ELASTICID, rs.getString(2));
                node.put(Sjm.SYSMLID, rs.getString(3));
                result.add(node);
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return result;
    }

    public boolean isDeleted(String sysmlid) {
        try (PreparedStatement query = getConn()
            .prepareStatement("SELECT id FROM \"nodes" + workspaceId + "\" WHERE sysmlid = ? AND deleted = true")) {
            query.setString(1, sysmlid);
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
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

    public boolean sysmlIdExists(String sysmlid) {
        try (PreparedStatement query = getConn()
            .prepareStatement("SELECT id FROM \"nodes" + workspaceId + "\" WHERE sysmlid = ?")) {
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
        try (PreparedStatement query = prepareStatement(
            "SELECT id FROM \"edges" + workspaceId + "\" WHERE parent = (SELECT id FROM \"nodes" + workspaceId
                + "\" WHERE sysmlid = ?) AND child = (SELECT id FROM \"nodes" + workspaceId
                + "\" WHERE sysmlid = ?) AND edgetype = ?")) {
            query.setString(1, parent);
            query.setString(2, child);
            query.setInt(3, dbet.getValue());
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
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

    public List<String> getElasticIdsFromSysmlIdsNodes(List<String> sysmlids, boolean withDeleted) {
        return getElasticIdsFromSysmlIds(sysmlids, withDeleted, "nodes");
    }

    public List<String> getElasticIdsFromSysmlIdsArtifacts(List<String> sysmlids, boolean withDeleted) {
        return getElasticIdsFromSysmlIds(sysmlids, withDeleted, "artifacts");
    }

    @SuppressWarnings("unchecked")
    public List<String> getElasticIdsFromSysmlIds(List<String> sysmlids, boolean withDeleted, String type) {
        if (sysmlids == null || sysmlids.isEmpty()) {
            return new ArrayList<>();
        }

        String query = String.format("SELECT elasticid FROM \"%s%s\" WHERE sysmlid = ANY (?)", type, workspaceId);
        if (!withDeleted) {
            query += " AND deleted = false";
        }

        return new ArrayList(getCollectionFromCollection(sysmlids, query));
    }

    public Node getNodeFromSysmlId(String sysmlId) {
        return getNodeFromSysmlId(sysmlId, false);
    }

    public Node getNodeFromSysmlId(String sysmlId, boolean withDeleted) {
        return (Node) getFromSysmlId(sysmlId, withDeleted, "nodes");
    }

    public Artifact getArtifactFromSysmlId(String sysmlId, boolean withDeleted) {
        return (Artifact) getFromSysmlId(sysmlId, withDeleted, "artifacts");
    }

    public Object getFromSysmlId(String sysmlId, boolean withDeleted, String type) {
        StringBuilder queryString = new StringBuilder("SELECT * FROM \"" + type + workspaceId + "\" WHERE sysmlId = ?");
        if (!withDeleted) {
            queryString.append(" AND deleted = false");
        }
        try (PreparedStatement query = prepareStatement(queryString.toString())) {
            query.setString(1, sysmlId);

            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
                    if (type.equals("nodes")) {
                        return new Node(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getBoolean(7));
                    } else {
                        return new Artifact(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6));
                    }
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return null;
    }

    public Set<String> getElasticIdsNodes() {
        return getElasticIdsNodes(false);
    }

    public Set<String> getElasticIdsNodes(boolean withDeleted) {
        return getElasticIds("nodes", withDeleted);
    }

    public Set<String> getElasticIdsArtifacts() {
        return getElasticIdsArtifacts(false);
    }

    public Set<String> getElasticIdsArtifacts(boolean withDeleted) {
        return getElasticIds("artifacts", withDeleted);
    }

    public Set<String> getElasticIds(String table, boolean withDeleted) {
        Set<String> elasticIds = new HashSet<>();
        try {
            StringBuilder query =
                new StringBuilder(String.format("SELECT elasticid FROM \"%s%s\"", table, workspaceId));
            if (!withDeleted) {
                query.append(" WHERE deleted = false");
            }
            try (ResultSet rs = execQuery(query.toString())) {
                while (rs.next()) {
                    elasticIds.add(rs.getString(1));
                }
            }
            elasticIds.remove("holding_bin");
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return elasticIds;
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

    public String getElasticIdFromSysmlIdArtifact(String sysmlId, boolean withDeleted) {
        if (logger.isDebugEnabled())
            logger.debug("Getting ElasticId for: " + sysmlId);
        Artifact artifact = getArtifactFromSysmlId(sysmlId, withDeleted);
        if (artifact != null) {
            return artifact.getElasticId();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public Set<String> filterListByDeleted(Collection<String> sysmlids, String type) {
        Set<String> deletedIds = new HashSet<>();
        if (sysmlids == null || sysmlids.isEmpty()) {
            return deletedIds;
        }

        String query = String.format("SELECT sysmlid FROM \"%s%s\" WHERE sysmlid = ANY (?) AND deleted = true", type, workspaceId);

        return new HashSet(getCollectionFromCollection(sysmlids, query));
    }

    public Collection<String> getCollectionFromCollection(Collection<String> sysmlids, String query) {
        Collection<String> resultingIds = new ArrayList<>();
        try (PreparedStatement ps = prepareStatement(query)) {
            String[] ids = new String[sysmlids.size()];
            ids = sysmlids.toArray(ids);
            Array arrayIds = getConn().createArrayOf("varchar", ids);
            ps.setArray(1, arrayIds);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultingIds.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return resultingIds;
    }

    public String insertCommit(String elasticId, DbCommitTypes type, String creator) {
        return insertCommit(elasticId, type, creator, null);
    }

    public String insertCommit(String elasticId, DbCommitTypes type, String creator, Timestamp time) {
        try {
            Map<String, Object> map = new HashMap<>();
            // we can hard code the commit type here....but we should still store the integer value
            // from the DB in memory
            map.put("elasticId", elasticId);
            map.put("commitType", type.getValue());
            map.put("refId", workspaceId);
            map.put("creator", creator);
            if (time != null) {
                map.put("timestamp", time);
            }
            insert("commits", map);
        } catch (Exception e) {
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
        try (ResultSet rs = execQuery(String
            .format("SELECT elasticId FROM commits WHERE refId = '%s' ORDER BY timestamp DESC LIMIT 1", workspaceId))) {
            if (rs.next()) {
                return rs.getString(1);
            } else {
                try (PreparedStatement statement = prepareStatement(String.format(
                    "SELECT commits.elasticid FROM refs LEFT JOIN commits ON refs.parentcommit = commits.id WHERE refs.refid = '%s'",
                    workspaceId))) {
                    try (ResultSet nrs = statement.executeQuery()) {
                        if (nrs.next()) {
                            return nrs.getString(1);
                        }
                    }
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
            Map<String, Object> map = new HashMap<>();
            map.put("elasticId", elasticId);
            map.put("sysmlId", sysmlId);
            map.put("nodeType", type.getValue());
            insert("nodes" + workspaceId, map);
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public void executeBulkStatements(String query, List<List<Object>> values) {
        int limit = Integer.parseInt(EmsConfig.get("pg.limit.insert"));
        try (PreparedStatement statement = prepareStatement(query)) {
            int count = 0;
            for (int i = 0; i < values.size(); i++) {
                List<Object> value = values.get(i);
                for (int j = 0; j < value.size(); j++) {
                    statementSetter(statement, value.get(j), j);
                }
                statement.addBatch();
                count++;

                if (count >= limit || i == values.size() - 1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Statement: " + statement.toString());
                    }
                    statement.executeBatch();
                    statement.clearBatch();
                    count = 0;
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public void statementSetter(PreparedStatement statement, Object value, int index) {
        try {
            if (value instanceof String) {
                statement.setString(index + 1, (String) value);
            } else if (value instanceof Integer) {
                statement.setInt(index + 1, (Integer) value);
            } else if (value instanceof Boolean) {
                statement.setBoolean(index + 1, (Boolean) value);
            } else if (value instanceof Timestamp) {
                statement.setTimestamp(index + 1, (Timestamp) value);
            } else if (value == null) {
                statement.setNull(index + 1, Types.NULL);
            } else if (value instanceof JsonPrimitive) {
                JsonPrimitive primitive = ((JsonPrimitive) value).getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    statement.setBoolean(index + 1, primitive.getAsBoolean());
                } else if (primitive.isString()) {
                    statement.setString(index + 1, primitive.getAsString());
                } else if (primitive.isNumber()) {
                    statement.setInt(index + 1, primitive.getAsInt());
                } else {
                    logger.info(String.format("Unable to set value: %s", primitive));
                    logger.info(String.format("Class is: %s", primitive.getClass()));
                }
            }
        } catch (SQLException se) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(se)));
        }
    }

    public int updateElasticId(String sysmlId, String elasticId) {
        try (PreparedStatement statement = getConn()
            .prepareStatement(String.format("UPDATE \"nodes%s\" SET elasticid = ? WHERE sysmlid = ?", workspaceId))) {
            statement.setString(1, elasticId);
            statement.setString(2, sysmlId);
            return statement.executeUpdate();
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return -1;
    }

    public void deleteNode(String sysmlId) {
        try (PreparedStatement query = getConn()
            .prepareStatement("UPDATE \"nodes" + workspaceId + "\" SET deleted = ? WHERE sysmlid = ?")) {
            query.setBoolean(1, true);
            query.setString(2, sysmlId);
            query.execute();
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public void deleteArtifact(String sysmlId) {
        try (PreparedStatement query = getConn()
            .prepareStatement("UPDATE \"artifacts" + workspaceId + "\" SET deleted = ? WHERE sysmlid = ?")) {
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

        String query = String.format(
            "INSERT INTO \"edges%1$s\" (parent, child, edgeType) VALUES ((SELECT id FROM \"nodes%1$s\" WHERE sysmlId = ?), (SELECT id FROM \"nodes%1$s\" WHERE sysmlId = ?), ?)",
            workspaceId);

        try (PreparedStatement statement = prepareStatement(query)) {
            statement.setString(1, parentSysmlId);
            statement.setString(2, childSysmlId);
            statement.setInt(3, edgeType.getValue());
            statement.executeUpdate();
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

    public int getCommitId(String commitId) {
        if (commitId == null) {
            return 0;
        }
        try (PreparedStatement statement = prepareStatement("SELECT id FROM commits WHERE elasticid = ?")) {
            statement.setString(1, commitId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return 0;
    }

    public Map<String, String> getCommitAndTimestamp(String lookUp, Object value) {
        return getCommitAndTimestamp(lookUp, value, "=");
    }

    public Map<String, String> getCommitAndTimestamp(String lookUp, Object value, String operator) {
        return getCommitAndTimestamp(lookUp, value, operator, 0);
    }

    public Map<String, String> getCommitAndTimestamp(String lookUp, Object value, String operator, int limit) {
        Map<String, String> commit = new HashMap<>();
        StringBuilder query = new StringBuilder(String
            .format("SELECT elasticId, timestamp FROM commits WHERE %s %s ?", StringEscapeUtils.escapeSql(lookUp),
                operator));

        if (limit > 0) {
            query.append(" LIMIT ?");
        }

        try (PreparedStatement statement = prepareStatement(query.toString())) {
            statementSetter(statement, value, 0);

            if (limit > 0) {
                statementSetter(statement, limit, 1);
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    commit.put(Sjm.COMMITID, rs.getString(1));
                    commit.put(Sjm.TIMESTAMP, rs.getString(2));
                    return commit;
                }
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
        String query = String.format("SELECT timestamp FROM commits WHERE %s = ?", StringEscapeUtils.escapeSql(lookUp));
        try (PreparedStatement statement = prepareStatement(query)) {
            statement.setString(1, value);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    timestamp = rs.getTimestamp(1).getTime();
                    return timestamp;
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return null;
    }

    public Map<String, Object> getCommit(String commitId) {
        try (PreparedStatement query = prepareStatement(
            "SELECT commits.id, commits.elasticId, commits.refid, commits.timestamp, committype.name, creator FROM commits JOIN committype ON commits.committype = committype.id WHERE elasticId = ?")) {
            query.setString(1, commitId);

            try (ResultSet rs = query.executeQuery()) {
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

            try (ResultSet rs = execQuery(query)) {
                while (rs.next()) {
                    Map<String, String> commit = new HashMap<>();
                    commit.put("commitId", rs.getString(1));
                    commit.put("refId", rs.getString(2));
                    commit.put("timestamp", rs.getString(3));
                    commits.add(commit);
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return commits;
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

            if (n == null) {
                return result;
            }

            String query = "SELECT * FROM get_immediate_parents(?, ?, ?)";

            try (PreparedStatement statement = prepareStatement(query)) {
                statement.setInt(1, n.getId());
                statement.setInt(2, et.getValue());
                statement.setString(3, workspaceId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(new Pair<>(rs.getString(1), rs.getString(2)));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public String getImmediateParentOfType(String sysmlId, DbEdgeTypes et, Set<DbNodeTypes> dnts) {
        Set<Pair<String, String>> immediateParents = getImmediateParents(sysmlId, et);
        while (!immediateParents.isEmpty()) {
            String parentId = null;
            for (Pair<String, String> immediateParent : immediateParents) {
                parentId = immediateParent.first;
                String query = String.format("SELECT nodetype FROM nodes%s WHERE sysmlid = ?", workspaceId);

                try (PreparedStatement statement = prepareStatement(query)) {
                    statement.setString(1, parentId);

                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            // Project nodetype elements should not have immediate parents
                            if (rs.getLong(1) == DbNodeTypes.PROJECT.getValue()) {
                                return null;
                            }
                            for (DbNodeTypes dnt : dnts) {
                                if (dnt.getValue() == rs.getLong(1)) {
                                    return parentId;
                                }
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

        return null;
    }

    public Set<Pair<String, Integer>> getParentsOfType(String sysmlId, DbEdgeTypes dbet) {
        Set<Pair<String, Integer>> result = new HashSet<>();
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null) {
                return result;
            }

            String query = String.format(
                "SELECT N.sysmlid, N.nodetype FROM \"nodes%s\" N JOIN (SELECT * FROM get_parents(?, ?, ?)) P ON N.id = P.id ORDER BY P.height",
                workspaceId);

            try (PreparedStatement statement = prepareStatement(query)) {
                statement.setInt(1, n.getId());
                statement.setInt(2, dbet.getValue());
                statement.setString(3, workspaceId);
                result.add(new Pair<>(n.getSysmlId(), n.getNodeType()));

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(new Pair<>(rs.getString(1), rs.getInt(2)));
                    }
                }
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

            if (n == null) {
                return null;
            }

            String query = String.format(
                "SELECT N.sysmlid, N.elasticid, N.nodetype FROM \"nodes%s\" N JOIN (SELECT * FROM get_parents(?, ?, ?)) P ON N.id = P.id ORDER BY P.height",
                workspaceId);

            try (PreparedStatement statement = prepareStatement(query)) {
                statement.setInt(1, n.getId());
                statement.setInt(2, DbEdgeTypes.CONTAINMENT.getValue());
                statement.setString(3, workspaceId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        if (rs.getInt(3) == DbNodeTypes.SITEANDPACKAGE.getValue()) {
                            return rs.getString(1);
                        } else if (rs.getInt(3) == DbNodeTypes.SITE.getValue()) {
                            return null;
                        }
                    }
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

            if (n == null) {
                return result;
            }

            String query = String.format(
                "SELECT sysmlId, elasticId FROM \"nodes%s\" WHERE id IN (SELECT id FROM get_children(?, ?, ?, ?))",
                workspaceId);

            try (PreparedStatement statement = prepareStatement(query)) {
                statement.setInt(1, n.getId());
                statement.setInt(2, et.getValue());
                statement.setString(3, workspaceId);
                statement.setInt(4, depth);

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(new Pair<>(rs.getString(1), rs.getString(2)));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    // returns list of elasticId
    public List<String> getGroupDocuments(String sysmlId, DbEdgeTypes et, int depth, DbNodeTypes nt) {
        List<String> result = new ArrayList<>();
        try {
            Node n = getNodeFromSysmlId(sysmlId);

            if (n == null) {
                return result;
            }

            String query = String.format(
                "SELECT elasticId FROM \"nodes%s\" WHERE id IN (SELECT id FROM get_group_docs(?, ?, ?, ?, ?, ?))",
                workspaceId);

            try (PreparedStatement statement = prepareStatement(query)) {
                statement.setInt(1, n.getId());
                statement.setInt(2, et.getValue());
                statement.setString(3, workspaceId);
                statement.setInt(4, depth);
                statement.setInt(5, nt.getValue());
                statement.setInt(6, DbNodeTypes.DOCUMENT.getValue());

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.add(rs.getString(1));
                    }
                }
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

            if (n == null) {
                return;
            }

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

            if (n == null) {
                return;
            }

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

    public List<String> findNullParents() {
        List<String> nullParents = new ArrayList<>();
        try (ResultSet rs = execQuery(
            "SELECT n.elasticId FROM \"nodes" + workspaceId + "\" n INNER JOIN (SELECT * FROM \"edges" + workspaceId
                + "\" WHERE edgeType = 1 and parent IS NULL) as e ON (n.id = e.child);");) {
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

    public int createOrganization(String orgId, String orgName) throws PSQLException {
        int recordId = 0;
        try (PreparedStatement query = getConn("config")
            .prepareStatement("SELECT count(id) FROM organizations WHERE orgId = ?")) {
            query.setString(1, orgId);
            if (query.execute()) {
                try (PreparedStatement insertOrg = getConn("config")
                    .prepareStatement("INSERT INTO organizations (orgId, orgName) VALUES (?,?)")) {
                    insertOrg.setString(1, orgId);
                    insertOrg.setString(2, orgName);
                    if (insertOrg.execute()) {
                        try (ResultSet rs = insertOrg.getGeneratedKeys()) {
                            if (rs.next()) {
                                recordId = rs.getInt(1);
                            }
                        }
                    }
                }
            }
        } catch (PSQLException pe) {
            ServerErrorMessage em = pe.getServerErrorMessage();
            logger.warn(em.toString());
            // Do nothing for duplicate found
            if (!em.getConstraint().equals("unique_organizations")) {
                throw pe;
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        return recordId;
    }

    public int updateOrganization(String orgId, String orgName) throws PSQLException {
        int recordId = 0;
        try (PreparedStatement query = getConn("config")
            .prepareStatement("SELECT count(id) FROM organizations WHERE orgId = ?")) {
            query.setString(1, orgId);
            if (query.execute()) {
                try (PreparedStatement updateOrg = getConn("config")
                    .prepareStatement("UPDATE organizations SET orgName = ? WHERE orgId = ?")) {
                    updateOrg.setString(1, orgName);
                    updateOrg.setString(2, orgId);
                    if (updateOrg.execute()) {
                        try (ResultSet rs = updateOrg.getGeneratedKeys()) {
                            if (rs.next()) {
                                recordId = rs.getInt(1);
                            }
                        }
                    }
                }
            }
        } catch (PSQLException pe) {
            ServerErrorMessage em = pe.getServerErrorMessage();
            // Do nothing for duplicate found
            if (!em.getConstraint().equals("unique_organizations")) {
                throw pe;
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        return recordId;
    }

    public void createProjectDatabase(String projectId, String orgId, String name, String location) {
        int organizationId = 0;
        try (PreparedStatement query = getConn("config")
            .prepareStatement("SELECT id FROM organizations WHERE orgId = ?")) {
            query.setString(1, orgId);
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
                    organizationId = rs.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }

        try {
            if (location == null) {
                location = "";
            }
            if (organizationId > 0) {
                try (PreparedStatement insertProject = getConn("config")
                    .prepareStatement("INSERT INTO projects (projectId, name, orgId, location) VALUES (?,?,?,?)")) {
                    insertProject.setString(1, projectId);
                    insertProject.setString(2, name);
                    insertProject.setInt(3, organizationId);
                    insertProject.setString(4, location);
                    insertProject.execute();
                }
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
            try (Statement statement = getConn().createStatement()) {
                statement.execute(String.format("CREATE DATABASE \"_%s\";", projectId));
                statement.execute(String
                    .format("GRANT ALL PRIVILEGES ON DATABASE \"_%s\" TO %s;", projectId, EmsConfig.get("pg.user")));
                statement.execute(String
                    .format("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO %s;", EmsConfig.get("pg.user")));
            }
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
            try (ResultSet exists = execQuery(
                "SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c WHERE c.relname = 'nodetypes' AND c.relkind = 'r')")) {
                if (exists.next()) {
                    if (exists.getBoolean(1)) {
                        return;
                    }
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
                "CREATE TABLE refs(id bigserial primary key, parent text not null, refId text not null unique, refName text not null, parentCommit integer, elasticId text, tag boolean DEFAULT false, timestamp timestamp DEFAULT current_timestamp, deleted boolean DEFAULT false);");
            execUpdate("CREATE INDEX refsIndex on refs(id)");

            execUpdate(
                "CREATE TABLE artifacts(id bigserial primary key, elasticId text not null unique, sysmlId text not null unique, lastCommit text, initialCommit text, deleted boolean default false);");
            execUpdate("CREATE INDEX artifactIndex on artifacts(id);");
            execUpdate("CREATE INDEX sysmlArtifactIndex on artifacts(sysmlId);");

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

            execUpdate("CREATE OR REPLACE FUNCTION get_group_docs(integer, integer, text, integer, integer, integer)\n"
                + "  returns table(id bigint) as $$\n" + "  begin\n" + "    return query\n" + "    execute '\n"
                + "    with recursive children(depth, nid, path, cycle, deleted, ntype) as (\n"
                + "      select 0 as depth, node.id, ARRAY[node.id], false, node.deleted, node.nodetype from ' || format('nodes%s', $3) || '\n"
                + "        node where node.id = ' || $1 || '  union\n"
                + "      select (c.depth + 1) as depth, edge.child as nid, path || cast(edge.child as bigint) as path, edge.child = ANY(path) as cycle, node.deleted as deleted, node.nodetype as ntype \n"
                + "        from ' || format('edges%s', $3) || ' edge, children c, ' || format('nodes%s', $3) || ' node where edge.parent = nid and node.id = edge.child and node.deleted = false and \n"
                + "        edge.edgeType = ' || $2 || ' and not cycle and depth < ' || $4 || ' and (node.nodetype <> '|| $5 ||' or nid = ' || $1 || ') \n"
                + "      )\n" + "      select distinct nid from children where ntype = ' || $6 || ';';\n" + "  end;\n"
                + "$$ language plpgsql;");

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

            execUpdate(String.format(
                "CREATE TABLE artifacts%s (LIKE artifacts%s INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES)",
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
            copyTable("artifacts", childWorkspaceNameSanitized, workspaceId);

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

            if (isTag && (commitId == null || commitId.isEmpty())) {
                setAsTag(childWorkspaceNameSanitized);
            }

        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    private void copyTable(String name, String toRef, String fromRef) {
        try {
            execUpdate(String.format("INSERT INTO %1$s%2$s SELECT * FROM %1$s%3$s", name, sanitizeRefId(toRef),
                sanitizeRefId(fromRef)));
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public boolean isTag(String refId) {
        try (PreparedStatement statement = prepareStatement(
            "SELECT tag FROM refs WHERE (refId = ? OR refName = ?) AND deleted = false")) {
            statement.setString(1, sanitizeRefId(refId));
            statement.setString(2, sanitizeRefId(refId));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return false;
    }

    public void setAsTag(String refId) {
        try (PreparedStatement statement = prepareStatement(
            "UPDATE refs SET tag = true WHERE (refId = ? OR refName = ?) AND deleted = false")) {
            statement.setString(1, sanitizeRefId(refId));
            statement.setString(2, sanitizeRefId(refId));
            statement.executeUpdate();

            execUpdate(String.format("REVOKE INSERT, UPDATE, DELETE ON nodes%1$s, edges%1$s, artifacts%1$s FROM %2$s",
                sanitizeRefId(refId), EmsConfig.get("pg.user")));
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public void deleteRef(String refId) {
        try (PreparedStatement statement = prepareStatement("UPDATE refs SET deleted = true WHERE refId = ?")) {
            statement.setString(1, sanitizeRefId(refId));
            statement.executeUpdate();
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
    }

    public Map<String, String> getRefElastic(String refId) {
        if (refId == null || refId.isEmpty()) {
            refId = "master";
        }
        try (PreparedStatement statement = prepareStatement(
            "SELECT refId, elasticId, parent, tag FROM refs WHERE deleted = false AND refId = ?")) {
            statement.setString(1, sanitizeRefId(refId));
            try (ResultSet rs = statement.executeQuery()) {
                Map<String, String> res = new HashMap<>();
                if (rs.next()) {
                    res.put("refId", rs.getString(1));
                    res.put("elasticId", rs.getString(2));
                    res.put("parent", rs.getString(3));
                    // Tricky needs a class
                    res.put("isTag", String.valueOf(rs.getBoolean(4)));
                    return res;
                }
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
        try (PreparedStatement statement = prepareStatement(
            "SELECT parent, timestamp FROM refs WHERE deleted = false AND refId = ?")) {
            statement.setString(1, sanitizeRefId(refId));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String checkForMaster = (rs.getString(1).equals("") && !refId.equals("master")) ? "master" : rs.getString(1);
                    return new Pair<>(checkForMaster, rs.getTimestamp(2).getTime());
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return null;
    }

    public List<Pair<String, String>> getRefsElastic() {
        return getRefsElastic(false);
    }

    public List<Pair<String, String>> getRefsElastic(boolean includeDeleted) {
        List<Pair<String, String>> result = new ArrayList<>();
        StringBuilder query = new StringBuilder("SELECT refId, elasticId FROM refs ");
        if (!includeDeleted) {
            query.append("WHERE deleted = false ");
        }
        query.append("ORDER BY timestamp ASC");

        try (PreparedStatement statement = prepareStatement(query.toString())) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new Pair<>(rs.getString(1), rs.getString(2)));
                }
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
        return getRefsCommits(refId, commitId, 0);
    }

    public List<Map<String, Object>> getRefsCommits(String refId, int commitId, int limit) {
        return getRefsCommits(refId, commitId, null, limit, 0);
    }

    public List<Map<String, Object>> getRefsCommits(String refId, Timestamp timestamp, int limit) {
        return getRefsCommits(refId, 0, timestamp, limit, 0);
    }

    public List<Map<String, Object>> getRefsCommits(String refId, int commitId, Timestamp timestamp, int limit,
        int count) {

        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String refIdString = sanitizeRefId(refId);
            if (refId.equals("master")) {
                refId = "";
            }
            if (refIdString.equals("")) {
                refIdString = "master";
            }

            int commitColNum = 0;
            int limitColNum = 0;
            int timestampColNum = 0;

            StringBuilder query = new StringBuilder(
                "SELECT elasticId, creator, timestamp, refId, commitType.name FROM commits JOIN commitType ON commitType.id = commits.commitType WHERE (refId = ? OR refId = ?)");

            if (commitId != 0) {
                query.append(" AND timestamp <= (SELECT timestamp FROM commits WHERE id = ?)");
                commitColNum = 3;
            }

            if (timestamp != null) {
                query.append(" AND date_trunc('milliseconds', timestamp) <= ?");
                timestampColNum = commitColNum == 3 ? 4 : 3;
            }

            query.append(" ORDER BY timestamp DESC");

            if (limit != 0) {
                query.append(" LIMIT ?");
                limitColNum = (commitColNum == 3 || timestampColNum >= 3) ? (timestampColNum == 4 ? 5 : 4) : 3;
            }

            try (PreparedStatement statement = prepareStatement(query.toString())) {
                statement.setString(1, refId);
                statement.setString(2, refIdString);
                if (commitId != 0) {
                    statement.setInt(commitColNum, commitId);
                }
                if (timestampColNum != 0) {
                    statement.setTimestamp(timestampColNum, timestamp);
                }
                if (limit != 0) {
                    statement.setInt(limitColNum, limit);
                }

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        if (limit == 0 || count < limit) {
                            Map<String, Object> commit = new HashMap<>();
                            commit.put(Sjm.SYSMLID, rs.getString(1));
                            commit.put(Sjm.CREATOR, rs.getString(2));
                            commit.put(Sjm.CREATED, rs.getTimestamp(3));
                            commit.put("refId", rs.getString(4));
                            commit.put("commitType", rs.getString(5));
                            result.add(commit);
                            count++;
                        }
                    }

                    try (PreparedStatement parentStatement = prepareStatement(
                        "SELECT parent, parentCommit FROM refs WHERE refId = ? OR refId = ?")) {
                        parentStatement.setString(1, refId);
                        parentStatement.setString(2, refIdString);

                        try (ResultSet nrs = parentStatement.executeQuery()) {
                            if (nrs.next() && nrs.getInt(2) != 0 && (limit == 0 || count < limit)) {
                                String nextRefId = nrs.getString(1);
                                result.addAll(getRefsCommits(nextRefId, nrs.getInt(2), timestamp, limit, count));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }
        return result;
    }

    public String getProjectInitialCommit() {
        try (PreparedStatement statement = prepareStatement(
            "SELECT elasticid FROM commits WHERE id = (SELECT min(id) FROM commits)")) {
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            close();
        }

        return null;
    }

    public List<Pair<String, String>> getTags() {
        List<Pair<String, String>> result = new ArrayList<>();
        try (ResultSet rs = execQuery("SELECT refId, elasticId FROM refs WHERE tag = true AND deleted = false")) {
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
            Map<String, Object> map = new HashMap<>();
            map.put("refId", newWorkspaceId);
            map.put("refName", newWorkspaceName);
            map.put("parent", parent);
            map.put("parentCommit", headCommit);
            map.put("elasticId", elasticId);
            map.put("tag", isTag);
            insert("refs", map);
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public void updateRef(String refId, String refName, String elasticId, boolean isTag) {
        try (PreparedStatement statement = prepareStatement(
            "UPDATE refs SET refName = ?, elasticId = ?, tag = ? WHERE refId = ?")) {
            statement.setString(1, refName);
            statement.setString(2, elasticId);
            statement.setBoolean(3, isTag);
            statement.setString(4, refId);
            statement.executeUpdate();
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public boolean orgExists(String orgId) {
        try (PreparedStatement query = getConn("config")
            .prepareStatement("SELECT count(id) FROM organizations WHERE orgId = ?")) {
            query.setString(1, orgId);
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
                    if (rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            closeConfig();
        }
        return false;
    }

    public boolean refExists(String refId) {
        String currentWorkspace = this.workspaceId;
        this.workspaceId = "";
        refId = sanitizeRefId(refId);
        try {
            connect();
            try (PreparedStatement query = prepareStatement("SELECT count(id) FROM refs WHERE refId = ?")) {
                query.setString(1, refId);
                try (ResultSet rs = query.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getInt(1) > 0) {
                            return true;
                        }
                    }
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
        // SQL Injection Vulnerability avoidance tryhard
        String sanitizedDBName = StringEscapeUtils.escapeSql(databaseName);
        String query = "ALTER DATABASE  \"_" + sanitizedDBName + "\" CONNECTION LIMIT 0";
        String query2 =
            "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = \'_" + sanitizedDBName + "\'";
        String query3 = "DROP DATABASE \"_" + sanitizedDBName + "\";";

        try (Statement statement = getConn("config").createStatement();
            CallableStatement callable = getConn("config").prepareCall(query2)) {
            statement.executeUpdate(query);
            callable.execute();
            statement.executeUpdate(query3);

            // Should only try to remove the connection from postgres if the queries succeed.
            PostgresPool.removeConnection(EmsConfig.get("pg.host"), databaseName);
        } catch (SQLException e) {

            logger.error(String.format("%s", LogUtil.getStackTrace(e)));

            // If any of the queries fail, reset the connection limit of the database
            query = "ALTER DATABASE  \"_" + databaseName + "\" CONNECTION LIMIT -1";
            try (Statement statement = getConn("config").createStatement()) {
                statement.executeUpdate(query);
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
        String query = "DELETE FROM projects WHERE projectid = ?";

        try (PreparedStatement statement = getConn("config").prepareStatement(query)) {
            statement.setString(1, projectId);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        closeConfig();
    }

    public static String sanitizeRefId(String refId) {
        return StringEscapeUtils.escapeSql(refId.replace("-", "_").replaceAll("\\s+", ""));
    }

    public boolean deleteOrganization(String orgId) {
        boolean orgDeleted = false;

        try (PreparedStatement query = getConn("config")
            .prepareStatement("DELETE FROM organizations WHERE orgid = ?")) {
            query.setString(1, orgId);
            query.executeUpdate();
            orgDeleted = true;
        } catch (SQLException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        closeConfig();

        return orgDeleted;
    }
}
