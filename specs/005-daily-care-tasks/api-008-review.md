# Review API 005–008 — 2026-10-30

Phạm vi: lượt 1 của việc review thành viên C, trên checkout `feature/005-daily-care-and-hand-over` đang ghép develop `d2bd7de0`. Đối chiếu contracts 005 mục 1.2, 1.4, 1.5; contracts 008 mục 2.4, 2.5, 2.6, 2.8; mã API, DTO, fake chung và consumer hiện tại. Chưa nghiệm thu story 005, chưa chốt merge; không sửa mã hoặc test của E.

## 1. Kết quả đối chiếu

| API / kiểu | Hợp đồng và consumer | Fake chung | Bản thật và phần còn lại |
| --- | --- | --- | --- |
| `TaskTypeApi.requirements` / `TaskRequirements` | Consumer `AssignmentSources` giữ vai trò và tập chứng chỉ; DTO sao chép tập đầu vào | `FakeCareRequirements.require` đặt dữ liệu, thiếu yêu cầu báo lỗi; `FakeM04ApiConfig` có proxy API riêng | Chưa có bản cài story; T007/T064 phải dùng danh mục chung trước khi nối thật |
| `CarePlanApi` | Giữ đủ `effectiveOn`, `waterTargetMl`, `hasEffectivePlan`, `currentTaskTypes`; 008 đọc loại công việc hiện hành | Fake consumer giữ `plan/currentTaskTypes`; ba truy vấn mới báo thiếu fixture. Proxy API cho phép đặt đủ đối số | Chưa có bản cài kế hoạch chăm sóc; không xem fake là nguồn thật |
| `ShiftTaskApi.onAbsence` | Đúng `(staffId, shiftId)`, MANDATORY; `PostPublicationService` gọi trong transaction lệnh vắng, không nuốt lỗi nguồn | Hook `onAbsent` giữ nguyên; phương thức callback có MANDATORY | Còn cài chuyển công việc đúng ca và phát sự kiện nghiệp vụ 005 |
| `ShiftTaskApi.onResignation` | Đúng `(staffId, effectiveAt)`, MANDATORY; `StaffEmploymentEffects.apply` cùng transaction và giữ lỗi nguồn | Hook `onResign` giữ nguyên; phương thức callback có MANDATORY | Còn cài chuyển công việc còn mở/tương lai thành chung |
| `ShiftTaskApi.onApprovedLeave` | Giữ `(staffId, Set<Long> shiftIds)` và MANDATORY; chưa thuộc luồng 008 được nghiệm thu trong lượt này | Fake consumer báo thiếu fixture; proxy API riêng cho phép cấu hình | Chưa kiểm luồng 015 hoặc cài tác động nghỉ có duyệt |
| `ShiftTaskApi.onAssignmentChanged/onAssignmentEnded` | `AssignmentSources` gọi đúng đối số; lệnh caller giữ transaction | Fake hỗ trợ lỗi kết thúc để kiểm rollback | Còn bản cài cập nhật công việc của story 005 |
| `ShiftTaskApi.onHandoverConfirmed` / `HandoverTransferResult` | Giữ `Long nextShiftId` nullable và kết quả hai danh sách. `HandoverService.confirm` truyền null khi thiếu ca sau, không đặt id thay thế | Fake consumer và proxy nhận null; DTO sao chép danh sách | Còn cài đóng/chuyển công việc theo FR-047/047a; test với fake chưa chứng minh xử lý công việc thật |
| `TaskHandoverApi` | 008 gọi `unclosed`, `currentStatus`, `recordedInHandover`; thêm mục bàn giao gọi callback trong transaction | `FakeCareRequirements` cung cấp nguồn/trạng thái và callback MANDATORY | Chưa có nguồn công việc thật cho bàn giao |

`ShiftTaskApi` gắn MANDATORY ở interface; fake consumer khai MANDATORY trên từng callback, còn các hàm dựng fixture được gọi ngoài transaction. `FakeM04Config` chứa SPI và fake consumer; `FakeM04ApiConfig` chứa proxy API riêng có Primary. Test ghép xác nhận phân giải bean khi import đồng thời. Đây là hai vai trò fixture hiện có, không tạo thêm fake.

## 2. Điểm cần giữ khi triển khai tiếp

- `TaskHandoverApi` (008 đang dùng) và `HandoverTaskApi` (khai báo Phase 2 của 005) đang song song, dùng DTO và tên truy vấn khác nhau. Lượt review giữ nguyên hai hợp đồng đã có; khi cài nguồn thật phải phục vụ cả hai hoặc trình duyệt phương án thống nhất API. Không tự bỏ interface, đổi consumer hoặc suy rằng proxy của một interface cung cấp interface kia.
- Không tìm thấy lớp production cài `TaskTypeApi`, `CarePlanApi`, `ShiftTaskApi`, `TaskHandoverApi` hoặc `HandoverTaskApi`. Các dòng chờ nguồn thật ở team-plan phải giữ; không đánh hoàn tất T003 hoặc story chỉ vì test consumer với fake xanh.
- Phần review của C cho API care có thể ghi nhận riêng; phần 003, D, E, A và nguồn 015 chưa nằm trong kết luận này. Không xóa toàn bộ dòng chờ nhiều chủ.
- Còn chờ A/E review ghép; A giữ việc đăng ký route T007a trong `ApiContractIT` và review đăng ký Care job. Không sửa file chung A trong lượt này.

## 3. Xác minh

JDK 25 dùng riêng trong process (`E:/JAVA/JDK_GE/INS_25`), không đổi thiết lập máy. Lượt sandbox đầu không chạy được vì khóa cache Maven; lỗi môi trường này không tính là test nghiệp vụ đỏ.

Lệnh đơn vị từ `backend/`:

```powershell
.\mvnw.cmd "-Dtest=T003_PublicCareApiTest,T003_FakePublicApiTest,T003_MergedApiCompatibilityTest,AssignmentSourcesTest,StaffingSourceContractsTest,ShiftPortFakesTest,AbsenceSourceContractsTest,NextHandoverShiftTest" test
```

Kết quả: exit 0, 19 test, 0 failure/error/skip. Log: `C:/Users/nguye/AppData/Local/Temp/eldercare-c-api-review-unit-20261030.log`.

Lệnh tích hợp (nạp `TEST_DB_*`, `DB_PASSWORD`, `DB_RETENTION_PASSWORD` từ `backend/.env` vào process, không in giá trị):

```powershell
.\mvnw.cmd test-compile failsafe:integration-test failsafe:verify "-Dit.test=AssignmentIT,HandoverIT,StaffEmploymentIT,PostPublicationShiftIT,PostPublicationPortsIT"
```

Kết quả tích hợp: exit 0, 84 test, 0 failure/error/skip: `AssignmentIT` 31, `HandoverIT` 25, `StaffEmploymentIT` 7, `PostPublicationShiftIT` 17, `PostPublicationPortsIT` 4. Log: `C:/Users/nguye/AppData/Local/Temp/eldercare-c-api-review-it-20261030.log`.

Các kiểm có ý nghĩa: `FR_020_absenceEndsOnlyThisShiftAssignments_andCallsTasksInSameTransaction`, `FR_020_taskFailureRollsBackAbsenceAssignmentsAuditAndRatio`, `FR_020_absenceLeavesOtherShiftAndItsAssignmentsUnchanged`, `BR_M09_04_careFailureRollsBackAccountShiftAssignmentAndStaff`, `FR_044_confirmationCallbacksShareTransactionAndFailureRollsBackAllEffects`, `FR_038_floorManagerCanConfirmWhenThereIsNoNextShift` đều xanh.

Log tích hợp có một lỗi bất đồng bộ ở `StaffingRatioEvents.status`: thiếu nguồn/cấu hình tính lại tỷ lệ, giữ sự kiện để thử lại. Không làm 84 assertion thất bại; chưa xác minh là lỗi baseline hoặc đã xử lý xong, không kết luận toàn bộ luồng tỷ lệ ổn. Bàn giao E trong team-plan để rà riêng.

Đây là kiểm hồi quy trong phạm vi review, không thay thế `clean verify` toàn kho, test công việc thật của 005 hoặc test luồng nghỉ 015. Không thêm test mô phỏng lại chữ ký chỉ để tăng số kiểm; sử dụng các kịch bản rollback, ca khác không đổi và bàn giao thiếu ca sau đã có.


## 4. Review nguồn ca/phân công M09 theo thời điểm — 2026-10-30

Phạm vi C trong các dòng lịch sử phụ trách, SPI ca tối thiểu và nối Phase4 của team-plan8. Đối chiếu DutyDirectory,ShiftQueries,ShiftStore,AssignmentQueries,AssignmentStore,CareShiftLookup,ShiftLocationReferences; hợp đồng005 mục2.1 và008 mục2.4/2.5. Không đổi mã,API,test hoặc migration.

| Phần review | Kết luận có bằng chứng |
| --- | --- |
| DutyApi.shiftLead(floor,at) | Đọc shift_lead_history và khoảng thành viên, không dùng con trỏ lead hiện tại để sửa quá khứ. Hai người phụ trách lịch sử chồng nhau gây InvalidPermissionDataException |
| AssignmentApi.history/activeAt | Giữ phân công/lịch sử người cũ; scopeEndsAt cắt phạm vi khi thay người,kết thúc sớm/vắng. Kết thúc ca bình thường vẫn giữ biên CFG-M15-07 theo002; không cộng biên qua mốc cắt sớm |
| Thành viên tại thời điểm | ShiftQueries/ShiftStore loại ca Nháp/Hủy, dùng khoảng nửa mở và các mốc added_at,absent_at,left_at,withdrawn_on_resignation_at; nghỉ đã duyệt không là thành viên có mặt |
| Nguồn thật | StaffModuleIntegrationIT kiểm bean từ staffing,không import fake M09/M15; gọi ScopeApi thật tại trước/đúng mốc thay và vắng. Có9interface trong danh sách kiểm hiện tại, gồm ShiftNursePort bổ sung |
| CareShiftPort/LocationReferences cũ | Có bản cài CareShiftLookup/ShiftLocationReferences; giữ DTO/NamedInterface và fake chung hiện có. AssignmentIT kiểm phân công chính cụ thể nhất và tham chiếu vị trí thật |

### Phụ thuộc chưa giải quyết

- SPI chính thức005 là ShiftStaffPort (005 T002,contracts2.1),khác CareShiftPort tối thiểu008 từng khai báo. M09 mới cài CareShiftPort; chưa tìm thấy bean production ShiftStaffPort. Hai interface khác shiftContaining/shiftAt,responsibleNurses/assignedNurses,onShift/listedInShift,DTO phân công; ShiftStaffPort còn có handoverTasks. Thêm dòng E cung cấp bản thật theo hợp đồng hiện có,giữ tương thích nguồn cũ; không tự thêm adapter củaE hoặc fake thứ hai.
- Không nghiệm thu story005 từ StaffModuleIntegrationIT: test này dùng CareShiftPort cũ,các story kế hoạch/công việc005 chưa có provider. DutyPermissionIT dùng FakeM15Config/FakeDuties/FakeAssignments nên10ca xanh chỉ xác minh cách002 dùng hợp đồng,không chứng minh DutyDirectory.shiftHasNoFloorManager đúng Q-262.
- Q-262 vẫn còn: DutyDirectory.shiftHasNoFloorManager chỉ xét có Trưởng tầng được giao tại time.now,không kiểm thành viên/vắng/nghỉ của ca đó. Giữ dòng E đã có; ảnh hưởng002 FR-019e và014 FR-066. Không sửa source008 hoặc nghiệp vụ trong lượt C.
- AssignmentQueries.careAssignments(resident,shift) chọn dữ liệu tại giờ bắt đầu ca; assignedNurses(resident,at) chọn tại thời điểm truyền vào. Không diễn giải hai hàm là cùng truy vấn hiện hành; khi E nối ShiftStaffPort và C cài story phải đối chiếu đúng thời điểm/tác động đổi phân công theo hợp đồng005. AssignmentView.from cho thành viên là giờ ca theo hợp đồng002; không coi đây là bằng chứng đã có mốc bắt đầu phạm vi riêng khi thêm giữa ca (A đã ghi nhận ở progress30/10).
- Lỗi async StaffingRatioEvents.status thiếu nguồn/cấu hình tỷ lệ tái hiện22:13:45 trong lượt này,assertion vẫn xanh. Bổ sung log vào dòng chờ E đã có; chưa kết luận baseline hoặc xử lý xong.

### Kiểm mới và cập nhật bàn giao

JDK25 riêng process,MySQL test; biến TEST_DB_* và DB_PASSWORD/DB_RETENTION_PASSWORD nạp từ backend/.env,không in giá trị. Từ backend:

```powershell
.\mvnw.cmd test failsafe:integration-test failsafe:verify "-Dtest=ShiftPortFakesTest,PrimaryAssigneeResolverTest" "-Dit.test=StaffModuleIntegrationIT,ShiftQueriesIT,DutyPermissionIT,AssignmentIT,PostPublicationShiftIT"
```

Exit0:11unit (ShiftPortFakes3,PrimaryAssigneeResolver8),72IT (StaffModuleIntegration7,ShiftQueries7,DutyPermission10,Assignment31,PostPublicationShift17),0failure/error/skip. Trong đó StaffModuleIntegration kiểm lịch sử/phạm vi bằng nguồn thật; không gộp10DutyPermission dùng fake thành bằng chứng Q-262. Log C:/Users/nguye/AppData/Local/Temp/eldercare-c-m09-time-review-20261030.log. Không chạy clean verify toàn kho; không sửa Java nên không chạy lại Spotless.

Team-plan bỏ phần C review nguồn cũ đã xong,giữ phần D review và C/D kiểm story thật; thêm E cung cấp ShiftStaffPort,cập nhật lỗi async tái hiện; giữ Q-262. Không xóa các việc005 thiếu provider hoặc ghi phase đã hoàn tất. Chưa commit/push/hoàn tất merge.
