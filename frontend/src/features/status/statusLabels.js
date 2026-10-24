// Nhãn tiếng Việt cho lệnh trạng thái người cao tuổi (001 US4; hợp đồng api-001, schema UserCommand).

export const COMMAND_LABELS = {
  COMPLETE_ADMISSION: 'Hoàn tất tiếp nhận',
  CANCEL_ADMISSION: 'Hủy tiếp nhận',
  GRANT_TEMPORARY_ABSENCE: 'Cho tạm vắng',
  RECORD_RETURN: 'Ghi nhận trở về',
  TRANSFER_OUT: 'Chuyển viện',
  END_RESIDENCY: 'Kết thúc lưu trú',
  RECORD_DEATH: 'Ghi nhận qua đời',
}

// Tên lệnh trong lịch sử, gồm cả lệnh do hệ thống ghi (điểm danh của chuyến đi, 014) và Tạo hồ sơ
export const HISTORY_COMMAND_LABELS = {
  ...COMMAND_LABELS,
  CREATE_PROFILE: 'tạo hồ sơ',
  ROLL_CALL_DEPARTURE: 'điểm danh rời viện',
  ROLL_CALL_RETURN: 'điểm danh về',
  CANCEL_DEPARTURE_RECORD: 'hủy ghi nhận rời viện',
}

// Thứ tự nút: lệnh thường dùng trước, lệnh kết thúc sau cùng
export const COMMAND_ORDER = [
  'COMPLETE_ADMISSION',
  'GRANT_TEMPORARY_ABSENCE',
  'RECORD_RETURN',
  'TRANSFER_OUT',
  'CANCEL_ADMISSION',
  'END_RESIDENCY',
  'RECORD_DEATH',
]

// Câu nêu tác động trước khi bấm xác nhận (research H-02 mục 4)
export const COMMAND_EFFECTS = {
  COMPLETE_ADMISSION: 'Người cao tuổi chuyển sang Đang lưu trú; lịch chăm sóc, thuốc, suất ăn tính từ lúc này.',
  CANCEL_ADMISSION: 'Hồ sơ chuyển sang Hủy tiếp nhận và chỉ còn xem được.',
  GRANT_TEMPORARY_ABSENCE: 'Người cao tuổi chuyển sang Tạm vắng; công việc, suất ăn, liều thuốc trong thời gian vắng được xử lý.',
  RECORD_RETURN: 'Người cao tuổi trở lại Đang lưu trú.',
  TRANSFER_OUT: 'Người cao tuổi chuyển sang Điều trị tại bệnh viện; người liên hệ chính được báo ở mức Khẩn cấp.',
  END_RESIDENCY: 'Hồ sơ chuyển sang Kết thúc lưu trú và chỉ còn xem được; lịch tương lai bị hủy, giường được giải phóng.',
  RECORD_DEATH: 'Hồ sơ chuyển sang Qua đời và chỉ còn xem được; người liên hệ chính được báo ở mức Khẩn cấp.',
}

export const FINAL_STATUSES = ['RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED']

export const BASIS_KINDS = [
  { value: 'incidentId', label: 'Sự cố', hint: 'Mã sự cố', numeric: true },
  { value: 'doctorOrder', label: 'Chỉ định của Bác sĩ', hint: 'Nội dung chỉ định' },
  { value: 'externalInfo', label: 'Thông tin gia đình, cơ sở y tế', hint: 'Nội dung thông tin' },
]
