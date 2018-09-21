package gov.nasa.jpl.view_repo.db;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.DelegatingConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.atLeast;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Created by lm392c on 4/19/2018.
 */
public class PostgresPoolTest {


    @Before
    public void setup() {
        EmsConfig.setProperty(PostgresPool.PG_CONN_MAX, "96");
        EmsConfig.setProperty(PostgresPool.PG_SEC, "true");
    }

    @After
    public void teardown() {
        PostgresPool.purgeDatabasePools();
    }

    @Test
    public void testPoolCreationGoActive() throws SQLException {

        PostgresPool.IBasicDataSourceFactory mockFactory = mock(PostgresPool.IBasicDataSourceFactory.class);
        PostgresPool.setBasicDataSourceFactory(mockFactory);

        BasicDataSource mockBasicDataSource = mock(BasicDataSource.class);
        Connection mockConnection = mock(Connection.class);

        when(mockFactory.getNewBasicDataSource()).thenReturn(mockBasicDataSource);
        when(mockBasicDataSource.getConnection()).thenReturn(mockConnection);

        Connection connection = PostgresPool.getInstance("host", "name").getConnection();

        assertSame(mockConnection, connection);

        verify(mockBasicDataSource, atLeast(1)).setMaxIdle(PostgresPool.MAX_IDLE_CONN);
        verify(mockBasicDataSource, never()).setMaxIdle(PostgresPool.MIN_IDLE_CONN);
    }

    @Test
    public void testPoolNoActiveOverflowBySize() throws SQLException {

        PostgresPool.IBasicDataSourceFactory mockFactory = mock(PostgresPool.IBasicDataSourceFactory.class);
        PostgresPool.setBasicDataSourceFactory(mockFactory);

        BasicDataSource mockBasicDataSource = mock(BasicDataSource.class);
        Connection mockConnection = mock(Connection.class);

        when(mockFactory.getNewBasicDataSource()).thenReturn(mockBasicDataSource);
        when(mockBasicDataSource.getConnection()).thenReturn(mockConnection);

        when(mockBasicDataSource.getNumIdle()).thenReturn(1);
        when(mockBasicDataSource.getNumActive()).thenReturn(1);

        for(int count = 0; count < PostgresPool.MAX_CONN_LIMIT/4; ++count){
            PostgresPool.getInstance("host", String.valueOf(count)).getConnection();
        }

        verify(mockBasicDataSource, never()).setMaxIdle(PostgresPool.MIN_IDLE_CONN);
    }

    @Test
    public void testPoolActiveOverflow() throws SQLException {

        PostgresPool.IBasicDataSourceFactory mockFactory = mock(PostgresPool.IBasicDataSourceFactory.class);
        PostgresPool.setBasicDataSourceFactory(mockFactory);

        BasicDataSource mockBasicDataSource = mock(BasicDataSource.class);
        Connection mockConnection = mock(Connection.class);

        when(mockFactory.getNewBasicDataSource()).thenReturn(mockBasicDataSource);
        when(mockBasicDataSource.getConnection()).thenReturn(mockConnection);

        when(mockBasicDataSource.getNumIdle()).thenReturn(1);
        when(mockBasicDataSource.getNumActive()).thenReturn(1);

        for(int count = 0; count < PostgresPool.MAX_CONN_LIMIT/2 + 1; ++count){
            PostgresPool.getInstance("host", String.valueOf(count)).getConnection();
        }

        verify(mockBasicDataSource, atLeast(1)).setMaxIdle(PostgresPool.MIN_IDLE_CONN);
    }

    @Test
    public void testPoolNoActiveOverflowByMultiHost() throws SQLException {

        PostgresPool.IBasicDataSourceFactory mockFactory = mock(PostgresPool.IBasicDataSourceFactory.class);
        PostgresPool.setBasicDataSourceFactory(mockFactory);

        BasicDataSource mockBasicDataSource = mock(BasicDataSource.class);
        Connection mockConnection = mock(Connection.class);

        when(mockFactory.getNewBasicDataSource()).thenReturn(mockBasicDataSource);
        when(mockBasicDataSource.getConnection()).thenReturn(mockConnection);

        when(mockBasicDataSource.getNumIdle()).thenReturn(1);
        when(mockBasicDataSource.getNumActive()).thenReturn(1);

        for(int count = 0; count < PostgresPool.MAX_CONN_LIMIT/2 + 1; ++count){
            PostgresPool.getInstance(String.format("host%d", count), String.valueOf(count)).getConnection();
        }

        verify(mockBasicDataSource, never()).setMaxIdle(PostgresPool.MIN_IDLE_CONN);
    }

    @Test
    public void testPoolInactiveToActive() throws SQLException {
        PostgresPool.IBasicDataSourceFactory mockFactory = mock(PostgresPool.IBasicDataSourceFactory.class);
        PostgresPool.setBasicDataSourceFactory(mockFactory);

        BasicDataSource mockBasicDataSource = mock(BasicDataSource.class);
        Connection mockConnection = mock(Connection.class);

        when(mockFactory.getNewBasicDataSource()).thenReturn(mockBasicDataSource);
        when(mockBasicDataSource.getConnection()).thenReturn(mockConnection);

        when(mockBasicDataSource.getNumIdle()).thenReturn(1);
        when(mockBasicDataSource.getNumActive()).thenReturn(1);

        PostgresPool instance = PostgresPool.getInstance("host", "name");
        instance.getConnection();

        reset(mockBasicDataSource);

        PostgresPool.idleDatabasePools();

        verify(mockBasicDataSource, atLeast(1)).setMaxIdle(PostgresPool.MIN_IDLE_CONN);
        verify(mockBasicDataSource, never()).setMaxIdle(PostgresPool.MAX_IDLE_CONN);

        reset(mockBasicDataSource);

        PostgresPool instance2 = PostgresPool.getInstance("host", "name");
        instance2.getConnection();

        assertSame(instance, instance2);

        verify(mockBasicDataSource, never()).setMaxIdle(PostgresPool.MIN_IDLE_CONN);
        verify(mockBasicDataSource, atLeast(1)).setMaxIdle(PostgresPool.MAX_IDLE_CONN);
    }

}
