-- 012 T004: hồ sơ người thân và quan hệ với người cao tuổi (data-model mục 0, 2; FR-001, FR-002, FR-007, FR-012, DBR-02;
-- research F-03, F-04). Theo R-15: khóa theo người cao tuổi (nguoi_than_nct) ở resident_module_state loại
-- M10.FAMILY_RESIDENT_STATE; lịch sử quyền, vai trò (lich_su_quyen) ở status_history loại M10.FAMILY_RELATIONSHIP.
-- Người cao tuổi là tham chiếu logic, không khóa ngoại sang module khác. Cú pháp dùng được ở MySQL 8.0 và 8.4 (R-01).

-- Người thân, nhóm 1: sửa thông tin liên hệ có nhật ký trước/sau; không xóa khi đã có quan hệ (khóa ngoại dưới).
-- Số giấy tờ là dữ liệu cá nhân (CA_NHAN); số điện thoại không duy nhất (FR-001).
CREATE TABLE family_member
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name          VARCHAR(200) NOT NULL,
    phone_number       VARCHAR(20)  NOT NULL,
    id_document_type   VARCHAR(40)  NULL,
    id_document_number VARCHAR(40)  NULL,
    address            VARCHAR(500) NULL,
    version            INT          NOT NULL DEFAULT 0,
    created_by         BIGINT       NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    KEY ix_family_member_phone (phone_number),
    KEY ix_family_member_id_document (id_document_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=CATALOG';

-- Quan hệ, nhóm 2: vai trò và sáu quyền tính riêng cho từng quan hệ (FR-002, FR-007); quyền chỉ đổi qua yêu cầu
-- (BR-M10-07) hoặc tác động FR-006, FR-009, FR-010. Đã kết thúc thì không kích hoạt lại (FR-006).
CREATE TABLE family_relationship
(
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    family_member_id    BIGINT        NOT NULL,
    resident_id         BIGINT        NOT NULL,
    relationship        ENUM ('SPOUSE', 'CHILD', 'GRANDCHILD', 'SIBLING', 'OTHER') NOT NULL,
    representative      BOOLEAN       NOT NULL DEFAULT FALSE,
    primary_contact     BOOLEAN       NOT NULL DEFAULT FALSE,
    contact_order       INT           NOT NULL COMMENT 'Thứ tự gọi trong nhóm cho 009 (FR-012)',
    view_health         BOOLEAN       NOT NULL DEFAULT FALSE,
    view_costs          BOOLEAN       NOT NULL DEFAULT FALSE,
    receive_urgent      BOOLEAN       NOT NULL DEFAULT FALSE,
    register_visits     BOOLEAN       NOT NULL DEFAULT FALSE,
    pickup_allowed      BOOLEAN       NOT NULL DEFAULT FALSE,
    request_services    BOOLEAN       NOT NULL DEFAULT FALSE,
    status              ENUM ('ACTIVE', 'ENDED') NOT NULL DEFAULT 'ACTIVE',
    started_at          DATETIME(6)   NOT NULL,
    ended_at            DATETIME(6)   NULL,
    end_reason          VARCHAR(2000) NULL,
    version             INT           NOT NULL DEFAULT 0,
    created_by          BIGINT        NOT NULL,
    created_at          DATETIME(6)   NOT NULL,
    active_key          TINYINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', 1, NULL)) STORED
        COMMENT 'UNIQUE cùng người thân, người cao tuổi: một quan hệ Hiệu lực mỗi cặp (FR-002)',
    primary_contact_key BIGINT GENERATED ALWAYS AS (IF(status = 'ACTIVE' AND primary_contact, resident_id, NULL)) STORED
        COMMENT 'UNIQUE: tối đa một người liên hệ chính Hiệu lực (DBR-02)',
    CONSTRAINT uq_family_relationship_active UNIQUE (family_member_id, resident_id, active_key),
    CONSTRAINT uq_family_relationship_primary_contact UNIQUE (primary_contact_key),
    CONSTRAINT fk_family_relationship_member FOREIGN KEY (family_member_id) REFERENCES family_member (id) ON DELETE RESTRICT,
    CONSTRAINT ck_family_relationship_ended CHECK (status <> 'ENDED' OR (ended_at IS NOT NULL AND end_reason IS NOT NULL)),
    CONSTRAINT ck_family_relationship_period CHECK (ended_at IS NULL OR ended_at >= started_at),
    KEY ix_family_relationship_resident (resident_id, status, contact_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'DataGroup=BUSINESS';

-- Ứng dụng không xóa (FR-001, FR-006); job loại bỏ dữ liệu xóa theo hồ sơ (LuuTruProvider, T008).
GRANT INSERT, UPDATE ON family_member TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON family_relationship TO 'eldercare_app'@'%';
GRANT DELETE ON family_member TO 'eldercare_retention'@'%';
GRANT DELETE ON family_relationship TO 'eldercare_retention'@'%';
