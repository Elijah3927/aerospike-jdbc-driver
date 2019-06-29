package com.nosqldriver.aerospike.sql;

import com.aerospike.client.Record;
import com.aerospike.client.query.RecordSet;

import java.sql.SQLException;


public class ResultSetOverAerospikeRecordSet extends AerospikeRecordResultSet {
    private final RecordSet rs;

    public ResultSetOverAerospikeRecordSet(String schema, String[] names, RecordSet rs) {
        super(schema, names);
        this.rs = rs;
    }


    @Override
    protected boolean moveToNext() {
        return rs.next();
    }


    @Override
    public void close() throws SQLException {
        rs.close();
        super.close();
    }


    @Override
    protected Record getRecord() {
        return rs.getRecord();
    }
}
