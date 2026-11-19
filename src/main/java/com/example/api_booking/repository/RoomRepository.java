package com.example.api_booking.repository;

import com.example.api_booking.common.api.*;
import com.example.api_booking.common.exception.BusinessException;
import com.example.api_booking.dto.RoomRequest;
import com.example.api_booking.entity.Room;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {
  private final JdbcTemplate db;
  private static final String PRICE =
      "coalesce((select base_price from room_types t where t.id=rooms.room_type_id),rooms.price)";
  private static final String CAPACITY =
      "coalesce((select capacity from room_types t where t.id=rooms.room_type_id),rooms.capacity)";
  private static final String SELECT =
      "select rooms.*, "
          + PRICE
          + " as effective_price, "
          + CAPACITY
          + " as effective_capacity, coalesce((select name from room_types t where"
          + " t.id=rooms.room_type_id),rooms.type) as effective_type from rooms";

  public RoomRepository(JdbcTemplate db) {
    this.db = db;
  }

  private final org.springframework.jdbc.core.RowMapper<Room> mapper =
      (r, n) ->
          new Room(
              r.getLong("id"),
              r.getString("code"),
              r.getString("name"),
              r.getString("effective_type"),
              r.getBigDecimal("effective_price"),
              r.getInt("effective_capacity"),
              r.getBoolean("locked"),
              r.getLong("version"),
              List.of());

  public Room find(long id, boolean lock) {
    return db.query(SELECT + " where id=?" + (lock ? " for update" : ""), mapper, id).stream()
        .findFirst()
        .orElseThrow(BusinessException::missing);
  }

  public Long roomTypeId(long id) {
    return db.queryForObject("select room_type_id from rooms where id=?", Long.class, id);
  }

  public Long hotelId(long id) {
    return db.queryForObject(
        "select t.hotel_id from rooms r left join room_types t on t.id=r.room_type_id where r.id=?",
        Long.class,
        id);
  }

  public record Amenity(long id, String name, String icon) {}

  public List<Amenity> amenities(long id) {
    return db.query(
        "select a.* from amenities a join room_type_amenities link on link.amenity_id=a.id join"
            + " rooms r on r.room_type_id=link.room_type_id where r.id=? and a.active=true order by"
            + " a.id",
        (r, n) -> new Amenity(r.getLong("id"), r.getString("name"), r.getString("icon")),
        id);
  }

  public List<String> images(long id) {
    return db.queryForList(
        "select url from room_images where room_id=? order by position", String.class, id);
  }

  public PageResponse<Room> list(
      PageQuery p, String status, boolean publicOnly, java.time.LocalDate today) {
    String where = " where 1=1";
    List<Object> args = new ArrayList<>();
    if (publicOnly) where += " and not locked";
    if ("LOCKED".equals(status)) where += " and locked";
    String booked =
        "exists(select 1 from bookings b where b.room_id=rooms.id and b.status='CONFIRMED' and"
            + " b.check_out>?)";
    if ("BOOKED".equals(status)) where += " and not locked and " + booked;
    if ("AVAILABLE".equals(status)) where += " and not locked and not " + booked;
    if ("BOOKED".equals(status) || "AVAILABLE".equals(status)) args.add(today);
    long count =
        db.queryForObject("select count(*) from rooms" + where, Long.class, args.toArray());
    args.add(p.size());
    args.add(p.offset());
    return PageResponse.of(
        db.query(
            SELECT
                + where
                + " order by "
                + p.order().replace("price", "effective_price")
                + " limit ? offset ?",
            mapper,
            args.toArray()),
        p.page(),
        p.size(),
        count);
  }

  public long create(RoomRequest r) {
    validateRoomType(r.roomTypeId());
    Long id =
        InsertSupport.insert(
            db,
            "insert into rooms(code,name,type,price,capacity) values(?,?,?,?,?)",
            r.code().trim(),
            r.name().trim(),
            r.type().trim(),
            r.price(),
            r.capacity());
    images(id, r.images());
    if (r.roomTypeId() != null)
      db.update("update rooms set room_type_id=? where id=?", r.roomTypeId(), id);
    return id;
  }

  public PageResponse<Room> search(
      PageQuery p,
      java.time.LocalDate start,
      java.time.LocalDate end,
      Integer guests,
      Long hotelId,
      Long roomTypeId,
      java.math.BigDecimal minPrice,
      java.math.BigDecimal maxPrice) {
    String where = " where not locked";
    List<Object> args = new ArrayList<>();
    if (start != null) {
      where +=
          " and not exists(select 1 from bookings b where b.room_id=rooms.id and b.status in"
              + " ('PENDING','CONFIRMED') and (b.hold_expires_at is null or"
              + " b.hold_expires_at>current_timestamp) and b.check_in<? and b.check_out>?)";
      args.add(end);
      args.add(start);
      where +=
          " and not exists(select 1 from room_inventory i where i.room_id=rooms.id and"
              + " i.stay_date>=? and i.stay_date<? and i.state<>'AVAILABLE' and (i.hold_expires_at"
              + " is null or i.hold_expires_at>current_timestamp))";
      args.add(start);
      args.add(end);
    }
    if (guests != null) {
      where += " and " + CAPACITY + ">=?";
      args.add(guests);
    }
    if (hotelId != null) {
      where += " and room_type_id in(select id from room_types where hotel_id=? and active=true)";
      args.add(hotelId);
    }
    if (roomTypeId != null) {
      where += " and room_type_id=?";
      args.add(roomTypeId);
    }
    where +=
        " and (room_type_id is null or exists(select 1 from room_types t where"
            + " t.id=rooms.room_type_id and t.active=true))";
    if (minPrice != null) {
      where += " and " + PRICE + ">=?";
      args.add(minPrice);
    }
    if (maxPrice != null) {
      where += " and " + PRICE + "<=?";
      args.add(maxPrice);
    }
    long count =
        db.queryForObject("select count(*) from rooms" + where, Long.class, args.toArray());
    args.add(p.size());
    args.add(p.offset());
    return PageResponse.of(
        db.query(
            SELECT
                + where
                + " order by "
                + p.order().replace("price", "effective_price")
                + " limit ? offset ?",
            mapper,
            args.toArray()),
        p.page(),
        p.size(),
        count);
  }

  public void update(long id, RoomRequest r) {
    validateRoomType(r.roomTypeId());
    db.update(
        "update rooms set code=?,name=?,type=?,price=?,capacity=?,version=version+1 where id=?",
        r.code().trim(),
        r.name().trim(),
        r.type().trim(),
        r.price(),
        r.capacity(),
        id);
    images(id, r.images());
    if (r.roomTypeId() != null)
      db.update("update rooms set room_type_id=? where id=?", r.roomTypeId(), id);
  }

  private void validateRoomType(Long id) {
    if (id != null
        && db.queryForObject(
                "select count(*) from room_types where id=? and active=true", Long.class, id)
            != 1)
      throw BusinessException.invalid("Loại phòng không tồn tại hoặc đã ngừng hoạt động");
  }

  private void images(long id, List<String> urls) {
    db.update("delete from room_images where room_id=?", id);
    for (int i = 0; i < urls.size(); i++)
      db.update("insert into room_images(room_id,position,url) values(?,?,?)", id, i, urls.get(i));
  }

  public void locked(long id, boolean value) {
    db.update("update rooms set locked=?,version=version+1 where id=?", value, id);
  }
}
