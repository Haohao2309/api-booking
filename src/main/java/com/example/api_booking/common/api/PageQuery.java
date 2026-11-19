package com.example.api_booking.common.api;

import com.example.api_booking.common.exception.BusinessException;

public record PageQuery(int page, int size, String order) {
  public static PageQuery of(int page, int size, String sort, String... allowed) {
    if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
      throw BusinessException.invalid("Phân trang không hợp lệ");
    String[] parts = sort.split(",", -1);
    if (parts.length != 2
        || !java.util.List.of(allowed).contains(parts[0])
        || !java.util.List.of("asc", "desc").contains(parts[1]))
      throw BusinessException.invalid("Sort không hợp lệ");
    return new PageQuery(
        page, size, parts[0] + " " + parts[1] + (parts[0].equals("id") ? "" : ", id asc"));
  }

  public int offset() {
    return page * size;
  }
}
