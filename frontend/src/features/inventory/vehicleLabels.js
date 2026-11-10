// Nhãn và hàm thuần của lịch xe (019 US6; spec FR-017, FR-018; api-019 VehicleSchedule; DESIGN.md 3.5).

export const SCHEDULE_STATUS_LABELS = {
  BOOKED: 'Đã đặt',
  IN_USE: 'Đang dùng',
  COMPLETED: 'Đã hoàn thành',
  CANCELLED: 'Đã hủy',
}

export const SCHEDULE_FILTERS = [
  ['ALL', 'Tất cả', null],
  ['BOOKED', 'Đã đặt', ['BOOKED']],
  ['IN_USE', 'Đang dùng', ['IN_USE']],
  ['COMPLETED', 'Đã hoàn thành', ['COMPLETED']],
  ['CANCELLED', 'Đã hủy', ['CANCELLED']],
]

export const PURPOSES = [
  ['TRIP', 'Chuyến đi ngoài viện'],
  ['MEDICAL_VISIT', 'Đưa đi khám'],
  ['NON_EMERGENCY_TRANSFER', 'Chuyển viện không cấp cứu'],
  ['OTHER', 'Khác'],
]

// FR-017: Trưởng tầng, Hành chính, Quản lý viện đặt lịch; FR-018: người đặt hoặc Quản lý viện ra lệnh
export const BOOKER_ROLES = ['FLOOR_MANAGER', 'OFFICE_STAFF', 'NURSING_HOME_MANAGER']

export function purposeLabel(code) {
  return PURPOSES.find(([c]) => c === code)?.[1] ?? code
}

const DAY = 24 * 60 * 60 * 1000

function parse(date) {
  return new Date(`${date}T00:00:00Z`)
}

function format(date) {
  return date.toISOString().slice(0, 10)
}

export function addDays(date, days) {
  return format(new Date(parse(date).getTime() + days * DAY))
}

// Hôm nay theo giờ Việt Nam (yyyy-MM-dd)
export function todayInVietnam(now = new Date()) {
  return format(new Date(now.getTime() + 7 * 60 * 60 * 1000))
}

// Tuần thứ Hai → Chủ nhật chứa ngày; truy vấn [thứ Hai 00:00, thứ Hai kế 00:00) giờ Việt Nam
export function weekOf(date) {
  const day = parse(date).getUTCDay()
  const monday = addDays(date, -((day + 6) % 7))
  return {
    monday,
    sunday: addDays(monday, 6),
    from: `${monday}T00:00:00+07:00`,
    to: `${addDays(monday, 7)}T00:00:00+07:00`,
  }
}

// Ô datetime-local là giờ Việt Nam (không đổi giờ theo mùa)
export function vietnamIso(local) {
  return local ? `${local}:00+07:00` : ''
}

// ISO → giá trị ô datetime-local theo giờ Việt Nam
export function toLocalInput(iso) {
  if (!iso) return ''
  return new Date(new Date(iso).getTime() + 7 * 60 * 60 * 1000).toISOString().slice(0, 16)
}
