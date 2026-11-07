// Nhãn của api-012 tag visits (glossary 012; spec FR-039, DESIGN.md 3.5)

export const VISIT_STATUS_LABELS = {
  APPROVED: 'Đã duyệt',
  CHECKED_IN: 'Đã vào',
  CHECKED_OUT: 'Đã ra',
  NO_SHOW: 'Không đến',
  CANCELLED: 'Hủy',
}

export const slotLabel = (v) => `${v.startTime}–${v.endTime}`

// Ngày hôm nay theo giờ Việt Nam, dạng yyyy-MM-dd
export function todayIso() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date())
}
