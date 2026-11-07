import 'dart:convert';

import '../../common/api/api_client.dart';
import '../../common/offline/offline_queue.dart';
import 'medication_round_cache.dart';
import 'medication_round_gateway.dart';

/// Loại bản ghi ngoại tuyến của kết quả liều (backend `DoseResultOfflineHandler.RECORD_TYPE`).
const doseResultRecordType = 'M07.DOSE_RESULT';

/// Liều thuốc kiểm soát đặc biệt không được lưu tạm trên máy (FR-029, BR-M07-20, Q-01).
class RequiresOnline implements Exception {}

class MedicationRoundLoad {
  const MedicationRoundLoad(this.round, {required this.offline, this.queued = const {}});
  final CachedMedicationRound round;
  final bool offline;

  /// doseKey → kết quả đã ghi trên máy, chưa đồng bộ (nhãn "Chưa đồng bộ", DESIGN.md 15.5).
  final Map<String, String> queued;
}

enum RecordSaved { online, queued }

/// Tải vòng phát thuốc và ghép trường hiển thị của FR-020 từ các operation có sẵn (không đổi hợp đồng `Dose`):
/// `Prescription` (liều, đường dùng, hướng dẫn), `Medication` (tên, hàm lượng, đơn vị), hồ sơ 001 (họ tên, dị ứng
/// Hiệu lực). Mất mạng thì trả bản đã tải gần nhất; 403, 404 thì xóa bản đó, không phục hồi nội dung đã mất quyền.
class MedicationRoundRepository {
  MedicationRoundRepository(this.gateway, this.cache, this.queue, {DateTime Function()? clock})
    : clock = clock ?? DateTime.now;

  final MedicationRoundGateway gateway;
  final MedicationRoundCache cache;
  final OfflineQueue queue;
  final DateTime Function() clock;

  Future<MedicationRoundLoad> load(int accountId) async {
    try {
      final round = CachedMedicationRound(await _compose(await gateway.rounds()), clock());
      await cache.save(accountId, round);
      return MedicationRoundLoad(round, offline: false, queued: await _queued(accountId));
    } on ApiUnreachable {
      final cached = await cache.read(accountId);
      if (cached == null) rethrow;
      return MedicationRoundLoad(cached, offline: true, queued: await _queued(accountId));
    } on ApiProblem catch (problem) {
      if (problem.status == 403 || problem.status == 404) await cache.remove(accountId);
      rethrow;
    }
  }

  Future<Map<String, String>> _queued(int accountId) async => {
    for (final record in await queue.unsynced(accountId))
      if (record.recordType == doseResultRecordType)
        (jsonDecode(record.content) as Map)['doseKey'] as String:
            (jsonDecode(record.content) as Map)['resultType'] as String,
  };

  Future<List<Map<String, dynamic>>> _compose(List<Map<String, dynamic>> doses) async {
    final prescriptions = await _byId(doses.map((d) => d['prescriptionId'] as int), gateway.prescription);
    final medications = await _byId(prescriptions.values.map((p) => p['medicationId'] as int), gateway.medication);
    final residents = await _byId(doses.map((d) => d['residentId'] as int), _residentOrNull);
    return [
      for (final dose in doses)
        _item(
          dose,
          prescriptions[dose['prescriptionId']]!,
          medications[prescriptions[dose['prescriptionId']]!['medicationId']]!,
          residents[dose['residentId']],
        ),
    ];
  }

  /// Hồ sơ ngoài phạm vi xem của người dùng vẫn để liều hiện (backend đã cho liều vào vòng), chỉ thiếu họ tên.
  Future<Map<String, dynamic>> _residentOrNull(int id) async {
    try {
      return await gateway.resident(id);
    } on ApiProblem catch (problem) {
      if (problem.status == 403 || problem.status == 404) return const {};
      rethrow;
    }
  }

  static Future<Map<int, Map<String, dynamic>>> _byId(
    Iterable<int> ids,
    Future<Map<String, dynamic>> Function(int) fetch,
  ) async {
    final unique = ids.toSet().toList();
    final values = await Future.wait(unique.map(fetch));
    return {for (var i = 0; i < unique.length; i++) unique[i]: values[i]};
  }

  static Map<String, dynamic> _item(
    Map<String, dynamic> dose,
    Map<String, dynamic> prescription,
    Map<String, dynamic> medication,
    Map<String, dynamic>? resident,
  ) {
    final hidden = (resident?['hiddenFields'] as List? ?? const []).contains('activeHealthItems');
    final healthItems = (resident?['activeHealthItems'] as List? ?? const []).map(
      (e) => Map<String, dynamic>.from(e as Map),
    );
    return {
      'dose': dose,
      'medication': {
        'name': medication['name'],
        'concentration': medication['concentration'],
        'usageUnit': medication['usageUnit'],
      },
      'prescription': {
        'dosage': prescription['dosage'],
        'route': prescription['route'],
        'instructionNote': prescription['instructionNote'],
      },
      'resident': {
        'fullName': (resident?['personalInfo'] as Map?)?['fullName'],
        // null: không được xem phần sức khỏe, màn không hiện dòng dị ứng.
        'allergies': hidden || resident == null || resident.isEmpty
            ? null
            : [
                for (final item in healthItems)
                  if (item['type'] == 'ALLERGY' && item['status'] != 'EXCLUDED')
                    (item['allergen'] as Map?)?['name'] ?? item['freeText'],
              ].whereType<String>().toList(),
      },
    };
  }

  /// Ghi kết quả liều: có mạng thì gọi lệnh trực tuyến; mất mạng thì đưa vào hàng đợi `M07.DOSE_RESULT` (kết quả
  /// đồng bộ từng bản ghi ở màn Đồng bộ của common, BR-M15-20); liều kiểm soát đặc biệt mất mạng thì [RequiresOnline].
  Future<RecordSaved> record({
    required int accountId,
    required Map<String, dynamic> item,
    required DoseOutcome outcome,
    required Map<String, dynamic> body,
    required String key,
  }) async {
    final dose = Map<String, dynamic>.from(item['dose'] as Map);
    try {
      await gateway.record(dose['doseKey'] as String, outcome, body, key);
      return RecordSaved.online;
    } on ApiUnreachable {
      if (dose['requiresOnline'] == true) throw RequiresOnline();
      await queue.enqueue(
        accountId: accountId,
        recordType: doseResultRecordType,
        content: {
          'doseKey': dose['doseKey'],
          'residentId': dose['residentId'],
          'resultType': outcome.resultType,
          'administeredAt': ?body['administeredAt'],
          'reason': ?body['reason'],
          'adverseReaction': ?body['adverseReaction'],
          'note': ?body['note'],
        },
      );
      return RecordSaved.queued;
    }
  }
}
