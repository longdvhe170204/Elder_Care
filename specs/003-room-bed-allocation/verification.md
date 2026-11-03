# Đối chiếu kiểm chứng Spec 003 — Phase 11

Ngày: 2026-10-25. Phạm vi: T061–T065, đối chiếu data-model mục11, các ghi chú triển khai và mã hiện có. Bảng này ghi bằng chứng thực tế, không thay thế yêu cầu nghiệp vụ hoặc tự đánh hoàn tất các phase trước.

## 1. Kiểm đã chạy trong lượt này

| Phần | Bằng chứng | Kết quả đã quan sát |
| --- | --- | --- |
| T061 ba API dữ liệu | `003-p11-reporting-red2.log` → `003-p11-reporting-green.log` |5test đỏ vì thiếu bean thật rồi xanh cùng25test kiến trúc |
| Fix review T061 | `003-p11-review-red.log` → `003-p11-review-green.log` |2test mới đỏ đúng scope theo cửa sổ/Stream đọc CFG muộn; sau sửa7IT+25test kiến trúc xanh |
| T062/T064 | `003-p11-cross-cutting.log` |7IT xanh: trigger chung4, hàng thật003 một, HTTP contract một, hiệu năng một;25kiến trúc xanh |
| T065 bổ sung nguồn sự kiện | `003-p11-traceability.log` | FR_024_BedEventsTest có4test đơn vị (thêm FR-051 cho12loại thông báo), cùng25kiến trúc xanh;12IT chuyển kiểm soát xanh, đã khẳng định payload BedTransferred từ lệnh thật |
| Web | `003-p11-web-test.log`, `003-p11-web-lint.log`, `003-p11-web-build.log` |250test/38file, lint/build qua; cảnh báo chunk>500kB còn tồn tại; không đổi UI |
| Flutter | `003-p11-flutter-test.log` | exit1 trước khi chạy test: Windows yêu cầu symlink/Developer Mode; không ghi mobile xanh và không tự đổi cài đặt hệ thống |
| Kiểm cuối | `003-p11-clean-verify-final.log` | Chạy nền `mvnw.cmd clean verify`; kết quả cuối được bổ sung ở phần5 sau khi kết thúc |

Các log nằm trong `C:/Users/nguye/AppData/Local/Temp/`; không chứa cấu hình bí mật trong tài liệu/kho mã.

## 2. Truy vết quy tắc tới test thật

Tên file test trong bảng thuộc `backend/src/test/java/com/eldercare/backend/roombedallocation/`, trừ nơi ghi khác. “Có test” chỉ nói phạm vi nêu trong cột cuối, không mặc định luồng/module khác đã tích hợp thật.

| Mã gốc | File kiểm thực tế | Phạm vi và giới hạn |
| --- | --- | --- |
| DBR-09 | `DBR_09_AllocationSchemaIT`, `FR_020_AllocationConcurrencyIT`, `FR_020_AllocationLocksIT`, `FR_020_AllocationRetryIT` | Cột sinh/UNIQUE, overlap, khóa và10luồng; nguồn ngoài qua API/fake đúng chủ |
| DBR-10 | `BR_M03_01_AllocationConditionsIT`, `DBR_26_BedSchemaIT` | Điều kiện FR-018 và CHECK thuộc tính giữ chỗ |
| DBR-23 | `DBR_23_RecordTablesIT`; `administration/RecordGroupTriggerIT.DBR_23_m03SqlRecordTablesHaveBothAppendOnlyTriggers` | Hai bảng nhóm3: bed_state_history, room_isolation_history; app và admin đều bị chặn UPDATE/DELETE trên hàng thật |
| DBR-25 | `FR_016_BedStateIT.FR_016a_delayedExecutionStoresBothTimesAndReadsConfiguration`, `FR_044_BedRemindersIT` | Thời điểm thực tế/dự kiến, CFG ngưỡng trễ; chưa chứng minh chuyển theo lịch T032–T033 còn thiếu |
| DBR-26 | `DBR_26_BedSchemaIT`, `BR_M03_09_BedReadinessIT` | Phụ thuộc trạng thái/CHECK, xác nhận sau đóng; nhánh chuyển đang chờ hiện503 khi thiếu provider |
| DBR-32, phần003 | `BR_M03_22_BedCreationIT` | Ghi tăng tài sản cùng transaction qua fake019; không thay cho bean019 thật |
| BR-M03-01 | `BR_M03_01_AllocationConditionsIT`, `SC_008_BedReportingIT.FR_018_availableCountRejectsFutureReservationsAssetFlagsAndCordons` | Mức, loại, giới tính, cách ly, trạng thái, giữ chỗ, dấu và đếm giường thỏa;007/019 còn fake |
| BR-M03-02 | `FR_020_AllocationConcurrencyIT`, `DBR_09_AllocationSchemaIT` | Một giường chỉ một người thắng; DB chặn ghi thẳng vi phạm |
| BR-M03-03 | `BR_M03_03_DayCareCapacityIT` | Sức chứa từng buổi/ngày, giao khung, CFG unset/khoanh vùng; nguồn lịch004 còn fake |
| BR-M03-04 | `BR_M03_21_ControlledTransferIT.BR_M03_21_prescribedTransferBypassesIsolationOnlyAndClosesOldAllocation` | Đã kiểm BedTransferred có đúng resident/old/new allocation và tầng từ lệnh thật; chưa có receiver005/011 thực hiện tác động sau commit |
| BR-M03-05 | `FR_013_BedAssetStateIT.BR_M03_05_occupiedBedRejectsMaintenanceAndRetirement` | Chặn bảo trì/thanh lý khi có người |
| BR-M03-06 | `FR_013_BedAssetStateIT`, `FR_015_HoldExpiryIT`, `BR_M03_09_BedReadinessIT` | Các đường về Trống và giữ Chờ sẵn sàng, terminal; cổng nguồn là fake khi chưa có bản thật |
| BR-M03-07 | `FR_028_ControlledTransferAtomicityIT` | Rollback nguyên tử chuyển kiểm soát; chuyển thường T036 và chuyển theo lịch T032–T033 chưa đủ, không gọi đây là phủ toàn bộ US3 |
| BR-M03-09 | `BR_M03_09_BedReadinessIT`, `FR_044_BedRemindersIT` | Quyền theo tầng/ca, chuyển sẵn sàng và nhắc một lần; app nhân viên chưa mở |
| BR-M03-10 | `BR_M03_09_BedReadinessIT.BR_M03_10_currentInfectionListIsRecheckedAtCommandTime`, `.BR_M03_10_databaseRejectsMissingDisinfectionEvidence` | Hỏi lại007 tại lệnh; thiếu bằng chứng422 và CHECK MySQL; chưa nhận sự kiện tiếp xúc thật007 |
| BR-M03-13, phần003 | `FR_013_BedAssetStateIT` | Hủy FUTURE ban đầu khi hỏng lúc chuẩn bị; nhánh yêu cầu chuyển theo lịch chưa có flow đầy đủ |
| BR-M03-15, phần003 | `FR_011_BedCommandsIT.BR_M03_15_suspendRejectsAwaitingResidentTransfer`, `BR_M03_09_BedReadinessIT.FR_013b_assetAwaitingTransferPreventsWaitingAdmissionSignal` | Dấu chờ chuyển người ngăn thao tác/tín hiệu nhận người mới; lifecycle019 còn cần nguồn thật |
| BR-M03-19 | `FR_024_AllocationStatusParticipantIT.FR_025a_returnWithoutAllocationMarksResidentWithoutBed`, `FR_044_BedRemindersIT` | Đặt dấu khi trở về, nhắc và gỡ; không tự tạo phân bổ |
| BR-M03-20 | `FR_022_AllocationHttpIT` | Quyền và phạm vi Trưởng tầng khi phân bổ ban đầu; không suy quyền từ UI |
| BR-M03-21 | `BR_M03_21_ControlledTransferIT` | Bác sĩ Hoạt động; loại LOCKED/INACTIVE/TEMPORARILY_LOCKED; vẫn giữ điều kiện ngoài cách ly |
| BR-M03-22 | `BR_M03_22_BedCreationIT` | Cảnh báo vượt số giường tiêu chuẩn, không chặn tùy tiện |
| BR-M03-23 | `FR_034_AllocationSuitabilityIT`, `FR_044_BedRemindersIT.FR_034_flagCommandStartsAndResetsReminderCycleBeforeFirstJob` | Đánh giá lại, đặt/gỡ marker cùng lệnh và reset đúng chu kỳ; còn sự kiện/nguồn ngoài chưa triển khai |
| BR-M03-24 | `BR_M03_24_AllocationCorrectionIT` | View đã áp đính chính, before/after, RR; consumer007 thật chưa có |
| BR-M03-25 | `BR_M03_25_DayCareParameterIT`, `FR_038_DayCareHttpIT` | Đếm lại sau đổi CFG/khung, cảnh báo, không tự hủy lịch; nguồn004 còn fake |
| FR-024, FR-024a | `FR_024_AllocationStatusParticipantIT`, `SC_008_BedReportingIT` | Tạm vắng/giữ/trở về/cảnh báo cách ly, bàn giao chỉ dấu chờ bác sĩ chưa xử lý; event gỡ khoanh/thêm tiếp xúc007 chưa đủ |
| FR-025, FR-025a | `FR_024_AllocationStatusParticipantIT`, `FR_023_AllocationCommandsIT`, `FR_022_AllocationHttpIT`, `FR_044_BedRemindersIT` | Đóng/giải phóng khi kết thúc, dấu không giường và nhắc/gỡ; fixture001 toàn kho còn lỗi riêng |
| FR-051 của003 | `FR_024_BedEventsTest.FR_051_notificationLevelsMatchTheApprovedMatrix` |12loại thông báo LOW/MEDIUM/URGENT theo ma trận; không nhầm với FR-051 lưu giữ của000 trong FR_049_M03GovernanceIT |
| SC-008 | `SC_008_BedReportingIT` | Cùng snapshot cho số đếm/stateAt, chưa tồn tại trước CREATE_BED, scope rỗng, scope theo từng cửa sổ và Stream không đọc CFG sau khi trả |

BR-M03-16/17/18 thuộc019 theo data-model11, không ghi đã kiểm toàn bộ019 qua vài test cổng giường.

## 3. Đối chiếu quickstart mục3

Chưa chạy được nguyên37kịch bản bằng profile demo. Endpoint công tắc fake được nêu trong thiết kế quickstart chưa có controller/hợp đồng HTTP đã cài; các provider004/007/008/019 thật chưa có và còn luồng chuyển thường/theo lịch thiếu. Bảng dưới là đối chiếu test backend, không phải biên bản thao tác UI/demo.

| Kịch bản | Bằng chứng tự động hiện có | Phần chưa đủ |
| --- | --- | --- |
| Q1–Q2 | FR_001_StructureHttpIT, FR_003_UniqueCodesIT, BR_M03_22_BedCreationIT | Chưa chạy UI/demo với tài khoản/nguồn019 thật |
| Q3 | FR_006_RoomPolicyIT, FR_006_RoomPolicyConcurrencyIT | Chưa chạy UI/demo |
| Q4–Q11 | BR_M03_01_AllocationConditionsIT, FR_022_AllocationHttpIT, FR_020_AllocationConcurrencyIT, FR_024_AllocationStatusParticipantIT | Nguồn004/007/019 còn fake; trạng thái M01 dùng API thật nhưng các điều kiện ngoài003 có fixture riêng |
| Q12–Q13 | FR_028_ControlledTransferAtomicityIT phủ rollback nhánh kiểm soát | Chuyển thường/đồng thời chuyển thường chưa có endpoint thực thi |
| Q14–Q15 | FR_044_BedRemindersIT phủ nhắc các dấu | Chuyển khác giá, vòng đời yêu cầu và gỡ dấu theo kết quả004 chưa đủ |
| Q16–Q19 | BR_M03_09_BedReadinessIT, FR_044_BedRemindersIT, BR_M03_01_AllocationConditionsIT | App nhân viên chưa mở; Q18 thông báo khi thêm tiếp xúc thật007 chưa có; lỗi thiếu khử khuẩn đã duyệt422 |
| Q20–Q21 | FR_016_BedStateIT kiểm trễ nói chung; readiness kiểm thiếu provider không ghi thành công giả | Chuyển theo lịch/thực hiện ngay sau xác nhận còn T032–T033/004 |
| Q22–Q23 | FR_024_AllocationStatusParticipantIT, BR_M03_21_ControlledTransferIT, SC_008_BedReportingIT | Sự kiện007/ca008 thật và demo còn thiếu |
| Q24–Q25 | BR_M03_21_ControlledTransferIT, BR_M03_01_AllocationConditionsIT | Chuyển thường ra vùng cách ly chưa có; nhánh kiểm soát đã có test |
| Q26–Q29 | FR_012_BedHoldsIT, FR_015_HoldExpiryIT, FR_024_AllocationStatusParticipantIT, FR_034_AllocationSuitabilityIT, FR_044_BedRemindersIT | Cổng004 còn fake, không coi là tích hợp nguồn thật |
| Q30–Q32 | FR_027_BackfillSchemaIT, FR_035_LocationHistoryIT/HttpIT, BR_M03_24_AllocationCorrectionIT | Nhập bù chuyển thường chưa đủ; phép tra/đính chính đã có; không đồng nhất kiểm schema với toàn workflow nhập bù |
| Q33–Q35 | FR_013_BedAssetStateIT, FR_011_BedCommandsIT, BR_M03_09_BedReadinessIT | Còn lifecycle019 thật và nhánh chuyển theo lịch |
| Q36–Q37 | FR_038_DayCareHttpIT/SchemaIT/ConcurrencyIT, BR_M03_03_DayCareCapacityIT, BR_M03_25_DayCareParameterIT | Nguồn lịch004 thật; UI sửa/ngừng và danh sách người có lịch theo quyết định giữ contract/SPI trước |

## 4. Hiệu năng và giới hạn dữ liệu

- `T064_BedListPerformanceIT` dựng100phòng/300giường trên MySQL thật, warm-up một lần, đo10lần GET /beds theo tầng qua MockMvc (có quyền/serialization/assert số dòng). Lượt hẹp có mẫu96–256ms, trung vị tính từ10mẫu là116.5ms; mọi mẫu<1000ms. Không phải phép đo mạng, trình duyệt hoặc60người dùng đồng thời.
- SC-005 quickstart5 còn yêu cầu ít nhất10lần thao tác Hành chính có hướng dẫn,60người dùng và trung vị<2phút: chưa đo, không gắn nhãn đạt SC-005 toàn phần từ test API này.
- BedStatusApi/BedStatisticsApi dùng lịch sử trạng thái gốc để tái dựng state; số liệu phân bổ/nguồn người dùng view đã áp đính chính. Khoảng dùng nửa mở, cùng thời điểm phân xử bằng id. Khoảng Chờ sẵn sàng được cắt vào cửa sổ hỏi; deadline từ CFG-M03-04 tại lúc vào trạng thái, không từ đầu cửa sổ hỏi.
- Resident scope của mỗi khoảng Chờ sẵn sàng gắn với người có phân bổ đã đóng gần nhất trước lần vào trạng thái đó; REUSE không có người vừa rời. Không dùng người ở đầu toàn bộ khoảng hỏi để cấp quyền cho mọi cửa sổ về sau.
- Nguồn cấu hình phòng/tài sản hiện chỉ có dữ liệu hiện hành; API không bịa lịch sử của chúng. Fact giường có floor_area_id nhưng không lưu room_id; lịch sử cách ly phòng không có snapshot tầng. Quan hệ phòng/cơ cấu quá khứ trước khi thay đổi cha không tái dựng đầy đủ từ schema hiện tại. Không gọi bộ lọc điều kiện/phòng quá khứ là kiểm chứng đầy đủ khi thiếu dữ liệu này.
- T063 mapping đúng:004→admission;007→incident/health;008→staffing;019→roombedallocation. Không có lớp production cài các SPI003 cần từ những feature đó trong checkout; medication thuộc006 và reporting thuộc016, không dùng hai gói này để kết luận nguồn007/019.

## 5. Gate cuối và thứ tự còn lại

`003-p11-clean-verify-final.log` đã kết thúc exit1/04:09phút:393test đơn vị/kiến trúc xanh;1073IT có6failure+9error,0skip. Đọc XML xác nhận227IT M03 xanh,15lỗi còn lại thuộc8lớpM01 đã bàn giaoB. Spotless1328file,0cần sửa. Lượt hiệu năng cuối:67–85ms, trung vị77.5ms cho10lần đọc sau warm-up. Frontend250test/lint/build qua; Flutter chưa chạy test vì thiếu hỗ trợ symlink. Không đánh thêm checkbox. Cần tách rõ:

1. T061: implementation ba API và các test đã có; còn giới hạn dữ liệu lịch sử được nêu trên, chờ gate toàn kho.
2. T062: contract đã đăng ký và kiểm các endpoint đã cài; còn pending chuyển thường là task trước, không gỡ giả; kiểm hai bảng nhóm3 đã bổ sung.
3. T063: chờ provider thật004/007/008/019, không thay bằng fake production.
4. T064: cần môi trường Flutter hỗ trợ symlink, demo đủ nguồn/luồng và phép đo SC-005 người dùng thực tế; không tự đổi cài đặt Windows.
5. T065: bảng truy vết đã đối chiếu; những hàng thiếu integration/workflow vẫn phải hoàn thiện ở task gốc. Không tuyên bố toàn bộ003 đã hoàn tất vì chỉ có bảng mapping.


## 6. Review UUID lần chạy job M03 — 2026-10-30

C review phần của mình trong dòng đổi `Basis.schedulerRunId` của team-plan mục8 (000 T108 phần2). `Basis.schedulerRunId` và `SchedulerRuns.start` dùng `String` UUID. Không đổi API hoặc mã production trong lượt này.

| Job | Đường truyền mã lần chạy | Kết luận |
| --- | --- | --- |
| AllocationReminderJob | SchedulerRuns → AllocationReminderProcessor.remind → Basis.schedulerRunId | Giữ nguyên UUID trong JSON nhật ký, tra được START/FINISH của đúng job |
| BedReadinessReminderJob | SchedulerRuns → BedReadinessReminderProcessor.remind → Basis.schedulerRunId | Giữ nguyên UUID trong JSON nhật ký, tra được START/FINISH của đúng job |
| HoldExpiryJob | SchedulerRuns → HoldExpiryProcessor.expire → BedTransition.basis hoặc audit trực tiếp | UUID không bị đưa vào basis_id dạng số; kiểm đường hết hạn giữ hồ sơ chờ tới nhật ký và log |
| FutureAllocationPastStartJob | SchedulerRuns.start/finish, processor chỉ phát cảnh báo | Processor không nhận runId, không tự bắt đầu phân bổ; cơ chế UUID/log kiểm qua SchedulingIT, hành vi cảnh báo kiểm qua FR_023_FutureAllocationPastStartIT |

Bổ sung assertion trong hai test C: FR_015_HoldExpiryIT và FR_044_BedRemindersIT. Đọc basis.schedulerRunId thật từ MySQL theo loại/id đối tượng riêng của test, kiểm UUID hợp lệ, đối chiếu JobLogCapture dùng chung với START đúng job và FINISH thành công. Assertion bắt được lỗi mất/đổi mã lần chạy hoặc không ghi log kết thúc. Không thêm fake, không sửa common hoặc test người khác. Đây là kiểm bổ sung cho implementation có sẵn, không tuyên bố đã chạy chu kỳ TDD đỏ/xanh cho mã production mới.

Xác minh JDK25, MySQL8.0; nạp TEST_DB_* cùng DB_PASSWORD/DB_RETENTION_PASSWORD vào process, không in giá trị:

```powershell
.\mvnw.cmd spotless:apply "-DspotlessFiles=.*FR_015_HoldExpiryIT.java,.*FR_044_BedRemindersIT.java"
.\mvnw.cmd test failsafe:integration-test failsafe:verify "-Dtest=SchedulerScopeArchTest,TimeArchTest" "-Dit.test=FR_015_HoldExpiryIT,FR_023_FutureAllocationPastStartIT,FR_044_BedRemindersIT,FR_044_ReminderAtomicityIT,SchedulingIT"
```

Spotless định dạng đúng hai file; lệnh kiểm exit0:4test kiến trúc,12IT (SchedulingIT2,FR_0153,FR_0232,FR_0444,atomicity1),0failure/error/skip. Log: C:/Users/nguye/AppData/Local/Temp/eldercare-c-uuid-review-20261030.log; định dạng: eldercare-c-uuid-spotless-20261030.log. Không chạy clean verify toàn kho hoặc kiểm R-15/T066 trong lượt này. Gỡ riêng C khỏi dòng review UUID, giữ việc B. Chưa commit/push/hoàn tất merge.


## 7. Review consumer M03 cho002/008/016 — 2026-10-30

Phạm vi: các dòng review tên tầng002, trường báo cáo016, ghép consumer008, sự kiện tỷ lệ và API giường/bàn giao trong team-plan8. Không đổi source/API/test/migration trong lượt này; chỉ ghi kết luận và cập nhật phần việc còn lại. Các giới hạn lịch sử ở mục4 vẫn áp dụng; chưa làm T066/R-15 hoặc nghiệm thu feature.

| Phần review | Kết quả và bằng chứng |
| --- | --- |
| FloorAreaNamesForAccount | Gọi LocationStructureApi.floor, giữ Optional rỗng khi không có tầng, không đọc bảng002; FR_041c_FloorAreaNameIT1/1 xanh trên bean thật |
| BedStateOnDate | Bản thật trả roomId và phân biệt HOLD_WAITING_RECORD/ABSENT_KEEP_BED thành WAITING_RECORD/ABSENT_RESIDENT; sự kiện HELD không biết thì báo lỗi; consumer016 nhóm theo đúng holdReason và phòng |
| AwaitingReadyInterval | Bản thật cắt from/to theo cửa sổ hỏi nhưng giữ startedAt gốc; consumer016 dùng startedAt cho mốc/khoá nguồn và thời lượng đầy đủ; BedReportingFieldsIT2/2,SC_008_BedReportingIT7/7 và test chỉ tiêu xanh |
| BedFlagView | Nguồn thật trả bedId null cho WITHOUT_BED; SC_008_BedReportingIT kiểm trực tiếp. Không tìm thấy consumer production gọi flaggedBeds ngoài producer trong checkout, nên chưa nghiệm thu việc hiển thị của consumer từ kết quả này |
| Consumer vị trí/bàn giao008 | AssignmentSources dùng ResidentLocationApi; ShiftLocationReferences cài currentReferences; HandoverSources đọc BedStatisticsApi.stateAt cho trạng thái giường hiện hành, không gọi BedStatusApi.currentState; AssignmentIT31,HandoverIT25 xanh |
| Sự kiện phân bổ | StaffingRatioEvents nghe BedAllocationStarted/Closed/Transferred; sự kiện chuyển kiểm soát có test payload producer; StaffingRatioIT14/14 kiểm consumer sau commit, tầng cũ/mới, biên đóng và vị trí nghỉ bán trú |
| Trigger003 | DBR_09_AllocationSchemaIT12/12 và DBR_23_RecordTablesIT1/1 xanh, gồm chống chồng/bất biến và cấm sửa/xóa lịch sử bằng tài khoản ứng dụng lẫn quản trị. Không thay thế kiểm migration gộp sau này |

### Phần chưa đóng sau review

1. FakeHandoverLocations.stateAt trả roomId=0 và holdReason=null, kể cả khi fixture beds có roomId; state(bed,"HELD") không cấu hình được lý do giữ. awaitingReady dùng awaitingSince làm from, không giao với from/to được hỏi và có thể trả khoảng không hợp lệ khi hỏi trước lúc bắt đầu. Đây là thiếu sót của fake chung hiện có, không phải kết luận bản thật sai. Dòng review trường bổ sung016 chuyển thành việc C sửa fake bằng test trước; không tạo fake thứ hai. Test reporting016 hiện dùng stub riêng và các IT M03 dùng nguồn thật nên kết quả xanh không bảo vệ thiếu sót fake này.
2. BedReportingService đã cài BedHandoverAlertApi.byFloor nhưng 008 gọi AllocationHandoverApi.atFloor; chưa có bean production cài AllocationHandoverApi hoặc LocationApi. ResidentLocationApi có bản thật và008 đã dùng. Giữ dòng nguồn thật của C; không coi hai API cảnh báo hoặc hai API vị trí là tự thay thế cho nhau.
3. Listener AllocationStarted/AllocationClosed/BedChanged cũ vẫn có ở008; chưa tìm thấy producer phát hai sự kiện AllocationStarted/AllocationClosed. Giữ dòng C/E chốt sự kiện cũ, không xóa trong lượt review ba sự kiện thật. Test consumer chứng minh nhận sự kiện thật, không phải kiểm toàn luồng mọi producer/callback của cả kho.
4. Contracts008 mục2.5 còn ghi BedStatusApi.currentState trong khi consumer đã dùng BedStatisticsApi.stateAt; thêm việc E đồng bộ tài liệu theo mã ghép, không thêm lại chữ ký cũ ở003. Không sửa phần006 của D hoặc tài liệu008 của E trong lượt này.
5. Review độc lập của A/E về consumer016/008 và các giới hạn Phase11 vẫn còn. Lỗi async tỷ lệ đã bàn giao E ở lượt API005 vẫn giữ, không tự kết luận đã hết vì bộ test lần này xanh.

### Xác minh mới

JDK25 dùng riêng process; TEST_DB_* và DB_PASSWORD/DB_RETENTION_PASSWORD nạp từ backend/.env, không in giá trị. Từ backend:

```powershell
.\mvnw.cmd test failsafe:integration-test failsafe:verify "-Dtest=MetricBedStatusTest,MetricBedAwaitingReadyTest,MetricShiftStaffingRatioTest,StaffingRatioCalculatorTest,StaffingSourceContractsTest,AssignmentSourcesTest" "-Dit.test=FR_041c_FloorAreaNameIT,BedReportingFieldsIT,SC_008_BedReportingIT,StaffingRatioIT,HandoverIT,AssignmentIT,BR_M03_21_ControlledTransferIT,DBR_23_RecordTablesIT,DBR_09_AllocationSchemaIT"
```

Exit0:19unit/6lớp và105IT/9lớp,0failure/error/skip. Chi tiết IT: BedReportingFields2,ControlledTransfer12,AllocationSchema12,RecordTables1,FloorAreaName1,BedReporting7,Assignment31,Handover25,StaffingRatio14. Log C:/Users/nguye/AppData/Local/Temp/eldercare-c-m03-consumer-review-20261030.log. Không sửa Java nên không chạy lại Spotless; không chạy clean verify toàn kho.

Team-plan: xóa hai dòng chỉ chờ C review tên tầng và consumer tỷ lệ; dòng trường báo cáo giữ phần sửa fake; dòng ghép chỉ còn D/E; dòng API giường giữ A/E review. Plan/progress ghi kết quả; không thêm checkbox, chưa commit/push/hoàn tất merge.


## 8. Review nguồn ca/phân công M09 — 2026-10-30

Phần C review LocationReferences/CareShiftPort tối thiểu và nối consumer phân công003 đã được đối chiếu cùng nguồn lịch sử M09. ShiftLocationReferences trả tham chiếu ca/phân công/Trưởng tầng và giữ everReferenced; AssignmentIT kiểm nguồn thật. DutyApi/AssignmentApi dùng lịch sử phụ trách và mốc cắt phạm vi,không mất quá khứ khi thay/vắng. Chi tiết,phân biệt test nguồn thật/fake và phụ thuộc005 ở [api-008-review.md mục4](../005-daily-care-tasks/api-008-review.md#4-review-nguồn-caphân-công-m09-theo-thời-điểm--2026-10-30).

Lượt kiểm JDK25/MySQL exit0:11unit+72IT,0failure/error/skip. Log TEMP eldercare-c-m09-time-review-20261030.log. Chỉ gỡ phần C review đã xong; giữD review,nguồn005/006/007 thật chưa hoàn tất,Q-262 và lỗi async tỷ lệ chờE. M09 chưa có adapter ShiftStaffPort chính thức005,dù CareShiftPort cũ có bản thật. Không đổi mã/test/API/migration hoặc checkbox; chưa làm T066 hoặc clean verify toàn kho; chưa commit/push/hoàn tất merge.


## 9. Sửa fake giường và nhật ký người cao tuổi của đính chính — 2026-10-30

Phạm vi: hai việc C được chủ dự án cho sửa tiếp sau review; không mở rộng sang provider bàn giao thật,R-15/T066 hoặc module người khác.

- FakeHandoverLocations: bỏ metadata phòng/tầng giả0; giữ snapshot từ fixture beds và thêm state(BedStateOnDate) để đặt roomId/holdReason đầy đủ. Giữ state(long,String) cùng alias trạng thái cũ cho consumer008; báo lỗi nếu trạng thái HELD được hỏi mà chưa cấu hình lý do. Lọc phạm vi tầng/phòng trước kiểm lý do để dữ liệu thiếu ở tầng khác không làm hỏng truy vấn. Khoảng awaitingReady giao với from/to,loại khoảng không giao và giữ startedAt gốc; không tạo fake thứ hai.
- AllocationCorrectionProvider: ghi đè residentId(id),đọc residentId từ AllocationCorrectionStore.required(id). CorrectionApi ghi CORRECTION_ADDED với người cao tuổi của bản phân bổ gốc; không thêm dòng audit riêng,không đổi chữ ký SPI hoặc đọc bảng của module khác.

### Bằng chứng test trước

1. FakeHandoverLocationsTest:5ca assertion đỏ trước implementation (phòng0,thiếu metadata giữ,không chặn lý do chưa đặt,không cắt khoảng,không loại khoảng không giao); sau sửa5/5xanh. Log eldercare-c-fake-red-20261030.log.
2. FR_049_correctionAuditKeepsResidentOfOriginalAllocation:1failure vì audit_log.resident_id NULL khi CorrectionApi chạy thật; sau override đã xanh. Log eldercare-c-correction-red-20261030.log.
3. Review độc lập bổ sung lỗi thứ tự kiểm phạm vi; thêm FR_024_missingHoldReasonOutsideTheRequestedFloorDoesNotBreakTheReport,tái hiện1error vì giường HELD tầng11 làm truy vấn tầng10 lỗi. Sửa lọc phạm vi trước reportState;ca cuối kiểm cả tầng10 thành công và tầng11 vẫn báo thiếu lý do. Log eldercare-c-fake-scope-red-20261030.log. Reviewer đọc bản cuối không còn finding cần sửa.

### Xác minh

JDK25 và MySQL test; biến TEST_DB_* cùng DB_PASSWORD/DB_RETENTION_PASSWORD chỉ nạp trong process từ backend/.env,không in giá trị.

- Verify toàn backend (trước lần chỉnh thứ tự lọc của fake):714unit/kiến trúc xanh;1537IT có8failure+9error,0skip,exit1/13:38phút. Toàn bộ253IT M03/52lớp xanh,gồm9ca đính chính;staffing251IT và care35IT xanh. So tên phương thức với eldercare-merge-20261030-clean-verify.log:16tên lỗi trùng mốc trước,thêm BR_M01_01_pastTheMilestone_alertsExactlyOnce_evenWhenTheJobRunsAgain lệch thời gian khoảng1ms. Không ghi tất cả17lỗi là baseline; không sửa M01 hoặc ApiContractIT để xanh. Log eldercare-c-two-fixes-verify-20261030.log.
- Kiểm cuối sau sửa fake theo review:17unit/kiến trúc (fake6,LayerArch10,ModuleBoundary1) và44IT (đính chính9,bàn giao25,trường báo cáo2,báo cáo giường7,ca M01 chạy riêng1),0failure/error/skip,exit0/01:25phút. Ca thời gian M01 không tái hiện khi chạy riêng;thêm dòngB xác minh thay vì kết luận đã sửa. Spotless:apply và Spotless:check đúng4file đều đạt;CRLF giữ nguyên. Log eldercare-c-two-fixes-final-20261030.log.

Lệnh kiểm cuối từ backend:

```powershell
.\mvnw.cmd spotless:apply test failsafe:integration-test failsafe:verify spotless:check "-DspotlessFiles=.*AllocationCorrectionProvider.java,.*BR_M03_24_AllocationCorrectionIT.java,.*FakeHandoverLocations.java,.*FakeHandoverLocationsTest.java" "-Dtest=FakeHandoverLocationsTest,LayerArchTest,ModuleBoundaryTest" "-Dit.test=BR_M03_24_AllocationCorrectionIT,HandoverIT,BedReportingFieldsIT,SC_008_BedReportingIT,BR_M01_01_TemporaryAbsenceOverdueIT#BR_M01_01_pastTheMilestone_alertsExactlyOnce_evenWhenTheJobRunsAgain"
```

Các log ở C:/Users/nguye/AppData/Local/Temp/. Không lặp verify toàn kho sau chỉnh fixture test cuối; bộ hồi quy liên quan đã chạy trên bản cuối. Không đổi nghiệp vụ,endpoint,hợp đồng công khai,migration hoặc thêm thuật ngữ nghiệp vụ;glossary không cần mục mới. Không sửa PENDING_* vì chưa làm các việc cấu trúc/gộp bảng tương ứng.

Team-plan bỏ phần fake củaC và phần C của dòng residentId,giữ E;thêm việcB xác minh thời gian. Tasks cập nhật mô tả T047 và ghi chú sửa hẹp,không đổi checkbox hoặc nghiệm thu toàn phase. Hai sửa hiện ở checkout;chưa commit/push/hoàn tất merge hoặc vào develop.

## 10. Nguồn AllocationHandoverApi thật — 2026-10-31

V003_07 thêm doctor_confirmation_requested_at nullable trên bed_allocation; không thêm bảng,không sửa migration cũ. Writer trở về ghi BusinessTime khi cờ chuyển false→true,giữ mốc nếu còn pending;chuyển kiểm soát ghi mốc lệnh cho dấu trên phân bổ mới. Legacy thiếu mốc giữNULL,không suy từ start_at. BedReportingService cài AllocationHandoverApi;đọc view đã áp đính chính cùng timestamp gốc,chỉ trả phân bổ ACTIVE còn hiệu lực,cùng tầng,đang chờ bác sĩ,và mốc đã biết không sau thời điểm hỏi. Đây là nguồn dấu đang mở,không dựng lại lịch sử mọi dấu đã gỡ. API/DTO giữ chữ ký;contracts/glossary/data-model mô tả metadata.

TDD và kiểm liên quan:

- RED trước implementation:7test lỗi vì thiếu bean AllocationHandoverApi hoặc cột timestamp;log eldercare-c-allocation-handover-red-20261031.log.
- Lượt GREEN đầu:38/39 qua;test HTTP008 không thấy fixture chưa commit vì consumer đọc song song. Sửa riêng test C dùng NOT_SUPPORTED để dữ liệu nhìn thấy từ worker;dọn ca bằng CANCELLED và đóng/gỡ dấu phân bổ sau test. Không đổi production để che lỗi fixture.
- GREEN cuối:39IT/3lớp,0failure/error/skip,exit0/02:54phút;FR_024a_AllocationHandoverIT5,FR_024_AllocationStatusParticipantIT22,BR_M03_21_ControlledTransferIT12. Có kiểm id/người/phòng,thời điểm thực khác start_at,tầng,trạng thái,gỡ dấu,legacyNULL,lặp writer giữ mốc,và GET source-records008 đọc provider003 thật. Spotless:apply đúng10file Java thành công. Log eldercare-c-allocation-handover-final-20261031.log.
- Review độc lập bản cuối không còn finding về timestamp,bộ lọc,legacy và fixture song song. Không sửa source/test củaE hoặc đưa fake nguồn bàn giao vào ứng dụng.

Lệnh từ backend (JDK25,MySQL,biến môi trường nạp kín từ .env):

```powershell
.\mvnw.cmd spotless:apply test-compile failsafe:integration-test failsafe:verify "-DspotlessFiles=.*AllocationHandoverView.java,.*AllocationCommandStore.java,.*ControlledTransferStore.java,.*ControlledTransferService.java,.*BedReportingSnapshot.java,.*BedReportingStore.java,.*BedReportingService.java,.*FR_024a_AllocationHandoverIT.java,.*FR_024_AllocationStatusParticipantIT.java,.*BR_M03_21_ControlledTransferIT.java" "-Dit.test=FR_024a_AllocationHandoverIT,FR_024_AllocationStatusParticipantIT,BR_M03_21_ControlledTransferIT"
.\mvnw.cmd verify
```

Log trong C:/Users/nguye/AppData/Local/Temp/. Kết quả verify toàn kho được ghi tiếp sau khi lệnh kết thúc. T061/T063 giữ[ ];LocationApi,R-15/T066 và review consumer độc lập vẫn còn. Team-plan bỏ riêng phần C cài AllocationHandoverApi,giữ LocationApi và A/E review. Chưa commit/push/hoàn tất merge.
Kết quả verify bản cuối:exit1/27:02phút,715unit/kiến trúc xanh;1542IT có8failure+9error,0skip;Spotless:check toàn2230file sạch. M03:258IT/53lớp,có257qua và1failure T064_BedListPerformanceIT;care35IT vàstaffing251IT đều xanh,gồm HandoverIT25ca và nguồn mới5ca. So với eldercare-c-two-fixes-verify-20261030.log:16phương thức lỗi trùng;ca M01 lệch thời gian trước đó không đỏ lần này;thêm ca hiệu năng M03 (không gọi BedReportingStore.flags hoặc AllocationHandoverApi). Không kết luận16lỗi là đã sửa;gate toàn kho vẫn đỏ.

Hiệu năng M03:trong verify samples=[873,873,877,886,890,970,977,985,1012,1410]ms,median930ms,max1410ms vượt1000ms. Chạy riêng không đổi mã/ngưỡng bằng `mvnw.cmd failsafe:integration-test failsafe:verify -Dit.test=T064_BedListPerformanceIT` vẫn lỗi,exit1/01:29phút;samples=[711,755,788,846,881,902,916,1033,1243,1355],median891.5ms,max1355ms. Đây là lỗi còn tái hiện củaT064,không gọi là baseline hoặc đã hết. Đường gọi BedController→BedQueries→BedStore/kiểm quyền theo giường không đi qua nguồn bàn giao vừa cài;chưa định lượng nguyên nhân hiệu năng,không sửa tối ưu ngoài phạm vi triển khai nguồn. Thêm dòngC theo dõi ở team-plan,giữT064[ ]. Log eldercare-c-allocation-handover-verify-20261031.log và eldercare-c-allocation-handover-performance-20261031.log. Không lặp verify nếu chưa sửa mã;không commit/push/hoàn tất merge.