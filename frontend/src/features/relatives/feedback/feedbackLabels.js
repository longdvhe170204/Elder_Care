// Nhãn của phản hồi (012 US6; glossary 18.3). Dùng chung cho màn nhân viên và cổng người thân.

export const FEEDBACK_TYPES = [
  ['COMPLAINT', 'Khiếu nại'],
  ['SUGGESTION', 'Góp ý'],
  ['QUESTION', 'Hỏi đáp'],
]

export const FEEDBACK_GROUPS = [
  ['CARE', 'Chăm sóc, sinh hoạt'],
  ['HEALTH_MEDICATION', 'Sức khỏe, thuốc'],
  ['EATING', 'Ăn uống'],
  ['COSTS_CONTRACT', 'Chi phí, hợp đồng'],
  ['OTHER', 'Khác'],
]

export const FEEDBACK_STATUS = {
  NEW: 'Mới',
  IN_PROGRESS: 'Đang xử lý',
  REPLIED: 'Đã phản hồi',
  REOPENED: 'Mở lại',
  CLOSED: 'Đóng',
}

export const PRIORITY = { HIGH: 'Cao', NORMAL: 'Thường' }

// Vai trò người nhận giao lại (FR-060; api-012 AssignableStaff)
export const ASSIGNEE_ROLES = { FLOOR_MANAGER: 'Trưởng tầng', NURSING_STAFF: 'Điều dưỡng', OFFICE_STAFF: 'Hành chính' }

export const ITEM_LABELS = {
  CONTENT: 'Nội dung',
  REPLY: 'Viện trả lời',
  HANDLING_DIRECTION: 'Hướng xử lý',
  NOTE: 'Ghi chú',
  REASSIGNMENT: 'Giao lại',
  PRIORITY_CHANGE: 'Đổi mức',
  REOPEN: 'Mở lại',
  TYPE_CHANGE: 'Đổi loại',
}

// Mong muốn chỉ người cao tuổi gửi (FR-058b): không có trong form của người thân, chỉ có ở nhãn và form ghi thay
export const WISH = ['WISH', 'Mong muốn']

export const WISH_TOPICS = [
  ['VISITORS_PICKERS', 'Người đến thăm, người đón'],
  ['OTHER', 'Việc khác'],
]

const TYPE_MAP = Object.fromEntries([...FEEDBACK_TYPES, WISH])
const GROUP_MAP = Object.fromEntries(FEEDBACK_GROUPS)

export function typeLabel(code) {
  return TYPE_MAP[code] ?? code
}

export function groupLabel(code) {
  return GROUP_MAP[code] ?? code
}
