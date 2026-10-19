---
description: "Danh sách task triển khai feature 006 – Quản lý thuốc"
---

# Tasks: Quản lý thuốc (006)

**Input**: Tài liệu thiết kế ở `specs/006-medication-management/` (plan.md, spec.md, research.md D-01 → D-20, data-model.md, contracts/api-006.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001 (`MucSucKhoeApi`, `DiNguyenApi`, `HoSoSucKhoeApi`, `BanDongYApi`, `ThamGiaChuyenTrangThai`, sự kiện dị ứng), 003 (`ViTriApi`), 004 (`MucViecSauQuaDoiPort`, `DonGiaApi`), 005 (`LieuThuocChecklistPort`, `ThoiKhoaBieuNguonPort`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo data-model.md mục 14 và quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M07: `BE/m07/{api,danhmuc,donthuoc,lichthuoc,lieu,prn,doichieu,thuocgui,mangtheo,tiepnhanthuoc,trangthai,lich}/`
- Web `frontend/src/tinh-nang/{danh-muc-thuoc,don-thuoc,lich-thuoc,doi-chieu-thuoc,thuoc-gia-dinh-gui,thuoc-mang-theo,cong-nguoi-than/thuoc}/`; app nhân viên `mobile/lib/nhan-vien/{vong-phat-thuoc,xac-nhan-lieu,dong-bo,thuoc-mang-theo,thuoc-gia-dinh-gui,xem-lai-lieu}/`; app người thân `mobile/lib/nguoi-than/thuoc/`

---

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; 009 FR-001a): sửa T004. Mã task cũ giữ nguyên.

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/m07/{api,danhmuc,donthuoc,lichthuoc,lieu,prn,doichieu,thuocgui,mangtheo,tiepnhanthuoc,trangthai,lich}/` với `package-info.java` (research D-01)
- [ ] T002 [P] Tạo bản giả SPI 006 khai báo trong `BT/support/gia006/`: `DieuDuongCaPort` (008), `ChuyenDiThuocPort` (014), `DeNghiMuaHoPort` (010); công tắc `/api/v1/kiem-thu/gia-lap/{module}` (contracts mục 2.1)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m07.api`, sự kiện, danh mục thuốc, bảng khóa theo người, khung hai job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 [P] Tạo giao diện công khai `BE/m07/api/`: `DonThuocApi`, `LichThuocApi`, `LieuBanGiaoApi`, `MangThuocChuyenDiApi`, `ThuocGiaDinhGuiApi`, `ThuocCongNguoiThanApi`, `ThuocThongKeApi` (contracts mục 1.2 → 1.8) và SPI `DieuDuongCaPort`, `ChuyenDiThuocPort`, `DeNghiMuaHoPort` (mục 2.1) (research D-02)
- [ ] T004 [P] Tạo sự kiện `BE/m07/api/sukien/` theo contracts mục 1.9 (yêu cầu cảnh báo, nhắc, thông báo, `SuKienTinhPhiThuoc` mang loại nguồn và id); sự kiện gửi 009 cài `CoYeuCauThongBao` (research D-17); bổ sung 2026-10-18 (009 FR-001a): sự kiện có người nhận là người thân khai `doiTuongThongTin` theo research D-17, test hợp đồng kiểm mọi yêu cầu tới người thân có đối tượng
- [ ] T005 Tạo `MIG/V006_01__nen_m07.sql`: `thuoc_nct` (`nct_id` khóa chính; `han_doi_chieu`; `ly_do_doi_chieu ENUM('TIEP_NHAN','TRO_VE_BENH_VIEN') NULL` (chỉ hai lý do có hạn; phiếu theo yêu cầu chuyên môn không dùng, Q-340); `da_canh_bao_doi_chieu_luc`; `phien_ban`), `lan_chay_m07 (viec, moc)` khóa chính hai cột, `thuoc` (mẫu nhóm 1; `UNIQUE (ten, ham_luong, dang_bao_che)`; `cua_so_truoc_phut`, `cua_so_sau_phut SMALLINT NULL` – rỗng dùng CFG-M07-01; `kiem_soat_dac_biet BOOLEAN`), `thuoc_hoat_chat (thuoc_id, di_nguyen_id)` khóa chính hai cột (data-model mục 2, 3)
- [ ] T006 Cài danh mục thuốc (AC-01; tạo, sửa, ngừng hiệu lực; xóa bị chặn khi có đơn bằng `ON DELETE RESTRICT`; dấu "chưa có mục vật phẩm" qua `DonGiaApi.thuocNguonVien`) trong `BE/m07/danhmuc/internal/ThuocService.java` và `BE/m07/danhmuc/web/ThuocController.java` (`/thuoc`, `/{id}`, `/{id}/ngung-hieu-luc`) (FR-001 → FR-003; research D-04)
- [ ] T007 Cài `BE/m07/trangthai/internal/KhoaThuoc.java` (khóa `thuoc_nct` theo research D-03) và khung job `BE/m07/lich/ViecTheoPhutM07.java` (mỗi phút, ShedLock, chạy bù) và `ViecHangNgayM07.java` (`lan_chay_m07`) – việc đăng ký ở các story (research D-19)
- [ ] T008 [P] Cài provider cho 000/002 trong `BE/m07/trangthai/internal/`: `PhanLoaiTruongProvider` (`SUC_KHOE` cho thuốc, hoạt chất, liều, đường dùng, ghi chú, phản ứng, lý do, vấn đề, dòng phiếu, tên thuốc gửi), `LuuTruProvider` theo data-model mục 13, `GanPhamViProvider`, `QuyenTepProvider` (`M07.ANH_DON_THUOC`, `M07.ANH_CAN_CU`, `M07.ANH_THUOC_GUI`)
- [ ] T009 [P] Tạo màn web `frontend/src/tinh-nang/danh-muc-thuoc/`

**Checkpoint**: Nền 006 sẵn sàng.

---

## Phase 3: User Story 1 – Nhập đơn thuốc, kiểm trùng hoạt chất và dị ứng (Priority: P1) 🎯 MVP

**Goal**: Đơn định kỳ và PRN, nội bộ hoặc bên ngoài; `KiemTraDonMoi` trả vấn đề cần lý do; quyền kê; listener dị ứng mới (FR-004 → FR-013, BR-M07-05, BR-M07-06, BR-M06-05 phần 006).

**Independent Test**: A dị ứng Penicillin, đang dùng Amlodipin 5 mg; nhập Amoxicillin (xung đột dị ứng), thuốc phối hợp chứa amlodipin (trùng hoạt chất) và một đơn hợp lệ.

### Tests cho User Story 1

- [ ] T010 [P] [US1] Test đơn vị `BT/m07/BR_M07_06_TrungHoatChatDiUngTest.java` (trùng hoạt chất, xung đột dị ứng, dị ứng "khác" cần xem)
- [ ] T011 [P] [US1] Test `BT/m07/BR_M07_05_QuyenKeDonIT.java`, `FR_012_DiUngMoiIT.java`, `DonThuocTriggerIT.java`: kê đơn nội bộ chỉ Bác sĩ có giấy phép (khóa `CHUYEN_MON`); đơn bên ngoài bắt buộc cơ sở, người kê, ngày kê; vấn đề phải có lý do xác nhận; dị ứng mới ghi ở 001 tạo yêu cầu cảnh báo và báo `daKiemTra`; trigger khóa trường đơn đã lưu

### Implementation cho User Story 1

- [ ] T012 [US1] Tạo `MIG/V006_02__don_thuoc.sql`: `don_thuoc` theo data-model mục 4 (`loai_don ENUM('DINH_KY','PRN')`; `kieu_tan_suat ENUM('CAC_MOC','MOI_N_GIO','NGAY_TRONG_TUAN') NULL`; `ngay_trong_tuan SET(...)`; `khoang_cach_toi_thieu_phut CHECK (> 0)`, `so_lan_toi_da_24h CHECK (>= 1)` bắt buộc với PRN; `nguon_thuoc ENUM('VIEN','GIA_DINH_GUI')`; `co_so_ke_loai ENUM('NOI_BO','BEN_NGOAI')` với `CHECK (co_so_ke_loai='NOI_BO' OR (co_so_ke_ten IS NOT NULL AND nguoi_ke_ten IS NOT NULL AND ngay_ke IS NOT NULL))`; `trang_thai ENUM('CHO_HIEU_LUC','HIEU_LUC','TAM_DUNG','DA_NGUNG','HET_HAN')`; `ly_do_tam_dung ENUM('LENH','CHO_DOI_CHIEU') NULL`; `thay_the_cho_id` `UNIQUE`; `ly_do_ket_thuc ENUM('NGUNG','THAY_THE','HET_HAN','PHIEU_DOI_CHIEU','TRANG_THAI_CUOI') NULL`; `ghi_chu_huong_dan VARCHAR(2000) NULL`; trigger `trg_don_thuoc_khoa` và trigger trạng thái cuối; bốn chỉ mục), `don_thuoc_hoat_chat`, `van_de_don_thuoc` (`loai` `TRUNG_HOAT_CHAT|XUNG_DOT_DI_UNG|DI_UNG_KHAC_CAN_XEM`), `can_cu_co_so_ke` (nhóm 3)
- [ ] T013 [P] [US1] Tạo entity `BE/m07/donthuoc/internal/DonThuoc.java` và bảng con
- [ ] T014 [US1] Cài `BE/m07/donthuoc/internal/KiemTraDonMoi.java` (hoạt chất chép vào `don_thuoc_hoat_chat`; đối chiếu đơn Hiệu lực, Tạm dừng, Chờ hiệu lực và `MucSucKhoeApi.hieuLuc`) và `DonThuocService.java` (nhập đơn `HIEU_LUC`/`CHO_HIEU_LUC`, sửa ghi chú hướng dẫn với `If-Match`; ảnh đơn qua `TepApi`) với `BangChuyenDon` (research D-05)
- [ ] T015 [US1] Cài listener `DiUngDaGhiNhan` (idempotent theo `mucId`) trong `BE/m07/donthuoc/internal/KiemTraDiUngMoi.java`: `YeuCauCanhBao(XUNG_DOT_DI_UNG)`, `MucSucKhoeApi.daKiemTra(mucId, THUOC)` (FR-012)
- [ ] T016 [US1] Tạo `BE/m07/donthuoc/web/DonThuocController.java`: `POST/GET /nguoi-cao-tuoi/{nctId}/don-thuoc`, `POST …/don-thuoc/kiem-tra`, `GET /don-thuoc/{id}`, `POST /don-thuoc/{id}/ghi-chu`; cài `DonThuocApi`
- [ ] T017 [P] [US1] Tạo màn web `frontend/src/tinh-nang/don-thuoc/` (nhập đơn, vấn đề cần lý do)

**Checkpoint**: Nhập đơn chạy độc lập.

---

## Phase 4: User Story 2 – Lịch thuốc tính từ đơn; xác nhận từng liều trong cửa sổ (Priority: P1)

**Goal**: Lịch là hàm thuần; liều chỉ vật hóa ở đầu cửa sổ hoặc kèm lệnh; Trễ, Bỏ lỡ theo giờ; xác nhận Đã dùng/Từ chối/Không thực hiện; người phụ trách liều tính từ 008; liều chung của tầng; ngoại tuyến; từ chối liên tiếp; sự kiện tính phí (FR-018 → FR-023, FR-026 → FR-029, FR-031 → FR-033, DBR-13, BR-M07-01, 02, 12, 13, 14, 20, 21).

**Independent Test**: Đơn 08:00, 20:00 cho A; lịch hiện đúng các ngày tới; xác nhận 08:00 lúc 08:10; để 20:00 → 20:30 Trễ, 21:00 Bỏ lỡ.

### Tests cho User Story 2

- [ ] T018 [P] [US2] Test đơn vị `BT/m07/BR_M07_01_LichThuocTest.java` (ba kiểu tần suất, khoảng vắng) và `BR_M07_13_TuChoiLienTiepTest.java`
- [ ] T019 [P] [US2] Test `BT/m07/SC_001_BayNgayIT.java`, `DBR_13_LieuDuyNhatIT.java`, `BR_M07_02_TreBoLoIT.java`, `BR_M07_12_XacNhanMotLanIT.java`: 7 ngày không có dòng `lieu_thuoc` có `cua_so_tu > bây giờ` không gắn lần giao (Q-306); `UNIQUE (don_thuoc_id, thoi_diem_du_kien)`; Trễ, Bỏ lỡ (CFG-M07-02) đúng giờ với `ket_qua_lieu` `BO_LO` do Hệ thống; xác nhận lần hai bị chặn
- [ ] T020 [P] [US2] Test `BT/m07/FR_026_QuyenXacNhanIT.java`, `BR_M07_21_TangKhongDieuDuongIT.java`, `PhanUngIT.java`, `BR_M07_14_SuKienTinhPhiIT.java`: điều dưỡng được phân công; liều chung của tầng khi không xác định người phụ trách; tầng không có điều dưỡng báo đầu ca; phản ứng tạo yêu cầu cảnh báo; nguồn viện phát sự kiện tính phí
- [ ] T021 [P] [US2] Test `BT/m07/FR_029_HaiBanNgoaiTuyenIT.java`, `FR_029_DongBoMuonIT.java`: hai thiết bị ghi cùng liều ngoại tuyến → một chấp nhận, một vào xem lại; thuốc kiểm soát đặc biệt bị từ chối ngoại tuyến (BR-M07-20)

### Implementation cho User Story 2

- [ ] T022 [US2] Tạo `MIG/V006_03__lieu_thuoc.sql`: `lieu_thuoc` (`UNIQUE (don_thuoc_id, thoi_diem_du_kien)`; `trang_thai ENUM('CHUA_DEN_GIO','DEN_GIO','TRE','BO_LO','DA_DUNG','TU_CHOI','KHONG_THUC_HIEN','TAM_DUNG','MANG_THEO','DA_HUY')`; `ly_do_huy ENUM('NGUNG_DON','DOI_LIEU','HET_HAN','PHIEU_DOI_CHIEU','TRANG_THAI_CUOI') NULL`; `cho_ghi_nhan_sau_tro_ve`; trigger `trg_lieu_thuoc_trang_thai_cuoi`; năm chỉ mục), `ket_qua_lieu` (nhóm 3; `UNIQUE (lieu_thuoc_id)`; `loai ENUM('DA_DUNG','TU_CHOI','KHONG_THUC_HIEN','BO_LO')`; `can_cu_quyen ENUM('DUOC_PHAN_CONG','NGUOI_PHU_TRACH_CA','LIEU_CHUNG_TANG','MANG_THEO','GHI_THEO_BAO_LAI','HE_THONG')`; `CHECK (thoi_diem_dung <= COALESCE(thoi_diem_thiet_bi, thoi_diem))`; `ly_do` bắt buộc với `TU_CHOI`, `KHONG_THUC_HIEN`), `xem_lai_lieu` (`trang_thai` `CHO_XEM_LAI|CHAP_NHAN|CAN_DINH_CHINH`) (data-model mục 5)
- [ ] T023 [P] [US2] Cài `BE/m07/lichthuoc/internal/ChuoiThoiDiemDon.java`, `LichThuoc.java` (hàm thuần; `lieuKey = {donThuocId}-{thời điểm dự kiến}`) và `BangChuyenLieu.java` (research D-06, D-07)
- [ ] T024 [US2] Đăng ký việc theo phút: vật hóa liều ở đầu cửa sổ (`DEN_GIO`/`TAM_DUNG`/`MANG_THEO`), `DEN_GIO → TRE` (nhắc một lần), `TRE → BO_LO` sau CFG-M07-02 (thêm `ket_qua_lieu` `BO_LO`, `HE_THONG`), `TAM_DUNG` qua cuối cửa sổ; `CHO_HIEU_LUC → HIEU_LUC` của đơn; tầng không có điều dưỡng ở đầu ca (`lan_chay_m07`) (BR-M07-02, BR-M07-21)
- [ ] T025 [US2] Cài `BE/m07/lieu/internal/XacDinhNguoiPhuTrachLieu.java` (tính lúc đọc và ghi từ `DieuDuongCaPort`; liều chung của tầng; research D-09) và `XacNhanLieuService.java` (Đã dùng, Từ chối, Không thực hiện; vật hóa khi lệnh trước đầu cửa sổ; `DemTuChoiLienTiep`; phản ứng → yêu cầu cảnh báo; nguồn viện → `SuKienTinhPhiThuoc`; lô thuốc gửi trừ qua biến động) (research D-08)
- [ ] T026 [US2] Cài `NgoaiTuyenHandler` `M07.KET_QUA_LIEU` (từ chối thuốc kiểm soát đặc biệt; trùng liều → `xem_lai_lieu`) và xem lại liều trong `BE/m07/lieu/internal/` (research D-11)
- [ ] T027 [US2] Tạo `BE/m07/lichthuoc/web/LichThuocController.java` (`GET /nguoi-cao-tuoi/{nctId}/lich-thuoc`, `GET /lieu-thuoc/vong-phat-thuoc`, `GET /lieu-thuoc/{lieuKey}`) và `BE/m07/lieu/web/LieuController.java` (`POST /lieu-thuoc/{lieuKey}/da-dung`, `/tu-choi`, `/khong-thuc-hien`, `GET /xem-lai-lieu`, `POST /xem-lai-lieu/{id}/ket-qua`); cài `LichThuocApi`, `LieuThuocChecklistPort` (005), `ThoiKhoaBieuNguonPort` (005)
- [ ] T028 [P] [US2] Tạo app `mobile/lib/nhan-vien/vong-phat-thuoc/` (tải, lưu ngoại tuyến, dấu "chưa cập nhật", chặn kiểm soát đặc biệt), `xac-nhan-lieu/`, `dong-bo/`, `xem-lai-lieu/` và test `mobile/test/nhan-vien/ket_qua_dong_bo_thuoc_test.dart` (BR-M15-20), `vong_phat_thuoc_ngoai_tuyen_test.dart`
- [ ] T029 [P] [US2] Tạo màn web `frontend/src/tinh-nang/lich-thuoc/` (lịch người cao tuổi, vòng phát thuốc web)

**Checkpoint**: Phát thuốc và xác nhận chạy độc lập, kể cả ngoại tuyến.

---

## Phase 5: User Story 3 – Đổi liều bằng đơn thay thế, ngừng, gia hạn, hết hạn (Priority: P1)

**Goal**: Không sửa đơn; đổi liều tạo đơn thay thế trỏ đơn cũ trong một transaction; tạm dừng/tiếp tục/ngừng có căn cứ cơ sở kê; gia hạn; hết hạn hằng ngày, nhắc trước (FR-014 → FR-017, DBR-14, BR-M07-07, 08, 15, 16).

**Independent Test**: Amlodipin 5 mg 08:00; thử sửa thành 10 mg bị chặn; Đổi liều từ 14:00 → đơn cũ Đã ngừng, đơn mới Hiệu lực trỏ về đơn cũ, liều 08:00 hôm nay vẫn giữ.

### Tests cho User Story 3

- [ ] T030 [P] [US3] Test `BT/m07/DBR_14_DonThayTheIT.java`, `BR_M07_07_DoiLieuIT.java`, `BR_M07_15_HuyDoiLieuIT.java`, `BR_M07_16_CanCuCoSoKeIT.java`, `BR_M07_08_HetHanDonIT.java`: đổi liều ngay và tương lai; liều chưa đóng của đơn cũ → `DA_HUY`; ngừng đơn thay thế Chờ hiệu lực; đơn bên ngoài cần căn cứ cơ sở kê khi tạm dừng/ngừng/gia hạn/đổi liều; hết hạn đúng ngày và nhắc trước

### Implementation cho User Story 3

- [ ] T031 [US3] Cài các lệnh tạm dừng, tiếp tục, ngừng (ngay hoặc `ngung_tu`), đổi liều (ngay hoặc tương lai; `thay_the_cho_id`; chuyển liên kết thuốc gửi sang đơn mới), gia hạn trong `DonThuocService.java`; ghi `can_cu_co_so_ke` khi đơn bên ngoài; hủy liều đã vật hóa của đơn kết thúc (research D-05)
- [ ] T032 [US3] Đăng ký việc theo phút `ngung_tu` và việc hằng ngày `HET_HAN_DON`, `NHAC_HET_HAN_DON` (`da_nhac_het_han_luc`) (FR-017)
- [ ] T033 [US3] Bổ sung endpoint `POST /don-thuoc/{id}/tam-dung`, `/tiep-tuc`, `/ngung`, `/doi-lieu`, `/gia-han` vào `DonThuocController.java`; màn web chuỗi thay thế trong `frontend/src/tinh-nang/don-thuoc/`

**Checkpoint**: Vòng đời đơn đầy đủ.

---

## Phase 6: User Story 4 – Đối chiếu thuốc khi tiếp nhận và khi trở về từ bệnh viện (Priority: P1)

**Goal**: Đơn Tạm dừng chờ đối chiếu khi Hoàn tất tiếp nhận / Ghi nhận trở về; phiếu do Bác sĩ, Điều dưỡng lập; quy tắc hai người; hạn CFG-M07-05; phiếu lập trước tiếp nhận; đề nghị đính chính phiếu (FR-034 → FR-039, BR-M07-09, 17, 22).

**Independent Test**: A có 3 đơn Hiệu lực, chuyển viện rồi trở về với đơn ra viện 2 thuốc → 3 đơn cũ Tạm dừng, điều dưỡng được báo; lập phiếu, thử gửi khi thiếu dòng; xác nhận bởi người khác.

### Tests cho User Story 4

- [ ] T034 [P] [US4] Test `BT/m07/BR_M07_09_DoiChieuIT.java`, `BR_M07_17_DinhChinhPhieuIT.java`, `BR_M07_22_NhieuLanGiaoIT.java` (phần phiếu), `ThuocDangDungDoiIT.java`: một phiếu mở mỗi người; người xác nhận ≠ người gửi và ≠ người ghi dòng (SC-014); xác nhận áp quyết định từng dòng (Tiếp tục, Ngừng, Thay đổi liều, Thêm mới); quá hạn cảnh báo một lần; phiếu lập trước tiếp nhận tạo đơn `CHO_HIEU_LUC` + `dau_cho_tiep_nhan`; `HoSoSucKhoeBanDauDaDoi` báo người thực hiện
- [ ] T060 [P] [US4] Test `BT/m07/BR_M07_25_DoiChieuChuyenMonIT.java`: lập phiếu theo yêu cầu chuyên môn thiếu lý do → 422; có lý do → `MO`, không đơn nào tạm dừng, liều vẫn chạy, không hạn, job quá hạn không cảnh báo; người cao tuổi Đang tiếp nhận hoặc Điều trị tại bệnh viện → 409; gửi thiếu dòng cho đơn Hiệu lực → 409 `THIEU_DONG`; xác nhận: Ngừng → Đã ngừng, Tiếp tục với đơn Hiệu lực giữ nguyên; tự xác nhận bị chặn (Q-55); hệ thống không tự lập, không nhắc (spec 006 US4 #11; FR-034b, BR-M07-25, Q-340)

### Implementation cho User Story 4

- [ ] T035 [US4] Tạo `MIG/V006_04__doi_chieu.sql`: `phieu_doi_chieu` (`ly_do ENUM('TIEP_NHAN','TRO_VE_BENH_VIEN','THEO_YEU_CAU_CHUYEN_MON')`; `ly_do_lap` có `CHECK` bắt buộc với lý do thứ ba (FR-034b); `trang_thai ENUM('MO','CHO_XAC_NHAN','DA_XAC_NHAN','DA_HUY')`; cột sinh `mo_nct`, `tiep_nhan_nct` `UNIQUE`; `CHECK (nguoi_xac_nhan_id IS NULL OR nguoi_xac_nhan_id <> nguoi_gui_id)`; trigger `trg_phieu_doi_chieu_khoa`), `dong_doi_chieu` (`nguon` `DON_HIEN_CO|DON_RA_VIEN|THUOC_DANG_DUNG_TIEP_NHAN`; `quyet_dinh` `TIEP_TUC|NGUNG|THAY_DOI_LIEU|THEM_MOI`), `de_nghi_dinh_chinh_phieu` (`trang_thai` `CHO_XAC_NHAN|DA_XAC_NHAN|TRA_LAI`; người xác nhận ≠ người lập) (data-model mục 7)
- [ ] T036 [US4] Cài `BE/m07/doichieu/internal/PhieuDoiChieuService.java`: lập (kể cả phiếu theo yêu cầu chuyên môn: trạng thái người cao tuổi hợp lệ, lý do lập bắt buộc, không tạm dừng đơn, không đặt hạn; research D-13, FR-034b), thay dòng (`If-Match`), gửi (với phiếu chuyên môn: đủ dòng cho đơn Hiệu lực, Tạm dừng, Chờ hiệu lực), trả lại, xác nhận (kiểm vấn đề đơn qua `KiemTraDonMoi` trên dòng; áp quyết định tạo/ngừng/thay đơn trong một transaction; xóa `han_doi_chieu`), đề nghị đính chính phiếu và xác nhận; `DinhChinhDuocProvider` `M07.PHIEU_DOI_CHIEU`; listener `HoSoSucKhoeBanDauDaDoi` (research D-13)
- [ ] T037 [US4] Cài phần đối chiếu của bean `BE/m07/trangthai/internal/ThamGiaM07.java`: `HOAN_TAT_TIEP_NHAN` (đơn → `TAM_DUNG` `CHO_DOI_CHIEU`, đơn "chờ tiếp nhận" → `HIEU_LUC`, đặt `han_doi_chieu`, đặt `han_luc` phiếu lập trước), `GHI_NHAN_TRO_VE` từ bệnh viện, `HUY_TIEP_NHAN` (hủy phiếu); việc theo phút đặt `qua_han_luc` và cảnh báo (research D-16)
- [ ] T038 [US4] Tạo `BE/m07/doichieu/web/DoiChieuController.java`: `POST/GET /nguoi-cao-tuoi/{nctId}/phieu-doi-chieu`, `GET /phieu-doi-chieu/{id}`, `PUT …/{id}/dong`, `POST …/gui-xac-nhan`, `/tra-lai`, `/xac-nhan`, `/de-nghi-dinh-chinh`, `POST /de-nghi-dinh-chinh-phieu/{id}/xac-nhan`, `/tra-lai`; màn web `frontend/src/tinh-nang/doi-chieu-thuoc/`

**Checkpoint**: P1 hoàn tất.

---

## Phase 7: User Story 5 – Liều khi vắng mặt: Tạm dừng hoặc Mang theo (Priority: P2)

**Goal**: Khoảng vắng theo lệnh 001 (tạm vắng, bệnh viện, chuyến đi 014); liều Tạm dừng không nhắc; Mang theo, lần giao, nhận lại, ghi theo báo lại; lệnh không mang thuốc; trạng thái cuối (FR-021, FR-027, FR-047, FR-048 → FR-048b, BR-M07-03, BR-M04-16).

**Independent Test**: A có liều 08:00, 12:00, 20:00; Tạm vắng không mang thuốc 09:00–15:00 → liều 12:00 Tạm dừng, không nhắc; chuyến đi 07:00–13:00 hôm sau → liều 08:00, 12:00 Mang theo.

### Tests cho User Story 5

- [ ] T039 [P] [US5] Test `BT/m07/BR_M07_03_VangMatIT.java`, `FR_027_MangTheoBaoLaiIT.java`, `BR_M07_22_NhieuLanGiaoIT.java` (phần giao): liều trong khoảng vắng Tạm dừng/Mang theo; trở về trước cuối cửa sổ quay lại; chỉ Điều dưỡng ghi liều Mang theo theo báo lại có người báo; nhiều lần giao trong một khoảng; nhận lại ≤ số giao; trạng thái cuối hủy liều và đóng đơn

### Implementation cho User Story 5

- [ ] T040 [US5] Tạo `MIG/V006_05__vang_mang_theo.sql`: `khoang_vang_thuoc` (`loai` 5 giá trị ở data-model mục 9; cột sinh một khoảng mở mỗi người `UNIQUE`), `lan_giao_thuoc` (nhóm 3, chỉ cột `trang_thai` `DANG_MANG_THEO|CHO_NHAN_LAI|DA_NHAN_LAI` đổi), `lan_giao_thuoc_dong`, `lan_giao_lieu`, `lan_nhan_lai_thuoc` (`lan_giao_id UNIQUE`), `lan_nhan_lai_thuoc_dong` (`so_luong CHECK >= 0`), `lenh_khong_mang_thuoc` (`UNIQUE (nct_id, chuyen_id)`)
- [ ] T041 [US5] Cài `BE/m07/mangtheo/internal/MangTheoService.java`: thuốc cần mang, lần giao (gắn liều, vật hóa trước đầu cửa sổ), nhận lại (biến động thuốc gửi), không mang thuốc (`ChuyenDiThuocPort.danhGiaDat`); `MangThuocChuyenDiApi` (014); `DinhChinhDuocProvider` `M07.LAN_GIAO_THUOC`, `M07.LAN_NHAN_LAI_THUOC`; listener `LuotVangDaGiaHan` (research D-15)
- [ ] T042 [US5] Cài phần vắng và trạng thái cuối của `ThamGiaM07.java`: `CHO_TAM_VANG` (`chiTiet.mangThuoc`), `CHUYEN_VIEN`, `DIEM_DANH_ROI_VIEN` (khoảng đi qua `ChuyenDiThuocPort`), `GHI_NHAN_TRO_VE`, `DIEM_DANH_VE`, `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI` (đơn → `DA_NGUNG`, liều → `DA_HUY`/`KHONG_THUC_HIEN` Hệ thống) (research D-16)
- [ ] T043 [US5] Tạo `BE/m07/mangtheo/web/MangTheoController.java` (`GET /nguoi-cao-tuoi/{nctId}/thuoc-can-mang`, `POST/GET …/lan-giao-thuoc`, `POST /lan-giao-thuoc/{id}/nhan-lai`, `POST /nguoi-cao-tuoi/{nctId}/khong-mang-thuoc`); màn web `frontend/src/tinh-nang/thuoc-mang-theo/`, app `mobile/lib/nhan-vien/thuoc-mang-theo/`

**Checkpoint**: Vắng mặt chạy độc lập.

---

## Phase 8: User Story 6 – Thuốc khi cần (PRN) có giới hạn (Priority: P2)

**Goal**: Lần dùng PRN nhóm 3; khoảng cách tối thiểu và số lần trong 24 giờ trượt; ngoại tuyến (FR-024 → FR-025a, BR-M07-04).

**Independent Test**: PRN Paracetamol, cách 4 giờ, tối đa 4 lần/ngày; ghi 08:00; thử 11:00 bị chặn; ghi 12:00, 16:00, 20:00; thử 23:59 bị chặn.

### Tests cho User Story 6

- [ ] T044 [P] [US6] Test đơn vị `BT/m07/BR_M07_04_GioiHanPrnTest.java` và `PrnDongThoiIT.java` (hai lần ghi đồng thời không vượt giới hạn dưới khóa `thuoc_nct`)

### Implementation cho User Story 6

- [ ] T045 [US6] Tạo `MIG/V006_06__prn.sql`: `lan_dung_prn` (nhóm 3; `CHECK (lieu_dung > 0)`; `ly_do` bắt buộc) và cài `BE/m07/prn/internal/KiemTraGioiHanPrn.java`, `PrnService.java` (không vượt liều đơn; trừ lô thuốc gửi; sự kiện tính phí nguồn viện; `DinhChinhDuocProvider` `M07.LAN_DUNG_PRN`), `NgoaiTuyenHandler` `M07.LAN_DUNG_PRN` (research D-10)
- [ ] T046 [US6] Tạo endpoint `POST/GET /don-thuoc/{id}/lan-dung-prn` trong `BE/m07/prn/web/PrnController.java`; app xác nhận PRN trong `mobile/lib/nhan-vien/xac-nhan-lieu/`

**Checkpoint**: PRN chạy độc lập.

---

## Phase 9: User Story 7 – Thuốc gia đình gửi: đối chiếu trước khi dùng và theo dõi số lượng (Priority: P2)

**Goal**: Lô Chờ đối chiếu không dùng được; gắn đơn khi Xác nhận sử dụng; số lượng chỉ đổi qua biến động; số ngày còn lại và mốc nhắc CFG-M07-06, 07; hoàn trả, hủy; hạn dùng; việc sau qua đời (FR-040 → FR-047, BR-M07-10, 11, 18, 19).

**Independent Test**: Tiếp nhận 30 viên Donepezil; Chờ đối chiếu, không gắn vào lịch; đối chiếu với đơn nguồn gia đình gửi; xác nhận 26 liều; kiểm số còn và nhắc sắp hết.

### Tests cho User Story 7

- [ ] T047 [P] [US7] Test đơn vị `BT/m07/BR_M07_11_SoNgayConLaiTest.java`, `BR_M07_18_LoDungChungTest.java`
- [ ] T048 [P] [US7] Test `BT/m07/BR_M07_10_ThuocGuiChuaDoiChieuIT.java`, `ThuocGuiSapHetIT.java`, `BR_M07_19_HoanTraIT.java`: chọn lô Chờ đối chiếu bị chặn; trigger chặn đổi `so_luong_con` ngoài biến động, không âm (SC-007); về 0 → `DA_DUNG_HET`; mốc nhắc báo một lần mỗi mốc; hoàn trả, hủy bằng số còn; mua hộ qua `DeNghiMuaHoPort`

### Implementation cho User Story 7

- [ ] T049 [US7] Tạo `MIG/V006_07__thuoc_gui.sql`: `thuoc_gia_dinh_gui` (`so_luong_ban_dau`, `so_luong_con DECIMAL(10,2)` với `CHECK (so_luong_con >= 0)`; `nguoi_giao_loai ENUM('NGUOI_THAN','TU_DO','NHAN_VIEN_MUA_HO')`; `trang_thai ENUM('CHO_DOI_CHIEU','DUOC_SU_DUNG','CHI_GIU_HO','DA_DUNG_HET','DA_HOAN_TRA','DA_HUY_THEO_YEU_CAU')`; trigger `trg_tgdg_so_luong` chỉ cho đổi khi `@m07_bien_dong = 1`), `thuoc_gui_don` (`lo_id`, `don_thuoc_id`, `tu`, `den`), `bien_dong_thuoc_gui` (nhóm 3; `loai` 9 giá trị; trigger đặt `@m07_bien_dong` và cập nhật `so_luong_con`), `moc_nhac_thuoc_gui` (khóa chính `(khoa_nhom, moc, so_thu_tu)`) (data-model mục 8)
- [ ] T050 [US7] Cài `BE/m07/thuocgui/internal/ThuocGuiService.java`: tiếp nhận (ảnh qua `TepApi`), xác nhận sử dụng (gắn đơn cùng thuốc cùng người), chỉ giữ hộ, ngừng sử dụng, kiểm kê điều chỉnh, hoàn trả, hủy (người thân hoặc người đại diện qua 012); `TinhSoNgayConLai`, mốc nhắc; việc hằng ngày `HET_HAN_DUNG`; `ThuocGiaDinhGuiApi`; `MucViecSauQuaDoiPort` `THUOC_GUI`; điều kiện `KT_C_THUOC_GUI` trong `ThamGiaM07.java` (research D-14)
- [ ] T051 [US7] Tạo `BE/m07/thuocgui/web/ThuocGuiController.java`: `POST/GET /nguoi-cao-tuoi/{nctId}/thuoc-gia-dinh-gui`, `GET /thuoc-gia-dinh-gui/{id}`, `POST …/xac-nhan-su-dung`, `/chi-giu-ho`, `/ngung-su-dung`, `/kiem-ke-dieu-chinh`, `/hoan-tra`, `/huy`; màn web `frontend/src/tinh-nang/thuoc-gia-dinh-gui/`, app `mobile/lib/nhan-vien/thuoc-gia-dinh-gui/`

**Checkpoint**: Thuốc gửi chạy độc lập.

---

## Phase 10: User Story 8 – Đính chính liều đã xác nhận và tra lịch sử thuốc (Priority: P3)

**Goal**: Đính chính `M07.KET_QUA_LIEU` (cách A/B theo tầng), sự kiện cho 010; quyền xem theo vai trò và bản đồng ý (FR-030, FR-049, DBR-17 phần 006, DBR-03 phần 006).

**Independent Test**: Xác nhận nhầm Đã dùng; thử sửa trực tiếp bị chặn; đính chính "Không thực hiện" có lý do; trạng thái hiện hành đổi, bản gốc còn, 010 nhận sự kiện.

### Tests cho User Story 8

- [ ] T052 [P] [US8] Test `BT/m07/FR_030_DinhChinhChiPhiIT.java` và `FR_049_NguoiThanXemThuocIT.java`: đính chính phát sự kiện điều chỉnh cho 010; Hành chính, Bếp, Nhân viên chăm sóc không thấy thông tin thuốc; Trưởng tầng thấy liều, không thấy đơn và phiếu; người thân chỉ thấy đơn, phiếu khi có bản đồng ý

### Implementation cho User Story 8

- [ ] T053 [US8] Cài `DinhChinhDuocProvider` `M07.KET_QUA_LIEU` trong `BE/m07/lieu/internal/KetQuaLieuDinhChinhProvider.java` (`tangKhuVuc` = `tang_khu_vuc_id` lúc đóng; `sauKhiDinhChinh` phát sự kiện tính phí điều chỉnh và cập nhật biến động thuốc gửi) (research D-12)
- [ ] T054 [US8] Cài `ThuocCongNguoiThanApi` (012; thuốc gửi luôn; đơn, phiếu khi `BanDongYApi.baoGom`) và `ThuocThongKeApi` (016) trong `BE/m07/trangthai/internal/`; màn `frontend/src/tinh-nang/cong-nguoi-than/thuoc/` và app `mobile/lib/nguoi-than/thuoc/` (research D-18)

**Checkpoint**: Mọi user story của 006 chạy độc lập.

---

## Phase 10b: User Story 9 – Tiếp nhận thuốc cung ứng (Priority: P3) *(bổ sung 2026-10-11, Q-337)*

**Goal**: Điều dưỡng ghi lần tiếp nhận Đạt / Không đạt cho dòng của yêu cầu mua sắm loại "thuốc" đã ghi đã mua; không tồn kho, không tính phí, không đổi đơn, liều (FR-054, FR-055, BR-M07-24).

**Independent Test**: spec 006 User Story 9 với bản giả `YeuCauThuocCungUngPort`.

### Tests cho User Story 9

- [ ] T061 [P] [US9] Test `BT/m07/BR_M07_24_TiepNhanThuocCungUngIT.java`: Đạt lưu gắn dòng, không đổi đơn, liều, không phát `SuKienTinhPhiThuoc`; Không đạt thiếu sai lệch hoặc lý do → 422, đủ thì lưu và phát `TiepNhanThuocKhongDat` (Nhẹ) mang `boPhanId` của yêu cầu, người nhận là người lập và `PhuTrachBoPhan(boPhanId)`; đổi Phụ trách bộ phận sau khi ghi đã mua thì người mới nhận, người cũ không nhận; người lập chuyển bộ phận thì vẫn dùng bộ phận của yêu cầu; giao bù là lần mới; yêu cầu chưa ghi đã mua (port trả rỗng) → 409; `daNhan` khi mọi dòng có lần Đạt; xác nhận liều của đơn chưa có lần tiếp nhận không bị chặn; trigger chặn `UPDATE`, `DELETE`; Điều dưỡng ngoài phạm vi phân công của người cao tuổi của dòng → 403 (spec 006 US9 #1 → #3; FR-054, FR-055, BR-M07-24, BR-M11-17)

### Implementation cho User Story 9

- [ ] T062 [US9] Khai báo SPI `YeuCauThuocCungUngPort` và giao diện `TiepNhanThuocCungUngApi`, bổ sung `DonThuocApi.donNguonVienChuaKetThuc` trong `BE/m07/api/` (contracts mục 1.2, 1.8a, 2.1); bản giả có công tắc trong `BT/support/gia006/` cho port
- [ ] T063 [US9] Tạo `MIG/V006_08__tiep_nhan_thuoc_cung_ung.sql`: `lan_tiep_nhan_thuoc_cung_ung` theo data-model mục 9a (`ket_qua ENUM('DAT','KHONG_DAT')`, `CHECK` sai lệch và lý do khi Không đạt, `CHECK (so_luong_nhan > 0)`, trigger nhóm 3)
- [ ] T064 [US9] Cài `BE/m07/tiepnhanthuoc/internal/TiepNhanThuocCungUngService.java` (kiểm dòng qua port, quyền `TIEP_NHAN_THUOC_CUNG_UNG, TAO` và phạm vi theo người cao tuổi của dòng, phát `TiepNhanThuocKhongDat` cho 009 với nhóm `NhanVien`, `PhuTrachBoPhan(boPhanId)`, không tự gửi thông báo; không tự lập hay hủy yêu cầu, Q-307), `DinhChinhDuocProvider` `M07.LAN_TIEP_NHAN_THUOC`, bản cài `TiepNhanThuocCungUngApi`, `donNguonVienChuaKetThuc`
- [ ] T065 [US9] Tạo `BE/m07/tiepnhanthuoc/web/TiepNhanThuocController.java` (`POST`, `GET /tiep-nhan-thuoc-cung-ung`), màn web `frontend/src/tinh-nang/tiep-nhan-thuoc/` và app `mobile/lib/nhan-vien/tiep-nhan-thuoc/`

**Checkpoint**: Tiếp nhận thuốc cung ứng chạy độc lập với bản giả 019.

---

## Phase 11: Polish & Cross-Cutting

- [ ] T055 [P] Cài `LieuBanGiaoApi` (008; liều chưa đóng của ca, BR-M09-06) trong `BE/m07/lieu/internal/`
- [ ] T056 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/006-medication-management/contracts/api-006.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 006
- [ ] T057 Khi 008, 010, 014 có bản thật, chạy lại test US2, US5, US7 với bean thật thay T002; khi 019 có bản thật `YeuCauThuocCungUngPort`, chạy lại test US9 thay bản giả của T062 (bổ sung 2026-10-11)
- [ ] T058 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T059 Rà truy vết data-model mục 14: DBR-03, 13, 14, 15, 17, 23, 25 và BR-M07-01 → 22, BR-M07-24, 25 có test (BR-M07-23 ngoài phạm vi, không có chức năng); FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-034a, FR-050, FR-051, FR-052, FR-053 (bổ sung 2026-10-10) — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 006 chặn 007, 008, 010, 012, 014, 016 (cần `m07.api`)
- **US1 (P1)**: sau Foundational
- **US2 (P1)**: sau US1
- **US3 (P1)**: sau US2 (hủy liều đã vật hóa)
- **US4 (P1)**: sau US3 (quyết định Thay đổi liều, Ngừng)
- **US5 (P2)**: sau US2
- **US6 (P2)**: sau US1
- **US7 (P2)**: sau US2 (trừ lô khi xác nhận)
- **US8 (P3)**: sau US2
- **US9 (P3)** *(bổ sung 2026-10-11)*: sau US1 (đơn nguồn viện); 019 cài `YeuCauThuocCungUngPort` sau khi 006 xong

### Parallel Opportunities

- T003, T004, T008, T009 song song
- US6 song song với US2–US4; US5 và US7 song song sau US2
- Web và app song song khi controller đã có

## Parallel Example: User Story 2

```text
Task: "T018 BR_M07_01_LichThuocTest + BR_M07_13_TuChoiLienTiepTest"
Task: "T019 SC_001/DBR_13/BR_M07_02/BR_M07_12 IT"
Task: "T020 FR_026/BR_M07_21/PhanUng/BR_M07_14 IT"
Task: "T021 ngoại tuyến IT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2
2. **Dừng và kiểm chứng**: SC-001 (không lập sẵn liều), phát thuốc và xác nhận, Trễ/Bỏ lỡ
3. Thêm US3, US4 cho vòng đời đơn và đối chiếu

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7 → US8 → US9; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- 006 không lập sẵn liều, không tự tạo phiếu hay chi phí, không chọn sẵn lô (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M07-*, CFG-M05-04)
- Ngoại tuyến chỉ cho xác nhận liều và PRN của thuốc không kiểm soát đặc biệt (Q-01)
