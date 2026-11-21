package com.example.api_booking.controller;

import com.example.api_booking.common.api.ApiResponse;
import com.example.api_booking.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
  private final UserService users;

  public UserController(UserService users) {
    this.users = users;
  }

  @GetMapping
  public Object me() {
    return ApiResponse.ok(users.dto(users.current()));
  }

  @PatchMapping
  public Object updateProfile(
      @Valid @RequestBody com.example.api_booking.dto.ProfileUpdateRequest request) {
    return ApiResponse.ok(users.updateProfile(request));
  }

  @PutMapping("/password")
  public Object changePassword(@Valid @RequestBody ChangePasswordRequest r) {
    users.changePassword(r.oldPassword(), r.newPassword(), r.confirmNewPassword());
    return ApiResponse.ok(null);
  }

  public record ChangePasswordRequest(
      @NotBlank @Size(max = 72) String oldPassword,
      @NotBlank @Size(min = 8, max = 72) String newPassword,
      @NotBlank @Size(max = 72) String confirmNewPassword) {}
}
