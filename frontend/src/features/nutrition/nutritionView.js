// 011 (màn web; bố cục duyệt 2026-11-08): nhãn tiếng Việt theo glossary 17 và hàm thuần chuyển dữ liệu api-011 sang dạng
// hiển thị. Màn chỉ đọc qua các hàm này để khi hợp đồng đổi chỉ sửa một chỗ (DESIGN.md mục 13 điều 6).

export const TEXTURE = { REGULAR: 'Thường', SOFT: 'Mềm', PUREED: 'Xay nhuyễn' }

export const CATALOG_STATUS = { ACTIVE: 'Hiệu lực', INACTIVE: 'Ngừng hiệu lực' }

export const DISH_STATUS = {
  DRAFT: 'Nháp',
  PENDING_CONFIRMATION: 'Chờ xác nhận',
  ACTIVE: 'Hiệu lực',
  INACTIVE: 'Ngừng hiệu lực',
  CANCELLED: 'Đã hủy',
}

export const MENU_STATUS = {
  DRAFT: 'Nháp',
  PENDING_PUBLICATION: 'Chờ công bố',
  PUBLISHED: 'Đã công bố',
  APPLIED: 'Đã áp dụng',
  CANCELLED: 'Đã hủy',
}

export const ASSIGNMENT_STATUS = {
  PROPOSED: 'Đề xuất',
  PENDING_EFFECTIVE: 'Chờ hiệu lực',
  ACTIVE: 'Hiệu lực',
  DISCONTINUED: 'Ngừng',
  CANCELLED: 'Đã hủy',
}

export const ASSIGNMENT_BASIS = { ASSESSMENT: 'Đánh giá', REVIEW: 'Xem lại chế độ ăn', OTHER: 'Khác' }

export const SLIP_STATUS = {
  CLOSED: 'Đã chốt',
  PREPARED: 'Đã chuẩn bị',
  DELIVERED: 'Đã giao',
  HAS_DISCREPANCY: 'Có sai lệch',
  RECEIVED: 'Đã nhận',
}

export const RESOLUTION = { ADD_PORTION: 'Bổ sung suất', REPLACE_PORTION: 'Đổi suất', OTHER: 'Khác' }

export const FAMILY_FOOD_STATUS = {
  NOT_ALLOWED: 'Không sử dụng',
  NEEDS_CONFIRMATION: 'Cần xác nhận',
  ALLOWED: 'Được sử dụng',
  ENDED: 'Đã kết thúc',
}

export const DISPOSAL = { RETURNED: 'Trả lại gia đình', DISCARDED: 'Hủy bỏ', CONSUMED: 'Đã dùng hết' }

// Lý do hệ thống của đồ ăn gia đình (plan Phase 9)
export const FAMILY_FOOD_REASON = {
  FORBIDDEN_TYPE: 'Loại đồ ăn bị cấm',
  EXPIRED: 'Đã quá hạn dùng',
  ALLERGY: 'Có thành phần gây dị ứng',
  PERSONAL_RESTRICTION: 'Vi phạm hạn chế riêng',
  DIET_RESTRICTION: 'Vi phạm hạn chế của chế độ ăn',
  INGREDIENTS_UNKNOWN: 'Không rõ thành phần',
  OTHER_INGREDIENT: 'Có thành phần ngoài danh mục',
  RESIDENT_OTHER_ALLERGY: 'Người cao tuổi có dị ứng loại khác',
  TEXTURE_NOT_REGULAR: 'Kết cấu chỉ định khác Thường',
  SENDER_NOT_RELATED: 'Người gửi không có quan hệ Hiệu lực',
  TREATMENT_DIET: 'Chế độ ăn liên quan điều trị',
}

export const CLOSING_FLAG = {
  NO_MENU: 'Chưa có thực đơn công bố',
  CLOSED_AFTER_MEAL_TIME: 'Chốt sau giờ bữa',
  MISSING_SUBSTITUTE_DISH: 'Thiếu món thay thế',
  NO_DIET_ASSIGNED: 'Có người chưa gán chế độ ăn',
}

// Dấu của suất đặc biệt trên nhãn phiếu (FR-040a, FR-036a, FR-037a)
export const PORTION_FLAG = {
  EXPECTED_RETURN: 'Dự kiến trở về',
  NEEDS_CROSS_CHECK: 'Cần đối chiếu dị ứng loại khác',
  MISSING_SUBSTITUTE_DISH: 'Thiếu món thay thế',
  SAFE_DISH: 'Món an toàn',
  NO_DISH_IN_MENU: 'Thực đơn không có món',
  NO_DIET_ASSIGNED: 'Chưa gán chế độ ăn',
  HELD_PORTION: 'Suất giữ',
}

export const ADJUSTMENT_CHANGE = {
  ADDED: 'Thêm suất',
  REMOVED: 'Bớt suất',
  MOVED_OUT: 'Chuyển đi',
  MOVED_IN: 'Chuyển đến',
  PORTION_CHANGED: 'Đổi suất',
}

export const MISSING_DATA = { STAY: 'Chưa đọc được lượt người thân ở lại', TRIP: 'Chưa đọc được chuyến đi ngoài viện' }

export const REVIEW_SOURCE = { PROLONGED_POOR_EATING: 'Ăn kém kéo dài', WEIGHT_LOSS: 'Sụt cân' }

export const label = (map, code) => (code == null ? '—' : (map[code] ?? code))

// Món đang hiệu lực hiển thị theo nội dung `effective`; món Nháp chưa có thì dùng bản sửa chờ
export function dishContent(dish) {
  return dish.effective ?? dish.pendingRevision ?? null
}

// Lệnh trên món theo bảng trạng thái (FR-004, FR-004a). Bếp lập, Bác sĩ xác nhận; backend kiểm lại.
export function dishCommands(dish, { kitchen, doctor }) {
  const out = []
  const s = dish.status
  if (kitchen && s === 'DRAFT') out.push('submit', 'cancel')
  if (kitchen && s === 'PENDING_CONFIRMATION') out.push('withdraw')
  if (doctor && s === 'PENDING_CONFIRMATION') out.push('confirm', 'return')
  if (s === 'ACTIVE' && dish.pendingRevision) {
    if (kitchen) out.push('withdraw-revision')
    if (doctor) out.push('confirm-revision', 'return-revision')
  }
  if (s === 'ACTIVE' && (kitchen || doctor)) out.push('deactivate')
  return out
}

export const DISH_COMMAND = {
  submit: 'Gửi xác nhận',
  cancel: 'Hủy món',
  withdraw: 'Rút lại',
  confirm: 'Xác nhận món',
  return: 'Trả lại…',
  'confirm-revision': 'Xác nhận bản sửa',
  'return-revision': 'Trả lại bản sửa…',
  'withdraw-revision': 'Rút bản sửa',
  deactivate: 'Ngừng hiệu lực…',
}

// Lệnh cần lý do (FR-004a: trả lại; ngừng)
export const DISH_REASON_COMMANDS = new Set(['return', 'return-revision', 'deactivate', 'cancel'])

export function menuCommands(menu, { kitchen, doctor }) {
  const s = menu.status
  const out = []
  if (kitchen && s === 'DRAFT') out.push('submit', 'cancel')
  if (kitchen && s === 'PENDING_PUBLICATION') out.push('withdraw')
  if (doctor && s === 'PENDING_PUBLICATION') out.push('publish', 'return')
  return out
}

export const MENU_COMMAND = {
  submit: 'Gửi công bố',
  cancel: 'Hủy thực đơn…',
  withdraw: 'Rút lại',
  publish: 'Công bố thực đơn',
  return: 'Trả lại…',
}

// Mã cảnh báo của lần kiểm tra cần xác nhận đã xem trước khi gửi, công bố (FR-025 b, c)
export function warningCodes(check) {
  return check?.warningCodes ?? []
}

// Ngày trong tuần bắt đầu từ thứ Hai (yyyy-MM-dd), ISO 1 = thứ Hai
export function weekDays(weekStart) {
  const [y, m, d] = weekStart.split('-').map(Number)
  return Array.from({ length: 7 }, (_, i) => new Date(Date.UTC(y, m - 1, d + i)).toISOString().slice(0, 10))
}

const WEEKDAY = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật']

export function weekdayLabel(date) {
  const [y, m, d] = date.split('-').map(Number)
  const iso = ((new Date(Date.UTC(y, m - 1, d)).getUTCDay() + 6) % 7) + 1
  return `${WEEKDAY[iso - 1]}, ${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}`
}

// Thứ Hai của tuần chứa ngày
export function mondayOf(date) {
  const [y, m, d] = date.split('-').map(Number)
  const day = new Date(Date.UTC(y, m - 1, d))
  const shift = (day.getUTCDay() + 6) % 7
  return new Date(Date.UTC(y, m - 1, d - shift)).toISOString().slice(0, 10)
}

export function addDays(date, days) {
  const [y, m, d] = date.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10)
}

// Món của một ô (ngày, bữa, chế độ ăn) trong nội dung thực đơn
export function cellDishes(content, date, mealId, dietId) {
  return (content?.dishes ?? []).filter((r) => r.date === date && r.mealId === mealId && r.dietId === dietId)
}

// Tổng suất theo chế độ ăn của kết quả tính sẵn / chốt (cộng các tầng/khu)
export function portionsByDiet(result) {
  const totals = new Map()
  for (const p of result?.portionsByFloorArea ?? []) totals.set(p.dietId, (totals.get(p.dietId) ?? 0) + p.portionCount)
  return [...totals.entries()].map(([dietId, portionCount]) => ({ dietId, portionCount }))
}

// Lệnh trên phiếu theo trạng thái và phía xem (bếp hay tầng; FR-047, FR-048)
export function slipCommands(slip, { kitchen, receiver }) {
  const s = slip.status
  const out = []
  if (kitchen && s === 'CLOSED') out.push('mark-prepared')
  if (kitchen && s === 'PREPARED') out.push('deliver')
  if (kitchen && s === 'HAS_DISCREPANCY') out.push('resolve-discrepancy')
  if (receiver && s === 'DELIVERED') out.push('receive', 'report-discrepancy')
  return out
}

export const SLIP_COMMAND = {
  'mark-prepared': 'Đánh dấu đã chuẩn bị',
  deliver: 'Giao phiếu',
  receive: 'Nhận phiếu',
  'report-discrepancy': 'Báo sai lệch…',
  'resolve-discrepancy': 'Ghi xử lý sai lệch…',
}

// Lệnh trên đồ ăn gia đình theo trạng thái (FR-055)
export function familyFoodCommands(food) {
  switch (food.status) {
    case 'NEEDS_CONFIRMATION':
      return ['approve-use', 'reject-use']
    case 'NOT_ALLOWED':
      return ['record-disposal']
    case 'ALLOWED':
      return ['end']
    default:
      return []
  }
}

export const FAMILY_FOOD_COMMAND = {
  'approve-use': 'Cho sử dụng',
  'reject-use': 'Không cho sử dụng…',
  'record-disposal': 'Ghi xử lý…',
  end: 'Kết thúc…',
}

// Bản gán hiện hành (Hiệu lực) và bản chờ (Đề xuất, Chờ hiệu lực) của một người
export function assignmentState(assignments) {
  const list = assignments ?? []
  return {
    current: list.find((a) => a.status === 'ACTIVE') ?? null,
    pending: list.filter((a) => a.status === 'PROPOSED' || a.status === 'PENDING_EFFECTIVE'),
    history: list.filter((a) => a.status === 'DISCONTINUED' || a.status === 'CANCELLED'),
  }
}

// Tên tầng/khu từ danh mục của 003; không đọc được thì hiện mã
export function floorName(floors, id) {
  return floors.find((f) => f.id === id)?.name ?? `Tầng/khu mã ${id}`
}
