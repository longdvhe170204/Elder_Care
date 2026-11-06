# Checkpoint Phase 5 — feature 010, 07/11/2026

Đã cài và kiểm T015–T019 trên nhánh impl/010-phase5, từ develop d9a62fc5. Chọn 010 vì Phase 5 của 008 đã có. Người dùng giao tự chọn phương án sát spec và cấm chạy verify. Chưa commit/push/merge; chưa nghiệm thu toàn feature.

## Kết quả

- Kiểm tra/bỏ kiểm tra, duyệt/trả/hủy khoản; kiểm tra và duyệt theo danh sách hoặc bộ lọc. Lô trả riêng khoản bỏ qua và lý do, không chọn sẵn khoản. Kiểm quyền phía máy chủ và tách người kiểm tra/người duyệt; duyệt riêng khoản vượt yêu cầu lý do.
- Gửi chốt, trả bảng, chốt nguyên tử. Khóa người → kỳ → bảng → khoản theo id; locking read sau chờ khóa tránh snapshot cũ. Chốt lưu tổng JSON và khóa khoản, cập nhật kỳ khi đủ, ghi audit/phát sự kiện/thông báo cùng transaction. Không ghi chứng từ/số dư 017.
- V010_04 mới tăng version và mở lại bảng khi có khoản Nháp mới; không sửa migration đã merge. Luồng tạo khoản đầy đủ vẫn thuộc T011/T012.
- Cảnh báo so hai bảng thường đủ tháng, ngưỡng CFG và ba loại tăng nhiều nhất; mẫu số 0 vẫn cảnh báo, không trả tỷ lệ không hữu hạn. Phần nguồn lưu trú/bán trú dùng API đã công bố, nghiệm thu toàn chuỗi còn T033.
- CostDailyTask dùng khung T007, tạo kỳ, chuyển trạng thái/hạn CFG-M11-01, gỡ dấu lập trước ở khoản chưa khóa, nhắc thiếu bảng/tới hạn. Không lập bảng/khoản tự động. Audit actor Bộ lập lịch có Basis và marker commit thật đã được kiểm.
- 12 operation HTTP khoản/bảng/kỳ; lịch sử lệnh từ StatusHistoryApi. Schema kỳ theo DTO đã công bố; commandHistory và cách bỏ increaseRatio tùy chọn ghi trong contracts/phase5-contract-handoff-2026-11-07.md. Không đổi nghiệp vụ/spec; A/consumer còn review phần hợp đồng, đăng ký test và client trong PR.
- Web tại frontend/src/features/charges, nối App mục charges, chỉ dùng client sinh. Có chọn kỳ, lọc khoản, xem lịch sử, kiểm/duyệt, gửi/trả/chốt có xác nhận, kết quả bỏ qua, trạng thái chờ/rỗng/lỗi. If-Match lấy từ GET; 412 giữ nội dung và khóa xác nhận tới khi tải lại; 403 xóa dữ liệu màn. Khóa gửi trùng và dùng Idempotency-Key.

## Kiểm tra

- TDD đỏ trước cài: 010-phase5-red.log, workflow-red, daily-red; scheduler-basis-red tái hiện thiếu căn cứ audit; zero-base/period-history/web-red tái hiện thiếu cảnh báo 0, endpoint/lịch sử và màn. Không bỏ test hoặc đổi assertion để né lỗi.
- Lượt cuối 010-phase5-delivery.log: **66 unit/arch ở 26 lớp + 110 IT ở 13 lớp, 0 failure/error/skip**, đã cộng từ XML mới. Gồm toàn bộ *ArchTest/ModuleBoundaryTest, các unit mới, mọi billing IT, RecordGroupTriggerIT, ApiContractIT và SchedulingSwitchIT. CostWorkflowIT 20 và CostJobCatchUpIT 9 xanh. Đây là nhóm chọn, không phải chạy toàn backend.
- Lượt trước mở rộng đã kiểm 175 unit/arch và 108 IT theo kết quả mới nhất từng lớp; không cộng chồng với lượt cuối. Các test đỏ Phase 3 có sẵn không được sửa hoặc tuyên bố đã xanh.
- Web: **103 tệp / 610 test xanh**, trong đó 9 test màn chi phí; npm run lint và npm run build qua. Build còn cảnh báo kích thước chunk dùng chung. Đã xem thử trên Edge headless ở bố cục rộng/sáng và hẹp/tối; dữ liệu thử chỉ trong tệp tạm, không vào sản phẩm.
- Spotless đã áp cho đúng file Java phase này, không định dạng mã Phase 3 bên cạnh. Log/ảnh tại backend/target/010-phase5-*. Không chạy Maven lifecycle/goal verify; chỉ dùng test, test-compile, failsafe:integration-test, spotless:apply. Đã đọc XML vì integration-test có thể trả BUILD SUCCESS dù IT lỗi.

## Phạm vi còn mở

T008/Phase 2, các task Phase 3/4, luồng tạo nguồn T011/T012, tính lại T014/T021 và kỳ cuối T024 chưa được nghiệm thu bởi phase này. Kiểm nguồn thật, chuỗi lưu giữ liên module và dọn chỉ mục vẫn là gate T033 trước hoàn tất feature/merge/vận hành. T032 chỉ kéo phần đăng ký hợp đồng lên; 17 operation story khác vẫn pending đúng thực tế. Job nhắc bổ sung/nguồn đổi làm cùng task sau. Commit cần người dùng đồng ý; chưa mở PR hoặc chạy verify.