package com.sdp4j.smigration.services;

import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.smigration.annotations.*;
import com.sdp4j.smigration.metadata.ColumnMetadata;
import com.sdp4j.smigration.metadata.TableMetadata;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class MetadataParser {

    public TableMetadata parse(Class<?> clazz) {
        TableMetadata tableMetadata = parseTableMetadata(clazz);
        tableMetadata.setColumnMetadata(parseColumnsMetadata(clazz, tableMetadata));
        return tableMetadata;
    }

    private List<ColumnMetadata> parseColumnsMetadata(Class<?> clazz, TableMetadata tableMetadata) {
        validateClassFields(clazz);
        List<ColumnMetadata> columnMetadata = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            columnMetadata.add(parseColumnMetadata(clazz, tableMetadata, field));
        }
        return columnMetadata;
    }

    private ColumnMetadata parseColumnMetadata(Class<?> clazz, TableMetadata tableMetadata, Field classField) {
        ColumnMetadata columnMetadata = new ColumnMetadata();
        columnMetadata.setName(tableMetadata.isSnakeCase() ? CommonUtil.toSnakeCase(classField.getName()) : classField.getName());
        columnMetadata.setType(mapType(clazz, classField.getType()));
        if (classField.getAnnotation(PrimaryKey.class) != null) {
            columnMetadata.setPrimaryKey(true);
            columnMetadata.setNullable(false);
        }
        if (classField.getAnnotation(NotNull.class) != null) {
            columnMetadata.setNullable(false);
        }
        setDefaultValue(clazz, classField, columnMetadata);
        setForeignKeyMetadata(classField, columnMetadata);
        if (classField.getAnnotation(Index.class) != null) {
            columnMetadata.setIndex(true);
        }
        return columnMetadata;
    }

    private void setForeignKeyMetadata(Field classField, ColumnMetadata columnMetadata) {
        if (classField.getAnnotation(ForeignKey.class) != null) {
            ForeignKey foreignKeyAnnotation = classField.getAnnotation(ForeignKey.class);
            Class<?> referenceClass = foreignKeyAnnotation.mapsTo();
            if (referenceClass.getAnnotation(Table.class) == null) {
                throw new RuntimeException("Class: " + referenceClass.getSimpleName() + " is not annotated with @Table");
            }
            Table referenceTable = referenceClass.getAnnotation(Table.class);
            String referencedTableName = CommonUtil.isValidString(referenceTable.name())
                    ? referenceTable.name()
                    : parseTableName(referenceClass.getSimpleName(), referenceTable.mapToSnakeCase());
            columnMetadata.setForeignKeyTableReference(referencedTableName);
            columnMetadata.setForeignKeyReferencedColumn(foreignKeyAnnotation.referencedColumn());
            columnMetadata.setForeignKeyOnDeleteAction(foreignKeyAnnotation.action().toSql());
        }
    }

    private void setDefaultValue(Class<?> clazz, Field classField, ColumnMetadata columnMetadata) {
        if (classField.getAnnotation(DefaultDoublePrecision.class) != null) {
            if (!columnMetadata.getType().equals("DOUBLE PRECISION")) {
                throw new RuntimeException("Field: " + classField.getName() + " in " + clazz.getSimpleName() + " cannot have annotation @DefaultDoublePrecision");
            }
            columnMetadata.setDefaultDoublePrecisionValue(classField.getAnnotation(DefaultDoublePrecision.class).value());
        }
        if (classField.getAnnotation(DefaultInt.class) != null) {
            if (!columnMetadata.getType().equals("INT")) {
                throw new RuntimeException("Field: " + classField.getName() + " in " + clazz.getSimpleName() + " cannot have annotation @DefaultInt");
            }
            columnMetadata.setDefaultIntValue(classField.getAnnotation(DefaultInt.class).value());
        }
        if (classField.getAnnotation(DefaultTrue.class) != null) {
            if (!columnMetadata.getType().equals("BOOLEAN")) {
                throw new RuntimeException("Field: " + classField.getName() + " in " + clazz.getSimpleName() + " cannot have annotation @DefaultTrue");
            }
            columnMetadata.setDefaultTrue(true);
        }
        if (classField.getAnnotation(DefaultFalse.class) != null) {
            if (!columnMetadata.getType().equals("BOOLEAN")) {
                throw new RuntimeException("Field: " + classField.getName() + " in " + clazz.getSimpleName() + " cannot have annotation @DefaultFalse");
            }
            columnMetadata.setDefaultFalse(true);
        }
        if (classField.getAnnotation(DefaultReal.class) != null) {
            if (!columnMetadata.getType().equals("REAL")) {
                throw new RuntimeException("Field: " + classField.getName() + " in " + clazz.getSimpleName() + " cannot have annotation @DefaultReal");
            }
            columnMetadata.setDefaultRealValue(classField.getAnnotation(DefaultReal.class).value());
        }
        if (classField.getAnnotation(DefaultBigInt.class) != null) {
            if (!columnMetadata.getType().equals("BIGINT")) {
                throw new RuntimeException("Field: " + classField.getName() + " in " + clazz.getSimpleName() + " cannot have annotation @DefaultBigInt");
            }
            columnMetadata.setDefaultBigIntValue(classField.getAnnotation(DefaultBigInt.class).value());
        }
        if (classField.getAnnotation(DefaultString.class) != null) {
            if (!columnMetadata.getType().equals("VARCHAR(255)")) {
                throw new RuntimeException("Field: " + classField.getName() + " in " + clazz.getSimpleName() + " cannot have annotation @DefaultString");
            }
            columnMetadata.setDefaultStringValue(classField.getAnnotation(DefaultString.class).value());
        }
    }

    private void validateClassFields(Class<?> clazz) {
        if (clazz.getDeclaredFields().length < 1) {
            throw new RuntimeException("Class: " + clazz.getSimpleName() + " has no fields");
        }
    }

    private TableMetadata parseTableMetadata(Class<?> clazz) {
        validateClass(clazz);
        Table tableAnnotation = clazz.getAnnotation(Table.class);
        boolean mapToSnakeCase = tableAnnotation.mapToSnakeCase();
        String tableName = CommonUtil.isValidString(tableAnnotation.name())
                ? tableAnnotation.name()
                : parseTableName(clazz.getSimpleName(), mapToSnakeCase);
        Map<Integer, String[]> tableUniqueKeysConstraints = parseTableUniqueKeysConstraints(clazz);
        return new TableMetadata(tableName, tableUniqueKeysConstraints, mapToSnakeCase);
    }

    private void validateClass(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Table.class)) {
            throw new RuntimeException("Class: " + clazz.getSimpleName() + " is not annotated with @Table");
        }
    }

    private String parseTableName(String className, boolean convertByDefaultToSnakeCasse) {
        return convertByDefaultToSnakeCasse ? CommonUtil.toSnakeCase(className) : className.toLowerCase();
    }

    private Map<Integer, String[]> parseTableUniqueKeysConstraints(Class<?> clazz) {
        Map<Integer, String[]> uniqueKeysConstraints = new HashMap<>();
        boolean mapToSnakeCase = clazz.getAnnotation(Table.class).mapToSnakeCase();
        UniqueKeysConstraint[] annotations = clazz.getAnnotationsByType(UniqueKeysConstraint.class);
        for (int index = 0; index < annotations.length; index++) {
            String[] keys = annotations[index].keys();
            if (mapToSnakeCase) {
                keys = Arrays.stream(keys).map(CommonUtil::toSnakeCase).toArray(String[]::new);
            }
            uniqueKeysConstraints.put(index, keys);
        }
        return uniqueKeysConstraints;
    }

    private String mapType(Class<?> clazz, Class<?> fieldType) {
        if (fieldType == String.class) return "VARCHAR(255)";
        if (fieldType == Long.class || fieldType == long.class) return "BIGINT";
        if (fieldType == Integer.class || fieldType == int.class) return "INT";
        if (fieldType == Double.class || fieldType == double.class) return "DOUBLE PRECISION";
        if (fieldType == Float.class || fieldType == float.class) return "REAL";
        if (fieldType == Boolean.class || fieldType == boolean.class) return "BOOLEAN";
        if (fieldType == LocalDate.class) return "DATE";
        if (fieldType == LocalDateTime.class) return "TIMESTAMP";
        if (fieldType == Instant.class) return "TIMESTAMP";
        if (fieldType == UUID.class) return "UUID";
        throw new RuntimeException("Field: " + fieldType.getName() + " in " + clazz.getSimpleName() + " is not a valid data type");
    }
}
