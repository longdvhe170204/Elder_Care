import { formatDateTime } from '@/common/dateTime'

export const SCOPES = { FLOOR: 'Tầng', AREA: 'Khu vực', FACILITY: 'Toàn viện' }
export const STATUSES = {
  DRAFT: 'Nháp',
  PUBLISHED: 'Đã công bố',
  IN_PROGRESS: 'Đang diễn ra',
  AWAITING_HANDOVER: 'Chờ bàn giao',
  CLOSED: 'Đã đóng',
  CANCELLED: 'Đã hủy',
}
export const scopeLabel = (schedule) =>
  `${SCOPES[schedule.scopeType]}${schedule.scopeId ? ` #${schedule.scopeId}` : ''}`
export const versionHeader = (version) => `"${version}"`
// Ô nhập giờ địa phương dùng cùng múi giờ với tiện ích hiển thị chung.
export function localInput(iso) {
  if (!iso) return ''
  const [date, time] = formatDateTime(iso).split(' ')
  const [day, month, year] = date.split('/')
  return `${year}-${month}-${day}T${time}`
}
export function currentMonth() {
  return localInput(new Date().toISOString()).slice(0, 7)
}
export function nextMonth(month) {
  const [year, number] = month.split('-').map(Number)
  return `${number === 12 ? year + 1 : year}-${String(number === 12 ? 1 : number + 1).padStart(2, '0')}-01`
}
