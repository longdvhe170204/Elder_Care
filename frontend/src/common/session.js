// Phiên web (002 research A-02, T045): mã thiết bị gửi trong X-Device-Id, vai trò chính để chọn menu, nơi đến sau khi
// đăng nhập. Token nằm trong cookie HttpOnly do máy chủ đặt, mã web không đọc được.

const DEVICE_KEY = 'elder-care-device-id'

// Mã thiết bị sinh lần đầu mở web trên trình duyệt, giữ trong localStorage (FR-014a). Không đọc, ghi được thì sinh mới.
export function deviceId() {
  try {
    const saved = localStorage.getItem(DEVICE_KEY)
    if (saved) return saved
    const created = crypto.randomUUID()
    localStorage.setItem(DEVICE_KEY, created)
    return created
  } catch {
    return crypto.randomUUID()
  }
}

// Mã vai trò (AC-xx) → mã vai trò của khung nhân viên (navigation.js ROLES), theo thứ tự ưu tiên khi có nhiều vai trò
const ROLE_BY_CODE = [
  ['AC-01', 'NURSING_HOME_MANAGER'],
  ['AC-02', 'FLOOR_MANAGER'],
  ['AC-03', 'DOCTOR'],
  ['AC-04', 'NURSING_STAFF'],
  ['AC-05', 'CAREGIVER'],
  ['AC-07', 'KITCHEN_STAFF'],
  ['AC-09', 'OFFICE_STAFF'],
  ['AC-14', 'ACCOUNTANT'],
]

// Vai trò dùng để dựng menu; menu chỉ để hiển thị, quyền thật do backend kiểm (constitution XI)
export function primaryRole(codes = []) {
  return ROLE_BY_CODE.find(([code]) => codes.includes(code))?.[1] ?? null
}

// Nơi đến sau khi đăng nhập hoặc đổi mật khẩu (FR-006): buộc đổi mật khẩu trước, rồi theo loại tài khoản
export function homeOf(user) {
  if (user.mustChangePassword) return '/change-password'
  return user.type === 'FAMILY_MEMBER' ? '/family' : '/'
}
