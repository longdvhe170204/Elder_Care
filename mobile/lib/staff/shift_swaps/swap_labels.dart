import '../../common/widgets/status_pill.dart';

/// Nhãn trạng thái, dạng, lệnh của đổi ca và nghỉ đột xuất (015; glossary mục 21 của 000), cùng chữ với màn web.
const swapStatusLabels = {
  'AWAITING_RECIPIENT': 'Chờ đồng ý',
  'PENDING_APPROVAL': 'Chờ duyệt',
  'APPLIED': 'Đã áp dụng',
  'PARTIALLY_APPLIED': 'Đã duyệt một phần',
  'REJECTED': 'Từ chối',
  'CANCELLED': 'Đã hủy',
  'RECIPIENT_DECLINED': 'Người nhận từ chối',
  'EXPIRED': 'Hết hiệu lực',
};

const swapKindLabels = {'MUTUAL': 'Đổi hai chiều', 'TAKE_OVER': 'Nhận thay'};
const leaveKindLabels = {'BY_SHIFT': 'Nghỉ theo ca', 'BY_DAY': 'Nghỉ theo ngày'};

/// Lệnh → (nhãn nút, cần lý do).
const swapCommands = {
  'accept': ('Đồng ý', false),
  'decline': ('Từ chối', true),
  'withdraw-acceptance': ('Rút đồng ý', false),
  'cancel': ('Rút yêu cầu', true),
  'approve': ('Duyệt', false),
  'reject': ('Từ chối duyệt', true),
};

const leaveCommands = {'approve': ('Duyệt', false), 'reject': ('Từ chối', true), 'cancel': ('Hủy yêu cầu', true)};

StatusTone swapTone(String status) => switch (status) {
  'AWAITING_RECIPIENT' || 'PENDING_APPROVAL' => StatusTone.attention,
  'APPLIED' || 'PARTIALLY_APPLIED' => StatusTone.settled,
  _ => StatusTone.neutral,
};
