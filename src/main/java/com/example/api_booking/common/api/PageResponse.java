package com.example.api_booking.common.api;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, long totalItems, long totalPages) {
  public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
    return new PageResponse<>(items, page, size, total, (total + size - 1) / size);
  }
}
