# Giao diện giữa module: Feature 012

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `m10.api`, và bản cài các SPI do module thấp hơn khai báo (research F-02, F-14). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong` theo 000, 001, 002. Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 1. 012 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M10 cài

| SPI (gói khai báo) | Bản cài của 012 | Dùng bởi |
| --- | --- | --- |
| `QuanHeNguoiThanApi` (`m15.taikhoan.api`; hợp nhất theo plan này, F-14) | Chữ ký ở dưới | 001 FR-011, FR-023c; 002 FR-045 → FR-052; 006 FR-040, FR-041; 011 FR-057 |
| `DieuKienChuThich` (`m15.quyen.api`) | Chú thích ² "chỉ người đại diện": `laNguoiDaiDienHieuLuc(nd.nguoiThanId, doiTuong.nctId, now)` | 002 A-05 |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Bean `@Order` sau 010, trước 013. Điều kiện `DBR_02` (`coTheMien = false`) cho `HOAN_TAT_TIEP_NHAN`. Tác động: `CHO_TAM_VANG`, `DIEU_TRI_BV` → hủy lượt thăm trùng thời gian, nhắc kết thúc lượt ở lại; `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN` → FR-075 | 001 H-02 |
| `NguoiDaiDienPort` (`m02.api`) | `laNguoiDaiDienHieuLuc`, `coNguoiDaiDien` | 004, 010 |
| `BanGhiDonPort` (`m02.api`) | `banGhiDonHopLe`; thêm `daDung` (MANDATORY, bổ sung theo plan này) | 004 FR-048, FR-066 |
| `BanGhiDonBanTruPort` (`m04.api`) | `hopLe`; thêm `daDung` (MANDATORY, bổ sung theo plan này) | 005 điểm danh về |
| `ThoiKhoaBieuNguonPort` (`m04.api`) | Lượt thăm Đã duyệt, Đã vào trong ngày | 005 FR-048 |
| `NguoiThanKhanCapPort` (`m05.api`) | `nguoiLienHeChinh`, `nguoiDaiDien` (theo `thu_tu_lien_he`), `luotThamTrongKhoang` (FR-038) | 007 FR-047, FR-050b, FR-060 |
| `NguoiThanNhanThongBaoPort` (`m13.api`) | `quanHeHieuLuc`, `thuTuGoi`, `lienHe` | 009 |
| `LuotOLaiAnPort` (`m08.api`) | `dangOLaiCoDangKyAn(t)` | 011 FR-034 (c) |
| `GanPhamViProvider` (002) | `M10.PHAN_HOI`, `M10.LUOT_THAM`, `M10.LUOT_O_LAI`, `M10.YEU_CAU_QUYEN`, `M10.NGOAI_LE_DON`: người cao tuổi | 002 FR-034 |
| `PhanLoaiTruongProvider`, `LuuTruProvider`, `DinhChinhDuocProvider` (000) | data-model; đính chính `M10.LUOT_THAM` (giờ vào/ra), `M10.BAN_GHI_DON` | 000 |
| `QuyenTepProvider` (`common.tep`) | `M10.BANG_CHUNG`, `M10.PHIEU_DANG_KY`: Hành chính, Quản lý viện; `M10.PHAN_HOI`: người gửi, người phụ trách, Quản lý viện | NFR-08 |

```java
interface QuanHeNguoiThanApi {     // m15.taikhoan.api; M10 cài
  List<QuanHeView> hieuLuc(long nguoiThanId);                                   // 002 phạm vi người thân (FR-045)
  Optional<String> soDienThoaiDaDangKy(long nguoiThanId);                       // 002 FR-050 (gọi lại số đã đăng ký)
  boolean laNguoiDaiDienHieuLuc(long nguoiThanId, long nctId, Instant t);      // 006; từ 2026-10-18 (Q-348) 001 không dùng cho FR-011, FR-023c
  boolean thuocNguoiCaoTuoi(long nguoiThanId, long nctId);                      // quan hệ Hiệu lực; 001, 006, 011
  boolean quyenDangBat(long nguoiThanId, long nctId, QuyenNguoiThan quyen, Instant t);   // lớp quyền của 002 FR-046
}
```

Hàm `daDung` thêm vào hai port:

```java
// m02.api.BanGhiDonPort (004)
void daDung(long banGhiDonId, String lenhNguon, long nguonId);   // MANDATORY: Cho tạm vắng, bàn giao khi Kết thúc lưu trú; lần hai → BAN_GHI_DON_DA_DUNG
// m04.api.BanGhiDonBanTruPort (005)
void daDung(long banGhiDonId, long diemDanhId);                  // MANDATORY: Điểm danh về; lần hai → BAN_GHI_DON_DA_DUNG
```

### 1.2. `m10.api.QuyenNguoiThanApi` (013, 014, 016, 017, 018; REST của các feature đó phục vụ cổng)

```java
boolean coQuanHeHieuLuc(long nguoiThanId, long nctId);
boolean quyenCoTacDung(long nguoiThanId, long nctId, QuyenNguoiThan quyen, Instant t);   // XEM_SUC_KHOE kèm bản đồng ý (FR-011)
boolean laNguoiDaiDien(long nguoiThanId, long nctId, Instant t);
boolean xemDoGui(long nguoiThanId, long nctId, Instant t);           // FR-046b: người đại diện hoặc được phép đón đang bật
List<NguoiThanView> nguoiDaiDien(long nctId);
Optional<NguoiThanView> nguoiLienHeChinh(long nctId);
List<NguoiThanView> duocPhepDonTai(long nctId, Instant t);           // 013 xác minh người nhận đồ (FR-016)
Optional<GiayToView> giayTo(long nguoiThanId);                        // 013
```

### 1.3. `m10.api.NguoiThanThongKeApi` (016)

```java
Stream<LuotThamBaoCaoView> luotTham(PhamViLoc phamVi, LocalDate tu, LocalDate den);
Stream<PhanHoiBaoCaoView> phanHoi(PhamViLoc phamVi, LocalDate tu, LocalDate den);   // hạn, quá hạn, mức
```

### 1.4. SPI khai báo ở `m10.api` cho module làm plan sau

```java
// HoatDongBanTinPort: *(Bỏ theo Q-396, 2026-10-29)*
```

```java
interface HoatDongKenhNctPort {    // 014 cài; chỉ đọc; bổ sung 2026-10-18 (FR-083 (c), 014 FR-068a)
  List<BuoiSapToiView> sapToi(long nctId, LocalDate tu, LocalDate den);     // buổi của tầng người cao tuổi và toàn viện; coTen; không có đánh giá khả năng tham gia
}
interface DoGuiKenhNctPort {       // 013 cài; chỉ đọc; bổ sung 2026-10-18 (FR-083 (e), 013 FR-030a)
  List<DoGuiDangGiuView> dangGiu(long nctId);                               // Đang giữ, Đang được sử dụng: tên, trạng thái, ảnh; không có lịch sử bàn giao
}
```

Bean thiếu → phần tương ứng của kênh hiện "chưa có dữ liệu".

### 1.5. Sự kiện 012 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `m08.api.LuotOLaiDaDoi` | 011 | FR-042 (a) |
| `m11.api.SuKienTinhPhiNguon` / `SuKienTinhPhiNguonDaDoi` (`DEM_O_LAI`) | 010 | FR-042 (b), FR-043 |
| `QuanHeDaDoi(nguoiThanId, nctId, quanHeId, loai)`; `QuyenNguoiThanDaDoi(quanHeId, quyen, giaTri, tuLuc)` | 002 (xóa đệm quyền), 009, 013 | FR-006, FR-013 |
| `LuotThamDaVao`, `LuotThamDaRa`, `PhanHoiQuaHan` | 016 | FR-033, FR-063 |
| Thông báo (cài `CoYeuCauThongBao`) | 009 | FR-069 |

---

## 2. 012 cần từ module khác

| Giao diện | Module | Dùng cho | Kiểm thử thay thế |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `BanDongYApi.hienHanh`, `baoGom`; `MucSucKhoeApi.hieuLuc` | 001 | FR-003, FR-005, FR-011, FR-046 | Bản giả |
| `TaiKhoanApi.xetLaiNguoiThan` (MANDATORY); `QuyenApi`, `PhamViApi`, `NhiemVuApi.truongTangDuocGiao` | 002 (008 cài phần nhiệm vụ) | FR-006, FR-022, Quy ước Trưởng tầng | Bản thật |
| `ViTriApi.hienTai`, phòng và số giường đang sử dụng; khu nghỉ bán trú, khu tiếp khách | 003 | FR-030 (c), FR-040 | Bản giả |
| `HopDongDocApi`, `LuotVangApi`, `DieuKienTiepNhanApi` | 004 | FR-030 (d), FR-046 (4) | Bản giả |
| `CoMatBanTruApi.theoNgay`, `diemDanhVe` (MANDATORY); `KetQuaChamSocCongApi` (bổ sung theo plan này) | 005 | FR-021 Viện đưa về, FR-030 (d), FR-046 (2), FR-052 | Bản giả |
| `ThuocCongNguoiThanApi` | 006 | FR-046 (2) | Bản giả |
| `SucKhoeCongNguoiThanApi`, `KhoanhVungApi.dangChiuTheoNguoi`, `SuCoApi.theoNguoi` | 007 | FR-030 (c), FR-041, FR-046, FR-052, FR-053; sự cố tách `phanChung` / `phanSucKhoe` (007 research S-19a, sửa 2026-10-18) | Bản giả |
| `NguonYKienApi.kiemTra`; `BanDongYApi.daiDienPhapLuat` | 001 | FR-020a, FR-020c (bổ sung 2026-10-18) | Có sẵn sau 001 |
| `ThongBaoTraCuuApi.xemLuc` | 009 | FR-054 | Bản giả |
| `ChiPhiCongNguoiThanApi.tamTinh` | 010 | FR-052 | Bản giả |
| `CheDoAnApi.hienHanh`, `ThucDonChungApi` | 011 | FR-046 | Bản giả |
| *(Bổ sung 2026-10-18)* `SuatAnDuKienApi.cuaNguoiCaoTuoi(nctId, tu, den)` (`m08.api`): món, món thay thế theo bữa; không có chế độ ăn, dị ứng | 011 | FR-083 (b) | Bản giả |
| *(Bổ sung 2026-10-18)* `TaiKhoanApi.theoNguoiCaoTuoi`, phạm vi `BAN_THAN` | 002 | FR-083 → FR-086 | Có sẵn sau 002 US8 |
| *(Bổ sung 2026-10-18)* `CoMatBanTruApi.theoNgay` | 005 | FR-083 (a) | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `NhacQuaHan`, `DinhChinhApi` | 000 | Chung | Bản thật |

`KetQuaChamSocCongApi` (bổ sung vào hợp đồng 005 theo plan này):

```java
List<KetQuaNguoiThanView> ketQua(long nctId, Instant tu, Instant den);   // kết quả công việc chăm sóc đã ghi (không có ghi chú nội bộ)
TongHopAnUongView tongHop(long nctId, LocalDate tu, LocalDate den);      // tỷ lệ ăn TB, lượng nước TB; rỗng khi không có dữ liệu
```

### 2.1. Sự kiện 012 nghe

| Sự kiện (module) | Xử lý | FR |
| --- | --- | --- |
| `BanDongYDaDoi` (001) | Cập nhật hiển thị "không có tác dụng"; không đổi giá trị quyền | FR-011 |
| `LuotVangDaMo`, `BaoVangBanTruDaGhi` (004); `NguoiCaoTuoiDaChuyenTrangThai` (001) | Hủy lượt thăm trùng; nhắc kết thúc lượt ở lại | FR-036, FR-044 |
| `KhoanhVungDaTao` (007) | Hủy lượt thăm trong vùng; báo khách đang vào | FR-037 |
| `ThongBaoDaXem` (009) | Nhật ký xem sức khỏe khi bản có phần `SUC_KHOE` | FR-048 |

Mọi listener idempotent theo `khoaSuKien` (`su_kien_m10_da_xu_ly`).
