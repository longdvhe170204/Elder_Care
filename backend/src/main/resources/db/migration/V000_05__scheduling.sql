-- 000 T033, T035: bảng hạ tầng kỹ thuật cho sự kiện và bộ lập lịch (research R-09, R-10); không thuộc ba nhóm dữ liệu,
-- không ghi nhật ký nghiệp vụ (data-model mục 6).

-- Event Publication Registry của Spring Modulith: sự kiện lưu cùng transaction của lệnh, giao sau commit, giao lại khi
-- tiến trình dừng giữa chừng. Lấy nguyên schema v2 MySQL của spring-modulith-events-jdbc 2.1.1 (giữ tên bảng viết hoa
-- như truy vấn của thư viện).
CREATE TABLE IF NOT EXISTS EVENT_PUBLICATION
(
    ID                     VARCHAR(36)   NOT NULL,
    LISTENER_ID            VARCHAR(512)  NOT NULL,
    EVENT_TYPE             VARCHAR(512)  NOT NULL,
    SERIALIZED_EVENT       VARCHAR(4000) NOT NULL,
    PUBLICATION_DATE       TIMESTAMP(6)  NOT NULL,
    COMPLETION_DATE        TIMESTAMP(6) DEFAULT NULL NULL,
    STATUS                 VARCHAR(20),
    COMPLETION_ATTEMPTS    INT,
    LAST_RESUBMISSION_DATE TIMESTAMP(6) DEFAULT NULL NULL,
    PRIMARY KEY (ID),
    INDEX EVENT_PUBLICATION_BY_COMPLETION_DATE_IDX (COMPLETION_DATE)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- ShedLock: khóa job để không chạy chồng (schema MySQL chuẩn của ShedLock 7).
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Mỗi lần chạy job một dòng; id là "định danh lần chạy" trong căn cứ nhật ký (FR-045).
CREATE TABLE scheduler_run
(
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    job             VARCHAR(60) NOT NULL,
    started_at      DATETIME(6) NOT NULL,
    finished_at     DATETIME(6) NULL,
    result          VARCHAR(20) NULL,
    processed_count INT         NULL,
    KEY idx_scheduler_run_job (job, started_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

GRANT INSERT, UPDATE, DELETE ON EVENT_PUBLICATION TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON shedlock TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON scheduler_run TO 'eldercare_app'@'%';
