# Giao diện giữa module: Feature 005

**Ngày**: 2026-10-09 | **Plan**: [../plan.md](../plan.md)

Hợp đồng trong backend, ở gói `com.cambae.backend.m04.api`. Module khác chỉ dùng các giao diện này, không đọc bảng của 005 (constitution X). Chữ ký viết dạng Java rút gọn để mô tả hợp đồng; mã thật viết ở giai đoạn triển khai.

Chiều phụ thuộc theo research C-02: 005 phụ thuộc 000, 001, 002, 003, 004. Các module 006, 007, 008, 010, 011, 012, 014, 015, 016 phụ thuộc 005. Chỗ 005 cần dữ liệu của module phụ thuộc nó thì 005 khai báo SPI (mục 2.1), module đó cài. Plan của các module này phải giữ đúng chữ ký hoặc ghi rõ thay đổi.

---

## 1. 005 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M04 cài

| SPI (gói khai báo) | Bản cài của 005 | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Bean `@Order` sau 004, 003. Không có điều kiện chặn. Tác động theo bảng dưới | 001 H-02 |
| `TrangThaiCoMatBanTruPort` (`m02.api`) | `trangThai(nctId, ngay, buoiId)` đọc `co_mat_ban_tru`: `CO_MAT` (gồm `DA_VE`), `VANG_CO_BAO`, `VANG_KHONG_BAO`; rỗng khi `CHUA_DEN` hoặc `DA_HUY_NGAY_NGHI` | 004 FR-060 |
| `NgoaiTuyenHandler` (`m15.taikhoan.api`) | `M04.KET_QUA_CONG_VIEC`, `M04.GHI_NHAN_PHAT_SINH` (research C-14) | 002 A-11 |
| `DinhChinhDuocProvider` (000) | `M04.KET_QUA_GHI_NHAN` (`tangKhuVuc` = tầng/khu của công việc; `choPhep` sai với kết quả đo chỉ số; `sauKhiDinhChinh` phát sự kiện cho 010 khi có tính phí); `M04.DIEM_DANH_BAN_TRU` (`sauKhiDinhChinh` cập nhật giờ trên dòng có mặt) | 000 R-08 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 10, 11 | |
| `GanPhamViProvider` (002) | `M04.KE_HOACH`, `M04.CONG_VIEC`, `M04.KET_QUA_GHI_NHAN`, `M04.CO_MAT_BAN_TRU`: trả `DoiTuong` với người cao tuổi và tầng/khu | 002 FR-034 |

**Tác động của bean `ThamGiaChuyenTrangThai`** (trong transaction của 001, `Propagation.MANDATORY`):

| Lệnh | Tác động | FR |
| --- | --- | --- |
| `CHO_TAM_VANG`, `CHUYEN_VIEN`, `DIEM_DANH_ROI_VIEN` | Hủy công việc Chưa đến hạn có thời điểm dự kiến từ thời điểm rời, lý do `VANG_MAT`; công việc Đến hạn, Quá hạn giữ nguyên | FR-020, FR-027, BR-M04-04 |
| `GHI_NHAN_TRO_VE`, `DIEM_DANH_VE` | Phát `CanLapCongViec(nctId, TRO_VE, thoiDiem)` | FR-020 |
| `HUY_GHI_NHAN_ROI_VIEN` | Áp `CongViecVangMatApi.tinhLai` với khoảng đi rỗng | FR-020a |
| `HOAN_TAT_TIEP_NHAN` | Phát `CanLapCongViec(nctId, HOAN_TAT_TIEP_NHAN, thoiDiem)` | FR-017 (CHK008) |
| `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN` | Bảng FR-003 hai dòng cuối; Chưa đến hạn → Hủy; Đến hạn, Quá hạn → Không thực hiện (Hệ thống, `FR_010_TRANG_THAI_CUOI`); phát `YeuCauDongCanhBao(TRANG_THAI_CUOI)`; không nhắc, không leo thang tiếp | FR-010 |

### 1.2. `KeHoachChamSocApi` (đọc; 007, 011, 012, 016)

```java
Optional<PhienBanKeHoachView> hieuLucTaiNgay(long nctId, LocalDate ngay);   // kèm danh sách mục
Optional<Integer> mucTieuNuocMl(long nctId, LocalDate ngay);               // FR-039; 016 hiển thị
boolean coKeHoachHieuLuc(long nctId);                                      // FR-009
Set<Long> loaiCongViecHienHanh(long nctId);
    // bổ sung theo plan 008: loại công việc trong phiên bản kế hoạch Hiệu lực (008 FR-031)
```

`LoaiCongViecApi` (đọc; bổ sung theo plan 008):

```java
YeuCauLoaiCongViec yeuCau(long loaiCongViecId);   // vai trò yêu cầu, loại giấy phép/đào tạo yêu cầu (FR-038; 008 FR-028a, FR-030)
```

### 1.3. `CoMatBanTruApi` (003, 007, 010, 011, 012, 014, 016)

```java
Optional<CoMatView> theoNgay(long nctId, LocalDate ngay);           // trạng thái, giờ đến/về dự kiến, giờ về hiện hành, giờ thực tế
Optional<LocalTime> gioVeTheoNgay(long nctId, LocalDate ngay);      // FR-018a: nguồn cho 003, 010, 011
List<CoMatView> theoKhoang(long nctId, LocalDate tu, LocalDate den); // 010 tính phí buổi, 016
void doiGioVe(long nctId, LocalDate ngay, LocalTime gioMoi, long dangKyId);  // MANDATORY; 014 đồng ý về muộn (FR-018a)
void traGioVe(long nctId, LocalDate ngay, long dangKyId);                    // MANDATORY; 014 hủy đăng ký trước giờ bắt đầu
void diemDanhVe(long nctId, LocalDate ngay, long banGhiDonId, LoaiVe loai,
                Instant thoiDiem, NguoiDungHienTai nd);                      // MANDATORY; 012 khi ghi bàn giao "Viện đưa về" (Q-295)
```

### 1.4. `CongViecCaApi`: tác động kèm lệnh của 008, 015 (mọi hàm MANDATORY)

```java
void khiVangCa(long nhanVienId, long caId);                       // FR-032, BR-M04-13, Q-91
void khiNghiCoDuyet(long nhanVienId, Set<Long> caIds);            // FR-032 (015 FR-029)
void khiNghiViec(long nhanVienId, Instant tuThoiDiem);            // FR-032, BR-M09-04
void khiDoiPhanCong(long nctId, long caId);                       // FR-024, BR-M04-28
void khiKetThucPhanCong(long phanCongId, long caId);              // FR-032 (008 FR-027; 015 FR-024 b)
KetQuaXacNhan khiXacNhanBanGiao(long banGiaoId, long caTruocId, long caSauId,
                                Set<Long> congViecIdsTrongBanGiao, Instant thoiDiem);
    // FR-047, FR-047a: công việc Thường đang Quá hạn, có trong bàn giao, vẫn là công việc chung chưa ai nhận
    // (hoặc người thực hiện thuộc ca trước) → Không thực hiện "hệ thống đóng sau bàn giao";
    // công việc chưa đóng khác → giao theo phân công ca sau hoặc chung, dấu TON_TU_CA_TRUOC; trả danh sách đã đóng, đã chuyển
```

Mỗi hàm phát `CongViecThanhChung` (Trung bình, Trưởng tầng) khi có công việc thành chung, và `CongViecChungKhongAiDuDieuKien` khi ca không có ai đủ điều kiện.

### 1.5. `CongViecBanGiaoApi` (008; FR-046, BR-M04-07, BR-M09-06)

```java
List<CongViecChoBanGiaoView> chuaDong(long tangKhuVucId, long caId);
    // Đến hạn, Quá hạn và Chưa đến hạn có thời điểm dự kiến trong ca; người cao tuổi, loại, thời điểm, trạng thái,
    // mức, người thực hiện, số lần đã nhắc, cảnh báo liên quan. Không có công việc của người đã ở trạng thái cuối (FR-010)
Map<Long, TrangThaiCongViecView> trangThaiHienHanh(Collection<Long> congViecIds);   // mục bàn giao hiển thị trạng thái hiện hành
void daGhiVaoBanGiao(long banGiaoId, Collection<Long> congViecIds);                  // MANDATORY; ghi lịch sử GHI_VAO_BAN_GIAO
```

Không có hàm lập sẵn mục bàn giao (Q-307).

### 1.6. `NguonCongViecApi` (007, 011, 014; MANDATORY)

```java
void nguonBiHuy(LoaiCanCu loai, long canCuId, LyDo lyDo);   // FR-022: hủy công việc Chưa đến hạn có căn cứ đó (bữa hủy, bữa bỏ vì vắng, đăng ký hủy, lịch đo ngừng)
void nguonDaDoi(LoaiCanCu loai, long canCuId);              // FR-022: chỉ báo Người phụ trách ca, Trưởng tầng; không hủy
```

### 1.7. `CongViecVangMatApi` (014; MANDATORY)

```java
void tinhLai(long nctId, Optional<KhoangThoiGian> khoangCu, Optional<KhoangThoiGian> khoangMoi);  // FR-020a
```

### 1.8. `CongViecDoChiSoApi` (007; MANDATORY)

```java
void daXacNhan(long congViecId, long banGhiChiSoId, Instant thoiDiemDo, long nguoiDoId);  // FR-034 "đo lại để xác nhận"
```

### 1.9. `CongViecThongKeApi` (016; đọc, FR-050 dòng "Gửi 016")

```java
Stream<CongViecBaoCaoView> theoKhoang(PhamViLoc phamVi, Instant tu, Instant den);
    // trạng thái và lịch sử trạng thái, mức, loại, tầng/khu, ca, người thực hiện, người đóng và quy tắc đóng (FR-028),
    // thời điểm dự kiến, thời điểm thực hiện (gồm thời điểm thiết bị), nhãn ghi muộn, nhãn đồng bộ, từng Quá hạn
Stream<CoMatView> coMatTheoKhoang(PhamViLoc phamVi, LocalDate tu, LocalDate den);
Stream<KetQuaView> ketQuaTheoKhoang(long nctId, Set<LoaiKetQua> loai, Instant tu, Instant den);  // 007 FR-039c (ngủ), 014 hồ sơ tinh thần
Set<Long> nguoiDaGhiKetQua(long nctId, Instant tu, Instant den);
    // bổ sung theo plan 007: nhân viên đã ghi kết quả công việc cho người đó trong khoảng, kể cả công việc chung (007 FR-060 c, CHK018)
```

### 1.10. `ThoiKhoaBieuApi` (012 cổng người thân nếu spec 012 cho xem; 016)

```java
List<MucThoiKhoaBieuView> theoNgay(long nctId, LocalDate ngay);   // FR-048
```

### 1.10a. `KetQuaChamSocCongApi` (012; bổ sung theo plan 012, research F-11, F-12)

```java
List<KetQuaNguoiThanView> ketQua(long nctId, Instant tu, Instant den);   // kết quả công việc chăm sóc đã ghi, không có ghi chú nội bộ (012 FR-046 (2))
TongHopAnUongView tongHop(long nctId, LocalDate tu, LocalDate den);      // tỷ lệ ăn TB, lượng nước TB; rỗng khi không có dữ liệu (012 FR-052)
```

012 kiểm quyền xem sức khỏe và ghi nhật ký lượt xem (012 FR-048) trước khi gọi.

### 1.11. Sự kiện 005 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `YeuCauCanhBao(loai, muc, nctId, nguon, khoa, loaiCanCu, canCuId)` (thêm `loaiCanCu`, `canCuId` của công việc nguồn theo plan 007; công việc căn cứ `LAN_DO` được 007 ghi với khóa "bỏ lỡ lần đo theo lịch", 007 FR-039b) | 007 (tạo hoặc gộp theo BR-M05-02) | FR-039, FR-040, FR-042, FR-044 c |
| `YeuCauDongCanhBao(congViecId, lyDo)` | 007 | FR-010, FR-035 |
| `NhacQuaHan(congViecId, nguoiNhan)`; `NhacQuaHanGop(nguoiNhan, congViecIds)` | 009 (Nhẹ) | FR-043, FR-052 b, BR-M04-29 |
| `CongViecQuaHanCanXuLy(congViecId, muc, nguoiNhan)` | 009 (Trung bình) | FR-044 a, b |
| `CaChuaCoCongViec(caId, tangKhuVucId)` | 009 (Người phụ trách ca, Trưởng tầng) | FR-017 |
| `CanLapCongViec(nctId, lyDo, tuThoiDiem)` | 009 (Người phụ trách ca của tầng hoặc khu bán trú) | FR-017, FR-018, FR-020, FR-020a |
| `KeHoachDaDoi(nctId, phienBanMoi, phienBanCu)`; `NguonCongViecDaDoi(loai, canCuId, nctId)` | 009 (Người phụ trách ca, Trưởng tầng) | FR-006, FR-022 |
| `PhienBanKeHoachDaGuiDuyet`, `…DaTraLai`, `…DaThuHoi`, `…QuaHanDuyet`, `…BaoQuanLyVien` | 009 | FR-003, FR-005 |
| `CanXemXetKeHoach(nctId, nguon, thamChieu)` | 009 (điều dưỡng phụ trách, người có quyền duyệt) | FR-011 |
| `CongViecThanhChung`, `CongViecChungKhongAiDuDieuKien` | 009 (Trưởng tầng) | FR-032, CHK037 |
| `DaDiemDanhDen(nctId, ngay, gioDen)` | 009 (Người phụ trách ca khu bán trú) | FR-018, SC-009 |
| `VangKhongBao(nctId, ngay)` | 009 (Trưởng tầng) | FR-013 |
| `BanTruConCoMatHetNgay(nctId, ngay)` | 009 (Trung bình Người phụ trách ca đang diễn ra; Nhẹ Trưởng tầng) | FR-019 |
| `TrangThaiCoMatDaXacDinh`, `TrangThaiCoMatDaDoi` (`nctId`, `ngay`, trạng thái, buổi phát sinh) | 010, 011, 004 | FR-016 |
| `KetQuaCoTinhPhi(ketQuaId, nctId, loaiCongViecId, dichVuId, soLuong, thoiDiemThucHien)` | 010 | FR-051, DBR-15 |
| `KetQuaTinhPhiDaDinhChinh(ketQuaId, kieu, soLuongMoi)` | 010 | FR-037, DBR-17 |
| `AnKemKeoDai(nctId, buaIds, thoiDiem)` | 011 | FR-040 |
| `CongViecDoBiBo(congViecId, nctId, lichDoId, chiSoIds, lyDo, mucQuanTrong)` (bổ sung theo plan 007): công việc căn cứ `LAN_DO` đóng Không thực hiện (không tính đóng do trạng thái cuối) hoặc còn Quá hạn khi hết ca; `lyDo` là `KHONG_THUC_HIEN` hoặc `QUA_HAN_HET_CA` | 007 (quy tắc bỏ lỡ lần đo, 007 FR-039b, Q-75) | FR-026, FR-044 |
| `KetQuaNguDaGhi(ketQuaId, nctId, ngay, nhomGiaTri)` (bổ sung theo plan 007): khi ghi hoặc đính chính kết quả ngủ; `ngay` là ngày dương lịch của thời điểm dự kiến | 007 (mất ngủ kéo dài, 007 FR-039c) | FR-034, FR-037 |

Mỗi sự kiện mang khóa `loại + id + mốc` để bên nghe bỏ trùng (NFR-04).

---

## 2. 005 cần từ module khác

### 2.1. SPI 005 khai báo ở `m04.api`, module khác cài

**Triển khai T002, 2026-10-26**: chữ ký logic bên dưới được giữ nguyên số phương thức/đối số và ánh xạ sang tiếng Anh trong `m04.api.spi` theo glossary mục 11. Đưa phần khai báo tám SPI lên T002 để fake có interface thật; API khác của T003 vẫn chưa cài. Bản giả duy nhất do 005 sở hữu là `support/fake/m04/FakeM04Config`, import tường minh ở test/demo.

Các view chỉ phục vụ SPI được đặt lồng trong interface. Những view thiết kế chưa liệt kê trường được cụ thể hóa thành projection tối thiểu sau (không thay API dữ liệu của module nguồn):

| SPI | View/kiểu trao đổi khi triển khai |
| --- | --- |
| `ShiftStaffPort` | `ShiftView(id, floorAreaId, startsAt, endsAt, published)`; `CareAssignmentView(staffId, role, primary)`; `HandoverTasksView(receiveAtStart, prepareAtEnd)` là hai việc checklist điều dưỡng theo FR-029 |
| `TaskSourcePort` | `SourceType` tương ứng đủ 12 giá trị LoaiCanCu; `SourceValidation(valid, residentMatches, timeMatches, prescribed)` theo ghi chú kiểm căn cứ và FR-023 |
| `VitalSignPort` | `RecordingResult(status, vitalSignRecordId, reason, remeasurementSessionId)` với status `SAVED`, `BLOCKED`, `AWAITING_REMEASUREMENT`; các id/lý do dùng Optional |
| `MedicationChecklistPort` | `DoseView(doseKey, residentId, prescriptionId, plannedAt, windowFrom, windowTo, status, sharedOnFloor, awaitingHandoverConfirmation, awaitingRecordingAfterReturn, onlineRequired, medicationSource, deliveryId, medication)` giữ trường của 006 mục 1.3; `MedicationSummary(description)` là projection mô tả, không tự khai danh mục thuốc; status/medicationSource là mã do 006 cung cấp |
| `AlertReadPort` | `AlertView(id, residentId)` là định danh/tham chiếu cảnh báo đang mở; không khẳng định đã cung cấp toàn bộ nội dung cảnh báo của 007 |
| `DayCarePickupPort` | `PickupView(id, pickupType)`; nguồn 012 chịu trách nhiệm xác minh điều kiện hợp lệ, `pickupType` là mã nguồn; lỗi dùng lại `BAN_GHI_DON_DA_DUNG` tương ứng `PICKUP_RECORD_ALREADY_USED` |
| `QualityCheckPort` | `FailedCheckView(id, shiftId)`; cặp kết quả/công việc gốc nằm trong đối số truy vấn |
| `TimetableSourcePort` | `TimetableItemView(at, source, description, status)` theo schema MucThoiKhoaBieu; source/status giữ mã nguồn |

`MANDATORY` của `VitalSignPort.record` và `DayCarePickupPort.markUsed` là trách nhiệm provider thật. Fixture bộ nhớ không thay thế kiểm rollback/MySQL hoặc tính điều kiện nghiệp vụ nguồn.

```java
interface NhanSuCaPort {                    // 008 cài
  Optional<CaView> ca(long caId);                                         // tầng/khu, giờ bắt đầu, kết thúc, đã công bố
  List<CaView> caDaCongBo(long tangKhuVucId, Instant tu, Instant den);    // FR-017: Trưởng tầng lập trước
  Optional<CaView> caChua(long tangKhuVucId, Instant thoiDiem);           // ca chứa thời điểm dự kiến
  Optional<Long> nguoiPhuTrachCa(long caId);
  List<PhanCongChamSocView> phanCong(long nctId, long caId);              // nhân viên, vai trò, chính/hỗ trợ (FR-024)
  List<Long> dieuDuongPhuTrach(long nctId, Instant thoiDiem);             // FR-011, FR-040
  boolean coTenTrongCa(long nhanVienId, Instant thoiDiem);                // nhận việc (FR-031), FR-047a
  boolean duDieuKien(long nhanVienId, Set<String> chungChi, LocalDate ngay);  // BR-M09-01
  boolean coNguoiDuDieuKien(long caId, VaiTro vaiTro, Set<String> chungChi);   // CHK037
  boolean banGiaoDaXacNhan(long caTruocId);                               // FR-047a
  ViecBanGiaoView viecBanGiao(long nhanVienId, long caId);                // checklist điều dưỡng (FR-029)
}

interface NguonCongViecPort {               // 007 (LAN_DO, THEO_DOI_SAU_NGA, THEO_DOI_TIEP_XUC, CANH_BAO_UONG_NUOC),
                                            // 011 (BUA), 014 (BUOI_HOAT_DONG, LAM_LAI) mỗi bên một bean
  Set<LoaiCanCu> loaiQuanLy();
  KetQuaKiemTraCanCu kiemTra(LoaiCanCu loai, long canCuId, long nctId, Instant thoiDiemDuKien);
      // hợp lệ, người cao tuổi khớp, thời điểm khớp; với LAN_DO trả "theo chỉ định" để buộc mức Bắt buộc (FR-023)
  default boolean xacNhanPhucVu(long buaId, long nctId) { return true; }   // 011: FR-050 (suất đặc biệt, cần đối chiếu)
}

interface ChiSoPort {                       // 007 cài; MANDATORY
  KetQuaGhiChiSo ghi(long nctId, String loaiChiSo, BigDecimal giaTri, Instant thoiDiemDo,
                     long nguoiDoId, long congViecId, Optional<Instant> thoiDiemThietBi);
      // DA_LUU(banGhiChiSoId) | BI_CHAN(lyDo, CFG-M06-03) | CHO_DO_LAI(phienId) (FR-034, FR-041, BR-M04-10)
}

interface LieuThuocChecklistPort {          // 006 cài; chỉ đọc
  List<LieuHienThiView> theoCa(Collection<Long> nctIds, Instant tu, Instant den);   // FR-029
}

interface CanhBaoDocPort {                  // 007 cài; chỉ đọc
  List<CanhBaoView> dangMo(Collection<Long> nctIds);             // checklist điều dưỡng (FR-029); bàn giao (FR-046)
  Map<Long, List<Long>> theoCongViec(Collection<Long> congViecIds);
}

interface BanGhiDonBanTruPort {             // 012 cài
  Optional<BanGhiDonView> hopLe(long banGhiDonId, long nctId, Instant thoiDiem);
      // người đón hợp lệ hoặc ngoại lệ đón Hiệu lực, hoặc "Tự về" khi dấu "được tự về" có tác dụng (Q-125); loại đón
  void daDung(long banGhiDonId, long diemDanhId);
      // bổ sung theo plan 012: MANDATORY, trong lệnh Điểm danh về; lần hai → BAN_GHI_DON_DA_DUNG
}

interface KetQuaKiemTraPort {               // 014 cài
  Optional<KetQuaKiemTraView> khongDat(long ketQuaKiemTraId, long congViecGocId);   // FR-026a: kèm ca của danh sách kiểm tra
}

interface ThoiKhoaBieuNguonPort {           // 006, 012, 014 mỗi bên một bean; chỉ đọc
  List<MucThoiKhoaBieuView> theoNgay(long nctId, LocalDate ngay);   // FR-048: thời điểm, nguồn, trạng thái
}
```

Bean `NguonCongViecPort` thiếu cho một loại căn cứ thì lệnh lập công việc với căn cứ đó bị chặn `CAN_CU_KHONG_HOP_LE` (mặc định an toàn).

### 2.2. Sự kiện 005 nghe

| Sự kiện | Module | Xử lý | FR |
| --- | --- | --- | --- |
| `DanhGiaDaDoiMucHoacNhuCau` | 001 | `CanXemXetKeHoach` | FR-011, Q-296 |
| `DaChuyenGiuong` | 003 | Gán lại công việc chưa đóng theo phân công khu mới (idempotent theo `phanBoMoiId`) | FR-032 |
| `PhuLucDaApDung` | 004 | Có `MUC_CHAM_SOC`: `CanXemXetKeHoach`; có `LICH_DEN`, `LOAI_LUU_TRU`: cập nhật dòng có mặt từ ngày hiệu lực | FR-011, FR-012 |
| `BaoVangBanTruDaGhi`, `NgayNghiBanTruDaDoi` | 004 | Bảng FR-013 | FR-013, FR-014 |
| `SuCoNgaDaGhiNhan(nctId, suCoId)`; `LuaChonChamSocGiamNheDaGhi(nctId)`; `LichDoDaDoi(lichDoId, nctId)` | 007 (tên đề xuất; plan 007 giữ hoặc ghi thay đổi) | `CanXemXetKeHoach`; `NguonCongViecDaDoi` | FR-011, FR-022 |
| `ThamSoDaDoi(CFG-M04-*)` | 000 | Xóa bộ đệm tham số; không tính lại kết quả cũ | Edge Case |

### 2.3. Giao diện có sẵn

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `theoTrangThai`; `DanhGiaApi.luuYHienHanh`; `TrangThaiNguoiCaoTuoiApi.khoangTrangThai` | M01 (001) | FR-017, FR-025, FR-030, FR-039 | Có sẵn |
| `NguonYKienApi.kiemTra` (001 mục 1.8b); `QuanHeNguoiThanApi.thuocNguoiCaoTuoi` (012) | M01 (001), M10 (012) | FR-002a: nguồn ý kiến, người đại diện của người bệnh (bổ sung 2026-10-18) | Có sẵn sau 001; bản giả cho 012 |
| `ViTriApi.hienTai`, `CoCauViTriApi` | M03 (003) | Thẻ người cao tuổi, tầng của người (FR-030) | Có sẵn |
| `HopDongDocApi.lichDenTaiNgay`, `noiDungTaiNgay` (loại lưu trú) | M02 (004) | FR-012 | Có sẵn |
| `QuyenApi`, `PhamViApi` (`boLoc`, `nguoiPhuTrachCa`, `truongTangDuocGiao`), `NguoiDuyetApi`, `NguoiDungHienTai` | M15 (002) | Research C-16 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `NhacQuaHan`, `common` | M15 (000) | Mọi lệnh | Có sẵn |
| Bản cài các SPI mục 2.1 | 006, 007, 008, 011, 012, 014 | | Bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}` |
