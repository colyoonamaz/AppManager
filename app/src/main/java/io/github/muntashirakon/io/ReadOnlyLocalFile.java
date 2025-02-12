// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.io;

import android.system.ErrnoException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ReadOnlyLocalFile extends LocalFile {
    public ReadOnlyLocalFile(@NonNull String pathname) {
        super(pathname);
    }

    private ReadOnlyLocalFile(@Nullable String parent, @NonNull String child) {
        super(parent, child);
    }

    @Override
    protected LocalFile create(String path) {
        String[] children = LocalFileOverlay.listChildren(new File(path));
        if (children != null) {
            // Emulated directory
            // Check for potential alias
            for (String child : children) {
                if (child.startsWith("/")) {
                    return super.create(child);
                }
            }
            // No alias found
            return new ReadOnlyLocalFile(path);
        }
        return super.create(path);
    }

    @NonNull
    @Override
    public LocalFile getChildFile(String name) {
        String[] children = LocalFileOverlay.listChildren(this);
        if (children != null) {
            for (String child : children) {
                if (child.equals(name)) {
                    return new ReadOnlyLocalFile(getPath(), child);
                }
            }
        }
        return super.getChildFile(name);
    }

    @SuppressWarnings("OctalInteger")
    @Override
    public int getMode() {
        try {
            return super.getMode();
        } catch (ErrnoException e) {
            // Folder + read-only
            return 0040444;
        }
    }

    @Override
    public UidGidPair getUidGid() {
        try {
            return super.getUidGid();
        } catch (ErrnoException e) {
            return new UidGidPair(0, 0);
        }
    }

    @Nullable
    @Override
    public String[] list() {
        String[] children = LocalFileOverlay.listChildren(this);
        if (children == null) {
            return super.list();
        }
        List<String> childList = new ArrayList<>(children.length);
        for (String child : children) {
            if (!child.startsWith("/")) {
                // Check if the path actually exist
                if (new File(this, child).exists()) {
                    childList.add(child);
                }
            }
        }
        return childList.toArray(new String[0]);
    }
}