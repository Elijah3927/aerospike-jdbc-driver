package com.nosqldriver.aerospike.sql.query;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.query.Statement;
import com.nosqldriver.aerospike.sql.ResultSetOverDistinctMap;

import java.sql.ResultSet;

public class AerospikeDistinctQuery extends AerospikeQuery<Statement, QueryPolicy> {
    private final String[] aliases;

    public AerospikeDistinctQuery(String schema, String[] names, String[] aliases, Statement statement, QueryPolicy policy) {
        super(schema, names, statement, policy);
        this.aliases = aliases;
    }

    @Override
    public ResultSet apply(IAerospikeClient client) {
        return new ResultSetOverDistinctMap(schema, names, client.queryAggregate(policy, criteria));
    }
}
