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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class DupfiTwo {
    public static boolean quiet = false;

    public static boolean dryRun = false;

    private static final String DRY_RUN = "--dry-run";

    private static final String QUIET = "--quiet";

    private static final String USAGE =
        "Usage: ./dupfitwo [" + DRY_RUN + "] [" + QUIET + "] BASEDIR";

    /** Not meant for instantiation. */
    private DupfiTwo() {
    }

    public static void main(final String[] args) throws IOException {
        if (0 == args.length) {
            System.err.println("Missing argument: BASEDIR");
            abort();
        }

        final String startPathName = args[args.length - 1];
        for (int i = 0; i < args.length - 1; ++i) {
            switch (args[i]) {
                case DRY_RUN:
                    if (dryRun) {
                        System.err.println("Warning: Duplicate " + DRY_RUN);
                    }
                    dryRun = true;
                    break;
                case QUIET:
                    if (quiet) {
                        System.err.println("Warning: Duplicate " + QUIET);
                    }
                    quiet = true;
                    break;

                default:
                    System.err.println("Unknown flag: " + args[i]);
                    abort();
            }
        }
        run(startPathName);
    }


    private static void abort() {
        System.err.println(USAGE);
        System.exit(1);
        throw new InternalError();
    }

    public static void run(final String startPathName) throws IOException {
        if (!quiet) {
            if (dryRun) {
                System.out.println("--dry-run is enabled.");
            }
        }
        final Path startPath = Paths.get(new File(startPathName).toURI());
        if (!startPathName.equals(startPath.toString())) {
            System.err.println("Given path: " + startPathName);
            System.err.println("To prevent mistakes, please always start"
                + " DupfiTwo with the real (canonical) path.");
            System.err.println("In this case: " + startPath);
            System.exit(2);
        }

        final MyVisitor visitor = new MyVisitor();
        Files.walkFileTree(startPath, visitor);

        if (!quiet) {
            visitor.brag();
            System.out.println(visitor.getSmallCounter()
                + " files ignored (smaller than "
                + MyVisitor.MIN_REPORT_SIZE + ")");
            System.out.println(visitor.getEntries().getColl().size()
                + " files considered.");
            System.out.println(visitor.getEntries().getBacking().size()
                + " unique sizes.");
        }

        final Summarizer s = new Summarizer(visitor.getEntries());
        s.summarize();
    }
}
