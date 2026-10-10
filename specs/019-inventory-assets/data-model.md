# Data Model: Kho nguyên liệu và tài sản của viện (019)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bảng nằm ở ba schema module: M08 (kho), M11 (mua sắm), M03 (tài sản, lịch xe). Bộ phận, nhân viên, dị nguyên, bữa ngày, chuyến đi, người cao tuổi, tệp là tham chiếu logic; `giuong` là bảng của 003 trong cùng M03. Số lượng `DECIMAL(12,3)`; tiền `BIGINT` (đồng). Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 1. Tổng quan

| Bảng | Module | Nhóm (1.5) | Mục spec |
| --- | --- | --- | --- |
| `nguyen_lieu`, `nguyen_lieu_di_nguyen` | M08 | 1 | FR-001 |
| `lo_nguyen_lieu` | M08 | 2 dẫn xuất | FR-004, DBR-31 |
| `phieu_kho`, `phieu_kho_dong` | M08 | 3 | FR-002, FR-003, FR-006 |
| `phieu_kiem_ke_kho`, `phieu_kiem_ke_kho_dong` | M08 | 2 → 3 | FR-010 |
| `yeu_cau_mua_sam`, `yeu_cau_mua_sam_dong` | M11 | 2 → 3 | FR-008 |
| `mua_sam_gan_nguon` | M11 | 3 | FR-008b, FR-008c, DBR-36 |
| `loai_tai_san` | M03 | 1 | FR-011 |
| `tai_san` | M03 | 1 + 2 | FR-011 → FR-013 |
| `tai_san_vi_tri`, `tai_san_bo_phan`, `tai_san_bao_tri`, `tai_san_hu_hong` | M03 | 3 | FR-012 → FR-016, DBR-32 |
| `lich_xe` | M03 | 2 | FR-017, FR-018 |
| `nhac_019` | M03/M08/M11 | Kỹ thuật | Chống nhắc trùng |

---

## 2. Kho nguyên liệu (M08)

- `nguyen_lieu`: `id`, `ten`, `nhom`, `don_vi`, `ton_toi_thieu`, `trang_thai` (`DANG_DUNG`, `NGUNG_HIEU_LUC`). `nguyen_lieu_di_nguyen`: `nguyen_lieu_id`, `di_nguyen_id`.
- `lo_nguyen_lieu`: `id`, `nguyen_lieu_id`, `so_lo`, `han_dung`, `ton` (≥ 0, `CHECK`), `UNIQUE (nguyen_lieu_id, so_lo, han_dung)`. Chỉ đổi trong transaction ghi phiếu.
- `phieu_kho` (nhóm 3): `id`, `loai` (`NHAP`, `XUAT`, `DAO`, `DIEU_CHINH`), `ngay`, `muc_dich` (`CHO_BUA` + `bua_ngay_id`, `HUY`, `KHAC` + ghi chú), `nha_cung_cap`, `nguon` (`MUA` + `yeu_cau_mua_sam_id`, `TANG`, `CHUYEN`) + mô tả nguồn, `tep_chung_tu_ids`, `phieu_goc_id NULL`, `kiem_ke_id NULL`, `ly_do`, `lap_boi_id`, `luc`. `phieu_kho_dong`: `phieu_id`, `nguyen_lieu_id`, `lo_id`, `so_luong` (có dấu theo loại), `don_gia`, `kiem_tra` (`DAT`, `KHONG_DAT`), `ly_do_khong_dat`, `ly_do_chon_lo`. Trigger chặn `UPDATE`, `DELETE`.
- `phieu_kiem_ke_kho`: `id`, `ma_ky`, `trang_thai` (`CHO_DUYET`, `DA_DUYET`, `BI_TRA_LAI`, `DA_HUY`), `lap_boi_id`, `duyet_boi_id`, `ly_do`; cột sinh `khoa_cho = IF(trang_thai = 'CHO_DUYET', ma_ky, NULL)` `UNIQUE`. Dòng: `lo_id`, `ton_he_thong`, `ton_thuc_te`, `chenh_lech`, `ly_do`.

| Từ | Lệnh | Đến | Người |
| --- | --- | --- | --- |
| — | Lập phiếu kiểm kê | `CHO_DUYET` | Nhân viên bếp |
| `CHO_DUYET` | Duyệt / Trả lại | `DA_DUYET` / `BI_TRA_LAI` | Phụ trách bộ phận Bếp; Quản lý viện khi người lập là Phụ trách |
| `CHO_DUYET` | Hủy | `DA_HUY` | Người lập |

---

## 3. Yêu cầu mua sắm (M11)

`yeu_cau_mua_sam`: `id`, `bo_phan_id`, `lap_boi_id`, `loai` (`NGUYEN_LIEU`, `TAI_SAN`, `DICH_VU`, `KHAC`), `tong_du_kien`, `so_duoc_duyet`, `ly_do_giam`, `ly_do`, `ngay_can`, `tham_chieu` `JSON`, `yeu_cau_goc_id NULL`, `so_tien_thuc_te`, `ngay_mua`, `nha_cung_cap`, `tep_chung_tu_ids`, `ghi_da_mua_boi_id`, `ket_qua_doi_chieu`, `so_lech`, `cach_xu_ly_lech`, `doi_chieu_boi_id`, `trang_thai` (`NHAP`, `CHO_DUYET`, `DA_DUYET`, `CHO_DOI_CHIEU`, `LECH`, `DA_DOI_CHIEU`, `TU_CHOI`, `DA_HUY`), `da_nhac_mua_luc`, `da_nhac_doi_chieu_luc`, `phien_ban`. Dòng: tên, số lượng, đơn giá dự kiến, thành tiền. Trigger: nội dung khóa khi rời `NHAP`.

`mua_sam_gan_nguon` (nhóm 3): `yeu_cau_id`, `loai_nguon` (`PHIEU_NHAP`, `TAI_SAN`, `BAO_TRI`), `nguon_id`, `so_tien`, `luc`; `UNIQUE (loai_nguon, nguon_id)` (một nguồn gắn đúng một yêu cầu, DBR-36).

| Từ | Lệnh | Đến | Người |
| --- | --- | --- | --- |
| — | Lập | `NHAP` | Phụ trách bộ phận |
| `NHAP` | Gửi duyệt / Hủy | `CHO_DUYET` / `DA_HUY` | Phụ trách bộ phận / người lập |
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

- `tai_san_vi_tri` (nhóm 3): `tai_san_id`, `vi_tri` (khu vực, tầng, phòng, `BAI_XE`, `KHO_TAI_SAN`), `tu`, `den NULL`, `ly_do`, `boi_id`; cột sinh `khoa_mo = IF(den IS NULL, tai_san_id, NULL)` `UNIQUE` (không chồng khoảng; `den` đặt một lần).
- `tai_san_bo_phan` (nhóm 3): bộ phận cũ, mới, lý do, người, lúc.
- `tai_san_bao_tri` (nhóm 3): `loai` (`BAO_TRI`, `KIEM_DINH`, `DANG_KIEM`, `BAO_HIEM`), `mo_luc`, `ket_qua` (`DAT`, `KHONG_DAT`) đặt một lần, ngày, nội dung, đơn vị, `thue_ngoai`, `chi_phi`, `yeu_cau_mua_sam_id NULL`.
- `tai_san_hu_hong` (nhóm 3): mô tả, `nguon` (`BAO_HONG`, `DON_GIUONG`), `muc_anh_huong` (`MAT_AN_TOAN`, `KHONG_MAT_AN_TOAN`) `NULL`, `cho_chuyen_nguoi`, gỡ dấu (lý do, nội dung đã sửa, người sửa, lúc) đặt một lần.
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

## 5. `lich_xe` (M03)

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
| DBR-31 | Tồn lô chỉ đổi qua phiếu dưới khóa lô; `CHECK ton >= 0`; phiếu nhóm 3 |
| DBR-32 | `giuong_id` `UNIQUE`; tài sản giường tạo kèm lệnh Tạo giường; trạng thái giường ↔ tài sản đổi cùng transaction qua port 003 |
| DBR-36 | `mua_sam_gan_nguon` `UNIQUE (loai_nguon, nguon_id)`; phiếu nhập mua, tài sản mua, bảo trì thuê ngoài kiểm yêu cầu Đã duyệt cùng bộ phận trong lệnh |
| BR-M03-16 | `KiemTraDatLich` dưới khóa xe |
| BR-M08-18 | Lô quá hạn chỉ xuất hủy |
| BR-M11-18 | `KiemTraVuot` theo CFG-M11-07 |
| BR-M11-19 | Người đối chiếu ≠ người ghi đã mua; Kế toán không lập |
