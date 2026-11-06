import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/theme/app_theme.dart';

const _types = [('COMPLAINT', 'Khiếu nại'), ('SUGGESTION', 'Góp ý'), ('QUESTION', 'Hỏi đáp')];
const _groups = [
  ('CARE', 'Chăm sóc, sinh hoạt'),
  ('HEALTH_MEDICATION', 'Sức khỏe, thuốc'),
  ('EATING', 'Ăn uống'),
  ('COSTS_CONTRACT', 'Chi phí, hợp đồng'),
  ('OTHER', 'Khác'),
];
const _status = {
  'NEW': 'Mới',
  'IN_PROGRESS': 'Đang xử lý',
  'REPLIED': 'Đã phản hồi',
  'REOPENED': 'Mở lại',
  'CLOSED': 'Đóng',
};
const _itemLabels = {'REPLY': 'Viện trả lời', 'HANDLING_DIRECTION': 'Hướng xử lý', 'REOPEN': 'Mở lại'};
const _healthLock = 'Thông tin sức khỏe chỉ hiện với người thân được người cao tuổi đồng ý chia sẻ.';

String _label(List<(String, String)> options, Object? code) =>
    options.firstWhere((o) => o.$1 == code, orElse: () => ('$code', '$code')).$2;

/// Phản hồi của người thân trên app (012 T030; US6, FR-058, FR-061, FR-064 → FR-066), cùng chức năng với màn Phản hồi
/// của cổng web: gửi cho người cao tuổi đầu tiên có quan hệ Hiệu lực (chọn người khi có nhiều người chưa thiết kế,
/// DESIGN.md 14); nội dung đã gửi không sửa; Đã phản hồi thì Đồng ý đóng hoặc Mở lại có lý do. Chỉ trực tuyến.
class FeedbackScreen extends StatefulWidget {
  const FeedbackScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<FeedbackScreen> createState() => _FeedbackScreenState();
}

class _FeedbackScreenState extends State<FeedbackScreen> {
  Map<String, dynamic>? _resident;
  List<Map<String, dynamic>>? _items;
  Object? _loadError;
  String? _notice;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final residents = (await widget.api.getList('/family-portal/residents')).cast<Map<String, dynamic>>();
      final items = (await widget.api.getList('/feedback')).cast<Map<String, dynamic>>();
      if (!mounted) return;
      final resident = residents.isEmpty ? null : residents.first;
      setState(() {
        _resident = resident;
        _items = items.where((f) => f['residentId'] == resident?['residentId']).toList();
        _loadError = null;
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  void _changed(String message) {
    setState(() => _notice = message);
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    if (_loadError != null) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(backendMessage(_loadError!), style: text.bodyMedium),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: _load, child: const Text('Tải lại')),
          ],
        ),
      );
    }
    final items = _items;
    if (items == null) return const Center(child: CircularProgressIndicator());
    final resident = _resident;
    if (resident == null) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Text('Chưa có người cao tuổi có quan hệ Hiệu lực với bạn.', style: text.bodyMedium),
      );
    }
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _SubmitCard(
          api: widget.api,
          residentId: resident['residentId'] as int,
          residentName: resident['fullName'] as String?,
          onSent: () => _changed('Đã gửi phản hồi. Viện sẽ báo bạn khi có trả lời.'),
        ),
        if (_notice != null) ...[
          const SizedBox(height: 12),
          Semantics(liveRegion: true, child: Text(_notice!, style: text.bodyMedium)),
        ],
        const SizedBox(height: 16),
        Text('Phản hồi của bạn', style: text.titleMedium),
        const SizedBox(height: 8),
        if (items.isEmpty) Text('Bạn chưa gửi phản hồi nào.', style: text.bodyMedium),
        for (final f in items) ...[
          _FeedbackCard(key: ValueKey(f['id']), api: widget.api, item: f, onChanged: _changed),
          const SizedBox(height: 8),
        ],
      ],
    );
  }
}

class _SubmitCard extends StatefulWidget {
  const _SubmitCard({required this.api, required this.residentId, required this.residentName, required this.onSent});

  final ApiClient api;
  final int residentId;
  final String? residentName;
  final VoidCallback onSent;

  @override
  State<_SubmitCard> createState() => _SubmitCardState();
}

class _SubmitCardState extends State<_SubmitCard> {
  final _content = TextEditingController();
  String _key = _uuid();
  String? _type;
  String? _group;
  bool _submitted = false;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _content.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _submitted = true);
    if (_type == null || _group == null || _content.text.trim().isEmpty) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.post(
        '/feedback',
        {'residentId': widget.residentId, 'type': _type, 'group': _group, 'content': _content.text.trim()},
        headers: {'Idempotency-Key': _key},
      );
      _content.clear();
      setState(() {
        _type = null;
        _group = null;
        _submitted = false;
        _key = _uuid();
      });
      widget.onSent();
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _choices(
    String label,
    List<(String, String)> options,
    String? value,
    ValueChanged<String> onPick,
    String? error,
  ) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: theme.textTheme.labelLarge),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final (code, text) in options)
              ChoiceChip(label: Text(text), selected: value == code, onSelected: (_) => setState(() => onPick(code))),
          ],
        ),
        if (error != null) ...[
          const SizedBox(height: 4),
          Text(error, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
        ],
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tones = theme.extension<Tones>()!;
    final contentMissing = _submitted && _content.text.trim().isEmpty;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Gửi phản hồi về ${widget.residentName ?? 'người cao tuổi'}', style: theme.textTheme.titleSmall),
            const SizedBox(height: 12),
            _choices(
              'Loại',
              _types,
              _type,
              (v) => _type = v,
              _submitted && _type == null ? 'Chọn loại phản hồi' : null,
            ),
            if (_type != null) ...[
              const SizedBox(height: 4),
              Text('Viện trả lời trong ${_type == 'COMPLAINT' ? 24 : 72} giờ.', style: theme.textTheme.bodySmall),
            ],
            const SizedBox(height: 12),
            _choices(
              'Nội dung về',
              _groups,
              _group,
              (v) => _group = v,
              _submitted && _group == null ? 'Chọn nội dung phản hồi về việc gì' : null,
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _content,
              minLines: 3,
              maxLines: 6,
              maxLength: 4000,
              decoration: InputDecoration(
                labelText: 'Nội dung',
                errorText: contentMissing ? 'Nhập nội dung phản hồi' : null,
              ),
            ),
            if (_error != null)
              Semantics(
                liveRegion: true,
                child: Text(_error!, style: theme.textTheme.bodySmall?.copyWith(color: tones.attentionInk)),
              ),
            const SizedBox(height: 8),
            FilledButton(onPressed: _busy ? null : _submit, child: const Text('Gửi phản hồi')),
          ],
        ),
      ),
    );
  }
}

class _FeedbackCard extends StatefulWidget {
  const _FeedbackCard({super.key, required this.api, required this.item, required this.onChanged});

  final ApiClient api;
  final Map<String, dynamic> item;
  final ValueChanged<String> onChanged;

  @override
  State<_FeedbackCard> createState() => _FeedbackCardState();
}

class _FeedbackCardState extends State<_FeedbackCard> {
  final _reason = TextEditingController();
  final _key = _uuid();
  bool _reopening = false;
  bool _submitted = false;
  bool _busy = false;
  String? _error;
  Map<String, dynamic>? _detail;
  bool _showingReplies = false;

  // Đã phản hồi: đọc chi tiết để biết lệnh được phép (allowedCommands, đổi api 2026-11-05)
  @override
  void initState() {
    super.initState();
    if (widget.item['status'] == 'REPLIED') _loadDetail();
  }

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _send(String command, Map<String, dynamic> body, String done) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.post('/feedback/${widget.item['id']}/$command', body, headers: {'Idempotency-Key': _key});
      widget.onChanged(done);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _loadDetail() async {
    try {
      final detail = await widget.api.get('/feedback/${widget.item['id']}');
      if (mounted) setState(() => _detail = detail);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  void _showReplies() {
    setState(() => _showingReplies = true);
    if (_detail == null) _loadDetail();
  }

  bool _can(String command) => (_detail?['allowedCommands'] as List<dynamic>? ?? const []).contains(command);

  void _reopen() {
    setState(() => _submitted = true);
    if (_reason.text.trim().isEmpty) return;
    _send('reopen', {'reason': _reason.text.trim()}, 'Đã mở lại phản hồi. Viện sẽ trả lời theo hạn mới.');
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tones = theme.extension<Tones>()!;
    final f = widget.item;
    final status = f['status'];
    final replied = status == 'REPLIED';
    final detail = _showingReplies ? _detail : null;
    final replies = (detail?['items'] as List<dynamic>? ?? const [])
        .cast<Map<String, dynamic>>()
        .where((i) => i['type'] != 'CONTENT')
        .toList();
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              '${_label(_types, f['type'])}, ${_label(_groups, f['group']).toLowerCase()}, hạn trả lời '
              '${formatDateTime(DateTime.parse(f['dueAt'] as String))}',
              style: theme.textTheme.bodySmall,
            ),
            const SizedBox(height: 4),
            Text(_status[status] ?? '$status', style: theme.textTheme.labelMedium),
            const SizedBox(height: 8),
            Text('${f['content']}', style: theme.textTheme.bodyMedium),
            if ((replied || status == 'CLOSED') && !_showingReplies)
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton(onPressed: _showReplies, child: const Text('Xem trả lời')),
              ),
            if (detail != null) ...[
              const SizedBox(height: 8),
              for (final r in replies)
                Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Text(
                    '${_itemLabels[r['type']] ?? r['type']}: ${r['content']}',
                    style: theme.textTheme.bodyMedium,
                  ),
                ),
              if (detail['healthReplyHidden'] == true) Text(_healthLock, style: theme.textTheme.bodySmall),
            ],
            if (_error != null)
              Semantics(
                liveRegion: true,
                child: Text(_error!, style: theme.textTheme.bodySmall?.copyWith(color: tones.attentionInk)),
              ),
            if (!_reopening && (_can('agree-to-close') || _can('reopen'))) ...[
              const SizedBox(height: 8),
              Wrap(
                spacing: 12,
                runSpacing: 8,
                children: [
                  if (_can('agree-to-close'))
                    OutlinedButton(
                      onPressed: _busy ? null : () => _send('agree-to-close', const {}, 'Đã đóng phản hồi.'),
                      child: const Text('Đồng ý đóng'),
                    ),
                  if (_can('reopen'))
                    TextButton(onPressed: () => setState(() => _reopening = true), child: const Text('Mở lại')),
                ],
              ),
            ],
            if (_reopening) ...[
              const SizedBox(height: 8),
              TextField(
                controller: _reason,
                maxLength: 2000,
                minLines: 2,
                maxLines: 4,
                decoration: InputDecoration(
                  labelText: 'Lý do mở lại',
                  errorText: _submitted && _reason.text.trim().isEmpty ? 'Nhập lý do mở lại' : null,
                ),
              ),
              Wrap(
                spacing: 12,
                runSpacing: 8,
                children: [
                  FilledButton(onPressed: _busy ? null : _reopen, child: const Text('Gửi mở lại')),
                  OutlinedButton(
                    onPressed: () => setState(() {
                      _reopening = false;
                      _submitted = false;
                    }),
                    child: const Text('Quay lại'),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Khóa idempotency dạng UUID v4.
String _uuid() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
