package com.example.api_booking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RoomTypeRequest(
    @Positive long hotelId,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 10000) String description,
    @Min(1) @Max(100) int capacity,
    @NotNull @DecimalMin("1") @Digits(integer = 10, fraction = 0) BigDecimal basePrice,
    boolean active) {}
