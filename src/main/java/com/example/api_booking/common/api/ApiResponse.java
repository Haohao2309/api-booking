package com.example.api_booking.common.api;

public record ApiResponse<T>(boolean success, String message, T data, int status) {
  public static <T> ApiResponse<T> success(String message, T data, int status) {
    return new ApiResponse<>(true, message, data, status);
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "Thành công", data, 200);
  }

  public static <T> ApiResponse<T> created(T data) {
    return new ApiResponse<>(true, "Tạo thành công", data, 201);
  }
}
