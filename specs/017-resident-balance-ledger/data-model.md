# Data Model: Số dư và thu chi của người cao tuổi (017)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bảng nằm trong schema M11 cùng bảng của 010. Người cao tuổi, hợp đồng, bảng chi phí, đề nghị mua hộ, người thân, tệp, yêu cầu phê duyệt là tham chiếu logic (`BIGINT`). Số tiền `BIGINT` (đồng, có dấu). Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `so_du` | 2 dẫn xuất | A (một dòng mỗi hồ sơ: số dư, số dư cọc, mã nộp tiền, tình trạng) |
| `giao_dich` | 2 → 3 khi Đã xác nhận | B, C |
| `day_so_phieu` | Kỹ thuật | B (FR-005) |
| `chot_quy_ngay`, `chot_quy_ngay_phieu` | 2 → 3 khi Đã xác nhận | B (FR-005a) |
| `lan_nhap_sao_ke` | 3 | B (FR-007) |
| `dong_sao_ke` | 3 nội dung, 2 trạng thái | B |
| `lich_su_tinh_trang` | 3 | D |
| `lan_xuat_so_du` | 3 | F |
| `su_kien_m11sd_da_xu_ly`, `lan_chay_m11sd` | Kỹ thuật | Listener, job |

---

## 2. `so_du` – nhóm 2 dẫn xuất

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `nct_id` (khóa chính), `mo_luc`, `hop_dong_mo_id` | Mở khi hợp đồng đầu tiên chuyển Chờ ký | FR-001 |
| `ma_nop_tien` `UNIQUE` | Không đổi, không dùng lại | FR-001, DBR-28 |
| `so_du`, `so_du_coc` | = tổng giao dịch Đã xác nhận của từng sổ | FR-002, DBR-28 |
| `tinh_trang` (`BINH_THUONG`, `SAP_HET_TIEN`, `CON_NO`), `so_ngay_con_du`, `chu_ky_tu`, `lan_bao_truoc`, `con_no_tu` | | FR-016 → FR-019 |
| `dung_nhac`, `dung_nhac_boi_id`, `dung_nhac_ly_do` | Chỉ hồ sơ trạng thái cuối còn nợ | FR-020 |
| `nhac_cuoi_luc`, `nhac_hoan_coc_luc` | CFG-M02-09 | FR-015a, FR-024 |
| `phien_ban` | Khóa giao dịch | E-03 |

Chỉ cập nhật trong transaction ghi giao dịch Đã xác nhận hoặc lần tính tình trạng.

---

## 3. `giao_dich` – nhóm 2 → 3

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `so` (`SO_DU`, `COC`) | | FR-010 |
| `loai` (`NOP_TIEN_MAT`, `NOP_CHUYEN_KHOAN`, `THANH_TOAN_BANG`, `HOAN_TIEN`, `CAN_TRU_COC`, `DIEU_CHINH`, `DAO`, `THU_COC`, `HOAN_COC`) | | FR-010 |
| `so_tien` (có dấu), `thoi_diem`, `phuong_thuc` (`TIEN_MAT`, `CHUYEN_KHOAN`, `KHAC`, `NOI_BO`) | | FR-010 |
| `so_phieu NULL`, `day_phieu NULL` (`THU`, `CHI`) | `UNIQUE (day_phieu, so_phieu)` | FR-005 |
| `dong_sao_ke_id NULL` `UNIQUE`; `bang_chi_phi_id NULL` `UNIQUE`; `giao_dich_goc_id NULL`; `cap_id NULL` | | DBR-29, DBR-30, FR-009a, FR-011, FR-015 |
| `nguoi_nop`, `nguoi_nhan` `JSON NULL`, `tep_chung_tu_id NULL`, `ghi_chu` | | FR-005, FR-014 |
| `lap_boi_id` (Kế toán / Hệ thống), `yeu_cau_phe_duyet_id NULL`, `duyet_boi_id`, `ly_do` | | FR-013 |
| `trang_thai` (`CHO_DUYET`, `DA_XAC_NHAN`, `TU_CHOI`, `DA_HUY`), `xac_nhan_luc` | | Bảng trạng thái giao dịch |
| `da_dao` | Đặt một lần | FR-011 |
| `phat_sinh_sau_chot_quy` | | FR-005a |
| `khoa_dao` sinh `IF(loai = 'DAO' AND trang_thai IN ('CHO_DUYET','DA_XAC_NHAN'), giao_dich_goc_id, NULL)` `UNIQUE` | | FR-011 |

Trigger: khi `OLD.trang_thai = 'DA_XAC_NHAN'` chỉ cho đặt `da_dao` từ false; `DELETE` bị chặn.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Phiếu thu tiền mặt; xác nhận dòng sao kê; bảng Đã chốt; cấn trừ cọc | `DA_XAC_NHAN` | Kế toán; Hệ thống (thanh toán bảng) |
| — | Lập hoàn tiền / hoàn cọc / điều chỉnh / cặp chuyển tiền / đảo | `CHO_DUYET` | Kế toán |
| `CHO_DUYET` | Duyệt / Từ chối | `DA_XAC_NHAN` / `TU_CHOI` | Quản lý viện |
| `CHO_DUYET` | Hủy | `DA_HUY` | Kế toán |

`day_so_phieu`: `loai` (khóa chính), `gia_tri_cuoi`; khóa `FOR UPDATE` khi cấp.

---

## 4. Chốt quỹ ngày

`chot_quy_ngay`: `id`, `ngay`, `tong_thu`, `tong_chi`, `phai_co`, `thuc_co`, `chenh_lech`, `ly_do NULL` (bắt buộc khi chênh lệch ≠ 0), `lap_boi_id`, `lap_luc`, `trang_thai` (`DA_LAP`, `DA_XAC_NHAN`, `BI_TRA_LAI`), `xac_nhan_boi_id`, `ly_do_tra_lai`, `bo_sung_cho_ngay NULL`; cột sinh `khoa_ngay = IF(trang_thai IN ('DA_LAP','DA_XAC_NHAN') AND bo_sung_cho_ngay IS NULL, ngay, NULL)` `UNIQUE` (DBR-35). `chot_quy_ngay_phieu`: `chot_id`, `giao_dich_id`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập chốt quỹ | `DA_LAP` | Kế toán |
| `DA_LAP` | Xác nhận / Trả lại | `DA_XAC_NHAN` / `BI_TRA_LAI` | Quản lý viện |
| `DA_XAC_NHAN` | Giao dịch tiền mặt mới cho ngày đó | `DA_XAC_NHAN` (giữ; giao dịch mang dấu) | Hệ thống |

---

## 5. Sao kê

- `lan_nhap_sao_ke`: `id`, `boi_id`, `luc`, `ten_file`, `tep_id`, `so_nhan`, `so_bo_qua`, `so_tu_choi`, `tu_choi` `JSON`.
- `dong_sao_ke`: `id`, `lan_nhap_id`, `ma_giao_dich_ngan_hang` `UNIQUE`, `thoi_diem`, `so_tien`, `noi_dung` (nội dung nhóm 3); `trang_thai` (`DA_KHOP`, `CHUA_KHOP`, `DA_GHI_NHAN`, `KHONG_THUOC_NCT`), `nct_id NULL`, `ly_do_chua_khop`, `ly_do_thao_tac`, `giao_dich_id NULL` (nhóm 2). Trigger: chặn đổi nội dung.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Nhập: đúng một mã, số tiền dương / còn lại | `DA_KHOP` / `CHUA_KHOP` | Hệ thống |
| `DA_KHOP` | Bỏ khớp | `CHUA_KHOP` | Kế toán (lý do) |
| `CHUA_KHOP` | Gán cho hồ sơ | `DA_KHOP` | Kế toán (lý do) |
| `DA_KHOP` | Xác nhận vào số dư / cọc | `DA_GHI_NHAN` | Kế toán |
| `CHUA_KHOP` | Không thuộc người cao tuổi | `KHONG_THUOC_NCT` | Kế toán (lý do) |

---

## 6. Lịch sử, xuất

- `lich_su_tinh_trang` (nhóm 3): `nct_id`, `ngay`, `so_du`, `tong_chua_chot`, `phi_mot_ngay`, `so_ngay_con_du`, `tinh_trang`, `da_bao` (`NGAY`, `NHAC_LAI`, `NOP_TIEN_DU`, `NOP_TIEN_CHUA_DU`, `KHONG`); `UNIQUE (nct_id, ngay, nguon)` với `nguon` (`HANG_NGAY`, `GIAO_DICH:<id>`).
- `lan_xuat_so_du` (nhóm 3): `boi_id`, `vai_tro`, `luc`, `loai` (`SAO_KE`, `THU_CHI`, `SO_COC`), `tu`, `den`, `pham_vi` `JSON`, `so_dong`, `tep_id`.

---

## 7. DBR và quy tắc

| Mã | Cách bảo đảm |
| --- | --- |
| DBR-17 | Giao dịch Đã xác nhận không sửa (trigger); thanh toán bảng không đảo |
| DBR-28 | `so_du` = tổng giao dịch Đã xác nhận, cập nhật cùng transaction; đối chiếu hằng đêm; mã nộp tiền `UNIQUE` |
| DBR-29 | `UNIQUE (ma_giao_dich_ngan_hang)`; `UNIQUE (dong_sao_ke_id)` trên giao dịch |
| DBR-30 | `UNIQUE (bang_chi_phi_id)` trên giao dịch thanh toán bảng |
| DBR-35 | `khoa_ngay` `UNIQUE`; dãy số phiếu cấp dưới khóa trong transaction |
| BR-M11-10 → 16 | Hàm thuần và bảng chuyển ở research E-05 → E-09 |
