package com.example.api_booking.repository;

import com.example.api_booking.common.api.*;
import com.example.api_booking.common.exception.BusinessException;
import com.example.api_booking.dto.BookingRequest;
import com.example.api_booking.entity.Booking;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingRepository {
  private final JdbcTemplate db;
  private final NotificationRepository notifications;

  public BookingRepository(JdbcTemplate db, NotificationRepository notifications) {
    this.notifications = notifications;
    this.db = db;
  }

  private final org.springframework.jdbc.core.RowMapper<Booking> mapper =
      (r, n) ->
          new Booking(
              r.getLong("id"),
              r.getLong("room_id"),
              r.getLong("customer_id"),
              r.getString("room_name"),
              r.getString("customer_name"),
              r.getString("customer_email"),
              r.getObject("check_in", LocalDate.class),
              r.getObject("check_out", LocalDate.class),
              r.getInt("guests"),
              r.getBigDecimal("price_per_night"),
              r.getBigDecimal("total_price"),
              r.getString("status"),
              r.getString("request_key"));

  public Booking find(long id) {
    return db.query("select * from bookings where id=?", mapper, id).stream()
        .findFirst()
        .orElseThrow(BusinessException::missing);
  }

  public Optional<Booking> byKey(long user, String key) {
    return db
        .query("select * from bookings where customer_id=? and request_key=?", mapper, user, key)
        .stream()
        .findFirst();
  }

  public boolean overlaps(long room, LocalDate start, LocalDate end, long exclude) {
    return db.queryForObject(
            "select count(*) from bookings b where (room_id=? or exists(select 1 from booking_rooms"
                + " br where br.booking_id=b.id and br.room_id=?)) and id<>? and status in"
                + " ('PENDING','CONFIRMED') and check_in<? and check_out>?",
            Long.class,
            room,
            room,
            exclude,
            end,
            start)
        > 0;
  }

  public boolean active(long room, LocalDate today) {
    return db.queryForObject(
            "select count(*) from bookings b where (room_id=? or exists(select 1 from booking_rooms"
                + " br where br.booking_id=b.id and br.room_id=?)) and status in"
                + " ('PENDING','CONFIRMED') and check_out>?",
            Long.class,
            room,
            room,
            today)
        > 0;
  }

  public boolean booked(long room, LocalDate today) {
    return db.queryForObject(
            "select count(*) from bookings b where (room_id=? or exists(select 1 from booking_rooms"
                + " br where br.booking_id=b.id and br.room_id=?)) and status='CONFIRMED' and"
                + " check_out>?",
            Long.class,
            room,
            room,
            today)
        > 0;
  }

  public PageResponse<Booking> list(PageQuery p, String status, Long owner, String bookingCode) {
    String where = " where 1=1";
    List<Object> args = new ArrayList<>();
    if (status != null) {
      where += " and status=?";
      args.add(status);
    }
    if (bookingCode != null) {
      where += " and booking_code=?";
      args.add(bookingCode);
    }
    if (owner != null) {
      where += " and customer_id=?";
      args.add(owner);
    }
    long count =
        db.queryForObject("select count(*) from bookings" + where, Long.class, args.toArray());
    args.add(p.size());
    args.add(p.offset());
    return PageResponse.of(
        db.query(
            "select * from bookings" + where + " order by " + p.order() + " limit ? offset ?",
            mapper,
            args.toArray()),
        p.page(),
        p.size(),
        count);
  }

  public long create(
      BookingRequest r,
      long user,
      String name,
      String email,
      String room,
      BigDecimal price,
      BigDecimal total,
      String key) {
    return InsertSupport.insert(
        db,
        "insert into"
            + " bookings(room_id,customer_id,customer_name,customer_email,room_name,check_in,check_out,guests,price_per_night,total_price,request_key,booking_code)"
            + " values(?,?,?,?,?,?,?,?,?,?,?,?)",
        r.selectedRooms().getFirst(),
        user,
        name,
        email,
        room,
        r.checkIn(),
        r.checkOut(),
        r.guests(),
        price,
        total,
        key,
        "BK-" + java.util.UUID.randomUUID());
  }

  public void confirm(Booking b, long actor) {
    if (db.update(
            "update bookings set status='CONFIRMED',confirmed_by=?,confirmed_at=current_timestamp"
                + " where id=? and status='PENDING'",
            actor,
            b.id())
        != 1) throw BusinessException.conflict("Đơn đã được xử lý");
    notifications.enqueue(
        b.id(),
        db.queryForObject("select booking_code from bookings where id=?", String.class, b.id()),
        b.customerEmail(),
        "BOOKING_CONFIRMED");
  }
}
