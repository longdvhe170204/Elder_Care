import 'package:flutter/material.dart';

import '../../common/api/problem_message.dart';
import '../../common/widgets/page_body.dart';
import 'assignment_labels.dart';
import 'assignment_moment.dart';
import 'assignments_controller.dart';

class FloorManagerSheet extends StatefulWidget {
  const FloorManagerSheet({super.key, required this.controller});
  final AssignmentsController controller;
  @override
  State<FloorManagerSheet> createState() => _FloorManagerSheetState();
}

class _FloorManagerSheetState extends State<FloorManagerSheet> {
  List<Map<String, dynamic>> rows = [], staff = [], floors = [];
  bool loading = true;
  String? error, from, to;
  int? person, floor;
  final Map<int, Map<String, dynamic>> endDrafts = {};
  final reason = TextEditingController();
  AssignmentsController get c => widget.controller;
  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  void dispose() {
    reason.dispose();
    super.dispose();
  }

  Future<void> load() async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      rows = await c.floorManagers();
      if (c.homeManager) {
        staff = await c.catalog('/staff');
        floors = await c.catalog('/floors');
      }
    } on Object catch (e) {
      error = backendMessage(e);
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<String?> day() async {
    final now = DateTime.now().toUtc().add(const Duration(hours: 7));
    return assignmentMoment(context, now, withTime: false);
  }

  String label(String value) {
    final p = value.split('-');
    return '${p[2]}/${p[1]}/${p[0]}';
  }

  Future<void> end(Map<String, dynamic> row) async {
    final id = row['id'] as int;
    String? to = endDrafts[id]?['to'] as String?;
    var reason = endDrafts[id]?['reason'] as String? ?? '';
    final body = await showDialog<Map<String, dynamic>>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, update) => AlertDialog(
          title: const Text('Rút ngắn giao Trưởng tầng'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                OutlinedButton(
                  onPressed: () async {
                    final value = await day();
                    if (value != null) {
                      update(() {
                        to = value;
                        endDrafts[id] = {'to': to, 'reason': reason};
                      });
                    }
                  },
                  child: Text(to == null ? 'Chọn ngày kết thúc' : label(to!)),
                ),
                TextFormField(
                  initialValue: reason,
                  maxLines: 3,
                  decoration: const InputDecoration(labelText: 'Lý do bắt buộc'),
                  onChanged: (value) => update(() {
                    reason = value;
                    endDrafts[id] = {'to': to, 'reason': reason};
                  }),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: const Text('Quay lại')),
            FilledButton(
              onPressed: to == null || reason.trim().isEmpty
                  ? null
                  : () => Navigator.pop(context, <String, dynamic>{'to': to, 'reason': reason.trim()}),
              child: const Text('Xác nhận'),
            ),
          ],
        ),
      ),
    );
    if (body != null) {
      final ok = await c.endFloor(row['id'] as int, body);
      if (!mounted) return;
      if (ok) {
        endDrafts.remove(id);
        await load();
      } else {
        setState(() => error = c.error);
      }
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Giao Trưởng tầng')),
    body: ListenableBuilder(
      listenable: c,
      builder: (context, _) => PageBody(
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            if (loading) const Center(child: CircularProgressIndicator()),
            if (error != null) ...[Text(error!), TextButton(onPressed: load, child: const Text('Thử lại'))],
            if (!loading && rows.isEmpty) const Text('Chưa có giao Trưởng tầng trong phạm vi được xem.'),
            for (final row in rows)
              ColoredBox(
                color: Theme.of(context).colorScheme.surface,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(assignmentName(staff, row['staffId'])),
                      Text(assignmentName(floors, row['floorAreaId'], fallback: 'Tầng')),
                      Text(
                        '${label(row['from'] as String)} → ${row['to'] == null ? 'Chưa có ngày kết thúc' : label(row['to'] as String)}',
                      ),
                      if (c.homeManager && c.can('UPDATE'))
                        TextButton(onPressed: c.busy ? null : () => end(row), child: const Text('Rút ngắn thời hạn')),
                    ],
                  ),
                ),
              ),
            if (c.homeManager && c.can('CREATE') && !loading) ...[
              const SizedBox(height: 24),
              Text('Giao mới', style: Theme.of(context).textTheme.titleLarge),
              DropdownButtonFormField<int>(
                isExpanded: true,
                isDense: false,
                itemHeight: null,
                decoration: const InputDecoration(labelText: 'Trưởng tầng'),
                items: [
                  for (final s in staff) DropdownMenuItem(value: s['id'] as int, child: Text(s['fullName'] as String)),
                ],
                onChanged: c.busy ? null : (v) => setState(() => person = v),
              ),
              DropdownButtonFormField<int>(
                isExpanded: true,
                isDense: false,
                itemHeight: null,
                decoration: const InputDecoration(labelText: 'Tầng'),
                items: [
                  for (final f in floors) DropdownMenuItem(value: f['id'] as int, child: Text(f['name'] as String)),
                ],
                onChanged: c.busy ? null : (v) => setState(() => floor = v),
              ),
              OutlinedButton(
                onPressed: c.busy
                    ? null
                    : () async {
                        final v = await day();
                        if (v != null && mounted) setState(() => from = v);
                      },
                child: Text(from == null ? 'Chọn ngày bắt đầu' : label(from!)),
              ),
              OutlinedButton(
                onPressed: c.busy
                    ? null
                    : () async {
                        final v = await day();
                        if (v != null && mounted) setState(() => to = v);
                      },
                child: Text(to == null ? 'Chọn ngày kết thúc (không bắt buộc)' : label(to!)),
              ),
              if (to != null)
                TextButton(onPressed: () => setState(() => to = null), child: const Text('Bỏ ngày kết thúc')),
              TextField(
                controller: reason,
                decoration: const InputDecoration(labelText: 'Lý do (nếu có)'),
                maxLines: 3,
              ),
              FilledButton(
                onPressed: c.busy || person == null || floor == null || from == null
                    ? null
                    : () async {
                        final ok = await c.assignFloor({
                          'staffId': person,
                          'floorAreaId': floor,
                          'from': from,
                          if (to != null) 'to': to,
                          if (reason.text.trim().isNotEmpty) 'reason': reason.text.trim(),
                        });
                        if (!mounted) return;
                        if (ok) {
                          reason.clear();
                          await load();
                        } else {
                          setState(() => error = c.error);
                        }
                      },
                child: const Text('Giao Trưởng tầng'),
              ),
            ],
          ],
        ),
      ),
    ),
  );
}
