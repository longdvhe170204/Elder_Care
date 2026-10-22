# Giao diện giữa module: Feature 010

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `m11.api`, port nội bộ M11 cho 017, 019, và bản cài các SPI do module thấp hơn khai báo (research P-02, P-12). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong` theo 000, 001, 002. Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 1. 010 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M11 cài

| SPI (gói khai báo) | Bản cài của 010 | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Bean `@Order` sau 007. Điều kiện `KT_A_CHI_PHI` (`coTheMien = true`) cho `KET_THUC_LUU_TRU`; tác động `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN` theo research P-10 | 001 H-02, 004 T-09 |
| `KyCuoiPort` (`m02.api`) | `taoKyCuoiNhap`, `tinhLai`, `huyMocDung` (MANDATORY; research P-10); giữ đúng chữ ký 004 | 004 FR-066 |
| `MucViecSauQuaDoiPort` (`m02.api`) | `ma() = CHI_PHI_KY_CUOI`; `HOAN_THANH` khi kỳ cuối và bảng bổ sung tồn tại lúc qua đời đã chốt (FR-036) | 004 FR-071 |
| `DeNghiMuaHoPort` (`m07.api`) | `daMua(deNghiId, nctId)`: đề nghị `DA_MUA`, mục, số lượng, nhân viên mua (Q-139) | 006 FR-040 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 9, 10 | |
| `GanPhamViProvider` (002) | `M11.BANG_CHI_PHI`, `M11.KHOAN_CHI_PHI`, `M11.DE_NGHI_MUA_HO`: người cao tuổi | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M11.CHUNG_TU_MUA_HO`: Hành chính, Quản lý viện, Kế toán, người đại diện của người đó | NFR-08 |

### 1.2. `m11.api.ChiPhiCongNguoiThanApi` (012; FR-037, FR-038)

```java
List<BangChiPhiView> bangDaChot(long nctId, NguoiDungHienTai nguoiThan);          // tên thuốc theo quyền (FR-019)
TamTinhView tamTinh(long nctId, LocalDate tu, LocalDate den, NguoiDungHienTai nd); // FR-038: tổng theo loại, nhãn "tạm tính", ghi chú thiếu đơn giá
List<DeNghiMuaHoView> deNghiChoDongY(long nctId, long nguoiThanId);                // FR-023a; kèm dấu số dư không đủ (Q-219)
```

### 1.3. `m11.api.ChiPhiKyCuoiApi` (004 hiển thị danh sách điều kiện)

```java
TrangThaiDieuKien dieuKienKyCuoi(long nctId);   // FR-034a: DAT | CHUA_DAT kèm lý do (bảng chưa chốt, ngày thiếu khoản, điều chỉnh ảnh hưởng chưa chốt)
```

### 1.4. `m11.api.ChiPhiThongKeApi` (016; 18.4, 18.5)

```java
Stream<KhoanBaoCaoView> khoan(PhamViLoc phamVi, LocalDate tu, LocalDate den);
    // loại, nguồn sinh, người lập, dịch vụ/vật phẩm/hoạt động (thuốc: mã), ngày phát sinh, số lượng, đơn giá, thành tiền, dấu, trạng thái
Stream<BangBaoCaoView> bang(Optional<Long> kyId);    // thường/bổ sung, kỳ, trạng thái, hạn chốt
Stream<KyView> ky();
```

### 1.5. Sự kiện `m11.api` cho module làm plan sau phát (011, 012, 014)

```java
record SuKienTinhPhiNguon(String loaiNguon, String nguonId, String donVi, long nctId, Optional<Long> dichVuId,
                          BigDecimal soLuong, LocalDate ngayPhatSinh, Instant thoiDiem, String khoaSuKien) {}
    // 011: SUAT_AN_NGUOI_THAN (một suất); 012: DEM_O_LAI (mỗi mốc 00:00 một đêm, ngày bắt đầu của đêm);
    // 014: DIEM_DANH_HOAT_DONG (lượt có mặt, kể cả "bỏ giữa chừng"; chuyến đi: điểm danh rời viện, ngày rời viện thực tế)
record SuKienTinhPhiNguonDaDoi(String loaiNguon, String nguonId, String donVi, Optional<BigDecimal> soLuongMoi,
                               boolean daHuy, String canCu, String khoaSuKien) {}
```

### 1.6. Sự kiện 010 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `BangChiPhiDaChot(bangId, loaiBang, kyId, nctId, tong, chotLuc)` (đúng một lần mỗi bảng) | 017 (giao dịch thanh toán bảng, DBR-30), 012, 016 | FR-033, FR-042 |
| `DeNghiMuaHoDaGui`, `…DaQuyetDinh`, `…DaHuy`, `…DaMua` (`deNghiId`, `nctId`, số tiền, trạng thái) | 017, 012 | FR-023, bảng giao tiếp "Gửi 017" |
| `KhoanChiPhiDaDoi(nctId, kyId)` | 017 (tính lại tổng chưa chốt), 016 | FR-038a |
| Thông báo theo bảng FR-044 (cài `CoYeuCauThongBao`, loại `CHI_PHI`, không tên thuốc) | 009 | FR-044 |

---

## 2. Port nội bộ M11 (017, 019)

Theo research P-12 (`ChiPhiChoSoDuPort` 010 cài; `SoDuMuaHoPort` 017 cài). *(Bổ sung 2026-10-22, FR-042, Q-378)* `ChiPhiChoSoDuPort.bangDaChot(bangId)` trả chi tiết khoản Đã chốt và tổng theo loại cho phiếu báo phí, bảng quyết toán của 017 (chữ ký ở research P-12). Chỉ `m11.sodu`, `m11.muasam` được dùng (ArchUnit). Khi 017 chưa có, `SoDuMuaHoPort` không có bean và đề nghị không mang dấu "số dư không đủ".

---

## 3. 010 cần từ module khác

### 3.1. Sự kiện 010 nghe

| Sự kiện | Module | Xử lý | FR |
| --- | --- | --- | --- |
| `KetQuaCoTinhPhi`, `KetQuaTinhPhiDaDinhChinh` | 005 | Chỉ mục; tính lại / hủy / biến động sau chốt | FR-005, FR-020 |
| `TrangThaiCoMatDaXacDinh`, `TrangThaiCoMatDaDoi` | 005 | Buổi bán trú: tính lại khoản buổi | FR-005b, FR-020 |
| `SuKienTinhPhiThuoc`, `SuKienTinhPhiThuocDaDieuChinh`, `SuKienTinhPhiThuocDaDinhChinh` | 006 | Chỉ mục; tính lại (nhận lại thuốc, đính chính) | FR-005, FR-020 |
| `NgayLuuTruDaDoi(nctId, tuNgay, denNgay, nguyenNhan)` | 004 | Khoản phí lưu trú trong khoảng | FR-005a, FR-020 |
| `PhuLucDaApDung` | 004 | Khoản chưa chốt từ ngày hiệu lực tính lại; đổi phòng sau chuyển giường y tế: biến động và báo Hành chính (Edge Case) | FR-008, FR-027 |
| `HoSoKetThucDaLap`, `…DaDoiNgay`, `…DaHuy`, `…HoanTat` | 004 | Bổ trợ `KyCuoiPort` (thông báo, kiểm lại điều kiện); idempotent | FR-034 |
| `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi` | 004 | Khoản buổi bán trú tính lại hoặc hủy | FR-005b |
| `PhienBanDonGiaDaTao(dichVuId, hieuLucTu, hieuLucDen)` (bổ sung theo plan này vào hợp đồng 004) | 004 | Tính lại khoản `THIEU_DON_GIA` chưa chốt | FR-012 |
| `SuKienTinhPhiNguon`, `SuKienTinhPhiNguonDaDoi` | 011, 012, 014 | Chỉ mục; tính lại / hủy | FR-005, FR-020 |
| `ThamSoDaDoi(CFG-M11-*)` | 000 | Xóa bộ đệm; không đổi đề nghị đã gửi | FR-023 |

### 3.2. Giao diện có sẵn hoặc đã bổ sung

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `HopDongDocApi`, `DonGiaApi` (`donGiaTaiNgay` MANDATORY khi đánh dấu tham chiếu, `donGiaHopDong`, `dichVuChamSoc`, `thuocNguonVien`), `NgayLuuTruApi` | M02 (004) | P-04, P-05 | Có sẵn |
| `NguoiDaiDienPort.laNguoiDaiDienHieuLuc`, `coNguoiDaiDien` (`m02.api`, 012 cài) | M10 (012) | FR-023, FR-023a | Bản giả |
| `NguoiCaoTuoiApi.trangThai`, `thoiDiemTrangThaiCuoi` | M01 (001) | FR-011, FR-036 | Có sẵn |
| `DanhSachViecSauQuaDoiApi.capNhat` | M02 (004) | FR-036 | Có sẵn |
| `QuyenApi`, `QuyenXemTruongApi` (tên thuốc theo quyền), `NguoiDungHienTai` | M15 (002) | P-06, P-14 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `TepApi`, `NhacQuaHan`, `common` (`thongbao`) | M15 (000) | Mọi lệnh | Có sẵn |
| Tên thuốc theo mã cho người được xem: `DonThuocApi`/danh mục thuốc của 006 (đọc) | M07 (006) | FR-019 | Chỉ hiện mã |
