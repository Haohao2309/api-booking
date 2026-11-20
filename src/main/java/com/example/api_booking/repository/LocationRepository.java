package com.example.api_booking.repository;
import com.example.api_booking.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LocationRepository extends JpaRepository<Location, Long> {}
