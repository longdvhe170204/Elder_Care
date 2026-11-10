# Giao diện giữa module: Feature 014

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Feature 014 thêm gói con vào module M04 (cùng 005). Giao diện công khai ở `care.api` (bổ sung vào các giao diện 005 đã khai), bản cài các SPI do module khác khai báo, và dịch vụ nội bộ dùng chung với 005 (research L-01, L-02). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong` theo 000, 001, 002. Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 1. 014 cung cấp

### 1.1. SPI khai báo ở module khác, 014 cài

| SPI (gói khai báo) | Bản cài của 014 | Dùng bởi |
| --- | --- | --- |
| `NguonCongViecPort` (`care.api`, 005) | `loaiQuanLy() = {BUOI_HOAT_DONG, LAM_LAI}`; `kiemTra(BUOI_HOAT_DONG, dangKyId, nctId, t)`: đăng ký còn hiệu lực, buổi khớp thời điểm; `kiemTra(LAM_LAI, ketQuaId, …)`: kết quả Không đạt trỏ công việc gốc | 005 FR-017, FR-026a |
| `KetQuaKiemTraPort` (`care.api`, 005) | `khongDat(ketQuaKiemTraId, congViecGocId)` kèm ca của danh sách | 005 FR-026a |
| `ThoiKhoaBieuNguonPort` (`care.api`, 005) | Buổi đã đăng ký, chuyến đi trong ngày | 005 FR-048 |
| `ThamGiaChuyenTrangThai` (`resident.api`) | Bean `@Order` sau 005. Tác động `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN`: hủy đăng ký chưa diễn ra, gỡ khỏi người thường xuyên (FR-022); `CHUYEN_VIEN`, `GHI_NHAN_QUA_DOI` khi đang trong chuyến: người tham gia `ROI_DOAN` (FR-045). Không có điều kiện chặn | 001 H-02 |
| `ChuyenDiThuocPort` (`medication.api`, 006) | `danhGiaDat(nctId, chuyenId)`, `khoangDi(chuyenId)` | 006 FR-048b |
| `DiemDanhTruyVetPort` (`care.api.spi.AttendanceTracePort`, chuyển từ `incident.api` 2026-11-08; 007 đọc) | `cungThamGia(nctId, tu, den)` (`coAttendance`): buổi, thời điểm, người cùng Có mặt (kể cả chuyến). Cài ở `ActivityAttendanceTraceSource` (2026-11-10): buổi trong viện theo kết quả hiện hành sau đính chính, khoảng giờ bắt đầu → kết thúc; chuyến theo điểm danh rời viện chưa Hủy ghi nhận, khoảng giờ rời dự kiến → lúc kết thúc điểm danh về (chưa kết thúc: giờ về dự kiến hiện hành); không gồm chính người được truy vết | 007 FR-060 (b) |
| `ChuyenDiNhiemVuPort` (`staffing.api`, 008) | `truongDoan`, `nguoiDiCung`, `chuyenTrongCa(tang, tu, den)` | 008 FR-039 (e); 002 FR-019 |
| `ChuyenDiBuaAnPort` (`nutrition.api`, 011) | `trungGioBua(gioBua)`: chuyến Đã lên lịch/Đang đi trùng giờ bữa, người tham gia Được đi/Đã rời viện, dự kiến trở lại | 011 FR-034 |
| ~~`HoatDongBanTinPort`~~ | *(Bỏ theo Q-396: không còn bản tin)* | — |
| `HoatDongKenhNctPort` (`family.api`, 012) *(bổ sung 2026-10-18)* | `sapToi(nctId, tu, den)`: buổi của tầng và toàn viện, cờ có tên (L-16) | 012 FR-083 (c) |
| `ChuyenDiSosPort` (`incident.api`, 007) *(bổ sung 2026-10-18)* | `dangDienRa(nctId)`: chuyến, trưởng đoàn, người đi cùng hiện tại (L-16) | 007 FR-047e |
| `NgoaiTuyenHandler` (`admission.api`) | `M04.DIEM_DANH_BUOI`, `M04.DIEM_DANH_ROI_VIEN`, `M04.DIEM_DANH_VE`; lệnh Báo thiếu người, Kết thúc điểm danh về, ghi sự cố thiếu người: `batBuocTrucTuyen()` | Q-01, FR-046a |
| `DinhChinhDuocProvider` (000) | `M04.DIEM_DANH_BUOI` (gắn tầng), `M04.DIEM_DANH_CHUYEN` (Trưởng tầng của chuyến; FR-031a), `M04.KET_QUA_KIEM_TRA` | 000 FR-026 |
| `GanPhamViProvider` (002) | `M04.BUOI`, `M04.DANG_KY`, `M04.CHUYEN_DI`, `M04.SO_THICH`: người cao tuổi / tầng của địa điểm; `M04.KIEM_TRA_CHAT_LUONG`: ca và tầng, chỉ người kiểm tra và Quản lý viện | 002 FR-034 |

### 1.2. Giao diện mới ở `care.api` cho module làm plan sau

```java
interface ChuyenDiNhiemVuLenhApi {           // 015 gọi; MANDATORY
  void goNhiemVu(long nhanVienId, long caId, CanCu canCu);
      // FR-035: đổi ca, nghỉ đột xuất đã duyệt làm người rời ca → gỡ trưởng đoàn / người đi cùng của chuyến chồng ca;
      // báo Trưởng tầng phân công lại; không chuyển cho người nhận ca
}

record ChuyenDiThayDoi(long chuyenId, String loai, boolean xeCuaVien, Instant roiDuKien, Instant veDuKien,
                       Optional<Instant> thoiDiemThucTe, String khoaSuKien) {}
    // loai: LEN_LICH | DOI | HUY | ROI_VIEN_DAU | GIA_HAN | HUY_ROI_VIEN_CUOI | KET_THUC_VE; 016

interface HoatDongThongKeApi {               // 016
  Stream<BuoiThongKeView> buoi(PhamViLoc phamVi, LocalDate tu, LocalDate den);       // FR-070: đăng ký, Có mặt, Vắng theo lý do, Không ghi nhận
  Stream<ChuyenThongKeView> chuyen(PhamViLoc phamVi, LocalDate tu, LocalDate den);   // giờ về dự kiến, gia hạn, về thực tế, thiếu người
  Stream<KetQuaKiemTraView> kiemTra(PhamViLoc phamVi, LocalDate tu, LocalDate den);  // FR-065: vai trò kiểm tra, không thuộc phạm vi
  Stream<CoLapView> coLap();                                                         // FR-058
}
```

Lịch xe thuộc M03 (019): 014 gọi `roombedallocation.api.LichXeApi` (đọc) và `roombedallocation.api.LichXeLenhApi` (MANDATORY) do 019 cung cấp, vì M04 phụ thuộc M03 (sửa theo plan 019; trước đó plan này khai `LichXeChuyenPort` ở `care.api` cho 019 cài, sẽ tạo vòng M03 ↔ M04):

```java
// roombedallocation.api (019 cung cấp)
boolean coLichDaDat(long chuyenId, Instant gioRoi);                         // LichXeApi; FR-040
KetQuaKiemTraDoi kiemTraDoi(long chuyenId, Instant roiMoi, Instant veMoi);  // LichXeApi; Q-239
void theoChuyen(long chuyenId, LoaiThayDoiChuyen loai, Instant roiDuKien, Instant veDuKien, Optional<Instant> thucTe);
    // LichXeLenhApi, MANDATORY trong lệnh của 014: DOI | HUY | ROI_VIEN_DAU | GIA_HAN | HUY_ROI_VIEN_CUOI | KET_THUC_VE (019 FR-018)
```

Khi 019 chưa có (bản giả), chuyến dùng xe của viện bị chặn rời viện "chưa có lịch xe" (mặc định an toàn).

### 1.3. Sự kiện 014 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `incident.api.YeuCauTaoCanhBao` (`THIEU_NGUOI_KHI_VE` Khẩn cấp; `NGUY_CO_CO_LAP` Nhẹ) | 007 | FR-047, FR-055 |
| `incident.api.YeuCauGhiDienBienCanhBao` (bổ sung vào hợp đồng 007 theo plan này) | 007 | FR-047, FR-056 |
| `billing.api.SuKienTinhPhiNguon` / `SuKienTinhPhiNguonDaDoi` (`DIEM_DANH_HOAT_DONG`) | 010 | FR-032, FR-031 |
| `nutrition.api.ChuyenDiDaDoi` | 011 | FR-034 |
| `ChuyenDiThayDoi` | 016 | FR-040 |
| `BuoiDaHuy`, `BuoiDaDoi`, `BuoiDaKetThuc(buoiId)` | 005 nội bộ M04 (công việc "hoạt động") | FR-012, FR-013, FR-014a |
| Thông báo (cài `CoYeuCauThongBao`) | 009 | FR-071 |

---

## 2. 014 cần từ module khác

| Giao diện | Module | Dùng cho | Kiểm thử thay thế |
| --- | --- | --- | --- |
| `TrangThaiNguoiCaoTuoiApi.thucHien(DIEM_DANH_ROI_VIEN / DIEM_DANH_VE / HUY_GHI_NHAN_ROI_VIEN, chiTiet{chuyenId, thoiDiem})` (MANDATORY); `NguoiCaoTuoiApi`; lưu ý chăm sóc của lần đánh giá gần nhất | 001 | FR-018, FR-036, FR-041, FR-044, FR-031a | Bản thật trong test M04 |
| `QuyenApi`, `PhamViApi`, `NhiemVuApi` | 002 | FR-017, FR-027, FR-066, FR-067 | Bản thật |
| Phòng, khu vực, `ViTriApi` | 003 | FR-002a, FR-021 | Bản giả |
| `HopDongDocApi.lichDenTaiNgay`; dịch vụ | 004 | FR-016, FR-002 | Bản giả |
| Nội bộ M04 (005): `CoMatBanTruApi.doiGioVe`, `traGioVe` (MANDATORY); `NguonCongViecApi.nguonBiHuy`; `CongViecVangMatApi.tinhLai`; lập công việc làm lại; công việc của ca; kết quả tâm trạng | 005 | FR-016a, FR-013, FR-041, FR-064, FR-060, FR-053 | Bản thật |
| `MangThuocChuyenDiApi.daChuanBi`, `chuyenThayDoi`, `tinhLaiKhoang` (MANDATORY) | 006 | FR-038, FR-038a, FR-031a | Bản giả |
| `SuCoLenhApi.ghiNhan`, `themDienBien` (MANDATORY); `SuCoApi.dau`, `KhoanhVungApi` | 007 | FR-015, FR-046 → FR-048, FR-021 | Bản giả |
| `CaTrucApi` (ca chồng chuyến, kết ca, ca không có Trưởng tầng) | 008 | FR-035, FR-059a, FR-063, FR-066 | Bản giả |
| `QuyenNguoiThanApi` | 012 | FR-016a, FR-068 | Bản giả |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi` | 000 | Chung | Bản thật |

### 2.1. Sự kiện 014 nghe

| Sự kiện (module) | Xử lý | FR |
| --- | --- | --- |
| `KhoanhVungDaTao` (007) | Hủy buổi có địa điểm trong vùng, đăng ký của người có giường trong vùng; báo trưởng đoàn khi người đi trở về vùng | FR-021 |
| `DauNghiNhiemDaDoi` (007, bổ sung theo plan này) | Gắn: hủy đăng ký nhóm và chuyến, Không đi, báo trưởng đoàn nếu đang đi | FR-021a |
| `CaDaKetThuc` (008) | Mục kiểm tra Quá hạn; ca không danh sách | FR-063 |
| Công việc bị Hủy ghi nhận (005, nội bộ) | Mục kiểm tra Không kiểm tra được | FR-062 |
| `NguoiCaoTuoiDaChuyenTrangThai` (001) | Tạm vắng, Điều trị bệnh viện: hiển thị sẵn "Vắng – đang vắng mặt"; chuỗi cô lập bỏ qua ngày | FR-028, FR-054 |
| `roombedallocation.api.events.VehicleScheduleCorrected` (019, *bổ sung 2026-11-09, chủ dự án chốt*) | Đồng bộ trong lệnh đính chính `M03.VEHICLE_SCHEDULE`: chuyến Đã về đổi giờ rời, về dự kiến hiện hành; giữ tác động đã xảy ra, không phát `TripChanged`, không là đính chính của 014; chuyến không có hoặc không ở Đã về thì lỗi, lệnh 019 rollback; cùng giá trị không ghi | 019 plan "Đính chính tài sản và lịch xe" |

Mọi listener idempotent theo `khoaSuKien` (`su_kien_m04hd_da_xu_ly`). *(R-15: `su_kien_m04hd_da_xu_ly` → `moc_da_nhac`)*

## 3. Bản dịch và chiều phụ thuộc Phase 6 (chủ dự án duyệt 05/11/2026)

Mục 1.1 giữ tên logic để truy vết. Khi triển khai, care không cài trực tiếp SPI trong medication/staffing vì hai module đó đã phụ thuộc care; không tạo vòng module và không sửa API cũ của D/E.

- `care.api.TripMedicationReadApi`: `Optional<TripInterval> approvedTripInterval(long residentId, long tripId)`, `Optional<TripInterval> tripInterval(long tripId)`. `care.api.dto.TripInterval(Instant from, Instant to)`. D cài adapter của `medication.api.spi.TripMedicationPort` gọi API này.
- `care.api.spi.TripMedicationPreparationPort`: `boolean isPrepared(long residentId, long tripId)`; `void onTripChanged(long tripId, long residentId, TripMedicationChangeType changeType)` và `void recalculateInterval(long tripId, long residentId, TripInterval newInterval)` MANDATORY. Enum `care.api.dto.TripMedicationChangeType`: `CANCEL_TRIP`, `NOT_ATTENDING`, `RESCHEDULE`, ánh xạ đúng enum 006. D cài adapter ủy API thuốc hiện có; không đưa fake vào production. Fake C sở hữu: `FakeTripMedicationPreparation` trong `FakeM04Config`.
- `care.api.TripDutyReadApi`: `Optional<Long> groupLeader(long tripId)`, `List<Long> companions(long tripId)`, `List<TripInShiftView> inShift(long floorAreaId, Instant from, Instant to)`. `care.api.dto.TripInShiftView(long tripId, List<Long> staffIds, Instant expectedReturnAt, List<Long> residentIds, Instant departedAt)` giữ năm trường của 008. E cài adapter từ `staffing.api.spi.TripDutyPort` hiện có; không đổi constructor/kiểu cũ của staffing.
- `care.api.spi.TripStaffSchedulePort.hasOverlappingPublishedShift(long staffId, Instant from, Instant to)`: nhân viên còn có mặt trong ca đã công bố, ca giao với khoảng chuyến; không giới hạn vào tầng tổ chức. E cài. Tài khoản Hoạt động/vai trò kiểm qua `AccountApi` có sẵn. Fake C sở hữu: `FakeTripStaffSchedule` trong `FakeM04Config`.
- Lịch xe do 019 cung cấp: tên tiếng Anh ở contracts 019 mục 2.2; care chỉ gọi `roombedallocation.api`, không khai SPI xe trong care.

Thiếu bean nguồn thuốc, ca hoặc lịch xe tại nhánh cần dùng trả `503 CARE_SOURCE_UNAVAILABLE`; chưa có lịch xe Đã đặt trả `409 VEHICLE_SCHEDULE_REQUIRED`. Xe ngoài viện không gọi nguồn lịch xe của viện. Các adapter thật do D/E thực hiện và review trong PR của họ.
## 4. Nghi nhiễm theo khoảng — duyệt 07/11/2026

`care.api.spi.ActivityHealthGatePort.suspectedInfectionDuring(long residentId, Instant from, Instant to)` trả boolean: có dấu nghi nhiễm bất kỳ lúc nào trong khoảng nửa mở `[from, to)`. Ngày nghiệp vụ lấy hai mốc 00:00 tại Asia/Ho_Chi_Minh. C khai và giữ bản giả chung `FakeActivityHealthGate` (hỗ trợ nhiều khoảng, khoảng chưa kết thúc); D/007 cài bean production. Bản giả giữ công tắc nguồn 007, không thay nguồn production.

08/11/2026: chủ dự án không duyệt SPI lịch sử cảnh báo mới. Hai GET sở thích/hồ sơ tinh thần dùng schema đã duyệt; `alerts = null` báo chưa có nguồn lịch sử. T003/T021/T022 còn chuỗi cô lập: JSON M04 của `resident_module_state` đang bị khóa 005 ghi đè bằng `{version}`. Chủ dự án chọn giữ phạm vi 014, không sửa `CareLockStore` 005 trong lượt này.

## 5. Hướng giao diện duyệt 07/11, triển khai 09/11/2026

Thay hai dòng SPI `HoatDongKenhNctPort`/`ChuyenDiSosPort` ở mục 1.1 bằng API chỉ đọc ở care để tránh vòng C-02; không cài hai SPI cũ và không sửa mã 012/007:

- `care.api.ResidentActivityReadApi.upcoming(long residentId, LocalDate from, LocalDate to)` trả `List<ResidentActivitySessionView>`; hai đầu ngày tính cả. DTO ở `care.api.dto`: `(sessionId, name, startAt, endAt, facilityWide, registered)`. Chỉ buổi Đã lên lịch của tầng người cao tuổi hoặc toàn viện; không có đánh giá sức khỏe hay lý do bị chặn.
- `care.api.ResidentTripReadApi.ongoing(long residentId)` trả `Optional<ResidentOngoingTripView>`; DTO `(tripId, Optional<Long> leaderId, List<Long> companionIds)`. Người đã rời viện chưa về; nhiệm vụ hiện hành sau chuyển trưởng đoàn. 007 gọi để xử lý SOS.
- `care.api.FeedbackPreferenceCommandApi.recordFromFeedback(long residentId, long feedbackId, Long interestGroupId, String description, String level, OpinionSource source, boolean confirmReplacement)` trả mã mục sở thích, **MANDATORY**. Kiểm quyền và luật nguồn/nhóm/xác nhận thay Q-363 như lệnh sở thích hiện có; `feedbackId` trong JSON `M04.PREFERENCE` (tên logic `M04.SO_THICH`, không thêm cột). Phát `resident.api.events.OpinionRecordedFromFeedback` cùng transaction, giá trị `sourceModule=M04`, `itemType=INTEREST`.
- `care.api.spi.TripIncidentPort.recordMissing(long residentId, long tripId, Long leaderId, String lastSeenPlace, Instant lastSeenAt, String description)` trả `long` mã sự cố; **MANDATORY**, adapter 007 ghi sự cố đi lạc hoặc không trở về, Khẩn cấp, nguồn hoạt động ngoài viện. `recordFoundReturned(long incidentId, long residentId, long tripId, Instant at)` **MANDATORY**, ghi diễn biến đã tìm thấy, đã trở về, không tự đóng. Thiếu bean trả 503 `CARE_SOURCE_UNAVAILABLE`; lỗi nguồn rollback lệnh chuyến. Thay việc gọi trực tiếp `SuCoLenhApi` ở mục 2.

Bản giả chung cho ba API: `FakeM04ApiConfig` + `FakeM04ApiFixture`; bản giả port: `FakeTripIncidents` trong `FakeM04Config`. Caller 012/007 dùng bản giả khi chưa tích hợp, không viết bản giả thứ hai.

Ngoại tuyến: loại `M04.TRIP_MISSING` có `TripMissingOfflineHandler.requiresOnline() = true`; bộ đồng bộ chung trả `REJECTED/REQUIRES_ONLINE` trước khi giải nội dung, áp dụng cho hai lệnh ghi sự cố thiếu người. Lệnh HTTP nhận `deviceAt` cũng bị chặn `409 REQUIRES_ONLINE`. Không mở ghi ngoại tuyến rời viện/về (còn chờ B/001).

Khóa quyền ghi sự cố: `INCIDENT_EMERGENCY/CREATE` theo 19.3, Phụ lục 27 (mọi người đi cùng, kể cả Nhân viên bếp); kiểm nhiệm vụ chuyến ở domain vẫn bắt buộc. Các lệnh chuyến khác giữ khóa/quy tắc hiện có.

## 6. Dòng trạng thái riêng — quyết định 09/11/2026

Chuỗi phải ở dòng riêng `M04.HOAT_DONG_NCT`, không dùng dòng khóa M04 của 005. Kiểm mã nền hiện tại: `ResidentModuleStateApi` dùng `kind.module()` (chỉ tiền tố M04), khóa bảng `(resident_id,module)` với `module VARCHAR(8)`. Chỉ đổi SharedKind vẫn ghi chung dòng; không tự sửa nền 000 hoặc ghi JDBC vượt hợp đồng. Chủ dự án chọn chờ A mở rộng nền 000 trong phiên 09/11; lưu chuỗi/job/cảnh báo và thống kê chuỗi tiếp tục chờ. Các hàm thuần ngày tính/chuỗi/tỷ lệ có thể kiểm độc lập. Lịch sử cảnh báo hồ sơ tinh thần chưa có quyết định, giữ `alerts = null`.
