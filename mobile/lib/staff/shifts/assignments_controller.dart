import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/notifications/notification_api.dart';

/// T022b: điều phối yêu cầu; mọi quyết định phân công do backend trả.
class AssignmentsController extends ChangeNotifier {
  AssignmentsController(this.api, {DateTime? initialMonth})
    : month = initialMonth ?? DateTime.now().toUtc().add(const Duration(hours: 7));
  final ApiClient api;
  DateTime month;
  bool loading = false, busy = false, manage = false, homeManager = false, view = false;
  String? error;
  List<Map<String, dynamic>> permissions = [], schedules = [], shifts = [], rows = [], staff = [];
  Map<String, dynamic>? shift;
  final Map<String, String> _keys = {};
  int _generation = 0;
  bool _disposed = false;
  void _emit() {
    if (!_disposed) notifyListeners();
  }

  bool can(String operation) =>
      permissions.any((p) => p['function'] == 'SHIFT_SCHEDULE' && p['operation'] == operation);
  bool command(String name) => (shift?['allowedCommands'] as List<dynamic>? ?? []).contains(name);
  String get monthLabel => '${month.month.toString().padLeft(2, '0')}/${month.year}';
  static String date(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Future<void> load({int move = 0}) async {
    final generation = ++_generation;
    month = DateTime(month.year, month.month + move);
    loading = true;
    error = null;
    shift = null;
    rows = [];
    shifts = [];
    schedules = [];
    permissions = [];
    view = false;
    manage = false;
    homeManager = false;
    _emit();
    try {
      final grants = await api.getList('/me/permissions');
      if (generation != _generation || _disposed) return;
      permissions = _maps(grants);
      view = can('VIEW');
      if (!view) throw ApiProblem(403, 'PERMISSION_DENIED', 'Bạn không có quyền xem phân công.');
      final me = await api.get('/me');
      if (generation != _generation || _disposed) return;
      homeManager = (me?['roles'] as List<dynamic>? ?? []).contains('AC-01');
      manage = can('CREATE') || can('UPDATE') || can('APPROVE');
      final current = date(month).substring(0, 7);
      final data = await api.getList(
        manage
            ? '/shift-schedules?month=$current'
            : '/me/shifts?from=${date(month)}&to=${date(DateTime(month.year, month.month + 1))}',
      );
      if (generation != _generation || _disposed) return;
      schedules = manage ? _maps(data) : [];
      shifts = manage ? [for (final s in _maps(data)) ..._maps(s['shifts'])] : _maps(data);
    } on Object catch (e) {
      if (generation == _generation) error = backendMessage(e);
    } finally {
      if (generation == _generation) {
        loading = false;
        _emit();
      }
    }
  }

  static List<Map<String, dynamic>> _maps(Object? data) =>
      (data as List<dynamic>? ?? []).map((e) => e as Map<String, dynamic>).toList();

  Future<void> selectShift(int id) async {
    final generation = ++_generation;
    loading = true;
    error = null;
    shift = null;
    rows = [];
    _emit();
    try {
      final detail = await api.get('/shifts/$id');
      final assignments = await api.getList('/shifts/$id/assignments');
      if (generation != _generation || _disposed) return;
      shift = detail;
      rows = _maps(assignments);
    } on Object catch (e) {
      if (generation == _generation) error = backendMessage(e);
    } finally {
      if (generation == _generation) {
        loading = false;
        _emit();
      }
    }
  }

  Future<List<Map<String, dynamic>>> catalog(String path) async => _maps(await api.getList(path));
  Future<List<Map<String, dynamic>>> residents({String query = ''}) async {
    final page = await api.get('/residents?q=${Uri.encodeQueryComponent(query)}');
    return _maps(page?['items']);
  }

  Future<Map<String, dynamic>?> draft(int id) => api.get('/assignments/$id');
  Future<Map<String, dynamic>?> preview(Map<String, dynamic> body, {Map<String, dynamic>? original}) async {
    return api.post(
      original == null
          ? '/shifts/${shift!['id']}/assignments/preview'
          : '/assignments/${original['id']}/draft-update-preview',
      body,
      headers: original == null ? null : {'If-Match': '"${original['version']}"'},
    );
  }

  Future<bool> save(Map<String, dynamic> body, {Map<String, dynamic>? original}) async {
    if (original == null ? !command('createAssignment') : !command('updateDraftAssignment')) return false;
    final path = original == null
        ? '/shifts/${shift!['id']}/assignments'
        : '/assignments/${original['id']}/update-draft';
    return _write(path, body, version: original?['version'] as int?);
  }

  Future<bool> finish(Map<String, dynamic> row, String kind, {String reason = ''}) async {
    final commandName = {'delete': 'deleteAssignment', 'cancel': 'cancelAssignment', 'end': 'endAssignment'}[kind];
    final state = {'delete': 'DRAFT', 'cancel': 'SCHEDULED', 'end': 'ACTIVE'}[kind];
    if (commandName == null ||
        !command(commandName) ||
        row['status'] != state ||
        (kind != 'delete' && reason.trim().isEmpty)) {
      return false;
    }
    final path = '/assignments/${row['id']}';
    if (kind != 'delete') return _write('$path/$kind', {'reason': reason.trim()});
    return _run(() async {
      await api.requestJson('DELETE', path);
    });
  }

  Future<Map<String, dynamic>?> copy(int source) async {
    if (!command('copyAssignments')) return null;
    Map<String, dynamic>? result;
    final path = '/shifts/${shift!['id']}/copy-assignments';
    final body = <String, dynamic>{'sourceShiftId': source};
    if (!await _run(() async {
      result = await api.post(path, body, headers: {'Idempotency-Key': _key(path, body, null)});
    })) {
      return null;
    }
    return result;
  }

  Future<Map<String, dynamic>?> resolve(int resident, String at, String role, {int? task}) => api.get(
    '/shifts/${shift!['id']}/assignment-resolution?residentId=$resident&at=${Uri.encodeQueryComponent(at)}&role=$role${task == null ? '' : '&taskTypeId=$task'}',
  );
  Future<Map<String, dynamic>?> unassigned() => api.get('/shifts/${shift!['id']}/unassigned-residents');
  Future<List<Map<String, dynamic>>> floorManagers() async => _maps(await api.getList('/floor-manager-assignments'));
  Future<bool> assignFloor(Map<String, dynamic> body) async =>
      homeManager && can('CREATE') && await _write('/floor-manager-assignments', body);
  Future<bool> endFloor(int id, Map<String, dynamic> body) async =>
      homeManager && can('UPDATE') && await _write('/floor-manager-assignments/$id/end', body);
  Future<bool> _write(String path, Map<String, dynamic> body, {int? version}) {
    final value = Map<String, dynamic>.fromEntries(body.entries.toList()..sort((a, b) => a.key.compareTo(b.key)));
    return _run(() async {
      await api.post(
        path,
        value,
        headers: {'Idempotency-Key': _key(path, value, version), if (version != null) 'If-Match': '"$version"'},
      );
    });
  }

  Future<bool> _run(Future<void> Function() call) async {
    if (busy) return false;
    busy = true;
    error = null;
    _emit();
    try {
      await call();
      _keys.clear();
      return true;
    } on Object catch (e) {
      error = backendMessage(e);
      return false;
    } finally {
      busy = false;
      _emit();
    }
  }

  String _key(String path, Map<String, dynamic> body, int? version) =>
      _keys.putIfAbsent(jsonEncode([path, body, version]), newIdempotencyKey);
  @override
  void dispose() {
    _disposed = true;
    _generation++;
    super.dispose();
  }
}
