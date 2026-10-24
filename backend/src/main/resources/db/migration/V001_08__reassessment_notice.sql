-- Lần hệ thống báo bác sĩ cần đánh giá lại (FR-039 → FR-043; research H-11). Bảng kỹ thuật của chính M01, không phải
-- yêu cầu (Q-307): không có lệnh, không có trạng thái nghiệp vụ. "Cần đánh giá lại" tính từ các lần báo có notified_at
-- sau lần đánh giá Đã xác nhận gần nhất; hạn là min(deadline_from) cộng CFG-M01-03. deadline_from rỗng khi người cao
-- tuổi đang Điều trị tại bệnh viện; Ghi nhận trở về đặt giá trị. UNIQUE chống báo lặp khi job chạy lại hoặc sự kiện phát
-- lại (NFR-04). Không khóa ngoại tới resident (như return_overdue_alert): dòng không chứa dữ liệu cá nhân, không chặn
-- job loại bỏ hồ sơ hết hạn.
CREATE TABLE reassessment_notice
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    resident_id        BIGINT      NOT NULL,
    basis              ENUM ('PERIODIC','AFTER_INCIDENT','RETURN_FROM_HOSPITAL','ASSESSMENT_CORRECTION') NOT NULL,
    source_type        VARCHAR(30) NOT NULL COMMENT 'ASSESSMENT, INCIDENT, STATUS_HISTORY, CORRECTION',
    source_id          BIGINT      NOT NULL COMMENT 'Lần đánh giá, sự cố, dòng lịch sử trạng thái, bản đính chính',
    notified_at        DATETIME(6) NOT NULL,
    deadline_from      DATETIME(6) NULL COMMENT 'Mốc tính hạn CFG-M01-03; rỗng khi đang Điều trị tại bệnh viện',
    overdue_alerted_at DATETIME(6) NULL COMMENT 'Rỗng tới khi job cảnh báo quá hạn',
    PRIMARY KEY (id),
    CONSTRAINT uq_reassessment_notice_source UNIQUE (resident_id, basis, source_type, source_id),
    INDEX ix_reassessment_notice_resident (resident_id, notified_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT ON reassessment_notice TO 'eldercare_app'@'%';
GRANT UPDATE (deadline_from, overdue_alerted_at) ON reassessment_notice TO 'eldercare_app'@'%';
GRANT DELETE ON reassessment_notice TO 'eldercare_retention'@'%';
