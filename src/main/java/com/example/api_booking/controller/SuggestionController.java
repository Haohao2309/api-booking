package com.example.api_booking.controller;

import com.example.api_booking.common.api.*;
import com.example.api_booking.service.SuggestionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionController {
  private final SuggestionService service;

  public SuggestionController(SuggestionService service) {
    this.service = service;
  }

  @GetMapping
  public Object getSuggestions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,asc") String sort,
      @RequestParam(required = false) Long hotelId) {
    return ApiResponse.ok(
        service.getSuggestions(PageQuery.of(page, size, sort, "id", "name", "category"), hotelId));
  }
}
