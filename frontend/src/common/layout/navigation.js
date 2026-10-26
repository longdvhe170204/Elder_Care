// Menu theo quyền. Mỗi mục ghi dòng của Phụ lục 27 (docs/business/nghiep-vu.md) làm căn cứ: `functions` là mã
// chức năng của dòng đó (002 `Function`), `roles` là các vai trò có quyền khác "—" ở dòng đó. Có phiên thật thì mục
// hiện khi tài khoản có ít nhất một quyền của chức năng ở GET /me/permissions (hợp mọi vai trò, sau thu hẹp; 002
// FR-042); mục không có `functions` (Yêu cầu phê duyệt, Thông báo) hiện cho mọi tài khoản nhân viên. Bản mẫu không có
// backend thì lọc theo `roles`. Mục chưa có màn (không có trong `built`) thì ẩn, có màn thì tự hiện. Chỉ để hiển thị,
// backend vẫn kiểm mọi lệnh.

export const ROLES = [
  { code: 'NURSING_HOME_MANAGER', label: 'Quản lý viện' },
  { code: 'FLOOR_MANAGER', label: 'Trưởng tầng' },
  { code: 'DOCTOR', label: 'Bác sĩ' },
  { code: 'NURSING_STAFF', label: 'Điều dưỡng' },
  { code: 'CAREGIVER', label: 'Nhân viên chăm sóc' },
  { code: 'KITCHEN_STAFF', label: 'Nhân viên bếp' },
  { code: 'OFFICE_STAFF', label: 'Hành chính' },
  { code: 'ACCOUNTANT', label: 'Kế toán' },
  { code: 'FAMILY_MEMBER', label: 'Người thân' },
]

const QL = 'NURSING_HOME_MANAGER'
const TT = 'FLOOR_MANAGER'
const BS = 'DOCTOR'
const DD = 'NURSING_STAFF'
const CS = 'CAREGIVER'
const BEP = 'KITCHEN_STAFF'
const HC = 'OFFICE_STAFF'
const KT = 'ACCOUNTANT'
const NT = 'FAMILY_MEMBER'

export const NAV_GROUPS = [
  {
    label: null,
    items: [
      // Dòng "Dashboard, báo cáo"
      { key: 'dashboard', label: 'Tổng quan', icon: 'LayoutDashboard', functions: ['DASHBOARD_REPORT'], roles: [QL, TT, BS, DD, HC, KT] },
      // Dòng "Dashboard, báo cáo" (016; tab theo bảng FR-010 trong màn)
      { key: 'reports', label: 'Báo cáo', icon: 'ChartColumn', functions: ['DASHBOARD_REPORT'], roles: [QL, TT, BS, DD, HC, KT] },
      // Yêu cầu phê duyệt (000 US5): mọi vai trò nhân viên lập hoặc duyệt được tùy loại yêu cầu; danh sách do backend lọc theo phạm vi
      {
        key: 'approval-requests',
        label: 'Yêu cầu phê duyệt',
        icon: 'ClipboardCheck',
        roles: [QL, TT, BS, DD, CS, BEP, HC, KT],
      },
      // Thông báo (009 T035, T028): mọi vai trò nhân viên có hộp thông báo; tab khác theo bảng quyền FR-039 của spec 009
      // vì Phụ lục 27 không có dòng thông báo
      { key: 'notifications', label: 'Thông báo', icon: 'Inbox', roles: [QL, TT, BS, DD, CS, BEP, HC, KT] },
      // Dòng "Hồ sơ người cao tuổi"
      { key: 'residents', label: 'Người cao tuổi', icon: 'Users', functions: ['RESIDENT_PROFILE'], roles: [QL, TT, BS, DD, CS, HC, NT, KT] },
    ],
  },
  {
    label: 'Trong ca',
    items: [
      // Dòng "Checklist, ghi nhận công việc"
      { key: 'care-tasks', label: 'Checklist ca', icon: 'ListChecks', functions: ['TASK_CHECKLIST'], roles: [QL, TT, BS, DD, CS] },
      // Dòng "Phát thuốc, thuốc khi cần"
      { key: 'medication', label: 'Phát thuốc', icon: 'Pill', functions: ['MEDICATION_ADMINISTRATION'], roles: [QL, TT, BS, DD] },
      // Dòng "Danh mục thuốc"
      { key: 'medication-catalog', label: 'Danh mục thuốc', icon: 'BookOpen', functions: ['MEDICATION_CATALOG'], roles: [QL, BS, DD] },
      // Dòng "Xử lý cảnh báo"
      { key: 'alerts', label: 'Cảnh báo', icon: 'Bell', functions: ['ALERT_HANDLING'], roles: [QL, TT, BS, DD, CS] },
      // Dòng "Sự cố, khẩn cấp"
      { key: 'incidents', label: 'Sự cố', icon: 'TriangleAlert', functions: ['INCIDENT_EMERGENCY'], roles: [QL, TT, BS, DD, CS, BEP, HC, NT, KT] },
      // Dòng "Bàn giao ca"
      { key: 'handovers', label: 'Bàn giao ca', icon: 'ArrowLeftRight', functions: ['SHIFT_HANDOVER'], roles: [QL, TT, BS, DD, CS] },
    ],
  },
  {
    label: 'Tầng và nhân sự',
    items: [
      // Dòng Hồ sơ nhân viên, chứng chỉ (Phụ lục 27): Quản lý viện T, Trưởng tầng X.
      { key: 'staff', label: 'Nhân sự', icon: 'Users', functions: ['STAFF_PROFILE'], roles: [QL, TT] },
      // Dòng "Phân bổ, chuyển giường" và "Cách ly phòng"
      { key: 'beds', label: 'Phòng, giường', icon: 'BedDouble', functions: ['BED_ALLOCATION', 'ROOM_ISOLATION'], roles: [QL, TT, HC, BS] },
      // Dòng "Lịch ca, phân công"
      { key: 'shifts', label: 'Lịch ca', icon: 'CalendarDays', functions: ['SHIFT_SCHEDULE'], roles: [QL, TT, BS, DD, CS, BEP, HC] },
      // Dòng "Suất ăn đã chốt, phiếu bữa ăn"
      { key: 'meals', label: 'Suất ăn', icon: 'Utensils', functions: ['MEAL_COUNT_TICKET'], roles: [QL, TT, BS, BEP] },
    ],
  },
  {
    label: 'Hành chính, tài chính',
    items: [
      // Dòng "Tiếp nhận, hợp đồng, đặt cọc"
      { key: 'admissions', label: 'Tiếp nhận, hợp đồng', icon: 'FileSignature', functions: ['ADMISSION_CONTRACT_DEPOSIT'], roles: [QL, HC, NT, KT] },
      // Dòng "Chi phí, khoản điều chỉnh"
      { key: 'charges', label: 'Chi phí', icon: 'Receipt', functions: ['COST_ADJUSTMENT'], roles: [QL, HC, NT, KT] },
      // Dòng "Mong muốn về khách thăm, người đón" (Q-364); người thân và quyền (dòng "Người thân và quyền") thêm khi thiết kế 012
      { key: 'relatives', label: 'Người thân', icon: 'HeartHandshake', functions: ['VISITOR_PREFERENCE'], roles: [QL, TT, DD, HC] },
      // Dòng "Đăng ký thăm"
      { key: 'visits', label: 'Đăng ký thăm', icon: 'CalendarCheck', functions: ['VISIT_REGISTRATION'], roles: [QL, TT, HC, NT] },
    ],
  },
  {
    label: 'Quản trị',
    items: [
      // Dòng "Tài khoản, phân quyền, tham số"
      { key: 'admin', label: 'Tài khoản, tham số', icon: 'Settings', functions: ['ACCOUNT_PERMISSION_PARAMETER'], roles: [QL, HC] },
      // Dòng "Nhật ký thay đổi"
      { key: 'audit-log', label: 'Nhật ký thay đổi', icon: 'History', functions: ['AUDIT_LOG'], roles: [QL, TT, BS, DD, CS, BEP, HC, KT] },
    ],
  },
]

// hasFunction: (mã chức năng) => có quyền theo /me/permissions; null khi chạy bản mẫu (lọc theo vai trò).
// built: tập khóa mục đã có màn; null thì không lọc.
export function canSee(item, role, hasFunction = null, built = null) {
  if (built && !built.has(item.key)) return false
  if (!hasFunction) return item.roles.includes(role)
  return item.functions ? item.functions.some(hasFunction) : role !== 'FAMILY_MEMBER'
}

export function navForRole(role, hasFunction = null, built = null) {
  return NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => canSee(item, role, hasFunction, built)),
  })).filter((group) => group.items.length > 0)
}
