# Đồng bộ develop và Q-394 → Q-397 — 2026-10-29

## 1. Nhánh và công việc đang dở

- Người dùng yêu cầu pull develop trước khi tiếp tục. Đã fast-forward nhánh `impl/010-incurred-costs` từ `da9636a` lên `origin/develop e7b2b71`, nhận bốn commit (hai thay đổi và hai commit merge PR #52/#53 có sẵn). Không tạo commit mới, stash, nhánh hoặc chuyển nhánh; chưa push/PR.
- Đã sao lưu 18 tệp đang sửa/tạo và đối chiếu SHA-256 trước khi pull; 11 tệp không chồng lấn giữ nguyên byte, bảy tệp tài liệu chồng lấn đã ghép. Hai chỗ trùng ở progress/team-plan được ghép giữ nội dung hai bên: progress chỉ nối các dòng tiến độ, không sửa lịch sử; team-plan giữ sửa mới Q-392 của A và ghi chú kiểu tiền 010 của E.
- Bản sao: `.worktrees/.snapshots/010-before-q394-397-20261029-1793278666668/` ở checkout gốc; có manifest SHA-256, bản từng tệp, diff và log pull. [Manifest](../../../.snapshots/010-before-q394-397-20261029-1793278666668/manifest.json). Không sửa các worktree 008 hoặc đưa mã ở đó sang 010.

## 2. Ảnh hưởng tới phần E

| Quyết định | Áp dụng | Trạng thái sau đồng bộ |
| --- | --- | --- |
| Q-394 | B/004 và A/016 bỏ điểm ưu tiên tự tính; Hành chính đánh dấu Ưu tiên có lý do, xếp theo dấu rồi ngày đăng ký | Đã nhận tài liệu từ develop; không sửa module B/A |
| Q-395 | Nội trú vắng vẫn tính đủ giá; từ chính ngày Giải phóng giường không tính phí; bán trú giữ tỷ lệ nguồn từ CFG-M02-05 | Đã sửa helper nội trú của E bỏ tham số hệ số. Bổ sung test bán trú 0/50/100%. Chưa cài kiểm ngày giải phóng vì thiếu nguồn B/004 và phần lập khoản |
| Q-396 | Bỏ bản tin người thân, US7/FR-051 → 057 và các bảng/API/port bản tin | Đã nhận spec/tasks mới của 012; không làm các task đã ghi Bỏ. Gỡ yêu cầu trigger bản tin đã cũ ở team-plan. Cổng người thân còn triển khai sau |
| Q-397 | 015 chỉ còn đổi ca/nghỉ đột xuất; bỏ mẫu xoay ca/phủ tối thiểu/lời mời; nghỉ theo ngày mở cho mọi nhân viên, chọn phạm vi | Đã nhận tài liệu mới; team-plan dùng tên logic DoiCaThongKeApi. Chưa cài API hoặc 015. Không đánh dấu các task bị bỏ là đã làm |
| 008 liên quan Q-397 | A đã bỏ INSUFFICIENT_MINIMUM_COVERAGE và rotation khỏi ArchTest; nguồn INVITED/ROLE_INVITATION_SENT còn cần rà | Đã nhận thay đổi A trên nhánh 010. Ghi phần còn lại ở team-plan; chưa xóa dữ liệu/migration hoặc sửa các worktree 008 đang dở |

Nguồn: `docs/business/nghiep-vu.md` Q-394 → Q-397; `specs/010-incurred-costs/spec.md` mục cập nhật 2026-10-29, FR-005a/007/009 và kịch bản mục C. Ngày giải phóng là mốc loại trừ, ví dụ giải phóng 05/11 thì chỉ tính tới 04/11. Đã sửa ghi chú data-model 010 còn ghi “ngày sau” cho khớp spec; không sửa nghiệp vụ hoặc spec.

## 3. Mã và kiểm chứng

- Test được sửa trước mã: `q395-unit-red.log` lỗi `testCompile` vì lớp cũ còn yêu cầu hệ số. Sau bỏ hệ số khỏi `ResidencyDayFee.monthly/direct`, `q395-unit-green.log` có 49 test xanh: DongRounding 12, ResidencyDayFee 20, DayCareSessionFee 13, M11ArchTest 4. Không gọi lượt chưa biên dịch được là 49 assertion đã chạy đỏ.
- Helper chỉ tính số tiền cho ngày đã được nguồn xác định là được tính phí. Các test toán học không chứng minh đã có nguồn ngày vắng/ngày giải phóng, quyền hoặc lệnh lập khoản. BillingPeriod chưa cài; T006 vẫn [ ] (3/4). T001 đã [x], các task nền khác chưa đóng.
- Spotless M11 đã qua. Kết quả clean verify sau pull: clean verify nền đã nạp .env/backend/.env và giữ mật khẩu demo; hoàn tất 08:03: 689 unit/arch xanh (M11 49), 1426 IT còn 7 thất bại/9 lỗi, 0 bỏ qua, đúng 9 lớp M01/M03 đã có trong team-plan và lượt trước pull; không có lớp lỗi mới. Spotless check 2009 file sạch. Maven trả BUILD FAILURE do các lỗi nền này, không coi toàn kho xanh. Bằng chứng ignored: q395-clean-verify.log và q395-full-summary.json tại backend/target/010-foundation-verification của checkout gốc.
- Đối chiếu lại bản dịch API010 với HEAD mới: giữ 28 đường dẫn, 29 thao tác, 17 schema, 101 ref, 16 body; cấu trúc sau ánh xạ tên trùng bản gốc. File API010 trên develop không đổi giữa da9636a/e7b2b71 nên xác nhận duyệt của A/chủ dự án vẫn có hiệu lực.
- Không có thay đổi mã web/mobile từ phần E trong lượt này nên không chạy npm test/lint/build hoặc flutter test. Các thay đổi web/contract 006/008 do A đã có trong commit vừa pull.

## 4. Tới đâu cần chờ

- **B/004:** chưa có API/fake tài chính đúng chủ cho hợp đồng theo ngày, đơn giá/ngày lưu trú, ngày Giải phóng giường và kiểu hồ sơ kết thúc; cần trước khi nối BillingPeriod/kiểm ngày lưu trú/lập khoản. Đã ghi rõ mốc loại trừ trong team-plan và contracts/module-interfaces mục 0. Không tự thêm API của B hoặc fake thứ hai.
- **C/005:** còn thiếu sự kiện kết quả tính phí/đính chính/trạng thái có mặt; **D/006:** còn thiếu producer điều chỉnh thuốc và PRN/giao thuốc. Các phần nguồn tương ứng phải chờ; chưa coi record đã khai báo là nguồn thật.
- **A nền web:** thêm 010 vào `frontend/scripts/gen-api.mjs:9` mới sinh được client010. Không viết tay client hoặc tự sửa tệp nền A. Hướng dẫn cụ thể ở foundation-verification mục 5.
- **E còn làm được độc lập:** T003/T004 khai báo API/port và fake của E, T005 test ràng buộc/migration theo tám dấu mới, T007/T008 theo nền/phase. Đây không phải việc phải đợi B/C/D làm thay.
- Trước các task HTTP có phản hồi chưa rõ schema: còn 17 response 2xx của bản gốc phải cụ thể hóa theo quy trình duyệt hợp đồng. Không tự suy đoán DTO.

## 5. Câu chữ còn cũ cần lưu ý ở lượt sau

- `spec.md:393` (FR-007) còn câu “ngày sau Giải phóng giường”, trong khi mục cập nhật Q-395 tại dòng 110 và kịch bản mục C tại dòng 174 nói từ chính ngày giải phóng. Phần math hiện tại không quyết định ngày được tính phí nên chưa cài theo câu chữ mâu thuẫn này; bộ kiểm ngày phải theo quyết định đã chốt khi nối nguồn B. Chưa tự sửa spec.
- `spec.md:484` (FR-038) đã ghi bỏ bản tin nhưng nguồn còn dẫn 012 FR-052; dòng 656 còn nhắc thành phần bản tin. Các đoạn này cần đồng bộ theo Q-396, không dựng lại API/bảng bản tin. Chưa sửa spec.
- `specs/012-family-portal/tasks.md:246,250,270` còn phụ thuộc/thứ tự US7 dù task US7 đã ghi Bỏ; xử lý tài liệu 012 trên nhánh tương ứng khi tới feature đó. Không coi các tham chiếu cũ là yêu cầu làm bản tin.

Nhật ký cũ trong progress được giữ nguyên; kết quả đồng bộ mới chỉ thêm cuối file. Tài liệu/mã E giữ UTF-8 không BOM, CRLF; chưa commit/push/PR.
