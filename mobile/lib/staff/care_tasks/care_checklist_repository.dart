import '../../common/api/api_client.dart';
import 'care_checklist_cache.dart';
import 'care_checklist_gateway.dart';
import 'care_offline_recording.dart';

class CareChecklistLoad {
  const CareChecklistLoad(this.snapshot, {required this.offline});
  final CachedCareChecklist snapshot;
  final bool offline;
}

/// Chỉ dùng cache khi mất kết nối; 403 không được phục hồi nội dung đã mất quyền.
class CareChecklistRepository {
  CareChecklistRepository(this.gateway, this.cache, {this.recorder});
  final CareChecklistGateway gateway;
  final CareChecklistCache cache;
  final CareOfflineRecording? recorder;
  bool lastQueued = false;
  Set<int> pendingTaskIds = {};

  Future<void> record(int taskId, Map<String, dynamic> body, String key) async {
    lastQueued = false;
    if (recorder != null) {
      lastQueued = await recorder!.recordTask(taskId, body, key);
      if (lastQueued) pendingTaskIds.add(taskId);
    } else {
      await gateway.record(taskId, body, key);
    }
  }

  Future<void> notPerform(int taskId, int reasonTypeId, String? note, String key) async {
    lastQueued = false;
    if (recorder != null) {
      lastQueued = await recorder!.notPerform(taskId, reasonTypeId, note, key);
      if (lastQueued) pendingTaskIds.add(taskId);
    } else {
      await gateway.notPerform(taskId, reasonTypeId, note, key);
    }
  }

  Future<CareChecklistLoad> load(int accountId, int shiftId) async {
    pendingTaskIds = await recorder?.pendingTaskIds() ?? {};
    try {
      final content = await gateway.load(shiftId);
      await cache.save(accountId, shiftId, content);
      return CareChecklistLoad(
        CachedCareChecklist(content, DateTime.parse(content['loadedAt'] as String)),
        offline: false,
      );
    } on ApiUnreachable {
      final snapshot = await cache.read(accountId, shiftId);
      if (snapshot == null) rethrow;
      return CareChecklistLoad(snapshot, offline: true);
    } on ApiProblem catch (problem) {
      if (problem.status == 403 || problem.status == 404) await cache.remove(accountId, shiftId);
      rethrow;
    }
  }
}
