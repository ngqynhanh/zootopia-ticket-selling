package com.zoopass.common.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Stable JSON shape for paginated lists, independent of how Spring serializes {@link Page}.
 * {@code from}/{@code to} let the UI print "Showing 21-40 of 156 results".
 *
 * @param content       items of the current page
 * @param page          zero-based page index
 * @param size          requested page size
 * @param totalElements total matching items
 * @param totalPages    total number of pages
 * @param from          1-based index of the first item on this page (0 when empty)
 * @param to            1-based index of the last item on this page (0 when empty)
 * @param <T>           item type
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long from,
        long to) {

    /** Converts a Spring Data page, mapping each entity to a DTO. */
    public static <E, T> PageResponse<T> of(Page<E> source, Function<E, T> mapper) {
        List<T> items = source.getContent().stream().map(mapper).toList();
        long from = items.isEmpty() ? 0 : source.getPageable().getOffset() + 1;
        long to = items.isEmpty() ? 0 : from + items.size() - 1;
        return new PageResponse<>(items, source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages(), from, to);
    }
}
