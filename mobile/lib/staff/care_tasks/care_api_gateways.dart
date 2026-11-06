import '../../common/api/api_client.dart';
import 'care_task_planning_gateway.dart';
import 'day_care_attendance_gateway.dart';

export 'care_task_planning_gateway.dart' show CareTaskPlanningError;

List<Map<String, dynamic>> _rows(List<dynamic> rows) => rows.map((r) => Map<String, dynamic>.from(r as Map)).toList();

Future<bool> _can(ApiClient api, String function, String operation) async => (await api.getList(
  '/me/permissions',
)).any((p) => p is Map && p['function'] == function && p['operation'] == operation);

/// T026: dùng transport chung, giữ lỗi từng dòng và khóa chống lặp của màn.
class ApiCareTaskPlanningGateway implements CareTaskPlanningGateway {
  ApiCareTaskPlanningGateway(this.api);
  final ApiClient api;

  @override
  Future<List<Map<String, dynamic>>> shifts() async {
    final now = DateTime.now().toUtc().add(const Duration(hours: 7));
    final month = '${now.year}-${now.month.toString().padLeft(2, '0')}';
    if (await _can(api, 'SHIFT_SCHEDULE', 'CREATE')) {
      final schedules = await api.getList('/shift-schedules?month=$month');
      return [
        for (final s in schedules.whereType<Map<String, dynamic>>().where((s) => s['status'] == 'PUBLISHED'))
          ..._rows(s['shifts'] as List<dynamic>? ?? []).where((s) => s['status'] != 'CANCELLED'),
      ];
    }
    final next = DateTime.utc(now.year, now.month + 1);
    final until = '${next.year}-${next.month.toString().padLeft(2, '0')}-01';
    return _rows(
      await api.getList('/me/shifts?from=$month-01&to=$until'),
    ).where((s) => !['DRAFT', 'CANCELLED'].contains(s['status'])).toList();
  }

  @override
  Future<CareTaskPlanningData> load(int shiftId) async {
    final source = await api.get('/shifts/$shiftId/source-data');
    final roster = await api.get('/shifts/$shiftId');
    final residents = await api.get('/residents');
    return CareTaskPlanningData(
      source: source ?? {},
      residents: _rows(residents?['items'] as List<dynamic>? ?? []),
      taskTypes: _rows(await api.getList('/task-types')),
      tasks: _rows(await api.getList('/shifts/$shiftId/care-tasks')),
      canCreate: await _can(api, 'SHIFT_TASK_PLANNING', 'CREATE'),
      shiftMembers: _rows(
        roster?['staff'] as List<dynamic>? ?? [],
      ).where((s) => s['absent'] != true && s['approvedLeave'] != true).toList(),
    );
  }

  @override
  Future<void> plan(int shiftId, List<Map<String, dynamic>> rows, String idempotencyKey) async {
    try {
      await api.requestJson(
        'POST',
        '/shifts/$shiftId/care-tasks/plan',
        body: rows,
        headers: {'Idempotency-Key': idempotencyKey},
      );
    } on ApiProblem catch (problem) {
      throw CareTaskPlanningError(
        problem.detail ?? 'Chưa lưu được lô công việc.',
        rowErrors: _rows(problem.properties['rowErrors'] as List<dynamic>? ?? []),
      );
    }
  }
}

/// T031: điểm danh trực tuyến; không đưa lệnh điểm danh vào hàng đợi ghi nhận.
class ApiDayCareAttendanceGateway implements DayCareAttendanceGateway {
  ApiDayCareAttendanceGateway(this.api);
  final ApiClient api;

  @override
  Future<DayCareAttendanceData> load(String date) async {
    final residents = await api.get('/residents');
    return DayCareAttendanceData(
      rows: _rows(await api.getList('/day-care-attendances?date=$date')),
      residents: _rows(residents?['items'] as List<dynamic>? ?? []),
      canCreate: await _can(api, 'DAY_CARE_ATTENDANCE', 'CREATE'),
    );
  }

  @override
  Future<void> checkIn(int residentId, String idempotencyKey) async {
    await api.post(
      '/day-care-attendances/check-in',
      {'residentId': residentId},
      headers: {'Idempotency-Key': idempotencyKey},
    );
  }

  @override
  Future<void> checkOut(int attendanceId, int pickupRecordId, String idempotencyKey) async {
    await api.post(
      '/day-care-attendances/$attendanceId/check-out',
      {'pickupRecordId': pickupRecordId},
      headers: {'Idempotency-Key': idempotencyKey},
    );
  }

  @override
  Future<void> correct(int recordId, String at, String reason, String idempotencyKey) async {
    await api.post(
      '/day-care-attendance-records/$recordId/correct',
      {
        'type': 'AMEND_CONTENT',
        'correctedContent': {'at': at},
        'reason': reason,
      },
      headers: {'Idempotency-Key': idempotencyKey},
    );
  }
}
