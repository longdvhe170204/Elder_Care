# Data Model: Quản lý dinh dưỡng và suất ăn (011)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Người cao tuổi, dị nguyên, mục dị ứng, tầng/khu vực, giường, hợp đồng, lượt vắng, chuyến đi, lượt ở lại, người thân, sự cố, cảnh báo, tệp là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25). Bảng kho nguyên liệu (12.7) thuộc plan 019.

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp: `ban_gan_che_do_an`; `thuc_don`; `lan_chot` (người được tính, suất theo chế độ thành `JSON`).
- `danh_muc_chung`: `che_do_an`, `bua_an`, `loai_do_an_cam`, `mon_an` (thành phần, xác nhận Bác sĩ trong `thuoc_tinh`, `xac_nhan_boi`).
- `su_kien_chung`: `thuc_don_kiem_tra`, `doi_mon`, `phat_sinh`, `yeu_cau_suat_bo_sung`, `sai_lech_phieu`, `phan_bo_sung`, `xac_nhan_phuc_vu`.
- `ho_so_chung`: `phieu_bua_an` (suất đặc biệt thành `JSON`), `suat_an_nguoi_than`, `do_an_gia_dinh`, `luu_mau` (món thành `JSON`, món thêm sau phát sinh là sự kiện).
- `khoa_ky_thuat`: `bua_ngay`; `moc_da_nhac`: `thong_bao_xem_lai`; `trang_thai_nct`: `dinh_duong_nct`.
- Bảng phụ, bảng con *(bổ sung 2026-10-30, rà đủ tên bảng của data-model)*: `che_do_an_han_che` → `danh_muc_chung` (thuộc tính của chế độ ăn); `ban_gan_han_che` → cột `JSON` `han_che_rieng` của `ban_gan_che_do_an` *(bổ sung 2026-10-30, sót khi rà)*; `mon_an_phien_ban`, `mon_an_phien_ban_thanh_phan`, `mon_an_phien_ban_che_do` → `ho_so_chung` (phiên bản món, khóa khi Bác sĩ xác nhận; thành phần, chế độ thành `JSON`); `thuc_don_mon`, `thuc_don_mon_thay_the` → cột `JSON` của `thuc_don`; `su_kien_m08_da_xu_ly`, `lan_chay_m08` → `moc_da_nhac`; `nguoi_duoc_tinh`, `suat_theo_che_do` → cột `JSON` của `lan_chot`; `suat_dac_biet`, `luu_mau_mon` → `ho_so_chung`. Không thêm bảng riêng.
- Còn 3 bảng riêng của feature.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-30; sinh từ mục 0 và ràng buộc ở mô tả bảng các mục sau; chủ module rà, hoàn thiện khi viết migration, task T046)*

Quy ước: loại ở bảng chung đặt mã `<module>.<TÊN_BẢNG_CŨ>` (module M08; đổi sang tên tiếng Anh theo glossary khi dịch); `noi_dung`, `thuoc_tinh` hoặc `du_lieu` mang các cột của bảng cũ, có JSON Schema theo loại; bảng gộp riêng của feature có cột `kind` (hoặc `loai`) và cột `NULL` có `CHECK` theo loại. Mỗi dòng có ràng buộc ở bảng dưới cần một test tích hợp chứng minh ràng buộc vẫn giữ sau gộp (constitution XII), ghi vào bảng DBR áp dụng khi cài.

**Bảng giữ hoặc là bảng đích** (giữ cột, ràng buộc như mô tả ở các mục sau; bảng đích nhận thêm cột của bảng gộp vào): `ban_gan_che_do_an`, `thuc_don`, `lan_chot`.

| Bảng cũ | Đích | Loại ở bảng chung | Ràng buộc của bảng cũ | Cách giữ sau gộp |
| --- | --- | --- | --- | --- |
| `dinh_duong_nct` | `trang_thai_nct` | `M08.DINH_DUONG_NCT` | — | cột vào `JSON` hoặc cột của bảng đích |
| `che_do_an` | `danh_muc_chung` | `M08.CHE_DO_AN` | / `mac_dinh`; `khoa_mac_dinh` sinh `IF(mac_dinh AND trang_thai = 'HIEU_LUC', 1, NULL)` `UNIQUE` / Đúng một chế độ mặc định / FR-014 / | `khoa_duy_nhat` |
| `mon_an` | `danh_muc_chung` | `M08.MON_AN` | `mon_an_phien_ban` (nhóm 3 có bản chờ): `id`, `mon_an_id`, `so` (tăng), `ket_cau` (`THUONG`, `MEM`, `XAY_NHUYEN`), `khong_co_thanh_phan_trong_danh_muc`, `gui_boi_id`, `gui_luc`, `trang_thai` (`NHAP`, `CHO_XAC_NHAN`, `DA_XAC_NHAN`, `DA_TRA_LAI`, `DA_RUT`), `ly_… | cột vào `JSON` hoặc cột của bảng đích |
| `bua_an` | `danh_muc_chung` | `M08.BUA_AN` | — | cột vào `JSON` hoặc cột của bảng đích |
| `loai_do_an_cam` | `danh_muc_chung` | `M08.LOAI_DO_AN_CAM` | — | cột vào `JSON` hoặc cột của bảng đích |
| `thuc_don_kiem_tra` | `su_kien_chung` | `M08.THUC_DON_KIEM_TRA` | — | cột vào `JSON` hoặc cột của bảng đích |
| `doi_mon` | `su_kien_chung` | `M08.DOI_MON` | — | cột vào `JSON` hoặc cột của bảng đích |
| `bua_ngay` | `khoa_ky_thuat` | — | `id`, `bua_an_id`, `ngay`, `gio_du_kien` (chốt từ CFG-M08-04 lúc tạo), `nguong_giao_tre_phut`, `UNIQUE (bua_an_id, ngay)`; cột "đã làm lúc" cho job: `nhac_chot_luc`, `mon_an_toan_luc`, `suat_giu_luc`, `nhac_phat_sinh_luc`, `nhac_luu_mau_luc`. | `khoa_duy_nhat` |
| `nguoi_duoc_tinh` | cột `JSON` của `lan_chot` | — | — | ghi cùng bản ghi cha; domain kiểm |
| `suat_theo_che_do` | cột `JSON` của `lan_chot` | — | — | ghi cùng bản ghi cha; domain kiểm |
| `phat_sinh` | `su_kien_chung` | `M08.PHAT_SINH` | / `khoa_su_kien` `UNIQUE` / Sự kiện nguồn + bữa ngày (chống trùng NFR-04) / M-08 /; / `tao_luc`, `bep_xac_nhan_boi_id`, `bep_xac_nhan_luc` / Đặt một lần / FR-039, FR-041 / | `khoa_duy_nhat`; trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `yeu_cau_suat_bo_sung` | `su_kien_chung` | `M08.YEU_CAU_SUAT_BO_SUNG` | `id`, `bua_ngay_id`, `tang_khu_vuc_id`, `nct_id`, `ly_do`, `boi_id`, `luc`, `bep_xac_nhan_luc` (đặt một lần). Chỉ lập sau giờ bữa tới hết ngưỡng giao trễ (FR-040). | trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `suat_an_nguoi_than` | `ho_so_chung` | `M08.SUAT_AN_NGUOI_THAN` | — | cột vào `JSON` hoặc cột của bảng đích |
| `phieu_bua_an` | `ho_so_chung` | `M08.PHIEU_BUA_AN` | / `id`, `bua_ngay_id`, `tang_khu_vuc_id` / `UNIQUE (bua_ngay_id, tang_khu_vuc_id)` / FR-044, DBR-27 / | `khoa_duy_nhat` |
| `suat_dac_biet` | `ho_so_chung` | `M08.SUAT_DAC_BIET` | — | cột vào `JSON` hoặc cột của bảng đích |
| `sai_lech_phieu` | `su_kien_chung` | `M08.SAI_LECH_PHIEU` | `id`, `phieu_id`, `noi_dung`, `bao_boi_id`, `bao_luc`; xử lý đặt một lần: `cach_xu_ly` (`BO_SUNG_SUAT`, `DOI_SUAT`, `KHAC`), `mo_ta`, `xu_ly_boi_id`, `xu_ly_luc`. | trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `phan_bo_sung` | `su_kien_chung` | `M08.PHAN_BO_SUNG` | `id`, `phieu_id`, `phat_sinh_id NULL`, `yeu_cau_bo_sung_id NULL` (đúng một khác `NULL`), `giao_boi_id`, `giao_luc`, `nhan_boi_id`, `nhan_luc` (đặt một lần). | trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `xac_nhan_phuc_vu` | `su_kien_chung` | `M08.XAC_NHAN_PHUC_VU` | — | cột vào `JSON` hoặc cột của bảng đích |
| `do_an_gia_dinh` | `ho_so_chung` | `M08.DO_AN_GIA_DINH` | — | cột vào `JSON` hoặc cột của bảng đích |
| `luu_mau` | `ho_so_chung` | `M08.LUU_MAU` | `luu_mau` (nhóm 3): `id`, `bua_ngay_id` `UNIQUE`, `boi_id`, `luc`, `vi_tri_luu`, `het_han_luu` (lúc + CFG-M08-06), cột hủy đặt một lần `huy_boi_id`, `huy_luc`. `luu_mau_mon`: `luu_mau_id`, `mon_an_id`, `da_luu`, `ly_do_khong_luu`, `them_sau_phat_sinh`. | `khoa_duy_nhat`; trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `thong_bao_xem_lai` | `moc_da_nhac` | `M08.THONG_BAO_XEM_LAI` | — | cột vào `JSON` hoặc cột của bảng đích |
| `che_do_an_han_che` | `danh_muc_chung` (thuộc tính của chế độ ăn) | `M08.CHE_DO_AN_HAN_CHE` | — | cột vào `JSON` hoặc cột của bảng đích |
| `mon_an_phien_ban` | `ho_so_chung` (phiên bản món, khóa khi Bác sĩ xác nhận; thành phần, chế độ thành `JSON`) | `M08.MON_AN_PHIEN_BAN` | `mon_an_phien_ban` (nhóm 3 có bản chờ): `id`, `mon_an_id`, `so` (tăng), `ket_cau` (`THUONG`, `MEM`, `XAY_NHUYEN`), `khong_co_thanh_phan_trong_danh_muc`, `gui_boi_id`, `gui_luc`, `trang_thai` (`NHAP`, `CHO_XAC_NHAN`, `DA_XAC_NHAN`, `DA_TRA_LAI`, `DA_RUT`), `ly_ | ghi cùng bản ghi cha; domain kiểm |
| `mon_an_phien_ban_thanh_phan` | `ho_so_chung` (phiên bản món, khóa khi Bác sĩ xác nhận; thành phần, chế độ thành `JSON`) | `M08.MON_AN_PHIEN_BAN_THANH_PHAN` | — | ghi cùng bản ghi cha; domain kiểm |
| `mon_an_phien_ban_che_do` | `ho_so_chung` (phiên bản món, khóa khi Bác sĩ xác nhận; thành phần, chế độ thành `JSON`) | `M08.MON_AN_PHIEN_BAN_CHE_DO` | — | ghi cùng bản ghi cha; domain kiểm |
| `thuc_don_mon` | cột `JSON` của `thuc_don` | — | `thuc_don_mon`: `id`, `thuc_don_id`, `ngay`, `bua_an_id`, `che_do_an_id`, `mon_an_id`, `thu_tu`. `thuc_don_mon_thay_the`: `thuc_don_mon_id`, `mon_an_id`, `uu_tien`. Sửa chỉ khi `NHAP` (trigger đối chiếu trạng thái thực đơn), trừ lệnh đổi món (FR-031) ghi kèm ` | `khoa_luc` + trigger khóa theo dòng |
| `thuc_don_mon_thay_the` | cột `JSON` của `thuc_don` | — | `thuc_don_mon`: `id`, `thuc_don_id`, `ngay`, `bua_an_id`, `che_do_an_id`, `mon_an_id`, `thu_tu`. `thuc_don_mon_thay_the`: `thuc_don_mon_id`, `mon_an_id`, `uu_tien`. Sửa chỉ khi `NHAP` (trigger đối chiếu trạng thái thực đơn), trừ lệnh đổi món (FR-031) ghi kèm ` | `khoa_luc` + trigger khóa theo dòng |
| `su_kien_m08_da_xu_ly` | `moc_da_nhac` | `M08.SU_KIEN_M08_DA_XU_LY` | - `su_kien_m08_da_xu_ly`: `khoa_su_kien` `UNIQUE`, `xu_ly_luc` (listener idempotent). | `khoa_duy_nhat` |
| `lan_chay_m08` | `moc_da_nhac` | `M08.LAN_CHAY_M08` | — | cột vào `JSON` hoặc cột của bảng đích |
| `luu_mau_mon` | `ho_so_chung` | `M08.LUU_MAU_MON` | `luu_mau` (nhóm 3): `id`, `bua_ngay_id` `UNIQUE`, `boi_id`, `luc`, `vi_tri_luu`, `het_han_luu` (lúc + CFG-M08-06), cột hủy đặt một lần `huy_boi_id`, `huy_luc`. `luu_mau_mon`: `luu_mau_id`, `mon_an_id`, `da_luu`, `ly_do_khong_luu`, `them_sau_phat_sinh`. | `khoa_duy_nhat`; trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `dinh_duong_nct` | 2 (kỹ thuật) | Đích khóa theo người (M-03) *(R-15: `dinh_duong_nct` → `trang_thai_nct`)* |
| `che_do_an`, `che_do_an_han_che` | 1 | A (FR-001, FR-002) *(R-15: `che_do_an` → `danh_muc_chung`)* |
| `mon_an` | 1 có xác nhận | A (FR-003, FR-004) *(R-15: `mon_an` → `danh_muc_chung`)* |
| `mon_an_phien_ban`, `mon_an_phien_ban_thanh_phan`, `mon_an_phien_ban_che_do` | 3 (có bản chờ) | A *(R-15: `mon_an_phien_ban`, `mon_an_phien_ban_thanh_phan`, `mon_an_phien_ban_che_do` → `ho_so_chung` (phiên bản món, khóa khi Bác sĩ xác nhận; thành phần, chế độ thành `JSON`))* |
| `bua_an`, `loai_do_an_cam` | 1 | A (FR-006, FR-007) *(R-15: `bua_an`, `loai_do_an_cam` → `danh_muc_chung`)* |
| `ban_gan_che_do_an` | 2 | B |
| `thuc_don`, `thuc_don_mon`, `thuc_don_mon_thay_the` | 2 | D *(R-15: `thuc_don_mon`, `thuc_don_mon_thay_the` → cột `JSON` của `thuc_don`)* |
| `thuc_don_kiem_tra` | 3 | D (FR-025, FR-029) *(R-15: `thuc_don_kiem_tra` → `su_kien_chung`)* |
| `doi_mon` | 3 | D (FR-031) *(R-15: `doi_mon` → `su_kien_chung`)* |
| `bua_ngay` | 2 (kỹ thuật) | E *(R-15: `bua_ngay` → `khoa_ky_thuat`)* |
| `lan_chot`, `nguoi_duoc_tinh`, `suat_theo_che_do` | 3 | E (FR-033 → FR-036a) *(R-15: `nguoi_duoc_tinh`, `suat_theo_che_do` → cột `JSON` của `lan_chot`)* |
| `phat_sinh` | 3 (cột xác nhận đặt một lần) | E (FR-039 → FR-041) *(R-15: `phat_sinh` → `su_kien_chung`)* |
| `yeu_cau_suat_bo_sung` | 3 | E (FR-040) *(R-15: `yeu_cau_suat_bo_sung` → `su_kien_chung`)* |
| `suat_an_nguoi_than` | 2 | E (FR-043) *(R-15: `suat_an_nguoi_than` → `ho_so_chung`)* |
| `phieu_bua_an`, `suat_dac_biet` | 2 | F *(R-15: `phieu_bua_an`, `suat_dac_biet` → `ho_so_chung`)* |
| `sai_lech_phieu`, `phan_bo_sung` | 3 (cột xử lý, giao, nhận đặt một lần) | F (FR-048, FR-048b) *(R-15: `sai_lech_phieu`, `phan_bo_sung` → `su_kien_chung`)* |
| `xac_nhan_phuc_vu` | 3 | G *(R-15: `xac_nhan_phuc_vu` → `su_kien_chung`)* |
| `do_an_gia_dinh` | 2 | H *(R-15: `do_an_gia_dinh` → `ho_so_chung`)* |
| `thong_bao_xem_lai` | Kỹ thuật | I *(R-15: `thong_bao_xem_lai` → `moc_da_nhac`)* |
| `luu_mau`, `luu_mau_mon` | 3 | J *(R-15: `luu_mau`, `luu_mau_mon` → `ho_so_chung`)* |
| `su_kien_m08_da_xu_ly`, `lan_chay_m08` | Kỹ thuật | Listener, job *(R-15: `su_kien_m08_da_xu_ly`, `lan_chay_m08` → `moc_da_nhac`)* |

---

## 2. `dinh_duong_nct` – nhóm 2 (kỹ thuật) → gộp theo R-15: `dinh_duong_nct` → `trang_thai_nct`

`nct_id` (khóa chính), `phien_ban`. Mọi lệnh đổi bản gán, đồ ăn gia đình của một người khóa dòng này (M-03).

---

## 3. Danh mục (mục A)

### `che_do_an` – nhóm 1 → gộp theo R-15: `che_do_an` → `danh_muc_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `ten`, `mo_ta`, `lien_quan_dieu_tri` | | FR-001 |
| `mac_dinh`; `khoa_mac_dinh` sinh `IF(mac_dinh AND trang_thai = 'HIEU_LUC', 1, NULL)` `UNIQUE` | Đúng một chế độ mặc định | FR-014 |
| `mon_an_toan_id NULL` | Bắt buộc khi tạo mới; món Hiệu lực, phù hợp, không chứa hạn chế (kiểm FR-005) | FR-001, FR-037a |
| `trang_thai` (`HIEU_LUC`, `NGUNG_HIEU_LUC`) | Ngừng bị chặn khi còn bản gán Hiệu lực/Chờ hiệu lực hoặc món trong thực đơn chưa hết tuần | FR-002 |

`che_do_an_han_che`: `che_do_an_id`, `di_nguyen_id`. Đổi danh sách chạy lại FR-005 với thực đơn chưa hết tuần (M-04). *(R-15: `che_do_an_han_che` → `danh_muc_chung` (thuộc tính của chế độ ăn))*

### `mon_an` – nhóm 1 có xác nhận → gộp theo R-15: `mon_an` → `danh_muc_chung`

| Cột | Ghi chú |
| --- | --- |
| `id`, `ten`, `ghi_chu` | Sửa không cần xác nhận |
| `trang_thai` (`NHAP`, `CHO_XAC_NHAN`, `HIEU_LUC`, `NGUNG_HIEU_LUC`, `DA_HUY`) | |
| `phien_ban_hieu_luc_id NULL` | Phiên bản đang dùng cho kiểm tra, chốt, phục vụ |
| `tung_hieu_luc` | Chặn Hủy khi đã từng Hiệu lực |
| `ly_do_ngung`, `ngung_boi_id`, `ngung_luc` | |
| `phien_ban` | `If-Match` |

`mon_an_phien_ban` (nhóm 3 có bản chờ): `id`, `mon_an_id`, `so` (tăng), `ket_cau` (`THUONG`, `MEM`, `XAY_NHUYEN`), `khong_co_thanh_phan_trong_danh_muc`, `gui_boi_id`, `gui_luc`, `trang_thai` (`NHAP`, `CHO_XAC_NHAN`, `DA_XAC_NHAN`, `DA_TRA_LAI`, `DA_RUT`), `ly_do_tra_lai`, `xac_nhan_boi_id`, `xac_nhan_luc`. Cột sinh `khoa_cho = IF(trang_thai IN ('NHAP','CHO_XAC_NHAN'), mon_an_id, NULL)` `UNIQUE` (tối đa một bản đang soạn hoặc chờ). Trigger: khi `OLD.trang_thai = 'DA_XAC_NHAN'` chặn mọi đổi. Con: `mon_an_phien_ban_thanh_phan (phien_ban_id, di_nguyen_id)`, `mon_an_phien_ban_che_do (phien_ban_id, che_do_an_id)`. *(R-15: `mon_an_phien_ban` → `ho_so_chung` (phiên bản món, khóa khi Bác sĩ xác nhận; thành phần, chế độ thành `JSON`))*

**Bảng chuyển** (khớp bảng trạng thái món của spec):

| Từ | Lệnh | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Tạo | `NHAP` | Nhân viên bếp | — |
| `NHAP` | Gửi xác nhận | `CHO_XAC_NHAN` | Nhân viên bếp | Có thành phần hoặc dấu không có; ≥ 1 chế độ ăn phù hợp |
| `CHO_XAC_NHAN` | Xác nhận / Trả lại / Rút lại | `HIEU_LUC` / `NHAP` / `NHAP` | Bác sĩ / Bác sĩ (lý do) / Nhân viên bếp | — |
| `HIEU_LUC` | Gửi bản sửa; xác nhận, trả lại, rút bản sửa | `HIEU_LUC` | Nhân viên bếp; Bác sĩ | Bản sửa là `mon_an_phien_ban` mới; xác nhận đặt `phien_ban_hieu_luc_id` *(R-15: `mon_an_phien_ban` → `ho_so_chung` (phiên bản món, khóa khi Bác sĩ xác nhận; thành phần, chế độ thành `JSON`))* |
| `HIEU_LUC` | Ngừng hiệu lực | `NGUNG_HIEU_LUC` | Nhân viên bếp; Bác sĩ | Lý do; không là món an toàn của chế độ ăn Hiệu lực |
| `NHAP` | Hủy | `DA_HUY` | Nhân viên bếp | `tung_hieu_luc = false` |

### `bua_an` – nhóm 1 → gộp theo R-15: `bua_an` → `danh_muc_chung`

`id`, `ma` (khóa của CFG-M08-04), `ten`, `phuc_vu_noi_tru`, `phuc_vu_ban_tru`, `thu_tu`, `trang_thai`. Bữa đã có `lan_chot` không xóa được, chỉ Ngừng hiệu lực (FR-006).

### `loai_do_an_cam` – nhóm 1 → gộp theo R-15: `loai_do_an_cam` → `danh_muc_chung`

`id`, `ten`, `mo_ta`, `trang_thai`.

---

## 4. `ban_gan_che_do_an` – nhóm 2 (mục B)

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `che_do_an_id`, `ket_cau` | Kết cấu thức ăn | FR-010 |
| `han_che_rieng` (danh sách `di_nguyen_id`, bảng con `ban_gan_han_che`), `chi_dinh_rieng` | | FR-010 |
| `can_cu` (`DANH_GIA`, `XEM_LAI`, `KHAC`), `can_cu_id NULL`, `thong_bao_xem_lai_id NULL` | | FR-012, FR-059 |
| `ly_do_chuyen_mon NULL` | Bắt buộc khi `CanLyDoChuyenMon` trả true | FR-013 |
| `hieu_luc_tu_yeu_cau`, `hieu_luc_tu` (thực tế, FR-017) | | FR-011, FR-017 |
| `trang_thai` (`DE_XUAT`, `CHO_HIEU_LUC`, `HIEU_LUC`, `NGUNG`, `DA_HUY`) | | Bảng trạng thái |
| `canh_bao_da_xac_nhan` `JSON` | Cảnh báo FR-016 tại lúc áp dụng | FR-016 |
| `ngung_luc`, `ly_do_huy`, `huy_boi_id` | | |
| `khoa_cho` sinh `IF(trang_thai IN ('DE_XUAT','CHO_HIEU_LUC'), nct_id, NULL)` `UNIQUE` | Một bản đang chờ mỗi người | FR-012 |
| `khoa_hieu_luc` sinh `IF(trang_thai = 'HIEU_LUC', nct_id, NULL)` `UNIQUE` | Một bản Hiệu lực | FR-011 |

Trigger: nội dung (`che_do_an_id`, `ket_cau`, hạn chế, chỉ định, căn cứ) không đổi sau khi lập; chỉ trạng thái và cột thời điểm.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Lập đề xuất | `DE_XUAT` | Bác sĩ |
| `DE_XUAT` | Áp dụng | `HIEU_LUC` / `CHO_HIEU_LUC` | Bác sĩ |
| `DE_XUAT`, `CHO_HIEU_LUC` | Hủy | `DA_HUY` | Bác sĩ (lý do) |
| `CHO_HIEU_LUC` | Tới `hieu_luc_tu` | `HIEU_LUC` | Bộ lập lịch |
| `HIEU_LUC` | Bản mới Hiệu lực; trạng thái cuối của người | `NGUNG` | Hệ thống |
| `DE_XUAT`, `CHO_HIEU_LUC` | Trạng thái cuối của người | `DA_HUY` | Hệ thống |

---

## 5. Thực đơn tuần (mục D)

### `thuc_don` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `ngay_dau_tuan` (thứ 2) | `khoa_tuan` sinh `IF(trang_thai <> 'DA_HUY', ngay_dau_tuan, NULL)` `UNIQUE` | FR-023 |
| `sao_chep_tu_id NULL` | | FR-023 |
| `trang_thai` (`NHAP`, `CHO_CONG_BO`, `CONG_BO`, `DA_AP_DUNG`, `DA_HUY`) | | Bảng trạng thái |
| `xac_nhan_che_bien`, `ghi_chu_che_bien` | Bắt buộc khi gửi | FR-025a |
| `gui_boi_id`, `gui_luc`, `cong_bo_boi_id`, `cong_bo_luc`, `ly_do_tra_lai`, `ly_do_huy` | | FR-029 |
| `ap_dung_luc` | Đặt bởi job hoặc lệnh công bố khi tuần đã bắt đầu | |
| `phien_ban` | `If-Match` khi sửa Nháp | |

`thuc_don_mon`: `id`, `thuc_don_id`, `ngay`, `bua_an_id`, `che_do_an_id`, `mon_an_id`, `thu_tu`. `thuc_don_mon_thay_the`: `thuc_don_mon_id`, `mon_an_id`, `uu_tien`. Sửa chỉ khi `NHAP` (trigger đối chiếu trạng thái thực đơn), trừ lệnh đổi món (FR-031) ghi kèm `doi_mon`. *(R-15: `doi_mon` → `su_kien_chung`)*

### `thuc_don_kiem_tra` – nhóm 3 → gộp theo R-15: `thuc_don_kiem_tra` → `su_kien_chung`

`id`, `thuc_don_id`, `lan` (`GUI`, `CONG_BO`, `DOI_MON`), `ket_qua_do_phu` `JSON`, `canh_bao_lap_mon` `JSON`, `canh_bao_di_ung` `JSON` (số người; tên chỉ ở bản cho Bác sĩ), `xac_nhan_boi_id`, `xac_nhan_luc`.

### `doi_mon` – nhóm 3 → gộp theo R-15: `doi_mon` → `su_kien_chung`

`id`, `thuc_don_id`, `ngay`, `bua_an_id`, `che_do_an_id`, `mon_truoc_id`, `mon_sau_id`, `ly_do`, `boi_id`, `luc`, `sau_chot` (đã tạo phát sinh).

---

## 6. Chốt suất và phát sinh (mục E)

### `bua_ngay` – nhóm 2 (kỹ thuật) → gộp theo R-15: `bua_ngay` → `khoa_ky_thuat`

`id`, `bua_an_id`, `ngay`, `gio_du_kien` (chốt từ CFG-M08-04 lúc tạo), `nguong_giao_tre_phut`, `UNIQUE (bua_an_id, ngay)`; cột "đã làm lúc" cho job: `nhac_chot_luc`, `mon_an_toan_luc`, `suat_giu_luc`, `nhac_phat_sinh_luc`, `nhac_luu_mau_luc`.

### `lan_chot` – nhóm 3

`id`, `bua_ngay_id` `UNIQUE`, `chot_boi_id`, `chot_luc`, `chot_sau_gio_bua`, `thuc_don_id NULL` (`NULL` → dấu "chưa có thực đơn", FR-038), `tong_suat`.

### `nguoi_duoc_tinh` – nhóm 3 → gộp theo R-15: `nguoi_duoc_tinh` → cột `JSON` của `lan_chot`

`lan_chot_id`, `loai` (`NOI_TRU`, `BAN_TRU`, `NGUOI_THAN`), `nct_id`, `nguoi_than_id NULL`, `luot_o_lai_id NULL`, `tang_khu_vuc_id`, `che_do_an_id`, `ban_gan_id NULL` (`NULL` → `CHUA_GAN_CHE_DO_AN`), `ket_cau`, `dau` (tập: `DU_KIEN_TRO_VE`, `CAN_DOI_CHIEU`), `suat_dac_biet`. *(R-15: `suat_dac_biet` → `ho_so_chung`)*

### `suat_theo_che_do` – nhóm 3 → gộp theo R-15: `suat_theo_che_do` → cột `JSON` của `lan_chot`

`lan_chot_id`, `tang_khu_vuc_id`, `che_do_an_id`, `mon_an_id`, `mon_an_phien_ban_id`, `so_suat`.

### `phat_sinh` – nhóm 3 (cột xác nhận đặt một lần) → gộp theo R-15: `phat_sinh` → `su_kien_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `bua_ngay_id`, `tang_khu_vuc_id`, `che_do_an_id`, `so_suat` (±) | | FR-039 |
| `nct_id NULL`, `nguoi_than_id NULL`, `loai_thay_doi` | Trở về, vắng, chuyển giường, đổi chế độ, đổi món, dị ứng, suất giữ… | FR-039 |
| `khoa_su_kien` `UNIQUE` | Sự kiện nguồn + bữa ngày (chống trùng NFR-04) | M-08 |
| `can_giao` | `false` với ghi nhận suất giữ | FR-040a |
| `doi_suat_dac_biet` `JSON NULL` | Suất đặc biệt trước/sau | FR-039 |
| `tao_luc`, `bep_xac_nhan_boi_id`, `bep_xac_nhan_luc` | Đặt một lần | FR-039, FR-041 |

### `yeu_cau_suat_bo_sung` – nhóm 3 → gộp theo R-15: `yeu_cau_suat_bo_sung` → `su_kien_chung`

`id`, `bua_ngay_id`, `tang_khu_vuc_id`, `nct_id`, `ly_do`, `boi_id`, `luc`, `bep_xac_nhan_luc` (đặt một lần). Chỉ lập sau giờ bữa tới hết ngưỡng giao trễ (FR-040).

### `suat_an_nguoi_than` – nhóm 2 → gộp theo R-15: `suat_an_nguoi_than` → `ho_so_chung`

`id`, `lan_chot_id`, `bua_ngay_id`, `nguoi_than_id`, `nct_id`, `luot_o_lai_id`, `trang_thai` (`DA_GHI`, `DA_HUY`), `huy_luc`, `ma_su_kien_tinh_phi` (khóa nguồn cho 010). Hủy chỉ trước giờ bữa (FR-043).

---

## 7. Phiếu bữa ăn (mục F)

### `phieu_bua_an` – nhóm 2 → gộp theo R-15: `phieu_bua_an` → `ho_so_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `bua_ngay_id`, `tang_khu_vuc_id` | `UNIQUE (bua_ngay_id, tang_khu_vuc_id)` | FR-044, DBR-27 |
| `lap_tu` (`CHOT`, `PHAT_SINH`) | | FR-044 |
| `trang_thai` (`DA_CHOT`, `DA_CHUAN_BI`, `DA_GIAO`, `CO_SAI_LECH`, `DA_NHAN`) | | Bảng trạng thái |
| `dau` (`CHUA_CO_THUC_DON`, `CHOT_SAU_GIO_BUA`) | | FR-038 |
| `giao_boi_id`, `giao_luc`, `nhan_boi_id`, `nhan_luc` | | FR-047, FR-048 |
| `da_bao_tre_luc` | Một lần | FR-048a |
| `phien_ban` | | |

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Chốt suất; xác nhận phát sinh "+" của tầng chưa có phiếu | `DA_CHOT` | Nhân viên bếp |
| `DA_CHOT` | Đánh dấu chuẩn bị xong | `DA_CHUAN_BI` | Nhân viên bếp |
| `DA_CHUAN_BI` | Phát sinh làm thêm hoặc đổi suất đặc biệt | `DA_CHOT` | Hệ thống |
| `DA_CHUAN_BI` | Giao (FR-047) | `DA_GIAO` | Nhân viên bếp |
| `DA_GIAO` | Xác nhận nhận / Báo sai lệch | `DA_NHAN` / `CO_SAI_LECH` | Người nhận tại tầng |
| `CO_SAI_LECH` | Ghi xử lý | `DA_GIAO` | Nhân viên bếp |
| `DA_GIAO`, `CO_SAI_LECH`, `DA_NHAN` | Phát sinh, suất bổ sung | Giữ nguyên (thêm `phan_bo_sung`) | Hệ thống *(R-15: `phan_bo_sung` → `su_kien_chung`)* |

### `suat_dac_biet` – nhóm 2 → gộp theo R-15: `suat_dac_biet` → `ho_so_chung`

`id`, `phieu_id`, `nct_id`, `nguoi_than_id NULL`, `phong`, `che_do_an_id`, `ket_cau`, `mon_an_id NULL`, `mon_an_phien_ban_id NULL`, `mon_thay_the_id NULL`, `dau` (tập: `THIEU_MON_THAY_THE`, `MON_AN_TOAN`, `CHUA_CO_MON`, `CHUA_GAN_CHE_DO_AN`, `DU_KIEN_TRO_VE`, `SUAT_GIU`, `CAN_DOI_CHIEU`), `xu_ly_boi_id`, `xu_ly_luc` (Bác sĩ chỉ định món, FR-037), `da_chuan_bi`, `trang_thai` (`HIEU_LUC`, `DA_BO`: người không còn được tính).

### `sai_lech_phieu` – nhóm 3 → gộp theo R-15: `sai_lech_phieu` → `su_kien_chung`

`id`, `phieu_id`, `noi_dung`, `bao_boi_id`, `bao_luc`; xử lý đặt một lần: `cach_xu_ly` (`BO_SUNG_SUAT`, `DOI_SUAT`, `KHAC`), `mo_ta`, `xu_ly_boi_id`, `xu_ly_luc`.

### `phan_bo_sung` – nhóm 3 → gộp theo R-15: `phan_bo_sung` → `su_kien_chung`

`id`, `phieu_id`, `phat_sinh_id NULL`, `yeu_cau_bo_sung_id NULL` (đúng một khác `NULL`), `giao_boi_id`, `giao_luc`, `nhan_boi_id`, `nhan_luc` (đặt một lần).

**DBR-27**: `SUM(suat_theo_che_do.so_suat)` của lần chốt + `SUM(phat_sinh.so_suat WHERE can_giao AND bep_xac_nhan_luc IS NOT NULL)` = tổng suất trên các phiếu (tính từ `suat_theo_che_do` + phát sinh gắn tầng). Chỉ lệnh chốt và lệnh xác nhận phát sinh đổi các số này, trong cùng transaction với khóa `bua_ngay` (M-09). *(R-15: `suat_theo_che_do` → cột `JSON` của `lan_chot`; `bua_ngay` → `khoa_ky_thuat`)*

---

## 8. `xac_nhan_phuc_vu` – nhóm 3 (mục G) → gộp theo R-15: `xac_nhan_phuc_vu` → `su_kien_chung`

`id`, `suat_dac_biet_id`, `nct_id`, `bua_ngay_id`, `tang_khu_vuc_id`, `ket_qua` (`DUNG_SUAT`, `CHAN_XUNG_DOT_DA_AN`, `CHAN_XUNG_DOT_CHUA_AN`), `su_co_id NULL`, `canh_bao_nguon` (khóa nguồn đã gửi 007), `boi_id`, `luc`, `thiet_bi_luc NULL` (ghi ngoại tuyến, Q-01). Đính chính qua 000 (`M08.XAC_NHAN_PHUC_VU`).

---

## 9. `do_an_gia_dinh` – nhóm 2 (mục H) → gộp theo R-15: `do_an_gia_dinh` → `ho_so_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `nguoi_gui_ten`, `nguoi_than_id NULL`, `loai`, `mo_ta`, `thanh_phan` (bảng con `di_nguyen_id`), `han_dung`, `cach_bao_quan`, `nhan_luc`, `nhan_boi_id` | | FR-052 |
| `trang_thai` (`KHONG_SU_DUNG`, `CAN_XAC_NHAN`, `DUOC_SU_DUNG`, `DA_KET_THUC`) | | Bảng trạng thái |
| `ly_do_he_thong` `JSON` | Kết quả `DoiChieuDoAn` (xung đột, loại cấm, nghi vấn) | FR-053, FR-054 |
| `xac_nhan_boi_id`, `xac_nhan_luc`, `ghi_chu_xac_nhan`, `ly_do_khong_cho` | | |
| `chuyen_khong_su_dung_vi` (`XUNG_DOT_MOI`, `HET_HAN`) | | FR-055, FR-056 |
| `cach_ket_thuc` (`TRA_LAI`, `HUY_BO`, `DA_DUNG_HET`), `nguoi_nhan_lai`, `ket_thuc_boi_id`, `ket_thuc_luc` | | Bảng trạng thái |

Lưu giữ `THEO_HO_SO`. Không có chuyển `KHONG_SU_DUNG → DUOC_SU_DUNG`.

---

## 10. Xem lại, lưu mẫu (mục I, J)

`thong_bao_xem_lai` (kỹ thuật): `id`, `nct_id`, `nguon` (`AN_KEM_KEO_DAI`, `SUT_CAN`), `nguon_id`, `luc`, `ban_gan_dang_dung_id`. Không có trạng thái (Q-307). *(R-15: `thong_bao_xem_lai` → `moc_da_nhac`)*

`luu_mau` (nhóm 3): `id`, `bua_ngay_id` `UNIQUE`, `boi_id`, `luc`, `vi_tri_luu`, `het_han_luu` (lúc + CFG-M08-06), cột hủy đặt một lần `huy_boi_id`, `huy_luc`. `luu_mau_mon`: `luu_mau_id`, `mon_an_id`, `da_luu`, `ly_do_khong_luu`, `them_sau_phat_sinh`. *(R-15: `luu_mau`, `luu_mau_mon` → `ho_so_chung`)*

---

## 11. Kỹ thuật

- `su_kien_m08_da_xu_ly`: `khoa_su_kien` `UNIQUE`, `xu_ly_luc` (listener idempotent). *(R-15: `su_kien_m08_da_xu_ly` → `moc_da_nhac`)*
- `lan_chay_m08`: `ten_viec`, `moc`, `bat_dau`, `ket_thuc`, `ket_qua` (chạy bù). *(R-15: `lan_chay_m08` → `moc_da_nhac`)*

## 12. DBR áp dụng

| DBR | Cách bảo đảm |
| --- | --- |
| DBR-15 | `suat_an_nguoi_than` là bản ghi nguồn chi phí; 010 nhận qua `SuKienTinhPhiNguon` (khóa `ma_su_kien_tinh_phi`) |
| DBR-25 | Ngày, giờ bữa theo Asia/Ho_Chi_Minh |
| DBR-36 | Mua sắm: thuộc plan 019 (DBR-31 kho nguyên liệu bỏ theo Q-392) |
| DBR-27 | Mục 7; test bất biến (M-15) |
| Ràng buộc nhóm 3 (000) | Trigger chặn UPDATE/DELETE trên bảng nhóm 3 ngoài cột đặt một lần |
