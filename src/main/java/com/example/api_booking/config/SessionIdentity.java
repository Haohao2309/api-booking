package com.example.api_booking.config;

public record SessionIdentity(String email, long version)
    implements java.security.Principal, java.io.Serializable {
  @Override
  public String getName() {
    return email;
  }
}
