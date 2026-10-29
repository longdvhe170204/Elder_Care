# Data Model: Tài sản, lịch xe và mua sắm của viện (019; kho nguyên liệu bỏ theo Q-392)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bảng nằm ở hai schema module: M11 (mua sắm), M03 (tài sản, lịch xe); M08 (kho) bỏ theo Q-392. Bộ phận, nhân viên, dị nguyên, bữa ngày, chuyến đi, người cao tuổi, tệp là tham chiếu logic; `giuong` là bảng của 003 trong cùng M03. Số lượng `DECIMAL(12,3)`; tiền `DECIMAL(15,0)`, số nguyên đồng (quy ước 000 data-model; sửa 2026-10-29, trước đó `BIGINT`): `don_gia`, `thanh_tien`, `tong_du_kien`, `so_duoc_duyet`, `so_tien_thuc_te`, `so_lech` của yêu cầu mua sắm, `so_tien` của `mua_sam_gan_nguon`, `nguyen_gia` của tài sản, `chi_phi` của bảo trì. Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ: `tai_san` (M03); yêu cầu mua sắm (M11, nguồn gắn thành `JSON`).
- `lich_su_chung`: `tai_san_vi_tri`, `tai_san_bo_phan`; `su_kien_chung`: `tai_san_bao_tri`, `tai_san_hu_hong`.
- `ho_so_chung`: `lich_xe`.
- Bảng phụ, bảng con *(bổ sung 2026-10-30, rà đủ tên bảng của data-model)*: `yeu_cau_mua_sam_dong` → cột `JSON` của `yeu_cau_mua_sam`; `loai_tai_san` → `danh_muc_chung`; `nhac_019` → `moc_da_nhac`; `mua_sam_gan_nguon` → cột `JSON` của `yeu_cau_mua_sam`. Không thêm bảng riêng.
- Còn 2 bảng riêng của feature.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-30; sinh từ mục 0 và ràng buộc ở mô tả bảng các mục sau; chủ module rà, hoàn thiện khi viết migration, task T029)*

Quy ước: loại ở bảng chung đặt mã `<module>.<TÊN_BẢNG_CŨ>` (module M03/M11; đổi sang tên tiếng Anh theo glossary khi dịch); `noi_dung`, `thuoc_tinh` hoặc `du_lieu` mang các cột của bảng cũ, có JSON Schema theo loại; bảng gộp riêng của feature có cột `kind` (hoặc `loai`) và cột `NULL` có `CHECK` theo loại. Mỗi dòng có ràng buộc ở bảng dưới cần một test tích hợp chứng minh ràng buộc vẫn giữ sau gộp (constitution XII), ghi vào bảng DBR áp dụng khi cài.

**Bảng giữ hoặc là bảng đích** (giữ cột, ràng buộc như mô tả ở các mục sau): `tai_san` (M03), `yeu_cau_mua_sam` (M11).

| Bảng cũ | Đích | Loại ở bảng chung | Ràng buộc của bảng cũ | Cách giữ sau gộp |
| --- | --- | --- | --- | --- |
| `lich_xe` | `ho_so_chung` | `M03.LICH_XE` | — | cột vào `JSON` hoặc cột của bảng đích |
| `tai_san_vi_tri` | `lich_su_chung` | `M03.TAI_SAN_VI_TRI` | - `tai_san_vi_tri` (nhóm 3): `tai_san_id`, `vi_tri` (khu vực, tầng, phòng, `BAI_XE`, `KHO_TAI_SAN`), `tu`, `den NULL`, `ly_do`, `boi_id`; cột sinh `khoa_mo = IF(den IS NULL, tai_san_id, NULL)` `UNIQUE` (không chồng khoảng; `den` đặt một lần). | `khoa_duy_nhat`; trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `tai_san_bo_phan` | `lich_su_chung` | `M03.TAI_SAN_BO_PHAN` | — | cột vào `JSON` hoặc cột của bảng đích |
| `tai_san_bao_tri` | `su_kien_chung` | `M03.TAI_SAN_BAO_TRI` | - `tai_san_bao_tri` (nhóm 3): `loai` (`BAO_TRI`, `KIEM_DINH`, `DANG_KIEM`, `BAO_HIEM`), `mo_luc`, `ket_qua` (`DAT`, `KHONG_DAT`) đặt một lần, ngày, nội dung, đơn vị, `thue_ngoai`, `chi_phi`, `yeu_cau_mua_sam_id NULL`. | trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `tai_san_hu_hong` | `su_kien_chung` | `M03.TAI_SAN_HU_HONG` | - `tai_san_hu_hong` (nhóm 3): mô tả, `nguon` (`BAO_HONG`, `DON_GIUONG`), `muc_anh_huong` (`MAT_AN_TOAN`, `KHONG_MAT_AN_TOAN`) `NULL`, `cho_chuyen_nguoi`, gỡ dấu (lý do, nội dung đã sửa, người sửa, lúc) đặt một lần. | trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `yeu_cau_mua_sam_dong` | cột `JSON` của `yeu_cau_mua_sam` | — | — | ghi cùng bản ghi cha; domain kiểm |
| `loai_tai_san` | `danh_muc_chung` | `M03.LOAI_TAI_SAN` | — | cột vào `JSON` hoặc cột của bảng đích |
| `nhac_019` | `moc_da_nhac` | `M03.NHAC_019` | — | cột vào `JSON` hoặc cột của bảng đích |
| `mua_sam_gan_nguon` | cột `JSON` của yêu cầu mua sắm | — | `yeu_cau_mua_sam`: `id`, `bo_phan_id`, `lap_boi_id`, `loai` (`NGUYEN_LIEU`, `TAI_SAN`, `DICH_VU`, `THUOC`, `KHAC`; `THUOC` bổ sung 2026-10-11, Q-337), `muc_uu_tien NULL` (`THUONG`, `GAP`), `thoi_diem_can NULL`, `tep_lien_quan_ids NULL` (bắt buộc hai cột đầu kh | ghi cùng bản ghi cha; domain kiểm |

---

## 1. Tổng quan

| Bảng | Module | Nhóm (1.5) | Mục spec |
| --- | --- | --- | --- |
| `yeu_cau_mua_sam`, `yeu_cau_mua_sam_dong` | M11 | 2 → 3 | FR-008 *(R-15: `yeu_cau_mua_sam_dong` → cột `JSON` của `yeu_cau_mua_sam`)* |
| `mua_sam_gan_nguon` | M11 | 3 | FR-008b, FR-008c, DBR-36 *(R-15: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm)* |
| `loai_tai_san` | M03 | 1 | FR-011 *(R-15: `loai_tai_san` → `danh_muc_chung`)* |
| `tai_san` | M03 | 1 + 2 | FR-011 → FR-013 |
| `tai_san_vi_tri`, `tai_san_bo_phan`, `tai_san_bao_tri`, `tai_san_hu_hong` | M03 | 3 | FR-012 → FR-016, DBR-32 *(R-15: `tai_san_vi_tri`, `tai_san_bo_phan` → `lich_su_chung`; `tai_san_bao_tri`, `tai_san_hu_hong` → `su_kien_chung`)* |
| `lich_xe` | M03 | 2 | FR-017, FR-018 *(R-15: `lich_xe` → `ho_so_chung`)* |
| `nhac_019` | M03/M11 | Kỹ thuật | Chống nhắc trùng *(R-15: `nhac_019` → `moc_da_nhac`)* |

---

## 2. Kho nguyên liệu (M08) *(Bỏ theo Q-392)*

Không còn bảng kho: `nguyen_lieu`, `nguyen_lieu_di_nguyen`, `lo_nguyen_lieu`, `phieu_kho`, `phieu_kho_dong`, `phieu_kiem_ke_kho`, `phieu_kiem_ke_kho_dong`. Nguyên liệu mua chỉ là dòng của yêu cầu mua sắm loại `NGUYEN_LIEU`.

---

## 3. Yêu cầu mua sắm (M11)

`yeu_cau_mua_sam`: `id`, `bo_phan_id`, `lap_boi_id`, `loai` (`NGUYEN_LIEU`, `TAI_SAN`, `DICH_VU`, `THUOC`, `KHAC`; `THUOC` bổ sung 2026-10-11, Q-337), `muc_uu_tien NULL` (`THUONG`, `GAP`), `thoi_diem_can NULL`, `tep_lien_quan_ids NULL` (bắt buộc hai cột đầu khi `THUOC`, `CHECK`), `tong_du_kien`, `so_duoc_duyet`, `ly_do_giam`, `ly_do`, `ngay_can`, `tham_chieu` `JSON`, `yeu_cau_goc_id NULL`, `so_tien_thuc_te`, `ngay_mua`, `nha_cung_cap`, `tep_chung_tu_ids`, `ghi_da_mua_boi_id`, `ket_qua_doi_chieu`, `so_lech`, `cach_xu_ly_lech`, `doi_chieu_boi_id`, `trang_thai` (`NHAP`, `CHO_DUYET`, `DA_DUYET`, `CHO_DOI_CHIEU`, `LECH`, `DA_DOI_CHIEU`, `TU_CHOI`, `DA_HUY`), `da_nhac_mua_luc`, `da_nhac_doi_chieu_luc`, `phien_ban`. Dòng: tên, số lượng, đơn giá dự kiến, thành tiền; với `THUOC` thêm `nct_id`, `don_thuoc_id`, `hoat_chat`, `ham_luong`, `dang_thuoc`, `don_vi` (bắt buộc, kiểm ở backend vì phụ thuộc loại của yêu cầu cha; FR-008d). Trigger: nội dung khóa khi rời `NHAP`. Không có `mua_sam_gan_nguon` cho `THUOC` (lần tiếp nhận ở 006). *(R-15: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm)*

`mua_sam_gan_nguon` (nhóm 3): `yeu_cau_id`, `loai_nguon` (`TAI_SAN`, `BAO_TRI`; `PHIEU_NHAP` bỏ theo Q-392), `nguon_id`, `so_tien`, `luc`; `UNIQUE (loai_nguon, nguon_id)` (một nguồn gắn đúng một yêu cầu, DBR-36). *(R-15: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm)*

| Từ | Lệnh | Đến | Người |
| --- | --- | --- | --- |
| — | Lập | `NHAP` | Phụ trách bộ phận; Điều dưỡng chỉ loại `THUOC` (FR-008d) |
| `NHAP` | Gửi duyệt / Hủy | `CHO_DUYET` / `DA_HUY` | Người lập |
| `CHO_DUYET` | Rút lại | `NHAP` | Người lập |
| `CHO_DUYET` | Duyệt (có thể giảm) / Từ chối | `DA_DUYET` / `TU_CHOI` | Quản lý viện |
| `DA_DUYET` | Ghi đã mua | `CHO_DOI_CHIEU` | Phụ trách bộ phận |
| `DA_DUYET` | Hủy (chưa có nguồn gắn) | `DA_HUY` | Phụ trách bộ phận |
| `CHO_DOI_CHIEU` | Xác nhận khớp / Trả lại / Ghi lệch | `DA_DOI_CHIEU` / `DA_DUYET` / `LECH` | Kế toán (không phải người ghi đã mua) |
| `LECH` | Chấp nhận lệch / yêu cầu hoàn tiền | `DA_DOI_CHIEU` | Quản lý viện |

---

## 4. Tài sản (M03)

| Cột `tai_san` | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `ma` `UNIQUE`, `loai_id`, `ten`, `bo_phan_id` | | FR-011 |
| `yeu_cau_mua_sam_id NULL`, `nguon` (`MUA`, `TANG`, `CHUYEN`, `GIUONG_003`), `nguyen_gia NULL` | Bắt buộc nguyên giá khi gắn yêu cầu (Q-294) | FR-011 |
| `ngay_dua_vao_su_dung`, `chu_ky_bao_tri`, `han_bao_tri`, `bien_so`, `so_cho`, `han_dang_kiem`, `han_bao_hiem` | | FR-011, FR-014 |
| `giuong_id NULL` `UNIQUE` | Loại giường | FR-011, DBR-32 |
| `trang_thai` (`SAN_SANG`, `DANG_SU_DUNG`, `HONG`, `DANG_BAO_TRI`, `DA_THANH_LY`) | | FR-013 |
| `qua_han_bao_tri`, `qua_han_dang_kiem`, `qua_han_bao_hiem`, `cho_chuyen_nguoi` | | FR-014, FR-015 |
| `phien_ban` | | |

- `tai_san_vi_tri` (nhóm 3): `tai_san_id`, `vi_tri` (khu vực, tầng, phòng, `BAI_XE`, `KHO_TAI_SAN`), `tu`, `den NULL`, `ly_do`, `boi_id`; cột sinh `khoa_mo = IF(den IS NULL, tai_san_id, NULL)` `UNIQUE` (không chồng khoảng; `den` đặt một lần). *(R-15: `tai_san_vi_tri` → `lich_su_chung`)*
- `tai_san_bo_phan` (nhóm 3): bộ phận cũ, mới, lý do, người, lúc. *(R-15: `tai_san_bo_phan` → `lich_su_chung`)*
- `tai_san_bao_tri` (nhóm 3): `loai` (`BAO_TRI`, `KIEM_DINH`, `DANG_KIEM`, `BAO_HIEM`), `mo_luc`, `ket_qua` (`DAT`, `KHONG_DAT`) đặt một lần, ngày, nội dung, đơn vị, `thue_ngoai`, `chi_phi`, `yeu_cau_mua_sam_id NULL`. *(R-15: `tai_san_bao_tri` → `su_kien_chung`)*
- `tai_san_hu_hong` (nhóm 3): mô tả, `nguon` (`BAO_HONG`, `DON_GIUONG`), `muc_anh_huong` (`MAT_AN_TOAN`, `KHONG_MAT_AN_TOAN`) `NULL`, `cho_chuyen_nguoi`, gỡ dấu (lý do, nội dung đã sửa, người sửa, lúc) đặt một lần. *(R-15: `tai_san_hu_hong` → `su_kien_chung`)*
- Thanh lý dùng yêu cầu phê duyệt của 000, loại `M03.THANH_LY_TAI_SAN` (Phụ trách bộ phận lập, Quản lý viện duyệt; `apDung` chuyển tài sản `DA_THANH_LY`, với giường gọi `apLenhTaiSan(THANH_LY)`); không có bảng riêng.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Ghi tăng (không phải giường) / 003 tạo giường | `SAN_SANG` | Phụ trách bộ phận / Hệ thống kèm lệnh 003 |
| `SAN_SANG` | Đưa vào sử dụng (không phải giường) | `DANG_SU_DUNG` | Phụ trách bộ phận |
| `SAN_SANG`, `DANG_SU_DUNG` | Báo hỏng | `HONG` (giường có người: giữ nguyên, dấu chờ chuyển người) | Phụ trách bộ phận; Trưởng tầng (tầng mình) |
| `SAN_SANG`, `DANG_SU_DUNG`, `HONG` | Đưa vào bảo trì | `DANG_BAO_TRI` | Phụ trách bộ phận; Trưởng tầng |
| `DANG_BAO_TRI` | Kết quả Đạt / Không đạt | `SAN_SANG` / `HONG` | Phụ trách bộ phận |
| Mọi trạng thái trừ `DA_THANH_LY` | Thanh lý (yêu cầu được duyệt) | `DA_THANH_LY` | Phụ trách bộ phận lập; Quản lý viện duyệt |
| Mọi trạng thái trừ `DA_THANH_LY` | Chuyển bộ phận | giữ nguyên | Quản lý viện |

---

## 5. `lich_xe` (M03) → gộp theo R-15: `lich_xe` → `ho_so_chung`

`id`, `xe_id` (tài sản loại xe), `di_du_kien`, `ve_du_kien`, `muc_dich` (`CHUYEN_DI`, `DI_KHAM`, `CHUYEN_VIEN_KHONG_CAP_CUU`, `KHAC`), `chuyen_di_id NULL`, `nct_id NULL`, `nguoi_lai_id` (hồ sơ nhân viên), `dat_boi_id`, `trang_thai` (`DA_DAT`, `DANG_DUNG`, `DA_HOAN_THANH`, `DA_HUY`), `xuat_phat_luc`, `tra_luc`, `ly_do_huy`, `da_nhac_qua_gio_luc`, `phien_ban`. Chỉ mục `(xe_id, di_du_kien, ve_du_kien)`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Đặt lịch | `DA_DAT` | Trưởng tầng, Hành chính, Quản lý viện |
| `DA_DAT` | Ghi xuất phát; người đầu tiên của chuyến rời viện | `DANG_DUNG` | Người đặt, Quản lý viện; Hệ thống (kèm lệnh 014) |
| `DANG_DUNG` | Chuyến gia hạn giờ về | `DANG_DUNG` | Hệ thống (kèm lệnh 014) |
| `DANG_DUNG` | Mọi bản ghi rời viện của chuyến bị Hủy ghi nhận | `DA_DAT` | Hệ thống (kèm lệnh 014) |
| `DANG_DUNG` | Ghi trả xe; Kết thúc điểm danh về | `DA_HOAN_THANH` | Người đặt, Quản lý viện; Hệ thống |
| `DA_DAT` | Hủy; chuyến bị hủy | `DA_HUY` | Người đặt, Quản lý viện; Hệ thống |
| `DA_DAT` | Dời giờ | `DA_DAT` | Người đặt, Quản lý viện; Hệ thống khi chuyến đổi giờ |

---

## 6. DBR và quy tắc

| Mã | Cách bảo đảm |
| --- | --- |
| DBR-32 | `giuong_id` `UNIQUE`; tài sản giường tạo kèm lệnh Tạo giường; trạng thái giường ↔ tài sản đổi cùng transaction qua port 003 |
| DBR-36 | `mua_sam_gan_nguon` `UNIQUE (loai_nguon, nguon_id)`; tài sản mua, bảo trì thuê ngoài kiểm yêu cầu Đã duyệt cùng bộ phận trong lệnh *(R-15: `mua_sam_gan_nguon` → cột `JSON` của yêu cầu mua sắm)* |
| BR-M03-16 | `KiemTraDatLich` dưới khóa xe |
| BR-M11-18 | `KiemTraVuot` theo CFG-M11-07 |
| BR-M11-19 | Người đối chiếu ≠ người ghi đã mua; Kế toán không lập |
