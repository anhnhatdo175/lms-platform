package com.example.lms.shared.pagination;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Envelope danh sách {@code {"data": [...], "meta": {page, size, totalElements, totalPages}}} (Spec 19.3).
 * Chủ sở hữu: A; bản tạm để controller của B biên dịch được, lấy bản của A khi có.
 */
public record PageResponse<T>(List<T> data, PageMeta meta) {

    public record PageMeta(int page, int size, long totalElements, int totalPages) {
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    public static <T> PageResponse<T> of(List<T> data, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(data, new PageMeta(page, size, totalElements, totalPages));
    }
}
