package com.example.api_booking.repository;

import com.example.api_booking.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryRepository {
  private final JdbcTemplate db;

  public InventoryRepository(JdbcTemplate db) {
    this.db = db;
  }

  public record Hotel(long id, String policy, String timezone, LocalTime checkInTime) {}

  public java.util.Optional<Hotel> hotelForRoom(long room) {
    return db
        .query(
            "select h.* from hotels h join room_types t on t.hotel_id=h.id join rooms r on"
                + " r.room_type_id=t.id where r.id=? and t.active=true",
            (r, n) ->
                new Hotel(
                    r.getLong("id"),
                    r.getString("cancellation_policy"),
                    r.getString("timezone"),
                    r.getObject("check_in_time", LocalTime.class)),
            room)
        .stream()
        .findFirst();
  }

  public void reserve(
      long booking,
      long room,
      String label,
      BigDecimal price,
      LocalDate start,
      LocalDate end,
      Instant expires) {
    int nights = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(start, end));
    long line =
        InsertSupport.insert(
            db,
            "insert into"
                + " booking_rooms(booking_id,room_id,room_label_snapshot,price_per_night,number_of_nights,subtotal)"
                + " values(?,?,?,?,?,?)",
            booking,
            room,
            label,
            price,
            nights,
            price.multiply(BigDecimal.valueOf(nights)));
    // Caller holds the physical room row lock, including for missing inventory dates.
    for (LocalDate day = start; day.isBefore(end); day = day.plusDays(1)) {
      var states =
          db.queryForList(
              "select state from room_inventory where room_id=? and stay_date=?",
              String.class,
              room,
              day);
      if (states.isEmpty())
        db.update(
            "insert into room_inventory(room_id,stay_date,state,booking_room_id,hold_expires_at)"
                + " values(?,?,'HELD',?,?)",
            room,
            day,
            line,
            java.sql.Timestamp.from(expires));
      else if (db.update(
              "update room_inventory set"
                  + " state='HELD',booking_room_id=?,hold_expires_at=?,version=version+1,updated_at=current_timestamp"
                  + " where room_id=? and stay_date=? and state='AVAILABLE'",
              line,
              java.sql.Timestamp.from(expires),
              room,
              day)
          != 1) throw new BusinessException(409, "ROOM_NOT_AVAILABLE", "Phòng không còn trống");
    }
  }

  public void release(long booking) {
    db.update(
        "update room_inventory set"
            + " state='AVAILABLE',booking_room_id=null,hold_expires_at=null,version=version+1,updated_at=current_timestamp"
            + " where booking_room_id in(select id from booking_rooms where booking_id=?)",
        booking);
  }

  public void lockBookingRooms(long booking, long legacyRoom) {
    var ids =
        db.queryForList(
            "select room_id from booking_rooms where booking_id=? order by room_id",
            Long.class,
            booking);
    if (ids.isEmpty()) ids = java.util.List.of(legacyRoom);
    for (long id : ids)
      db.queryForObject("select id from rooms where id=? for update", Long.class, id);
  }

  public void protectPaid(long booking) {
    db.update(
        "update room_inventory set"
            + " hold_expires_at=null,version=version+1,updated_at=current_timestamp where"
            + " booking_room_id in(select id from booking_rooms where booking_id=?) and"
            + " state='HELD'",
        booking);
  }

  public void confirm(long booking) {
    db.update(
        "update room_inventory set"
            + " state='BOOKED',hold_expires_at=null,version=version+1,updated_at=current_timestamp"
            + " where booking_room_id in(select id from booking_rooms where booking_id=?) and"
            + " state='HELD'",
        booking);
  }
}
