export '../../common/residents/resident_labels.dart';

// Nhãn riêng phần nhân viên cho hồ sơ người cao tuổi (hợp đồng api-001; cùng chữ với frontend `residentLabels.js`,
// `assessmentLabels.js`); nhãn dùng chung (trạng thái, giới tính, mức chăm sóc, nhóm sức khỏe, tông) ở `common/residents/`.

/// Các mục lọc trạng thái của thanh lọc, cùng thứ tự với màn web.
const statusFilters = ['ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', 'TEMPORARILY_ABSENT', 'HOSPITALIZED'];

const idDocumentLabels = {'CITIZEN_ID': 'CCCD', 'OTHER': 'Giấy tờ khác'};

const residenceTypeLabels = {
  'LONG_TERM_RESIDENTIAL': 'Nội trú dài hạn',
  'SHORT_TERM_RESIDENTIAL': 'Nội trú ngắn ngày',
  'DAY_CARE': 'Bán trú',
};

/// Lệnh trạng thái app nhân viên thực hiện được (001 T052), theo thứ tự nút của web; lệnh khác (Hoàn tất tiếp nhận, Hủy
/// tiếp nhận, Kết thúc lưu trú) làm trên web.
const commandLabels = {
  'GRANT_TEMPORARY_ABSENCE': 'Cho tạm vắng',
  'RECORD_RETURN': 'Ghi nhận trở về',
  'TRANSFER_OUT': 'Chuyển viện',
  'RECORD_DEATH': 'Ghi nhận qua đời',
};

/// Tên lệnh trong lịch sử, gồm lệnh do hệ thống ghi (điểm danh của chuyến đi, 014) và Tạo hồ sơ.
const historyCommandLabels = {
  'COMPLETE_ADMISSION': 'Hoàn tất tiếp nhận',
  'CANCEL_ADMISSION': 'Hủy tiếp nhận',
  'GRANT_TEMPORARY_ABSENCE': 'Cho tạm vắng',
  'RECORD_RETURN': 'Ghi nhận trở về',
  'TRANSFER_OUT': 'Chuyển viện',
  'END_RESIDENCY': 'Kết thúc lưu trú',
  'RECORD_DEATH': 'Ghi nhận qua đời',
  'CREATE_PROFILE': 'tạo hồ sơ',
  'ROLL_CALL_DEPARTURE': 'điểm danh rời viện',
  'ROLL_CALL_RETURN': 'điểm danh về',
  'CANCEL_DEPARTURE_RECORD': 'hủy ghi nhận rời viện',
};

/// Câu nêu tác động trước khi bấm xác nhận (research H-02 mục 4).
const commandEffects = {
  'GRANT_TEMPORARY_ABSENCE':
      'Người cao tuổi chuyển sang Tạm vắng; công việc, suất ăn, liều thuốc trong thời gian vắng được xử lý.',
  'RECORD_RETURN': 'Người cao tuổi trở lại Đang lưu trú.',
  'TRANSFER_OUT': 'Người cao tuổi chuyển sang Điều trị tại bệnh viện; người liên hệ chính được báo ở mức Khẩn cấp.',
  'RECORD_DEATH': 'Hồ sơ chuyển sang Qua đời và chỉ còn xem được; người liên hệ chính được báo ở mức Khẩn cấp.',
};

const finalStatuses = ['RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED'];

/// Căn cứ của Chuyển viện (`details.basis`): mã sự cố là số, hai loại kia là chữ.
const basisKinds = [
  (value: 'incidentId', label: 'Sự cố', hint: 'Mã sự cố', numeric: true),
  (value: 'doctorOrder', label: 'Chỉ định của Bác sĩ', hint: 'Nội dung chỉ định', numeric: false),
  (value: 'externalInfo', label: 'Thông tin gia đình, cơ sở y tế', hint: 'Nội dung thông tin', numeric: false),
];
