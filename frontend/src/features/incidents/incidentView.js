// Nhãn và nhóm của sự cố theo api-007 `Incident` (spec 007 FR-040 → FR-046; DESIGN.md 3.5). Hàm thuần, không JSX.

export const LEVEL_LABEL = { CRITICAL: 'Khẩn cấp', MEDIUM: 'Trung bình', LOW: 'Nhẹ' }
export const LEVEL_ORDER = ['CRITICAL', 'MEDIUM', 'LOW']

export const STATUS_LABEL = {
  NEW: 'Mới',
  IN_PROGRESS: 'Đang xử lý',
  MONITORING: 'Đang theo dõi',
  CLOSED: 'Đã đóng',
  VOIDED: 'Đã hủy',
}
export const OPEN_STATUSES = ['NEW', 'IN_PROGRESS', 'MONITORING']

export function isOpen(incident) {
  return OPEN_STATUSES.includes(incident.status)
}

// Mới, Đang xử lý hổ phách như cảnh báo mở; Đã đóng xanh lá; còn lại trung tính (DESIGN.md 3.5)
export function statusTone(incident) {
  if (['NEW', 'IN_PROGRESS'].includes(incident.status)) return 'attention'
  if (incident.status === 'CLOSED') return 'settled'
  return 'neutral'
}

// Khẩn cấp trước, rồi mới phát hiện trước
export function sortIncidents(incidents) {
  return incidents.toSorted(
    (a, b) =>
      LEVEL_ORDER.indexOf(a.level) - LEVEL_ORDER.indexOf(b.level) ||
      String(b.content?.discoveredAt).localeCompare(String(a.content?.discoveredAt)),
  )
}

export function residentName(profile) {
  return profile?.personalInfo?.fullName ?? 'Người cao tuổi'
}

// Giờ Việt Nam cho ô datetime-local (yyyy-MM-ddTHH:mm) và ISO-8601 có +07:00 gửi backend (frontend/README mục 4)
export function vietnamNowLocal(now = new Date()) {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Ho_Chi_Minh',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    })
      .formatToParts(now)
      .map((p) => [p.type, p.value]),
  )
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`
}

export function vietnamIso(local) {
  return `${local}:00+07:00`
}

// Mức thấp hơn mặc định của loại cần lý do (FR-042)
export function needsLowerReason(level, type) {
  return !!type && LEVEL_ORDER.indexOf(level) > LEVEL_ORDER.indexOf(type.defaultLevel)
}
