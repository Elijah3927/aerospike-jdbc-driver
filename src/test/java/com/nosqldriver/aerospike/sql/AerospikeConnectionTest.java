package com.nosqldriver.aerospike.sql;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

import static com.nosqldriver.aerospike.sql.TestDataUtils.aerospikePort;
import static com.nosqldriver.aerospike.sql.TestDataUtils.getTestConnection;
import static java.lang.String.format;
import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT;
import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.HOLD_CURSORS_OVER_COMMIT;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AerospikeConnectionTest {
    private final String JDBC_LOCAL = format("jdbc:aerospike:%s", TestDataUtils.aerospikeHost);
    private final String JDBC_LOCAL_TEST = format("%s/%s", JDBC_LOCAL, "test");
    private Connection testConn = getTestConnection();

    @Test
    void createValidConnectionWithoutNamespace() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL, new Properties());
        assertTrue(conn.isValid(1));
        conn.setReadOnly(true);
        assertTrue(conn.isReadOnly());
        conn.setReadOnly(false);
        assertFalse(conn.isReadOnly());

        assertNull(conn.getCatalog());
        conn.setCatalog(null);
        conn.setCatalog("something");
        assertEquals("something", conn.getCatalog());

        assertNull(conn.getSchema());
        conn.setCatalog(null);
        conn.setCatalog("something"); // ignored
        assertNull(conn.getSchema());

        conn.close();
        assertFalse(conn.isValid(1));
        assertThrows(SQLException.class, () -> conn.setReadOnly(true));
        assertThrows(SQLException.class, () -> conn.setReadOnly(false));

    }

    @Test
    void createValidConnectionWithPort() throws SQLException {
        assertConnectionIsClosed(format("%s:%d", JDBC_LOCAL, aerospikePort), new Properties());
    }


    @Test
    void createValidConnectionWithNamespace() throws SQLException {
        assertConnectionIsClosed(JDBC_LOCAL_TEST, new Properties());
    }

    @Test
    void validateNetworkTimeout() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL, new Properties());
        int defaultTimeout = conn.getNetworkTimeout();
        assertEquals(0, defaultTimeout);
        conn.setNetworkTimeout(null, 12345);
        assertEquals(12345, conn.getNetworkTimeout());
    }

    private void assertConnectionIsClosed(String url, Properties props) throws SQLException {
        Connection conn = new AerospikeConnection(url, props);
        assertFalse(conn.isClosed());
        conn.close();
        assertTrue(conn.isClosed());
    }

    @Test
    void createInValidConnectionWrongHost() {
        assertThrows(SQLException.class, () -> new AerospikeConnection("jdbc:aerospike:someotherhostthatdoesnotexist", new Properties()));
    }

    @Test
    void createInValidConnectionWrongPort() {
        assertThrows(SQLException.class, () -> new AerospikeConnection(format("%s:%d", JDBC_LOCAL, 4321), new Properties()));
    }


    @Test
    void createStatement() throws SQLException {
        assertNotNull(testConn.createStatement());
        assertNotNull(testConn.createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY));
        assertNotNull(testConn.createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY, testConn.getHoldability()));

        assertThrows(SQLException.class, () -> testConn.createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY, 123));
        assertThrows(SQLException.class, () -> testConn.createStatement(TYPE_FORWARD_ONLY, 123, testConn.getHoldability()));
        assertThrows(SQLException.class, () -> testConn.createStatement(123, CONCUR_READ_ONLY, testConn.getHoldability()));
    }

    @Test
    void prepareStatement() throws SQLException {
        String query = "select 1";
        assertNotNull(testConn.prepareStatement(query));
        assertNotNull(testConn.prepareStatement(query, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY));
        assertNotNull(testConn.prepareStatement(query, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY, testConn.getHoldability()));

        assertThrows(SQLException.class, () -> testConn.prepareStatement(query, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY, 123));
        assertThrows(SQLException.class, () -> testConn.prepareStatement(query, TYPE_FORWARD_ONLY, 123, testConn.getHoldability()));
        assertThrows(SQLException.class, () -> testConn.prepareStatement(query, 123, CONCUR_READ_ONLY, testConn.getHoldability()));

        // autogenerated keys requirement is ignored if queyry is not insert
        assertNotNull(testConn.prepareStatement(query, RETURN_GENERATED_KEYS));
        assertNotNull(testConn.prepareStatement(query, new int[] {1}));
        assertNotNull(testConn.prepareStatement(query, new String[] {"id"}));
    }


    @Test
    void holdability() throws SQLException {
        // default value
        assertEquals(HOLD_CURSORS_OVER_COMMIT, testConn.getHoldability());
        // set same value as default
        testConn.setHoldability(HOLD_CURSORS_OVER_COMMIT);
        assertEquals(HOLD_CURSORS_OVER_COMMIT, testConn.getHoldability());

        // set other valid value
        testConn.setHoldability(CLOSE_CURSORS_AT_COMMIT);
        assertEquals(CLOSE_CURSORS_AT_COMMIT, testConn.getHoldability());
        // try to set invalid value and check that exception was thrown and previous value was not changed
        assertThrows(SQLException.class, () -> testConn.setHoldability(12345));
        assertEquals(CLOSE_CURSORS_AT_COMMIT, testConn.getHoldability());

        // restore default
        testConn.setHoldability(HOLD_CURSORS_OVER_COMMIT);
        assertEquals(HOLD_CURSORS_OVER_COMMIT, testConn.getHoldability());

        // try to set invalid value and check that exception was thrown and previous value was not changed
        assertThrows(SQLException.class, () -> testConn.setHoldability(12345));
        assertEquals(HOLD_CURSORS_OVER_COMMIT, testConn.getHoldability());
    }

    @Test
    void commit() throws SQLException {
        testConn.clearWarnings();
        assertTrue(testConn.getAutoCommit());
        testConn.setAutoCommit(true);
        assertNull(testConn.getWarnings()); // transaction type was not changed, so the setAutoCommit() is no-op
        testConn.commit(); //ignored

        testConn.setAutoCommit(false);
        assertEquals("Aerospike does not  support transactions and therefore behaves like autocommit ON", testConn.getWarnings().getMessage());
        assertFalse(testConn.getAutoCommit());
        testConn.clearWarnings();
        testConn.setAutoCommit(true);
        assertNull(testConn.getWarnings()); // transaction type was changed to auto-commit - no warnings
    }


    @Test
    void unsupported() throws SQLException {
        String query = "select 1";
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.setSavepoint());
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.setSavepoint("something"));
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.rollback());
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.rollback(null));
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.releaseSavepoint(null));

        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.prepareCall(query));
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.prepareCall(query, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY));
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.prepareCall(query, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY, testConn.getHoldability()));

        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.prepareStatement("insert into mytest (id) values (?)", RETURN_GENERATED_KEYS));
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.prepareStatement("insert into mytest (id) values (?)", new int[] {1}));
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.prepareStatement("insert into mytest (id) values (?)", new String[] {"id"}));

        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.createSQLXML());
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.createStruct("person", new Object[0]));

        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.nativeSQL(query));


        assertEquals(TRANSACTION_NONE, testConn.getTransactionIsolation());
        testConn.setTransactionIsolation(TRANSACTION_NONE);
        assertEquals(TRANSACTION_NONE, testConn.getTransactionIsolation());
        assertThrows(SQLFeatureNotSupportedException.class, () -> testConn.setTransactionIsolation(TRANSACTION_READ_UNCOMMITTED));
        assertEquals(TRANSACTION_NONE, testConn.getTransactionIsolation());
   }


    @Test
    void validateClientInfo() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL, new Properties());
        assertTrue(conn.getClientInfo().isEmpty());
        conn.setClientInfo("test1", "value1");
        Properties info1 = conn.getClientInfo();
        assertEquals(1, info1.size());
        assertEquals("value1", info1.getProperty("test1"));
        assertEquals("value1", conn.getClientInfo("test1"));
        conn.close();
    }

    @Test
    void validateClientInfoSetAll() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL, new Properties());
        assertTrue(conn.getClientInfo().isEmpty());

        Properties props = new Properties();
        props.setProperty("test1", "value1");
        props.setProperty("test2", "value2");

        conn.setClientInfo(props);
        Properties info1 = conn.getClientInfo();
        assertEquals(props.size(), info1.size());
        assertEquals("value1", info1.getProperty("test1"));
        assertEquals("value2", info1.getProperty("test2"));
        conn.close();
    }

    @Test
    void validateClosedConnection() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL, new Properties());
        conn.setHoldability(HOLD_CURSORS_OVER_COMMIT); // success
        conn.close();
        // same call on closed connection fails
        assertThrows(SQLException.class, () -> conn.setHoldability(HOLD_CURSORS_OVER_COMMIT));
    }



    @Test
    void validateTypeMap() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL, new Properties());
        assertTrue(conn.getTypeMap().isEmpty());
        class Athletes {}
        conn.setTypeMap(singletonMap("mySchemaName.ATHLETES", Athletes.class));

        Map<String, Class<?>> types = conn.getTypeMap();
        assertEquals(1, types.size());
        assertEquals(Athletes.class, types.get("mySchemaName.ATHLETES"));
    }

    @Test
    void warnings() throws SQLException {
        assertNull(testConn.getWarnings());
        testConn.clearWarnings();
        assertNull(testConn.getWarnings());
    }

    @Test
    void noShema() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL, new Properties());
        assertNull(conn.getSchema());
        assertNull(conn.getCatalog());
    }

    @Test
    void shema() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL_TEST, new Properties());
        assertEquals("test", conn.getCatalog());
        assertNull(conn.getSchema());
    }

    @Test
    void changeShema() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL_TEST, new Properties());
        assertNull(conn.getSchema());
        assertEquals("test", conn.getCatalog());
        conn.setSchema("test2");
        assertNull(conn.getSchema());
        assertEquals("test", conn.getCatalog());
    }

    @Test
    void abort() throws SQLException {
        Connection conn = new AerospikeConnection(JDBC_LOCAL_TEST, new Properties());
        assertFalse(conn.isClosed());
        conn.abort(Executors.newSingleThreadExecutor());
        await().atMost(5, SECONDS).until(conn::isClosed, c -> c);
        assertTrue(conn.isClosed());
    }
}