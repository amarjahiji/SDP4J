package com.sdp4j.sm4j.services.ddlgenerators;

public class DropDdlsGenerator extends CommonDdlsGenerator {

    public String generateDropColumn(String tableName, String columnName) {
        validateSqlIdentifiers("dropColumn", tableName, columnName);
        return "ALTER TABLE " + tableName + " DROP COLUMN IF EXISTS " + columnName + ";";
    }

    public String generateDropIndex(String tableName, String columnName) {
        validateSqlIdentifiers("dropIndex", tableName, columnName);
        String indexName = buildIndexName(tableName, columnName);
        return "DROP INDEX IF EXISTS " + indexName + ";";
    }

    public String generateDropTable(String tableName) {
        validateSqlIdentifiers("dropTable", tableName);
        return "DROP TABLE IF EXISTS " + tableName + ";";
    }

    public String generateDropUniqueConstraint(String tableName, String... columnNames) {
        validateSqlIdentifiers("dropUniqueConstraint", tableName);
        validateSqlIdentifiers("dropUniqueConstraint", columnNames);
        String uniqueConstraintName = buildUniqueConstraintName(tableName, columnNames);
        return "ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + uniqueConstraintName + ";";
    }

    public String generateDropForeignKey(String tableName, String columnName) {
        validateSqlIdentifiers("dropForeignKey", tableName, columnName);
        String foreignKeyConstraintName = buildForeignKeyName(tableName, columnName);
        return "ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + foreignKeyConstraintName + ";";
    }
}
