package com.example.api_booking.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Presence flags preserve PATCH semantics: omitted keeps a value; null clears phone only. */
public class ProfileUpdateRequest {
  private String fullName;
  private String phone;
  private boolean fullNamePresent;
  private boolean phonePresent;

  @Size(min = 1, max = 150)
  public String getFullName() {
    return fullName;
  }

  public void setFullName(String value) {
    fullNamePresent = true;
    fullName = value;
  }

  @Pattern(
      regexp = "\\+?[0-9]{8,15}",
      message = "Số điện thoại gồm 8–15 chữ số, có thể bắt đầu bằng +")
  public String getPhone() {
    return phone;
  }

  public void setPhone(String value) {
    phonePresent = true;
    phone = value;
  }

  public boolean hasFullName() {
    return fullNamePresent;
  }

  public boolean hasPhone() {
    return phonePresent;
  }
}
