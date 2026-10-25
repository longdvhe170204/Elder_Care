-- 003 T006: cơ cấu phòng/giường, danh mục nhóm 1 (data-model mục 2).
CREATE TABLE area (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 code VARCHAR(20) NOT NULL, name VARCHAR(100) NOT NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 version INT NOT NULL DEFAULT 0, UNIQUE KEY uq_area_code(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE floor (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, area_id BIGINT NOT NULL,
 code VARCHAR(20) NOT NULL, name VARCHAR(100) NOT NULL, building_name VARCHAR(100) NULL,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE', version INT NOT NULL DEFAULT 0,
 UNIQUE KEY uq_floor_area_code(area_id,code),
 CONSTRAINT fk_floor_area FOREIGN KEY(area_id) REFERENCES area(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE room_type (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL,
 standard_beds SMALLINT NOT NULL, serves_long_term BOOLEAN NOT NULL DEFAULT TRUE,
 serves_short_term BOOLEAN NOT NULL DEFAULT TRUE,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE', version INT NOT NULL DEFAULT 0,
 active_name VARCHAR(100) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED,
 UNIQUE KEY uq_room_type_active_name(active_name),
 CONSTRAINT chk_room_type_beds CHECK(standard_beds>0),
 CONSTRAINT chk_room_type_residency CHECK(serves_long_term OR serves_short_term)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE room_type_care_level (
 room_type_id BIGINT NOT NULL, care_level ENUM('BASIC','FREQUENT','SPECIAL') NOT NULL,
 PRIMARY KEY(room_type_id,care_level),
 CONSTRAINT fk_room_type_level FOREIGN KEY(room_type_id) REFERENCES room_type(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE room (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, floor_id BIGINT NOT NULL, room_type_id BIGINT NOT NULL,
 code VARCHAR(20) NOT NULL, name VARCHAR(100) NOT NULL,
 gender_policy ENUM('MALE','FEMALE','UNRESTRICTED') NOT NULL,
 isolated BOOLEAN NOT NULL DEFAULT FALSE, status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 version INT NOT NULL DEFAULT 0, UNIQUE KEY uq_room_code(code),
 CONSTRAINT fk_room_floor FOREIGN KEY(floor_id) REFERENCES floor(id) ON DELETE RESTRICT,
 CONSTRAINT fk_room_type FOREIGN KEY(room_type_id) REFERENCES room_type(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE room_allowed_care_level (
 room_id BIGINT NOT NULL, care_level ENUM('BASIC','FREQUENT','SPECIAL') NOT NULL,
 PRIMARY KEY(room_id,care_level),
 CONSTRAINT fk_room_level FOREIGN KEY(room_id) REFERENCES room(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE bed_transfer_reason (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, code VARCHAR(30) NOT NULL, name VARCHAR(100) NOT NULL,
 medical_safety BOOLEAN NOT NULL DEFAULT FALSE, fixed BOOLEAN NOT NULL DEFAULT FALSE,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE', version INT NOT NULL DEFAULT 0,
 active_name VARCHAR(100) GENERATED ALWAYS AS (IF(status='ACTIVE',name,NULL)) STORED,
 UNIQUE KEY uq_bed_transfer_reason_code(code), UNIQUE KEY uq_bed_transfer_reason_name(active_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- Danh mục cố định theo FR-010, không phải tự sinh bản ghi nghiệp vụ.
INSERT INTO bed_transfer_reason(code,name,medical_safety,fixed)
 VALUES('INFECTION_CONTROL','Kiểm soát lây nhiễm',TRUE,TRUE);
GRANT INSERT,UPDATE,DELETE ON area TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON floor TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON room_type TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON room_type_care_level TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON room TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON room_allowed_care_level TO 'eldercare_app'@'%';
GRANT INSERT,UPDATE,DELETE ON bed_transfer_reason TO 'eldercare_app'@'%';
