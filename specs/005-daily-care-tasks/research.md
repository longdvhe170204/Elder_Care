# Research: Chăm sóc hằng ngày (005)

**Ngày**: 2026-10-09 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 (000), A-01 → A-14 (002), H-01 → H-15 (001), G-01 → G-16 (003), T-01 → T-16 (004) được dẫn lại, không quyết lại. Không mục nào đổi nghiệp vụ (constitution IX); chỗ spec hoặc nguồn còn thiếu ghi ở "Điểm cần báo lại" của plan.

---

## C-01. Vị trí trong backend

**Quyết định**: Feature 005 là module M04 (mục 8, phần 8.1 → 8.7). Gói `com.cambae.backend.m04` có các gói con:

| Gói | Nội dung | Mục spec |
| --- | --- | --- |
| `kehoach` | Phiên bản kế hoạch, mục kế hoạch, duyệt, so sánh, thông báo xem xét kế hoạch | A |
| `comat` | Trạng thái có mặt bán trú theo ngày, điểm danh đến/về, giờ về dự kiến | B |
| `congviec` | Lập công việc của ca, công việc phát sinh, làm lại; vòng đời; hủy tự động; gán lại | C |
| `ghinhan` | Kết quả ghi nhận, Ghi nhận phát sinh, đính chính, ngoại tuyến | D |
| `checklist` | Checklist theo ca, công việc chung, nhận việc, phân lại (chỉ đọc + lệnh phân công) | D |
| `danhmuc` | Loại công việc | E |
| `quytac` | Quy tắc mục F: nước uống, chuỗi bữa kém, chuỗi ngày tâm trạng | F |
| `quahan` | Nhắc, leo thang, xử lý việc quá hạn, giao tiếp bàn giao với 008 | G |
| `thoikhoabieu` | Thời khóa biểu cá nhân (chỉ đọc, tổng hợp) | H |
| `trangthai` | Bean `ThamGiaChuyenTrangThai` của 001 | A, C (FR-010, FR-020) |
| `lich` | Job theo phút và job hằng ngày (C-12) | K |
| `api` | Giao diện, DTO, sự kiện, SPI công khai | J |

Bảng `cham_soc_nct` (một dòng mỗi hồ sơ người cao tuổi) là đích khóa tuần tự cho lệnh kế hoạch và các lệnh của 005 cần nhìn toàn bộ công việc của một người (FR-006, FR-010, FR-021), giống `luu_tru_nct` của 004.

**Lý do**: Constitution X. Khóa dòng `nguoi_cao_tuoi` của M01 là truy cập dữ liệu module khác.

---

## C-02. Chiều phụ thuộc giữa module

**Quyết định**: Theo mẫu 001 H-02, 003 G-02, 004 T-02: module làm plan trước khai báo SPI trong gói `api` của mình, module làm plan sau cài.

| Cặp | Chiều mã | Cách giữ một chiều |
| --- | --- | --- |
| 005 → 000, 002 | 005 dùng | `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `NhacQuaHan`, `QuyenApi`, `PhamViApi`, `NguoiDuyetApi`, `NgoaiTuyenHandler` |
| 005 ↔ 001 | 005 → 001 | 005 cài `ThamGiaChuyenTrangThai`; đọc `NguoiCaoTuoiApi`, `DanhGiaApi.luuYHienHanh`, `TrangThaiNguoiCaoTuoiApi.khoangTrangThai`; nghe `DanhGiaDaDoiMucHoacNhuCau` |
| 005 ↔ 003 | 005 → 003 | Đọc `ViTriApi.hienTai`, `CoCauViTriApi`; nghe `DaChuyenGiuong` |
| 005 ↔ 004 | 005 → 004 | Đọc `HopDongDocApi.lichDenTaiNgay`; cài `TrangThaiCoMatBanTruPort`; nghe `PhuLucDaApDung`, `BaoVangBanTruDaGhi`, `NgayNghiBanTruDaDoi` |
| 005 ↔ 006, 007, 008, 010, 011, 012, 014 | Các module này → 005 | Họ làm plan sau và cần gọi lệnh của 005 trong transaction của mình (vắng ca, xác nhận bàn giao, hủy nguồn, xác nhận đo lại, dời giờ về). 005 cần dữ liệu của họ qua SPI khai báo ở `m04.api` (module-interfaces mục 2.1): `NhanSuCaPort` (008), `NguonCongViecPort` (007, 011, 014 mỗi bên một bean), `ChiSoPort`, `CanhBaoDocPort` (007), `LieuThuocChecklistPort` (006), `BanGhiDonBanTruPort` (012), `KetQuaKiemTraPort` (014), `ThoiKhoaBieuNguonPort` (006, 012, 014) |
| 005 → 007, 009, 010, 011, 016 | Sự kiện | Yêu cầu tạo/gộp/đóng cảnh báo, thông báo, sự kiện tính phí, ăn kém kéo dài: sự kiện ghi cùng transaction qua outbox (C-13) |

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02). 008 phải gọi 005 đồng bộ khi ca sau xác nhận bàn giao (FR-047 xét trạng thái "tại thời điểm xác nhận"), khi ghi vắng ca, nghỉ việc, đổi phân công (FR-024, FR-032). Nếu 005 gọi trực tiếp API của 008 thì có vòng; nên dữ liệu ca, phân công, chứng chỉ mà 005 cần đi qua `NhanSuCaPort`.

**Phương án đã cân nhắc**: 008 chỉ phát sự kiện sau commit cho 005: không đáp ứng FR-047 (đóng tại đúng thời điểm xác nhận, trong cùng lệnh) và FR-047a (công việc đã nhận trong lúc chờ không bị đóng), vì giữa commit và lúc nghe có khoảng hở.

---

## C-03. Khóa và đồng thời

**Quyết định**:
- **Thứ tự khóa toàn hệ thống** (nối 003 G-03, 004 T-03): `nguoi_cao_tuoi` (001, khi lệnh đi qua 001) → `luu_tru_nct` (004) → `vi_tri_nct`, `giuong` (003) → `cham_soc_nct` (005) → dòng nghiệp vụ của 005 (phiên bản kế hoạch, công việc, có mặt). Bean của 005 chạy sau 004, 003 theo `@Order` của 001.
- **Lệnh trên một công việc** (ghi nhận, không thực hiện, hủy, nhận việc, phân lại): `SELECT … FOR UPDATE` dòng `cong_viec`, kiểm tra lại trên trạng thái mới. Không dùng `If-Match`, vì FR-036 đòi lần sau bị từ chối kèm "ai đã đóng", không phải lỗi phiên bản. Lỗi trả 409 `CONG_VIEC_DA_DONG` với `dongBoi`, `dongLuc`, và `noiDungBiTuChoi` (nội dung kết quả gửi lên) để client giữ và chuyển thành đính chính hoặc Ghi nhận phát sinh.
- **Lập công việc của ca theo lô**: lệnh khóa các dòng `cham_soc_nct` của những người có trong lô, theo thứ tự `nct_id` tăng, rồi kiểm tra từng công việc. Một công việc lỗi thì cả lô bị từ chối với danh sách lỗi theo dòng ("hoặc toàn bộ, hoặc không", 000). Khóa trùng (FR-021) còn có cột sinh `UNIQUE` làm lưới an toàn.
- **Job chuyển trạng thái theo giờ** (C-12) cũng khóa từng dòng công việc, nên lệnh ghi nhận và job không cùng đóng một công việc.

**Lý do**: SC-011 và Edge Case "hai nhân viên cùng ghi" đòi đúng một lần đóng.

---

## C-04. Kế hoạch chăm sóc theo phiên bản (mục A, FR-001 → FR-011, BR-M04-19, 20, DBR-11)

**Quyết định**:
- **Bảng**: `ke_hoach_cham_soc` (phiên bản, nhóm 2) và `muc_ke_hoach` (bảng con). `muc_ke_hoach` sửa được chỉ khi phiên bản ở `NHAP`; trigger `trg_muc_ke_hoach_khoa` chặn `INSERT/UPDATE/DELETE` khi phiên bản cha không ở `NHAP` (FR-004, BR-M04-19).
- **Một phiên bản đang soạn/chờ**: cột sinh `mo_nct = IF(trang_thai IN ('NHAP','CHO_DUYET','CHO_HIEU_LUC'), nct_id, NULL)` có `UNIQUE` (FR-004).
- **DBR-11**: cột sinh `hieu_luc_nct = IF(trang_thai='HIEU_LUC', nct_id, NULL)` có `UNIQUE`; khoảng `[hieu_luc_tu, hieu_luc_den]` của các phiên bản Hiệu lực và Hết hiệu lực không giao nhau, kiểm ở backend dưới khóa `cham_soc_nct` (mẫu 000 mục 1.2). Lệnh Duyệt chặn `ngay_hieu_luc <= ngày duyệt` (Q-34); vì vậy phiên bản mới luôn bắt đầu sau phiên bản đang Hiệu lực và việc đặt `hieu_luc_den` của phiên bản trước là ngày liền trước không tạo giao khoảng.
- **Vòng đời riêng**, không dùng loại yêu cầu của 000: bảng FR-003 có Trả lại về Nháp, Thu hồi duyệt, hủy do trạng thái cuối, không khớp vòng đời chung của 000. Phần dùng lại của 000:
  - quyền duyệt `KhoaQuyen(KE_HOACH_CHAM_SOC, DUYET)` qua `QuyenApi` (Bác sĩ mặc định; Điều dưỡng khi được gán thêm, Q-15, 002 A-05);
  - tự duyệt (Q-10): lệnh Duyệt của người lập bắt buộc `lyDo`, cột `tu_duyet = true`, nhật ký có dấu;
  - nhắc người duyệt (CFG-M15-05) và báo Quản lý viện (CFG-M15-06) bằng `NhacQuaHan` của 000 từ job hằng giờ của 005 (FR-005); người nhận lấy từ `NguoiDuyetApi.danhSach`.
- **Ngày hiệu lực**: job hằng ngày lúc 00:00 (C-12) chuyển phiên bản `CHO_HIEU_LUC` có `ngay_hieu_luc <= hôm nay` sang `HIEU_LUC`, đặt phiên bản trước `HET_HIEU_LUC` với `hieu_luc_den = ngay_hieu_luc − 1`, rồi phát `KeHoachDaDoi(nctId, phienBanMoi, phienBanCu)` cho 009 (Người phụ trách ca, Trưởng tầng của tầng người cao tuổi; FR-006, BR-M04-03). Không đụng tới công việc.
- **Lập phiên bản** (FR-003 dòng đầu): nội dung khởi đầu là bản sao các mục của phiên bản Hiệu lực, hoặc trống. Không có đường tạo tự động từ đánh giá (Q-280).
- **So sánh hai phiên bản** (FR-008): hàm thuần `SoSanhPhienBan` ghép mục theo `muc_goc_id` (mục được chép từ phiên bản trước giữ id gốc), trả thêm / bớt / đổi theo từng trường.
- **Thông báo xem xét kế hoạch** (FR-011, BR-M04-20, Q-306): 005 nghe bốn nguồn và phát `CanXemXetKeHoach(nctId, nguon, thamChieu)` cho 009, người nhận là mọi điều dưỡng có người cao tuổi trong phạm vi ca hiện tại (`NhanSuCaPort.dieuDuongPhuTrach`) và người có quyền duyệt (`NguoiDuyetApi`). Không có bảng yêu cầu, không có hạn, không có job theo dõi:

  | Nguồn | Sự kiện nghe |
  | --- | --- |
  | Đánh giá lại đổi mức hoặc nhu cầu (Q-296) | `DanhGiaDaDoiMucHoacNhuCau` (001) |
  | Sự cố ngã | `SuCoNgaDaGhiNhan` (007; khai báo tên ở module-interfaces mục 2.2) |
  | Phụ lục đổi mức áp dụng | `PhuLucDaApDung` với `loaiThayDoi` chứa `MUC_CHAM_SOC` (004) |
  | Lựa chọn ở lại viện chăm sóc giảm nhẹ | `LuaChonChamSocGiamNheDaGhi` (007) |

- **Trạng thái cuối** (FR-003 hai dòng cuối, FR-010): bean `ThamGiaChuyenTrangThai` của 005 cho `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN` chuyển phiên bản Hiệu lực sang Hết hiệu lực (`hieu_luc_den` = ngày của lệnh) và phiên bản Nháp, Chờ duyệt, Chờ hiệu lực sang Đã hủy, trong transaction của 001.
- **Dấu "chưa có kế hoạch chăm sóc hiệu lực"** (FR-009) suy ra lúc đọc: người Đang lưu trú không có dòng `hieu_luc_nct`.

**Lý do**: Bảng trạng thái của spec là căn cứ (8.1 đã chép bảng này). Cột sinh `UNIQUE` bảo đảm hai điều "tối đa một" ở cơ sở dữ liệu.

**Phương án đã cân nhắc**: Loại yêu cầu `M04.DUYET_KE_HOACH` của 000: phải thêm chuyển "Trả lại về Nháp" và "Thu hồi" vào vòng đời chung, đổi hành vi đã thiết kế của 000.

---

## C-05. Mục kế hoạch và chuỗi thời điểm dự kiến (FR-002, BR-M04-25)

**Quyết định**:
- `muc_ke_hoach` lưu tần suất có cấu trúc: `kieu_tan_suat` (`SO_LAN_NGAY`, `MOI_N_GIO`, `NGAY_TRONG_TUAN`, `MOT_LAN_MOI_CA`), `so_lan`, `n_gio`, `moc_bat_dau` (`TIME`, rỗng khi "trong ca"), `ngay_trong_tuan` (tập bit), `cac_moc` (JSON danh sách giờ khi "số lần/ngày" có mốc cụ thể).
- Hàm thuần `ChuoiThoiDiemMuc.thoiDiemTrongCa(muc, ca, ngay)`:
  - `MOI_N_GIO`: bắt đầu lại mỗi ngày từ `moc_bat_dau`, lặp mỗi N giờ tới trước 24:00, không nối sang ngày sau; lấy các thời điểm thuộc `[batDauCa, ketThucCa)`; ca qua đêm được tách theo hai ngày;
  - `NGAY_TRONG_TUAN`: lọc theo ngày của thời điểm;
  - `MOT_LAN_MOI_CA` hoặc mốc "trong ca": một thời điểm, khung bằng cả ca.
- Hàm này **không sinh công việc**. Nó chỉ dùng để kiểm lệnh lập: công việc có căn cứ là mục kế hoạch phải có thời điểm dự kiến thuộc chuỗi, khung và mức quan trọng bằng của mục (FR-017, US2 #1). Người lập tự nhập từng công việc (Q-307).
- Khung thời gian của mục: lấy theo thứ tự mục → loại công việc → CFG-M04-02. Giá trị đã điền vào mục được lưu cố định trên mục khi gửi duyệt (FR-038 câu cuối), không đọc lại loại công việc.

**Lý do**: Spec yêu cầu chặn công việc "không khớp mục kế hoạch"; muốn chặn thì phải tính được tập thời điểm hợp lệ.

**Phương án đã cân nhắc**: Lưu tần suất dạng văn bản tự do: không kiểm được.

---

## C-06. Trạng thái có mặt bán trú và điểm danh (mục B, FR-012 → FR-016, FR-018a, FR-019)

**Quyết định**:
- **Bảng** `co_mat_ban_tru` (nhóm 2), khóa `(nct_id, ngay)` `UNIQUE`; cột `gio_den_du_kien`, `gio_ve_du_kien` (theo lịch), `gio_ve_du_kien_hien_hanh` (sau khi 014 dời, FR-018a), `trang_thai` (`CHUA_DEN`, `CO_MAT`, `DA_VE`, `VANG_CO_BAO`, `VANG_KHONG_BAO`), `buoi_phat_sinh`, `bao_vang_muon_id`.
- **Xác định trạng thái đầu ngày** (FR-012 "tính từ lịch đến, không cần lệnh"): job hằng ngày 00:00 đọc `HopDongDocApi.lichDenTaiNgay` (004) cho mọi người bán trú Đang lưu trú, bỏ ngày khu nghỉ, ghi dòng `CHUA_DEN` hoặc `VANG_CO_BAO` (đã có báo vắng đủ hạn) và phát `TrangThaiCoMatDaXacDinh` cho 010, 011 (FR-016). Đây là trạng thái do hệ thống xác định theo bảng FR-013 (dòng "— → Chưa đến, Hệ thống"), không phải bản ghi nghiệp vụ do hệ thống tự lập (000 FR-010a). Lịch đổi trong ngày (phụ lục 004 áp dụng, báo vắng mới, ngày nghỉ khai báo sau 00:00) được cập nhật bằng listener của `PhuLucDaApDung`, `BaoVangBanTruDaGhi`, `NgayNghiBanTruDaDoi`.
- **Điểm danh** là bản ghi nhóm 3 `diem_danh_ban_tru` (loại `DEN`, `VE`; thời điểm; người; `ban_ghi_don_id`; `loai_ve`: `QUA_DON`, `TU_VE`, `VIEN_DUA_VE`). Dòng `co_mat_ban_tru` trỏ tới bản ghi đến và về. Sai giờ đi đường đính chính `M04.DIEM_DANH_BAN_TRU` (FR-015); `sauKhiDinhChinh` cập nhật giờ trên dòng có mặt, phát lại sự kiện cho 010.
- **Điểm danh đến** `POST /co-mat-ban-tru/diem-danh-den`: chỉ trực tuyến; người cao tuổi Đang lưu trú (001); từ `CHUA_DEN`, `VANG_KHONG_BAO`, hoặc ngày không có lịch / ngày khu nghỉ (tạo dòng với `buoi_phat_sinh = true`, FR-013 dòng 2, Q-140). Tác động: `CO_MAT`, phát `DaDiemDanhDen` cho 009 (Người phụ trách ca của khu bán trú, FR-018, SC-009) và `TrangThaiCoMatDaDoi` cho 010, 011.
- **Điểm danh về** `POST /co-mat-ban-tru/{id}/diem-danh-ve` với `banGhiDonId` của 012. 005 kiểm qua `BanGhiDonBanTruPort.hopLe(banGhiDonId, nctId, thoiDiem)` (012 cài): người đón hợp lệ, hoặc "Tự về" khi dấu "được tự về" có tác dụng (Q-125). Trường hợp "Viện đưa về" (Q-295): lệnh ghi bàn giao của 012 gọi `CoMatBanTruApi.diemDanhVe(...)` (MANDATORY) trong transaction của 012, nên trước lúc ghi bàn giao người đó vẫn `CO_MAT`. Tác động: `DA_VE`, hủy công việc Chưa đến hạn sau giờ về (C-08).
- **Vắng không báo**: job theo phút chuyển `CHUA_DEN` sang `VANG_KHONG_BAO` khi `bây giờ >= gio_den_du_kien + CFG-M02-07`, báo Trưởng tầng. Buổi trùng ngày khu nghỉ không có dòng nên không chuyển (Q-140).
- **Hết ngày vẫn Có mặt** (FR-019): job 00:00 tìm dòng của ngày trước còn `CO_MAT`, phát `BanTruConCoMatHetNgay` (Trung bình cho Người phụ trách ca đang diễn ra của khu, Nhẹ cho Trưởng tầng; Q-115). Không đổi trạng thái; dòng vẫn chờ lệnh Điểm danh về (Edge Case CHK036).
- **Giờ về dự kiến** (FR-018a): 014 gọi `CoMatBanTruApi.doiGioVe(nctId, ngay, gioMoi, dangKyId)` và `traGioVe(nctId, ngay, dangKyId)` (MANDATORY). 005 cài `TrangThaiCoMatBanTruPort` của 004 và cung cấp `CoMatBanTruApi.gioVeTheoNgay` cho 003, 010, 011.
- **Báo vắng muộn** (FR-014): `BaoVangBanTruDaGhi(duHan = false)` của 004 chỉ gắn `bao_vang_muon_id`, không đổi trạng thái.

**Lý do**: Một dòng mỗi (người, ngày) giữ đúng "mỗi ngày có đúng một trạng thái có mặt" (Edge Case CHK036) và là chỗ 010, 011 đọc.

**Phương án đã cân nhắc**: Không ghi dòng `CHUA_DEN`, suy ra lúc đọc từ lịch của 004: mọi nơi đọc (010, 011, 016, bàn giao) phải ghép lịch, và không có chỗ ghi "Vắng không báo".

---

## C-07. Lập công việc (FR-017 → FR-025, FR-026a, FR-029, BR-M04-01, 24 → 26, Q-306, Q-307)

**Quyết định**:
- **Lệnh lập theo lô** `POST /ca/{caId}/cong-viec/lap` (người lập là Người phụ trách ca của ca, hoặc Trưởng tầng của tầng được giao; 19.3): thân là danh sách công việc người lập tự nhập. Mỗi dòng có `nctId` (hoặc vị trí với kiểm tra đầu ca, giám sát), `loaiCongViecId`, `canCu {loai, id}`, `thoiDiemDuKien`, `khungTruoc`, `khungSau`, `mucQuanTrong`, `nguoiThucHienId` hoặc `congViecChung = true`. Hệ thống không trả danh sách gợi ý, không điền sẵn (Q-307). Màn hình chỉ hiển thị dữ liệu nguồn (mục kế hoạch Hiệu lực, lịch đo, lịch bữa, buổi đã đăng ký, các việc theo dõi) để người lập đối chiếu; đó là dữ liệu đã có, không phải đề xuất.
- **Mốc sớm nhất**: Người phụ trách ca lập từ `batDauCa − CFG-M04-01`; Trưởng tầng lập cho mọi ca đã công bố của tầng (`NhanSuCaPort.caDaCongBo`), kể cả trước mốc đó (FR-017).
- **Kiểm tra từng dòng** (hàm `KiemTraLapCongViec`, trả đủ lỗi theo dòng):

  | Mã lỗi | Kiểm tra | Nguồn |
  | --- | --- | --- |
  | `NCT_KHONG_DANG_LUU_TRU` | Người cao tuổi không ở Đang lưu trú (001) | FR-017 |
  | `NCT_VANG_MAT` | Đang Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài lúc lập (không biết lúc nào về nên chặn mọi thời điểm từ nay) | FR-017, BR-M04-04 |
  | `BAN_TRU_CHUA_CO_MAT` | Bán trú: dòng có mặt của ngày không ở `CO_MAT`, hoặc thời điểm ngoài `[gio_den_thuc_te, gio_ve_du_kien_hien_hanh]` | FR-018, BR-M04-02 |
  | `NGOAI_CA` | Thời điểm dự kiến ngoài `[batDauCa, ketThucCa)` | FR-017 |
  | `KHONG_KHOP_MUC_KE_HOACH` | Căn cứ là mục kế hoạch của phiên bản Hiệu lực tại ngày đó, thời điểm thuộc `ChuoiThoiDiemMuc`, khung và mức bằng mục | FR-002, FR-017 |
  | `CAN_CU_KHONG_HOP_LE` | Căn cứ khác (lần đo, bữa, buổi, sự cố ngã, danh sách tiếp xúc, cảnh báo uống nước thiếu, kết quả kiểm tra Không đạt) được module sở hữu xác nhận qua `NguonCongViecPort.kiemTra(loai, id, nctId, thoiDiem)`; công việc từ lịch đo theo chỉ định phải mức Bắt buộc | FR-017, FR-023 |
  | `LIEU_THUOC_KHONG_LA_CONG_VIEC` | Loại công việc nhóm "uống thuốc" | FR-017, 8.3 |
  | `NGUOI_THUC_HIEN_KHONG_HOP_LE` | Người được chọn không có trong phân công chăm sóc của người cao tuổi ở ca đó với vai trò khớp (Điều dưỡng được nhận việc của vai trò Nhân viên chăm sóc, Q-214); hoặc chọn "công việc chung" khi phân công có người khớp | FR-024 |
  | `THIEU_CHUNG_CHI` | `NhanSuCaPort.duDieuKien(nhanVienId, chungChiYeuCau, ngay)` sai | BR-M09-01 |
  | `TRUNG_CAN_CU` | Đã có công việc chưa Hủy cùng (loại căn cứ, căn cứ, thời điểm dự kiến) | FR-021, DBR-12 |
  | `TRUNG_KHUNG_PHIEN_BAN_CU` | Còn công việc cùng loại của phiên bản trước ở Đến hạn hoặc Quá hạn mà khung chứa thời điểm mới | FR-006 (CHK039) |
  | `KIEM_TRA_DAU_CA_DA_CO` | Kiểm tra đầu ca hoặc giám sát đã có cho (ca, điều dưỡng) | FR-029, BR-M04-24 |

- **Trạng thái lúc lập** (BR-M04-26, FR-052 a): theo đồng hồ lúc lập: trước đầu khung là Chưa đến hạn; trong khung là Đến hạn; sau khung là Quá hạn (kèm nhắc FR-043 và leo thang FR-044 tính từ lúc lập).
- **Nhắc ca chưa có công việc** (FR-017): job theo phút, tại `batDauCa`, nếu tầng/khu của ca chưa có công việc nào của ca thì phát `CaChuaCoCongViec` cho Người phụ trách ca và Trưởng tầng. Bảng `lan_chay_m04` (việc, mốc) bảo đảm một lần mỗi ca; chạy bù chỉ gửi khi ca còn diễn ra (FR-052 g).
- **Báo Người phụ trách ca lập công việc** (không sinh): phát `CanLapCongViec(nctId, lyDo, tuThoiDiem)` cho 009 khi điểm danh đến (FR-018), trở về Đang lưu trú (FR-020), Hoàn tất tiếp nhận (FR-017 CHK008), đính chính chuyến đi làm công việc đã Hủy nay nằm ngoài khoảng đi (FR-020a).
- **Công việc phát sinh** `POST /cong-viec/phat-sinh` (Trưởng tầng, Điều dưỡng; FR-025; 19.3: Điều dưỡng không giữ nhiệm vụ Người phụ trách ca chỉ tạo được phát sinh). Không chịu khóa trùng; vẫn qua các kiểm tra có mặt, vai trò, chứng chỉ.
- **Công việc làm lại** `POST /cong-viec/{id}/lam-lai` (người kiểm tra của 014; FR-026a): căn cứ là kết quả kiểm tra Không đạt, 005 hỏi `KetQuaKiemTraPort.khongDat(ketQuaId, congViecGocId)`; cùng loại, cùng người, mức bằng công việc gốc; thời điểm trong ca của danh sách kiểm tra.
- **Kiểm tra đầu ca, giám sát** (FR-029, BR-M04-24): hai loại công việc chuyên môn của danh mục, gắn tầng/khu và điều dưỡng, không gắn người cao tuổi; khung bằng cả ca. Người phụ trách ca lập trong lô như các công việc khác.
- **Người thực hiện được lưu theo lệnh**; đổi phân công giữa ca thì gán lại theo C-10.

**Lý do**: 1.3 và Q-306, Q-307: mọi công việc do actor lập; hệ thống kiểm tra và chặn. Kiểm tra theo lô trả đủ lỗi một lần giúp người lập sửa nhanh ở giờ giao ca (NFR-05 khung 06:00–08:00).

**Phương án đã cân nhắc**:
- Mỗi công việc một lệnh: hàng trăm lệnh mỗi ca, chậm và không "hoặc toàn bộ, hoặc không" cho một lần lập.
- Màn hình "chép từ kế hoạch" điền sẵn công việc: trái Q-307.

---

## C-08. Vòng đời công việc và hủy tự động (FR-026 → FR-028, FR-010, FR-019, FR-020, FR-020a, FR-022, FR-027)

**Quyết định**:
- **Cột thời gian** trên `cong_viec`: `thoi_diem_du_kien`, `dau_khung`, `cuoi_khung` (tính một lần lúc lập: dự kiến ± khung), `den_han_luc`, `qua_han_luc`, `dong_luc`.
- **Bảng chuyển** cài thành hằng bảng `BangChuyenCongViec` đúng bảng FR-026; chuyển ngoài bảng trả 409 `CHUYEN_KHONG_HOP_LE`. Trigger `trg_cong_viec_trang_thai_cuoi` chặn `UPDATE` cột trạng thái, kết quả, người đóng khi trạng thái cũ là cuối (FR-028).
- **Người đóng** (FR-028): `dong_boi_loai` (`NHAN_VIEN` | `HE_THONG`), `dong_boi_id`, `can_cu_quyen` (`DUOC_GIAO` | `VAI_TRO_TRUONG_TANG` | `NHIEM_VU_PHU_TRACH_CA`), `quy_tac_dong` (`FR_010_TRANG_THAI_CUOI` | `FR_047_SAU_BAN_GIAO` | `HUY_VANG_MAT` | `HUY_DA_VE` | `HUY_NGUON` ). 016 tách công việc do hệ thống đóng theo hai cột này.
- **Hủy tự động** (chỉ từ Chưa đến hạn) là tác động kèm lệnh nguồn, trong transaction của lệnh đó:

  | Nguồn | Đường gọi | Phạm vi hủy | Lý do |
  | --- | --- | --- | --- |
  | Cho tạm vắng, Chuyển viện (001), Điểm danh rời viện (014 qua 001) | Bean `ThamGiaChuyenTrangThai` của 005 | Chưa đến hạn có `thoi_diem_du_kien >= thời điểm rời` | `VANG_MAT` |
  | Điểm danh về bán trú | Lệnh của 005 | Chưa đến hạn sau giờ về | `DA_VE` |
  | Hủy bữa, hủy đăng ký buổi, ngừng lịch đo, bữa bỏ vì vắng | 007, 011, 014 gọi `NguonCongViecApi.nguonBiHuy(loai, id, lyDo)` (MANDATORY) | Chưa đến hạn có căn cứ đó | `NGUON_BI_HUY` |
  | Trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận) | Bean của 005 | Chưa đến hạn → Hủy; Đến hạn, Quá hạn → Không thực hiện; phát `YeuCauDongCanhBao` cho 007 với các công việc có cảnh báo | `TRANG_THAI_CUOI` |

- **Trở về Đang lưu trú** (Ghi nhận trở về, Điểm danh về chuyến đi): bean chỉ phát `CanLapCongViec` (FR-020). Không tạo, không mở lại công việc đã Hủy.
- **Đính chính chuyến đi** (FR-020a): 014 gọi `CongViecVangMatApi.tinhLai(nctId, khoangCu, khoangMoi)` (MANDATORY). Công việc Chưa đến hạn nay nằm trong khoảng đi đúng thì Hủy `VANG_MAT`; công việc đã Hủy vì vắng nay nằm ngoài khoảng và chưa tới thời điểm dự kiến thì chỉ phát `CanLapCongViec`. Công việc đã đóng không đổi.
- **Kế hoạch hoặc lịch đo đổi nội dung** (FR-022): không hủy; phát `KeHoachDaDoi` hoặc nghe `LichDoDaDoi` (007) rồi phát `NguonCongViecDaDoi` cho 009. Trưởng tầng hủy bằng lệnh `POST /cong-viec/{id}/huy` (bắt buộc lý do; Người phụ trách ca không có lệnh này, Q-35).
- **Đến hạn, Quá hạn khi người cao tuổi rời đi tạm thời** (FR-027): giữ nguyên, nhắc và leo thang vẫn chạy; người thực hiện hoặc Trưởng tầng đóng Không thực hiện có lý do.
- **Bàn giao chưa xác nhận lúc người cao tuổi chuyển trạng thái cuối** (FR-010 câu cuối): 008 đọc trạng thái hiện hành của công việc qua `CongViecBanGiaoApi.trangThaiHienHanh(ids)`, nên mục bàn giao tự hiển thị "đã đóng, trạng thái cuối"; FR-047 bỏ qua công việc đã đóng.

**Lý do**: Bảng FR-026 và 8.3 là căn cứ; mọi hủy, đóng tự động là tác động kèm lệnh của actor hoặc chuyển theo giờ, đúng giới hạn của Q-306.

---

## C-09. Ghi nhận kết quả (FR-033 → FR-037, FR-040a, FR-041, BR-M04-12, 14)

**Quyết định**:
- **Bảng** `ket_qua_ghi_nhan` (nhóm 3, chỉ thêm): `cong_viec_id` (`UNIQUE` – mỗi công việc một kết quả gốc), `loai_ket_qua`, giá trị theo loại (`gia_tri_so`, `gia_tri_ma`, `gia_tri_json`), `thoi_diem_thuc_hien`, `thoi_diem` (máy chủ nhận), `thoi_diem_thiet_bi`, `nhan_ghi_muon`, `nhan_dong_bo_sau_mat_quyen`, `ghi_chu`, `ban_ghi_chi_so_id`, `can_cu_quyen`, `tang_khu_vuc_id`. Lần "Không thực hiện" không có dòng kết quả; lý do nằm ở công việc và nhật ký (FR-026: không tham gia quy tắc mục F).
- **Kiểm tra giá trị theo loại** (FR-034) bằng `KiemTraKetQua` theo `loai_ket_qua` của loại công việc: danh sách mã cố định cho ăn uống (`AN_HET`, `PHAN_LON`, `MOT_PHAN`, `KHONG_AN`, `BO_BUA`) và vận động; ml nguyên dương, vượt `gioi_han_tren` thì 422 `CAN_XAC_NHAN_LAI` trừ khi gửi `xacNhanVuotGioiHan = true`; chất lượng ngủ, tâm trạng, loại hành vi lấy từ danh mục nhóm 1 `danh_muc_gia_tri_ket_qua` (có nhóm `MAT_NGU`, `TIEU_CUC` cho 007 FR-039c và BR-M04-11).
- **Lệnh ghi** `POST /cong-viec/{id}/ghi-nhan` (hoàn thành) và `/khong-thuc-hien`:
  - quyền theo FR-033: được giao; Trưởng tầng của tầng; Người phụ trách ca chỉ khi công việc Quá hạn và trong phạm vi ca (`QuyenApi` của 002 trả `canCu`). Người phụ trách ca với công việc chưa Quá hạn dùng `POST /cong-viec/{id}/nhan-viec` rồi ghi như người được giao;
  - thời điểm thực hiện mặc định là thời điểm ghi; không sau thời điểm ghi, không trước `dau_khung` (FR-035);
  - trạng thái mới: Đến hạn → Hoàn thành; Quá hạn → Hoàn thành nếu `thoi_diem_thuc_hien <= cuoi_khung`, ngược lại Hoàn thành trễ (bắt buộc lý do với Quan trọng, Bắt buộc); Quá hạn đóng trong khung thì phát `YeuCauDongCanhBao(lyDo = DA_THUC_HIEN_DUNG_KHUNG)`;
  - nhãn ghi muộn: `(thoi_diem_thiet_bi ?? thoi_diem) − thoi_diem_thuc_hien > CFG-M04-06`.
- **Ăn uống có suất đặc biệt hoặc cần đối chiếu** (FR-040a): trước khi ghi, hỏi `NguonCongViecPort` của 011: `xacNhanPhucVu(buaId, nctId)`; chưa có thì 409 `CHUA_XAC_NHAN_PHUC_VU`.
- **Đo chỉ số** (FR-041, BR-M04-10): 005 gọi `ChiSoPort.ghi(nctId, loaiChiSo, giaTri, thoiDiemDo, nguoiDo, congViecId)` (007 cài, MANDATORY) trong transaction ghi nhận. Kết quả:
  - `DA_LUU(banGhiChiSoId)`: công việc đóng, trỏ bản ghi chỉ số;
  - `BI_CHAN(lyDo)` (ngoài CFG-M06-03): 422, công việc giữ nguyên;
  - `CHO_DO_LAI(phienId)`: công việc mang dấu `cho_xac_nhan_do_lai`, chưa đóng; 007 gọi `CongViecDoChiSoApi.daXacNhan(congViecId, banGhiChiSoId, thoiDiemDo)` (MANDATORY) khi lưu giá trị đã xác nhận hoặc người đo chọn "xử lý ngay".
- **Ghi nhận phát sinh** `POST /ghi-nhan-phat-sinh` (FR-025, Q-36): một transaction tạo `cong_viec` (căn cứ `GHI_NHAN_PHAT_SINH`, trạng thái `HOAN_THANH`, không khung) và `ket_qua_ghi_nhan`. Kiểm tra: vai trò của loại khớp vai trò người ghi; chứng chỉ; thời điểm thực hiện không sau thời điểm ghi, trong khoảng có mặt (001 `khoangTrangThai`, bán trú sau giờ điểm danh đến), trong phạm vi ca của người ghi. Nếu có công việc cùng loại Đến hạn hoặc Quá hạn của người đó, phản hồi `200` lần đầu trả `congViecCungLoai[]` khi client chưa gửi `daXemCongViecCungLoai = true` (hiển thị, không chọn thay).
- **Hai lần ghi đồng thời** (FR-036): theo C-03.
- **Đính chính** (FR-037, BR-M04-14): `DinhChinhDuocProvider` `M04.KET_QUA_GHI_NHAN`:
  - `tangKhuVuc` = tầng/khu của công việc; cách A cho người ghi gốc; cách B cho Trưởng tầng, Người phụ trách ca của tầng trong phạm vi ca (000 mục 4, 002 FR-019b);
  - kiểu `SUA_NOI_DUNG` và `HUY_GHI_NHAN`; công việc giữ trạng thái;
  - `sauKhiDinhChinh`: nếu loại có tính phí thì phát `KetQuaTinhPhiDaDinhChinh` cho 010 (DBR-17); nếu kết quả đo chỉ số thì không cho đính chính ở 005 (`choPhep` trả sai; đính chính bản ghi chỉ số ở 007).
- **Giá trị hiện hành** cho quy tắc mục F lấy qua `DinhChinhApi.hienHanh` (gồm cờ `daHuyGhiNhan`).

**Lý do**: Nhóm 3 theo 000 R-05; một bản kết quả gốc mỗi công việc giữ "đóng đúng một lần".

---

## C-10. Checklist, công việc chung, nhận việc, phân lại (FR-024, FR-029 → FR-032, FR-047a, BR-M04-13, 28)

**Quyết định**:
- **Checklist** `GET /checklist?caId=` dựng từ: công việc có `nguoi_thuc_hien_id = tôi` và `ca_id` của ca, công việc mang dấu tồn (`ton_tu_ca_truoc`), công việc chung của tầng/khu mà vai trò và chứng chỉ của tôi phù hợp. Điều dưỡng có thêm phần đọc từ nguồn: liều thuốc (`LieuThuocChecklistPort`, 006), việc nhận/lập bàn giao (`NhanSuCaPort.viecBanGiao`, 008), cảnh báo đang mở (đọc từ 007 qua `CanhBaoDocPort`). Thẻ người cao tuổi gồm vị trí (`ViTriApi.hienTai`, 003) và lưu ý chăm sóc (`DanhGiaApi.luuYHienHanh`, 001). Sắp theo thời điểm dự kiến, Quá hạn lên đầu (FR-030).
- **Phạm vi hiển thị** từ đầu phạm vi dữ liệu của ca (`batDauCa − CFG-M15-07`, 002 FR-032).
- **Hiệu năng** (SC-008, NFR-02 ≤ 3 giây): một truy vấn có chỉ mục `(ca_id, nguoi_thuc_hien_id)` và `(tang_khu_vuc_id, ca_id, nguoi_thuc_hien_id IS NULL)`; vị trí và lưu ý gọi theo lô một lần; cỡ trả về khoảng 15 người × 20 công việc.
- **Nhận việc** `POST /cong-viec/{id}/nhan-viec`: công việc chung chưa đóng; vai trò, chứng chỉ phù hợp; nhân viên có ca trùng thời điểm dự kiến. **Phân lại** `POST /cong-viec/{id}/phan-lai` (Trưởng tầng; Người phụ trách ca trong phạm vi ca). Mỗi lần ghi dòng `lich_su_xu_ly_cong_viec` (nhóm 3: người, căn cứ, trước, sau, thời điểm, lý do).
- **Thành công việc chung** (`nguoi_thuc_hien_id = NULL`) là tác động kèm lệnh của 008 hoặc 015, gọi `CongViecCaApi` (MANDATORY):
  - `khiVangCa(nhanVienId, caId)`, `khiNghiCoDuyet(nhanVienId, caIds)`: chỉ công việc chưa đóng của ca đó (Q-91);
  - `khiNghiViec(nhanVienId, tuThoiDiem)`: ca hiện tại và mọi ca tương lai;
  - `khiDoiPhanCong(nctId, caId)`, `khiKetThucPhanCong(phanCongId)` (đổi ca, 008 FR-027): gán lại cho người được phân công mới khớp vai trò, hoặc thành chung (FR-024, BR-M04-28);
  - mỗi lần thành chung mà ca không có ai đủ điều kiện (`NhanSuCaPort.coNguoiDuDieuKien`) thì phát `CongViecChungKhongAiDuDieuKien` mức Trung bình cho Trưởng tầng (Edge Case CHK037). Lần nào cũng phát `CongViecThanhChung` cho Trưởng tầng (FR-032).
- **Chuyển giường sang khu khác** (FR-032 câu cuối, 003 FR-031): listener `DaChuyenGiuong` (003, sau commit, idempotent theo `phanBoMoiId`) gán lại công việc chưa đóng theo phân công của khu mới.
- **Tồn chờ xác nhận bàn giao** (FR-047a, BR-M09-08): job theo phút, tại `batDauCa` của ca sau, hỏi `NhanSuCaPort.banGiaoDaXacNhan(caTruocId)`; nếu chưa, công việc chưa đóng mà người được giao thuộc ca trước và không có tên trong ca nào đang diễn ra thành chung, dấu `TON_CHO_XAC_NHAN`.

**Lý do**: Gán lại là tác động kèm lệnh của 008, đúng ranh giới module; checklist là truy vấn đọc, không lưu bản sao.

---

## C-11. Quy tắc mục F (FR-039, FR-040, FR-042, BR-M04-08, 09, 11)

**Quyết định**: Ba hàm thuần có test bảng; dữ liệu vào đọc lúc đánh giá, không có bảng trạng thái chuỗi.

- **`DanhGiaNuocUong`** (FR-039): chạy một lần mỗi ngày tại giờ của CFG-M04-03 (job theo phút, `lan_chay_m04 (NUOC_UONG, ngay)`):
  - mục tiêu: mục kế hoạch nhóm "hỗ trợ uống nước" có `muc_tieu_ml` của phiên bản Hiệu lực ngày đó; không có thì bỏ qua;
  - chỉ xét người đang có mặt tại mốc (không vắng; bán trú `CO_MAT`);
  - giờ có mặt từ 00:00 tới mốc, tính tới phút: nội trú trừ các khoảng Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài (`khoangTrangThai` của 001); bán trú tính từ giờ điểm danh đến;
  - mục tiêu điều chỉnh = `floor(mucTieu × phutCoMat / phutTu0hToiMoc)` ml; so tổng ml hiện hành (sau đính chính, bỏ kết quả đã hủy ghi nhận, gồm Ghi nhận phát sinh) với tỷ lệ của CFG-M04-03;
  - dưới ngưỡng thì phát `YeuCauCanhBao(UONG_NUOC_THIEU, NHE)` cho 007. Không tạo công việc (Q-307).
- **`DemChuoiBuaKem`** (FR-040): chạy khi một kết quả ăn uống được ghi hoặc đính chính. Lấy các bữa của người đó theo thời điểm dự kiến giảm dần (qua các ngày); bỏ công việc Hủy, Không thực hiện, kết quả đã hủy ghi nhận; đếm liên tiếp `MOT_PHAN`, `KHONG_AN`, `BO_BUA` tới khi gặp kết quả khác. Chuỗi ≥ CFG-M04-04 thì phát `YeuCauCanhBao(AN_KEM_KEO_DAI, NHE, nguoiNhan = điều dưỡng phụ trách)` cho 007 (007 gộp theo BR-M05-02) và `AnKemKeoDai(nctId, buaIds)` cho 011. Bữa ghi muộn của một thời điểm sớm hơn được đếm theo thời điểm dự kiến; hàm xét chuỗi kết thúc ở bữa mới nhất.
- **`DemNgayTamTrang`** (FR-042): chạy khi kết quả tâm trạng hoặc hành vi được ghi. Đếm ngày dương lịch liên tiếp tới ngày của kết quả có ít nhất một kết quả nhóm `TIEU_CUC` hoặc hành vi bất thường "Có"; ngày không có ghi nhận làm đứt chuỗi. Đủ CFG-M04-05 thì phát `YeuCauCanhBao(TAM_TRANG_KEO_DAI, NHE)`; không đính kèm hoạt động (Q-307).
- Tham số đọc lúc đánh giá (000 FR-018); đổi CFG-M04-03 trong ngày áp từ lần kế tiếp (Edge Case).

**Lý do**: Tính lại từ dữ liệu gốc bảo đảm đúng sau đính chính, Hủy ghi nhận và ghi muộn (SC-007), không phải giữ đồng bộ một bộ đếm.

**Phương án đã cân nhắc**: Bảng bộ đếm chuỗi cập nhật mỗi lần ghi: sai khi kết quả bị đính chính hoặc ghi muộn cho bữa cũ.

---

## C-12. Bộ lập lịch của 005 (FR-003, FR-013, FR-017, FR-019, FR-039, FR-043, FR-044, FR-047a, FR-052, NFR-04)

**Quyết định**: Hai job có ShedLock, chạy cả khi khởi động (chạy bù).

| Job | Chu kỳ | Việc |
| --- | --- | --- |
| `ViecTheoPhutM04` | Mỗi phút | (1) Chưa đến hạn → Đến hạn khi `dau_khung <= bây giờ`; (2) Đến hạn → Quá hạn khi `cuoi_khung <= bây giờ`, kèm nhắc (FR-043), báo Trưởng tầng và Người phụ trách ca với Quan trọng, yêu cầu cảnh báo Trung bình với Bắt buộc (FR-044 b, c); (3) công việc Thường Quá hạn chưa đóng sau CFG-M04-12 và chưa báo: báo Người phụ trách ca (FR-044 a); (4) Vắng không báo (FR-013); (5) mốc kiểm tra nước (FR-039); (6) tại giờ bắt đầu ca: nhắc ca chưa có công việc (FR-017), tồn chờ xác nhận bàn giao (FR-047a) |
| `ViecHangNgayM04` | 00:00 và khi khởi động | (1) Chờ hiệu lực → Hiệu lực (FR-003, FR-006); (2) xác định trạng thái có mặt bán trú của ngày (C-06); (3) báo bán trú còn Có mặt hết ngày trước (FR-019); (4) nhắc phiên bản Chờ duyệt (FR-005, CFG-M15-05, 06) |

- **Một lần**: chuyển trạng thái xét `trang_thai` dưới khóa dòng nên chạy lại không chuyển hai lần; các mốc theo ngày hoặc theo ca dùng bảng `lan_chay_m04 (viec, moc)` khóa chính hai cột; báo leo thang có cột `da_bao_nptc_luc`, `da_bao_leo_thang_luc`.
- **Chạy bù** (FR-052): mọi việc (1) → (3) của job theo phút xét mọi công việc đã qua mốc, thời điểm chuyển ghi là lúc chạy thật, kèm mốc dự kiến (`den_han_luc`, `qua_han_luc` là lúc chạy; `dau_khung`, `cuoi_khung` giữ mốc dự kiến). Nhắc bị dồn (một lần chạy có nhiều công việc cùng người nhận mà mốc cũ hơn hai chu kỳ) gộp thành một sự kiện `NhacQuaHanGop(nguoiNhan, congViecIds)`. Mốc nước chỉ chạy nếu còn trong ngày; nhắc ca chưa có công việc chỉ gửi nếu ca còn diễn ra; phiên bản Chờ hiệu lực chuyển với ngày hiệu lực giữ nguyên.
- **Hiệu năng** (NFR-04 ≤ 5 phút, SC-005 ≤ 1 phút): mỗi việc là một truy vấn có chỉ mục `(trang_thai, dau_khung)`, `(trang_thai, cuoi_khung)`, `(trang_thai, muc_quan_trong, da_bao_nptc_luc, qua_han_luc)`; mỗi phút ở quy mô 300 người chỉ vài chục dòng; mỗi công việc chuyển trong một transaction ngắn (khóa một dòng), để một lỗi không chặn cả lô.
- Người thực hiện ghi ở nhật ký là Bộ lập lịch, kèm căn cứ (000 FR-045).

**Lý do**: SC-005 đòi chuyển và nhắc trong 1 phút sau khi hết khung; một job theo phút đủ và đơn giản.

**Phương án đã cân nhắc**: Lên lịch riêng cho từng công việc (timer trong bộ nhớ): mất khi khởi động lại, không chạy bù được.

---

## C-13. Sự kiện và thông báo

**Quyết định**: Mọi yêu cầu tới 007, 009, 010, 011, 016 là sự kiện ghi cùng transaction qua outbox của 000 (R-09): lệnh rollback thì sự kiện không có, lệnh commit thì sự kiện chắc chắn được giao ("hoặc toàn bộ, hoặc không", FR-050). Mỗi sự kiện mang khóa `loại + id + mốc` để bên nghe bỏ trùng khi phát lại (NFR-04). Ngoại lệ là `ChiSoPort` (đồng bộ, C-09) vì kết quả lưu chỉ số quyết định trạng thái công việc.

Mức thông báo (spec, 009 FR-043b; dòng có dấu * là plan áp nguyên tắc 3 bậc, xem plan điểm báo lại):

| Mức | Thông báo |
| --- | --- |
| Trung bình | báo leo thang Thường, Quan trọng (FR-044 a, b); vắng ca, nghỉ việc, công việc thành chung (FR-032); phiên bản Chờ duyệt quá hạn (FR-005); bán trú còn Có mặt lúc hết ngày cho Người phụ trách ca (FR-019); công việc chung không ai đủ điều kiện (Edge Case CHK037); ca chưa có công việc lúc bắt đầu ca* |
| Nhẹ | nhắc quá hạn cho người thực hiện (FR-043); bán trú còn Có mặt cho Trưởng tầng (FR-019); xem xét kế hoạch (FR-011)*; kế hoạch đã đổi, nguồn đổi (FR-006, FR-022)*; cần lập công việc khi điểm danh đến, trở về, Hoàn tất tiếp nhận (FR-017, FR-018, FR-020)*; Vắng không báo (FR-013)*; phiên bản gửi duyệt, trả lại, thu hồi* |

Sự kiện yêu cầu 007:
- `YeuCauCanhBao(loai, muc, nctId, nguon)` với loại `CONG_VIEC_BAT_BUOC_QUA_HAN` (Trung bình), `UONG_NUOC_THIEU`, `AN_KEM_KEO_DAI`, `TAM_TRANG_KEO_DAI` (Nhẹ);
- `YeuCauDongCanhBao(congViecId, lyDo)` với lý do `DA_THUC_HIEN_DUNG_KHUNG`, `DA_DONG`, `TRANG_THAI_CUOI`.

Gửi 010: `KetQuaCoTinhPhi`, `KetQuaTinhPhiDaDinhChinh`, `TrangThaiCoMatDaXacDinh`, `TrangThaiCoMatDaDoi`. Gửi 011: `AnKemKeoDai`, `TrangThaiCoMatDaDoi`. Gửi 016: không cần sự kiện riêng; 016 đọc `CongViecThongKeApi` (module-interfaces mục 1.9).

**Lý do**: Constitution XIV; mẫu 001 H-14, 003 G-12, 004 T-12.

**Phương án đã cân nhắc**: Gọi đồng bộ SPI của 007 để tạo cảnh báo: 007 làm plan sau, và lỗi của 007 sẽ chặn lệnh ghi nhận tại giường, trái mục tiêu của XIV.

---

## C-14. Ngoại tuyến (FR-035, FR-036, BR-M04-27, Q-01, BR-M15-20)

**Quyết định**:
- **Phạm vi**: chỉ ghi nhận thực hiện (8.6): ghi nhận hoàn thành, không thực hiện, Ghi nhận phát sinh. Lệnh lập, hủy, phân lại, điểm danh bán trú, kế hoạch chỉ trực tuyến.
- **Hai `NgoaiTuyenHandler`** (002 A-11): `M04.KET_QUA_CONG_VIEC` và `M04.GHI_NHAN_PHAT_SINH`. `khoaQuyen` = `CHECKLIST_GHI_NHAN, THUC_HIEN`; `apDung` gọi cùng service với lệnh trực tuyến, `thoi_diem_thiet_bi` là "thời điểm ghi" cho nhãn ghi muộn (BR-M04-12).
- **Công việc đã đóng hoặc Hủy khi đồng bộ**: `TU_CHOI` kèm `dongBoi` và nội dung để app giữ ở mục "bị từ chối" với hai lệnh: tạo đính chính (nếu là bản ghi khác cho cùng việc) hoặc chuyển thành Ghi nhận phát sinh.
- **Công việc bị hệ thống đóng vì trạng thái cuối** (CHK056): nếu `thoi_diem_thuc_hien` trước thời điểm trạng thái cuối và quyền tại thời điểm thiết bị đạt, handler ghi như Ghi nhận phát sinh trỏ `cong_viec_goc_id`; sau thời điểm đó thì `TU_CHOI`.
- **Đo chỉ số ngoại tuyến**: handler gọi `ChiSoPort.ghi` lúc đồng bộ; `BI_CHAN` thì `TU_CHOI`; `CHO_DO_LAI` thì chấp nhận, công việc chờ xác nhận.
- **App nhân viên**: drift (000 R-13) lưu checklist tải gần nhất kèm `taiLuc`, hiển thị "chưa cập nhật từ <thời điểm>"; hàng đợi bản ghi có `Idempotency-Key`; sau đồng bộ hiển thị kết quả từng bản ghi (BR-M15-20).

**Lý do**: Q-01, constitution XIV; khung chung của 000, 002 đã có, 005 chỉ cài handler.

---

## C-15. Danh mục loại công việc và thời khóa biểu (FR-038, FR-048)

**Quyết định**:
- `loai_cong_viec` (nhóm 1, mẫu 000 mục 1.1): `nhom` (enum theo FR-038, gồm `UONG_THUOC` chỉ để chặn, `KIEM_TRA_DAU_CA`, `GIAM_SAT`), `loai_ket_qua`, `vai_tro_mac_dinh`, `muc_mac_dinh`, `khung_truoc`, `khung_sau` (rỗng thì CFG-M04-02), `co_tinh_phi`, `dich_vu_id` (004), `gioi_han_tren`, bảng con `loai_cong_viec_chung_chi`. Quản lý viện cấu hình (quyền C, spec Assumptions).
- Sửa loại chỉ áp cho mục, công việc tạo sau (giá trị được chép vào mục và công việc lúc tạo); riêng chứng chỉ yêu cầu đọc trực tiếp từ loại ở mỗi lần giao, nhận, phân lại (FR-038, BR-M09-01).
- **Thời khóa biểu** `GET /nguoi-cao-tuoi/{nctId}/thoi-khoa-bieu?ngay=` gộp: công việc của 005; liều thuốc (006), buổi hoạt động (014), lịch thăm (012) qua `ThoiKhoaBieuNguonPort` (mỗi module một bean, trả mục có thời điểm, nguồn, trạng thái); lịch đến/về bán trú của 005. Chỉ đọc (FR-048).

---

## C-16. Quyền

**Quyết định**: Khóa quyền theo dòng Phụ lục 27, 19.3 là căn cứ khi khác (002 A-04):

| Lệnh | Khóa | Thu hẹp ở 005 |
| --- | --- | --- |
| Lập, cập nhật, gửi duyệt, hủy phiên bản | `KE_HOACH_CHAM_SOC, THUC_HIEN` | Điều dưỡng, trong phạm vi |
| Duyệt, trả lại, thu hồi | `KE_HOACH_CHAM_SOC, DUYET` | Bác sĩ; Điều dưỡng được gán thêm (Q-15) |
| Xem kế hoạch | `KE_HOACH_CHAM_SOC, XEM` | CS chỉ phiên bản Hiệu lực trong phạm vi (P); Hành chính, Bếp, Người thân không (FR-007) |
| Lập công việc của ca | `LAP_CONG_VIEC_CA, THUC_HIEN` | Nhiệm vụ Người phụ trách ca của ca (thời gian ca); Trưởng tầng của tầng được giao. Điều dưỡng không giữ nhiệm vụ: không có lệnh này (19.3) |
| Tạo công việc phát sinh | `LAP_CONG_VIEC_CA, THUC_HIEN` | Trưởng tầng, Điều dưỡng (FR-025) |
| Checklist, ghi nhận, Ghi nhận phát sinh, nhận việc | `CHECKLIST_GHI_NHAN, THUC_HIEN` | TT, ĐD, CS; phạm vi ca; ĐD làm được việc của CS (Q-214) |
| Xem kết quả ghi nhận (Bác sĩ) | `CHECKLIST_GHI_NHAN, XEM` | Bác sĩ xem mọi kết quả, không checklist, không lệnh (FR-049, chú thích ²²) |
| Xử lý việc quá hạn, hủy, phân lại | `XU_LY_VIEC_QUA_HAN, THUC_HIEN` | Trưởng tầng; Người phụ trách ca theo chú thích ⁹ (chỉ Quá hạn, trừ hủy) |
| Điểm danh bán trú | `DIEM_DANH_BAN_TRU, THUC_HIEN` | HC, CS, ĐD (²⁷); TT, QL xem |
| Danh mục loại công việc | `LOAI_CONG_VIEC, CAU_HINH` | Quản lý viện |
| Xem việc quá hạn toàn viện | `XU_LY_VIEC_QUA_HAN, XEM` | Quản lý viện (FR-045) |

Kết quả ghi nhận, lưu ý chăm sóc mang `@NhomTruong(SUC_KHOE)` theo 002 A-08; Bếp, Hành chính không thấy (Hành chính chỉ thấy trạng thái có mặt).

---

## C-17. Lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Lưu giữ** (000 R-11): mọi bảng gắn người cao tuổi (`ke_hoach_cham_soc`, `muc_ke_hoach`, `cong_viec`, `ket_qua_ghi_nhan`, `lich_su_xu_ly_cong_viec`, `co_mat_ban_tru`, `diem_danh_ban_tru`) đăng ký `LuuTruProvider` loại `THEO_HO_SO`. Công việc kiểm tra đầu ca, giám sát (không gắn người) theo `KHONG_GAN_HO_SO`. Danh mục không bị loại bỏ.
- **Nhật ký**: hành động `M04_KE_HOACH_*`, `M04_CONG_VIEC_LAP`, `M04_CONG_VIEC_CHUYEN`, `M04_CONG_VIEC_DONG`, `M04_CONG_VIEC_HUY`, `M04_PHAN_LAI`, `M04_NHAN_VIEC`, `M04_GHI_NHAN`, `M04_DIEM_DANH_*`, `M04_LOAI_CONG_VIEC_*`, `M04_NHAC`, `M04_BAO`; dấu "chạy bù" khi có.
- **Kiểm thử** (000 R-14):
  - hàm thuần có test bảng: `ChuoiThoiDiemMuc`, `KiemTraLapCongViec`, `BangChuyenCongViec`, `XacDinhTrangThaiDong` (Hoàn thành / Hoàn thành trễ / nhãn ghi muộn), `DanhGiaNuocUong`, `DemChuoiBuaKem`, `DemNgayTamTrang`, `SoSanhPhienBan`;
  - Test tích hợp MySQL: trigger mục kế hoạch, trigger trạng thái cuối công việc, cột sinh `UNIQUE` (DBR-11, DBR-12, một phiên bản mở);
  - đồng thời: hai lần ghi nhận một công việc; ghi nhận và job Quá hạn cùng lúc; lập trùng hai lô;
  - đồng hồ giả lập: 7 ngày × 20 người (SC-001), leo thang CFG-M04-12, chạy bù sau 3 giờ dừng;
  - bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}` cho mọi SPI 005 khai báo (006, 007, 008, 011, 012, 014);
  - client: Vitest cho màn lập công việc (lỗi theo dòng, không có nút "chép từ kế hoạch"), checklist; `flutter_test` cho checklist ngoại tuyến, ghi nhận theo loại, danh sách kết quả đồng bộ (BR-M15-20, constitution XV).
