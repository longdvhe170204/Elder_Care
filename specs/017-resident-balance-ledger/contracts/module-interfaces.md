# Giao diện giữa module: Feature 017

**Ngày**: 2026-10-22 (lập lại theo spec viết lại) | **Plan**: [../plan.md](../plan.md)

Feature 017 thêm gói `billing.sodu` vào module M11 (cùng 010). Giao diện dưới đây gồm port nội bộ M11, bản cài SPI do 004, 001 khai báo, giao diện công khai và sự kiện (research E-02). Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi. Chữ ký viết dạng Java rút gọn với tên logic tiếng Việt; tên mã theo glossary khi dịch hợp đồng.

---

## 1. Port nội bộ M11 (010 research P-12)

| Port | Vai trò của 017 |
| --- | --- |
| `ChiPhiChoSoDuPort` (010 cài) | Dùng `tongChuaChot(nctId)` (FR-014, FR-016, FR-021), `phiNgayCoSo(nctId, ngay)` (FR-016), `bangDaChot(bangId)` (FR-031, FR-032, FR-034; thêm 2026-10-22) |
| `SoDuMuaHoPort` (017 cài) | `soDuKhongDu(nctId, soTien, t)` = (số dư − tổng chưa chốt) < số tiền (FR-021, BR-M11-15) |

Chỉ `billing.sodu` dùng `ChiPhiChoSoDuPort` (ArchUnit của 010).

---

## 2. 017 cung cấp

### 2.1. SPI khai báo ở module thấp hơn, 017 cài

| SPI (gói khai báo) | Bản cài của 017 | Dùng bởi |
| --- | --- | --- |
| `TrangThaiDatCocPort` (`admission.api`) | `trangThai(hopDongId)`: `KHONG_YEU_CAU` / `DA_DAP_UNG` / `CHUA_DAP_UNG` kèm giao dịch làm đổi gần nhất và Kế toán lập (FR-006) | 004 FR-004, FR-031, FR-033 |
| `MucViecSauQuaDoiPort` (`admission.api`) | `ma() = QUYET_TOAN`; `KHONG_AP_DUNG` / `HOAN_THANH` / `CHUA_HOAN_THANH` kèm lý do (FR-023) | 004 FR-071 |
| `ThamGiaChuyenTrangThai` (`resident.api`) | Bean `@Order` sau 013. Cho `KET_THUC_LUU_TRU`: `KT_H_QUYET_TOAN` (ba ý tiền, `coTheMien = true`), `KT_H_XAC_NHAN_QUYET_TOAN` (không còn Chờ duyệt, đã xác nhận quyết toán và số liệu không đổi, `coTheMien = false`) (FR-022, BR-M11-22; 004 T-09). Không có tác động | 001 H-02, 004 FR-063, FR-064 |
| `LoaiYeuCauHandler` (000) | `M11.GIAO_DICH_SO_DU` (hoàn tiền, hoàn cọc, điều chỉnh): `kiemTraKhiDuyet` kiểm lại FR-011, FR-014; `apDung` ghi Đã xác nhận | 000 FR-031 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | Trường tài chính `TAI_CHINH`; lưu theo CFG-M15-03 | 000 |
| `GanPhamViProvider` (002) | `M11.SO_DU`, `M11.GIAO_DICH_SO_DU`, `M11.PHIEU_BAO_PHI`, `M11.BANG_QUYET_TOAN`: theo người cao tuổi | 002 FR-034 |
| `KiemTraGiaTriThamSo` (`administration.thamso.api`, 000; SPI mới, bổ sung 2026-10-22) | Bản cài cho CFG-M11-10: `maBin` có trong `ngan_hang` và còn hiệu lực, `soTaiKhoan` chỉ chữ số, `chuTaiKhoan` không rỗng (research E-04) | 000 lệnh đổi giá trị tham số (FR-016 của 000) *(R-15: `ngan_hang` → `danh_muc_chung`)* |
| `QuyenTepProvider` (`common.tep`) | `M11.PHIEU_BAO_PHI`, `M11.BANG_QUYET_TOAN`, `M11.BAN_KY_QUYET_TOAN`: Kế toán, Quản lý viện, người thân có quyền xem chi phí của người cao tuổi đó; `M11.FILE_XUAT`: người xuất và vai trò xem được loại xuất đó (research E-12) | FR-028, FR-037 |

### 2.2. `billing.api.SoDuApi` (đọc; 004 hiển thị, 012, 016)

```java
Optional<SoDuView> soDu(long nctId);                  // số dư, số dư cọc, tình trạng, số ngày còn đủ tiền, mã nộp tiền
DieuKienQuyetToanView dieuKienQuyetToan(long nctId);  // FR-022: hai mã, mỗi mã DAT | CHUA_DAT kèm lý do; phiên bản bảng hiện hành
List<ChungTuView> chungTu(long nctId);                // FR-038: phiếu báo phí và bảng quyết toán hiện hành (loại, mã, ngày, kỳ, phiên bản, tệp)
```

### 2.3. `billing.api.SoDuThongKeApi` (016; 18.4, 18.5)

```java
Stream<SoDuTinhTrangView> tinhTrang(PhamViLoc phamVi);                   // hồ sơ Sắp hết tiền, Còn nợ (dashboard)
BaoCaoThuChiView thuChi(LocalDate tu, LocalDate den);                    // tổng nộp, tổng đã trừ theo bảng, tổng hoàn, danh sách còn nợ (Q-386)
```

Bỏ `chotQuy` của bản cũ (Q-385).

### 2.4. Sự kiện 017 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `TrangThaiDatCocDaDoi(hopDongId, nctId, trangThai, giaoDichId, keToanId, thoiDiem)` | 004 (danh sách điều kiện tiếp nhận) | FR-006 |
| `DieuKienQuyetToanDaDoi(nctId)` | 004 (màn điều kiện kết thúc) | FR-022 |
| `TinhTrangSoDuDaDoi(nctId, tinhTrang)` | 016 | FR-017 |
| `ChungTuDaPhatHanh(nctId, loai, id)` (`PHIEU_BAO_PHI`, `BANG_QUYET_TOAN`) | 012 (làm mới danh sách chứng từ trên cổng) | FR-031, FR-034, FR-038 |
| Thông báo (khuôn `common.notification`) | 009 | FR-030 |

---

## 3. 017 cần từ module khác

| Giao diện | Module | Dùng cho | Kiểm thử thay thế |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `laTrangThaiCuoi`, `nhanDang` | 001 | FR-014, FR-016, FR-024, chứng từ | Bản giả |
| `HopDongDocApi` (`khoanDatCoc`, hợp đồng hiệu lực, loại giá, lịch đến bán trú); `HoSoKetThucDocApi` (`dangChuanBi`, `thongTinQuyetToan`, `coNgoaiLeQuyetToanDaDuyet`; 2026-10-22); `DanhSachViecSauQuaDoiApi.xem`, `capNhat` | 004 | FR-006, FR-015, FR-016, FR-022 → FR-024, FR-034 | Bản giả |
| `QuyenNguoiThanApi.quyenCoTacDung(XEM_CHI_PHI)`, `laNguoiDaiDien`, `nguoiDaiDien` | 012 | FR-003, FR-014, FR-018, FR-026, FR-038 | Bản giả |
| `PdfBieuMau`, `ExcelBieuMau` (`common.bieumau`, 007 T104); `FileApi.upload`, `attach` (`common.tep`; thêm kiểu `.xlsx` cho tệp hệ thống tạo) | `common` | FR-025, FR-031 → FR-037 | Bản thật; `S3Client` giả |
| `NhatKyApi`, `ThamSoApi` (CFG-M11-04, 05, 10, 11, CFG-M02-09, CFG-M14-01, CFG-M15-03 → 06), `YeuCauPheDuyetApi` | 000 | Chung | Bản thật |

### 3.1. Sự kiện 017 nghe

| Sự kiện (module) | Xử lý | FR |
| --- | --- | --- |
| `HopDongDaDoiTrangThai` (004): → Chờ ký | Mở sổ, cấp mã nộp tiền (hồ sơ chưa có sổ) | FR-001 |
| `HopDongDaDoiTrangThai` → Đã hủy trước Hiệu lực; `NguoiCaoTuoiDaChuyenTrangThai` Hủy tiếp nhận (001) | Đặt `can_hoan_coc_tu`, nhắc hoàn cọc | FR-015a |
| `KhoanCocDaDoi` (004) | Tính lại trạng thái đặt cọc | FR-006 |
| `HoSoKetThucDa*`, `QuaDoiDaGhiNhan`, `NgoaiLeKetThucDaDuyet` (004) | Tính lại điều kiện quyết toán, mục việc sau qua đời | FR-022, FR-023 |
| `BangChiPhiDaChot` (010) | Giao dịch thanh toán bảng (một lần mỗi bảng) | FR-012 |
| `DeNghiMuaHo*`, `KhoanChiPhiDaDoi` (010) | Không ghi; dấu mua hộ tính lúc 010 hỏi | FR-021 |

Mọi listener idempotent theo khóa sự kiện (`su_kien_m11sd_da_xu_ly`). *(R-15: `su_kien_m11sd_da_xu_ly` → `moc_da_nhac`)*
