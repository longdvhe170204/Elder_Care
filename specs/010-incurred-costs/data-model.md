# Data Model: Chi phí phát sinh (010)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Người cao tuổi, hợp đồng, phiên bản nội dung, dịch vụ, phiên bản đơn giá, lượt vắng, công việc, kết quả, liều, lần giao, lượt ở lại, suất ăn, điểm danh, người thân, tệp là tham chiếu logic (`BIGINT` hoặc mã nguồn), không có khóa ngoại sang module khác. Số tiền lưu `BIGINT` (đồng, số nguyên, FR-013); số lượng `DECIMAL(10,2)`; hệ số `DECIMAL(5,2)` (phần trăm). Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `chi_phi_nct` | 2 (kỹ thuật) | Đích khóa theo người (P-03) |
| `ky_chi_phi` | 2 | A |
| `bang_chi_phi` | 2 → 3 khi Đã chốt | A, G |
| `bang_chi_phi_tong_loai` | 3 | A (tổng lúc chốt) |
| `khoan_chi_phi` | 2 → 3 khi Đã chốt | B, C, D, E, F |
| `lich_su_tinh_lai` | 3 | D (FR-018) |
| `bien_dong_nguon_sau_chot` | 3 | F (FR-027) |
| `su_kien_tinh_phi` | Kỹ thuật (chỉ mục) | B (P-04) |
| `su_kien_nguon_da_xu_ly` | Kỹ thuật | B (chống trùng) |
| `de_nghi_mua_ho`, `de_nghi_gui_nguoi_dai_dien` | 2 | E |
| `quyet_dinh_mua_ho` | 3 | E |
| `lan_xuat_ke_toan`, `lan_xuat_bang` | 3 | H (FR-041) |
| `lan_chay_m11` | Kỹ thuật | Job |

---

## 2. `chi_phi_nct` – nhóm 2 (kỹ thuật)

`nct_id` (khóa chính), `mien_hoan_thanh_tu NULL` (FR-036: thời điểm mục "chốt chi phí kỳ cuối" Hoàn thành sau qua đời), `phien_ban`.

---

## 3. Kỳ và bảng (mục A, G)

### `ky_chi_phi` – nhóm 2

`id`, `thang` (`yyyy-MM`, `UNIQUE`), `tu_ngay`, `den_ngay` (luôn trọn tháng dương lịch, FR-001), `trang_thai` (`DANG_MO`, `CHO_CHOT`, `DA_CHOT`), `han_chot` (đặt khi sang `CHO_CHOT`, từ CFG-M11-01), `da_chot_luc`.

| Từ | Sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Tới ngày 1 của tháng | `DANG_MO` | Hệ thống (theo lịch) |
| `DANG_MO` | Hết ngày cuối tháng | `CHO_CHOT` | Bộ lập lịch |
| `CHO_CHOT` | Bảng cuối cùng của kỳ Đã chốt | `DA_CHOT` | Hệ thống (trong lệnh chốt bảng) |

### `bang_chi_phi` – nhóm 2 → 3 khi Đã chốt

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `ky_id`, `nct_id`, `loai_bang` (`THUONG`, `BO_SUNG`) | `UNIQUE (ky_id, nct_id, loai_bang)` | FR-002, FR-002a |
| `ky_cuoi`, `den_ngay NULL` | Kỳ cuối tính đến E | FR-034 |
| `trang_thai` (`DANG_MO`, `CHO_CHOT`, `DA_CHOT`) | | Bảng trạng thái bảng |
| `tong` `BIGINT NULL` | Lưu khi chốt | FR-033 |
| `gui_chot_boi_id`, `gui_chot_luc`, `canh_bao_bien_dong` `JSON NULL` | | FR-032 |
| `chot_boi_id`, `chot_luc` | | FR-033 |
| `lan_xuat_dau_id NULL` | Đặt một lần (trigger cho phép duy nhất cột này sau chốt) | FR-041 |
| `phien_ban` | `If-Match` cho lệnh chốt | FR-033 |

Trigger: khi `OLD.trang_thai = 'DA_CHOT'` chỉ cho đặt `lan_xuat_dau_id` từ `NULL`; mọi đổi khác bị chặn. `BEFORE DELETE` theo mẫu 000.

**Bảng chuyển** (khớp bảng trạng thái bảng chi phí):

| Từ | Lệnh / sự kiện | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Lập khoản đầu tiên; lập hồ sơ kết thúc (004); lập khoản điều chỉnh cần bảng bổ sung | `DANG_MO` | Hành chính (lệnh của 010 hoặc 004) | FR-002, FR-002a |
| `DANG_MO` | Gửi chốt | `CHO_CHOT` | Hành chính | Thường: đã qua ngày cuối hoặc kỳ cuối; không còn Nháp |
| `CHO_CHOT` | Trả lại | `DANG_MO` | Quản lý viện | Lý do |
| `CHO_CHOT` | Có khoản Nháp mới / khoản về Nháp | `DANG_MO` | Hệ thống | — |
| `CHO_CHOT` | Chốt | `DA_CHOT` | Quản lý viện | Mọi khoản Đã duyệt hoặc Đã hủy |

### `bang_chi_phi_tong_loai` – nhóm 3

`bang_id`, `loai_chi_phi` (FR-014a), `tong`. Ghi khi chốt.

---

## 4. Khoản chi phí (mục B → F)

### `khoan_chi_phi` – nhóm 2 → 3 khi Đã chốt

| Cột | Kiểu / ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `bang_id`, `ky_id` | | FR-014 |
| `loai_chi_phi` | `ENUM` 10 loại của FR-014a | FR-014a |
| `nguon_sinh` | `ENUM('THEO_SU_KIEN','NHAP_TAY','MUA_HO','DIEU_CHINH')` | FR-014 |
| `loai_nguon`, `id_nguon`, `don_vi_tinh_phi` | Với `THEO_SU_KIEN`, điều chỉnh bổ sung; ví dụ `NGAY_LUU_TRU`, `nct:2026-10-05`, `NGAY` | DBR-15 |
| `de_nghi_mua_ho_id NULL` | Với `MUA_HO` | FR-024 |
| `dich_vu_id NULL`, `ma_vat_pham NULL`, `mo_ta` | Thuốc: mã vật phẩm; tên thuốc không lưu ở khoản (đọc qua 006 khi được phép xem) | FR-019 |
| `ngay_phat_sinh`, `thoi_diem_phat_sinh NULL` | | FR-005, FR-036 |
| `so_luong`, `don_vi`, `don_gia NULL`, `he_so NULL`, `so_tien NULL` | `so_tien NULL` khi `THIEU_DON_GIA` | FR-008 → FR-013 |
| `can_cu_don_gia` `JSON` | Phiên bản nội dung hợp đồng hoặc phiên bản đơn giá | FR-008 |
| `gia_tri_nguon_da_dung` `JSON` | Giá trị ngày lưu trú, trạng thái có mặt, số lượng nguồn lúc tính | FR-005a |
| `dau` `SET('THUOC_GOI','THIEU_DON_GIA','CHO_QUYET_DINH','KHONG_CO_DONG_CHINH_SACH','CHUA_VAO_O','LAP_TRUOC','PHAT_SINH_THUOC_KY_DA_CHOT','VUOT_SO_DONG_Y','MUA_GAP_VUOT','ANH_HUONG_KET_THUC')` | | FR-014 |
| `ly_do` | Bắt buộc với nhập tay, điều chỉnh | FR-021, FR-025 |
| `khoan_goc_id NULL`, `ky_goc_id NULL`, `loai_dieu_chinh NULL`, `bien_dong_id NULL` | Điều chỉnh | FR-025, FR-027 |
| `trang_thai` | `ENUM('NHAP','DA_KIEM_TRA','DA_DUYET','DA_CHOT','DA_HUY')` | Bảng khoản |
| `nguoi_tao_id`, `kiem_tra_boi_id`, `kiem_tra_luc`, `duyet_boi_id`, `duyet_luc`, `ly_do_chap_nhan_vuot`, `huy_ly_do`, `huy_boi_id NULL` (rỗng = Hệ thống) | | FR-014, FR-024 |
| `khoa_don_vi_hieu_luc` | Cột sinh `IF(nguon_sinh = 'THEO_SU_KIEN' AND trang_thai <> 'DA_HUY', CONCAT(loai_nguon, ':', id_nguon, ':', don_vi_tinh_phi), NULL)`, `UNIQUE` | DBR-15, FR-006 |
| `phien_ban` | | |

Chỉ mục `(bang_id, trang_thai)`, `(nct_id, ngay_phat_sinh)`, `(loai_nguon, id_nguon)`, `(khoan_goc_id)`, `(dich_vu_id, trang_thai)` (tính lại thiếu đơn giá).

Trigger: chặn mọi đổi khi `OLD.trang_thai IN ('DA_CHOT','DA_HUY')`; chặn đổi `so_luong`, `don_gia`, `he_so`, `so_tien` khi không kèm biến phiên `@tinh_lai` (chỉ service tính lại đặt, FR-016). Ràng buộc "tổng gốc + điều chỉnh không âm" (FR-026) kiểm ở backend dưới khóa bảng chứa khoản gốc.

**Bảng chuyển** (khớp bảng trạng thái khoản):

| Từ | Lệnh / sự kiện | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Lập theo sự kiện | `NHAP` | Hành chính | FR-005, FR-006, FR-007, FR-011 |
| — | Lập nhập tay | `NHAP` | Hành chính | Ngoài danh mục; lý do |
| — | Ghi đã mua | `NHAP` | Hành chính | Đề nghị Được phép mua; FR-024 |
| — | Lập điều chỉnh | `NHAP` | Hành chính | FR-025, FR-026 |
| `NHAP` | Tính lại | `NHAP` | Hệ thống | Nguồn đổi; có đơn giá cho khoản thiếu; ngày lập trước kết thúc |
| `NHAP` | Kiểm tra | `DA_KIEM_TRA` | Hành chính | Có số tiền |
| `DA_KIEM_TRA` | Bỏ kiểm tra | `NHAP` | Hành chính | Bảng Đang mở; lý do |
| `NHAP` | Hủy | `DA_HUY` | Hệ thống (nguồn bị hủy, ngoài thời gian tính phí); Hành chính (khoản tự lập) | Lý do / tham chiếu |
| `DA_KIEM_TRA`, `DA_DUYET` | Nguồn đổi / bị hủy | `NHAP` rồi tính lại hoặc hủy | Hệ thống | Chưa chốt |
| `DA_KIEM_TRA` | Duyệt | `DA_DUYET` | Quản lý viện | Khoản vượt: lý do chấp nhận |
| `DA_KIEM_TRA`, `DA_DUYET` | Trả lại | `NHAP` | Quản lý viện | Lý do; bảng chưa chốt |
| `DA_DUYET` | Chốt bảng | `DA_CHOT` | Hệ thống (trong lệnh chốt của Quản lý viện) | — |

### `lich_su_tinh_lai` – nhóm 3

`khoan_id`, `thoi_diem`, `truoc` `JSON` (số lượng, đơn giá, hệ số, số tiền, dấu), `sau` `JSON`, `nguyen_nhan` (`NGUON_DINH_CHINH`, `NGAY_LUU_TRU_DOI`, `CO_MAT_DOI`, `NGAY_NGHI_BAN_TRU`, `NHAN_LAI_THUOC`, `CO_DON_GIA`, `HET_LAP_TRUOC`, `PHU_LUC`), `can_cu` (tham chiếu bản đính chính, sự kiện), `chuyen_ve_nhap` (bool).

### `bien_dong_nguon_sau_chot` – nhóm 3

`id`, `khoan_goc_id`, `su_kien` (`DINH_CHINH`, `HUY_GHI_NHAN`, `QUYET_DINH_GIU_GIUONG`, `NHAN_LAI_THUOC`, `DOI_NGAY_KET_THUC`, `QUA_DOI`, `AP_DUNG_DOI_PHONG`), `gia_tri_truoc`, `gia_tri_sau` `JSON`, `tham_chieu`, `thoi_diem`. Khoản điều chỉnh do Hành chính lập trỏ `bien_dong_id` (FR-027).

---

## 5. Chỉ mục sự kiện tính phí (kỹ thuật)

### `su_kien_tinh_phi`

`loai_nguon` (`KET_QUA_CONG_VIEC`, `LIEU`, `LAN_DUNG_PRN`, `LAN_GIAO`, `SUAT_AN_NGUOI_THAN`, `DEM_O_LAI`, `DIEM_DANH_HOAT_DONG`), `nguon_id`, `don_vi`, khóa chính ba cột; `nct_id`, `ngay_phat_sinh`, `thoi_diem`, `dich_vu_id NULL`, `thuoc_id NULL`, `so_luong_hien_hanh`, `da_huy`, `cap_nhat_luc`, `phien_ban`. Chỉ mục `(nct_id, loai_nguon, ngay_phat_sinh)`. Cập nhật bởi listener; không có lệnh người dùng. Ngày lưu trú, buổi bán trú không lưu ở đây (đọc `NgayLuuTruApi`).

### `su_kien_nguon_da_xu_ly`

`khoa` khóa chính, `xu_ly_luc` (NFR-04).

---

## 6. Đề nghị mua hộ (mục E)

### `de_nghi_mua_ho` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `vat_pham_id NULL`, `mo_ta`, `so_luong` | | FR-023 |
| `so_tien_du_kien`, `so_tien_duoc_phep NULL`, `cfg_m11_02_luc_gui` | | FR-023, FR-024, Q-259 |
| `ly_do`, `nguoi_yeu_cau_loai` (`NHAN_VIEN`, `NGUOI_THAN`), `nguoi_yeu_cau_id` | | FR-023 |
| `trang_thai` | `ENUM('NHAP','CHO_DONG_Y','DUOC_PHEP_MUA','TU_CHOI','HUY','DA_MUA')` | Bảng đề nghị |
| `dang_cho_bo_sung`, `so_tien_bo_sung_xin NULL` | | Q-251, Q-269 |
| `so_tien_thuc_te`, `ngay_mua`, `tep_chung_tu_id`, `ly_do_mua_gap`, `nguoi_yeu_cau_mua_gap_id` | | FR-024, Q-269 |
| `gui_luc`, `da_nhac_luc`, `da_bao_qlv_luc` | Nhắc CFG-M15-05, 06 | Bảng đề nghị |
| `phien_ban` | | |

Trigger chặn mọi đổi khi trạng thái cuối (`TU_CHOI`, `HUY`, `DA_MUA`).

**Bảng chuyển** (khớp bảng đề nghị mua hộ):

| Từ | Lệnh | Đến | Người |
| --- | --- | --- | --- |
| — | Lập | `NHAP` | Hành chính |
| `NHAP` | Gửi (≤ CFG-M11-02) | `DUOC_PHEP_MUA` | Hành chính |
| `NHAP` | Gửi (> CFG-M11-02) | `CHO_DONG_Y` | Hành chính |
| `CHO_DONG_Y` | Đồng ý | `DUOC_PHEP_MUA` | Người đại diện; Quản lý viện |
| `CHO_DONG_Y` (lần đầu) | Từ chối | `TU_CHOI` | Người đại diện; Quản lý viện |
| `NHAP`, `CHO_DONG_Y`, `DUOC_PHEP_MUA` | Hủy | `HUY` | Hành chính |
| `DUOC_PHEP_MUA` | Xin đồng ý bổ sung | `CHO_DONG_Y` (`dang_cho_bo_sung`) | Hành chính |
| `CHO_DONG_Y` (bổ sung) | Từ chối; Rút yêu cầu bổ sung | `DUOC_PHEP_MUA` (số cũ) | Người đại diện, Quản lý viện; Hành chính |
| `DUOC_PHEP_MUA` | Ghi đã mua | `DA_MUA` | Hành chính |

### `de_nghi_gui_nguoi_dai_dien`

`de_nghi_id`, `lan_gui` (1 = đầu, 2… = bổ sung), `nguoi_than_id`. Danh sách người đại diện Hiệu lực lúc gửi (FR-023a).

### `quyet_dinh_mua_ho` – nhóm 3

`de_nghi_id`, `loai` (`DONG_Y`, `TU_CHOI`, `RUT_BO_SUNG`, `XIN_BO_SUNG`), `so_cu`, `so_moi`, `nguoi_quyet_dinh_loai`, `nguoi_quyet_dinh_id`, `kenh` (`CONG_NGUOI_THAN`, `QUAN_LY_VIEN`, `HANH_CHINH`), `ly_do`, `so_du_khong_du` (dấu lúc quyết định, Q-219), `thoi_diem`.

---

## 7. Xuất kế toán (mục H)

### `lan_xuat_ke_toan` – nhóm 3

`id`, `ky_id`, `pham_vi` (`TOAN_BO`, `CHUA_TUNG_XUAT`), `dinh_dang` (`XLSX`, `CSV`), `nguoi_xuat_id`, `thoi_diem`, `so_bang`, `so_khoan`, `tong_tien`, `danh_sach_chua_chot` `JSON`.

### `lan_xuat_bang` – nhóm 3

`lan_xuat_id`, `bang_id`, `lan_dau` (bool). Khóa chính hai cột đầu.

---

## 8. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bất biến | Cách bảo đảm | Nguồn |
| --- | --- | --- |
| Mỗi bản ghi nguồn tối đa một khoản đang hiệu lực mỗi đơn vị (không kể điều chỉnh); khoản có đúng một nguồn hoặc nhập tay/mua hộ | `UNIQUE khoa_don_vi_hieu_luc`; `CHECK` cột nguồn theo `nguon_sinh` | DBR-15 |
| Đơn giá theo hợp đồng / phiên bản tại ngày phát sinh; khoản thuộc gói 0 đồng | Backend (P-05); `can_cu_don_gia` lưu căn cứ; 004 đánh dấu phiên bản đã tham chiếu | DBR-16 |
| Bảng đã chốt và khoản của nó không đổi; sai sót bằng điều chỉnh; tổng gốc + điều chỉnh ≥ 0 | Trigger `bang_chi_phi`, `khoan_chi_phi`; backend dưới khóa | DBR-17 |
| Mỗi người mỗi kỳ tối đa một bảng thường, một bảng bổ sung | `UNIQUE (ky_id, nct_id, loai_bang)` | FR-002, FR-002a |
| Kỳ là tháng dương lịch | `UNIQUE thang`; backend | FR-001 |
| Mọi thay đổi có nhật ký | `NhatKyApi` trong transaction | DBR-23 |

DBR-30 (mỗi bảng Đã chốt có đúng một giao dịch thanh toán) do 017 bảo đảm khi nghe `BangChiPhiDaChot`; 010 phát sự kiện đúng một lần mỗi bảng (khóa `bangId`).

---

## 9. Nhật ký và phân loại trường

- Loại đối tượng: `M11.KY_CHI_PHI`, `M11.BANG_CHI_PHI`, `M11.KHOAN_CHI_PHI`, `M11.DE_NGHI_MUA_HO`, `M11.LAN_XUAT_KE_TOAN`.
- `PhanLoaiTruongProvider`: số tiền, đơn giá, tổng → `TAI_CHINH`; tên thuốc hiển thị (lấy từ 006 lúc đọc) → `THUOC`; ghi chú, lý do → `TAI_CHINH`.
- Không đăng ký `DinhChinhDuocProvider` (khoản đã chốt sửa bằng điều chỉnh, 000 R-08).

---

## 10. Lưu giữ (000 R-11)

| Loại dữ liệu | Hạn | Chuỗi tham chiếu |
| --- | --- | --- |
| Bảng, khoản, lịch sử tính lại, biến động, đề nghị mua hộ, quyết định | `THEO_HO_SO` (CFG-M15-03) | Khoản điều chỉnh ↔ khoản gốc; giao dịch số dư của 017 trỏ bảng |
| Kỳ của viện, lần xuất | `KHONG_GAN_HO_SO` (CFG-M15-04) | Bảng trong lần xuất |
| Chỉ mục sự kiện tính phí | Theo nguồn; xóa khi nguồn bị loại bỏ | — |

---

## 11. Truy vết DBR, BR và FR

| Mã | Bảo đảm ở | Test |
| --- | --- | --- |
| DBR-15 | Mục 4, 8 | `DBR_15_MotKhoanMoiDonViIT`, `LapTrungDongThoiIT` |
| DBR-16 | P-05 | `DBR_16_DonGiaNgayPhatSinhIT`, `BR_M11_03_ThuocGoiIT` |
| DBR-17 | Mục 3, 4 | `DBR_17_BangDaChotIT`, `FR_026_TongKhongAmIT` |
| BR-M11-01 | P-04 | `BR_M11_01_LapTheoSuKienIT`, `FR_005_HangLoatIT` |
| BR-M11-02 | P-05 | `BR_M11_02_PhienBanDonGiaIT` |
| BR-M11-03 | P-05 | `BR_M11_03_ThuocGoiIT` |
| BR-M11-04 | P-06 | `BR_M11_04_NhapTayIT` |
| BR-M11-05 | P-06 | `BR_M11_05_TinhLaiIT`, `FR_027_BaoSauChotIT` |
| BR-M11-06 | P-07 | `BR_M11_06_ChotBangIT` |
| BR-M11-07 | P-08, P-09 | `BR_M11_07_BienDongTest`, `BR_M11_07_MuaHoIT`, `FR_024_VuotSoDongYIT` |
| BR-M11-08 | P-10 | `BR_M11_08_KyCuoiIT`, `DieuKienChiPhiKyCuoiTest` |
| BR-M11-09 | P-05 | `BR_M11_09_ChiaGiaThangTest` (SC-002), `PhiBuoiBanTruTest` |
| BR-M02-06 (phần 010) | Hệ số từ 004 | `NgayVangHeSoIT` |
