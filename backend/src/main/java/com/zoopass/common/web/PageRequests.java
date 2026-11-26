package com.zoopass.common.web;

import com.zoopass.common.exception.FieldValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a validated {@link Pageable} from query parameters.
 *
 * <p>Sort syntax: {@code sort=price:desc,name:asc} (or repeated {@code sort=} params). The direction
 * defaults to ascending. Only whitelisted fields are accepted, so clients cannot sort by internal
 * columns such as {@code passwordHash}.
 */
public final class PageRequests {

    public static final int MAX_PAGE_SIZE = 100;

    private PageRequests() {
    }

    public static Pageable of(int page, int size, List<String> sort, Set<String> allowedFields, Sort defaultSort) {
        if (page < 0) {
            throw new FieldValidationException("page", "Số trang không được âm");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new FieldValidationException("size", "Kích thước trang phải từ 1 đến " + MAX_PAGE_SIZE);
        }
        if (sort == null || sort.isEmpty()) {
            return PageRequest.of(page, size, defaultSort);
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String item : sort) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String[] parts = item.trim().split(":");
            String field = parts[0];
            if (!allowedFields.contains(field)) {
                throw new FieldValidationException("sort",
                        "Không thể sắp xếp theo '" + field + "'. Cho phép: " + String.join(", ", allowedFields));
            }
            String direction = parts.length > 1 ? parts[1].toLowerCase() : "asc";
            if (!direction.equals("asc") && !direction.equals("desc")) {
                throw new FieldValidationException("sort", "Chiều sắp xếp phải là asc hoặc desc");
            }
            orders.add(direction.equals("asc") ? Sort.Order.asc(field) : Sort.Order.desc(field));
        }
        // Always end with id so pages are stable when the sort values are equal.
        orders.add(Sort.Order.asc("id"));
        return PageRequest.of(page, size, Sort.by(orders));
    }
}
