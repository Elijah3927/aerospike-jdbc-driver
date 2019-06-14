package com.nosqldriver.aerospike.sql;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Log;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.IndexType;
import com.nosqldriver.Person;
import com.nosqldriver.VisibleForPackage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;

import static com.aerospike.client.Log.setCallback;
import static com.aerospike.client.Log.setLevel;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This class contains various utilities needed for easier manipulations with data in DB.
 */
@VisibleForPackage
class TestDataUtils {
    @VisibleForPackage static final String NAMESPACE = "test";
    @VisibleForPackage static final String PEOPLE = "people";
    @VisibleForPackage static final String INSTRUMENTS = "instruments";
    @VisibleForPackage static final String GUITARS = "guitars";
    @VisibleForPackage static final String KEYBOARDS = "keyboards";
    @VisibleForPackage static final String SUBJECT_SELECTION = "subject_selection";
    @VisibleForPackage static final String SELECT_ALL = "select * from people";

    static {
        setCallback((level, message) -> System.out.println(message));
        setLevel(Log.Level.DEBUG);
    }
    @VisibleForPackage static final AerospikeClient client = new AerospikeClient("localhost", 3000);
    @VisibleForPackage static Connection conn;

    static {
        try {
            conn = DriverManager.getConnection("jdbc:aerospike:localhost/test");
            assertNotNull(conn);
        } catch (SQLException e) {
            throw new  IllegalStateException("Cannot create connection to DB", e);
        }
    }

    @VisibleForPackage static final Person[] beatles = new Person[] {
            new Person(1, "John", "Lennon", 1940, 2),
            new Person(2, "Paul", "McCartney", 1942, 5),
            new Person(3, "George", "Harrison", 1943, 1),
            new Person(4, "Ringo", "Starr", 1940, 3),
    };


    @VisibleForPackage static Function<String, Integer> executeUpdate = new Function<String, Integer>() {
        @Override
        public Integer apply(String sql) {
            try {
                return conn.createStatement().executeUpdate(sql);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    @VisibleForPackage static Function<String, Boolean> execute = new Function<String, Boolean>() {
        @Override
        public Boolean apply(String sql) {
            try {
                return conn.createStatement().execute(sql);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    @VisibleForPackage static Function<String, ResultSet> executeQuery = new Function<String, ResultSet>() {
        @Override
        public ResultSet apply(String sql) {
            try {
                return conn.createStatement().executeQuery(sql);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    @VisibleForPackage static Function<String, ResultSet> executeQueryPreparedStatement = new Function<String, ResultSet>() {
        @Override
        public ResultSet apply(String sql) {
            try {
                return conn.prepareStatement(sql).executeQuery();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    };


    @VisibleForPackage static boolean resultSetNext(ResultSet rs) {
        try {
            return rs.next();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @VisibleForPackage static Collection<String> retrieveColumn(String sql, String column) throws SQLException {
        ResultSet rs = conn.createStatement().executeQuery(sql);
        Collection<String> data = new HashSet<>();
        while (rs.next()) {
            data.add(rs.getString(column));
        }
        return data;
    }


    @VisibleForPackage static void writeBeatles() {
        WritePolicy writePolicy = new WritePolicy();
        write(PEOPLE, writePolicy, 1, person(1, "John", "Lennon", 1940, 2));
        write(PEOPLE, writePolicy, 2, person(2, "Paul", "McCartney", 1942, 5));
        write(PEOPLE, writePolicy, 3, person(3, "George", "Harrison", 1943, 1));
        write(PEOPLE, writePolicy, 4, person(4, "Ringo", "Starr", 1940, 3));
    }

    //Juliawrin Lennon 1963
    //Sean Lennon 1975
    // Heather McCartney, 1962
    // Mary McCartney, 1969
    // Stella McCartney, 1971
    // James McCartney, 1977
    // Dhani Harrison, 1978
    // Zak Starkey, 1965

    @VisibleForPackage static void writeSubjectSelection() {
        //reference: https://stackoverflow.com/questions/2421388/using-group-by-on-multiple-columns
        WritePolicy writePolicy = new WritePolicy();
        int id = 1;
        write(SUBJECT_SELECTION, writePolicy, id++, subjectSelection("ITB001", 1, "John"));
        write(SUBJECT_SELECTION, writePolicy, id++, subjectSelection("ITB001", 1, "Bob"));
        write(SUBJECT_SELECTION, writePolicy, id++, subjectSelection("ITB001", 1, "Mickey"));
        write(SUBJECT_SELECTION, writePolicy, id++, subjectSelection("ITB001", 2, "Jenny"));
        write(SUBJECT_SELECTION, writePolicy, id++, subjectSelection("ITB001", 2, "James"));
        write(SUBJECT_SELECTION, writePolicy, id++, subjectSelection("MKB114", 1, "John"));
        write(SUBJECT_SELECTION, writePolicy, id, subjectSelection("MKB114", 1, "Erica"));
    }


    @VisibleForPackage static void writeMainPersonalInstruments() {
        WritePolicy writePolicy = new WritePolicy();
        write(INSTRUMENTS, writePolicy, 1, personalInstrument(1, 1, "guitar"));
        write(INSTRUMENTS, writePolicy, 2, personalInstrument(2, 2, "bass guitar"));
        write(INSTRUMENTS, writePolicy, 3, personalInstrument(3, 3, "guitar"));
        write(INSTRUMENTS, writePolicy, 4, personalInstrument(4, 4, "drums"));
    }

    @VisibleForPackage static void writeAllPersonalInstruments() {
        WritePolicy writePolicy = new WritePolicy();
        // John Lennon
        write(INSTRUMENTS, writePolicy, 1, personalInstrument(1, 1, "vocals"));
        write(INSTRUMENTS, writePolicy, 2, personalInstrument(2, 1, "guitar"));
        write(INSTRUMENTS, writePolicy, 3, personalInstrument(3, 1, "keyboards"));
        write(INSTRUMENTS, writePolicy, 4, personalInstrument(4, 1, "harmonica"));

        // Paul McCartney
        write(INSTRUMENTS, writePolicy, 5, personalInstrument(5, 2, "vocals"));
        write(INSTRUMENTS, writePolicy, 6, personalInstrument(6, 2, "bass guitar"));
        write(INSTRUMENTS, writePolicy, 7, personalInstrument(7, 2, "guitar"));
        write(INSTRUMENTS, writePolicy, 8, personalInstrument(8, 2, "keyboards"));

        // George Harrison
        write(INSTRUMENTS, writePolicy, 9, personalInstrument(9, 3, "vocals"));
        write(INSTRUMENTS, writePolicy, 10, personalInstrument(10, 3, "guitar"));
        write(INSTRUMENTS, writePolicy, 11, personalInstrument(11, 3, "sitar"));

        // Ringo Starr
        write(INSTRUMENTS, writePolicy, 12, personalInstrument(12, 4, "drums"));
        write(INSTRUMENTS, writePolicy, 13, personalInstrument(13, 4, "vocals"));
    }

    @VisibleForPackage static void writeGuitars() {
        WritePolicy writePolicy = new WritePolicy();
        write(GUITARS, writePolicy, 2, personalInstrument(2, 1, "guitar")); // John Lennon
        write(GUITARS, writePolicy, 7, personalInstrument(7, 2, "guitar")); // Paul McCartney
        write(GUITARS, writePolicy, 10, personalInstrument(10, 3, "guitar")); // George Harrison
    }


    @VisibleForPackage static void writeKeyboards() {
        WritePolicy writePolicy = new WritePolicy();
        write(KEYBOARDS, writePolicy, 3, personalInstrument(3, 1, "keyboards")); // John Lennon
        write(KEYBOARDS, writePolicy, 8, personalInstrument(8, 2, "keyboards")); // Paul McCartney
    }

    private static void write(WritePolicy writePolicy, Key key, Bin... bins) {
        client.put(writePolicy, key, bins);
    }

    private static void write(String table, WritePolicy writePolicy, int id, Bin ... bins) {
        write(writePolicy, new Key(NAMESPACE, table, id), bins);
    }

    private static Bin[] person(int id, String firstName, String lastName, int yearOfBirth, int kidsCount) {
        return new Bin[] {new Bin("id", id), new Bin("first_name", firstName), new Bin("last_name", lastName), new Bin("year_of_birth", yearOfBirth), new Bin("kids_count", kidsCount)};
    }

    private static Bin[] subjectSelection(String subject, int semester, String attendee) {
        return new Bin[] {new Bin("subject", subject), new Bin("semester", semester), new Bin("attendee", attendee)};
    }

    private static Bin[] personalInstrument(int id, int personId, String name) {
        return new Bin[] {new Bin("id", id), new Bin("person_id", personId), new Bin("name", name)};
    }

    @VisibleForPackage static void deleteAllRecords(String namespace, String table) {
        client.scanAll(new ScanPolicy(), namespace, table, (key, record) -> client.delete(new WritePolicy(), key));
    }

    @VisibleForPackage static void dropIndexSafely(String fieldName) {
        try {
            dropIndex(fieldName);
        } catch (AerospikeException e) {
            if (e.getResultCode() != 201) {
                throw e;
            }
        }
    }

    @VisibleForPackage static void createIndex(String fieldName, IndexType indexType) {
        client.createIndex(null, NAMESPACE, PEOPLE, getIndexName(fieldName), fieldName, indexType).waitTillComplete();
    }

    private static void dropIndex(String fieldName) {
        client.dropIndex(null, NAMESPACE, PEOPLE, getIndexName(fieldName)).waitTillComplete();
    }

    private static String getIndexName(String fieldName) {
        return format("%s_%s_INDEX", PEOPLE, fieldName.toUpperCase());
    }


    @VisibleForPackage
    static ResultSet executeQuery(String sql, String expectedSchema, Object ... expectedMetadataFields) throws SQLException {
        ResultSet rs = conn.createStatement().executeQuery(sql);

        if (expectedMetadataFields.length > 0) {
            validate(rs.getMetaData(), expectedSchema, expectedMetadataFields);
        }

        return rs;
    }

    /**
     * Validates metadata against expected values
     * @param md
     * @param expectedMetadataFields expected fields: name,label,type
     * @return the metadata sent as an argument
     * @throws SQLException
     */
    @VisibleForPackage
    static ResultSetMetaData validate(ResultSetMetaData md, String expectedSchema, Object ... expectedMetadataFields) throws SQLException {
        assertNotNull(md);

        int n = expectedMetadataFields.length / 3;
        assertEquals(n, md.getColumnCount());
        for (int i = 1, j = 0; i <= n; i++, j+=3) {
            assertEquals(expectedSchema, md.getSchemaName(i));
            String name = (String)expectedMetadataFields[j];
            String label = (String)expectedMetadataFields[j + 1];
            Integer type = (Integer)expectedMetadataFields[j + 2];
            if (name != null) {
                assertEquals(name, md.getColumnName(i));
            }
            if (label != null) {
                assertEquals(label, md.getColumnLabel(i));
            }
            if (type != null) {
                assertEquals(type.intValue(), md.getColumnType(i));
            }
        }
        return md;
    }
}
