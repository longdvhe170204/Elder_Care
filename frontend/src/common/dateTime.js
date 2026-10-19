// Ngày giờ hiển thị theo NFR-11: dd/MM/yyyy HH:mm, giờ Asia/Ho_Chi_Minh (backend trả ISO-8601).

const DATE_TIME = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Ho_Chi_Minh',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

export function formatDateTime(iso) {
  const p = Object.fromEntries(DATE_TIME.formatToParts(new Date(iso)).map((part) => [part.type, part.value]))
  return `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute}`
}

const DATE = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Ho_Chi_Minh',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

// Ngày dd/MM/yyyy: ngày không giờ (yyyy-MM-dd) giữ nguyên ngày; thời điểm đổi sang ngày ở Việt Nam.
export function formatDate(value) {
  if (!value) return ''
  const plain = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (plain) return `${plain[3]}/${plain[2]}/${plain[1]}`
  const p = Object.fromEntries(DATE.formatToParts(new Date(value)).map((part) => [part.type, part.value]))
  return `${p.day}/${p.month}/${p.year}`
}
