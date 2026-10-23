-- 006 T005: Bảng nền tảng của Feature 006 (Module M07) - Quản lý thuốc.
-- Gồm: resident_medication (khóa theo người, hạn đối chiếu), m07_job_run (lần chạy job),
-- medication (danh mục thuốc - nhóm 1), medication_active_ingredient (hoạt chất thuốc).
-- Căn cứ: data-model mục 2, 3; research D-01, D-03, D-04, D-19.

-- 1. resident_medication (thuoc_nct) - Nhóm 2 kỹ thuật
-- Đích khóa theo người, theo dõi hạn đối chiếu thuốc khi tiếp nhận hoặc trở về từ bệnh viện.
CREATE TABLE resident_medication
(
    resident_id              BIGINT                                NOT NULL PRIMARY KEY COMMENT 'Khóa chính, logic id người cao tuổi',
    reconciliation_due_at    DATETIME(6)                           NULL COMMENT 'Thời điểm hết hạn đối chiếu',
    reconciliation_reason    ENUM ('ADMISSION', 'HOSPITAL_RETURN') NULL COMMENT 'Lý do đối chiếu có hạn (TIEP_NHAN, TRO_VE_BENH_VIEN)',
    reconciliation_warned_at DATETIME(6)                           NULL COMMENT 'Thời điểm đã phát cảnh báo hạn đối chiếu',
    version                  INT                                   NOT NULL DEFAULT 0 COMMENT 'Khóa lạc quan',
    CONSTRAINT chk_resident_med_reconciliation_reason CHECK (reconciliation_due_at IS NULL OR reconciliation_reason IS NOT NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 2. m07_job_run (lan_chay_m07) - Bảng kỹ thuật
-- Theo dõi lần chạy job (research D-19) để bảo đảm mỗi việc mỗi mốc một lần, kể cả chạy bù.
CREATE TABLE m07_job_run
(
    job_key          VARCHAR(64)  NOT NULL COMMENT 'Mã việc: NCT_CHUA_PHU_TRACH, TANG_KHONG_DIEU_DUONG, HET_HAN_DON, NHAC_HET_HAN_DON, HET_HAN_DUNG',
    checkpoint       VARCHAR(128) NOT NULL COMMENT 'Mốc chạy ví dụ caId:nctId, tang:caId hoặc ngày yyyy-MM-dd',
    result           VARCHAR(500) NULL COMMENT 'Tóm tắt kết quả chạy',
    scheduler_run_id BIGINT       NULL COMMENT 'Tham chiếu phiên chạy lịch của 000',
    completed_at     DATETIME(6)  NULL COMMENT 'Thời điểm hoàn thành',
    PRIMARY KEY (job_key, checkpoint)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 3. medication (thuoc) - Danh mục nhóm 1 (FR-001, FR-002, AC-01)
CREATE TABLE medication
(
    id                    BIGINT                      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name                  VARCHAR(200)                NOT NULL COMMENT 'Tên thuốc',
    concentration         VARCHAR(100)                NOT NULL COMMENT 'Hàm lượng (ví dụ 500mg, 10ml)',
    dosage_form           VARCHAR(100)                NOT NULL COMMENT 'Dạng bào chế (viên nén, siro, dung dịch...)',
    default_route         VARCHAR(100)                NOT NULL COMMENT 'Đường dùng mặc định (Uống, Tiêm, Bôi...)',
    usage_unit            VARCHAR(50)                 NOT NULL COMMENT 'Đơn vị dùng (viên, ml, gói...)',
    before_window_minutes SMALLINT                    NULL COMMENT 'Cửa sổ trước (phút), rỗng dùng CFG-M07-01',
    after_window_minutes  SMALLINT                    NULL COMMENT 'Cửa sổ sau (phút), rỗng dùng CFG-M07-01',
    special_control       BOOLEAN                     NOT NULL DEFAULT FALSE COMMENT 'Thuốc kiểm soát đặc biệt',
    status                ENUM ('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Trạng thái hiệu lực',
    version               INT                         NOT NULL DEFAULT 0 COMMENT 'Phiên bản danh mục',
    CONSTRAINT uq_medication_identity UNIQUE (name, concentration, dosage_form),
    CONSTRAINT chk_medication_window_before CHECK (before_window_minutes IS NULL OR before_window_minutes >= 0),
    CONSTRAINT chk_medication_window_after CHECK (after_window_minutes IS NULL OR after_window_minutes >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 4. medication_active_ingredient (thuoc_hoat_chat) - Nhóm 1 bảng con
-- Liên kết thuốc với danh mục dị nguyên nhóm thuốc của 001 để kiểm tra dị ứng và trùng hoạt chất.
CREATE TABLE medication_active_ingredient
(
    medication_id BIGINT NOT NULL COMMENT 'Khóa ngoại trỏ tới medication',
    allergen_id   BIGINT NOT NULL COMMENT 'Tham chiếu logic dị nguyên của 001',
    PRIMARY KEY (medication_id, allergen_id),
    CONSTRAINT fk_med_active_ingredient_med FOREIGN KEY (medication_id) REFERENCES medication (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Quyền tài khoản theo R-05
GRANT INSERT, UPDATE, DELETE ON medication TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON medication_active_ingredient TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON resident_medication TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON m07_job_run TO 'eldercare_app'@'%';
