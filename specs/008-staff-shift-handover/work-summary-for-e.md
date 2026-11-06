# Tổng hợp công việc của E: đã làm gì, ở đâu, đã kiểm gì

Ngày tổng hợp: 2026-10-31. Người thực hiện phần E: ptlog18. Báo cáo này gom các công việc có bằng chứng trong nhật ký, mã và biên bản của các worktree đang giữ trên máy.

**Trạng thái hiện tại: phần độc lập của 008 T050 đã cài và kiểm; T050 còn mở vì chờ API quét sự kiện của A. Toàn bộ feature 008 và 010 đều chưa được coi là nghiệm thu xong.**

## 1. Code và tài liệu đang nằm ở đâu?

Thư mục gốc kho trên máy:

```text
C:/Users/PHAN THANH LONG/Capstone project/Elder_Care
```

Phần đang làm gần nhất nằm trong `.worktrees/008-q262`, nhánh `impl/008-q262`, nền develop `d2bd7de0`. Các phần FR-057, mobile và 010 vẫn có bản đang làm ở những worktree riêng trong bảng dưới. Worktree là một thư mục làm việc của một nhánh Git; vì thế mở terminal ở gốc kho có thể thấy nhánh và thay đổi khác với terminal ở `.worktrees/008-q262`.

| Phần việc | Thư mục tính từ gốc kho | Nhánh |
| --- | --- | --- |
| Thông báo đính chính bàn giao FR-057 | Gốc kho | impl/008-fr057-handover-notifications |
| Review API thống kê và nguồn ca 008 | .worktrees/008-api-review | docs/008-api-review |
| Sửa fixture lưu giữ | .worktrees/008-governance-fixture | impl/008-governance-fixture |
| Mobile “Ca của tôi” | .worktrees/008-my-shifts-mobile | impl/008-my-shifts-mobile |
| Mobile “Phân công” và hợp đồng bổ sung | .worktrees/008-mobile-assignments | impl/008-mobile-assignments |
| Sửa fixture truy vấn ca | .worktrees/008-shift-query-fixture | impl/008-shift-query-fixture |
| Nền chi phí phát sinh 010 | .worktrees/010-incurred-costs | impl/010-incurred-costs |
| Q-262, cấu trúc mới và gộp bảng 008 | .worktrees/008-q262 | impl/008-q262 |

Các thay đổi chưa commit được liệt kê ở [phụ lục từng file](work-summary-for-e-files.md). Danh sách của riêng nhánh hiện tại nằm ở [r15-implementation-files.md](r15-implementation-files.md).

**Chưa commit, push, mở PR hoặc merge các thay đổi đang giữ này.** Trong bước tổng hợp báo cáo này không đổi nhánh hoặc sửa code nghiệp vụ.

## 2. Những việc đã làm trước nhánh Q-262/R-15

Bảng này lấy từ biên bản và progress của từng worktree. Kết quả test áp dụng cho đúng phiên bản được ghi tại đó; các phần này cần đối chiếu khi tích hợp với develop hiện tại.

| Việc | Đã làm và ý nghĩa | Bằng chứng / phần còn lại |
| --- | --- | --- |
| FR-057: thông báo khi đính chính bàn giao | Tra người đã xác nhận qua AccountApi.byId; thông báo tới nhóm Người phụ trách ca hiện tại bằng ShiftLead. Giữ mức Nhẹ và nội dung chung đã duyệt. | [Progress FR-057 — dòng 55](../../../../docs/progress.md). Ba test đơn vị và 27 HandoverIT xanh; lượt chẩn đoán toàn kho có lỗi nên chưa coi toàn kho đạt. |
| Sửa fixture M09GovernanceIT | Thay năm chỗ lấy LAST_INSERT_ID qua lệnh truy vấn riêng bằng helper trả ID từ chính lần INSERT. | [Progress fixture lưu giữ — dòng 58](../../../008-governance-fixture/docs/progress.md). Test lớp 7/7 xanh; full verify của mốc đó còn lỗi nền B/C. |
| Review API cho 009/016 | Đối chiếu API thống kê, người có ca và Bác sĩ trực với hợp đồng; kiểm dữ liệu/phạm vi thực tế. Phát hiện lỗi lấy ID trong fixture ShiftQueriesIT. | [Biên bản review API](../../../008-api-review/specs/008-staff-shift-handover/api-review-2026-10-28.md). Review không phải là cài thêm các API nghiệp vụ mới. |
| Sửa fixture ShiftQueriesIT | Dùng AssignmentFixtures.insert cho năm chỗ lấy ID; có test dùng hai kết nối MySQL để tái hiện lỗi trước sửa. | [Progress fixture ca — dòng 58](../../../008-shift-query-fixture/docs/progress.md). 8/8 test lớp xanh; giữ assertion nghiệp vụ. |
| Mobile “Ca của tôi” T016b | Màn tháng, chi tiết ca, người phụ trách/người cùng ca, giờ Việt Nam, tải/lỗi/rỗng/Thử lại; thêm getList và tab theo quyền trong phạm vi A đã duyệt. | [Progress Ca của tôi — dòng 59](../../../008-my-shifts-mobile/docs/progress.md). 77/77 Flutter test xanh tại nhánh đó; còn kiểm thiết bị/backend thật và tích hợp. |
| Hợp đồng sửa Nháp và đọc kết quả phân công | Sau khi A/chủ dự án duyệt, bổ sung năm thao tác HTTP; sửa Nháp nguyên tử, giữ ID/If-Match/chống lặp; đọc người phụ trách và danh sách thiếu từ backend. | [Biên bản Phân công, gồm lượt review sau sửa](../../../008-mobile-assignments/specs/008-staff-shift-handover/assignment-verification-2026-10-29.md). Giữ 62 thao tác cũ của nền tại thời điểm đó. |
| Mobile “Phân công” T022b | Xem trước/cảnh báo; sửa/xóa Nháp, hủy/kết thúc, sao chép, giao/rút ngắn Trưởng tầng, đọc kết quả backend. Review tiếp sửa lỗi preview cũ và kết quả tìm kiếm trả chậm ghi đè lựa chọn mới. | [Biên bản Phân công, gồm lượt review sau sửa](../../../008-mobile-assignments/specs/008-staff-shift-handover/assignment-verification-2026-10-29.md). Lượt cuối 75/75 Flutter test xanh; T022b vẫn mở vì còn nguồn thật/lựa chọn/thiết bị và tích hợp. |
| Chuẩn bị và làm nền 010 | Dịch hợp đồng đã được duyệt, viết hàm tính phí độc lập, khai API/DTO/sự kiện/port và fake chung, tạo ba bảng riêng cùng test; đồng bộ gói/cấu trúc theo luật mới. | Xem mục 7 của báo cáo này. Chưa cài xong các luồng chi phí thực tế. |

Ví dụ lỗi fixture: một kết nối INSERT được bản ghi ID 15; lệnh SELECT LAST_INSERT_ID chạy trên kết nối khác có thể trả 0 hoặc ID cũ. Khi đó test nối sai khóa ngoại dù nghiệp vụ cần kiểm không sai. Helper lấy ID từ chính lần INSERT giải quyết nguyên nhân này.

**Số task phải đọc theo phiên bản của nhánh.** Nhật ký FR-057 cũ gọi việc đó là T050; tasks hiện tại dùng T050 cho gộp bảng R-15, T051 cho Q-262. Luôn đối chiếu cả tên việc, nhánh và ngày.

## 3. Sửa Q-262: quyền phụ thuộc người có mặt trong đúng ca

Trước sửa, hệ thống chủ yếu nhìn việc tầng đã được giao Trưởng tầng hay chưa. Nếu đã giao nhưng người đó không trực ca, đã vắng hoặc nghỉ được duyệt, Người phụ trách ca vẫn có thể bị chặn ghi kết quả kiểm tra chất lượng.

Sau sửa, hệ thống kiểm người được giao Trưởng tầng có mặt trong **đúng ca đang xét**, dùng mốc thành viên/vắng/nghỉ và thời gian nghiệp vụ.

Ví dụ: Trưởng tầng bắt đầu vắng lúc 10:00. Trước 10:00, ngoại lệ chưa mở; từ đúng mốc vắng, Người phụ trách ca được xét theo Q-262. Trưởng tầng đang trực tầng khác cũng không chặn ca này. Ca Nháp hoặc không tồn tại không tự sinh quyền.

File chính:

- [DutyDirectory.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/DutyDirectory.java): sửa shiftHasNoFloorManager.
- [ShiftQueries.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/ShiftQueries.java): thêm helper presentInShift, dùng dữ liệu có mặt của ca.
- [DutyDirectoryIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/DutyDirectoryIT.java): bảy tình huống gồm không trực, biên giờ vắng, nghỉ đã duyệt, có mặt, trực tầng khác, ca Nháp, ca không tồn tại.

Đã thấy bốn thất bại nghiệp vụ trong bảy test trước sửa; sau sửa bảy test xanh. T051 hiện [x]. A/C vẫn cần review phía consumer; toàn luồng kiểm tra chất lượng 014 còn phần của C. Chi tiết ở [q262-review.md](q262-review.md), mục 2–4.

## 4. T050a: sửa cấu trúc để đúng luật mới của leader

Bảy nhóm assignment, credential, duty, handover, ratio, shift, staff được chuyển vào dưới staffing/internal. Controller về staffing/web. SQL của chín dịch vụ được chuyển vào các Store tương ứng; M09Retention và factory tạo provider được đặt ở persistence.

Ví dụ đường dẫn của mã ca:

```text
Trước: staffing/shift/internal/service/...
Sau:   staffing/internal/shift/service/...
```

| Thành phần | Hiểu đơn giản | Cách sửa |
| --- | --- | --- |
| service | Kiểm quyền, điều kiện và thực hiện lệnh nghiệp vụ | Gọi phương thức Store thay cho tự giữ câu SQL |
| persistence / Store | Đọc và ghi dữ liệu | Nhận truy vấn từ dịch vụ, đặt trong phương thức có mục đích cụ thể |
| domain | Dữ liệu và quy tắc tính toán | Đặt theo nhóm chức năng |
| web | Nhận/trả yêu cầu HTTP | Gom controller, request, response đúng cấu trúc README |

Đã xóa các ngoại lệ của E trong LayerArchTest để mã E phải chịu luật chung; các ngoại lệ của thành viên khác được giữ. Trước sửa ba luật đỏ; sau sửa 19 test tập trung xanh. T050a hiện [x].

Nhiều đường dẫn cũ hiện chữ D trong Git vì file đã chuyển chỗ, trong khi file ở chỗ mới còn chưa stage. Để xem đầy đủ cần đọc cả danh sách file mới. [r15-architecture-files.md](r15-architecture-files.md) ghi các đường dẫn đã chuyển; [r15-architecture-review.md](r15-architecture-review.md) giữ kết quả kiểm tại mốc T050a.

## 5. T050/R-15: đã gộp dữ liệu như thế nào?

### 5.1. Bảng giảm, dữ liệu được chuyển sang chỗ lưu mới

008 trước đó có 25 bảng riêng. Migration chuyển dữ liệu để còn tám bảng riêng; một phần dùng bảng chung đã có của A, một phần nằm trong JSON hoặc bảng riêng được mở rộng.

Migration là script thay đổi cấu trúc và chuyển dữ liệu trong cơ sở dữ liệu. Hai file mới:

- [V008_94__shared_staffing_legacy_ids.sql](../../backend/src/main/resources/db/migration/V008_94__shared_staffing_legacy_ids.sql): thêm chỉ mục định danh cũ riêng M09 trên bảng chung; A cần review.
- [V008_95__staffing_table_consolidation.sql](../../backend/src/main/resources/db/migration/V008_95__staffing_table_consolidation.sql): chép dữ liệu, đổi tham chiếu, giữ ràng buộc và thu hồi quyền bảng cũ trước khi DROP.

| Dữ liệu trước đây | Chỗ lưu sau chuyển | Ý nghĩa |
| --- | --- | --- |
| Bộ phận, loại chứng chỉ, phạm vi hành nghề, mẫu ca | catalog_item qua CatalogApi | Dùng nền danh mục chung |
| Lịch sử phụ trách bộ phận và ca | status_history qua StatusHistoryApi | Giữ khoảng hiệu lực để tra quyền ở thời điểm quá khứ |
| Bản công bố, ý kiến/phản hồi bàn giao, lần tính tỷ lệ | record_event qua RecordEventApi | Giữ các bản ghi chỉ thêm |
| Lần import nhân viên | simple_record qua SimpleRecordApi | Giữ thông tin file, số dòng, người tạo và thời điểm |
| Liên kết phạm vi của chứng chỉ | JSON practice_scope_ids trong credential | Giữ đủ phạm vi và ánh xạ đúng ID |
| Giao Trưởng tầng | staff_assignment với kind=FLOOR_MANAGER | Cùng bảng với phân công CARE, kiểm rõ từng loại |
| Mục và nhận định bàn giao | JSON items/assessments trong shift_handover | Gắn nội dung với đúng bàn giao cha |
| Khóa tầng và dấu xử lý job | TechnicalLocks / ReminderMarkers | Khóa đồng thời và chống xử lý lặp dùng nền chung |

Tám bảng riêng còn lại: staff_profile, staff_department, credential, shift_schedule, work_shift, shift_staff, staff_assignment, shift_handover.

Mã được sửa để dùng các API chung thật của A. Các câu xóa dữ liệu chung của provider lưu giữ chạy trên kết nối tài khoản lưu giữ đúng phạm vi loại/ID. Không dựng thêm bản giả trong ứng dụng thật.

### 5.2. Giữ ID cũ để nhật ký và quyền không trỏ nhầm

Theo lựa chọn của bạn, ID công khai/logic cũ được giữ bằng metadata legacy; khóa ngoại trong cơ sở dữ liệu dùng ID vật lý mới của bảng chung.

Ví dụ minh họa kỹ thuật:

| Loại | ID cũ mà nghiệp vụ dùng | ID vật lý sau gộp, ví dụ |
| --- | --- | --- |
| Bộ phận | 7 | 501 |
| Loại chứng chỉ | 7 | 502 |

Hai số 7 thuộc hai loại khác nhau. Store kiểm đúng loại rồi tìm dòng vật lý tương ứng. Dãy ID cấp mới được đặt vượt miền ID cũ để tránh che mất một bản ghi cũ.

Không sửa các dòng nhật ký/đính chính cũ. ID phạm vi hành nghề do E trả vẫn khớp các ID logic đang được dữ liệu chuyên môn M15 tham chiếu. Tên loại bản ghi không đổi nên không cần đổi loại qua LegacyRecordTypeSource trong phần này.

Mã liên quan: [StaffingCatalogStore](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingCatalogStore.java), [StaffingHistoryStore](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingHistoryStore.java), [StaffingRecordStore](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingRecordStore.java). Kiểm bằng StaffingCatalogIdentityIT, gồm trường hợp nhiều loại cùng ID logic 7001.

### 5.3. Bảo vệ bàn giao sau khi nội dung nằm trong JSON

JSON ở đây là nội dung có cấu trúc được lưu trong một cột. Tôi đã giữ các quy tắc:

- Mỗi bàn giao có bộ đếm next_item_id riêng, cấp ID dưới khóa của bàn giao cha.
- Xóa một mục rồi thêm mục mới không tái sử dụng ID đã xóa.
- Hai bàn giao có cùng itemId cục bộ không ghi đè nhau.
- Khi PREPARED, phần đã chốt giữ nguyên; chỉ thêm đúng mục AFTER_PREPARATION được phép.
- Khi CONFIRMED, trigger chặn sửa/xóa nội dung.
- Đính chính trỏ ID bàn giao gốc; ID mục nằm trong nội dung đính chính.

Trigger là chốt kiểm trong MySQL, giúp chặn thao tác trái quy tắc ngay cả khi ghi trực tiếp vào DB. COMMENT DataGroup khai bảng thuộc nhóm dữ liệu nào; bảng nhiều nhóm có locked_at và trigger theo dòng.

Đã bỏ đúng 25 ngoại lệ E trong PENDING_DATA_GROUP sau khi test nhóm dữ liệu xanh. Mã chính: [HandoverStore](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/persistence/HandoverStore.java) và [HandoverJsonData](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverJsonData.java); test HandoverEmbeddedItemsIT, HandoverSchemaIT, M09RecordProtectionIT.

### 5.4. Job: giữ chống lặp và truy được nhật ký

Job là tác vụ chạy theo lịch, như chuyển trạng thái ca khi tới giờ. M09Jobs dùng ReminderMarkers; dấu đã xử lý và tác động được commit cùng transaction. Transaction là nhóm thao tác cùng thành công hoặc cùng hoàn tác. Nếu lỗi, dấu cũng hoàn tác để có thể thử lại.

SchedulerRuns tạo UUID của lần chạy và ghi log. UUID được đưa vào căn cứ nhật ký của các tác vụ E để tra ngược cùng lần chạy; có kiểm khôi phục MDC của luồng gọi. Test có ca chạy lại, lỗi rồi thử lại và hai worker chạy cùng mốc.

Mã chính: [M09Jobs](../../backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09Jobs.java), M09TimedTask và các tác vụ tương ứng. Bảng m09_job_run cũ được thay theo thiết kế R-15.

## 6. Tôi đã review những gì, tìm ra gì và xử lý ra sao?

| Nội dung review | Phát hiện | Kết quả |
| --- | --- | --- |
| Quyền Q-262 | Chỉ xét Trưởng tầng được giao, chưa xét có mặt trong đúng ca | Đã sửa, bảy test xanh |
| API báo cáo 016 | Cần giữ số nhân viên/trọng số/mẫu/giờ ca, bàn giao cũ và phạm vi | Đã đối chiếu mã thật và test; không dùng fake làm bằng chứng lọc thực tế |
| Nguồn ca/Bác sĩ trực 009 | Cần đúng tầng, thời điểm có mặt và dấu Bác sĩ trực | Đã đối chiếu truy vấn và test |
| Fixture sau PR #66 | Đổi nền nghiệp vụ chuyên môn/phạm vi sang catalog_item | Đã review hai test, giữ ý nghĩa kiểm |
| Cấu trúc/SOLID | Gói đặt sai tầng và SQL trong dịch vụ | Đã chuyển đúng chỗ, bỏ ngoại lệ E, test kiến trúc xanh |
| Định danh khi gộp | ID vật lý mới có thể làm nhật ký hoặc quyền trỏ nhầm | Giữ ID logic/legacy; test loại và ID trùng xanh |
| JSON bàn giao | Cấm hết cập nhật sau lập sẽ làm mất lệnh thêm phần phát sinh | Giữ phần cũ, cho thêm đúng phần phát sinh; test xanh |
| Giao Trưởng tầng | CHECK mới từng bắt buộc lý do trong khi hợp đồng cho tùy chọn | Đã bỏ điều kiện thừa, giữ actor/điều kiện hợp lệ |
| Fixture sau chuyển bảng | Tên bảng cũ, trùng tên mẫu, ID trùng dữ liệu import, thiếu transaction | Đã sửa fixture; không hạ assertion nghiệp vụ |
| Lưu giữ tỷ lệ | Chưa tìm được lịch sử khi ca Nháp đã bị xóa | Còn một test đỏ; chờ API A |
| Thông báo nguồn phân bổ của C | Interface/DTO nguồn bàn giao chưa khớp bản thật | Giữ dòng chờ C; không tự lấy giờ hiện tại thay thời điểm nguồn |

Review chi tiết mới nhất nằm ở [r15-implementation-review.md](r15-implementation-review.md). Review phía chủ module A/C vẫn ghi chờ trong team-plan; việc tôi đã rà mã không tự thay cho phần duyệt đó.

Các thay đổi leader cũng đã được rà riêng trong worktree 010: đổi gói gốc/module, luật cấu trúc, bảng chung, các Q-394–397. Chứng cứ ở [Review thay đổi leader 2026-10-30](../../../010-incurred-costs/specs/010-incurred-costs/leader-changes-review-2026-10-30.md) và [Biên bản cấu trúc/đồng bộ 010](../../../010-incurred-costs/specs/010-incurred-costs/architecture-fix-2026-10-30.md). Các mục này ghi rõ phần đối chiếu mã của leader và phần E sửa tiếp.

## 7. Spec 010: đã làm tới đâu?

Phần này đang ở .worktrees/010-incurred-costs, nền đã kiểm gần nhất 73cc9d61; chưa được nhập vào nhánh 008-q262.

| Phần | Đã làm | Còn lại |
| --- | --- | --- |
| Hợp đồng | Bản dịch được duyệt; giữ 28 đường dẫn/29 thao tác, cấu trúc đã đối chiếu | 17 phản hồi thành công thiếu schema cần chốt trước endpoint liên quan |
| Hàm tính phí | DongRounding, ResidencyDayFee, DayCareSessionFee; đã áp Q-395 bỏ hệ số nội trú | BillingPeriod và dữ liệu hợp đồng/ngày giải phóng giường từ B |
| API | FamilyPortalCostApi, FinalPeriodCostApi, CostStatisticsApi, DTO, tám sự kiện | Bản cài nghiệp vụ thật và nơi phát/xử lý sự kiện thuộc task sau |
| Port và dữ liệu thử | CostForBalancePort, PurchaseBalancePort; fake chung chính chủ cho test | Consumer/bản cài thật; không lấy fake làm nghiệm thu nghiệp vụ |
| Ba bảng riêng | cost_statement, cost_item, cost_source_index; test schema/khóa/quyền | Tích hợp trạng thái/marker/API chung của A; khai nhóm dữ liệu theo nền mới |
| Cấu trúc mới | Chuyển gói com.eldercare.backend.billing, dời năm lớp sai vị trí | Các luồng nghiệp vụ và các phase sau vẫn mở |

Theo tasks ở worktree 010: T001/T003/T004/T005a đã [x]; T002/T005/T006/T007/T008/T038 còn [ ]. T006 mới xong ba trong bốn hàm.

Lượt kiểm gần nhất của nhánh đó: 745 unit/arch xanh; 1480 IT còn 7 thất bại/9 lỗi tại chín lớp nền B/C; 15 IT billing xanh. Các số này áp dụng cho 010 trên 73cc9d61. Nền 008 hiện tại đã mới hơn, nên còn phải đối chiếu khi tích hợp; một số ghi chú chờ T108 trong báo cáo 010 cũ đã được báo cáo mới đính chính thành việc E tích hợp nền đã có.

Tài liệu đối chiếu: [Biên bản khai báo API 010](../../../010-incurred-costs/specs/010-incurred-costs/api-declaration-verification.md), [Biên bản ba bảng riêng 010](../../../010-incurred-costs/specs/010-incurred-costs/r15-storage-verification.md), [Biên bản cấu trúc/đồng bộ 010](../../../010-incurred-costs/specs/010-incurred-costs/architecture-fix-2026-10-30.md) và [Tasks ở worktree 010](../../../010-incurred-costs/specs/010-incurred-costs/tasks.md).

## 8. Kết quả test hiện tại phải hiểu thế nào?

| Lượt kiểm | Kết quả thực tế | Kết luận |
| --- | --- | --- |
| Full clean verify cho nhánh 008 hiện tại | 674 test đơn vị/kiến trúc xanh; 1514 IT có 8 thất bại/9 lỗi, không bỏ qua | BUILD FAILURE |
| Phần staffing trong 1514 IT trên | 285/286 xanh, một thất bại | Thiếu bước quét tỷ lệ không còn ca cha |
| Baseline B/C | Chín lớp giữ cùng phương thức và cùng số lỗi so với mốc trước | Chưa xử lý trong nhánh E |
| Kiểm bổ sung sau dọn dependency thừa | 10 test kiến trúc + 39 IT ca/tỷ lệ/báo cáo/hợp đồng xanh | BUILD SUCCESS cho đúng tập test này |

674 là số lượt test đơn vị/kiến trúc trên toàn backend. 286 test staffing nằm trong tổng 1514 IT. Mỗi số test gắn với một lần chạy và một phiên bản; không cộng các con số của nhiều worktree để suy ra số test riêng biệt.

Test đỏ nghĩa là kỳ vọng kiểm chưa đạt. Test xanh xác nhận đúng tình huống mà test đó kiểm. Vì vẫn còn test đỏ nên chưa thể báo toàn backend đạt, chưa thể đánh [x] T050.

Các lần chạy trước đã phát hiện lỗi rồi sửa; lượt bị ngắt hoặc chưa biên dịch được được ghi rõ trong biên bản. Riêng các test mới 010 từng đỏ ở bước biên dịch do chưa có lớp; không ghi chúng thành assertion nghiệp vụ đã thực thi đỏ.

Log gốc giữ ngoài thư mục target của worktree đang chạy để clean không xóa bằng chứng:

```text
ROOT/backend/target/008-q262-verification/
ROOT/backend/target/008-r15-verification/
```

Các file chính: r15-clean-verify.log, r15-full-summary.json, final-shift-regression-retry.log. Không cần gửi file .env hoặc mật khẩu cho người review.

## 9. Còn chờ ai và vì sao chưa đánh xong?

Các việc 010 bên dưới phản ánh snapshot 73cc9d61 đang giữ; khi tiếp tục phải rà develop mới để cập nhật phụ thuộc đã có.

| Người/phần | Việc còn lại | Ảnh hưởng |
| --- | --- | --- |
| A / 000 | RecordEventApi.scanBefore(kind, cutoff, afterId, limit), bản thật/fake chung/test theo đề xuất | Chặn đóng T050 |
| A / 000,016 | Review chỉ mục V008_94 và fixture FR_020_StaffingDashboardIT | Review phần bảng chung và test chủ module |
| A / 002, C / 014 | Review consumer của sửa Q-262 | Chốt liên thông quyền/luồng chất lượng |
| C / 003 | Chốt nguồn AllocationHandoverApi so với BedHandoverAlertApi và metadata thời điểm | Nghiệm thu nguồn cảnh báo bàn giao thật |
| B/C/D chủ nguồn khác | Các nguồn thật, dữ liệu và kiểm liên module theo team-plan | Nghiệm thu toàn feature 008 |
| E + các chủ phần liên quan | Tích hợp mobile cũ, kiểm thiết bị/backend thật, đo các SC | T016b/T022b chưa tích hợp; T030 bàn giao và T041b vắng ca chưa hoàn thành; còn Q38/đo SC |
| B / 004 và các chủ nguồn 010 | Đầu vào hợp đồng/tài chính/ngày giải phóng/người đại diện, các producer liên quan | Còn phần nền/nghiệp vụ 010 |
| E / 010 | Tích hợp API chung đã có, trạng thái dùng chung 010/017 và các task còn mở | Không phải tất cả việc 010 đều đang chờ người khác |

Vì sao cần scanBefore: một ca Nháp có lịch sử tính tỷ lệ rồi bị xóa. Lịch sử vẫn phải giữ đến hạn; nhưng API hiện tại đòi biết ca cha để liệt kê sự kiện. Khi ca không còn, duyệt các ca đang tồn tại sẽ bỏ sót lịch sử đó.

Đường xóa một sự kiện đã biết ID đã xanh với cả ID mới và ID legacy. Phần còn thiếu là **tìm được các sự kiện đủ hạn khi cha đã mất**. A cần bổ sung cổng đọc này; bạn đã chọn chờ A và tôi giữ đúng phạm vi đó.

Một test vẫn đỏ để chỉ ra thiếu sót: FR_037_retentionFindsExpiredRatioEvenWhenDraftShiftHasBeenDeleted. Không bỏ qua hoặc sửa kỳ vọng của test để báo xanh.

Đề xuất để chuyển A: [shared-event-scan-proposal.md](shared-event-scan-proposal.md), mục 1. Dòng chờ ở [team-plan.md — mục 8, dòng 121 ghi API quét](../../docs/team-plan.md). Các phụ thuộc khác của feature vẫn đọc theo mục 8 của team-plan.

## 10. File nào ghi loại thông tin nào?

| Muốn xem gì? | Mở file |
| --- | --- |
| Nhật ký đã làm, nhánh, test, còn lại | [progress.md của nhánh hiện tại](../../docs/progress.md); dòng 88–90 là Q-262, T050a và T050 tại mốc trước báo cáo |
| Task nào được [x], task nào còn [ ] | [tasks.md](tasks.md), T050/T050a/T051 và các task mobile |
| Quyết định triển khai/lệch kỹ thuật | [plan.md](plan.md) |
| Dữ liệu trước/sau gộp | [data-model.md](data-model.md), mục 0.1 |
| Tên tiếng Anh của phần E | [glossary.md — mục 14](../000-shared-foundation/glossary.md) |
| Review Q-262/API nguồn | [q262-review.md](q262-review.md) |
| Review chuyển cấu trúc | [r15-architecture-review.md](r15-architecture-review.md) |
| Review migration, test và giới hạn mới nhất | [r15-implementation-review.md](r15-implementation-review.md) |
| Danh sách file nhánh hiện tại | [r15-implementation-files.md](r15-implementation-files.md) |
| Danh sách file của cả tám worktree | [work-summary-for-e-files.md](work-summary-for-e-files.md) |
| Việc A cần cung cấp | [shared-event-scan-proposal.md](shared-event-scan-proposal.md) |
| Ai đang chờ ai/review gì | [team-plan.md — mục 8, dòng 121 ghi API quét](../../docs/team-plan.md) |

Cách đọc: đọc mục 3–6 để hiểu việc 008; mục 8 để hiểu kết quả test; mục 9 để biết còn chờ gì. Khi cần xem một file cụ thể, mở phụ lục rồi bấm tên file. Nếu trình xem không mở liên kết, dùng Ctrl+P trong VS Code để tìm tên file; Ctrl+G để tới số dòng ghi trong báo cáo.

## 11. Bước tiếp theo sau báo cáo

1. Chuyển đề xuất API cho A; chờ A chốt/cài và cung cấp trên develop.
2. Khi có API, đối chiếu các thay đổi của nhánh đang dở trước khi đồng bộ. Việc tạo/chuyển nhánh, commit và push tiếp tục theo quyền bạn đã giữ cho mình.
3. E nối phần quét tỷ lệ, chạy lại test đang đỏ và hồi quy; cuối bước chạy clean verify và đối chiếu baseline.
4. Khi test của task đạt, mới đánh [x] T050, sửa/xóa dòng đã xong trong team-plan và thêm progress. Việc commit/push/PR vẫn chờ bạn quyết định.
5. Mobile cũ và 010 được xử lý theo phần còn lại nêu trên; kết quả cũ cần được kiểm trên nền tích hợp mới.

Bảy worktree cũ đã được kiểm lại HEAD/status và SHA-256 của 155 file, ba stash được giữ. CRLF/UTF-8 không BOM đã được kiểm cho thay đổi nhánh hiện tại. Lượt lập báo cáo này chỉ sửa tài liệu, dùng lại bằng chứng test đã ghi; không chạy lại backend/web/mobile hoặc thay đổi cơ sở dữ liệu.
