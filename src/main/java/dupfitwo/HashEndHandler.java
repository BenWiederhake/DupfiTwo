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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map.Entry;

import com.google.common.collect.HashMultimap;

// FIXME: Remove code duplication with HashStartHandler.
public final class HashEndHandler implements ClashHandler {
    private final ClashHandler backing;

    private final MessageDigest md;

    private final ByteBuffer buffer;

    private long timeSpentHashing;

    private int hashes;

    public HashEndHandler(final ClashHandler backing, final int bufSize) {
        this.backing = backing;
        buffer = ByteBuffer.allocateDirect(bufSize);

        try {
            /* Use a FAST hash. TODO: Faster than MD5? */
            md = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(
                "Specification says yes, call said no?!", e);
        }
    }

    @Override
    public long handle(final long size,
    final Collection<Entry<Long, Collection<Path>>> files) {
        final HashMultimap<ByteBuffer, Entry<Long, Collection<Path>>> table =
            HashMultimap.create();

        try {
            for (final Entry<Long, Collection<Path>> physicalFile : files) {
                /*
                 * Happens only about 50K times in total. Is that Iterator
                 * object bad? I don't think so.
                 */
                final Path logicalFile =
                    physicalFile.getValue().iterator().next();
                timeSpentHashing -= System.nanoTime();
                final ByteBuffer hash = doHash(logicalFile);
                ++hashes;
                timeSpentHashing += System.nanoTime();
                table.put(hash, physicalFile);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        long ret = 0;
        for (final Entry<ByteBuffer,
                         Collection<Entry<Long, Collection<Path>>>> e : table
            .asMap()
            .entrySet()) {
            if (e.getValue().size() > 1) {
                ret += backing.handle(size, e.getValue());
            }
        }
        return ret;
    }

    private ByteBuffer doHash(final Path path)
    throws IOException {
        try (
        final RandomAccessFile file = new RandomAccessFile(path.toFile(), "r");
        final FileChannel inChannel = file.getChannel();) {
            inChannel.position(Math.max(0, inChannel.size() - buffer.capacity()));

            int bytesRead;
            do {
                bytesRead = inChannel.read(buffer);
            } while (bytesRead >= 0 && buffer.hasRemaining());
            buffer.flip();
            md.update(buffer);
            buffer.clear();

            inChannel.close();
            file.close();

            return ByteBuffer.wrap(md.digest());
        }
    }

    @Override
    public void done() {
        backing.done();
        System.out.println("Spent " + (timeSpentHashing / 1_000_000)
            + "ms hashing " + hashes + " buffers á "
            + buffer.capacity() + " bytes.");
    }
}
