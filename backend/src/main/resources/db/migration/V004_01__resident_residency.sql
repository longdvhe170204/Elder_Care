-- 004 T005: khóa theo người và bảng kỹ thuật (data-model mục 2, 11; research T-03, T-11). Cú pháp dùng được ở MySQL 8.0
-- và 8.4 (research R-01 của 000). Người cao tuổi là định danh logic, không có khóa ngoại sang module khác
-- (constitution X).

-- Đích khóa FOR UPDATE của mọi lệnh 004 trên cùng người (DBR-06, research T-03) và dấu "hồ sơ lưu trú đã đóng"
-- (FR-072): closed_at đặt một lần, không gỡ. Tạo khi lập lượt đăng ký hoặc hợp đồng đầu tiên.
CREATE TABLE resident_residency
(
    resident_id BIGINT      NOT NULL PRIMARY KEY,
    closed_at   DATETIME(6) NULL COMMENT 'Hồ sơ lưu trú đã đóng (FR-072)',
    version     INT         NOT NULL DEFAULT 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Mỗi việc của job hằng ngày chạy đúng một lần cho mỗi ngày, kể cả chạy bù (FR-077, BR-M02-19). scheduler_run_id là
-- lần chạy trong scheduler_run của 000. Việc lỗi không ghi dòng nên lần chạy sau làm lại; SKIPPED là ngày bị lỡ mà việc
-- chỉ có nghĩa với ngày mới nhất (research T-11).
CREATE TABLE daily_run
(
    task             VARCHAR(30)                            NOT NULL,
    `date`           DATE                                   NOT NULL,
    result           ENUM ('SUCCEEDED', 'SKIPPED')          NOT NULL,
    scheduler_run_id BIGINT                                 NULL,
    PRIMARY KEY (task, `date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhắc theo chu kỳ (như 001, 003): loại nhắc theo glossary mục 10.6.
CREATE TABLE m02_reminder
(
    type             VARCHAR(40) NOT NULL,
    subject_id       BIGINT      NOT NULL,
    last_reminded_at DATETIME(6) NOT NULL,
    `count`          INT         NOT NULL DEFAULT 1,
    PRIMARY KEY (type, subject_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT, UPDATE ON resident_residency TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON daily_run TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON m02_reminder TO 'eldercare_app'@'%';
