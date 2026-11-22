-- Q-414 (2026-11-22): kế hoạch chăm sóc không còn bước duyệt. Phiên bản đang Chờ duyệt trở về Nháp để Bác sĩ ban hành;
-- không đổi enum trạng thái (dữ liệu cũ, nhật ký vẫn đọc được). Cú pháp dùng được ở MySQL 8.0 và 8.4.
UPDATE care_plan
SET status = 'DRAFT', submitted_at = NULL, reminded_at = NULL, escalated_at = NULL, version = version + 1
WHERE status = 'PENDING_APPROVAL';
