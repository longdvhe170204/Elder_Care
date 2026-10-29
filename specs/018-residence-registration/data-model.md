# Data Model: Khai báo tạm trú, lưu trú (018)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bảng nằm trong schema M02 cùng bảng của 004; người cao tuổi, hợp đồng, phụ lục, tệp là tham chiếu logic. Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- `ho_so_chung`: `khai_bao_tam_tru` (loại `M02.RESIDENCE_DECLARATION`, trạng thái và kết quả khai báo trong `trang_thai`, `noi_dung`; cột sinh có chỉ mục cho hạn khai báo nếu job quét) *(sửa 2026-10-29, mục tiêu 79 bảng)*.
- `trang_thai_nct`: `khai_bao_nct`, `nghia_vu_khai_bao` (trong `du_lieu`).
- Không còn bảng riêng.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `khai_bao_nct` | 2 (kỹ thuật) | Đích khóa; cờ thường trú cùng xã/phường của hồ sơ *(R-15: `khai_bao_nct` → `trang_thai_nct`)* |
| `khai_bao_tam_tru` | 2 (nội dung ghi là nhóm 3) | FR-003 → FR-005 *(R-15: `khai_bao_tam_tru` → `ho_so_chung`)* |
| `khai_bao_lan_nop` | 3 | FR-003 (mỗi lần nộp, kết quả, từ chối) |
| `nghia_vu_khai_bao` | Kỹ thuật (đệm số liệu tính) | FR-001, FR-001a, FR-002, FR-006, FR-007 *(R-15: `nghia_vu_khai_bao` → `trang_thai_nct`)* |
| `nhac_khai_bao` | Kỹ thuật | Chống nhắc trùng |

---

## 2. `khai_bao_nct` → gộp theo R-15: `khai_bao_nct` → `trang_thai_nct`

`nct_id` (khóa chính), `thuong_tru_cung_xa`, `ghi_chu_can_cu`, `xac_nhan_boi_id`, `xac_nhan_luc`, `phien_ban`.

---

## 3. `khai_bao_tam_tru` → gộp theo R-15: `khai_bao_tam_tru` → `ho_so_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `loai` (`DANG_KY_TAM_TRU`, `THONG_BAO_LUU_TRU`), `la_gia_han` | | FR-001, FR-006 |
| `nghia_vu_id`, `han_khai_bao` | Lấy từ nghĩa vụ; từ chối → thời điểm ghi + CFG-M02-12 | FR-002 |
| `thuong_tru_cung_xa`, `ghi_chu_can_cu` | | FR-001 |
| `trang_thai` (`CAN_KHAI_BAO`, `DA_NOP`, `DA_XAC_NHAN`, `HET_HAN`, `DA_XOA`, `DUOC_THAY_THE`, `DA_HUY`) | | FR-005 |
| `lan_nop_hien_hanh_id NULL` | | FR-003 |
| `ngay_het_han NULL` | Bắt buộc khi Đăng ký tạm trú được chấp nhận | FR-003 |
| `tep_bang_chung_id NULL`, `xoa_luc`, `xoa_boi_id`, `tep_bang_chung_xoa_id` | | FR-003, FR-008 |
| `thay_the_khai_bao_id NULL` | Khai báo cũ bị thay | FR-005 |
| `khoa_dang_xu_ly` sinh `IF(trang_thai IN ('CAN_KHAI_BAO','DA_NOP'), nct_id, NULL)` `UNIQUE` | | DBR-33 |
| `khoa_xac_nhan` sinh `IF(trang_thai = 'DA_XAC_NHAN', nct_id, NULL)` `UNIQUE` | | DBR-33 |

`khai_bao_lan_nop` (nhóm 3): `khai_bao_id`, `ngay_nop`, `kenh` (`CONG_DICH_VU_CONG`, `TRUC_TIEP`), `co_quan`, `ma_ho_so`, `ket_qua` (`CHO`, `CHAP_NHAN`, `TU_CHOI`) đặt một lần, `ly_do_tu_choi`, `boi_id`, `luc`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập khai báo; lập khai báo gia hạn | `CAN_KHAI_BAO` | Hành chính |
| `CAN_KHAI_BAO` | Xác nhận thường trú cùng xã/phường | `CAN_KHAI_BAO` | Hành chính |
| `CAN_KHAI_BAO` | Ghi đã nộp | `DA_NOP` | Hành chính |
| `DA_NOP` | Ghi chấp nhận / từ chối | `DA_XAC_NHAN` / `CAN_KHAI_BAO` | Hành chính |
| `DA_XAC_NHAN` | Qua ngày hết hạn chưa có gia hạn Đã xác nhận | `HET_HAN` | Bộ lập lịch |
| `DA_XAC_NHAN`, `HET_HAN` | Ghi đã khai báo xóa (người đã Kết thúc lưu trú / Qua đời) | `DA_XOA` | Hành chính |
| `DA_XAC_NHAN` | Khai báo mới cùng người Đã xác nhận | `DUOC_THAY_THE` | Hệ thống (kèm lệnh) |
| `CAN_KHAI_BAO`, `DA_NOP` | Người cao tuổi chuyển trạng thái cuối | `DA_HUY` | Hệ thống (kèm lệnh của 001) |

---

## 4. `nghia_vu_khai_bao` – kỹ thuật → gộp theo R-15: `nghia_vu_khai_bao` → `trang_thai_nct`

`id`, `nct_id`, `moc_loai` (`HOAN_TAT_TIEP_NHAN`, `NOI_TRU_HIEU_LUC`, `PHU_LUC_GIA_HAN`, `HET_HAN`), `moc_luc`, `han`, `nguon_id` (hợp đồng / phụ lục / khai báo), `khai_bao_id NULL`, `da_nop`, `da_bao_ql_luc`, `huy_luc NULL`; `UNIQUE (nct_id, moc_loai, nguon_id)`. Dựng lại được từ dữ liệu nguồn.

`nhac_khai_bao`: `loai` (`LAP`, `QUA_HAN`, `GIA_HAN`, `XOA`), `nct_id`, `ngay`; `UNIQUE` theo cả ba cột.

---

## 5. DBR

| Mã | Cách bảo đảm |
| --- | --- |
| DBR-33 | `khoa_dang_xu_ly`, `khoa_xac_nhan` `UNIQUE`; thay thế trong cùng transaction dưới khóa `khai_bao_nct` *(R-15: `khai_bao_nct` → `trang_thai_nct`)* |
| DBR-25 | Ngày, hạn theo Asia/Ho_Chi_Minh |
