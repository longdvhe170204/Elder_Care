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

// Loại dữ liệu đã loại bỏ hiện theo module (tiền tố mã, research R-02 của 000), không hiện mã thô (016 T109)
const PURGED_MODULES = {
  M01: 'hồ sơ người cao tuổi',
  M02: 'tiếp nhận, lưu trú',
  M03: 'phòng, giường',
  M04: 'chăm sóc hằng ngày',
  M05: 'sự cố, cảnh báo',
  M06: 'sức khỏe',
  M07: 'thuốc',
  M08: 'dinh dưỡng',
  M09: 'nhân sự, ca trực',
  M11: 'chi phí',
}

function purgedNote(value) {
  const modules = [...new Set((value.purgedDataTypes ?? []).map((t) => PURGED_MODULES[t.split('.')[0]] ?? t))]
  return modules.length ? `${VALUE_LABELS.PURGED_BY_RETENTION} (${modules.join(', ')})` : VALUE_LABELS.PURGED_BY_RETENTION
}

export function labelNotes(value) {
  return (value.labels ?? [])
    .map((l) => {
      if (l === 'PENDING_REVIEW') return `${VALUE_LABELS[l]} (${value.pendingReviewCount})`
      if (l === 'PURGED_BY_RETENTION') return purgedNote(value)
      return VALUE_LABELS[l] ?? l
    })
    .join(', ')
}

// yyyy-MM-dd của một ngày theo giờ Việt Nam (DBR-25)
export function isoDate(date) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(date)
}

// Nhãn ô chia nhóm (breakdown.key) theo chỉ tiêu; chữ trùng màn của feature nguồn (giường 003, lưu trú 001, 004)
const BED_STATES = {
  EMPTY: 'Trống',
  OCCUPIED: 'Đang sử dụng',
  HELD: 'Đang giữ chỗ',
  AWAITING_READY: 'Chờ sẵn sàng',
  UNDER_MAINTENANCE: 'Đang bảo trì',
  SUSPENDED: 'Tạm ngừng sử dụng',
  RETIRED: 'Không sử dụng',
}
const HOLD_REASONS = { WAITING_RECORD: 'hồ sơ chờ', ABSENT_RESIDENT: 'người vắng' }
const KEY_LABELS = {
  // trạng thái hồ sơ chờ (004)
  WAITING: 'Đang chờ',
  CONTACTED: 'Đã liên hệ',
  ADMITTED: 'Đã tiếp nhận',
  DECLINED: 'Từ chối',
  WAITING_CANCELLED: 'Hủy chờ',
  // loại lưu trú
  LONG_TERM_RESIDENTIAL: 'Nội trú dài hạn',
  SHORT_TERM_RESIDENTIAL: 'Nội trú ngắn ngày',
  DAY_CARE: 'Bán trú',
  // mức lúc đo chỉ số (007)
  NORMAL: 'Bình thường',
  WARNING: 'Cảnh báo',
  DANGEROUS: 'Nguy hiểm',
  NO_THRESHOLD: 'Chưa có ngưỡng',
  // bàn giao (008), tỷ lệ phục vụ, giấy phép
  PREPARED: 'Đã lập',
  HAS_COMMENTS: 'Có ý kiến',
  DRAFT: 'Bản nháp',
  MET: 'Đạt',
  NOT_MET: 'Không đạt',
  NOT_CONFIGURED: 'Chưa cấu hình',
  PROFESSIONAL_LICENSE: 'Giấy phép hành nghề',
  TRAINING: 'Chứng chỉ đào tạo',
  // phiếu đối chiếu thuốc (006)
  OPEN: 'Mở',
  PENDING_CONFIRMATION: 'Chờ xác nhận',
  // mức cảnh báo, sự cố; nguồn nút; loại theo dõi (007)
  LOW: 'Nhẹ',
  MEDIUM: 'Trung bình',
  CRITICAL: 'Khẩn cấp',
  SOS: 'Nút SOS',
  EMERGENCY_ASSIST: 'Nút hỗ trợ khẩn cấp',
  POST_FALL: 'Theo dõi sau ngã',
  CONTACT: 'Theo dõi tiếp xúc',
  // dấu quá hạn của tài sản (003, 019)
  MAINTENANCE_OVERDUE: 'Quá hạn bảo trì',
  VEHICLE_INSPECTION_OVERDUE: 'Quá hạn đăng kiểm',
  INSURANCE_OVERDUE: 'Quá hạn bảo hiểm',
  // trạng thái hồ sơ 5.5, loại lưu trú chưa xác định, biến động (001, 004)
  IN_RESIDENCE: 'Đang lưu trú',
  TEMPORARILY_ABSENT: 'Tạm vắng',
  OFF_SITE_ACTIVITY: 'Hoạt động bên ngoài',
  HOSPITALIZED: 'Điều trị tại bệnh viện',
  UNDETERMINED: 'Chưa xác định',
  COMPLETE_ADMISSION: 'Hoàn tất tiếp nhận',
  CANCEL_ADMISSION: 'Hủy tiếp nhận',
  GRANT_TEMPORARY_ABSENCE: 'Bắt đầu tạm vắng',
  'RECORD_RETURN:TEMPORARILY_ABSENT': 'Kết thúc tạm vắng',
  'RECORD_RETURN:HOSPITALIZED': 'Về viện sau điều trị',
  TRANSFER_OUT: 'Chuyển điều trị tại bệnh viện',
  END_RESIDENCY: 'Kết thúc lưu trú',
  RECORD_DEATH: 'Qua đời',
  CARE_LEVEL_CHANGE: 'Đổi mức chăm sóc',
  // trạng thái công việc 8.3, mức quan trọng (005)
  NOT_YET_DUE: 'Chưa đến hạn',
  DUE: 'Đến hạn',
  OVERDUE: 'Quá hạn',
  COMPLETED: 'Hoàn thành',
  COMPLETED_LATE: 'Hoàn thành trễ',
  NOT_PERFORMED: 'Không thực hiện',
  CANCELLED: 'Hủy',
  IMPORTANT: 'Quan trọng',
  MANDATORY: 'Bắt buộc',
  COMMON: 'Việc chung của tầng',
  UNASSIGNED: 'Chưa có người thực hiện',
  // vai trò kiểm tra chất lượng (014)
  FLOOR_MANAGER: 'Trưởng tầng',
  SHIFT_LEAD: 'Người phụ trách ca',
}
const FLOOR_KEYED = ['BED.AWAITING_READY', 'BED.AWAITING_READY_MAX', 'BED.AWAITING_READY_OVERDUE', 'BED.OCCUPANTS']
const SHIFT_KEYED = ['MED.MISSED_RATE', 'MED.REFUSED_RATE']
const STAFF_KEYED = ['TASK.BY_STAFF', 'TASK.LATE_RECORDINGS', 'QC.PASS_RATE', 'SHIFT.WORKING_HOURS']
const TRIP_OVERDUE = { OVERDUE: 'Quá giờ về', OVERDUE_DOUBLE: 'Tới mốc gấp đôi' }

// Khóa của chỉ tiêu nguồn 010, 015, 017, 018, 019 (016 T008, T014, T020, T024)
const COST_TYPES = {
  RESIDENCY_FEE: 'Phí lưu trú',
  DAY_CARE_SESSION_FEE: 'Phí buổi bán trú',
  SERVICE: 'Dịch vụ',
  CONSUMABLE: 'Vật phẩm tiêu hao',
  ACTIVITY: 'Hoạt động',
  MEDICATION: 'Thuốc',
  FAMILY_OVERNIGHT_STAY: 'Người thân ở lại',
  FAMILY_MEAL: 'Suất ăn người thân',
  PURCHASE_ASSISTANCE: 'Mua hộ',
  OTHER: 'Khác',
}
const COST_ORIGINS = {
  EVENT_BASED: 'Theo sự kiện',
  MANUAL: 'Nhập tay',
  PURCHASE_ASSISTANCE: 'Mua hộ',
  ADJUSTMENT: 'Điều chỉnh',
}
const STATEMENT_STATUSES = {
  DRAFT: 'Bản nháp',
  CHECKED: 'Đã kiểm',
  APPROVED: 'Đã duyệt',
  FINALIZED: 'Đã chốt',
}
const PROCUREMENT_STATUSES = {
  DRAFT: 'Nháp',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  PENDING_RECONCILIATION: 'Chờ đối chiếu',
  DISCREPANCY: 'Có lệch',
  RECONCILED: 'Đã đối chiếu',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
}
const BALANCE_STATUSES = { RUNNING_LOW: 'Sắp hết tiền', IN_DEBT: 'Còn nợ' }
const REGISTRATION_TO_DO = {
  TO_CREATE: 'Cần lập',
  OVERDUE: 'Quá hạn',
  EXPIRED: 'Sắp hết hạn',
  TO_DEREGISTER: 'Cần xóa đăng ký',
}
const REQUEST_TYPES = { 'M09.SHIFT_SWAP': 'Đổi ca', 'M09.LEAVE_REQUEST': 'Nghỉ đột xuất' }
const REQUEST_STATUSES = {
  DRAFT: 'Nháp',
  AWAITING_RECIPIENT: 'Chờ người nhận',
  RECIPIENT_DECLINED: 'Người nhận từ chối',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  PARTIALLY_APPLIED: 'Áp dụng một phần',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Hết hạn',
}
const KEYED_BY_METRIC = {
  'COST.SUMMARY': COST_TYPES,
  'COST.EVENT_GENERATED_RATE': COST_ORIGINS,
  'COST.STATEMENTS': STATEMENT_STATUSES,
  'COST.PURCHASING': PROCUREMENT_STATUSES,
  'BALANCE.LOW_OR_OWING': BALANCE_STATUSES,
  'REGISTRATION.TO_DO': REGISTRATION_TO_DO,
}

// Chữ của một ô chia nhóm; khóa chưa biết giữ nguyên
export function breakdownLabel(metricCode, key, floors = []) {
  if (FLOOR_KEYED.includes(metricCode)) {
    const floor = floors.find((f) => String(f.id) === key)
    return floor ? floor.name : `Tầng/khu ${key}`
  }
  if (SHIFT_KEYED.includes(metricCode)) {
    return key === 'NO_SHIFT' ? 'Không xác định ca' : `Ca ${key}`
  }
  if (metricCode === 'ACT.OVERDUE_TRIPS') return TRIP_OVERDUE[key] ?? key
  if (KEYED_BY_METRIC[metricCode]) return KEYED_BY_METRIC[metricCode][key] ?? key
  if (metricCode === 'COST.POST_FINALIZATION_ADJUSTMENTS' && /^\d{4}-\d{2}$/.test(key)) {
    const [year, month] = key.split('-')
    return `Kỳ gốc ${month}/${year}`
  }
  if (metricCode === 'SHIFT.REQUESTS' && key.includes(':')) {
    const at = key.lastIndexOf(':')
    const [type, status] = [key.slice(0, at), key.slice(at + 1)]
    return `${REQUEST_TYPES[type] ?? type}, ${REQUEST_STATUSES[status] ?? status}`
  }
  if (metricCode === 'MEAL.LATE_TICKETS' || metricCode === 'MEAL.DISCREPANCY_TICKETS') return `Bữa ${key}`
  if (STAFF_KEYED.includes(metricCode) && /^\d+$/.test(key)) return `Nhân viên ${key}`
  if (metricCode === 'TASK.BY_STATUS' && key.includes(':SYSTEM:')) {
    const [status, rule] = key.split(':SYSTEM:')
    return `${KEY_LABELS[status] ?? status} (Hệ thống, ${rule})`
  }
  if (metricCode === 'BED.STATUS') {
    const [state, ...reason] = key.startsWith('HELD_') ? ['HELD', key.slice(5)] : [key]
    const label = BED_STATES[state] ?? key
    return reason.length ? `${label} (${HOLD_REASONS[reason[0]] ?? reason[0]})` : label
  }
  return KEY_LABELS[key] ?? key
}
