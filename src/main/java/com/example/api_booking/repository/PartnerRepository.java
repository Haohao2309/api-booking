package com.example.api_booking.repository;

import com.example.api_booking.common.api.*;
import com.example.api_booking.common.exception.BusinessException;
import com.example.api_booking.dto.PartnerRequest;
import com.example.api_booking.entity.Partner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PartnerRepository {
  private final JdbcTemplate db;

  public PartnerRepository(JdbcTemplate db) {
    this.db = db;
  }

  private final org.springframework.jdbc.core.RowMapper<Partner> mapper =
      (r, n) ->
          new Partner(
              r.getLong("id"),
              r.getString("name"),
              r.getString("contact"),
              r.getString("tax_code"),
              r.getString("address"),
              r.getLong("version"));

  public Partner find(long id) {
    return db.query("select * from partners where id=?", mapper, id).stream()
        .findFirst()
        .orElseThrow(BusinessException::missing);
  }

  public PageResponse<Partner> list(PageQuery p, String search) {
    String pattern = "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    String where = " where lower(name) like lower(?) escape '!'";
    long count = db.queryForObject("select count(*) from partners" + where, Long.class, pattern);
    return PageResponse.of(
        db.query(
            "select * from partners" + where + " order by " + p.order() + " limit ? offset ?",
            mapper,
            pattern,
            p.size(),
            p.offset()),
        p.page(),
        p.size(),
        count);
  }

  public long create(PartnerRequest r) {
    return InsertSupport.insert(
        db,
        "insert into partners(name,contact,tax_code,address) values(?,?,?,?)",
        r.name().trim(),
        r.contact().trim(),
        r.taxCode().trim(),
        r.address().trim());
  }

  public void update(long id, long version, PartnerRequest r) {
    if (db.update(
            "update partners set name=?,contact=?,tax_code=?,address=?,version=version+1 where id=?"
                + " and version=?",
            r.name().trim(),
            r.contact().trim(),
            r.taxCode().trim(),
            r.address().trim(),
            id,
            version)
        != 1) throw BusinessException.conflict("Đối tác đã thay đổi, tải lại dữ liệu");
  }

  public void link(long partner, long room) {
    db.update("insert into partner_room(partner_id,room_id) values(?,?)", partner, room);
  }

  public void unlink(long partner, long room) {
    if (db.update("delete from partner_room where partner_id=? and room_id=?", partner, room) == 0)
      throw BusinessException.missing();
  }

  public PageResponse<Partner> byRoom(long room, PageQuery p) {
    long total =
        db.queryForObject("select count(*) from partner_room where room_id=?", Long.class, room);
    return PageResponse.of(
        db.query(
            "select p.* from partners p join partner_room pr on p.id=pr.partner_id where"
                + " pr.room_id=? order by p.id limit ? offset ?",
            mapper,
            room,
            p.size(),
            p.offset()),
        p.page(),
        p.size(),
        total);
  }
}
