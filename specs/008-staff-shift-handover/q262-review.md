# Review 008: Q-262 và các thay đổi chờ E — 2026-10-30

## 1. Chuẩn bị và giữ phần đang làm dở

- Nhánh: `impl/008-q262`; worktree `.worktrees/008-q262`.
- Nền: `origin/develop`, commit `d2bd7de0777d61467598f397a2a2076b1ee03fff` (PR #66).
- Đã tạo worktree riêng trên `develop`, chạy `git pull --ff-only origin develop`, rồi tạo nhánh; trước khi viết test worktree sạch.
- Bảy worktree cũ giữ nguyên. Sao lưu 155 file chưa commit và kiểm SHA-256, giữ ba stash: `.worktrees/.snapshots/before-008-q262-20261030-203538/manifest.json` (tính từ gốc kho).
- Chưa commit, push, merge PR hay mở PR. Phần 010 chưa commit và các worktree mobile/FR-057 vẫn riêng, chưa được đưa vào nhánh này.

## 2. Review mã Q-262 — T051

Căn cứ: `nghiep-vu.md` Q-262; 002 FR-019e; 014 FR-066; dòng việc A báo sau review M09 ngày 2026-10-30.

Trước sửa, `DutyDirectory.shiftHasNoFloorManager` chỉ kiểm tầng có Trưởng tầng được giao tại thời điểm hiện tại hay không. Vì vậy Trưởng tầng không trực ca, trực tầng khác, đã vắng hoặc nghỉ đã duyệt vẫn chặn Người phụ trách ca ghi kết quả kiểm tra chất lượng.

Sau sửa:

- Chỉ xét ca đã công bố và có phạm vi; ca Nháp, đã hủy hoặc không tồn tại không mở ngoại lệ.
- Lấy thời điểm một lần qua `BusinessTime`.
- Tra người được giao làm Trưởng tầng rồi xét thành viên của đúng `shiftId`.
- `ShiftQueries.presentInShift` dùng `ShiftStore.members` và `ShiftMember.presentAt` đang có; mốc vắng, nghỉ đã duyệt và giới hạn thành viên được dùng chung với nguồn ca.
- Không mở quyền trước mốc vắng; ca tầng khác không làm mất quyền của Người phụ trách ca đang xét.
- Không thêm SQL trong service, không đổi `DutyApi`, DTO, HTTP, migration hay dữ liệu mẫu. Ngoại lệ `PENDING_*` và `PENDING_DATA_GROUP` giữ nguyên vì T051 không phải task gộp/cấu trúc.

File mã:

- `backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/DutyDirectory.java`: hàm `shiftHasNoFloorManager`.
- `backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/ShiftQueries.java`: hàm nội bộ `presentInShift`.
- `backend/src/test/java/com/eldercare/backend/staffing/DutyDirectoryIT.java`: test MySQL với `DutyApi` và `PermissionApi` thật; không dùng fake nhiệm vụ/quyền.

## 3. Review test trước và sau sửa

Lượt đầu lỗi fixture thiếu `requires_handover`, không coi là bằng chứng tái hiện Q-262. Đã sửa fixture và định dạng đúng file test trước khi sửa mã triển khai.

Lượt đỏ hợp lệ: 7 test, 4 thất bại nghiệp vụ, 0 lỗi thực thi, 0 bỏ qua. Chỉ sau lượt này mới sửa hai lớp triển khai.

| Test trong DutyDirectoryIT | Kỳ vọng | Sau sửa |
| --- | --- | --- |
| `FR_019e_Q_262_managerNotInPublishedShift_allowsShiftLeadQualityCheck` | Không trực ca: mở quyền, căn cứ SHIFT_LEAD | Xanh |
| `FR_019e_Q_262_managerAbsenceStartsAtRecordedInstant_withoutGrantingEarly` | Trước mốc vắng chặn, đúng mốc vắng mở | Xanh |
| `FR_019e_Q_262_approvedLeaveAllowsShiftLeadQualityCheck` | Nghỉ đã duyệt: mở quyền | Xanh |
| `FR_019e_Q_262_managerPresentInTheSameShift_keepsExceptionClosed` | Có mặt: chặn ngoại lệ | Xanh |
| `FR_019e_Q_262_managerWorkingAnotherFloor_doesNotBlockThisShiftLead` | Trực tầng khác: không chặn ca này | Xanh |
| `FR_019e_Q_262_draftShiftDoesNotQualifyForTheException` | Ca Nháp không tạo quyền | Xanh |
| `FR_019e_Q_262_unknownShiftDoesNotQualifyForTheException` | Ca không tồn tại không tạo quyền | Xanh |

Lượt kiểm tập trung: 39 test kiến trúc + 71 IT xanh, không bỏ qua, BUILD SUCCESS (02:27). Trong đó: DutyDirectoryIT 7, DutyPermissionIT 10, AssignmentIT 31, M09GovernanceIT 6, ShiftQueriesIT 7, StaffModuleIntegrationIT 7, StaffStatisticsReportFieldsIT 3.

Log giữ ngoài target của worktree để `clean` không xóa bằng chứng: `backend/target/008-q262-verification/` tại gốc kho. Lượt đỏ hợp lệ: `q262-red-retry.log`; lượt xanh: `q262-green.log`; định dạng: `green-spotless.log`.

`clean verify` cuối hoàn tất 14:10 sau khi nạp `backend/.env` ở gốc kho, giữ mật khẩu demo: 674 unit/arch xanh; 1486 IT còn 7 thất bại/9 lỗi ở đúng chín lớp baseline, 0 bỏ qua, không lớp đỏ mới. 258 IT staffing đều xanh, gồm DutyDirectoryIT 7 và StaffImportTransportIT 1. Maven BUILD FAILURE do baseline, không gọi toàn backend là xanh. T051 đã [x]; số 39+71 ở trên là lượt tập trung, không cộng trùng vào tổng cuối.

## 4. Review các thay đổi đang chờ E

### Dòng 116 — API thống kê cho 016

- `StaffStatisticsService.staffingRatios` giữ số người, tổng trọng số, mẫu ca và giờ bắt đầu/kết thúc ca từ bản ghi gốc.
- `openHandovers` không giới hạn ngày; query chọn PREPARED, HAS_COMMENTS hoặc DRAFT của ca AWAITING_HANDOVER; lọc phạm vi trước khi trả.
- `HandoverReportView.shiftEndsAt` và `ExpiringCredentialView.credentialGroup` được cài, không chỉ khai báo DTO.
- Ba test `StaffStatisticsReportFieldsIT` kiểm cả bàn giao cũ 40 ngày, ngoài phạm vi, đã xác nhận và trường báo cáo; đã chạy xanh.
- FakeStaffStatistics chỉ trả dữ liệu đã đặt, không mô phỏng đủ lọc phạm vi/ngày/trạng thái ca; bằng chứng bộ lọc ở bản thật là StaffStatisticsReportFieldsIT, không lấy fake làm nghiệm thu.
- Kết luận: đồng ý phần E review; giữ các trường/hành vi này khi làm T050.

### Dòng 120 — người có ca và Bác sĩ trực cho 009

- `staffOnShift` lấy thành viên có mặt của ca tầng tại mốc chỉ định.
- `onDutyDoctors` dùng truy vấn Bác sĩ trực có mặt, vai trò DOCTOR và dấu trực; không gom người không có dấu.
- `ShiftQueriesIT` đã kiểm vắng, kết thúc ca, tầng không có ca và Bác sĩ được đánh dấu; 7 test của lớp xanh.
- Kết luận: đồng ý phần E review; không đổi hợp đồng hay bản giả.

### Dòng 207 — hai test sau PR #66

- `AssignmentIT.FR_030_assignedNurseUsesTheConfiguredMedicationLicenseAndFeedsAllThreeSpis` đổi cấu hình nghiệp vụ chuyên môn sang `catalog_item.attributes.requiredLicenseType`; vẫn kiểm thiếu giấy phép bị chặn rồi đủ giấy phép được phân công.
- `M09GovernanceIT.FR_051_professionalActivityReferencesPreventCatalogPurge` đặt `practiceScopeIds` trong `catalog_item`; vẫn kiểm tham chiếu giấy phép/phạm vi ngăn loại bỏ.
- Hai test chạy trong transaction; không để cấu hình thử làm ảnh hưởng test khác. AssignmentIT 31 và M09GovernanceIT 6 xanh.
- Kết luận: đồng ý phần E review; không sửa assertion hay test của A.

### Dòng 181 và nguồn bàn giao C — còn việc cần C

- `BedFlagView.bedId` là Long, null cho WITHOUT_BED. Consumer staffing hiện không đọc BedFlagView nên không có chỗ ép null thành 0 trong nguồn này.
- `BedStatusApi`, `BedStatisticsApi` có bản thật; phần nguồn giường của E đang dùng hai API này.
- Phát hiện còn mở: `HandoverSources` đọc `AllocationHandoverApi.atFloor`, nhưng C cài `BedHandoverAlertApi.byFloor` ở `BedReportingService`.
- DTO khác nhau: `AllocationHandoverView(id,residentId,roomId,description,createdAt)` và `BedAlertView(allocationId,residentId,bedId,code)`. Chưa có bản thật AllocationHandoverApi.
- Mốc nguồn còn được dùng ở HandoverService.sources khi bàn giao cũ chưa có snapshot sourceKeys: at rỗng sẽ bỏ qua mục phát sinh sau khi lập. Vì vậy không tự đặt createdAt=now hoặc bỏ metadata thời điểm để nối tạm hai DTO.
- Không tự dùng giờ hiện tại thay createdAt hay tự đổi API của C. C cần chốt/cài nguồn đúng hợp đồng để E nối; dòng chờ nguồn C phải giữ. Test fake chưa chứng minh được liên thông cảnh báo thật.

### Dòng 122 — fixture nguồn trạng thái của C

- `FR_035_LocationHistoryHttpIT` dùng MockitoSpyBean ResidentStatusApi thật.
- `FR_036_missingM01AbsenceSourceFailsClosed` tạo id hồ sơ trong dữ liệu phân bổ nhưng không dựng hồ sơ M01 và không chủ động làm nguồn vắng không sẵn sàng.
- Vì bản đọc lịch sử M01 đã cài thật, không thể dùng giả định stub chưa cài để bảo đảm nhận 503. C/B cần cô lập rõ tình huống nguồn lỗi và dữ liệu không tồn tại.
- Không sửa mã/test C/B hoặc đổi assertion 503 thành 404 trong nhánh này. Lượt toàn kho cuối tái hiện đúng 404/503 tại lớp đỏ baseline này; còn chờ C/B sửa fixture đúng tình huống.

## 5. Ghi chú cũ đã cập nhật và phần tiếp theo

- T050 bỏ chờ 000 T108; API bảng chung và bản giả đã có trên develop.
- T016b/T022b/T044b ghi mobile đã mở; các worktree mobile đang giữ phải được đối chiếu trước khi tích hợp, chưa đánh task mobile hoàn thành.
- Kết quả lịch sử ở progress.md không sửa/xóa.
- T050/V008_95 và việc cấu trúc staffing chưa cài trong bước Q-262 này; sẽ theo data-model mục 0/0.1, sáu ý R-15 và danh sách PENDING đã báo.

## 6. Kết quả cuối và giới hạn

| Lượt kiểm | Kết quả | Kết luận |
| --- | --- | --- |
| Test trước sửa | 7 test, 4 thất bại nghiệp vụ, 0 lỗi/0 bỏ qua | Tái hiện Q-262 trước khi sửa mã |
| Kiểm tập trung sau sửa | 39 kiến trúc + 71 IT xanh, 0 bỏ qua | BUILD SUCCESS |
| clean verify lần 1 | Dừng khi nghẽn output; 674 unit/arch, 1404 IT đã có báo cáo, 13 thất bại/22 lỗi | Lượt chưa hoàn tất, giữ bằng chứng; không dùng làm kết quả cuối |
| clean verify lần 2, output test vào file | 674 unit/arch xanh; 1486 IT, 7 thất bại/9 lỗi, 0 bỏ qua; 14:10 | Không lớp đỏ mới; BUILD FAILURE do baseline |

Lượt 2 chạy đủ toàn bộ test, chỉ thêm `-Dmaven.test.redirectTestOutputToFile=true` để output nằm ở `*-output.txt`. Không đổi logging level, pool, timeout, test, assertion, ngưỡng hoặc mã. Có thread dump cho thấy luồng test chờ ghi stdout trong lượt đầu; đây là bằng chứng nghẽn của lượt đó, không coi mọi lỗi ban đầu là lỗi nghiệp vụ.

Những lớp bị lỗi ngoài baseline trong lượt đầu đều xanh ở lượt cuối: C `FR_024_AllocationStatusParticipantIT` 22, `T064_BedListPerformanceIT` 1 (giữ ngưỡng 1000 ms); E `CredentialLifecycleIT` 11, `HandoverIT`, `M09GovernanceIT` 6, `PostPublicationPortsIT` 4, `PostPublicationShiftIT` 17, `ShiftHttpIT` 3, `ShiftScheduleIT` 11. `DutyDirectoryIT` 7 xanh cả lượt kiểm tập trung và lượt toàn kho cuối.

Lệnh cuối chạy từ backend của worktree 008-q262, sau khi nạp backend/.env ở kho gốc: `.\mvnw.cmd -B clean verify "-Dmaven.test.redirectTestOutputToFile=true"`.

Chín lớp baseline còn đỏ:

| Chủ | Lớp | Thất bại | Lỗi |
| --- | --- | ---: | ---: |
| B | BR_11_TemporaryAbsenceOverdueIT | 0 | 1 |
| B | BR_05_ReassessmentNoticeIT | 0 | 2 |
| B | CompleteAdmissionConsentIT | 0 | 2 |
| B | CompleteAdmissionWishWarningsIT | 0 | 3 |
| B | FR_023h_KhongDauNangLucIT | 1 | 0 |
| B | FR_034a_CompleteAdmissionIT | 4 | 0 |
| B | ResidentContractIT | 1 | 0 |
| B | StatusHistoryIT | 0 | 1 |
| C | FR_035_LocationHistoryHttpIT | 1 | 0 |

So với progress.md của A ngày 2026-10-30: không có lớp ngoài baseline; `StaffImportTransportIT` vốn được A ghi lỗi môi trường đã xanh ở máy này. Chưa sửa test/mã B/C và chưa nghiệm thu toàn kho, demo, SC hoặc 014 đầu-cuối.

Bằng chứng tại `backend/target/008-q262-verification` ở gốc kho: `q262-red-retry.log`, `q262-green.log`, `clean-verify.log` (lượt dừng), `attempt1-partial-summary.json`, `clean-verify-redirected.log`, `full-summary.json`; ba báo cáo lỗi đại diện lượt đầu và hai thread dump giữ nguyên.

## 7. Review tài liệu, phạm vi và việc tiếp theo

- Team-plan: xóa dòng sửa Q-262 và ba dòng review đã xử lý (116, 120, 207 của bản d2bd7de0); hai dòng phối hợp 122/181 chỉ bỏ phần E, giữ B/C và C/A còn dở.
- Giữ/cập nhật dòng chờ C chốt nguồn cảnh báo thật; thêm A/002, C/014 review consumer Q-262. Bỏ phần E/015 chờ ExtendedPublicationCheck ở dòng ghép vì T029/Q-397 đã bỏ chức năng đó; giữ B/013, C/014 còn dở.
- T016b/T022b/T044b và dòng demo ghi mobile đã mở, chưa đánh task mobile xong; T050 bỏ chờ T108. Plan cập nhật FileService thật, phạm vi sửa và kết quả kiểm; glossary chỉ thêm một dòng mục 14.8.
- Progress chỉ thêm một dòng cuối; không sửa lịch sử. Không sửa API/HTTP, migration, PENDING, POM, cấu hình hoặc mã web/mobile. Không có lý do chạy npm/Flutter trong bước backend này.
- Các worktree FR-057/mobile/010, 155 file sao lưu và ba stash phải giữ nguyên qua kiểm cuối. Không commit/push/mở PR/merge PR.
- Bước tiếp theo còn là T050/V008_95 và cấu trúc staffing; 000 T108 đã đủ. Gộp bảng chưa cài trong nhánh Q-262 này; nguồn bàn giao C và nghiệm thu thật vẫn mở.
