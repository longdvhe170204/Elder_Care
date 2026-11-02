import '../widgets/status_pill.dart';

/// Nhãn của hợp đồng api-009, cùng chữ với web (`notificationLabels.js`).
const levelLabels = {'URGENT': 'Khẩn cấp', 'MEDIUM': 'Trung bình', 'LOW': 'Nhẹ'};

const statusLabels = {
  'HELD_FOR_QUIET_HOURS': 'Chờ hết giờ yên tĩnh',
  'NOT_SENT': 'Không gửi',
  'SENT': 'Đã gửi',
  'DELIVERY_FAILED': 'Gửi thất bại',
  'VIEWED': 'Đã xem',
  'ACKNOWLEDGED': 'Đã xác nhận',
  'UNREACHABLE': 'Không liên lạc được',
};

const partOrder = ['GENERAL', 'HEALTH', 'COSTS', 'VISITS', 'CONTRACT'];

const partLabels = {
  'GENERAL': 'Phần chung',
  'HEALTH': 'Phần sức khỏe',
  'COSTS': 'Chi phí',
  'VISITS': 'Thăm',
  'CONTRACT': 'Hợp đồng',
};

/// Tông theo DESIGN.md 3.5.
StatusTone levelTone(String level) => switch (level) {
  'URGENT' => StatusTone.urgent,
  'MEDIUM' => StatusTone.attention,
  _ => StatusTone.neutral,
};

StatusTone statusTone(String status) => switch (status) {
  'VIEWED' || 'ACKNOWLEDGED' => StatusTone.settled,
  'DELIVERY_FAILED' || 'UNREACHABLE' => StatusTone.attention,
  _ => StatusTone.neutral,
};
