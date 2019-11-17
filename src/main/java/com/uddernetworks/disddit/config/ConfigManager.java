package com.uddernetworks.disddit.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;

public class ConfigManager {

    private String fileName;
    private FileConfig config;

    public ConfigManager(String fileName) {
        this.fileName = fileName;
    }

    public void init() {
        config = CommentedFileConfig.builder(this.fileName).autosave().build();
        config.load();
    }

    public <T> T get(Config path) {
        return config.get(path.getPath());
    }

    public <T> T get(Config path, T def) {
        return config.getOrElse(path.getPath(), def);
    }

    public FileConfig getConfig() {
        return config;
    }
}
