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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map.Entry;

public final class WriteOutHandler implements ClashHandler {
    private static final String NAME = "matches.txt";

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private final OutputStream out;

    public WriteOutHandler() {
        if (!DupfiTwo.quiet) {
            System.out.println("Writing matches to " + NAME);
        }
        try {
            out = new BufferedOutputStream(
                new FileOutputStream(new File(NAME)));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long handle(final long size,
    final Collection<Entry<Long, Collection<Path>>> files) {
        try {
            for (final Entry<Long, Collection<Path>> e : files) {
                for (final Path p : e.getValue()) {
                    /* TODO: Buffer byte[] instances */
                    out.write(p.toString().getBytes(DEFAULT_CHARSET));
                    out.write('\n');
                }
                out.write('\n');
            }
            out.write('\n');
            return size * (files.size() - 1);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void done() {
        /* Nothing to do here. */
    }
}
