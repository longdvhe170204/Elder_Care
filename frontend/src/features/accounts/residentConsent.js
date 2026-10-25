// Xác nhận của chính người cao tuổi (ResidentConsent; FR-067 tạo, FR-073 mở khóa): kiểm ô và dựng thân yêu cầu.

export const EMPTY_CONSENT = { method: '', staffId: '' }

export function consentErrors(consent) {
  const errors = {}
  if (!consent.method) errors.method = 'Chọn người cao tuổi tự yêu cầu hay đồng ý'
  if (!/^\d+$/.test(consent.staffId.trim())) errors.staffId = 'Nhập mã hồ sơ nhân viên'
  return errors
}

export function consentBody(consent) {
  return { method: consent.method, withStaffId: Number(consent.staffId.trim()) }
}
