package com.example.api_booking.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationRepository {
  private final JdbcTemplate db;
  private final String publicBaseUrl;

  public NotificationRepository(
      JdbcTemplate db,
      @org.springframework.beans.factory.annotation.Value("${app.public-base-url:}")
          String publicBaseUrl) {
    this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    this.db = db;
  }

  public record Delivery(
      long id, String recipient, String subject, String body, int attempts, String token) {}

  public void enqueue(long booking, String code, String recipient, String templateCode) {
    var templates =
        db.queryForList(
            "select * from email_templates where template_code=? and active=true", templateCode);
    if (templates.isEmpty()) return;
    var template = templates.getFirst();
    // Named replacements only: templates are plain text, never executable expressions.
    db.update(
        "insert into notifications(booking_id,event_key,recipient,subject,body,template_id)"
            + " values(?,?,?,?,?,?)",
        booking,
        templateCode + ":" + booking,
        recipient,
        template.get("subject_template").toString().replace("{bookingCode}", code),
        template.get("body_template").toString().replace("{bookingCode}", code)
            + (publicBaseUrl.isBlank()
                ? ""
                : "\nTải chứng từ sau khi đăng nhập: "
                    + publicBaseUrl
                    + "/api/v1/bookings/"
                    + booking
                    + "/document"),
        template.get("id"));
  }

  @Transactional
  public Delivery claim() {
    var rows =
        db.query(
            "select * from notifications where status='PENDING' and"
                + " next_attempt_at<=current_timestamp and (locked_until is null or"
                + " locked_until<current_timestamp) order by id limit 1 for update skip locked",
            (r, n) ->
                new Delivery(
                    r.getLong("id"),
                    r.getString("recipient"),
                    r.getString("subject"),
                    r.getString("body"),
                    r.getInt("attempts"),
                    UUID.randomUUID().toString()));
    if (rows.isEmpty()) return null;
    var row = rows.getFirst();
    db.update(
        "update notifications set locked_until=?,claim_token=? where id=?",
        java.sql.Timestamp.from(Instant.now().plusSeconds(120)),
        row.token(),
        row.id());
    return row;
  }

  public void sent(Delivery d) {
    db.update(
        "update notifications set"
            + " status='SENT',sent_at=current_timestamp,locked_until=null,claim_token=null where"
            + " id=? and claim_token=?",
        d.id(),
        d.token());
  }

  public void failed(Delivery d) {
    int attempts = d.attempts() + 1;
    db.update(
        "update notifications set"
            + " attempts=?,status=?,next_attempt_at=?,locked_until=null,claim_token=null where id=?"
            + " and claim_token=?",
        attempts,
        attempts >= 5 ? "FAILED" : "PENDING",
        java.sql.Timestamp.from(Instant.now().plusSeconds(Math.min(3600, 30L * (1L << attempts)))),
        d.id(),
        d.token());
  }
}
