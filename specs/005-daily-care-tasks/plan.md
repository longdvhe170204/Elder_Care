# Implementation Plan: Chăm sóc hằng ngày

**Branch**: `005-daily-care-tasks` | **Date**: 2026-10-09, cập nhật 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-daily-care-tasks/spec.md`

### Ghi chú triển khai — tiếp nhận Phase 6 (2026-11-01)

- Checkout đầu phiên `44a7c201`, nhánh hiện có; không đổi nhánh, commit/push/merge. Báo đủ 18 dòng chứa HungNX-nguyen tại mục 8. Giữ Working-history.md và hand-over-task-003.md chưa theo dõi có sẵn; không sửa tài liệu bàn giao 003.
- Chủ dự án đã duyệt bản dịch sáu operation T039 bằng câu trả lời “1”; chi tiết tại phase6-contract-proposal.md. Chưa tự mở rộng API/SPI hoặc thư viện. Các gate nguồn B/D/E, retention, VOID và mobile common của Phase 5 giữ nguyên. Khi merge ưu tiên listener nguồn tương ứng B/E, tránh xử lý trùng.
- T032: hàm thuần CareResultTiming.completedStatus(windowFrom,windowTo,performedAt), đặt tại internal/result/domain. Hai biên bao gồm; sau cuối khung là COMPLETED_LATE, trước đầu khung hoặc khung đảo bị từ chối. Không đưa giờ đồng bộ vào hàm để thời điểm nhận muộn không làm sai trạng thái đã thực hiện đúng khung (FR-035). Kiểm quyền/trạng thái đang mở và giới hạn thời điểm ghi thuộc service T036, không đưa vào luật thuần.
- TDD T032: RED 46 kiểm gồm 2 failure + 4 error do thiếu CareResultTiming; GREEN 46/46 (6 ca T032 + 40 ArchTest), exit0. Đã chạy spotless:apply giới hạn care. Log 005-p6-t032-{red,green}.log và 005-p6-spotless.log ở thư mục artifacts phiên. Chưa đánh task hoàn thành trước clean verify cuối phase.
- R-15/T035: kết quả gộp vào care_task, id kết quả bằng id task; giữ bất biến nhóm 3 và chỉ mục resident/resultType/performedAt, không tạo bảng kết quả riêng. Rà FR-038 thấy snapshot CareTaskData hiện thiếu upperLimit/vitalSignType; phải xử lý trong T036 để không áp cấu hình danh mục mới vào task đã lập.

### Ghi chú triển khai Phase 6 — phần trực tuyến và các gate

- T035/T036 dùng V005_98 (cột kết quả/check/trigger), V005_99 (carryover chỉ đọc; producer thuộc Phase7), V005_100 (reason_type_id tham chiếu catalog_item.id vật lý). ResultStore lưu kết quả và trạng thái trong một UPDATE có version, khóa người trước task. Pending đo lại giữ task mở, chặn record/notPerform đến khi nguồn xác nhận. JSON null được giữ để validator trả lỗi nghiệp vụ thay vì NullPointerException.
- FR-038: CareTaskData chụp upperLimit/vitalSignType và resultConfigurationCaptured nội bộ khi lập task mới, kể cả không đặt giới hạn. Task ML cũ thiếu cả giới hạn lẫn dấu đã chụp trả503; không suy danh mục hiện tại hoặc backfill lịch sử không có bằng chứng. CareTask.resultType chỉ đọc từ snapshot là trường HTTP bổ sung đã duyệt tại mục5 proposal, không đổi API/SPI Java.
- T038 đọc thỏa thuận hiện hành theo lô, gọi nguồn vị trí/lưu ý theo lô, lọc quyền từng người và kiểm chứng chỉ hiện hành khi nhận việc. Việc tồn được đọc cả trước đầu cửa sổ CFG_M15_07. Thiếu nguồn thực trả503, không dùng fake production. AllowedCommands dùng cùng điều kiện service, lệnh vẫn kiểm quyền riêng.
- R-15 governance: M04.CARE_RESULT/TASK cùng id và dòng care_task; graph hai chiều buộc kiểm attachment cả hai trước xóa, seen-set sẵn có chặn vòng. Provider result không DELETE, provider task xóa vật lý. Scope kết quả chỉ nhận dòng có result_performed_at; planned_at dùng BusinessTime.ZONE. Gate shared retention điểm danh của A vẫn còn.
- Web dùng CareChecklistPage/Detail/CareResultForm qua client gen:api. Form chọn theo snapshot; GET /result-values không query, lọc type/status phía client. Lỗi giữ input/idempotency key;202 chỉ báo chờ đo lại. Mobile dùng cache Drift riêng feature theo accountId/shiftId, fallback khi mất kết nối;403/404 xóa snapshot. CareChecklistScreen nhận accountId/shiftId từ shell A, không sửa common hoặc tự chọn ca. Mobile đã có form không cần danh mục mảng; SLEEP/MOOD/BEHAVIOR, phát sinh/đồng bộ/bản bị từ chối còn thiếu, chưa coi T040 đầy đủ.
- Quyết định chủ dự án: T034/T037 payload chờ A; summary chờ E/012; phát sinh đo chỉ số và callback khác người/giờ gốc chờ D/007. Ngày01/11 chủ dự án chọn số lượng tính phí chờ E/010: chặn lệnh billable trước lưu, không suy ml/phút/giá trị đo thành số lượng dịch vụ hoặc bỏ sự kiện. Giữ ưu tiên listener nguồn B/E khi merge như Phase5.
- TDD bổ sung: schema resultType RED1/42 do thiếu field, checklist10/10 xanh ở lượt tuần tự005-p6-billing-gate-red2 (recording RED1 đúng gate tính phí). JSON null RED1 NPE; safety RED3/25 (legacy ML, đóng pending, tính phí). Web12/12 test mới xanh; lint/build lượt đầu qua. Mobile7/7 rồi thêm retry/record RED, GREEN9/9. Hai lượt Maven chồng target gây thiếu FakeResidents$Entry: đã tìm nguyên nhân, chạy lại tuần tự, không sửa fake B hoặc tính lỗi môi trường là RED nghiệp vụ.

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
- 005 làm plan trước 006, 007, 008, 011, 012, 014, nên khai báo SPI ở `care.api` để lấy dữ liệu của họ (ca, phân công, chứng chỉ, nguồn công việc, chỉ số, liều thuốc, bản ghi đón) mà không tạo vòng phụ thuộc (C-02).
- Yêu cầu tạo/đóng cảnh báo, thông báo, sự kiện tính phí, ăn kém kéo dài đi qua outbox ghi cùng transaction. Riêng lưu chỉ số đo gọi đồng bộ `ChiSoPort`, vì kết quả lưu quyết định trạng thái công việc (C-09, C-13).
- Ngoại tuyến chỉ cho ghi nhận thực hiện, qua hai `NgoaiTuyenHandler` của khung 002 (C-14).

Plan tuân theo FR-010a của 000 (Q-306, Q-307):
- 005 không sinh, không gợi ý công việc; không tạo yêu cầu xem xét kế hoạch; không tạo công việc "hỗ trợ uống nước" hay công việc làm lại;
- hệ thống chỉ: chuyển trạng thái theo giờ (công việc, phiên bản kế hoạch, Vắng không báo), xác định trạng thái có mặt đầu ngày theo lịch đến, tạo yêu cầu cảnh báo, nhắc, báo, kiểm tra và chặn, tác động kèm lệnh của actor.

### Cập nhật 2026-10-18 (spec mục "Cập nhật 2026-10-18": Q-347, Q-349, Q-350)

- **Thỏa thuận lựa chọn có rủi ro** (FR-002a, FR-030): bảng `thoa_thuan_rui_ro` gắn phiên bản kế hoạch, kèm người tham gia và ý kiến có nguồn (dùng `NguonYKienApi` của 001); nhận định của Bác sĩ gắn với thỏa thuận; người quyết định theo BR-M01-15 tầng 2. Lệnh `PUT /ke-hoach-cham-soc/{id}/thoa-thuan`; checklist trả thỏa thuận của phiên bản Hiệu lực. Bảng riêng nên không bao giờ là căn cứ công việc (research C-18, data-model mục 3). *(R-15: `thoa_thuan_rui_ro` → `muc_ke_hoach`)*
- **Loại lý do Không thực hiện** (FR-028a): danh mục nhóm 1 `loai_ly_do_khong_thuc_hien` có dòng cố định "Người cao tuổi từ chối"; lệnh Không thực hiện nhận `loaiLyDoId` và `ghiChu`; app lưu danh mục để ghi ngoại tuyến (research C-14, C-18). *(R-15: `loai_ly_do_khong_thuc_hien` → `danh_muc_chung`)*
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
| X. Kiến trúc | Đạt | Một khối; gói `care`; module khác chỉ dùng `care.api`. Không có vòng phụ thuộc (C-02). REST `/api/v1`; MySQL; hai client (Q-346). NFR ở bảng bên dưới |
| XI. Nghiệp vụ ở backend | Đạt | Kiểm tra lập, quyền ghi, bảng chuyển, quy tắc mục F ở backend. Mỗi lệnh có `POST …/{id}/<lenh>` riêng; lệnh trạng thái người cao tuổi dùng endpoint của 001 |
| XII. Dữ liệu | Đạt | DBR-11, DBR-12 bằng cột sinh `UNIQUE` và khóa; nhóm 3 theo mẫu 000; nhật ký trong transaction; `Clock`; `thoi_diem_thiet_bi` cho ngoại tuyến (DBR-25) |
| XIII. Tham số | Đạt | Như V. Đổi CFG-M04-03 trong ngày áp từ lần kiểm tra kế tiếp |
| XIV. Ngoại tuyến, thông báo | Đạt | Ghi nhận thực hiện ngoại tuyến theo Q-01 qua khung 002; các lệnh khác chỉ trực tuyến. Mọi thông báo là sự kiện tới 009, có mức (C-13) |
| XV. Truy vết, kiểm thử | Đạt | Tên test theo BR, FR. Mỗi BR của 005 có test (data-model mục 12). BR-M04-27 và BR-M15-20 có thêm test Flutter |

**Kiểm lại sau Phase 1**: Đạt. Thiết kế không thêm vai trò, trạng thái hay quy tắc nghiệp vụ ngoài spec. Có sáu bổ sung kỹ thuật, giải thích ở Complexity Tracking:
1. Bảng `cham_soc_nct` làm đích khóa theo người. *(R-15: `cham_soc_nct` → `trang_thai_nct`)*
2. Trạng thái `DA_HUY_NGAY_NGHI` của dòng có mặt.
3. Lệnh lập công việc theo lô, từ chối cả lô.
4. Tám SPI khai báo ở `care.api` cho các module làm plan sau.
5. `ChiSoPort` gọi đồng bộ, khác các yêu cầu tới 007 đi qua outbox.
6. Hai job (theo phút, hằng ngày) và bảng `lan_chay_m04`. *(R-15: `lan_chay_m04` → `moc_da_nhac`)*

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
backend/src/main/java/com/eldercare/backend/care/
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

**Structure Decision**: Theo R-02 của 000. Feature 005 nằm ở `backend/…/care` với chín gói con nghiệp vụ, gói `trangthai` gom SPI cài cho module thấp hơn, gói `lich` và gói `api`. Phần 8.8 → 8.10 của M04 (hoạt động, ngoài viện, tinh thần, kiểm tra chất lượng) thuộc feature 014; plan 014 thêm gói con vào cùng `care`. Web có đủ chức năng. App nhân viên có checklist, ghi nhận ngoại tuyến và các thao tác tại tầng. App người thân không có màn của 005.

## Complexity Tracking

| Điểm cần giải thích | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Bảng `cham_soc_nct` (một dòng mỗi người) | Đích khóa tuần tự cho DBR-11, FR-006, khóa trùng theo người khi lập theo lô | Khóa dòng `nguoi_cao_tuoi` của M01 là truy cập dữ liệu module khác *(R-15: `cham_soc_nct` → `trang_thai_nct`)* |
| Trạng thái `DA_HUY_NGAY_NGHI` của dòng có mặt | Bảng FR-013 ghi "không còn trạng thái có mặt cho buổi đó"; giữ dòng để 010, 016 thấy buổi đã bị hủy do ngày nghỉ, có nhật ký | Xóa dòng: mất dấu vết và vi phạm "không mặc định CRUD" |
| Lệnh lập theo lô, từ chối cả lô khi một dòng lỗi | Người phụ trách ca lập hàng trăm công việc trong giờ giao ca; "hoặc toàn bộ, hoặc không" của 000; trả đủ lỗi một lần | Mỗi công việc một lệnh: chậm, và lần lập dừng giữa chừng để lại ca lập dở |
| Tám SPI khai báo ở `care.api` (008, 007, 006, 011, 012, 014) | 008, 007, 011, 014 phải gọi lệnh của 005 trong transaction của họ, nên phụ thuộc 005; 005 cần lại dữ liệu của họ | 005 gọi thẳng API của họ: vòng phụ thuộc, Modulith từ chối |
| `ChiSoPort` đồng bộ, các yêu cầu khác tới 007 qua outbox | Giá trị bị chặn hoặc cần đo lại thì công việc không được đóng (FR-034); chỉ biết khi 007 trả lời ngay | Outbox cho chỉ số: công việc đã đóng trong khi giá trị bị từ chối |
| Job theo phút, job hằng ngày, bảng `lan_chay_m04` | SC-005 đòi chuyển và nhắc ≤ 1 phút; FR-052 đòi chạy bù không trùng; mốc theo ca, theo ngày chạy đúng một lần. Chu kỳ và giờ chạy là thông số vận hành, không có trong Phụ lục 25 | Mỗi việc một job: khó bảo đảm thứ tự (Hiệu lực kế hoạch trước, rồi có mặt đầu ngày) và nhiều khóa *(R-15: `lan_chay_m04` → `moc_da_nhac`)* |

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

## Đồng bộ merge 2026-10-28 (chủ dự án duyệt)

Giữ API M04 đang được M09 trên develop gọi: TaskTypeApi.requirements trả TaskRequirements; ShiftTaskApi dùng onAbsence, onResignation và HandoverTransferResult onHandoverConfirmed với Long nextShiftId (có thể thiếu ca sau). Giữ onApprovedLeave và ba truy vấn CarePlanApi của 005; chưa cài provider story. NamedInterface gói api giữ tên checklist của develop. Không đổi caller M09 hoặc triển khai nghiệp vụ mới.

FakeM04Config giữ tám SPI/công tắc của 005 và FakeCareRequirements/FakeCareShifts cho hồi quy M09. Khi import cùng FakeM04ApiConfig, chỉ proxy API mới là Primary; fake M09 không tự bịa kết quả cho các hàm mới, thiếu fixture thì báo lỗi. Fixture API nhận đối số null để khớp hợp đồng bàn giao. Hai record tên trùng nghĩa do nhánh 005 khai báo được bỏ, glossary đồng bộ về kiểu develop.

Khóa Function.CARE_TASK_CATALOG đã được A công bố theo Q-391 trên develop; thay thế trạng thái chờ quyền cũ. T007/T007a còn chờ cài service/controller, không mở rộng công việc danh mục trong lượt giải quyết conflict này.

**Kết quả kiểm merge 2026-10-28**: Kiểm đầu vào bằng JDK25 lỗi biên dịch do marker conflict. Sau ghép: 73 test M04/fake/kiến trúc xanh; 265 IT M04/M09 có 264 qua, 1 error ở ShiftQueriesIT.FR_023_onDutyDoctorsArePresentDoctorsMarkedOnDutyFor009 do FK work_shift.schedule_id; không sửa test E. Spotless check sạch toàn bộ 2127 Java file. Lượt verify toàn kho trước sửa lỗi fake có 679 unit xanh, 1440 IT với 11 failure/117 error (gồm M01 đã bàn giao và lỗi fake đã được sửa, không coi là kết quả cuối xanh); không chạy lại toàn kho sau sửa, chỉ chạy lại M04/M09 và kiến trúc. Log ở backend/target/merge-m04-before.log, merge-m04-regression-red.log, merge-m04-m09-final.log. Không đánh xong phase.

## T007a — danh mục lý do Không thực hiện (2026-10-28)

Ghi chú hiện hành này thay các trạng thái chờ quyền/service T007a trong lịch sử 2026-10-27 ở trên: Function.CARE_TASK_CATALOG đã có trên develop theo Q-391. Cài NonPerformanceReasonType, NonPerformanceReasonStore, NonPerformanceReasonService, NonPerformanceReasonController ở cấu trúc internal/catalog/{domain,persistence,service} và web của README. T007 loại công việc/giá trị kết quả còn mở; không triển khai story hoặc giao diện.

Giữ đúng ba đường dẫn/bốn operation của api-005, response đọc chỉ id/name/fixed/status; POST201 và PUT200 trả ETag, không thêm body/GET chi tiết/kích hoạt lại/xóa. Tên trống hoặc quá200ký tự trả422 INVALID_VALUE; trùng tên đang hiệu lực409 DUPLICATE_NAME; dòng cố định409 REASON_TYPE_FIXED. Khóa pessimistic khi sửa/ngừng; PUT kiểm If-Match, trả412 VERSION_MISMATCH kèm phiên bản hiện tại. Quyền đặt tại service, audit cùng transaction; Idempotency-Key bắt buộc trên cả ba lệnh. Schema/seed/trigger đã có trong migration V005_01_1, không sửa migration đã merge.

Khoảng trống hợp đồng giữ nguyên: GET danh sách không có version/ETag từng dòng và chưa có GET chi tiết; client sửa dữ liệu cũ cần chủ dự án chốt cách lấy phiên bản trước khi làm UI. Không tự thêm trường hoặc endpoint.

Chủ dự án chọn phương án2: giữ ApiContractIT chung của A nguyên trạng, thêm bàn giao A đăng ký bốn operation T007a; các operation 005 chưa cài giữ pending. Gate toàn kho dự kiến báo đường dẫn chưa đăng ký cho tới khi A xử lý; không tắt test hoặc ẩn controller.

**TDD và review**: 15 ca HTTP đầu tiên đỏ404 khi chưa có controller. Lượt đầu sau cài phát hiện ba ca trả400 do Bean Validation thay vì422; chuyển kiểm tên về service để theo INVALID_VALUE chung. Review phát hiện header bắt buộc mới chỉ ở tài liệu; test bổ sung đỏ (thiếu key vẫn201), sau đó bắt buộc @RequestHeader tại controller. Reviewer đọc lại không còn finding quan trọng. Lượt clean verify cuối xác nhận FR_028a_ReasonCatalogHttpIT18/18, FR_028a_ReasonCatalogAtomicityIT3/3 và FR_028a_NonPerformanceReasonTypeIT2/2 xanh; đánh[x] riêng T007a theo yêu cầu task có test xanh, không đánh hoàn tất Phase2. Không sửa test chung A; lượt verify trước bị dừng để kiểm trên mã cuối, không dùng kết quả lượt đó làm bằng chứng.

**Kiểm cuối T007a**: nạp backend/.env, JDK25; mvnw.cmd -o clean verify nền kết thúc exit1 sau20:37phút:680unit/kiến trúc xanh;1468IT có7failure+9error,0skip. Đúng15lỗi/8lớpM01 baseline đã bàn giaoB; thêm1failure ApiContractIT.R_14_generatedApi_matchesContract nêu đúng ba đường dẫn lý do chưa đăng ký, theo lựa chọn giữ file A nguyên trạng. Không có lỗi M04:35IT/6lớp xanh, gồm23ca T007a. M15 tham số không tái hiện lỗi cũ. Failsafe dừng trước bước định dạng verify nên chạy riêng spotless:check, exit0. Log ngoài kho C:/Users/nguye/AppData/Local/Temp/eldercare-t007a-final-clean-verify-20261028.log; spotless ở eldercare-t007a-final-spotless-20261028.log. Nền/Phase2 còn mở; T007 tiếp theo, A đăng ký hợp đồng và B xử lý baseline. Không commit/push/PR/hoàn tất merge.

## Gộp bảng theo R-15 *(bổ sung 2026-10-29, constitution XII 3.2.0)*

- Ánh xạ bảng ở data-model mục 0; task T064. Trước khi viết migration, viết data-model mục 0.1 theo mẫu của 001, 009 (bảng gộp, loại ở bảng chung kèm nội dung, ràng buộc chuyển về backend, bảo đảm và test), rồi sửa bảng DBR áp dụng.
- Bảo đảm DBR, BR (XII): như feature đã cài, mỗi ràng buộc của bảng cũ có nơi bảo đảm mới (`khoa_duy_nhat`, cột sinh, `CHECK` theo `kind`, trigger khóa theo dòng hoặc domain kèm test tích hợp).
- Task, plan, research nhắc bảng cũ có ghi chú "R-15:" chỉ bảng đích: tạo bảng đích hoặc dùng bảng chung thay cho bảng cũ.

## Ghép develop d2bd7de0 — 2026-10-30

Chủ dự án duyệt xử lý toàn bộ conflict được báo: 9 file nội dung,87 đường dẫn AU do đổi package. Giữ toàn bộ API/DTO/event/fake và mã C đã cài, chuyển sang com.eldercare.backend.care/roombedallocation và support.fake.care. Chuyển37file còn ở cây cũ, bỏ10package-info rỗng cấp một trái README mới; test T001 kiểm lớp thật của bốn tầng thay vì kiểm khung gói rỗng. Chỉ đổi tổ chức kỹ thuật, không thêm story hoặc endpoint. ShiftTaskApi giữ6hàm, onApprovedLeave, MANDATORY và Long nextShiftId; không sửa caller staffing.

Giữ TechnicalDataCleanupJob của A và hai job Care trong SchedulerScopeArchTest bằng tên mới. Hai Care job dùng String runId theo SchedulerRuns mới; biên dịch trước sửa đã báo6lỗi kiểu long/String, sau sửa production và test đều biên dịch xanh. Thay đổi file chung do chủ dự án duyệt trong lượt ghép, bàn giao A review; không sửa ApiContractIT hoặc thêm SELF_CHECKED.

Giữ đủ lịch sử progress của hai nhánh; plan/glossary giữ cả kết quả C và kế hoạch R-15 mới. Tasks005 giữ T007a mở theo yêu cầu chuyển CatalogApi mới; bằng chứng23test cũ vẫn ghi lịch sử. Không dùng kết quả trên bảng riêng để nghiệm thu R-15.

Kiểm trước sửa lỗi do marker conflict; kiểm hẹp sau ghép:709unit/kiến trúc xanh,56IT có1failure F_04 mới của RecordGroupTriggerIT vì5bảng005 chưa khai DataGroup. Không sửa test A hoặc thêm ngoại lệ: thêm V005_02__declare_data_groups.sql chỉ COMMENT nhóm theo data-model005 mục1 (care_resident_lock nhóm2, m04_job_milestone kỹ thuật, task_type/certificate/result_value nhóm1). Không sửa migration đã merge, chưa làm gộp bảng T064. Kết quả clean verify cuối ghi bên dưới.

**Kiểm cuối ghép30/10**: clean verify nền nạp backend/.env/JDK25 kết thúc exit1/10:15phút. XML xác nhận709unit/kiến trúc xanh;1536IT có7failure+9error,0skip. 15lỗi/8lớp resident/M01 khớp baseline;1failure ApiContractIT do ba route T007a chưa đăng ký, tiếp tục giữ file A theo quyết định trước. care35IT/6lớp, roombedallocation252IT/52lớp, staffing251IT/36lớp đều xanh. F_04/RecordGroupTriggerIT6/6xanh sau V005_02; Spotless:check riêng sạch2228file. FR035 nguồn thật/fail-closed và ShiftQueriesIT cũ đều xanh; xóa các dòng lỗi test đã xử lý khỏi team-plan, giữ review API và nguồn thật chưa cài. Không đóng phase/R-15 hoặc thêm checkbox theo số test. Log C:/Users/nguye/AppData/Local/Temp/eldercare-merge-20261030-clean-verify.log; scoped/compile/spotless cùng tiền tố. Không commit/push/PR/hoàn tất merge.

## Review API 005–008 — 2026-10-30

Thực hiện lượt1 theo thứ tự chủ dự án xác nhận: đối chiếu hợp đồng005 mục1.2/1.4/1.5 và consumer008, DTO, fake/config hiện có. Báo cáo chi tiết ở [api-008-review.md](api-008-review.md). Không đổi API, source/test production, migration hoặc checkbox; chưa làm gộp R-15, chưa cài story mới.

Giữ đủ CarePlanApi, TaskRequirements, sáu callback ShiftTaskApi/MANDATORY và Long nextShiftId nullable. Hai cấu hình fake ghép được, caller008 giữ transaction/rollback. TaskHandoverApi (008 dùng) và HandoverTaskApi (khai báo Phase2) vẫn là hai hợp đồng hiện có; khi cài thật phải phục vụ cả hai hoặc trình duyệt thống nhất, không tự bỏ interface. Chưa có provider nghiệp vụ thật cho năm API đọc/ghi công việc và kế hoạch; test fake không đóng được việc nguồn thật.

Xác minh mới với JDK25 riêng process:19unit/8lớp và84IT/5lớp (AssignmentIT31,HandoverIT25,StaffEmploymentIT7,PostPublicationShiftIT17,PostPublicationPortsIT4),0failure/error/skip,hai lệnh exit0. Log TEMP:eldercare-c-api-review-unit-20261030.log và eldercare-c-api-review-it-20261030.log. Lượt sandbox đầu bị khóa cache Maven, không tính RED nghiệp vụ. Không chạy clean verify toàn kho trong lượt review này; không sửa Java nên không chạy spotless:apply.

Team-plan cập nhật riêng phần review005 ở năm dòng, giữ nguồn thật và review A/E/phần003/D còn mở. Log có lỗi async StaffingRatioEvents.status do thiếu nguồn/cấu hình tính lại tỷ lệ; thêm dòng bàn giao E xác minh, chưa kết luận baseline hoặc đã xử lý. Chưa commit/push/hoàn tất merge.


## Review nguồn ca/phân công M09 — 2026-10-30

Đã review phần C của các dòng lịch sử phụ trách,SPI ca tối thiểu và nối Phase4; bằng chứng ở api-008-review.md mục4. Nguồn DutyApi/AssignmentApi giữ lịch sử và scopeEndsAt đúng mốc thay/vắng;11unit+72IT xanh,0failure/error/skip,exit0. StaffModuleIntegrationIT dùng ScopeApi và bean staffing thật; DutyPermissionIT dùng fake nên không nghiệm thu Q-262 từ10ca này. Không đổi source/test/API/migration/checkbox,không chạy clean verify toàn kho.

Phụ thuộc mới xác định rõ: M09 chỉ cài CareShiftPort tối thiểu,trong khi005 công bố ShiftStaffPort khác chữ ký/DTO và có handoverTasks; bàn giao E cài bản thật theo hợp đồng005,giữ tương thích nguồn cũ. Còn provider story005; không ghi nguồn chính thức đã tích hợp. Q-262 và lỗi async tỷ lệ vẫn chờE (lỗi async tái hiện trong lượt này). Team-plan chỉ bỏ phần C review nguồn cũ,giữ phầnD/nguồn nghiệp vụ và thêm bàn giao cụ thể. Không commit/push/hoàn tất merge.

## Tiếp tục nền Phase 1–2 — 2026-10-31

Phạm vi chủ dự án xác nhận: hoàn thiện phần làm được của T001–T009/T007a,áp R-15 cho nền hiện có;chưa cài story Phase3. Dùng kế hoạch/tasks feature hiện tại,không tạo kế hoạch riêng hoặc đổi nhánh. TDD mỗi thay đổi;kiểm task và ArchTest sau task;cuối phase clean verify nền rồi mới xét checkbox. Giữ API/HTTP đã duyệt,không thêm thư viện hoặc sửa module khác.

Thứ tự kỹ thuật: (1) T006 kiểm đỏ công tắc khởi động rồi sửa hai job; (2) T005/T006 chuyển sáu bảng nền sang catalog_item,resident_module_state,reminder_marker bằng migration V005_95 và adapter qua API chung;test scratchFlyway chép dữ liệu và va chạm id; (3) T007a đổi store/domain sang CatalogApi,giữ quyền/audit/HTTP/id bản ghi cũ; (4) T007 cài sáu lệnh/hai GET danh mục trên CatalogApi,kiểm quyền,Q-391,ETag,Reason,audit/rollback; (5) T008 ánh xạ R-15 cho phần nền thực có,không tự tạo bảng story để nghiệm thu; (6) hồi quy T001–T009,clean verify,review cuối và cập nhật Working-history/progress/team-plan.

Quyết định: sửa T006 trước vì độc lập với schema,có thể kiểm hành vi rõ trước migration. Công tắc on-startup đọc cùng fallback eldercare.scheduling.enabled qua Environment hiện có để giữ constructor của job và test gọi trực tiếp,đồng nghĩa biểu thức @Value trong README;lịch định kỳ vẫn do SchedulingConfig điều khiển. Không cần sửa common hoặc test củaA.

T006 công tắc:RED3test có2failure đúng lỗi,bản sửa GREEN44unit/kiến trúc,0failure/error/skip,exit0/01:08phút;log005-p2-startup-{red,green}-20261031.log. Chưa đóngT006 vì còn đổi nơi lưu khóa/marker.

Quyết định R-15 danh mục nền: các bảng cũ có idAUTO_INCREMENT riêng nên có thể trùng nhau và catalog_item của module khác. V005_95 cấp id vật lý mới lớn hơn max id của tất cả nguồn và đích,attributes.legacy giữ table/id;adapterC trả id công khai cũ cho dòng chuyển,ID mới dùng id vật lý (được cấp sau toàn bộ dải cũ). Nhờ vậy loại/id của audit giữ nguyên,không phải ánh xạ lại nhật ký hoặc đổi HTTP/moduleAPI;không cần LegacyRecordTypeSource vì loại bản ghi không đổi. Không sửa cột/bảng chung củaA. Mốc cũ job/milestone có độ dài100: dùng markerSHA256(job+':'+milestone),kind/entity_typeM04.DAILY_WORK,entity_id0,details giữ giá trị gốc;không cắt chuỗi để lọt giới hạn bảng chung. Cột code100 của kết quả giữ trong attributes thay vì code40 của catalog_item. Tất cả có test scratch dữ liệu chuyển.

T007 RED12IT/12failure do endpoint404,0error;GREEN42unit/kiến trúc+12IT,0failure/error/skip,exit0/01:23phút,log005-p2-t007-{red,green}-20261031.log. CareCatalogService/Store dùng CatalogApi,TASK_TYPES/RESULT_VALUES có record nội dung BeanValidation theo enum/kiểu của schema cũ;controller/request/response đúngtầng vàđúng8operation,không thêmAPI. CareTaskTypeLookup cài TaskTypeApi hiệncó,đọc chứngchỉ mỗi lần để áp ngay;không cache. Bodyid/status là metadata do lệnh/vòngđời quảnlý nhưmẫu danh mục,không dùng để chọnID hoặc bỏqua lệnh ngừng. Khungnull giữnghĩa CFG;không áp mốc CFG vào danh mục trước khi lập mục. Mã/chứngchỉ giữ VARCHAR100 và NOTNULL của schema cũ,không tự thêm quy tắc khôngrỗng ngoài nguồn nghiệpvụ. Bổsung6ca auditrollback T007 để kiểm transaction thật;test schema/reasonSQL chuyển sang API mới,giữ nguyên các bảođảm có nghĩa sauR-15.

Kiểm ghép bean sau T007 phát hiện nguồn thật TaskTypeApi cùng FakeCareRequirements (khi chỉimportFakeM04Config) gâyNoUniqueBeanDefinition. Dừng JVM của lượt cleanverify đầu,chưa dùng kếtquả đó đểnghiệmthu. Tạo T003_RealCatalogCompatibilityTest trước sửa,RED1error đúng hai bean. Quyếtđịnh đặt @Fallback cho CareTaskTypeLookup:ứngdụng thật vẫn dùng nguồn duy nhất này;test/demo import fake chung thì bean fixture thường/Primary được ưu tiên,không sửa fake hoặc caller của module khác và không tạo fake thứhai. Giữ cấu hình FakeM04ApiConfig đãreview với cácproxyPrimary khi ghép cảhai cấu hình. Kiểm GREEN và cleanverify bảncuối ghi tiếp khi kếtthúc.

## Kết quả kiểm cuối nền và rà Phase3 — 2026-10-31

Cleanverify bảncuối kếtthúc exit1/09:50phút:721unit/kiếntrúc xanh;1563IT có7failure+9error,0skip;Spotless2250file sạch. M04/care56IT/9lớp,M03phònggiường258IT/53lớp,M09ca251IT/36lớp đều xanh. So log eldercare-c-allocation-handover-verify-20261031.log:16phươngthức lỗi trùng,không có phươngthức lỗi mới;15caM01 và1ApiContractIT (A chưa đăng kýapi005,giờcó12operationdanhmục9route),không gọi toànbackend xanh. T064M03 khôngtáihiện lần này,max267ms;không kếtluận đãsửa lỗi hiệunăng từlượt005. LogTEMP005-p2-clean-verify-final-20261031.log. Không chạy lại gate nếu chưa sửa mã hoặc có lỗi mới cần xác minh.

Reviewcuối tìm2lỗi đã có RED/GREEN:trùngtênđồngthời lọt500 và ghépTaskTypeApi thật/fake cũ bịambiguous. Bảncuối xửlý409đúng và dùngFallback;reviewđọc lại khôngcònfinding. Không đổi chữkýAPI/endpoint/field hoặc thêmthưviện. GiữcheckboxPhase1–2 vìgate chưaqua,giữT064cho phầnstory/DBR/triggercòn lại;T008readinessđãkiểm nhưngphảinốischema thật saugộp khi tạo story. PENDING_API_ROOT_TYPES.MedicationChecklistItem vẫn mở,không đổi đườngdẫn kiểucôngkhai hoặc sửa callerD tronglượt này.

User yêucầu ràPhase3 tronglúc kiểm nền:đãchỉđọc9taskT010–T017/T016a,hợpđồngđãduyệt và API nguồn. Chưa córuntime kếhoạch/thỏathuận. Đủcăn cứ chuẩn bị testT010/T011,sau chốt nền mớicài T012/T013→T014→T015→T016→T016a→T017. T012 dùng sốmigration mới vìV005_02đãdùng,không tạo lại bảngthỏathuận/ngườithamgia/ýkiến đãgộp theo0.1. FamilyRelationshipApi cóhàm nhưngchưa thấybeanproduction012;OpinionSourceService cóthật,dùngSPIquan hệ nênnhánhngườithân cầnnguồnthật để nghiệmthu. T017màn sửa danhmục cầnchốt cáchlấyversiontheohợpđồng;không tựthêmtrường/endpoint. Working-history.md ghi bảng từngtask và toàn bộlịch sửthực hiện. KhôngcàiPhase3 tronglượtràsoát.

## Triển khai Phase 3 theo yêu cầu mới — 2026-10-31

Chủ dự án yêu cầu thực hiện phần Phase3 có thể làm ngay sau báo cáo rà soát. Tiếp tục trên checkout hiện tại, giữ các thay đổi nền và lịch sử; không suy thành Phase2 đã nghiệm thu hoặc sửa các gate A/B. Không đổi hợp đồng đã duyệt, thư viện hoặc commit/push. Mỗi task có RED trước implementation và kiểm riêng cùng ArchTest; cuối lượt chạy clean verify nền và ghi kết quả thật, chỉ xét checkbox khi đủ điều kiện.

Kế hoạch: T010 cài hai hàm thuần về chuỗi thời điểm/so sánh; T011 viết IT schema/vòng đời trước T012–T014; T012 dùng migration mới sauV005_95, care_plan/care_plan_item đúng mục0.1, loạiTASK táchRISK_AGREEMENT, dữ liệu con qua bảng chung; T013 domain/read model đúng tầng; T014 service transaction/quyền/lifecycle/audit; T015 đăng ký bước đổi hiệu lực và nhắc trên khung job hiện có; T016 giữ11operation/CarePlanApi; T016a dùng OpinionSourceApi/FamilyRelationshipApi và fake chung khi kiểm; T017 chỉ làm phần không đòi đổi hợp đồng, phần lấyversion danh mục cần quyết định riêng. Điểm nghiệp vụ/hợp đồng chưa rõ sẽ hỏi, không tự thêm quy tắc.

T010 chọn CarePlanItemSequence.withinShift dùng CarePlanItemView hiện có làm đầu vào, trả các mốc đại diện trong khoảng nửa mở; không tạo bản ghi/công việc. EVERY_N_HOURS bắt đầu lại từng ngày nghiệp vụ, không lấy modulo từ hôm trước. Mốc không cố định trong ca dùng đầu ca làm đại diện; không tự rải giờ theo số lần/ngày. CarePlanComparison nhận map keyed by lineage/sourceItemId, bỏ qua id bản sao khi so sánh, trả thêm/bớt/đổi các trường nội dung. Chỉ các mụcTASK được đưa vào bộ so sánh này; thỏa thuận có luồng riêng, không biến thành căn cứ công việc. Không đổi DTO/API công khai; record kết quả ở internal/careplan/domain.

T012 đã kiểm GREEN: 41 ArchTest và 8 IT schema, không failure/error/skip; exit 0 sau 01:01 phút, log C:/Users/nguye/AppData/Local/Temp/005-p3-t012-schema-green-20261031.log. T011 vẫn còn test vòng đời ở service và thỏa thuận; chưa đóng T011 hoặc Phase 3.

Chủ dự án chọn phương án 2 trong câu hỏi về T017: duyệt thêm trường version chỉ đọc vào TaskType, ResultValue, NonPerformanceReasonType (2026-10-31). Đây là thay đổi hợp đồng HTTP đã được cho phép; không đổi endpoint hoặc module SPI. Client lấy version từng dòng từ GET và gửi If-Match. Test hợp đồng và HTTP viết trước: lần đầu test không biên dịch vì getProperties trả raw Object, đã sửa cast Schema; RED thực tế ở lần 2 do schema chưa có version. Sau đó mới sửa ba response và api-005.yaml. Một lượt kiểm GREEN bị ProfileOpenGuardArchTest lỗi không tìm thấy fixture đúng lúc một Maven khác biên dịch cùng target; không coi lượt đó là bằng chứng xanh, chạy lại tuần tự để xác minh. Không sửa test hoặc mã của B.
T011/T013/T014/T016 dùng lát cắt HTTP thật để kiểm vòng đời, quyền và ETag trước implementation, thay vì thêm fake kế hoạch. Thứ tự kỹ thuật này cho phép test cùng transaction/audit thực; không đổi thứ tự phụ thuộc nghiệp vụ. T013 lưu snapshot typed ở domain, SQL chỉ ở persistence. Các lệnh khóa resident trước, sau đó đọc plan bằng locking read để không dùng snapshot Repeatable Read cũ khi hai lệnh chờ nhau. T017 version đã duyệt không chặn phần backend kế hoạch.

Kiểm tuần tự bảng chuyển và version: exit 0/01:40 phút; 45 unit/Arch và 30 IT xanh (0 failure/error/skip), log 005-p3-transitions-version-green-20261031.log. Lỗi fixture ArchTest ở lượt chồng Maven không tái hiện sau chạy tuần tự; không sửa test/mã B.
Quyết định nghiệp vụ do chủ dự án chốt 2026-10-31: mỗi phiên bản chỉ một mục mang waterTargetMl; mục đó thuộc nhóm DRINKING_ASSISTANCE. Backend trả 422 trước ghi nếu danh sách có nhiều mục tiêu. Không tự cộng hoặc chọn mục đầu. Test RED/GREEN ghi ở Working-history.md.

Lát cắt backend đã có 13 IT vòng đời/thỏa thuận/nhắc xanh cùng 44 unit/Arch (log 005-p3-reminders-green-20261031.log), chưa nghiệm thu phase. So sánh Nháp giữ null của khung, không ép CarePlanItemView có int thành giá trị CFG mới. CarePlanDifference dùng nội dung nullable cho HTTP, CarePlanComparison hiện có phục vụ projection đã cố định. Mục TASK và RISK_AGREEMENT luôn lọc theo kind; ý kiến/người tham gia qua RecordEventApi; thỏa thuận sao chép giữ metadata nguồn. Notice duyệt dùng ApproverApi và từng payload nhỏ, không mang danh sách dài trong outbox.

T017: generator chưa đăng ký 005; thêm đăng ký và bộ lọc x-implementation-phase như 003, chỉ 23 operation đã duyệt (11 kế hoạch + 12 danh mục). npm run gen:api tạo api005/index, test client 2/2 xanh. Bàn giao A review hai dòng generator; không sửa client bằng tay. Màn ở features/care-tasks theo README, không tạo thư mục theo tên logic trong tasks. Test form được chạy RED trước component. Không đổi package.json/common hoặc thêm thư viện.
### Review và khắc phục Phase 3 — 2026-10-31

Reviewer chỉ đọc, không chạy Maven: xác định mất cập nhật do snapshot RR, thông báo Trả lại/Thu hồi gửi người được phân công thay vì người lập, thiếu nhóm Người phụ trách ca/Trưởng tầng khi đổi hiệu lực, thiếu lịch sử nội dung mục Nháp, và guard trạng thái cuối khi kích hoạt. Đã bổ sung test trước sửa: 16 IT đỏ 5 failure đúng hành vi (005-p3-review-red-20261031.log). Mục được đọc FOR UPDATE sau khóa resident, nhật ký ITEMS_REPLACED có before/after items, người lập được ánh xạ AccountApi → RecipientGroup.Staff, bước hiệu lực kiểm trạng thái cuối và gửi ShiftLead/FloorManager của tầng phân bổ. Khi chưa có vị trí, không tự bịa tầng: giữ AssignedNurse với Substitution.Default của notification; cần kiểm nguồn thật theo phụ thuộc liên module trước nghiệm thu đầy đủ.

Kiểm thêm race danh mục: lệnh thay mục đang chờ resident lock vẫn thấy loại vừa bị ngừng vì snapshot cũ; RED 1 failure (005-p3-catalog-snapshot-red-20261031.log). Entry point lệnh của CarePlanService/CareRiskService và runner/bước job của C dùng READ_COMMITTED để các API đọc tham số/danh mục/nguồn sau chờ thấy dữ liệu đã commit. Các khóa resident/parent/UNIQUE vẫn quyết định tính duy nhất. lockedItems là current read nên trường hợp caller đã có outer RR vẫn không ghi ngược nội dung mục; có test hai transaction thật. Không thay isolation hoặc API của module A/B.

Hai test bổ sung RED: ngày hiệu lực đã được trả lại/duyệt sang ngày mới trong khi job chờ khóa phải được kiểm lại, và thu hồi phải giữ approvedBy/approvedAt/selfApproved của lần duyệt trong bản đã hủy. GREEN cuối của nhóm fix: 45 unit/Arch và 17 IT, không failure/error/skip, exit 0/02:26 phút; log005-p3-review-green-20261031.log.

Retention: T008 có RED 1 failure vì provider TASK đọc cả RISK_AGREEMENT. Sửa CareRetentionType/CareGovernanceStore để hai loại cùng care_plan_item nhưng lọc kind, tham chiếu từ plan/source trả đúng dataType; record_event của ý kiến/người tham gia được SharedRowRetention của 000 xóa cùng EntityRef M04.RISK_AGREEMENT, care không SQL/xóa bảng chung. GREEN 41 Arch +6 IT, exit0; log005-p3-retention-green-20261031.log. Không đổi loại bản ghi công khai hoặc di chuyển id bản ghi cũ nên không cần LegacyRecordTypeSource mới cho phần này.

Web: test form RED trước component, sau cài 7/7 xanh; test danh sách/menu RED trước Page/registration, kỳ vọng tìm text trùng option/button đã sửa truy vấn theo role (không đổi behavior để ép xanh). Kiểm toàn frontend: 361 test/64 file xanh, npm run lint exit0, npm run build exit0 (còn cảnh báo chunk>500kB chung). UI mới ở features/care-tasks, App/navigation chỉ thêm hai đăng ký của005. Không có dữ liệu minh họa trong source production.

Kiểm hình ảnh chưa hoàn tất: harness tạm phase3-visual.test.html/.jsx dùng fixture được tạo để kiểm, Vite chạy cục bộ; IAB không truy cập localhost (refused/timeout), truy cập Chrome qua Computer Use chờ quyền rồi timeout. Đã dừng Vite và xóa cả hai file harness; không ghi đã xem/chụp hoặc đạt kiểm trực quan. Không có ảnh màn hình trong bản giao.

Review đọc lại nêu thêm create phải kiểm writable sau resident lock; đang có test RED/GREEN riêng. FR-003 yêu cầu thông báo Trả lại kèm lý do nhưng public CarePlanReturned chưa có reason: đã trình chủ dự án thêm String reason, giữ constructor cũ và chỉ đưa lý do vào ContentPart HEALTH, không tiêu đề/push. Chưa tự đổi event trong lúc chờ duyệt; phần độc lập tiếp tục làm.

Kiểm bổ sung 31/10: create-final-race đã GREEN 41 Arch +3 IT. Sáu payload null trong danh sách đã RED (NPE) rồi GREEN với lỗi 422; OpenAPI tạo kế hoạch khai 201/ETag theo hợp đồng, test RED rồi GREEN. Nhóm HTTP cuối: 41 Arch +22 IT, exit0. Form giờ giữ chuỗi đang nhập đến submit để không mất dấu phẩy; trường timesText chỉ ở UI, không gửi API. Test tái hiện RED rồi GREEN; toàn frontend cuối 362 test/65file, lint/build exit0. Lượt clean verify đầu thất bại Spotless vì sửa test trong lúc chạy; đã định dạng và chạy lại clean verify cuối với nguồn cố định, chưa coi lượt trước là gate đạt. Chi tiết log và quan hệ file ở Working-history.md.

Thứ tự tiếp tục sau lượt kiểm cuối: (1) T011/T014: chốt đề xuất CarePlanReturned.reason trước khi viết test RED và thay hợp đồng; (2) T016: A review/đăng ký 23 operation005 trong ApiContractIT, không vô hiệu test chung; (3) T016a: kiểm lại với FamilyRelationshipApi thật khi chủ nguồn cung cấp, giữ nguồn fake hiện có cho test cô lập; (4) T017: kiểm hình ảnh và review phần đăng ký web chung; (5) sau khi xử lý gate A/B, chạy lại clean verify rồi nghiệm thu/đánh checkbox Phase3. Task kế tiếp theo tasks.md là T018 rồi T019 của Phase4, chỉ bắt đầu khi có yêu cầu và qua checkpoint; lượt hiện tại không cài công việc của ca hoặc callback T057.

Kết quả clean verify cuối31/10: exit1/17:42 phút;732unit/Arch xanh,1597IT có7failure+9error/0skip. Đọc XML:90ITcare,258ITM03,251ITstaffing xanh. Spotless2297file sạch. So đủ tên16phương thức lỗi với baseline005-p2-clean-verify-final-20261031.log không thêm/bớt:15resident/B và1ApiContractIT/A chưa đăng ký005; không sửa test/module người khác. Giữ checkbox mở; frontend362test/65file, lint/build exit0. Log cuối005-p3-clean-verify-final-20261031.log và nhật ký đầy đủ tại Working-history.md.

### Bổ sung sau phê duyệt CarePlanReturned.reason — 2026-10-31

Chủ dự án duyệt thêm String reason, giữ constructor ba tham số và tương thích JSON cũ; lý do chỉ trong ContentPart HEALTH, không trong title/shortContent. Đã TDD: HTTP test đỏ1failure do thiếu HEALTH, rồi sửa producer RETURN truyền nguyên Reason.value; CarePlanReturned override notificationRequests và dùng overload nội bộ của CareEventNotifications, không đổi nhóm nhận/mức/key hay API HTTP. NFR_04_CareEventsTest kiểm constructor/JSON cũ và roundtrip lý do không cắt.

Test biên phát hiện JSON4233ký tự với lý do hợp lệ2000dấu ngoặc kép, vượt VARCHAR(4000) của outbox. Sau khi trình phương án đổi duy nhất cột SERIALIZED_EVENT sang TEXT NOT NULL, chủ dự án chọn chờ A mở rộng trước. Không tạo migration; giữ nguyên giới hạn Reason2000, không cắt nội dung hoặc đổi hợp đồng để né lỗi. Đã chuyển kiểm độ dài cố định thành ca lưu/đọc thật FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage để A có thể kiểm migration khi cung cấp; đây là test đỏ cần giải quyết, không tắt/bỏ qua.

Gate task `-Dtest=NFR_04_CareEventsTest,*ArchTest -Dit.test=FR_003_CarePlanLifecycleIT verify`:47unit/Arch xanh,23IT có1error do MySQL Data too long for column SERIALIZED_EVENT,22IT còn lại xanh;Spotless2297file sạch;exit1/01:59phút. Log005-p3-return-reason-scoped-20261031.log. Kết quả clean verify trước bổ sung reason không đại diện nguồn mới; chưa nghiệm thu và không dùng để đánh checkbox. Chờ A rồi chạy lại ca biên, bộ task/kiến trúc và clean verify cuối trước khi đóng Phase3; không chạy lại toàn kho trong lượt chờ dependency đã được chủ dự án chọn.

ModuleBoundaryTest kiểm riêng thêm1test xanh,exit0/18.429giây,log005-p3-return-reason-boundary-20261031.log. Tổng48unit/Arch/Boundary xanh qua hai lượt; giữ nguyên1IT đỏ outbox. Không sửa Java sau kiểm task, không thay đổi web hay chạy lại frontend cho thay đổi chỉ ở backend này.

## Tiếp nhận và triển khai Phase 4 — 2026-11-01

Chủ C HungNX-nguyen yêu cầu T018–T026, giữ nhánh feature/005-daily-care-and-hand-over (HEAD đầu lượt df032811), không commit/push/đổi nhánh. Đã báo 7 dòng chứa tên GitHub trong mục8; các dòng C không ghi tên vẫn là phụ thuộc cần theo dõi. Giữ gate Phase3/outbox và A/B, không coi Phase3 đã nghiệm thu.

Chủ dự án trả lời “1” duyệt mục1–3 của phase4-contract-proposal.md: dịch7operation T025/các schema, rowErrors từ1, TaskSourcePort.sources và ShiftStaffPort.members; giữ chữ ký cũ, sửa fake duy nhất hiện có. Mục5 (ongoingShifts cho T024) và mục6 (ApiClient mobile hỗ trợ mảng/Problem Details đầy đủ) đã gửi câu hỏi riêng, chưa có câu trả lời tính tới ghi chú này. Không suy elapsed time thành duyệt.

T018 dùng CareTaskTransitions thuần ở internal/task/domain. RED5failure/0error do thiếu class, GREEN46unit/Arch/Boundary. T019/20–23/25 triển khai lát cắt HTTP–transaction–SQL để kiểm nguyên tử xuyên tầng:10IT RED404 trước controller/migration; V005_97 mới vì số cũ đã dùng, care_task BUSINESS, UNIQUE sinh và trigger khóa trạng thái cuối; snapshot typed CareTaskData trong JSON, các cột ràng buộc/chỉ mục để riêng. Lịch sử qua StatusHistoryApi, không tạo care_task_history. Không thêm thư viện.

Lượt đầu cài có lỗi compile do AccountView dùng accountId, đã đối chiếu DTO và sửa. Lượt kế48unit/Arch xanh;10IT có9xanh/1error do FakeM04Config kèm FakeCareRequirements lấn CarePlanApi @Fallback. Trong nội bộ005, validator/query dùng CarePlanQueries cụ thể để đọc nguồn thật cùng module; không thêm fake hoặc sửa nguồn E. Hai callback T025 có RED2failure/0error thiếu provider; đang cài CareTaskSources MANDATORY. Có bổ sung test7unit validator và4IT callback/unique/trigger, chưa ghi GREEN trước khi chạy.

T026 web theo component hiện có, có RED3failure/1pass sau scaffold; lỗi import component trước scaffold không tính RED hành vi. Đã sinh client qua npm run gen:api (7operation mới, không sửa tay). Màn giữ dòng tự nhập/lỗi theo dòng, idempotency key giữ qua retry cùng payload; chưa nghiệm thu web/mobile/kiểm trực quan.

## Quyết định và rà soát tiếp Phase 4 — 2026-11-01

Chủ dự án chọn phương án 2 cho cả mục 5–6 của phase4-contract-proposal.md: giữ nguyên SPI hiện có, T024 chờ E cung cấp nguồn liệt kê đầy đủ ca; không cho C sửa ApiClient nền, T026 chờ A cung cấp transport hỗ trợ mảng JSON và Problem Details.rowErrors. Không thêm ongoingShifts, không sửa mobile/lib/common/api/api_client.dart. Chủ dự án chọn phương án 1 cho cảnh báo Bắt buộc gắn vị trí: chờ thống nhất với chủ 007; không tự gán residentId hoặc id giả. Các lựa chọn này là quyết định hoãn, không còn là câu hỏi duyệt đang chờ.

Các quyết định kỹ thuật sau review: REWORK chỉ được lập qua lệnh đã kiểm QualityCheckPort.failed và có originalTaskId; lập lô không được dùng REWORK để bỏ kiểm căn cứ. Trước khi dùng floor của ca làm Target, kiểm BedAllocationApi.openAt tại plannedAt: phân bổ có sẵn phải đúng floor; không có phân bổ thì cần phân công xác thực trong ca. Nguồn ca qua nửa đêm hợp nhất mục kế hoạch hiệu lực của từng ngày nghiệp vụ giao với [startsAt,endsAt), không sinh công việc. Web lấy thành viên vị trí qua GET /shifts/{id} đã có của 008, lọc absent/approvedLeave; mobile nhận cùng dữ liệu qua gateway.shiftMembers và chờ adapter A. Không thêm trường vào HTTP005 hoặc thêm thư viện.

Ba IT review đã chạy RED (3 failure/0 error): REWORK bypass trả201 thay409, resident tầng khác trả201 thay403, thiếu kế hoạch ngày sau của ca đêm. Test chọn thành viên không có assignment theo người cũng RED ở web (1/6) và mobile (1/5), rồi mới sửa. Review chỉ đọc xác nhận các đường mã đã xử lý bốn finding; không thay thế kết quả test.

Gate cảnh báo vị trí chưa giải quyết: CareTaskNotices.overdue trả503 khi MANDATORY không có residentId; khi chạy bù có thể rollback toàn transaction CareTaskDueStep. Chưa nghiệm thu T024 hoặc cho rằng đã xử lý trường hợp này. Cần chốt hợp đồng với 007 trước khi bàn giao production. Nguồn ShiftStaffPort thật cũng chưa có; test fake không đại diện tích hợp thật.

Kiểm biên T024 phát hiện fixture truyền Instant nano trong khi MySQL lưu DATETIME(6), gây failure ngẫu nhiên ở đúng đầu khung. Đổi test đọc window_from/window_to đã lưu để kiểm đúng hai biên, không sửa thời gian production/giảm assertion. Trong quá trình rà có thêm thử nghiệm DUE → OVERDUE và chạy RED đúng thiếu bước này; đối chiếu tasks.md xác nhận đó là T044/Phase7, nên trả test về phạm vi T024 (NOT_YET_DUE → DUE, lặp không ghi thêm). Không cài T044 sớm hoặc ghi Phase7 đã làm. Bản test cuối vẫn kiểm đúng đầu/cuối khung trong Phase4; ca catch-up vào OVERDUE và nhắc khi lập muộn giữ nguyên. Log thử nghiệm005-p4-clock-expiry-red.log có1failure/0error ở assert OVERDUE; không dùng làm gate Phase4.

Kết quả task cuối Phase4: `005-p4-final-scoped.log` BUILD SUCCESS, exit0/01:11 phút;58unit/Arch/Boundary và31IT,0failure/error/skip;Spotless2323file sạch. Test biên dùng mốc MySQL đã lưu, không còn failure ngẫu nhiên ở lượt này. Review chỉ đọc cuối xác nhận T024/T044 và docs nhất quán, không có finding mới ngoài các gate đã nêu. Sau đó chạy nền `.\mvnw.cmd clean verify` toàn backend, log005-p4-clean-verify-final.log; giữ nguồn cố định, chưa ghi kết quả khi lệnh còn chạy. Frontend368test/67file,lint/build exit0;mobile49test xanh và analyze phầnPhase4 khôngissue. Không đánh checkbox trước khi đọc gate cuối và không coi dependency đã hoãn là hoàn thành.

Lượt cleanverify đầu sau kiểm task đã dừng chủ động khi phát hiện fixture T008 còn CREATE TABLE care_task;751unit/Arch và31ITPhase4 lúc đó xanh nhưng không coi lượt bị ngắt là gate cuối. Cập nhật T008_CareGovernanceIT sang schema thật V005_97, id riêng, giữ kiểm candidate/scope/retention và kiểm xóa thật; không CREATE/DROP hoặc giả định bảng rỗng. Đây là nối schema thật theo ghi chú T008 khi story được tạo, không đổi nghiệp vụ/API. Đang kiểm riêng T008/Arch rồi cleanverify mới;gate ParameterAsOfIT/A xuất hiện thêm cần đọc kết quả cuối,không gộp tất cả lỗi vào baseline.

T008 dùng schema thật đã GREEN:41Arch/Boundary+6IT,0failure/error/skip,Spotless2323file,exit0/01:01phút (005-p4-governance-green.log). Reviewer khôngfinding; bắt đầu cleanverify nền mới005-p4-clean-verify-final2.log,nguồn cố định.

### Kết quả cuối lượt Phase 4 — 2026-11-01

Gate cuối `005-p4-clean-verify-final2.log` kết thúc **exit1 sau10:14phút**:751unit/Arch xanh;1629IT có8failure+10error,0skip;Spotless2323file sạch. 31ITPhase4 và6ITT008 đều xanh. So đủ tên với baseline005-p3-clean-verify-final-20261031.log:giữ nguyên16phươngthức lỗi,thêm2ca là FR_003_maximumEscapedReturnReasonSurvivesOutboxStorage (outbox reason đã biết/chờA) và ParameterAsOfIT.US4_8_valueAtPastInstant_isTheValueCurrentThen (expected20M/actual10M,mới so baseline,tái hiện cảhai lượtcleanPhase4,đã bàn giaoA). Không nói toànbackend xanh hoặc tất cả lỗi đều baseline. Không sửa testA/B hoặc bỏtest.

Giữ toàn bộ checkboxPhase4[ ]:T019 còn kiểm ca rỗng;T024 chờ nguồn E và hợp đồng cảnh báo vị trí007 (guard503/có thểrollback lượtchạybù vẫn tồn tại);T025 chờ nguồn thật;T026mobile chờ transportA/đăngkýapp và UI thực tế chưakiểm. Phần cài đãkiểm vẫn ởcheckout,chưacommit/push/PR. Tiếp tục đúng thứ tựtasks:T019test ca rỗng saukhicónguồn;T020–T023giữbảncàiđãkiểm/ràghépnguồn;T024nhắc ca rỗng/cảnhbáovịtrí;T025kiểmnguồnthật;T026adaptermobile/đăngkýmàn và nghiệmthuUI;sauđócleanverifyvàgateA/B trướccheckbox. T027Phase5chưabắtđầu.

## Kế hoạch triển khai Phase 5 — 2026-11-01

Phạm vi T027–T031 theo yêu cầu mới, giữ checkout hiện tại HEADfe7891ea và các gate phase trước. Không đổi nhánh/commit/push,khôngthêmthưviện. T027 viết test bảngFR013 rồi IT luồngđiểmdanh/schema; T028 dùng SimpleRecordApi/RecordEventApi thay hai bảng logic, UNIQUE loại/người/ngày và chỉmục module trong migration mới; T029 service khóa resident→attendance, audit/history/correction và API đọc/checkout/giờvề; T030 đăng ký CareDailyWork/CareMinuteWork hiệncó; T031 chỉ dịch/cài HTTP sau duyệt,web/mobile dùng client sinh/phần nền hiện có. Sau mỗi lát cắt kiểm riêng task+Arch; cuối cleanverify nền trước checkbox. Working-history.md là ledger thực hiện, không tự chạy script tạo commit hoặc thay checkout từ skill vì user cấm git mutation chưa duyệt.

Các interface dùng chung: T027/T028/T029 cùng model trạng thái/khóa unique; T029/T030 cùng calendar/schedule/clock và marker; T031 dùng service/correction và hợp đồng đã dịch. Không tự tạo attendance_record business bằng job: FR012/C06 xác định trạng thái đầu ngày từ lịch có sẵn, điểm danh chỉ từ lệnh người dùng. Thiếu nguồn holiday/absence và event004,đã trình phase5-contract-proposal.md,không default false hoặc khai hộ eventB. Phần thuầnT027 độc lập được làm trước khi chờAPI.

T027 phần bảngchuyển:RED2failure→GREEN43unit/Arch/Boundary (005-p5-transitions-{red,green}.log),chưahoànthànhITluồng. T028:FR_012_AttendanceSchemaIT RED3/4test trước migration V005_94;GREEN41Arch+4IT/01:01phút. Mởrộngsimple_record bằng cột/indextiềntốm04 vàUNIQUEngười/effective_from,cácloạikhácNULL;khôngtạobảngstoryriêng. ĐangkiểmthêmJSONnullchoexpectedArrivalcủabuổiphátsinh. Reviewđộclậpchưafindingbắtbuộc,đềxuấtAPIvẫnchờtrảlời;khôngđánh[x]/chưanghiệmthuPhase5.

Checkpoint cuối phần độc lập trước duyệt API:5ITschema+43unit/Arch/Boundary xanh,0failure/error/skip,Spotless2326file,exit0/47.274giây(005-p5-checkpoint-green2.log). TestbuổiphátsinhJSONnull đãRED1error;NULLIFsửađầu làmgiờthậtlỗi,đãgiữtestvàthayCASEkiểmJSON_TYPErõràngtrướcCAST. KhôngsửaJava/migrationsaukiểm. ChưacàiT029–T031/chưachạycleanverifycuốiphasevìAPIcònchờduyệt;giữcheckboxT027–T031[ ]. TiếpT027ITluồng→T028payload/runtime→T029service/correction/provider→T030jobs→T031HTTP/UI→gatecuối. KhôngđổiAPIsẵnhoặccommon/mobiletransportkhi chưa códuyệt.

### Sau duyệt hợp đồng Phase5

Chủdựánchọn1duyệttoànbộđềxuất(4HTTP/calendarSPI/refreshcallback). TesthợpđồngRED2trướcđổi;fakecalendar005duynhất,giữchữkýcũ,clientwebsinhlại. RED10IT(8failure404+2errorchưaprovider)trướcservice/controller. SaukhắcphụcArchcontrollerđipersistencebằngAttendanceQueries,GREEN43unit/Arch+10IT/01:18phút,Spotless2342file. Tiếp5testhủycôngviệc/noShow/daystart/dayend/correctionđãRED5/15trướccài. APIgetonDateđọcprojectionđãlưu;callbackcùngtransaction;parentattendance/record_eventlàM01.RESIDENTđểretention000xóatoànchuỗi,recordcontentgiữattendanceId. Khôngtạobảngriêng.

Web5testphầnmàn/menuđãxanh(REDtrướccài),mobile3widgettestxanh(REDtrướccài),chưaadapterproductionvìtransportAkhôngđọcmảngJSON vàuserđãchọnchờA. CâuhỏinghiệpvụVOIDcònchờ,AMEND_CONTENTtiếptụctheohợpđồngđãduyệt. CácgatePhase4vẫngiữ;khôngngầmnghiệmthu.

### Review và các gate Phase5

Review chỉđọc xácnhậnP1retention:parentM01.RESIDENTchỉbảođảmxóarawsharedrows,khôngtựenumerateM04.ATTENDANCE_RECORDđểCorrectionRetentionkiểmhạn trướcxóa. AliasCareRetentionTypecòntrỏbảngcũkhôngtồntại. BàngiaoAadapter/APIsharedretention;khôngSQLbảngchunghoặcpurgecảresidenttừ005đểnéthiếuhàm. Đínhchínhghi chútrướcrằngparentM01đãbảođảmtoànchuỗi:chưachứngminh/chưađạt,đặcbiệtcorrection/chaincóthểbịmồcôi. KhôngnghiệmthuT028/T029/R15trướckhigiảiquyếtgate.

ReviewP1LOAI_LUU_TRU:callback004đãđổiDAY_CAREsangnộitrúthìguardcheckResident409,rollbacklệnhngoài;rowNOT_ARRIVEDcũcũngcóthểlàmrollbackjobtheophút. Đãhỏinghiệpvụriêng,chưatựgánHOLIDAY_CANCELLEDhaybỏdòng. ReviewP2VOID:cáchCtạoapprovaltrướckhiafterCorrectionguardchạy;đangTDDchặnsớmendpoint005,generic000cầnnốichínhsáchđínhchínhđầyđủsaukhichốtnghiệpvụ. KhôngcoiVOIDđãhỗtrợ.

### Điều chỉnh kỹ thuật ghép nguồn004 theo C-02

Đọc lại research C-02 xác nhận chiều mã005→004. Đề xuất đểBtrựctiếpcàiSPIcare/callcallbackcaretừmoduleadmission cầnđiềuchỉnh: BcôngbốAPI/eventadmission,CcàiadapterDayCareCalendarPort/listenersyncincarevàgọirefreshCalendartrongtransactionnguồn. NếuBimportcaretrongkhicaređãimportContractReadApithìModulithtừchốivòng. GiữchữkýAPIđãduyệt,khôngviếtBprovider/eventkhichưacócontract/types;testfakecarekhôngchứngminhnguồnproduction. Đây là lýdo kỹthuậttrongplan,handoverphảigiữchiềumãđúng.

Kiểmcuốitrướcgate:005-p5-final-scoped.logBUILD SUCCESS,46unit/Arch/Boundary+29IT(18luồng+5schema+6T008),0failure/error/skip,Spotless2351file,exit0/01:50phút. AuditdiffgiờvềđãRED1rồiGREEN;chỉghi trường thayđổi,giữnullvàrecord/registration/absenceIDsđểtruyvết,metadataquyềntrườngcủa005bổsungtươngứng. VOIDcontrollerđãRED1trướcsửavàgiờchặn409trướctạoapproval. Webfinal374test/69file,lint/buildexit0;mobilefull52test/analyze3filenoissue. Đãbắtđầucleanverifytoànbackendnền005-p5-clean-verify-final.log,giữnguồncốđịnh,chưaghiKQkhichưaxong.

### Quyết định nghiệp vụ và xác minh bị chặn — 2026-11-01

Chủ dự án đã trả lời “1” cho hai câu hỏi riêng: (1) triển khai AMEND_CONTENT sửa giờ trước, VOID chờ chốt; (2) nhánh LOAI_LUU_TRU chuyển khỏi DAY_CARE chờ thống nhất với B. Không còn coi hai câu hỏi này đang đợi trả lời. Giữ guard và gate đã mô tả; không tự quyết trạng thái hủy/tính phí/công việc ngày đổi.

Clean verify nền `005-p5-clean-verify-final.log` đã kết thúc exit1 ởunit:756test,2failure/0error/0skip;chưa chạy IT. Hai failure đều từ thay đổi005: T003_PublicCareApiTest còn kỳ vọng6hàm DayCareAttendanceApi trong khi callback đã duyệt làm thành7; T002_FakeSwitchContractTest phát hiện DayCareAttendanceCorrection chèn sai cấp trongrequest của /testing/fakes do phép replace khôngneođầudòng. Đã cập nhật testC sang7vàbổsungkiểmMANDATORYrefreshCalendar;giữtestT002nguyênvẹn. BổsungparserTestFR015trongFR_012_AttendanceContractTestđểbắtcomponentchènsaicấplầnsauthayvìchỉtìmchuỗi.

Lệnh Maven kiểm lại các test hợp đồng/Arch đã bị automatic approval review không thực thi vì hết usage limit (không phải kết luậnunsafe). Không chạy Maven bằng đường khác để né kiểm duyệt. Sửa YAML dựa trên RED thật T002 đã có: dựng lại từ HEAD, chỉ thay4path/schema đãduyệt, chèncomponentbằngregexneo ^  responses ởcomponents. Kiểm tĩnh bằng Node/yaml/assert đã qua:4pathmới đủ,requestBody làcomponent thật,testing/fakes giữ204/400;mọipath/component cũngoài4operation thayđổi códeepEqualvớiHEAD. npmrungen:api chạy được;SHA256api005 trước/sau sửaYAML giữ nguyên. Đây không thay thếMaven/Spotless/cleanverify;hai filetestJava mới sửa chưa kiểm Maven hoặc định dạng lại bằngSpotless.

Cần khôi phục hạn mức của phiên để chạy spotless:apply,test hợpđồng/Arch và cleanverify nền mới. Chưa đánh[x]T027–T031,khônggọiphasehoànthành. Các kếtquả46unit+29ITxanh trước sửa hợpđồngcuối được giữ đúng mốc,không suy thành toànbackend hiện tại xanh. Frontend374test/69file,lint/buildxanh;mobile52test/analyzephầnmớixanh,khôngsửaUI sau các gate đó.


**Ghi chú khi merge — ưu tiên listener của B/E (chủ dự án đã chốt):** nếu nhánh ghép đã có listener và kiểu sự kiện nguồn tương ứng do B/E cung cấp, ưu tiên dùng bản của B/E thay cho đường tích hợp tạm đã làm ở005. Đối chiếu và hợp nhất adapter/callback hiện có để mỗi sự kiện chỉ được xử lý một lần; bỏ phần tích hợp tạm bị thay thế khi đã kiểm tương đương. Giữ chữ ký API cũ và bàn giao B/E review trước khi ghép. Chỗ đặt lớp và chiều phụ thuộc vẫn theo C-02; nếu hợp đồng khác nhau thì ghi rõ chênh lệch để review/duyệt, không tự đổi chữ ký hoặc giữ hai listener chạy trùng. Ghi chú này là quy tắc cho lần merge sau, chưa xác nhận B/E đã có listener và chưa thay mã nguồn hiện tại.

- Gate nghiệp vụ T039: chủ dự án chọn phương án 1, để PublicCareResultApi.summary chờ thống nhất E/012. Nguồn chỉ liệt kê năm mức ăn, chưa quy đổi averageEatingRatio hoặc chốt mẫu số ăn/nước theo ngày/lần ghi; không tự gán phần trăm hoặc trả rỗng giả. Đã thêm dòng bàn giao E ở mục 8. T033 RED thật 14 failure/0 error (404 thiếu endpoint), sau sửa đường dẫn nạp backend/.env; T035 RED 6 ca có 1 failure/2 error vì thiếu cột; các ca âm ban đầu chưa chứng minh constraint vì lỗi unknown column nên phải kiểm lại sau migration. Contract T039 RED 1 failure, sau bản dịch phát hiện riêng path history còn tên cũ do thay placeholder trước tên path; đã sửa đúng path đã duyệt, đang kiểm lại, chưa ghi xanh.

### Kết quả review Phase6 và quyết định nhận việc tồn

- Review độc lập phát hiện hai P1 (target quyền lịch sử do from/to, mobile đổi account/shift) và bốn P2 (nguồn điều dưỡng bị bỏ, mất projection careNotes/roomBed, refresh web lỗi thời, claim tồn dùng ca gốc). Các sửa đều có test tái hiện RED trước; log kết quả tại Working-history.
- Target kiểm quyền history dùng vị trí hiện tại; khoảng from/to chỉ lọc kết quả. Mobile didUpdateWidget xóa dữ liệu cũ và kiểm generation. Web mọi đường tải dùng generation, mở detail giữ projection đã có nhưng không phục hồi careNotes khi server che content. Thuốc/cảnh báo/bàn giao hiện thành nguồn đọc riêng, không gọi lệnh module khác.
- Chủ dự án chọn chờ E/008 ở Phase7 cho nhận việc có dấu tồn của ca sau. Giữ kiểm ca/giờ gốc hiện có, không coi nhánh claim tồn đã hoàn tất; checklist chỉ đưa allowedCommands qua cùng canClaim/canRecord của lệnh. Đây là gate nghiệp vụ đã chốt chờ, không phải quyết định tự đổi ca nhận việc.
- Chưa nghiệm thu trực quan: Windows computer-use tự dừng do không xác định chắc URL; đã dừng UI và xóa fixture preview tạm. Không báo UI đã kiểm ảnh. Full web/mobile test được chạy lại sau sửa review; clean verify backend cuối vẫn phải hoàn tất trước đánh dấu.

### Kết quả kiểm cuối Phase6 — 01/11/2026

- Scoped sau review:61unit/kiến trúc và67IT xanh,0skip; gồm toàn bộ test Phase6,T008,ModuleBoundaryTest và40ArchTest. Web389/389 test xanh (--maxWorkers=2 sau một timeout StaffPage đã kiểm riêng6/6), lint sạch, build sau sửa cuối qua. Mobile thêm ô performedAt sau RED thiếu trường, scoped12/12 xanh; nhận dd/MM/yyyy HH:mm theo giờ Việt Nam, dùng common formatDateTime kiểm ngày hợp lệ, gửi ISO UTC, để trống giữ mặc định server. Không đổi hợp đồng; full mobile/analyzer lượt cuối đang được xác minh riêng.
- .\mvnw.cmd clean verify chạy nền kết thúc exit1 sau20:54phút:772unit/Arch xanh;1713IT có8failure+10error,0skip;Spotless2387file sạch. Đã đọc toàn bộ XML lỗi và đối chiếu Working-history trước Phase5:18tên phương thức đỏ đều đã được ghi từ Phase4, không có tên lỗi mới trong lượt này. Care206IT có1error ở reason outbox chờA từ Phase3,0failure; các test Phase6 không đỏ.
- Giữ task [] và không tuyên bố Phase6 hoàn tất: payload/offline và mobile transport/shell chờA; callback/phát sinh đo chỉ số chờD007; summary chờE012; quantity tính phí chờE010; nhận việc tồn ca sau chờE008. Giữ các gate Phase3–5/outbox/retention/VOID/nguồnB, ưu tiên listener B/E khi merge. Chưa nghiệm thu trực quan do công cụ Windows dừng kiểm URL. Không commit/push/PR hoặc thay đổi nhánh.

Xác minh mobile sau bổ sung performedAt:64/64 full test xanh;12/12 scoped cuối xanh, analyzer No issues found. Web build sau sửa cuối cũng đã chạy lại và qua. Nội dung đầy đủ và18tên lỗi clean verify được lưu Working-history.md; docs/progress.md đã nối thêm kết quả, checkbox Phase6 giữ nguyên.

## Kế hoạch thực thi Phase 7 — 02/11/2026

Phạm vi T042–T046 trên checkout hiện có, không đổi nhánh/commit/push. T042 viết IT mốc DUE→OVERDUE/nhắc/leo thang/CFG/chạy bù; T043 viết IT projection bàn giao/callback MANDATORY/đóng/chuyển/tồn; rồi T044 bổ sung công việc theo phút trong internal/overdue/{service,persistence,domain} khi có lớp, tái sử dụng CareMinuteWork/ReminderMarkers/StatusHistoryApi. T045 cài hai projection tương thích API hiện có, không đổi chữ ký consumer008. T046 dịch endpoint được chủ dự án duyệt qua trả lời “1”: GET /care-tasks/overdue, floorAreaId, mảng CareTask; dùng quyền OVERDUE_TASK_HANDLING, kiểm từng dòng và field visibility. UI tái sử dụng mẫu care-tasks, không thêm màu/component nền/thư viện. Sau mỗi task kiểm test liên quan và ArchTest; cuối clean verify nền, chỉ đánh[x] khi gate đạt. Ledger thực tế ở Working-history.md.

Giữ gate đã chọn chờ: claim việc tồn ca sau cần E008 chốt ca/giờ; MANDATORY gắn vị trí cần D007 chốt cảnh báo không residentId; ShiftStaffPort production chưa có. Không dùng fake để khẳng định tích hợp nguồn thật. UI mobile chưa có transport mảng do A giữ; chỉ làm widget/gateway test được, không sửa common hoặc gắn fixture vào app.

### Quyết định kỹ thuật và giới hạn bản cài Phase7

- T044 giữ CareTaskDueStep dưới internal/task/service (không tạo bộ quét thứ hai chỉ để chép tên logic QuaHanService); CareTaskClockProcessor tách transaction và khóa; T045 đặt các lớp dưới internal/overdue/service; controller ở web, response ở web/response. Schema carryover đã có V005_99, marker/lịch sử dùng bảng chung qua API, không tạo bảng hoặc migration riêng.
- Dòng thông thường có transaction riêng; chạy bù cần transaction theo nhóm người nhận để chuyển trạng thái, đánh dấu một lần và publication gộp cùng commit. Điều phối tiếp tục nhóm khác khi một nhóm lỗi, sau đó báo lỗi cho SchedulerRuns. Đây là lệch kỹ thuật so với C12 mô tả mỗi dòng, cần giữ rõ: lỗi trong một nhóm có thể rollback nhóm đó, không bảo đảm cô lập từng task trong nhóm. Chưa nghiệm thu trường hợp MANDATORY vị trí do hợp đồng cảnh báo thiếu residentId.
- CareHandoverQueries cài HandoverTaskApi projection đầy đủ005; CareTaskHandoverProjection giữ TaskHandoverApi tương thích008, không đổi chữ ký. Primitive residentId của projection008 không biểu diễn task vị trí: guard503, không gán id0. Cần E thống nhất dùng projection Optional đã công bố/adapter tương thích trước nghiệm thu danh sách đủ.
- CareTaskHandover.onHandoverConfirmed MANDATORY giữ taskIds từ nội dung008 làm điều kiện tự đóng; marker theo handoverId/taskId chống replay, kể cả nextShiftId null; không tự tạo ca/id giả. CareTaskHandling giữ constructor cũ và bổ sung người trước/sau, handoverId để truy vết. Callback vắng/đổi phân công của T038 chưa có bản cài, trả503 rõ ràng; không coi toàn ShiftTaskApi sẵn sàng.
- Review độc lập tìm2P1, đều đã có RED thật ở FR_047_ReviewFindingsIT: tồn qua ca thứ3 và bỏ qua chứng chỉ mới/hết hiệu lực. Bản sửa áp FR047a cho task FROM_PREVIOUS_SHIFT khi ca hiện tại hết, dùng catalog yêu cầu hiện hành và qualified trước giao; không đủ thì chung. Test Phase4 từng chỉ kiểm T024 ở window_to đã được tăng kiểm to-1microsecond DUE/to OVERDUE theo T044, giữ kiểm không sinh/lặp.
- Web chỉ xem danh sách qua endpoint đã duyệt, mở chi tiết đọc và đường sang checklist để xử lý bằng các lệnh sẵn có; không tự thêm endpoint phân lại còn chưa dịch. Mobile widget nhận loader/accountId/canView, xóa dữ liệu khi đổi tài khoản/phạm vi/quyền và bỏ response cũ; transport mảng/đăng ký app chờA, không sửa common/pubspec. Chưa nghiệm thu trực quan: IAB localhost timeout, Windows Computer Use dừng vì không xác định chắc URL. Hai fixture preview tạm đã xóa, không chụp được ảnh màn sản phẩm.

### Quyết định payload gộp lớn — chủ dự án chọn “1” ngày02/11/2026

FR052 yêu cầu một nhắc gộp cho mỗi nhóm người nhận, nhưng outbox nền còn4000ký tự và AGENTS cấm danh sáchid dài. Chủ dự án chọn chờA thống nhất payload an toàn, không duyệt chia nhiều thông báo. MAX_GROUPED_TASKS=32 là giới hạn kỹ thuật tạm để bảo vệ outbox, không phải tham số nghiệp vụ/CFG. Nhóm vượt giới hạn bị chặn503 trước đổi trạng thái/marker/event; các nhóm khác vẫn tiếp tục. Không chia một nhóm thành nhiều publication. Vì vậy chạy bù nhóm lớn chưa đạt FR052 và T042/T044 vẫn[ ]; cần A chốt cơ chế tham chiếu hoặc payload, C retest trước bỏ guard. Test FR_052_OverdueBatchCapacityTest đã chạy RED thật (không ném lỗi khi vẫn chia nhóm), sau đó mới sửa guard.

Clean verify đầu005-p7-clean-verify.log đã dừng chủ động ởcompile sau khi nhận quyết định này để tránh lấy gate trên nguồn còn chia nhóm. Không dùng lượt bị ngắt làm kết quả cuối; sau test guard/Arch xanh chạy cleanverify mới.

### Sửa wiring sau clean verify

Lượt005-p7-clean-verify-final.log kết thúc exit1/02:04phút ởunit:773test,0failure+1error,0skip; chưa chạy IT. T003_MergedApiCompatibilityTest phát hiện @Primary trên FakeCareRequirements (đa interface) xung đột fakeCarePlanApi khi hai config được import cùng nhau. Đây là lỗi mới của bản cài, không baseline. Bỏ thay đổi FakeM04Config (file trở lại nội dungHEAD), đánh @Fallback cho ba providerproduction bàn giao theo mẫu CarePlanQueries/AttendanceQueries đã có. Không sửa assertion của compatibilitytest hoặc tạo fake thứhai. Test production lấy bean theo tên vẫn kiểm nguồn thật; khi importfake cóchủđích, fake thường/Primary hiện có được ưu tiên. Đang kiểm lại trước cleanverify mới.

### Kết quả cuối lượt Phase7 — 02/11/2026

005-p7-clean-verify-final2.log kết thúc exit1/12:05phút:773unit/Arch xanh;1730IT có9failure+36error,0skip;Spotless2402file sạch. 17ITPhase7 và1unitcapacity xanh;care223IT có1error reasonoutbox đãchờA,0failure. Cả18tên lỗi baselinePhase6 còn; thêm27caAssignmentIT (2failure+25error), không gom vào baseline. Chạy riêng FR025_draftDoesNotGrantScopeAndPublishedAssignmentDoes vẫnPermissionDenied trước callbackcare. FixtureE tạo TestUsers.staff rồi mới lùi clock01/11, trongkhi TestUsers gán quyền hiệu lực now−1ngày; suy luận về timefixture cầnE xácnhận, C khôngsửa nguồn/testE hoặc common. JSONsummary vàXMLfullAssignment đãlưu trước diagnostic vì reporttarget sauđó chỉ còn1case.

Webcuối392test/72file,lint/buildexit0;mobile68fulltest/4scoped cuối vàanalyzer sạch. Mànweb hiểnthị cảngày dựkiến quaformatDateTime sauRED thiếu ngày; không thêmfield. UIthựctế chưa kiểm ảnh vìcôngcụkhôngxácđịnhchắcURL. GiữT042–T046[ ]/chưanghiệmthu toànphase: payloadgộpA,nguồn/projection/claimE,cảnhbáovịtrí/metadataD,mobileA,callbackT038vàfullverify cònmở. Chưa làmT047;khôngcommit/push/đổinhánh/PR. Chi tiếttừngfile/luồng/quyếtđịnh/lệnh ởWorking-history.md;docs/progress cậpnhật lượt này,team-plan giữgatecũ vàthêm bàn giao mới.

## Thực thi Phase 8 — 02/11/2026

T047–T049 theo yêu cầu mới; giữ checkout hiện tại và gatePhase7, không đổi nhánh/commit/push. Ba hàm thuần đặt internal/rules/domain; điều phối và projection liên nguồn trong internal/rules/service, tái sử dụng CareResultStore/CareTaskStore ở tầng persistence hiện có thay vì tạo adapter SQL trùng; đăng ký qua CareMinuteWork hiện có, không tạo job hoặc bản ghi nghiệp vụ mới. T047 test trước; T048 MySQL/CorrectionApi.current; T049 nối ghi nhận và CorrectionMade đã công bố, không mở rộng API hoặc thêm thư viện/UI. Sau task chỉ kiểm test liên quan+Arch, cuối clean verify nền trước checkbox.

Chủ dự án chọn“1” cho mâu thuẫnT048/FR040: bỏ kết quả bữa đãVOID, không làm đứt chuỗi bữa. Ngày thiếu mood/behavior vẫn làm đứt chuỗi theoFR042. Sửa mô tả task tương ứng, không sửa spec/nghiệp vụ. Bữa EATING phát sinh thiếu mealId thật: chủ dự án chọn giữ gate chờ D/011 chốt liên kết, không dùng taskId làm mealId; tiếp tục MEAL, nước và tâm trạng. CorrectionMade có sẵn cho hook sau áp dụng; provider đính chính kết quả care chưa có ở checkout, không coi test projection sau đính chính là đã nghiệm thu tạo/duyệt correction thật. Log phiên005-p8-* và Working-history giữ lịch sử.

### Quyết định kỹ thuật Phase 8

- `CurrentCareResults` gọi `CorrectionApi.current` theo lô, bỏ VOID, đọc map hiện hành theo cùng convention của `PublicCareResults`; thứ tự bữa dùng plannedAt/id giảm dần, ngày tâm trạng dùng performedAt. Không giữ bộ đếm cần đồng bộ sau đính chính. Truy vấn metadata task hiện dùng adapter sẵn có theo từng dòng; chưa có nghiệm thu hiệu năng lịch sử dài.
- `CareResultRules` chạy cùng transaction lệnh ghi/áp dụng correction; khóa resident trước đọc projection, marker và event cùng commit. Nước mỗi resident một transaction, đọc CFG lúc đánh giá, kiểm hiện diện tại mốc bằng statusPeriods hoặc AttendanceView cho bán trú. Chạy bù chỉ ngày hiện tại; tập ứng viên gồm cả người đã kết thúc lưu trú sau mốc, quyết định hiện diện vẫn theo lịch sử. Không thêm ScheduledJob hoặc đổi common/runJobs.
- Marker nước theo resident/ngày, kể cả kết quả không thiếu nước; không tiêu thụ marker khi chưa tới mốc, thiếu kế hoạch/hiện diện hoặc projection lỗi. Chuỗi bữa dùng SHA-256 thành phần chuỗi; context milestone dùng cùng digest để downstream không bỏ nhầm sự kiện chuỗi thay đổi. Tâm trạng dùng ngày kết thúc và độ dài trong marker/eventKey.
- Gate mealId chỉ áp dụng chuỗi kém cần phát sự kiện, không chặn lịch sử nằm sau bữa tốt ngắt chuỗi. Không bỏ kết quả phát sinh khỏi cách đếm. Chuỗi vượt 32 bữa giữ 503 trước marker/event để không đưa danh sách dài vào outbox VARCHAR(4000); đây là giới hạn kỹ thuật chưa nghiệm thu, cần A/D thống nhất payload, không phải ngưỡng nghiệp vụ mới. Không rút gọn mealIds hoặc tách thông báo tùy ý.
- Loại cảnh báo dùng ba mã String ở glossary 11.11 trên payload sẵn có; người nhận AssignedNurse, mức LOW. Chưa có nghiệm thu consumer 007/011 thật; không mở rộng hợp đồng API trong lượt này.

### Xác minh cuối Phase 8 — 02/11/2026

Scoped cuối005-p8-scoped-final.log: exit0/1:36phút,44unit/ArchTest và14IT xanh,0skip;Spotless2413file sạch. Review lại chỉ đọc không còn P1/P2 cụ thể ngoài gate. Đã sửa lỗi gate lịch sử, eventKey, tập ứng viên chạy bù và validation projection, với bằng chứng RED/fixture sửa đúng nêu trong Working-history.

`./mvnw.cmd clean verify` chạy nền kết thúc exit1/11:48phút:777unit/kiến trúc xanh;1744IT có9failure+38error/0skip;Spotless2413file sạch.18test riêng Phase8 (4unit+14IT) xanh;care237IT có0failure/1error outbox reason cũ. JSON005-p8-final-verification-summary.json đối chiếu summaryPhase7:45tên lỗi cũ còn nguyên, thêm đúng2ParameterChangeIT (restoreDefault/staleIfMatch), DuplicateKeyException parameter_history.uq_parameter_history với CFG-M05-01/effective_from trùng microsecond. Chưa kết luận nguyên nhân; ghi dòng chờA, không sửa test/nguồnA hoặc chạy diagnostic ngoài scoped. Không có tên lỗi mới của care.

GiữT047–T049[ ] và chưa nghiệm thu toànPhase8 do gate fullverify, mealId phát sinh D/011, payload chuỗi dài A/D, consumer thật. Test projection/hook không thay bằng chứng tạo/duyệt correction: provider làT051 củaPhase9. Thứ tự tiếp: chốt gate và kiểm lạiT047→T048→T049; sau đó T050test đính chính rồiT051provider/endpoint theo hợp đồng đã duyệt (chưa bắt đầu). Không đổi API, thư viện, UI, commit/push/PR hoặc nhánh. docs/progress thêm kết quả; team-plan thêm4dòng chờ/review, giữ gate cũ; Working-history bổ sung file/luồng/lệnh/kết quả thực tế.

## Thực thi Phase 9 — 02/11/2026

Tiếp T050→T051 theo yêu cầu mới, giữ gate Phase8, checkout feature/005-daily-care-and-hand-over tại HEADbd646d1e; đầu phiên chỉ Working-history.md và hand-over-task-003.md untracked. Đã báo35dòng có HungNX-nguyen tại mục8. Chủ dự án duyệt POST /care-results/{id}/correct, body type/correctedContent/reason, AMEND_CONTENT/VOID, Idempotency-Key và201khôngbody. Không tự đổi API khác, thêm thư viện/UI hoặc git.

Quyết định: provider M04.CARE_RESULT tại internal/result/service, DTO HTTP ở web/request; dùng CorrectionApi thật, task/result persistence hiện có, không thêm bảng/migration. T050 kiểm HTTP và provider/MySQL trước cài, gồm nguyên bản bất biến/cáchA-B/quyền tầng/đo chỉ số/VOID/chuỗiF. Structural validation trước create; kiểm theo snapshot resultType trong callback có id, lỗi rollback transaction, không giữ id bằng ThreadLocal. Entry service READ_COMMITTED để callback quy tắc đọc projection mới; khóa resident trước khóa task/chuỗi khi ghi, chỉ đọc metadata khi chain() readOnly. Không inject CorrectionApi hoặc CareResultRules vào provider để tránh vòng constructor của CorrectionService; hook CorrectionMade Phase8 đã nối quy tắcF, không gọi trùng lần thứhai.

Gate quantity tính phí E/010 từPhase6 giữ nguyên: không suy numericValue thành newQuantity, chưa phát BillableCareResultCorrected trên dữ liệu chưa thống nhất; guard trước lưu, T050/T051 phần này mở. Chủ dự án chọn giữ gate VOID kết quả ngủ chờ D/007 vì SleepResultRecorded chưa có cách biểu diễn hủy; không dùng nhóm rỗng/null làm tín hiệu tự đặt. AMEND_CONTENT ngủ và các loại khác tiếp tục. Không coi projection spy Phase8 là lifecycle thật; lượt này dùng CorrectionApi/chain/current thật cho nhánh được hỗ trợ.

### Quyết định kỹ thuật và giới hạn Phase 9

- Dùng service nhỏ làm transaction READ_COMMITTED quanh CorrectionApi.create vì phần nền000 có isolation mặc định khác; không đổi service hoặc cấu hình của A. Guard isolation ở allows/callback, trước khóa/ghi; metadata/isConfirmed/originalContent/chain không bị coi là lệnh tạo. Khóa resident→task→correction_chain để đồng bộ F với lệnh ghi nhận đang có. Cách C bị từ chối cả entry lẫn callback; không để đường apply chung bỏ qua thu hẹp của module.
- Đã có RED khi genericHTTP trả201 dù chưa bảo đảm isolation, và RED riêng khi guard đặt ởisConfirmed làm chain đọc trongouterRR bị chặn. Chuyển guard sangallows, kiểm hai lệnhcare song song qua service thật và markerDB. Chặn đườngghi generic000 tạm503 là giới hạn kỹ thuật của type mới, không tuyên bố đườngnày đã hỗ trợ; chờ A thống nhất isolation/cách tôn trọngallows trước tạoapproval. Đọc chain vẫn phải qua kiểm quyền VIEW hiện có khi trả nội dung kết quả.
- SPI validateContent không có id: kiểm shape/tên trường/kiểu trước lưu, kiểm đúng loại/giới hạn theo snapshot trong afterCorrection; sai thì rollback correction_chain/correction/audit cùng transaction. Kiểm atomic bằng test không có outertransaction, không chỉ nhìn dữ liệu chưa rollback của test@Transactional. correctedContent là giá trị thay thế đầy đủ, không phải patch hoặc cách đổi metadata.
- Không inject CorrectionApi/rules vào provider nên không có vòngconstructor. SleepAMEND có sự kiện riêng theo hợp đồng đang có; F dùng CorrectionMade đã cài. Không phát BillableCareResultCorrected với số lượng suy đoán hoặc tự mở rộng SleepResultRecorded choVOID.
- Client005 được sinh bằng npm run gen:api; diff nội dung chỉapi005.js thêmpostCareResultsByIdCorrect. Không sửa tay generated/generator hoặc thêmUI/menu. Web392test/72file,lint/build đãexit0; build có cảnh báo chunk lớn từVite, không sửa cấu hình ngoài scope để dập cảnh báo. Không chạy mobile vì không đổi mobile; không chụp màn.

Bổ sung bảo vệ đường đọc khi đăng ký provider: hai test GETchain generic đã RED thật (ngoài phạm vi tầng và HEALTH chỉ PRESENCE_ONLY đều trả200). originalContent hiện yêu cầu FieldVisibilityApi.HEALTH=FULL rồi gọi CareResultQueries.original với Target M04.CARE_RESULT để dùng kiểm TASK_CHECKLIST/VIEW sẵn có; không trả raworiginal/current/corrections trước khi qua kiểm. Không đổi ma trận quyền hoặc response API. Review lại xác nhận không có vòng constructor hoặc P1/P2 cụ thể còn lại; kết quả scoped/cuối phase ghi bên dưới khi lệnh hoàn tất.

### Scoped cuối Phase 9

005-p9-scoped-final4.log BUILD SUCCESS/exit0/2:19phút:44unit/Arch/ModuleBoundary +19IT xanh,0skip;Spotless2420file sạch. Các lượt RED/fix và giới hạn bằng chứng ghi đầy đủ ở Working-history.md. 3unit riêngPhase9 +19IT là22test mới. Web392test/72file,lint/buildexit0; không đổiUI/mobile. Đã chạy clean verify nền cuối sau scoped này, đang chờ kết quả tổng trước checkpoint/checkbox.

### Bổ sung sau clean verify đầu và tiếp tục 03/11/2026

Full đầu exit1/13:11phút:780unit xanh,1763IT9failure+38error.2lỗi mới T008_CareGovernanceIT do fixturePhase9NOT_SUPPORTED để lại task commit, FakeResidents kháccontext không có id; sửa cleanup đúngresidentfixture ởtest mới, không sửa governance/test cũ hoặc nuốt lỗi production.2ParameterChangeIT củaPhase8 không tái hiện trong lượt này, chưa kết luận đã giải quyết. Log/summary đầu được giữ.

T051 cần nối những trường đọc đã tồn tại: CareResultResponse trước đó luônfalseflags vàgiá trịgốc. TestAPI mới RED2000vs200 trước cài; history nay bulkCorrectionApi.current, detail đọccurrent1id, factory ánh xạcorrected/recordingCancelled vàcurrentValue, giữhealthmask/metadata. VOIDfallback bản gốc kèmflag như000chain, không cộng vàoquy tắcF. Không đổi APIcontract/field/UI vàkhông thay queries.original dùng cho chuỗi gốc. Đườnglogic tasks chỉ tên chức năng nên không tạo thêm persistence/projection trùng.

Scoped-final5exit0/2:13phút:44unit/Arch/Boundary +26IT (20Phase9+6governance) xanh,0skip;Spotless2420 sạch. Reviewcuối không có P1/P2 cụ thể ngoài gate. Chạy clean verify nền lần2 ngày03/11 sau scoped này; chưa đánh[x] trước kết quả cuối. Web392test/lint/build đã qua, sourcefront không đổi sau đó nên không lặp kiểm frontend.

Clean verify lần2 exit1/15:37phút:780unit xanh,1764IT9failure+40error;23testPhase9 xanh,6governance xanh.4lỗi mới là fixture nướcPhase8 chạy trước15:00 nhưng recordedAt lấygiờthực sớm hơnperformedAt môphỏng, vi phạmck_care_result_time. Sửa helper save của FR_039_CareResultRulesIT lấy recordedAt không trướcperformedAt; không sửa constraint/production/assertion. Scoped-final6exit0/2:03phút:44unit/Arch/Boundary +40IT liênquan(14Phase8,20Phase9,6governance) xanh/0skip,Spotless2420sạch. Đã chạy clean verify nền lần3 sau sửa fixture; kết quả cuối ghi sau khi hoàn tất.

Full lần3exit1/14:07phút:780unit xanh,1764IT9failure+36error,care257IT1error outbox cũ;không có tên lỗi mới soPhase8,23test riêngPhase9 xanh. Thêm test bảo toàn publication khác loại cùng sốID: RED2→0 trước khi siết cleanup theo context.sourceType/recordType +ID, tránh coi ID số là định danh toàn cục. Chỉ sửa testfixture, productionkhôngđổi. Scoped-final7exit0/2:13phút:44unit/Arch/Boundary +41IT (21Phase9,14Phase8,6governance) xanh,0skip;Spotless2420. Chạy clean verify nền lần4 sau sửa cuối, chưa đánh[x] trước kết quả.

### Kết quả cuối Phase 9 — 03/11/2026

005-p9-clean-verify-final4.log kết thúc exit1/14:24phút:780unit/kiến trúc xanh,1765IT có9failure+36error/0skip;Spotless2420file sạch. Care258IT có0failure/1error outbox reason cũ;24test riêngPhase9 (3unit+21IT) xanh. 14ITPhase8 và6governance xanh; các hồi quy fixturecommit/timestamp đã hết. 005-p9-final-verification-summary.json đối chiếuPhase8:không có tên lỗi mới,45tên cũ còn;2ParameterChangeIT không tái hiện, chưa coi là đã xử lý khi không có sửa nguồnA. Không chạy lại test A/E riêng lẻ hoặc sửa assertion/production ngoài care để làm xanh.

GiữT050/T051[ ] vì nhánh billable event/newQuantity chưa nghiệm thu (E/010), VOID ngủ chờ tín hiệuD/007 theo quyết định chủ dự án, genericghi chờA thống nhất isolation; consumer thật và gateF trước còn mở. NhánhA/B qua endpointcare, bản gốc/trạng thái, projectionhistory/detail/family, quyềnxem, callbackF/ngủAMEND, rollback và concurrency đã có bằng chứng. Không dùng testguard thay nghiệm thu luồng bị chặn.

Thứ tự tiếp: hoàn tất phần còn lại T050 (test payload billable khi quantity đã thống nhất) → T051 (producer/gates và retest tích hợp); sau đó T052 test bốn nguồn báo xem xét kế hoạch → T053 listener củaPhase10. Chưa bắt đầuPhase10. Ghi chú bàn giao PR: Phase9/T050–T051 một phần; đổi1HTTP operation đã duyệt, không đổi JavaAPI/SPI/event; kết quả/gates như trên. Chưa tạoPR/commit/push/đổi nhánh; giữ feature/005-daily-care-and-hand-over,HEADbd646d1e. docs/progress thêm kết quả cuối, team-plan thêm3dòng chờPhase9 và chỉ cập nhật trạng thái phần còn dở; Working-history giữ đầy đủ lịch sử/file/luồng/lệnh/kết quả.
