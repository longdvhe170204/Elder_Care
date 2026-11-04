---
description: "Danh sách task triển khai feature 003 – Phòng, giường và phân bổ giường"
---

# Tasks: Phòng, giường và phân bổ giường (003)

**Input**: Tài liệu thiết kế ở `specs/003-room-bed-allocation/` (plan.md, spec.md, research.md G-01 → G-16, data-model.md, contracts/api-003.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001 đã có `resident.api` (SPI `ThamGiaChuyenTrangThai`, sự kiện mức chăm sóc).

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 11.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M03 (phần phòng, giường): `BE/roombedallocation/{api,cocau,giuong,phanbo,cachly,bantru,lich}/`; `roombedallocation.taisan` thuộc feature 019
- Web `frontend/src/tinh-nang/{co-cau-phong-giuong,bang-giuong,phan-bo-giuong,lich-su-vi-tri,cach-ly-phong,khu-nghi-ban-tru}/`; app nhân viên `mobile/lib/nhan-vien/{bang-giuong-tang,chuyen-giuong,cach-ly-phong}/`

---

## Phase 1: Setup

- [x] T001 Tạo khung tài liệu gói `BE/roombedallocation/api/`, `BE/roombedallocation/internal/{structure,bed,allocation,isolation,daycare}/`, `BE/roombedallocation/scheduling/` bằng `package-info.java` theo backend/README.md; cổng giữa giường 003 và tài sản 019 nằm ở `internal/bed/domain/`, không cho hai phần đọc trực tiếp dữ liệu của nhau (research G-01; test `FR_001_ModuleSetupTest`)
- [x] T002 [P] Tạo bản giả trong `BT/support/fake/roombedallocation/`, nhập bằng `FakeM03Config`: `AllocationResidencySource`, `DayCareArrivalCounter` (004), `AllocationInfectionStatus` (007), `LocationReferences` (008), `BedAssetPort` (019); tên logic và chữ ký theo contracts mục 2.1, mục 3, glossary mục 9 (research G-16; test `FR_018_FakeM03Test`)

**Ghi chú kỹ thuật Phase 1 (2026-10-22)**: đưa riêng khai báo bốn SPI, `BedAssetPort` và các kiểu chữ ký cần cho T002 từ T004 lên Phase 1 để bản giả biên dịch được. Không đổi thao tác trong hợp đồng, không thêm REST endpoint. T004 vẫn chưa xong: còn cổng lệnh tài sản giường, đối chiếu với API của T003 và hoàn thiện toàn bộ khai báo Phase 2. Xem plan mục "Ghi chú triển khai"; T001/T002 chỉ đánh `[x]` sau khi `clean verify` qua.

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `roombedallocation.api`, SPI cho module khác cài, sự kiện, bảng cơ cấu và giường mà mọi story dùng.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T003 [P] Tạo giao diện công khai `BE/roombedallocation/api/`: `PhanBoGiuongLenhApi` (mọi hàm MANDATORY), `ViTriApi`, `LichSuViTriApi`, `SucChuaBanTruApi`, `CoCauViTriApi`, `PhongChoPhepApi`, `TinhTrangGiuongApi`, `GiuongThongKeApi` (016), `CanhBaoBanGiaoApi` (008) đúng contracts/module-interfaces.md mục 1.2 → 1.9
- [x] T004 [P] Khai báo SPI ở `BE/roombedallocation/api/`: `NguonLuuTruChoPhanBo`, `DemLichDenBanTru`, `TinhTrangLayNhiemChoPhanBo`, `ThamChieuViTri`; port `BE/roombedallocation/giuong/port/GiuongTheoTaiSanPort.java` và `TaiSanGiuongPort` (contracts mục 2.1; research G-02)
- [x] T005 [P] Tạo sự kiện `BE/roombedallocation/api/sukien/` theo contracts mục 1.11 (`DaChuyenGiuong`, `GiuongDaTrong`, `PhongDaDatCachLy`, `PhongDaGoCachLy`, các sự kiện nhắc, báo); sự kiện gửi 009 cài `CoYeuCauThongBao` (research G-12)
- [x] T006 Tạo `MIG/V003_01__co_cau.sql`: `khu_vuc` (`ma VARCHAR(20) UNIQUE` kể cả mục Ngừng hiệu lực), `tang` (`UNIQUE (khu_vuc_id, ma)`, `ten_toa_nha NULL`), `loai_phong` (`so_giuong_tieu_chuan SMALLINT > 0`, `CHECK (phuc_vu_dai_han OR phuc_vu_ngan_ngay)`, `ten_hieu_luc UNIQUE`), `loai_phong_muc`, `phong` (`ma VARCHAR(20) UNIQUE`, `chinh_sach_gioi_tinh ENUM('NAM','NU','KHONG_GIOI_HAN')`, `dang_cach_ly BOOLEAN` mặc định `false`), `phong_muc_cho_phep`, `ly_do_chuyen_giuong` (`ma VARCHAR(30) UNIQUE`, `y_te_an_toan`, `co_dinh`; nạp dòng `KIEM_SOAT_LAY_NHIEM` cố định) theo data-model mục 2; mã mức `ENUM('CO_BAN','THUONG_XUYEN','DAC_BIET')` không khóa ngoại sang M01
- [x] T007 Tạo `MIG/V003_02__giuong.sql`: `giuong` theo data-model mục 3 (`UNIQUE (phong_id, ma)`; `trang_thai ENUM('TRONG','DANG_SU_DUNG','DANG_GIU_CHO','CHO_SAN_SANG','DANG_BAO_TRI','TAM_NGUNG','KHONG_SU_DUNG')`; `ly_do_giu_cho ENUM('NGUOI_VANG','HO_SO_CHO') NULL`; năm `CHECK` của mục 3; `phan_bo_vua_dong_id`; `ly_do_tam_ngung VARCHAR(2000) NULL`) và `lich_su_trang_thai_giuong` (nhóm 3 chỉ thêm; `thoi_diem_du_kien`, `thuc_hien_tre`, `da_khu_khuan`, `da_dung_bao_ho`, `bat_buoc_khu_khuan`, `tang_khu_vuc_id`; `CHECK (su_kien <> 'XAC_NHAN_SAN_SANG' OR bat_buoc_khu_khuan = false OR (da_khu_khuan AND da_dung_bao_ho))`; hai trigger)
- [x] T008 Cài `BE/roombedallocation/giuong/internal/GiuongTrangThaiService.java`: một điểm ghi trạng thái giường với bảng chuyển data-model mục 3 cài trong mã; khóa giường theo `id`; mỗi lần chuyển thêm một dòng lịch sử và nhật ký `M03_GIUONG_CHUYEN`; tính `thuc_hien_tre` theo CFG-M03-08 cho việc của Bộ lập lịch (research G-04; FR-011 → FR-016a)
- [x] T009 [P] Test kiến trúc `BT/roombedallocation/ChiMotNoiGhiTrangThaiArchTest.java`: chỉ `GiuongTrangThaiService` ghi `giuong.trang_thai`
- [x] T010 [P] Cài provider cho 000/002 trong `BE/roombedallocation/phanbo/internal/`: `PhanLoaiTruongProvider` (`lyDo` cách ly và chuyển kiểm soát lây nhiễm → `SUC_KHOE`), `GanPhamViProvider` (`M03.GIUONG`, `M03.PHONG`, `M03.PHAN_BO_GIUONG`), `LuuTruProvider` theo data-model mục 10 (research G-14, G-15)

**Checkpoint**: Bảng cơ cấu, giường và điểm ghi trạng thái sẵn sàng.

**Ghi chú kỹ thuật Phase 2 (2026-10-23)**: mã thật theo glossary mục 9: các API ở `roombedallocation.api`, sự kiện ở `api/events`, cổng nội bộ ở `internal/bed/domain`; migration `V003_01__room_structure.sql`, `V003_02__bed_state.sql`. T008 dùng `BedStateService` điều phối và `BedStateStore` giữ SQL; T009 là `SingleBedStateWriterArchTest` chặn mọi lớp khác gọi `writeState`, được viết đỏ trước T008. T003/T004 có fake tập trung `FakeM03ApiConfig`. Thêm khóa ngoại `bed.last_closed_allocation_id` tại V003_03/T023, khi `bed_allocation` đã tồn tại. Xem plan mục "Ghi chú triển khai" cho mapping, điều kiện tích hợp còn chờ và bằng chứng kiểm từng task.

---

## Phase 3: User Story 1 – Quản lý viện dựng cơ cấu khu vực, tầng, phòng, giường (Priority: P1) 🎯 MVP

**Goal**: Cơ cấu bốn cấp, mã duy nhất, chính sách phòng, ngừng hiệu lực có chặn, tạo giường ghi tăng tài sản (FR-001 → FR-007, BR-M03-22, DBR-32 phần 003).

**Independent Test**: 1 khu vực, 2 tầng, mỗi tầng 2 phòng khác chính sách, mỗi phòng 2–4 giường; thử mã trùng, xóa phòng đã từng có phân bổ, sửa bằng Trưởng tầng.

### Tests cho User Story 1

- [x] T011 [P] [US1] Test `BT/roombedallocation/CoCauIT.java`, `MaDuyNhatIT.java`, `NgungHieuLucCoCauIT.java`: mã trùng → 409; Trưởng tầng sửa → 403; ngừng hiệu lực phòng còn giường không `KHONG_SU_DUNG` (kể cả `TAM_NGUNG`) → 409 liệt kê; `ThamChieuViTri` còn phân công → 409 (US1 #1–8; FR-003, FR-005)
- [x] T012 [P] [US1] Test `BT/roombedallocation/FR_006_DoiChinhSachPhongIT.java`, `FR_007_KhongDoiChaIT.java`: đổi giới tính/mức của phòng vi phạm phân bổ hiện tại → 409 liệt kê người; đổi tầng của phòng hoặc phòng của giường đã có phân bổ → 409
- [x] T013 [P] [US1] Test `BT/roombedallocation/BR_M03_22_SoGiuongTieuChuanIT.java`, `Q_238_TaoGiuongGhiTangIT.java`: vượt số giường tiêu chuẩn → 201 kèm cảnh báo `VUOT_SO_GIUONG_TIEU_CHUAN`; tạo giường gọi `TaiSanGiuongPort.ghiTangChoGiuong` cùng transaction, lỗi thì rollback

### Implementation cho User Story 1

- [x] T014 [P] [US1] Tạo entity `BE/roombedallocation/cocau/internal/{KhuVuc,Tang,LoaiPhong,Phong,LyDoChuyenGiuong}.java` (`@NhomDuLieu(DANH_MUC)`) và `BE/roombedallocation/giuong/internal/Giuong.java`
- [x] T015 [US1] Cài `BE/roombedallocation/cocau/internal/CoCauService.java`: tạo, sửa, ngừng hiệu lực, kích hoạt lại, xóa (chỉ khi chưa từng tham chiếu, kể cả `ThamChieuViTri.daTungThamChieu`) cho khu vực, tầng, loại phòng, phòng, lý do chuyển; sửa chính sách phòng khóa giường và kiểm (a), (c); nhật ký `M03_CO_CAU_*` (FR-001 → FR-007)
- [x] T016 [US1] Cài tạo giường (`TAO_GIUONG` → `TRONG`, gọi ghi tăng tài sản 019, cảnh báo BR-M03-22) trong `BE/roombedallocation/giuong/internal/GiuongService.java`; giường không có lệnh xóa (Q-237)
- [ ] T017 [US1] Tạo controller `BE/roombedallocation/cocau/web/CoCauController.java` và `BE/roombedallocation/giuong/web/GiuongController.java` cho `/khu-vuc`, `/tang`, `/loai-phong`, `/phong`, `/ly-do-chuyen-giuong` (kèm `/{id}`, `/ngung-hieu-luc`, `/kich-hoat-lai`), `POST/GET /giuong`, `GET /giuong/{id}` theo contracts/api-003.yaml; cài `CoCauViTriApi`, `PhongChoPhepApi`
- [x] T018 [P] [US1] Tạo màn web `frontend/src/tinh-nang/co-cau-phong-giuong/` (cây khu vực → tầng → phòng → giường, loại phòng, lý do chuyển)

**Checkpoint**: Cơ cấu dựng được độc lập.

**Ghi chú kỹ thuật Phase 3 (2026-10-23, chưa hoàn tất)**: T014 dùng entity tiếng Anh ở `internal/structure/domain`, `internal/bed/domain`; chuẩn bị mapping trước service. Test T011 là `FR_001_StructureIT`, `FR_003_UniqueCodesIT`, `FR_005_DeactivateStructureIT`; T013 gộp hai kiểm tra tạo/cảnh báo và rollback tài sản trong `BR_M03_22_BedCreationIT`; T015 có test bổ sung `FR_003_StructureVersionIT`. `StructureService`/`StructureStore`, `BedService`/`BedStore` theo tầng README. Sau quyết định của chủ dự án, đã dịch 39 operation HTTP Phase 3, cài controller/API đọc và màn frontend/src/features/beds. T012 và phần FR-006/T015 chờ API/fake M01 cùng dữ liệu phân bổ T023; chủ dự án chọn chờ B, không viết hộ. Đổi phòng giường tạm hoãn vì chưa duyệt mở rộng BedAssetPort; chỉ sửa mã giường, T017 giữ chưa đánh vì phần sửa phòng chưa đủ. Đã kiểm nền clean verify: 167 unit/kiến trúc + 450 IT, 0 failure/error/skip; web 105 test, lint/build qua. Chỉ đánh T011/T013/T014/T016/T018 sau các kết quả này; chưa nghiệm thu toàn Phase 3. Task chưa xong đầu tiên: T012. Xem plan và Working-history.md cho giới hạn và bằng chứng.

---

## Phase 4: User Story 2 – Hành chính phân bổ giường; không bao giờ để một giường có hai người (Priority: P1)

**Goal**: `KiemTraPhanBo` dùng chung cho danh sách và lệnh; phân bổ ban đầu `TUONG_LAI`; phân bổ cho người chưa có giường; khóa tuần tự và ràng buộc CSDL chống trùng (FR-017 → FR-023, FR-025a, DBR-09, DBR-10, BR-M03-01, 02, 19, 20).

**Independent Test**: Phòng nữ chỉ cơ bản, phòng cách ly, giường bảo trì; thử phân bổ từng người vi phạm từng điều kiện; hai tài khoản phân bổ cùng giường gần đồng thời.

### Tests cho User Story 2

- [ ] T019 [P] [US2] Test `BT/roombedallocation/BR_M03_01_DieuKienPhanBoIT.java`: mức (hiện hành → hợp đồng → lượt đăng ký, Q-301), loại hình, giới tính, cách ly/khoanh vùng, trạng thái giường, giữ chỗ, dấu tài sản; trả đủ điều kiện chưa thỏa (DBR-10, BR-M03-01)
- [ ] T020 [P] [US2] Test `BT/roombedallocation/BR_M03_02_HaiNguoiCungGiuongIT.java`, `DBR_09_KhongChongIT.java`, `FR_020_PhanBoDongThoiIT.java`: 10 luồng trên một giường chỉ một thành công (SC-001); `INSERT` trực tiếp chồng khoảng bị trigger chặn; cột sinh `UNIQUE` chặn hai đang hiệu lực
- [ ] T021 [P] [US2] Test `BT/roombedallocation/FR_023_PhanBoBanDauIT.java`, `BR_M03_20_PhamViPhanBoBanDauIT.java`, `FR_022_DanhSachGiuongIT.java`, `BR_M03_19_TroVeChuaCoGiuongIT.java`: người Đang tiếp nhận chỉ có `TUONG_LAI`; phạm vi theo giường; danh sách giường phù hợp không chọn sẵn và khớp lệnh; người trở về không có giường mang dấu "chưa có giường" và được nhắc (US2 #1–15)
- [x] T022 [P] [US2] Test `BT/roombedallocation/PhanBoRangBuocDbIT.java`: mọi cột sinh, trigger, `CHECK` của `phan_bo_giuong` (tích hợp MySQL)

### Implementation cho User Story 2

- [x] T023 [US2] Tạo `MIG/V003_03__phan_bo_giuong.sql`: `phan_bo_giuong` theo data-model mục 4 (`trang_thai ENUM('TUONG_LAI','DANG_HIEU_LUC','DA_DONG','DA_HUY')`; cột sinh `ket_thuc_hieu_dung = COALESCE(ket_thuc, ket_thuc_du_kien, '9999-12-31')`; cột sinh `giuong_dang_hieu_luc`, `nct_dang_hieu_luc`, `nct_tuong_lai` có `UNIQUE`; năm `CHECK` của mục 4; chỉ mục `(giuong_id, bat_dau)`, `(nct_id, bat_dau)`, `(trang_thai, bat_dau)`; trigger `trg_phan_bo_giuong_khong_chong`, `_khong_sua` khi `DA_DONG`/`DA_HUY`, `_khong_xoa`), `vi_tri_nct` (`nct_id` khóa chính, `chua_co_giuong_tu`, `phien_ban`), view `v_phan_bo_hien_hanh` (áp `v_dinh_chinh_moi_nhat`, loại `v_ban_ghi_da_huy`), `nhac_phan_bo` (khóa chính `(loai_doi_tuong, doi_tuong_id, loai_dau)`)
- [x] T024 [P] [US2] Tạo entity `BE/roombedallocation/phanbo/internal/PhanBoGiuong.java`, `ViTriNct.java`, `NhacPhanBo.java`
- [ ] T025 [US2] Cài `BE/roombedallocation/phanbo/internal/KiemTraPhanBo.java` (FR-018 (a)–(g), dùng `NguonLuuTruChoPhanBo`, `TinhTrangLayNhiemChoPhanBo`, dấu tài sản) và `KhoaTuanTu.java` (khóa người cao tuổi khi qua 001 → dòng `trang_thai_nct` của M03 (`resident_module_state`, thay `vi_tri_nct` theo R-15) → giường theo `id`; `@Retryable` khi deadlock) (research G-03, G-05) *(câu chữ sửa 2026-10-30 theo R-15; đã cài theo bảng cũ ở PR #33, chuyển bảng ở T066)*
- [ ] T026 [US2] Cài `BE/roombedallocation/phanbo/internal/PhanBoService.java`: phân bổ cho người Đang tiếp nhận (`TUONG_LAI`, giường `CHO_SAN_SANG` theo FR-011a, gọi `daPhanBo` kết thúc giữ chỗ hồ sơ chờ), phân bổ cho người chưa có giường (`DANG_HIEU_LUC`, gỡ dấu), hủy phân bổ tương lai có lý do; nhật ký `M03_PB_*`; cài `PhanBoGiuongApi` (002), `ViTriApi` (FR-017 → FR-023, FR-025a)
- [ ] T027 [US2] Tạo `BE/roombedallocation/phanbo/web/PhanBoController.java`: `GET /phan-bo-giuong/giuong-phu-hop`, `POST/GET /phan-bo-giuong`, `GET /phan-bo-giuong/{id}`, `POST …/{id}/huy`, `GET /nguoi-cao-tuoi/{nctId}/vi-tri`, `GET /giuong/tong-hop`
- [ ] T028 [P] [US2] Tạo màn web `frontend/src/tinh-nang/phan-bo-giuong/ChonGiuong.jsx` (không chọn sẵn), `frontend/src/tinh-nang/bang-giuong/BangGiuong.jsx` và test `ChonGiuong.test.jsx`, `BangGiuong.test.jsx`, `DieuKienChuaThoa.test.jsx`

**Checkpoint**: Phân bổ an toàn đồng thời.

**Ghi chú khởi động Phase 4 (2026-10-23)**: Đã viết trước `DBR_09_AllocationSchemaIT` (phần T020/T022), chưa có migration/service mới. Lệnh test đã biên dịch trên JDK 25 nhưng 5 ca lỗi khởi tạo do thiếu `TEST_DB_URL`; chưa quan sát được assertion đỏ do thiếu chức năng, chưa đủ điều kiện viết triển khai theo quy trình test trước. Giữ toàn bộ T019–T028 chưa đánh. T019/T025/T026 còn cần API/fake M01 (trạng thái, giới tính, mức chăm sóc) do B sở hữu; không viết bản giả thứ hai. Khóa dòng M01 chỉ do lệnh trạng thái của 001 giữ trước khi gọi 003 (G-03), không thêm cổng khóa M01 mới. Chủ dự án đã duyệt dịch riêng HTTP Phase 4, chưa sửa hợp đồng/client khi backend chưa triển khai. Xem plan và Working-history.md cho kết quả kiểm cuối lượt.

**Tiếp tục Phase 4 (2026-10-23)**: Dịch 6 operation đã tồn tại của T027 sang tiếng Anh theo duyệt của chủ dự án, giữ 64 operation khác và các component cũ. Thêm schema clone, glossary mục 9, sinh lại client; cập nhật tên 6 endpoint pending trong ApiContractIT, không gỡ pending. Test DB chuẩn bị tăng lên 8 ca, thêm khóa giường ACTIVE ngay cả khi không chồng, thứ tự effective_end, view đính chính/hủy ghi nhận. Chưa có migration/service/controller Phase 4; toàn bộ task vẫn chưa đánh. T027 còn lệch: tasks yêu cầu GET danh sách nhưng contract chưa khai báo; đã hỏi bổ sung GET /bed-allocations và đang chờ duyệt, chưa tự thêm.

---

## Phase 5: User Story 3 – Chuyển giường bằng một lệnh đóng bản ghi cũ và mở bản ghi mới (Priority: P1)

**Goal**: Chuyển giường một transaction; chuyển theo lịch gắn yêu cầu thay đổi lưu trú; chuyển gấp khác giá; nhập bù qua phê duyệt; dấu "phòng không còn phù hợp" (FR-027 → FR-034, BR-M03-04, 07, 23).

**Independent Test**: Chuyển A từ P201-G1 sang P305-G2 lúc 14:00 → bản cũ kết thúc và bản mới bắt đầu đúng 14:00, G1 Chờ sẵn sàng, G2 Đang sử dụng; lỗi giữa chừng không để thay đổi nửa vời.

### Tests cho User Story 3

- [x] T029 [P] [US3] Test `BT/roombedallocation/BR_M03_07_ChuyenGiuongIT.java`, `FR_033_ChuyenDongThoiIT.java`, `BR_M03_04_SuKienChuyenIT.java`: một transaction; lỗi giữa chừng rollback; hai lệnh chuyển cùng người chỉ một thành công; phát `DaChuyenGiuong`; người vắng có giữ giường chuyển sang giường mới `DANG_GIU_CHO` với hạn còn lại (FR-032)
- [ ] T030 [P] [US3] Test `BT/roombedallocation/FR_029_ChuyenTheoLichIT.java` (đồng hồ giả lập): duyệt yêu cầu giữ giường bằng `TUONG_LAI`; ngày hiệu lực giường chưa Trống → `cho_giuong_san_sang`, chuyển khi xác nhận sẵn sàng; áp dụng không thành → `DA_HUY`
- [ ] T031 [P] [US3] Test `BT/roombedallocation/FR_030_ChuyenKhacGiaIT.java`, `FR_027_NhapBuIT.java`, `BR_M03_23_TuongLaiKhongPhuHopIT.java`, `FR_034_PhongKhongPhuHopIT.java`: chuyển gấp khác giá chỉ với lý do y tế/an toàn, đặt dấu chờ lập yêu cầu và nhắc CFG-M03-02; từ chối → dấu chờ về phòng cùng giá; nhập bù tạo yêu cầu `M03.NHAP_BU_PHAN_BO`; mức đổi làm phân bổ tương lai không phù hợp → báo; mức/giới tính đổi → dấu phòng không phù hợp

### Implementation cho User Story 3

- [ ] T032 [US3] Cài `BE/roombedallocation/phanbo/internal/ChuyenGiuongService.java`: khóa tuần tự, `KiemTraPhanBo` cho giường mới, đóng phân bổ cũ (`CHUYEN_GIUONG`) và mở phân bổ mới cùng thời điểm, giường cũ `CHO_SAN_SANG`, giường mới `DANG_SU_DUNG` hoặc `DANG_GIU_CHO` (FR-032); lý do chuyển bắt buộc; khác đơn giá (`khacDonGia`) chỉ khi lý do `y_te_an_toan` và đặt `dau_cho_lap_yeu_cau` (research G-07; FR-028 → FR-033)
- [ ] T033 [US3] Cài phần chuyển theo lịch trong `BE/roombedallocation/phanbo/internal/ChuyenTheoLich.java` (`kiemTraDatTruocChuyenTheoLich`, `datTruocChuyenTheoLich`, `kiemTraChuyenTheoLich`, `thucHienChuyenTheoLich`, `huyPhanBoTheoLich`, `daLapYeuCauSauChuyenGap`, `ketQuaYeuCauSauChuyenGap`) cài `PhanBoGiuongLenhApi` (FR-029, FR-030)
- [ ] T034 [US3] Tạo `MIG/V003_04__yeu_cau_nhap_bu.sql` (`yeu_cau_phe_duyet_id` khóa chính, `nct_id`, `phan_bo_hien_tai_id`, `giuong_moi_id`, `thoi_diem_chuyen`; không `UPDATE`) và cài handler `BE/roombedallocation/phanbo/internal/NhapBuYeuCauHandler.java` cho `M03.NHAP_BU_PHAN_BO` (`quyenTao` = `PHAN_BO_CHUYEN_GIUONG, TAO`; duyệt AC-01; `kiemTraDieuKienApDung` chạy lại FR-018, FR-019, FR-027 (c); `apDung` chuyển giường nhập bù với `nhap_bu`, `thoi_diem_nhap_thuc`) (FR-027)
- [ ] T035 [US3] Cài listener `MucChamSocDaDoi`, `DanhGiaDauVaoDaXacNhan`, `ThongTinCaNhanDaCapNhat`, `LoaiLuuTruDaDoi` trong `BE/roombedallocation/phanbo/internal/DanhGiaLaiPhuHop.java`: đặt/gỡ `dau_phong_khong_phu_hop`, báo phân bổ tương lai không phù hợp (BR-M03-23, FR-034)
- [ ] T036 *(đối chiếu merge 2026-10-30: web đã có; controller chuyển thường và app nhân viên chưa đủ)* [US3] Bổ sung endpoint `POST /phan-bo-giuong/{id}/chuyen-giuong` (kèm nhập bù) vào `PhanBoController.java`; màn web `frontend/src/tinh-nang/phan-bo-giuong/ChuyenGiuong.jsx`; app `mobile/lib/nhan-vien/chuyen-giuong/` và test `mobile/test/nhan-vien/chuyen_giuong_test.dart`

**Checkpoint**: Chuyển giường đủ biến thể.

**Cập nhật đợt1 ngày 2026-10-29**: T029 có BR_M03_07_BedTransferIT15ca (chuyển thường, giữ hạn vắng, sự kiện, đồng thời và rollback); bộ hẹp43IT xanh. BedTransferService/BedTransferStore cài phần chuyển ngay của T032, giữ T032[ ]: chủ dự án chọn giữ SPI hiện hành, chuyển từ phân bổ có awaiting_request/awaiting_same_price tạm trả503 trước ghi và giữ nguyên dấu/reminder vì chưa có nguồn đối chiếu giá hợp đồng. T033/T034 và controller T036 chưa cài. Khóa quyền/checker/M01 cơ bản đã có, không còn chờ statusPeriods; nguồn004/007/019 thật vẫn thuộc kiểm T063. Không coi test producer là nghiệm thu receiver005.

**Khởi động Phase 5 (2026-10-24; chỉ phần độc lập, chưa hoàn tất)**: Chủ dự án duyệt dịch riêng operation chuyển giường T036, tiếp tục chọn chờ B cho API/fake/sự kiện M01. Đã viết đỏ rồi cài phần migration của T034 bằng `V003_04__bed_backfill_request.sql`; `FR_027_BackfillSchemaIT` có 5 IT (schema/chỉ mục, PK/FK, bất biến admin/retention, rollback và dọn cùng phân bổ), sau cài cùng 23 ArchTest xanh. Handler T034 chưa cài vì còn cần checker FR-018/lệnh chuyển giường và nguồn khoảng vắng M01. T036 mới dịch `POST /bed-allocations/{id}/transfer`, clone request/response và sinh client; chưa controller/web/mobile. Giữ T029–T036 chưa đánh; không dùng dữ liệu giả thay nguồn M01. Ghi chú và kết quả clean verify nền ở plan/Working-history.md.

---

## Phase 6: User Story 4 – Trạng thái giường chỉ thay đổi qua lệnh và sự kiện; giữ chỗ có lý do và hạn (Priority: P1)

**Goal**: Đủ 17 dòng chuyển giường; giữ chỗ hồ sơ chờ và người vắng; tạm ngừng, sử dụng lại; port cho 019; tham gia lệnh trạng thái của 001 (FR-011 → FR-016a, BR-M03-05, 06, 13, 15).

**Independent Test**: Đi hết mọi chuyển trong bảng trạng thái giường, thử mọi chuyển ngoài bảng; tua đồng hồ qua hạn giữ chỗ.

### Tests cho User Story 4

- [x] T037 [P] [US4] Test đơn vị `BT/roombedallocation/BangChuyenTrangThaiGiuongTest.java` tham số hóa mọi dòng của bảng chuyển data-model mục 3 và mọi cặp ngoài bảng
- [ ] T038 [P] [US4] Test `BT/roombedallocation/BR_M03_05_GiuongCoNguoiIT.java`, `BR_M03_06_VeTrongIT.java`, `BR_M03_13_GiuongHongDatTruocIT.java`, `BR_M03_15_DauChoChuyenNguoiIT.java`, `TamNgungSuDungIT.java`, `FR_016a_ThucHienTreIT.java`: giường có người chặn lệnh tài sản; hết hạn giữ chỗ hồ sơ chờ về Trống và gọi 004; giường hỏng hủy phân bổ tương lai theo FR-013a; Báo hỏng giường có người ghi dấu "chờ chuyển người"; tạm ngừng có lý do, sử dụng lại về `CHO_SAN_SANG`; việc trễ hơn CFG-M03-08 ghi `thuc_hien_tre` (US4 #1–16)
- [ ] T039 [P] [US4] Test `BT/roombedallocation/PhanBoThamGiaChuyenTrangThaiIT.java`: mỗi lệnh của bảng G-06 (Hoàn tất tiếp nhận, Hủy tiếp nhận, Cho tạm vắng, Chuyển viện, Ghi nhận trở về, Kết thúc lưu trú, Ghi nhận qua đời) đổi giường, phân bổ đúng; lỗi thì rollback cả lệnh 001

### Implementation cho User Story 4

- [ ] T040 [US4] Cài giữ chỗ trong `BE/roombedallocation/giuong/internal/GiuChoService.java`: `giuChoHoSoCho` (hạn CFG-M02-02), `huyGiuCho` (idempotent), `giaiPhongGiuongKhiVang`, `giaHanKetThucDuKien` của `PhanBoGiuongLenhApi` (FR-012, FR-014, FR-015)
- [x] T041 [US4] Cài lệnh tạm ngừng, sử dụng lại (AC-01; Q-246, Q-261) trong `GiuongService.java` và endpoint `POST /giuong/{id}/tam-ngung-su-dung`, `/su-dung-lai`, `GET /giuong/{id}/lich-su-trang-thai` trong `GiuongController.java`
- [ ] T042 [US4] Cài `BE/roombedallocation/giuong/port/GiuongTheoTaiSanPortImpl.java`: `TAI_SAN_BAO_TRI`, `TAI_SAN_THANH_LY`, `TAI_SAN_SAN_SANG`, dấu "chờ chuyển người", ngoại lệ FR-013a, FR-013b; chặn khi giường có người (BR-M03-05, 13, 15)
- [ ] T043 [US4] Cài bean `ThamGiaChuyenTrangThai` của 003 trong `BE/roombedallocation/phanbo/internal/PhanBoThamGiaChuyenTrangThai.java` (`@Order` sau 004; `chiTiet.doiPhanBoSangGiuongId` cho Hoàn tất tiếp nhận; giữ giường khi vắng theo `giuGiuongKhiVang` (Q-395: 004 luôn trả giữ, không hạn); giải phóng khi kết thúc, qua đời; cảnh báo FR-024a khi trở về phòng chịu chặn) (research G-06)
- [ ] T044 [US4] Cài job `BE/roombedallocation/lich/HetHanGiuCho.java` và `BaoQuaBatDauDuKien.java` (ShedLock, 15 phút; chống lặp theo `da_bao_het_han_luc`, `da_bao_qua_bat_dau_cho`; gọi `hoSoChoHetHanGiuCho` cùng transaction) (research G-09)

**Kết quả lượt Phase 6 (2026-10-24; chưa hoàn tất cả phase)**: Chỉ đánh T037/T041 sau `mvnw.cmd clean verify` nền BUILD SUCCESS (02:06), 351 unit/kiến trúc +495 IT, 0 failure/error/skip, Spotless 786 file qua. T037 có 184 ca bảng chuyển; T041 có lệnh tạm ngừng/sử dụng lại và lịch sử, 3 operation được duyệt dịch và ApiContractIT đã kiểm. T040 mới có 4 tác động nội bộ BedHoldService, chưa nối facade API đầy đủ; T044 mới có HoldExpiryJob, chưa BaoQuaBatDauDuKien. T038/T039/T040/T042/T043/T044 giữ chưa đánh: thiếu hợp đồng/fake M01 và checker/lệnh theo lịch của phase trước. Không đổi UI, không chụp màn hình; frontend 113 test, lint/build qua. Xem plan và Working-history.md; task chưa hoàn tất tiếp theo của Phase 6 là T038.
**Checkpoint**: P1 hoàn tất; giường và phân bổ đầy đủ.

---

## Phase 7: User Story 5 – Tra cứu lịch sử vị trí phục vụ truy vết lây nhiễm (Priority: P2)

**Goal**: Vị trí tại thời điểm, lịch sử theo người/giường, danh sách cùng phòng, có áp đính chính (FR-026, FR-035 → FR-037, BR-M03-24).

**Independent Test**: Lịch sử 30 ngày cho 10 người trong 3 phòng; hỏi vị trí tại 5 thời điểm và danh sách cùng phòng của một người trong 5 ngày; so với dữ liệu.

### Tests cho User Story 5

- [ ] T045 [P] [US5] Test `BT/roombedallocation/LichSuViTriIT.java`, `CungPhongIT.java`, `TraCuuDinhChinhIT.java`, `BR_M03_24_DinhChinhBao007IT.java`, `QuyenTraCuuIT.java`: kết quả khớp dữ liệu (SC-003 < 1 giây); đính chính phân bổ đổi kết quả tra cứu và báo 007; người ngoài phạm vi bị chặn

### Implementation cho User Story 5

- [x] Phần sửa fixture FR-036 thuộc T045/T048 (2026-10-28): đã tái hiện FR_036_missingM01AbsenceSourceFailsClosed mong 503 nhưng nhận 404 trước sửa. Test nay dùng spy M01 với lỗi nguồn được cấu hình riêng cho ca 503; ca FR_036_roommateAbsencesUseRealM01HistoryAndClipToOverlap nạp hồ sơ/lịch sử thật, không stub statusPeriods, kiểm khoảng vắng của cả hai người bị cắt theo khoảng cùng phòng và loại khoảng ngoài giao. Hồi quy 23 IT lịch sử/HTTP/đính chính xanh, 2 unit khung xanh; clean verify cuối có 680 unit và toàn bộ 231 IT M03 xanh, Spotless sạch; toàn kho còn 20 lỗi M01/M15 đã ghi bàn giao. Chỉ phần sửa fixture này hoàn tất, không đánh toàn T045/T046/T048 vì các phạm vi khác chưa nghiệm thu.

- [ ] T046 [US5] Cài `BE/roombedallocation/phanbo/internal/LichSuViTriService.java` (`LichSuViTriApi`) trên `v_phan_bo_hien_hanh` (research G-10)
- [ ] T047 [US5] Cài `DinhChinhDuocProvider` cho `M03.PHAN_BO_GIUONG` và `M03.XAC_NHAN_SAN_SANG` (`tangKhuVuc` là tầng của giường; `sauKhiDinhChinh` báo 007) trong `BE/roombedallocation/phanbo/internal/PhanBoDinhChinhProvider.java` Bổ sung SPI đã có residentId(id): đọc người cao tuổi của bản phân bổ gốc để nhật ký CORRECTION_ADDED gắn resident_id (FR-049; sửa hẹp2026-10-30,không đổi hợp đồng000).
- [ ] T048 [US5] Tạo `BE/roombedallocation/phanbo/web/LichSuViTriController.java`: `GET /lich-su-vi-tri/nguoi-cao-tuoi/{nctId}`, `/lich-su-vi-tri/giuong`, `/lich-su-vi-tri/cung-phong`, `POST /phan-bo-giuong/{id}/dinh-chinh`, `POST /giuong/{id}/lich-su-trang-thai/{lichSuId}/dinh-chinh`; màn web `frontend/src/tinh-nang/lich-su-vi-tri/`

**Ghi chú triển khai 2026-10-24**: T045–T048 đã có bản cài và kiểm hẹp xanh; tên lớp/đường dẫn thực tế và lệch SPI ghi ở plan. `clean verify` cuối: 392 unit/kiến trúc và 170 IT M03 xanh, toàn kho còn 14 lỗi test M01. Chưa đánh `[x]`: M01 `statusPeriods` đã có bản thật; fixture lỗi nguồn được sửa và kiểm dữ liệu thật ngày2026-10-28; chủ dự án giữ nguyên schema lịch sử theo giường nên chưa thể trả dấu nhập bù FR-027 ở HTTP này; UI chưa kiểm trực quan browser đủ ba bề rộng/chế độ tối.

**Checkpoint**: Truy vết vị trí chạy độc lập.

---

## Phase 8: User Story 6 – Khu nghỉ bán trú có sức chứa theo buổi (Priority: P2)

**Goal**: Một khu nghỉ Hiệu lực, buổi không chồng khung, sức chứa CFG-M03-01, đếm lại khi đổi khung (FR-038 → FR-042, BR-M03-03, BR-M03-25).

**Independent Test**: CFG-M03-01 = 3; đăng ký 3 người sáng thứ Hai, thử người thứ 4; báo vắng một người rồi đăng ký lại; lịch lặp hằng tuần có một ngày vượt.

### Tests cho User Story 6

- [ ] T049 [P] [US6] Test `BT/roombedallocation/BR_M03_03_SucChuaIT.java`, `FR_039_BuoiGiaoIT.java`, `BR_M03_25_DoiKhungGioIT.java`, `KhuNghiDuyNhatIT.java`: vượt sức chứa bị chặn; đổi CFG-M03-01 hoặc khung giờ thì đếm lại và cảnh báo buổi vượt; hai khu nghỉ Hiệu lực bị `UNIQUE` chặn; ngừng hiệu lực khi còn lịch tương lai → 409

### Implementation cho User Story 6

- [ ] T050 [US6] Khu nghỉ, buổi bán trú (`V003_05` đã có; sau gộp R-15 là loại `M03.DAY_CARE_REST_AREA`, `M03.DAY_CARE_SESSION` của `danh_muc_chung`, data-model 0.1, chuyển ở T066): khu nghỉ có đúng một trong tầng hoặc khu vực và chỉ một khu Hiệu lực (kiểm dưới khóa `M03.KHU_NGHI`), buổi có giờ kết thúc sau giờ bắt đầu và tên Hiệu lực không trùng (`ten_hieu_luc`) *(câu chữ sửa 2026-10-30 theo R-15; đã cài theo bảng cũ ở PR #33, chuyển bảng ở T066)*
- [ ] T051 [US6] Cài `BE/roombedallocation/bantru/internal/BanTruService.java`: cấu hình khu nghỉ, buổi (không chồng khung dưới khóa khu nghỉ), `SucChuaBanTruApi` cho 004, 005, đếm lại qua `DemLichDenBanTru` khi đổi buổi hoặc `ThamSoDaDoi(CFG-M03-01)`, nhật ký `M03_BAN_TRU_CANH_BAO_VUOT` (research G-11)
- [ ] T052 [US6] Tạo `BE/roombedallocation/bantru/web/BanTruController.java` (`/khu-nghi-ban-tru`, `/{id}`, `/{id}/ngung-hieu-luc`, `/so-dang-ky`, `/buoi-ban-tru`, `/{id}`, `/{id}/ngung-hieu-luc`) và màn web `frontend/src/tinh-nang/khu-nghi-ban-tru/`

**Checkpoint**: Bán trú có sức chứa.

---

**Ghi chú Phase 8 (2026-10-24, chưa nghiệm thu)**: Bản cài dùng `internal/daycare/{domain,persistence,service}`, `web/{request,response}`, migration `V003_05__day_care_capacity.sql`, UI `features/beds/DayCarePanel`. Test schema/capacity/HTTP/CFG đã RED trước implementation; test RR và vị trí cha RED trước fix. Bổ sung guard chung cho ngừng/xóa AREA/FLOOR để khu nghỉ Hiệu lực không bị mồ côi; ánh xạ LocalTime bằng JDBC4.2, lý do và bằng chứng ở plan. Lượt hồi quy `003-p8-review-green.log`: 16 IT cùng ArchTests xanh. Clean verify nền cuối:392unit/kiến trúc và186IT M03 xanh; toàn kho1031IT còn15lỗiM01 (14cũ +1regex capacity đã bàn giaoB). Frontend243test/lint/build xanh. Chưa đánh [x]: chủ dự án đã chọn giữ nguyên contract/SPI (phương án2 cho cả hai đề nghị), nên sửa/ngừng UI tiếp tục chờ và backend chỉ chặn chung, chưa liệt kê người có lịch; còn xử lý lỗi kiểm toàn kho và kiểm trực quan UI. T052 hiện UI xem/tạo/đếm, chưa sửa/ngừng hiệu lực trên UI.
**Xác minh riêng T049–T050 (2026-10-24)**: Đã đối chiếu đủ test/migration và bổ sung ca nhiều ngày chỉ một ngày vượt, CHECK khung giờ rỗng. `003-t049-t050-verify.log`:16IT+25testkiếntrúc xanh; `003-t049-t050-extra-verify.log`:8IT+25testkiếntrúc xanh, spotless qua. Không cần thêm implementation spec khác để kiểm hai task này; không sửa production/migration/HTTP/SPI. Giữ `[ ]` theo gate clean verify cuốiPhase8, không nhầm với thiếu test hoặc thiếu migration. Task kế tiếp là T051 rồi T052.
## Phase 9: User Story 7 – Bác sĩ, Quản lý viện đặt và gỡ cách ly phòng (Priority: P2)

**Goal**: Đặt/gỡ cách ly có lý do, chặn phân bổ vào/ra; chuyển theo chỉ định kiểm soát lây nhiễm; xác nhận ở lại vùng cách ly (FR-008 → FR-010, FR-024a, BR-M03-21).

**Independent Test**: Đặt cách ly P105, thử phân bổ, chuyển vào/ra; Trưởng tầng gỡ (bị từ chối) rồi Bác sĩ gỡ.

### Tests cho User Story 7

- [ ] T053 [P] [US7] Test `BT/roombedallocation/CachLyPhongIT.java`, `BR_M03_01_CachLyIT.java`, `BR_M03_21_BacSiChiDinhIT.java`, `FR_024a_TroVePhongCachLyIT.java`: chỉ AC-01, AC-03 đặt/gỡ; phòng cách ly và vùng khoanh chặn phân bổ; chuyển kiểm soát lây nhiễm cần bác sĩ chỉ định (`TaiKhoanApi.coVaiTro`); trở về phòng chịu chặn đặt dấu chờ bác sĩ xác nhận, gỡ khi `KhoanhVungDaGo`

### Implementation cho User Story 7

- [ ] T054 [US7] Tạo `MIG/V003_06__lich_su_cach_ly_phong.sql` (`hanh_dong ENUM('DAT','GO')`, `ly_do` bắt buộc, `su_co_id NULL`; trigger chỉ thêm) và cài `BE/roombedallocation/cachly/internal/CachLyService.java` (đặt, gỡ, `M03_CACH_LY`, phát `PhongDaDatCachLy`/`PhongDaGoCachLy`, listener `KhoanhVungDaGo`, `DaThemVaoDanhSachTiepXuc`) (research G-08)
- [ ] T055 [US7] Cài chuyển kiểm soát lây nhiễm (`bac_si_chi_dinh_id`, lý do `KIEM_SOAT_LAY_NHIEM`) và xác nhận ở lại vùng cách ly (`bs_xac_nhan_*`) trong `ChuyenGiuongService.java`
- [ ] T056 *(rà 2026-10-30: backend, web xong (PR #33); còn app nhân viên)* [US7] Tạo `BE/roombedallocation/cachly/web/CachLyController.java` (`POST /phong/{id}/dat-cach-ly`, `/go-cach-ly`, `GET /phong/{id}/lich-su-cach-ly`) và endpoint `POST /phan-bo-giuong/{id}/chuyen-giuong-kiem-soat-lay-nhiem`, `/xac-nhan-o-lai-vung-cach-ly`; màn web `frontend/src/tinh-nang/cach-ly-phong/` và app `mobile/lib/nhan-vien/cach-ly-phong/`

**Checkpoint**: Cách ly chạy độc lập.

**Ghi chú triển khai Phase 9 (2026-10-25, chưa nghiệm thu)**: Đường dẫn logic được đặt thành `internal/isolation/{domain,persistence,service}`, `web/{request,response}`, `V003_06__room_isolation_history.sql`; UI trong `features/beds/{RoomIsolationPanel,IsolationWorkspace}`. T053 có các lớp `FR_008_RoomIsolationIT`, `BR_M03_21_ControlledTransferIT`, `FR_028_ControlledTransferAtomicityIT`; phần trở về phòng đã có hồi quy `FR_024_AllocationStatusParticipantIT` của Phase6. T055 dùng ControlledTransferService, chỉ miễn điều kiện cách ly; lệnh/giường/lịch sử/audit cùng rollback, giữ hạn giữ giường khi vắng. Chủ dự án duyệt5operation dịch và3GET phụ trợ quyền; client sinh lại. Scoped cuối `003-p9-review-green.log`:19IT+25testkiến trúc xanh; web250test/lint/build qua. Giữ checkbox tới khi clean verify cuối hoàn tất và đạt gate. T053/T054 còn listener sự kiện007 chờD; T056 có backend/web, mobile nhân viên chưa mở theo CLAUDE/mobile README. Không suy phần còn thiếu thành hoàn tất phase.

**Kết quả gate Phase9**: clean verify nền đã kết thúc (003-p9-clean-verify-final.log, exit1,06:25phút),392unit/kiến trúc và205IT M03 xanh; toàn kho1050IT còn6failure+9error của8lớpM01,0skip; spotless qua. Không đánh thêm[x]. Thứ tự việc còn lại trong Phase9: T053 bổ sung ca sự kiện007→T054 listener sauD công bố API/fake→T055 giữ implementation đã kiểm, chờ gate xanh→T056 đóng phần backend/web khi đạt gate, appnhân viên chờ mở phạm vi. Task đầu Phase10 là T057, chưa thực hiện trong lượt này.

---

## Phase 10: User Story 8 – Xác nhận giường sẵn sàng trước khi nhận người mới (Priority: P2)

**Goal**: Giường `CHO_SAN_SANG` chỉ về Trống bằng lệnh xác nhận; khử khuẩn và bảo hộ bắt buộc khi người rời thuộc danh sách; nhắc quá hạn CFG-M03-04 (FR-044 → FR-048, BR-M03-09, BR-M03-10, DBR-26).

**Independent Test**: Kết thúc lưu trú → giường Chờ sẵn sàng; phân bổ bắt đầu ngay bị chặn; quá CFG-M03-04 → nhắc; xác nhận; lặp lại với người thuộc danh sách tiếp xúc.

### Tests cho User Story 8

- [ ] T057 [P] [US8] Test `BT/roombedallocation/BR_M03_09_ChoSanSangIT.java`, `BR_M03_10_KhuKhuanIT.java`, `XacNhanSanSangQuyenIT.java` (đồng hồ giả lập): nhắc Trưởng tầng một lần khi quá hạn; người rời thuộc danh sách thì thiếu khử khuẩn/bảo hộ → 422 và `CHECK` chặn; chỉ Trưởng tầng, Người phụ trách ca của tầng xác nhận (research G-14); có chuyển theo lịch đang chờ thì xác nhận chuyển ngay

### Implementation cho User Story 8

- [ ] T058 [US8] Cài `BE/roombedallocation/giuong/internal/XacNhanSanSangService.java`: `bat_buoc_khu_khuan = TinhTrangLayNhiemChoPhanBo.thuocDanhSach(nguoi của phan_bo_vua_dong_id)`; `XAC_NHAN_SAN_SANG` → `TRONG` (phát `GiuongDaTrong`) hoặc thực hiện phân bổ `cho_giuong_san_sang` (FR-011a, FR-044, FR-045)
- [ ] T059 [US8] Cài job `BE/roombedallocation/lich/NhacGiuongChoSanSang.java` và `NhacChuKyPhanBo.java` (CFG-M03-04, CFG-M03-02; chống lặp `da_nhac_san_sang_luc` của giường và `moc_da_nhac` loại `M03.ALLOCATION_REMINDER.<flagType>` (thay `nhac_phan_bo` theo R-15); nhật ký `M03_NHAC`) (research G-09) *(câu chữ sửa 2026-10-30 theo R-15; đã cài theo bảng cũ ở PR #33, chuyển bảng ở T066)*
- [ ] T060 *(rà 2026-10-30: backend xong (PR #33); còn app nhân viên)* [US8] Tạo endpoint `GET /giuong/{id}/dieu-kien-xac-nhan-san-sang`, `POST /giuong/{id}/xac-nhan-san-sang` trong `GiuongController.java`; app `mobile/lib/nhan-vien/bang-giuong-tang/` và test `mobile/test/nhan-vien/xac_nhan_san_sang_test.dart` (ô khử khuẩn bắt buộc khi `canKhuKhuan`)

**Checkpoint**: Mọi user story của 003 chạy độc lập.

**Ghi chú Phase 10 (2026-10-25, đang kiểm cuối)**: T057 ánh xạ thành BR_M03_09_BedReadinessIT, FR_044_BedRemindersIT, FR_044_ReminderAtomicityIT; kiểm cả quyền thật002/SHIFT_LEAD, điều kiện007 đổi sau preview, CHECK MySQL, nhắc một lần/chu kỳ/gỡ dấu, nhóm nhận và rollback. T058 dùng internal/bed/{domain,persistence,service}/BedReadiness*, note INSERT qua BedTransition/BedStateStore, chỉ BedStateService ghi trạng thái. T059 có scheduling/{BedReadinessReminderJob,AllocationReminderJob}, processor/store đúng tầng; đăng ký2job003 theo SchedulerScopeArchTest, không đổi APIclock000. T060 có BedReadinessController, web/request/BedReadinessWrite, web/response/BedReadinessResponse,2operation đã duyệt dịch và422; client sinh lại. Scoped cuối15IT+25kiến trúc xanh; frontend250test/lint/build qua. Giữ[ ] trong khi clean verify chạy. T057–T058 còn nhánh chuyển theo lịch thật: BedAllocationCommandApi của T032–T033 chưa có provider; có pending thì503/rollback. T060 app nhân viên chưa mở theo CLAUDE.md:12, không tự làm web mới thay task app. T063 sẽ kiểm bean007/008/004 thật khi có, không coi fake là production.

**Gate Phase10 đã kết thúc**: `003-p10-clean-verify-final.log`, exit1/04:06phút;392unit/kiến trúc và218IT M03 xanh; toàn kho1063IT còn6failure+9error ở8lớpM01 đã bàn giaoB,0skip; spotless qua. Giữ T057–T060[ ]. Việc tiếp trong phạm vi phase: hoàn thiện nhánh T057→T058 khi có providerT032–T033, chốt T059 sau gate toàn kho xanh, T060 còn appchờ mở. Task đầu Phase11 là T061, chưa thực hiện lượt này.

---

## Phase 11: Polish & Cross-Cutting

- [ ] T061 [P] Cài `TinhTrangGiuongApi`, `GiuongThongKeApi` (016), `CanhBaoBanGiaoApi` (008) trong `BE/roombedallocation/giuong/internal/` Bổ sung bản cài AllocationHandoverApi đã công bố cho008; V003_07 lưu mốc đặt dấu chờ bác sĩ trên bed_allocation,giữNULL cho dữ liệu cũ,không suy từ start_at; test nguồn thật và consumer HTTP008 theo plan ngày2026-10-31.
- [ ] T062 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/003-room-bed-allocation/contracts/api-003.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 003
- [ ] T063 Khi 004, 007, 008, 019 có bản thật, chạy lại test của US2, US4, US7, US8 với bean thật thay T002
- [ ] T064 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC-005 (danh sách giường < 1 giây) theo quickstart mục 5
- [ ] T065 Rà truy vết data-model mục 11: DBR-09, 10, 23, 25, 26, 32 (phần 003) và BR-M03-01 → 07, 09, 10, 13, 15, 19 → 25 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-024, FR-025, FR-051 (bổ sung 2026-10-10) — mỗi FR có task hoặc test tương ứng (constitution XV)

**Ghi chú Phase11 (2026-10-25, chưa nghiệm thu)**: T061 có BedReportingService cài3API hiện có, BedReportingStore và domain BedReportingSnapshot; đã duyệt BedFlagView.bedId nullable riêng WITHOUT_BED. T062 giữ đăng ký HTTP đã có, mở rộng RecordGroupTriggerIT cho2bảng003 SQL-only và DBR_23_RecordTablesIT trên hàng thật. T063 chưa có nguồn thật004/007/008/019. T064 có T064_BedListPerformanceIT (100phòng/300giường,10request sauwarmup), không phải phép đo đầy đủ SC-00560người dùng/2phút. Flutter exit1 trước test vì Windows thiếu symlink; demo đủ37kịch bản chưa chạy được do thiếu công tắc fake/luồng/provider. T065 có [verification.md](verification.md) mapping đúng class/method và phần thiếu; bổ sung test FR-051 mức thông báo và producer BedTransferred. Kiểm cuối nền393unit/kiến trúc,227IT M03 xanh; toàn kho1073IT còn15lỗiM01 đã bàn giaoB (6failure+9error),0skip; web250test/lint/build qua; spotless qua. Giữ T061–T065[ ]; task đầu còn lại trong Phase11 làT061 chốt giới hạn/gate, rồiT062→T063→T064→T065 theo nội dung thiếu. Không có Phase12 trong tasks này.
- [ ] T066 *(bổ sung 2026-10-29, research R-15 bản 2 của 000; chờ A xong 000 T108)* Gộp bảng đã cài theo data-model mục 0: migration `V003_95` tạo bảng gộp hoặc cột `JSON`, chép dữ liệu (dòng chép sang bảng chung ghi `chi_tiet.legacy`), đổi khóa ngoại và `tep.chu_so_huu_id`, `DROP TABLE` bảng cũ; giữ id của bảng nhóm 3 là đích của `dinh_chinh`, `nhat_ky` (R-15 mục 5); sửa entity, lớp truy cập dữ liệu, service sang `CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`, `ReminderMarkers`, `TechnicalLocks`; job ghi lần chạy ra file log. Test: giữ kịch bản nghiệp vụ, đổi bảng đích ở 50 file test, fixture liệt kê cuối data-model 0.1 (test, fixture của module khác: báo chủ module trong PR; fixture `support/` sửa trong cùng PR); bảng truy vết sau gộp ở 0.1 có test cho từng mã, test bảo vệ nhóm dữ liệu (trigger, quyền `eldercare_app`) cho bảng gộp, test migration chép đủ số dòng trên dữ liệu demo; không đổi API (`ApiContractIT` xanh)

---

## Dependencies & Execution Order

- **Setup → Foundational**: chặn mọi story; Foundational của 003 chặn 004 (cần `PhanBoGiuongLenhApi`, `SucChuaBanTruApi`) và 019 (port)
- **US1 (P1)**: sau Foundational
- **US2 (P1)**: sau US1
- **US3 (P1)**: sau US2
- **US4 (P1)**: sau US2 (phân bổ để thử giường có người)
- **US5 (P2)**: sau US2, US3
- **US6 (P2)**: sau US1; độc lập với US2–US5
- **US7 (P2)**: sau US2, US3
- **US8 (P2)**: sau US4 (giường `CHO_SAN_SANG` từ lệnh của 001)

### Parallel Opportunities

- T003–T005, T009, T010 song song
- US6 làm song song với US2–US4 sau US1; US5 và US7 song song sau US3
- Test [P] trong mỗi story song song; web và app song song khi controller đã có

## Parallel Example: User Story 2

```text
Task: "T019 BR_M03_01_DieuKienPhanBoIT"
Task: "T020 BR_M03_02/DBR_09/FR_020 IT"
Task: "T021 FR_023/BR_M03_20/FR_022/BR_M03_19 IT"
Task: "T022 PhanBoRangBuocDbIT"
```

## Implementation Strategy

### MVP First

1. Phase 1–2 → US1 → US2 → US4 (phần tham gia 001)
2. **Dừng và kiểm chứng**: SC-001 (10 luồng một giường), Hoàn tất tiếp nhận của 001 bắt đầu phân bổ
3. Chuyển sang plan 004

### Incremental Delivery

US1 → US2 → US3 → US4 → US5 → US6 → US7 → US8; mỗi bước chạy `./mvnw verify` và kịch bản quickstart tương ứng.

## Notes

- 003 không gợi ý hay chọn sẵn giường, không tự lập yêu cầu (Q-306, Q-307)
- Mọi mốc đọc `ThamSoApi` (CFG-M02-02, CFG-M03-01, 02, 04, 07, 08)
- Lệnh của 003 chỉ chạy trực tuyến

**Chốt phần nền độc lập Phase 4 (2026-10-24; chưa xong cả phase)**: Sau khi chủ dự án cung cấp cấu hình MySQL và duyệt thêm GET /bed-allocations, đã cài T023 bằng V003_03__bed_allocation.sql, T024 bằng BedAllocation/ResidentPosition/AllocationReminder ở internal/allocation/domain. T022 là DBR_09_AllocationSchemaIT (12 IT). T020/T025 mới có AllocationLocks, 3 IT kiểm khóa gồm hồi quy deadlock đã RED rồi GREEN khi đổi INSERT IGNORE sang upsert theo G-03. T026 mới có AllocationQueries cài BedAllocationApi/ResidentLocationApi (4 IT), chưa lệnh tạo/hủy. HTTP Phase 4 có 7 operation đã duyệt, vẫn pending controller; client được sinh lại. `clean verify` nền bản cuối BUILD SUCCESS, 02:25, 167 unit/kiến trúc +470 IT, 0 failure/error/skip, Spotless qua. Web 105 test, lint/build qua. Chỉ sau kết quả này mới đánh T022/T023/T024; T019/T020/T021/T025/T026/T027/T028 giữ chưa đánh. Nguồn M01 vẫn chờ B, không tạo fake thay. Chi tiết ở plan/Working-history.md.

**Kiểm chốt phần độc lập Phase 5 (2026-10-24)**: clean verify nền bản cuối sau sửa fixture BUILD SUCCESS (04:10), 167 unit/kiến trúc +475 IT, 0 failure/error/skip; web 105 test, lint/build qua. Không đánh thêm task: T034 thiếu handler, T036 thiếu controller/UI; T029 là task Phase 5 chưa xong đầu tiên. Chủ dự án giữ quyết định chờ B cho nguồn M01. Xem plan/Working-history.md về lần kiểm đầu thất bại do fixture tăng AUTO_INCREMENT và kết quả kiểm lại sau sửa.

**Bổ sung T036 UI web (2026-10-24)**: Sau nhắc của chủ dự án, đã làm `frontend/src/features/beds/BedTransfersPanel.jsx`, `BedTransferForm.jsx` và gắn tab vào `BedsPage.jsx` theo quyền BED_ALLOCATION. Test trước RED→GREEN: 7 ca panel +6 ca page; kiểm cuối frontend 113 test, lint/build qua; kiểm trực quan ba kích thước, chế độ tối và bàn phím bằng API fixture cục bộ. T036 giữ chưa đánh vì controller chưa có; mobile chưa mở theo mobile/README.md. Các ghi chú trước “chưa UI” là trạng thái ở thời điểm ghi, đã được cập nhật bởi mục này. Chờ B vẫn chỉ chặn nguồn M01/backend; không chặn phần UI độc lập đã hoàn thành. Xem plan/Working-history.md.

**Bổ sung tiếp tục T038/T042 sau nhắc điểm dừng sớm (2026-10-24)**: Đã thêm BedAssetStateService/BedAssetStateStore và 9 IT viết RED→GREEN cho nhánh trạng thái tài sản độc lập, gồm chặn có người, bảo trì/thanh lý, khôi phục Tạm ngừng, hủy phân bổ ban đầu khi giường hỏng và rollback. `clean verify` nền bản mới BUILD SUCCESS (02:12), 351 unit/kiến trúc +504 IT, 0 failure/error/skip; Spotless 789 file qua. T038/T042 vẫn chưa [x] vì chưa đủ adapter BedAssetCommandPort/FR-013b/suitable checker và xử lý căn cứ theo lịch. Kết quả 495 IT ở ghi chú trước là thời điểm trước phần bổ sung này; không thay thế kết quả cuối. Xem plan/Working-history.md.

**Tiếp tục tích hợp sau merge M01 (chưa nghiệm thu)**: T025 bổ sung retry toàn transaction bằng resilience có sẵn của Spring Framework 7; T043 bổ sung chỉ mục kỹ thuật waiting_hold_bed_ids trong resident_position bằng migration mới V003_04_1__waiting_hold_index.sql để lấy id hiện hành trước khóa giường tăng dần trong transaction RR của M01. Không đổi API/M01 isolation; cần kiểm hồi quy và clean verify cuối trước đánh task. Chi tiết lý do, phạm vi và bằng chứng test ở plan/Working-history.md.

**Kết quả kiểm sau tích hợp M01 (2026-10-24)**: 148 IT M03 và 392 unit/kiến trúc của kho xanh; frontend 232 test, lint/build qua. Clean verify nền toàn kho vẫn exit 1 (993 IT: 5 failure +9 error), chỉ còn fixture/contract M01 đang chờ B theo quyết định chủ dự án; không skip test và không đánh thêm [x]. T025/T043 có thêm V003_04_2__allocation_write_guard.sql, RC S/RR X trước position/room/bed, cùng pool đọc view RC độc lập để giữ đúng đính chính trong transaction RR. Không đổi isolation M01, API hoặc thư viện. Xem plan/Working-history.md cho đánh đổi và kết quả từng lệnh.

**Đối chiếu merge 2026-10-30**: giữ T017/T030–T035 mở vì chưa đủ nhánh/handler/provider hoặc kiểm tương ứng tại checkout; không lấy checkbox đánh sẵn của phía develop làm bằng chứng test. T029 đã có kiểm15ca ngày29/10; T032 còn guard503 theo quyết định giữ SPI. Các đường dẫn gói dùng roombedallocation; R-15/T066 vẫn là việc sau, không sửa migration cũ trong lượt ghép.

Đối chiếu checkbox khi ghép: giữ trạng thái có bằng chứng của nhánh C cho T001–T065; thêm T066/R-15 từ develop nhưng không tự nghiệm thu các task chỉ được đánh sẵn. Trạng thái kiểm hiện tại ghi ở plan mục merge2026-10-30.

**Sửa hẹp 2026-10-30**: FakeHandoverLocations dùng fixture trạng thái đầy đủ,giữ roomId/holdReason,lọc phạm vi trước kiểm dữ liệu và cắt khoảng Chờ sẵn sàng nhưng giữ startedAt; không tạo fake mới. T047 có kiểm nhật ký người cao tuổi thật. Bằng chứng đỏ/xanh và review ở plan,verification mục9. Chỉ hai phần sửa này hoàn tất tại checkout; giữ nguyên checkbox T047,T066 và các task/phase chưa đủ phạm vi.

**Cập nhật T061/T063 ngày 2026-10-31**: AllocationHandoverApi đã có provider thật trong BedReportingService;V003_07 lưu mốc đặt dấu chờ bác sĩ,legacy giữNULL. 39IT liên quan xanh,gồm consumer008 qua HTTP đọc nguồn thật;Spotless10file đạt,review độc lập không finding. Chi tiết và verify toàn kho ở verification.md mục10. Giữ checkbox vì T061/T063 còn phạm vi khác;LocationApi,R-15/T066 và nghiệm thu Phase11 chưa xong.
**Tiếp tục T033 ngày 2026-11-04:** đã cài `BedAllocationCommands` ủy bốn lệnh giữ chỗ cho `BedHoldService`, bảy lệnh chuyển theo lịch/callback cho `ScheduledBedTransfers`. Bộ cuối BUILD SUCCESS với 48 kiểm kiến trúc và 276 IT M03 xanh, gồm 18 ca `FR_029_ScheduledTransferIT`; Spotless qua. Đã quan sát RED trước cài facade, Q-322 và các hồi quy review. Q-322 được duyệt thêm `CANCELLED`; lệnh sẵn sàng giữ giường Trống khi hủy phân bổ chờ không còn phù hợp. Marker sau từ chối giữ nguyên khi lập lại yêu cầu; giờ dự kiến lấy từ ngày đã duyệt. Hạn lưu trú, giờ bắt đầu thực tế, trạng thái và metadata được kích hoạt bằng một UPDATE để tránh chồng tạm với lịch sử xen giữa. `V003_08` thêm chỉ mục căn cứ. Lượt clean verify toàn kho có 1.047 unit/kiến trúc xanh, 2.585 IT với 4 failure ở 004, 0 error, 2 skip; kết quả cụ thể trong plan và progress. Giữ T033 `[ ]` vì còn kết nối gỡ dấu tài sản 019 (nhánh có dấu trả 503 trước ghi), kiểm nguồn thật 004/007/019 ở T063 và gate toàn kho; 004 cần nối kiểm áp dụng/callback và đối chiếu fixture như mục 8 team-plan.


## Phase 12: Convergence

**Ngày rà soát: 2026-11-04.** Chỉ đánh giá trạng thái code hiện tại theo spec/plan/tasks và constitution, không so Git/nhánh/lịch sử, không chạy lại build/test trong lượt converge. Inventory: 52 FR còn áp dụng (8 FR đã bỏ loại khỏi việc mới), 14 SC, 67 acceptance scenarios, 22 edge cases; 17 nhóm quyết định G-01–G-16/R-15 và 16 nguyên tắc constitution. Các finding dưới đây là khoảng trống tĩnh có bằng chứng; không coi checkbox cũ hoặc log kiểm hẹp là nghiệm thu toàn feature.

**Phạm vi và thứ tự:** Giữ nguyên T001–T066; các task mới là phần còn thiếu được truy về task/FR gốc, không yêu cầu làm lại chức năng đã đủ. `BE/` = `backend/src/main/java/com/eldercare/backend/`, `BT/` = `backend/src/test/java/com/eldercare/backend/`. T033 đã có BedAllocationCommands/ScheduledBedTransfers và kiểm riêng; M01.statusPeriods, nguồn lưu trú/lịch đến004, LocationReferences008, API sự kiện004/007 và nền shared000 hiện đã có. Không tiếp tục dùng ghi chú chờ nguồn cũ thay cho kiểm code. C chủ trì003/019; phần nguồn004 thuộc B,007 thuộc D,008 thuộc E và nền chung thuộc A; cần Đổi api/sửa ngoài phạm vi phải được duyệt. Mobile nhân viên đã mở, chỉ dùng lệnh online theo plan003.

**Cách thực hiện:** Xử lý CRITICAL trước; test RED trước implementation mới, mỗi task kiểm phần liên quan cùng ArchTests, không bịa RED cho phần đã có. F2 phụ thuộc phần chuyển/nhập bù F3/F4 theo nhánh; F7/F8 cần phối hợp workflow004/019; F12 cần yêu cầu thật F3 và contract được duyệt; F14/F16 chờ chốt validator/nguồn; F19 cần HTTP F2; F24/F25 thực hiện khi đủ nguồn và các gap chức năng. R-15/F22 ảnh hưởng toàn lớp dữ liệu và fixture: làm theo migration/giữ id của T066, không sửa migration cũ. Converge không duyệt các đề nghị contract từng bị từ chối và không tự đóng task cũ.

- [ ] T067 **CRITICAL — F1** Bổ sung thao tác hủy phân bổ Tương lai, tạm ngừng/sử dụng lại và xác nhận giường sẵn sàng trên web tại `frontend/src/features/beds/`; dùng endpoint/backend hiện có, đúng vai trò/phạm vi, lý do, If-Match khi contract yêu cầu, Idempotency-Key và giữ khóa khi retry; bổ sung test UI/request thật. Không dùng trạng thái hiển thị thay quyền backend. Bằng chứng hiện tại: BedsPage.jsx/BedAllocationsPanel.jsx không có lệnh hủy FUTURE, tạm ngừng/sử dụng lại hoặc xác nhận sẵn sàng. Theo Constitution X; plan:NFR-10; FR-011/023/044; T028/T041/T060 (missing).

- [ ] T068 **CRITICAL — F2** Hoàn thiện `BE/roombedallocation/web/BedAllocationController.java` và request/response cho `POST /bed-allocations/{id}/transfer` đã duyệt, nối `BedTransferService`, xử lý chuyển ngay/nhập bù theo contract; viết test HTTP RED trước cài, kiểm quyền, rollback, lý do và status/schema, chỉ bỏ pending trong `ApiContractIT` sau endpoint thật hoạt động. Nhánh nhập bù phụ thuộc handler ở finding F3. Bằng chứng hiện tại: BedAllocationController.java chỉ có list/create/cancel/suitable; không có POST /{id}/transfer. Theo FR-028/029; US3/AC1; T036 (missing).

- [ ] T069 **CRITICAL — F3** Cài phần lệnh nhập bù và handler `M03.NHAP_BU_PHAN_BO` tại `BE/roombedallocation/internal/allocation/{service,domain,persistence}/`: phân biệt trong/ngoài CFG-M03-07, kiểm FR-018/019 trên toàn khoảng, dùng `ResidentStatusApi.statusPeriods()` thật đã có để chặn Tạm vắng/Điều trị/Hoạt động bên ngoài, kiểm lại khi duyệt AC-01, append dữ liệu yêu cầu và mốc nhập thực, không đổi vị trí khi còn chờ; viết test trực tiếp/duyệt/từ chối/chồng/rollback trước cài. Dùng cơ chế lưu trữ sau R-15 nếu T066 đã được thực hiện. Bằng chứng hiện tại: Chỉ có V003_04 và schema backfill; không có handler hoặc lệnh nhập bù. Theo FR-027; US3; SC-014; T031/T034 (missing).

- [ ] T070 **HIGH — F4** Hoàn thiện nhánh chuyển tiếp của phân bổ mang AWAITING_REQUEST/AWAITING_SAME_PRICE trong `BedTransferService` và các test FR-030; phối hợp B/004 chốt nguồn đối chiếu với giá hợp đồng (hiện guard 503), không suy giá từ hai loại phòng hiện tại để tự gỡ dấu. Chỉ mở rộng SPI sau duyệt, rồi kiểm y tế/an toàn, duyệt/từ chối, chuyển về cùng giá và marker cùng transaction. Bằng chứng hiện tại: BedTransferService.java:100–105 chặn 503 khi phân bổ đang chờ xử lý giá. Theo FR-030; SC-013; T031/T032 (partial).

- [ ] T071 **HIGH — F5** Cài bean production cho `roombedallocation.api.LocationApi.at(residentId, at)` trong `internal/allocation/service/`, đọc dữ liệu phân bổ đã áp đính chính, đúng khoảng nửa mở và vị trí lịch sử; dùng API/Store hiện có, không thêm fake thứ hai. Kiểm có/không phân bổ, qua chuyển/đính chính và consumer đang dùng `ObjectProvider<LocationApi>`. Bằng chứng hiện tại: api/LocationApi.java có khai báo nhưng không có provider production; LocationHistoryReading chỉ cài API khác. Theo FR-017/035; plan:G-05/G-10; T003/T026 (missing).

- [ ] T072 **HIGH — F6** Hoàn thiện đổi phòng giường chưa từng có phân bổ ở `internal/bed/service/BedService` đồng thời đồng bộ vị trí tài sản 019 trong cùng transaction; trước cài phải chốt/duyệt thao tác còn thiếu của `BedAssetPort` trong hợp đồng 003–019 và fake chung. C phụ trách cả phần 003/019; test chặn đã từng phân bổ, thành công chưa phân bổ, lỗi tài sản rollback; không lấy 503 hiện tại làm hành vi nghiệm thu. Bằng chứng hiện tại: BedService.update vẫn trả 503 khi đổi phòng giường chưa từng phân bổ; BedAssetPort thiếu thao tác đồng bộ. Theo FR-007; US1/AC7; T017 (partial).

- [ ] T073 **HIGH — F7** Cài bean `BedAssetCommandPort` tại `internal/bed/service/` bao đủ check/apply, gắn/gỡ dấu, giường Trống phù hợp cho người đang nằm; tái dùng `BedAssetStateService`, checker/locks/state và `ScheduledBedTransfers`. Nối `residentTransferFlagRemoved` để tiếp tục phân bổ chờ khi gỡ dấu theo FR-013b; giữ ngữ nghĩa CANCELLED đã duyệt cho Q-322, không tạo phiếu/yêu cầu. Test MANDATORY, thứ tự khóa, rollback và tương tác 019, phối hợp owner nguồn khi tích hợp. Bằng chứng hiện tại: BedAssetCommandPort có 5 method nhưng chưa có bean cài; ScheduledBedTransfers chặn nhánh có dấu tài sản. Theo FR-013b; FR-011a/029; T004/T033/T042 (missing).

- [ ] T074 **HIGH — F8** Hoàn thiện nhánh FUTURE có căn cứ/yêu cầu liên kết tại `BedAssetStateService` thay guard 503: ngoại lệ giường hỏng hợp lệ hủy các FUTURE, giữ giường cũ của người và xử lý yêu cầu đổi lưu trú theo FR-013a trong cùng transaction; phối hợp B/004 và A/000 trên hợp đồng hiện hành, xin duyệt trước nếu thiếu thao tác. Bổ sung test thành công và lỗi giữa chừng, không chỉ test còn 503; không đổi test người khác để xanh. Bằng chứng hiện tại: BedAssetStateService:108–111 trả 503 nếu FUTURE có căn cứ/yêu cầu liên kết. Theo FR-013a; US4/AC3; T038/T042 (partial).

- [ ] T075 **HIGH — F9** Nối `admission.api.events.ResidenceTypeChanged` đã công bố vào consumer đánh giá lại phù hợp của 003; giữ chiều phụ thuộc Modulith hợp lệ, không import internal admission hoặc khai event/fake thứ hai. Viết test sự kiện đổi loại lưu trú đặt/gỡ dấu ACTIVE/FUTURE, cảnh báo một lần và marker nhắc, không tự hủy/chuyển hoặc rollback lệnh đổi loại nguồn. Bằng chứng hiện tại: AllocationSuitabilityListener chỉ nghe 3 sự kiện M01, không nghe ResidenceTypeChanged đã có. Theo FR-034; BR-M03-23; T035 (missing).

- [ ] T076 **HIGH — F10** Nối consumer cho `incident.api.events.IsolationLifted` và `ContactAdded` đã có vào chức năng isolation/readiness của 003, dùng nguồn khoanh vùng/tiếp xúc hiện hành để gỡ dấu chờ bác sĩ chỉ khi phòng hết mọi chặn và báo Trưởng tầng khi người vừa rời giường Chờ sẵn sàng được thêm tiếp xúc. Viết test RED trước cài cho vùng gỡ nhưng còn cách ly riêng/vùng khác, lặp event, đúng tầng và kiểm điều kiện ngay lúc xác nhận; phối hợp D nếu còn thiếu provider, giữ chiều phụ thuộc theo hợp đồng. Bằng chứng hiện tại: roombedallocation không có consumer IsolationLifted/ContactAdded dù API incident đã có. Theo FR-009/024a/045; T053/T054 (missing).

- [ ] T077 **HIGH — F11** Hoàn thiện projection bảng giường và danh sách phù hợp trong `BedController`, `BedResponse`, `BedAllocationController.suitable`, service/Store và web beds: trả người đang ở, mốc phân bổ Tương lai, các dấu đúng người/giường, người trở về chưa có giường thay các null/List.of rỗng cố định; hiển thị các dữ liệu đó theo contract và quyền, chỉ nhận dạng/giới tính/mức chăm sóc được phép, không thông tin sức khỏe khác. Test HTTP với dữ liệu thật và UI, giữ query/checker nhất quán và không chọn sẵn. Bằng chứng hiện tại: BedResponse.of đặt occupant/futureAllocationFrom=null, flags=[]; BedController.list luôn trả residentsWithoutBed=[]; suitable cũng bỏ mốc FUTURE. Theo FR-022/024a/025a/034; T021/T027/T028 (partial).

- [ ] T078 **HIGH — F12** Hoàn thiện dấu có nhập bù đang chờ duyệt cho tra cứu theo người lẫn giường trong history service/Store/controller và web, kể cả giường đích chưa có phân bổ và mọi giường liên quan. Chốt biểu diễn HTTP với chủ dự án trước sửa schema (đề nghị thêm field trước chưa được duyệt); dùng yêu cầu thật từ F3, giữ lịch sử hiện có khi chờ và gỡ dấu đúng khi kết thúc yêu cầu, không suy chỉ từ người đang ở. Bằng chứng hiện tại: HTTP BedLocationHistoryRow thiếu dấu cấp giường; pending nội bộ chỉ theo người có dòng phân bổ. Theo FR-027/035; T034/T045/T046/T048 (partial).

- [ ] T079 **HIGH — F14** Hoàn thiện UI sửa/ngừng hiệu lực khu nghỉ và buổi tại `DayCarePanel.jsx` cùng test; trước cài chốt với chủ dự án cách lấy validator của từng tài nguyên vì response hiện thiếu version/ETag chi tiết và đề nghị thêm version trước đã bị từ chối. Sau duyệt dùng If-Match thật, reload khi xung đột, lý do ngừng, hiển thị buổi vượt sau đếm lại và giữ lịch cũ; sinh lại client nếu contract được đổi. Bằng chứng hiện tại: DayCarePanel chỉ import GET/POST tạo, thiếu sửa/ngừng khu nghỉ và buổi. Theo FR-038/040; plan:web đầy đủ; T052 (partial).

- [ ] T080 **HIGH — F15** Sửa lệnh tạo khu nghỉ/buổi của `DayCarePanel.jsx` gửi `Idempotency-Key` đúng contract đã có và giữ cùng khóa cho retry cùng nội dung, không chỉ dựa vào cờ busy. Viết test request/header đi qua client hoặc HTTP fixture và replay không tạo hai mục; áp dụng cùng cơ chế cho lệnh sửa/ngừng bổ sung ở F14, không thay contract hoặc bỏ header bắt buộc. Bằng chứng hiện tại: DayCarePanel.save không gửi Idempotency-Key, trong khi operation tạo bán trú yêu cầu header này; client không tự thêm. Theo plan:R-03/API contract; T052 (contradicts).

- [ ] T081 **HIGH — F16** Hoàn thiện lỗi 409 khi ngừng khu nghỉ còn lịch hiện tại/tương lai: phối hợp B chốt kênh trả residentId từ nguồn lịch 004 (SPI hiện chỉ boolean, đề nghị mở trước chưa được duyệt), cập nhật interface/fake/hợp đồng đúng quy trình rồi trả danh sách người gây chặn và hiển thị trên web. Kiểm đủ dữ liệu tương lai theo nguồn 004, không dùng thông báo chung thay danh sách và không tự hủy lịch. Bằng chứng hiện tại: DayCareConfigurationService chỉ trả thông báo chung; DayCareArrivalCounter chỉ có boolean kiểm lịch tương lai. Theo spec:Edge Cases khu nghỉ còn lịch tương lai; T051/T052 (partial).

- [ ] T082 **HIGH — F17** Hoàn thiện tính đúng dữ liệu thời điểm quá khứ cho `BedReportingService`/`BedReportingStore`: đối chiếu contract SC-008 và nguồn lịch sử quan hệ phòng/tầng, chính sách, cách ly, giữ chỗ/FUTURE và dấu tài sản thay việc ghép state lịch sử với `policies()`/flags hiện hành. Chốt dữ liệu/port cần thêm với chủ dự án, A/016, E/008 và C/019 trước đổi hợp đồng; test thay đổi sau thời điểm hỏi không làm đổi kết quả/scope cũ, không mặc nhiên nhận giới hạn hiện tại là đã đạt. Bằng chứng hiện tại: countAvailable đọc policies/dấu hiện hành; pendingStays/isolatedRooms dùng quan hệ/dấu hiện tại cho thời điểm quá khứ. Theo SC-008; FR-007; plan:T061; T061 (partial).

- [ ] T083 **HIGH — F19** Cài và đăng ký màn chuyển giường mobile trong `mobile/lib/staff/` theo cấu trúc README, dùng client/session và API đã duyệt của 003; chỉ online, tự chọn giường, lý do bắt buộc, phạm vi Trưởng tầng, hiển thị đủ điều kiện/lỗi và chống gửi lặp. Phụ thuộc HTTP chuyển thường F2; viết test widget/request trước cài, không tiếp tục dùng lý do mobile nhân viên chưa mở. Bằng chứng hiện tại: mobile/lib/staff chưa có màn chuyển giường hoặc test tương ứng. Theo plan:Target Platform; FR-028/029; T036 (missing).

- [ ] T084 **HIGH — F20** Cài và đăng ký màn mobile đặt/gỡ cách ly phòng, xem lịch sử và xác nhận ở lại vùng cách ly của Bác sĩ theo T056/plan Target Platform; dùng API/quyền hiện có, lý do và tài khoản ACTIVE, chỉ trực tuyến. Viết test widget/request và các ca ngoài quyền/lỗi/không tự gỡ dấu sai, phối hợp phần nền mobile A khi cần, không thêm thư viện hoặc endpoint ngoài contract. Bằng chứng hiện tại: mobile/lib/staff chưa có màn cách ly/xác nhận ở lại hoặc test tương ứng. Theo plan:Target Platform; FR-008/010/024a; T056 (missing).

- [ ] T085 **HIGH — F21** Cài và đăng ký bảng giường tầng và xác nhận sẵn sàng mobile tại `mobile/lib/staff/`, đọc điều kiện từ API 003 và bắt buộc checkbox khử khuẩn/bảo hộ khi cần; đúng Trưởng tầng/Người phụ trách ca của tầng, chỉ trực tuyến, giữ Idempotency-Key khi retry. Viết test widget/request/422/quyền và nhánh xác nhận tiếp chuyển chờ; dùng nền A hiện có, không tạo fake nguồn thứ hai. Bằng chứng hiện tại: mobile/lib/staff chưa có bảng giường tầng/xác nhận sẵn sàng hoặc test. Theo plan:Target Platform; FR-044/045; T060 (missing).

- [ ] T086 **HIGH — F22** Hoàn thiện T066/R-15 theo data-model 0.1: nền 000 T108 và shared APIs hiện đã có; thêm migration `V003_95`, chép/ánh xạ dữ liệu và legacy id, sửa khóa ngoại/JSON/view/retention/correction/audit, chuyển service/Store/entity sang CatalogApi/StatusHistoryApi/RecordEventApi/SimpleRecordApi/ResidentModuleStateApi/ReminderMarkers/TechnicalLocks trước bỏ bảng cũ. Giữ id fact nhóm 3 và đủ ràng buộc, chuyển fixture dùng chung/test thuộc các module theo danh sách 0.1 có owner review; test migration dữ liệu có sẵn, quyền/trigger/race và ApiContractIT, không sửa migration đã merge. Bằng chứng hiện tại: Chưa có V003_95; stores/entity vẫn dùng area/floor/room_type/resident_position/bảng nhắc/khóa cũ, chưa gọi shared APIs. Theo plan:R-15; tasks:T066; data-model 0.1 (missing).

- [ ] T087 **HIGH — F24** Hoàn thiện kiểm tích hợp T063 với bean production: tái dùng nguồn M01/004/008 đã có, phối hợp D cung cấp AllocationInfectionStatus và C/019 BedAssetPort; phần C nối facade/callback thật, B review `checkScheduledTransfer` khi áp dụng sau đặt trước và callback sau chuyển gấp trong 004, E review consumer 008. Chạy lại US2/4/7/8, quyền/transaction/rollback/sự kiện theo nguồn thật, không để fake @Primary che provider hoặc sửa code người khác khi chưa được phép; giữ T033 mới đã cài, không khai lại facade. Bằng chứng hiện tại: 004/008 có nguồn thật nhưng 007 chưa cài AllocationInfectionStatus, 019 chưa cài BedAssetPort; caller004 thiếu kiểm/callback theo facade mới. Theo FR-029/030/031/042/045; plan:G-02; T063 (partial).

- [ ] T088 **HIGH — F25** Hoàn thiện kiểm và truy vết T062/T064/T065 sau các gap của phase này: test các nhánh thiếu trước cài, pending HTTP chuyển thường chỉ gỡ sau F2, kiểm trigger/quyền MySQL và từng DBR/BR/FR có test thực, chạy quickstart Q1–Q37 với dữ liệu/nguồn được phép. Tách phần cùng phòng 003 dưới 1 giây và journey SC-003 qua 007, benchmark server 300 giường khỏi SC-005 60 người dùng/ít nhất 10 thao tác/trung vị 2 phút; xác minh lại test cạnh tranh/performance trong kiểm toàn kho, không hạ ngưỡng/skip. Chạy test liên quan+ArchTests và frontend/mobile đúng phạm vi, cuối phase clean verify nền, phối hợp B xử lý lỗi 004 thực tế còn lại thay ghi build toàn kho xanh từ scoped log. Bằng chứng hiện tại: Có scoped tests/log T033 xanh, nhưng endpoint còn pending và chưa đủ bằng chứng quickstart/E2E/performance/coverage với các workflow thật. Theo SC-001–014; plan:G-16; T062/T064/T065; Constitution XV (partial).

- [ ] T089 **HIGH — F27** Đồng bộ `AllocationStatusParticipant` và test FR_024 với FR-012/015/024 cập nhật Q-395: lệnh cho tạm vắng/chuyển viện mới luôn giữ giường không hạn, không tự đóng phân bổ theo nhánh HeldBed(false) hoặc đặt hạn người vắng; chỉ giải phóng bằng lệnh được quy định. Bổ sung RED cho nhánh cũ và giữ đọc được dữ liệu/lịch sử cũ, không xóa event lịch sử hoặc tự thay public API, kiểm trở về/giải phóng cùng transaction nguồn004. Bằng chứng hiện tại: AllocationStatusParticipant:174–202 còn nhánh không giữ và giữ có hạn theo HeldBed; test FR_024 còn kỳ vọng nhánh cũ. Theo FR-012/015/024; Q-395; US4; T039/T043 (contradicts).

- [ ] T090 **MEDIUM — F13** Nối khả năng xem giá trị gốc và chuỗi đính chính từ `frontend/src/features/beds/LocationHistoryPanel.jsx` qua cơ chế/API correction 000 đã có; giữ kết quả tra cứu theo giá trị hiện hành đã đính chính, đúng quyền và không lộ sức khỏe. Nếu contract hiện có thiếu khóa liên kết thì xin duyệt trước bổ sung, không sửa tay client sinh; test mở xem gốc sau nhiều lần đính chính và bị chặn ngoài phạm vi. Bằng chứng hiện tại: LocationHistoryPanel chỉ có nhãn đính chính, không nối xem giá trị gốc/chuỗi qua CorrectionSheet. Theo FR-035; US5/AC4; T048 (partial).

- [ ] T091 **MEDIUM — F18** Review và biện minh việc `BedStatusApi.flaggedBeds` đang trả AWAITING_REQUEST/AWAITING_SAME_PRICE ngoài ba dấu mô tả trong hợp đồng module 1.8; phối hợp A/016 và E/008 xác định consumer. Nếu giữ phải được duyệt và bổ sung hợp đồng/glossary/fake đúng owner; nếu không đưa implementation về hợp đồng, không tự bỏ quy tắc nhắc FR-030 hoặc đổi chữ ký cũ. Bằng chứng hiện tại: flaggedBeds trả thêm AWAITING_REQUEST/AWAITING_SAME_PRICE ngoài ba dấu được mô tả ở hợp đồng thao tác. Theo plan:T061; contracts/module-interfaces 1.8 (unrequested).

- [ ] T092 **MEDIUM — F23** Chuyển các record dữ liệu trả về còn khai lồng trong `AllocationReminderStore`, `BedAssetStateStore`, `BedHoldStore`, `BedReadinessStore` sang `internal/<function>/domain/`, cập nhật imports/service/test liên quan theo backend README/R-02; giữ hành vi/chữ ký công khai, không refactor ngoài phạm vi. Chạy test phần tương ứng cùng LayerArchTest/ModuleBoundaryTest/SolidArchTest. Bằng chứng hiện tại: 4 Store còn khai record dữ liệu trả về lồng trong persistence thay vì domain. Theo plan:R-02/layering; T001/T066 (partial).

- [ ] T093 **MEDIUM — F26** Hoàn thiện cách kiểm độc lập/test-demo theo plan NFR-13/G-16 và quickstart bằng fake chung đúng owner và công tắc/harness có thể chạy thật; kiểm hiện có trước khi bổ sung, không đưa fake vào production. Nếu cần HTTP testing hoặc mở runJobs ngoài hợp đồng hiện hành phải chốt với A/chủ dự án trước (đề nghị cũ không mặc nhiên được duyệt); test profile isolation và ghi bằng chứng chạy kịch bản, không nhận endpoint mới chỉ từ mô tả quickstart. Bằng chứng hiện tại: FakeM03Config có fake test nhưng chưa có công tắc/demo harness cho nguồn 003 theo kế hoạch. Theo plan:NFR-13/G-16; T002/T064 (partial).

- [ ] T094 **MEDIUM — F28** Review/biện minh hai kiểu `api/events/AllocationStarted` và `AllocationClosed` không có producer trong khi `staffing.ratio.StaffingRatioEvents` nghe cả aliases lẫn BedAllocationStarted/Closed; thống nhất với E bộ sự kiện thật cho T005/G-12. Nếu aliases không có yêu cầu được duyệt thì đề xuất loại bỏ và cập nhật consumer theo quy trình Đổi api; nếu giữ, ghi rõ compatibility/producer và test, không tự khai producer trùng gây xử lý hai lần. Bằng chứng hiện tại: AllocationStarted/AllocationClosed còn khai báo nhưng không có producer, trong khi StaffingRatioEvents nghe cả chúng và BedAllocationStarted/Closed. Theo plan:G-12; T005/T063/T065 (unrequested).

**Kết quả converge:** `tasks_appended`, 28 finding/task (3 CRITICAL, 20 HIGH, 5 MEDIUM; 11 missing, 13 partial, 2 contradicts, 2 unrequested). Chỉ thêm Phase 12; các tiêu chí xác minh/đóng task theo quy trình đang áp dụng, không lấy bản ghi này thay kết quả test. Lượt tiếp theo chạy `$speckit-implement` trên các task Convergence được phép/đủ điều kiện, rồi converge lại sau triển khai.
