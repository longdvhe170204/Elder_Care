# Quan sát nghiệm thu 010: test hủy phụ lục 004 — 06/11/2026

**Trạng thái:** bộ clean verify toàn kho còn đang chạy; chưa kết luận qua. Đây là ghi nhận lỗi và ranh giới sở hữu, không phải thay đổi hợp đồng hoặc đề nghị thay nghiệp vụ.

- Nhánh E: impl/010-foundation-next, HEAD 09d569c0. Develop đối chiếu: 2fd4cd8c, nhánh E còn thiếu 12 commit. Test và hai lớp liên quan bên dưới không khác HEAD → origin/develop; không khẳng định đã chạy tái hiện trên develop.
- Lượt hiện tại chạy DEBUG=false/TRACE=false, pool test 20/cache 2. Đã ghi nhận 1.099 unit/arch xanh; lúc lập ghi nhận này, 1.411 IT đã có kết quả với 1 error. Những lớp billing đã có XML: 157 ca/17 lớp, 0 failure, 0 error, 0 skip. Không dùng số này thay kết quả full verify cuối.
- Lớp lỗi: `backend/src/test/java/com/eldercare/backend/admission/FR_046_DuplicateAttributeIT.java`, method `FR_046_cancellingTheFirstRequest_releasesTheAttribute_andClosesItsAddendum`, gọi awaitAddendumStatus tại dòng 84. Awaitility chờ tối đa 10 giây, phụ lục vẫn `PENDING_EFFECTIVE`, mong đợi `NOT_APPLIED`.
- Chủ phần: **B, spec 004, T043 (test thuộc tính chờ hiệu lực/hủy yêu cầu), T045 (handler thay đổi lưu trú)**. Không ghi B chưa làm xong toàn bộ các task đã đánh xong; đây là lỗi nghiệm thu cần phân tích thêm.
- Luồng liên quan: 000 hủy yêu cầu và phát `RequestCancelled` → `admission/internal/change/service/ResidencyChangeListeners.onCancelled` (`@ApplicationModuleListener`) → khóa người qua ResidencyLock → AddendumClosing.close. Chưa kết luận nguyên nhân là logic hay độ trễ/cô lập test khi chạy toàn kho. Cần đối chiếu log event và chạy riêng lớp lỗi sau khi bộ hiện tại kết thúc; không chạy Maven đồng thời trên cùng target/schema test.
- Không đổi timeout, không tắt test, không sửa listener/B hoặc mã nền A để làm xanh. T008 và checkpoint Phase 2 chưa được chốt qua bằng kết quả này.

Raw log/XML của lượt hiện tại lưu cục bộ ở thư mục snapshot, con trỏ `.worktrees/.snapshots/010-phase2-final-verify-latest-path.txt`. Thông tin chẩn đoán billing ở billing-observation.json; không đưa raw XML chứa cấu hình môi trường vào Git.

## Quyết định phạm vi cần người dùng chốt nếu lỗi vẫn tồn tại

AGENTS.md mục 2.2 giới hạn sửa phần E; mục 3 và gate Foundational của tasks 010 yêu cầu kiểm xanh trước khi chốt phase/làm user story. Có thể giữ E và bàn giao lỗi cho B, hoặc xin phép mở rộng đúng luồng hủy 004 để B review. Nếu muốn phát triển Phase 3 độc lập trong lúc B xử lý thì cần chốt rõ việc giữ nghiệm thu Phase 2/toàn kho mở; không gọi full verify đã qua.
