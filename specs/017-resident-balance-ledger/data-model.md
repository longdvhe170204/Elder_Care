# Data Model: Số dư và thu chi của người cao tuổi (017)

**Ngày**: 2026-10-22 (lập lại theo spec viết lại; bản 2026-10-10 bỏ) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bảng nằm trong schema M11 cùng bảng của 010. Người cao tuổi, hợp đồng, bảng chi phí, đề nghị mua hộ, người thân, tệp, yêu cầu phê duyệt là tham chiếu logic (`BIGINT`). Số tiền `BIGINT` (đồng, có dấu). Thời điểm `DATETIME(6)`, ngày `DATE` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ: `giao_dich` (tính vào số bảng của 010).
- `su_kien_chung`: `phieu_bao_phi`, `bang_quyet_toan` (loại `M11.FEE_NOTICE`, `M11.SETTLEMENT_STATEMENT`; mỗi phiên bản bảng quyết toán là một sự kiện, xác nhận quyết toán là sự kiện `M11.SETTLEMENT_CONFIRMATION` trỏ phiên bản; tệp PDF, Excel gắn chủ là id sự kiện) *(sửa 2026-10-29, mục tiêu 79 bảng)*.
- `danh_muc_chung`: `ngan_hang`; `lich_su_chung`: `lich_su_tinh_trang`; `trang_thai_nct`: `so_du` (số dư dẫn xuất trong `du_lieu`, cột sinh có chỉ mục).
- Bảng phụ, bảng con *(bổ sung 2026-10-30, rà đủ tên bảng của data-model)*: `lan_xuat_so_du`, `lan_tai_chung_tu` → hành động `nhat_ky`; `su_kien_m11sd_da_xu_ly`, `lan_chay_m11sd` → `moc_da_nhac`. Không thêm bảng riêng.

### 0.1. Lược đồ sau gộp *(bổ sung 2026-10-30; sinh từ mục 0 và ràng buộc ở mô tả bảng các mục sau; chủ module rà, hoàn thiện khi viết migration, task T046)*

Quy ước: loại ở bảng chung đặt mã `<module>.<TÊN_BẢNG_CŨ>` (module M11; đổi sang tên tiếng Anh theo glossary khi dịch); `noi_dung`, `thuoc_tinh` hoặc `du_lieu` mang các cột của bảng cũ, có JSON Schema theo loại; bảng gộp riêng của feature có cột `kind` (hoặc `loai`) và cột `NULL` có `CHECK` theo loại. Mỗi dòng có ràng buộc ở bảng dưới cần một test tích hợp chứng minh ràng buộc vẫn giữ sau gộp (constitution XII), ghi vào bảng DBR áp dụng khi cài.

**Bảng giữ hoặc là bảng đích** (giữ cột, ràng buộc như mô tả ở các mục sau; bảng đích nhận thêm cột của bảng gộp vào): `giao_dich`.

| Bảng cũ | Đích | Loại ở bảng chung | Ràng buộc của bảng cũ | Cách giữ sau gộp |
| --- | --- | --- | --- | --- |
| `so_du` | `trang_thai_nct` | `M11.SO_DU` | / `ma_nop_tien` `UNIQUE` / Không đổi, không dùng lại (research E-04) / FR-001, DBR-28 / | `khoa_duy_nhat` |
| `ngan_hang` | `danh_muc_chung` | `M11.NGAN_HANG` | — | cột vào `JSON` hoặc cột của bảng đích |
| `lich_su_tinh_trang` | `lich_su_chung` | `M11.LICH_SU_TINH_TRANG` | `id`, `nct_id`, `ngay`, `nguon` (`HANG_NGAY`, `GIAO_DICH:<id>`), `so_du`, `tong_chua_chot`, `phi_mot_ngay`, `so_ngay_con_du NULL`, `tinh_trang`, `da_bao` (`NGAY`, `NHAC_LAI`, `NOP_TIEN_DU`, `NOP_TIEN_CHUA_DU`, `BAO_QUAN_LY`, `KHONG`). `UNIQUE (nct_id, ngay, ng… | `khoa_duy_nhat` |
| `phieu_bao_phi` | `su_kien_chung` | `M11.PHIEU_BAO_PHI` | / `id`, `ma_phieu` `UNIQUE` (`PBP-<yyyyMM>-<bangId>`) / / DBR-37 /; / `bang_chi_phi_id` `UNIQUE`, `loai_bang`, `ky`, `nct_id` / Một phiếu mỗi bảng / FR-031, DBR-37 / | `khoa_duy_nhat` |
| `bang_quyet_toan` | `su_kien_chung` | `M11.BANG_QUYET_TOAN` | / `phien_ban` / `UNIQUE (nct_id, phien_ban)`; tăng dần mỗi hồ sơ / DBR-38 /; / `hien_hanh` / `BOOLEAN`; cột sinh `khoa_hien_hanh = IF(hien_hanh, nct_id, NULL)` `UNIQUE` / DBR-38 /; / `ban_ky_tep_id NULL`, `ban_ky_luc NULL`, `ban_ky_boi_id NULL` / Đặt một lần (… | `khoa_duy_nhat`; trigger của bảng chung (chỉ thêm hoặc `khoa_luc`) |
| `lan_xuat_so_du` | hành động `nhat_ky` | — | — | cột vào `JSON` hoặc cột của bảng đích |
| `lan_tai_chung_tu` | hành động `nhat_ky` | — | — | cột vào `JSON` hoặc cột của bảng đích |
| `su_kien_m11sd_da_xu_ly` | `moc_da_nhac` | `M11.SU_KIEN_M11SD_DA_XU_LY` | — | cột vào `JSON` hoặc cột của bảng đích |
| `lan_chay_m11sd` | `moc_da_nhac` | `M11.LAN_CHAY_M11SD` | — | cột vào `JSON` hoặc cột của bảng đích |

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `so_du` | 2 dẫn xuất | A, D (một dòng mỗi hồ sơ: số dư, số dư cọc, mã nộp tiền, tình trạng) *(R-15: `so_du` → `trang_thai_nct`)* |
| `giao_dich` | 2 → 3 khi Đã xác nhận | B, C |
| `lich_su_tinh_trang` | 3 | D *(R-15: `lich_su_tinh_trang` → `lich_su_chung`)* |
| `phieu_bao_phi` | 3 | J (FR-031, FR-032) *(R-15: `phieu_bao_phi` → `su_kien_chung`)* |
| `bang_quyet_toan` | 3, có phiên bản | E, J (FR-034) *(R-15: `bang_quyet_toan` → `su_kien_chung`)* |
| `lan_xuat_so_du` | 3 | F (FR-027) *(R-15: `lan_xuat_so_du` → hành động `nhat_ky`)* |
| `lan_tai_chung_tu` | 3 | J (FR-037) *(R-15: `lan_tai_chung_tu` → hành động `nhat_ky`)* |
| `su_kien_m11sd_da_xu_ly`, `lan_chay_m11sd` | Kỹ thuật | Listener, job *(R-15: `su_kien_m11sd_da_xu_ly`, `lan_chay_m11sd` → `moc_da_nhac`)* |
| `ngan_hang` *(bổ sung 2026-10-22)* | 1, cố định | A (danh mục ngân hàng cho CFG-M11-10 và VietQR) *(R-15: `ngan_hang` → `danh_muc_chung`)* |

Không còn các bảng của bản cũ: `day_so_phieu`, `chot_quy_ngay`, `chot_quy_ngay_phieu`, `lan_nhap_sao_ke`, `dong_sao_ke` (Q-385).

---

## 2. `so_du` – nhóm 2 dẫn xuất → gộp theo R-15: `so_du` → `trang_thai_nct`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `nct_id` (khóa chính), `mo_luc`, `hop_dong_mo_id` | Mở khi hợp đồng đầu tiên chuyển Chờ ký | FR-001 |
| `ma_nop_tien` `UNIQUE` | Không đổi, không dùng lại (research E-04) | FR-001, DBR-28 |
| `so_du`, `so_du_coc` | = tổng giao dịch Đã xác nhận của từng sổ | FR-002, DBR-28 *(R-15: `so_du` → `trang_thai_nct`)* |
| `tinh_trang` (`BINH_THUONG`, `SAP_HET_TIEN`, `CON_NO`), `so_ngay_con_du NULL`, `chu_ky_tu`, `lan_bao_truoc`, `con_no_tu` | | FR-016 → FR-019 |
| `dung_nhac`, `dung_nhac_boi_id`, `dung_nhac_ly_do` | Chỉ hồ sơ trạng thái cuối còn nợ | FR-020 |
| `can_hoan_coc_tu NULL`, `nhac_hoan_coc_luc NULL` | Hợp đồng hủy trước Hiệu lực, Hủy tiếp nhận còn cọc | FR-015a |
| `nhac_sau_trang_thai_cuoi_luc NULL` | | FR-024 |
| `phien_ban` | | research E-03 |

Chỉ cập nhật trong transaction ghi giao dịch Đã xác nhận, lần tính tình trạng, lệnh dừng nhắc hoặc listener nhắc hoàn cọc.

---

## 2a. `ngan_hang` – nhóm 1, cố định *(bổ sung 2026-10-22; research E-04)* → gộp theo R-15: `ngan_hang` → `danh_muc_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `ma_bin` (khóa chính) | `CHAR(6)`, chỉ chữ số; mã NAPAS cho VietQR | FR-003 |
| `ten_viet_tat`, `ten_day_du` | | FR-003 |
| `con_hieu_luc` | `BOOLEAN`; tắt thay cho xóa | |

Nạp và đổi chỉ bằng migration; tài khoản ứng dụng chỉ có quyền `SELECT`.

---

## 3. `giao_dich` – nhóm 2 → 3

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `so` (`SO_DU`, `COC`) | | FR-010 |
| `loai` (`NOP_TIEN_MAT`, `NOP_CHUYEN_KHOAN`, `THANH_TOAN_BANG`, `HOAN_TIEN`, `CAN_TRU_COC`, `DIEU_CHINH`, `THU_COC`, `HOAN_COC`) | `CHECK` cặp (`so`, `loai`) hợp lệ theo FR-010 | FR-010 |
| `so_tien` (có dấu), `thoi_diem`, `phuong_thuc` (`TIEN_MAT`, `CHUYEN_KHOAN`, `NOI_BO`, `KHAC`) | `CHECK (so_tien > 0)` với loại nộp, thu cọc; `< 0` với hoàn; thanh toán bảng theo tổng bảng | FR-005, FR-007, FR-010 |
| `ma_giao_dich_ngan_hang NULL` | Bắt buộc với chuyển khoản (`CHECK`); chỉ mục thường, không `UNIQUE` (FR-007) | FR-007, DBR-29 |
| `bang_chi_phi_id NULL` | `UNIQUE`; chỉ với `THANH_TOAN_BANG` | FR-012, DBR-30 |
| `giao_dich_goc_id NULL` | Điều chỉnh trỏ giao dịch được sửa; không trỏ `THANH_TOAN_BANG` (backend kiểm) | FR-011 |
| `giao_dich_lien_ket_id NULL` | Cặp cấn trừ cọc | FR-015 |
| `nguoi_nop NULL`, `nguoi_nhan` `JSON NULL` (người thân id hoặc họ tên người nhận khác), `ghi_chu NULL` | | FR-005, FR-007, FR-014 |
| `lap_boi_id` (Kế toán; rỗng = Hệ thống với thanh toán bảng), `yeu_cau_phe_duyet_id NULL`, `duyet_boi_id NULL`, `ly_do NULL` | `ly_do` bắt buộc với điều chỉnh | FR-013 |
| `trang_thai` (`CHO_DUYET`, `DA_XAC_NHAN`, `TU_CHOI`, `DA_HUY`), `xac_nhan_luc NULL` | | Bảng trạng thái giao dịch |
| `da_dieu_chinh` | Đặt một lần khi điều chỉnh trỏ tới được duyệt | FR-011 |
| `xac_nhan_trung_ma` | Kế toán đã xác nhận lại khi mã ngân hàng đã dùng | FR-007 |

Chỉ mục `(nct_id, so, trang_thai, xac_nhan_luc)` (sổ, sao kê), `(ma_giao_dich_ngan_hang)`, `(trang_thai, loai)` (hộp duyệt). Trigger: khi `OLD.trang_thai = 'DA_XAC_NHAN'` chỉ cho đặt `da_dieu_chinh` từ false; khi `OLD.trang_thai IN ('TU_CHOI','DA_HUY')` chặn mọi đổi; `DELETE` bị chặn.

| Từ | Lệnh / sự kiện | Đến | Người | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Ghi nộp tiền mặt, thu cọc tiền mặt | `DA_XAC_NHAN` | Kế toán | FR-005 | Sổ; đặt cọc; tình trạng |
| — | Ghi nhận chuyển khoản (nộp, thu cọc) | `DA_XAC_NHAN` | Kế toán | FR-007; trùng mã cần `xac_nhan_trung_ma` | Sổ; đặt cọc; tình trạng |
| — | Bảng chi phí Đã chốt | `DA_XAC_NHAN` | Hệ thống | FR-012, `UNIQUE (bang_chi_phi_id)` | Sổ; tình trạng; điều kiện quyết toán |
| — | Cấn trừ cọc (cặp) | `DA_XAC_NHAN` | Kế toán | FR-015 | Sổ cọc giảm, số dư tăng |
| — | Lập hoàn tiền, hoàn cọc, điều chỉnh | `CHO_DUYET` | Kế toán | Lý do (điều chỉnh); FR-011, FR-014 | Yêu cầu phê duyệt `M11.GIAO_DICH_SO_DU`; báo Quản lý viện |
| `CHO_DUYET` | Duyệt | `DA_XAC_NHAN` | Quản lý viện | Kiểm lại FR-011, FR-014 | Sổ; `da_dieu_chinh` của gốc; tình trạng; đặt cọc; điều kiện quyết toán |
| `CHO_DUYET` | Từ chối | `TU_CHOI` | Quản lý viện | Lý do | Báo Kế toán |
| `CHO_DUYET` | Hủy | `DA_HUY` | Kế toán | Lý do | — |

---

## 4. `lich_su_tinh_trang` – nhóm 3 → gộp theo R-15: `lich_su_tinh_trang` → `lich_su_chung`

`id`, `nct_id`, `ngay`, `nguon` (`HANG_NGAY`, `GIAO_DICH:<id>`), `so_du`, `tong_chua_chot`, `phi_mot_ngay`, `so_ngay_con_du NULL`, `tinh_trang`, `da_bao` (`NGAY`, `NHAC_LAI`, `NOP_TIEN_DU`, `NOP_TIEN_CHUA_DU`, `BAO_QUAN_LY`, `KHONG`). `UNIQUE (nct_id, ngay, nguon)` (SC-012, NFR-04). *(R-15: `so_du` → `trang_thai_nct`)*

**Bảng chuyển tình trạng** (khớp bảng của spec mục D): thực hiện bằng hàm `QuyetDinhBao` (research E-07); tình trạng hiện tại lưu ở `so_du`. *(R-15: `so_du` → `trang_thai_nct`)*

---

## 5. `phieu_bao_phi` – nhóm 3 *(Q-377, Q-378)* → gộp theo R-15: `phieu_bao_phi` → `su_kien_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `ma_phieu` `UNIQUE` (`PBP-<yyyyMM>-<bangId>`) | | DBR-37 |
| `bang_chi_phi_id` `UNIQUE`, `loai_bang`, `ky`, `nct_id` | Một phiếu mỗi bảng | FR-031, DBR-37 |
| `giao_dich_thanh_toan_id` | Giao dịch `THANH_TOAN_BANG` của bảng | FR-031 |
| `so_du_truoc`, `so_du_sau`, `tong` | | FR-032 |
| `so_lieu` `JSON` | Bản chụp nội dung phiếu (khoản, tổng theo loại, người đại diện, thông tin nộp tiền) | FR-037 |
| `tep_pdf_id`, `tep_xlsx_id` | `common.tep`, loại `M11.PHIEU_BAO_PHI` | FR-032, FR-037 |
| `phat_hanh_boi_id`, `phat_hanh_luc` | | FR-032 |

Không `UPDATE`, `DELETE` (mẫu 000). Không có trạng thái: phát hành là tạo dòng.

---

## 6. `bang_quyet_toan` – nhóm 3, có phiên bản *(Q-379, Q-385)* → gộp theo R-15: `bang_quyet_toan` → `su_kien_chung`

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id` | | FR-034 |
| `can_cu_loai` (`HO_SO_KET_THUC`, `DANH_SACH_SAU_QUA_DOI`, `SAU_TRANG_THAI_CUOI`), `can_cu_id NULL` | Hồ sơ kết thúc lưu trú hoặc danh sách việc sau qua đời (004) | FR-034, DBR-38 |
| `phien_ban` | `UNIQUE (nct_id, phien_ban)`; tăng dần mỗi hồ sơ | DBR-38 |
| `hien_hanh` | `BOOLEAN`; cột sinh `khoa_hien_hanh = IF(hien_hanh, nct_id, NULL)` `UNIQUE` | DBR-38 |
| `co_ngoai_le` | Ba ý tiền được miễn bằng ngoại lệ đã duyệt của 004 | FR-022, FR-034 |
| `so_con_no_hoac_phai_hoan` | Khác 0 chỉ khi `co_ngoai_le` | FR-034 |
| `giao_dich_cuoi_id` | Giao dịch Đã xác nhận mới nhất của hồ sơ lúc xác nhận; giao dịch sau đó làm ý xác nhận Chưa đạt | FR-022, FR-034 |
| `so_lieu` `JSON` | Bản chụp nội dung bảng (research E-09) | FR-034, FR-037 |
| `tep_pdf_id` | `common.tep`, loại `M11.BANG_QUYET_TOAN` | FR-034 |
| `ban_ky_tep_id NULL`, `ban_ky_luc NULL`, `ban_ky_boi_id NULL` | Đặt một lần (trigger) | FR-034 |
| `xac_nhan_boi_id`, `xac_nhan_luc` | | FR-034 |

Trigger: chỉ cho `UPDATE` `hien_hanh` từ true sang false và ba cột bản ký từ `NULL`; `DELETE` bị chặn.

| Từ | Lệnh | Đến | Người | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Xác nhận quyết toán | Phiên bản mới, hiện hành; bản trước không hiện hành | Kế toán | Research E-09 (căn cứ; ba ý tiền hoặc ngoại lệ; không còn Chờ duyệt) | PDF; `DieuKienQuyetToanDaDoi`; thông báo người thân; `capNhat` danh sách việc sau qua đời |
| Hiện hành | Đính kèm bản ký | Giữ | Kế toán | Chưa có bản ký | — |

---

## 7. Xuất và tải

- `lan_xuat_so_du` (nhóm 3): `id`, `boi_id`, `vai_tro` (`KE_TOAN`, `QUAN_LY_VIEN`, `NGUOI_THAN`), `luc`, `loai` (`SAO_KE`, `THU_CHI`), `dinh_dang` (`XLSX`, `PDF`), `tu`, `den`, `pham_vi` `JSON` (danh sách hồ sơ), `so_dong`, `tep_id` (loại `M11.FILE_XUAT`). FR-027. *(R-15: `lan_xuat_so_du` → hành động `nhat_ky`)*
- `lan_tai_chung_tu` (nhóm 3): `id`, `loai` (`PHIEU_BAO_PHI`, `BANG_QUYET_TOAN`, `FILE_XUAT`), `doi_tuong_id`, `dinh_dang`, `boi_id`, `vai_tro`, `luc`. FR-037 (cùng dòng nhật ký `CHUNG_TU_TAI`). *(R-15: `lan_tai_chung_tu` → hành động `nhat_ky`)*

---

## 8. Nhật ký, phân loại trường, lưu giữ

- Loại đối tượng nhật ký: `M11.SO_DU`, `M11.GIAO_DICH_SO_DU`, `M11.PHIEU_BAO_PHI`, `M11.BANG_QUYET_TOAN`, `M11.XUAT_SO_DU`. Hành động: `SD_MO`, `GD_NOP_TIEN_MAT`, `GD_NOP_CHUYEN_KHOAN`, `GD_THANH_TOAN_BANG`, `GD_LAP`, `GD_DUYET`, `GD_TU_CHOI`, `GD_HUY`, `GD_CAN_TRU_COC`, `SD_DUNG_NHAC`, `PBP_PHAT_HANH`, `BQT_XAC_NHAN`, `BQT_BAN_KY`, `XUAT`, `CHUNG_TU_TAI`.
- `PhanLoaiTruongProvider`: mọi trường tiền, mã nộp tiền, thông tin nộp tiền → `TAI_CHINH` (người thân theo chú thích ¹⁶; Hành chính chỉ thấy số dư, số dư cọc, trạng thái đặt cọc).
- `LuuTruProvider`: mọi bảng trên và tệp gắn `THEO_HO_SO` (CFG-M15-03) (FR-029).

---

## 9. DBR và quy tắc

| Mã | Cách bảo đảm | Test |
| --- | --- | --- |
| DBR-17 | Giao dịch Đã xác nhận không sửa (trigger); thanh toán bảng không điều chỉnh (backend) | `DBR_17_GiaoDichKhongSuaIT` |
| DBR-28 | `so_du` = tổng giao dịch Đã xác nhận, cập nhật cùng transaction; đối chiếu hằng đêm; mã nộp tiền `UNIQUE` | `DBR_28_SoDuBatBienIT`, SC-001 *(R-15: `so_du` → `trang_thai_nct`)* |
| DBR-29 | Mã nộp tiền `UNIQUE`; mã giao dịch ngân hàng trùng → cảnh báo, không chặn | `BR_M11_12_ChuyenKhoanIT` |
| DBR-30 | `UNIQUE (bang_chi_phi_id)` trên giao dịch thanh toán bảng | `DBR_30_ThanhToanBangIT` |
| DBR-37 | `UNIQUE (ma_phieu)`, `UNIQUE (bang_chi_phi_id)` trên `phieu_bao_phi`; không `UPDATE`, `DELETE` | `BR_M11_21_PhieuBaoPhiIT` *(R-15: `phieu_bao_phi` → `su_kien_chung`)* |
| DBR-38 | `UNIQUE (nct_id, phien_ban)`; `khoa_hien_hanh` `UNIQUE`; trigger | `BR_M11_22_XacNhanQuyetToanIT` |
| BR-M11-10, 11, 14 | Bảng chuyển mục 3; `GioiHanHoan`; handler duyệt | `BR_M11_10_DieuChinhIT`, `BR_M11_11_ThanhToanBangIT`, `BR_M11_14_HoanTienIT` |
| BR-M11-12, 16 | Lệnh nộp tiền mặt, chuyển khoản | `BR_M11_12_ChuyenKhoanIT`, `BR_M11_16_TienMatIT` |
| BR-M11-13 | `TinhTrang`, `QuyetDinhBao`, job | `BR_M11_13_TinhTrangIT` |
| BR-M11-15 | `SoDuMuaHoPort` | `BR_M11_15_DauMuaHoIT` |
| BR-M11-21, 23 | `phieu_bao_phi`; `common.bieumau`; tệp | `BR_M11_21_PhieuBaoPhiIT`, `BR_M11_23_ChungTuIT` *(R-15: `phieu_bao_phi` → `su_kien_chung`)* |
| BR-M11-22 | `bang_quyet_toan`; `DieuKienQuyetToan` | `BR_M11_22_XacNhanQuyetToanIT`, `FR_022_DieuKienQuyetToanIT` *(R-15: `bang_quyet_toan` → `su_kien_chung`)* |
