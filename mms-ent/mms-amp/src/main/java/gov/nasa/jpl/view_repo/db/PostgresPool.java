package gov.nasa.jpl.view_repo.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;

import gov.nasa.jpl.view_repo.util.EmsConfig;

public class PostgresPool {

    private static final int MAX_IDLE_CONN = 2;
    private static final int MAX_ACTIVE_CONN = 96;

    private static final String PG_CONN_MAX = "pg.conn.max";
    private static final String PG_USER = "pg.user";
    private static final String PG_PASS = "pg.pass";

    private String connectString;
    private static Map<String, PostgresPool> dataSource = new HashMap<>();
    private Map<String, BasicDataSource> bds = new HashMap<>();

    private PostgresPool(String connectString) {
        this.connectString = connectString;
        if (!bds.containsKey(connectString)) {
            bds.put(connectString, new BasicDataSource());
            bds.get(connectString).setDriverClassName("org.postgresql.Driver");
            bds.get(connectString).setUrl(connectString);
            bds.get(connectString).setUsername(EmsConfig.get(PG_USER));
            bds.get(connectString).setPassword(EmsConfig.get(PG_PASS));
            bds.get(connectString).setInitialSize(10);
            bds.get(connectString).setMaxIdle(MAX_IDLE_CONN);
            bds.get(connectString).setMaxActive((!EmsConfig.get(PG_CONN_MAX).equals("")) ? Integer.parseInt(EmsConfig.get(
                PG_CONN_MAX)) : MAX_ACTIVE_CONN);
            bds.get(connectString).setMaxWait(10000);
            bds.get(connectString).setDefaultAutoCommit(true);
            bds.get(connectString).setRemoveAbandonedTimeout(1);
            bds.get(connectString).setLogAbandoned(true);
            bds.get(connectString).setRemoveAbandoned(true);
        }
    }

    public static PostgresPool getInstance(String host, String name) {
        String connectString = host + name;
        if (!dataSource.containsKey(connectString)) {
            dataSource.put(connectString, newInstance(host, name));
        }
        return dataSource.get(connectString);
    }

    private static PostgresPool newInstance(String host, String name) {
        return new PostgresPool(host + name);
    }

    public Connection getConnection() throws SQLException {
        return this.bds.get(connectString).getConnection();
    }

    public static Connection getStandaloneConnection(String host, String name) throws SQLException {
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName("org.postgresql.Driver");
        bds.setUrl(host + name);
        bds.setUsername(EmsConfig.get(PG_USER));
        bds.setPassword(EmsConfig.get(PG_PASS));
        bds.setInitialSize(10);
        bds.setMaxIdle(MAX_IDLE_CONN);
        bds.setMaxActive(
            (!EmsConfig.get(PG_CONN_MAX).equals("")) ? Integer.parseInt(EmsConfig.get(PG_CONN_MAX)) : MAX_ACTIVE_CONN);
        bds.setMaxWait(10000);
        bds.setDefaultAutoCommit(true);
        bds.setRemoveAbandonedTimeout(1);
        return bds.getConnection();
    }

    /**
     * Given a host and name this will remove the connection source from the PostgresPool
     * @param host
     * @param name
     */
    static void removeConnection(String host, String name) {
        String connectString;
        if (!name.startsWith("_")) {
            connectString = host + "_" + name;
        } else {
            connectString = host + name;
        }
        if (dataSource.containsKey(connectString)) {
            dataSource.remove(connectString);
        }
    }
}
