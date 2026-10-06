# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Hệ thống quản lý viện dưỡng lão

Đồ án phân tích nghiệp vụ, viết spec theo Spec Kit (thư mục specs/, .specify/). Hiện chưa có mã nguồn, không có lệnh build, lint hay test.

## Giai đoạn hiện tại: PHÂN TÍCH

- Phân tích hoàn tất ngày 2026-10-06 (Q-01 → Q-304). Không còn việc treo; chờ chủ dự án mở giai đoạn thiết kế.
- KHÔNG chạy /speckit.plan, /speckit.tasks, /speckit.implement cho tới khi chủ dự án mở giai đoạn kế tiếp và sửa mục này.
- KHÔNG tạo mã nguồn, thư mục src/, file cấu hình build hay schema dữ liệu.
- Chỉ sửa spec khi được yêu cầu, hoặc để đồng bộ khi tài liệu nguồn thay đổi. Không tự mở thêm lượt clarify hay checklist mới.

## Phạm vi nghiệp vụ (tóm tắt, căn cứ là docs/nghiep-vu.md)

- Vai trò (2.3): Quản lý viện, Trưởng tầng, Bác sĩ (kiêm dinh dưỡng), Điều dưỡng, Nhân viên chăm sóc, Nhân viên bếp, Hành chính, Kế toán, Người thân.
- Đã bỏ từ 2026-10-05:
  - Bỏ hai vai trò Dinh dưỡng viên và Nhân viên vệ sinh, cùng nghiệp vụ vệ sinh. Nhân viên không dùng hệ thống chỉ có hồ sơ nhân viên, không có tài khoản.
  - Bỏ thang điểm và quy đổi tự động: Bác sĩ đánh giá bằng nhận xét rồi chọn mức chăm sóc.
  - Mức chăm sóc chỉ còn 3 mức (cơ bản, thường xuyên, đặc biệt); các nhu cầu cụ thể (hỗ trợ vận động, phục hồi…) là danh mục nhu cầu chăm sóc riêng (Q-291, 2026-10-06).
  - Bỏ mọi cờ gắn cho người cao tuổi: cờ nguy cơ, chỉ định hạn chế hoạt động (Q-287).
  - Không dùng cổng thanh toán.
- Kho và tài sản: mỗi bộ phận tự quản lý (13.1a). Quản lý viện không vận hành kho, tài sản; chỉ thực hiện quyền phê duyệt cấp viện (yêu cầu mua sắm 15.10, thanh lý và chuyển tài sản 7.7, xử lý lệch). Kế toán đối chiếu chứng từ.
- Bán trú không qua đêm. Ở lại qua đêm thì thuê giường theo nội trú ngắn ngày.
- Quy mô mã hiện có:
  - 241 BR (BR-Mxx-yy; đã xóa BR-M03-08, 11, 12, 14).
  - Quyết định Q-01 → Q-304.
  - Luồng BF-01 → BF-18.
  - Use case UC-01 → UC-95, trừ UC-71 → 73 (92 UC); actor AC-00 → AC-15, trừ AC-06, AC-08.
  - 120 tham số CFG.
  - Quy tắc dữ liệu DBR-01 → DBR-36.

## Nguồn tài liệu

- docs/nghiep-vu.md là nguồn duy nhất về quy tắc nghiệp vụ:
  - BR, CFG (Phụ lục 25);
  - quyết định Q-xx (mục 24: 24.1 còn mở, 24.2 đã chốt, 24.4 cần đối chiếu);
  - actor và use case (Phụ lục 26);
  - ma trận quyền (Phụ lục 27; khi khác thì mục 19.3 là căn cứ);
  - DBR (Phụ lục 28);
  - căn cứ pháp lý (Phụ lục 29): văn bản thay đổi thì rà các mục ở cột "Quy tắc dựa vào", đổi quy tắc thì mở Q mới.
- docs/luong-nghiep-vu.md mô tả các luồng BF-01 → BF-18 theo từng bước: ai làm, làm gì, trạng thái đổi thế nào, căn cứ.
- Không sửa hai file trên trừ khi được yêu cầu rõ ràng. Phát hiện mâu thuẫn thì báo lại, không tự sửa.
- Không có file dẫn xuất. Các file tom-tat-nghiep-vu.md, quy-tac-nghiep-vu.md, business-rule.md, cau-hoi-ra-soat-van-hanh.md đã xoá ngày 2026-10-05; không tạo lại. Tham chiếu tới chúng trong checklist cũ chỉ là lịch sử.
- docs/phan-tich-yeu-cau.md đã bỏ ngày 2026-09-28. Các tham chiếu cũ "4.1", "4.2", "4.4", "DBR" trong spec tra theo bảng ánh xạ ở đầu Phụ lục 26.
- .specify/memory/constitution.md: 9 nguyên tắc (I–IX) mà spec và checklist phải tuân thủ; spec trích dẫn dạng "constitution I". Sửa constitution phải ghi Sync Impact Report và tăng phiên bản. Mục "Tài liệu nguồn" của constitution chưa nhắc Phụ lục 29.

## Spec Kit và cấu trúc spec

- Lệnh dùng qua skill `/speckit-*` (specify, clarify, checklist, analyze, plan, tasks…); script hỗ trợ ở .specify/scripts/powershell. Ở giai đoạn này chỉ dùng specify, clarify, checklist (constitution IX).
- .specify/feature.json chỉ feature mà các lệnh `/speckit-*` đang làm việc; kiểm tra hoặc đổi trước khi chạy lệnh cho một spec khác.
- specs/NNN-ten/spec.md, số NNN là cột Feature của Phụ lục 26.2 (bảng UC → feature). 000 là nền tảng dùng chung (yêu cầu phê duyệt, đính chính, nhật ký, tham số) mà các spec khác gọi là "feature 000 FR-xx".
- Mỗi spec gồm:
  - Clarifications: các Session và mục "Cập nhật <ngày>[, lượt n]" ghi thay đổi theo từng đợt Q; đây là lịch sử.
  - Phạm vi, User Scenarios (Given/When/Then), Requirements (FR-xx, mỗi FR kết thúc bằng *(Nguồn: …)*), bảng trạng thái, Key Entities, Success Criteria, Assumptions; đây là nội dung hiện hành. Dòng "Truy vết use case" nối nhóm FR với UC.
  - Bảng giao tiếp giữa các feature (Nhận / Cung cấp / Gửi, kèm số feature).
  - "Điểm cần báo lại về tài liệu nguồn" ở cuối: nhật ký nối thêm theo thời gian, mục đã xử lý gắn "(Đã xử lý <ngày>: …)".
- Trong spec được giữ nhãn lịch sử "(Sửa/Bổ sung/Bỏ, <ngày>, Q-xx)" ngay trong FR; tài liệu nguồn thì không (xem Quy ước).
- specs/*/checklists/ là kết quả checklist cũ, chỉ có giá trị lịch sử.
- Phần lịch sử của spec có thể nhắc mã đã xóa khỏi tài liệu nguồn (AC-06, AC-08, UC-71 → 73, BR-M03-08, 11, 12, 14, CFG-M01-05, CFG-M03-03, 05, 06, CFG-M04-14); đó không phải lỗi. Bản tài liệu nguồn còn ghi chú thay đổi nằm ở commit 3543384.

## Quy ước

- Viết spec bằng tiếng Việt; thuật ngữ theo mục 2.4 docs/nghiep-vu.md.
- Giữ nguyên mã BR, DBR, UC, CFG, Q, BF khi trích dẫn. Mã bị bỏ thì xóa khỏi tài liệu nguồn; không đánh lại số và không dùng lại mã đó. Riêng mã Q bị bỏ vẫn giữ một dòng ở 24.2: "Không còn áp dụng, thay bằng Q-xx".
- Tài liệu nguồn chỉ ghi nội dung hiện hành (từ 2026-10-06): sửa thẳng nội dung, không đánh dấu "(Bổ sung/Sửa/Bỏ…)", không giữ nội dung gạch. Lịch sử thay đổi tra theo git và dòng Q ở 24.2. Số mục và số bước của luồng giữ cố định để spec tham chiếu.
- Cách viết dải mã: "BR-M01-09, 10" nghĩa là BR-M01-09 và BR-M01-10; "UC-71 → 73" là dải. Vì vậy không đặt số mục ngay sau mã bằng dấu phẩy ("BR-M01-09, 17" sẽ bị đọc là BR-M01-17); viết "BR-M01-09; mục 17".
- Điểm chưa rõ: không tự đoán; luôn tham chiếu mã Q-xx ở mục 24.
  - Quyết định còn mở đã có Mặc định ở 24.1: ghi "theo mặc định Q-xx" và viết yêu cầu theo giá trị đó.
  - Chưa có Mặc định hoặc chưa có mã Q: đánh dấu [NEEDS CLARIFICATION] kèm mã Q-xx (Q-204).
- Khi chốt một quyết định (từ 24.1, hoặc chủ dự án quyết trực tiếp), làm theo thứ tự:
  1. Ghi dòng vào 24.2 (theo thứ tự số Q), kèm các Q cũ bị thay hoặc sửa.
  2. Viết lại các dòng Q cũ đó theo nội dung hiện hành, hoặc đổi thành "Không còn áp dụng, thay bằng Q-xx".
  3. Cập nhật thân tài liệu và Phụ lục 25 → 29 nếu liên quan.
  4. Cập nhật docs/luong-nghiep-vu.md.
  5. Cập nhật bảng 24.4 nếu cần đối chiếu.
  6. Cập nhật mục "Điểm cần báo lại" của các spec liên quan.
- Không đưa diagram vào spec ở giai đoạn này; trạng thái thể hiện bằng bảng.
- Sau mỗi bước spec: nếu yêu cầu còn mơ hồ thì gợi ý bước tiếp theo; nếu không thì ghi hoàn thành, tránh đào quá sâu làm lệch với các spec khác.

## Ghi chú công cụ

- Môi trường không có Python; dùng node cho script sửa file hàng loạt.
- Trong lệnh Bash (kể cả heredoc), chuỗi `\\` có thể bị rút thành `\`. Không dựng regex từ chuỗi có backslash; dùng regex literal hoặc `split`/`startsWith`. Script sửa file phải kiểm tra số lần khớp trước khi ghi.
