package com.collegelibrary.dto;

import org.springframework.data.domain.Page;

/** Standard page wrapper so the frontend always gets the same shape. */
public record PageResponse<T>(
        java.util.List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages()
        );
    }
}
