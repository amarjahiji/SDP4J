package com.sdp4j.sq4j.validation;

import com.sdp4j.sq4j.metadata.EntityDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FromScope {

    private final Map<String, EntityDescriptor> descriptorsByQualifier = new LinkedHashMap<>();

    public void register(String qualifier, EntityDescriptor descriptor) {
        if (descriptorsByQualifier.containsKey(qualifier)) {
            throw new IllegalStateException(
                    "Duplicate table qualifier '" + qualifier + "' in FROM/JOIN");
        }
        descriptorsByQualifier.put(qualifier, descriptor);
    }

    public boolean hasQualifier(String qualifier) {
        return descriptorsByQualifier.containsKey(qualifier);
    }

    public EntityDescriptor descriptorFor(String qualifier) {
        return descriptorsByQualifier.get(qualifier);
    }

    public Set<String> qualifiers() {
        return Collections.unmodifiableSet(descriptorsByQualifier.keySet());
    }

    public boolean isMultiTable() {
        return descriptorsByQualifier.size() > 1;
    }

    public List<String> qualifiersWhereColumnExists(String columnName) {
        List<String> matching = new ArrayList<>();
        for (Map.Entry<String, EntityDescriptor> entry : descriptorsByQualifier.entrySet()) {
            if (entry.getValue().hasColumn(columnName)) {
                matching.add(entry.getKey());
            }
        }
        return matching;
    }
}
