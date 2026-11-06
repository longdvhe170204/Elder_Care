# Giao diện giữa module: Feature 010

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Giao diện Java công khai ở `billing.api`, port nội bộ M11 cho 017, 019, và bản cài các SPI do module thấp hơn khai báo (research P-02, P-12). Kiểu `DieuKien`, `CanCu`, `LyDo`, `NguoiDungHienTai`, `DoiTuong` theo 000, 001, 002. Hàm ghi có ghi chú `MANDATORY` chạy trong transaction của lệnh gọi.

---

## 0. Trạng thái nền develop 62fdb4f2 — cập nhật 2026-11-04

Các chữ ký ở mục 1–3 dùng tên logic thiết kế; chữ ký E thực tế ở mục 4 và glossary 16.6. T003/T004 và các fake chung ở support/fake/billing đã vào develop. Nhánh impl/010-foundation-next tiếp tục nền: BillingPeriod, khóa, lịch sử/marker, khung job và provider metadata/phạm vi; chưa có bản thật các API thống kê/chi phí hoặc listener nguồn. Không có fake production. Bản dịch HTTP được duyệt 2026-10-29, nhưng còn 17 phản hồi thành công thiếu schema phải chốt trước endpoint liên quan. Xem [checkpoint hiện tại](../foundation-checkpoint-2026-11-04.md); các báo cáo chuẩn bị cũ là lịch sử.

| Nguồn | Đã có trong mã | Còn chờ / tác động |
| --- | --- | --- |
| A, 000 T108 | API bảng chung, ReminderMarkers/TechnicalLocks và fake đúng chủ đã có | T005/T007 dùng API thật và kiểm MySQL; không còn chờ A khai báo. Không đổi api A; hai đăng ký job M11 trong test kiến trúc cần A review |
| B, 001 | `resident.api.ResidentApi` (status/finalStatusAt), `resident.api.spi.StatusTransitionParticipant` | Có thể dùng phần hiện có; không sửa chữ ký |
| B, 004 | ContractReadApi.contentAtDate/contract/ofResident, UnitPriceApi, ResidencyDayApi, ResidencyEndingReadApi, SPI kỳ cuối/việc sau qua đời/người đại diện và FakeContractSchedules/FakeRepresentatives đã có | T006 hàm thuần xong; adapter nguồn T011/T014 còn E. Cần B/E rà billingStopDate khi hồ sơ PREPARING quá ngày dự kiến so với FR-035; không sửa B hoặc suy mốc dừng |
| C, 005 | BillableCareResultRecorded/BillableCareResultCorrected đã khai báo; mã nguồn vẫn giữ gate billable | Quantity/newQuantity đã được người dùng chốt; xem [quyết định 05/11](../care-quantity-decision-2026-11-05.md). C còn cài producer T033/T036/T050/T051, E còn consumer T010/T021; chưa nghiệm thu toàn chuỗi, không tự suy đơn vị hoặc sửa API nguồn |
| D, 006 | MedicationBilledEvent phát từ dùng liều và PRN; MedicationBillAdjustedEvent phát từ đính chính liều/PRN; PurchaseAssistancePort và fake đã có | Đã có thêm producer so với rà 30/10; chưa nghiệm thu toàn chuỗi consumer 010 hoặc khẳng định đủ nhánh DISPENSE_RECORD; không sửa record/producer của D |
| A, 012 / E, 017 | RepresentativePort đã có bản thật của A/012; DepositStatusPort/PurchaseBalancePort còn bản thật của E/017 | T002 dùng fake B hiện có; 017 là việc E làm sau 010. Không còn giao việc 012 cho E, không dùng fake production |

## 1. 010 cung cấp

**T033 — tích hợp retention, đề xuất 06/11 chưa áp dụng (tách khỏi gate nền T008):** [proposal-retention-cross-module-2026-11-06.md](proposal-retention-cross-module-2026-11-06.md) mô tả overload referencesTo(DataRef) ở SPI 000 và điểm gọi job để nối M01.RESIDENT → bảng → khoản của 010. Đây chưa phải API đã công bố hoặc bằng chứng toàn chuỗi đã xanh; người dùng đã chốt giữ E, bàn giao để A review/cài. Phần E đã sửa giữ thứ tự khi delete(Collection), không đổi chữ ký API.

**Cập nhật nền đơn giá 06/11:** `billing.internal.pricing.service.CostPricingService` dùng `ContractReadApi.contentAtDate(...).recordedUnitPrices` và `content.services[].serviceId/inPackage` theo hợp đồng 004 hiện có; ngoài hợp đồng gọi `UnitPriceApi.unitPriceAt(..., markReferenced=true)` trong transaction MANDATORY của caller. T006a đã kiểm xanh, không phải API công khai mới hoặc bản cài ba API chi phí ở mục dưới. Không tạo khoản/bảng; adapter lưu trú/bán trú và listener tính lại/thông báo vẫn thuộc T014. Xem [checkpoint 06/11](../pricing-checkpoint-2026-11-06.md).

### 1.1. SPI khai báo ở module thấp hơn, M11 cài

| SPI (gói khai báo) | Bản cài của 010 | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`resident.api`) | Bean `@Order` sau 007. Điều kiện `KT_A_CHI_PHI` (`coTheMien = true`) cho `KET_THUC_LUU_TRU`; tác động `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN` theo research P-10 | 001 H-02, 004 T-09 |
| `KyCuoiPort` (`admission.api`) | `taoKyCuoiNhap`, `tinhLai`, `huyMocDung` (MANDATORY; research P-10); giữ đúng chữ ký 004 | 004 FR-066 |
| `MucViecSauQuaDoiPort` (`admission.api`) | `ma() = CHI_PHI_KY_CUOI`; `HOAN_THANH` khi kỳ cuối và bảng bổ sung tồn tại lúc qua đời đã chốt (FR-036) | 004 FR-071 |
| `DeNghiMuaHoPort` (`medication.api`) | `daMua(deNghiId, nctId)`: đề nghị `DA_MUA`, mục, số lượng, nhân viên mua (Q-139) | 006 FR-040 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 9, 10 | |
| `GanPhamViProvider` (002) | `M11.BANG_CHI_PHI`, `M11.KHOAN_CHI_PHI`, `M11.DE_NGHI_MUA_HO`: người cao tuổi | 002 FR-034 |
| `QuyenTepProvider` (`common.tep`) | `M11.CHUNG_TU_MUA_HO`: Hành chính, Quản lý viện, Kế toán, người đại diện của người đó | NFR-08 |

### 1.2. `billing.api.ChiPhiCongNguoiThanApi` (012; FR-037, FR-038)

```java
List<BangChiPhiView> bangDaChot(long nctId, NguoiDungHienTai nguoiThan);          // tên thuốc theo quyền (FR-019)
TamTinhView tamTinh(long nctId, LocalDate tu, LocalDate den, NguoiDungHienTai nd); // FR-038: tổng theo loại, nhãn "tạm tính", ghi chú thiếu đơn giá
List<DeNghiMuaHoView> deNghiChoDongY(long nctId, long nguoiThanId);                // FR-023a; kèm dấu số dư không đủ (Q-219)
```

### 1.3. `billing.api.ChiPhiKyCuoiApi` (004 hiển thị danh sách điều kiện)

```java
TrangThaiDieuKien dieuKienKyCuoi(long nctId);   // FR-034a: DAT | CHUA_DAT kèm lý do (bảng chưa chốt, ngày thiếu khoản, điều chỉnh ảnh hưởng chưa chốt)
```

### 1.4. `billing.api.ChiPhiThongKeApi` (016; 18.4, 18.5)

```java
Stream<KhoanBaoCaoView> khoan(PhamViLoc phamVi, LocalDate tu, LocalDate den);
    // loại, nguồn sinh, người lập, dịch vụ/vật phẩm/hoạt động (thuốc: mã), ngày phát sinh, số lượng, đơn giá, thành tiền, dấu, trạng thái
Stream<BangBaoCaoView> bang(Optional<Long> kyId);    // thường/bổ sung, kỳ, trạng thái, hạn chốt
Stream<KyView> ky();
```

### 1.5. Sự kiện `billing.api` cho module làm plan sau phát (011, 012, 014)

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

Theo research P-12 (`ChiPhiChoSoDuPort` 010 cài; `SoDuMuaHoPort` 017 cài). *(Bổ sung 2026-10-22, FR-042, Q-378)* `ChiPhiChoSoDuPort.bangDaChot(bangId)` trả chi tiết khoản Đã chốt và tổng theo loại cho phiếu báo phí, bảng quyết toán của 017 (chữ ký ở research P-12). CostForBalancePort chỉ billing.internal.balance/billing.internal.procurement gọi; PurchaseBalancePort chỉ billing.internal.purchase gọi (ArchUnit theo hai chiều P-12). Khi 017 chưa có, `SoDuMuaHoPort` không có bean và đề nghị không mang dấu "số dư không đủ".

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
| `NguoiDaiDienPort.laNguoiDaiDienHieuLuc`, `coNguoiDaiDien` (`admission.api`, 012 cài) | M10 (012) | FR-023, FR-023a | Chưa có khai báo/fake này ở 004; chờ B khai báo, E cài ở 012 |
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

## Đổi api — chọn đơn vị nguồn, chủ dự án duyệt 06/11/2026

- HTTP BillingSource thêm billingUnit (khóa đơn vị tính phí); unit giữ đơn vị sử dụng/hiển thị. CreateCostsFromEvents thêm sourceUnits gồm sourceId/billingUnit; chọn đúng một kiểu sourceIds/sourceUnits/dateRange. sourceIds cũ chỉ dùng khi xác định duy nhất một đơn vị; mơ hồ trả dòng bỏ qua CONDITION_NOT_MET, không tự lập tất cả.
- BillingSourceView của E thêm thành phần billingUnit; rà toàn kho chưa có nơi tạo record này ngoài khai báo. Không đổi chữ ký sự kiện CostSourceBilled/CostSourceChanged hoặc API nguồn C/D. Chưa có bản cài endpoint/lệnh đầy đủ.
- Quyết định và ví dụ: proposal-source-unit-selection-2026-11-06.md. Nguồn metadata/đăng ký client còn bàn giao ở source-metadata-handoff-2026-11-06.md; A/các consumer review thay đổi công khai trước khi tích hợp. Không tự sửa script sinh client của A hoặc frontend/src/api.

## Đổi api — tính lại giá Phase 4, 06/11/2026

- Người dùng giao chọn/cài phương án phù hợp spec từ đề xuất proposal-cost-item-pricing-notification-2026-11-06.md. CostItemChanged nay có (residentId, periodId, List<NotificationRequest> notificationRequests), cài HasNotificationRequests; constructor (residentId, periodId) giữ nguyên. Payload outbox cũ thiếu trường hoặc null đọc thành danh sách rỗng.
- Chỉ gửi Nhẹ tới FacilityRole(OFFICE_STAFF), InformationType.COSTS cho tác động giá; không tên thuốc/chênh lệch tính sẵn. CostItemChanged vẫn là tín hiệu để 017/016 đọc lại; không thêm mảng id chi phí. Ghi team-plan để A (009/016) và consumer 017 review, chưa coi đã review.
- Consumer 010 dùng thêm DayCareCalendarApi.onDate của B cho ngày nghỉ trong mẫu số tháng; đọc lịch sử lịch đến từ ContractReadApi.contentAtDate, không qua bảng private hoặc Care API sức khỏe. Không đổi chữ ký B và không tạo fake B/C/D thứ hai.
- Thuốc dùng UnitPriceApi.facilityMedicationServiceOf; phân giải chăm sóc theo CareLevel của nội dung hợp đồng đúng ngày và kiểm khớp đăng ký; không dùng mức chăm sóc hiện tại hoặc tự gộp thành phí phòng.
