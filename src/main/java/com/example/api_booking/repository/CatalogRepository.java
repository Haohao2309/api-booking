package com.example.api_booking.repository;

import com.example.api_booking.common.api.*;
import com.example.api_booking.dto.ServiceResponseDTO;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
  private final JdbcTemplate db;

  public CatalogRepository(JdbcTemplate db) {
    this.db = db;
  }

  public PageResponse<ServiceResponseDTO> findActiveServices(PageQuery page, Long hotelId) {
    String where = " where active=true";
    List<Object> args = new ArrayList<>();
    if (hotelId != null) {
      where += " and hotel_id=?";
      args.add(hotelId);
    }
    long total =
        db.queryForObject(
            "select count(*) from hotel_services" + where, Long.class, args.toArray());
    args.add(page.size());
    args.add(page.offset());
    var rows =
        db.query(
            "select * from hotel_services"
                + where
                + " order by "
                + page.order()
                + " limit ? offset ?",
            (rs, n) ->
                new ServiceResponseDTO(
                    rs.getLong("id"),
                    rs.getLong("hotel_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getBigDecimal("unit_price"),
                    rs.getString("unit"),
                    rs.getString("image_url")),
            args.toArray());
    return PageResponse.of(rows, page.page(), page.size(), total);
  }
}
