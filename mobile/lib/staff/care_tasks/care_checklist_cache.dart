import 'dart:convert';

import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';

part 'care_checklist_cache.g.dart';

/// Cache kỹ thuật theo tài khoản/ca; không dùng bảng hàng đợi nghiệp vụ của common.
class CareChecklistSnapshots extends Table {
  IntColumn get accountId => integer()();
  IntColumn get shiftId => integer()();
  TextColumn get content => text()();
  DateTimeColumn get loadedAt => dateTime()();
  @override
  Set<Column<Object>> get primaryKey => {accountId, shiftId};
}

class CachedCareChecklist {
  const CachedCareChecklist(this.content, this.loadedAt);
  final Map<String, dynamic> content;
  final DateTime loadedAt;
}

@DriftDatabase(tables: [CareChecklistSnapshots])
class CareChecklistCache extends _$CareChecklistCache {
  CareChecklistCache(super.executor);
  factory CareChecklistCache.onDevice() => CareChecklistCache(driftDatabase(name: 'care_checklist_cache'));
  @override
  int get schemaVersion => 1;
  Future<List<int>> shiftIds(int accountId) async => (await (select(
    careChecklistSnapshots,
  )..where((r) => r.accountId.equals(accountId))).get()).map((row) => row.shiftId).toList();
  Future<void> save(int accountId, int shiftId, Map<String, dynamic> content) async {
    final loaded = DateTime.parse(content['loadedAt'] as String);
    await into(careChecklistSnapshots).insertOnConflictUpdate(
      CareChecklistSnapshotsCompanion.insert(
        accountId: accountId,
        shiftId: shiftId,
        content: jsonEncode(content),
        loadedAt: loaded,
      ),
    );
  }

  Future<CachedCareChecklist?> read(int accountId, int shiftId) async {
    final row = await (select(
      careChecklistSnapshots,
    )..where((r) => r.accountId.equals(accountId) & r.shiftId.equals(shiftId))).getSingleOrNull();
    if (row == null) return null;
    return CachedCareChecklist(
      (jsonDecode(row.content) as Map).cast<String, dynamic>(),
      DateTime.parse((jsonDecode(row.content) as Map)['loadedAt'] as String),
    );
  }

  Future<void> remove(int accountId, int shiftId) =>
      (delete(careChecklistSnapshots)..where((r) => r.accountId.equals(accountId) & r.shiftId.equals(shiftId))).go();
  Future<void> clearAccount(int accountId) =>
      (delete(careChecklistSnapshots)..where((r) => r.accountId.equals(accountId))).go();
}
