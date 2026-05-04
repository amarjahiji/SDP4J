package com.sdp4j.simplemigration.metadata;

import java.util.List;

public class MigrationMetadata {
    private String name;
    private List<String> scripts;

    public MigrationMetadata(String name, List<String> scripts) {
        this.name = name;
        this.scripts = scripts;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getScripts() {
        return scripts;
    }

    public void setScripts(List<String> scripts) {
        this.scripts = scripts;
    }
}
