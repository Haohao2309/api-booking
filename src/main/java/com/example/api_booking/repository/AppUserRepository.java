package com.example.api_booking.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.api_booking.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
