# Data Model: Sức khỏe, cảnh báo và sự cố (007)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1; không nhắc lại ở từng bảng. Người cao tuổi, nhân viên, người thân, ca, tầng/khu vực, phòng, công việc, đơn thuốc, lượt thăm, đồ gửi, tệp là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Thời điểm lưu `DATETIME(6)` (000 R-04; DBR-25). Giá trị chỉ số lưu `DECIMAL(7,2)`.

Bảng thuộc hai module: tiền tố mục "M06" là gói `m06`, "M05" là gói `m05` (research S-01).

---

## 1. Tổng quan

| Bảng | Module | Nhóm (1.5) | Mục spec |
| --- | --- | --- | --- |
| `chi_so_nct` | M06 | 2 (kỹ thuật) | Đích khóa theo người; mốc nhắc thiếu ngưỡng (S-03, S-07) |
| `chi_so`, `chi_so_thanh_phan` | M06 | 1 | A (FR-001) |
| `lich_do`, `lich_do_chi_so` | M06 | 2 | A (FR-003 → FR-006) |
| `ban_ghi_chi_so`, `ban_ghi_chi_so_thanh_phan` | M06 | 3 có bản nháp | B |
| `nguong_khoa` | M06 | Kỹ thuật | C (khóa ngưỡng mặc định) |
| `nguong_phien_ban`, `nguong_thanh_phan` | M06 | 2 | C |
| `giay_phep_co_so`, `giay_phep_co_so_pham_vi` | M06 | 1 có lịch sử | J (FR-070, FR-075) |
| `so_lien_he_vien` | M06 | 1 | J (FR-070) |
| `ban_ghi_kham` | M06 | 3 | J (FR-072 → FR-074) |
| `theo_doi_nct` | M05 | 2 (kỹ thuật) | Đích khóa theo người (S-03) |
| `quy_tac_xu_huong`, `loai_su_co`, `loai_dien_bien` | M05 | 1 | E, F, G |
| `canh_bao` | M05 | 2 | D |
| `nguon_canh_bao`, `lich_su_canh_bao`, `lan_nhac_canh_bao` | M05 | 3 | D |
| `su_kien_canh_bao_da_nhan` | M05 | Kỹ thuật | D (S-09) |
| `su_co` | M05 | 3 | F (nội dung) |
| `su_co_xu_ly` | M05 | 2 | F (trạng thái xử lý) |
| `dien_bien_su_co`, `doi_muc_su_co`, `nguoi_xu_ly_su_co` | M05 | 3 | F, G |
| `phan_cong_su_co` *(bổ sung 2026-10-11, Q-339)* | M05 | 3 | F (FR-045a) |
| `ban_chup_the_khan_cap`, `xac_nhan_doi_chieu`, `ban_tom_tat_chuyen_vien` | M05 | 3 | G |
| `dau_nguy_kich` | M05 | 2 | G1 |
| `ket_qua_xac_nhan_lai`, `lua_chon_thuc_hien` | M05 | 3 | G1 |
| `khoang_theo_doi_sau_nga` | M05 | 2 | H |
| `danh_sach_tiep_xuc`, `nguoi_tiep_xuc` | M05 | 2 | I |
| `khoanh_vung`, `khoanh_vung_pham_vi`, `khoanh_vung_su_co` | M05 | 2 | I |
| `lan_chay_m05`, `lan_chay_m06` | M05, M06 | Kỹ thuật | Job (S-21) |

---

## 2. Bảng kỹ thuật theo người

### `chi_so_nct` – M06, nhóm 2 (kỹ thuật)

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `nct_id` | `BIGINT` | Khóa chính; tạo khi lần đầu có lệnh M06 cho người đó | S-03 |
| `da_nhac_thieu_nguong_luc` | `DATETIME(6) NULL` | Một nhắc mỗi lần thiếu; xóa khi có ngưỡng cá nhân | FR-025 |
| `phien_ban` | `INT` | | |

### `theo_doi_nct` – M05, nhóm 2 (kỹ thuật)

`nct_id` (khóa chính), `phien_ban`. Đích khóa cho gộp cảnh báo, ghi sự cố Khẩn cấp, dấu nguy kịch, khoảng theo dõi sau ngã, xác nhận danh sách tiếp xúc (S-03).

---

## 3. Danh mục (M06, M05)

### `chi_so` – nhóm 1

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `ma`, `ten`, `don_vi`, `thu_tu` | | `UNIQUE (ma)`; sáu chỉ số khởi tạo `HUYET_AP`, `NHIP_TIM`, `NHIET_DO`, `SPO2`, `DUONG_HUYET`, `CAN_NANG` | FR-001 |
| `sinh_hieu` | `BOOLEAN` | Nhóm "sinh hiệu" cho theo dõi sau ngã | FR-001, FR-051 |
| `trang_thai_hieu_luc` | Mẫu 000 | | FR-002 |

### `chi_so_thanh_phan` – nhóm 1 (bảng con)

`chi_so_id` FK, `ma` (`GIA_TRI` khi chỉ một thành phần; `TAM_THU`, `TAM_TRUONG` với huyết áp), `ten`, `so_chu_so_thap_phan`. Khóa chính `(chi_so_id, ma)`. Khoảng hợp lệ vật lý **không** lưu ở đây: CFG-M06-03 kiểu `BANG` (research S-04).

### `quy_tac_xu_huong` – M05, nhóm 1

`ma` (`SUT_CAN`, `BO_LO_LAN_DO`, `MAT_NGU_KEO_DAI`, `TU_CHOI_THUOC`; khóa chính, không thêm được), `ten`, `bat` `BOOLEAN`, `muc` `ENUM('NHE','TRUNG_BINH','KHAN_CAP')`, `ma_tham_so` (CFG-M05-03, —, CFG-M05-05, CFG-M05-04), `phien_ban`. Đổi `bat`, `muc` ghi nhật ký trước/sau và lý do (FR-039).

### `loai_su_co` – M05, nhóm 1

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `ma`, `ten` | Khởi tạo: `NGA`, `LAY_NHIEM_NGHI_NGO`, `THUOC`, `AN_UONG`, `PHUC_VU_SAI_SUAT_AN`, `KHO_THO_MAT_Y_THUC`, `DI_LAC_KHONG_TRO_VE`, `HANH_VI`, `CHAN_THUONG_KHAC`, `CO_SO_VAT_CHAT`, `DO_GUI_THAT_LAC`, `DO_GUI_HU_HONG`, `KHAC` | FR-042 |
| `muc_mac_dinh` | `NGA` Khẩn cấp, `DI_LAC_KHONG_TRO_VE` Khẩn cấp, `DO_GUI_*` Trung bình | FR-042, 9.3 |
| `khong_thuoc_suc_khoe` | Bật mặc định cho `CO_SO_VAT_CHAT`, `DO_GUI_*`; đổi ghi nhật ký | FR-042, 016 FR-042a |
| `cho_phep_khong_nct` | Chỉ `CO_SO_VAT_CHAT` | FR-040, Edge Case |
| `trang_thai_hieu_luc` | Mẫu 000; mã của các loại có quy tắc riêng (`NGA`, `LAY_NHIEM_NGHI_NGO`, `DO_GUI_*`, `CO_SO_VAT_CHAT`) không Ngừng hiệu lực được | |

### `loai_dien_bien` – M05, nhóm 1

`id`, `ma`, `ten`, `nhom` (`DANH_GIA`, `HANH_DONG`, `THONG_BAO`, `THEO_DOI`, `HE_THONG`), `bien_phap_hoi_suc` `BOOLEAN`, `trang_thai_hieu_luc`. Khởi tạo: năm biện pháp hồi sức của FR-048a (cần Bác sĩ đối chiếu, 24.4); `GOI_HO_TRO`, `GOI_CAP_CUU`, `BAO_CO_QUAN_Y_TE`; loại hệ thống `NGUON_DA_XU_LY`, `CHUYEN_VIEN_BI_TU_CHOI`, `DA_TIM_THAY`, `DE_NGHI_HUY` (người ghi gốc không có quyền xử lý đề nghị hủy, FR-046).

---

## 4. Lịch đo (M06, mục A)

### `lich_do` – nhóm 2

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `nct_id` | | | FR-003 |
| `kieu_tan_suat` | `ENUM('MOC_GIO','MOI_N_GIO','THEO_THU')` | | FR-003 |
| `tham_so_tan_suat` | `JSON` | Mốc giờ; hoặc N và mốc gốc; hoặc thứ trong tuần và mốc giờ | FR-003 |
| `bat_dau`, `ket_thuc` | `DATETIME(6)`, `DATETIME(6) NULL` | | FR-003 |
| `muc_quan_trong` | `ENUM` của 005 | Mặc định `BAT_BUOC` | FR-003, BR-M04-06 |
| `ghi_chu` | `VARCHAR(2000)` | | |
| `nguon` | `ENUM('CHI_DINH_BAC_SI')` | Chỉ một giá trị (Q-307) | FR-003 |
| `thay_the_cho` | `BIGINT NULL` FK `lich_do` | | FR-005 |
| `trang_thai` | `ENUM('CHO_HIEU_LUC','HIEU_LUC','DA_NGUNG','HET_HAN')` | | FR-006 |
| `ngung_luc`, `ly_do_ngung`, `nguoi_ngung_id` | | Người là Hệ thống khi trạng thái cuối | FR-006 |
| `nguoi_tao_id`, `tao_luc`, `phien_ban` | | | |

Chỉ mục `(nct_id, trang_thai)`. Trigger `BEFORE UPDATE`: chặn đổi mọi cột trừ `trang_thai`, `ngung_luc`, `ly_do_ngung`, `nguoi_ngung_id`, `phien_ban`; chặn đổi khi `OLD.trang_thai IN ('DA_NGUNG','HET_HAN')` (FR-005, FR-006).

### `lich_do_chi_so` – nhóm 2 (bảng con)

`lich_do_id`, `chi_so_id`; khóa chính hai cột; không sửa sau khi lưu.

**Bảng chuyển `lich_do`** (khớp bảng FR-006):

| Từ | Lệnh / sự kiện | Đến | Người | Tác động |
| --- | --- | --- | --- | --- |
| — | Tạo | `HIEU_LUC` / `CHO_HIEU_LUC` | Bác sĩ | Căn cứ `LAN_DO` cho 005 |
| `CHO_HIEU_LUC` | Tới `bat_dau` | `HIEU_LUC` | Bộ lập lịch | — |
| `HIEU_LUC`, `CHO_HIEU_LUC` | Thay đổi lịch đo | `DA_NGUNG` | Bác sĩ | Lịch mới cùng transaction; `nguonDaDoi(LAN_DO)` |
| `HIEU_LUC`, `CHO_HIEU_LUC` | Ngừng lịch đo | `DA_NGUNG` | Bác sĩ | `nguonBiHuy(LAN_DO)` (plan điểm báo lại 2) |
| `HIEU_LUC` | Tới `ket_thuc` | `HET_HAN` | Bộ lập lịch | — |
| `HIEU_LUC`, `CHO_HIEU_LUC` | Trạng thái cuối | `DA_NGUNG` | Hệ thống | `nguonBiHuy(LAN_DO)` |

---

## 5. Bản ghi chỉ số (M06, mục B)

### `ban_ghi_chi_so` – nhóm 3 có bản nháp

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `nct_id`, `chi_so_id` | | | FR-010 |
| `thoi_diem_do` | `DATETIME(6)` | ≤ `tao_luc` (FR-012) | FR-012 |
| `nguoi_do_id`, `cong_viec_id` | | Công việc nguồn có thể rỗng (đo ngoài lịch) | FR-010 |
| `thoi_diem_thiet_bi`, `ngoai_tuyen` | | DBR-25; `tao_luc` là thời điểm đồng bộ | FR-010 |
| `nguong_phien_ban_id` | `BIGINT NULL` | Rỗng = chưa có ngưỡng | FR-013, FR-023 |
| `muc` | `ENUM('BINH_THUONG','CANH_BAO','NGUY_HIEM','CHUA_CO_NGUONG')` | Thành phần nặng nhất | FR-013 |
| `trang_thai_xac_nhan` | `ENUM('CHO_DO_LAI','DA_XAC_NHAN','DA_DO_LAI')` | | FR-015, FR-016 |
| `han_do_lai` | `DATETIME(6) NULL` | `thoi_diem_do + CFG-M06-04` | FR-017 |
| `do_lai_cho` | `BIGINT NULL` FK `ban_ghi_chi_so` | Lần đo lại trỏ bản ghi đầu; `UNIQUE` (một lần đo lại) | FR-016 |
| `nhan` | `SET('XU_LY_NGAY','CHUA_DO_LAI','GHI_NHAN_MUON','GHI_NGOAI_TUYEN')` | | FR-015, FR-017, FR-012 |
| `ghi_chu` | | | |
| `thoi_diem_xac_nhan` | `DATETIME(6) NULL` | Rỗng khi `CHO_DO_LAI` | 000 R-05 |
| `tao_luc`, `nguoi_tao_id` | | | |

Chỉ mục `(nct_id, chi_so_id, thoi_diem_do)` (lịch sử, sụt cân, gần nhất), `(trang_thai_xac_nhan, han_do_lai)` (job), `(cong_viec_id)`.

Trigger `BEFORE UPDATE`: cho phép đúng một lần đặt `thoi_diem_xac_nhan` từ `NULL` cùng `trang_thai_xac_nhan` (`DA_XAC_NHAN` hoặc `DA_DO_LAI`) và thêm nhãn `CHUA_DO_LAI`; mọi đổi khác, và mọi đổi khi `OLD.thoi_diem_xac_nhan IS NOT NULL`, bị chặn. `BEFORE DELETE` theo mẫu 000.

### `ban_ghi_chi_so_thanh_phan` – nhóm 3 (bảng con)

`ban_ghi_id`, `ma_thanh_phan`, `gia_tri` `DECIMAL(7,2)`, `muc_thanh_phan`. Khóa chính hai cột đầu. Không `UPDATE`.

**Bảng chuyển xác nhận** (BR-M06-04):

| Từ | Sự kiện | Đến | Ghi chú |
| --- | --- | --- | --- |
| — | Ghi, mức khác Nguy hiểm, hoặc Nguy hiểm + "xử lý ngay" | `DA_XAC_NHAN` | Phát `ChiSoDaXacNhan` |
| — | Ghi, Nguy hiểm | `CHO_DO_LAI` | `han_do_lai` |
| `CHO_DO_LAI` | Lần đo lại hợp lệ | `DA_DO_LAI` | Lần đo lại `DA_XAC_NHAN` và là nguồn cảnh báo |
| `CHO_DO_LAI` | Quá `han_do_lai` | `DA_XAC_NHAN` + nhãn `CHUA_DO_LAI` | Bộ lập lịch; cảnh báo Khẩn cấp |

---

## 6. Ngưỡng (M06, mục C)

### `nguong_khoa` – kỹ thuật

`chi_so_id` khóa chính; đích khóa cho lệnh ngưỡng mặc định (ngưỡng cá nhân khóa `chi_so_nct`).

### `nguong_phien_ban` – nhóm 2

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `pham_vi` | `ENUM('CO_SO','CA_NHAN')` | | FR-020 |
| `nct_id` | `BIGINT NULL` | Bắt buộc khi `CA_NHAN`, rỗng khi `CO_SO` (`CHECK`) | FR-020 |
| `chi_so_id`, `hieu_luc_tu`, `hieu_luc_den` | | `hieu_luc_den` đặt khi bị thay hoặc ngừng | FR-022, FR-024 |
| `trang_thai` | `ENUM('CHO_HIEU_LUC','HIEU_LUC','HET_HIEU_LUC','DA_HUY')` | | FR-022 |
| `nguoi_thiet_lap_id`, `ly_do`, `tao_luc` | | Lý do bắt buộc khi đã có phiên bản trước | FR-020 |
| `ly_do_ket_thuc`, `ket_thuc_boi_id` | | | FR-022 |
| `khoa_pham_vi` | Cột sinh `CONCAT(pham_vi, ':', IFNULL(nct_id, 0), ':', chi_so_id)` | | |
| `khoa_hieu_luc` | Cột sinh `IF(trang_thai = 'HIEU_LUC', khoa_pham_vi, NULL)`, `UNIQUE` | Tối đa một Hiệu lực | FR-022 |
| `khoa_cho` | Cột sinh `IF(trang_thai = 'CHO_HIEU_LUC', khoa_pham_vi, NULL)`, `UNIQUE` | Tối đa một Chờ hiệu lực | FR-022 (tác động) |

Chỉ mục `(nct_id, chi_so_id, hieu_luc_tu)`, `(pham_vi, chi_so_id, hieu_luc_tu)`. Trigger: chặn đổi `pham_vi`, `nct_id`, `chi_so_id`, `hieu_luc_tu`; chặn mọi đổi khi `OLD.trang_thai IN ('HET_HIEU_LUC','DA_HUY')`.

### `nguong_thanh_phan` – nhóm 2 (bảng con, không sửa)

`phien_ban_id`, `ma_thanh_phan`, `duoi_nguy_hiem`, `duoi_canh_bao`, `tren_canh_bao`, `tren_nguy_hiem` (`DECIMAL(7,2) NULL`). Trigger chặn `UPDATE`. Thứ tự cận và khoảng vật lý kiểm ở backend (`KiemTraNguong`, FR-021), vì khoảng nằm ở CFG.

**Bảng chuyển `nguong_phien_ban`** (khớp bảng FR-022):

| Từ | Lệnh / sự kiện | Đến | Người | Tác động |
| --- | --- | --- | --- | --- |
| — | Thiết lập | `HIEU_LUC` / `CHO_HIEU_LUC` | Bác sĩ | Cùng (phạm vi, chỉ số): `HIEU_LUC` → `HET_HIEU_LUC` tại `hieu_luc_tu`; `CHO_HIEU_LUC` cũ → `DA_HUY` |
| `CHO_HIEU_LUC` | Tới `hieu_luc_tu` | `HIEU_LUC` | Bộ lập lịch | Như trên |
| `CHO_HIEU_LUC` | Hủy phiên bản | `DA_HUY` | Bác sĩ | Lý do |
| `HIEU_LUC` | Phiên bản mới có hiệu lực | `HET_HIEU_LUC` | Hệ thống | — |
| `HIEU_LUC` | Ngừng ngưỡng cá nhân | `HET_HIEU_LUC` | Bác sĩ | Chỉ `CA_NHAN`; xóa `chi_so_nct.da_nhac_thieu_nguong_luc` |
| `HIEU_LUC`, `CHO_HIEU_LUC` | Trạng thái cuối | `HET_HIEU_LUC` / `DA_HUY` | Hệ thống | Chỉ `CA_NHAN` |

---

## 7. Cảnh báo (M05, mục D)

### `canh_bao` – nhóm 2

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `nct_id`, `tang_khu_vuc_id` | | Tầng xác định lúc tạo theo S-08; cập nhật khi chuyển giường không cần (người nhận xác định lúc gửi) | FR-027, FR-031 |
| `loai`, `khoa_loai` | `VARCHAR(64)`, `VARCHAR(128)` | `KhoaLoai.tu` | FR-028 |
| `muc` | `ENUM('NHE','TRUNG_BINH','KHAN_CAP')` | Mức hiện hành | FR-027 |
| `muc_luc_tiep_nhan` | `ENUM NULL` | Cho báo cáo | FR-081 |
| `so_lan` | `INT` | = số dòng `nguon_canh_bao` | FR-030 |
| `lan_dau_luc`, `gan_nhat_luc` | | `lan_dau_luc` không đặt lại khi gộp | FR-027, FR-081 |
| `trang_thai` | `ENUM('MOI','LEO_THANG','DA_TIEP_NHAN','DANG_XU_LY','DA_DONG','CHUYEN_SU_CO')` | | FR-029 |
| `cap_leo_thang` | `TINYINT` 0..2 | | FR-033 |
| `han_tiep_nhan` | `DATETIME(6) NULL` | Rỗng với Khẩn cấp, sau tiếp nhận | FR-031 |
| `leo_thang_toi_da_luc`, `so_lan_nhac_toi_da` | | | FR-034 |
| `nguoi_phu_trach_id`, `tam_nhan_ban_giao` | | | FR-031, FR-037 |
| `nguoi_tiep_nhan_id`, `tiep_nhan_luc` | | Với kích hoạt khẩn cấp: thời điểm kích hoạt | FR-081 |
| `ket_qua`, `ly_do_dong`, `nguoi_dong_id`, `dong_boi_he_thong`, `dong_luc` | | | FR-029, FR-032 |
| `su_co_id` | `BIGINT NULL` | Khi Chuyển sự cố | FR-029 |
| `dau_nguy_kich_id` | `BIGINT NULL` | Cảnh báo nguy kịch | FR-050b |
| `khong_co_bac_si_truc` | `BOOLEAN` | | FR-047b |
| `dung_leo_thang` | `BOOLEAN` | Trạng thái cuối | FR-038 |
| `tao_boi_id`, `mo_ta` | | Thủ công: bắt buộc mô tả | FR-026 (c) |
| `khoa_mo` | Cột sinh `IF(trang_thai IN ('MOI','LEO_THANG','DA_TIEP_NHAN','DANG_XU_LY'), khoa_loai, NULL)`; `UNIQUE (nct_id, khoa_mo)` | DBR-18 |
| `phien_ban` | | | |

Chỉ mục `(trang_thai, han_tiep_nhan)` (job), `(tang_khu_vuc_id, trang_thai)` (dashboard, bàn giao), `(nct_id, trang_thai)`, `(nguoi_phu_trach_id, trang_thai)`. Trigger: chặn đổi `nct_id`, `khoa_loai`, `lan_dau_luc`; chặn mọi đổi khi `OLD.trang_thai IN ('DA_DONG','CHUYEN_SU_CO')` trừ khi cột đổi duy nhất là `phien_ban` (nguồn đã xử lý ghi ở `nguon_canh_bao`, không ở cảnh báo).

### `nguon_canh_bao` – nhóm 3

`id`, `canh_bao_id`, `loai_nguon` (`CHI_SO`, `CONG_VIEC`, `LIEU_THUOC`, `LAN_DUNG_PRN`, `DON_THUOC`, `THUOC_NCT`, `BUA_AN`, `KET_QUA_NGU`, `HOAT_DONG`, `THU_CONG`, `DAU_NGUY_KICH`…), `nguon_id`, `muc`, `thoi_diem`, `khoa_su_kien`, `da_xu_ly_luc` `NULL`, `ly_do_xu_ly`. Chỉ mục `(loai_nguon, nguon_id)`, `(canh_bao_id, thoi_diem)`. Trigger chỉ cho đặt `da_xu_ly_luc`, `ly_do_xu_ly` một lần từ `NULL`.

### `lich_su_canh_bao` – nhóm 3

`canh_bao_id`, `thoi_diem`, `su_kien` (`TAO`, `GOP`, `LEO_THANG`, `LEO_THANG_TOI_DA`, `TIEP_NHAN`, `GIAO`, `BAT_DAU_XU_LY`, `NANG_MUC`, `HA_MUC`, `CHUYEN_NGUOI_PHU_TRACH`, `TAM_NHAN`, `NGUON_DA_XU_LY`, `NGUON_DA_DINH_CHINH`, `BAO_LAP_LAI`, `DONG`, `CHUYEN_SU_CO`), `cap_cu`, `cap_moi`, `muc_cu`, `muc_moi`, `nguoi_cu_id`, `nguoi_moi_id`, `nhom_nguoi_nhan` `JSON`, `nguoi_thuc_hien_id` (rỗng = Hệ thống), `thoi_diem_du_kien` (chạy bù), `ly_do`.

### `lan_nhac_canh_bao` – nhóm 3

`canh_bao_id` hoặc `su_co_id`, `so_thu_tu`, `thoi_diem`, `loai` (`NHAC_LEO_THANG_TOI_DA`). `UNIQUE (loai, canh_bao_id, su_co_id, so_thu_tu)` (Q-266). Không tính vào số lần leo thang (FR-034).

### `su_kien_canh_bao_da_nhan` – kỹ thuật

`khoa` `VARCHAR(191)` khóa chính, `nhan_luc`. Chống xử lý trùng khi outbox giao lại (NFR-04).

**Bảng chuyển `canh_bao`** (khớp bảng FR-029):

| Từ | Lệnh / sự kiện | Đến | Người | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| — | Tạo | `MOI` | Hệ thống; TT, BS, ĐD (thủ công) | Chưa trạng thái cuối; không có cảnh báo mở cùng khóa | Người phụ trách, hạn; thông báo |
| Đang mở | Phát sinh cùng loại | Giữ | Hệ thống | — | Gộp; nâng mức nếu cao hơn |
| `MOI`, `LEO_THANG` | Quá hạn | `LEO_THANG` | Bộ lập lịch | `cap_leo_thang < 2`, không `dung_leo_thang` | Lên một cấp, hạn mới |
| `MOI`, `LEO_THANG` | Tiếp nhận | `DA_TIEP_NHAN` | ĐD, TT, BS; QL khi cấp 2 | — | Dừng leo thang |
| `DA_TIEP_NHAN` | Giao người phụ trách | Giữ | QL (người phụ trách) | Người được giao có quyền; ghi chú | Thông báo |
| `DA_TIEP_NHAN` | Bắt đầu xử lý | `DANG_XU_LY` | Người có quyền FR-036 | — | — |
| `DANG_XU_LY` | Đóng | `DA_DONG` | ĐD, TT, BS | Kết quả; lý do với quá hạn Bắt buộc; nguy kịch: FR-050d | `capNhat` việc sau qua đời nếu trạng thái cuối |
| `DANG_XU_LY` | Chuyển sự cố | `CHUYEN_SU_CO` | ĐD, TT, BS | — | Tạo sự cố liên kết |
| `MOI`, `LEO_THANG`, `DA_TIEP_NHAN` | Kích hoạt khẩn cấp từ cảnh báo | `CHUYEN_SU_CO` | ĐD, TT, BS | Ba dòng lịch sử | Sự cố Khẩn cấp, mục G |
| Đang mở | Mọi nguồn đã xử lý | `DA_DONG` | Hệ thống | Không phải nguy kịch | Kết quả "nguồn đã được xử lý" |
| Đang mở | Nâng / Hạ mức | Giữ | ĐD, TT, BS; hạ từ Khẩn cấp: BS | Lý do | Hạn tính lại nếu `MOI`, `LEO_THANG` |
| Đang mở | Bàn giao được xác nhận | Giữ | Hệ thống | Người phụ trách thuộc ca trước | Đổi người phụ trách |
| Đang mở | Trạng thái cuối | Giữ | Hệ thống | — | `dung_leo_thang` |

---

## 8. Sự cố (M05, mục F, G)

### `su_co` – nhóm 3 (nội dung, không `UPDATE`)

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id NULL`, `loai_su_co_id` | `nct_id` rỗng chỉ khi loại `cho_phep_khong_nct` | FR-040 |
| `nguon_phat_sinh` | `ENUM` 10 giá trị của FR-040 | FR-040 |
| `xay_ra_luc NULL`, `phat_hien_luc` | | FR-040, FR-051 |
| `tang_khu_vuc_id`, `dia_diem` | Bắt buộc với `CO_SO_VAT_CHAT` (từ cơ cấu 003) | FR-040, CHK004 |
| `hoat_dong_dang_dien_ra`, `nguoi_phat_hien`, `mo_ta`, `xu_ly_ban_dau` | | FR-040 |
| `muc_ban_dau`, `ly_do_muc_thap` | Lý do khi thấp hơn mức mặc định | FR-042 |
| `canh_bao_nguon_id NULL` | | FR-040 |
| `do_gui_id NULL`, `ban_giao_do_gui_id NULL` | Sự cố đồ gửi | FR-040, FR-046b |
| `ngoai_pham_vi` | Ghi theo ngoại lệ 002 FR-044a | FR-041, SC-012 |
| `nguoi_ghi_id`, `tao_luc`, `thoi_diem_xac_nhan` | Xác nhận ngay khi lưu | 000 R-05 |

Sự cố lây nhiễm nghi ngờ bắt buộc `nct_id` (BR-M05-18); backend kiểm khi ghi nhận và khi đính chính đổi loại.

### `su_co_xu_ly` – nhóm 2 (1–1 với `su_co`)

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `su_co_id` | Khóa chính | |
| `trang_thai` | `ENUM('MOI','DANG_XU_LY','DANG_THEO_DOI','DA_DONG','DA_HUY')` | FR-045 |
| `muc`, `da_tung_khan_cap` | Mức hiện hành; cờ bảo vệ | FR-044, FR-046 |
| `loai_su_co_id_hien_hanh` | Theo đính chính đổi loại | FR-046a |
| `cap_leo_thang`, `han_tiep_nhan`, `leo_thang_toi_da_luc`, `so_lan_nhac_toi_da` | | FR-044 |
| `nguoi_phu_trach_id`, `tam_nhan_ban_giao` | | FR-037 |
| `tiep_nhan_luc`, `nguoi_tiep_nhan_id` | Cho 016 (Q-199) | FR-081 |
| `ket_qua`, `ket_qua_lay_nhiem` (`XAC_NHAN_NHIEM`, `LOAI_TRU`, `KHONG_XAC_DINH`), `nguoi_dong_id`, `dong_luc` | | FR-045, FR-066 |
| `khong_co_bac_si_truc`, `dung_leo_thang`, `phien_ban` | | FR-047b, FR-038 |
| `khoa_khan_cap_mo` | Cột sinh `IF(muc = 'KHAN_CAP' AND trang_thai IN ('MOI','DANG_XU_LY','DANG_THEO_DOI'), 1, NULL)` cùng `nct_id` chép từ `su_co` — **không** `UNIQUE` (cho phép tạo sự cố riêng, FR-041); dùng chỉ mục để tìm | FR-041 |

Trigger: chặn mọi đổi khi `OLD.trang_thai IN ('DA_DONG','DA_HUY')`, trừ `DA_DONG → DA_HUY` và đổi `loai_su_co_id_hien_hanh` khi đi kèm bản đính chính (cờ phiên `@dinh_chinh_id` do service đặt, mẫu 000).

**Bảng chuyển `su_co_xu_ly`** (khớp bảng FR-045):

| Từ | Lệnh / sự kiện | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Ghi nhận | `MOI` | Nhân viên (FR-041) | FR-040 |
| — | Chuyển sự cố / Kích hoạt khẩn cấp từ cảnh báo | `DANG_XU_LY` | ĐD, TT, BS | FR-029 |
| `MOI` | Tiếp nhận xử lý | `DANG_XU_LY` | ĐD, TT, BS | — |
| `MOI` | Quá hạn | `MOI` (`cap_leo_thang` + 1) | Bộ lập lịch | FR-044 |
| `MOI` | Giao người xử lý *(FR-045a)* | `DANG_XU_LY` | TT trong phạm vi, BS, QL | Ghi chú; người được giao có `SU_CO, XU_LY` với đối tượng; dừng leo thang |
| `DANG_XU_LY`, `DANG_THEO_DOI` | Giao người xử lý khác *(FR-045a)* | Giữ (đổi `nguoi_phu_trach_id`) | Người xử lý hiện tại, TT trong phạm vi, BS, QL | Như trên; khác người xử lý hiện tại |
| `MOI`, `DANG_XU_LY`, `DANG_THEO_DOI` | Ghi diễn biến; Nâng / Hạ mức | Giữ | FR-045 | Lý do khi đổi mức |
| `DANG_XU_LY` | Chuyển theo dõi | `DANG_THEO_DOI` | ĐD, TT, BS | ≥ 1 diễn biến `DANH_GIA` và ≥ 1 `HANH_DONG` |
| `DANG_THEO_DOI` | Mở lại xử lý | `DANG_XU_LY` | ĐD, TT, BS | Lý do |
| `MOI`, `DANG_XU_LY` | Chuyển viện | Giữ | ĐD, BS | FR-049 |
| `DANG_XU_LY`, `DANG_THEO_DOI` | Đóng | `DA_DONG` | Nhẹ: ĐD, TT, BS; khác: ĐD, BS | Kết quả; FR-066 |
| Mọi trạng thái trừ `DA_HUY` | Đính chính Hủy ghi nhận | `DA_HUY` | FR-046 | Lý do; FR-066 |
| Mọi trạng thái trừ `DA_HUY` | Nguồn đồ gửi bị hủy | `DA_HUY` | Hệ thống (013) | FR-046b |

Bảng FR-045 có dòng "Hệ thống tạo (thiếu người khi trở về, phục vụ suất có thành phần gây dị ứng)"; theo FR-040 và Q-307 thiết kế không có đường này (plan điểm báo lại 1).

### `dien_bien_su_co` – nhóm 3

`id`, `su_co_id`, `loai_dien_bien_id`, `noi_dung`, `dien_ra_luc`, `nguoi_ghi_id NULL` (rỗng = Hệ thống), `tao_luc`, `ngoai_pham_vi`. Chỉ mục `(su_co_id, dien_ra_luc)`.

### `doi_muc_su_co` – nhóm 3

`su_co_id`, `muc_cu`, `muc_moi`, `ly_do`, `nguoi_id`, `thoi_diem`.

### `phan_cong_su_co` – nhóm 3 *(bổ sung 2026-10-11, FR-045a, Q-339)*

`id`, `su_co_id`, `nguoi_giao_id`, `nguoi_duoc_giao_id`, `ghi_chu` (`NOT NULL`), `trang_thai_luc_giao`, `thoi_diem`. Chỉ mục `(su_co_id, thoi_diem)`. Trigger nhóm 3 chặn `UPDATE`, `DELETE`. Mỗi lệnh giao một dòng; người xử lý hiện tại vẫn ở `su_co_xu_ly.nguoi_phu_trach_id`.

### `nguoi_xu_ly_su_co` – nhóm 3

`su_co_id`, `nhan_vien_id`, `vai_tro_tham_gia` (`GHI_NHAN`, `TIEP_NHAN`, `THAM_GIA`, `GHI_DIEN_BIEN`), `tu_luc`. `UNIQUE (su_co_id, nhan_vien_id, vai_tro_tham_gia)`.

### `ban_chup_the_khan_cap` – nhóm 3

`su_co_id`, `thoi_diem`, `noi_dung` `JSON` (mục FR-048, gồm các mục "không lấy được"), `nguoi_duoc_hien_thi` `JSON`. Một dòng mỗi lần kích hoạt (tạo hoặc nâng lên Khẩn cấp).

### `xac_nhan_doi_chieu` – nhóm 3

`su_co_id` `UNIQUE`, `nguoi_xac_nhan_id`, `thoi_diem`, `nguyen_vong_phien_ban_id`. DBR-19.

### `ban_tom_tat_chuyen_vien` – nhóm 3

`id`, `su_co_id NULL`, `canh_bao_id NULL` (nguy kịch), `nct_id`, `co_so_tiep_nhan`, `noi_dung` `JSON` (dị ứng, thuốc đang dùng, chỉ số gần nhất mỗi loại kèm thời điểm, diễn biến, nguyện vọng), `nguoi_lap_id`, `thoi_diem`, `lich_su_trang_thai_id` (của 001).

---

## 9. Nguy kịch (M05, mục G1)

### `dau_nguy_kich` – nhóm 2

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `nct_id`, `loai` (`CHINH_THUC`, `TAM`) | | FR-050a |
| `trang_thai` | `ENUM('TAM','DANG_MO','DA_GO')` | Bảng dấu nguy kịch |
| `nhan_dinh`, `nguoi_ghi_id`, `ghi_luc` | | |
| `bac_si_xac_nhan_id`, `xac_nhan_luc` | Dấu tạm được xác nhận | Q-218 |
| `go_luc`, `nguoi_go_id NULL`, `ly_do_go` | Rỗng người = Hệ thống ("tự gỡ theo trạng thái") | |
| `canh_bao_id` | `NOT NULL`, `UNIQUE` | DBR-34 |
| `khoa_mo` | Cột sinh `IF(trang_thai <> 'DA_GO', nct_id, NULL)`, `UNIQUE` | DBR-34 |

Trigger: chặn mọi đổi khi `OLD.trang_thai = 'DA_GO'`; chặn đổi `nct_id`, `canh_bao_id`, `nhan_dinh`.

| Từ | Lệnh / sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Ghi dấu | `DANG_MO` | Bác sĩ |
| — | Ghi dấu tạm | `TAM` | Điều dưỡng (không có Bác sĩ trực) |
| `TAM` | Xác nhận | `DANG_MO` | Bác sĩ |
| `DANG_MO`, `TAM` | Gỡ | `DA_GO` | Bác sĩ (lý do) |
| `DANG_MO`, `TAM` | Chuyển viện, trạng thái cuối | `DA_GO` | Hệ thống |

### `ket_qua_xac_nhan_lai` – nhóm 3

`canh_bao_id`, `nguoi_duoc_lien_he` (người thân id hoặc tên), `kenh` (`GOI_DIEN`, `TRUC_TIEP`), `thoi_diem`, `ket_qua` (`GIU`, `MOI`, `KHONG_LIEN_LAC_DUOC`), `nguyen_vong_phien_ban_moi_id NULL`, `nguoi_ghi_id`.

### `lua_chon_thuc_hien` – nhóm 3

`canh_bao_id`, `lua_chon` (`CHUYEN_BENH_VIEN`, `VE_NHA`, `GIAM_NHE`), `ly_do` (bắt buộc khi không liên lạc được và không có nguyện vọng, FR-050c), `ban_tom_tat_id NULL`, `nguoi_ghi_id`, `thoi_diem`.

---

## 10. Ngã, tiếp xúc, khoanh vùng (M05, mục H, I)

### `khoang_theo_doi_sau_nga` – nhóm 2

`id`, `nct_id`, `su_co_id`, `moc`, `ket_thuc`, `khoang_do_phut` (chốt từ CFG-M05-06 lúc tạo), `trang_thai` (`DANG_THEO_DOI`, `DUOC_THAY`, `KET_THUC_SOM`, `DA_HET`), `ly_do`, `ket_thuc_boi_id`. Cột sinh `khoa_dang = IF(trang_thai = 'DANG_THEO_DOI', nct_id, NULL)` `UNIQUE` (FR-052). Trigger chặn đổi `moc`, `ket_thuc`, và mọi đổi từ trạng thái cuối.

### `danh_sach_tiep_xuc` – nhóm 2

`id`, `su_co_id` `UNIQUE`, `trang_thai` (`NHAP`, `DA_XAC_NHAN`, `DA_KET_THUC`), `khong_co_nguoi_tiep_xuc`, `ly_do_khong_co`, `truy_vet_tu`, `truy_vet_den` (tính từ `phat_hien_luc` và CFG-M05-07, chốt lúc lập), `nguoi_lap_id`, `nguoi_xac_nhan_id`, `xac_nhan_luc`, `phien_ban`.

| Từ | Lệnh / sự kiện | Đến | Điều kiện |
| --- | --- | --- | --- |
| — | Lập | `NHAP` | Sự cố lây nhiễm chưa đóng; chưa có danh sách |
| `NHAP` | Thêm, bỏ người | `NHAP` | Có nhóm, nguồn, khoảng |
| `NHAP` | Xác nhận | `DA_XAC_NHAN` | ≥ 1 người hoặc "không có" kèm lý do |
| `DA_XAC_NHAN` | Bổ sung; Loại người | `DA_XAC_NHAN` | Lý do |
| `NHAP`, `DA_XAC_NHAN` | Sự cố đóng | `DA_KET_THUC` | — |

### `nguoi_tiep_xuc` – nhóm 2 (theo dòng)

`id`, `danh_sach_id`, `loai` (`NGUOI_CAO_TUOI`, `NHAN_VIEN`, `NGUOI_THAN`, `NGUOI_DI_CUNG`), `doi_tuong_id NULL`, `ho_ten`, `quan_he`, `so_dien_thoai` (người đi cùng, người thân), `nhom` (`CUNG_PHONG`, `HOAT_DONG`, `NHAN_VIEN_CHAM_SOC`, `NGUOI_THAN_THAM`, `KHAC`), `nguon`, `tiep_xuc_tu`, `tiep_xuc_den`, `ly_do_them`, `them_luc`, `them_boi_id`, `theo_doi_tu`, `theo_doi_den` (chỉ người cao tuổi), `bi_loai_luc`, `ly_do_loai`, `co_kenh_thong_bao` (người thân). Khi `DA_XAC_NHAN`, dòng chỉ được đặt `bi_loai_luc` (trigger). `UNIQUE (danh_sach_id, loai, doi_tuong_id)` với `doi_tuong_id` có giá trị.

Dấu "tiếp xúc" tính lúc đọc: tồn tại dòng `NGUOI_CAO_TUOI` chưa bị loại có `theo_doi_tu <= t < theo_doi_den` (khoảng trùng tự gộp).

### `khoanh_vung` – nhóm 2

`id`, `trang_thai` (`DANG_KHOANH_VUNG`, `DA_GO`), `bat_dau`, `ket_thuc`, `nguoi_khoanh_id`, `ly_do`, `nguoi_go_id`, `ly_do_go`, `phien_ban`. Trigger chặn đổi khi `DA_GO`.

### `khoanh_vung_pham_vi` – nhóm 2 (không sửa)

`khoanh_vung_id`, `loai` (`TANG`, `KHU_VUC`), `doi_tuong_id`. Một vùng là các tầng, hoặc đúng một khu vực (`CHECK` ở backend; Q-69).

### `khoanh_vung_su_co` – nhóm 2

`khoanh_vung_id`, `su_co_id`, `gan_luc`, `go_luc NULL`. Lệnh gắn thêm sự cố (để gỡ ràng buộc FR-066) thêm dòng.

---

## 11. Giấy phép cơ sở, liên hệ, khám (M06, mục J)

### `giay_phep_co_so` – nhóm 1 có lịch sử (không `UPDATE`, không `DELETE`)

`id`, `so`, `co_quan_cap`, `ngay_cap`, `ngay_het_han`, `co_kham_chua_benh`, `tep_scan_id`, `thay_the_cho NULL`, `ly_do_thay`, `thay_the_do_sai_sot` (đặt trên bản mới, nghĩa là bản `thay_the_cho` không còn dùng), `nguoi_tao_id`, `tao_luc`.

### `giay_phep_co_so_pham_vi`

`giay_phep_id`, `ma_pham_vi` (danh mục phạm vi hành nghề của 008). Khóa chính hai cột.

### `so_lien_he_vien` – nhóm 1

Một dòng: `so_dien_thoai`, `ghi_chu`, `phien_ban`. Đổi ghi nhật ký (FR-070).

### `ban_ghi_kham` – nhóm 3

`id`, `nct_id`, `loai` (`KHAM`, `DANH_GIA`, `CHAN_DOAN`, `CHI_DINH`, `DIEU_TRI`, `KET_QUA`), `noi_dung`, `tai_vien`, `ten_co_so_ngoai`, `nguoi_thuc_hien_id NULL`, `ten_nguoi_thuc_hien_ngoai`, `thuc_hien_luc`, `tep_ids` `JSON`, `nguoi_ghi_id`, `tao_luc`, `can_cu_phap_ly` `JSON` (giấy phép bác sĩ, giấy phép cơ sở lúc lưu, cho FR-073). `CHECK (tai_vien OR ten_co_so_ngoai IS NOT NULL)`.

---

## 12. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bất biến | Cách bảo đảm | Nguồn |
| --- | --- | --- |
| Một cảnh báo đang mở mỗi (người, khóa loại) | `UNIQUE (nct_id, khoa_mo)` + khóa `theo_doi_nct` | DBR-18 |
| Sự cố Khẩn cấp không sửa, không xóa | `su_co`, `dien_bien_su_co`, `xac_nhan_doi_chieu`, `ban_chup_the_khan_cap` nhóm 3 (không `UPDATE`, `DELETE` theo mẫu 000); `su_co_xu_ly` trigger trạng thái cuối | DBR-19, BR-M05-08 |
| Có nguyện vọng thì phải có xác nhận đối chiếu trước biện pháp hồi sức | Backend (S-12); `xac_nhan_doi_chieu.su_co_id UNIQUE` | DBR-19, BR-M05-13 |
| Một dấu nguy kịch mở mỗi người; một cảnh báo mỗi dấu | `UNIQUE khoa_mo`, `UNIQUE canh_bao_id`, `NOT NULL` | DBR-34 |
| Một phiên bản ngưỡng Hiệu lực, một Chờ hiệu lực mỗi (phạm vi, chỉ số) | `UNIQUE khoa_hieu_luc`, `UNIQUE khoa_cho` | FR-022 |
| Phiên bản ngưỡng, lịch đo không sửa nội dung | Trigger | FR-005, FR-023 |
| Bản ghi chỉ số không sửa sau xác nhận; một lần đo lại | Trigger nhóm 3 có bản nháp; `UNIQUE do_lai_cho` | BR-M06-06, FR-016 |
| Một khoảng theo dõi sau ngã đang diễn ra | `UNIQUE khoa_dang` | FR-052 |
| Một danh sách tiếp xúc mỗi sự cố | `UNIQUE su_co_id` | FR-061 |
| Lần nhắc leo thang tối đa không trùng | `UNIQUE (loai, canh_bao_id, su_co_id, so_thu_tu)` | Q-266, NFR-04 |
| Giấy phép cơ sở không sửa, không xóa | Trigger | FR-075 |
| Mọi thay đổi có nhật ký | `NhatKyApi.ghi` trong transaction | DBR-23 |
| Thời điểm theo Asia/Ho_Chi_Minh; ngoại tuyến lưu hai mốc | `Clock`; `thoi_diem_thiet_bi` | DBR-25 |

---

## 13. Nhật ký và phân loại trường

- Loại đối tượng nhật ký: `M06.LICH_DO`, `M06.NGUONG`, `M06.BAN_GHI_CHI_SO`, `M06.GIAY_PHEP_CO_SO`, `M06.SO_LIEN_HE_VIEN`, `M06.BAN_GHI_KHAM`, `M06.CHI_SO` (danh mục), `M05.CANH_BAO`, `M05.SU_CO`, `M05.DAU_NGUY_KICH`, `M05.KHOANG_THEO_DOI_SAU_NGA`, `M05.DANH_SACH_TIEP_XUC`, `M05.KHOANH_VUNG`, `M05.QUY_TAC_XU_HUONG`, `M05.LOAI_SU_CO`, `M05.LOAI_DIEN_BIEN`.
- Dấu nhật ký riêng: `ngoaiPhamVi` (FR-041), `apDungBu` và `thoiDiemDuKien` (FR-084), `khongCoBacSiTruc` (FR-047b).
- `PhanLoaiTruongProvider`: `giaTri`, `muc`, `noiDung` của khám, `moTa`, `dienBien`, `theKhanCap`, `nguyenVong`, `nhanDinh` → `SUC_KHOE`; `soDienThoai` của người tiếp xúc → `LIEN_HE`.
- Loại đính chính (`DinhChinhDuocProvider`): `M06.BAN_GHI_CHI_SO`, `M06.BAN_GHI_KHAM`, `M05.SU_CO`, `M05.DIEN_BIEN_SU_CO`, `M05.KET_QUA_XAC_NHAN_LAI`, `M05.LUA_CHON_THUC_HIEN`. `tangKhuVuc` là tầng của người cao tuổi lúc ghi (sự cố cơ sở vật chất: tầng/khu của sự cố).

---

## 14. Lưu giữ (000 R-11)

| Loại dữ liệu | Hạn | Chuỗi tham chiếu |
| --- | --- | --- |
| Bản ghi chỉ số, ngưỡng cá nhân, lịch đo, khám | `THEO_HO_SO` (CFG-M15-03) | Đính chính; cảnh báo trỏ tới |
| Cảnh báo, nguồn, lịch sử | `THEO_HO_SO` | Sự cố liên kết |
| Sự cố gắn người cao tuổi và các bảng con, dấu nguy kịch, kết quả, lựa chọn, bản tóm tắt | `THEO_HO_SO` | Danh sách tiếp xúc; khoanh vùng |
| Sự cố không gắn người cao tuổi, khoanh vùng, ngưỡng mặc định, giấy phép cơ sở | `KHONG_GAN_HO_SO` (CFG-M15-04) | Sự cố gắn |
| Danh sách tiếp xúc | Theo sự cố | Người tiếp xúc là người cao tuổi khác: giữ tới khi cả chuỗi hết hạn |

---

## 15. Truy vết DBR, BR và FR

| Mã | Bảo đảm ở | Test |
| --- | --- | --- |
| DBR-18 | Mục 7, 12; research S-03, S-08 | `DBR_18_GopDongThoiIT` (SC-003) |
| DBR-19 | Mục 8, 12; S-12 | `DBR_19_DoiChieuNguyenVongIT`, `SuCoTriggerIT` |
| DBR-23 | Nhật ký trong transaction; provider đính chính | `NhomBaTriggerIT` (000) mở rộng |
| DBR-25 | `thoi_diem_thiet_bi`, `Clock` | `ChiSoNgoaiTuyenIT` |
| DBR-34 | Mục 9 | `DBR_34_DauNguyKichIT` |
| BR-M05-01 | S-08, job | `BR_M05_01_LeoThangIT`, `ChuoiLeoThangTest` |
| BR-M05-02 | S-03, S-08 | `BR_M05_02_GopCanhBaoIT` |
| BR-M05-03 | S-08 (`DemLap24h`) | `BR_M05_03_BaoLapLaiIT` |
| BR-M05-04 | S-10 | `SutCanTest`, `DemDemMatNguTest`, `BR_M05_04_BoLoLanDoIT` |
| BR-M05-05 | S-18 | `BR_M05_05_BanGiaoIT`, `FR_037_TamNhanIT` |
| BR-M05-06 | S-12 | `BR_M05_06_ThongBaoKhanCapIT`, `FR_047b_KhongCoBacSiTrucIT` |
| BR-M05-07 | S-14 | `BR_M05_07_NgaIT`, `MocTheoDoiSauNgaTest` |
| BR-M05-08 | Mục 8, 12 | `SuCoTriggerIT` |
| BR-M05-09 | S-11 | `BR_M05_09_DongSuCoIT` |
| FR-045a (Q-339) | `phan_cong_su_co`; bảng chuyển `su_co_xu_ly`; research mục sự cố | `FR_045a_GiaoNguoiXuLyIT` |
| BR-M05-10, 11, 12 | S-15 | `BR_M05_10_TraCuuTiepXucIT` (SC-008), `BR_M05_11_KhoanhVungIT`, `BR_M05_12_GoKhoanhVungIT` |
| BR-M05-13 | S-12 | `BR_M05_13_TheKhanCapIT` |
| BR-M05-14 | S-12 | `BR_M05_14_ChuyenVienIT` |
| BR-M05-15, 16 | S-13 | `BR_M05_15_NguyKichIT`, `BR_M05_16_XacNhanLaiIT` |
| BR-M05-17 | S-11 (`cho_phep_khong_nct`, tầng/khu bắt buộc) | `BR_M05_17_SuCoCoSoVatChatIT` |
| BR-M05-18 | `nct_id` bắt buộc với `LAY_NHIEM_NGHI_NGO` (backend) | `BR_M05_18_LayNhiemGanNctIT` |
| BR-M05-19 | S-08 (tầng của người vắng mặt; hạn không dừng; Nhẹ sát giờ kết ca) | `BR_M05_19_NguoiVangMatIT` |
| BR-M05-20 | `loai_dien_bien.bien_phap_hoi_suc` | `BR_M05_20_DanhMucHoiSucIT` |
| BR-M05-21 | Provider `M05.SU_CO.choPhep` (S-11) | `BR_M05_21_HuySuCoKhanCapIT` |
| BR-M05-22 | `YeuCauDungNhacGoiDien` (S-11) | `BR_M05_22_HaMucDungGoiIT` |
| BR-M05-23 | `canhBao` của bean trạng thái (S-15) | `BR_M05_23_VungKhoanhKhongChanIT` |
| BR-M05-24 | Endpoint người thân cần liên hệ; `so_lien_he_vien` (S-15, S-16) | `BR_M05_24_HanhChinhTiepXucIT` |
| BR-M05-25 | Job một cấp mỗi lần (S-08, S-21) | `BR_M05_25_ChayBuIT` |
| BR-M05-26 | `SuCoDaHuyTrongBanTin` (S-11) | `BR_M05_26_SuCoHuyBanTinIT` |
| BR-M06-01 | S-07 | `BR_M06_01_NguongApDungIT`, `SoNguongTest` |
| BR-M06-02 | S-07 | `BR_M06_02_NhacThieuNguongIT` |
| BR-M06-03 | S-06, S-08 | `BR_M06_03_TaoCanhBaoIT` |
| BR-M06-04 | Mục 5 | `BR_M06_04_DoLaiIT`, `SC_001_XepMucTest` |
| BR-M06-05 | S-16 | `BR_M06_05_PhamViKhamIT` (SC-010) |
| BR-M06-06 | Mục 5; S-06 | `BR_M06_06_DinhChinhChiSoIT` |
| BR-M04-10 (phần 007) | S-06 | `BR_M04_10_DoChiSoCongViecIT` |
