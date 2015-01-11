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
import java.util.Collection;
import java.util.Map.Entry;

public interface ClashHandler {
    /**
     * Handle the given clash appropriately.
     * 
     * @param size The size of each file (all sizes are equal)
     * @param files A mapping inode->{file} of all involved files. Note that for
     *            backups by rsnapshot, the ratio can easily be 1:100 (1 inode,
     *            100 files).
     * @return How much space could be saved, in bytes.
     * @throws RuntimeException If any IOException gets thrown.
     */
    long handle(final long size,
    final Collection<Entry<Long, Collection<Path>>> files);

    void done();
}
