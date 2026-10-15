# Giao diện giữa module: Feature 015

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Feature 015 thêm gói con vào module M09 (cùng 008). Giao diện dưới đây gồm port nội bộ M09 (dùng và bổ sung), bản cài SPI, giao diện công khai cho 016 và API của module khác mà 015 gọi (research X-02, X-03). Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 1. Port nội bộ M09

### 1.1. Port của 008 mà 015 dùng (008 research N-07)

`CaLenhPort` (`themVaoCa`, `roiCa`, `doiNguoiPhuTrach`, `ganThieuNguoiPhuTrach`), `PhanCongLenhPort` (`kiemTra`, `lap`, `huy`). Căn cứ 015 truyền: `CanCuVaoCa.DOI_CA | NHAN_THAY | LOI_MOI`, `CanCuRoiCa.DOI_CA | NGHI_CO_DUYET` kèm mã yêu cầu hoặc lời mời.

### 1.2. Bổ sung theo plan này (gói `m09.lichca.port`)

```java
interface LichNhapLenhPort {        // 008 cài; MANDATORY; chỉ m09.xoayca, m09.doica dùng
  long taoNhap(PhamViCa phamVi, YearMonth thang, List<CaNhap> ca, CanCu canCu);   // 015 FR-008, FR-014
  void thayNoiDungNhap(long lichCaId, List<CaNhap> ca, CanCu canCu);              // 015 FR-013 (Lập lại)
  void goNhanVienKhoiNhap(long lichCaId, long nhanVienId, LocalDate ngay, CanCu canCu);
      // 015 FR-026a: gỡ khỏi ca Nháp bắt đầu trong ngày và phân công Nháp; người phụ trách để trống
  Optional<LichView> lichThang(PhamViCa phamVi, YearMonth thang);
}
record CaNhap(LocalDate ngay, long mauCaId, List<Long> nhanVienIds) {}   // người phụ trách luôn trống (015 FR-011, Q-307)

interface NghiCoDuyetPort {         // 015 cài; 008 dùng
  boolean nghiTheoNgay(long nhanVienId, LocalDate ngay);                      // 008 FR-012: cảnh báo khi sửa tay lịch Nháp
  void ghiXepLai(long nhanVienId, LocalDate ngay, long lichCaId, LyDo lyDo, NguoiDungHienTai nd);   // MANDATORY; 015 FR-026a
  List<YeuCauNghiView> nghiTheoNgayChoDuyet(long lichCaId);                   // 008 FR-016: hiển thị khi công bố
}
```

### 1.3. `KiemTraCongBoMoRong` (015 cài; 008 gọi trong lệnh công bố)

`kiemTra(lichCaId)` trả: ca thiếu phủ, mục "thiếu ca", yêu cầu nghỉ theo ngày còn chờ (cảnh báo không chặn; lệnh công bố cần xác nhận đã xem và lý do, 015 FR-017).

### 1.4. Sự kiện nội bộ M09 015 nghe (008 phát)

| Sự kiện | Xử lý |
| --- | --- |
| `LichNhapDaLuu(lichCaId)` | Tính phủ ca Nháp, mục "thiếu ca" |
| `LichCaDaCongBo(lichId, …)` | Mở cảnh báo thiếu phủ; nghỉ theo ngày còn chờ → theo ca |
| `CaDaThayDoi`, `CaDaHuy`, vắng ca, `NhanVienDaBoSungVaoCa` | Tính phủ; yêu cầu, lời mời hết hiệu lực khi không còn thỏa |
| `NhanVienDaNghiViec`, `GiayPhepDaDoiHieuLuc`, `DaoTaoDaDoiHieuLuc` | Tính phủ các ca chưa kết thúc của người đó; yêu cầu hết hiệu lực |

`LichNhapDaLuu`, `GiayPhepDaDoiHieuLuc`, `DaoTaoDaDoiHieuLuc` bổ sung vào danh sách sự kiện của 008 theo plan này (008 đã có `CaDaThayDoi`, `CaDaHuy`, `LichCaDaCongBo`).

---

## 2. 015 cung cấp ra ngoài M09

### 2.1. `m09.api.XoayCaThongKeApi` (016; FR-042)

```java
Stream<LanTinhPhuView> phu(PhamViLoc phamVi, LocalDate tu, LocalDate den);          // ca thiếu phủ, thời gian thiếu phủ theo vai trò
Stream<MucThieuCaView> thieuCaDaCongBo(PhamViLoc phamVi, YearMonth thang);
Stream<YeuCauNhanSuView> yeuCau(PhamViLoc phamVi, LocalDate tu, LocalDate den);     // loại, trạng thái cuối, thời gian lập → quyết định
Stream<GioLamView> gioLamTheoThang(PhamViLoc phamVi, YearMonth thang);              // FR-034 (không có lý do)
Stream<LoiMoiView> loiMoi(PhamViLoc phamVi, LocalDate tu, LocalDate den);           // tỷ lệ nhận
```

### 2.2. SPI và provider 015 cài

| SPI | Bản cài | Dùng bởi |
| --- | --- | --- |
| `LoaiYeuCauHandler` (000) | `M09.DOI_CA`, `M09.NGHI_DOT_XUAT`: `chuyenMoRong()` cho các trạng thái ngoại lệ (000 FR-031 đã khai); `choTuDuyet = true` với dấu "tự duyệt" (Q-10); `apDung` gọi tác động FR-024 / FR-029 | 000 |
| `GanPhamViProvider` (002) | `M09.DOI_CA`, `M09.NGHI_DOT_XUAT`, `M09.THIEU_PHU`, `M09.LOI_MOI`: phạm vi ca | 002 FR-034 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | Lý do `LY_DO_NHAN_SU`; lưu ≥ CFG-M15-04 | 000 |

### 2.3. Sự kiện 015 phát

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| Thông báo (cài `CoYeuCauThongBao`) | 009 | FR-044 |
| `CaThieuPhuDaMo`, `CaThieuPhuDaDong` | 016 | FR-042 |

---

## 3. 015 cần từ module khác

| Giao diện | Module | Dùng cho | Kiểm thử thay thế |
| --- | --- | --- | --- |
| `TaiKhoanApi.theoNhanVien`, `coVaiTro`; `QuyenApi` | 002 | FR-003, FR-019, FR-033, FR-040 | Bản thật |
| `CongViecCaApi.khiNghiCoDuyet`, `khiKetThucPhanCong` (MANDATORY) | 005 | FR-024 (b), FR-029 (c) | Bản giả ghi lại lệnh |
| `ChuyenDiNhiemVuLenhApi.goNhiemVu` (MANDATORY); `ChuyenDiNhiemVuPort` (đọc, qua 008) | 014 | FR-021 (h), FR-024 (g), FR-027, FR-029 (h) | Bản giả |
| `NhatKyApi`, `ThamSoApi` (CFG-M09-02, 06, 07, 10, 11, CFG-M15-04 → 06), `YeuCauPheDuyetApi`, `NhacQuaHan` | 000 | Chung | Bản thật |
