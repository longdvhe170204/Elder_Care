# Research: Quản lý hoạt động và chất lượng chăm sóc (014)

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Quyết định nền R-01 → R-14 của 000 và các mẫu đã chốt ở 001 → 013 (H-02 bean trạng thái và lệnh hệ thống của 001, C-02 SPI của 005, S-09 yêu cầu cảnh báo, B-02 khuôn thông báo, P-04 sự kiện tính phí) được dùng lại, không quyết lại. Các quyết định dưới đây là kỹ thuật; chúng không đổi nghiệp vụ (constitution IX). Chỗ spec chưa rõ hoặc mâu thuẫn được ghi ở plan, mục "Điểm cần báo lại".

---

## L-01. Vị trí trong backend

**Quyết định**: Feature 014 là phần 8.8 → 8.10 và BR-M04-23 của module M04, thêm gói con vào `com.cambae.backend.m04` (plan 005, Structure Decision).

| Gói con | Nội dung | Mục spec |
| --- | --- | --- |
| `m04.hoatdong` | Loại hoạt động, hoạt động, mẫu lặp, người tham gia thường xuyên | A, B |
| `m04.buoi` | Buổi trong viện, tạo từ mẫu, dời, hủy, đăng ký, đồng ý về muộn, điểm danh | C, D, F |
| `m04.chuyendi` | Chuyến đi: phân công, đánh giá khả năng, rời viện, gia hạn, về, thiếu người | G, H |
| `m04.tinhthan` | Sở thích, hồ sơ tinh thần, chuỗi cô lập | I |
| `m04.kiemtra` | Danh sách kiểm tra chất lượng, mục, kết quả | J |
| `m04.hoatdong.lich`, `m04.api` (bổ sung) | Job; giao diện công khai và bản cài SPI | |

Gói của 014 dùng dịch vụ nội bộ của 005 trong cùng `m04` (công việc "hoạt động", công việc làm lại, có mặt bán trú) qua lớp `internal` được ArchUnit cho phép giữa các gói của M04, không qua `m04.api`.

---

## L-02. Chiều phụ thuộc giữa module

**Quyết định**: M04 phụ thuộc 000 → 003 (đã có). 014 thêm phụ thuộc gì ra ngoài M04 thì chỉ được tới module **không** phụ thuộc M04; với module phụ thuộc M04 (006, 007, 008, 010, 011, 012, 015, 016, 019), 014 dùng SPI đã khai ở module đó hoặc khai SPI mới ở `m04.api`.

| Cặp | Cách nối |
| --- | --- |
| 014 → 001 | `TrangThaiNguoiCaoTuoiApi` với lệnh hệ thống `DIEM_DANH_ROI_VIEN`, `DIEM_DANH_VE`, `HUY_GHI_NHAN_ROI_VIEN` (`doHeThong = true`), `chiTiet.chuyenId` (001 mục 1.2; 006 đọc `chiTiet.chuyenId`) |
| 014 → 002, 003, 004 | `QuyenApi`, `PhamViApi`; vị trí, khu vực; lịch đến bán trú (`HopDongDocApi`) |
| 014 ↔ 006 | 006 → M04. 014 cài `ChuyenDiThuocPort` (`m07.api`); gọi `MangThuocChuyenDiApi` (`daChuanBi`, `chuyenThayDoi`, `tinhLaiKhoang` MANDATORY) |
| 014 ↔ 007 | 007 → M04. 014 cài `DiemDanhTruyVetPort` (`m05.api`); gọi `SuCoLenhApi.ghiNhan`, `themDienBien` (MANDATORY); phát `m05.api.YeuCauTaoCanhBao` (`THIEU_NGUOI_KHI_VE` Khẩn cấp, `NGUY_CO_CO_LAP` Nhẹ) và `m05.api.YeuCauGhiDienBienCanhBao` (bổ sung theo plan này, L-11); đọc `KhoanhVungApi`, `SuCoApi.dau` (nghi nhiễm); nghe `KhoanhVungDaTao`, `DauNghiNhiemDaDoi` (bổ sung theo plan này) |
| 014 ↔ 008 | 008 → M04. 014 cài `ChuyenDiNhiemVuPort` (`m09.api`); đọc `CaTrucApi` (ca chồng chuyến, người kiểm tra, mốc kết ca) |
| 014 → 010, 011, 012 | Phát `m11.api.SuKienTinhPhiNguon*` (`DIEM_DANH_HOAT_DONG`); cài `ChuyenDiBuaAnPort`, phát `m08.api.ChuyenDiDaDoi`; cài `HoatDongBanTinPort` (`m10.api`), đọc `QuyenNguoiThanApi` |
| 014 ↔ 015, 016, 019 | Họ làm plan sau. 014 khai ở `m04.api`: `ChuyenDiNhiemVuLenhApi` (015 gọi khi đổi ca, nghỉ đột xuất), sự kiện `ChuyenDiThayDoi` (016), `HoatDongThongKeApi` (016). Lịch xe: 014 gọi `m03.api.LichXeApi`, `LichXeLenhApi` (MANDATORY) do 019 cung cấp trong M03 (M04 → M03 hợp lệ; sửa theo plan 019) |

**Lý do**: Modulith `verify()` từ chối vòng (000 R-02). 005 đã khai sẵn các SPI cho 014 trong cùng M04 (`NguonCongViecPort` loại `BUOI_HOAT_DONG`, `LAM_LAI`; `KetQuaKiemTraPort`; `ThoiKhoaBieuNguonPort`), 014 cài như bean nội bộ.

---

## L-03. Khóa và đồng thời

**Quyết định**:
- **Thứ tự khóa** (nối chuỗi của 005): `nguoi_cao_tuoi` (001) → … → `cham_soc_nct` (005) → `buoi_hoat_dong` / `chuyen_di` → `dang_ky_buoi` (theo `id` tăng) → `hoat_dong_nct` (014, một dòng mỗi người cao tuổi, cho sở thích và chuỗi cô lập). *(R-15: `cham_soc_nct` → `trang_thai_nct`; `chuyen_di` → `buoi_hoat_dong`)*
- **Sức chứa buổi** (FR-015 (a)): khóa `buoi_hoat_dong` `FOR UPDATE`, đếm đăng ký còn hiệu lực trong khóa.
- **Chồng thời gian** (FR-016): kiểm trong khóa `cham_soc_nct` của người cao tuổi. *(R-15: `cham_soc_nct` → `trang_thai_nct`)*
- **Buổi duy nhất** (FR-007): `UNIQUE (hoat_dong_id, bat_dau)` trên buổi, kể cả buổi Đã hủy (buổi hủy thủ công không tạo lại).
- **Danh sách kiểm tra** (FR-059): `UNIQUE (ca_id, tang_khu_vuc_id)`; mỗi công việc tối đa một mục: `UNIQUE (cong_viec_id)` trên mục.
- **Sự cố thiếu người mở** (FR-048): cột `su_co_thieu_nguoi_id` trên người tham gia đặt một lần; lệnh thứ hai bị chặn (sau khi sự cố đóng, 007 cho ghi mới qua lệnh điểm danh về muộn rồi đi lại — không có trong spec; giữ một lần cho mỗi lượt đi).

---

## L-04. Hoạt động, mẫu lặp, tạo buổi từ mẫu (mục A, B; BR-M04-21, Q-306)

**Quyết định**:
- `loai_hoat_dong`, `hoat_dong`, `mau_lap` (nhóm 1; mẫu có phiên bản theo ngày áp dụng: `mau_lap_phien_ban`), `nguoi_thuong_xuyen` (nhóm 2 có lịch sử). *(R-15: `loai_hoat_dong`, `hoat_dong` → `danh_muc_chung`; `mau_lap_phien_ban`, `nguoi_thuong_xuyen` → `ho_so_chung`)*
- Hàm thuần `BuoiTheoMau(mauPhienBan, tuNgay, denNgay)` trả danh sách thời điểm bắt đầu; lệnh "Tạo buổi từ mẫu" (Trưởng tầng, khoảng ngày tự chọn, ngày đầu ≥ hôm nay) tạo các thời điểm chưa có buổi (kể cả buổi Đã hủy chặn tạo lại) và đăng ký sẵn người thường xuyên đạt `DieuKienDangKy` (L-05); người không đạt gộp một thông báo cho Trưởng tầng. Không có job tạo buổi.
- **Đổi mẫu** (FR-009): phiên bản mới từ ngày áp dụng; trong cùng lệnh hủy buổi chưa diễn ra, chưa điểm danh, từ ngày áp dụng mà `BuoiTheoMau(moi)` không chứa; giữ buổi trùng thời điểm; báo Trưởng tầng tạo lại.
- **Ngừng hoạt động** (FR-004): hủy buổi chưa điểm danh từ ngày ngừng.
- **Người thường xuyên** (FR-010): thêm → đăng ký vào buổi đã tạo chưa diễn ra; bỏ → hủy đăng ký nguồn "thường xuyên" chưa diễn ra.

---

## L-05. Đăng ký, đồng ý về muộn (mục D; BR-M04-15, BR-M05-11, Q-165, Q-167, Q-176, Q-287)

**Quyết định**:
- Hàm thuần `DieuKienDangKy(buoi, nct, dangKyKhac, lichBanTru, vung, dauNghiNhiem, dongYVeMuon, t)` trả lỗi chặn (FR-015 (a), (c) → (g), FR-016) và cảnh báo không chặn (FR-018, FR-052). Dữ liệu: `KhoanhVungApi.dangChiuTheoNguoi` và theo phòng của địa điểm; `SuCoApi.dau(nctId, t).nghiNhiem`; `CoMatBanTruApi`, `HopDongDocApi.lichDenTaiNgay`; lưu ý chăm sóc từ `DanhGiaApi` (001); sở thích "không thích" (sửa 2026-10-18, Q-363: cảnh báo liệt kê từng mục "không thích" hiện hành kèm nguồn, kể cả khi mục của chính người cao tuổi là "thích").
- Cảnh báo: lệnh không có `xacNhanCanhBao = true` → 409 `CAN_XAC_NHAN_CANH_BAO` kèm danh sách; có → lưu dấu và danh sách đã hiển thị. Thông tin sức khỏe chi tiết qua giới hạn trường của 002.
- `dang_ky_buoi` (nhóm 2): `DA_DANG_KY | DA_HUY | DA_DIEM_DANH`, nguồn (`THUONG_XUYEN`, `THEM_TAY`, `THEM_LUC_DIEM_DANH`), cảnh báo đã xác nhận. Đăng ký mới → bean `NguonCongViecPort` (`BUOI_HOAT_DONG`) cho 005 lập công việc "hoạt động" (005 tự lập theo kế hoạch; 014 chỉ trả căn cứ); hủy → `NguonCongViecApi.nguonBiHuy(BUOI_HOAT_DONG, dangKyId, …)` (MANDATORY).
- **Đồng ý về muộn** (FR-016a): `dong_y_ve_muon` (nhóm 2) `HIEU_LUC | DA_RUT | DA_DUNG | HET_HIEU_LUC`; ghi bởi Hành chính (bản ký) hoặc người đại diện qua REST cổng của 014 (`QuyenNguoiThanApi.laNguoiDaiDien`). Khi đăng ký được lưu: `CoMatBanTruApi.doiGioVe(nct, ngay, gioKetThuc hoặc gioVeDuKien, dangKyId)` (MANDATORY); gia hạn chuyến → gọi lại với giờ mới; rút hoặc hủy → `traGioVe` (MANDATORY). Job theo phút: tới giờ bắt đầu → `DA_DUNG`. *(R-15: `dong_y_ve_muon` → `ho_so_chung`)*
- **Hủy tự động**: listener `KhoanhVungDaTao` (FR-021), dấu nghi nhiễm qua `DauNghiNhiemDaDoi` (FR-021a), trạng thái cuối qua bean `ThamGiaChuyenTrangThai` (FR-022, cùng transaction lệnh của 001).

---

## L-06. Điểm danh buổi trong viện (mục F; BR-M04-18, Q-169, Q-173)

**Quyết định**:
- `diem_danh_buoi` (nhóm 3): đăng ký, kết quả (`CO_MAT`, `VANG`, `KHONG_GHI_NHAN`), lý do vắng, mức tham gia, mức giao tiếp, tình trạng sau, ghi chú, `ghi_nhan_muon`, người, thời điểm, thời điểm thiết bị. Ghi ngoại tuyến qua `NgoaiTuyenHandler` `M04.DIEM_DANH_BUOI` (Q-01), so theo thời điểm thiết bị (DBR-25). *(R-15: `diem_danh_buoi` → `su_kien_chung`)*
- Hiển thị sẵn "Vắng – đang vắng mặt / không đến" từ trạng thái người cao tuổi và `CoMatBanTruApi`.
- Hoàn tất điểm danh khi mọi đăng ký còn hiệu lực có kết quả → buổi `DA_DIEM_DANH`.
- **Sự kiện tính phí** (FR-032): mỗi lượt Có mặt ở hoạt động có thu phí → `SuKienTinhPhiNguon(DIEM_DANH_HOAT_DONG, nguonId = diemDanhId, donVi = LUOT, dichVuId của hoạt động, ngày của buổi)`; đính chính → `SuKienTinhPhiNguonDaDoi` hoặc sự kiện mới (FR-031). Không tạo khoản (Q-306).
- **Hết ngày** (FR-014a): job 00:05 cho buổi của ngày trước còn `DA_LEN_LICH`: gán `KHONG_GHI_NHAN`, chuyển `DA_DIEM_DANH` (dấu "điểm danh không đủ") hoặc `KHONG_DIEM_DANH`, báo. Nhắc CFG-M04-06 bằng job theo phút.
- **Đính chính** (FR-031): `DinhChinhDuocProvider` `M04.DIEM_DANH_BUOI` (gắn tầng của buổi; buổi toàn viện gắn tầng của người cao tuổi).
- Bản cài `DiemDanhTruyVetPort.cungThamGia` (007 FR-060 (b)) và `HoatDongBanTinPort.soHoatDongDaThamGia` (012): số lượt Có mặt (gồm chuyến đi theo điểm danh rời viện).

---

## L-07. Chuyến đi: chuẩn bị (mục G; BR-M04-16, Q-161, Q-171, Q-172, Q-214, Q-220)

**Quyết định**:
- Chuyến là `buoi_hoat_dong` loại ngoài viện với bảng phụ `chuyen_di` (điểm đến, giờ rời, giờ về dự kiến, phương tiện, `xe_cua_vien`, trạng thái riêng `DA_LEN_LICH | DANG_DI | DA_VE | DA_HUY`, dấu "quá giờ về", "trưởng đoàn tạm từ xa"), `chuyen_di_gia_han` (nhóm 3), `nhiem_vu_chuyen` (trưởng đoàn, người đi cùng; lịch sử), `nguoi_tham_gia` (trạng thái theo bảng người tham gia). *(R-15: `chuyen_di` → `buoi_hoat_dong`; `nguoi_tham_gia` → `dang_ky_buoi`)*
- **Phân công** (FR-035): trưởng đoàn có vai trò Trưởng tầng/Nhân viên chăm sóc/Điều dưỡng; mọi người phân công có tài khoản Hoạt động và tên trong ca chồng thời gian (`CaTrucApi.dangTrongCa` theo khoảng). Phạm vi dữ liệu của họ do 002 tính qua `ChuyenDiNhiemVuPort` (008 → 002 `NhiemVuApi`).
- **Đánh giá khả năng** (FR-036): chỉ Trưởng tầng của tầng người cao tuổi; màn hiển thị lưu ý chăm sóc, nghi nhiễm, cảnh báo/sự cố mở, liều trong khoảng đi (006 qua `ChuyenDiThuocPort.danhGiaDat` do chính 014 cài trả khoảng, 006 tự tính liều). Đạt → `DUOC_DI`, thông báo điều dưỡng phụ trách chuẩn bị thuốc.
- **Dời, hủy** (FR-038a): dời cùng ngày giữ đánh giá; khác ngày → người tham gia về `DA_DANG_KY`; gọi `MangThuocChuyenDiApi.chuyenThayDoi` (MANDATORY) cho người đã có lần giao.
- **Cung cấp khoảng đi** (FR-034): phát `m08.api.ChuyenDiDaDoi` (011), `m04.api.ChuyenDiThayDoi` (016) và gọi `LichXeLenhApi.theoChuyen` (019, MANDATORY) với chuyến dùng xe của viện ở mỗi thay đổi; 006 đọc qua `ChuyenDiThuocPort.khoangDi`.

---

## L-08. Rời viện, trong chuyến, trở về (mục H; BR-M04-16, BR-M04-17, Q-166, Q-170, Q-307)

**Quyết định**:
- **Điểm danh rời viện** (FR-040, FR-041): kiểm trưởng đoàn, đánh giá Đạt, `MangThuocChuyenDiApi.daChuanBi`, trạng thái Đang lưu trú (bán trú: đã điểm danh đến), mốc CFG-M04-13, xe của viện có lịch Đã đặt (`m03.api.LichXeApi.coLichDaDat`; chưa có lịch → chặn "chưa có lịch xe"). Trong cùng transaction cho mỗi người: bản ghi `diem_danh_chuyen` loại `ROI_VIEN` (nhóm 3); `TrangThaiNguoiCaoTuoiApi.thucHien(DIEM_DANH_ROI_VIEN, chiTiet{chuyenId, thoiDiem})` (001 gọi bean 005, 006, 011… theo `@Order`, gồm hủy công việc trong khoảng và liều Mang theo); phát `SuKienTinhPhiNguon` (thu phí; ngày rời thực tế). Người đầu tiên → chuyến `DANG_DI`. *(R-15: `diem_danh_chuyen` → `su_kien_chung`)*
- **Điểm danh về** (FR-044): bản ghi `VE`; `TrangThaiNguoiCaoTuoiApi.thucHien(DIEM_DANH_VE, …)`; `CongViecVangMatApi.tinhLai` qua bean của 005 trong lệnh 001. Người "Thiếu khi về" về muộn → `SuCoLenhApi.themDienBien(…, DA_TRO_VE)` nếu có sự cố, hoặc `YeuCauGhiDienBienCanhBao` cho cảnh báo.
- **Ngoại tuyến** (FR-046a): `NgoaiTuyenHandler` `M04.DIEM_DANH_ROI_VIEN`, `M04.DIEM_DANH_VE`; "Báo thiếu người", "Kết thúc điểm danh về", ghi sự cố thiếu người: `batBuocTrucTuyen()`.
- **Báo thiếu người** (FR-046, FR-048): `SuCoLenhApi.ghiNhan(loai DI_LAC_KHONG_TRO_VE, muc KHAN_CAP, nguon HOAT_DONG_NGOAI_VIEN, chuyến, trưởng đoàn, nơi và lúc thấy lần cuối, nd)` (MANDATORY); người tham gia → `THIEU_KHI_VE`, đặt `su_co_thieu_nguoi_id` một lần.
- **Kết thúc điểm danh về** (FR-047): người đã rời, chưa về, không rời đoàn → `THIEU_KHI_VE`; người chưa có sự cố → `YeuCauTaoCanhBao(THIEU_NGUOI_KHI_VE, KHAN_CAP, loaiNguon NGUOI_THAM_GIA_CHUYEN, nguonId)` (007 gửi trưởng đoàn, trưởng tầng); không tạo sự cố. Chuyến → `DA_VE`.
- **Rời đoàn** (FR-045): listener `NguoiCaoTuoiDaChuyenTrangThai` (Điều trị bệnh viện, Qua đời) khi người tham gia `DA_ROI_VIEN`/`THIEU_KHI_VE` → `ROI_DOAN`.
- **Quá giờ** (FR-049, FR-049a): job theo phút, một lần mỗi mốc giờ về (cột "đã báo cho mốc").
- **Đính chính** (FR-031a): `DinhChinhDuocProvider` `M04.DIEM_DANH_CHUYEN` — chỉ Trưởng tầng của chuyến; (a) đổi thời điểm → `MangThuocChuyenDiApi.tinhLaiKhoang`, `ChuyenDiDaDoi`, 001 lịch sử; (b) Hủy ghi nhận rời viện → `TrangThaiNguoiCaoTuoiApi.thucHien(HUY_GHI_NHAN_ROI_VIEN)`, `SuKienTinhPhiNguonDaDoi(daHuy)`; chuyến không còn bản ghi rời viện → `DA_LEN_LICH`, `LichXeLenhApi.theoChuyen(HUY_ROI_VIEN_CUOI)`; (c) Hủy ghi nhận về → `DIEM_DANH_ROI_VIEN` lại với thời điểm gốc (chuyển Hoạt động bên ngoài).
- **Chuyển trưởng đoàn, gỡ nhiệm vụ** (FR-035, FR-035a): `ChuyenDiNhiemVuLenhApi.goNhiemVu(nhanVienId, caId, canCu)` (MANDATORY) cho 015; trưởng đoàn trống khi Đang đi → dấu "trưởng đoàn tạm từ xa", báo Quản lý viện.

---

## L-09. Sở thích, hồ sơ tinh thần, nguy cơ cô lập (mục I; BR-M04-22, Q-165, Q-169, Q-173, Q-307)

**Quyết định**:
- `so_thich` (nhóm 2): `HIEN_HANH | DA_THAY | DA_BO`; cột sinh `khoa_hien_hanh = IF(trang_thai = 'HIEN_HANH', CONCAT(nct_id, ':', nhom_id, ':', nguon_loai, ':', IFNULL(nguon_nguoi_than_id, 0)), NULL)` `UNIQUE` (sửa 2026-10-18, Q-363: một mục hiện hành mỗi nhóm và mỗi nguồn); nguồn theo mẫu cột 001 data-model mục 6a, kiểm bằng `NguonYKienApi.kiemTra`; ghi mục mới cùng nhóm và cùng nguồn cần `xacNhanThay = true`. Thói quen sinh hoạt là nhóm sở thích trong danh mục (Q-363). *(R-15: `so_thich` → `ho_so_chung`)*
- Hồ sơ tinh thần (FR-053): đọc tổng hợp từ điểm danh, `KetQuaTheoKhoang` của 005 (tâm trạng, hành vi), sở thích, `SuCoApi`/`CanhBaoApi` của 007. Không lưu.
- **Chuỗi cô lập** (FR-054 → FR-056): hàm thuần `NgayTinh(nct, ngay, khoangVang, nghiNhiem, khongGhiNhan, banTruDen)` và `CapNhatChuoi(chuoiCu, ngay, coMatNhom, laNgayTinh)`. `chuoi_co_lap` (kỹ thuật: `nct_id`, số ngày tính, ngày cuối xét, đã gửi cảnh báo cho chuỗi). Job 00:00 xét ngày vừa kết thúc; đạt CFG-M04-10 → `YeuCauTaoCanhBao(NGUY_CO_CO_LAP, NHE, loaiNguon CO_LAP, nguonId = nctId)` (007 gộp theo DBR-18). Lượt Có mặt hoạt động nhóm → đếm lại và `YeuCauGhiDienBienCanhBao(CO_LAP, nctId, DA_THAM_GIA_HOAT_DONG_NHOM)`; không đóng cảnh báo. Đính chính điểm danh ngày cũ → xét lại chuỗi từ ngày đó (job chạy lại theo ngày bị ảnh hưởng). *(R-15: `chuoi_co_lap` → `trang_thai_nct`)*
- Không có danh sách gợi ý (Q-307).

---

## L-10. Kiểm tra chất lượng (mục J; BR-M04-23, Q-163, Q-168, Q-174, Q-254, Q-262, Q-270, Q-306, Q-307)

**Quyết định**:
- `danh_sach_kiem_tra` (nhóm 2: ca, tầng, người lập, vai trò, dấu "không có danh sách"), `muc_kiem_tra` (nhóm 2 → kết quả nhóm 3: `CHO_KIEM_TRA | DAT | KHONG_DAT | KHONG_KIEM_TRA_DUOC | QUA_HAN_KIEM_TRA`, người kiểm tra, vai trò, ghi chú, ảnh, công việc làm lại). *(R-15: `danh_sach_kiem_tra` → `ho_so_chung`; `muc_kiem_tra` → `su_kien_chung`)*
- Hàm thuần `CongViecDuDieuKien(congViec, ca, truongTang, nguoiPhuTrachCaTaiLucHoanThanh)` (FR-060) và `SoMucToiThieu(soDuDieuKien, cfg11)` = trần(tỷ lệ × n), tối thiểu 1 khi n ≥ 1. Danh sách công việc đủ điều kiện đọc từ 005 nội bộ M04 (công việc của ca, thời điểm hoàn thành, người thực hiện, đính chính).
- `NguoiKiemTra(ca, nd, t)` (FR-066): Trưởng tầng được giao; hoặc Người phụ trách ca khi Trưởng tầng được giao không có tên trong ca hoặc có bản ghi vắng ca / nghỉ đột xuất đã duyệt (từ thời điểm hiệu lực); Người phụ trách ca không ghi cho mục mình thực hiện. Đọc `CaTrucApi.caKhongCoTruongTang`, `NhiemVuApi`.
- **Ghi Không đạt** (FR-064): một lệnh gồm kết quả và công việc làm lại; 014 gọi dịch vụ nội bộ của 005 lập công việc làm lại (căn cứ `LAM_LAI`, trỏ kết quả kiểm tra) trong cùng transaction; 005 kiểm qua `KetQuaKiemTraPort.khongDat` do 014 cài. Người thực hiện công việc gốc được báo; người thực hiện không được báo khi công việc được chọn.
- Danh sách, mục chỉ hiện cho người kiểm tra và Quản lý viện (giới hạn bằng `GanPhamViProvider` riêng của `M04.KIEM_TRA_CHAT_LUONG`).
- **Job**: tại mốc CFG-M09-04 trước kết ca → nhắc khi chưa có danh sách hoặc thiếu tỷ lệ; nghe `CaDaKetThuc` (008) → mục chưa có kết quả `QUA_HAN_KIEM_TRA`, báo Quản lý viện một lần mỗi danh sách; ca có công việc đủ điều kiện mà không có danh sách → ghi `ca_khong_danh_sach` kèm lý do, báo Quản lý viện. Công việc bị Hủy ghi nhận (sự kiện của 005) → mục `KHONG_KIEM_TRA_DUOC`. *(R-15: `ca_khong_danh_sach` → `moc_da_nhac`)*

---

## L-11. Bổ sung hợp đồng 007: diễn biến cảnh báo

**Quyết định**: Thêm sự kiện `m05.api.YeuCauGhiDienBienCanhBao(String loaiNguon, long nguonId, String maDienBien, String noiDung, String khoaSuKien)`: 007 ghi diễn biến vào cảnh báo đang mở của nguồn đó (không đổi trạng thái, không đóng). 014 dùng cho `CO_LAP` ("đã tham gia hoạt động nhóm", FR-056) và `NGUOI_THAM_GIA_CHUYEN` ("đã trở về", FR-047 khi chưa có sự cố). Không có cảnh báo mở → bỏ qua.

**Lý do**: 007 chưa có lệnh ghi diễn biến cảnh báo từ module khác; dùng sự kiện để 014 không phải chờ transaction của 007 (diễn biến không phải điều kiện của lệnh 014).

---

## L-12. Cổng người thân (FR-068, FR-016a)

**Quyết định**: REST của 014 dưới `/cong/nguoi-cao-tuoi/{nctId}/hoat-dong` (buổi đã đăng ký, chuyến đi, số buổi đã tham gia; không có mức tham gia, cảnh báo, kết quả kiểm tra) và `/cong/dong-y-ve-muon` (người đại diện ghi, rút). Kiểm `QuyenNguoiThanApi.coQuanHeHieuLuc` / `laNguoiDaiDien` mỗi lần gọi.

---

## L-13. Thông báo (FR-071)

**Đối tượng thông tin** (bổ sung 2026-10-18; FR-071, 009 FR-001a): dòng đồng ý về muộn tới người đại diện và người đã đăng ký khai `doiTuongThongTin = VE_VIEC_NGUOI_THAN`; các thông báo khác chỉ tới nhân viên.

**Quyết định**: Mức theo bảng FR-071 (đủ mức). Khóa `(loại, buổi[, người cao tuổi])`, `(loại, danh sách)`, thêm mốc với nhắc theo mốc. Cách thay khi nhóm trống: `CachThay.ChiDinh` theo quy tắc FR-071 (Trưởng tầng → Người phụ trách ca đang diễn ra; trưởng đoàn → Trưởng tầng; người phụ trách buổi → Trưởng tầng). Cảnh báo cô lập, thiếu người đi qua 007.

---

## L-14. Quyền (FR-067)

**Quyết định**:

| `ChucNang` | Thao tác |
| --- | --- |
| `HOAT_DONG` | `CAU_HINH` (Quản lý viện: loại hoạt động); `THUC_HIEN` (Trưởng tầng: hoạt động, mẫu, buổi trong tầng hoặc toàn viện theo FR-002b); `XEM` (Quản lý viện) |
| `DANG_KY_DIEM_DANH` | `THUC_HIEN` (Trưởng tầng, Nhân viên chăm sóc, Điều dưỡng trong phạm vi hoặc là người phụ trách buổi) |
| `DONG_Y_VE_MUON` | `GHI` (Hành chính; người đại diện qua cổng) |
| `CHUYEN_DI` | `PHAN_CONG`, `DANH_GIA_KHA_NANG`, `DINH_CHINH_DIEM_DANH` (Trưởng tầng); `ROI_VIEN_VE` (trưởng đoàn; Trưởng tầng thay); `BAO_THIEU_NGUOI` (trưởng đoàn, mọi người đi cùng, Trưởng tầng) |
| `SO_THICH` | `THUC_HIEN` (Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc trong phạm vi) |
| `KIEM_TRA_CHAT_LUONG` | `THUC_HIEN` (người kiểm tra theo `NguoiKiemTra`); `XEM` (Quản lý viện; Người phụ trách ca với ca ngoại lệ của mình) |

Nhiệm vụ trưởng đoàn, người đi cùng chỉ mở các lệnh của chuyến đó (002 FR-019); người đi cùng không thuộc ba vai trò chỉ "Báo thiếu người".

---

## L-15. Bộ lập lịch, lưu giữ, nhật ký, kiểm thử

**Quyết định**:
- **Job**: `ViecTheoPhutM04HoatDong` (nhắc điểm danh CFG-M04-06; đồng ý về muộn Đã dùng; quá giờ về 1× và 2× CFG-M04-07; mốc CFG-M09-04 kiểm tra chất lượng); `ViecNuaDemM04HoatDong` 00:05 (hết ngày của buổi; chuỗi cô lập). Không tạo buổi, danh sách, mục, công việc làm lại (Q-306). Cột "đã làm lúc" chống trùng; chạy bù.
- **Lưu giữ**: đăng ký, điểm danh, người tham gia, sở thích, kết quả kiểm tra: `THEO_HO_SO`; hoạt động, buổi, chuyến, danh sách: `KHONG_GAN_HO_SO`.
- **Nhật ký** (FR-069): mọi lệnh.
- **Kiểm thử**:
  - test bảng `BuoiTheoMau`, `DieuKienDangKy`, `NgayTinh`, `CapNhatChuoi`, `CongViecDuDieuKien`, `SoMucToiThieu`, `NguoiKiemTra`, công thức tỷ lệ tham gia (FR-070);
  - Test tích hợp MySQL: `UNIQUE` buổi theo (hoạt động, bắt đầu), danh sách theo (ca, tầng), mục theo công việc, sở thích hiện hành; trigger nhóm 3;
  - test đồng thời: hai đăng ký chỗ cuối; điểm danh rời viện của hai thiết bị; hai lệnh Báo thiếu người;
  - đồng hồ giả lập cho CFG-M04-06, 07, 10, 11, 13, CFG-M09-04, nửa đêm;
  - bản giả `LichXeApi`/`LichXeLenhApi` (019), 006 `MangThuocChuyenDiApi`, 007 `SuCoLenhApi`.
- Tên test theo BR/FR (`BR_M04_17_ThieuNguoiIT`, `BR_M04_23_KiemTraChatLuongIT`).

---

## L-16. Kênh người cao tuổi, nút SOS, sở thích từ phản hồi *(bổ sung 2026-10-18; FR-068a, FR-071; Q-366, Q-369)*

**Quyết định**:
- `HoatDongKenhNctPortImpl.sapToi`: buổi Đã lên lịch trong khoảng của tầng có giường của người cao tuổi và buổi toàn viện; `coTen` khi có đăng ký còn hiệu lực; DTO không có kết quả đánh giá, lý do.
- `ChuyenDiSosPortImpl.dangDienRa`: chuyến có người cao tuổi ở trạng thái Đã rời viện; trưởng đoàn hiện tại (sau chuyển trưởng đoàn), người đi cùng.
- Nhắc buổi trong ngày: khóa `(dang_ky_id, NHAC_NCT)`; chỉ khi người cao tuổi có tài khoản (`TaiKhoanApi.theoNguoiCaoTuoi`).
- `so_thich.phan_hoi_id`: nhận ở lệnh ghi sở thích; phát `YKienDaGhiTuPhanHoi` cùng transaction.

**Lý do**: SPI khai báo ở module dùng (`m10.api`, `m05.api`) như các SPI 014 đang cài, không tạo vòng.

