package com.johnymuffin.autorankup.beta;

import com.johnymuffin.jstats.core.JSONConfiguration;
import com.johnymuffin.jstats.core.simplejson.JSONArray;

import java.io.File;

public class ARConfig extends JSONConfiguration {
    public ARConfig(File file) {
        super(file);
        reload();
    }

    private void write() {
        generateConfigOption("checkFrequency", 60);
        generateConfigOption("rankups", new JSONArray());
    }

    private void reload() {
        this.write();
        this.saveFile();
    }

}
