# Data Model: Chi phí phát sinh (010)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Người cao tuổi, hợp đồng, phiên bản nội dung, dịch vụ, phiên bản đơn giá, lượt vắng, công việc, kết quả, liều, lần giao, lượt ở lại, suất ăn, điểm danh, người thân, tệp là tham chiếu logic (`BIGINT` hoặc mã nguồn), không có khóa ngoại sang module khác. Số tiền và đơn giá lưu `DECIMAL(15,0)` (đồng, số nguyên, FR-013; quy ước 000 cập nhật 2026-10-29); số lượng `DECIMAL(10,2)`; hệ số `DECIMAL(5,2)` (phần trăm). Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp (cùng 017, data-model 017 mục 0): `bang_chi_phi` (+ `ky_chi_phi`; tổng theo loại thành `JSON` khi chốt); `khoan_chi_phi`; `su_kien_tinh_phi`.
- `lich_su_chung`: `lich_su_tinh_lai`; `su_kien_chung`: `bien_dong_nguon_sau_chot`, `de_nghi_gui_nguoi_dai_dien`, `quyet_dinh_mua_ho`.
- `ho_so_chung`: `de_nghi_mua_ho`; `trang_thai_nct`: `chi_phi_nct`; `moc_da_nhac`: `su_kien_nguon_da_xu_ly`; `lan_xuat_ke_toan`, `lan_xuat_bang` → hành động nhật ký.
- Số bảng 4 gồm cả bảng `giao_dich` của 017.
- Bảng phụ, bảng con *(bổ sung 2026-10-30, rà đủ tên bảng của data-model)*: `lan_chay_m11` → `moc_da_nhac`; `bang_chi_phi_tong_loai` → cột `JSON` của `bang_chi_phi`. Không thêm bảng riêng.
- Còn 4 bảng riêng của feature.
- 010 có ba bảng riêng; bảng giao_dich thứ tư thuộc 017, không tạo trong T005a.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-30; sinh từ mục 0 và ràng buộc ở mô tả bảng các mục sau; chủ module rà, hoàn thiện khi viết migration, task T038)*

Quy ước: loại ở bảng chung đặt mã `<module>.<TÊN_BẢNG_CŨ>` (module M11; đổi sang tên tiếng Anh theo glossary khi dịch); `noi_dung`, `thuoc_tinh` hoặc `du_lieu` mang các cột của bảng cũ, có JSON Schema theo loại; bảng gộp riêng của feature có cột `kind` (hoặc `loai`) và cột `NULL` có `CHECK` theo loại. Mỗi dòng có ràng buộc ở bảng dưới cần một test tích hợp chứng minh ràng buộc vẫn giữ sau gộp (constitution XII), ghi vào bảng DBR áp dụng khi cài.

**Bảng giữ hoặc là bảng đích** (giữ cột, ràng buộc như mô tả ở các mục sau; bảng đích nhận thêm cột của bảng gộp vào): `bang_chi_phi`, `khoan_chi_phi`, `su_kien_tinh_phi`.

| Bảng cũ | Đích | Loại ở bảng chung | Ràng buộc của bảng cũ | Cách giữ sau gộp |
| --- | --- | --- | --- | --- |
| `chi_phi_nct` | `trang_thai_nct` | `M11.CHI_PHI_NCT` | — | cột vào `JSON` hoặc cột của bảng đích |
| `ky_chi_phi` | `bang_chi_phi` | — | `id`, `thang` (`yyyy-MM`, `UNIQUE`), `tu_ngay`, `den_ngay` (luôn trọn tháng dương lịch, FR-001), `trang_thai` (`DANG_MO`, `CHO_CHOT`, `DA_CHOT`), `han_chot` (đặt khi sang `CHO_CHOT`, từ CFG-M11-01), `da_chot_luc`. | cột sinh `UNIQUE` theo `kind` |
| `bang_chi_phi_tong_loai` | cột `JSON` của `bang_chi_phi` | — | — | ghi cùng bản ghi cha; domain kiểm |
| `lich_su_tinh_lai` | `lich_su_chung` | `M11.LICH_SU_TINH_LAI` | — | cột vào `JSON` hoặc cột của bảng đích |
| `bien_dong_nguon_sau_chot` | `su_kien_chung` | `M11.BIEN_DONG_NGUON_SAU_CHOT` | — | cột vào `JSON` hoặc cột của bảng đích |
| `su_kien_nguon_da_xu_ly` | `moc_da_nhac` | `M11.SU_KIEN_NGUON_DA_XU_LY` | — | cột vào `JSON` hoặc cột của bảng đích |
| `de_nghi_mua_ho` | `ho_so_chung` | `M11.DE_NGHI_MUA_HO` | Trigger chặn mọi đổi khi trạng thái cuối (`TU_CHOI`, `HUY`, `DA_MUA`). | trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `de_nghi_gui_nguoi_dai_dien` | `su_kien_chung` | `M11.DE_NGHI_GUI_NGUOI_DAI_DIEN` | — | cột vào `JSON` hoặc cột của bảng đích |
| `quyet_dinh_mua_ho` | `su_kien_chung` | `M11.QUYET_DINH_MUA_HO` | — | cột vào `JSON` hoặc cột của bảng đích |
| `lan_xuat_ke_toan` | hành động `nhat_ky` | — | — | cột vào `JSON` hoặc cột của bảng đích |
| `lan_xuat_bang` | hành động `nhat_ky` | — | — | cột vào `JSON` hoặc cột của bảng đích |
| `lan_chay_m11` | `moc_da_nhac` | `M11.LAN_CHAY_M11` | — | cột vào `JSON` hoặc cột của bảng đích |

### 0.2. Lược đồ thực tế của 010 sau R-15 — chuẩn bị T005 ngày 2026-10-30

Mục này thay tên bảng và cách lưu của các mục 2 → 8 khi cài mới. Chỉ ba bảng riêng: cost_statement (gồm kỳ), cost_item, cost_source_index; V010_01/V010_02 đã có trên develop. Bảng giao dịch của 017 do feature 017 làm sau. Không thêm hoặc sửa bảng chung; ghi qua API của A. SQL chỉ đọc dòng có tiền tố M11 trong persistence theo quy ước R-15 ở team-plan mục 8.

**cost_statement**: kind = PERIOD hoặc STATEMENT; id BIGINT. Dòng PERIOD giữ period_month CHAR(7), period_starts_on/period_ends_on DATE, status, finalization_due_on, finalized_at, locked_at, version, created_at/by. Dòng STATEMENT giữ period_id (FK tới PERIOD trong cùng bảng), resident_id, statement_type REGULAR/SUPPLEMENTARY, final_period, through_date, status, total DECIMAL(15,0), totals_by_type JSON, submitted_by/at, change_warning JSON, finalized_by/at, first_export_id, locked_at và cột chuẩn.

- CHECK theo kind: PERIOD không có người cao tuổi/loại bảng/cha; STATEMENT có đủ period_id/resident_id/statement_type, không có các cột tháng/ngày riêng của kỳ. Dòng kỳ là trọn tháng dương lịch.
- unique_key VARCHAR(200) do backend đặt và CHECK đối chiếu: PERIOD:<period_month> hoặc STATEMENT:<period_id>:<resident_id>:<statement_type>; UNIQUE giữ FR-001/002/002a. FK tự tham chiếu cùng trigger kiểm period_id trỏ đúng kind PERIOD.
- totals_by_type là mảng {type, total}; type theo mười loại CostType, total nguyên đồng có dấu. JSON Schema được giữ trong CHECK JSON_SCHEMA_VALID; kiểm tổng/loại không lặp thuộc lệnh chốt T017. Không có bảng cost_statement_type_total.
- status OPEN/AWAITING_FINALIZATION/FINALIZED; FINALIZED có finalized_at và locked_at, trạng thái khác chưa có hai mốc này. Khi đã khóa, trigger chặn mọi đổi kể cả bỏ khóa, đổi status/version/JSON; chỉ cho first_export_id từ NULL sang id một lần (FR-041). Không có bảng lần xuất: giá trị này là tham chiếu hành động nhật ký, bộ dựng xuất/ghi nhật ký thuộc T030.
- Dòng PERIOD đã chốt vẫn là đích tham chiếu cho bảng bổ sung phát sinh muộn. Quyết định lập loại bảng/kỳ là nghiệp vụ của backend, không thêm luật chặn bảng bổ sung vào trigger của kỳ.

**cost_item**: id, resident_id, statement_id, period_id; cost_type (10 loại), origin EVENT_BASED/MANUAL/PURCHASE_ASSISTANCE/ADJUSTMENT; source_type/source_id/billing_unit; purchase_assistance_id; service_id, item_code, description; incurred_on, occurred_at; quantity DECIMAL(10,2), unit, unit_price DECIMAL(15,0), factor_percent DECIMAL(5,2), amount DECIMAL(15,0); unit_price_basis/source_values JSON; flags SET; reason, original_item_id/original_period_id, adjustment_type, source_change_id; checked_by/at, approved_by/at, excess_approval_reason, cancelled_by/cancellation_reason; unique_key, locked_at, version, created_at/by.

- FK chỉ tới bảng M11 hiện có trong migration này. resident/service/purchase_assistance/source_change là tham chiếu logic tới nguồn hoặc bảng chung, chưa thêm FK sang module khác.
- DBR-15: CHECK theo origin giữ đúng nguồn; MANUAL/ADJUSTMENT có lý do. ADJUSTMENT trỏ khoản gốc hoặc nguồn bổ sung cùng kỳ gốc. EVENT_BASED còn hiệu lực có unique_key = <source_type>:<source_id>:<billing_unit>, hủy thì NULL; loại khác NULL. UNIQUE chống lập trùng kể cả đồng thời, không chặn khoản điều chỉnh.
- Tám dấu: INCLUDED_IN_PACKAGE, MISSING_UNIT_PRICE, NOT_YET_MOVED_IN, CREATED_IN_ADVANCE, AFTER_FINALIZED_PERIOD, EXCEEDS_CONSENT, URGENT_EXCESS, AFFECTS_TERMINATION. amount NULL khi và chỉ khi MISSING_UNIT_PRICE; không có hai dấu chính sách vắng đã bỏ theo Q-395.
- status DRAFT/CHECKED/APPROVED/FINALIZED/CANCELLED; hai trạng thái cuối có locked_at. Trigger chặn mọi đổi dòng đã khóa. Thay quantity/unit_price/factor_percent/amount chỉ khi biến phiên @m11_recalculating = 1 (FR-016); dịch vụ tính lại chịu trách nhiệm đặt/xóa biến trên cùng connection khi cài.
- BEFORE INSERT/UPDATE/DELETE kiểm bảng cha: không thêm/sửa/xóa khoản của STATEMENT đã chốt, không đổi khóa cha để thoát bảo vệ; statement/period/resident phải khớp. Chỉ tài khoản retention được xóa dòng đã khóa theo chuỗi hợp lệ; kiểm điều kiện hết hạn thuộc framework/provider, không thuộc trigger.
- Giữ năm chỉ mục: statement/status, resident/incurred_on, source_type/source_id, original_item_id, service_id/status. Lập chỉ mục để tìm khoản không thêm danh sách chưa lập.
- Tính giá, gói, tổng gốc + điều chỉnh không âm, kiểm quyền, trạng thái lệnh, khoảng tính phí/ngày giải phóng giường, nhật ký và phát sự kiện nằm ở backend các task sau; test schema không thay các test này.

**cost_source_index**: khóa chính (source_type, source_id, billing_unit); resident_id, incurred_on, occurred_at, service_id NULL, medication_id NULL, current_quantity DECIMAL(10,2), cancelled, updated_at, version; chỉ mục resident/source_type/incurred_on. Chỉ nhận CARE_TASK_RESULT, DOSE, PRN_ADMINISTRATION, DISPENSE_RECORD, FAMILY_MEAL, OVERNIGHT_STAY, ACTIVITY_ATTENDANCE. Không lưu ngày lưu trú/buổi bán trú, không tự tạo khoản. Đây là chỉ mục kỹ thuật mutable.

**Quyền/trigger ba bảng**: app được SELECT/INSERT/UPDATE; không được DELETE bảng chi phí/khoản; retention được SELECT/DELETE. Chỉ mục kỹ thuật cho app DELETE khi nguồn không còn. Trigger DELETE dùng USER() theo R-05, không dùng CURRENT_USER() của definer. Thời điểm nghiệp vụ do Clock/BusinessTime của caller truyền; fixture dùng mốc riêng, không giả định bảng rỗng.

**Phần chuyển sang bảng chung — API A đã có trên develop `62fdb4f2`, cập nhật 2026-11-04**:

| Bảng chung/API chủ A | Loại/đối tượng của M11 | Nội dung được giữ | Ràng buộc và test ở task sau |
| --- | --- | --- | --- |
| ResidentModuleStateApi | SharedKind M11.RESIDENT_COST_STATE, module M11, resident_id | BillingResidentState {finalCostCompletedFrom: Instant nullable} thay chi_phi_nct | CostLock khóa kỹ thuật trước trạng thái, rồi bảng; CostLockIT xanh. Mỗi người/module chỉ một dòng: 017 phải mở rộng và dùng chung record này, không ghi một payload M11 khác làm mất trường 010; T024 còn cài nghiệp vụ kỳ cuối |
| StatusHistoryApi | HistoryTrack(entityType=M11.COST_ITEM, attribute=RECALCULATION) | CostRecalculation {before, after, cause, resetToDraft}; mỗi snapshot có quantity/unitPrice/factorPercent/amount; basis, thời điểm/người thực hiện ở HistoryEntry chung | CostSharedRowsIT kiểm append-only, transaction và giới hạn DECIMAL(10,2)/(15,0)/(5,2) bằng @Valid/@Digits trước ghi JSON; so giá trị BigDecimal không phụ thuộc scale JSON MySQL. Handler BR_M11_05 còn ở T020/T021 |
| RecordEventApi | M11.POST_FINALIZATION_CHANGE, COST_ITEM | {event, before, after, reference, occurredAt} | Chỉ thêm; khoản điều chỉnh trỏ id sự kiện; FR_027 T020/T022 |
| SimpleRecordApi | M11.PURCHASE_ASSISTANCE_REQUEST | Giữ toàn bộ cột mục 6 trong nội dung, status/locked_at/version ở cột chung | Máy trạng thái/domain M11, khóa yêu cầu khi quyết định; T025–T027 |
| RecordEventApi | M11.PURCHASE_RECIPIENT, PURCHASE_ASSISTANCE_REQUEST | {round, familyMemberId} | unique_key M11.RECIPIENT:<requestId>:<round>:<familyMemberId>, giữ danh sách lúc gửi |
| RecordEventApi | M11.PURCHASE_DECISION, PURCHASE_ASSISTANCE_REQUEST | {type, previousAmount, newAmount, actorType, actorId, channel, reason, insufficientBalance, occurredAt} | Chỉ thêm; một quyết định thắng bằng khóa/version yêu cầu; T025/T027 |
| ReminderMarkers | M11.SOURCE_PROCESSED, M11.BILLING_SOURCE | entityId là residentId, markerKey là khóa sự kiện nguồn | CostSharedRowsIT kiểm UNIQUE và rollback; listener thật còn T010/T021 |
| AuditLogApi hiện có | COST_ACCOUNTING_EXPORT/COST_STATEMENT_EXPORT | Giữ nội dung bộ lọc/phạm vi/định dạng/số bảng/số khoản/tổng/nhãn xuất trước, id bảng trong chi tiết | Không có cost_export/cost_export_statement; T030, nhật ký giao dịch nguyên tử |
| Structured logging | Job 010 | jobRunId, mã job, thời điểm, kết quả; căn cứ nhật ký tự đủ dữ liệu | Không có cost_scheduler_run; T007/T018, theo cấu hình chung A |

Khung T007 lưu mốc kỹ thuật qua ReminderMarkers: M11.JOB_ANCHOR/M11.JOB_DONE, entityType M11.JOB.<tên tác vụ>, entityId DAILY=1/HOURLY=2. Mốc đầu được lưu riêng trước khi chạy để lượt đầu thất bại vẫn chạy lại đúng giờ cũ. Mỗi slot có transaction REQUIRES_NEW chứa marker thành công và tác động; lỗi rollback cả hai, dừng các slot sau của cùng tác vụ. Tác vụ latestOnly đánh dấu slot bỏ qua và chỉ chạy slot mới nhất; không có tác vụ giả trong production. M11Jobs chỉ đọc marker M11 qua CostJobMarkers. Chi tiết kiểm đồng thời/chạy bù ở CostJobCatchUpIT; nghiệp vụ chuyển kỳ/nhắc thuộc các story sau.

Tên loại/nội dung ở trên là hợp đồng dữ liệu của E theo R-15; chưa khai báo thêm chữ ký API của A hoặc tạo fake của A. JSON Schema/đăng ký loại và kiểm tích hợp bảng chung chỉ cài sau T108. Bản thật hiện chưa có administration.shared, ReminderMarkers, TechnicalLocks/V000_09. T005 chỉ đóng khi cả phần cần bảng chung có thể kiểm; ba bảng riêng được tách thành T005a.

| Bất biến sau gộp | Nơi bảo đảm | Test dự kiến của phần độc lập |
| --- | --- | --- |
| DBR-15: đúng nguồn/một khoản hiệu lực | CHECK theo origin + unique_key đối chiếu + UNIQUE | CostSchemaIT.DBR_15_* |
| DBR-17: bảng/khoản đã chốt không đổi | locked_at + trigger giữ dòng và bảo vệ cha cũ/mới | CostSchemaIT.DBR_17_*; test lệnh/tổng không âm vẫn ở T020 |
| FR-001/002/002a: kỳ trọn tháng, bảng duy nhất | CHECK kind/ngày/unique_key + UNIQUE + FK/trigger | CostSchemaIT.FR_001_*, FR_002_* |
| FR-012/016, Q-395 | CHECK dấu/NULL tiền, tám dấu, cổng tính lại theo connection | CostSchemaIT.FR_012_*, FR_016_*, Q_395_* |
| Nhóm dữ liệu/retention | Trigger + GRANT trên MySQL thật | CostSchemaIT.BR_M15_03_* |

T005a đã có V010_01__costs.sql, 15 ca CostSchemaIT xanh; tổng M11 có 74 unit/arch xanh. Cả T005/T038 vẫn chờ phần bảng chung của A. Bằng chứng, bước đỏ và giới hạn ở r15-storage-verification.md.


---

## 1. Tổng quan

> **Sửa 2026-10-29 (Q-395):** bỏ dấu `CHO_QUYET_DINH`, `KHONG_CO_DONG_CHINH_SACH`; phí lưu trú nội trú có `he_so` 100% kể cả ngày vắng, từ chính ngày Giải phóng giường không lập được khoản (mục cập nhật Q-395 và kịch bản mục C của spec). `he_so` còn dùng cho buổi bán trú.

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `chi_phi_nct` | 2 (kỹ thuật) | Đích khóa theo người (P-03) *(R-15: `chi_phi_nct` → `trang_thai_nct`)* |
| `ky_chi_phi` | 2 | A *(R-15: `ky_chi_phi` → `bang_chi_phi`)* |
| `bang_chi_phi` | 2 → 3 khi Đã chốt | A, G |
| `bang_chi_phi_tong_loai` | 3 | A (tổng lúc chốt) *(R-15: `bang_chi_phi_tong_loai` → cột `JSON` của `bang_chi_phi`)* |
| `khoan_chi_phi` | 2 → 3 khi Đã chốt | B, C, D, E, F |
| `lich_su_tinh_lai` | 3 | D (FR-018) *(R-15: `lich_su_tinh_lai` → `lich_su_chung`)* |
| `bien_dong_nguon_sau_chot` | 3 | F (FR-027) *(R-15: `bien_dong_nguon_sau_chot` → `su_kien_chung`)* |
| `su_kien_tinh_phi` | Kỹ thuật (chỉ mục) | B (P-04) |
| `su_kien_nguon_da_xu_ly` | Kỹ thuật | B (chống trùng) *(R-15: `su_kien_nguon_da_xu_ly` → `moc_da_nhac`)* |
| `de_nghi_mua_ho`, `de_nghi_gui_nguoi_dai_dien` | 2 | E *(R-15: `de_nghi_mua_ho` → `ho_so_chung`; `de_nghi_gui_nguoi_dai_dien` → `su_kien_chung`)* |
| `quyet_dinh_mua_ho` | 3 | E *(R-15: `quyet_dinh_mua_ho` → `su_kien_chung`)* |
| `lan_xuat_ke_toan`, `lan_xuat_bang` | 3 | H (FR-041) *(R-15: `lan_xuat_ke_toan`, `lan_xuat_bang` → hành động `nhat_ky`)* |
| `lan_chay_m11` | Kỹ thuật | Job *(R-15: `lan_chay_m11` → `moc_da_nhac`)* |

---

## 2. `chi_phi_nct` – nhóm 2 (kỹ thuật) → gộp theo R-15: `chi_phi_nct` → `trang_thai_nct`

`nct_id` (khóa chính), `mien_hoan_thanh_tu NULL` (FR-036: thời điểm mục "chốt chi phí kỳ cuối" Hoàn thành sau qua đời), `phien_ban`.

---

## 3. Kỳ và bảng (mục A, G)

### `ky_chi_phi` – nhóm 2 → gộp theo R-15: `ky_chi_phi` → `bang_chi_phi`

`id`, `thang` (`yyyy-MM`, `UNIQUE`), `tu_ngay`, `den_ngay` (luôn trọn tháng dương lịch, FR-001), `trang_thai` (`DANG_MO`, `CHO_CHOT`, `DA_CHOT`), `han_chot` (đặt khi sang `CHO_CHOT`, từ CFG-M11-01), `da_chot_luc`.

| Từ | Sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Tới ngày 1 của tháng | `DANG_MO` | Hệ thống (theo lịch) |
| `DANG_MO` | Hết ngày cuối tháng | `CHO_CHOT` | Bộ lập lịch |
| `CHO_CHOT` | Bảng cuối cùng của kỳ Đã chốt | `DA_CHOT` | Hệ thống (trong lệnh chốt bảng) |

### `bang_chi_phi` – nhóm 2 → 3 khi Đã chốt

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `ky_id`, `nct_id`, `loai_bang` (`THUONG`, `BO_SUNG`) | `UNIQUE (ky_id, nct_id, loai_bang)` | FR-002, FR-002a |
| `ky_cuoi`, `den_ngay NULL` | Kỳ cuối tính đến E | FR-034 |
| `trang_thai` (`DANG_MO`, `CHO_CHOT`, `DA_CHOT`) | | Bảng trạng thái bảng |
| `tong` `DECIMAL(15,0) NULL` | Lưu khi chốt | FR-033 |
| `gui_chot_boi_id`, `gui_chot_luc`, `canh_bao_bien_dong` `JSON NULL` | | FR-032 |
| `chot_boi_id`, `chot_luc` | | FR-033 |
| `lan_xuat_dau_id NULL` | Đặt một lần (trigger cho phép duy nhất cột này sau chốt) | FR-041 |
| `phien_ban` | `If-Match` cho lệnh chốt | FR-033 |

Trigger: khi `OLD.trang_thai = 'DA_CHOT'` chỉ cho đặt `lan_xuat_dau_id` từ `NULL`; mọi đổi khác bị chặn. `BEFORE DELETE` theo mẫu 000.

**Bảng chuyển** (khớp bảng trạng thái bảng chi phí):

| Từ | Lệnh / sự kiện | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Lập khoản đầu tiên; lập hồ sơ kết thúc (004); lập khoản điều chỉnh cần bảng bổ sung | `DANG_MO` | Hành chính (lệnh của 010 hoặc 004) | FR-002, FR-002a |
| `DANG_MO` | Gửi chốt | `CHO_CHOT` | Hành chính | Thường: đã qua ngày cuối hoặc kỳ cuối; không còn Nháp |
| `CHO_CHOT` | Trả lại | `DANG_MO` | Quản lý viện | Lý do |
| `CHO_CHOT` | Có khoản Nháp mới / khoản về Nháp | `DANG_MO` | Hệ thống | — |
| `CHO_CHOT` | Chốt | `DA_CHOT` | Quản lý viện | Mọi khoản Đã duyệt hoặc Đã hủy |

### `bang_chi_phi_tong_loai` – nhóm 3 → gộp theo R-15: `bang_chi_phi_tong_loai` → cột `JSON` của `bang_chi_phi`

`bang_id`, `loai_chi_phi` (FR-014a), `tong DECIMAL(15,0)`. Ghi khi chốt; nhóm 3 không cho sửa, xóa theo quy ước 000.

---

## 4. Khoản chi phí (mục B → F)

### `khoan_chi_phi` – nhóm 2 → 3 khi Đã chốt

| Cột | Kiểu / ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `bang_id`, `ky_id` | | FR-014 |
| `loai_chi_phi` | `ENUM` 10 loại của FR-014a | FR-014a |
| `nguon_sinh` | `ENUM('THEO_SU_KIEN','NHAP_TAY','MUA_HO','DIEU_CHINH')` | FR-014 |
| `loai_nguon`, `id_nguon`, `don_vi_tinh_phi` | Với `THEO_SU_KIEN`, điều chỉnh bổ sung; ví dụ `NGAY_LUU_TRU`, `nct:2026-10-05`, `NGAY` | DBR-15 |
| `de_nghi_mua_ho_id NULL` | Với `MUA_HO` | FR-024 |
| `dich_vu_id NULL`, `ma_vat_pham NULL`, `mo_ta` | Thuốc: mã vật phẩm; tên thuốc không lưu ở khoản (đọc qua 006 khi được phép xem) | FR-019 |
| `ngay_phat_sinh`, `thoi_diem_phat_sinh NULL` | | FR-005, FR-036 |
| `so_luong`, `don_vi`, `don_gia NULL`, `he_so NULL`, `so_tien NULL` | Đơn giá, số tiền `DECIMAL(15,0)`; `so_tien NULL` khi `THIEU_DON_GIA` | FR-008 → FR-013 |
| `can_cu_don_gia` `JSON` | Phiên bản nội dung hợp đồng hoặc phiên bản đơn giá | FR-008 |
| `gia_tri_nguon_da_dung` `JSON` | Giá trị ngày lưu trú, trạng thái có mặt, số lượng nguồn lúc tính | FR-005a |
| `dau` `SET('THUOC_GOI','THIEU_DON_GIA','CHUA_VAO_O','LAP_TRUOC','PHAT_SINH_THUOC_KY_DA_CHOT','VUOT_SO_DONG_Y','MUA_GAP_VUOT','ANH_HUONG_KET_THUC')` | | FR-014 |
| `ly_do` | Bắt buộc với nhập tay, điều chỉnh | FR-021, FR-025 |
| `khoan_goc_id NULL`, `ky_goc_id NULL`, `loai_dieu_chinh NULL`, `bien_dong_id NULL` | Điều chỉnh | FR-025, FR-027 |
| `trang_thai` | `ENUM('NHAP','DA_KIEM_TRA','DA_DUYET','DA_CHOT','DA_HUY')` | Bảng khoản |
| `nguoi_tao_id`, `kiem_tra_boi_id`, `kiem_tra_luc`, `duyet_boi_id`, `duyet_luc`, `ly_do_chap_nhan_vuot`, `huy_ly_do`, `huy_boi_id NULL` (rỗng = Hệ thống) | | FR-014, FR-024 |
| `khoa_don_vi_hieu_luc` | Cột sinh `IF(nguon_sinh = 'THEO_SU_KIEN' AND trang_thai <> 'DA_HUY', CONCAT(loai_nguon, ':', id_nguon, ':', don_vi_tinh_phi), NULL)`, `UNIQUE` | DBR-15, FR-006 |
| `phien_ban` | | |

Chỉ mục `(bang_id, trang_thai)`, `(nct_id, ngay_phat_sinh)`, `(loai_nguon, id_nguon)`, `(khoan_goc_id)`, `(dich_vu_id, trang_thai)` (tính lại thiếu đơn giá).

Trigger: chặn mọi đổi khi `OLD.trang_thai IN ('DA_CHOT','DA_HUY')`; chặn đổi `so_luong`, `don_gia`, `he_so`, `so_tien` khi không kèm biến phiên `@tinh_lai` (chỉ service tính lại đặt, FR-016). Ràng buộc "tổng gốc + điều chỉnh không âm" (FR-026) kiểm ở backend dưới khóa bảng chứa khoản gốc.

**Bảng chuyển** (khớp bảng trạng thái khoản):

| Từ | Lệnh / sự kiện | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Lập theo sự kiện | `NHAP` | Hành chính | FR-005, FR-006, FR-007, FR-011 |
| — | Lập nhập tay | `NHAP` | Hành chính | Ngoài danh mục; lý do |
| — | Ghi đã mua | `NHAP` | Hành chính | Đề nghị Được phép mua; FR-024 |
| — | Lập điều chỉnh | `NHAP` | Hành chính | FR-025, FR-026 |
| `NHAP` | Tính lại | `NHAP` | Hệ thống | Nguồn đổi; có đơn giá cho khoản thiếu; ngày lập trước kết thúc |
| `NHAP` | Kiểm tra | `DA_KIEM_TRA` | Hành chính | Có số tiền |
| `DA_KIEM_TRA` | Bỏ kiểm tra | `NHAP` | Hành chính | Bảng Đang mở; lý do |
| `NHAP` | Hủy | `DA_HUY` | Hệ thống (nguồn bị hủy, ngoài thời gian tính phí); Hành chính (khoản tự lập) | Lý do / tham chiếu |
| `DA_KIEM_TRA`, `DA_DUYET` | Nguồn đổi / bị hủy | `NHAP` rồi tính lại hoặc hủy | Hệ thống | Chưa chốt |
| `DA_KIEM_TRA` | Duyệt | `DA_DUYET` | Quản lý viện | Khoản vượt: lý do chấp nhận |
| `DA_KIEM_TRA`, `DA_DUYET` | Trả lại | `NHAP` | Quản lý viện | Lý do; bảng chưa chốt |
| `DA_DUYET` | Chốt bảng | `DA_CHOT` | Hệ thống (trong lệnh chốt của Quản lý viện) | — |

### `lich_su_tinh_lai` – nhóm 3 → gộp theo R-15: `lich_su_tinh_lai` → `lich_su_chung`

`khoan_id`, `thoi_diem`, `truoc` `JSON` (số lượng, đơn giá, hệ số, số tiền, dấu), `sau` `JSON`, `nguyen_nhan` (`NGUON_DINH_CHINH`, `NGAY_LUU_TRU_DOI`, `CO_MAT_DOI`, `NGAY_NGHI_BAN_TRU`, `NHAN_LAI_THUOC`, `CO_DON_GIA`, `HET_LAP_TRUOC`, `PHU_LUC`), `can_cu` (tham chiếu bản đính chính, sự kiện), `chuyen_ve_nhap` (bool).

### `bien_dong_nguon_sau_chot` – nhóm 3 → gộp theo R-15: `bien_dong_nguon_sau_chot` → `su_kien_chung`

`id`, `khoan_goc_id`, `su_kien` (`DINH_CHINH`, `HUY_GHI_NHAN`, `QUYET_DINH_GIU_GIUONG`, `NHAN_LAI_THUOC`, `DOI_NGAY_KET_THUC`, `QUA_DOI`, `AP_DUNG_DOI_PHONG`), `gia_tri_truoc`, `gia_tri_sau` `JSON`, `tham_chieu`, `thoi_diem`. Khoản điều chỉnh do Hành chính lập trỏ `bien_dong_id` (FR-027).

---

## 5. Chỉ mục sự kiện tính phí (kỹ thuật)

### `su_kien_tinh_phi`

`loai_nguon` (`KET_QUA_CONG_VIEC`, `LIEU`, `LAN_DUNG_PRN`, `LAN_GIAO`, `SUAT_AN_NGUOI_THAN`, `DEM_O_LAI`, `DIEM_DANH_HOAT_DONG`), `nguon_id`, `don_vi`, khóa chính ba cột; `nct_id`, `ngay_phat_sinh`, `thoi_diem`, `dich_vu_id NULL`, `thuoc_id NULL`, `so_luong_hien_hanh`, `da_huy`, `cap_nhat_luc`, `phien_ban`. Chỉ mục `(nct_id, loai_nguon, ngay_phat_sinh)`. Cập nhật bởi listener; không có lệnh người dùng. Ngày lưu trú, buổi bán trú không lưu ở đây (đọc `NgayLuuTruApi`).

### `su_kien_nguon_da_xu_ly` → gộp theo R-15: `su_kien_nguon_da_xu_ly` → `moc_da_nhac`

`khoa` khóa chính, `xu_ly_luc` (NFR-04).

---

## 6. Đề nghị mua hộ (mục E)

### `de_nghi_mua_ho` – nhóm 2 → gộp theo R-15: `de_nghi_mua_ho` → `ho_so_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `vat_pham_id NULL`, `mo_ta`, `so_luong` | | FR-023 |
| `so_tien_du_kien`, `so_tien_duoc_phep NULL`, `cfg_m11_02_luc_gui` | `DECIMAL(15,0)` | FR-023, FR-024, Q-259 |
| `ly_do`, `nguoi_yeu_cau_loai` (`NHAN_VIEN`, `NGUOI_THAN`), `nguoi_yeu_cau_id` | | FR-023 |
| `trang_thai` | `ENUM('NHAP','CHO_DONG_Y','DUOC_PHEP_MUA','TU_CHOI','HUY','DA_MUA')` | Bảng đề nghị |
| `dang_cho_bo_sung`, `so_tien_bo_sung_xin NULL` | Số tiền `DECIMAL(15,0)` | Q-251, Q-269 |
| `so_tien_thuc_te`, `ngay_mua`, `tep_chung_tu_id`, `ly_do_mua_gap`, `nguoi_yeu_cau_mua_gap_id` | Số tiền `DECIMAL(15,0)`; id giữ `BIGINT` | FR-024, Q-269 |
| `gui_luc`, `da_nhac_luc`, `da_bao_qlv_luc` | Nhắc CFG-M15-05, 06 | Bảng đề nghị |
| `phien_ban` | | |

Trigger chặn mọi đổi khi trạng thái cuối (`TU_CHOI`, `HUY`, `DA_MUA`).

**Bảng chuyển** (khớp bảng đề nghị mua hộ):

| Từ | Lệnh | Đến | Người |
| --- | --- | --- | --- |
| — | Lập | `NHAP` | Hành chính |
| `NHAP` | Gửi (≤ CFG-M11-02) | `DUOC_PHEP_MUA` | Hành chính |
| `NHAP` | Gửi (> CFG-M11-02) | `CHO_DONG_Y` | Hành chính |
| `CHO_DONG_Y` | Đồng ý | `DUOC_PHEP_MUA` | Người đại diện; Quản lý viện |
| `CHO_DONG_Y` (lần đầu) | Từ chối | `TU_CHOI` | Người đại diện; Quản lý viện |
| `NHAP`, `CHO_DONG_Y`, `DUOC_PHEP_MUA` | Hủy | `HUY` | Hành chính |
| `DUOC_PHEP_MUA` | Xin đồng ý bổ sung | `CHO_DONG_Y` (`dang_cho_bo_sung`) | Hành chính |
| `CHO_DONG_Y` (bổ sung) | Từ chối; Rút yêu cầu bổ sung | `DUOC_PHEP_MUA` (số cũ) | Người đại diện, Quản lý viện; Hành chính |
| `DUOC_PHEP_MUA` | Ghi đã mua | `DA_MUA` | Hành chính |

### `de_nghi_gui_nguoi_dai_dien` → gộp theo R-15: `de_nghi_gui_nguoi_dai_dien` → `su_kien_chung`

`de_nghi_id`, `lan_gui` (1 = đầu, 2… = bổ sung), `nguoi_than_id`. Danh sách người đại diện Hiệu lực lúc gửi (FR-023a). *(R-15: `lan_gui` → `su_kien_chung`)*

### `quyet_dinh_mua_ho` – nhóm 3 → gộp theo R-15: `quyet_dinh_mua_ho` → `su_kien_chung`

`de_nghi_id`, `loai` (`DONG_Y`, `TU_CHOI`, `RUT_BO_SUNG`, `XIN_BO_SUNG`), `so_cu`, `so_moi`, `nguoi_quyet_dinh_loai`, `nguoi_quyet_dinh_id`, `kenh` (`CONG_NGUOI_THAN`, `QUAN_LY_VIEN`, `HANH_CHINH`), `ly_do`, `so_du_khong_du` (dấu lúc quyết định, Q-219), `thoi_diem`. `so_cu`, `so_moi` dùng `DECIMAL(15,0)`.

---

## 7. Xuất kế toán (mục H)

### `lan_xuat_ke_toan` – nhóm 3 → gộp theo R-15: `lan_xuat_ke_toan` → hành động `nhat_ky`

`id`, `ky_id`, `pham_vi` (`TOAN_BO`, `CHUA_TUNG_XUAT`), `dinh_dang` (`XLSX`, `CSV`), `nguoi_xuat_id`, `thoi_diem`, `so_bang`, `so_khoan`, `tong_tien`, `danh_sach_chua_chot` `JSON`. `tong_tien` dùng `DECIMAL(15,0)`.

### `lan_xuat_bang` – nhóm 3 → gộp theo R-15: `lan_xuat_bang` → hành động `nhat_ky`

`lan_xuat_id`, `bang_id`, `lan_dau` (bool). Khóa chính hai cột đầu.

---

## 8. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

**Áp dụng audit 2026-10-29, F-07/F-09/F-13:** mọi cột tiền và đơn giá của 010 theo `DECIMAL(15,0)`, gồm tổng, số tiền gốc/điều chỉnh, số cũ/mới trong quyết định và ngưỡng tiền lưu tại lúc gửi; không đổi kiểu id hoặc JSON integer/int64. Bảng con khóa theo cha theo 000 data-model mục 1.4:

| Bảng con (tên logic) | Cha / trạng thái khóa | Bảo đảm khi cài migration |
| --- | --- | --- |
| `khoan_chi_phi` | `bang_chi_phi.trang_thai = DA_CHOT` | BEFORE UPDATE/DELETE chặn khi cha đã chốt, kể cả đổi khóa tham chiếu để đưa khoản ra khỏi bảng; giữ thêm khóa trạng thái cuối của chính khoản |
| `bang_chi_phi_tong_loai` | Tổng được ghi trong lệnh chốt bảng | Nhóm 3: không UPDATE/DELETE theo mẫu 000, độc lập với thời điểm sửa bảng cha |
| `de_nghi_gui_nguoi_dai_dien` | `de_nghi_mua_ho` ở TU_CHOI/HUY/DA_MUA | BEFORE UPDATE/DELETE đọc cha và chặn khi cha ở trạng thái cuối; không cho đổi khóa cha để né khóa |
| `lan_xuat_bang` | `lan_xuat_ke_toan` là nhóm 3 ngay khi ghi | Nhóm 3: không UPDATE/DELETE theo mẫu 000 |

Tên vật lý và phần đã gộp theo mục 0.1/glossary 16.7. Trigger của ba bảng riêng đã có test MySQL xanh ở T005a; các dòng bảng con đã chuyển sang bảng chung phải được kiểm qua API chủ A sau T108.

| Bất biến | Cách bảo đảm | Nguồn |
| --- | --- | --- |
| Mỗi bản ghi nguồn tối đa một khoản đang hiệu lực mỗi đơn vị (không kể điều chỉnh); khoản có đúng một nguồn hoặc nhập tay/mua hộ | `UNIQUE khoa_don_vi_hieu_luc`; `CHECK` cột nguồn theo `nguon_sinh` | DBR-15 |
| Đơn giá theo hợp đồng / phiên bản tại ngày phát sinh; khoản thuộc gói 0 đồng | Backend (P-05); `can_cu_don_gia` lưu căn cứ; 004 đánh dấu phiên bản đã tham chiếu | DBR-16 |
| Bảng đã chốt và khoản của nó không đổi; sai sót bằng điều chỉnh; tổng gốc + điều chỉnh ≥ 0 | Trigger `bang_chi_phi`, `khoan_chi_phi`; backend dưới khóa | DBR-17 |
| Mỗi người mỗi kỳ tối đa một bảng thường, một bảng bổ sung | `UNIQUE (ky_id, nct_id, loai_bang)` | FR-002, FR-002a |
| Kỳ là tháng dương lịch | `UNIQUE thang`; backend | FR-001 |
| Mọi thay đổi có nhật ký | `NhatKyApi` trong transaction | DBR-23 |

DBR-30 (mỗi bảng Đã chốt có đúng một giao dịch thanh toán) do 017 bảo đảm khi nghe `BangChiPhiDaChot`; 010 phát sự kiện đúng một lần mỗi bảng (khóa `bangId`).

---

## 9. Nhật ký và phân loại trường

- Loại đối tượng: `M11.KY_CHI_PHI`, `M11.BANG_CHI_PHI`, `M11.KHOAN_CHI_PHI`, `M11.DE_NGHI_MUA_HO`, `M11.LAN_XUAT_KE_TOAN`.
- `PhanLoaiTruongProvider`: số tiền, đơn giá, tổng → `TAI_CHINH`; tên thuốc hiển thị (lấy từ 006 lúc đọc) → `THUOC`; ghi chú, lý do → `TAI_CHINH`.
- Không đăng ký `DinhChinhDuocProvider` (khoản đã chốt sửa bằng điều chỉnh, 000 R-08).

---

## 10. Lưu giữ (000 R-11)

| Loại dữ liệu | Hạn | Chuỗi tham chiếu |
| --- | --- | --- |
| Bảng, khoản, lịch sử tính lại, biến động, đề nghị mua hộ, quyết định | `THEO_HO_SO` (CFG-M15-03) | Khoản điều chỉnh ↔ khoản gốc; giao dịch số dư của 017 trỏ bảng |
| Kỳ của viện, lần xuất | `KHONG_GAN_HO_SO` (CFG-M15-04) | Bảng trong lần xuất |
| Chỉ mục sự kiện tính phí | Theo nguồn; xóa khi nguồn bị loại bỏ | — |

---

## 11. Truy vết DBR, BR và FR

| Mã | Bảo đảm ở | Test |
| --- | --- | --- |
| DBR-15 | Mục 4, 8 | `DBR_15_MotKhoanMoiDonViIT`, `LapTrungDongThoiIT` |
| DBR-16 | P-05 | `DBR_16_DonGiaNgayPhatSinhIT`, `BR_M11_03_ThuocGoiIT` |
| DBR-17 | Mục 3, 4 | `DBR_17_BangDaChotIT`, `FR_026_TongKhongAmIT` |
| BR-M11-01 | P-04 | `BR_M11_01_LapTheoSuKienIT`, `FR_005_HangLoatIT` |
| BR-M11-02 | P-05 | `BR_M11_02_PhienBanDonGiaIT` |
| BR-M11-03 | P-05 | `BR_M11_03_ThuocGoiIT` |
| BR-M11-04 | P-06 | `BR_M11_04_NhapTayIT` |
| BR-M11-05 | P-06 | `BR_M11_05_TinhLaiIT`, `FR_027_BaoSauChotIT` |
| BR-M11-06 | P-07 | `BR_M11_06_ChotBangIT` |
| BR-M11-07 | P-08, P-09 | `BR_M11_07_BienDongTest`, `BR_M11_07_MuaHoIT`, `FR_024_VuotSoDongYIT` |
| BR-M11-08 | P-10 | `BR_M11_08_KyCuoiIT`, `DieuKienChiPhiKyCuoiTest` |
| BR-M11-09 | P-05 | `BR_M11_09_ChiaGiaThangTest` (SC-002), `PhiBuoiBanTruTest` |
| BR-M02-06 (phần 010) | Nội trú tính đủ giá, chặn từ ngày giải phóng giường theo Q-395; bán trú giữ tỷ lệ nguồn | ResidencyDayFeeTest/DayCareSessionFeeTest xanh phần số tiền; kiểm nguồn ngày còn chờ B/004 |
