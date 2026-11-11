import 'dart:convert';

import '../../common/api/api_client.dart';
import '../../common/offline/offline_queue.dart';

/// T040/BR-M15-20: lưu phong bì trước khi gửi, luôn gửi lại cùng khóa khi mất phản hồi.
class CareOfflineRecording {
  CareOfflineRecording(this.api, this.queue, this.accountId);
  final ApiClient api;
  final OfflineQueue queue;
  final int accountId;
  final Map<String, String> _persistedKeys = {};

  Future<Set<int>> pendingTaskIds() async => {
    for (final row in await queue.unsynced(accountId))
      if (row.recordType == 'M04.CARE_TASK_RESULT') (jsonDecode(row.content) as Map<String, dynamic>)['taskId'] as int,
  };

  Future<bool> recordTask(int taskId, Map<String, dynamic> recording, String key) =>
      _send('M04.CARE_TASK_RESULT', {'taskId': taskId, 'action': 'RECORD', 'recording': recording}, key);

  Future<bool> notPerform(int taskId, int reasonTypeId, String? note, String key) {
    final body = {'reasonTypeId': reasonTypeId, if (note != null && note.isNotEmpty) 'note': note};
    return _send('M04.CARE_TASK_RESULT', {'taskId': taskId, 'action': 'NOT_PERFORM', ...body}, key);
  }

  Future<bool> recordAdHoc(Map<String, dynamic> body, String key) => _send('M04.AD_HOC_RECORDING', body, key);

  /// 014 T014: điểm danh buổi (FR-027 → FR-029), cùng phong bì lưu trước khi gửi như kết quả công việc.
  Future<bool> recordSessionAttendance(int sessionId, List<Map<String, dynamic>> results, String key) =>
      _send('M04.SESSION_ATTENDANCE', {'sessionId': sessionId, 'results': results}, key);

  /// 014 T018 (Q24): điểm danh rời viện (`M04.TRIP_DEPARTURE`) hoặc về (`M04.TRIP_RETURN`) của chuyến, cùng thân lệnh.
  Future<bool> recordTripAttendance(bool returning, int tripId, List<int> residentIds, String? reason, String key) =>
      _send(returning ? 'M04.TRIP_RETURN' : 'M04.TRIP_DEPARTURE', {
        'tripId': tripId,
        'residentIds': residentIds,
        if (!returning && reason != null && reason.isNotEmpty) 'reason': reason,
      }, key);

  Future<bool> _send(String type, Map<String, dynamic> content, String key) async {
    final persisted = _persistedKeys[key] ??= await queue.enqueue(
      accountId: accountId,
      recordType: type,
      content: content,
    );
    try {
      final report = await queue.sync(accountId);
      if (report.sessionEnded) throw ApiProblem(401, 'SESSION_ENDED', 'Phiên đã kết thúc. Bản ghi được giữ trên máy.');
      final outcomes = await queue.synced(accountId);
      final completed = outcomes.where((row) => row.idempotencyKey == persisted).firstOrNull;
      if (completed == null) return true;
      if (completed.result == 'REJECTED' || completed.result == 'PENDING_REVIEW') {
        throw ApiProblem(
          409,
          completed.result,
          completed.reason ?? 'Bản ghi đang chờ xem lại. Nội dung được giữ trong màn Đồng bộ.',
        );
      }
      return false;
    } on ApiUnreachable {
      return true;
    }
  }
}
