import '../widgets/status_pill.dart';

// Nhãn tiếng Việt dùng chung cho hồ sơ người cao tuổi ở phần nhân viên và người thân (hợp đồng api-001; cùng chữ với
// frontend `residentLabels.js`). Đưa lên `common/` 2026-11-07; nhãn chỉ phần nhân viên dùng giữ ở `staff/residents/`.

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

const genderLabels = {'MALE': 'Nam', 'FEMALE': 'Nữ'};

const careLevelLabels = {
  'BASIC': 'Chăm sóc cơ bản',
  'REGULAR': 'Chăm sóc thường xuyên',
  'SPECIAL': 'Chăm sóc đặc biệt',
};

/// Nhóm mục sức khỏe, theo thứ tự hiện: dị ứng trước để không chìm giữa các mục khác (DESIGN.md 7.2).
const healthTypeLabels = {'ALLERGY': 'Dị ứng', 'CHRONIC_CONDITION': 'Bệnh nền', 'MEDICAL_HISTORY': 'Tiền sử bệnh'};

String statusLabel(String status) => statusLabels[status] ?? status;

/// Tông theo DESIGN.md 3.5, cùng ánh xạ với `StatusBadge` của web; trạng thái khác giữ neutral.
StatusTone statusTone(String status) => switch (status) {
  'IN_RESIDENCE' => StatusTone.settled,
  'ADMISSION_IN_PROGRESS' || 'TEMPORARILY_ABSENT' || 'HOSPITALIZED' => StatusTone.attention,
  _ => StatusTone.neutral,
};
