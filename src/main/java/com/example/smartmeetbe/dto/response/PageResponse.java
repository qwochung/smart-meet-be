package com.example.smartmeetbe.dto.response;

import java.util.List;

/** Payload phân trang chung cho các danh sách. */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
