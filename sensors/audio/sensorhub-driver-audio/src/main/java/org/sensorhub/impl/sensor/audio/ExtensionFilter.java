package org.sensorhub.impl.sensor.audio;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFilter implements FilenameFilter {
    private String[] exts;

    public ExtensionFilter(String [] exts) {
        this.exts = exts;
    }

    @Override
    public boolean accept(File dir, String name) {
        for (String ext : exts) {
             if (name.endsWith(ext)) {
                 return true;
             }
        }

        return false;
    }
}
