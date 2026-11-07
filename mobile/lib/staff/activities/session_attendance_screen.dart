import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/offline/offline_queue.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/page_body.dart';
import '../care_tasks/care_offline_recording.dart';

const participationLevels = {
  'ACTIVE': 'Tích cực',
  'NORMAL': 'Bình thường',
  'PASSIVE': 'Thụ động',
  'LEFT_EARLY': 'Bỏ giữa chừng',
};
const absenceReasons = {
  'REFUSED': 'Từ chối',
  'HEALTH': 'Sức khỏe',
  'AWAY': 'Đang vắng mặt',
  'NO_SHOW': 'Không đến',
  'OTHER': 'Khác',
};

/// 014 T013, T014 (FR-027 → FR-030, SC-009): điểm danh buổi trong viện trên app, cùng thứ tự và câu chữ với khối "Điểm
/// danh" của web (bố cục duyệt 05/11/2026). Mỗi đăng ký chưa có kết quả một dòng: Có mặt kèm mức tham gia bắt buộc, Vắng
/// kèm lý do bắt buộc. Lưu qua hàng đợi ngoại tuyến chung (phong bì `M04.SESSION_ATTENDANCE`); không tự điền sẵn kết quả.
class SessionAttendanceScreen extends StatefulWidget {
  const SessionAttendanceScreen({
    super.key,
    required this.api,
    required this.queue,
    required this.accountId,
    required this.sessionId,
  });
  final ApiClient api;
  final OfflineQueue queue;
  final int accountId;
  final int sessionId;
  @override
  State<SessionAttendanceScreen> createState() => _SessionAttendanceScreenState();
}

class _Entry {
  String? result;
  String? level;
  String? reason;
  bool get complete => result == 'PRESENT' ? level != null : result == 'ABSENT' && reason != null;
  Map<String, dynamic> toJson(int residentId) => {
    'residentId': residentId,
    'result': result,
    if (result == 'PRESENT') 'participationLevel': level,
    if (result == 'ABSENT') 'absenceReason': reason,
  };
}

class _SessionAttendanceScreenState extends State<SessionAttendanceScreen> {
  Map<String, dynamic>? _session;
  Map<int, String> _names = {};
  final Map<int, _Entry> _entries = {};
  String? _message;
  bool _busy = false;
  String? _key;
  late final CareOfflineRecording _recorder = CareOfflineRecording(widget.api, widget.queue, widget.accountId);

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final session = await widget.api.requestJson('GET', '/activity-sessions/${widget.sessionId}');
      final residents = await widget.api.requestJson('GET', '/residents');
      final items = residents is Map<String, dynamic> ? residents['items'] as List? ?? const [] : const [];
      if (!mounted) return;
      setState(() {
        _session = session as Map<String, dynamic>;
        _names = {for (final r in items.whereType<Map<String, dynamic>>()) r['id'] as int: r['fullName'] as String};
      });
    } catch (e) {
      if (mounted) setState(() => _message = backendMessage(e));
    }
  }

  List<Map<String, dynamic>> get _open => [
    for (final r in (_session?['registrations'] as List? ?? const []).whereType<Map<String, dynamic>>())
      if (r['status'] == 'REGISTERED') r,
  ];

  Future<void> _save() async {
    final done = [
      for (final r in _open)
        if (_entries[r['id']]?.complete ?? false) _entries[r['id']]!.toJson(r['residentId'] as int),
    ];
    setState(() {
      _busy = true;
      _message = null;
    });
    // Cùng nội dung thì giữ cùng khóa khi thử lại (BR-M15-20).
    _key ??= UniqueKey().toString();
    try {
      final kept = await _recorder.recordSessionAttendance(widget.sessionId, done, _key!);
      _key = null;
      if (mounted) setState(() => _message = kept ? 'Đã lưu trên máy, sẽ gửi khi có mạng' : 'Đã lưu điểm danh');
    } catch (e) {
      if (mounted) setState(() => _message = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tones = Theme.of(context).extension<Tones>()!;
    final rows = _open;
    final ready = rows.where((r) => _entries[r['id']]?.complete ?? false).length;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Điểm danh buổi'),
        backgroundColor: tones.header,
        foregroundColor: tones.headerInkStrong,
      ),
      bottomNavigationBar: _session == null
          ? null
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (_message != null) Semantics(liveRegion: true, child: Text(_message!)),
                    const SizedBox(height: 8),
                    FilledButton(
                      style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
                      onPressed: _busy || ready == 0 ? null : _save,
                      child: Text('Lưu điểm danh ($ready/${rows.length})'),
                    ),
                  ],
                ),
              ),
            ),
      body: PageBody(
        child: _session == null
            ? (_message == null ? const Center(child: CircularProgressIndicator()) : Text(_message!))
            : ListView(
                children: [
                  Text(
                    'Bắt đầu ${formatDateTime(DateTime.parse(_session!['startsAt'] as String))}',
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  const SizedBox(height: 12),
                  if (rows.isEmpty) const Text('Mọi đăng ký đã có kết quả điểm danh.'),
                  for (final r in rows) _row(context, r),
                ],
              ),
      ),
    );
  }

  Widget _row(BuildContext context, Map<String, dynamic> registration) {
    final id = registration['id'] as int;
    final entry = _entries.putIfAbsent(id, _Entry.new);
    final name = _names[registration['residentId']] ?? 'Người cao tuổi ${registration['residentId']}';
    Widget choices(String label, Map<String, String> values, String? value, void Function(String) pick) => Semantics(
      label: label,
      child: Wrap(
        spacing: 8,
        runSpacing: 8,
        children: [
          for (final e in values.entries)
            ChoiceChip(
              label: Text(e.value),
              selected: value == e.key,
              onSelected: _busy ? null : (_) => setState(() => pick(e.key)),
            ),
        ],
      ),
    );
    return Card(
      key: ValueKey('attendance-$id'),
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(name, style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 8),
            choices('Kết quả của $name', const {'PRESENT': 'Có mặt', 'ABSENT': 'Vắng'}, entry.result, (v) {
              entry.result = v;
              entry.level = null;
              entry.reason = null;
            }),
            if (entry.result == 'PRESENT') ...[
              const SizedBox(height: 8),
              Text('Mức tham gia (bắt buộc)', style: Theme.of(context).textTheme.labelLarge),
              choices('Mức tham gia của $name', participationLevels, entry.level, (v) => entry.level = v),
            ],
            if (entry.result == 'ABSENT') ...[
              const SizedBox(height: 8),
              Text('Lý do vắng (bắt buộc)', style: Theme.of(context).textTheme.labelLarge),
              choices('Lý do vắng của $name', absenceReasons, entry.reason, (v) => entry.reason = v),
            ],
          ],
        ),
      ),
    );
  }
}
