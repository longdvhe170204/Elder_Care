# Data Model: Đổi ca và nghỉ đột xuất (015)

> **Sửa 2026-10-29 (Q-397):** bỏ mẫu xoay ca, nhóm xoay ca, lập lịch từ mẫu, phủ tối thiểu, thiếu ca, cảnh báo thiếu phủ, lời mời nhận ca; 015 chỉ còn đổi ca và nghỉ đột xuất. Nghỉ "theo ngày" mở cho mọi nhân viên, người lập chọn phạm vi. Phần dưới còn nhắc các phần đã bỏ thì bỏ qua; spec là căn cứ.

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bảng nằm trong schema M09 cùng bảng của 008; `ca`, `lich_ca`, `mau_ca`, `nhan_vien`, `phan_cong`, `ca_nhan_vien` là bảng của 008 (015 chỉ ghi qua port, research X-03). Yêu cầu đổi ca, nghỉ đột xuất là loại yêu cầu của `yeu_cau_phe_duyet` (000) kèm bảng nghiệp vụ dưới đây. Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- `ho_so_chung`: `yeu_cau_doi_ca`, `yeu_cau_nghi` (máy trạng thái hai bước ở domain của module; `khoa_duy_nhat` thay `yeu_cau_ca_mo`: một ca không nằm trong hai yêu cầu đang mở).
- `su_kien_chung`: `nghi_theo_ngay`.
- Còn 0 bảng riêng của feature.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `yeu_cau_doi_ca` | 2 | D *(R-15: `yeu_cau_doi_ca` → `ho_so_chung`)* |
| `yeu_cau_nghi`, `yeu_cau_nghi_muc` | 2 | E *(R-15: `yeu_cau_nghi` → `ho_so_chung`)* |
| `nghi_theo_ngay` | 3 | E (FR-026a) *(R-15: `nghi_theo_ngay` → `su_kien_chung`)* |
| `yeu_cau_ca_mo` | Kỹ thuật | D, E (một yêu cầu mở mỗi ca, nhân viên) *(R-15: `yeu_cau_ca_mo` → `khoa_duy_nhat` của `ho_so_chung`)* |
| `lan_chay_m09xc` | Kỹ thuật | Job |

Cột 015 trên bảng 008 (`ca_nhan_vien.nghi_co_duyet_yeu_cau_id`, `can_cu_vao_ca`, `can_cu_roi_ca`, `roi_ca_luc`) đã khai ở data-model 008.

---

## 2. Mẫu, nhóm (mục A) *(Bỏ theo Q-397, 2026-10-29)*

## 3. Lập lịch, phủ (mục A, B, C) *(Bỏ theo Q-397, 2026-10-29)*

---

## 4. `yeu_cau_doi_ca` – nhóm 2 (mục D) → gộp theo R-15: `yeu_cau_doi_ca` → `ho_so_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `yeu_cau_id` (khóa chính, trỏ `yeu_cau_phe_duyet` loại `M09.DOI_CA`) | | FR-022 |
| `dang` (`HAI_CHIEU`, `NHAN_THAY`), `nguoi_nhuong_id` (A), `ca_x_id`, `nguoi_nhan_id` (B), `ca_y_id NULL` | | FR-019 |
| `ly_do` (`LY_DO_NHAN_SU`), `ly_do_tu_choi_nguoi_nhan NULL` | Chỉ A thấy lý do từ chối của B | FR-045 |
| `trang_thai` (`CHO_NGUOI_NHAN_DONG_Y`, `CHO_DUYET`, `DA_AP_DUNG`, `NGUOI_NHAN_TU_CHOI`, `TU_CHOI`, `DA_HUY`, `HET_HIEU_LUC`) | Đồng bộ với `yeu_cau_phe_duyet` qua `chuyenMoRong` | FR-022 |
| `kiem_tra_lap`, `kiem_tra_dong_y`, `kiem_tra_duyet` `JSON` | Kết quả FR-021 từng bước | FR-021 |
| `nguoi_duyet_id`, `nguoi_phu_trach_chon_x NULL`, `nguoi_phu_trach_chon_y NULL`, `tu_duyet` | | FR-023, FR-024, Q-10 |
| `moc_bat_dau_som_nhat`, `da_nhac_luc` | CFG-M09-11 | FR-025 |

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập | `CHO_NGUOI_NHAN_DONG_Y` | A |
| `CHO_NGUOI_NHAN_DONG_Y` | Đồng ý / Từ chối | `CHO_DUYET` / `NGUOI_NHAN_TU_CHOI` | B |
| `CHO_DUYET` | Rút đồng ý | `NGUOI_NHAN_TU_CHOI` | B |
| `CHO_NGUOI_NHAN_DONG_Y`, `CHO_DUYET` | Hủy | `DA_HUY` | A |
| `CHO_DUYET` | Duyệt / Từ chối | `DA_AP_DUNG` / `TU_CHOI` | Trưởng tầng được giao; Quản lý viện (ca toàn viện) |
| `CHO_NGUOI_NHAN_DONG_Y`, `CHO_DUYET` | Tới giờ ca sớm hơn; ca bị hủy; A/B không còn thỏa | `HET_HIEU_LUC` | Hệ thống |

---

## 5. Nghỉ đột xuất (mục E)

### `yeu_cau_nghi` – nhóm 2 → gộp theo R-15: `yeu_cau_nghi` → `ho_so_chung`

`yeu_cau_id` (khóa chính, loại `M09.NGHI_DOT_XUAT`), `dang` (`THEO_CA`, `THEO_NGAY`), `nhan_vien_id`, `pham_vi_loai`, `tang_khu_vuc_id NULL`, `lap_boi_id`, `lap_thay`, `ly_do` (`LY_DO_NHAN_SU`), `trang_thai` (`CHO_DUYET`, `DA_AP_DUNG`, `DA_AP_DUNG_MOT_PHAN`, `TU_CHOI`, `DA_HUY`, `HET_HIEU_LUC`), `nguoi_duyet_id`, `tu_duyet`, `moc_som_nhat`, `da_nhac_luc`.

`yeu_cau_nghi_muc`: `yeu_cau_id`, `ca_id NULL`, `ngay NULL` (đúng một), `ket_qua` (`CHO`, `DUYET`, `TU_CHOI`, `DA_TACH`, `DA_ROI`), `ly_do_tu_choi`, `nguoi_phu_trach_moi_id NULL`, `ghi_chu` (gồm "xếp lại dù đã duyệt nghỉ" kèm người, lý do).

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập | `CHO_DUYET` | Nhân viên; Trưởng tầng, Quản lý viện (lập thay) |
| `CHO_DUYET` | Duyệt tất cả / một phần / Từ chối tất cả | `DA_AP_DUNG` / `DA_AP_DUNG_MOT_PHAN` / `TU_CHOI` | Người duyệt |
| `CHO_DUYET` | Hủy | `DA_HUY` | Nhân viên; người lập thay |
| `CHO_DUYET` | Mọi ca/ngày đã rời; nghỉ việc | `HET_HIEU_LUC` | Hệ thống |
| `CHO_DUYET` (theo ngày) | Lịch tháng công bố | `CHO_DUYET` (theo ca) | Hệ thống |

### `nghi_theo_ngay` – nhóm 3 → gộp theo R-15: `nghi_theo_ngay` → `su_kien_chung`

`nhan_vien_id`, `ngay`, `yeu_cau_id`; `UNIQUE (nhan_vien_id, ngay)`.

### `yeu_cau_ca_mo` – kỹ thuật → gộp theo R-15: `yeu_cau_ca_mo` → `khoa_duy_nhat` của `ho_so_chung`

`ca_id`, `nhan_vien_id`, `yeu_cau_id`; `UNIQUE (ca_id, nhan_vien_id)`. Ghi khi lập; xóa khi yêu cầu vào trạng thái cuối hoặc mục rời yêu cầu.

---

## 6. Thiếu phủ và lời mời (mục F) *(Bỏ theo Q-397, 2026-10-29)*

---

## 7. DBR và quy tắc

| Mã | Cách bảo đảm |
| --- | --- |
| DBR-21 (008) | Thêm vào ca qua `CaLenhPort.themVaoCa` (kiểm chồng giờ dưới khóa `nhan_vien`) |
| DBR-25 | Giờ ca theo Asia/Ho_Chi_Minh |
| BR-M09-10 | Vòng đời đổi ca; `KiemTraDoiCa` lúc duyệt |
| Một yêu cầu mở mỗi (ca, nhân viên) | `yeu_cau_ca_mo` `UNIQUE` *(R-15: `yeu_cau_ca_mo` → `khoa_duy_nhat` của `ho_so_chung`)* |
| Nhóm 3 (000) | Trigger trên `nghi_theo_ngay` *(R-15: `nghi_theo_ngay` → `su_kien_chung`)* |
