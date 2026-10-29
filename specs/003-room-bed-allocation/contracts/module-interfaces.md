# Giao diện giữa module: Feature 003

**Ngày**: 2026-10-07 | **Plan**: [../plan.md](../plan.md)

Hợp đồng trong backend, ở gói `com.eldercare.backend.roombedallocation.api`. Module khác chỉ dùng các giao diện này, không đọc bảng của 003 (constitution X). Ngoại lệ: báo cáo (016) và 007 được `JOIN` view `v_phan_bo_hien_hanh` (data-model mục 4), như 000 cho view đính chính. Chữ ký viết dạng Java rút gọn để mô tả hợp đồng; mã thật viết ở giai đoạn triển khai.

Chiều phụ thuộc theo research G-02: 003 phụ thuộc 000, 001, 002; 004, 005, 007, 008, 011, 016 phụ thuộc 003. Chỗ 003 cần dữ liệu của module phụ thuộc nó thì 003 khai báo SPI (mục 2), module đó cài.

---

## 1. 003 cung cấp

### 1.1. `PhanBoGiuongApi` (SPI khai báo ở `administration.taikhoan.api` cho 002; M03 cài)

Giữ đúng chữ ký plan 002 đã khai báo ([../../002-account-permission/contracts/module-interfaces.md](../../002-account-permission/contracts/module-interfaces.md) mục 2). Interface nằm ở gói của 002 để tránh vòng M15 ↔ M03.

```java
Optional<PhanBoView> dangMo(long nctId, Instant t);          // phân bổ DANG_HIEU_LUC tại t: giường, phòng, tầng, khu vực
Set<Long> nguoiCaoTuoiTai(PhamViLoc phamVi, Instant t);      // người có phân bổ đang hiệu lực trong tập tầng/phòng tại t
```

### 1.2. `ViTriApi`: vị trí hiện tại và tại thời điểm (đọc)

```java
Optional<ViTriView> hienTai(long nctId);                      // khu – tầng – phòng – giường, nhãn đầy đủ (FR-003); 001 hiển thị trên hồ sơ
Map<Long, ViTriView> hienTai(Collection<Long> nctIds);        // theo lô: 005 checklist, 011 chia suất theo tầng
Optional<ViTriView> taiThoiDiem(long nctId, Instant t);       // FR-035 (a); 007, 010
Optional<Long> tangTaiNgay(long nctId, LocalDate ngay);       // 011, 016
boolean chuaCoGiuong(long nctId);                             // dấu FR-025a; 004, 016
Optional<PhanBoView> phanBoHienHanhHoacTuongLai(long nctId);  // bổ sung theo plan 004: 004 FR-021 (giường từ ngày bắt đầu hợp đồng), FR-004 (d)
```

### 1.3. `LichSuViTriApi`: tra cứu lịch sử (FR-035, FR-036)

```java
List<DongViTri> theoNguoi(long nctId, Instant tu, Instant den);
List<DongGiuong> theoViTri(ViTriLoc loc, Instant t);                         // (b) tại thời điểm
List<DongGiuong> theoViTriTrongNgay(ViTriLoc loc, LocalDate ngay);           // (b) trong ngày, gồm người ở một phần ngày
List<NguoiCungPhong> cungPhong(long nctId, Instant tu, Instant den);         // (c) 007 BR-M05-10; khoảng chồng, khoảng vắng, dấu
```

Kết quả chỉ có họ tên, giường, phòng, khoảng thời gian, `daDinhChinh`, `coNhapBuChoDuyet`, `khoangVang[]`. 007 gọi với quyền của người dùng 007 (FR-037).

### 1.4. `PhanBoGiuongLenhApi`: lệnh từ 004 (mọi hàm `Propagation.MANDATORY`)

```java
void giuChoHoSoCho(long hoSoChoId, long nctId, long giuongId, Instant thoiDiem);   // 004 FR-014; hạn CFG-M02-02
void huyGiuCho(long hoSoChoId, LyDoHuyGiuCho lyDo);                                // từ chối, hủy chờ; idempotent
void giaiPhongGiuongKhiVang(long nctId, Instant thoiDiem, CanCu canCu);            // BR-M02-07 (Quản lý viện chọn giải phóng)
void giaHanKetThucDuKien(long nctId, Instant ketThucDuKienMoi);                    // nội trú ngắn ngày gia hạn; chồng phân bổ tương lai → DieuKienChuaThoa
List<DieuKien> kiemTraDatTruocChuyenTheoLich(long nctId, long giuongMoiId, LocalDate ngayHieuLuc);
long datTruocChuyenTheoLich(long yeuCauId, long nctId, long giuongMoiId, LocalDate ngayHieuLuc); // trong khiDuyet của handler 004
List<DieuKien> kiemTraChuyenTheoLich(long yeuCauId);                               // trong kiemTraDieuKienApDung; giường chưa Trống không phải vi phạm
KetQuaChuyenTheoLich thucHienChuyenTheoLich(long yeuCauId, Instant thoiDiemDuKien); // DA_CHUYEN | CHO_GIUONG_SAN_SANG
void huyPhanBoTheoLich(long yeuCauId, LyDo lyDo);                                  // khi yêu cầu Áp dụng không thành; idempotent
void daLapYeuCauSauChuyenGap(long phanBoId, long yeuCauId);                        // FR-030: gỡ dấu chờ lập yêu cầu
void ketQuaYeuCauSauChuyenGap(long phanBoId, boolean duocDuyet);                   // FR-030: từ chối → dấu chờ chuyển về phòng cùng giá
```

### 1.5. `SucChuaBanTruApi`: kiểm tra sức chứa cho 004, 005 (FR-038 → FR-042)

```java
CauHinhBanTru khoa();                                          // MANDATORY; FOR UPDATE dòng khu nghỉ; trả buổi, sức chứa CFG-M03-01, tầng/khu vực
List<BuoiNgay> buoiBiChiem(LocalDate ngay, LocalTime gioDen, LocalTime gioVe);  // giao nửa mở [gioDen, gioVe) (FR-039)
List<ViPhamSucChua> kiemTraSucChua(Map<BuoiNgay, SoDangKy> truocSau);           // vi phạm khi sau > sức chứa và sau > trước; khoanh vùng (FR-042)

record SoDangKy(int truoc, int sau) {}
record ViPhamSucChua(BuoiNgay buoi, int sucChua, int conLai, String ma) {}       // ma: VUOT_SUC_CHUA | KHU_NGHI_KHOANH_VUNG
```

004 ném `DieuKienChuaThoa` với toàn bộ danh sách, mã BR-M03-03.

### 1.6. `CoCauViTriApi`: cơ cấu (đọc)

```java
Optional<ViTriView> giuong(long giuongId);  Optional<PhongView> phong(long phongId);
Optional<TangView> tang(long tangId);       Optional<KhuVucView> khuVuc(long khuVucId);
boolean hieuLuc(ViTriRef viTri);                         // 008 kiểm vị trí của phân công
Set<Long> tangCuaKhuVuc(long khuVucId);
Optional<ViTriRef> viTriKhuNghiBanTru();                 // 011 FR-048: người nhận phiếu bữa ăn bán trú
Optional<LoaiPhongRef> loaiPhongCuaGiuong(long giuongId); // 004, 010: đơn giá theo loại phòng
```

### 1.7. `PhongChoPhepApi` (FR-034 (1))

```java
boolean phongHienTaiChoPhepMuc(long nctId, MaMucChamSoc muc);   // 004: cảnh báo trên yêu cầu đổi mức
ThuocTinhPhanBo thuocTinhPhanBo(long giuongId);
    // bổ sung theo plan 004 (004 FR-012): chính sách giới tính, mức cho phép, loại hình phục vụ của loại phòng,
    // chiuChanCachLy tại bây giờ, có phân bổ tương lai, mang dấu "chờ chuyển người"
```

### 1.8. `TinhTrangGiuongApi`: số liệu cho 016 (SC-008)

```java
Map<TrangThaiGiuong, Integer> demTheoTrangThai(PhamViLoc phamVi, Instant t);
int demTrongTheoDieuKien(PhamViLoc phamVi, MaMucChamSoc muc, GioiTinh gioiTinh, LoaiLuuTru loai, Instant t);
List<GiuongDauView> giuongMangDau(PhamViLoc phamVi);     // phòng không còn phù hợp, chờ bác sĩ xác nhận, chưa có giường
Optional<Instant> batDauLuotTrong(long giuongId);
    // bổ sung theo plan 004 (004 FR-012, CHK015): thời điểm giường về Trống gần nhất mà sau đó chưa có phân bổ bắt đầu,
    // chưa vào Đang bảo trì, Tạm ngừng sử dụng, Không sử dụng; khoảng Đang giữ chỗ (hồ sơ chờ) rồi về Trống vẫn cùng lượt.
    // Tính từ lich_su_trang_thai_giuong; rỗng khi giường không Trống
List<GiuongView> choSanSangQuaHan(long tangId, Instant t);
    // bổ sung theo plan 008 (008 FR-039 f): giường Chờ sẵn sàng của tầng đã quá CFG-M03-04; chỉ để hiển thị trong danh sách
    // bản ghi nguồn của bàn giao, không lập mục (Q-307)
```

**Bổ sung đã duyệt 2026-10-25 (T061)**: kiểu mã nguồn `BedFlagView(Long residentId, Long bedId, Set<String> flags)` dùng `bedId = null` chỉ cho dấu `WITHOUT_BED`; không dùng id giường giả như0 hoặc-1. Các dấu gắn phân bổ vẫn có bedId thật. Chỉ thay khả năng null của trường này, giữ nguyên thao tác và trường khác.

`TAM_NGUNG` không tính vào giường có thể dùng (Q-246).

### 1.8a. `GiuongThongKeApi` (016; bổ sung theo plan 016, research O-10)

```java
Stream<TrangThaiGiuongTaiNgayView> trangThaiTaiNgay(PhamViLoc phamVi, Instant t);   // 016 FR-024 (ngày đã qua)
Stream<KhoangChoSanSangView> choSanSang(PhamViLoc phamVi, Instant tu, Instant den); // 016 FR-033: khoảng Chờ sẵn sàng, vượt CFG-M03-04
Stream<PhongCachLyView> phongCachLy(PhamViLoc phamVi, Instant t);                   // 016 FR-020
```

*(Bổ sung 2026-10-27, luật "Đổi api", A làm cho 016, chủ dự án duyệt; C review)* `TrangThaiGiuongTaiNgayView` (`BedStateOnDate`) thêm `roomId` và `holdReason` (`WAITING_RECORD`, `ABSENT_RESIDENT`, suy từ sự kiện vào Đang giữ chỗ `HOLD_WAITING_RECORD`, `ABSENT_KEEP_BED`; rỗng khi không giữ chỗ) cho 016 FR-020, FR-024; `KhoangChoSanSangView` (`AwaitingReadyInterval`) thêm `startedAt`: lúc vào Chờ sẵn sàng, không cắt theo khoảng tra cứu (016 FR-033, Q-390).

### 1.9. `CanhBaoBanGiaoApi` (008)

```java
List<CanhBaoView> theoTang(long tangId, Instant t);   // phân bổ mang dấu "chờ bác sĩ xác nhận vùng cách ly" (FR-024a)
```

Chỉ là cảnh báo chưa xử lý để 008 hiển thị trong bàn giao (1.3). Giường Chờ sẵn sàng quá hạn không có ở đây: người bàn giao tự ghi (FR-044, Q-307).

### 1.10. SPI 003 cài cho module khác

| SPI | Loại / đối tượng | Ghi chú |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (001) | `HOAN_TAT_TIEP_NHAN`, `HUY_TIEP_NHAN`, `CHO_TAM_VANG`, `CHUYEN_VIEN`, `GHI_NHAN_TRO_VE`, `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI` | Research G-06; `@Order` sau 004; `chiTiet` sở hữu: `HOAN_TAT_TIEP_NHAN.doiPhanBoSangGiuongId`; cài `canhBao` cho `GHI_NHAN_TRO_VE` (FR-024a) |
| `LoaiYeuCauHandler` (000) | `M03.NHAP_BU_PHAN_BO` | `quyenTao` = `PHAN_BO_CHUYEN_GIUONG, TAO`; `quyenDuyet` = AC-01; `kiemTraDieuKienApDung` chạy lại FR-018, FR-019, FR-027 (c); `apDung` thực hiện chuyển giường nhập bù |
| `DinhChinhDuocProvider` (000) | `M03.PHAN_BO_GIUONG`, `M03.XAC_NHAN_SAN_SANG` | Research G-10; `tangKhuVuc` là tầng của giường |
| `PhanLoaiTruongProvider` (000) | Các loại ở data-model mục 9 | |
| `LuuTruProvider` (000) | data-model mục 10 | |
| `GanPhamViProvider` (002) | `M03.GIUONG`, `M03.PHONG`, `M03.PHAN_BO_GIUONG` | Research G-14 |
| `PhanBoGiuongApi` (002) | Mục 1.1 | |

### 1.11. Sự kiện 003 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `GiuongDaTrong(giuongId, lyDo, thoiDiem)` | 004 (báo Hành chính chọn hồ sơ chờ), 016 | FR-014, BR-M03-06 |
| `PhanBoDaBatDau(phanBoId, nctId, giuongId, thoiDiem)`; `PhanBoDaDong(phanBoId, nctId, giuongId, thoiDiem, lyDo)` | 004 (hồ sơ chờ Đã tiếp nhận khi đã xử lý trong lệnh; dùng để đối soát), 016 | FR-017 |
| `DaChuyenGiuong(nctId, phanBoCuId, phanBoMoiId, tangCu, tangMoi, thoiDiem)` | 005 (BR-M03-04), 011, 016 | FR-031 |
| `DaChuyenGiuongKhacGia(phanBoId, nctId, nguoiThucHien)` | 009 (Hành chính, Quản lý viện), 004 | FR-030 |
| `BacSiDuocGhiChiDinh(phanBoId, bacSiId)` | 009 | FR-010, BR-M03-21 |
| `TroVePhongCachLy(phanBoId, nctId, phongId)` | 009 (Khẩn cấp: bác sĩ trực, Trưởng tầng) | FR-024a |
| `PhanBoKhongConPhuHop(phanBoId, dieuKien)` | 009 (Hành chính, Trưởng tầng phụ trách giường) | FR-034, BR-M03-23 |
| `PhanBoTuongLaiQuaBatDau(phanBoId, mocDuKien)` | 009 (Hành chính) | FR-023 |
| `PhanBoTuongLaiBiHuyDoGiuongHong(giuongId, danhSachNct)` | 009 (Hành chính, Trưởng tầng) | FR-013a |
| `ChuyenTheoLichChoGiuong(phanBoId, giuongId)` | 009 (Trưởng tầng, Hành chính) | FR-011a |
| `GiuChoNguoiVangHetHan(giuongId, nctId)` | 004 (BR-M02-07) | FR-015 |
| `NhacPhanBo(loaiDau, doiTuongId, lanNhac)` | 009 | FR-025a, FR-030, FR-034 |
| `NhacGiuongChoSanSang(giuongId, han)` | 009 (Trưởng tầng được giao) | FR-044 |
| `NguoiRoiGiuongVaoDanhSachTiepXuc(giuongId, nctId)` | 009 (Trưởng tầng phụ trách) | FR-045 |
| `ViecLichTre(viec, doiTuong, duKien, thucTe)` | 009 (Quản lý viện) | FR-016a |
| `KhuNghiVuotSucChua(danhSachBuoi)` | 009 (Quản lý viện) | FR-040, BR-M03-25 |
| `PhongDaDatCachLy(phongId, suCoId)`; `PhongDaGoCachLy(phongId)` | 007, 016 | FR-008 |
| `PhanBoDaDinhChinh(phanBoId, truoc, sau)` | 007 (xem lại danh sách tiếp xúc) | FR-026, BR-M03-24 |

Mỗi sự kiện gửi 009 mang mức, nhóm người nhận, loại thông tin và khóa sự kiện theo research G-12.

---

## 2. 003 cần từ module khác

### 2.1. SPI 003 khai báo ở `roombedallocation.api`, module khác cài

```java
interface NguonLuuTruChoPhanBo {                 // 004 cài
  Optional<LoaiLuuTru> loaiLuuTru(long nctId);   // hợp đồng Hiệu lực → hợp đồng đang lập → lượt đăng ký (CHK007)
  Optional<MaMucChamSoc> mucTheoHopDong(long nctId);       // hợp đồng Nháp, Chờ ký, Hiệu lực (Q-301)
  Optional<MaMucChamSoc> mucTheoLuotDangKy(long nctId);
  Optional<Instant> ketThucDuKien(long nctId);             // nội trú ngắn ngày
  GiuGiuong giuGiuongKhiVang(long nctId);                  // gọi trong lệnh vắng, sau bean 004: (giu, han?)
  boolean khacDonGia(long nctId, long loaiPhongCu, long loaiPhongMoi, LocalDate ngay); // FR-030, CHK016
  void hoSoChoHetHanGiuCho(long hoSoChoId);                // MANDATORY; job HetHanGiuCho
  void daPhanBo(long nctId, long phanBoId);                // MANDATORY; hồ sơ chờ Đã tiếp nhận (004 FR-016)
}

interface DemLichDenBanTru {                     // 004 cài
  Map<BuoiNgay, Integer> demTuNgay(LocalDate tuNgay, List<BuoiBanTru> buoi);   // đếm lại theo khung (FR-040)
  boolean coLichTuongLai(LocalDate tuNgay);                                    // chặn Ngừng hiệu lực khu nghỉ
}

interface TinhTrangLayNhiemChoPhanBo {           // 007 cài
  VungKhoanh vungDangKhoanh(Instant t);          // tập tầng, tập khu vực (FR-009, Q-69)
  boolean thuocDanhSach(long nctId, Instant t);  // nghi nhiễm hoặc tiếp xúc (FR-045, BR-M03-10)
}

interface ThamChieuViTri {                       // 008 cài; module khác MAY cài
  List<ThamChieuView> dangThamChieu(ViTriRef viTri);   // phân công Hiệu lực, Nháp, giao Trưởng tầng, ca chưa đóng (FR-005 c)
  boolean daTungThamChieu(ViTriRef viTri);             // quyết định có cho xóa không
}
```

Sự kiện 003 nghe: `KhoanhVungDaGo(tangIds, khuVucIds)`, `DaThemVaoDanhSachTiepXuc(nctId, thoiDiem)` (007); `LoaiLuuTruDaDoi(nctId)` (004); `MucChamSocDaDoi`, `DanhGiaDauVaoDaXacNhan`, `ThongTinCaNhanDaCapNhat` (001); `ThamSoDaDoi(CFG-M03-01)` (000).

### 2.2. Giao diện có sẵn hoặc đã bổ sung

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `gioiTinh`, `nhanDang` | M01 (001) | FR-018 (c), (h); kết quả tra cứu | Có sẵn sau 001 |
| `MucChamSocApi.hienHanh` | 001 | FR-018 (a) | Có sẵn |
| `TrangThaiNguoiCaoTuoiApi.khoangTrangThai(nctId, tt, tu, den)` (bổ sung theo plan này) | 001 | FR-027 (c), FR-036 | Bản giả |
| SPI `ThamGiaChuyenTrangThai.canhBao` (bổ sung theo plan này) | 001 | FR-024a (1) | 003 vẫn đặt dấu và báo; chỉ thiếu cảnh báo trước khi xác nhận |
| `QuyenApi`, `PhamViApi` (`trongPhamVi`, `boLocLichSu`, `truongTangDuocGiao`, `nguoiPhuTrachCa`), `TaiKhoanApi.coVaiTro`, `NguoiDungHienTai` | M15 (002) | Research G-14 | Có sẵn sau 002 |
| Ngoại lệ quyền theo nhiệm vụ cho Người phụ trách ca với Xác nhận giường sẵn sàng | 002 | FR-044 | Chưa có (plan, điểm báo lại 1); chỉ Trưởng tầng được giao thực hiện được |
| `NhiemVuApi`: bác sĩ trực | M09 (008) qua 002 | Người nhận `TroVePhongCachLy` | Mọi bác sĩ |
| `LoaiYeuCauHandler.kiemTraKhiDuyet`, `khiDuyet` (bổ sung theo plan này) | M15 (000) | FR-029 qua handler của 004 | Không có thì chuyển theo lịch chỉ đặt trước được ở ngày hiệu lực |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `YeuCauPheDuyetApi`, `common` | 000 | Mọi lệnh | Có sẵn |

---

## 3. Giao diện nội bộ M03 với phần tài sản (feature 019)

Hai port ở gói `roombedallocation.giuong.port`, chỉ `roombedallocation.taisan` được dùng (ArchUnit). Mọi hàm `Propagation.MANDATORY`, chạy trong transaction của lệnh tài sản hoặc lệnh Tạo giường.

```java
interface GiuongTheoTaiSanPort {                 // 003 cài, 019 gọi
  List<DieuKien> kiemTraLenhTaiSan(long giuongId, LenhTaiSanGiuong lenh, boolean ganHuHongKhiDonGiuong, boolean laBaoHongLai);
      // FR-013, FR-013a; giường có người → BR-M03-05 (019 tự ghi dấu "chờ chuyển người" với Báo hỏng)
  void apLenhTaiSan(long giuongId, LenhTaiSanGiuong lenh, CanCu canCu);
      // BAO_TRI | SAN_SANG | THANH_LY; đổi trạng thái giường; hủy phân bổ tương lai khi ngoại lệ FR-013a áp dụng
  void daGanDauChoChuyenNguoi(long giuongId, List<Long> nctAnhHuong);   // FR-013b: báo phân bổ tương lai bị ảnh hưởng
  void daGoDauChoChuyenNguoi(long giuongId);   // FR-013b: giường Trống → GiuongDaTrong; chuyển theo lịch đang chờ → thực hiện
  List<ViTriView> giuongTrongPhuHopChoNguoiDangNam(long giuongId);       // Q-256: theo tầng, đạt FR-018
}

interface TaiSanGiuongPort {                     // 019 cài, 003 gọi
  void ghiTangChoGiuong(long giuongId);          // Q-238, DBR-32: trong lệnh Tạo giường
  boolean mangDauChoChuyenNguoi(long giuongId);  // FR-018 (i), FR-014
  Set<Long> giuongMangDau(Collection<Long> giuongIds);   // lọc danh sách giường
}
```

Khi 019 chưa có, bản giả `TaiSanGiuongPort` không ghi gì và trả "không mang dấu"; test của 003 bật dấu qua công tắc giả lập.
