import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/status_pill.dart';
import 'call_reminder_api.dart';
import 'call_result_sheet.dart';

/// Mở trình quay số của máy, người dùng tự bấm gọi; `false` là máy không mở được.
Future<bool> dialNumber(String phone) async {
  try {
    return await launchUrl(Uri(scheme: 'tel', path: phone));
  } on PlatformException {
    return false;
  }
}

/// Chi tiết lượt nhắc gọi (009 T022; FR-027, FR-027a, FR-029a, FR-030a), màn riêng, nút chính cố định ở chân màn. Số điện
/// thoại là số hiện hành lúc mở. Hợp đồng chỉ có `residentId` nên màn không hiện tên người cao tuổi; nội dung cần báo do
/// module nguồn viết.
class CallReminderScreen extends StatefulWidget {
  const CallReminderScreen({super.key, required this.reminders, required this.id, this.dial = dialNumber});

  final CallReminderApi reminders;
  final int id;
  final Future<bool> Function(String phone) dial;

  @override
  State<CallReminderScreen> createState() => _CallReminderScreenState();
}

class _CallReminderScreenState extends State<CallReminderScreen> {
  Map<String, dynamic>? _reminder;
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final reminder = await widget.reminders.detail(widget.id);
      if (mounted) setState(() => _reminder = reminder);
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  Future<void> _call(String phone) async {
    if (await widget.dial(phone) || !mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Máy này không mở được trình gọi điện.')));
  }

  Future<void> _record(String? phone) async {
    final saved = await showCallResultSheet(context, reminders: widget.reminders, id: widget.id, phone: phone);
    if (saved != true || !mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Đã ghi kết quả gọi')));
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final r = _reminder;
    return Scaffold(
      appBar: headerAppBar(
        context,
        leading: IconButton(
          icon: const Icon(LucideIcons.arrowLeft),
          tooltip: 'Quay lại',
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: 'Lượt nhắc gọi',
      ),
      body: _loadError != null && r == null
          ? Padding(padding: const EdgeInsets.all(16), child: Text(backendMessage(_loadError!)))
          : r == null
          ? const Center(child: CircularProgressIndicator())
          : SafeArea(child: _content(context, r)),
    );
  }

  Widget _content(BuildContext context, Map<String, dynamic> r) {
    final theme = Theme.of(context);
    final text = theme.textTheme;
    final muted = text.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant);
    final person = r['personToCall'] as Map<String, dynamic>;
    final phone = person['phoneNumber'] as String?;
    final status = r['status'] as String;
    final stopReason = r['stopReason'] as String?;
    final content = (r['contentToReport'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
    final handoffs = (r['handoffs'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
    final results = (r['results'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
    final kind = person['type'] == 'STAFF' ? 'Nhân viên' : 'Người thân';
    final relationship = person['relationship'] as String?;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _Section(
                children: [
                  Align(
                    alignment: Alignment.centerLeft,
                    child: StatusPill(label: reminderStatusLabels[status] ?? status, tone: reminderTone(status)),
                  ),
                  const SizedBox(height: 8),
                  Text(person['fullName'] as String, style: text.titleMedium),
                  Text(relationship == null ? kind : '$kind, $relationship', style: muted),
                  const SizedBox(height: 8),
                  if (phone == null)
                    Text('Chưa có số điện thoại', style: text.bodyMedium)
                  else ...[
                    Text(phone, style: text.titleLarge),
                    const SizedBox(height: 8),
                    OutlinedButton.icon(
                      onPressed: () => _call(phone),
                      icon: const Icon(LucideIcons.phone),
                      label: const Text('Gọi số này'),
                    ),
                  ],
                ],
              ),
              _Section(
                title: 'Nội dung cần báo',
                children: [
                  for (final c in content) ...[
                    Text(formatTime(DateTime.parse(c['occurredAt'] as String)), style: muted),
                    Text(c['content'] as String, style: text.bodyMedium),
                    const SizedBox(height: 4),
                  ],
                ],
              ),
              _Section(
                children: [
                  Text('Đang nhắc', style: muted),
                  Text(r['currentlyReminded'] as String? ?? '', style: text.bodyMedium),
                  const SizedBox(height: 4),
                  Text('Hạn', style: muted),
                  Text(formatDateTime(DateTime.parse(r['dueAt'] as String)), style: text.bodyMedium),
                  if (stopReason != null) ...[
                    const SizedBox(height: 4),
                    Text('Lý do dừng', style: muted),
                    Text(stopReason, style: text.bodyMedium),
                  ],
                ],
              ),
              if (results.isNotEmpty)
                _Section(title: 'Kết quả gọi', children: [for (final result in results) _ResultLines(result)]),
              if (handoffs.isNotEmpty)
                _Section(
                  title: 'Lịch sử',
                  children: [
                    for (final h in handoffs)
                      Text(
                        '${formatTime(DateTime.parse(h['occurredAt'] as String))} '
                        '${handoffLabels[h['reason']] ?? 'Chuyển nhắc'}',
                        style: muted,
                      ),
                  ],
                ),
            ],
          ),
        ),
        if (r['canRecordResult'] == true)
          Padding(
            padding: const EdgeInsets.all(16),
            child: FilledButton(
              onPressed: () => _record(phone),
              child: Text(status == 'AWAITING_RESULT' ? 'Ghi kết quả gọi' : 'Ghi lần gọi bổ sung'),
            ),
          ),
      ],
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({this.title, required this.children});

  final String? title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (title != null) ...[
              Text(title!, style: Theme.of(context).textTheme.titleSmall),
              const SizedBox(height: 4),
            ],
            ...children,
          ],
        ),
      ),
    ),
  );
}

class _ResultLines extends StatelessWidget {
  const _ResultLines(this.result);

  final Map<String, dynamic> result;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final muted = theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant);
    final label = callResultLabels[result['result']] ?? '';
    final reason = callReasonLabels[result['reason']];
    final answeredBy = result['answeredBy'] as String?;
    final note = result['note'] as String?;
    final calledAt = formatDateTime(DateTime.parse(result['calledAt'] as String));
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(reason == null ? label : '$label, $reason', style: theme.textTheme.bodyMedium),
          Text(result['supplementary'] == true ? '$calledAt, lần gọi bổ sung' : calledAt, style: muted),
          if (answeredBy != null) Text('Người nghe máy: $answeredBy', style: muted),
          if (note != null) Text(note, style: theme.textTheme.bodyMedium),
        ],
      ),
    );
  }
}
