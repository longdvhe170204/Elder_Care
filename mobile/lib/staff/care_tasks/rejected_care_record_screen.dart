import 'dart:convert';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/offline/offline_database.dart';
import '../../common/offline/offline_queue.dart';
import '../../common/widgets/page_body.dart';
import 'ad_hoc_care_recording_screen.dart';
import 'care_checklist_screen.dart' show careCommandKey;

/// Các trường giá trị được đính chính của kết quả chăm sóc (`M04.CARE_RESULT`, 005 FR-037).
const _correctable = {'numericValue', 'codeValue', 'jsonValue', 'upperLimitConfirmed', 'note'};

/// T040 (BR-M15-20, FR-036; chủ dự án duyệt 2026-11-11): kết quả công việc ghi khi mất mạng bị từ chối lúc đồng bộ (công
/// việc đã đóng hoặc đã hủy). Nội dung gốc được giữ; người ghi chọn đính chính kết quả đang có của công việc hoặc chuyển
/// thành Ghi nhận phát sinh (người cao tuổi, loại công việc lấy từ `GET /care-tasks/{id}`, không suy từ tên loại).
class RejectedCareRecordScreen extends StatefulWidget {
  const RejectedCareRecordScreen({
    super.key,
    required this.api,
    required this.queue,
    required this.accountId,
    required this.record,
  });

  final ApiClient api;
  final OfflineQueue queue;
  final int accountId;
  final QueuedRecord record;

  @override
  State<RejectedCareRecordScreen> createState() => _RejectedCareRecordScreenState();
}

class _RejectedCareRecordScreenState extends State<RejectedCareRecordScreen> {
  late final Map<String, dynamic> content = jsonDecode(widget.record.content) as Map<String, dynamic>;
  late final int taskId = content['taskId'] as int;
  late final Map<String, dynamic> recording = Map<String, dynamic>.from(content['recording'] as Map? ?? const {});
  final reason = TextEditingController();
  String correctionKey = careCommandKey();
  Map<String, dynamic>? task;
  String? error, message;
  bool busy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    reason.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final detail = await widget.api.get('/care-tasks/$taskId');
      if (mounted) {
        setState(() {
          task = detail;
          error = null;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => error = e is ApiProblem ? e.detail : 'Không tải được công việc. Kiểm tra kết nối rồi thử lại.');
      }
    }
  }

  bool get _hasResult => task?['result'] != null;

  Future<void> _correct() async {
    if (reason.text.trim().isEmpty) {
      setState(() => error = 'Nhập lý do đính chính.');
      return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await widget.api.post(
        '/care-results/$taskId/correct',
        {
          'type': 'AMEND_CONTENT',
          'correctedContent': {
            for (final e in recording.entries)
              if (_correctable.contains(e.key)) e.key: e.value,
          },
          'reason': reason.text.trim(),
        },
        headers: {'Idempotency-Key': correctionKey},
      );
      if (mounted) setState(() => message = 'Đã lưu đính chính.');
    } on ApiProblem catch (p) {
      correctionKey = careCommandKey();
      if (mounted) setState(() => error = p.detail ?? 'Chưa lưu được đính chính.');
    } on ApiUnreachable {
      if (mounted) setState(() => error = 'Đính chính cần có mạng. Thử lại khi có kết nối.');
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  void _toAdHoc() {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => AdHocCareRecordingScreen(
          api: widget.api,
          queue: widget.queue,
          accountId: widget.accountId,
          initialResidentId: task!['residentId'] as int?,
          initialTaskTypeId: task!['taskTypeId'] as int?,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final values = [
      if (recording['numericValue'] != null) 'Giá trị: ${recording['numericValue']}',
      if (recording['codeValue'] != null) 'Kết quả: ${recording['codeValue']}',
      if (recording['note'] != null) 'Ghi chú: ${recording['note']}',
    ];
    return Scaffold(
      appBar: AppBar(title: const Text('Bản ghi bị từ chối')),
      body: PageBody(
        child: ListView(
          children: [
            if (widget.record.reason != null) Text('Lý do từ chối: ${widget.record.reason}', style: text.bodyMedium),
            const SizedBox(height: 8),
            Text('Nội dung đã ghi trên máy', style: text.titleSmall),
            for (final v in values) Text(v, style: text.bodyMedium),
            const SizedBox(height: 16),
            if (error != null) Semantics(liveRegion: true, child: Text(error!)),
            if (message != null) Semantics(liveRegion: true, child: Text(message!)),
            if (task == null && error == null) const Center(child: CircularProgressIndicator()),
            if (task == null && error != null) OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
            if (task != null) ...[
              Text(task!['taskType'] as String? ?? 'Công việc', style: text.titleMedium),
              if (_hasResult) ...[
                const SizedBox(height: 12),
                TextField(
                  controller: reason,
                  decoration: const InputDecoration(labelText: 'Lý do đính chính'),
                ),
                const SizedBox(height: 8),
                FilledButton(
                  onPressed: busy ? null : _correct,
                  child: const Text('Đính chính kết quả đã ghi'),
                ),
              ],
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: task!['taskTypeId'] == null ? null : _toAdHoc,
                child: const Text('Chuyển thành Ghi nhận phát sinh'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
