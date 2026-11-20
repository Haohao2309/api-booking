package com.example.api_booking.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
  @GetMapping("/api/auth/csrf")
  public CsrfResponse csrf(CsrfToken csrfToken) {
    return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
  }

  @GetMapping("/api/v1/auth/csrf")
  public com.example.api_booking.common.api.ApiResponse<CsrfResponse> versioned(CsrfToken token) {
    return com.example.api_booking.common.api.ApiResponse.ok(csrf(token));
  }

  public record CsrfResponse(String headerName, String token) {}
}
