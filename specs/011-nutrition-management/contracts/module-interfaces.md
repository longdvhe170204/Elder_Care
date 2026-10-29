# Giao diện giữa module: Feature 011

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `m08.api`, port nội bộ M08 cho 019, và bản cài các SPI do module thấp hơn khai báo (research M-02, M-12). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong` theo 000, 001, 002. Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 1. 011 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M08 cài

| SPI (gói khai báo) | Bản cài của 011 | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Bean `@Order` sau 006, trước 010. Không có điều kiện chặn. Tác động `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN`: bản gán Hiệu lực → `NGUNG`, Đề xuất/Chờ hiệu lực → `DA_HUY`; phát sinh "−1" cho bữa đã chốt chưa tới giờ (FR-015). `HOAN_TAT_TIEP_NHAN`: báo Bác sĩ khi chưa có bản gán (FR-014) | 001 H-02 |
| `NguonCongViecPort` (`m04.api`) | `loaiQuanLy() = {BUA}`; `kiemTra(BUA, buaNgayId, nctId, t)`: bữa ngày tồn tại, người có trong lịch bữa, `t` khớp giờ bữa; `xacNhanPhucVu(buaNgayId, nctId)` theo research M-10 | 005 FR-017, 011 FR-042, FR-050 |
| `NgoaiTuyenHandler` (`m02.api`) | `M08.XAC_NHAN_PHUC_VU` (ghi ngoại tuyến của app nhân viên, Q-01): kiểm tra lại khi đồng bộ; bị chặn thì trả lý do để app hiển thị | 002 |
| `PhanLoaiTruongProvider`, `LuuTruProvider`, `DinhChinhDuocProvider` (000) | data-model; đính chính `M08.XAC_NHAN_PHUC_VU`, `M08.LUU_MAU` | 000 |
| `GanPhamViProvider` (002) | `M08.BAN_GAN`, `M08.XAC_NHAN_PHUC_VU`, `M08.DO_AN_GIA_DINH`: người cao tuổi; `M08.PHIEU_BUA_AN`: tầng/khu | 002 FR-034 |

### 1.2. `m08.api.CheDoAnApi` (006, 007, 012, 016)

```java
Optional<CheDoAnHienHanhView> hienHanh(long nctId);      // chế độ ăn, kết cấu, hạn chế riêng; mặc định nếu chưa gán (dấu CHUA_GAN)
List<BanGanView> lichSu(long nctId);                      // 012 hiển thị khi có quyền xem sức khỏe
```

### 1.3. `m08.api.ThucDonChungApi` (012; FR-028)

```java
List<MonTheoBuaView> thucDonChung(LocalDate tuNgay, LocalDate denNgay);   // món của chế độ ăn mặc định theo ngày, bữa; không có thông tin cá nhân
List<String> tenCheDoAn();
```

### 1.3a. `m08.api.SuatAnDuKienApi` (012; FR-063; bổ sung 2026-10-18)

```java
List<SuatDuKienView> cuaNguoiCaoTuoi(long nctId, LocalDate tuNgay, LocalDate denNgay);
    // ngày, bữa, tên món, laMonThayThe; không có chế độ ăn, kết cấu, dị ứng (research M-16)
```

### 1.4. `m08.api.LichBuaApi` (005, 014)

```java
List<BuaNgayView> buaTrongNgay(LocalDate ngay);                       // bữa ngày, giờ dự kiến, đã chốt chưa
boolean coTrongLichBua(long buaNgayId, long nctId);                   // 005 lập công việc BUA
```

### 1.5. `m08.api.DinhDuongThongKeApi` (016)

```java
Stream<PhieuBaoCaoView> phieu(PhamViLoc phamVi, LocalDate tu, LocalDate den);   // trạng thái, giờ giao, trễ, sai lệch
Stream<SuatBaoCaoView> suat(PhamViLoc phamVi, LocalDate tu, LocalDate den);     // số suất theo tầng, chế độ ăn, phát sinh
Stream<DoAnGiaDinhView> doAnGiaDinh(PhamViLoc phamVi, LocalDate tu, LocalDate den);
```

### 1.6. SPI khai báo ở `m08.api` cho module làm plan sau (research M-12)

```java
interface LuotOLaiAnPort {                 // 012 cài; chỉ đọc
  List<LuotOLaiView> dangOLaiCoDangKyAn(Instant t);    // người thân, người cao tuổi được gắn, khoảng, đăng ký ăn (FR-034 c)
}
record LuotOLaiDaDoi(long luotId, long nctId, long nguoiThanId, String trangThai, boolean dangKyAn,
                     Instant tu, Instant den, String khoaSuKien) {}
    // 012 phát: bắt đầu, kết thúc sớm, hủy, đổi đăng ký ăn

interface ChuyenDiBuaAnPort {              // 014 cài; chỉ đọc
  List<ChuyenTrungBuaView> trungGioBua(Instant gioBua);   // chuyến đã lên lịch trùng giờ bữa: người tham gia, dự kiến trở lại
}
record ChuyenDiDaDoi(long chuyenId, Set<Long> nctIds, Instant tu, Instant denDuKien, String loai, String khoaSuKien) {}
    // 014 phát: lên lịch, đổi người tham gia, gia hạn giờ về, hủy chuyến, đính chính điểm danh
```

Bean thiếu: `LuotOLaiAnPort` → không tính người thân, màn tính sẵn ghi "chưa có dữ liệu ở lại"; `ChuyenDiBuaAnPort` → không trừ người đi chuyến đã lên lịch (người đã điểm danh rời viện vẫn trừ theo trạng thái của 001).

### 1.7. Port nội bộ M08 (019) *(Bỏ theo Q-392)*

Không còn `TonKhoNguyenLieuPort`: hệ thống không quản lý kho nguyên liệu.

### 1.8. Sự kiện 011 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `m05.api.YeuCauTaoCanhBao` (`XUNG_DOT_DI_UNG_CHE_DO_AN`, nguồn `THUC_DON_MON`; `PHUC_VU_SUAT_DI_UNG`, nguồn `SUAT_DAC_BIET`) | 007 | FR-021, FR-049 |
| `m05.api.YeuCauDongCanhBaoNguon` | 007 | FR-021 (dị ứng loại trừ, đổi món) |
| `m11.api.SuKienTinhPhiNguon` / `SuKienTinhPhiNguonDaDoi` (`SUAT_AN_NGUOI_THAN`) | 010 | FR-043 |
| `BanGanCheDoAnDaHieuLuc(nctId, banGanId, cheDoAnId, ketCau, thoiDiem)` | 005 (nhắc xem kế hoạch ăn), 012 | FR-011 |
| `PhieuDaGiao`, `PhieuCoSaiLech`, `PhieuGiaoTre` | 016 | FR-047, FR-048, FR-048a |
| Thông báo (cài `CoYeuCauThongBao`) | 009 | FR-065 |

---

## 2. 011 cần từ module khác

| Giao diện | Module | Dùng cho | Kiểm thử thay thế |
| --- | --- | --- | --- |
| `MucSucKhoeApi.hieuLuc(nctId, DI_UNG)`, `daKiemTra(mucId, AN)`; `DiNguyenApi`, `themDiNguyen` | 001 | FR-003, FR-020, FR-021 | Bản giả |
| `NguoiCaoTuoiApi.theoTrangThai`, `QuanHeNguoiThanApi.thuocNguoiCaoTuoi` | 001 (012 cài phần người thân) | FR-034, FR-057 | Bản giả |
| `ViTriApi.hienTai(Collection)`, `taiThoiDiem`, `viTriKhuNghiBanTru` | 003 | Chia suất theo tầng; người nhận bán trú | Bản giả |
| `HopDongDocApi.lichDenTaiNgay`, `LuotVangApi.dangMo` | 004 | FR-034 (a), (b) | Bản giả |
| `CoMatBanTruApi`, `NguonCongViecApi.nguonBiHuy(BUA, …)` (MANDATORY) | 005 | FR-034 (b), FR-042 | Bản giả |
| `SuCoLenhApi.ghiNhan` (MANDATORY) | 007 | FR-049 "đã ăn" | Bản giả ghi lại lệnh |
| `CaTrucApi.caDangDienRa`, `NhiemVuApi.nguoiNhanTaiTang`, `nguoiPhuTrachCa` | 008, 002 | FR-028, FR-048 | Bản giả |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi` | 000, 002 | Chung | Bản thật trong test module |

### 2.1. Sự kiện 011 nghe

| Sự kiện (module) | Xử lý | FR |
| --- | --- | --- |
| `DiUngDaGhiNhan`, `DiUngKhacDaGhiNhan`, `DiUngDaLoaiTru` (001) | Đối chiếu thực đơn, suất đã chốt, đồ ăn gia đình; `daKiemTra` | FR-021, FR-036a, FR-056 |
| `NguoiCaoTuoiDaChuyenTrangThai` (001) | Phát sinh khi Hoạt động bên ngoài, trở về | FR-039 |
| `DaChuyenGiuong` (003) | Phát sinh đổi tầng | FR-039 |
| `LuotVangDaMo`, `LuotVangDaGiaHan`, `LuotVangDaDong`, `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi`, `PhuLucDaApDung` (004) | Phát sinh; hủy bữa cho 005 | FR-034, FR-039, FR-042 |
| `TrangThaiCoMatDaXacDinh`, `TrangThaiCoMatDaDoi`, `AnKemKeoDai` (005) | Phát sinh bán trú; báo xem lại | FR-039, FR-058 |
| `SutCanPhatHien` (007) | Báo xem lại | FR-058 |
| `LuotOLaiDaDoi` (012), `ChuyenDiDaDoi` (014) | Phát sinh; hủy suất ăn người thân | FR-039, FR-043 |

Mọi listener idempotent theo `khoaSuKien` (`su_kien_m08_da_xu_ly`) và chỉ xét các bữa đã chốt chưa tới giờ.
