package com.example.api_booking.repository;

import com.example.api_booking.common.api.*;
import com.example.api_booking.dto.SuggestionResponseDTO;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SuggestionRepository {
  private final JdbcTemplate db;

  public SuggestionRepository(JdbcTemplate db) {
    this.db = db;
  }

  public PageResponse<SuggestionResponseDTO> findActiveSuggestions(PageQuery page, Long hotelId) {
    String where = " where active=true";
    List<Object> args = new ArrayList<>();
    if (hotelId != null) {
      where += " and hotel_id=?";
      args.add(hotelId);
    }
    long total =
        db.queryForObject(
            "select count(*) from nearby_suggestions" + where, Long.class, args.toArray());
    args.add(page.size());
    args.add(page.offset());
    var rows =
        db.query(
            "select * from nearby_suggestions"
                + where
                + " order by "
                + page.order()
                + " limit ? offset ?",
            (rs, n) ->
                new SuggestionResponseDTO(
                    rs.getLong("id"),
                    rs.getLong("hotel_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getString("address"),
                    rs.getBigDecimal("latitude"),
                    rs.getBigDecimal("longitude"),
                    rs.getString("image_url")),
            args.toArray());
    return PageResponse.of(rows, page.page(), page.size(), total);
  }
}
