package com.sdp4j.sq4j.render;

public class PostgresDialect implements SqlDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        return identifier;
    }

    @Override
    public String placeholder(int oneBasedIndex) {
        return "?";
    }

    @Override
    public String renderLimit(Integer limit, Integer offset) {
        StringBuilder sb = new StringBuilder();
        if (limit != null) {
            sb.append(" LIMIT ").append(limit);
        }
        if (offset != null) {
            sb.append(" OFFSET ").append(offset);
        }
        return sb.toString();
    }

    @Override
    public String name() {
        return "postgres";
    }
}