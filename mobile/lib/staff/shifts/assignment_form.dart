import 'package:flutter/material.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import 'assignment_labels.dart';
import 'assignment_moment.dart';
import 'assignments_controller.dart';

Future<String?> pickAssignmentTime(BuildContext context, String value) async {
  final vn = DateTime.parse(value).toUtc().add(const Duration(hours: 7));
  return assignmentMoment(context, vn, withTime: true);
}

/// Form trực tuyến, mọi lần đổi dữ liệu đều hủy xem trước cũ (FR-031).
class AssignmentForm extends StatefulWidget {
  const AssignmentForm({super.key, required this.controller, this.original});
  final AssignmentsController controller;
  final Map<String, dynamic>? original;
  @override
  State<AssignmentForm> createState() => _AssignmentFormState();
}

class _AssignmentFormState extends State<AssignmentForm> {
  final _form = GlobalKey<FormState>();
  Map<String, dynamic>? original, preview;
  late Map<String, dynamic> input;
  List<Map<String, dynamic>> staff = [], targets = [], types = [];
  bool loading = true, busy = false, confirmed = false;
  String? error, typesError;
  int generation = 0;
  AssignmentsController get c => widget.controller;
  @override
  void initState() {
    super.initState();
    original = widget.original;
    input = _values(original);
    _load();
  }

  Map<String, dynamic> _values(Map<String, dynamic>? value) => {
    'staffId': value?['staffId'],
    'targetType': value?['targetType'] ?? 'FLOOR',
    'targetId': value?['targetId'],
    'role': value?['role'] ?? 'CAREGIVER',
    'assignmentKind': value?['assignmentKind'] ?? 'PRIMARY',
    'assignedNurse': value?['assignedNurse'] ?? false,
    if (value?['taskTypeId'] != null) 'taskTypeId': value!['taskTypeId'],
    'startsAt': assignmentIso((value?['startsAt'] ?? c.shift!['startsAt']) as String),
    'endsAt': assignmentIso((value?['endsAt'] ?? c.shift!['endsAt']) as String),
  };

  Future<void> _load() async {
    setState(() => loading = true);
    try {
      staff = await c.catalog('/staff');
      final members = (c.shift!['staff'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
      staff = staff.where((s) => members.any((m) => m['staffId'] == s['id'])).toList();
      // Danh mục dùng đúng HTTP đã công bố của 005; lỗi nguồn không được coi là danh mục rỗng.
      try {
        types = (await c.catalog(
          '/task-types',
        )).where((t) => t['status'] == 'ACTIVE' || t['id'] == original?['taskTypeId']).toList();
        typesError = null;
      } on Object catch (e) {
        typesError = backendMessage(e);
      }
      await _targets();
    } on Object catch (e) {
      error = backendMessage(e);
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _targets({String query = ''}) async {
    final request = ++generation;
    final type = input['targetType'] as String;
    final rows = type == 'RESIDENT'
        ? await c.residents(query: query)
        : await c.catalog({'FLOOR': '/floors', 'AREA': '/areas', 'ROOM': '/rooms'}[type]!);
    if (mounted && request == generation) setState(() => targets = rows);
  }

  void change(String key, Object? value) {
    setState(() {
      if (value == null) {
        input.remove(key);
      } else {
        input[key] = value;
      }
      preview = null;
      confirmed = false;
      error = null;
    });
  }

  Future<void> check() async {
    if (!_form.currentState!.validate()) return;
    setState(() {
      busy = true;
      preview = null;
      confirmed = false;
      error = null;
    });
    try {
      final value = await c.preview(Map.of(input), original: original);
      if (mounted) setState(() => preview = value);
    } on Object catch (e) {
      if (mounted) setState(() => error = backendMessage(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> save() async {
    setState(() => busy = true);
    final ok = await c.save(Map.of(input), original: original);
    if (!mounted) return;
    if (ok) {
      Navigator.pop(context, true);
    } else {
      setState(() {
        busy = false;
        error = c.error ?? 'Không còn được phép thực hiện. Tải lại ca để kiểm tra.';
      });
    }
  }

  Future<void> time(String field) async {
    final value = await pickAssignmentTime(context, input[field] as String);
    if (value != null && mounted) change(field, value);
  }

  Widget choice(String field, String label, List<DropdownMenuItem<Object>> items) {
    final selected = items.any((i) => i.value == input[field]) ? input[field] : null;
    return DropdownButtonFormField<Object>(
      key: ValueKey('$field:$selected'),
      initialValue: selected,
      isExpanded: true,
      isDense: false,
      itemHeight: null,
      decoration: InputDecoration(labelText: label),
      items: items,
      onChanged: busy
          ? null
          : (v) async {
              change(field, v);
              if (field == 'targetType') {
                change('targetId', null);
                setState(() => targets = []);
                try {
                  await _targets();
                } on Object catch (e) {
                  if (mounted) setState(() => error = backendMessage(e));
                }
              }
            },
      validator: (v) => v == null ? 'Hãy chọn $label' : null,
    );
  }

  @override
  Widget build(BuildContext context) {
    final warnings = (preview?['warnings'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
    return Scaffold(
      appBar: AppBar(title: Text(original == null ? 'Lập phân công' : 'Sửa phân công Nháp')),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _form,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (error != null) ...[
                    Text(error!, semanticsLabel: 'Lỗi: $error'),
                    TextButton(
                      onPressed: busy
                          ? null
                          : () async {
                              if (original != null) {
                                try {
                                  final latest = await c.draft(original!['id'] as int);
                                  if (latest == null) throw const FormatException('Không có chi tiết phân công');
                                  if (mounted) {
                                    setState(() {
                                      original = latest;
                                      input = _values(latest);
                                      preview = null;
                                      confirmed = false;
                                      error = null;
                                      targets = [];
                                    });
                                    await _targets();
                                  }
                                } on Object catch (e) {
                                  if (mounted) setState(() => error = backendMessage(e));
                                }
                              } else {
                                await _load();
                              }
                            },
                      child: const Text('Đọc lại dữ liệu hiện hành'),
                    ),
                  ],
                  choice('staffId', 'Nhân viên', [
                    for (final s in staff)
                      DropdownMenuItem(value: s['id'] as Object, child: Text(s['fullName'] as String)),
                  ]),
                  const SizedBox(height: 16),
                  choice('targetType', 'Loại đối tượng', [
                    for (final t in assignmentTargets.entries) DropdownMenuItem(value: t.key, child: Text(t.value)),
                  ]),
                  const SizedBox(height: 16),
                  if (input['targetType'] == 'RESIDENT')
                    TextFormField(
                      decoration: const InputDecoration(labelText: 'Tìm người cao tuổi'),
                      onFieldSubmitted: (q) async {
                        try {
                          await _targets(query: q);
                        } on Object catch (e) {
                          if (mounted) setState(() => error = backendMessage(e));
                        }
                      },
                    ),
                  choice('targetId', 'Đối tượng', [
                    for (final t in targets)
                      DropdownMenuItem(
                        value: t['id'] as Object,
                        child: Text((t['fullName'] ?? t['name'] ?? t['code']).toString()),
                      ),
                  ]),
                  const SizedBox(height: 16),
                  choice('role', 'Vai trò', [
                    for (final r in assignmentRoles.entries) DropdownMenuItem(value: r.key, child: Text(r.value)),
                  ]),
                  const SizedBox(height: 16),
                  choice('assignmentKind', 'Phân loại', const [
                    DropdownMenuItem(value: 'PRIMARY', child: Text('Chính')),
                    DropdownMenuItem(value: 'SUPPORT', child: Text('Hỗ trợ')),
                  ]),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<int>(
                    initialValue: input['taskTypeId'] as int?,
                    isExpanded: true,
                    isDense: false,
                    itemHeight: null,
                    decoration: const InputDecoration(labelText: 'Loại công việc'),
                    items: [
                      const DropdownMenuItem<int>(value: null, child: Text('Không giới hạn')),
                      if (input['taskTypeId'] != null && !types.any((t) => t['id'] == input['taskTypeId']))
                        DropdownMenuItem(
                          value: input['taskTypeId'] as int,
                          child: const Text('Loại đang áp dụng (chưa tải được tên)'),
                        ),
                      for (final t in types) DropdownMenuItem(value: t['id'] as int, child: Text(t['name'] as String)),
                    ],
                    onChanged: busy ? null : (v) => change('taskTypeId', v),
                  ),
                  if (typesError != null) Text('Chưa tải được loại công việc: $typesError'),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Điều dưỡng phụ trách'),
                    value: input['assignedNurse'] as bool,
                    onChanged: busy ? null : (v) => change('assignedNurse', v),
                  ),
                  for (final field in ['startsAt', 'endsAt'])
                    OutlinedButton(
                      onPressed: busy ? null : () => time(field),
                      child: Text(
                        '${field == 'startsAt' ? 'Từ' : 'Đến'} ${formatDateTime(DateTime.parse(input[field] as String))}',
                      ),
                    ),
                  OutlinedButton(onPressed: busy ? null : check, child: const Text('Xem trước')),
                  for (final warning in warnings)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: Text(
                        (warning['description'] ?? warning['message'] ?? 'Cần kiểm tra phân công').toString(),
                      ),
                    ),
                  if (warnings.isNotEmpty)
                    CheckboxListTile(
                      title: const Text('Tôi đã đọc cảnh báo'),
                      value: confirmed,
                      onChanged: busy ? null : (v) => setState(() => confirmed = v ?? false),
                    ),
                  FilledButton(
                    onPressed: !busy && preview != null && (warnings.isEmpty || confirmed) ? save : null,
                    child: Text(busy ? 'Đang xử lý…' : 'Lưu phân công'),
                  ),
                ],
              ),
            ),
    );
  }
}
