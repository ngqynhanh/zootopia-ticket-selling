package com.zoopass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zoopass.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks the Flyway schema against the course requirements and that key
 * business rules are enforced by the database itself.
 * Each test runs in a transaction that is rolled back, so tests do not affect each other.
 */
@IntegrationTest
@Transactional
class SchemaMigrationTest {

    private static final List<String> JUNCTION_TABLES = List.of("combo_zones", "user_promotions");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void schemaMeetsSizeRequirements() {
        List<String> tables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, String.class);
        assertThat(tables).containsAll(JUNCTION_TABLES);
        assertThat(tables.size() - JUNCTION_TABLES.size()).as("non-junction tables").isGreaterThanOrEqualTo(8);

        Integer columns = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
                """, Integer.class);
        assertThat(columns).as("total columns").isGreaterThanOrEqualTo(50);
    }

    @Test
    void everyForeignKeyIsIndexed() {
        // Foreign keys whose columns are not the leading columns of any index.
        List<String> unindexed = jdbc.queryForList("""
                SELECT c.conrelid::regclass || '.' || c.conname
                FROM pg_constraint c
                WHERE c.contype = 'f'
                  AND NOT EXISTS (
                      SELECT 1 FROM pg_index i
                      WHERE i.indrelid = c.conrelid
                        AND (i.indkey::int2[])[0:cardinality(c.conkey) - 1] = c.conkey)
                  -- audit columns (created_by / updated_by) are never used in WHERE/JOIN
                  AND NOT EXISTS (
                      SELECT 1 FROM pg_attribute a
                      WHERE a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
                        AND a.attname IN ('created_by', 'updated_by'))
                """, String.class);
        assertThat(unindexed).isEmpty();
    }

    @Test
    void orderItemMustReferenceExactlyOneProduct() {
        long orderId = insertOrder();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO order_items (order_id, quantity, unit_price, line_total)
                VALUES (?, 1, 60000, 60000)
                """, orderId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_order_items_product");
    }

    @Test
    void groupBookingNeedsAtLeastTenVisitors() {
        long orderId = insertOrder();
        long policyId = insertGroupPolicy();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO group_bookings (order_id, group_policy_id, organization_name, contact_name,
                                            contact_phone, total_visitors)
                VALUES (?, ?, 'Trường THCS Lê Lợi', 'Nguyễn Văn A', '0901234567', 9)
                """, orderId, policyId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_group_bookings_visitors");
    }

    @Test
    void oneGuideServesAtMostFiftyVisitors() {
        long orderId = insertOrder();
        long policyId = insertGroupPolicy();
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO group_bookings (order_id, group_policy_id, organization_name, contact_name,
                                            contact_phone, total_visitors, guide_count, guide_language)
                VALUES (?, ?, 'Trường THCS Lê Lợi', 'Nguyễn Văn A', '0901234567', 51, 1, 'VI')
                """, orderId, policyId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_group_bookings_guides");
    }

    @Test
    void orderTotalMustEqualSubtotalMinusDiscount() {
        long userId = insertUser("total-check@example.com");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO orders (order_code, user_id, order_type, status, visit_date, contact_email,
                                    subtotal, discount_amount, total_amount)
                VALUES ('ZP-TOTAL', ?, 'INDIVIDUAL', 'PENDING_PAYMENT', CURRENT_DATE + 1, 'a@example.com',
                        120000, 10000, 120000)
                """, userId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_orders_amounts");
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        insertUser("visitor@example.com");
        assertThatThrownBy(() -> insertUser("Visitor@Example.com"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_users_email");
    }

    @Test
    void deletingAnOrderCascadesToItsItems() {
        long orderId = insertOrder();
        long ticketTypeId = insertTicketType("GATE_ADULT", 60000);
        jdbc.update("""
                INSERT INTO order_items (order_id, ticket_type_id, quantity, unit_price, line_total)
                VALUES (?, ?, 2, 60000, 120000)
                """, orderId, ticketTypeId);

        jdbc.update("DELETE FROM orders WHERE id = ?", orderId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM order_items WHERE order_id = ?", Integer.class, orderId))
                .isZero();
    }

    @Test
    void ticketTypeInUseCannotBeHardDeleted() {
        long orderId = insertOrder();
        long ticketTypeId = insertTicketType("GATE_CHILD", 40000);
        jdbc.update("""
                INSERT INTO order_items (order_id, ticket_type_id, quantity, unit_price, line_total)
                VALUES (?, ?, 1, 40000, 40000)
                """, orderId, ticketTypeId);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM ticket_types WHERE id = ?", ticketTypeId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatedAtTriggerRefreshesTimestamp() {
        long ticketTypeId = insertTicketType("GATE_ADULT", 60000);
        jdbc.update("UPDATE ticket_types SET updated_at = now() - interval '1 day' WHERE id = ?", ticketTypeId);
        jdbc.update("UPDATE ticket_types SET price = 65000 WHERE id = ?", ticketTypeId);
        Boolean refreshed = jdbc.queryForObject(
                "SELECT updated_at > now() - interval '1 minute' FROM ticket_types WHERE id = ?",
                Boolean.class, ticketTypeId);
        assertThat(refreshed).isTrue();
    }

    private long insertUser(String email) {
        return jdbc.queryForObject("""
                INSERT INTO users (email, password_hash, full_name) VALUES (?, 'hash', 'Test User') RETURNING id
                """, Long.class, email);
    }

    private long insertTicketType(String code, int price) {
        return jdbc.queryForObject("""
                INSERT INTO ticket_types (code, name, category, price) VALUES (?, ?, 'GATE', ?) RETURNING id
                """, Long.class, code, code, price);
    }

    private long insertGroupPolicy() {
        return jdbc.queryForObject("""
                INSERT INTO group_policies (group_category, name, visitor_discount_percent)
                VALUES ('STANDARD', 'Đoàn khác', 10) RETURNING id
                """, Long.class);
    }

    private long insertOrder() {
        long userId = insertUser("buyer-" + System.nanoTime() + "@example.com");
        return jdbc.queryForObject("""
                INSERT INTO orders (order_code, user_id, order_type, status, visit_date, contact_email)
                VALUES (?, ?, 'INDIVIDUAL', 'PENDING_PAYMENT', CURRENT_DATE + 1, 'buyer@example.com')
                RETURNING id
                """, Long.class, "ZP" + (System.nanoTime() % 1_000_000_000L), userId);
    }
}
