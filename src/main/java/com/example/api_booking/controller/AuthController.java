package com.example.api_booking.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.example.api_booking.service.AuthService;
import com.example.api_booking.dto.RegisterRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AuthService.LoginResult> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        var result = authService.login(
            new AuthService.LoginCommand(request.email(), request.password()), httpRequest, httpResponse);
        return ApiResponse.success("Login successful", result, 200);
    }

    @GetMapping("/me")
    public ApiResponse<AuthService.LoginResult> currentUser(Authentication authentication) {
        return ApiResponse.success("Authenticated user", authService.currentUser(authentication.getName()), 200);
    }


    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<com.example.api_booking.dto.RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Account created", authService.register(request), 201);
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record ApiResponse<T>(boolean success, String message, T data, int status) {
        static <T> ApiResponse<T> success(String message, T data, int status) {
            return new ApiResponse<>(true, message, data, status);
        }
    }
}
