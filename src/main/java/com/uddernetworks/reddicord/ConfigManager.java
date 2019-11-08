package com.uddernetworks.reddicord;

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

    public FileConfig getConfig() {
        return config;
    }
}
