-- 004 Phase 7 (US5): phụ lục hợp đồng và chỉ mục thuộc tính của yêu cầu thay đổi lưu trú (data-model mục 7; FR-037 → FR-047,
-- DBR-07). Tên cột theo glossary 10.3: phu_luc_hop_dong = contract_addendum, yeu_cau_thay_doi_thuoc_tinh =
-- change_request_attribute.

-- Phụ lục chỉ do một yêu cầu M02.RESIDENCY_CHANGE đã duyệt tạo (DBR-07: approval_request_id bắt buộc, duy nhất). content_before,
-- content_after chỉ giữ các thuộc tính bị đổi (dạng khác biệt), nên không lệch khi nội dung cơ sở đổi giữa lúc duyệt và lúc
-- áp dụng. Không có lệnh sửa, xóa (FR-047): trigger chỉ cho đặt trạng thái và phiên bản nội dung một lần.
CREATE TABLE contract_addendum
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    contract_id         BIGINT       NOT NULL,
    approval_request_id BIGINT       NOT NULL,
    change_types        SET ('RESIDENCE_TYPE', 'CARE_LEVEL', 'SERVICE', 'DURATION', 'BED', 'ARRIVAL_SCHEDULE') NOT NULL,
    content_before      JSON         NOT NULL,
    content_after       JSON         NOT NULL,
    effective_date      DATE         NOT NULL,
    status              ENUM ('PENDING_EFFECTIVE', 'APPLIED', 'NOT_APPLIED') NOT NULL DEFAULT 'PENDING_EFFECTIVE',
    content_version_id  BIGINT       NULL,
    created_at          DATETIME(6)  NOT NULL,
    CONSTRAINT uq_contract_addendum_request UNIQUE (approval_request_id),
    CONSTRAINT chk_contract_addendum_applied CHECK (status <> 'APPLIED' OR content_version_id IS NOT NULL),
    CONSTRAINT fk_contract_addendum_contract FOREIGN KEY (contract_id) REFERENCES contract (id) ON DELETE RESTRICT,
    CONSTRAINT fk_contract_addendum_version FOREIGN KEY (content_version_id) REFERENCES contract_content_version (id) ON DELETE RESTRICT,
    KEY ix_contract_addendum_contract (contract_id, effective_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Phiên bản nội dung nay có khóa ngoại tới phụ lục (V004_05 chưa có bảng này). Hai phụ lục khác thuộc tính cùng ngày hiệu lực
-- là hợp lệ (FR-046 chỉ chặn cùng thuộc tính), nên (contract_id, effective_from) không còn duy nhất: phiên bản sau (sequence_no
-- lớn hơn) xây trên phiên bản trước và thắng khi tra theo ngày.
ALTER TABLE contract_content_version
    DROP INDEX uq_contract_content_version_from,
    ADD KEY ix_contract_content_version_from (contract_id, effective_from, sequence_no),
    ADD CONSTRAINT fk_contract_content_version_addendum FOREIGN KEY (addendum_id) REFERENCES contract_addendum (id) ON DELETE RESTRICT;

-- Chỉ mục kỹ thuật cho FR-046: mỗi (người cao tuổi, thuộc tính) có tối đa một yêu cầu Đã duyệt chờ hiệu lực. Dòng được thêm
-- khi lập yêu cầu (pending_effective = FALSE, để tra yêu cầu đang mở của người đó), đặt TRUE khi duyệt, FALSE khi phụ lục rời
-- Chờ hiệu lực. lock_key chỉ có giá trị khi chờ hiệu lực nên UNIQUE chỉ ràng buộc các dòng đó.
CREATE TABLE change_request_attribute
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    approval_request_id BIGINT      NOT NULL,
    contract_id         BIGINT      NOT NULL,
    resident_id         BIGINT      NOT NULL,
    attribute           ENUM ('RESIDENCE_TYPE', 'CARE_LEVEL', 'SERVICE', 'DURATION', 'BED', 'ARRIVAL_SCHEDULE') NOT NULL,
    pending_effective   BOOLEAN     NOT NULL DEFAULT FALSE,
    lock_key            VARCHAR(60) GENERATED ALWAYS AS (IF(pending_effective, CONCAT(resident_id, ':', attribute), NULL)) STORED,
    CONSTRAINT uq_change_request_attribute_request UNIQUE (approval_request_id, attribute),
    CONSTRAINT uq_change_request_attribute_lock UNIQUE (lock_key),
    KEY ix_change_request_attribute_resident (resident_id, attribute)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Dùng USER() (tài khoản đang kết nối): trong trigger CURRENT_USER() là tài khoản tạo trigger. Thông báo bằng ASCII.
DELIMITER $$

-- DBR-07: ngày hiệu lực không sớm hơn ngày bắt đầu hợp đồng gốc (FR-040). Backend kiểm trước; đây là lưới an toàn.
CREATE TRIGGER trg_contract_addendum_effective_date
    BEFORE INSERT
    ON contract_addendum
    FOR EACH ROW
BEGIN
    IF NEW.effective_date < (SELECT c.start_date FROM contract c WHERE c.id = NEW.contract_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'addendum effective date cannot precede the contract start date (DBR-07)';
    END IF;
END$$

-- FR-047: phụ lục không bị sửa. Chỉ trạng thái (Chờ hiệu lực sang Đã áp dụng hoặc Không áp dụng, một lần) và phiên bản
-- nội dung được ghi; mọi cột còn lại bất biến.
CREATE TRIGGER trg_contract_addendum_immutable
    BEFORE UPDATE
    ON contract_addendum
    FOR EACH ROW
BEGIN
    IF NOT (NEW.contract_id <=> OLD.contract_id
        AND NEW.approval_request_id <=> OLD.approval_request_id
        AND NEW.change_types <=> OLD.change_types
        AND NEW.effective_date <=> OLD.effective_date
        AND NEW.created_at <=> OLD.created_at
        AND CAST(NEW.content_before AS CHAR) <=> CAST(OLD.content_before AS CHAR)
        AND CAST(NEW.content_after AS CHAR) <=> CAST(OLD.content_after AS CHAR)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract_addendum content is immutable (FR-047)';
    END IF;
    IF OLD.status <> 'PENDING_EFFECTIVE' AND NOT (NEW.status <=> OLD.status AND NEW.content_version_id <=> OLD.content_version_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract_addendum in a final status cannot change (FR-047)';
    END IF;
END$$

CREATE TRIGGER trg_contract_addendum_no_delete
    BEFORE DELETE
    ON contract_addendum
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'contract_addendum rows are removed only by the retention job (FR-047)';
    END IF;
END$$

CREATE TRIGGER trg_change_request_attribute_no_delete
    BEFORE DELETE
    ON change_request_attribute
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'change_request_attribute rows are removed only by the retention job';
    END IF;
END$$
DELIMITER ;

GRANT INSERT, UPDATE ON contract_addendum TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON change_request_attribute TO 'eldercare_app'@'%';
GRANT DELETE ON contract_addendum TO 'eldercare_retention'@'%';
GRANT DELETE ON change_request_attribute TO 'eldercare_retention'@'%';
