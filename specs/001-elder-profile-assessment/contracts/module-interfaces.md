# Giao diện giữa module: Feature 001

**Ngày**: 2026-10-07 | **Plan**: [../plan.md](../plan.md)

Hợp đồng trong backend, ở gói `com.cambae.backend.m01.api`. Module khác chỉ dùng các giao diện này, không đọc bảng của M01 (constitution X). Chữ ký viết dạng Java rút gọn để mô tả hợp đồng; mã thật viết ở giai đoạn triển khai.

Mục 1.1 giữ đúng chữ ký `NguoiCaoTuoiApi` mà plan 002 đã khai báo ([../../002-account-permission/contracts/module-interfaces.md](../../002-account-permission/contracts/module-interfaces.md) mục 2) và bổ sung phần còn thiếu.

---

## 1. 001 cung cấp

### 1.1. `NguoiCaoTuoiApi`

```java
TrangThaiNct trangThai(long nctId);                     // 002 (FR-044a, FR-046, FR-049), mọi module
NhanDangView nhanDang(long nctId);                      // họ tên, ảnh, mã hồ sơ; 007 cho người ngoài phạm vi (002 FR-044a)
Optional<Instant> thoiDiemTrangThaiCuoi(long nctId);    // 002 job khóa tài khoản người thân (CFG-M01-04), lưu giữ
boolean laTrangThaiCuoi(long nctId);
Optional<GioiTinh> gioiTinh(long nctId);                // 003 (BR-M03-01)
List<Long> theoTrangThai(Set<TrangThaiNct> tt);         // job của module khác
```

### 1.2. `TrangThaiNguoiCaoTuoiApi`: lệnh từ module khác (research H-03)

```java
KetQuaChuyen thucHien(long nctId, LenhTrangThai lenh, NguoiDungHienTai nd,
                      Optional<LyDo> lyDo, JsonNode chiTiet, CanCu canCu);   // Propagation.MANDATORY
List<DieuKien> xemDieuKien(long nctId, LenhTrangThai lenh);                 // 004 FR-004: danh sách điều kiện tiếp nhận
List<KhoangTrangThai> khoangTrangThai(long nctId, Set<TrangThaiNct> tt, Instant tu, Instant den);
    // bổ sung theo plan 003 (research G-13): khoảng Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài từ lịch sử trạng thái
    // (003 FR-027 c, FR-036); đọc, không khóa
```

| Người gọi | Lệnh được phép |
| --- | --- |
| 014 | `DIEM_DANH_ROI_VIEN`, `DIEM_DANH_VE`, `HUY_GHI_NHAN_ROI_VIEN` (`doHeThong = true`) |
| 007 | `CHUYEN_VIEN` khi ghi chuyển viện trên sự cố Khẩn cấp (BR-M05-14) |
| 004 | `HUY_TIEP_NHAN` khi hồ sơ chờ chuyển Từ chối hoặc Hủy chờ (004 FR-002); `chiTiet` mang `hoSoChoId`, `lenhHoSoCho`; người thực hiện là Hành chính ra lệnh (bổ sung theo plan 004, research T-03) |

Lời gọi của module khác với lệnh ngoài bảng trên bị từ chối (ArchUnit cùng kiểm tra lúc chạy).

### 1.3. SPI `ThamGiaChuyenTrangThai` (module khác cài; research H-02)

```java
Set<LenhTrangThai> lenhQuanTam();
List<DieuKien> kiemTraDieuKien(NguCanhLenh nc);        // chỉ đọc; rỗng = thỏa
default Optional<MienDieuKien> mienDieuKien(NguCanhLenh nc) { return Optional.empty(); } // chỉ 004
default void kiemTraChiTiet(LenhTrangThai lenh, JsonNode chiTiet) {}                  // lỗi → 422
void thucHienTacDong(KetQuaChuyen kq);                 // Propagation.MANDATORY; ném lỗi = rollback cả lệnh
default List<CanhBao> canhBao(NguCanhLenh nc) { return List.of(); }
    // bổ sung theo plan 003 (research G-06): cảnh báo không chặn, trả trong GET …/dieu-kien/{lenh} và kèm phản hồi lệnh
    // (ví dụ 003 FR-024a: Ghi nhận trở về khi giường giữ chỗ thuộc phòng đang chịu chặn cách ly)

record NguCanhLenh(long nctId, TrangThaiNct tu, LenhTrangThai lenh, NguoiDungHienTai nd,
                   Instant thoiDiem, JsonNode chiTiet) {}
record DieuKien(String ma, String moTa, String canCu, boolean coTheMien) {} // canCu: BR/FR, ví dụ "BR-M12-03"
record MienDieuKien(long yeuCauPheDuyetId, Set<String> maDieuKien) {}
record KetQuaChuyen(long nctId, TrangThaiNct tu, TrangThaiNct den, LenhTrangThai lenh,
                    long lichSuId, Instant thoiDiem, JsonNode chiTiet) {}
```

Module cài theo research H-02 mục 4: 003, 004, 005, 006, 007, 010, 011, 012, 013. Nếu nguồn chốt thêm điều kiện "số dư và tiền cọc đã quyết toán" vào spec 001 (plan, điểm báo lại 2), 017 cài thêm một bean; M01 không phải sửa. Plan của từng module ghi rõ điều kiện, tác động và kiểu của `chiTiet` mà nó sở hữu. Thứ tự gọi `thucHienTacDong` theo `@Order`: 004 trước (mở hoặc đóng lượt vắng, hợp đồng), rồi 003, rồi 005, 006, 011, 010, 012, 013.

### 1.4. `MucChamSocApi`

```java
Optional<MucChamSoc> hienHanh(long nctId);
Optional<MucChamSocTaiNgay> taiNgay(long nctId, LocalDate ngay);   // kèm căn cứ (FR-053)
Set<NhuCauChamSoc> nhuCauHienHanh(long nctId);                      // từ lần Đã xác nhận gần nhất
void apDungThayDoi(long nctId, MucChamSoc muc, LocalDate ngayHieuLuc, long yeuCauPheDuyetId); // MANDATORY; chỉ handler áp dụng của 004
```

Dùng bởi 003 (phòng – mức), 004 (hợp đồng, BR-M02-22), 008 (tỷ lệ phục vụ), 010 (dịch vụ chăm sóc theo mức), 016.

### 1.5. `DanhGiaApi`, `DanhGiaLaiApi`

```java
Map<Long, Optional<String>> luuYHienHanh(Collection<Long> nctIds);  // 005, 007: thẻ người cao tuổi, thẻ khẩn cấp (FR-038)
Optional<DanhGiaTomTat> xacNhanGanNhat(long nctId);                 // 004 FR-004 (a)
Map<Long, TrangThaiDanhGiaLai> trangThai(Collection<Long> nctIds);  // CAN | QUA_HAN (kèm thời điểm) | KHONG; 016 FR-044
```

### 1.6. `MucSucKhoeApi`, `HoSoSucKhoeApi`, `DiNguyenApi`

```java
List<MucSucKhoeView> hieuLuc(long nctId, LoaiMuc... loai);   // kèm khongKiemTraTuDong, choKiemTra; 006, 007, 011
void daKiemTra(long mucId, NguonKiemTra nguon);              // 006 (THUOC), 011 (AN) gỡ cờ (BR-M01-12)
Optional<HoSoSucKhoeBanDauView> banDau(long nctId);          // 006: thuốc đang dùng khi tiếp nhận
Optional<DiNguyenView> diNguyen(long id);  List<DiNguyenView> diNguyenHieuLuc(NhomDiNguyen nhom); // 006, 011
long themDiNguyen(TaoDiNguyen lenh);                         // 011 FR-003 (Bác sĩ thêm thành phần thực phẩm)
```

### 1.7. `BanDongYApi`

```java
Optional<BanDongYView> hienHanh(long nctId);   // phạm vi người thân, dongYHinhAnh, dongYThongBao; 009, 012
                                               // dongYThongBao: thông báo không khẩn về người cao tuổi (Q-353)
boolean baoGom(long nctId, long nguoiThanId);  // dùng trong DieuKienChuThich "1"
Optional<Long> daiDienPhapLuat(long nctId);    // bổ sung 2026-10-18 (012 FR-020c): người thân ký bản đồng ý Hiệu lực với DAI_DIEN_PHAP_LUAT
```

M01 cài `DieuKienChuThich` với `maChuThich() = "1"` cho 002 (BR-M01-08, DBR-03).

### 1.8. `NguyenVongApi`

```java
Optional<NguyenVongView> hieuLuc(long nctId);                  // 007: thẻ khẩn cấp, cảnh báo nguy kịch (FR-023)
long ghiPhienBan(GhiNguyenVong lenh, NguonNguyenVong nguon);   // 007: XAC_NHAN_LAI_NGUY_KICH (BR-M05-16); MANDATORY
```

### 1.8b. `NguonYKienApi` và kiểu `NguonYKien` *(bổ sung 2026-10-18; FR-023g, research H-16)*

```java
enum LoaiNguonYKien { NGUOI_CAO_TUOI, NGUOI_THAN, NHAN_VIEN_QUAN_SAT }
record NguonYKien(LoaiNguonYKien loai, Long nguoiThanId) {}   // nguoiThanId chỉ có khi NGUOI_THAN
void kiemTra(long nctId, NguonYKien nguon);   // NGUOI_THAN: quan hệ Hiệu lực lúc gọi; sai → 422 NGUON_Y_KIEN_KHONG_HOP_LE
```

Dùng bởi 004 (ý kiến về việc vào ở), 005 (thỏa thuận lựa chọn có rủi ro), 012 (mong muốn về khách thăm, người đón), 014 (sở thích). Mỗi module lưu nguồn ở bảng của mình theo data-model 001 mục 6a.

### 1.8a. `NguoiCaoTuoiThongKeApi` (016; bổ sung theo plan 016, research O-10)

```java
Stream<TrangThaiTaiNgayView> trangThaiTaiNgay(PhamViLoc phamVi, LocalDate ngay);   // trạng thái 5.5, mức chăm sóc hiệu lực tại ngày (016 FR-022)
Stream<BienDongView> bienDong(PhamViLoc phamVi, Instant tu, Instant den);         // lệnh trạng thái, đổi mức theo thời điểm hiệu lực (016 FR-023)
Stream<DanhGiaQuaHanView> danhGiaLaiQuaHan(PhamViLoc phamVi);                      // trạng thái "quá hạn đánh giá lại" do 001 xác định (016 FR-044)
```

Chỉ đọc, không khóa; không trả bản ghi Hủy ghi nhận (016 FR-007).

### 1.9. SPI M01 cài cho 000, 002 và `common`

| SPI | Loại | Ghi chú |
| --- | --- | --- |
| `DinhChinhDuocProvider` | `M01.DANH_GIA`, `M01.HO_SO_SUC_KHOE_BAN_DAU`, `M01.LICH_SU_TRANG_THAI` | `tangKhuVuc` rỗng; trên hồ sơ trạng thái cuối `choPhep` chỉ cho cách (c) (FR-049a); `sauKhiDinhChinh` của `M01.DANH_GIA` theo research H-10 |
| `PhanLoaiTruongProvider` | Các loại ở data-model mục 10 | |
| `LuuTruProvider` | Các loại ở data-model mục 11 | |
| `DieuKienChuThich` | `"1"` | BR-M01-08 |
| `GanPhamViProvider` | `M01.NGUOI_CAO_TUOI` và các bản ghi con | Trả `DoiTuong` với `nguoiCaoTuoiId` để 002 tính phạm vi |
| `QuyenTepProvider` (`common.tep`) | `M01.ANH_NGUOI_CAO_TUOI`, `M01.BAN_DONG_Y`, `M01.NGUYEN_VONG`, `M01.QUA_DOI` | Quyền tải tệp bằng quyền xem bản ghi chủ sở hữu (H-13) |

### 1.10. Sự kiện 001 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `NguoiCaoTuoiDaTao(nctId, hoSoTruocId)` | 004 (lượt đăng ký), 016 | FR-001, FR-004 |
| `NguoiCaoTuoiDaChuyenTrangThai(nctId, tu, den, lenh, lichSuId, thoiDiem)` | 009 (Khẩn cấp cho người liên hệ chính khi `CHUYEN_VIEN`, `GHI_NHAN_QUA_DOI`), 002, 016 | FR-046, Q-97 |
| `BanDongYDaDoi(nctId, banHieuLucId)` | 009, 012 | FR-015 |
| `DiUngDaGhiNhan(mucId, nctId, diNguyenId)` | 006, 011 (kiểm tra; idempotent theo `mucId`) | FR-020, BR-M01-12 |
| `DiUngKhacDaGhiNhan(mucId, nctId)` | 011 (011 FR-036a) | FR-020 |
| `DiUngDaLoaiTru(mucId, nctId)` | 011, 007 | FR-020 |
| `HoSoSucKhoeBanDauDaDoi(nctId)` (bổ sung theo plan 006): "thuốc đang sử dụng khi tiếp nhận" đổi, khi sửa lúc Đang tiếp nhận hoặc đính chính `M01.HO_SO_SUC_KHOE_BAN_DAU` | 006 (006 FR-034a, BR-M07-22) | FR-017 |
| `DanhGiaDauVaoDaXacNhan(nctId, danhGiaId, mucChamSoc)` | 004 (004 FR-025b) | FR-033 |
| `DanhGiaDaDoiMucHoacNhuCau(nctId, danhGiaId, mucTruoc, mucSau, nhuCauTruoc, nhuCauSau)` | 005 (BR-M04-20) | FR-043 |
| `CanDanhGiaLai(nctId, lanBaoId, canCu)`; `QuaHanDanhGiaLai(nctId, hanLuc)` | 009 | FR-039 → FR-041 |
| `CanLapYeuCauDoiMuc(nctId, danhGiaId, mucMoi)`; `NhacLapYeuCauDoiMuc(…, lanNhac)` | 009 | FR-036 |
| `NhacThieuBanDongY(nctId, lanNhac)`; `NhacThieuNguyenVong(nctId, lanNhac)`; `NhacXemLaiNhanDinh(nctId, nguyenVongId)` (bổ sung 2026-10-18, FR-023i) | 009 | FR-012, FR-023d |
| `TamVangQuaHan(nctId, luotVangId, mocDuKien)` | 009 | FR-050 |
| `MucChamSocDaDoi(nctId, mucTruoc, mucSau, hieuLucTu, canCu)` | 003, 008, 010, 016 | FR-052 |
| `ThongTinCaNhanDaCapNhat(nctId, truongDaDoi)` (bổ sung theo plan 003) | 003 (giới tính đổi: dấu "phòng không còn phù hợp", 003 FR-034) | FR-006 |

Mỗi sự kiện gửi 009 mang mức, nhóm người nhận, loại thông tin và khóa sự kiện theo research H-14.

---

## 2. 001 cần từ module khác

| Giao diện | Module cung cấp (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| Bean `ThamGiaChuyenTrangThai` | 003, 004, 005, 006, 007, 010, 011, 012, 013 | Điều kiện, tác động ở bảng trạng thái | Bean giả trả Đạt / Chưa đạt theo cấu hình (CHK035) |
| `YeuCauDoiMucApi.thayTheBoiDanhGia(nctId, danhGiaId)` (MANDATORY); `coYeuCauDoiMucChuaKetThuc(nctId)` | M02 (004) | FR-036, FR-036a; job nhắc | Bản giả |
| `HopDongApi.loaiLuuTruHienHanh(nctId, ngay)`; `lichSuLoaiLuuTru(nctId)` | 004 | FR-009, FR-052, FR-053 | Bản giả |
| `LuotVangApi.dangMo()` trả mốc dự kiến trở lại | 004 (FR-048, FR-049) | FR-050 | Bản giả |

Ba giao diện trên được khai báo trong `m01.api` dưới dạng SPI, M02 cài, để không có vòng M01 ↔ M02 (bổ sung theo plan 004, research T-02); chữ ký giữ nguyên.
| `QuanHeNguoiThanApi.thuocNguoiCaoTuoi(nguoiThanId, nctId)` (SPI khai báo ở `m15.taikhoan.api`, M10 cài; theo plan 012). Từ 2026-10-18 (Q-348) 001 không dùng `laNguoiDaiDienHieuLuc` cho FR-011, FR-023c | M10 (012) | FR-011, FR-023c, FR-023g, phạm vi bản đồng ý | Bản giả |
| `ChiSoApi.ganNhat(nctId)` | M06 (007) | FR-024 | Bản giả trả rỗng |
| Sự kiện `SuCoDaGhiNhan(suCoId, nctId, loai, muc, canDanhGiaLai)` (thêm `canDanhGiaLai` theo plan 007: Trung bình trở lên hoặc Ngã, trừ đồ gửi; phát lại khi sự cố nâng lên Trung bình hoặc đổi loại sang Ngã). 001 chỉ báo đánh giá lại khi `canDanhGiaLai = true`, khóa bỏ trùng `(suCoId, mốc)` | M05 (007) | FR-040 | Không có thì không báo căn cứ sau sự cố |
| `NhiemVuApi`: bác sĩ trực, Điều dưỡng phụ trách, Trưởng tầng của tầng | M09 (008) qua 002 | Người nhận ở research H-14 | Bản giả |
| `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi`, `NguoiDungHienTai` | M15 (002) | Mọi lệnh, tra cứu | Có sẵn sau 002 |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `YeuCauPheDuyetApi`, `common` | M15 (000) | Mọi lệnh | Có sẵn sau 000 |
| `TepApi` (`common.tep`) | Bổ sung theo plan này (research H-13) | Ảnh, bản scan | Có sẵn cùng 001 |

**Điểm đồng bộ với plan 000**:
- Data-model của 000 ghi trạng thái mở rộng `DUOC_THAY_THE` là "của 001". Thực tế trạng thái này thuộc loại yêu cầu `M02.THAY_DOI_LUU_TRU` do 004 khai báo; 001 chỉ kích hoạt qua `YeuCauDoiMucApi`.
- Để 004 chuyển yêu cầu sang trạng thái mở rộng ngoài lệnh của người duyệt, `YeuCauPheDuyetApi` cần thêm `chuyenMoRong(yeuCauId, maChuyen, CanCu)`. Hàm chạy MANDATORY, ghi nhật ký với người thực hiện là hệ thống kèm căn cứ.
- Hai điểm trên và `TepApi` đã được ghi bổ sung vào [../../000-shared-foundation/contracts/module-interfaces.md](../../000-shared-foundation/contracts/module-interfaces.md).
