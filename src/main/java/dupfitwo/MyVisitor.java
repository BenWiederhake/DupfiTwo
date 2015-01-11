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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public final class MyVisitor implements FileVisitor<Path> {
    public static final long MIN_REPORT_SIZE = 2048;

    private static final int THRESHOLD_INFORM_START = 1000;

    private static final int THRESHOLD_INFORM_STEP = 10;

    private final Mmap<FileEntry> entries = new Mmap<>();

    private final long startTime = System.currentTimeMillis();

    private long smallCounter;

    private long files;

    private long nextFiles = THRESHOLD_INFORM_START;

    public MyVisitor() {
        /* Nothing to do here. */
    }

    public void brag() {
        System.out.println("Processed all " + files + " files in "
            + (System.currentTimeMillis() - startTime) + "ms.");
    }

    public Mmap<FileEntry> getEntries() {
        return entries;
    }

    public long getSmallCounter() {
        return smallCounter;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir,
    final BasicFileAttributes attrs) throws IOException {
        /* Nothing to do here. */
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file,
    final BasicFileAttributes attrs)
    throws IOException {
        // http://cr.openjdk.java.net/~alanb/7006126/webrev.00/test/
        // java/nio/file/Files/FileAttributes.java.html
        if (attrs.isRegularFile()) {
            if (!DupfiTwo.quiet) {
                if (nextFiles == ++files) {
                    System.out.println("Processed " + files + " files ...");
                    nextFiles *= THRESHOLD_INFORM_STEP;
                }
            }

            if (attrs.size() < MIN_REPORT_SIZE) {
                ++smallCounter;
            } else {
                entries.getColl().put(attrs.size(), new FileEntry(
                    (Long) Files.getAttribute(file, "unix:ino"),
                    file));
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file,
    final IOException exc) throws IOException {
        throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir,
    final IOException exc) throws IOException {
        /* Nothing to do here. */
        return FileVisitResult.CONTINUE;
    }
}
