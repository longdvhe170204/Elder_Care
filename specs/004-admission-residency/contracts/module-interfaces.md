# Giao diện giữa module: Feature 004

**Ngày**: 2026-10-07 | **Plan**: [../plan.md](../plan.md)

Hợp đồng trong backend, ở gói `com.cambae.backend.m02.api`. Module khác chỉ dùng các giao diện này, không đọc bảng của 004 (constitution X). Chữ ký viết dạng Java rút gọn để mô tả hợp đồng; mã thật viết ở giai đoạn triển khai.

Chiều phụ thuộc theo research T-02: 004 phụ thuộc 000, 001, 002, 003. Các module 005, 010, 011, 012, 017, 018 phụ thuộc 004. Chỗ 004 cần dữ liệu của module phụ thuộc nó thì 004 khai báo SPI (mục 2.1), module đó cài.

---

## 1. 004 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M02 cài

| SPI (gói khai báo) | Chữ ký (giữ như plan đã khai báo) | Dùng bởi |
| --- | --- | --- |
| `YeuCauDoiMucApi` (`m01.api`) | `void thayTheBoiDanhGia(long nctId, long danhGiaId)` (MANDATORY: chuyển yêu cầu đổi mức chưa kết thúc sang `DUOC_THAY_THE` qua `chuyenMoRong` của 000); `boolean coYeuCauDoiMucChuaKetThuc(long nctId)` | 001 FR-036, FR-036a |
| `HopDongApi` (`m01.api`) | `Optional<LoaiLuuTru> loaiLuuTruHienHanh(long nctId, LocalDate ngay)`; `List<KhoangLoaiLuuTru> lichSuLoaiLuuTru(long nctId)` | 001 FR-009, FR-052, FR-053 |
| `LuotVangApi` (`m01.api`) | `Optional<LuotVangView> dangMo(long nctId)` (kèm mốc dự kiến trở lại hiện hành) | 001 FR-050 |
| `KetThucLuuTruApi` (`m15.taikhoan.api`) | `Optional<Instant> thoiDiemTrangThaiCuoi(long nctId)` | 002 FR-049, job `KhoaSauKetThucLuuTru` |
| `NguonLuuTruChoPhanBo` (`m03.api`) | Theo [003 module-interfaces](../../003-room-bed-allocation/contracts/module-interfaces.md) mục 2.1 | 003 FR-018, FR-024, FR-030, FR-015, 004 FR-016 |
| `DemLichDenBanTru` (`m03.api`) | Theo 003 mục 2.1; cách đếm ở research T-10 | 003 FR-040, Ngừng hiệu lực khu nghỉ |
| `NguonYKienApi` (`m01.api`) | `kiemTra(nctId, nguon)` cho ý kiến về việc vào ở (bổ sung 2026-10-18, FR-001) | 001 mục 1.8b |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Bean `@Order(1)` (chạy đầu tiên; `canhBao` cho `HOAN_TAT_TIEP_NHAN` trả `CHUA_GHI_Y_KIEN_VAO_O`, `NCT_KHONG_DONG_Y_VAO_O`, bổ sung 2026-10-18) cho `HOAN_TAT_TIEP_NHAN`, `HUY_TIEP_NHAN`, `CHO_TAM_VANG`, `CHUYEN_VIEN`, `GHI_NHAN_TRO_VE`, `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`; cài `mienDieuKien` cho `KET_THUC_LUU_TRU` | Research T-03, T-08, T-09, T-13 |

**Kiểu `chiTiet` 004 sở hữu** (trường `chiTiet` của lệnh trạng thái ở api-001):

| Lệnh | Trường |
| --- | --- |
| `CHO_TAM_VANG` | `loaiVang`, `duKienTroLai`, `nguoiDonId`, `banGhiDonId`, `nhanVienBanGiaoId`, `mangThuoc`, `lyDoChuan` (tùy chọn) |
| `CHUYEN_VIEN` | `donViDuaDi`, `duKienTroLai` (tùy chọn), `nhanVienBanGiaoId` (cùng `canCu` theo 001) |
| `GHI_NHAN_TRO_VE` | `thoiDiemVe` (mặc định là thời điểm lệnh) |
| `KET_THUC_LUU_TRU` | `hoSoKetThucId`, `thoiDiemHieuLuc` |
| `GHI_NHAN_QUA_DOI` | `thoiDiemQuaDoi`, `diaDiem`, `nguoiPhatHien`, `nguoiXacNhanId`, `nguyenNhan`, `tepBangChungId` |
| `HUY_TIEP_NHAN` | `hoSoChoId`, `lenhHoSoCho` (`TU_CHOI` hoặc `HUY_CHO`): chỉ có khi lệnh khởi đầu từ danh sách chờ (FR-002) |

### 1.2. `HopDongDocApi` (đọc; 003, 005, 010, 012, 017, 018)

```java
Optional<NoiDungHopDongView> noiDungTaiNgay(long nctId, LocalDate ngay);     // FR-024; phiên bản nội dung, kèm đơn giá đã ghi
Optional<HopDongView> hopDong(long hopDongId);                              // kèm trạng thái, dấu quá hạn
List<HopDongView> cuaNguoiCaoTuoi(long nctId);
Optional<KhoanDatCocView> khoanDatCoc(long hopDongId);                      // 017 (FR-030)
List<LichDenView> lichDenTaiNgay(long nctId, LocalDate ngay);               // 005 điểm danh, 011 suất
```

Trường tài chính mang `@NhomTruong(TAI_CHINH)`; 012 hiển thị cho người đại diện theo chú thích ⁷.

### 1.3. `DonGiaApi` (010; 004 tự dùng)

```java
Optional<DonGiaView> donGiaTaiNgay(long dichVuId, LocalDate ngay, boolean danhDauThamChieu); // MANDATORY khi danhDauThamChieu; BR-M11-02
Optional<DonGiaView> donGiaHopDong(long nctId, long dichVuId, LocalDate ngay);              // đơn giá đã ghi trong nội dung hợp đồng (FR-036)
Optional<DichVuView> dichVuChamSoc(MaMucChamSoc muc);                                       // Q-297
Optional<DichVuView> phiLuuTruTheoLoaiPhong(long loaiPhongId);
Optional<DichVuView> thuocNguonVien(long thuocId);
```

Không có phiên bản hiệu lực tại ngày thì trả rỗng; 010 ghi khoản "thiếu đơn giá" (010 FR-012).

### 1.4. `NgayLuuTruApi` (010; FR-052, FR-060, FR-074)

```java
List<NgayLuuTruView> theoKhoang(long nctId, LocalDate tu, LocalDate den);
    // mỗi ngày trong thời gian tính phí: hopDongId, phienBanNoiDungId, coMat | VANG, luotVangId, soThuTu, heSo,
    // dau (CHO_QUYET_DINH, KHONG_CO_DONG, CHUA_VAO_O, QUA_HAN_HOP_DONG), ngoaiThoiGianTinhPhi (sau mốc dừng, sau ngày qua đời)
List<BuoiBanTruView> buoiBanTru(long nctId, LocalDate tu, LocalDate den);
    // mỗi buổi có lịch: trạng thái có mặt (005), hệ số (FR-060), ngày khu bán trú nghỉ (FR-060a); buổi chưa xác định không có hệ số
Optional<LocalDate> mocDungTinhPhi(long nctId);   // ngày kết thúc dự kiến (FR-066), ngày qua đời (FR-070), ngày Hủy tiếp nhận (FR-075a)
```

### 1.5. `DieuKienTiepNhanApi` (012 cổng người thân; 016)

```java
DanhSachDieuKienView dieuKienTiepNhan(long nctId);       // FR-004 (a) → (f) (thêm (f) DBR-02, Q-343), Đạt / Chưa đạt / Không áp dụng, căn cứ
TienTrinhView tienTrinh(long luotDangKyId);              // FR-003
DanhSachDieuKienView dieuKienKetThuc(long hoSoKetThucId); // FR-063, kèm ngoại lệ (012 FR-068)
```

### 1.5a. `LuuTruThongKeApi` (016; bổ sung theo plan 016, research O-10)

```java
Stream<LoaiLuuTruTaiNgayView> loaiLuuTruTaiNgay(PhamViLoc phamVi, LocalDate ngay);  // 016 FR-022: hợp đồng hiệu lực (kể cả quá hạn), chờ hiệu lực, chưa xác định
Stream<HoSoChoView> danhSachCho(LocalDate tu, LocalDate den);                       // 016 FR-025: trạng thái, loại lưu trú, mức dự kiến, thời gian chờ, lý do rời
```

### 1.6. `DanhSachViecSauQuaDoiApi`

```java
void capNhat(long nctId);   // module sở hữu gọi sau lệnh của mình (010, 013, 006, 007, 017); 004 hỏi lại MucViecSauQuaDoiPort
DanhSachViecView xem(long nctId);
```

### 1.7. SPI 004 cài cho 000, 002

| SPI | Loại / đối tượng | Ghi chú |
| --- | --- | --- |
| `LoaiYeuCauHandler` (000) | `M02.THAY_DOI_LUU_TRU` | Research T-06; cài `kiemTraKhiDuyet`, `khiDuyet`, `gioiHanVaiTroDuyet` (thay đổi nhỏ, FR-043, Q-338; hàm này bổ sung vào SPI của 000 theo plan này); chuyển mở rộng `DUOC_THAY_THE` |
| | `M02.DIEU_CHINH_DIEM` | `apDung` thêm điều chỉnh, tính lại điểm (FR-011) |
| | `M02.DUYET_DIEU_KHOAN_HOP_DONG` | `kiemTraDieuKienApDung`: hợp đồng còn Nháp, băm nội dung khớp (FR-025a) |
| | `M02.NGOAI_LE_KET_THUC` | `quyenTao` gồm Kế toán khi điều kiện là `KT_H_QUYET_TOAN`; `apDung` thêm `ngoai_le_dieu_kien` (FR-064) |
| `DinhChinhDuocProvider` (000) | `M02.LUOT_VANG`, `M02.BAN_GHI_QUA_DOI`, `M02.QUYET_DINH_GIU_GIUONG`, `M02.BAN_GIAO_NCT`, `M02.LICH_SU_DIEM` | `tangKhuVuc` rỗng (không gắn tầng); `sauKhiDinhChinh` của `M02.LUOT_VANG` tính lại ngày vắng (FR-061) |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 13, 14 | |
| `GanPhamViProvider` (002) | Mọi loại của 004 | Trả `DoiTuong` với `nguoiCaoTuoiId` |
| `QuyenTepProvider` (`common.tep`) | `M02.BAN_SCAN_HOP_DONG`, `M02.BANG_CHUNG_QUA_DOI` | Người tải bằng chứng qua đời được xem lại tệp của mình (FR-069) |

### 1.8. Sự kiện 004 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `GiuongTrongCanChonHoSo(giuongId, soHoSoPhuHop)` | 009 (Hành chính, Nhẹ) | FR-013, FR-015 |
| `LechMucHopDong(nctId, mucDanhGia, mucHopDong, trangThaiHopDong)` | 009 (Hành chính, Trung bình, Q-302) | FR-025b |
| `HopDongDaDoiTrangThai(hopDongId, nctId, tu, den, lyDo)`; `KhoanCocDaDoi(hopDongId, khoanMoi)` | 017, 018, 010, 012 | FR-033a, FR-033b |
| `TiepNhanHoanTat(nctId, hopDongId, loaiLuuTru, ngayBatDau, ngayKetThucDuKien)` | 018 | FR-033b |
| `PhuLucDaApDung(phuLucId, hopDongId, nctId, loaiThayDoi, ngayHieuLuc)` | 010, 018, 005 (báo điều dưỡng phụ trách xem xét kế hoạch), 011 | FR-042, FR-033b |
| `LuotVangDaMo`, `LuotVangDaGiaHan`, `LuotVangDaDong` (`luotVangId`, `nctId`, mốc) | 001, 006, 011, 010 | FR-048, FR-049 |
| `NgayLuuTruDaDoi(nctId, tuNgay, denNgay, nguyenNhan)` | 010 | FR-052, FR-056, FR-061 |
| `CanQuyetDinhGiuGiuong(luotVangId, soNgay, dongChinhSach)`; `DenNgayXemXetGiuGiuong(luotVangId)` | 009 (Quản lý viện) | FR-055, FR-056, FR-061 |
| `KhongCoDongChinhSach(luotVangId, ngay)` | 009 (Quản lý viện, Trung bình) | FR-053 |
| `HopDongQuaHanBaoQuanLy(hopDongId, soNgay)`; các lần nhắc `NhacM02(loai, doiTuongId)` | 009 | FR-017, FR-021, FR-026, FR-029, FR-066a, FR-072, FR-076 |
| `DatCocChuaDapUngSauLuuTru(hopDongId)` | 009 (Quản lý viện) | FR-032 |
| `YeuCauNguoiDaiDienCanHoanThien(yeuCauId)` | 009 (Hành chính) | FR-039 |
| `HoSoKetThucDaLap`, `…DaDoiNgay`, `…DaHuy`, `…HoanTat` (`hoSoKetThucId`, `nctId`, ngày) | 010, 017, 018, 012 | FR-033a, FR-066 |
| `QuaDoiDaGhiNhan(nctId, thoiDiem, banGhiId)` | 017, 018, 012 | FR-033a, FR-070 |
| `NgayNghiBanTruDaDoi(ngay, hieuLuc)`; `BaoVangBanTruDaGhi(nctId, ngay, buoi, duHan)` | 005, 010, 011 | FR-060, FR-060a |
| `PhienBanDonGiaDaTao(dichVuId, hieuLucTu, hieuLucDen)` (bổ sung theo plan 010): khi tạo phiên bản đơn giá, kể cả hiệu lực lùi cho khoảng chưa có phiên bản (DBR-08) | 010 (tính lại khoản thiếu đơn giá, 010 FR-012) | FR-034 → FR-036 |

---

## 2. 004 cần từ module khác

### 2.1. SPI 004 khai báo ở `m02.api`, module khác cài

```java
interface NguoiDaiDienPort {          // 012 cài
  boolean laNguoiDaiDienHieuLuc(long nguoiThanId, long nctId, LocalDate ngay);   // DBR-02, FR-021, FR-039
  boolean coNguoiDaiDien(long nctId);                                            // lập hợp đồng
}
interface BanGhiDonPort {             // 012 cài
  Optional<BanGhiDonView> banGhiDonHopLe(long nctId, long nguoiDonId, Instant thoiDiem); // FR-048; 012 FR-021, FR-024, FR-026
  void daDung(long banGhiDonId, String lenhNguon, long nguonId);
      // bổ sung theo plan 012: MANDATORY, trong lệnh Cho tạm vắng và bàn giao khi Kết thúc lưu trú; lần hai → BAN_GHI_DON_DA_DUNG
}
interface TrangThaiDatCocPort {       // 017 cài
  TrangThaiDatCoc trangThai(long hopDongId);   // CHUA_DAP_UNG | DA_DAP_UNG | KHONG_YEU_CAU, kèm căn cứ (FR-031)
}
    // bổ sung theo plan 017: 017 phát m11.api.TrangThaiDatCocDaDoi và DieuKienQuyetToanDaDoi khi kết quả đổi;
    // 004 dùng để làm mới màn điều kiện, giá trị vẫn đọc qua port
interface KyCuoiPort {                // 010 cài; mọi hàm MANDATORY
  long taoKyCuoiNhap(long nctId, LocalDate ngayKetThucDuKien);   // FR-066 (1): tác động kèm lệnh lập hồ sơ kết thúc
  void tinhLai(long nctId, LocalDate ngayMoi);                   // đổi ngày, quá ngày dự kiến
  void huyMocDung(long nctId);                                   // hồ sơ kết thúc bị hủy
}
interface TrangThaiCoMatBanTruPort {  // 005 cài
  Optional<TrangThaiCoMat> trangThai(long nctId, LocalDate ngay, long buoiId);  // CO_MAT | VANG_CO_BAO | VANG_KHONG_BAO (FR-060)
}
interface MucViecSauQuaDoiPort {      // 010, 013, 006, 007, 017 mỗi bên một bean
  MaMucViec ma();                                                // CHI_PHI_KY_CUOI | DO_GUI | THUOC_GUI | CANH_BAO_SU_CO | QUYET_TOAN
  TrangThaiMuc trangThai(long nctId);                            // HOAN_THANH | CHUA_HOAN_THANH | KHONG_AP_DUNG, kèm căn cứ (FR-071)
}
```

**Mã điều kiện kết thúc** mà bean `ThamGiaChuyenTrangThai` của 010, 013, 006, 007, 017 phải dùng cho lệnh `KET_THUC_LUU_TRU`: `KT_A_CHI_PHI` (010), `KT_B_DO_GUI` (013), `KT_C_THUOC_GUI` (006), `KT_D_CANH_BAO_SU_CO` (007), `KT_H_QUYET_TOAN` (017); `coTheMien = true` (research T-09). 004 dùng `KT_E_BAN_GIAO` với `coTheMien = false`.

Sự kiện 004 nghe: `NguoiCaoTuoiDaChuyenTrangThai`, `DanhGiaDauVaoDaXacNhan`, `MucChamSocDaDoi` (001); `GiuongDaTrong` (003); `YeuCauDaQuyetDinh`, `YeuCauApDungKhongThanh`, `YeuCauDaHuy`, `ThamSoDaDoi(CFG-M02-05, CFG-M02-08)` (000).

### 2.2. Giao diện có sẵn hoặc đã bổ sung

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi`, `MucChamSocApi` (`hienHanh`, `apDungThayDoi`), `DanhGiaApi.xacNhanGanNhat`, `TrangThaiNguoiCaoTuoiApi` (`thucHien` với `HUY_TIEP_NHAN`, `xemDieuKien`, `khoangTrangThai`) | M01 (001) | FR-002, FR-004, FR-008, FR-042, FR-063, FR-074 | Có sẵn sau 001; 004 được thêm vào bảng người gọi của `thucHien` (bổ sung theo plan này) |
| `PhanBoGiuongLenhApi`, `SucChuaBanTruApi`, `ViTriApi`, `PhongChoPhepApi.thuocTinhPhanBo`, `TinhTrangGiuongApi.batDauLuotTrong` | M03 (003) | FR-012 → FR-016, FR-021, FR-041, FR-054 → FR-059, FR-065, FR-070 | Hai hàm cuối bổ sung theo plan này |
| `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi`, `NguoiDungHienTai`; `TaiKhoanApi.coVaiTro` (Quyết định giữ giường chỉ Quản lý viện, chú thích ⁸, Q-345; bổ sung 2026-10-11) | M15 (002) | Research T-14 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `YeuCauPheDuyetApi` (gồm `lap`, `chuyenMoRong`, `dongDoTrangThaiCuoi`), `LoaiYeuCauHandler` (gồm `kiemTraKhiDuyet`, `khiDuyet`), `TepApi` | M15 (000), `common` | Mọi lệnh | `dongDoTrangThaiCuoi` bổ sung theo plan này |
