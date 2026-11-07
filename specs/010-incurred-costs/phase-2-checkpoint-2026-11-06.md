# 010 — Checkpoint nền và chuyển tiếp Phase 3, 06/11/2026

**Phase 2 chưa nghiệm thu; toàn feature 010 chưa hoàn tất.** Nhánh `impl/010-foundation-next`, HEAD `09d569c0`, 11/42 task trước khi làm Phase 3. Không đánh T008 xong.

- Đã sửa phân rã kỹ thuật: T008 là provider dữ liệu hiện có; metadata/phạm vi/retention mua hộ đi cùng T026, quyền tệp cùng T027. Chuỗi hồ sơ thật 001 → 010/bảng chung và dọn chỉ mục theo nguồn giữ là gate T033 trước hoàn tất feature/merge/vận hành. Không bỏ NFR hoặc tạo provider rỗng.
- Bộ toàn kho bị gián đoạn, không có Maven exit thành công: 1099 unit/arch xanh, 1411 IT ghi nhận 1 error. Billing đã có 157 ca/17 lớp xanh. Kết quả dở không thay bằng kết quả full verify đã qua.
- Lỗi đang bàn giao B: `FR_046_DuplicateAttributeIT.FR_046_cancellingTheFirstRequest_releasesTheAttribute_andClosesItsAddendum`, 004 T043/T045; phụ lục vẫn `PENDING_EFFECTIVE` sau 10 giây thay vì `NOT_APPLIED`. Xem [bản ghi cụ thể](contracts/004-cancellation-observation-2026-11-06.md). Chưa sửa test hoặc code 004, chưa kết luận nguyên nhân.
- Đã tìm và sửa lỗi môi trường khởi chạy: DEBUG không phải boolean kế thừa từ công cụ làm Spring Boot 4.1.1 bật log SQL. Chỉ đặt DEBUG=false/TRACE=false và -Ddebug=false/-Dtrace=false cho tiến trình test; pool test 20/cache 2 và đường dẫn socket ngắn qua JAVA_TOOL_OPTIONS. Các lượt trước dừng/error được giữ riêng, không gọi là xanh hoặc lỗi baseline develop. Không sửa cấu hình hệ thống/kho mã, không bỏ test, giữ mật khẩu tài khoản demo khi chạy.
- Người dùng đã chốt: “Giữ E, bàn giao lỗi cho B; cho làm Phase 3 độc lập trong khi giữ nghiệm thu Phase 2 mở.” Vì vậy tiếp tục T009 → T012 trong phạm vi E; gate toàn kho/cuối feature/PR vẫn mở.
- Origin/develop được đối chiếu ở `2fd4cd8c`, nhánh 010 thiếu 12 commit, có PR #109/#110; chưa đồng bộ theo yêu cầu trước. API retention vẫn chỉ có `referencesTo(long)`; source care chỉ khai báo hai event, chưa có producer. Chờ A cài đề xuất mở rộng retention, không ghi A chưa xong task gốc 000.
- Khi được phép đồng bộ: sau khi commit phần đang làm, `git fetch origin` rồi đưa `origin/develop` vào chính nhánh feature. Không tự merge, đổi nhánh, commit, push hay mở PR.

Raw log/XML được giữ riêng trong snapshot cục bộ, con trỏ `.worktrees/.snapshots/010-phase2-final-verify-latest-path.txt`; `interrupted-observation.json` ghi kết quả dở và trạng thái gián đoạn. File báo cáo 05/11 người dùng đang sửa được giữ nguyên, không tự stage.
