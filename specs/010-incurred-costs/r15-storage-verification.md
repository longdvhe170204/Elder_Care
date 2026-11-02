# T005a và fixture nguồn — R-15, 2026-10-30

Nhánh impl/010-incurred-costs, nền origin/develop 8573190. Chưa hoàn tất toàn bộ 010 hoặc Phase 1/2; phần độc lập đã có test xanh, các phần dùng API chung phải chờ đúng chủ.

## 1. Đồng bộ và giữ công việc đang dở

- Khi bắt đầu T005, phát hiện develop có PR #54 (4692b98/8573190): constitution 3.2.0, R-15 gộp bảng còn 79. Đã fetch rồi pull --ff-only trên chính nhánh 010, không đổi nhánh, tạo commit, stash, push hoặc PR.
- Sao lưu SHA-256 đủ 59 tệp trước pull; 52 tệp không chồng lấn giữ nguyên byte, bảy tài liệu được ghép. Ba chỗ trùng: progress nối nguyên các dòng hai phía, plan giữ phần E và thêm phần R-15 của A, tasks giữ trạng thái/test E và nối các ghi chú R-15. Không mất Java/API/glossary hoặc công việc 008 ở worktree khác.
- Snapshot: .worktrees/.snapshots/010-before-r15-20261030-1793294325047 tại checkout gốc, có manifest, bản từng tệp, base/ours/theirs/ready, patch và pull.log.

## 2. Phần đã kiểm xanh

- Đã viết data-model mục 0.1 trước migration, nêu ba bảng riêng, loại/nội dung M11 ở bảng chung, cách giữ BR/DBR và test. T038 đã có sẵn trong tasks; đã gộp ghi chú vào dòng gốc và bỏ dòng trùng thêm nhầm. Tách T005a độc lập; T005/T038 vẫn chờ phần bảng chung.
- Migration V010_01__costs.sql: cost_statement chứa PERIOD/STATEMENT, cost_item, cost_source_index. Tổng theo loại thành JSON; không tạo lại cost_period, resident_cost_guard, lịch sử tính lại, bảng xử lý sự kiện, đề nghị mua hộ, bảng xuất hoặc bảng lần chạy.
- CHECK/UNIQUE giữ tháng dương lịch, một bảng mỗi loại/người/kỳ, nguồn của khoản và khóa đơn vị; tám dấu theo Q-395; cột tiền DECIMAL(15,0); JSON tổng theo schema. Khóa theo locked_at, bảo vệ cha cũ/mới, chỉ cho tham chiếu xuất đầu đặt một lần, cổng tính lại trên đúng connection; admin không sửa được dòng khóa, retention xóa đúng quyền.
- FakeCostSources là fixture dùng ApplicationEventPublisher, đăng ký trong FakeM11Config do E giữ; ba ca phát hai loại sự kiện cho FAMILY_MEAL/OVERNIGHT_STAY/ACTIVITY_ATTENDANCE xanh. Đây là test fixture, không phải producer thật D/011, E/012, C/014.

## 3. TDD và lệnh kiểm

- Fixture viết trước: r15-fixture-red.log lỗi testCompile vì thiếu FakeCostSources; sau đó mới tạo lớp/bean.
- Schema viết trước SQL: r15-schema-red.log chạy thật 15 ca MySQL, 1 thất bại/14 lỗi vì ba bảng chưa tồn tại. Đây là lỗi đã chạy test, không chỉ lỗi biên dịch.
- Lượt thêm SQL đầu có 14/15 ca qua; một assertion đọc thông báo Spring bọc ngoài nên không thấy lỗi quyền DELETE gốc. Đã sửa helper kiểm SQLException gốc và giữ nguyên loại/lý do lỗi cần chặn; không nới assertion hoặc sửa quyền để qua test.
- r15-targeted-green.log: 74 unit/arch M11 + 15 CostSchemaIT xanh, 0 thất bại/lỗi/bỏ qua. Spotless apply M11 và check qua.
- **Clean verify toàn backend:** 05:59 min: 714 unit/arch xanh (M11 74), 1441 IT còn 7 thất bại/9 lỗi tại đúng 9 lớp nền M01/M03, 0 bỏ qua/không lớp đỏ mới; 15 IT M11 xanh. Spotless check 2051 file sạch. Maven BUILD FAILURE do lỗi nền, chưa nghiệm thu toàn backend/Phase 1–2. Bằng chứng r15-clean-verify.log/r15-full-summary.json. Runner nạp .env/backend/.env từ checkout gốc và giữ DB_PASSWORD/DB_RETENTION_PASSWORD cho demo.

Log và script ignored tại backend/target/010-foundation-verification của checkout gốc. Không lưu mật khẩu vào tài liệu hoặc mã.

## 4. Phải chờ ai và phần chưa xong

- **A/000 T108:** mới có tài liệu trên 8573190, chưa có V000_09/administration.shared hoặc ReminderMarkers/TechnicalLocks. Chờ ResidentModuleStateApi, StatusHistoryApi, RecordEventApi, SimpleRecordApi, các helper common và fake đúng chủ để nối trạng thái, lịch sử, marker, khóa, hồ sơ nhỏ. Không tự tạo API/fake của A hoặc đọc/ghi trực tiếp bảng chung.
- **B/004:** NguoiDaiDienPort còn chưa khai báo/fake; T002 giữ [ ] chỉ cho phần này sau khi fixture nguồn E đã xanh. BillingPeriod của T006 vẫn chờ kiểu hợp đồng/hồ sơ kết thúc/ngày giải phóng giường.
- **C/005, D/006:** sự kiện/producer tính phí/có mặt/điều chỉnh/PRN/giao thuốc còn như team-plan. Fixture mới không thay nghiệm thu nguồn thật.
- **A nền web:** vẫn cần đăng ký 010 trong TRANSLATED mới sinh được client; bản dịch HTTP đã duyệt giữ nguyên, không xin duyệt lại chỉ vì đổi cách lưu.
- T005a [x] cho ba bảng riêng; T005/T038 chưa đóng do phần bảng chung. T007 cần khóa/trạng thái qua API A. Provider T008 và các user story chưa được nghiệm thu; backend còn phải kiểm quyền, nhật ký, nguồn thật, tính giá/tổng điều chỉnh không âm, đồng thời và phát sự kiện. 17 response HTTP còn thiếu schema vẫn cần cụ thể hóa trước endpoint tương ứng.
- Chưa có API nghiệp vụ/service M11 thật hoặc màn web/mobile; không chạy npm test/lint/build/flutter vì không sửa mã web/mobile. Các task bị bỏ bởi Q-396/Q-397 không được làm lại.

## 5. Địa chỉ để đọc/review

- backend/src/main/resources/db/migration/V010_01__costs.sql (ba bảng, sáu trigger và GRANT).
- backend/src/test/java/com/eldercare/backend/billing/CostSchemaIT.java (15 ca MySQL).
- backend/src/test/java/com/eldercare/backend/billing/CostSourceFixtureTest.java; support/fake/billing/FakeCostSources.java; FakeM11Config.java.
- specs/010-incurred-costs/data-model.md mục 0.1; tasks T005a/T005/T002/T038; plan phần T005a và R-15; glossary chỉ mục 16.7–16.8; contracts/module-interfaces mục 0.
- docs/team-plan.md mục 8 có dòng A/000 T108 chờ riêng cho 010; progress chỉ thêm dòng mới sau khi kiểm xong.

Phạm vi: M11, test/fake M11, V010, tài liệu 010, glossary mục 16 và dòng tiến độ/phụ thuộc. Không sửa common/administration.shared hoặc spec/nghiệp vụ, không tạo/chuyển nhánh/commit/push/PR; giữ CRLF/UTF-8.
