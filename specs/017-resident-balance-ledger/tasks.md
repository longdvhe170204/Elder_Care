---
description: "Danh sách task triển khai feature 017 – Số dư và thu chi của người cao tuổi"
---

# Tasks: Số dư và thu chi của người cao tuổi (017)

**Input**: Tài liệu thiết kế ở `specs/017-resident-balance-ledger/` (plan.md, spec.md, research.md E-01 → E-12, data-model.md, contracts/api-017.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 010 đã xong (port nội bộ `ChiPhiChoSoDuPort`, khai báo `SoDuMuaHoPort`, sự kiện `BangChiPhiDaChot`, `DeNghiMuaHo*`, `KhoanChiPhiDaDoi`); 001, 004 (`TrangThaiDatCocPort`, `MucViecSauQuaDoiPort`, `HopDongDocApi`, sự kiện hợp đồng, hồ sơ kết thúc), 012 (`QuyenNguoiThanApi`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Dùng ở giai đoạn triển khai; CLAUDE.md hiện chặn `/speckit-implement`.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/vn/eldercare/`, `BT/` = `backend/src/test/java/vn/eldercare/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M11 (phần 017): `BE/m11/sodu/{so,thu,saoke,giaodich,tinhtrang,quyettoan,xuat,lich}/`; bổ sung `BE/m11/api/`
- Web `web/src/tinh-nang/so-du/{so-du,thu-chot-quy,sao-ke,giao-dich,tinh-trang,xuat}/`, `web/src/tinh-nang/cong-nguoi-than/so-du/`; app người thân `app-nguoi-than/lib/tinh-nang/so-du/`

---

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/m11/sodu/{so,thu,saoke,giaodich,tinhtrang,quyettoan,xuat,lich}/`; mở rộng `BT/m11/M11ArchTest.java` (010 T001): gói `..lich..` không tạo giao dịch ngoài thanh toán bảng kèm sự kiện chốt (research E-01)
- [ ] T002 [P] Thêm thư viện sinh VietQR (chuỗi EMVCo) hoặc tiện ích tự viết trong `BE/m11/sodu/so/internal/VietQr.java` (không thanh toán trực tuyến) (research E-04)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Bảng sổ và giao dịch, khóa theo hồ sơ, cập nhật số dư dẫn xuất cùng transaction, dãy số phiếu, khai báo `m11.api`, khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 [P] Bổ sung `BE/m11/api/`: `SoDuApi`, `SoDuThongKeApi`, sự kiện `TrangThaiDatCocDaDoi`, `DieuKienQuyetToanDaDoi`, `TinhTrangSoDuDaDoi` và thông báo (cài `CoYeuCauThongBao`) (contracts mục 2.2 → 2.4)
- [ ] T004 Tạo `MIG/V017_01__so_du.sql`: `so_du` (`nct_id` khóa chính; `ma_nop_tien UNIQUE`; `so_du`, `so_du_coc`; `tinh_trang` `BINH_THUONG|SAP_HET_TIEN|CON_NO`; `dung_nhac`; `phien_ban`), `giao_dich` (`so` `SO_DU|COC`; `loai` 9 giá trị; `so_tien` có dấu; `UNIQUE (day_phieu, so_phieu)`; `dong_sao_ke_id UNIQUE`; `bang_chi_phi_id UNIQUE`; `trang_thai` `CHO_DUYET|DA_XAC_NHAN|TU_CHOI|DA_HUY`; cột sinh `khoa_dao` `UNIQUE`; trigger sau `DA_XAC_NHAN` chỉ cho đặt `da_dao`, chặn `DELETE`), `day_so_phieu` (`loai` khóa chính `THU|CHI`), `su_kien_m11sd_da_xu_ly`, `lan_chay_m11sd` (data-model mục 2, 3)
- [ ] T005 Cài `BE/m11/sodu/giaodich/internal/GhiGiaoDich.java`: khóa `so_du` theo hồ sơ (E-03), cấp số phiếu từ `day_so_phieu` `FOR UPDATE` trong transaction (không khoảng trống, DBR-35), ghi giao dịch `DA_XAC_NHAN` và cập nhật `so_du`/`so_du_coc` cùng transaction (DBR-28)
- [ ] T006 Cài khung job `BE/m11/sodu/lich/ViecM11SoDu.java` (ShedLock, chạy bù): đối chiếu bất biến số dư hằng đêm, tình trạng, nhắc; test `BT/m11/ChayBuM11SoDuIT.java` (research E-12)
- [ ] T007 [P] Cài provider: `PhanLoaiTruongProvider` (`TAI_CHINH`), `LuuTruProvider` (CFG-M15-03), `GanPhamViProvider` (`M11.SO_DU`, `M11.GIAO_DICH`, `M11.SAO_KE`, `M11.CHOT_QUY`), `QuyenTepProvider` (`M11.SAO_KE_NHAP`, `M11.CHUNG_TU_SO_DU`, `M11.SAO_KE_XUAT`)
- [ ] T008 [P] Test `BT/m11/DBR_28_SoDuIT.java`: `so_du` luôn bằng tổng giao dịch Đã xác nhận; hai phiếu thu đồng thời không lệch

**Checkpoint**: Sổ cái sẵn sàng.

---

## Phase 3: User Story 1 – Mở sổ, cấp mã nộp tiền, người thân xem số dư (Priority: P1) 🎯 MVP

**Goal**: Mở sổ kèm hợp đồng đầu tiên chuyển Chờ ký; mã nộp tiền không đổi, không dùng lại; VietQR; cổng người thân theo quyền xem chi phí (FR-001 → FR-004, DBR-28).

**Independent Test**: Hợp đồng của A chuyển Chờ ký → sổ, mã nộp tiền; hiển thị đúng theo vai trò.

### Tests cho User Story 1

- [ ] T009 [P] [US1] Test `BT/m11/MoSoIT.java` (mở sổ idempotent; mã duy nhất) và app `app-nguoi-than/test/so_du_test.dart` (ẩn khi không có quyền; hiện mã QR) (Q1 → Q3)

### Implementation cho User Story 1

- [ ] T010 [US1] Cài listener `HopDongDaDoiTrangThai` mở sổ, `BE/m11/sodu/so/internal/SoDuService.java` (`SoDuApi`, thông tin nộp tiền đọc CFG-M11-10 qua `ThamSoApi` (Q-335), VietQR) và endpoint `GET /nguoi-cao-tuoi/{nctId}/so-du`, `/giao-dich`, `/cong/nguoi-cao-tuoi/{nctId}/so-du`, `/sao-ke`; màn web `web/src/tinh-nang/so-du/so-du/`, `web/src/tinh-nang/cong-nguoi-than/so-du/`; app `app-nguoi-than/lib/tinh-nang/so-du/` (research E-04)

**Checkpoint**: Sổ mở được.

---

## Phase 4: User Story 2 – Kế toán ghi thu tiền mặt và thu tiền cọc (Priority: P1)

**Goal**: Phiếu thu số liên tục; thu cọc; trạng thái đặt cọc dẫn xuất cho 004 (`TrangThaiDatCocPort`); chốt quỹ ngày Kế toán lập, Quản lý viện xác nhận/trả lại; giao dịch sau chốt quỹ mang dấu (FR-005, FR-005a, FR-006, BR-M11-16, DBR-35).

**Independent Test**: Hợp đồng A cần cọc 20.000.000 đồng; thu 2 lần 10.000.000 đồng → trạng thái sau từng lần; Hành chính không lập được phiếu.

### Tests cho User Story 2

- [ ] T011 [P] [US2] Test đơn vị `BT/m11/TrangThaiDatCocTest.java`, `TongQuyNgayTest.java` và `BT/m11/BR_M11_16_ChotQuyIT.java`, `DBR_35_SoPhieuIT.java` (Q4 → Q7)

### Implementation cho User Story 2

- [ ] T012 [US2] Tạo bảng `chot_quy_ngay` (cột sinh `khoa_ngay` `UNIQUE`; `ly_do` bắt buộc khi chênh lệch ≠ 0; `trang_thai` `DA_LAP|DA_XAC_NHAN|BI_TRA_LAI`), `chot_quy_ngay_phieu` trong `MIG/V017_02__chot_quy.sql`; cài `BE/m11/sodu/thu/internal/PhieuThuService.java` (chỉ Kế toán; thu tiền mặt, thu cọc; phát `TrangThaiDatCocDaDoi`), `TrangThaiDatCocPortImpl.java` (004), `ChotQuyService.java` (research E-05)
- [ ] T013 [US2] Tạo endpoint `POST /phieu-thu`, `/chot-quy-ngay`, `/chot-quy-ngay/{id}/{lenh}`; màn web `web/src/tinh-nang/so-du/thu-chot-quy/`

**Checkpoint**: Thu tiền mặt chạy độc lập.

---

## Phase 5: User Story 3 – Nhập sao kê và đối soát chuyển khoản (Priority: P1)

**Goal**: Nhập file sao kê; `KhopMa` khi đúng một mã và số tiền dương; bỏ khớp, gán, xác nhận vào số dư/cọc, không thuộc người cao tuổi; nhập lại cùng file không trùng; cặp điều chỉnh chuyển tiền (FR-007 → FR-009a, BR-M11-12, DBR-29).

**Independent Test**: Sao kê 6 dòng: 3 đúng mã, 1 không mã, 1 chứa 2 mã, 1 đã nhập trước → kết quả khớp đúng; nhập lại cùng file không trùng.

### Tests cho User Story 3

- [ ] T014 [P] [US3] Test đơn vị `BT/m11/KhopMaTest.java`, `SaoKeTest.java` và `BT/m11/BR_M11_12_DoiSoatIT.java`, `DBR_29_DongSaoKeIT.java` (Q8 → Q11)

### Implementation cho User Story 3

- [ ] T015 [US3] Tạo bảng `lan_nhap_sao_ke` (nhóm 3), `dong_sao_ke` (`ma_giao_dich_ngan_hang UNIQUE`; `trang_thai` `DA_KHOP|CHUA_KHOP|DA_GHI_NHAN|KHONG_THUOC_NCT`) trong `MIG/V017_03__sao_ke.sql`; cài `BE/m11/sodu/saoke/internal/SaoKeService.java` (nhập, khớp không tạo giao dịch, bỏ khớp, gán, xác nhận tạo `NOP_CHUYEN_KHOAN` qua `GhiGiaoDich`) và endpoint `POST /sao-ke/nhap`, `GET /sao-ke/dong`, `POST /sao-ke/dong/{id}/{lenh}`; màn web `web/src/tinh-nang/so-du/sao-ke/` (màn đối soát) với test Vitest (research E-06)

**Checkpoint**: Đối soát chạy độc lập.

---

## Phase 6: User Story 4 – Chốt bảng chi phí tự trừ số dư (Priority: P1)

**Goal**: Listener `BangChiPhiDaChot` tạo đúng một giao dịch `THANH_TOAN_BANG` (âm hoặc dương với bảng bổ sung); không đảo (FR-012, DBR-30, DBR-17).

**Independent Test**: A số dư 10.000.000; chốt tháng 10 tổng 9.300.000, tháng 11 tổng 9.000.000, bảng bổ sung −200.000 → số dư đúng mỗi bước.

### Tests cho User Story 4

- [ ] T016 [P] [US4] Test `BT/m11/DBR_30_ThanhToanBangIT.java` (sự kiện giao lại không tạo giao dịch thứ hai; `UNIQUE bang_chi_phi_id`) (Q12)

### Implementation cho User Story 4

- [ ] T017 [US4] Cài listener `BE/m11/sodu/giaodich/internal/ThanhToanBangListener.java` (idempotent theo `bangId`; `GhiGiaoDich`; tính lại tình trạng) (research E-07)

**Checkpoint**: Thanh toán bảng tự động kèm lệnh chốt.

---

## Phase 7: User Story 5 – Báo "sắp hết tiền" và "còn nợ" (Priority: P1)

**Goal**: `PhiMotNgay` (qua `ChiPhiChoSoDuPort.phiNgayCoSo`), số ngày còn đủ tiền (số dư − tổng chưa chốt), tình trạng theo CFG-M11-04, quyết định báo theo bảng chuyển (ngày, nhắc lại, nộp đủ); dừng nhắc cho hồ sơ trạng thái cuối còn nợ; `SoDuMuaHoPort` cho 010 (FR-016 → FR-021, BR-M11-13, BR-M11-15).

**Independent Test**: Đồng hồ giả lập: số dư giảm qua ngưỡng CFG-M11-04 rồi âm → ngày gửi, người nhận, mức; dừng nhắc khi gia đình nộp đủ.

### Tests cho User Story 5

- [ ] T018 [P] [US5] Test đơn vị `BT/m11/PhiMotNgayTest.java`, `TinhTrangTest.java`, `QuyetDinhBaoTest.java` và `BT/m11/BR_M11_13_TinhTrangIT.java`, `BR_M11_15_MuaHoIT.java` (Q17 → Q25)

### Implementation cho User Story 5

- [ ] T019 [US5] Tạo bảng `lich_su_tinh_trang` (nhóm 3) trong `MIG/V017_04__tinh_trang.sql`; cài `BE/m11/sodu/tinhtrang/internal/TinhTrangService.java` (tính sau mỗi giao dịch và hằng ngày; báo theo `QuyetDinhBao`; phát `TinhTrangSoDuDaDoi`; dừng nhắc), `SoDuMuaHoPortImpl.java` và endpoint `GET /tinh-trang-so-du`, `POST /nguoi-cao-tuoi/{nctId}/so-du/dung-nhac`; màn web `web/src/tinh-nang/so-du/tinh-trang/` (research E-08)

**Checkpoint**: P1 hoàn tất.

---

## Phase 8: User Story 6 – Hoàn tiền, điều chỉnh, giao dịch đảo có Quản lý viện duyệt (Priority: P2)

**Goal**: Loại yêu cầu `M11.GIAO_DICH_SO_DU` (hoàn tiền, hoàn cọc, điều chỉnh, cặp chuyển tiền, đảo); `GioiHanHoan` kiểm lúc lập và lúc duyệt; một đảo mỗi gốc; thanh toán bảng không đảo (FR-010, FR-011, FR-013 → FR-015, BR-M11-10, 11, 14).

**Independent Test**: Số dư 2.000.000; hoàn 3.000.000 (bị chặn), hoàn 2.000.000 (duyệt), đảo phiếu thu ghi nhầm người.

### Tests cho User Story 6

- [ ] T020 [P] [US6] Test đơn vị `BT/m11/GioiHanHoanTest.java` và `BT/m11/BR_M11_10_GiaoDichDuyetIT.java`, `BR_M11_14_HoanTienIT.java` (duyệt hoàn cùng lúc thanh toán bảng) (Q13 → Q16)

### Implementation cho User Story 6

- [ ] T021 [US6] Cài handler `BE/m11/sodu/giaodich/internal/GiaoDichSoDuHandler.java` (`kiemTraKhiDuyet` kiểm lại giới hạn và người nhận qua `QuyenNguoiThanApi`; `apDung` qua `GhiGiaoDich`; đảo đặt `da_dao`) và `GiaoDichService.java` (lập, hủy; cấn trừ cọc của Kế toán); endpoint `/giao-dich`, `/giao-dich/can-tru-coc`, `/giao-dich/cho-duyet`, `/giao-dich/{id}/{lenh}`; màn web `web/src/tinh-nang/so-du/giao-dich/` (màn duyệt hiển thị giới hạn tính lại) (research E-07)

**Checkpoint**: Giao dịch có duyệt chạy độc lập.

---

## Phase 9: User Story 7 – Quyết toán khi kết thúc lưu trú hoặc qua đời (Priority: P2)

**Goal**: `DieuKienQuyetToan` (`KT_H_QUYET_TOAN`); mục việc sau qua đời `QUYET_TOAN`; trình tự cấn trừ cọc, hoàn do Kế toán lập; nhắc hoàn cọc sau trạng thái cuối CFG-M02-09 (FR-015a, FR-022 → FR-024, Q-222).

**Independent Test**: A kết thúc với số dư −1.500.000 và cọc 20.000.000 → trình tự cấn trừ, hoàn, trạng thái điều kiện.

### Tests cho User Story 7

- [ ] T022 [P] [US7] Test đơn vị `BT/m11/DieuKienQuyetToanTest.java` và `BT/m11/FR_022_QuyetToanIT.java`, `FR_024_SauTrangThaiCuoiIT.java` (Q26 → Q31)

### Implementation cho User Story 7

- [ ] T023 [US7] Cài `BE/m11/sodu/quyettoan/internal/` (`DieuKienQuyetToan`, bean `ThamGiaChuyenTrangThai` `@Order` sau 013 với `KT_H_QUYET_TOAN` `coTheMien = true`, `MucViecSauQuaDoiPort` `QUYET_TOAN`, listener `HoSoKetThucDa*`, `QuaDoiDaGhiNhan`, hủy hợp đồng trước hiệu lực → nhắc hoàn cọc; phát `DieuKienQuyetToanDaDoi`) và endpoint `GET /nguoi-cao-tuoi/{nctId}/quyet-toan` (research E-09)

**Checkpoint**: Quyết toán chạy độc lập.

---

## Phase 10: User Story 8 – Xuất sao kê, báo cáo thu chi, sổ cọc (Priority: P2)

**Goal**: Xuất theo khoảng (CFG-M14-01), phạm vi, ghi `lan_xuat_so_du` và nhật ký (FR-025 → FR-027).

**Independent Test**: Dữ liệu tháng 10 của 3 người; xuất ba loại; so với tính tay.

### Tests cho User Story 8

- [ ] T024 [P] [US8] Test `BT/m11/FR_025_XuatIT.java` (Q33, Q34) và bộ SC-001 (3 tháng × 50 hồ sơ) `BT/m11/SC_001_SoDuIT.java`

### Implementation cho User Story 8

- [ ] T025 [US8] Tạo bảng `lan_xuat_so_du` (nhóm 3; `loai` `SAO_KE|THU_CHI|SO_COC`) trong `MIG/V017_05__xuat.sql`; cài `BE/m11/sodu/xuat/internal/XuatSoDuService.java` và endpoint `POST /xuat/so-du`; màn web `web/src/tinh-nang/so-du/xuat/` (research E-10)

**Checkpoint**: Mọi user story của 017 chạy độc lập.

---

## Phase 11: Polish & Cross-Cutting

- [ ] T026 [P] Cài `SoDuThongKeApi` (016); mở rộng `BT/common/HopDongApiTest.java` với `specs/017-resident-balance-ledger/contracts/api-017.yaml`; mở rộng `NhomGhiNhanCoTriggerIT`
- [ ] T027 Thay bản giả `TrangThaiDatCocPort`, `MucViecSauQuaDoiPort` `QUYET_TOAN` (004) và `SoDuMuaHoPort` (010) bằng bản cài của 017; chạy lại test 004, 010
- [ ] T028 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T029 Rà truy vết: DBR-17, 28, 29, 30, 35 và BR-M11-10 → 16 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-028, FR-029, FR-030 — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US1 (P1)** → **US2, US3, US4 (P1)** song song → **US5 (P1)**
- **US6, US7, US8 (P2)**: sau US5; US7 dùng giao dịch của US6

### Parallel Opportunities

- T003, T007, T008 song song; US2, US3, US4 song song

## Parallel Example: P1

```text
Task: "T011 TrangThaiDatCocTest + TongQuyNgayTest + BR_M11_16 + DBR_35"
Task: "T014 KhopMaTest + SaoKeTest + BR_M11_12 + DBR_29"
Task: "T016 DBR_30_ThanhToanBangIT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2 → US4
2. **Dừng và kiểm chứng**: sổ, thu cọc, đặt cọc cho 004, thanh toán bảng khi chốt
3. Thêm US3, US5

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7 → US8; mỗi bước chạy `./mvnw verify`.

## Notes

- 017 không tự tạo giao dịch ngoài thanh toán bảng; không tự hoàn, cấn trừ, đối soát (Q-306, Q-307)
- Không có thanh toán trực tuyến; VietQR chỉ hiển thị
- Mọi mốc đọc `ThamSoApi` (CFG-M11-04, 05, 10, CFG-M02-09, CFG-M14-01, CFG-M15-03 → 06)
