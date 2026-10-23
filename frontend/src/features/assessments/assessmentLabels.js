// Nhãn tiếng Việt cho đánh giá và mức chăm sóc (001 US3; FR-024 → FR-038). Tên trường theo hợp đồng api-001
// (Assessment, AssessmentDetail, CareLevelAtDate). Không có điểm, mức đề xuất hay nhãn năng lực (DESIGN.md 2.2).

export const CARE_LEVEL_LABELS = {
  BASIC: 'Chăm sóc cơ bản',
  REGULAR: 'Chăm sóc thường xuyên',
  SPECIAL: 'Chăm sóc đặc biệt',
}

export const CARE_LEVEL_ORDER = ['BASIC', 'REGULAR', 'SPECIAL']

export const ASSESSMENT_TYPE_LABELS = {
  INITIAL: 'Đánh giá đầu vào',
  REASSESSMENT: 'Đánh giá lại',
}

// Nhãn trạng thái dùng StatusBadge (Bản nháp, Đã hủy giữ tông neutral).
export const ASSESSMENT_STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  CONFIRMED: 'Đã xác nhận',
  DISCARDED: 'Đã hủy',
}

export const RESIDENCE_TYPE_LABELS = {
  LONG_TERM_RESIDENTIAL: 'Nội trú dài hạn',
  SHORT_TERM_RESIDENTIAL: 'Nội trú ngắn ngày',
  DAY_CARE: 'Bán trú',
}

export const BASIS_TYPE_LABELS = {
  ASSESSMENT: 'theo lần đánh giá',
  RESIDENCY_CHANGE_REQUEST: 'theo yêu cầu thay đổi lưu trú',
}

// Loại bản ghi của 000 và các ô đính chính được (FR-032). Form đính chính dùng chung gửi mọi ô: nhận xét và mức để
// trống là giữ nguyên, lưu ý chăm sóc để trống là bỏ lưu ý.
export const ASSESSMENT_RECORD_TYPE = 'M01.ASSESSMENT'

export const ASSESSMENT_CORRECTION_FIELDS = [
  { name: 'comment', label: 'Nhận xét đánh giá (để trống nếu giữ nguyên)' },
  { name: 'careNote', label: 'Lưu ý chăm sóc (để trống nếu bỏ lưu ý)' },
  {
    name: 'careLevel',
    label: 'Mức chăm sóc: BASIC = cơ bản, REGULAR = thường xuyên, SPECIAL = đặc biệt (để trống nếu giữ nguyên)',
  },
]

// Trạng thái người cao tuổi cho phép lập từng loại đánh giá (FR-035); backend vẫn kiểm lại.
export const REASSESSABLE_STATUSES = ['IN_RESIDENCE', 'TEMPORARILY_ABSENT', 'OFF_SITE_ACTIVITY']

// Căn cứ báo đánh giá lại (001 US6; FR-039 → FR-042; contracts ReassessmentStatus.basis[].basis).
export const REASSESSMENT_BASIS_LABELS = {
  PERIODIC: 'Định kỳ',
  AFTER_INCIDENT: 'Sau sự cố',
  RETURN_FROM_HOSPITAL: 'Trở về từ bệnh viện',
  ASSESSMENT_CORRECTION: 'Đính chính đánh giá',
}
