package com.fest.visuals.api.system.configs;

import lombok.Getter;
import com.fest.visuals.api.system.files.AbstractFile;

public class StaffManager extends AbstractFile {
    @Getter private static final StaffManager instance = new StaffManager();

    @Override
    public String fileName() {
        return "staffs";
    }
}
