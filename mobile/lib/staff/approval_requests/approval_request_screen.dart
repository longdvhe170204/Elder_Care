import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/status_pill.dart';
import 'approval_labels.dart';

/// Chi tiết một yêu cầu phê duyệt (000 T098; FR-033 → FR-040), màn riêng vì khung chi tiết dài (DESIGN.md 15.3), cùng thứ
/// tự nội dung với web. Nút theo `allowedCommands`; backend vẫn kiểm mọi lệnh. Ý kiến bắt buộc khi từ chối; tự duyệt thiếu
/// ý kiến thì backend trả 422, báo tại ô. Sửa nội dung nháp chưa có trên màn, như web.
class ApprovalRequestScreen extends StatefulWidget {
  const ApprovalRequestScreen({
    super.key,
    required this.api,
    required this.id,
    required this.displayCode,
    required this.onChanged,
  });

  final ApiClient api;
  final int id;
  final String displayCode;
  final ValueChanged<Map<String, dynamic>> onChanged;

  @override
  State<ApprovalRequestScreen> createState() => _ApprovalRequestScreenState();
}

class _ApprovalRequestScreenState extends State<ApprovalRequestScreen> {
  final _comment = TextEditingController();
  final _cancelReason = TextEditingController();
  Map<String, dynamic>? _request;
  List<Map<String, dynamic>>? _timeline;
  Object? _loadError;
  String? _commentError;
  String? _cancelError;
  String? _failure;
  bool _cancelling = false;
  bool _busy = false;

  String get _path => '/approval-requests/${widget.id}';

  @override
  void initState() {
    super.initState();
    _load();
    _loadTimeline();
  }

  @override
  void dispose() {
    _comment.dispose();
    _cancelReason.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final request = await widget.api.get(_path);
      if (mounted) setState(() => _request = request);
      if (request != null) widget.onChanged(request);
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  /// Không có quyền xem nhật ký của yêu cầu (403) hoặc không tìm thấy: ẩn mục Diễn biến.
  Future<void> _loadTimeline() async {
    try {
      final page = await widget.api.get('/audit-logs/entities/M15.APPROVAL_REQUEST/${widget.id}');
      final items = (page?['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _timeline = items);
    } on Exception {
      if (mounted) setState(() => _timeline = null);
    }
  }

  Future<void> _run(String command, Map<String, dynamic> body, void Function(String) showAtField) async {
    setState(() {
      _busy = true;
      _failure = null;
      _commentError = null;
      _cancelError = null;
    });
    try {
      final changed = await widget.api.post(
        '$_path/$command',
        body,
        headers: {'If-Match': '"${_request!['version']}"'},
      );
      if (!mounted || changed == null) return;
      setState(() {
        _request = changed;
        _cancelling = false;
      });
      _comment.clear();
      _cancelReason.clear();
      widget.onChanged(changed);
      await _loadTimeline();
    } on ApiProblem catch (p) {
      if (!mounted) return;
      if (p.status == 422) {
        setState(() => showAtField(backendMessage(p)));
      } else {
        setState(() => _failure = backendMessage(p));
        // Người khác đã xử lý (409) hoặc đã đổi (412): tải lại để thấy trạng thái mới
        if (p.status == 409 || p.status == 412) await _load();
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _approve() {
    final comment = _comment.text.trim();
    _run('approve', {if (comment.isNotEmpty) 'comment': comment}, (m) => _commentError = m);
  }

  void _reject() {
    final comment = _comment.text.trim();
    if (comment.isEmpty) {
      setState(() => _commentError = 'Nhập ý kiến khi từ chối');
      return;
    }
    _run('reject', {'comment': comment}, (m) => _commentError = m);
  }

  void _cancel() {
    final reason = _cancelReason.text.trim();
    if (reason.isEmpty) {
      setState(() => _cancelError = 'Nhập lý do hủy');
      return;
    }
    _run('cancel', {'reason': reason}, (m) => _cancelError = m);
  }

  @override
  Widget build(BuildContext context) {
    final request = _request;
    return Scaffold(
      appBar: headerAppBar(
        context,
        leading: IconButton(
          icon: const Icon(LucideIcons.arrowLeft),
          tooltip: 'Quay lại',
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: widget.displayCode,
      ),
      body: _loadError != null
          ? Padding(padding: const EdgeInsets.all(16), child: Text(backendMessage(_loadError!)))
          : request == null
          ? const Center(child: CircularProgressIndicator())
          : SafeArea(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(child: _content(context, request)),
                  _actions(request),
                ],
              ),
            ),
    );
  }

  Widget _content(BuildContext context, Map<String, dynamic> r) {
    final text = Theme.of(context).textTheme;
    final allowed = (r['allowedCommands'] as List<dynamic>? ?? const []).cast<String>();
    final content = (r['content'] as Map<String, dynamic>?) ?? const {};
    final status = r['status'] as String;
    final decidedAt = r['decidedAt'] as String?;
    final comment = r['decisionComment'] as String?;
    final closing = r['closingReason'] as String?;
    final effective = _effectiveLine(r);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(typeLabel(r['type'] as String), style: text.titleMedium),
        const SizedBox(height: 4),
        Align(
          alignment: Alignment.centerLeft,
          child: StatusPill(label: statusLabel(status), tone: statusTone(status)),
        ),
        if (effective != null) Text(effective, style: text.titleSmall),
        if (closing != null) Text('Lý do đóng: $closing', style: text.bodyMedium),
        const SizedBox(height: 12),
        _Line('Người yêu cầu', actorLabel(r['requester'] as Map<String, dynamic>?)),
        _Line('Lập lúc', formatDateTime(DateTime.parse(r['createdAt'] as String))),
        _Line('Hiệu lực đề nghị', formatDate(r['requestedEffectiveDate'] as String)),
        _Line('Lý do', r['reason'] as String),
        if (decidedAt != null)
          _Line(
            'Quyết định',
            '${actorLabel(r['decidedBy'] as Map<String, dynamic>?)}, ${formatDateTime(DateTime.parse(decidedAt))}'
                '${r['selfApproved'] == true ? ' (tự duyệt)' : ''}',
          ),
        if (comment != null) _Line('Ý kiến', comment),
        if (content.isNotEmpty) ...[
          const SizedBox(height: 12),
          Text('Nội dung', style: text.labelMedium),
          for (final e in content.entries) Text('${e.key}: ${_valueText(e.value)}', style: text.bodyMedium),
        ],
        if (_failure != null) ...[
          const SizedBox(height: 12),
          Text(_failure!, style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error)),
        ],
        if (allowed.contains('approve') || allowed.contains('reject')) ...[
          const SizedBox(height: 16),
          TextField(
            controller: _comment,
            maxLength: 2000,
            maxLines: 3,
            decoration: InputDecoration(
              labelText: 'Ý kiến',
              helperText: 'Bắt buộc khi từ chối, hoặc khi duyệt yêu cầu do chính mình lập.',
              helperMaxLines: 2,
              errorText: _commentError,
            ),
          ),
        ],
        if (_cancelling) ...[
          const SizedBox(height: 16),
          TextField(
            controller: _cancelReason,
            maxLength: 2000,
            maxLines: 3,
            decoration: InputDecoration(labelText: 'Lý do hủy', errorText: _cancelError),
          ),
        ],
        if (_timeline?.isNotEmpty ?? false) ...[
          const SizedBox(height: 16),
          Text('Diễn biến', style: text.labelMedium),
          for (final entry in _timeline!) _TimelineEntry(entry: entry),
        ],
      ],
    );
  }

  /// Nút chính cố định ở chân màn (DESIGN.md 15.3); lùi lên theo bàn phím vì nằm trong thân `Scaffold`.
  Widget _actions(Map<String, dynamic> r) {
    final allowed = (r['allowedCommands'] as List<dynamic>? ?? const []).cast<String>();
    final buttons = <Widget>[
      if (_cancelling) ...[
        FilledButton(onPressed: _busy ? null : _cancel, child: const Text('Xác nhận hủy')),
        OutlinedButton(
          onPressed: _busy ? null : () => setState(() => _cancelling = false),
          child: const Text('Không hủy'),
        ),
      ] else ...[
        if (allowed.contains('approve')) FilledButton(onPressed: _busy ? null : _approve, child: const Text('Duyệt')),
        if (allowed.contains('reject')) OutlinedButton(onPressed: _busy ? null : _reject, child: const Text('Từ chối')),
        if (allowed.contains('submit'))
          FilledButton(
            onPressed: _busy ? null : () => _run('submit', const {}, (m) => _failure = m),
            child: const Text('Gửi duyệt'),
          ),
        if (allowed.contains('cancel'))
          OutlinedButton(
            onPressed: _busy ? null : () => setState(() => _cancelling = true),
            child: const Text('Hủy yêu cầu'),
          ),
      ],
    ];
    if (buttons.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
      child: Row(
        children: [
          for (var i = 0; i < buttons.length; i++) ...[
            if (i > 0) const SizedBox(width: 12),
            Expanded(child: buttons[i]),
          ],
        ],
      ),
    );
  }
}

String? _effectiveLine(Map<String, dynamic> r) {
  final actual = r['actualEffectiveDate'] as String?;
  if (r['status'] == 'APPLIED' && actual != null) {
    return 'Áp dụng ngày ${formatDate(actual)}${r['catchUpApplied'] == true ? ' (áp dụng bù)' : ''}';
  }
  if (r['status'] == 'APPROVED') {
    return 'Chờ hiệu lực ngày ${formatDate(r['requestedEffectiveDate'] as String)}';
  }
  return null;
}

String _valueText(Object? value) => value == null ? 'trống' : value.toString();

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

class _TimelineEntry extends StatelessWidget {
  const _TimelineEntry({required this.entry});

  final Map<String, dynamic> entry;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final action = entry['action'] as String;
    final reason = entry['reason'] as String?;
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(actionLabels[action] ?? action, style: text.bodyMedium),
          Text(
            '${formatDateTime(DateTime.parse(entry['occurredAt'] as String))}, '
            '${actorLabel(entry['actor'] as Map<String, dynamic>?)}',
            style: text.bodySmall,
          ),
          if (reason != null) Text(reason, style: text.bodySmall),
        ],
      ),
    );
  }
}
