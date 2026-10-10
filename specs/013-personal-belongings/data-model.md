# Data Model: Quản lý đồ gửi của người cao tuổi (013)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Người cao tuổi, người thân, nhân viên, tầng/khu, sự cố, tệp, yêu cầu phê duyệt là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Số lượng `DECIMAL(12,2)`; số tiền `BIGINT` (đồng). Thời điểm `DATETIME(6)` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `do_gui_nct` | 2 (kỹ thuật) | Đích khóa theo người cao tuổi (K-03) |
| `loai_do`, `loai_do_lich_su_dau` | 1; 3 | A (FR-001) |
| `muc_tinh_trang` | 1 | A (FR-001a) |
| `vi_tri_luu_giu` | 1 | A (FR-002) |
| `do_gui` | 2 | B, D |
| `ban_giao_do_gui`, `ban_giao_anh` | 3 | B, C, D |
| `lan_tra`, `lan_chan_tra` | 3 | E |
| `xac_nhan_nguoi_nhan`, `xac_nhan_nguoi_nhan_do`, `bang_chung_lien_he` | 2; 2; 3 | E (FR-017, FR-017a) |
| `dong_y_tu_giu` | 2 | D (FR-015a) |
| `phieu_kiem_ke`, `phieu_kiem_ke_dong`, `do_khong_ro_chu` | 2 → 3 khi Hoàn thành | H2 |
| `su_kien_m12_da_xu_ly`, `lan_chay_m12` | Kỹ thuật | Listener, job |

Đề nghị xử lý đồ không người nhận dùng `yeu_cau_phe_duyet` của 000 (loại `M12.XU_LY_DO_KHONG_NGUOI_NHAN`, nội dung JSON); không có bảng riêng.

---

## 2. Danh mục (mục A)

- `do_gui_nct`: `nct_id` (khóa chính), `phien_ban`.
- `loai_do`: `id`, `ten`, `co_so_tien`, `can_dong_y_giao_su_dung`, `trang_thai`. Không xóa khi đã có đồ tham chiếu. Dấu "đồ có giá trị" không lưu ở đây (CFG-M12-01).
- `loai_do_lich_su_dau` (nhóm 3): `loai_do_id`, `gia_tri`, `ly_do`, `boi_id`, `luc`.
- `muc_tinh_trang`: `id`, `ten`, `thu_tu`, `khong_dung_duoc` (đúng một mức cuối: cột sinh `UNIQUE`), `trang_thai`.
- `vi_tri_luu_giu`: `id`, `ten`, `mo_ta`, `tang_khu_vuc_id NULL`, `trang_thai`. Ngừng bị chặn khi còn đồ Đang giữ hoặc Hư hỏng.

---

## 3. `do_gui` – nhóm 2 (mục B, D)

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `loai_do_id`, `mo_ta` | | FR-003 |
| `so_luong DECIMAL NULL`, `so_tien BIGINT NULL`, `don_vi` | Đúng một trong hai theo `co_so_tien` | FR-003 |
| `do_goc_id NULL`, `tiep_nhan_goc_id` | Đồ tách trỏ đồ gốc và bản ghi Tiếp nhận gốc | FR-004, FR-012 |
| `trang_thai` (`DANG_GIU`, `DANG_SU_DUNG`, `DA_TRA`, `THAT_LAC`, `HU_HONG`, `DA_XU_LY`) | Suy ra từ `ban_giao_hien_hanh_id` | FR-011 |
| `nguoi_giu_loai` (`NHAN_VIEN`, `NGUOI_CAO_TUOI`, `NULL` khi Thất lạc/cuối), `nguoi_giu_id` | Suy ra | FR-011 |
| `vi_tri_id NULL`, `muc_tinh_trang_id` | Suy ra | FR-011 |
| `ban_giao_hien_hanh_id`, `ban_giao_hien_hanh_luc` | | SC-008 |
| `bi_loai` | Hủy ghi nhận Tiếp nhận không có bản ghi sau | FR-011a |
| `moc_tinh_xu_ly NULL` | Thời điểm Tìm thấy sau khi mục Hoàn thành/hồ sơ Đã đóng | FR-026a |
| `nhac_cuoi_luc` | CFG-M12-02 | FR-027 |
| `phien_ban` | So trong mỗi lệnh | FR-011b |

Chỉ mục `(nct_id, trang_thai)`, `(nguoi_giu_loai, nguoi_giu_id, trang_thai)`, `(vi_tri_id, trang_thai)`.

**Bảng chuyển** (khớp bảng trạng thái đồ gửi):

| Từ | Lệnh | Đến | Người |
| --- | --- | --- | --- |
| — | Tiếp nhận / Tiếp nhận và giao ngay | `DANG_GIU` / `DANG_SU_DUNG` | Hành chính, Điều dưỡng (phạm vi) |
| `DANG_GIU` | Giao sử dụng | `DANG_SU_DUNG` | Hành chính, Điều dưỡng |
| `DANG_SU_DUNG` | Thu lại | `DANG_GIU` | Hành chính, Điều dưỡng |
| `DANG_GIU`, `HU_HONG` | Chuyển giữ | giữ nguyên | Hành chính, Điều dưỡng |
| `DANG_GIU`, `DANG_SU_DUNG`, `HU_HONG` | Trả | `DA_TRA` | Hành chính; Điều dưỡng (đồ không có giá trị, K-06) |
| `DANG_GIU`, `DANG_SU_DUNG`, `HU_HONG` | Báo thất lạc | `THAT_LAC` | Hành chính, Điều dưỡng |
| `DANG_GIU`, `DANG_SU_DUNG` | Ghi hư hỏng | `HU_HONG` | Hành chính, Điều dưỡng |
| `THAT_LAC` | Tìm thấy | `DANG_GIU` | Hành chính, Điều dưỡng |
| `DANG_GIU`, `HU_HONG` | Áp dụng đề nghị xử lý | `DA_XU_LY` | (kèm áp dụng yêu cầu đã duyệt) |

---

## 4. `ban_giao_do_gui` – nhóm 3

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `do_gui_id`, `loai`, `trang_thai_dich` | | FR-011 |
| `nguoi_giao_loai`, `nguoi_giao_id NULL`, `nguoi_giao_ten`, `nguoi_giao_giay_to` | Trống với Tìm thấy | FR-003, FR-014 |
| `nguoi_nhan_loai`, `nguoi_nhan_id NULL`, `nguoi_nhan_ten` | Trống với Báo thất lạc; hình thức xử lý với Xử lý | FR-011 |
| `thoi_diem`, `so_luong`, `so_tien`, `muc_tinh_trang_id`, `mo_ta_tinh_trang`, `vi_tri_id NULL` | Vị trí bắt buộc khi đích `DANG_GIU`, `HU_HONG` | FR-011 |
| `ly_do NULL`, `nguoi_giu_vang_mat` | Bắt buộc với Chuyển giữ, Báo thất lạc, Ghi hư hỏng | FR-011, FR-014 |
| `la_do_co_gia_tri` | CFG-M12-01 lúc lệnh | FR-009 |
| `ghi_nhan_boi_id`, `ghi_luc` | | FR-011 |
| `lan_tra_id NULL`, `xac_nhan_nguoi_nhan_id NULL` | Lệnh Trả | FR-016 → FR-019 |
| `su_co_id NULL` | Đặt trong lệnh hoặc liên kết sau (một lần) | FR-021, FR-024 |
| `yeu_cau_phe_duyet_id NULL`, `don_vi_tiep_nhan NULL` | Lệnh Xử lý | FR-027a |
| `phieu_kiem_ke_id NULL` | Phát sinh khi hoàn thành kiểm kê | FR-028b |
| `ghi_chu` | "thu từ người cao tuổi" | FR-018 |

Trigger: chặn `UPDATE` ngoài `su_co_id` từ `NULL`; chặn `DELETE`. Đính chính theo 000 (`M12.BAN_GIAO_DO_GUI`, K-05). `ban_giao_anh`: `ban_giao_id`, `tep_id`, `bo_sung_boi_dinh_chinh_id NULL`.

---

## 5. Trả (mục E)

### `lan_tra` – nhóm 3

`id`, `nct_id`, `nguoi_nhan_loai` (`NGUOI_THAN`, `NGUOI_CAO_TUOI`, `NGUOI_KHAC`), `nguoi_than_id NULL`, `ho_ten`, `giay_to_xuat_trinh` `JSON`, `ket_qua_xac_minh` (`KHOP`, `CHUA_CO_GIAY_TO_DA_DOI_CHIEU`), `can_cu` (`CO_QUYEN_NHAN`, `NCT_NGOAI_LE_Q253`, `XAC_NHAN_NGUOI_NHAN_KHAC`), `xac_nhan_nguoi_nhan_id NULL`, `chu_ky_tep_id`, `ly_do_khong_ky NULL`, `y_kien_tinh_trang NULL`, `thuc_hien_boi_id`, `luc`.

### `lan_chan_tra` – nhóm 3

`nct_id`, `nguoi_nhan_khai` `JSON`, `ly_do` (`GIAY_TO_KHONG_KHOP`, `KHONG_CO_QUYEN_NHAN`, `XAC_NHAN_KHONG_HOP_LE`, `DIEU_DUONG_DO_CO_GIA_TRI`), `boi_id`, `luc`.

### `xac_nhan_nguoi_nhan` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `nguoi_nhan_ten`, `nguoi_nhan_giay_to`, `la_nguoi_cao_tuoi` | Khóa sau khi lập (trigger) | FR-017 |
| `lap_boi_id`, `lap_luc` | Hành chính | FR-017 |
| `trang_thai` (`CHO_XAC_NHAN`, `HIEU_LUC`, `TU_CHOI`, `HUY`, `DA_DUNG`, `HET_HAN`) | | FR-017b |
| `cach` (`CONG`, `BAN_KY`, `QL_DUYET_THAY`), `xac_nhan_boi`, `xac_nhan_luc`, `tep_ban_ky_id NULL` | | FR-017 |
| `can_cu_duyet_thay` (`A_KHONG_LIEN_HE_DUOC`, `B_KHONG_CO_DAI_DIEN`) `NULL`, `ly_do_duyet_thay` | | FR-017a |
| `da_tu_choi` | Chặn duyệt thay sau từ chối | FR-017 |
| `het_han_luc`, `lan_tra_id NULL` | CFG-M10-08; dùng một lần | FR-017 |
| `da_bao_ql_luc` | Đủ điều kiện duyệt thay | FR-032 |

`xac_nhan_nguoi_nhan_do`: `xac_nhan_id`, `do_gui_id`, `bi_loai_luc NULL`. `bang_chung_lien_he` (nhóm 3): `doi_tuong` (`XAC_NHAN`, `DE_NGHI_XU_LY`), `doi_tuong_id`, `nguoi_than_id`, `kenh` (`GOI_DIEN`, `TIN_NHAN`, `THONG_BAO_CONG`), `luc`, `ket_qua`, `boi_id`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập / Lập kèm bản ký | `CHO_XAC_NHAN` / `HIEU_LUC` | Hành chính |
| `CHO_XAC_NHAN` | Xác nhận / Từ chối | `HIEU_LUC` / `TU_CHOI` | Người đại diện |
| `CHO_XAC_NHAN` | Duyệt thay | `HIEU_LUC` | Quản lý viện (FR-017a) |
| `CHO_XAC_NHAN`, `HIEU_LUC` | Hủy | `HUY` | Hành chính (lý do); Hệ thống (không còn đồ) |
| `HIEU_LUC` | Lệnh Trả dùng | `DA_DUNG` | (kèm lệnh Trả) |
| `HIEU_LUC` | Hết CFG-M10-08 | `HET_HAN` | Bộ lập lịch |

---

## 6. `dong_y_tu_giu` – nhóm 2

`id`, `do_gui_id`, `nct_id`, `nguoi_dai_dien_id`, `cach` (`CONG`, `BAN_KY`, `TRONG_LENH_TIEP_NHAN`), `tep_ban_ky_id NULL`, `luc`, `trang_thai` (`HIEU_LUC`, `DA_RUT`, `HET_HIEU_LUC`), `rut_boi_id`, `rut_luc`, `ly_do_rut`; cột sinh `khoa_hieu_luc = IF(trang_thai = 'HIEU_LUC', do_gui_id, NULL)` `UNIQUE`.

---

## 7. Kiểm kê (mục H2)

### `phieu_kiem_ke` – nhóm 2 → 3 khi `HOAN_THANH`

`id`, `vi_tri_id`, `ma_ky`, `han`, `trang_thai` (`CHO_KIEM_KE`, `HOAN_THANH`, `DA_HUY`), `tre_han`, `lap_boi_id`, `lap_luc`, `kiem_ke_boi_id`, `hoan_thanh_luc`, `ly_do_huy`; `UNIQUE (vi_tri_id, ma_ky)`; cột "đã nhắc" theo ngày ở bảng kỹ thuật `nhac_kiem_ke (vi_tri_id, ma_ky, ngay)` `UNIQUE`.

`phieu_kiem_ke_dong`: `phieu_id`, `do_gui_id`, `so_luong_luc_lap`, `muc_luc_lap`, `ket_qua` (`DUNG`, `LECH_TINH_TRANG`, `KHONG_TIM_THAY`, `BI_LOAI`) `NULL`, `muc_moi NULL`, `ghi_chu`, `ban_giao_id NULL`. `do_khong_ro_chu`: `phieu_id`, `mo_ta`, `tep_ids`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập phiếu | `CHO_KIEM_KE` | Hành chính |
| `CHO_KIEM_KE` | Hoàn thành | `HOAN_THANH` | Hành chính |
| `CHO_KIEM_KE` | Hủy (mọi đồ đã rời vị trí) | `DA_HUY` | Hành chính |
| `CHO_KIEM_KE` | Quá hạn | giữ nguyên, `tre_han` | Bộ lập lịch |

---

## 8. DBR và quy tắc

| Mã | Cách bảo đảm |
| --- | --- |
| DBR-22 | Mỗi đồ truy về đúng một bản ghi `TIEP_NHAN` qua `tiep_nhan_goc_id`; lệnh Tiếp nhận tạo đồ và bản ghi cùng transaction; bất biến tổng số lượng kiểm cuối mỗi lệnh và đính chính |
| DBR-23 | Nhật ký mọi lệnh, đính chính (nhóm "đặc biệt quan trọng") |
| DBR-25 | Thời điểm, kỳ kiểm kê theo Asia/Ho_Chi_Minh |
| BR-M12-01 | Trigger nhóm 3 trên `ban_giao_do_gui`, `lan_tra`; trạng thái đồ chỉ đổi qua lệnh |
| BR-M12-03 | `DieuKienDoGui` → `KT_B_DO_GUI` |
| BR-M12-04 | `NguoiNhanHopLe`; `xac_nhan_nguoi_nhan` một lần dùng |
| BR-M12-05 | Kiểm ảnh trong lệnh theo `la_do_co_gia_tri` |
| BR-M12-06 (Q-155) | `khoa_hieu_luc` `UNIQUE` của đồng ý; Giao sử dụng kiểm đồng ý Hiệu lực |
| BR-M12-07 | `UNIQUE (vi_tri_id, ma_ky)`; job nhắc |
| BR-M12-08 | Handler `M12.XU_LY_DO_KHONG_NGUOI_NHAN` (K-09) |
| BR-M12-09 | `QuyenNguoiThanApi.xemDoGui` mỗi lần gọi REST cổng (K-12) |
