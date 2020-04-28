package com.nosqldriver.aerospike.sql;

import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.nosqldriver.aerospike.sql.TestDataUtils.INSTRUMENTS;
import static com.nosqldriver.aerospike.sql.TestDataUtils.NAMESPACE;
import static com.nosqldriver.aerospike.sql.TestDataUtils.PEOPLE;
import static com.nosqldriver.aerospike.sql.TestDataUtils.aerospikeTestUrl;
import static com.nosqldriver.aerospike.sql.TestDataUtils.executeQuery;
import static com.nosqldriver.aerospike.sql.TestDataUtils.getTestConnection;
import static com.nosqldriver.sql.DataColumn.DataColumnRole.DATA;
import static com.nosqldriver.sql.DataColumn.DataColumnRole.PK;
import static java.lang.String.format;
import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENTS_PLACEHOLDER;

public class SelectWithPkTest {
    private static Connection queryKeyConn;
    @BeforeAll
    static void init() throws SQLException {
        dropAll();
        WritePolicy p = new WritePolicy();
        p.sendKey = true;
        TestDataUtils.writeBeatles(p);
        TestDataUtils.writeMainPersonalInstruments(p);
        queryKeyConn = DriverManager.getConnection(aerospikeTestUrl + "?policy.query.sendKey=true");
    }

    static void dropAll() {
        TestDataUtils.deleteAllRecords(NAMESPACE, PEOPLE);
        TestDataUtils.deleteAllRecords(NAMESPACE, INSTRUMENTS);
        TestDataUtils.deleteAllRecords(NAMESPACE, TestDataUtils.DATA);
    }

    @AfterAll
    static void tearDown() throws SQLException {
        dropAll();
        queryKeyConn.close();
    }

    @ParameterizedTest(name = ARGUMENTS_PLACEHOLDER)
    @ValueSource(strings = {
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where PK=0",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where PK=1",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where PK in (1)",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where PK in (1, 2, 3, 4)",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where id=0",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where id=1",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where id in (1)",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where id in (1, 2, 3, 4)",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where 2=1",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where 2=1+1",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people limit 0",
    })
    void metadataOneTableWithPk(String sql) throws SQLException {
        try(ResultSet rs = executeQuery(queryKeyConn, sql,
                PK.create(NAMESPACE, PEOPLE, "PK", "PK").withType(BIGINT),
                DATA.create(NAMESPACE, PEOPLE, "id", "id").withType(BIGINT),
                DATA.create(NAMESPACE, PEOPLE, "first_name", "first_name").withType(VARCHAR),
                DATA.create(NAMESPACE, PEOPLE, "last_name", "last_name").withType(VARCHAR),
                DATA.create(NAMESPACE, PEOPLE, "year_of_birth", "year_of_birth").withType(BIGINT),
                DATA.create(NAMESPACE, PEOPLE, "kids_count", "kids_count").withType(BIGINT)
        )) {
            while (rs.next()) {
                assertEquals(rs.getInt("PK"), rs.getInt("id"));
            }
        }
    }


    @ParameterizedTest(name = ARGUMENTS_PLACEHOLDER)
    @ValueSource(strings = {
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where PK=0",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where PK=1",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where PK in (1)",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where PK in (1, 2, 3, 4)",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where id=0",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where id=1",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where id in (1)",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where id in (1, 2, 3, 4)",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where 2=1",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people where 2=1+1",
            "select PK, id, first_name, last_name, year_of_birth, kids_count from people limit 0",
    })
    void metadataOneTableWithUnknownPk(String sql) throws SQLException {
        executeQuery(getTestConnection(), sql,
                PK.create(NAMESPACE, PEOPLE, "PK", "PK"),
                DATA.create(NAMESPACE, PEOPLE, "id", "id").withType(BIGINT),
                DATA.create(NAMESPACE, PEOPLE, "first_name", "first_name").withType(VARCHAR),
                DATA.create(NAMESPACE, PEOPLE, "last_name", "last_name").withType(VARCHAR),
                DATA.create(NAMESPACE, PEOPLE, "year_of_birth", "year_of_birth").withType(BIGINT),
                DATA.create(NAMESPACE, PEOPLE, "kids_count", "kids_count").withType(BIGINT)
        ).close();
    }


    @ParameterizedTest(name = ARGUMENTS_PLACEHOLDER)
    @ValueSource(strings = {
            "select * from people",
            "select * from people where PK=0",
            "select * from people where PK=1",
            "select * from people where PK in (1)",
            "select * from people where PK in (1, 2, 3, 4)",
            "select * from people where id=0",
            "select * from people where id=1",
            "select * from people where id in (1)",
            "select * from people where id in (1, 2, 3, 4)",
            "select * from people where 2=1",
            "select * from people where 2=1+1",
            "select * from people limit 0",
    })
    void metadataAllFieldsOneTableWithPk(String sql) throws SQLException {
        metadataAllFieldsOneTableWithPk(queryKeyConn, sql);
    }


    @ParameterizedTest(name = ARGUMENTS_PLACEHOLDER)
    @ValueSource(strings = {
            "query",
            "batch",
            "scan"
    })
    void metadataAllFieldsOneTableWithPkBulk(String policy) throws SQLException {
        metadataAllFieldsOneTableWithPk(
                DriverManager.getConnection(format("%s?policy.%s.sendKey=true", aerospikeTestUrl, policy)),
                "select * from people");
    }



    @ParameterizedTest(name = ARGUMENTS_PLACEHOLDER)
    @ValueSource(strings = {
            "select people.PK, instruments.PK, first_name, instruments.name as instrument from people as p join instruments as i on p.id=i.person_id",
    })
    void metadataJoinWithPk(String sql) throws SQLException {
        try(ResultSet rs = executeQuery(queryKeyConn, sql,
                PK.create(NAMESPACE, PEOPLE, "people.PK", "people.PK").withType(BIGINT),
                PK.create(NAMESPACE, INSTRUMENTS, "instruments.PK", "instruments.PK").withType(BIGINT),
                DATA.create(NAMESPACE, PEOPLE, "first_name", "first_name").withType(VARCHAR),
                DATA.create(NAMESPACE, INSTRUMENTS, "name", "instrument").withType(VARCHAR)
        )) {
            while(rs.next()) {
                assertEquals(rs.getInt(1), rs.getInt(2));
            }
        }
    }

    @Test
    void insertAndSelect() throws SQLException {
        Connection writeConn = DriverManager.getConnection(aerospikeTestUrl + "?policy.write.sendKey=true");
        assertTrue(writeConn.createStatement().execute("insert into data (PK, val) values ('hello', 'bye')"));
        queryKeyConn.createStatement().executeQuery("select * from data where PK='hello'");

        try(ResultSet rs = executeQuery(queryKeyConn, "select * from data where PK='hello'",
                PK.create(NAMESPACE, TestDataUtils.DATA, "PK", "PK").withType(VARCHAR),
                DATA.create(NAMESPACE, TestDataUtils.DATA, "val", "val").withType(VARCHAR)
        )) {
            assertTrue(rs.next());
            assertEquals("hello", rs.getString("PK"));
            assertEquals("bye", rs.getString("val"));
            assertFalse(rs.next());
        }
    }


    private void metadataAllFieldsOneTableWithPk(Connection conn, String sql) throws SQLException {
        executeQuery(conn, sql,
                PK.create(NAMESPACE, PEOPLE, "PK", "PK").withType(BIGINT),
                DATA.create(NAMESPACE, PEOPLE, "kids_count", "kids_count").withType(BIGINT),
                DATA.create(NAMESPACE, PEOPLE, "last_name", "last_name").withType(VARCHAR),
                DATA.create(NAMESPACE, PEOPLE, "id", "id").withType(BIGINT),
                DATA.create(NAMESPACE, PEOPLE, "first_name", "first_name").withType(VARCHAR),
                DATA.create(NAMESPACE, PEOPLE, "year_of_birth", "year_of_birth").withType(BIGINT)
        ).close();
    }

}