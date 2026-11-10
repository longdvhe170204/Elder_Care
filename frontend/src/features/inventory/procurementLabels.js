// Nhãn và hàm thuần của yêu cầu mua sắm (019 US7; spec FR-008 → FR-008d; api-019 ProcurementRequest; DESIGN.md 3.5).

export const PROCUREMENT_STATUS_LABELS = {
  DRAFT: 'Nháp',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  PENDING_RECONCILIATION: 'Chờ đối chiếu',
  DISCREPANCY: 'Lệch',
  RECONCILED: 'Đã đối chiếu',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
}

export const PROCUREMENT_TYPES = [
  ['INGREDIENT', 'Nguyên liệu'],
  ['ASSET', 'Tài sản'],
  ['SERVICE', 'Dịch vụ sửa chữa, bảo trì'],
  ['MEDICATION', 'Thuốc'],
  ['OTHER', 'Khác'],
]

export const PRIORITIES = [
  ['NORMAL', 'Thường'],
  ['URGENT', 'Gấp'],
]

export const RESOLUTIONS = [
  ['ACCEPT_DISCREPANCY', 'Chấp nhận lệch'],
  ['REQUEST_REFUND', 'Yêu cầu bộ phận hoàn tiền'],
]

// Bộ lọc: Đã xong gộp các trạng thái cuối (FR-008: Đã đối chiếu, Từ chối, Đã hủy)
export const PROCUREMENT_FILTERS = [
  ['ALL', 'Tất cả', null],
  ['DRAFT', 'Nháp', ['DRAFT']],
  ['PENDING_APPROVAL', 'Chờ duyệt', ['PENDING_APPROVAL']],
  ['APPROVED', 'Đã duyệt', ['APPROVED']],
  ['PENDING_RECONCILIATION', 'Chờ đối chiếu', ['PENDING_RECONCILIATION']],
  ['DISCREPANCY', 'Lệch', ['DISCREPANCY']],
  ['DONE', 'Đã xong', ['RECONCILED', 'REJECTED', 'CANCELLED']],
]

export function typeLabel(code) {
  return PROCUREMENT_TYPES.find(([c]) => c === code)?.[1] ?? code
}

export function priorityLabel(code) {
  return PRIORITIES.find(([c]) => c === code)?.[1] ?? code
}

const MONEY = new Intl.NumberFormat('vi-VN')

// Tiền theo đồng (NFR-11)
export function money(value) {
  return value === null || value === undefined ? '—' : `${MONEY.format(value)} đ`
}

// Thành tiền dòng = số lượng × đơn giá dự kiến, làm tròn tới đồng (plan 019, cách backend tính lại)
export function lineAmount(line) {
  const quantity = Number(line.quantity)
  const price = Number(line.estimatedUnitPrice)
  return Number.isFinite(quantity) && Number.isFinite(price) ? Math.round(quantity * price) : 0
}

// "Đã nhận" chỉ có ở loại thuốc; null là chưa biết (006 chưa có nguồn tiếp nhận)
export function receivedLabel(request) {
  if (request.type !== 'MEDICATION' || request.received === null || request.received === undefined) return null
  return request.received ? 'Đã nhận' : 'Chưa nhận đủ'
}

// Đơn có thể gắn dòng thuốc (FR-008d): nguồn viện cung cấp, Hiệu lực, Tạm dừng hoặc Chờ hiệu lực
export function facilityUnended(prescription) {
  return (
    prescription.medicationSource === 'FACILITY' &&
    ['ACTIVE', 'SUSPENDED', 'PENDING_EFFECTIVE'].includes(prescription.status)
  )
}

// Ô datetime-local là giờ Việt Nam (không đổi giờ theo mùa)
export function vietnamIso(local) {
  return local ? `${local}:00+07:00` : ''
}
