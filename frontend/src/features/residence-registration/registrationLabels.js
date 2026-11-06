// Nhãn và tông của khai báo tạm trú, lưu trú (018; api-018, DESIGN.md bố cục K2, K5). Tông dùng biến thể của Badge.

export const FUNCTION_CODE = 'RESIDENCE_REGISTRATION'
export const RECORD_TYPE = 'M02.RESIDENCE_REGISTRATION'

export const TYPE_LABELS = {
  TEMPORARY_RESIDENCE_REGISTRATION: 'Đăng ký tạm trú',
  STAY_NOTIFICATION: 'Thông báo lưu trú',
}

export const STATUS_LABELS = {
  TO_SUBMIT: 'Cần khai báo',
  SUBMITTED: 'Đã nộp',
  CONFIRMED: 'Đã xác nhận',
  EXPIRED: 'Hết hạn',
  DEREGISTERED: 'Đã xóa',
  SUPERSEDED: 'Được thay thế',
  CANCELLED: 'Đã hủy',
}

export const STATUS_TONES = {
  TO_SUBMIT: 'attention',
  SUBMITTED: 'neutral',
  CONFIRMED: 'settled',
  EXPIRED: 'urgent',
  DEREGISTERED: 'neutral-outline',
  SUPERSEDED: 'neutral-outline',
  CANCELLED: 'neutral-outline',
}

// Khai báo còn hiệu lực xử lý hiện ra đầu khung; ba trạng thái cuối thu gọn (K3)
export const CURRENT_STATUSES = ['TO_SUBMIT', 'SUBMITTED', 'CONFIRMED', 'EXPIRED']
export const IN_PROGRESS_STATUSES = ['TO_SUBMIT', 'SUBMITTED']

export const TASK_LABELS = {
  OVERDUE: 'Quá hạn',
  TO_CREATE: 'Cần lập',
  EXPIRING_SOON: 'Sắp hết hạn',
  EXPIRED: 'Hết hạn',
  TO_DEREGISTER: 'Cần khai báo xóa',
}

export const TASK_TONES = {
  OVERDUE: 'urgent',
  TO_CREATE: 'attention',
  EXPIRING_SOON: 'attention',
  EXPIRED: 'urgent',
  TO_DEREGISTER: 'attention',
}

// Quá hạn trước, rồi theo hạn tăng dần; việc không có hạn (khai báo xóa) cuối cùng
const TASK_RANK = { OVERDUE: 0, EXPIRED: 1, TO_CREATE: 2, EXPIRING_SOON: 3, TO_DEREGISTER: 4 }

export function compareTasks(a, b) {
  const rank = TASK_RANK[a.taskType] - TASK_RANK[b.taskType]
  if (rank !== 0) return rank
  if (!a.dueAt || !b.dueAt) return (a.dueAt ? 0 : 1) - (b.dueAt ? 0 : 1)
  return a.dueAt.localeCompare(b.dueAt)
}

export const CHANNEL_LABELS = { PUBLIC_SERVICE_PORTAL: 'Cổng dịch vụ công', IN_PERSON: 'Trực tiếp' }

export const RESULT_LABELS = { PENDING: 'Chờ kết quả', ACCEPTED: 'Được chấp nhận', REJECTED: 'Bị từ chối' }
export const RESULT_TONES = { PENDING: 'neutral', ACCEPTED: 'settled', REJECTED: 'attention' }

export const COMMANDS = {
  CONFIRM_LOCAL: 'confirm-local-permanent-resident',
  SUBMITTED: 'record-submitted',
  ACCEPTED: 'record-accepted',
  REJECTED: 'record-rejected',
  DEREGISTERED: 'record-deregistered',
}

// Ô nội dung đính chính được (RegistrationCorrectionProvider.originalContent của backend)
export const CORRECTION_FIELDS = [
  { name: 'localPermanentResident', label: 'Thường trú cùng xã/phường' },
  { name: 'basisNote', label: 'Căn cứ xác nhận thường trú' },
  { name: 'submittedDate', label: 'Ngày nộp' },
  { name: 'channel', label: 'Kênh nộp' },
  { name: 'receivingAuthority', label: 'Cơ quan tiếp nhận' },
  { name: 'dossierCode', label: 'Mã hồ sơ' },
  { name: 'result', label: 'Kết quả' },
  { name: 'rejectionReason', label: 'Lý do bị từ chối' },
  { name: 'expiryDate', label: 'Ngày hết hạn' },
  { name: 'evidenceFileId', label: 'Mã tệp bằng chứng' },
]
