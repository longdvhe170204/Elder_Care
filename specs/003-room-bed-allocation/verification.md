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

Tên file test trong bảng thuộc `backend/src/test/java/com/cambae/backend/m03/`, trừ nơi ghi khác. “Có test” chỉ nói phạm vi nêu trong cột cuối, không mặc định luồng/module khác đã tích hợp thật.

| Mã gốc | File kiểm thực tế | Phạm vi và giới hạn |
| --- | --- | --- |
| DBR-09 | `DBR_09_AllocationSchemaIT`, `FR_020_AllocationConcurrencyIT`, `FR_020_AllocationLocksIT`, `FR_020_AllocationRetryIT` | Cột sinh/UNIQUE, overlap, khóa và10luồng; nguồn ngoài qua API/fake đúng chủ |
| DBR-10 | `BR_M03_01_AllocationConditionsIT`, `DBR_26_BedSchemaIT` | Điều kiện FR-018 và CHECK thuộc tính giữ chỗ |
| DBR-23 | `DBR_23_RecordTablesIT`; `m15/RecordGroupTriggerIT.DBR_23_m03SqlRecordTablesHaveBothAppendOnlyTriggers` | Hai bảng nhóm3: bed_state_history, room_isolation_history; app và admin đều bị chặn UPDATE/DELETE trên hàng thật |
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
- T063 mapping đúng:004→m02;007→m05/m06;008→m09;019→m03. Không có lớp production cài các SPI003 cần từ những feature đó trong checkout; m07 thuộc006 và m14 thuộc016, không dùng hai gói này để kết luận nguồn007/019.

## 5. Gate cuối và thứ tự còn lại

`003-p11-clean-verify-final.log` đã kết thúc exit1/04:09phút:393test đơn vị/kiến trúc xanh;1073IT có6failure+9error,0skip. Đọc XML xác nhận227IT M03 xanh,15lỗi còn lại thuộc8lớpM01 đã bàn giaoB. Spotless1328file,0cần sửa. Lượt hiệu năng cuối:67–85ms, trung vị77.5ms cho10lần đọc sau warm-up. Frontend250test/lint/build qua; Flutter chưa chạy test vì thiếu hỗ trợ symlink. Không đánh thêm checkbox. Cần tách rõ:

1. T061: implementation ba API và các test đã có; còn giới hạn dữ liệu lịch sử được nêu trên, chờ gate toàn kho.
2. T062: contract đã đăng ký và kiểm các endpoint đã cài; còn pending chuyển thường là task trước, không gỡ giả; kiểm hai bảng nhóm3 đã bổ sung.
3. T063: chờ provider thật004/007/008/019, không thay bằng fake production.
4. T064: cần môi trường Flutter hỗ trợ symlink, demo đủ nguồn/luồng và phép đo SC-005 người dùng thực tế; không tự đổi cài đặt Windows.
5. T065: bảng truy vết đã đối chiếu; những hàng thiếu integration/workflow vẫn phải hoàn thiện ở task gốc. Không tuyên bố toàn bộ003 đã hoàn tất vì chỉ có bảng mapping.
