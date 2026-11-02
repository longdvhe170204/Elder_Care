import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../api/api_client.dart';
import '../api/problem_message.dart';
import '../format/date_time.dart';
import '../theme/app_theme.dart';
import '../widgets/status_pill.dart';
import 'acknowledge_actions.dart';
import 'notification_api.dart';
import 'notification_labels.dart';
import 'notification_screen.dart';
import 'push_coordinator.dart';

const _levels = [(null, 'Tất cả mức'), ('URGENT', 'Khẩn cấp'), ('MEDIUM', 'Trung bình'), ('LOW', 'Nhẹ')];

/// Hộp thông báo của phần nhân viên và người thân (009 T022, T031; FR-033 → FR-036), cùng công thức `notification-inbox`
/// với web (Q-305): khối "Khẩn cấp chưa xác nhận" trên cùng có nút xác nhận, dưới là danh sách dòng; chip chưa xem, mức;
/// loại sự kiện, người cao tuổi ở "Lọc thêm". Kéo xuống để tải lại; có đẩy thì tải lại ngay.
class NotificationInbox extends StatefulWidget {
  const NotificationInbox({
    super.key,
    required this.api,
    this.push,
    this.leading,
    this.sourceAction,
    this.onRefresh,
    this.onUrgentCount,
  });

  final ApiClient api;

  /// Nhận đẩy: có thông báo thì tải lại, bấm thông báo thì mở đúng thông báo đó (009 T031).
  final PushCoordinator? push;

  /// Khối đứng đầu danh sách; `reload` tải lại hộp. Phần nhân viên: "Cần gọi điện".
  final Widget? Function(BuildContext context, VoidCallback reload)? leading;

  /// Nút mở bản ghi nguồn ở chi tiết thông báo.
  final SourceAction? sourceAction;

  /// Chạy mỗi lần tải lại trang đầu.
  final Future<void> Function()? onRefresh;

  /// Số Khẩn cấp chưa xác nhận sau mỗi lần tải.
  final ValueChanged<int>? onUrgentCount;

  @override
  State<NotificationInbox> createState() => _NotificationInboxState();
}

class _NotificationInboxState extends State<NotificationInbox> {
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
    widget.push?.addListener(_onPush);
    WidgetsBinding.instance.addPostFrameCallback((_) => _openPending());
  }

  @override
  void dispose() {
    widget.push?.removeListener(_onPush);
    super.dispose();
  }

  void _onPush() {
    _load();
    _openPending();
  }

  void _openPending() {
    final id = widget.push?.takeOpened();
    if (id != null && mounted) _openById(id);
  }

  NotificationFilter get _filter =>
      NotificationFilter(level: _level, unread: _unreadOnly, eventType: _eventType, residentId: _residentId);

  Future<void> _load([String? cursor]) async {
    if (cursor == null) await widget.onRefresh?.call();
    try {
      final page = await _notifications.page(filter: _filter, cursor: cursor);
      if (!mounted) return;
      widget.onUrgentCount?.call(page.urgentCount);
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

  int get _moreFilterCount => (_eventType == null ? 0 : 1) + (_residentId == null ? 0 : 1);

  Future<void> _moreFilters() => showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (sheet) => Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.viewInsetsOf(sheet).bottom),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Lọc thêm', style: Theme.of(sheet).textTheme.titleMedium),
          const SizedBox(height: 16),
          _Filter<String>(
            label: 'Loại sự kiện',
            options: {for (final type in _eventTypes.toList()..sort()) type: type},
            value: _eventType,
            onChanged: (value) => _reload(() => _eventType = value),
          ),
          if (_residents != null) ...[
            const SizedBox(height: 12),
            _Filter<int>(
              label: 'Người cao tuổi',
              options: {for (final r in _residents!) r['id'] as int: _residentName(r)},
              value: _residentId,
              onChanged: (value) => _reload(() => _residentId = value),
            ),
          ],
        ],
      ),
    ),
  );

  Future<void> _open(Map<String, dynamic> notification) => _openById(notification['id'] as int);

  Future<void> _openById(int id) async {
    final hidden = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => NotificationScreen(notifications: _notifications, id: id, sourceAction: widget.sourceAction),
      ),
    );
    if (!mounted) return;
    if (hidden ?? false) {
      setState(() => _items = _items?.where((n) => n['id'] != id).toList());
    }
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final items = _items;
    final urgent = items?.where(isOpenUrgent).toList() ?? const <Map<String, dynamic>>[];
    final rest = items?.where((n) => !isOpenUrgent(n)).toList() ?? const <Map<String, dynamic>>[];
    final leading = widget.leading?.call(context, _load);
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
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: FilterChip(
                  label: Text(_moreFilterCount == 0 ? 'Lọc thêm' : 'Lọc thêm ($_moreFilterCount)'),
                  selected: _moreFilterCount > 0,
                  onSelected: (_) => _moreFilters(),
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
                      ?leading,
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
class _UrgentItem extends StatelessWidget {
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
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final text = theme.textTheme;
    final n = notification;
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
                onTap: onOpen,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(n['title'] as String, style: text.titleSmall),
                    Text(_sentLine(n), style: text.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              AcknowledgeActions(notification: n, notifications: notifications, onDone: onAcknowledged),
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
