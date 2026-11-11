---
description: "Danh sách task triển khai feature 017 – Số dư và thu chi của người cao tuổi (lập lại 2026-10-22)"
---

# Tasks: Số dư và thu chi của người cao tuổi (017)

**Input**: Tài liệu thiết kế ở `specs/017-resident-balance-ledger/` (plan.md, spec.md, research.md E-01 → E-14, data-model.md, contracts/api-017.yaml, contracts/module-interfaces.md, quickstart.md)

**Lập lại 2026-10-22** theo spec viết lại (Q-377 → Q-379, Q-381, Q-385, Q-386). Bản tasks 2026-10-10 (phiếu thu, chốt quỹ, sao kê ngân hàng, giao dịch đảo) bị bỏ toàn bộ; chưa task nào của bản cũ được làm nên đánh số lại từ T001.

**Prerequisites**: 000, 002 đã xong Phase 2 (002 có `Function` `BALANCE_LEDGER`, `STATEMENT_REPORT`, `PAYMENT_NOTICE`, `SETTLEMENT_RECORD`); 010 có port nội bộ `ChiPhiChoSoDuPort` (gồm `bangDaChot`, 010 T037), khai báo `SoDuMuaHoPort`, sự kiện `BangChiPhiDaChot`; 001, 004 có `api` (`TrangThaiDatCocPort`, `MucViecSauQuaDoiPort`, `HopDongDocApi`, `HoSoKetThucDocApi` 004 T108, sự kiện hợp đồng, hồ sơ kết thúc, qua đời, `NgoaiLeKetThucDaDuyet`); 012 có `QuyenNguoiThanApi` (bản giả được); `common.bieumau` (007 T104). Phụ thuộc chưa xong dùng bản giả của người khai báo (docs/team-plan.md mục 3).

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Tên gói, file là tên logic; đặt theo [backend/README.md](../../backend/README.md), research R-02 của 000. Module M11 (phần 017): `BE/billing/sodu/{so,giaodich,tinhtrang,quyettoan,chungtu,xuat,lich}/`; bổ sung `BE/billing/api/`
- Web `frontend/src/tinh-nang/so-du/`, `frontend/src/tinh-nang/cong-nguoi-than/so-du/` (đặt theo [frontend/README.md](../../frontend/README.md)); app người thân `mobile/lib/nguoi-than/so-du/`

---

## Phase 1: Setup

- [x] T001 *(Xong 2026-11-08, A: gói `billing.balance` (`internal/{ledger, transaction, status, settlement, document, export}`, `web`, `scheduling`); `LayerArchTest.FEATURE_PARTS` cho phần của feature trong module; `M11ArchTest`: `CostForBalancePort` chỉ `billing.balance..`, procurement gọi, `Q_306_balanceSchedulersDoNotPostTransactions`)* Tạo khung gói `BE/billing/sodu/{so,giaodich,tinhtrang,quyettoan,chungtu,xuat,lich}/`; mở rộng `BT/billing/M11ArchTest.java` (010): chỉ `billing.sodu` dùng `ChiPhiChoSoDuPort`; gói `..lich..` không tạo giao dịch (research E-01, E-02)
- [x] T002 [P] *(Xong 2026-11-08, A: `zxing.version` 3.5.4 trong `backend/pom.xml`, biên dịch được; `common.document.PdfDocument` và fastexcel đã có)* Thêm ZXing core 3.5.4 vào `backend/pom.xml` (đã duyệt 2026-10-22; research E-04, 007 research S-26); kiểm `common.bieumau` (007 T104) và fastexcel (010) đã có
- [x] T003 [P] *(Xong 2026-10-22: `common.file.FileTypes`, `FileTypesTest`; ghi chú triển khai ở plan)* Bổ sung kiểu `.xlsx` cho tệp do hệ thống tạo trong `FileApi.upload` của `BE/common/file/` (không mở cho `POST /tep`) và test (research E-11)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Bảng, khai báo `billing.api` của 017, bản cài SPI, provider, bản giả.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T004 *(Xong 2026-11-08, A: `V017_01__resident_balance.sql`: `resident_balance` (giữ bảng, lý do ở plan "Ghi chú triển khai"), `balance_transaction` (`CHECK` sổ, loại, dấu, phương thức; `UNIQUE(cost_statement_id)`; trigger khóa dòng, chặn xóa); bảng khác gộp vào bảng chung khi dùng)* Tạo `MIG/V017_01__so_du.sql` theo data-model mục 2 → 7: `so_du` (`ma_nop_tien UNIQUE`), `giao_dich` (`CHECK` cặp sổ, loại, dấu số tiền; `ma_giao_dich_ngan_hang` chỉ mục thường; `bang_chi_phi_id UNIQUE`; trigger nhóm 2 → 3), `lich_su_tinh_trang` (`UNIQUE (nct_id, ngay, nguon)`), `phieu_bao_phi` (`UNIQUE ma_phieu`, `UNIQUE bang_chi_phi_id`), `bang_quyet_toan` (`UNIQUE (nct_id, phien_ban)`, cột sinh `khoa_hien_hanh UNIQUE`, trigger), `lan_xuat_so_du`, `lan_tai_chung_tu`, `su_kien_m11sd_da_xu_ly`, `lan_chay_m11sd`; trigger nhóm 3 theo mẫu 000 **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `so_du` → `trang_thai_nct`; `lich_su_tinh_trang` → `lich_su_chung`; `phieu_bao_phi`, `bang_quyet_toan` → `su_kien_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [x] T005 *(Xong 2026-11-08, A: `BalanceApi`, `BalanceStatisticsApi`, DTO ở `billing.api.dto`; sự kiện `BalanceStatusChanged`, `BalanceDocumentIssued` ở `billing.api.events`, `DepositStatusChanged`, `SettlementConditionChanged` ở `admission.api.events` (tránh vòng module); bản giả `FakeBalances`, `FakeBalanceStatistics`, `FakeBalanceConfig`)* [P] Khai báo ở `BE/billing/api/`: `SoDuApi` (`soDu`, `dieuKienQuyetToan`, `chungTu`), `SoDuThongKeApi` (`tinhTrang`, `thuChi`), sự kiện `TrangThaiDatCocDaDoi`, `DieuKienQuyetToanDaDoi`, `TinhTrangSoDuDaDoi`, `ChungTuDaPhatHanh` (module-interfaces mục 2.2 → 2.4) và bản giả `BT/support/fake/billing/` cho 004, 012, 016
- [x] T006 *(2026-11-08, A: dùng lại `FakeCostForBalance`, `FakeFamilyRights`, `FakeDepositStatuses`, `FakeContractSchedules`; còn `finalizedStatement` của `CostForBalancePort` (E, 010 T037) cho Phase 9; bản giả `ResidencyEndingReadApi` B viết 2026-11-08: `support/fake/admission/FakeResidencyEndingReads` + `FakeResidencyEndingReadsConfig`, `BalanceTestBase` import)* [P] Bản giả cho test của 017 trong `BT/support/`: `ChiPhiChoSoDuPort` (010, có `bangDaChot`), `HoSoKetThucDocApi` và phát sự kiện 004, `QuyenNguoiThanApi` (012) — dùng bản giả do người khai báo viết nếu đã có (docs/team-plan.md mục 3) *(2026-11-09, B: xong. `FakeCostForBalance` có `statement(...)` cho `finalizedStatement`, `FakeResidencyEndingReads` của B, `FakeFamilyRights`, `FakeDepositStatuses`, `FakeContractSchedules`, `FakeRepresentatives` đủ cho test 017; US7, US9, T040 dùng chúng)*
- [x] T007 *(2026-11-08, A: phân loại trường, lưu giữ (kèm `referencing`, trang), gắn phạm vi cho `M11.BALANCE`, `M11.BALANCE_TRANSACTION` (`BalanceGovernanceConfig`, `T007_BalanceProvidersIT`); `FilePermissionProvider` của phiếu báo phí, bảng quyết toán, bản ký, file xuất chuyển B làm cùng bản ghi chủ (Phase 8–10); 2026-11-08, B: xong `M11.SETTLEMENT_STATEMENT`, `M11.SETTLEMENT_SIGNED_COPY` (`SettlementStatementFiles`, `SettlementSignedCopyFiles`); 2026-11-08, B Phase 9: xong `M11.PAYMENT_NOTICE` (`PaymentNoticeFiles`); 2026-11-08, B Phase 10: xong `M11.BALANCE_EXPORT` (`BalanceExportFiles`))* Cài provider trong `BE/billing/sodu/`: `PhanLoaiTruongProvider` (`TAI_CHINH`), `LuuTruProvider` (`THEO_HO_SO`), `GanPhamViProvider`, `QuyenTepProvider` (`M11.PHIEU_BAO_PHI`, `M11.BANG_QUYET_TOAN`, `M11.BAN_KY_QUYET_TOAN`, `M11.FILE_XUAT`) theo research E-12; khóa `@RequiresPermission` theo bảng E-12
- [ ] T008 [P] *(Chia 2026-11-08: `VietQrPayloadTest` và `VietQrPayload` do B làm cùng phiếu báo phí (Phase 9), `TrangThaiDatCocTest` do A làm; phần A xong 2026-11-08: `DepositStatusRule`, `DepositStatusTest`)* Test bảng các hàm thuần dùng chung về sau: `BT/billing/sodu/VietQrPayloadTest.java` (test vector, CRC16) và `TrangThaiDatCocTest.java` (research E-04, E-05, E-13) *(2026-11-08, B: phần `VietQrPayload` + `VietQrPayloadTest` xong, 4 test, `billing.balance.internal.document.domain`; còn `TrangThaiDatCocTest` của A, nên giữ [ ])*

**Checkpoint**: Bảng, API, bản giả sẵn sàng.

---

## Phase 3: User Story 1 – Mở sổ, cấp mã nộp tiền, người thân xem số dư và thông tin nộp tiền (Priority: P1) 🎯 MVP

**Goal**: Sổ và mã nộp tiền mở khi hợp đồng đầu tiên Chờ ký; hiển thị theo vai trò; cổng người thân (FR-001 → FR-004).

**Independent Test**: Quickstart Q1, Q2.

### Tests cho User Story 1

- [x] T009 *(Xong 2026-11-08, A: `US1_BalanceIT` (Q1, Q1a, Q2 gộp một IT theo ngân sách AGENTS mục 4), `PaymentCodeTest`)* [P] [US1] Test `BT/billing/sodu/MoSoIT.java`, `SoDuHienThiIT.java`, `CongSoDuIT.java` (Q1, Q2): một sổ, một mã mỗi hồ sơ; sự kiện lặp, trả về nháp, gia hạn không mở thêm; hồ sơ quay lại có sổ mới; HC chỉ thấy số dư, cọc, đặt cọc; DD 403; người thân không có quyền xem chi phí 403; thanh toán bảng chỉ hiện tổng (US1 #1 → #6)

### Implementation cho User Story 1

- [x] T010 *(Xong 2026-11-08, A: `ledger.service.LedgerOpening` (nghe `ContractStatusChanged` → Chờ ký trong transaction lệnh gửi ký), `PaymentCode` (Crockford mod 37, bỏ mã có ký tự kiểm tra đặc biệt), `BalanceQueries`, `CollectionAccounts` (VietQR), `BalanceController`; cổng kiểm quyền xem chi phí qua `FamilyRelationshipApi.rightEnabled` của 002)* [US1] Cài `BE/billing/sodu/so/` : listener `HopDongDaDoiTrangThai` mở sổ (idempotent), sinh `ma_nop_tien` có ký tự kiểm tra, `VietQrPayload` (CFG-M11-10), `GET /nguoi-cao-tuoi/{nctId}/so-du`, `GET /nguoi-cao-tuoi/{nctId}/giao-dich`, `GET /so-du/tim-theo-ma`, `GET /cong/nguoi-cao-tuoi/{nctId}/so-du` (kiểm `quyenCoTacDung(XEM_CHI_PHI)` mỗi lần) (research E-04)
- [x] T011 *(Xong 2026-11-08, A: cổng `family-portal/live/LiveCostsPage` (mã QR `qrcode.react`) gắn vào `LIVE_PAGES.costs`, app `lib/family/balance/family_balance_screen.dart` mở từ màn cổng khi có quyền xem chi phí; màn số dư trên hồ sơ cho nhân viên là `ResidentBalance` của B (nhánh `impl/017-b-phase7-10`))* [P] [US1] Màn web số dư trên hồ sơ trong `frontend/src/tinh-nang/so-du/` và cổng `frontend/src/tinh-nang/cong-nguoi-than/so-du/` (vẽ mã QR từ chuỗi payload) cùng test Vitest; app `mobile/lib/nguoi-than/so-du/` theo mục "Giai đoạn hiện tại" của CLAUDE.md
- [x] T045 *(Xong 2026-11-08, A: `V017_02` nạp 70 ngân hàng (danh sách VietQR, `catalog_item` loại `M11.BANK`), `GET /banks`, `CollectionAccountCheck` (`ParameterValueCheck` CFG-M11-10: `bin`, `accountNumber`, `accountHolder`), ô chọn ngân hàng `parameters/CollectionAccountEditor`; kiểm trong `US1_BalanceIT`, không tách `NganHangThamSoIT`)* [US1] *(Bổ sung 2026-10-22; phần 000 xong 2026-10-22: SPI `ParameterValueCheck` ở `administration.parameter.api.spi`, `ParameterService` gọi trước khi lưu, bản giả `FakeParameterValueCheck`, `ParameterValueCheckIT`; còn phần của 017)* Danh mục ngân hàng cho CFG-M11-10 (research E-04): migration bảng `ngan_hang` và nạp danh sách mã BIN NAPAS (trong `MIG/V017_*` của 017); `GET /ngan-hang`; khai báo SPI `KiemTraGiaTriThamSo` ở `administration.thamso.api` và gọi trong lệnh đổi giá trị tham số của 000 (sửa `ParameterService`, chủ 000: A) cùng bản cài của 017 cho CFG-M11-10; ô chọn ngân hàng trên màn Tham số (`frontend/src/features/parameters/`); CFG-M11-10 chưa có giá trị thì ẩn thông tin nộp tiền; test `NganHangThamSoIT` (quickstart Q1a) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `ngan_hang` → `danh_muc_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**

**Checkpoint**: Q1, Q2 xanh.

---

## Phase 4: User Story 2, 3 – Kế toán ghi nhận tiền nộp, tiền cọc; ghi nhận chuyển khoản (Priority: P1)

**Goal**: Tiền mặt và chuyển khoản ghi bằng tay, Đã xác nhận ngay; cảnh báo trùng mã ngân hàng; trạng thái đặt cọc cho 004 (FR-005 → FR-007; BR-M11-12, 16).

**Independent Test**: Quickstart Q3 → Q6.

### Tests cho User Story 2, 3

- [x] T012 *(Xong 2026-11-08, A: `US2_PaymentIT` (Q3 → Q6 gộp một IT))* [P] [US2] Test `BT/billing/sodu/BR_M11_16_TienMatIT.java`, `FR_006_DatCocIT.java` (Q3, Q4, Q6): tiền mặt vào số dư, sổ cọc; số tiền ≤ 0 → 422; HC 403; không có số phiếu; đặt cọc đổi khi đủ khoản cọc, `TrangThaiDatCocDaDoi` phát kèm giao dịch và Kế toán; hợp đồng không yêu cầu cọc → `KHONG_YEU_CAU` (US2 #1 → #6)
- [x] T013 *(Xong 2026-11-08, A: trong `US2_PaymentIT`)* [P] [US3] Test `BT/billing/sodu/BR_M11_12_ChuyenKhoanIT.java` (Q5): ghi theo mã nộp tiền; hai dòng cùng mã trong một lệnh; mã đã dùng → 409 kèm giao dịch trước, gửi lại có cờ → 201; hồ sơ trạng thái cuối vẫn ghi được (US3 #1 → #3, #5; DBR-29)

### Implementation cho User Story 2, 3

- [x] T014 *(Xong 2026-11-08, A: `PaymentService` (tiền mặt, chuyển khoản, 409 `BANK_TRANSACTION_CODE_USED` kèm `duplicateTransactions`), `LedgerPosting` (khóa dòng sổ), `DepositStatuses` (cài `DepositStatusPort`, phát `DepositStatusChanged` khi kết quả đổi, nghe `DepositAmountChanged`), `PaymentController`)* [US2] Cài `BE/billing/sodu/giaodich/`: `POST /nguoi-cao-tuoi/{nctId}/nop-tien-mat`, `/nop-chuyen-khoan` dưới khóa `so_du`; cập nhật sổ; gọi tính lại tình trạng (để trống tới T021) và trạng thái đặt cọc; cài `TrangThaiDatCocPort`, phát `TrangThaiDatCocDaDoi`; listener `KhoanCocDaDoi` (research E-05) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `so_du` → `trang_thai_nct`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [x] T015 *(Xong 2026-11-08, A: `features/balance/PaymentRecording` (tìm theo mã, hai dòng, hộp xác nhận trùng mã) kèm `PaymentRecording.test.jsx`; gắn vào `BalancePage` của B khi nhánh B vào `develop`)* [P] [US3] Màn web ghi nhận tiền nộp trong `frontend/src/tinh-nang/so-du/` (tìm theo mã nộp tiền, hai dòng cọc và số dư, hộp xác nhận khi trùng mã) cùng test Vitest `NopChuyenKhoan.test.jsx`

**Checkpoint**: Q3 → Q6 xanh; 004 nhận trạng thái đặt cọc.

---

## Phase 5: User Story 4 – Chốt bảng chi phí tự trừ số dư (Priority: P1)

**Goal**: Một giao dịch thanh toán mỗi bảng Đã chốt (FR-012, DBR-30).

**Independent Test**: Quickstart Q7.

- [x] T016 *(Xong 2026-11-08, A: `US4_StatementPaymentIT` (Q7, DBR-30))* [P] [US4] Test `BT/billing/sodu/DBR_30_ThanhToanBangIT.java`, `BR_M11_11_ThanhToanBangIT.java` (Q7): tổng dương, âm, 0; sự kiện lặp không tạo giao dịch thứ hai; số dư âm hiện "còn nợ" (US4 #1 → #5; SC-002)
- [x] T017 *(Xong 2026-11-08, A: `StatementPayments` nghe `CostStatementFinalized` trong transaction lệnh chốt; không tính lại tình trạng vì số dư − tổng chưa chốt không đổi (plan "Ghi chú triển khai"); `M13ArchTest` sửa theo ý "chỉ 009 gửi thông báo")* [US4] Cài listener `BangChiPhiDaChot` trong `BE/billing/sodu/giaodich/` (idempotent theo `bang_chi_phi_id`, cập nhật sổ, tính lại tình trạng, điều kiện quyết toán) (research E-06)

**Checkpoint**: Q7 xanh.

---

## Phase 6: User Story 5 – Báo "sắp hết tiền" và "còn nợ" (Priority: P1)

**Goal**: Tình trạng hằng ngày và khi nộp tiền; báo theo bảng chuyển; dừng nhắc; dấu mua hộ (FR-016 → FR-021; BR-M11-13, 15).

**Independent Test**: Quickstart Q8 → Q10, Q34 → Q36 với đồng hồ giả lập.

### Tests cho User Story 5

- [x] T018 *(Xong 2026-11-08, A: `BalanceStatusRuleTest` (tên logic `TinhTrangTest`), `NoticeDecisionTest` (`QuyetDinhBaoTest`, 16 dòng của bảng chuyển); `PhiMotNgayTest` không viết vì phí một ngày lấy từ `CostForBalancePort.baseDailyFee` của 010)* [P] [US5] Test đơn vị `BT/billing/sodu/PhiMotNgayTest.java`, `TinhTrangTest.java`, `QuyetDinhBaoTest.java` (bảng mọi dòng của bảng chuyển tình trạng; phí 0; bán trú giá buổi) (research E-07)
- [x] T019 *(Xong 2026-11-08, A: `US5_BalanceStatusIT` (Q8 → Q10, Q35, Q36 gộp một IT; Q34 không chặn lệnh khác: 017 không có điểm chặn nào, không cần IT))* [P] [US5] Test `BT/billing/sodu/BR_M11_13_TinhTrangIT.java`, `BR_M11_15_DauMuaHoIT.java`, `FR_020_KhongChanIT.java` (Q8 → Q10, Q34 → Q36): người nhận và phần nội dung theo quyền xem chi phí; chu kỳ CFG-M11-05; báo Quản lý viện khi còn nợ kéo dài; nộp tiền về Bình thường, chưa đủ (Q-252, Q-257); chạy lại cùng ngày không báo trùng (SC-005, SC-012); dừng nhắc chỉ hồ sơ trạng thái cuối

### Implementation cho User Story 5

- [x] T020 *(Xong 2026-11-08, A: `status.domain.{BalanceStatusRule, NoticeDecision, BalanceNotices, StatusSnapshot}`, `status.service.{BalanceStatusService, BalanceStatusCommands, PurchaseBalance}`, `BalanceStatusController`; lịch sử tình trạng vào `status_history` `M11.BALANCE`/`STATUS`)* [US5] Cài `BE/billing/sodu/tinhtrang/` (`PhiMotNgay`, `TinhTrang`, `QuyetDinhBao`, ghi `lich_su_tinh_trang`, thông báo theo FR-030 với phần `CHUNG` / `CHI_PHI`), `SoDuMuaHoPort`, lệnh `POST /nguoi-cao-tuoi/{nctId}/so-du/dung-nhac`, `GET /tinh-trang-so-du` **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lich_su_tinh_trang` → `lich_su_chung`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.**
- [x] T021 *(Xong 2026-11-08, A: `billing.balance.scheduling.BalanceStatusJob` (02:00, ShedLock, chạy bù), `DailyBalanceStatus` (một transaction mỗi hồ sơ, dấu `reminder_marker` theo ngày); tính lại ngay qua `PaymentService.PaymentRecordedHook`)* [US5] Cài job `BE/billing/sodu/lich/ViecHangNgayM11SoDu.java` (02:00, ShedLock, chạy bù; phần tình trạng và đối chiếu bất biến sổ) và nối tính lại ngay trong lệnh nộp tiền (T014)
- [x] T022 *(Xong 2026-11-08, A: `features/balance/BalanceStatuses` (lọc, dừng nhắc của Quản lý viện) kèm test; gắn vào `BalancePage` của B khi nhánh B vào `develop`)* [P] [US5] Màn web tình trạng số dư trong `frontend/src/tinh-nang/so-du/` (danh sách Sắp hết tiền, Còn nợ; dừng nhắc của Quản lý viện)

**Checkpoint**: Q8 → Q10, Q34 → Q36 xanh.

---

## Phase 7: User Story 6 – Hoàn tiền và điều chỉnh có Quản lý viện duyệt (Priority: P2)

**Goal**: Hoàn tiền, hoàn cọc, điều chỉnh Chờ duyệt qua yêu cầu phê duyệt của 000; kiểm lại khi duyệt; sửa sai bằng điều chỉnh trỏ giao dịch gốc; cấn trừ cọc; nhắc hoàn cọc (FR-010 → FR-015a; BR-M11-10, 14).

**Independent Test**: Quickstart Q11 → Q15, Q24.

### Tests cho User Story 6

- [x] T023 [P] [US6] *(Xong 2026-11-08, B: lớp thật `RefundLimitTest` ở `BT/billing/balance/`, 2 test; hàm `RefundLimit` ở `billing.balance.internal.transaction.domain`)* Test đơn vị `BT/billing/sodu/GioiHanHoanTest.java` (FR-014 mọi nhánh)
- [x] T024 [P] [US6] *(Xong 2026-11-08, B: theo ngân sách IT (B chọn mỗi phase một lớp IT) gộp năm lớp thành `US6_BalanceTransactionIT`, một ca: US6 #1 → #6, #8, Q24 cấn trừ, FR-015a nhắc hoàn cọc; DBR-17 tầng cơ sở dữ liệu đã ở `DBR_28_BalanceInvariantIT` của A, tầng lệnh: hợp đồng không có lệnh sửa, xóa (`ApiContractIT`); `TransactionChecks` kiểm bằng `RefundLimitTest` và IT)* Test `BT/billing/sodu/BR_M11_14_HoanTienIT.java`, `BR_M11_10_DieuChinhIT.java`, `DBR_17_GiaoDichKhongSuaIT.java`, `FR_015_CanTruCocIT.java`, `FR_015a_NhacHoanCocIT.java` (Q11 → Q15, Q24): Chờ duyệt không đổi sổ; duyệt kiểm lại giới hạn, người nhận; từ chối, hủy; điều chỉnh trỏ gốc đặt `da_dieu_chinh`; không trỏ thanh toán bảng; không có lệnh sửa, xóa giao dịch Đã xác nhận (trigger); cặp cấn trừ cùng số tiền, ngược dấu (SC-007, SC-013)

### Implementation cho User Story 6

- [x] T025 [US6] *(Xong 2026-11-08, B: `BalanceTransactionService.create`, `pendingApproval` (trả `residentId`, `residentName`), handler `BalanceTransactionRequestHandler` (`checkOnApprove` kiểm lại, chặn thì báo Kế toán ở transaction riêng `BlockedApprovalNotices`; `apply` xác nhận, cập nhật sổ, đặt "đã điều chỉnh"; nghe từ chối, hủy, áp dụng không thành); `V017_10` nới `CHECK` mã ngân hàng cho hoàn tiền chuyển khoản)* Cài `POST /giao-dich`, `GET /giao-dich/cho-duyet` và handler `M11.GIAO_DICH_SO_DU` (`LoaiYeuCauHandler` của 000: `kiemTraKhiDuyet`, `apDung`) trong `BE/billing/sodu/giaodich/` (research E-06)
- [x] T026 [US6] *(Xong 2026-11-08, B: `BalanceTransactionService.offset` (điều kiện qua `ResidencyEndingReadApi.settlementInfo` (hồ sơ kết thúc Đang chuẩn bị hoặc danh sách việc sau qua đời đang mở) và `FinalPeriodCostApi.finalPeriodCondition` của 010, thiếu bean thì chưa đạt); nhắc hoàn cọc `status.service.DepositRefundReminders` gọi từ `BalanceStatusJob`; bản giả `FakeResidencyEndingReads` (T006 phần B))* Cài `POST /nguoi-cao-tuoi/{nctId}/can-tru-coc` (điều kiện qua `HoSoKetThucDocApi`, `DanhSachViecSauQuaDoiApi`, giao dịch thanh toán kỳ cuối) và nhắc hoàn cọc (listener hợp đồng hủy, Hủy tiếp nhận; phần nhắc của job T021)
- [x] T027 [P] [US6] Màn web lập hoàn tiền, điều chỉnh, cấn trừ trong `frontend/src/tinh-nang/so-du/`; giao dịch Chờ duyệt hiện trong hộp duyệt của 000 (T097 của 000) với chi tiết do 017 trả *(2026-11-08, B: xong màn `ResidentBalance` (số dư, giao dịch), `TransactionForm` (hoàn tiền, hoàn cọc, điều chỉnh), `OffsetForm` (cấn trừ), `PendingTransactions` trong `frontend/src/features/balance/` kèm 9 test Vitest `ResidentBalance.test.jsx`, `PendingTransactions` qua `BalancePage.test.jsx`; chưa chạy với API thật (chờ A); hộp duyệt của 000 hiện chi tiết giao dịch chờ A cài handler; 2026-11-08 sau khi cài T025: cột Người cao tuổi theo `residentName` của `PendingBalanceTransaction`, 41 test Vitest xanh; chưa thử trên trình duyệt với backend thật)*

**Checkpoint**: Q11 → Q15, Q24 xanh.

---

## Phase 8: User Story 7, 10 – Quyết toán: điều kiện, Xác nhận quyết toán, bảng quyết toán (Priority: P2)

**Goal**: Hai mã điều kiện cho 004; mục việc sau qua đời; Xác nhận quyết toán dựng bảng PDF có phiên bản; bản ký; nhắc sau trạng thái cuối (FR-022 → FR-024, FR-034; BR-M11-22; DBR-38).

**Independent Test**: Quickstart Q16 → Q23.

### Tests cho User Story 7, 10

- [x] T028 [P] [US7] *(Xong 2026-11-08, B: lớp thật `SettlementConditionsTest`, 5 test; hàm `SettlementConditions` ở `billing.balance.internal.settlement.domain`; ngoại lệ chỉ ảnh hưởng `Result.satisfied(exceptionApproved)`)* Test đơn vị `BT/billing/sodu/DieuKienQuyetToanTest.java`: ba ý tiền và ý xác nhận; ngoại lệ chỉ miễn ba ý tiền; còn Chờ duyệt thì ý xác nhận Chưa đạt dù có ngoại lệ (BR-M11-22, chủ dự án chọn 2026-10-22); giao dịch sau xác nhận làm ý xác nhận Chưa đạt; bảng bổ sung chưa chốt không ảnh hưởng
- [x] T029 [P] [US7] *(Xong 2026-11-08, B: theo ngân sách IT (mỗi phase một lớp IT) gộp ba lớp với T030 thành `US7_SettlementIT`, một ca: bean trả hai mã, `waivable`; `SettlementConditionChanged` phát khi lập hồ sơ kết thúc, sau giao dịch, xác nhận; mục `SETTLEMENT` Hoàn thành khi có ngoại lệ đã duyệt và đã xác nhận (Q-387), Không áp dụng khi sổ chưa có giao dịch khác 0; nhắc sau trạng thái cuối (FR-024) mỗi CFG-M02-09. Nhánh từ chối, hàm thuần ở `SettlementRulesTest` (4 test))* Test `BT/billing/sodu/FR_022_DieuKienQuyetToanIT.java`, `FR_023_ViecSauQuaDoiIT.java`, `FR_024_SauTrangThaiCuoiIT.java` (Q16, Q22, Q22a, Q23): bean trả đúng hai mã và `coTheMien`; mục `QUYET_TOAN` của người qua đời Hoàn thành khi có ngoại lệ đã duyệt và đã xác nhận (Q-387); `DieuKienQuyetToanDaDoi` phát khi đổi; mục `QUYET_TOAN` Hoàn thành, Không áp dụng; nhắc sau trạng thái cuối tới khi số dư 0 và xác nhận lại
- [x] T030 [P] [US10] *(Xong 2026-11-08, B: trong `US7_SettlementIT` (Q17 → Q21): lệnh bị chặn khi kỳ cuối chưa chốt, còn Chờ duyệt; phiên bản 1, 2, một hiện hành; nội dung PDF (trích chữ) khớp sổ, dòng "Chứng từ này không thay thế hóa đơn"; tải lại cùng byte, mỗi lần tải ghi nhật ký; bản ký một lần, không vào phiên bản cũ (409); người thân có quyền xem chi phí được báo; điều kiện lệnh ở `SettlementRulesTest`)* Test `BT/billing/sodu/BR_M11_22_XacNhanQuyetToanIT.java` (Q17 → Q21): điều kiện lệnh (căn cứ, ba ý tiền hoặc ngoại lệ, không còn Chờ duyệt); phiên bản tăng, một hiện hành; nội dung bảng khớp sổ, kỳ cuối, thông tin hồ sơ kết thúc; dòng "Chứng từ này không thay thế hóa đơn"; bản ký một lần vào phiên bản hiện hành; người thân có quyền xem chi phí được báo (SC-008, SC-015)

### Implementation cho User Story 7, 10

- [x] T031 [US7] *(Xong 2026-11-08, B: `settlement.service.{SettlementService.evaluate, SettlementTransitionParticipant, SettlementPostDeathItem, SettlementChanges}`, `settlement.domain.{SettlementLedger, SettlementRules}`; kỳ cuối qua `FinalPeriodCostApi` (thiếu bean thì chưa đạt); nhắc FR-024 ở `status.service.AfterFinalStatusReminders` gọi từ `BalanceStatusJob`. Lớp nền test 001 `StatusTestBase`, 004 `M02TestBase` gỡ bean thật của 017 như bean thật của 003, 012 (bản giả `otherModules` đứng thay))* Cài `BE/billing/sodu/quyettoan/`: `DieuKienQuyetToan`, bean `ThamGiaChuyenTrangThai` (hai mã), `MucViecSauQuaDoiPort` (`QUYET_TOAN`), listener `HoSoKetThucDa*`, `QuaDoiDaGhiNhan`, `NgoaiLeKetThucDaDuyet`, phát `DieuKienQuyetToanDaDoi`; phần nhắc sau trạng thái cuối của job T021 (research E-08)
- [x] T032 [US10] Cài lệnh `POST /nguoi-cao-tuoi/{nctId}/quyet-toan/xac-nhan` (chụp số liệu, dựng PDF bằng `PdfBieuMau`, `FileApi.upload` + `attach`, phiên bản), `GET /nguoi-cao-tuoi/{nctId}/quyet-toan`, `POST /bang-quyet-toan/{id}/ban-ky`, `GET /bang-quyet-toan/{id}/file` (ghi `lan_tai_chung_tu`) (research E-09) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_tai_chung_tu` → hành động `nhat_ky`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.** *(2026-11-08, B: xong lệnh, bốn endpoint, phiên bản ở `record_event` (`M11.SETTLEMENT_STATEMENT`, `M11.SETTLEMENT_SIGNED_COPY`, khóa duy nhất, không cần `V017_94`), PDF lưu bằng `FileApi`, tải qua `FileService.download` ghi nhật ký `BALANCE_DOCUMENT_DOWNLOADED`, `FilePermissionProvider` hai loại tệp; giữ [ ] vì bảng còn thiếu hai mục của FR-034 do chưa có nguồn: họ tên người đại diện (chưa có cổng đọc người đại diện hiện tại của người cao tuổi) và tổng bảng kỳ cuối theo loại (`CostForBalancePort.finalizedStatement` của 010, E); team-plan mục 8)* *(2026-11-09, B: tổng bảng kỳ cuối theo loại đã nối `CostForBalancePort.finalizedStatement` qua `SettlementLedger.finalPeriodStatementId`, `US7_SettlementIT` kiểm trong PDF; còn dòng họ tên người đại diện chờ `RepresentativePort` (A, chủ dự án), giữ [ ])* *(2026-11-10, A: chủ dự án duyệt `RepresentativePort.activeRepresentative`; bảng quyết toán và phiếu báo phí ghi họ tên người đại diện qua `RepresentativeNames`; `US7_SettlementIT` đỏ rồi xanh, `T006_RelationshipSpiIT` kiểm bản cài 012)*
- [x] T033 [P] [US10] *(2026-11-08, B: `SettlementPanel` + `QuyetToan.test.jsx` 6 test Vitest, chạy với API giả `mockRoutes`; chạy với backend thật sau khi A cài endpoint, T032)* Màn web quyết toán trong `frontend/src/tinh-nang/so-du/` (hai mã điều kiện kèm lý do, nút xác nhận, phiên bản, tải PDF, đính kèm bản ký) cùng test Vitest `QuyetToan.test.jsx`

**Checkpoint**: Q16 → Q23 xanh; 004 chạy Kết thúc lưu trú với bean thật của 017 (004 T106).

---

## Phase 9: User Story 9 – Phiếu báo phí (Priority: P2)

**Goal**: Phát hành phiếu PDF, Excel cho mỗi bảng Đã chốt, mã duy nhất, tải lại đúng file, người thân được báo và tải (FR-031, FR-032, FR-037, FR-038; BR-M11-21, 23; DBR-37).

**Independent Test**: Quickstart Q25 → Q28.

- [x] T034 [P] [US9] Test `BT/billing/sodu/BR_M11_21_PhieuBaoPhiIT.java`, `BR_M11_23_ChungTuIT.java`, `CongChungTuIT.java` *(2026-11-08, B Phase 9: một lớp `US9_PaymentNoticeIT` đỏ rồi xanh 1/1: phát hành, lần hai 409 `ALREADY_ISSUED`, PDF và Excel cùng số liệu, ảnh QR, tải lại cùng byte, nhật ký mỗi lần tải 3 lần, danh sách theo hồ sơ, cổng người thân theo quyền xem chi phí; còn danh sách chờ phát hành, 409 `STATEMENT_NOT_PAID`, số dư trước/sau khớp sổ: chờ lệnh phát hành, giữ [ ])* (Q25 → Q28): danh sách chờ phát hành; phát hành lần hai 409; nội dung PDF (trích chữ) và Excel (đọc ô) cùng số liệu; số dư trước, sau khớp sổ; khi âm có số còn nợ, mã nộp tiền, tài khoản, ảnh QR; không có tên thuốc; tải lại cùng byte; mỗi lần tải ghi nhật ký; người thân không có quyền xem chi phí 403 (SC-014, SC-016, SC-017) *(2026-11-09, B: xong. Lệnh phát hành và danh sách chờ nối `CostForBalancePort.finalizedStatement` thật của 010; `US9_PaymentNoticeIT` thêm ca danh sách chờ, 404, 409 `STATEMENT_NOT_PAID`, số dư trước và sau khớp sổ, mã nộp tiền, ngân hàng, tài khoản thu theo CFG-M11-10 trên phiếu khi còn nợ, không tên thuốc; xanh 2/2; chưa kiểm ảnh QR trong PDF qua lệnh thật, đã kiểm `QrImageTest`)*
- [x] T035 [US9] Cài `BE/billing/sodu/chungtu/`: `GET /phieu-bao-phi/cho-phat-hanh`, `POST /bang-chi-phi/{bangId}/phieu-bao-phi` (chụp số liệu từ `ChiPhiChoSoDuPort.bangDaChot` và sổ; PDF qua `PdfBieuMau` kèm ảnh QR ZXing; Excel qua `ExcelBieuMau`; `FileApi`; thông báo, `ChungTuDaPhatHanh`), `GET /nguoi-cao-tuoi/{nctId}/phieu-bao-phi`, `GET /phieu-bao-phi/{id}/file`; cổng `GET /cong/nguoi-cao-tuoi/{nctId}/chung-tu`, `GET /cong/chung-tu/{loai}/{id}/file` (research E-10) *(2026-11-08, B: xong phần dựng PDF, Excel `PaymentNoticeDocument` + `PaymentNoticeDocumentTest` 5 test; ảnh QR `QrImage` (ZXing, `QrImageTest`) có nhưng chưa chèn được vào thân PDF vì `PdfDocument` chưa có hàm chèn ảnh, team-plan mục 8; còn lệnh phát hành, endpoint chờ A, giữ [ ]; 2026-11-08, Phase 9: xong `PaymentNoticeService` (`publish`, `list`, `download`, `portalDocuments`), `PaymentNoticeRecords` (`record_event` `M11.PAYMENT_NOTICE`, khóa duy nhất theo bảng, DBR-37), `FamilyCostsRight`, `PaymentNoticeFiles`, ảnh QR vào PDF qua `PdfDocument.image`, endpoint `GET /residents/{residentId}/payment-notices`, `GET /payment-notices/{id}/file`, hai endpoint `/family-portal`; còn lệnh phát hành `POST /costs/statements/{statementId}/payment-notice` và `GET /payment-notices/pending-issue` chờ `CostForBalancePort.finalizedStatement` của E (team-plan mục 8), giữ [ ])* *(2026-11-09, B: xong lệnh phát hành `POST /costs/statements/{statementId}/payment-notice` và `GET /payment-notices/pending-issue` (`PaymentNoticeIssuance`, `PaymentNoticeStore`); đủ endpoint của T035; `ApiContractIT` xanh, danh sách chờ 017 rỗng)*
- [x] T036 [P] [US9] Màn web phiếu báo phí trong `frontend/src/tinh-nang/so-du/` (danh sách chờ phát hành, phát hành, tải) (mục chứng từ trên cổng làm ở 012 T073); test Vitest `PhieuBaoPhi.test.jsx`; app người thân tải chứng từ theo mục "Giai đoạn hiện tại" *(2026-11-08, B: xong web `PaymentNotices` (chờ phát hành, phát hành) và `ResidentNotices` (đã phát hành, tải PDF, Excel) + `PaymentNotices.test.jsx` 5, `PhieuBaoPhi.test.jsx` 2; 2026-11-08, app người thân tải chứng từ xong (xem T039); còn chạy thật màn phát hành khi T035 có lệnh phát hành, giữ [ ])* *(2026-11-09, B: xong. Backend có lệnh phát hành và danh sách chờ, `PaymentNotices.test.jsx`, `PhieuBaoPhi.test.jsx` xanh trong Vitest 907/907; chưa chạy tay màn trên trình duyệt)*

**Checkpoint**: Q25 → Q28 xanh.

---

## Phase 10: User Story 8 – Xuất sao kê và báo cáo thu chi (Priority: P2)

**Goal**: Sao kê và báo cáo thu chi thu gọn, Excel và PDF cùng số liệu; người thân tải sao kê (FR-025 → FR-027; Q-381, Q-386).

**Independent Test**: Quickstart Q29 → Q33.

- [x] T037 [P] [US8] Test đơn vị `BT/billing/sodu/SaoKeTest.java`, `BaoCaoThuChiTest.java` và tích hợp `FR_025_XuatIT.java`, `FR_026_SaoKeCongIT.java` (Q29 → Q33): đầu kỳ + tổng = cuối kỳ từng sổ; chỉ Đã xác nhận; ba tổng và danh sách còn nợ theo số dư cuối khoảng; Excel và PDF cùng số liệu; khoảng > CFG-M14-01 → 422; HC 403; mỗi lần xuất ghi `lan_xuat_so_du`, file không đổi khi dữ liệu đổi (SC-009) **Theo R-15 (data-model mục 0, 0.1; sửa câu chữ 2026-10-30): không tạo bảng riêng cho các bảng sau mà ghi vào bảng đích: `lan_xuat_so_du` → hành động `nhat_ky`; ràng buộc giữ theo cột "Cách giữ sau gộp" của 0.1.** *(2026-11-08, B: xong phần đơn vị `BalanceStatementTest` 4, `IncomeExpenseReportTest` 4 (lớp thật theo tên tiếng Anh); 2026-11-08, Phase 10: IT `FR_025_ExportIT` 1/1 xanh gộp cả `FR_026_*` (cổng người thân theo quyền xem chi phí, khoảng quá hạn 422, Hành chính 403, Quản lý viện chỉ tải, file không đổi khi sổ đổi, nhật ký lần xuất và lần tải; viết sau mã nên không có bước đỏ trước))*
- [ ] T038 [US8] Cài `BE/billing/sodu/xuat/`: `POST /xuat/sao-ke`, `POST /xuat/bao-cao-thu-chi`, `GET /xuat/{id}/file`, `POST /cong/nguoi-cao-tuoi/{nctId}/sao-ke` (fastexcel, `PdfBieuMau`, `FileApi`); `SoDuThongKeApi.thuChi`, `tinhTrang` cho 016 (research E-11) *(2026-11-08, B: xong phần dựng file `BalanceExportDocument` + `BalanceExportDocumentTest` 2 test; 2026-11-08, Phase 10: xong `BalanceExportService` (`statement`, `incomeExpense`, `download`, `portalStatement`), `BalanceExportRecords` (`record_event` `M11.BALANCE_EXPORT`), `ConfirmedEntries`, `BalanceExportFiles`, `BalanceExportController` với 4 endpoint; 2026-11-08, sau đó: xong `BalanceStatistics` (`BalanceStatisticsApi` thật: `incomeExpense` cùng cách tính với báo cáo xuất, `statuses` đọc tình trạng đã lưu qua `StatusReads`, lọc theo `ScopeFilter`) + `US8_BalanceStatisticsIT` 1/1; còn phiếu cuối task chờ A xem lại, giữ [ ] đến khi 016 dùng bản thật)*
- [x] T039 [P] [US8] Màn web xuất trong `frontend/src/tinh-nang/so-du/` và nút tải sao kê trên cổng *(2026-11-08, B: xong web `BalanceExports` (sao kê nhiều người, báo cáo thu chi, tải lại file) + `BalanceExports.test.jsx` 5; còn nút tải sao kê trên cổng người thân (012 T073); backend `POST /family-portal/residents/{residentId}/balance-statement` đã có từ Phase 10; 2026-11-08, app người thân: xong `FamilyDocumentsScreen` (danh sách phiếu, bảng quyết toán, tải qua `share_plus`, sao kê theo khoảng ngày PDF/Excel) + `ApiClient.postBytes`, `family_documents_screen_test` 3 test; còn mục chứng từ trên cổng web (012 T073), giữ [ ])*
  - *(2026-11-11, A)* Web: `frontend/src/features/family-portal/live/LiveDocuments.jsx` trong trang Chi phí, số dư của cổng (phiếu báo phí, bảng quyết toán tải đúng file theo định dạng; sao kê theo khoảng ngày PDF/Excel; 403 thì ẩn), cùng chữ với app `FamilyDocumentsScreen` (017 T036). `LiveDocuments.test.jsx` 3 ca đỏ rồi xanh; Vitest family-portal 28, eslint, prettier xanh. Chưa kiểm trực quan trên trình duyệt.

**Checkpoint**: Q29 → Q33 xanh.

---

## Phase 11: Polish & Cross-Cutting

- [x] T040 *(2026-11-08, phần A xong: `DBR_28_BalanceInvariantIT` (50 hồ sơ × 3 tháng nộp tiền, chốt bảng; hai lần nộp đồng thời; đối chiếu hằng đêm `LedgerStore.mismatches` ở job T021); quyền của endpoint A kiểm trong US1, US2, US5 IT và `PermissionMatrixIT`; lưu giữ FR-029 ở `T007_BalanceProvidersIT`. 2026-11-08, B: `T040_ConcurrencyAndPermissionsIT` 3/3 xanh: hai lần phát hành phiếu và hai lần xác nhận quyết toán đồng thời (một kết quả hợp lệ, phiên bản không trùng), bảng quyền theo vai trò của phiếu, quyết toán, xuất (SC-010); còn "duyệt cùng thanh toán" đồng thời vì `SwitchableUser` là một người dùng chung cho mọi luồng (duyệt cần Quản lý viện, thanh toán cần Kế toán), giữ [ ])* [P] Test `BT/billing/sodu/DBR_28_SoDuBatBienIT.java` (bộ 3 tháng × 50 hồ sơ, SC-001), `DongThoiSoDuIT.java` (hai lần nộp; duyệt cùng thanh toán; hai lần phát hành; hai lần xác nhận), `QuyenSoDuIT.java` (bảng quyền research E-12; SC-010), `LuuTruSoDuIT.java` (FR-029) *(2026-11-09, B: xong. Thêm ca `DBR_28_approvalRacingStatementPayment_ledgerStaysConsistent` vào `T040_ConcurrencyAndPermissionsIT` (4/4 xanh): duyệt hoàn tiền chạy cùng lúc với thanh toán bảng (sự kiện hệ thống, không cần đổi người dùng); số dư luôn bằng tổng giao dịch Đã xác nhận, hoặc duyệt được (số dư -1.000.000) hoặc bị chặn 409 và giữ Chờ duyệt (500.000), chỉ một giao dịch thanh toán bảng. Ca này xanh ngay lần chạy đầu vì không cần đổi mã chính, không có lượt đỏ trước)*
- [x] T041 *(2026-11-08, phần A xong: `ApiContractIT` so 9 operation của A; trigger `balance_transaction` (chặn sửa, xóa; chỉ đặt `adjusted` một lần) kiểm ở `DBR_28_BalanceInvariantIT`. 2026-11-08, B: `ApiContractIT` so các operation Phase 7–10 đã cài (còn hai operation phiếu: lệnh phát hành, danh sách chờ, chờ E); dòng `record_event` của phiếu không sửa, xóa, khóa duy nhất kiểm ở `T040_ConcurrencyAndPermissionsIT`; giữ [ ] đến khi hai operation còn lại cài)* [P] Mở rộng `BT/common/HopDongApiTest.java` so springdoc với `specs/017-resident-balance-ledger/contracts/api-017.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 017 *(2026-11-09, B: xong. Hai operation phiếu đã cài, danh sách chờ của api-017 trong `ApiContractIT` rỗng, xanh)*
- [ ] T042 *(2026-11-08, phần A xong: chờ bản thật `CostForBalancePort` (010), hợp đồng thật của 004 rồi chạy lại US1, US2, US4, US5 IT với bean thật)* Khi 004, 010, 012 có bản thật, chạy lại test US2, US6, US7, US9 với bean thật thay bản giả (T006)
- [ ] T043 *(2026-11-08, phần A xong: 2026-11-08, B: Phase 7–10 xong phần làm được, `./mvnw clean verify -Dit.forkCount=3` 2026-11-08: unit 949, IT 409 xanh, 21 phút 44 giây; còn quickstart mục 3, `npm`, `flutter`, đo SC sau khi T034 → T036 có lệnh phát hành; chờ B xong Phase 7–10 rồi chạy quickstart mục 3, đo SC)* Chạy `./mvnw verify`, `npm test`, `npm run lint`, `npm run build`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [x] T044 *(2026-11-08, B: truy vết Phase 7–10 ở plan "Ghi chú triển khai"; phần A xong: truy vết DBR, BR, FR của Phase 1–6 ở plan "Ghi chú triển khai" (bảng 2026-11-08); còn phần Phase 7–10 của B)* Rà truy vết data-model mục 9: DBR-17, 28, 29, 30, 37, 38 và BR-M11-10 → 16, 21 → 23 có test; mọi FR còn hiệu lực của spec có task hoặc test (constitution XV)
- [x] T046 *(2026-11-08, B: không tạo bảng nào, phiếu, bảng quyết toán, lần xuất đều trên `record_event`, `V017_10` chỉ nới ràng buộc; phần A xong: `V017_01` (hai bảng giữ, lý do ở plan), `V017_02` (ngân hàng vào `catalog_item`), lịch sử tình trạng vào `status_history`, dấu job vào `reminder_marker`, nhóm dữ liệu khai bằng `COMMENT`; còn phần bảng của B)* *(bổ sung 2026-10-29, research R-15 bản 2 của 000)* Khi viết migration và entity của feature, áp data-model mục 0: không tạo các bảng đã gộp, dùng bảng gộp, cột `JSON` và bảng chung (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`); test bảo vệ nhóm dữ liệu viết cho bảng gộp *(2026-10-30, audit cơ sở dữ liệu F-04, PR #63)* Mọi bảng tạo hoặc gộp khai nhóm dữ liệu: entity bằng `@DataGroup`, bảng JDBC bằng `COMMENT = 'DataGroup=CATALOG|BUSINESS|RECORD|MULTIPLE|TECHNICAL'` (backend/README.md mục 4); bảng `RECORD` có `trg_<bảng>_no_update`, `trg_<bảng>_no_delete`; bảng chứa nhóm 2 và nhóm 3 dùng `@DataGroup(MULTIPLE)`, cột `locked_at`, trigger `trg_<bảng>_row_lock`; dòng bảng chung đặt `entity_type` trùng `RetentionProvider.dataType()` của bản ghi cha; job dùng `SchedulerRuns` (module-interfaces 000 mục 1.8). `RecordGroupTriggerIT` kiểm.

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story. T002 cần chủ dự án duyệt thư viện; T003 sửa `common` (chủ `common`).
- **US1 (P1)**: sau Foundational.
- **US2, US3 (P1)**: sau US1 (cần sổ).
- **US4 (P1)**: sau US1; cần sự kiện `BangChiPhiDaChot` của 010 (bản giả được).
- **US5 (P1)**: sau US2, US4.
- **US6 (P2)**: sau US2; cấn trừ cần US4 (kỳ cuối) và 004 `HoSoKetThucDocApi`.
- **US7, US10 (P2)**: sau US6; cần 004 T106 → T108 (hai mã, `HoSoKetThucDocApi`, `NgoaiLeKetThucDaDuyet`).
- **US9 (P2)**: sau US4; cần 010 T037 (`bangDaChot`), 007 T104 (`common.bieumau`).
- **US8 (P2)**: sau US2, US4.

### Parallel Opportunities

- T002, T003, T005, T006, T008 song song.
- Sau US1: US2/US3 và US4 song song; US9 và US8 song song sau US4.
- Màn web song song khi controller đã có.

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2/US3 → US4 → US5.
2. **Dừng và kiểm chứng**: sổ, nộp tiền, trừ bảng, báo sắp hết tiền (Q1 → Q10).

### Incremental Delivery

US1 → US2/US3 → US4 → US5 → US6 → US7/US10 → US9 → US8; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- 017 không tự tạo giao dịch ngoài thanh toán bảng (kèm lệnh chốt của 010); không tự hoàn, tự cấn trừ, tự xác nhận, tự phát hành (Q-306, Q-307).
- Không có phiếu thu chi, chốt quỹ, nhập sao kê, giao dịch đảo (Q-385).
- Mọi mốc đọc `ThamSoApi` (CFG-M11-04, 05, 10, 11, CFG-M02-09, CFG-M14-01, CFG-M15-03 → 06).
