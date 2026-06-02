package com.sdp4j.demo.runners;

import com.sdp4j.demo.ExampleDatabase;

/**
 * Runs every example in order against the database configured in
 * {@link ExampleDatabase}: migrate the schema first, then the SQ4J and SPS4J
 * demos. Each section is isolated so a failure in one (for example, a query
 * demo run before any rows exist) still lets the rest proceed.
 */
public class RunExamples {

    public static void main(String[] args) {
        run("Migration", () -> MigrationDemo.main(args));
        run("Insert", () -> InsertDemo.main(args));
        run("Query (SELECT)", () -> QueryDemo.main(args));
        run("Update", () -> UpdateDemo.main(args));
        run("Mapping (@Length / NUMERIC)", () -> MappingDemo.main(args));
        run("SPS4J (low-level)", () -> Sps4jDemo.main(args));
        run("Delete", () -> DeleteDemo.main(args));
    }

    private static void run(String title, ExampleSection section) {
        System.out.println("\n========== " + title + " ==========");
        try {
            section.run();
        } catch (Exception e) {
            System.out.println("[" + title + "] failed: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ExampleSection {
        void run() throws Exception;
    }
}
