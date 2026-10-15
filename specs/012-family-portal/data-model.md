# Data Model: Người thân và cổng thông tin gia đình (012)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Người cao tuổi, phòng, giường, tầng/khu, lượt vắng, sự cố, thông báo, tệp, tài khoản, nhân viên là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `nguoi_than_nct` | 2 (kỹ thuật) | Đích khóa theo người cao tuổi (F-03) |
| `nguoi_than` | 1 | A |
| `quan_he_nguoi_than` | 2 | A, B |
| `lich_su_quyen` | 3 | B (FR-068) |
| `dau_tu_ve` | 2 | C (FR-013) |
| `phieu_dang_ky_nguoi_than`, `phieu_dang_ky_dong` | 3 | B (FR-008) |
| `yeu_cau_quyen` | 2 | C |
| `ban_ghi_don`, `ban_ghi_don_su_kien`, `lan_chan_don` | 3 | D |
| `yeu_cau_ngoai_le_don` | 2 | D (FR-024) |
| `luot_tham`, `luot_tham_di_cung` | 2 | E |
| `khung_tham_ngay` | 2 (kỹ thuật) | E (FR-030 b) |
| `luot_o_lai`, `luot_o_lai_gia_han` | 2; 3 | F |
| `dem_o_lai` | Kỹ thuật | F (FR-043) |
| `nhat_ky_xem_suc_khoe` | 3 | G (FR-048) |
| `ban_tin`, `ban_tin_phan`, `ban_tin_so_lieu`, `ban_tin_nhan_xet` | 2 → 3 khi đã gửi | H |
| `phan_hoi` | 2 | I |
| `phan_hoi_muc` | 3 | I (FR-065) |
| `su_kien_m10_da_xu_ly`, `lan_chay_m10` | Kỹ thuật | Listener, job |

---

## 2. Người thân và quan hệ (mục A, B)

### `nguoi_than_nct` – nhóm 2 (kỹ thuật)

`nct_id` (khóa chính), `phien_ban`. Mọi lệnh đổi quan hệ, vai trò, quyền, lượt ở lại của một người cao tuổi khóa dòng này.

### `nguoi_than` – nhóm 1

`id`, `ho_ten`, `so_dien_thoai`, `loai_giay_to NULL`, `so_giay_to NULL` (`CA_NHAN`), `dia_chi`, `phien_ban`. Không xóa khi đã có quan hệ (FR-001). Số điện thoại không `UNIQUE`.

### `quan_he_nguoi_than` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nguoi_than_id`, `nct_id`, `quan_he` (`VO_CHONG`, `CON`, `CHAU`, `ANH_CHI_EM`, `KHAC`) | `UNIQUE (nguoi_than_id, nct_id, khoa_hieu_luc)` | FR-002 |
| `la_dai_dien`, `la_lien_he_chinh`, `thu_tu_lien_he` | | FR-002, FR-012 |
| `xem_suc_khoe`, `xem_chi_phi`, `nhan_khan`, `dang_ky_tham`, `duoc_phep_don`, `yeu_cau_dich_vu` | Chỉ đổi qua yêu cầu hoặc FR-006, FR-009, FR-010 | FR-007 |
| `trang_thai` (`HIEU_LUC`, `DA_KET_THUC`), `ket_thuc_luc`, `ly_do_ket_thuc` | Không kích hoạt lại | FR-006 |
| `khoa_hieu_luc` sinh `IF(trang_thai = 'HIEU_LUC', 1, NULL)` | Một quan hệ Hiệu lực mỗi cặp | |
| `khoa_lien_he_chinh` sinh `IF(trang_thai = 'HIEU_LUC' AND la_lien_he_chinh, nct_id, NULL)` `UNIQUE` | ≤ 1 liên hệ chính | DBR-02 |
| `phien_ban` | | |

Lệnh: Lập quan hệ, Đổi liên hệ chính (lý do), Đặt người đại diện đầu tiên (lý do, bằng chứng), Đổi thứ tự liên hệ (lý do), Kết thúc quan hệ (lý do). Đặt/Thôi khi đã có người đại diện đi qua `yeu_cau_quyen`. Sau mỗi lệnh: `KiemTraDbr02` (F-05).

### `lich_su_quyen` – nhóm 3

`quan_he_id`, `quyen` (sáu quyền, `DAI_DIEN`, `LIEN_HE_CHINH`), `gia_tri`, `tu_luc`, `yeu_cau_id NULL`, `ly_do_tu_dong NULL` (FR-009, FR-010, FR-006). Trả lời "ai được phép đón tại T" (FR-068).

### `dau_tu_ve` – nhóm 2

`nct_id` (khóa chính), `bat`, `yeu_cau_id`, `tu_luc`. Có tác dụng khi bật (Q-281).

### `phieu_dang_ky_nguoi_than` – nhóm 3

`id`, `nct_id`, `nguoi_dai_dien_id`, `tep_scan_id` (bắt buộc), `ghi_boi_id`, `luc`. `phieu_dang_ky_dong`: `phieu_id`, `quan_he_id`, `quyen`, `yeu_cau_id` (yêu cầu `HIEU_LUC` tạo kèm).

---

## 3. `yeu_cau_quyen` – nhóm 2 (mục C)

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `loai` (`QUYEN`, `TU_VE`, `DAT_DAI_DIEN`, `THOI_DAI_DIEN`) | | FR-013, FR-005 |
| `nct_id`, `quan_he_id NULL` (trống với `TU_VE`), `quyen NULL`, `gia_tri_moi` | | FR-013 |
| `nguoi_don_moi` `JSON NULL` (họ tên, số giấy tờ, quan hệ) | Khi thêm người đón chưa có hồ sơ: lệnh lập hồ sơ và quan hệ kèm | FR-013 |
| `nguon` (`QUAY`, `CONG`, `PHIEU_DANG_KY`), `lap_boi_id`, `lap_luc`, `ly_do` | | FR-013 |
| `trang_thai` (`CHO_XAC_NHAN`, `HIEU_LUC`, `TU_CHOI`, `HUY`) | | FR-020 |
| `cach_xac_nhan` (`CONG`, `BAN_KY`, `QL_DUYET`), `xac_nhan_boi_id`, `xac_nhan_luc`, `tep_bang_chung_id NULL`, `ly_do_quyet_dinh` | | FR-015 |
| `ap_ngay_khi_lap` | Tắt `duoc_phep_don`, tắt `TU_VE` | FR-017 |
| `khoa_cho` sinh `IF(trang_thai = 'CHO_XAC_NHAN', CONCAT(nct_id, ':', IFNULL(quan_he_id, 0), ':', IFNULL(quyen, loai)), NULL)` `UNIQUE` | | FR-018 |
| `lan_nhac`, `da_bao_ql_luc` | CFG-M10-11 | FR-019 |

| Từ | Lệnh | Đến | Người |
| --- | --- | --- | --- |
| — | Lập | `CHO_XAC_NHAN` | Hành chính; người thân theo FR-014 |
| — | Lập (người đại diện qua cổng; Hành chính kèm bản ký; phiếu đăng ký) | `HIEU_LUC` | Người đại diện; Hành chính |
| `CHO_XAC_NHAN` | Xác nhận | `HIEU_LUC` | Người đại diện (với đặt/thôi đại diện: người đại diện hiện có khác người bị tác động) |
| `CHO_XAC_NHAN` | Duyệt | `HIEU_LUC` | Quản lý viện (lý do) |
| `CHO_XAC_NHAN` | Từ chối | `TU_CHOI` | Người đại diện, Quản lý viện (lý do) |
| `CHO_XAC_NHAN` | Hủy | `HUY` | Người lập (lý do); Hệ thống (yêu cầu bỏ thay thế, FR-018; kết thúc quan hệ; trạng thái cuối, FR-075) |

---

## 4. Đón (mục D)

### `ban_ghi_don` – nhóm 3

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `loai` (`TAM_VANG`, `DI_CHOI_GIA_DINH`, `BAN_TRU_VE`, `TU_VE`, `VIEN_DUA_VE`, `KET_THUC_LUU_TRU`) | | FR-021, FR-026 |
| `nguoi_than_id NULL` (trống với `TU_VE`) | | FR-026 |
| `can_cu` (`DANH_SACH`, `NGOAI_LE_DAI_DIEN`, `NGOAI_LE_QL`, `TU_VE`), `ngoai_le_id NULL` | | FR-026 |
| `xac_minh` (`KHOP`, loại và số giấy tờ đã xem) | | FR-021 (3) |
| `thoi_diem`, `nhan_vien_ban_giao_id`, `ghi_boi_id` | | FR-021 (4), (5) |
| `het_hieu_luc_luc NULL` | `thoi_diem` + CFG-M10-09; `NULL` với `VIEN_DUA_VE` | FR-027 |

`ban_ghi_don_su_kien` (nhóm 3): `ban_ghi_don_id`, `loai` (`DA_DUNG`, `KHONG_DUNG`), `lenh_nguon NULL`, `nguon_id NULL`, `ly_do NULL`, `luc`; cột sinh `khoa_da_dung = IF(loai = 'DA_DUNG', ban_ghi_don_id, NULL)` `UNIQUE`. Trạng thái suy ra: không sự kiện → `CHUA_DUNG`; sự kiện gần nhất.

`lan_chan_don` (nhóm 3): `nct_id`, `nguoi_don_khai` `JSON`, `ly_do` (`NGOAI_DANH_SACH`, `GIAY_TO_KHONG_KHOP`, `NGOAI_LE_HET_HAN`, `DAU_TU_VE_TAT`), `thuc_hien_boi_id`, `luc`.

### `yeu_cau_ngoai_le_don` – nhóm 2

`id`, `nct_id`, `ho_ten`, `so_giay_to`, `quan_he`, `ly_do`, `lap_boi_id`, `trang_thai` (`CHO_XAC_NHAN`, `HIEU_LUC`, `DA_DUNG`, `HET_HAN`, `TU_CHOI`, `HUY`), `quyet_dinh_boi_id`, `cach` (`CONG`, `QL_DUYET`), `ly_do_quyet_dinh`, `hieu_luc_luc`, `het_han_luc` (lúc Hiệu lực + CFG-M10-08), `ban_ghi_don_id NULL` (đặt một lần).

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập | `CHO_XAC_NHAN` | Người thực hiện quy trình đón (FR-022) |
| `CHO_XAC_NHAN` | Xác nhận / Duyệt | `HIEU_LUC` | Người đại diện / Quản lý viện (lý do) |
| `CHO_XAC_NHAN` | Từ chối; Hủy | `TU_CHOI`; `HUY` | Người đại diện, Quản lý viện; người lập |
| `HIEU_LUC` | Ghi đón dùng ngoại lệ | `DA_DUNG` | (kèm lệnh Ghi đón) |
| `HIEU_LUC` | Tới `het_han_luc` | `HET_HAN` | Bộ lập lịch |
| `HIEU_LUC` | Hủy | `HUY` | Người đại diện, Quản lý viện (lý do) |

---

## 5. Thăm (mục E)

### `khung_tham_ngay` – nhóm 2 (kỹ thuật)

`ngay`, `gio_bat_dau`, `gio_ket_thuc` (khóa chính), `so_nguoi_da_giu`. Khóa `FOR UPDATE` khi đăng ký, hủy, Không đến; sức chứa đọc từ CFG-M10-04 lúc đăng ký.

### `luot_tham` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `nguoi_than_id` (người đăng ký), `ngay`, `gio_bat_dau`, `gio_ket_thuc` | Khung chép lúc đăng ký | FR-029, FR-032 |
| `so_nguoi`, `nguon` (`CONG`, `QUAY`), `dang_ky_boi_id` | ≤ CFG-M10-06 | FR-029 |
| `trang_thai` (`DA_DUYET`, `DA_VAO`, `DA_RA`, `KHONG_DEN`, `HUY`) | | FR-039 |
| `vao_luc`, `vao_boi_id`, `ra_luc`, `ra_boi_id`, `ly_do_huy`, `huy_boi` (người, Hành chính, Hệ thống) | | FR-033 → FR-037 |
| `da_nhac_ra_luc` | Một lần | FR-034 |

`luot_tham_di_cung`: `luot_id`, `ho_ten`, `quan_he`, `so_dien_thoai NULL`, `thuc_te_vao`, `them_luc_vao`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Đăng ký đạt FR-030 | `DA_DUYET` | Người thân có quyền; Hành chính |
| `DA_DUYET` | Hủy | `HUY` | Người đăng ký (trước giờ bắt đầu); Hành chính (lý do); Hệ thống (FR-036, FR-037, FR-006, FR-075) |
| `DA_DUYET` | Vào | `DA_VAO` | Hành chính |
| `DA_DUYET` | Hết khung | `KHONG_DEN` | Bộ lập lịch |
| `DA_VAO` | Ra | `DA_RA` | Hành chính |

---

## 6. Ở lại (mục F)

### `luot_o_lai` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nguoi_than_id`, `quan_he_id`, `nct_id`, `vi_tri` (`PHONG_NCT` hoặc mã vị trí khác), `ly_do`, `dang_ky_an` | | FR-040 |
| `bat_dau_du_kien`, `ket_thuc_du_kien`, `bat_dau_thuc_te`, `ket_thuc_thuc_te` | | FR-040, FR-043 |
| `trang_thai` (`CHO_XAC_NHAN`, `DA_XAC_NHAN`, `TU_CHOI`, `DANG_O_LAI`, `DA_KET_THUC`, `HUY`) | | FR-045 |
| `xac_nhan_boi_id`, `xac_nhan_luc`, `y_kien_cung_phong` `JSON NULL`, `ly_do_tu_choi`, `ly_do_huy` | | FR-040 |
| `da_nhac_ket_thuc_luc` | | FR-044 |

`luot_o_lai_gia_han` (nhóm 3): `luot_id`, `ket_thuc_du_kien_cu`, `moi`, `ly_do`, `boi_id`, `luc`.

| Từ | Lệnh | Đến | Người |
| --- | --- | --- | --- |
| — | Đăng ký | `CHO_XAC_NHAN` | Hành chính |
| `CHO_XAC_NHAN` | Xác nhận / Từ chối | `DA_XAC_NHAN` / `TU_CHOI` | Trưởng tầng được giao (không có: Quản lý viện) |
| `DA_XAC_NHAN` | Bắt đầu | `DANG_O_LAI` | Hành chính, Trưởng tầng |
| `CHO_XAC_NHAN`, `DA_XAC_NHAN` | Hủy | `HUY` | Hành chính (lý do) |
| `DANG_O_LAI` | Gia hạn | `DANG_O_LAI` | Hành chính (lý do) |
| `DANG_O_LAI` | Kết thúc | `DA_KET_THUC` | Hành chính, Trưởng tầng |

### `dem_o_lai` – kỹ thuật

`luot_id`, `ngay` (ngày bắt đầu của đêm), `UNIQUE (luot_id, ngay)`, `phat_luc`, `huy_luc NULL`. Một dòng mỗi lần lượt đi qua mốc 00:00; khóa nguồn gửi 010 là `luot_id:ngay`.

---

## 7. `nhat_ky_xem_suc_khoe` – nhóm 3 (mục G)

`id`, `nguoi_than_id`, `nct_id`, `phan` (`DI_UNG_BENH_NEN`, `CHI_SO`, `CAN_NANG`, `THUOC`, `SU_CO`, `KET_QUA_CHAM_SOC`, `AN_UONG`, `CHE_DO_AN`, `LICH_KHAM`, `BAN_TIN`, `THONG_BAO`), `nguon` (`CONG`, `BAN_TIN`, `THONG_BAO`), `nguon_id NULL`, `luc`. Chỉ mục `(nct_id, luc)`, `(nguoi_than_id, luc)`.

---

## 8. Bản tin (mục H)

### `ban_tin` – nhóm 2 → 3 khi `DA_GUI`, `KHONG_GUI`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `ky_tu`, `ky_den`, `moc` | Kỳ 7 ngày liền trước mốc | FR-051 |
| `ban_goc_id NULL`, `ly_do_dinh_chinh NULL` | Bản đính chính | FR-056 |
| `khoa_ky` sinh `IF(ban_goc_id IS NULL, CONCAT(nct_id, ':', ky_tu), NULL)` `UNIQUE` | Lệnh lập trùng bị chặn | FR-051 |
| `trang_thai` (`CHO_DUYET`, `DA_GUI`, `KHONG_GUI`), `ly_do_khong_gui` (`QUA_KY`, `QUA_DOI`) | | FR-057 |
| `lap_boi_id`, `lap_luc`, `duyet_boi_id`, `duyet_luc`, `duyet_thay`, `giai_thich_su_co NULL`, `muc_su_co_cao_nhat` | | FR-053, BR-M10-09 |
| `gui_luc`, `yeu_cau_thong_bao_id` | Thời điểm xem từng người lấy từ 009 | FR-054 |
| `da_dinh_chinh` | Đặt một lần khi có bản đính chính | FR-056 |
| `nhac_48h_luc`, `bao_duyet_thay_luc` | | FR-055 |

`ban_tin_phan` (`ban_tin_id`, `loai` `CHUNG`/`SUC_KHOE`/`CHI_PHI`, `noi_dung`); `ban_tin_so_lieu` (`ban_tin_id`, `ma`, `gia_tri` `JSON` lưu lúc duyệt, `loai_phan`; số liệu chi phí chỉ ở phần `CHI_PHI`); `ban_tin_nhan_xet` (nhóm 3: người, vai trò, nội dung, lúc). Trigger: khi `OLD.trang_thai <> 'CHO_DUYET'` chỉ cho đặt `da_dinh_chinh`.

---

## 9. Phản hồi (mục I)

### `phan_hoi` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nguoi_than_id`, `nct_id`, `loai` (`GOP_Y`, `KHIEU_NAI`, `HOI_DAP`), `nhom` (`CHAM_SOC`, `SUC_KHOE_THUOC`, `AN_UONG`, `CHI_PHI_HOP_DONG`, `KHAC`), `nguon`, `gui_luc` | | FR-058 |
| `muc` (`CAO`, `THUONG`), `moc_tinh_han`, `han_xu_ly` | | FR-061, FR-062 |
| `phu_trach_vai_tro`, `phu_trach_id NULL` | Hành chính: nhóm cho tới khi có người Nhận xử lý | FR-059, FR-060 |
| `trang_thai` (`MOI`, `DANG_XU_LY`, `DA_PHAN_HOI`, `MO_LAI`, `DONG`) | | FR-066 |
| `qua_han`, `han_da_leo_thang NULL` | Một lần mỗi hạn | FR-063 |
| `da_phan_hoi_luc`, `dong_luc`, `cach_dong` (`NGUOI_GUI`, `TU_DONG`) | | FR-064 |

`phan_hoi_muc` (nhóm 3): `phan_hoi_id`, `loai` (`NOI_DUNG`, `TRA_LOI`, `HUONG_XU_LY`, `GHI_CHU`, `GIAO_LAI`, `DOI_MUC`, `MO_LAI`), `noi_dung`, `phan_loai_thong_tin` (`CHUNG`, `SUC_KHOE`), `tep_ids`, `boi_id`, `luc`.

---

## 10. Kỹ thuật

- `su_kien_m10_da_xu_ly`: `khoa_su_kien` `UNIQUE`, `xu_ly_luc`.
- `lan_chay_m10`: `ten_viec`, `moc`, `bat_dau`, `ket_thuc`, `ket_qua`.

## 11. DBR và quy tắc

| Mã | Cách bảo đảm |
| --- | --- |
| DBR-02 | `khoa_lien_he_chinh` `UNIQUE` (≤ 1); `KiemTraDbr02` trong mọi lệnh đổi vai trò (≥ 1 đại diện, đúng 1 liên hệ chính) dưới khóa `nguoi_than_nct`; điều kiện `DBR_02` của Hoàn tất tiếp nhận |
| DBR-03 | `QuyenCoTacDung` kiểm bản đồng ý tại thời điểm xem; DTO người thân phần sức khỏe chỉ khi có tác dụng |
| DBR-25 | Ngày, mốc, khung theo Asia/Ho_Chi_Minh |
| BR-M10-07 (một yêu cầu chờ) | `khoa_cho` `UNIQUE` |
| BR-M10-03 (một lần dùng) | `khoa_da_dung` `UNIQUE`; ngoại lệ `ban_ghi_don_id` đặt một lần |
| BR-M10-08 (một bản tin mỗi kỳ) | `khoa_ky` `UNIQUE` |
| BR-M10-04 (đêm) | `dem_o_lai` `UNIQUE (luot_id, ngay)` |
| Nhóm 3 (000) | Trigger chặn UPDATE/DELETE trên bảng nhóm 3 ngoài cột đặt một lần |
