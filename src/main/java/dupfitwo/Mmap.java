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

import gnu.trove.TDecorators;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

public final class Mmap<T> {
    private static final int EXPECTED_KEYS = 1_000_000;

    private final TLongObjectHashMap<Collection<T>> backing;

    private final SetMultimap<Long, T> coll;

    public Mmap() {
        backing = new TLongObjectHashMap<Collection<T>>(
            EXPECTED_KEYS);
        coll = Multimaps.newSetMultimap(
            TDecorators.wrap(backing),
            new Supplier<Set<T>>() {
                public Set<T> get() {
                    return new LinkedHashSet<T>();
                }
            });
    }

    public TLongObjectHashMap<Collection<T>> getBacking() {
        return backing;
    }

    public SetMultimap<Long, T> getColl() {
        return coll;
    }
}
