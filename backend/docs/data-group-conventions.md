# Quy ước nhóm dữ liệu cho bảng và mã

Căn cứ: spec 000 mục A (FR-001 → FR-010), data-model 000 mục 1, research R-05. Mỗi module áp quy ước này cho bảng của mình. Mẫu chạy được ở `src/main/resources/db/migration/demo/V000_90__sample_data.sql` (dữ liệu mẫu, profile `demo`, `test`) và mã mẫu ở `src/test/java/com/cambae/backend/m15/sample/`.

## Nhóm 1 – Danh mục

- Entity kế thừa `common.data.CatalogBase`, mang `@DataGroup(CATALOG)`.
- Cột chuẩn: `name`, `status ENUM('ACTIVE','INACTIVE')`, `version INT`, cột sinh `active_name` có `UNIQUE` để hai mục Hiệu lực không trùng tên (FR-003, BR-M15-06).

```sql
CREATE TABLE <catalog> (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    status      ENUM ('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    active_name VARCHAR(100) GENERATED ALWAYS AS (IF(status = 'ACTIVE', name, NULL)) STORED,
    version     INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_<catalog>_active_name UNIQUE (active_name)
);
GRANT INSERT, UPDATE, DELETE ON <catalog> TO 'eldercare_app'@'%';
```

- Bảng nhóm 2, 3 tham chiếu danh mục bằng khóa ngoại `ON DELETE RESTRICT`. Xóa mục đã được tham chiếu: MySQL báo lỗi 1451, `ProblemDetailsHandler` trả 409 `CATALOG_ITEM_IN_USE` (FR-002).
- Nghiệp vụ mới chỉ chọn mục `isActive()`; kích hoạt lại truyền mục Hiệu lực cùng tên (nếu có) vào `reactivate(...)`.
- Mỗi lần tạo, sửa, ngừng hiệu lực, kích hoạt lại, xóa ghi một bản ghi nhật ký qua `AuditLogApi.write` trong cùng transaction (FR-043).

## Nhóm 2 – Nghiệp vụ có trạng thái

- Entity mang `@DataGroup(BUSINESS)`, không có setter công khai, không dùng Lombok `@Data`, `@Setter`; trạng thái chỉ đổi qua phương thức lệnh. Kiểm bằng `DataGroupArchTest`.
- Cột chuẩn: `status`, `version` (khóa lạc quan, đi với `If-Match`), `created_at`, `created_by`.
- Phương thức lệnh ở service mang `@BusinessCommand` và nhận `Reason`; thiếu lý do thì `BusinessCommandAspect` trả 422 `REASON_REQUIRED` trước khi mở transaction (FR-007, SC-008).
- API chỉ có `POST …/{id}/<lệnh>`, không có `PUT`, `PATCH`, `DELETE` (R-03).
- Phiên bản có hiệu lực: bảng phiên bản có `effective_from DATE`, `effective_to DATE NULL` và `CHECK (effective_to IS NULL OR effective_to >= effective_from)`; thêm phiên bản qua `EffectiveVersioning.addVersion(...)` (khóa dòng cha, kiểm không chồng khoảng; FR-008). Đổi thuộc tính theo phiên bản (FR-004) qua `EffectiveVersioning.addVersionFrom(...)`: ngày hiệu lực mới phải sau hôm nay, phiên bản đang mở được đóng ở ngày trước đó. Tài khoản ứng dụng cần `UPDATE` trên bảng cha để `SELECT … FOR UPDATE`.

```sql
CREATE TABLE <entity>_version (
    id             BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    <entity>_id    BIGINT NOT NULL,
    effective_from DATE   NOT NULL,
    effective_to   DATE   NULL,
    CONSTRAINT fk_<entity>_version FOREIGN KEY (<entity>_id) REFERENCES <entity> (id) ON DELETE RESTRICT,
    CONSTRAINT chk_<entity>_version_range CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
GRANT INSERT ON <entity>_version TO 'eldercare_app'@'%';
-- Chỉ để đóng phiên bản đang mở (FR-004); các cột khác không sửa được
GRANT UPDATE (effective_to) ON <entity>_version TO 'eldercare_app'@'%';
```

## Nhóm 3 – Ghi nhận đã xác nhận

- Entity mang `@DataGroup(RECORD)`, repository kế thừa `AppendOnlyRepository` (chỉ `append`, đọc).
- Cột chuẩn: `occurred_at` (giờ máy chủ), `device_time` (bản ghi ngoại tuyến), `actor_id`; `confirmed_at` khi bảng có bản nháp; `floor_area_id` khi loại bản ghi gắn tầng/khu vực (FR-026 b).
- Bắt buộc hai trigger; `RecordGroupTriggerIT` đối chiếu mọi entity nhóm 3 với `information_schema.TRIGGERS`. Dùng `USER()` (tài khoản đang kết nối), không dùng `CURRENT_USER()` (trong trigger là tài khoản tạo trigger). Thông báo viết ASCII.

```sql
-- Bảng không có bản nháp: chặn mọi UPDATE, tài khoản ứng dụng chỉ có INSERT.
CREATE TRIGGER trg_<table>_no_update
    BEFORE UPDATE ON <table>
    FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '<table> is append-only (FR-009)';

-- Bảng có bản nháp thay trigger trên bằng: IF OLD.confirmed_at IS NOT NULL THEN SIGNAL ... END IF;

DELIMITER $$
CREATE TRIGGER trg_<table>_no_delete
    BEFORE DELETE ON <table>
    FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) <> 'eldercare_retention' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '<table> rows are removed only by the retention job (FR-051)';
    END IF;
END$$
DELIMITER ;

GRANT INSERT ON <table> TO 'eldercare_app'@'%';
GRANT DELETE ON <table> TO 'eldercare_retention'@'%';
```

## Quyền tài khoản

`V000_00` chỉ cấp `SELECT` mức schema cho `eldercare_app`, `eldercare_retention`. Migration của từng bảng cấp quyền ghi cần thiết như các mẫu trên (R-05).
