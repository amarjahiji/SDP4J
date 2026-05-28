package com.sdp4j.sq4j.metadata;

import java.lang.reflect.Field;
import java.util.Map;

public class DtoDescriptor {

    private final Class<?> dtoClass;
    private final Map<String, Field> fieldByColumnLabel;

    public DtoDescriptor(Class<?> dtoClass, Map<String, Field> fieldByColumnLabel) {
        this.dtoClass = dtoClass;
        this.fieldByColumnLabel = fieldByColumnLabel;
    }

    public Class<?> dtoClass() {
        return dtoClass;
    }

    public Field fieldFor(String columnLabel) {
        return fieldByColumnLabel.get(columnLabel);
    }

    public Map<String, Field> fieldByColumnLabel() {
        return fieldByColumnLabel;
    }
}
