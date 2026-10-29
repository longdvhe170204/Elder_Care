# Data Model: Hồ sơ và đánh giá người cao tuổi (001)

**Ngày**: 2026-10-07, cập nhật 2026-10-18 (Q-347, Q-348, Q-353) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, trường, ràng buộc và cách bảo đảm từng quy tắc. Đây không phải script SQL; migration được viết ở giai đoạn triển khai. Quy ước kiểu dữ liệu, nhóm dữ liệu và bảo đảm ở cơ sở dữ liệu theo [../000-shared-foundation/data-model.md](../000-shared-foundation/data-model.md) mục 1.

Mọi bảng thuộc module M01 (`com.cambae.backend.m01`), trừ bảng `tep` thuộc `common` (research H-13). Module khác không đọc các bảng này, chỉ gọi giao diện ở [contracts/module-interfaces.md](contracts/module-interfaces.md). Tham chiếu tới người thân (012), sự cố (007), lượt vắng (004), yêu cầu phê duyệt (000) là định danh logic, không có khóa ngoại chéo module (constitution X).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp: `nguoi_cao_tuoi`; `muc_suc_khoe` (`health_item`); `ban_dong_y`; `nguyen_vong` = `nguyen_vong_cuoi_doi` + `nguyen_vong_dieu_tri` (cột `loai`); `danh_gia` (nhu cầu `danh_gia_nhu_cau` thành `JSON`).
- `danh_muc_chung`: `muc_cham_soc` (`care_level`), `allergen`, `allergy_severity`, `information_source`, `care_need`.
- `lich_su_chung`: `lich_su_trang_thai_nct`, `lich_su_muc_cham_soc` (`mien_dieu_kien` vào `chi_tiet`; lý do bắt buộc FR-046 kiểm ở backend; tệp `M01.GIAY_TO_BENH_VIEN` trỏ id mới).
- `su_kien_chung`: `nguyen_vong_y_kien`, `nguyen_vong_dieu_tri_y_kien`; cột `JSON` của bảng cha (ghi cùng lúc tạo): `ban_dong_y_nguoi_than`, `nguyen_vong_dieu_tri_nguoi_bao`.
- `ho_so_chung`: `ho_so_suc_khoe_ban_dau` (`initial_health_record`, khóa khi xác nhận), `lan_bao_danh_gia_lai` (`reassessment_notice`); `moc_da_nhac`: `nhac_chu_ky`, `canh_bao_tro_lai`.
- Còn 5 bảng riêng của feature.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-29; tên mã theo bảng đã cài, migration `V001_95`)*

Mô tả dưới đây thay các mục tương ứng khi cài 001 T084; cột không nhắc lại giữ như bảng đã cài (`V001_02` → `V001_10`). `file` (`tep`) thuộc `common`, giữ nguyên.

**`resident`** – giữ nguyên mọi cột, `UNIQUE`, `CHECK`, trigger. Bỏ khóa ngoại `current_care_level` → `care_level` (mức chăm sóc vào `danh_muc_chung` loại `M01.CARE_LEVEL`, khóa theo `ma`); backend kiểm mã qua `CatalogApi`.

**`health_item`** (`muc_suc_khoe`) – giữ nguyên mọi cột, `CHECK`, trigger `trg_health_item_immutable`. Khóa ngoại `allergen_id`, `severity_id`, `source_id` trỏ `danh_muc_chung.id` (id mới, đổi trong migration); backend kiểm đúng loại `M01.ALLERGEN`, `M01.ALLERGY_SEVERITY`, `M01.INFORMATION_SOURCE`.

**`consent`** (`ban_dong_y`) – giữ nguyên. Thêm `family_member_ids JSON NOT NULL` (thay `consent_family_member`, ghi cùng `INSERT`; quyền theo cột hiện có đã không cho sửa) và chỉ mục nhiều giá trị `((CAST(family_member_ids AS UNSIGNED ARRAY)))` cho tra "bản đồng ý gồm người thân X" (`JSON_CONTAINS`, `MEMBER OF`).

**`wish`** (`nguyen_vong`, mới) – nhóm 2 có phiên bản. Gộp `end_of_life_wish`, `treatment_wish`:

| Cột | Ghi chú |
| --- | --- |
| `id` | Giữ id của `end_of_life_wish`; nguyện vọng điều trị cấp id mới |
| `kind ENUM('END_OF_LIFE','TREATMENT')` | |
| `resident_id`, `note`, `respondent_type`, `respondent_family_member_id`, `signed_copy_file_id`, `status`, `superseded_by_id`, `recorded_by`, `recorded_at` | Cột chung; `status` gộp `ACTIVE`, `SUPERSEDED` (cuối đời), `EXPIRED` (điều trị) |
| Cột riêng của `END_OF_LIFE` | `choice`, `first_contact`, `relationship`, `assessing_doctor_id`, `can_self_determine`, `assessed_at`, `support_method`, `review_date`, `signer_type`, `signer_family_member_id`, `representative_selection`, `court_decision_*`, `signed_at`, `source`: `NULL` với `TREATMENT` |
| Cột riêng của `TREATMENT` | `hospital1_*`, `hospital2_*`, `insurance_*`, `ask_family_first`, `escort`: `NULL` với `END_OF_LIFE` |
| `notify_family_member_ids JSON NULL` | Thay `treatment_wish_notify` (chỉ `TREATMENT`, ghi cùng `INSERT`); chỉ mục nhiều giá trị |
| `active_key` | Cột sinh `IF(status = 'ACTIVE', CONCAT(kind, ':', resident_id), NULL)` `UNIQUE` (thay hai `active_resident_id`) |

Mọi `CHECK` cũ giữ, mỗi `CHECK` thêm điều kiện `kind = …` (ví dụ `kind <> 'END_OF_LIFE' OR can_self_determine = (signer_type = 'RESIDENT')`); thêm `CHECK` cột riêng của loại kia là `NULL`. Quyền `eldercare_app`: `INSERT`, `UPDATE (status, superseded_by_id)`; trigger `trg_wish_only_supersede` chỉ kiểm chuyển trạng thái (`ACTIVE` → `SUPERSEDED` với cuối đời, `ACTIVE` → `EXPIRED` với điều trị; `superseded_by_id` đặt một lần), các cột khác đã bị quyền theo cột chặn. Chỉ mục `(kind, status, review_date)`, `(resident_id, kind, id)`.

**`assessment`** (`danh_gia`) – giữ nguyên. Thêm `care_need_ids JSON NOT NULL DEFAULT (JSON_ARRAY())` (thay `assessment_care_need`); trigger `trg_assessment_no_update` hiện có đã chặn sửa khi `CONFIRMED`, `DISCARDED`, nên chặn cả đổi nhu cầu. Bỏ khóa ngoại `care_level` → `care_level` như `resident`.

**Loại ở bảng chung**:

| Bảng chung | `loai` | Đối tượng | Nội dung | `khoa_duy_nhat`, ràng buộc chuyển về backend |
| --- | --- | --- | --- | --- |
| `danh_muc_chung` | `M01.CARE_LEVEL` | — | `ma` = mã mức; `thu_tu` = `display_order`; nạp bằng migration | `UNIQUE (loai, ma)` |
| `danh_muc_chung` | `M01.ALLERGEN` | — | `{allergenGroup}` | `ten_hieu_luc` thay `active_name` |
| `danh_muc_chung` | `M01.ALLERGY_SEVERITY`, `M01.INFORMATION_SOURCE`, `M01.CARE_NEED` | — | `thu_tu` cho mức độ | như trên |
| `lich_su_chung` | thuộc tính `TRANG_THAI` | `NGUOI_CAO_TUOI` | `gia_tri_truoc`, `gia_tri` = trạng thái; `lenh`; `do_he_thong`; `ly_do`; `can_cu_*`; `chi_tiet` `{conditionWaiver}` | Lý do bắt buộc trừ `do_he_thong` hoặc `CREATE_PROFILE` (FR-046): kiểm ở `StatusHistoryApi` theo luật khai báo của loại, test `StatusHistoryIT`; `resident.status` bằng `gia_tri` dòng mới nhất (test hiện có) |
| `lich_su_chung` | thuộc tính `MUC_CHAM_SOC` | `NGUOI_CAO_TUOI` | `gia_tri` = mã mức; `tu` = `effective_from`; `can_cu_*` | `M01.CLH:<residentId>:<tu>` (thay `uq_care_level_history`) |
| `su_kien_chung` | `M01.WISH_OPINION` | `WISH` | `{sourceType, sourceFamilyMemberId, content}`; `nguoi_thuc_hien_id` = `recorded_by` | `sourceFamilyMemberId` có khi và chỉ khi `FAMILY_MEMBER`; nội dung không rỗng |
| `ho_so_chung` | `M01.INITIAL_HEALTH_RECORD` | `nct_id` | `{bloodType, mobility, selfCareAbility, cognition, initialNeedsNote, nutritionNeeds, currentMedications, rehabilitationNeeds, performedBy, performedAt, confirmedBy}`; `trang_thai` `DRAFT`, `CONFIRMED` | `M01.IHR:<residentId>` (một hồ sơ mỗi người); `khoa_luc` = thời điểm xác nhận (thay `trg_initial_health_record_no_update`) |
| `ho_so_chung` | `M01.REASSESSMENT_NOTICE` | `nct_id`; `loai_doi_tuong`, `doi_tuong_id` = nguồn | `{basis, notifiedAt, deadlineFrom, overdueAlertedAt}` | `M01.RAN:<residentId>:<basis>:<sourceType>:<sourceId>`; cột sinh `m01_deadline_from` có chỉ mục cho job quá hạn |
| `moc_da_nhac` | `M01.PERIODIC_REMINDER.<type>` | `NGUOI_CAO_TUOI` | `moc` = `START:<milestoneId>` khi bắt đầu, `<milestoneId>:<lần>` mỗi lần nhắc | Số lần, lần cuối tính từ các dòng (thay cột `reminder_count`, `last_reminded_at`) |
| `moc_da_nhac` | `M01.RETURN_OVERDUE_ALERT` | `LUOT_VANG` | `moc` = mốc dự kiến trở lại | |

**Chuyển dữ liệu (`V001_95`)**: bốn danh mục và `care_level` vào `danh_muc_chung`, đổi `health_item.allergen_id`, `severity_id`, `source_id` theo id mới; `resident_status_history`, `care_level_history` vào `lich_su_chung`, đổi `file.owner_id` của `M01.GIAY_TO_BENH_VIEN` và `source_id` của thông báo đánh giá lại loại `STATUS_HISTORY` theo id mới; `initial_health_record`, `reassessment_notice` vào `ho_so_chung`; hai bảng nguyện vọng vào `wish`, ý kiến vào `su_kien_chung`, danh sách người được báo và người thân của bản đồng ý vào JSON; `assessment_care_need` vào `assessment.care_need_ids`; `periodic_reminder`, `return_overdue_alert` vào `moc_da_nhac`. Dòng đổi id ghi `legacy`; `dinh_chinh` trỏ đánh giá, mục sức khỏe giữ id (hai bảng giữ nguyên).

**Bảo đảm** (thay phần tương ứng ở các mục sau): một bản đồng ý Hiệu lực mỗi người (`active_resident_id`), một nguyện vọng Hiệu lực mỗi loại mỗi người (`active_key`), một bản nháp đánh giá và một đánh giá đầu vào xác nhận mỗi người (cột sinh hiện có), một hồ sơ sức khỏe ban đầu mỗi người và không trùng thông báo đánh giá lại (`khoa_duy_nhat`). Test m01 hiện có phải xanh, giữ kịch bản nghiệp vụ và đổi bảng đích trong test (danh sách cuối mục); thêm test chọn mục danh mục sai loại bị từ chối.

**Bảng truy vết sau gộp** *(bổ sung 2026-10-30; khi làm task gộp, sửa cột "bảo đảm bằng" của bảng DBR, BR ở các mục sau theo bảng này)*:

| Mã | Bảo đảm sau gộp | Test |
| --- | --- | --- |
| DBR-05 | Xác nhận đánh giá bắt buộc nhận xét và mức chăm sóc: `CHECK` của `assessment` giữ; mã mức kiểm qua `CatalogApi` loại `M01.CARE_LEVEL` (bỏ khóa ngoại) | `BR_M01_09_AssessmentIT`; mới `CareLevelCatalogReferenceIT` (mã không có trong danh mục bị từ chối ở `resident`, `assessment`, M1) |
| DBR-39 | Một nguyện vọng Hiệu lực mỗi loại mỗi người: `wish.active_key` `UNIQUE` | `EndOfLifeWishVersionIT`, `TreatmentWishVersionIT` |
| BR-M01-14 | Nguồn ý kiến: `su_kien_chung` loại `M01.WISH_OPINION`, kiểm nguồn ở domain | `BR_M01_14_OpinionSourceIT` |
| BR-M01-01 | Một cảnh báo mỗi mốc trở lại: `moc_da_nhac` loại `M01.RETURN_OVERDUE_ALERT` | `BR_M01_01_TemporaryAbsenceOverdueIT` |
| BR-M01-02 | Không trùng thông báo đánh giá lại: `ho_so_chung` loại `M01.REASSESSMENT_NOTICE`, `khoa_duy_nhat`; cột sinh hạn có chỉ mục | `BR_M01_02_ReassessmentNoticeIT` |
| FR-046 | Lý do bắt buộc khi đổi trạng thái: `StatusHistoryApi` theo luật của thuộc tính `TRANG_THAI` | `StatusHistoryIT` |

**Test phải đổi bảng đích** *(rà 2026-10-30; 30 file đọc, ghi thẳng bảng cũ bằng SQL)*: giữ kịch bản nghiệp vụ, chỉ đổi bảng, cột đích.
- Test của module: `AssessmentCorrectionIT`, `AssessmentTestBase`, `BR_M01_02_ReassessmentNoticeIT`, `BR_M01_06_ModuleCommandsIT`, `BR_M01_09_AssessmentIT`, `BR_M01_11_CatalogIT`, `CatalogSchemaIT`, `CompleteAdmissionConsentIT`, `CompleteAdmissionWishWarningsIT`, `ConsentIT`, `CreateProfileIT`, `EndOfLifeWishSignerIT`, `EndOfLifeWishVersionIT`, `FR_023h_KhongDauNangLucIT`, `HealthEndpointsIT`, `HealthItemImmutableIT`, `HealthTestBase`, `InitialHealthRecordIT`, `M01AuditCompletenessIT`, `MissingConsentReminderIT`, `MissingEndOfLifeWishReminderIT`, `RecordDeathIT`, `ResidentSchemaIT`, `SelfDeterminationReviewReminderIT`, `StatusHistoryIT`, `StatusTestBase`, `TreatmentWishVersionIT`, `WishTestBase`.
- Test của m09 (chủ module review): `HandoverIT`, `StaffingRatioIT`.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Nội dung | FR |
| --- | --- | --- | --- |
| `nguoi_cao_tuoi` | 2 | Hồ sơ cá nhân, trạng thái, mức chăm sóc hiện hành | FR-001 → FR-009, FR-044, FR-052 |
| `lich_su_trang_thai_nct` | 3, chỉ thêm | Mỗi lần chuyển trạng thái | FR-002, FR-046 *(R-15: `lich_su_trang_thai_nct` → `lich_su_chung`)* |
| `muc_cham_soc` | 1, cố định | Ba mức chăm sóc (mục 4) | FR-029, FR-051 *(R-15: `muc_cham_soc` → `danh_muc_chung`)* |
| `lich_su_muc_cham_soc` | 3, chỉ thêm | Mức chăm sóc theo thời gian và căn cứ | FR-052, FR-053 *(R-15: `lich_su_muc_cham_soc` → `lich_su_chung`)* |
| `ban_dong_y`, `ban_dong_y_nguoi_than` | 2; bảng con chỉ thêm | Bản đồng ý và phạm vi người thân | FR-010 → FR-016 *(R-15: `ban_dong_y_nguoi_than` → cột `JSON` của `ban_dong_y`)* |
| `ho_so_suc_khoe_ban_dau` | 3 có bản nháp | Hồ sơ sức khỏe ban đầu | FR-017 *(R-15: `ho_so_suc_khoe_ban_dau` → `ho_so_chung`)* |
| `muc_suc_khoe` | 2, nội dung bất biến | Dị ứng, bệnh nền, tiền sử bệnh | FR-018 → FR-023 |
| `di_nguyen`, `muc_do_di_ung`, `nguon_thong_tin` | 1 | Danh mục dùng cho mục sức khỏe | FR-018, FR-018a, BR-M01-11 |
| `nguyen_vong_cuoi_doi`, `nguyen_vong_y_kien` | 2, có phiên bản; bảng con chỉ thêm | Phiếu nguyện vọng cuối đời; ý kiến khác có nguồn | FR-023a → FR-023f, FR-023g *(R-15: `nguyen_vong_cuoi_doi` → `nguyen_vong` (`wish`); `nguyen_vong_y_kien` → `su_kien_chung`)* |
| `nguyen_vong_dieu_tri`, `nguyen_vong_dieu_tri_nguoi_bao`, `nguyen_vong_dieu_tri_y_kien` | 2, có phiên bản; bảng con chỉ thêm | Phiếu nguyện vọng khi điều trị (bổ sung 2026-10-22) | FR-023j → FR-023m, DBR-39 *(R-15: `nguyen_vong_dieu_tri` → `nguyen_vong` (`wish`); `nguyen_vong_dieu_tri_nguoi_bao` → cột `JSON` của `nguyen_vong`; `nguyen_vong_dieu_tri_y_kien` → `su_kien_chung`)* |
| `danh_gia`, `danh_gia_nhu_cau` | 3 có bản nháp | Lần đánh giá và nhu cầu chăm sóc được chọn | FR-024 → FR-038 *(R-15: `danh_gia_nhu_cau` → cột `JSON` của `danh_gia`)* |
| `nhu_cau_cham_soc` | 1 | Danh mục nhu cầu chăm sóc (Q-291) | FR-025, FR-029 |
| `lan_bao_danh_gia_lai` | Kỹ thuật | Lần hệ thống báo bác sĩ cần đánh giá lại | FR-039 → FR-043 *(R-15: `lan_bao_danh_gia_lai` → `ho_so_chung`)* |
| `nhac_chu_ky` | Kỹ thuật | Lần nhắc cuối của các nhắc theo chu kỳ; nhắc ngày xem lại nhận định | FR-012, FR-023d, FR-023i, FR-036 *(R-15: `nhac_chu_ky` → `moc_da_nhac`)* |
| `canh_bao_tro_lai` | Kỹ thuật | Chống cảnh báo lặp khi tạm vắng quá hạn | FR-050 *(R-15: `canh_bao_tro_lai` → `moc_da_nhac`)* |
| `tep` (`common`) | Kỹ thuật | Tệp đính kèm | FR-010, FR-014, FR-023a, FR-023j, FR-047b, FR-047c |

"Cố định" là nạp từ migration, không có API tạo, sửa, xóa. "Kỹ thuật" là dữ liệu hạ tầng hoặc dẫn xuất, không thuộc ba nhóm của mục 1.5 (plan, Complexity Tracking).

---

## 2. Người cao tuổi và trạng thái (mục A, F)

### `nguoi_cao_tuoi` – nhóm 2

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | Khóa chính | |
| `ma_ho_so` | `VARCHAR(20)` | `UNIQUE`; `NCT-<năm>-<id>` (H-04) | FR-003, DBR-01 |
| `ho_so_truoc_id` | `BIGINT NULL` | Khóa ngoại tới `nguoi_cao_tuoi.id` | FR-004 |
| `ho_ten` | `VARCHAR(200)` | `NOT NULL` | FR-001 |
| `ho_ten_so_khop` | Cột sinh, `VARCHAR(200) COLLATE utf8mb4_0900_as_ci` | `TRIM` và gộp khoảng trắng của `ho_ten`; chỉ mục `(ho_ten_so_khop, ngay_sinh)` | FR-005 |
| `ngay_sinh` | `DATE` | `NOT NULL` | FR-001 |
| `gioi_tinh` | `ENUM('NAM','NU')` | `NOT NULL` | FR-001; Assumptions |
| `loai_giay_to` | `ENUM('CCCD','KHAC') NULL` | | FR-001 |
| `so_giay_to` | `VARCHAR(30) NULL` | `CHECK ((loai_giay_to IS NULL) = (so_giay_to IS NULL))` | |
| `cccd_dang_mo` | Cột sinh | `IF(loai_giay_to='CCCD' AND trang_thai NOT IN ('KET_THUC_LUU_TRU','QUA_DOI','HUY_TIEP_NHAN'), so_giay_to, NULL)`; `UNIQUE` | DBR-01 |
| `anh_tep_id` | `BIGINT NULL` | Tệp ảnh (H-13) | FR-001 |
| `dia_chi_thuong_tru`, `dia_chi_lien_he` | `VARCHAR(500) NULL` | | FR-001, Q-244 |
| `lien_he` | `JSON NULL` | Số điện thoại, thư điện tử | FR-001 |
| `thong_tin_dac_biet` | `TEXT NULL` | Văn bản tự do | FR-001, FR-018 |
| `trang_thai` | `ENUM` 8 mã ở bảng dưới | `NOT NULL` | FR-044, DBR-01 |
| `trang_thai_tu_luc` | `DATETIME(6)` | Thời điểm vào trạng thái hiện tại | FR-046 |
| `muc_cham_soc_hien_hanh` | `VARCHAR(20) NULL` | Khóa ngoại `muc_cham_soc.ma`; `NULL` tới khi đánh giá đầu vào được xác nhận | FR-033, FR-052 |
| `phien_ban` | `INT` | Khóa lạc quan, chỉ dùng cho Cập nhật thông tin cá nhân | FR-006 |
| `tao_luc`, `tao_boi` | | | |

Mỗi hồ sơ có đúng một trạng thái hiện tại vì `trang_thai NOT NULL` (DBR-01). Loại hình lưu trú hiện hành không lưu ở đây; nó lấy từ hợp đồng Hiệu lực của 004 (FR-052, H-10).

**Mã trạng thái**:

| Mã | Tên trong spec | Trạng thái cuối |
| --- | --- | --- |
| `DANG_TIEP_NHAN` | Đang tiếp nhận | |
| `DANG_LUU_TRU` | Đang lưu trú | |
| `TAM_VANG` | Tạm vắng | |
| `HOAT_DONG_BEN_NGOAI` | Hoạt động bên ngoài | |
| `DIEU_TRI_BENH_VIEN` | Điều trị tại bệnh viện | |
| `KET_THUC_LUU_TRU` | Kết thúc lưu trú | ✔ |
| `QUA_DOI` | Qua đời | ✔ |
| `HUY_TIEP_NHAN` | Hủy tiếp nhận | ✔ |

### Bảng chuyển trạng thái (FR-044 → FR-050)

Cài đúng bảng trạng thái của spec. Quy ước chung cho các lệnh (research H-02):
- endpoint `POST /nguoi-cao-tuoi/{id}/<lenh>`, thân gồm `lyDo` và `chiTiet`;
- khóa dòng `FOR UPDATE`;
- `HoSoMoGuard`;
- gộp điều kiện của mọi bean `ThamGiaChuyenTrangThai`.

| Từ | Lệnh (mã) | Đến | Endpoint / nguồn | Người thực hiện (khóa quyền) | Điều kiện M01 tự kiểm | Nhật ký |
| --- | --- | --- | --- | --- | --- | --- |
| — | `TAO_HO_SO` | `DANG_TIEP_NHAN` | `POST /nguoi-cao-tuoi` | AC-09 (`HO_SO_NGUOI_CAO_TUOI, TAO`) | FR-003, FR-005 | `NCT_TAO` |
| `DANG_TIEP_NHAN` | `HOAN_TAT_TIEP_NHAN` | `DANG_LUU_TRU` | `…/hoan-tat-tiep-nhan` | AC-09 (`TIEP_NHAN_HOP_DONG_DAT_COC, TAO`; sửa tên 2026-10-18 cho khớp 004 research T-14) | Đánh giá đầu vào Đã xác nhận; lần Đã xác nhận gần nhất chưa quá CFG-M01-02 (FR-034a) | `NCT_CHUYEN` |
| `DANG_TIEP_NHAN` | `HUY_TIEP_NHAN` | `HUY_TIEP_NHAN` | `…/huy-tiep-nhan` | AC-09 | Lý do | `NCT_CHUYEN` |
| `DANG_LUU_TRU` | `CHO_TAM_VANG` | `TAM_VANG` | `…/cho-tam-vang` | AC-09, AC-02 (`TAM_VANG_TRO_VE, TAO`) | Lý do; không qua phê duyệt (FR-047a) | `NCT_CHUYEN` |
| `DANG_LUU_TRU` | `DIEM_DANH_ROI_VIEN` | `HOAT_DONG_BEN_NGOAI` | `TrangThaiNguoiCaoTuoiApi` từ 014 | Người điểm danh; `doHeThong` | — | `NCT_CHUYEN` |
| `DANG_LUU_TRU`, `TAM_VANG`, `HOAT_DONG_BEN_NGOAI` | `CHUYEN_VIEN` | `DIEU_TRI_BENH_VIEN` | `…/chuyen-vien`; hoặc API từ 007 (H-03) | AC-03, AC-04 (`CHUYEN_VIEN, TAO`) | Lý do; `chiTiet.canCu` là sự cố hoặc chỉ định (CHK016) | `NCT_CHUYEN` |
| `TAM_VANG` | `GHI_NHAN_TRO_VE` | `DANG_LUU_TRU` | `…/ghi-nhan-tro-ve` | AC-09, AC-02 | Lý do | `NCT_CHUYEN` |
| `HOAT_DONG_BEN_NGOAI` | `DIEM_DANH_VE`, `HUY_GHI_NHAN_ROI_VIEN` | `DANG_LUU_TRU` | API từ 014 | Người điểm danh; Trưởng tầng đính chính | — | `NCT_CHUYEN` |
| `DIEU_TRI_BENH_VIEN` | `GHI_NHAN_TRO_VE` | `DANG_LUU_TRU` | `…/ghi-nhan-tro-ve` | AC-09, AC-02 | Lý do. Tác động M01: đặt `han_tinh_tu`, thêm lần báo `TRO_VE_BENH_VIEN` (H-11) | `NCT_CHUYEN` |
| `DANG_LUU_TRU`, `TAM_VANG`, `DIEU_TRI_BENH_VIEN` | `KET_THUC_LUU_TRU` | `KET_THUC_LUU_TRU` | `…/ket-thuc-luu-tru` | AC-09, AC-03 (`KET_THUC_QUA_DOI, TAO`) | Lý do | `NCT_CHUYEN` |
| `DANG_LUU_TRU`, `TAM_VANG`, `HOAT_DONG_BEN_NGOAI`, `DIEU_TRI_BENH_VIEN` | `GHI_NHAN_QUA_DOI` | `QUA_DOI` | `…/ghi-nhan-qua-doi` | AC-03; AC-09 chỉ khi trạng thái hiện tại khác `DANG_LUU_TRU` | FR-047b: AC-09 bắt buộc `chiTiet.bangChungTepId` | `NCT_CHUYEN` |

Chuyển không có trong bảng trả 409 `CHUYEN_KHONG_HOP_LE`. Điều kiện và tác động của module khác theo research H-02 mục 4.

### `lich_su_trang_thai_nct` – nhóm 3, chỉ thêm → gộp theo R-15: `lich_su_trang_thai_nct` → `lich_su_chung`

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `id` | `BIGINT` | Khóa chính |
| `nguoi_cao_tuoi_id` | `BIGINT` | Khóa ngoại; chỉ mục `(nguoi_cao_tuoi_id, thoi_diem)` |
| `tu_trang_thai` | `ENUM NULL` | `NULL` với `TAO_HO_SO` |
| `den_trang_thai` | `ENUM` | |
| `lenh` | `VARCHAR(30)` | Mã lệnh ở bảng trên |
| `thoi_diem` | `DATETIME(6)` | Đồng hồ máy chủ |
| `nguoi_thuc_hien_id` | `BIGINT` | Tài khoản; với lệnh từ 014 là người điểm danh |
| `do_he_thong` | `BOOLEAN` | `true` với ba lệnh của 014 (CHK046) |
| `ly_do` | `VARCHAR(2000) NULL` | `CHECK (ly_do IS NOT NULL OR do_he_thong OR lenh = 'TAO_HO_SO')` (FR-046) |
| `can_cu_loai`, `can_cu_id` | `NULL` | Sự cố, chuyến đi, chỉ định, bằng chứng |
| `mien_dieu_kien` | `JSON NULL` | Yêu cầu ngoại lệ đã duyệt và các điều kiện được miễn (US4 #10) |

*(Bổ sung 2026-10-22, FR-047c)* Giấy ra viện, đơn thuốc (lệnh `GHI_NHAN_TRO_VE` từ `DIEU_TRI_BENH_VIEN`) và giấy chuyển viện (lệnh `CHUYEN_VIEN` của 007) không thêm cột: tệp gắn vào dòng lịch sử qua `tep.chu_so_huu_loai = M01.GIAY_TO_BENH_VIEN`, `chu_so_huu_id = lich_su_trang_thai_nct.id` (research H-18).

Trigger `trg_lich_su_trang_thai_nct_khong_sua` và `trg_lich_su_trang_thai_nct_khong_xoa` theo mẫu của 000. Không có cột nháp; tài khoản `eldercare_app` không có quyền `UPDATE`. Test tích hợp kiểm `nguoi_cao_tuoi.trang_thai` luôn bằng `den_trang_thai` của dòng lịch sử mới nhất.

---

## 3. Mức chăm sóc (mục G)

### `muc_cham_soc` – nhóm 1, cố định → gộp theo R-15: `muc_cham_soc` → `danh_muc_chung`

| `ma` | `ten` | `thu_tu` |
| --- | --- | --- |
| `CO_BAN` | Chăm sóc cơ bản | 1 |
| `THUONG_XUYEN` | Chăm sóc thường xuyên | 2 |
| `DAC_BIET` | Chăm sóc đặc biệt | 3 |

Nguồn: mục 4, Q-291. Dịch vụ chăm sóc theo từng mức và đơn giá thuộc 004 (6.4). Ràng buộc phòng – mức thuộc 003 (BR-M03-01, FR-051).

### `lich_su_muc_cham_soc` – nhóm 3, chỉ thêm → gộp theo R-15: `lich_su_muc_cham_soc` → `lich_su_chung`

| Cột | Ràng buộc |
| --- | --- |
| `id`, `nguoi_cao_tuoi_id` | Chỉ mục `(nguoi_cao_tuoi_id, hieu_luc_tu)` |
| `muc_cham_soc` | Khóa ngoại `muc_cham_soc.ma` *(R-15: `muc_cham_soc` → `danh_muc_chung`)* |
| `hieu_luc_tu` | `DATETIME(6)`. Đánh giá: thời điểm xác nhận. Yêu cầu thay đổi lưu trú: 00:00 của ngày hiệu lực thực tế (BR-M15-07) |
| `can_cu_loai` | `ENUM('DANH_GIA','YEU_CAU_THAY_DOI_LUU_TRU')` |
| `can_cu_id` | Lần đánh giá, hoặc yêu cầu phê duyệt của 004 |

`UNIQUE (nguoi_cao_tuoi_id, hieu_luc_tu)`. Lệnh ghi cập nhật `nguoi_cao_tuoi.muc_cham_soc_hien_hanh` và thêm dòng lịch sử trong cùng transaction. Giá trị tại ngày D là dòng có `hieu_luc_tu` lớn nhất mà `<=` cuối ngày D (FR-053, US6 #11).

Nhu cầu chăm sóc hiện hành không lưu riêng. Nó là tập `danh_gia_nhu_cau` của lần Đã xác nhận gần nhất (Key Entities). *(R-15: `danh_gia_nhu_cau` → cột `JSON` của `danh_gia`)*

---

## 4. Bản đồng ý (mục B)

### `ban_dong_y` – nhóm 2

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id`, `nguoi_cao_tuoi_id` | | | |
| `nguoi_dong_y_loai` | `ENUM('NGUOI_CAO_TUOI','DAI_DIEN_PHAP_LUAT')` | | FR-010, FR-011 (sửa 2026-10-18, Q-348) |
| `nguoi_dong_y_nguoi_than_id` | `BIGINT NULL` | Bắt buộc khi `DAI_DIEN_PHAP_LUAT` (`CHECK`); người thân có quan hệ Hiệu lực với người cao tuổi (012) | FR-011 |
| `qd_toa_so`, `qd_toa_ngay`, `qd_toa_toa_an`, `qd_toa_tep_id` | `VARCHAR(100)`, `DATE`, `VARCHAR(200)`, `BIGINT`; đều `NULL` | Bắt buộc cả bốn khi `DAI_DIEN_PHAP_LUAT`, rỗng cả bốn khi `NGUOI_CAO_TUOI` (`CHECK`) | FR-011 (Q-348) |
| `dong_y_hinh_anh`, `dong_y_thong_bao` | `BOOLEAN` | `dong_y_thong_bao` áp cho thông báo không khẩn về người cao tuổi (Q-353); 009 đọc qua `BanDongYApi` | FR-010 |
| `thoi_diem_dong_y` | `DATETIME(6)` | | FR-010 |
| `bang_chung_tep_id` | `BIGINT` | `NOT NULL` | FR-010, US5 #2 |
| `trang_thai` | `ENUM('HIEU_LUC','DA_RUT_LAI')` | | |
| `dang_hieu_luc` | Cột sinh `IF(trang_thai='HIEU_LUC', nguoi_cao_tuoi_id, NULL)` | `UNIQUE` | FR-013 |
| `rut_lai_luc`, `rut_lai_boi`, `rut_lai_ly_do`, `rut_lai_bang_chung_tep_id` | `NULL` | Bằng chứng bắt buộc khi rút lại bằng lệnh; rỗng khi được thay thế | FR-014 |
| `thay_the_boi_id` | `BIGINT NULL` | Khóa ngoại tới `ban_dong_y.id` | FR-013 |
| `phien_ban`, `tao_luc`, `tao_boi` | | | |

Trigger `BEFORE UPDATE`: chỉ cho đổi các cột rút lại và thay thế, và chỉ theo chiều `HIEU_LUC → DA_RUT_LAI` (FR-014, FR-016). Không xóa (mẫu 000).

### `ban_dong_y_nguoi_than` – chỉ thêm → gộp theo R-15: `ban_dong_y_nguoi_than` → cột `JSON` của `ban_dong_y`

`(ban_dong_y_id, nguoi_than_id)` là khóa chính. Ghi cùng transaction với bản đồng ý; tài khoản ứng dụng không có quyền `UPDATE`, `DELETE`.

### Bảng chuyển

| Từ | Lệnh | Đến | Endpoint | Kiểm tra | Nhật ký |
| --- | --- | --- | --- | --- | --- |
| — | Ghi nhận | `HIEU_LUC` | `POST /nguoi-cao-tuoi/{id}/ban-dong-y` | AC-09; hồ sơ chưa ở trạng thái cuối; có tệp; FR-011: người ký thay phải có đủ bằng chứng quyết định của Tòa án; vai trò "người đại diện" của 012 không được xét (Q-348). Bản Hiệu lực cũ chuyển `DA_RUT_LAI` với lý do "được thay thế" trong cùng transaction | `BDY_GHI_NHAN` (+`BDY_THAY_THE`) |
| `HIEU_LUC` | Rút lại đồng ý | `DA_RUT_LAI` | `POST /ban-dong-y/{id}/rut-lai` | AC-09; lý do; tệp bằng chứng; được làm cả trên hồ sơ trạng thái cuối (FR-014a) | `BDY_RUT_LAI` |

---

## 5. Sức khỏe (mục C)

### `ho_so_suc_khoe_ban_dau` – nhóm 3 có bản nháp → gộp theo R-15: `ho_so_suc_khoe_ban_dau` → `ho_so_chung`

| Cột | Ghi chú |
| --- | --- |
| `id`, `nguoi_cao_tuoi_id` | `UNIQUE (nguoi_cao_tuoi_id)` |
| `nhom_mau` | `VARCHAR(10) NULL` |
| `kha_nang_van_dong`, `kha_nang_tu_cham_soc`, `kha_nang_nhan_thuc` | `TEXT NULL` |
| `ghi_chu_nhu_cau_ban_dau`, `nhu_cau_dinh_duong`, `thuoc_dang_dung`, `nhu_cau_phuc_hoi` | `TEXT NULL` (FR-017) |
| `nguoi_thuc_hien_id`, `thoi_diem` | Người ghi bản nháp (cột chuẩn nhóm 3) |
| `nguoi_xac_nhan_id`, `thoi_diem_xac_nhan` | `NULL` khi còn nháp |
| `phien_ban` | Khóa lạc quan khi sửa nháp |

Trigger `trg_ho_so_suc_khoe_ban_dau_khong_sua` chặn `UPDATE` khi `OLD.thoi_diem_xac_nhan IS NOT NULL`. Đính chính qua 000, loại `M01.HO_SO_SUC_KHOE_BAN_DAU` (H-08).

| Từ | Lệnh | Đến | Endpoint | Kiểm tra | Nhật ký |
| --- | --- | --- | --- | --- | --- |
| — | Lập | Nháp | `PUT /nguoi-cao-tuoi/{id}/ho-so-suc-khoe-ban-dau` (lần đầu) | AC-03, AC-04; hồ sơ chưa ở trạng thái cuối | `HSSK_LAP` |
| Nháp | Sửa nháp | Nháp | `PUT …` cùng `If-Match` | AC-03, AC-04 | `HSSK_SUA_NHAP` |
| Nháp | Xác nhận | Đã xác nhận | `POST …/ho-so-suc-khoe-ban-dau/xac-nhan` | AC-03 | `HSSK_XAC_NHAN` |

### `muc_suc_khoe` – nhóm 2, nội dung bất biến

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id`, `nguoi_cao_tuoi_id` | | Chỉ mục `(nguoi_cao_tuoi_id, loai, trang_thai)` | |
| `loai` | `ENUM('DI_UNG','BENH_NEN','TIEN_SU')` | | FR-018 |
| `di_nguyen_id` | `BIGINT NULL` | Khóa ngoại `di_nguyen.id`; chỉ với `DI_UNG` | FR-018a |
| `noi_dung_van_ban` | `VARCHAR(1000) NULL` | `CHECK (di_nguyen_id IS NOT NULL OR noi_dung_van_ban IS NOT NULL)` | FR-018 |
| `khong_kiem_tra_tu_dong` | Cột sinh `loai='DI_UNG' AND di_nguyen_id IS NULL` | | FR-018a |
| `muc_do_id` | `BIGINT NULL` | Khóa ngoại; `CHECK (loai <> 'DI_UNG' OR muc_do_id IS NOT NULL)` | FR-018, BR-M01-11 |
| `nguon_id` | `BIGINT` | Khóa ngoại `nguon_thong_tin.id` | FR-018 |
| `nguoi_ghi_id`, `thoi_diem` | | | FR-018 |
| `trang_thai` | `ENUM('HIEU_LUC','DA_LOAI_TRU')` | | |
| `loai_tru_luc`, `loai_tru_boi`, `ly_do_loai_tru` | `NULL` | Bắt buộc cả ba khi `DA_LOAI_TRU` (`CHECK`) | FR-019, DBR-04 |
| `cho_kiem_tra_thuoc`, `cho_kiem_tra_an` | `BOOLEAN` | Cờ kỹ thuật (H-07) | BR-M01-12 |
| `phien_ban` | | | |

Bảo đảm:
- Trigger `trg_muc_suc_khoe_noi_dung_bat_bien` (`BEFORE UPDATE`) chặn mọi thay đổi ở `loai`, `di_nguyen_id`, `noi_dung_van_ban`, `muc_do_id`, `nguon_id`, `nguoi_ghi_id`, `thoi_diem`. Trigger chặn cả `DA_LOAI_TRU → HIEU_LUC`.
- `trg_muc_suc_khoe_khong_xoa` theo mẫu 000.
- Không có endpoint sửa hay xóa (DBR-04, SC-003).

| Từ | Lệnh | Đến | Endpoint | Kiểm tra | Nhật ký |
| --- | --- | --- | --- | --- | --- |
| — | Ghi nhận | `HIEU_LUC` | `POST /nguoi-cao-tuoi/{id}/muc-suc-khoe` | AC-03, AC-04; hồ sơ chưa ở trạng thái cuối; trùng thì 409 `MUC_SUC_KHOE_TRUNG` trừ khi `xacNhanTrung` (FR-021); danh mục đang `HIEU_LUC` | `MSK_GHI_NHAN` |
| `HIEU_LUC` | Loại trừ | `DA_LOAI_TRU` | `POST /muc-suc-khoe/{id}/loai-tru` | AC-03, AC-04; lý do | `MSK_LOAI_TRU` |

### Danh mục

| Bảng | Cột riêng (ngoài cột chuẩn nhóm 1) | Người cấu hình | Khởi tạo |
| --- | --- | --- | --- |
| `di_nguyen` | `nhom ENUM('THUOC_HOAT_CHAT','THUC_PHAM','KHAC')` | `THUOC_HOAT_CHAT`: AC-01 (006 FR-002). `THUC_PHAM`: AC-03 (011 FR-003). `KHAC`: AC-01 (tạm, plan điểm báo lại 3) | Rỗng; nạp khi triển khai |
| `muc_do_di_ung` | `thu_tu` | AC-01 | Nhẹ, Trung bình, Nặng (BR-M01-11) |
| `nguon_thong_tin` | — | AC-01 | Người cao tuổi, Người thân, Hồ sơ y tế kèm theo, Bác sĩ khám |
| `nhu_cau_cham_soc` | — | AC-01 | Hỗ trợ vận động, Theo dõi sức khỏe thường xuyên, Phục hồi sau tai biến, Phục hồi chức năng khác (Q-291) |

Người cấu hình `muc_do_di_ung`, `nguon_thong_tin`, `nhu_cau_cham_soc` theo quyền cấu hình danh mục chung của Quản lý viện (UC-69, 19.2). Tài liệu nguồn chưa nêu dòng riêng ở Phụ lục 27 (plan, điểm báo lại 3). 006 và 011 tham chiếu `di_nguyen.id` bằng định danh logic và đọc danh mục qua `DiNguyenApi`.

---

## 6. Nguyện vọng cuối đời (mục C1)

### `nguyen_vong_cuoi_doi` – nhóm 2, có phiên bản → gộp theo R-15: `nguyen_vong_cuoi_doi` → `nguyen_vong` (`wish`)

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id`, `nguoi_cao_tuoi_id` | | | |
| `lua_chon` | `ENUM('CHUYEN_BENH_VIEN','DUA_VE_NHA','O_LAI_GIAM_NHE')` | | FR-023a |
| `nguoi_lien_he_truoc` | `VARCHAR(500)` | | FR-023a |
| `ghi_chu` | `TEXT NULL` | Ghi chú chung; ý kiến của gia đình không ghi ở đây mà ở `nguyen_vong_y_kien` (FR-023c, FR-023g) | FR-023a *(R-15: `nguyen_vong_y_kien` → `su_kien_chung`)* |
| `nguoi_tra_loi_loai` | `ENUM('NGUOI_CAO_TUOI','DAI_DIEN_NGUOI_BENH','DAI_DIEN_PHAP_LUAT')` | | FR-023a |
| `nguoi_tra_loi_nguoi_than_id`, `quan_he` | | Bắt buộc khi người trả lời không phải người cao tuổi | FR-023a |
| `bac_si_nhan_dinh_id` | `BIGINT` | Có vai trò AC-03 lúc ghi | FR-023a, Q-216 |
| `tu_quyet_duoc` | `BOOLEAN` | Nhận định cho chính phiếu này, không phải dấu của người cao tuổi (FR-023h) | FR-023a, FR-023c (sửa 2026-10-18, thay `du_nang_luc`) |
| `thoi_diem_nhan_dinh` | `DATETIME(6)` | | FR-023a, FR-023h |
| `cach_ho_tro_hieu` | `TEXT` | `NOT NULL`, không rỗng | FR-023a, FR-023h |
| `ngay_xem_lai_nhan_dinh` | `DATE` | `NOT NULL`; `>= DATE(thoi_diem_nhan_dinh)` (`CHECK`) | FR-023a, FR-023h |
| `nguoi_ky_loai` | `ENUM('NGUOI_CAO_TUOI','DAI_DIEN_NGUOI_BENH','DAI_DIEN_PHAP_LUAT')` | `CHECK (tu_quyet_duoc = (nguoi_ky_loai = 'NGUOI_CAO_TUOI'))` | FR-023c |
| `nguoi_ky_nguoi_than_id` | `BIGINT NULL` | Bắt buộc khi người ký không phải người cao tuổi (`CHECK`) | FR-023c |
| `cach_chon_dai_dien` | `ENUM('NCT_DA_CHON','GIA_DINH_CHON') NULL` | Bắt buộc khi `DAI_DIEN_NGUOI_BENH`, rỗng khi khác (`CHECK`) | FR-023c |
| `qd_toa_so`, `qd_toa_ngay`, `qd_toa_toa_an`, `qd_toa_tep_id` | Như `ban_dong_y`; `NULL` | Bắt buộc cả bốn khi `DAI_DIEN_PHAP_LUAT` (`CHECK`) | FR-023c, FR-011 |
| `thoi_diem_ky` | `DATETIME(6)` | | |
| `ban_ky_tep_id` | `BIGINT` | `NOT NULL` | FR-023a |
| `nguon` | `ENUM('TIEP_NHAN','YEU_CAU_GIA_DINH','XAC_NHAN_LAI_NGUY_KICH')` | | Key Entities |
| `nguoi_thuc_hien_id`, `thoi_diem` | | Người ghi nhận | FR-023a |
| `trang_thai` | `ENUM('HIEU_LUC','DUOC_THAY_THE')` | | FR-023b |
| `thay_the_boi_id` | `BIGINT NULL` | | |
| `dang_hieu_luc` | Cột sinh `IF(trang_thai='HIEU_LUC', nguoi_cao_tuoi_id, NULL)` | `UNIQUE` | DBR-34 |

Trigger chỉ cho `UPDATE` hai cột `trang_thai` và `thay_the_boi_id`, theo chiều `HIEU_LUC → DUOC_THAY_THE`. Không xóa.

### `nguyen_vong_y_kien` – chỉ thêm *(bổ sung 2026-10-18, FR-023c, FR-023g)* → gộp theo R-15: `nguyen_vong_y_kien` → `su_kien_chung`

Ý kiến khác với lựa chọn đã ghi (thường là của gia đình). Mỗi ý kiến một dòng, ghi cùng transaction với phiên bản phiếu; tài khoản ứng dụng không có quyền `UPDATE`, `DELETE`. Phiên bản mới mang tập ý kiến của riêng nó.

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id`, `nguyen_vong_id` | | Khóa ngoại tới `nguyen_vong_cuoi_doi.id` | |
| `nguon_loai`, `nguon_nguoi_than_id` | Theo mẫu cột nguồn ý kiến (mục 6a) | | FR-023g |
| `noi_dung` | `TEXT` | `NOT NULL` | FR-023c |
| `nguoi_ghi_id`, `thoi_diem` | | | FR-023g |

| Từ | Lệnh | Đến | Endpoint | Kiểm tra | Nhật ký |
| --- | --- | --- | --- | --- | --- |
| —, `HIEU_LUC` | Ghi phiên bản mới | `HIEU_LUC`; bản trước `DUOC_THAY_THE` | `POST /nguoi-cao-tuoi/{id}/nguyen-vong`; hoặc `NguyenVongApi.ghiPhienBan` từ 007 | AC-03, AC-04; hồ sơ chưa ở trạng thái cuối; người ký theo FR-023c (người cao tuổi khi `tu_quyet_duoc`; ngược lại người thân có quan hệ Hiệu lực, kiểm qua 012, kèm cách chọn hoặc quyết định của Tòa án); mỗi ý kiến khác có nguồn hợp lệ (mục 6a) | `NV_GHI` |

---

## 6b. Nguyện vọng khi điều trị (mục C1a) *(bổ sung 2026-10-22; FR-023j → FR-023m, BR-M01-16, DBR-39; research H-18)*

### `nguyen_vong_dieu_tri` – nhóm 2, có phiên bản → gộp theo R-15: `nguyen_vong_dieu_tri` → `nguyen_vong` (`wish`)

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id`, `nguoi_cao_tuoi_id` | | | |
| `bv1_ten`, `bv1_dia_chi` | `VARCHAR(255)`, `VARCHAR(500)` | `NOT NULL`, không rỗng | FR-023j |
| `bv2_ten`, `bv2_dia_chi` | Như trên, `NULL` | `CHECK ((bv2_ten IS NULL) = (bv2_dia_chi IS NULL))` | FR-023j |
| `bhyt_noi_dang_ky`, `bhyt_so_the`, `bhyt_han_the` | `VARCHAR(255) NULL`, `VARCHAR(20) NULL`, `DATE NULL` | Không bắt buộc; pháp lý chờ 24.4 (plan, điểm báo lại 13) | FR-023j |
| `hoi_gia_dinh_truoc` | `BOOLEAN` | `NOT NULL` | FR-023j |
| `nguoi_di_kem` | `ENUM(GIA_DINH,VIEN_CU_NHAN_VIEN,TUY_TINH_HUONG)` | `NOT NULL` | FR-023j |
| `ghi_chu` | `TEXT NULL` | Ý kiến gia đình khác không ghi ở đây mà ở `nguyen_vong_dieu_tri_y_kien` | FR-023j *(R-15: `nguyen_vong_dieu_tri_y_kien` → `su_kien_chung`)* |
| `nguon_loai`, `nguon_nguoi_than_id` | Mẫu cột nguồn ý kiến (mục 6a) | Người trả lời | FR-023j, FR-023g |
| `ban_ky_tep_id` | `BIGINT NULL` | Không bắt buộc | FR-023j |
| `nguoi_thuc_hien_id`, `thoi_diem` | | Người ghi (AC-03, AC-04) | FR-023j |
| `trang_thai` | `ENUM(HIEU_LUC,HET_HIEU_LUC)` | | FR-023k, DBR-39 |
| `thay_the_boi_id` | `BIGINT NULL` | | FR-023k |
| `dang_hieu_luc` | Cột sinh `IF(trang_thai=HIEU_LUC, nguoi_cao_tuoi_id, NULL)` | `UNIQUE` | DBR-39 |

Trigger chỉ cho `UPDATE` hai cột `trang_thai` và `thay_the_boi_id`, theo chiều `HIEU_LUC → HET_HIEU_LUC`. Không xóa.

### `nguyen_vong_dieu_tri_nguoi_bao` – chỉ thêm → gộp theo R-15: `nguyen_vong_dieu_tri_nguoi_bao` → cột `JSON` của `nguyen_vong`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `nguyen_vong_dieu_tri_id`, `nguoi_than_id` | `BIGINT` | Khóa chính ghép; người thân có quan hệ Hiệu lực lúc ghi (`QuanHeNguoiThanApi.thuocNguoiCaoTuoi`); 0..n dòng (chủ dự án chọn 2026-10-22) | FR-023j |

### `nguyen_vong_dieu_tri_y_kien` – chỉ thêm → gộp theo R-15: `nguyen_vong_dieu_tri_y_kien` → `su_kien_chung`

Như `nguyen_vong_y_kien` (mục 6), khóa ngoại tới `nguyen_vong_dieu_tri.id`: `nguon_loai`, `nguon_nguoi_than_id` (mẫu mục 6a), `noi_dung TEXT NOT NULL`, `nguoi_ghi_id`, `thoi_diem`; không cấp `UPDATE`, `DELETE` (BR-M01-14). *(R-15: `nguyen_vong_y_kien` → `su_kien_chung`)*

| Từ | Lệnh | Đến | Endpoint | Kiểm tra | Nhật ký |
| --- | --- | --- | --- | --- | --- |
| —, `HIEU_LUC` | Ghi phiên bản mới | `HIEU_LUC`; bản trước `HET_HIEU_LUC` | `POST /nguoi-cao-tuoi/{id}/nguyen-vong-dieu-tri` | `(DI_UNG_BENH_NEN, TAO)` (AC-03, AC-04) trong phạm vi; hồ sơ chưa ở trạng thái cuối (`HoSoMoGuard`); người cần báo và người trả lời `NGUOI_THAN` có quan hệ Hiệu lực; ý kiến khác có nguồn hợp lệ; `hoiGiaDinhTruoc` mà không có người cần báo thì cảnh báo `CHUA_CO_NGUOI_CAN_BAO` | `NVDT_GHI` |

---

## 6a. Nguồn của ý kiến và không có dấu năng lực *(bổ sung 2026-10-18; FR-023g, FR-023h; research H-16)*

**Mẫu cột nguồn ý kiến** (001 định nghĩa; 004, 005, 012, 014 dùng cho bảng của mình):

| Cột | Kiểu | Ràng buộc |
| --- | --- | --- |
| `nguon_loai` | `ENUM('NGUOI_CAO_TUOI','NGUOI_THAN','NHAN_VIEN_QUAN_SAT')` | `NOT NULL` |
| `nguon_nguoi_than_id` | `BIGINT NULL` | `CHECK ((nguon_loai = 'NGUOI_THAN') = (nguon_nguoi_than_id IS NOT NULL))`; người thân có quan hệ Hiệu lực với người cao tuổi lúc ghi, kiểm bằng `NguonYKienApi.kiemTra` |
| `nguoi_ghi_id`, `thoi_diem` | | Cột chuẩn; thời điểm từ `Clock` |
| `phan_hoi_id` | `BIGINT NULL` | *(Bổ sung 2026-10-18, FR-023g)* Cột tùy chọn của mẫu: chỉ bảng có lệnh ghi mục từ phản hồi Mong muốn của 012 mới có (hiện là `so_thich` của 014); có giá trị thì phát `YKienDaGhiTuPhanHoi` (research H-17). `mong_muon_nct` của 012 không cần cột này vì liên kết nằm ở `phan_hoi_y_kien` cùng module; bảng của 004, 005 không có cột *(R-15: `mong_muon_nct` → `ho_so_chung`; `phan_hoi_y_kien` → `su_kien_chung`)* |

Bảng dùng mẫu này không có quyền `UPDATE` trên cột nội dung và nguồn (trigger hoặc không cấp quyền); thay đổi bằng dòng mới, dòng cũ vẫn đọc được (FR-023g).

**Không có dấu năng lực** (FR-023h): `nguoi_cao_tuoi` và mọi bảng của M01 không có cột năng lực chung. Nhận định "tự quyết được" chỉ có ở `nguyen_vong_cuoi_doi.tu_quyet_duoc` (gắn với phiếu) và ở mục thỏa thuận lựa chọn có rủi ro của 005. Test kiến trúc đọc `information_schema.COLUMNS` của schema và OpenAPI của mọi module, báo lỗi khi bảng hay schema gắn với người cao tuổi có cột hay trường chứa `nang_luc` / `nangLuc`, trừ danh sách cho phép rỗng.

---

## 7. Đánh giá (mục D)

### `danh_gia` – nhóm 3 có bản nháp

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id`, `nguoi_cao_tuoi_id` | | Chỉ mục `(nguoi_cao_tuoi_id, trang_thai, thoi_diem_xac_nhan)` | |
| `loai` | `ENUM('DAU_VAO','DANH_GIA_LAI')` | | FR-025 |
| `can_cu` | `VARCHAR(1000) NULL` | Lý do, căn cứ | FR-025 |
| `nguoi_thuc_hien_id`, `thoi_diem` | | Người lập, thời điểm tạo | FR-025 |
| `nhan_xet` | `TEXT NULL` | Bắt buộc khi xác nhận | FR-025, DBR-05 |
| `luu_y_cham_soc` | `VARCHAR(1000) NULL` | | FR-025, BR-M01-10 |
| `muc_cham_soc` | `VARCHAR(20) NULL` | Khóa ngoại; bắt buộc khi xác nhận | FR-028, DBR-05 *(R-15: `muc_cham_soc` → `danh_muc_chung`)* |
| `loai_luu_tru_de_xuat` | `ENUM('NOI_TRU_DAI_HAN','NOI_TRU_NGAN_NGAY','BAN_TRU') NULL` | Chỉ với `DAU_VAO` (`CHECK`) | FR-025, FR-033 |
| `trang_thai` | `ENUM('NHAP','DA_XAC_NHAN','DA_HUY')` | | FR-026 |
| `nguoi_xac_nhan_id`, `thoi_diem_xac_nhan` | `NULL` | | FR-025 |
| `huy_boi`, `huy_luc`, `ly_do_huy` | `NULL` | Bắt buộc khi `DA_HUY` | FR-026, BR-M01-13 |
| `nhap_dang_mo` | Cột sinh `IF(trang_thai='NHAP', nguoi_cao_tuoi_id, NULL)` | `UNIQUE` | BR-M01-13 |
| `dau_vao_da_xac_nhan` | Cột sinh `IF(loai='DAU_VAO' AND trang_thai='DA_XAC_NHAN', nguoi_cao_tuoi_id, NULL)` | `UNIQUE` | FR-034 |
| `phien_ban` | | Khóa lạc quan khi sửa nháp | |

`CHECK (trang_thai <> 'DA_XAC_NHAN' OR (nhan_xet IS NOT NULL AND CHAR_LENGTH(TRIM(nhan_xet)) > 0 AND muc_cham_soc IS NOT NULL))` (DBR-05). Trigger `trg_danh_gia_khong_sua` chặn `UPDATE` khi `OLD.trang_thai IN ('DA_XAC_NHAN','DA_HUY')`. Không xóa.

### `danh_gia_nhu_cau` → gộp theo R-15: `danh_gia_nhu_cau` → cột `JSON` của `danh_gia`

`(danh_gia_id, nhu_cau_id)` là khóa chính. Trigger `BEFORE INSERT` và `BEFORE DELETE` chặn khi lần đánh giá cha không còn `NHAP`.

### Bảng chuyển

| Từ | Lệnh | Đến | Endpoint | Kiểm tra | Nhật ký |
| --- | --- | --- | --- | --- | --- |
| — | Tạo | `NHAP` | `POST /nguoi-cao-tuoi/{id}/danh-gia` | AC-03, AC-04 (`DANH_GIA_MUC_CHAM_SOC, TAO`); không có Nháp khác (409 `DA_CO_BAN_NHAP`); `DAU_VAO` chỉ khi chưa có đầu vào Đã xác nhận và hồ sơ `DANG_TIEP_NHAN`; `DANH_GIA_LAI` theo trạng thái ở FR-035 | `DG_TAO` |
| `NHAP` | Sửa nháp | `NHAP` | `PUT /danh-gia/{id}` cùng `If-Match` | Người tạo hoặc AC-03 | `DG_SUA_NHAP` |
| `NHAP` | Xác nhận | `DA_XAC_NHAN` | `POST /danh-gia/{id}/xac-nhan` | AC-03 (`DANH_GIA_MUC_CHAM_SOC, DUYET`); FR-028 | `DG_XAC_NHAN` |
| `NHAP` | Hủy nháp | `DA_HUY` | `POST /danh-gia/{id}/huy-nhap` | Người tạo hoặc AC-03; lý do | `DG_HUY_NHAP` |

Tác động khi xác nhận: theo research H-10, trong cùng transaction. Các tác động đó gồm đặt mức hiện hành, gọi 004 thay thế yêu cầu, sự kiện cho 004, 005, 009, và đáp ứng các lần báo đánh giá lại.

---

## 8. Bảng kỹ thuật

### `lan_bao_danh_gia_lai` → gộp theo R-15: `lan_bao_danh_gia_lai` → `ho_so_chung`

| Cột | Ghi chú |
| --- | --- |
| `id`, `nguoi_cao_tuoi_id` | |
| `can_cu` | `ENUM('DINH_KY','SAU_SU_CO','TRO_VE_BENH_VIEN','DINH_CHINH_DANH_GIA')` |
| `nguon_loai`, `nguon_id` | Lần đánh giá, sự cố, dòng lịch sử trạng thái, bản đính chính |
| `thoi_diem_bao` | |
| `han_tinh_tu` | `DATETIME(6) NULL`. `NULL` khi người cao tuổi đang Điều trị tại bệnh viện; Ghi nhận trở về đặt giá trị (FR-041) |
| `da_canh_bao_qua_han_luc` | `NULL` tới khi job cảnh báo |

`UNIQUE (nguoi_cao_tuoi_id, can_cu, nguon_loai, nguon_id)` chống báo lặp (NFR-04). Bảng không bị xóa theo nghiệp vụ; lưu giữ theo hồ sơ (mục 11).

### `nhac_chu_ky` → gộp theo R-15: `nhac_chu_ky` → `moc_da_nhac`

| Cột | Ghi chú |
| --- | --- |
| `loai` | `ENUM('THIEU_BAN_DONG_Y','THIEU_NGUYEN_VONG','LAP_YEU_CAU_DOI_MUC','XEM_LAI_NHAN_DINH')` (giá trị cuối bổ sung 2026-10-18, FR-023i) |
| `nguoi_cao_tuoi_id` | |
| `moc_id` | Với `LAP_YEU_CAU_DOI_MUC`: lần đánh giá lại gây ra việc nhắc; với `XEM_LAI_NHAN_DINH`: id phiếu nguyện vọng (nhắc một lần); với hai loại kia: 0 |
| `bat_dau_luc`, `lan_nhac_cuoi_luc`, `so_lan_nhac` | |

Khóa chính `(loai, nguoi_cao_tuoi_id, moc_id)`. Dòng được thêm khi điều kiện nhắc bắt đầu, ví dụ Hoàn tất tiếp nhận mà thiếu bản đồng ý. Job cập nhật `lan_nhac_cuoi_luc`. Khi điều kiện hết (có bản đồng ý, có phiếu, có yêu cầu, hoặc hồ sơ ở trạng thái cuối) thì job bỏ qua, không cần xóa dòng.

### `canh_bao_tro_lai` → gộp theo R-15: `canh_bao_tro_lai` → `moc_da_nhac`

`(luot_vang_id, moc_du_kien)` là khóa chính, cùng cột `canh_bao_luc`. Mỗi mốc dự kiến trở lại có đúng một cảnh báo (FR-050).

### `tep` (`common`, research H-13)

| Cột | Ghi chú |
| --- | --- |
| `id`, `uuid` | `uuid` là khóa đối tượng trong bucket S3 (sửa 2026-10-19) |
| `ten_goc`, `kieu_mime`, `kich_thuoc`, `sha256` | Kiểm kiểu và kích thước khi tải lên |
| `tai_len_boi`, `tai_len_luc` | |
| `chu_so_huu_loai`, `chu_so_huu_id`, `gan_luc` | `NULL` tới khi gắn. Trigger chặn đổi khi đã gắn |

---

## 9. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bảo đảm | Bảng | Cách |
| --- | --- | --- |
| Mã hồ sơ duy nhất; CCCD duy nhất trong hồ sơ chưa ở trạng thái cuối | `nguoi_cao_tuoi` | `UNIQUE ma_ho_so`; cột sinh `cccd_dang_mo` có `UNIQUE` |
| Một trạng thái hiện tại | `nguoi_cao_tuoi` | `trang_thai NOT NULL` |
| Lịch sử trạng thái chỉ thêm | `lich_su_trang_thai_nct` | Hai trigger theo mẫu 000; không có quyền `UPDATE`, `DELETE` *(R-15: `lich_su_trang_thai_nct` → `lich_su_chung`)* |
| Tối đa một bản đồng ý Hiệu lực | `ban_dong_y` | Cột sinh `dang_hieu_luc` có `UNIQUE` |
| Bản đồng ý không sửa nội dung | `ban_dong_y`, `ban_dong_y_nguoi_than` | Trigger chỉ cho cột rút lại; bảng con không có quyền `UPDATE` *(R-15: `ban_dong_y_nguoi_than` → cột `JSON` của `ban_dong_y`)* |
| Mục sức khỏe không xóa, không sửa nội dung | `muc_suc_khoe` | `trg_muc_suc_khoe_noi_dung_bat_bien`, `trg_muc_suc_khoe_khong_xoa` |
| Dị ứng có mức độ | `muc_suc_khoe` | `CHECK` |
| Hồ sơ sức khỏe ban đầu không sửa sau xác nhận | `ho_so_suc_khoe_ban_dau` | `trg_…_khong_sua` *(R-15: `ho_so_suc_khoe_ban_dau` → `ho_so_chung`)* |
| Tối đa một phiên bản nguyện vọng Hiệu lực | `nguyen_vong_cuoi_doi` | Cột sinh `dang_hieu_luc` có `UNIQUE`; trigger chỉ cho chuyển sang Được thay thế *(R-15: `nguyen_vong_cuoi_doi` → `nguyen_vong` (`wish`))* |
| Tối đa một phiên bản nguyện vọng khi điều trị Hiệu lực (DBR-39) | `nguyen_vong_dieu_tri` | Cột sinh `dang_hieu_luc` có `UNIQUE`; trigger chỉ cho chuyển sang Hết hiệu lực; bảng con không có quyền `UPDATE`, `DELETE` *(R-15: `nguyen_vong_dieu_tri` → `nguyen_vong` (`wish`))* |
| Người ký thay bản đồng ý có quyết định của Tòa án | `ban_dong_y` | `CHECK` bốn cột `qd_toa_*` theo `nguoi_dong_y_loai` |
| Người ký phiếu nguyện vọng khớp nhận định của phiếu | `nguyen_vong_cuoi_doi` | `CHECK` `tu_quyet_duoc`, `nguoi_ky_loai`, `cach_chon_dai_dien`, `qd_toa_*` *(R-15: `nguyen_vong_cuoi_doi` → `nguyen_vong` (`wish`))* |
| Ý kiến có nguồn, không sửa | `nguyen_vong_y_kien` | `CHECK` cột nguồn; không có quyền `UPDATE`, `DELETE` *(R-15: `nguyen_vong_y_kien` → `su_kien_chung`)* |
| Lần đánh giá Đã xác nhận có nhận xét và mức | `danh_gia` | `CHECK` |
| Tối đa một Nháp; tối đa một đầu vào Đã xác nhận | `danh_gia` | Hai cột sinh có `UNIQUE` |
| Đánh giá không sửa sau xác nhận, hủy | `danh_gia`, `danh_gia_nhu_cau` | Trigger *(R-15: `danh_gia_nhu_cau` → cột `JSON` của `danh_gia`)* |
| Mức hiện hành khớp lịch sử | `nguoi_cao_tuoi`, `lich_su_muc_cham_soc` | Cùng transaction; test tích hợp *(R-15: `lich_su_muc_cham_soc` → `lich_su_chung`)* |
| Bảng nhóm 3 có trigger | Mọi bảng `@NhomDuLieu(GHI_NHAN)` của M01 | Test đối chiếu `information_schema.TRIGGERS` của 000 |

---

## 10. Nhật ký và phân loại trường

Mọi lệnh ghi `nhat_ky` của 000 trong cùng transaction (000 FR-047). `nguoi_cao_tuoi_id` luôn có, để lưu giữ theo hồ sơ (000 FR-050).

`hanh_dong` của 001:
- Hồ sơ: `NCT_TAO`, `NCT_XAC_NHAN_KHONG_TRUNG`, `NCT_CAP_NHAT`, `NCT_CHUYEN`, `MUC_CS_DOI`.
- Bản đồng ý: `BDY_GHI_NHAN`, `BDY_THAY_THE`, `BDY_RUT_LAI`.
- Sức khỏe: `HSSK_LAP`, `HSSK_SUA_NHAP`, `HSSK_XAC_NHAN`, `MSK_GHI_NHAN`, `MSK_XAC_NHAN_TRUNG`, `MSK_LOAI_TRU`, `MSK_DA_KIEM_TRA`.
- Nguyện vọng: `NV_GHI` (chi tiết gồm các ý kiến khác và nguồn); `NVDT_GHI` (phiếu nguyện vọng khi điều trị, bổ sung 2026-10-22).
- Đánh giá: `DG_TAO`, `DG_SUA_NHAP`, `DG_XAC_NHAN`, `DG_HUY_NHAP`.
- Bộ lập lịch: `DGL_BAO`, `DGL_CANH_BAO_QUA_HAN`, `NHAC_THIEU_DONG_Y`, `NHAC_THIEU_NGUYEN_VONG`, `NHAC_LAP_YEU_CAU_DOI_MUC`, `CANH_BAO_TAM_VANG_QUA_HAN`.
- Danh mục: `DM_TAO`, `DM_SUA`, `DM_NGUNG`, `DM_KICH_HOAT_LAI`.

`PhanLoaiTruongProvider` (000 mục 1.3) cho các loại đối tượng của M01:

| Loại đối tượng | Trường → nhóm thông tin |
| --- | --- |
| `M01.NGUOI_CAO_TUOI` | `soGiayTo`, `anh` → `NHAN_DANG`; `diaChi*`, `lienHe` → `LIEN_HE`; `mucChamSocHienHanh` → `MUC_CHAM_SOC`; còn lại → `CONG_KHAI_NOI_BO` |
| `M01.MUC_SUC_KHOE`, `M01.HO_SO_SUC_KHOE_BAN_DAU`, `M01.DANH_GIA` | Mọi trường → `SUC_KHOE`; riêng `mucChamSoc` của đánh giá → `MUC_CHAM_SOC` |
| `M01.NGUYEN_VONG` | Mọi trường → `NGUYEN_VONG` |
| `M01.NGUYEN_VONG_DIEU_TRI` *(bổ sung 2026-10-22)* | Mọi trường → `SUC_KHOE` (FR-023m); cờ `coNguyenVongDieuTri` trên `M01.NGUOI_CAO_TUOI` → `CONG_KHAI_NOI_BO` nhưng chỉ đưa vào DTO cho người có `(DI_UNG_BENH_NEN, XEM)` hoặc Hành chính; Kế toán không có (research H-18; chủ dự án chọn 2026-10-22) |
| `M01.BAN_DONG_Y` | `qdToaSo`, `qdToaNgay`, `qdToaToaAn`, `qdToaTepId` (quyết định của Tòa án về năng lực hành vi, sửa 2026-10-18) → `NHAN_DANG`, người thân không thấy; còn lại → `CONG_KHAI_NOI_BO` |

Nhờ bảng này, Hành chính xem lịch sử hồ sơ thấy "đã thay đổi" ở trường sức khỏe mà không thấy giá trị (19.3, BR-M15-11).

---

## 11. Lưu giữ (000 R-11)

`LuuTruProvider` do M01 đăng ký. Mọi loại có `loaiHan = THEO_HO_SO` (CFG-M15-03, mốc là thời điểm trạng thái cuối):
- `M01.NGUOI_CAO_TUOI`: gốc của chuỗi; bị tham chiếu bởi mọi bảng khác, kể cả của module khác qua `thamChieuToi`;
- `lich_su_trang_thai_nct`, `lich_su_muc_cham_soc`, `ban_dong_y`, `ho_so_suc_khoe_ban_dau`, `muc_suc_khoe`, `nguyen_vong_cuoi_doi`, `nguyen_vong_y_kien`, `nguyen_vong_dieu_tri` và hai bảng con, `danh_gia`; *(R-15: `lich_su_trang_thai_nct`, `lich_su_muc_cham_soc` → `lich_su_chung`; `ho_so_suc_khoe_ban_dau` → `ho_so_chung`; `nguyen_vong_cuoi_doi`, `nguyen_vong_dieu_tri` → `nguyen_vong` (`wish`); `nguyen_vong_y_kien` → `su_kien_chung`)*
- `lan_bao_danh_gia_lai`, `nhac_chu_ky`; *(R-15: `lan_bao_danh_gia_lai` → `ho_so_chung`; `nhac_chu_ky` → `moc_da_nhac`)*
- `tep` gắn với các bản ghi trên.

Hồ sơ có hồ sơ sau liên kết tới (FR-004) không bị loại bỏ trước hồ sơ sau, vì `thamChieuToi` trả liên kết đó (000 FR-051a).

---

## 12. Truy vết DBR, BR và FR

| Mã | Bảo đảm bằng | Nơi kiểm thử |
| --- | --- | --- |
| DBR-01 | `ma_ho_so UNIQUE`; `cccd_dang_mo UNIQUE`; `trang_thai NOT NULL`; liên kết hồ sơ trước (H-04) | `HoSoCccdIT`, `HoSoLienKetIT` |
| DBR-02 | Điều kiện của bean 012 trong Hoàn tất tiếp nhận | `HoanTatTiepNhanIT` (bean giả) |
| DBR-03 | `DieuKienChuThich("1")` kiểm bản đồng ý Hiệu lực ở mỗi yêu cầu | `NguoiThanSucKhoeIT` (dùng chung với 002), `RutLaiDongYIT` |
| DBR-04 | Trigger mục sức khỏe; không có endpoint sửa, xóa | `MucSucKhoeBatBienIT` |
| DBR-05 | `CHECK` trên `danh_gia`; kiểm ở service | `XacNhanDanhGiaIT` |
| DBR-23 | Nhật ký trong transaction (000) | `NhatKyDayDuM01IT` (proxy `DataSource` của 000) |
| DBR-25 | `Clock` của 000; không có thời điểm từ client cho lệnh của 001 | ArchUnit của 000 |
| DBR-34 | `dang_hieu_luc UNIQUE`; trigger | `NguyenVongPhienBanIT` |
| DBR-39 | `nguyen_vong_dieu_tri.dang_hieu_luc UNIQUE`; trigger | `NguyenVongDieuTriPhienBanIT` |
| BR-M01-01 | Job `CanhBaoQuaHanTroLai`; `canh_bao_tro_lai` | `BR_M01_01_TamVangQuaHanIT` *(R-15: `canh_bao_tro_lai` → `moc_da_nhac`)* |
| BR-M01-02 | `lan_bao_danh_gia_lai`; hai job; listener sự cố | `BR_M01_02_BaoDanhGiaLaiIT` *(R-15: `lan_bao_danh_gia_lai` → `ho_so_chung`)* |
| BR-M01-03 | Tác động xác nhận (H-10); `YeuCauDoiMucApi.thayTheBoiDanhGia`; job nhắc | `BR_M01_03_DoiMucIT` |
| BR-M01-04 | Bảng chuyển trong mã; lịch sử chỉ thêm | `BR_M01_04_BangChuyenTest`, `LichSuTrangThaiIT` |
| BR-M01-05 | `HoSoMoGuard`; ArchUnit buộc gọi guard | `BR_M01_05_HoSoDongIT`, `HoSoMoGuardArchTest` |
| BR-M01-06 | Endpoint riêng cho từng lệnh; API cho 014, 007 | `BR_M01_06_LenhRiengIT` |
| BR-M01-07 | Trigger; sự kiện kiểm tra dị ứng | `BR_M01_07_DiUngIT` |
| BR-M01-08 | Chú thích ¹ | `BR_M01_08_DongYIT` |
| BR-M01-09 | Bảng chuyển đánh giá; quyền DUYET chỉ AC-03 | `BR_M01_09_DanhGiaIT` |
| BR-M01-10 | `DanhGiaApi.luuYHienHanh` | `BR_M01_10_LuuYChamSocIT` |
| BR-M01-11 | Danh mục `muc_do_di_ung`, `nguon_thong_tin` | `BR_M01_11_DanhMucIT` |
| BR-M01-12 | Cờ chờ kiểm tra; outbox; job thử lại | `BR_M01_12_ChoKiemTraIT` |
| BR-M01-13 | `nhap_dang_mo UNIQUE`; lệnh Hủy nháp | `BR_M01_13_MotBanNhapIT` |
| BR-M01-14 (phần 001) | Mẫu cột nguồn ý kiến; `NguonYKienApi`; `nguyen_vong_y_kien` | `BR_M01_14_NguonYKienIT` *(R-15: `nguyen_vong_y_kien` → `su_kien_chung`)* |
| BR-M01-15 (phần 001) | FR-011: `CHECK` quyết định của Tòa án; FR-023c: `CHECK` người ký; FR-023h: test kiến trúc | `FR_011_DaiDienPhapLuatIT`, `NguyenVongNguoiKyIT`, `FR_023h_KhongDauNangLucArchTest` |
| BR-M01-16 | Mục 6b; cảnh báo `THIEU_NGUYEN_VONG_DIEU_TRI` của `ThamGiaM01`; `NguyenVongApi.dieuTriHieuLuc` | `NguyenVongDieuTriPhienBanIT`, `NguyenVongDieuTriGioiHanXemIT`, `HoanTatTiepNhanCanhBaoDieuTriIT` |
| FR-047c (BR-M05-32) | `tepDinhKemIds` của lệnh trạng thái; `M01.GIAY_TO_BENH_VIEN` | `GiayToBenhVienIT` |
| FR-045, FR-047 | Gộp điều kiện; tác động trong transaction (H-02) | `FR_045_LietKeDieuKienIT`, `FR_047_RollbackTacDongIT` |
| FR-048 | `SELECT … FOR UPDATE` | `FR_048_LenhDongThoiIT` |
| FR-053 | `lich_su_muc_cham_soc` | `FR_053_MucTaiNgayIT` *(R-15: `lich_su_muc_cham_soc` → `lich_su_chung`)* |
