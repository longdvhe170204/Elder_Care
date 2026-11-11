export { COST_TYPE } from '@/common/costLabels'
export const STATEMENT_STATUS = { OPEN: 'Đang mở', AWAITING_FINALIZATION: 'Chờ chốt', FINALIZED: 'Đã chốt' }
export const ITEM_STATUS = {
  DRAFT: 'Nháp',
  CHECKED: 'Đã kiểm tra',
  APPROVED: 'Đã duyệt',
  FINALIZED: 'Đã chốt',
  CANCELLED: 'Đã hủy',
}
export const ORIGIN = {
  EVENT_BASED: 'Theo sự kiện',
  MANUAL: 'Nhập tay',
  PURCHASE_ASSISTANCE: 'Mua hộ',
  ADJUSTMENT: 'Điều chỉnh',
}
export const FLAGS = {
  INCLUDED_IN_PACKAGE: 'Thuộc gói',
  MISSING_UNIT_PRICE: 'Thiếu đơn giá',
  NOT_YET_MOVED_IN: 'Chưa vào ở',
  CREATED_IN_ADVANCE: 'Lập trước',
  AFTER_FINALIZED_PERIOD: 'Thuộc kỳ đã chốt',
  EXCEEDS_CONSENT: 'Vượt số đã đồng ý',
  URGENT_EXCESS: 'Mua gấp vượt số đã đồng ý',
  AFFECTS_TERMINATION: 'Ảnh hưởng kết thúc lưu trú',
}
export const COMMAND = {
  CHECK: 'Kiểm tra',
  UNCHECK: 'Bỏ kiểm tra',
  APPROVE: 'Duyệt',
  RETURN: 'Trả lại',
  CANCEL: 'Hủy',
  FINALIZE: 'Chốt',
}
const MONEY = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
export const money = (value) => (value == null ? 'Chưa có số tiền' : MONEY.format(value))
export const periodLabel = (value) => (value ? `${value.slice(5)}/${value.slice(0, 4)}` : '')
export const controlClass = 'min-h-11 rounded-md border border-input bg-popover px-3 type-body-sm'
export const excess = (item) => item?.flags?.some((flag) => ['EXCEEDS_CONSENT', 'URGENT_EXCESS'].includes(flag))
