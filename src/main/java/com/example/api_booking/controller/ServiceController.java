package com.example.api_booking.controller;

import com.example.api_booking.common.api.*;
import com.example.api_booking.service.ServiceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceController {
  private final ServiceService service;

  public ServiceController(ServiceService service) {
    this.service = service;
  }

  @GetMapping
  public Object getServices(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort,
      @RequestParam(required = false) Long hotelId) {
    return ApiResponse.ok(
        service.getServices(PageQuery.of(page, size, sort, "id", "name", "unit_price"), hotelId));
  }
}
