package com.example.api_booking.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentRepository {
  private final JdbcTemplate db;

  public DocumentRepository(JdbcTemplate db) {
    this.db = db;
  }

  public java.util.Optional<byte[]> find(long booking, String state) {
    return db
        .query(
            "select content from booking_documents where booking_id=? and booking_status=?",
            (r, n) -> r.getBytes(1),
            booking,
            state)
        .stream()
        .findFirst();
  }

  public void save(long booking, String code, String state, byte[] content) {
    if (db.update(
            "update booking_documents set booking_status=?,content=?,updated_at=current_timestamp"
                + " where booking_id=?",
            state,
            content,
            booking)
        == 0)
      db.update(
          "insert into"
              + " booking_documents(booking_id,document_number,storage_key,booking_status,content)"
              + " values(?,?,?,?,?)",
          booking,
          "DOC-" + code,
          "db:booking_documents/" + booking,
          state,
          content);
  }
}
