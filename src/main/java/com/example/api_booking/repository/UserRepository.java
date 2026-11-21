package com.example.api_booking.repository;

import com.example.api_booking.common.api.*;
import com.example.api_booking.common.exception.BusinessException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  private final JdbcTemplate db;

  public UserRepository(JdbcTemplate db) {
    this.db = db;
  }

  public record UserRow(
      long id,
      String email,
      String fullName,
      String passwordHash,
      boolean enabled,
      long securityVersion,
      String primaryRole,
      String phone) {}

  private final org.springframework.jdbc.core.RowMapper<UserRow> mapper =
      (r, n) ->
          new UserRow(
              r.getLong("id"),
              r.getString("email"),
              r.getString("full_name"),
              r.getString("password_hash"),
              r.getBoolean("enabled"),
              r.getLong("security_version"),
              r.getString("role"),
              r.getString("phone"));

  public UserRow byEmail(String email) {
    return db.query("select * from app_users where lower(email)=lower(?)", mapper, email).stream()
        .findFirst()
        .orElseThrow(
            () -> new BusinessException(401, "AUTHENTICATION_REQUIRED", "Phiên không hợp lệ"));
  }

  /** Lookup that does not reveal through an exception whether the email exists. */
  public Optional<UserRow> findByEmail(String email) {
    return db.query("select * from app_users where lower(email)=lower(?)", mapper, email).stream()
        .findFirst();
  }

  public UserRow find(long id, boolean lock) {
    return db
        .query("select * from app_users where id=?" + (lock ? " for update" : ""), mapper, id)
        .stream()
        .findFirst()
        .orElseThrow(BusinessException::missing);
  }

  public List<String> roles(long id) {
    List<String> roles =
        db.queryForList(
            "select r.name from roles r join user_roles ur on ur.role_id=r.id where ur.user_id=?"
                + " order by r.id",
            String.class,
            id);
    return roles.isEmpty() ? List.of(find(id, false).primaryRole()) : roles;
  }

  public PageResponse<UserRow> list(PageQuery p, String search) {
    String pattern = "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    String where = " where lower(coalesce(full_name,'')) like lower(?) escape '!'";
    long total = db.queryForObject("select count(*) from app_users" + where, Long.class, pattern);
    return PageResponse.of(
        db.query(
            "select * from app_users" + where + " order by " + p.order() + " limit ? offset ?",
            mapper,
            pattern,
            p.size(),
            p.offset()),
        p.page(),
        p.size(),
        total);
  }

  public void profile(long id, String name) {
    db.update("update app_users set full_name=? where id=?", name, id);
  }

  public void updateProfile(long id, String fullName, String phone) {
    db.update(
        "update app_users set full_name=?,phone=?,updated_at=current_timestamp where id=?",
        fullName,
        phone,
        id);
  }

  public void password(long id, String hash) {
    db.update(
        "update app_users set password_hash=?,security_version=security_version+1 where id=?",
        hash,
        id);
  }

  public void enabled(long id, boolean enabled) {
    db.update(
        "update app_users set enabled=?,security_version=security_version+1 where id=?",
        enabled,
        id);
  }

  public void login(long id) {
    db.update("update app_users set last_login=current_timestamp where id=?", id);
  }

  public void assign(long id, List<Long> roles) {
    db.update("delete from user_roles where user_id=?", id);
    for (long role : roles)
      db.update("insert into user_roles(user_id,role_id) values(?,?)", id, role);
    String primary =
        db.queryForObject(
            "select name from roles where id=?",
            String.class,
            roles.stream().min(Long::compare).orElseThrow());
    db.update("update app_users set role=? where id=?", primary, id);
  }

  public Map<Long, String> availableRoles() {
    Map<Long, String> result = new LinkedHashMap<>();
    db.query(
        "select id,name from roles order by id",
        (org.springframework.jdbc.core.RowCallbackHandler)
            r -> result.put(r.getLong(1), r.getString(2)));
    return result;
  }

  public PageResponse<Map<String, Object>> rolePage(PageQuery p) {
    long total = db.queryForObject("select count(*) from roles", Long.class);
    var items =
        db.query(
            "select id,name from roles order by id limit ? offset ?",
            (r, n) -> Map.<String, Object>of("id", r.getLong(1), "name", r.getString(2)),
            p.size(),
            p.offset());
    return PageResponse.of(items, p.page(), p.size(), total);
  }

  public void lockRoleAdministration() {
    db.queryForObject("select id from roles where id=1 for update", Long.class);
  }

  public void audit(long actor, String action, long target) {
    db.update(
        "insert into audit_events(actor_id,action,target_id) values(?,?,?)", actor, action, target);
  }
}
