# Data Model: Tài khoản và phân quyền (002)

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, trường, ràng buộc và cách bảo đảm từng quy tắc. Đây không phải script SQL; migration được viết ở giai đoạn triển khai. Quy ước kiểu dữ liệu, nhóm dữ liệu và bảo đảm ở cơ sở dữ liệu theo [../000-shared-foundation/data-model.md](../000-shared-foundation/data-model.md) mục 1.

Mọi bảng nằm trong schema của ứng dụng, thuộc module M15 (gói `m15.taikhoan`). Module khác không đọc các bảng này, chỉ gọi giao diện ở [contracts/module-interfaces.md](contracts/module-interfaces.md).

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Nội dung | FR |
| --- | --- | --- | --- |
| `tai_khoan` | 1 (vai trò), 2 (trạng thái), theo 3.2 | Tài khoản, trạng thái, mật khẩu | FR-001 → FR-015 |
| `lich_su_trang_thai_tai_khoan` | 3, chỉ thêm | Mỗi lần chuyển trạng thái | FR-015, FR-041a |
| `phien_dang_nhap` | Kỹ thuật | Phiên, thiết bị | FR-013, FR-014a |
| `thiet_bi` | Kỹ thuật | Lần kết nối gần nhất của thiết bị | FR-041c |
| `thiet_bi_gan` | 2 | *(bổ sung 2026-10-18)* Thiết bị gắn với tài khoản người cao tuổi, token thiết bị | FR-070, FR-071 |
| `ma_qr_dang_nhap` | 2 | *(bổ sung 2026-10-18)* Mã QR đăng nhập dùng một lần | FR-069 |
| `vai_tro` | 1, cố định | 10 vai trò (thêm AC-16, Q-365) | FR-016 |
| `ma_tran_quyen` | 1, cố định | Quyền tối đa theo Phụ lục 27 và 19.3 | FR-018, FR-020 |
| `gan_vai_tro` | 1 có lịch sử | Tài khoản – vai trò | FR-017, FR-026 |
| `thu_hep_quyen` | 1 có lịch sử | Thu hẹp theo vai trò hoặc tài khoản | FR-025 |
| `quyen_gan_them` | 1 có lịch sử | Duyệt kế hoạch chăm sóc cho Điều dưỡng | FR-024 |
| `quy_tac_xem_truong` | 1, cố định | Vai trò × nhóm thông tin | FR-022 |
| `nghiep_vu_chuyen_mon`, `nghiep_vu_chuyen_mon_pham_vi` | 1 | Danh mục nghiệp vụ chuyên môn | FR-036, FR-037 |
| `ban_ghi_cho_xem_lai` | 2 | Bản ghi ngoại tuyến thời điểm bất hợp lý | FR-041c |
| `loi_du_lieu_quyen` | Kỹ thuật | Gộp báo lỗi dữ liệu nguồn | FR-041b |
| `da_canh_bao_giay_phep` | Kỹ thuật | Chống gửi lặp cảnh báo giấy phép | FR-039 |

"Cố định" là nạp từ migration, không có API tạo, sửa, xóa; đổi bằng migration khi tài liệu nguồn đổi.

---

## 2. Tài khoản, trạng thái, phiên (mục A, B)

### `tai_khoan`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | Khóa chính | |
| `ten_dang_nhap` | `VARCHAR(64)` | `UNIQUE`; không đổi sau khi tạo | FR-001, CHK024 |
| `loai` | `ENUM('NHAN_VIEN','NGUOI_THAN','NGUOI_CAO_TUOI')` | Không đổi | FR-001 (`NGUOI_CAO_TUOI` bổ sung 2026-10-18, Q-365) |
| `nhan_vien_id` | `BIGINT NULL` | `UNIQUE`; tham chiếu logic hồ sơ nhân viên (008) | FR-002 |
| `nguoi_than_id` | `BIGINT NULL` | `UNIQUE`; tham chiếu logic hồ sơ người thân (012) | FR-002 |
| `nguoi_cao_tuoi_id` | `BIGINT NULL` | `UNIQUE`; tham chiếu logic hồ sơ người cao tuổi (001); bổ sung 2026-10-18 | FR-067 |
| `trang_thai` | `ENUM('HOAT_DONG','KHOA_TAM','KHONG_HOAT_DONG','DA_KHOA')` | | FR-007 |
| `nguyen_nhan_khoa` | `ENUM('THU_CONG','NGHI_VIEC','HET_HAN_SAU_KET_THUC','HET_QUAN_HE','NCT_KET_THUC_LUU_TRU','NCT_TU_YEU_CAU','CO_LY_DO','THEO_DE_NGHI_NGUOI_THAN') NULL` | Có khi `DA_KHOA`; dùng cho FR-011, FR-073 (bốn giá trị cuối bổ sung 2026-10-18) | FR-011 |
| `khoa_tam_den` | `DATETIME(6) NULL` | Có khi `KHOA_TAM` | FR-008 |
| `so_lan_sai_lien_tiep` | `INT` | `>= 0` | FR-008 |
| `mat_khau_bam` | `VARCHAR(255)` | Argon2id | A-03 |
| `buoc_doi_mat_khau` | `BOOLEAN` | | FR-006 |
| `lan_dang_nhap_cuoi` | `DATETIME(6) NULL` | | FR-001, FR-010 |
| `moc_tinh_khong_hoat_dong` | `DATETIME(6)` | Tạo, kích hoạt lại, mở khóa, hoặc đăng nhập, lấy mốc muộn nhất | FR-010, CHK015 |
| `phien_ban` | `INT` | Khóa lạc quan cho lệnh trạng thái | 000 FR-040 |
| `phien_ban_quyen` | `INT` | Tăng khi gán, gỡ vai trò, thu hẹp hay gán thêm theo tài khoản | FR-026, FR-027 |
| `tao_luc`, `tao_boi` | | | |

Ràng buộc:
- `CHECK` mỗi loại có đúng một liên kết: `NHAN_VIEN` → `nhan_vien_id`; `NGUOI_THAN` → `nguoi_than_id`; `NGUOI_CAO_TUOI` → `nguoi_cao_tuoi_id`; hai cột còn lại `NULL` (FR-002, FR-003, FR-067).
- Không có quyền `DELETE` cho `eldercare_app` và có trigger chặn xóa (FR-005), theo mẫu 1.4 của 000.
- Hồ sơ nhân viên không ở Nghỉ việc, có chức danh ứng với vai trò hệ thống (FR-004, FR-004a); hồ sơ người thân có ít nhất một quan hệ với người cao tuổi chưa ở trạng thái cuối (FR-050): kiểm ở backend qua API của 008, 012 khi tạo.
- *(Bổ sung 2026-10-18)* Tài khoản người cao tuổi: người cao tuổi Đang lưu trú qua `NguoiCaoTuoiApi.trangThai` (001); lệnh có `xacNhan` của chính người cao tuổi (A-15).

### Bảng chuyển trạng thái tài khoản

| Từ | Lệnh / sự kiện (endpoint) | Đến | Kiểm tra ở backend | Nhật ký (`hanh_dong`) |
| --- | --- | --- | --- | --- |
| — | `POST /tai-khoan` (nhân viên) | `HOAT_DONG` | Quyền `(TAI_KHOAN_PHAN_QUYEN, TAO)` của AC-01; FR-002 → FR-004a; ít nhất một vai trò AC-01 → 05, 07, 09, 14 | `TK_TAO` |
| — | `POST /tai-khoan-nguoi-than` | `HOAT_DONG` | AC-01 hoặc AC-09 (¹²); FR-050; `cachXacMinh` (FR-052); vai trò duy nhất AC-10 | `TK_TAO` (+`cachXacMinh`) |
| `HOAT_DONG` | đăng nhập sai đủ ngưỡng | `KHOA_TAM` | Ngưỡng CFG-M15-01 | `TK_KHOA_TAM` (Bộ lập lịch, căn cứ CFG-M15-01) |
| `KHOA_TAM` | hết `khoa_tam_den` (tính khi có yêu cầu kế tiếp, A-03) | `HOAT_DONG` | `khoa_tam_den <= bây giờ` | `TK_HET_KHOA_TAM` (Bộ lập lịch) |
| `KHOA_TAM` | `POST …/mo-khoa-som` | `HOAT_DONG` | AC-01; hoặc Trưởng tầng, Người phụ trách ca với tài khoản nhân viên cùng tầng/khu và cùng ca (FR-019c); `lyDo`; xác minh trực tiếp | `TK_MO_KHOA_SOM` (+`canCu`) |
| `HOAT_DONG` | job `ChuyenKhongHoatDong` | `KHONG_HOAT_DONG` | Chỉ người thân; `moc_tinh_khong_hoat_dong` quá CFG-M15-02 | `TK_KHONG_HOAT_DONG` (Bộ lập lịch) |
| `KHONG_HOAT_DONG` | `POST …/kich-hoat-lai` | `HOAT_DONG` | AC-01 hoặc AC-09; `lyDo`; `cachXacMinh`; điều kiện FR-011 với người thân | `TK_KICH_HOAT_LAI` |
| `HOAT_DONG`, `KHOA_TAM`, `KHONG_HOAT_DONG` | `POST …/khoa` | `DA_KHOA` | AC-01; `lyDo`; FR-012; nếu đang là Phụ trách bộ phận thì có `nguoiThayPhuTrach` (gọi 008 trong cùng transaction, Q-292) | `TK_KHOA` |
| `HOAT_DONG`, `KHOA_TAM`, `KHONG_HOAT_DONG` | `TaiKhoanApi.khoaDoNghiViec` (lệnh của 008) | `DA_KHOA` | Cùng transaction với Nghỉ việc | `TK_KHOA` (Bộ lập lịch, căn cứ BR-M09-04, lệnh nguồn) |
| `HOAT_DONG`, `KHOA_TAM`, `KHONG_HOAT_DONG` | job `KhoaSauKetThucLuuTru`; hoặc `TaiKhoanApi.xetLaiNguoiThan` (lệnh của 012) | `DA_KHOA` | Người thân; CFG-M01-04 (5.6) hoặc hết quan hệ Hiệu lực (BR-M15-14) | `TK_KHOA` (Bộ lập lịch, căn cứ) |
| — | `POST /tai-khoan-nguoi-cao-tuoi` *(bổ sung 2026-10-18)* | `HOAT_DONG` | AC-01 hoặc AC-09; FR-067; `xacNhan` (cách, nhân viên nghe); vai trò duy nhất AC-16 | `TK_TAO` (+`xacNhan`) |
| `HOAT_DONG`, `KHOA_TAM` | sự kiện `NguoiCaoTuoiDaChuyenTrangThai` sang trạng thái cuối *(bổ sung 2026-10-18)* | `DA_KHOA` | Tài khoản người cao tuổi; `nguyen_nhan_khoa = NCT_KET_THUC_LUU_TRU`; gỡ thiết bị (A-16) | `TK_KHOA` (Bộ lập lịch, căn cứ sự kiện) |
| `HOAT_DONG`, `KHOA_TAM` | `POST …/khoa-nguoi-cao-tuoi`; hoặc `apDung` của yêu cầu `M15.KHOA_TAI_KHOAN_NCT` *(bổ sung 2026-10-18)* | `DA_KHOA` | AC-01 hoặc AC-09, `canCu`, `lyDo`; yêu cầu: AC-01 duyệt (A-16) | `TK_KHOA` |
| `DA_KHOA` | `POST …/mo-khoa-nguoi-cao-tuoi` *(bổ sung 2026-10-18)* | `HOAT_DONG` | AC-01 hoặc AC-09; `xacNhan`; `nguyen_nhan_khoa <> NCT_KET_THUC_LUU_TRU` (FR-073) | `TK_MO_KHOA` |
| `DA_KHOA` | `POST …/mo-khoa` | `HOAT_DONG` | AC-01; `lyDo`; nguyên nhân khóa tự động không còn (FR-011) | `TK_MO_KHOA` |

Mọi chuyển sang trạng thái khác `HOAT_DONG` kết thúc mọi phiên của tài khoản trong cùng transaction (FR-013). Mọi chuyển thêm một dòng `lich_su_trang_thai_tai_khoan`. Không có trạng thái kết thúc (FR-005).

Lệnh không chuyển trạng thái nhưng có nhật ký: `POST …/cap-lai-mat-khau` (AC-01 với nhân viên; AC-01, AC-09 với người thân; có `cachXacMinh` với người thân; đặt `buoc_doi_mat_khau`) → `TK_CAP_LAI_MAT_KHAU`; `POST /xac-thuc/doi-mat-khau` → `TK_DOI_MAT_KHAU`; đăng nhập → `DANG_NHAP` / `DANG_NHAP_THAT_BAI`. *(Bổ sung 2026-10-18)* `POST …/ma-qr` → `QR_TAO`; `POST /xac-thuc/dang-nhap-qr` → `QR_DUNG`, `TB_GAN` (và `TB_GO` cho thiết bị cũ); `POST …/go-thiet-bi` → `TB_GO`.

### `lich_su_trang_thai_tai_khoan` – nhóm 3, chỉ thêm

| Cột | Ghi chú |
| --- | --- |
| `id`, `tai_khoan_id` | |
| `trang_thai`, `nguyen_nhan_khoa` | Trạng thái mới |
| `hieu_luc_tu` | `DATETIME(6)`; `UNIQUE (tai_khoan_id, hieu_luc_tu)` |
| `nguoi_thuc_hien_id`, `nhat_ky_id` | |

Trạng thái tại thời điểm `t` là dòng có `hieu_luc_tu` lớn nhất mà `<= t` (FR-041a).

### `phien_dang_nhap` – kỹ thuật

| Cột | Ghi chú |
| --- | --- |
| `ma_bam_token` | `BINARY(32)`, khóa chính (SHA-256 của token) |
| `tai_khoan_id` | |
| `ma_thiet_bi` | `CHAR(36)`; chỉ mục `(ma_thiet_bi, ket_thuc_luc)` |
| `kenh` | `ENUM('WEB','APP_NHAN_VIEN','APP_NGUOI_THAN','APP_NGUOI_CAO_TUOI')` (Q-305; kênh cuối bổ sung 2026-10-18, Q-370) |
| `bat_dau_luc`, `hoat_dong_cuoi_luc`, `ket_thuc_luc NULL`, `ly_do_ket_thuc` | `ly_do_ket_thuc`: `DANG_XUAT`, `TAI_KHOAN_ROI_HOAT_DONG`, `THIET_BI_DANG_NHAP_KHAC`, `HET_THOI_GIAN` (quá CFG-M15-10 của kênh, FR-013a); *(bổ sung 2026-10-18)* `GO_THIET_BI`, `DOI_MAT_KHAU`; kênh `APP_NGUOI_CAO_TUOI` không có `HET_THOI_GIAN` (FR-013a) |

- Đăng nhập: trong một transaction, đặt `ket_thuc_luc` cho mọi phiên đang mở có cùng `ma_thiet_bi` (BR-M15-15), rồi thêm phiên mới.
- Phiên đang mở của một thiết bị: tối đa một; bảo đảm bằng cột sinh `thiet_bi_dang_mo = IF(ket_thuc_luc IS NULL, ma_thiet_bi, NULL)` có `UNIQUE`.
- Bảng kỹ thuật được `UPDATE` (`hoat_dong_cuoi_luc`, `ket_thuc_luc`); thay đổi nghiệp vụ tương ứng đã có nhật ký (đăng nhập, khóa).

### `thiet_bi` – kỹ thuật

| Cột | Ghi chú |
| --- | --- |
| `ma_thiet_bi` | Khóa chính |
| `lan_ket_noi_cuoi` | Cập nhật ở mọi yêu cầu trực tuyến |
| `tai_khoan_cuoi_id` | |

Lần kết nối trước khi ngoại tuyến được app gửi kèm phong bì đồng bộ và đối chiếu với `lan_ket_noi_cuoi` trước khi cập nhật (FR-041c).

### `thiet_bi_gan` – nhóm 2 *(bổ sung 2026-10-18, Q-370)*

| Cột | Ghi chú |
| --- | --- |
| `id`, `tai_khoan_id` | Tài khoản loại `NGUOI_CAO_TUOI` |
| `ma_thiet_bi` | `CHAR(36)`, giá trị `X-Thiet-Bi` lúc gắn |
| `ma_bam_token_thiet_bi` | `BINARY(32)`, `UNIQUE` (SHA-256 của token thiết bị, A-15) |
| `gan_luc`, `ma_qr_id NULL` | Mã QR đã dùng để gắn; `NULL` khi gắn bằng mật khẩu dự phòng |
| `go_luc NULL`, `go_boi NULL`, `ly_do_go NULL` | `ly_do_go`: `GAN_MAY_MOI`, `GO_THU_CONG` (kèm `lyDo`), `TAI_KHOAN_DA_KHOA` |

- Tối đa một thiết bị đang gắn cho mỗi tài khoản: cột sinh `tai_khoan_dang_gan = IF(go_luc IS NULL, tai_khoan_id, NULL)` có `UNIQUE` (FR-070).
- Một thiết bị gắn với tối đa một tài khoản: cột sinh `thiet_bi_dang_gan = IF(go_luc IS NULL, ma_thiet_bi, NULL)` có `UNIQUE` (FR-014a).
- Chỉ thêm và đặt `go_*` một lần; không xóa.

### `ma_qr_dang_nhap` – nhóm 2 *(bổ sung 2026-10-18, Q-370)*

| Cột | Ghi chú |
| --- | --- |
| `id`, `tai_khoan_id` | |
| `ma_bam` | `BINARY(32)`, `UNIQUE` (SHA-256 của mã) |
| `tao_boi`, `tao_luc`, `het_han_luc` | `het_han_luc = tao_luc + CFG-M15-11` đọc lúc tạo |
| `dung_luc NULL`, `ma_thiet_bi_dung NULL` | Đặt một lần khi dùng |
| `vo_hieu_luc_luc NULL` | Đặt khi có mã mới của cùng tài khoản |

- Mã dùng được khi `dung_luc`, `vo_hieu_luc_luc` đều `NULL` và `het_han_luc > bây giờ`; kiểm và đánh dấu dưới khóa dòng trong transaction đăng nhập (FR-069).

---

## 3. Vai trò và quyền (mục C)

### `vai_tro` – nhóm 1, cố định

| Cột | Ghi chú |
| --- | --- |
| `ma` | `AC-01` … `AC-05`, `AC-07`, `AC-09`, `AC-10`, `AC-14`, `AC-16` (FR-016; AC-16 bổ sung 2026-10-18, Q-365) |
| `ten`, `loai_tai_khoan` | `NHAN_VIEN`, `NGUOI_THAN` (AC-10) hoặc `NGUOI_CAO_TUOI` (AC-16) |
| `pham_vi_thuc_hien` | `PHAN_CONG` (AC-02, 04, 05) hoặc `TOAN_VIEN` (AC-01, 03, 07, 09, 14); AC-10 theo quan hệ (FR-034, FR-045); AC-16 `BAN_THAN` (FR-068) |
| `phien_ban_quyen` | Tăng khi thu hẹp theo vai trò (A-05) |

### `ma_tran_quyen` – nhóm 1, cố định

| Cột | Ghi chú |
| --- | --- |
| `vai_tro`, `chuc_nang`, `loai_thao_tac` | Khóa chính; `chuc_nang` là một dòng của Phụ lục 27 |
| `pham_vi` | `TOAN_VIEN`, `PHAN_CONG`, `QUAN_HE` |
| `ma_chu_thich` | `JSON`, ví dụ `["1"]`, `["2"]`, `["10"]` |
| `nguon` | `PL27` hoặc `19.3` |

Quy đổi ký hiệu theo research A-04. Các dòng `nguon = '19.3'` (điều chỉnh của 19.3 so với Phụ lục 27, 19.3 là căn cứ):

| Điều chỉnh | Căn cứ |
| --- | --- |
| Quản lý viện: `DUYET`, `TAO` (lập thay) với yêu cầu đổi ca, nghỉ đột xuất của ca phạm vi toàn viện | 19.3, Q-177; spec FR-025 |
| Trưởng tầng: `TAO` (lập thay) yêu cầu nghỉ đột xuất cho nhân viên tầng mình | 19.3; spec 015 |
| Điều dưỡng: mọi `TAO` của Nhân viên chăm sóc trong phạm vi (Phụ lục 27 đã ghi bằng ²⁷, ⁴⁰, ¹⁵…; khi nạp không còn ô nào thiếu nên không có dòng `19.3`, plan Ghi chú T007) | 2.4, Q-214, chú thích ²⁷ |
| Bác sĩ, Hành chính (¹¹), Kế toán (²⁹): phạm vi dashboard, báo cáo toàn viện (chú thích của chính Phụ lục 27 ghi "toàn viện", nên nạp như dòng `PL27` phạm vi `TOAN_VIEN`, plan Ghi chú T007) | 19.3, chú thích ¹¹, ²⁹; spec FR-035 |

Danh sách đầy đủ được rà và chốt ở task nạp dữ liệu; test `MaTranQuyenKhopNguonTest` đối chiếu với tài liệu nguồn (research A-04).

### `gan_vai_tro` – nhóm 1 có lịch sử

| Cột | Ràng buộc |
| --- | --- |
| `id`, `tai_khoan_id`, `vai_tro` | |
| `hieu_luc_tu`, `hieu_luc_den NULL` | `DATETIME(6)`; tại một thời điểm mỗi `(tai_khoan_id, vai_tro)` có tối đa một dòng còn hiệu lực: cột sinh `con_hieu_luc = IF(hieu_luc_den IS NULL, vai_tro, NULL)` có `UNIQUE (tai_khoan_id, con_hieu_luc)` |
| `nguoi_gan_id`, `ly_do` | `ly_do NOT NULL` (FR-026) |

Kiểm ở backend: vai trò cùng loại với tài khoản (FR-003); tài khoản nhân viên luôn còn ít nhất một vai trò (FR-004). *(Bổ sung 2026-10-18)* AC-16 là vai trò duy nhất của tài khoản người cao tuổi, gán lúc tạo (FR-067); `POST …/gan-vai-tro`, `…/go-vai-tro` từ chối tài khoản loại `NGUOI_CAO_TUOI` và từ chối gán AC-16 cho loại khác.

### `thu_hep_quyen` – nhóm 1 có lịch sử

| Cột | Ghi chú |
| --- | --- |
| `id` | |
| `doi_tuong` | `VAI_TRO` hoặc `TAI_KHOAN` |
| `vai_tro NULL`, `tai_khoan_id NULL` | Đúng một cột có giá trị theo `doi_tuong` (`CHECK`) |
| `chuc_nang`, `loai_thao_tac` | |
| `kieu` | `BO_THAO_TAC` hoặc `HA_PHAM_VI` |
| `nhom_chi_tieu NULL`, `kenh NULL` | Chỉ với `DASHBOARD_BAO_CAO` (feature 016 FR-010a) |
| `hieu_luc_tu`, `hieu_luc_den NULL`, `nguoi_thuc_hien_id`, `ly_do` | |

### `quyen_gan_them` – nhóm 1 có lịch sử

| Cột | Ghi chú |
| --- | --- |
| `id`, `tai_khoan_id` | Tài khoản phải có vai trò AC-04 tại lúc gán |
| `chuc_nang`, `loai_thao_tac` | `CHECK (chuc_nang='KE_HOACH_CHAM_SOC' AND loai_thao_tac='DUYET')` (FR-024, Q-07) |
| `hieu_luc_tu`, `hieu_luc_den NULL`, `nguoi_thuc_hien_id`, `ly_do` | |

### Tính quyền vai trò

Thứ tự ở research A-05. Quyền tại thời điểm `t` (FR-026a, BR-M15-16) dùng cùng thuật toán với các dòng có hiệu lực tại `t`.

### `quy_tac_xem_truong` – nhóm 1, cố định

| Cột | Ghi chú |
| --- | --- |
| `vai_tro`, `nhom_thong_tin` | Khóa chính |
| `muc_xem` | `DAY_DU`, `CHI_CO_HAY_KHONG` (nguyện vọng với Hành chính, Kế toán), `KHONG` |

Nguồn: 19.3, chú thích ⁵, ¹⁴, ¹⁶, ³⁰ của Phụ lục 27; spec FR-022. Ví dụ AC-09 và AC-14 với `SUC_KHOE`, `THUOC` là `KHONG`; với `MUC_CHAM_SOC` là `DAY_DU`.

---

## 4. Điều kiện pháp lý (mục E)

### `nghiep_vu_chuyen_mon` – nhóm 1

| Cột | Ghi chú |
| --- | --- |
| `id`, `ma`, `ten` | Theo mẫu nhóm 1 của 000 (`trang_thai`, `ten_hieu_luc`, `phien_ban`) |
| `chuc_nang` | Khóa quyền áp dụng với `loai_thao_tac = CHUYEN_MON` |
| `loai_giay_phep_yeu_cau` | Tham chiếu logic loại giấy phép/đào tạo của 008 |
| `can_pham_vi_co_so` | `BOOLEAN` |
| `mac_dinh` | `BOOLEAN`; `true` thì không ngừng hiệu lực được (FR-036) |

Hai dòng mặc định: chẩn đoán và kê đơn nội bộ (bác sĩ, giấy phép đúng phạm vi, cần phạm vi khám chữa bệnh của cơ sở); phát thuốc và xác nhận liều (điều dưỡng, giấy phép còn hiệu lực).

### `nghiep_vu_chuyen_mon_pham_vi`

| Cột | Ghi chú |
| --- | --- |
| `nghiep_vu_id`, `pham_vi_hanh_nghe_id` | Khóa chính; `pham_vi_hanh_nghe_id` là mục của danh mục phạm vi hành nghề do 008 sở hữu (FR-037, BR-M15-19) |

Thêm, sửa, ngừng hiệu lực chỉ AC-01, có `lyDo`, ghi nhật ký `NVCM_*` (FR-036).

### `da_canh_bao_giay_phep` – kỹ thuật

| Cột | Ghi chú |
| --- | --- |
| `loai` | `CO_SO` hoặc `NGUOI_HANH_NGHE` |
| `giay_phep_id`, `ngay_het_han` | Khóa chính `(loai, giay_phep_id, ngay_het_han)` |
| `canh_bao_luc` | |

---

## 5. Ngoại tuyến và lỗi dữ liệu nguồn (mục F)

### `ban_ghi_cho_xem_lai` – nhóm 2

| Cột | Ghi chú |
| --- | --- |
| `id` | |
| `loai_ban_ghi` | Mã do module đăng ký `NgoaiTuyenHandler`, ví dụ `M04.KET_QUA_CONG_VIEC` |
| `noi_dung` | `JSON`, nội dung gốc từ thiết bị |
| `idempotency_key` | `UNIQUE` cùng `nguoi_ghi_id` |
| `nguoi_ghi_id`, `ma_thiet_bi`, `thoi_diem_thiet_bi`, `thoi_diem_dong_bo`, `lan_ket_noi_cuoi` | |
| `ly_do_cho` | `MUON_HON_DONG_BO`, `SOM_HON_KET_NOI_CUOI`, `QUA_CFG_M15_08` |
| `tang_khu_vuc_id NULL` | Người quyết định theo FR-041c, CHK056 |
| `trang_thai` | `CHO_XEM_LAI` → `DA_CHAP_NHAN` / `DA_TU_CHOI` |
| `nguoi_quyet_dinh_id`, `thoi_diem_quyet_dinh`, `ly_do_tu_choi`, `ket_qua_loai`, `ket_qua_id` | `ly_do_tu_choi` bắt buộc khi từ chối |
| `phien_ban` | Khóa lạc quan; quyết định đầu tiên thắng (000 FR-040) |

| Từ | Lệnh | Đến | Kiểm tra | Nhật ký |
| --- | --- | --- | --- | --- |
| — | đồng bộ, thời điểm thiết bị bất hợp lý | `CHO_XEM_LAI` | FR-041c (a)–(c) | `NT_CHO_XEM_LAI`; báo người quyết định |
| `CHO_XEM_LAI` | `POST /ban-ghi-cho-xem-lai/{id}/chap-nhan` | `DA_CHAP_NHAN` | Trưởng tầng được giao của tầng; không có thì Người phụ trách ca đang diễn ra; không gắn tầng thì AC-01. `NgoaiTuyenHandler.apDung` ghi bản ghi thật, hiệu lực từ lúc chấp nhận | `NT_CHAP_NHAN` |
| `CHO_XEM_LAI` | `POST …/tu-choi` | `DA_TU_CHOI` | Như trên; `lyDo` bắt buộc | `NT_TU_CHOI` |

### `loi_du_lieu_quyen` – kỹ thuật

| Cột | Ghi chú |
| --- | --- |
| `nguon`, `ban_ghi_id`, `loai_loi` | Khóa duy nhất của một lỗi đang mở (CHK059) |
| `lan_dau_luc`, `lan_cuoi_luc`, `so_lan` | |
| `da_khac_phuc_luc NULL` | Đặt khi lần kiểm tra kế tiếp đọc được dữ liệu hợp lệ |

Lần đầu của một lỗi phát sự kiện `LoiDuLieuQuyen` tới 009 để báo Quản lý viện (FR-041b).

---

## 6. Nhật ký

Mọi lệnh ghi `nhat_ky` của 000 trong cùng transaction (000 FR-047). Các `hanh_dong` của 002: `TK_TAO`, `TK_KHOA_TAM`, `TK_HET_KHOA_TAM`, `TK_MO_KHOA_SOM`, `TK_KHONG_HOAT_DONG`, `TK_KICH_HOAT_LAI`, `TK_KHOA`, `TK_MO_KHOA`, `TK_CAP_LAI_MAT_KHAU`, `TK_DOI_MAT_KHAU`, `DANG_NHAP`, `DANG_NHAP_THAT_BAI`, `VT_GAN`, `VT_GO`, `QUYEN_THU_HEP`, `QUYEN_KHOI_PHUC`, `QUYEN_GAN_THEM`, `QUYEN_GO_THEM`, `NVCM_TAO`, `NVCM_SUA`, `NVCM_NGUNG`, `NT_CHO_XEM_LAI`, `NT_CHAP_NHAN`, `NT_TU_CHOI`, `QUYEN_TU_CHOI_DU_LIEU_NGUON`; *(bổ sung 2026-10-18)* `QR_TAO`, `QR_DUNG`, `TB_GAN`, `TB_GO`.

- `du_lieu_truoc`, `du_lieu_sau` của thay đổi quyền chứa tập quyền trước và sau của vai trò hoặc tài khoản (FR-026, SC-007).
- `chi_tiet` chứa `cachXacMinh` (FR-052), `canCu` nhiệm vụ (FR-019a → f), `ngoaiPhamVi` (FR-044a).
- Mật khẩu, mật khẩu tạm, token không bao giờ vào nhật ký.

---

## 7. Truy vết BR, DBR và FR

| Mã | Bảo đảm bằng | Nơi kiểm thử |
| --- | --- | --- |
| BR-M15-01 | `QuyenApi.coQuyen` ba lớp, interceptor `@CanQuyen` (research A-10); ArchUnit bắt mọi `@LenhNghiepVu` có `@CanQuyen`, trừ danh sách ngoại lệ có tên (lệnh SOS của 007, A-15) | `QuyenBaLopIT`, `MaTranQuyenIT`, `CanQuyenArchTest` |
| BR-M15-01 (ngoại lệ sự cố) | Bỏ lớp phạm vi với khóa `(SU_CO_KHAN_CAP, TAO)`; nhật ký `ngoaiPhamVi` | `SuCoNgoaiPhamViIT` |
| BR-M15-02 | Phạm vi tính trong từng yêu cầu; khoảng CFG-M15-07 | `PhamViTheoCaIT`, `PhamViDoiPhanCongIT` |
| BR-M15-04 | Khóa `(TAI_KHOAN_PHAN_QUYEN, *)` chỉ AC-01; nhật ký trước/sau | `PhanQuyenChiQuanLyIT` |
| BR-M15-05 | `so_lan_sai_lien_tiep`, `khoa_tam_den`, job `ChuyenKhongHoatDong`; mở khóa sớm theo FR-019c | `DangNhapKhoaTamIT`, `KhongHoatDongIT`, `MoKhoaSomIT` |
| BR-M15-14 | `TaiKhoanApi.xetLaiNguoiThan` trong transaction của 012 | `HetQuanHeKhoaTaiKhoanIT` |
| BR-M15-15 | `UNIQUE` trên `thiet_bi_dang_mo` | `MotThietBiMotPhienIT` |
| BR-M15-16 | Bảng có hiệu lực theo thời điểm; `lich_su_trang_thai_tai_khoan` | `QuyenTaiThoiDiemIT` |
| BR-M15-17 | Không có trường ngày hiệu lực trong lệnh; `If-Match` trên `phien_ban_quyen` | `DoiQuyenDongThoiIT` |
| BR-M15-18 | Thứ tự tính ở A-05; lệnh thu hẹp chặn vượt ma trận | `ThuTuThuHepTest`, `ThuHepKhongVuotIT` |
| BR-M15-19 | `nghiep_vu_chuyen_mon_pham_vi` dùng danh mục của 008 | `PhamViHanhNgheIT` |
| BR-M15-20 | Kết quả từng bản ghi trong phản hồi đồng bộ; hiển thị ở app | `DongBoKetQuaIT`, `ket_qua_dong_bo_test.dart` |
| BR-M15-21 | Kiểm CFG-M15-09 khi đổi mật khẩu; so `hoat_dong_cuoi_luc` với CFG-M15-10 ở mỗi lần tra phiên | `DoManhMatKhauIT`, `TuDangXuatIT` |
| BR-M15-22 | Lệnh `khoi-tao-quan-ly-vien` chỉ chạy khi chưa có AC-01 | `KhoiTaoQuanLyVienIT` |
| BR-M06-05 | `nghiep_vu_chuyen_mon` cùng `GiayPhepApi`, `GiayPhepCoSoApi` theo ngày | `DieuKienPhapLyIT` |
| BR-M09-04 | `TaiKhoanApi.khoaDoNghiViec` trong transaction của 008 | `NghiViecKhoaTaiKhoanIT` |
| BR-M01-08, DBR-03 | Điều kiện chú thích ¹ do 001 cài, kiểm mỗi lần xem | `NguoiThanSucKhoeIT` |
| FR-012 | Đếm AC-01 Hoạt động dưới khóa dòng | `LuonConQuanLyVienIT` |
| FR-022 | `quy_tac_xem_truong`, `BeanSerializerModifier` | `GioiHanTruongIT` |
| FR-023 *(bổ sung 2026-10-21, T082)* | Ma trận (Phụ lục 27) không có ô của AC-02 cho duyệt thay đổi lưu trú, chi phí và kê đơn; thu hẹp không thêm được quyền (FR-025) | `FloorManagerLimitsIT` |
| FR-035a *(bổ sung 2026-10-21, T021)* | `PhamViApi.boLocLichSu` dựng từ lịch sử phân công; ArchUnit cấm gọi từ `@LenhNghiepVu` | `PhamViBaoCaoLichSuIT` (`HistoryReportScopeIT`), `CanQuyenArchTest` |
| FR-041b | `loi_du_lieu_quyen` | `DuLieuNguonLoiIT` |
| FR-041c | `ban_ghi_cho_xem_lai`, `thiet_bi` | `NgoaiTuyenChoXemLaiIT` |
| BR-M15-23 *(bổ sung 2026-10-18)* | Nghe `NguoiCaoTuoiDaChuyenTrangThai`; lệnh `khoa-nguoi-cao-tuoi`; loại yêu cầu `M15.KHOA_TAI_KHOAN_NCT`; `mo-khoa-nguoi-cao-tuoi` chặn `NCT_KET_THUC_LUU_TRU` | `KhoaTaiKhoanNguoiCaoTuoiIT` |
| BR-M15-24 *(bổ sung 2026-10-18)* | `thiet_bi_gan` với hai cột sinh `UNIQUE`; kênh `APP_NGUOI_CAO_TUOI` bỏ CFG-M15-10; job Không hoạt động lọc người thân | `ThietBiGanIT`, `PhienNguoiCaoTuoiKhongHetIT` |
| FR-067, FR-068 | Lệnh tạo kiểm trạng thái và `xacNhan`; phạm vi `BAN_THAN` | `TaoTaiKhoanNguoiCaoTuoiIT` |
| FR-069 | `ma_qr_dang_nhap`, CFG-M15-11 | `MaQrDangNhapIT` |
| FR-071 | `ThietBiGanApi.xacDinh` theo `ma_bam_token_thiet_bi` | `XacDinhThietBiSosIT` |
| FR-075 | Không có quyền AC-10 ở dòng "Tài khoản người cao tuổi" | `NguoiThanKhongXemTaiKhoanNctIT` |
