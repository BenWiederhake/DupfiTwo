/*
 * DupfiTwo: Collapses duplicates in rsnapshot-backups by using hardlinks.
 * Copyright (C) 2015 Ben Wiederhake
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program (see LICENSE). If not, see <http://www.gnu.org/licenses/>.
 */
package dupfitwo;

import java.nio.file.Path;

public final class FileEntry {
    private final long inode;

    /* TODO: Store char[] instead of Path? */
    private final Path path;

    public FileEntry(final long inode, final Path path) {
        this.inode = inode;
        this.path = path;
    }

    public long getInode() {
        return inode;
    }

    public Path getPath() {
        return path;
    }
}
