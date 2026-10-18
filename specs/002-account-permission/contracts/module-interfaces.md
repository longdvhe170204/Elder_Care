# Giao diện giữa module: Feature 002

**Ngày**: 2026-10-07 | **Plan**: [../plan.md](../plan.md)

Hợp đồng trong backend, ở gói `com.cambae.backend.m15.taikhoan.api`. Module khác chỉ dùng các giao diện này, không đọc bảng của 002 (constitution X). Chữ ký viết dạng Java rút gọn để mô tả hợp đồng; mã thật viết ở giai đoạn triển khai.

Mục 1 giữ đúng chữ ký mà feature 000 đã khai báo trước ([../../000-shared-foundation/contracts/module-interfaces.md](../../000-shared-foundation/contracts/module-interfaces.md) mục 2) và bổ sung phần còn thiếu. Chỗ khác với 000 được ghi rõ.

---

## 1. 002 cung cấp

### 1.1. Người dùng hiện tại

```java
interface NguoiDungHienTai {
  long taiKhoanId();
  LoaiTaiKhoan loai();                 // NHAN_VIEN | NGUOI_THAN
  Optional<Long> nhanVienId();  Optional<Long> nguoiThanId();
  Set<VaiTro> vaiTro();                // AC-xx đang gán
  boolean laBoLapLich();               // AC-11, FR-044
  String maThietBi();  KenhTruyCap kenh();
}
```

Bộ lập lịch có `NguoiDungHienTai` riêng, `laBoLapLich() = true`, được đặt khi job chạy (000 FR-045).

### 1.2. `QuyenApi`: ba lớp BR-M15-01

```java
KetQuaQuyen coQuyen(NguoiDungHienTai nd, KhoaQuyen khoa, DoiTuong doiTuong);          // FR-041
KetQuaQuyen coQuyenTaiThoiDiem(NguoiDungHienTai nd, KhoaQuyen khoa, DoiTuong doiTuong, Instant t); // FR-041a
Set<KhoaQuyen> tapQuyen(NguoiDungHienTai nd);                                          // FR-042, GET /toi/quyen
Set<String> lenhDuocPhep(NguoiDungHienTai nd, DoiTuong doiTuong, Map<String,KhoaQuyen> lenh); // FR-042

record KhoaQuyen(ChucNang chucNang, LoaiThaoTac thaoTac) {}
record KetQuaQuyen(boolean duocPhep, Optional<Lop> lopKhongThoa,     // VAI_TRO | PHAM_VI | PHAP_LY | DU_LIEU_NGUON
                   Optional<GiayPhepView> giayPhep, CanCu canCu) {}   // canCu: VAI_TRO | NHIEM_VU(loai) | NGOAI_PHAM_VI | BO_LAP_LICH
```

- 000 khai báo `coQuyen(nd, KhoaQuyen, PhamVi)`. 002 đổi tham số thứ ba thành `DoiTuong` (loại, id, người cao tuổi, tầng/khu nếu có) để kiểm được phạm vi, giới hạn trường và ngoại lệ sự cố trên cùng một đối tượng. `PhamVi` của 000 vẫn dùng được: `DoiTuong.tuPhamVi(phamVi)`. Plan 000 không cần sửa vì 000 chỉ dùng qua bản giả.
- Chú thích dùng ở service: `@CanQuyen(chucNang = …, thaoTac = …)`; interceptor gọi `coQuyen` trước lệnh, ném `KhongCoQuyen` (403) hoặc `KhongTimThay` (404) theo FR-043.

### 1.3. `PhamViApi`

```java
boolean trongPhamVi(NguoiDungHienTai nd, DoiTuong doiTuong);                    // khai báo ở 000
PhamViLoc boLoc(NguoiDungHienTai nd, KhoaQuyen khoa);                            // FR-035: TOAN_VIEN hoặc tập id
PhamViLoc boLocLichSu(NguoiDungHienTai nd, KhoaQuyen khoa, Instant tu, Instant den); // FR-035a; chỉ để đọc
Optional<Long> nguoiPhuTrachCa(long tangKhuVucId, Instant thoiDiem);             // khai báo ở 000; lấy từ 008
Optional<Long> truongTangDuocGiao(long tangKhuVucId);                            // khai báo ở 000; lấy từ 008
NguoiNhanXemLai nguoiQuyetDinhXemLai(Optional<Long> tangKhuVucId);               // FR-041c, CHK056

record PhamViLoc(boolean toanVien, Set<Long> tangKhuVucIds, Set<Long> phongIds, Set<Long> nguoiCaoTuoiIds) {}
```

ArchUnit cấm gọi `boLocLichSu` từ phương thức `@LenhNghiepVu` (FR-035a chỉ cho xem).

### 1.4. `QuyenXemTruongApi`

```java
MucXem duocXem(NguoiDungHienTai nd, NhomThongTin nhom, DoiTuong doiTuong);   // DAY_DU | CHI_CO_HAY_KHONG | KHONG
```

- 000 khai báo `duocXem(nd, NhomThongTin)`; 002 thêm `DoiTuong` vì giới hạn trường xét theo cặp (nhóm, phạm vi) của từng vai trò (FR-034). Bản rút gọn của 000 tương đương `duocXem(nd, nhom, DoiTuong.KHONG_CU_THE)` và chỉ dùng cho nhật ký toàn viện của Quản lý viện.
- Chú thích DTO: `@NhomTruong(NhomThongTin.SUC_KHOE)` trên trường; `BeanSerializerModifier` của 002 áp khi tuần tự hóa (research A-08).

### 1.5. `NguoiDuyetApi` (khai báo ở 000)

```java
boolean coNguoiCoQuyen(KhoaQuyen khoa, DoiTuong doiTuong);   // BR-M15-09
List<Long> danhSach(KhoaQuyen khoa, DoiTuong doiTuong);      // để gửi nhắc (000 FR-031a)
                                                             // bổ sung theo plan 007 (Q-339): 007 kiểm người được giao xử lý sự cố có (SU_CO, XU_LY) với đối tượng (FR-045a);
                                                             // danhSach luôn xét phạm vi, không áp ngoại lệ ghi nhận sự cố FR-044a; trả id tài khoản
```

Duyệt danh sách tài khoản Hoạt động có quyền vai trò, nhiệm vụ và phạm vi bao đối tượng tại thời điểm hỏi.

### 1.6. `TaiKhoanApi`: tác động kèm lệnh của module khác

```java
void khoaDoNghiViec(long nhanVienId, CanCu canCu);         // gọi trong transaction lệnh Cho nghỉ việc của 008 (FR-009, BR-M09-04)
void xetLaiNguoiThan(long nguoiThanId, CanCu canCu);       // gọi trong transaction lệnh chấm dứt quan hệ của 012 (FR-049, BR-M15-14)
Optional<TaiKhoanView> theoNhanVien(long nhanVienId);      // 008: vai trò hệ thống để xếp ca (008 FR-002)
Optional<TaiKhoanView> theoNguoiThan(long nguoiThanId);    // 009: trạng thái tài khoản không chặn gửi ngoài ứng dụng (CHK036)
Optional<TaiKhoanView> theoNguoiCaoTuoi(long nguoiCaoTuoiId); // bổ sung 2026-10-18: 009 nhóm người nhận (f) (009 FR-007), 012 kênh người cao tuổi
boolean coVaiTro(long taiKhoanId, VaiTro... vaiTro);       // 008: chặn giao Phụ trách bộ phận cho AC-01, AC-14 (FR-019f);
                                                          // bổ sung theo plan 004 (Q-338): 000 lọc người duyệt theo gioiHanVaiTroDuyet;
                                                          // bổ sung theo plan 007 (Q-339): 007 kiểm vai trò người giao, người được giao sự cố (FR-045a)
```

Hai lệnh đầu dùng `Propagation.MANDATORY`. Chúng không tạo bản ghi nghiệp vụ mới ngoài chuyển trạng thái tài khoản và nhật ký, đúng FR-010a của 000.

### 1.7. Ngoại tuyến

```java
interface NgoaiTuyenHandler<T> {          // SPI, module ghi nhận cài (005, 006, 007, 011, 014…)
  String loaiBanGhi();                    // "M04.KET_QUA_CONG_VIEC"
  KhoaQuyen khoaQuyen();
  DoiTuong doiTuong(T noiDung);
  void kiemTraNoiDung(T noiDung);
  ThamChieu apDung(T noiDung, KetQuaDongBo ketQua, Instant thoiDiemHieuLuc); // ghi bản ghi thật
  default boolean batBuocTrucTuyen() { return false; }  // true: kích hoạt khẩn cấp, liều có kiểm soát đặc biệt (Q-01)
}

KetQuaDongBo xuLy(PhongBiNgoaiTuyen phongBi, Object noiDung);   // 002 điều phối, research A-11
```

`KetQuaDongBo` và `PhongBiNgoaiTuyen` là kiểu của `common` (000). Với `CHAP_NHAN_KEM_NHAN`, module lưu cột `nhan_dong_bo_sau_mat_quyen = true` trên bản ghi của mình.

### 1.8. Điều kiện chú thích

```java
interface DieuKienChuThich {               // SPI, module sở hữu điều kiện cài
  String maChuThich();                     // "1", "2", "10"…
  boolean thoa(NguoiDungHienTai nd, DoiTuong doiTuong, Instant t);
}
```

Chú thích chưa có bản cài thì `QuyenApi` từ chối với `lopKhongThoa = VAI_TRO` (mặc định an toàn) và ghi cảnh báo khởi động.

### 1.9. `GanPhamViProvider` (SPI)

```java
String loaiDoiTuong();                      // "M09.LICH_CA", "M10.PHAN_HOI"
DoiTuong ganPhamVi(long id);                // tầng/khu, người cao tuổi, hoặc nhân viên liên quan (FR-034, CHK049)
```

### 1.10. Sự kiện 002 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nhận (009 xử lý) | FR |
| --- | --- | --- |
| `TaiKhoanDaTao(taiKhoanId, loai)` | Không gửi; dùng cho báo cáo | FR-015 |
| `TaiKhoanDoiTrangThai(taiKhoanId, tu, den, nguyenNhan)` | Không gửi thông báo; app nhận qua phiên kết thúc | FR-015 |
| `BanGhiDongBoSauMatQuyen(loaiBanGhi, id, tangKhuVucId)` | Trưởng tầng của tầng; Người phụ trách ca nếu chưa có Trưởng tầng; Quản lý viện nếu không gắn tầng | FR-041a, CHK056 |
| `BanGhiChoXemLai(id, tangKhuVucId)` | Như trên | FR-041c |
| `LoiDuLieuQuyen(nguon, banGhiId, loaiLoi)` | Quản lý viện | FR-041b |
| `GiayPhepSapHetHan(loai, giayPhepId, ngayHetHan, nghiepVuBiTat, nguoiLienQuan)` | Quản lý viện | FR-039 |

Mức thông báo do 009 xếp theo nguyên tắc chung (009 FR-043b); 002 không tự gửi.

### 1.11. `ThietBiGanApi`: nút SOS *(bổ sung 2026-10-18, Q-369, Q-370)*

```java
Optional<NguoiCaoTuoiCuaThietBi> xacDinh(String tokenThietBi); // 007 FR-047d; không cần phiên; rỗng khi token sai, thiết bị đã gỡ, tài khoản Đã khóa
record NguoiCaoTuoiCuaThietBi(long nguoiCaoTuoiId, long taiKhoanId, String maThietBi) {}
```

Chỉ endpoint nút SOS của 007 gọi hàm này (ArchUnit kiểm). Token thiết bị không mở phiên, nên mọi endpoint khác vẫn qua bộ lọc phiên (research A-15).

---

## 2. 002 cần từ module khác

| Giao diện | Module cung cấp (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `HoSoNhanVienApi.trangThai(nhanVienId, ngay)`; `chucDanhCoVaiTro(nhanVienId)` | M09 (008) | FR-004, FR-004a, FR-011 | Bản giả |
| `GiayPhepApi.hieuLuc(nhanVienId, loai, ngay)` trả giấy phép và mục phạm vi hành nghề | 008 (FR-006) | FR-037, FR-038, A-09 | Bản giả |
| `DanhMucPhamViHanhNgheApi` | 008 | FR-037, BR-M15-19 | Bản giả |
| `PhanCongApi.dangHieuLuc(nhanVienId, thoiDiem)`; `lichSu(nhanVienId, tu, den)` | 008 (FR-037a) | FR-028 → FR-035a | Bản giả |
| `NhiemVuApi`: người phụ trách ca, trưởng tầng được giao (kể cả tạm), ca không có trưởng tầng, Phụ trách bộ phận, trưởng đoàn, người đi cùng, người nhận tại tầng. Plan 008 thêm `nguoiThayTruongTang(tang, t)` (Q-90), `boPhanCua(nv, t)`, `laPhuTrachBoPhan(nv)` | 008 (014 cho chuyến đi qua 008) | FR-019a → f, FR-031 | Bản giả |

`HoSoNhanVienApi`, `GiayPhepApi`, `DanhMucPhamViHanhNgheApi`, `PhanCongApi`, `NhiemVuApi` được khai báo trong `m15.taikhoan.api` dưới dạng SPI, M09 cài, để không có vòng M15 ↔ M09 (bổ sung theo plan 008, research N-02); chữ ký giữ nguyên. 002 cung cấp thêm cho 008 hàm đọc `NghiepVuChuyenMonApi.loaiYeuCau(maNghiepVu)` (loại giấy phép/đào tạo của nghiệp vụ chuyên môn; 008 FR-030).
| `PhanBoGiuongApi.dangMo(nguoiCaoTuoiId, thoiDiem)`; `nguoiCaoTuoiTai(phamVi, thoiDiem)`. Khai báo trong `m15.taikhoan.api` dưới dạng SPI, M03 cài, để không có vòng M15 ↔ M03 (bổ sung theo plan 003, research G-02) | M03 (003) | FR-029, FR-030 | Bản giả |
| `NguoiCaoTuoiApi.trangThai(id)`; `nhanDang(id)`; điều kiện chú thích ¹ (bản đồng ý) | M01 (001) | FR-044a, FR-046, FR-049 | Bản giả |
| `QuanHeNguoiThanApi.hieuLuc(nguoiThanId)`; `soDienThoaiDaDangKy(nguoiThanId)`; điều kiện chú thích ² (người đại diện) và quyền theo người thân. Khai báo trong `m15.taikhoan.api` dưới dạng SPI, M10 cài, để không có vòng M15 ↔ M10; 001, 006, 011 dùng cùng khai báo (bổ sung theo plan 012, research F-14; chữ ký hợp nhất ở hợp đồng 012 mục 1.1) | M10 (012) | FR-045 → FR-052 | Bản giả |
| `KetThucLuuTruApi.thoiDiemTrangThaiCuoi(nguoiCaoTuoiId)`. Khai báo trong `m15.taikhoan.api` dưới dạng SPI, M02 cài, để không có vòng M15 ↔ M02 (bổ sung theo plan 004, research T-02) | M02 (004) | FR-049, job `KhoaSauKetThucLuuTru` | Bản giả |
| `GiayPhepCoSoApi.coPhamVi(phamVi, ngay)`; `sapHetHan(trongVong)` | M06 (007, FR-070) | FR-037 (c), FR-039 | Bản giả |
| `ThamSoApi`, `NhatKyApi`, `NhacQuaHan`, `common` | M15 (000) | Mọi lệnh | Có sẵn |
| Lắng nghe sự kiện mục 1.10 | M13 (009) | FR-039, FR-041a → c | Sự kiện lưu ở outbox |
| *(Bổ sung 2026-10-18)* `NguoiCaoTuoiApi.trangThai(id)` cho lệnh tạo, mở khóa tài khoản người cao tuổi; nghe `NguoiCaoTuoiDaChuyenTrangThai` sang trạng thái cuối để khóa | M01 (001) | FR-067, FR-072 (A-16) | Bản giả; sự kiện giả trong test |
| *(Bổ sung 2026-10-18)* `LoaiYeuCauHandler` cho loại `M15.KHOA_TAI_KHOAN_NCT` (002 cài, 000 gọi) | M15 (000) | FR-072 (A-16) | Có sẵn |
