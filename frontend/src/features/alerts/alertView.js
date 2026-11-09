// Nhãn và nhóm của cảnh báo theo api-007 `Alert` (spec 007 FR-027 → FR-036; DESIGN.md 3.5). Hàm thuần, không JSX.

export const LEVEL_LABEL = { CRITICAL: 'Khẩn cấp', MEDIUM: 'Trung bình', LOW: 'Nhẹ' }
export const LEVEL_ORDER = ['CRITICAL', 'MEDIUM', 'LOW']

export const STATUS_LABEL = {
  NEW: 'Mới',
  ESCALATED: 'Leo thang',
  RECEIVED: 'Đã tiếp nhận',
  IN_PROGRESS: 'Đang xử lý',
  CLOSED: 'Đã đóng',
  CONVERTED_TO_INCIDENT: 'Chuyển sự cố',
}
export const OPEN_STATUSES = ['NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS']

// Loại cảnh báo do hệ thống tạo (mã loại trong api-007); cảnh báo thủ công mang mã loại sự cố đã chọn
const TYPE_LABEL = {
  CHI_SO: 'Chỉ số vượt ngưỡng',
  WEIGHT_LOSS: 'Sụt cân',
  MISSED_MEASUREMENT: 'Bỏ lỡ lần đo theo lịch',
  MEDICATION_REFUSAL: 'Từ chối thuốc liên tiếp',
  PROLONGED_INSOMNIA: 'Mất ngủ kéo dài',
  LOW_WATER_INTAKE: 'Uống nước chưa đủ',
  PROLONGED_POOR_EATING: 'Ăn kém kéo dài',
  PROLONGED_NEGATIVE_MOOD: 'Tâm trạng tiêu cực kéo dài',
  OVERDUE_CARE_TASK: 'Công việc Bắt buộc quá hạn',
  MISSING_ON_RETURN: 'Thiếu người khi về',
  BO_LO_LIEU: 'Bỏ lỡ liều thuốc',
  XUNG_DOT_DI_UNG: 'Xung đột dị ứng thuốc',
  PHAN_UNG_THUOC: 'Phản ứng thuốc',
  POSSIBLE_DUPLICATE_DOSE: 'Nghi dùng trùng liều',
  CHUA_DOI_CHIEU: 'Thuốc chưa đối chiếu',
  DIET_ALLERGY_CONFLICT: 'Xung đột dị ứng với chế độ ăn',
  CRITICAL_CONDITION: 'Tình trạng nguy kịch',
  RESIDENT_HELP_REQUEST: 'Người cao tuổi bấm nút SOS',
  STAFF_ASSIST_REQUEST: 'Nhân viên bấm nút hỗ trợ khẩn cấp',
}

const ORIGIN_LABEL = { SYSTEM: 'Hệ thống', MANUAL: 'Nhân viên tạo', SOS: 'Nút SOS', ASSIST_BUTTON: 'Nút hỗ trợ' }

export function typeLabel(type) {
  return TYPE_LABEL[type] ?? 'Cảnh báo'
}

export function originLabel(origin) {
  return ORIGIN_LABEL[origin] ?? 'Hệ thống'
}

export function isOpen(alert) {
  return OPEN_STATUSES.includes(alert.status)
}

// Leo thang tối đa là việc gấp (DESIGN.md 3.5); leo thang thường ghi cấp
export function statusLabel(alert) {
  if (alert.status === 'ESCALATED') {
    return alert.maxEscalationReached ? 'Đã leo thang tối đa' : `Leo thang cấp ${alert.escalationLevel}`
  }
  return STATUS_LABEL[alert.status] ?? alert.status
}

export function statusTone(alert) {
  if (alert.status === 'ESCALATED' && alert.maxEscalationReached) return 'urgent'
  if (['NEW', 'ESCALATED'].includes(alert.status)) return 'attention'
  if (alert.status === 'CLOSED') return 'settled'
  return 'neutral'
}

export function isUrgent(alert) {
  return isOpen(alert) && (alert.level === 'CRITICAL' || alert.maxEscalationReached)
}

// Khẩn cấp trước, rồi leo thang tối đa, rồi mới nhất trước (DESIGN.md 7.2 alert-card)
export function sortAlerts(alerts) {
  return alerts.toSorted(
    (a, b) =>
      LEVEL_ORDER.indexOf(a.level) - LEVEL_ORDER.indexOf(b.level) ||
      Number(!!b.maxEscalationReached) - Number(!!a.maxEscalationReached) ||
      String(b.latestAt).localeCompare(String(a.latestAt)),
  )
}

export function residentName(profile) {
  return profile?.personalInfo?.fullName ?? 'Người cao tuổi'
}
