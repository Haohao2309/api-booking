package com.example.api_booking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;

public record HotelRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 255) String address,
    @NotBlank @Email @Size(max = 150) String contactEmail,
    @NotBlank @Pattern(regexp = "\\+?[0-9]{8,15}") String phone,
    @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
    @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
    @NotBlank @Size(max = 50) String timezone,
    @NotNull LocalTime checkInTime,
    @NotNull LocalTime checkOutTime,
    @NotNull @Pattern(regexp = "FULL_REFUND_24H_V1") String cancellationPolicy) {}
