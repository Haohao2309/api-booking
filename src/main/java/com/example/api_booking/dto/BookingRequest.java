package com.example.api_booking.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record BookingRequest(
    @Positive Long roomId,
    @NotNull LocalDate checkIn,
    @NotNull LocalDate checkOut,
    @Min(1) @Max(100) int guests,
    @Size(max = 10) java.util.List<@NotNull @jakarta.validation.Valid RoomSelection> rooms,
    @Size(max = 20) java.util.List<@NotNull @jakarta.validation.Valid ServiceSelection> services) {
  public record RoomSelection(@Positive long roomId) {}

  public record ServiceSelection(
      @Positive long serviceId,
      @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2)
          java.math.BigDecimal quantity) {}

  public java.util.List<Long> selectedRooms() {
    if (roomId != null && rooms != null && !rooms.isEmpty())
      throw com.example.api_booking.common.exception.BusinessException.invalid(
          "Chỉ gửi roomId hoặc rooms");
    var ids =
        roomId != null
            ? java.util.List.of(roomId)
            : rooms == null
                ? java.util.List.<Long>of()
                : rooms.stream().map(RoomSelection::roomId).sorted().toList();
    if (ids.isEmpty() || ids.stream().distinct().count() != ids.size())
      throw com.example.api_booking.common.exception.BusinessException.invalid(
          "Danh sách phòng rỗng hoặc trùng");
    return ids;
  }

  public java.util.List<ServiceSelection> selectedServices() {
    return services == null ? java.util.List.of() : services;
  }
}
