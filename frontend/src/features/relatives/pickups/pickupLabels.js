// Nhãn tiếng Việt cho mã của api-012 tag pickups (glossary 012; spec FR-021, FR-023, FR-026)

export const PICKUP_TYPE_LABELS = {
  TEMPORARY_ABSENCE: 'Tạm vắng',
  FAMILY_OUTING: 'Đi chơi với gia đình',
  DAY_CARE_DEPARTURE: 'Bán trú về',
  SELF_RETURN: 'Tự về',
  INSTITUTE_RETURN: 'Viện đưa về',
  RESIDENCY_END: 'Kết thúc lưu trú',
}

export const PICKUP_BASIS_LABELS = {
  PICKUP_LIST: 'Trong danh sách',
  REPRESENTATIVE_EXCEPTION: 'Ngoại lệ do người đại diện xác nhận',
  MANAGER_EXCEPTION: 'Ngoại lệ do Quản lý viện duyệt',
  SELF_RETURN: 'Được tự về',
}

export const PICKUP_STATUS_LABELS = { UNUSED: 'Chưa dùng', USED: 'Đã dùng', NOT_USED: 'Không dùng' }

// Lý do chặn (blockReason của 409 PICKUP_BLOCKED, PICKUP_EXCEPTION_EXPIRED)
export const BLOCK_REASON_LABELS = {
  NOT_ON_LIST: 'Người đón không thuộc danh sách được phép đón và không có ngoại lệ Hiệu lực.',
  ID_DOCUMENT_MISMATCH: 'Giấy tờ xuất trình không khớp số đã đăng ký.',
  EXCEPTION_EXPIRED: 'Yêu cầu ngoại lệ đã hết hạn.',
  SELF_RETURN_OFF: 'Người cao tuổi không có dấu được tự về đang bật.',
}

// Nhãn ô nhân viên theo loại đón (FR-026: nhân viên tiễn với Tự về, người đưa về với Viện đưa về)
export function handoverLabel(type) {
  if (type === 'SELF_RETURN') return 'Nhân viên tiễn'
  if (type === 'INSTITUTE_RETURN') return 'Người đưa về'
  return 'Nhân viên bàn giao'
}

export const EXCEPTION_COMMAND_LABELS = { approve: 'Duyệt', reject: 'Từ chối', cancel: 'Hủy' }
export const EXCEPTION_COMMAND_DONE = { approve: 'Đã duyệt', reject: 'Đã từ chối', cancel: 'Đã hủy' }
