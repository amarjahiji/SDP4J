package com.sdp4j.demo;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.sm4j.metadata.MigrationStepMetadata;
import com.sdp4j.sm4j.SM4J;

import java.util.ArrayList;
import java.util.List;

public class MigrationService {

    private final SM4J sm4j;

    public MigrationService() {
        this.sm4j = new Sdp4jClient("", "", "", "").getSm4j();
    }

    public void migrate() {
        sm4j.executeMigration(dropAndOrModifyExistingSchemaElementsDdls());
    }

    public List<MigrationStepMetadata> dropAndOrModifyExistingSchemaElementsDdls() {
        List<MigrationStepMetadata> changesToMigrate = new ArrayList<>();
        changesToMigrate.add(sm4j.dropTable("users"));
        changesToMigrate.add(sm4j.renameColumn("ss", "dd", "users"));
        changesToMigrate.add(sm4j.alterColumnSetDefault("ss", "sss", 2));
        changesToMigrate.add(sm4j.dropUniqueConstraint("users", "uq_users_email"));
        changesToMigrate.add(sm4j.dropForeignKey("users", "fk_users_role_id"));
        changesToMigrate.add(sm4j.dropColumn("users", "age"));
        changesToMigrate.add(sm4j.renameTable("users", "app_users"));
        changesToMigrate.add(sm4j.alterColumnSetDefault("ss", "sss", "varchar(255)"));
        changesToMigrate.add(sm4j.dropIndex("users", "idx_users_name"));
        changesToMigrate.add(sm4j.alterColumnSetType("users", "name", "varchar(255)"));
        changesToMigrate.add(sm4j.dropUniqueConstraint("users", "uq_users_name"));
        changesToMigrate.add(sm4j.alterColumnDropNotNull("users", "email"));
        changesToMigrate.add(sm4j.alterColumnDropDefault("", ""));
        changesToMigrate.add(sm4j.alterColumnSetNotNull("", ""));

        return changesToMigrate;
    }
}