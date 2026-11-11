import 'dart:convert';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/notifications/notification_api.dart';
import '../../common/widgets/page_body.dart';
import '../../common/widgets/status_pill.dart';
import 'handover_cache.dart';

/// T030: đọc và các lệnh trực tuyến; thêm, bỏ mục và ghi nhận định gửi If-Match theo ETag của `GET /handovers/{id}`.
/// Bản lưu ngoại tuyến (FR-060) chỉ khi `/me/shifts` cho hạn phạm vi `scopeEndsAt`; mất mạng thì xem bản lưu, không có lệnh.
class HandoverScreen extends StatefulWidget {
  const HandoverScreen({super.key, required this.api, this.initialShiftId, this.cache});

  final ApiClient api;
  final int? initialShiftId;
  final HandoverCache? cache;

  @override
  State<HandoverScreen> createState() => _HandoverScreenState();
}

class _HandoverScreenState extends State<HandoverScreen> {
  static const _states = {
    'DRAFT': 'Bản nháp',
    'PREPARED': 'Đã lập',
    'HAS_COMMENTS': 'Có ý kiến',
    'CONFIRMED': 'Đã xác nhận',
  };
  static const _groups = {
    'TASK': 'Công việc',
    'DOSE': 'Liều thuốc',
    'VITAL_SIGN': 'Chỉ số vượt ngưỡng',
    'ALERT': 'Cảnh báo',
    'INCIDENT': 'Sự cố',
    'RESIDENT_CHANGE': 'Biến động người cao tuổi',
    'BED_AWAITING_READINESS': 'Giường chờ sẵn sàng',
    'STORED_BELONGING': 'Đồ gửi đang giữ',
    'FOLLOW_UP_ISSUE': 'Vấn đề cần theo dõi',
  };

  late DateTime month;
  List<Map<String, dynamic>> shifts = [], sources = [];
  Map<String, dynamic>? shift, record;
  Map<int, String> names = {};
  final comment = TextEditingController();
  final Map<int, String> replies = {};
  final Map<String, String> keys = {};
  int? selected;
  int generation = 0;
  bool loading = false, busy = false;
  String? error, sourceError, etag;
  CachedHandover? offline;
  final assessment = TextEditingController();

  @override
  void initState() {
    super.initState();
    final vn = DateTime.now().toUtc().add(const Duration(hours: 7));
    month = DateTime.utc(vn.year, vn.month);
    selected = widget.initialShiftId;
    load();
  }

  @override
  void dispose() {
    generation++;
    comment.dispose();
    assessment.dispose();
    replies.clear();
    record = null;
    sources = [];
    names = {};
    super.dispose();
  }

  List<Map<String, dynamic>> maps(Object? data) => (data as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
  String day(DateTime value) => '${value.year}-${value.month.toString().padLeft(2, '0')}-01';
  bool allowed(String command) =>
      offline == null && (record?['allowedCommands'] as List<dynamic>? ?? []).contains(command);

  DateTime? scopeEndsAt(int? shiftId) {
    final row = shifts.where((s) => s['id'] == shiftId).firstOrNull;
    return row?['scopeEndsAt'] is String ? DateTime.parse(row!['scopeEndsAt'] as String) : null;
  }

  Future<void> load({int move = 0, int? shiftId}) async {
    final request = ++generation;
    setState(() {
      month = DateTime.utc(month.year, month.month + move);
      if (move != 0) selected = null;
      selected = shiftId ?? selected;
      loading = true;
      error = null;
      sourceError = null;
      shift = null;
      record = null;
      sources = [];
      names = {};
      offline = null;
      etag = null;
    });
    try {
      await widget.cache?.purgeExpired();
      final grants = maps(await widget.api.getList('/me/permissions'));
      if (!grants.any((p) => p['function'] == 'SHIFT_HANDOVER' && p['operation'] == 'VIEW')) {
        throw ApiProblem(403, 'PERMISSION_DENIED', 'Bạn không có quyền xem bàn giao ca.');
      }
      if (widget.initialShiftId == null) {
        final manage = grants.any(
          (p) => p['function'] == 'SHIFT_SCHEDULE' && ['CREATE', 'UPDATE', 'APPROVE'].contains(p['operation']),
        );
        final rows = maps(
          await widget.api.getList(
            manage
                ? '/shift-schedules?month=${day(month).substring(0, 7)}'
                : '/me/shifts?from=${day(month)}&to=${day(DateTime.utc(month.year, month.month + 1))}',
          ),
        );
        if (!mounted || request != generation) return;
        shifts = manage ? [for (final schedule in rows) ...maps(schedule['shifts'])] : rows;
        if (!shifts.any((row) => row['id'] == selected)) selected = null;
      }
      if (selected == null) return;
      final current = await widget.api.get('/shifts/$selected');
      Map<String, dynamic>? value;
      try {
        value = await widget.api.get('/shifts/$selected/handovers');
      } on ApiProblem catch (p) {
        if (p.status != 404) rethrow;
      }
      if (value != null) {
        // ETag cho If-Match của lệnh sửa (FR-040); thân mới nhất của cùng bàn giao
        final full = await widget.api.requestResponse('GET', '/handovers/${value['id']}');
        if (full.body is Map<String, dynamic>) value = full.body as Map<String, dynamic>;
        etag = full.headers['etag'];
      }
      if (!mounted || request != generation) return;
      shift = current;
      record = value;
      assessment.text = (value?['generalAssessment'] as String?) ?? '';
      final endsAt = scopeEndsAt(selected);
      if (value == null) {
        await widget.cache?.remove(selected!);
        return;
      }
      if (endsAt != null) await widget.cache?.save(selected!, value, endsAt);
      List<Map<String, dynamic>> readSources = [];
      String? failedSource;
      try {
        readSources = maps(
          await widget.api.getList(
            '/handovers/${value['id']}/source-records?afterPreparation=${value['status'] != 'DRAFT'}',
          ),
        );
      } on ApiProblem catch (p) {
        if (p.status == 401 || p.status == 403 || p.status == 404) rethrow;
        failedSource = backendMessage(p);
      } on Object catch (e) {
        failedSource = backendMessage(e);
      }
      if (!mounted || request != generation) return;
      sources = readSources;
      sourceError = failedSource;
      final people = {
        for (final row in [...maps(value['residentAssessments']), ...maps(value['items'])])
          if (row['residentId'] is int) row['residentId'] as int,
      };
      for (final id in people) {
        try {
          final person = await widget.api.get('/residents/$id');
          if (mounted && request == generation && person?['fullName'] is String) {
            names[id] = person!['fullName'] as String;
          }
        } on Object {
          // Backend giới hạn tên theo quyền; không lấy tên từ nguồn khác.
        }
      }
    } on ApiUnreachable {
      final cached = selected == null ? null : await widget.cache?.read(selected!);
      if (mounted && request == generation) {
        offline = cached;
        record = cached?.record;
        error = cached == null ? 'Không kết nối được máy chủ và máy không có bản lưu của ca này.' : null;
      }
    } on Object catch (e) {
      if (e is ApiProblem && [401, 403, 404].contains(e.status) && selected != null) {
        await widget.cache?.remove(selected!);
      }
      if (mounted && request == generation) {
        error = backendMessage(e);
        shift = null;
        record = null;
        sources = [];
        names = {};
        if (e is ApiProblem && [401, 403, 404].contains(e.status)) {
          comment.clear();
          replies.clear();
          keys.clear();
          shifts = [];
        }
      }
    } finally {
      if (mounted && request == generation) setState(() => loading = false);
    }
  }

  Future<void> act(String command, {Map<String, dynamic> body = const {}}) async {
    final canCreate = (shift?['allowedCommands'] as List<dynamic>? ?? []).contains('createHandover');
    if (busy || loading || (command == 'create' ? !canCreate : !allowed(command))) return;
    final path = command == 'create'
        ? '/shifts/$selected/handovers'
        : '/handovers/${record!['id']}/${{'comment': 'comments', 'supplementAndResubmit': 'supplement-and-resubmit'}[command] ?? command}';
    if (command == 'complete' || command == 'confirm') {
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(command == 'confirm' ? 'Xác nhận bàn giao' : 'Hoàn tất bàn giao'),
          content: const Text('Bạn đã kiểm tra đầy đủ nội dung bàn giao?'),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Quay lại')),
            FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Tiếp tục')),
          ],
        ),
      );
      if (confirmed != true || !mounted) return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      final key = keys.putIfAbsent(jsonEncode([path, body]), newIdempotencyKey);
      await widget.api.post(path, body, headers: {'Idempotency-Key': key});
      if (!mounted) return;
      comment.clear();
      replies.clear();
      keys.clear();
      await load();
    } on Object catch (e) {
      if (!mounted) return;
      setState(() {
        error = backendMessage(e);
        if (e is ApiProblem && [401, 403, 404].contains(e.status)) {
          record = null;
          shift = null;
          sources = [];
          names = {};
          comment.clear();
          replies.clear();
          keys.clear();
        }
      });
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  /// Lệnh sửa Bản nháp hoặc thêm phát sinh sau khi lập (FR-040, FR-041): gửi If-Match theo ETag; 412 thì tải lại.
  Future<void> edit(String method, String path, String done, {Object? body}) async {
    if (busy || loading || etag == null) return;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      final key = keys.putIfAbsent(jsonEncode([method, path, body]), newIdempotencyKey);
      await widget.api.requestJson(
        method,
        path,
        body: body,
        headers: {'If-Match': etag!, if (method != 'DELETE') 'Idempotency-Key': key},
      );
      keys.clear();
      await load();
      if (mounted) setState(() => error = done);
    } on ApiProblem catch (e) {
      if (!mounted) return;
      if (e.status == 412) {
        await load();
        if (mounted) setState(() => error = 'Bàn giao vừa được người khác sửa; đã tải lại, hãy làm lại thao tác.');
      } else {
        setState(() => error = backendMessage(e));
      }
    } on Object catch (e) {
      if (mounted) setState(() => error = backendMessage(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final comments = maps(record?['comments']);
    return PageBody(
      child: ListView(
        padding: EdgeInsets.zero,
        children: [
          if (widget.initialShiftId == null) ...[
            Text('Tháng ${month.month}/${month.year}', style: text.titleLarge),
            Wrap(
              spacing: 8,
              children: [
                TextButton(onPressed: busy || loading ? null : () => load(move: -1), child: const Text('Tháng trước')),
                TextButton(onPressed: busy || loading ? null : () => load(move: 1), child: const Text('Tháng sau')),
              ],
            ),
            DropdownButtonFormField<int>(
              key: ValueKey('shift:$selected:${day(month)}'),
              initialValue: shifts.any((row) => row['id'] == selected) ? selected : null,
              isExpanded: true,
              isDense: false,
              itemHeight: null,
              decoration: const InputDecoration(labelText: 'Chọn ca bàn giao'),
              items: [
                for (final row in shifts)
                  DropdownMenuItem(
                    value: row['id'] as int,
                    child: Text(formatDateTime(DateTime.parse(row['startsAt'] as String))),
                  ),
              ],
              onChanged: busy || loading ? null : (id) => id == null ? null : load(shiftId: id),
            ),
          ],
          OutlinedButton(onPressed: busy || loading ? null : load, child: const Text('Tải lại')),
          if (loading) const Center(child: CircularProgressIndicator()),
          if (error != null) Semantics(liveRegion: true, child: Text(error!)),
          if (!loading && error == null && record == null)
            Text(selected == null ? 'Chọn ca để xem bàn giao.' : 'Chưa có bàn giao được phép xem ở ca này.'),
          if (!loading &&
              record == null &&
              (shift?['allowedCommands'] as List<dynamic>? ?? []).contains('createHandover'))
            FilledButton(onPressed: busy ? null : () => act('create'), child: const Text('Lập bàn giao')),
          if (!loading && record != null) ...[
            if (offline != null)
              Semantics(
                liveRegion: true,
                child: Text(
                  'Bản lưu trên máy lúc ${formatDateTime(offline!.savedAt)}, chỉ xem khi mất mạng; '
                  'tự xóa lúc ${formatDateTime(offline!.scopeEndsAt)} khi hết phạm vi ca.',
                ),
              ),
            const SizedBox(height: 16),
            StatusPill(
              label: _states[record!['status']] ?? 'Chưa đọc được trạng thái',
              tone: record!['status'] == 'CONFIRMED' ? StatusTone.settled : StatusTone.neutral,
            ),
            if (record!['finalizedAt'] is String)
              Text('Lập lúc ${formatDateTime(DateTime.parse(record!['finalizedAt'] as String))}'),
            if (record!['confirmedAt'] is String)
              Text('Xác nhận lúc ${formatDateTime(DateTime.parse(record!['confirmedAt'] as String))}'),
            const SizedBox(height: 16),
            Text('Nhận định chung', style: text.titleMedium),
            if (allowed('assess')) ...[
              TextField(
                controller: assessment,
                decoration: const InputDecoration(labelText: 'Nhận định chung'),
                maxLines: 4,
                enabled: !busy,
              ),
              OutlinedButton(
                onPressed: busy
                    ? null
                    : () => edit(
                        'PUT',
                        '/handovers/${record!['id']}/assessments',
                        'Đã lưu nhận định',
                        body: {
                          'general': assessment.text.trim(),
                          'byResident': [
                            for (final a in maps(record!['residentAssessments']))
                              {'residentId': a['residentId'], 'content': a['content']},
                          ],
                        },
                      ),
                child: const Text('Lưu nhận định'),
              ),
            ] else
              Text((record!['generalAssessment'] as String?) ?? 'Chưa có nội dung được phép xem.'),
            for (final assessment in maps(record!['residentAssessments'])) ...[
              const SizedBox(height: 16),
              Text(
                names[assessment['residentId']] ?? 'Người cao tuổi: chưa có tên được phép xem',
                style: text.titleMedium,
              ),
              Text((assessment['content'] as String?) ?? 'Nội dung giới hạn theo quyền xem.'),
            ],
            const SizedBox(height: 16),
            Text('Nội dung bàn giao', style: text.titleLarge),
            for (final item in maps(record!['items']))
              ColoredBox(
                color: Theme.of(context).colorScheme.surface,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(_groups[item['group']] ?? 'Nội dung theo dõi', style: text.titleMedium),
                      if (item['section'] == 'AFTER_PREPARATION') const Text('Phát sinh sau khi lập'),
                      if (item['section'] == 'RESOLVED_DURING_SHIFT') const Text('Đã xử lý trong ca'),
                      if (item['residentId'] != null)
                        Text(names[item['residentId']] ?? 'Người cao tuổi: chưa có tên được phép xem'),
                      Text((item['description'] as String?) ?? 'Nội dung giới hạn theo quyền xem.'),
                      if (item['note'] is String) Text('Ghi chú: ${item['note']}'),
                      if (item['noteAuthor'] is String) Text('Người ghi: ${item['noteAuthor']}'),
                      if (item['resolved'] == true) const Text('Đã xử lý'),
                      if (allowed('deleteItem') && item['id'] is int)
                        Align(
                          alignment: Alignment.centerLeft,
                          child: TextButton(
                            onPressed: busy
                                ? null
                                : () => edit('DELETE', '/handovers/${record!['id']}/items/${item['id']}', 'Đã bỏ mục'),
                            child: const Text('Bỏ mục'),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            if (sourceError != null) Text('Chưa đọc được nguồn tham khảo: $sourceError'),
            if (sources.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text('Nguồn tham khảo hiện hành', style: text.titleMedium),
              for (final source in sources)
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        source['dataUnavailable'] == true
                            ? 'Chưa đọc được nguồn.'
                            : (source['description'] as String?) ?? 'Nội dung giới hạn theo quyền xem.',
                      ),
                    ),
                    if (allowed('addItem') && source['dataUnavailable'] != true && source['description'] is String)
                      TextButton(
                        onPressed: busy
                            ? null
                            : () => edit(
                                'POST',
                                '/handovers/${record!['id']}/items',
                                'Đã thêm mục',
                                body: {
                                  'group': source['group'],
                                  'sourceType': source['sourceType'],
                                  'sourceId': source['sourceId'],
                                  if (source['residentId'] != null) 'residentId': source['residentId'],
                                  if (source['roomId'] != null) 'roomId': source['roomId'],
                                  'description': source['description'],
                                },
                              ),
                        child: const Text('Thêm'),
                      ),
                  ],
                ),
            ],
            for (final row in comments) ...[
              const SizedBox(height: 16),
              Text((row['sender'] as String?) ?? 'Ý kiến bàn giao', style: text.titleMedium),
              Text((row['content'] as String?) ?? ''),
              for (final reply in maps(row['replies'])) Text((reply['content'] as String?) ?? ''),
              if (allowed('supplementAndResubmit'))
                TextFormField(
                  key: ValueKey('reply:${row['id']}'),
                  initialValue: replies[row['id']],
                  decoration: const InputDecoration(labelText: 'Phản hồi ý kiến'),
                  maxLines: 3,
                  enabled: !busy,
                  onChanged: (value) => setState(() => replies[row['id'] as int] = value),
                ),
            ],
            if (allowed('comment')) ...[
              const SizedBox(height: 16),
              TextField(
                controller: comment,
                decoration: const InputDecoration(labelText: 'Ý kiến bàn giao'),
                maxLines: 3,
                enabled: !busy,
                onChanged: (_) => setState(() {}),
              ),
              FilledButton(
                onPressed: busy || comment.text.trim().isEmpty
                    ? null
                    : () => act('comment', body: {'content': comment.text.trim()}),
                child: const Text('Gửi ý kiến'),
              ),
            ],
            if (allowed('supplementAndResubmit'))
              FilledButton(
                onPressed: busy || comments.any((row) => (replies[row['id']] ?? '').trim().isEmpty)
                    ? null
                    : () => act(
                        'supplementAndResubmit',
                        body: {
                          'replies': [
                            for (final row in comments) {'commentId': row['id'], 'content': replies[row['id']]!.trim()},
                          ],
                        },
                      ),
                child: const Text('Bổ sung và gửi lại'),
              ),
            if (allowed('complete'))
              FilledButton(onPressed: busy ? null : () => act('complete'), child: const Text('Hoàn tất bàn giao')),
            if (allowed('confirm'))
              FilledButton(onPressed: busy ? null : () => act('confirm'), child: const Text('Xác nhận bàn giao')),
          ],
        ],
      ),
    );
  }
}
