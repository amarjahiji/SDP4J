package com.sdp4j.sq4j.render;

public interface SqlDialect {

    String quoteIdentifier(String identifier);

    String placeholder(int oneBasedIndex);

    String renderLimit(Integer limit, Integer offset);

    String name();
}
