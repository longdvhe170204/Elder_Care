---
description: "Danh sách task triển khai feature 016 – Báo cáo và dashboard"
---

# Tasks: Báo cáo và dashboard (016)

**Input**: Tài liệu thiết kế ở `specs/016-reports-dashboard/` (plan.md, spec.md, research.md O-01 → O-11, data-model.md, contracts/api-016.yaml, contracts/chi-tieu.md, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 (phạm vi, `boLocLichSu`, thu hẹp `DASHBOARD_BAO_CAO`), 008 (lịch sử ca) đã xong; các module nguồn đã cài `*ThongKeApi`: 001 `NguoiCaoTuoiThongKeApi`, 003 `TinhTrangGiuongApi`, `GiuongThongKeApi`, 004 `LuuTruThongKeApi`, 005 `CongViecThongKeApi`, 006 `ThuocThongKeApi`, 007 `CanhBaoThongKeApi`, `ChiSoThongKeApi`, 008 `NhanSuThongKeApi`, 010 `ChiPhiThongKeApi`, 011 `DinhDuongThongKeApi`, 014 `HoatDongThongKeApi`, 015 `XoayCaThongKeApi`.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Dùng ở giai đoạn triển khai; CLAUDE.md hiện chặn `/speckit-implement`.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/vn/eldercare/`, `BT/` = `backend/src/test/java/vn/eldercare/`
- Module M14: `BE/m14/{chitieu,phamvi,dashboard,baocao,chitiet,xuat}/`; không có bảng riêng (data-model mục 1)
- Web `web/src/tinh-nang/bao-cao/{dashboard,nguoi-cao-tuoi,cham-soc,chat-luong,suc-khoe-thuoc,chi-phi,nhan-su-ca,chi-tiet}/`

---

## Phase 1: Setup

- [ ] T001 Tạo khung gói `BE/m14/{chitieu,phamvi,dashboard,baocao,chitiet,xuat}/`; ArchUnit `BT/m14/M14ArchTest.java`: cấm gọi API lệnh của module khác, cấm repository ghi; mọi service `@Transactional(readOnly = true)` trừ ghi nhật ký xuất (research O-01, O-09)
- [ ] T002 [P] Thêm thư viện tạo XLSX (Apache POI) vào `backend/pom.xml` (research O-08)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khung chỉ tiêu (hàm tính và hàm chi tiết dùng cùng tập bản ghi), phạm vi báo cáo lọc hai bước, quyền nhóm chỉ tiêu, giới hạn trường.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 Tạo mô hình đọc `BE/m14/chitieu/ChiTieu.java`, `GiaTriChiTieu.java` (nhãn `KHONG_CO_DU_LIEU`, `CHUA_CAU_HINH`, `NGAY_CHUA_KET_THUC`, `CHO_XEM_LAI:n`, `LOAI_BO_LUU_TRU:loai`), `DongChiTiet.java` và khung đăng ký chỉ tiêu cố định trong mã: mỗi mã ở `contracts/chi-tieu.md` có hàm tính thuần và hàm chi tiết trên cùng tập bản ghi đã lọc (data-model mục 2; research O-05; FR-003, FR-009)
- [ ] T004 Cài `BE/m14/phamvi/PhamViBaoCao.java` (dựng từ `PhamViApi.boLoc`/`boLocLichSu` của 002 và lịch sử ca 008; lọc hai bước: lấy tập bản ghi theo phạm vi thô rồi lọc theo khoảng thời gian của từng phạm vi) và `QuyenNhomChiTieu.java` (nhóm quyền FR-010; thu hẹp `DASHBOARD_BAO_CAO` theo `nhom_chi_tieu`, `kenh`) với test `BT/m14/PhamViBaoCaoTest.java` (research O-03)
- [ ] T005 [P] Cài giới hạn trường cho DTO và cột file (`@NhomTruong`; tên người cao tuổi rút gọn Q-196; tên thuốc ẩn với vai trò không xem) (research O-04)
- [ ] T006 [P] Bổ sung kiểm CFG-M14-01 (khoảng tối đa) dùng chung cho báo cáo và xuất (FR-060)

**Checkpoint**: Khung báo cáo sẵn sàng.

---

## Phase 3: User Story 1 – Dashboard trong ngày cho Quản lý viện và trưởng tầng (Priority: P1) 🎯 MVP

**Goal**: Dashboard theo vai trò cho ngày và ca hiện tại; khối tính song song; bộ đệm 20 giây; làm mới 30 giây bằng ETag (FR-020, FR-021, SC-002, SC-009).

**Independent Test**: Dữ liệu một ngày cho 2 tầng (40 công việc, 3 Quá hạn, cảnh báo mở có dấu leo thang tối đa, sự cố…) → số trên dashboard đúng theo vai trò.

### Tests cho User Story 1

- [ ] T007 [P] [US1] Test `ChiTieu_*Test` cho các mã dashboard (`NCT.DANG_LUU_TRU`, `GIUONG.TRANG_THAI`, `CV.HOM_NAY`, `CV.QUA_HAN`, `HD.*`, `SK.CANH_BAO`, `SK.SU_CO`…) trong `BT/m14/chitieu/` (SC-001; Q14 → Q16); test hiệu năng dashboard 60 người dùng (SC-002) trong `BT/m14/DashboardHieuNangIT.java`

### Implementation cho User Story 1

- [ ] T008 [US1] Cài hàm tính và chi tiết cho các chỉ tiêu dashboard trong `BE/m14/chitieu/` gọi `*ThongKeApi` tương ứng (contracts/chi-tieu.md)
- [ ] T009 [US1] Cài `BE/m14/dashboard/internal/DashboardService.java` (khối theo vai trò, song song, bộ đệm trong tiến trình 20 giây, ETag) và `BE/m14/dashboard/web/DashboardController.java` (`GET /dashboard`, `GET /chi-tieu`) (research O-06)
- [ ] T010 [P] [US1] Tạo màn web `web/src/tinh-nang/bao-cao/dashboard/` (định nghĩa chỉ tiêu tại chỗ; "—" khi mẫu số 0; làm mới 30 giây với ETag) với test Vitest

**Checkpoint**: Dashboard chạy độc lập.

---

## Phase 4: User Story 2 – Mọi số liệu tuân theo phạm vi và giới hạn trường (Priority: P1)

**Goal**: Phạm vi theo thời gian (trưởng tầng giao tạm, đã thôi), không lộ ngoài phạm vi, bộ lọc chỉ giá trị trong phạm vi, giới hạn trường (FR-010 → FR-017, FR-014, FR-042a, SC-004, SC-011).

**Independent Test**: Bộ 11 tài khoản kiểm thử quyền (QLV; trưởng tầng tầng 1; trưởng tầng giao tạm tầng 3 có thời hạn; trưởng tầng đã thôi…) → mỗi người chỉ thấy đúng phạm vi.

### Tests cho User Story 2

- [ ] T011 [P] [US2] Test `BT/m14/FR_012_PhamViTheoThoiGianIT.java` (SC-011), bộ 11 tài khoản `BT/m14/SC_004_PhamViIT.java`, `FR_014_GioiHanTruongIT.java`, `FR_042a_SuCoHanhChinhIT.java`, `SC_005_TenThuocIT.java` (Q3 → Q11)

### Implementation cho User Story 2

- [ ] T012 [US2] Nối `PhamViBaoCao`, `QuyenNhomChiTieu`, giới hạn trường vào mọi đường đọc của `DashboardService` và khung báo cáo; bộ lọc trả chỉ giá trị trong phạm vi

**Checkpoint**: P1 hoàn tất.

---

## Phase 5: User Story 3 – Báo cáo người cao tuổi (Priority: P2)

**Goal**: `NCT.TAI_NGAY`, `NCT.BIEN_DONG`, `NCT.DANH_SACH_CHO`, `GIUONG.*` theo khoảng (FR-022 → FR-025, FR-033).

**Independent Test**: 120 hồ sơ với trạng thái, loại lưu trú, mức khác nhau; 5 người đổi mức, 3 người chuyển tầng trong tháng 10 → báo cáo đúng.

### Tests cho User Story 3

- [ ] T013 [P] [US3] Test `ChiTieu_NCT_*Test`, `ChiTieu_GIUONG_*Test` trong `BT/m14/chitieu/`

### Implementation cho User Story 3

- [ ] T014 [US3] Cài chỉ tiêu nhóm người cao tuổi, giường và `BE/m14/baocao/internal/BaoCaoService.java` + `BE/m14/baocao/web/BaoCaoController.java` (`GET /bao-cao/{nhom}`) (research O-07); màn web `web/src/tinh-nang/bao-cao/nguoi-cao-tuoi/`

---

## Phase 6: User Story 4 – Báo cáo chăm sóc và chất lượng (Priority: P2)

**Goal**: `CV.*`, `BUA.*`, `HD.*`, `KT.*` (FR-030 → FR-037).

**Independent Test**: Tháng 10 tầng 1: 1.200 công việc (60 Bắt buộc: 54 Hoàn thành, 4 trễ, 2 Không thực hiện), 15 ghi nhận muộn, 40 mục kiểm tra → số đúng.

### Tests cho User Story 4

- [ ] T015 [P] [US4] Test `ChiTieu_CV_*Test`, `ChiTieu_BUA_*Test`, `ChiTieu_HD_*Test`, `ChiTieu_KT_*Test` (Q17)

### Implementation cho User Story 4

- [ ] T016 [US4] Cài chỉ tiêu nhóm chăm sóc, bữa ăn, hoạt động, kiểm tra chất lượng (005, 011, 014); màn web `web/src/tinh-nang/bao-cao/cham-soc/`, `web/src/tinh-nang/bao-cao/chat-luong/`

---

## Phase 7: User Story 5 – Báo cáo sức khỏe và thuốc (Priority: P2)

**Goal**: `SK.*`, `THUOC.*` (FR-040 → FR-044).

**Independent Test**: Tháng 10 với 50 cảnh báo, 8 lần leo thang, 900 liều đến hạn (20 Bỏ lỡ, 15 Từ chối), 3 phiếu đối chiếu quá hạn → số đúng.

### Tests cho User Story 5

- [ ] T017 [P] [US5] Test `ChiTieu_SK_*Test`, `ChiTieu_THUOC_*Test` (Q20, Q21)

### Implementation cho User Story 5

- [ ] T018 [US5] Cài chỉ tiêu nhóm sức khỏe, thuốc (001, 006, 007); màn web `web/src/tinh-nang/bao-cao/suc-khoe-thuoc/`

---

## Phase 8: User Story 6 – Báo cáo chi phí phát sinh (Priority: P3)

**Goal**: Chi phí theo kỳ đã chốt và chưa chốt, nhập tay, điều chỉnh (FR-050 → FR-054, SC-007).

**Independent Test**: Chốt bảng tháng 10 của 3 người, 1 chưa chốt; 2 khoản nhập tay, 1 điều chỉnh sau chốt của tháng 9 nằm trong bảng tháng 10 → khớp bảng đã chốt.

### Tests cho User Story 6

- [ ] T019 [P] [US6] Test `BT/m14/SC_007_KhopBangChotIT.java` (Q18, Q19)

### Implementation cho User Story 6

- [ ] T020 [US6] Cài chỉ tiêu nhóm chi phí qua `ChiPhiThongKeApi` (010); màn web `web/src/tinh-nang/bao-cao/chi-phi/`

---

## Phase 9: User Story 7 – Số liệu nhất quán và truy về bản ghi nguồn (Priority: P3)

**Goal**: Mọi con số mở được danh sách chi tiết có số dòng bằng giá trị; dashboard và báo cáo cùng ngày khớp (FR-009, SC-006).

**Independent Test**: 20 chỉ tiêu ngẫu nhiên: số dashboard = số báo cáo cùng ngày = số dòng chi tiết.

### Tests cho User Story 7

- [ ] T021 [P] [US7] Test `BT/m14/FR_009_ChiTietKhopSoIT.java` (SC-006; Q12, Q13)

### Implementation cho User Story 7

- [ ] T022 [US7] Cài `BE/m14/chitiet/internal/ChiTietService.java` và `BE/m14/chitiet/web/ChiTietController.java` (`GET /chi-tiet/{maChiTieu}`; phân trang; giới hạn trường); màn web `web/src/tinh-nang/bao-cao/chi-tiet/`

---

## Phase 10: User Story 8 – Báo cáo nhân sự và ca trực (Priority: P3)

**Goal**: Ca, thiếu phủ, đổi ca, nghỉ, tỷ lệ phục vụ, chứng chỉ (FR-070 → FR-073).

**Independent Test**: Tháng 11 tầng 2 với 60 ca, 4 ca thiếu phủ (20 giờ), 1 mục thiếu ca đã công bố, 6 yêu cầu đổi ca → số đúng.

### Tests cho User Story 8

- [ ] T023 [P] [US8] Test `ChiTieu_NS_*Test` trong `BT/m14/chitieu/` (Q22, Q23)

### Implementation cho User Story 8

- [ ] T024 [US8] Cài chỉ tiêu nhân sự, ca qua `NhanSuThongKeApi` (008), `XoayCaThongKeApi` (015); màn web `web/src/tinh-nang/bao-cao/nhan-su-ca/`

---

## Phase 11: User Story 9 – Xuất báo cáo ra file (Priority: P3)

**Goal**: Xuất XLSX theo báo cáo, khoảng, bộ lọc, chế độ, có/không định danh, mục đích; ghi nhật ký "Xuất báo cáo" trong transaction trước khi trả liên kết; tệp lưu ở `common.tep` loại `M14.BAO_CAO_XUAT` (FR-060 → FR-062).

**Independent Test**: Xuất báo cáo chi phí tháng 10 bằng Quản lý viện và Hành chính; xuất báo cáo sức khỏe bằng trưởng tầng → file khớp màn hình, có bản ghi nhật ký.

### Tests cho User Story 9

- [ ] T025 [P] [US9] Test đơn vị `BT/m14/FR_061_NoiDungXuatTest.java` và `BT/m14/FR_062_NhatKyXuatIT.java` (Q25 → Q28)

### Implementation cho User Story 9

- [ ] T026 [US9] Cài `BE/m14/xuat/internal/XuatBaoCaoService.java` (`YeuCauXuat`; POI; `NhatKyApi.ghi` `XUAT_BAO_CAO` với bộ lọc ở `chi_tiet`; `TepApi`; `QuyenTepProvider` `M14.BAO_CAO_XUAT`: người xuất, Quản lý viện) và endpoint `POST /bao-cao/{nhom}/xuat`, `GET /bao-cao/lan-xuat` (research O-08)

**Checkpoint**: Mọi user story của 016 chạy độc lập.

---

## Phase 12: Polish & Cross-Cutting

- [ ] T027 [P] Test `BT/m14/SC_008_ChiDocIT.java` (đếm bản ghi trước/sau mọi báo cáo; Q29) và hiệu năng báo cáo 12 tháng (SC-003; Q24)
- [ ] T028 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/016-reports-dashboard/contracts/api-016.yaml`
- [ ] T029 Chạy `./mvnw verify`, `npm test` và kịch bản quickstart.md mục 3; đo SC theo quickstart mục 5
- [ ] T030 Rà: mọi mã ở `contracts/chi-tieu.md` có `ChiTieu_*Test` (SC-001); DBR-25 (mốc theo Asia/Ho_Chi_Minh)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story
- **US1 → US2 (P1)**: US2 nối phạm vi vào mọi đường đọc
- **US3, US4, US5 (P2)**: sau US2, song song
- **US6, US7, US8, US9 (P3)**: sau US2; US7 cần chỉ tiêu của US1, US3 → US6, US8; US9 sau US3

### Parallel Opportunities

- T005, T006 song song; US3, US4, US5 song song; US6, US8 song song

## Parallel Example: P2

```text
Task: "T013 ChiTieu_NCT_* + ChiTieu_GIUONG_*"
Task: "T015 ChiTieu_CV_* + BUA + HD + KT"
Task: "T017 ChiTieu_SK_* + THUOC"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2
2. **Dừng và kiểm chứng**: dashboard đúng phạm vi với bộ 11 tài khoản
3. Thêm báo cáo theo nhóm

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7 → US8 → US9; mỗi bước chạy `./mvnw verify`.

## Notes

- 016 chỉ đọc; không tạo, sửa dữ liệu nghiệp vụ, không gửi thông báo (FR-001, Q-306)
- Mọi mốc đọc `ThamSoApi` (CFG-M14-*)
