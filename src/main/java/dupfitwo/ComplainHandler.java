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
import java.util.Iterator;
import java.util.Map.Entry;

public final class ComplainHandler implements ClashHandler {
    private static final int THRESHOLD = 6;

    private static final int REPORT_MAX = 3;

    public ComplainHandler() {
        /* Nothing to do here */
    }

    @Override
    public long handle(final long size,
    final Collection<Entry<Long, Collection<Path>>> files) {
        if (!DupfiTwo.quiet) {
            System.out.println("Clash at size " + size
                + " with " + files.size() + " inodes.");
            if (files.size() > THRESHOLD) {
                final Iterator<Entry<Long, Collection<Path>>> iter =
                    files.iterator();
                for (int i = 0; i < REPORT_MAX && iter.hasNext(); ++i) {
                    final Entry<Long, Collection<Path>> e = iter.next();
                    final Path f = e.getValue().iterator().next();
                    System.out.println("\t" + e.getKey() + "\t" + f);
                }
            }
        }
        return size * (files.size() - 1);
    }

    @Override
    public void done() {
        /* Nothing to do here. */
    }
}
