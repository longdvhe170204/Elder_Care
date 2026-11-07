// Nhãn của lượt người thân ở lại (012 US8; data-model mục 6; DESIGN.md 3.5).

export const STAY_STATUS_LABELS = {
  PENDING_CONFIRMATION: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  REJECTED: 'Từ chối',
  STAYING: 'Đang ở lại',
  ENDED: 'Đã kết thúc',
  CANCELLED: 'Hủy',
}

// Bộ lọc: Đã xong gộp các trạng thái cuối
export const STAY_FILTERS = [
  ['PENDING_CONFIRMATION', 'Chờ xác nhận', ['PENDING_CONFIRMATION']],
  ['CONFIRMED', 'Đã xác nhận', ['CONFIRMED']],
  ['STAYING', 'Đang ở lại', ['STAYING']],
  ['DONE', 'Đã xong', ['ENDED', 'REJECTED', 'CANCELLED']],
]

// Vị trí khác do cơ sở khai báo chưa có danh mục; hiện chỉ có phòng của người cao tuổi
export const ROOM_OF_RESIDENT = 'PHONG_NCT'

export function locationLabel(code) {
  return code === ROOM_OF_RESIDENT ? 'Phòng của người cao tuổi' : code
}

export const ROOMMATE_RESULTS = [
  ['AGREED', 'Đồng ý'],
  ['DISAGREED', 'Không đồng ý'],
]

// Ô datetime-local là giờ Việt Nam (không đổi giờ theo mùa)
export function vietnamIso(local) {
  return local ? `${local}:00+07:00` : ''
}
