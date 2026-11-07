import 'dart:convert';

import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';

part 'medication_round_cache.g.dart';

/// Bản vòng phát thuốc đã tải gần nhất của từng tài khoản, để xem khi mất mạng kèm dấu "Chưa cập nhật"
/// (006 T028, BR-M04-27 áp cho thuốc). Cache kỹ thuật; bản ghi ngoại tuyến đi qua hàng đợi của common.
class MedicationRoundSnapshots extends Table {
  IntColumn get accountId => integer()();
  TextColumn get content => text()();
  DateTimeColumn get loadedAt => dateTime()();
  @override
  Set<Column<Object>> get primaryKey => {accountId};
}

class CachedMedicationRound {
  const CachedMedicationRound(this.items, this.loadedAt);

  /// Mỗi phần tử là một liều đã ghép (xem `MedicationRoundRepository`).
  final List<Map<String, dynamic>> items;
  final DateTime loadedAt;
}

@DriftDatabase(tables: [MedicationRoundSnapshots])
class MedicationRoundCache extends _$MedicationRoundCache {
  MedicationRoundCache(super.executor);
  factory MedicationRoundCache.onDevice() => MedicationRoundCache(driftDatabase(name: 'medication_round_cache'));
  @override
  int get schemaVersion => 1;

  Future<void> save(int accountId, CachedMedicationRound round) =>
      into(medicationRoundSnapshots).insertOnConflictUpdate(
        MedicationRoundSnapshotsCompanion.insert(
          accountId: Value(accountId),
          content: jsonEncode(round.items),
          loadedAt: round.loadedAt.toUtc(),
        ),
      );

  Future<CachedMedicationRound?> read(int accountId) async {
    final row = await (select(medicationRoundSnapshots)..where((r) => r.accountId.equals(accountId))).getSingleOrNull();
    if (row == null) return null;
    final items = (jsonDecode(row.content) as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
    return CachedMedicationRound(items, row.loadedAt);
  }

  Future<void> remove(int accountId) =>
      (delete(medicationRoundSnapshots)..where((r) => r.accountId.equals(accountId))).go();
}
