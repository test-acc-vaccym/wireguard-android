/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package com.wireguard.android.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * A keyed list where all elements are sorted by the comparator returned by {@code comparator()}
 * applied to their keys.
 */

public interface SortedKeyedList<K, E extends Keyed<? extends K>> extends KeyedList<K, E> {
    Comparator<? super K> comparator();

    K firstKey();

    Set<K> keySet();

    K lastKey();

    Collection<E> values();
}
