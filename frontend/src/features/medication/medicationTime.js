// Giờ dạng HH:mm trong bản mẫu Phát thuốc (cùng ngày). Cửa sổ mặc định ±30 phút theo CFG-M07-01;
// thuốc có cửa sổ riêng trong danh mục do backend trả (spec 006 FR-020).
export const WINDOW_MINUTES = 30

export function minutes(hhmm) {
  const [h, m] = hhmm.split(':').map(Number)
  return h * 60 + m
}

export function toHHMM(total) {
  const t = ((total % 1440) + 1440) % 1440
  return `${String(Math.floor(t / 60)).padStart(2, '0')}:${String(t % 60).padStart(2, '0')}`
}

export function windowOf(time) {
  const m = minutes(time)
  return { from: toHHMM(m - WINDOW_MINUTES), to: toHHMM(m + WINDOW_MINUTES) }
}
