-- 001 T006: danh mục của M01, nhóm 1 (data-model mục 5 "Danh mục"; BR-M01-11, BR-M15-06; Q-291). Mẫu bảng nhóm 1 của 000
-- (docs/data-group-conventions.md). Bảng nhóm 2, 3 tham chiếu bằng khóa ngoại ON DELETE RESTRICT khi bảng đó được tạo.
-- Cú pháp dùng được ở MySQL 8.0 và 8.4.

-- Dị nguyên (di_nguyen): rỗng, nạp khi triển khai. Nhóm: hoạt chất thuốc (006 FR-002), thực phẩm (011 FR-003), khác.
CREATE TABLE allergen
(
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    allergen_group ENUM ('ACTIVE_INGREDIENT', 'FOOD', 'OTHER') NOT NULL,
    status         ENUM ('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    active_name    VARCHAR(100) GENERATED ALWAYS AS (IF(status = 'ACTIVE', name, NULL)) STORED,
    version        INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_allergen_active_name UNIQUE (active_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Mức độ dị ứng: Nhẹ, Trung bình, Nặng (BR-M01-11).
CREATE TABLE allergy_severity
(
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    display_order INT          NOT NULL,
    status        ENUM ('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    active_name   VARCHAR(100) GENERATED ALWAYS AS (IF(status = 'ACTIVE', name, NULL)) STORED,
    version       INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_allergy_severity_active_name UNIQUE (active_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nguồn thông tin của mục sức khỏe (BR-M01-11).
CREATE TABLE information_source
(
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    status      ENUM ('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    active_name VARCHAR(100) GENERATED ALWAYS AS (IF(status = 'ACTIVE', name, NULL)) STORED,
    version     INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_information_source_active_name UNIQUE (active_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Nhu cầu chăm sóc (Q-291): danh mục riêng, không gắn cờ cho người cao tuổi.
CREATE TABLE care_need
(
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    status      ENUM ('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    active_name VARCHAR(100) GENERATED ALWAYS AS (IF(status = 'ACTIVE', name, NULL)) STORED,
    version     INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_care_need_active_name UNIQUE (active_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO allergy_severity (name, display_order)
VALUES ('Nhẹ', 1),
       ('Trung bình', 2),
       ('Nặng', 3);

INSERT INTO information_source (name)
VALUES ('Người cao tuổi'),
       ('Người thân'),
       ('Hồ sơ y tế kèm theo'),
       ('Bác sĩ khám');

INSERT INTO care_need (name)
VALUES ('Hỗ trợ vận động'),
       ('Theo dõi sức khỏe thường xuyên'),
       ('Phục hồi sau tai biến'),
       ('Phục hồi chức năng khác');

GRANT INSERT, UPDATE, DELETE ON allergen TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON allergy_severity TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON information_source TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON care_need TO 'eldercare_app'@'%';
