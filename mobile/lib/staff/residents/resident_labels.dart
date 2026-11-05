import '../../common/widgets/status_pill.dart';

// Nhãn tiếng Việt cho mã của hồ sơ người cao tuổi (hợp đồng api-001; cùng chữ với frontend `residentLabels.js`,
// `assessmentLabels.js`).

const statusLabels = {
  'ADMISSION_IN_PROGRESS': 'Đang tiếp nhận',
  'IN_RESIDENCE': 'Đang lưu trú',
  'TEMPORARILY_ABSENT': 'Tạm vắng',
  'OFF_SITE_ACTIVITY': 'Đi hoạt động ngoài viện',
  'HOSPITALIZED': 'Điều trị tại bệnh viện',
  'RESIDENCY_ENDED': 'Kết thúc lưu trú',
  'DECEASED': 'Qua đời',
  'ADMISSION_CANCELLED': 'Hủy tiếp nhận',
};

/// Các mục lọc trạng thái của thanh lọc, cùng thứ tự với màn web.
const statusFilters = ['ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', 'TEMPORARILY_ABSENT', 'HOSPITALIZED'];

const genderLabels = {'MALE': 'Nam', 'FEMALE': 'Nữ'};

const idDocumentLabels = {'CITIZEN_ID': 'CCCD', 'OTHER': 'Giấy tờ khác'};

const careLevelLabels = {
  'BASIC': 'Chăm sóc cơ bản',
  'REGULAR': 'Chăm sóc thường xuyên',
  'SPECIAL': 'Chăm sóc đặc biệt',
};

const residenceTypeLabels = {
  'LONG_TERM_RESIDENTIAL': 'Nội trú dài hạn',
  'SHORT_TERM_RESIDENTIAL': 'Nội trú ngắn ngày',
  'DAY_CARE': 'Bán trú',
};

String statusLabel(String status) => statusLabels[status] ?? status;

/// Tông theo DESIGN.md 3.5, cùng ánh xạ với `StatusBadge` của web; trạng thái khác giữ neutral.
StatusTone statusTone(String status) => switch (status) {
  'IN_RESIDENCE' => StatusTone.settled,
  'ADMISSION_IN_PROGRESS' || 'TEMPORARILY_ABSENT' || 'HOSPITALIZED' => StatusTone.attention,
  _ => StatusTone.neutral,
};
