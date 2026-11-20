package com.example.api_booking.repository;

import com.example.api_booking.common.exception.BusinessException;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingServiceRepository {
  private final JdbcTemplate db;

  public BookingServiceRepository(JdbcTemplate db) {
    this.db = db;
  }

  public record ServiceLine(
      long serviceId,
      String name,
      String unit,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal subtotal) {}

  public ServiceLine price(long hotel, long id, BigDecimal quantity) {
    return db
        .query(
            "select * from hotel_services where id=? and hotel_id=? and active=true for update",
            (r, n) ->
                new ServiceLine(
                    id,
                    r.getString("name"),
                    r.getString("unit"),
                    quantity,
                    r.getBigDecimal("unit_price"),
                    r.getBigDecimal("unit_price")
                        .multiply(quantity)
                        .setScale(2, java.math.RoundingMode.HALF_UP)),
            id,
            hotel)
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                BusinessException.invalid("Dịch vụ không thuộc khách sạn hoặc đã ngừng hoạt động"));
  }

  public void insert(long booking, ServiceLine s) {
    db.update(
        "insert into"
            + " booking_services(booking_id,service_id,service_name_snapshot,unit_snapshot,quantity,unit_price,subtotal)"
            + " values(?,?,?,?,?,?,?)",
        booking,
        s.serviceId(),
        s.name(),
        s.unit(),
        s.quantity(),
        s.unitPrice(),
        s.subtotal());
  }

  public java.util.List<ServiceLine> lines(long booking) {
    return db.query(
        "select * from booking_services where booking_id=? order by service_id",
        (r, n) ->
            new ServiceLine(
                r.getLong("service_id"),
                r.getString("service_name_snapshot"),
                r.getString("unit_snapshot"),
                r.getBigDecimal("quantity"),
                r.getBigDecimal("unit_price"),
                r.getBigDecimal("subtotal")),
        booking);
  }
}
