package com.fest.visuals.api.system.configs;

import lombok.Getter;
import com.fest.visuals.api.system.files.AbstractFile;

public class FriendManager extends AbstractFile {
    @Getter private static final FriendManager instance = new FriendManager();

    @Override
    public String fileName() {
        return "friends";
    }
}
