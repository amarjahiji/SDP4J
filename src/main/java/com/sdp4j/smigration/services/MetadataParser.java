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

    /**
     * Parse table and column metadata from a model class annotated with @Table.
     *
     * @param clazz the model class annotated with {@code @Table} used to build the metadata
     * @return a TableMetadata instance containing the table name, unique-key constraints, snake-case setting, and parsed column metadata
     */
    public TableMetadata parse(Class<?> clazz) {
        TableMetadata tableMetadata = parseTableMetadata(clazz);
        tableMetadata.setColumnMetadata(parseColumnsMetadata(clazz, tableMetadata));
        return tableMetadata;
    }

    /**
     * Builds a list of ColumnMetadata entries for every declared field of the given class.
     *
     * @param clazz the model class whose declared fields will be inspected
     * @param tableMetadata table-level metadata used to determine column naming and mapping rules
     * @return a list of ColumnMetadata objects corresponding to the class's declared fields in declaration order
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
     * Builds a ColumnMetadata instance for a single declared field of a model class.
     *
     * Populates the column name (respecting the table's snake-case setting), SQL type mapping, primary-key and not-null flags,
     * default value metadata, foreign-key metadata, and index flag based on the field's annotations and type.
     *
     * @param clazz the model class containing the field
     * @param tableMetadata table-level metadata used to determine naming conventions (e.g., snake-case)
     * @param classField the reflected field to parse into column metadata
     * @return a populated ColumnMetadata representing the given field
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
     * Populates foreign-key reference information on the provided ColumnMetadata when the given field is annotated with @ForeignKey.
     *
     * If the field has a @ForeignKey annotation, this method resolves the referenced class and determines the referenced table name
     * (using the referenced class's @Table.name() when present; otherwise computing a default name), then sets the column's
     * foreignKeyTableReference, foreignKeyReferencedColumn, and foreignKeyOnDeleteAction accordingly.
     *
     * @param classField the field to inspect for a @ForeignKey annotation
     * @param columnMetadata the ColumnMetadata to populate with foreign-key information
     * @throws RuntimeException if the referenced class specified by @ForeignKey.mapsTo() is not annotated with @Table
     */
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

    /**
     * Sets default-value metadata on the provided ColumnMetadata according to any default-value annotations present on the given field.
     *
     * For each supported annotation, this method validates that the column's SQL type matches the annotation's required SQL type and then sets the corresponding default value or flag on the ColumnMetadata.
     *
     * @param clazz the declaring class of the field (used for error messages)
     * @param classField the field to inspect for default-value annotations
     * @param columnMetadata the column metadata to populate with default values
     * @throws RuntimeException if a default-value annotation is present but the column's SQL type does not match the annotation's required SQL type
     */
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

    /**
     * Ensures the given class declares at least one field.
     *
     * @param clazz the class to validate
     * @throws RuntimeException if the class has no declared fields
     */
    private void validateClassFields(Class<?> clazz) {
        if (clazz.getDeclaredFields().length < 1) {
            throw new RuntimeException("Class: " + clazz.getSimpleName() + " has no fields");
        }
    }

    /**
     * Builds a TableMetadata instance for the provided class by reading its {@code @Table} annotation.
     *
     * @param clazz the class annotated with {@code @Table} whose table metadata will be parsed
     * @return a TableMetadata containing the resolved table name, the map of unique-key constraints, and the snake-case mapping flag
     */
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

    /**
     * Verifies the provided class is annotated with {@code @Table}.
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
     * Computes the default table name for a class.
     *
     * If {@code convertByDefaultToSnakeCasse} is {@code true}, returns the class name converted to snake_case; otherwise returns the class name in lower case.
     *
     * @param className the simple name of the class
     * @param convertByDefaultToSnakeCasse whether to convert the class name to snake_case
     * @return the table name derived from the class name
     */
    private String parseTableName(String className, boolean convertByDefaultToSnakeCasse) {
        return convertByDefaultToSnakeCasse ? CommonUtil.toSnakeCase(className) : className.toLowerCase();
    }

    /**
     * Extracts all `@UniqueKeysConstraint` annotations from the given class and returns their key arrays indexed by annotation order.
     *
     * @param clazz the class to read `@UniqueKeysConstraint` annotations from
     * @return a map where each key is the annotation index (0-based) and the value is the corresponding array of column names; column names are converted to snake_case when the class's `@Table.mapToSnakeCase()` is true
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
     * Maps a Java field type to its corresponding SQL column type string.
     *
     * @param clazz the owning class used in error messages when the field type is unsupported
     * @param fieldType the Java type of the field to map
     * @return the SQL type string for the given Java type (e.g., "VARCHAR(255)", "INT", "TIMESTAMP", "UUID")
     * @throws RuntimeException if the provided Java type is not supported for mapping
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
