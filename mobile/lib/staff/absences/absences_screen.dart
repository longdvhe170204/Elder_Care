import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/status_pill.dart';

const _absenceTypes = {
  'HOME_VISIT': 'Về nhà',
  'FAMILY_OUTING': 'Đi chơi với gia đình',
  'SAME_DAY_MEDICAL_VISIT': 'Khám trong ngày',
  'OTHER': 'Lý do khác',
  'HOSPITAL': 'Chuyển viện',
};

/// Lượt vắng của một người cao tuổi và gia hạn dự kiến trở lại (004 T056; US6, FR-048, FR-049; chỉ trực tuyến, không vào hàng
/// đợi ngoại tuyến). Cho tạm vắng và Ghi nhận trở về là lệnh của 001 đi qua quy trình đón; Giải phóng giường chỉ Quản lý
/// viện nên không có ở đây. Gia hạn: Hành chính, Trưởng tầng; backend kiểm quyền, lỗi hiện tại thẻ.
class AbsencesScreen extends StatefulWidget {
  const AbsencesScreen({super.key, required this.api, required this.residentId});

  final ApiClient api;
  final int residentId;

  @override
  State<AbsencesScreen> createState() => _AbsencesScreenState();
}

class _AbsencesScreenState extends State<AbsencesScreen> {
  List<Map<String, dynamic>>? _items;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = (await widget.api.getList(
        '/absences?residentId=${widget.residentId}',
      )).cast<Map<String, dynamic>>();
      if (!mounted) return;
      setState(() {
        _items = items;
        _error = null;
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    if (_error != null) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(backendMessage(_error!), style: text.bodyMedium),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: _load, child: const Text('Tải lại')),
          ],
        ),
      );
    }
    final items = _items;
    if (items == null) return const Center(child: CircularProgressIndicator());
    if (items.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Text('Chưa có lượt vắng nào.', style: text.bodyMedium),
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      separatorBuilder: (_, _) => const SizedBox(height: 8),
      itemBuilder: (_, i) =>
          _AbsenceCard(key: ValueKey(items[i]['id']), api: widget.api, absence: items[i], onChanged: _load),
    );
  }
}

class _AbsenceCard extends StatefulWidget {
  const _AbsenceCard({super.key, required this.api, required this.absence, required this.onChanged});

  final ApiClient api;
  final Map<String, dynamic> absence;
  final Future<void> Function() onChanged;

  @override
  State<_AbsenceCard> createState() => _AbsenceCardState();
}

class _AbsenceCardState extends State<_AbsenceCard> {
  final _date = TextEditingController();
  final _time = TextEditingController();
  final _reason = TextEditingController();
  final _key = _uuid();
  bool _extending = false;
  bool _submitted = false;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _date.dispose();
    _time.dispose();
    _reason.dispose();
    super.dispose();
  }

  // Ngày dd/MM/yyyy và giờ HH:mm theo giờ Việt Nam; sai định dạng thì null
  String? _isoDateTime() {
    final d = _date.text.trim().split('/');
    final t = _time.text.trim().split(':');
    if (d.length != 3 || t.length != 2) return null;
    final nums = [...d, ...t].map(int.tryParse).toList();
    if (nums.contains(null)) return null;
    String two(int n) => n.toString().padLeft(2, '0');
    return '${nums[2]}-${two(nums[1]!)}-${two(nums[0]!)}T${two(nums[3]!)}:${two(nums[4]!)}:00+07:00';
  }

  Future<void> _extend() async {
    setState(() => _submitted = true);
    final iso = _isoDateTime();
    if (iso == null || _reason.text.trim().isEmpty) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.post(
        '/absences/${widget.absence['id']}/extend-expected-return',
        {'expectedReturnAt': iso, 'reason': _reason.text.trim()},
        headers: {'Idempotency-Key': _key},
      );
      await widget.onChanged();
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final absence = widget.absence;
    final open = absence['status'] == 'OPEN';
    final expected = absence['expectedReturnAt'] as String?;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    _absenceTypes[absence['absenceType']] ?? '${absence['absenceType']}',
                    style: theme.textTheme.titleSmall,
                  ),
                ),
                StatusPill(
                  label: open ? 'Tạm vắng' : 'Đã đóng',
                  tone: open ? StatusTone.attention : StatusTone.neutral,
                ),
              ],
            ),
            Text(
              'Rời viện ${formatDateTime(DateTime.parse(absence['departedAt'] as String))}',
              style: theme.textTheme.bodyMedium,
            ),
            if (open && expected != null)
              Text('Dự kiến trở lại ${formatDateTime(DateTime.parse(expected))}', style: theme.textTheme.bodyMedium),
            if (absence['returnedAt'] != null)
              Text(
                'Đã trở lại ${formatDateTime(DateTime.parse(absence['returnedAt'] as String))}',
                style: theme.textTheme.bodyMedium,
              ),
            if (_error != null) ...[
              const SizedBox(height: 8),
              Semantics(liveRegion: true, child: Text(_error!, style: theme.textTheme.bodySmall)),
            ],
            if (open) ...[
              const SizedBox(height: 12),
              if (_extending) ...[
                TextField(
                  controller: _date,
                  keyboardType: TextInputType.datetime,
                  decoration: InputDecoration(
                    labelText: 'Ngày dự kiến trở lại (dd/MM/yyyy)',
                    errorText: _submitted && _isoDateTime() == null ? 'Nhập ngày và giờ dự kiến trở lại' : null,
                  ),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _time,
                  keyboardType: TextInputType.datetime,
                  decoration: const InputDecoration(labelText: 'Giờ dự kiến trở lại (HH:mm)'),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _reason,
                  maxLength: 2000,
                  minLines: 2,
                  maxLines: 4,
                  decoration: InputDecoration(
                    labelText: 'Lý do gia hạn',
                    errorText: _submitted && _reason.text.trim().isEmpty ? 'Nhập lý do' : null,
                  ),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 12,
                  runSpacing: 8,
                  children: [
                    FilledButton(onPressed: _busy ? null : _extend, child: const Text('Lưu gia hạn')),
                    OutlinedButton(
                      onPressed: () => setState(() {
                        _extending = false;
                        _submitted = false;
                      }),
                      child: const Text('Đóng'),
                    ),
                  ],
                ),
              ] else
                Align(
                  alignment: Alignment.centerLeft,
                  child: OutlinedButton(
                    onPressed: () => setState(() => _extending = true),
                    child: const Text('Gia hạn dự kiến trở lại'),
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

String _uuid() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
