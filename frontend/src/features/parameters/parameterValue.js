// Hiển thị và đọc giá trị tham số theo kiểu (000 T081; hợp đồng schema Parameter, glossary mục 6.3).

export const UNIT_LABELS = {
  MINUTES: 'phút',
  HOURS: 'giờ',
  DAYS: 'ngày',
  MONTHS: 'tháng',
  YEARS: 'năm',
  TIMES: 'lần',
  PERCENT: '%',
  VND: 'đồng',
  PERSONS: 'người',
}

const NUMBER = new Intl.NumberFormat('vi-VN')

export function isDuration(value) {
  return (
    value !== null &&
    typeof value === 'object' &&
    !Array.isArray(value) &&
    Object.keys(value).length === 2 &&
    'value' in value &&
    'unit' in value
  )
}

export function formatValue(type, value, unit) {
  if (value === null || value === undefined) return 'Chưa đặt'
  if (type === 'TABLE') {
    return Array.isArray(value?.rows) ? `Bảng ${value.rows.length} dòng` : 'Bảng'
  }
  if (isDuration(value)) return `${NUMBER.format(value.value)} ${UNIT_LABELS[value.unit] ?? value.unit}`
  if (type === 'PERCENTAGE') return `${NUMBER.format(value)}%`
  if (typeof value === 'number') {
    const label = UNIT_LABELS[unit]
    return label ? `${NUMBER.format(value)} ${label}` : NUMBER.format(value)
  }
  return String(value)
}
