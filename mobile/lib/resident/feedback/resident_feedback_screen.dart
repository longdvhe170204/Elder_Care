import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../channel/channel_screens.dart';

// "Góp ý với viện" của người cao tuổi (012 T070; US9, FR-058, FR-058a, FR-058b, FR-066; DESIGN.md 15.6): danh sách góp ý
// đã gửi kèm trạng thái và trả lời; gửi mới theo bước, mỗi màn một việc. Lệnh trên một phản hồi đi qua
// /feedback/{id}/{command} (quyền Phản hồi phạm vi chính mình).

/// Bốn loại, dưới mỗi tên một dòng giải thích (DESIGN.md 15.6; nghiep-vu 14.7).
const residentTypes = [
  ('SUGGESTION', 'Góp ý', 'Muốn viện làm tốt hơn'),
  ('QUESTION', 'Hỏi đáp', 'Muốn hỏi một việc'),
  ('COMPLAINT', 'Khiếu nại', 'Có việc không đúng'),
  ('WISH', 'Mong muốn', 'Điều mình muốn'),
];

const _wishTopics = [('VISITORS_PICKERS', 'Về người đến thăm, người đón tôi'), ('OTHER', 'Việc khác')];

/// Nhóm nội dung bắt buộc theo FR-058 (quyết định người phụ trách, FR-059); DESIGN.md 15.6 (6.8.5).
const _groups = [
  ('CARE', 'Chăm sóc, sinh hoạt'),
  ('HEALTH_MEDICATION', 'Sức khỏe, thuốc'),
  ('EATING', 'Ăn uống'),
  ('COSTS_CONTRACT', 'Chi phí, hợp đồng'),
  ('OTHER', 'Việc khác'),
];

const _status = {
  'NEW': 'Đã gửi',
  'IN_PROGRESS': 'Viện đang xem',
  'REPLIED': 'Đã có trả lời',
  'REOPENED': 'Đã gửi lại',
  'CLOSED': 'Đã xong',
};

const _itemLabels = {'REPLY': 'Viện trả lời', 'HANDLING_DIRECTION': 'Cách viện xử lý', 'REOPEN': 'Bạn hỏi lại'};

String _typeLabel(String? code) => residentTypes.firstWhere((t) => t.$1 == code, orElse: () => ('', '$code', '')).$2;

String _uuid() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}

class ResidentFeedbackScreen extends StatefulWidget {
  const ResidentFeedbackScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<ResidentFeedbackScreen> createState() => _ResidentFeedbackScreenState();
}

class _ResidentFeedbackScreenState extends State<ResidentFeedbackScreen> {
  List<Map<String, dynamic>>? _items;
  bool _failed = false;
  String? _notice;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final list = await widget.api.getList('/resident-channel/feedback');
      if (mounted) {
        setState(() {
          _items = list.cast<Map<String, dynamic>>();
          _failed = false;
        });
      }
    } on Exception {
      if (mounted) setState(() => _failed = true);
    }
  }

  Future<void> _new() async {
    final sent = await Navigator.of(
      context,
    ).push<bool>(MaterialPageRoute(builder: (_) => _NewFeedbackScreen(api: widget.api)));
    if (sent == true) {
      setState(() => _notice = 'Đã gửi tới viện. Viện sẽ trả lời trong ứng dụng.');
      await _load();
    }
  }

  Future<void> _open(Map<String, dynamic> f) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => _FeedbackDetailScreen(api: widget.api, id: f['id'] as int),
      ),
    );
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final items = _items;
    return Scaffold(
      appBar: residentBackBar(context),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('Góp ý với viện', style: text.headlineMedium),
            const SizedBox(height: 16),
            FilledButton(onPressed: _new, child: const Text('Gửi góp ý mới')),
            if (_notice != null) ...[
              const SizedBox(height: 12),
              Semantics(liveRegion: true, child: Text(_notice!, style: text.bodyLarge)),
            ],
            const SizedBox(height: 16),
            if (_failed) Text('Máy chưa kết nối mạng.', style: text.bodyLarge),
            if (items != null && items.isEmpty) Text('Bạn chưa gửi góp ý nào.', style: text.bodyLarge),
            for (final f in items ?? const <Map<String, dynamic>>[])
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        '${_typeLabel(f['type'] as String?)}, ${formatDateTime(DateTime.parse(f['submittedAt'] as String))}',
                        style: text.bodyMedium,
                      ),
                      Text(_status[f['status']] ?? '${f['status']}', style: text.titleMedium),
                      const SizedBox(height: 4),
                      Text('${f['content']}', style: text.bodyLarge),
                      if (f['status'] == 'REPLIED' || f['status'] == 'CLOSED') ...[
                        const SizedBox(height: 8),
                        OutlinedButton(onPressed: () => _open(f), child: const Text('Xem trả lời')),
                      ],
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// Gửi mới: chọn loại; Mong muốn chọn về việc gì (chủ đề); loại khác chọn về việc gì (nhóm nội dung); viết; gửi.
class _NewFeedbackScreen extends StatefulWidget {
  const _NewFeedbackScreen({required this.api});

  final ApiClient api;

  @override
  State<_NewFeedbackScreen> createState() => _NewFeedbackScreenState();
}

class _NewFeedbackScreenState extends State<_NewFeedbackScreen> {
  final _content = TextEditingController();
  final _key = _uuid();
  String? _type;
  String? _subject;
  bool _submitted = false;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _content.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    setState(() => _submitted = true);
    if (_content.text.trim().isEmpty) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    final wish = _type == 'WISH';
    try {
      await widget.api.post(
        '/resident-channel/feedback',
        {
          'type': _type,
          // Mong muốn: nhóm đặt theo chủ đề như backend (FR-059)
          'group': wish ? (_subject == 'VISITORS_PICKERS' ? 'OTHER' : 'CARE') : _subject,
          if (wish) 'wishTopic': _subject,
          'content': _content.text.trim(),
        },
        headers: {'Idempotency-Key': _key},
      );
      if (mounted) Navigator.of(context).pop(true);
    } on ApiUnreachable {
      if (mounted) setState(() => _error = 'Máy chưa kết nối mạng. Góp ý chưa gửi được.');
    } on ApiProblem catch (e) {
      if (mounted) setState(() => _error = e.detail ?? 'Chưa gửi được. Thử lại sau.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _choice(String label, String? hint, VoidCallback onTap) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: OutlinedButton(
        onPressed: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Column(
            children: [
              Text(label),
              if (hint != null) Text(hint, style: text.bodyMedium),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final List<Widget> step;
    if (_type == null) {
      step = [
        Text('Bạn muốn gửi gì?', style: text.titleLarge),
        const SizedBox(height: 12),
        for (final t in residentTypes) _choice(t.$2, t.$3, () => setState(() => _type = t.$1)),
      ];
    } else if (_subject == null) {
      final options = _type == 'WISH' ? _wishTopics : _groups;
      step = [
        Text(_type == 'WISH' ? 'Mong muốn về việc gì?' : 'Về việc gì?', style: text.titleLarge),
        const SizedBox(height: 12),
        for (final o in options) _choice(o.$2, null, () => setState(() => _subject = o.$1)),
      ];
    } else {
      final missing = _submitted && _content.text.trim().isEmpty;
      step = [
        Text('Viết điều bạn muốn nói', style: text.titleLarge),
        const SizedBox(height: 12),
        TextField(
          controller: _content,
          minLines: 4,
          maxLines: 8,
          maxLength: 4000,
          decoration: InputDecoration(
            hintText: 'Có thể bấm biểu tượng micrô trên bàn phím để nói',
            errorText: missing ? 'Viết điều muốn nói' : null,
          ),
        ),
        if (_error != null) Semantics(liveRegion: true, child: Text(_error!, style: text.bodyLarge)),
        const SizedBox(height: 12),
        FilledButton(onPressed: _busy ? null : _send, child: const Text('Gửi tới viện')),
      ];
    }
    return Scaffold(
      appBar: residentBackBar(context),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('Góp ý mới', style: text.headlineMedium),
            const SizedBox(height: 16),
            ...step,
          ],
        ),
      ),
    );
  }
}

/// Trả lời của viện; trả lời về sức khỏe chỉ phần chung (FR-058a (b)); Đồng ý đóng, Hỏi lại theo lệnh backend cho phép.
class _FeedbackDetailScreen extends StatefulWidget {
  const _FeedbackDetailScreen({required this.api, required this.id});

  final ApiClient api;
  final int id;

  @override
  State<_FeedbackDetailScreen> createState() => _FeedbackDetailScreenState();
}

class _FeedbackDetailScreenState extends State<_FeedbackDetailScreen> {
  final _reason = TextEditingController();
  final _key = _uuid();
  Map<String, dynamic>? _detail;
  bool _reopening = false;
  bool _submitted = false;
  String? _message;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final d = await widget.api.get('/feedback/${widget.id}');
      if (mounted) setState(() => _detail = d);
    } on Exception {
      if (mounted) setState(() => _message = 'Máy chưa kết nối mạng.');
    }
  }

  Future<void> _command(String name, Map<String, dynamic> body) async {
    try {
      await widget.api.post('/feedback/${widget.id}/$name', body, headers: {'Idempotency-Key': _key});
      if (mounted) Navigator.of(context).pop();
    } on ApiUnreachable {
      if (mounted) setState(() => _message = 'Máy chưa kết nối mạng. Chưa gửi được.');
    } on ApiProblem catch (e) {
      if (mounted) setState(() => _message = e.detail ?? 'Chưa gửi được. Thử lại sau.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final d = _detail;
    final allowed = (d?['allowedCommands'] as List<dynamic>? ?? const []).cast<String>();
    final replies = (d?['items'] as List<dynamic>? ?? const [])
        .cast<Map<String, dynamic>>()
        .where((i) => i['type'] != 'CONTENT')
        .toList();
    return Scaffold(
      appBar: residentBackBar(context),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('Trả lời của viện', style: text.headlineMedium),
            const SizedBox(height: 16),
            if (d != null) ...[
              Text('${d['content']}', style: text.bodyLarge),
              const SizedBox(height: 12),
              for (final r in replies)
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(_itemLabels[r['type']] ?? '${r['type']}', style: text.titleMedium),
                        Text('${r['content']}', style: text.bodyLarge),
                      ],
                    ),
                  ),
                ),
              if (d['healthReplyHidden'] == true)
                Text('Câu trả lời về sức khỏe sẽ được nhân viên nói trực tiếp.', style: text.bodyLarge),
              const SizedBox(height: 16),
              if (allowed.contains('agree-to-close') && !_reopening) ...[
                FilledButton(onPressed: () => _command('agree-to-close', const {}), child: const Text('Đồng ý đóng')),
                const SizedBox(height: 12),
              ],
              if (allowed.contains('reopen') && !_reopening)
                OutlinedButton(onPressed: () => setState(() => _reopening = true), child: const Text('Hỏi lại')),
              if (_reopening) ...[
                TextField(
                  controller: _reason,
                  minLines: 2,
                  maxLines: 4,
                  maxLength: 2000,
                  decoration: InputDecoration(
                    labelText: 'Vì sao bạn muốn hỏi lại',
                    errorText: _submitted && _reason.text.trim().isEmpty ? 'Viết lý do' : null,
                  ),
                ),
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: () {
                    setState(() => _submitted = true);
                    if (_reason.text.trim().isEmpty) return;
                    _command('reopen', {'reason': _reason.text.trim()});
                  },
                  child: const Text('Gửi lại tới viện'),
                ),
              ],
            ],
            if (_message != null) Semantics(liveRegion: true, child: Text(_message!, style: text.bodyLarge)),
          ],
        ),
      ),
    );
  }
}
