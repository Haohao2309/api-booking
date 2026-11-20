package com.example.api_booking.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

final class InsertSupport {
  private InsertSupport() {}

  static long insert(JdbcTemplate db, String sql, Object... args) {
    var key = new GeneratedKeyHolder();
    db.update(
        connection -> {
          var statement = connection.prepareStatement(sql, new String[] {"id"});
          for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
          return statement;
        },
        key);
    return java.util.Objects.requireNonNull(key.getKey()).longValue();
  }
}
