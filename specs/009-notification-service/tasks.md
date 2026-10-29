---
description: "Danh sách task triển khai feature 009 – Dịch vụ thông báo"
---

# Tasks: Dịch vụ thông báo (009)

**Input**: Tài liệu thiết kế ở `specs/009-notification-service/` (plan.md, spec.md, research.md B-01 → B-13, data-model.md, contracts/api-009.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000 (khuôn `common.thongbao` ở 000 T034, outbox), 002 (tài khoản, mã thiết bị, quyền xem trường), 008 (`CaTrucApi`, `NhiemVuApi`) đã xong; module nguồn 000 → 008 đã phát sự kiện cài `CoYeuCauThongBao`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 11.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M13: `BE/m13/{api,tiepnhan,nguoinhan,gui,yentinh,khancap,hopthu,lienhe,tracuu,cauhinh,lich}/`; khuôn yêu cầu ở `BE/common/thongbao/`
- Web `frontend/src/tinh-nang/thong-bao/`; app nhân viên `mobile/lib/nhan-vien/thong-bao/`; app người thân `mobile/lib/nguoi-than/thong-bao/`

---

**Cập nhật 2026-10-18** (plan mục "Cập nhật 2026-10-18"; Q-352 → Q-354): sửa T004, T006, T007, T013 → T015, T030, T034, T039; thêm T015a. Mã task cũ giữ nguyên.

## Phase 1: Setup

- [ ] T001 Thêm Firebase Admin SDK (Java) vào `backend/pom.xml`; `firebase_messaging` vào `mobile/pubspec.yaml`, `mobile/pubspec.yaml`; cấu hình khóa dịch vụ FCM qua biến môi trường trong `backend/src/main/resources/application.yml` (research B-05) *(2026-10-21: phần backend xong, `firebase-admin` 9.11.0, khóa qua `FIREBASE_CREDENTIALS`; phần mobile chờ mở mobile)*
- [x] T002 [P] Tạo khung gói `BE/m13/{api,tiepnhan,nguoinhan,gui,yentinh,khancap,hopthu,lienhe,tracuu,cauhinh,lich}/`; ArchUnit `BT/m13/M13ArchTest.java`: chỉ `m13` cài listener `CoYeuCauThongBao`; module khác không gọi kênh gửi trực tiếp
- [x] T003 [P] Tạo bản giả `NguoiThanNhanThongBaoPort` (012) trong `BT/support/gia009/` và bản giả `NhaCungCapTinNhan` ghi lại tin đã gửi (contracts mục 2.1)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m13.api`, bảng nhóm 3 và kỹ thuật, đường xử lý outbox → tiếp nhận → bộ phát, khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T004 [P] Hoàn thiện khuôn `BE/common/thongbao/` (000 T034) theo contracts mục 1.1: biến thể `NhomNhan`, `CachThay` (`MAC_DINH`, `KHONG_THAY`, `NGUOI_THAY_CHI_DINH`), `LoaiThongTin` (`CHUNG`, `SUC_KHOE`, `CHI_PHI`, `THAM_NOM`, `HOP_DONG`), `TRUONG_SUC_KHOE`, và (bổ sung 2026-10-18) `DoiTuongThongTin` (`VE_NGUOI_CAO_TUOI`, `VE_VIEC_NGUOI_THAN`), `MaTruongHopQ354` (`KHAN_CAP_NGUOI_LIEN_HE_CHINH`, `DANH_SACH_TIEP_XUC`, `XAC_NHAN_LAI_NGUYEN_VONG`, `CHUYEN_VIEN`, `QUA_DOI`), hai trường mới của `YeuCauThongBao` (research B-02)
- [x] T005 [P] Tạo `BE/m13/api/`: `ThongBaoTraCuuApi` (`theoNguon`, `xemLuc`), SPI `NguoiThanNhanThongBaoPort`, `NhaCungCapTinNhan`, sự kiện `ThongBaoDaXem`, `ThongBaoKhanCapDaXacNhan`, `ThongBaoKhongLienLacDuoc` (contracts mục 1.2, 1.4, 2.1)
- [x] T006 Tạo `MIG/V009_01__thong_bao.sql`: `kenh_thong_bao` (nhóm 1; `ma` khóa chính `TRONG_UNG_DUNG|CONG_NGUOI_THAN|TIN_NHAN|KHAC_*`; `muc_gan SET('NHE','TRUNG_BINH','KHAN_CAP')`; `cau_hinh JSON`), `yeu_cau_thong_bao` (nhóm 3; `UNIQUE (loai_ban_ghi_nguon, id_ban_ghi_nguon, khoa_su_kien)`; `muc ENUM('NHE','TRUNG_BINH','KHAN_CAP')`; `ket_qua_tiep_nhan ENUM('HOP_LE','BI_TU_CHOI')`; `dau SET('THIEU_MUC','THIEU_LOAI_THONG_TIN','THIEU_RUT_GON','KHONG_CO_NGUOI_NHAN','VIEC_SAU_QUA_DOI','THIEU_DOI_TUONG_THONG_TIN','THIEU_MA_TRUONG_HOP')`; `doi_tuong_thong_tin ENUM('VE_NGUOI_CAO_TUOI','VE_VIEC_NGUOI_THAN') NULL`; `ma_truong_hop ENUM('KHAN_CAP_NGUOI_LIEN_HE_CHINH','DANH_SACH_TIEP_XUC','XAC_NHAN_LAI_NGUYEN_VONG','CHUYEN_VIEN','QUA_DOI') NULL`), `yeu_cau_nhan_trung`, `yeu_cau_phan_noi_dung`, `yeu_cau_nhom_nhan`, `thong_bao` (nhóm 3; `UNIQUE (yeu_cau_id, loai_nguoi_nhan, nguoi_nhan_id)`; `ma_truong_hop` cùng enum, `NULL`), `su_kien_thong_bao` (nhóm 3; `loai` `GIU|TAI_DINH_THOI|GUI|KHONG_GUI|XEM|XAC_NHAN|KHONG_LIEN_LAC_DUOC|AN`; cột sinh `UNIQUE` cho xem lần đầu và xác nhận một lần), `thong_bao_trang_thai` (kỹ thuật dẫn xuất), `lan_gui` (`UNIQUE (thong_bao_id, kenh, lan_thu)`), `ket_qua_lan_gui` (`UNIQUE (lan_gui_id)`; `ket_qua` 7 giá trị), `thiet_bi_day`, `lan_chay_m13` (data-model mục 2 → 5)
- [x] T007 Cài listener duy nhất `BE/m13/tiepnhan/internal/TiepNhanListener.java` (`@ApplicationModuleListener` cho `CoYeuCauThongBao`): kiểm yêu cầu (thiếu mức → `NHE` và dấu; thiếu loại thông tin → `SUC_KHOE`; có nhóm người thân mà thiếu đối tượng → `VE_NGUOI_CAO_TUOI` và dấu `THIEU_DOI_TUONG_THONG_TIN`, FR-001a), chống trùng (`yeu_cau_nhan_trung`), lưu yêu cầu; lỗi không làm hỏng lệnh nguồn (FR-001 → FR-006; research B-03)
- [x] T008 Cài `BE/m13/gui/internal/BoPhatKenh.java`: gửi các `lan_gui` song song trên luồng ảo sau commit; mỗi kết quả một dòng `ket_qua_lan_gui` trong transaction ngắn; executor ưu tiên riêng cho Khẩn cấp; bộ chuyển FCM (`thiet_bi_day`; token lỗi → `vo_hieu_luc`) (research B-03, B-05; NFR-03) *(2026-10-21: bộ phát, kênh trong ứng dụng chạy test với FCM giả; `FcmPushGateway` chưa chạy với FCM thật vì chưa có khóa dịch vụ)*
- [x] T009 Cài `BE/m13/hopthu/internal/TrangThaiThongBao.java` (hàm thuần trên sự kiện theo bảng FR-044, cập nhật `thong_bao_trang_thai`) và test `BT/m13/TrangThaiThongBaoTest.java` (research B-09)
- [x] T010 Cài khung job `BE/m13/lich/ViecNhanhM13.java` (20 giây) và `ViecHangNgayM13.java` (ShedLock; `lan_chay_m13`; chạy bù) và test `BT/m13/ChayBuM13IT.java` (research B-10)
- [x] T011 [P] Cài provider: `PhanLoaiTruongProvider` (nội dung theo `loai_thong_tin`; số điện thoại → `LIEN_HE`), `LuuTruProvider` (`DAI_HON(CFG-M15-04)` và không ngắn hơn nguồn), `GanPhamViProvider` `M13.THONG_BAO` (data-model mục 9, 10)
- [x] T012 [P] Test `BT/m13/FR_003_KiemTraYeuCauIT.java`, `FR_005_ChongTrungIT.java`, `FR_006_LoiKhongHongNguonIT.java` (Q14, Q36)

**Checkpoint**: Đường xử lý thông báo chạy với người nhận rỗng.

---

## Phase 3: User Story 2 – Nhận yêu cầu từ mọi module và xác định người nhận theo phân công hiện tại (Priority: P1) 🎯 MVP

**Goal**: Người nhận theo nhóm và phân công tại thời điểm gửi (008), cách thay, không có người nhận thì báo, gộp người nhận, lọc người thân theo quyền và bản đồng ý, Khẩn cấp cho người thân, người cao tuổi qua đời (FR-007 → FR-015, BR-M13-04, DBR-03 phần 009).

**Independent Test**: Hai ca có đổi người phụ trách giữa ca, tầng thiếu Điều dưỡng phụ trách, một người giữ hai nhiệm vụ; gửi 20 yêu cầu; danh sách người nhận thực tế khớp mong đợi.

### Tests cho User Story 2

- [x] T013 [P] [US2] *(2026-10-26: `SC_004_RecipientTableIT`, 200 yêu cầu qua `RecipientResolver`; cần tài khoản thật nên là test tích hợp)* *(2026-10-22: xong `SubstitutionChainTest`, `ContentFilterTest`, `ConsentScopeTest`; còn `SC_004_NguoiNhanTest` 200 yêu cầu khi đủ nhóm ca trực)* Test đơn vị `BT/m13/ChuoiThayTest.java`, `LocPhanNoiDungTest.java`, `XetPhamViDongYTest.java` (bổ sung 2026-10-18: bảng đối tượng × trong/ngoài phạm vi × có/không mã × mức), `SC_004_NguoiNhanTest.java` (200 yêu cầu)
- [x] T014 [P] [US2] *(2026-10-26: nhóm có ca, Bác sĩ trực ở `BR_M13_04_ShiftGroupsIT` (5); người vắng ca không có trong ca đang diễn ra, `ShiftQueriesIT` của 008)* *(2026-10-22: phần làm được ở `RecipientResolutionIT` (11 kịch bản: BR-M13-04, FR-008, FR-010, FR-011, FR-012, FR-013, FR-014, FR-014a); 2026-10-24: phần sức khỏe, phạm vi đồng ý, FR-015 xong ở `FR_014_015_ConsentAndDeathIT`; còn nhóm có ca, vắng ca chờ `CaTrucApi` (008))* Test `BT/m13/BR_M13_04_NguoiNhanTheoPhanCongIT.java`, `FR_010_CachThayIT.java`, `FR_011_KhongNguoiNhanIT.java`, `FR_012_GopNguoiNhanIT.java`, `FR_013_LocNguoiThanIT.java`, `FR_014_KhanCapNguoiThanIT.java`, `FR_014_NhanThongBaoDongYIT.java`, `FR_014a_MaTruongHopIT.java` (bổ sung 2026-10-18; Q38 → Q42), `FR_015_QuaDoiIT.java`: người thân chỉ thấy phần được phép (`phan_duoc_xem`); không có người nhận → Quản lý viện nhận thay "chỉ nhận dạng"; một người một thông báo mỗi yêu cầu (Q8 → Q15, Q22 → Q25)

### Implementation cho User Story 2

- [x] T015 [US2] *(2026-10-26: `OnShiftStaff`, `OnDutyDoctor` qua `ShiftApi.staffOnShift`, `onDutyDoctors` (luật "Đổi api" trên M09, chủ dự án duyệt), chứng chỉ qua `LicenseApi.valid` ngày gửi)* *(2026-10-22: `RecipientResolver`, `SubstitutionChain`, `ContentFilter`, `NotificationCreation` xong, nối vào tiếp nhận; 2026-10-24: bản đồng ý, qua đời qua `ResidentFacts` (`ConsentApi`, `ResidentApi` của 001); còn `OnShiftStaff`, `OnDutyDoctor`, vắng ca (`CaTrucApi`); ghi chú triển khai ở plan)* Cài `BE/m13/nguoinhan/internal/XacDinhNguoiNhan.java` (biến thể `NhomNhan` qua `NhiemVuApi`, `CaTrucApi`, `PhamViApi`, `NguoiThanNhanThongBaoPort`; `NguoiCoQuyenDuyet` dùng thẳng danh sách tài khoản 000 truyền, không tra lại `NguoiDuyetApi`, sửa theo plan 004), `ChuoiThay.java`, `LocPhanNoiDung.java` (`QuyenXemTruongApi`, bản đồng ý) và nối vào `TiepNhanListener` để thêm `thong_bao` + `lan_gui` dự kiến (research B-04)
- [x] T015a [US2] *(2026-10-22: hàm thuần `ConsentScope` xong, đã gọi trong `RecipientResolver`; 2026-10-24: phạm vi theo `ConsentApi.current(...).notificationConsent` (Q-357))* Cài hàm thuần `BE/m13/nguoinhan/internal/XetPhamViDongY.java` (đối tượng thông tin; bản đồng ý Hiệu lực với `dongYThongBao` qua `BanDongYApi.hienHanh`; người ngoài phạm vi: có mã thì gửi và ghi `thong_bao.ma_truong_hop`, không mã thì không gửi, Khẩn cấp không mã thì lỗi gửi `THIEU_MA_TRUONG_HOP`) và gọi trước `LocPhanNoiDung` trong `XacDinhNguoiNhan` (FR-001a, FR-014, FR-014a; research B-04; bổ sung 2026-10-18)

**Checkpoint**: Người nhận đúng theo phân công.

---

## Phase 4: User Story 1 – Thông báo khẩn cấp gửi đồng thời, theo dõi xác nhận và lượt nhắc gọi (Priority: P1)

**Goal**: Khẩn cấp gửi song song ≤ 10 giây; xác nhận trực tuyến; "Xác nhận và tiếp nhận"; lượt nhắc gọi khi chưa xác nhận sau CFG-M13-04; kết quả gọi; chuyển nhắc khi quá hạn hoặc hết ca; chuỗi gọi người thân; một lượt Chờ mỗi (người cần gọi, người cao tuổi) (FR-026 → FR-032, BR-M13-02, NFR-03).

**Independent Test**: Khẩn cấp với sáu người nhận (D, P, T, B, Q, M); bốn xác nhận, hai không → thời điểm gửi, trạng thái từng người, lượt nhắc gọi đúng người, đúng hạn.

### Tests cho User Story 1

- [x] T016 [P] [US1] *(2026-10-26: `CallReminderTargetTest` (5) cho hàm thuần `CallReminderTarget`)* Test đơn vị `BT/m13/NguoiNhacGoiTest.java`
- [ ] T017 [P] [US1] *(2026-10-23: xong phần xác nhận `FR_026_AcknowledgeIT` (3); 2026-10-27: xong lượt nhắc gọi, gộp, gửi thất bại, chuyển nhắc, tự xác nhận dừng ở `BR_M13_02_CallReminderIT` (7), kết quả gọi, chuỗi gọi, lần gọi bổ sung, quyền gọi, đọc lượt ở `FR_029a_CallResultIT` (4); còn `FR_026a` xác nhận và tiếp nhận (lệnh tiếp nhận của 007), dừng theo `YeuCauDungNhacGoiDien`, `DaCoKetQuaXacNhanLai` (sự kiện của 007 chưa khai báo); còn lượt nhắc gọi, chuỗi gọi chờ `CaTrucApi` (008))* Test `BT/m13/SC_001_KhanCapDongThoiIT.java`, `BR_M13_02_LuotNhacGoiIT.java`, `FR_027a_GopLuotIT.java`, `FR_028_ThatBaiMoiKenhIT.java`, `FR_029a_ChuoiGoiIT.java`, `FR_030a_ChuyenNhacIT.java`, `FR_026a_XacNhanVaTiepNhanIT.java`, `FR_032_MotLuotIT.java`: song song; một lượt tự động mỗi thông báo (`UNIQUE`); một lượt Chờ mỗi cặp (`khoa_cho`); `YeuCauDungNhacGoiDien`, `DaCoKetQuaXacNhanLai` dừng lượt; xác nhận và tiếp nhận gọi lệnh tiếp nhận của 007 (Q1 → Q7, Q32 → Q35)

### Implementation cho User Story 1

- [x] T018 [US1] *(2026-10-27: `V009_04__call_reminders.sql` (số 02 đã dùng cho thiết bị đẩy); bảng chuyển nhắc thêm cột bậc `tier`; tên tiếng Anh ở glossary mục 15.6)* Tạo `MIG/V009_02__nhac_goi.sql`: `luot_nhac_goi` (nhóm 3; `chuoi_goi_id`), `luot_nhac_goi_thong_bao` (`thong_bao_id UNIQUE`), `chuyen_nhac` (`ly_do` `KHOI_TAO|QUA_HAN|HET_CA|GOP_CAP_NHAT|NHAC_LAI_QLV`; `han_luc = thoi_diem + CFG-M13-04`), `luot_nhac_goi_trang_thai` (kỹ thuật; cột sinh `khoa_cho` `UNIQUE`), `ket_qua_goi` (nhóm 3; `ket_qua` `DA_LIEN_LAC|KHONG_LIEN_LAC`; `ly_do` `KHONG_NGHE_MAY|SAI_SO|KHONG_CO_SO|...`) (data-model mục 6)
- [ ] T019 [US1] *(2026-10-23: xong phần xác nhận `UrgentAcknowledgement` (`m13/internal/urgent`); 2026-10-27: xong lượt nhắc gọi, gộp, chuyển nhắc, kết quả gọi (lần gọi bổ sung), chuỗi gọi theo `callOrder`, dừng khi tự xác nhận, đính chính `M13.CALL_RESULT`; còn xác nhận và tiếp nhận, listener hai sự kiện của 007 (D), dừng khi người cần gọi không còn đủ điều kiện (chờ sự kiện quan hệ của 012); 2026-10-26: hàm thuần người được nhắc gọi `CallReminderTarget` (T016), bậc Quản lý viện trả mọi người đủ điều kiện, chọn một người làm cùng lượt nhắc; còn xác nhận và tiếp nhận (lệnh của 007, D), lượt nhắc gọi, chuyển nhắc, chuỗi gọi (chờ `CaTrucApi`))* Cài `BE/m13/khancap/internal/KhanCapService.java`: xác nhận (bắt buộc trực tuyến), xác nhận và tiếp nhận (gọi lệnh tiếp nhận cảnh báo/sự cố của 007), lượt nhắc gọi (`NguoiNhacGoi`, gộp vào lượt Chờ), kết quả gọi (lần gọi bổ sung), chuyển nhắc, chuỗi gọi theo `thuTuGoi`; listener `YeuCauDungNhacGoiDien`, `DaCoKetQuaXacNhanLai`; `DinhChinhDuocProvider` `M13.KET_QUA_GOI` (research B-07, B-08)
- [x] T020 [US1] *(2026-10-27: hai việc 20 giây `CREATE_CALL_REMINDERS`, `HAND_OFF_CALL_REMINDERS` chạy trong `M13FastJob`)* Đăng ký việc 20 giây: tạo lượt nhắc khi quá CFG-M13-04 chưa xác nhận; chuyển nhắc quá hạn hoặc hết ca; nhắc lại Quản lý viện
- [ ] T021 [US1] *(2026-10-23: xong `POST /notifications/{id}/acknowledge` (`UrgentController`); 2026-10-27: xong `GET /me/call-reminders`, `GET /call-reminders/{id}`, `POST /call-reminders/{id}/results`; còn `/acknowledge-and-take-charge` chờ 007; `ApiContractIT` chỉ còn `/acknowledge-and-take-charge` chờ cài (2026-10-27))* Tạo `BE/m13/khancap/web/KhanCapController.java`: `POST /thong-bao/{id}/xac-nhan`, `/xac-nhan-va-tiep-nhan`, `GET /toi/luot-nhac-goi`, `GET /luot-nhac-goi/{id}`, `POST /luot-nhac-goi/{id}/ket-qua`
- [ ] T022 [P] [US1] Tạo app `mobile/lib/nhan-vien/thong-bao/` (nhận đẩy, xác nhận hai thao tác, xác nhận và tiếp nhận, ghi kết quả gọi) và test `mobile/test/nhan-vien/nhan_day_test.dart`, `xac_nhan_hai_thao_tac_test.dart` (SC-008), `xac_nhan_bat_buoc_truc_tuyen_test.dart`, `ghi_ket_qua_goi_test.dart`

**Checkpoint**: P1 hoàn tất; đo SC-001 (≤ 10 giây).

---

## Phase 5: User Story 3 – Chọn kênh theo mức và gửi qua kênh ngoài ứng dụng (Priority: P2)

**Goal**: `ChonKenh` theo mức và ca; tin nhắn qua `NhaCungCapTinNhan` (mặc định chưa cấu hình, Q-06); gộp tin nhắn Trung bình theo CFG-M13-03; gửi lại CFG-M13-05; chặn nội dung sức khỏe ở kênh ngoài; liên hệ trực tiếp; cấu hình kênh (FR-016 → FR-021, BR-M13-01).

**Independent Test**: Ba yêu cầu Nhẹ, Trung bình, Khẩn cấp cho cùng người khi tin nhắn bật rồi tắt → lần gửi theo kênh và nội dung tin nhắn đúng.

### Tests cho User Story 3

- [x] T023 [P] [US3] *(2026-10-23: `ChannelSelectionTest`, `SmsBatchingTest` (gồm kiểm nội dung kênh ngoài))* Test đơn vị `BT/m13/ChonKenhTest.java`, `KhungGopTest.java`
- [x] T024 [P] [US3] *(2026-10-26: `FR_016a_OffShiftIT` (2, gồm tin tổng hợp gửi nhân viên có tầng và ca))* *(2026-10-23: xong `BR_M13_01_ChannelSelectionIT` (gồm FR-018 gửi lại, FR-020), `FR_016b_SmsBatchingIT`, `FR_021_DirectContactIT`, thêm `FR_016_ChannelConfigIT`; còn `FR_016a_NgoaiCaIT` chờ `CaTrucApi` của 008)* Test `BT/m13/BR_M13_01_ChonKenhIT.java`, `FR_016a_NgoaiCaIT.java`, `FR_016b_GopTinNhanIT.java`, `FR_018_GuiLaiIT.java`, `FR_020_NoiDungKenhNgoaiIT.java`, `FR_021_LienHeTrucTiepIT.java`: nhân viên ngoài ca; một khung gộp mở mỗi người; gửi lại không trùng; nội dung chứa trường sức khỏe bị từ chối ở kênh ngoài; Trung bình thất bại mọi kênh tới người thân → liên hệ trực tiếp (Q16 → Q19, Q26)

### Implementation cho User Story 3

- [x] T025 [US3] *(2026-10-23: `V009_03__out_of_app_channels.sql`; tên bảng tiếng Anh ở glossary mục 15.6)* Tạo `MIG/V009_03__kenh_ngoai.sql`: `khung_gop_tin_nhan` (cột sinh `khoa_mo` `UNIQUE`), `lien_he_truc_tiep` (nhóm 2; `thong_bao_id UNIQUE`; `trang_thai` `CHO_LIEN_HE|DA_LIEN_HE|KHONG_LIEN_HE_DUOC`), `ket_qua_lien_he` (nhóm 3; `ly_do` bắt buộc khi không liên hệ được)
- [x] T026 [US3] *(2026-10-26: FR-016a qua `ShiftApi.onShift` trong `OutOfAppPlanner`; tin tổng hợp gửi nhân viên nêu tầng (003 `LocationStructureApi`) và ca (008) ở `SmsWindowCloser`)* *(2026-10-23: xong `ChannelSelection`, `SmsBatching`, `OutOfAppContent`, `UnconfiguredSmsProvider`, `SmsSender`, việc `CLOSE_SMS_WINDOWS`, `RESEND_OUT_OF_APP`; còn FR-016a ngoài ca và danh sách tầng/khu, ca trong tin tổng hợp gửi nhân viên, chờ `CaTrucApi` (008) và tên tầng (003))* Cài `BE/m13/gui/internal/ChonKenh.java`, `KhungGop.java`, `KiemTraNoiDungKenhNgoai.java`, bean `ChuaCauHinh` của `NhaCungCapTinNhan`; việc 20 giây đóng khung gộp, gửi lại kênh ngoài (research B-05)
- [ ] T026a [US3] *(Bổ sung 2026-10-19; chờ chủ dự án chốt nhà cung cấp SMS, Q-06)* Cài bộ chuyển `BE/m13/gui/internal/NhaCungCapTinNhanSms.java` gọi API nhà cung cấp, đổi phản hồi thành `KetQua` (thành công, lỗi tạm gửi lại, lỗi vĩnh viễn, mã lỗi nhà cung cấp), khóa API từ biến môi trường, chỉ nạp khi có cấu hình; test đơn vị với máy chủ HTTP giả cho phản hồi thành công và lỗi (research B-05)
- [x] T027 [US3] *(2026-10-23: `DirectContactService`, `DirectContactDetector` (`DIRECT_CONTACTS`), `DirectContactReminder` (`REMIND_DIRECT_CONTACTS`), `DirectContactResultCorrection`, `ChannelService`)* Cài `BE/m13/lienhe/internal/LienHeTrucTiepService.java` (`DinhChinhDuocProvider` `M13.KET_QUA_LIEN_HE`) và `BE/m13/cauhinh/internal/KenhService.java` (Quản lý viện bật/tắt, gắn kênh khác vào mức; lý do; nhật ký)
- [x] T028 [US3] *(2026-10-23: bốn endpoint, `ApiContractIT` so đủ; màn web tab Liên hệ trực tiếp, Kênh trong `frontend/src/features/notifications/`)* Tạo endpoint `GET /lien-he-truc-tiep`, `POST /lien-he-truc-tiep/{id}/ket-qua`, `GET /kenh-thong-bao`, `PUT /kenh-thong-bao/{ma}`; màn web liên hệ trực tiếp và cấu hình kênh trong `frontend/src/tinh-nang/thong-bao/`

**Checkpoint**: Kênh ngoài chạy độc lập.

---

## Phase 6: User Story 4 – Người thân nhận thông báo theo quyền và giờ yên tĩnh (Priority: P2)

**Goal**: Giờ yên tĩnh CFG-M13-02 cho người thân với Nhẹ/Trung bình; giữ và giải phóng; đổi tham số tính lại; Khẩn cấp không giữ (FR-022 → FR-025, BR-M13-03).

**Independent Test**: M (liên hệ chính), R (đại diện, có quyền xem chi phí), N (không có quyền xem sức khỏe); gửi lúc 22:30 và 10:00 → ai nhận, lúc nào, phần nội dung nào.

### Tests cho User Story 4

- [x] T029 [P] [US4] *(2026-10-23: xong `QuietHoursTest`, `BR_M13_03_QuietHoursIT` (4), `FR_024_ParameterChangeIT`; 2026-10-24: kịch bản qua đời, bản đồng ý lúc giải phóng ở `FR_014_015_ConsentAndDeathIT`)* Test đơn vị `BT/m13/GioYenTinhTest.java` và `BT/m13/BR_M13_03_GioYenTinhIT.java`, `FR_024_DoiThamSoIT.java`: giữ tới hết giờ yên tĩnh; khi giải phóng không còn đủ điều kiện → `KHONG_GUI`; người cao tuổi qua đời → `KHONG_GUI`; đổi CFG-M13-02 tính lại thời điểm dự kiến (Q20, Q21, Q24, Q27)
- [x] T040 [P] [US4] *(Bổ sung 2026-10-18, Q-366)* Test `BT/m13/FR_022a_NguoiCaoTuoiNhanIT.java`: nhóm (f) chỉ nhận Nhẹ phần `CHUNG`, sai thì `BI_TU_CHOI`; giờ yên tĩnh áp cho người cao tuổi; không có tài khoản thì không có người nhận và không vào lỗi gửi (FR-007, FR-022, FR-022a; quickstart R1 → R3)

### Implementation cho User Story 4

- [x] T030 [US4] *(2026-10-23: `QuietHoursPolicy`, giữ trong `NotificationCreation`, `QuietHoursRelease` (việc `RELEASE_QUIET_HOURS`), `QuietHoursRescheduler` xong; 2026-10-24: `ResidentDeathListener` nghe `ResidentStatusChanged` sang `DECEASED`, giải phóng xét bản đồng ý hiện hành và qua đời)* Cài `BE/m13/yentinh/internal/GioYenTinh.java` (giữ trong transaction tiếp nhận, giải phóng ở việc 20 giây, kiểm lại điều kiện lúc giải phóng, gồm `XetPhamViDongY` với bản đồng ý hiện hành) và listener `ThamSoDaDoi(CFG-M13-02)`, `NguoiCaoTuoiDaChuyenTrangThai(GHI_NHAN_QUA_DOI)` (research B-06)
- [ ] T031 [P] [US4] Tạo app `mobile/lib/nguoi-than/thong-bao/` (nhận đẩy, hộp thông báo)
- [x] T041 [US4] *(2026-10-23: `RecipientGroup.Resident`; `push_device.app` đã có `RESIDENT_APP` từ `V009_02` nên không cần `V009_04`)* *(Bổ sung 2026-10-18)* Thêm `NhomNhan.NguoiCaoTuoi` vào `common.thongbao`, phân giải trong `BE/m13/nguoinhan/internal/XacDinhNguoiNhan.java` qua `TaiKhoanApi.theoNguoiCaoTuoi`; kiểm FR-022a trong kiểm tra yêu cầu; `GioYenTinh` áp cho `NGUOI_CAO_TUOI`; `thiet_bi_day.ung_dung` thêm `APP_NGUOI_CAO_TUOI` (migration `MIG/V009_04__nguoi_cao_tuoi_nhan.sql`)
- [ ] T042 [P] [US4] *(Bổ sung 2026-10-18)* Tạo `mobile/lib/nguoi-cao-tuoi/thong-bao/` (nhận đẩy, hộp thông báo chữ lớn) và test `mobile/test/nguoi-cao-tuoi/thong_bao_test.dart`

**Checkpoint**: Thông báo người thân chạy đúng.

---

## Phase 7: User Story 5 – Hộp thông báo, đã xem và lịch sử gửi (Priority: P3)

**Goal**: Hộp thông báo hiển thị theo quyền hiện hành; đẩy web qua SSE; đã xem; ẩn; lịch sử gửi theo người nhận và bản ghi nguồn; lỗi gửi; dữ liệu cho module nguồn (FR-033 → FR-038, BR-M13-05, BR-M10-06 phần 009).

**Independent Test**: 10 thông báo cho D, D mở 6 → số chưa xem, thời điểm xem, liên kết nguồn; Quản lý viện tra theo người nhận và bản ghi nguồn.

### Tests cho User Story 5

- [x] T032 [P] [US5] *(Xong 2026-10-22: `InboxIT`, `NotificationLookupIT`)* Test `BT/m13/FR_034_HienThiTheoQuyenIT.java`, `BR_M13_05_LichSuGuiIT.java`, `FR_035_DuLieuChoNguonIT.java`: nội dung hiển thị theo quyền hiện hành (không theo lúc gửi); xem lần đầu một lần; `ThongBaoTraCuuApi.theoNguon` đủ trường (Q28 → Q31)

### Implementation cho User Story 5

- [x] T033 [US5] *(Xong 2026-10-22: `InboxService`, `NotificationStreams`, `InboxController`; ghi chú triển khai ở plan)* Cài `BE/m13/hopthu/internal/HopThuService.java` (danh sách, chi tiết, xem, ẩn; phát `ThongBaoDaXem`), luồng SSE `BE/m13/hopthu/web/LuongThongBaoController.java` (`GET /thong-bao/luong`), đăng ký thiết bị đẩy (`POST /thiet-bi-day`) (research B-11)
- [x] T034 [US5] *(Xong 2026-10-22: `NotificationLookupService`, `NotificationLookupController`; lượt nhắc gọi trong kết quả rỗng tới US1)* Cài `BE/m13/tracuu/internal/TraCuuService.java` (`ThongBaoTraCuuApi`; bản cài `NhatKyThongBaoPort` của 007 ủy cho `theoNguon`; lỗi gửi) và endpoint `GET /toi/thong-bao`, `/toi/thong-bao/{id}`, `POST /toi/thong-bao/{id}/an`, `GET /lich-su-thong-bao`, `GET /loi-gui` (research B-12)
- [x] T035 [P] [US5] *(2026-10-23: xong hộp thông báo, nút "Xác nhận đã nhận", SSE kèm hỏi lại, lịch sử, lỗi gửi (`frontend/src/features/notifications/`, 15 Vitest); 2026-10-24: lọc theo người cao tuổi (`GET /residents` của 001), 17 Vitest)* Tạo màn web `frontend/src/tinh-nang/thong-bao/HopThongBao.jsx` (Khẩn cấp lên đầu, xác nhận), luồng SSE (dự phòng polling), lịch sử, lỗi gửi với test Vitest

**Checkpoint**: Mọi user story của 009 chạy độc lập.

---

## Phase 8: Polish & Cross-Cutting

- [x] T036 [P] *(2026-10-23: `ApiContractIT` so api-009 từ T034; `M13RecordGroupTriggerIT` liệt kê tám bảng nhóm 3 của 009 vì M13 ghi bằng JDBC, `RecordGroupTriggerIT` chỉ tìm qua `@Entity`)* Mở rộng `BT/common/HopDongApiTest.java` với `specs/009-notification-service/contracts/api-009.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 009
- [ ] T037 Chạy kiểm thử đầu–cuối với sự kiện thật của 001 → 008 (một sự kiện mỗi module) và đo NFR-03 (Khẩn cấp ≤ 10 giây)
- [ ] T038 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T039 Rà truy vết data-model mục 11: BR-M13-01 → 05, FR-007 (f), FR-022a, BR-M10-06 (phần 009), DBR-03 (phần 009), FR-005, FR-012, FR-032, FR-001a, FR-014 và FR-014a (bổ sung 2026-10-18) có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-039, FR-040, FR-041, FR-042, FR-043, FR-043a, FR-043b — mỗi FR có task hoặc test tương ứng (constitution XV)
- [ ] T043 *(bổ sung 2026-10-29, research R-15 bản 2 của 000; chờ A xong 000 T108)* Gộp bảng đã cài theo data-model mục 0: migration `V009_95` tạo bảng gộp hoặc cột `JSON`, chép dữ liệu (dòng chép sang bảng chung ghi `chi_tiet.legacy`), đổi khóa ngoại và `tep.chu_so_huu_id`, `DROP TABLE` bảng cũ; giữ id của bảng nhóm 3 là đích của `dinh_chinh`, `nhat_ky` (R-15 mục 5); sửa entity, lớp truy cập dữ liệu, service sang `CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`; job ghi lần chạy ra file log. Test: toàn bộ test của module xanh như trước khi gộp, test bảo vệ nhóm dữ liệu (trigger, quyền `eldercare_app`) cho bảng gộp, test migration chép đủ số dòng trên dữ liệu demo; không đổi API (`ApiContractIT` xanh)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US2 (P1)**: sau Foundational
- **US1 (P1)**: sau US2 (cần người nhận)
- **US3 (P2)**: sau US2
- **US4 (P2)**: sau US2
- **US5 (P3)**: sau US2

### Parallel Opportunities

- T004, T005, T011, T012 song song
- US3, US4, US5 song song sau US2
- App nhân viên, app người thân, web song song khi controller đã có

## Parallel Example: User Story 1

```text
Task: "T016 NguoiNhacGoiTest"
Task: "T017 SC_001/BR_M13_02/FR_027a/FR_028/FR_029a/FR_030a/FR_026a/FR_032 IT"
# sau T018–T021:
Task: "T022 app nhân viên thông báo"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US2 → US1
2. **Dừng và kiểm chứng**: sự kiện Khẩn cấp của 007 tới đúng người ≤ 10 giây; lượt nhắc gọi
3. Thêm US3, US4, US5

### Incremental Delivery

US2 → US1 → US3 → US4 → US5; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- 009 không tạo yêu cầu gọi điện hay việc giao; không tự đổi mức hay quyết định sự kiện cần thông báo (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M13-*)
- Xác nhận Khẩn cấp bắt buộc trực tuyến
