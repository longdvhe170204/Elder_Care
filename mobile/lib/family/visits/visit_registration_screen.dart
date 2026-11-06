import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/status_pill.dart';

const _statusLabels = {
  'APPROVED': ('Đã duyệt', StatusTone.settled),
  'CHECKED_IN': ('Đã vào', StatusTone.occupied),
  'CHECKED_OUT': ('Đã ra', StatusTone.neutral),
  'NO_SHOW': ('Không đến', StatusTone.neutral),
  'CANCELLED': ('Hủy', StatusTone.neutral),
};

String _two(int v) => v.toString().padLeft(2, '0');
String _iso(DateTime d) => '${d.year}-${_two(d.month)}-${_two(d.day)}';

/// Đăng ký thăm trên app người thân (012 T025; US4, FR-029 → FR-031, FR-035; SC-004 trong 2 phút). Chọn ngày (tối đa 14
/// ngày tới, CFG-M10-05), khung còn chỗ, người đi cùng; đạt thì lượt Đã duyệt ngay, không đạt thì hiện mọi lý do và
/// không tạo lượt. Lượt Đã duyệt của mình hủy được trước giờ bắt đầu. Chỉ trực tuyến. Không chọn sẵn ngày, khung.
class VisitRegistrationScreen extends StatefulWidget {
  const VisitRegistrationScreen({
    super.key,
    required this.api,
    required this.residentId,
    required this.residentName,
    this.today,
  });

  final ApiClient api;
  final int residentId;
  final String residentName;

  /// Ngày hiện tại theo giờ Việt Nam; test truyền ngày cố định.
  final DateTime? today;

  @override
  State<VisitRegistrationScreen> createState() => _VisitRegistrationScreenState();
}

class _Person {
  final name = TextEditingController();
  final relationship = TextEditingController();

  void dispose() {
    name.dispose();
    relationship.dispose();
  }
}

class _VisitRegistrationScreenState extends State<VisitRegistrationScreen> {
  final _companions = <_Person>[];
  String _key = _uuid();
  DateTime? _date;
  List<Map<String, dynamic>>? _slots;
  Map<String, dynamic>? _slot;
  List<Map<String, dynamic>>? _visits;
  bool _submitted = false;
  bool _busy = false;
  String? _error;
  List<String> _reasons = const [];
  String? _done;

  DateTime get _today {
    final t = widget.today ?? DateTime.now().toUtc().add(const Duration(hours: 7));
    return DateTime(t.year, t.month, t.day);
  }

  @override
  void initState() {
    super.initState();
    _loadVisits();
  }

  @override
  void dispose() {
    for (final p in _companions) {
      p.dispose();
    }
    super.dispose();
  }

  Future<void> _loadVisits() async {
    try {
      final rows = (await widget.api.getList('/visits?residentId=${widget.residentId}')).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _visits = rows);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Future<void> _pickDate(DateTime date) async {
    setState(() {
      _date = date;
      _slot = null;
      _slots = null;
    });
    try {
      final slots = (await widget.api.getList('/visit-slots?date=${_iso(date)}')).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _slots = slots);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  bool get _companionMissing =>
      _companions.any((p) => p.name.text.trim().isEmpty || p.relationship.text.trim().isEmpty);

  Future<void> _submit() async {
    setState(() {
      _submitted = true;
      _done = null;
    });
    if (_date == null || _slot == null || _companionMissing || _busy) return;
    setState(() {
      _busy = true;
      _error = null;
      _reasons = const [];
    });
    try {
      await widget.api.post(
        '/visits',
        {
          'residentId': widget.residentId,
          'date': _iso(_date!),
          'startTime': _slot!['startTime'],
          'endTime': _slot!['endTime'],
          'companions': [
            for (final p in _companions) {'fullName': p.name.text.trim(), 'relationship': p.relationship.text.trim()},
          ],
        },
        headers: {'Idempotency-Key': _key},
      );
      _key = _uuid();
      for (final p in _companions) {
        p.dispose();
      }
      if (!mounted) return;
      setState(() {
        _companions.clear();
        _slot = null;
        _submitted = false;
        _done = 'Đã đăng ký, lượt thăm đã được duyệt';
      });
      await _loadVisits();
      if (_date != null) await _pickDate(_date!);
    } on ApiProblem catch (e) {
      final conditions = (e.properties['unmetConditions'] as List?) ?? const [];
      if (mounted) {
        setState(() {
          _reasons = [for (final c in conditions) '${(c as Map)['description']}'];
          _error = _reasons.isEmpty ? backendMessage(e) : 'Chưa đăng ký được:';
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _cancel(Map<String, dynamic> visit) async {
    try {
      await widget.api.post('/visits/${visit['id']}/cancel', const {}, headers: {'Idempotency-Key': _uuid()});
      await _loadVisits();
      if (_date != null) await _pickDate(_date!);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  String _slotText(Map<String, dynamic> s) {
    final left = s['seatsLeft'] as int;
    return '${s['startTime']}–${s['endTime']}, ${left > 0 ? 'còn $left chỗ' : 'hết chỗ'}';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tones = theme.extension<Tones>()!;
    final days = [for (var i = 0; i <= 14; i++) _today.add(Duration(days: i))];
    final slots = _slots;
    final visits = _visits;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Đăng ký thăm ${widget.residentName}', style: theme.textTheme.titleLarge),
          const SizedBox(height: 4),
          Text(
            'Lượt đạt điều kiện được duyệt ngay. Đổi khung thì hủy lượt cũ rồi đăng ký lại.',
            style: theme.textTheme.bodySmall,
          ),
          const SizedBox(height: 16),
          Text('Ngày', style: theme.textTheme.titleSmall),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                for (final d in days)
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text('${_two(d.day)}/${_two(d.month)}'),
                      selected: _date == d,
                      onSelected: _busy ? null : (_) => _pickDate(d),
                    ),
                  ),
              ],
            ),
          ),
          if (_submitted && _date == null)
            Text('Chọn ngày', style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
          if (_date != null) ...[
            const SizedBox(height: 12),
            Text('Khung giờ', style: theme.textTheme.titleSmall),
            if (slots == null)
              const Center(child: CircularProgressIndicator())
            else
              RadioGroup<String>(
                groupValue: _slot == null ? null : '${_slot!['startTime']}',
                onChanged: (v) => setState(() => _slot = slots.firstWhere((s) => s['startTime'] == v)),
                child: Column(
                  children: [
                    for (final s in slots)
                      RadioListTile<String>(
                        value: '${s['startTime']}',
                        title: Text(_slotText(s)),
                        enabled: !_busy && (s['seatsLeft'] as int) > 0,
                      ),
                  ],
                ),
              ),
          ],
          if (_submitted && _slot == null)
            Text('Chọn khung giờ', style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
          const SizedBox(height: 12),
          for (final (i, p) in _companions.indexed) ...[
            TextField(
              controller: p.name,
              decoration: InputDecoration(
                labelText: 'Họ tên người đi cùng ${i + 1}',
                errorText: _submitted && p.name.text.trim().isEmpty ? 'Nhập họ tên' : null,
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: p.relationship,
              decoration: InputDecoration(
                labelText: 'Quan hệ người đi cùng ${i + 1}',
                errorText: _submitted && p.relationship.text.trim().isEmpty ? 'Nhập quan hệ' : null,
              ),
            ),
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton(
                onPressed: () => setState(() => _companions.removeAt(i).dispose()),
                child: Text('Bỏ người đi cùng ${i + 1}'),
              ),
            ),
          ],
          Align(
            alignment: Alignment.centerLeft,
            child: OutlinedButton(
              onPressed: _busy ? null : () => setState(() => _companions.add(_Person())),
              child: const Text('Thêm người đi cùng'),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Semantics(
              liveRegion: true,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(_error!, style: theme.textTheme.bodyMedium?.copyWith(color: tones.attentionInk)),
                  for (final r in _reasons) Text(r, style: theme.textTheme.bodyMedium),
                ],
              ),
            ),
          ],
          if (_done != null) ...[
            const SizedBox(height: 12),
            Semantics(liveRegion: true, child: Text(_done!, style: theme.textTheme.bodyMedium)),
          ],
          const SizedBox(height: 16),
          FilledButton(onPressed: _busy ? null : _submit, child: const Text('Đăng ký thăm')),
          const SizedBox(height: 24),
          Text('Lượt thăm của bạn', style: theme.textTheme.titleMedium),
          if (visits == null)
            const Center(child: CircularProgressIndicator())
          else if (visits.isEmpty)
            Text('Chưa có lượt thăm nào.', style: theme.textTheme.bodyMedium)
          else
            for (final v in visits) _VisitRow(visit: v, onCancel: () => _cancel(v)),
        ],
      ),
    );
  }
}

class _VisitRow extends StatelessWidget {
  const _VisitRow({required this.visit, required this.onCancel});

  final Map<String, dynamic> visit;
  final VoidCallback onCancel;

  @override
  Widget build(BuildContext context) {
    final date = (visit['date'] as String).split('-').reversed.join('/');
    final (label, tone) = _statusLabels[visit['status']] ?? ('${visit['status']}', StatusTone.neutral);
    return ListTile(
      minTileHeight: 64,
      contentPadding: EdgeInsets.zero,
      title: Text('$date, ${visit['startTime']}–${visit['endTime']}'),
      subtitle: Align(
        alignment: Alignment.centerLeft,
        child: StatusPill(label: label, tone: tone),
      ),
      trailing: visit['status'] == 'APPROVED' ? TextButton(onPressed: onCancel, child: const Text('Hủy lượt')) : null,
    );
  }
}

/// Khóa idempotency dạng UUID v4 cho một lần đăng ký.
String _uuid() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
