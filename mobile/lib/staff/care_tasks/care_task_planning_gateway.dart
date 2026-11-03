/// Nguồn trực tuyến của màn T026; adapter gọi client chung sau khi phần nền hỗ trợ JSON array.
abstract interface class CareTaskPlanningGateway {
  Future<List<Map<String, dynamic>>> shifts();
  Future<CareTaskPlanningData> load(int shiftId);
  Future<void> plan(int shiftId, List<Map<String, dynamic>> rows, String idempotencyKey);
}

class CareTaskPlanningData {
  const CareTaskPlanningData({
    required this.source,
    required this.residents,
    required this.taskTypes,
    required this.tasks,
    required this.canCreate,
    this.shiftMembers = const [],
  });
  final Map<String, dynamic> source;
  final List<Map<String, dynamic>> residents;
  final List<Map<String, dynamic>> taskTypes;
  final List<Map<String, dynamic>> tasks;
  final bool canCreate;

  /// Danh sách staff của GET /shifts/{id} đã có ở 008; không giả là phân công theo cư dân.
  final List<Map<String, dynamic>> shiftMembers;
}

class CareTaskPlanningError implements Exception {
  const CareTaskPlanningError(this.message, {this.rowErrors = const []});
  final String message;
  final List<Map<String, dynamic>> rowErrors;
}
