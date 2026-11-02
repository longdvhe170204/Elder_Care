# T003/T004 — khai báo API và port chi phí

Nhánh impl/010-incurred-costs, nền e7b2b71. Người dùng đã tự chuyển terminal vào worktree 010; không tạo/chuyển nhánh, commit, push hoặc PR trong lượt này. Bản dịch HTTP đã được A/chủ dự án duyệt trước vẫn giữ nguyên.

> **Cập nhật R-15 ngày 2026-10-30:** nền hiện tại 8573190, thêm ba bảng riêng T005a và fixture nguồn. Xem [r15-storage-verification.md](r15-storage-verification.md). Chữ ký HTTP/Java khai báo ở lượt này không đổi; các số 71/711 là bằng chứng trước R-15.

## 1. Phần đã làm

- T003: ba interface FamilyPortalCostApi, FinalPeriodCostApi, CostStatisticsApi cùng DTO; tám sự kiện CostSourceBilled, CostSourceChanged, CostStatementFinalized, PurchaseAssistanceSent/Decided/Cancelled/Purchased, CostItemChanged. Các gói api/api.dto/api.events có NamedInterface.
- T004: CostForBalancePort tại provision/internal/domain và PurchaseBalancePort tại purchase/internal/domain. Chỉ khai báo hai phương thức đọc của port chi phí và một phương thức kiểm dấu của port số dư; bangDaChot thuộc T036/T037.
- Fake chung đúng chủ E: FakeFamilyPortalCosts, FakeFinalPeriodCosts, FakeCostStatistics, FakeCostForBalance, FakePurchaseBalance; cấu hình duy nhất FakeM11Config ở src/test/.../support/fake/m11. Không có fake/bean thay thế trong ứng dụng thật.
- Ghi chữ ký thực tế ở [contracts/module-interfaces.md mục 4](contracts/module-interfaces.md); cập nhật tên lớp/kiểu/hàm và đường dẫn vật lý tại glossary mục 16.6. Đây là khai báo đầu của M11, không sửa API hoặc fake của A/B/C/D.

## 2. TDD và kết quả kiểm

- Viết CostApiContractsTest trước, chạy api-unit-red.log: thất bại testCompile vì API/port chưa có. Thêm CostSharedFakesTest/CostViewSchemaTest trước mã tương ứng, api-fakes-red.log tiếp tục thất bại testCompile vì kiểu/fake chưa có. Không nói các assertion đã chạy đỏ ở lượt chưa biên dịch được.
- Sau cài: api-unit-green.log có 71 test M11 xanh, 0 thất bại/lỗi/bỏ qua. Gồm 49 ca cũ và 22 ca mới: 13 ca hợp đồng/đọc lại outbox, 5 ca fake/config, 4 ca đối chiếu trường DTO với YAML đã duyệt.
- Kiểm giữ định danh/số lượng thập phân/serviceId tùy chọn và khóa sự kiện nguồn; hủy nguồn khác số lượng 0; năm loại sự kiện có thông báo đọc lại đúng RecipientGroup và InformationSubject.ABOUT_FAMILY_MATTER, không Q-354; mẫu payload trong giới hạn outbox 4000 ký tự.
- Kiểm bốn view khớp tập trường schema thực tế; port giữ long/Optional<Long> và Instant; fake phân biệt giá 0 với thiếu giá, giữ dấu số tiền và mốc truy vấn, tách fixture theo ScopeFilter và khoảng ngày gồm hai đầu, cấu hình test cung cấp đúng năm fake.
- Spotless apply giới hạn Java M11 đã qua. **Clean verify toàn backend:** 15:49 min: 711 unit/arch xanh (M11 71), 1426 IT còn 7 thất bại/9 lỗi, 0 bỏ qua, đúng 9 lớp M01/M03 đã ghi nhận ở team-plan/lượt trước, không có lớp đỏ mới. Spotless check qua. Maven trả BUILD FAILURE do lỗi nền; chưa coi toàn backend xanh hoặc Phase 1/2 hoàn tất. Đã nạp .env/backend/.env và giữ mật khẩu demo; api-clean-verify.log/api-full-summary.json giữ bằng chứng.

Log/script/report ignored ở backend/target/010-foundation-verification của checkout gốc: api-unit-red.log, api-fakes-red.log, api-unit-green.log, api-spotless.log, api-clean-verify.log. Không đưa biến môi trường/mật khẩu vào tài liệu hoặc kho.

## 3. Giới hạn và phần còn lại

- T003/T004 xong phần khai báo và có test xanh ở nhánh E; chưa commit/push/merge nên người khác chưa nhận được trên develop. T001/T003/T004 là ba task đã [x]; T002/T005/T006/T007/T008 vẫn [ ], Phase 1/2 chưa hoàn tất.
- Chưa có bản cài thật của ba API và hai port. Chưa có producer/listener cho tám sự kiện mới: D/011, E/012, C/014 phát hai sự kiện nguồn khi triển khai; E/010 phát sự kiện bảng/khoản/mua hộ tại các task nghiệp vụ sau. Không nghiệm thu nghiệp vụ chỉ bằng fake.
- Test outbox dùng yêu cầu thông báo mẫu để kiểm bảo toàn metadata. Chọn nhóm người nhận, mức, nội dung, kiểm quyền, không lộ tên thuốc và tạo yêu cầu thật theo FR-044/P-13 vẫn thuộc dịch vụ nguồn; lượt này không khẳng định đã kiểm producer chưa tồn tại.
- Các trường object trong hợp đồng chưa có cấu trúc được giữ Map/List<Map> ở kiểu đọc, không tự điền các schema HTTP còn thiếu. Hợp đồng REST không đổi; 17 phản hồi thành công thiếu schema vẫn cần cụ thể hóa trước endpoint tương ứng.
- Quy tắc không có bean PurchaseBalancePort thì không đặt dấu được ghi trong hợp đồng; consumer và test hành vi thật thuộc T027, chưa có caller ở lượt này. Không dùng fake production để vượt chỗ chờ.
- T002 còn fixture nguồn và NguoiDaiDienPort do B/004 khai báo; T006 còn BillingPeriod/nguồn ngày giải phóng giường; các nguồn B/C/D và client A còn như dòng chờ team-plan. T005 migration, T007 khóa/job, T008 provider là việc E còn làm được theo nền/phase.
- Các worktree 008, source/spec của chủ khác và tài liệu nghiệp vụ giữ nguyên; không có thay đổi web/mobile, không chạy npm test/lint/build/flutter trong lượt này. CRLF/UTF-8 và progress chỉ thêm được kiểm cuối.

## 4. File để review

- backend/src/main/java/com/cambae/backend/m11/api/ (interface, DTO, events và NamedInterface).
- backend/src/main/java/com/cambae/backend/m11/provision/internal/domain/CostForBalancePort.java.
- backend/src/main/java/com/cambae/backend/m11/purchase/internal/domain/PurchaseBalancePort.java.
- backend/src/test/java/com/cambae/backend/m11/CostApiContractsTest.java, CostViewSchemaTest.java, CostSharedFakesTest.java.
- backend/src/test/java/com/cambae/backend/support/fake/m11/ (năm fake và FakeM11Config).
- tasks/plan/contracts/module-interfaces, glossary mục 16.6, team-plan mục 8 và dòng tiến độ mới.
