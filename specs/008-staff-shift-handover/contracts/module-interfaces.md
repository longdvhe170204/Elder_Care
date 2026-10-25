# Giao diện giữa module: Feature 008

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `m09.api` và bản cài các SPI do module thấp hơn khai báo (research N-01, N-02). Module khác chỉ dùng các giao diện này (constitution X). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong`, `VaiTro` theo 000, 001, 002. Mọi hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi; hàm đọc không khóa.

Plan làm sau (009, 011, 013, 014, 015, 016) giữ đúng chữ ký hoặc ghi rõ thay đổi.

**Triển khai Phase 3, 2026-10-24**: `ShiftApi` có bản thật ở `m09.duty.internal.service.ShiftQueries`; bản giả chung vẫn là `support/fake/m09/FakeShifts`. Các SPI thấp hơn chưa có trên nhánh nên người dùng cho phép khai báo phần đọc ca tối thiểu: `m03.api.spi.LocationReferences`, `m04.api.spi.CareShiftPort`, `m07.api.spi.NursingShiftPort`, `m05.api.spi.AlertStaffPort`, cùng bản giả duy nhất tại `support/fake/m03`, `m04`, `m07`, `m05`. Chữ ký Java trong các interface và bảng dịch glossary mục 14.8 là phần đã cài; không phải toàn bộ SPI phân công/bàn giao trong bảng thiết kế bên dưới. Hàm phân công, giao Trưởng tầng nối ở T019–T021; trạng thái bàn giao/ca sau nối T026–T028. Không cài bản trả rỗng để giả là các phần này đã hoàn thành. C/D review trước khi gộp (team-plan mục 8).

Truy vấn ca tại tầng dựa trên thời gian `[bắt đầu, kết thúc)` và lịch đã công bố, không chờ job cập nhật nhãn trạng thái; thành viên bị trừ từ mốc vắng/rút/rời ca và nghỉ đã duyệt. Nếu nhiều ca đang có hiệu lực cùng tầng tại cùng thời điểm, báo lỗi dữ liệu nguồn qua `InvalidPermissionDataException`, không chọn tùy ý một ca. Vai trò không xếp ca dùng `CFG_M13_06` qua `ParameterApi`.

---



| SPI (gói khai báo) | Bản cài của 008 | Dùng bởi |
| --- | --- | --- |
| `HoSoNhanVienApi` (`m15.taikhoan.api`; khai báo dạng SPI theo plan này, chữ ký giữ đúng 002) | `trangThai(nhanVienId, ngay)`, `chucDanhCoVaiTro(nhanVienId)`; `taoHoSoToiThieu(...)` cho lệnh khởi tạo Quản lý viện đầu tiên (002 A-03) | 002 FR-004, FR-004a, FR-011 |
| `GiayPhepApi` (`m15.taikhoan.api`) | `hieuLuc(nhanVienId, loai, ngay)` theo `HieuLucChungChi` (tới hết ngày hết hạn); `sapHetHan(trongVong)` cho cảnh báo giấy phép hành nghề của 002 (CFG-M06-02) | 002 FR-037, FR-038, FR-039 |
| `DanhMucPhamViHanhNgheApi` (`m15.taikhoan.api`) | Danh mục `pham_vi_hanh_nghe` | 002 FR-037, 007 FR-070 |
| `PhanCongApi` (`m15.taikhoan.api`) | `dangHieuLuc(nhanVienId, t)`, `lichSu(nhanVienId, tu, den)` (FR-037a; gồm giao trưởng tầng, thành viên ca, người phụ trách ca) | 002 FR-028 → FR-035a |
| `NhiemVuApi` (`m15.taikhoan.api`) | Mục 1.2 | 002 FR-019a → f, FR-031 |
| `ThamChieuViTri` (`m03.api`) | Phân công Nháp, Tương lai, Hiệu lực; giao trưởng tầng đang/sẽ hiệu lực; ca chưa đóng hoặc hủy của vị trí; `daTungThamChieu` cho quyết định xóa | 003 FR-005 |
| `NhanSuCaPort` (`m04.api`) | Giữ đúng chữ ký 005 mục 2.1; `phanCong` theo `XacDinhNhanVienChinh`; `banGiaoDaXacNhan(caTruocId)` | 005 FR-017, FR-024, FR-031, FR-047a |
| `DieuDuongCaPort` (`m07.api`) | Giữ đúng chữ ký 006 mục 2.1 | 006 FR-026a, FR-028 |
| `NhanSuCanhBaoPort` (`m05.api`) | Giữ đúng chữ ký 007 mục 2.1; `bacSiTruc(t)` theo FR-023 | 007 FR-031, FR-033, FR-037, FR-047b, FR-060 (c) |
| `DinhChinhDuocProvider` (000) | `M09.BAN_GIAO` (research N-10) | 000 R-08 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 9, 10 | |
| `GanPhamViProvider` (002) | `M09.LICH_CA`, `M09.CA`, `M09.PHAN_CONG`, `M09.BAN_GIAO`: tầng/khu của ca; `M09.NHAN_VIEN`, `M09.CHUNG_CHI`: nhân viên | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M09.SCAN_CHUNG_CHI`: chỉ Quản lý viện và chính nhân viên (nhóm hạn chế, FR-052) | NFR-08 |

### 1.2. `NhiemVuApi` (bản cài của 008)

```java
Optional<Long> nguoiPhuTrachCa(long tangKhuVucId, Instant t);       // từ nguoi_phu_trach_ca; rỗng khi ca thiếu người phụ trách
Optional<Long> truongTangDuocGiao(long tangKhuVucId, Instant t);    // giao_truong_tang hiệu lực (kể cả giao tạm)
Optional<Long> nguoiThayTruongTang(long tangKhuVucId, Instant t);   // Q-90: Người phụ trách ca đang diễn ra khi tầng chưa có Trưởng tầng
boolean caKhongCoTruongTang(long caId);                              // 002 FR-019e (014 FR-066, Q-262)
Optional<Long> phuTrachBoPhan(long boPhanId);  boolean laPhuTrachBoPhan(long nhanVienId);
Optional<Long> boPhanCua(long nhanVienId, Instant t);               // DBR-36; 019
Optional<Long> truongDoan(long chuyenId);  List<Long> nguoiDiCung(long chuyenId);   // ủy cho ChuyenDiNhiemVuPort (014)
List<Long> nguoiNhanTaiTang(long tangKhuVucId, long caId);          // 011 UC-76: TT, ĐD, CS có phân công trong tầng và ca
```

### 1.3. `m09.api.CaTrucApi` (đọc; 009, 011, 013, 015, 016)

```java
Optional<CaView> caDangDienRa(long tangKhuVucId, Instant t);
List<CaView> caCuaNhanVien(long nhanVienId, Instant tu, Instant den);
boolean dangTrongCa(long nhanVienId, Instant t);                    // 009: nhân viên "trong ca" để chọn kênh (BR-M13-01); cùng quy tắc với NhanSuCanhBaoPort.dangTrongCa (007, bổ sung 2026-10-22)
Optional<NhanVienView> nhanVien(long nhanVienId);                   // nhóm (a) công khai
```

### 1.4. `m09.api.NhanSuThongKeApi` (016; FR-037, FR-037a, FR-049 dòng 016)

```java
Stream<LanTinhTyLeView> tyLePhucVu(PhamViLoc phamVi, LocalDate tu, LocalDate den);   // lần tính cuối trước khi ca kết thúc, dấu từng không đạt (Q-200)
Stream<BanGiaoBaoCaoView> banGiao(PhamViLoc phamVi, LocalDate tu, LocalDate den);     // đúng hạn / trễ, trạng thái, thời điểm lập, xác nhận
Stream<ChungChiSapHetHanView> chungChiSapHetHan(int trongVongNgay);
Stream<CanCuNhiemVuView> canCuKhacVaiTro(PhamViLoc phamVi, Instant tu, Instant den);  // FR-056
Stream<LichSuPhanCongView> lichSuPhanCong(PhamViLoc phamVi, Instant tu, Instant den); // FR-037a
```

### 1.5. Port nội bộ M09 cho 015 (gói `m09.lichca.port`, `m09.phancong.port`)

Theo research N-07 (`CaLenhPort`, `PhanCongLenhPort`, `KiemTraCongBoMoRong`). Chỉ `m09.doica`, `m09.xoayca` được dùng (ArchUnit). Khi 015 chưa có, `KiemTraCongBoMoRong` không có bean và lệnh công bố bỏ qua phần của 015.

Bổ sung theo plan 015 (hợp đồng 015 mục 1.2, 1.4): `LichNhapLenhPort` (008 cài: `taoNhap`, `thayNoiDungNhap`, `goNhanVienKhoiNhap`, `lichThang`; MANDATORY) để lệnh "Lập lịch từ mẫu xoay ca" và nghỉ theo ngày ghi lịch Nháp; `NghiCoDuyetPort` (015 cài, 008 dùng khi sửa tay lịch Nháp và khi công bố, 008 FR-012, FR-016); sự kiện nội bộ M09 008 phát thêm: `LichNhapDaLuu(lichCaId)`, `GiayPhepDaDoiHieuLuc(nhanVienId, giayPhepId)`, `DaoTaoDaDoiHieuLuc(nhanVienId, daoTaoId)` để 015 tính lại phủ (015 FR-007).

### 1.6. Sự kiện 008 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `LichCaDaCongBo(lichId, nhanVienIds)` | 009 (Nhẹ, mỗi người chỉ ca của mình), 005, 006, 016 | FR-016, FR-054 |
| `CaDaThayDoi(caId, loai, nhanVienIds)` (bổ sung, chuyển người phụ trách, hủy) | 009 (Nhẹ) | FR-017 → FR-021 |
| `CaDaBatDau(caId, phamVi)`; `CaDaKetThuc(caId, phamVi, trangThai)` | 005, 006, 007, 013 (khóa bỏ trùng, xác định "người giữ vắng mặt") | FR-022, 013 FR-014 |
| `NhanVienVangCa(caId, nhanVienId)`; `NhanVienNghiViec(nhanVienId, hieuLuc)` | 009 (Trung bình, Trưởng tầng; không nêu lý do), 016 | FR-020, FR-004, FR-053 |
| `CaThieuNguoiPhuTrach(caId)`; `TangChuaCoTruongTang(tangId)` | 009 (Trung bình: Trưởng tầng, Quản lý viện) | FR-004, FR-019, FR-024 |
| `PhanCongDaDoi(caId, nhanVienId)`; `PhanCongKetThucDoChungChi(nhanVienId, caIds, nctIds)` | 005, 006 (gán lại); 009 (Trung bình, Trưởng tầng) | FR-009, FR-027 |
| `ChungChiSapHetHan(chungChiId, nhanVienId, loai, ngayHetHan)`; `CanhBaoChungChiDaDong(chungChiId)` | 009 (Nhẹ: Quản lý viện, nhân viên, Trưởng tầng có ca liên quan) | FR-008 |
| `CaDuoiNguongPhucVu(caId, tyLe, nguong)` | 009 (Trung bình: Trưởng tầng; Quản lý viện khi trong CFG-M09-08) | FR-036 |
| `NctChuaCoNguoiPhuTrach(caId, nctIds)` | 009 (Trung bình: Trưởng tầng, Người phụ trách ca) | FR-029 |
| `BanGiaoChuaLap(caId)`; `BanGiaoChuaHoanTatKhiHetCa(banGiaoId)`; `BanGiaoChuaXacNhan(banGiaoId)`; `BanGiaoCoYKien(banGiaoId)`; `BanGiaoDaLap(banGiaoId)`; `BanGiaoDangDoDaChuyen(banGiaoId, nguoiMoi)`; `BanGiaoDaDinhChinh(banGiaoId)` | 009 (Trung bình; đính chính: Nhẹ) | FR-039, FR-042, FR-043, FR-019, FR-057 |
| `BanGiaoDaXacNhan(banGiaoId, caTruocId, caSauId)` | 016 (đúng hạn/trễ); 005, 006, 007 đã xử lý trong transaction (không cần nghe) | FR-044 |
| `BoPhanChuaCoPhuTrach(boPhanId)` | 009 (Trung bình, Quản lý viện) | FR-001a |

Mỗi sự kiện mang khóa `loại + id + mốc` (NFR-04), mức, nhóm người nhận, loại thông tin theo research N-14.

---

## 2. 008 cần từ module khác

### 2.1. SPI 008 khai báo ở `m09.api`, module khác cài

```java
interface DoGuiBanGiaoPort {                       // 013 cài; chỉ đọc
  List<DoGuiDangGiuView> dangGiu(Collection<Long> nhanVienIds);   // FR-039 (g): đồ có giá trị Đang giữ, người giữ, người cao tuổi
  Map<Long, Boolean> conDangGiu(Collection<Long> doGuiIds, Collection<Long> nhanVienIds); // FR-040 (g): "đã xử lý xong"
}

interface ChuyenDiNhiemVuPort {                    // 014 cài; chỉ đọc
  Optional<Long> truongDoan(long chuyenId);  List<Long> nguoiDiCung(long chuyenId);   // 002 FR-019 qua NhiemVuApi
  List<ChuyenTrongCaView> chuyenTrongCa(long tangKhuVucId, Instant tu, Instant den);
      // FR-039 (e): người đang đi chuyến, chuyến quá giờ về dự kiến (014 FR-049)
}
```

Bean thiếu thì nhóm (g), phần chuyến đi của nhóm (e) trả "chưa có dữ liệu"; `truongDoan`, `nguoiDiCung` trả rỗng (002 không cấp quyền theo nhiệm vụ này: mặc định an toàn).

**Tên triển khai T002 (2026-10-23)**: chữ ký trên là tên logic; mã theo `backend/README.md` và glossary mục 14.6:

| Tên logic | Khai báo tiếng Anh |
| --- | --- |
| `DoGuiBanGiaoPort.dangGiu`, `.conDangGiu` | `m09.api.spi.HeldBelongingsHandoverPort.heldBy`, `.stillHeld` |
| `ChuyenDiNhiemVuPort.truongDoan`, `.nguoiDiCung`, `.chuyenTrongCa` | `m09.api.spi.TripDutyPort.groupLeader`, `.companions`, `.inShift` |
| `DoGuiDangGiuView` | `HeldBelongingView(belongingId, holderStaffId, residentId, storageLocationId)`; vị trí lưu giữ có thể rỗng theo data-model 013 |
| `ChuyenTrongCaView` | `TripInShiftView(tripId, staffIds, expectedReturnAt)` |

Hai kiểu chỉ SPI dùng nằm cùng gói `api.spi`. Bản giả dùng chung ở `support/fake/m09/`: test `@Import(FakeM09Config.class)` rồi tiêm bản giả để đặt dữ liệu. `available(false)` mô phỏng nguồn không có dữ liệu bằng lỗi, tách biệt với kết quả rỗng khi nguồn hoạt động; cách xử lý khi thiếu bean vẫn theo đoạn trên. SPI nội bộ `KiemTraCongBoMoRong` ở mục 1.5 có tên triển khai `m09.shift.internal.service.port.ExtendedPublicationCheck.check(scheduleId)`; cảnh báo `PublicationWarning(code, description, details)` dùng tên trường của schema `Warning`.

### 2.2. Sự kiện 008 nghe

| Sự kiện | Module | Xử lý | FR |
| --- | --- | --- | --- |
| `MucChamSocDaDoi(nctId, …, hieuLucTu)` | 001 | Tính lại tỷ lệ các ca chưa đóng từ `hieuLucTu` của tầng người đó | FR-035 |
| `NguoiCaoTuoiDaChuyenTrangThai(nctId, tu, den, lenh, …)` | 001 | Vắng mặt, trở về: tính lại tỷ lệ; trạng thái cuối: kết thúc phân công theo người đó, tính lại | FR-035, Edge Case |
| `PhanBoDaBatDau`, `PhanBoDaDong`, `DaChuyenGiuong` | 003 | Tính lại tỷ lệ các ca chưa đóng của tầng cũ và mới | FR-035 |
| `ThamSoDaDoi(CFG-M09-*)` | 000 | Xóa bộ đệm; không tính lại kết quả cũ | |

### 2.3. Giao diện có sẵn hoặc đã bổ sung

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `TaiKhoanApi.khoaDoNghiViec` (MANDATORY), `theoNhanVien`, `coVaiTro`; `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi`, `NguoiDungHienTai` | M15 (002) | FR-002, FR-004, FR-001a, N-12 | Có sẵn |
| `NghiepVuChuyenMonApi.loaiYeuCau(maNghiepVu)` (bổ sung theo plan này: loại giấy phép/đào tạo của nghiệp vụ "phát thuốc và xác nhận liều") | M15 (002) | FR-030 (Điều dưỡng phụ trách) | Bản giả |
| `MucChamSocApi.hienHanh`, `taiNgay`; `TrangThaiNguoiCaoTuoiApi.khoangTrangThai`; `NguoiCaoTuoiApi.trangThai` | M01 (001) | FR-034, FR-039 (e) | Có sẵn |
| `ViTriApi`, `LichSuViTriApi.theoViTriTrongNgay`, `CoCauViTriApi`, `CanhBaoBanGiaoApi.theoTang`; `TinhTrangGiuongApi.choSanSangQuaHan(tangId, t)` (bổ sung theo plan này) | M03 (003) | FR-026, FR-034, FR-039 (d), (e), (f) | Hàm cuối: bản giả |
| `LuotVangApi.dangMo` (`m01.api`, 004 cài); `HopDongDocApi.lichDenTaiNgay` | M02 (004) | FR-039 (e), FR-034 | Có sẵn |
| `CongViecCaApi` (MANDATORY), `CongViecBanGiaoApi`; `LoaiCongViecApi.yeuCau(loaiCongViecId)` và `KeHoachChamSocApi.loaiCongViecHienHanh(nctId)` (bổ sung theo plan này) | M04 (005) | FR-004, FR-020, FR-027, FR-030, FR-031, FR-039 (a), FR-044 | Hai hàm cuối: bản giả |
| `LieuBanGiaoApi` | M07 (006) | FR-039 (b), FR-040 | Có sẵn |
| `CanhBaoBanGiaoApi` (`dangMo`, `khiXacNhanBanGiao` MANDATORY), `ChiSoApi.vuotNguongTrongKhoang` | M05, M06 (007) | FR-039 (c), (d), FR-044 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `TepApi`, `common` | M15 (000) | Mọi lệnh | Có sẵn |
| Bản cài các SPI mục 2.1; `KiemTraCongBoMoRong` (015) | 013, 014, 015 | | Bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}` |

### 2.4. Ghi chú nối Phase 4 (2026-10-24)

- `AssignmentApi`/`DutyApi` của 002 được M09 cài bằng `AssignmentQueries`/`DutyDirectory`; phần thay Phụ trách bộ phận tham gia transaction của lệnh gọi bằng `MANDATORY`. Các SPI đọc phân công của 005/006/007 được nối cùng bảng phân công; tên tiếng Anh ở glossary 14.9.
- 001 đã có trên `origin/develop` (`b049b1f`, PR #23 tại `dc0c452`). 008 dùng đúng `m01.api.ResidentApi.isFinalStatus(long)` và `m01.api.events.ResidentStatusChanged`, với enum `ResidentStatus`/`StatusCommand` do B khai báo. Listener kết thúc/hủy phân công trực tiếp cùng transaction lệnh nguồn; cập nhật tỷ lệ thuộc T037. API/DTO và bản giả `FakeResidents` được đồng bộ từ B thay cho khai báo tạm, chưa gộp toàn bộ bản cài 001 vào nhánh E.
- Khi test độc lập, dùng duy nhất `support/fake/m01/FakeResidents` qua `FakeM01Config`, đặt trạng thái tường minh bằng `resident`/`changeStatus`. `FakeLocations` và `FakeCareRequirements` là nguồn test cho API tối thiểu 003/005 đã được người dùng duyệt, chờ C review/cài thật. Không có bean giả trong ứng dụng thật; thiếu nguồn kiểm nghiệp vụ thì báo thiếu nguồn, riêng nhắc phân công giữ cờ chờ để thử lại.
- `AssignmentView.scopeEndsAt` là mốc ngừng cấp phạm vi khi kết thúc sớm, được người dùng duyệt; vẫn giữ constructor bảy tham số. Phạm vi không cộng CFG-M15-07 qua mốc này; lịch sử trước mốc vẫn truy được.
- `FileApi` đã có bản cài trong develop sau 001. Nhánh E hiện vẫn dùng interface có sẵn và cần gộp develop có kiểm soát trước khi kiểm scan chứng chỉ với nguồn thật; không sao chép riêng dịch vụ lưu tệp hay tạo thêm fake tệp.


### 2.5. Nối Phase 5 đã được chủ dự án duyệt (2026-10-25)

| Chủ / phần | Chữ ký tiếng Anh đã khai báo/cài | Điều kiện cần giữ khi cài thật |
| --- | --- | --- |
| B — 001 | `ResidentStatusApi.statusPeriods(residentId, statuses, from, to)` | Hợp đồng không đổi. Bản đọc repository đã cài; biên thật, giao khoảng nửa mở; B review. Không sửa lệnh trạng thái. |
| C — 003 | `BedStatusApi.awaitingReadinessOverdue(floorAreaId, at)`, **mới** `currentState(Collection<Long> bedIds, Instant at): Map<Long,String>`; `AllocationHandoverApi.atFloor` | `currentState` trả trạng thái thật kể cả giường không còn quá hạn. Thiếu khóa = chưa đọc được, không suy ra giường đã rời AWAITING_READINESS khi CFG thay đổi. Fake chung `FakeHandoverLocations`. |
| C — 005 | `TaskHandoverApi.unclosed`, `currentStatus`, `recordedInHandover`; `ShiftTaskApi.onHandoverConfirmed(handoverId, previousShiftId, Long nextShiftId, taskIds, at)` | Callback MANDATORY, trả HandoverTransferResult; null ca sau = công việc chung tầng; lọc công việc theo quy tắc 005. Dùng một `FakeCareRequirements` đã có. |
| D — 006 | `DoseHandoverApi.inShift`, `currentStatus`; `NursingShiftPort.handoverConfirmed` | Chỉ đọc nguồn/trạng thái; 008 không chạy lệnh chuyển liều. FakeDoseHandover/FakeNursingShifts dùng chung. |
| D — 007 | `AlertHandoverApi.open`, **mới** `currentStatus(Collection<SourceRef>)`, `onHandoverConfirmed(..., Long nextShiftId, floorAreaId)`; `VitalSignApi.aboveThresholdInRange` | Trạng thái/mức hiện tại kể cả nguồn đã đóng; callback MANDATORY, không bỏ mất lỗi. FakeAlertHandover/FakeVitalSigns dùng chung. |
| C/D — SPI ca | `CareShiftPort.handoverConfirmed`, `NursingShiftPort.handoverConfirmed`, `AlertStaffPort.startedWithoutConfirmedHandover(from,to)` | Đọc trạng thái thật của M09; danh sách chỉ ca sau đã bắt đầu trong khoảng và chưa xác nhận. Bản giả ca có dữ liệu trạng thái/range. |
| C — 014 | `TripInShiftView(..., residentIds, departedAt)` | Giữ constructor ba tham số cũ; bản nguồn thật cần hai giá trị mới để gắn đúng người và thời điểm; thiếu metadata báo nguồn chưa sẵn sàng. Dùng FakeTripDuties đã có. |
| E — sự kiện | `HandoverConfirmed(..., Long nextShiftId, ...)` | nextShiftId=null khi Trưởng tầng nhận bàn giao không tìm được ca sau; không tạo ca thay thế. |

Chỉ cài phần tối thiểu cần cho task; Java là chữ ký cụ thể, bảng tên logic phía trên vẫn là thiết kế tổng thể. Các bổ sung đã được người dùng duyệt trong phiên Phase 5, chờ B/C/D review ở team-plan mục 8. API HTTP mới duy nhất là GET `/shifts/{id}/handovers` đã ghi trong api-008; nhóm có nguồn bắt buộc không mở thêm nhóm/endpoint.


**Sửa phần dùng chung đã được chủ dự án duyệt 2026-10-25**: RecipientGroup/Substitution thêm nhãn `kind` và danh sách loại JSON tường minh để Spring Modulith đọc lại sự kiện thông báo đã lưu; không đổi chữ ký các record, nhóm nhận hay lệnh nghiệp vụ. Không thêm thư viện. Test đọc/ghi 18 trường hợp đã xanh cùng bộ tích hợp có khởi động context mới; A review hai kiểu common (team-plan mục 8). Đây là metadata sự kiện nội bộ, không bổ sung trường vào API HTTP 008.

### 2.6. Nối Phase 6 được chủ dự án duyệt (2026-10-25)

- **C — 005:** ShiftTaskApi.onResignation(long staffId, Instant effectiveAt), theo khiNghiViec có sẵn của hợp đồng 005 mục 1.4. Tham gia transaction lệnh gọi (MANDATORY), chuyển công việc còn mở/tương lai thành chung; dùng FakeCareRequirements chung với hook onResign, không có fake production. Hồ sơ không tài khoản và không có ca/công việc hệ thống không cần nguồn 005; hồ sơ có liên kết mà thiếu nguồn thì từ chối thay vì bỏ qua tác động.
- **A — common web/web client:** chủ dự án duyệt generator FormData/Blob và sửa riêng IdempotencyFilter cho multipart, có test gửi file HTTP thật/chống lặp và hồi quy JSON. A review ở team-plan mục 8; không thêm thư viện hoặc sửa endpoint.
- **A — 009:** 008 phát CredentialExpiryWarningClosed khi gia hạn trong cùng transaction; A nối đóng cảnh báo nguồn cũ. Các thông báo Phase 6 giữ mức FR-058 và chỉ nội dung chung, không chứa lý do nhân sự/số hoặc scan chứng chỉ.
- **HTTP 008:** chủ dự án duyệt bổ sung code trong danh mục GET /practice-scopes để dùng đúng giá trị Credential.practiceScopes; SPI PracticeScopeCatalogApi/PracticeScopeView của 002 giữ nguyên.
