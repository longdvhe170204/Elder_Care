---
description: "Danh sách task triển khai feature 012 – Người thân và cổng thông tin gia đình"
---

# Tasks: Người thân và cổng thông tin gia đình (012)

**Input**: Tài liệu thiết kế ở `specs/012-family-portal/` (plan.md, spec.md, research.md F-01 → F-17, data-model.md, contracts/api-012.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2 (002 khai báo `QuanHeNguoiThanApi` dạng SPI); 001, 003 → 011 đã có `api` (SPI 012 cài: `NguoiDaiDienPort`, `BanGhiDonPort`, `BanGhiDonBanTruPort`, `ThoiKhoaBieuNguonPort`, `NguoiThanKhanCapPort`, `NguoiThanNhanThongBaoPort`, `LuotOLaiAnPort`; API đọc cho cổng: `KetQuaChamSocCongApi`, `ThuocCongNguoiThanApi`, `SucKhoeCongNguoiThanApi`, `ChiPhiCongNguoiThanApi`, `CheDoAnApi`).

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX). Từ 2026-10-16 là giai đoạn thiết kế; task chỉ được làm khi chủ dự án mở giai đoạn triển khai.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M10: `BE/m10/{api,nguoithan,quyen,don,tham,olai,cong,bantin,phanhoi,lich}/`
- Web `frontend/src/tinh-nang/nguoi-than/{ho-so,yeu-cau-quyen,don,tham,o-lai,ban-tin,phan-hoi,cong-nguoi-than}/`; app nhân viên `mobile/lib/nhan-vien/nguoi-than/`; app người thân `mobile/lib/nguoi-than/`

---

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; Q-347, Q-348, Q-351 → Q-353): sửa T003, T006, T008, T014 → T017, T026, T027, T031, T032, T040; thêm T016a, T016b. Mã task cũ giữ nguyên. Phụ thuộc mới: `NguonYKienApi`, `BanDongYApi.daiDienPhapLuat` (001), `SuCoNguoiThanView` (007 T061a).

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/m10/...` với `package-info.java`; ArchUnit `BT/m10/M10ArchTest.java`: gói `..lich..` không lập bản tin, không lập khoản (Q-306); DTO người thân không có trường nhật ký (research F-01)
- [ ] T002 [P] Tạo bản giả `HoatDongBanTinPort` (014) trong `BT/support/gia012/` (contracts mục 1.4)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Hồ sơ người thân, quan hệ, quyền có tác dụng, DBR-02, khai báo `m10.api`, bản cài `QuanHeNguoiThanApi` (002, 001, 006, 011 cần), khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 [P] Tạo `BE/m10/api/`: `QuyenNguoiThanApi`, `NguoiThanThongKeApi`, SPI `HoatDongBanTinPort`, sự kiện mục 1.5 (cài `CoYeuCauThongBao`; mỗi yêu cầu tới người thân khai `doiTuongThongTin` theo bảng research F-15, bổ sung 2026-10-18) (contracts mục 1.2 → 1.5)
- [ ] T004 Tạo `MIG/V012_01__nguoi_than.sql`: `nguoi_than_nct` (`nct_id` khóa chính), `nguoi_than` (số giấy tờ `CA_NHAN`; không xóa khi đã có quan hệ), `quan_he_nguoi_than` (`quan_he` `VO_CHONG|CON|CHAU|ANH_CHI_EM|KHAC`; sáu quyền; `trang_thai` `HIEU_LUC|DA_KET_THUC`; cột sinh `khoa_hieu_luc` với `UNIQUE (nguoi_than_id, nct_id, khoa_hieu_luc)`; cột sinh `khoa_lien_he_chinh` `UNIQUE`), `lich_su_quyen` (nhóm 3), `su_kien_m10_da_xu_ly`, `lan_chay_m10` (data-model mục 2, 10)
- [ ] T005 [P] Cài hàm thuần `BE/m10/quyen/internal/QuyenCoTacDung.java` (quan hệ + bản đồng ý `BanDongYApi` tại thời điểm xem; DBR-03) và `BE/m10/nguoithan/internal/KiemTraDbr02.java` (≥ 1 đại diện, đúng 1 liên hệ chính) với test `BT/m10/QuyenCoTacDungTest.java`, `KiemTraDbr02Test.java` (research F-04, F-05)
- [ ] T006 Cài bản cài SPI trong `BE/m10/nguoithan/internal/`: `QuanHeNguoiThanApi` (`hieuLuc`, `soDienThoaiDaDangKy`, `laNguoiDaiDienHieuLuc`, `thuocNguoiCaoTuoi`, `quyenDangBat`), `DieuKienChuThich` ² (người đại diện), `NguoiDaiDienPort` (004), `NguoiThanNhanThongBaoPort` (009), `NguoiThanKhanCapPort` (`nguoiLienHeChinh`, `nguoiDaiDien`) (007), `QuyenNguoiThanApi` (contracts mục 1.1, 1.2)
- [ ] T007 Cài khung job `BE/m10/lich/ViecTheoPhutM10.java`, `ViecHangGioM10.java`, `ViecNuaDemM10.java`, `ViecBanTinM10.java` (ShedLock, chạy bù) và test `BT/m10/ChayBuM10IT.java` (research F-17)
- [ ] T008 [P] Cài provider: `PhanLoaiTruongProvider` (số giấy tờ → `CA_NHAN`), `LuuTruProvider` (`THEO_HO_SO`), `GanPhamViProvider` (`M10.*` theo người cao tuổi, gồm `M10.MONG_MUON` bổ sung 2026-10-18; `so_giay_to` của mong muốn → `CA_NHAN`), `QuyenTepProvider` (`M10.BANG_CHUNG`, `M10.PHIEU_DANG_KY`, `M10.PHAN_HOI`)

**Checkpoint**: Quan hệ và quyền sẵn sàng cho 001, 002, 006, 011.

---

## Phase 3: User Story 1 – Hồ sơ người thân, vai trò và quyền theo từng người thân (Priority: P1) 🎯 MVP

**Goal**: Lập hồ sơ, quan hệ, người đại diện, liên hệ chính, thứ tự liên hệ, phiếu đăng ký người thân; DBR-02 dưới khóa; điều kiện `DBR_02` cho Hoàn tất tiếp nhận; kết thúc quan hệ khóa tài khoản qua 002 (FR-001 → FR-012, DBR-02, DBR-03).

**Independent Test**: R, M, N cho A với vai trò, quyền khác nhau; thử bỏ liên hệ chính, bỏ người đại diện cuối cùng, bật xem sức khỏe cho N khi N không có trong bản đồng ý.

### Tests cho User Story 1

- [ ] T009 [P] [US1] Test `BT/m10/DBR_02_LienHeChinhIT.java`, `FR_008_PhieuDangKyIT.java`, `Q249_ThongBaoDaiDienKhacIT.java`, `FR_075_TrangThaiCuoiIT.java`: ≤ 1 liên hệ chính (`UNIQUE`); bỏ người đại diện cuối bị chặn; kết thúc quan hệ gọi `TaiKhoanApi.xetLaiNguoiThan` cùng transaction, bỏ khỏi danh sách đón, hủy lượt thăm; Hoàn tất tiếp nhận bị chặn khi thiếu `DBR_02` (Q1 → Q7)

### Implementation cho User Story 1

- [ ] T010 [US1] Tạo bảng `phieu_dang_ky_nguoi_than`, `phieu_dang_ky_dong` (nhóm 3; tệp scan bắt buộc) trong `MIG/V012_02__phieu_dang_ky.sql`
- [ ] T011 [US1] Cài `BE/m10/nguoithan/internal/NguoiThanService.java`: hồ sơ, lập quan hệ, đổi liên hệ chính, đặt người đại diện đầu tiên (lý do, bằng chứng), đổi thứ tự liên hệ, kết thúc quan hệ (không kích hoạt lại), phiếu đăng ký; khóa `nguoi_than_nct`; `KiemTraDbr02`; ghi `lich_su_quyen` (research F-04, F-05)
- [ ] T012 [US1] Cài bean `ThamGiaChuyenTrangThai` của 012 (`@Order` sau 010, trước 013; điều kiện `DBR_02` `coTheMien = false` cho `HOAN_TAT_TIEP_NHAN`; tác động vắng, trạng thái cuối) trong `BE/m10/nguoithan/internal/ThamGiaM10.java`
- [ ] T013 [US1] Tạo endpoint `/nguoi-than`, `/{id}`, `/nguoi-cao-tuoi/{nctId}/quan-he`, `/quan-he/{id}/{lenh}`, `/nguoi-cao-tuoi/{nctId}/phieu-dang-ky`; màn web `frontend/src/tinh-nang/nguoi-than/ho-so/`

**Checkpoint**: Quan hệ người thân chạy độc lập.

---

## Phase 4: User Story 3 – Thay đổi danh sách đón và quyền qua xác nhận/duyệt (Priority: P1)

**Goal**: Yêu cầu quyền, dấu "được tự về", đặt/thôi đại diện theo BR-M10-07; một yêu cầu chờ mỗi (quyền, quan hệ); áp ngay khi tắt quyền đón; nhắc CFG-M10-11 (FR-013 → FR-020).

**Independent Test**: Yêu cầu thêm X vào danh sách đón, bật xem chi phí cho N, bỏ R khỏi danh sách đón; xác nhận, từ chối, hủy → quyền áp dụng ở lần kiểm tra kế tiếp.

### Tests cho User Story 3

- [ ] T014 [P] [US3] Test đơn vị `BT/m10/KiemTraYeuCauTest.java` (bổ sung 2026-10-18: trái mong muốn theo loại, chiều bật/tắt, ngoại lệ tắt đón, người đại diện theo pháp luật) và `BT/m10/BR_M10_07_YeuCauQuyenIT.java`, `FR_020_MongMuonIT.java` (bổ sung 2026-10-18: US3 #9 → #11; mong muốn bất biến, thay thế; xác nhận cổng bị từ chối khi trái; Quản lý viện duyệt thiếu `traoDoiVoiNguoiCaoTuoi` → 422; dòng phiếu trái thành Chờ xác nhận; cảnh báo quyền trái): `UNIQUE khoa_cho`; yêu cầu bỏ thay thế yêu cầu chờ; người đại diện qua cổng và Hành chính kèm bản ký → `HIEU_LUC` ngay; hai người đại diện xác nhận đồng thời (Q8 → Q12)

### Implementation cho User Story 3

- [ ] T015 [US3] Tạo `MIG/V012_03__yeu_cau_quyen.sql`: `yeu_cau_quyen` (`loai` `QUYEN|TU_VE|DAT_DAI_DIEN|THOI_DAI_DIEN`; `nguon` `QUAY|CONG|PHIEU_DANG_KY`; `trang_thai` `CHO_XAC_NHAN|HIEU_LUC|TU_CHOI|HUY`; `cach_xac_nhan` `CONG|BAN_KY|QL_DUYET`; cột sinh `khoa_cho` `UNIQUE`), `dau_tu_ve` (`nct_id` khóa chính)
- [ ] T016 [US3] Cài `BE/m10/quyen/internal/YeuCauQuyenService.java` (lập, xác nhận, duyệt, từ chối, hủy; áp ngay khi tắt đón hoặc tắt tự về; người đón mới chưa có hồ sơ thì lập kèm; nhắc trong `ViecHangGioM10`) (research F-06)
- [ ] T016a [US3] Tạo `MIG/V012_03a__mong_muon.sql` (`mong_muon_nct` theo data-model mục 3a: `CHECK` người được nói tới; `loai ENUM('THAM','DON')`; mẫu cột nguồn 001; `trang_thai ENUM('HIEU_LUC','DUOC_THAY_THE')`; cột sinh `khoa_hieu_luc` `UNIQUE`; trigger bất biến) và thêm vào `yeu_cau_quyen` hai cột `trai_mong_muon_id`, `trao_doi_voi_nct` với `CHECK` ở data-model mục 3; cài `BE/m10/quyen/internal/MongMuonService.java` (ghi, thay thế; `NguonYKienApi.kiemTra`; quyền Hành chính, Trưởng tầng, Điều dưỡng có phạm vi) (FR-020a; research F-06a)
- [ ] T016b [US3] Mở rộng `KiemTraYeuCau` và `YeuCauQuyenService` cho FR-020b, FR-020c (trái mong muốn chỉ qua Quản lý viện; tắt đón áp ngay; người đại diện theo pháp luật qua `BanDongYApi.daiDienPhapLuat`; phiếu đăng ký: dòng trái thành `CHO_XAC_NHAN`), tính `quyenTraiMongMuon` lúc đọc (research F-06a)
- [ ] T017 [US3] Tạo endpoint `/yeu-cau-quyen`, `/yeu-cau-quyen/{id}/{lenh}`, `GET /nguoi-cao-tuoi/{nctId}/danh-sach-don`, `GET/POST /nguoi-cao-tuoi/{nctId}/mong-muon` (bổ sung 2026-10-18); màn web `frontend/src/tinh-nang/nguoi-than/yeu-cau-quyen/`; app người thân xác nhận yêu cầu

**Checkpoint**: Thay đổi quyền chạy độc lập.

---

## Phase 5: User Story 2 – Quy trình đón người cao tuổi (Priority: P1)

**Goal**: Ghi đón (xác minh giấy tờ, danh sách tại thời điểm, ngoại lệ, tự về, viện đưa về); lần chặn; bản ghi đón hết hiệu lực CFG-M10-09; dùng một lần qua `daDung` của 004, 005 (FR-021 → FR-028, BR-M10-03, Q-295).

**Independent Test**: Danh sách đón của A gồm R, M; đón bởi R giấy tờ khớp; M giấy tờ không khớp; X ngoài danh sách có và không có ngoại lệ.

### Tests cho User Story 2

- [ ] T018 [P] [US2] Test `BT/m10/BR_M10_03_QuyTrinhDonIT.java`, `FR_024_NgoaiLeDonIT.java`, `FR_027_BanGhiDonHetHieuLucIT.java`, `Q295_VienDuaVeIT.java`: dùng lần hai → `BAN_GHI_DON_DA_DUNG` (`khoa_da_dung UNIQUE`); bỏ người đón cùng lúc ghi đón; ghi đón chỉ trực tuyến (app) (Q13 → Q20)

### Implementation cho User Story 2

- [ ] T019 [US2] Tạo `MIG/V012_04__don.sql`: `ban_ghi_don` (nhóm 3; `loai` 6 giá trị; `can_cu` `DANH_SACH|NGOAI_LE_DAI_DIEN|NGOAI_LE_QL|TU_VE`; `het_hieu_luc_luc`), `ban_ghi_don_su_kien` (`loai` `DA_DUNG|KHONG_DUNG`; cột sinh `khoa_da_dung` `UNIQUE`), `lan_chan_don` (nhóm 3), `yeu_cau_ngoai_le_don` (`trang_thai` `CHO_XAC_NHAN|HIEU_LUC|DA_DUNG|HET_HAN|TU_CHOI|HUY`)
- [ ] T020 [US2] Cài `BE/m10/don/internal/DonService.java` (ghi đón, lần chặn, ngoại lệ; `lich_su_quyen` tại thời điểm; `DinhChinhDuocProvider` `M10.BAN_GHI_DON`), bản cài `BanGhiDonPort` (004) và `BanGhiDonBanTruPort` (005) với `daDung` MANDATORY; việc theo phút hết hiệu lực, ngoại lệ hết hạn (research F-07)
- [ ] T021 [US2] Tạo endpoint `/nguoi-cao-tuoi/{nctId}/don`, `/ban-ghi-don`, `/ngoai-le-don`, `/ngoai-le-don/{id}/{lenh}`; màn web `frontend/src/tinh-nang/nguoi-than/don/`; app `mobile/lib/nhan-vien/nguoi-than/` (ghi đón chỉ trực tuyến) với test flutter

**Checkpoint**: P1 hoàn tất.

---

## Phase 6: User Story 4 – Đăng ký thăm và ghi nhận vào/ra (Priority: P2)

**Goal**: Đăng ký tự duyệt theo khung CFG-M10-04 và sức chứa (khóa `khung_tham_ngay`); số người tối đa CFG-M10-06; vào/ra; Không đến; tự hủy khi vắng, khoanh vùng, trạng thái cuối (FR-029 → FR-039, BR-M10-02).

**Independent Test**: Khung 09:00–11:00 sức chứa 10; đăng ký tới đầy; ngoài khung; khi Tầng 2 khoanh vùng; khi A Tạm vắng.

### Tests cho User Story 4

- [ ] T022 [P] [US4] Test đơn vị `BT/m10/KiemTraDangKyThamTest.java` và `BT/m10/BR_M10_02_DangKyThamIT.java`, `FR_037_KhoanhVungHuyThamIT.java`, `SC_004_SucChuaDongThoiIT.java` (20 đăng ký chỗ cuối) (Q21 → Q28)

### Implementation cho User Story 4

- [ ] T023 [US4] Tạo `MIG/V012_05__tham.sql`: `khung_tham_ngay` (khóa chính `(ngay, gio_bat_dau, gio_ket_thuc)`; `so_nguoi_da_giu`), `luot_tham` (`trang_thai` `DA_DUYET|DA_VAO|DA_RA|KHONG_DEN|HUY`; `nguon` `CONG|QUAY`), `luot_tham_di_cung`
- [ ] T024 [US4] Cài `BE/m10/tham/internal/ThamService.java` (đăng ký trả mọi lý do từ chối, hủy, vào, ra; listener `LuotVangDaMo`, `KhoanhVungDaTao`, kết thúc quan hệ; việc theo phút Không đến, nhắc ghi giờ ra; `NguoiThanKhanCapPort.luotThamTrongKhoang`; `ThoiKhoaBieuNguonPort` (005); `DinhChinhDuocProvider` `M10.LUOT_THAM`) (research F-08)
- [ ] T025 [US4] Tạo endpoint `/khung-tham`, `/luot-tham`, `/luot-tham/{id}/{lenh}`; màn web `frontend/src/tinh-nang/nguoi-than/tham/` với test Vitest; app người thân đăng ký thăm (SC-004 ≤ 2 phút) với test flutter

**Checkpoint**: Thăm chạy độc lập.

---

## Phase 7: User Story 5 – Cổng thông tin theo quyền từng người thân (Priority: P2)

**Goal**: Bốn loại thông tin (chung, sức khỏe, chi phí, hợp đồng); phần sức khỏe đi qua 012 để kiểm quyền và ghi nhật ký lượt xem; REST của 010, 013, 014, 017 kiểm quyền qua `QuyenNguoiThanApi` (FR-046 → FR-050, BR-M10-01, NFR-08).

**Independent Test**: R (đại diện, xem chi phí), M (xem sức khỏe, trong bản đồng ý), N (không quyền riêng) mở từng phần → phần hiện, ẩn, nhật ký lượt xem.

### Tests cho User Story 5

- [ ] T026 [P] [US5] Test `BT/m10/BR_M10_01_CongTheoQuyenIT.java`, `FR_048_NhatKyXemIT.java`, `SC_002_SucKhoeTest.java`; bổ sung 2026-10-18: người thân không có quyền xem sức khỏe thấy phần chung của sự cố (kèm mức), không thấy phần sức khỏe, không có dòng nhật ký xem (FR-046, Q-352)

### Implementation cho User Story 5

- [ ] T027 [US5] Tạo bảng `nhat_ky_xem_suc_khoe` (nhóm 3; `phan` 11 giá trị) trong `MIG/V012_06__nhat_ky_xem.sql`; cài `BE/m10/cong/internal/CongService.java` (phần chung, gồm phần chung của sự cố từ `SucKhoeCongNguoiThanApi.suCo`; phần sức khỏe gọi `KetQuaChamSocCongApi`, `ThuocCongNguoiThanApi`, `SucKhoeCongNguoiThanApi`, `CheDoAnApi` sau khi `QuyenCoTacDung` thỏa và ghi nhật ký; hợp đồng qua `HopDongDocApi`) (research F-10, F-11)
- [ ] T028 [US5] Tạo endpoint `/cong/nguoi-cao-tuoi`, `/{nctId}/chung`, `/{nctId}/suc-khoe/{phan}`, `/{nctId}/hop-dong`, `GET /nhat-ky-xem-suc-khoe`; màn web `frontend/src/tinh-nang/nguoi-than/cong-nguoi-than/` (ẩn phần không có quyền) với test Vitest; app người thân phần cổng

**Checkpoint**: Cổng chạy độc lập.

---

## Phase 8: User Story 6 – Phản hồi, khiếu nại có hạn và leo thang (Priority: P2)

**Goal**: Phản hồi mức Cao/Thường, hạn theo CFG, Nhận xử lý, trả lời, quá hạn leo thang một lần mỗi hạn, mở lại, tự đóng (FR-058 → FR-066, BR-M10-05).

**Independent Test**: Hai phản hồi Cao và Thường; một quá hạn; trả lời rồi người thân mở lại, rồi tự đóng.

### Tests cho User Story 6

- [ ] T029 [P] [US6] Test đơn vị `BT/m10/HanPhanHoiTest.java` và `BT/m10/BR_M10_05_PhanHoiIT.java` (Q43 → Q46)

### Implementation cho User Story 6

- [ ] T030 [US6] Tạo bảng `phan_hoi` (`trang_thai` `MOI|DANG_XU_LY|DA_PHAN_HOI|MO_LAI|DONG`; `muc` `CAO|THUONG`), `phan_hoi_muc` (nhóm 3) trong `MIG/V012_07__phan_hoi.sql`; cài `BE/m10/phanhoi/internal/PhanHoiService.java` (việc theo phút quá hạn, leo thang, tự đóng) và endpoint `/phan-hoi`, `/{id}`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/nguoi-than/phan-hoi/`, app người thân gửi phản hồi (research F-13)

**Checkpoint**: Phản hồi chạy độc lập.

---

## Phase 9: User Story 7 – Điều dưỡng soạn bản tin tuần, duyệt và gửi (Priority: P3)

**Goal**: Nhắc tại mốc; số liệu tính sẵn khi Điều dưỡng soạn; không soạn sẵn nội dung; giải thích sự cố; duyệt (hoặc duyệt thay sau 48 giờ); gửi; đính chính; không gửi khi quá kỳ, qua đời (FR-051 → FR-057, BR-M10-06, 08, 09).

**Independent Test**: Dữ liệu một tuần cho A; tới mốc thứ 2 có lời nhắc; Điều dưỡng soạn, duyệt, gửi; đính chính.

### Tests cho User Story 7

- [ ] T031 [P] [US7] Test đơn vị `BT/m10/NguoiDuyetBanTinTest.java` và `BT/m10/BR_M10_08_BanTinIT.java` (bổ sung 2026-10-18: yêu cầu gửi bản tin khai `VE_NGUOI_CAO_TUOI`; số liệu sự cố tách phần chung kèm mức và phần sức khỏe; FR-052, FR-054), `BR_M10_09_GiaiThichSuCoIT.java`, `FR_056_DinhChinhIT.java`, `FR_073_BaoBanTinChuaLapIT.java` (tới mốc kỳ kế tiếp, bản tin kỳ trước chưa lập hoặc chuyển Không gửi vì quá kỳ → báo Quản lý viện mức Nhẹ kèm tình trạng và các lần nhắc đã gửi; đồng hồ giả lập) (Q37 → Q42)

### Implementation cho User Story 7

- [ ] T032 [US7] Tạo bảng `ban_tin` (`trang_thai` `CHO_DUYET|DA_GUI|KHONG_GUI`; cột sinh `khoa_ky` `UNIQUE`; `ban_goc_id`), `ban_tin_phan`, `ban_tin_so_lieu`, `ban_tin_nhan_xet` trong `MIG/V012_08__ban_tin.sql`; cài `BE/m10/bantin/internal/BanTinService.java` (báo Quản lý viện theo FR-073 trong `ViecBanTinM10`; số liệu từ `KetQuaChamSocCongApi.tongHop`, `ChiSoApi`, `SuCoApi`, `ChiPhiCongNguoiThanApi`, `HoatDongBanTinPort`; gửi qua 009; `ThongBaoTraCuuApi.xemLuc`; `ViecBanTinM10` nhắc và báo duyệt thay) (research F-12)
- [ ] T033 [US7] Tạo endpoint `/ban-tin`, `/{id}`, `/{id}/so-lieu`, `/{id}/{lenh}`, `/{id}/dinh-chinh`; màn web `frontend/src/tinh-nang/nguoi-than/ban-tin/` (không có nội dung soạn sẵn) với test Vitest; app nhân viên phần bản tin

**Checkpoint**: Bản tin chạy độc lập.

---

## Phase 10: User Story 8 – Người thân ở lại chăm sóc (Priority: P3)

**Goal**: Đăng ký, Trưởng tầng xác nhận, bắt đầu, gia hạn, kết thúc; đêm tính phí tại mốc 00:00 phát `SuKienTinhPhiNguon` `DEM_O_LAI`; suất ăn cho 011 (FR-040 → FR-045, BR-M10-04).

**Independent Test**: M ở lại 18:00 ngày 01/11 → 08:00 ngày 03/11 có đăng ký ăn; bắt đầu khi chưa xác nhận bị chặn; số đêm tính phí là 2.

### Tests cho User Story 8

- [ ] T034 [P] [US8] Test đơn vị `BT/m10/SoDemTest.java` và `BT/m10/BR_M10_04_DemOLaiIT.java`, `FR_040_GioiHanOLaiIT.java` (Q29 → Q33)

### Implementation cho User Story 8

- [ ] T035 [US8] Tạo bảng `luot_o_lai` (`trang_thai` `CHO_XAC_NHAN|DA_XAC_NHAN|TU_CHOI|DANG_O_LAI|DA_KET_THUC|HUY`), `luot_o_lai_gia_han` (nhóm 3), `dem_o_lai` (`UNIQUE (luot_id, ngay)`) trong `MIG/V012_09__o_lai.sql`; cài `BE/m10/olai/internal/OLaiService.java` (`ViecNuaDemM10` phát đêm; phát `LuotOLaiDaDoi`; `LuotOLaiAnPort` cho 011) và endpoint `/luot-o-lai`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/nguoi-than/o-lai/` (research F-09)

**Checkpoint**: Mọi user story của 012 chạy độc lập.

---

## Phase 10a: User Story 9 – Người cao tuổi xem thông tin của mình và gửi phản hồi (Priority: P2) *(bổ sung 2026-10-18, Q-365 → Q-371)*

**Goal**: Kênh người cao tuổi với năm phần thông tin, phản hồi của người cao tuổi (kể cả ghi thay, loại Mong muốn), nút SOS và trạng thái trên app (FR-058 → FR-058b, FR-066, FR-069, FR-083 → FR-086, BR-M10-11, 12).

**Independent Test**: Quickstart "Kịch bản bổ sung 2026-10-18, lượt 3" K1 → K9 với bản giả 002, 007, 011, 013, 014.

### Tests cho User Story 9

- [ ] T062 [P] [US9] Test `BT/m10/FR_083_KenhNguoiCaoTuoiIT.java`, `FR_084_KenhKhongLoIT.java`: đủ năm phần; không có trường cấm; không mở được người khác; ArchUnit `m10.kenhnct` không gọi API sức khỏe, thuốc, chi phí, số dư (K1, K2; SC-011)
- [ ] T063 [P] [US9] Test `BT/m10/FR_058a_PhanHoiNguoiCaoTuoiIT.java`, `BR_M10_12_MongMuonIT.java`: gửi qua kênh; ghi thay có và không có tài khoản; Mong muốn chỉ người cao tuổi; chặn Trả lời khi chưa gắn mục; `doi-loai` có lý do; ghi thay `dong-y-dong`; trả lời nhóm sức khỏe chỉ phần chung (K3, K4, K6 → K8)
- [ ] T064 [P] [US9] Test `BT/m10/FR_086_NguoiThanKhongThayIT.java`: người thân không thấy phản hồi của người cao tuổi ở `/phan-hoi`, cổng, bản tin, thông báo (K5)

### Implementation cho User Story 9

- [ ] T065 [US9] Tạo `MIG/V012_10__kenh_nguoi_cao_tuoi.sql`: cột `nguoi_gui_loai`, `ghi_thay_boi_id`, cột `chu_de_mong_muon`, giá trị `MONG_MUON`, `KENH_NGUOI_CAO_TUOI`, `GHI_TRUC_TIEP`, `GHI_THAY`, `DOI_LOAI` và `CHECK` mới cho `phan_hoi`, `phan_hoi_muc`; bảng `phan_hoi_y_kien` (data-model mục 9)
- [ ] T066 [US9] Mở rộng `BE/m10/phanhoi/internal/`: người gửi người cao tuổi, ghi thay, loại Mong muốn và người phụ trách mặc định; lệnh `doi-loai`, `gan-y-kien` (kiểm `mong_muon_nct`; nghe `YKienDaGhiTuPhanHoi` của 014); chặn Trả lời theo BR-M10-12; lọc `nguoi_gui_loai` cho AC-10 (research F-13)
- [ ] T067 [US9] Khai báo `HoatDongKenhNctPort`, `DoGuiKenhNctPort` ở `BE/m10/api/` và bản giả trong `BT/support/gia012/`; bản giả `SuatAnDuKienApi` của 011
- [ ] T068 [US9] Cài `BE/m10/kenhnct/internal/KenhNguoiCaoTuoiService.java` và `web/KenhNguoiCaoTuoiController.java` cho `/kenh-nguoi-cao-tuoi/**` (contracts/api-012.yaml), DTO riêng (research F-18)
- [ ] T069 [US9] Thêm hai thông báo cho nhóm `NguoiCaoTuoi` (FR-069): job `BE/m10/lich/NhacLuotThamNct.java` lúc 00:05 và phát khi lượt tạo trong ngày; "phản hồi đã được trả lời" cho người gửi là người cao tuổi
- [ ] T070 [P] [US9] Tạo `mobile/lib/nguoi-cao-tuoi/{hom-nay,thuc-don,hoat-dong,luot-tham,do-gui,phan-hoi}/` theo DESIGN.md mục 15 (chữ lớn, một việc mỗi màn) và test `mobile/test/nguoi-cao-tuoi/hom_nay_test.dart`, `phan_hoi_test.dart`
- [ ] T071 [P] [US9] Tạo `mobile/lib/nguoi-cao-tuoi/sos/`: nút ở màn đầu, chạm một lần thì đếm ngược 3 giây có nút Hủy (Q-372), hết giờ hoặc chạm lần hai thì gọi `POST /sos` bằng token thiết bị, màn trạng thái hỏi lại 5 giây, lưu `lienHe` gần nhất, thông báo lần đầu "không thay chuông gọi"; test `mobile/test/nguoi-cao-tuoi/nut_sos_test.dart` (Hủy trong lúc đếm không gửi; chạm lần hai gửi ngay; mất kết nối hiện số liên hệ, không xếp hàng gửi sau) (FR-085, K9)
- [ ] T072 [P] [US9] Màn web phản hồi: Hành chính, Điều dưỡng ghi thay cho người cao tuổi; lệnh đổi loại, gắn ý kiến trong `frontend/src/tinh-nang/phan-hoi/` và test Vitest

**Checkpoint**: K1 → K9 xanh.

---

## Phase 11: Polish & Cross-Cutting

- [ ] T036 [P] Cài `NguoiThanThongKeApi` (016)
- [ ] T037 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/012-family-portal/contracts/api-012.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 012
- [ ] T038 Thay bản giả của 002 (`QuanHeNguoiThanApi`), 004 (`NguoiDaiDienPort`, `BanGhiDonPort`), 005 (`BanGhiDonBanTruPort`), 007 (`NguoiThanKhanCapPort`), 009 (`NguoiThanNhanThongBaoPort`), 011 (`LuotOLaiAnPort`) bằng bản cài của 012 và chạy lại test các feature đó
- [ ] T039 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T040 Rà truy vết: DBR-02, 03, 25 và BR-M10-01 → 09, 11, 12 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-067, FR-068, FR-069, FR-070, FR-071, FR-072, FR-074, FR-075, FR-046d (bổ sung 2026-10-10) — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 012 thay bản giả của 001, 002, 006, 011
- **US1 (P1)**: sau Foundational
- **US3 (P1)**: sau US1
- **US2 (P1)**: sau US3 (danh sách đón)
- **US4, US5, US6 (P2)**: sau US1, song song với nhau
- **US7, US8 (P3)**: sau US1 (US7 dùng cổng của US5)

### Parallel Opportunities

- T003, T005, T008 song song; US4, US5, US6 song song; US7, US8 song song

## Parallel Example: User Story 4

```text
Task: "T022 KiemTraDangKyThamTest + BR_M10_02 + FR_037 + SC_004"
# sau T023–T024:
Task: "T025 web tham + app người thân đăng ký thăm"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US3 → US2
2. **Dừng và kiểm chứng**: quan hệ, quyền, đón; thay bản giả ở 002, 004, 005
3. Thêm US5 (cổng) và US4 (thăm)

### Incremental Delivery

US1 → US3 → US2 → US4 → US5 → US6 → US7 → US8; mỗi bước chạy `./mvnw verify`.

## Notes

- 012 không tạo bản tin, không soạn sẵn nội dung, không tạo khoản chi phí, không tự từ chối yêu cầu (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M10-*)
- Ghi đón chỉ trực tuyến
