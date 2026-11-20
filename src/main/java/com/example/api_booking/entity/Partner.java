package com.example.api_booking.entity;

public record Partner(
    long id, String name, String contact, String taxCode, String address, long version) {}
