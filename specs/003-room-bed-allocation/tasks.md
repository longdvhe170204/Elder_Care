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

- [ ] T001 Tạo khung gói `BE/m03/{api,cocau,giuong,phanbo,cachly,bantru,lich}/` với `package-info.java`; gói `BE/m03/giuong/port/` là cổng duy nhất cho `m03.taisan` (019) gọi vào (research G-01)
- [ ] T002 [P] Tạo bản giả SPI 003 khai báo (contracts mục 2.1) trong `BT/support/gia003/`: `NguonLuuTruChoPhanBo`, `DemLichDenBanTru` (004), `TinhTrangLayNhiemChoPhanBo` (007), `ThamChieuViTri` (008), và bản giả `TaiSanGiuongPort` (019) (research G-16)

---

## Phase 2: Foundational (chặn mọi user story)

**Purpose**: Khai báo `m03.api`, SPI cho module khác cài, sự kiện, bảng cơ cấu và giường mà mọi story dùng.

**⚠️ CRITICAL**: Xong phase này mới làm user story.

- [ ] T003 [P] Tạo giao diện công khai `BE/m03/api/`: `PhanBoGiuongLenhApi` (mọi hàm MANDATORY), `ViTriApi`, `LichSuViTriApi`, `SucChuaBanTruApi`, `CoCauViTriApi`, `PhongChoPhepApi`, `TinhTrangGiuongApi`, `GiuongThongKeApi` (016), `CanhBaoBanGiaoApi` (008) đúng contracts/module-interfaces.md mục 1.2 → 1.9
- [ ] T004 [P] Khai báo SPI ở `BE/m03/api/`: `NguonLuuTruChoPhanBo`, `DemLichDenBanTru`, `TinhTrangLayNhiemChoPhanBo`, `ThamChieuViTri`; port `BE/m03/giuong/port/GiuongTheoTaiSanPort.java` và `TaiSanGiuongPort` (contracts mục 2.1; research G-02)
- [ ] T005 [P] Tạo sự kiện `BE/m03/api/sukien/` theo contracts mục 1.11 (`DaChuyenGiuong`, `GiuongDaTrong`, `PhongDaDatCachLy`, `PhongDaGoCachLy`, các sự kiện nhắc, báo); sự kiện gửi 009 cài `CoYeuCauThongBao` (research G-12)
- [ ] T006 Tạo `MIG/V003_01__co_cau.sql`: `khu_vuc` (`ma VARCHAR(20) UNIQUE` kể cả mục Ngừng hiệu lực), `tang` (`UNIQUE (khu_vuc_id, ma)`, `ten_toa_nha NULL`), `loai_phong` (`so_giuong_tieu_chuan SMALLINT > 0`, `CHECK (phuc_vu_dai_han OR phuc_vu_ngan_ngay)`, `ten_hieu_luc UNIQUE`), `loai_phong_muc`, `phong` (`ma VARCHAR(20) UNIQUE`, `chinh_sach_gioi_tinh ENUM('NAM','NU','KHONG_GIOI_HAN')`, `dang_cach_ly BOOLEAN` mặc định `false`), `phong_muc_cho_phep`, `ly_do_chuyen_giuong` (`ma VARCHAR(30) UNIQUE`, `y_te_an_toan`, `co_dinh`; nạp dòng `KIEM_SOAT_LAY_NHIEM` cố định) theo data-model mục 2; mã mức `ENUM('CO_BAN','THUONG_XUYEN','DAC_BIET')` không khóa ngoại sang M01
- [ ] T007 Tạo `MIG/V003_02__giuong.sql`: `giuong` theo data-model mục 3 (`UNIQUE (phong_id, ma)`; `trang_thai ENUM('TRONG','DANG_SU_DUNG','DANG_GIU_CHO','CHO_SAN_SANG','DANG_BAO_TRI','TAM_NGUNG','KHONG_SU_DUNG')`; `ly_do_giu_cho ENUM('NGUOI_VANG','HO_SO_CHO') NULL`; năm `CHECK` của mục 3; `phan_bo_vua_dong_id`; `ly_do_tam_ngung VARCHAR(2000) NULL`) và `lich_su_trang_thai_giuong` (nhóm 3 chỉ thêm; `thoi_diem_du_kien`, `thuc_hien_tre`, `da_khu_khuan`, `da_dung_bao_ho`, `bat_buoc_khu_khuan`, `tang_khu_vuc_id`; `CHECK (su_kien <> 'XAC_NHAN_SAN_SANG' OR bat_buoc_khu_khuan = false OR (da_khu_khuan AND da_dung_bao_ho))`; hai trigger)
- [ ] T008 Cài `BE/m03/giuong/internal/GiuongTrangThaiService.java`: một điểm ghi trạng thái giường với bảng chuyển data-model mục 3 cài trong mã; khóa giường theo `id`; mỗi lần chuyển thêm một dòng lịch sử và nhật ký `M03_GIUONG_CHUYEN`; tính `thuc_hien_tre` theo CFG-M03-08 cho việc của Bộ lập lịch (research G-04; FR-011 → FR-016a)
- [ ] T009 [P] Test kiến trúc `BT/m03/ChiMotNoiGhiTrangThaiArchTest.java`: chỉ `GiuongTrangThaiService` ghi `giuong.trang_thai`
- [ ] T010 [P] Cài provider cho 000/002 trong `BE/m03/phanbo/internal/`: `PhanLoaiTruongProvider` (`lyDo` cách ly và chuyển kiểm soát lây nhiễm → `SUC_KHOE`), `GanPhamViProvider` (`M03.GIUONG`, `M03.PHONG`, `M03.PHAN_BO_GIUONG`), `LuuTruProvider` theo data-model mục 10 (research G-14, G-15)

**Checkpoint**: Bảng cơ cấu, giường và điểm ghi trạng thái sẵn sàng.

---

## Phase 3: User Story 1 – Quản lý viện dựng cơ cấu khu vực, tầng, phòng, giường (Priority: P1) 🎯 MVP

**Goal**: Cơ cấu bốn cấp, mã duy nhất, chính sách phòng, ngừng hiệu lực có chặn, tạo giường ghi tăng tài sản (FR-001 → FR-007, BR-M03-22, DBR-32 phần 003).

**Independent Test**: 1 khu vực, 2 tầng, mỗi tầng 2 phòng khác chính sách, mỗi phòng 2–4 giường; thử mã trùng, xóa phòng đã từng có phân bổ, sửa bằng Trưởng tầng.

### Tests cho User Story 1

- [ ] T011 [P] [US1] Test `BT/m03/CoCauIT.java`, `MaDuyNhatIT.java`, `NgungHieuLucCoCauIT.java`: mã trùng → 409; Trưởng tầng sửa → 403; ngừng hiệu lực phòng còn giường không `KHONG_SU_DUNG` (kể cả `TAM_NGUNG`) → 409 liệt kê; `ThamChieuViTri` còn phân công → 409 (US1 #1–8; FR-003, FR-005)
- [ ] T012 [P] [US1] Test `BT/m03/FR_006_DoiChinhSachPhongIT.java`, `FR_007_KhongDoiChaIT.java`: đổi giới tính/mức của phòng vi phạm phân bổ hiện tại → 409 liệt kê người; đổi tầng của phòng hoặc phòng của giường đã có phân bổ → 409
- [ ] T013 [P] [US1] Test `BT/m03/BR_M03_22_SoGiuongTieuChuanIT.java`, `Q_238_TaoGiuongGhiTangIT.java`: vượt số giường tiêu chuẩn → 201 kèm cảnh báo `VUOT_SO_GIUONG_TIEU_CHUAN`; tạo giường gọi `TaiSanGiuongPort.ghiTangChoGiuong` cùng transaction, lỗi thì rollback

### Implementation cho User Story 1

- [ ] T014 [P] [US1] Tạo entity `BE/m03/cocau/internal/{KhuVuc,Tang,LoaiPhong,Phong,LyDoChuyenGiuong}.java` (`@NhomDuLieu(DANH_MUC)`) và `BE/m03/giuong/internal/Giuong.java`
- [ ] T015 [US1] Cài `BE/m03/cocau/internal/CoCauService.java`: tạo, sửa, ngừng hiệu lực, kích hoạt lại, xóa (chỉ khi chưa từng tham chiếu, kể cả `ThamChieuViTri.daTungThamChieu`) cho khu vực, tầng, loại phòng, phòng, lý do chuyển; sửa chính sách phòng khóa giường và kiểm (a), (c); nhật ký `M03_CO_CAU_*` (FR-001 → FR-007)
- [ ] T016 [US1] Cài tạo giường (`TAO_GIUONG` → `TRONG`, gọi ghi tăng tài sản 019, cảnh báo BR-M03-22) trong `BE/m03/giuong/internal/GiuongService.java`; giường không có lệnh xóa (Q-237)
- [ ] T017 [US1] Tạo controller `BE/m03/cocau/web/CoCauController.java` và `BE/m03/giuong/web/GiuongController.java` cho `/khu-vuc`, `/tang`, `/loai-phong`, `/phong`, `/ly-do-chuyen-giuong` (kèm `/{id}`, `/ngung-hieu-luc`, `/kich-hoat-lai`), `POST/GET /giuong`, `GET /giuong/{id}` theo contracts/api-003.yaml; cài `CoCauViTriApi`, `PhongChoPhepApi`
- [ ] T018 [P] [US1] Tạo màn web `frontend/src/tinh-nang/co-cau-phong-giuong/` (cây khu vực → tầng → phòng → giường, loại phòng, lý do chuyển)

**Checkpoint**: Cơ cấu dựng được độc lập.

---

## Phase 4: User Story 2 – Hành chính phân bổ giường; không bao giờ để một giường có hai người (Priority: P1)

**Goal**: `KiemTraPhanBo` dùng chung cho danh sách và lệnh; phân bổ ban đầu `TUONG_LAI`; phân bổ cho người chưa có giường; khóa tuần tự và ràng buộc CSDL chống trùng (FR-017 → FR-023, FR-025a, DBR-09, DBR-10, BR-M03-01, 02, 19, 20).

**Independent Test**: Phòng nữ chỉ cơ bản, phòng cách ly, giường bảo trì; thử phân bổ từng người vi phạm từng điều kiện; hai tài khoản phân bổ cùng giường gần đồng thời.

### Tests cho User Story 2

- [ ] T019 [P] [US2] Test `BT/m03/BR_M03_01_DieuKienPhanBoIT.java`: mức (hiện hành → hợp đồng → lượt đăng ký, Q-301), loại hình, giới tính, cách ly/khoanh vùng, trạng thái giường, giữ chỗ, dấu tài sản; trả đủ điều kiện chưa thỏa (DBR-10, BR-M03-01)
- [ ] T020 [P] [US2] Test `BT/m03/BR_M03_02_HaiNguoiCungGiuongIT.java`, `DBR_09_KhongChongIT.java`, `FR_020_PhanBoDongThoiIT.java`: 10 luồng trên một giường chỉ một thành công (SC-001); `INSERT` trực tiếp chồng khoảng bị trigger chặn; cột sinh `UNIQUE` chặn hai đang hiệu lực
- [ ] T021 [P] [US2] Test `BT/m03/FR_023_PhanBoBanDauIT.java`, `BR_M03_20_PhamViPhanBoBanDauIT.java`, `FR_022_DanhSachGiuongIT.java`, `BR_M03_19_TroVeChuaCoGiuongIT.java`: người Đang tiếp nhận chỉ có `TUONG_LAI`; phạm vi theo giường; danh sách giường phù hợp không chọn sẵn và khớp lệnh; người trở về không có giường mang dấu "chưa có giường" và được nhắc (US2 #1–15)
- [ ] T022 [P] [US2] Test `BT/m03/PhanBoRangBuocDbIT.java`: mọi cột sinh, trigger, `CHECK` của `phan_bo_giuong` (tích hợp MySQL)

### Implementation cho User Story 2

- [ ] T023 [US2] Tạo `MIG/V003_03__phan_bo_giuong.sql`: `phan_bo_giuong` theo data-model mục 4 (`trang_thai ENUM('TUONG_LAI','DANG_HIEU_LUC','DA_DONG','DA_HUY')`; cột sinh `ket_thuc_hieu_dung = COALESCE(ket_thuc, ket_thuc_du_kien, '9999-12-31')`; cột sinh `giuong_dang_hieu_luc`, `nct_dang_hieu_luc`, `nct_tuong_lai` có `UNIQUE`; năm `CHECK` của mục 4; chỉ mục `(giuong_id, bat_dau)`, `(nct_id, bat_dau)`, `(trang_thai, bat_dau)`; trigger `trg_phan_bo_giuong_khong_chong`, `_khong_sua` khi `DA_DONG`/`DA_HUY`, `_khong_xoa`), `vi_tri_nct` (`nct_id` khóa chính, `chua_co_giuong_tu`, `phien_ban`), view `v_phan_bo_hien_hanh` (áp `v_dinh_chinh_moi_nhat`, loại `v_ban_ghi_da_huy`), `nhac_phan_bo` (khóa chính `(loai_doi_tuong, doi_tuong_id, loai_dau)`)
- [ ] T024 [P] [US2] Tạo entity `BE/m03/phanbo/internal/PhanBoGiuong.java`, `ViTriNct.java`, `NhacPhanBo.java`
- [ ] T025 [US2] Cài `BE/m03/phanbo/internal/KiemTraPhanBo.java` (FR-018 (a)–(g), dùng `NguonLuuTruChoPhanBo`, `TinhTrangLayNhiemChoPhanBo`, dấu tài sản) và `KhoaTuanTu.java` (khóa người cao tuổi khi qua 001 → `vi_tri_nct` → giường theo `id`; `@Retryable` khi deadlock) (research G-03, G-05)
- [ ] T026 [US2] Cài `BE/m03/phanbo/internal/PhanBoService.java`: phân bổ cho người Đang tiếp nhận (`TUONG_LAI`, giường `CHO_SAN_SANG` theo FR-011a, gọi `daPhanBo` kết thúc giữ chỗ hồ sơ chờ), phân bổ cho người chưa có giường (`DANG_HIEU_LUC`, gỡ dấu), hủy phân bổ tương lai có lý do; nhật ký `M03_PB_*`; cài `PhanBoGiuongApi` (002), `ViTriApi` (FR-017 → FR-023, FR-025a)
- [ ] T027 [US2] Tạo `BE/m03/phanbo/web/PhanBoController.java`: `GET /phan-bo-giuong/giuong-phu-hop`, `POST/GET /phan-bo-giuong`, `GET /phan-bo-giuong/{id}`, `POST …/{id}/huy`, `GET /nguoi-cao-tuoi/{nctId}/vi-tri`, `GET /giuong/tong-hop`
- [ ] T028 [P] [US2] Tạo màn web `frontend/src/tinh-nang/phan-bo-giuong/ChonGiuong.jsx` (không chọn sẵn), `frontend/src/tinh-nang/bang-giuong/BangGiuong.jsx` và test `ChonGiuong.test.jsx`, `BangGiuong.test.jsx`, `DieuKienChuaThoa.test.jsx`

**Checkpoint**: Phân bổ an toàn đồng thời.

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

---

## Phase 6: User Story 4 – Trạng thái giường chỉ thay đổi qua lệnh và sự kiện; giữ chỗ có lý do và hạn (Priority: P1)

**Goal**: Đủ 17 dòng chuyển giường; giữ chỗ hồ sơ chờ và người vắng; tạm ngừng, sử dụng lại; port cho 019; tham gia lệnh trạng thái của 001 (FR-011 → FR-016a, BR-M03-05, 06, 13, 15).

**Independent Test**: Đi hết mọi chuyển trong bảng trạng thái giường, thử mọi chuyển ngoài bảng; tua đồng hồ qua hạn giữ chỗ.

### Tests cho User Story 4

- [ ] T037 [P] [US4] Test đơn vị `BT/m03/BangChuyenTrangThaiGiuongTest.java` tham số hóa mọi dòng của bảng chuyển data-model mục 3 và mọi cặp ngoài bảng
- [ ] T038 [P] [US4] Test `BT/m03/BR_M03_05_GiuongCoNguoiIT.java`, `BR_M03_06_VeTrongIT.java`, `BR_M03_13_GiuongHongDatTruocIT.java`, `BR_M03_15_DauChoChuyenNguoiIT.java`, `TamNgungSuDungIT.java`, `FR_016a_ThucHienTreIT.java`: giường có người chặn lệnh tài sản; hết hạn giữ chỗ hồ sơ chờ về Trống và gọi 004; giường hỏng hủy phân bổ tương lai theo FR-013a; Báo hỏng giường có người ghi dấu "chờ chuyển người"; tạm ngừng có lý do, sử dụng lại về `CHO_SAN_SANG`; việc trễ hơn CFG-M03-08 ghi `thuc_hien_tre` (US4 #1–16)
- [ ] T039 [P] [US4] Test `BT/m03/PhanBoThamGiaChuyenTrangThaiIT.java`: mỗi lệnh của bảng G-06 (Hoàn tất tiếp nhận, Hủy tiếp nhận, Cho tạm vắng, Chuyển viện, Ghi nhận trở về, Kết thúc lưu trú, Ghi nhận qua đời) đổi giường, phân bổ đúng; lỗi thì rollback cả lệnh 001

### Implementation cho User Story 4

- [ ] T040 [US4] Cài giữ chỗ trong `BE/m03/giuong/internal/GiuChoService.java`: `giuChoHoSoCho` (hạn CFG-M02-02), `huyGiuCho` (idempotent), `giaiPhongGiuongKhiVang`, `giaHanKetThucDuKien` của `PhanBoGiuongLenhApi` (FR-012, FR-014, FR-015)
- [ ] T041 [US4] Cài lệnh tạm ngừng, sử dụng lại (AC-01; Q-246, Q-261) trong `GiuongService.java` và endpoint `POST /giuong/{id}/tam-ngung-su-dung`, `/su-dung-lai`, `GET /giuong/{id}/lich-su-trang-thai` trong `GiuongController.java`
- [ ] T042 [US4] Cài `BE/m03/giuong/port/GiuongTheoTaiSanPortImpl.java`: `TAI_SAN_BAO_TRI`, `TAI_SAN_THANH_LY`, `TAI_SAN_SAN_SANG`, dấu "chờ chuyển người", ngoại lệ FR-013a, FR-013b; chặn khi giường có người (BR-M03-05, 13, 15)
- [ ] T043 [US4] Cài bean `ThamGiaChuyenTrangThai` của 003 trong `BE/m03/phanbo/internal/PhanBoThamGiaChuyenTrangThai.java` (`@Order` sau 004; `chiTiet.doiPhanBoSangGiuongId` cho Hoàn tất tiếp nhận; giữ/không giữ giường theo `giuGiuongKhiVang`; giải phóng khi kết thúc, qua đời; cảnh báo FR-024a khi trở về phòng chịu chặn) (research G-06)
- [ ] T044 [US4] Cài job `BE/m03/lich/HetHanGiuCho.java` và `BaoQuaBatDauDuKien.java` (ShedLock, 15 phút; chống lặp theo `da_bao_het_han_luc`, `da_bao_qua_bat_dau_cho`; gọi `hoSoChoHetHanGiuCho` cùng transaction) (research G-09)

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
