package com.example.api_booking.dto;

import jakarta.validation.constraints.*;

public record PartnerRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 200) String contact,
    @NotBlank @Size(max = 30) String taxCode,
    @NotBlank @Size(max = 500) String address) {}
