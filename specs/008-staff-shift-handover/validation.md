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
- Mobile nhân viên vẫn chờ mở: T016b, T022b, T030, T041b. T043b/T044b/T049b giữ `[ ]`; không kết luận toàn spec đã nghiệm thu.

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
| FR-057 bảo vệ, đính chính, lưu giữ | T009/T021/T028/T042; `HandoverSchemaIT`, `HandoverIT` (đính chính/lưu giữ cả chuỗi), `M09RecordProtectionIT`, `M09GovernanceIT`, `ShiftGovernanceIT`, `AssignmentGovernanceIT`. Một số tên test lưu giữ cũ bắt đầu FR_051; nội dung đối chiếu yêu cầu lưu giữ FR-057 |
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

SC-003 ghi tháng, tổng bàn giao thuộc mẫu, số xác nhận đúng hạn, số quá hạn, số quá hạn đã nhắc và cách kiểm từng mẫu. Không đưa ca không yêu cầu bàn giao vào mẫu; ghi riêng trường hợp Trưởng tầng xác nhận không có ca sau. Không có dữ liệu thì để **Chưa đo**, không điền số minh họa thành kết quả.
