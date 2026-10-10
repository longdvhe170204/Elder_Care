---
description: "Danh sách task triển khai feature 018 – Khai báo tạm trú, lưu trú"
---

# Tasks: Khai báo tạm trú, lưu trú (018)

**Input**: Tài liệu thiết kế ở `specs/018-residence-registration/` (plan.md, spec.md, research.md Z-01 → Z-07, data-model.md, contracts/api-018.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001 (`NguoiCaoTuoiApi`, `ThamGiaChuyenTrangThai`), 004 (dịch vụ hợp đồng nội bộ M02, sự kiện `TiepNhanHoanTat`, `HopDongDaDoiTrangThai`, `PhuLucDaApDung`) đã xong.

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4.

**Giai đoạn**: Dùng ở giai đoạn triển khai; CLAUDE.md hiện chặn `/speckit-implement`.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/vn/eldercare/`, `BT/` = `backend/src/test/java/vn/eldercare/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Gói `BE/m02/khaibao/{internal,web,lich}/`; bổ sung `BE/m02/api/KhaiBaoThongKeApi.java`
- Web `web/src/tinh-nang/khai-bao-tam-tru/`

---

## Phase 1: Setup

- [ ] T001 Tạo gói `BE/m02/khaibao/{internal,web,lich}/`; ArchUnit `BT/m02/KhaiBaoArchTest.java`: gói `..lich..` không tạo khai báo (Q-306) (research Z-01)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Bảng khai báo, nghĩa vụ (đệm số liệu tính), hàm thuần, khung job.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T002 Tạo `MIG/V018_01__khai_bao.sql`: `khai_bao_nct` (`nct_id` khóa chính; `thuong_tru_cung_xa`; `phien_ban`), `khai_bao_tam_tru` (`loai` `DANG_KY_TAM_TRU|THONG_BAO_LUU_TRU`; `la_gia_han`; `trang_thai` `CAN_KHAI_BAO|DA_NOP|DA_XAC_NHAN|HET_HAN|DA_XOA|DUOC_THAY_THE|DA_HUY`; `ngay_het_han` bắt buộc khi Đăng ký tạm trú được chấp nhận; cột sinh `khoa_dang_xu_ly`, `khoa_xac_nhan` `UNIQUE`), `khai_bao_lan_nop` (nhóm 3; `kenh` `CONG_DICH_VU_CONG|TRUC_TIEP`; `ket_qua` `CHO|CHAP_NHAN|TU_CHOI` đặt một lần), `nghia_vu_khai_bao` (`moc_loai` `HOAN_TAT_TIEP_NHAN|NOI_TRU_HIEU_LUC|PHU_LUC_GIA_HAN|HET_HAN`), `nhac_khai_bao` (`UNIQUE (loai, nct_id, ngay)`) (data-model mục 2 → 4)
- [ ] T003 [P] Cài hàm thuần `BE/m02/khaibao/internal/TongThoiGianLuuTru.java`, `LoaiKhaiBao.java` (thời gian lưu trú cộng dồn; CFG-M02-11) và test `BT/m02/TongThoiGianLuuTruTest.java`, `LoaiKhaiBaoTest.java` (research Z-03)
- [ ] T004 Cài khung job `BE/m02/khaibao/lich/ViecKhaiBao.java` (ShedLock, chạy bù, không nhắc trùng) và test `BT/m02/ChayBuM02KhaiBaoIT.java` (research Z-06)
- [ ] T005 [P] Cài provider: `PhanLoaiTruongProvider`, `LuuTruProvider` (CFG-M15-03), `QuyenTepProvider` `M02.BANG_CHUNG_KHAI_BAO` (Hành chính, Quản lý viện), `GanPhamViProvider`

**Checkpoint**: Nền 018 sẵn sàng.

---

## Phase 3: User Story 1 – Hành chính được nhắc và lập khai báo khi người nội trú vào ở (Priority: P1) 🎯 MVP

**Goal**: Nghĩa vụ từ Hoàn tất tiếp nhận (nội trú) và hợp đồng nội trú mới của người bán trú; loại khai báo theo thời gian cộng dồn; nhắc lập, nhắc quá hạn; Hành chính lập; xác nhận thường trú cùng xã/phường; DBR-33 (FR-001 → FR-002, BR-M02-11).

**Independent Test**: Hoàn tất tiếp nhận nội trú dài hạn, nội trú ngắn ngày 14 ngày, bán trú → lời nhắc, lập khai báo cho người nội trú, đúng loại; bán trú không có nghĩa vụ.

### Tests cho User Story 1

- [ ] T006 [P] [US1] Test `BT/m02/BR_M02_11_KhaiBaoIT.java`, `DBR_33_MotKhaiBaoIT.java` (một khai báo đang xử lý và một Đã xác nhận mỗi người; không tự tạo khai báo) (Q1 → Q6)

### Implementation cho User Story 1

- [ ] T007 [US1] Cài listener `TiepNhanHoanTat`, `HopDongDaDoiTrangThai` ghi `nghia_vu_khai_bao` trong `BE/m02/khaibao/internal/NghiaVuService.java`; nhắc lập, quá hạn theo CFG-M02-12 trong job (research Z-03)
- [ ] T008 [US1] Cài `BE/m02/khaibao/internal/KhaiBaoService.java` (lập, xác nhận thường trú cùng xã/phường; khóa `khai_bao_nct`) và `BE/m02/khaibao/web/KhaiBaoController.java` (`GET /khai-bao/can-lam`, `POST/GET /nguoi-cao-tuoi/{nctId}/khai-bao`, `POST /khai-bao/{id}/{lenh}`); màn web `web/src/tinh-nang/khai-bao-tam-tru/`

**Checkpoint**: Lập khai báo chạy độc lập.

---

## Phase 4: User Story 2 – Ghi nộp và kết quả khai báo (Priority: P1)

**Goal**: Ghi đã nộp (kênh, cơ quan, mã hồ sơ); ghi chấp nhận (ngày hết hạn, bằng chứng) hoặc từ chối (hạn mới = thời điểm ghi + CFG-M02-12); khai báo mới Đã xác nhận thay khai báo cũ; đính chính (FR-003 → FR-005).

**Independent Test**: Ghi nộp rồi xác nhận một khai báo; ghi nộp rồi từ chối khai báo khác; thử ghi kết quả khi chưa ghi nộp (bị chặn).

### Tests cho User Story 2

- [ ] T009 [P] [US2] Test `BT/m02/KhaiBaoNopKetQuaIT.java` (Q7 → Q10)

### Implementation cho User Story 2

- [ ] T010 [US2] Bổ sung lệnh ghi nộp, ghi kết quả, thay thế (`DUOC_THAY_THE` trong cùng transaction) vào `KhaiBaoService.java`; `DinhChinhDuocProvider` `M02.KHAI_BAO` (không gắn tầng) (research Z-04)

**Checkpoint**: P1 hoàn tất.

---

## Phase 5: User Story 3 – Gia hạn và chuyển loại khi gia hạn hợp đồng (Priority: P2)

**Goal**: Nhắc gia hạn trước ngày hết hạn (CFG-M02-13); Hết hạn theo giờ; phụ lục gia hạn hợp đồng ngắn ngày làm đổi loại (Thông báo lưu trú → Đăng ký tạm trú) (FR-006, FR-007, BR-M02-12, BR-M02-14).

**Independent Test**: Khai báo hết hạn 30/09/2027; gia hạn một người, để một người quá hạn; gia hạn hợp đồng ngắn ngày 14 → 60 ngày.

### Tests cho User Story 3

- [ ] T011 [P] [US3] Test `BT/m02/BR_M02_12_GiaHanIT.java`, `BR_M02_14_ChuyenLoaiIT.java` (Q11 → Q17)

### Implementation cho User Story 3

- [ ] T012 [US3] Cài listener `PhuLucDaApDung` (mốc `PHU_LUC_GIA_HAN`, `NOI_TRU_HIEU_LUC`), nhắc gia hạn, chuyển `DA_XAC_NHAN → HET_HAN` trong job; lệnh lập khai báo gia hạn trong `KhaiBaoService.java` (research Z-03)

---

## Phase 6: User Story 4 – Khai báo xóa khi kết thúc lưu trú hoặc qua đời (Priority: P2)

**Goal**: Bean trạng thái không chặn (SC-004); khai báo đang xử lý → `DA_HUY` kèm lệnh 001; nhắc khai báo xóa (CFG-M02-09); Hành chính ghi đã khai báo xóa có bằng chứng (FR-008, BR-M02-13).

**Independent Test**: Kết thúc lưu trú A có khai báo Đã xác nhận, B có khai báo Đã nộp → B Đã hủy, A được nhắc xóa; ghi đã khai báo xóa cho A.

### Tests cho User Story 4

- [ ] T013 [P] [US4] Test `BT/m02/BR_M02_13_KhaiBaoXoaIT.java` (Q18, Q19)

### Implementation cho User Story 4

- [ ] T014 [US4] Cài bean `ThamGiaChuyenTrangThai` trong `BE/m02/khaibao/internal/ThamGiaKhaiBao.java` (không điều kiện; tác động `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN`), nhắc khai báo xóa và lệnh ghi đã khai báo xóa (research Z-05)

**Checkpoint**: Mọi user story của 018 chạy độc lập.

---

## Phase 7: Polish & Cross-Cutting

- [ ] T015 [P] Cài `KhaiBaoThongKeApi.tongHop()` (016); mở rộng `BT/common/HopDongApiTest.java` với `specs/018-residence-registration/contracts/api-018.yaml`
- [ ] T016 Chạy `./mvnw verify`, `npm test` và kịch bản quickstart.md mục 3
- [ ] T017 Rà truy vết: DBR-25, 33 và BR-M02-11 → 14 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-009, FR-010, FR-011 — mỗi FR có task hoặc test tương ứng (constitution XV)

---

## Dependencies & Execution Order

- **Setup → Foundational** → **US1 → US2 (P1)** → **US3, US4 (P2)** song song

## Parallel Example: P2

```text
Task: "T011 BR_M02_12 + BR_M02_14"
Task: "T013 BR_M02_13_KhaiBaoXoaIT"
```

## Implementation Strategy

- MVP: Phase 1–2 → US1 → US2; kiểm chứng nhắc, lập, nộp, kết quả cho người nội trú
- Sau đó US3, US4; mỗi bước chạy `./mvnw verify`

## Notes

- Hệ thống không tạo khai báo, khai báo gia hạn, việc khai báo xóa; không nộp thay, không kết nối cơ quan công an (Q-306)
- Mọi mốc đọc `ThamSoApi` (CFG-M02-09, 11 → 13)
