package com.example.api_booking.repository;

import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RefundRepository {
  private final JdbcTemplate db;

  public RefundRepository(JdbcTemplate db) {
    this.db = db;
  }

  public record Job(long id, long paymentId, String key, String token, int attempts) {}

  @Transactional
  public Job claimStripe() {
    var rows =
        db.query(
            "select r.* from refunds r join payments p on p.id=r.payment_id where"
                + " r.status='PENDING' and r.created_at>? and r.provider_refund_id is null and"
                + " r.retry_count<5 and p.provider='STRIPE' and r.next_retry_at<=current_timestamp"
                + " and (r.locked_until is null or r.locked_until<current_timestamp) order by r.id"
                + " limit 1 for update skip locked",
            (r, n) ->
                new Job(
                    r.getLong("id"),
                    r.getLong("payment_id"),
                    r.getString("idempotency_key"),
                    java.util.UUID.randomUUID().toString(),
                    r.getInt("retry_count")),
            java.sql.Timestamp.from(Instant.now().minusSeconds(20 * 3600)));
    if (rows.isEmpty()) return null;
    var job = rows.getFirst();
    db.update(
        "update refunds set locked_until=?,claim_token=? where id=?",
        java.sql.Timestamp.from(Instant.now().plusSeconds(120)),
        job.token(),
        job.id());
    return job;
  }

  public void accepted(Job job, String providerId, boolean complete) {
    db.update(
        "update refunds set"
            + " provider_refund_id=?,status=?,refunded_at=?,locked_until=null,claim_token=null,last_error=null,updated_at=current_timestamp"
            + " where id=? and claim_token=? and status='PENDING'",
        providerId,
        complete ? "REFUNDED" : "PENDING",
        complete ? java.sql.Timestamp.from(Instant.now()) : null,
        job.id(),
        job.token());
  }

  public void retry(Job job) {
    db.update(
        "update refunds set"
            + " retry_count=retry_count+1,next_retry_at=?,locked_until=null,claim_token=null,last_error='Gateway"
            + " result unavailable; same idempotency key retained' where id=? and claim_token=?",
        java.sql.Timestamp.from(Instant.now().plusSeconds(60L * (job.attempts() + 1))),
        job.id(),
        job.token());
  }

  @Transactional
  public Job claimVnpay() {
    var rows =
        db.query(
            "select r.* from refunds r join payments p on p.id=r.payment_id where"
                + " r.status='PENDING' and r.retry_count<96 and p.provider='VNPAY' and"
                + " r.next_retry_at<=current_timestamp and (r.locked_until is null or"
                + " r.locked_until<current_timestamp) order by r.id limit 1 for update skip locked",
            (r, n) ->
                new Job(
                    r.getLong("id"),
                    r.getLong("payment_id"),
                    r.getString("idempotency_key"),
                    java.util.UUID.randomUUID().toString(),
                    r.getInt("retry_count")));
    if (rows.isEmpty()) return null;
    var job = rows.getFirst();
    db.update(
        "update refunds set locked_until=?,claim_token=? where id=?",
        java.sql.Timestamp.from(Instant.now().plusSeconds(120)),
        job.token(),
        job.id());
    return job;
  }

  public boolean markSubmission(Job job) {
    return db.update(
            "update refunds set request_sent_at=current_timestamp where id=? and claim_token=? and"
                + " request_sent_at is null",
            job.id(),
            job.token())
        == 1;
  }

  public void pollLater(Job job) {
    db.update(
        "update refunds set"
            + " retry_count=retry_count+1,next_retry_at=?,locked_until=null,claim_token=null,last_error='Awaiting"
            + " verified refund outcome; do not resubmit refund' where id=? and claim_token=?",
        java.sql.Timestamp.from(Instant.now().plusSeconds(900)),
        job.id(),
        job.token());
  }

  @Transactional
  public void updateVerified(
      String key,
      String providerId,
      String transaction,
      java.math.BigDecimal amount,
      String status) {
    var rows =
        db.queryForList(
            "select r.id,r.amount,r.provider_refund_id,p.provider_transaction_id from refunds r"
                + " join payments p on p.id=r.payment_id where r.idempotency_key=? and"
                + " p.provider='STRIPE' for update",
            key);
    if (rows.isEmpty()) return;
    var row = rows.getFirst();
    if (providerId == null
        || providerId.isBlank()
        || row.get("provider_refund_id") != null
            && !providerId.equals(row.get("provider_refund_id"))
        || ((java.math.BigDecimal) row.get("amount")).compareTo(amount) != 0
        || !transaction.equals(row.get("provider_transaction_id")))
      throw com.example.api_booking.common.exception.BusinessException.conflict(
          "Kết quả hoàn tiền không khớp");
    if (!java.util.Set.of("succeeded", "failed", "canceled", "pending", "requires_action")
        .contains(status)) return;
    String state =
        status.equals("succeeded")
            ? "REFUNDED"
            : java.util.Set.of("failed", "canceled").contains(status) ? "FAILED" : "PENDING";
    db.update(
        "update refunds set"
            + " provider_refund_id=?,status=?,refunded_at=?,updated_at=current_timestamp where id=?"
            + " and (status='PENDING' or (status='FAILED' and ?='REFUNDED'))",
        providerId,
        state,
        state.equals("REFUNDED") ? java.sql.Timestamp.from(Instant.now()) : null,
        row.get("id"),
        state);
  }
}
