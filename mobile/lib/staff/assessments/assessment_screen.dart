import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/notifications/notification_api.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/status_pill.dart';

/// Đánh giá của một người cao tuổi trên app nhân viên (001 T042; US3; FR-025, FR-026, FR-029; BR-M01-13): lịch sử các lần
/// đánh giá, lập bản nháp (đầu vào hoặc đánh giá lại), sửa và hủy bản nháp. Không có ô điểm, không có mức đề xuất (FR-024):
/// nhận xét và mức chăm sóc do người dùng tự nhập. Xác nhận lần đánh giá làm trên web (chỉ Bác sĩ). Chỉ trực tuyến.
/// Lưu bản nháp gửi `If-Match` lấy từ header `ETag` của lần đọc gần nhất (000 FR-040), 412 thì đọc lại.
class AssessmentScreen extends StatefulWidget {
  const AssessmentScreen({super.key, required this.api, required this.residentId, required this.allowedCommands});

  final ApiClient api;
  final int residentId;
  final List<String> allowedCommands;

  @override
  State<AssessmentScreen> createState() => _AssessmentScreenState();
}

const _statusLabels = {'DRAFT': 'Bản nháp', 'CONFIRMED': 'Đã xác nhận', 'DISCARDED': 'Đã hủy'};
const _typeLabels = {'INITIAL': 'Đánh giá đầu vào', 'REASSESSMENT': 'Đánh giá lại'};
const _careLevelLabels = {
  'BASIC': 'Chăm sóc cơ bản',
  'REGULAR': 'Chăm sóc thường xuyên',
  'SPECIAL': 'Chăm sóc đặc biệt',
};

class _AssessmentScreenState extends State<AssessmentScreen> {
  List<Map<String, dynamic>>? _items;
  Object? _loadError;

  bool get _canCreate => widget.allowedCommands.contains('CREATE_ASSESSMENT');

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = (await widget.api.getList(
        '/residents/${widget.residentId}/assessments',
      )).cast<Map<String, dynamic>>();
      if (mounted) {
        setState(() {
          _items = items;
          _loadError = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _items = const [];
          _loadError = e;
        });
      }
    }
  }

  Future<void> _openDraft(int id) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => _DraftEditor(api: widget.api, assessmentId: id),
      ),
    );
    if (mounted) await _load();
  }

  Future<void> _create() async {
    final created = await showModalBottomSheet<int>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => _CreateSheet(api: widget.api, residentId: widget.residentId),
    );
    if (created != null && mounted) await _openDraft(created);
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final secondary = text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant);
    final items = _items;
    final draft = items?.where((i) => i['status'] == 'DRAFT').firstOrNull;
    return Scaffold(
      appBar: headerAppBar(context, title: 'Đánh giá'),
      body: items == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (_loadError != null)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Text(
                      backendMessage(_loadError!),
                      style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error),
                    ),
                  ),
                if (draft != null)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: FilledButton(
                      onPressed: () => _openDraft(draft['id'] as int),
                      child: const Text('Sửa bản nháp'),
                    ),
                  )
                else if (_canCreate && _loadError == null)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: FilledButton(onPressed: _create, child: const Text('Lập đánh giá')),
                  ),
                for (final item in items) ...[
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Wrap(
                          spacing: 12,
                          crossAxisAlignment: WrapCrossAlignment.center,
                          children: [
                            Text(_typeLabels[item['type']] ?? '${item['type']}', style: text.titleSmall),
                            StatusPill(
                              label: _statusLabels[item['status']] ?? '${item['status']}',
                              tone: item['status'] == 'CONFIRMED' ? StatusTone.settled : StatusTone.neutral,
                            ),
                          ],
                        ),
                        if (item['careLevel'] != null)
                          Text(
                            'Mức chăm sóc: ${_careLevelLabels[item['careLevel']] ?? item['careLevel']}',
                            style: secondary,
                          ),
                        Text('Lập bởi ${item['createdBy']}', style: secondary),
                      ],
                    ),
                  ),
                  const Divider(height: 1),
                ],
              ],
            ),
    );
  }
}

/// Chọn loại và căn cứ rồi tạo bản nháp; trả mã lần đánh giá vừa tạo.
class _CreateSheet extends StatefulWidget {
  const _CreateSheet({required this.api, required this.residentId});

  final ApiClient api;
  final int residentId;

  @override
  State<_CreateSheet> createState() => _CreateSheetState();
}

class _CreateSheetState extends State<_CreateSheet> {
  String _type = 'INITIAL';
  final _basis = TextEditingController();
  Object? _failure;
  bool _busy = false;
  final _key = newIdempotencyKey();

  @override
  void dispose() {
    _basis.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _failure = null;
      _busy = true;
    });
    try {
      final basis = _basis.text.trim();
      final created = await widget.api.post(
        '/residents/${widget.residentId}/assessments',
        {'type': _type, if (basis.isNotEmpty) 'basis': basis},
        headers: {'Idempotency-Key': _key},
      );
      if (mounted) Navigator.of(context).pop(created?['id'] as int?);
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.of(context).viewInsets.bottom),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Lập đánh giá', style: text.titleMedium),
          const SizedBox(height: 12),
          SegmentedButton<String>(
            segments: [for (final e in _typeLabels.entries) ButtonSegment(value: e.key, label: Text(e.value))],
            selected: {_type},
            onSelectionChanged: (s) => setState(() => _type = s.first),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _basis,
            decoration: const InputDecoration(labelText: 'Căn cứ đánh giá'),
          ),
          if (_failure != null)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Text(
                backendMessage(_failure!),
                style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error),
              ),
            ),
          const SizedBox(height: 16),
          FilledButton(onPressed: _busy ? null : _submit, child: const Text('Tạo bản nháp')),
        ],
      ),
    );
  }
}

/// Sửa bản nháp: nhận xét, lưu ý chăm sóc, mức chăm sóc; lưu kèm `If-Match`, hủy kèm lý do.
class _DraftEditor extends StatefulWidget {
  const _DraftEditor({required this.api, required this.assessmentId});

  final ApiClient api;
  final int assessmentId;

  @override
  State<_DraftEditor> createState() => _DraftEditorState();
}

class _DraftEditorState extends State<_DraftEditor> {
  final _comment = TextEditingController();
  final _careNote = TextEditingController();
  final _reason = TextEditingController();
  String? _careLevel;
  String? _etag;
  bool _loaded = false;
  bool _busy = false;
  bool _discarding = false;
  bool _saved = false;
  String? _reasonError;
  Object? _failure;

  String get _path => '/assessments/${widget.assessmentId}';

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _comment.dispose();
    _careNote.dispose();
    _reason.dispose();
    super.dispose();
  }

  void _apply(ApiResponse response) {
    final body = response.body as Map<String, dynamic>? ?? const {};
    _comment.text = body['comment'] as String? ?? '';
    _careNote.text = body['careNote'] as String? ?? '';
    _careLevel = body['careLevel'] as String?;
    _etag = response.headers['etag'];
  }

  Future<void> _load() async {
    try {
      final response = await widget.api.requestResponse('GET', _path);
      if (mounted) {
        setState(() {
          _apply(response);
          _loaded = true;
        });
      }
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _failure = e;
          _loaded = true;
        });
      }
    }
  }

  Future<void> _save() async {
    setState(() {
      _failure = null;
      _saved = false;
      _busy = true;
    });
    try {
      final response = await widget.api.requestResponse(
        'PUT',
        _path,
        body: {'comment': _comment.text.trim(), 'careNote': _careNote.text.trim(), 'careLevel': ?_careLevel},
        headers: {'If-Match': ?_etag},
      );
      if (mounted) {
        setState(() {
          _apply(response);
          _saved = true;
        });
      }
    } on ApiProblem catch (e) {
      if (mounted) setState(() => _failure = e);
      if (e.status == 412) await _load();
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _discard() async {
    if (_reason.text.trim().isEmpty) {
      setState(() => _reasonError = 'Nhập lý do hủy');
      return;
    }
    setState(() {
      _reasonError = null;
      _failure = null;
      _busy = true;
    });
    try {
      await widget.api.post(
        '$_path/discard-draft',
        {'reason': _reason.text.trim()},
        headers: {'Idempotency-Key': newIdempotencyKey()},
      );
      if (mounted) Navigator.of(context).pop();
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;
    final tones = Theme.of(context).extension<Tones>();
    return Scaffold(
      appBar: headerAppBar(context, title: 'Bản nháp đánh giá'),
      body: !_loaded
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                TextField(
                  controller: _comment,
                  minLines: 3,
                  maxLines: 8,
                  decoration: const InputDecoration(labelText: 'Nhận xét đánh giá'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _careNote,
                  maxLength: 1000,
                  minLines: 2,
                  maxLines: 4,
                  decoration: const InputDecoration(labelText: 'Lưu ý chăm sóc'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _careLevel,
                  decoration: const InputDecoration(labelText: 'Mức chăm sóc'),
                  items: [
                    for (final e in _careLevelLabels.entries) DropdownMenuItem(value: e.key, child: Text(e.value)),
                  ],
                  onChanged: (v) => setState(() => _careLevel = v),
                ),
                if (_failure != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 12),
                    child: Text(backendMessage(_failure!), style: text.bodyMedium?.copyWith(color: scheme.error)),
                  ),
                if (_saved)
                  Padding(
                    padding: const EdgeInsets.only(top: 12),
                    child: Text('Đã lưu bản nháp', style: text.bodyMedium?.copyWith(color: tones?.settledInk)),
                  ),
                const SizedBox(height: 16),
                FilledButton(onPressed: _busy ? null : _save, child: const Text('Lưu bản nháp')),
                const SizedBox(height: 8),
                if (!_discarding)
                  OutlinedButton(onPressed: () => setState(() => _discarding = true), child: const Text('Hủy bản nháp'))
                else ...[
                  TextField(
                    controller: _reason,
                    decoration: InputDecoration(labelText: 'Lý do hủy', errorText: _reasonError),
                  ),
                  const SizedBox(height: 8),
                  OutlinedButton(onPressed: _busy ? null : _discard, child: const Text('Xác nhận hủy')),
                ],
              ],
            ),
    );
  }
}
