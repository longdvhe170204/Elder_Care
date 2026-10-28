# Implementation Plan: Chăm sóc hằng ngày

**Branch**: `005-daily-care-tasks` | **Date**: 2026-10-09, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-daily-care-tasks/spec.md`

## Summary

Feature 005 là module M04, phần 8.1 → 8.7 (UC-22 → UC-28). Nó cung cấp:
1. Kế hoạch chăm sóc theo phiên bản: Nháp, gửi duyệt, duyệt, có hiệu lực từ ngày hôm sau ngày duyệt trở đi, hết hiệu lực; so sánh phiên bản; báo điều dưỡng xem xét kế hoạch (mục A; UC-22, UC-23).
2. Trạng thái có mặt theo ngày của bán trú, điểm danh đến/về, giờ về dự kiến (mục B; UC-28).
3. Lệnh "Lập công việc của ca" do Người phụ trách ca hoặc Trưởng tầng thực hiện; hệ thống chỉ kiểm tra và chặn. Công việc phát sinh, Ghi nhận phát sinh, công việc làm lại. Vòng đời công việc và hủy tự động kèm lệnh nguồn (mục C; UC-24).
4. Checklist theo ca, công việc chung, nhận việc, phân lại; ghi nhận kết quả theo loại, ghi muộn, ngoại tuyến, đính chính (mục D; UC-25, UC-26).
5. Danh mục loại công việc (mục E).
6. Quy tắc từ kết quả: nước uống thiếu, ăn kém kéo dài, tâm trạng tiêu cực kéo dài (mục F).
7. Quá hạn: nhắc một lần, leo thang theo mức, xử lý, giao tiếp bàn giao với 008 (mục G; UC-27).
8. Thời khóa biểu cá nhân, chỉ xem (mục H).

Hướng kỹ thuật:
- Lệnh lập công việc theo lô: người lập tự nhập mọi dòng; backend kiểm tra từng dòng bằng một hàm (`KiemTraLapCongViec`) và trả đủ lỗi theo dòng; một dòng lỗi thì cả lô không lưu (research C-07). Không có endpoint gợi ý hay chép sẵn (Q-307).
- Chuyển trạng thái theo giờ, nhắc và leo thang do một job theo phút (SC-005 ≤ 1 phút, NFR-04), có chạy bù và nhắc gộp (C-12).
- Ràng buộc đưa xuống MySQL: cột sinh `UNIQUE` cho DBR-11, một phiên bản mở, DBR-12 (bỏ qua công việc Hủy), một kiểm tra đầu ca mỗi (ca, điều dưỡng); trigger khóa mục kế hoạch ngoài Nháp và khóa công việc ở trạng thái cuối (data-model mục 9).
- Hủy và đóng tự động là tác động kèm lệnh của actor ở module khác, trong cùng transaction: bean `ThamGiaChuyenTrangThai` của 001; `CongViecCaApi` cho 008, 015 (vắng ca, nghỉ việc, đổi phân công, xác nhận bàn giao); `NguonCongViecApi` cho 007, 011, 014 (C-08, C-10).
- 005 làm plan trước 006, 007, 008, 011, 012, 014, nên khai báo SPI ở `m04.api` để lấy dữ liệu của họ (ca, phân công, chứng chỉ, nguồn công việc, chỉ số, liều thuốc, bản ghi đón) mà không tạo vòng phụ thuộc (C-02).
- Yêu cầu tạo/đóng cảnh báo, thông báo, sự kiện tính phí, ăn kém kéo dài đi qua outbox ghi cùng transaction. Riêng lưu chỉ số đo gọi đồng bộ `ChiSoPort`, vì kết quả lưu quyết định trạng thái công việc (C-09, C-13).
- Ngoại tuyến chỉ cho ghi nhận thực hiện, qua hai `NgoaiTuyenHandler` của khung 002 (C-14).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 005 không sinh, không gợi ý công việc; không tạo yêu cầu xem xét kế hoạch; không tạo công việc "hỗ trợ uống nước" hay công việc làm lại;
- hệ thống chỉ: chuyển trạng thái theo giờ (công việc, phiên bản kế hoạch, Vắng không báo), xác định trạng thái có mặt đầu ngày theo lịch đến, tạo yêu cầu cảnh báo, nhắc, báo, kiểm tra và chặn, tác động kèm lệnh của actor.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": Q-347, Q-349, Q-350)

- **Thỏa thuận lựa chọn có rủi ro** (FR-002a, FR-030): bảng `thoa_thuan_rui_ro` gắn phiên bản kế hoạch, kèm người tham gia và ý kiến có nguồn (dùng `NguonYKienApi` của 001); nhận định của Bác sĩ gắn với thỏa thuận; người quyết định theo BR-M01-15 tầng 2. Lệnh `PUT /ke-hoach-cham-soc/{id}/thoa-thuan`; checklist trả thỏa thuận của phiên bản Hiệu lực. Bảng riêng nên không bao giờ là căn cứ công việc (research C-18, data-model mục 3).
- **Loại lý do Không thực hiện** (FR-028a): danh mục nhóm 1 `loai_ly_do_khong_thuc_hien` có dòng cố định "Người cao tuổi từ chối"; lệnh Không thực hiện nhận `loaiLyDoId` và `ghiChu`; app lưu danh mục để ghi ngoại tuyến (research C-14, C-18).
- Không thêm thư viện, vai trò, trạng thái. Điểm báo lại 11, 12 bên dưới.

## Technical Context

### Ghi chú triển khai — Phase 1 (2026-10-26)

- Phạm vi T001–T002, chủ C (`HungNX-nguyen`). Đã đọc mục 8 team-plan; các việc BedTransferred, thống kê, provider chuyển giường của 003 không được ghi thành hoàn tất trong Phase 1 của 005. Checkout đầu lượt ở `feature/003-manage-bedroom`; giữ nguyên nhánh, không commit/push. `Working-history.md` và `specs/003-room-bed-allocation/hand-over-task-003.md` đã untracked từ đầu lượt, chỉ bổ sung lịch sử vào file đầu theo yêu cầu.
- T001 là ngoại lệ tạo khung được task yêu cầu rõ, so với nguyên tắc README chỉ tạo package khi có lớp. Ánh xạ tên tiếng Anh ở glossary mục 11; `scheduling` thay gói logic `lich`. Chỉ có package-info, chưa thêm service/domain/persistence/config/web rỗng. Gói chức năng đánh `@NullMarked` để compiler phát descriptor và xác định quy ước không-null; `api` dùng `@NamedInterface("daily-care")`, `api.spi` dùng `@NamedInterface("daily-care-spi")`. Lớp triển khai về sau phải theo các tầng README, không đặt ngay trong khung chức năng.
- T002 cần fake cài interface thật nhưng T003 mới yêu cầu khai báo SPI. Quyết định kỹ thuật: đưa riêng tám SPI và kiểu chỉ SPI dùng lên T002, giữ toàn bộ API khác/sự kiện/nền dữ liệu ở Phase 2. Không đánh xong T003. Kiểu lồng trong từng SPI theo ngoại lệ README cho kiểu chỉ SPI dùng, chưa tạo `api/dto`/`events` rỗng. Vai trò dùng `common.Role` đã có, không khai enum vai trò thứ hai. Không gọi API/bảng của module nguồn.
- Hợp đồng Java mục 2.1 được ánh xạ tên tương đương qua glossary, không thêm phương thức. Các view chưa được định nghĩa trường ở hợp đồng thiết kế được ghi rõ projection trong module-interfaces; không coi các projection là toàn bộ API nguồn. `DoseView` giữ các trường đã nêu của `LieuHienThiView` ở 006 mục 1.3; mã trạng thái/nguồn thuốc vẫn do 006 quyết định. Chưa công bố hay dịch các HTTP operation nghiệp vụ 005 còn lại.
- Tất cả fake, công tắc, controller và cấu hình ở `src/test/.../support/fake/m04`, do 005 sở hữu; `@TestConfiguration`, `@TestComponent` và profile `test`/`demo` ngăn tự quét vào ứng dụng và JAR production. Muốn dùng phải `@Import(FakeM04Config.class)` trên test classpath. Không có fake production cho demo; demo thông thường vẫn dùng nguồn thật. Sáu SPI một nguồn được `@Primary`; TaskSourcePort và TimetableSourcePort là danh sách nhiều bean, không `@Primary`, dùng tên bean khi đặt fixture. Khi có nguồn thật nhiều bean, fixture test cần chủ động chọn cấu hình nguồn phù hợp, không trộn fake thành fallback production.
- Fixture theo đúng khóa đối số: người/ca, người/thời điểm, người/ngày, loại+căn cứ+người+thời điểm. Lọc ca đã công bố theo tầng và khoảng nửa mở; liều thuốc lọc theo người và khoảng nửa mở. Mặc định đọc không có dữ liệu/false; căn cứ chưa đặt là không hợp lệ. Riêng xác nhận phục vụ giữ default true của hợp đồng thiết kế. Ghi chỉ số phải đặt trước kết quả SAVED/BLOCKED/AWAITING_REMEASUREMENT, không tự áp ngưỡng CFG; fake đón kiểm dùng lại. `reset()` xóa fixture và dấu gọi, công tắc bật lại giữ fixture; tắt module gây IllegalStateException rõ nguồn thay vì giả vờ trả dữ liệu rỗng. Fixture là bộ nhớ phục vụ kiểm thử, không mô phỏng transaction/rollback của database nguồn thật; MANDATORY của hai lệnh nguồn sẽ phải kiểm khi provider thật được cài.
- Chủ dự án duyệt riêng bằng câu trả lời “1” trong phiên ngày 2026-10-26: `PUT /api/v1/testing/fakes/{module}`, body `{ "enabled": boolean }`, 204; module 006/007/008/011/012/014, chỉ test/demo khi import. Đã bổ sung đúng operation này vào api-005.yaml, không dịch hoặc thay endpoint/trường nghiệp vụ còn lại, không đổi hợp đồng 000 hoặc generator chung. Thiếu/null enabled và module không hỗ trợ trả 400. Đây là điều khiển fixture, không ghi bản ghi nghiệp vụ, không đăng ký job.
- Bằng chứng TDD ban đầu: T001 12 failure/0 error do thiếu descriptor package; sau tạo khung, 12 ca cùng kiến trúc xanh. T002 chạy trước implementation thất bại biên dịch do thiếu tám SPI/fake; sau cài, sửa một tham chiếu test `FakeVitalSigns.Measurement`, 10 ca fake/HTTP cùng 28 kiểm kiến trúc xanh (38 tổng). Test contract riêng chạy đỏ do thiếu `/testing/fakes/{module}` trước khi thêm YAML. Lượt đầu dùng JDK24 và lượt sandbox chặn lock cache `.m2` là lỗi môi trường, không tính RED nghiệp vụ. Chuyển JAVA_HOME riêng process tới `E:/JAVA/JDK_GE/INS_25`, dùng cache hiện có; không sửa pom hoặc thiết lập máy, không thêm thư viện.
- Kiểm cuối phase: `mvnw.cmd clean verify` chạy nền với JDK25/MySQL thật đã kết thúc exit1 sau 04:12 phút. Đọc XML: 461 test đơn vị/kiến trúc xanh; 1098 IT có 6 failure + 9 error, 0 skip. Cả 23 ca mới (T001 12, fake 8, HTTP 2, contract 1) xanh; Spotless check 1417 file, 0 cần sửa. Kiểm nội dung JAR không có `support/fake/m04`/FakeM04Controller. Reviewer độc lập chỉ đọc không có finding trong phạm vi Phase1; `git diff --check` qua. Log: `C:/Users/nguye/.codex/visualizations/2026/10/26/01a176d7-6a54-7db1-b0ab-d145a335d1e4/005-p1-clean-verify.log`.
- 15 lỗi kiểm tích hợp đều ở 8 lớp M01 đã bàn giao B: BR_M01_01_TemporaryAbsenceOverdueIT (1 error), BR_M01_02_ReassessmentNoticeIT (2 error), CompleteAdmissionConsentIT (2 error), CompleteAdmissionWishWarningsIT (3 error), FR_023h_KhongDauNangLucIT (1 failure), FR_034a_CompleteAdmissionIT (4 failure), ResidentContractIT (1 failure), StatusHistoryIT (1 error). Nguồn lưu trú/điều kiện trạng thái, regex capacity và hai endpoint vị trí003 trong test contract001 vẫn còn; không sửa production/test M01 hoặc nới kiểm. Không nhận kết quả cũ của003 là lượt kiểm mới.
- Trạng thái: T001–T002 đã cài và có kiểm riêng xanh nhưng giữ `[ ]` vì gate clean verify chưa xanh; chưa nghiệm thu Phase1. Task tiếp theo theo thứ tự là T003, phần API còn lại (8SPI đã khai báo ở T002), không tự bắt đầu Phase2. `.specify/feature.json` tạo cục bộ (Git ignore) trỏ feature005; không đổi nhánh/commit/push. Nhật ký đầy đủ và danh sách file ở Working-history.md.

### Ghi chú triển khai — Phase 2 (2026-10-27)

- Đầu phiên báo tám dòng ghi rõ HungNX-nguyen trong team-plan8, đọc tasks/ghi chú Phase1 trước. Nhánh hiện là `feature/005-daily-care-and-hand-over`, working tree đầu lượt chỉ có Working-history.md và hand-over-task-003.md untracked cũ; không đổi nhánh/commit/push. User yêu cầu Phase2 nên tiếp tục phần độc lập dù gate M01 Phase1 còn mở, không đánh xong Phase1.
- T003: 11 interface theo contracts1.2–1.10a (gồm TaskTypeApi ở1.2), DTO/enum tại api/dto, giữ tám SPI Phase1. Các lệnh có MANDATORY đúng hợp đồng. Chưa có adapter nghiệp vụ story. FakeM04ApiConfig/Fixture chỉ ở test/demo khi import; khóa theo interface+method+đủ đối số, Supplier cho Stream mới; chưa đặt fixture thì báo lỗi. Scoped31test xanh, gồm3ca T003 và28kiến trúc lúc đó.
- T004: 28 record sự kiện ở api/events, CareEventContext ở DTO. CareNotice cài HasNotificationRequests; producer tương lai phải truyền nhóm người nhận đúng nghiệp vụ, mức theo C-13/FR-053. FR019 tách ShiftLead MEDIUM/FloorManager LOW. Chưa có producer/listener/outbox story nên không nhận đã kiểm toàn luồng. Khóa bounded SHA-256(namespace nguồn+mốc), tiền tố tên sự kiện+sourceId; TaskSourceChanged thêm SourceType, RiskAgreementReviewReminder thêm reviewDateType. Giờ phát lại không đổi khóa.
- T005: V005_01__care_foundation.sql chỉ tạo5bảng: care_resident_lock, m04_job_milestone, care_task_type, care_task_type_certificate, care_result_value. Đủ18nhóm/10loại kết quả, active_name UNIQUE, FK chứng chỉ nội bộ; window NULL dành CFG ở story. Không tạo bảng story hoặc FK module khác. Ba IT schema từ lỗi thiếu bảng sang xanh trên MySQL.
- T006: CareResidentLock MANDATORY; CareLockStore distinct/sort ID tăng dần, upsert no-op lấy exclusive lock ngay rồi SELECT FOR UPDATE, tránh nâng shared lock của INSERT IGNORE. Hai job CareMinuteJob/CARE_MINUTE và CareDailyJob/CARE_DAILY có ShedLock, cron phút/00:00 Asia/Ho_Chi_Minh và chạy bù khởi động; test profile không tự chạy. Work đăng ký dần theo story, hiện rỗng trong production; không tự lập công việc. Daily work trả ngày còn thiếu từ nguồn thật, runner ghi marker cùng transaction với work, rollback marker khi lỗi, sort/distinct ngày. Minute work sẽ xét mốc đã lỡ/nhắc gộp ở story, chưa ghi phase này đã làm nhắc gộp. Chỉ thêm2dòng SchedulerScopeArchTest theo hướng dẫn ngay trong test, giao A review; không đổi APIclock000/runJobs.
- **Chờ quyết định T007/T007a**: plan C-16 có LOAI_CONG_VIEC/CAU_HINH nhưng Function/Phụ lục27 không có dòng đó; Q24.1 không có quyết định mở. Đã hỏi chủ dự án dùng kiểm AC-01 trực tiếp và thêm9method SELF_CHECKED như M13 hay chờ A công bố khóa, chưa nhận câu trả lời. Không mượn khóa khác, chưa sửa SELF_CHECKED, chưa cài service/controller danh mục. Phương án cụ thể ở contracts/phase2-permission-review.md. T008[P]/T009[P] làm trong lúc chờ vì độc lập; không suy thời gian chờ thành duyệt.
- **API đã duyệt riêng**: user chọn phương án1 cho contracts/phase2-api-review.md: dịch task-types/non-performance-reason-types/result-values, thêm PUT /result-values/{id}, POST /result-values/{id}/deactivate. YAML đúng12operation/schema liên quan, test contract RED thiếu path rồi GREEN. Không dịch các endpoint story; không tự thêm version vào body. Giới hạn thiết kế: list catalog chưa có version/ETag từng dòng dù PUT dùng If-Match, cần chốt trước khi nối UI. Test YAML không phải endpoint chạy thật, không hoàn tất T007.
- T007a phần schema: V005_01_1__non_performance_reason_type.sql thay tên logic V005_01a vì Flyway version cần số. Danh mục seed đúng tên Người cao tuổi từ chối, CHECK luôn ACTIVE và trigger cấm đổi name/fixed/status/xóa dòng cố định; trigger DELETE giữ nghĩa luôn có sẵn. Hai IT SQL xanh, các dòng khác sửa/ngừng được. Service/endpoint/lỗi HTTP REASON_TYPE_FIXED còn chờ quyền, giữ task mở.
- T008: phân loại trường, scope4loại, 12provider lưu giữ. Scope đọc bảng M04, thiếu bảng/bản ghi ném InvalidPermissionDataException thay vì Target.UNSPECIFIED; vị trí qua ResidentLocationApi003. Mọi trường thỏa thuận/ý kiến, kể cả định danh/người ghi, HEALTH; trạng thái/thời điểm INTERNAL_PUBLIC. Retention nhận cutoff từ job chung, mốc hồ sơ qua ResidentApi.finalStatusAt001, thiếu mốc thì giữ; dữ liệu vị trí dùng plannedAt. Tách LOCATION_TASK/LOCATION_CARE_RESULT/LOCATION_TASK_HISTORY để dùng CFG-M15-04 vì SPI chỉ có một RetentionKind mỗi dataType, không làm giả mốc để dùng CFG-M15-03. Graph provider con, xóa qua RetentionDatabase. Bảng story chưa có được nhận diện metadata, không nuốt lỗi SQL khác. Tên bảng/cột dự kiến ở glossary11.2 cần giữ đồng bộ khi làm migration story. Test DDL fixture của M04 được dọn sau test, không đưa bảng story vào production sớm; kiểm hàng thật/cutoff/tài khoản retention, chưa kiểm toàn graph sau story hoặc hiệu năng hàng triệu dòng. Năm IT T008 xanh sau bổ sung metadata.
- T009: M04ArchTest cấm gọi trực tiếp007(m05/m06)/008(m09), cấm job và implementation CareMinuteWork/CareDailyWork gọi create/createTasks/planShift/createAdHoc/createRework của M04. Test riêng biên dịch bytecode vi phạm trong @TempDir để chứng minh luật bắt lỗi, không viết vào module khác. Khi story đặt tên lệnh mới cần cập nhật tập đích, không coi đây là phân tích call graph tùy ý.
- Review độc lập nêu2Important (key thiếu namespace, lock upgrade) và1Minor (bỏ sót step scheduler). RED2unit failure+1IT error deadlock, rồi tách test ngày xem lại RED3failure. Regrade step thành Important vì job thực tế gọi qua step, T009 phải bao phủ chính đường đó: fixture RED1failure rồi sửa predicate. Fix một lượt namespace/upsert X-lock/scope luật; cuối37unit/kiến trúc và4IT liên quan xanh, không re-review. Không còn minor trì hoãn trong3finding này.
- Log tại `C:/Users/nguye/.codex/visualizations/2026/10/26/01a176d7-6a54-7db1-b0ab-d145a335d1e4/005-p2-*.log`. Runner ngoài kho mã dùng JDK25/nạp riêng TEST_DB_* từ .env, không in giá trị. Một lượt RED review dùng maven.test.failure.ignore=true chỉ để chạy tiếp IT sau unit đỏ: log BUILD SUCCESS nhưng XML có2failure+1error, không tính xanh; mọi lượt GREEN/clean verify không có cờ này. Spotless apply đã chạy; không thêm thư viện, sửa common production, Function/ma trận quyền, M01, UI/client sinh.
- Kiểm cuối `mvnw.cmd clean verify` chạy nền kết thúc exit1/04:32phút. XML xác nhận474test đơn vị/kiến trúc xanh;1112IT có6failure+9error/0skip, đúng8lớpM01 đã bàn giaoB: BR_M01_01_TemporaryAbsenceOverdueIT(1error), BR_M01_02_ReassessmentNoticeIT(2error), CompleteAdmissionConsentIT(2error), CompleteAdmissionWishWarningsIT(3error), FR_023h_KhongDauNangLucIT(1failure), FR_034a_CompleteAdmissionIT(4failure), ResidentContractIT(1failure), StatusHistoryIT(1error). Cả27kiểm mới củaPhase2 xanh (13unit/kiến trúc,14IT). Spotless1509file sạch; không sửa B để ép gate. Log005-p2-clean-verify.log và .log.exit. Giữ T003–T009/T007a[ ], chưa nghiệm thuPhase2 vì gate và service/controller T007/T007a còn chờ quyền. Chi tiết từng file/lệnh và thứ tự tiếp theo ở Working-history.md.

### Ghi chú triển khai — tiếp nhận Phase 3 (2026-10-27)

- Chủ dự án đã chọn phương án 2 của điểm quyền danh mục: chờ A công bố khóa chính thức. Ghi chú "chưa nhận câu trả lời" ở phần lịch sử Phase 2 đã được thay thế bởi quyết định này; không cài AC-01 trực tiếp, không sửa SELF_CHECKED/Function hoặc nguồn nghiệp vụ.
- Đối chiếu tasks.md: Phase 2 ghi rõ hoàn tất nền mới làm user story. T007 và phần service/HTTP T007a còn thiếu, nên chưa bắt đầu implementation T010–T017 hoặc tự đánh hoàn tất nền. Yêu cầu Phase 3 được tiếp nhận; phần chuẩn bị hợp đồng được thực hiện trước.
- Tạo contracts/phase3-api-review.md: đề nghị dịch đúng 11 operation kế hoạch, schema/trường/enum/lỗi tương ứng, giữ header/mã HTTP. Phát hiện hai tham chiếu NhanDinhTuQuyet/NguonYKien không có đích trong YAML 005; đề nghị dùng đúng SelfDeterminationAssessment/OpinionSource đã có của 001, không sáng tạo cấu trúc mới. Chưa sửa api-005.yaml hoặc sinh client khi chưa duyệt.
- Lượt chuẩn bị này chỉ đọc và sửa tài liệu; không chạy test/build, không dùng kết quả kiểm Phase 2 làm bằng chứng mới. Không có thay đổi UI, screenshot, thư viện, commit/push hoặc đổi nhánh. Thứ tự tiếp theo: A công bố quyền → T007 → T007a → kiểm cuối nền; sau đó T010, T011 và các task implementation Phase 3 theo tasks.md, với hợp đồng được duyệt trước HTTP/client.

### Ghi chú triển khai — áp dụng hợp đồng Phase 3 đã duyệt (2026-10-27)

- Chủ dự án trả lời "1" cho phase3-api-review.md: áp dụng đúng 11 operation `/care-plans` và hai schema nguồn của 001. Quyết định quyền Phase 2 vẫn là chờ A; không suy duyệt HTTP thành cho phép bỏ nền hoặc cài AC-01 trực tiếp.
- Test mới FR_003_CarePlanContractTest có 3 ca kiểm operation/header/mã phản hồi, field/enum/required, và hai schema đồng nhất với bản công bố 001. Sau lỗi biên dịch kiểu Schema của test ban đầu, lượt RED thật ghi 3 failure/0 error do thiếu bản dịch; chỉ sau đó sửa YAML. Lượt GREEN đầu còn 1 failure do parser sắp lại mảng required; đổi assertion sang containsExactlyInAnyOrder vì thứ tự required không có ý nghĩa OpenAPI. Lượt cuối 34 test hợp đồng/kiến trúc xanh, exit0/33.771s; riêng 3 ca mới xanh. Không tính lỗi biên dịch thành RED nghiệp vụ.
- Các schema dùng chung đổi tên CarePlanItem/RiskAgreement nên cập nhật cả tham chiếu tại các story chưa dịch; giữ tên operation/trường bao ngoài của những story đó. Tách Importance, ResidentId và CarePlanReason riêng để không đổi enum/header/lý do tiếng Việt của các phần chưa duyệt. Lỗi kế hoạch trỏ tới hình dạng tiếng Anh CareCatalogProblem đã có, không thêm trường ngoài bản dịch duyệt.
- Chép đúng OpinionSource/SelfDeterminationAssessment từ 001; không sửa hợp đồng hoặc nguồn của 001. Không sửa DTO/API Java công khai, không sinh client khi controller chưa được cài. Glossary chỉ thêm mục 11.3 của 005.
- Spotless apply qua (1510 file, chỉ test mới được định dạng trong lượt cuối). Reviewer độc lập chỉ đọc không có finding Critical/Important/Minor trong phạm vi bản dịch; không coi review/test YAML là kiểm runtime. git diff --check qua. Log ngoài kho 005-p3-contract-{red,red2,green,green2}.log; không chạy clean verify hoặc nghiệm thu Phase 3 vì nền T007/T007a và story T010–T017 chưa hoàn tất. Giữ checkbox trống; không commit/push/đổi nhánh.

**Language/Version**: Java 25; JavaScript (JSX); Dart 3.x với Flutter stable 3.x (000 R-01).

**Primary Dependencies**: Như 000 → 004: Spring Boot 4.1.x, Spring Modulith 2.x, Flyway (bản do Spring Boot quản lý), springdoc-openapi 3.x, ShedLock, Spring Security 7. Web: React 19, TanStack Query. App nhân viên: Flutter, drift (hàng đợi ngoại tuyến, 000 R-13). Không thêm thư viện mới.

**Storage**: MySQL 8.0. Bảng ở [data-model.md](data-model.md).

**Testing**:
- Như 000 R-14.
- Test đơn vị dạng bảng cho tám hàm thuần: chuỗi thời điểm của mục, kiểm tra lập công việc, bảng chuyển công việc, xác định trạng thái đóng và nhãn ghi muộn, nước uống, chuỗi bữa kém, chuỗi ngày tâm trạng, so sánh phiên bản.
- Test tích hợp MySQL cho trigger, cột sinh.
- Test đồng thời: hai lần ghi một công việc; ghi nhận và job Quá hạn; hai lô lập trùng.
- Đồng hồ giả lập cho mọi CFG-M04, gồm chạy bù.
- Bản giả cho mọi SPI 005 khai báo (research C-17).

**Target Platform**: Như 000.
- Web: mọi màn của 005, gồm danh mục loại công việc, kế hoạch, lập công việc của ca, checklist, xử lý quá hạn, thời khóa biểu, điểm danh bán trú.
- App nhân viên: checklist, ghi nhận (có ngoại tuyến), Ghi nhận phát sinh, nhận việc, điểm danh bán trú (trực tuyến), xem kế hoạch Hiệu lực, lập công việc của ca và xử lý quá hạn cho Người phụ trách ca, Trưởng tầng.
- App người thân: không có màn của 005 (người thân không xem kế hoạch, FR-007).

**Project Type**: Web service (REST `/api/v1`), một web client và một mobile client chung (Q-346).

**Performance Goals**:
- Mở checklist ≤ 3 giây (SC-008, NFR-02): một truy vấn có chỉ mục theo ca và người, vị trí và lưu ý lấy theo lô.
- Ghi nhận ≤ 2 giây (NFR-02): khóa một dòng, thêm một kết quả, vài sự kiện outbox; quy tắc mục F là truy vấn theo chỉ mục `(nct_id, loai_ket_qua, thoi_diem_thuc_hien)`.
- Job theo phút ≤ 1 phút mỗi lần ở quy mô 300 người (khoảng 7.500 công việc mỗi ngày, vài chục chuyển mỗi phút), trong NFR-04.
- Lô lập công việc của một tầng (khoảng 40 người × 10 công việc) trong vài giây: kiểm tra dùng dữ liệu nguồn đọc theo lô.

**Constraints**:
- Không có thao tác sửa trạng thái; mỗi lệnh một endpoint (constitution XI).
- Kết quả ghi nhận, điểm danh, lịch sử xử lý không bị `UPDATE`, `DELETE`; phiên bản kế hoạch ngoài Nháp không sửa (constitution XII).
- Nhật ký trong transaction (000 FR-047). CFG đọc lúc chạy (constitution XIII).
- Không tự sinh bản ghi nghiệp vụ (000 FR-010a).

**Scale/Scope**:
- 300 người cao tuổi, khoảng 7.500 công việc và 6.000 kết quả mỗi ngày (khoảng 2,7 triệu công việc mỗi năm).
- 3 thực thể có vòng đời (phiên bản kế hoạch, công việc, có mặt bán trú); 4 bảng nhóm 3.
- 2 job (theo phút, hằng ngày).
- YAML hiện có41đường dẫn/45operation sau đếm lại ngày2026-10-27, gồm cả công tắc Phase1, danh mục được duyệt và các operation story chưa dịch/chưa cài; đây không phải số endpoint runtime đã triển khai ([contracts/api-005.yaml](contracts/api-005.yaml)).
- 8 SPI 005 khai báo; 6 SPI 005 cài cho module thấp hơn; 9 giao diện 005 cung cấp.

Không còn mục NEEDS CLARIFICATION. Các lựa chọn kỹ thuật đã chốt ở [research.md](research.md). Các điểm về tài liệu nguồn ghi ở "Điểm cần báo lại"; chúng không chặn thiết kế.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Bằng chứng |
| --- | --- | --- |
| I. Nguồn gốc, truy vết | Đạt | Mỗi quyết định dẫn FR, BR, DBR. Data-model mục 12 là bảng truy vết DBR-11, 12, 15, 17, 23, 25 và BR-M04-01 → 14, 19, 20, 24 → 29 |
| II. Spec không nêu công nghệ | Đạt | Spec không đổi; công nghệ chỉ ở plan, research |
| III. Không mặc định CRUD | Đạt | Loại công việc theo mẫu nhóm 1. Mục kế hoạch chỉ sửa ở Nháp. Phiên bản, công việc, có mặt chỉ có lệnh. Kết quả, điểm danh, lịch sử xử lý chỉ thêm và đính chính |
| IV. Trạng thái kiểm tra được | Đạt | Bảng chuyển ở data-model mục 3, 5, 6 khớp bảng FR-003, FR-013, FR-026. Mỗi kịch bản có test (quickstart mục 4) |
| V. Không cố định giá trị | Đạt | CFG-M04-01 → 06, 12, CFG-M02-07, CFG-M09-04, CFG-M15-05 → 08, CFG-M06-03 (qua 007) đọc từ `ThamSoApi`. Chu kỳ job một phút và 00:00 là thông số kỹ thuật (Complexity Tracking) |
| VI. Vai trò, quyền | Đạt | Khóa quyền theo dòng Phụ lục 27 (research C-16), 19.3 là căn cứ cho "Lập công việc của ca". Bác sĩ chỉ xem kết quả theo chú thích ²². Không thêm vai trò |
| VII. Điểm chưa rõ | Đạt | Không tự đoán nghiệp vụ. Mức các thông báo spec chưa nêu: plan áp nguyên tắc 3 bậc và báo lại (điểm 4). BR-M04-07 lệch BR-M09-06: plan theo spec FR-046 và BR-M09-06 (điểm 1) |
| VIII. Tiêu chí thành công | Đạt | SC-001 → SC-012 có cách đo ở quickstart mục 5 |
| IX. Phạm vi giai đoạn | Đạt | Chỉ tài liệu thiết kế; không mã, không `src/`, không cấu hình build, không SQL; không đổi nghiệp vụ. Không sửa hợp đồng của 000 → 004 |
| X. Kiến trúc | Đạt | Một khối; gói `m04`; module khác chỉ dùng `m04.api`. Không có vòng phụ thuộc (C-02). REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Kiểm tra lập, quyền ghi, bảng chuyển, quy tắc mục F ở backend. Mỗi lệnh có `POST …/{id}/<lenh>` riêng; lệnh trạng thái người cao tuổi dùng endpoint của 001 |
| XII. Dữ liệu | Đạt | DBR-11, DBR-12 bằng cột sinh `UNIQUE` và khóa; nhóm 3 theo mẫu 000; nhật ký trong transaction; `Clock`; `thoi_diem_thiet_bi` cho ngoại tuyến (DBR-25) |
| XIII. Tham số | Đạt | Như V. Đổi CFG-M04-03 trong ngày áp từ lần kiểm tra kế tiếp |
| XIV. Ngoại tuyến, thông báo | Đạt | Ghi nhận thực hiện ngoại tuyến theo Q-01 qua khung 002; các lệnh khác chỉ trực tuyến. Mọi thông báo là sự kiện tới 009, có mức (C-13) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 005 có test (data-model mục 12). BR-M04-27 và BR-M15-20 có thêm test Flutter |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có sáu bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Bảng `cham_soc_nct` làm đích khóa theo người.
2. Trạng thái `DA_HUY_NGAY_NGHI` của dòng có mặt.
3. Lệnh lập công việc theo lô, từ chối cả lô.
4. Tám SPI khai báo ở `m04.api` cho các module làm plan sau.
5. `ChiSoPort` gọi đồng bộ, khác các yêu cầu tới 007 đi qua outbox.
6. Hai job (theo phút, hằng ngày) và bảng `lan_chay_m04`.

### Đáp ứng yêu cầu phi chức năng (constitution X)

| NFR | 005 chịu ảnh hưởng | Cách đáp ứng |
| --- | --- | --- |
| NFR-01 Quy mô | Có | 300 người, 60 người dùng đồng thời giờ giao ca; chỉ mục ở data-model mục 6, 7 |
| NFR-02 Hiệu năng | Có | Checklist một truy vấn theo ca và người; ghi nhận khóa một dòng; quy tắc mục F theo chỉ mục |
| NFR-03 Thông báo khẩn cấp | Không | 005 không có thông báo Khẩn cấp |
| NFR-04 Tác vụ định kỳ | Có | Job theo phút; khóa dòng và cột `da_*_luc` chống gửi trùng; khóa sự kiện cho bên nghe |
| NFR-05 Khả dụng | Có | Chạy bù khi khởi động (FR-052); ngoại tuyến cho ghi nhận tại giường |
| NFR-06 Sao lưu | Gián tiếp | Dữ liệu ở MySQL |
| NFR-07 Lưu trữ | Có | `LuuTruProvider` theo hồ sơ (data-model mục 11) |
| NFR-08 Bảo mật | Có | Kết quả, lưu ý, nội dung mục mang `SUC_KHOE`; Hành chính, Bếp không thấy |
| NFR-09 Thời gian | Có | `Clock`; ngày dương lịch Asia/Ho_Chi_Minh cho mốc nước, hết ngày bán trú, chuỗi ngày tâm trạng |
| NFR-10 Thiết bị | Có | App nhân viên một tay, nút lớn cho checklist và ghi nhận; web đủ chức năng |
| NFR-11 Ngôn ngữ | Có | Giờ HH:mm, ngày dd/MM/yyyy ở client; lỗi lập công việc liệt kê theo dòng kèm mã |
| NFR-12 Cấu hình | Có | Mọi CFG-M04 đổi qua tham số; loại công việc, giá trị kết quả qua danh mục |
| NFR-13 Kiểm thử | Có | Đồng hồ giả lập; bản giả bật tắt được |

## Project Structure

### Documentation (this feature)

```text
specs/005-daily-care-tasks/
├── plan.md                      # File này
├── research.md                  # Phase 0: C-01 → C-17
├── data-model.md                # Phase 1: bảng, bảng chuyển, bảo đảm ở cơ sở dữ liệu, truy vết
├── quickstart.md                # Phase 1: kịch bản kiểm chứng, truy vết kịch bản → test
├── contracts/
│   ├── api-005.yaml             # OpenAPI 3.1: kế hoạch, có mặt, lập công việc, checklist, ghi nhận, danh mục, thời khóa biểu
│   └── module-interfaces.md     # Giao diện Java: 005 cung cấp / 005 cần; sự kiện; tác động của bean trạng thái
└── tasks.md                     # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

Cấu trúc dự kiến cho giai đoạn triển khai. Giai đoạn thiết kế không tạo các thư mục này (CLAUDE.md).

```text
backend/src/main/java/com/cambae/backend/m04/
├── api/           # KeHoachChamSocApi, CoMatBanTruApi, CongViecCaApi, CongViecBanGiaoApi, NguonCongViecApi,
│                  # CongViecVangMatApi, CongViecDoChiSoApi, CongViecThongKeApi, ThoiKhoaBieuApi,
│                  # SPI 005 khai báo (NhanSuCaPort, NguonCongViecPort, ChiSoPort, LieuThuocChecklistPort,
│                  # CanhBaoDocPort, BanGhiDonBanTruPort, KetQuaKiemTraPort, ThoiKhoaBieuNguonPort), sự kiện
├── kehoach/       {internal, web}   # phiên bản, mục, ChuoiThoiDiemMuc, SoSanhPhienBan
├── comat/         {internal, web}   # có mặt bán trú, điểm danh, giờ về
├── congviec/      {internal, web}   # lập theo lô, KiemTraLapCongViec, BangChuyenCongViec, hủy tự động
├── ghinhan/       {internal, web}   # kết quả, Ghi nhận phát sinh, đính chính, NgoaiTuyenHandler
├── checklist/     {internal, web}   # checklist, nhận việc, phân lại, công việc chung
├── danhmuc/       {internal, web}   # loại công việc, giá trị kết quả
├── quytac/        {internal}        # DanhGiaNuocUong, DemChuoiBuaKem, DemNgayTamTrang
├── quahan/        {internal, web}   # nhắc, leo thang, danh sách quá hạn, bàn giao
├── thoikhoabieu/  {internal, web}
├── trangthai/     {internal}        # bean ThamGiaChuyenTrangThai; cài TrangThaiCoMatBanTruPort
└── lich/                            # ViecTheoPhutM04, ViecHangNgayM04

frontend/src/tinh-nang/
├── ke-hoach-cham-soc/       # phiên bản, mục, duyệt, so sánh
├── lap-cong-viec-ca/        # dữ liệu nguồn, nhập lô, lỗi theo dòng
├── checklist/               # checklist web, ghi nhận
├── viec-qua-han/            # danh sách, phân lại, đóng
├── diem-danh-ban-tru/
├── loai-cong-viec/
└── thoi-khoa-bieu/

mobile/lib/nhan-vien/
├── checklist/               # tải, lưu ngoại tuyến, dấu "chưa cập nhật"
├── ghi-nhan/                # theo loại kết quả, Ghi nhận phát sinh, bản ghi bị từ chối
├── dong-bo/                 # kết quả đồng bộ từng bản ghi (BR-M15-20)
├── lap-cong-viec-ca/        # Người phụ trách ca, Trưởng tầng
├── viec-qua-han/
└── diem-danh-ban-tru/
```

**Structure Decision**: Theo R-02 của 000. Feature 005 nằm ở `backend/…/m04` với chín gói con nghiệp vụ, gói `trangthai` gom SPI cài cho module thấp hơn, gói `lich` và gói `api`. Phần 8.8 → 8.10 của M04 (hoạt động, ngoài viện, tinh thần, kiểm tra chất lượng) thuộc feature 014; plan 014 thêm gói con vào cùng `m04`. Web có đủ chức năng. App nhân viên có checklist, ghi nhận ngoại tuyến và các thao tác tại tầng. App người thân không có màn của 005.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Bảng `cham_soc_nct` (một dòng mỗi người) | Đích khóa tuần tự cho DBR-11, FR-006, khóa trùng theo người khi lập theo lô | Khóa dòng `nguoi_cao_tuoi` của M01 là truy cập dữ liệu module khác |
| Trạng thái `DA_HUY_NGAY_NGHI` của dòng có mặt | Bảng FR-013 ghi "không còn trạng thái có mặt cho buổi đó"; giữ dòng để 010, 016 thấy buổi đã bị hủy do ngày nghỉ, có nhật ký | Xóa dòng: mất dấu vết và vi phạm "không mặc định CRUD" |
| Lệnh lập theo lô, từ chối cả lô khi một dòng lỗi | Người phụ trách ca lập hàng trăm công việc trong giờ giao ca; "hoặc toàn bộ, hoặc không" của 000; trả đủ lỗi một lần | Mỗi công việc một lệnh: chậm, và lần lập dừng giữa chừng để lại ca lập dở |
| Tám SPI khai báo ở `m04.api` (008, 007, 006, 011, 012, 014) | 008, 007, 011, 014 phải gọi lệnh của 005 trong transaction của họ, nên phụ thuộc 005; 005 cần lại dữ liệu của họ | 005 gọi thẳng API của họ: vòng phụ thuộc, Modulith từ chối |
| `ChiSoPort` đồng bộ, các yêu cầu khác tới 007 qua outbox | Giá trị bị chặn hoặc cần đo lại thì công việc không được đóng (FR-034); chỉ biết khi 007 trả lời ngay | Outbox cho chỉ số: công việc đã đóng trong khi giá trị bị từ chối |
| Job theo phút, job hằng ngày, bảng `lan_chay_m04` | SC-005 đòi chuyển và nhắc ≤ 1 phút; FR-052 đòi chạy bù không trùng; mốc theo ca, theo ngày chạy đúng một lần. Chu kỳ và giờ chạy là thông số vận hành, không có trong Phụ lục 25 | Mỗi việc một job: khó bảo đảm thứ tự (Hiệu lực kế hoạch trước, rồi có mặt đầu ngày) và nhiều khóa |

## Điểm cần báo lại về tài liệu nguồn

Plan không tự sửa các điểm dưới đây (constitution I, IX). Đề nghị chủ dự án quyết bằng mã Q mới khi cần, rồi sửa `docs/business/nghiep-vu.md` và spec liên quan.

1. **BR-M04-07 còn câu "tự động đưa vào bản nháp bàn giao".** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - BR-M09-06 (Q-307) ghi "bàn giao do người bàn giao tự ghi … Hệ thống không lập sẵn mục"; spec 005 FR-046 cũng vậy.
   - Plan theo FR-046 và BR-M09-06: 005 chỉ cung cấp danh sách công việc chưa đóng (`CongViecBanGiaoApi.chuaDong`).
   - Đề nghị sửa BR-M04-07 thành "Công việc chưa đóng khi kết thúc ca được người lập bàn giao xem và ghi vào bàn giao (BR-M09-06)".

2. **DBR-12 hẹp hơn FR-021.** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - DBR-12: "duy nhất theo (mục kế hoạch, thời điểm dự kiến)". FR-021 mở rộng cho mọi căn cứ (lần đo, bữa, buổi, việc theo dõi) và chỉ xét công việc chưa Hủy (CHK040).
   - Plan cài theo FR-021 (cột sinh `khoa_trung`).
   - Đề nghị ghi DBR-12 theo FR-021.

3. **BR-M04-02, BR-M04-04 nói công việc "trong khoảng vắng" / "sau giờ về" tự động chuyển Hủy**, trong khi bảng 8.3 và FR-027 chỉ cho Hủy từ Chưa đến hạn. Plan theo bảng 8.3 (spec điểm báo lại 3, câu cuối, vẫn còn chờ). Đề nghị thêm "Chưa đến hạn" vào hai BR. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

4. **Mức của các thông báo spec 005 chưa nêu (009 FR-043a, Q-108).** Plan áp nguyên tắc 3 bậc (research C-13): ca chưa có công việc lúc bắt đầu ca ở mức Trung bình; xem xét kế hoạch (FR-011), kế hoạch hoặc nguồn đã đổi, cần lập công việc (điểm danh đến, trở về, Hoàn tất tiếp nhận, đính chính chuyến đi), Vắng không báo, gửi duyệt / trả lại / thu hồi phiên bản ở mức Nhẹ. Đề nghị ghi vào spec 005 và bảng 009 FR-043b khi đồng bộ. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

5. **Phụ lục 27 dòng "Lập công việc của ca" cho ĐD "T"**, trong khi 19.3 ghi "Điều dưỡng không giữ nhiệm vụ Người phụ trách ca chỉ tạo công việc phát sinh". Plan theo 19.3 (ô T của ĐD hiểu là công việc phát sinh). Đề nghị thêm chú thích cho ô này. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

6. **Buổi phát sinh của bán trú và sức chứa khu nghỉ.** Điểm danh đến ngày không có lịch tạo một buổi phát sinh (Q-140), nhưng 003 FR-039 chỉ kiểm sức chứa "khi đăng ký hoặc thay đổi lịch đến". Plan không kiểm sức chứa khi điểm danh buổi phát sinh. Đề nghị xác nhận, hoặc chốt bằng Q nếu buổi phát sinh phải chịu giới hạn CFG-M03-01. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

7. **Người thực hiện: FR-024 "lấy theo phân công" và FR-017 "người lập nhập".** Plan hiểu: người lập chọn; backend chỉ chấp nhận người có trong phân công chăm sóc của người cao tuổi ở ca đó với vai trò khớp (chính hoặc hỗ trợ), và chỉ cho "công việc chung" khi không có ai khớp. Hệ thống không điền sẵn (Q-307). Đề nghị ghi rõ ở FR-024 rằng nhân viên hỗ trợ cũng được chọn, hoặc chỉ nhân viên phụ trách chính. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

8. **Lập công việc tương lai cho người đang vắng.** FR-017 chặn khi người cao tuổi "đang vắng mặt tại thời điểm dự kiến". Với ca lập trước, chưa biết người đó có về kịp không; plan chặn mọi thời điểm khi người đó đang vắng lúc lập, và dựa vào thông báo `CanLapCongViec` khi trở về (FR-020). Đề nghị xác nhận. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

9. **(Đồng bộ câu chữ spec 005, không đổi nghiệp vụ)** *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*
   - Assumptions còn câu dẫn `docs/phan-tich-yeu-cau.md` mục 4.2 (đã bỏ); có thể giữ làm lịch sử.
   - FR-038, FR-048 ghi "mục 4.2 chưa có UC riêng, xem điểm báo lại 12": tham chiếu cũ; plan coi hai chức năng là cấu hình danh mục (quyền C của Quản lý viện) và màn xem, không thêm UC.
   - Edge Case "Bộ lập lịch bỏ lỡ lần chạy" và FR-052 (a) đã đúng sau Q-306; không cần sửa.

10. **Sự kiện của 007 mà 005 nghe chưa có tên trong plan nào** (`SuCoNgaDaGhiNhan`, `LuaChonChamSocGiamNheDaGhi`, `LichDoDaDoi`). Tên trong [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 2.2 là đề xuất; plan 007, 008, 011, 012, 014 giữ đúng chữ ký các SPI ở mục 2.1 hoặc ghi rõ thay đổi. *(Đã xử lý 2026-10-10: Q-311 → Q-336; xem mục "Cập nhật 2026-10-10, lượt 2" và "Điểm cần báo lại" của spec.)*

11. **(2026-10-18) Hiển thị thỏa thuận trên checklist** (spec "Điểm cần báo lại" 1 của lượt 2026-10-18): 8.1 không nêu chỗ hiển thị; plan làm theo FR-030 (checklist trả thỏa thuận của phiên bản Hiệu lực). Chờ chủ dự án xác nhận; nếu bỏ thì chỉ bỏ trường `thoaThuan` của `Checklist`. *(Đã xử lý 2026-10-18: Q-360: giữ hiển thị trên checklist)*

12. **(2026-10-18) Hai "ngày xem lại" của thỏa thuận** (ngày xem lại thỏa thuận và ngày xem lại nhận định, FR-002a): spec không nói tới hạn thì làm gì. Plan chỉ lưu và hiển thị, không nhắc (Q-306). Cùng câu hỏi với plan 001 điểm báo lại 11. Ngoài ra FR-002a ghi người cao tuổi "MUST có trong danh sách khi tham gia được"; hệ thống không biết khi nào người cao tuổi tham gia được, nên plan chỉ bắt buộc khi Bác sĩ nhận định người cao tuổi tự quyết được (`CHECK (nct_tham_gia OR NOT tu_quyet_duoc)`). Đề nghị xác nhận. *(Đã xử lý 2026-10-18: Q-360: nhắc một lần mỗi ngày xem lại (research C-18); quy tắc `nct_tham_gia` giữ theo plan, chủ dự án ủy quyền chốt)*
