import 'dart:math';
import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';
import '../../common/widgets/status_pill.dart';
import 'care_checklist_repository.dart';
import 'care_result_form.dart';
import 'care_task_context.dart';

String careCommandKey() {
  final random = Random.secure();
  final bytes = List<int>.generate(16, (_) => random.nextInt(256));
  bytes[6] = (bytes[6] & 15) | 64;
  bytes[8] = (bytes[8] & 63) | 128;
  final h = bytes.map((v) => v.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}

/// Nhận accountId/shiftId từ shell của A; không tự chọn một ca hoặc một tài khoản giả.
class CareChecklistScreen extends StatefulWidget {
  const CareChecklistScreen({super.key, required this.repository, required this.accountId, required this.shiftId});
  final CareChecklistRepository repository;
  final int accountId;
  final int shiftId;
  @override
  State<CareChecklistScreen> createState() => _CareChecklistScreenState();
}

class _CareChecklistScreenState extends State<CareChecklistScreen> {
  CareChecklistLoad? loaded;
  String? error;
  bool busy = false;
  int generation = 0;
  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  void didUpdateWidget(CareChecklistScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.accountId != widget.accountId ||
        oldWidget.shiftId != widget.shiftId ||
        oldWidget.repository != widget.repository) {
      loaded = null;
      error = null;
      load();
    }
  }

  Future<void> load() async {
    final token = ++generation;
    final accountId = widget.accountId, shiftId = widget.shiftId;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      final next = await widget.repository.load(accountId, shiftId);
      if (mounted && generation == token) setState(() => loaded = next);
    } catch (e) {
      if (mounted && generation == token) {
        setState(() {
          error = e is ApiProblem ? e.detail : 'Không tải được checklist. Kiểm tra kết nối rồi thử lại.';
          loaded = null;
        });
      }
    } finally {
      if (mounted && generation == token) setState(() => busy = false);
    }
  }

  Future<void> claim(Map<String, dynamic> task) async {
    setState(() => busy = true);
    try {
      await widget.repository.gateway.claim(task['id'] as int, careCommandKey());
      await load();
    } catch (e) {
      if (mounted) {
        setState(() => error = e is ApiProblem ? e.detail : 'Chưa nhận được công việc. Tải lại rồi thử lại.');
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> notPerform(Map<String, dynamic> task) async {
    final reasons =
        (loaded!.snapshot.content['nonPerformanceReasonTypes'] as List? ?? [])
            .map((r) => Map<String, dynamic>.from(r as Map))
            .where((r) => r['status'] == 'ACTIVE')
            .toList()
          ..sort((a, b) => (b['fixed'] == true ? 1 : 0).compareTo(a['fixed'] == true ? 1 : 0));
    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _NotPerformedForm(repository: widget.repository, taskId: task['id'] as int, reasons: reasons),
    );
    if (saved == true && mounted) await load();
  }

  Future<void> record(Map<String, dynamic> task) async {
    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (_) => CareResultForm(
        task: task,
        repository: widget.repository,
        newKey: careCommandKey,
        resultValues: (loaded!.snapshot.content['resultValues'] as List? ?? [])
            .map((v) => Map<String, dynamic>.from(v as Map))
            .toList(),
      ),
    );
    if (saved == true && mounted) await load();
  }

  @override
  Widget build(BuildContext context) {
    final data = loaded?.snapshot.content;
    final tasks = (data?['tasks'] as List? ?? []).map((t) => Map<String, dynamic>.from(t as Map)).toList()
      ..sort((a, b) {
        final urgency = (b['status'] == 'OVERDUE' ? 1 : 0).compareTo(a['status'] == 'OVERDUE' ? 1 : 0);
        return urgency != 0 ? urgency : (a['plannedAt'] as String? ?? '').compareTo(b['plannedAt'] as String? ?? '');
      });
    final agreements = (data?['riskAgreements'] as List? ?? []).map((a) => Map<String, dynamic>.from(a as Map));
    return Scaffold(
      appBar: AppBar(title: const Text('Checklist ca')),
      body: PageBody(
        child: ListView(
          children: [
            if (busy) const LinearProgressIndicator(),
            if (widget.repository.lastQueued)
              Semantics(
                liveRegion: true,
                child: const Text('Chưa đồng bộ. Bản ghi đã lưu trên máy, sẽ đồng bộ khi có mạng.'),
              ),
            if (error != null) Text(error!, semanticsLabel: error),
            if (loaded != null)
              Text('${loaded!.offline ? 'Chưa cập nhật · ' : 'Cập nhật '}${formatDateTime(loaded!.snapshot.loadedAt)}'),
            if (loaded?.offline == true)
              Text(
                widget.repository.recorder == null
                    ? 'Đang xem bản đã tải. Kết nối lại và tải mới trước khi ghi nhận.'
                    : 'Đang ngoại tuyến. Bản ghi sẽ đồng bộ khi có mạng.',
              ),
            TextButton(onPressed: busy ? null : load, child: const Text('Tải lại checklist')),
            if (loaded != null && tasks.isEmpty) const Text('Chưa có công việc trong phạm vi được giao.'),
            for (final dose in (data?['medicationDoses'] as List? ?? []).map(
              (d) => Map<String, dynamic>.from(d as Map),
            ))
              ListTile(
                title: Text((dose['medication'] as Map?)?['description'] as String? ?? 'Liều thuốc ${dose['doseKey']}'),
                subtitle: dose['plannedAt'] == null
                    ? null
                    : Text('Dự kiến ${formatTime(DateTime.parse(dose['plannedAt'] as String))}'),
              ),
            for (final alert in (data?['openAlerts'] as List? ?? []).map((a) => Map<String, dynamic>.from(a as Map)))
              ListTile(title: Text('Cảnh báo #${alert['id']}')),
            if ((data?['handoverTasks'] as Map?)?['receiveAtStart'] == true)
              const ListTile(title: Text('Nhận bàn giao đầu ca')),
            if ((data?['handoverTasks'] as Map?)?['prepareAtEnd'] == true)
              const ListTile(title: Text('Lập bàn giao cuối ca')),
            for (final task in tasks)
              Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Divider(),
                  Material(
                    color: Theme.of(context).colorScheme.surface,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          StatusPill(
                            label: _status(task['status'] as String?),
                            tone: task['status'] == 'OVERDUE' ? StatusTone.urgent : StatusTone.neutral,
                          ),
                          Text(task['taskType'] as String? ?? '', style: Theme.of(context).textTheme.titleMedium),
                          if (widget.repository.pendingTaskIds.contains(task['id'])) const Text('Chưa đồng bộ'),
                          CareTaskContext(task: task),
                          if (task['awaitingRemeasurementConfirmation'] == true) const Text('Chờ xác nhận đo lại'),
                          if ((loaded?.offline == false || widget.repository.recorder != null) &&
                              (task['allowedCommands'] as List? ?? []).contains('record'))
                            FilledButton(
                              onPressed: busy || widget.repository.pendingTaskIds.contains(task['id'])
                                  ? null
                                  : () => record(task),
                              child: const Text('Ghi nhận kết quả'),
                            ),
                          for (final entry in agreements.where((a) => a['residentId'] == task['residentId']))
                            for (final agreement in (entry['agreements'] as List? ?? []).map(
                              (a) => Map<String, dynamic>.from(a as Map),
                            ))
                              Text(agreement['choice'] as String? ?? ''),
                          if (loaded?.offline == false && (task['allowedCommands'] as List? ?? []).contains('claim'))
                            OutlinedButton(
                              onPressed: busy ? null : () => claim(task),
                              child: const Text('Nhận công việc'),
                            ),
                          if ((loaded?.offline == false || widget.repository.recorder != null) &&
                              (task['allowedCommands'] as List? ?? []).contains('notPerform'))
                            OutlinedButton(
                              onPressed: busy || widget.repository.pendingTaskIds.contains(task['id'])
                                  ? null
                                  : () => notPerform(task),
                              child: const Text('Không thực hiện được…'),
                            ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}

String _status(String? status) =>
    const {
      'NOT_YET_DUE': 'Chưa đến hạn',
      'DUE': 'Đến hạn',
      'OVERDUE': 'Quá hạn',
      'COMPLETED': 'Hoàn thành',
      'COMPLETED_LATE': 'Hoàn thành trễ',
      'NOT_PERFORMED': 'Không thực hiện',
      'CANCELLED': 'Hủy',
    }[status] ??
    status ??
    '';

class _NotPerformedForm extends StatefulWidget {
  const _NotPerformedForm({required this.repository, required this.taskId, required this.reasons});
  final CareChecklistRepository repository;
  final int taskId;
  final List<Map<String, dynamic>> reasons;
  @override
  State<_NotPerformedForm> createState() => _NotPerformedFormState();
}

class _NotPerformedFormState extends State<_NotPerformedForm> {
  final note = TextEditingController();
  int? reason;
  String? error;
  String? retrySignature;
  String? retryKey;
  bool busy = false;
  @override
  void dispose() {
    note.dispose();
    super.dispose();
  }

  Future<void> save() async {
    if (reason == null) {
      setState(() => error = 'Chọn loại lý do Không thực hiện.');
      return;
    }
    final signature = '$reason:${note.text}';
    if (signature != retrySignature) {
      retrySignature = signature;
      retryKey = careCommandKey();
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await widget.repository.notPerform(widget.taskId, reason!, note.text, retryKey!);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        setState(() => error = e is ApiProblem ? e.detail : 'Chưa gửi được. Giữ nguyên nội dung để thử lại.');
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: !busy,
    child: Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, MediaQuery.viewInsetsOf(context).bottom + 16),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Không thực hiện', style: Theme.of(context).textTheme.titleLarge),
            const Text('Loại lý do (bắt buộc)'),
            RadioGroup<int>(
              groupValue: reason,
              onChanged: (value) {
                if (!busy) setState(() => reason = value);
              },
              child: Column(
                children: [
                  for (final r in widget.reasons)
                    RadioListTile<int>(value: r['id'] as int, title: Text(r['name'] as String), enabled: !busy),
                ],
              ),
            ),
            TextField(
              controller: note,
              enabled: !busy,
              maxLength: 2000,
              decoration: const InputDecoration(labelText: 'Ghi chú'),
            ),
            const Text('Có thể ghi điều người cao tuổi đã nói.'),
            if (error != null) Text(error!),
            FilledButton(onPressed: busy ? null : save, child: Text(busy ? 'Đang lưu…' : 'Lưu Không thực hiện')),
          ],
        ),
      ),
    ),
  );
}
