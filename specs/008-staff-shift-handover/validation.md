# Kiểm chứng và phần chờ nghiệm thu 008

Chốt ngày 2026-10-26, Phase 9 trên `impl/008-staff-shift-handover`, HEAD đầu phiên `668fe1d`. Tài liệu này ghi bằng chứng đã chạy và việc còn thiếu; không thay đổi spec/nghiệp vụ. Tên lớp test dưới đây là tên thật trong `backend/src/test/java/com/eldercare/backend/staffing/`.

## 1. Kết quả đã chạy

| Lệnh / lượt kiểm | Kết quả |
| --- | --- |
| Test trước sửa lịch sử, cùng ArchTest | `StaffModuleIntegrationIT` đỏ hai assertion: người phụ trách tại giờ cũ trả người mới; lịch sử phạm vi người cũ rỗng. Ba test `AlertStaffPortIT` xanh. Lỗi fixture thiếu `requires_handover` đã sửa trước khi ghi nhận hai lỗi hành vi này |
| Test sau sửa + hồi quy liên quan | 28 lượt test kiến trúc và 84 IT xanh: `StaffModuleIntegrationIT` (6), `AlertStaffPortIT` (3), `M09RecordProtectionIT` (2), `AssignmentIT` (31), `HandoverIT` (25), `PostPublicationShiftIT` (17) |
| Bảo vệ lịch sử chuyển bộ phận | Hai assertion đỏ khi bổ sung staff_department vào danh sách; sau V008_08: 28 lượt kiến trúc + 21 IT xanh (`M09RecordProtectionIT`, `DepartmentCommandsIT`, `DepartmentDutyIT`, `StaffSchemaIT`), gồm chuyển bộ phận và dọn fixture qua tài khoản lưu giữ |
| `spotless:apply`, `clean verify` chạy nền, JDK 25/MySQL 8.0 | **336 lượt đơn vị/kiến trúc + 1106 IT = 1442**, không lỗi/thất bại/bỏ qua, **08:00**, BUILD SUCCESS. Có `ApiContractIT`, Modulith và mọi ArchTest |
| `npm test`, `npm run lint`, `npm run build` | **271 test/41 file**, lint/build qua. Vite vẫn báo cảnh báo kích thước chunk; không lỗi build. Lượt đầu bị sandbox chặn `spawn EPERM`; lượt chạy ngoài sandbox đã qua, không đổi dependencies |
| `flutter test --no-pub` | **12 test qua**, thuộc phần người cao tuổi 002 hiện có. Chưa có bài kiểm mobile nhân viên 008; kết quả này không nghiệm thu T030/FR-060 trên app |

Không sửa giao diện nên không chụp màn hình. Không sửa API HTTP, chữ ký SPI, thư viện, migration cũ hay test/assertion của A/B/C/D. Thêm migration riêng **V008_08__protect_staff_department_history.sql** sau khi hai assertion đỏ chứng minh thiếu trigger lịch sử chuyển bộ phận: chỉ cho đóng ends_at một lần, không viết lại bản gốc/xóa ngoài tài khoản lưu giữ; thu hẹp quyền UPDATE của ứng dụng về ends_at. Hai fixture E (`AssignmentIT`, `HandoverIT`) bổ sung lịch sử phụ trách như lệnh công bố thật; mọi assertion giữ nguyên. Bảy file Flutter thay đổi sẵn đã đối chiếu SHA-256 với bản đầu phiên, nội dung không đổi. Không commit/push/merge/đổi nhánh.

## 2. Phạm vi phần E và phụ thuộc

- **T042:** `ApiContractIT` đã đăng ký 008 với pending/pendingResponses rỗng và checkOperations=true. `M09RecordProtectionIT` bổ sung phần JDBC mà test 000 chỉ quét entity JPA: mười một bảng có BEFORE UPDATE/DELETE; quản trị cũng bị chặn sửa/xóa bản đã chốt. Lịch sử được đóng một lần vẫn giữ quy tắc hiện có, không biến thành bất biến khi còn mở.
- **T043:** tám SPI StaffProfileApi, LicenseApi, PracticeScopeCatalogApi, AssignmentApi, DutyApi, CareShiftPort, NursingShiftPort, AlertStaffPort lấy bean M09 thật; ScopeApi thật của A kiểm lịch sử trước/sau đổi phụ trách, vắng và biên CFG. Test cô lập dùng fake của chủ module khác vẫn giữ nguyên. **T043b** chờ C/D có nghiệp vụ/bộ test 005–007 để chạy tích hợp nguồn thật; A review kết nối 002.
- **T049/K1:** onShift/floorDuringShift/floorsWithShift đã có bản cài. Ba test mới kiểm biên đầu/cuối ca, vắng, Nháp/hủy, tầng vẫn có ca dù một thành viên vắng và giờ hành chính đọc CFG. Q-332 chỉ coi Quản lý viện/Hành chính/Kế toán là vai trò không xếp ca; ví dụ Bếp trong K1 cũ đã sửa thành Hành chính. **T049b** chờ D có H1–H9 của US13 007; chưa có test nghiệp vụ đó trên nhánh này.
- **T044b:** chưa đủ nguồn/dữ liệu demo để chạy mọi Q đầu–cuối với module thật; không có endpoint công tắc fake trên ứng dụng. Không tự tạo bản cài giả trả rỗng hoặc bịa kết quả demo/SC.
- Mobile nhân viên đã mở 2026-10-27: T016b, T022b, T030, T041b làm được phần trực tuyến/test widget; đăng nhập, điều hướng và client chung còn chờ A theo mobile/README.md mục 6. T043b/T044b/T049b giữ `[ ]`; không kết luận toàn spec đã nghiệm thu.

## 3. Truy vết FR chung

DBR/BR ở [data-model mục 11](data-model.md#11-truy-vết-dbr-br-và-fr). Bảng này chỉ rõ task/test và phần chưa nghiệm thu, không dùng một tên test làm bằng chứng hoàn tất nguồn bên ngoài.

| FR | Task và bằng chứng |
| --- | --- |
| FR-048 quyền | T006/T019–T022/T027/T040; `AssignmentIT`, `HandoverIT`, `PostPublicationShiftIT`, `StaffModuleIntegrationIT` dùng quyền/phạm vi thật, kiểm ngoài phạm vi và nhiệm vụ |
| FR-049 giao tiếp | T007/T015/T021/T026/T037/T040/T043/T049; `StaffSpiIT`, `ShiftQueriesIT`, `AssignmentIT`, `StaffModuleIntegrationIT`, `StaffStatisticsIT`, `AlertStaffPortIT`; nguồn C/D và hồi quy liên module còn T043b/T049b |
| FR-050 dữ liệu sức khỏe | T027/T029; `HandoverIT.FR_047_caregiverOnlySeesAssignedResidentItemsAndAssessmentsPlusGeneral`, test che cả chuỗi đính chính; nguồn phản hồi/ý kiến theo cùng quyền |
| FR-051 liên kết mục, nhắc nhập theo người, thành viên bổ sung | T027/T029/T040/T041; hợp đồng mục bàn giao/HandoverService kiểm đối tượng; form nhận định nhắc không ghi sức khỏe riêng ở nhận định chung; quyền xem theo phân công thật. Ca demo thành viên được bổ sung đọc bàn giao đã nhận còn đối chiếu tại T044b |
| FR-052 nhóm trường nhân viên | T006/T009/T039; `StaffFieldVisibilityIT`, `M09GovernanceIT` (phân loại/tải scan), `PostPublicationShiftIT` |
| FR-053 lý do riêng tư | T039/T040; `PostPublicationShiftIT` kiểm người ghi/Trưởng tầng/QLV và lý do không lan vào phân công/thông báo; `AbsenceSourceContractsTest` |
| FR-054 lịch cá nhân | T016/T021; `ShiftScheduleIT.FR_054_publicationNotificationContainsOnlyEachRecipientsOwnShifts`, `ShiftHttpIT`, `AssignmentHttpIT` |
| FR-055 nhật ký/bản công bố | T013/T019/T027/T040/T042; `ShiftScheduleIT.FR_055_publicationStoresSnapshotAndAuditButDraftEditsHaveNoPerEditAudit`, `M09RecordProtectionIT`; các test lệnh kiểm audit và rollback |
| FR-056 căn cứ nhiệm vụ | T037/T041; `PostPublicationShiftIT.FR_056_dutyHistoryIsManagerOnlyFilteredByFloorAndContainsNoPrivateReason`, `StaffStatisticsIT` (phân trang lịch sử/căn cứ); không nhật ký lần xem bàn giao |
| FR-057 bảo vệ, đính chính, lưu giữ | T009/T021/T028/T042/T051; `HandoverSchemaIT`, `HandoverIT` (đính chính/lưu giữ cả chuỗi), `M09RecordProtectionIT`, `M09GovernanceIT`, `ShiftGovernanceIT`, `AssignmentGovernanceIT`. T050 bổ sung `HandoverCorrectionNotificationsTest`, `HandoverNotificationsTest` và hai phương thức `HandoverIT.FR_057_*`: người xác nhận qua tài khoản 002, Người phụ trách ca hiện tại khi ca xác nhận đã kết thúc, không lặp người giao kiêm người nhận. Ba test đơn vị đỏ trước sửa rồi xanh; chốt 2026-10-28 với ba đơn vị và toàn bộ 27 HandoverIT xanh trên MySQL cấu hình của người dùng, pool mặc định 5. Lỗi tài nguyên khi kiểm toàn kho và fixture lưu giữ còn mở ở team-plan, không lấy lượt chẩn đoán pool 20 thay nghiệm thu cấu hình chuẩn. Một số tên test lưu giữ cũ bắt đầu FR_051; nội dung đối chiếu yêu cầu lưu giữ FR-057 |
| FR-058 mức thông báo | T008/T013/T027/T033/T037/T040; `CredentialLifecycleIT`, `StaffNotificationsTest`, `StaffingRatioWarningIT`, `PostPublicationNotificationsTest`, `HandoverIT` |
| FR-059 nội dung kênh ngoài | T039; `PostPublicationNotificationsTest`, `PostPublicationShiftIT` kiểm không lý do riêng; `HandoverEventSerializationTest` kiểm đọc lại khuôn thông báo. A/009 còn review và kiểm kênh gửi thật |
| FR-060 trực tuyến/ngoại tuyến | T027/T028 đã có lệnh HTTP; **T030 và T044b còn mở** cho bộ nhớ bàn giao theo phạm vi ca, xóa khi hết quyền, không xếp hàng lệnh bàn giao trên app. Không dùng 12 test resident để đánh đạt phần này |

## 4. Đối chiếu quickstart

Đây là ánh xạ bài kiểm tự động với kịch bản logic, không phải biên bản đã thao tác hết profile demo.

| Kịch bản | Bằng chứng tự động / phần chờ |
| --- | --- |
| Q1–Q6 | ShiftSchemaIT, ShiftScheduleIT, ShiftHttpIT: 24 giờ, chồng ca, cảnh báo, công bố, khóa lệnh sau công bố |
| Q7 | PostPublicationShiftIT: chuyển người giữ nội dung/tác giả; StaffModuleIntegrationIT: truy vấn theo mốc lịch sử |
| Q8–Q14 | AssignmentIT, CredentialValidityTest, CredentialLifecycleIT: công bố lại kiểm chứng chỉ, ưu tiên đối tượng, đồng thời, sao chép, thu hồi |
| Q15–Q19 | CredentialLifecycleIT, StaffEmploymentIT, DepartmentCommandsIT, DepartmentDutyIT: cảnh báo, nghỉ/nhận lại, thay phụ trách, rollback |
| Q20–Q21 | StaffingRatioCalculatorTest, StaffingRatioIT, StaffingRatioWarningIT: bảng trọng số, vắng, cảnh báo không chặn công bố |
| Q22–Q25 | PostPublicationShiftIT, AssignmentIT: chỉ đúng ca, khoảng ghi vắng, người thay, giao trưởng tầng, hủy trước giờ |
| Q26 | AssignmentIT.FR_024_floorManagerAbsenceAndSubstitutionAreEvaluatedAtTheCurrentTime, HandoverIT; kiểm demo quyền thay Trưởng tầng còn T044b |
| Q27–Q37 | HandoverIT, HandoverSchemaIT, CriticalHandoverItemTest, NextHandoverShiftTest, ShiftLifecycleIT: nguồn/chốt/ý kiến/xác nhận/rollback/nhắc/trigger/đính chính |
| Q38 | Chờ T030/T044b; chưa có app bàn giao để kiểm mất kết nối |
| Q39 | T028 kiểm quyền theo phạm vi; bước gửi lại sau hết phạm vi trên profile demo còn T044b, chưa ghi đạt toàn kịch bản |
| Q40–Q41 | PostPublicationShiftIT, PostPublicationNotificationsTest, HandoverIT: riêng tư và chăm sóc chỉ xem người trong phạm vi |
| Q42 | M09JobCatchUpIT, ShiftLifecycleIT: chạy bù/chuyển trạng thái, không sinh nghiệp vụ thay actor |
| I1–I7 | StaffImportIT, StaffImportRowValidatorTest, StaffImportTransportIT; 500 dòng kiểm riêng từng bước dưới 10 giây |
| K1 | AlertStaffPortIT, SPI thật; H1–H9 đầu–cuối của 007 chờ T049b |

## 5. SC và cách lưu số đo thực tế

| SC | Bằng chứng / trạng thái |
| --- | --- |
| SC-001 | HandoverIT (nguồn/nhắc/thiếu ghi chú), ShiftLifecycleIT; còn xác nhận danh sách nguồn thật qua T043b/T044b |
| SC-002 | **Chưa đo người dùng**: ít nhất 10 lần Điều dưỡng đã hướng dẫn, tầng khoảng 30 người, khoảng 20 mục/5 mục nghiêm trọng; từ lập đến Đã lập, trung vị ≤10 phút |
| SC-003 | **Chưa có vận hành thử một tháng**: ≥90% xác nhận trong CFG-M09-05, 100% quá hạn được nhắc; lấy mọi bàn giao cần bàn giao của tháng, trường hợp Trưởng tầng xác nhận không ca sau tính từ kết ca |
| SC-004 | HandoverIT kiểm callback/rollback nguyên tử; công việc/cảnh báo thật còn C/D kiểm T043b |
| SC-005 | AssignmentIT chạy 200 lượt kiểm điều kiện chứng chỉ/phân công, ShiftScheduleIT kiểm chồng ca/giờ liên tục/đồng thời; đây là kiểm tự động, không thay thử thao tác người dùng |
| SC-006 | CredentialValidityTest, CredentialLifecycleIT, ShiftScheduleIT, PostPublicationShiftIT, HandoverIT, M09JobCatchUpIT dùng Clock/CFG |
| SC-007 | StaffingRatioCalculatorTest, StaffingRatioIT, StaffingRatioWarningIT so bảng tính và nhắc; dữ liệu vị trí/bán trú thật chờ B/C |
| SC-008 | StaffEmploymentIT kiểm khóa/rút ca/kết thúc/rollback; công việc nguồn thật còn C kiểm T043b |
| SC-009 | HandoverSchemaIT, M09RecordProtectionIT, HandoverIT (đính chính giữ gốc) |
| SC-010 | **Chưa đo người dùng**: ≥10 lần Trưởng tầng đã hướng dẫn, ca khoảng 30 người/6 nhân viên/tối đa 3 phân công không sao chép được; từ mở ca đích đến lưu cuối, trung vị ≤5 phút |
| SC-011 | AssignmentIT, ShiftScheduleIT: phân công chính/giao trưởng tầng/chồng ca đồng thời |
| SC-012 | Test riêng tư/nhật ký như FR-053/055/056/059; A/009 kiểm kênh gửi thật tại T044b |
| SC-013 | StaffImportIT.SC_013_fiveHundredRowsPreviewAndConfirmEachWithinTenSeconds đã xanh; mỗi bước có assertion <10 giây và kiểm 500 hồ sơ |

Mẫu biên bản SC-002/010: `Mã SC | Lần | Vai trò/người thử đã hướng dẫn | Mã ca | Bắt đầu | Kết thúc | Số phút | Điều kiện dữ liệu | Lỗi gặp`. Ghi mười dòng thật; sắp thời gian tăng dần, trung vị của mười lần là trung bình phần tử 5 và 6. Thời gian chạy JUnit/CI không phải thời gian người dùng thao tác.

SC-003 ghi tháng, tổng bàn giao thuộc mẫu, số xác nhận đúng hạn, số quá hạn, số quá hạn đã nhắc và cách kiểm từng mẫu. Không đưa ca không yêu cầu bàn giao vào mẫu; không dùng ngoại lệ Trưởng tầng xác nhận khi không có ca sau vì ngoại lệ này đã bị bỏ theo quyết định 2026-11-05 (FR-038); dữ liệu lịch sử trước thay đổi cần được nhận diện riêng, không dùng để nghiệm thu quyền mới. Không có dữ liệu thì để **Chưa đo**, không điền số minh họa thành kết quả.

## Cập nhật 2026-11-05 — T050 và T016b

- Người dùng đã cho phép kiểm thử sau yêu cầu clean ban đầu. T050: 117 unit/architecture +333 integration của staffing và consumer/hợp đồng liên quan xanh, 0 failure/error/skip; không chạy lifecycle verify toàn kho. Báo cáo XML giữ tại .worktrees/.snapshots/008-retention-20261105/reports-staffing-green.
- T016b: mã Ca của tôi từ nhánh cũ đã ghép đúng client hiện tại và StaffRoot; toàn app 179 test xanh, analyze sạch, định dạng qua. Bộ 27 ca riêng kiểm tháng/giờ Việt Nam, ca qua ngày, phản hồi trả chậm, không lộ liên hệ, lỗi/mất quyền, menu Thêm và chữ 200% sáng/tối. Mã nguồn nhánh cũ giữ nguyên; pubspec/lock không đổi; không đổi Developer Mode của Windows.
- Các câu chờ mở mobile/nền A trong ghi chú lịch sử không còn chặn T016b. T022b/T030/T041b và nghiệm thu nguồn thật T043b/T049b/T044b vẫn mở, không lấy widget test thay bằng chứng thiết bị hoặc nguồn thật.
- Người dùng xác nhận chưa có biên bản đo 10 lần SC-002/SC-010 hoặc vận hành một tháng SC-003. Các mục đó vẫn Chưa đo, không dùng số liệu giả/timing CI thay thế.
- Hai điểm 008 cần chủ dự án chốt vẫn chưa có quyết định: mốc kiểm người nhận việc tồn; đối tượng/thời điểm hiện/tắt hai cờ checklist bàn giao. Đã giải thích luồng/vai trò và ví dụ, không tự chọn hoặc đề xuất quy tắc mới.

## Quyền nhận bàn giao theo lịch ca sau — chốt và kiểm ngày 2026-11-05

Người dùng chốt người xác nhận bắt buộc thuộc đúng ca sau đã xếp, kể cả Trưởng tầng. Nguồn nghiệp vụ13.5/24.2, BF02/BF13 và spec008 đã đồng bộ; chưa đổi mốc kiểm việc tồn005 hoặc chốt thay vòng đời hai cờ checklist. Không có endpoint/schema/DTO/sự kiện/API module mới; không có migration hoặc phụ thuộc mới.

- Trước sửa receiver: lượt FR038 chạy7 test, có5 failure bắt đúng ngoại lệ ngoài lịch/không ca sau; một error do test thành viên thường gọi allowedCommands ngoài quyền xem bản bàn giao ca trước. Test đó đã sửa để đòi NotFound ở truy vấn và PermissionDenied ở lệnh, không mở thêm quyền xem. XML đỏ giữ nguyên ở red-reports.
- Sau sửa: 47 unit/arch thuộc8 lớp CriticalHandoverItemTest, NextHandoverShiftTest, HandoverNotificationsTest, HandoverCorrectionNotificationsTest, HandoverEventSerializationTest, LayerArchTest, ModuleBoundaryTest, TimeArchTest đều xanh.
- 67 IT thuộc6 lớp HandoverIT, HandoverEmbeddedItemsIT, HandoverSchemaIT, PostPublicationShiftIT, StaffModuleIntegrationIT và ApiContractIT xanh; 0 failure/error/skip. HandoverIT có7 lượt FR038 cho không có ca sau, không có tên, vắng, rút do nghỉ việc, nghỉ được duyệt, Trưởng tầng có tên hợp lệ và thành viên thường không có nhiệm vụ xác nhận; các luồng xác nhận/ý kiến/rollback cũ được chạy lại.
- Lệnh: Maven spotless:apply chỉ hai file HandoverService.java/HandoverIT.java; Maven test failsafe:integration-test với danh sách lớp ở trên, kiểm XML failsafe-summary để không bỏ sót failure của integration-test; không chạy lifecycle verify toàn kho. git diff --check qua. Không chạy lại Flutter/web vì không sửa mã giao diện hoặc hình dạng HTTP.

Chứng cứ: .worktrees/.snapshots/008-handover-next-shift-20261105/{red-reports,green-reports,result.json,spotless.log,green.log}. Lượt đầu bị sandbox chặn Maven cache; lượt Maven được cấp quyền đã chạy test thật. Không cộng kết quả lượt này vào tổng117+333 của checkpoint trước như số ca duy nhất. T043c vẫn mở, checklist53/60; không dùng các test này thay số đo người dùng hoặc vận hành một tháng của T044b.

### Bổ sung quyết định giờ nhận việc tồn (2026-11-05)

Người dùng đã chốt kiểm người nhận việc tồn tại lúc máy chủ xử lý lệnh nhận (ví dụ14:10), giữ giờ dự kiến cũ (13:50), trạng thái và lịch sử. Đã cập nhật nghiệp vụ13.5/24.2 trước FR044a/tiêu chí chấp nhận008, ghi chú hợp đồng và bàn giao C trong carryover-claim-decision-2026-11-05.md. Team-plan chuyển chờ quyết định sang C cài lệnh/allowedCommands, E cài nguồn thật; không xóa phần chưa làm.

Lượt này chỉ sửa tài liệu; kiểm git diff --check, chỉ số task vẫn53/60, không chạy lại test mã và không dùng kết quả47+67 của quyền xác nhận để nghiệm thu nhận việc tồn005. Source/spec/test005 giữ nguyên. Không thêm API/migration/thư viện; chưa chốt đủ vòng đời hai cờ checklist, không đánh xong T043c hoặc toàn008.

## T043c đã kiểm xanh — nguồn ShiftStaffPort thật (2026-11-05)

- TDD đỏ5lượt:3failure do test chọn fake005 trả mặc định thay nguồn thật,2error gồm thiếu bean production và fixture đổi ca đã Hủy. Test mới chọn rõ careStaffLookup; sửa thứ tự fixture để không sửa ca Hủy; không đổi fake hoặc test của C.
- Nguồn thật đủ12 hàm: ca/published/range/containing/lead/members/onShift/qualified/hasQualifiedStaff, phân công PRIMARY/SUPPORT, Điều dưỡng phụ trách, trạng thái xác nhận và hai cờ. Checklist kiểm cửa sổ lập, không tự tạo bản nháp, chuỗi DRAFT→PREPARED→HAS_COMMENTS→PREPARED→CONFIRMED, đúng người/ca sau, vắng và hết cửa sổ xử lý. HandoverAccess dùng chung với lệnh, giữ hồi quy quyền/rollback/điều chỉnh ca.
- 51unit/arch thuộc10lớp: CriticalHandoverItemTest,NextHandoverShiftTest,HandoverNotificationsTest,HandoverCorrectionNotificationsTest,HandoverEventSerializationTest,LayerArchTest,ModuleBoundaryTest,TimeArchTest,SolidArchTest,ApiNamedInterfaceArchTest; đều xanh.
- 123IT thuộc10lớp: ApiContractIT(1),AssignmentIT(32),CredentialLifecycleIT(11),DutyDirectoryIT(7),HandoverEmbeddedItemsIT(2) đã xanh trước khi tiến trình bị ngắt; HandoverIT,HandoverSchemaIT,PostPublicationShiftIT,StaffModuleIntegrationIT,ShiftLifecycleIT chạy tiếp70IT xanh. Tổng0failure/error/skip, không đếm trùng.
- Đính chính định dạng: các lệnh Spotless dùng đường dẫn tương đối trước đó không khớp bộ lọc regex đường dẫn tuyệt đối trên Windows. Đã chạy spotless:apply và spotless:check thực sự trên10file E:8file T043c vàM09Retention/StaffingRatioSchemaIT củaT050; log báo10file,9được định dạng,1đã sạch;check xanh. Không chạy verify toàn kho. Không cần chạy lại web/Flutter ở bước chỉ Java này; nguồn đã biên dịch từ cùng thay đổi logic trước lần chỉ định dạng cuối.

Chứng cứ: .worktrees/.snapshots/008-handover-next-shift-20261105/staff-port-green-reports/{unit,integration},staff-port-result.json,staff-port-real-spotless.log,staff-port-resume-result.json,staff-port-resume.log; XML đỏ ở staff-port-red-reports. Đánh xT043c,54/60task. Còn ba task mobile/T043b/T049b/T044b. Các nguồn B/C/D dùng fake trong test vẫn là giới hạn; không dùng kết quả này để nghiệm thu năm callback005, mốc claim005 hoặc số đo vận hành.

## T041b — app ghi vắng ca trực tuyến(2026-11-05)

Test đỏ trước cài vì thiếu ShiftAbsenceSheet;7test nghiệp vụ gồm chọn người/lý do,người thay/header/payload,ngoài cửa sổ backend,lỗi mạng giữ nội dung+khóa,403 xóa nội dung,không có lệnh thì không form,chặn gửi hai lần. Bổ sung2test chữ200% sáng/tối và1test từ Ca của tôi tới lệnh rồi tải lại trạng thái backend. Full flutter test--no-pub:189pass;flutter analyze--no-pub:No issues;dart format4file. Không sửa common hoặc pubspec/lock; hai hash vẫn như checkpointT016b. Diff--check qua. Không chạy lại Maven vì bước này chỉ Dart/tài liệu.

Người dùng chốt trực tuyến trước, không tạo hàng đợi ngoại tuyến hoặc mốc đồng bộ muộn. Chưa xác nhận kết quả máy chủ thì giữ nội dung/thử lại với cùng khóa,không báo thành công giả. TaskT041b được x,55/60; không đánh xT044b hoặc coi widgettest là thử trên thiết bị thật. Log mobile-absence-red.log,mobile-absence-green.log,mobile-absence-full-test.log,mobile-absence-analyze.log,mobile-absence-format.log tại snapshot008-handover-next-shift-20261105.

## Kiểm ghép T054/T055, T022b và phần trực tuyến T030 —2026-11-05

- Chứng cứ mới tại .worktrees/.snapshots/008-mobile-assignments-20261105; không cộng với checkpoint cũ như số test duy nhất. Red hợp lệ:28IT,26failure/0error vì endpoint chưa có; red-fixture.log chỉ là lỗi chuyển fixture, không lấy làm chứng cứ nghiệp vụ. mobile-red và mobile-navigation-red chứng minh thiếu mã/mục điều hướng; mobile-labels-red kiểm tên nguồn.
- Maven: test chọn AssignmentDetailSerializationTest,AssignmentReadSourcesTest,LayerArchTest,ModuleBoundaryTest,TimeArchTest,SolidArchTest và failsafe:integration-test chọn AssignmentDraftUpdateIT,AssignmentDraftPermissionIT,AssignmentDraftAtomicityIT,AssignmentResolutionHttpIT,AssignmentFixtureIsolationIT,AssignmentIT,ApiContractIT. Fresh XML cuối:19unit/arch+64IT,0failure/error/skip, mavenExit0. Includes If-Match/khóa/lặp/UPDATEkhôngCREATE/rollback/công bố đồng thời, vị trí lịch sử/vắng/bán trú/nguồn thiếu/GET không ghi và hợp đồng. green-result.json cùng green-surefire/green-failsafe lưu XML.
- Spotless apply/check dùng regex đường dẫn tuyệt đối theo16tên file E; log thực báo16file,8được định dạng/8sạch,check qua. Không dùng bộ lọc0file hoặc exit Maven đơn lẻ để kết luận IT.
- npm run gen:api chuẩn đã chạy, chỉ api008.js có diff; dùng node_modules có sẵn qua junction bị ignore, không cài/thay dependency. npm test:472/81file;lint/build qua. Vite cần tiến trình con nên lượt sandbox EPERM không tính là test sản phẩm đỏ. Sau chỉnh mã cảnh báo trong fixture E,3test AssignmentPanel chạy lại xanh. Không sửa test của A/B/C/D.
- Toàn app sau ghép phân công và bàn giao trực tuyến:227test xanh. Test riêng bàn giao10ca và4ca menu theo quyền; không giả lập nghiệm thu offline/xóa hết phạm vi. Test giữ lỗi nguồn,403 xóa, ý kiến/khóa thử lại, lệnh lập theo allowedCommands, hoàn tất bị409, xác nhận có người bấm và tải lại trạng thái, gửi lại đúng replies, chữ200% sáng/tối. Log final-mobile-test,final-mobile-analyze,handover-*; thứ tự import/ngoặc lint được sửa, kết quả analyzer cuối cần No issues found trước kết luận.

T054/T055 được[x],57/62task. T022b/T030/T043b/T049b/T044b giữ mở; không nói toàn008 hoàn tất hoặc hệ thống chạy trơn tru từ test fake. Client ETag/hạn phạm vi cần A; callback005 cần C; US13 cần D; người dùng chưa có kết quả nghiệm thu thật.
