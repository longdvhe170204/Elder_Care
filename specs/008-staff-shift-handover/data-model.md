# Data Model: Nhân sự, ca trực và bàn giao (008)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1; không nhắc lại ở từng bảng. Tài khoản, người cao tuổi, tầng/khu vực, phòng, loại công việc, công việc, liều, cảnh báo, sự cố, giường, đồ gửi, chuyến đi, tệp là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Thời điểm lưu `DATETIME(6)` (000 R-04; DBR-25).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp: `nhan_vien`; `nhan_vien_bo_phan` (DBR-36); `chung_chi` (phạm vi thành `JSON`); `lich_ca` (+ cột công bố); `ca`; `ca_nhan_vien`; `phan_cong` (+ `giao_truong_tang`, cột `loai`); `ban_giao` (+ `muc_ban_giao`, `nhan_dinh_ban_giao`, khóa theo dòng khi xác nhận).
- `danh_muc_chung`: `bo_phan`, `loai_chung_chi`, `pham_vi_hanh_nghe`, `mau_ca`.
- `lich_su_chung`: `lich_su_phu_trach_bo_phan`, `nguoi_phu_trach_ca` (kiểu `KHOANG`).
- `su_kien_chung`: `ban_cong_bo_lich`, `y_kien_ban_giao`, `phan_hoi_y_kien`, `lan_tinh_ty_le`.
- `ho_so_chung`: `lan_import_nhan_vien`; `khoa_ky_thuat`: `tang_ca_khoa`; `moc_da_nhac`: `m09_job_run` (lần chạy ra file log).
- Bảng phụ, bảng con *(bổ sung 2026-10-30, rà đủ tên bảng của data-model)*: `lan_chay_m09` → `moc_da_nhac`; `chung_chi_pham_vi` → cột `JSON` của `chung_chi`. Không thêm bảng riêng.
- Còn 8 bảng riêng của feature.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-29; tên mã theo bảng đã cài, migration `V008_95`)*

Mô tả dưới đây thay các mục tương ứng khi cài 008 T050; cột không nhắc lại giữ như bảng đã cài (`V008_01` → `V008_08`).

**Giữ nguyên**: `staff_profile` (bỏ khóa ngoại `import_batch_id` vì lần import chuyển vào `ho_so_chung`), `staff_department` (DBR-36, `open_staff_id`, trigger đóng một lần), `shift_schedule` (`uq_shift_schedule_month`), `work_shift`, `shift_staff` (`uq_shift_staff`) cùng trigger hiện có. `staff_department.department_id`, `work_shift.template_id`, `credential.credential_type_id` trỏ `danh_muc_chung.id` (id mới).

**`credential`** (`chung_chi`) – giữ mọi cột, trigger `trg_credential_protected_fields`. Thêm `practice_scope_ids JSON NOT NULL` (thay `credential_practice_scope`, ghi khi tạo; trigger chặn đổi).

**`staff_assignment`** (`phan_cong`, mở rộng) – nhóm 2. Gộp `floor_manager_assignment`: thêm `kind ENUM('CARE','FLOOR_MANAGER') NOT NULL DEFAULT 'CARE'`, `floor_area_id NULL`, `assigned_by NULL`, `reason NULL`; `shift_id`, `target_type`, `target_id`, `task_type_id`, `role`, `assignment_kind` `NULL` với `FLOOR_MANAGER`. `CHECK` theo `kind`: `FLOOR_MANAGER` có `floor_area_id`, `assigned_by`, `reason`; `CARE` có `shift_id`, `target_*`. Trigger `trg_staff_assignment_update` gộp luật của `trg_floor_manager_update` theo `kind`. Giữ id của `staff_assignment`; giao trưởng tầng cấp id mới.

**`shift_handover`** (`ban_giao`, mở rộng) – nhóm 2 → 3 khi xác nhận (BR-M15-03). Thêm `items JSON NOT NULL` (thay `handover_item`: mảng `{id, itemGroup, sourceType, sourceId, residentId, roomId, description, note, noteAuthor, resolved, section, statusAtFinalization, createdAt}`, `id` cục bộ trong bàn giao) và `assessments JSON NOT NULL` (thay `handover_assessment`: `{assessmentKind, residentId, residentKey, content}`). Trigger `trg_handover_update` mở rộng: hai cột JSON chỉ đổi khi bàn giao còn nháp (thay các trigger thêm, sửa của hai bảng con); không trùng (`assessmentKind`, `residentKey`) kiểm ở domain (thay `uq_handover_assessment`). Đính chính mục bàn giao trỏ `M09.HANDOVER` + id cục bộ của mục.

**Loại ở bảng chung**:

| Bảng chung | `loai` | Nội dung | Ràng buộc chuyển về backend |
| --- | --- | --- | --- |
| `danh_muc_chung` | `M09.DEPARTMENT` | `{leadStaffId}` | `ten_hieu_luc` thay `active_name` |
| `danh_muc_chung` | `M09.CREDENTIAL_TYPE` | `ma` = mã; `{credentialGroup, hasExpiry, requiredForRoles[], hasPracticeScopes}` | |
| `danh_muc_chung` | `M09.PRACTICE_SCOPE` | `ma` = mã | |
| `danh_muc_chung` | `M09.SHIFT_TEMPLATE` | `{startTime, endTime, scopeType, requiresHandover}` | |
| `lich_su_chung` | thuộc tính `NGUOI_PHU_TRACH`, đối tượng `BO_PHAN` | kiểu `KHOANG`; `gia_tri_id` = nhân viên | Một khoảng mở mỗi bộ phận (`khoa_mo`, thay `open_department_id`); `den` đặt một lần (trigger bảng chung) |
| `lich_su_chung` | thuộc tính `NGUOI_PHU_TRACH`, đối tượng `CA` | kiểu `KHOANG`; `gia_tri_id` = nhân viên; `lenh` = căn cứ (`LAP_LICH`, `CHUYEN`, `VANG_CA`, `DOI_CA_015`) | Một khoảng mở mỗi ca (`khoa_mo`, thay `uq_shift_lead_open`) |
| `su_kien_chung` | `M09.SHIFT_PUBLICATION` | đối tượng `SHIFT_SCHEDULE`; `{content}` | Bản công bố không sửa |
| `su_kien_chung` | `M09.HANDOVER_COMMENT`, `M09.HANDOVER_REPLY` | đối tượng `HANDOVER`; `{content, sender}`; phản hồi có `tra_loi_cho_id` | Chỉ thêm khi bàn giao đã hoàn tất (luật của trigger thêm cũ, kiểm ở domain) |
| `su_kien_chung` | `M09.STAFFING_RATIO_CALCULATION` | đối tượng `CA`; `{calculatedAt, event, totalWeight, staffCount, ratio, threshold, meetsThreshold}` | |
| `ho_so_chung` | `M09.STAFF_IMPORT_BATCH` | `{fileName, rowCount}`; khóa khi thêm | |
| `khoa_ky_thuat` | `M09.TANG:<floorAreaId>` | | Thay `floor_scope_lock` |
| `moc_da_nhac` | `M09.JOB.<job>` | `moc` = `scheduled_at` | Thay `m09_job_run` (lần chạy ra `logs/jobs.log`) |

**Chuyển dữ liệu (`V008_95`)**: `department`, `credential_type`, `practice_scope`, `shift_template` vào `danh_muc_chung`, đổi khóa ngoại theo id mới; `credential_practice_scope` vào JSON; `department_lead_history`, `shift_lead_history` vào `lich_su_chung`; `shift_publication`, `handover_comment`, `handover_reply` (đổi `comment_id` thành `tra_loi_cho_id` mới), `staffing_ratio_calculation` vào `su_kien_chung`; `handover_item`, `handover_assessment` vào JSON của `shift_handover`; `floor_manager_assignment` vào `staff_assignment`; `staff_import_batch` vào `ho_so_chung`; `floor_scope_lock` vào `khoa_ky_thuat`; `m09_job_run` vào `moc_da_nhac`. `ShiftCommandPort`, `AssignmentCommandPort`, `DutyApi`, `AssignmentApi` giữ chữ ký (team-plan mục 8 dòng E (015)).

**Bảo đảm** (thay phần tương ứng ở các mục sau): một bộ phận mỗi nhân viên tại một thời điểm (`staff_department` giữ); một người phụ trách mở mỗi bộ phận, mỗi ca (`khoa_mo`); một lịch mỗi (tháng, phạm vi) và một dòng mỗi (ca, nhân viên) (`UNIQUE` giữ); nội dung bàn giao khóa khi xác nhận (trigger `shift_handover`). Test m09 hiện có (gồm `M09RecordProtectionIT`, `PostPublicationPortsIT`) phải xanh, giữ kịch bản nghiệp vụ và đổi bảng đích trong test (danh sách cuối mục); `StaffImportTransportIT` đang lỗi môi trường (team-plan mục 8).

**Bảng truy vết sau gộp** *(bổ sung 2026-10-30; khi làm task gộp, sửa cột "bảo đảm bằng" của bảng DBR, BR ở các mục sau theo bảng này)*:

| Mã | Bảo đảm sau gộp | Test |
| --- | --- | --- |
| DBR-20, BR-M09-07, 08 | Một bàn giao mỗi ca (`UNIQUE (shift_id)` giữ); nội dung khóa khi xác nhận (trigger `shift_handover` mở rộng cho `items`, `assessments`) | `HandoverIT`, `M09RecordProtectionIT` |
| DBR-23 | Bản công bố lịch không sửa: `su_kien_chung` loại `M09.SHIFT_PUBLICATION` | `PostPublicationShiftIT` |
| BR-M09-15 | Cắt quyền đúng mốc người phụ trách: `lich_su_chung` thuộc tính `NGUOI_PHU_TRACH`, `khoa_mo` | `DepartmentDutyIT`, `PostPublicationPortsIT` |

**Test phải đổi bảng đích** *(rà 2026-10-30; 33 file đọc, ghi thẳng bảng cũ bằng SQL)*: giữ kịch bản nghiệp vụ, chỉ đổi bảng, cột đích.
- Test của module: `AlertStaffPortIT`, `AssignmentGovernanceIT`, `AssignmentHttpIT`, `AssignmentIT`, `AssignmentSchemaIT`, `CredentialLifecycleIT`, `DepartmentCommandsIT`, `DepartmentDutyIT`, `HandoverIT`, `HandoverSchemaIT`, `M09GovernanceIT`, `M09JobCatchUpIT`, `M09RecordProtectionIT`, `PostPublicationPortsIT`, `PostPublicationShiftIT`, `ShiftGovernanceIT`, `ShiftHttpIT`, `ShiftLifecycleIT`, `ShiftQueriesIT`, `ShiftScheduleIT`, `ShiftSchemaIT`, `StaffCommandsHttpIT`, `StaffEmploymentIT`, `StaffModuleIntegrationIT`, `StaffProfileHttpIT`, `StaffSchemaIT`, `StaffSpiIT`, `StaffStatisticsIT`, `StaffStatisticsReportFieldsIT`, `StaffingRatioHttpIT`, `StaffingRatioSchemaIT`, `StaffingRatioWarningIT`.
- Test của m14 (chủ module review): `FR_020_StaffingDashboardIT`.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `nhan_vien` | 1 (mô tả) + 2 (trạng thái) | A (FR-001 → FR-004) |
| `bo_phan`, `nhan_vien_bo_phan` | 1; lịch sử | A (FR-001a) |
| `lich_su_phu_trach_bo_phan` | 3 | A (FR-001a) *(R-15: `lich_su_phu_trach_bo_phan` → `lich_su_chung`)* |
| `loai_chung_chi`, `pham_vi_hanh_nghe` | 1 | A (FR-005) *(R-15: `loai_chung_chi`, `pham_vi_hanh_nghe` → `danh_muc_chung`)* |
| `chung_chi`, `chung_chi_pham_vi` | 1 có lịch sử | A (FR-006 → FR-010) *(R-15: `chung_chi_pham_vi` → cột `JSON` của `chung_chi`)* |
| `mau_ca` | 1 | B (FR-011) *(R-15: `mau_ca` → `danh_muc_chung`)* |
| `lich_ca`, `ca`, `ca_nhan_vien` | 2 | B (FR-012 → FR-023) |
| `nguoi_phu_trach_ca` | 3 (lịch sử) | B (FR-019, FR-037a) *(R-15: `nguoi_phu_trach_ca` → `lich_su_chung`)* |
| `ban_cong_bo_lich` | 3 | B (FR-055) *(R-15: `ban_cong_bo_lich` → `su_kien_chung`)* |
| `tang_ca_khoa` | Kỹ thuật | C (khóa theo tầng, N-03) *(R-15: `tang_ca_khoa` → `khoa_ky_thuat`)* |
| `giao_truong_tang`, `phan_cong` | 2 | C (FR-024 → FR-032) *(R-15: `giao_truong_tang` → `phan_cong` (`staff_assignment`))* |
| `lan_tinh_ty_le` | 3 | D (FR-037) *(R-15: `lan_tinh_ty_le` → `su_kien_chung`)* |
| `ban_giao` | 2 (trạng thái) | E (FR-038 → FR-047) |
| `muc_ban_giao`, `nhan_dinh_ban_giao` | 3 có bản nháp | E (FR-039 → FR-041) *(R-15: `muc_ban_giao`, `nhan_dinh_ban_giao` → cột `JSON` của `ban_giao`)* |
| `y_kien_ban_giao`, `phan_hoi_y_kien` | 3 | E (FR-042) *(R-15: `y_kien_ban_giao`, `phan_hoi_y_kien` → `su_kien_chung`)* |
| `lan_chay_m09` | Kỹ thuật | Job (N-13) *(R-15: `lan_chay_m09` → `moc_da_nhac`)* |

---

## 2. Nhân viên và bộ phận (mục A)

### `nhan_vien` – nhóm 1 (mô tả) + nhóm 2 (trạng thái)

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `ho_ten`, `chuc_danh`, `chuyen_mon`, `ngay_bat_dau` | | | FR-001 |
| `dien_thoai`, `email`, `dia_chi` | | `@NhomTruong(HAN_CHE_NHAN_VIEN)` | FR-052 |
| `tai_khoan_id` | `BIGINT NULL UNIQUE` | Tham chiếu 002; rỗng với nhân viên không dùng hệ thống (Q-282) | FR-001, FR-002 |
| `dien_thoai_chuan` *(bổ sung 2026-10-22)* | `VARCHAR(15) NULL` | Chỉ chữ số, service ghi khi tạo, sửa; chỉ mục (không `UNIQUE`, plan điểm báo lại 7) | FR-001b |
| `nguon` *(bổ sung 2026-10-22)* | `ENUM('TAO_TAY','IMPORT')` | Mặc định `TAO_TAY` | FR-001b |
| `lan_import_id` *(bổ sung 2026-10-22)* | `BIGINT NULL` | Khóa ngoại `lan_import_nhan_vien` | FR-001b *(R-15: `lan_import_nhan_vien` → `ho_so_chung`)* |
| `trang_thai` | `ENUM('DANG_LAM_VIEC','NGHI_VIEC')` | Chỉ đổi qua lệnh (trigger) | FR-003 |
| `nghi_viec_hieu_luc`, `ly_do_nghi_viec`, `da_ap_nghi_viec_luc` | | Lý do mang nhóm hạn chế | FR-003, FR-004, FR-053 |
| `phien_ban` | | | |

Trigger `BEFORE DELETE` chặn mọi xóa (FR-001). `BEFORE UPDATE` chặn đổi `trang_thai` khi biến phiên `@lenh_trang_thai_nv` không đặt.

**Bảng chuyển** (khớp FR-003):

| Từ | Lệnh | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Tạo hồ sơ | `DANG_LAM_VIEC` | Quản lý viện | Họ tên, chức danh |
| — | Import (xác nhận) *(bổ sung 2026-10-22)* | `DANG_LAM_VIEC`, `nguon = IMPORT` | Quản lý viện | FR-001b: mọi dòng hợp lệ, không trùng số điện thoại; hoặc toàn bộ, hoặc không |
| `DANG_LAM_VIEC` | Cho nghỉ việc | `NGHI_VIEC` (tại `nghi_viec_hieu_luc`) | Quản lý viện | Lý do; hiệu lực ≥ bây giờ; Phụ trách bộ phận cần người thay |
| `NGHI_VIEC` | Nhận lại làm việc | `DANG_LAM_VIEC` | Quản lý viện | Lý do |

### `lan_import_nhan_vien` – nhóm 3 *(bổ sung 2026-10-22, Q-382; research N-16)* → gộp theo R-15: `lan_import_nhan_vien` → `ho_so_chung`

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `nguoi_thuc_hien_id`, `thoi_diem` | | | FR-001c |
| `ten_file` | `VARCHAR(255)` | Không lưu nội dung file | FR-001c |
| `so_dong` | `INT` | `<=` CFG-M09-13 lúc thực hiện | FR-001b |

Danh sách hồ sơ đã tạo là các `nhan_vien` có `lan_import_id` này (và nằm trong nhật ký `NV_IMPORT`). Không `UPDATE`, `DELETE`.

### `bo_phan` – nhóm 1 → gộp theo R-15: `bo_phan` → `danh_muc_chung`

`id`, `ten`, `phu_trach_id NULL` (nhân viên), `trang_thai_hieu_luc`, `phien_ban`. Khởi tạo: Y tế, Chăm sóc, Bếp, Hành chính – Kế toán (Q-284).

### `nhan_vien_bo_phan` – lịch sử

`nhan_vien_id`, `bo_phan_id`, `tu`, `den NULL`, `nguoi_thuc_hien_id`, `ly_do`. Cột sinh `khoa_hien_hanh = IF(den IS NULL, nhan_vien_id, NULL)` `UNIQUE` (DBR-36: một bộ phận tại một thời điểm). Không có dòng cho tài khoản Quản lý viện (Q-299, backend kiểm).

Triển khai Phase 9: bảng thật `staff_department`; migration V008_08 bổ sung trigger chỉ đóng `ends_at` một lần, không đổi nhân viên/bộ phận/thời điểm đầu/người ghi/lý do, không sửa bản đã đóng. Xóa chỉ qua tài khoản lưu giữ khi đủ điều kiện; quyền ứng dụng chỉ INSERT và UPDATE(ends_at). M09RecordProtectionIT kiểm cả quản trị, DepartmentCommandsIT kiểm lệnh chuyển bộ phận vẫn hợp lệ.

### `lich_su_phu_trach_bo_phan` – nhóm 3 → gộp theo R-15: `lich_su_phu_trach_bo_phan` → `lich_su_chung`

`bo_phan_id`, `nhan_vien_id`, `tu`, `den NULL`, `nguoi_chi_dinh_id`, `ly_do`. Một dòng mở mỗi bộ phận: cột sinh `khoa_mo = IF(den IS NULL, bo_phan_id, NULL)` `UNIQUE`; dòng chỉ được đặt `den` một lần.

---

## 3. Giấy phép, đào tạo (mục A)

### `loai_chung_chi` – nhóm 1 → gộp theo R-15: `loai_chung_chi` → `danh_muc_chung`

`id`, `ten`, `nhom` (`GIAY_PHEP_HANH_NGHE`, `DAO_TAO`), `co_thoi_han`, `bat_buoc_voi_vai_tro` `JSON` (tập `VaiTro`), `co_pham_vi`, `trang_thai_hieu_luc`.

### `pham_vi_hanh_nghe` – nhóm 1 → gộp theo R-15: `pham_vi_hanh_nghe` → `danh_muc_chung`

`ma`, `ten`, `trang_thai_hieu_luc`. Dùng chung cho 002, 007 qua `DanhMucPhamViHanhNgheApi`.

### `chung_chi` – nhóm 1 có lịch sử

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nhan_vien_id`, `loai_id`, `so`, `don_vi_cap` | `so` nhóm hạn chế | FR-006, FR-052 |
| `ngay_cap`, `ngay_het_han NULL` | `ngay_het_han` bắt buộc khi loại có thời hạn | FR-006 |
| `thay_the_cho NULL` | Gia hạn; bản bị trỏ là "Đã thay thế" | FR-006 |
| `thu_hoi_tu NULL`, `ly_do_thu_hoi` | | FR-006 |
| `tep_scan_id` | Nhóm hạn chế | FR-052 |
| `da_canh_bao_het_han_luc`, `da_ap_het_hieu_luc_luc` | Job | FR-008, FR-009 |

Chỉ mục `(nhan_vien_id, loai_id)`. Trigger chặn `DELETE`; `UPDATE` chỉ cho các cột thu hồi, cột job và cột sửa sai đi kèm biến phiên `@sua_sai` (FR-006). Tình trạng hiệu lực không lưu: hàm `HieuLucChungChi` (research N-05).

### `chung_chi_pham_vi` → gộp theo R-15: `chung_chi_pham_vi` → cột `JSON` của `chung_chi`

`chung_chi_id`, `ma_pham_vi`. Khóa chính hai cột.

---

## 4. Mẫu ca, lịch ca, ca (mục B)

### `mau_ca` – nhóm 1 → gộp theo R-15: `mau_ca` → `danh_muc_chung`

`id`, `ten`, `gio_bat_dau`, `gio_ket_thuc` (`TIME`; ≤ bắt đầu là qua ngày, bằng nhau là 24 giờ), `loai_pham_vi` (`TANG`, `KHU_VUC`, `TOAN_VIEN`), `yeu_cau_ban_giao` (mặc định theo loại phạm vi), `trang_thai_hieu_luc`. Sửa không ảnh hưởng ca đã tạo (ca chép giờ).

### `lich_ca` – nhóm 2

`id`, `thang` (`yyyy-MM`), `loai_pham_vi`, `pham_vi_id NULL` (rỗng với toàn viện), `trang_thai` (`NHAP`, `DA_CONG_BO`), `cong_bo_luc`, `nguoi_cong_bo_id`, `tu_duyet`, `ly_do_cong_bo`, `phien_ban`. `UNIQUE (thang, loai_pham_vi, pham_vi_id)`.

### `ca` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `lich_ca_id`, `mau_ca_id` | | FR-012 |
| `bat_dau`, `ket_thuc` | `CHECK (ket_thuc > bat_dau AND ket_thuc <= bat_dau + 24h)` (backend; MySQL `CHECK` trên biểu thức thời gian) | FR-011 |
| `loai_pham_vi`, `pham_vi_id`, `yeu_cau_ban_giao` | Chép từ lịch, mẫu | FR-011 |
| `trang_thai` | `ENUM('NHAP','DA_CONG_BO','DANG_DIEN_RA','CHO_BAN_GIAO','DA_DONG','DA_HUY')` | FR-022 |
| `nguoi_phu_trach_id`, `thieu_nguoi_phu_trach` | Hiện hành; lịch sử ở `nguoi_phu_trach_ca` | FR-015, FR-019 *(R-15: `nguoi_phu_trach_ca` → `lich_su_chung`)* |
| `dang_duoi_nguong`, `da_bao_qlv_duoi_nguong_luc` | Chống cảnh báo trùng | FR-036 |
| `da_nhac_lap_ban_giao_luc`, `da_nhac_het_ca_luc`, `da_nhac_chua_xac_nhan_luc`, `da_bao_chua_phu_trach_nct_luc` | Job | FR-039, FR-043, FR-029 |
| `ly_do_huy`, `phien_ban` | | FR-021 |

Chỉ mục `(loai_pham_vi, pham_vi_id, bat_dau)`, `(trang_thai, bat_dau)`, `(trang_thai, ket_thuc)`.

**Bảng chuyển** (khớp FR-022):

| Từ | Sự kiện / lệnh | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Tạo ca trong lịch Nháp | `NHAP` | TT, QL | Lịch Nháp |
| `NHAP` | Công bố lịch | `DA_CONG_BO` | TT, QL | FR-016 |
| `DA_CONG_BO` | Tới `bat_dau` | `DANG_DIEN_RA` | Hệ thống | — |
| `DA_CONG_BO` | Hủy ca | `DA_HUY` | QL | Chưa bắt đầu; lý do |
| `DANG_DIEN_RA` | Tới `ket_thuc` | `DA_DONG` | Hệ thống | Không yêu cầu bàn giao, hoặc bàn giao `DA_LAP`, `DA_XAC_NHAN` |
| `DANG_DIEN_RA` | Tới `ket_thuc` | `CHO_BAN_GIAO` | Hệ thống | Bàn giao chưa có, `BAN_NHAP`, `CO_Y_KIEN` |
| `CHO_BAN_GIAO` | Bàn giao chuyển `DA_LAP` | `DA_DONG` | Hệ thống | — |

Trigger: chặn mọi đổi khi `OLD.trang_thai IN ('DA_DONG','DA_HUY')` trừ cột nhắc; chặn đổi `bat_dau`, `ket_thuc` khi khác `NHAP` (FR-017).

### `ca_nhan_vien` – nhóm 2

`id`, `ca_id`, `nhan_vien_id`, `bac_si_truc`, `vang_luc`, `ly_do_vang` (`@NhomTruong(LY_DO_NHAN_SU)`), `nguoi_ghi_vang_id`, `rut_do_nghi_viec_luc`, `bo_sung_luc`, `ly_do_bo_sung`, `nghi_co_duyet_yeu_cau_id` (015), `can_cu_vao_ca` (`LAP_LICH`, `BO_SUNG`, `DOI_CA`, `NHAN_THAY`, `LOI_MOI`), `can_cu_roi_ca`, `roi_ca_luc`. `UNIQUE (ca_id, nhan_vien_id)`. Ở lịch Nháp: xóa dòng được; sau công bố: chỉ đặt cột (trigger chặn `DELETE` khi ca khác `NHAP`). Chồng giờ (DBR-21) kiểm ở backend dưới khóa `nhan_vien` (N-03); chỉ mục `(nhan_vien_id)`.

"Có tên trong ca tại t" = dòng tồn tại, `vang_luc`, `rut_do_nghi_viec_luc`, `roi_ca_luc` không có hoặc sau `t`, không nghỉ có duyệt.

### `nguoi_phu_trach_ca` – nhóm 3 (lịch sử) → gộp theo R-15: `nguoi_phu_trach_ca` → `lich_su_chung`

`ca_id`, `nhan_vien_id`, `tu`, `den NULL`, `can_cu` (`LAP_LICH`, `CHUYEN`, `VANG_CA`, `DOI_CA_015`), `nguoi_thuc_hien_id`, `ly_do`. Một dòng mở mỗi ca: cột sinh `khoa_mo = IF(den IS NULL, ca_id, NULL)` `UNIQUE` (FR-015). Chỉ được đặt `den` một lần.

### `ban_cong_bo_lich` – nhóm 3 → gộp theo R-15: `ban_cong_bo_lich` → `su_kien_chung`

`lich_ca_id`, `cong_bo_luc`, `noi_dung` `JSON` (ca, nhân viên, người phụ trách, phân công lúc công bố). FR-055 "bản gốc có nhật ký".

---

## 5. Giao trưởng tầng, phân công (mục C)

### `tang_ca_khoa` – kỹ thuật → gộp theo R-15: `tang_ca_khoa` → `khoa_ky_thuat`

`tang_khu_vuc_id` khóa chính. Đích khóa cho lệnh phân công, giao trưởng tầng (N-03).

### `giao_truong_tang` – nhóm 2 → gộp theo R-15: `giao_truong_tang` → `phan_cong` (`staff_assignment`)

`id`, `nhan_vien_id`, `tang_khu_vuc_id`, `tu`, `den NULL`, `trang_thai` (`HIEU_LUC`, `DA_KET_THUC`), `nguoi_giao_id`, `ly_do`, `phien_ban`. Không chồng thời gian trên cùng tầng (FR-024): kiểm ở backend dưới khóa `tang_ca_khoa`. Rút ngắn đặt `den` sớm hơn (không lùi trước bây giờ). *(R-15: `tang_ca_khoa` → `khoa_ky_thuat`)*

### `phan_cong` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `ca_id`, `nhan_vien_id` | | FR-025 |
| `loai_doi_tuong` (`TANG`, `KHU_VUC`, `PHONG`, `NGUOI_CAO_TUOI`), `doi_tuong_id` | Không có "nhóm" (Q-92) | FR-025 |
| `loai_cong_viec_id NULL` | Giới hạn | FR-025, FR-028 |
| `vai_tro` (`VaiTro`), `chinh_ho_tro` | | FR-028, FR-028a |
| `dieu_duong_phu_trach` | Nhiệm vụ đặc biệt | FR-025, FR-030 |
| `bat_dau`, `ket_thuc`, `ket_thuc_thuc_te NULL` | Mặc định giờ ca | FR-025, FR-037a |
| `trang_thai` | `ENUM('NHAP','TUONG_LAI','HIEU_LUC','DA_KET_THUC','DA_HUY')` | FR-027 |
| `ly_do_ket_thuc`, `nguon` (`LAP_TAY`, `SAO_CHEP`, `DOI_CA_015`) | | FR-027, FR-032 |
| `phien_ban` | | |

Chỉ mục `(ca_id, trang_thai)`, `(nhan_vien_id, bat_dau)`, `(loai_doi_tuong, doi_tuong_id, bat_dau)`. FR-028 (không hai phân công chính cùng mức, cùng vai trò, cùng giới hạn, cùng đối tượng, chồng thời gian) kiểm ở backend dưới khóa `tang_ca_khoa`. Trigger chặn mọi đổi khi `OLD.trang_thai IN ('DA_KET_THUC','DA_HUY')`; ở `NHAP` được `DELETE` (lịch Nháp, FR-025), trạng thái khác thì không. *(R-15: `tang_ca_khoa` → `khoa_ky_thuat`)*

**Bảng chuyển** (khớp FR-027):

| Từ | Sự kiện / lệnh | Đến | Người |
| --- | --- | --- | --- |
| — | Lập trên ca Nháp | `NHAP` | TT, QL (kể cả sao chép) |
| `NHAP` | Công bố lịch | `TUONG_LAI` / `HIEU_LUC` | TT, QL |
| — | Lập trên ca đã công bố | `TUONG_LAI` / `HIEU_LUC` | TT, QL (kể cả sao chép) |
| `TUONG_LAI` | Tới `bat_dau` | `HIEU_LUC` | Hệ thống |
| `TUONG_LAI` | Hủy phân công | `DA_HUY` | TT, QL, Hệ thống (FR-004, FR-009, FR-020, FR-021, 015) |
| `HIEU_LUC` | Kết thúc phân công | `DA_KET_THUC` | TT, Người phụ trách ca, QL, Hệ thống |
| `HIEU_LUC` | Tới `ket_thuc` | `DA_KET_THUC` | Hệ thống |

---

## 6. Tỷ lệ phục vụ (mục D)

### `lan_tinh_ty_le` – nhóm 3 → gộp theo R-15: `lan_tinh_ty_le` → `su_kien_chung`

`id`, `ca_id`, `thoi_diem`, `su_kien` (`LUU_NHAP`, `CONG_BO`, `VANG_CA`, `BO_SUNG`, `NGHI_VIEC`, `DOI_CA_015`, `MUC_CHAM_SOC`, `PHAN_BO`, `VANG_MAT_NCT`, `TRANG_THAI_CUOI`), `tong_trong_so` `DECIMAL(8,2)`, `so_nhan_vien`, `ty_le` `DECIMAL(8,2) NULL` (rỗng khi không có nhân viên), `nguong`, `dat`. Chỉ mục `(ca_id, thoi_diem)`.

---

## 7. Bàn giao (mục E)

### `ban_giao` – nhóm 2 (trạng thái); nội dung ở bảng con

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `ca_id` `UNIQUE` | Một bàn giao mỗi ca | DBR-20 |
| `ca_sau_id NULL` | `XacDinhCaSau`; rỗng thì người xác nhận là Trưởng tầng | FR-038 |
| `nguoi_ban_giao_id` | Đổi khi chuyển người phụ trách ca ở Bản nháp, Có ý kiến | FR-019 |
| `trang_thai` | `ENUM('BAN_NHAP','DA_LAP','CO_Y_KIEN','DA_XAC_NHAN')` | FR-042 |
| `lap_luc`, `chot_luc`, `xac_nhan_luc`, `nguoi_xac_nhan_id`, `can_cu_xac_nhan` | | FR-041, FR-044 |
| `phien_ban` | Cho sửa mục ở Bản nháp (`If-Match`) | |

Trigger: chặn mọi đổi khi `OLD.trang_thai = 'DA_XAC_NHAN'`; chặn đổi `chot_luc` khi đã có.

**Bảng chuyển** (khớp FR-042):

| Từ | Lệnh | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Lập | `BAN_NHAP` | Người phụ trách ca; TT (FR-041) | Từ mốc FR-039; ca có yêu cầu bàn giao |
| `BAN_NHAP` | Hoàn tất | `DA_LAP` | Người phụ trách ca; TT | Nhận định chung; mục nghiêm trọng có ghi chú |
| `DA_LAP` | Xác nhận | `DA_XAC_NHAN` | Người phụ trách ca sau; TT | Trong phạm vi ca sau |
| `DA_LAP` | Gửi ý kiến | `CO_Y_KIEN` | Người phụ trách ca sau; TT | Nội dung |
| `CO_Y_KIEN` | Bổ sung và gửi lại | `DA_LAP` | Người bàn giao; TT | Phản hồi từng ý kiến |

### `muc_ban_giao` – nhóm 3 có bản nháp → gộp theo R-15: `muc_ban_giao` → cột `JSON` của `ban_giao`

`id`, `ban_giao_id`, `nhom` (`CONG_VIEC`, `LIEU`, `CHI_SO`, `CANH_BAO`, `SU_CO`, `BIEN_DONG_NCT`, `GIUONG_CHO_SAN_SANG`, `DO_GUI`, `VAN_DE_THEO_DOI`), `nguon_loai`, `nguon_id` (chuỗi, ví dụ `lieuKey`), `nct_id NULL`, `phong_id NULL` (một trong hai bắt buộc với `VAN_DE_THEO_DOI`, FR-051), `mo_ta`, `ghi_chu`, `nguoi_ghi_chu_id`, `trang_thai_nguon_luc_chot`, `phan` (`DA_CHOT`, `PHAT_SINH_SAU_KHI_LAP`, `DA_XU_LY_TRONG_CA`), `them_boi_id`, `them_luc`. Không có cột "tự lập".

Trigger: khi `ban_giao.chot_luc IS NOT NULL`, dòng `DA_CHOT`, `DA_XU_LY_TRONG_CA` không `UPDATE`, `DELETE`; dòng `PHAT_SINH_SAU_KHI_LAP` chỉ thêm; khi `DA_XAC_NHAN` mọi dòng khóa. Ở Bản nháp: xóa dòng nhóm `CHI_SO`, `BIEN_DONG_NCT` bị chặn (Q-88).

### `nhan_dinh_ban_giao` – nhóm 3 có bản nháp → gộp theo R-15: `nhan_dinh_ban_giao` → cột `JSON` của `ban_giao`

`ban_giao_id`, `loai` (`CHUNG`, `THEO_NGUOI_CAO_TUOI`), `nct_id NULL`, `noi_dung`, `nguoi_viet_id`. Cùng trigger khóa sau `chot_luc`.

### `y_kien_ban_giao`, `phan_hoi_y_kien` – nhóm 3 → gộp theo R-15: `y_kien_ban_giao`, `phan_hoi_y_kien` → `su_kien_chung`

`y_kien_ban_giao`: `ban_giao_id`, `nguoi_gui_id`, `noi_dung`, `thoi_diem`. `phan_hoi_y_kien`: `y_kien_id`, `nguoi_viet_id`, `noi_dung`, `thoi_diem`. Chỉ thêm. *(R-15: `y_kien_ban_giao`, `phan_hoi_y_kien` → `su_kien_chung`)*

---

## 8. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bất biến | Cách bảo đảm | Nguồn |
| --- | --- | --- |
| Mỗi ca có yêu cầu bàn giao có đúng một bàn giao; bàn giao đã xác nhận không sửa | `UNIQUE (ca_id)`; trigger `ban_giao`, `muc_ban_giao`, `nhan_dinh_ban_giao`; ca không `DA_DONG` khi chưa lập xong (job, N-06) | DBR-20, BR-M09-07, 08 *(R-15: `muc_ban_giao`, `nhan_dinh_ban_giao` → cột `JSON` của `ban_giao`)* |
| Không phân công nhân viên thiếu chứng chỉ; không hai ca chồng giờ | Backend dưới khóa (`HieuLucChungChi`, chồng giờ); test đồng thời | DBR-21, BR-M09-01, 03 |
| Một người phụ trách ca mỗi thời điểm | `UNIQUE khoa_mo` của `nguoi_phu_trach_ca` | FR-015 *(R-15: `nguoi_phu_trach_ca` → `lich_su_chung`)* |
| Một nhân viên chính mỗi (người, vai trò, thời điểm) | Backend dưới khóa `tang_ca_khoa` | FR-028, SC-011 *(R-15: `tang_ca_khoa` → `khoa_ky_thuat`)* |
| Một Trưởng tầng được giao mỗi tầng mỗi thời điểm | Backend dưới khóa `tang_ca_khoa` | FR-024, SC-011 *(R-15: `tang_ca_khoa` → `khoa_ky_thuat`)* |
| Mỗi tài khoản nhân viên thuộc đúng một bộ phận; mỗi bộ phận một Phụ trách | `UNIQUE khoa_hien_hanh`; `UNIQUE khoa_mo` | DBR-36, FR-001a |
| Hồ sơ nhân viên, chứng chỉ không xóa | Trigger `BEFORE DELETE` | FR-001, FR-006 |
| Mọi thay đổi có nhật ký | `NhatKyApi.ghi` trong transaction; công bố lưu `ban_cong_bo_lich` | DBR-23, FR-055 *(R-15: `ban_cong_bo_lich` → `su_kien_chung`)* |
| Thời điểm theo Asia/Ho_Chi_Minh | `Clock`; hiệu lực chứng chỉ tới hết ngày theo `ThoiGianNghiepVu` | DBR-25, NFR-09 |

---

## 9. Nhật ký và phân loại trường

- Loại đối tượng nhật ký: `M09.NHAN_VIEN`, `M09.BO_PHAN`, `M09.CHUNG_CHI`, `M09.MAU_CA`, `M09.LICH_CA`, `M09.CA`, `M09.GIAO_TRUONG_TANG`, `M09.PHAN_CONG`, `M09.BAN_GIAO`.
- Chi tiết nhật ký: `canCuQuyen` (`VAI_TRO`, `NHIEM_VU_NGUOI_PHU_TRACH_CA`, `THAY_TRUONG_TANG`, `TAM_NHAN`, `LOI_MOI`), `tuDuyet`, `suKienKichHoat` (thay đổi do hệ thống).
- `PhanLoaiTruongProvider`: liên hệ, số giấy phép, tệp scan, lý do nghỉ việc, thu hồi, sửa sai → `HAN_CHE_NHAN_VIEN`; lý do vắng ca → `LY_DO_NHAN_SU`; ghi chú, nhận định, mô tả mục, ý kiến, phản hồi → `SUC_KHOE`.
- Loại đính chính: `M09.BAN_GIAO` (mục, nhận định, ý kiến, phản hồi); `tangKhuVuc` = phạm vi ca.

---

## 10. Lưu giữ (000 R-11)

| Loại dữ liệu | Hạn | Chuỗi tham chiếu |
| --- | --- | --- |
| Bàn giao và bảng con | `THEO_HO_SO` với tập người cao tuổi có mục; giữ tới khi mọi người hết hạn | Cả bàn giao một chuỗi (FR-047, FR-057) |
| Hồ sơ nhân viên, bộ phận, chứng chỉ, mẫu ca, lịch ca, ca, nhân viên trong ca, người phụ trách ca, giao trưởng tầng, phân công, lần tính tỷ lệ, bản công bố | `KHONG_GAN_HO_SO` (CFG-M15-04) | Giữ khi còn bản ghi tham chiếu (công việc, liều, bàn giao) |

---

### Ghi chú triển khai Phase 4 (2026-10-24)

- Tên bảng thật theo glossary 14.9: `floor_scope_lock`, `floor_manager_assignment`, `staff_assignment`, chỉ khóa ngoại nội bộ M09. Trạng thái cuối được bảo vệ ở trigger; chỉ tài khoản lưu giữ được loại bỏ bản ghi cuối theo provider. *(R-15: `floor_scope_lock` → `khoa_ky_thuat`; `floor_manager_assignment` → `phan_cong` (`staff_assignment`))*
- `work_shift.publication_assignments_notified_at` là cờ kỹ thuật cho mốc công bố, tách khỏi `unassigned_residents_notified_at` của mốc đầu ca. Nguồn B/C chưa có thì cờ còn null để thử lại, không lưu kết quả người cao tuổi rỗng thay dữ liệu chưa lấy được.
- Provider `M09.ASSIGNMENT`, `M09.FLOOR_MANAGER_ASSIGNMENT` cùng phạm vi đăng ký khi tạo bảng T019. Mốc lưu giữ phân công lấy `COALESCE(actual_end_at,ends_at)`, chỉ chọn trạng thái cuối và ca cha đã đóng/hủy; hồ sơ nhân viên, ca giữ lại khi còn tham chiếu.

## 11. Truy vết DBR, BR và FR

| Mã | Bảo đảm ở | Test |
| --- | --- | --- |
| DBR-20 | Mục 4, 7, 8 | `HandoverSchemaIT.DBR_20_oneHandoverPerShift`, `HandoverIT`, `M09RecordProtectionIT` |
| DBR-21 | Mục 4, 5, 8; research N-05, N-06 | `AssignmentIT.FR_030_missingCertificateOrCertificateExpiringDuringIntervalIsRejected`, `ShiftScheduleIT` (FR-013, đồng thời) |
| DBR-23 | Nhật ký trong transaction, bảo vệ bản gốc JDBC | `M09RecordProtectionIT`, `ShiftScheduleIT` (FR-055), `PostPublicationShiftIT`, `StaffEmploymentIT` (rollback) |
| DBR-25 | `Clock`, `BusinessTime`, ngày Việt Nam | `CredentialValidityTest`, `AlertStaffPortIT`, `ShiftScheduleIT` |
| DBR-36 (phần bộ phận) | Mục 2 | `StaffSchemaIT.DBR_36_staffCannotHaveTwoOpenDepartments`, `DepartmentCommandsIT`, `DepartmentDutyIT` |
| BR-M09-01 | N-05, N-08 | `AssignmentIT` (FR-030, SC-005 kiểm 200 lần), `CredentialValidityTest` |
| BR-M09-02 | N-09 | `StaffingRatioCalculatorTest`, `StaffingRatioIT`, `StaffingRatioWarningIT` |
| BR-M09-03 | N-06 | `ContinuousShiftChainTest`, `ShiftScheduleIT` (FR-013/014/016 và đồng thời) |
| BR-M09-04 | N-04 | `StaffEmploymentIT` (nghỉ việc và rollback tác động) |
| BR-M09-05 | N-05 | `CredentialLifecycleIT`, `ShiftScheduleIT`, `StaffEmploymentIT` (nghỉ không ngừng cảnh báo) |
| BR-M09-06 | N-10 | `HandoverIT` (FR-039/040/041/045, nguồn song song), `CriticalHandoverItemTest` |
| BR-M09-07 | N-06, N-10, N-13 | `ShiftLifecycleIT`, `HandoverSchemaIT`, `HandoverIT` (Chờ bàn giao, hoàn tất muộn, nhắc) |
| BR-M09-08 | N-10 | `HandoverIT` (FR-044 callback/rollback, FR-042 đồng thời) |
| BR-M09-12 | Mục 4, research N-06 | `ShiftSchemaIT.BR_M09_12_shiftAllows24HoursAndRejectsZeroOrMoreThan24Hours`, `ShiftScheduleIT` |
| BR-M09-13 | FR-019, N-07 | `PostPublicationShiftIT` (chuyển người giữ nội dung/tác giả), `StaffModuleIntegrationIT` (lịch sử phụ trách thật) |
| BR-M09-14 | FR-034, N-09 | `StaffingRatioIT.BR_M09_14_membersWithoutAssignmentsStillCount_doubleRoleOnce_excludesFloorOnlyAndApprovedLeave` |
| BR-M09-15 | FR-025, phạm vi 002 thật | `AssignmentIT` (Nháp không góp, kết thúc sớm), `StaffModuleIntegrationIT` (đổi người/vắng cắt ngay, kết ca thường giữ biên CFG) |
| BR-M09-16 | FR-049, tham chiếu vị trí | `AssignmentIT.BR_M09_16_locationReferencesIncludeDraftAssignmentsAndCurrentFloorManager`, `ShiftQueriesIT` |
| BR-M09-17 | FR-024, nghỉ dài ngày | `AssignmentIT.BR_M09_17_noShiftDoesNotLockAccountOrEraseFloorManagerHistory` |
| BR-M04-13 (phần 008) | N-07 | `PostPublicationShiftIT` (FR-020 chỉ đúng ca, callback/rollback); C còn kiểm công việc thật tại T043b |
| BR-M05-05 (phần 008) | N-10 | `HandoverIT.FR_044_confirmationCallbacksShareTransactionAndFailureRollsBackAllEffects`; C/D còn kiểm nguồn thật tại T043b |

Tên lớp trên là tên thật dưới `backend/src/test/java/com/cambae/backend/m09/`; ma trận FR-048 → FR-060, kịch bản Q và SC ở [validation.md](validation.md). Các bài kiểm fixture/fake nguồn xác minh phần điều phối của 008; không thay nghiệm thu nghiệp vụ module nguồn hoặc mobile.
