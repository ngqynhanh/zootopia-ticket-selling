-- =====================================================================
-- Reference data the application needs to run (not demo data).
-- Prices and rules follow the public Saigon Zoo pages listed in
-- extra-resources/online-resource.txt (read 12/11/2026).
-- Combo prices are NOT published by the source: they are team assumptions.
-- Demo data (100+ users, orders, ...) goes in database/seed-data.sql (week 5).
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
