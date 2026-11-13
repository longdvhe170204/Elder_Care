# Review triển khai T050 — gộp bảng 008

Ngày 2026-10-31, nhánh impl/008-q262, nền d2bd7de0. Người dùng đã chọn giữ ID logic/liên kết cũ. Chưa commit/push/PR.

## 1. Kết quả đã kiểm

| Phần | Đỏ trước cài | Sau cài |
| --- | --- | --- |
| Migration trên schema tạm | StaffingTableConsolidationMigrationIT: 10 thất bại, 0 lỗi thực thi | 10 xanh |
| Danh mục và định danh runtime | StaffingCatalogIdentityIT: 1 thất bại, 3 lỗi truy vấn bảng cũ | 4 xanh |
| Cấu trúc tại mốc trên | LayerArchTest | 10 xanh |
| JSON bàn giao | HandoverEmbeddedItemsIT: 2 lỗi truy vấn bảng con cũ | 2 xanh; HandoverIT 25 và HandoverSchemaIT 6 xanh |
| Ràng buộc nội dung bảng chung | StaffingSharedConstraintsIT: 7 thất bại trước validation | 7 xanh qua API thật |
| Marker và khung job | M09JobCatchUpIT: 1 thất bại, 4 lỗi bảng cũ | 5 xanh, gồm retry và hai worker cùng mốc |
| Liên kết UUID job đến audit | ShiftLifecycleIT: UUID null khi chưa truyền Basis | 2 xanh sau cài, cùng UUID cho hai chuyển trạng thái |
| Nhóm dữ liệu/bảo vệ bản gốc | RecordGroupTriggerIT / M09RecordProtectionIT | 6 + 2 xanh sau bỏ đúng 25 ngoại lệ E |
| Xóa tỷ lệ mồ côi theo ID | StaffingRatioSchemaIT: 2 thất bại, bản ghi chưa bị xóa | 2 xanh trong clean verify |
| Quét tỷ lệ mồ côi | StaffingRatioSchemaIT: 1 thất bại, danh sách ứng viên rỗng | Còn chờ API scanBefore của A |

Log ngoài mã nguồn ở backend/target/008-r15-verification của worktree gốc: migration-red-retry.log, migration-green-retry.log, catalog-identity-red.log, catalog-identity-green.log, handover-json-red.log, handover-json-green.log, shared-constraints-red.log, shared-constraints-green-retry.log, job-markers-red.log, job-markers-green.log, retention-audit-red.log, staffing-regression.log, orphan-retention-red.log.

Lượt chọn target 8.90 sai đã sửa sang 8.8 thực sự tồn tại, sau đó chạy các migration còn lại đến latest. Lượt đỏ hợp lệ là 10 thất bại/0 lỗi thực thi, trước khi viết V008_94/V008_95. Lỗi delimiter trigger trong lần cài đầu cũng đã sửa, không đổi assertion để xanh.

## 2. Migration: dữ liệu và định danh

- [V008_94 — chỉ mục legacy](../../backend/src/main/resources/db/migration/V008_94__shared_staffing_legacy_ids.sql): thêm ba cột sinh/chỉ mục duy nhất cho (loại, legacy.id) của M09 trên catalog_item, record_event, simple_record; A review phần bảng chung.
- [V008_95 — chuyển dữ liệu](../../backend/src/main/resources/db/migration/V008_95__staffing_table_consolidation.sql): bốn danh mục sang catalog_item; hai lịch sử sang status_history; công bố/ý kiến/phản hồi/tỷ lệ sang record_event; import sang simple_record; khóa tầng và dấu job sang bảng kỹ thuật chung.
- Giữ tám bảng riêng: staff_profile, staff_department, credential, shift_schedule, work_shift, shift_staff, staff_assignment, shift_handover. Giữ dữ liệu mọi trường cũ; FK vật lý đổi theo ánh xạ. Dòng chép mang metadata legacy.
- Chỉ đặt dãy cấp ID ở migration, vượt miền ID cũ, không lấy max+1 trong request concurrent. Store tách id logic/physical theo loại; không đổi tên loại audit hoặc sửa nhật ký cũ.
- Giao Trưởng tầng có id vật lý mới, legacy_floor_manager_id giữ id cũ; bản ghi CARE giữ id cũ. Dòng legacy vốn cho NULL actor/lý do được giữ nguyên, không bịa actor; lệnh mới vẫn cần actor; lý do giao Trưởng tầng tùy chọn theo hợp đồng hiện có.
- credential.practice_scope_ids chứa ID vật lý danh mục; CredentialStore trả ID logic/mã cũ cho SPI LicenseValidity. Không cập nhật M15.PROFESSIONAL_ACTIVITY.practiceScopeIds của A.
- Mã phạm vi/loại chứng chỉ dài cũ được giữ trong legacyCode nếu code bảng chung 40 ký tự không đủ; tên mẫu đầy đủ giữ originalName, tên kỹ thuật bảng chung dùng băm khi vượt 200 ký tự. Hợp đồng trả dữ liệu gốc.
- Nhắc job thành dấu SUCCEEDED; dòng lỗi/chưa xong được giữ trong metadata legacy riêng để không chặn retry. Counter item là cục bộ, khóa theo bàn giao cha và không tái sử dụng ID đã xóa.
- Thu hồi quyền của bảng cũ trước DROP; migration đã merge V008_01–08 không sửa.

## 3. Lớp danh mục và lịch sử đã kiểm

- [StaffingCatalogData](../../backend/src/main/java/com/eldercare/backend/staffing/internal/domain/StaffingCatalogData.java): record nội dung/legacy và loại riêng M09; không khai loại công khai mới.
- [StaffingCatalogStore](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingCatalogStore.java): CatalogApi thật, phân biệt ID logic với vật lý, khóa lệnh theo TechnicalLocks; không truy SQL trực tiếp bảng chung.
- CredentialStore, DepartmentDutyStore, StaffStore, phần danh mục của ShiftStore đã qua bốn test runtime. Có kiểm tạo mới sau chuyển không che ID cũ và không đọc nhầm loại khác.
- StaffingHistoryStore ghi/đóng khoảng qua StatusHistoryApi. Khoảng quyền được đọc từ lịch sử và membership, không suy từ con trỏ hiện tại.
- Việc đổi trạng thái danh mục dùng API chung; phiên bản optimistic là phiên bản bảng chung. Không thêm endpoint hoặc trường HTTP.

## 4. Bảo vệ bàn giao

- Giữ M09.HANDOVER + id bàn giao gốc cho đính chính; id mục nằm trong cùng parent.
- JSON giữ nguyên snapshot đã chốt, gồm chuỗi thời điểm/microsecond khi thêm phần AFTER_PREPARATION; không deserialize rồi ghi lại toàn bộ phần bất biến.
- Trigger khóa cả dòng sau CONFIRMED; khi PREPARED chỉ cho thêm mục mới đúng phần phát sinh và không sửa/xóa mục cũ; nguồn VITAL_SIGN/RESIDENT_CHANGE không bỏ hoặc đổi định danh.
- Hai test bổ sung kiểm hai bàn giao cùng itemId=1 không đè nhau, không tái dùng ID sau xóa, bổ sung sau lập và khóa sau xác nhận.

## 5. Phụ thuộc và giới hạn hiện tại

T050 vẫn [ ]. Người dùng chọn chờ A bổ sung RecordEventApi.scanBefore theo [đề xuất](shared-event-scan-proposal.md) mục 1. Bản thật hiện có chỉ liệt kê theo parent; không thể tìm tỷ lệ khi ca Nháp đã bị xóa. E không sửa interface, bản thật, bản giả hoặc hợp đồng của A. Test FR_037_retentionFindsExpiredRatioEvenWhenDraftShiftHasBeenDeleted được giữ nguyên, chưa có lý do để báo xanh phần này.

Lưu giữ các bản ghi có parent vẫn dùng API hiện có; xóa sự kiện theo ID dùng findByUniqueKey cho legacy và require cho bản mới. Đường đọc SQL trong staffing không quay lại bảng chung; câu DELETE loại/ID trên bảng chung chỉ chạy trong RetentionProvider bằng tài khoản lưu giữ. Khung administration.retention xóa dòng con chung trước khi gọi provider, nên HandoverRetention chỉ xóa header/JSON của E.

Nguồn cảnh báo AllocationHandoverApi/BedHandoverAlertApi vẫn chờ C chốt theo team-plan. Đây là phụ thuộc nghiệp vụ đã có, riêng với API quét sự kiện của A. Bản demo không có migration V008, nên kiểm chuyển dữ liệu dùng schema tạm với fixture cho đủ 25 bảng cũ, không coi đó là nghiệm thu dữ liệu demo/người dùng thật.

## 6. Hồi quy và phạm vi review

- Tập trung: các lượt xanh ở bảng mục 1 chỉ chứng minh phạm vi từng lượt. Lượt toàn staffing đầu tiên: 292 IT, 2 thất bại/29 lỗi thực thi; tìm ra fixture cleanup theo tên bảng cũ, trùng tên danh mục, ID bị AUTO_INCREMENT vượt bộ đếm và fixture gọi API MANDATORY ngoài transaction. Đã sửa fixture, không hạ assertion nghiệp vụ.
- Full clean verify đã chạy nền sau nạp .env, 08:00 phút: 674 test đơn vị/kiến trúc xanh; 1514 IT có 8 thất bại, 9 lỗi thực thi, không bỏ qua. Trong 008: 285/286 IT xanh, một thất bại là test quét tỷ lệ mồ côi đang chờ API A. Chín lớp B/C giữ đúng phương thức và số lỗi của mốc T050a; không lấy kết quả cũ thay cho migration mới. Log r15-clean-verify.log và r15-full-summary.json ở thư mục kiểm của worktree gốc.
- A cần review V008_94 (ba chỉ mục chỉ áp dụng module M09) và FR_020_StaffingDashboardIT (hai SQL thay bảng, assertion giữ nguyên). Đã thêm dòng mục 8; không đổi API của A.
- Đường HTTP/SPI của 008 giữ chữ ký/trường; ApiContractIT, RecordGroupTriggerIT và FR_020_StaffingDashboardIT đều xanh trong full verify. StaffImportTransportIT cũng xanh; ghi chú lỗi môi trường cũ không áp dụng cho lượt này.
- Chưa commit/push/PR, không đổi nhánh. Bảy worktree cũ, 155 file dở và ba stash đã kiểm lại SHA/status/HEAD, không đổi.

## 7. Cách đọc mã để review

1. V008_94 rồi V008_95: xem metadata legacy, dãy ID, FK, số trường chép, trigger và thu hồi quyền trước DROP. Migration cũ đã merge giữ nguyên.
2. StaffingCatalogData/Store, StaffingHistoryData/Store, StaffingEventData, StaffingRecordData/Store: xem loại và ranh giới API bảng chung. Đối chiếu StaffingCatalogIdentityIT với cùng ID logic 7001 ở bốn loại.
3. HandoverJsonData và HandoverStore: xem khóa parent, next_item_id, giữ JSON cũ khi bổ sung; đọc hai test HandoverEmbeddedItemsIT và sáu HandoverSchemaIT.
4. AssignmentStore: kiểm WHERE kind của CARE/FLOOR_MANAGER, ID logic Trưởng tầng, khóa TechnicalLocks; đối chiếu AssignmentIT/AssignmentSchemaIT/AssignmentGovernanceIT.
5. M09Jobs, tám M09TimedTask và ShiftLifecycleIT: marker/tác động cùng transaction, retry sau rollback, UUID và khôi phục MDC, số nguồn xử lý. SchedulerRuns ghi log, không tạo bảng m09_job_run mới.
6. M09Retention/Factory và HandoverRetention: kiểm ID logic → vật lý, mốc lưu giữ, tham chiếu ngược. TODO quét mồ côi là phần đang chờ A, không được bỏ khi review.


## 8. Đối chiếu lỗi có sẵn sau full verify

| Lớp | Thất bại | Lỗi thực thi | Đối chiếu mốc trước |
| --- | --- | --- | --- |
| resident.BR_11_TemporaryAbsenceOverdueIT | 0 | 1 | Cùng phương thức, cùng số lỗi |
| resident.BR_05_ReassessmentNoticeIT | 0 | 2 | Cùng phương thức, cùng số lỗi |
| resident.CompleteAdmissionConsentIT | 0 | 2 | Cùng phương thức, cùng số lỗi |
| resident.CompleteAdmissionWishWarningsIT | 0 | 3 | Cùng phương thức, cùng số lỗi |
| resident.FR_023h_KhongDauNangLucIT | 1 | 0 | Cùng phương thức, cùng số lỗi |
| resident.FR_034a_CompleteAdmissionIT | 4 | 0 | Cùng phương thức, cùng số lỗi |
| resident.ResidentContractIT | 1 | 0 | Cùng phương thức, cùng số lỗi |
| resident.StatusHistoryIT | 0 | 1 | Cùng phương thức, cùng số lỗi |
| roombedallocation.FR_035_LocationHistoryHttpIT | 1 | 0 | Cùng phương thức, cùng số lỗi |

Một lỗi ngoài baseline: StaffingRatioSchemaIT.FR_037_retentionFindsExpiredRatioEvenWhenDraftShiftHasBeenDeleted. Hai ca xóa sự kiện đã biết ID (mới/legacy) xanh; phần thiếu là tìm ra ứng viên không còn parent. Chưa được merge/nghiệm thu T050 với test này còn đỏ.

Danh sách file, gồm file chưa được Git theo dõi: [r15-implementation-files.md](r15-implementation-files.md). Bảng này đi kèm review cấu trúc trước đó, không thay số liệu lịch sử của T050a.


Lưu ý về giới hạn stat: git diff --stat hiện chỉ tính file đã được theo dõi, nên hiển thị nhiều đường dẫn cũ bị xóa khi chuyển tầng. Các file mới/chuyển tới vẫn chưa stage; danh sách r15-implementation-files.md đọc cả file chưa theo dõi. Không xem số dòng xóa trong stat là toàn bộ thay đổi hoặc là đã bỏ chức năng.


## 9. Kiểm cuối sau dọn dependency

Sau dọn dependency JsonMapper không còn được ShiftStore dùng, chạy Spotless/verify tập trung: LayerArchTest 10 và 39 IT (ApiContractIT 1, ShiftHttpIT 3, ShiftScheduleIT 11, StaffingRatioIT 14, StaffStatisticsIT 7, StaffStatisticsReportFieldsIT 3) đều xanh, không bỏ qua; BUILD SUCCESS, 01:50. Lượt bổ sung trước đó bị ngắt khi ghi báo cáo, không được tính đạt; lượt chạy lại ở final-shift-regression-retry.log đã kết thúc đủ. Kết quả full clean verify ở mục 6/8 vẫn là BUILD FAILURE với một test E chờ API và baseline B/C; không dùng lượt tập trung để gọi toàn backend xanh.
