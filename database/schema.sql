-- =====================================================================
-- ZooPass - initial schema (PostgreSQL 14+)
--
-- Conventions
--   * Surrogate keys: BIGINT GENERATED ALWAYS AS IDENTITY.
--   * Audit columns on every business table: created_at, updated_at,
--     created_by, updated_by (FK -> users, SET NULL when the user is removed).
--   * Soft delete: is_active + deleted_at on catalog / master data.
--   * Money in VND: NUMERIC(12,2), never negative.
--   * Status / enum columns are VARCHAR + CHECK (readable, easy to extend).
--   * Every foreign key has an index.
-- =====================================================================

-- Keeps updated_at correct even for rows changed by raw SQL (seed scripts, admin fixes).
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- 1. users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(20),
    date_of_birth   DATE,
    avatar_url      VARCHAR(500),
    role            VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by      BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN')),
    -- Date-of-birth "not in the future" is validated in the application:
    -- CHECK constraints must not depend on CURRENT_DATE (not immutable).
    CONSTRAINT ck_users_email_format CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);
-- Emails are unique regardless of case.
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
CREATE INDEX ix_users_role_active ON users (role, is_active);

-- One-time tokens for email verification and password reset.
CREATE TABLE user_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL UNIQUE,
    purpose     VARCHAR(30)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_tokens_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'REFRESH'))
);
CREATE INDEX ix_user_tokens_user ON user_tokens (user_id, purpose);

-- ---------------------------------------------------------------------
-- 2. Catalog: ticket types and experience combos
-- ---------------------------------------------------------------------
CREATE TABLE ticket_types (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(40)   NOT NULL UNIQUE,
    name            VARCHAR(120)  NOT NULL,
    category        VARCHAR(20)   NOT NULL,
    description     TEXT,
    price           NUMERIC(12,2) NOT NULL,
    min_height_cm   SMALLINT,
    max_height_cm   SMALLINT,
    min_quantity    SMALLINT      NOT NULL DEFAULT 1,
    entries_per_unit SMALLINT     NOT NULL DEFAULT 1,
    validity_days   SMALLINT      NOT NULL DEFAULT 30,
    display_order   SMALLINT      NOT NULL DEFAULT 0,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by      BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_ticket_types_category CHECK (category IN ('GATE', 'TRAIN', 'TOUR', 'PASS', 'SERVICE')),
    CONSTRAINT ck_ticket_types_price CHECK (price >= 0),
    CONSTRAINT ck_ticket_types_height CHECK (
        (min_height_cm IS NULL OR min_height_cm >= 0)
        AND (max_height_cm IS NULL OR min_height_cm IS NULL OR max_height_cm > min_height_cm)),
    CONSTRAINT ck_ticket_types_min_qty CHECK (min_quantity >= 1),
    CONSTRAINT ck_ticket_types_entries CHECK (entries_per_unit >= 1),
    CONSTRAINT ck_ticket_types_validity CHECK (validity_days BETWEEN 1 AND 366)
);
CREATE INDEX ix_ticket_types_category_active ON ticket_types (category, is_active);

CREATE TABLE combos (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code              VARCHAR(40)   NOT NULL UNIQUE,
    name              VARCHAR(150)  NOT NULL,
    description       TEXT,
    price             NUMERIC(12,2) NOT NULL,
    duration_minutes  SMALLINT      NOT NULL,
    min_participants  SMALLINT      NOT NULL DEFAULT 1,
    max_participants  SMALLINT,
    min_age           SMALLINT,
    image_url         VARCHAR(500),
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by        BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_combos_price CHECK (price >= 0),
    CONSTRAINT ck_combos_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_combos_participants CHECK (
        min_participants >= 1 AND (max_participants IS NULL OR max_participants >= min_participants)),
    CONSTRAINT ck_combos_min_age CHECK (min_age IS NULL OR min_age BETWEEN 0 AND 100)
);
CREATE INDEX ix_combos_active_price ON combos (is_active, price);

-- ---------------------------------------------------------------------
-- 3. Visit planning: zones, animals, activity schedule, FAQ
-- ---------------------------------------------------------------------
CREATE TABLE zones (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code          VARCHAR(40)  NOT NULL UNIQUE,
    name          VARCHAR(120) NOT NULL,
    zone_type     VARCHAR(20)  NOT NULL DEFAULT 'ANIMAL',
    description   TEXT,
    highlights    VARCHAR(500),
    svg_path_id   VARCHAR(60),
    map_x         NUMERIC(6,2),
    map_y         NUMERIC(6,2),
    image_url     VARCHAR(500),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_zones_type CHECK (zone_type IN ('ANIMAL', 'PLANT', 'HERITAGE', 'PLAYGROUND', 'FOOD', 'FACILITY', 'GATE')),
    -- Map coordinates are percentages of the map image so the map scales on any screen.
    CONSTRAINT ck_zones_map_x CHECK (map_x IS NULL OR map_x BETWEEN 0 AND 100),
    CONSTRAINT ck_zones_map_y CHECK (map_y IS NULL OR map_y BETWEEN 0 AND 100)
);
CREATE INDEX ix_zones_type_active ON zones (zone_type, is_active);

CREATE TABLE animals (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    zone_id              BIGINT       NOT NULL REFERENCES zones (id) ON DELETE RESTRICT,
    common_name          VARCHAR(120) NOT NULL,
    scientific_name      VARCHAR(150),
    conservation_status  VARCHAR(5),
    description          TEXT,
    image_url            VARCHAR(500),
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by           BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by           BIGINT REFERENCES users (id) ON DELETE SET NULL,
    -- IUCN Red List categories
    CONSTRAINT ck_animals_iucn CHECK (conservation_status IS NULL
        OR conservation_status IN ('LC', 'NT', 'VU', 'EN', 'CR', 'EW', 'EX', 'DD', 'NE'))
);
CREATE INDEX ix_animals_zone ON animals (zone_id);

CREATE TABLE activities (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    zone_id        BIGINT       NOT NULL REFERENCES zones (id) ON DELETE RESTRICT,
    title          VARCHAR(150) NOT NULL,
    activity_type  VARCHAR(20)  NOT NULL,
    description    TEXT,
    start_time     TIME         NOT NULL,
    end_time       TIME         NOT NULL,
    -- ISO days: 1 = Monday ... 7 = Sunday
    days_of_week   SMALLINT[]   NOT NULL DEFAULT '{1,2,3,4,5,6,7}',
    -- NULL = every week; otherwise weeks of the month (e.g. {2,4} for Idol Zoobiz)
    weeks_of_month SMALLINT[],
    includes_holidays BOOLEAN   NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_activities_type CHECK (activity_type IN ('FEEDING', 'SHOW', 'INTERACTION', 'EVENT')),
    CONSTRAINT ck_activities_time CHECK (end_time > start_time),
    CONSTRAINT ck_activities_days CHECK (
        cardinality(days_of_week) BETWEEN 1 AND 7 AND days_of_week <@ ARRAY[1,2,3,4,5,6,7]::SMALLINT[]),
    CONSTRAINT ck_activities_weeks CHECK (
        weeks_of_month IS NULL OR weeks_of_month <@ ARRAY[1,2,3,4,5]::SMALLINT[])
);
CREATE INDEX ix_activities_zone_start ON activities (zone_id, start_time);

CREATE TABLE faqs (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category       VARCHAR(30)  NOT NULL,
    question       VARCHAR(300) NOT NULL,
    answer         TEXT         NOT NULL,
    display_order  SMALLINT     NOT NULL DEFAULT 0,
    is_published   BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Full-text search over question + answer ('simple' config: Vietnamese has no built-in stemmer).
    search_vector  TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple', coalesce(question, '') || ' ' || coalesce(answer, ''))) STORED,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_faqs_category CHECK (category IN ('TICKET', 'SERVICE', 'GROUP', 'RULE', 'DIRECTION', 'OTHER'))
);
CREATE INDEX ix_faqs_category_order ON faqs (category, display_order);
CREATE INDEX ix_faqs_search ON faqs USING GIN (search_vector);

-- ---------------------------------------------------------------------
-- 4. Promotions and group policies
-- ---------------------------------------------------------------------
CREATE TABLE promotions (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code              VARCHAR(30)   NOT NULL UNIQUE,
    description       VARCHAR(300),
    discount_type     VARCHAR(10)   NOT NULL,
    discount_value    NUMERIC(12,2) NOT NULL,
    max_discount      NUMERIC(12,2),
    min_order_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
    max_uses          INTEGER,
    used_count        INTEGER       NOT NULL DEFAULT 0,
    valid_from        TIMESTAMPTZ   NOT NULL,
    valid_to          TIMESTAMPTZ   NOT NULL,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by        BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_promotions_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT ck_promotions_value CHECK (
        discount_value > 0 AND (discount_type <> 'PERCENT' OR discount_value <= 100)),
    CONSTRAINT ck_promotions_amounts CHECK (min_order_amount >= 0 AND (max_discount IS NULL OR max_discount > 0)),
    CONSTRAINT ck_promotions_uses CHECK (used_count >= 0 AND (max_uses IS NULL OR used_count <= max_uses)),
    CONSTRAINT ck_promotions_period CHECK (valid_to > valid_from)
);
CREATE INDEX ix_promotions_validity ON promotions (is_active, valid_from, valid_to);

-- Discount rules per group category (Saigon Zoo policy for groups of 10+).
CREATE TABLE group_policies (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_category            VARCHAR(30)  NOT NULL UNIQUE,
    name                      VARCHAR(150) NOT NULL,
    description               TEXT,
    min_group_size            SMALLINT     NOT NULL DEFAULT 10,
    visitor_discount_percent  NUMERIC(5,2) NOT NULL DEFAULT 0,
    beneficiary_discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    staff_discount_percent    NUMERIC(5,2) NOT NULL DEFAULT 0,
    train_discount_percent    NUMERIC(5,2) NOT NULL DEFAULT 0,
    free_adult_per_students   SMALLINT,
    requires_verification     BOOLEAN      NOT NULL DEFAULT FALSE,
    deposit_percent           NUMERIC(5,2) NOT NULL DEFAULT 30,
    is_active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by                BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by                BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_group_policies_category CHECK (
        group_category IN ('PRESCHOOL', 'SOCIAL_WELFARE', 'REVOLUTIONARY_MERIT', 'STANDARD')),
    CONSTRAINT ck_group_policies_size CHECK (min_group_size >= 1),
    CONSTRAINT ck_group_policies_percents CHECK (
        visitor_discount_percent BETWEEN 0 AND 100
        AND beneficiary_discount_percent BETWEEN 0 AND 100
        AND staff_discount_percent BETWEEN 0 AND 100
        AND train_discount_percent BETWEEN 0 AND 100
        AND deposit_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_group_policies_free_adult CHECK (free_adult_per_students IS NULL OR free_adult_per_students > 0)
);

-- ---------------------------------------------------------------------
-- 5. Orders
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_code       VARCHAR(20)   NOT NULL UNIQUE,
    user_id          BIGINT        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    order_type       VARCHAR(15)   NOT NULL,
    status           VARCHAR(25)   NOT NULL,
    visit_date       DATE          NOT NULL,
    subtotal         NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    paid_amount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    promotion_id     BIGINT REFERENCES promotions (id) ON DELETE SET NULL,
    contact_email    VARCHAR(255)  NOT NULL,
    contact_phone    VARCHAR(20),
    note             VARCHAR(500),
    policy_accepted_at TIMESTAMPTZ,
    payment_deadline TIMESTAMPTZ,
    cancelled_at     TIMESTAMPTZ,
    cancel_reason    VARCHAR(300),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by       BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_orders_type CHECK (order_type IN ('INDIVIDUAL', 'GROUP')),
    CONSTRAINT ck_orders_status CHECK (status IN (
        'DRAFT', 'PENDING_PAYMENT', 'SUBMITTED', 'PENDING_VERIFICATION', 'APPROVED', 'REJECTED',
        'AWAITING_DEPOSIT', 'CONFIRMED', 'PAID', 'TICKET_ISSUED', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_orders_amounts CHECK (
        subtotal >= 0 AND discount_amount >= 0 AND discount_amount <= subtotal
        AND total_amount = subtotal - discount_amount
        AND paid_amount >= 0 AND paid_amount <= total_amount),
    CONSTRAINT ck_orders_cancel CHECK (status <> 'CANCELLED' OR cancelled_at IS NOT NULL)
);
CREATE INDEX ix_orders_user_status ON orders (user_id, status);
CREATE INDEX ix_orders_visit_date ON orders (visit_date);
CREATE INDEX ix_orders_status_created ON orders (status, created_at);
CREATE INDEX ix_orders_promotion ON orders (promotion_id);
-- Speeds up the "cancel unpaid orders" scheduled job.
CREATE INDEX ix_orders_payment_deadline ON orders (payment_deadline) WHERE status = 'PENDING_PAYMENT';

CREATE TABLE order_items (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id          BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    ticket_type_id    BIGINT REFERENCES ticket_types (id) ON DELETE RESTRICT,
    combo_id          BIGINT REFERENCES combos (id) ON DELETE RESTRICT,
    visitor_group     VARCHAR(20)   NOT NULL DEFAULT 'GENERAL',
    quantity          INTEGER       NOT NULL,
    -- Price snapshot at purchase time (justified denormalization: catalog prices change later).
    unit_price        NUMERIC(12,2) NOT NULL,
    discount_percent  NUMERIC(5,2)  NOT NULL DEFAULT 0,
    line_total        NUMERIC(12,2) NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_order_items_product CHECK (num_nonnulls(ticket_type_id, combo_id) = 1),
    CONSTRAINT ck_order_items_visitor_group CHECK (visitor_group IN (
        'GENERAL', 'ADULT', 'CHILD', 'INFANT', 'STUDENT', 'TEACHER', 'BENEFICIARY', 'SUPPORT_STAFF')),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_amounts CHECK (
        unit_price >= 0 AND discount_percent BETWEEN 0 AND 100 AND line_total >= 0)
);
CREATE INDEX ix_order_items_order ON order_items (order_id);
CREATE INDEX ix_order_items_ticket_type ON order_items (ticket_type_id);
CREATE INDEX ix_order_items_combo ON order_items (combo_id);

-- Extra data for GROUP orders (1-1 with orders).
CREATE TABLE group_bookings (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id             BIGINT        NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    group_policy_id      BIGINT        NOT NULL REFERENCES group_policies (id) ON DELETE RESTRICT,
    organization_name    VARCHAR(200)  NOT NULL,
    organization_address VARCHAR(300),
    contact_name         VARCHAR(120)  NOT NULL,
    contact_phone        VARCHAR(20)   NOT NULL,
    total_visitors       INTEGER       NOT NULL,
    guide_count          SMALLINT      NOT NULL DEFAULT 0,
    guide_language       VARCHAR(5),
    uses_train           BOOLEAN       NOT NULL DEFAULT FALSE,
    deposit_amount       NUMERIC(12,2) NOT NULL DEFAULT 0,
    verification_status  VARCHAR(20)   NOT NULL DEFAULT 'NOT_REQUIRED',
    reviewed_by          BIGINT REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at          TIMESTAMPTZ,
    reject_reason        VARCHAR(500),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by           BIGINT REFERENCES users (id) ON DELETE SET NULL,
    updated_by           BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_group_bookings_visitors CHECK (total_visitors >= 10),
    -- One guide (thuyet minh vien) serves at most 50 visitors.
    CONSTRAINT ck_group_bookings_guides CHECK (
        guide_count = 0 OR guide_count * 50 >= total_visitors),
    CONSTRAINT ck_group_bookings_language CHECK (
        (guide_count = 0 AND guide_language IS NULL)
        OR (guide_count > 0 AND guide_language IN ('VI', 'EN'))),
    CONSTRAINT ck_group_bookings_deposit CHECK (deposit_amount >= 0),
    CONSTRAINT ck_group_bookings_verification CHECK (
        verification_status IN ('NOT_REQUIRED', 'PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_group_bookings_review CHECK (
        verification_status NOT IN ('APPROVED', 'REJECTED') OR reviewed_at IS NOT NULL),
    CONSTRAINT ck_group_bookings_reject CHECK (
        verification_status <> 'REJECTED' OR reject_reason IS NOT NULL)
);
CREATE INDEX ix_group_bookings_policy ON group_bookings (group_policy_id);
CREATE INDEX ix_group_bookings_reviewer ON group_bookings (reviewed_by);
CREATE INDEX ix_group_bookings_verification ON group_bookings (verification_status);

-- Uploaded proof documents (group eligibility papers, monthly-pass portrait photo).
CREATE TABLE verifications (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uploaded_by       BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    group_booking_id  BIGINT REFERENCES group_bookings (id) ON DELETE CASCADE,
    order_id          BIGINT REFERENCES orders (id) ON DELETE CASCADE,
    document_type     VARCHAR(30)   NOT NULL,
    file_url          VARCHAR(500)  NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    mime_type         VARCHAR(50)   NOT NULL,
    file_size_bytes   INTEGER       NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    reviewer_id       BIGINT REFERENCES users (id) ON DELETE SET NULL,
    reviewer_note     VARCHAR(500),
    reviewed_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_verifications_target CHECK (num_nonnulls(group_booking_id, order_id) >= 1),
    CONSTRAINT ck_verifications_doc_type CHECK (document_type IN (
        'CONFIRMATION_LETTER', 'BENEFICIARY_LIST', 'MERIT_CERTIFICATE', 'ID_CARD', 'PORTRAIT_PHOTO', 'OTHER')),
    CONSTRAINT ck_verifications_mime CHECK (mime_type IN ('image/jpeg', 'image/png', 'image/webp', 'application/pdf')),
    -- 5 MB upload limit, enforced again at the database level.
    CONSTRAINT ck_verifications_size CHECK (file_size_bytes BETWEEN 1 AND 5242880),
    CONSTRAINT ck_verifications_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_verifications_review CHECK (status = 'PENDING' OR reviewed_at IS NOT NULL)
);
CREATE INDEX ix_verifications_uploader ON verifications (uploaded_by);
CREATE INDEX ix_verifications_group_booking ON verifications (group_booking_id);
CREATE INDEX ix_verifications_order ON verifications (order_id);
CREATE INDEX ix_verifications_reviewer ON verifications (reviewer_id);
CREATE INDEX ix_verifications_status_created ON verifications (status, created_at);

CREATE TABLE payments (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id          BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    amount            NUMERIC(12,2) NOT NULL,
    method            VARCHAR(15)   NOT NULL,
    purpose           VARCHAR(15)   NOT NULL,
    status            VARCHAR(15)   NOT NULL DEFAULT 'PENDING',
    transaction_ref   VARCHAR(64)   NOT NULL UNIQUE,
    gateway_txn_no    VARCHAR(64),
    gateway_response  JSONB,
    paid_at           TIMESTAMPTZ,
    received_by       BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_method CHECK (method IN ('VNPAY', 'STRIPE', 'CASH')),
    CONSTRAINT ck_payments_purpose CHECK (purpose IN ('FULL', 'DEPOSIT', 'REMAINING')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_payments_paid CHECK (status <> 'SUCCESS' OR paid_at IS NOT NULL),
    -- Cash at the counter must record which staff member received it.
    CONSTRAINT ck_payments_cash CHECK (method <> 'CASH' OR received_by IS NOT NULL)
);
CREATE INDEX ix_payments_order ON payments (order_id);
CREATE INDEX ix_payments_received_by ON payments (received_by);
CREATE INDEX ix_payments_status_paid ON payments (status, paid_at);

-- ---------------------------------------------------------------------
-- 6. E-tickets and gate check-ins
-- ---------------------------------------------------------------------
CREATE TABLE tickets (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id       BIGINT       NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    qr_token       VARCHAR(64)  NOT NULL UNIQUE,
    holder_name    VARCHAR(120),
    total_entries  INTEGER      NOT NULL,
    used_entries   INTEGER      NOT NULL DEFAULT 0,
    status         VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE',
    valid_from     DATE         NOT NULL,
    expires_at     TIMESTAMPTZ  NOT NULL,
    issued_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_tickets_entries CHECK (total_entries > 0 AND used_entries BETWEEN 0 AND total_entries),
    CONSTRAINT ck_tickets_status CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT ck_tickets_used CHECK (status <> 'USED' OR used_entries = total_entries),
    CONSTRAINT ck_tickets_expiry CHECK (expires_at > issued_at)
);
CREATE INDEX ix_tickets_order ON tickets (order_id);
-- Speeds up the nightly "expire tickets" job.
CREATE INDEX ix_tickets_expires_active ON tickets (expires_at) WHERE status = 'ACTIVE';

CREATE TABLE check_ins (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id     BIGINT      NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    staff_id      BIGINT REFERENCES users (id) ON DELETE SET NULL,
    people_count  SMALLINT    NOT NULL,
    gate          CHAR(1)     NOT NULL,
    scanned_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- One scan admits at most 4 people (Saigon Zoo e-ticket rule).
    CONSTRAINT ck_check_ins_people CHECK (people_count BETWEEN 1 AND 4),
    CONSTRAINT ck_check_ins_gate CHECK (gate IN ('A', 'B', 'C', 'D'))
);
CREATE INDEX ix_check_ins_ticket ON check_ins (ticket_id);
CREATE INDEX ix_check_ins_staff ON check_ins (staff_id);
CREATE INDEX ix_check_ins_scanned ON check_ins (scanned_at);

-- ---------------------------------------------------------------------
-- 7. Junction tables (many-to-many)
-- ---------------------------------------------------------------------
-- Which zones a combo visits, in order.
CREATE TABLE combo_zones (
    combo_id     BIGINT   NOT NULL REFERENCES combos (id) ON DELETE CASCADE,
    zone_id      BIGINT   NOT NULL REFERENCES zones (id) ON DELETE CASCADE,
    visit_order  SMALLINT NOT NULL,
    PRIMARY KEY (combo_id, zone_id),
    CONSTRAINT uq_combo_zones_order UNIQUE (combo_id, visit_order),
    CONSTRAINT ck_combo_zones_order CHECK (visit_order >= 1)
);
CREATE INDEX ix_combo_zones_zone ON combo_zones (zone_id);

-- Which user redeemed which promotion (one redemption per user per promotion).
CREATE TABLE user_promotions (
    user_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    promotion_id  BIGINT      NOT NULL REFERENCES promotions (id) ON DELETE CASCADE,
    order_id      BIGINT REFERENCES orders (id) ON DELETE SET NULL,
    used_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, promotion_id)
);
CREATE INDEX ix_user_promotions_promotion ON user_promotions (promotion_id);
CREATE INDEX ix_user_promotions_order ON user_promotions (order_id);

-- "My visit plan": activities a user saved for a given day.
CREATE TABLE user_saved_activities (
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    activity_id  BIGINT      NOT NULL REFERENCES activities (id) ON DELETE CASCADE,
    plan_date    DATE        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, activity_id, plan_date)
);
CREATE INDEX ix_user_saved_activities_activity ON user_saved_activities (activity_id);

-- ---------------------------------------------------------------------
-- 8. Cross-cutting: notifications, audit log, system settings
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL,
    title       VARCHAR(150) NOT NULL,
    content     VARCHAR(1000),
    link        VARCHAR(300),
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_notifications_type CHECK (type IN (
        'ORDER', 'PAYMENT', 'VERIFICATION', 'TICKET', 'REMINDER', 'SYSTEM')),
    CONSTRAINT ck_notifications_read CHECK (NOT is_read OR read_at IS NOT NULL)
);
CREATE INDEX ix_notifications_user_unread ON notifications (user_id, is_read, created_at DESC);

CREATE TABLE audit_logs (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    action       VARCHAR(40)  NOT NULL,
    entity_type  VARCHAR(40)  NOT NULL,
    entity_id    BIGINT,
    old_value    JSONB,
    new_value    JSONB,
    ip_address   VARCHAR(45),
    request_id   VARCHAR(36),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX ix_audit_logs_actor_created ON audit_logs (actor_id, created_at DESC);
CREATE INDEX ix_audit_logs_created ON audit_logs (created_at DESC);

CREATE TABLE system_settings (
    setting_key    VARCHAR(60)  PRIMARY KEY,
    setting_value  VARCHAR(500) NOT NULL,
    value_type     VARCHAR(10)  NOT NULL DEFAULT 'STRING',
    description    VARCHAR(300),
    is_public      BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_system_settings_type CHECK (value_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'TIME'))
);

-- ---------------------------------------------------------------------
-- updated_at triggers
-- ---------------------------------------------------------------------
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'users', 'ticket_types', 'combos', 'zones', 'animals', 'activities', 'faqs',
        'promotions', 'group_policies', 'orders', 'order_items', 'group_bookings',
        'verifications', 'payments', 'tickets', 'system_settings']
    LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION set_updated_at()',
            t, t);
    END LOOP;
END;
$$;
-- =====================================================================
-- Reference data the application needs to run (not demo data).
-- Prices and rules follow the public Saigon Zoo pages listed in
-- extra-resources/online-resource.txt (read 12/11/2026).
-- Combo prices are NOT published by the source: they are team assumptions.
-- Demo data (100+ users, orders, ...) goes in database/seed-data.sql.
-- =====================================================================

-- ---------------------------------------------------------------------
-- System settings
-- ---------------------------------------------------------------------
INSERT INTO system_settings (setting_key, setting_value, value_type, description, is_public) VALUES
    ('zoo.opening_time',               '07:00', 'TIME',    'Giờ mở cửa', TRUE),
    ('zoo.closing_time',               '18:30', 'TIME',    'Giờ đóng cửa', TRUE),
    ('ticket.sales_cutoff_time',       '17:30', 'TIME',    'Giờ ngưng bán vé cho ngày hiện tại', TRUE),
    ('ticket.validity_days',           '30',    'INTEGER', 'E-ticket có giá trị 30 ngày kể từ ngày xuất', TRUE),
    ('ticket.max_people_per_scan',     '4',     'INTEGER', 'Số người tối đa cho mỗi lượt quét QR', TRUE),
    ('booking.max_days_ahead',         '30',    'INTEGER', 'Chỉ đặt vé tối đa 30 ngày tới', TRUE),
    ('order.payment_timeout_minutes',  '15',    'INTEGER', 'Hủy đơn chưa thanh toán sau N phút', FALSE),
    ('group.min_size',                 '10',    'INTEGER', 'Số khách tối thiểu để tính là đoàn', TRUE),
    ('group.deposit_percent',          '30',    'DECIMAL', 'Tỷ lệ đặt cọc vé đoàn (%)', TRUE),
    ('group.max_visitors_per_guide',   '50',    'INTEGER', 'Số khách tối đa cho 1 thuyết minh viên', TRUE),
    ('upload.max_file_size_mb',        '5',     'INTEGER', 'Dung lượng tối đa file upload (MB)', TRUE);

-- ---------------------------------------------------------------------
-- Ticket types
-- ---------------------------------------------------------------------
INSERT INTO ticket_types
    (code, name, category, description, price, min_height_cm, max_height_cm, min_quantity, entries_per_unit, validity_days, display_order)
VALUES
    ('GATE_ADULT',  'Vé cổng người lớn', 'GATE',
     'Khách có chiều cao từ 1,3 m trở lên.', 60000, 130, NULL, 1, 1, 30, 1),
    ('GATE_CHILD',  'Vé cổng trẻ em', 'GATE',
     'Trẻ em cao từ 1 m đến dưới 1,3 m.', 40000, 100, 130, 1, 1, 30, 2),
    ('GATE_INFANT', 'Trẻ em dưới 1 m', 'GATE',
     'Miễn phí, phải đi cùng phụ huynh hoặc người giám hộ.', 0, NULL, 100, 1, 1, 30, 3),
    ('TRAIN_ADULT', 'Xe lửa / xe điện - người lớn', 'TRAIN',
     'Một vòng tham quan, khách từ 1,3 m.', 30000, 130, NULL, 1, 1, 30, 10),
    ('TRAIN_CHILD', 'Xe lửa / xe điện - trẻ em', 'TRAIN',
     'Một vòng tham quan, khách từ 0,8 m đến dưới 1,3 m.', 20000, 80, 130, 1, 1, 30, 11),
    ('TRAIN_INFANT', 'Xe lửa / xe điện - dưới 0,8 m', 'TRAIN',
     'Miễn phí khi đi cùng gia đình.', 0, NULL, 80, 1, 1, 30, 12),
    ('TOUR_PRIVATE', 'Tour xe điện riêng', 'TOUR',
     'Tour xe điện riêng cho nhóm, tối thiểu 6 vé mỗi tour.', 150000, NULL, NULL, 6, 1, 30, 20),
    ('TOUR_VIP',    'Tour xe điện VIP', 'TOUR',
     'Cho thú ăn tại các điểm dừng (hươu cao cổ, gấu mèo, linh dương, voi, dê cừu, hươu nai), có quà lưu niệm. Tối thiểu 6 vé.',
     200000, NULL, NULL, 6, 1, 30, 21),
    ('PASS_BOOK_ADULT', 'Vé tập 10 vé - từ 1,3 m', 'PASS',
     '1 tập = 10 vé vào cổng, giảm 10%, dùng chung được.', 540000, 130, NULL, 1, 10, 30, 30),
    ('PASS_BOOK_CHILD', 'Vé tập 10 vé - 1 m đến dưới 1,3 m', 'PASS',
     '1 tập = 10 vé vào cổng, giảm 10%, dùng chung được.', 360000, 100, 130, 1, 10, 30, 31),
    ('PASS_MONTHLY', 'Vé tháng', 'PASS',
     'Vào cổng mỗi ngày trong 30 ngày kể từ ngày đăng ký. Thẻ cá nhân có ảnh 3x4, không chuyển nhượng.',
     500000, NULL, NULL, 1, 30, 30, 32),
    ('GUIDE_VI',    'Thuyết minh viên - tiếng Việt', 'SERVICE',
     '1h30, tối đa 50 khách mỗi thuyết minh viên.', 300000, NULL, NULL, 1, 1, 30, 40),
    ('GUIDE_EN',    'Thuyết minh viên - tiếng Anh', 'SERVICE',
     '1h30, tối đa 50 khách mỗi thuyết minh viên.', 500000, NULL, NULL, 1, 1, 30, 41);

-- ---------------------------------------------------------------------
-- Group policies (groups of 10+ visitors)
-- ---------------------------------------------------------------------
INSERT INTO group_policies
    (group_category, name, description, min_group_size, visitor_discount_percent, beneficiary_discount_percent,
     staff_discount_percent, train_discount_percent, free_adult_per_students, requires_verification, deposit_percent)
VALUES
    ('PRESCHOOL', 'Đoàn học sinh mầm non',
     'Giảm 40% vé cổng cho học sinh; cứ 10 học sinh miễn vé 1 người lớn; giảm 10% xe lửa / xe điện.',
     10, 40, 0, 0, 10, 10, FALSE, 30),
    ('SOCIAL_WELFARE', 'Đoàn bảo trợ xã hội',
     'Mái ấm, lớp học tình thương, trường khuyết tật... Miễn phí vé cổng cho đối tượng khuyết tật / hoàn cảnh đặc biệt; giảm 50% cho người phục vụ; giảm 20% xe lửa / xe điện. Cần giấy xác nhận hoặc danh sách của cơ quan có thẩm quyền.',
     10, 0, 100, 50, 20, NULL, TRUE, 30),
    ('REVOLUTIONARY_MERIT', 'Người có công với cách mạng',
     'Miễn phí vé cổng. Cần giấy tờ hợp lệ do cơ quan có thẩm quyền cấp.',
     10, 0, 100, 0, 0, NULL, TRUE, 30),
    ('STANDARD', 'Đoàn khác',
     'Tiểu học, THCS, THPT, đại học, cơ quan... Giảm 10% vé cổng cả đoàn; giảm 10% xe lửa / xe điện.',
     10, 10, 0, 10, 10, NULL, FALSE, 30);

-- ---------------------------------------------------------------------
-- Experience combos (prices are assumptions - the source lists none)
-- ---------------------------------------------------------------------
INSERT INTO combos (code, name, description, price, duration_minutes, min_participants, max_participants, min_age) VALUES
    ('COMBO_01', 'Khám phá những điều kỳ thú',
     'Một vòng xe lửa / xe điện, đi bộ cùng thuyết minh viên, tương tác với thú, trò chơi.', 180000, 150, 10, 50, 4),
    ('COMBO_02', 'Tour xe điện VIP',
     'Lấy thức ăn, cho thú ăn tại các điểm dừng, thuyết minh, quà lưu niệm.', 250000, 90, 6, 20, 3),
    ('COMBO_03', 'Thiên nhiên diệu kỳ',
     'Tìm hiểu cây cổ thụ quý, kỹ thuật chăm sóc cây, trang trí chậu cây hoặc nuôi sâu, 2 trò chơi.', 160000, 150, 10, 50, 6),
    ('COMBO_04', 'Nhà khoa học tí hon',
     'Xe lửa, cho hươu cao cổ ăn, nhập vai nhà khảo cổ, tham quan bảo tàng, nhận diện mẫu hóa thạch, 1 trò chơi.', 220000, 180, 10, 40, 6),
    ('COMBO_05', 'Cùng chăm sóc các bạn nhỏ đáng yêu',
     'Chuẩn bị thức ăn cho công và hồng hạc, chăm sóc da rùa, tham quan bệnh viện thú y.', 200000, 150, 10, 30, 6),
    ('COMBO_06', 'Đánh thức giác quan',
     'Trải nghiệm thị giác, xúc giác, vị giác, khứu giác, thính giác và đu quay lồng đèn.', 170000, 120, 10, 40, 4),
    ('COMBO_07', 'Giải mã Thảo Cầm Viên',
     'Hoạt động nhóm, sổ tay kiến thức, quà tặng.', 190000, 180, 10, 60, 8),
    ('COMBO_08', 'Vườn thú của bé',
     'Đi cùng thuyết minh viên, cho thú ăn, xe lửa, chơi trò chơi không giới hạn trong 30 phút.', 150000, 120, 10, 40, 3),
    ('COMBO_09', 'Khu rừng giữa lòng thành phố',
     'Trà, bánh và không gian yên tĩnh giữa cây xanh.', 120000, 90, 2, 30, NULL),
    ('COMBO_10', 'Khi màn đêm buông xuống',
     'Tour đêm quan sát các loài thú hoạt động về đêm.', 300000, 120, 6, 30, 8),
    ('COMBO_11', 'Tham quan bằng xe điện',
     'Tham quan toàn bộ Thảo Cầm Viên bằng xe điện suốt thời gian tham quan.', 150000, 90, 1, 20, NULL);
