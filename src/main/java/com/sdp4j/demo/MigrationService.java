package com.sdp4j.demo;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.sm4j.metadata.MigrationStepMetadata;
import com.sdp4j.sm4j.services.Migration;
import com.sdp4j.sm4j.services.SimpleMigrationService;

import java.util.ArrayList;
import java.util.List;

public class MigrationService implements SimpleMigrationService {

    private final Migration migration;

    public MigrationService() {
        this.migration = new Sdp4jClient("", "", "", "").getMigration();
    }

    @Override
    public void migrate() {
        migration.executeMigration(dropAndOrModifyExistingSchemaElementsDdls());
    }

    @Override
    public List<MigrationStepMetadata> dropAndOrModifyExistingSchemaElementsDdls() {
        List<MigrationStepMetadata> changesToMigrate = new ArrayList<>();
        changesToMigrate.add(migration.dropTable("users"));
        changesToMigrate.add(migration.renameColumn("ss", "dd", "users"));
        changesToMigrate.add(migration.alterColumnSetDefault("ss", "sss", 2));
        changesToMigrate.add(migration.dropUniqueConstraint("users", "uq_users_email"));
        changesToMigrate.add(migration.dropForeignKey("users", "fk_users_role_id"));
        changesToMigrate.add(migration.dropColumn("users", "age"));
        changesToMigrate.add(migration.renameTable("users", "app_users"));
        changesToMigrate.add(migration.alterColumnSetDefault("ss", "sss", "varchar(255)"));
        changesToMigrate.add(migration.dropIndex("users", "idx_users_name"));
        changesToMigrate.add(migration.alterColumnSetType("users", "name", "varchar(255)"));
        changesToMigrate.add(migration.dropUniqueConstraint("users", "uq_users_name"));
        changesToMigrate.add(migration.alterColumnDropNotNull("users", "email"));
        changesToMigrate.add(migration.alterColumnDropDefault("", ""));
        changesToMigrate.add(migration.alterColumnSetNotNull("", ""));

        return changesToMigrate;
    }
}
