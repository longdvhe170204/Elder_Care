---
description: "Danh sách task triển khai feature 003 – Phòng, giường và phân bổ giường"
---

# Tasks: Phòng, giường và phân bổ giường (003)

**Input**: Tài liệu thiết kế ở `specs/003-room-bed-allocation/` (plan.md, spec.md, research.md G-01 → G-16, data-model.md, contracts/api-003.yaml, contracts/module-interfaces.md, quickstart.md)

**Prerequisites**: 000, 002 đã xong Phase 2; 001 đã có `m01.api` (SPI `ThamGiaChuyenTrangThai`, sự kiện mức chăm sóc).

**Tests**: Bắt buộc (constitution XV). Tên lớp test theo quickstart.md mục 4 và data-model.md mục 11.

**Giai đoạn**: Theo mục "Giai đoạn hiện tại" của CLAUDE.md (constitution IX); task của `mobile/` chỉ làm khi mục đó mở phần mobile, trừ màn mẫu thiết kế.

## Format: `[ID] [P?] [Story] Mô tả`

## Quy ước đường dẫn

- `BE/` = `backend/src/main/java/com/cambae/backend/`, `BT/` = `backend/src/test/java/com/cambae/backend/`, `MIG/` = `backend/src/main/resources/db/migration/`
- Module M03 (phần phòng, giường): `BE/m03/{api,cocau,giuong,phanbo,cachly,bantru,lich}/`; `m03.taisan` thuộc feature 019
- Web `frontend/src/tinh-nang/{co-cau-phong-giuong,bang-giuong,phan-bo-giuong,lich-su-vi-tri,cach-ly-phong,khu-nghi-ban-tru}/`; app nhân viên `mobile/lib/nhan-vien/{bang-giuong-tang,chuyen-giuong,cach-ly-phong}/`

---

## Phase 1: Setup

- [x] T001 Tạo khung tài liệu gói `BE/m03/api/`, `BE/m03/internal/{structure,bed,allocation,isolation,daycare}/`, `BE/m03/scheduling/` bằng `package-info.java` theo backend/README.md; cổng giữa giường 003 và tài sản 019 nằm ở `internal/bed/domain/`, không cho hai phần đọc trực tiếp dữ liệu của nhau (research G-01; test `FR_001_ModuleSetupTest`)
- [x] T002 [P] Tạo bản giả trong `BT/support/fake/m03/`, nhập bằng `FakeM03Config`: `AllocationResidencySource`, `DayCareArrivalCounter` (004), `AllocationInfectionStatus` (007), `LocationReferences` (008), `BedAssetPort` (019); tên logic và chữ ký theo contracts mục 2.1, mục 3, glossary mục 9 (research G-16; test `FR_018_FakeM03Test`)

**Ghi chú kỹ thuật Phase 1 (2026-10-22)**: đưa riêng khai báo bốn SPI, `BedAssetPort` và các kiểu chữ ký cần cho T002 từ T004 lên Phase 1 để bản giả biên dịch được. Không đổi thao tác trong hợp đồng, không thêm REST endpoint. T004 vẫn chưa xong: còn cổng lệnh tài sản giường, đối chiếu với API của T003 và hoàn thiện toàn bộ khai báo Phase 2. Xem plan mục "Ghi chú triển khai"; T001/T002 chỉ đánh `[x]` sau khi `clean verify` qua.

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m03.api`, SPI cho module khác cài, sự kiện, bảng cơ cấu và giường mà mọi story dùng.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [x] T003 [P] Tạo giao diện công khai `BE/m03/api/`: `PhanBoGiuongLenhApi` (mọi hàm MANDATORY), `ViTriApi`, `LichSuViTriApi`, `SucChuaBanTruApi`, `CoCauViTriApi`, `PhongChoPhepApi`, `TinhTrangGiuongApi`, `GiuongThongKeApi` (016), `CanhBaoBanGiaoApi` (008) đúng contracts/module-interfaces.md mục 1.2 → 1.9
- [x] T004 [P] Khai báo SPI ở `BE/m03/api/`: `NguonLuuTruChoPhanBo`, `DemLichDenBanTru`, `TinhTrangLayNhiemChoPhanBo`, `ThamChieuViTri`; port `BE/m03/giuong/port/GiuongTheoTaiSanPort.java` và `TaiSanGiuongPort` (contracts mục 2.1; research G-02)
- [x] T005 [P] Tạo sự kiện `BE/m03/api/sukien/` theo contracts mục 1.11 (`DaChuyenGiuong`, `GiuongDaTrong`, `PhongDaDatCachLy`, `PhongDaGoCachLy`, các sự kiện nhắc, báo); sự kiện gửi 009 cài `CoYeuCauThongBao` (research G-12)
- [x] T006 Tạo `MIG/V003_01__co_cau.sql`: `khu_vuc` (`ma VARCHAR(20) UNIQUE` kể cả mục Ngừng hiệu lực), `tang` (`UNIQUE (khu_vuc_id, ma)`, `ten_toa_nha NULL`), `loai_phong` (`so_giuong_tieu_chuan SMALLINT > 0`, `CHECK (phuc_vu_dai_han OR phuc_vu_ngan_ngay)`, `ten_hieu_luc UNIQUE`), `loai_phong_muc`, `phong` (`ma VARCHAR(20) UNIQUE`, `chinh_sach_gioi_tinh ENUM('NAM','NU','KHONG_GIOI_HAN')`, `dang_cach_ly BOOLEAN` mặc định `false`), `phong_muc_cho_phep`, `ly_do_chuyen_giuong` (`ma VARCHAR(30) UNIQUE`, `y_te_an_toan`, `co_dinh`; nạp dòng `KIEM_SOAT_LAY_NHIEM` cố định) theo data-model mục 2; mã mức `ENUM('CO_BAN','THUONG_XUYEN','DAC_BIET')` không khóa ngoại sang M01
- [x] T007 Tạo `MIG/V003_02__giuong.sql`: `giuong` theo data-model mục 3 (`UNIQUE (phong_id, ma)`; `trang_thai ENUM('TRONG','DANG_SU_DUNG','DANG_GIU_CHO','CHO_SAN_SANG','DANG_BAO_TRI','TAM_NGUNG','KHONG_SU_DUNG')`; `ly_do_giu_cho ENUM('NGUOI_VANG','HO_SO_CHO') NULL`; năm `CHECK` của mục 3; `phan_bo_vua_dong_id`; `ly_do_tam_ngung VARCHAR(2000) NULL`) và `lich_su_trang_thai_giuong` (nhóm 3 chỉ thêm; `thoi_diem_du_kien`, `thuc_hien_tre`, `da_khu_khuan`, `da_dung_bao_ho`, `bat_buoc_khu_khuan`, `tang_khu_vuc_id`; `CHECK (su_kien <> 'XAC_NHAN_SAN_SANG' OR bat_buoc_khu_khuan = false OR (da_khu_khuan AND da_dung_bao_ho))`; hai trigger)
- [x] T008 Cài `BE/m03/giuong/internal/GiuongTrangThaiService.java`: một điểm ghi trạng thái giường với bảng chuyển data-model mục 3 cài trong mã; khóa giường theo `id`; mỗi lần chuyển thêm một dòng lịch sử và nhật ký `M03_GIUONG_CHUYEN`; tính `thuc_hien_tre` theo CFG-M03-08 cho việc của Bộ lập lịch (research G-04; FR-011 → FR-016a)
- [x] T009 [P] Test kiến trúc `BT/m03/ChiMotNoiGhiTrangThaiArchTest.java`: chỉ `GiuongTrangThaiService` ghi `giuong.trang_thai`
- [x] T010 [P] Cài provider cho 000/002 trong `BE/m03/phanbo/internal/`: `PhanLoaiTruongProvider` (`lyDo` cách ly và chuyển kiểm soát lây nhiễm → `SUC_KHOE`), `GanPhamViProvider` (`M03.GIUONG`, `M03.PHONG`, `M03.PHAN_BO_GIUONG`), `LuuTruProvider` theo data-model mục 10 (research G-14, G-15)

**Checkpoint**: Bảng cơ cấu, giường và điểm ghi trạng thái sẵn sàng.

**Ghi chú kỹ thuật Phase 2 (2026-10-23)**: mã thật theo glossary mục 9: các API ở `m03.api`, sự kiện ở `api/events`, cổng nội bộ ở `internal/bed/domain`; migration `V003_01__room_structure.sql`, `V003_02__bed_state.sql`. T008 dùng `BedStateService` điều phối và `BedStateStore` giữ SQL; T009 là `SingleBedStateWriterArchTest` chặn mọi lớp khác gọi `writeState`, được viết đỏ trước T008. T003/T004 có fake tập trung `FakeM03ApiConfig`. Thêm khóa ngoại `bed.last_closed_allocation_id` tại V003_03/T023, khi `bed_allocation` đã tồn tại. Xem plan mục "Ghi chú triển khai" cho mapping, điều kiện tích hợp còn chờ và bằng chứng kiểm từng task.

---

## Phase 3: User Story 1 – Quản lý viện dựng cơ cấu khu vực, tầng, phòng, giường (Priority: P1) 🎯 MVP

**Goal**: Cơ cấu bốn cấp, mã duy nhất, chính sách phòng, ngừng hiệu lực có chặn, tạo giường ghi tăng tài sản (FR-001 → FR-007, BR-M03-22, DBR-32 phần 003).

**Independent Test**: 1 khu vực, 2 tầng, mỗi tầng 2 phòng khác chính sách, mỗi phòng 2–4 giường; thử mã trùng, xóa phòng đã từng có phân bổ, sửa bằng Trưởng tầng.

### Tests cho User Story 1

- [x] T011 [P] [US1] Test `BT/m03/CoCauIT.java`, `MaDuyNhatIT.java`, `NgungHieuLucCoCauIT.java`: mã trùng → 409; Trưởng tầng sửa → 403; ngừng hiệu lực phòng còn giường không `KHONG_SU_DUNG` (kể cả `TAM_NGUNG`) → 409 liệt kê; `ThamChieuViTri` còn phân công → 409 (US1 #1–8; FR-003, FR-005)
- [ ] T012 [P] [US1] Test `BT/m03/FR_006_DoiChinhSachPhongIT.java`, `FR_007_KhongDoiChaIT.java`: đổi giới tính/mức của phòng vi phạm phân bổ hiện tại → 409 liệt kê người; đổi tầng của phòng hoặc phòng của giường đã có phân bổ → 409
- [x] T013 [P] [US1] Test `BT/m03/BR_M03_22_SoGiuongTieuChuanIT.java`, `Q_238_TaoGiuongGhiTangIT.java`: vượt số giường tiêu chuẩn → 201 kèm cảnh báo `VUOT_SO_GIUONG_TIEU_CHUAN`; tạo giường gọi `TaiSanGiuongPort.ghiTangChoGiuong` cùng transaction, lỗi thì rollback

### Implementation cho User Story 1

- [x] T014 [P] [US1] Tạo entity `BE/m03/cocau/internal/{KhuVuc,Tang,LoaiPhong,Phong,LyDoChuyenGiuong}.java` (`@NhomDuLieu(DANH_MUC)`) và `BE/m03/giuong/internal/Giuong.java`
- [ ] T015 [US1] Cài `BE/m03/cocau/internal/CoCauService.java`: tạo, sửa, ngừng hiệu lực, kích hoạt lại, xóa (chỉ khi chưa từng tham chiếu, kể cả `ThamChieuViTri.daTungThamChieu`) cho khu vực, tầng, loại phòng, phòng, lý do chuyển; sửa chính sách phòng khóa giường và kiểm (a), (c); nhật ký `M03_CO_CAU_*` (FR-001 → FR-007)
- [x] T016 [US1] Cài tạo giường (`TAO_GIUONG` → `TRONG`, gọi ghi tăng tài sản 019, cảnh báo BR-M03-22) trong `BE/m03/giuong/internal/GiuongService.java`; giường không có lệnh xóa (Q-237)
- [ ] T017 [US1] Tạo controller `BE/m03/cocau/web/CoCauController.java` và `BE/m03/giuong/web/GiuongController.java` cho `/khu-vuc`, `/tang`, `/loai-phong`, `/phong`, `/ly-do-chuyen-giuong` (kèm `/{id}`, `/ngung-hieu-luc`, `/kich-hoat-lai`), `POST/GET /giuong`, `GET /giuong/{id}` theo contracts/api-003.yaml; cài `CoCauViTriApi`, `PhongChoPhepApi`
- [x] T018 [P] [US1] Tạo màn web `frontend/src/tinh-nang/co-cau-phong-giuong/` (cây khu vực → tầng → phòng → giường, loại phòng, lý do chuyển)

**Checkpoint**: Cơ cấu dựng được độc lập.

**Ghi chú kỹ thuật Phase 3 (2026-10-23, chưa hoàn tất)**: T014 dùng entity tiếng Anh ở `internal/structure/domain`, `internal/bed/domain`; chuẩn bị mapping trước service. Test T011 là `FR_001_StructureIT`, `FR_003_UniqueCodesIT`, `FR_005_DeactivateStructureIT`; T013 gộp hai kiểm tra tạo/cảnh báo và rollback tài sản trong `BR_M03_22_BedCreationIT`; T015 có test bổ sung `FR_003_StructureVersionIT`. `StructureService`/`StructureStore`, `BedService`/`BedStore` theo tầng README. Sau quyết định của chủ dự án, đã dịch 39 operation HTTP Phase 3, cài controller/API đọc và màn frontend/src/features/beds. T012 và phần FR-006/T015 chờ API/fake M01 cùng dữ liệu phân bổ T023; chủ dự án chọn chờ B, không viết hộ. Đổi phòng giường tạm hoãn vì chưa duyệt mở rộng BedAssetPort; chỉ sửa mã giường, T017 giữ chưa đánh vì phần sửa phòng chưa đủ. Đã kiểm nền clean verify: 167 unit/kiến trúc + 450 IT, 0 failure/error/skip; web 105 test, lint/build qua. Chỉ đánh T011/T013/T014/T016/T018 sau các kết quả này; chưa nghiệm thu toàn Phase 3. Task chưa xong đầu tiên: T012. Xem plan và Working-history.md cho giới hạn và bằng chứng.

---

## Phase 4: User Story 2 – Hành chính phân bổ giường; không bao giờ để một giường có hai người (Priority: P1)

**Goal**: `KiemTraPhanBo` dùng chung cho danh sách và lệnh; phân bổ ban đầu `TUONG_LAI`; phân bổ cho người chưa có giường; khóa tuần tự và ràng buộc CSDL chống trùng (FR-017 → FR-023, FR-025a, DBR-09, DBR-10, BR-M03-01, 02, 19, 20).

**Independent Test**: Phòng nữ chỉ cơ bản, phòng cách ly, giường bảo trì; thử phân bổ từng người vi phạm từng điều kiện; hai tài khoản phân bổ cùng giường gần đồng thời.

### Tests cho User Story 2

- [ ] T019 [P] [US2] Test `BT/m03/BR_M03_01_DieuKienPhanBoIT.java`: mức (hiện hành → hợp đồng → lượt đăng ký, Q-301), loại hình, giới tính, cách ly/khoanh vùng, trạng thái giường, giữ chỗ, dấu tài sản; trả đủ điều kiện chưa thỏa (DBR-10, BR-M03-01)
- [ ] T020 [P] [US2] Test `BT/m03/BR_M03_02_HaiNguoiCungGiuongIT.java`, `DBR_09_KhongChongIT.java`, `FR_020_PhanBoDongThoiIT.java`: 10 luồng trên một giường chỉ một thành công (SC-001); `INSERT` trực tiếp chồng khoảng bị trigger chặn; cột sinh `UNIQUE` chặn hai đang hiệu lực
- [ ] T021 [P] [US2] Test `BT/m03/FR_023_PhanBoBanDauIT.java`, `BR_M03_20_PhamViPhanBoBanDauIT.java`, `FR_022_DanhSachGiuongIT.java`, `BR_M03_19_TroVeChuaCoGiuongIT.java`: người Đang tiếp nhận chỉ có `TUONG_LAI`; phạm vi theo giường; danh sách giường phù hợp không chọn sẵn và khớp lệnh; người trở về không có giường mang dấu "chưa có giường" và được nhắc (US2 #1–15)
- [x] T022 [P] [US2] Test `BT/m03/PhanBoRangBuocDbIT.java`: mọi cột sinh, trigger, `CHECK` của `phan_bo_giuong` (tích hợp MySQL)

### Implementation cho User Story 2

- [x] T023 [US2] Tạo `MIG/V003_03__phan_bo_giuong.sql`: `phan_bo_giuong` theo data-model mục 4 (`trang_thai ENUM('TUONG_LAI','DANG_HIEU_LUC','DA_DONG','DA_HUY')`; cột sinh `ket_thuc_hieu_dung = COALESCE(ket_thuc, ket_thuc_du_kien, '9999-12-31')`; cột sinh `giuong_dang_hieu_luc`, `nct_dang_hieu_luc`, `nct_tuong_lai` có `UNIQUE`; năm `CHECK` của mục 4; chỉ mục `(giuong_id, bat_dau)`, `(nct_id, bat_dau)`, `(trang_thai, bat_dau)`; trigger `trg_phan_bo_giuong_khong_chong`, `_khong_sua` khi `DA_DONG`/`DA_HUY`, `_khong_xoa`), `vi_tri_nct` (`nct_id` khóa chính, `chua_co_giuong_tu`, `phien_ban`), view `v_phan_bo_hien_hanh` (áp `v_dinh_chinh_moi_nhat`, loại `v_ban_ghi_da_huy`), `nhac_phan_bo` (khóa chính `(loai_doi_tuong, doi_tuong_id, loai_dau)`)
- [x] T024 [P] [US2] Tạo entity `BE/m03/phanbo/internal/PhanBoGiuong.java`, `ViTriNct.java`, `NhacPhanBo.java`
- [ ] T025 [US2] Cài `BE/m03/phanbo/internal/KiemTraPhanBo.java` (FR-018 (a)–(g), dùng `NguonLuuTruChoPhanBo`, `TinhTrangLayNhiemChoPhanBo`, dấu tài sản) và `KhoaTuanTu.java` (khóa người cao tuổi khi qua 001 → `vi_tri_nct` → giường theo `id`; `@Retryable` khi deadlock) (research G-03, G-05)
- [ ] T026 [US2] Cài `BE/m03/phanbo/internal/PhanBoService.java`: phân bổ cho người Đang tiếp nhận (`TUONG_LAI`, giường `CHO_SAN_SANG` theo FR-011a, gọi `daPhanBo` kết thúc giữ chỗ hồ sơ chờ), phân bổ cho người chưa có giường (`DANG_HIEU_LUC`, gỡ dấu), hủy phân bổ tương lai có lý do; nhật ký `M03_PB_*`; cài `PhanBoGiuongApi` (002), `ViTriApi` (FR-017 → FR-023, FR-025a)
- [ ] T027 [US2] Tạo `BE/m03/phanbo/web/PhanBoController.java`: `GET /phan-bo-giuong/giuong-phu-hop`, `POST/GET /phan-bo-giuong`, `GET /phan-bo-giuong/{id}`, `POST …/{id}/huy`, `GET /nguoi-cao-tuoi/{nctId}/vi-tri`, `GET /giuong/tong-hop`
- [ ] T028 [P] [US2] Tạo màn web `frontend/src/tinh-nang/phan-bo-giuong/ChonGiuong.jsx` (không chọn sẵn), `frontend/src/tinh-nang/bang-giuong/BangGiuong.jsx` và test `ChonGiuong.test.jsx`, `BangGiuong.test.jsx`, `DieuKienChuaThoa.test.jsx`

**Checkpoint**: Phân bổ an toàn đồng thời.

**Ghi chú khởi động Phase 4 (2026-10-23)**: Đã viết trước `DBR_09_AllocationSchemaIT` (phần T020/T022), chưa có migration/service mới. Lệnh test đã biên dịch trên JDK 25 nhưng 5 ca lỗi khởi tạo do thiếu `TEST_DB_URL`; chưa quan sát được assertion đỏ do thiếu chức năng, chưa đủ điều kiện viết triển khai theo quy trình test trước. Giữ toàn bộ T019–T028 chưa đánh. T019/T025/T026 còn cần API/fake M01 (trạng thái, giới tính, mức chăm sóc) do B sở hữu; không viết bản giả thứ hai. Khóa dòng M01 chỉ do lệnh trạng thái của 001 giữ trước khi gọi 003 (G-03), không thêm cổng khóa M01 mới. Chủ dự án đã duyệt dịch riêng HTTP Phase 4, chưa sửa hợp đồng/client khi backend chưa triển khai. Xem plan và Working-history.md cho kết quả kiểm cuối lượt.

**Tiếp tục Phase 4 (2026-10-23)**: Dịch 6 operation đã tồn tại của T027 sang tiếng Anh theo duyệt của chủ dự án, giữ 64 operation khác và các component cũ. Thêm schema clone, glossary mục 9, sinh lại client; cập nhật tên 6 endpoint pending trong ApiContractIT, không gỡ pending. Test DB chuẩn bị tăng lên 8 ca, thêm khóa giường ACTIVE ngay cả khi không chồng, thứ tự effective_end, view đính chính/hủy ghi nhận. Chưa có migration/service/controller Phase 4; toàn bộ task vẫn chưa đánh. T027 còn lệch: tasks yêu cầu GET danh sách nhưng contract chưa khai báo; đã hỏi bổ sung GET /bed-allocations và đang chờ duyệt, chưa tự thêm.

---

## Phase 5: User Story 3 – Chuyển giường bằng một lệnh đóng bản ghi cũ và mở bản ghi mới (Priority: P1)

**Goal**: Chuyển giường một transaction; chuyển theo lịch gắn yêu cầu thay đổi lưu trú; chuyển gấp khác giá; nhập bù qua phê duyệt; dấu "phòng không còn phù hợp" (FR-027 → FR-034, BR-M03-04, 07, 23).

**Independent Test**: Chuyển A từ P201-G1 sang P305-G2 lúc 14:00 → bản cũ kết thúc và bản mới bắt đầu đúng 14:00, G1 Chờ sẵn sàng, G2 Đang sử dụng; lỗi giữa chừng không để thay đổi nửa vời.

### Tests cho User Story 3

- [ ] T029 [P] [US3] Test `BT/m03/BR_M03_07_ChuyenGiuongIT.java`, `FR_033_ChuyenDongThoiIT.java`, `BR_M03_04_SuKienChuyenIT.java`: một transaction; lỗi giữa chừng rollback; hai lệnh chuyển cùng người chỉ một thành công; phát `DaChuyenGiuong`; người vắng có giữ giường chuyển sang giường mới `DANG_GIU_CHO` với hạn còn lại (FR-032)
- [ ] T030 [P] [US3] Test `BT/m03/FR_029_ChuyenTheoLichIT.java` (đồng hồ giả lập): duyệt yêu cầu giữ giường bằng `TUONG_LAI`; ngày hiệu lực giường chưa Trống → `cho_giuong_san_sang`, chuyển khi xác nhận sẵn sàng; áp dụng không thành → `DA_HUY`
- [ ] T031 [P] [US3] Test `BT/m03/FR_030_ChuyenKhacGiaIT.java`, `FR_027_NhapBuIT.java`, `BR_M03_23_TuongLaiKhongPhuHopIT.java`, `FR_034_PhongKhongPhuHopIT.java`: chuyển gấp khác giá chỉ với lý do y tế/an toàn, đặt dấu chờ lập yêu cầu và nhắc CFG-M03-02; từ chối → dấu chờ về phòng cùng giá; nhập bù tạo yêu cầu `M03.NHAP_BU_PHAN_BO`; mức đổi làm phân bổ tương lai không phù hợp → báo; mức/giới tính đổi → dấu phòng không phù hợp

### Implementation cho User Story 3

- [ ] T032 [US3] Cài `BE/m03/phanbo/internal/ChuyenGiuongService.java`: khóa tuần tự, `KiemTraPhanBo` cho giường mới, đóng phân bổ cũ (`CHUYEN_GIUONG`) và mở phân bổ mới cùng thời điểm, giường cũ `CHO_SAN_SANG`, giường mới `DANG_SU_DUNG` hoặc `DANG_GIU_CHO` (FR-032); lý do chuyển bắt buộc; khác đơn giá (`khacDonGia`) chỉ khi lý do `y_te_an_toan` và đặt `dau_cho_lap_yeu_cau` (research G-07; FR-028 → FR-033)
- [ ] T033 [US3] Cài phần chuyển theo lịch trong `BE/m03/phanbo/internal/ChuyenTheoLich.java` (`kiemTraDatTruocChuyenTheoLich`, `datTruocChuyenTheoLich`, `kiemTraChuyenTheoLich`, `thucHienChuyenTheoLich`, `huyPhanBoTheoLich`, `daLapYeuCauSauChuyenGap`, `ketQuaYeuCauSauChuyenGap`) cài `PhanBoGiuongLenhApi` (FR-029, FR-030)
- [ ] T034 [US3] Tạo `MIG/V003_04__yeu_cau_nhap_bu.sql` (`yeu_cau_phe_duyet_id` khóa chính, `nct_id`, `phan_bo_hien_tai_id`, `giuong_moi_id`, `thoi_diem_chuyen`; không `UPDATE`) và cài handler `BE/m03/phanbo/internal/NhapBuYeuCauHandler.java` cho `M03.NHAP_BU_PHAN_BO` (`quyenTao` = `PHAN_BO_CHUYEN_GIUONG, TAO`; duyệt AC-01; `kiemTraDieuKienApDung` chạy lại FR-018, FR-019, FR-027 (c); `apDung` chuyển giường nhập bù với `nhap_bu`, `thoi_diem_nhap_thuc`) (FR-027)
- [ ] T035 [US3] Cài listener `MucChamSocDaDoi`, `DanhGiaDauVaoDaXacNhan`, `ThongTinCaNhanDaCapNhat`, `LoaiLuuTruDaDoi` trong `BE/m03/phanbo/internal/DanhGiaLaiPhuHop.java`: đặt/gỡ `dau_phong_khong_phu_hop`, báo phân bổ tương lai không phù hợp (BR-M03-23, FR-034)
- [ ] T036 [US3] Bổ sung endpoint `POST /phan-bo-giuong/{id}/chuyen-giuong` (kèm nhập bù) vào `PhanBoController.java`; màn web `frontend/src/tinh-nang/phan-bo-giuong/ChuyenGiuong.jsx`; app `mobile/lib/nhan-vien/chuyen-giuong/` và test `mobile/test/nhan-vien/chuyen_giuong_test.dart`

**Checkpoint**: Chuyển giường đủ biến thể.

**Khởi động Phase 5 (2026-10-24; chỉ phần độc lập, chưa hoàn tất)**: Chủ dự án duyệt dịch riêng operation chuyển giường T036, tiếp tục chọn chờ B cho API/fake/sự kiện M01. Đã viết đỏ rồi cài phần migration của T034 bằng `V003_04__bed_backfill_request.sql`; `FR_027_BackfillSchemaIT` có 5 IT (schema/chỉ mục, PK/FK, bất biến admin/retention, rollback và dọn cùng phân bổ), sau cài cùng 23 ArchTest xanh. Handler T034 chưa cài vì còn cần checker FR-018/lệnh chuyển giường và nguồn khoảng vắng M01. T036 mới dịch `POST /bed-allocations/{id}/transfer`, clone request/response và sinh client; chưa controller/web/mobile. Giữ T029–T036 chưa đánh; không dùng dữ liệu giả thay nguồn M01. Ghi chú và kết quả clean verify nền ở plan/Working-history.md.

---

## Phase 6: User Story 4 – Trạng thái giường chỉ thay đổi qua lệnh và sự kiện; giữ chỗ có lý do và hạn (Priority: P1)

**Goal**: Đủ 17 dòng chuyển giường; giữ chỗ hồ sơ chờ và người vắng; tạm ngừng, sử dụng lại; port cho 019; tham gia lệnh trạng thái của 001 (FR-011 → FR-016a, BR-M03-05, 06, 13, 15).

**Independent Test**: Đi hết mọi chuyển trong bảng trạng thái giường, thử mọi chuyển ngoài bảng; tua đồng hồ qua hạn giữ chỗ.

### Tests cho User Story 4

- [x] T037 [P] [US4] Test đơn vị `BT/m03/BangChuyenTrangThaiGiuongTest.java` tham số hóa mọi dòng của bảng chuyển data-model mục 3 và mọi cặp ngoài bảng
- [ ] T038 [P] [US4] Test `BT/m03/BR_M03_05_GiuongCoNguoiIT.java`, `BR_M03_06_VeTrongIT.java`, `BR_M03_13_GiuongHongDatTruocIT.java`, `BR_M03_15_DauChoChuyenNguoiIT.java`, `TamNgungSuDungIT.java`, `FR_016a_ThucHienTreIT.java`: giường có người chặn lệnh tài sản; hết hạn giữ chỗ hồ sơ chờ về Trống và gọi 004; giường hỏng hủy phân bổ tương lai theo FR-013a; Báo hỏng giường có người ghi dấu "chờ chuyển người"; tạm ngừng có lý do, sử dụng lại về `CHO_SAN_SANG`; việc trễ hơn CFG-M03-08 ghi `thuc_hien_tre` (US4 #1–16)
- [ ] T039 [P] [US4] Test `BT/m03/PhanBoThamGiaChuyenTrangThaiIT.java`: mỗi lệnh của bảng G-06 (Hoàn tất tiếp nhận, Hủy tiếp nhận, Cho tạm vắng, Chuyển viện, Ghi nhận trở về, Kết thúc lưu trú, Ghi nhận qua đời) đổi giường, phân bổ đúng; lỗi thì rollback cả lệnh 001

### Implementation cho User Story 4

- [ ] T040 [US4] Cài giữ chỗ trong `BE/m03/giuong/internal/GiuChoService.java`: `giuChoHoSoCho` (hạn CFG-M02-02), `huyGiuCho` (idempotent), `giaiPhongGiuongKhiVang`, `giaHanKetThucDuKien` của `PhanBoGiuongLenhApi` (FR-012, FR-014, FR-015)
- [x] T041 [US4] Cài lệnh tạm ngừng, sử dụng lại (AC-01; Q-246, Q-261) trong `GiuongService.java` và endpoint `POST /giuong/{id}/tam-ngung-su-dung`, `/su-dung-lai`, `GET /giuong/{id}/lich-su-trang-thai` trong `GiuongController.java`
- [ ] T042 [US4] Cài `BE/m03/giuong/port/GiuongTheoTaiSanPortImpl.java`: `TAI_SAN_BAO_TRI`, `TAI_SAN_THANH_LY`, `TAI_SAN_SAN_SANG`, dấu "chờ chuyển người", ngoại lệ FR-013a, FR-013b; chặn khi giường có người (BR-M03-05, 13, 15)
- [ ] T043 [US4] Cài bean `ThamGiaChuyenTrangThai` của 003 trong `BE/m03/phanbo/internal/PhanBoThamGiaChuyenTrangThai.java` (`@Order` sau 004; `chiTiet.doiPhanBoSangGiuongId` cho Hoàn tất tiếp nhận; giữ/không giữ giường theo `giuGiuongKhiVang`; giải phóng khi kết thúc, qua đời; cảnh báo FR-024a khi trở về phòng chịu chặn) (research G-06)
- [ ] T044 [US4] Cài job `BE/m03/lich/HetHanGiuCho.java` và `BaoQuaBatDauDuKien.java` (ShedLock, 15 phút; chống lặp theo `da_bao_het_han_luc`, `da_bao_qua_bat_dau_cho`; gọi `hoSoChoHetHanGiuCho` cùng transaction) (research G-09)

**Kết quả lượt Phase 6 (2026-10-24; chưa hoàn tất cả phase)**: Chỉ đánh T037/T041 sau `mvnw.cmd clean verify` nền BUILD SUCCESS (02:06), 351 unit/kiến trúc +495 IT, 0 failure/error/skip, Spotless 786 file qua. T037 có 184 ca bảng chuyển; T041 có lệnh tạm ngừng/sử dụng lại và lịch sử, 3 operation được duyệt dịch và ApiContractIT đã kiểm. T040 mới có 4 tác động nội bộ BedHoldService, chưa nối facade API đầy đủ; T044 mới có HoldExpiryJob, chưa BaoQuaBatDauDuKien. T038/T039/T040/T042/T043/T044 giữ chưa đánh: thiếu hợp đồng/fake M01 và checker/lệnh theo lịch của phase trước. Không đổi UI, không chụp màn hình; frontend 113 test, lint/build qua. Xem plan và Working-history.md; task chưa hoàn tất tiếp theo của Phase 6 là T038.
**Checkpoint**: P1 hoàn tất; giường và phân bổ đầy đủ.

---

## Phase 7: User Story 5 – Tra cứu lịch sử vị trí phục vụ truy vết lây nhiễm (Priority: P2)

**Goal**: Vị trí tại thời điểm, lịch sử theo người/giường, danh sách cùng phòng, có áp đính chính (FR-026, FR-035 → FR-037, BR-M03-24).

**Independent Test**: Lịch sử 30 ngày cho 10 người trong 3 phòng; hỏi vị trí tại 5 thời điểm và danh sách cùng phòng của một người trong 5 ngày; so với dữ liệu.

### Tests cho User Story 5

- [ ] T045 [P] [US5] Test `BT/m03/LichSuViTriIT.java`, `CungPhongIT.java`, `TraCuuDinhChinhIT.java`, `BR_M03_24_DinhChinhBao007IT.java`, `QuyenTraCuuIT.java`: kết quả khớp dữ liệu (SC-003 < 1 giây); đính chính phân bổ đổi kết quả tra cứu và báo 007; người ngoài phạm vi bị chặn

### Implementation cho User Story 5

- [ ] T046 [US5] Cài `BE/m03/phanbo/internal/LichSuViTriService.java` (`LichSuViTriApi`) trên `v_phan_bo_hien_hanh` (research G-10)
- [ ] T047 [US5] Cài `DinhChinhDuocProvider` cho `M03.PHAN_BO_GIUONG` và `M03.XAC_NHAN_SAN_SANG` (`tangKhuVuc` là tầng của giường; `sauKhiDinhChinh` báo 007) trong `BE/m03/phanbo/internal/PhanBoDinhChinhProvider.java`
- [ ] T048 [US5] Tạo `BE/m03/phanbo/web/LichSuViTriController.java`: `GET /lich-su-vi-tri/nguoi-cao-tuoi/{nctId}`, `/lich-su-vi-tri/giuong`, `/lich-su-vi-tri/cung-phong`, `POST /phan-bo-giuong/{id}/dinh-chinh`, `POST /giuong/{id}/lich-su-trang-thai/{lichSuId}/dinh-chinh`; màn web `frontend/src/tinh-nang/lich-su-vi-tri/`

**Checkpoint**: Truy vết vị trí chạy độc lập.

---

## Phase 8: User Story 6 – Khu nghỉ bán trú có sức chứa theo buổi (Priority: P2)

**Goal**: Một khu nghỉ Hiệu lực, buổi không chồng khung, sức chứa CFG-M03-01, đếm lại khi đổi khung (FR-038 → FR-042, BR-M03-03, BR-M03-25).

**Independent Test**: CFG-M03-01 = 3; đăng ký 3 người sáng thứ Hai, thử người thứ 4; báo vắng một người rồi đăng ký lại; lịch lặp hằng tuần có một ngày vượt.

### Tests cho User Story 6

- [ ] T049 [P] [US6] Test `BT/m03/BR_M03_03_SucChuaIT.java`, `FR_039_BuoiGiaoIT.java`, `BR_M03_25_DoiKhungGioIT.java`, `KhuNghiDuyNhatIT.java`: vượt sức chứa bị chặn; đổi CFG-M03-01 hoặc khung giờ thì đếm lại và cảnh báo buổi vượt; hai khu nghỉ Hiệu lực bị `UNIQUE` chặn; ngừng hiệu lực khi còn lịch tương lai → 409

### Implementation cho User Story 6

- [ ] T050 [US6] Tạo `MIG/V003_05__ban_tru.sql`: `khu_nghi_ban_tru` (`CHECK ((tang_id IS NULL) <> (khu_vuc_id IS NULL))`, cột sinh `hieu_luc_duy_nhat` `UNIQUE`), `buoi_ban_tru` (`CHECK (gio_ket_thuc > gio_bat_dau)`, `ten_hieu_luc UNIQUE`)
- [ ] T051 [US6] Cài `BE/m03/bantru/internal/BanTruService.java`: cấu hình khu nghỉ, buổi (không chồng khung dưới khóa khu nghỉ), `SucChuaBanTruApi` cho 004, 005, đếm lại qua `DemLichDenBanTru` khi đổi buổi hoặc `ThamSoDaDoi(CFG-M03-01)`, nhật ký `M03_BAN_TRU_CANH_BAO_VUOT` (research G-11)
- [ ] T052 [US6] Tạo `BE/m03/bantru/web/BanTruController.java` (`/khu-nghi-ban-tru`, `/{id}`, `/{id}/ngung-hieu-luc`, `/so-dang-ky`, `/buoi-ban-tru`, `/{id}`, `/{id}/ngung-hieu-luc`) và màn web `frontend/src/tinh-nang/khu-nghi-ban-tru/`

**Checkpoint**: Bán trú có sức chứa.

---

## Phase 9: User Story 7 – Bác sĩ, Quản lý viện đặt và gỡ cách ly phòng (Priority: P2)

**Goal**: Đặt/gỡ cách ly có lý do, chặn phân bổ vào/ra; chuyển theo chỉ định kiểm soát lây nhiễm; xác nhận ở lại vùng cách ly (FR-008 → FR-010, FR-024a, BR-M03-21).

**Independent Test**: Đặt cách ly P105, thử phân bổ, chuyển vào/ra; Trưởng tầng gỡ (bị từ chối) rồi Bác sĩ gỡ.

### Tests cho User Story 7

- [ ] T053 [P] [US7] Test `BT/m03/CachLyPhongIT.java`, `BR_M03_01_CachLyIT.java`, `BR_M03_21_BacSiChiDinhIT.java`, `FR_024a_TroVePhongCachLyIT.java`: chỉ AC-01, AC-03 đặt/gỡ; phòng cách ly và vùng khoanh chặn phân bổ; chuyển kiểm soát lây nhiễm cần bác sĩ chỉ định (`TaiKhoanApi.coVaiTro`); trở về phòng chịu chặn đặt dấu chờ bác sĩ xác nhận, gỡ khi `KhoanhVungDaGo`

### Implementation cho User Story 7

- [ ] T054 [US7] Tạo `MIG/V003_06__lich_su_cach_ly_phong.sql` (`hanh_dong ENUM('DAT','GO')`, `ly_do` bắt buộc, `su_co_id NULL`; trigger chỉ thêm) và cài `BE/m03/cachly/internal/CachLyService.java` (đặt, gỡ, `M03_CACH_LY`, phát `PhongDaDatCachLy`/`PhongDaGoCachLy`, listener `KhoanhVungDaGo`, `DaThemVaoDanhSachTiepXuc`) (research G-08)
- [ ] T055 [US7] Cài chuyển kiểm soát lây nhiễm (`bac_si_chi_dinh_id`, lý do `KIEM_SOAT_LAY_NHIEM`) và xác nhận ở lại vùng cách ly (`bs_xac_nhan_*`) trong `ChuyenGiuongService.java`
- [ ] T056 [US7] Tạo `BE/m03/cachly/web/CachLyController.java` (`POST /phong/{id}/dat-cach-ly`, `/go-cach-ly`, `GET /phong/{id}/lich-su-cach-ly`) và endpoint `POST /phan-bo-giuong/{id}/chuyen-giuong-kiem-soat-lay-nhiem`, `/xac-nhan-o-lai-vung-cach-ly`; màn web `frontend/src/tinh-nang/cach-ly-phong/` và app `mobile/lib/nhan-vien/cach-ly-phong/`

**Checkpoint**: Cách ly chạy độc lập.

---

## Phase 10: User Story 8 – Xác nhận giường sẵn sàng trước khi nhận người mới (Priority: P2)

**Goal**: Giường `CHO_SAN_SANG` chỉ về Trống bằng lệnh xác nhận; khử khuẩn và bảo hộ bắt buộc khi người rời thuộc danh sách; nhắc quá hạn CFG-M03-04 (FR-044 → FR-048, BR-M03-09, BR-M03-10, DBR-26).

**Independent Test**: Kết thúc lưu trú → giường Chờ sẵn sàng; phân bổ bắt đầu ngay bị chặn; quá CFG-M03-04 → nhắc; xác nhận; lặp lại với người thuộc danh sách tiếp xúc.

### Tests cho User Story 8

- [ ] T057 [P] [US8] Test `BT/m03/BR_M03_09_ChoSanSangIT.java`, `BR_M03_10_KhuKhuanIT.java`, `XacNhanSanSangQuyenIT.java` (đồng hồ giả lập): nhắc Trưởng tầng một lần khi quá hạn; người rời thuộc danh sách thì thiếu khử khuẩn/bảo hộ → 422 và `CHECK` chặn; chỉ Trưởng tầng, Người phụ trách ca của tầng xác nhận (research G-14); có chuyển theo lịch đang chờ thì xác nhận chuyển ngay

### Implementation cho User Story 8

- [ ] T058 [US8] Cài `BE/m03/giuong/internal/XacNhanSanSangService.java`: `bat_buoc_khu_khuan = TinhTrangLayNhiemChoPhanBo.thuocDanhSach(nguoi của phan_bo_vua_dong_id)`; `XAC_NHAN_SAN_SANG` → `TRONG` (phát `GiuongDaTrong`) hoặc thực hiện phân bổ `cho_giuong_san_sang` (FR-011a, FR-044, FR-045)
- [ ] T059 [US8] Cài job `BE/m03/lich/NhacGiuongChoSanSang.java` và `NhacChuKyPhanBo.java` (CFG-M03-04, CFG-M03-02; chống lặp `da_nhac_san_sang_luc`, `nhac_phan_bo`; nhật ký `M03_NHAC`) (research G-09)
- [ ] T060 [US8] Tạo endpoint `GET /giuong/{id}/dieu-kien-xac-nhan-san-sang`, `POST /giuong/{id}/xac-nhan-san-sang` trong `GiuongController.java`; app `mobile/lib/nhan-vien/bang-giuong-tang/` và test `mobile/test/nhan-vien/xac_nhan_san_sang_test.dart` (ô khử khuẩn bắt buộc khi `canKhuKhuan`)

**Checkpoint**: Mọi user story của 003 chạy độc lập.

---

## Phase 11: Polish & Cross-Cutting

- [ ] T061 [P] Cài `TinhTrangGiuongApi`, `GiuongThongKeApi` (016), `CanhBaoBanGiaoApi` (008) trong `BE/m03/giuong/internal/`
- [ ] T062 [P] Mở rộng `BT/common/HopDongApiTest.java` với `specs/003-room-bed-allocation/contracts/api-003.yaml`; mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 003
- [ ] T063 Khi 004, 007, 008, 019 có bản thật, chạy lại test của US2, US4, US7, US8 với bean thật thay T002
- [ ] T064 Chạy `./mvnw verify`, `npm test`, `flutter test` và kịch bản quickstart.md mục 3; đo SC-005 (danh sách giường < 1 giây) theo quickstart mục 5
- [ ] T065 Rà truy vết data-model mục 11: DBR-09, 10, 23, 25, 26, 32 (phần 003) và BR-M03-01 → 07, 09, 10, 13, 15, 19 → 25 có test; FR chung đã làm trong các task khác mà task chưa ghi số (quyền, nhật ký, thông báo, bảng giao tiếp, số liệu cung cấp…): FR-024, FR-025, FR-051 (bổ sung 2026-10-10) — mỗi FR có task hoặc test tương ứng (constitution XV)

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
