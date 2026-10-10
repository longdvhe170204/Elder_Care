# Giao diện giữa module: Feature 000

**Ngày**: 2026-10-06 | **Plan**: [../plan.md](../plan.md)

Đây là hợp đồng trong backend, ở gói `vn.eldercare.m15.<goi>.api` và `vn.eldercare.common`. Module khác chỉ được dùng các giao diện này, không đọc bảng của 000 (constitution X). Ngoại lệ duy nhất: báo cáo được `JOIN` hai view `v_dinh_chinh_moi_nhat` và `v_ban_ghi_da_huy` (data-model mục 4).

Chữ ký viết dạng Java rút gọn, chỉ để mô tả hợp đồng; mã thật viết ở giai đoạn triển khai.

---

## 1. 000 cung cấp

### 1.1. `common`: dùng ở mọi module

| Thành phần | Hợp đồng | FR |
| --- | --- | --- |
| `@NhomDuLieu(DANH_MUC \| NGHIEP_VU \| GHI_NHAN)` | Bắt buộc trên mọi entity nghiệp vụ; ArchUnit kiểm tra | FR-001 |
| `@LenhNghiepVu(batBuocLyDo = true)` | Đánh dấu phương thức lệnh nhóm 2; ArchUnit kiểm tham số `LyDo` | FR-005, FR-007 |
| `ChiThemRepository<T, ID>` | Chỉ có `them(T)`, `timTheoId`, truy vấn đọc | FR-009 |
| `LyDo` (value object) | Không rỗng, không chỉ có khoảng trắng, tối đa 2000 ký tự; sai thì `422 THIEU_LY_DO` | FR-044, SC-008 |
| `Clock` (bean), `ThoiGianNghiepVu` | `homNay()`, `bayGio()`, `batDauNgay(LocalDate)`, `trongKhoang(LocalDate, tu, den)` (gồm cả hai đầu) | FR-008, FR-046, BR-M15-07 |
| `LoiNghiepVu` | `DieuKienChuaThoa(List<DieuKien>)` → 409; `KhongCoQuyen` → 403; `DaDuocXuLy` → 409; `SaiPhienBan(giaTriHienHanh)` → 412 | FR-006, FR-021, FR-040 |
| `PhongBiNgoaiTuyen` | `idempotencyKey`, `thoiDiemThietBi`; `KetQuaDongBo` gồm `CHAP_NHAN`, `CHAP_NHAN_KEM_NHAN`, `CHO_XEM_LAI`, `TU_CHOI` kèm lý do | FR-046, DBR-25, BR-M15-20 |
| `common.thongbao` (bổ sung theo plan 009, research B-02) | `YeuCauThongBao`, `CoYeuCauThongBao`, `NhomNhan`, `CachThay`, `MucThongBao`, `LoaiThongTin`, `TRUONG_SUC_KHOE`: khuôn yêu cầu thông báo duy nhất; mọi sự kiện gửi 009 (kể cả các sự kiện của 000 ở mục 1.5) cài `CoYeuCauThongBao` | 009 FR-001, FR-043a |
| `TepApi` (`common.tep`; bổ sung theo plan 001, research H-13) | `taiLen(...)` trả `tepId`; `gan(tepId, loaiChuSoHuu, idChuSoHuu)` trong transaction của lệnh (MANDATORY), tệp đã gắn không gắn lại; SPI `QuyenTepProvider` của module sở hữu quyết quyền tải xuống | NFR-08 |

### 1.2. `m15.nhatky.api.NhatKyApi`

```java
void ghi(BanGhiNhatKy banGhi);   // Propagation.MANDATORY: phải nằm trong transaction của lệnh (FR-047)

BanGhiNhatKy.builder()
    .hanhDong(String)                 // lấy từ danh mục hành động của module
    .doiTuong(String loai, Object id)
    .nguoiCaoTuoi(Long)               // nếu gắn hồ sơ (FR-050)
    .truoc(Map<String,Object>).sau(Map<String,Object>)
    .lyDo(LyDo)                       // bắt buộc theo FR-044; build() báo lỗi nếu thiếu
    .canCu(CanCu)                     // bắt buộc khi người thực hiện là Bộ lập lịch (FR-045)
    .chiTiet(Map<String,Object>)      // FR-043a
    .thoiDiemThietBi(Instant)         // ngoại tuyến
    .build();
```

- `ghiNgoaiTransaction(BanGhiNhatKy)` dùng `REQUIRES_NEW`. Chỉ hai nơi được gọi: ghi lỗi áp dụng (FR-037b) và ghi bản tóm tắt loại bỏ dữ liệu (FR-051).
- Lần xuất báo cáo (016) và lần đăng nhập (002) gọi `ghi(...)` với `truoc`/`sau` rỗng (FR-043a). Riêng xuất báo cáo, 016 chỉ giao file sau khi transaction ghi nhật ký đã commit (FR-047).

### 1.3. `m15.nhatky.api.PhanLoaiTruongProvider` (SPI, module sở hữu cài)

```java
String loaiDoiTuong();
Map<String, NhomThongTin> phanLoaiTruong();   // ví dụ diUng → SUC_KHOE; trường không khai báo → CONG_KHAI_NOI_BO
```

`NhatKyQueryService` dùng SPI này cùng `QuyenXemTruongApi` (002) để che trường và che `lyDo` (FR-049, BR-M15-11). Thiếu provider cho một loại đối tượng thì chỉ Quản lý viện xem được lịch sử của loại đó. Đây là mặc định an toàn.

### 1.4. `m15.thamso.api.ThamSoApi`

```java
GiaTriThamSo doc(MaThamSo ma);                         // giá trị hiện hành; gọi một lần mỗi lần kiểm tra quy tắc (FR-018)
GiaTriThamSo docTaiThoiDiem(MaThamSo ma, Instant t);   // FR-019
// GiaTriThamSo: asDuration(), asInt(), asPercent(), asMoney(), asLocalTime(), asTable(Class<T>)
```

- Sự kiện `ThamSoDaDoi(ma, giaTriTruoc, giaTriSau, thoiDiem)` phát sau commit. Module chỉ dùng sự kiện để cập nhật bộ đệm của mình, không dùng để tính lại kết quả cũ (FR-017).
- `MaThamSo` là enum của toàn bộ mã ở Phụ lục 25. Module không được viết cứng giá trị mặc định trong mã (constitution XIII); test ArchUnit cấm hằng số thời lượng trong gói `service` của module.

### 1.5. `m15.pheduyet.api.YeuCauPheDuyetApi`

```java
YeuCauView lap(LapYeuCau lenh);                 // dùng khi lệnh của một actor ở module khác lập kèm yêu cầu; người lập là actor đó (FR-010a, Q-307)
Optional<YeuCauView> xem(long id);
Optional<KetQuaApDung> ketQuaCua(long yeuCauId); // FR-039
void chuyenMoRong(long yeuCauId, String maChuyen, CanCu canCu);
    // bổ sung theo plan 001: chuyển mở rộng do hệ thống thực hiện kèm lệnh của actor ở module khác
    // (ví dụ DUOC_THAY_THE của M02.THAY_DOI_LUU_TRU khi bác sĩ xác nhận đánh giá lại, 001 FR-036a);
    // Propagation.MANDATORY; chỉ chuyển đã khai báo trong chuyenMoRong() của handler; ghi nhật ký kèm căn cứ
void dongDoTrangThaiCuoi(long yeuCauId, CanCu canCu);
    // bổ sung theo plan 004 (research T-06): Propagation.MANDATORY, kèm lệnh làm người cao tuổi sang trạng thái cuối
    // (004 FR-065, FR-070); Nháp, Chờ duyệt → Hủy; Đã duyệt (chờ hiệu lực) → Áp dụng không thành; trạng thái cuối thì không làm gì;
    // người thực hiện là hệ thống, căn cứ là lệnh; phát YeuCauDaHuy / YeuCauApDungKhongThanh
```

**SPI `LoaiYeuCauHandler`**: mỗi loại yêu cầu do module sở hữu cài một handler.

```java
String maLoai();                                     // "M02.THAY_DOI_LUU_TRU"
Class<?> kieuNoiDung();                              // để kiểm schema JSON
void kiemTraNoiDung(Object noiDung);                 // lỗi → 422
KhoaQuyen quyenTao();  KhoaQuyen quyenDuyet();       // tra Phụ lục 27 qua QuyenApi (002)
PhamVi phamVi(YeuCauView yc);                        // để kiểm phạm vi duyệt (FR-034)
List<DieuKien> kiemTraDieuKienApDung(YeuCauView yc); // rỗng = thỏa (FR-038)
ThamChieu apDung(YeuCauView yc, LocalDate ngayHieuLucThucTe); // trong transaction; ném lỗi = rollback (FR-037b)
default boolean choTuDuyet() { return true; }        // 013 FR-027a: false
default Optional<MaThamSo> thamSoNhacRieng() { return Optional.empty(); } // FR-031a "MAY ngắn hơn"
default BangChuyenMoRong chuyenMoRong() { return BangChuyenMoRong.KHONG; } // 004 (DUOC_THAY_THE, kích hoạt từ 001), 015
default List<DieuKien> kiemTraKhiDuyet(YeuCauView yc) { return List.of(); }
    // bổ sung theo plan 003 (research G-07, G-13): gọi khi lệnh Duyệt, trước khi đổi trạng thái; không rỗng → 409, yêu cầu giữ Chờ duyệt
default void khiDuyet(YeuCauView yc, LocalDate ngayHieuLucThucTe) {}
    // bổ sung theo plan 003: Propagation.MANDATORY, trong transaction của lệnh Duyệt; tác động kèm quyết định
    // (ví dụ 004 giữ giường bằng phân bổ tương lai cho chuyển giường theo lịch, 003 FR-029); không gọi khi Từ chối
```

**`NhacQuaHan`**: dùng lại cho các vòng đời riêng (010, 012, 013, 017, 019). Module tự gọi nó từ job của mình, với mốc lấy từ CFG của module (FR-031a; đối chiếu Q-130).

```java
KetQuaNhac kiemTra(Instant moc, Instant daNhacLuc, Instant daBaoLuc, MaThamSo nhac, MaThamSo baoQlv, boolean conNguoiDuyet);
```

Sự kiện phát sau commit:
- `YeuCauDaLap`, `YeuCauDaGuiDuyet`;
- `YeuCauCanNhacNguoiDuyet(yc, danhSachNguoiDuyet)`;
- `YeuCauCanBaoQuanLyVien(yc, lyDo: QUA_CFG_M15_06 | KHONG_CON_NGUOI_DUYET)`;
- `YeuCauDaQuyetDinh(yc, ketQua)`;
- `YeuCauDaApDung(yc, ketQua)`;
- `YeuCauApDungKhongThanh(yc, dieuKien)`;
- `YeuCauDaHuy`.

### 1.6. `m15.dinhchinh.api.DinhChinhApi`

```java
KetQuaDinhChinh taoDinhChinh(String loaiBanGhi, long idGoc, KieuDinhChinh kieu, Object noiDungDung, LyDo lyDo);
    // trả DaLuu(dinhChinh) cho cách A/B; ChoDuyet(yeuCau) cho cách C (FR-026)
Map<Long, GiaTriHienHanh> hienHanh(String loaiBanGhi, Collection<Long> idGoc); // FR-030; gồm cờ daHuyGhiNhan
ChuoiDinhChinhView chuoi(String loaiBanGhi, long idGoc);                       // FR-028
```

**SPI `DinhChinhDuocProvider`**: mỗi loại bản ghi nhóm 3 có đính chính do module sở hữu cài một provider.

```java
String loaiBanGhi();                         // "M07.LIEU_THUOC"
boolean daXacNhan(long id);
long nguoiGhiGoc(long id);
Optional<Long> tangKhuVuc(long id);          // rỗng = không gắn tầng (FR-026 c); bắt buộc khai báo (CHK051)
boolean coQuyenGhiNhan(NguoiDung nd);        // FR-026 (a), (c)
void kiemTraNoiDung(Object noiDungDung);
default boolean choPhep(NguoiDung nd, CachTao cach, long id) { return true; } // module thu hẹp thêm
default void sauKhiDinhChinh(DinhChinhView dc) {}  // tác động dây chuyền do module (ví dụ 010 lập khoản điều chỉnh)
```

Sự kiện: `DaDinhChinh(loaiBanGhi, idGoc, kieu, dinhChinhId)`.

Không có provider nghĩa là loại đó không có đính chính: chi phí đã chốt, giao dịch số dư (1.5, DBR-17). Gọi đính chính cho các loại này thì trả 409 `KHONG_HO_TRO_DINH_CHINH`.

### 1.7. `m15.luutru.api.LuuTruProvider` (SPI)

```java
String loaiDuLieu();
LoaiHan loaiHan();                                   // THEO_HO_SO (CFG-M15-03) | KHONG_GAN_HO_SO (CFG-M15-04) | DAI_HON(MaThamSo)
Stream<UngVien> ungVienHetHan(LocalDate moc, int lo); // bản ghi đã qua hạn
Set<ThamChieuDuLieu> thamChieuToi(long id);          // ai đang trỏ tới bản ghi này (FR-051a)
void xoa(Collection<Long> ids);                      // chỉ được gọi bởi job, qua DataSource eldercare_luu_tru
```

Job `LoaiBoDuLieuHetHan` của 000 dùng các provider này để dựng chuỗi và xóa (research R-11).

---

## 2. 000 cần từ module khác

Đây là giao diện 000 gọi tới. 000 khai báo trước; plan của module cung cấp phải giữ đúng chữ ký hoặc ghi rõ thay đổi.

| Giao diện | Module cung cấp (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `NguoiDungHienTai` (id, vai trò AC-xx, `laBoLapLich`) | M15 tài khoản (002) | Mọi lệnh và nhật ký (FR-045) | Bản giả trong test |
| `QuyenApi.coQuyen(nd, KhoaQuyen, PhamVi)`: ba lớp BR-M15-01. Plan 002 mở rộng tham số thứ ba thành `DoiTuong`, có `DoiTuong.tuPhamVi(phamVi)` | 002 | FR-013, FR-034, FR-034a | Bản giả: bảng vai trò → quyền tối thiểu |
| `PhamViApi.trongPhamVi(nd, doiTuong)`; `nguoiPhuTrachCa(tang, thoiDiem)`; `truongTangDuocGiao(tang)` | 002, 008 | FR-026 (b), CHK033, FR-049 | Bản giả |
| `QuyenXemTruongApi.duocXem(nd, NhomThongTin)`. Plan 002 thêm tham số `DoiTuong` (FR-034 của 002); bản không đối tượng dùng cho nhật ký toàn viện | 002 (19.3) | FR-049, BR-M15-11 | Bản giả: Quản lý viện xem hết |
| `NguoiDuyetApi.coNguoiCoQuyen(KhoaQuyen, PhamVi)`; `danhSach(...)` | 002 | FR-031a, BR-M15-09 | Bản giả |
| Lắng nghe sự kiện mục 1.5 và gửi thông báo | M13 thông báo (009) | FR-031a, FR-038 | Sự kiện vẫn được lưu ở `event_publication`; không có người nghe thì không gửi |
