// Menu theo vai trò. Mỗi mục ghi dòng của Phụ lục 27 (docs/business/nghiep-vu.md) làm căn cứ;
// `roles` là các vai trò có quyền khác "—" ở dòng đó. Đây chỉ là cách hiển thị: quyền thật
// do backend trả (feature 002, `/toi/quyen`) và backend vẫn kiểm mọi lệnh.

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
      { key: 'dashboard', label: 'Tổng quan', icon: 'LayoutDashboard', roles: [QL, TT, BS, DD, HC, KT] },
      // Dòng "Hồ sơ người cao tuổi"
      { key: 'residents', label: 'Người cao tuổi', icon: 'Users', roles: [QL, TT, BS, DD, CS, HC, NT, KT] },
    ],
  },
  {
    label: 'Trong ca',
    items: [
      // Dòng "Checklist, ghi nhận công việc"
      { key: 'care-tasks', label: 'Công việc chăm sóc', icon: 'ListChecks', roles: [QL, TT, BS, DD, CS] },
      // Dòng "Phát thuốc, thuốc khi cần"
      { key: 'medication', label: 'Phát thuốc', icon: 'Pill', roles: [QL, TT, BS, DD] },
      // Dòng "Xử lý cảnh báo"
      { key: 'alerts', label: 'Cảnh báo', icon: 'Bell', roles: [QL, TT, BS, DD, CS] },
      // Dòng "Sự cố, khẩn cấp"
      { key: 'incidents', label: 'Sự cố', icon: 'TriangleAlert', roles: [QL, TT, BS, DD, CS, BEP, HC, NT, KT] },
      // Dòng "Bàn giao ca"
      { key: 'handovers', label: 'Bàn giao ca', icon: 'ArrowLeftRight', roles: [QL, TT, BS, DD, CS] },
    ],
  },
  {
    label: 'Tầng và nhân sự',
    items: [
      // Dòng "Phân bổ, chuyển giường"
      { key: 'beds', label: 'Phòng, giường', icon: 'BedDouble', roles: [QL, TT, HC] },
      // Dòng "Lịch ca, phân công"
      { key: 'shifts', label: 'Lịch ca', icon: 'CalendarDays', roles: [QL, TT, BS, DD, CS, BEP, HC] },
      // Dòng "Suất ăn đã chốt, phiếu bữa ăn"
      { key: 'meals', label: 'Suất ăn', icon: 'Utensils', roles: [QL, TT, BS, BEP] },
    ],
  },
  {
    label: 'Hành chính, tài chính',
    items: [
      // Dòng "Tiếp nhận, hợp đồng, đặt cọc"
      { key: 'admissions', label: 'Tiếp nhận, hợp đồng', icon: 'FileSignature', roles: [QL, HC, NT, KT] },
      // Dòng "Chi phí, khoản điều chỉnh"
      { key: 'charges', label: 'Chi phí', icon: 'Receipt', roles: [QL, HC, NT, KT] },
      // Dòng "Đăng ký thăm"
      { key: 'visits', label: 'Đăng ký thăm', icon: 'CalendarCheck', roles: [QL, TT, HC, NT] },
    ],
  },
  {
    label: 'Quản trị',
    items: [
      // Dòng "Tài khoản, phân quyền, tham số"
      { key: 'admin', label: 'Tài khoản, tham số', icon: 'Settings', roles: [QL, HC] },
      // Dòng "Nhật ký thay đổi"
      { key: 'audit-log', label: 'Nhật ký thay đổi', icon: 'History', roles: [QL, TT, BS, DD, CS, BEP, HC, KT] },
    ],
  },
]

export function navForRole(role) {
  return NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => item.roles.includes(role)),
  })).filter((group) => group.items.length > 0)
}
