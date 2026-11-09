// Nhãn lịch đo và ngưỡng theo api-007 `MeasurementSchedule`, `ThresholdVersion` (spec 007 FR-003 → FR-006,
// FR-020 → FR-025). Hàm thuần, không JSX.

export const SCHEDULE_STATUS = {
  PENDING: 'Chờ hiệu lực',
  EFFECTIVE: 'Hiệu lực',
  STOPPED: 'Đã ngừng',
  EXPIRED: 'Hết hiệu lực',
}
export const THRESHOLD_STATUS = {
  PENDING: 'Chờ hiệu lực',
  EFFECTIVE: 'Hiệu lực',
  EXPIRED: 'Hết hiệu lực',
  CANCELLED: 'Đã hủy',
}
export const PRIORITY_LABEL = { NORMAL: 'Thường', IMPORTANT: 'Quan trọng', MANDATORY: 'Bắt buộc' }
export const FREQUENCY_LABEL = { CLOCK_TIMES: 'Theo giờ', EVERY_N_HOURS: 'Mỗi n giờ', WEEKDAYS: 'Theo thứ' }
const WEEKDAY = { 1: 'Thứ 2', 2: 'Thứ 3', 3: 'Thứ 4', 4: 'Thứ 5', 5: 'Thứ 6', 6: 'Thứ 7', 7: 'Chủ nhật' }
export const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7].map((d) => [d, WEEKDAY[d]])

export function indicatorNames(ids, catalog) {
  return ids.map((id) => catalog.find((c) => c.id === id)?.name ?? 'Chỉ số').join(', ')
}

export function frequencyText(frequency) {
  if (!frequency) return ''
  if (frequency.type === 'EVERY_N_HOURS') return `Mỗi ${frequency.everyHours} giờ`
  const times = (frequency.times ?? []).join(', ')
  if (frequency.type === 'WEEKDAYS') {
    const days = (frequency.weekdays ?? []).map((d) => WEEKDAY[d]).join(', ')
    return times ? `${days}, lúc ${times}` : days
  }
  return `Lúc ${times}`
}

const BOUND_LABEL = [
  ['dangerBelow', 'nguy hiểm dưới'],
  ['warningBelow', 'cảnh báo dưới'],
  ['warningAbove', 'cảnh báo trên'],
  ['dangerAbove', 'nguy hiểm trên'],
]
export const BOUNDS = BOUND_LABEL

export function thresholdText(component, indicator) {
  const name = indicator?.components?.find((c) => c.code === component.code)?.name ?? component.code
  const parts = BOUND_LABEL.filter(([key]) => component[key] != null).map(
    ([key, label]) => `${label} ${component[key]}`,
  )
  return `${name}: ${parts.join(', ')}`
}

// "08:00, 20:00" → ["08:00", "20:00"]; giờ sai dạng thì trả null để form báo lỗi
export function parseTimes(text) {
  const times = text
    .split(/[,;\s]+/)
    .map((t) => t.trim())
    .filter(Boolean)
  if (times.length === 0 || times.some((t) => !/^[0-2][0-9]:[0-5][0-9]$/.test(t))) return null
  return times
}

export function vietnamIso(local) {
  return local ? `${local}:00+07:00` : undefined
}

export function residentName(r) {
  return r?.personalInfo?.fullName ?? 'Người cao tuổi'
}
