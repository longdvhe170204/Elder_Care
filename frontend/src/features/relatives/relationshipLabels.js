// Nhãn của quan hệ người thân (012; api-012 Relationship, glossary 18.3). Hàm thuần, không JSX.

export const RELATIONSHIP_LABELS = {
  SPOUSE: 'Vợ/chồng',
  CHILD: 'Con',
  GRANDCHILD: 'Cháu',
  SIBLING: 'Anh chị em',
  OTHER: 'Khác',
}

// Thứ tự sáu quyền theo FR-007
export const RIGHT_ORDER = [
  'VIEW_HEALTH',
  'VIEW_COSTS',
  'RECEIVE_URGENT',
  'REGISTER_VISITS',
  'PICKUP_ALLOWED',
  'REQUEST_SERVICES',
]

export const RIGHT_LABELS = {
  VIEW_HEALTH: 'Xem sức khỏe',
  VIEW_COSTS: 'Xem chi phí',
  RECEIVE_URGENT: 'Nhận thông báo khẩn',
  REGISTER_VISITS: 'Đăng ký thăm',
  PICKUP_ALLOWED: 'Được phép đón',
  REQUEST_SERVICES: 'Được yêu cầu thay đổi dịch vụ',
}

// Lỗi 409 của lệnh trên quan hệ: câu nói rõ điều kiện (bổ sung cho detail của backend)
export const CONFLICT_HINTS = {
  DBR_02_VIOLATED: 'Người cao tuổi phải có đúng một người liên hệ chính và ít nhất một người đại diện (DBR-02).',
  REPRESENTATIVE_ALREADY_EXISTS:
    'Người cao tuổi đã có người đại diện. Đặt thêm người đại diện đi qua yêu cầu thay đổi quyền.',
  STATE_CHANGED: 'Quan hệ đã thay đổi. Tải lại danh sách rồi thực hiện lại.',
  ID_DOCUMENT_REQUIRED: 'Người được phép đón phải có loại và số giấy tờ tùy thân.',
  CONSENT_NOT_INCLUDED: 'Bản đồng ý của người cao tuổi chưa gồm người thân này nên không bật được quyền xem sức khỏe.',
  REQUEST_NOT_ALLOWED: 'Người ký phiếu phải là người đại diện; quyền yêu cầu thay đổi dịch vụ chỉ cho người đại diện.',
}

export function rolesOf(relationship) {
  return [relationship.representative && 'Người đại diện', relationship.primaryContact && 'Người liên hệ chính']
    .filter(Boolean)
    .join(', ')
}

// Sáu quyền: "Có", "Không" hoặc "Có, không có tác dụng" khi bật mà không có tác dụng (FR-011)
export function rightLines(relationship) {
  return RIGHT_ORDER.map((right) => {
    const state = relationship.rights?.[right] ?? { enabled: false, effective: false }
    const value = !state.enabled ? 'Không' : state.effective ? 'Có' : 'Có, không có tác dụng'
    return { right, label: RIGHT_LABELS[right], value }
  })
}

// Tên các quyền đang bật, cho cột của bảng
export function enabledRightsText(relationship) {
  const enabled = rightLines(relationship).filter((line) => line.value !== 'Không')
  if (enabled.length === 0) return 'Chưa bật quyền nào'
  return enabled.map((line) => (line.value === 'Có' ? line.label : `${line.label} (không có tác dụng)`)).join(', ')
}
