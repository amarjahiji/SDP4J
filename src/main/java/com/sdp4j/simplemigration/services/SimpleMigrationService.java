package com.sdp4j.simplemigration.services;

import java.io.IOException;
import java.sql.SQLException;

public interface SimpleMigrationService {

    void migrateSchema() throws SQLException;

    void migrateChanges() throws SQLException, IOException;
}
