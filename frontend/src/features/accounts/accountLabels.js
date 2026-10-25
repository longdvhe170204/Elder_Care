// Nhãn tiếng Việt cho tài khoản (002; nghiep-vu.md 2.3, 19.1; data-model mục 2). Mã giữ theo hợp đồng api-002.

// Vai trò nhân viên được gán (StaffRoleCode, FR-003)
export const STAFF_ROLES = ['AC-01', 'AC-02', 'AC-03', 'AC-04', 'AC-05', 'AC-07', 'AC-09', 'AC-14']

export const ROLE_LABELS = {
  'AC-01': 'Quản lý viện',
  'AC-02': 'Trưởng tầng',
  'AC-03': 'Bác sĩ',
  'AC-04': 'Điều dưỡng',
  'AC-05': 'Nhân viên chăm sóc',
  'AC-07': 'Nhân viên bếp',
  'AC-09': 'Hành chính',
  'AC-10': 'Người thân',
  'AC-14': 'Kế toán',
  'AC-16': 'Người cao tuổi',
}

export const STATUS_LABELS = {
  ACTIVE: 'Hoạt động',
  TEMPORARILY_LOCKED: 'Khóa tạm',
  INACTIVE: 'Không hoạt động',
  LOCKED: 'Đã khóa',
}

export const TYPE_LABELS = {
  STAFF: 'Nhân viên',
  FAMILY_MEMBER: 'Người thân',
  RESIDENT: 'Người cao tuổi',
}

export const LOCK_CAUSE_LABELS = {
  MANUAL: 'Khóa thủ công',
  RESIGNATION: 'Nghỉ việc',
  EXPIRED_AFTER_RESIDENCY_END: 'Hết hạn sau kết thúc lưu trú',
  RELATIONSHIP_ENDED: 'Hết quan hệ người thân',
  RESIDENT_RESIDENCY_ENDED: 'Kết thúc lưu trú',
  RESIDENT_REQUESTED: 'Người cao tuổi tự yêu cầu',
  WITH_REASON: 'Có lý do',
  FAMILY_REQUEST: 'Theo đề nghị của người thân',
}

// Người cao tuổi yêu cầu hoặc đồng ý (ResidentConsent, FR-067, FR-073)
export const CONSENT_METHOD_LABELS = { REQUESTED: 'Tự yêu cầu', AGREED: 'Đồng ý' }

// Căn cứ khóa tài khoản người cao tuổi (FR-072 (b), (c))
export const RESIDENT_LOCK_BASIS_LABELS = { RESIDENT_REQUESTED: 'Người cao tuổi tự yêu cầu', WITH_REASON: 'Có lý do khác' }

// Lệnh trên tài khoản người cao tuổi theo allowedCommands (002 T095): nhãn nút mở form, nhãn nút gửi
export const RESIDENT_COMMANDS = {
  'login-qr-code': { label: 'Tạo mã QR' },
  'unbind-device': { label: 'Gỡ thiết bị', submit: 'Gỡ thiết bị đang gắn' },
  'lock-resident': { label: 'Khóa', submit: 'Khóa tài khoản' },
  'unlock-resident': { label: 'Mở khóa', submit: 'Mở khóa tài khoản' },
}

// Lệnh trên tài khoản nhân viên theo allowedCommands (FR-042): nhãn nút mở form, nhãn nút gửi
export const COMMANDS = {
  lock: { label: 'Khóa tài khoản', submit: 'Khóa tài khoản' },
  unlock: { label: 'Mở khóa', submit: 'Mở khóa' },
  'reset-password': { label: 'Cấp lại mật khẩu', submit: 'Cấp mật khẩu tạm' },
  'assign-role': { label: 'Gán vai trò', submit: 'Gán vai trò' },
  'remove-role': { label: 'Gỡ vai trò', submit: 'Gỡ vai trò' },
}

// Hồ sơ liên kết: họ tên khi có (T032a), không thì mã hồ sơ
export function profileLabel(account) {
  if (account.fullName) return account.fullName
  if (account.staffId) return `Hồ sơ nhân viên #${account.staffId}`
  if (account.familyMemberId) return `Hồ sơ người thân #${account.familyMemberId}`
  if (account.residentId) return `Người cao tuổi #${account.residentId}`
  return 'Chưa có họ tên'
}

export function roleNames(codes = []) {
  return codes.map((code) => ROLE_LABELS[code] ?? code).join(', ')
}
