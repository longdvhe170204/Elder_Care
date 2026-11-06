# Đồng bộ develop cho E — 04/11/2026

Đã fetch, checkout develop (nhánh vốn đang là develop), pull --ff-only từ `c8a8addd` lên `55807c24fb874da1078bf03dfa714c65d9a50535`, thêm 95 commit. Mốc kéo gần nhất theo reflog: 02/11/2026 22:36:50, giờ Việt Nam. Cây làm việc sạch trước cập nhật; không cần tích hợp nhánh riêng.

So sánh theo khoảng commit trên, không theo ngày tác giả: có 86 dòng tiến độ mới được đưa vào develop, gồm cả nhật ký cũ từ nhánh vừa merge. Báo cáo này là kết quả rà tài liệu/mã và truy vấn chỉ đọc; không phải kết quả chạy lại toàn bộ test.

## (a) Trạng thái chung

14 PR mới trên lịch sử first-parent của develop:

| PR | Nội dung |
| --- | --- |
| [#92](https://github.com/longdvhe170204/Elder_Care/pull/92) | 002: màn web tạo mã QR đăng nhập người cao tuổi. |
| [#93](https://github.com/longdvhe170204/Elder_Care/pull/93) | 008: sửa thông báo bàn giao FR-057; đưa nền 010 T001/T003/T004/T005a vào develop. |
| [#94](https://github.com/longdvhe170204/Elder_Care/pull/94) | 001: hồ sơ, đánh giá và tích hợp R-15. |
| [#95](https://github.com/longdvhe170204/Elder_Care/pull/95) | 006: thống kê thuốc, gộp bảng và cấu trúc module. |
| [#96](https://github.com/longdvhe170204/Elder_Care/pull/96) | 004: tiếp nhận/lưu trú theo R-15; công bố nguồn hợp đồng, đơn giá, ngày lưu trú. |
| [#97](https://github.com/longdvhe170204/Elder_Care/pull/97) | 007: chỉ số sức khỏe, cảnh báo, sự cố; Phase 1–3 và US1 backend. |
| [#98](https://github.com/longdvhe170204/Elder_Care/pull/98) | 007: sửa nhóm dữ liệu R-15, dịch đủ api-007 và kiểm hợp đồng T062. |
| [#99](https://github.com/longdvhe170204/Elder_Care/pull/99) | 016: sửa checksum SC_008_ReadOnlyIT ổn định giữa các kết nối. |
| [#100](https://github.com/longdvhe170204/Elder_Care/pull/100) | 005: chăm sóc hằng ngày, tích hợp develop và khớp hợp đồng API. |
| [#101](https://github.com/longdvhe170204/Elder_Care/pull/101) | Review 005: đính chính, retention dòng chung và transport mobile. |
| [#102](https://github.com/longdvhe170204/Elder_Care/pull/102) | 000: sửa khóa kỹ thuật, ẩn CFG đã bỏ; A nhận 011/012, review 007. |
| [#103](https://github.com/longdvhe170204/Elder_Care/pull/103) | 000/009: nhóm dữ liệu file, sửa Nháp yêu cầu 004 và dừng chuỗi nhắc gọi. |
| [#104](https://github.com/longdvhe170204/Elder_Care/pull/104) | Q-399: nghiệp vụ/spec/hợp đồng tự đặt lại mật khẩu qua tin nhắn; chưa có luồng production. |
| [#105](https://github.com/longdvhe170204/Elder_Care/pull/105) | 008: sửa fixture AssignmentIT và ShiftQueriesIT. |

Lượt clean verify đầy đủ gần nhất trong progress trước #105: 1047 test đơn vị/kiến trúc xanh; 2565 IT, 2 failure và 26 error ở AssignmentIT/ShiftQueriesIT. #105 ghi kiểm riêng AssignmentIT 31/31 và ShiftQueriesIT 7/7 xanh. Chưa có chứng cứ full verify trên chính HEAD #105 trong tài liệu đã đọc; phiên này không chạy Maven/npm/Flutter.

Các lớp còn được theo dõi trong mục 8: `T064_BedListPerformanceIT` (C, vượt 1000 ms); `FR_024_AllocationStatusParticipantIT.FR_020…` (C, nhạy tải); `BR_M01_02_ReassessmentNoticeIT.T048…` (B, ngưỡng 5 giây); `StaffImportTransportIT` (E, môi trường loopback Windows). Hai ca `ParameterChangeIT.US4_7/US4_9` còn dòng theo dõi A/C, không tái hiện ở lượt sau và chưa chốt nguyên nhân.

Không tiếp tục tính AssignmentIT/ShiftQueriesIT là lỗi chưa sửa sau #105. HandoverIT đã 27/27 xanh; SC_008_ReadOnlyIT được #99 sửa; timeout NFR_03_DeliveryDispatcherIT và lỗi Hikari không tái hiện ở lượt #102. Dòng ResidentContractIT thiếu schema at còn trong bảng nhưng progress sau đó xác định trùng tên OpenAPI của care và đã xử lý khi merge #100; không coi đó là lỗi đang tái hiện nếu chưa chạy lại.

## (b) Ảnh hưởng tới E

- Theo team-plan mục 1 và CODEOWNERS: E giữ `staffing`, `billing`; 008, 010, 015, 017; migration V008/V010/V015/V017 và màn tương ứng. **012/family chuyển từ E sang A ngày 03/11 ở #102**, chưa có mã phải bàn giao. E chỉ review các lời gọi 012 sang staffing/billing.
- Đợt E: 1 → 008; 2 → 010; 3 → 015, 017 sau 010; 4 → sửa lỗi tích hợp. 015 chỉ đổi ca/nghỉ đột xuất theo Q-397.
- 008 thêm T051 [x] cho FR-057; không đổi chữ ký HandoverCorrected, sửa nhóm nhận gồm người bàn giao, người đã xác nhận qua AccountApi và ShiftLead hiện tại. T050 vẫn là R-15. Mobile T016b/T022b/T030/T041b còn mở; giai đoạn đã mở 27/10.
- 010: T001/T003/T004/T005a đã [x]; T002/T005/T006/T007/T008 còn mở hoặc một phần. Mã thực tế đã chuyển namespace sang `com.eldercare.backend.billing`, domain theo `billing.internal.*.domain`. 015/017 tasks không đổi trong khoảng cập nhật.
- API E sở hữu mới vào develop: `FamilyPortalCostApi`, `FinalPeriodCostApi`, `CostStatisticsApi`, DTO/sự kiện billing và fake chung. Đây mới là khai báo, không mặc nhiên có bản cài/producer.
- Nguồn B/004 mới có `ContractReadApi`, `UnitPriceApi`, `ResidencyDayApi` (gồm billingStopDate, cờ bedReleased), `ResidencyEndingReadApi`, DTO/sự kiện và các port `FinalPeriodPort`, `DepositStatusPort`, `PostDeathItemPort`. FinalPeriodPort còn ghi tên provisional chờ E duyệt. T002 của 010 không còn phải chờ B khai báo người đại diện: `FakeRepresentatives` đã có; dùng fake đúng chủ.
- 005 đã merge API/DTO/sự kiện và `ShiftStaffPort` có members/handoverTasks. Staffing hiện vẫn cài CareShiftPort cũ. `CareTaskHandover` có bản cài nhưng **năm callback vắng/nghỉ/đổi phân công còn trả 503**; không nghiệm thu tích hợp chỉ bằng fake.
- 007 công bố thêm API/DTO/sự kiện health/incident, `ContactAdded` thêm phong bì nhưng giữ constructor cũ. T003/T004 còn chờ E review. Toàn api-007 đã dịch tiếng Anh; client và kiểm hợp đồng đã cập nhật.
- 000 thêm `CorrectableRecordProvider.supports(CorrectionType)` mặc định true; loại không hỗ trợ bị từ chối 409 trước khi lập yêu cầu cách C. `SharedRowRetention.rowsFor(EntityRef)` để xét phần đi kèm trước khi xóa; ApprovalRequestApi.editDraft/RequestTypeHandler.mayActOnDraft dành cho sửa Nháp. Các thay đổi này cần được giữ khi nối đính chính/retention/yêu cầu.
- Các review cũ về openHandovers, trường báo cáo StaffingRatioView, ShiftApi.staffOnShift/onDutyDoctors, AssignmentQueries.history vẫn mở; không phải chữ ký vừa mới thêm sau c8a8addd. T110 đổi gói DTO/events cũng là việc đồng bộ còn ghi trong bảng, phiên này đã kéo và rà phần E.

**CFG:** thêm CFG-M15-12 (mã đặt lại mật khẩu 5 phút), M15-13 (5 lần sai), M15-14 (tối đa 5 yêu cầu/ngày, cách ít nhất 60 giây), nạp bởi V000_17. Gỡ khỏi ParameterCode/API 10 mã đã bị nghiệp vụ bỏ: CFG-M02-08; M07-06/07; M08-08/09; **M09-06/07/10**; M10-03/12. Dòng DB giữ để bảo toàn lịch sử; enum hiện 115 mã. Không gọi lại ba CFG M09 đã bỏ.

**Quyết định mục 24 thực sự mới/sửa:** Q-398 chốt danh mục sáu chỉ số và bảy loại sự cố; không suy ngưỡng y khoa. Q-399 cho nhân viên/người thân tự đặt lại mật khẩu bằng mã tin nhắn; chưa có hiệu lực khi chưa chọn/cấu hình nhà cung cấp. Q-06 sửa theo Q-399, vẫn chờ chủ dự án chọn nhà cung cấp. Q-395–397 không đổi trong khoảng commit này: vẫn cần áp ngày Giải phóng giường ngay từ chính ngày đó và bỏ xoay ca/thiếu phủ/lời mời.

**Ghi nhớ/Áp dụng:** mới có quy tắc LAST_INSERT_ID theo kết nối (dùng KeyHolder hoặc TransactionTemplate) và xử lý migration số nhỏ đến sau. E cần rà bảy fixture staffing được liệt kê tại mục 8. Các ghi nhớ demo/R-15/DECIMAL(15,0)/trigger khóa theo cha chỉ đổi phần người nhận sau chuyển chủ 011/012; không phải quy tắc nghiệp vụ mới. AGENTS đổi outbox SERIALIZED_EVENT sang TEXT từ V000_16, vẫn tránh danh sách dài trong sự kiện.

## (c) Mọi dòng có E ở cột Ai làm

48 dòng tại develop 55807c24, trong đó 26 dòng mới hoặc đổi so c8a8addd; đã loại các dòng Ghi nhớ/Áp dụng. **Số dòng dưới đây thuộc ảnh chụp trước khi dọn bảng trong phiên**, giúp tra bằng `git show 55807c24:docs/team-plan.md`. “Mới/đổi” chỉ so nội dung dòng, không đồng nghĩa phát sinh quy tắc mới.

| Dòng | So mốc kéo trước | Ai làm | Việc và kết luận |
| --- | --- | --- | --- |
| 103 | Mới/đổi | E (008) | `AssignmentIT` 2 failure/25 error và HandoverIT từng đứng. **Đã được cập nhật:** #105 ghi AssignmentIT 31/31, ShiftQueriesIT 7/7 xanh; HandoverIT đã xanh trước đó. Xóa dòng chờ cũ trong phiên này. |
| 104 | Mới/đổi | B (001, 013), C (003, 014), E (008), A (012) | Review API/DTO/sự kiện/fake 007 T004, gồm phong bì thông báo `ContactAdded`; D chờ E và các consumer review. |
| 106 | Mới/đổi | C (005), E (008, 016), A (012) | Review 007 T003: `VitalSignApi`, `MeasurementScheduleApi`, `FacilityContactApi`, `VitalSignStatisticsApi`, bốn sự kiện health; D chờ review, nguồn thật còn theo task D. |
| 110 | Cũ | C (003), E (008) | Cài `HandoverCorrection.residentId` nếu bản gốc gắn NCT, để nhật ký đính chính có resident_id. Trùng một phần dòng 124; chưa làm. |
| 112 | Mới/đổi | C (005), E (008) | Review ghép API M04: giữ onAbsence/onResignation, DTO, transaction MANDATORY và nextShiftId nullable; A đã review sau #100, còn E. |
| 120 | Mới/đổi | C (003), E (008) | C chốt phát hay bỏ AllocationStarted/AllocationClosed cũ; E bỏ listener nếu bỏ sự kiện, tránh tính tỷ lệ hai lần. Chờ C rồi E. |
| 124 | Mới/đổi | E (008) | Phần E của `CorrectableRecordProvider.residentId`; C đã cài provider phân bổ. Cùng việc E ở dòng 110. |
| 130 | Cũ | E (008), A (016) | Review API thống kê 008 do A mở rộng cho 016: openHandovers; các trường nhân lực, mốc ca, nhóm giấy phép. Đã cài/test xanh theo bảng; E review. |
| 134 | Cũ | E (008), A (009) | Review `ShiftApi.staffOnShift/onDutyDoctors`, fake và ShiftQueriesIT cho 009; bản cài đã có, E review. |
| 136 | Mới/đổi | A (web chung), E (008) | Review ghép generator/client/navigation/menu Nhân sự. E đã kiểm; trạng thái còn chờ A review, ghi chú chưa commit là lịch sử cũ. |
| 137 | Mới/đổi | E (008), A (002 review kết nối) | Review StaffService đọc accountId qua AccountApi.byStaff; còn A xác nhận liên kết và kiểm môi trường tách riêng. |
| 138 | Mới/đổi | A (web chung), E (008) | Review BookOpen trong AppShell để tránh trắng trang; E đã sửa/kiểm, còn A review. |
| 139 | Cũ | C (003), D (006), E (008) | Review ghép 28 conflict với API vị trí/giường/liều chính thức; E đã ghép, C/D còn review. |
| 140 | Cũ | E (008), C (005), D (006/007) | Review lịch sử phụ trách/phạm vi theo thời điểm và tám SPI thật. A đã review; còn C/D kiểm với nguồn nghiệp vụ thật. |
| 141 | Cũ | E (008) | Sửa `DutyDirectory.shiftHasNoFloorManager` theo Q-262: Trưởng tầng không trực hoặc được ghi vắng/nghỉ từ mốc hiệu lực. E đang chặn A/002 và C/014. |
| 142 | Cũ | E (008), D (007) | Review AlertStaffPort ba hàm ca/tầng; E đã có bản cài, còn D/007 chạy H1–H9 US13 với nguồn thật. |
| 143 | Cũ | E (008), nhóm nghiệm thu; B/C/D chủ nguồn demo | Nghiệm thu quickstart 008, số đo SC-002/010 và vận hành một tháng SC-003; chờ nguồn/demo và người nghiệm thu. Mobile nhân viên đã mở 27/10, ghi chú chờ mở đã cũ. |
| 144 | Mới/đổi | C (005), E (008) | Nguồn thật ShiftTaskApi.onAbsence cho 008; C đã review caller/fake, nhưng callback production hiện trả 503. Chờ C cài, E kiểm tích hợp. |
| 145 | Cũ | E (015) | 015 dùng ShiftCommandPort/AssignmentCommandPort trong cùng transaction, chỉ đổi ca theo Q-397; E nối khi triển khai 015. |
| 147 | Mới/đổi | C (005), E (008) | Nguồn thật ShiftTaskApi.onResignation; caller/fake đã được C review, production hiện trả 503. Chờ C rồi E kiểm. |
| 148 | Mới/đổi | C/D (003/005/006/007), E (008) | Review/nối nguồn bàn giao Task/Dose/Alert/VitalSign/Bed/Allocation; C đã review một phần, còn C/D cung cấp nguồn và nghiệm thu thật. |
| 149 | Cũ | B (001), E (008) | B review sửa bộ lọc ResidentContractIT tránh gom endpoint bàn giao 008; E đã cài/kiểm, còn B review. |
| 155 | Cũ | B (013), C (014), E (015) | B/013 cài HeldBelongingsHandoverPort, C/014 TripDutyPort, E/015 ExtendedPublicationCheck. Các nguồn thật còn theo feature. |
| 159 | Cũ | E (008) | C review LocationReferences do E đã cài, tách tham chiếu hiện tại và lịch sử; phần E đã có, còn C review. |
| 160 | Cũ | E | Dòng vẫn giao E cài FamilyRelationshipApi/FootnoteCondition/rightEnabled của 012. **Sai chủ hiện tại:** 012 đã chuyển sang A ở #102; không triển khai vào family với tư cách E. |
| 175 | Cũ | E (017) | 017 T045: E cài ParameterValueCheck cho CFG-M11-10, kiểm ngân hàng còn hiệu lực, số tài khoản và chủ tài khoản. |
| 188 | Cũ | C (003), D (006), E (008) | Thông tin công tắc lịch test/on-startup chung; ba job M09 đã tự tắt trong test. Chỉ cần áp quy ước khi thêm/sửa job. |
| 190 | Cũ | C (003, HungNX-nguyen), E (008) review | Review bản thật API giường và nullable bedId (chỉ WITHOUT_BED); C đã có bản cài, E review consumer, giữ giới hạn lịch sử. |
| 192 | Mới/đổi | E (008) review | Review consumer API giường và AllocationHandoverApi.atFloor; createdAt legacy có thể null. A đã review 016, còn E phần bàn giao. |
| 194 | Cũ | E (008) | Sửa HandoverSources.bedRows dùng AwaitingReadyInterval.startedAt thay period.from đã cắt theo đầu ca. E tự làm được. |
| 199 | Mới/đổi | E (010, 015, 017) | 010 có CostStatisticsApi/fake, còn bản thật COST.*; E khai báo thống kê đổi ca 015 và số dư 017 kèm fake để A/016 dùng. |
| 205 | Mới/đổi | C (003), D (006), E (008) | Glossary 008 còn thiếu tên bảng staff_import_batch; E bổ sung phần mình, C/D giữ việc của họ. |
| 208 | Mới/đổi | E (010, 015, 008), A (012) | Rà Q-395–397: phí nội trú 100% kể cả vắng, bỏ bản tin/rotation/thiếu phủ/lời mời; E rà căn cứ INVITED không còn nguồn tạo. Các quyết định này đã có trước mốc kéo. |
| 216 | Cũ | B (001), C (003), D (006), E (008) | 008 T050/V008_95 gộp bảng R-15; API chung 000 T108 đã có, E không còn phải chờ A công bố bảng chung. |
| 218 | Cũ | E (008) | Review fixture staffing sau gộp bảng 002 T098: AssignmentIT/M09GovernanceIT dùng catalog_item và practiceScopeIds; E review. |
| 220 | Mới/đổi | B, C, E | 000 T110 đổi DTO/events sang gói con. Đã kéo develop và rà import staffing/billing không còn các kiểu cũ được liệt kê; gỡ E khỏi dòng này trong phiên. |
| 226 | Cũ | E (008) | Converge cấu trúc staffing: bảy chức năng vào internal, controller vào web, gộp service/port, đưa SQL sang Store. Thực hiện trong converge 008. |
| 227 | Mới/đổi | E (008) | Xác minh StaffingRatioEvents.status báo thiếu nguồn/cấu hình khi xử lý async; chưa được chứng minh đã sửa bởi #105 (PR đó chỉ sửa fixture test). |
| 228 | Mới/đổi | E (008) | Sửa mô tả contracts 008 mục 2.5 từ BedStatusApi.currentState sang BedStatisticsApi.stateAt đúng consumer hiện có; E tự làm được. |
| 229 | Mới/đổi | E (008) | Cài adapter production care.api.spi.ShiftStaffPort thay nguồn CareShiftPort cũ, gồm handoverTasks; giữ tương thích và test bean thật. E đang chặn C. |
| 232 | Mới/đổi | E (008), C (005, HungNX-nguyen) bàn giao/retest | ShiftStaffPort thật gồm members(shiftId), chốt cách liệt kê đủ ca đang diễn ra kể cả khu chưa có giường; chưa tự thêm ongoingShifts chưa duyệt. E/C cần thống nhất. |
| 241 | Mới/đổi | E (010), C (005, HungNX-nguyen) chờ quantity Phase6 | Thống nhất BillableCareResultRecorded.quantity theo đơn vị dịch vụ với C; chưa có căn cứ thì hỏi chủ dự án. Hiện lệnh billable chặn trước lưu. |
| 243 | Mới/đổi | E (008), C (005, HungNX-nguyen) chờ claim việc tồn Phase7 | Chốt ca/giờ kiểm người nhận việc tồn theo C10/Q-93 với C; ca sau nhận việc chưa nghiệm thu, không tự chuyển sang ca hiện tại. |
| 244 | Mới/đổi | E (008), C (005, HungNX-nguyen) bàn giao Phase7 | Review bàn giao care với ShiftStaffPort thật; task vị trí không biểu diễn được bởi residentId primitive, cần adapter/hợp đồng đã duyệt. Năm callback T038 còn 503, phần này chờ C. |
| 252 | Mới/đổi | E (010), C (005, HungNX-nguyen) chờ newQuantity Phase9 | Thống nhất BillableCareResultCorrected.newQuantity cho AMEND_CONTENT/VOID cùng quy tắc quantity dòng 241; nhánh billable còn 503. |
| 253 | Cũ | E (008) | StaffImportTransportIT lỗi loopback Windows: cách chạy đã biết là -DargLine=-Djdk.net.unixdomain.tmpdir=C:\eldercare-tmp. E ghi hướng dẫn; nếu đổi pom cần A. |
| 254 | Cũ | E (008) | Review AssignmentQueries.history.from dùng startsAt thực của phân công; 016 dùng thêm staffShifts. Không đổi chữ ký 008, E review. |
| 259 | Mới/đổi | E (010, 017), C (005), D (006, 007), B (013), A (012) | 004 đã công bố các port; phần E cần FinalPeriodPort (010), DepositStatusPort (017), PostDeathItemPort cho 010/017. B đang chờ nguồn thật; Representative/Pickup thuộc A/012. |

17 dòng khác có E trong cột “Ai đang chờ” (7 dòng mới/đổi); những dòng E cùng C/D ở bảng trên cũng bao gồm phụ thuộc của E:

| Dòng | So mốc kéo trước | Ai làm | E còn chờ gì / trạng thái thực tế |
| --- | --- | --- | --- |
| 113 | Mới/đổi | C (005, HungNX-nguyen) | C/005 đã công bố tám SPI và merge #100; không còn chờ khai báo. Nguồn thật và kiểm tích hợp vẫn tách riêng; trạng thái chưa merge ở dòng này đã cũ. |
| 115 | Cũ | A (000) | A/000: script sinh client mobile T010b khi cần; client viết tay hiện dùng được. Đăng nhập/điều hướng/offline nền đã xong 27/10. |
| 116 | Mới/đổi | B (001) | B/004 đã có AbsenceLookup implements resident.api.spi.AbsenceApi sau #96. Không còn thiếu bean như dòng cũ; cần kiểm nguồn bàn giao dùng đúng dữ liệu. |
| 119 | Mới/đổi | C (003) | C/003: chốt nguồn LocationApi cũ hay API vị trí hiện hành; dòng vẫn chờ provider, không tự dựng API thay C. |
| 128 | Cũ | D (007) | D/007: nguồn thật AlertHandoverApi cho bàn giao. |
| 135 | Mới/đổi | C (003) | C/003: rà fixture participant lưu trú/ALLOCATION_SOURCE_UNAVAILABLE với nguồn thật; lỗi M01 cũ không phải danh sách đỏ hiện tại. |
| 146 | Cũ | C (003) | C/003: review consumer tỷ lệ 008 và ba sự kiện phân bổ thật; E đã nối/kiểm. |
| 150 | Cũ | B (001) | B/001: review statusPeriods/FakeAbsences và biên khoảng thời gian do E đã cài. |
| 151 | Cũ | C (014), B (013) | C/014: TripInShiftView có residentIds/departedAt và nguồn thật; B/013: HeldBelongingsHandoverPort. |
| 152 | Cũ | B (001), C (003, 005), D (006, 007) | B/C/D: review tích hợp phân công Phase 4 và cung cấp nguồn thật. Dòng ghi đang chờ merge/PR của E là ghi chú cũ, không tự mở PR. |
| 153 | Mới/đổi | B (001), D (006, 007) review; C (005) nguồn thật | B/D review nguồn trạng thái/liều/cảnh báo; C cung cấp TaskType/CarePlan/ShiftTask thật. Sau #100 đã có một phần bản cài nhưng năm callback care vẫn trả 503. |
| 154 | Cũ | B (001) | B/001: FileApi.attach đã có bản thật; còn kiểm scan chứng chỉ trên môi trường lưu tệp thật. |
| 176 | Cũ | C (003, 005), D (006, 007) | C/D: review SPI ca/phân công tối thiểu và bộ fake duy nhất do chủ interface giữ. |
| 178 | Mới/đổi | A (012) | A/012: FamilyNotificationPort và quyền nhận khẩn cấp. 012 đã chuyển chủ, các tham chiếu E/012 cũ cần A rà. |
| 183 | Cũ | B (001) | B/001: nhận quyền sở hữu/review FakeConsentApi chung do A viết, cập nhật cùng ConsentApi khi đổi. |
| 189 | Cũ | B (001) | B/001: review statusPeriods theo lịch sử trạng thái, thêm test module resident; trùng một phần dòng 150. |
| 209 | Mới/đổi | B (004) | B/004: sửa mô tả và xác nhận ngày Giải phóng giường không tính phí từ chính ngày đó; dữ liệu mới ResidencyDayView đã mô tả theo cách này. |

Bảng có dòng trùng 110/124, 190/192, 229/232 và nhiều câu “chưa merge” từ nhật ký cũ; không biến chúng thành các task mới độc lập. Dòng 160 còn ghi E làm 012 trái với mục 1/CODEOWNERS; để A rà lại phân công.

## (d) Thứ tự nên làm tiếp

| Ưu tiên | Việc | Chặn bởi ai |
| --- | --- | --- |
| 1 | 008: cài ShiftStaffPort thật, gồm members/handoverTasks; kiểm ghép bàn giao/task vị trí với C, thống nhất ca nhận việc tồn C10/Q-93. Gỡ chặn 005. | E làm adapter; C phối hợp hợp đồng/ca tồn và năm callback production. Điểm nghiệp vụ chưa rõ cần chủ dự án. |
| 2 | 010: thống nhất quantity/newQuantity theo đơn vị dịch vụ cho hai sự kiện care; không tự quy ml/phút thành dịch vụ. Gỡ gate tính phí/đính chính 005. | E và C; thiếu căn cứ nghiệp vụ thì chủ dự án chốt. |
| 3 | 008: sửa Q-262 trong DutyDirectory để mở đúng quyền A/002 và C/014; viết test các ca không trực/vắng/có mặt. | E, không cần chờ nguồn khác cho phần sửa này. |
| 4 | Review T003/T004 của 007, API báo cáo/ca do A sửa, consumer API giường nullable/startedAt; sửa HandoverSources.bedRows, tài liệu stateAt, residentId đính chính, rà async tỷ lệ. | E chủ động; C/D/A review phần họ sở hữu. |
| 5 | 010: tiếp T002 với FakeRepresentatives có sẵn; T005 dùng API bảng chung T108 đã có; T006 BillingPeriod dùng DTO 004 mới. Sau đó T007/T008 → T009–T012. Chốt tên FinalPeriodPort với B rồi cài bản thật theo task kỳ cuối; nối PostDeathItemPort. | Không còn chờ công bố T108 hay API 004; vẫn cần B xác nhận đủ dữ liệu ngày giải phóng/kỳ, nguồn C/D và A đăng ký generator 010. |
| 6 | 008: T050 R-15/V008_95 và converge cấu trúc, glossary staff_import_batch; test trước và kiểm API không đổi. Mobile T016b/T022b/T030/T041b có thể bắt đầu phần trực tuyến. | E; tích hợp đủ nguồn bàn giao vẫn cần B/C/D. Nền mobile đã mở, không chờ mở giai đoạn. |
| 7 | 015: T001–T003 còn hiệu lực, API thống kê đổi ca và fake, rồi nền đổi ca/nghỉ; không làm task đã Bỏ theo Q-397. 017: T001/T004–T008, API/fake số dư và DepositStatusPort, tiếp T045 kiểm CFG-M11-10. | Sau nền 010; 017 còn phụ thuộc bản thật CostForBalancePort và A/012 quyền người thân. Thư viện chung cần A thêm khi thật sự thiếu. |
| 8 | Nghiệm thu 008 T043b/T049b với nghiệp vụ C/D thật; T044b đo SC bằng người dùng, chuyến/đồ gửi và demo hoàn chỉnh. | B/013, C/005/014, D/006/007 và nhóm nghiệm thu; không thay số đo bằng thời gian test. |

`frontend/scripts/gen-api.mjs` vẫn chưa đăng ký 010 trong TRANSLATED; nhờ A thêm trước khi cần client web 010. Không có quyền tự sửa file dùng chung với tư cách E. Việc 012 thuộc A, E không tiếp tục task 012.

## Kiểm môi trường — chỉ đọc

MySQL80 đang Running. Process của phiên không có DB_URL/DB_USERNAME/TEST_DB_*; backend/.env chỉ có DB_PASSWORD, DB_RETENTION_PASSWORD. Dùng tài khoản eldercare_app được kho mã quy định, truy vấn schema mặc định elder_care bị từ chối quyền; tài khoản đọc được eldercare_demo và eldercare_test. Chưa xác nhận cấu hình schema riêng trong IDE.

Đã đọc flyway_schema_history trên **eldercare_demo**: 60 migration thành công, phiên bản lớn nhất 009.03, chưa có cả 33 migration mới kéo. **31/33 thấp hơn 009.03** (các V000/V001/V003/V004/V005/V006/V007); hai V010 cao hơn. Schema demo còn thiếu cả migration từ trước mốc kéo, nên không coi danh sách mới dưới đây là toàn bộ phần chưa áp.

Theo “Ghi nhớ migration số nhỏ”: chạy một lần với `spring.flyway.out-of-order=true` hoặc dựng lại schema dev. Profile demo đã cấu hình out-of-order=true. Ví dụ lệnh từ backend khi đã nạp đủ DB_*:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo" "-Dspring-boot.run.arguments=--spring.flyway.out-of-order=true"
```

Lệnh chỉ được ghi để tham khảo, **không chạy trong phiên**; không đổi schema, quyền, mật khẩu hoặc cấu hình máy. Schema test không cần thao tác thủ công theo ghi nhớ mục 8.

Không thay đổi backend/pom.xml, frontend/package.json, mobile/pubspec.yaml hoặc hai lockfile trong khoảng cập nhật; không có lệnh cài lại bắt buộc vì thư viện mới. Khi cần dựng lại môi trường theo README: backend dùng Maven wrapper, frontend `npm ci`, mobile `flutter pub get`.

33 migration mới (chỉ thêm, không có migration cũ bị sửa trong khoảng này):

- `V000_16__event_publication_text.sql`
- `V000_17__parameter_q399.sql`
- `V001_06_2__file_data_group.sql`
- `V003_07__doctor_confirmation_requested_at.sql`
- `V004_01__price_version.sql`
- `V004_02__admission_record.sql`
- `V004_03__contract.sql`
- `V004_04__absence.sql`
- `V004_05__residency_ending.sql`
- `V004_06__parameter_q394_q395.sql`
- `V004_94__shared_service_care_level.sql`
- `V005_01_1__non_performance_reason_type.sql`
- `V005_01__care_foundation.sql`
- `V005_02__declare_data_groups.sql`
- `V005_100__care_result_reason_reference.sql`
- `V005_94__shared_day_care_attendance.sql`
- `V005_95__care_foundation_consolidation.sql`
- `V005_96__care_plan_revisions.sql`
- `V005_97__shift_care_tasks.sql`
- `V005_98__care_task_results.sql`
- `V005_99__care_checklist_carryover.sql`
- `V006_15__prn_correction_context.sql`
- `V006_95__table_consolidation.sql`
- `V007_01__vital_sign_record.sql`
- `V007_02_1__emergency_incident_type_catalog.sql`
- `V007_02__health_incident_catalogs.sql`
- `V007_03__alert.sql`
- `V007_04__vital_sign_offline_permission_flag.sql`
- `V007_05__monitoring_threshold_versions.sql`
- `V007_06__alert_monitoring_data_group.sql`
- `V007_07__alert_staff_assist_type.sql`
- `V010_01__costs.sql`
- `V010_02__cost_data_groups.sql`

## Ghi nhận cuối phiên

Chỉ tạo báo cáo này, thêm một dòng cuối docs/progress.md; xóa dòng chờ AssignmentIT đã được #105 xử lý và gỡ E khỏi dòng đồng bộ T110 đã kéo xong. Giữ các dòng cần review/nguồn thật/nghiệp vụ chưa xong. Không sửa mã chức năng, tasks/spec/nghiệp vụ, không commit/push/mở PR. Kiểm chứng tài liệu bằng git diff --check và kiểm phạm vi/đếm dòng báo cáo; kết quả test nêu trên lấy từ progress, không phải lượt chạy mới.

## Bổ sung sau lần fetch tiếp theo ngày 04/11/2026

origin/develop hiện ở 62fdb4f2, thêm 5 commit so với develop cục bộ 55807c24; PR #106 của A đã merge. Chỉ fetch và đọc remote, chưa pull/tích hợp thay đổi.

A đã xong 000 T108 (API bảng chung, bản cài, migration, fake), nền mobile, và qua #106 đã có FamilyRightsApi/FakeFamilyRights/bản cài FamilyRightsLookup, FamilyRelationshipApi, RepresentativePort, FamilyNotificationPort, EmergencyFamilyPort của 012. 012 mới xong nền và US1, chưa xong toàn feature; generator web vẫn chưa đăng ký 010. Lượt clean verify cuối của A ghi 1071 test đơn vị/kiến trúc xanh; 2601 IT có 2 error ở StaffImportTransportIT và FR_024_AllocationStatusParticipantIT, không phải toàn backend xanh.

Rà cả mã chưa commit ở các worktree E: 008-q262 đã đánh xong Q-262 và T050a chuyển cấu trúc/SQL; T050 R-15 vẫn mở. 008-my-shifts-mobile đã cài và đánh T016b, nhật ký ghi 77/77 Flutter xanh. 008-mobile-assignments đã có màn và test nhưng T022b vẫn mở chờ review/tích hợp/nguồn thật. Các bản này chưa vào develop và nhiều bản còn namespace cũ; cần đối chiếu/tích hợp trên nền mới, không viết lại từ đầu. Số T051 ở worktree cũ là Q-262, trùng số FR-057 trên develop, phải đối chiếu nội dung trước khi ghép checkbox.

Trên develop đã merge: 008 có 49 dòng task [x], 8 dòng [ ]; 010 có 4 [x] (T001/T003/T004/T005a), 35 [ ] và chưa xong Phase 1–2. Không còn lý do chờ A/000 T108 để làm 010 T005 hoặc 008 T050. Tiếp theo ưu tiên rà phần WIP 008 đã có, cài ShiftStaffPort thật/chốt claim tồn để gỡ chặn C, thống nhất quantity/newQuantity với C, rồi hoàn tất 010 T002/T005/T006 → T007/T008. Phần generator 010 vẫn nhờ A. Không chạy lại test hoặc sửa mã trong lượt kiểm này.
