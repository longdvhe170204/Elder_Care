<!--
Sync Impact Report
- Version change: (template, chưa ban hành) → 1.0.0
- Modified principles: toàn bộ placeholder được thay bằng 9 nguyên tắc:
  I. Nguồn gốc và truy vết
  II. Spec mô tả cái gì và vì sao, không mô tả cách làm
  III. Không mặc định CRUD
  IV. Trạng thái và quy tắc phải kiểm tra được
  V. Không cố định giá trị trong spec
  VI. Vai trò và quyền
  VII. Điểm chưa rõ
  VIII. Tiêu chí thành công
  IX. Phạm vi giai đoạn
- Added sections: "Tài liệu nguồn" (SECTION_2), "Quy trình làm spec" (SECTION_3), "Quản trị"
- Removed sections: không có
- Templates: không sửa template (theo phạm vi lệnh); spec-template, plan-template,
  tasks-template đọc constitution khi chạy
- Follow-up TODOs: không có
-->

# Hệ thống quản lý viện dưỡng lão – Constitution (giai đoạn phân tích)

## Core Principles

### I. Nguồn gốc và truy vết

- Quy tắc nghiệp vụ MUST nằm ở `docs/nghiep-vu.md`; spec của từng feature là nguồn gốc hành vi
  của feature đó.
- Mỗi yêu cầu chức năng trong spec MUST tham chiếu mã quy tắc (BR-Mxx-yy, DBR-xx) và use case
  (UC-xx) tương ứng; mã tham số (CFG-Mxx-yy) MUST được giữ nguyên khi trích dẫn.
- Khi nghiệp vụ thay đổi: MUST sửa tài liệu nghiệp vụ trước, rồi mới sửa spec.

Lý do: mọi hành vi của hệ thống phải truy ngược được về một quy tắc đã thống nhất.

### II. Spec mô tả cái gì và vì sao, không mô tả cách làm

- Spec MUST NOT nêu ngôn ngữ lập trình, framework, cơ sở dữ liệu, API, bảng hay màn hình cụ thể.
- Spec MUST viết cho người nghiệp vụ đọc hiểu được; thuật ngữ MUST theo mục 2.4 của
  `docs/nghiep-vu.md`.

Lý do: spec là thỏa thuận với người nghiệp vụ, không phải bản thiết kế kỹ thuật.

### III. Không mặc định CRUD

- Dữ liệu MUST được phân theo 3 nhóm ở mục 1.5 `docs/nghiep-vu.md`: (1) danh mục; (2) nghiệp vụ
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

- Actor và quyền trong spec MUST khớp danh sách actor (mục 4.1) và Permission Matrix (mục 4.4)
  trong `docs/phan-tich-yeu-cau.md`.

Lý do: tránh phát sinh vai trò hoặc quyền ngoài mô hình phân quyền đã thống nhất.

### VII. Điểm chưa rõ

- Điểm chưa chốt MUST được đánh dấu `[NEEDS CLARIFICATION]` và tham chiếu mã quyết định (Q-xx) ở
  mục 24 `docs/nghiep-vu.md`; MUST NOT tự đoán.

Lý do: giả định ngầm là nguồn lỗi nghiệp vụ khó phát hiện nhất.

### VIII. Tiêu chí thành công

- Mỗi feature MUST có tiêu chí thành công đo được, diễn đạt theo kết quả nghiệp vụ, không phụ
  thuộc công nghệ.

### IX. Phạm vi giai đoạn

- Giai đoạn hiện tại MUST chỉ gồm specify, clarify, checklist.
- Nguyên tắc kỹ thuật sẽ được bổ sung vào constitution khi chuyển sang giai đoạn thiết kế.

## Tài liệu nguồn

- `docs/nghiep-vu.md`: quy tắc nghiệp vụ (BR, DBR, CFG), phân loại dữ liệu (mục 1.5), thuật ngữ
  (mục 2.4), quyết định còn mở Q-xx (mục 24).
- `docs/phan-tich-yeu-cau.md`: actor (mục 4.1), use case, Permission Matrix (mục 4.4), ERD khái
  niệm.
- Hai tài liệu trên MUST NOT bị sửa khi làm spec trừ khi được yêu cầu rõ ràng; mâu thuẫn phát hiện
  được MUST được báo lại thay vì tự sửa.

## Quy trình làm spec

- Spec được viết bằng tiếng Việt, giữ nguyên các mã BR, DBR, UC, CFG, Q khi trích dẫn.
- Spec không chứa diagram ở giai đoạn này; trạng thái thể hiện bằng bảng.
- Trước khi coi spec là sẵn sàng, checklist MUST xác nhận spec tuân thủ các nguyên tắc I–IX.

## Governance

- Constitution ưu tiên hơn mọi nội dung trong spec; ngoại lệ MUST được ghi lý do và được duyệt.
- Sửa đổi constitution MUST ghi Sync Impact Report và tăng phiên bản theo semantic versioning:
  MAJOR khi bỏ hoặc định nghĩa lại nguyên tắc; MINOR khi thêm nguyên tắc/mục hoặc mở rộng đáng
  kể; PATCH khi làm rõ câu chữ.
- Mỗi lần clarify hoặc checklist MUST kiểm tra spec theo constitution hiện hành.

**Version**: 1.0.0 | **Ratified**: 2026-09-25 | **Last Amended**: 2026-09-25
