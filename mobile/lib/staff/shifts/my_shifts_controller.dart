import 'package:flutter/foundation.dart';

import '../../common/api/api_client.dart';

/// Lịch cá nhân chỉ đọc (008 T016b, FR-033, FR-054); quyền và phạm vi do backend quyết định.
class MyShiftsController extends ChangeNotifier {
  MyShiftsController({required ApiClient api, DateTime? now}) : _api = api {
    final vn = (now ?? DateTime.now()).toUtc().add(const Duration(hours: 7));
    month = DateTime.utc(vn.year, vn.month);
  }

  final ApiClient _api;
  late DateTime month;
  List<Map<String, dynamic>> shifts = [];
  Map<int, String> staffNames = {};
  bool loading = false;
  Object? error;
  int _request = 0;
  bool _disposed = false;

  Future<void> load() async {
    final request = ++_request;
    final from = _date(month);
    final to = _date(DateTime.utc(month.year, month.month + 1));
    loading = true;
    error = null;
    shifts = [];
    staffNames = {};
    notifyListeners();
    try {
      final responses = await Future.wait([_api.getList('/me/shifts?from=$from&to=$to'), _api.getList('/staff')]);
      if (_disposed || request != _request) return;
      final rows = responses[0].cast<Map<String, dynamic>>().toList();
      rows.sort((a, b) => DateTime.parse(a['startsAt'] as String).compareTo(DateTime.parse(b['startsAt'] as String)));
      final names = {
        for (final s in responses[1].cast<Map<String, dynamic>>()) s['id'] as int: s['fullName'] as String,
      };
      shifts = rows;
      staffNames = names;
    } on Object catch (e) {
      if (!_disposed && request == _request) error = e;
    } finally {
      if (!_disposed && request == _request) {
        loading = false;
        notifyListeners();
      }
    }
  }

  Future<void> changeMonth(int offset) {
    month = DateTime.utc(month.year, month.month + offset);
    return load();
  }

  static String _date(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-${value.month.toString().padLeft(2, '0')}-01';

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
