// Nhãn tiếng Việt cho tab Nguyện vọng (001 T070, T080; hợp đồng api-001, schema EndOfLifeWish, TreatmentWish).

export const CHOICE_LABELS = {
  TRANSFER_TO_HOSPITAL: 'Chuyển bệnh viện điều trị tích cực',
  RETURN_HOME: 'Đưa về nhà',
  STAY_PALLIATIVE: 'Ở lại viện chăm sóc giảm nhẹ',
}

export const SIGNER_LABELS = {
  RESIDENT: 'Người cao tuổi',
  PATIENT_REPRESENTATIVE: 'Người đại diện của người bệnh',
  LEGAL_REPRESENTATIVE: 'Người đại diện theo pháp luật',
}

export const SELECTION_LABELS = {
  CHOSEN_BY_RESIDENT: 'Do người cao tuổi chọn',
  CHOSEN_BY_FAMILY: 'Do thành viên gia đình chọn',
}

export const SOURCE_LABELS = {
  ADMISSION: 'Khi tiếp nhận',
  FAMILY_REQUEST: 'Theo yêu cầu của gia đình',
  CRITICAL_RECONFIRMATION: 'Xác nhận lại lúc nguy kịch',
}

export const END_OF_LIFE_STATUS_LABELS = {
  ACTIVE: 'Hiệu lực',
  SUPERSEDED: 'Được thay thế',
}

export const TREATMENT_STATUS_LABELS = {
  ACTIVE: 'Hiệu lực',
  EXPIRED: 'Hết hiệu lực',
}

export const ESCORT_LABELS = {
  FAMILY: 'Gia đình',
  FACILITY_STAFF: 'Viện cử nhân viên',
  CASE_BY_CASE: 'Tùy tình huống',
}

export const OPINION_SOURCE_LABELS = {
  RESIDENT: 'Người cao tuổi',
  FAMILY_MEMBER: 'Người thân',
  OBSERVING_STAFF: 'Nhân viên quan sát',
}

// Mã lỗi nghiệp vụ → câu hiện ở ô tương ứng (backend trả 409/422)
export const WISH_ERRORS = {
  INVALID_SIGNER: ['signerId', 'Người ký phải khớp nhận định và là người thân có quan hệ Hiệu lực với người cao tuổi'],
  INVALID_RESPONDENT: ['signerId', 'Người trả lời phải là người thân có quan hệ Hiệu lực với người cao tuổi'],
  COURT_DECISION_REQUIRED: ['courtNumber', 'Cần đủ số, ngày, Tòa án và tệp quyết định của Tòa án'],
  REPRESENTATIVE_SELECTION_REQUIRED: ['selection', 'Chọn cách người đại diện được chọn'],
  SELF_DETERMINATION_ASSESSMENT_REQUIRED: ['supportMethod', 'Nhập cách hỗ trợ hiểu thông tin và ngày xem lại'],
  INVALID_REVIEW_DATE: ['reviewDate', 'Ngày xem lại không được trước hôm nay'],
  INVALID_ASSESSING_DOCTOR: ['doctorId', 'Người nhận định phải là Bác sĩ'],
  FIRST_CONTACT_REQUIRED: ['firstContact', 'Nhập người cần liên hệ trước khi thực hiện'],
  EVIDENCE_REQUIRED: ['signedCopy', 'Chọn tệp bản ký đã scan'],
  PREFERRED_HOSPITAL_REQUIRED: ['hospital1', 'Nhập tên và địa chỉ bệnh viện ưu tiên'],
  TOO_MANY_PREFERRED_HOSPITALS: ['hospital1', 'Tối đa hai bệnh viện ưu tiên'],
  INVALID_FAMILY_MEMBER: ['notifyIds', 'Người cần báo phải là người thân có quan hệ Hiệu lực với người cao tuổi'],
  INVALID_OPINION_SOURCE: ['opinions', 'Nguồn ý kiến không hợp lệ: người thân phải có quan hệ Hiệu lực'],
}

// Hồ sơ trạng thái cuối: không ghi phiên bản mới (BR-M01-05), phiên bản Hiệu lực vẫn xem được (FR-023f)
export const FINAL_STATUSES = ['RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED']
