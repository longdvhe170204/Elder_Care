import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/notifications/notification_inbox.dart';
import '../../common/notifications/push_coordinator.dart';
import '../../common/theme/app_theme.dart';
import 'call_reminder_api.dart';
import 'call_reminder_screen.dart';

/// Tab Thông báo của nhân viên (009 T022; US1): hộp thông báo chung, trên cùng là khối "Cần gọi điện" các lượt nhắc đang Chờ
/// kết quả (FR-027, FR-029). Quản lý viện thấy cả lượt đang nhắc người khác, dòng "Đang nhắc" cho biết ai đang giữ. Không
/// lưu thông báo xuống máy: spec chỉ ghi MAY xem ngoại tuyến, và máy dùng chung có thể lộ phần đã hết quyền xem (FR-034).
class StaffInboxScreen extends StatefulWidget {
  const StaffInboxScreen({super.key, required this.api, this.push, this.onUrgentCount, this.dial = dialNumber});

  final ApiClient api;
  final PushCoordinator? push;
  final ValueChanged<int>? onUrgentCount;
  final Future<bool> Function(String phone) dial;

  @override
  State<StaffInboxScreen> createState() => _StaffInboxScreenState();
}

class _StaffInboxScreenState extends State<StaffInboxScreen> {
  late final _reminders = CallReminderApi(widget.api);
  List<Map<String, dynamic>> _awaiting = const [];

  Future<void> _loadReminders() async {
    try {
      final items = await _reminders.mine();
      if (mounted) setState(() => _awaiting = items);
    } on Exception {
      // hộp thông báo báo lỗi tải riêng; khối này giữ danh sách trước đó
    }
  }

  Future<void> _openReminder(BuildContext context, int id, [VoidCallback? reload]) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => CallReminderScreen(reminders: _reminders, id: id, dial: widget.dial),
      ),
    );
    reload?.call();
  }

  /// Thông báo của lượt nhắc gọi có nguồn `M13.CALL_REMINDER` (research B-08).
  Widget? _sourceAction(BuildContext context, Map<String, dynamic> n) {
    final source = n['sourceRecord'] as Map<String, dynamic>?;
    final id = source?['type'] == 'M13.CALL_REMINDER' ? int.tryParse(source?['id'] as String? ?? '') : null;
    if (id == null) return null;
    return OutlinedButton(onPressed: () => _openReminder(context, id), child: const Text('Mở lượt nhắc gọi'));
  }

  Widget? _leading(BuildContext context, VoidCallback reload) {
    if (_awaiting.isEmpty) return null;
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Cần gọi điện', style: theme.textTheme.titleMedium?.copyWith(color: theme.colorScheme.error)),
          for (final r in _awaiting)
            _ReminderItem(reminder: r, onOpen: () => _openReminder(context, r['id'] as int, reload)),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) => NotificationInbox(
    api: widget.api,
    push: widget.push,
    leading: _leading,
    sourceAction: _sourceAction,
    onRefresh: _loadReminders,
    onUrgentCount: widget.onUrgentCount,
  );
}

class _ReminderItem extends StatelessWidget {
  const _ReminderItem({required this.reminder, required this.onOpen});

  final Map<String, dynamic> reminder;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final person = reminder['personToCall'] as Map<String, dynamic>;
    final due = formatTime(DateTime.parse(reminder['dueAt'] as String));
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          border: Border.all(color: theme.colorScheme.error, width: 1.5),
          borderRadius: BorderRadius.circular(AppRadius.lg),
        ),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Gọi ${person['fullName']}', style: theme.textTheme.titleSmall),
              Text(
                'Đang nhắc ${reminder['currentlyReminded']}. Hạn $due',
                style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 8),
              OutlinedButton(onPressed: onOpen, child: const Text('Mở lượt nhắc gọi')),
            ],
          ),
        ),
      ),
    );
  }
}
