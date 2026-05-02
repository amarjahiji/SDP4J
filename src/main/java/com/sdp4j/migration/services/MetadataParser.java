package com.sdp4j.migration.services;

import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.migration.annotations.*;
import com.sdp4j.migration.metadata.ColumnMetadata;
import com.sdp4j.migration.metadata.TableMetadata;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class MetadataParser {

    /**
     * Builds TableMetadata for the given model class by parsing its table and column annotations.
     *
     * @param clazz the model class annotated with {@code @Table}
     * @return a fully populated {@link TableMetadata} including parsed column metadata
     */
    public TableMetadata parse(Class<?> clazz) {
        TableMetadata tableMetadata = parseTableMetadata(clazz);
        tableMetadata.setColumnMetadata(parseColumnsMetadata(clazz, tableMetadata));
        return tableMetadata;
    }

    /**
     * Builds column metadata for every declared field of the provided model class.
     *
     * @param clazz the model class whose declared fields will be parsed into columns
     * @param tableMetadata table-level settings used when constructing each ColumnMetadata
     * @return a list of ColumnMetadata objects, one for each declared field in {@code clazz}
     */
    private List<ColumnMetadata> parseColumnsMetadata(Class<?> clazz, TableMetadata tableMetadata) {
        validateClassFields(clazz);
        List<ColumnMetadata> columnMetadata = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            columnMetadata.add(parseColumnMetadata(clazz, tableMetadata, field));
        }
        return columnMetadata;
    }

    /**
     * Builds a ColumnMetadata instance for the specified field using the provided table context.
     *
     * The resulting metadata includes the column name (converted to snake_case when the table requests it),
     * the mapped SQL type, nullability and primary-key status (from `@PrimaryKey` / `@NotNull`), any supported
     * default value annotations, foreign-key information (from `@ForeignKey`), and index presence (from `@Index`).
     *
     * @param clazz the class that declares the field; used for annotation-based validations and type mapping context
     * @param tableMetadata table-level metadata that determines naming conventions (e.g., snake_case)
     * @param classField the field to parse into column metadata
     * @return a populated ColumnMetadata for the given field
     */
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

    /**
     * Populates the columnMetadata's foreign key reference and on-delete action when the classField is annotated with {@code @ForeignKey}.
     *
     * @param classField      the reflected field that may declare a foreign key mapping
     * @param columnMetadata  the column metadata object to update with foreign key information
     * @throws RuntimeException if the referenced class specified in {@code @ForeignKey.mapsTo()} is not annotated with {@code @Table}
     */
    private void setForeignKeyMetadata(Field classField, ColumnMetadata columnMetadata) {
        if (classField.getAnnotation(ForeignKey.class) != null) {
            ForeignKey foreignKeyAnnotation = classField.getAnnotation(ForeignKey.class);
            Class<?> referenceClass = foreignKeyAnnotation.mapsTo();
            if (referenceClass.getAnnotation(Table.class) == null) {
                throw new RuntimeException("Class: " + referenceClass.getSimpleName() + " is not annotated with @Table");
            }
            columnMetadata.setForeignKeyTableReference(parseTableName(referenceClass.getSimpleName(), referenceClass.getAnnotation(Table.class).mapToSnakeCase()));
            columnMetadata.setForeignKeyOnDeleteAction(foreignKeyAnnotation.action().toSql());
        }
    }

    /**
     * Applies any supported `@Default*` annotations present on the given field to the provided
     * ColumnMetadata and validates that each default is compatible with the column's mapped SQL type.
     *
     * For each recognized default annotation, the corresponding default value on `columnMetadata`
     * is set when the annotation is present; if the column's SQL type does not match the annotation's
     * expected SQL type, a RuntimeException is thrown.
     *
     * @param clazz the containing class of the field (used for error messages)
     * @param classField the field to inspect for default-value annotations
     * @param columnMetadata the column metadata to populate with default values
     * @throws RuntimeException if a default annotation is used while the column's mapped SQL type
     *                          does not match the annotation's required SQL type
     */
    private void setDefaultValue(Class<?> clazz, Field classField, ColumnMetadata columnMetadata) {
        if (classField.getAnnotation(DefaultDouble.class) != null) {
            if (!columnMetadata.getType().equals("DOUBLE PRECISION")) {
                throw new RuntimeException("Field: " + classField.getName() + " in " + clazz.getSimpleName() + " cannot have annotation @DefaultDouble");
            }
            columnMetadata.setDefaultDoubleValue(classField.getAnnotation(DefaultDouble.class).value());
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
        if (classField.getAnnotation(DefaultFloat.class) != null) {
            if (!columnMetadata.getType().equals("REAL")) {
                throw new RuntimeException("Field: " + classField.getName() + " in " + clazz.getSimpleName() + " cannot have annotation @DefaultFloat");
            }
            columnMetadata.setDefaultFloatValue(classField.getAnnotation(DefaultFloat.class).value());
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

    /**
     * Ensures the provided class declares at least one field.
     *
     * @param clazz the class to validate
     * @throws RuntimeException if the class declares no fields
     */
    private void validateClassFields(Class<?> clazz) {
        if (clazz.getDeclaredFields().length < 1) {
            throw new RuntimeException("Class: " + clazz.getSimpleName() + " has no fields");
        }
    }

    /**
     * Builds TableMetadata for the given annotated class.
     *
     * Parses the class's @Table configuration to determine the table name, whether
     * names should be converted to snake_case, and any unique key constraints.
     *
     * @param clazz the class annotated with @Table to parse
     * @return a TableMetadata containing the table name, unique key constraints, and snake-case mapping
     */
    private TableMetadata parseTableMetadata(Class<?> clazz) {
        validateClass(clazz);
        boolean mapToSnakeCase = clazz.getAnnotation(Table.class).mapToSnakeCase();
        String tableName = parseTableName(clazz.getSimpleName(), mapToSnakeCase);
        Map<Integer, String[]> tableUniqueKeysConstraints = parseTableUniqueKeysConstraints(clazz);
        return new TableMetadata(tableName, tableUniqueKeysConstraints, mapToSnakeCase);
    }

    /**
     * Validates that the provided class is annotated with {@code @Table}.
     *
     * @param clazz the class to validate
     * @throws RuntimeException if {@code clazz} is not annotated with {@code @Table}
     */
    private void validateClass(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Table.class)) {
            throw new RuntimeException("Class: " + clazz.getSimpleName() + " is not annotated with @Table");
        }
    }

    /**
     * Compute the table name for a class, optionally converting it to snake_case.
     *
     * @param className the simple name of the class to derive the table name from
     * @param convertByDefaultToSnakeCasse if true, convert the className to snake_case; otherwise use the className in lower case
     * @return the resulting table name (snake_case when requested, otherwise lowercased)
     */
    private String parseTableName(String className, boolean convertByDefaultToSnakeCasse) {
        return convertByDefaultToSnakeCasse ? CommonUtil.toSnakeCase(className) : className.toLowerCase();
    }

    /**
     * Extracts `@UniqueKeysConstraint` values from the given class and returns each constraint's keys,
     * converting keys to snake_case when the class's `@Table.mapToSnakeCase()` is true.
     *
     * @param clazz the class to inspect for `@UniqueKeysConstraint` annotations
     * @return a map from the constraint index (0-based) to the array of column names for that constraint
     */
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

    /**
     * Map a Java field type to the corresponding SQL type name.
     *
     * Supported mappings include common scalar and temporal types (e.g. `String` → `VARCHAR(255)`, `int`/`Integer` → `INT`, `long`/`Long` → `BIGINT`, `double`/`Double` → `DOUBLE PRECISION`, `float`/`Float` → `REAL`, `boolean`/`Boolean` → `BOOLEAN`, `LocalDate` → `DATE`, `LocalDateTime`/`Instant` → `TIMESTAMP`, `UUID` → `UUID`).
     *
     * @param clazz the declaring class of the field; used to provide context in error messages
     * @param fieldType the Java type of the field to map
     * @return the SQL type name corresponding to the provided Java field type
     * @throws RuntimeException if the provided fieldType is not a supported/mappable Java type
     */
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
