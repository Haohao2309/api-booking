package com.example.api_booking.common.exception;

public class BusinessException extends RuntimeException {
  public final int status;
  public final String code;

  public BusinessException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public static BusinessException missing() {
    return new BusinessException(404, "RESOURCE_NOT_FOUND", "Không tìm thấy dữ liệu");
  }

  public static BusinessException conflict(String message) {
    return new BusinessException(409, "STATE_CONFLICT", message);
  }

  public static BusinessException invalid(String message) {
    return new BusinessException(400, "INVALID_REQUEST", message);
  }
}
