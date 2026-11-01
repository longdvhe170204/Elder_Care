import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/notifications/notification_api.dart';
import 'call_reminder_api.dart';

/// Mở khung ghi kết quả gọi (009 FR-030, FR-031; DESIGN.md 15.3 bottom sheet); trả `true` khi đã lưu.
Future<bool?> showCallResultSheet(
  BuildContext context, {
  required CallReminderApi reminders,
  required int id,
  required String? phone,
}) => showModalBottomSheet<bool>(
  context: context,
  isScrollControlled: true,
  useSafeArea: true,
  builder: (_) => _CallResultForm(reminders: reminders, id: id, phone: phone),
);

class _CallResultForm extends StatefulWidget {
  const _CallResultForm({required this.reminders, required this.id, required this.phone});

  final CallReminderApi reminders;
  final int id;
  final String? phone;

  @override
  State<_CallResultForm> createState() => _CallResultFormState();
}

class _CallResultFormState extends State<_CallResultForm> {
  late final _phone = TextEditingController(text: widget.phone);
  final _answeredBy = TextEditingController();
  final _note = TextEditingController();
  DateTime _calledAt = DateTime.now();
  String _key = newIdempotencyKey();
  bool? _reached;
  String? _reason;
  String? _choiceError;
  String? _noteError;
  String? _failure;
  bool _busy = false;

  @override
  void dispose() {
    _phone.dispose();
    _answeredBy.dispose();
    _note.dispose();
    super.dispose();
  }

  /// Chọn giờ trong ngày; giờ sau hiện tại là cuộc gọi của hôm trước (ghi kết quả ngay sau khi gọi, có thể qua nửa đêm).
  Future<void> _pickTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(_calledAt),
      helpText: 'Thời điểm gọi',
      cancelText: 'Hủy',
      confirmText: 'Chọn',
      builder: (context, child) =>
          MediaQuery(data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true), child: child!),
    );
    if (picked == null || !mounted) return;
    final now = DateTime.now();
    final value = DateTime(now.year, now.month, now.day, picked.hour, picked.minute);
    setState(() => _calledAt = value.isAfter(now) ? value.subtract(const Duration(days: 1)) : value);
  }

  Future<void> _submit() async {
    final reached = _reached;
    final note = _note.text.trim();
    setState(() {
      _choiceError = switch (reached) {
        null => 'Chọn kết quả gọi.',
        false when _reason == null => 'Chọn lý do không liên lạc được.',
        _ => null,
      };
      _noteError = reached == true && note.isEmpty ? 'Cần ghi chú khi đã liên lạc được.' : null;
      _failure = null;
    });
    if (_choiceError != null || _noteError != null) return;
    final phone = _phone.text.trim();
    final answeredBy = _answeredBy.text.trim();
    setState(() => _busy = true);
    try {
      await widget.reminders.record(widget.id, {
        'result': reached! ? 'REACHED' : 'NOT_REACHED',
        'calledAt': _calledAt.toUtc().toIso8601String(),
        if (phone.isNotEmpty) 'calledPhoneNumber': phone,
        if (reached && answeredBy.isNotEmpty) 'answeredBy': answeredBy,
        if (!reached) 'reason': _reason,
        if (note.isNotEmpty) 'note': note,
      }, _key);
      if (mounted) Navigator.of(context).pop(true);
    } on ApiUnreachable {
      if (mounted) setState(() => _failure = 'Ghi kết quả gọi cần kết nối mạng. Kiểm tra mạng rồi thử lại.');
    } on ApiProblem catch (e) {
      // Backend đã từ chối, lần lưu sau là một lệnh mới
      _key = newIdempotencyKey();
      if (mounted) {
        setState(() {
          if (e.status == 422) {
            _noteError = backendMessage(e);
          } else {
            _failure = backendMessage(e);
          }
        });
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _choose(VoidCallback change) => setState(() {
    change();
    _choiceError = null;
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final text = theme.textTheme;
    final error = text.bodyMedium?.copyWith(color: theme.colorScheme.error);
    final reached = _reached;
    return SingleChildScrollView(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.viewInsetsOf(context).bottom),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Ghi kết quả gọi', style: text.titleMedium),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final value in const [true, false])
                ChoiceChip(
                  label: Text(callResultLabels[value ? 'REACHED' : 'NOT_REACHED']!),
                  selected: reached == value,
                  onSelected: (_) => _choose(() => _reached = value),
                ),
            ],
          ),
          if (reached == false) ...[
            const SizedBox(height: 12),
            Text('Lý do', style: text.titleSmall),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final e in callReasonChoices.entries)
                  ChoiceChip(
                    label: Text(e.value),
                    selected: _reason == e.key,
                    onSelected: (_) => _choose(() => _reason = e.key),
                  ),
              ],
            ),
          ],
          if (_choiceError != null) ...[const SizedBox(height: 8), Text(_choiceError!, style: error)],
          const SizedBox(height: 12),
          InkWell(
            onTap: _pickTime,
            child: InputDecorator(
              decoration: const InputDecoration(labelText: 'Thời điểm gọi', suffixIcon: Icon(LucideIcons.clock)),
              child: Text(formatDateTime(_calledAt)),
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _phone,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(labelText: 'Số đã gọi'),
          ),
          if (reached == true) ...[
            const SizedBox(height: 12),
            TextField(
              controller: _answeredBy,
              decoration: const InputDecoration(labelText: 'Người nghe máy (nếu khác người cần gọi)'),
            ),
          ],
          const SizedBox(height: 12),
          TextField(
            controller: _note,
            minLines: 2,
            maxLines: 4,
            decoration: InputDecoration(labelText: 'Ghi chú', errorText: _noteError, errorMaxLines: 3),
          ),
          const SizedBox(height: 8),
          Text(
            'Kết quả đã lưu chỉ sửa được bằng đính chính.',
            style: text.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
          if (_failure != null) ...[const SizedBox(height: 8), Text(_failure!, style: error)],
          const SizedBox(height: 12),
          FilledButton(onPressed: _busy ? null : _submit, child: const Text('Lưu kết quả')),
        ],
      ),
    );
  }
}
