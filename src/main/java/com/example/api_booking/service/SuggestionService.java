package com.example.api_booking.service;

import com.example.api_booking.common.api.*;
import com.example.api_booking.common.exception.BusinessException;
import com.example.api_booking.dto.SuggestionResponseDTO;
import com.example.api_booking.repository.SuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SuggestionService {
  private final SuggestionRepository repository;

  public SuggestionService(SuggestionRepository repository) {
    this.repository = repository;
  }

  public PageResponse<SuggestionResponseDTO> getSuggestions(PageQuery page, Long hotelId) {
    if (hotelId != null && hotelId <= 0)
      throw BusinessException.invalid("hotelId phải là số dương");
    return repository.findActiveSuggestions(page, hotelId);
  }
}
