import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';
import 'day_care_attendance_gateway.dart';

/// T031: chỉ trực tuyến; nguồn thật và quyền do adapter cung cấp, không tự ghi điểm danh.
class DayCareAttendanceScreen extends StatefulWidget {
  const DayCareAttendanceScreen({super.key, required this.gateway, required this.date});
  final DayCareAttendanceGateway gateway;
  final String date;
  @override
  State<DayCareAttendanceScreen> createState() => _DayCareAttendanceScreenState();
}

class _DayCareAttendanceScreenState extends State<DayCareAttendanceScreen> {
  static const labels = {
    'NOT_ARRIVED': 'Chưa đến',
    'PRESENT': 'Có mặt',
    'LEFT': 'Đã về',
    'REPORTED_ABSENT': 'Vắng có báo',
    'UNREPORTED_ABSENT': 'Vắng không báo',
    'HOLIDAY_CANCELLED': 'Đã hủy do ngày nghỉ',
  };
  late final TextEditingController _date;
  final _record = TextEditingController(), _correctAt = TextEditingController(), _reason = TextEditingController();
  final _pickups = <int, TextEditingController>{};
  final _keys = <String, String>{};
  DayCareAttendanceData? _data;
  int? _resident;
  bool _loading = true, _busy = false;
  String? _error, _message;
  @override
  void initState() {
    super.initState();
    _date = TextEditingController(text: widget.date.split('-').reversed.join('/'));
    _load(widget.date);
  }

  @override
  void dispose() {
    _date.dispose();
    _record.dispose();
    _correctAt.dispose();
    _reason.dispose();
    for (final c in _pickups.values) {
      c.dispose();
    }
    super.dispose();
  }

  String? _isoDate(String text) {
    final m = RegExp(r'^(\d{2})/(\d{2})/(\d{4})$').firstMatch(text);
    if (m == null) return null;
    final d = int.parse(m[1]!), month = int.parse(m[2]!), y = int.parse(m[3]!);
    final value = DateTime.utc(y, month, d);
    if (value.day != d || value.month != month || value.year != y) return null;
    return '${m[3]}-${m[2]}-${m[1]}';
  }

  Future<void> _load(String date) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await widget.gateway.load(date);
      if (mounted) setState(() => _data = data);
    } catch (_) {
      if (mounted) setState(() => _error = 'Chưa tải được trạng thái. Kiểm tra kết nối rồi thử lại.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String _uuid() {
    final r = Random.secure(), b = List<int>.generate(16, (_) => r.nextInt(256));
    b[6] = (b[6] & 15) | 64;
    b[8] = (b[8] & 63) | 128;
    final h = b.map((v) => v.toRadixString(16).padLeft(2, '0')).join();
    return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
  }

  Future<void> _command(String signature, Future<void> Function(String) send, String message) async {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
      _message = null;
    });
    final key = _keys.putIfAbsent(signature, _uuid);
    try {
      await send(key);
      _keys.remove(signature);
      if (mounted) {
        setState(() => _message = message);
        final date = _isoDate(_date.text);
        if (date != null) await _load(date);
      }
    } on ApiProblem catch (e) {
      if (mounted) setState(() => _error = e.detail ?? 'Chưa đủ điều kiện điểm danh. Kiểm tra thông tin rồi thử lại.');
    } on ApiUnreachable {
      if (mounted) setState(() => _error = 'Điểm danh cần kết nối mạng. Thông tin đang nhập được giữ lại.');
    } catch (_) {
      if (mounted) setState(() => _error = 'Chưa gửi được lệnh. Thông tin đang nhập được giữ lại.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  String _time(Object? value) {
    final parsed = value is String ? DateTime.tryParse(value) : null;
    return parsed == null ? 'Chưa điểm danh' : formatDateTime(parsed);
  }

  Future<void> _correct() async {
    final id = int.tryParse(_record.text),
        m = RegExp(r'^(\d{2})/(\d{2})/(\d{4}) (\d{2}):(\d{2})$').firstMatch(_correctAt.text);
    if (id == null || id <= 0 || m == null || _reason.text.trim().isEmpty) {
      setState(() => _error = 'Nhập mã bản ghi, thời điểm đúng và lý do đính chính.');
      return;
    }
    final date = _isoDate('${m[1]}/${m[2]}/${m[3]}'), hour = int.parse(m[4]!), minute = int.parse(m[5]!);
    if (date == null || hour > 23 || minute > 59) {
      setState(() => _error = 'Thời điểm đúng chưa hợp lệ.');
      return;
    }
    final at = '${date}T${m[4]}:${m[5]}:00+07:00';
    await _command(
      'correct:$id:$at:${_reason.text}',
      (key) => widget.gateway.correct(id, at, _reason.text, key),
      'Đã gửi đính chính giờ theo quyền và quy trình phê duyệt.',
    );
  }

  Widget _field(TextEditingController c, String label, {bool number = false}) => Padding(
    padding: const EdgeInsets.only(bottom: 16),
    child: TextField(
      controller: c,
      enabled: !_busy,
      keyboardType: number ? TextInputType.number : TextInputType.text,
      decoration: InputDecoration(labelText: label),
    ),
  );
  @override
  Widget build(BuildContext context) {
    final data = _data;
    return PageBody(
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Điểm danh bán trú', style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 16),
            _field(_date, 'Ngày xem trạng thái (dd/MM/yyyy)'),
            OutlinedButton(
              onPressed: _busy
                  ? null
                  : () {
                      final date = _isoDate(_date.text);
                      if (date == null) {
                        setState(() => _error = 'Ngày chưa hợp lệ.');
                      } else {
                        _load(date);
                      }
                    },
              child: const Text('Xem trạng thái'),
            ),
            if (_loading) const LinearProgressIndicator(),
            if (_error != null)
              Semantics(
                liveRegion: true,
                child: Padding(padding: const EdgeInsets.symmetric(vertical: 16), child: Text(_error!)),
              ),
            if (_message != null)
              Semantics(
                liveRegion: true,
                child: Padding(padding: const EdgeInsets.symmetric(vertical: 16), child: Text(_message!)),
              ),
            if (data != null && data.canCreate) ...[
              const SizedBox(height: 16),
              DropdownButtonFormField<int>(
                key: const Key('attendance-resident'),
                initialValue: _resident,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Người cao tuổi đến'),
                items: [
                  for (final p in data.residents)
                    DropdownMenuItem(value: p['id'] as int, child: Text(p['fullName'] as String)),
                ],
                onChanged: _busy ? null : (v) => setState(() => _resident = v),
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: _busy || _resident == null
                    ? null
                    : () => _command(
                        'arrive:$_resident',
                        (key) => widget.gateway.checkIn(_resident!, key),
                        'Đã điểm danh đến.',
                      ),
                child: const Text('Điểm danh đến'),
              ),
            ],
            if (data != null) ...[
              if (data.rows.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 16),
                  child: Text('Chưa có trạng thái có mặt trong ngày đã chọn.'),
                ),
              for (final row in data.rows)
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(
                          data.residents
                                  .where((p) => p['id'] == row['residentId'])
                                  .map((p) => p['fullName'] as String)
                                  .firstOrNull ??
                              'Người cao tuổi · mã ${row['residentId']}',
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const SizedBox(height: 8),
                        Text(labels[row['status']] ?? 'Chưa xác định'),
                        if (row['adHocSession'] == true) const Text('Buổi phát sinh'),
                        if (row['lateAbsenceReported'] == true) const Text('Đã lưu kèm báo vắng muộn.'),
                        Text('Đến: ${_time(row['arrivedAt'])}'),
                        Text('Về: ${_time(row['leftAt'])}'),
                        Text('Dự kiến về: ${row['currentExpectedReturn'] ?? 'Chưa có'}'),
                        if (data.canCreate && row['status'] == 'PRESENT') ...[
                          const SizedBox(height: 16),
                          _field(
                            _pickups.putIfAbsent(row['id'] as int, TextEditingController.new),
                            'Bản ghi đón của dòng ${row['id']}',
                            number: true,
                          ),
                          FilledButton(
                            onPressed: _busy
                                ? null
                                : () {
                                    final id = row['id'] as int, pickup = int.tryParse(_pickups[id]!.text);
                                    if (pickup == null || pickup <= 0) {
                                      setState(() => _error = 'Nhập mã bản ghi đón hợp lệ.');
                                      return;
                                    }
                                    _command(
                                      'leave:$id:$pickup',
                                      (key) => widget.gateway.checkOut(id, pickup, key),
                                      'Đã điểm danh về và xử lý công việc chưa đến hạn.',
                                    );
                                  },
                            child: Text('Điểm danh về dòng ${row['id']}'),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              if (data.canCreate)
                ExpansionTile(
                  title: const Text('Đính chính giờ điểm danh'),
                  children: [
                    _field(_record, 'Mã bản ghi cần đính chính', number: true),
                    _field(_correctAt, 'Thời điểm đúng (dd/MM/yyyy HH:mm)'),
                    _field(_reason, 'Lý do đính chính'),
                    FilledButton(onPressed: _busy ? null : _correct, child: const Text('Gửi đính chính giờ')),
                  ],
                ),
            ],
          ],
        ),
      ),
    );
  }
}
