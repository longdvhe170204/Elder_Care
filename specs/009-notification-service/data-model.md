# Data Model: Dịch vụ thông báo (009)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bản ghi nguồn, người cao tuổi, tầng/khu vực, nhân viên, người thân, tài khoản là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Thời điểm lưu `DATETIME(6)` (000 R-04; DBR-25). Không có DBR trong Phụ lục 28 cho thông báo; các bất biến của spec (FR-005, FR-012, FR-032) được đưa xuống cơ sở dữ liệu ở mục 8.

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp: `yeu_cau_thong_bao` (nội dung, nhóm nhận thành `JSON`); `thong_bao` (+ `thong_bao_trang_thai`: cột trạng thái dẫn xuất được sửa sau khóa); `luot_nhac_goi` (+ `luot_nhac_goi_trang_thai`, như trên).
- `danh_muc_chung`: `kenh_thong_bao`.
- `su_kien_chung`: `su_kien_thong_bao`, `lan_gui`, `ket_qua_lan_gui`, `luot_nhac_goi_thong_bao`, `chuyen_nhac`, `ket_qua_goi`, `ket_qua_lien_he`.
- `ho_so_chung`: `lien_he_truc_tiep`; `moc_da_nhac`: `notification_request_duplicate`; `ho_so_chung`: `khung_gop_tin_nhan` (giữ dữ liệu khung, không chỉ để khóa); `moc_da_nhac`: `m13_job_run` (lần chạy ra file log); giữ `thiet_bi_day` (`push_device`).
- Bảng phụ, bảng con *(bổ sung 2026-10-30, rà đủ tên bảng của data-model)*: `lan_chay_m13` → `moc_da_nhac`; `yeu_cau_phan_noi_dung`, `yeu_cau_nhom_nhan` → cột `JSON` của `yeu_cau_thong_bao`. Không thêm bảng riêng.
- Còn 4 bảng riêng của feature.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-29; tên mã theo bảng đã cài, migration `V009_95`)*

Mô tả dưới đây thay các mục 2 → 7 khi cài 009 T043; cột không nhắc lại giữ như bảng đã cài (`V009_01` → `V009_04`).

**`notification_request`** (`yeu_cau_thong_bao`) – nhóm 3, chỉ thêm. Giữ mọi cột, `UNIQUE (source_record_type, source_record_id, event_key)`, `CHECK` lý do từ chối, trigger không sửa, không xóa. Thêm:

| Cột | Kiểu | Thay cho |
| --- | --- | --- |
| `contents` | `JSON NOT NULL` | `notification_request_content`: mảng `{seq, informationType, content}`, `seq` không trùng |
| `recipient_groups` | `JSON NOT NULL` | `notification_request_recipient_group`: mảng `{seq, groupCode, params, substitution, substituteGroup, outcome}` |

Hai cột ghi cùng câu `INSERT` (trigger hiện có đã chặn sửa); backend kiểm JSON Schema `M13.RequestContents`, `M13.RecipientGroups` trước khi ghi.

**`notification`** (`thong_bao`) – nhóm 3, khóa theo dòng (`khoa_luc` = `created_at`). Giữ mọi cột, `UNIQUE (request_id, recipient_type, recipient_id)`. Thêm các cột dẫn xuất của `notification_state` (bỏ bảng): `status`, `scheduled_send_at`, `sent_at`, `viewed_at`, `acknowledged_at`, `acknowledgement_method`, `hidden`, `resident_id`; `recipient_key` thành cột sinh `CONCAT(recipient_type, ':', recipient_id)`. Chỉ mục `(recipient_key, sent_at)`, `(status, scheduled_send_at)`, `(level, status, sent_at)` như `notification_state`. Quyền `eldercare_app`: `SELECT, INSERT`, `UPDATE (status, scheduled_send_at, sent_at, viewed_at, acknowledged_at, acknowledgement_method, hidden)` (quyền theo cột); trigger `trg_notification_row_lock` chặn đổi mọi cột khác.

**`call_reminder`** (`luot_nhac_goi`) – nhóm 3, khóa theo dòng (`khoa_luc` = `created_at`). Giữ mọi cột. Thêm cột dẫn xuất của `call_reminder_state` (bỏ bảng): `status`, `final_result`, `stop_reason`, `due_at`, `current_staff_id`, `callee_key`; cột sinh `pending_key` `UNIQUE` và chỉ mục `(status, due_at)` như cũ. Quyền theo cột `UPDATE (status, final_result, stop_reason, due_at, current_staff_id)`; trigger chặn đổi cột khác.

**`push_device`** – kỹ thuật, giữ nguyên (token đẩy do 009 quản lý, không chuyển sang `thiet_bi` của 002).

**Loại ở bảng chung** (JSON Schema ở `m13/internal/.../schema`, tên theo glossary mục 15):

| Bảng chung | `loai` | Đối tượng | `noi_dung` / `thuoc_tinh` | `khoa_duy_nhat`, ràng buộc chuyển về backend |
| --- | --- | --- | --- | --- |
| `danh_muc_chung` | `M13.NOTIFICATION_CHANNEL` | — | `ma` = mã kênh; `{enabled, levels[], config}` | `UNIQUE (loai, ma)` |
| `su_kien_chung` | `M13.NOTIFICATION_EVENT` | `NOTIFICATION` | `{type, acknowledgementMethod, reason, scheduledSendAt}`; `nguoi_thuc_hien_id` = `actor_id` | `M13.NE:<notificationId>:<type>` với `SEND`, `NOT_SENT`, `VIEW`, `ACKNOWLEDGE` (thay `once_key`); `acknowledgementMethod` có khi và chỉ khi `ACKNOWLEDGE`; `scheduledSendAt` bắt buộc với `HOLD`, `RESCHEDULE` |
| `su_kien_chung` | `M13.DELIVERY_ATTEMPT` | `NOTIFICATION` | `{channel, address, attemptNo, scheduledAt, batchWindowId, content}` | `M13.DA:<notificationId>:<channel>:<attemptNo>`; cột sinh `m13_scheduled_at` có chỉ mục cho job gửi |
| `su_kien_chung` | `M13.DELIVERY_RESULT` | `DELIVERY_ATTEMPT` (id sự kiện lần gửi) | `{result, reason, digestAttemptId}` | `M13.DR:<attemptId>` (một kết quả mỗi lần gửi) |
| `su_kien_chung` | `M13.CALL_REMINDER_NOTIFICATION` | `CALL_REMINDER` | `{notificationId, mergedAt}` | `M13.CRN:<notificationId>` |
| `su_kien_chung` | `M13.CALL_REMINDER_HANDOFF` | `CALL_REMINDER` | `{previousStaffId, remindedStaffId, tier, reason, dueAt}` | — |
| `su_kien_chung` | `M13.CALL_RESULT` | `CALL_REMINDER` | `{calledAt, calledPhoneNumber, result, reason, answeredBy, note, supplementary}`; `nguoi_thuc_hien_id` = `called_by` | `reason` bắt buộc khi `NOT_REACHED`, `note` bắt buộc khi `REACHED` |
| `su_kien_chung` | `M13.DIRECT_CONTACT_RESULT` | `DIRECT_CONTACT` (id hồ sơ) | `{occurredAt, result, reason, note}`; `nguoi_thuc_hien_id` = `contacted_by` | `M13.DCR:<directContactId>`; `reason` bắt buộc khi `UNREACHABLE` |
| `ho_so_chung` | `M13.DIRECT_CONTACT` | `NOTIFICATION` | `{familyMemberId, fullName, relationship, phoneNumber, shortContent, remindedAt}`; `trang_thai` `PENDING`, `CONTACTED`, `UNREACHABLE` | `M13.DC:<notificationId>`; `khoa_luc` đặt khi rời `PENDING` (thay `trg_direct_contact_final`) |
| `ho_so_chung` | `M13.SMS_BATCH_WINDOW` | — | `{recipientKey, startedAt, endsAt, firstAttemptId, digestAttemptId}`; `trang_thai` `OPEN`, `CLOSED` | `M13.SBW:<recipientKey>` khi `OPEN`, `NULL` khi đóng (thay `open_key`); cột sinh `m13_window_ends_at` có chỉ mục |
| `moc_da_nhac` | `M13.REQUEST_DUPLICATE` | `NOTIFICATION_REQUEST` | `moc` = thời điểm nhận bản trùng | |
| `moc_da_nhac` | `M13.JOB_DONE` | `JOB` (mã job) | `moc` = ngày nghiệp vụ (thay `m13_job_run`; lần chạy ra `logs/jobs.log`) | |

**Chuyển dữ liệu (`V009_95`)**: chép `notification_request_content`, `notification_request_recipient_group` vào JSON; `notification_state` vào `notification` (migration chạy bằng tài khoản Flyway: xóa trigger cũ, chép, tạo trigger mới); `call_reminder_state` vào `call_reminder`; các bảng sự kiện, kết quả, liên hệ trực tiếp, khung gộp tin, bản trùng, `m13_job_run` vào bảng chung, cấp id mới, ghi `legacy`. Các id tham chiếu trong JSON (`digestAttemptId`, `firstAttemptId`, `batchWindowId`) đổi sang id mới theo bảng ánh xạ tạm trong migration. `delivery_attempt` không là đích của `dinh_chinh`.

**Bảo đảm DBR, FR** (thay phần tương ứng ở mục 8): một sự kiện `SEND`, `VIEW`, `ACKNOWLEDGE` mỗi thông báo và một kết quả mỗi lần gửi bằng `khoa_duy_nhat`; một lượt nhắc gọi đang chờ mỗi người cần gọi bằng `pending_key`; một khung gộp tin mở mỗi người nhận bằng `khoa_duy_nhat`. Test: `NotificationEventOnceIT`, `DeliveryResultOnceIT`, `CallReminderPendingUniqueIT`, `SmsBatchWindowOpenUniqueIT` (viết mới hoặc đổi từ test hiện có), `RecordGroupTriggerIT` với `notification`, `call_reminder`.

**Bảng truy vết sau gộp** *(bổ sung 2026-10-30; khi làm task gộp, sửa cột "bảo đảm bằng" của bảng DBR, BR ở các mục sau theo bảng này)*:

| Mã | Bảo đảm sau gộp | Test |
| --- | --- | --- |
| FR-005 | Không trùng yêu cầu: `UNIQUE (source_record_type, source_record_id, event_key)` giữ; bản trùng ở `moc_da_nhac` | `FR_005_DeduplicationIT` |
| FR-012 | Một thông báo mỗi người nhận mỗi yêu cầu: `UNIQUE (request_id, recipient_type, recipient_id)` giữ | test nhận người nhận hiện có |
| FR-032 | Một lượt nhắc gọi đang chờ mỗi người cần gọi: `call_reminder.pending_key` `UNIQUE` | `BR_M13_02_CallReminderIT` |
| NFR (L2) | Sự kiện thông báo, lần gửi vào `su_kien_chung` (bảng lớn nhất): truy vấn theo `(loai_doi_tuong, doi_tuong_id, loai)`; hộp thông báo đọc `notification` (cột dẫn xuất có chỉ mục) | đo lại phần NFR của quickstart sau T043 |

**Test phải đổi bảng đích** *(rà 2026-10-30; 23 file đọc, ghi thẳng bảng cũ bằng SQL)*: giữ kịch bản nghiệp vụ, chỉ đổi bảng, cột đích.
- Test của module: `BR_M13_01_ChannelSelectionIT`, `BR_M13_02_CallReminderIT`, `BR_M13_03_QuietHoursIT`, `BR_M13_04_ShiftGroupsIT`, `CallReminderTestBase`, `FR_003_RequestValidationIT`, `FR_005_DeduplicationIT`, `FR_014_015_ConsentAndDeathIT`, `FR_016_ChannelConfigIT`, `FR_016a_OffShiftIT`, `FR_016b_SmsBatchingIT`, `FR_021_DirectContactIT`, `FR_022a_ResidentRecipientIT`, `FR_024_ParameterChangeIT`, `FR_026_AcknowledgeIT`, `FR_029a_CallResultIT`, `FR_031_CorrectionResidentIT`, `InboxIT`, `M13ProvidersIT`, `M13RecordGroupTriggerIT`, `NFR_03_DeliveryDispatcherIT`, `NFR_04_M13CatchUpIT`, `RecipientResolutionIT`.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `kenh_thong_bao` | 1 | G (FR-016, FR-039) *(R-15: `kenh_thong_bao` → `danh_muc_chung`)* |
| `yeu_cau_thong_bao`, `yeu_cau_phan_noi_dung`, `yeu_cau_nhom_nhan` | 3 | A (FR-001 → FR-005) *(R-15: `yeu_cau_phan_noi_dung`, `yeu_cau_nhom_nhan` → cột `JSON` của `yeu_cau_thong_bao`)* |
| `thong_bao` | 3 | B, F (một người nhận một dòng) |
| `su_kien_thong_bao` | 3 | F (FR-044, trạng thái suy ra) *(R-15: `su_kien_thong_bao` → `su_kien_chung`)* |
| `lan_gui`, `ket_qua_lan_gui` | 3 | C (FR-018) *(R-15: `lan_gui`, `ket_qua_lan_gui` → `su_kien_chung`)* |
| `luot_nhac_goi`, `luot_nhac_goi_thong_bao`, `chuyen_nhac` | 3 | E (FR-027 → FR-030a) *(R-15: `luot_nhac_goi_thong_bao`, `chuyen_nhac` → `su_kien_chung`)* |
| `ket_qua_goi` | 3 | E (FR-030, FR-031) *(R-15: `ket_qua_goi` → `su_kien_chung`)* |
| `lien_he_truc_tiep` | 2 | C (FR-021) *(R-15: `lien_he_truc_tiep` → `ho_so_chung`)* |
| `ket_qua_lien_he` | 3 | C (FR-021, FR-031) *(R-15: `ket_qua_lien_he` → `su_kien_chung`)* |
| `thiet_bi_day` | Kỹ thuật | C (token FCM theo tài khoản, thiết bị; 002 cấp mã thiết bị) |

---

## 2. `kenh_thong_bao` – nhóm 1 → gộp theo R-15: `kenh_thong_bao` → `danh_muc_chung`

`ma` (`TRONG_UNG_DUNG`, `CONG_NGUOI_THAN`, `TIN_NHAN`, `KHAC_*`; khóa chính), `ten`, `bat`, `muc_gan` `SET('NHE','TRUNG_BINH','KHAN_CAP')` (chỉ với kênh khác; mặc định Khẩn cấp), `cau_hinh` `JSON` (thông tin nhà cung cấp; không có bí mật, bí mật ở cấu hình triển khai), `phien_ban`. Đổi có nhật ký và lý do (FR-040).

---

## 3. Yêu cầu thông báo (mục A)

### `yeu_cau_thong_bao` – nhóm 3

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `module_nguon`, `loai_su_kien`, `nhom_nguon` | | Nhóm theo mục 17 | FR-001 |
| `loai_ban_ghi_nguon`, `id_ban_ghi_nguon`, `khoa_su_kien` | `VARCHAR(64)`, `VARCHAR(64)`, `VARCHAR(191)` | `UNIQUE (loai_ban_ghi_nguon, id_ban_ghi_nguon, khoa_su_kien)` | FR-005 |
| `muc` | `ENUM('NHE','TRUNG_BINH','KHAN_CAP')` | Thiếu thì `NHE` | FR-002, FR-003 |
| `nct_id`, `tang_khu_vuc_id` | `NULL` | | FR-001 |
| `noi_dung_rut_gon`, `tieu_de` | | Rút gọn thuộc `CHUNG` | FR-019, FR-034 |
| `nguoi_gay_ra_id`, `bao_nguoi_gay_ra` | | | FR-012 |
| `phat_sinh_luc`, `tiep_nhan_luc` | | | FR-001, FR-004 |
| `ket_qua_tiep_nhan` | `ENUM('HOP_LE','BI_TU_CHOI')` | Lý do khi bị từ chối | FR-003 |
| `dau` | `SET('THIEU_MUC','THIEU_LOAI_THONG_TIN','THIEU_RUT_GON','KHONG_CO_NGUOI_NHAN','VIEC_SAU_QUA_DOI','THIEU_DOI_TUONG_THONG_TIN','THIEU_MA_TRUONG_HOP')` | Đặt lúc tiếp nhận (một lần) | FR-001a, FR-003, FR-011, FR-013, FR-015 |
| `doi_tuong_thong_tin` | `ENUM('VE_NGUOI_CAO_TUOI','VE_VIEC_NGUOI_THAN') NULL` | Khai bởi module nguồn; rỗng khi yêu cầu không có nhóm người thân | FR-001a (bổ sung 2026-10-18) |
| `ma_truong_hop` | `ENUM('KHAN_CAP_NGUOI_LIEN_HE_CHINH','DANH_SACH_TIEP_XUC','XAC_NHAN_LAI_NGUYEN_VONG','CHUYEN_VIEN','QUA_DOI') NULL` | Mã Q-354 do module nguồn khai | FR-001a, FR-014a (bổ sung 2026-10-18) |
Lần nhận trùng (FR-005) không sửa dòng nhóm 3: mỗi lần ghi một dòng ở bảng kỹ thuật `yeu_cau_nhan_trung (yeu_cau_id, thoi_diem)`; view tra cứu đếm số dòng.

### `yeu_cau_phan_noi_dung` *(gộp theo R-15 vào cột `JSON` của `yeu_cau_thong_bao`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `yeu_cau_nhom_nhan` *(gộp theo R-15 vào cột `JSON` của `yeu_cau_thong_bao`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

---

## 4. Thông báo (mục B, F)

### `thong_bao` – nhóm 3 (một dòng mỗi người nhận mỗi yêu cầu)

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `yeu_cau_id` | | |
| `loai_nguoi_nhan` (`NHAN_VIEN`, `NGUOI_THAN`, `NGUOI_CAO_TUOI` bổ sung 2026-10-18), `nguoi_nhan_id`, `tai_khoan_id NULL` | `UNIQUE (yeu_cau_id, loai_nguoi_nhan, nguoi_nhan_id)` | FR-012 |
| `can_cu` `JSON` | Mọi căn cứ: nhóm, "thay <nhóm> – không có người", "tạm nhận – bàn giao chưa xác nhận", "nhận thay – không có người nhận" | FR-009 → FR-012 |
| `phan_duoc_xem` `SET(...)` | Loại thông tin người nhận thấy lúc gửi (người thân) | FR-013 |
| `chi_nhan_dang` | Nhận thay không có phạm vi | FR-011 |
| `ma_truong_hop` `NULL` | Mã Q-354 áp dụng cho chính người nhận này: chỉ có khi người thân nằm ngoài phạm vi bản đồng ý lúc gửi; đây là "bản ghi gửi ghi trường hợp áp dụng" (BR-M10-06) | FR-014a (bổ sung 2026-10-18) |
| `muc` | Chép từ yêu cầu | |
| `tao_luc` | | |

Không `UPDATE`, không `DELETE` (mẫu 000).

### `su_kien_thong_bao` *(gộp theo R-15 vào `su_kien_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `thong_bao_trang_thai` *(gộp theo R-15 vào `thong_bao` (cột dẫn xuất); mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

---

## 5. Gửi theo kênh (mục C)

### `lan_gui` *(gộp theo R-15 vào `su_kien_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `ket_qua_lan_gui` *(gộp theo R-15 vào `su_kien_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `khung_gop_tin_nhan` *(gộp theo R-15 vào `ho_so_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `thiet_bi_day` – kỹ thuật

`tai_khoan_id`, `ma_thiet_bi` (002), `ung_dung` (`APP_NHAN_VIEN`, `APP_NGUOI_THAN`, `APP_NGUOI_CAO_TUOI` bổ sung 2026-10-18), `fcm_token`, `cap_nhat_luc`, `vo_hieu_luc`. Token hết hạn (FCM trả lỗi) thì đặt `vo_hieu_luc`.

---

## 6. Lượt nhắc gọi (mục E)

### `luot_nhac_goi` – nhóm 3

`id`, `chuoi_goi_id` (bằng `id` của lượt đầu chuỗi), `loai_nguoi_can_goi` (`NHAN_VIEN`, `NGUOI_THAN`), `nguoi_can_goi_id`, `nct_id NULL`, `nguon_khi_khong_nct` (loại, id) `NULL`, `tang_khu_vuc_id NULL`, `tao_luc`, `ly_do_tao` (`QUA_HAN_XAC_NHAN`, `GUI_THAT_BAI`, `KHONG_LIEN_LAC_NGUOI_TRUOC`).

### `luot_nhac_goi_thong_bao` *(gộp theo R-15 vào `su_kien_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `chuyen_nhac` *(gộp theo R-15 vào `su_kien_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `luot_nhac_goi_trang_thai` *(gộp theo R-15 vào `luot_nhac_goi` (cột dẫn xuất); mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `ket_qua_goi` *(gộp theo R-15 vào `su_kien_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

---

## 7. Liên hệ trực tiếp (FR-021)

### `lien_he_truc_tiep` *(gộp theo R-15 vào `ho_so_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

### `ket_qua_lien_he` *(gộp theo R-15 vào `su_kien_chung`; mô tả cũ đã bỏ 2026-10-30, lược đồ và ràng buộc ở mục 0.1)*

---

## 8. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bất biến | Cách bảo đảm | Nguồn |
| --- | --- | --- |
| Yêu cầu duy nhất theo (bản ghi nguồn, khóa sự kiện) | `UNIQUE` trên `yeu_cau_thong_bao` | FR-005, NFR-04 |
| Mỗi người nhận tối đa một thông báo mỗi yêu cầu | `UNIQUE (yeu_cau_id, loai_nguoi_nhan, nguoi_nhan_id)` | FR-012 |
| Mỗi thông báo thuộc tối đa một lượt nhắc tự động | `UNIQUE (thong_bao_id)` trên `luot_nhac_goi_thong_bao` | FR-032 *(R-15: `luot_nhac_goi_thong_bao` → `su_kien_chung`)* |
| Tối đa một lượt Chờ mỗi (người cần gọi, người cao tuổi) | `UNIQUE khoa_cho` | FR-032, Q-100 |
| Xem lần đầu, xác nhận một lần | `UNIQUE` cột sinh trên `su_kien_thong_bao` | FR-034 *(R-15: `su_kien_thong_bao` → `su_kien_chung`)* |
| Thông báo, lần gửi, xác nhận, kết quả gọi không sửa, không xóa | Nhóm 3 theo mẫu 000 (tài khoản `eldercare_app` không có `UPDATE`, `DELETE`) | FR-036 |
| Gửi lại không trùng | `UNIQUE (thong_bao_id, kenh, lan_thu)` | FR-018, NFR-04 |
| Một khung gộp mở mỗi người | `UNIQUE khoa_mo` | FR-016b |
| Thời điểm theo đồng hồ máy chủ | `Clock`; thời điểm thiết bị không dùng | NFR-09, DBR-25 |

---

## 9. Nhật ký và phân loại trường

- Nhật ký (`NhatKyApi`) cho lệnh người dùng: xác nhận Khẩn cấp, kết quả gọi, liên hệ trực tiếp, cấu hình kênh (lý do). Hoạt động của hệ thống được ghi ở `su_kien_thong_bao`, `ket_qua_lan_gui`, `chuyen_nhac` (người thực hiện Hệ thống, sự kiện kích hoạt) (FR-040; research B-13). *(R-15: `su_kien_thong_bao`, `ket_qua_lan_gui`, `chuyen_nhac` → `su_kien_chung`)*
- `PhanLoaiTruongProvider`: `yeu_cau_phan_noi_dung.noi_dung` theo `loai_thong_tin`; số điện thoại → `LIEN_HE`.

---

## 10. Lưu giữ (000 R-11)

| Loại dữ liệu | Hạn | Chuỗi tham chiếu |
| --- | --- | --- |
| Yêu cầu, thông báo, sự kiện, lần gửi, lượt nhắc, kết quả gọi, liên hệ trực tiếp | `DAI_HON(CFG-M15-04)` và không ngắn hơn bản ghi nguồn (yêu cầu gắn người cao tuổi theo CFG-M15-03 của hồ sơ) | Bản ghi nguồn (`loai_ban_ghi_nguon`, `id`) |
| Bảng kỹ thuật (trạng thái dẫn xuất, khung gộp, thiết bị) | Theo dòng gốc; thiết bị xóa khi token vô hiệu quá 90 ngày (kỹ thuật) | — |

---

## 11. Truy vết BR và FR

| Mã | Bảo đảm ở | Test |
| --- | --- | --- |
| BR-M13-01 | Research B-05 (`ChonKenh`, gộp tin nhắn, ngoài ca) | `BR_M13_01_ChonKenhTest`, `FR_016a_NgoaiCaIT`, `FR_016b_GopTinNhanIT` |
| BR-M13-02 | Research B-07, B-08 | `BR_M13_02_LuotNhacGoiIT`, `FR_029a_ChuoiGoiIT`, `FR_030a_ChuyenNhacIT`, `FR_027a_GopLuotIT` |
| BR-M13-03 | Research B-06 | `BR_M13_03_GioYenTinhIT`, `GioYenTinhTest` |
| BR-M13-04 | Research B-04 | `BR_M13_04_NguoiNhanTheoPhanCongIT`, `SC_004_NguoiNhanTest` (200 yêu cầu) |
| BR-M13-05 | Mục 3 → 6; B-11 | `BR_M13_05_LichSuGuiIT` |
| BR-M10-06 (phần 009) | `XEM` gửi lại module nguồn qua `ThongBaoTraCuuApi`; `thong_bao.ma_truong_hop` (bổ sung 2026-10-18) | `FR_035_DuLieuChoNguonIT`, `FR_014a_MaTruongHopIT` |
| FR-001a, FR-014 (Q-353, sửa 2026-10-18) | `XetPhamViDongY`; `doi_tuong_thong_tin`; dấu `THIEU_DOI_TUONG_THONG_TIN`, `THIEU_MA_TRUONG_HOP` | `XetPhamViDongYTest`, `FR_014_NhanThongBaoDongYIT` |
| DBR-03 (phần 009) | `LocPhanNoiDung` | `FR_013_LocNguoiThanTest` |
| FR-007 (f), FR-022a *(bổ sung 2026-10-18)* | B-02, B-04, B-06 | `FR_022a_NguoiCaoTuoiNhanIT` |
| FR-005, FR-012, FR-032 | Mục 8 | `FR_005_ChongTrungIT`, `FR_012_GopNguoiNhanIT`, `FR_032_MotLuotIT` |
