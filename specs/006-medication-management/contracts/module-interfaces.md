# Giao diện giữa module: Feature 006

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Hợp đồng trong backend, ở gói `vn.eldercare.m07.api`. Module khác chỉ dùng các giao diện này, không đọc bảng của 006 (constitution X). Chữ ký viết dạng Java rút gọn để mô tả hợp đồng; mã thật viết ở giai đoạn triển khai.

Chiều phụ thuộc theo research D-02: 006 phụ thuộc 000, 001, 002, 003, 004, 005. Các module 007, 008, 010, 012, 014, 016 phụ thuộc 006. Chỗ 006 cần dữ liệu của module phụ thuộc nó thì 006 khai báo SPI (mục 2.1), module đó cài. Plan của các module này phải giữ đúng chữ ký hoặc ghi rõ thay đổi.

---

## 1. 006 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M07 cài

| SPI (gói khai báo) | Bản cài của 006 | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Bean `@Order` sau 005. Điều kiện `KT_C_THUOC_GUI` cho `KET_THUC_LUU_TRU`; tác động theo research D-16 | 001 H-02 |
| `MucViecSauQuaDoiPort` (`m02.api`) | `ma() = THUOC_GUI`; `trangThai(nctId)`: `CHUA_HOAN_THANH` khi còn lô Chờ đối chiếu, Được sử dụng, Chỉ giữ hộ hoặc lần giao chưa Đã nhận lại; `KHONG_AP_DUNG` khi chưa từng có; còn lại `HOAN_THANH` | 004 FR-071, Q-64 |
| `LieuThuocChecklistPort` (`m04.api`) | `theoCa(nctIds, tu, den)` trả `LieuHienThiView` (mục 1.3); giữ đúng chữ ký của 005 | 005 FR-029 |
| `ThoiKhoaBieuNguonPort` (`m04.api`) | `theoNgay(nctId, ngay)`: liều định kỳ trong ngày (thời điểm, nguồn "thuốc", trạng thái hiện hành), không nêu tên thuốc khi người xem không có quyền xem thuốc | 005 FR-048 |
| `NgoaiTuyenHandler` (`m15.taikhoan.api`) | `M07.KET_QUA_LIEU`, `M07.LAN_DUNG_PRN` (research D-11); từ chối thuốc kiểm soát đặc biệt | 002 A-11 |
| `DinhChinhDuocProvider` (000) | `M07.KET_QUA_LIEU`, `M07.LAN_DUNG_PRN`, `M07.LAN_GIAO_THUOC`, `M07.LAN_NHAN_LAI_THUOC`, `M07.PHIEU_DOI_CHIEU` (research D-12) | 000 R-08 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 12, 13 | |
| `GanPhamViProvider` (002) | Mọi loại của 006: trả `DoiTuong` với người cao tuổi và tầng/khu hiện tại | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M07.ANH_DON_THUOC`, `M07.ANH_CAN_CU`, `M07.ANH_THUOC_GUI`: quyền tải bằng quyền xem bản ghi chủ | NFR-08 |

**Kiểu `chiTiet` 006 đọc** (do module khác sở hữu): `CHO_TAM_VANG.mangThuoc`, `duKienTroLai` (004); `GHI_NHAN_TRO_VE.thoiDiemVe` (004); `GHI_NHAN_QUA_DOI.thoiDiemQuaDoi` (004); `DIEM_DANH_ROI_VIEN.chuyenId` (014; đề xuất, plan 014 giữ hoặc ghi thay đổi).

### 1.2. `DonThuocApi` (đọc; 007, 016)

```java
List<ThuocDangDungView> thuocDangDung(long nctId);
    // FR-050: đơn Hiệu lực và Tạm dừng, kèm thuốc, liều, đường dùng, mốc giờ hoặc giới hạn PRN, nguồn; 007 thẻ khẩn cấp, tóm tắt chuyển viện
List<DonThuocView> lichSu(long nctId);   // FR-016a: chuỗi thay thế theo thời gian
```

### 1.3. `LichThuocApi` (đọc; 005 qua SPI, 008, 016)

```java
record LieuHienThiView(String lieuKey, long nctId, long donThuocId, Instant thoiDiemDuKien,
                       Instant cuaSoTu, Instant cuaSoDen, TrangThaiLieu trangThai,
                       boolean lieuChungTang, boolean tonChoXacNhanBanGiao, boolean choGhiNhanSauTroVe,
                       boolean batBuocTrucTuyen, NguonThuoc nguon, Optional<Long> lanGiaoId,
                       Optional<ThuocTomTat> thuoc) {}   // thuoc mang @NhomTruong(SUC_KHOE)
List<LieuHienThiView> theoNguoiCaoTuoi(long nctId, Instant tu, Instant den);
Set<Long> nguoiDaGhiLieu(long nctId, Instant tu, Instant den);
    // bổ sung theo plan 007: nhân viên đã ghi kết quả liều hoặc lần dùng PRN cho người đó trong khoảng (007 FR-060 c, CHK018)
```

Liều trả về là kết quả tính từ đơn ghép với dòng đã vật hóa (research D-06); gọi hàm này không tạo bản ghi.

### 1.4. `LieuBanGiaoApi` (008; FR-028, BR-M09-06, Q-307)

```java
List<LieuChoBanGiaoView> trongCa(long tangKhuVucId, long caId);
    // liều Trễ, Bỏ lỡ, Từ chối có thời điểm dự kiến trong ca; liều Mang theo "chờ ghi nhận sau khi trở về";
    // lần giao Chờ nhận lại; bản ghi thuốc kiểm soát đặc biệt Chờ xem lại. Không có người đã ở trạng thái cuối (FR-047)
Map<String, TrangThaiLieu> trangThaiHienHanh(Collection<String> lieuKeys);
```

Không có hàm lập sẵn mục bàn giao (Q-307).

### 1.5. `MangThuocChuyenDiApi` (014)

```java
boolean daChuanBi(long nctId, long chuyenId);
    // FR-048b: có lần giao cho chuyến, hoặc lệnh "Không mang thuốc", hoặc không có liều/đơn PRN nào trong khoảng đi
void chuyenThayDoi(long chuyenId, long nctId, LoaiThayDoiChuyen loai);  // MANDATORY; HUY_CHUYEN | KHONG_DI | DOI_NGAY: nhắc Nhận lại (Q-58)
void tinhLaiKhoang(long chuyenId, long nctId, KhoangThoiGian khoangMoi); // MANDATORY; đính chính thời điểm rời/về (FR-048b)
```

### 1.6. `ThuocGiaDinhGuiApi` (004, 012, 016)

```java
boolean conDangGiu(long nctId);                                // 004 FR-063 (c): lô chưa ở trạng thái cuối hoặc lần giao chưa nhận lại
List<ThuocGuiView> theoNguoiCaoTuoi(long nctId);               // tên, trạng thái, số lượng còn, hạn dùng
```

### 1.7. `ThuocCongNguoiThanApi` (012; FR-049, Q-59)

```java
List<ThuocGuiView> thuocGui(long nctId, long nguoiThanId);              // mọi người thân có quan hệ Hiệu lực (CHK051)
Optional<List<DonThuocNguoiThanView>> donThuoc(long nctId, long nguoiThanId);      // rỗng khi không có bản đồng ý bao gồm người thân
Optional<List<PhieuDoiChieuView>> phieuDaXacNhan(long nctId, long nguoiThanId);    // như trên
```

Không có hàm trả liều cho người thân. Mỗi lần xem được 012 ghi nhật ký lượt xem sức khỏe (NFR-08, 012 FR-048; theo plan 012).

### 1.8. `ThuocThongKeApi` (016; FR-051)

```java
Stream<LieuBaoCaoView> lieuTheoKhoang(PhamViLoc phamVi, Instant tu, Instant den);
    // người cao tuổi, tầng, ca (theo thời điểm dự kiến), thời điểm dự kiến, trạng thái hiện hành (sau đính chính),
    // người đóng và quy tắc đóng, căn cứ (liều chung, mang theo), nhãn dùng trễ, nhãn ghi sau mất kết nối, kết quả xem lại;
    // gồm liều kết thúc ở Tạm dừng, Đã hủy để 016 loại khỏi mẫu số theo FR-051
Stream<PhieuDoiChieuBaoCaoView> phieuTheoKhoang(PhamViLoc phamVi, Instant tu, Instant den);  // lý do, trạng thái, hạn, dấu quá hạn
```

### 1.9. Sự kiện 006 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `YeuCauCanhBao(loai, muc, nctId, nguon, khoa)`; loại `BO_LO_LIEU`, `PHAN_UNG_THUOC`, `TU_CHOI_LIEN_TIEP`, `XUNG_DOT_DI_UNG`, `CHUA_DOI_CHIEU`, `PRN_VUOT_GIOI_HAN`, `NGHI_DUNG_TRUNG_LIEU` | 007 (tạo hoặc gộp theo BR-M05-02) | FR-012, FR-021, FR-027, FR-029, FR-031, FR-032, FR-034 |
| `YeuCauDongCanhBao(nguon, lyDo)` (`DA_DUNG_TRONG_CUA_SO`, `DA_DOI_CHIEU`, `TRANG_THAI_CUOI`, `DINH_CHINH`) | 007 | FR-029, FR-030, FR-036, FR-047 |
| `NhacLieuTre(lieuKey, nguoiNhan)`; `NhacLieuTreGop(nguoiNhan, lieuKeys)` | 009 (Trung bình) | FR-022, FR-026a |
| `NctChuaCoDieuDuongPhuTrach(nctId, caId)`; `TangKhongCoDieuDuong(tang, caId, lieuSapToiGio)` | 009 (Trung bình; Trưởng tầng, Người phụ trách ca / Quản lý viện) | FR-026a, BR-M07-21 |
| `LieuKsdbChoXemLai(xemLaiId, nguoiXemLai)` | 009 (Trung bình; Trưởng tầng chỉ được báo) | FR-029, Q-66 |
| `DonSapHetHan(donId, nguoiNhan)` | 009 (Nhẹ) | FR-017 |
| `CanLapPhieuDoiChieu(nctId, lyDo, han)`; `PhieuDoiChieuDaGui`, `…DaTraLai` | 009 (Trung bình; Nhẹ) | FR-034, FR-035 |
| `ThuocDangDungTiepNhanDaDoi(nctId, phieuId)` | 009 (Nhẹ; người thực hiện, người xác nhận) | FR-034a, BR-M07-22 |
| `ThuocGuiSapHet(khoaNhom, moc, soThuTu, soNgayConLai)` | 009 (Trung bình; người liên hệ chính, người đại diện; thêm Điều dưỡng phụ trách với CFG-M07-07; nội dung chia phần `CHUNG`, `SUC_KHOE`) | FR-046 |
| `ThuocGuiHetHanDung(loId)`; `ThuocGuiDaDungHet(loId)`; `ThuocGuiChuyenChiGiuHo(loId, lyDo)`; `HetThuocGuiKhiDenLieu(lieuKey)` | 009 | FR-041, US7 #8 |
| `CanNhanLaiThuocMangTheo(lanGiaoId)` | 009 (Điều dưỡng phụ trách) | FR-048b, Q-58 |
| `SuKienTinhPhiThuoc(nguon: LIEU \| LAN_DUNG_PRN \| LAN_GIAO, id, nctId, thuocId, soLuong, thoiDiem)` | 010 | FR-025a, FR-033, FR-048a, BR-M07-14 |
| `SuKienTinhPhiThuocDaDieuChinh(nguon, id, soLuongMoi, lyDo: DINH_CHINH \| NHAN_LAI)` | 010 | FR-030, FR-048a, DBR-17 |

Mỗi sự kiện mang khóa `loại + id + mốc` để bên nghe bỏ trùng (NFR-04). Mức theo research D-17.

---

## 2. 006 cần từ module khác

### 2.1. SPI 006 khai báo ở `m07.api`, module khác cài

```java
interface DieuDuongCaPort {                 // 008 cài; chỉ đọc
  Optional<CaView> caChua(long tangKhuVucId, Instant thoiDiem);           // ca của tầng chứa thời điểm
  Optional<Long> duocPhanCong(long nctId, Instant thoiDiem);              // điều dưỡng phụ trách người cao tuổi trong ca
  List<Long> dieuDuongCoCa(long tangKhuVucId, Instant thoiDiem);          // FR-026a: liều chung, BR-M07-21
  boolean coTenTrongCaDangDienRa(long nhanVienId, Instant thoiDiem);      // FR-026a câu cuối (Q-85)
  boolean banGiaoDaXacNhan(long caId);                                    // FR-028, 008 FR-044a
}

interface ChuyenDiThuocPort {               // 014 cài; chỉ đọc
  Optional<KhoangThoiGian> danhGiaDat(long nctId, long chuyenId);         // FR-048b: khoảng đi dự kiến khi người đó được đánh giá Đạt
  Optional<KhoangThoiGian> khoangDi(long chuyenId);                       // bean DIEM_DANH_ROI_VIEN
}

interface DeNghiMuaHoPort {                 // 010 cài; chỉ đọc
  Optional<DeNghiMuaHoView> daMua(long deNghiId, long nctId);             // FR-040, Q-139: đề nghị ở Đã mua, thuốc, số lượng, nhân viên mua
}
```

Bean thiếu thì lệnh cần nó bị chặn (mặc định an toàn): không xác định được người phụ trách thì mọi liều là liều chung của tầng và chỉ Người phụ trách ca xác nhận được; thiếu `ChuyenDiThuocPort` thì lệnh "Không mang thuốc" trả 409; thiếu `DeNghiMuaHoPort` thì tiếp nhận thuốc mua hộ trả 409.

### 2.2. Sự kiện 006 nghe

| Sự kiện | Module | Xử lý | FR |
| --- | --- | --- | --- |
| `DiUngDaGhiNhan(mucId, nctId, diNguyenId)` | 001 | Kiểm đơn, `YeuCauCanhBao(XUNG_DOT_DI_UNG)`, `MucSucKhoeApi.daKiemTra(mucId, THUOC)`; idempotent theo `mucId` | FR-012 |
| `HoSoSucKhoeBanDauDaDoi(nctId)` (bổ sung vào hợp đồng 001) | 001 | Phiếu tiếp nhận Mở: báo người thực hiện; Chờ xác nhận, Đã xác nhận: báo người thực hiện và người xác nhận | FR-034a, BR-M07-22 |
| `LuotVangDaGiaHan(luotVangId, nctId, moc)` | 004 | Cập nhật `den_du_kien` của khoảng vắng | FR-048 |
| `ThamSoDaDoi(CFG-M07-*, CFG-M05-04)` | 000 | Xóa bộ đệm tham số; không tính lại kết quả cũ | FR-002, Edge Case |

### 2.3. Giao diện có sẵn

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `thoiDiemTrangThaiCuoi`; `MucSucKhoeApi.hieuLuc`, `daKiemTra`; `HoSoSucKhoeApi.banDau`; `DiNguyenApi.diNguyenHieuLuc(THUOC)`; `BanDongYApi.baoGom` | M01 (001) | FR-001, FR-007, FR-008, FR-012, FR-034a, FR-049 | Có sẵn |
| `QuanHeNguoiThanApi.thuocNguoiCaoTuoi` (khai báo theo 001, 012 cài); `NguoiDaiDienPort.laNguoiDaiDienHieuLuc` (`m02.api`, 012 cài) | 012 | FR-040, FR-041 (hoàn trả, hủy) | Bản giả |
| `ViTriApi.hienTai` | M03 (003) | Tầng của liều (FR-026, CHK033) | Có sẵn |
| `DanhSachViecSauQuaDoiApi.capNhat`; `DonGiaApi.thuocNguonVien` | M02 (004) | FR-047; dấu "chưa có mục vật phẩm" (FR-003) | Có sẵn |
| `LieuThuocChecklistPort`, `ThoiKhoaBieuNguonPort` (khai báo) | M04 (005) | 006 cài | Có sẵn |
| `QuyenApi` (khóa `CHUYEN_MON` cho kê đơn nội bộ, phát thuốc), `PhamViApi.nguoiPhuTrachCa`, `QuyenXemTruongApi`, `NguoiDungHienTai` | M15 (002) | Research D-08, D-18 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `TepApi`, `common` | M15 (000) | Mọi lệnh | Có sẵn |
| Bản cài các SPI mục 2.1 | 008, 014, 010 | | Bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}` |
