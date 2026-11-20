package com.example.api_booking.entity;

import java.math.BigDecimal;
import java.util.List;

public record Room(
    long id,
    String code,
    String name,
    String type,
    BigDecimal price,
    int capacity,
    boolean locked,
    long version,
    List<String> images) {}
