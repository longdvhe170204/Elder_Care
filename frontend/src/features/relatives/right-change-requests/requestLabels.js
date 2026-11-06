import { RIGHT_LABELS } from '../relationshipLabels'

// Nhãn của yêu cầu thay đổi quyền và mong muốn của người cao tuổi (012 T017; api-012 RightChangeRequest, VisitPickupWish;
// glossary 18). Hàm thuần, không JSX.

export const REQUEST_STATUS_LABELS = {
  PENDING_CONFIRMATION: 'Chờ xác nhận',
  EFFECTIVE: 'Hiệu lực',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
}

export const REQUEST_SOURCE_LABELS = {
  COUNTER: 'Lập tại quầy',
  PORTAL: 'Người thân lập qua cổng',
  REGISTRATION_FORM: 'Phiếu đăng ký',
}

export const CONFIRMATION_LABELS = {
  PORTAL: 'Người đại diện xác nhận qua cổng',
  SIGNED_FORM: 'Bản ký của người đại diện',
  MANAGER_APPROVAL: 'Quản lý viện duyệt',
}

// Lỗi 409, 403 của lệnh trên yêu cầu: câu nói rõ điều kiện (bổ sung cho detail của backend)
export const REQUEST_HINTS = {
  STATE_CHANGED: 'Yêu cầu đã có kết quả hoặc quyền đã đổi. Tải lại danh sách rồi thực hiện lại.',
  DUPLICATE_PENDING_REQUEST: 'Quyền này đã có một yêu cầu Chờ xác nhận. Xử lý yêu cầu đó trước.',
  ID_DOCUMENT_REQUIRED: 'Người được phép đón phải có loại và số giấy tờ tùy thân.',
  CONSENT_NOT_INCLUDED: 'Bản đồng ý của người cao tuổi chưa gồm người thân này nên không bật được quyền xem sức khỏe.',
  DBR_02_VIOLATED: 'Người cao tuổi phải còn ít nhất một người đại diện (DBR-02).',
  IS_CONSENTER: 'Người đại diện này là người ký bản đồng ý đang hiệu lực; cần bản đồng ý thay thế trước.',
  REQUEST_NOT_ALLOWED: 'Người ký phải là người đại diện đang hiệu lực của người cao tuổi.',
  NOMINEE_NOT_CONFIRMED: 'Người bị đặt hoặc thôi làm người đại diện không xác nhận được yêu cầu của chính mình.',
  WISH_CONFLICT_REQUIRES_MANAGER: 'Yêu cầu trái mong muốn của người cao tuổi chỉ có hiệu lực khi Quản lý viện duyệt.',
}

export const WISH_TYPE_LABELS = { VISIT: 'đến thăm', PICKUP: 'đón' }

function personOf(request, relationships) {
  if (request.newPicker?.fullName) return request.newPicker.fullName
  return relationships.find((r) => r.id === request.relationshipId)?.familyMember.fullName ?? 'người thân'
}

// Nội dung yêu cầu viết thành câu ngắn ("Tắt quyền Được phép đón cho Trần Thu Hà")
export function describeRequest(request, relationships) {
  const value = request.newValue ? 'Bật' : 'Tắt'
  switch (request.type) {
    case 'RIGHT':
      return `${value} quyền ${RIGHT_LABELS[request.right] ?? request.right} cho ${personOf(request, relationships)}`
    case 'SELF_RETURN':
      return `${value} dấu được tự về`
    case 'SET_REPRESENTATIVE':
      return `Đặt người đại diện: ${personOf(request, relationships)}`
    case 'REMOVE_REPRESENTATIVE':
      return `Thôi người đại diện: ${personOf(request, relationships)}`
    default:
      return request.type
  }
}

// Câu của một mong muốn ("Không muốn Trần Thu Hà đến thăm")
export function describeWish(wish, relationships) {
  const name =
    wish.fullName ?? relationships.find((r) => r.id === wish.relationshipId)?.familyMember.fullName ?? 'người này'
  return `${wish.wanted ? 'Muốn' : 'Không muốn'} ${name} ${WISH_TYPE_LABELS[wish.type] ?? wish.type}`
}

// Nhãn người nói theo OpinionSource (DESIGN.md 2.2, source-tag)
export function wishSource(wish, relationships) {
  switch (wish.source?.type) {
    case 'RESIDENT':
      return { source: 'resident' }
    case 'FAMILY_MEMBER': {
      const r = relationships.find((x) => x.familyMember.id === wish.source.familyMemberId)
      return { source: 'family', relative: r ? r.familyMember.fullName : 'người thân' }
    }
    default:
      return { source: 'staff' }
  }
}
