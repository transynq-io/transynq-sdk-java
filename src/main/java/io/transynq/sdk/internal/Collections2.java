package io.transynq.sdk.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Unmodifiable copies that, unlike {@code List.copyOf}, keep null elements (the API sends some). */
public final class Collections2 {
    private Collections2() {
    }

    /**
     * An unmodifiable copy, or null.
     *
     * @param list the list
     * @param <T> the element type
     * @return the copy
     */
    public static <T> List<T> immutableList(List<T> list) {
        return list == null ? null : Collections.unmodifiableList(new ArrayList<>(list));
    }

    /**
     * An unmodifiable copy, or null.
     *
     * @param map the map
     * @param <V> the value type
     * @return the copy
     */
    public static <V> Map<String, V> immutableMap(Map<String, V> map) {
        return map == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
