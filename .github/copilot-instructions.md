# Hướng dẫn cho GitHub Copilot

Luật chung của dự án ở [AGENTS.md](../AGENTS.md). Đọc AGENTS.md, CLAUDE.md, docs/team-plan.md và README của thư mục đang sửa (frontend/README.md, mobile/README.md) trước khi gợi ý mã. Tóm tắt:

- Nghiệp vụ chỉ lấy từ docs/business/nghiep-vu.md; không đoán, không thêm quy tắc.
- Chỉ sửa phần của người đang làm (docs/team-plan.md, .github/CODEOWNERS); module khác chỉ gọi qua gói `api`.
- Test viết trước, tên có mã quy tắc; hợp đồng `specs/*/contracts/api-*.yaml` là luật, không thêm bớt endpoint, trường.
- Tên trong mã tiếng Anh theo specs/000-shared-foundation/glossary.md; chữ trên màn, tài liệu tiếng Việt.
- Không sửa frontend/src/api/ (sinh tự động), không ghi mật khẩu vào mã.
- Việc người khác đang chờ ở docs/team-plan.md mục 8: khi được hỏi việc cần làm, báo các dòng có tên người dùng ở cột "Ai làm"; cần người khác làm gì thì thêm dòng vào bảng đó.
