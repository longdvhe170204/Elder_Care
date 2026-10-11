# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Hệ thống quản lý viện dưỡng lão

Đồ án quản lý viện dưỡng lão: nghiệp vụ và spec viết theo Spec Kit (thư mục specs/, .specify/); mã nguồn triển khai theo plan và tasks của từng feature.

## Giai đoạn hiện tại: TRIỂN KHAI

- Lịch sử:
  - phân tích xong ngày 2026-10-06;
  - thiết kế từ 2026-10-06 tới 2026-10-10: cả 20 feature (000 → 019) có plan, tasks và đã qua /speckit-analyze; điểm báo lại của plan chốt bằng Q-311 → Q-336;
  - chủ dự án mở giai đoạn triển khai ngày 2026-10-10.
- Công nghệ (Q-305, constitution 2.1.0, nguyên tắc X–XV):
  - backend Spring Boot, RESTful API `/api/v1`, MySQL; một khối chia gói theo module M01–M15;
  - web React (TypeScript) có mọi chức năng, gồm quản trị và cổng người thân trên trình duyệt;
  - app Flutter nhân viên (ghi ngoại tuyến theo Q-01) và app Flutter người thân;
  - phiên bản: Java 21 LTS, Spring Boot 3.5.x, MySQL 8.4 LTS, TypeScript 5.x, Flutter stable 3.x (research R-01 của 000); thư viện cụ thể ghi ở plan và tasks.
- Lệnh được dùng:
  - /speckit-implement, /speckit-analyze, /speckit-converge;
  - /speckit-plan, /speckit-tasks chỉ để sửa plan, tasks khi spec hoặc tài liệu nguồn thay đổi;
  - /speckit-taskstoissues chỉ khi chủ dự án yêu cầu (đẩy dữ liệu lên GitHub).
- Thứ tự triển khai theo phụ thuộc: 000 → 002 → 001 → 003 → 004 → 005 → … → 019; điều kiện của từng feature ghi ở dòng "Prerequisites" đầu tasks.md. Đổi .specify/feature.json trước mỗi feature.
- Cách làm mỗi feature:
  - làm trên nhánh riêng `impl/NNN-ten` tách từ `develop`, gộp về `develop` khi feature xong;
  - làm lần lượt từng phase của tasks.md; hết mỗi phase chạy test, đánh dấu `[x]` các task đã xong và đề nghị chủ dự án commit;
  - phần phụ thuộc feature chưa làm thì dùng bản giả như tasks ghi, không bỏ qua task.
- Mã nguồn:
  - theo constitution X–XV: nghiệp vụ, quyền và điều kiện kiểm ở backend; mỗi lệnh nhóm 2 là một endpoint riêng; bản ghi nhóm 3 chỉ thêm, sai sót dùng đính chính; mọi CFG đọc qua `ThamSoApi`; mọi thông báo qua 009; module gọi nhau qua giao diện ở contracts/module-interfaces.md; mỗi BR có test tự động;
  - tên gói, lớp, bảng, endpoint theo plan, data-model, contracts; tên test gắn mã (ví dụ `BR_M02_11_KhaiBaoIT`, `FR_076_…IT`);
  - không đổi nghiệp vụ trong mã. Gặp thiếu sót nghiệp vụ hoặc lệch giữa spec, plan, tasks thì dừng task đó và báo lại; sửa theo thứ tự docs/business/nghiep-vu.md → spec → plan → tasks rồi mới viết mã (constitution I, IX);
  - lệch kỹ thuật (tên lớp, thư viện, cách cài) thì sửa plan, tasks cùng lúc với mã và ghi lý do ở plan.
- Còn mở 3 điểm chờ tư vấn pháp lý (24.4): Q-03, Thông tư 27/2024/TT-BYT, Luật Cư trú 118/2025/QH15. Các ngưỡng liên quan là tham số nên không chặn việc viết mã; cần chốt trước khi vận hành.
- Chạy /speckit-analyze theo cùng thứ tự, đổi feature.json trước mỗi feature; analyze chỉ đọc, kết quả trả trong hội thoại, không ghi file trừ khi chủ dự án yêu cầu. Sau analyze từng feature, rà thêm giao diện giữa các feature (mục "bổ sung theo plan này" phải có ở feature cung cấp).
- Xử lý kết quả analyze (sau khi chủ dự án duyệt):
  - lệch ở plan, tasks: sửa plan, tasks;
  - spec lệch tài liệu nguồn: đồng bộ spec, ghi mục "Cập nhật <ngày>" ở Clarifications và một dòng "(Đã xử lý …)" ở "Điểm cần báo lại";
  - thiếu sót nghiệp vụ: báo lại để sửa docs/business/nghiep-vu.md trước;
  - xung đột constitution (CRITICAL): sửa spec, plan hoặc tasks, không sửa constitution trong analyze.
- Chỉ sửa spec khi được yêu cầu, hoặc để đồng bộ khi tài liệu nguồn thay đổi. Không tự mở thêm lượt clarify hay checklist mới.

## Môi trường, cấu trúc mã và lệnh

- Cần cài: JDK 21, Node.js và npm, Flutter stable, Docker (Testcontainers chạy MySQL 8.4 cho test `*IT`). Maven dùng qua `./mvnw` của dự án.
- Một kho mã, bốn thư mục gốc (plan 000 mục Project Structure, research R-02):
  - `backend/`: `src/main/java/vn/eldercare/{common, m01 … m15}`, Flyway ở `src/main/resources/db/migration/`, test ở `src/test/java/vn/eldercare/` (`*Test` đơn vị, `*IT` tích hợp, `*ArchTest` kiến trúc);
  - `web/`: `src/{api, chung, tinh-nang}`;
  - `app-nhan-vien/`, `app-nguoi-than/`: `lib/{chung, tinh-nang}`.
- Các thư mục và file build được tạo ở Phase 1 của tasks 000; trước đó các lệnh dưới chưa chạy được.
- Lệnh (quickstart 000):
  - `backend/`: `./mvnw verify` chạy test đơn vị, tích hợp, kiến trúc và so hợp đồng; `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` chạy với dữ liệu mẫu;
  - `web/`: `npm ci && npm test && npm run dev`;
  - `app-nhan-vien/`, `app-nguoi-than/`: `flutter test`.
- Profile `test` và `demo` có đồng hồ giả lập để kiểm quy tắc theo thời gian (NFR-13; task T012 của 000).
- Khi lệnh thực tế khác (ví dụ cách chạy một lớp test), cập nhật mục này.

## Phạm vi nghiệp vụ (tóm tắt, căn cứ là docs/business/nghiep-vu.md)

- Vai trò (2.3): Quản lý viện, Trưởng tầng, Bác sĩ (kiêm dinh dưỡng), Điều dưỡng, Nhân viên chăm sóc, Nhân viên bếp, Hành chính, Kế toán, Người thân.
- Đã bỏ từ 2026-10-05:
  - Bỏ hai vai trò Dinh dưỡng viên và Nhân viên vệ sinh, cùng nghiệp vụ vệ sinh. Nhân viên không dùng hệ thống chỉ có hồ sơ nhân viên, không có tài khoản.
  - Bỏ thang điểm và quy đổi tự động: Bác sĩ đánh giá bằng nhận xét rồi chọn mức chăm sóc.
  - Mức chăm sóc chỉ còn 3 mức (cơ bản, thường xuyên, đặc biệt); các nhu cầu cụ thể (hỗ trợ vận động, phục hồi…) là danh mục nhu cầu chăm sóc riêng (Q-291, 2026-10-06).
  - Bỏ mọi cờ gắn cho người cao tuổi: cờ nguy cơ, chỉ định hạn chế hoạt động (Q-287).
  - Không dùng cổng thanh toán.
- Hệ thống không tự sinh, tự tạo hay tự gợi ý (Q-306, Q-307, 2026-10-06): mọi bản ghi do actor lập bằng lệnh; không gợi ý, đề xuất, không tự tạo yêu cầu hay sự cố, không tự thực hiện lệnh thay actor. Vẫn giữ: tự chuyển trạng thái theo giờ, cảnh báo do hệ thống tạo và leo thang, nhắc, thông báo, kiểm tra và chặn, bản ghi và tác động kèm theo lệnh của actor, số liệu tính để hiển thị.
- Kho và tài sản: mỗi bộ phận tự quản lý (13.1a). Quản lý viện không vận hành kho, tài sản; chỉ thực hiện quyền phê duyệt cấp viện (yêu cầu mua sắm 15.10, thanh lý và chuyển tài sản 7.7, xử lý lệch). Kế toán đối chiếu chứng từ.
- Bán trú không qua đêm. Ở lại qua đêm thì thuê giường theo nội trú ngắn ngày.
- Quy mô mã hiện có:
  - 245 BR (BR-Mxx-yy; đã xóa BR-M03-08, 11, 12, 14; thêm BR-M07-24, 25 theo Q-337, Q-340).
  - Quyết định Q-01 → Q-345 (Q-337 → Q-345 chốt 2026-10-11 sau đối chiếu tài liệu gốc DOCX).
  - Luồng BF-01 → BF-18.
  - Use case UC-01 → UC-95, trừ UC-71 → 73 (92 UC); actor AC-00 → AC-15, trừ AC-06, AC-08.
  - 118 tham số CFG (đã xóa CFG-M04-08, 09, CFG-M07-03 theo Q-306; CFG-M02-01, CFG-M08-02 theo Q-307; thêm CFG-M11-10 theo Q-335).
  - Quy tắc dữ liệu DBR-01 → DBR-36.

## Nguồn tài liệu

- docs/business/nghiep-vu.md là nguồn duy nhất về quy tắc nghiệp vụ:
  - BR, CFG (Phụ lục 25);
  - quyết định Q-xx (mục 24: 24.1 còn mở, 24.2 đã chốt, 24.4 cần đối chiếu);
  - actor và use case (Phụ lục 26);
  - ma trận quyền (Phụ lục 27; khi khác thì mục 19.3 là căn cứ);
  - DBR (Phụ lục 28);
  - căn cứ pháp lý (Phụ lục 29): văn bản thay đổi thì rà các mục ở cột "Quy tắc dựa vào", đổi quy tắc thì mở Q mới.
- docs/business/luong-nghiep-vu.md mô tả các luồng BF-01 → BF-18 theo từng bước: ai làm, làm gì, trạng thái đổi thế nào, căn cứ.
- Không sửa hai file trên trừ khi được yêu cầu rõ ràng. Phát hiện mâu thuẫn thì báo lại, không tự sửa.
- Không có file dẫn xuất. Các file tom-tat-nghiep-vu.md, quy-tac-nghiep-vu.md, business-rule.md, cau-hoi-ra-soat-van-hanh.md đã xoá ngày 2026-10-05; không tạo lại. Tham chiếu tới chúng trong checklist cũ chỉ là lịch sử.
- docs/phan-tich-yeu-cau.md đã bỏ ngày 2026-09-28. Các tham chiếu cũ "4.1", "4.2", "4.4", "DBR" trong spec tra theo bảng ánh xạ ở đầu Phụ lục 26.
- .specify/memory/constitution.md (2.1.0): nguyên tắc I–IX cho spec, X–XV là nguyên tắc kỹ thuật cho plan, tasks, mã; spec trích dẫn dạng "constitution I". Sửa constitution phải ghi Sync Impact Report và tăng phiên bản.

## Spec Kit và cấu trúc spec

- Lệnh dùng qua skill `/speckit-*` (specify, clarify, checklist, analyze, plan, tasks…); script hỗ trợ ở .specify/scripts/powershell. Phạm vi lệnh được dùng theo constitution IX và mục "Giai đoạn hiện tại".
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

- Viết spec bằng tiếng Việt; thuật ngữ theo mục 2.4 docs/business/nghiep-vu.md.
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
  4. Cập nhật docs/business/luong-nghiep-vu.md.
  5. Cập nhật bảng 24.4 nếu cần đối chiếu.
  6. Cập nhật mục "Điểm cần báo lại" của các spec liên quan.
- Không đưa diagram vào spec; trạng thái thể hiện bằng bảng.
- Sau mỗi bước spec: nếu yêu cầu còn mơ hồ thì gợi ý bước tiếp theo; nếu không thì ghi hoàn thành, tránh đào quá sâu làm lệch với các spec khác.

## Ghi chú công cụ

- Môi trường không có Python; dùng node cho script sửa file hàng loạt.
- Trong lệnh Bash (kể cả heredoc), chuỗi `\\` có thể bị rút thành `\`. Không dựng regex từ chuỗi có backslash; dùng regex literal hoặc `split`/`startsWith`. Script sửa file phải kiểm tra số lần khớp trước khi ghi.
