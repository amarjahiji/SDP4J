package com.sdp4j.core.enums;

public enum OnDelete {
    RESTRICT,
    CASCADE,
    SET_NULL {
        @Override
        public String toSql() { return "SET NULL"; }
    },
    NO_ACTION {
        @Override
        public String toSql() { return "NO ACTION"; }
    };

    public String toSql() { return name(); }
}
