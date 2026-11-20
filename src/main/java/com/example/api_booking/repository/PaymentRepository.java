package com.example.api_booking.repository;

import com.example.api_booking.common.api.*;
import com.example.api_booking.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {
  private final JdbcTemplate db;

  public PaymentRepository(JdbcTemplate db) {
    this.db = db;
  }

  public record Payment(
      long id,
      long bookingId,
      String provider,
      String reference,
      String transactionId,
      BigDecimal amount,
      String currency,
      String status,
      String reviewReason,
      boolean appliedToBooking,
      String paymentUrl) {}

  private final org.springframework.jdbc.core.RowMapper<Payment> mapper =
      (r, n) ->
          new Payment(
              r.getLong("id"),
              r.getLong("booking_id"),
              r.getString("provider"),
              r.getString("merchant_reference"),
              r.getString("provider_transaction_id"),
              r.getBigDecimal("amount"),
              r.getString("currency"),
              r.getString("status"),
              r.getString("review_reason"),
              r.getBoolean("applied_to_booking"),
              r.getString("payment_url"));

  public Payment find(long id, boolean lock) {
    return db
        .query("select * from payments where id=?" + (lock ? " for update" : ""), mapper, id)
        .stream()
        .findFirst()
        .orElseThrow(BusinessException::missing);
  }

  public Payment byReference(String provider, String reference) {
    return db
        .query(
            "select * from payments where provider=? and merchant_reference=?",
            mapper,
            provider,
            reference)
        .stream()
        .findFirst()
        .orElseThrow(BusinessException::missing);
  }

  public Optional<Payment> byKey(long booking, String key) {
    return db
        .query("select * from payments where booking_id=? and request_key=?", mapper, booking, key)
        .stream()
        .findFirst();
  }

  public Payment create(long booking, String provider, BigDecimal amount, String key) {
    long id =
        InsertSupport.insert(
            db,
            "insert into"
                + " payments(booking_id,provider,merchant_reference,amount,currency,request_key)"
                + " values(?,?,?,?,'VND',?)",
            booking,
            provider,
            "PAY" + UUID.randomUUID().toString().replace("-", ""),
            amount,
            key);
    return find(id, false);
  }

  public void url(long id, String url) {
    db.update("update payments set payment_url=?,updated_at=current_timestamp where id=?", url, id);
  }

  public void paid(long id, String transaction, boolean applied, String review) {
    db.update(
        "update payments set"
            + " status='PAID',provider_transaction_id=?,paid_at=current_timestamp,applied_to_booking=?,review_reason=?,updated_at=current_timestamp"
            + " where id=?",
        transaction,
        applied,
        review,
        id);
  }

  public void failed(long id) {
    db.update(
        "update payments set status='FAILED',updated_at=current_timestamp where id=? and"
            + " status='PENDING'",
        id);
  }

  public boolean eventExists(String provider, String key) {
    return db.queryForObject(
            "select count(*) from payment_events where provider=? and event_key=?",
            Long.class,
            provider,
            key)
        > 0;
  }

  public void event(long id, String provider, String key) {
    db.update(
        "insert into payment_events(payment_id,provider,event_key) values(?,?,?)",
        id,
        provider,
        key);
  }

  public List<Payment> forBooking(long id) {
    return db.query("select * from payments where booking_id=? order by id", mapper, id);
  }

  public PageResponse<Payment> reviews(PageQuery p) {
    long count =
        db.queryForObject(
            "select count(*) from payments where review_reason is not null", Long.class);
    return PageResponse.of(
        db.query(
            "select * from payments where review_reason is not null order by id limit ? offset ?",
            mapper,
            p.size(),
            p.offset()),
        p.page(),
        p.size(),
        count);
  }

  public record Refund(
      long id,
      long paymentId,
      BigDecimal amount,
      String reason,
      String status,
      String providerRefundId) {}

  private final org.springframework.jdbc.core.RowMapper<Refund> refundMapper =
      (r, n) ->
          new Refund(
              r.getLong("id"),
              r.getLong("payment_id"),
              r.getBigDecimal("amount"),
              r.getString("reason"),
              r.getString("status"),
              r.getString("provider_refund_id"));

  public Optional<Refund> refundByKey(String key) {
    return db.query("select * from refunds where idempotency_key=?", refundMapper, key).stream()
        .findFirst();
  }

  public Optional<Refund> refundForPayment(long id) {
    return db.query("select * from refunds where payment_id=?", refundMapper, id).stream()
        .findFirst();
  }

  public Refund requestFullRefund(Payment payment, String reason, String key) {
    long id =
        InsertSupport.insert(
            db,
            "insert into refunds(payment_id,idempotency_key,amount,reason) values(?,?,?,?)",
            payment.id(),
            key,
            payment.amount(),
            reason);
    return db.queryForObject("select * from refunds where id=?", refundMapper, id);
  }
}
