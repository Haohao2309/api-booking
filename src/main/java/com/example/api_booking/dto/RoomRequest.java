package com.example.api_booking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record RoomRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 80) String type,
    @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @Min(1) @Max(100) Integer capacity,
    @NotNull @Size(max = 20)
        List<@NotBlank @Size(max = 500) @Pattern(regexp = "https?://[^\\s]+") String> images,
    @Positive Long roomTypeId) {}
