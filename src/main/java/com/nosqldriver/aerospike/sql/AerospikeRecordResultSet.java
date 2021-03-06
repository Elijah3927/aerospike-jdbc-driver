package com.nosqldriver.aerospike.sql;

import com.aerospike.client.query.KeyRecord;
import com.nosqldriver.sql.BaseSchemalessResultSet;
import com.nosqldriver.sql.DataColumn;
import com.nosqldriver.sql.TypeDiscoverer;
import com.nosqldriver.util.ValueExtractor;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nosqldriver.aerospike.sql.SpecialField.PK;
import static com.nosqldriver.aerospike.sql.SpecialField.PK_DIGEST;
import static com.nosqldriver.sql.TypeTransformer.cast;

abstract class AerospikeRecordResultSet extends BaseSchemalessResultSet<KeyRecord> {
    private final ValueExtractor valueExtractor = new ValueExtractor();

    protected AerospikeRecordResultSet(Statement statement, String schema, String table, List<DataColumn> columns, TypeDiscoverer typeDiscoverer, Collection<SpecialField> specialFields) {
        super(statement, schema, table, columns, typeDiscoverer, specialFields);
    }

    @Override
    protected abstract KeyRecord getRecord();

    @Override
    protected Object getValue(KeyRecord record, String label) {
        return valueExtractor.getValue(toMap(record), label);
    }

    @Override
    protected String getString(KeyRecord record, String label) throws SQLException {
        return getTypedValue(record, label, String.class);
    }

    @Override
    protected boolean getBoolean(KeyRecord record, String label) throws SQLException {
        return getTypedValue(record, label, boolean.class);
    }

    @Override
    protected byte getByte(KeyRecord record, String label) throws SQLException {
        return getTypedValue(record, label, byte.class);
    }

    @Override
    protected short getShort(KeyRecord record, String label) throws SQLException {
        return getTypedValue(record, label, short.class);
    }

    @Override
    protected int getInt(KeyRecord record, String label) throws SQLException {
        return getTypedValue(record, label, int.class);
    }

    @Override
    protected long getLong(KeyRecord record, String label) throws SQLException {
        return getTypedValue(record, label, long.class);
    }

    @Override
    protected float getFloat(KeyRecord record, String label) throws SQLException {
        return getTypedValue(record, label, float.class);
    }

    @Override
    protected double getDouble(KeyRecord record, String label) throws SQLException {
        return getTypedValue(record, label, double.class);
    }

    private <T> T getTypedValue(KeyRecord record, String label, Class<T> clazz) throws SQLException {
        return cast(valueExtractor.getValue(toMap(record), label), clazz);
    }

    private Map<String, Object> toMap(KeyRecord record) {
        Map<String, Object> data = record != null && record.record != null && record.record.bins != null ? record.record.bins : new HashMap<>();
        if (record != null) {
            if (specialFields.contains(PK) && record.key.userKey != null) {
                data.put("PK", record.key.userKey.getObject());
            }
            if (specialFields.contains(PK_DIGEST)) {
                data.put("PK_DIGEST", record.key.digest);
            }
        }
        return data;
    }
}
