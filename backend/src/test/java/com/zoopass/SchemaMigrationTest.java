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
 */
@IntegrationTest
@Transactional
class SchemaMigrationTest {

    private static final List<String> JUNCTION_TABLES = List.of("combo_zones", "user_promotions", "user_saved_activities");

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
                  -- audit columns (created_by / updated_by) are never queried by
                  AND NOT EXISTS (
                      SELECT 1 FROM pg_attribute a
                      WHERE a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
                        AND a.attname IN ('created_by', 'updated_by'))
                """, String.class);
        assertThat(unindexed).isEmpty();
    }

    @Test
    void referenceDataIsLoaded() {
        assertThat(count("ticket_types")).isEqualTo(13);
        assertThat(count("group_policies")).isEqualTo(4);
        assertThat(count("combos")).isEqualTo(11);
        assertThat(jdbc.queryForObject(
                "SELECT setting_value FROM system_settings WHERE setting_key = 'group.min_size'", String.class))
                .isEqualTo("10");
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
        long policyId = jdbc.queryForObject(
                "SELECT id FROM group_policies WHERE group_category = 'STANDARD'", Long.class);
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
        long policyId = jdbc.queryForObject(
                "SELECT id FROM group_policies WHERE group_category = 'STANDARD'", Long.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO group_bookings (order_id, group_policy_id, organization_name, contact_name,
                                            contact_phone, total_visitors, guide_count, guide_language)
                VALUES (?, ?, 'Trường THCS Lê Lợi', 'Nguyễn Văn A', '0901234567', 51, 1, 'VI')
                """, orderId, policyId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_group_bookings_guides");
    }

    @Test
    void checkInAdmitsAtMostFourPeoplePerScan() {
        long orderId = insertOrder();
        long ticketId = jdbc.queryForObject("""
                INSERT INTO tickets (order_id, qr_token, total_entries, valid_from, expires_at)
                VALUES (?, 'qr-test-token', 6, CURRENT_DATE, now() + interval '30 days') RETURNING id
                """, Long.class, orderId);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO check_ins (ticket_id, people_count, gate) VALUES (?, 5, 'A')", ticketId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_check_ins_people");
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
        long ticketTypeId = jdbc.queryForObject("SELECT id FROM ticket_types WHERE code = 'GATE_ADULT'", Long.class);
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
        long ticketTypeId = jdbc.queryForObject("SELECT id FROM ticket_types WHERE code = 'GATE_CHILD'", Long.class);
        jdbc.update("""
                INSERT INTO order_items (order_id, ticket_type_id, quantity, unit_price, line_total)
                VALUES (?, ?, 1, 40000, 40000)
                """, orderId, ticketTypeId);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM ticket_types WHERE id = ?", ticketTypeId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatedAtTriggerRefreshesTimestamp() {
        jdbc.update("UPDATE ticket_types SET updated_at = now() - interval '1 day' WHERE code = 'GATE_ADULT'");
        jdbc.update("UPDATE ticket_types SET price = 65000 WHERE code = 'GATE_ADULT'");
        Boolean refreshed = jdbc.queryForObject(
                "SELECT updated_at > now() - interval '1 minute' FROM ticket_types WHERE code = 'GATE_ADULT'",
                Boolean.class);
        assertThat(refreshed).isTrue();
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private long insertUser(String email) {
        return jdbc.queryForObject("""
                INSERT INTO users (email, password_hash, full_name) VALUES (?, 'hash', 'Test User') RETURNING id
                """, Long.class, email);
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
