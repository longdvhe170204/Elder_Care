# Checkpoint chung 008 và 010 — 06/11/2026

Người dùng yêu cầu tạm dừng triển khai và đưa toàn bộ công việc hiện có lên `develop`. Đây là checkpoint lưu hiện trạng; các task chưa nghiệm thu giữ mở. Không triển khai tiếp T011/T012 chỉ để đổi kết quả test.

## Phần được gom

- Nhánh `impl/008-integration`: R-15, Q-262, nguồn `ShiftStaffPort`, phân công và app ca/bàn giao/vắng ca, các tài liệu và ảnh đang sửa. Giữ năm task nghiệm thu còn mở của 008.
- Nhánh `impl/010-foundation-next`: nền, lưu giữ, đơn giá, chỉ mục nguồn T010, chọn đơn vị nguồn, phần đang viết của T011/T012 cùng test trước cài đặt. Giữ T008/Phase 2 và các phần chưa hoàn tất mở.
- Báo cáo tại gốc và tài liệu riêng ở worktree cũ. Khôi phục hai bản sửa fixture `M09GovernanceIT`/`ShiftQueriesIT` cùng test đổi kết nối vào package/bảng hiện hành. Nhật ký được ghép bằng cách giữ các dòng của hai phía và bổ sung chín dòng lịch sử còn thiếu.
- Giữ menu mobile mới trên develop: Người cao tuổi, Phê duyệt, mã QR, Ca của tôi, Phân công, Bàn giao. Test quyền của E kiểm cả mục ẩn bên trong "Thêm"; không bỏ assertion hoặc test.
- Nhận toàn bộ develop đến PR #117 (`4fecb027`), gồm các màn và hợp đồng admission mới của B. Client web được sinh lại bằng `npm run gen:api`.

## Kiểm tra

- Web: 601 test/102 file xanh sau PR #117; lint và build qua.
- Mobile: 364 test xanh sau PR #117; analyzer sạch trên bản cuối. Không đổi dependency hoặc cấu hình Developer Mode của máy.
- Backend: lượt `clean verify -Dit.forkCount=3` trước khi nhận PR #117 chạy 1199 test đơn vị/kiến trúc, 16 thất bại ở `CostSourceRequestTest` và 5 lỗi ở `ResidencyBillingSourcesTest`. Đây là test T011/T012 đã viết trước, request còn thiếu validation và adapter còn `UnsupportedOperationException`. Maven dừng trước IT; không gọi full verify xanh, không tắt/bỏ qua test.
- Kiểm IT riêng trên bản sau PR #117: 39 test/5 lớp xanh, không failure/error/skip (`M09GovernanceIT`, `ShiftQueriesIT`, `ApiContractIT`, `BillableSourceIndexIT`, `BillingSourceLookupIT`). Spotless kiểm hai file Java khôi phục qua. Lượt trước nhận PR trong khi test đang chạy nên `ApiContractIT` đọc YAML mới với bytecode cũ; lượt biên dịch lại cuối đã xanh. Kết quả IT riêng không thay cho full verify còn đỏ nêu trên.

## Bảo toàn công việc

Diff và mọi file chưa commit, chưa bị ignore của mười worktree đã được sao lưu cục bộ kèm SHA-256 tại `.worktrees/.snapshots/publish-e-20261106/before/manifest.json`. Giữ nguyên các worktree cũ và bốn stash. Hai test client cũ `api_delete_test.dart`/`api_list_test.dart` thuộc bản client trước đã được thay bằng client chung và test controller hiện hành; không đưa API cũ trở lại production.

Việc đẩy checkpoint không đóng các gate nghiệp vụ, nguồn thật, retention liên module hoặc nghiệm thu. Sau khi tiếp tục triển khai, xử lý các test đỏ của 010 và chạy lại full verify trước khi nghiệm thu phase.
