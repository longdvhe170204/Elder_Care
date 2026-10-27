# Đề nghị hợp đồng HTTP Phase 2 — 2026-10-27

**Đã duyệt trong phiên**: chủ dự án chọn phương án 1, duyệt bản dịch và bổ sung cả hai operation result-values. YAML đã cập nhật, FR_038_CatalogContractTest xanh. Đây là bằng chứng hợp đồng, **chưa phải endpoint đã được cài**; điểm quyền riêng còn ở phase2-permission-review.md.

Chỉ áp dụng danh mục T007/T007a. Giữ nguyên số operation hiện có, mã HTTP, header Idempotency-Key/If-Match và trường nghiệp vụ; không dịch các operation story chưa cài. Lý do: quy định AGENTS/CLAUDE yêu cầu duyệt bản dịch trước triển khai HTTP.

| Tên logic | Tên tiếng Anh đề nghị |
| --- | --- |
| /loai-cong-viec | /task-types |
| /loai-ly-do-khong-thuc-hien | /non-performance-reason-types |
| /danh-muc-gia-tri-ket-qua | /result-values |
| /{id}/ngung-hieu-luc | /{id}/deactivate |
| LoaiCongViec, LoaiLyDoKhongThucHien, GiaTriKetQua | TaskType, NonPerformanceReasonType, ResultValue |
| ten, trangThai, coDinh | name, status, fixed |
| nhom, chuyenMon, loaiKetQua, vaiTroMacDinh, mucMacDinh | group, professional, resultType, defaultRole, defaultImportance |
| khungTruocPhut, khungSauPhut, coTinhPhi, dichVuId | windowBeforeMinutes, windowAfterMinutes, billable, serviceId |
| gioiHanTren, loaiChiSo, chungChi | upperLimit, vitalSignType, certificates |
| loai, ma, nhomQuyTac | type, code, ruleGroup |
| HIEU_LUC, NGUNG_HIEU_LUC | ACTIVE, INACTIVE |
| THUONG, QUAN_TRONG, BAT_BUOC | NORMAL, IMPORTANT, MANDATORY |
| NGU_CHAT_LUONG, TAM_TRANG, HANH_VI | SLEEP_QUALITY, MOOD, BEHAVIOR |
| MAT_NGU, TIEU_CUC | INSOMNIA, NEGATIVE |
| lyDo (thân lệnh), LyDo (schema) | reason, Reason (dùng schema mới cho danh mục, giữ schema logic của các story khác) |
| LOAI_LY_DO_CO_DINH | REASON_TYPE_FIXED |

Enum nhóm công việc/loại kết quả và vai trò giữ đủ giá trị của data-model/glossary, dịch tương đương; không thêm vai trò. Field id giữ tên id; không tự thêm version vào body hợp đồng.

**Điểm lệch cần quyết định:** T007 nói tạo/sửa/ngừng giá trị kết quả, nhưng hợp đồng chỉ có GET/POST /danh-muc-gia-tri-ket-qua. Đề nghị bổ sung `PUT /result-values/{id}` (If-Match, Idempotency-Key, body ResultValue; 200/412) và `POST /result-values/{id}/deactivate` (Idempotency-Key, body reason; 200). Nếu không duyệt bổ sung, chỉ cài hai operation hiện có và ghi phần sửa/ngừng của T007 chưa hoàn tất.

Không thêm thư viện, không đổi API các module khác, không đổi endpoint testing đã duyệt Phase 1. Chữ ký Java mục 1.2–1.10a chỉ dịch tên tương đương và thêm kiểu record theo phần dữ liệu mô tả, không thêm phương thức dashboard ngoài hợp đồng.
