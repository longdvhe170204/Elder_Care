# Data Model: Quản lý hoạt động và chất lượng chăm sóc (014)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bảng của 014 nằm trong schema của M04 cùng bảng của 005; công việc, ca, người cao tuổi, phòng, tầng/khu, dịch vụ, sự cố, cảnh báo, nhân viên, người thân, tệp là tham chiếu logic (`BIGINT`). Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `hoat_dong_nct` | 2 (kỹ thuật) | Đích khóa sở thích, chuỗi cô lập (L-03) |
| `loai_hoat_dong`, `loai_hoat_dong_nhom_so_thich`, `nhom_so_thich` | 1 | A, I |
| `hoat_dong` | 1 | A |
| `mau_lap_phien_ban` | 1 (có lịch sử) | B |
| `nguoi_thuong_xuyen` | 2 | B |
| `buoi_hoat_dong` | 2 | C |
| `dang_ky_buoi` | 2 | D |
| `dong_y_ve_muon` | 2 | D (FR-016a) |
| `diem_danh_buoi` | 3 | F |
| `chuyen_di`, `chuyen_di_gia_han`, `nhiem_vu_chuyen` | 2; 3; 2 | G, H |
| `nguoi_tham_gia` | 2 | G, H |
| `danh_gia_kha_nang` | 3 | G (FR-036) |
| `diem_danh_chuyen` | 3 | H |
| `so_thich` | 2 | I |
| `chuoi_co_lap` | Kỹ thuật | I (FR-054) |
| `danh_sach_kiem_tra`, `muc_kiem_tra`, `ca_khong_danh_sach` | 2; 2 → 3 khi có kết quả; 3 | J |
| `su_kien_m04hd_da_xu_ly`, `lan_chay_m04hd` | Kỹ thuật | Listener, job |

---

## 2. Danh mục, hoạt động, mẫu (mục A, B)

- `loai_hoat_dong`: `id`, `ten`, `hoat_dong_nhom`, `ngoai_vien`, `trang_thai`. `nhom_so_thich`: `id`, `ten`, `trang_thai`. `loai_hoat_dong_nhom_so_thich`: cặp khóa.
- `hoat_dong`: `id`, `ten`, `loai_id`, `pham_vi` (`TANG` + `tang_khu_vuc_id` | `TOAN_VIEN`), `tao_boi_truong_tang_id`, `dia_diem_mac_dinh` (phòng/khu vực của 003, hoặc mô tả điểm đến), `nguoi_phu_trach_mac_dinh_id`, `thoi_luong_phut`, `so_luong_toi_da`, `doi_tuong` `JSON` (mức chăm sóc, loại lưu trú, tầng mời), `co_thu_phi`, `dich_vu_id NULL` (bắt buộc khi có thu phí), `hoat_dong_nhom` (mặc định theo loại; đổi kèm lý do), `trang_thai`, `ngung_tu NULL`.
- `mau_lap_phien_ban`: `hoat_dong_id`, `ap_dung_tu`, `ngay_trong_tuan` hoặc `chu_ky_ngay`, `gio_bat_dau`, `gio_ket_thuc`, `ngay_bat_dau`, `ngay_ket_thuc NULL`, `ly_do`, `boi_id`.
- `nguoi_thuong_xuyen` (nhóm 2): `hoat_dong_id`, `nct_id`, `tu_luc`, `den_luc NULL`, `ly_do_go`; cột sinh `UNIQUE` cho bản đang hiệu lực.

---

## 3. `buoi_hoat_dong` – nhóm 2 (mục C)

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `hoat_dong_id`, `bat_dau`, `ket_thuc` | `UNIQUE (hoat_dong_id, bat_dau)` kể cả buổi Đã hủy | FR-007 |
| `nguon` (`MAU`, `LE`) | | FR-006, FR-011 |
| `dia_diem_phong_id` / `dia_diem_khu_vuc_id`, `tang_khu_vuc_id` | Buổi trong viện luôn gắn tầng | FR-002a |
| `nguoi_phu_trach_id` | Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng | FR-002a |
| `trang_thai` (`DA_LEN_LICH`, `DA_DIEM_DANH`, `KHONG_DIEM_DANH`, `DA_HUY`) | Buổi trong viện; chuyến đi dùng `chuyen_di.trang_thai` | Bảng trạng thái buổi |
| `diem_danh_khong_du`, `ly_do_huy`, `huy_boi` (người / Hệ thống: `DOI_MAU`, `NGUNG`, `KHOANH_VUNG`) | | FR-009, FR-013, FR-014a, FR-021 |
| `da_nhac_diem_danh_luc` | CFG-M04-06 | FR-030 |
| `phien_ban` | | |

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Tạo buổi từ mẫu / Tạo buổi lẻ | `DA_LEN_LICH` | Trưởng tầng |
| `DA_LEN_LICH` | Dời buổi | `DA_LEN_LICH` | Trưởng tầng (lý do) |
| `DA_LEN_LICH` | Hủy buổi | `DA_HUY` | Trưởng tầng; Hệ thống (đổi mẫu, ngừng, khoanh vùng) |
| `DA_LEN_LICH` | Hoàn tất điểm danh | `DA_DIEM_DANH` | Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng |
| `DA_LEN_LICH` | Hết ngày (có / không có lượt điểm danh) | `DA_DIEM_DANH` (dấu) / `KHONG_DIEM_DANH` | Bộ lập lịch |

---

## 4. Đăng ký (mục D)

### `dang_ky_buoi` – nhóm 2

`id`, `buoi_id`, `nct_id`, `nguon` (`THUONG_XUYEN`, `THEM_TAY`, `THEM_LUC_DIEM_DANH`), `dang_ky_boi_id`, `luc`, `trang_thai` (`DA_DANG_KY`, `DA_HUY`, `DA_DIEM_DANH`), `ly_do_huy`, `huy_tu_dong` (`DOI_MAU`, `DOI_BUOI`, `KHOANH_VUNG`, `NGHI_NHIEM`, `TRANG_THAI_CUOI`, `RUT_DONG_Y`), `canh_bao_da_xac_nhan` `JSON`, `dong_y_ve_muon_id NULL`; cột sinh `khoa_hieu_luc = IF(trang_thai <> 'DA_HUY', CONCAT(buoi_id, ':', nct_id), NULL)` `UNIQUE`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Đăng ký; đăng ký sẵn từ mẫu; thêm lúc điểm danh | `DA_DANG_KY` (thêm lúc điểm danh: `DA_DIEM_DANH`) | Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng; kèm lệnh tạo buổi |
| `DA_DANG_KY` | Hủy đăng ký | `DA_HUY` | Như trên (lý do, trước giờ bắt đầu) |
| `DA_DANG_KY` | Hủy tự động | `DA_HUY` | Hệ thống |
| `DA_DANG_KY` | Ghi kết quả điểm danh; hết ngày | `DA_DIEM_DANH` | Người điểm danh; Bộ lập lịch (Không ghi nhận) |

### `dong_y_ve_muon` – nhóm 2

`id`, `nct_id`, `buoi_id`, `nguoi_dai_dien_id`, `cach` (`CONG`, `BAN_KY`), `tep_ban_ky_id NULL`, `gio_ve_du_kien_moi`, `ghi_nhan_boi_id`, `luc`, `trang_thai` (`HIEU_LUC`, `DA_RUT`, `DA_DUNG`, `HET_HIEU_LUC`), `rut_boi`, `rut_luc`; `UNIQUE` (buổi, người cao tuổi) cho bản Hiệu lực.

---

## 5. `diem_danh_buoi` – nhóm 3 (mục F)

`id`, `dang_ky_id`, `buoi_id`, `nct_id`, `ket_qua` (`CO_MAT`, `VANG`, `KHONG_GHI_NHAN`), `ly_do_vang` (`TU_CHOI`, `SUC_KHOE`, `DANG_VANG_MAT`, `KHONG_DEN`, `KHAC`), `muc_tham_gia` (`TICH_CUC`, `BINH_THUONG`, `THU_DONG`, `BO_GIUA_CHUNG`), `muc_giao_tiep`, `tinh_trang_sau`, `ghi_chu`, `ghi_nhan_muon`, `boi_id`, `luc`, `thoi_diem_thiet_bi NULL`, `tang_khu_vuc_id` (tầng gắn đính chính); `UNIQUE (dang_ky_id)` cho bản hiện hành (đính chính theo 000).

---

## 6. Chuyến đi (mục G, H)

### `chuyen_di` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `buoi_id` (khóa chính, trỏ `buoi_hoat_dong`) | | FR-034 |
| `diem_den`, `roi_du_kien`, `ve_du_kien` (hiện hành), `phuong_tien`, `xe_cua_vien`, `xe_mo_ta` | | FR-034, FR-040 |
| `trang_thai` (`DA_LEN_LICH`, `DANG_DI`, `DA_VE`, `DA_HUY`) | | Bảng trạng thái chuyến |
| `qua_gio_ve`, `da_bao_qua_gio_cho_moc`, `da_bao_ql_cho_moc` | Một lần mỗi mốc giờ về | FR-049, FR-049a |
| `truong_doan_tam_tu_xa` | | FR-035a |
| `ket_thuc_ve_luc`, `ket_thuc_ve_boi_id` | | FR-047, FR-050 |

`chuyen_di_gia_han` (nhóm 3): `buoi_id`, `ve_du_kien_cu`, `moi`, `ly_do`, `boi_id`, `luc`.

`nhiem_vu_chuyen` (nhóm 2): `buoi_id`, `nhan_vien_id`, `vai` (`TRUONG_DOAN`, `DI_CUNG`), `tu_luc`, `den_luc NULL`, `ly_do_ket_thuc` (`CHUYEN_TRUONG_DOAN`, `DOI_CA`, `NGHI_DOT_XUAT`, `GO`); cột sinh `UNIQUE` một trưởng đoàn hiện hành mỗi chuyến.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Tạo từ mẫu / buổi lẻ | `DA_LEN_LICH` | Trưởng tầng |
| `DA_LEN_LICH` | Dời / Hủy | `DA_LEN_LICH` / `DA_HUY` | Trưởng tầng; Hệ thống (đổi mẫu, ngừng) |
| `DA_LEN_LICH` | Điểm danh rời viện người đầu tiên | `DANG_DI` | Trưởng đoàn; Trưởng tầng thay |
| `DANG_DI` | Rời viện đi sau; gia hạn; chuyển trưởng đoàn; về từng phần; báo thiếu người; quá giờ | `DANG_DI` | Theo bảng của spec |
| `DANG_DI` | Hủy ghi nhận bản ghi rời viện cuối cùng | `DA_LEN_LICH` | Trưởng tầng |
| `DANG_DI` | Kết thúc điểm danh về | `DA_VE` | Trưởng đoàn, Trưởng tầng |
| `DA_VE` | Điểm danh về muộn | `DA_VE` | Trưởng đoàn, Trưởng tầng |

### `nguoi_tham_gia` – nhóm 2

`id`, `buoi_id`, `nct_id`, `dang_ky_id`, `trang_thai` (`DA_DANG_KY`, `DUOC_DI`, `KHONG_DI`, `DA_ROI_VIEN`, `DA_VE`, `THIEU_KHI_VE`, `ROI_DOAN`), `ly_do`, `su_co_thieu_nguoi_id NULL` (đặt một lần), `canh_bao_thieu_nguoi_da_yeu_cau`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Đăng ký | `DA_DANG_KY` | Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng |
| `DA_DANG_KY` | Đánh giá Đạt | `DUOC_DI` | Trưởng tầng |
| `DA_DANG_KY`, `DUOC_DI` | Không đạt; nghi nhiễm; hủy đăng ký | `KHONG_DI` | Trưởng tầng; Hệ thống; người đăng ký |
| `DUOC_DI` | Rời viện | `DA_ROI_VIEN` | Trưởng đoàn; Trưởng tầng thay |
| `DA_ROI_VIEN` | Về / Báo thiếu người / Kết thúc về khi chưa về | `DA_VE` / `THIEU_KHI_VE` / `THIEU_KHI_VE` | Trưởng đoàn, người đi cùng (báo thiếu), Trưởng tầng |
| `DA_ROI_VIEN` | Hủy ghi nhận rời viện | `DUOC_DI` hoặc `KHONG_DI` | Trưởng tầng |
| `DA_VE` | Hủy ghi nhận về | `DA_ROI_VIEN` | Trưởng tầng |
| `DUOC_DI` | Chuyến dời sang ngày khác | `DA_DANG_KY` | Hệ thống |
| `DA_ROI_VIEN`, `THIEU_KHI_VE` | Chuyển viện / Qua đời | `ROI_DOAN` | Hệ thống (theo lệnh nguồn) |
| `THIEU_KHI_VE` | Về muộn | `DA_VE` | Trưởng đoàn, Trưởng tầng |

`danh_gia_kha_nang` (nhóm 3): `nguoi_tham_gia_id`, `ket_qua` (`DAT`, `KHONG_DAT`), `ly_do`, `boi_id` (Trưởng tầng của tầng người cao tuổi), `luc`, `hien_thi` `JSON` (thông tin đã hiển thị), `het_hieu_luc_luc NULL` (dời sang ngày khác).

`diem_danh_chuyen` (nhóm 3): `id`, `nguoi_tham_gia_id`, `loai` (`ROI_VIEN`, `VE`), `thoi_diem`, `boi_id`, `thay_truong_doan_ly_do NULL`, `thoi_diem_thiet_bi NULL`. Đính chính (`M04.DIEM_DANH_CHUYEN`) chỉ Trưởng tầng của chuyến; không đổi người, loại.

---

## 7. Tinh thần (mục I)

- `hoat_dong_nct`: `nct_id`, `phien_ban`.
- `so_thich` (nhóm 2): `id`, `nct_id`, `nhom_id`, `mo_ta`, `muc` (`THICH`, `KHONG_THICH`), `nguon_loai` (`NGUOI_CAO_TUOI`, `NGUOI_THAN`, `NHAN_VIEN_QUAN_SAT`), `nguon_nguoi_than_id NULL` (mẫu cột nguồn 001 data-model mục 6a; sửa 2026-10-18, Q-363), `trang_thai` (`HIEN_HANH`, `DA_THAY`, `DA_BO`), `boi_id`, `luc`, `ly_do_bo`, `thay_boi_id NULL`; cột sinh `khoa_hien_hanh` theo (người cao tuổi, nhóm, nguồn) `UNIQUE` (L-09).
- `chuoi_co_lap` (kỹ thuật): `nct_id`, `so_ngay_tinh`, `ngay_bat_dau_chuoi`, `ngay_xet_cuoi`, `da_yeu_cau_canh_bao_cho_chuoi`.

---

## 8. Kiểm tra chất lượng (mục J)

### `danh_sach_kiem_tra` – nhóm 2

`id`, `ca_id`, `tang_khu_vuc_id`, `lap_boi_id`, `vai_tro_lap` (`TRUONG_TANG`, `NGUOI_PHU_TRACH_CA`), `lap_luc`, `so_du_dieu_kien_luc_lap`, `da_bao_ql_qua_han_luc`; `UNIQUE (ca_id, tang_khu_vuc_id)`.

### `muc_kiem_tra` – nhóm 2 → 3 khi có kết quả

`id`, `danh_sach_id`, `cong_viec_id` `UNIQUE`, `nguoi_thuc_hien_goc_id`, `trang_thai` (`CHO_KIEM_TRA`, `DAT`, `KHONG_DAT`, `KHONG_KIEM_TRA_DUOC`, `QUA_HAN_KIEM_TRA`), `kiem_tra_boi_id`, `vai_tro_kiem_tra`, `ghi_chu`, `tep_anh_ids`, `cong_viec_lam_lai_id NULL`, `luc`. Trigger: khi `OLD.trang_thai <> 'CHO_KIEM_TRA'` chặn mọi đổi (đính chính theo 000).

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập danh sách; thêm mục | `CHO_KIEM_TRA` | Người kiểm tra |
| `CHO_KIEM_TRA` | Ghi Đạt / Không đạt (kèm công việc làm lại) / Không kiểm tra được | `DAT` / `KHONG_DAT` / `KHONG_KIEM_TRA_DUOC` | Người kiểm tra (FR-066) |
| `CHO_KIEM_TRA` | Công việc bị Hủy ghi nhận | `KHONG_KIEM_TRA_DUOC` | Hệ thống |
| `CHO_KIEM_TRA` | Hết ca | `QUA_HAN_KIEM_TRA` | Hệ thống |

`ca_khong_danh_sach` (nhóm 3): `ca_id`, `tang_khu_vuc_id`, `so_du_dieu_kien`, `ly_do` (`CA_KHONG_CO_NGUOI_KIEM_TRA`, `KHONG_LAP`), `luc`.

---

## 9. DBR và quy tắc

| Mã | Cách bảo đảm |
| --- | --- |
| DBR-12 (tương tự) | `UNIQUE (hoat_dong_id, bat_dau)` |
| DBR-15 | Sự kiện tính phí khóa theo `diem_danh_buoi.id` / `diem_danh_chuyen.id` (010 chống trùng) |
| DBR-18 | Cảnh báo cô lập gộp ở 007 theo nguồn `CO_LAP` |
| DBR-23 | Nhật ký mọi lệnh |
| DBR-25 | Thời điểm thiết bị cho bản ghi ngoại tuyến; ngày tính theo Asia/Ho_Chi_Minh |
| BR-M04-15 | `DieuKienDangKy`; khóa buổi khi đếm sức chứa |
| BR-M04-16 | Điều kiện rời viện (đánh giá Đạt, thuốc, lịch xe) |
| BR-M04-17 | `su_co_thieu_nguoi_id` một lần; cảnh báo Khẩn cấp khi kết thúc về |
| BR-M04-18 | Sự kiện tính phí cho lượt Có mặt ở hoạt động có thu phí |
| BR-M04-21 | Tạo buổi từ mẫu bằng lệnh; đổi mẫu theo phiên bản |
| BR-M04-22 | `chuoi_co_lap`; job 00:00 |
| BR-M04-23 | `UNIQUE (ca_id, tang_khu_vuc_id)`, `UNIQUE (cong_viec_id)`; kiểm tỷ lệ khi lập |
