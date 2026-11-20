package com.example.api_booking.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.api_booking.service.AuthService.InvalidCredentialsException;
import com.example.api_booking.service.AuthService.EmailAlreadyRegisteredException;
import com.example.api_booking.service.AuthService.InvalidResetCodeException;
import com.example.api_booking.service.AuthService.EmailNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError invalidCredentials() {
        return new ApiError(false, "Invalid email or password", null, 401);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError duplicateEmail() {
        return new ApiError(false, "Email is already registered", Map.of("code", "EMAIL_ALREADY_REGISTERED"), 409);
    }

    @ExceptionHandler(InvalidResetCodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidResetCode() {
        return new ApiError(false, "Mã xác nhận không đúng hoặc đã hết hạn", Map.of("code", "INVALID_RESET_CODE"), 400);
    }

    @ExceptionHandler(EmailNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError emailNotFound() {
        return new ApiError(false, "Email không tồn tại trong hệ thống", Map.of("code", "EMAIL_NOT_FOUND"), 404);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidInput(MethodArgumentNotValidException exception) {
        return new ApiError(false, "Invalid request data", Map.of("code", "VALIDATION_ERROR"), 400);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError unreadableBody(HttpMessageNotReadableException exception) {
        return new ApiError(false, "Invalid JSON request body", Map.of("code", "INVALID_REQUEST"), 400);
    }

    public record ApiError(boolean success, String message, Object data, int status) {}
}
