package com.sdp4j.sm4j.services;

import com.sdp4j.sm4j.metadata.MigrationStepMetadata;

import java.util.List;

public interface SimpleMigrationService {

    void migrate();

    List<MigrationStepMetadata> dropAndOrModifyExistingSchemaElementsDdls();
}
