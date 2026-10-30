# Giao diện giữa module: Feature 019

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Feature 019 thêm gói vào hai module: `billing.muasam`, `roombedallocation.taisan`, `roombedallocation.lichxe` (research W-01, W-02). Gói `nutrition.kho` bỏ theo Q-392. Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 1. Port nội bộ đã khai cho 019

| Port (module, người khai) | Vai trò của 019 |
| --- | --- |
| `GiuongTheoTaiSanPort` (M03, 003) | Dùng: `kiemTraLenhTaiSan`, `apLenhTaiSan`, `daGanDauChoChuyenNguoi`, `daGoDauChoChuyenNguoi`, `giuongTrongPhuHopChoNguoiDangNam` |
| `TaiSanGiuongPort` (M03, 003) | Cài: `ghiTangChoGiuong` (trong lệnh Tạo giường), `mangDauChoChuyenNguoi`, `giuongMangDau` |

---

## 2. Giao diện mới

### 2.1. SPI `roombedallocation.api.YeuCauMuaSamPort` (M11 cài cho M03; `billing.api.YeuCauMuaSamApi` cho kho bỏ theo Q-392)

```java
// M11 cài
List<DieuKien> kiemTraDuocDung(long yeuCauId, long boPhanId, LoaiMuaSam loai);
    // FR-008c: yêu cầu Đã duyệt, cùng bộ phận, loại phù hợp (TAI_SAN | DICH_VU)
void ganNguon(long yeuCauId, LoaiNguonMuaSam loaiNguon, long nguonId, long soTien);
    // MANDATORY: TAI_SAN | BAO_TRI; lỗi khi yêu cầu không còn Đã duyệt hoặc nguồn đã gắn (DBR-36)
```

M03 không gọi M11 trực tiếp vì M11 → M02 → M03; `YeuCauMuaSamPort` khai ở `roombedallocation.api`, M11 cài. Bean thiếu → lệnh ghi tăng tài sản mua mới, bảo trì thuê ngoài có chi phí bị chặn (mặc định an toàn).

### 2.2. `roombedallocation.api.LichXeApi`, `LichXeLenhApi` (014 gọi)

```java
interface LichXeApi {
  boolean coLichDaDat(long chuyenId, Instant gioRoi);                         // FR-019
  KetQuaKiemTraDoi kiemTraDoi(long chuyenId, Instant roiMoi, Instant veMoi);  // Q-239
}
interface LichXeLenhApi {   // MANDATORY, trong lệnh của 014
  void theoChuyen(long chuyenId, LoaiThayDoiChuyen loai, Instant roiDuKien, Instant veDuKien, Optional<Instant> thucTe);
      // DOI | HUY | ROI_VIEN_DAU | GIA_HAN | HUY_ROI_VIEN_CUOI | KET_THUC_VE (FR-018, Q-231, Q-236, Q-240)
}
```

Thay cho `LichXeChuyenPort` mà plan 014 khai ở `care.api` (đã sửa hợp đồng 014): M03 cài SPI ở `care.api` sẽ tạo vòng M03 ↔ M04.

### 2.3. Giao diện thống kê cho 016

```java
// roombedallocation.api.TaiSanThongKeApi
Stream<TaiSanQuaHanView> quaHan();                                  // bảo trì, đăng kiểm, bảo hiểm (18.5)
Stream<LichXeView> lichXe(LocalDate tu, LocalDate den);
// billing.api.MuaSamThongKeApi
Stream<YeuCauMuaSamView> theoBoPhan(LocalDate tu, LocalDate den);   // trạng thái, số được duyệt, thực tế (016 FR-055a)
```

---

## 3. SPI, provider 019 cài

| SPI | Bản cài | Dùng bởi |
| --- | --- | --- |
| `LoaiYeuCauHandler` (000) | `M03.THANH_LY_TAI_SAN`: `quyenTao` Phụ trách bộ phận quản lý tài sản, `quyenDuyet` Quản lý viện, `choTuDuyet = false`; `kiemTraKhiDuyet` theo FR-015 với giường; `apDung` chuyển `DA_THANH_LY` | 000 FR-031 |
| `DinhChinhDuocProvider` (000) | `M03.TAI_SAN_LICH_SU` (vị trí, bảo trì, hư hỏng), `M03.LICH_XE` | 000 FR-026 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | Nguyên giá, chi phí `TAI_CHINH` | 000 |
| `YeuCauThuocCungUngPort` (`medication.api`, 006) *(bổ sung 2026-10-11, Q-337)* | Trả dòng của yêu cầu `THUOC` đã ghi đã mua (Chờ đối chiếu, Đã đối chiếu, Lệch), id nhân viên của người lập, `bo_phan_id` của yêu cầu (sửa 2026-10-11, lượt 3: 006 báo Phụ trách bộ phận hiện tại qua 009); khác thì rỗng | 006 FR-054, BR-M11-17 |
| `GanPhamViProvider` (002) | `M11.MUA_SAM`: bộ phận (với dòng loại `THUOC` do Điều dưỡng lập: thêm người cao tuổi của dòng, Q-337); `M03.TAI_SAN`: bộ phận quản lý và tầng của vị trí; `M03.LICH_XE`: toàn viện | 002 FR-034, FR-019f |
| `QuyenTepProvider` (`common.tep`) | `M11.CHUNG_TU_MUA_SAM`: người lập, Phụ trách bộ phận, Kế toán, Quản lý viện | NFR-08 |

---

## 4. 019 cần từ module khác

| Giao diện | Module | Dùng cho |
| --- | --- | --- |
| `NhiemVuApi.boPhanCua`, `laPhuTrachBoPhan`, `phuTrachBoPhan` (`administration.taikhoan.api`, 008 cài) | 002/008 | Quyền Phụ trách bộ phận, người nhận thông báo, chặn khi bộ phận chưa có Phụ trách (Q-292) |
| `HoSoNhanVienApi` (`administration.taikhoan.api`); `TaiKhoanApi.theoNhanVien` | 002/008 | Người lái; báo người lái có tài khoản (Q-298) |
| `DiNguyenApi` | 001 | FR-001 |
| `DonThuocApi.donNguonVienChuaKetThuc`, `TiepNhanThuocCungUngApi.theoYeuCau` (`medication.api`) *(bổ sung 2026-10-11)* | 006 | FR-008d: gắn dòng loại `THUOC` với đơn nguồn viện; hiển thị "Đã nhận" |
| `NhatKyApi`, `ThamSoApi` (CFG-M03-02, 09, CFG-M11-07 → 09), kho tệp | 000 | Chung |

Sự kiện 019 phát: thông báo (cài `CoYeuCauThongBao`) theo FR-021, gồm Khẩn cấp cho báo hỏng giường có người mức "mất an toàn"; `TaiSanDaDoiTrangThai`, `YeuCauMuaSamDaDoi` cho 016.
