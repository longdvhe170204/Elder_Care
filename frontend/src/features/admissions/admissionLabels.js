import { formatDateTime } from '@/common/dateTime'

// Nhãn tiếng Việt của enum feature 004 (api-004; glossary mục 10; thuật ngữ nghiep-vu.md mục 2.4).

export const RESIDENCE_TYPES = [
  ['LONG_TERM_RESIDENTIAL', 'Nội trú dài hạn'],
  ['SHORT_TERM_RESIDENTIAL', 'Nội trú ngắn ngày'],
  ['DAY_CARE', 'Bán trú'],
]

export const CARE_LEVELS = [
  ['BASIC', 'Cơ bản'],
  ['REGULAR', 'Thường xuyên'],
  ['SPECIAL', 'Đặc biệt'],
]

export const APPLICATION_STATUS = {
  IN_PROGRESS: 'Đang tiếp nhận',
  WAITING: 'Chờ giường',
  ADMITTED: 'Đã vào',
  CANCELLED: 'Đã hủy',
}

export const WAITING_STATUS = {
  WAITING: 'Đang chờ',
  CONTACTED: 'Đã liên hệ',
  ADMITTED: 'Đã vào',
  DECLINED: 'Từ chối',
  WAITING_CANCELLED: 'Đã hủy chờ',
}

export const CONTRACT_STATUS = {
  DRAFT: 'Nháp',
  PENDING_SIGNATURE: 'Chờ ký',
  ACTIVE: 'Hiệu lực',
  ENDED: 'Kết thúc',
  TERMINATED: 'Chấm dứt',
  CANCELLED: 'Đã hủy',
}

export const BILLING_PERIODS = [
  ['MONTH', 'Theo tháng'],
  ['DAY', 'Theo ngày'],
  ['SESSION', 'Theo buổi'],
]

export const CONDITION_STATUS = {
  MET: 'Đạt',
  NOT_MET: 'Chưa đạt',
  NOT_APPLICABLE: 'Không áp dụng',
  MET_BY_EXCEPTION: 'Đạt (ngoại lệ)',
  AUTOMATIC: 'Tự động',
}

export const OPINIONS = [
  ['AGREE', 'Đồng ý'],
  ['DISAGREE', 'Không đồng ý'],
  ['UNABLE_TO_EXPRESS', 'Không thể bày tỏ'],
]

export const OPINION_SOURCES = [
  ['RESIDENT', 'Người cao tuổi'],
  ['FAMILY_MEMBER', 'Người thân'],
  ['OBSERVING_STAFF', 'Nhân viên quan sát'],
]

export const PROGRESS_STEPS = {
  APPLICATION: 'Đăng ký',
  INFORMATION_COLLECTION: 'Thu thập thông tin',
  ASSESSMENT: 'Đánh giá',
  RESIDENCE_TYPE_DETERMINATION: 'Xác định loại lưu trú',
  CARE_DETERMINATION: 'Xác định chăm sóc',
  SERVICE_DETERMINATION: 'Xác định dịch vụ',
  CONDITION_CHECK: 'Kiểm tra điều kiện',
  ADMISSION: 'Hoàn tất tiếp nhận',
}

export const SERVICE_TYPES = [
  ['CARE', 'Chăm sóc'],
  ['RESIDENCY_FEE', 'Phí lưu trú'],
  ['DAY_CARE_SESSION', 'Buổi bán trú'],
  ['REHABILITATION', 'Phục hồi chức năng'],
  ['ACTIVITY', 'Hoạt động'],
  ['MEDICAL_ESCORT', 'Đưa đi khám'],
  ['INJECTION', 'Tiêm'],
  ['FAMILY_STAY', 'Người thân ở lại'],
  ['MEAL', 'Bữa ăn'],
  ['FACILITY_SUPPLIED_MEDICATION', 'Thuốc viện cấp'],
  ['OTHER', 'Khác'],
]

export const SERVICE_BILLING_PERIODS = [
  ['MONTH', 'Tháng'],
  ['DAY', 'Ngày'],
  ['SESSION', 'Buổi'],
  ['OCCURRENCE', 'Lần'],
  ['UNIT', 'Đơn vị'],
]

export const CHANGE_TYPES = [
  ['RESIDENCE_TYPE', 'Loại lưu trú'],
  ['CARE_LEVEL', 'Mức chăm sóc'],
  ['SERVICE', 'Dịch vụ'],
  ['DURATION', 'Thời hạn'],
  ['BED', 'Giường'],
  ['ARRIVAL_SCHEDULE', 'Lịch đến'],
]

export const ADDENDUM_STATUS = {
  PENDING_EFFECTIVE: 'Chờ hiệu lực',
  APPLIED: 'Đã áp dụng',
  NOT_APPLICABLE: 'Áp dụng không thành',
}

export const ABSENCE_TYPES = {
  HOME_VISIT: 'Về nhà',
  FAMILY_OUTING: 'Đi chơi với gia đình',
  SAME_DAY_MEDICAL_VISIT: 'Khám trong ngày',
  OTHER: 'Lý do khác',
  HOSPITAL: 'Chuyển viện',
}

export const ABSENCE_STATUS = { OPEN: 'Tạm vắng', CLOSED: 'Đã đóng' }

export const END_CASES = [
  ['CONTRACT_EXPIRED', 'Hết hợp đồng'],
  ['DISCHARGE', 'Xuất viện'],
  ['TRANSFER_TO_ANOTHER_FACILITY', 'Chuyển cơ sở'],
  ['CONTRACT_TERMINATION', 'Chấm dứt hợp đồng'],
]

export const ENDING_STATUS = { PREPARING: 'Đang chuẩn bị', COMPLETED: 'Đã kết thúc', CANCELLED: 'Đã hủy' }

export const RECIPIENT_TYPES = [
  ['FAMILY_MEMBER', 'Người thân'],
  ['RECEIVING_FACILITY', 'Cơ sở tiếp nhận'],
]

// Điều kiện (a) → (d), (h) có thể xin ngoại lệ; (e) không có ngoại lệ (FR-064)
export const EXCEPTION_CONDITIONS = [
  'END_A_COSTS',
  'END_B_BELONGINGS',
  'END_C_STORED_MEDICATION',
  'END_D_ALERTS_INCIDENTS',
  'END_H_SETTLEMENT',
]

export const POST_DEATH_ITEMS = {
  BELONGINGS: 'Đồ gửi',
  STORED_MEDICATION: 'Thuốc gia đình gửi',
  FINAL_PERIOD_COSTS: 'Chi phí kỳ cuối',
  ALERTS_INCIDENTS: 'Cảnh báo, sự cố',
  SETTLEMENT: 'Quyết toán số dư và tiền cọc',
}

export const POST_DEATH_STATUS = { NOT_COMPLETED: 'Chưa xong', COMPLETED: 'Đã xong', NOT_APPLICABLE: 'Không áp dụng' }

export function labelOf(options, code) {
  return options.find(([key]) => key === code)?.[1] ?? code
}

// Ô datetime-local là giờ Việt Nam (không đổi giờ theo mùa)
export function vietnamIso(local) {
  return local ? `${local}:00+07:00` : ''
}

export function money(value) {
  return value === undefined || value === null ? '—' : `${Number(value).toLocaleString('vi-VN')} đ`
}

export const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

export const newKey = () => ({ 'Idempotency-Key': crypto.randomUUID() })

export function absenceLabel(absence) {
  const type = ABSENCE_TYPES[absence.absenceType] ?? absence.absenceType
  return absence.status === 'OPEN'
    ? `${type}, dự kiến trở lại ${absence.expectedReturnAt ? formatDateTime(absence.expectedReturnAt) : 'chưa có'}`
    : `${type}, Đã đóng`
}
