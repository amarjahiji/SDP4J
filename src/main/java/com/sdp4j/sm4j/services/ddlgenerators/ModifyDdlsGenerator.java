package com.sdp4j.sm4j.services.ddlgenerators;

public class ModifyDdlsGenerator extends CommonDdlsGenerator {

    public String generateAlterColumnSetType(String tableName, String columnName, String type) {
        validateSqlIdentifiers("alterColumnSetType", tableName, columnName);
        return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " TYPE " + type + ";";
    }

    public String generateAlterColumnSetNotNull(String tableName, String columnName) {
        validateSqlIdentifiers("alterColumnSetNotNull", tableName, columnName);
        return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " SET NOT NULL;";
    }

    public String generateAlterColumnDropNotNull(String tableName, String columnName) {
        validateSqlIdentifiers("alterColumnDropNotNull", tableName, columnName);
        return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " DROP NOT NULL;";
    }

    public String generateAlterColumnSetDefault(String tableName, String columnName, Object defaultValue) {
        validateSqlIdentifiers("alterColumnSetDefault", tableName, columnName);
        return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " SET DEFAULT " + defaultValue + ";";
    }

    public String generateAlterColumnDropDefault(String tableName, String columnName) {
        validateSqlIdentifiers("alterColumnDropDefault", tableName, columnName);
        return "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " DROP DEFAULT;";
    }

    public String generateRenameTable(String oldTableName, String newTableName) {
        validateSqlIdentifiers("renameTable", oldTableName, newTableName);
        return "ALTER TABLE " + oldTableName + " RENAME TO " + newTableName + ";";
    }

    public String generateRenameIndex(String tableName, String oldIndexName, String newIndexName) {
        validateSqlIdentifiers("renameIndex", tableName, oldIndexName, newIndexName);
        return "ALTER INDEX " + oldIndexName + " RENAME TO " + newIndexName + ";";
    }

    public String generateRenameForeignKey(String tableName, String oldForeignKeyName, String newForeignKeyName) {
        validateSqlIdentifiers("renameForeignKey", tableName, oldForeignKeyName, newForeignKeyName);
        return "ALTER TABLE " + tableName + " RENAME CONSTRAINT " + oldForeignKeyName + " TO " + newForeignKeyName + ";";
    }

    public String generateRenameUniqueConstraint(String tableName, String oldUniqueConstraintName, String newUniqueConstraintName) {
        validateSqlIdentifiers("renameUniqueConstraint", tableName, oldUniqueConstraintName, newUniqueConstraintName);
        return "ALTER TABLE " + tableName + " RENAME CONSTRAINT " + oldUniqueConstraintName + " TO " + newUniqueConstraintName + ";";
    }

    public String generateRenameColumn(String tableName, String oldColumnName, String newColumnName) {
        validateSqlIdentifiers("renameColumn", tableName, oldColumnName, newColumnName);
        return "ALTER TABLE " + tableName + " RENAME COLUMN " + oldColumnName + " TO " + newColumnName + ";";
    }
}
