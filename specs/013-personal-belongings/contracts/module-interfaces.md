# Giao diện giữa module: Feature 013

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `m12.api` và bản cài các SPI do module thấp hơn khai báo (research K-02). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong` theo 000, 001, 002. Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 1. 013 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M12 cài

| SPI (gói khai báo) | Bản cài của 013 | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Bean `@Order` sau 012. Điều kiện `KT_B_DO_GUI` (`coTheMien = true`) cho `KET_THUC_LUU_TRU` theo `DieuKienDoGui` (K-08); căn cứ liệt kê đồ chặn và đồ Thất lạc có sự cố mở "chặn qua điều kiện (d)". Không có tác động | 001 H-02, 004 T-09 |
| `MucViecSauQuaDoiPort` (`m02.api`) | `ma() = DO_GUI`; `KHONG_AP_DUNG` / `HOAN_THANH` / `CHUA_HOAN_THANH` (FR-026) | 004 FR-071 |
| `DoGuiBanGiaoPort` (`m09.api`) | `dangGiu(nhanVienIds)`: đồ có giá trị Đang giữ, người giữ, người cao tuổi, vị trí | 008 FR-039 (g) |
| `LoaiYeuCauHandler` (000) | `M12.XU_LY_DO_KHONG_NGUOI_NHAN`: `choTuDuyet = false`, `kiemTraDieuKienApDung` loại đồ đã đổi trạng thái, `apDung` ghi bản ghi `XU_LY` (K-09) | 000 FR-031 |
| `DinhChinhDuocProvider` (000) | `M12.BAN_GIAO_DO_GUI` (không gắn tầng; giới hạn FR-011a; `sauKhiDinhChinh` tính lại đồ), `M12.PHIEU_KIEM_KE` | 000 FR-026 (c) |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model; bản ghi bàn giao thuộc danh sách bảo vệ BR-M15-03 | 000 |
| `GanPhamViProvider` (002) | `M12.DO_GUI`, `M12.XAC_NHAN_NGUOI_NHAN`, `M12.DONG_Y_TU_GIU`: người cao tuổi; `M12.PHIEU_KIEM_KE`: toàn viện | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M12.ANH_DO_GUI`: Hành chính, Điều dưỡng (phạm vi), Quản lý viện, Trưởng tầng (phạm vi), người thân có `xemDoGui`; `M12.BAN_KY`, `M12.BIEN_BAN`: Hành chính, Quản lý viện | NFR-08, BR-M12-09 |

### 1.2. `m12.api.DoGuiApi` (đọc; 004 hiển thị căn cứ, 016)

```java
DieuKienDoGuiView dieuKien(long nctId);                 // FR-025: DAT | CHUA_DAT, danh sách đồ chặn, đồ Thất lạc có sự cố mở
List<DoGuiView> theoNguoiCaoTuoi(long nctId);           // trạng thái, người giữ, vị trí, bản ghi gần nhất (SC-008)
```

### 1.3. `m12.api.DoGuiThongKeApi` (016; Q-194 giai đoạn sau)

```java
Stream<DoGuiBaoCaoView> theoKy(PhamViLoc phamVi, LocalDate tu, LocalDate den);   // thất lạc, hư hỏng, chưa trả sau kết thúc
Stream<PhieuKiemKeView> kiemKe(String maKy);
```

### 1.4. Sự kiện 013 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `DoGuiDaDoiTrangThai(doGuiId, nctId, tu, den, banGiaoId, thoiDiem)` | 016; 004 (làm mới màn điều kiện) | FR-011, FR-025 |
| Thông báo (cài `CoYeuCauThongBao`) | 009 | FR-032 |

013 không phát sự kiện để 007 hủy sự cố khi bản ghi bàn giao bị Hủy ghi nhận (Q-307; plan, điểm báo lại 1); 013 gửi thông báo cho người xử lý sự cố.

---

## 2. 013 cần từ module khác

| Giao diện | Module | Dùng cho | Kiểm thử thay thế |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `thoiDiemTrangThaiCuoi` | 001 | FR-006, FR-027, FR-027a | Bản giả |
| `QuyenApi`, `PhamViApi`; `TaiKhoanApi.theoNhanVien` | 002 | FR-007, FR-014, FR-029 | Bản thật |
| `ViTriApi.hienTai`, `tangTaiNgay` | 003 | FR-028 | Bản giả |
| `DanhSachViecSauQuaDoiApi.capNhat` | 004 | FR-026 | Bản giả |
| `SuCoLenhApi.ghiNhan`, `themDienBien` (MANDATORY); `SuCoApi.xem` | 007 | FR-021, FR-024, FR-025, bảng (Tìm thấy) | Bản giả ghi lại lệnh |
| `CaTrucApi.dangTrongCa`, `nguoiPhuTrachCa` | 008 | FR-014, FR-014a | Bản giả |
| `ThongBaoTraCuuApi` (người đã nhận thông báo gốc) | 009 | FR-011a | Bản giả |
| `QuyenNguoiThanApi.laNguoiDaiDien`, `duocPhepDonTai`, `giayTo`, `xemDoGui`, `nguoiDaiDien`, `nguoiLienHeChinh`; `NguoiDaiDienPort` | 012 | FR-005, FR-015a, FR-016, FR-017, FR-030, FR-032 | Bản giả |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `YeuCauPheDuyetApi`, `NhacQuaHan` | 000 | Chung | Bản thật |

### 2.1. Sự kiện 013 nghe

| Sự kiện (module) | Xử lý | FR |
| --- | --- | --- |
| `CaDaKetThuc` (008) | Nhắc nhân viên còn giữ đồ có giá trị và Người phụ trách ca | FR-014a |
| `DaChuyenGiuong` (003) | Đổi tầng: báo Hành chính, điều dưỡng phụ trách tầng mới | FR-028 |
| `HoSoKetThucDaLap` (004) | Còn đồ chưa trả → báo người đại diện | FR-032 |
| `NguoiCaoTuoiDaChuyenTrangThai` (001) | Vào trạng thái cuối: bắt đầu nhắc CFG-M12-02 (trừ Qua đời, theo 004 FR-072) | FR-027 |
| `QuanHeDaDoi`, `QuyenNguoiThanDaDoi` (012) | Không đổi dữ liệu; quyền xét lại lúc dùng | FR-017, FR-030 |

Mọi listener idempotent theo `khoaSuKien` (`su_kien_m12_da_xu_ly`).
