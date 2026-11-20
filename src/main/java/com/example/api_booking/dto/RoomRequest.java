package com.example.api_booking.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record RoomRequest(@NotBlank String number,@NotBlank String name,@NotBlank String type,String description,@NotNull @Min(1) Integer capacity,@NotNull @DecimalMin("0.0") BigDecimal pricePerNight,@NotNull Boolean active,Long locationId) {}
