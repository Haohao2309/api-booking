package com.example.api_booking.repository;
import com.example.api_booking.entity.Room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoomRepository extends JpaRepository<Room, Long> { List<Room> findByActiveTrueOrderByIdDesc(); List<Room> findByActiveTrueAndLocationIdOrderByIdDesc(Long locationId); }
