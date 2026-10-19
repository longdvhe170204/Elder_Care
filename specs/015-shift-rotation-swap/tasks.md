---
description: "Danh sách task triển khai feature 015 – Lịch ca xoay vòng, phủ ca, đổi ca và nghỉ đột xuất"
---

# Tasks: Lịch ca xoay vòng, phủ ca tối thiểu, đổi ca và nghỉ đột xuất (015)

**Input**: Tài liệu thiết kế ở `specs/015-shift-rotation-swap/` (plan.md, spec.md, research.md X-01 → X-13, data-model.md, contracts/api-015.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 008 đã xong (port nội bộ `CaLenhPort`, `PhanCongLenhPort`, sự kiện nội bộ M09, chỗ gọi `KiemTraCongBoMoRong`); 005 (`CongViecCaApi`), 014 (`ChuyenDiNhiemVuLenhApi`) đã có `api`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M09 (phần 015): `BE/m09/{xoayca,phuca,doica}/`; port bổ sung ở `BE/m09/lichca/port/`
- Web `frontend/src/tinh-nang/lich-ca/{xoay-ca,phu-ca,doi-ca,nghi,loi-moi}/`; app nhân viên `mobile/lib/nhan-vien/lich-ca/`

---

## Phase 1: Setup

- [ ] T001 Tạo ba gói con `BE/m09/{xoayca,phuca,doica}/`; mở rộng ArchUnit `BT/m09/M09ArchTest.java` (008 T001): chỉ `m09.doica`, `m09.xoayca` dùng port nội bộ; gói `..lich..` không lập lịch, không chỉ định người phụ trách, không gửi lời mời (research X-01)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Port bổ sung của 008, bảng mẫu, nhóm, hàm thuần xếp ca và tính phủ, khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T002 Bổ sung port trong `BE/m09/lichca/port/`: `LichNhapLenhPort` (`taoNhap` kiểm không ghi đè lịch tháng), `NghiCoDuyetPort`; cài ở gói `m09.lichca.internal` của 008 (contracts mục 1.2; research X-03)
- [ ] T003 [P] Bổ sung `BE/m09/api/XoayCaThongKeApi.java` và sự kiện `CaThieuPhuDaMo`, `CaThieuPhuDaDong` cùng thông báo (cài `CoYeuCauThongBao`) (contracts mục 2.1, 2.3)
- [ ] T004 Tạo `MIG/V015_01__xoay_ca.sql`: `mau_xoay_ca` (`loai_pham_vi` `TANG_KHU|TOAN_VIEN`; `do_dai_chu_ky`), `mau_xoay_ca_ngay` (`thu_tu` 0 … L−1; `mau_ca_id NULL` là nghỉ), `nhom_xoay_ca` (`ngay_goc`, `ngung_tu`), `thanh_vien_nhom` (`vi_tri_bat_dau` 0 … L−1; không chồng khoảng cùng nhân viên kiểm ở backend), `moc_nhac_lich` (`UNIQUE` năm cột), `lan_chay_m09xc` (data-model mục 2, 3)
- [ ] T005 [P] Cài hàm thuần `BE/m09/xoayca/internal/MauCaTaiNgay.java`, `LapLichThang.java` và test `BT/m09/MauCaTaiNgayTest.java`, `LapLichThangTest.java` (SC-001: 3 mẫu × 4 tháng; vị trí chu kỳ nối tiếp qua tháng) (research X-05)
- [ ] T006 [P] Cài hàm thuần `BE/m09/phuca/internal/DongApDung.java`, `TinhPhu.java` (luồng cực đại phân người vào dòng yêu cầu CFG-M09-07, tie-break theo thứ tự khai báo, xét giấy phép, đào tạo qua `HieuLucChungChi`) và test `BT/m09/DongApDungTest.java`, `TinhPhuTest.java` (research X-06)
- [ ] T007 Cài khung job `BE/m09/xoayca/lich/ViecM09XoayCa.java` (ShedLock, `lan_chay_m09xc`) cho nhắc lập lịch CFG-M09-06, nhắc công bố, Hết hiệu lực yêu cầu, tách ca theo giờ, đóng cảnh báo hết ca (research X-13)
- [ ] T008 [P] Cài provider: `GanPhamViProvider` (`M09.DOI_CA`, `M09.NGHI_DOT_XUAT`, `M09.THIEU_PHU`, `M09.LOI_MOI`), `PhanLoaiTruongProvider` (lý do `LY_DO_NHAN_SU`), `LuuTruProvider` (≥ CFG-M15-04)

**Checkpoint**: Nền 015 sẵn sàng.

---

## Phase 3: User Story 1 – Lập bản nháp lịch ca tháng từ mẫu xoay ca (Priority: P1) 🎯 MVP

**Goal**: Mẫu, nhóm, thành viên; lệnh "Lập lịch từ mẫu" và "Lập lại" (xem trước, lý do); báo cáo lập lịch; không ghi đè; người phụ trách để trống; nhắc lập (FR-001 → FR-004, FR-008 → FR-014, BR-M09-09).

**Independent Test**: Mẫu 6 ngày, nhóm tầng 2 gồm 6 nhân viên vị trí 0 → 5; tới mốc CFG-M09-06 tháng 11 → lời nhắc; lập lịch → đúng chu kỳ, nối tiếp tháng 10.

### Tests cho User Story 1

- [ ] T009 [P] [US1] Test `BT/m09/BR_M09_09_LapLichTuMauIT.java`, `FR_013_LapLaiIT.java`: hai lệnh lập lịch đồng thời; lịch đã có không bị ghi đè; lập lại thay bản Nháp có lý do; không tự lập lịch (Q1 → Q7)

### Implementation cho User Story 1

- [ ] T010 [US1] Tạo bảng `lan_lap_lich`, `bao_cao_lap_lich_muc` (nhóm 3; `loai` `LAP|LAP_LAI`; `ket_qua` `THANH_CONG|LOI`) trong `MIG/V015_02__lap_lich.sql`
- [ ] T011 [US1] Cài `BE/m09/xoayca/internal/XoayCaService.java` (mẫu, nhóm, thành viên; Trưởng tầng, Quản lý viện) và `LapLichService.java` (lập, xem trước lập lại, lập lại qua `LichNhapLenhPort`, `CaLenhPort.themVaoCa`; báo cáo; nhắc lập) (research X-05)
- [ ] T012 [US1] Tạo endpoint `/xoay-ca/mau`, `/xoay-ca/nhom`, `/{id}`, `/{id}/thanh-vien`, `/xoay-ca/lap-lich`, `/lap-lai/xem-truoc`, `/lap-lai`, `/lan-lap-lich`; màn web `frontend/src/tinh-nang/lich-ca/xoay-ca/`

**Checkpoint**: Lập lịch từ mẫu chạy độc lập.

---

## Phase 4: User Story 2 – Yêu cầu phủ ca tối thiểu và công bố lịch (Priority: P1)

**Goal**: Phủ từng ca theo CFG-M09-07 có xét chứng chỉ; mục thiếu ca không tạo ca rỗng; `KiemTraCongBoMoRong` trong lệnh công bố của 008 (hai danh sách: chặn và cần xác nhận); nhắc công bố mỗi ngày; tính lại theo sự kiện nội bộ (FR-005 → FR-007, FR-015 → FR-018, Q-178).

**Independent Test**: Yêu cầu chung ≥ 1 Điều dưỡng và riêng ca ngày tầng 3 ≥ 2 Nhân viên chăm sóc; lịch có ba ca thiếu phủ → ba ca được đánh dấu; công bố hiển thị hai danh sách.

### Tests cho User Story 2

- [ ] T013 [P] [US2] Test `BT/m09/FR_015_NhacCongBoIT.java` và kiểm tra công bố trong `BT/m09/FR_016_CongBoIT.java` (008) mở rộng cho phủ (Q8 → Q12)

### Implementation cho User Story 2

- [ ] T014 [US2] Tạo bảng `muc_thieu_ca` (`UNIQUE (lich_ca_id, ngay, mau_ca_id)`; `trang_thai` `MO|DA_CO_CA|DA_CONG_BO`), `lan_tinh_phu`, `lan_tinh_phu_dong` (nhóm 3; `su_kien` nhiều giá trị) trong `MIG/V015_03__phu_ca.sql`
- [ ] T015 [US2] Cài `BE/m09/phuca/internal/PhuCaService.java` (listener `LichNhapDaLuu`, `LichCaDaCongBo`, `CaDaThayDoi`, `CaDaHuy`, `NhanVienDaBoSungVaoCa`, `NhanVienDaNghiViec`, `GiayPhepDaDoiHieuLuc`, `DaoTaoDaDoiHieuLuc`; ghi `lan_tinh_phu`) và `KiemTraCongBoMoRongImpl.java` (research X-06, X-07)
- [ ] T016 [US2] Tạo endpoint `GET /ca/{id}/phu`, `/lich-ca/{id}/phu`; màn web `frontend/src/tinh-nang/lich-ca/phu-ca/` và màn công bố hiển thị hai danh sách với test Vitest

**Checkpoint**: P1 hoàn tất.

---

## Phase 5: User Story 3 – Đổi ca: người nhận đồng ý, trưởng tầng duyệt (Priority: P2)

**Goal**: Loại yêu cầu `M09.DOI_CA` (hai chiều, nhận thay) dùng vòng đời mở rộng của 000; `KiemTraDoiCa` lúc lập, đồng ý, duyệt; tác động khi duyệt trong một transaction (ca, phân công, người phụ trách, `CongViecCaApi`, nhiệm vụ chuyến đi 014); Hết hiệu lực; nhắc CFG-M09-11; riêng tư lý do (FR-019 → FR-025, BR-M09-10).

**Independent Test**: Trên lịch đã công bố: một yêu cầu hợp lệ, một gây chồng giờ, một người nhận thiếu giấy phép cho phân công Điều dưỡng → đúng kết quả từng bước.

### Tests cho User Story 3

- [ ] T017 [P] [US3] Test đơn vị `BT/m09/KiemTraDoiCaTest.java` và `BT/m09/BR_M09_10_DoiCaIT.java`: duyệt đổi ca và vắng ca đồng thời; một yêu cầu mở mỗi (ca, nhân viên); chỉ A thấy lý do từ chối của B (Q13 → Q20, Q34)

### Implementation cho User Story 3

- [ ] T018 [US3] Tạo bảng `yeu_cau_doi_ca` (`yeu_cau_id` khóa chính trỏ `yeu_cau_phe_duyet`; `dang` `HAI_CHIEU|NHAN_THAY`; `trang_thai` 7 giá trị đồng bộ qua `chuyenMoRong`), `yeu_cau_ca_mo` (`UNIQUE (ca_id, nhan_vien_id)`) trong `MIG/V015_04__doi_ca.sql`
- [ ] T019 [US3] Cài handler `BE/m09/doica/internal/DoiCaHandler.java` cho `M09.DOI_CA` (`chuyenMoRong` cho `CHO_NGUOI_NHAN_DONG_Y`, `NGUOI_NHAN_TU_CHOI`, `HET_HIEU_LUC`; không có `NHAP`; `kiemTraKhiDuyet`; `khiDuyet` hoặc `apDung` gọi `CaLenhPort`, `PhanCongLenhPort`, `CongViecCaApi.khiKetThucPhanCong`, `ChuyenDiNhiemVuLenhApi.goNhiemVu`; tự duyệt Q-10) và `DoiCaService.java` (lập, đồng ý, từ chối, rút, hủy) (research X-08)
- [ ] T020 [US3] Tạo endpoint `/doi-ca`, `/doi-ca/ca-cua-nguoi-nhan`, `/{id}/kiem-tra`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/lich-ca/doi-ca/`; app `mobile/lib/nhan-vien/lich-ca/` (SC-006 ≤ 2 phút) với test flutter

**Checkpoint**: Đổi ca chạy độc lập.

---

## Phase 6: User Story 4 – Nghỉ đột xuất có duyệt (Priority: P2)

**Goal**: `M09.NGHI_DOT_XUAT` theo ca hoặc theo ngày; lập thay; màn tác động trước duyệt; duyệt một phần; tác động (rút khỏi ca, phân công, `CongViecCaApi.khiNghiCoDuyet`, người phụ trách mới); theo ngày chuyển theo ca khi lịch công bố (FR-026 → FR-031).

**Independent Test**: Nghỉ ba ca, một ca làm tầng thiếu Điều dưỡng; xem tác động trước duyệt; duyệt; kiểm lịch, phân công, công việc.

### Tests cho User Story 4

- [ ] T021 [P] [US4] Test `BT/m09/FR_029_NghiTheoCaIT.java`, `FR_026a_NghiTheoNgayIT.java` (Q21 → Q27)

### Implementation cho User Story 4

- [ ] T022 [US4] Tạo bảng `yeu_cau_nghi` (`dang` `THEO_CA|THEO_NGAY`; `lap_thay`), `yeu_cau_nghi_muc` (`ket_qua` `CHO|DUYET|TU_CHOI|DA_TACH|DA_ROI`), `nghi_theo_ngay` (nhóm 3; `UNIQUE (nhan_vien_id, ngay)`) trong `MIG/V015_05__nghi.sql`
- [ ] T023 [US4] Cài handler `BE/m09/doica/internal/NghiDotXuatHandler.java` (`DA_AP_DUNG_MOT_PHAN`, `HET_HIEU_LUC`; tác động qua `NghiCoDuyetPort`, `CaLenhPort`) và `NghiService.java` (tác động trước duyệt; tách ca theo giờ trong job; listener `LichCaDaCongBo`) (research X-09)
- [ ] T024 [US4] Tạo endpoint `/nghi-dot-xuat`, `/{id}/truoc-khi-duyet`, `/{id}/{lenh}`; màn web `frontend/src/tinh-nang/lich-ca/nghi/`; app phần nghỉ

**Checkpoint**: Nghỉ đột xuất chạy độc lập.

---

## Phase 7: User Story 5 – Cảnh báo thiếu phủ; trưởng tầng chọn và mời người thay (Priority: P3)

**Goal**: Một cảnh báo mở mỗi ca; tra cứu người thay không xếp hạng (sắp theo họ tên, kèm giờ trong tháng, chồng giờ); lời mời là lệnh bổ sung vào ca khi người được mời nhận; hết hiệu lực khi đủ phủ (FR-032 → FR-039, BR-M09-11).

**Independent Test**: Ca đêm tầng 2 thiếu một Điều dưỡng sau duyệt nghỉ; năm điều dưỡng với số giờ khác nhau, một người chồng ca → tra cứu không xếp hạng; mời; nhận; cảnh báo đóng.

### Tests cho User Story 5

- [ ] T025 [P] [US5] Test đơn vị `BT/m09/DieuKienNguoiThayTest.java`, `GioTrongThangTest.java` và `BT/m09/BR_M09_11_ThieuPhuIT.java`, `FR_038_LoiMoiIT.java`, `FR_045_RiengTuIT.java`, `SC_004_BoYeuCauIT.java`: hai người nhận lời mời chỗ cuối đồng thời (Q28 → Q33, Q35, Q36)

### Implementation cho User Story 5

- [ ] T026 [US5] Tạo bảng `canh_bao_thieu_phu` (`trang_thai` `DANG_MO|DONG_DU_PHU|DONG_HET_CA`), `loi_moi_nhan_ca` (`trang_thai` `CHO_TRA_LOI|DA_NHAN|DA_TU_CHOI|DA_RUT|HET_HIEU_LUC`) trong `MIG/V015_06__thieu_phu.sql`; cài `BE/m09/phuca/internal/ThieuPhuService.java` (mở/cập nhật/đóng; phát `CaThieuPhuDaMo/DaDong`) và `BE/m09/doica/internal/LoiMoiService.java` (gửi, nhận → `CaLenhPort.themVaoCa`, từ chối, rút, hết hiệu lực) (research X-10)
- [ ] T027 [US5] Tạo endpoint `/canh-bao-thieu-phu`, `/ca/{id}/tra-cuu-nguoi-thay`, `/loi-moi`, `/loi-moi/{id}/{lenh}`; màn web `frontend/src/tinh-nang/lich-ca/loi-moi/` (sắp theo họ tên, không cột xếp hạng) với test Vitest; app trả lời lời mời

**Checkpoint**: Mọi user story của 015 chạy độc lập.

---

## Phase 8: Polish & Cross-Cutting

- [ ] T028 [P] Cài `XoayCaThongKeApi` (016); mở rộng `BT/common/HopDongApiTest.java` với `specs/015-shift-rotation-swap/contracts/api-015.yaml`
- [ ] T029 Thay bản giả `KiemTraCongBoMoRong` ở test 008 (008 T002) bằng bản cài của 015 và chạy lại
- [ ] T030 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC-001, SC-004, SC-006 theo quickstart mục 5
- [ ] T031 Rà truy vết: DBR-21 (qua 008), DBR-25 và BR-M09-09 → 11 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-040, FR-041, FR-042, FR-043, FR-044, FR-045, FR-046 — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US1 → US2 (P1)**
- **US3, US4 (P2)**: sau US2 (tính phủ), song song với nhau
- **US5 (P3)**: sau US2; dùng tình huống thiếu phủ từ US4

### Parallel Opportunities

- T003, T005, T006, T008 song song; US3 và US4 song song

## Parallel Example: User Story 3

```text
Task: "T017 KiemTraDoiCaTest + BR_M09_10_DoiCaIT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2
2. **Dừng và kiểm chứng**: lập lịch từ mẫu, tính phủ, công bố có kiểm phủ
3. Thêm US3, US4, US5

### Incremental Delivery

US1 → US2 → US3 → US4 → US5; mỗi bước chạy `./mvnw verify`.

## Notes

- 015 không tự lập lịch, không chỉ định người phụ trách, không gợi ý hay xếp hạng người thay, không tự mời (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M09-02, 06, 07, 10, 11, CFG-M15-04 → 06)
