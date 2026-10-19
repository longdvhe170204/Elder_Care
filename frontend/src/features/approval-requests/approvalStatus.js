// Nhãn tiếng Việt của yêu cầu phê duyệt (000 T097; data-model mục 3; glossary mục 6.2).

export const STATUS_LABELS = {
  DRAFT: 'Nháp',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  APPLIED: 'Đã áp dụng',
  APPLY_FAILED: 'Áp dụng không thành',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
}

// Loại yêu cầu do module sở hữu đăng ký; module sau thêm nhãn ở đây khi có loại mới
export const TYPE_LABELS = {
  'M15.CORRECTION': 'Đính chính',
}

export const ACTION_LABELS = {
  REQUEST_CREATED: 'Lập yêu cầu',
  REQUEST_DRAFT_EDITED: 'Sửa nháp',
  REQUEST_SUBMITTED: 'Gửi duyệt',
  REQUEST_APPROVED: 'Duyệt',
  REQUEST_REJECTED: 'Từ chối',
  REQUEST_CANCELLED: 'Hủy',
  REQUEST_APPLIED: 'Áp dụng',
  REQUEST_APPLY_FAILED: 'Áp dụng không thành',
  REQUEST_APPLY_ERROR: 'Lỗi khi áp dụng, sẽ thử lại',
  REQUEST_REMINDED: 'Nhắc người duyệt',
  REQUEST_ESCALATED: 'Báo Quản lý viện',
  REQUEST_EXTENDED_TRANSITION: 'Chuyển trạng thái riêng của loại yêu cầu',
  REQUEST_CLOSED_FOR_FINAL_STATE: 'Đóng vì bản ghi liên quan đã kết thúc',
}

export function statusLabel(status) {
  return STATUS_LABELS[status] ?? status
}

export function typeLabel(type) {
  return TYPE_LABELS[type] ?? type
}

export function actorLabel(actor) {
  if (!actor) return ''
  if (actor.type === 'SCHEDULER') return 'Bộ lập lịch hệ thống'
  return actor.fullName ?? `Tài khoản ${actor.id}`
}
