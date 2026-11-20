package com.example.api_booking.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Booking(
    long id,
    long roomId,
    long customerId,
    String roomName,
    String customerName,
    String customerEmail,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    BigDecimal pricePerNight,
    BigDecimal totalPrice,
    String status,
    String requestKey) {}
