package com.example.api_booking.service;

import com.example.api_booking.common.api.*;
import com.example.api_booking.common.exception.BusinessException;
import com.example.api_booking.entity.Room;
import com.example.api_booking.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public room catalog: search and detail. Room administration (create, update, lock) belongs to
 * the staff catalog branch and is not exposed here.
 */
@Service
@Transactional(readOnly = true)
public class RoomService {
  private final RoomRepository rooms;
  private final BookingRepository bookings;
  private final java.time.ZoneId zone;

  public RoomService(
      RoomRepository rooms,
      BookingRepository bookings,
      @org.springframework.beans.factory.annotation.Value("${app.booking.zone:Asia/Ho_Chi_Minh}")
          String zone) {
    this.rooms = rooms;
    this.bookings = bookings;
    this.zone = java.time.ZoneId.of(zone);
  }

  public record RoomResponseDTO(
      long id,
      String code,
      String name,
      String type,
      java.math.BigDecimal price,
      int capacity,
      String status,
      long version,
      java.util.List<String> images,
      Long roomTypeId,
      Long hotelId,
      java.util.List<RoomRepository.Amenity> amenities) {}

  private RoomResponseDTO dto(Room r, boolean details) {
    return new RoomResponseDTO(
        r.id(),
        r.code(),
        r.name(),
        r.type(),
        r.price(),
        r.capacity(),
        r.locked()
            ? "LOCKED"
            : bookings.booked(r.id(), java.time.LocalDate.now(zone)) ? "BOOKED" : "AVAILABLE",
        r.version(),
        details ? rooms.images(r.id()) : java.util.List.of(),
        rooms.roomTypeId(r.id()),
        rooms.hotelId(r.id()),
        details ? rooms.amenities(r.id()) : java.util.List.of());
  }

  /** Locked rooms are hidden from the public, as if they did not exist. */
  public RoomResponseDTO getPublicRoom(long id) {
    Room r = rooms.find(id, false);
    if (r.locked()) throw BusinessException.missing();
    return dto(r, true);
  }

  public PageResponse<RoomResponseDTO> searchRooms(
      PageQuery page,
      java.time.LocalDate start,
      java.time.LocalDate end,
      Integer guests,
      Long hotelId,
      Long roomTypeId,
      java.math.BigDecimal minPrice,
      java.math.BigDecimal maxPrice) {
    if ((start == null) != (end == null))
      throw BusinessException.invalid("Cần gửi đồng thời checkIn và checkOut");
    if (start != null
        && (start.isBefore(java.time.LocalDate.now(zone))
            || !end.isAfter(start)
            || java.time.temporal.ChronoUnit.DAYS.between(start, end) > 365))
      throw BusinessException.invalid("Khoảng lưu trú phải từ hôm nay, 1–365 đêm");
    if (guests != null && (guests < 1 || guests > 100)
        || hotelId != null && hotelId < 1
        || roomTypeId != null && roomTypeId < 1)
      throw BusinessException.invalid("Bộ lọc phòng không hợp lệ");
    if (minPrice != null && minPrice.signum() < 0
        || maxPrice != null && maxPrice.signum() < 0
        || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)
      throw BusinessException.invalid("Khoảng giá không hợp lệ");
    var result = rooms.search(page, start, end, guests, hotelId, roomTypeId, minPrice, maxPrice);
    return PageResponse.of(
        result.items().stream().map(r -> dto(r, false)).toList(),
        page.page(),
        page.size(),
        result.totalItems());
  }
}
