package com.sdp4j.sq4j.metadata;

import com.sdp4j.core.util.CommonUtil;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class DtoMetadataCache {

    private final Map<Class<?>, DtoDescriptor> descriptorsByDtoClass = new HashMap<>();

    public synchronized DtoDescriptor descriptorFor(Class<?> dtoClass) {
        DtoDescriptor cached = descriptorsByDtoClass.get(dtoClass);
        if (cached != null) {
            return cached;
        }
        DtoDescriptor freshlyBuilt = buildDescriptor(dtoClass);
        descriptorsByDtoClass.put(dtoClass, freshlyBuilt);
        return freshlyBuilt;
    }

    private DtoDescriptor buildDescriptor(Class<?> dtoClass) {
        Map<String, Field> fieldByColumnLabel = new LinkedHashMap<>();
        for (Field declaredField : dtoClass.getDeclaredFields()) {
            declaredField.setAccessible(true);
            String columnLabel = CommonUtil.toSnakeCase(declaredField.getName());
            fieldByColumnLabel.put(columnLabel, declaredField);
        }
        return new DtoDescriptor(dtoClass, fieldByColumnLabel);
    }
}
