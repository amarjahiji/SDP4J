package com.sdp4j.sm4j.metadata;

import java.util.List;

public final class MigrationMetadata {
    private final List<MigrationStepMetadata> steps;

    public MigrationMetadata(List<MigrationStepMetadata> steps) {
        this.steps = List.copyOf(steps);
    }

    public List<MigrationStepMetadata> getSteps() {
        return steps;
    }
}