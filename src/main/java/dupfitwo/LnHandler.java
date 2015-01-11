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
import java.util.Map.Entry;

public final class LnHandler implements ClashHandler {
    private static final String[] CMD_ARR = {
        "ln", "-f", null, null
    };

    private static final int DST_ARG = 2;

    private static final int MOD_ARG = 3;

    private final String[] cmdarr = Arrays.copyOf(CMD_ARR, CMD_ARR.length);

    private int lnCalls;

    public LnHandler() {
        /* Nothing to do here */
    }

    @Override
    public long handle(final long size,
    final Collection<Entry<Long, Collection<Path>>> files) {
        final ArrayList<Entry<Long, Collection<Path>>> list =
            new ArrayList<>(files);
        longestToFront(list);

        cmdarr[DST_ARG] = list.get(0).getValue().iterator().next().toString();
        try {
            for (int i = 1; i < list.size(); ++i) {
                for (final Path p : list.get(i).getValue()) {
                    link(p.toString());
                }
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return size * (files.size() - 1);
    }

    /**
     * Try to avoid calling ln too often => Swap the inode with most files
     * to front.
     */
    private void longestToFront(
    final ArrayList<Entry<Long, Collection<Path>>> list) {
        int currFiles = list.get(0).getValue().size();
        for (int i = 1; i < list.size(); ++i) {
            final int thatFiles = list.get(i).getValue().size();
            if (currFiles < thatFiles) {
                /* swap(list[0], list[i]); */
                final Entry<Long, Collection<Path>> tmp = list.get(0);
                list.set(0, list.get(i));
                list.set(i, tmp);
                currFiles = thatFiles;
            }
        }
    }

    private void link(final String mod)
    throws InterruptedException, IOException {
        cmdarr[MOD_ARG] = mod;
        ++lnCalls;
        if (!DupfiTwo.quiet) {
            System.out.println("ln -f " + cmdarr[DST_ARG] + " " + mod);
        }
        if (!DupfiTwo.dryRun) {
            final int res = Runtime.getRuntime().exec(cmdarr).waitFor();
            if (0 != res) {
                throw new RuntimeException("Couldn't create link: ln returned "
                    + res + "\ncmdarr: " + Arrays.toString(cmdarr));
            }
        }
    }

    @Override
    public void done() {
        System.out.println("Did " + lnCalls + " calls to ln");
    }
}
