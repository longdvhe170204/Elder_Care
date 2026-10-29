# Data Model: Chăm sóc hằng ngày (005)

**Ngày**: 2026-10-09, cập nhật 2026-10-18 (Q-347, Q-349, Q-350) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Tài liệu mô tả bảng, cột, ràng buộc và cách bảo đảm DBR. Đây không phải script SQL; migration viết ở giai đoạn triển khai. Quy ước kiểu, tên, cột chuẩn của nhóm 1, 2, 3 và mẫu trigger theo [data-model của 000](../000-shared-foundation/data-model.md) mục 1; không nhắc lại ở từng bảng. Người cao tuổi, nhân viên, ca, tầng/khu vực, dịch vụ, bữa, buổi, lần đo, sự cố, cảnh báo, bản ghi chỉ số, bản ghi đón là tham chiếu logic (`BIGINT`), không có khóa ngoại sang module khác. Thời điểm lưu `DATETIME(6)` theo Asia/Ho_Chi_Minh (DBR-25).

---

## 0. Gộp bảng theo R-15 *(bổ sung 2026-10-29, bản 2)*

Gộp kỹ thuật theo [research R-15 của 000](../000-shared-foundation/research.md) (bản 2, chủ dự án duyệt 2026-10-29, constitution XII 3.2.0); không đổi nghiệp vụ, API, quyền. Mô tả bảng ở các mục sau vẫn là căn cứ cột và ràng buộc: bảng gộp mang đủ cột của bảng cũ (cột riêng vào `JSON` hoặc cột `NULL` có `CHECK` theo `loai`); ràng buộc duy nhất thành `khoa_duy_nhat`; dòng nhóm 3 hoặc đã khóa có `khoa_luc` và trigger theo dòng. Bảy bảng chung ở [data-model 000 mục 8](../000-shared-foundation/data-model.md): `danh_muc_chung`, `lich_su_chung`, `su_kien_chung`, `ho_so_chung`, `trang_thai_nct`, `moc_da_nhac`, `khoa_ky_thuat`. Khi cài thấy bảng nào không thể gộp mà vẫn giữ được BR, DBR thì giữ và ghi lý do ở plan.

- Giữ, gộp: `ke_hoach_cham_soc`; `muc_ke_hoach` (+ `thoa_thuan_rui_ro`, cột `loai`); `cong_viec` (+ `ket_qua_ghi_nhan`, khóa theo dòng khi hoàn thành).
- `danh_muc_chung`: `loai_cong_viec`, `danh_muc_gia_tri_ket_qua`, `loai_ly_do_khong_thuc_hien`.
- `lich_su_chung`: `lich_su_xu_ly_cong_viec`; `su_kien_chung`: `diem_danh_ban_tru`.
- `ho_so_chung`: `co_mat_ban_tru`; `trang_thai_nct`: `cham_soc_nct`.
- Còn 3 bảng riêng của feature.

---

## 1. Tổng quan

| Bảng | Nhóm (1.5) | Mục spec |
| --- | --- | --- |
| `cham_soc_nct` | 2 (kỹ thuật) | Đích khóa theo người (research C-01) *(R-15: `cham_soc_nct` → `trang_thai_nct`)* |
| `ke_hoach_cham_soc` | 2, theo phiên bản | A |
| `muc_ke_hoach` | 2, sửa được chỉ khi phiên bản Nháp | A |
| `thoa_thuan_rui_ro`, `thoa_thuan_nguoi_tham_gia`, `thoa_thuan_y_kien` | 2, sửa được chỉ khi phiên bản Nháp | A (FR-002a; bổ sung 2026-10-18) *(R-15: `thoa_thuan_rui_ro` → `muc_ke_hoach`)* |
| `loai_ly_do_khong_thuc_hien` | 1 | C, D (FR-028a; bổ sung 2026-10-18) *(R-15: `loai_ly_do_khong_thuc_hien` → `danh_muc_chung`)* |
| `loai_cong_viec`, `loai_cong_viec_chung_chi`, `danh_muc_gia_tri_ket_qua` | 1 | E, D (FR-034) *(R-15: `loai_cong_viec`, `danh_muc_gia_tri_ket_qua` → `danh_muc_chung`)* |
| `co_mat_ban_tru` | 2 | B *(R-15: `co_mat_ban_tru` → `ho_so_chung`)* |
| `diem_danh_ban_tru` | 3 | B *(R-15: `diem_danh_ban_tru` → `su_kien_chung`)* |
| `cong_viec` | 2 | C |
| `ket_qua_ghi_nhan` | 3 | D *(R-15: `ket_qua_ghi_nhan` → `cong_viec`)* |
| `lich_su_xu_ly_cong_viec` | 3 | C, D, G *(R-15: `lich_su_xu_ly_cong_viec` → `lich_su_chung`)* |
| `lan_chay_m04` | Kỹ thuật | K |

---

## 2. `cham_soc_nct` – nhóm 2 (kỹ thuật) → gộp theo R-15: `cham_soc_nct` → `trang_thai_nct`

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| `nct_id` | `BIGINT` | Khóa chính; tạo khi lập phiên bản đầu tiên hoặc công việc đầu tiên của người đó |
| `phien_ban` | `INT` | |

Không mang dữ liệu nghiệp vụ; chỉ để `SELECT … FOR UPDATE` theo thứ tự khóa ở research C-03.

---

## 3. Kế hoạch chăm sóc (mục A)

### `ke_hoach_cham_soc` – nhóm 2

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | | |
| `nct_id` | `BIGINT` | | FR-001 |
| `so_phien_ban` | `INT` | `UNIQUE (nct_id, so_phien_ban)`; tăng dần theo người | FR-001 |
| `trang_thai` | `ENUM('NHAP','CHO_DUYET','CHO_HIEU_LUC','HIEU_LUC','HET_HIEU_LUC','DA_HUY')` | | FR-003 |
| `ngay_hieu_luc` | `DATE NULL` | Đề xuất khi gửi duyệt; bắt buộc từ `CHO_DUYET` | FR-003, Q-34 |
| `hieu_luc_den` | `DATE NULL` | Đặt khi Hết hiệu lực | FR-003, DBR-11 |
| `phien_ban_truoc_id` | `BIGINT NULL` | Phiên bản Hiệu lực lúc lập (nguồn sao chép) | FR-003, FR-008 |
| `ly_do_thay_doi` | `VARCHAR(2000) NULL` | Bắt buộc khi gửi duyệt nếu có phiên bản trước | FR-001, FR-003 |
| `nguoi_lap_id`, `lap_luc` | | | FR-001 |
| `nguoi_duyet_id`, `duyet_luc`, `tu_duyet` | `BIGINT NULL`, `DATETIME(6) NULL`, `BOOLEAN` | | FR-005, Q-10 |
| `gui_duyet_luc`, `da_nhac_luc`, `da_bao_qlv_luc` | `DATETIME(6) NULL` | Mốc cho `NhacQuaHan` của 000 | FR-005 |
| `ly_do_ket_thuc` | `ENUM('PHIEN_BAN_MOI','TRANG_THAI_CUOI','TRA_LAI','HUY','THU_HOI') NULL` | | FR-003 |
| `mo_nct` | Cột sinh `IF(trang_thai IN ('NHAP','CHO_DUYET','CHO_HIEU_LUC'), nct_id, NULL)` | `UNIQUE` | FR-004 |
| `hieu_luc_nct` | Cột sinh `IF(trang_thai='HIEU_LUC', nct_id, NULL)` | `UNIQUE` | DBR-11 |
| `phien_ban` | `INT` | Khóa lạc quan cho lệnh cập nhật mục ở Nháp | |

Chỉ mục `(trang_thai, ngay_hieu_luc)` cho job hằng ngày; `(nct_id, ngay_hieu_luc)` để tra phiên bản Hiệu lực tại ngày D: dùng `ngay_hieu_luc <= D AND (hieu_luc_den IS NULL OR hieu_luc_den >= D) AND trang_thai IN ('HIEU_LUC','HET_HIEU_LUC')`.

**Bảng chuyển** (khớp bảng FR-003, cài ở `BangChuyenKeHoach`):

| Từ | Lệnh / sự kiện | Đến | Endpoint / nơi gọi |
| --- | --- | --- | --- |
| — | Lập phiên bản | `NHAP` | `POST /nguoi-cao-tuoi/{nctId}/ke-hoach-cham-soc` |
| `NHAP` | Cập nhật mục | `NHAP` | `PUT /ke-hoach-cham-soc/{id}/muc` (`If-Match`) |
| `NHAP` | Gửi duyệt | `CHO_DUYET` | `POST …/{id}/gui-duyet` |
| `CHO_DUYET` | Duyệt | `CHO_HIEU_LUC` | `POST …/{id}/duyet` |
| `CHO_DUYET` | Trả lại | `NHAP` | `POST …/{id}/tra-lai` |
| `NHAP`, `CHO_DUYET` | Hủy phiên bản | `DA_HUY` | `POST …/{id}/huy` |
| `CHO_HIEU_LUC` | Thu hồi duyệt | `DA_HUY` | `POST …/{id}/thu-hoi` |
| `CHO_HIEU_LUC` | Tới ngày hiệu lực | `HIEU_LUC` | Job `ViecHangNgayM04` |
| `HIEU_LUC` | Phiên bản mới có hiệu lực | `HET_HIEU_LUC` | Cùng transaction của dòng trên |
| `HIEU_LUC` | Trạng thái cuối | `HET_HIEU_LUC` | Bean `ThamGiaChuyenTrangThai` |
| `NHAP`, `CHO_DUYET`, `CHO_HIEU_LUC` | Trạng thái cuối | `DA_HUY` | Bean `ThamGiaChuyenTrangThai` |

### `muc_ke_hoach` – nhóm 2 (sửa chỉ khi phiên bản cha `NHAP`)

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `ke_hoach_id` | `BIGINT` | | |
| `muc_goc_id` | `BIGINT NULL` | Id mục ở phiên bản trước khi được sao chép; dùng cho so sánh | FR-008 |
| `loai_cong_viec_id` | `BIGINT` FK `ON DELETE RESTRICT` | Chỉ chọn loại `HIEU_LUC` | FR-002, FR-038 |
| `noi_dung` | `VARCHAR(1000)` | | FR-002 |
| `kieu_tan_suat` | `ENUM('SO_LAN_NGAY','MOI_N_GIO','NGAY_TRONG_TUAN','MOT_LAN_MOI_CA')` | | FR-002, BR-M04-25 |
| `n_gio`, `so_lan` | `SMALLINT NULL` | | |
| `moc_bat_dau` | `TIME NULL` | `NULL` = "trong ca" | FR-002 |
| `cac_moc` | `JSON NULL` | Danh sách giờ khi "số lần/ngày" có mốc | |
| `ngay_trong_tuan` | `TINYINT NULL` | Bit thứ 2 → CN | FR-002 (CHK011) |
| `khung_truoc`, `khung_sau` | `SMALLINT` (phút) | Điền lúc lập mục từ loại hoặc CFG-M04-02; cố định khi gửi duyệt | FR-002 |
| `vai_tro_thuc_hien` | `ENUM` vai trò AC | | FR-002 |
| `loai_ket_qua` | Mã theo FR-034 | | FR-002 |
| `muc_quan_trong` | `ENUM('THUONG','QUAN_TRONG','BAT_BUOC')` | | FR-002 |
| `co_tinh_phi` | `BOOLEAN` | | FR-002, FR-051 |
| `muc_tieu_ml` | `INT NULL` | Chỉ với nhóm "hỗ trợ uống nước" | FR-039, Q-30 |

`CHECK`: `kieu_tan_suat='MOI_N_GIO'` thì `n_gio BETWEEN 1 AND 23` và `moc_bat_dau IS NOT NULL`; `moc_bat_dau IS NULL` thì khung không dùng (khung bằng cả ca).

### `thoa_thuan_rui_ro` – nhóm 2 (sửa chỉ khi phiên bản cha `NHAP`) *(bổ sung 2026-10-18, FR-002a, Q-349; research C-18)* → gộp theo R-15: `thoa_thuan_rui_ro` → `muc_ke_hoach`

"Mục loại thỏa thuận lựa chọn có rủi ro" của spec được lưu ở bảng riêng, không ở `muc_ke_hoach`: mục này không có tần suất, mốc giờ, vai trò, nên không bao giờ là căn cứ lập công việc (`loai_can_cu` không có giá trị trỏ tới bảng này).

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `ke_hoach_id` | `BIGINT` | | FR-002a |
| `thoa_thuan_goc_id` | `BIGINT NULL` | Id ở phiên bản trước khi được sao chép | FR-008 |
| `lua_chon` | `VARCHAR(2000)` | `NOT NULL` | FR-002a |
| `rui_ro_da_trao_doi` | `VARCHAR(2000)` | `NOT NULL` | FR-002a |
| `nct_tham_gia` | `BOOLEAN` | `CHECK (nct_tham_gia OR NOT tu_quyet_duoc)` | FR-002a |
| `bac_si_nhan_dinh_id` | `BIGINT` | Có vai trò AC-03 lúc ghi | FR-002a, 001 FR-023h |
| `tu_quyet_duoc` | `BOOLEAN` | Nhận định cho chính lựa chọn này | 001 FR-023h |
| `thoi_diem_nhan_dinh` | `DATETIME(6)` | | 001 FR-023h |
| `cach_ho_tro_hieu` | `VARCHAR(2000)` | `NOT NULL`, không rỗng | 001 FR-023h |
| `ngay_xem_lai_nhan_dinh` | `DATE` | `NOT NULL` | 001 FR-023h |
| `nguoi_quyet_dinh_loai` | `ENUM('NGUOI_CAO_TUOI','DAI_DIEN_NGUOI_BENH','BAC_SI')` | `CHECK (tu_quyet_duoc = (nguoi_quyet_dinh_loai = 'NGUOI_CAO_TUOI'))` | FR-002a, BR-M01-15 tầng 2 |
| `nguoi_quyet_dinh_nguoi_than_id` | `BIGINT NULL` | Bắt buộc khi `DAI_DIEN_NGUOI_BENH`, rỗng khi khác (`CHECK`); người thân có quan hệ Hiệu lực | FR-002a |
| `nguoi_quyet_dinh_bac_si_id` | `BIGINT NULL` | Bắt buộc khi `BAC_SI`, rỗng khi khác (`CHECK`); có vai trò AC-03 | FR-002a |
| `ly_do_bac_si_quyet_dinh` | `VARCHAR(1000) NULL` | Bắt buộc khi `BAC_SI` (`CHECK`): ghi căn cứ không có văn bản nguyện vọng trước và không có người đại diện của người bệnh (BR-M01-15 tầng 2; bổ sung 2026-10-18) | FR-002a |
| `ngay_xem_lai` | `DATE` | `NOT NULL`; ngày xem lại thỏa thuận | FR-002a |
| `nguoi_ghi_id`, `ghi_luc` | | | |

`thoa_thuan_nguoi_tham_gia (thoa_thuan_id, loai ENUM('NGUOI_THAN','NHAN_VIEN'), doi_tuong_id)`: khóa chính ba cột; người cao tuổi tham gia ghi ở `nct_tham_gia`.

`thoa_thuan_y_kien`: `id`, `thoa_thuan_id`, cột nguồn theo mẫu data-model 001 mục 6a (`nguon_loai`, `nguon_nguoi_than_id`, `nguoi_ghi_id`, `thoi_diem`), `noi_dung VARCHAR(2000) NOT NULL`. Mỗi ý kiến một dòng (BR-M01-14).

Cả ba bảng chịu cùng trigger khóa như `muc_ke_hoach` (`trg_thoa_thuan_*_khoa`: chặn `INSERT/UPDATE/DELETE` khi phiên bản cha không ở `NHAP`), nên nội dung đã duyệt không đổi; ý kiến đổi thì ghi ở phiên bản mới, phiên bản cũ vẫn tra được (FR-023g của 001).

---

## 4. Danh mục (mục E)

### `loai_cong_viec` – nhóm 1 → gộp theo R-15: `loai_cong_viec` → `danh_muc_chung`

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `ten`, `trang_thai`, `ten_hieu_luc`, `phien_ban` | Mẫu 000 mục 1.1 | | FR-038 |
| `nhom` | `ENUM('VE_SINH_CA_NHAN','TAM','THAY_QUAN_AO','HO_TRO_AN','HO_TRO_UONG_NUOC','HO_TRO_DI_VE_SINH','VAN_DONG','PHUC_HOI','DO_CHI_SO','HOAT_DONG','NGU','TAM_TRANG','HANH_VI','THAY_TA_VAT_PHAM','KIEM_TRA_DAU_CA','GIAM_SAT','UONG_THUOC','KHAC')` | `UONG_THUOC` không chọn được cho mục kế hoạch hay công việc | FR-038, 8.3 |
| `chuyen_mon` | `BOOLEAN` | Nhóm chuyên môn (kiểm tra đầu ca, giám sát…) | FR-029 |
| `loai_ket_qua` | `ENUM('AN_UONG','ML','GIA_TRI_DO','VAN_DONG','NGU','TAM_TRANG','HANH_VI','SO_LUONG_VAT_PHAM','DA_THUC_HIEN','KHONG_GHI')` | | FR-034 |
| `vai_tro_mac_dinh` | `ENUM` vai trò | | FR-038 |
| `muc_mac_dinh` | `ENUM('THUONG','QUAN_TRONG','BAT_BUOC')` | | FR-038 |
| `khung_truoc`, `khung_sau` | `SMALLINT NULL` | `NULL` dùng CFG-M04-02 | FR-038 |
| `co_tinh_phi`, `dich_vu_id` | `BOOLEAN`, `BIGINT NULL` | Dịch vụ của 004 | FR-038, FR-051 |
| `gioi_han_tren` | `INT NULL` | Ví dụ ml mỗi lần uống | FR-034 |
| `loai_chi_so` | `VARCHAR NULL` | Với `DO_CHI_SO`: mã chỉ số của 007 | FR-041 |

`loai_cong_viec_chung_chi (loai_cong_viec_id, ma_chung_chi)`: khóa chính hai cột. Đọc trực tiếp mỗi lần giao, nhận, phân lại (BR-M09-01).

### `danh_muc_gia_tri_ket_qua` – nhóm 1 → gộp theo R-15: `danh_muc_gia_tri_ket_qua` → `danh_muc_chung`

`loai` (`NGU_CHAT_LUONG`, `TAM_TRANG`, `HANH_VI`), `ma`, `ten`, `nhom_quy_tac` (`MAT_NGU`, `TIEU_CUC`, `NULL`), cột chuẩn nhóm 1. Dùng cho FR-034, BR-M04-11, 007 FR-039c.

### `loai_ly_do_khong_thuc_hien` – nhóm 1 *(bổ sung 2026-10-18, FR-028a, Q-350)* → gộp theo R-15: `loai_ly_do_khong_thuc_hien` → `danh_muc_chung`

Cột chuẩn nhóm 1 (mẫu 000 mục 1.1) và `co_dinh BOOLEAN`. Migration nạp một dòng `co_dinh = true`, tên "Người cao tuổi từ chối"; `CHECK (NOT co_dinh OR trang_thai = 'HIEU_LUC')` và trigger chặn đổi `ten`, `co_dinh` của dòng cố định, nên dòng này không bị ngừng hay đổi nghĩa. Quản lý viện tạo, sửa, ngừng các loại khác (quyền như danh mục loại công việc). Tên loại mô tả việc đã xảy ra; đây là quy ước khi cấu hình, không kiểm bằng mã.

---

## 5. Có mặt bán trú (mục B)

### `co_mat_ban_tru` – nhóm 2 → gộp theo R-15: `co_mat_ban_tru` → `ho_so_chung`

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `nct_id`, `ngay` | | `UNIQUE (nct_id, ngay)` | FR-012, CHK036 |
| `gio_den_du_kien`, `gio_ve_du_kien` | `TIME NULL` | Theo lịch của 004; `NULL` với buổi phát sinh | FR-012 |
| `gio_ve_du_kien_hien_hanh` | `TIME NULL` | Sau khi 014 dời; mặc định bằng `gio_ve_du_kien` | FR-018a |
| `dang_ky_ve_muon_id` | `BIGINT NULL` | Đăng ký của 014 làm dời giờ về | FR-018a |
| `trang_thai` | `ENUM('CHUA_DEN','CO_MAT','DA_VE','VANG_CO_BAO','VANG_KHONG_BAO','DA_HUY_NGAY_NGHI')` | | FR-013 |
| `buoi_phat_sinh` | `BOOLEAN` | | FR-013, Q-140 |
| `diem_danh_den_id`, `diem_danh_ve_id` | `BIGINT NULL` | Trỏ `diem_danh_ban_tru` | FR-013 *(R-15: `diem_danh_ban_tru` → `su_kien_chung`)* |
| `bao_vang_id`, `bao_vang_muon_id` | `BIGINT NULL` | Bản ghi báo vắng của 004 | FR-013, FR-014 |
| `da_bao_het_ngay_luc` | `DATETIME(6) NULL` | Một lần báo FR-019 | FR-019 |
| `phien_ban` | `INT` | | |

Chỉ mục `(trang_thai, ngay, gio_den_du_kien)` cho chuyển Vắng không báo; `(ngay, trang_thai)` cho FR-019.

**Bảng chuyển** (khớp bảng FR-013):

| Từ | Lệnh / sự kiện | Đến | Nơi thực hiện |
| --- | --- | --- | --- |
| — | Ngày có lịch đến (job 00:00 hoặc lịch đổi trong ngày) | `CHUA_DEN` hoặc `VANG_CO_BAO` | `ViecHangNgayM04`, listener 004 |
| — | Điểm danh đến ngày không có lịch, ngày khu nghỉ | `CO_MAT` (`buoi_phat_sinh`) | `POST /co-mat-ban-tru/diem-danh-den` |
| `CHUA_DEN`, `VANG_KHONG_BAO` | Khai báo ngày khu nghỉ | `DA_HUY_NGAY_NGHI` | Listener `NgayNghiBanTruDaDoi` |
| — hoặc `CHUA_DEN` | Báo vắng đủ hạn (004) | `VANG_CO_BAO` | Listener `BaoVangBanTruDaGhi(duHan = true)` |
| `CHUA_DEN` | Quá giờ đến + CFG-M02-07 | `VANG_KHONG_BAO` | `ViecTheoPhutM04` |
| `CHUA_DEN`, `VANG_KHONG_BAO` | Điểm danh đến | `CO_MAT` | `POST /co-mat-ban-tru/diem-danh-den` |
| `CO_MAT` | Điểm danh về | `DA_VE` | `POST /co-mat-ban-tru/{id}/diem-danh-ve`; `CoMatBanTruApi.diemDanhVe` (012, Viện đưa về) |

`DA_HUY_NGAY_NGHI` là cách lưu "không còn trạng thái có mặt cho buổi đó" (bảng FR-013 dòng 3): 010 không tính phí, 004 `TrangThaiCoMatBanTruPort` trả rỗng.

### `diem_danh_ban_tru` – nhóm 3 → gộp theo R-15: `diem_danh_ban_tru` → `su_kien_chung`

`id`, `co_mat_id`, `loai` (`DEN`, `VE`), `thoi_diem_thuc_te`, `loai_ve` (`QUA_DON`, `TU_VE`, `VIEN_DUA_VE`; chỉ với `VE`), `ban_ghi_don_id`, `nguoi_thuc_hien_id`, `thoi_diem`, `tang_khu_vuc_id` (khu bán trú). Đính chính loại `M04.DIEM_DANH_BAN_TRU` (FR-015).

---

## 6. Công việc (mục C)

### `cong_viec` – nhóm 2

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id` | `BIGINT` | | |
| `nct_id` | `BIGINT NULL` | `NULL` với kiểm tra đầu ca, giám sát | FR-023, FR-029 |
| `vi_tri_loai`, `vi_tri_id` | `ENUM('PHONG','GIUONG','TANG','KHU_VUC') NULL`, `BIGINT NULL` | Với công việc gắn vị trí | FR-030 (CHK042) |
| `loai_cong_viec_id` | `BIGINT` FK | | FR-023 |
| `loai_can_cu` | `ENUM('MUC_KE_HOACH','LAN_DO','BUA','BUOI_HOAT_DONG','THEO_DOI_SAU_NGA','THEO_DOI_TIEP_XUC','CANH_BAO_UONG_NUOC','KIEM_TRA_DAU_CA','GIAM_SAT','PHAT_SINH','GHI_NHAN_PHAT_SINH','LAM_LAI')` | | FR-017, FR-023, FR-025, FR-026a |
| `can_cu_id` | `BIGINT NULL` | Id mục kế hoạch, lần đo, bữa, buổi, sự cố, danh sách tiếp xúc, cảnh báo, kết quả kiểm tra | FR-021 |
| `ke_hoach_id` | `BIGINT NULL` | Phiên bản của mục, cho FR-006 | FR-006 |
| `cong_viec_goc_id` | `BIGINT NULL` | Công việc làm lại, hoặc Ghi nhận phát sinh trỏ công việc đã bị đóng vì trạng thái cuối | FR-026a, FR-035 (CHK056) |
| `noi_dung` | `VARCHAR(1000)` | | |
| `ca_id`, `tang_khu_vuc_id` | `BIGINT` | Ca chứa thời điểm dự kiến | FR-023, BR-M04-25 |
| `thoi_diem_du_kien` | `DATETIME(6)` | | FR-023 |
| `dau_khung`, `cuoi_khung` | `DATETIME(6) NULL` | `NULL` với Ghi nhận phát sinh | FR-023, FR-026 |
| `muc_quan_trong` | `ENUM('THUONG','QUAN_TRONG','BAT_BUOC')` | | FR-023 |
| `vai_tro_thuc_hien` | `ENUM` vai trò | | FR-023 |
| `nguoi_thuc_hien_id` | `BIGINT NULL` | `NULL` = công việc chung của tầng/khu | FR-023, FR-024 |
| `dau_ton` | `ENUM('TON_TU_CA_TRUOC','TON_CHO_XAC_NHAN') NULL` | | FR-047, FR-047a |
| `co_tinh_phi`, `dich_vu_id` | `BOOLEAN`, `BIGINT NULL` | Chép từ mục hoặc loại lúc lập | FR-051 |
| `trang_thai` | `ENUM('CHUA_DEN_HAN','DEN_HAN','QUA_HAN','HOAN_THANH','HOAN_THANH_TRE','KHONG_THUC_HIEN','HUY')` | | FR-026 |
| `den_han_luc`, `qua_han_luc`, `dong_luc` | `DATETIME(6) NULL` | Thời điểm chuyển thật (có thể là lúc chạy bù) | FR-052 |
| `ly_do` | `VARCHAR(2000) NULL` | Khi Hủy, Hoàn thành trễ; với Không thực hiện do nhân viên là ghi chú, không bắt buộc (FR-028a) | FR-023, FR-028a |
| `loai_ly_do_id` | `BIGINT NULL` FK `loai_ly_do_khong_thuc_hien` | `CHECK (trang_thai <> 'KHONG_THUC_HIEN' OR dong_boi_loai <> 'NHAN_VIEN' OR loai_ly_do_id IS NOT NULL)`; Không thực hiện do hệ thống đóng giữ `quy_tac_dong`, không có loại (FR-028a câu cuối) | FR-028a (bổ sung 2026-10-18) *(R-15: `loai_ly_do_khong_thuc_hien` → `danh_muc_chung`)* |
| `dong_boi_loai`, `dong_boi_id`, `can_cu_quyen`, `quy_tac_dong` | Theo research C-08 | | FR-028 |
| `cho_xac_nhan_do_lai` | `BOOLEAN` | | FR-034 (đo chỉ số) |
| `da_nhac_luc`, `da_bao_nptc_luc`, `da_bao_leo_thang_luc`, `so_lan_nhac` | | | FR-043, FR-044, FR-046 |
| `nguoi_lap_id`, `lap_luc` | | | FR-023 |
| `khoa_trung` | Cột sinh `IF(trang_thai <> 'HUY' AND loai_can_cu NOT IN ('PHAT_SINH','GHI_NHAN_PHAT_SINH','LAM_LAI','KIEM_TRA_DAU_CA','GIAM_SAT'), CONCAT(loai_can_cu,':',can_cu_id,':',nct_id,':',thoi_diem_du_kien), NULL)` | `UNIQUE` | FR-021, DBR-12 |
| `khoa_chuyen_mon_ca` | Cột sinh `IF(loai_can_cu IN ('KIEM_TRA_DAU_CA','GIAM_SAT') AND trang_thai <> 'HUY', CONCAT(loai_can_cu,':',ca_id,':',nguoi_thuc_hien_id), NULL)` | `UNIQUE` | FR-029, BR-M04-24 |
| `phien_ban` | `INT` | Không dùng cho lệnh trạng thái (C-03) | |

`CHECK`: `nct_id IS NOT NULL OR vi_tri_id IS NOT NULL`; `dau_khung <= thoi_diem_du_kien <= cuoi_khung` khi có khung.

Chỉ mục: `(trang_thai, dau_khung)`, `(trang_thai, cuoi_khung)`, `(trang_thai, muc_quan_trong, da_bao_nptc_luc, qua_han_luc)` cho job; `(ca_id, nguoi_thuc_hien_id)`, `(tang_khu_vuc_id, ca_id, nguoi_thuc_hien_id)` cho checklist; `(nct_id, thoi_diem_du_kien)` cho hủy tự động, thời khóa biểu, chuỗi bữa; `(loai_can_cu, can_cu_id)` cho hủy theo nguồn.

**Bảng chuyển** (khớp bảng FR-026; cài ở `BangChuyenCongViec`):

| Từ | Lệnh / sự kiện | Đến | Nơi thực hiện |
| --- | --- | --- | --- |
| — | Lập công việc của ca; tạo phát sinh; lập làm lại | `CHUA_DEN_HAN` (hoặc `DEN_HAN`, `QUA_HAN` theo đồng hồ, BR-M04-26) | `POST /ca/{caId}/cong-viec/lap`, `POST /cong-viec/phat-sinh`, `POST /cong-viec/{id}/lam-lai` |
| — | Ghi nhận phát sinh | `HOAN_THANH` | `POST /ghi-nhan-phat-sinh` |
| `CHUA_DEN_HAN` | Tới đầu khung | `DEN_HAN` | `ViecTheoPhutM04` |
| `CHUA_DEN_HAN` | Hủy tự động | `HUY` | Bean 001; `diemDanhVe`; `NguonCongViecApi.nguonBiHuy`; `CongViecVangMatApi.tinhLai` |
| `CHUA_DEN_HAN` | Hủy công việc (Trưởng tầng) | `HUY` | `POST /cong-viec/{id}/huy` |
| `DEN_HAN` | Ghi nhận hoàn thành | `HOAN_THANH` | `POST /cong-viec/{id}/ghi-nhan` |
| `DEN_HAN` | Ghi nhận không thực hiện | `KHONG_THUC_HIEN` | `POST /cong-viec/{id}/khong-thuc-hien` |
| `DEN_HAN` | Hết khung | `QUA_HAN` | `ViecTheoPhutM04` |
| `QUA_HAN` | Ghi nhận, thời điểm thực hiện trong khung | `HOAN_THANH` | `…/ghi-nhan` |
| `QUA_HAN` | Ghi nhận, sau khung | `HOAN_THANH_TRE` | `…/ghi-nhan` |
| `QUA_HAN` | Ghi nhận không thực hiện | `KHONG_THUC_HIEN` | `…/khong-thuc-hien` |
| `QUA_HAN` (Thường) | Ca sau xác nhận bàn giao chứa công việc | `KHONG_THUC_HIEN` | `CongViecCaApi.khiXacNhanBanGiao` (008) |
| `DEN_HAN`, `QUA_HAN` | Trạng thái cuối của người cao tuổi | `KHONG_THUC_HIEN` | Bean 001 |

---

## 7. Ghi nhận (mục D)

### `ket_qua_ghi_nhan` – nhóm 3 → gộp theo R-15: `ket_qua_ghi_nhan` → `cong_viec`

| Cột | Kiểu | Ghi chú | Nguồn |
| --- | --- | --- | --- |
| `id`, `cong_viec_id` | `BIGINT` | `UNIQUE (cong_viec_id)` | FR-036 |
| `nct_id`, `tang_khu_vuc_id` | `BIGINT` | `tang_khu_vuc_id` dùng cho đính chính cách B | 000 mục 1.3 |
| `loai_ket_qua` | Như `loai_cong_viec.loai_ket_qua` | | FR-034 |
| `gia_tri_so` | `DECIMAL(10,2) NULL` | ml, số lượng, thời gian ngủ (phút), giá trị đo (bản sao để hiển thị) | FR-034 |
| `gia_tri_ma` | `VARCHAR(40) NULL` | Mã ăn uống, vận động, mã danh mục | FR-034 |
| `gia_tri_json` | `JSON NULL` | Hành vi (có/không, loại, mô tả), ngủ (thời gian, chất lượng) | FR-034 |
| `vuot_gioi_han_da_xac_nhan` | `BOOLEAN` | | FR-034 |
| `thoi_diem_thuc_hien` | `DATETIME(6)` | | FR-035 |
| `thoi_diem`, `thoi_diem_thiet_bi`, `nguoi_thuc_hien_id` | Mẫu 000 | | DBR-25 |
| `nhan_ghi_muon` | `BOOLEAN` | | BR-M04-12 |
| `nhan_dong_bo_sau_mat_quyen` | `BOOLEAN` | 002 A-11 | |
| `can_cu_quyen` | `ENUM('DUOC_GIAO','VAI_TRO_TRUONG_TANG','NHIEM_VU_PHU_TRACH_CA')` | | FR-033 |
| `ban_ghi_chi_so_id` | `BIGINT NULL` | Bản ghi của 007 | FR-041 |
| `bua_id` | `BIGINT NULL` | Bản sao căn cứ bữa để đếm chuỗi nhanh | FR-040 |
| `ghi_chu` | `VARCHAR(2000) NULL` | | 8.6 |

`CHECK`: `thoi_diem_thuc_hien <= COALESCE(thoi_diem_thiet_bi, thoi_diem)`. Trigger nhóm 3 của 000 (không sửa, không xóa). Chỉ mục `(nct_id, loai_ket_qua, thoi_diem_thuc_hien)` cho quy tắc mục F.

### `lich_su_xu_ly_cong_viec` – nhóm 3 → gộp theo R-15: `lich_su_xu_ly_cong_viec` → `lich_su_chung`

`id`, `cong_viec_id`, `loai` (`NHAC`, `NHAC_GOP`, `BAO_NPTC`, `BAO_TRUONG_TANG`, `YEU_CAU_CANH_BAO`, `YEU_CAU_DONG_CANH_BAO`, `NHAN_VIEC`, `PHAN_LAI`, `THANH_CHUNG`, `GHI_VAO_BAN_GIAO`, `CHUYEN_CA`, `HE_THONG_DONG_SAU_BAN_GIAO`, `HE_THONG_DONG_TRANG_THAI_CUOI`), `nguoi_id`, `can_cu`, `truoc`, `sau` (JSON: người thực hiện trước/sau), `ly_do`, `thoi_diem`, `chay_bu`. Dùng cho FR-031, FR-045, FR-046 ("số lần đã nhắc"), US5 #7.

---

## 8. Bảng kỹ thuật

- `lan_chay_m04 (viec, moc)`: khóa chính hai cột; `viec` ∈ `NUOC_UONG` (mốc = ngày), `CA_CHUA_CO_CONG_VIEC`, `TON_CHO_XAC_NHAN` (mốc = ca), `HIEU_LUC_KE_HOACH`, `CO_MAT_DAU_NGAY`, `BAN_TRU_HET_NGAY`, `XEM_LAI_THOA_THUAN` (bổ sung 2026-10-18, Q-360) (mốc = ngày); `ket_qua`, `lan_chay_lich_id` (000). Bảo đảm mỗi việc mỗi mốc một lần, kể cả chạy bù (FR-052).

---

## 9. Bảo đảm ở cơ sở dữ liệu (tổng hợp)

| Bảo đảm | Cách làm | DBR / FR |
| --- | --- | --- |
| Tối đa một phiên bản Hiệu lực mỗi người; khoảng không giao | Cột sinh `hieu_luc_nct UNIQUE`; kiểm khoảng dưới khóa `cham_soc_nct` | DBR-11, FR-004 *(R-15: `cham_soc_nct` → `trang_thai_nct`)* |
| Tối đa một phiên bản Nháp / Chờ duyệt / Chờ hiệu lực | Cột sinh `mo_nct UNIQUE` | FR-004 |
| Mục kế hoạch chỉ sửa ở Nháp | Trigger `trg_muc_ke_hoach_khoa` | FR-004, BR-M04-19 |
| Thỏa thuận lựa chọn có rủi ro chỉ sửa ở Nháp; người quyết định khớp nhận định | Trigger `trg_thoa_thuan_*_khoa`; `CHECK` trên `thoa_thuan_rui_ro` | FR-002a, BR-M04-19 *(R-15: `thoa_thuan_rui_ro` → `muc_ke_hoach`)* |
| Không thực hiện do nhân viên có loại lý do; loại "Người cao tuổi từ chối" luôn hiệu lực | `CHECK` trên `cong_viec`; `CHECK` và trigger trên `loai_ly_do_khong_thuc_hien` | FR-028a *(R-15: `loai_ly_do_khong_thuc_hien` → `danh_muc_chung`)* |
| Ngày hiệu lực muộn hơn ngày duyệt | Backend trong lệnh Duyệt; `CHECK (duyet_luc IS NULL OR ngay_hieu_luc > DATE(duyet_luc))` | Q-34, SC-010 |
| Công việc duy nhất theo (căn cứ, thời điểm dự kiến), bỏ qua Hủy | Cột sinh `khoa_trung UNIQUE` | DBR-12, FR-021 |
| Kiểm tra đầu ca, giám sát một lần mỗi (ca, điều dưỡng) | Cột sinh `khoa_chuyen_mon_ca UNIQUE` | FR-029, BR-M04-24 |
| Trạng thái cuối của công việc không đổi | Trigger `trg_cong_viec_trang_thai_cuoi` | FR-028 |
| Mỗi công việc tối đa một kết quả gốc | `UNIQUE (cong_viec_id)` | FR-036 |
| Kết quả, điểm danh, lịch sử xử lý chỉ thêm; đính chính trỏ đúng một bản gốc | Trigger nhóm 3 của 000; `dinh_chinh` của 000 | DBR-23, BR-M04-14 |
| Một trạng thái có mặt mỗi (người, ngày) | `UNIQUE (nct_id, ngay)` | FR-012, CHK036 |
| Kết quả có tính phí có nguồn | Sự kiện `KetQuaCoTinhPhi` mang `ketQuaId`; 010 giữ DBR-15 | DBR-15 |
| Thời gian | `Clock` của 000; `thoi_diem_thiet_bi` cho ngoại tuyến | DBR-25 |

---

## 10. Nhật ký và phân loại trường

- Hành động theo research C-17.
- `PhanLoaiTruongProvider`:
  - `SUC_KHOE`: mọi giá trị kết quả (`gia_tri_*`, `ghi_chu`) của `M04.KET_QUA_GHI_NHAN`; `noi_dung` của mục kế hoạch và công việc; mọi trường của thỏa thuận lựa chọn có rủi ro và ý kiến của nó (bổ sung 2026-10-18); loại lý do và ghi chú Không thực hiện;
  - `CONG_KHAI_NOI_BO`: trạng thái công việc, thời điểm, người thực hiện; trạng thái có mặt và giờ đến/về.
- Hành chính thấy trạng thái có mặt và điểm danh; không thấy kết quả chăm sóc (FR-007, FR-049).

---

## 11. Lưu giữ (000 R-11)

| Loại | `LoaiHan` | Mốc |
| --- | --- | --- |
| `M04.KE_HOACH` (cùng mục, thỏa thuận và ý kiến), `M04.CONG_VIEC` có `nct_id` (cùng kết quả, lịch sử xử lý), `M04.CO_MAT_BAN_TRU` (cùng điểm danh) | `THEO_HO_SO` | Thời điểm trạng thái cuối của người cao tuổi |
| `M04.CONG_VIEC` gắn vị trí (kiểm tra đầu ca, giám sát) | `KHONG_GAN_HO_SO` | `thoi_diem_du_kien` |

Danh mục loại công việc, giá trị kết quả, loại lý do Không thực hiện không bị loại bỏ.

---

## 12. Truy vết DBR, BR và FR

| Mã | Bảo đảm (mục) | Test (quickstart mục 4) |
| --- | --- | --- |
| DBR-11 | Mục 3, 9 | `DBR_11_MotPhienBanHieuLucIT` |
| DBR-12 | Mục 6, 9 | `DBR_12_KhongTrungCanCuIT`, `FR_021_LapLaiSauHuyVangIT` |
| DBR-15, DBR-17 (phần 005) | Sự kiện tính phí, đính chính | `FR_051_SuKienTinhPhiIT`, `US7_4_DinhChinhCoTinhPhiIT` |
| DBR-23, DBR-25 | Trigger 000; `Clock`; `thoi_diem_thiet_bi` | `NhomBaTriggerIT` (000) mở rộng, `BR_M04_12_GhiMuonNgoaiTuyenIT` |
| BR-M04-01 | `KiemTraLapCongViec`, lệnh lập theo lô, nhắc ca | `BR_M04_01_LapCongViecCaIT`, `KiemTraLapCongViecTest` |
| BR-M04-02 | Mục 5; hủy khi điểm danh về | `BR_M04_02_BanTruIT` |
| BR-M04-03 | `KeHoachDaDoi`, không hủy | `BR_M04_03_KeHoachDoiChiBaoIT` |
| BR-M04-04 | Bean 001 | `BR_M04_04_HuyKhiVangIT` |
| BR-M04-05 | Job theo phút | `BR_M04_05_QuaHanNhacIT` |
| BR-M04-06 | Job theo phút; lý do khi đóng | `BR_M04_06_LeoThangTheoMucIT` |
| BR-M04-07 | `CongViecBanGiaoApi` (theo BR-M09-06, Q-307; plan điểm báo lại 1) | `BR_M04_07_DanhSachChoBanGiaoIT` |
| BR-M04-08 | `DanhGiaNuocUong` | `BR_M04_08_NuocUongTest`, `NuocUongIT` |
| BR-M04-09 | `DemChuoiBuaKem` | `BR_M04_09_ChuoiBuaKemTest` |
| BR-M04-10 | `ChiSoPort` | `BR_M04_10_DoChiSoIT` |
| BR-M04-11 | `DemNgayTamTrang` | `BR_M04_11_TamTrangTest` |
| BR-M04-12 | Quyền ghi; nhãn ghi muộn | `BR_M04_12_QuyenGhiNhanIT`, `XacDinhTrangThaiDongTest` |
| BR-M04-13 | `CongViecCaApi.khiVangCa` | `BR_M04_13_CongViecChungIT` |
| BR-M04-14 | Nhóm 3; provider đính chính | `BR_M04_14_DinhChinhIT` |
| BR-M04-19 | Mục 3 | `BR_M04_19_PhienBanKhoaIT`, `FR_003_VongDoiKeHoachIT` |
| BR-M04-20 | Listener bốn nguồn | `BR_M04_20_BaoXemXetKeHoachIT` |
| BR-M04-24 | `khoa_chuyen_mon_ca` | `BR_M04_24_KiemTraDauCaIT` |
| BR-M04-25 | `ChuoiThoiDiemMuc` | `BR_M04_25_ChuoiMoiNGioTest` |
| BR-M04-26 | Trạng thái lúc lập; nhắc gộp | `BR_M04_26_LapMuonVaChayBuIT` |
| BR-M04-27 | Handler ngoại tuyến | `BR_M04_27_DongBoCongViecDaDongIT`, test Flutter |
| BR-M04-28 | `CongViecCaApi.khiDoiPhanCong`; `TRUNG_KHUNG_PHIEN_BAN_CU` | `BR_M04_28_GanLaiIT` |
| BR-M04-29 | `da_nhac_luc` một lần | `BR_M04_29_NhacMotLanIT` |
| FR-002a, BR-M01-14, 15 (phần 005) | `thoa_thuan_rui_ro` và bảng con; không là căn cứ công việc | `FR_002a_ThoaThuanRuiRoIT` *(R-15: `thoa_thuan_rui_ro` → `muc_ke_hoach`)* |
| FR-028a | `loai_ly_do_khong_thuc_hien`; `CHECK` trên `cong_viec` | `FR_028a_LoaiLyDoKhongThucHienIT` *(R-15: `loai_ly_do_khong_thuc_hien` → `danh_muc_chung`)* |
| BR-M09-01 (phần 005) | `NhanSuCaPort.duDieuKien` | `BR_M09_01_ChungChiCongViecIT` |
| BR-M09-08 (phần 005) | `khiXacNhanBanGiao`, job tồn chờ | `FR_047_TuDongDongSauBanGiaoIT`, `FR_047a_TonChoXacNhanIT` |
| BR-M15-20 (phần 005) | Kết quả đồng bộ từng bản ghi | Test Flutter `KetQuaDongBoTest` |

BR-M04-15 → 18, 21 → 23 thuộc feature 014; 005 chỉ cung cấp lệnh lập công việc làm lại (BR-M04-23, FR-026a) và giờ về dự kiến (FR-018a).
