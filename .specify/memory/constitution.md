<!--
Sync Impact Report (2026-10-16, lượt 2)
- Phiên bản: 3.0.0 → 3.1.0 (MINOR: đổi ngôn ngữ mặc định của web client trong nguyên tắc X; số client và kiến trúc giữ nguyên).
- Nguyên tắc sửa:
  - X. Kiến trúc: web React với JavaScript thay cho TypeScript, theo dự án `frontend/` chủ dự án đã khởi tạo và commit (b9d88d7):
    React 19, Vite, Tailwind CSS v4, shadcn/ui. Chủ dự án chốt không chuyển sang TypeScript.
- Mục thêm, bỏ: không.
- Cần cập nhật theo:
  - ✅ specs/000-shared-foundation/research.md (R-01), plan.md (Technical Context, Project Structure), tasks.md (T005, T008), glossary.md.
  - ✅ "Language/Version" của specs/*/plan.md; đường dẫn `.tsx`, `.ts` của web trong specs/*/tasks.md, quickstart.md thành `.jsx`, `.js`.
  - ✅ CLAUDE.md.
-->

<!--
Sync Impact Report (2026-10-16)
- Phiên bản: 2.1.0 → 3.0.0 (MAJOR: định nghĩa lại nguyên tắc X, số client từ ba thành hai).
- Nguyên tắc sửa:
  - X. Kiến trúc: hai client (web React với TypeScript; một app Flutter chung cho nhân viên và người thân, Q-346);
    phiên bản mặc định Java 25, Spring Boot 4.x, MySQL 8.0 theo môi trường của chủ dự án; tên định danh trong mã bằng tiếng Anh.
  - XIV. Ngoại tuyến và thông báo: "hai app nhận thông báo đẩy" thành "app di động nhận thông báo đẩy".
- Mục thêm, bỏ: không.
- Cần cập nhật theo:
  - ✅ specs/000-shared-foundation/research.md (R-01, R-02, R-03, R-14), plan.md (Technical Context, Project Structure).
  - ✅ đường dẫn client trong specs/*/plan.md, tasks.md (web/ → frontend/, app-nhan-vien/, app-nguoi-than/ → mobile/).
  - ✅ CLAUDE.md.
  - ⚠ specs/*/contracts/api-*.yaml, data-model.md: tên vẫn là tên logic tiếng Việt; dịch theo bảng thuật ngữ khi triển khai từng feature (research R-03).
-->

# Hệ thống quản lý viện dưỡng lão – Constitution

## Core Principles

### I. Nguồn gốc và truy vết

- Quy tắc nghiệp vụ MUST nằm ở `docs/business/nghiep-vu.md`; spec của từng feature là nguồn gốc hành vi
  của feature đó.
- Mỗi yêu cầu chức năng trong spec MUST tham chiếu mã quy tắc (BR-Mxx-yy, DBR-xx) và use case
  (UC-xx) tương ứng; mã tham số (CFG-Mxx-yy) MUST được giữ nguyên khi trích dẫn.
- Khi nghiệp vụ thay đổi: MUST sửa tài liệu nghiệp vụ trước, rồi mới sửa spec.

Lý do: mọi hành vi của hệ thống phải truy ngược được về một quy tắc đã thống nhất.

### II. Spec mô tả cái gì và vì sao, không mô tả cách làm

- Spec MUST NOT nêu ngôn ngữ lập trình, framework, cơ sở dữ liệu, API, bảng hay màn hình cụ thể.
- Spec MUST viết cho người nghiệp vụ đọc hiểu được; thuật ngữ MUST theo mục 2.4 của
  `docs/business/nghiep-vu.md`.

Lý do: spec là thỏa thuận với người nghiệp vụ, không phải bản thiết kế kỹ thuật. Quyết định công
nghệ nằm ở plan theo các nguyên tắc X–XV.

### III. Không mặc định CRUD

- Dữ liệu MUST được phân theo 3 nhóm ở mục 1.5 `docs/business/nghiep-vu.md`: (1) danh mục; (2) nghiệp vụ
  có trạng thái; (3) ghi nhận đã xác nhận.
- Với nhóm 2, spec MUST mô tả các lệnh nghiệp vụ và điều kiện của chúng; MUST NOT mô tả thao tác
  "sửa trạng thái".
- Với nhóm 3, spec MUST chỉ cho phép ghi thêm và đính chính; MUST NOT cho phép sửa hay xóa.

Lý do: hồ sơ chăm sóc người cao tuổi cần toàn vẹn và truy vết được, không thể ghi đè tùy ý.

### IV. Trạng thái và quy tắc phải kiểm tra được

- Mỗi thực thể có vòng đời MUST liệt kê trạng thái, chuyển hợp lệ, điều kiện và tác động, thể hiện
  bằng bảng.
- Mỗi quy tắc MUST có ít nhất một kịch bản chấp nhận dạng Given/When/Then.

Lý do: yêu cầu không kiểm tra được thì không nghiệm thu được.

### V. Không cố định giá trị trong spec

- Ngưỡng, thời hạn, tỷ lệ MUST được gọi bằng mã CFG kèm giá trị mặc định; MUST NOT viết như hằng
  số.

Lý do: các giá trị này do viện cấu hình và có thể thay đổi mà không đổi nghiệp vụ.

### VI. Vai trò và quyền

- Actor và quyền trong spec MUST khớp danh sách actor (Phụ lục 26.1), ma trận quyền (Phụ lục 27)
  và mục 19.3 của `docs/business/nghiep-vu.md`; khi Phụ lục 27 khác 19.3, 19.3 là căn cứ.

Lý do: tránh phát sinh vai trò hoặc quyền ngoài mô hình phân quyền đã thống nhất.

### VII. Điểm chưa rõ

- Điểm chưa chốt MUST tham chiếu mã quyết định (Q-xx) ở mục 24 `docs/business/nghiep-vu.md`; MUST NOT tự
  đoán.
- Quyết định còn mở đã có giá trị Mặc định ở mục 24.1: spec MUST ghi "theo mặc định Q-xx" và viết
  yêu cầu, tiêu chí kiểm thử theo giá trị đó (Q-204).
- Điểm chưa có Mặc định, hoặc chưa có mã Q: spec MUST đánh dấu `[NEEDS CLARIFICATION]` kèm mã Q-xx
  (hoặc ghi rõ cần mã Q mới).

Lý do: giả định ngầm là nguồn lỗi nghiệp vụ khó phát hiện nhất.

### VIII. Tiêu chí thành công

- Mỗi feature MUST có tiêu chí thành công đo được, diễn đạt theo kết quả nghiệp vụ, không phụ
  thuộc công nghệ.

### IX. Phạm vi giai đoạn

- Giai đoạn hiện tại và các lệnh được dùng trong giai đoạn đó MUST theo mục "Giai đoạn hiện tại"
  của `CLAUDE.md`; chỉ chủ dự án được chuyển giai đoạn.
- Implement (viết mã) MUST NOT bắt đầu cho tới khi chủ dự án mở giai đoạn triển khai ở mục đó.
- Plan, tasks MUST NOT làm thay đổi nghiệp vụ: phát hiện thiếu sót nghiệp vụ khi thiết kế thì sửa
  `docs/business/nghiep-vu.md` và spec trước (I), không tự giải quyết trong plan.

Lý do: thiết kế dựa trên nghiệp vụ đã chốt; viết mã khi thiết kế chưa ổn định gây làm lại.

### X. Kiến trúc

- Hệ thống MUST gồm một backend Spring Boot cung cấp RESTful API (JSON, có phiên bản, tiền tố
  `/api/v1`) và hai client dùng chung API đó:
  - web React với JavaScript: mọi chức năng, gồm quản trị và cổng người thân trên trình duyệt;
  - một app Flutter chung cho nhân viên và người thân, chức năng theo loại tài khoản (Q-305, Q-346).
- Cơ sở dữ liệu MUST là MySQL.
- Backend MUST là một khối (không tách microservice), chia gói theo module nghiệp vụ M01–M15 của
  `docs/business/nghiep-vu.md`; module gọi nhau qua giao diện của module, không truy cập trực tiếp dữ liệu
  của module khác.
- Phiên bản cụ thể (mặc định Java 25, Spring Boot 4.x, MySQL 8.0, React với JavaScript, Flutter
  bản stable) MUST được ghi ở plan.
- Tên định danh trong mã (gói, lớp, hàm, biến, file mã, bảng, cột, đường dẫn API, trường JSON, mã lỗi)
  MUST bằng tiếng Anh, dịch từ tên logic tiếng Việt của plan, data-model, contracts theo một bảng
  thuật ngữ chung; mã nghiệp vụ (BR, FR, CFG, Q, UC…) giữ nguyên.
- Plan MUST nêu cách đáp ứng từng yêu cầu phi chức năng ở mục 22 `docs/business/nghiep-vu.md` (NFR) mà
  feature chịu ảnh hưởng, gồm hiệu năng, thiết bị, thời gian.

Lý do: một backend và một nguồn quy tắc cho ba client; một khối chia module vừa quy mô đồ án mà
vẫn giữ ranh giới feature của spec.

### XI. Nghiệp vụ ở backend

- Mọi kiểm tra BR, điều kiện chuyển trạng thái và quyền MUST do backend thực hiện, gồm đủ ba lớp
  của BR-M15-01 (quyền vai trò ∩ phạm vi dữ liệu ∩ điều kiện pháp lý); kiểm tra ở client chỉ để hỗ
  trợ người dùng, MUST NOT là nơi kiểm tra duy nhất.
- Mỗi lệnh nghiệp vụ trên dữ liệu nhóm 2 MUST là một endpoint hành động riêng
  (ví dụ `POST .../{id}/hoan-tat-tiep-nhan`); MUST NOT có endpoint cho sửa trạng thái trực tiếp
  (III).

Lý do: ba client không được cho ra ba cách hiểu quy tắc khác nhau; quyền kiểm tra ở client có thể
bị vượt qua.

### XII. Dữ liệu

- Bản ghi nhóm 3 MUST chỉ được thêm; đính chính là bản ghi mới trỏ tới bản gốc; MUST NOT có
  UPDATE hay DELETE trên bản ghi đã xác nhận.
- Mọi thay đổi MUST được ghi nhật ký với người thực hiện, thời điểm, giá trị trước/sau và lý do
  (19.4).
- Thời gian MUST theo NFR-09 và DBR-25 (múi giờ Asia/Ho_Chi_Minh, đồng hồ máy chủ, trừ bản ghi
  ngoại tuyến).
- Mỗi DBR (Phụ lục 28) MUST được bảo đảm bằng ràng buộc cơ sở dữ liệu hoặc kiểm tra ở backend, và
  plan MUST chỉ rõ cách bảo đảm.

Lý do: toàn vẹn dữ liệu là yêu cầu nghiệp vụ (III), không phải lựa chọn kỹ thuật.

### XIII. Tham số

- Mọi giá trị CFG (Phụ lục 25) MUST được lưu trong cấu hình có lịch sử và đọc lúc chạy; MUST NOT
  viết cứng trong code (V, BR-M15-04).

Lý do: viện đổi tham số qua màn hình cấu hình mà không cần sửa mã.

### XIV. Ngoại tuyến và thông báo

- App nhân viên MUST ghi tạm khi mất kết nối và đồng bộ sau theo Q-01, BR-M15-20, CFG-M15-08;
  thao tác khẩn cấp MUST bắt buộc trực tuyến.
- Mọi thông báo MUST đi qua dịch vụ thông báo chung (spec 009) với mức do module nguồn nêu;
  app di động nhận thông báo đẩy (Q-305, Q-346).

Lý do: chăm sóc tại giường không được dừng vì mất mạng; thông báo phân tán thì không kiểm soát được
mức và người nhận.

### XV. Truy vết và kiểm thử

- Plan, tasks và mã MUST tham chiếu FR, BR tương ứng.
- Mỗi BR MUST có ít nhất một kiểm thử tự động ở tầng thực thi quy tắc đó, dựng từ kịch bản
  Given/When/Then của spec; thường là backend (XI). BR có phần thực hiện ở client (ví dụ báo kết
  quả đồng bộ ngoại tuyến, BR-M15-20) MUST có thêm kiểm thử ở client đó.

Lý do: giữ chuỗi truy vết từ quy tắc tới mã (I) và nghiệm thu được (IV).

## Tài liệu nguồn

- `docs/business/nghiep-vu.md`: quy tắc nghiệp vụ (BR, CFG), phân loại dữ liệu (mục 1.5), thuật ngữ
  (mục 2.4), quyết định Q-xx (mục 24), actor và use case (Phụ lục 26), ma trận quyền (Phụ lục 27),
  quy tắc dữ liệu DBR (Phụ lục 28), căn cứ pháp lý (Phụ lục 29).
- `docs/business/luong-nghiep-vu.md`: luồng nghiệp vụ chính BF-xx, nguồn duy nhất của mã BF.
- Hai tài liệu trên MUST NOT bị sửa khi làm spec hay plan trừ khi được yêu cầu rõ ràng; mâu thuẫn
  phát hiện được MUST được báo lại thay vì tự sửa.
- `docs/phan-tich-yeu-cau.md` đã bỏ ngày 2026-09-28; nội dung còn dùng được chuyển vào Phụ lục
  26 → 28 của `docs/business/nghiep-vu.md`.

## Quy trình làm spec và thiết kế

- Spec được viết bằng tiếng Việt, giữ nguyên các mã BR, DBR, UC, CFG, Q khi trích dẫn.
- Spec không chứa diagram; trạng thái thể hiện bằng bảng.
- Trước khi coi spec là sẵn sàng, checklist MUST xác nhận spec tuân thủ các nguyên tắc I–IX.
- Plan và tasks được làm theo thứ tự phụ thuộc giữa feature (000 trước, rồi các feature mà feature
  khác gọi tới); plan MUST xác nhận tuân thủ X–XV và nêu ngoại lệ kèm lý do.

## Governance

- Constitution ưu tiên hơn mọi nội dung trong spec và plan; ngoại lệ MUST được ghi lý do và được
  duyệt.
- Sửa đổi constitution MUST ghi Sync Impact Report và tăng phiên bản theo semantic versioning:
  MAJOR khi bỏ hoặc định nghĩa lại nguyên tắc; MINOR khi thêm nguyên tắc/mục hoặc mở rộng đáng
  kể; PATCH khi làm rõ câu chữ.
- Mỗi lần clarify, checklist, plan hoặc analyze MUST kiểm tra theo constitution hiện hành.

**Version**: 3.1.0 | **Ratified**: 2026-09-25 | **Last Amended**: 2026-10-16
