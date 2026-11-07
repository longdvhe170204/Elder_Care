import '../../common/widgets/status_pill.dart';

/// Nhãn trạng thái ca theo 008 FR-022, cùng chữ với web; không suy trạng thái từ đồng hồ máy.
(String, StatusTone) shiftStatusLabel(String status) => switch (status) {
  'DRAFT' => ('Nháp', StatusTone.neutral),
  'PUBLISHED' => ('Đã công bố', StatusTone.settled),
  'IN_PROGRESS' => ('Đang diễn ra', StatusTone.settled),
  'AWAITING_HANDOVER' => ('Chờ bàn giao', StatusTone.attention),
  'CLOSED' => ('Đã đóng', StatusTone.neutral),
  'CANCELLED' => ('Đã hủy', StatusTone.neutral),
  _ => ('Chưa có thông tin trạng thái', StatusTone.neutral),
};
