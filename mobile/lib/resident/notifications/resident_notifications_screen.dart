import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/notifications/notification_api.dart';

/// Nút "Quay lại" có chữ ở góc trái thanh tiêu đề (DESIGN.md 15.6); bề rộng theo cỡ chữ của máy để không cắt chữ, tiêu đề
/// màn nằm trong thân màn vì thanh không đủ chỗ cho cả hai khi chữ lớn.
AppBar _backBar(BuildContext context) => AppBar(
  automaticallyImplyLeading: false,
  leadingWidth: MediaQuery.textScalerOf(context).scale(120),
  leading: TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Quay lại')),
);

String _when(Map<String, dynamic> n) {
  final sentAt = n['sentAt'] as String?;
  return sentAt == null ? '' : formatDateTime(DateTime.parse(sentAt));
}

/// Thông báo của người cao tuổi (009 T042; FR-022a; DESIGN.md 15.6): chỉ các nhắc mức Nhẹ của chính mình, chữ lớn, câu ngắn,
/// mỗi màn một việc; không ẩn, không xác nhận, không lọc. Mất mạng thì nói rõ bằng câu ngắn.
class ResidentNotificationsScreen extends StatefulWidget {
  const ResidentNotificationsScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<ResidentNotificationsScreen> createState() => _ResidentNotificationsScreenState();
}

class _ResidentNotificationsScreenState extends State<ResidentNotificationsScreen> {
  late final _notifications = NotificationApi(widget.api);
  List<Map<String, dynamic>>? _items;
  String? _nextCursor;
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load([String? cursor]) async {
    try {
      final page = await _notifications.page(cursor: cursor);
      if (!mounted) return;
      setState(() {
        _items = cursor == null ? page.items : [...?_items, ...page.items];
        _nextCursor = page.nextCursor;
        _failed = false;
      });
    } on Exception {
      if (mounted) setState(() => _failed = true);
    }
  }

  Future<void> _open(Map<String, dynamic> notification) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => ResidentNotificationScreen(notifications: _notifications, id: notification['id'] as int),
      ),
    );
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final colors = Theme.of(context).colorScheme;
    final items = _items;
    return Scaffold(
      appBar: _backBar(context),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('Thông báo', style: text.headlineMedium),
            const SizedBox(height: 16),
            if (_failed) ...[
              Text('Máy chưa kết nối mạng.', style: text.bodyLarge),
              const SizedBox(height: 12),
              OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
              const SizedBox(height: 16),
            ],
            if (items == null && !_failed) const Center(child: CircularProgressIndicator()),
            if (items != null && items.isEmpty) Text('Chưa có thông báo nào.', style: text.bodyLarge),
            for (final n in items ?? const <Map<String, dynamic>>[]) ...[
              Card(
                clipBehavior: Clip.antiAlias,
                child: InkWell(
                  onTap: () => _open(n),
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(n['title'] as String, style: text.titleMedium),
                        const SizedBox(height: 4),
                        Text(_when(n), style: text.bodyMedium),
                        if (n['viewedAt'] == null) ...[
                          const SizedBox(height: 4),
                          // chữ chính: phần này cần tương phản 7:1 (DESIGN.md 15.6), chữ phụ của nhãn trạng thái chưa đạt
                          Text('Chưa xem', style: text.bodyMedium?.copyWith(color: colors.onSurface)),
                        ],
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
            if (_nextCursor != null) OutlinedButton(onPressed: () => _load(_nextCursor), child: const Text('Xem thêm')),
          ],
        ),
      ),
    );
  }
}

/// Mở một thông báo: backend ghi thời điểm xem lần đầu (FR-034). Bấm vào sẽ mở đúng màn của việc đó khi các màn của 012, 014
/// có trên app (DESIGN.md 15.6).
class ResidentNotificationScreen extends StatefulWidget {
  const ResidentNotificationScreen({super.key, required this.notifications, required this.id});

  final NotificationApi notifications;
  final int id;

  @override
  State<ResidentNotificationScreen> createState() => _ResidentNotificationScreenState();
}

class _ResidentNotificationScreenState extends State<ResidentNotificationScreen> {
  Map<String, dynamic>? _notification;
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final notification = await widget.notifications.open(widget.id);
      if (mounted) setState(() => _notification = notification);
    } on Exception {
      if (mounted) setState(() => _failed = true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final n = _notification;
    final content = (n?['content'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
    return Scaffold(
      appBar: _backBar(context),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            if (_failed) Text('Máy chưa kết nối mạng.', style: text.bodyLarge),
            if (n == null && !_failed) const Center(child: CircularProgressIndicator()),
            if (n != null) ...[
              Text(n['title'] as String, style: text.headlineSmall),
              const SizedBox(height: 8),
              Text(_when(n), style: text.bodyMedium),
              const SizedBox(height: 16),
              for (final p in content) Text(p['content'] as String, style: text.bodyLarge),
            ],
          ],
        ),
      ),
    );
  }
}
