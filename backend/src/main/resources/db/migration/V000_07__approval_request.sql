-- 000 T089: yêu cầu phê duyệt (data-model mục 3, research R-10; FR-031 → FR-041, BR-M15-08 → 10).
-- approval_request: nhóm 2, chỉ đổi qua lệnh (khóa lạc quan version); lịch sử trạng thái ở audit_log.
-- display_code đặt ngay sau khi thêm (cột sinh không được tham chiếu AUTO_INCREMENT).
CREATE TABLE approval_request
(
    id                       BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    display_code             VARCHAR(20)   NULL,
    type                     VARCHAR(40)   NOT NULL,
    status                   VARCHAR(30)   NOT NULL,
    content                  JSON          NOT NULL,
    entity_type              VARCHAR(40)   NULL,
    entity_id                VARCHAR(64)   NULL,
    resident_id              BIGINT        NULL,
    floor_area_id            BIGINT        NULL,
    requester_id             BIGINT        NOT NULL,
    created_at               DATETIME(6)   NOT NULL,
    reason                   VARCHAR(2000) NOT NULL,
    requested_effective_date DATE          NOT NULL,
    actual_effective_date    DATE          NULL,
    submitted_at             DATETIME(6)   NULL,
    decided_by               BIGINT        NULL,
    decided_at               DATETIME(6)   NULL,
    decision_comment         VARCHAR(2000) NULL,
    self_approved            BOOLEAN       NOT NULL DEFAULT FALSE,
    closing_reason           VARCHAR(2000) NULL,
    applied_at               DATETIME(6)   NULL,
    catch_up_applied         BOOLEAN       NOT NULL DEFAULT FALSE,
    reminded_at              DATETIME(6)   NULL,
    escalated_at             DATETIME(6)   NULL,
    version                  INT           NOT NULL DEFAULT 0,
    CONSTRAINT uq_approval_request_display_code UNIQUE (display_code),
    CONSTRAINT chk_approval_request_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    KEY idx_approval_request_apply (status, actual_effective_date, decided_at),
    KEY idx_approval_request_remind (status, submitted_at),
    KEY idx_approval_request_requester (requester_id, status),
    KEY idx_approval_request_type (type, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- application_result: nhóm 3, chỉ thêm. Khóa chính là yêu cầu: một yêu cầu có tối đa một kết quả, nên tác động không
-- thể ghi hai lần (FR-037, SC-003).
CREATE TABLE application_result
(
    approval_request_id BIGINT      NOT NULL PRIMARY KEY,
    result_type         VARCHAR(40) NOT NULL,
    result_id           VARCHAR(64) NOT NULL,
    recorded_at         DATETIME(6) NOT NULL,
    CONSTRAINT fk_application_result_request FOREIGN KEY (approval_request_id) REFERENCES approval_request (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TRIGGER trg_application_result_no_update
    BEFORE UPDATE
    ON application_result
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'application_result is append-only (FR-037)';

DELIMITER $$
CREATE TRIGGER trg_application_result_no_delete
    BEFORE DELETE
    ON application_result
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'application_result rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

-- Đính chính cách C trỏ yêu cầu phê duyệt đã tạo ra nó (V000_06 để dành khóa ngoại cho bước này).
ALTER TABLE correction
    ADD CONSTRAINT fk_correction_approval_request FOREIGN KEY (approval_request_id) REFERENCES approval_request (id)
        ON DELETE RESTRICT;

GRANT INSERT, UPDATE ON approval_request TO 'eldercare_app'@'%';
GRANT INSERT ON application_result TO 'eldercare_app'@'%';
GRANT DELETE ON approval_request TO 'eldercare_retention'@'%';
GRANT DELETE ON application_result TO 'eldercare_retention'@'%';
