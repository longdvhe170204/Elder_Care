// 014 T009: nhãn và hàm thuần của màn Buổi hoạt động (glossary 20.6; trạng thái theo data-model mục 3, 4).

export const SESSION_STATUS = {
  SCHEDULED: 'Đã lên lịch',
  ATTENDANCE_TAKEN: 'Đã điểm danh',
  NO_ATTENDANCE: 'Không điểm danh',
  CANCELLED: 'Đã hủy',
}

export const REGISTRATION_SOURCE = {
  REGULAR: 'Thường xuyên',
  MANUAL: 'Thêm tay',
  ADDED_AT_ATTENDANCE: 'Thêm lúc điểm danh',
}

export const REGISTRATION_STATUS = {
  REGISTERED: 'Đã đăng ký',
  CANCELLED: 'Đã hủy',
  ATTENDED: 'Đã điểm danh',
}

const DAY = 24 * 60 * 60 * 1000

function parse(date) {
  const [y, m, d] = date.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d))
}

function format(value) {
  return value.toISOString().slice(0, 10)
}

export function addDays(date, days) {
  return format(new Date(parse(date).getTime() + days * DAY))
}

// Tuần từ thứ Hai tới Chủ nhật chứa ngày (yyyy-MM-dd).
export function weekOf(date) {
  const day = parse(date).getUTCDay()
  const from = addDays(date, -((day + 6) % 7))
  return { from, to: addDays(from, 6) }
}

export function countByStatus(sessions) {
  const counts = { ALL: sessions.length }
  for (const code of Object.keys(SESSION_STATUS)) counts[code] = 0
  for (const s of sessions) if (s.status in counts) counts[s.status] += 1
  return counts
}

// Ô datetime-local (giờ Việt Nam) → ISO-8601 có +07:00 (frontend/README.md luật 6).
export function toOffsetIso(local) {
  return local ? `${local}:00+07:00` : undefined
}

// Hôm nay theo giờ Việt Nam (yyyy-MM-dd).
export function todayInVietnam(now = new Date()) {
  return format(new Date(now.getTime() + 7 * 60 * 60 * 1000))
}

export const ATTENDANCE_RESULT = { PRESENT: 'Có mặt', ABSENT: 'Vắng' }

export const PARTICIPATION_LEVEL = {
  ACTIVE: 'Tích cực',
  NORMAL: 'Bình thường',
  PASSIVE: 'Thụ động',
  LEFT_EARLY: 'Bỏ giữa chừng',
}

export const COMMUNICATION_LEVEL = { PROACTIVE: 'Chủ động', WHEN_ASKED: 'Khi được hỏi', MINIMAL: 'Ít giao tiếp' }

export const ABSENCE_REASON = {
  REFUSED: 'Từ chối',
  HEALTH: 'Sức khỏe',
  AWAY: 'Đang vắng mặt',
  NO_SHOW: 'Không đến',
  OTHER: 'Khác',
}

// Thân SessionAttendance (api-014) từ các dòng đã chọn kết quả; null khi chưa có dòng nào hoặc còn dòng thiếu trường
// bắt buộc (Có mặt cần mức tham gia, Vắng cần lý do; FR-028, FR-029).
export function attendanceBody(rows, entries) {
  const results = []
  for (const row of rows) {
    const e = entries[row.residentId] ?? {}
    if (!e.result) continue
    if (e.result === 'PRESENT' && !e.participationLevel) return null
    if (e.result === 'ABSENT' && !e.absenceReason) return null
    const item = { residentId: row.residentId }
    if (row.addedAtAttendance) item.addedAtAttendance = true
    item.result = e.result
    if (e.result === 'PRESENT') {
      item.participationLevel = e.participationLevel
      if (e.communicationLevel) item.communicationLevel = e.communicationLevel
    } else item.absenceReason = e.absenceReason
    if (e.conditionAfter?.trim()) item.conditionAfter = e.conditionAfter.trim()
    if (e.note?.trim()) item.note = e.note.trim()
    results.push(item)
  }
  return results.length ? { results } : null
}
