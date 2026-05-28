package com.sdp4j.sq4j.metadata;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sm4j.annotations.Table;
import com.sdp4j.sm4j.metadata.ColumnMetadata;
import com.sdp4j.sm4j.metadata.TableMetadata;
import com.sdp4j.sm4j.parsers.MetadataParser;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EntityMetadataCache {

    private final MetadataParser metadataParser = new MetadataParser();
    private final Map<Class<?>, EntityDescriptor> descriptorsByClass = new HashMap<>();

    public synchronized EntityDescriptor metadataFor(Class<?> entityClass) {
        EntityDescriptor cached = descriptorsByClass.get(entityClass);
        if (cached != null) {
            return cached;
        }
        EntityDescriptor freshlyBuilt = buildDescriptor(entityClass);
        descriptorsByClass.put(entityClass, freshlyBuilt);
        return freshlyBuilt;
    }

    private EntityDescriptor buildDescriptor(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Table.class)) {
            throw new Sdp4jValidationException(
                    "Class " + entityClass.getName() + " is not annotated with @Table");
        }
        TableMetadata tableMetadata = metadataParser.parse(entityClass);
        Map<String, Field> fieldByColumn = mapFieldsByColumnName(entityClass, tableMetadata.getColumnMetadata());
        return new EntityDescriptor(entityClass, tableMetadata, fieldByColumn);
    }

    private Map<String, Field> mapFieldsByColumnName(Class<?> entityClass, List<ColumnMetadata> columns) {
        Map<String, Field> result = new LinkedHashMap<>();
        Field[] declaredFields = entityClass.getDeclaredFields();
        int upperBound = Math.min(declaredFields.length, columns.size());
        for (int i = 0; i < upperBound; i++) {
            Field declaredField = declaredFields[i];
            declaredField.setAccessible(true);
            result.put(columns.get(i).getName(), declaredField);
        }
        return result;
    }
}
