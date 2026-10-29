# Data Model: Phòng, giường và phân bổ giường (003)

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Các cột chuẩn của nhóm 1 (`trang_thai` Hiệu lực/Ngừng hiệu lực, `phien_ban`) và nhóm 2 (`phien_ban`, `tao_luc`, `tao_boi`) không nhắc lại ở từng bảng trừ khi có điểm riêng.

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp: `phong` (mức chăm sóc cho phép thành `JSON`); `giuong`; `lich_su_trang_thai_giuong` (`CHECK` BR-M03-10); `phan_bo_giuong`. Tài sản của 019 (M03): `tai_san`, xem data-model 019 mục 0.
- `danh_muc_chung`: `khu_vuc`, `tang` (`cha_id` là khu vực), `loai_phong` (mức chăm sóc trong `thuoc_tinh`), `ly_do_chuyen_giuong`, `khu_nghi_ban_tru`, `buoi_ban_tru` (sức chứa trong `thuoc_tinh`).
- `lich_su_chung`: `lich_su_cach_ly_phong` (kiểu `KHOANG`).
- `trang_thai_nct`: `vi_tri_nct` (`resident_position`); `ho_so_chung`: `yeu_cau_nhap_bu`; `moc_da_nhac`: `nhac_phan_bo`; `khoa_ky_thuat`: `allocation_write_guard`, `day_care_capacity_guard`.
- Còn 4 bảng riêng của feature.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-29; tên mã theo bảng đã cài, migration `V003_95`)*

Mô tả dưới đây thay các mục tương ứng khi cài 003 T066; cột không nhắc lại giữ như bảng đã cài (`V003_01` → `V003_06`).

**`room`** (`phong`) – nhóm 1. Giữ mọi cột và `uq_room_code`. `floor_id` trỏ `danh_muc_chung.id` loại `M03.FLOOR`; `room_type_id` trỏ loại `M03.ROOM_TYPE` (id mới). Thêm `allowed_care_levels JSON NOT NULL` (thay `room_allowed_care_level`, mảng mã `M01.CARE_LEVEL`).

**`bed`** (`giuong`) – giữ nguyên mọi cột, `CHECK`, `uq_bed_room_code`, khóa ngoại.

**`bed_state_history`** (`lich_su_trang_thai_giuong`) – giữ nguyên (R-15: `CHECK` BR-M03-10).

**`bed_allocation`** (`phan_bo_giuong`) – giữ nguyên mọi cột, ba `UNIQUE` cột sinh, `CHECK`, trigger chống chồng thời gian. `transfer_reason_id` trỏ `danh_muc_chung.id` loại `M03.BED_TRANSFER_REASON` (id mới).

**View `v_current_allocation`** – viết lại: `JOIN catalog_item f ON f.id = r.floor_id`, `f.parent_id AS area_id` thay `JOIN floor`.

**Loại ở bảng chung**:

| Bảng chung | `loai` | Nội dung | Ràng buộc chuyển về backend |
| --- | --- | --- | --- |
| `danh_muc_chung` | `M03.AREA` | `ma` = mã khu vực | `UNIQUE` mã theo `ma_khoa` |
| `danh_muc_chung` | `M03.FLOOR` | `cha_id` = khu vực; `ma` = mã tầng; `{buildingName}` | Mã không trùng trong khu vực (`ma_khoa` gồm `cha_id`, thay `uq_floor_area_code`) |
| `danh_muc_chung` | `M03.ROOM_TYPE` | `{standardBeds, servesLongTerm, servesShortTerm, careLevels[]}` | `standardBeds > 0`, phục vụ ít nhất một loại lưu trú (thay hai `CHECK`) |
| `danh_muc_chung` | `M03.BED_TRANSFER_REASON` | `ma` = mã; `{medicalSafety, fixed}` | Lý do cố định không sửa, không ngừng |
| `danh_muc_chung` | `M03.DAY_CARE_REST_AREA` | `{floorId, areaId}` (đúng một trong hai) | Một khu nghỉ Hiệu lực (thay `active_singleton`): lệnh kích hoạt khóa `khoa_ky_thuat` `M03.KHU_NGHI` rồi kiểm |
| `danh_muc_chung` | `M03.DAY_CARE_SESSION` | `{startTime, endTime}` | `endTime > startTime` trong một ngày |
| `lich_su_chung` | thuộc tính `CACH_LY`, đối tượng `PHONG` | kiểu `MOC`; `gia_tri` `ISOLATE`, `LIFT`; `ly_do` bắt buộc; `can_cu_loai` `INCIDENT` khi có sự cố | Lý do không rỗng (thay `chk_isolation_reason`) |
| `ho_so_chung` | `M03.BED_BACKFILL_REQUEST` | `nct_id`; đối tượng `APPROVAL_REQUEST`; `{currentAllocationId, newBedId, transferAt}`; khóa khi thêm | Một dòng mỗi yêu cầu phê duyệt (`khoa_duy_nhat` `M03.BBR:<approvalRequestId>`); cột sinh `m03_new_bed_id`, `m03_transfer_at` có chỉ mục cho kiểm chồng thời gian |
| `trang_thai_nct` | module `M03` | `{withoutBedSince, waitingHoldBedIds[]}` | Thay `resident_position`; khóa dòng như cũ |
| `moc_da_nhac` | `M03.ALLOCATION_REMINDER.<flagType>` | đối tượng `target_type`, `target_id`; `moc` = `START` khi bắt đầu, số lần mỗi lần nhắc | Thay `allocation_reminder` (số lần, lần cuối tính từ các dòng; xóa cờ thành dòng `END`) |
| `khoa_ky_thuat` | `M03.PHAN_BO`, `M03.SUC_CHUA_BAN_TRU` | | Thay `allocation_write_guard`, `day_care_capacity_guard` |

**Chuyển dữ liệu (`V003_95`)**: `area`, `floor`, `room_type` (kèm `room_type_care_level`), `bed_transfer_reason`, `day_care_rest_area`, `day_care_session` vào `danh_muc_chung`, đổi `room.floor_id`, `room.room_type_id`, `bed_allocation.transfer_reason_id` và tham chiếu khu nghỉ, buổi bán trú ở module khác (đọc qua `LocationStructureApi`, không có khóa ngoại ngoài 003) theo id mới; `room_allowed_care_level` vào `room.allowed_care_levels`; `room_isolation_history` vào `lich_su_chung`; `bed_backfill_request` vào `ho_so_chung`; `resident_position` vào `trang_thai_nct`; `allocation_reminder` vào `moc_da_nhac`; hai bảng khóa vào `khoa_ky_thuat`. Viết lại `v_current_allocation`. `dinh_chinh` trỏ `M03.BED_ALLOCATION` giữ id.

**Bảo đảm** (thay phần tương ứng ở các mục sau): một phân bổ hiện hành mỗi giường, mỗi người và không chồng thời gian (giữ ở `bed_allocation`); BR-M03-10 (giữ `CHECK`); mã tầng không trùng trong khu vực (`ma_khoa`); một khu nghỉ bán trú Hiệu lực (kiểm dưới khóa). Test m03 hiện có phải xanh, giữ kịch bản nghiệp vụ và đổi bảng đích trong test (danh sách cuối mục), trừ `FR_035_LocationHistoryHttpIT` đang đỏ sẵn (team-plan mục 8).

**Bảng truy vết sau gộp** *(bổ sung 2026-10-30; khi làm task gộp, sửa cột "bảo đảm bằng" của bảng DBR, BR ở các mục sau theo bảng này)*:

| Mã | Bảo đảm sau gộp | Test |
| --- | --- | --- |
| DBR-23 | Lịch sử cách ly chỉ thêm: `lich_su_chung` (trigger không sửa, không xóa; `den` đặt một lần) | `RecordGroupTriggerIT`, test cách ly phòng hiện có |

**Test phải đổi bảng đích** *(rà 2026-10-30; 50 file đọc, ghi thẳng bảng cũ bằng SQL)*: giữ kịch bản nghiệp vụ, chỉ đổi bảng, cột đích.
- Fixture dùng chung (`support/`, ảnh hưởng mọi module; sửa trong cùng PR, báo cả nhóm ở team-plan mục 8): `FR_018_FakeM03Test`, `FakeLocations`.
- Test của module: `BR_M03_03_DayCareCapacityIT`, `BR_M03_21_ControlledTransferIT`, `BR_M03_22_BedCreationIT`, `BR_M03_25_DayCareParameterIT`, `DBR_09_AllocationSchemaIT`, `DBR_23_RecordTablesIT`, `DBR_26_BedSchemaIT`, `FR_001_StructureEntitiesTest`, `FR_001_StructureHttpIT`, `FR_001_StructureSchemaIT`, `FR_003_UniqueCodesIT`, `FR_005_DeactivateStructureIT`, `FR_008_RoomIsolationIT`, `FR_016_BedStateIT`, `FR_017_AllocationEntitiesIT`, `FR_017_AllocationQueriesIT`, `FR_017_StructureApiIT`, `FR_020_AllocationLocksIT`, `FR_022_AllocationHttpIT`, `FR_023_AllocationCommandsIT`, `FR_024_AllocationStatusParticipantIT`, `FR_027_BackfillSchemaIT`, `FR_028_ControlledTransferAtomicityIT`, `FR_035_LocationHistoryIT`, `FR_038_DayCareConcurrencyIT`, `FR_038_DayCareHttpIT`, `FR_038_DayCareSchemaIT`, `FR_041c_FloorAreaNameIT`, `FR_044_BedRemindersIT`, `FR_049_M03GovernanceIT`, `SC_008_BedReportingIT`, `StructureTestSupport`, `T064_BedListPerformanceIT`.
- Test của m09 (chủ module review): `AssignmentIT`, `AssignmentSourcesTest`, `NextHandoverShiftTest`, `PostPublicationNotificationsTest`, `PostPublicationPortsIT`, `PrimaryAssigneeResolverTest`, `ShiftPortFakesTest`, `ShiftQueriesIT`, `ShiftScheduleIT`, `StaffStatisticsIT`, `StaffingRatioIT`, `StaffingSourceContractsTest`.
- Test của m14 (chủ module review): `MetricLicenseStaffTest`.
- Test của m15 (chủ module review): `AuditLogReportExportIT`, `RecordGroupTriggerIT`.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Ghi chú |
| --- | --- | --- |
| `khu_vuc`, `tang`, `loai_phong`, `loai_phong_muc`, `phong`, `phong_muc_cho_phep` | 1 | `phong.dang_cach_ly` đổi qua lệnh (nhóm 2), mục 5 *(R-15: `loai_phong`, `loai_phong_muc` → `danh_muc_chung`)* |
| `ly_do_chuyen_giuong` | 1 | Danh mục lý do, có cờ "y tế/an toàn" (FR-030) *(R-15: `ly_do_chuyen_giuong` → `danh_muc_chung`)* |
| `giuong` | 1 cho mã, 2 cho trạng thái | Mục 3 |
| `lich_su_trang_thai_giuong` | 3, chỉ thêm | Gồm lần Xác nhận giường sẵn sàng (FR-044, FR-045) |
| `phan_bo_giuong` | 2; bất biến khi Đã đóng, Đã hủy | Mục 4 |
| `vi_tri_nct` | 2 (kỹ thuật) | Một dòng mỗi người cao tuổi: khóa tuần tự, dấu "chưa có giường" *(R-15: `vi_tri_nct` → `trang_thai_nct`)* |
| `lich_su_cach_ly_phong` | 3, chỉ thêm | Mục 5 *(R-15: `lich_su_cach_ly_phong` → `lich_su_chung`)* |
| `khu_nghi_ban_tru`, `buoi_ban_tru` | 1 | Mục 6 *(R-15: `khu_nghi_ban_tru`, `buoi_ban_tru` → `danh_muc_chung`)* |
| `yeu_cau_nhap_bu`, `nhac_phan_bo` | Kỹ thuật | Mục 7 *(R-15: `yeu_cau_nhap_bu` → `ho_so_chung`; `nhac_phan_bo` → `moc_da_nhac`)* |

Mức chăm sóc dùng mã `CO_BAN`, `THUONG_XUYEN`, `DAC_BIET` của 001 (`MaMucChamSoc`). Cột lưu `ENUM` cùng giá trị, không khóa ngoại sang bảng của M01 (constitution X). Người cao tuổi, tài khoản, sự cố, yêu cầu phê duyệt là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác.

---

## 2. Cơ cấu vị trí (mục A, FR-001 → FR-007)

### `khu_vuc` – nhóm 1 → gộp theo R-15: `khu_vuc` → `danh_muc_chung`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | Khóa chính | |
| `ma` | `VARCHAR(20)` | `UNIQUE` (duy nhất trong viện, kể cả mục đã Ngừng hiệu lực) | FR-003 |
| `ten` | `VARCHAR(100)` | | |

### `tang` – nhóm 1 → gộp theo R-15: `tang` → `danh_muc_chung`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `khu_vuc_id` | `BIGINT` | Khóa ngoại `khu_vuc` `ON DELETE RESTRICT` | FR-001 |
| `ma` | `VARCHAR(20)` | `UNIQUE (khu_vuc_id, ma)` | FR-003 |
| `ten` | `VARCHAR(100)` | | |
| `ten_toa_nha` | `VARCHAR(100) NULL` | | FR-001, Assumptions |

### `loai_phong` – nhóm 1; `loai_phong_muc` → gộp theo R-15: `loai_phong`, `loai_phong_muc` → `danh_muc_chung`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `ten` | `VARCHAR(100)` | `ten_hieu_luc` `UNIQUE` (000 mục 1.1) | |
| `so_giuong_tieu_chuan` | `SMALLINT` | `> 0`; chỉ để cảnh báo | FR-004, BR-M03-22 |
| `phuc_vu_dai_han`, `phuc_vu_ngan_ngay` | `BOOLEAN` | `CHECK (phuc_vu_dai_han OR phuc_vu_ngan_ngay)`; mặc định cả hai | FR-004, Assumptions |

`loai_phong_muc (loai_phong_id, ma_muc)`: khóa chính hai cột; danh sách mức mặc định.

### `phong` – nhóm 1 (thuộc tính), nhóm 2 (cách ly)

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `tang_id` | `BIGINT` | Khóa ngoại `tang`. Không đổi khi phòng đã có giường từng có phân bổ (backend; FR-007) | FR-001, FR-007 |
| `ma` | `VARCHAR(20)` | `UNIQUE` (duy nhất trong viện) | FR-003 |
| `ten` | `VARCHAR(100)` | | |
| `loai_phong_id` | `BIGINT` | Khóa ngoại. Tạo mới hoặc đổi loại chỉ chọn loại Hiệu lực; loại Ngừng hiệu lực vẫn giữ ở phòng cũ (US1 #8) | FR-004 |
| `chinh_sach_gioi_tinh` | `ENUM('NAM','NU','KHONG_GIOI_HAN')` | | FR-004 |
| `dang_cach_ly` | `BOOLEAN` | Mặc định `false`; chỉ đổi qua lệnh ở mục 5 | FR-008 |

`phong_muc_cho_phep (phong_id, ma_muc)`: khi tạo phòng sao từ `loai_phong_muc`; Quản lý viện thu hẹp hoặc mở rộng sau (FR-004). *(R-15: `loai_phong_muc` → `danh_muc_chung`)*

**Lệnh và kiểm tra ở backend**:
- **Sửa chính sách giới tính, danh sách mức** (FR-006): khóa các dòng `giuong` của phòng, chạy kiểm (a), (c) cho mọi phân bổ `DANG_HIEU_LUC`, `TUONG_LAI` của phòng. Vi phạm thì 409 liệt kê người cao tuổi.
- **Ngừng hiệu lực** (FR-005):
  - phòng: chặn khi còn giường không ở `KHONG_SU_DUNG`, kể cả `TAM_NGUNG` (Q-260);
  - tầng, khu vực: chặn khi còn con chưa Ngừng hiệu lực;
  - phòng, tầng, khu vực: chặn khi `ThamChieuViTri.dangThamChieu(viTri)` (008) trả phân công còn trỏ vào.

  Khi chặn, 409 liệt kê đối tượng gây chặn.
- **Xóa**: chỉ khu vực, tầng, phòng, loại phòng chưa từng được tham chiếu. Khóa ngoại `RESTRICT` trong 003, cộng `ThamChieuViTri.daTungThamChieu` cho tham chiếu từ module khác. Giường không có lệnh xóa: giường tạo nhầm thì Thanh lý tài sản (Q-237).

### `ly_do_chuyen_giuong` – nhóm 1 → gộp theo R-15: `ly_do_chuyen_giuong` → `danh_muc_chung`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `ma` | `VARCHAR(30)` | `UNIQUE` | |
| `ten` | `VARCHAR(100)` | `ten_hieu_luc` `UNIQUE` | |
| `y_te_an_toan` | `BOOLEAN` | | FR-030 |
| `co_dinh` | `BOOLEAN` | Dòng `KIEM_SOAT_LAY_NHIEM` nạp sẵn, `y_te_an_toan = true`, `co_dinh = true`. Dòng cố định không sửa cờ, không Ngừng hiệu lực | FR-010, FR-030 |

---

## 3. Giường và trạng thái (mục C, FR-011 → FR-016a)

### `giuong` – nhóm 1 (mã), nhóm 2 (trạng thái)

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `phong_id` | `BIGINT` | Khóa ngoại `phong`. Đổi được chỉ khi giường chưa từng có phân bổ (FR-007, Q-243) | FR-001 |
| `ma` | `VARCHAR(20)` | `UNIQUE (phong_id, ma)` | FR-003 |
| `trang_thai` | `ENUM('TRONG','DANG_SU_DUNG','DANG_GIU_CHO','CHO_SAN_SANG','DANG_BAO_TRI','TAM_NGUNG','KHONG_SU_DUNG')` | Chỉ `GiuongTrangThaiService` ghi (research G-04) | FR-011 |
| `ly_do_giu_cho` | `ENUM('NGUOI_VANG','HO_SO_CHO') NULL` | | FR-012 |
| `giu_cho_nct_id` | `BIGINT NULL` | Người được giữ cho; với hồ sơ chờ là người Đang tiếp nhận của hồ sơ | FR-012, DBR-10 |
| `giu_cho_ho_so_cho_id` | `BIGINT NULL` | | FR-012 |
| `han_giu_cho` | `DATETIME(6) NULL` | `NULL` với người vắng (giữ không hạn, Q-395) | FR-012 |
| `da_bao_het_han_luc` | `DATETIME(6) NULL` | Chống báo lặp giữ người vắng hết hạn (G-09) | FR-015 |
| `cho_san_sang_tu`, `han_xac_nhan_san_sang` | `DATETIME(6) NULL` | `han = tu + CFG-M03-04` đọc lúc vào trạng thái | FR-011a, FR-044 |
| `phan_bo_vua_dong_id` | `BIGINT NULL` | Khóa ngoại `phan_bo_giuong`; phân bổ mà lần đóng đưa giường vào `CHO_SAN_SANG`. `NULL` khi vào từ Sử dụng lại | FR-045, DBR-26 |
| `da_nhac_san_sang_luc` | `DATETIME(6) NULL` | Chống nhắc lặp (G-09) | FR-044 |
| `ly_do_tam_ngung` | `VARCHAR(2000) NULL` | | Q-246 |

**`CHECK`**:
- `(trang_thai = 'DANG_GIU_CHO') = (ly_do_giu_cho IS NOT NULL)`
- `ly_do_giu_cho <> 'NGUOI_VANG' OR giu_cho_nct_id IS NOT NULL`
- `ly_do_giu_cho <> 'HO_SO_CHO' OR (giu_cho_ho_so_cho_id IS NOT NULL AND han_giu_cho IS NOT NULL)`
- `(trang_thai = 'CHO_SAN_SANG') = (han_xac_nhan_san_sang IS NOT NULL)`
- `(trang_thai = 'TAM_NGUNG') = (ly_do_tam_ngung IS NOT NULL)`

**Tạo giường** (Q-238): `POST /giuong` thêm dòng `TRONG`, một dòng lịch sử (`den = TRONG`, sự kiện `TAO_GIUONG`), và gọi `TaiSanGiuongPort.ghiTangChoGiuong(giuongId)` của 019 trong cùng transaction (module-interfaces mục 3). Số giường của phòng (không tính `KHONG_SU_DUNG`) vượt `so_giuong_tieu_chuan` thì phản hồi 201 kèm cảnh báo `VUOT_SO_GIUONG_TIEU_CHUAN` (BR-M03-22).

### Bảng chuyển trạng thái giường

Khớp bảng trạng thái giường của spec (mục C). Cột "Gọi từ" cho biết lệnh hoặc giao diện kích hoạt.

| Từ | Sự kiện (`su_kien`) | Đến | Gọi từ | Điều kiện chặn | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | `TAO_GIUONG` | `TRONG` | `POST /giuong` (AC-01) | Phòng Hiệu lực; mã duy nhất | Ghi tăng tài sản (019) |
| `TRONG` | `PHAN_BO_BAT_DAU` | `DANG_SU_DUNG` | Chuyển giường; Hoàn tất tiếp nhận (G-06); chuyển theo lịch (G-07); phân bổ cho người chưa có giường (FR-025a) | FR-018 | — |
| `TRONG` | `GIU_CHO_HO_SO_CHO` | `DANG_GIU_CHO` | `PhanBoGiuongLenhApi.giuChoHoSoCho` (004) | Không có phân bổ tương lai chồng khoảng giữ | Hạn = thời điểm + CFG-M02-02 |
| `DANG_GIU_CHO` (hồ sơ chờ) | `PHAN_BO_BAT_DAU` | `DANG_SU_DUNG` | Hoàn tất tiếp nhận của chính người được giữ | FR-018 | Xóa thuộc tính giữ chỗ |
| `DANG_GIU_CHO` (hồ sơ chờ) | `HET_HAN_GIU_CHO`, `HUY_GIU_CHO` | `TRONG` | Job `HetHanGiuCho`; `huyGiuCho` (004); bean Hủy tiếp nhận | — | `GiuongDaTrong` (FR-014) |
| `DANG_SU_DUNG` | `VANG_GIU_GIUONG` | `DANG_GIU_CHO` (người vắng) | Bean Cho tạm vắng, Chuyển viện | `giuGiuongKhiVang` trả giữ | Hạn theo chính sách; phân bổ giữ nguyên |
| `DANG_GIU_CHO` (người vắng) | `TRO_VE` | `DANG_SU_DUNG` | Bean Ghi nhận trở về | — | FR-024a nếu phòng chịu chặn |
| `DANG_GIU_CHO` (người vắng) | `GIAI_PHONG_KHI_VANG`, `KET_THUC_LUU_TRU`, `QUA_DOI` | `CHO_SAN_SANG` | `giaiPhongGiuongKhiVang` (004); bean | — | Đóng phân bổ (FR-025) |
| `DANG_GIU_CHO` (người vắng) | `CHUYEN_DI` | `CHO_SAN_SANG` | Chuyển giường | FR-018 cho giường mới | Giường mới `DANG_GIU_CHO` cùng lý do, hạn còn lại (FR-032) |
| `DANG_SU_DUNG` | `CHUYEN_DI`, `KET_THUC_LUU_TRU`, `QUA_DOI`, `VANG_KHONG_GIU_GIUONG` | `CHO_SAN_SANG` | Chuyển giường; bean | — | Đóng phân bổ; `han_xac_nhan_san_sang` |
| `CHO_SAN_SANG` | `XAC_NHAN_SAN_SANG` | `TRONG`, hoặc `DANG_SU_DUNG` khi có chuyển theo lịch đang chờ | `POST /giuong/{id}/xac-nhan-san-sang` | Người thực hiện theo G-14; ghi khử khuẩn, bảo hộ khi FR-045 áp dụng | `GiuongDaTrong` nếu không có phân bổ tương lai; hoặc thực hiện chuyển theo lịch đang chờ (FR-011a) |
| `TRONG`, `CHO_SAN_SANG`, `TAM_NGUNG` | `TAI_SAN_BAO_TRI` | `DANG_BAO_TRI` | `GiuongTheoTaiSanPort` (019) | FR-013, ngoại lệ FR-013a | Hủy phân bổ tương lai khi ngoại lệ áp dụng |
| `TRONG`, `CHO_SAN_SANG`, `DANG_BAO_TRI`, `TAM_NGUNG` | `TAI_SAN_THANH_LY` | `KHONG_SU_DUNG` | `GiuongTheoTaiSanPort` (019) | FR-013a cho phân bổ tương lai | Trạng thái cuối |
| `DANG_BAO_TRI` | `TAI_SAN_SAN_SANG` | `TRONG`, hoặc `TAM_NGUNG` nếu lần vào bảo trì gần nhất đi từ `TAM_NGUNG` | `GiuongTheoTaiSanPort` (019) | Phòng Hiệu lực | `GiuongDaTrong` khi về `TRONG` |
| `TRONG` | `TAM_NGUNG` | `TAM_NGUNG` | `POST /giuong/{id}/tam-ngung-su-dung` (AC-01) | Lý do; không phân bổ tương lai; không mang dấu "chờ chuyển người" | — |
| `TAM_NGUNG` | `SU_DUNG_LAI` | `CHO_SAN_SANG` | `POST /giuong/{id}/su-dung-lai` (AC-01) | Phòng Hiệu lực | `phan_bo_vua_dong_id = NULL` (Q-261) |
| `DANG_SU_DUNG`, `DANG_GIU_CHO` | `TAI_SAN_*` | — (chặn) | 019 | BR-M03-05; Báo hỏng thì 019 ghi dấu "chờ chuyển người", giường giữ nguyên | — |

### `lich_su_trang_thai_giuong` – nhóm 3, chỉ thêm

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `giuong_id` | `BIGINT` | Khóa ngoại | FR-016 |
| `tu`, `den` | `ENUM` như `giuong.trang_thai`; `tu` `NULL` ở dòng tạo giường | | FR-016 |
| `su_kien` | `VARCHAR(30)` | Mã ở bảng chuyển | FR-016 |
| `thoi_diem` | `DATETIME(6)` | Thời điểm thực tế (Clock) | FR-016a |
| `thoi_diem_du_kien` | `DATETIME(6) NULL` | Có với việc của Bộ lập lịch | FR-016a |
| `thuc_hien_tre` | `BOOLEAN` | `thoi_diem − thoi_diem_du_kien > CFG-M03-08` | FR-016a |
| `nguoi_thuc_hien_id` | `BIGINT NULL` | `NULL` khi là Bộ lập lịch (nhật ký ghi `laBoLapLich`) | FR-016 |
| `ly_do` | `VARCHAR(2000) NULL` | | FR-016 |
| `can_cu_loai`, `can_cu_id` | `VARCHAR(40)`, `BIGINT` | `PHAN_BO`, `HO_SO_CHO`, `LUOT_VANG`, `YEU_CAU`, `LENH_TAI_SAN`, `LENH_TRANG_THAI_NCT` | FR-016 |
| `da_khu_khuan`, `da_dung_bao_ho` | `BOOLEAN NULL` | Chỉ dòng `XAC_NHAN_SAN_SANG` | FR-045, BR-M03-10 |
| `bat_buoc_khu_khuan` | `BOOLEAN NULL` | Kết quả `thuocDanhSach` tại thời điểm xác nhận; lưu để truy vết SC-010 | FR-045 |
| `ghi_chu` | `VARCHAR(2000) NULL` | | FR-044 |
| `tang_khu_vuc_id` | `BIGINT` | Tầng của giường; dùng cho đính chính cách (b) của 000 | 000 FR-026 |

`CHECK (su_kien <> 'XAC_NHAN_SAN_SANG' OR bat_buoc_khu_khuan = false OR (da_khu_khuan AND da_dung_bao_ho))` bảo đảm BR-M03-10 ở cơ sở dữ liệu.

---

## 4. Phân bổ giường (mục D, E, FR-017 → FR-034)

### `phan_bo_giuong` – nhóm 2; bất biến khi `DA_DONG`, `DA_HUY`

| Cột | Kiểu | Ràng buộc, ghi chú | Nguồn |
| --- | --- | --- | --- |
| `nct_id` | `BIGINT` | | FR-017 |
| `giuong_id` | `BIGINT` | Khóa ngoại `giuong` | FR-017 |
| `trang_thai` | `ENUM('TUONG_LAI','DANG_HIEU_LUC','DA_DONG','DA_HUY')` | | FR-017 |
| `bat_dau` | `DATETIME(6)` | Với `TUONG_LAI` là thời điểm bắt đầu dự kiến; khi bắt đầu được ghi lại bằng thời điểm thực tế | FR-017, FR-023 |
| `bat_dau_du_kien` | `DATETIME(6) NULL` | Giữ mốc dự kiến ban đầu sau khi bắt đầu | FR-023 |
| `ket_thuc` | `DATETIME(6) NULL` | | FR-017 |
| `ket_thuc_du_kien` | `DATETIME(6) NULL` | Nội trú ngắn ngày, từ hợp đồng (004) | FR-017 |
| `ket_thuc_hieu_dung` | Cột sinh `COALESCE(ket_thuc, ket_thuc_du_kien, '9999-12-31')` | Dùng xét chồng (research G-03) | FR-019 |
| `ly_do_bat_dau`, `ly_do_ket_thuc`, `ly_do_huy` | `VARCHAR(2000)` | `ly_do_ket_thuc` kèm mã: `CHUYEN_GIUONG`, `KET_THUC_LUU_TRU`, `QUA_DOI`, `GIAI_PHONG_KHI_VANG`, `VANG_KHONG_GIU_GIUONG` | FR-017 |
| `ly_do_chuyen_id` | `BIGINT NULL` | Khóa ngoại `ly_do_chuyen_giuong`; với phân bổ mở bằng chuyển giường | FR-030 *(R-15: `ly_do_chuyen_giuong` → `danh_muc_chung`)* |
| `nguoi_mo_id`, `nguoi_dong_id` | `BIGINT NULL` | `NULL` khi là Bộ lập lịch | FR-017 |
| `can_cu_loai`, `can_cu_id` | `VARCHAR(40)`, `BIGINT NULL` | `LENH`, `YEU_CAU_THAY_DOI_LUU_TRU`, `YEU_CAU_NHAP_BU`, `LENH_TRANG_THAI_NCT` | FR-017 |
| `phan_bo_truoc_id` | `BIGINT NULL` | Khóa ngoại tự trỏ; phân bổ mở bằng chuyển giường | FR-017, FR-028 |
| `muc_da_kiem_tra`, `nguon_muc` | `ENUM` mức; `ENUM('HIEN_HANH','HOP_DONG','LUOT_DANG_KY')` | | FR-018 (a), Q-301 |
| `bac_si_chi_dinh_id` | `BIGINT NULL` | Chỉ phân bổ mở bằng chuyển kiểm soát lây nhiễm | FR-010, BR-M03-21 |
| `nhap_bu`, `thoi_diem_nhap_thuc` | `BOOLEAN`, `DATETIME(6) NULL` | | FR-027 (e) |
| `thoi_diem_du_kien`, `thuc_hien_tre` | `DATETIME(6) NULL`, `BOOLEAN` | Phân bổ bắt đầu hoặc đóng bởi Bộ lập lịch | FR-016a |
| `cho_giuong_san_sang` | `BOOLEAN` | Chuyển theo lịch đã tới ngày, chờ giường về Trống | FR-011a |
| `dau_phong_khong_phu_hop` | `BOOLEAN` | | FR-034, BR-M03-23 |
| `dau_cho_lap_yeu_cau` | `BOOLEAN` | Sau chuyển gấp khác giá, chưa có yêu cầu | FR-030 |
| `dau_cho_ve_cung_gia` | `BOOLEAN` | Yêu cầu sau chuyển gấp bị từ chối | FR-030 |
| `dau_cho_bs_xac_nhan` | `BOOLEAN` | | FR-024a |
| `bs_xac_nhan_id`, `bs_xac_nhan_luc`, `bs_xac_nhan_ly_do` | | | FR-024a (a) |
| `da_bao_qua_bat_dau_cho` | `DATETIME(6) NULL` | Mốc `bat_dau` đã báo quá hạn (G-09) | FR-023 |
| `giuong_dang_hieu_luc` | Cột sinh `IF(trang_thai='DANG_HIEU_LUC', giuong_id, NULL)` | `UNIQUE` | DBR-09 |
| `nct_dang_hieu_luc` | Cột sinh `IF(trang_thai='DANG_HIEU_LUC', nct_id, NULL)` | `UNIQUE` | DBR-09 |
| `nct_tuong_lai` | Cột sinh `IF(trang_thai='TUONG_LAI', nct_id, NULL)` | `UNIQUE` | DBR-09, FR-019 |

**`CHECK`**:
- `(trang_thai = 'DA_DONG') = (ket_thuc IS NOT NULL)`
- `ket_thuc IS NULL OR ket_thuc > bat_dau`
- `ket_thuc_du_kien IS NULL OR ket_thuc_du_kien > bat_dau`
- `bac_si_chi_dinh_id IS NULL OR ly_do_chuyen_id IS NOT NULL`
- `(trang_thai = 'DA_HUY') = (ly_do_huy IS NOT NULL)`

**Chỉ mục**: `(giuong_id, bat_dau)`, `(nct_id, bat_dau)`, `(trang_thai, bat_dau)` cho job.

**Trigger**:
- `trg_phan_bo_giuong_khong_chong` (`BEFORE INSERT`, `BEFORE UPDATE`): `SIGNAL` nếu tồn tại dòng khác cùng `giuong_id`, `trang_thai <> 'DA_HUY'`, `bat_dau < NEW.ket_thuc_hieu_dung AND NEW.bat_dau < ket_thuc_hieu_dung` (DBR-09). Lưới an toàn; khóa ở research G-03 là cơ chế chính.
- `trg_phan_bo_giuong_khong_sua` (`BEFORE UPDATE`): `SIGNAL` khi `OLD.trang_thai IN ('DA_DONG','DA_HUY')` (FR-026, BR-M03-07).
- `trg_phan_bo_giuong_khong_xoa` (`BEFORE DELETE`): theo mẫu 000 mục 1.4.

Cột dấu trên phân bổ `DANG_HIEU_LUC`, `TUONG_LAI` đổi được (nhóm 2). Mỗi lần đặt hoặc gỡ dấu ghi nhật ký `M03_PB_DAU` kèm loại dấu, lý do, người thực hiện hoặc Bộ lập lịch.

### Bảng chuyển trạng thái phân bổ

| Từ | Lệnh / sự kiện | Đến | Gọi từ | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Phân bổ giường (người Đang tiếp nhận) | `TUONG_LAI` | `POST /phan-bo-giuong` | FR-018, FR-019; FR-011a nếu giường `CHO_SAN_SANG`; người thực hiện theo FR-021 | Giường khóa cho khoảng đó; không phát `GiuongDaTrong`; kết thúc giữ chỗ hồ sơ chờ của người này ở giường khác (004 FR-016) |
| — | Đặt trước chuyển theo lịch | `TUONG_LAI` | `datTruocChuyenTheoLich` (004, lúc duyệt) | FR-018, FR-019 | `can_cu` là yêu cầu |
| — | Phân bổ giường (người chưa có giường, FR-025a); Chuyển giường; Hoàn tất tiếp nhận có đổi giường | `DANG_HIEU_LUC` | `POST /phan-bo-giuong`, `…/chuyen-giuong*`, bean | FR-018, FR-019; FR-030 khi khác đơn giá | Giường `DANG_SU_DUNG` (hoặc `DANG_GIU_CHO` với người vắng, FR-032); `DaChuyenGiuong` |
| `TUONG_LAI` | Hoàn tất tiếp nhận; chuyển theo lịch tới ngày và giường `TRONG`; giường về `TRONG` khi `cho_giuong_san_sang` | `DANG_HIEU_LUC` | Bean (G-06); `thucHienChuyenTheoLich`; `XAC_NHAN_SAN_SANG`, gỡ dấu (019) | FR-018 tại thời điểm thực tế; giường `TRONG` hoặc giữ cho chính người | `bat_dau` = thời điểm thực tế; với chuyển: đóng phân bổ cũ |
| `TUONG_LAI` | Hủy phân bổ tương lai; Hủy tiếp nhận; Kết thúc lưu trú, qua đời; đổi giường khi tiếp nhận; giường hỏng (FR-013a); chuyển theo lịch áp dụng không thành | `DA_HUY` | `POST …/huy`; bean; `GiuongTheoTaiSanPort`; `huyPhanBoTheoLich` | Có lý do | Giường hết bị khóa; bản ghi giữ lại |
| `DANG_HIEU_LUC` | Chuyển giường; Kết thúc lưu trú; Ghi nhận qua đời; Giải phóng khi vắng | `DA_DONG` | Lệnh tương ứng | — | Giường `CHO_SAN_SANG`; `phan_bo_vua_dong_id` |

`DANG_HIEU_LUC` không đổi khi người cao tuổi vắng có giữ giường (FR-024). Người Đang tiếp nhận không bao giờ có phân bổ `DANG_HIEU_LUC` (FR-023): bean `HOAN_TAT_TIEP_NHAN` là đường duy nhất chuyển phân bổ ban đầu sang `DANG_HIEU_LUC`.

### `vi_tri_nct` – nhóm 2 (kỹ thuật) → gộp theo R-15: `vi_tri_nct` → `trang_thai_nct`

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `nct_id` | `BIGINT` | Khóa chính | |
| `chua_co_giuong_tu` | `DATETIME(6) NULL` | Dấu "chưa có giường" | FR-025a, BR-M03-19 |
| `phien_ban` | `INT` | | |

Dòng là đích khóa tuần tự cho mọi lệnh trên cùng người (research G-03). Vị trí hiện tại vẫn suy ra từ phân bổ `DANG_HIEU_LUC` (FR-017), không lưu ở đây.

### View `v_phan_bo_hien_hanh`

Các cột của `phan_bo_giuong` với `giuong_id`, `bat_dau`, `ket_thuc` thay bằng giá trị sau đính chính từ `v_dinh_chinh_moi_nhat` của 000 (loại `M03.PHAN_BO_GIUONG`). Thêm `phong_id`, `tang_id`, `khu_vuc_id` qua nối cơ cấu, cùng `da_dinh_chinh`. Dòng có bản `HUY_GHI_NHAN` bị loại (000 `v_ban_ghi_da_huy`). Dùng cho FR-035, FR-036, 007, 016 (research G-10).

---

## 5. Cách ly phòng (mục B, FR-008 → FR-010)

### Bảng chuyển

| Từ | Lệnh | Đến | Ai | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| Không cách ly | `POST /phong/{id}/dat-cach-ly` | Đang cách ly | AC-01, AC-03 | Lý do (FR-008) | Phòng chịu chặn (FR-009, FR-010); `PhongDaDatCachLy` |
| Đang cách ly | `POST /phong/{id}/go-cach-ly` | Không cách ly | AC-01, AC-03 | Lý do | Gỡ dấu "chờ bác sĩ xác nhận vùng cách ly" nếu phòng không còn thuộc vùng khoanh vùng (FR-024a (c)); `PhongDaGoCachLy` |

### `lich_su_cach_ly_phong` – nhóm 3, chỉ thêm → gộp theo R-15: `lich_su_cach_ly_phong` → `lich_su_chung`

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| `phong_id` | `BIGINT` | Khóa ngoại |
| `hanh_dong` | `ENUM('DAT','GO')` | |
| `thoi_diem`, `nguoi_thuc_hien_id`, `ly_do` | | Lý do bắt buộc |
| `su_co_id` | `BIGINT NULL` | Tham chiếu logic tới sự cố của 007 |

---

## 6. Khu nghỉ bán trú (mục G, FR-038 → FR-042)

### `khu_nghi_ban_tru` – nhóm 1, duy nhất → gộp theo R-15: `khu_nghi_ban_tru` → `danh_muc_chung`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `ma`, `ten` | | | FR-038 |
| `tang_id`, `khu_vuc_id` | `BIGINT NULL` | `CHECK ((tang_id IS NULL) <> (khu_vuc_id IS NULL))`; sửa được để dời khu nghỉ | FR-038 |
| `hieu_luc_duy_nhat` | Cột sinh `IF(trang_thai='HIEU_LUC', 1, NULL)` | `UNIQUE` | FR-038 |

Ngừng hiệu lực bị chặn khi `DemLichDenBanTru.coLichTuongLai()` (004) trả có (Edge Case). Sức chứa không lưu; luôn là CFG-M03-01.

### `buoi_ban_tru` – nhóm 1 → gộp theo R-15: `buoi_ban_tru` → `danh_muc_chung`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `ten` | `VARCHAR(50)` | `ten_hieu_luc` `UNIQUE` | FR-038 |
| `gio_bat_dau`, `gio_ket_thuc` | `TIME` | `CHECK (gio_ket_thuc > gio_bat_dau)`; không chồng khung với buổi Hiệu lực khác (backend, dưới khóa dòng khu nghỉ) | FR-038, Q-47 |

Sửa khung giờ hoặc thêm, ngừng buổi kích hoạt đếm lại (research G-11, BR-M03-25).

---

## 7. Bảng kỹ thuật

### `yeu_cau_nhap_bu` → gộp theo R-15: `yeu_cau_nhap_bu` → `ho_so_chung`

| Cột | Ghi chú |
| --- | --- |
| `yeu_cau_phe_duyet_id` | Khóa chính; yêu cầu `M03.NHAP_BU_PHAN_BO` của 000 |
| `nct_id`, `phan_bo_hien_tai_id`, `giuong_moi_id`, `thoi_diem_chuyen` | Để gắn dấu "có nhập bù đang chờ duyệt" (FR-027, CHK051) mà không đọc JSON của 000 |

Dòng được thêm cùng lúc lập yêu cầu. Dấu chỉ hiện khi `YeuCauPheDuyetApi.xem` trả trạng thái chưa kết thúc. Bảng không được `UPDATE`.

### `nhac_phan_bo` → gộp theo R-15: `nhac_phan_bo` → `moc_da_nhac`

| Cột | Ghi chú |
| --- | --- |
| `loai_doi_tuong`, `doi_tuong_id`, `loai_dau` | Khóa chính. `loai_dau`: `CHUA_CO_GIUONG`, `CHO_LAP_YEU_CAU`, `CHO_VE_CUNG_GIA`, `PHONG_KHONG_PHU_HOP` |
| `bat_dau_luc`, `lan_nhac_cuoi`, `so_lan` | Mốc kế tiếp = `lan_nhac_cuoi` (hoặc `bat_dau_luc`) + CFG-M03-02 đọc lúc chạy |

Dòng được thêm khi đặt dấu và xóa khi gỡ dấu, trong cùng transaction với lệnh đặt hoặc gỡ dấu (SC-013). Mỗi lần nhắc ghi nhật ký `M03_NHAC` (Bộ lập lịch, căn cứ CFG-M03-02).

---

## 8. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bảo đảm | Cách làm | DBR / FR |
| --- | --- | --- |
| Mỗi giường tối đa một phân bổ đang hiệu lực | `UNIQUE(giuong_dang_hieu_luc)` | DBR-09 |
| Mỗi người tối đa một đang hiệu lực, một tương lai | `UNIQUE(nct_dang_hieu_luc)`, `UNIQUE(nct_tuong_lai)` | DBR-09, FR-019 |
| Khoảng của cùng giường không chồng | Khóa `giuong` (backend) và trigger `trg_phan_bo_giuong_khong_chong` | DBR-09, FR-020 |
| Phân bổ đã đóng, đã hủy bất biến | Trigger `trg_phan_bo_giuong_khong_sua` | FR-026, BR-M03-07 |
| Nhóm 3 chỉ thêm | Trigger `_khong_sua`, `_khong_xoa` trên `lich_su_trang_thai_giuong`, `lich_su_cach_ly_phong` | DBR-23 *(R-15: `lich_su_cach_ly_phong` → `lich_su_chung`)* |
| Thuộc tính giường khớp trạng thái | `CHECK` mục 3 | FR-012, DBR-26 |
| Xác nhận sẵn sàng có khử khuẩn khi bắt buộc | `CHECK` trên `lich_su_trang_thai_giuong` | BR-M03-10 |
| Một khu nghỉ bán trú Hiệu lực | `UNIQUE(hieu_luc_duy_nhat)` | FR-038 |
| Mã duy nhất | `UNIQUE` ở mục 2, 3 | FR-003 |
| Điều kiện phân bổ (mức, loại hình, giới tính, cách ly, trạng thái giường, giữ chỗ, dấu) | Backend, `KiemTraPhanBo`, sau khi khóa | DBR-10, BR-M03-01 |
| Chờ sẵn sàng chỉ về Trống qua lệnh, không có phân bổ đang hiệu lực | Bảng chuyển (backend); `UNIQUE(giuong_dang_hieu_luc)` cùng việc đóng phân bổ trước khi đổi giường | DBR-26 |

---

## 9. Nhật ký và phân loại trường

| Hành động | Đối tượng | `truoc` / `sau` | `chi_tiet` |
| --- | --- | --- | --- |
| `M03_CO_CAU_TAO`, `_SUA`, `_NGUNG`, `_KICH_HOAT`, `_XOA` | Khu vực, tầng, loại phòng, phòng, giường, lý do chuyển, khu nghỉ, buổi | Trường đổi | Cảnh báo `VUOT_SO_GIUONG_TIEU_CHUAN` |
| `M03_GIUONG_CHUYEN` | Giường | `trangThai` | `suKien`, `canCu`, `thoiDiemDuKien`, `thucHienTre` |
| `M03_PB_PHAN_BO`, `M03_PB_CHUYEN`, `M03_PB_DONG`, `M03_PB_HUY` | Phân bổ, người cao tuổi | Vị trí cũ, vị trí mới (khu – tầng – phòng – giường), thời điểm | `nhapBu`, `bacSiChiDinhId`, `lyDoChuyen`, `yTeAnToan`, `nguonMuc` |
| `M03_PB_DAU` | Phân bổ, `vi_tri_nct` | Dấu trước, sau | Loại dấu, căn cứ *(R-15: `vi_tri_nct` → `trang_thai_nct`)* |
| `M03_CACH_LY` | Phòng | `dangCachLy` | `suCoId` |
| `M03_BAN_TRU_CANH_BAO_VUOT` | Khu nghỉ | — | Danh sách buổi vượt |
| `M03_NHAC`, `M03_BAO` | Theo việc | — | Mốc dự kiến, `thucHienTre`, căn cứ CFG |

`PhanLoaiTruongProvider` theo research G-15: `lyDo` của `M03_CACH_LY` và của chuyển giường kiểm soát lây nhiễm thuộc `SUC_KHOE`; các trường khác `CONG_KHAI_NOI_BO`.

---

## 10. Lưu giữ (000 R-11)

| Loại | `LoaiHan` | Mốc | Tham chiếu tới |
| --- | --- | --- | --- |
| `M03.PHAN_BO_GIUONG` | `THEO_HO_SO` | Thời điểm trạng thái cuối của người cao tuổi | `phan_bo_truoc_id`, `giuong.phan_bo_vua_dong_id`, lịch sử giường có căn cứ là phân bổ, đính chính |
| `M03.LICH_SU_TRANG_THAI_GIUONG` | `KHONG_GAN_HO_SO` | `thoi_diem` | Đính chính `M03.XAC_NHAN_SAN_SANG` |
| `M03.LICH_SU_CACH_LY_PHONG` | `KHONG_GAN_HO_SO` | `thoi_diem` | — |

Bảng kỹ thuật `yeu_cau_nhap_bu`, `nhac_phan_bo` bị xóa cùng chuỗi của đối tượng chúng trỏ tới. *(R-15: `yeu_cau_nhap_bu` → `ho_so_chung`; `nhac_phan_bo` → `moc_da_nhac`)*

---

## 11. Truy vết DBR, BR và FR

| Mã | Bảo đảm (mục) | Test (quickstart mục 4) |
| --- | --- | --- |
| DBR-09 | Mục 4, mục 8; research G-03 | `DBR_09_KhongChongIT`, `FR_020_PhanBoDongThoiIT` |
| DBR-10 | `KiemTraPhanBo`; mục 3 `CHECK` giữ chỗ | `BR_M03_01_DieuKienPhanBoIT` |
| DBR-23 | Trigger mục 8 | `NhomBaTriggerIT` (000) mở rộng cho bảng của 003 |
| DBR-25 | `Clock`, `thoi_diem_du_kien` | `FR_016a_ThucHienTreIT` |
| DBR-26 | Bảng chuyển giường, `phan_bo_vua_dong_id` | `BR_M03_09_ChoSanSangIT` |
| DBR-32 (phần 003) | Tạo giường gọi ghi tăng tài sản trong cùng transaction | `Q_238_TaoGiuongGhiTangIT` (bản giả 019) |
| BR-M03-01 | FR-018, `KiemTraPhanBo` | `BR_M03_01_DieuKienPhanBoIT`, `BR_M03_01_CachLyIT` |
| BR-M03-02 | Research G-03 | `BR_M03_02_HaiNguoiCungGiuongIT` |
| BR-M03-03 | `SucChuaBanTruApi` | `BR_M03_03_SucChuaIT` |
| BR-M03-04 | Sự kiện `DaChuyenGiuong` | `BR_M03_04_SuKienChuyenIT` |
| BR-M03-05 | Bảng chuyển, `GiuongTheoTaiSanPort` | `BR_M03_05_GiuongCoNguoiIT` |
| BR-M03-06 | Bảng chuyển; job `HetHanGiuCho` | `BR_M03_06_VeTrongIT` |
| BR-M03-07 | `chuyenGiuong` một transaction; trigger bất biến | `BR_M03_07_ChuyenGiuongIT` |
| BR-M03-09 | `CHO_SAN_SANG`, job nhắc | `BR_M03_09_ChoSanSangIT` |
| BR-M03-10 | `CHECK`, `bat_buoc_khu_khuan` | `BR_M03_10_KhuKhuanIT` |
| BR-M03-13, 15 (phần 003) | `GiuongTheoTaiSanPort`, FR-013a, FR-013b | `BR_M03_13_GiuongHongDatTruocIT`, `BR_M03_15_DauChoChuyenNguoiIT` |
| BR-M03-19 | Dấu `chua_co_giuong_tu`, nhắc | `BR_M03_19_TroVeChuaCoGiuongIT` |
| BR-M03-20 | Phạm vi theo giường | `BR_M03_20_PhamViPhanBoBanDauIT` |
| BR-M03-21 | `TaiKhoanApi.coVaiTro`, sự kiện | `BR_M03_21_BacSiChiDinhIT` |
| BR-M03-22 | Cảnh báo khi tạo giường | `BR_M03_22_SoGiuongTieuChuanIT` |
| BR-M03-23 | `danhGiaLaiPhuHop` | `BR_M03_23_TuongLaiKhongPhuHopIT` |
| BR-M03-24 | `sauKhiDinhChinh` | `BR_M03_24_DinhChinhBao007IT` |
| BR-M03-25 | Đếm lại khi đổi buổi | `BR_M03_25_DoiKhungGioIT` |

BR-M03-16, 17, 18 thuộc tài sản và xe (feature 019); 003 chỉ cài phần giường của BR-M03-13, 15 qua port.
