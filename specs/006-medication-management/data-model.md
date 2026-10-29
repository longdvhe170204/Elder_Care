# Data Model: Quản lý thuốc (006)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1; không nhắc lại ở từng bảng. Người cao tuổi, nhân viên, người thân, ca, tầng/khu vực, mục dị ứng, dị nguyên, mục hồ sơ ban đầu, lượt vắng, chuyến đi, đề nghị mua hộ, tệp là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Thời điểm lưu `DATETIME(6)` theo Asia/Ho_Chi_Minh (DBR-25). Số lượng thuốc lưu `DECIMAL(10,2)` theo đơn vị dùng của thuốc (spec Assumptions).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp: `don_thuoc` (hoạt chất thành `JSON` ghi một lần); `lieu_thuoc`; `ket_qua_lieu` (+ `lan_dung_prn`, cột `kind`); `phieu_doi_chieu` (dòng thành `JSON`, khóa khi Đã xác nhận); `thuoc_gia_dinh_gui` (đơn liên kết thành `JSON`); `lan_giao_nhan_thuoc` = `lan_giao_thuoc` + `lan_nhan_lai_thuoc` + `lenh_khong_mang_thuoc` + `lan_tiep_nhan_thuoc_cung_ung` và bảng dòng (`medication_handover*`, `medication_return*`, `no_medication_order`, `supplied_medication_intake`).
- `danh_muc_chung`: `thuoc` (hoạt chất trong `thuoc_tinh`).
- `su_kien_chung`: `van_de_don_thuoc`, `can_cu_co_so_ke`.
- `ho_so_chung`: `xem_lai_lieu`, `khoang_vang_thuoc`, `de_nghi_dinh_chinh_phieu`; `trang_thai_nct`: `thuoc_nct` (`resident_medication`, cột sinh hạn đối chiếu); `moc_da_nhac`: `m07_job_run` (lần chạy ra file log).
- Còn 6 bảng riêng của feature.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-29; tên mã theo bảng đã cài, migration `V006_95`)*

Mô tả dưới đây thay các mục tương ứng khi cài 006 T069; cột không nhắc lại giữ như bảng đã cài (`V006_01` → `V006_14`). Sửa so với danh sách trên: `ket_qua_lieu` giữ bảng riêng (là đích của `dinh_chinh` `M07.DOSE_RESULT` và tách nhóm 3 khỏi lịch liều nhóm 2), `lan_dung_prn` gộp vào `ket_qua_lieu`; căn cứ cơ sở kê là sự kiện (ghi thêm khi gia hạn, thay thế). 006 còn 6 bảng.

**`prescription`** (`don_thuoc`) – nhóm 2, giữ mọi cột. `medication_id` trỏ `danh_muc_chung.id` loại `M07.MEDICATION` (id mới). Thêm `active_ingredient_ids JSON NOT NULL` (thay `prescription_active_ingredient`, ghi khi tạo; trigger chặn đổi cột này).

**`medication_dose`** (`lieu_thuoc`) – nhóm 2, giữ nguyên (`UNIQUE (prescription_id, scheduled_time)`).

**`dose_result`** (`ket_qua_lieu`) – nhóm 3, giữ mọi cột, trigger, quyền. Gộp `prn_administration`: thêm `kind ENUM('SCHEDULED','PRN') NOT NULL DEFAULT 'SCHEDULED'`, `prescription_id NULL`, `dosage NULL`, `dosage_unit NULL`, `exceeded_limit NULL`; `dose_id` `NULL` khi `PRN`. Chỉ mục `(kind, prescription_id, administered_at)` cho kiểm giới hạn PRN (BR-M07-04). `CHECK ((kind = 'SCHEDULED') = (dose_id IS NOT NULL))`, `CHECK (kind = 'SCHEDULED' OR (prescription_id IS NOT NULL AND dosage IS NOT NULL))`. Cột của PRN trùng nghĩa dùng cột sẵn có (`administered_at`, `recorder_id` = `administered_by_id`, `reported_by_*`, `reason`, `adverse_reaction`, `note`, `device_recorded_at` = `offline_device_time`, `synced_after_permission_loss` = `recorded_after_disconnect`). Dòng PRN cấp id mới; `dinh_chinh`, `nhat_ky` trỏ `M07.PRN_ADMINISTRATION` được đổi sang `M07.DOSE_RESULT` + id mới ở lớp đọc theo `legacy`.

**`medication_reconciliation`** (`phieu_doi_chieu`) – nhóm 2 → 3, giữ mọi cột và `UNIQUE`. Thêm `lines JSON NOT NULL` (thay `reconciliation_item`: mảng `{itemOrder, source, prescriptionId, admissionMedicationItemId, medicationDescription, decision, reason, newPrescription, createdById, createdAt, discontinuedOutsideTicket, createdPrescriptionId}`); `khoa_luc` = `confirmed_at`, trigger chặn sửa khi đã xác nhận (thay trigger của dòng). Không trùng đơn, không trùng mục thuốc khi tiếp nhận trong một phiếu (hai `UNIQUE` cũ) kiểm ở domain.

**`family_medication_batch`** (`thuoc_gia_dinh_gui`) – nhóm 2, giữ mọi cột sau `V006_14`. Thêm `prescription_bindings JSON NOT NULL` (thay `family_medication_prescription`: mảng `{prescriptionId, boundFrom, boundTo}`); trigger `trg_family_medication_batch_guard` hiện có giữ.

**`medication_handover`** (`lan_giao_nhan_thuoc`, mở rộng) – nhóm 2, khóa theo dòng. Gộp `medication_handover`, `medication_handover_item`, `medication_handover_dose`, `medication_return`, `medication_return_item`, `no_medication_order`, `supplied_medication_intake`:

| Cột | Ghi chú |
| --- | --- |
| `id`, `kind ENUM('HANDOVER','RETURN','NO_MEDICATION','SUPPLIED_INTAKE')` | Giữ id của `medication_handover`; loại khác cấp id mới |
| `resident_id`, `trip_id`, `absence_interval_id`, `handover_id NULL` (lần nhận lại trỏ lần giao) | |
| Cột của giao (`recipient_*`, `relationship`, `dispensed_by_id`, `handover_time`, `status`, `version`), nhận lại (`recorded_by_id`, `return_time`, `reason`), không mang (`reason`, `created_by_id`), tiếp nhận cung ứng (`procurement_line_id`, `procurement_request_id`, `prescription_id`, `medication_id`, `medication_name`, `concentration`, `dosage_form`, `quantity_received`, `unit`, `expiry_date`, `item_condition`, `intake_result`, `discrepancy`, `deliverer_name`, `receiver_id`, `received_at`) | `NULL` với loại khác; `CHECK` theo `kind` |
| `lines JSON NULL` | Dòng thuốc giao `{prescriptionId, quantity, familyMedicationBatchId, medicationSource}`, liều mang theo `{doseId, revertedAt}`, dòng nhận lại `{prescriptionId, quantity}` |
| `no_medication_key` | Cột sinh `IF(kind = 'NO_MEDICATION', CONCAT(resident_id, ':', trip_id), NULL)` `UNIQUE` (thay `uq_resident_trip_no_med`) |
| `khoa_luc`, `created_at` | `RETURN`, `NO_MEDICATION`, `SUPPLIED_INTAKE` khóa khi thêm (nhóm 3); `HANDOVER` khóa khi trạng thái cuối |

Chỉ mục `(kind, resident_id, created_at)`, `(trip_id)`, `(procurement_request_id)`. `absence_interval_id` là khóa ngoại tới `simple_record.id`; domain kiểm dòng đó đúng loại `M07.ABSENCE_INTERVAL` (L1).

**Loại ở bảng chung**:

| Bảng chung | `loai` | Nội dung | Ràng buộc chuyển về backend |
| --- | --- | --- | --- |
| `danh_muc_chung` | `M07.MEDICATION` | `ten` = tên; `{concentration, dosageForm, defaultRoute, usageUnit, beforeWindowMinutes, afterWindowMinutes, specialControl, activeIngredientIds[]}`; `ma` = khóa chuẩn hóa tên, hàm lượng, dạng | Không trùng (tên, hàm lượng, dạng) qua `ma_khoa` (thay `uq_medication_identity`); hoạt chất là mục `M01.ALLERGEN` nhóm `ACTIVE_INGREDIENT` |
| `su_kien_chung` | `M07.PRESCRIPTION_ISSUE` | đối tượng `PRESCRIPTION`; `{reconciliationLineId, lineNumber, issueType, issueCode, duplicatePrescriptionId, allergyItemId, severity, activeIngredient, manualReviewRequired, confirmationReason}`; `nguoi_thuc_hien_id` = người xác nhận | Ghi cùng lệnh lập đơn |
| `su_kien_chung` | `M07.PRESCRIPTION_BASIS` | đối tượng `PRESCRIPTION`; `{command, facilityName, prescriberName, prescribedDate, basisType, callerName, receiverName, calledAt, documentFileId}` | Tệp `documentFileId` gắn chủ `M07.PRESCRIPTION_BASIS` + id sự kiện |
| `ho_so_chung` | `M07.DOSE_REVIEW` | đối tượng `DOSE_RESULT`; `{floorAreaId, reviewerCandidate, reviewerId, reviewedAt, note}`; `trang_thai` | `M07.DR:<doseResultId>` |
| `ho_so_chung` | `M07.ABSENCE_INTERVAL` | `nct_id`; `{absenceType, fromTime, expectedToTime, actualToTime, absenceSessionId, tripId, noMedicationOrderId}`; `trang_thai` `OPEN`, `CLOSED` | `M07.AI:<residentId>` khi `OPEN` (thay `uq_open_absence_resident`) |
| `ho_so_chung` | `M07.RECONCILIATION_CORRECTION_REQUEST` | đối tượng `RECONCILIATION`; `{correctedContent, reason, returnReason, confirmedById, confirmedAt, correctionId}`; `trang_thai` | |
| `trang_thai_nct` | module `M07` | `{reconciliationDueAt, reconciliationReason, reconciliationWarnedAt}` | Thay `resident_medication`; cột sinh `m07_reconciliation_due_at` có chỉ mục cho job cảnh báo |
| `moc_da_nhac` | `M07.JOB.<jobKey>` | `moc` = `checkpoint` | Thay `m07_job_run` (lần chạy ra `logs/jobs.log`) |

**Chuyển dữ liệu (`V006_95`)**: `medication` (kèm hoạt chất) vào `danh_muc_chung`, đổi `medication_id` ở `prescription`, `medication_dose`, `family_medication_batch`, dòng tiếp nhận cung ứng; `prescription_active_ingredient`, `reconciliation_item`, `family_medication_prescription` vào JSON của bảng cha; `prn_administration` vào `dose_result` (đổi `dose_review.prn_usage_id` thành id mới); bảy bảng giao nhận vào `medication_handover` (đổi `no_medication_order_id` của khoảng vắng, `leave_handover_id` của kết quả liều); `prescription_issue`, `prescription_basis` vào `su_kien_chung`; `dose_review`, `medication_absence_interval`, `reconciliation_correction_request` vào `ho_so_chung`; `resident_medication` vào `trang_thai_nct`; `m07_job_run` vào `moc_da_nhac`.

**Bảo đảm** (thay phần tương ứng ở các mục sau): một liều mỗi (đơn, giờ) (`UNIQUE` giữ); kết quả liều và lần dùng PRN không sửa, không xóa (trigger `dose_result` giữ); một phiếu đối chiếu đang mở và một phiếu khi tiếp nhận mỗi người (`UNIQUE` giữ); một khoảng vắng mở mỗi người và một lệnh không mang thuốc mỗi (người, chuyến) (`khoa_duy_nhat`, `no_medication_key`). Test m07 hiện có phải xanh, giữ kịch bản nghiệp vụ và đổi bảng đích trong test (danh sách cuối mục).

**Bảng truy vết sau gộp** *(bổ sung 2026-10-30; khi làm task gộp, sửa cột "bảo đảm bằng" của bảng DBR, BR ở các mục sau theo bảng này)*:

| Mã | Bảo đảm sau gộp | Test |
| --- | --- | --- |
| BR-M07-03 | Một khoảng vắng mở mỗi người: `ho_so_chung` loại `M07.ABSENCE_INTERVAL`, `khoa_duy_nhat` | test vắng mặt hiện có (`MedicationHandoverTest`, IT vắng mặt) |
| BR-M07-04 | Giới hạn khoảng cách, số lần PRN: đọc `dose_result` loại `PRN` theo đơn và thời điểm; chỉ mục `(kind, prescription_id, administered_at)` (M2) | `BR_M07_04_PrnLimitTest`, IT PRN hiện có |
| BR-M07-16 | Căn cứ cơ sở kê: `su_kien_chung` loại `M07.PRESCRIPTION_BASIS` | `BR_M07_16_ExternalPrescriberBasisIT` |
| BR-M07-17 | Đề nghị đính chính phiếu: `ho_so_chung` loại `M07.RECONCILIATION_CORRECTION_REQUEST` | `MedicationReconciliationTest`, IT đính chính phiếu hiện có |
| BR-M07-18 | Lô dùng chung cho nhiều đơn: `family_medication_batch.prescription_bindings`; chỉ mục nhiều giá trị `((CAST(prescription_bindings->'$[*].prescriptionId' AS UNSIGNED ARRAY)))` cho tra lô theo đơn (M2) | `FamilyMedicationTest` |

**Test phải đổi bảng đích** *(rà 2026-10-30; 10 file đọc, ghi thẳng bảng cũ bằng SQL)*: giữ kịch bản nghiệp vụ, chỉ đổi bảng, cột đích.
- Test của module: `BR_M07_05_PrescriberAuthorityIT`, `BR_M07_07_DosageChangeIT`, `BR_M07_08_PrescriptionExpirationIT`, `BR_M07_15_CancelDosageChangeIT`, `BR_M07_16_ExternalPrescriberBasisIT`, `DBR_14_ReplacementPrescriptionIT`, `FR_012_NewAllergyIT`, `PrescriptionTriggerIT`.
- Test của common (chủ module review): `RecipientGroupTest`.
- Test của m14 (chủ module review): `DetailRowPolicyTest`.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `thuoc_nct` | 2 (kỹ thuật) | Đích khóa theo người, hạn đối chiếu (research D-01, D-13) *(R-15: `thuoc_nct` → `trang_thai_nct`)* |
| `thuoc`, `thuoc_hoat_chat` | 1 | A *(R-15: `thuoc_hoat_chat` → `danh_muc_chung` (thuộc tính của thuốc))* |
| `don_thuoc` | 2 | B |
| `don_thuoc_hoat_chat`, `van_de_don_thuoc`, `can_cu_co_so_ke` | 3 | B (FR-004, FR-007, FR-013) *(R-15: `don_thuoc_hoat_chat` → cột `JSON` của `don_thuoc`; `van_de_don_thuoc`, `can_cu_co_so_ke` → `su_kien_chung`)* |
| `lieu_thuoc` | 2 (do hệ thống điều khiển), chỉ dòng đã vật hóa | C |
| `ket_qua_lieu` | 3 | C, E |
| `xem_lai_lieu` | 2 | E (FR-029) *(R-15: `xem_lai_lieu` → `ho_so_chung`)* |
| `lan_dung_prn` | 3 | D *(R-15: `lan_dung_prn` → `ket_qua_lieu` (`dose_result`))* |
| `phieu_doi_chieu`, `dong_doi_chieu` | 2 khi Mở, Chờ xác nhận; 3 khi Đã xác nhận | F *(R-15: `dong_doi_chieu` → cột `JSON` của `phieu_doi_chieu`)* |
| `lan_tiep_nhan_thuoc_cung_ung` *(bổ sung 2026-10-11, Q-337)* | 3 | I (FR-054, FR-055) *(R-15: `lan_tiep_nhan_thuoc_cung_ung` → `lan_giao_nhan_thuoc` (`medication_handover`))* |
| `de_nghi_dinh_chinh_phieu` | 2 | F (FR-039) *(R-15: `de_nghi_dinh_chinh_phieu` → `ho_so_chung`)* |
| `thuoc_gia_dinh_gui` | 2 | G |
| `thuoc_gui_don` | 2 (liên kết) | G (FR-043) *(R-15: `thuoc_gui_don` → cột `JSON` của `thuoc_gia_dinh_gui`)* |
| `khoang_vang_thuoc` | 2 | H (FR-048) *(R-15: `khoang_vang_thuoc` → `ho_so_chung`)* |
| `lan_giao_thuoc`, `lan_giao_thuoc_dong`, `lan_giao_lieu` | 3 (trạng thái do hệ thống) | H (FR-048a) *(R-15: `lan_giao_thuoc` → `lan_giao_nhan_thuoc` (`medication_handover`))* |
| `lan_nhan_lai_thuoc`, `lan_nhan_lai_thuoc_dong` | 3 | H (FR-048a) *(R-15: `lan_nhan_lai_thuoc` → `lan_giao_nhan_thuoc` (`medication_handover`))* |
| `lenh_khong_mang_thuoc` | 3 | H (FR-048b) *(R-15: `lenh_khong_mang_thuoc` → `lan_giao_nhan_thuoc` (`medication_handover`))* |
| `lan_chay_m07` | Kỹ thuật | Job (research D-19) |

---

## 2. `thuoc_nct` – nhóm 2 (kỹ thuật) → gộp theo R-15: `thuoc_nct` → `trang_thai_nct`

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `nct_id` | `BIGINT` | Khóa chính; tạo khi lần đầu có lệnh 006 cho người đó, hoặc trong bean Hoàn tất tiếp nhận | D-01 |
| `han_doi_chieu` | `DATETIME(6) NULL` | Thời điểm Hoàn tất tiếp nhận / Ghi nhận trở về + CFG-M07-05; xóa khi phiếu Đã xác nhận hoặc trạng thái cuối | FR-034, BR-M07-09 |
| `ly_do_doi_chieu` | `ENUM('TIEP_NHAN','TRO_VE_BENH_VIEN') NULL` | Chỉ hai lý do có hạn; phiếu `THEO_YEU_CAU_CHUYEN_MON` không dùng cột này (FR-034b, Q-340) | FR-034 |
| `da_canh_bao_doi_chieu_luc` | `DATETIME(6) NULL` | Một cảnh báo mỗi hạn | FR-034 |
| `phien_ban` | `INT` | | |

---

## 3. Danh mục thuốc (mục A)

### `thuoc` – nhóm 1 → gộp theo R-15: `thuoc` → `danh_muc_chung`

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `ten`, `ham_luong`, `dang_bao_che`, `duong_dung_mac_dinh`, `don_vi_dung` | | `UNIQUE (ten, ham_luong, dang_bao_che)` | FR-001 |
| `cua_so_truoc_phut`, `cua_so_sau_phut` | `SMALLINT NULL` | Rỗng = CFG-M07-01 | FR-001, 11.2 |
| `kiem_soat_dac_biet` | `BOOLEAN` | | 8.6, FR-029 |
| `trang_thai_hieu_luc` | Mẫu 000 | Ngừng hiệu lực: không chọn cho đơn mới | FR-002 |

### `thuoc_hoat_chat` – nhóm 1 (bảng con) → gộp theo R-15: `thuoc_hoat_chat` → `danh_muc_chung` (thuộc tính của thuốc)

`thuoc_id` FK, `di_nguyen_id` (danh mục dị nguyên nhóm thuốc của 001). Khóa chính hai cột. Sửa chỉ áp cho đơn lưu sau (đơn chép vào `don_thuoc_hoat_chat`). *(R-15: `don_thuoc_hoat_chat` → cột `JSON` của `don_thuoc`)*

`don_thuoc.thuoc_id` FK `ON DELETE RESTRICT`: thuốc đã có đơn không xóa được (FR-002).

---

## 4. Đơn thuốc (mục B)

### `don_thuoc` – nhóm 2

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `nct_id` | `BIGINT` | | FR-004 |
| `thuoc_id` | `BIGINT` FK | | FR-004 |
| `lieu`, `don_vi` | `DECIMAL(10,2)`, `VARCHAR(20)` | Đơn vị dùng của thuốc | FR-004 |
| `duong_dung` | `VARCHAR(40)` | | FR-004 |
| `loai_don` | `ENUM('DINH_KY','PRN')` | | FR-004 |
| `kieu_tan_suat` | `ENUM('CAC_MOC','MOI_N_GIO','NGAY_TRONG_TUAN') NULL` | Bắt buộc với `DINH_KY` | FR-004 |
| `cac_moc` | `JSON NULL` | Danh sách `HH:mm` | FR-004 |
| `n_gio`, `moc_dau` | `SMALLINT NULL`, `DATETIME(6) NULL` | Với `MOI_N_GIO`; chuỗi liên tục từ `moc_dau` | FR-004, CHK036 |
| `ngay_trong_tuan` | `SET('1','2','3','4','5','6','7') NULL` | | FR-004 |
| `khoang_cach_toi_thieu_phut`, `so_lan_toi_da_24h` | `INT NULL`, `SMALLINT NULL` | Bắt buộc với `PRN`; `CHECK (> 0)`, `CHECK (>= 1)` | FR-010, FR-025 |
| `bat_dau_luc` | `DATETIME(6)` | Đặt lại = thời điểm Hoàn tất tiếp nhận với đơn "chờ tiếp nhận" | FR-004, FR-034a |
| `ngay_ket_thuc` | `DATE NULL` | Đổi bằng lệnh Gia hạn | FR-004, FR-015 |
| `ngung_tu` | `DATETIME(6) NULL` | Thời điểm ngừng đã hẹn (đổi liều tương lai, ngừng tương lai) | FR-016, FR-015 |
| `nguon_thuoc` | `ENUM('VIEN','GIA_DINH_GUI')` | | FR-004 |
| `co_so_ke_loai` | `ENUM('NOI_BO','BEN_NGOAI')` | | FR-005, FR-006 |
| `co_so_ke_ten`, `nguoi_ke_ten`, `nguoi_ke_id`, `ngay_ke` | | `nguoi_ke_id` với đơn nội bộ; ba trường kia bắt buộc với bên ngoài | FR-006 |
| `tep_don_id` | `BIGINT NULL` | Ảnh đơn/toa (`TepApi`) | FR-006 |
| `ghi_chu_huong_dan` | `VARCHAR(2000) NULL` | Trường duy nhất sửa được; `If-Match` | FR-014 |
| `trang_thai` | `ENUM('CHO_HIEU_LUC','HIEU_LUC','TAM_DUNG','DA_NGUNG','HET_HAN')` | | FR-011 |
| `ly_do_tam_dung` | `ENUM('LENH','CHO_DOI_CHIEU') NULL` | | FR-011, FR-037 |
| `dau_cho_tiep_nhan` | `BOOLEAN` | | FR-034a |
| `thay_the_cho_id` | `BIGINT NULL` FK tự trỏ | `UNIQUE`: mỗi đơn bị thay tối đa một lần | FR-016a, DBR-14 |
| `ly_do_ket_thuc` | `ENUM('NGUNG','THAY_THE','HET_HAN','PHIEU_DOI_CHIEU','TRANG_THAI_CUOI') NULL` | | FR-011 |
| `phieu_doi_chieu_id` | `BIGINT NULL` | Đơn tạo từ phiếu | FR-036 |
| `da_nhac_het_han_luc` | `DATETIME(6) NULL` | | FR-017 |
| `nguoi_lap_id`, `lap_luc`, `phien_ban` | | `phien_ban` chỉ cho lệnh sửa ghi chú | |

`CHECK`: `loai_don='DINH_KY'` thì `kieu_tan_suat` không rỗng và hai cột PRN rỗng; ngược lại. `CHECK (co_so_ke_loai='NOI_BO' OR (co_so_ke_ten IS NOT NULL AND nguoi_ke_ten IS NOT NULL AND ngay_ke IS NOT NULL))`.

Trigger `trg_don_thuoc_khoa` (FR-014, DBR-14): chặn `UPDATE` của `thuoc_id`, `lieu`, `don_vi`, `duong_dung`, `loai_don`, cột tần suất, cột PRN, `nguon_thuoc`, `co_so_ke_*`, `thay_the_cho_id` khi dòng đã lưu; chặn `UPDATE` `bat_dau_luc` trừ khi `dau_cho_tiep_nhan` chuyển từ `true` sang `false` cùng lệnh; chặn mọi đổi `trang_thai` khi trạng thái cũ là `DA_NGUNG`, `HET_HAN` (FR-013).

Chỉ mục: `(nct_id, trang_thai)` cho lịch, trùng hoạt chất; `(trang_thai, bat_dau_luc)`, `(trang_thai, ngung_tu)`, `(trang_thai, ngay_ket_thuc)` cho job.

**Bảng chuyển** (khớp bảng FR-011; cài ở `BangChuyenDon`):

| Từ | Lệnh / sự kiện | Đến | Nơi thực hiện |
| --- | --- | --- | --- |
| — | Nhập đơn | `HIEU_LUC` / `CHO_HIEU_LUC` | `POST /nguoi-cao-tuoi/{nctId}/don-thuoc` |
| — | Phiếu "tiếp nhận" lập trước được xác nhận | `CHO_HIEU_LUC` + `dau_cho_tiep_nhan` | `POST /phieu-doi-chieu/{id}/xac-nhan` |
| `CHO_HIEU_LUC` | Tới `bat_dau_luc` (không "chờ tiếp nhận") | `HIEU_LUC` | `ViecTheoPhutM07` |
| `CHO_HIEU_LUC` "chờ tiếp nhận" | Hoàn tất tiếp nhận | `HIEU_LUC` | Bean 001 |
| `HIEU_LUC` | Tạm dừng đơn | `TAM_DUNG` (`LENH`) | `POST /don-thuoc/{id}/tam-dung` |
| `HIEU_LUC`, `CHO_HIEU_LUC` | Hoàn tất tiếp nhận / trở về từ bệnh viện | `TAM_DUNG` (`CHO_DOI_CHIEU`) | Bean 001 |
| `TAM_DUNG` (`LENH`) | Tiếp tục đơn | `HIEU_LUC` | `POST /don-thuoc/{id}/tiep-tuc` |
| `TAM_DUNG` | Phiếu xác nhận Tiếp tục | `HIEU_LUC` | `…/xac-nhan` của phiếu |
| `HIEU_LUC`, `TAM_DUNG`, `CHO_HIEU_LUC` | Ngừng đơn (ngay) | `DA_NGUNG` | `POST /don-thuoc/{id}/ngung` |
| `HIEU_LUC` | Ngừng hoặc đổi liều tương lai: tới `ngung_tu` | `DA_NGUNG` | `ViecTheoPhutM07` |
| `HIEU_LUC`, `TAM_DUNG`, `CHO_HIEU_LUC` | Đổi liều (ngay); phiếu xác nhận Thay đổi liều, Ngừng | `DA_NGUNG` | `POST /don-thuoc/{id}/doi-lieu`; phiếu |
| `HIEU_LUC`, `TAM_DUNG` | Hết ngày kết thúc | `HET_HAN` | `ViecHangNgayM07` |
| `HIEU_LUC`, `TAM_DUNG`, `CHO_HIEU_LUC` | Trạng thái cuối của người cao tuổi | `DA_NGUNG` | Bean 001 |

### `don_thuoc_hoat_chat` – nhóm 3 → gộp theo R-15: `don_thuoc_hoat_chat` → cột `JSON` của `don_thuoc`

`don_thuoc_id`, `di_nguyen_id`. Chép từ `thuoc_hoat_chat` lúc lưu đơn; dùng cho FR-007 (a), FR-012. *(R-15: `thuoc_hoat_chat` → `danh_muc_chung` (thuộc tính của thuốc))*

### `van_de_don_thuoc` – nhóm 3 → gộp theo R-15: `van_de_don_thuoc` → `su_kien_chung`

`id`, `don_thuoc_id` (hoặc `dong_doi_chieu_id` khi kiểm trên phiếu), `loai` (`TRUNG_HOAT_CHAT`, `XUNG_DOT_DI_UNG`, `DI_UNG_KHAC_CAN_XEM`), `don_trung_id`, `muc_di_ung_id`, `muc_do`, `hoat_chat`, `ly_do_xac_nhan` (bắt buộc trừ `DI_UNG_KHAC_CAN_XEM`), `nguoi_xac_nhan_id`, `thoi_diem`. Nguồn: FR-007, FR-008, SC-003.

### `can_cu_co_so_ke` – nhóm 3 → gộp theo R-15: `can_cu_co_so_ke` → `su_kien_chung`

`id`, `don_thuoc_id`, `lenh` (`TAM_DUNG`, `TIEP_TUC`, `NGUNG`, `GIA_HAN`, `DOI_LIEU`), `co_so_ten`, `nguoi_chi_dinh`, `ngay`, `hinh_thuc` (`DON_MOI`, `GIAY_RA_VIEN`, `DIEN_THOAI`), `nguoi_goi`, `nguoi_tra_loi`, `thoi_diem_goi`, `tep_id`, `nguoi_ghi_id`, `thoi_diem`. `CHECK`: `hinh_thuc='DIEN_THOAI'` thì ba cột điện thoại không rỗng; ngược lại `tep_id` không rỗng. Nguồn: FR-013, BR-M07-16.

---

## 5. Liều thuốc (mục C, E)

### `lieu_thuoc` – nhóm 2 (do hệ thống điều khiển), chỉ dòng đã vật hóa

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | | |
| `don_thuoc_id`, `nct_id`, `thuoc_id` | `BIGINT` | `thuoc_id` chép để đếm từ chối và báo cáo | FR-020 |
| `thoi_diem_du_kien` | `DATETIME(6)` | `UNIQUE (don_thuoc_id, thoi_diem_du_kien)` | FR-019, DBR-13 |
| `cua_so_tu`, `cua_so_den` | `DATETIME(6)` | Chốt lúc vật hóa | FR-020 |
| `lieu`, `nguon_thuoc`, `kiem_soat_dac_biet` | | Chép từ đơn, thuốc | FR-020 |
| `trang_thai` | `ENUM('CHUA_DEN_GIO','DEN_GIO','TRE','BO_LO','DA_DUNG','TU_CHOI','KHONG_THUC_HIEN','TAM_DUNG','MANG_THEO','DA_HUY')` | Giá trị gốc; hiện hành sau đính chính đọc qua 000 | FR-021 |
| `den_gio_luc`, `tre_luc`, `dong_luc`, `ket_thuc_luc` | `DATETIME(6) NULL` | `ket_thuc_luc` cho `TAM_DUNG` qua cuối cửa sổ | FR-022 |
| `ly_do_huy` | `ENUM('NGUNG_DON','DOI_LIEU','HET_HAN','PHIEU_DOI_CHIEU','TRANG_THAI_CUOI') NULL` | | FR-021 |
| `dong_boi_loai`, `dong_boi_id`, `quy_tac_dong` | Theo research D-07 | | FR-022 |
| `cho_ghi_nhan_sau_tro_ve` | `BOOLEAN` | | FR-027, FR-028 |
| `da_nhac_tre_luc` | `DATETIME(6) NULL` | Một lần | FR-022 |
| `tang_khu_vuc_id` | `BIGINT NULL` | Tầng lúc đóng; cho đính chính cách B và báo cáo theo ca | FR-030, FR-051 |

Trigger `trg_lieu_thuoc_trang_thai_cuoi`: chặn `UPDATE` cột trạng thái khi trạng thái cũ thuộc `BO_LO`, `DA_DUNG`, `TU_CHOI`, `KHONG_THUC_HIEN`, `DA_HUY`, hoặc khi `ket_thuc_luc` đã đặt (FR-022). Không có `DELETE`.

Chỉ mục: `(trang_thai, cua_so_tu)`, `(trang_thai, cua_so_den)`, `(trang_thai, tre_luc)` cho job; `(nct_id, thoi_diem_du_kien)` cho lịch, checklist, báo cáo; `(nct_id, thuoc_id, thoi_diem_du_kien)` cho từ chối liên tiếp.

**Bảng chuyển** (khớp bảng FR-021; cài ở `BangChuyenLieu`; dòng "—" là trạng thái tính của liều chưa vật hóa, research D-06):

| Từ | Lệnh / sự kiện | Đến | Nơi thực hiện |
| --- | --- | --- | --- |
| — (tính) | Tới đầu cửa sổ | `DEN_GIO` / `TAM_DUNG` / `MANG_THEO` (vật hóa) | `ViecTheoPhutM07` |
| — (tính) | Lệnh ghi nhận hoặc gắn lần giao trước đầu cửa sổ | Dòng được vật hóa rồi chuyển theo lệnh | Lệnh tương ứng |
| `CHUA_DEN_GIO` | Tới đầu cửa sổ | `DEN_GIO` | `ViecTheoPhutM07` |
| `DEN_GIO`, `TRE` | Xác nhận Đã dùng / Từ chối / Không thực hiện | `DA_DUNG` / `TU_CHOI` / `KHONG_THUC_HIEN` | `POST /lieu-thuoc/{lieuKey}/da-dung`, `/tu-choi`, `/khong-thuc-hien`; handler ngoại tuyến |
| `DEN_GIO` | Hết cửa sổ | `TRE` | `ViecTheoPhutM07` |
| `TRE` | Quá CFG-M07-02 | `BO_LO` | `ViecTheoPhutM07` |
| `TAM_DUNG` | Trở về từ Tạm vắng; đơn Tiếp tục; phiếu xác nhận Tiếp tục; cuối cửa sổ chưa qua | `CHUA_DEN_GIO` / `DEN_GIO` | Bean 001; lệnh đơn; phiếu |
| `MANG_THEO` (chưa ghi nhận) | Trở về trước cuối cửa sổ | `CHUA_DEN_GIO` / `DEN_GIO` | Bean 001 |
| `MANG_THEO` | Ghi nhận Đã dùng / Không thực hiện | `DA_DUNG` / `KHONG_THUC_HIEN` | `…/da-dung`, `…/khong-thuc-hien` (căn cứ mang theo / ghi theo báo lại) |
| `CHUA_DEN_GIO`, `DEN_GIO`, `TAM_DUNG`, `MANG_THEO` | Ngừng, đổi liều, Hết hạn, phiếu Ngừng / Thay đổi liều | `DA_HUY` | Lệnh đơn; job; phiếu |
| `CHUA_DEN_GIO`, `TAM_DUNG`, `MANG_THEO` | Trạng thái cuối | `DA_HUY` | Bean 001 |
| `DEN_GIO`, `TRE` | Trạng thái cuối | `KHONG_THUC_HIEN` (Hệ thống) | Bean 001 |
| `TAM_DUNG` | Qua cuối cửa sổ | `TAM_DUNG` (đặt `ket_thuc_luc`) | `ViecTheoPhutM07` |

### `ket_qua_lieu` – nhóm 3

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `lieu_thuoc_id` | `BIGINT` | `UNIQUE (lieu_thuoc_id)`: một lần đóng gốc | DBR-13, BR-M07-12 |
| `nct_id`, `tang_khu_vuc_id` | `BIGINT` | | 000 mục 1.3 |
| `loai` | `ENUM('DA_DUNG','TU_CHOI','KHONG_THUC_HIEN','BO_LO')` | | FR-021 |
| `thoi_diem_dung` | `DATETIME(6) NULL` | Với `DA_DUNG` | FR-020 |
| `thoi_diem`, `thoi_diem_thiet_bi`, `nguoi_ghi_id` | Mẫu 000; `nguoi_ghi_id` rỗng khi Hệ thống | | DBR-25 |
| `can_cu_quyen` | `ENUM('DUOC_PHAN_CONG','NGUOI_PHU_TRACH_CA','LIEU_CHUNG_TANG','MANG_THEO','GHI_THEO_BAO_LAI','HE_THONG')` | | FR-026, FR-027 |
| `nguoi_bao_lai_loai`, `nguoi_bao_lai_id`, `nguoi_bao_lai_ten`, `quan_he` | | Bắt buộc với `GHI_THEO_BAO_LAI` | FR-027 |
| `phan_ung`, `ly_do`, `ghi_chu` | `VARCHAR(2000) NULL` | `ly_do` bắt buộc với `TU_CHOI`, `KHONG_THUC_HIEN` | FR-020, FR-021 |
| `nhan_dung_tre`, `nhan_ghi_sau_mat_ket_noi`, `nhan_dong_bo_sau_mat_quyen` | `BOOLEAN` | | FR-021, FR-029, 002 A-11 |
| `lan_giao_id` | `BIGINT NULL` | `lo_thuoc_gui_id` bỏ theo Q-393: liều không chọn lô | FR-048a |

`CHECK`: `thoi_diem_dung <= COALESCE(thoi_diem_thiet_bi, thoi_diem)`; `loai='DA_DUNG'` thì `thoi_diem_dung` không rỗng. Trigger nhóm 3 của 000. Đính chính loại `M07.KET_QUA_LIEU`.

### `xem_lai_lieu` – nhóm 2 → gộp theo R-15: `xem_lai_lieu` → `ho_so_chung`

`id`, `ket_qua_lieu_id` (`UNIQUE`), `lan_dung_prn_id`, `tang_khu_vuc_id`, `nguoi_xem_lai_du_kien` (`JSON`: một người hoặc danh sách), `trang_thai` (`CHO_XEM_LAI`, `CHAP_NHAN`, `CAN_DINH_CHINH`), `nguoi_xem_lai_id`, `xem_lai_luc`, `ghi_chu`. `CHECK (nguoi_xem_lai_id IS NULL OR nguoi_xem_lai_id <> nguoi_ghi_id)` với `nguoi_ghi_id` chép từ bản ghi. Nguồn: FR-029, Q-66, SC-012.

---

## 6. Lần dùng PRN (mục D)

### `lan_dung_prn` – nhóm 3 → gộp theo R-15: `lan_dung_prn` → `ket_qua_lieu` (`dose_result`)

`id`, `don_thuoc_id`, `nct_id`, `thuoc_id`, `tang_khu_vuc_id`, `thoi_diem_dung`, `lieu_dung` (`CHECK (lieu_dung > 0)`; không vượt liều đơn kiểm ở backend), `ly_do` (bắt buộc), `phan_ung`, `ghi_chu`, `lan_giao_id` (không có `lo_thuoc_gui_id`, Q-393), `can_cu_quyen`, cột người báo lại như `ket_qua_lieu`, `vuot_gioi_han BOOLEAN`, `thoi_diem`, `thoi_diem_thiet_bi`, `nguoi_ghi_id`, `nhan_ghi_sau_mat_ket_noi`, `nhan_dong_bo_sau_mat_quyen`. Chỉ mục `(don_thuoc_id, thoi_diem_dung)`. Đính chính loại `M07.LAN_DUNG_PRN`. Nguồn: FR-024 → FR-025a.

---

## 7. Đối chiếu thuốc (mục F)

### `phieu_doi_chieu` – nhóm 2 khi `MO`, `CHO_XAC_NHAN`; nhóm 3 khi `DA_XAC_NHAN`

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `nct_id` | `BIGINT` | | FR-038 |
| `ly_do` | `ENUM('TIEP_NHAN','TRO_VE_BENH_VIEN','THEO_YEU_CAU_CHUYEN_MON')` | Giá trị thứ ba bổ sung 2026-10-11 (FR-034b, Q-340) | FR-034, FR-034b |
| `ly_do_lap` | `VARCHAR(2000) NULL` | `CHECK (ly_do <> 'THEO_YEU_CAU_CHUYEN_MON' OR ly_do_lap IS NOT NULL)` | FR-034b |
| `trang_thai` | `ENUM('MO','CHO_XAC_NHAN','DA_XAC_NHAN','DA_HUY')` | | FR-035 |
| `han_luc`, `qua_han_luc` | `DATETIME(6) NULL` | `han_luc` rỗng khi lập trước tiếp nhận và luôn rỗng với `THEO_YEU_CAU_CHUYEN_MON` | FR-035, FR-034b |
| `nguoi_lap_id`, `lap_luc`, `nguoi_gui_id`, `gui_luc`, `nguoi_xac_nhan_id`, `xac_nhan_luc` | | | FR-038 |
| `ly_do_tra_lai`, `ly_do_huy` | `VARCHAR(2000) NULL` | `ly_do_huy`: chuyển viện lại, trạng thái cuối, Hủy tiếp nhận | FR-035 |
| `mo_nct` | Cột sinh `IF(trang_thai IN ('MO','CHO_XAC_NHAN'), nct_id, NULL)` | `UNIQUE` | FR-035 |
| `tiep_nhan_nct` | Cột sinh `IF(ly_do='TIEP_NHAN' AND trang_thai <> 'DA_HUY', nct_id, NULL)` | `UNIQUE` | FR-034a |
| `phien_ban` | `INT` | `If-Match` cho lệnh thay dòng | |

`CHECK (nguoi_xac_nhan_id IS NULL OR nguoi_xac_nhan_id <> nguoi_gui_id)`. Trigger `trg_phieu_doi_chieu_khoa`: chặn `UPDATE` khi trạng thái cũ là `DA_XAC_NHAN`, `DA_HUY` (FR-039).

**Bảng chuyển** (khớp bảng FR-035):

| Từ | Lệnh / sự kiện | Đến | Nơi thực hiện |
| --- | --- | --- | --- |
| — | Lập phiếu | `MO` | `POST /nguoi-cao-tuoi/{nctId}/phieu-doi-chieu` |
| — | Lập phiếu theo yêu cầu chuyên môn (lý do lập bắt buộc; không tạm dừng đơn, không hạn) | `MO` | Cùng endpoint, `lyDo = THEO_YEU_CAU_CHUYEN_MON` (FR-034b) |
| `MO` | Thay dòng, ghi quyết định | `MO` | `PUT /phieu-doi-chieu/{id}/dong` |
| `MO` | Gửi xác nhận | `CHO_XAC_NHAN` | `POST /phieu-doi-chieu/{id}/gui-xac-nhan` |
| `CHO_XAC_NHAN` | Trả lại | `MO` | `POST /phieu-doi-chieu/{id}/tra-lai` |
| `CHO_XAC_NHAN` | Xác nhận | `DA_XAC_NHAN` | `POST /phieu-doi-chieu/{id}/xac-nhan` |
| `MO`, `CHO_XAC_NHAN` (lập trước) | Hoàn tất tiếp nhận | Giữ nguyên, đặt `han_luc` | Bean 001 |
| `MO`, `CHO_XAC_NHAN` | Tới hạn | Giữ nguyên, đặt `qua_han_luc` | `ViecTheoPhutM07` |
| `MO`, `CHO_XAC_NHAN` | Chuyển lại bệnh viện; trạng thái cuối; Hủy tiếp nhận | `DA_HUY` | Bean 001 |

### `dong_doi_chieu` – bảng con (theo nhóm của phiếu) → gộp theo R-15: `dong_doi_chieu` → cột `JSON` của `phieu_doi_chieu`

`id`, `phieu_id`, `thu_tu`, `nguon` (`DON_HIEN_CO`, `DON_RA_VIEN`, `THUOC_DANG_DUNG_TIEP_NHAN`), `don_thuoc_id`, `muc_thuoc_dang_dung_id`, `mo_ta_thuoc` (tự do cho đơn ra viện), `quyet_dinh` (`TIEP_TUC`, `NGUNG`, `THAY_DOI_LIEU`, `THEM_MOI`), `ly_do`, `don_moi` (`JSON` theo FR-004, kèm cơ sở kê), `nguoi_ghi_id`, `ghi_luc`, `da_ngung_ngoai_phieu`, `don_tao_id`. `UNIQUE (phieu_id, don_thuoc_id)`, `UNIQUE (phieu_id, muc_thuoc_dang_dung_id)`. Vấn đề FR-007 lưu ở `van_de_don_thuoc` với `dong_doi_chieu_id`. Trigger: chặn `INSERT/UPDATE/DELETE` khi phiếu cha không ở `MO`, trừ cột `don_tao_id` trong lệnh xác nhận và `da_ngung_ngoai_phieu`. *(R-15: `van_de_don_thuoc` → `su_kien_chung`)*

### `de_nghi_dinh_chinh_phieu` – nhóm 2 → gộp theo R-15: `de_nghi_dinh_chinh_phieu` → `ho_so_chung`

`id`, `phieu_id`, `noi_dung_dung` (`JSON`), `ly_do`, `nguoi_lap_id` (phải là người gửi hoặc người xác nhận của phiếu), `trang_thai` (`CHO_XAC_NHAN`, `DA_XAC_NHAN`, `TRA_LAI`), `nguoi_xac_nhan_id` (`CHECK <> nguoi_lap_id`), `dinh_chinh_id` (000). Nguồn: FR-039, BR-M07-17.

---

## 8. Thuốc gia đình gửi (mục G)

### `thuoc_gia_dinh_gui` – nhóm 2 (một lô mỗi lần tiếp nhận)

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `nct_id` | `BIGINT` | | FR-040 |
| `thuoc_id` | `BIGINT NULL` FK | Rỗng khi ghi tự do tới lúc Xác nhận sử dụng | FR-040 (CHK015) |
| `ten_tu_do`, `hoat_chat_tu_do`, `ham_luong` | `VARCHAR NULL` | | FR-040 |
| `so_luong_ban_dau` | `DECIMAL(10,2)` | Số lượng lúc tiếp nhận, không đổi sau đó; không có `so_luong_con` (Q-393) | FR-045, BR-M07-11 |
| `don_vi`, `han_dung`, `tinh_trang_bao_bi`, `co_toa`, `tep_ids` | | | FR-040 |
| `nguoi_giao_loai` | `ENUM('NGUOI_THAN','TU_DO','NHAN_VIEN_MUA_HO')` | | FR-040 |
| `nguoi_giao_id`, `nguoi_giao_ten`, `quan_he` | | | FR-040 |
| `de_nghi_mua_ho_id` | `BIGINT NULL` | Với thuốc mua hộ | Q-139 |
| `nguoi_nhan_id`, `tiep_nhan_luc` | | | FR-040 |
| `trang_thai` | `ENUM('CHO_DOI_CHIEU','DUOC_SU_DUNG','CHI_GIU_HO','DA_DUNG_HET','DA_HOAN_TRA','DA_HUY_THEO_YEU_CAU')` | | FR-041 |
| `ly_do_trang_thai` | `VARCHAR(2000) NULL` | Chỉ giữ hộ, Ngừng sử dụng, hết hạn dùng | FR-041 |
| `so_luong_ket_thuc`, `ket_thuc_luc`, `ket_thuc_boi_id`, `nguoi_nhan_tra_ten`, `quan_he_nguoi_nhan_tra`, `nguoi_chung_kien` | `NULL` | Ghi một lần khi Hoàn trả hoặc Hủy: số lượng đếm thực tế, người nhận (hoàn trả) hoặc người chứng kiến (hủy) (Q-393) | FR-041, FR-045 |

Trigger `trg_tgdg_so_luong` (`BEFORE UPDATE`): chặn đổi `so_luong_ban_dau`; chặn mọi đổi khi trạng thái cũ là cuối (FR-042). *(Sửa 2026-10-29, Q-393: bỏ cột số lượng còn và biến động.)* Chỉ mục `(nct_id, trang_thai)`, `(trang_thai, han_dung)`.

**Bảng chuyển** (khớp bảng FR-041):

| Từ | Lệnh / sự kiện | Đến | Nơi thực hiện |
| --- | --- | --- | --- |
| — | Tiếp nhận | `CHO_DOI_CHIEU` | `POST /nguoi-cao-tuoi/{nctId}/thuoc-gia-dinh-gui` |
| `CHO_DOI_CHIEU`, `CHI_GIU_HO` | Xác nhận sử dụng (gắn đơn) | `DUOC_SU_DUNG` | `POST /thuoc-gia-dinh-gui/{id}/xac-nhan-su-dung` |
| `CHO_DOI_CHIEU` | Chỉ giữ hộ | `CHI_GIU_HO` | `POST /thuoc-gia-dinh-gui/{id}/chi-giu-ho` |
| `DUOC_SU_DUNG` | Ngừng sử dụng | `CHI_GIU_HO` | `POST /thuoc-gia-dinh-gui/{id}/ngung-su-dung` |
| `DUOC_SU_DUNG` | Đơn gắn kết thúc không có đơn thay thế cùng thuốc | `CHI_GIU_HO` | Lệnh đơn; job |
| `DUOC_SU_DUNG` | Qua ngày hạn dùng | `CHI_GIU_HO` | `ViecHangNgayM07` |
| `DUOC_SU_DUNG` | Ghi dùng hết | `DA_DUNG_HET` | `POST /thuoc-gia-dinh-gui/{id}/ghi-dung-het` |
| `DUOC_SU_DUNG` | Báo gia đình sắp hết (không đổi trạng thái) | `DUOC_SU_DUNG` | `POST /thuoc-gia-dinh-gui/{id}/bao-sap-het` |
| `CHO_DOI_CHIEU`, `CHI_GIU_HO` | Hoàn trả | `DA_HOAN_TRA` | `POST /thuoc-gia-dinh-gui/{id}/hoan-tra` |
| `CHO_DOI_CHIEU`, `CHI_GIU_HO` | Hủy theo yêu cầu gia đình | `DA_HUY_THEO_YEU_CAU` | `POST /thuoc-gia-dinh-gui/{id}/huy` |

### `thuoc_gui_don` – nhóm 2 (liên kết) → gộp theo R-15: `thuoc_gui_don` → cột `JSON` của `thuoc_gia_dinh_gui`

`lo_id`, `don_thuoc_id`, `tu`, `den` (rỗng khi còn gắn). Một đơn nhiều lô, một lô nhiều đơn cùng thuốc cùng người (FR-043, BR-M07-18; không tính số ngày còn, Q-393). Liên kết chuyển sang đơn thay thế: đóng dòng cũ (`den`), thêm dòng mới trong cùng lệnh.

### `bien_dong_thuoc_gui`, `moc_nhac_thuoc_gui` *(Bỏ theo Q-393)*

Không theo dõi số lượng còn: không có biến động số lượng, Kiểm kê điều chỉnh, tính số ngày còn và mốc nhắc tự động. Lần "Báo gia đình sắp hết" ghi nhật ký (`nhat_ky`, 000) và gửi thông báo qua 009, không có bảng riêng.

---

## 9. Vắng mặt và thuốc mang theo (mục H)

### `khoang_vang_thuoc` – nhóm 2 → gộp theo R-15: `khoang_vang_thuoc` → `ho_so_chung`

`id`, `nct_id`, `loai` (`TAM_VANG_KHONG_THUOC`, `TAM_VANG_MANG_THUOC`, `BENH_VIEN`, `CHUYEN_DI_MANG_THUOC`, `CHUYEN_DI_KHONG_THUOC`), `tu`, `den_du_kien`, `den_thuc_te`, `luot_vang_id`, `chuyen_id`, `lenh_trang_thai_id` (lịch sử trạng thái 001). Tối đa một khoảng mở mỗi người: cột sinh `mo_nct = IF(den_thuc_te IS NULL, nct_id, NULL)` `UNIQUE`. Nguồn: FR-048, BR-M07-03.

### `lan_giao_thuoc` – nhóm 3 (trạng thái do hệ thống) → gộp theo R-15: `lan_giao_thuoc` → `lan_giao_nhan_thuoc` (`medication_handover`)

`id`, `nct_id`, `khoang_vang_id`, `nguoi_nhan_loai` (`NHAN_VIEN`, `TRUONG_DOAN`, `NGUOI_THAN`), `nguoi_nhan_id`, `nguoi_nhan_ten`, `quan_he`, `nguoi_giao_id`, `thoi_diem`, `trang_thai` (`DANG_MANG_THEO`, `CHO_NHAN_LAI`, `DA_NHAN_LAI`). Chỉ cột `trang_thai` đổi được, theo bảng FR-048a (trigger). `lan_giao_thuoc_dong`: `lan_giao_id`, `don_thuoc_id`, `so_luong`, `lo_id`, `nguon_thuoc`. `lan_giao_lieu`: `lan_giao_id`, `lieu_thuoc_id` (`UNIQUE`), `thoi_thuoc_luc` (liều quay về khi trở về sớm, Q-60). Đính chính `M07.LAN_GIAO_THUOC`.

### `lan_nhan_lai_thuoc` – nhóm 3 → gộp theo R-15: `lan_nhan_lai_thuoc` → `lan_giao_nhan_thuoc` (`medication_handover`)

`id`, `lan_giao_id` (`UNIQUE`), `nguoi_ghi_id`, `thoi_diem`, `ly_do` (bắt buộc khi tổng 0); `lan_nhan_lai_thuoc_dong`: `don_thuoc_id`, `so_luong` (`CHECK >= 0`; ≤ số giao kiểm ở backend). Đính chính `M07.LAN_NHAN_LAI_THUOC`.

### `lenh_khong_mang_thuoc` – nhóm 3 → gộp theo R-15: `lenh_khong_mang_thuoc` → `lan_giao_nhan_thuoc` (`medication_handover`)

`id`, `nct_id`, `chuyen_id`, `ly_do`, `nguoi_ghi_id`, `thoi_diem`. `UNIQUE (nct_id, chuyen_id)`. Nguồn: FR-048b.

---

## 9a. Tiếp nhận thuốc cung ứng (mục I) *(bổ sung 2026-10-11, Q-337)*

### `lan_tiep_nhan_thuoc_cung_ung` – nhóm 3 → gộp theo R-15: `lan_tiep_nhan_thuoc_cung_ung` → `lan_giao_nhan_thuoc` (`medication_handover`)

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | | |
| `yeu_cau_mua_sam_id`, `dong_yeu_cau_id` | `BIGINT` | Dòng của yêu cầu loại "thuốc" (019); không khóa ngoại sang bảng của M11, kiểm qua `YeuCauThuocCungUngPort` khi ghi | FR-054, BR-M11-17 |
| `nct_id`, `don_thuoc_id` | `BIGINT` | Chép từ `DongYeuCauThuocView` lúc ghi | FR-054 |
| `thuoc_id` | `BIGINT` | Lấy từ `don_thuoc.thuoc_id` của `don_thuoc_id` (006 tự tra; dòng yêu cầu của 019 không có id thuốc) | FR-054 |
| `ham_luong`, `dang_thuoc`, `don_vi` | `VARCHAR` | Điều dưỡng ghi khi kiểm (giá trị thực nhận, so với dòng yêu cầu) | FR-054 |
| `so_luong_nhan` | `DECIMAL(12,3)` | `CHECK (so_luong_nhan > 0)` | FR-054 |
| `han_dung` | `DATE` | | FR-054 |
| `tinh_trang` | `VARCHAR(500)` | | FR-054 |
| `ket_qua` | `ENUM('DAT','KHONG_DAT')` | | FR-054 |
| `sai_lech`, `ly_do` | `VARCHAR(2000) NULL` | `CHECK (ket_qua = 'DAT' OR (sai_lech IS NOT NULL AND ly_do IS NOT NULL))` | FR-054 |
| `nguoi_giao`, `nguoi_nhan_id`, `thoi_diem` | | | FR-054 |

Trigger nhóm 3 của 000 chặn `UPDATE`, `DELETE`; sai sót sửa bằng đính chính `M07.LAN_TIEP_NHAN_THUOC`. Bảng không có số lượng tồn, không phát sự kiện tính phí, không đổi `don_thuoc`, `lieu_thuoc` (FR-054, BR-M07-24). "Đã nhận" của yêu cầu là số liệu tính: mọi dòng có ít nhất một lần `DAT`.

## 10. Bảng kỹ thuật

- `lan_chay_m07 (viec, moc)`: khóa chính hai cột; `viec` ∈ `NCT_CHUA_PHU_TRACH` (mốc = `caId:nctId`), `TANG_KHONG_DIEU_DUONG` (mốc = `tang:caId`), `HET_HAN_DON`, `NHAC_HET_HAN_DON`, `HET_HAN_DUNG` (mốc = ngày); `ket_qua`, `lan_chay_lich_id` (000). Bảo đảm mỗi việc mỗi mốc một lần, kể cả chạy bù.

---

## 11. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bảo đảm | Cách làm | DBR / FR |
| --- | --- | --- |
| Liều duy nhất theo (đơn, thời điểm dự kiến); một lần xác nhận | `UNIQUE (don_thuoc_id, thoi_diem_du_kien)`; `UNIQUE (lieu_thuoc_id)` trên `ket_qua_lieu` | DBR-13, FR-019, BR-M07-12 |
| Đơn hiệu lực không đổi thuốc, liều, tần suất; đơn thay thế trỏ đơn cũ, tối đa một | Trigger `trg_don_thuoc_khoa`; `UNIQUE (thay_the_cho_id)`; đổi liều trong một transaction | DBR-14, FR-014, FR-016a |
| Trạng thái cuối của đơn, liều, phiếu, lô không đổi | Trigger trên bốn bảng | FR-013, FR-022, FR-039, FR-042 |
| Không lập sẵn liều cho ngày tới | Chỉ job tại đầu cửa sổ và lệnh của actor thêm dòng `lieu_thuoc`; test SC-001 đếm dòng có `cua_so_tu > bây giờ` không gắn lần giao = 0 | Q-306, SC-001 |
| Tối đa một phiếu mở; một phiếu tiếp nhận chưa hủy | Cột sinh `mo_nct`, `tiep_nhan_nct` `UNIQUE` | FR-034a, FR-035 |
| Quy tắc hai người của phiếu | `CHECK` người xác nhận ≠ người gửi; backend kiểm khác mọi người ghi dòng | FR-038, SC-014 |
| Số lượng lúc tiếp nhận không đổi | Trigger `trg_tgdg_so_luong` | FR-045, SC-007 (Q-393) |
| Một khoảng vắng mở mỗi người | Cột sinh `mo_nct` `UNIQUE` | FR-048 |
| Kết quả, lần dùng PRN, biến động, lần giao, lần nhận lại, căn cứ chỉ thêm; đính chính trỏ đúng một bản gốc | Trigger nhóm 3 của 000; `dinh_chinh` của 000 | DBR-23, FR-030 |
| Chi phí có nguồn | Sự kiện `SuKienTinhPhiThuoc` mang loại nguồn và id; 010 giữ DBR-15 | DBR-15, FR-033 |
| Lần tiếp nhận thuốc cung ứng chỉ thêm; Không đạt có sai lệch, lý do; chỉ gắn yêu cầu đã ghi đã mua | Trigger nhóm 3; `CHECK` ở mục 9a; backend kiểm trạng thái yêu cầu qua `YeuCauThuocCungUngPort` | FR-054, BR-M07-24, BR-M11-17 |
| Thời gian | `Clock` của 000; `thoi_diem_thiet_bi` cho ngoại tuyến | DBR-25 |

---

## 12. Nhật ký và phân loại trường

- Hành động theo research D-20.
- `PhanLoaiTruongProvider`:
  - `SUC_KHOE`: thuốc, hoạt chất, liều, đường dùng, ghi chú hướng dẫn, phản ứng, lý do, vấn đề kiểm tra, nội dung dòng phiếu, tên thuốc gia đình gửi;
  - `CONG_KHAI_NOI_BO`: trạng thái, thời điểm, người thực hiện.
- Hành chính, Bếp, Nhân viên chăm sóc không thấy thông tin thuốc (FR-049, 19.3). Trưởng tầng thấy liều và trạng thái liều trong phạm vi, không thấy đơn và phiếu.

---

## 13. Lưu giữ (000 R-11)

| Loại | `LoaiHan` | Mốc |
| --- | --- | --- |
| `M07.DON_THUOC` (cùng bảng con), `M07.LIEU_THUOC` (cùng kết quả, xem lại), `M07.LAN_DUNG_PRN`, `M07.PHIEU_DOI_CHIEU` (cùng dòng, đề nghị đính chính), `M07.THUOC_GIA_DINH_GUI` (cùng liên kết, biến động), `M07.LAN_GIAO_THUOC` (cùng dòng, nhận lại), `M07.KHOANG_VANG_THUOC`, `M07.LENH_KHONG_MANG_THUOC` | `THEO_HO_SO` | Thời điểm trạng thái cuối của người cao tuổi (CFG-M15-03) |

Danh mục thuốc không bị loại bỏ.

---

## 14. Truy vết DBR, BR và FR

| Mã | Bảo đảm (mục) | Test (quickstart mục 4) |
| --- | --- | --- |
| DBR-13 | Mục 5, 11 | `DBR_13_LieuDuyNhatIT`, `FR_029_HaiBanNgoaiTuyenIT` |
| DBR-14 | Mục 4, 11 | `DBR_14_DonThayTheIT`, `DonThuocTriggerIT` |
| DBR-15, DBR-17 (phần 006) | Sự kiện tính phí, đính chính, nhận lại | `BR_M07_14_SuKienTinhPhiIT`, `FR_030_DinhChinhChiPhiIT` |
| DBR-23, DBR-25 | Trigger 000; `Clock`; `thoi_diem_thiet_bi` | `NhomBaTriggerIT` (000) mở rộng, `FR_029_DongBoMuonIT` |
| DBR-03 (phần 006) | `ThuocCongNguoiThanApi` theo bản đồng ý | `FR_049_NguoiThanXemThuocIT` |
| BR-M07-01 | `LichThuoc.tinh`, vật hóa ở đầu cửa sổ | `BR_M07_01_LichThuocTest`, `SC_001_BayNgayIT` |
| BR-M07-02 | Job theo phút | `BR_M07_02_TreBoLoIT` |
| BR-M07-03 | `khoang_vang_thuoc`, bean | `BR_M07_03_VangMatIT` *(R-15: `khoang_vang_thuoc` → `ho_so_chung`)* |
| BR-M07-04 | `KiemTraGioiHanPrn` | `BR_M07_04_GioiHanPrnTest`, `PrnDongThoiIT` |
| BR-M07-05 | Quyền kê (D-05) | `BR_M07_05_QuyenKeDonIT` |
| BR-M07-06 | `KiemTraDonMoi`; listener dị ứng | `BR_M07_06_TrungHoatChatDiUngTest`, `FR_012_DiUngMoiIT` |
| BR-M07-07 | Đổi liều, ngừng; Đã hủy tính và vật hóa | `BR_M07_07_DoiLieuIT` |
| BR-M07-08 | Job hằng ngày | `BR_M07_08_HetHanDonIT` |
| BR-M07-09 | Bean, phiếu, hạn | `BR_M07_09_DoiChieuIT` |
| BR-M07-10 | Bảng chuyển lô; chặn chọn lô | `BR_M07_10_ThuocGuiChuaDoiChieuIT` |
| BR-M07-11 | Biến động, `TinhSoNgayConLai`, mốc nhắc | `BR_M07_11_SoNgayConLaiTest`, `ThuocGuiSapHetIT` |
| BR-M07-12 | `ket_qua_lieu`, đính chính | `BR_M07_12_XacNhanMotLanIT` |
| BR-M07-13 | `DemTuChoiLienTiep`; phản ứng | `BR_M07_13_TuChoiLienTiepTest`, `PhanUngIT` |
| BR-M07-14 | Sự kiện tính phí theo nguồn | `BR_M07_14_SuKienTinhPhiIT` |
| BR-M07-15 | Ngừng đơn thay thế Chờ hiệu lực | `BR_M07_15_HuyDoiLieuIT` |
| BR-M07-16 | `can_cu_co_so_ke` | `BR_M07_16_CanCuCoSoKeIT` *(R-15: `can_cu_co_so_ke` → `su_kien_chung`)* |
| BR-M07-17 | `de_nghi_dinh_chinh_phieu` | `BR_M07_17_DinhChinhPhieuIT` *(R-15: `de_nghi_dinh_chinh_phieu` → `ho_so_chung`)* |
| BR-M07-18 | `thuoc_gui_don`; nhóm dùng chung | `BR_M07_18_LoDungChungTest` *(R-15: `thuoc_gui_don` → cột `JSON` của `thuoc_gia_dinh_gui`)* |
| BR-M07-19 | Hoàn trả, hủy bằng số còn | `BR_M07_19_HoanTraIT` |
| BR-M07-20 | Handler ngoại tuyến | `FR_029_HaiBanNgoaiTuyenIT` |
| BR-M07-21 | Job đầu ca | `BR_M07_21_TangKhongDieuDuongIT` |
| BR-M07-22 | Nhiều lần giao; listener hồ sơ ban đầu | `BR_M07_22_NhieuLanGiaoIT`, `ThuocDangDungDoiIT` |
| BR-M07-23 | Không kiểm tra tương tác, liều tối đa (ngoài phạm vi) | Không có test (không có chức năng) |
| BR-M07-24 *(bổ sung 2026-10-11)* | Mục 9a; research D-21 | `BR_M07_24_TiepNhanThuocCungUngIT` |
| BR-M07-25 *(bổ sung 2026-10-11)* | Mục 7 (`ly_do = THEO_YEU_CAU_CHUYEN_MON`); research D-13 | `BR_M07_25_DoiChieuChuyenMonIT` |
| BR-M04-16 (phần 006) | Chỉ Điều dưỡng ghi liều Mang theo | `FR_027_MangTheoBaoLaiIT` |
| BR-M06-05 (phần 006) | `QuyenApi` khóa `CHUYEN_MON` | `BR_M07_05_QuyenKeDonIT`, `FR_026_QuyenXacNhanIT` |
| BR-M15-20 (phần 006) | Kết quả đồng bộ từng bản ghi | Test Flutter `KetQuaDongBoThuocTest` |
