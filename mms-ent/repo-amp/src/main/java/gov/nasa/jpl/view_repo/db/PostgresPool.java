package gov.nasa.jpl.view_repo.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import org.apache.commons.dbcp.BasicDataSource;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import org.apache.log4j.Logger;

public class PostgresPool {
    static Logger logger = Logger.getLogger(PostgresPool.class);

    static final int MAX_IDLE_CONN = 2;
    static final int MIN_IDLE_CONN = 0;
    static final int MAX_ACTIVE_CONN = 96;
    static final int MAX_CONN_LIMIT = 192;

    static final String PG_CONN_MAX = "pg.conn.max";
    static final String PG_USER = "pg.user";
    static final String PG_PASS = "pg.pass";
    static final String PG_SEC = "pg.secured";

    private String host;
    private String name;
    private String connectString;
    private BasicDataSource bds = null;

    private static Map<String, PostgresPool> dataSources = new HashMap<>();
    private static Map<String, Cache<String, PostgresPool>> activeDataSources = new HashMap<>();


    interface IBasicDataSourceFactory {
        BasicDataSource getNewBasicDataSource();
    }


    private static IBasicDataSourceFactory basicDataSourceFactory = null;

    private PostgresPool(String host, String name) {
        this.host = host;
        this.name = name;
        this.connectString = getConnectString(host, name);

        this.bds = getBasicDataSourceFactory().getNewBasicDataSource();
        this.bds.setDriverClassName("org.postgresql.Driver");
        this.bds.setUrl(this.connectString);
        this.bds.setUsername(EmsConfig.get(PG_USER));
        this.bds.setPassword(EmsConfig.get(PG_PASS));
        this.bds.setInitialSize(10);
        this.bds.setMaxIdle(MAX_IDLE_CONN);
        this.bds.setMinIdle(MIN_IDLE_CONN);
        this.bds.setMaxActive(
            (!EmsConfig.get(PG_CONN_MAX).equals("")) ? Integer.parseInt(EmsConfig.get(PG_CONN_MAX)) : MAX_ACTIVE_CONN);
        this.bds.setMaxWait(10000);
        this.bds.setDefaultAutoCommit(true);
        this.bds.setRemoveAbandonedTimeout(1);
        this.bds.setLogAbandoned(true);
        this.bds.setRemoveAbandoned(true);
        this.bds.setTimeBetweenEvictionRunsMillis(1000 * 60);
        this.bds.setMinEvictableIdleTimeMillis(1000 * 60 * 5);

        if (EmsConfig.get(PG_SEC) != null && EmsConfig.get(PG_SEC).equalsIgnoreCase("true")) {
            this.bds.setConnectionProperties("ssl=true");
        }
    }

    private static String getConnectString(String host, String name) {
        return host + name;
    }

    public static synchronized PostgresPool getInstance(String host, String name) {
        String connectString = getConnectString(host, name);
        Cache<String, PostgresPool> cache = getActiveDatasetCache(host);
        PostgresPool pool = cache.getIfPresent(connectString);
        if (pool == null) {
            pool = dataSources.get(connectString);
            if (pool == null) {
                pool = newInstance(host, name);
                dataSources.put(connectString, pool);
            }
        }
        return pool;
    }

    private static PostgresPool newInstance(String host, String name) {
        return new PostgresPool(host, name);
    }

    public Connection getConnection() throws SQLException {
        goActive();
        Connection connection = this.bds.getConnection();
        updatePool();
        return connection;
    }

    private void updatePool() {
        getActiveDatasetCache(host).put(this.connectString, this);
    }

    private int getWeight() {
        return this.bds.getNumActive() + this.bds.getNumIdle();
    }

    private void goActive() {
        this.bds.setMaxIdle(MAX_IDLE_CONN);
    }

    private void goIdle() {
        this.bds.setMaxIdle(MIN_IDLE_CONN);
        if (this.bds.getNumActive() > 0) {
            logger.warn("Database connection pool (" + getConnectString(host, name)
                + ") with active connections was instructed to go idle.  Database may be overloaded");
        }
    }

    public static Connection getStandaloneConnection(String host, String name) throws SQLException {
        BasicDataSource bds = getBasicDataSourceFactory().getNewBasicDataSource();
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
        bds.setTimeBetweenEvictionRunsMillis(1000 * 60);
        bds.setMinEvictableIdleTimeMillis(1000 * 60 * 60);
        return bds.getConnection();
    }

    /**
     * Given a host and name this will remove the connection source from the PostgresPool
     *
     * @param host
     * @param name
     */
    static void removeConnection(String host, String name) {
        String connectString;
        //TODO: Why is this different here??
        if (!name.startsWith("_")) {
            connectString = host + "_" + name;
        } else {
            connectString = host + name;
        }
        dataSources.remove(connectString);
        getActiveDatasetCache(host).invalidate(connectString);
    }

    private static Cache<String, PostgresPool> getActiveDatasetCache(String host) {
        Cache<String, PostgresPool> cache = activeDataSources.get((host));

        if (cache == null) {
            cache = CacheBuilder.newBuilder().maximumWeight(MAX_CONN_LIMIT)
                .weigher((String key, PostgresPool pool) -> pool.getWeight()).expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener((RemovalNotification<String, PostgresPool> v) -> {
                    if (v.getCause() != RemovalCause.REPLACED) {
                        v.getValue().goIdle();
                    }
                }).build();
            activeDataSources.put(host, cache);
        }
        return cache;
    }


    public static IBasicDataSourceFactory getBasicDataSourceFactory() {
        if (basicDataSourceFactory == null) {
            basicDataSourceFactory = () -> new BasicDataSource();
        }
        return basicDataSourceFactory;
    }

    public static void setBasicDataSourceFactory(IBasicDataSourceFactory basicDataSourceFactory) {
        PostgresPool.basicDataSourceFactory = basicDataSourceFactory;
    }

    static void purgeDatabasePools() {
        activeDataSources.clear();
        dataSources.clear();
    }

    static void idleDatabasePools() {
        for (Cache<String, PostgresPool> cache : activeDataSources.values()) {
            cache.invalidateAll();
        }
    }

}
