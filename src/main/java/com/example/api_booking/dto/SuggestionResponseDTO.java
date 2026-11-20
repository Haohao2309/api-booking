package com.example.api_booking.dto;

public record SuggestionResponseDTO(
    long id,
    long hotelId,
    String name,
    String category,
    String description,
    String address,
    java.math.BigDecimal latitude,
    java.math.BigDecimal longitude,
    String imageUrl) {}
