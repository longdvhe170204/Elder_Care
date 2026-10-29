// Nhãn hiển thị của 016 (glossary mục 22): nhóm chỉ tiêu FR-010, nhóm báo cáo 18.1 → 18.6, nhãn giá trị.

export const METRIC_GROUP_LABELS = {
  RESIDENTS: 'Người cao tuổi',
  WAITING_LIST: 'Danh sách chờ',
  BEDS: 'Phòng, giường',
  CARE_TASKS: 'Công việc',
  PER_STAFF: 'Theo nhân viên',
  BED_AWAITING_READY: 'Giường chờ sẵn sàng',
  MEAL_TICKETS: 'Phiếu bữa ăn',
  ACTIVITIES: 'Hoạt động',
  QUALITY_CHECKS: 'Kiểm tra chất lượng',
  HEALTH_EVENTS: 'Cảnh báo, sự cố',
  NON_HEALTH_INCIDENTS: 'Sự cố không thuộc sức khỏe',
  OVERDUE_REASSESSMENTS: 'Đánh giá lại quá hạn',
  MEDICATION_DOSES: 'Liều thuốc',
  MEDICATION_RECONCILIATIONS: 'Đối chiếu thuốc',
  CONTAINMENT_ZONES: 'Khu đang khoanh vùng',
  HANDOVERS: 'Bàn giao chưa xác nhận',
  STAFFING_RATIO: 'Tỷ lệ phục vụ ca hiện tại',
  FACILITY_LICENSES: 'Giấy phép cơ sở',
  STAFF_LICENSES: 'Giấy phép hành nghề, đào tạo',
  INCURRED_COSTS: 'Chi phí',
  PURCHASING: 'Mua sắm',
  BALANCES: 'Số dư thấp, còn nợ',
  RESIDENCE_REGISTRATION: 'Khai báo tạm trú',
  ASSETS: 'Tài sản quá hạn',
  CRITICAL_FLAGS: 'Dấu nguy kịch',
  STAFFING_SHIFTS: 'Nhân sự, ca trực',
}

const QL = 'NURSING_HOME_MANAGER'
const TT = 'FLOOR_MANAGER'
const BS = 'DOCTOR'
const DD = 'NURSING_STAFF'
const HC = 'OFFICE_STAFF'
const KT = 'ACCOUNTANT'

// Nhóm báo cáo (backend ReportGroup). `roles` suy từ bảng FR-010: vai trò thấy ít nhất một nhóm chỉ tiêu của báo cáo.
// Chỉ để hiện tab; backend vẫn kiểm và trả 403.
export const REPORT_GROUPS = [
  { value: 'RESIDENTS', label: 'Người cao tuổi', roles: [QL, TT, BS, DD, HC] },
  { value: 'CARE', label: 'Chăm sóc', roles: [QL, TT, BS, DD] },
  { value: 'QUALITY', label: 'Chất lượng', roles: [QL, TT] },
  { value: 'HEALTH_MEDICATION', label: 'Sức khỏe, thuốc', roles: [QL, TT, BS, DD, HC] },
  { value: 'INCURRED_COSTS', label: 'Chi phí', roles: [QL, HC, KT] },
  { value: 'STAFFING_SHIFTS', label: 'Nhân sự, ca', roles: [QL, TT] },
]

export const REPORT_GROUP_LABELS = Object.fromEntries(REPORT_GROUPS.map((g) => [g.value, g.label]))

export const VALUE_LABELS = {
  NO_DATA: 'không có dữ liệu',
  NOT_CONFIGURED: 'chưa cấu hình',
  DAY_NOT_ENDED: 'ngày chưa kết thúc',
  PENDING_REVIEW: 'có bản ghi chờ xem lại',
  PURGED_BY_RETENTION: 'số liệu không đầy đủ do đã loại bỏ theo lưu trữ',
  PROVISIONAL: 'tạm tính',
  WITHIN_YOUR_SCOPE: 'trong phạm vi của bạn',
}

const NUMBER = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 })

// Mẫu số 0 thì giá trị rỗng: hiện "—" kèm "không có dữ liệu" (FR-008)
export function formatValue(value) {
  return value === null || value === undefined ? '—' : NUMBER.format(value)
}

export function labelNotes(value) {
  return (value.labels ?? [])
    .map((l) => (l === 'PENDING_REVIEW' ? `${VALUE_LABELS[l]} (${value.pendingReviewCount})` : VALUE_LABELS[l] ?? l))
    .join(', ')
}

// yyyy-MM-dd của một ngày theo giờ Việt Nam (DBR-25)
export function isoDate(date) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(date)
}
