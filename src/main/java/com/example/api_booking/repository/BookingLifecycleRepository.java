package com.example.api_booking.repository;

import com.example.api_booking.common.exception.BusinessException;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingLifecycleRepository {
  private final JdbcTemplate db;

  public BookingLifecycleRepository(JdbcTemplate db) {
    this.db = db;
  }

  public record Lifecycle(
      long id,
      Long hotelId,
      String code,
      String policy,
      Instant checkInAt,
      Instant freeCancelUntil,
      Instant expires,
      String timezone) {}

  public Lifecycle find(long id, boolean lock) {
    return db
        .query(
            "select * from bookings where id=?" + (lock ? " for update" : ""),
            (r, n) ->
                new Lifecycle(
                    r.getLong("id"),
                    r.getObject("hotel_id", Long.class),
                    r.getString("booking_code"),
                    r.getString("cancellation_policy_snapshot"),
                    instant(r.getTimestamp("check_in_at")),
                    instant(r.getTimestamp("free_cancel_until")),
                    instant(r.getTimestamp("hold_expires_at")),
                    r.getString("hotel_timezone_snapshot")),
            id)
        .stream()
        .findFirst()
        .orElseThrow(BusinessException::missing);
  }

  private static Instant instant(java.sql.Timestamp value) {
    return value == null ? null : value.toInstant();
  }

  public record RoomLine(
      long roomId,
      String label,
      java.math.BigDecimal pricePerNight,
      int nights,
      java.math.BigDecimal subtotal) {}

  public java.util.List<RoomLine> roomLines(long id) {
    return db.query(
        "select * from booking_rooms where booking_id=? order by room_id",
        (r, n) ->
            new RoomLine(
                r.getLong("room_id"),
                r.getString("room_label_snapshot"),
                r.getBigDecimal("price_per_night"),
                r.getInt("number_of_nights"),
                r.getBigDecimal("subtotal")),
        id);
  }

  public void snapshot(
      long id,
      long hotel,
      String policy,
      Instant arrival,
      Instant freeUntil,
      Instant expires,
      String timezone) {
    db.update(
        "update bookings set"
            + " hotel_id=?,cancellation_policy_snapshot=?,check_in_at=?,free_cancel_until=?,hold_expires_at=?,hotel_timezone_snapshot=?"
            + " where id=?",
        hotel,
        policy,
        java.sql.Timestamp.from(arrival),
        java.sql.Timestamp.from(freeUntil),
        java.sql.Timestamp.from(expires),
        timezone,
        id);
  }

  public void paid(long id) {
    db.update("update bookings set hold_expires_at=null where id=?", id);
  }

  public void cancel(long id, String reason) {
    db.update(
        "update bookings set"
            + " status='CANCELLED',cancelled_at=current_timestamp,cancellation_reason=?,hold_expires_at=null"
            + " where id=?",
        reason,
        id);
  }

  public void expire(long id) {
    db.update(
        "update bookings set status='EXPIRED',hold_expires_at=null where id=? and status='PENDING'",
        id);
  }

  public java.util.List<Long> due() {
    return db.queryForList(
        "select id from bookings where status='PENDING' and hold_expires_at<=current_timestamp"
            + " order by id limit 100",
        Long.class);
  }

  public java.util.List<Long> dueForRoom(long room) {
    return db.queryForList(
        "select id from bookings b where (room_id=? or exists(select 1 from booking_rooms br where"
            + " br.booking_id=b.id and br.room_id=?)) and status='PENDING' and"
            + " hold_expires_at<=current_timestamp order by id",
        Long.class,
        room,
        room);
  }
}
