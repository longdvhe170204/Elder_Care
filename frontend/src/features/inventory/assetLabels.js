// Nhãn và hàm thuần của tài sản (019 US4; spec FR-011 → FR-016; api-019 Asset; DESIGN.md 3.5).

export const ASSET_STATUS_LABELS = {
  READY: 'Sẵn sàng',
  IN_USE: 'Đang sử dụng',
  DAMAGED: 'Hỏng',
  UNDER_MAINTENANCE: 'Đang bảo trì',
  DISPOSED: 'Đã thanh lý',
}

export const ASSET_FILTERS = [
  ['ALL', 'Tất cả', null],
  ['READY', 'Sẵn sàng', ['READY']],
  ['IN_USE', 'Đang sử dụng', ['IN_USE']],
  ['DAMAGED', 'Hỏng', ['DAMAGED']],
  ['UNDER_MAINTENANCE', 'Đang bảo trì', ['UNDER_MAINTENANCE']],
  ['DISPOSED', 'Đã thanh lý', ['DISPOSED']],
]

// Dấu của tài sản (FR-014, FR-015)
export const ASSET_FLAG_LABELS = {
  MAINTENANCE_OVERDUE: 'Quá hạn bảo trì',
  VEHICLE_INSPECTION_OVERDUE: 'Quá hạn đăng kiểm',
  INSURANCE_OVERDUE: 'Quá hạn bảo hiểm',
  AWAITING_RESIDENT_TRANSFER: 'Chờ chuyển người',
}

export const SOURCES = [
  ['PURCHASED', 'Mua mới'],
  ['DONATED', 'Được tặng'],
  ['TRANSFERRED_IN', 'Chuyển từ nơi khác'],
]

export const IMPACT_LEVELS = [
  ['UNSAFE', 'Mất an toàn'],
  ['STILL_SAFE', 'Vẫn an toàn'],
]

export const MAINTENANCE_KINDS = [
  ['MAINTENANCE', 'Bảo trì'],
  ['INSPECTION', 'Kiểm định'],
  ['VEHICLE_INSPECTION', 'Đăng kiểm'],
  ['INSURANCE', 'Bảo hiểm'],
]

export const MAINTENANCE_RESULTS = [
  ['PASS', 'Đạt'],
  ['FAIL', 'Không đạt'],
]

// Loại vị trí chuyển tới (FR-012); phòng, tầng, khu vực chọn theo danh mục của 003
export const LOCATION_TYPES = [
  ['ROOM', 'Phòng'],
  ['FLOOR', 'Tầng'],
  ['AREA', 'Khu vực'],
  ['PARKING_LOT', 'Bãi xe'],
  ['ASSET_STORAGE', 'Kho tài sản'],
]

// Loại hệ thống mang mã BED (Giường), VEHICLE (Xe) ở GET /asset-types
export const BED_CODE = 'BED'
export const VEHICLE_CODE = 'VEHICLE'

export function statusLabel(code) {
  return ASSET_STATUS_LABELS[code] ?? code
}

export function flagLabels(flags = []) {
  return flags.map((f) => ASSET_FLAG_LABELS[f] ?? f)
}

const wholeNumber = (text) => (/^\d+$/.test(String(text).trim()) ? Number(String(text).trim()) : null)

export { wholeNumber }
