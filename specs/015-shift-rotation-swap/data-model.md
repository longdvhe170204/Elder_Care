# Data Model: Lịch ca xoay vòng, phủ ca tối thiểu, đổi ca và nghỉ đột xuất (015)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bảng nằm trong schema M09 cùng bảng của 008; `ca`, `lich_ca`, `mau_ca`, `nhan_vien`, `phan_cong`, `ca_nhan_vien` là bảng của 008 (015 chỉ ghi qua port, research X-03). Yêu cầu đổi ca, nghỉ đột xuất là loại yêu cầu của `yeu_cau_phe_duyet` (000) kèm bảng nghiệp vụ dưới đây. Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `mau_xoay_ca`, `mau_xoay_ca_ngay` | 1 | A (FR-001) |
| `nhom_xoay_ca`, `thanh_vien_nhom` | 1 có lịch sử | A (FR-002, FR-003) |
| `lan_lap_lich`, `bao_cao_lap_lich_muc` | 3 | B (FR-012) |
| `muc_thieu_ca` | 2 (kỹ thuật) | A (FR-006a) |
| `lan_tinh_phu`, `lan_tinh_phu_dong` | 3 | A (FR-007) |
| `yeu_cau_doi_ca` | 2 | D |
| `yeu_cau_nghi`, `yeu_cau_nghi_muc` | 2 | E |
| `nghi_theo_ngay` | 3 | E (FR-026a) |
| `yeu_cau_ca_mo` | Kỹ thuật | D, E (một yêu cầu mở mỗi ca, nhân viên) |
| `canh_bao_thieu_phu` | 2 | F |
| `loi_moi_nhan_ca` | 2 | F |
| `moc_nhac_lich` | Kỹ thuật | B, C (FR-008, FR-015) |
| `lan_chay_m09xc` | Kỹ thuật | Job |

Cột 015 trên bảng 008 (`ca_nhan_vien.nghi_co_duyet_yeu_cau_id`, `can_cu_vao_ca`, `can_cu_roi_ca`, `roi_ca_luc`) đã khai ở data-model 008.

---

## 2. Mẫu, nhóm (mục A)

- `mau_xoay_ca`: `id`, `ten`, `loai_pham_vi` (`TANG_KHU`, `TOAN_VIEN`), `do_dai_chu_ky`, `trang_thai`. `mau_xoay_ca_ngay`: `mau_id`, `thu_tu` (0 … L−1), `mau_ca_id NULL` (`NULL` = Nghỉ); mẫu ca cùng loại phạm vi, đang hiệu lực khi lưu.
- `nhom_xoay_ca`: `id`, `ten`, `pham_vi_loai`, `tang_khu_vuc_id NULL`, `mau_xoay_ca_id`, `ngay_goc`, `thu_tu`, `da_tung_co_thanh_vien`, `trang_thai`, `ngung_tu NULL`, `ly_do_ngung`. Trigger: chặn đổi `mau_xoay_ca_id`, `ngay_goc` khi `da_tung_co_thanh_vien` (Q-190).
- `thanh_vien_nhom`: `id`, `nhom_id`, `nhan_vien_id`, `vi_tri_bat_dau` (0 … L−1), `tu_ngay`, `den_ngay NULL`. Không chồng khoảng của cùng nhân viên ở hai nhóm (kiểm dưới khóa `nhan_vien`).

---

## 3. Lập lịch, phủ (mục A, B, C)

### `lan_lap_lich` – nhóm 3

`id`, `pham_vi_loai`, `tang_khu_vuc_id NULL`, `thang`, `loai` (`LAP`, `LAP_LAI`), `ly_do NULL`, `boi_id`, `luc`, `lich_ca_id`, `so_ca`, `ket_qua` (`THANH_CONG`, `LOI`), `loi NULL`. `bao_cao_lap_lich_muc`: `lan_id`, `loai` (`BO_QUA`, `CANH_BAO_GIO_LIEN_TUC`, `CANH_BAO_DAO_TAO`, `CA_THIEU_PHU`, `THIEU_CA`, `CHUA_CO_NGUOI_PHU_TRACH`, `DUOI_NGUONG_PHUC_VU`), `nhan_vien_id NULL`, `ca_id NULL`, `ngay NULL`, `mau_ca_id NULL`, `chi_tiet` `JSON` (lý do bỏ qua "nghỉ có duyệt" không nêu lý do nghỉ).

### `muc_thieu_ca` – nhóm 2 (kỹ thuật)

`lich_ca_id`, `ngay`, `mau_ca_id`, `dong_phu_thieu` `JSON`, `trang_thai` (`MO`, `DA_CO_CA`, `DA_CONG_BO`); `UNIQUE (lich_ca_id, ngay, mau_ca_id)`. Không tạo ca rỗng.

### `lan_tinh_phu` – nhóm 3

`id`, `ca_id`, `luc`, `su_kien` (`LAP_LICH`, `LAP_LAI`, `LUU_NHAP`, `NGHI_THEO_NGAY`, `CONG_BO`, `DOI_CA`, `NGHI_DOT_XUAT`, `VANG_CA`, `BO_SUNG`, `LOI_MOI`, `NGHI_VIEC`, `GIAY_PHEP`, `CFG_M09_07`), `ket_qua` (`DAT`, `THIEU_PHU`). `lan_tinh_phu_dong`: `lan_id`, `thu_tu_dong`, `vai_tro`, `so_yeu_cau`, `so_dat`. Lần mới nhất của ca là tình trạng hiện hành (chỉ mục `(ca_id, luc)`).

### `moc_nhac_lich` – kỹ thuật

`pham_vi_loai`, `tang_khu_vuc_id`, `thang`, `loai` (`LAP_LICH`, `CONG_BO`), `ngay_nhac`; `UNIQUE` theo cả năm cột (nhắc công bố mỗi ngày một lần).

---

## 4. `yeu_cau_doi_ca` – nhóm 2 (mục D)

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

### `yeu_cau_nghi` – nhóm 2

`yeu_cau_id` (khóa chính, loại `M09.NGHI_DOT_XUAT`), `dang` (`THEO_CA`, `THEO_NGAY`), `nhan_vien_id`, `pham_vi_loai`, `tang_khu_vuc_id NULL`, `lap_boi_id`, `lap_thay`, `ly_do` (`LY_DO_NHAN_SU`), `trang_thai` (`CHO_DUYET`, `DA_AP_DUNG`, `DA_AP_DUNG_MOT_PHAN`, `TU_CHOI`, `DA_HUY`, `HET_HIEU_LUC`), `nguoi_duyet_id`, `tu_duyet`, `moc_som_nhat`, `da_nhac_luc`.

`yeu_cau_nghi_muc`: `yeu_cau_id`, `ca_id NULL`, `ngay NULL` (đúng một), `ket_qua` (`CHO`, `DUYET`, `TU_CHOI`, `DA_TACH`, `DA_ROI`), `ly_do_tu_choi`, `nguoi_phu_trach_moi_id NULL`, `ghi_chu` (gồm "xếp lại dù đã duyệt nghỉ" kèm người, lý do).

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập | `CHO_DUYET` | Nhân viên; Trưởng tầng, Quản lý viện (lập thay) |
| `CHO_DUYET` | Duyệt tất cả / một phần / Từ chối tất cả | `DA_AP_DUNG` / `DA_AP_DUNG_MOT_PHAN` / `TU_CHOI` | Người duyệt |
| `CHO_DUYET` | Hủy | `DA_HUY` | Nhân viên; người lập thay |
| `CHO_DUYET` | Mọi ca/ngày đã rời; nghỉ việc | `HET_HIEU_LUC` | Hệ thống |
| `CHO_DUYET` (theo ngày) | Lịch tháng công bố | `CHO_DUYET` (theo ca) | Hệ thống |

### `nghi_theo_ngay` – nhóm 3

`nhan_vien_id`, `ngay`, `yeu_cau_id`; `UNIQUE (nhan_vien_id, ngay)`.

### `yeu_cau_ca_mo` – kỹ thuật

`ca_id`, `nhan_vien_id`, `yeu_cau_id`; `UNIQUE (ca_id, nhan_vien_id)`. Ghi khi lập; xóa khi yêu cầu vào trạng thái cuối hoặc mục rời yêu cầu.

---

## 6. Thiếu phủ và lời mời (mục F)

### `canh_bao_thieu_phu` – nhóm 2

`id`, `ca_id`, `chi_tiet_thieu` `JSON` (vai trò, số thiếu), `mo_luc`, `dong_luc`, `trang_thai` (`DANG_MO`, `DONG_DU_PHU`, `DONG_HET_CA`), `nguoi_nhan` (Trưởng tầng được giao / Quản lý viện), `da_nhac_bat_dau_luc`; cột sinh `khoa_mo = IF(trang_thai = 'DANG_MO', ca_id, NULL)` `UNIQUE`.

| Từ | Sự kiện | Đến |
| --- | --- | --- |
| — | Công bố với ca thiếu phủ; ca Đạt → Thiếu phủ | `DANG_MO` |
| `DANG_MO` | Chi tiết đổi | `DANG_MO` |
| `DANG_MO` | Ca Đạt | `DONG_DU_PHU` |
| `DANG_MO` | Hết ca; ca bị hủy | `DONG_HET_CA` |

### `loi_moi_nhan_ca` – nhóm 2

`id`, `canh_bao_id`, `ca_id`, `vai_tro`, `nguoi_gui_id`, `nguoi_duoc_moi_id`, `trang_thai` (`CHO_TRA_LOI`, `DA_NHAN`, `DA_TU_CHOI`, `DA_RUT`, `HET_HIEU_LUC`), `gui_luc`, `tra_loi_luc`, `ly_do_tu_choi NULL`; cột sinh `khoa_cho = IF(trang_thai = 'CHO_TRA_LOI', CONCAT(ca_id, ':', nguoi_duoc_moi_id), NULL)` `UNIQUE`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Gửi | `CHO_TRA_LOI` | Người nhận cảnh báo |
| `CHO_TRA_LOI` | Nhận / Từ chối | `DA_NHAN` / `DA_TU_CHOI` | Người được mời |
| `CHO_TRA_LOI` | Rút | `DA_RUT` | Người gửi |
| `CHO_TRA_LOI` | Đủ phủ vai trò; hết ca; người được mời hoặc người gửi không còn đủ điều kiện | `HET_HIEU_LUC` | Hệ thống |

---

## 7. DBR và quy tắc

| Mã | Cách bảo đảm |
| --- | --- |
| DBR-21 (008) | Thêm vào ca qua `CaLenhPort.themVaoCa` (kiểm chồng giờ dưới khóa `nhan_vien`) |
| DBR-25 | Mốc nhắc, giờ ca theo Asia/Ho_Chi_Minh |
| BR-M09-09 | Lập lịch bằng lệnh; không ghi đè (`LichNhapLenhPort.taoNhap` kiểm lịch tháng); người phụ trách trống |
| BR-M09-10 | Vòng đời đổi ca; `KiemTraDoiCa` lúc duyệt |
| BR-M09-11 | `TinhPhu`; cảnh báo một mở mỗi ca; tra cứu không xếp hạng |
| Một yêu cầu mở mỗi (ca, nhân viên) | `yeu_cau_ca_mo` `UNIQUE` |
| Nhóm 3 (000) | Trigger trên `lan_lap_lich`, `lan_tinh_phu`, `nghi_theo_ngay` |
