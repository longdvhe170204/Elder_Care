# Giao diện giữa module: Feature 017

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Feature 017 thêm gói `m11.sodu` vào module M11 (cùng 010). Giao diện dưới đây gồm port nội bộ M11, bản cài SPI do 004, 001 khai báo, giao diện công khai và sự kiện (research E-02). Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 1. Port nội bộ M11 (010 research P-12)

| Port | Vai trò của 017 |
| --- | --- |
| `ChiPhiChoSoDuPort` (010 cài) | Dùng `tongChuaChot(nctId)` (FR-014, FR-016, FR-021), `phiNgayCoSo(nctId, ngay)` (FR-016) |
| `SoDuMuaHoPort` (017 cài) | `soDuKhongDu(nctId, soTien, t)` = (số dư − tổng chưa chốt) < số tiền (FR-021, BR-M11-15) |

---

## 2. 017 cung cấp

### 2.1. SPI khai báo ở module thấp hơn, 017 cài

| SPI (gói khai báo) | Bản cài của 017 | Dùng bởi |
| --- | --- | --- |
| `TrangThaiDatCocPort` (`m02.api`) | `trangThai(hopDongId)`: `KHONG_YEU_CAU` / `DA_DAP_UNG` / `CHUA_DAP_UNG` kèm giao dịch và Kế toán nguồn (FR-006) | 004 FR-004, FR-031, FR-033 |
| `MucViecSauQuaDoiPort` (`m02.api`) | `ma() = QUYET_TOAN`; `KHONG_AP_DUNG` / `HOAN_THANH` / `CHUA_HOAN_THANH` (FR-023) | 004 FR-071 |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Bean `@Order` sau 013. Điều kiện `KT_H_QUYET_TOAN` (`coTheMien = true`) cho `KET_THUC_LUU_TRU` (FR-022). Không có tác động | 001 H-02, 004 T-09 |
| `LoaiYeuCauHandler` (000) | `M11.GIAO_DICH_SO_DU` (hoàn tiền, hoàn cọc, điều chỉnh, cặp chuyển tiền, đảo): `kiemTraKhiDuyet` kiểm lại FR-011, FR-014; `apDung` ghi Đã xác nhận | 000 FR-031 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | Trường tài chính `TAI_CHINH`; lưu theo CFG-M15-03 | 000 |
| `GanPhamViProvider` (002) | `M11.SO_DU`, `M11.GIAO_DICH`: người cao tuổi; `M11.SAO_KE`, `M11.CHOT_QUY`: toàn viện | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M11.SAO_KE_NHAP`: Kế toán, Quản lý viện; `M11.CHUNG_TU_SO_DU`: Kế toán, Quản lý viện; `M11.SAO_KE_XUAT`: người xuất, người thân có quyền xem chi phí với sao kê của mình | FR-029 |

### 2.2. `m11.api.SoDuApi` (đọc; 004, 012 hiển thị, 016)

```java
Optional<SoDuView> soDu(long nctId);                  // số dư, số dư cọc, tình trạng, số ngày còn đủ tiền
DieuKienQuyetToanView dieuKienQuyetToan(long nctId);  // FR-022: DAT | CHUA_DAT kèm lý do
```

### 2.3. `m11.api.SoDuThongKeApi` (016; 18.4, 18.5)

```java
Stream<SoDuTinhTrangView> tinhTrang(PhamViLoc phamVi);                  // hồ sơ Sắp hết tiền, Còn nợ
Stream<ThuChiView> thuChi(LocalDate tu, LocalDate den);                 // tổng theo loại, phương thức
Stream<ChotQuyView> chotQuy(LocalDate tu, LocalDate den);
```

### 2.4. Sự kiện 017 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `TrangThaiDatCocDaDoi(hopDongId, nctId, trangThai, giaoDichId, keToanId, thoiDiem)` | 004 (danh sách điều kiện tiếp nhận) | FR-006 |
| `DieuKienQuyetToanDaDoi(nctId, ketQua)` | 004 (màn điều kiện kết thúc) | FR-022 |
| `TinhTrangSoDuDaDoi(nctId, tinhTrang)` | 016 | FR-017 |
| Thông báo (cài `CoYeuCauThongBao`) | 009 | FR-030 |

---

## 3. 017 cần từ module khác

| Giao diện | Module | Dùng cho | Kiểm thử thay thế |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `laTrangThaiCuoi` | 001 | FR-014, FR-016, FR-024 | Bản giả |
| `HopDongDocApi` (`khoanDatCoc`, hợp đồng hiệu lực, loại giá, lịch đến bán trú); `DanhSachViecSauQuaDoiApi.capNhat`, `xem` | 004 | FR-006, FR-015, FR-016, FR-023 | Bản giả |
| `QuyenNguoiThanApi.nguoiDaiDien`, `laNguoiDaiDien`, `quyenCoTacDung(XEM_CHI_PHI)` | 012 | FR-003, FR-014, FR-018, FR-026 | Bản giả |
| `NhatKyApi`, `ThamSoApi` (CFG-M11-04, 05, CFG-M02-09, CFG-M13-06, CFG-M14-01, CFG-M15-03 → 06), `YeuCauPheDuyetApi`, `NhacQuaHan` | 000 | Chung | Bản thật |

### 3.1. Sự kiện 017 nghe

| Sự kiện (module) | Xử lý | FR |
| --- | --- | --- |
| `HopDongDaDoiTrangThai` (004): → Chờ ký | Mở sổ, cấp mã nộp tiền (hồ sơ chưa có sổ) | FR-001 |
| `HopDongDaDoiTrangThai` → Đã hủy trước Hiệu lực; `NguoiCaoTuoiDaChuyenTrangThai` Hủy tiếp nhận (001) | Bắt đầu nhắc hoàn cọc | FR-015a |
| `KhoanCocDaDoi` (004) | Tính lại trạng thái đặt cọc | FR-006 |
| `HoSoKetThucDa*`, `QuaDoiDaGhiNhan` (004) | Tính điều kiện quyết toán, mục việc sau qua đời | FR-022, FR-023 |
| `BangChiPhiDaChot` (010) | Giao dịch thanh toán bảng (một lần mỗi bảng) | FR-012 |
| `DeNghiMuaHo*`, `KhoanChiPhiDaDoi` (010) | Không ghi; dấu mua hộ tính lúc 010 hỏi | FR-021 |

Mọi listener idempotent theo `khoaSuKien` (`su_kien_m11sd_da_xu_ly`).
