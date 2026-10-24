-- Cảnh báo Tạm vắng quá hạn trở lại, một lần cho mỗi mốc dự kiến trở lại (BR-M01-01, FR-050). Gia hạn đổi mốc dự kiến
-- nên sinh dòng mới. Khóa chính chống cảnh báo lặp khi job chạy lại hoặc hai nút cùng chạy. Chỉ thêm.
CREATE TABLE return_overdue_alert
(
    absence_id         BIGINT      NOT NULL COMMENT 'Lượt Tạm vắng của feature 004',
    expected_return_at DATETIME(6) NOT NULL COMMENT 'Mốc dự kiến trở lại đã được cảnh báo',
    resident_id        BIGINT      NOT NULL COMMENT 'Không có khóa ngoại: dòng không chứa dữ liệu cá nhân, không chặn job loại bỏ hồ sơ hết hạn',
    alerted_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (absence_id, expected_return_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT ON return_overdue_alert TO 'eldercare_app'@'%';
