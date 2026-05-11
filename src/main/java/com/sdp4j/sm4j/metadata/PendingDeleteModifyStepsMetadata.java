package com.sdp4j.sm4j.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PendingDeleteModifyStepsMetadata {
    private final Map<String, String> renameTables = new HashMap<>();
    private final Map<String, Map<String, String>> renameColumns = new HashMap<>();
    private final Set<String> dropTables = new HashSet<>();
    private final Map<String, Set<String>> dropColumns = new HashMap<>();

    public Map<String, String> getRenameTables() {
        return renameTables;
    }

    public Map<String, Map<String, String>> getRenameColumns() {
        return renameColumns;
    }

    public Set<String> getDropTables() {
        return dropTables;
    }

    public Map<String, Set<String>> getDropColumns() {
        return dropColumns;
    }
}
