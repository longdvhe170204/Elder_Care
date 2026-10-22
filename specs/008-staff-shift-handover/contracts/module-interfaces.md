# Giao diện giữa module: Feature 008

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `m09.api` và bản cài các SPI do module thấp hơn khai báo (research N-01, N-02). Module khác chỉ dùng các giao diện này (constitution X). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong`, `VaiTro` theo 000, 001, 002. Mọi hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi; hàm đọc không khóa.

Plan làm sau (009, 011, 013, 014, 015, 016) giữ đúng chữ ký hoặc ghi rõ thay đổi.

---

## 1. 008 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M09 cài

| SPI (gói khai báo) | Bản cài của 008 | Dùng bởi |
| --- | --- | --- |
| `HoSoNhanVienApi` (`m15.taikhoan.api`; khai báo dạng SPI theo plan này, chữ ký giữ đúng 002) | `trangThai(nhanVienId, ngay)`, `chucDanhCoVaiTro(nhanVienId)`; `taoHoSoToiThieu(...)` cho lệnh khởi tạo Quản lý viện đầu tiên (002 A-03) | 002 FR-004, FR-004a, FR-011 |
| `GiayPhepApi` (`m15.taikhoan.api`) | `hieuLuc(nhanVienId, loai, ngay)` theo `HieuLucChungChi` (tới hết ngày hết hạn); `sapHetHan(trongVong)` cho cảnh báo giấy phép hành nghề của 002 (CFG-M06-02) | 002 FR-037, FR-038, FR-039 |
| `DanhMucPhamViHanhNgheApi` (`m15.taikhoan.api`) | Danh mục `pham_vi_hanh_nghe` | 002 FR-037, 007 FR-070 |
| `PhanCongApi` (`m15.taikhoan.api`) | `dangHieuLuc(nhanVienId, t)`, `lichSu(nhanVienId, tu, den)` (FR-037a; gồm giao trưởng tầng, thành viên ca, người phụ trách ca) | 002 FR-028 → FR-035a |
| `NhiemVuApi` (`m15.taikhoan.api`) | Mục 1.2 | 002 FR-019a → f, FR-031 |
| `ThamChieuViTri` (`m03.api`) | Phân công Nháp, Tương lai, Hiệu lực; giao trưởng tầng đang/sẽ hiệu lực; ca chưa đóng hoặc hủy của vị trí; `daTungThamChieu` cho quyết định xóa | 003 FR-005 |
| `NhanSuCaPort` (`m04.api`) | Giữ đúng chữ ký 005 mục 2.1; `phanCong` theo `XacDinhNhanVienChinh`; `banGiaoDaXacNhan(caTruocId)` | 005 FR-017, FR-024, FR-031, FR-047a |
| `DieuDuongCaPort` (`m07.api`) | Giữ đúng chữ ký 006 mục 2.1 | 006 FR-026a, FR-028 |
| `NhanSuCanhBaoPort` (`m05.api`) | Giữ đúng chữ ký 007 mục 2.1; `bacSiTruc(t)` theo FR-023 | 007 FR-031, FR-033, FR-037, FR-047b, FR-060 (c) |
| `DinhChinhDuocProvider` (000) | `M09.BAN_GIAO` (research N-10) | 000 R-08 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 9, 10 | |
| `GanPhamViProvider` (002) | `M09.LICH_CA`, `M09.CA`, `M09.PHAN_CONG`, `M09.BAN_GIAO`: tầng/khu của ca; `M09.NHAN_VIEN`, `M09.CHUNG_CHI`: nhân viên | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M09.SCAN_CHUNG_CHI`: chỉ Quản lý viện và chính nhân viên (nhóm hạn chế, FR-052) | NFR-08 |

### 1.2. `NhiemVuApi` (bản cài của 008)

```java
Optional<Long> nguoiPhuTrachCa(long tangKhuVucId, Instant t);       // từ nguoi_phu_trach_ca; rỗng khi ca thiếu người phụ trách
Optional<Long> truongTangDuocGiao(long tangKhuVucId, Instant t);    // giao_truong_tang hiệu lực (kể cả giao tạm)
Optional<Long> nguoiThayTruongTang(long tangKhuVucId, Instant t);   // Q-90: Người phụ trách ca đang diễn ra khi tầng chưa có Trưởng tầng
boolean caKhongCoTruongTang(long caId);                              // 002 FR-019e (014 FR-066, Q-262)
Optional<Long> phuTrachBoPhan(long boPhanId);  boolean laPhuTrachBoPhan(long nhanVienId);
Optional<Long> boPhanCua(long nhanVienId, Instant t);               // DBR-36; 019
Optional<Long> truongDoan(long chuyenId);  List<Long> nguoiDiCung(long chuyenId);   // ủy cho ChuyenDiNhiemVuPort (014)
List<Long> nguoiNhanTaiTang(long tangKhuVucId, long caId);          // 011 UC-76: TT, ĐD, CS có phân công trong tầng và ca
```

### 1.3. `m09.api.CaTrucApi` (đọc; 009, 011, 013, 015, 016)

```java
Optional<CaView> caDangDienRa(long tangKhuVucId, Instant t);
List<CaView> caCuaNhanVien(long nhanVienId, Instant tu, Instant den);
boolean dangTrongCa(long nhanVienId, Instant t);                    // 009: nhân viên "trong ca" để chọn kênh (BR-M13-01); cùng quy tắc với NhanSuCanhBaoPort.dangTrongCa (007, bổ sung 2026-10-22)
Optional<NhanVienView> nhanVien(long nhanVienId);                   // nhóm (a) công khai
```

### 1.4. `m09.api.NhanSuThongKeApi` (016; FR-037, FR-037a, FR-049 dòng 016)

```java
Stream<LanTinhTyLeView> tyLePhucVu(PhamViLoc phamVi, LocalDate tu, LocalDate den);   // lần tính cuối trước khi ca kết thúc, dấu từng không đạt (Q-200)
Stream<BanGiaoBaoCaoView> banGiao(PhamViLoc phamVi, LocalDate tu, LocalDate den);     // đúng hạn / trễ, trạng thái, thời điểm lập, xác nhận
Stream<ChungChiSapHetHanView> chungChiSapHetHan(int trongVongNgay);
Stream<CanCuNhiemVuView> canCuKhacVaiTro(PhamViLoc phamVi, Instant tu, Instant den);  // FR-056
Stream<LichSuPhanCongView> lichSuPhanCong(PhamViLoc phamVi, Instant tu, Instant den); // FR-037a
```

### 1.5. Port nội bộ M09 cho 015 (gói `m09.lichca.port`, `m09.phancong.port`)

Theo research N-07 (`CaLenhPort`, `PhanCongLenhPort`, `KiemTraCongBoMoRong`). Chỉ `m09.doica`, `m09.xoayca` được dùng (ArchUnit). Khi 015 chưa có, `KiemTraCongBoMoRong` không có bean và lệnh công bố bỏ qua phần của 015.

Bổ sung theo plan 015 (hợp đồng 015 mục 1.2, 1.4): `LichNhapLenhPort` (008 cài: `taoNhap`, `thayNoiDungNhap`, `goNhanVienKhoiNhap`, `lichThang`; MANDATORY) để lệnh "Lập lịch từ mẫu xoay ca" và nghỉ theo ngày ghi lịch Nháp; `NghiCoDuyetPort` (015 cài, 008 dùng khi sửa tay lịch Nháp và khi công bố, 008 FR-012, FR-016); sự kiện nội bộ M09 008 phát thêm: `LichNhapDaLuu(lichCaId)`, `GiayPhepDaDoiHieuLuc(nhanVienId, giayPhepId)`, `DaoTaoDaDoiHieuLuc(nhanVienId, daoTaoId)` để 015 tính lại phủ (015 FR-007).

### 1.6. Sự kiện 008 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `LichCaDaCongBo(lichId, nhanVienIds)` | 009 (Nhẹ, mỗi người chỉ ca của mình), 005, 006, 016 | FR-016, FR-054 |
| `CaDaThayDoi(caId, loai, nhanVienIds)` (bổ sung, chuyển người phụ trách, hủy) | 009 (Nhẹ) | FR-017 → FR-021 |
| `CaDaBatDau(caId, phamVi)`; `CaDaKetThuc(caId, phamVi, trangThai)` | 005, 006, 007, 013 (khóa bỏ trùng, xác định "người giữ vắng mặt") | FR-022, 013 FR-014 |
| `NhanVienVangCa(caId, nhanVienId)`; `NhanVienNghiViec(nhanVienId, hieuLuc)` | 009 (Trung bình, Trưởng tầng; không nêu lý do), 016 | FR-020, FR-004, FR-053 |
| `CaThieuNguoiPhuTrach(caId)`; `TangChuaCoTruongTang(tangId)` | 009 (Trung bình: Trưởng tầng, Quản lý viện) | FR-004, FR-019, FR-024 |
| `PhanCongDaDoi(caId, nhanVienId)`; `PhanCongKetThucDoChungChi(nhanVienId, caIds, nctIds)` | 005, 006 (gán lại); 009 (Trung bình, Trưởng tầng) | FR-009, FR-027 |
| `ChungChiSapHetHan(chungChiId, nhanVienId, loai, ngayHetHan)`; `CanhBaoChungChiDaDong(chungChiId)` | 009 (Nhẹ: Quản lý viện, nhân viên, Trưởng tầng có ca liên quan) | FR-008 |
| `CaDuoiNguongPhucVu(caId, tyLe, nguong)` | 009 (Trung bình: Trưởng tầng; Quản lý viện khi trong CFG-M09-08) | FR-036 |
| `NctChuaCoNguoiPhuTrach(caId, nctIds)` | 009 (Trung bình: Trưởng tầng, Người phụ trách ca) | FR-029 |
| `BanGiaoChuaLap(caId)`; `BanGiaoChuaHoanTatKhiHetCa(banGiaoId)`; `BanGiaoChuaXacNhan(banGiaoId)`; `BanGiaoCoYKien(banGiaoId)`; `BanGiaoDaLap(banGiaoId)`; `BanGiaoDangDoDaChuyen(banGiaoId, nguoiMoi)`; `BanGiaoDaDinhChinh(banGiaoId)` | 009 (Trung bình; đính chính: Nhẹ) | FR-039, FR-042, FR-043, FR-019, FR-057 |
| `BanGiaoDaXacNhan(banGiaoId, caTruocId, caSauId)` | 016 (đúng hạn/trễ); 005, 006, 007 đã xử lý trong transaction (không cần nghe) | FR-044 |
| `BoPhanChuaCoPhuTrach(boPhanId)` | 009 (Trung bình, Quản lý viện) | FR-001a |

Mỗi sự kiện mang khóa `loại + id + mốc` (NFR-04), mức, nhóm người nhận, loại thông tin theo research N-14.

---

## 2. 008 cần từ module khác

### 2.1. SPI 008 khai báo ở `m09.api`, module khác cài

```java
interface DoGuiBanGiaoPort {                       // 013 cài; chỉ đọc
  List<DoGuiDangGiuView> dangGiu(Collection<Long> nhanVienIds);   // FR-039 (g): đồ có giá trị Đang giữ, người giữ, người cao tuổi
  Map<Long, Boolean> conDangGiu(Collection<Long> doGuiIds, Collection<Long> nhanVienIds); // FR-040 (g): "đã xử lý xong"
}

interface ChuyenDiNhiemVuPort {                    // 014 cài; chỉ đọc
  Optional<Long> truongDoan(long chuyenId);  List<Long> nguoiDiCung(long chuyenId);   // 002 FR-019 qua NhiemVuApi
  List<ChuyenTrongCaView> chuyenTrongCa(long tangKhuVucId, Instant tu, Instant den);
      // FR-039 (e): người đang đi chuyến, chuyến quá giờ về dự kiến (014 FR-049)
}
```

Bean thiếu thì nhóm (g), phần chuyến đi của nhóm (e) trả "chưa có dữ liệu"; `truongDoan`, `nguoiDiCung` trả rỗng (002 không cấp quyền theo nhiệm vụ này: mặc định an toàn).

### 2.2. Sự kiện 008 nghe

| Sự kiện | Module | Xử lý | FR |
| --- | --- | --- | --- |
| `MucChamSocDaDoi(nctId, …, hieuLucTu)` | 001 | Tính lại tỷ lệ các ca chưa đóng từ `hieuLucTu` của tầng người đó | FR-035 |
| `NguoiCaoTuoiDaChuyenTrangThai(nctId, tu, den, lenh, …)` | 001 | Vắng mặt, trở về: tính lại tỷ lệ; trạng thái cuối: kết thúc phân công theo người đó, tính lại | FR-035, Edge Case |
| `PhanBoDaBatDau`, `PhanBoDaDong`, `DaChuyenGiuong` | 003 | Tính lại tỷ lệ các ca chưa đóng của tầng cũ và mới | FR-035 |
| `ThamSoDaDoi(CFG-M09-*)` | 000 | Xóa bộ đệm; không tính lại kết quả cũ | |

### 2.3. Giao diện có sẵn hoặc đã bổ sung

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `TaiKhoanApi.khoaDoNghiViec` (MANDATORY), `theoNhanVien`, `coVaiTro`; `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi`, `NguoiDungHienTai` | M15 (002) | FR-002, FR-004, FR-001a, N-12 | Có sẵn |
| `NghiepVuChuyenMonApi.loaiYeuCau(maNghiepVu)` (bổ sung theo plan này: loại giấy phép/đào tạo của nghiệp vụ "phát thuốc và xác nhận liều") | M15 (002) | FR-030 (Điều dưỡng phụ trách) | Bản giả |
| `MucChamSocApi.hienHanh`, `taiNgay`; `TrangThaiNguoiCaoTuoiApi.khoangTrangThai`; `NguoiCaoTuoiApi.trangThai` | M01 (001) | FR-034, FR-039 (e) | Có sẵn |
| `ViTriApi`, `LichSuViTriApi.theoViTriTrongNgay`, `CoCauViTriApi`, `CanhBaoBanGiaoApi.theoTang`; `TinhTrangGiuongApi.choSanSangQuaHan(tangId, t)` (bổ sung theo plan này) | M03 (003) | FR-026, FR-034, FR-039 (d), (e), (f) | Hàm cuối: bản giả |
| `LuotVangApi.dangMo` (`m01.api`, 004 cài); `HopDongDocApi.lichDenTaiNgay` | M02 (004) | FR-039 (e), FR-034 | Có sẵn |
| `CongViecCaApi` (MANDATORY), `CongViecBanGiaoApi`; `LoaiCongViecApi.yeuCau(loaiCongViecId)` và `KeHoachChamSocApi.loaiCongViecHienHanh(nctId)` (bổ sung theo plan này) | M04 (005) | FR-004, FR-020, FR-027, FR-030, FR-031, FR-039 (a), FR-044 | Hai hàm cuối: bản giả |
| `LieuBanGiaoApi` | M07 (006) | FR-039 (b), FR-040 | Có sẵn |
| `CanhBaoBanGiaoApi` (`dangMo`, `khiXacNhanBanGiao` MANDATORY), `ChiSoApi.vuotNguongTrongKhoang` | M05, M06 (007) | FR-039 (c), (d), FR-044 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `TepApi`, `common` | M15 (000) | Mọi lệnh | Có sẵn |
| Bản cài các SPI mục 2.1; `KiemTraCongBoMoRong` (015) | 013, 014, 015 | | Bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}` |
