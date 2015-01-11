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

import gnu.trove.procedure.TLongObjectProcedure;

import java.nio.file.Path;
import java.util.Collection;

import com.google.common.collect.SetMultimap;

public final class Summarizer implements
TLongObjectProcedure<Collection<FileEntry>> {
    private static final int B_PER_MIB = 1_048_576;

    private static final int STAGE_ONE_HASH = 1024;

    private static final int STAGE_TWO_HASH = 8 * 1024;

    private static final int STAGE_THREE_HASH = 16 * 1024;

    private final Mmap<FileEntry> entries;

    private final ClashHandler handler;

    private final Mmap<Path> clashBuf = new Mmap<>();

    private long actualSizeCounter = 0;

    public Summarizer(final Mmap<FileEntry> entries) {
        this.entries = entries;
        ClashHandler backing = new LnHandler();
        /*
         * READ FROM BOTTOM UP!
         * 
         * Files are probably equal, call diff:
         */
        backing = new DiffHandler(backing);
        /*
         * Files REALLY start the same => hash the end (esp. SQLite databases,
         * images, shared objects, objects)
         */
        backing = new HashEndHandler(backing, STAGE_THREE_HASH);
        /*
         * Files start the same => hash beyond all headers (esp. MPEG, it has a
         * 2088 byte header)
         */
        backing = new HashStartHandler(backing, STAGE_TWO_HASH);
        /*
         * Files have the same size => hash the start to find any obvious
         * differences.
         */
        backing = new HashStartHandler(backing, STAGE_ONE_HASH);
        handler = backing;
    }

    public void summarize() {
        if (!DupfiTwo.quiet) {
            System.out
                .println("Searching duplicates ...\n"
                    + "===== DO NOT RUN THE FOLLOWING COMMANDS BY HAND! =====\n"
                    + "The delayed execution means that rsnapshot might be\n"
                    + "called in the meantime.\n"
                    + "exit 127");
        }
        final long start = System.currentTimeMillis();
        entries.getBacking().forEachEntry(this);

        if (!DupfiTwo.quiet) {
            final long stop = System.currentTimeMillis();
            handler.done();
            System.out.println("Done in " + (stop - start)
                + "ms. This will free " + (actualSizeCounter / B_PER_MIB)
                + " MiB (" + actualSizeCounter + " bytes).");
        }
    }

    @Override
    public boolean execute(final long size, final Collection<FileEntry> group) {
        /* If there is only 1 file of size 'size', a clash is impossible. */
        if (group.size() > 1) {
            final SetMultimap<Long, Path> inodeFile = clashBuf.getColl();
            for (final FileEntry fe : group) {
                inodeFile.put(fe.getInode(), fe.getPath());
            }
            /*
             * If all files are actually the same inode, then no clash has to be
             * resolved (it's already handled efficiently by the filesystem).
             */
            if (clashBuf.getBacking().size() > 1) {
                actualSizeCounter +=
                    handler.handle(size, inodeFile.asMap().entrySet());
            }
            /* Prepare for re-use */
            clashBuf.getColl().clear();
        }
        return true;
    }
}
