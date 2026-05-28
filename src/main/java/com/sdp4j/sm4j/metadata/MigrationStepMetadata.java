package com.sdp4j.sm4j.metadata;

import java.util.List;

public record MigrationStepMetadata(String verb, List<String> args, String ddl) {
    public MigrationStepMetadata(String verb, List<String> args, String ddl) {
        this.verb = verb;
        this.args = List.copyOf(args);
        this.ddl = ddl;
    }

    public MigrationStepMetadata(String verb, String ddl, String... args) {
        this(verb, List.of(args), ddl);
    }

    public String getAction() {
        return args.isEmpty() ? verb + "()" : verb + "(" + String.join(", ", args) + ")";
    }
}
