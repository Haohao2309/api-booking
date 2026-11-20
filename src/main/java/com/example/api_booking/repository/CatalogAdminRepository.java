package com.example.api_booking.repository;

import com.example.api_booking.common.api.*;
import com.example.api_booking.common.exception.BusinessException;
import com.example.api_booking.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogAdminRepository {
  private final JdbcTemplate db;

  public CatalogAdminRepository(JdbcTemplate db) {
    this.db = db;
  }

  public record Hotel(
      long id,
      String name,
      String address,
      String contactEmail,
      String phone,
      String timezone,
      java.time.LocalTime checkInTime,
      java.time.LocalTime checkOutTime,
      String cancellationPolicy,
      long version) {}

  public record RoomType(
      long id,
      long hotelId,
      String name,
      String description,
      int capacity,
      java.math.BigDecimal basePrice,
      boolean active,
      long version) {}

  private final org.springframework.jdbc.core.RowMapper<Hotel> hotels =
      (r, n) ->
          new Hotel(
              r.getLong("id"),
              r.getString("name"),
              r.getString("address"),
              r.getString("contact_email"),
              r.getString("phone"),
              r.getString("timezone"),
              r.getObject("check_in_time", java.time.LocalTime.class),
              r.getObject("check_out_time", java.time.LocalTime.class),
              r.getString("cancellation_policy"),
              r.getLong("version"));
  private final org.springframework.jdbc.core.RowMapper<RoomType> types =
      (r, n) ->
          new RoomType(
              r.getLong("id"),
              r.getLong("hotel_id"),
              r.getString("name"),
              r.getString("description"),
              r.getInt("capacity"),
              r.getBigDecimal("base_price"),
              r.getBoolean("active"),
              r.getLong("version"));

  public Hotel hotel(long id, boolean lock) {
    return db
        .query("select * from hotels where id=?" + (lock ? " for update" : ""), hotels, id)
        .stream()
        .findFirst()
        .orElseThrow(BusinessException::missing);
  }

  public RoomType roomType(long id, boolean lock) {
    return db
        .query("select * from room_types where id=?" + (lock ? " for update" : ""), types, id)
        .stream()
        .findFirst()
        .orElseThrow(BusinessException::missing);
  }

  public PageResponse<Hotel> hotels(PageQuery p) {
    return PageResponse.of(
        db.query("select * from hotels order by id limit ? offset ?", hotels, p.size(), p.offset()),
        p.page(),
        p.size(),
        db.queryForObject("select count(*) from hotels", Long.class));
  }

  public PageResponse<RoomType> roomTypes(PageQuery p, Long hotel) {
    String where = hotel == null ? "" : " where hotel_id=?";
    var args = new java.util.ArrayList<Object>();
    if (hotel != null) args.add(hotel);
    long count =
        db.queryForObject("select count(*) from room_types" + where, Long.class, args.toArray());
    args.add(p.size());
    args.add(p.offset());
    return PageResponse.of(
        db.query(
            "select * from room_types" + where + " order by id limit ? offset ?",
            types,
            args.toArray()),
        p.page(),
        p.size(),
        count);
  }

  public long create(HotelRequest r) {
    return InsertSupport.insert(
        db,
        "insert into"
            + " hotels(name,address,contact_email,phone,latitude,longitude,timezone,check_in_time,check_out_time,cancellation_policy)"
            + " values(?,?,?,?,?,?,?,?,?,?)",
        r.name().trim(),
        r.address().trim(),
        r.contactEmail(),
        r.phone(),
        r.latitude(),
        r.longitude(),
        r.timezone(),
        r.checkInTime(),
        r.checkOutTime(),
        r.cancellationPolicy());
  }

  public void update(long id, HotelRequest r) {
    db.update(
        "update hotels set"
            + " name=?,address=?,contact_email=?,phone=?,latitude=?,longitude=?,timezone=?,check_in_time=?,check_out_time=?,cancellation_policy=?,version=version+1,updated_at=current_timestamp"
            + " where id=?",
        r.name().trim(),
        r.address().trim(),
        r.contactEmail(),
        r.phone(),
        r.latitude(),
        r.longitude(),
        r.timezone(),
        r.checkInTime(),
        r.checkOutTime(),
        r.cancellationPolicy(),
        id);
  }

  public long create(RoomTypeRequest r) {
    return InsertSupport.insert(
        db,
        "insert into room_types(hotel_id,name,description,capacity,base_price,active)"
            + " values(?,?,?,?,?,?)",
        r.hotelId(),
        r.name().trim(),
        r.description(),
        r.capacity(),
        r.basePrice(),
        r.active());
  }

  public void update(long id, RoomTypeRequest r) {
    db.update(
        "update room_types set"
            + " hotel_id=?,name=?,description=?,capacity=?,base_price=?,active=?,version=version+1,updated_at=current_timestamp"
            + " where id=?",
        r.hotelId(),
        r.name().trim(),
        r.description(),
        r.capacity(),
        r.basePrice(),
        r.active(),
        id);
  }

  public java.util.List<Long> lockRooms(long type) {
    return db.queryForList(
        "select id from rooms where room_type_id=? order by id for update", Long.class, type);
  }
}
