# Hệ thống quản lý viện dưỡng lão

## Giai đoạn hiện tại: PHÂN TÍCH (đã đóng 2026-10-01, chờ chủ dự án mở giai đoạn kế tiếp)

- Trạng thái: cả 20 spec (000 → 019) không còn mục checklist mở và không còn nhãn [NEEDS CLARIFICATION]; mục 24.1 docs/nghiep-vu.md không còn quyết định mở.
- Chỉ sửa spec khi được yêu cầu, hoặc để đồng bộ khi tài liệu nguồn thay đổi; không tự mở thêm lượt clarify hay checklist mới.
- KHÔNG chạy /speckit.plan, /speckit.tasks, /speckit.implement cho tới khi chủ dự án mở giai đoạn kế tiếp và sửa mục này.
- KHÔNG tạo mã nguồn, thư mục src/, file cấu hình build hay schema dữ liệu.
- Việc còn treo, cần xử lý trước khi triển khai:
  - Bảng 24.4 docs/nghiep-vu.md: các điểm cần người vận hành, chuyên môn đối chiếu.
- Đã xử lý 2026-10-01: các đề xuất của spec 000 → 008 đã vào docs/nghiep-vu.md thành Q-271 → Q-279 (24.2) và được đánh mã BR mới trong mục "Quy tắc nghiệp vụ" của các module 01, 02, 03, 04, 05, 07, 09, 15 (63 quy tắc; tổng 240 BR); hai điểm lệch của nguồn (Phụ lục 27 dòng "Ngưỡng cảnh báo"; mục 2.4 và BR-M09-02) đã sửa.

## Nguồn tài liệu

- docs/nghiep-vu.md: quy tắc nghiệp vụ (BR, CFG, quyết định Q-xx ở mục 24); actor và use case (Phụ lục 26), ma trận quyền (Phụ lục 27, mục 19.3 là căn cứ khi khác), quy tắc dữ liệu DBR (Phụ lục 28).
- docs/luong-nghiep-vu.md: mô tả các luồng nghiệp vụ chính (BF-01 → BF-17) theo từng bước: ai thực hiện, làm gì, trạng thái thay đổi thế nào và căn cứ.
- Không sửa hai file trên trừ khi được yêu cầu rõ ràng; nếu phát hiện mâu thuẫn, báo lại thay vì tự sửa.
- Hai file dẫn xuất, chỉ để đọc nhanh, **không phải nguồn**; không trích dẫn chúng trong spec, luôn trích docs/nghiep-vu.md hoặc docs/luong-nghiep-vu.md:
  - docs/tom-tat-nghiep-vu.md: tóm tắt nghiệp vụ, vai trò, nhóm chức năng và các luồng chính.
  - docs/quy-tac-nghiep-vu.md: bản chép nguyên văn BR, DBR, CFG, yêu cầu phi chức năng, tích hợp, quyền, quyết định còn mở từ docs/nghiep-vu.md.
  - docs/business-rule.md: danh sách toàn bộ quy tắc BR-Mxx-yy theo module, chép nguyên văn từ docs/nghiep-vu.md (cũng là file dẫn xuất, cập nhật cùng lượt).
- Khi hai file nguồn thay đổi (thêm BR, CFG, DBR, Q, luồng hoặc vai trò), cập nhật lại hai file dẫn xuất trong cùng lượt sửa; khi khác nhau, file nguồn là căn cứ.

## Quy ước

- Viết spec bằng tiếng Việt; thuật ngữ theo mục 2.4 docs/nghiep-vu.md.
- Giữ nguyên mã BR, DBR, UC, CFG, Q khi trích dẫn.
- Điểm chưa rõ: không tự đoán; luôn tham chiếu mã Q-xx ở mục 24 docs/nghiep-vu.md. Quyết định còn mở đã có Mặc định ở 24.1 thì ghi "theo mặc định Q-xx" và viết yêu cầu theo giá trị đó; chưa có Mặc định (hoặc chưa có mã Q) thì đánh dấu [NEEDS CLARIFICATION] kèm mã Q-xx (Q-204).
- Không đưa diagram vào spec ở giai đoạn này; trạng thái thể hiện bằng bảng.
- Khi một quyết định Q-xx được chốt: chuyển dòng từ 24.1 sang 24.2, rồi cập nhật thân tài liệu, Phụ lục 26 → 28 nếu liên quan, docs/luong-nghiep-vu.md và mục "Điểm cần báo lại" của spec liên quan (theo 24.2).
- Sau khi hoàn thành 1 bước spec nào đó, nếu yêu cầu còn quá mơ hồ thì hãy gợi ý các bước tôi nên làm tiếp theo, còn không thì hãy ghi hoàn thành để tránh việc tìm hiểu quá sâu, dễ bị bất đồng bộ với các spec khác.
- docs/phan-tich-yeu-cau.md đã bỏ (2026-09-28); các tham chiếu "4.1", "4.2", "4.4", "DBR" cũ trong spec tra theo bảng ánh xạ ở đầu Phụ lục 26 docs/nghiep-vu.md.
