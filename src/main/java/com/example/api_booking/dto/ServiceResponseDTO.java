package com.example.api_booking.dto;

public record ServiceResponseDTO(
    long id,
    long hotelId,
    String name,
    String description,
    java.math.BigDecimal unitPrice,
    String unit,
    String imageUrl) {}
