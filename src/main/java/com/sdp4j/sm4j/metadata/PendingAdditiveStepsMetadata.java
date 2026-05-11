package com.sdp4j.sm4j.metadata;

import java.util.ArrayList;
import java.util.List;

public class PendingAdditiveStepsMetadata {

    private final List<MigrationStepMetadata> createTable = new ArrayList<>();
    private final List<MigrationStepMetadata> addColumn = new ArrayList<>();
    private final List<MigrationStepMetadata> addForeignKey = new ArrayList<>();
    private final List<MigrationStepMetadata> addIndex = new ArrayList<>();

    public List<MigrationStepMetadata> getCreateTable() {
        return createTable;
    }

    public List<MigrationStepMetadata> getAddColumn() {
        return addColumn;
    }

    public List<MigrationStepMetadata> getAddForeignKey() {
        return addForeignKey;
    }

    public List<MigrationStepMetadata> getAddIndex() {
        return addIndex;
    }
}
