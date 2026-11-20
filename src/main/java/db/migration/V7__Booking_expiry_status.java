package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** V2 used an unnamed CHECK; discover its real name on both PostgreSQL and H2. */
public class V7__Booking_expiry_status extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    var connection = context.getConnection();
    var names = new java.util.ArrayList<String>();
    try (var statement =
            connection.prepareStatement(
                "select tc.constraint_name,cc.check_clause from"
                    + " information_schema.table_constraints tc join"
                    + " information_schema.check_constraints cc on"
                    + " tc.constraint_catalog=cc.constraint_catalog and"
                    + " tc.constraint_schema=cc.constraint_schema and"
                    + " tc.constraint_name=cc.constraint_name where lower(tc.table_name)='bookings'"
                    + " and tc.constraint_type='CHECK' and lower(tc.table_schema)='public'");
        var rs = statement.executeQuery()) {
      while (rs.next())
        if (rs.getString(2).toLowerCase(java.util.Locale.ROOT).matches("(?s).*\\bstatus\\b.*")
            && rs.getString(2).contains("'PENDING'")
            && rs.getString(2).contains("'CONFIRMED'")) names.add(rs.getString(1));
    }
    if (names.size() != 1)
      throw new IllegalStateException("Expected one booking status constraint");
    try (var statement = connection.createStatement()) {
      statement.execute(
          "alter table bookings drop constraint \""
              + names.getFirst().replace("\"", "\"\"")
              + "\"");
      statement.execute(
          "alter table bookings add constraint ck_booking_lifecycle_status check(status in"
              + " ('PENDING','CONFIRMED','CANCELLED','COMPLETED','EXPIRED'))");
    }
  }
}
