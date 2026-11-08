import '../../common/api/api_client.dart';

/// Ba lệnh ghi kết quả liều của api-006 (`/medication-doses/{doseKey}/administer|refuse|skip`).
enum DoseOutcome {
  administered('administer', 'ADMINISTERED', 'Đã dùng'),
  refused('refuse', 'REFUSED', 'Từ chối'),
  notAdministered('skip', 'NOT_ADMINISTERED', 'Không thực hiện');

  const DoseOutcome(this.command, this.resultType, this.label);

  /// Đoạn cuối đường dẫn lệnh trực tuyến.
  final String command;

  /// `resultType` của bản ghi ngoại tuyến `M07.DOSE_RESULT` (backend `DoseResultOfflineContent`).
  final String resultType;
  final String label;
}

/// Các operation app nhân viên dùng cho vòng phát thuốc (api-006 tag `dose`, api-001 `GET /residents/{id}`).
abstract interface class MedicationRoundGateway {
  Future<List<Map<String, dynamic>>> rounds();
  Future<Map<String, dynamic>> prescription(int id);
  Future<Map<String, dynamic>> medication(int id);
  Future<Map<String, dynamic>> resident(int id);
  Future<void> record(String doseKey, DoseOutcome outcome, Map<String, dynamic> body, String key);
  Future<List<Map<String, dynamic>>> pendingReviews();
  Future<void> reviewResult(int id, String result, String? note, String key);
}

class ApiMedicationRoundGateway implements MedicationRoundGateway {
  ApiMedicationRoundGateway(this.api);
  final ApiClient api;

  static List<Map<String, dynamic>> _maps(List<dynamic> list) =>
      list.map((e) => Map<String, dynamic>.from(e as Map)).toList();

  Future<Map<String, dynamic>> _object(String path) async {
    final data = await api.get(path);
    if (data == null) throw FormatException('Thiếu dữ liệu: $path');
    return data;
  }

  @override
  Future<List<Map<String, dynamic>>> rounds() async => _maps(await api.getList('/medication-doses/rounds'));

  @override
  Future<Map<String, dynamic>> prescription(int id) => _object('/prescriptions/$id');

  @override
  Future<Map<String, dynamic>> medication(int id) => _object('/medications/$id');

  @override
  Future<Map<String, dynamic>> resident(int id) => _object('/residents/$id');

  @override
  Future<void> record(String doseKey, DoseOutcome outcome, Map<String, dynamic> body, String key) async {
    await api.post('/medication-doses/$doseKey/${outcome.command}', body, headers: {'Idempotency-Key': key});
  }

  @override
  Future<List<Map<String, dynamic>>> pendingReviews() async =>
      _maps(await api.getList('/dose-reviews?status=PENDING_REVIEW'));

  @override
  Future<void> reviewResult(int id, String result, String? note, String key) async {
    await api.post(
      '/dose-reviews/$id/result',
      {'result': result, if (note != null && note.isNotEmpty) 'note': note},
      headers: {'Idempotency-Key': key},
    );
  }
}
