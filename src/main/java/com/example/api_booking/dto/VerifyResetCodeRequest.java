package com.example.api_booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyResetCodeRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Pattern(regexp = "\\d{6}", message = "Mã xác nhận gồm 6 chữ số") String code) {}
