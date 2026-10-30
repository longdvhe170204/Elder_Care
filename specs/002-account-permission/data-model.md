# Data Model: Tài khoản và phân quyền (002)

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, trường, ràng buộc và cách bảo đảm từng quy tắc. Đây không phải script SQL; migration được viết ở giai đoạn triển khai. Quy ước kiểu dữ liệu, nhóm dữ liệu và bảo đảm ở cơ sở dữ liệu theo [../000-shared-foundation/data-model.md](../000-shared-foundation/data-model.md) mục 1.

Mọi bảng nằm trong schema của ứng dụng, thuộc module M15 (gói `administration.taikhoan`). Module khác không đọc các bảng này, chỉ gọi giao diện ở [contracts/module-interfaces.md](contracts/module-interfaces.md).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp: `tai_khoan`; `phien_dang_nhap`; `thiet_bi`; `ma_tran_quyen`; `thiet_bi_xac_thuc` = `ma_qr_dang_nhap` + `thiet_bi_gan` (giữ bảng vì tra mã băm khi đăng nhập); `quyen_tai_khoan` = `gan_vai_tro` + `thu_hep_quyen` + `quyen_gan_them` (cột `loai`, cùng nhóm 1 có lịch sử).
- `danh_muc_chung`: `vai_tro`, `quy_tac_xem_truong`, `nghiep_vu_chuyen_mon` (phạm vi trong `thuoc_tinh`).
- `lich_su_chung`: `lich_su_trang_thai_tai_khoan`.
- `ho_so_chung`: `ban_ghi_cho_xem_lai`, `loi_du_lieu_quyen` (lỗi đang mở: `khoa_duy_nhat` = nguồn, bản ghi, loại lỗi); `moc_da_nhac`: `da_canh_bao_giay_phep`.
- Bảng phụ, bảng con *(bổ sung 2026-10-30, rà đủ tên bảng của data-model)*: `nghiep_vu_chuyen_mon_pham_vi` → `danh_muc_chung`. Không thêm bảng riêng.
- Còn 6 bảng riêng của feature.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-29; tên mã theo bảng đã cài, migration `V002_95`)*

Mô tả dưới đây thay mô tả của các bảng đã gộp (mô tả cũ đã bỏ ở các mục sau, 2026-10-30; mã gộp ở 002 T098); cột không nhắc lại giữ như bảng đã cài (`V002_01` → `V002_13`).

**Giữ nguyên**: `account` (mọi cột, `CHECK`, `UNIQUE`; trigger không xóa), `login_session` (`open_device_id` `UNIQUE`), `device`, `permission_matrix` (khóa chính `(role, function_code, operation)`; bỏ khóa ngoại tới `role` vì vai trò chuyển vào danh mục, backend kiểm theo `common.Role`).

**`permission_assignment`** (`quyen_tai_khoan`, mới) – nhóm 1 có lịch sử. Gộp `role_assignment`, `permission_restriction`, `additional_grant`:

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| `id` | `BIGINT` | Giữ id của `role_assignment`; hai bảng kia cấp id mới |
| `kind` | `ENUM('ROLE_ASSIGNMENT','RESTRICTION','ADDITIONAL_GRANT')` | |
| `applies_to` | `ENUM('ROLE','ACCOUNT')` | `ACCOUNT` với `ROLE_ASSIGNMENT`, `ADDITIONAL_GRANT` |
| `account_id` | `BIGINT NULL` | Khóa ngoại `account` |
| `role` | `VARCHAR(5) NULL` | Vai trò được gán, hoặc vai trò bị thu hẹp |
| `function_code`, `operation` | `NULL` | Bắt buộc với `RESTRICTION`, `ADDITIONAL_GRANT` |
| `restriction_kind`, `metric_group`, `channel` | `NULL` | Như `permission_restriction` |
| `effective_from`, `effective_to` | | |
| `actor_id` | `BIGINT NULL` | `assigned_by` / `actor_id` cũ; `NULL` chỉ với `ROLE_ASSIGNMENT` khởi tạo (BR-M15-22) |
| `reason` | `VARCHAR(2000)` | `CHECK (CHAR_LENGTH(TRIM(reason)) > 0)` |
| `active_role_key` | Cột sinh `IF(kind='ROLE_ASSIGNMENT' AND effective_to IS NULL, CONCAT(account_id, ':', role), NULL)` `UNIQUE` | Thay `uq_role_assignment_active` |

`CHECK` theo `kind` giữ đủ ràng buộc cũ: đích của thu hẹp (`ROLE` có `role`, `ACCOUNT` có `account_id`), `metric_group`, `channel` chỉ với `DASHBOARD_REPORT`, gán thêm chỉ `CARE_PLAN` `APPROVE`, `effective_to >= effective_from`. Chỉ mục `(kind, account_id, effective_to)`, `(kind, role, effective_to)`. Quyền `eldercare_app`: `INSERT, UPDATE` như ba bảng cũ.

**`device_credential`** (`thiet_bi_xac_thuc`, mới) – nhóm 2. Gộp `login_qr_code`, `bound_device` (cùng tài khoản, cùng một mã băm tra khi đăng nhập):

| Cột | Ghi chú |
| --- | --- |
| `id`, `kind ENUM('LOGIN_QR_CODE','BOUND_DEVICE')`, `account_id` | Giữ id của `bound_device`; mã QR cấp id mới |
| `secret_hash BINARY(32)` `UNIQUE` | `code_hash` hoặc `device_token_hash` |
| `device_id CHAR(36) NULL` | `used_device_id` của mã QR, `device_id` lúc gắn |
| `created_by`, `created_at`, `expires_at NULL` | Mã QR: người tạo, hạn |
| `used_at NULL`, `revoked_at NULL` | Mã QR |
| `login_qr_code_id NULL` | Thiết bị gắn bằng mã nào (khóa ngoại tới chính bảng) |
| `ended_at NULL`, `ended_by NULL`, `end_reason NULL`, `end_note NULL` | `unbound_*` của thiết bị gắn |
| `active_account_id`, `active_device_id` | Cột sinh chỉ với `BOUND_DEVICE` và `ended_at IS NULL`, `UNIQUE` (thay hai `UNIQUE` cũ) |

`CHECK` theo `kind`: mã QR có `expires_at > created_at`, `(used_at IS NULL) = (device_id IS NULL)`; thiết bị gắn có `(ended_at IS NULL) = (end_reason IS NULL)`, ghi chú bắt buộc khi gỡ `MANUAL` (*cài 2026-10-30*: viết `end_reason IS NULL OR end_reason <> 'MANUAL' OR (end_note IS NOT NULL AND …)`; ràng buộc cũ của `bound_device` để ghi chú `NULL` lọt qua vì `CHECK` nhận `NULL`, `ConsolidatedTablesIT` bắt được). Trigger không xóa giữ như cũ.

**Loại ở bảng chung**:

| Bảng chung | `loai` | Đối tượng | Nội dung | `khoa_duy_nhat`, ràng buộc chuyển về backend |
| --- | --- | --- | --- | --- |
| `danh_muc_chung` | `M15.ROLE` | — | `ma` = mã actor; `{accountType, executionScope, permissionVersion}` | `UNIQUE (loai, ma)`; `permissionVersion` chỉ tăng (A-05) |
| `danh_muc_chung` | `M15.FIELD_VISIBILITY` | — | `ma` = `<role>:<infoGroup>`; `{visibility}`; nạp bằng migration, không có API sửa | |
| `danh_muc_chung` | `M15.PROFESSIONAL_ACTIVITY` | — | `ma` = mã; `{functionCode, requiredLicenseType, requiresFacilityScope, isDefault, practiceScopeIds[]}` | `ten_hieu_luc` thay `active_name`; mặc định chỉ khi Hiệu lực (backend) |
| `lich_su_chung` | thuộc tính `TRANG_THAI` (`STATUS`), dòng `POINT` | `TAI_KHOAN` (`M15.ACCOUNT`) | `gia_tri` = trạng thái; `chi_tiet` `{lockCause, auditLogId}` | `UNIQUE (account_id, effective_from)` cũ thành `khoa_duy_nhat` `M15.ASH:<accountId>:<tu>` |
| `ho_so_chung` | `M15.PENDING_REVIEW_RECORD` | `nct_id` `NULL`, không có đối tượng (*cài 2026-10-30*: `recordType` vào nội dung vì `EntityRef` cần cả loại và id) | `{recordType, content, idempotencyKey, recorderId, deviceId, deviceTime, syncedAt, lastConnectedAt, pendingReason, floorAreaId, decidedBy, decidedAt, rejectionReason, resultReference}`; `trang_thai` `PENDING_REVIEW`, `ACCEPTED`, `REJECTED`; `nguoi_lap` = người ghi | `M15.PRR:<recorderId>:<idempotencyKey>`; `khoa_luc` khi quyết định; ba `CHECK` quyết định chuyển về domain; danh sách chờ xem lại đọc bằng `SimpleRecordApi.listByStatus` (chỉ mục `(kind, status)`, `V000_14`; đổi api 000 chủ dự án duyệt 2026-10-30), lọc tầng ở backend nên không thêm cột sinh `m15_floor_area_id` |
| `ho_so_chung` | `M15.PERMISSION_DATA_ERROR` | — | `{source, recordId, errorKind, firstAt, lastAt, occurrences, fixedAt}`; `trang_thai` `OPEN`, `FIXED`; `nguoi_lap` = người dùng của lần kiểm quyền (dòng chép từ bảng cũ: `0`) | `M15.PDE:<source>|<recordId>|<errorKind>` khi `OPEN`, `NULL` khi `FIXED` (thay `open_key`); khắc phục theo bản ghi đọc lỗi `OPEN` bằng `listByStatus` |
| `moc_da_nhac` | `M15.LICENSE_EXPIRY_WARNING` | `FACILITY` / `PRACTITIONER` + id giấy phép | `moc` = ngày hết hạn (`yyyy-MM-dd`) | |

**Chuyển dữ liệu (`V002_95`)**: ba bảng quyền vào `permission_assignment`; hai bảng mã QR, thiết bị gắn vào `device_credential` (đổi `login_qr_code_id` theo id mới); `role`, `field_visibility_rule`, `professional_activity` (kèm `professional_activity_practice_scope`) vào `danh_muc_chung`; `account_status_history` vào `lich_su_chung`; `pending_review_record`, `permission_data_error` vào `ho_so_chung`; `license_expiry_warning` vào `moc_da_nhac`. Ghi `legacy` cho dòng đổi id; `dinh_chinh`, `nhat_ky` trỏ `ROLE_ASSIGNMENT` giữ id. *(Cài 2026-10-30, chủ dự án duyệt)*: loại nhật ký, nguồn thông báo giữ tên (`M15.PROFESSIONAL_ACTIVITY`, `M15.PENDING_REVIEW_RECORD`, `M15.PERMISSION_DATA_ERROR`, `M15.LICENSE_EXPIRY_WARNING`) nên không khai `LegacyRecordTypeSource`; dòng nhật ký, thông báo đã có trỏ id cũ của dòng chuyển sang bảng chung không nối được tới dòng mới (id cũ còn ở `legacy`), chấp nhận vì hệ thống chưa vận hành. Mã `PA-<n>` của nghiệp vụ chuyên môn mới: `n` kế tiếp số lớn nhất đã cấp, tính dưới `TechnicalLocks`. Test chép dữ liệu: `TableConsolidationMigrationIT` (schema tạm, dữ liệu mẫu `demo/V002_90` và một dòng mỗi bảng cũ).

**Bảo đảm** (thay phần tương ứng ở các mục sau): một vai trò hiện hành mỗi tài khoản bằng `active_role_key`; một thiết bị gắn đang dùng mỗi tài khoản và mỗi máy bằng `active_account_id`, `active_device_id`; mã băm không trùng bằng `secret_hash`; bản ghi chờ xem lại không trùng khóa lặp lại và một lỗi dữ liệu quyền đang mở bằng `khoa_duy_nhat`. Test hiện có (`PermissionMatrixMatchesSourceIT`, các IT đăng nhập QR, gắn thiết bị, chờ xem lại, lỗi dữ liệu quyền) phải xanh, giữ kịch bản nghiệp vụ và đổi bảng đích trong test (danh sách cuối mục).

**Bảng truy vết sau gộp** *(bổ sung 2026-10-30; khi làm task gộp, sửa cột "bảo đảm bằng" của bảng DBR, BR ở các mục sau theo bảng này)*:

| Mã | Bảo đảm sau gộp | Test |
| --- | --- | --- |
| BR-M15-16 | Quyền tại thời điểm: `permission_assignment` (khoảng hiệu lực), trạng thái tài khoản ở `lich_su_chung` có `khoa_duy_nhat` | `PermissionsAsOfIT` |
| BR-M15-19 | Phạm vi hành nghề của nghiệp vụ chuyên môn: `danh_muc_chung` loại `M15.PROFESSIONAL_ACTIVITY` (`practiceScopeIds` trỏ loại `M09.PRACTICE_SCOPE`) | `PracticeScopeIT` |
| BR-M06-05 | Điều kiện pháp lý theo ngày: như trên, cùng `GiayPhepApi`, `GiayPhepCoSoApi` | `LegalConditionIT` |
| BR-M15-24 | Một thiết bị gắn đang dùng mỗi tài khoản, mỗi máy: `device_credential.active_account_id`, `active_device_id` `UNIQUE` | `BoundDeviceIT`, `ResidentSessionNoTimeoutIT`, `ConsolidatedTablesIT` |
| FR-024, BR-M15-18 *(cài 2026-10-30)* | Một vai trò hiện hành mỗi tài khoản: `permission_assignment.active_role_key` `UNIQUE`; `CHECK` theo `kind` (gán thêm chỉ `CARE_PLAN` `APPROVE`, đích thu hẹp, `metric_group`, `channel` chỉ với `DASHBOARD_REPORT`) | `ConsolidatedTablesIT`, `RoleAssignmentIT`, `CarePlanApprovalGrantIT` |
| FR-041b, FR-041c *(cài 2026-10-30)* | `khoa_duy_nhat` của `M15.PERMISSION_DATA_ERROR` (đang mở), `M15.PENDING_REVIEW_RECORD` (khóa lặp lại); dòng đã quyết định khóa (`khoa_luc`, trigger của `simple_record`) | `PermissionDataErrorIT`, `OfflinePendingReviewIT` |

**Test phải đổi bảng đích** *(rà 2026-10-30; 40 file đọc, ghi thẳng bảng cũ bằng SQL)*: giữ kịch bản nghiệp vụ, chỉ đổi bảng, cột đích.
- Fixture dùng chung (`support/`, ảnh hưởng mọi module; sửa trong cùng PR, báo cả nhóm ở team-plan mục 8): `TestUsers`.
- Test của module: `AccountEventNotificationsTest`, `AccountFixtures`, `ApproverApiIT`, `AuditLogAtomicityIT`, `AuditLogSchedulerIT`, `BoundDeviceIT`, `CarePlanApprovalGrantIT`, `CurrentUserIT`, `FamilyCannotSeeResidentAccountIT`, `FirstManagerBootstrapIT`, `InactivityIT`, `IncidentOutOfScopeIT`, `LegalConditionIT`, `LoginQrCodeIT`, `LoginTemporaryLockIT`, `ManagerAlwaysExistsIT`, `MeControllerIT`, `MetricGroupRestrictionsIT`, `OfflinePendingReviewIT`, `OnlyManagerChangesPermissionsIT`, `PermissionCommands`, `PermissionDataErrorIT`, `PermissionMatrixIT`, `PermissionMatrixMatchesSourceIT`, `PracticeScopeIT`, `ProfessionalActivityIT`, `RequiresPermissionAspectTest`, `ResidentAccountFinalStatusIT`, `ResidentAccountLockIT`, `ResignationLockIT`, `RestrictWithinMatrixIT`, `RoleAssignmentIT`, `RolePermissionCalculatorIT`, `SourceDataErrorScopeIT`, `SyncResultIT`, `ThreeLayerPermissionIT`.
- Test của staffing (chủ module review): `AssignmentIT`, `M09GovernanceIT`, `StaffStatisticsIT`.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Nội dung | FR |
| --- | --- | --- | --- |
| `tai_khoan` | 1 (vai trò), 2 (trạng thái), theo 3.2 | Tài khoản, trạng thái, mật khẩu | FR-001 → FR-015 |
| `lich_su_trang_thai_tai_khoan` | 3, chỉ thêm | Mỗi lần chuyển trạng thái | FR-015, FR-041a *(R-15: `lich_su_trang_thai_tai_khoan` → `lich_su_chung`)* |
| `phien_dang_nhap` | Kỹ thuật | Phiên, thiết bị | FR-013, FR-014a |
| `thiet_bi` | Kỹ thuật | Lần kết nối gần nhất của thiết bị | FR-041c |
| `thiet_bi_gan` | 2 | *(bổ sung 2026-10-18)* Thiết bị gắn với tài khoản người cao tuổi, token thiết bị | FR-070, FR-071 *(R-15: `thiet_bi_gan` → `thiet_bi_xac_thuc` (`device_credential`))* |
| `ma_qr_dang_nhap` | 2 | *(bổ sung 2026-10-18)* Mã QR đăng nhập dùng một lần | FR-069 *(R-15: `ma_qr_dang_nhap` → `thiet_bi_xac_thuc` (`device_credential`))* |
| `ma_tran_quyen` | 1, cố định | Quyền tối đa theo Phụ lục 27 và 19.3 | FR-018, FR-020 |
| `gan_vai_tro` | 1 có lịch sử | Tài khoản – vai trò | FR-017, FR-026 *(R-15: `gan_vai_tro` → `quyen_tai_khoan` (`permission_assignment`))* |
| `thu_hep_quyen` | 1 có lịch sử | Thu hẹp theo vai trò hoặc tài khoản | FR-025 *(R-15: `thu_hep_quyen` → `quyen_tai_khoan` (`permission_assignment`))* |
| `quyen_gan_them` | 1 có lịch sử | Duyệt kế hoạch chăm sóc cho Điều dưỡng | FR-024 *(R-15: `quyen_gan_them` → `quyen_tai_khoan` (`permission_assignment`))* |
| `nghiep_vu_chuyen_mon`, `nghiep_vu_chuyen_mon_pham_vi` | 1 | Danh mục nghiệp vụ chuyên môn | FR-036, FR-037 *(R-15: `nghiep_vu_chuyen_mon`, `nghiep_vu_chuyen_mon_pham_vi` → `danh_muc_chung`)* |
| `ban_ghi_cho_xem_lai` | 2 | Bản ghi ngoại tuyến thời điểm bất hợp lý | FR-041c *(R-15: `ban_ghi_cho_xem_lai` → `ho_so_chung`)* |

"Cố định" là nạp từ migration, không có API tạo, sửa, xóa; đổi bằng migration khi tài liệu nguồn đổi.

---

## 2. Tài khoản, trạng thái, phiên (mục A, B)

### `tai_khoan`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | Khóa chính | |
| `ten_dang_nhap` | `VARCHAR(64)` | `UNIQUE`; không đổi sau khi tạo | FR-001, CHK024 |
| `loai` | `ENUM('NHAN_VIEN','NGUOI_THAN','NGUOI_CAO_TUOI')` | Không đổi | FR-001 (`NGUOI_CAO_TUOI` bổ sung 2026-10-18, Q-365) |
| `nhan_vien_id` | `BIGINT NULL` | `UNIQUE`; tham chiếu logic hồ sơ nhân viên (008) | FR-002 |
| `nguoi_than_id` | `BIGINT NULL` | `UNIQUE`; tham chiếu logic hồ sơ người thân (012) | FR-002 |
| `nguoi_cao_tuoi_id` | `BIGINT NULL` | `UNIQUE`; tham chiếu logic hồ sơ người cao tuổi (001); bổ sung 2026-10-18 | FR-067 |
| `trang_thai` | `ENUM('HOAT_DONG','KHOA_TAM','KHONG_HOAT_DONG','DA_KHOA')` | | FR-007 |
| `nguyen_nhan_khoa` | `ENUM('THU_CONG','NGHI_VIEC','HET_HAN_SAU_KET_THUC','HET_QUAN_HE','NCT_KET_THUC_LUU_TRU','NCT_TU_YEU_CAU','CO_LY_DO','THEO_DE_NGHI_NGUOI_THAN') NULL` | Có khi `DA_KHOA`; dùng cho FR-011, FR-073 (bốn giá trị cuối bổ sung 2026-10-18) | FR-011 |
| `khoa_tam_den` | `DATETIME(6) NULL` | Có khi `KHOA_TAM` | FR-008 |
| `so_lan_sai_lien_tiep` | `INT` | `>= 0` | FR-008 |
| `mat_khau_bam` | `VARCHAR(255)` | Argon2id | A-03 |
| `buoc_doi_mat_khau` | `BOOLEAN` | | FR-006 |
| `lan_dang_nhap_cuoi` | `DATETIME(6) NULL` | | FR-001, FR-010 |
| `moc_tinh_khong_hoat_dong` | `DATETIME(6)` | Tạo, kích hoạt lại, mở khóa, hoặc đăng nhập, lấy mốc muộn nhất | FR-010, CHK015 |
| `phien_ban` | `INT` | Khóa lạc quan cho lệnh trạng thái | 000 FR-040 |
| `phien_ban_quyen` | `INT` | Tăng khi gán, gỡ vai trò, thu hẹp hay gán thêm theo tài khoản | FR-026, FR-027 |
| `tao_luc`, `tao_boi` | | | |

Ràng buộc:
- `CHECK` mỗi loại có đúng một liên kết: `NHAN_VIEN` → `nhan_vien_id`; `NGUOI_THAN` → `nguoi_than_id`; `NGUOI_CAO_TUOI` → `nguoi_cao_tuoi_id`; hai cột còn lại `NULL` (FR-002, FR-003, FR-067).
- Không có quyền `DELETE` cho `eldercare_app` và có trigger chặn xóa (FR-005), theo mẫu 1.4 của 000.
- Hồ sơ nhân viên không ở Nghỉ việc, có chức danh ứng với vai trò hệ thống (FR-004, FR-004a); hồ sơ người thân có ít nhất một quan hệ với người cao tuổi chưa ở trạng thái cuối (FR-050): kiểm ở backend qua API của 008, 012 khi tạo.
- *(Bổ sung 2026-10-18)* Tài khoản người cao tuổi: người cao tuổi Đang lưu trú qua `NguoiCaoTuoiApi.trangThai` (001); lệnh có `xacNhan` của chính người cao tuổi (A-15).

### Bảng chuyển trạng thái tài khoản

| Từ | Lệnh / sự kiện (endpoint) | Đến | Kiểm tra ở backend | Nhật ký (`hanh_dong`) |
| --- | --- | --- | --- | --- |
| — | `POST /tai-khoan` (nhân viên) | `HOAT_DONG` | Quyền `(TAI_KHOAN_PHAN_QUYEN, TAO)` của AC-01; FR-002 → FR-004a; ít nhất một vai trò AC-01 → 05, 07, 09, 14 | `TK_TAO` |
| — | `POST /tai-khoan-nguoi-than` | `HOAT_DONG` | AC-01 hoặc AC-09 (¹²); FR-050; `cachXacMinh` (FR-052); vai trò duy nhất AC-10 | `TK_TAO` (+`cachXacMinh`) |
| `HOAT_DONG` | đăng nhập sai đủ ngưỡng | `KHOA_TAM` | Ngưỡng CFG-M15-01 | `TK_KHOA_TAM` (Bộ lập lịch, căn cứ CFG-M15-01) |
| `KHOA_TAM` | hết `khoa_tam_den` (tính khi có yêu cầu kế tiếp, A-03) | `HOAT_DONG` | `khoa_tam_den <= bây giờ` | `TK_HET_KHOA_TAM` (Bộ lập lịch) |
| `KHOA_TAM` | `POST …/mo-khoa-som` | `HOAT_DONG` | AC-01; hoặc Trưởng tầng, Người phụ trách ca với tài khoản nhân viên cùng tầng/khu và cùng ca (FR-019c); `lyDo`; xác minh trực tiếp | `TK_MO_KHOA_SOM` (+`canCu`) |
| `HOAT_DONG` | job `ChuyenKhongHoatDong` | `KHONG_HOAT_DONG` | Chỉ người thân; `moc_tinh_khong_hoat_dong` quá CFG-M15-02 | `TK_KHONG_HOAT_DONG` (Bộ lập lịch) |
| `KHONG_HOAT_DONG` | `POST …/kich-hoat-lai` | `HOAT_DONG` | AC-01 hoặc AC-09; `lyDo`; `cachXacMinh`; điều kiện FR-011 với người thân | `TK_KICH_HOAT_LAI` |
| `HOAT_DONG`, `KHOA_TAM`, `KHONG_HOAT_DONG` | `POST …/khoa` | `DA_KHOA` | AC-01; `lyDo`; FR-012; nếu đang là Phụ trách bộ phận thì có `nguoiThayPhuTrach` (gọi 008 trong cùng transaction, Q-292) | `TK_KHOA` |
| `HOAT_DONG`, `KHOA_TAM`, `KHONG_HOAT_DONG` | `TaiKhoanApi.khoaDoNghiViec` (lệnh của 008) | `DA_KHOA` | Cùng transaction với Nghỉ việc | `TK_KHOA` (Bộ lập lịch, căn cứ BR-M09-04, lệnh nguồn) |
| `HOAT_DONG`, `KHOA_TAM`, `KHONG_HOAT_DONG` | job `KhoaSauKetThucLuuTru`; hoặc `TaiKhoanApi.xetLaiNguoiThan` (lệnh của 012) | `DA_KHOA` | Người thân; CFG-M01-04 (5.6) hoặc hết quan hệ Hiệu lực (BR-M15-14) | `TK_KHOA` (Bộ lập lịch, căn cứ) |
| — | `POST /tai-khoan-nguoi-cao-tuoi` *(bổ sung 2026-10-18)* | `HOAT_DONG` | AC-01 hoặc AC-09; FR-067; `xacNhan` (cách, nhân viên nghe); vai trò duy nhất AC-16 | `TK_TAO` (+`xacNhan`) |
| `HOAT_DONG`, `KHOA_TAM` | sự kiện `NguoiCaoTuoiDaChuyenTrangThai` sang trạng thái cuối *(bổ sung 2026-10-18)* | `DA_KHOA` | Tài khoản người cao tuổi; `nguyen_nhan_khoa = NCT_KET_THUC_LUU_TRU`; gỡ thiết bị (A-16) | `TK_KHOA` (Bộ lập lịch, căn cứ sự kiện) |
| `HOAT_DONG`, `KHOA_TAM` | `POST …/khoa-nguoi-cao-tuoi`; hoặc `apDung` của yêu cầu `M15.KHOA_TAI_KHOAN_NCT` *(bổ sung 2026-10-18)* | `DA_KHOA` | AC-01 hoặc AC-09, `canCu`, `lyDo`; yêu cầu: AC-01 duyệt (A-16) | `TK_KHOA` |
| `DA_KHOA` | `POST …/mo-khoa-nguoi-cao-tuoi` *(bổ sung 2026-10-18)* | `HOAT_DONG` | AC-01 hoặc AC-09; `xacNhan`; `nguyen_nhan_khoa <> NCT_KET_THUC_LUU_TRU` (FR-073) | `TK_MO_KHOA` |
| `DA_KHOA` | `POST …/mo-khoa` | `HOAT_DONG` | AC-01; `lyDo`; nguyên nhân khóa tự động không còn (FR-011) | `TK_MO_KHOA` |

Mọi chuyển sang trạng thái khác `HOAT_DONG` kết thúc mọi phiên của tài khoản trong cùng transaction (FR-013). Mọi chuyển thêm một dòng `lich_su_trang_thai_tai_khoan`. Không có trạng thái kết thúc (FR-005). *(R-15: `lich_su_trang_thai_tai_khoan` → `lich_su_chung`)*

Lệnh không chuyển trạng thái nhưng có nhật ký: `POST …/cap-lai-mat-khau` (AC-01 với nhân viên; AC-01, AC-09 với người thân; có `cachXacMinh` với người thân; đặt `buoc_doi_mat_khau`) → `TK_CAP_LAI_MAT_KHAU`; `POST /xac-thuc/doi-mat-khau` → `TK_DOI_MAT_KHAU`; đăng nhập → `DANG_NHAP` / `DANG_NHAP_THAT_BAI`. *(Bổ sung 2026-10-18)* `POST …/ma-qr` → `QR_TAO`; `POST /xac-thuc/dang-nhap-qr` → `QR_DUNG`, `TB_GAN` (và `TB_GO` cho thiết bị cũ); `POST …/go-thiet-bi` → `TB_GO`.

### `lich_su_trang_thai_tai_khoan` *(gộp theo R-15 vào `lich_su_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `phien_dang_nhap` – kỹ thuật

| Cột | Ghi chú |
| --- | --- |
| `ma_bam_token` | `BINARY(32)`, khóa chính (SHA-256 của token) |
| `tai_khoan_id` | |
| `ma_thiet_bi` | `CHAR(36)`; chỉ mục `(ma_thiet_bi, ket_thuc_luc)` |
| `kenh` | `ENUM('WEB','APP_NHAN_VIEN','APP_NGUOI_THAN','APP_NGUOI_CAO_TUOI')` (Q-305; kênh cuối bổ sung 2026-10-18, Q-370) |
| `bat_dau_luc`, `hoat_dong_cuoi_luc`, `ket_thuc_luc NULL`, `ly_do_ket_thuc` | `ly_do_ket_thuc`: `DANG_XUAT`, `TAI_KHOAN_ROI_HOAT_DONG`, `THIET_BI_DANG_NHAP_KHAC`, `HET_THOI_GIAN` (quá CFG-M15-10 của kênh, FR-013a); *(bổ sung 2026-10-18)* `GO_THIET_BI`, `DOI_MAT_KHAU`; kênh `APP_NGUOI_CAO_TUOI` không có `HET_THOI_GIAN` (FR-013a) |

- Đăng nhập: trong một transaction, đặt `ket_thuc_luc` cho mọi phiên đang mở có cùng `ma_thiet_bi` (BR-M15-15), rồi thêm phiên mới.
- Phiên đang mở của một thiết bị: tối đa một; bảo đảm bằng cột sinh `thiet_bi_dang_mo = IF(ket_thuc_luc IS NULL, ma_thiet_bi, NULL)` có `UNIQUE`.
- Bảng kỹ thuật được `UPDATE` (`hoat_dong_cuoi_luc`, `ket_thuc_luc`); thay đổi nghiệp vụ tương ứng đã có nhật ký (đăng nhập, khóa).

### `thiet_bi` – kỹ thuật

| Cột | Ghi chú |
| --- | --- |
| `ma_thiet_bi` | Khóa chính |
| `lan_ket_noi_cuoi` | Cập nhật ở mọi yêu cầu trực tuyến |
| `tai_khoan_cuoi_id` | |

Lần kết nối trước khi ngoại tuyến được app gửi kèm phong bì đồng bộ và đối chiếu với `lan_ket_noi_cuoi` trước khi cập nhật (FR-041c).

### `thiet_bi_gan` *(gộp theo R-15 vào `thiet_bi_xac_thuc` (`device_credential`); mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `ma_qr_dang_nhap` *(gộp theo R-15 vào `thiet_bi_xac_thuc` (`device_credential`); mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

---

## 3. Vai trò và quyền (mục C)

### `vai_tro` *(gộp theo R-15 vào `danh_muc_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `ma_tran_quyen` – nhóm 1, cố định

| Cột | Ghi chú |
| --- | --- |
| `vai_tro`, `chuc_nang`, `loai_thao_tac` | Khóa chính; `chuc_nang` là một dòng của Phụ lục 27 |
| `pham_vi` | `TOAN_VIEN`, `PHAN_CONG`, `QUAN_HE` |
| `ma_chu_thich` | `JSON`, ví dụ `["1"]`, `["2"]`, `["10"]` |
| `nguon` | `PL27` hoặc `19.3` |

Quy đổi ký hiệu theo research A-04. Các dòng `nguon = '19.3'` (điều chỉnh của 19.3 so với Phụ lục 27, 19.3 là căn cứ):

| Điều chỉnh | Căn cứ |
| --- | --- |
| Quản lý viện: `DUYET`, `TAO` (lập thay) với yêu cầu đổi ca, nghỉ đột xuất của ca phạm vi toàn viện | 19.3, Q-177; spec FR-025 |
| Trưởng tầng: `TAO` (lập thay) yêu cầu nghỉ đột xuất cho nhân viên tầng mình | 19.3; spec 015 |
| Điều dưỡng: mọi `TAO` của Nhân viên chăm sóc trong phạm vi (Phụ lục 27 đã ghi bằng ²⁷, ⁴⁰, ¹⁵…; khi nạp không còn ô nào thiếu nên không có dòng `19.3`, plan Ghi chú T007) | 2.4, Q-214, chú thích ²⁷ |
| Bác sĩ, Hành chính (¹¹), Kế toán (²⁹): phạm vi dashboard, báo cáo toàn viện (chú thích của chính Phụ lục 27 ghi "toàn viện", nên nạp như dòng `PL27` phạm vi `TOAN_VIEN`, plan Ghi chú T007) | 19.3, chú thích ¹¹, ²⁹; spec FR-035 |

Danh sách đầy đủ được rà và chốt ở task nạp dữ liệu; test `MaTranQuyenKhopNguonTest` đối chiếu với tài liệu nguồn (research A-04).

### `gan_vai_tro` *(gộp theo R-15 vào `quyen_tai_khoan` (`permission_assignment`); mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `thu_hep_quyen` *(gộp theo R-15 vào `quyen_tai_khoan` (`permission_assignment`); mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `quyen_gan_them` *(gộp theo R-15 vào `quyen_tai_khoan` (`permission_assignment`); mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### Tính quyền vai trò

Thứ tự ở research A-05. Quyền tại thời điểm `t` (FR-026a, BR-M15-16) dùng cùng thuật toán với các dòng có hiệu lực tại `t`.

### `quy_tac_xem_truong` *(gộp theo R-15 vào `danh_muc_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

---

## 4. Điều kiện pháp lý (mục E)

### `nghiep_vu_chuyen_mon` *(gộp theo R-15 vào `danh_muc_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `nghiep_vu_chuyen_mon_pham_vi` *(gộp theo R-15 vào `danh_muc_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `da_canh_bao_giay_phep` *(gộp theo R-15 vào `moc_da_nhac`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

---

## 5. Ngoại tuyến và lỗi dữ liệu nguồn (mục F)

### `ban_ghi_cho_xem_lai` *(gộp theo R-15 vào `ho_so_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `loi_du_lieu_quyen` *(gộp theo R-15 vào `ho_so_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

---

## 6. Nhật ký

Mọi lệnh ghi `nhat_ky` của 000 trong cùng transaction (000 FR-047). Các `hanh_dong` của 002: `TK_TAO`, `TK_KHOA_TAM`, `TK_HET_KHOA_TAM`, `TK_MO_KHOA_SOM`, `TK_KHONG_HOAT_DONG`, `TK_KICH_HOAT_LAI`, `TK_KHOA`, `TK_MO_KHOA`, `TK_CAP_LAI_MAT_KHAU`, `TK_DOI_MAT_KHAU`, `DANG_NHAP`, `DANG_NHAP_THAT_BAI`, `VT_GAN`, `VT_GO`, `QUYEN_THU_HEP`, `QUYEN_KHOI_PHUC`, `QUYEN_GAN_THEM`, `QUYEN_GO_THEM`, `NVCM_TAO`, `NVCM_SUA`, `NVCM_NGUNG`, `NT_CHO_XEM_LAI`, `NT_CHAP_NHAN`, `NT_TU_CHOI`, `QUYEN_TU_CHOI_DU_LIEU_NGUON`; *(bổ sung 2026-10-18)* `QR_TAO`, `QR_DUNG`, `TB_GAN`, `TB_GO`.

- `du_lieu_truoc`, `du_lieu_sau` của thay đổi quyền chứa tập quyền trước và sau của vai trò hoặc tài khoản (FR-026, SC-007).
- `chi_tiet` chứa `cachXacMinh` (FR-052), `canCu` nhiệm vụ (FR-019a → f), `ngoaiPhamVi` (FR-044a).
- Mật khẩu, mật khẩu tạm, token không bao giờ vào nhật ký.

---

## 7. Truy vết BR, DBR và FR

| Mã | Bảo đảm bằng | Nơi kiểm thử |
| --- | --- | --- |
| BR-M15-01 | `QuyenApi.coQuyen` ba lớp, interceptor `@CanQuyen` (research A-10); ArchUnit bắt mọi `@LenhNghiepVu` có `@CanQuyen`, trừ danh sách ngoại lệ có tên (lệnh SOS của 007, A-15) | `QuyenBaLopIT`, `MaTranQuyenIT`, `CanQuyenArchTest` |
| BR-M15-01 (ngoại lệ sự cố) | Bỏ lớp phạm vi với khóa `(SU_CO_KHAN_CAP, TAO)`; nhật ký `ngoaiPhamVi` | `SuCoNgoaiPhamViIT` |
| BR-M15-02 | Phạm vi tính trong từng yêu cầu; khoảng CFG-M15-07 | `PhamViTheoCaIT`, `PhamViDoiPhanCongIT` |
| BR-M15-04 | Khóa `(TAI_KHOAN_PHAN_QUYEN, *)` chỉ AC-01; nhật ký trước/sau | `PhanQuyenChiQuanLyIT` |
| BR-M15-05 | `so_lan_sai_lien_tiep`, `khoa_tam_den`, job `ChuyenKhongHoatDong`; mở khóa sớm theo FR-019c | `DangNhapKhoaTamIT`, `KhongHoatDongIT`, `MoKhoaSomIT` |
| BR-M15-14 | `TaiKhoanApi.xetLaiNguoiThan` trong transaction của 012 | `HetQuanHeKhoaTaiKhoanIT` |
| BR-M15-15 | `UNIQUE` trên `thiet_bi_dang_mo` | `MotThietBiMotPhienIT` |
| BR-M15-16 | Bảng có hiệu lực theo thời điểm (`permission_assignment`); trạng thái tài khoản ở `lich_su_chung` (`M15.ACCOUNT`, `STATUS`) | `QuyenTaiThoiDiemIT` |
| BR-M15-17 | Không có trường ngày hiệu lực trong lệnh; `If-Match` trên `phien_ban_quyen` | `DoiQuyenDongThoiIT` |
| BR-M15-18 | Thứ tự tính ở A-05; lệnh thu hẹp chặn vượt ma trận | `ThuTuThuHepTest`, `ThuHepKhongVuotIT` |
| BR-M15-19 | `practiceScopeIds` của `danh_muc_chung` loại `M15.PROFESSIONAL_ACTIVITY` dùng danh mục của 008 | `PhamViHanhNgheIT` |
| BR-M15-20 | Kết quả từng bản ghi trong phản hồi đồng bộ; hiển thị ở app | `DongBoKetQuaIT`, `ket_qua_dong_bo_test.dart` |
| BR-M15-21 | Kiểm CFG-M15-09 khi đổi mật khẩu; so `hoat_dong_cuoi_luc` với CFG-M15-10 ở mỗi lần tra phiên | `DoManhMatKhauIT`, `TuDangXuatIT` |
| BR-M15-22 | Lệnh `khoi-tao-quan-ly-vien` chỉ chạy khi chưa có AC-01 | `KhoiTaoQuanLyVienIT` |
| BR-M06-05 | `danh_muc_chung` loại `M15.PROFESSIONAL_ACTIVITY` cùng `GiayPhepApi`, `GiayPhepCoSoApi` theo ngày | `DieuKienPhapLyIT` |
| BR-M09-04 | `TaiKhoanApi.khoaDoNghiViec` trong transaction của 008 | `NghiViecKhoaTaiKhoanIT` |
| BR-M01-08, DBR-03 | Điều kiện chú thích ¹ do 001 cài, kiểm mỗi lần xem | `NguoiThanSucKhoeIT` |
| FR-012 | Đếm AC-01 Hoạt động dưới khóa dòng | `LuonConQuanLyVienIT` |
| FR-022 | `danh_muc_chung` loại `M15.FIELD_VISIBILITY`, `BeanSerializerModifier` | `GioiHanTruongIT` |
| FR-023 *(bổ sung 2026-10-21, T082)* | Ma trận (Phụ lục 27) không có ô của AC-02 cho duyệt thay đổi lưu trú, chi phí và kê đơn; thu hẹp không thêm được quyền (FR-025) | `FloorManagerLimitsIT` |
| FR-035a *(bổ sung 2026-10-21, T021)* | `PhamViApi.boLocLichSu` dựng từ lịch sử phân công; ArchUnit cấm gọi từ `@LenhNghiepVu` | `PhamViBaoCaoLichSuIT` (`HistoryReportScopeIT`), `CanQuyenArchTest` |
| FR-041b | `ho_so_chung` loại `M15.PERMISSION_DATA_ERROR` | `DuLieuNguonLoiIT` |
| FR-041c | `ho_so_chung` loại `M15.PENDING_REVIEW_RECORD`, `thiet_bi` | `NgoaiTuyenChoXemLaiIT` |
| BR-M15-23 *(bổ sung 2026-10-18)* | Nghe `NguoiCaoTuoiDaChuyenTrangThai`; lệnh `khoa-nguoi-cao-tuoi`; loại yêu cầu `M15.KHOA_TAI_KHOAN_NCT`; `mo-khoa-nguoi-cao-tuoi` chặn `NCT_KET_THUC_LUU_TRU` | `KhoaTaiKhoanNguoiCaoTuoiIT` |
| BR-M15-24 *(bổ sung 2026-10-18)* | `thiet_bi_xac_thuc` (`device_credential`) loại `BOUND_DEVICE` với hai cột sinh `UNIQUE`; kênh `APP_NGUOI_CAO_TUOI` bỏ CFG-M15-10; job Không hoạt động lọc người thân | `ThietBiGanIT`, `PhienNguoiCaoTuoiKhongHetIT` |
| FR-067, FR-068 | Lệnh tạo kiểm trạng thái và `xacNhan`; phạm vi `BAN_THAN` | `TaoTaiKhoanNguoiCaoTuoiIT` |
| FR-069 | `thiet_bi_xac_thuc` (`device_credential`) loại `LOGIN_QR_CODE`, CFG-M15-11 | `MaQrDangNhapIT` |
| FR-071 | `ThietBiGanApi.xacDinh` theo `ma_bam_token_thiet_bi` | `XacDinhThietBiSosIT` |
| FR-075 | Không có quyền AC-10 ở dòng "Tài khoản người cao tuổi" | `NguoiThanKhongXemTaiKhoanNctIT` |
