package com.example.inventory_management.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
/**
* Hibernate {@code ddl-auto: update} often issues {@code ADD COLUMN ... NOT NULL} without a default,
* which PostgreSQL rejects when {@code orders} already has rows. That leaves columns missing and
* causes runtime SQL errors (e.g. on {@code /ui/orders}). This runner repairs the table idempotently.
*/
@Component
@Order(0)
public class PostgresOrdersSchemaRepair implements ApplicationRunner {
private static final Logger log = LoggerFactory.getLogger(PostgresOrdersSchemaRepair.class);
private final DataSource dataSource;
private final JdbcTemplate jdbcTemplate;
public PostgresOrdersSchemaRepair(DataSource dataSource, JdbcTemplate jdbcTemplate) {
  this.dataSource = dataSource;
  this.jdbcTemplate = jdbcTemplate;
}
@Override
public void run(ApplicationArguments args) {
  if (!isPostgreSQL()) {
      return;
  }
  try {
      Integer n = jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'orders'",
              Integer.class);
      if (n == null || n == 0) {
          return;
      }
      jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS subtotal NUMERIC(19,2)");
      jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19,2)");
      jdbcTemplate.update("UPDATE orders SET subtotal = COALESCE(total_price, 0) WHERE subtotal IS NULL");
      jdbcTemplate.update("UPDATE orders SET discount_amount = 0 WHERE discount_amount IS NULL");
      jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN subtotal SET DEFAULT 0");
      jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN discount_amount SET DEFAULT 0");
      jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN subtotal SET NOT NULL");
      jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN discount_amount SET NOT NULL");
      log.debug("orders.subtotal / orders.discount_amount verified for PostgreSQL");
  } catch (Exception e) {
      log.warn("Could not repair orders financial columns (non-fatal if schema is already correct): {}", e.getMessage());
  }
}
private boolean isPostgreSQL() {
  try (Connection c = dataSource.getConnection()) {
      String name = c.getMetaData().getDatabaseProductName();
      return name != null && name.toLowerCase().contains("postgresql");
  } catch (SQLException e) {
      return false;
  }
}
}