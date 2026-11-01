import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../api/problem_message.dart';
import '../format/date_time.dart';
import '../widgets/header_app_bar.dart';
import '../widgets/status_pill.dart';
import 'acknowledge_actions.dart';
import 'notification_api.dart';
import 'notification_labels.dart';

/// Nút mở bản ghi nguồn ở chi tiết thông báo; `null` là không có nút.
typedef SourceAction = Widget? Function(BuildContext context, Map<String, dynamic> notification);

/// Chi tiết thông báo của nhân viên, người thân (009 T022, T031; FR-026, FR-034, FR-036), màn riêng, cùng thứ tự với web.
/// Mở là ghi xem lần đầu (backend), không phải xác nhận; nội dung theo quyền hiện hành: phần đã hết quyền thay bằng dòng
/// khóa, không lộ nội dung cũ. Khẩn cấp chưa xác nhận có nút cố định ở chân màn: từ màn hình khóa bấm thông báo rồi xác nhận
/// là hai thao tác (SC-008). Trả `true` khi đã ẩn khỏi danh sách.
class NotificationScreen extends StatefulWidget {
  const NotificationScreen({super.key, required this.notifications, required this.id, this.sourceAction});

  final NotificationApi notifications;
  final int id;
  final SourceAction? sourceAction;

  @override
  State<NotificationScreen> createState() => _NotificationScreenState();
}

class _NotificationScreenState extends State<NotificationScreen> {
  final _hideKey = newIdempotencyKey();
  Map<String, dynamic>? _notification;
  Object? _loadError;
  String? _failure;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final notification = await widget.notifications.open(widget.id);
      if (mounted) setState(() => _notification = notification);
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  Future<void> _hide() async {
    setState(() {
      _busy = true;
      _failure = null;
    });
    try {
      await widget.notifications.hide(widget.id, _hideKey);
      if (mounted) Navigator.of(context).pop(true);
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final n = _notification;
    return Scaffold(
      appBar: headerAppBar(
        context,
        leading: IconButton(
          icon: const Icon(LucideIcons.arrowLeft),
          tooltip: 'Quay lại',
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: 'Thông báo',
      ),
      body: _loadError != null
          ? Padding(padding: const EdgeInsets.all(16), child: Text(backendMessage(_loadError!)))
          : n == null
          ? const Center(child: CircularProgressIndicator())
          : SafeArea(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(child: _content(context, n)),
                  if (isOpenUrgent(n))
                    Padding(
                      padding: const EdgeInsets.all(16),
                      child: AcknowledgeActions(notification: n, notifications: widget.notifications, onDone: _load),
                    ),
                ],
              ),
            ),
    );
  }

  Widget _content(BuildContext context, Map<String, dynamic> n) {
    final theme = Theme.of(context);
    final text = theme.textTheme;
    final level = n['level'] as String;
    final status = n['status'] as String;
    final hidden = (n['hiddenParts'] as List<dynamic>? ?? const []).cast<String>();
    final content = (n['content'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
    final parts = partOrder.where((type) => content.any((p) => p['informationType'] == type) || hidden.contains(type));
    final sentAt = n['sentAt'] as String?;
    final viewedAt = n['viewedAt'] as String?;
    final acknowledgedAt = n['acknowledgedAt'] as String?;
    final source = widget.sourceAction?.call(context, n);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(n['title'] as String, style: text.titleMedium),
        const SizedBox(height: 4),
        Wrap(
          spacing: 12,
          children: [
            StatusPill(label: levelLabels[level] ?? level, tone: levelTone(level)),
            StatusPill(label: statusLabels[status] ?? status, tone: statusTone(status)),
          ],
        ),
        const SizedBox(height: 12),
        _Line('Gửi lúc', sentAt == null ? 'Chưa gửi' : formatDateTime(DateTime.parse(sentAt))),
        _Line('Xem lần đầu', viewedAt == null ? 'Chưa xem' : formatDateTime(DateTime.parse(viewedAt))),
        if (acknowledgedAt != null) _Line('Xác nhận lúc', formatDateTime(DateTime.parse(acknowledgedAt))),
        if (n['identityOnly'] == true)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Text(
              'Bạn nhận thay vì không có người nhận; chỉ xem được thông tin nhận dạng của người cao tuổi.',
              style: text.bodyMedium,
            ),
          ),
        for (final type in parts)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        if (type == 'HEALTH') ...[const Icon(LucideIcons.lock, size: 16), const SizedBox(width: 8)],
                        Text(partLabels[type] ?? type, style: text.titleSmall),
                      ],
                    ),
                    const SizedBox(height: 4),
                    if (hidden.contains(type))
                      Text(
                        'Bạn không còn quyền xem phần này.',
                        style: text.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      )
                    else
                      for (final p in content.where((p) => p['informationType'] == type))
                        Text(p['content'] as String, style: text.bodyMedium),
                  ],
                ),
              ),
            ),
          ),
        if (source != null) ...[const SizedBox(height: 16), source],
        if (_failure != null) ...[
          const SizedBox(height: 12),
          Text(_failure!, style: text.bodyMedium?.copyWith(color: theme.colorScheme.error)),
        ],
        if (viewedAt != null) ...[
          const SizedBox(height: 16),
          OutlinedButton(onPressed: _busy ? null : _hide, child: const Text('Ẩn khỏi danh sách')),
        ],
      ],
    );
  }
}

class _Line extends StatelessWidget {
  const _Line(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant)),
          Text(value, style: text.bodyMedium),
        ],
      ),
    );
  }
}
