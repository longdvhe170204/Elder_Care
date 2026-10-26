-- 004 Phase 10 (US8): bản ghi qua đời và danh sách việc sau qua đời (data-model mục 10; FR-069 → FR-073, BR-M02-21). Tên theo
-- glossary 10.6: ban_ghi_qua_doi = death_record, muc_viec_sau_qua_doi = post_death_task. Cú pháp dùng được ở MySQL 8.0 và 8.4
-- (research R-01 của 000).

-- Bản ghi qua đời (nhóm 3): mỗi người một bản ghi (resident_id UNIQUE), chỉ thêm. Bổ sung nguyên nhân hay sửa sai đi đường
-- đính chính M02.DEATH_RECORD (000). cause_of_death thuộc nhóm thông tin sức khỏe (FR-069, BR-M02-21). Bằng chứng bắt buộc
-- khi Hành chính thực hiện: 001 kiểm lúc nhận lệnh (FR-047b).
CREATE TABLE death_record
(
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resident_id      BIGINT        NOT NULL,
    death_at         DATETIME(6)   NOT NULL,
    place            VARCHAR(255)  NOT NULL,
    discovered_by    VARCHAR(255)  NOT NULL,
    confirmed_by_id  BIGINT        NOT NULL,
    cause_of_death   VARCHAR(2000) NULL,
    evidence_file_id BIGINT        NULL,
    history_id       BIGINT        NOT NULL COMMENT 'Lịch sử trạng thái của lệnh Ghi nhận qua đời (001)',
    recorded_by      BIGINT        NOT NULL,
    recorded_at      DATETIME(6)   NOT NULL,
    CONSTRAINT uq_death_record_resident UNIQUE (resident_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Kết quả tính của năm mục việc sau qua đời (nhóm 2): ghi lại khi MucViecSauQuaDoiPort trả kết quả khác (research T-13). Hồ sơ
-- lưu trú Đã đóng (resident_residency.closed_at) đặt khi mọi dòng xong và không gỡ được (FR-072).
CREATE TABLE post_death_task
(
    death_record_id BIGINT        NOT NULL,
    item_code       ENUM ('BELONGINGS', 'STORED_MEDICATION', 'FINAL_PERIOD_COSTS', 'ALERTS_INCIDENTS', 'SETTLEMENT') NOT NULL,
    status          ENUM ('NOT_COMPLETED', 'COMPLETED', 'NOT_APPLICABLE') NOT NULL,
    basis           VARCHAR(1000) NULL,
    updated_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (death_record_id, item_code),
    CONSTRAINT fk_post_death_task_record FOREIGN KEY (death_record_id) REFERENCES death_record (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$

CREATE TRIGGER trg_death_record_immutable
    BEFORE UPDATE
    ON death_record
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'death_record rows are append-only, use a correction (FR-069, DBR-23)';
END$$

CREATE TRIGGER trg_death_record_no_delete
    BEFORE DELETE
    ON death_record
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'death_record rows are removed only by the retention job';
    END IF;
END$$

CREATE TRIGGER trg_post_death_task_no_delete
    BEFORE DELETE
    ON post_death_task
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'post_death_task rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT ON death_record TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON post_death_task TO 'eldercare_app'@'%';
GRANT DELETE ON death_record TO 'eldercare_retention'@'%';
GRANT DELETE ON post_death_task TO 'eldercare_retention'@'%';
