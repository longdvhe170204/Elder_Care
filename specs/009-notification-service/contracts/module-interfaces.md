# Giao diện giữa module: Feature 009

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Khuôn yêu cầu thông báo ở `common.thongbao` (mọi module dùng), giao diện công khai ở `m13.api`, bản cài SPI do module khác khai báo (research B-02). Kiểu `NguoiDungHienTai`, `DoiTuong`, `VaiTro` theo 002. Mọi hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi; hàm đọc không khóa.

---

## 1. 009 cung cấp

### 1.1. Khuôn yêu cầu thông báo (`common.thongbao`; bổ sung theo plan này vào hợp đồng `common` của 000)

```java
interface CoYeuCauThongBao { List<YeuCauThongBao> yeuCauThongBao(); }   // sự kiện miền của module nguồn cài

record YeuCauThongBao(String moduleNguon, String loaiSuKien, NhomNguon nhomNguon,
                      BanGhiNguon banGhiNguon, String khoaSuKien, Optional<MucThongBao> muc,
                      Optional<Long> nctId, Optional<Long> tangKhuVucId,
                      List<NhomNhan> nhomNguoiNhan, List<PhanNoiDung> noiDung,
                      Optional<String> noiDungRutGon, Optional<String> tieuDe,
                      Optional<Long> nguoiGayRa, boolean baoNguoiGayRa,
                      Instant thoiDiemPhatSinh, Map<String, Object> thamSoHienThi,
                      boolean viecSauQuaDoi,          // viecSauQuaDoi: dấu của 004 (FR-015)
                      Optional<DoiTuongThongTin> doiTuongThongTin,   // bổ sung 2026-10-18 (FR-001a); bắt buộc khai khi có nhóm người thân
                      Optional<MaTruongHopQ354> maTruongHop) {}      // bổ sung 2026-10-18 (FR-001a, FR-014a); chỉ khi nội dung có thể vượt phạm vi đồng ý

enum DoiTuongThongTin { VE_NGUOI_CAO_TUOI, VE_VIEC_NGUOI_THAN }
enum MaTruongHopQ354 { KHAN_CAP_NGUOI_LIEN_HE_CHINH, DANH_SACH_TIEP_XUC, XAC_NHAN_LAI_NGUYEN_VONG, CHUYEN_VIEN, QUA_DOI }

enum MucThongBao { NHE, TRUNG_BINH, KHAN_CAP }
enum LoaiThongTin { CHUNG, SUC_KHOE, CHI_PHI, THAM_NOM, HOP_DONG }
record PhanNoiDung(LoaiThongTin loai, String noiDung) {}
record BanGhiNguon(String loai, String id) {}     // ví dụ ("M05.SU_CO", "123")

sealed interface NhomNhan {                        // research B-04; mỗi nhóm loại (a) có CachThay
  record DieuDuongPhuTrach(long nctId, CachThay cachThay) implements NhomNhan {}
  record NhanVienPhanCong(Optional<Long> nctId, Optional<Long> phongId, Optional<VaiTro> vaiTro, CachThay cachThay) implements NhomNhan {}
  record NhanVienCoCa(long tangKhuVucId, Set<VaiTro> vaiTro, Set<String> chungChi, CachThay cachThay) implements NhomNhan {}
  record NguoiPhuTrachCa(long tangKhuVucId, CachThay cachThay) implements NhomNhan {}
  record BacSiTruc(CachThay cachThay) implements NhomNhan {}
  record TruongTang(long tangKhuVucId, CachThay cachThay) implements NhomNhan {}
  record TruongDoan(long chuyenId, CachThay cachThay) implements NhomNhan {}
  record PhuTrachBoPhan(long boPhanId, CachThay cachThay) implements NhomNhan {}
  record VaiTroToanVien(VaiTro vaiTro) implements NhomNhan {}                 // QUAN_LY_VIEN, HANH_CHINH, BAC_SI, NHAN_VIEN_BEP, KE_TOAN
  record NguoiCoQuyenDuyet(String loaiYeuCau, List<Long> taiKhoanIds) implements NhomNhan {}
      // sửa theo plan 004, Q-338 (2026-10-11): 000 tính sẵn danh sách người duyệt đã lọc theo gioiHanVaiTroDuyet
      // (000 contracts mục 1.5 "Danh sách người duyệt"); 009 chỉ gửi cho các tài khoản này, không tự tra NguoiDuyetApi
  record NhanVien(long nhanVienId, String canCu) implements NhomNhan {}       // (d), kể cả người tạm nhận (căn cứ TAM_NHAN)
  record NguoiThan(long nguoiThanId, String canCu) implements NhomNhan {}     // (d): chỉ phần CHUNG khi không có quan hệ (Q-112)
  record NguoiLienHeChinh(long nctId) implements NhomNhan {}
  record NguoiDaiDien(long nctId) implements NhomNhan {}
  record NguoiThanCoQuyen(long nctId, QuyenNguoiThan quyen) implements NhomNhan {}  // XEM_SUC_KHOE, XEM_CHI_PHI, DANG_KY_THAM, NHAN_KHAN
  record NguoiCaoTuoi(long nctId) implements NhomNhan {}                    // (f) bổ sung 2026-10-18 (FR-007, FR-022a): chỉ Nhẹ, chỉ phần CHUNG
}
sealed interface CachThay { MacDinh, KhongThay, ChiDinh(NhomNhan nguoiThay) }

Set<String> TRUONG_SUC_KHOE;   // danh mục khóa thamSoHienThi cấm trong nội dung rút gọn (FR-020)
```

Nhóm người nhận các plan trước đã dùng ánh xạ như sau: `BAC_SI_TOAN_VIEN` = `VaiTroToanVien(BAC_SI)`; `NGUOI_TRONG_BAN_GHI_NGUON` = danh sách `NhanVien`/`NguoiThan`; `TRUONG_TANG(tang)` của 007 leo thang = `TruongTang(tang, KhongThay)`; `BAC_SI_TRUC` khi không có Bác sĩ trực (007 FR-047b) = `BacSiTruc(ChiDinh(VaiTroToanVien(BAC_SI)))`.

### 1.2. `m13.api.ThongBaoTraCuuApi` (đọc; 000, 007, 012, 016)

```java
List<NguoiDaThongBaoView> theoNguon(String loaiBanGhiNguon, String idBanGhiNguon);
    // FR-035: người nhận, căn cứ, trạng thái, kết quả kênh, gửi lúc, xem lúc, xác nhận lúc và cách, lượt nhắc, kết quả gọi, dấu thiếu mức
Optional<Instant> xemLuc(long thongBaoId);    // BR-M10-06 (012 bản tin)
```

Bản cài `NhatKyThongBaoPort` của 007 (`m05.api`) ủy cho `theoNguon`.

### 1.3. SPI 009 cài cho module khác

| SPI (gói khai báo) | Bản cài | Dùng bởi |
| --- | --- | --- |
| `NhatKyThongBaoPort` (`m05.api`) | `theoNguon` mục 1.2 | 007 FR-040, FR-048b |
| `DinhChinhDuocProvider` (000) | `M13.KET_QUA_GOI`, `M13.KET_QUA_LIEN_HE` | 000 R-08 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 9, 10 | |
| `GanPhamViProvider` (002) | `M13.THONG_BAO`: người nhận (của tôi), tầng của yêu cầu (tra cứu Trưởng tầng) | 002 FR-034 |

### 1.4. Sự kiện 009 phát

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `ThongBaoDaXem(thongBaoId, banGhiNguon, nguoiNhan, xemLuc)` | 012 (BR-M10-06), module nguồn quan tâm | FR-034, FR-044 |
| `ThongBaoKhanCapDaXacNhan(thongBaoId, banGhiNguon, cach)`; `ThongBaoKhongLienLacDuoc(thongBaoId, banGhiNguon)` | 007 (người được thông báo), 016 (giai đoạn sau) | FR-035, FR-044 |

---

## 2. 009 cần từ module khác

### 2.1. SPI 009 khai báo ở `m13.api`, module khác cài

```java
interface NguoiThanNhanThongBaoPort {             // 012 cài; chỉ đọc
  List<QuanHeNhanThongBaoView> quanHeHieuLuc(long nctId);
      // người thân có quan hệ còn hiệu lực: là liên hệ chính, là đại diện, quyền (XEM_SUC_KHOE, XEM_CHI_PHI, DANG_KY_THAM, NHAN_KHAN),
      // tài khoản cổng (trạng thái), số điện thoại; người liên hệ chính luôn có NHAN_KHAN (Q-96)
  List<Long> thuTuGoi(long nctId);                 // FR-029a: người đại diện, rồi người có NHAN_KHAN, theo thứ tự 012 (012 FR-012)
  Optional<LienHeNguoiThanView> lienHe(long nguoiThanId);   // số hiện hành, tài khoản cổng (nhóm (d) không có quan hệ)
}
```

Bean thiếu thì nhóm (e) trả rỗng và nhóm (d) người thân không có số: thông báo người thân rơi vào danh sách "cần liên hệ trực tiếp" (mặc định an toàn: không gửi sai người).

### 2.2. Sự kiện 009 nghe

| Sự kiện | Module | Xử lý | FR |
| --- | --- | --- | --- |
| Mọi sự kiện cài `CoYeuCauThongBao` | 000 → 008, 010 → 019 | Tiếp nhận, xác định người nhận, gửi | FR-001 |
| `YeuCauDungNhacGoiDien(nguonLoai, nguonId)` | 007 | Dừng lượt nhắc của thông báo khẩn cấp của nguồn; không tạo lượt mới cho chuỗi | FR-029a, BR-M05-22 |
| `DaCoKetQuaXacNhanLai(canhBaoId)` | 007 | Dừng lượt nhắc chỉ chứa thông báo của cảnh báo nguy kịch đó | 007 FR-050b |
| `ThamSoDaDoi(CFG-M13-02)` | 000 | Tính lại thời điểm gửi dự kiến của thông báo đang giữ | FR-024 |
| `ThamSoDaDoi(CFG-M13-*)` khác | 000 | Xóa bộ đệm | |
| `NguoiCaoTuoiDaChuyenTrangThai(…, GHI_NHAN_QUA_DOI)` | 001 | Thông báo đang giữ cho người thân của người đó → `KHONG_GUI` "người cao tuổi đã qua đời" khi tới hạn (xét lúc giải phóng) | FR-015, FR-025 |

### 2.3. Giao diện có sẵn hoặc đã bổ sung

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `NhiemVuApi`, `PhanCongApi` (`m15.taikhoan.api`, 008 cài); `CaTrucApi.dangTrongCa`, `caDangDienRa` | M09 (008) | Nhóm (a), FR-016a, FR-029, FR-030a | Có sẵn |
| `TaiKhoanApi` (trạng thái, vai trò, `theoNguoiThan`), `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi`, `NguoiDungHienTai`; mã thiết bị (A-02) | M15 (002) | Nhóm (b), (c), FR-008, FR-034 | Có sẵn |
| `BanDongYApi.baoGom`, `NguoiCaoTuoiApi.trangThai`, `thoiDiemTrangThaiCuoi`, `nhanDang` | M01 (001) | FR-013, FR-015, FR-011 | Có sẵn |
| `CanhBaoLenhApi.tiepNhan(canhBaoId, nd)`, `SuCoLenhApi.tiepNhan(suCoId, nd)` (bổ sung theo plan này vào `m05.api`; cùng kiểm tra và tác động như REST của 007) | M05 (007) | FR-026a | Nút "Xác nhận và tiếp nhận" không hiện |
| `SoLienHeVienApi.hienHanh` | M06 (007) | Nội dung mặc định, nội dung Q-72 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `common` | M15 (000) | Mọi lệnh | Có sẵn |
| Bản cài `NguoiThanNhanThongBaoPort` | M10 (012) | | Bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}` |
