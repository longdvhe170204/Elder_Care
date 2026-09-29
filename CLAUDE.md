# Hệ thống quản lý viện dưỡng lão

## Giai đoạn hiện tại: PHÂN TÍCH

- Chỉ tạo và chỉnh sửa spec (specify, clarify, checklist).
- KHÔNG chạy /speckit.plan, /speckit.tasks, /speckit.implement.
- KHÔNG tạo mã nguồn, thư mục src/, file cấu hình build hay schema dữ liệu.

## Nguồn tài liệu

- docs/nghiep-vu.md: quy tắc nghiệp vụ (BR, CFG, quyết định Q-xx ở mục 24); actor và use case (Phụ lục 26), ma trận quyền (Phụ lục 27, mục 19.3 là căn cứ khi khác), quy tắc dữ liệu DBR (Phụ lục 28).
- docs/luong-nghiep-vu.md: mô tả các luồng nghiệp vụ chính (BF-01 → BF-17) theo từng bước: ai thực hiện, làm gì, trạng thái thay đổi thế nào và căn cứ.
- Không sửa hai file trên trừ khi được yêu cầu rõ ràng; nếu phát hiện mâu thuẫn, báo lại thay vì tự sửa.
- Hai file dẫn xuất, chỉ để đọc nhanh, **không phải nguồn**; không trích dẫn chúng trong spec, luôn trích docs/nghiep-vu.md hoặc docs/luong-nghiep-vu.md:
  - docs/tom-tat-nghiep-vu.md: tóm tắt nghiệp vụ, vai trò, nhóm chức năng và các luồng chính.
  - docs/quy-tac-nghiep-vu.md: bản chép nguyên văn BR, DBR, CFG, yêu cầu phi chức năng, tích hợp, quyền, quyết định còn mở từ docs/nghiep-vu.md.
- Khi hai file nguồn thay đổi (thêm BR, CFG, DBR, Q, luồng hoặc vai trò), cập nhật lại hai file dẫn xuất trong cùng lượt sửa; khi khác nhau, file nguồn là căn cứ.

## Quy ước

- Viết spec bằng tiếng Việt; thuật ngữ theo mục 2.4 docs/nghiep-vu.md.
- Giữ nguyên mã BR, DBR, UC, CFG, Q khi trích dẫn.
- Điểm chưa rõ: không tự đoán; luôn tham chiếu mã Q-xx ở mục 24 docs/nghiep-vu.md. Quyết định còn mở đã có Mặc định ở 24.1 thì ghi "theo mặc định Q-xx" và viết yêu cầu theo giá trị đó; chưa có Mặc định (hoặc chưa có mã Q) thì đánh dấu [NEEDS CLARIFICATION] kèm mã Q-xx (Q-204).
- Không đưa diagram vào spec ở giai đoạn này; trạng thái thể hiện bằng bảng.
- Khi một quyết định Q-xx được chốt: chuyển dòng từ 24.1 sang 24.2, rồi cập nhật thân tài liệu, Phụ lục 26 → 28 nếu liên quan, docs/luong-nghiep-vu.md và mục "Điểm cần báo lại" của spec liên quan (theo 24.2).
- Sau khi hoàn thành 1 bước spec nào đó, hãy gợi ý cho tôi bước tiếp theo nên làm gì sao cho phù hợp nhất.
- docs/phan-tich-yeu-cau.md đã bỏ (2026-09-28); các tham chiếu "4.1", "4.2", "4.4", "DBR" cũ trong spec tra theo bảng ánh xạ ở đầu Phụ lục 26 docs/nghiep-vu.md.
