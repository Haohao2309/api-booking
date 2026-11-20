package com.example.api_booking.controller;

import com.example.api_booking.common.api.ApiResponse;
import com.example.api_booking.dto.RegisterRequest;
import com.example.api_booking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<com.example.api_booking.service.UserService.UserResponseDTO> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    var result =
        authService.login(
            new AuthService.LoginCommand(request.email(), request.password()),
            httpRequest,
            httpResponse);
    return ApiResponse.success("Login successful", result, 200);
  }

  @GetMapping("/me")
  public ApiResponse<com.example.api_booking.service.UserService.UserResponseDTO> currentUser(
      Authentication authentication) {
    return ApiResponse.success(
        "Authenticated user", authService.currentUser(authentication.getName()), 200);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> logout(HttpServletRequest request) {
    authService.logout(request);
    return ApiResponse.success("Logout successful", null, 200);
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<com.example.api_booking.service.UserService.UserResponseDTO> register(
      @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
    if (httpRequest.getRequestURI().startsWith("/api/v1/")
        && (request.fullName() == null
            || request.fullName().isBlank()
            || request.confirmPassword() == null
            || request.confirmPassword().isBlank()))
      throw com.example.api_booking.common.exception.BusinessException.invalid(
          "fullName và confirmPassword là bắt buộc");
    return ApiResponse.success("Account created", authService.register(request), 201);
  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
}
