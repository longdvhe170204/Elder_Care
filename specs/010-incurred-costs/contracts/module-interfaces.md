# Giao diện giữa module: Feature 010

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `m11.api`, port nội bộ M11 cho 017, 019, và bản cài các SPI do module thấp hơn khai báo (research P-02, P-12). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong` theo 000, 001, 002. Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 0. Trạng thái worktree nền develop 8573190 — cập nhật 2026-10-30

Các chữ ký ở mục 1–3 dùng tên logic của thiết kế; không phải tuyên bố các interface đã có trong mã. Tên tiếng Anh ở glossary mục 16.6. T003/T004 đã khai báo API/DTO, tám sự kiện và hai port nội bộ tại nhánh E; năm fake chung ở support/fake/m11 qua FakeM11Config, không có fake production. Chữ ký thực tế ở mục 4 bên dưới. Hợp đồng HTTP đã được A/chủ dự án duyệt ngày 2026-10-29; không tự mở rộng API của B/C/D. T001 và ba hàm thuần T006 đã có mã; API mới chỉ có khai báo/fake, chưa có bản cài thật, producer, listener hoặc provider. Chưa commit/push/merge vào develop. Bảng chi tiết và nơi cần duyệt ở [preparation-review.md](../preparation-review.md).

| Nguồn | Đã có trong mã | Còn chờ / tác động |
| --- | --- | --- |
| A, 000 T108 | R-15/constitution 3.2.0 đã vào develop; chưa có V000_09, m15.shared hoặc hai helper common | Chờ CatalogApi, StatusHistoryApi, RecordEventApi, SimpleRecordApi, ResidentModuleStateApi, ReminderMarkers/TechnicalLocks và fake đúng chủ; không tạo lại bảng chung hoặc khai báo API/fake của A |
| B, 001 | `m01.api.ResidentApi` (status/finalStatusAt), `m01.api.spi.StatusTransitionParticipant` | Có thể dùng phần hiện có; không sửa chữ ký |
| B, 004 | `m02.api.ContractReadApi.attendanceOn`, DTO và FakeContractSchedules | Chưa có nội dung hợp đồng theo ngày, đơn giá, ngày lưu trú (gồm ngày Giải phóng giường để chặn ngay từ ngày đó theo Q-395), SPI kỳ cuối/việc sau qua đời/người đại diện hoặc sự kiện tương ứng (gồm kiểu đầu vào cho BillingPeriod của T006); B cần khai báo đúng contracts 004 và cung cấp fake chung, rồi bản thật |
| C, 005 | Các API chăm sóc đã khai báo cho 008 | Chưa có các sự kiện tính phí/đính chính/trạng thái có mặt cần tại mục 3.1; chờ C khai báo, phát và kiểm nguồn thật |
| D, 006 | `MedicationBilledEvent`, `MedicationBillAdjustedEvent`, `PurchaseAssistancePort.purchasedDetails` và FakePurchaseAssistancePort | Mới tìm thấy phát MedicationBilledEvent tại lệnh dùng liều; chưa có nơi phát MedicationBillAdjustedEvent hoặc nhánh nguồn PRN_ADMINISTRATION/DISPENSE_RECORD trên develop. Chờ D, không tự viết producer hoặc đổi record của D |
| E, 012/017 | Chưa có bản thật người đại diện/số dư | Là phần E sẽ làm ở feature khác; không ghi thành việc phải chờ B/C. Port người đại diện vẫn phải do chủ khai báo ở 004 cung cấp trước |

## 1. 010 cung cấp

### 1.1. SPI khai báo ở module thấp hơn, M11 cài

| SPI (gói khai báo) | Bản cài của 010 | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`m01.api`) | Bean `@Order` sau 007. Điều kiện `KT_A_CHI_PHI` (`coTheMien = true`) cho `KET_THUC_LUU_TRU`; tác động `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN` theo research P-10 | 001 H-02, 004 T-09 |
| `KyCuoiPort` (`m02.api`) | `taoKyCuoiNhap`, `tinhLai`, `huyMocDung` (MANDATORY; research P-10); giữ đúng chữ ký 004 | 004 FR-066 |
| `MucViecSauQuaDoiPort` (`m02.api`) | `ma() = CHI_PHI_KY_CUOI`; `HOAN_THANH` khi kỳ cuối và bảng bổ sung tồn tại lúc qua đời đã chốt (FR-036) | 004 FR-071 |
| `DeNghiMuaHoPort` (`m07.api`) | `daMua(deNghiId, nctId)`: đề nghị `DA_MUA`, mục, số lượng, nhân viên mua (Q-139) | 006 FR-040 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 9, 10 | |
| `GanPhamViProvider` (002) | `M11.BANG_CHI_PHI`, `M11.KHOAN_CHI_PHI`, `M11.DE_NGHI_MUA_HO`: người cao tuổi | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M11.CHUNG_TU_MUA_HO`: Hành chính, Quản lý viện, Kế toán, người đại diện của người đó | NFR-08 |

### 1.2. `m11.api.ChiPhiCongNguoiThanApi` (012; FR-037, FR-038)

```java
List<BangChiPhiView> bangDaChot(long nctId, NguoiDungHienTai nguoiThan);          // tên thuốc theo quyền (FR-019)
TamTinhView tamTinh(long nctId, LocalDate tu, LocalDate den, NguoiDungHienTai nd); // FR-038: tổng theo loại, nhãn "tạm tính", ghi chú thiếu đơn giá
List<DeNghiMuaHoView> deNghiChoDongY(long nctId, long nguoiThanId);                // FR-023a; kèm dấu số dư không đủ (Q-219)
```

### 1.3. `m11.api.ChiPhiKyCuoiApi` (004 hiển thị danh sách điều kiện)

```java
TrangThaiDieuKien dieuKienKyCuoi(long nctId);   // FR-034a: DAT | CHUA_DAT kèm lý do (bảng chưa chốt, ngày thiếu khoản, điều chỉnh ảnh hưởng chưa chốt)
```

### 1.4. `m11.api.ChiPhiThongKeApi` (016; 18.4, 18.5)

```java
Stream<KhoanBaoCaoView> khoan(PhamViLoc phamVi, LocalDate tu, LocalDate den);
    // loại, nguồn sinh, người lập, dịch vụ/vật phẩm/hoạt động (thuốc: mã), ngày phát sinh, số lượng, đơn giá, thành tiền, dấu, trạng thái
Stream<BangBaoCaoView> bang(Optional<Long> kyId);    // thường/bổ sung, kỳ, trạng thái, hạn chốt
Stream<KyView> ky();
```

### 1.5. Sự kiện `m11.api` cho module làm plan sau phát (011, 012, 014)

```java
record SuKienTinhPhiNguon(String loaiNguon, String nguonId, String donVi, long nctId, Optional<Long> dichVuId,
                          BigDecimal soLuong, LocalDate ngayPhatSinh, Instant thoiDiem, String khoaSuKien) {}
    // 011: SUAT_AN_NGUOI_THAN (một suất); 012: DEM_O_LAI (mỗi mốc 00:00 một đêm, ngày bắt đầu của đêm);
    // 014: DIEM_DANH_HOAT_DONG (lượt có mặt, kể cả "bỏ giữa chừng"; chuyến đi: điểm danh rời viện, ngày rời viện thực tế)
record SuKienTinhPhiNguonDaDoi(String loaiNguon, String nguonId, String donVi, Optional<BigDecimal> soLuongMoi,
                               boolean daHuy, String canCu, String khoaSuKien) {}
```

### 1.6. Sự kiện 010 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `BangChiPhiDaChot(bangId, loaiBang, kyId, nctId, tong, chotLuc)` (đúng một lần mỗi bảng) | 017 (giao dịch thanh toán bảng, DBR-30), 012, 016 | FR-033, FR-042 |
| `DeNghiMuaHoDaGui`, `…DaQuyetDinh`, `…DaHuy`, `…DaMua` (`deNghiId`, `nctId`, số tiền, trạng thái) | 017, 012 | FR-023, bảng giao tiếp "Gửi 017" |
| `KhoanChiPhiDaDoi(nctId, kyId)` | 017 (tính lại tổng chưa chốt), 016 | FR-038a |
| Thông báo theo bảng FR-044 (cài `CoYeuCauThongBao`, loại `CHI_PHI`, không tên thuốc) | 009 | FR-044 |

---

## 2. Port nội bộ M11 (017, 019)

Theo research P-12 (`ChiPhiChoSoDuPort` 010 cài; `SoDuMuaHoPort` 017 cài). *(Bổ sung 2026-10-22, FR-042, Q-378)* `ChiPhiChoSoDuPort.bangDaChot(bangId)` trả chi tiết khoản Đã chốt và tổng theo loại cho phiếu báo phí, bảng quyết toán của 017 (chữ ký ở research P-12). CostForBalancePort chỉ m11.balance/m11.procurement gọi; PurchaseBalancePort chỉ m11.purchase gọi (ArchUnit theo hai chiều P-12). Khi 017 chưa có, `SoDuMuaHoPort` không có bean và đề nghị không mang dấu "số dư không đủ".

---

## 3. 010 cần từ module khác

### 3.1. Sự kiện 010 nghe

| Sự kiện | Module | Xử lý | FR |
| --- | --- | --- | --- |
| `KetQuaCoTinhPhi`, `KetQuaTinhPhiDaDinhChinh` | 005 | Chỉ mục; tính lại / hủy / biến động sau chốt | FR-005, FR-020 |
| `TrangThaiCoMatDaXacDinh`, `TrangThaiCoMatDaDoi` | 005 | Buổi bán trú: tính lại khoản buổi | FR-005b, FR-020 |
| `SuKienTinhPhiThuoc`, `SuKienTinhPhiThuocDaDieuChinh`, `SuKienTinhPhiThuocDaDinhChinh` | 006 | Chỉ mục; tính lại (nhận lại thuốc, đính chính) | FR-005, FR-020 |
| `NgayLuuTruDaDoi(nctId, tuNgay, denNgay, nguyenNhan)` | 004 | Khoản phí lưu trú trong khoảng | FR-005a, FR-020 |
| `PhuLucDaApDung` | 004 | Khoản chưa chốt từ ngày hiệu lực tính lại; đổi phòng sau chuyển giường y tế: biến động và báo Hành chính (Edge Case) | FR-008, FR-027 |
| `HoSoKetThucDaLap`, `…DaDoiNgay`, `…DaHuy`, `…HoanTat` | 004 | Bổ trợ `KyCuoiPort` (thông báo, kiểm lại điều kiện); idempotent | FR-034 |
| `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi` | 004 | Khoản buổi bán trú tính lại hoặc hủy | FR-005b |
| `PhienBanDonGiaDaTao(dichVuId, hieuLucTu, hieuLucDen)` (bổ sung theo plan này vào hợp đồng 004) | 004 | Tính lại khoản `THIEU_DON_GIA` chưa chốt | FR-012 |
| `SuKienTinhPhiNguon`, `SuKienTinhPhiNguonDaDoi` | 011, 012, 014 | Chỉ mục; tính lại / hủy | FR-005, FR-020 |
| `ThamSoDaDoi(CFG-M11-*)` | 000 | Xóa bộ đệm; không đổi đề nghị đã gửi | FR-023 |

### 3.2. Giao diện có sẵn hoặc đã bổ sung

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `HopDongDocApi`, `DonGiaApi` (`donGiaTaiNgay` MANDATORY khi đánh dấu tham chiếu, `donGiaHopDong`, `dichVuChamSoc`, `thuocNguonVien`), `NgayLuuTruApi` | M02 (004) | P-04, P-05 | Chưa đủ trên develop e7b2b71; chờ B khai báo và fake chung/bản thật |
| `NguoiDaiDienPort.laNguoiDaiDienHieuLuc`, `coNguoiDaiDien` (`m02.api`, 012 cài) | M10 (012) | FR-023, FR-023a | Chưa có khai báo/fake này ở 004; chờ B khai báo, E cài ở 012 |
| `NguoiCaoTuoiApi.trangThai`, `thoiDiemTrangThaiCuoi` | M01 (001) | FR-011, FR-036 | Có sẵn |
| `DanhSachViecSauQuaDoiApi.capNhat` | M02 (004) | FR-036 | Chưa có Java API ở 004; chờ B |
| `QuyenApi`, `QuyenXemTruongApi` (tên thuốc theo quyền), `NguoiDungHienTai` | M15 (002) | P-06, P-14 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `TepApi`, `NhacQuaHan`, `common` (`thongbao`) | M15 (000) | Mọi lệnh | Có sẵn |
| Tên thuốc theo mã cho người được xem: `DonThuocApi`/danh mục thuốc của 006 (đọc) | M07 (006) | FR-019 | Chỉ hiện mã |

## 4. Chữ ký tiếng Anh đã khai báo ở nhánh E — T003/T004

Đây là lần khai báo đầu của M11, không thay chữ ký API của B/C/D/A hoặc sửa hợp đồng REST đã duyệt. Ba gói api/api.dto/api.events có NamedInterface. DTO ở api.dto phục vụ trao đổi module; chưa có controller hoặc bộ dựng phản hồi HTTP. Trường object chưa có cấu trúc trong YAML giữ Map/List<Map>, không tự bổ sung schema cho 17 response còn thiếu. Chi tiết phạm vi kiểm ở [api-declaration-verification.md](../api-declaration-verification.md).

| Interface | Hàm thực tế | Người dùng / fake chung |
| --- | --- | --- |
| FamilyPortalCostApi | finalizedStatements(residentId, CurrentUser); provisional(residentId, from, to, CurrentUser); awaitingConsent(residentId, familyMemberId) | 012; FakeFamilyPortalCosts |
| FinalPeriodCostApi | finalPeriodCondition(residentId) → FinalPeriodConditionView(satisfied, reasons) | 004; FakeFinalPeriodCosts |
| CostStatisticsApi | items(ScopeFilter, from, to) → Stream<CostItemReportView>; statements(Optional<Long> periodId) → Stream<CostStatementReportView>; periods() → Stream<CostPeriodView> | 016; FakeCostStatistics |
| CostForBalancePort | unfinalizedTotal(residentId) → long; baseDailyFee(residentId, date) → Optional<Long> | 017/019; FakeCostForBalance |
| PurchaseBalancePort | insufficientBalance(residentId, amount, at) → boolean | 010 gọi, 017 cài; FakePurchaseBalance |

Port là nội bộ M11, đặt ở provision/internal/domain và purchase/internal/domain theo README, không xuất qua api. bangDaChot của P-12 thuộc T036/T037, chưa khai báo hoặc nghiệm thu ở T004. Chưa có caller thật nên quy tắc không có bean thì không đặt dấu còn được cài/kiểm ở T027.

| Sự kiện | Thành phần tiếng Anh | Ai phát sau này |
| --- | --- | --- |
| CostSourceBilled | sourceType, sourceId, unit, residentId, Optional<Long> serviceId, BigDecimal quantity, incurredOn, occurredAt, eventKey | D/011, E/012, C/014 |
| CostSourceChanged | sourceType, sourceId, unit, Optional<BigDecimal> newQuantity, cancelled, basis, eventKey | Cùng chủ nguồn khi sửa/hủy |
| CostStatementFinalized | statementId, statementType, periodId, residentId, total, finalizedAt, notificationRequests | E/010 T017 |
| PurchaseAssistanceSent/Decided/Cancelled/Purchased | requestId, residentId, estimatedAmount, status, notificationRequests | E/010 T027 |
| CostItemChanged | residentId, periodId | E/010 khi khoản đổi/chốt |

Số tiền long giữ dấu và đơn vị đồng theo chữ ký P-12/bảng giao tiếp; số lượng nguồn BigDecimal. Optional.empty cho số lượng mới khi hủy khác với giá trị 0. Các sự kiện có yêu cầu thông báo cài HasNotificationRequests và giữ danh sách bất biến; nhóm người nhận dùng RecipientGroup, không mang danh sách dài người nhận/id.

FR-044/P-13 vẫn do dịch vụ nguồn chịu trách nhiệm chọn đúng nhóm/mức/nội dung; yêu cầu tới người thân phải mang InformationSubject.ABOUT_FAMILY_MATTER và không có mã Q-354. Test lượt này kiểm khả năng giữ metadata qua outbox, không thay nghiệm thu producer hoặc bộ dựng yêu cầu thật (chưa có).

FakeM11Config là TestConfiguration; chỉ @Import trong test. Đã thêm FakeCostSources phát CostSourceBilled/CostSourceChanged qua ApplicationEventPublisher cho fixture của 011/012/014; đây không phải producer thật. FakeCostStatistics nhận dòng đã thuộc ScopeFilter do fixture đặt, rồi lọc ngày gồm cả hai đầu; không giả vờ đọc vị trí/phạm vi thật. FakeFinalPeriodCosts/FakeFamilyPortalCosts không tự trả trạng thái đạt hoặc tạm tính khi chưa cấu hình. Các fake này phải được dùng lại khi tích hợp, không tạo bản giả thứ hai.
