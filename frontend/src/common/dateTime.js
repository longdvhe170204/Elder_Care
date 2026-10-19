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
