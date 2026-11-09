// Ngày hôm nay ở Việt Nam (yyyy-MM-dd), cho bữa trong ngày và tuần thực đơn (NFR-11).
const DAY = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Ho_Chi_Minh',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

export function todayInVietnam() {
  return DAY.format(new Date())
}
