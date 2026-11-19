package com.example.api_booking.common.api;

import com.example.api_booking.common.exception.BusinessException;

public final class VersionPrecondition {
  private VersionPrecondition() {}

  public static long parse(String value) {
    if (value == null || !value.matches("\"[0-9]{1,18}\""))
      throw BusinessException.invalid("If-Match phải là ETag được trả về, ví dụ \"0\"");
    return Long.parseLong(value.substring(1, value.length() - 1));
  }
}
