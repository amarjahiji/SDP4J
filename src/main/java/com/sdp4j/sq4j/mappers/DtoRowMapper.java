package com.sdp4j.sq4j.mappers;

import com.sdp4j.core.exception.Sq4jException;
import com.sdp4j.sq4j.metadata.DtoDescriptor;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class DtoRowMapper<T> implements RowMapper<T> {

    private final Class<T> dtoClass;
    private final DtoDescriptor descriptor;

    public DtoRowMapper(Class<T> dtoClass, DtoDescriptor descriptor) {
        this.dtoClass = dtoClass;
        this.descriptor = descriptor;
    }

    @Override
    public T map(ResultSet rs) throws SQLException {
        T instance = instantiateDto();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
            String columnLabel = metaData.getColumnLabel(columnIndex);
            Field targetField = descriptor.fieldFor(columnLabel);
            if (targetField == null) continue;
            Object value = JdbcValueReader.read(rs, columnIndex, targetField.getType());
            assignFieldValue(instance, targetField, value);
        }
        return instance;
    }

    private T instantiateDto() {
        try {
            return dtoClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new Sq4jException("Failed to instantiate " + dtoClass.getName()
                    + " — a no-arg constructor is required", e);
        }
    }

    private void assignFieldValue(T instance, Field targetField, Object value) {
        try {
            targetField.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new Sq4jException("Failed to set field '" + targetField.getName()
                    + "' on " + dtoClass.getName(), e);
        }
    }
}
