-- 000 T053: đính chính bản ghi đã xác nhận (data-model mục 4, research R-08; FR-023 → FR-030, DBR-23).
-- correction: nhóm 3, chỉ thêm. original_record_id luôn là bản gốc ban đầu; khóa logic, không có khóa ngoại cứng vì bảng
-- gốc thay đổi theo loại. approval_request_id trỏ yêu cầu phê duyệt của cách C; khóa ngoại thêm ở V000_07 (T089).
CREATE TABLE correction
(
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_type         VARCHAR(40)   NOT NULL,
    original_record_id  BIGINT        NOT NULL,
    type                ENUM ('AMEND_CONTENT', 'VOID') NOT NULL,
    corrected_content   JSON          NULL,
    reason              VARCHAR(2000) NOT NULL,
    actor_id            BIGINT        NOT NULL,
    creation_method     ENUM ('A', 'B', 'C') NOT NULL,
    approval_request_id BIGINT        NULL,
    recorded_at         DATETIME(6)   NOT NULL,
    sequence_no         INT           NOT NULL,
    CONSTRAINT uq_correction_sequence UNIQUE (record_type, original_record_id, sequence_no),
    CONSTRAINT chk_correction_content CHECK (type <> 'AMEND_CONTENT' OR corrected_content IS NOT NULL),
    CONSTRAINT chk_correction_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT chk_correction_approval CHECK (creation_method <> 'C' OR approval_request_id IS NOT NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Bảng kỹ thuật tuần tự hóa các lần đính chính trên cùng bản gốc (khóa dòng khi thêm); không thuộc ba nhóm dữ liệu.
CREATE TABLE correction_chain
(
    record_type        VARCHAR(40) NOT NULL,
    original_record_id BIGINT      NOT NULL,
    correction_count   INT         NOT NULL DEFAULT 0,
    voided             BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (record_type, original_record_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Giá trị hiện hành (FR-030): bản AMEND_CONTENT có sequence_no lớn nhất của mỗi bản gốc.
CREATE VIEW v_latest_correction AS
SELECT c.record_type, c.original_record_id, c.id AS correction_id, c.corrected_content, c.recorded_at
FROM correction c
WHERE c.type = 'AMEND_CONTENT'
  AND c.sequence_no = (SELECT MAX(x.sequence_no)
                       FROM correction x
                       WHERE x.record_type = c.record_type
                         AND x.original_record_id = c.original_record_id
                         AND x.type = 'AMEND_CONTENT');

-- Bản gốc đã Hủy ghi nhận và thời điểm hủy, để loại khỏi thống kê từ thời điểm đó về sau (FR-029).
CREATE VIEW v_voided_record AS
SELECT record_type, original_record_id, MIN(recorded_at) AS voided_at
FROM correction
WHERE type = 'VOID'
GROUP BY record_type, original_record_id;

CREATE TRIGGER trg_correction_no_update
    BEFORE UPDATE
    ON correction
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'correction is append-only (DBR-23)';

DELIMITER $$
CREATE TRIGGER trg_correction_no_delete
    BEFORE DELETE
    ON correction
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'correction rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT ON correction TO 'eldercare_app'@'%';
GRANT DELETE ON correction TO 'eldercare_retention'@'%';
GRANT INSERT, UPDATE ON correction_chain TO 'eldercare_app'@'%';
GRANT DELETE ON correction_chain TO 'eldercare_retention'@'%';
