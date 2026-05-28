package com.sdp4j.sm4j.metadata;

import java.util.List;

public record MigrationMetadata(List<MigrationStepMetadata> steps) {
    public MigrationMetadata(List<MigrationStepMetadata> steps) {
        this.steps = List.copyOf(steps);
    }
}