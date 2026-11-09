// Nhãn của yêu cầu đổi ca và nghỉ đột xuất (015; glossary mục 21 của 000). Mã trạng thái theo hợp đồng api-015.

export const SWAP_STATUS = {
  AWAITING_RECIPIENT: 'Chờ đồng ý',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPLIED: 'Đã áp dụng',
  PARTIALLY_APPLIED: 'Đã duyệt một phần',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
  RECIPIENT_DECLINED: 'Người nhận từ chối',
  EXPIRED: 'Hết hiệu lực',
}

export const SWAP_KIND = { MUTUAL: 'Đổi hai chiều', TAKE_OVER: 'Nhận thay' }

export const LEAVE_KIND = { BY_SHIFT: 'Nghỉ theo ca', BY_DAY: 'Nghỉ theo ngày' }

export const ITEM_RESULT = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  SPLIT_OFF: 'Tách khỏi yêu cầu',
  LEFT: 'Rời yêu cầu',
}

// Lệnh theo allowedCommands của backend → nhãn nút và có cần lý do không
export const SWAP_COMMANDS = {
  accept: { label: 'Đồng ý', reason: false },
  decline: { label: 'Từ chối', reason: true },
  'withdraw-acceptance': { label: 'Rút đồng ý', reason: false },
  cancel: { label: 'Rút yêu cầu', reason: true },
  approve: { label: 'Duyệt', reason: false },
  reject: { label: 'Từ chối duyệt', reason: true },
}

export const LEAVE_COMMANDS = {
  approve: { label: 'Duyệt', reason: false },
  reject: { label: 'Từ chối', reason: true },
  cancel: { label: 'Hủy yêu cầu', reason: true },
}

export const SCOPE_TYPES = { FLOOR: 'Tầng', AREA: 'Khu vực', FACILITY: 'Toàn viện' }

export const WARNING_TEXT = {
  CONTINUOUS_WORK_HOURS: 'Chuỗi giờ làm liên tục vượt ngưỡng',
}
