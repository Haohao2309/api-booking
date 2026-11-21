package com.example.api_booking.service;

import com.example.api_booking.common.api.*;
import com.example.api_booking.common.exception.BusinessException;
import com.example.api_booking.dto.ServiceResponseDTO;
import com.example.api_booking.repository.CatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServiceService {
  private final CatalogRepository repository;

  public ServiceService(CatalogRepository repository) {
    this.repository = repository;
  }

  public PageResponse<ServiceResponseDTO> getServices(PageQuery page, Long hotelId) {
    if (hotelId != null && hotelId <= 0)
      throw BusinessException.invalid("hotelId phải là số dương");
    return repository.findActiveServices(page, hotelId);
  }
}
