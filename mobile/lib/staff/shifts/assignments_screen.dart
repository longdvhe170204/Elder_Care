import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';
import '../../common/widgets/status_pill.dart';
import 'assignment_form.dart';
import 'assignment_labels.dart';
import 'assignments_controller.dart';
import 'floor_manager_sheet.dart';

class AssignmentsScreen extends StatefulWidget {
  const AssignmentsScreen({super.key, required this.api, this.initialMonth});
  final ApiClient api;
  final DateTime? initialMonth;
  @override
  State<AssignmentsScreen> createState() => _AssignmentsScreenState();
}

class _AssignmentsScreenState extends State<AssignmentsScreen> {
  late final AssignmentsController c;
  int? schedule, selected;
  List<Map<String, dynamic>> names = [];
  Map<String, String> targets = {};
  int selectionRequest = 0;
  final Map<String, String> reasons = {};
  @override
  void initState() {
    super.initState();
    c = AssignmentsController(widget.api, initialMonth: widget.initialMonth);
    c.load();
  }

  @override
  void dispose() {
    c.dispose();
    super.dispose();
  }

  Future<void> select(int id) async {
    final request = ++selectionRequest;
    setState(() {
      selected = id;
      names = [];
      targets = {};
    });
    await c.selectShift(id);
    if (!mounted || request != selectionRequest || c.shift?['id'] != id) return;
    try {
      final list = await c.catalog('/staff');
      if (mounted && request == selectionRequest) setState(() => names = list);
    } on Object {
      // Không suy tên khi người dùng không được đọc nguồn nhân sự.
    }
    final labels = <String, String>{};
    for (final type in c.rows.map((row) => row['targetType'] as String).toSet()) {
      try {
        if (type == 'RESIDENT') {
          for (final row in c.rows.where((row) => row['targetType'] == type)) {
            final person = await c.api.get('/residents/${row['targetId']}');
            final name = person?['fullName'];
            if (name is String) labels['$type:${row['targetId']}'] = name;
          }
        } else {
          final path = {'FLOOR': '/floors', 'AREA': '/areas', 'ROOM': '/rooms'}[type];
          if (path == null) continue;
          for (final target in await c.catalog(path)) {
            final name = target['name'] ?? target['code'];
            if (name is String) labels['$type:${target['id']}'] = name;
          }
        }
      } on Object {
        // Thiếu tên chỉ giới hạn phần hiển thị, không đổi quyền hoặc kết quả phân công.
      }
    }
    if (mounted && request == selectionRequest) setState(() => targets = labels);
  }

  Future<void> edit([Map<String, dynamic>? row]) async {
    try {
      final detail = row == null ? null : await c.draft(row['id'] as int);
      if (!mounted) return;
      final saved = await Navigator.of(context).push<bool>(
        MaterialPageRoute(
          builder: (_) => AssignmentForm(controller: c, original: detail),
        ),
      );
      if (saved == true && selected != null) await select(selected!);
    } on Object catch (e) {
      if (mounted) await message('Chưa mở được phân công', backendMessage(e));
    }
  }

  Future<void> message(String title, String text) => showDialog<void>(
    context: context,
    builder: (context) => AlertDialog(
      title: Text(title),
      content: SingleChildScrollView(child: Text(text)),
      actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Đóng'))],
    ),
  );
  Future<void> finish(Map<String, dynamic> row, String kind) async {
    final draftKey = '${row['id']}:$kind';
    var reason = reasons[draftKey] ?? '';
    final label = {'delete': 'Xóa Nháp', 'cancel': 'Hủy phân công', 'end': 'Kết thúc phân công'}[kind]!;
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, update) => AlertDialog(
          title: Text(label),
          content: SingleChildScrollView(
            child: kind == 'delete'
                ? const Text('Xóa phân công Nháp này?')
                : TextFormField(
                    initialValue: reason,
                    maxLines: 3,
                    decoration: const InputDecoration(labelText: 'Lý do bắt buộc'),
                    onChanged: (value) => update(() {
                      reason = value;
                      reasons[draftKey] = value;
                    }),
                  ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Quay lại')),
            FilledButton(
              onPressed: kind == 'delete' || reason.trim().isNotEmpty ? () => Navigator.pop(context, true) : null,
              child: const Text('Xác nhận'),
            ),
          ],
        ),
      ),
    );
    if (ok == true && await c.finish(row, kind, reason: reason)) {
      reasons.remove(draftKey);
      if (selected != null) await select(selected!);
    }
  }

  Future<void> copy() async {
    int? source;
    final id = await showDialog<int>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, update) => AlertDialog(
          title: const Text('Sao chép phân công'),
          content: DropdownButtonFormField<int>(
            isExpanded: true,
            isDense: false,
            itemHeight: null,
            decoration: const InputDecoration(labelText: 'Ca nguồn'),
            items: [
              for (final s in c.shifts.where((s) => s['id'] != selected))
                DropdownMenuItem(
                  value: s['id'] as int,
                  child: Text(formatDateTime(DateTime.parse(s['startsAt'] as String))),
                ),
            ],
            onChanged: (v) => update(() => source = v),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: const Text('Quay lại')),
            FilledButton(
              onPressed: source == null ? null : () => Navigator.pop(context, source),
              child: const Text('Sao chép'),
            ),
          ],
        ),
      ),
    );
    if (id == null) return;
    final result = await c.copy(id);
    if (result != null && mounted) {
      final skipped = (result['notCopied'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
      await message(
        'Kết quả sao chép',
        'Đã sao chép ${(result['copiedIds'] as List<dynamic>? ?? []).length} phân công.${skipped.isEmpty ? '' : '\n${skipped.map((s) => 'Phân công mã ${s['sourceAssignmentId']}: ${s['reason']}').join('\n')}'}',
      );
      if (selected != null) await select(selected!);
    }
  }

  Future<void> unassigned() async {
    try {
      final data = await c.unassigned();
      if (!mounted) return;
      final items = (data?['items'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
      await message(
        'Người còn thiếu người phụ trách',
        items.isEmpty
            ? 'Đã đủ người phụ trách tại giờ bắt đầu ca.'
            : 'Tại ${formatDateTime(DateTime.parse(data!['evaluatedAt'] as String))}\n${items.map((i) => 'Người cao tuổi mã ${i['residentId']}: ${[if (i['missingAssignedNurse'] == true) 'thiếu Điều dưỡng phụ trách', if (i['missingPrimaryCaregiver'] == true) 'thiếu nhân viên chăm sóc chính'].join(', ')}').join('\n')}',
      );
    } on Object catch (e) {
      if (mounted) await message('Chưa đọc được danh sách', backendMessage(e));
    }
  }

  Future<void> resolve() async {
    try {
      final residents = await c.residents();
      if (!mounted) return;
      int? resident;
      String role = 'CAREGIVER';
      String at = c.shift!['startsAt'] as String;
      final query = await showDialog<(int, String, String)>(
        context: context,
        builder: (context) => StatefulBuilder(
          builder: (context, update) => AlertDialog(
            title: const Text('Tra người phụ trách'),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  DropdownButtonFormField<int>(
                    isExpanded: true,
                    isDense: false,
                    itemHeight: null,
                    decoration: const InputDecoration(labelText: 'Người cao tuổi'),
                    items: [
                      for (final r in residents)
                        DropdownMenuItem(value: r['id'] as int, child: Text((r['fullName'] ?? r['code']).toString())),
                    ],
                    onChanged: (v) => update(() => resident = v),
                  ),
                  DropdownButtonFormField<String>(
                    initialValue: role,
                    isExpanded: true,
                    isDense: false,
                    itemHeight: null,
                    items: [
                      for (final r in assignmentRoles.entries) DropdownMenuItem(value: r.key, child: Text(r.value)),
                    ],
                    onChanged: (v) => update(() => role = v!),
                  ),
                  OutlinedButton(
                    onPressed: () async {
                      final value = await pickAssignmentTime(context, at);
                      if (value != null) update(() => at = value);
                    },
                    child: Text(formatDateTime(DateTime.parse(at))),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(context), child: const Text('Quay lại')),
              FilledButton(
                onPressed: resident == null ? null : () => Navigator.pop(context, (resident!, at, role)),
                child: const Text('Tra cứu'),
              ),
            ],
          ),
        ),
      );
      if (query == null) return;
      final data = await c.resolve(query.$1, query.$2, query.$3);
      if (!mounted || data == null) return;
      await message(
        'Người phụ trách tại mốc đã chọn',
        'Người chính: ${data['primaryStaffId'] == null ? 'Chưa có' : assignmentName(names, data['primaryStaffId'])}\nĐiều dưỡng phụ trách: ${(data['assignedNurseStaffIds'] as List<dynamic>).map((id) => assignmentName(names, id)).join(', ')}',
      );
    } on Object catch (e) {
      if (mounted) await message('Chưa tra được người phụ trách', backendMessage(e));
    }
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: c,
    builder: (context, _) {
      final shifts = schedule == null ? c.shifts : AssignmentsControllerShifts.inSchedule(c.schedules, schedule!);
      return PageBody(
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            Wrap(
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              spacing: 8,
              children: [
                IconButton(
                  tooltip: 'Tháng trước',
                  onPressed: c.loading
                      ? null
                      : () {
                          setState(() {
                            schedule = null;
                            selected = null;
                          });
                          c.load(move: -1);
                        },
                  icon: const Icon(LucideIcons.chevronLeft),
                ),
                Text(c.monthLabel, style: Theme.of(context).textTheme.titleLarge),
                IconButton(
                  tooltip: 'Tháng sau',
                  onPressed: c.loading
                      ? null
                      : () {
                          setState(() {
                            schedule = null;
                            selected = null;
                          });
                          c.load(move: 1);
                        },
                  icon: const Icon(LucideIcons.chevronRight),
                ),
                TextButton.icon(
                  onPressed: c.loading ? null : () => selected == null ? c.load() : select(selected!),
                  icon: const Icon(LucideIcons.refreshCw),
                  label: const Text('Làm mới'),
                ),
              ],
            ),
            if (c.error != null) ...[
              Text(c.error!, semanticsLabel: 'Lỗi: ${c.error}'),
              TextButton(onPressed: c.loading ? null : () => c.load(), child: const Text('Thử lại')),
            ],
            if (c.loading) const Center(child: CircularProgressIndicator()),
            if (c.view && !c.loading) ...[
              if (c.manage && c.schedules.isNotEmpty)
                DropdownButtonFormField<int>(
                  initialValue: schedule,
                  isExpanded: true,
                  isDense: false,
                  itemHeight: null,
                  decoration: const InputDecoration(labelText: 'Chọn lịch'),
                  items: [
                    for (final s in c.schedules)
                      DropdownMenuItem(
                        value: s['id'] as int,
                        child: Text(
                          '${assignmentTargets[s['scopeType']] ?? 'Toàn viện'}${s['scopeId'] == null ? '' : ' mã ${s['scopeId']}'} · ${s['month'] ?? c.monthLabel}',
                        ),
                      ),
                  ],
                  onChanged: (v) => setState(() {
                    schedule = v;
                    selected = null;
                    c.shift = null;
                    c.rows = [];
                  }),
                ),
              const SizedBox(height: 16),
              DropdownButtonFormField<int>(
                key: ValueKey('shift:$selected:${c.monthLabel}:$schedule'),
                initialValue: shifts.any((s) => s['id'] == selected) ? selected : null,
                isExpanded: true,
                isDense: false,
                itemHeight: null,
                decoration: const InputDecoration(labelText: 'Chọn ca'),
                items: [
                  for (final s in shifts)
                    DropdownMenuItem(
                      value: s['id'] as int,
                      child: Text(
                        '${formatDateTime(DateTime.parse(s['startsAt'] as String))} – ${formatDateTime(DateTime.parse(s['endsAt'] as String))}',
                      ),
                    ),
                ],
                onChanged: (v) {
                  if (v != null) select(v);
                },
              ),
              if (shifts.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Text('Chưa có ca trong tháng được xem.'),
                ),
            ],
            if (c.shift != null && !c.loading) ...[
              const SizedBox(height: 24),
              Text('Phân công trong ca', style: Theme.of(context).textTheme.titleLarge),
              if (c.rows.isEmpty)
                const Padding(padding: EdgeInsets.symmetric(vertical: 16), child: Text('Chưa có phân công trong ca.')),
              for (final row in c.rows)
                ColoredBox(
                  color: Theme.of(context).colorScheme.surface,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(assignmentName(names, row['staffId']), style: Theme.of(context).textTheme.titleMedium),
                        StatusPill(
                          label: assignmentStates[row['status']] ?? 'Chưa xác định',
                          tone: row['status'] == 'ACTIVE' ? StatusTone.settled : StatusTone.neutral,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          targets['${row['targetType']}:${row['targetId']}'] ?? 'Chưa có tên đối tượng được phép xem',
                        ),
                        Text(
                          '${assignmentRoles[row['role']] ?? 'Vai trò chưa xác định'} · ${row['assignmentKind'] == 'PRIMARY' ? 'Chính' : 'Hỗ trợ'}',
                        ),
                        if (row['assignedNurse'] == true) const Text('Điều dưỡng phụ trách'),
                        Text(
                          '${formatDateTime(DateTime.parse(row['startsAt'] as String))} → ${formatDateTime(DateTime.parse(row['endsAt'] as String))}',
                        ),
                        Wrap(
                          spacing: 8,
                          children: [
                            if (row['status'] == 'DRAFT' && c.command('updateDraftAssignment'))
                              TextButton(onPressed: c.busy ? null : () => edit(row), child: const Text('Sửa Nháp')),
                            if (row['status'] == 'DRAFT' && c.command('deleteAssignment'))
                              TextButton(
                                onPressed: c.busy ? null : () => finish(row, 'delete'),
                                child: const Text('Xóa Nháp'),
                              ),
                            if (row['status'] == 'SCHEDULED' && c.command('cancelAssignment'))
                              TextButton(
                                onPressed: c.busy ? null : () => finish(row, 'cancel'),
                                child: const Text('Hủy phân công'),
                              ),
                            if (row['status'] == 'ACTIVE' && c.command('endAssignment'))
                              TextButton(
                                onPressed: c.busy ? null : () => finish(row, 'end'),
                                child: const Text('Kết thúc phân công'),
                              ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              if (c.command('createAssignment'))
                FilledButton.icon(
                  onPressed: c.busy ? null : edit,
                  icon: const Icon(LucideIcons.plus),
                  label: const Text('Lập phân công'),
                ),
              if (c.command('copyAssignments'))
                OutlinedButton(onPressed: c.busy ? null : copy, child: const Text('Sao chép phân công')),
              if (c.shift!['status'] != 'DRAFT' && c.shift!['status'] != 'CANCELLED') ...[
                OutlinedButton(onPressed: resolve, child: const Text('Tra người phụ trách')),
                OutlinedButton(onPressed: unassigned, child: const Text('Người còn thiếu người phụ trách')),
              ],
            ],
            if (c.view)
              OutlinedButton(
                onPressed: () => Navigator.of(
                  context,
                ).push<void>(MaterialPageRoute(builder: (_) => FloorManagerSheet(controller: c))),
                child: const Text('Giao Trưởng tầng'),
              ),
          ],
        ),
      );
    },
  );
}

/// Danh sách ca đã được backend lọc quyền trong lịch, không tự mở thêm phạm vi.
class AssignmentsControllerShifts {
  static List<Map<String, dynamic>> inSchedule(List<Map<String, dynamic>> schedules, int id) {
    final schedule = schedules.where((s) => s['id'] == id).firstOrNull;
    return (schedule?['shifts'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
  }
}
