# Data Model: Nhân sự, ca trực và bàn giao (008)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1; không nhắc lại ở từng bảng. Tài khoản, người cao tuổi, tầng/khu vực, phòng, loại công việc, công việc, liều, cảnh báo, sự cố, giường, đồ gửi, chuyến đi, tệp là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Thời điểm lưu `DATETIME(6)` (000 R-04; DBR-25).

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `nhan_vien` | 1 (mô tả) + 2 (trạng thái) | A (FR-001 → FR-004) |
| `bo_phan`, `nhan_vien_bo_phan` | 1; lịch sử | A (FR-001a) |
| `lich_su_phu_trach_bo_phan` | 3 | A (FR-001a) |
| `loai_chung_chi`, `pham_vi_hanh_nghe` | 1 | A (FR-005) |
| `chung_chi`, `chung_chi_pham_vi` | 1 có lịch sử | A (FR-006 → FR-010) |
| `mau_ca` | 1 | B (FR-011) |
| `lich_ca`, `ca`, `ca_nhan_vien` | 2 | B (FR-012 → FR-023) |
| `nguoi_phu_trach_ca` | 3 (lịch sử) | B (FR-019, FR-037a) |
| `ban_cong_bo_lich` | 3 | B (FR-055) |
| `tang_ca_khoa` | Kỹ thuật | C (khóa theo tầng, N-03) |
| `giao_truong_tang`, `phan_cong` | 2 | C (FR-024 → FR-032) |
| `lan_tinh_ty_le` | 3 | D (FR-037) |
| `ban_giao` | 2 (trạng thái) | E (FR-038 → FR-047) |
| `muc_ban_giao`, `nhan_dinh_ban_giao` | 3 có bản nháp | E (FR-039 → FR-041) |
| `y_kien_ban_giao`, `phan_hoi_y_kien` | 3 | E (FR-042) |
| `lan_chay_m09` | Kỹ thuật | Job (N-13) |

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
| `lan_import_id` *(bổ sung 2026-10-22)* | `BIGINT NULL` | Khóa ngoại `lan_import_nhan_vien` | FR-001b |
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

### `lan_import_nhan_vien` – nhóm 3 *(bổ sung 2026-10-22, Q-382; research N-16)*

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `nguoi_thuc_hien_id`, `thoi_diem` | | | FR-001c |
| `ten_file` | `VARCHAR(255)` | Không lưu nội dung file | FR-001c |
| `so_dong` | `INT` | `<=` CFG-M09-13 lúc thực hiện | FR-001b |

Danh sách hồ sơ đã tạo là các `nhan_vien` có `lan_import_id` này (và nằm trong nhật ký `NV_IMPORT`). Không `UPDATE`, `DELETE`.

### `bo_phan` – nhóm 1

`id`, `ten`, `phu_trach_id NULL` (nhân viên), `trang_thai_hieu_luc`, `phien_ban`. Khởi tạo: Y tế, Chăm sóc, Bếp, Hành chính – Kế toán (Q-284).

### `nhan_vien_bo_phan` – lịch sử

`nhan_vien_id`, `bo_phan_id`, `tu`, `den NULL`, `nguoi_thuc_hien_id`, `ly_do`. Cột sinh `khoa_hien_hanh = IF(den IS NULL, nhan_vien_id, NULL)` `UNIQUE` (DBR-36: một bộ phận tại một thời điểm). Không có dòng cho tài khoản Quản lý viện (Q-299, backend kiểm).

Triển khai Phase 9: bảng thật `staff_department`; migration V008_08 bổ sung trigger chỉ đóng `ends_at` một lần, không đổi nhân viên/bộ phận/thời điểm đầu/người ghi/lý do, không sửa bản đã đóng. Xóa chỉ qua tài khoản lưu giữ khi đủ điều kiện; quyền ứng dụng chỉ INSERT và UPDATE(ends_at). M09RecordProtectionIT kiểm cả quản trị, DepartmentCommandsIT kiểm lệnh chuyển bộ phận vẫn hợp lệ.

### `lich_su_phu_trach_bo_phan` – nhóm 3

`bo_phan_id`, `nhan_vien_id`, `tu`, `den NULL`, `nguoi_chi_dinh_id`, `ly_do`. Một dòng mở mỗi bộ phận: cột sinh `khoa_mo = IF(den IS NULL, bo_phan_id, NULL)` `UNIQUE`; dòng chỉ được đặt `den` một lần.

---

## 3. Giấy phép, đào tạo (mục A)

### `loai_chung_chi` – nhóm 1

`id`, `ten`, `nhom` (`GIAY_PHEP_HANH_NGHE`, `DAO_TAO`), `co_thoi_han`, `bat_buoc_voi_vai_tro` `JSON` (tập `VaiTro`), `co_pham_vi`, `trang_thai_hieu_luc`.

### `pham_vi_hanh_nghe` – nhóm 1

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

### `chung_chi_pham_vi`

`chung_chi_id`, `ma_pham_vi`. Khóa chính hai cột.

---

## 4. Mẫu ca, lịch ca, ca (mục B)

### `mau_ca` – nhóm 1

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
| `nguoi_phu_trach_id`, `thieu_nguoi_phu_trach` | Hiện hành; lịch sử ở `nguoi_phu_trach_ca` | FR-015, FR-019 |
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

### `nguoi_phu_trach_ca` – nhóm 3 (lịch sử)

`ca_id`, `nhan_vien_id`, `tu`, `den NULL`, `can_cu` (`LAP_LICH`, `CHUYEN`, `VANG_CA`, `DOI_CA_015`), `nguoi_thuc_hien_id`, `ly_do`. Một dòng mở mỗi ca: cột sinh `khoa_mo = IF(den IS NULL, ca_id, NULL)` `UNIQUE` (FR-015). Chỉ được đặt `den` một lần.

### `ban_cong_bo_lich` – nhóm 3

`lich_ca_id`, `cong_bo_luc`, `noi_dung` `JSON` (ca, nhân viên, người phụ trách, phân công lúc công bố). FR-055 "bản gốc có nhật ký".

---

## 5. Giao trưởng tầng, phân công (mục C)

### `tang_ca_khoa` – kỹ thuật

`tang_khu_vuc_id` khóa chính. Đích khóa cho lệnh phân công, giao trưởng tầng (N-03).

### `giao_truong_tang` – nhóm 2

`id`, `nhan_vien_id`, `tang_khu_vuc_id`, `tu`, `den NULL`, `trang_thai` (`HIEU_LUC`, `DA_KET_THUC`), `nguoi_giao_id`, `ly_do`, `phien_ban`. Không chồng thời gian trên cùng tầng (FR-024): kiểm ở backend dưới khóa `tang_ca_khoa`. Rút ngắn đặt `den` sớm hơn (không lùi trước bây giờ).

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

Chỉ mục `(ca_id, trang_thai)`, `(nhan_vien_id, bat_dau)`, `(loai_doi_tuong, doi_tuong_id, bat_dau)`. FR-028 (không hai phân công chính cùng mức, cùng vai trò, cùng giới hạn, cùng đối tượng, chồng thời gian) kiểm ở backend dưới khóa `tang_ca_khoa`. Trigger chặn mọi đổi khi `OLD.trang_thai IN ('DA_KET_THUC','DA_HUY')`; ở `NHAP` được `DELETE` (lịch Nháp, FR-025), trạng thái khác thì không.

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

### `lan_tinh_ty_le` – nhóm 3

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

### `muc_ban_giao` – nhóm 3 có bản nháp

`id`, `ban_giao_id`, `nhom` (`CONG_VIEC`, `LIEU`, `CHI_SO`, `CANH_BAO`, `SU_CO`, `BIEN_DONG_NCT`, `GIUONG_CHO_SAN_SANG`, `DO_GUI`, `VAN_DE_THEO_DOI`), `nguon_loai`, `nguon_id` (chuỗi, ví dụ `lieuKey`), `nct_id NULL`, `phong_id NULL` (một trong hai bắt buộc với `VAN_DE_THEO_DOI`, FR-051), `mo_ta`, `ghi_chu`, `nguoi_ghi_chu_id`, `trang_thai_nguon_luc_chot`, `phan` (`DA_CHOT`, `PHAT_SINH_SAU_KHI_LAP`, `DA_XU_LY_TRONG_CA`), `them_boi_id`, `them_luc`. Không có cột "tự lập".

Trigger: khi `ban_giao.chot_luc IS NOT NULL`, dòng `DA_CHOT`, `DA_XU_LY_TRONG_CA` không `UPDATE`, `DELETE`; dòng `PHAT_SINH_SAU_KHI_LAP` chỉ thêm; khi `DA_XAC_NHAN` mọi dòng khóa. Ở Bản nháp: xóa dòng nhóm `CHI_SO`, `BIEN_DONG_NCT` bị chặn (Q-88).

### `nhan_dinh_ban_giao` – nhóm 3 có bản nháp

`ban_giao_id`, `loai` (`CHUNG`, `THEO_NGUOI_CAO_TUOI`), `nct_id NULL`, `noi_dung`, `nguoi_viet_id`. Cùng trigger khóa sau `chot_luc`.

### `y_kien_ban_giao`, `phan_hoi_y_kien` – nhóm 3

`y_kien_ban_giao`: `ban_giao_id`, `nguoi_gui_id`, `noi_dung`, `thoi_diem`. `phan_hoi_y_kien`: `y_kien_id`, `nguoi_viet_id`, `noi_dung`, `thoi_diem`. Chỉ thêm.

---

## 8. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bất biến | Cách bảo đảm | Nguồn |
| --- | --- | --- |
| Mỗi ca có yêu cầu bàn giao có đúng một bàn giao; bàn giao đã xác nhận không sửa | `UNIQUE (ca_id)`; trigger `ban_giao`, `muc_ban_giao`, `nhan_dinh_ban_giao`; ca không `DA_DONG` khi chưa lập xong (job, N-06) | DBR-20, BR-M09-07, 08 |
| Không phân công nhân viên thiếu chứng chỉ; không hai ca chồng giờ | Backend dưới khóa (`HieuLucChungChi`, chồng giờ); test đồng thời | DBR-21, BR-M09-01, 03 |
| Một người phụ trách ca mỗi thời điểm | `UNIQUE khoa_mo` của `nguoi_phu_trach_ca` | FR-015 |
| Một nhân viên chính mỗi (người, vai trò, thời điểm) | Backend dưới khóa `tang_ca_khoa` | FR-028, SC-011 |
| Một Trưởng tầng được giao mỗi tầng mỗi thời điểm | Backend dưới khóa `tang_ca_khoa` | FR-024, SC-011 |
| Mỗi tài khoản nhân viên thuộc đúng một bộ phận; mỗi bộ phận một Phụ trách | `UNIQUE khoa_hien_hanh`; `UNIQUE khoa_mo` | DBR-36, FR-001a |
| Hồ sơ nhân viên, chứng chỉ không xóa | Trigger `BEFORE DELETE` | FR-001, FR-006 |
| Mọi thay đổi có nhật ký | `NhatKyApi.ghi` trong transaction; công bố lưu `ban_cong_bo_lich` | DBR-23, FR-055 |
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

- Tên bảng thật theo glossary 14.9: `floor_scope_lock`, `floor_manager_assignment`, `staff_assignment`, chỉ khóa ngoại nội bộ M09. Trạng thái cuối được bảo vệ ở trigger; chỉ tài khoản lưu giữ được loại bỏ bản ghi cuối theo provider.
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
