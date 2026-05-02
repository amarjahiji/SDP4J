package com.sdp4j.core.enums;

public enum OnDelete {
    RESTRICT,
    CASCADE,
    SET_NULL {
        /**
         * Provides the SQL fragment for the ON DELETE action represented by this constant.
         *
         * @return the SQL fragment "SET NULL"
         */
        @Override
        public String toSql() { return "SET NULL"; }
    },
    NO_ACTION {
        /**
         * Provide the SQL literal representing this delete action.
         *
         * @return the SQL literal "NO ACTION"
         */
        @Override
        public String toSql() { return "NO ACTION"; }
    };

    /**
 * Produce the SQL fragment representing this ON DELETE action.
 *
 * The default implementation returns the enum constant name; specific constants override this to return SQL literals such as "SET NULL" and "NO ACTION".
 *
 * @return the SQL fragment for this ON DELETE action (for example, "RESTRICT", "CASCADE", "SET NULL")
 */
public String toSql() { return name(); }
}
