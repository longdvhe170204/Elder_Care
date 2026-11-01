import 'dart:convert';
import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';
import 'care_task_planning_gateway.dart';

/// T026: màn trực tuyến; nguồn chỉ để đối chiếu, không tự điền hoặc xếp công việc.
class ShiftTaskPlanningScreen extends StatefulWidget {
  const ShiftTaskPlanningScreen({super.key, required this.gateway, this.shiftId});
  final CareTaskPlanningGateway gateway;
  final int? shiftId;
  @override
  State<ShiftTaskPlanningScreen> createState() => _ShiftTaskPlanningScreenState();
}

class _PlanningRow {
  _PlanningRow(this.key);
  final int key;
  final values = <String, String>{};
  bool shared = false;
}

class _ShiftTaskPlanningScreenState extends State<ShiftTaskPlanningScreen> {
  final _form = GlobalKey<FormState>();
  final _rows = <_PlanningRow>[];
  int _sequence = 0;
  int? _shiftId;
  List<Map<String, dynamic>> _shifts = [];
  CareTaskPlanningData? _data;
  bool _loading = true, _busy = false;
  String? _error, _message, _signature, _requestKey;
  List<Map<String, dynamic>> _rowErrors = [];
  static const _basis = {
    'CARE_PLAN_ITEM': 'Mục kế hoạch',
    'MEASUREMENT': 'Lần đo',
    'MEAL': 'Bữa ăn',
    'ACTIVITY_SESSION': 'Buổi hoạt động',
    'POST_FALL_MONITORING': 'Theo dõi sau ngã',
    'CONTACT_MONITORING': 'Theo dõi tiếp xúc',
    'WATER_INTAKE_ALERT': 'Cảnh báo uống nước',
    'SHIFT_START_CHECK': 'Kiểm tra đầu ca',
    'SUPERVISION': 'Giám sát',
    'AD_HOC': 'Phát sinh',
  };
  static const _importance = {'NORMAL': 'Thường', 'IMPORTANT': 'Quan trọng', 'MANDATORY': 'Bắt buộc'};
  static const _errors = {
    'DUPLICATE_BASIS': 'Trùng căn cứ và thời điểm dự kiến.',
    'INVALID_BASIS': 'Căn cứ chưa hợp lệ hoặc nguồn chưa xác nhận.',
    'RESIDENT_ABSENT': 'Người cao tuổi đang vắng mặt.',
    'RESIDENT_NOT_STAYING': 'Người cao tuổi không đang lưu trú.',
    'DAY_CARE_NOT_PRESENT': 'Người bán trú chưa có mặt trong khoảng này.',
    'OUTSIDE_SHIFT': 'Thời điểm dự kiến nằm ngoài ca.',
    'PLAN_ITEM_MISMATCH': 'Thời điểm, khung hoặc mức chưa khớp mục kế hoạch.',
    'INVALID_PERFORMER': 'Người thực hiện hoặc lựa chọn công việc chung chưa hợp lệ.',
    'MISSING_CERTIFICATE': 'Người thực hiện chưa đủ chứng chỉ.',
    'OLD_REVISION_WINDOW_OVERLAP': 'Trùng khung công việc của phiên bản trước.',
    'SHIFT_CHECK_ALREADY_EXISTS': 'Điều dưỡng đã có công việc này trong ca.',
    'MEDICATION_IS_NOT_A_TASK': 'Liều thuốc không được lập thành công việc.',
  };

  @override
  void initState() {
    super.initState();
    _shiftId = widget.shiftId;
    _initialize();
  }

  Future<void> _initialize() async {
    if (_shiftId != null) {
      await _load(_shiftId!);
      return;
    }
    try {
      final shifts = await widget.gateway.shifts();
      if (mounted) setState(() => _shifts = shifts);
    } catch (_) {
      if (mounted) setState(() => _error = 'Chưa tải được ca. Kiểm tra kết nối rồi thử lại.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _load(int id) async {
    setState(() {
      _shiftId = id;
      _loading = true;
      _error = null;
      _data = null;
    });
    try {
      final data = await widget.gateway.load(id);
      if (mounted) setState(() => _data = data);
    } on CareTaskPlanningError catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'Chưa tải được dữ liệu ca. Kiểm tra kết nối rồi thử lại.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  List<Map<String, dynamic>> _maps(Object? value) => (value as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
  Map<String, String> _options(List<Map<String, dynamic>> values, String id, String label) => {
    for (final row in values) row[id].toString(): row[label]?.toString() ?? 'Chưa có tên',
  };
  String? _dateIso(String? text) {
    final m = RegExp(r'^(\d{2})/(\d{2})/(\d{4}) (\d{2}):(\d{2})$').firstMatch(text ?? '');
    if (m == null) return null;
    final day = int.parse(m[1]!),
        month = int.parse(m[2]!),
        year = int.parse(m[3]!),
        hour = int.parse(m[4]!),
        minute = int.parse(m[5]!);
    final date = DateTime.utc(year, month, day, hour, minute);
    if (date.day != day || date.month != month || date.year != year || date.hour != hour || date.minute != minute) {
      return null;
    }
    return '${m[3]}-${m[2]}-${m[1]}T${m[4]}:${m[5]}:00+07:00';
  }

  String _uuid() {
    final random = Random.secure();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    final h = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
    return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
  }

  Map<String, dynamic> _payload(_PlanningRow row) {
    final v = row.values;
    final location = v['locationType'];
    return {
      if (location == null || location.isEmpty) 'residentId': int.parse(v['residentId']!),
      if (location != null && location.isNotEmpty)
        'location': {'type': location, 'id': (_data!.source['shift'] as Map<String, dynamic>)['floorAreaId']},
      'taskTypeId': int.parse(v['taskTypeId']!),
      'basis': {'type': v['basisType'], if (v['basisId']?.isNotEmpty ?? false) 'id': int.parse(v['basisId']!)},
      if (v['content']?.isNotEmpty ?? false) 'content': v['content'],
      'plannedAt': _dateIso(v['plannedAt']),
      'importance': v['importance'],
      if (v['before']?.isNotEmpty ?? false) 'windowBeforeMinutes': int.parse(v['before']!),
      if (v['after']?.isNotEmpty ?? false) 'windowAfterMinutes': int.parse(v['after']!),
      if (!row.shared) 'performerId': int.parse(v['performerId']!),
      'shared': row.shared,
    };
  }

  Future<void> _submit() async {
    if (_busy || _rows.isEmpty || !(_form.currentState?.validate() ?? false)) return;
    final body = _rows.map(_payload).toList();
    final signature = jsonEncode(body);
    if (_signature != signature) {
      _signature = signature;
      _requestKey = _uuid();
    }
    setState(() {
      _busy = true;
      _error = null;
      _rowErrors = [];
      _message = null;
    });
    try {
      await widget.gateway.plan(_shiftId!, body, _requestKey!);
      if (!mounted) return;
      setState(() {
        _rows.clear();
        _message = 'Đã lập công việc của ca.';
        _signature = null;
        _requestKey = null;
      });
      await _load(_shiftId!);
    } on CareTaskPlanningError catch (e) {
      if (mounted) {
        setState(() {
          _error = e.message;
          _rowErrors = e.rowErrors;
        });
      }
    } on ApiUnreachable {
      if (mounted) setState(() => _error = 'Lập công việc cần kết nối mạng. Các dòng đang nhập được giữ lại.');
    } catch (_) {
      if (mounted) setState(() => _error = 'Chưa gửi được lô công việc. Các dòng đang nhập được giữ lại.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _choice(
    _PlanningRow r,
    String name,
    String label,
    Map<String, String> options, {
    bool required = true,
    bool disabled = false,
  }) {
    final value = r.values[name];
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: DropdownButtonFormField<String>(
        key: ValueKey('$name-${r.key}-${value ?? ''}'),
        initialValue: options.containsKey(value) ? value : null,
        isExpanded: true,
        decoration: InputDecoration(labelText: label),
        items: [
          for (final e in options.entries)
            DropdownMenuItem(
              value: e.key,
              child: Text(e.value, overflow: TextOverflow.ellipsis),
            ),
        ],
        onChanged: _busy || disabled
            ? null
            : (v) => setState(() {
                r.values[name] = v ?? '';
                if (name == 'residentId' || name == 'basisType') {
                  r.values.remove('basisId');
                  r.values.remove('performerId');
                }
                _rowErrors = [];
              }),
        validator: required ? (v) => v == null || v.isEmpty ? 'Chọn giá trị cho dòng này.' : null : null,
      ),
    );
  }

  Widget _input(_PlanningRow r, int index, String name, String label, {bool numeric = false, bool date = false}) =>
      Padding(
        padding: const EdgeInsets.only(bottom: 16),
        child: TextFormField(
          key: ValueKey('$name-$index'),
          initialValue: r.values[name] ?? '',
          enabled: !_busy,
          maxLength: name == 'content' ? 1000 : null,
          decoration: InputDecoration(labelText: label),
          keyboardType: numeric ? TextInputType.number : TextInputType.text,
          onChanged: (v) => r.values[name] = v,
          validator: (v) {
            if (date) return _dateIso(v) == null ? 'Nhập ngày giờ theo dd/MM/yyyy HH:mm.' : null;
            if (numeric &&
                v != null &&
                v.isNotEmpty &&
                (int.tryParse(v) == null || int.parse(v) < 0 || int.parse(v) > 32767)) {
              return 'Nhập số phút từ 0 đến 32767.';
            }
            return null;
          },
        ),
      );
  Widget _row(_PlanningRow r, int index, CareTaskPlanningData data) {
    final sources = _maps(data.source['residents']);
    final resident = sources.where((s) => s['residentId'].toString() == r.values['residentId']).firstOrNull;
    final atLocation = r.values['locationType']?.isNotEmpty ?? false;
    final assigned = atLocation
        ? data.shiftMembers.where((m) => m['absent'] != true && m['approvedLeave'] != true).toList()
        : _maps(resident?['assignments']);
    final options = r.values['basisType'] == 'CARE_PLAN_ITEM'
        ? _options(_maps(resident?['planItems']), 'id', 'content')
        : _options(
            _maps(resident?['otherSources']).where((s) => s['type'] == r.values['basisType']).toList(),
            'id',
            'description',
          );
    final noBasisId = {'SHIFT_START_CHECK', 'SUPERVISION', 'AD_HOC'}.contains(r.values['basisType']);
    final errors = _rowErrors
        .where((e) => e['row'] == index)
        .expand((e) => (e['codes'] as List<dynamic>? ?? []))
        .toList();
    return Column(
      key: ValueKey(r.key),
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Divider(),
        Text('Dòng $index', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 16),
        for (final code in errors)
          Semantics(liveRegion: true, child: Text(_errors[code] ?? 'Dòng chưa đủ điều kiện, kiểm tra lại dữ liệu.')),
        _choice(
          r,
          'residentId',
          'Người cao tuổi dòng $index',
          _options(data.residents, 'id', 'fullName'),
          required: !atLocation,
          disabled: atLocation,
        ),
        _choice(r, 'locationType', 'Vị trí chung dòng $index', {
          'FLOOR': 'Tầng của ca',
          'AREA': 'Khu vực của ca',
        }, required: false),
        _choice(r, 'taskTypeId', 'Loại công việc dòng $index', _options(data.taskTypes, 'id', 'name')),
        _choice(r, 'basisType', 'Loại căn cứ dòng $index', _basis),
        if (!noBasisId) _choice(r, 'basisId', 'Căn cứ dòng $index', options),
        _input(r, index, 'plannedAt', 'Thời điểm dự kiến (dd/MM/yyyy HH:mm)', date: true),
        _input(r, index, 'before', 'Khung trước (phút) dòng $index', numeric: true),
        _input(r, index, 'after', 'Khung sau (phút) dòng $index', numeric: true),
        _choice(r, 'importance', 'Mức quan trọng dòng $index', _importance),
        _choice(
          r,
          'performerId',
          'Người thực hiện dòng $index',
          {
            for (final a in assigned)
              a['staffId'].toString(): a['fullName']?.toString() ?? 'Nhân viên · mã ${a['staffId']}',
          },
          required: !r.shared,
          disabled: r.shared,
        ),
        CheckboxListTile(
          title: Text('Công việc chung dòng $index'),
          value: r.shared,
          onChanged: _busy ? null : (v) => setState(() => r.shared = v ?? false),
          controlAffinity: ListTileControlAffinity.leading,
        ),
        _input(r, index, 'content', 'Nội dung dòng $index'),
        TextButton(
          onPressed: _busy
              ? null
              : () => setState(() {
                  _rows.remove(r);
                  _rowErrors = [];
                }),
          child: Text('Bỏ dòng $index'),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final data = _data;
    return PageBody(
      child: SingleChildScrollView(
        child: Form(
          key: _form,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Lập công việc của ca', style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 16),
              if (widget.shiftId == null)
                DropdownButtonFormField<int>(
                  initialValue: _shiftId,
                  decoration: const InputDecoration(labelText: 'Ca đã công bố'),
                  isExpanded: true,
                  items: [
                    for (final s in _shifts)
                      DropdownMenuItem(
                        value: s['id'] as int,
                        child: Text(
                          '${formatDateTime(DateTime.parse(s['startsAt'] as String))} – ${formatDateTime(DateTime.parse(s['endsAt'] as String))}',
                        ),
                      ),
                  ],
                  onChanged: _busy || _rows.isNotEmpty
                      ? null
                      : (id) {
                          if (id != null) _load(id);
                        },
                ),
              if (_loading) const LinearProgressIndicator(),
              if (_error != null) Semantics(liveRegion: true, child: Text(_error!)),
              if (_error != null && !_loading && data == null)
                OutlinedButton(
                  onPressed: _shiftId == null ? _initialize : () => _load(_shiftId!),
                  child: const Text('Thử tải lại'),
                ),
              if (data != null) ...[
                Text('Dữ liệu nguồn để đối chiếu', style: Theme.of(context).textTheme.titleMedium),
                const Text('Dữ liệu này không được tự chuyển thành công việc.'),
                for (final resident in _maps(data.source['residents'])) ...[
                  Text(
                    data.residents
                            .where((r) => r['id'] == resident['residentId'])
                            .firstOrNull?['fullName']
                            ?.toString() ??
                        'Hồ sơ chưa tải tên',
                  ),
                  for (final item in _maps(resident['planItems'])) Text(item['content']?.toString() ?? ''),
                  for (final item in _maps(resident['otherSources'])) Text(item['description']?.toString() ?? ''),
                ],
                const SizedBox(height: 24),
                Text('Công việc đã lập (${data.tasks.length})', style: Theme.of(context).textTheme.titleMedium),
                if (data.tasks.isEmpty) const Text('Ca chưa có công việc.'),
                for (final task in data.tasks)
                  ListTile(
                    title: Text(task['content']?.toString() ?? task['taskType']?.toString() ?? ''),
                    subtitle: Text(formatDateTime(DateTime.parse(task['plannedAt'] as String))),
                  ),
                if (data.canCreate) ...[
                  const SizedBox(height: 24),
                  Text('Các dòng đang nhập', style: Theme.of(context).textTheme.titleMedium),
                  if (_rows.isEmpty) const Text('Chưa có dòng công việc.'),
                  for (int i = 0; i < _rows.length; i++) _row(_rows[i], i + 1, data),
                  OutlinedButton(
                    onPressed: _busy ? null : () => setState(() => _rows.add(_PlanningRow(++_sequence))),
                    child: const Text('Thêm dòng công việc'),
                  ),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: _busy || _rows.isEmpty ? null : _submit,
                    child: Text(_busy ? 'Đang kiểm tra và lưu…' : 'Lập công việc của ca'),
                  ),
                  const SizedBox(height: 16),
                  const Text('Một dòng lỗi thì cả lô không được lưu. Lập công việc cần kết nối mạng.'),
                ],
              ],
              if (_message != null) Semantics(liveRegion: true, child: Text(_message!)),
            ],
          ),
        ),
      ),
    );
  }
}
