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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

public final class DiffHandler implements ClashHandler {
    private static final String[] CMD_ARR = {
        "diff", "-q", null, null
    };

    private static final int FIRST_ARG = 2;

    private static final int SECOND_ARG = 3;

    private final ClashHandler backing;

    private final String[] cmdarr = Arrays.copyOf(CMD_ARR, CMD_ARR.length);

    private int diffCalls;

    private int falsePositives;

    public DiffHandler(final ClashHandler backing) {
        this.backing = backing;
    }

    @Override
    public long handle(final long size,
    final Collection<Entry<Long, Collection<Path>>> files) {
        try {
            long ret = 0;
            Collection<Entry<Long, Collection<Path>>> unsorted = files;
            do {
                final ArrayList<Entry<Long, Collection<Path>>> nextUnsorted =
                    new ArrayList<>();
                ret += sort(size, unsorted, nextUnsorted);
                unsorted = nextUnsorted;
            } while (unsorted.size() > 1);
            return ret;
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long sort(final long size,
    final Collection<Entry<Long, Collection<Path>>> unsorted,
    final ArrayList<Entry<Long, Collection<Path>>> nextUnsorted)
    throws InterruptedException, IOException {
        final ArrayList<Entry<Long, Collection<Path>>> clashing =
            new ArrayList<>();
        final Iterator<Entry<Long, Collection<Path>>> iter =
            unsorted.iterator();
        clashing.add(iter.next());
        cmdarr[FIRST_ARG] =
            clashing.get(0).getValue().iterator().next().toString();
        do {
            final Entry<Long, Collection<Path>> e = iter.next();
            cmdarr[SECOND_ARG] = e.getValue().iterator().next().toString();
            ++diffCalls;
            final int res = Runtime.getRuntime().exec(cmdarr).waitFor();
            switch (res) {
                case 0:
                    /* Equal */
                    clashing.add(e);
                    break;
                case 1:
                    /* Differing */
                    ++falsePositives;
                    if (!DupfiTwo.quiet) {
                        System.out.println("# " + cmdarr[FIRST_ARG]
                            + " != " + cmdarr[SECOND_ARG]);
                    }
                    nextUnsorted.add(e);
                    break;

                default:
                    throw new RuntimeException("diff didn't work on "
                        + Arrays.toString(cmdarr));
            }
        } while (iter.hasNext());
        if (clashing.size() <= 1) {
            return 0;
        }
        return backing.handle(size, clashing);
    }

    @Override
    public void done() {
        backing.done();
        System.out.println("Did " + diffCalls + " calls to 'diff'; "
            + falsePositives + " detected false positives.");
    }
}
