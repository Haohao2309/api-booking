package com.example.api_booking.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Storage for one-time password reset codes. Callers lock the owning account row first. */
@Repository
public class PasswordResetRepository {
  private final JdbcTemplate db;

  public PasswordResetRepository(JdbcTemplate db) {
    this.db = db;
  }

  public record ResetCode(
      long id,
      long userId,
      String codeHash,
      Instant expiresAt,
      int failedAttempts,
      Instant usedAt,
      Instant createdAt) {}

  private static OffsetDateTime utc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }

  private static Instant instant(java.sql.ResultSet r, String column) throws java.sql.SQLException {
    var value = r.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  public Optional<ResetCode> latest(long userId) {
    return db
        .query(
            "select * from password_reset_codes where user_id=? order by id desc",
            (r, n) ->
                new ResetCode(
                    r.getLong("id"),
                    r.getLong("user_id"),
                    r.getString("code_hash"),
                    instant(r, "expires_at"),
                    r.getInt("failed_attempts"),
                    instant(r, "used_at"),
                    instant(r, "created_at")),
            userId)
        .stream()
        .findFirst();
  }

  public long create(long userId, String codeHash, Instant expiresAt, Instant now) {
    return InsertSupport.insert(
        db,
        "insert into password_reset_codes(user_id,code_hash,expires_at,created_at) values(?,?,?,?)",
        userId,
        codeHash,
        utc(expiresAt),
        utc(now));
  }

  /** Retires every unused code of the account, so only the newest code can ever be accepted. */
  public void retireActive(long userId, Instant now) {
    db.update(
        "update password_reset_codes set used_at=? where user_id=? and used_at is null",
        utc(now),
        userId);
  }

  public void failed(long id) {
    db.update(
        "update password_reset_codes set failed_attempts=failed_attempts+1 where id=?", id);
  }

  public void used(long id, Instant now) {
    db.update("update password_reset_codes set used_at=? where id=?", utc(now), id);
  }
}
