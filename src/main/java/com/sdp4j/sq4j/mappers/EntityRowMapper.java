package com.sdp4j.sq4j.mappers;

import com.sdp4j.core.exception.Sq4jException;
import com.sdp4j.sq4j.metadata.EntityDescriptor;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class EntityRowMapper<T> implements RowMapper<T> {

    private final Class<T> entityClass;
    private final EntityDescriptor descriptor;

    public EntityRowMapper(Class<T> entityClass, EntityDescriptor descriptor) {
        this.entityClass = entityClass;
        this.descriptor = descriptor;
    }

    @Override
    public T map(ResultSet rs) throws SQLException {
        T instance = instantiateEntity();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
            String columnName = metaData.getColumnLabel(columnIndex);
            Field targetField = descriptor.fieldFor(columnName);
            if (targetField == null) continue;
            Object value = JdbcValueReader.read(rs, columnIndex, targetField.getType());
            assignFieldValue(instance, targetField, value);
        }
        return instance;
    }

    private T instantiateEntity() {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new Sq4jException("Failed to instantiate " + entityClass.getName()
                    + " — a no-arg constructor is required", e);
        }
    }

    private void assignFieldValue(T instance, Field targetField, Object value) {
        try {
            targetField.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new Sq4jException("Failed to set field '" + targetField.getName()
                    + "' on " + entityClass.getName(), e);
        }
    }
}
