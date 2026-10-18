# Data Model: Dịch vụ thông báo (009)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1. Bản ghi nguồn, người cao tuổi, tầng/khu vực, nhân viên, người thân, tài khoản là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Thời điểm lưu `DATETIME(6)` (000 R-04; DBR-25). Không có DBR trong Phụ lục 28 cho thông báo; các bất biến của spec (FR-005, FR-012, FR-032) được đưa xuống cơ sở dữ liệu ở mục 8.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `kenh_thong_bao` | 1 | G (FR-016, FR-039) |
| `yeu_cau_thong_bao`, `yeu_cau_phan_noi_dung`, `yeu_cau_nhom_nhan` | 3 | A (FR-001 → FR-005) |
| `thong_bao` | 3 | B, F (một người nhận một dòng) |
| `su_kien_thong_bao` | 3 | F (FR-044, trạng thái suy ra) |
| `thong_bao_trang_thai` | Kỹ thuật (dẫn xuất) | F (research B-09) |
| `lan_gui`, `ket_qua_lan_gui` | 3 | C (FR-018) |
| `khung_gop_tin_nhan` | Kỹ thuật | C (FR-016b) |
| `luot_nhac_goi`, `luot_nhac_goi_thong_bao`, `chuyen_nhac` | 3 | E (FR-027 → FR-030a) |
| `luot_nhac_goi_trang_thai` | Kỹ thuật (dẫn xuất) | E (FR-030, FR-032) |
| `ket_qua_goi` | 3 | E (FR-030, FR-031) |
| `lien_he_truc_tiep` | 2 | C (FR-021) |
| `ket_qua_lien_he` | 3 | C (FR-021, FR-031) |
| `thiet_bi_day` | Kỹ thuật | C (token FCM theo tài khoản, thiết bị; 002 cấp mã thiết bị) |
| `lan_chay_m13` | Kỹ thuật | Job (B-10) |

---

## 2. `kenh_thong_bao` – nhóm 1

`ma` (`TRONG_UNG_DUNG`, `CONG_NGUOI_THAN`, `TIN_NHAN`, `KHAC_*`; khóa chính), `ten`, `bat`, `muc_gan` `SET('NHE','TRUNG_BINH','KHAN_CAP')` (chỉ với kênh khác; mặc định Khẩn cấp), `cau_hinh` `JSON` (thông tin nhà cung cấp; không có bí mật, bí mật ở cấu hình triển khai), `phien_ban`. Đổi có nhật ký và lý do (FR-040).

---

## 3. Yêu cầu thông báo (mục A)

### `yeu_cau_thong_bao` – nhóm 3

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `module_nguon`, `loai_su_kien`, `nhom_nguon` | | Nhóm theo mục 17 | FR-001 |
| `loai_ban_ghi_nguon`, `id_ban_ghi_nguon`, `khoa_su_kien` | `VARCHAR(64)`, `VARCHAR(64)`, `VARCHAR(191)` | `UNIQUE (loai_ban_ghi_nguon, id_ban_ghi_nguon, khoa_su_kien)` | FR-005 |
| `muc` | `ENUM('NHE','TRUNG_BINH','KHAN_CAP')` | Thiếu thì `NHE` | FR-002, FR-003 |
| `nct_id`, `tang_khu_vuc_id` | `NULL` | | FR-001 |
| `noi_dung_rut_gon`, `tieu_de` | | Rút gọn thuộc `CHUNG` | FR-019, FR-034 |
| `nguoi_gay_ra_id`, `bao_nguoi_gay_ra` | | | FR-012 |
| `phat_sinh_luc`, `tiep_nhan_luc` | | | FR-001, FR-004 |
| `ket_qua_tiep_nhan` | `ENUM('HOP_LE','BI_TU_CHOI')` | Lý do khi bị từ chối | FR-003 |
| `dau` | `SET('THIEU_MUC','THIEU_LOAI_THONG_TIN','THIEU_RUT_GON','KHONG_CO_NGUOI_NHAN','VIEC_SAU_QUA_DOI','THIEU_DOI_TUONG_THONG_TIN','THIEU_MA_TRUONG_HOP')` | Đặt lúc tiếp nhận (một lần) | FR-001a, FR-003, FR-011, FR-013, FR-015 |
| `doi_tuong_thong_tin` | `ENUM('VE_NGUOI_CAO_TUOI','VE_VIEC_NGUOI_THAN') NULL` | Khai bởi module nguồn; rỗng khi yêu cầu không có nhóm người thân | FR-001a (bổ sung 2026-10-18) |
| `ma_truong_hop` | `ENUM('KHAN_CAP_NGUOI_LIEN_HE_CHINH','DANH_SACH_TIEP_XUC','XAC_NHAN_LAI_NGUYEN_VONG','CHUYEN_VIEN','QUA_DOI') NULL` | Mã Q-354 do module nguồn khai | FR-001a, FR-014a (bổ sung 2026-10-18) |
Lần nhận trùng (FR-005) không sửa dòng nhóm 3: mỗi lần ghi một dòng ở bảng kỹ thuật `yeu_cau_nhan_trung (yeu_cau_id, thoi_diem)`; view tra cứu đếm số dòng.

### `yeu_cau_phan_noi_dung` – nhóm 3

`yeu_cau_id`, `thu_tu`, `loai_thong_tin` (`CHUNG`, `SUC_KHOE`, `CHI_PHI`, `THAM_NOM`, `HOP_DONG`; thiếu → `SUC_KHOE`), `noi_dung`.

### `yeu_cau_nhom_nhan` – nhóm 3

`yeu_cau_id`, `thu_tu`, `nhom` (mã biến thể `NhomNhan`, research B-04), `tham_so` `JSON`, `cach_thay` (`MAC_DINH`, `KHONG_THAY`, `NGUOI_THAY_CHI_DINH`), `nhom_thay` `JSON NULL`, `ket_qua` (`CO_NGUOI`, `THAY`, `BO_QUA_KHONG_THAY`, `KHONG_CO`).

---

## 4. Thông báo (mục B, F)

### `thong_bao` – nhóm 3 (một dòng mỗi người nhận mỗi yêu cầu)

| Cột | Ghi chú | Nguồn |
| --- | --- | --- |
| `id`, `yeu_cau_id` | | |
| `loai_nguoi_nhan` (`NHAN_VIEN`, `NGUOI_THAN`, `NGUOI_CAO_TUOI` bổ sung 2026-10-18), `nguoi_nhan_id`, `tai_khoan_id NULL` | `UNIQUE (yeu_cau_id, loai_nguoi_nhan, nguoi_nhan_id)` | FR-012 |
| `can_cu` `JSON` | Mọi căn cứ: nhóm, "thay <nhóm> – không có người", "tạm nhận – bàn giao chưa xác nhận", "nhận thay – không có người nhận" | FR-009 → FR-012 |
| `phan_duoc_xem` `SET(...)` | Loại thông tin người nhận thấy lúc gửi (người thân) | FR-013 |
| `chi_nhan_dang` | Nhận thay không có phạm vi | FR-011 |
| `ma_truong_hop` `NULL` | Mã Q-354 áp dụng cho chính người nhận này: chỉ có khi người thân nằm ngoài phạm vi bản đồng ý lúc gửi; đây là "bản ghi gửi ghi trường hợp áp dụng" (BR-M10-06) | FR-014a (bổ sung 2026-10-18) |
| `muc` | Chép từ yêu cầu | |
| `tao_luc` | | |

Không `UPDATE`, không `DELETE` (mẫu 000).

### `su_kien_thong_bao` – nhóm 3

`thong_bao_id`, `loai` (`GIU`, `TAI_DINH_THOI`, `GUI`, `KHONG_GUI`, `XEM`, `XAC_NHAN`, `KHONG_LIEN_LAC_DUOC`, `AN`), `cach_xac_nhan` (`TRONG_UNG_DUNG`, `QUA_CUOC_GOI`, `XAC_NHAN_MUON`, `XAC_NHAN_MUON_QUA_CUOC_GOI`), `ly_do`, `gui_du_kien` (với `GIU`, `TAI_DINH_THOI`), `nguoi_thuc_hien_id NULL` (rỗng = Hệ thống), `thoi_diem`. `UNIQUE (thong_bao_id, loai)` với `loai IN ('XEM','XAC_NHAN','GUI','KHONG_GUI')` qua cột sinh (xem lần đầu, xác nhận một lần; FR-034).

### `thong_bao_trang_thai` – kỹ thuật (dẫn xuất)

`thong_bao_id` (khóa chính), `trang_thai` (`CHO_HET_GIO_YEN_TINH`, `KHONG_GUI`, `DA_GUI`, `GUI_THAT_BAI`, `DA_XEM`, `DA_XAC_NHAN`, `KHONG_LIEN_LAC_DUOC`), `gui_du_kien`, `gui_luc`, `xem_luc`, `xac_nhan_luc`, `cach_xac_nhan`, `an`, `nguoi_nhan_key` (để lọc hộp thư), `muc`, `nct_id`. Cập nhật cùng transaction với `su_kien_thong_bao`, `ket_qua_lan_gui`; chỉ mục `(nguoi_nhan_key, gui_luc)`, `(trang_thai, gui_du_kien)`, `(muc, trang_thai, gui_luc)`.

**Bảng chuyển** (khớp FR-044; trạng thái tính từ sự kiện):

| Từ | Sự kiện | Đến | Người | Điều kiện |
| --- | --- | --- | --- | --- |
| — | Xác định người nhận | `CHO_HET_GIO_YEN_TINH` | Hệ thống | Người thân (Nhẹ/Trung bình) hoặc người cao tuổi (Nhẹ), trong giờ yên tĩnh |
| — | Xác định người nhận | `DA_GUI` / `GUI_THAT_BAI` | Hệ thống | Còn lại |
| `CHO_HET_GIO_YEN_TINH` | Tới `gui_du_kien` | `DA_GUI` / `GUI_THAT_BAI` | Hệ thống | Còn đủ điều kiện |
| `CHO_HET_GIO_YEN_TINH` | Tới `gui_du_kien` | `KHONG_GUI` | Hệ thống | Không còn đủ điều kiện; người cao tuổi Qua đời |
| `DA_GUI` | Mở | `DA_XEM` | Người nhận | — |
| `DA_GUI`, `DA_XEM` (Khẩn cấp) | Xác nhận | `DA_XAC_NHAN` | Người nhận | Trực tuyến |
| `DA_GUI`, `DA_XEM`, `GUI_THAT_BAI` (Khẩn cấp) | Kết quả gọi "Đã liên lạc được" | `DA_XAC_NHAN` | Người gọi | — |
| `DA_GUI`, `DA_XEM`, `GUI_THAT_BAI` (Khẩn cấp) | Mọi lượt nhắc của thông báo "Không liên lạc được" | `KHONG_LIEN_LAC_DUOC` | Hệ thống | Không còn lượt Chờ |
| `KHONG_LIEN_LAC_DUOC` | Xác nhận / lần gọi bổ sung "Đã liên lạc được" | `DA_XAC_NHAN` | Người nhận / người gọi | Xác nhận muộn |

---

## 5. Gửi theo kênh (mục C)

### `lan_gui` – nhóm 3

`id`, `thong_bao_id`, `kenh`, `dia_chi` (số điện thoại, token; hiển thị che một phần), `lan_thu` (1 + số lần gửi lại), `du_kien_luc`. `UNIQUE (thong_bao_id, kenh, lan_thu)` (gửi lại không trùng, NFR-04).

### `ket_qua_lan_gui` – nhóm 3

`lan_gui_id`, `ket_qua` (`DA_GUI`, `THAT_BAI`, `KENH_CHUA_CAU_HINH`, `KHONG_CO_DIA_CHI`, `BO_QUA_NGOAI_CA`, `DA_GOP`, `NOI_DUNG_KHONG_HOP_LE`), `ly_do`, `tin_tong_hop_id NULL` (với `DA_GOP`), `thoi_diem`. `UNIQUE (lan_gui_id)`.

### `khung_gop_tin_nhan` – kỹ thuật

`nguoi_nhan_key`, `bat_dau`, `ket_thuc`, `da_dong_luc`, `tin_tong_hop_lan_gui_id NULL`. Cột sinh `khoa_mo = IF(da_dong_luc IS NULL, nguoi_nhan_key, NULL)` `UNIQUE` (một khung mở mỗi người).

### `thiet_bi_day` – kỹ thuật

`tai_khoan_id`, `ma_thiet_bi` (002), `ung_dung` (`APP_NHAN_VIEN`, `APP_NGUOI_THAN`, `APP_NGUOI_CAO_TUOI` bổ sung 2026-10-18), `fcm_token`, `cap_nhat_luc`, `vo_hieu_luc`. Token hết hạn (FCM trả lỗi) thì đặt `vo_hieu_luc`.

---

## 6. Lượt nhắc gọi (mục E)

### `luot_nhac_goi` – nhóm 3

`id`, `chuoi_goi_id` (bằng `id` của lượt đầu chuỗi), `loai_nguoi_can_goi` (`NHAN_VIEN`, `NGUOI_THAN`), `nguoi_can_goi_id`, `nct_id NULL`, `nguon_khi_khong_nct` (loại, id) `NULL`, `tang_khu_vuc_id NULL`, `tao_luc`, `ly_do_tao` (`QUA_HAN_XAC_NHAN`, `GUI_THAT_BAI`, `KHONG_LIEN_LAC_NGUOI_TRUOC`).

### `luot_nhac_goi_thong_bao` – nhóm 3

`luot_id`, `thong_bao_id` `UNIQUE` (mỗi thông báo tối đa một lượt tự động, FR-032), `gop_luc`.

### `chuyen_nhac` – nhóm 3

`luot_id`, `nguoi_duoc_nhac_truoc_id NULL`, `nguoi_duoc_nhac_id`, `ly_do` (`KHOI_TAO`, `QUA_HAN`, `HET_CA`, `GOP_CAP_NHAT`, `NHAC_LAI_QLV`), `han_luc` (`thoi_diem + CFG-M13-04`), `thoi_diem`.

### `luot_nhac_goi_trang_thai` – kỹ thuật (dẫn xuất)

`luot_id` (khóa chính), `trang_thai` (`CHO_KET_QUA`, `DA_CO_KET_QUA`, `DA_DUNG`), `ket_qua_cuoi`, `ly_do_dung`, `han_luc`, `nguoi_duoc_nhac_hien_tai_id`, `khoa_cho` (cột sinh, research B-08) `UNIQUE` — tối đa một lượt Chờ mỗi (người cần gọi, người cao tuổi) (FR-032, Q-100).

**Bảng chuyển** (khớp FR-030):

| Từ | Sự kiện | Đến | Người |
| --- | --- | --- | --- |
| — | Gửi lượt nhắc | `CHO_KET_QUA` | Hệ thống |
| `CHO_KET_QUA` | Kết quả "Đã liên lạc được" | `DA_CO_KET_QUA` | Người có quyền gọi |
| `CHO_KET_QUA` | Kết quả "Không liên lạc được" | `DA_CO_KET_QUA` (không liên lạc được) | Người có quyền gọi |
| `CHO_KET_QUA` | Gộp thêm thông báo | `CHO_KET_QUA` | Hệ thống |
| `CHO_KET_QUA` | Người nhận tự xác nhận mọi thông báo của lượt | `DA_DUNG` | Hệ thống |
| `CHO_KET_QUA` | Người cần gọi không còn đủ điều kiện | `DA_DUNG` | Hệ thống |
| `CHO_KET_QUA` | Quá hạn CFG-M13-04; người được nhắc hết ca | `CHO_KET_QUA` | Hệ thống |
| `CHO_KET_QUA` | Sự cố hạ mức khỏi Khẩn cấp; đã có kết quả xác nhận lại (nguy kịch) | `DA_DUNG` | Hệ thống |

### `ket_qua_goi` – nhóm 3

`id`, `luot_id`, `nguoi_goi_id`, `goi_luc`, `so_da_goi`, `ket_qua` (`DA_LIEN_LAC`, `KHONG_LIEN_LAC`), `ly_do` (`KHONG_NGHE_MAY`, `SAI_SO`, `KHONG_CO_SO`, khác), `nguoi_nghe_may`, `ghi_chu`, `bo_sung` (lần gọi bổ sung, FR-029a), `tao_luc`. Đính chính qua 000 (loại `M13.KET_QUA_GOI`).

---

## 7. Liên hệ trực tiếp (FR-021)

### `lien_he_truc_tiep` – nhóm 2

`id`, `thong_bao_id` `UNIQUE`, `nguoi_than_id NULL`, `ho_ten`, `quan_he`, `so_dien_thoai`, `noi_dung_rut_gon`, `trang_thai` (`CHO_LIEN_HE`, `DA_LIEN_HE`, `KHONG_LIEN_HE_DUOC`), `da_nhac_luc`, `phien_ban`. Trigger chặn mọi đổi khi trạng thái cuối.

### `ket_qua_lien_he` – nhóm 3

`lien_he_id`, `nguoi_lien_he_id` (Hành chính), `thoi_diem`, `ket_qua`, `ly_do` (bắt buộc khi không liên hệ được), `ghi_chu`.

---

## 8. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bất biến | Cách bảo đảm | Nguồn |
| --- | --- | --- |
| Yêu cầu duy nhất theo (bản ghi nguồn, khóa sự kiện) | `UNIQUE` trên `yeu_cau_thong_bao` | FR-005, NFR-04 |
| Mỗi người nhận tối đa một thông báo mỗi yêu cầu | `UNIQUE (yeu_cau_id, loai_nguoi_nhan, nguoi_nhan_id)` | FR-012 |
| Mỗi thông báo thuộc tối đa một lượt nhắc tự động | `UNIQUE (thong_bao_id)` trên `luot_nhac_goi_thong_bao` | FR-032 |
| Tối đa một lượt Chờ mỗi (người cần gọi, người cao tuổi) | `UNIQUE khoa_cho` | FR-032, Q-100 |
| Xem lần đầu, xác nhận một lần | `UNIQUE` cột sinh trên `su_kien_thong_bao` | FR-034 |
| Thông báo, lần gửi, xác nhận, kết quả gọi không sửa, không xóa | Nhóm 3 theo mẫu 000 (tài khoản `eldercare_app` không có `UPDATE`, `DELETE`) | FR-036 |
| Gửi lại không trùng | `UNIQUE (thong_bao_id, kenh, lan_thu)` | FR-018, NFR-04 |
| Một khung gộp mở mỗi người | `UNIQUE khoa_mo` | FR-016b |
| Thời điểm theo đồng hồ máy chủ | `Clock`; thời điểm thiết bị không dùng | NFR-09, DBR-25 |

---

## 9. Nhật ký và phân loại trường

- Nhật ký (`NhatKyApi`) cho lệnh người dùng: xác nhận Khẩn cấp, kết quả gọi, liên hệ trực tiếp, cấu hình kênh (lý do). Hoạt động của hệ thống được ghi ở `su_kien_thong_bao`, `ket_qua_lan_gui`, `chuyen_nhac` (người thực hiện Hệ thống, sự kiện kích hoạt) (FR-040; research B-13).
- `PhanLoaiTruongProvider`: `yeu_cau_phan_noi_dung.noi_dung` theo `loai_thong_tin`; số điện thoại → `LIEN_HE`.

---

## 10. Lưu giữ (000 R-11)

| Loại dữ liệu | Hạn | Chuỗi tham chiếu |
| --- | --- | --- |
| Yêu cầu, thông báo, sự kiện, lần gửi, lượt nhắc, kết quả gọi, liên hệ trực tiếp | `DAI_HON(CFG-M15-04)` và không ngắn hơn bản ghi nguồn (yêu cầu gắn người cao tuổi theo CFG-M15-03 của hồ sơ) | Bản ghi nguồn (`loai_ban_ghi_nguon`, `id`) |
| Bảng kỹ thuật (trạng thái dẫn xuất, khung gộp, thiết bị) | Theo dòng gốc; thiết bị xóa khi token vô hiệu quá 90 ngày (kỹ thuật) | — |

---

## 11. Truy vết BR và FR

| Mã | Bảo đảm ở | Test |
| --- | --- | --- |
| BR-M13-01 | Research B-05 (`ChonKenh`, gộp tin nhắn, ngoài ca) | `BR_M13_01_ChonKenhTest`, `FR_016a_NgoaiCaIT`, `FR_016b_GopTinNhanIT` |
| BR-M13-02 | Research B-07, B-08 | `BR_M13_02_LuotNhacGoiIT`, `FR_029a_ChuoiGoiIT`, `FR_030a_ChuyenNhacIT`, `FR_027a_GopLuotIT` |
| BR-M13-03 | Research B-06 | `BR_M13_03_GioYenTinhIT`, `GioYenTinhTest` |
| BR-M13-04 | Research B-04 | `BR_M13_04_NguoiNhanTheoPhanCongIT`, `SC_004_NguoiNhanTest` (200 yêu cầu) |
| BR-M13-05 | Mục 3 → 6; B-11 | `BR_M13_05_LichSuGuiIT` |
| BR-M10-06 (phần 009) | `XEM` gửi lại module nguồn qua `ThongBaoTraCuuApi`; `thong_bao.ma_truong_hop` (bổ sung 2026-10-18) | `FR_035_DuLieuChoNguonIT`, `FR_014a_MaTruongHopIT` |
| FR-001a, FR-014 (Q-353, sửa 2026-10-18) | `XetPhamViDongY`; `doi_tuong_thong_tin`; dấu `THIEU_DOI_TUONG_THONG_TIN`, `THIEU_MA_TRUONG_HOP` | `XetPhamViDongYTest`, `FR_014_NhanThongBaoDongYIT` |
| DBR-03 (phần 009) | `LocPhanNoiDung` | `FR_013_LocNguoiThanTest` |
| FR-007 (f), FR-022a *(bổ sung 2026-10-18)* | B-02, B-04, B-06 | `FR_022a_NguoiCaoTuoiNhanIT` |
| FR-005, FR-012, FR-032 | Mục 8 | `FR_005_ChongTrungIT`, `FR_012_GopNguoiNhanIT`, `FR_032_MotLuotIT` |
