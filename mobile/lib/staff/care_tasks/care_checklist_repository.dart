import '../../common/api/api_client.dart';
import 'care_checklist_cache.dart';
import 'care_checklist_gateway.dart';

class CareChecklistLoad {
  const CareChecklistLoad(this.snapshot, {required this.offline});
  final CachedCareChecklist snapshot;
  final bool offline;
}

/// Chỉ dùng cache khi mất kết nối; 403 không được phục hồi nội dung đã mất quyền.
class CareChecklistRepository {
  CareChecklistRepository(this.gateway, this.cache);
  final CareChecklistGateway gateway;
  final CareChecklistCache cache;
  Future<CareChecklistLoad> load(int accountId, int shiftId) async {
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
