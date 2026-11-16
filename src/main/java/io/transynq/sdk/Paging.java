package io.transynq.sdk;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Auto-paging over {@link PagedResponse} operations. */
public final class Paging {
    private Paging() {
    }

    /**
     * A lazy iterable reading page after page until an empty page or {@code page * pageSize >= total}.
     *
     * @param fetch reads one page
     * @param startPage the first page
     * @param <T> the item type
     * @return the items
     */
    public static <T> Iterable<T> iterable(IntFunction<ApiResponse<PagedResponse<T>>> fetch, int startPage) {
        return () -> new Iterator<T>() {
            private int page = Math.max(1, startPage);
            private Iterator<T> current;
            private boolean last;

            @Override
            public boolean hasNext() {
                while (current == null || !current.hasNext()) {
                    if (last) {
                        return false;
                    }
                    PagedResponse<T> data = fetch.apply(page).data();
                    if (data == null || data.items().isEmpty()) {
                        last = true;
                        return false;
                    }
                    current = data.items().iterator();
                    last = (long) data.page() * data.pageSize() >= data.total();
                    page++;
                }
                return true;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current.next();
            }
        };
    }

    /**
     * The items as a sequential stream.
     *
     * @param items an auto-paging iterable
     * @param <T> the item type
     * @return the stream
     */
    public static <T> Stream<T> stream(Iterable<T> items) {
        return StreamSupport.stream(items.spliterator(), false);
    }
}
