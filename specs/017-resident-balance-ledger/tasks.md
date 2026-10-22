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

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Tên gói, file là tên logic; đặt theo [backend/README.md](../../backend/README.md), research R-02 của 000. Module M11 (phần 017): `BE/m11/sodu/{so,giaodich,tinhtrang,quyettoan,chungtu,xuat,lich}/`; bổ sung `BE/m11/api/`
- Web `frontend/src/tinh-nang/so-du/`, `frontend/src/tinh-nang/cong-nguoi-than/so-du/` (đặt theo [frontend/README.md](../../frontend/README.md)); app người thân `mobile/lib/nguoi-than/so-du/`

---

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/m11/sodu/{so,giaodich,tinhtrang,quyettoan,chungtu,xuat,lich}/`; mở rộng `BT/m11/M11ArchTest.java` (010): chỉ `m11.sodu` dùng `ChiPhiChoSoDuPort`; gói `..lich..` không tạo giao dịch (research E-01, E-02)
- [ ] T002 [P] Thêm ZXing core vào `backend/pom.xml` sau khi chủ dự án duyệt bản (research E-04); kiểm `common.bieumau` (007 T104) và fastexcel (010) đã có
- [ ] T003 [P] Bổ sung kiểu `.xlsx` cho tệp do hệ thống tạo trong `FileApi.upload` của `BE/common/file/` (không mở cho `POST /tep`) và test (research E-11)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Bảng, khai báo `m11.api` của 017, bản cài SPI, provider, bản giả.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T004 Tạo `MIG/V017_01__so_du.sql` theo data-model mục 2 → 7: `so_du` (`ma_nop_tien UNIQUE`), `giao_dich` (`CHECK` cặp sổ, loại, dấu số tiền; `ma_giao_dich_ngan_hang` chỉ mục thường; `bang_chi_phi_id UNIQUE`; trigger nhóm 2 → 3), `lich_su_tinh_trang` (`UNIQUE (nct_id, ngay, nguon)`), `phieu_bao_phi` (`UNIQUE ma_phieu`, `UNIQUE bang_chi_phi_id`), `bang_quyet_toan` (`UNIQUE (nct_id, phien_ban)`, cột sinh `khoa_hien_hanh UNIQUE`, trigger), `lan_xuat_so_du`, `lan_tai_chung_tu`, `su_kien_m11sd_da_xu_ly`, `lan_chay_m11sd`; trigger nhóm 3 theo mẫu 000
- [ ] T005 [P] Khai báo ở `BE/m11/api/`: `SoDuApi` (`soDu`, `dieuKienQuyetToan`, `chungTu`), `SoDuThongKeApi` (`tinhTrang`, `thuChi`), sự kiện `TrangThaiDatCocDaDoi`, `DieuKienQuyetToanDaDoi`, `TinhTrangSoDuDaDoi`, `ChungTuDaPhatHanh` (module-interfaces mục 2.2 → 2.4) và bản giả `BT/support/fake/m11/` cho 004, 012, 016
- [ ] T006 [P] Bản giả cho test của 017 trong `BT/support/`: `ChiPhiChoSoDuPort` (010, có `bangDaChot`), `HoSoKetThucDocApi` và phát sự kiện 004, `QuyenNguoiThanApi` (012) — dùng bản giả do người khai báo viết nếu đã có (docs/team-plan.md mục 3)
- [ ] T007 Cài provider trong `BE/m11/sodu/`: `PhanLoaiTruongProvider` (`TAI_CHINH`), `LuuTruProvider` (`THEO_HO_SO`), `GanPhamViProvider`, `QuyenTepProvider` (`M11.PHIEU_BAO_PHI`, `M11.BANG_QUYET_TOAN`, `M11.BAN_KY_QUYET_TOAN`, `M11.FILE_XUAT`) theo research E-12; khóa `@RequiresPermission` theo bảng E-12
- [ ] T008 [P] Test bảng các hàm thuần dùng chung về sau: `BT/m11/sodu/VietQrPayloadTest.java` (test vector, CRC16) và `TrangThaiDatCocTest.java` (research E-04, E-05, E-13)

**Checkpoint**: Bảng, API, bản giả sẵn sàng.

---

## Phase 3: User Story 1 – Mở sổ, cấp mã nộp tiền, người thân xem số dư và thông tin nộp tiền (Priority: P1) 🎯 MVP

**Goal**: Sổ và mã nộp tiền mở khi hợp đồng đầu tiên Chờ ký; hiển thị theo vai trò; cổng người thân (FR-001 → FR-004).

**Independent Test**: Quickstart Q1, Q2.

### Tests cho User Story 1

- [ ] T009 [P] [US1] Test `BT/m11/sodu/MoSoIT.java`, `SoDuHienThiIT.java`, `CongSoDuIT.java` (Q1, Q2): một sổ, một mã mỗi hồ sơ; sự kiện lặp, trả về nháp, gia hạn không mở thêm; hồ sơ quay lại có sổ mới; HC chỉ thấy số dư, cọc, đặt cọc; DD 403; người thân không có quyền xem chi phí 403; thanh toán bảng chỉ hiện tổng (US1 #1 → #6)

### Implementation cho User Story 1

- [ ] T010 [US1] Cài `BE/m11/sodu/so/` : listener `HopDongDaDoiTrangThai` mở sổ (idempotent), sinh `ma_nop_tien` có ký tự kiểm tra, `VietQrPayload` (CFG-M11-10), `GET /nguoi-cao-tuoi/{nctId}/so-du`, `GET /nguoi-cao-tuoi/{nctId}/giao-dich`, `GET /so-du/tim-theo-ma`, `GET /cong/nguoi-cao-tuoi/{nctId}/so-du` (kiểm `quyenCoTacDung(XEM_CHI_PHI)` mỗi lần) (research E-04)
- [ ] T011 [P] [US1] Màn web số dư trên hồ sơ trong `frontend/src/tinh-nang/so-du/` và cổng `frontend/src/tinh-nang/cong-nguoi-than/so-du/` (vẽ mã QR từ chuỗi payload) cùng test Vitest; app `mobile/lib/nguoi-than/so-du/` theo mục "Giai đoạn hiện tại" của CLAUDE.md

**Checkpoint**: Q1, Q2 xanh.

---

## Phase 4: User Story 2, 3 – Kế toán ghi nhận tiền nộp, tiền cọc; ghi nhận chuyển khoản (Priority: P1)

**Goal**: Tiền mặt và chuyển khoản ghi bằng tay, Đã xác nhận ngay; cảnh báo trùng mã ngân hàng; trạng thái đặt cọc cho 004 (FR-005 → FR-007; BR-M11-12, 16).

**Independent Test**: Quickstart Q3 → Q6.

### Tests cho User Story 2, 3

- [ ] T012 [P] [US2] Test `BT/m11/sodu/BR_M11_16_TienMatIT.java`, `FR_006_DatCocIT.java` (Q3, Q4, Q6): tiền mặt vào số dư, sổ cọc; số tiền ≤ 0 → 422; HC 403; không có số phiếu; đặt cọc đổi khi đủ khoản cọc, `TrangThaiDatCocDaDoi` phát kèm giao dịch và Kế toán; hợp đồng không yêu cầu cọc → `KHONG_YEU_CAU` (US2 #1 → #6)
- [ ] T013 [P] [US3] Test `BT/m11/sodu/BR_M11_12_ChuyenKhoanIT.java` (Q5): ghi theo mã nộp tiền; hai dòng cùng mã trong một lệnh; mã đã dùng → 409 kèm giao dịch trước, gửi lại có cờ → 201; hồ sơ trạng thái cuối vẫn ghi được (US3 #1 → #3, #5; DBR-29)

### Implementation cho User Story 2, 3

- [ ] T014 [US2] Cài `BE/m11/sodu/giaodich/`: `POST /nguoi-cao-tuoi/{nctId}/nop-tien-mat`, `/nop-chuyen-khoan` dưới khóa `so_du`; cập nhật sổ; gọi tính lại tình trạng (để trống tới T021) và trạng thái đặt cọc; cài `TrangThaiDatCocPort`, phát `TrangThaiDatCocDaDoi`; listener `KhoanCocDaDoi` (research E-05)
- [ ] T015 [P] [US3] Màn web ghi nhận tiền nộp trong `frontend/src/tinh-nang/so-du/` (tìm theo mã nộp tiền, hai dòng cọc và số dư, hộp xác nhận khi trùng mã) cùng test Vitest `NopChuyenKhoan.test.jsx`

**Checkpoint**: Q3 → Q6 xanh; 004 nhận trạng thái đặt cọc.

---

## Phase 5: User Story 4 – Chốt bảng chi phí tự trừ số dư (Priority: P1)

**Goal**: Một giao dịch thanh toán mỗi bảng Đã chốt (FR-012, DBR-30).

**Independent Test**: Quickstart Q7.

- [ ] T016 [P] [US4] Test `BT/m11/sodu/DBR_30_ThanhToanBangIT.java`, `BR_M11_11_ThanhToanBangIT.java` (Q7): tổng dương, âm, 0; sự kiện lặp không tạo giao dịch thứ hai; số dư âm hiện "còn nợ" (US4 #1 → #5; SC-002)
- [ ] T017 [US4] Cài listener `BangChiPhiDaChot` trong `BE/m11/sodu/giaodich/` (idempotent theo `bang_chi_phi_id`, cập nhật sổ, tính lại tình trạng, điều kiện quyết toán) (research E-06)

**Checkpoint**: Q7 xanh.

---

## Phase 6: User Story 5 – Báo "sắp hết tiền" và "còn nợ" (Priority: P1)

**Goal**: Tình trạng hằng ngày và khi nộp tiền; báo theo bảng chuyển; dừng nhắc; dấu mua hộ (FR-016 → FR-021; BR-M11-13, 15).

**Independent Test**: Quickstart Q8 → Q10, Q34 → Q36 với đồng hồ giả lập.

### Tests cho User Story 5

- [ ] T018 [P] [US5] Test đơn vị `BT/m11/sodu/PhiMotNgayTest.java`, `TinhTrangTest.java`, `QuyetDinhBaoTest.java` (bảng mọi dòng của bảng chuyển tình trạng; phí 0; bán trú giá buổi) (research E-07)
- [ ] T019 [P] [US5] Test `BT/m11/sodu/BR_M11_13_TinhTrangIT.java`, `BR_M11_15_DauMuaHoIT.java`, `FR_020_KhongChanIT.java` (Q8 → Q10, Q34 → Q36): người nhận và phần nội dung theo quyền xem chi phí; chu kỳ CFG-M11-05; báo Quản lý viện khi còn nợ kéo dài; nộp tiền về Bình thường, chưa đủ (Q-252, Q-257); chạy lại cùng ngày không báo trùng (SC-005, SC-012); dừng nhắc chỉ hồ sơ trạng thái cuối

### Implementation cho User Story 5

- [ ] T020 [US5] Cài `BE/m11/sodu/tinhtrang/` (`PhiMotNgay`, `TinhTrang`, `QuyetDinhBao`, ghi `lich_su_tinh_trang`, thông báo theo FR-030 với phần `CHUNG` / `CHI_PHI`), `SoDuMuaHoPort`, lệnh `POST /nguoi-cao-tuoi/{nctId}/so-du/dung-nhac`, `GET /tinh-trang-so-du`
- [ ] T021 [US5] Cài job `BE/m11/sodu/lich/ViecHangNgayM11SoDu.java` (02:00, ShedLock, chạy bù; phần tình trạng và đối chiếu bất biến sổ) và nối tính lại ngay trong lệnh nộp tiền (T014)
- [ ] T022 [P] [US5] Màn web tình trạng số dư trong `frontend/src/tinh-nang/so-du/` (danh sách Sắp hết tiền, Còn nợ; dừng nhắc của Quản lý viện)

**Checkpoint**: Q8 → Q10, Q34 → Q36 xanh.

---

## Phase 7: User Story 6 – Hoàn tiền và điều chỉnh có Quản lý viện duyệt (Priority: P2)

**Goal**: Hoàn tiền, hoàn cọc, điều chỉnh Chờ duyệt qua yêu cầu phê duyệt của 000; kiểm lại khi duyệt; sửa sai bằng điều chỉnh trỏ giao dịch gốc; cấn trừ cọc; nhắc hoàn cọc (FR-010 → FR-015a; BR-M11-10, 14).

**Independent Test**: Quickstart Q11 → Q15, Q24.

### Tests cho User Story 6

- [ ] T023 [P] [US6] Test đơn vị `BT/m11/sodu/GioiHanHoanTest.java` (FR-014 mọi nhánh)
- [ ] T024 [P] [US6] Test `BT/m11/sodu/BR_M11_14_HoanTienIT.java`, `BR_M11_10_DieuChinhIT.java`, `DBR_17_GiaoDichKhongSuaIT.java`, `FR_015_CanTruCocIT.java`, `FR_015a_NhacHoanCocIT.java` (Q11 → Q15, Q24): Chờ duyệt không đổi sổ; duyệt kiểm lại giới hạn, người nhận; từ chối, hủy; điều chỉnh trỏ gốc đặt `da_dieu_chinh`; không trỏ thanh toán bảng; không có lệnh sửa, xóa giao dịch Đã xác nhận (trigger); cặp cấn trừ cùng số tiền, ngược dấu (SC-007, SC-013)

### Implementation cho User Story 6

- [ ] T025 [US6] Cài `POST /giao-dich`, `GET /giao-dich/cho-duyet` và handler `M11.GIAO_DICH_SO_DU` (`LoaiYeuCauHandler` của 000: `kiemTraKhiDuyet`, `apDung`) trong `BE/m11/sodu/giaodich/` (research E-06)
- [ ] T026 [US6] Cài `POST /nguoi-cao-tuoi/{nctId}/can-tru-coc` (điều kiện qua `HoSoKetThucDocApi`, `DanhSachViecSauQuaDoiApi`, giao dịch thanh toán kỳ cuối) và nhắc hoàn cọc (listener hợp đồng hủy, Hủy tiếp nhận; phần nhắc của job T021)
- [ ] T027 [P] [US6] Màn web lập hoàn tiền, điều chỉnh, cấn trừ trong `frontend/src/tinh-nang/so-du/`; giao dịch Chờ duyệt hiện trong hộp duyệt của 000 (T097 của 000) với chi tiết do 017 trả

**Checkpoint**: Q11 → Q15, Q24 xanh.

---

## Phase 8: User Story 7, 10 – Quyết toán: điều kiện, Xác nhận quyết toán, bảng quyết toán (Priority: P2)

**Goal**: Hai mã điều kiện cho 004; mục việc sau qua đời; Xác nhận quyết toán dựng bảng PDF có phiên bản; bản ký; nhắc sau trạng thái cuối (FR-022 → FR-024, FR-034; BR-M11-22; DBR-38).

**Independent Test**: Quickstart Q16 → Q23.

### Tests cho User Story 7, 10

- [ ] T028 [P] [US7] Test đơn vị `BT/m11/sodu/DieuKienQuyetToanTest.java`: ba ý tiền và ý xác nhận; ngoại lệ chỉ miễn ba ý tiền; còn Chờ duyệt thì ý xác nhận Chưa đạt dù có ngoại lệ (BR-M11-22, chủ dự án chọn 2026-10-22); giao dịch sau xác nhận làm ý xác nhận Chưa đạt; bảng bổ sung chưa chốt không ảnh hưởng
- [ ] T029 [P] [US7] Test `BT/m11/sodu/FR_022_DieuKienQuyetToanIT.java`, `FR_023_ViecSauQuaDoiIT.java`, `FR_024_SauTrangThaiCuoiIT.java` (Q16, Q22, Q23): bean trả đúng hai mã và `coTheMien`; `DieuKienQuyetToanDaDoi` phát khi đổi; mục `QUYET_TOAN` Hoàn thành, Không áp dụng; nhắc sau trạng thái cuối tới khi số dư 0 và xác nhận lại
- [ ] T030 [P] [US10] Test `BT/m11/sodu/BR_M11_22_XacNhanQuyetToanIT.java` (Q17 → Q21): điều kiện lệnh (căn cứ, ba ý tiền hoặc ngoại lệ, không còn Chờ duyệt); phiên bản tăng, một hiện hành; nội dung bảng khớp sổ, kỳ cuối, thông tin hồ sơ kết thúc; dòng "Chứng từ này không thay thế hóa đơn"; bản ký một lần vào phiên bản hiện hành; người thân có quyền xem chi phí được báo (SC-008, SC-015)

### Implementation cho User Story 7, 10

- [ ] T031 [US7] Cài `BE/m11/sodu/quyettoan/`: `DieuKienQuyetToan`, bean `ThamGiaChuyenTrangThai` (hai mã), `MucViecSauQuaDoiPort` (`QUYET_TOAN`), listener `HoSoKetThucDa*`, `QuaDoiDaGhiNhan`, `NgoaiLeKetThucDaDuyet`, phát `DieuKienQuyetToanDaDoi`; phần nhắc sau trạng thái cuối của job T021 (research E-08)
- [ ] T032 [US10] Cài lệnh `POST /nguoi-cao-tuoi/{nctId}/quyet-toan/xac-nhan` (chụp số liệu, dựng PDF bằng `PdfBieuMau`, `FileApi.upload` + `attach`, phiên bản), `GET /nguoi-cao-tuoi/{nctId}/quyet-toan`, `POST /bang-quyet-toan/{id}/ban-ky`, `GET /bang-quyet-toan/{id}/file` (ghi `lan_tai_chung_tu`) (research E-09)
- [ ] T033 [P] [US10] Màn web quyết toán trong `frontend/src/tinh-nang/so-du/` (hai mã điều kiện kèm lý do, nút xác nhận, phiên bản, tải PDF, đính kèm bản ký) cùng test Vitest `QuyetToan.test.jsx`

**Checkpoint**: Q16 → Q23 xanh; 004 chạy Kết thúc lưu trú với bean thật của 017 (004 T106).

---

## Phase 9: User Story 9 – Phiếu báo phí (Priority: P2)

**Goal**: Phát hành phiếu PDF, Excel cho mỗi bảng Đã chốt, mã duy nhất, tải lại đúng file, người thân được báo và tải (FR-031, FR-032, FR-037, FR-038; BR-M11-21, 23; DBR-37).

**Independent Test**: Quickstart Q25 → Q28.

- [ ] T034 [P] [US9] Test `BT/m11/sodu/BR_M11_21_PhieuBaoPhiIT.java`, `BR_M11_23_ChungTuIT.java`, `CongChungTuIT.java` (Q25 → Q28): danh sách chờ phát hành; phát hành lần hai 409; nội dung PDF (trích chữ) và Excel (đọc ô) cùng số liệu; số dư trước, sau khớp sổ; khi âm có số còn nợ, mã nộp tiền, tài khoản, ảnh QR; không có tên thuốc; tải lại cùng byte; mỗi lần tải ghi nhật ký; người thân không có quyền xem chi phí 403 (SC-014, SC-016, SC-017)
- [ ] T035 [US9] Cài `BE/m11/sodu/chungtu/`: `GET /phieu-bao-phi/cho-phat-hanh`, `POST /bang-chi-phi/{bangId}/phieu-bao-phi` (chụp số liệu từ `ChiPhiChoSoDuPort.bangDaChot` và sổ; PDF qua `PdfBieuMau` kèm ảnh QR ZXing; Excel qua `ExcelBieuMau`; `FileApi`; thông báo, `ChungTuDaPhatHanh`), `GET /nguoi-cao-tuoi/{nctId}/phieu-bao-phi`, `GET /phieu-bao-phi/{id}/file`; cổng `GET /cong/nguoi-cao-tuoi/{nctId}/chung-tu`, `GET /cong/chung-tu/{loai}/{id}/file` (research E-10)
- [ ] T036 [P] [US9] Màn web phiếu báo phí trong `frontend/src/tinh-nang/so-du/` (danh sách chờ phát hành, phát hành, tải) (mục chứng từ trên cổng làm ở 012 T073); test Vitest `PhieuBaoPhi.test.jsx`; app người thân tải chứng từ theo mục "Giai đoạn hiện tại"

**Checkpoint**: Q25 → Q28 xanh.

---

## Phase 10: User Story 8 – Xuất sao kê và báo cáo thu chi (Priority: P2)

**Goal**: Sao kê và báo cáo thu chi thu gọn, Excel và PDF cùng số liệu; người thân tải sao kê (FR-025 → FR-027; Q-381, Q-386).

**Independent Test**: Quickstart Q29 → Q33.

- [ ] T037 [P] [US8] Test đơn vị `BT/m11/sodu/SaoKeTest.java`, `BaoCaoThuChiTest.java` và tích hợp `FR_025_XuatIT.java`, `FR_026_SaoKeCongIT.java` (Q29 → Q33): đầu kỳ + tổng = cuối kỳ từng sổ; chỉ Đã xác nhận; ba tổng và danh sách còn nợ theo số dư cuối khoảng; Excel và PDF cùng số liệu; khoảng > CFG-M14-01 → 422; HC 403; mỗi lần xuất ghi `lan_xuat_so_du`, file không đổi khi dữ liệu đổi (SC-009)
- [ ] T038 [US8] Cài `BE/m11/sodu/xuat/`: `POST /xuat/sao-ke`, `POST /xuat/bao-cao-thu-chi`, `GET /xuat/{id}/file`, `POST /cong/nguoi-cao-tuoi/{nctId}/sao-ke` (fastexcel, `PdfBieuMau`, `FileApi`); `SoDuThongKeApi.thuChi`, `tinhTrang` cho 016 (research E-11)
- [ ] T039 [P] [US8] Màn web xuất trong `frontend/src/tinh-nang/so-du/` và nút tải sao kê trên cổng

**Checkpoint**: Q29 → Q33 xanh.

---

## Phase 11: Polish & Cross-Cutting

- [ ] T040 [P] Test `BT/m11/sodu/DBR_28_SoDuBatBienIT.java` (bộ 3 tháng × 50 hồ sơ, SC-001), `DongThoiSoDuIT.java` (hai lần nộp; duyệt cùng thanh toán; hai lần phát hành; hai lần xác nhận), `QuyenSoDuIT.java` (bảng quyền research E-12; SC-010), `LuuTruSoDuIT.java` (FR-029)
- [ ] T041 [P] Mở rộng `BT/common/HopDongApiTest.java` so springdoc với `specs/017-resident-balance-ledger/contracts/api-017.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 017
- [ ] T042 Khi 004, 010, 012 có bản thật, chạy lại test US2, US6, US7, US9 với bean thật thay bản giả (T006)
- [ ] T043 Chạy `./mvnw verify`, `npm test`, `npm run lint`, `npm run build`, `flutter test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T044 Rà truy vết data-model mục 9: DBR-17, 28, 29, 30, 37, 38 và BR-M11-10 → 16, 21 → 23 có test; mọi FR còn hiệu lực của spec có task hoặc test (constitution XV)

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
