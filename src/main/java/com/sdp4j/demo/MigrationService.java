package com.sdp4j.demo;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.simplemigration.metadata.MigrationMetadata;
import com.sdp4j.simplemigration.services.Migration;
import com.sdp4j.simplemigration.services.SimpleMigrationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MigrationService implements SimpleMigrationService {

    private final Migration migration = new Sdp4jClient("", "root", "root", "").getMigration();

    @Override
    public void migrateSchema() throws SQLException {
        migration.migrateSchema();
    }

    @Override
    public void migrateChanges() throws SQLException, IOException {
        List<MigrationMetadata> changes = new ArrayList<>();
        changes.add(migration.renameColumn("username", "user_name", "users"));
        changes.add(migration.dropColumn("age", "users"));
        changes.add(migration.dropIndex("email", "users"));
        changes.add(migration.dropForeignKey("orders", "users"));
        changes.add(migration.dropUniqueConstraint("email", "users"));
        migration.migrateChanges(changes);
    }
}
