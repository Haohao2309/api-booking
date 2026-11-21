package com.example.api_booking.controller;

import com.example.api_booking.common.api.*;
import com.example.api_booking.service.RoomService;
import org.springframework.web.bind.annotation.*;

/** Public room catalog. Admin room endpoints are added by the staff catalog branch. */
@RestController
@RequestMapping("/api/v1")
public class RoomController {
  private final RoomService rooms;

  public RoomController(RoomService rooms) {
    this.rooms = rooms;
  }

  @GetMapping("/rooms")
  public Object publicRooms(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort,
      @RequestParam(required = false) java.time.LocalDate checkIn,
      @RequestParam(required = false) java.time.LocalDate checkOut,
      @RequestParam(required = false) Integer guests,
      @RequestParam(required = false) Long hotelId,
      @RequestParam(required = false) Long roomTypeId,
      @RequestParam(required = false) java.math.BigDecimal minPrice,
      @RequestParam(required = false) java.math.BigDecimal maxPrice) {
    return ApiResponse.ok(
        rooms.searchRooms(
            PageQuery.of(page, size, sort, "id", "name", "code", "price"),
            checkIn,
            checkOut,
            guests,
            hotelId,
            roomTypeId,
            minPrice,
            maxPrice));
  }

  @GetMapping("/rooms/{id}")
  public Object publicDetail(@PathVariable long id) {
    return ApiResponse.ok(rooms.getPublicRoom(id));
  }
}
