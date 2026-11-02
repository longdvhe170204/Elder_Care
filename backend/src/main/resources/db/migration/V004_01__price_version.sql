-- 004 T007, T110: phiên bản đơn giá (data-model mục 0, 6; FR-034, FR-035, DBR-08, Q-297; research T-05). Dịch vụ là mục
-- catalog_item loại M02.SERVICE (R-15). Cú pháp dùng được ở MySQL 8.0 và 8.4 (research R-01 của 000).

-- Phiên bản đơn giá, nhóm 1 theo phiên bản (000 mục 1.2). effective_to gồm cả ngày cuối, NULL là còn mở. referenced: hợp
-- đồng ký hoặc 010 lập khoản thì đặt, từ đó không sửa giá và ngày bắt đầu (FR-035).
CREATE TABLE price_version
(
    id             BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_id     BIGINT         NOT NULL,
    unit_price     DECIMAL(15, 0) NOT NULL,
    effective_from DATE           NOT NULL,
    effective_to   DATE           NULL,
    referenced     BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_price_version_service FOREIGN KEY (service_id) REFERENCES catalog_item (id) ON DELETE RESTRICT,
    CONSTRAINT chk_price_version_price CHECK (unit_price >= 0),
    CONSTRAINT chk_price_version_range CHECK (effective_to IS NULL OR effective_to >= effective_from),
    KEY idx_price_version_service (service_id, effective_from)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=CATALOG';

-- Cơ chế chính của DBR-08 là khóa TechnicalLocks theo dịch vụ; hai trigger đầu là lưới an toàn. Thông báo bằng ASCII.
DELIMITER $$
CREATE TRIGGER trg_price_version_no_overlap_insert
    BEFORE INSERT
    ON price_version
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1
               FROM price_version p
               WHERE p.service_id = NEW.service_id
                 AND p.effective_from <= COALESCE(NEW.effective_to, '9999-12-31')
                 AND (p.effective_to IS NULL OR p.effective_to >= NEW.effective_from)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'price versions of one service must not overlap (DBR-08)';
    END IF;
END$$

CREATE TRIGGER trg_price_version_no_overlap_update
    BEFORE UPDATE
    ON price_version
    FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1
               FROM price_version p
               WHERE p.service_id = NEW.service_id
                 AND p.id <> OLD.id
                 AND p.effective_from <= COALESCE(NEW.effective_to, '9999-12-31')
                 AND (p.effective_to IS NULL OR p.effective_to >= NEW.effective_from)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'price versions of one service must not overlap (DBR-08)';
    END IF;
END$$

-- Phiên bản đã tham chiếu: giữ giá và ngày bắt đầu, chỉ cho đặt effective_to khi đang NULL (đóng khoảng).
CREATE TRIGGER trg_price_version_immutable
    BEFORE UPDATE
    ON price_version
    FOR EACH ROW
BEGIN
    IF OLD.referenced AND NOT (OLD.service_id <=> NEW.service_id AND OLD.unit_price <=> NEW.unit_price
        AND OLD.effective_from <=> NEW.effective_from AND OLD.referenced <=> NEW.referenced
        AND (OLD.effective_to IS NULL OR OLD.effective_to <=> NEW.effective_to)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'a referenced price_version keeps its price and start date (DBR-08)';
    END IF;
END$$
DELIMITER ;

-- Không sửa giá và ngày bắt đầu; chỉ đóng khoảng và đánh dấu đã tham chiếu (FR-004 của 000, FR-035).
GRANT INSERT ON price_version TO 'eldercare_app'@'%';
GRANT UPDATE (effective_to, referenced) ON price_version TO 'eldercare_app'@'%';
