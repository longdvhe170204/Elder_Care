-- T050: hai danh mục bán trú; sức chứa luôn lấy CFG-M03-01, không lưu vào bảng.
CREATE TABLE day_care_rest_area (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(30) NOT NULL, name VARCHAR(100) NOT NULL,
 floor_id BIGINT NULL, area_id BIGINT NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE', version INT NOT NULL DEFAULT 0,
 active_name VARCHAR(100) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED,
 active_singleton TINYINT GENERATED ALWAYS AS (IF(status='ACTIVE',1,NULL)) STORED,
 UNIQUE KEY uq_day_care_active_name(active_name), UNIQUE KEY uq_day_care_singleton(active_singleton),
 CONSTRAINT chk_day_care_location CHECK((floor_id IS NULL)<>(area_id IS NULL)),
 CONSTRAINT fk_day_care_floor FOREIGN KEY(floor_id) REFERENCES floor(id) ON DELETE RESTRICT,
 CONSTRAINT fk_day_care_area FOREIGN KEY(area_id) REFERENCES area(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE day_care_session (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(50) NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE', version INT NOT NULL DEFAULT 0,
 active_name VARCHAR(50) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED,
 UNIQUE KEY uq_day_care_session_name(active_name),
 CONSTRAINT chk_day_care_session_time CHECK(start_time>=CAST('00:00:00' AS TIME) AND end_time<CAST('24:00:00' AS TIME) AND end_time>start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Dòng khóa kỹ thuật tồn tại trước cấu hình đầu tiên; không phải lịch đến hoặc dữ liệu nghiệp vụ tự sinh.
CREATE TABLE day_care_capacity_guard(id TINYINT NOT NULL PRIMARY KEY, CONSTRAINT chk_day_care_guard CHECK(id=1)) ENGINE=InnoDB;
INSERT INTO day_care_capacity_guard(id) VALUES(1);
GRANT INSERT,UPDATE ON day_care_rest_area TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE ON day_care_session TO 'eldercare_app'@'%';
-- MySQL yêu cầu quyền UPDATE để dùng SELECT ... FOR UPDATE trên dòng khóa.
GRANT UPDATE ON day_care_capacity_guard TO 'eldercare_app'@'%';
