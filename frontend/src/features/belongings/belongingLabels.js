// Nhãn tiếng Việt cho enum của api-013 (013; tên trạng thái theo nghiep-vu.md 13.x, DESIGN.md 3.5) và định dạng số
// lượng, số tiền (NFR-11). Hàm thuần, không JSX.

export const STATUS_LABELS = {
  HELD: 'Đang giữ',
  IN_USE: 'Đang được người cao tuổi sử dụng',
  RETURNED: 'Đã trả',
  LOST: 'Thất lạc',
  DAMAGED: 'Hư hỏng',
  DISPOSED: 'Đã xử lý',
}

export const KIND_LABELS = {
  INTAKE: 'Tiếp nhận',
  GIVE_FOR_USE: 'Giao sử dụng',
  TAKE_BACK: 'Thu lại',
  TRANSFER_CUSTODY: 'Chuyển giữ',
  RETURN: 'Trả',
  REPORT_LOST: 'Báo thất lạc',
  RECORD_DAMAGE: 'Ghi hư hỏng',
  FOUND: 'Tìm thấy',
  DISPOSAL: 'Xử lý đồ',
  RETURN_BLOCKED: 'Lần chặn trả',
}

export const GIVER_LABELS = {
  FAMILY_MEMBER: 'Người thân',
  OTHER_PERSON: 'Người khác',
  RESIDENT: 'Người cao tuổi',
  STAFF: 'Nhân viên',
}

export const statusLabel = (code) => STATUS_LABELS[code] ?? code
export const kindLabel = (code) => KIND_LABELS[code] ?? code

export function holderLabel(holder) {
  if (holder === 'STAFF') return 'Nhân viên'
  if (holder === 'RESIDENT') return 'Người cao tuổi'
  return 'Không còn người giữ'
}

const number = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 })

// Đồ tiền mặt mang số tiền (cashAmount), đồ khác mang số lượng (quantity) kèm đơn vị nếu có (FR-005)
export function amountText(item, unit) {
  if (item.cashAmount != null) return `${number.format(item.cashAmount)} đồng`
  if (item.quantity == null) return ''
  return unit ? `${number.format(item.quantity)} ${unit}` : number.format(item.quantity)
}

// Dòng mô tả đồ trong danh sách chọn (form trả, form xác nhận người nhận khác): có mã đồ, số lượng và vị trí để phân biệt
// hai đồ cùng tên
export function pickLabel(item, locationName) {
  const parts = [item.description, amountText(item), `mã ${item.id}`]
  if (locationName) parts.push(locationName(item.storageLocationId))
  return parts.filter(Boolean).join(', ')
}

// Xác nhận người nhận khác có dùng được cho lần trả đang nhập không: chép đúng luật ReturnRecipient.matches của backend
// (FR-017, Q-253) để gợi ý; backend vẫn kiểm khi lưu. belongingIds của xác nhận là các đồ còn lại (api-013).
// Trả { fits, reason: 'person' | 'items' | 'pending' | 'expired' | 'status' | null, extraIds: đồ của xác nhận không trả lần này }
const normalize = (s) => (s ?? '').trim().replace(/\s+/g, ' ').toLowerCase()
const same = (a, b) => normalize(a) !== '' && normalize(a) === normalize(b)

export function confirmationFit(confirmation, { type, fullName, documentType, documentNumber, belongingIds, now }) {
  const person =
    type === 'RESIDENT'
      ? !!confirmation.recipientIsResident
      : !confirmation.recipientIsResident &&
        same(confirmation.recipientName, fullName) &&
        (same(confirmation.recipientIdDocument, documentNumber) ||
          (normalize(documentType) !== '' &&
            same(confirmation.recipientIdDocument, `${documentType.trim()} ${(documentNumber ?? '').trim()}`)))
  const covers = belongingIds.length > 0 && belongingIds.every((id) => confirmation.belongingIds.includes(id))
  const extraIds = confirmation.belongingIds.filter((id) => !belongingIds.includes(id))
  let reason = null
  if (!person) reason = 'person'
  else if (!covers) reason = 'items'
  else if (confirmation.status === 'PENDING_CONFIRMATION') reason = 'pending'
  else if (confirmation.status !== 'EFFECTIVE') reason = 'status'
  else if (!confirmation.expiresAt || new Date(confirmation.expiresAt) <= now) reason = 'expired'
  return { fits: reason === null, reason, extraIds: reason === null ? extraIds : [] }
}

// Giá trị của ô datetime-local (giờ Việt Nam) thành ISO-8601 có +07:00 (frontend/README luật 6)
export function toOffsetIso(local) {
  return `${local.length === 16 ? `${local}:00` : local}+07:00`
}

// Giờ hiện tại theo giờ Việt Nam cho ô datetime-local (người dùng sửa được; không phải hệ thống tự ghi thay)
export function nowLocal() {
  const parts = new Intl.DateTimeFormat('sv-SE', {
    timeZone: 'Asia/Ho_Chi_Minh',
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date())
  return parts.replace(' ', 'T')
}

// Mục danh mục còn dùng (không Ngừng) thành ô chọn
export const activeOptions = (items) =>
  items.filter((i) => i.status !== 'INACTIVE').map((i) => ({ value: String(i.id), label: i.name }))

// Lệnh bàn giao theo trạng thái đồ (data-model 013 mục 3; HandoverCommandCheck của backend)
const COMMANDS = {
  HELD: ['GIVE_FOR_USE', 'TRANSFER_CUSTODY', 'REPORT_LOST', 'RECORD_DAMAGE'],
  IN_USE: ['TAKE_BACK', 'REPORT_LOST', 'RECORD_DAMAGE'],
  DAMAGED: ['TRANSFER_CUSTODY', 'REPORT_LOST'],
  LOST: ['FOUND'],
}

export const commandsFor = (status) => COMMANDS[status] ?? []

// Trạng thái xác nhận người nhận khác (FR-017, data-model 013 mục 5)
export const CONFIRMATION_LABELS = {
  PENDING_CONFIRMATION: 'Chờ xác nhận',
  EFFECTIVE: 'Hiệu lực',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
  USED: 'Đã dùng',
  EXPIRED: 'Hết hạn',
}

// Kênh liên hệ người thân (api-013 ContactAttempt.channel)
export const CHANNEL_OPTIONS = [
  { value: 'PHONE_CALL', label: 'Gọi điện' },
  { value: 'TEXT_MESSAGE', label: 'Tin nhắn' },
  { value: 'PORTAL_NOTIFICATION', label: 'Thông báo trên cổng' },
]
