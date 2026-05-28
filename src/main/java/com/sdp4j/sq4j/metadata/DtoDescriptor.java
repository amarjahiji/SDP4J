package com.sdp4j.sq4j.metadata;

import java.lang.reflect.Field;
import java.util.Map;

public record DtoDescriptor(Class<?> dtoClass, Map<String, Field> fieldByColumnLabel) {

    public Field fieldFor(String columnLabel) {
        return fieldByColumnLabel.get(columnLabel);
    }
}
