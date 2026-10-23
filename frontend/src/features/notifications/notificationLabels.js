// Nhãn tiếng Việt cho giá trị của hợp đồng api-009 (glossary mục 15.3; DESIGN.md 3.5). Giá trị chưa có nhãn hiện
// nguyên mã để không đoán nghĩa.

export const LEVEL_LABELS = { URGENT: 'Khẩn cấp', MEDIUM: 'Trung bình', LOW: 'Nhẹ' }

export const STATUS_LABELS = {
  HELD_FOR_QUIET_HOURS: 'Chờ hết giờ yên tĩnh',
  NOT_SENT: 'Không gửi',
  SENT: 'Đã gửi',
  DELIVERY_FAILED: 'Gửi thất bại',
  VIEWED: 'Đã xem',
  ACKNOWLEDGED: 'Đã xác nhận',
  UNREACHABLE: 'Không liên lạc được',
}

export const PART_LABELS = {
  GENERAL: 'Phần chung',
  HEALTH: 'Phần sức khỏe',
  COSTS: 'Chi phí',
  VISITS: 'Thăm',
  CONTRACT: 'Hợp đồng',
}

export const CHANNEL_LABELS = {
  IN_APP: 'Trong ứng dụng',
  FAMILY_PORTAL: 'Cổng người thân',
  SMS: 'Tin nhắn',
}

// Kết quả lần gửi (FR-018); rỗng là đang gửi
export const RESULT_LABELS = {
  SENT: 'Đã gửi',
  FAILED: 'Thất bại',
  CHANNEL_NOT_CONFIGURED: 'Kênh chưa cấu hình',
  NO_ADDRESS: 'Không có địa chỉ',
  SKIPPED_OFF_SHIFT: 'Bỏ qua, ngoài ca',
  BATCHED: 'Đã gộp vào tin tổng hợp',
  INVALID_CONTENT: 'Nội dung không hợp lệ',
}

// Căn cứ người nhận (RecipientResolver): tên nhóm, có thể kèm "SUBSTITUTE:" hay ":<chi tiết>"
const BASIS_LABELS = {
  AssignedNurse: 'Điều dưỡng phụ trách',
  AssignedStaff: 'Nhân viên được phân công',
  OnShiftStaff: 'Nhân viên đang có ca',
  ShiftLead: 'Người phụ trách ca',
  OnDutyDoctor: 'Bác sĩ trực',
  FloorManager: 'Trưởng tầng',
  TripLeader: 'Trưởng đoàn',
  DepartmentLead: 'Phụ trách bộ phận',
  PrimaryContact: 'Người liên hệ chính',
  Representative: 'Người đại diện',
  Resident: 'Người cao tuổi',
  NO_RECIPIENT_FALLBACK: 'Nhận thay vì không có người nhận',
  FacilityRole: 'Theo vai trò',
  Approvers: 'Người duyệt',
  Staff: 'Nhân viên gắn bản ghi',
  FamilyMember: 'Người thân gắn bản ghi',
  FamilyWithRight: 'Người thân có quyền',
}

export const ERROR_TYPE_LABELS = {
  REJECTED: 'Bị từ chối',
  MISSING_LEVEL: 'Thiếu mức',
  MISSING_INFORMATION_TYPE: 'Thiếu loại thông tin',
  NO_RECIPIENT: 'Không có người nhận',
  DELIVERY_FAILED: 'Gửi thất bại',
  INVALID_CONTENT: 'Nội dung kênh ngoài không hợp lệ',
}

const REASON_LABELS = {
  MISSING_SOURCE_RECORD: 'Thiếu bản ghi nguồn',
  MISSING_EVENT_KEY: 'Thiếu khóa sự kiện',
  MISSING_RECIPIENT_GROUPS: 'Thiếu nhóm người nhận',
  MISSING_CONTENT: 'Thiếu nội dung',
  REQUEST_BUILD_FAILED: 'Module nguồn không dựng được yêu cầu',
  INVALID_RESIDENT_GROUP: 'Gửi người cao tuổi sai mức hoặc có phần sức khỏe, chi phí',
}

export const CONTACT_STATUS_LABELS = {
  PENDING: 'Chờ liên hệ',
  CONTACTED: 'Đã liên hệ',
  UNREACHABLE: 'Không liên hệ được',
}

const RECIPIENT_TYPES = { STAFF: 'Nhân viên', FAMILY_MEMBER: 'Người thân', RESIDENT: 'Người cao tuổi' }

export const label = (labels, value) => (value ? (labels[value] ?? value) : '')

export function basisLabel(basis) {
  const substitute = basis.startsWith('SUBSTITUTE:')
  const [group, detail] = (substitute ? basis.slice('SUBSTITUTE:'.length) : basis).split(':')
  const text = BASIS_LABELS[group] ?? group
  const full = detail && group === 'FacilityRole' ? `${text} ${detail}` : text
  return substitute ? `Nhận thay cho: ${full}` : full
}

// "STAFF:301" → "Nhân viên 301" (tên hiện khi có API tên người nhận)
export function recipientLabel(recipient) {
  const [type, id] = (recipient ?? '').split(':')
  return RECIPIENT_TYPES[type] ? `${RECIPIENT_TYPES[type]} ${id}` : recipient
}

export const reasonLabel = (reason) => label(REASON_LABELS, reason)
