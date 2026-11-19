package com.example.api_booking.common.exception;

import com.example.api_booking.common.api.ApiResponse;
import com.example.api_booking.service.AuthService;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  private ResponseEntity<ApiResponse<Object>> error(int status, String code, String message) {
    return ResponseEntity.status(status)
        .body(new ApiResponse<>(false, message, Map.of("code", code), status));
  }

  @ExceptionHandler(BusinessException.class)
  public Object business(BusinessException e) {
    return error(e.status, e.code, e.getMessage());
  }

  @ExceptionHandler(AuthService.InvalidCredentialsException.class)
  public Object credentials() {
    return error(401, "INVALID_CREDENTIALS", "Invalid email or password");
  }

  @ExceptionHandler(AuthService.EmailAlreadyRegisteredException.class)
  public Object email() {
    return error(409, "EMAIL_ALREADY_REGISTERED", "Email đã được đăng ký");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Object validation(MethodArgumentNotValidException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(
            new ApiResponse<>(
                false,
                "Dữ liệu không hợp lệ",
                Map.of("code", "VALIDATION_ERROR", "fieldErrors", fields),
                400));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingRequestHeaderException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class
  })
  public Object invalid(Exception e) {
    return error(400, "INVALID_REQUEST", "Request không hợp lệ hoặc thiếu header bắt buộc");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public Object integrity() {
    return error(
        409, "DATA_CONFLICT", "Mã, email hoặc liên kết đã tồn tại, hoặc dữ liệu vi phạm ràng buộc");
  }

  @ExceptionHandler({
    ConcurrencyFailureException.class,
    org.springframework.orm.ObjectOptimisticLockingFailureException.class
  })
  public Object concurrency() {
    return error(409, "CONCURRENT_UPDATE", "Dữ liệu đang được thay đổi, vui lòng tải lại");
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public Object denied() {
    return error(403, "ACCESS_DENIED", "Không có quyền thực hiện");
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  public Object method() {
    return error(405, "METHOD_NOT_ALLOWED", "Method không được hỗ trợ");
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  public Object media() {
    return error(415, "UNSUPPORTED_MEDIA_TYPE", "Content-Type không được hỗ trợ");
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  public Object missing() {
    return error(404, "RESOURCE_NOT_FOUND", "Không tìm thấy API");
  }

  @ExceptionHandler(Exception.class)
  public Object unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(getClass())
        .error("Unhandled API exception type={}", e.getClass().getName());
    return error(500, "INTERNAL_ERROR", "Có lỗi hệ thống, vui lòng thử lại sau");
  }
}
