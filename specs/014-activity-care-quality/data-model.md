# Data Model: Quản lý hoạt động và chất lượng chăm sóc (014)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bảng của 014 nằm trong schema của M04 cùng bảng của 005; công việc, ca, người cao tuổi, phòng, tầng/khu, dịch vụ, sự cố, cảnh báo, nhân viên, người thân, tệp là tham chiếu logic (`BIGINT`). Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp: `buoi_hoat_dong` (+ `chuyen_di`, 1–1); `dang_ky_buoi` (+ `nguoi_tham_gia` của chuyến).
- `su_kien_chung`: `diem_danh_buoi` (lượt có mặt có thu phí là nguồn tính phí, `loaiBanGhi` `M04.DIEM_DANH_BUOI`), kết quả `muc_kiem_tra`.
- `ho_so_chung`: `dong_y_ve_muon`, `danh_sach_kiem_tra` (mục thành `JSON`).
- Bảng phụ, bảng con *(bổ sung 2026-10-30, rà đủ tên bảng của data-model)*: `hoat_dong_nct`, `chuoi_co_lap` → `trang_thai_nct`; `loai_hoat_dong`, `nhom_so_thich`, `loai_hoat_dong_nhom_so_thich`, `hoat_dong` → `danh_muc_chung`; `mau_lap_phien_ban`, `nguoi_thuong_xuyen`, `so_thich` → `ho_so_chung`; `nhiem_vu_chuyen` → `lich_su_chung`; `chuyen_di_gia_han`, `danh_gia_kha_nang`, `diem_danh_chuyen` → `su_kien_chung`; `ca_khong_danh_sach`, `su_kien_m04hd_da_xu_ly`, `lan_chay_m04hd` → `moc_da_nhac`. Không thêm bảng riêng.
- Còn 2 bảng riêng của feature.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-30; sinh từ mục 0 và ràng buộc ở mô tả bảng các mục sau; chủ module rà, hoàn thiện khi viết migration, task T034)*

Quy ước: loại ở bảng chung đặt mã `<module>.<TÊN_BẢNG_CŨ>` (module M04; đổi sang tên tiếng Anh theo glossary khi dịch); `noi_dung`, `thuoc_tinh` hoặc `du_lieu` mang các cột của bảng cũ, có JSON Schema theo loại; bảng gộp riêng của feature có cột `kind` (hoặc `loai`) và cột `NULL` có `CHECK` theo loại. Mỗi dòng có ràng buộc ở bảng dưới cần một test tích hợp chứng minh ràng buộc vẫn giữ sau gộp (constitution XII), ghi vào bảng DBR áp dụng khi cài.

**Bảng giữ hoặc là bảng đích** (giữ cột, ràng buộc như mô tả ở các mục sau; bảng đích nhận thêm cột của bảng gộp vào): `buoi_hoat_dong`, `dang_ky_buoi`.

| Bảng cũ | Đích | Loại ở bảng chung | Ràng buộc của bảng cũ | Cách giữ sau gộp |
| --- | --- | --- | --- | --- |
| `dong_y_ve_muon` | `ho_so_chung` | `M04.DONG_Y_VE_MUON` | `id`, `nct_id`, `buoi_id`, `nguoi_dai_dien_id`, `cach` (`CONG`, `BAN_KY`), `tep_ban_ky_id NULL`, `gio_ve_du_kien_moi`, `ghi_nhan_boi_id`, `luc`, `trang_thai` (`HIEU_LUC`, `DA_RUT`, `DA_DUNG`, `HET_HIEU_LUC`), `rut_boi`, `rut_luc`; `UNIQUE` (buổi, người cao tuổ… | `khoa_duy_nhat` |
| `diem_danh_buoi` | `su_kien_chung` | `M04.DIEM_DANH_BUOI` | `id`, `dang_ky_id`, `buoi_id`, `nct_id`, `ket_qua` (`CO_MAT`, `VANG`, `KHONG_GHI_NHAN`), `ly_do_vang` (`TU_CHOI`, `SUC_KHOE`, `DANG_VANG_MAT`, `KHONG_DEN`, `KHAC`), `muc_tham_gia` (`TICH_CUC`, `BINH_THUONG`, `THU_DONG`, `BO_GIUA_CHUNG`), `muc_giao_tiep`, `tinh… | cột vào `JSON` hoặc cột của bảng đích |
| `chuyen_di` | `buoi_hoat_dong` | — | `nhiem_vu_chuyen` (nhóm 2): `buoi_id`, `nhan_vien_id`, `vai` (`TRUONG_DOAN`, `DI_CUNG`), `tu_luc`, `den_luc NULL`, `ly_do_ket_thuc` (`CHUYEN_TRUONG_DOAN`, `DOI_CA`, `NGHI_DOT_XUAT`, `GO`); cột sinh `UNIQUE` một trưởng đoàn hiện hành mỗi chuyến. | cột sinh `UNIQUE` theo `kind` |
| `nguoi_tham_gia` | `dang_ky_buoi` | — | `id`, `buoi_id`, `nct_id`, `dang_ky_id`, `trang_thai` (`DA_DANG_KY`, `DUOC_DI`, `KHONG_DI`, `DA_ROI_VIEN`, `DA_VE`, `THIEU_KHI_VE`, `ROI_DOAN`), `ly_do`, `su_co_thieu_nguoi_id NULL` (đặt một lần), `canh_bao_thieu_nguoi_da_yeu_cau`. | `khoa_luc` + trigger khóa theo dòng |
| `danh_sach_kiem_tra` | `ho_so_chung` | `M04.DANH_SACH_KIEM_TRA` | `id`, `ca_id`, `tang_khu_vuc_id`, `lap_boi_id`, `vai_tro_lap` (`TRUONG_TANG`, `NGUOI_PHU_TRACH_CA`), `lap_luc`, `so_du_dieu_kien_luc_lap`, `da_bao_ql_qua_han_luc`; `UNIQUE (ca_id, tang_khu_vuc_id)`. | `khoa_duy_nhat` |
| `muc_kiem_tra` | `su_kien_chung` | `M04.MUC_KIEM_TRA` | `id`, `danh_sach_id`, `cong_viec_id` `UNIQUE`, `nguoi_thuc_hien_goc_id`, `trang_thai` (`CHO_KIEM_TRA`, `DAT`, `KHONG_DAT`, `KHONG_KIEM_TRA_DUOC`, `QUA_HAN_KIEM_TRA`), `kiem_tra_boi_id`, `vai_tro_kiem_tra`, `ghi_chu`, `tep_anh_ids`, `cong_viec_lam_lai_id NULL`,… | `khoa_duy_nhat` |
| `hoat_dong_nct` | `trang_thai_nct` | `M04.HOAT_DONG_NCT` | — | cột vào `JSON` hoặc cột của bảng đích |
| `chuoi_co_lap` | `trang_thai_nct` | `M04.CHUOI_CO_LAP` | — | cột vào `JSON` hoặc cột của bảng đích |
| `loai_hoat_dong` | `danh_muc_chung` | `M04.LOAI_HOAT_DONG` | — | cột vào `JSON` hoặc cột của bảng đích |
| `nhom_so_thich` | `danh_muc_chung` | `M04.NHOM_SO_THICH` | — | cột vào `JSON` hoặc cột của bảng đích |
| `loai_hoat_dong_nhom_so_thich` | `danh_muc_chung` | `M04.LOAI_HOAT_DONG_NHOM_SO_THICH` | — | cột vào `JSON` hoặc cột của bảng đích |
| `hoat_dong` | `danh_muc_chung` | `M04.HOAT_DONG` | — | cột vào `JSON` hoặc cột của bảng đích |
| `mau_lap_phien_ban` | `ho_so_chung` | `M04.MAU_LAP_PHIEN_BAN` | — | cột vào `JSON` hoặc cột của bảng đích |
| `nguoi_thuong_xuyen` | `ho_so_chung` | `M04.NGUOI_THUONG_XUYEN` | - `nguoi_thuong_xuyen` (nhóm 2): `hoat_dong_id`, `nct_id`, `tu_luc`, `den_luc NULL`, `ly_do_go`; cột sinh `UNIQUE` cho bản đang hiệu lực. | `khoa_duy_nhat` |
| `so_thich` | `ho_so_chung` | `M04.SO_THICH` | - `so_thich` (nhóm 2): `id`, `nct_id`, `nhom_id`, `mo_ta`, `muc` (`THICH`, `KHONG_THICH`), `nguon_loai` (`NGUOI_CAO_TUOI`, `NGUOI_THAN`, `NHAN_VIEN_QUAN_SAT`), `nguon_nguoi_than_id NULL` (mẫu cột nguồn 001 data-model mục 6a; sửa 2026-10-18, Q-363), `trang_thai | cột vào `JSON` hoặc cột của bảng đích |
| `nhiem_vu_chuyen` | `lich_su_chung` | `M04.NHIEM_VU_CHUYEN` | `nhiem_vu_chuyen` (nhóm 2): `buoi_id`, `nhan_vien_id`, `vai` (`TRUONG_DOAN`, `DI_CUNG`), `tu_luc`, `den_luc NULL`, `ly_do_ket_thuc` (`CHUYEN_TRUONG_DOAN`, `DOI_CA`, `NGHI_DOT_XUAT`, `GO`); cột sinh `UNIQUE` một trưởng đoàn hiện hành mỗi chuyến. | `khoa_duy_nhat` |
| `chuyen_di_gia_han` | `su_kien_chung` | `M04.CHUYEN_DI_GIA_HAN` | — | cột vào `JSON` hoặc cột của bảng đích |
| `danh_gia_kha_nang` | `su_kien_chung` | `M04.DANH_GIA_KHA_NANG` | — | cột vào `JSON` hoặc cột của bảng đích |
| `diem_danh_chuyen` | `su_kien_chung` | `M04.DIEM_DANH_CHUYEN` | — | cột vào `JSON` hoặc cột của bảng đích |
| `ca_khong_danh_sach` | `moc_da_nhac` | `M04.CA_KHONG_DANH_SACH` | — | cột vào `JSON` hoặc cột của bảng đích |
| `su_kien_m04hd_da_xu_ly` | `moc_da_nhac` | `M04.SU_KIEN_M04HD_DA_XU_LY` | — | cột vào `JSON` hoặc cột của bảng đích |
| `lan_chay_m04hd` | `moc_da_nhac` | `M04.LAN_CHAY_M04HD` | — | cột vào `JSON` hoặc cột của bảng đích |

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `hoat_dong_nct` | 2 (kỹ thuật) | Đích khóa sở thích, chuỗi cô lập (L-03) *(R-15: `hoat_dong_nct` → `trang_thai_nct`)* |
| `loai_hoat_dong`, `loai_hoat_dong_nhom_so_thich`, `nhom_so_thich` | 1 | A, I *(R-15: `loai_hoat_dong`, `loai_hoat_dong_nhom_so_thich`, `nhom_so_thich` → `danh_muc_chung`)* |
| `hoat_dong` | 1 | A *(R-15: `hoat_dong` → `danh_muc_chung`)* |
| `mau_lap_phien_ban` | 1 (có lịch sử) | B *(R-15: `mau_lap_phien_ban` → `ho_so_chung`)* |
| `nguoi_thuong_xuyen` | 2 | B *(R-15: `nguoi_thuong_xuyen` → `ho_so_chung`)* |
| `buoi_hoat_dong` | 2 | C |
| `dang_ky_buoi` | 2 | D |
| `dong_y_ve_muon` | 2 | D (FR-016a) *(R-15: `dong_y_ve_muon` → `ho_so_chung`)* |
| `diem_danh_buoi` | 3 | F *(R-15: `diem_danh_buoi` → `su_kien_chung`)* |
| `chuyen_di`, `chuyen_di_gia_han`, `nhiem_vu_chuyen` | 2; 3; 2 | G, H *(R-15: `chuyen_di` → `buoi_hoat_dong`)* |
| `nguoi_tham_gia` | 2 | G, H *(R-15: `nguoi_tham_gia` → `dang_ky_buoi`)* |
| `danh_gia_kha_nang` | 3 | G (FR-036) *(R-15: `danh_gia_kha_nang` → `su_kien_chung`)* |
| `diem_danh_chuyen` | 3 | H *(R-15: `diem_danh_chuyen` → `su_kien_chung`)* |
| `so_thich` | 2 | I *(R-15: `so_thich` → `ho_so_chung`)* |
| `chuoi_co_lap` | Kỹ thuật | I (FR-054) *(R-15: `chuoi_co_lap` → `trang_thai_nct`)* |
| `danh_sach_kiem_tra`, `muc_kiem_tra`, `ca_khong_danh_sach` | 2; 2 → 3 khi có kết quả; 3 | J *(R-15: `danh_sach_kiem_tra` → `ho_so_chung`; `muc_kiem_tra` → `su_kien_chung`)* |
| `su_kien_m04hd_da_xu_ly`, `lan_chay_m04hd` | Kỹ thuật | Listener, job *(R-15: `su_kien_m04hd_da_xu_ly`, `lan_chay_m04hd` → `moc_da_nhac`)* |

---

## 2. Danh mục, hoạt động, mẫu (mục A, B)

- `loai_hoat_dong`: `id`, `ten`, `hoat_dong_nhom`, `ngoai_vien`, `trang_thai`. `nhom_so_thich`: `id`, `ten`, `trang_thai`. `loai_hoat_dong_nhom_so_thich`: cặp khóa. *(R-15: `loai_hoat_dong`, `nhom_so_thich`, `loai_hoat_dong_nhom_so_thich` → `danh_muc_chung`)*
- `hoat_dong`: `id`, `ten`, `loai_id`, `pham_vi` (`TANG` + `tang_khu_vuc_id` | `TOAN_VIEN`), `tao_boi_truong_tang_id`, `dia_diem_mac_dinh` (phòng/khu vực của 003, hoặc mô tả điểm đến), `nguoi_phu_trach_mac_dinh_id`, `thoi_luong_phut`, `so_luong_toi_da`, `doi_tuong` `JSON` (mức chăm sóc, loại lưu trú, tầng mời), `co_thu_phi`, `dich_vu_id NULL` (bắt buộc khi có thu phí), `hoat_dong_nhom` (mặc định theo loại; đổi kèm lý do), `trang_thai`, `ngung_tu NULL`. *(R-15: `hoat_dong` → `danh_muc_chung`)*
- `mau_lap_phien_ban`: `hoat_dong_id`, `ap_dung_tu`, `ngay_trong_tuan` hoặc `chu_ky_ngay`, `gio_bat_dau`, `gio_ket_thuc`, `ngay_bat_dau`, `ngay_ket_thuc NULL`, `ly_do`, `boi_id`. *(R-15: `mau_lap_phien_ban` → `ho_so_chung`)*
- `nguoi_thuong_xuyen` (nhóm 2): `hoat_dong_id`, `nct_id`, `tu_luc`, `den_luc NULL`, `ly_do_go`; cột sinh `UNIQUE` cho bản đang hiệu lực. *(R-15: `nguoi_thuong_xuyen` → `ho_so_chung`)*

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

### `dong_y_ve_muon` – nhóm 2 → gộp theo R-15: `dong_y_ve_muon` → `ho_so_chung`

`id`, `nct_id`, `buoi_id`, `nguoi_dai_dien_id`, `cach` (`CONG`, `BAN_KY`), `tep_ban_ky_id NULL`, `gio_ve_du_kien_moi`, `ghi_nhan_boi_id`, `luc`, `trang_thai` (`HIEU_LUC`, `DA_RUT`, `DA_DUNG`, `HET_HIEU_LUC`), `rut_boi`, `rut_luc`; `UNIQUE` (buổi, người cao tuổi) cho bản Hiệu lực.

---

## 5. `diem_danh_buoi` – nhóm 3 (mục F) → gộp theo R-15: `diem_danh_buoi` → `su_kien_chung`

`id`, `dang_ky_id`, `buoi_id`, `nct_id`, `ket_qua` (`CO_MAT`, `VANG`, `KHONG_GHI_NHAN`), `ly_do_vang` (`TU_CHOI`, `SUC_KHOE`, `DANG_VANG_MAT`, `KHONG_DEN`, `KHAC`), `muc_tham_gia` (`TICH_CUC`, `BINH_THUONG`, `THU_DONG`, `BO_GIUA_CHUNG`), `muc_giao_tiep`, `tinh_trang_sau`, `ghi_chu`, `ghi_nhan_muon`, `boi_id`, `luc`, `thoi_diem_thiet_bi NULL`, `tang_khu_vuc_id` (tầng gắn đính chính); `UNIQUE (dang_ky_id)` cho bản hiện hành (đính chính theo 000).

---

## 6. Chuyến đi (mục G, H)

### `chuyen_di` – nhóm 2 → gộp theo R-15: `chuyen_di` → `buoi_hoat_dong`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `buoi_id` (khóa chính, trỏ `buoi_hoat_dong`) | | FR-034 |
| `diem_den`, `roi_du_kien`, `ve_du_kien` (hiện hành), `phuong_tien`, `xe_cua_vien`, `xe_mo_ta` | | FR-034, FR-040 |
| `trang_thai` (`DA_LEN_LICH`, `DANG_DI`, `DA_VE`, `DA_HUY`) | | Bảng trạng thái chuyến |
| `qua_gio_ve`, `da_bao_qua_gio_cho_moc`, `da_bao_ql_cho_moc` | Một lần mỗi mốc giờ về | FR-049, FR-049a |
| `truong_doan_tam_tu_xa` | | FR-035a |
| `ket_thuc_ve_luc`, `ket_thuc_ve_boi_id` | | FR-047, FR-050 |

`chuyen_di_gia_han` (nhóm 3): `buoi_id`, `ve_du_kien_cu`, `moi`, `ly_do`, `boi_id`, `luc`. *(R-15: `chuyen_di_gia_han` → `su_kien_chung`)*

`nhiem_vu_chuyen` (nhóm 2): `buoi_id`, `nhan_vien_id`, `vai` (`TRUONG_DOAN`, `DI_CUNG`), `tu_luc`, `den_luc NULL`, `ly_do_ket_thuc` (`CHUYEN_TRUONG_DOAN`, `DOI_CA`, `NGHI_DOT_XUAT`, `GO`); cột sinh `UNIQUE` một trưởng đoàn hiện hành mỗi chuyến. *(R-15: `nhiem_vu_chuyen` → `lich_su_chung`)*

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Tạo từ mẫu / buổi lẻ | `DA_LEN_LICH` | Trưởng tầng |
| `DA_LEN_LICH` | Dời / Hủy | `DA_LEN_LICH` / `DA_HUY` | Trưởng tầng; Hệ thống (đổi mẫu, ngừng) |
| `DA_LEN_LICH` | Điểm danh rời viện người đầu tiên | `DANG_DI` | Trưởng đoàn; Trưởng tầng thay |
| `DANG_DI` | Rời viện đi sau; gia hạn; chuyển trưởng đoàn; về từng phần; báo thiếu người; quá giờ | `DANG_DI` | Theo bảng của spec |
| `DANG_DI` | Hủy ghi nhận bản ghi rời viện cuối cùng | `DA_LEN_LICH` | Trưởng tầng |
| `DANG_DI` | Kết thúc điểm danh về | `DA_VE` | Trưởng đoàn, Trưởng tầng |
| `DA_VE` | Điểm danh về muộn | `DA_VE` | Trưởng đoàn, Trưởng tầng |

### `nguoi_tham_gia` – nhóm 2 → gộp theo R-15: `nguoi_tham_gia` → `dang_ky_buoi`

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

`danh_gia_kha_nang` (nhóm 3): `nguoi_tham_gia_id`, `ket_qua` (`DAT`, `KHONG_DAT`), `ly_do`, `boi_id` (Trưởng tầng của tầng người cao tuổi), `luc`, `hien_thi` `JSON` (thông tin đã hiển thị), `het_hieu_luc_luc NULL` (dời sang ngày khác). *(R-15: `danh_gia_kha_nang` → `su_kien_chung`)*

`diem_danh_chuyen` (nhóm 3): `id`, `nguoi_tham_gia_id`, `loai` (`ROI_VIEN`, `VE`), `thoi_diem`, `boi_id`, `thay_truong_doan_ly_do NULL`, `thoi_diem_thiet_bi NULL`. Đính chính (`M04.DIEM_DANH_CHUYEN`) chỉ Trưởng tầng của chuyến; không đổi người, loại. *(R-15: `diem_danh_chuyen` → `su_kien_chung`)*

---

## 7. Tinh thần (mục I)

- `hoat_dong_nct`: `nct_id`, `phien_ban`. *(R-15: `hoat_dong_nct` → `trang_thai_nct`)*
- `so_thich` (nhóm 2): `id`, `nct_id`, `nhom_id`, `mo_ta`, `muc` (`THICH`, `KHONG_THICH`), `nguon_loai` (`NGUOI_CAO_TUOI`, `NGUOI_THAN`, `NHAN_VIEN_QUAN_SAT`), `nguon_nguoi_than_id NULL` (mẫu cột nguồn 001 data-model mục 6a; sửa 2026-10-18, Q-363), `trang_thai` (`HIEN_HANH`, `DA_THAY`, `DA_BO`), `boi_id`, `luc`, `ly_do_bo`, `thay_boi_id NULL`; cột sinh `khoa_hien_hanh` theo (người cao tuổi, nhóm, nguồn) `UNIQUE` (L-09). *(Bổ sung 2026-10-18)* Thêm `phan_hoi_id NULL` (001 FR-023g, research L-16). *(R-15: `so_thich` → `ho_so_chung`)*
- `chuoi_co_lap` (kỹ thuật): `nct_id`, `so_ngay_tinh`, `ngay_bat_dau_chuoi`, `ngay_xet_cuoi`, `da_yeu_cau_canh_bao_cho_chuoi`. *(R-15: `chuoi_co_lap` → `trang_thai_nct`)*

---

## 8. Kiểm tra chất lượng (mục J)

### `danh_sach_kiem_tra` – nhóm 2 → gộp theo R-15: `danh_sach_kiem_tra` → `ho_so_chung`

`id`, `ca_id`, `tang_khu_vuc_id`, `lap_boi_id`, `vai_tro_lap` (`TRUONG_TANG`, `NGUOI_PHU_TRACH_CA`), `lap_luc`, `so_du_dieu_kien_luc_lap`, `da_bao_ql_qua_han_luc`; `UNIQUE (ca_id, tang_khu_vuc_id)`.

### `muc_kiem_tra` – nhóm 2 → 3 khi có kết quả → gộp theo R-15: `muc_kiem_tra` → `su_kien_chung`

`id`, `danh_sach_id`, `cong_viec_id` `UNIQUE`, `nguoi_thuc_hien_goc_id`, `trang_thai` (`CHO_KIEM_TRA`, `DAT`, `KHONG_DAT`, `KHONG_KIEM_TRA_DUOC`, `QUA_HAN_KIEM_TRA`), `kiem_tra_boi_id`, `vai_tro_kiem_tra`, `ghi_chu`, `tep_anh_ids`, `cong_viec_lam_lai_id NULL`, `luc`. Trigger: khi `OLD.trang_thai <> 'CHO_KIEM_TRA'` chặn mọi đổi (đính chính theo 000).

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập danh sách; thêm mục | `CHO_KIEM_TRA` | Người kiểm tra |
| `CHO_KIEM_TRA` | Ghi Đạt / Không đạt (kèm công việc làm lại) / Không kiểm tra được | `DAT` / `KHONG_DAT` / `KHONG_KIEM_TRA_DUOC` | Người kiểm tra (FR-066) |
| `CHO_KIEM_TRA` | Công việc bị Hủy ghi nhận | `KHONG_KIEM_TRA_DUOC` | Hệ thống |
| `CHO_KIEM_TRA` | Hết ca | `QUA_HAN_KIEM_TRA` | Hệ thống |

`ca_khong_danh_sach` (nhóm 3): `ca_id`, `tang_khu_vuc_id`, `so_du_dieu_kien`, `ly_do` (`CA_KHONG_CO_NGUOI_KIEM_TRA`, `KHONG_LAP`), `luc`. *(R-15: `ca_khong_danh_sach` → `moc_da_nhac`)*

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
| BR-M04-22 | `chuoi_co_lap`; job 00:00 *(R-15: `chuoi_co_lap` → `trang_thai_nct`)* |
| BR-M04-23 | `UNIQUE (ca_id, tang_khu_vuc_id)`, `UNIQUE (cong_viec_id)`; kiểm tỷ lệ khi lập |
