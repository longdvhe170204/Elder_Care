-- 011 T023 (US4; data-model mục 6, 0; FR-033 -> FR-043, BR-M08-01, 09, 13): lần chốt suất, nhóm 3.
-- Theo R-15: bua_ngay -> khóa kỹ thuật M08.DAILY_MEAL (id bữa ngày suy ra từ bữa và ngày, không có bảng; giờ bữa dự kiến
-- chốt vào lần chốt); nguoi_duoc_tinh, suat_theo_che_do -> cột JSON của portion_closing; phat_sinh, yeu_cau_suat_bo_sung
-- -> record_event; suat_an_nguoi_than, phieu_bua_an, suat_dac_biet -> simple_record. Cú pháp dùng được ở MySQL 8.0 và 8.4.
CREATE TABLE portion_closing
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    daily_meal_id          BIGINT      NOT NULL COMMENT 'Bữa ngày (bữa + ngày), UNIQUE: mỗi bữa chốt một lần (FR-033)',
    meal_id                BIGINT      NOT NULL COMMENT 'catalog_item loại M08.MEAL',
    meal_date              DATE        NOT NULL,
    meal_time              DATETIME(6) NOT NULL COMMENT 'Giờ bữa dự kiến chốt từ CFG-M08-04 (FR-006)',
    late_threshold_minutes INT         NOT NULL COMMENT 'Ngưỡng giao trễ CFG-M08-04 tại lúc chốt',
    closed_by              BIGINT      NOT NULL,
    closed_at              DATETIME(6) NOT NULL COMMENT 'Thời điểm chốt (FR-039)',
    closed_after_meal_time BOOLEAN     NOT NULL,
    menu_id                BIGINT      NULL COMMENT 'NULL: chưa có thực đơn công bố (FR-038)',
    total_portions         INT         NOT NULL,
    counted_residents      JSON        NOT NULL COMMENT 'CountedPerson[] (nguoi_duoc_tinh gộp)',
    portions_by_diet       JSON        NOT NULL COMMENT 'Số suất theo tầng/khu, chế độ ăn (suat_theo_che_do gộp)',
    flags                  JSON        NOT NULL COMMENT 'NO_MENU, CLOSED_AFTER_MEAL_TIME, MISSING_SUBSTITUTE_DISH, NO_DIET_ASSIGNED',
    missing_data           JSON        NOT NULL COMMENT 'STAY, TRIP: nguồn 012, 014 chưa có',
    CONSTRAINT uq_portion_closing_daily_meal UNIQUE (daily_meal_id),
    CONSTRAINT uq_portion_closing_meal_date UNIQUE (meal_id, meal_date),
    CONSTRAINT ck_portion_closing_total CHECK (total_portions >= 0),
    KEY ix_portion_closing_time (meal_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=RECORD';

-- Nhóm 3: lần chốt không sửa, không xóa bởi ứng dụng; thay đổi sau chốt là phát sinh.
DELIMITER $$
CREATE TRIGGER trg_portion_closing_no_update
    BEFORE UPDATE
    ON portion_closing
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'portion closing is immutable (group 3, FR-039)';
END$$

CREATE TRIGGER trg_portion_closing_no_delete
    BEFORE DELETE
    ON portion_closing
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'portion closing rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT SELECT, INSERT ON portion_closing TO 'eldercare_app'@'%';
GRANT DELETE ON portion_closing TO 'eldercare_retention'@'%';
