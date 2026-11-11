import 'dart:convert';

import '../../common/auth/secure_store.dart';

/// Bàn giao lưu trên máy để xem khi mất mạng (008 T030, FR-060; 002 FR-032, FR-033). Chỉ lưu khi biết hạn phạm vi của
/// người dùng ở ca ([scopeEndsAt] của `GET /me/shifts`, máy chủ tính kể cả kết thúc sớm và CFG-M15-07); không tự cộng
/// giờ, không dùng giờ kết ca. Khóa theo tài khoản nên tài khoản khác trên cùng máy không đọc được; bản quá hạn bị xóa ở
/// mỗi lần mở màn và khi đọc.
class HandoverCache {
  HandoverCache(this.store, this.accountId, {DateTime Function()? now}) : _now = now ?? DateTime.now;

  final SecureStore store;
  final int accountId;
  final DateTime Function() _now;

  String get _index => 'handover.index.$accountId';
  String _key(int shiftId) => 'handover.$accountId.$shiftId';

  Future<List<int>> _shifts() async {
    final raw = await store.read(_index);
    return raw == null ? [] : (jsonDecode(raw) as List<dynamic>).cast<int>();
  }

  Future<void> save(int shiftId, Map<String, dynamic> record, DateTime scopeEndsAt) async {
    if (!scopeEndsAt.isAfter(_now())) return;
    await store.write(
      _key(shiftId),
      jsonEncode({
        'record': record,
        'scopeEndsAt': scopeEndsAt.toUtc().toIso8601String(),
        'savedAt': _now().toUtc().toIso8601String(),
      }),
    );
    final shifts = await _shifts();
    if (!shifts.contains(shiftId)) await store.write(_index, jsonEncode([...shifts, shiftId]));
  }

  /// Bản lưu còn hạn của ca; quá hạn thì xóa và trả rỗng.
  Future<CachedHandover?> read(int shiftId) async {
    final raw = await store.read(_key(shiftId));
    if (raw == null) return null;
    final json = jsonDecode(raw) as Map<String, dynamic>;
    final endsAt = DateTime.parse(json['scopeEndsAt'] as String);
    if (!endsAt.isAfter(_now())) {
      await remove(shiftId);
      return null;
    }
    return CachedHandover(
      (json['record'] as Map).cast<String, dynamic>(),
      DateTime.parse(json['savedAt'] as String),
      endsAt,
    );
  }

  Future<void> remove(int shiftId) async {
    await store.delete(_key(shiftId));
    final shifts = await _shifts();
    if (shifts.contains(shiftId)) await store.write(_index, jsonEncode(shifts.where((s) => s != shiftId).toList()));
  }

  /// Xóa mọi bản lưu đã quá hạn của tài khoản.
  Future<void> purgeExpired() async {
    for (final shiftId in await _shifts()) {
      await read(shiftId);
    }
  }
}

class CachedHandover {
  CachedHandover(this.record, this.savedAt, this.scopeEndsAt);

  final Map<String, dynamic> record;
  final DateTime savedAt;
  final DateTime scopeEndsAt;
}
