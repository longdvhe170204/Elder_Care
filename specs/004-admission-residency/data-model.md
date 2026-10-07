# Data Model: Tiếp nhận và lưu trú (004)

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1; không nhắc lại ở từng bảng. Tiền lưu `DECIMAL(15,0)` (VND). Người cao tuổi, giường, loại phòng, thuốc, người thân, tài khoản, yêu cầu phê duyệt là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Mức chăm sóc dùng mã `CO_BAN`, `THUONG_XUYEN`, `DAC_BIET` của 001.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `luu_tru_nct` | 2 (kỹ thuật) | Khóa theo người, dấu "đã đóng" (FR-072) |
| `luot_dang_ky` | 2 | A |
| `ho_so_cho`, `ho_so_cho_tinh_huong` | 2 | B |
| `lich_su_diem`, `lien_he_giuong` | 3 | B |
| `hop_dong`, `hop_dong_dich_vu`, `hop_dong_chinh_sach_vang`, `hop_dong_lich_den` | 2 (sửa được chỉ ở Nháp) | C |
| `phien_ban_noi_dung_hop_dong` | 3 | C, F |
| `dich_vu` | 1 | E |
| `phien_ban_don_gia` | 1, theo phiên bản | E |
| `phu_luc_hop_dong`, `yeu_cau_thay_doi_thuoc_tinh` | 2; chỉ mục kỹ thuật | F |
| `luot_vang` | 2; bất biến khi đóng | G |
| `lich_su_du_kien_tro_lai`, `quyet_dinh_giu_giuong`, `bao_vang_ban_tru` | 3 | G |
| `ngay_vang` | Kết quả tính (kỹ thuật) | G, J |
| `ngay_nghi_ban_tru` | 2 | G |
| `ho_so_ket_thuc` | 2 | H |
| `ban_giao_nct`, `ngoai_le_dieu_kien` | 3 | H |
| `ban_ghi_qua_doi` | 3 | I |
| `muc_viec_sau_qua_doi` | 2 (kết quả tính) | I |
| `lan_chay_theo_ngay`, `nhac_m02` | Kỹ thuật | K |

---

## 2. `luu_tru_nct` – nhóm 2 (kỹ thuật)

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `nct_id` | `BIGINT` | Khóa chính; tạo khi lập lượt đăng ký hoặc hợp đồng đầu tiên | |
| `da_dong_luc` | `DATETIME(6) NULL` | Dấu "hồ sơ lưu trú đã đóng"; đặt một lần, không gỡ | FR-072 |
| `phien_ban` | `INT` | | |

Đích khóa `FOR UPDATE` của mọi lệnh 004 trên cùng người (research T-03).

---

## 3. Đăng ký tiếp nhận (mục A)

### `luot_dang_ky` – nhóm 2

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `nct_id` | `BIGINT` | | FR-001 |
| `ngay_dang_ky` | `DATE` | Không đổi khi đã có hồ sơ chờ (backend) | FR-001, FR-009 |
| `nguoi_lien_he`, `nhu_cau`, `ghi_chu` | `VARCHAR` | | FR-001 |
| `loai_luu_tru_mong_muon` | `ENUM('NOI_TRU_DAI_HAN','NOI_TRU_NGAN_NGAY','BAN_TRU')` | | FR-001 |
| `muc_du_kien` | `ENUM` mức | | FR-001, 003 FR-018 (a) |
| `ngay_mong_muon_vao` | `DATE` | | FR-001 |
| `trang_thai` | `ENUM('DANG_XU_LY','DANG_CHO','DA_TIEP_NHAN','DA_HUY')` | | FR-002 |
| `nct_dang_mo` | Cột sinh `IF(trang_thai IN ('DANG_XU_LY','DANG_CHO'), nct_id, NULL)` | `UNIQUE` | FR-001 |

| Từ | Lệnh / sự kiện | Đến | Gọi từ | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Lập lượt đăng ký | `DANG_XU_LY` | `POST /luot-dang-ky` (AC-09) | Người cao tuổi Đang tiếp nhận; chưa có lượt mở | — |
| `DANG_XU_LY`, `DANG_CHO` | Cập nhật lượt đăng ký | không đổi | `POST …/{id}/cap-nhat` (`If-Match`, lý do) | FR-001 | Nhật ký trước/sau |
| `DANG_XU_LY` | Đưa vào danh sách chờ | `DANG_CHO` | `POST /ho-so-cho` | FR-007 | Tạo hồ sơ chờ |
| `DANG_CHO` | Hồ sơ chờ Đã tiếp nhận | `DANG_XU_LY` | `daPhanBo` (003) | — | — |
| `DANG_XU_LY` | Hoàn tất tiếp nhận (001) | `DA_TIEP_NHAN` | Bean 004 | FR-004 | Sự kiện `TiepNhanHoanTat` (018) |
| `DANG_XU_LY`, `DANG_CHO` | Hủy tiếp nhận (001) | `DA_HUY` | Bean 004 | — | FR-002 |

---

## 4. Danh sách chờ (mục B)

### `ho_so_cho` – nhóm 2

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `luot_dang_ky_id` | `BIGINT` | Khóa ngoại | FR-007 |
| `nct_id` | `BIGINT` | | |
| `muc_cham_soc`, `muc_chua_danh_gia` | `ENUM`, `BOOLEAN` | Theo FR-008; cập nhật khi nhận `MucChamSocDaDoi` | FR-008 |
| `diem_hien_hanh` | `INT` | Do `TinhDiemUuTien` ghi | FR-009 |
| `diem_chi_tiet` | `JSON` | Thành phần: mức, thời gian chờ, tình huống, điều chỉnh | FR-009 |
| `giuong_dang_giu_id` | `BIGINT NULL` | Giường đang giữ (hiển thị); hạn giữ là dữ liệu của 003 | FR-014 |
| `moc_cap_nhat` | `DATETIME(6)` | Mốc tính CFG-M02-03 | FR-017 |
| `trang_thai` | `ENUM('DANG_CHO','DA_LIEN_HE','DA_TIEP_NHAN','TU_CHOI','HUY_CHO')` | | Bảng mục B |
| `nct_dang_mo` | Cột sinh `IF(trang_thai IN ('DANG_CHO','DA_LIEN_HE'), nct_id, NULL)` | `UNIQUE` | FR-007 |
| `giuong_giu_duy_nhat` | Cột sinh `IF(trang_thai='DA_LIEN_HE', id, NULL)` cùng `giuong_dang_giu_id NOT NULL` khi `DA_LIEN_HE` (`CHECK`) | | FR-014 |

`ho_so_cho_tinh_huong (ho_so_cho_id, ma_tinh_huong, trang_thai ENUM('XAC_NHAN','CHO_XAC_NHAN'))`: `ma_tinh_huong` là mã trong bảng CFG-M02-08. Dòng "từng lưu trú tại viện" được thêm sẵn ở `CHO_XAC_NHAN` (FR-007).

**Bảng chuyển**: khớp bảng trạng thái hồ sơ chờ của spec. Lệnh Từ chối, Hủy chờ đi qua `HUY_TIEP_NHAN` của 001 (research T-03).

| Từ | Lệnh / sự kiện | Đến | Gọi từ | Tác động |
| --- | --- | --- | --- | --- |
| — | Đưa vào danh sách chờ | `DANG_CHO` | `POST /ho-so-cho` | Lượt đăng ký `DANG_CHO`; tính điểm lần đầu |
| `DANG_CHO` | Ghi nhận đã liên hệ | `DA_LIEN_HE` | `POST /ho-so-cho/{id}/ghi-nhan-da-lien-he` | `giuChoHoSoCho` (003); dòng `lien_he_giuong` `LIEN_HE` |
| `DA_LIEN_HE` | Hết hạn giữ chỗ | `DANG_CHO` | `hoSoChoHetHanGiuCho` (job của 003) | Dòng `HET_HAN`; `GiuongTrongCanChonHoSo` |
| `DA_LIEN_HE` | Từ chối giường này | `DANG_CHO` | `POST …/tu-choi-giuong` | `huyGiuCho` (003); dòng `TU_CHOI_GIUONG`; `GiuongTrongCanChonHoSo` |
| `DANG_CHO`, `DA_LIEN_HE` | Phân bổ giường (003) | `DA_TIEP_NHAN` | `daPhanBo` | Lượt đăng ký `DANG_XU_LY` |
| `DANG_CHO`, `DA_LIEN_HE` | Ghi nhận từ chối | `TU_CHOI` | `POST …/tu-choi` → `HUY_TIEP_NHAN` (001) | Như Hủy tiếp nhận (FR-002) |
| `DANG_CHO`, `DA_LIEN_HE` | Hủy chờ | `HUY_CHO` | `POST …/huy-cho` → `HUY_TIEP_NHAN`; hoặc bean khi Hủy tiếp nhận từ 001 | Như trên; không gọi lại 001 |
| `DANG_CHO` | Xác nhận còn nhu cầu | `DANG_CHO` | `POST …/xac-nhan-con-nhu-cau` | `moc_cap_nhat` = bây giờ |

### `lich_su_diem` – nhóm 3

`ho_so_cho_id`, `diem_cu`, `diem_moi`, `thanh_phan JSON`, `nguyen_nhan ENUM('TINH_HANG_NGAY','DOI_MUC','DOI_TINH_HUONG','DIEU_CHINH','DOI_BANG_DIEM','TAO')`, `yeu_cau_phe_duyet_id NULL` (điều chỉnh), `thoi_diem` (FR-010, FR-011). Dòng `TINH_HANG_NGAY` chỉ thêm khi điểm đổi.

### `lien_he_giuong` – nhóm 3

`ho_so_cho_id`, `giuong_id`, `ket_qua ENUM('LIEN_HE','TU_CHOI_GIUONG','HET_HAN')`, `thoi_diem`, `nguoi_thuc_hien_id`, `ly_do`, `ly_do_khong_theo_thu_tu NULL` (FR-014). Dùng để loại hồ sơ khỏi "lượt trống hiện tại" (FR-012) và truy vết công bằng (BR-M02-10).

---

## 5. Hợp đồng (mục C, D)

### `hop_dong` – nhóm 2

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `so_hop_dong` | `VARCHAR(20)` | `UNIQUE`; `HD-<năm>-<id>` | FR-019 |
| `nct_id` | `BIGINT` | | |
| `loai_luu_tru` | `ENUM` như lượt đăng ký | | FR-019 |
| `ngay_bat_dau` | `DATE` | | FR-019 |
| `ngay_ket_thuc` | `DATE NULL` | Bắt buộc với `NOI_TRU_NGAN_NGAY` (`CHECK`) | FR-019 |
| `ngay_ket_thuc_hien_hanh` | `DATE NULL` | Bản sao ngày kết thúc của phiên bản nội dung mới nhất; chỉ handler áp dụng phụ lục ghi | FR-022 |
| `muc_cham_soc` | `ENUM` mức | | FR-019, FR-025 |
| `loai_phong_id`, `giuong_id` | `BIGINT NULL` | Nội trú: loại phòng hoặc giường | FR-019 |
| `nguoi_dai_dien_ky_id` | `BIGINT NULL` | Bắt buộc khi Ghi nhận đã ký | FR-021, DBR-02 |
| `ngay_ky`, `tep_ban_scan_id` | | Bắt buộc khi Ghi nhận đã ký | FR-021 |
| `ky_tinh_phi_luu_tru` | `ENUM('THANG','NGAY','BUOI')` | | FR-019 |
| `khoan_dat_coc` | `DECIMAL(15,0) NULL` | `NULL` cùng `khong_yeu_cau_coc = true` là "không yêu cầu" | FR-030 |
| `chinh_sach_giu_giuong`, `dieu_kien_cham_dut` | `VARCHAR(2000)` | | FR-019 |
| `bam_noi_dung` | `CHAR(64) NULL` | SHA-256 nội dung nháp chuẩn hóa | FR-025a |
| `dang_cho_duyet_dieu_khoan` | `BOOLEAN` | Khóa nội dung (CHK028) | FR-025a |
| `yeu_cau_duyet_dieu_khoan_id` | `BIGINT NULL` | Yêu cầu Đã áp dụng cho bản băm hiện tại | FR-025a |
| `trang_thai` | `ENUM('NHAP','CHO_KY','HIEU_LUC','KET_THUC','CHAM_DUT','DA_HUY')` | | FR-020 |
| `ly_do_cham_dut` | `VARCHAR(2000) NULL` | `CHECK (trang_thai <> 'CHAM_DUT' OR ly_do_cham_dut IS NOT NULL)` | FR-027 |
| `dau_qua_han_tu`, `da_bao_qlv_qua_han_luc` | `DATE NULL`, `DATETIME(6) NULL` | | FR-029 |

**Trigger**:
- `trg_hop_dong_khoa_noi_dung` (`BEFORE UPDATE`): `SIGNAL` khi đổi cột nội dung (mọi cột trừ `trang_thai`, `ly_do_cham_dut`, `ngay_ky`, `nguoi_dai_dien_ky_id`, `tep_ban_scan_id`, `ngay_ket_thuc_hien_hanh`, `dau_qua_han_tu`, `da_bao_qlv_qua_han_luc`, `dang_cho_duyet_dieu_khoan`, `yeu_cau_duyet_dieu_khoan_id`, `bam_noi_dung`, `phien_ban`) mà `OLD.trang_thai <> 'NHAP' OR OLD.dang_cho_duyet_dieu_khoan`. Bảng con có trigger tương tự theo trạng thái của hợp đồng cha (FR-020, FR-023, DBR-06).
- `trg_hop_dong_khong_chong` (`BEFORE UPDATE` khi `NEW.trang_thai='HIEU_LUC'` hoặc đổi `ngay_ket_thuc_hien_hanh`): `SIGNAL` nếu có hợp đồng `HIEU_LUC` khác cùng `nct_id` giao khoảng `[ngay_bat_dau, COALESCE(ngay_ket_thuc_hien_hanh,'9999-12-31')]` gồm cả hai đầu. Lưới an toàn; cơ chế chính là khóa `luu_tru_nct` (DBR-06, FR-022).

**Bảng con** (sửa được chỉ khi hợp đồng `NHAP` và không chờ duyệt điều khoản):
- `hop_dong_dich_vu`: `dich_vu_id`, `thuoc_goi BOOLEAN`, `so_luong`, `don_gia_rieng NULL` (giá khác chuẩn), `don_gia_ghi NULL` (đặt khi Ghi nhận đã ký, FR-036). Kiểm khi Gửi ký: đúng một dòng dịch vụ `CHAM_SOC` và mức của dòng trùng `muc_cham_soc` (FR-025).
- `hop_dong_chinh_sach_vang`: các dòng ghi đè cùng cấu trúc CFG-M02-05 (FR-050, FR-051). Không có dòng chồng khoảng ngày trong cùng loại lưu trú, loại vắng (backend).
- `hop_dong_lich_den`: `thu_trong_tuan` hoặc `ngay`, `gio_den`, `gio_ve TIME`; `CHECK (gio_ve > gio_den)` (FR-019, Q-286).

**Bảng chuyển hợp đồng** (khớp bảng của spec):

| Từ | Lệnh | Đến | Gọi từ | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Lập hợp đồng | `NHAP` | `POST /hop-dong` | Người cao tuổi Đang tiếp nhận hoặc đang lưu trú; có người đại diện (`NguoiDaiDienPort`) | Cấp số |
| `NHAP` | Sửa nội dung | `NHAP` | `PUT /hop-dong/{id}` (`If-Match`) | Không chờ duyệt điều khoản | Nhật ký; bỏ hiệu lực bản duyệt nếu băm đổi; cảnh báo FR-025 |
| `NHAP` | Gửi duyệt điều khoản | `NHAP` (chờ duyệt) | `POST …/gui-duyet-dieu-khoan` | Có điều khoản khác chuẩn; lý do | Lập yêu cầu `M02.DUYET_DIEU_KHOAN_HOP_DONG`; khóa nội dung |
| `NHAP` (chờ duyệt) | Yêu cầu được duyệt / từ chối, hủy | `NHAP` | `apDung` của handler; listener `YeuCauDaQuyetDinh` | 000 | Duyệt: ghi `yeu_cau_duyet_dieu_khoan_id`; mở khóa |
| `NHAP` | Gửi ký | `CHO_KY` | `POST …/gui-ky` | FR-019 đủ; FR-025 dịch vụ chăm sóc; FR-025a | Khóa nội dung; `HopDongDaDoiTrangThai` (017) |
| `CHO_KY` | Trả về nháp | `NHAP` | `POST …/tra-ve-nhap` | Lý do | Mở khóa |
| `CHO_KY` | Ghi nhận đã ký | `HIEU_LUC` | `POST …/ghi-nhan-da-ky` | FR-021; FR-022; bán trú: sức chứa (research T-10) | Phiên bản nội dung 0; ghi đơn giá; `da_tham_chieu` của phiên bản giá; sự kiện 017, 018 |
| `NHAP`, `CHO_KY` | Hủy hợp đồng | `DA_HUY` | `POST …/huy`; bean `HUY_TIEP_NHAN` | Lý do | 017 |
| `HIEU_LUC` | Áp dụng phụ lục | `HIEU_LUC` | Handler `M02.THAY_DOI_LUU_TRU` | FR-045 | Phiên bản nội dung mới |
| `HIEU_LUC` | Kết thúc lưu trú, ngày ≥ ngày kết thúc hiện hành | `KET_THUC` | Bean `KET_THUC_LUU_TRU` | Mục H | Gỡ dấu quá hạn |
| `HIEU_LUC` | Kết thúc sớm; Ghi nhận qua đời; Hủy tiếp nhận | `CHAM_DUT` | Bean | Mục H, I | Lý do chấm dứt |
| `HIEU_LUC` | Qua ngày kết thúc, chưa gia hạn, chưa kết thúc | `HIEU_LUC` (dấu) | Job `HOP_DONG` | — | FR-029 |

### `phien_ban_noi_dung_hop_dong` – nhóm 3

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `hop_dong_id` | `BIGINT` | Khóa ngoại | |
| `so_thu_tu` | `SMALLINT` | 0 lúc ký; tăng theo phụ lục; `UNIQUE (hop_dong_id, so_thu_tu)` | |
| `hieu_luc_tu` | `DATE` | Phiên bản 0: ngày bắt đầu; phụ lục: ngày hiệu lực thực tế; `UNIQUE (hop_dong_id, hieu_luc_tu)` | FR-024 |
| `phu_luc_id` | `BIGINT NULL` | `NULL` ở phiên bản 0 | DBR-07 |
| `noi_dung` | `JSON` | Toàn bộ nội dung, gồm dịch vụ kèm đơn giá đã ghi, dòng ghi đè, lịch đến | FR-024, FR-036 |
| `loai_luu_tru`, `muc_cham_soc`, `ngay_ket_thuc`, `loai_phong_id`, `khoan_dat_coc` | | Cột đọc nhanh, trích từ `noi_dung` | |

Nội dung tại ngày D: phiên bản `hieu_luc_tu` lớn nhất `<= D` của hợp đồng `HIEU_LUC`, `KET_THUC` hoặc `CHAM_DUT` có khoảng chứa D (research T-04). Chỉ mục `(hop_dong_id, hieu_luc_tu)`.

---

## 6. Dịch vụ và đơn giá (mục E)

### `dich_vu` – nhóm 1

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `loai` | `ENUM('CHAM_SOC','PHI_LUU_TRU','BUOI_BAN_TRU','PHUC_HOI','HOAT_DONG','DUA_DI_KHAM','TIEM','NGUOI_THAN_O_LAI','SUAT_AN','THUOC_NGUON_VIEN','KHAC')` | | FR-034 |
| `ten`, `don_vi_tinh` | | `ten_hieu_luc` `UNIQUE` | FR-034 |
| `ky_tinh` | `ENUM('THANG','NGAY','BUOI','LAN','DON_VI')` | | FR-034 |
| `ma_muc` | `ENUM` mức `NULL` | `CHECK ((loai='CHAM_SOC') = (ma_muc IS NOT NULL))` | Q-297 |
| `muc_hieu_luc` | Cột sinh `IF(loai='CHAM_SOC' AND trang_thai='HIEU_LUC', ma_muc, NULL)` | `UNIQUE`: mỗi mức một dịch vụ chăm sóc Hiệu lực | Q-297 |
| `loai_phong_id` | `BIGINT NULL` | Với `PHI_LUU_TRU` | FR-034 |
| `thuoc_id` | `BIGINT NULL` | Với `THUOC_NGUON_VIEN` (danh mục thuốc của 006) | FR-034 |

### `phien_ban_don_gia` – nhóm 1, theo phiên bản (000 mục 1.2)

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `dich_vu_id` | `BIGINT` | Khóa ngoại | |
| `don_gia` | `DECIMAL(15,0)` | `>= 0` | FR-035 |
| `hieu_luc_tu`, `hieu_luc_den` | `DATE`, `DATE NULL` | `CHECK (hieu_luc_den IS NULL OR hieu_luc_den >= hieu_luc_tu)` | FR-035 |
| `da_tham_chieu` | `BOOLEAN` | Hợp đồng ký hoặc 010 lập khoản thì đặt | FR-035 |

- Trigger `trg_phien_ban_don_gia_khong_chong` (`BEFORE INSERT`, `BEFORE UPDATE`) là lưới an toàn cho DBR-08; cơ chế chính là khóa dòng `dich_vu`.
- Trigger `trg_phien_ban_don_gia_khong_sua`: `SIGNAL` khi đổi `don_gia` hoặc `hieu_luc_tu` mà `OLD.da_tham_chieu`. Chỉ cho đặt `hieu_luc_den` khi nó đang `NULL` (đóng khoảng).

---

## 7. Yêu cầu thay đổi và phụ lục (mục F)

### `phu_luc_hop_dong` – nhóm 2

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `hop_dong_id` | `BIGINT` | Khóa ngoại | DBR-07 |
| `yeu_cau_phe_duyet_id` | `BIGINT` | `NOT NULL`, `UNIQUE`; yêu cầu `M02.THAY_DOI_LUU_TRU` đã duyệt | DBR-07 |
| `loai_thay_doi` | `SET('LOAI_LUU_TRU','MUC_CHAM_SOC','DICH_VU','THOI_GIAN','GIUONG','LICH_DEN')` | | FR-037 |
| `noi_dung_truoc`, `noi_dung_sau` | `JSON` | | FR-038 |
| `ngay_hieu_luc` | `DATE` | `>=` ngày bắt đầu hợp đồng (backend, khi lập yêu cầu và khi duyệt) | FR-040, DBR-07 |
| `trang_thai` | `ENUM('CHO_HIEU_LUC','DA_AP_DUNG','KHONG_AP_DUNG')` | Không có endpoint sửa, xóa; trigger chặn sửa nội dung | FR-042, FR-047 |
| `phien_ban_noi_dung_id` | `BIGINT NULL` | Có khi `DA_AP_DUNG` (`CHECK`) | FR-024 |

| Từ | Sự kiện | Đến | Gọi từ |
| --- | --- | --- | --- |
| — | Yêu cầu được duyệt | `CHO_HIEU_LUC` | `khiDuyet` của handler |
| `CHO_HIEU_LUC` | Yêu cầu được áp dụng | `DA_AP_DUNG` | `apDung` (cùng transaction với mọi tác động FR-042) |
| `CHO_HIEU_LUC` | Yêu cầu Áp dụng không thành, Hủy, đóng do trạng thái cuối | `KHONG_AP_DUNG` | Listener `YeuCauApDungKhongThanh`, `YeuCauDaHuy`; bean trạng thái cuối |

### `yeu_cau_thay_doi_thuoc_tinh` (chỉ mục kỹ thuật, FR-046)

`yeu_cau_phe_duyet_id`, `nct_id`, `thuoc_tinh` (một dòng mỗi loại thay đổi), `cho_hieu_luc BOOLEAN`, cột sinh `khoa = IF(cho_hieu_luc, CONCAT(nct_id,':',thuoc_tinh), NULL)` có `UNIQUE`. Đặt `cho_hieu_luc = true` trong `khiDuyet`, `false` khi phụ lục rời `CHO_HIEU_LUC`. Duyệt yêu cầu thứ hai cùng thuộc tính vi phạm `UNIQUE`; backend kiểm trước để trả 409 có mô tả.

Nội dung của yêu cầu (giá trị trước/sau, ảnh hưởng chi phí, tham chiếu đánh giá hoặc lệnh chuyển giường) nằm trong JSON của `yeu_cau_phe_duyet` (000), theo `kieuNoiDung()` của handler.

---

## 8. Vắng mặt (mục G)

### `luot_vang` – nhóm 2; bất biến khi `DA_DONG`

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `nct_id` | `BIGINT` | | FR-048 |
| `loai_vang` | `ENUM('VE_NHA','DI_CHOI_GIA_DINH','DI_KHAM_TRONG_NGAY','LY_DO_KHAC','BENH_VIEN')` | | FR-048 |
| `ly_do`, `ly_do_chuan` | `VARCHAR`, `ENUM('VE_NHA_NGUYEN_VONG_CUOI_DOI') NULL` | | FR-048, Q-218 |
| `roi_luc` | `DATETIME(6)` | | FR-048 |
| `du_kien_tro_lai` | `DATETIME(6) NULL` | Bắt buộc trừ `BENH_VIEN` (`CHECK`); giá trị hiện hành, lịch sử ở bảng riêng | FR-048, FR-049 |
| `ve_luc` | `DATETIME(6) NULL` | | FR-059 |
| `nguoi_don_id`, `ban_ghi_don_id` | `BIGINT NULL` | Tạm vắng: bản ghi đón hợp lệ (012) | FR-048 |
| `don_vi_dua_di` | `VARCHAR NULL` | `BENH_VIEN` | FR-048 |
| `nhan_vien_ban_giao_id` | `BIGINT` | | FR-048 |
| `mang_thuoc` | `BOOLEAN` | Mặc định `false`; bắt buộc với Tạm vắng | FR-048 |
| `giu_giuong_ban_dau` | `BOOLEAN` | Kết quả tra ngày 1 | FR-054 |
| `da_bao_nguong_luc`, `ngay_xem_xet_lai` | | Ngưỡng giữ giường, quyết định giữ tiếp | FR-055, FR-056 |
| `luot_truoc_id` | `BIGINT NULL` | Lượt Tạm vắng trước khi chuyển viện | FR-057 |
| `trang_thai` | `ENUM('DANG_MO','DA_DONG')` | | |
| `nct_dang_mo` | Cột sinh `IF(trang_thai='DANG_MO', nct_id, NULL)` | `UNIQUE` | |

Trigger `trg_luot_vang_khong_sua`: `SIGNAL` khi `OLD.trang_thai='DA_DONG'` (FR-061). Sai sót đi đính chính `M02.LUOT_VANG` (`ve_luc`, `roi_luc`).

### `lich_su_du_kien_tro_lai` – nhóm 3

`luot_vang_id`, `gia_tri_cu`, `gia_tri_moi`, `ly_do`, `nguoi_thuc_hien_id`, `thoi_diem` (FR-049).

### `ngay_vang` – kết quả tính

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `luot_vang_id`, `ngay` | Khóa chính hai cột | FR-058 |
| `so_thu_tu` | Ngày vắng thứ mấy của lượt | FR-052 |
| `he_so` | `DECIMAL(5,2)` phần trăm | FR-052 |
| `giu_giuong` | `BOOLEAN` | FR-052 |
| `nguon_dong` | `ENUM('HOP_DONG','CO_SO','MAC_DINH')` cùng `dong_chinh_sach JSON` | FR-051, FR-053 |
| `dau_cho_quyet_dinh`, `dau_khong_co_dong` | `BOOLEAN` | FR-053, FR-056 |
| `quyet_dinh_id` | Quyết định đã đặt hệ số, nếu có | FR-056 |
| `tinh_luc`, `thoi_diem_tham_so` | Bảng CFG-M02-05 dùng tại thời điểm nào | FR-052 |

Dòng được ghi bởi job `NGAY_VANG` và chỉ được ghi lại khi có quyết định giữ giường hoặc đính chính lượt vắng. Mỗi lần ghi lại có nhật ký `M02_VANG_TINH_LAI` và sự kiện `NgayLuuTruDaDoi`. Bảng không thuộc ba nhóm của 1.5 (Complexity Tracking của plan).

### `quyet_dinh_giu_giuong` – nhóm 3

`luot_vang_id`, `ngay_nguong`, `lua_chon ENUM('GIU_TIEP','GIAI_PHONG','CHI_DAT_HE_SO')`, `he_so`, `ngay_xem_xet_lai NULL` (bắt buộc với `GIU_TIEP`), `ap_tu_ngay`, `ap_den_ngay NULL` (với `CHI_DAT_HE_SO` của lượt đã đóng, FR-061), `nguoi_quyet_dinh_id`, `thoi_diem`, `ly_do`. `CHECK (lua_chon <> 'GIU_TIEP' OR ngay_xem_xet_lai IS NOT NULL)` (FR-055).

### `bao_vang_ban_tru` – nhóm 3

`nct_id`, `ngay`, `buoi_id NULL` (cả ngày nếu rỗng), `thoi_diem_gia_dinh_bao`, `du_han` (tính tại lúc ghi: lịch đến − thời điểm báo ≥ CFG-M02-06), `nguoi_ghi_id`, `thoi_diem` (FR-060).

### `ngay_nghi_ban_tru` – nhóm 2

`ngay`, `ly_do`, `trang_thai ENUM('HIEU_LUC','DA_HUY')`, cột sinh `ngay_hieu_luc` có `UNIQUE`, `khai_bao_muon BOOLEAN` (ngày đã qua: lý do bắt buộc) (FR-060a).

---

## 9. Kết thúc lưu trú (mục H)

### `ho_so_ket_thuc` – nhóm 2

| Cột | Kiểu | Ràng buộc | Nguồn |
| --- | --- | --- | --- |
| `nct_id` | `BIGINT` | | FR-062 |
| `truong_hop` | `ENUM('HET_HOP_DONG','XUAT_VIEN','CHUYEN_CO_SO','CHAM_DUT_HOP_DONG')` | | FR-062 |
| `ly_do_y_te` | `BOOLEAN` | Bắt buộc `true` khi người lập là Bác sĩ (backend) | FR-062 |
| `noi_chuyen_den` | `VARCHAR NULL` | `CHECK (truong_hop <> 'CHUYEN_CO_SO' OR noi_chuyen_den IS NOT NULL)` | FR-062 |
| `ngay_ket_thuc_du_kien` | `DATE` | | FR-066 |
| `thoi_diem_ket_thuc_thuc_te` | `DATETIME(6) NULL` | | FR-065 |
| `nguoi_yeu_cau` | `VARCHAR` | | FR-062 |
| `dau_qua_ngay_du_kien_tu` | `DATE NULL` | | FR-066a |
| `ky_cuoi_tham_chieu` | `BIGINT NULL` | Bảng kỳ cuối nháp do `KyCuoiPort` trả (010) | FR-066 |
| `trang_thai` | `ENUM('DANG_CHUAN_BI','HOAN_TAT','DA_HUY')` | | FR-067 |
| `nct_dang_mo` | Cột sinh `IF(trang_thai='DANG_CHUAN_BI', nct_id, NULL)` | `UNIQUE` | FR-062 |

Bảng chuyển khớp dòng "Hồ sơ kết thúc lưu trú" của spec. Đổi ngày qua `POST …/doi-ngay` (lý do) gọi `KyCuoiPort.tinhLai` và gỡ dấu "quá ngày dự kiến" khi ngày mới không sớm hơn hôm nay. Hủy qua `POST …/huy` (lý do) gọi `KyCuoiPort.huyMocDung`. Hoàn tất qua bean `KET_THUC_LUU_TRU`. Bean `GHI_NHAN_QUA_DOI` chuyển hồ sơ sang `DA_HUY`, lý do "Qua đời".

### `ban_giao_nct` – nhóm 3

`ho_so_ket_thuc_id`, `loai_nguoi_nhan ENUM('NGUOI_THAN','CO_SO_TIEP_NHAN')`, `nguoi_than_id NULL`, `ten_co_so NULL`, `thoi_diem_ban_giao`, `nhan_vien_ban_giao_id`, `nguoi_ghi_id`, `thoi_diem` (FR-063 (e)). Điều kiện (e) Đạt khi có ít nhất một dòng chưa bị Hủy ghi nhận (000 `v_ban_ghi_da_huy`).

### `ngoai_le_dieu_kien` – nhóm 3

`ho_so_ket_thuc_id`, `ma_dieu_kien ENUM('KT_A_CHI_PHI','KT_B_DO_GUI','KT_C_THUOC_GUI','KT_D_CANH_BAO_SU_CO','KT_H_QUYET_TOAN')`, `yeu_cau_phe_duyet_id` (`UNIQUE`), `thoi_diem`. Không có `KT_E_BAN_GIAO` (FR-064). Dòng do `apDung` của `M02.NGOAI_LE_KET_THUC` thêm.

---

## 10. Qua đời (mục I)

### `ban_ghi_qua_doi` – nhóm 3

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `nct_id` | `UNIQUE` | FR-069 |
| `thoi_diem_qua_doi`, `dia_diem` | | FR-069 |
| `nguoi_phat_hien`, `nguoi_xac_nhan_id` | | FR-069, 001 FR-047b |
| `nguyen_nhan` | `VARCHAR(2000) NULL`; `@NhomTruong(SUC_KHOE)` | FR-069, BR-M02-21 |
| `tep_bang_chung_id` | Bắt buộc khi người ghi là Hành chính (backend) | FR-069 |
| `nguoi_ghi_id`, `thoi_diem` | | |

Bổ sung nguyên nhân đi đính chính `M02.BAN_GHI_QUA_DOI`.

### `muc_viec_sau_qua_doi` – kết quả tính

`ban_ghi_qua_doi_id`, `ma_muc ENUM('DO_GUI','THUOC_GUI','CHI_PHI_KY_CUOI','CANH_BAO_SU_CO','QUYET_TOAN')`, `trang_thai ENUM('CHUA_HOAN_THANH','HOAN_THANH','KHONG_AP_DUNG')`, `can_cu`, `cap_nhat_luc`. Khóa chính `(ban_ghi_qua_doi_id, ma_muc)`. Ghi lại khi `MucViecSauQuaDoiPort` trả kết quả khác (research T-13). Mọi dòng xong thì `luu_tru_nct.da_dong_luc` được đặt.

---

## 11. Bảng kỹ thuật

- `lan_chay_theo_ngay (viec, ngay)`: khóa chính hai cột, `ket_qua`, `lan_chay_lich_id` (000). Bảo đảm mỗi việc mỗi ngày chạy đúng một lần, kể cả chạy bù (FR-077).
- `nhac_m02 (loai, doi_tuong_id)`: khóa chính, `lan_nhac_cuoi`, `so_lan`. Loại: `HO_SO_CHO_CAP_NHAT`, `PHAN_BO_LAI`, `HOP_DONG_SAP_HET`, `HOP_DONG_NGAN_NGAY`, `QUA_HAN_HOP_DONG`, `QUA_NGAY_DU_KIEN`, `VIEC_SAU_QUA_DOI`, `CHUA_VAO_O`, `XEM_XET_GIU_GIUONG`.

---

## 12. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bảo đảm | Cách làm | DBR / FR |
| --- | --- | --- |
| Tối đa một hợp đồng Hiệu lực tại một thời điểm | Khóa `luu_tru_nct` và trigger `trg_hop_dong_khong_chong` | DBR-06, FR-022 |
| Hợp đồng ngoài Nháp không sửa nội dung | Trigger `trg_hop_dong_khoa_noi_dung` và trigger bảng con | DBR-06, FR-020, FR-023 |
| Phụ lục gắn yêu cầu đã duyệt | `yeu_cau_phe_duyet_id NOT NULL UNIQUE`; chỉ `khiDuyet` tạo | DBR-07 |
| Phụ lục không sớm hơn ngày bắt đầu hợp đồng | Backend khi lập và khi duyệt | DBR-07, FR-040 |
| Phiên bản đơn giá không chồng khoảng; không sửa khi đã tham chiếu | Khóa `dich_vu`, hai trigger | DBR-08, FR-035 |
| Một lượt đăng ký, hồ sơ chờ, hồ sơ kết thúc, lượt vắng mở mỗi người | Cột sinh `UNIQUE` | FR-001, FR-007, FR-062 |
| Một dịch vụ chăm sóc Hiệu lực mỗi mức | Cột sinh `UNIQUE` | Q-297 |
| Một yêu cầu chờ hiệu lực mỗi thuộc tính | Cột sinh `UNIQUE` | FR-046 |
| Nhóm 3 chỉ thêm; lượt vắng đóng bất biến | Mẫu trigger 000; `trg_luot_vang_khong_sua` | DBR-23, FR-061 |
| Lịch đến không qua 00:00 | `CHECK (gio_ve > gio_den)` | FR-019, Q-286 |

---

## 13. Nhật ký và phân loại trường

- Hành động theo research T-15.
- `PhanLoaiTruongProvider`:
  - `TAI_CHINH`: các trường giá, khoản cọc, dòng ghi đè chính sách vắng của hợp đồng, phụ lục và yêu cầu (FR-028);
  - `SUC_KHOE`: `nguyenNhan` của bản ghi qua đời (FR-069);
  - `CONG_KHAI_NOI_BO`: các trường khác.
- `tinhHuongDacBiet` của hồ sơ chờ không phải thông tin sức khỏe. Nó được bảo vệ bằng quyền xem danh sách chờ (FR-018).

---

## 14. Lưu giữ (000 R-11)

| Loại | `LoaiHan` | Mốc |
| --- | --- | --- |
| `M02.LUOT_DANG_KY`, `M02.HO_SO_CHO`, `M02.HOP_DONG` (cùng bảng con, phiên bản nội dung, phụ lục), `M02.LUOT_VANG` (cùng ngày vắng, quyết định, lịch sử dự kiến), `M02.BAO_VANG_BAN_TRU`, `M02.HO_SO_KET_THUC` (cùng bàn giao, ngoại lệ), `M02.BAN_GHI_QUA_DOI` (cùng danh sách việc) | `THEO_HO_SO` | Thời điểm trạng thái cuối của người cao tuổi |
| `M02.NGAY_NGHI_BAN_TRU` | `KHONG_GAN_HO_SO` | `ngay` |

Dịch vụ, phiên bản đơn giá là danh mục, không bị loại bỏ.

---

## 15. Truy vết DBR, BR và FR

| Mã | Bảo đảm (mục) | Test (quickstart mục 4) |
| --- | --- | --- |
| DBR-02 (phần 004) | `NguoiDaiDienPort` khi Ghi nhận đã ký | `FR_021_GhiNhanDaKyIT` |
| DBR-06 | Mục 5, 12 | `DBR_06_HopDongKhongChongIT`, `HopDongKhoaNoiDungDbIT` |
| DBR-07 | Mục 7 | `DBR_07_PhuLucIT` |
| DBR-08 | Mục 6 | `DBR_08_PhienBanDonGiaIT` |
| DBR-23, DBR-25 | Mẫu trigger 000; `Clock` | `NhomBaTriggerIT` (000) mở rộng |
| BR-M02-01 | Research T-07 | `BR_M02_01_DanhSachPhuHopIT` |
| BR-M02-02 | `giuChoHoSoCho`, `hoSoChoHetHanGiuCho` | `BR_M02_02_GiuChoIT` |
| BR-M02-03 | Job `NHAC` | `BR_M02_03_NhacHoSoChoIT` |
| BR-M02-04 | Handler `M02.THAY_DOI_LUU_TRU` | `BR_M02_04_ApDungPhuLucIT` |
| BR-M02-05 | Job `HOP_DONG` | `BR_M02_05_NhacHopDongIT`, `FR_029_QuaHanHopDongIT` |
| BR-M02-06 | `TraChinhSachVang`, job `NGAY_VANG` | `BR_M02_06_TraChinhSachTest`, `NgayVangIT` |
| BR-M02-07 | Quyết định giữ giường | `BR_M02_07_QuyetDinhGiuGiuongIT` |
| BR-M02-08, 09 | Mục 5 | `DBR_06_HopDongKhongChongIT`, `BR_M02_09_NoiDungTaiNgayIT` |
| BR-M02-10 | `TinhDiemUuTien`, job `TINH_DIEM` | `BR_M02_10_DiemUuTienTest`, `DiemHangNgayIT` |
| BR-M02-15, 16 | Lệnh cập nhật; loại thay đổi `LICH_DEN` | `CapNhatLuotDangKyIT`, `DoiLichDenIT` |
| BR-M02-17, 18 | `TraChinhSachVang`; đính chính lượt vắng | `BR_M02_17_QuanLyQuyetDinhNgay1Test`, `BR_M02_18_DinhChinhVuotNguongIT` |
| BR-M02-19 | `lan_chay_theo_ngay` | `BR_M02_19_ChayBuIT` |
| BR-M02-20 | Lượt trống theo FR-012 (plan, điểm báo lại 1) | `FR_012_LuotTrongIT` |
| BR-M02-21 | `@NhomTruong(SUC_KHOE)` | `BR_M02_21_NguyenNhanTuVongIT` |
| BR-M02-22 | FR-004 (e), FR-025, FR-025b | `BR_M02_22_MucHopDongIT` |

BR-M02-11 → 14 (khai báo tạm trú) thuộc feature 018; 004 chỉ phát sự kiện (FR-033b).
