-- 002 T055: bản ghi ngoại tuyến chờ xem lại (data-model mục 5 ban_ghi_cho_xem_lai; research A-11; FR-041c, CHK056).
-- Nhóm 2: chuyển trạng thái một lần (PENDING_REVIEW → ACCEPTED / REJECTED), mỗi quyết định ghi nhật ký; bản ghi bị từ
-- chối vẫn lưu, không có hiệu lực. Quyết định đầu tiên thắng: khóa dòng và version (000 FR-040).
CREATE TABLE pending_review_record
(
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_type       VARCHAR(60)  NOT NULL COMMENT 'Mã OfflineHandler.recordType, ví dụ M04.CARE_TASK_RESULT',
    content           JSON         NOT NULL COMMENT 'Nội dung gốc từ thiết bị',
    idempotency_key   CHAR(36)     NOT NULL,
    recorder_id       BIGINT       NOT NULL COMMENT 'Tài khoản người ghi',
    device_id         CHAR(36)     NOT NULL,
    device_time       DATETIME(6)  NOT NULL,
    synced_at         DATETIME(6)  NOT NULL,
    last_connected_at DATETIME(6)  NOT NULL COMMENT 'Lần kết nối gần nhất trước khi ngoại tuyến đã dùng để đối chiếu',
    pending_reason    ENUM ('LATER_THAN_SYNC', 'EARLIER_THAN_LAST_CONNECTION', 'EXCEEDS_CFG_M15_08') NOT NULL,
    floor_area_id     BIGINT       NULL COMMENT 'NULL: bản ghi không gắn tầng, Quản lý viện quyết định (CHK056)',
    status            ENUM ('PENDING_REVIEW', 'ACCEPTED', 'REJECTED') NOT NULL DEFAULT 'PENDING_REVIEW',
    decided_by        BIGINT       NULL,
    decided_at        DATETIME(6)  NULL,
    rejection_reason  VARCHAR(2000) NULL,
    result_reference  VARCHAR(200) NULL COMMENT 'Tham chiếu bản ghi thật do OfflineHandler.apply trả khi chấp nhận',
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_pending_review_record_key UNIQUE (recorder_id, idempotency_key),
    CONSTRAINT fk_pending_review_record_recorder FOREIGN KEY (recorder_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT chk_pending_review_record_decision CHECK (
        (status = 'PENDING_REVIEW' AND decided_by IS NULL AND decided_at IS NULL)
            OR (status <> 'PENDING_REVIEW' AND decided_by IS NOT NULL AND decided_at IS NOT NULL)),
    CONSTRAINT chk_pending_review_record_rejection CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL)),
    CONSTRAINT chk_pending_review_record_result CHECK ((status = 'ACCEPTED') = (result_reference IS NOT NULL)),
    KEY idx_pending_review_record_status (status, floor_area_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT, UPDATE ON pending_review_record TO 'eldercare_app'@'%';
