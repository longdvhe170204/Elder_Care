import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/offline/offline_queue.dart';
import '../../common/widgets/page_body.dart';
import 'ad_hoc_care_recording_screen.dart';
import 'care_api_gateways.dart';
import 'care_checklist_cache.dart';
import 'care_checklist_gateway.dart';
import 'care_checklist_repository.dart';
import 'care_checklist_screen.dart';
import 'care_offline_recording.dart';
import 'day_care_attendance_screen.dart';
import 'overdue_tasks_screen.dart';
import 'shift_task_planning_screen.dart';

/// T026/T031/T040/T046: lối vào màn005; quyền lấy từ backend, không suy theo vai trò.
class CareTasksHomeScreen extends StatefulWidget {
  const CareTasksHomeScreen({super.key, required this.api, required this.accountId, this.queue, this.cache});
  final ApiClient api;
  final int accountId;
  final OfflineQueue? queue;
  final CareChecklistCache? cache;
  @override
  State<CareTasksHomeScreen> createState() => _CareTasksHomeScreenState();
}

class _CareTasksHomeScreenState extends State<CareTasksHomeScreen> {
  Set<String>? permissions;
  String? error;
  List<int>? offlineShifts;
  Future<void> load() async {
    try {
      final rows = await widget.api.getList('/me/permissions');
      final current = rows.whereType<Map<String, dynamic>>().map((p) => '${p['function']}:${p['operation']}').toSet();
      if (!current.contains('TASK_CHECKLIST:VIEW')) {
        final cache = widget.cache ?? CareChecklistCache.onDevice();
        try {
          await cache.clearAccount(widget.accountId);
        } finally {
          if (widget.cache == null) await cache.close();
        }
      }
      if (mounted) {
        setState(() {
          permissions = current;
          error = null;
          offlineShifts = null;
        });
      }
    } on ApiUnreachable {
      final cache = widget.cache ?? CareChecklistCache.onDevice();
      try {
        final cached = await cache.shiftIds(widget.accountId);
        if (mounted) {
          setState(() {
            offlineShifts = cached;
            error = 'Đang ngoại tuyến. Chỉ mở được checklist đã tải của tài khoản này.';
          });
        }
      } finally {
        if (widget.cache == null) await cache.close();
      }
    } catch (e) {
      if (mounted) {
        setState(() => error = e is ApiProblem ? e.detail : 'Không tải được quyền. Kiểm tra kết nối rồi thử lại.');
      }
    }
  }

  @override
  void initState() {
    super.initState();
    load();
  }

  bool can(String function, String operation) => permissions?.contains('$function:$operation') == true;
  void open(Widget screen) => Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => screen));

  Future<void> checklist() async {
    try {
      final rows = offlineShifts == null
          ? await ApiCareTaskPlanningGateway(widget.api).shifts()
          : [
              for (final id in offlineShifts!) <String, dynamic>{'id': id},
            ];
      if (!mounted) return;
      final selected = await showDialog<int>(
        context: context,
        builder: (context) => SimpleDialog(
          title: const Text('Chọn ca xem checklist'),
          children: [
            if (rows.isEmpty)
              const Padding(padding: EdgeInsets.all(16), child: Text('Chưa có ca đã công bố trong tháng.')),
            for (final row in rows)
              SimpleDialogOption(
                onPressed: () => Navigator.pop(context, row['id'] as int),
                child: Text('Ca ${row['id']}'),
              ),
          ],
        ),
      );
      if (selected == null || !mounted) return;
      final cache = widget.cache ?? CareChecklistCache.onDevice();
      final repository = CareChecklistRepository(
        ApiCareChecklistGateway(widget.api),
        cache,
        recorder: widget.queue == null ? null : CareOfflineRecording(widget.api, widget.queue!, widget.accountId),
      );
      try {
        await Navigator.of(context).push(
          MaterialPageRoute<void>(
            builder: (_) => CareChecklistScreen(repository: repository, accountId: widget.accountId, shiftId: selected),
          ),
        );
      } finally {
        if (widget.cache == null) await cache.close();
      }
    } catch (e) {
      if (mounted) {
        setState(() => error = e is ApiProblem ? e.detail : 'Không mở được checklist. Kiểm tra kết nối rồi thử lại.');
      }
    }
  }

  @override
  Widget build(BuildContext context) => PageBody(
    child: ListView(
      children: [
        if (offlineShifts?.isNotEmpty == true)
          ListTile(title: const Text('Checklist đã tải (ngoại tuyến)'), onTap: checklist),
        if (error != null) ...[
          Semantics(liveRegion: true, child: Text(error!)),
          OutlinedButton(onPressed: load, child: const Text('Thử lại')),
        ],
        if (permissions == null && error == null) const Center(child: CircularProgressIndicator()),
        if (can('TASK_CHECKLIST', 'VIEW'))
          ListTile(leading: const Icon(LucideIcons.listChecks), title: const Text('Checklist ca'), onTap: checklist),
        if (can('TASK_CHECKLIST', 'CREATE') && widget.queue != null)
          ListTile(
            leading: const Icon(LucideIcons.clipboardPlus),
            title: const Text('Ghi nhận phát sinh'),
            onTap: () =>
                open(AdHocCareRecordingScreen(api: widget.api, queue: widget.queue!, accountId: widget.accountId)),
          ),
        if (can('SHIFT_TASK_PLANNING', 'VIEW'))
          ListTile(
            leading: const Icon(LucideIcons.clipboardList),
            title: const Text('Lập công việc của ca'),
            onTap: () => open(
              Scaffold(
                appBar: AppBar(title: const Text('Lập công việc của ca')),
                body: ShiftTaskPlanningScreen(gateway: ApiCareTaskPlanningGateway(widget.api)),
              ),
            ),
          ),
        if (can('DAY_CARE_ATTENDANCE', 'VIEW'))
          ListTile(
            leading: const Icon(LucideIcons.userCheck),
            title: const Text('Điểm danh bán trú'),
            onTap: () {
              final day = DateTime.now().toUtc().add(const Duration(hours: 7));
              final date = '${day.year}-${day.month.toString().padLeft(2, '0')}-${day.day.toString().padLeft(2, '0')}';
              open(
                Scaffold(
                  appBar: AppBar(title: const Text('Điểm danh bán trú')),
                  body: DayCareAttendanceScreen(gateway: ApiDayCareAttendanceGateway(widget.api), date: date),
                ),
              );
            },
          ),
        if (can('OVERDUE_TASK_HANDLING', 'VIEW'))
          ListTile(
            leading: const Icon(LucideIcons.clockAlert),
            title: const Text('Công việc quá hạn'),
            onTap: () => open(
              Scaffold(
                appBar: AppBar(title: const Text('Công việc quá hạn')),
                body: OverdueTasksScreen(
                  accountId: widget.accountId,
                  canView: true,
                  load: (_, floor) async => (await widget.api.getList(
                    '/care-tasks/overdue${floor == null ? '' : '?floorAreaId=$floor'}',
                  )).map((r) => Map<String, dynamic>.from(r as Map)).toList(),
                ),
              ),
            ),
          ),
        if (permissions != null &&
            ![
              'TASK_CHECKLIST',
              'SHIFT_TASK_PLANNING',
              'DAY_CARE_ATTENDANCE',
              'OVERDUE_TASK_HANDLING',
            ].any((f) => can(f, 'VIEW')))
          const Text('Bạn không có quyền mở màn chăm sóc hằng ngày.'),
      ],
    ),
  );
}
