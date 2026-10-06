# Hệ thống quản lý viện dưỡng lão

Đồ án phân tích nghiệp vụ, viết spec theo Spec Kit (thư mục specs/, .specify/). Hiện chưa có mã nguồn.

## Giai đoạn hiện tại: PHÂN TÍCH

- Đóng lần đầu 2026-10-01. Ngày 2026-10-05, chủ dự án sửa phạm vi (Q-280 → Q-288); giai đoạn kế tiếp vẫn chưa mở.
- KHÔNG chạy /speckit.plan, /speckit.tasks, /speckit.implement cho tới khi chủ dự án mở giai đoạn kế tiếp và sửa mục này.
- KHÔNG tạo mã nguồn, thư mục src/, file cấu hình build hay schema dữ liệu.
- Chỉ sửa spec khi được yêu cầu, hoặc để đồng bộ khi tài liệu nguồn thay đổi. Không tự mở thêm lượt clarify hay checklist mới.
- Việc còn treo:
  - (Đã xong 2026-10-06) 20 spec bị ảnh hưởng bởi Q-280 → Q-288 đã viết lại; mỗi spec có mục "Cập nhật 2026-10-06" ở Clarifications. Các điểm suy ra khi viết lại đã được chủ dự án chốt: Q-289 (khai báo khi bán trú chuyển nội trú), Q-290 (không hủy yêu cầu mua sắm đã có hàng gắn vào).
  - Bảng 24.4 docs/nghiep-vu.md: các điểm cần người vận hành và người có chuyên môn đối chiếu trước khi triển khai.
  - (Đã đóng 2026-10-05, Q-288: không làm app cho người cao tuổi.)

## Phạm vi nghiệp vụ (tóm tắt, căn cứ là docs/nghiep-vu.md)

- Vai trò (2.3): Quản lý viện, Trưởng tầng, Bác sĩ (kiêm dinh dưỡng), Điều dưỡng, Nhân viên chăm sóc, Nhân viên bếp, Hành chính, Kế toán, Người thân.
- Đã bỏ từ 2026-10-05:
  - Bỏ hai vai trò Dinh dưỡng viên và Nhân viên vệ sinh, cùng nghiệp vụ vệ sinh. Nhân viên không dùng hệ thống chỉ có hồ sơ nhân viên, không có tài khoản.
  - Bỏ thang điểm và quy đổi tự động: Bác sĩ đánh giá bằng nhận xét rồi chọn mức chăm sóc.
  - Bỏ mọi cờ gắn cho người cao tuổi: cờ nguy cơ, chỉ định hạn chế hoạt động (Q-287).
  - Không dùng cổng thanh toán.
- Kho và tài sản: mỗi bộ phận tự quản lý (13.1a). Quản lý viện chỉ duyệt chi phí qua yêu cầu mua sắm (15.10). Kế toán đối chiếu chứng từ.
- Bán trú không qua đêm. Ở lại qua đêm thì thuê giường theo nội trú ngắn ngày.
- Quy mô mã hiện có:
  - 244 BR (BR-Mxx-yy).
  - Quyết định Q-01 → Q-290.
  - Luồng BF-01 → BF-18.
  - Use case UC-01 → UC-95.
  - Quy tắc dữ liệu DBR-01 → DBR-36.

## Nguồn tài liệu

- docs/nghiep-vu.md là nguồn duy nhất về quy tắc nghiệp vụ:
  - BR, CFG (Phụ lục 25);
  - quyết định Q-xx (mục 24: 24.1 còn mở, 24.2 đã chốt, 24.4 cần đối chiếu);
  - actor và use case (Phụ lục 26);
  - ma trận quyền (Phụ lục 27; khi khác thì mục 19.3 là căn cứ);
  - DBR (Phụ lục 28).
- docs/luong-nghiep-vu.md mô tả các luồng BF-01 → BF-18 theo từng bước: ai làm, làm gì, trạng thái đổi thế nào, căn cứ.
- Không sửa hai file trên trừ khi được yêu cầu rõ ràng. Phát hiện mâu thuẫn thì báo lại, không tự sửa.
- Không có file dẫn xuất. Các file tom-tat-nghiep-vu.md, quy-tac-nghiep-vu.md, business-rule.md, cau-hoi-ra-soat-van-hanh.md đã xoá ngày 2026-10-05; không tạo lại. Tham chiếu tới chúng trong checklist cũ chỉ là lịch sử.
- docs/phan-tich-yeu-cau.md đã bỏ ngày 2026-09-28. Các tham chiếu cũ "4.1", "4.2", "4.4", "DBR" trong spec tra theo bảng ánh xạ ở đầu Phụ lục 26.

## Quy ước

- Viết spec bằng tiếng Việt; thuật ngữ theo mục 2.4 docs/nghiep-vu.md.
- Giữ nguyên mã BR, DBR, UC, CFG, Q, BF khi trích dẫn. Mã bị bỏ không đánh lại số: giữ dòng và ghi "(Bỏ, ngày, Q-xx)" hoặc "Không còn áp dụng".
- Đánh dấu thay đổi trong tài liệu nguồn bằng "**(Bổ sung/Sửa/Bỏ, YYYY-MM-DD, Q-xx)**" như các đoạn đã có.
- Điểm chưa rõ: không tự đoán; luôn tham chiếu mã Q-xx ở mục 24.
  - Quyết định còn mở đã có Mặc định ở 24.1: ghi "theo mặc định Q-xx" và viết yêu cầu theo giá trị đó.
  - Chưa có Mặc định hoặc chưa có mã Q: đánh dấu [NEEDS CLARIFICATION] kèm mã Q-xx (Q-204).
- Khi chốt một quyết định (từ 24.1, hoặc chủ dự án quyết trực tiếp), làm theo thứ tự:
  1. Ghi dòng vào 24.2, kèm các Q cũ bị thay hoặc sửa.
  2. Đánh dấu các dòng Q cũ đó.
  3. Cập nhật thân tài liệu và Phụ lục 25 → 28 nếu liên quan.
  4. Cập nhật docs/luong-nghiep-vu.md.
  5. Cập nhật bảng 24.4 nếu cần đối chiếu.
  6. Cập nhật mục "Điểm cần báo lại" của các spec liên quan.
- Không đưa diagram vào spec ở giai đoạn này; trạng thái thể hiện bằng bảng.
- Sau mỗi bước spec: nếu yêu cầu còn mơ hồ thì gợi ý bước tiếp theo; nếu không thì ghi hoàn thành, tránh đào quá sâu làm lệch với các spec khác.

## Ghi chú công cụ

- Môi trường không có Python; dùng node cho script sửa file hàng loạt.
- Trong lệnh Bash (kể cả heredoc), chuỗi `\\` có thể bị rút thành `\`. Không dựng regex từ chuỗi có backslash; dùng regex literal hoặc `split`/`startsWith`. Script sửa file phải kiểm tra số lần khớp trước khi ghi.
