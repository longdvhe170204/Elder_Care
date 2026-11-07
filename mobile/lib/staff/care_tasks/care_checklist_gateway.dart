import '../../common/api/api_client.dart';

abstract interface class CareChecklistGateway {
  Future<Map<String, dynamic>> load(int shiftId);
  Future<void> notPerform(int taskId, int reasonTypeId, String? note, String key);
  Future<void> claim(int taskId, String key);
  Future<void> record(int taskId, Map<String, dynamic> body, String key);
}

/// Chỉ operation trả object/không body; không tự viết transport hoặc bỏ qua client common.
class ApiCareChecklistGateway implements CareChecklistGateway {
  ApiCareChecklistGateway(this.api);
  final ApiClient api;
  @override
  Future<Map<String, dynamic>> load(int shiftId) async {
    final data = await api.get('/checklist?shiftId=$shiftId');
    if (data == null) throw const FormatException('Checklist thiếu dữ liệu.');
    data['resultValues'] = await api.getList('/result-values');
    return data;
  }

  @override
  Future<void> notPerform(int taskId, int reasonTypeId, String? note, String key) async {
    await api.post(
      '/care-tasks/$taskId/not-perform',
      {'reasonTypeId': reasonTypeId, if (note != null && note.isNotEmpty) 'note': note},
      headers: {'Idempotency-Key': key},
    );
  }

  @override
  Future<void> claim(int taskId, String key) async {
    await api.post('/care-tasks/$taskId/claim', {}, headers: {'Idempotency-Key': key});
  }

  @override
  Future<void> record(int taskId, Map<String, dynamic> body, String key) async {
    await api.post('/care-tasks/$taskId/record', body, headers: {'Idempotency-Key': key});
  }
}
