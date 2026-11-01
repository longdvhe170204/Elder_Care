import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/notifications/notification_api.dart';
import '../../common/notifications/notification_labels.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/status_pill.dart';
import 'family_notification_screen.dart';

const _levels = [(null, 'Tất cả mức'), ('URGENT', 'Khẩn cấp'), ('MEDIUM', 'Trung bình'), ('LOW', 'Nhẹ')];

/// Hộp thông báo người thân trên app (009 T031; FR-033 → FR-036), cùng công thức `notification-inbox` với web (Q-305):
/// khối "Khẩn cấp chưa xác nhận" trên cùng có nút xác nhận, dưới là danh sách dòng; lọc mức, chưa xem, loại sự kiện,
/// người cao tuổi. Kéo xuống để tải lại; đẩy FCM thêm khi có cấu hình Firebase.
class FamilyInboxScreen extends StatefulWidget {
  const FamilyInboxScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<FamilyInboxScreen> createState() => _FamilyInboxScreenState();
}

class _FamilyInboxScreenState extends State<FamilyInboxScreen> {
  late final _notifications = NotificationApi(widget.api);
  List<Map<String, dynamic>>? _items;
  List<Map<String, dynamic>>? _residents;
  final _eventTypes = <String>{};
  int _unread = 0;
  int _urgent = 0;
  String? _nextCursor;
  Object? _loadError;
  String? _level;
  bool _unreadOnly = false;
  String? _eventType;
  int? _residentId;

  @override
  void initState() {
    super.initState();
    _load();
    _loadResidents();
  }

  NotificationFilter get _filter =>
      NotificationFilter(level: _level, unread: _unreadOnly, eventType: _eventType, residentId: _residentId);

  Future<void> _load([String? cursor]) async {
    try {
      final page = await _notifications.page(filter: _filter, cursor: cursor);
      if (!mounted) return;
      setState(() {
        _items = cursor == null ? page.items : [...?_items, ...page.items];
        _unread = page.unreadCount;
        _urgent = page.urgentCount;
        _nextCursor = page.nextCursor;
        _loadError = null;
        _eventTypes.addAll(page.items.map((n) => n['eventType'] as String?).whereType<String>());
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  /// Danh sách người cao tuổi trong phạm vi; không có quyền thì không có ô lọc (như web).
  Future<void> _loadResidents() async {
    try {
      final page = await widget.api.get('/residents');
      final items = (page?['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _residents = items);
    } on Exception {
      if (mounted) setState(() => _residents = null);
    }
  }

  void _reload(VoidCallback change) {
    setState(change);
    _load();
  }

  Future<void> _open(Map<String, dynamic> notification) async {
    final hidden = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => FamilyNotificationScreen(notifications: _notifications, id: notification['id'] as int),
      ),
    );
    if (!mounted) return;
    if (hidden ?? false) {
      setState(() => _items = _items?.where((n) => n['id'] != notification['id']).toList());
    }
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final items = _items;
    final urgent = items?.where(_isOpenUrgent).toList() ?? const <Map<String, dynamic>>[];
    final rest = items?.where((n) => !_isOpenUrgent(n)).toList() ?? const <Map<String, dynamic>>[];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
          child: Row(
            children: [
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: FilterChip(
                  label: const Text('Chưa xem'),
                  selected: _unreadOnly,
                  onSelected: (selected) => _reload(() => _unreadOnly = selected),
                ),
              ),
              for (final (value, label) in _levels)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(label),
                    selected: _level == value,
                    onSelected: (_) => _reload(() => _level = value),
                  ),
                ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: Row(
            children: [
              Expanded(
                child: _Filter<String>(
                  label: 'Loại sự kiện',
                  options: {for (final type in _eventTypes.toList()..sort()) type: type},
                  value: _eventType,
                  onChanged: (value) => _reload(() => _eventType = value),
                ),
              ),
              if (_residents != null) ...[
                const SizedBox(width: 12),
                Expanded(
                  child: _Filter<int>(
                    label: 'Người cao tuổi',
                    options: {for (final r in _residents!) r['id'] as int: _residentName(r)},
                    value: _residentId,
                    onChanged: (value) => _reload(() => _residentId = value),
                  ),
                ),
              ],
            ],
          ),
        ),
        Expanded(
          child: _loadError != null && items == null
              ? _LoadError(message: backendMessage(_loadError!), onRetry: _load)
              : items == null
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    children: [
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                        child: Text('$_unread chưa xem, $_urgent khẩn cấp chưa xác nhận.', style: text.bodyMedium),
                      ),
                      if (urgent.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Text('Khẩn cấp chưa xác nhận', style: text.titleMedium),
                              for (final n in urgent)
                                _UrgentItem(
                                  key: ValueKey(n['id']),
                                  notification: n,
                                  notifications: _notifications,
                                  onOpen: () => _open(n),
                                  onAcknowledged: _load,
                                ),
                            ],
                          ),
                        ),
                      if (rest.isEmpty && urgent.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(16),
                          child: Card(
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('Không có thông báo nào', style: text.titleSmall),
                                  const SizedBox(height: 4),
                                  Text(
                                    'Thông báo mới sẽ hiện ở đây. Đổi bộ lọc để xem thông báo khác.',
                                    style: text.bodyMedium,
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      for (final n in rest) ...[
                        _NotificationRow(notification: n, onTap: () => _open(n)),
                        const Divider(height: 1),
                      ],
                      if (_nextCursor != null)
                        Padding(
                          padding: const EdgeInsets.all(16),
                          child: OutlinedButton(onPressed: () => _load(_nextCursor), child: const Text('Xem thêm')),
                        ),
                    ],
                  ),
                ),
        ),
      ],
    );
  }
}

bool _isOpenUrgent(Map<String, dynamic> n) => n['level'] == 'URGENT' && n['acknowledgedAt'] == null;

String _residentName(Map<String, dynamic> r) =>
    (r['personalInfo'] as Map<String, dynamic>?)?['fullName'] as String? ?? 'Người cao tuổi ${r['id']}';

String _sentLine(Map<String, dynamic> n) {
  final sentAt = n['sentAt'] as String?;
  final level = levelLabels[n['level']] ?? '';
  return sentAt == null ? level : '$level, ${formatDateTime(DateTime.parse(sentAt))}';
}

/// Ô chọn một bộ lọc, mục đầu "Tất cả" là không lọc.
class _Filter<T> extends StatelessWidget {
  const _Filter({super.key, required this.label, required this.options, required this.value, required this.onChanged});

  final String label;
  final Map<T, String> options;
  final T? value;
  final ValueChanged<T?> onChanged;

  @override
  Widget build(BuildContext context) => DropdownButtonFormField<T?>(
    initialValue: value,
    isExpanded: true,
    icon: const Icon(LucideIcons.chevronDown),
    decoration: InputDecoration(labelText: label),
    items: [
      const DropdownMenuItem(child: Text('Tất cả')),
      for (final e in options.entries)
        DropdownMenuItem(
          value: e.key,
          child: Text(e.value, overflow: TextOverflow.ellipsis),
        ),
    ],
    onChanged: onChanged,
  );
}

/// Một thông báo Khẩn cấp chưa xác nhận (viền đỏ, `notification-inbox`): mở thông báo không phải xác nhận (FR-026).
class _UrgentItem extends StatefulWidget {
  const _UrgentItem({
    super.key,
    required this.notification,
    required this.notifications,
    required this.onOpen,
    required this.onAcknowledged,
  });

  final Map<String, dynamic> notification;
  final NotificationApi notifications;
  final VoidCallback onOpen;
  final Future<void> Function() onAcknowledged;

  @override
  State<_UrgentItem> createState() => _UrgentItemState();
}

class _UrgentItemState extends State<_UrgentItem> {
  final _key = newIdempotencyKey();
  bool _busy = false;
  String? _failure;

  Future<void> _acknowledge() async {
    setState(() {
      _busy = true;
      _failure = null;
    });
    try {
      await widget.notifications.acknowledge(widget.notification['id'] as int, _key);
      await widget.onAcknowledged();
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final text = theme.textTheme;
    final n = widget.notification;
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
              InkWell(
                onTap: widget.onOpen,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(n['title'] as String, style: text.titleSmall),
                    Text(_sentLine(n), style: text.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
                  ],
                ),
              ),
              if (_failure != null) ...[
                const SizedBox(height: 8),
                Text(_failure!, style: text.bodyMedium?.copyWith(color: theme.colorScheme.error)),
              ],
              const SizedBox(height: 8),
              FilledButton(onPressed: _busy ? null : _acknowledge, child: const Text('Xác nhận đã nhận')),
            ],
          ),
        ),
      ),
    );
  }
}

/// Dòng danh sách (DESIGN.md 15.4): tiêu đề, mức và thời điểm gửi, bên phải chữ trạng thái xem.
class _NotificationRow extends StatelessWidget {
  const _NotificationRow({required this.notification, required this.onTap});

  final Map<String, dynamic> notification;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final viewed = notification['viewedAt'] != null;
    return Material(
      color: theme.colorScheme.surface,
      child: InkWell(
        onTap: onTap,
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 64),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(notification['title'] as String, style: theme.textTheme.titleSmall),
                      Text(
                        _sentLine(notification),
                        style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                StatusPill(
                  label: viewed ? 'Đã xem' : 'Chưa xem',
                  tone: viewed ? StatusTone.settled : StatusTone.neutral,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _LoadError extends StatelessWidget {
  const _LoadError({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => ListView(
    padding: const EdgeInsets.all(16),
    children: [
      Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(message, style: Theme.of(context).textTheme.bodyMedium),
              const SizedBox(height: 12),
              OutlinedButton(onPressed: onRetry, child: const Text('Thử lại')),
            ],
          ),
        ),
      ),
    ],
  );
}
