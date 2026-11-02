import '../../common/widgets/status_pill.dart';

/// Nhãn tiếng Việt của yêu cầu phê duyệt, cùng chữ với web (000 T098; `approvalStatus.js`).
const statusLabels = {
  'DRAFT': 'Nháp',
  'PENDING_APPROVAL': 'Chờ duyệt',
  'APPROVED': 'Đã duyệt',
  'APPLIED': 'Đã áp dụng',
  'APPLY_FAILED': 'Áp dụng không thành',
  'REJECTED': 'Từ chối',
  'CANCELLED': 'Đã hủy',
};

/// Loại yêu cầu do module sở hữu đăng ký; module sau thêm nhãn ở đây khi có loại mới.
const typeLabels = {'M15.CORRECTION': 'Đính chính'};

const actionLabels = {
  'REQUEST_CREATED': 'Lập yêu cầu',
  'REQUEST_DRAFT_EDITED': 'Sửa nháp',
  'REQUEST_SUBMITTED': 'Gửi duyệt',
  'REQUEST_APPROVED': 'Duyệt',
  'REQUEST_REJECTED': 'Từ chối',
  'REQUEST_CANCELLED': 'Hủy',
  'REQUEST_APPLIED': 'Áp dụng',
  'REQUEST_APPLY_FAILED': 'Áp dụng không thành',
  'REQUEST_APPLY_ERROR': 'Lỗi khi áp dụng, sẽ thử lại',
  'REQUEST_REMINDED': 'Nhắc người duyệt',
  'REQUEST_ESCALATED': 'Báo Quản lý viện',
  'REQUEST_EXTENDED_TRANSITION': 'Chuyển trạng thái riêng của loại yêu cầu',
  'REQUEST_CLOSED_FOR_FINAL_STATE': 'Đóng vì bản ghi liên quan đã kết thúc',
};

String statusLabel(String status) => statusLabels[status] ?? status;

String typeLabel(String type) => typeLabels[type] ?? type;

/// Tông theo `StatusBadge` của web (DESIGN.md 3.5).
StatusTone statusTone(String status) => switch (status) {
  'PENDING_APPROVAL' || 'APPLY_FAILED' => StatusTone.attention,
  'APPROVED' || 'APPLIED' => StatusTone.settled,
  _ => StatusTone.neutral,
};

String actorLabel(Map<String, dynamic>? actor) {
  if (actor == null) return '';
  if (actor['type'] == 'SCHEDULER') return 'Bộ lập lịch hệ thống';
  return (actor['fullName'] as String?) ?? 'Tài khoản ${actor['id']}';
}
