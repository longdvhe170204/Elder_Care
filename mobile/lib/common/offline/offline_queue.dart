import 'dart:convert';
import 'dart:math';

import 'package:drift/drift.dart';

import '../api/api_client.dart';
import 'offline_database.dart';

/// Kết quả đồng bộ từng bản ghi (000 `SyncResult`; BR-M15-20).
enum SyncResult {
  accepted('ACCEPTED'),
  acceptedWithFlag('ACCEPTED_WITH_FLAG'),
  pendingReview('PENDING_REVIEW'),
  rejected('REJECTED');

  const SyncResult(this.code);

  final String code;

  static SyncResult of(String code) => values.firstWhere((r) => r.code == code);
}

class RecordOutcome {
  const RecordOutcome({required this.idempotencyKey, required this.result, this.reason, this.reference});

  final String idempotencyKey;
  final SyncResult result;
  final String? reason;
  final String? reference;
}

/// Phản hồi của một lần đồng bộ (api-002 `POST /sync`).
class SyncReport {
  const SyncReport({this.results = const [], this.sessionEnded = false, this.residentsInScope});

  final List<RecordOutcome> results;

  /// Tài khoản không còn Hoạt động: app kết thúc phiên (002 FR-013).
  final bool sessionEnded;

  /// Người cao tuổi còn trong phạm vi lúc đồng bộ; app chặn ghi mới cho người ngoài danh sách (CHK057).
  final List<int>? residentsInScope;
}

/// Hàng đợi ngoại tuyến dùng chung của phần nhân viên (000 T100, R-13; 002 research A-11). Module gọi [enqueue] khi
/// ghi nhận lúc mất mạng (Q-01, chỉ "ghi nhận thực hiện"), rồi [sync] khi có mạng. Thao tác bắt buộc trực tuyến không
/// đi qua hàng đợi.
class OfflineQueue {
  OfflineQueue(this._db, this._api, {DateTime Function()? clock, String Function()? newKey})
    : _clock = clock ?? DateTime.now,
      _newKey = newKey ?? _uuidV4;

  final OfflineDatabase _db;
  final ApiClient _api;
  final DateTime Function() _clock;
  final String Function() _newKey;

  /// Lưu một bản ghi chờ đồng bộ; trả `Idempotency-Key` của bản ghi.
  Future<String> enqueue({
    required int accountId,
    required String recordType,
    required Map<String, dynamic> content,
  }) async {
    final key = _newKey();
    await _db
        .into(_db.queuedRecords)
        .insert(
          QueuedRecordsCompanion.insert(
            idempotencyKey: key,
            accountId: accountId,
            recordType: recordType,
            deviceTime: _clock().toUtc(),
            connectedBefore: Value(_api.lastConnectedAt?.toUtc()),
            content: jsonEncode(content),
          ),
        );
    return key;
  }

  /// Bản ghi của tài khoản chưa có kết quả đồng bộ, theo thứ tự ghi.
  Future<List<QueuedRecord>> unsynced(int accountId) =>
      (_db.select(_db.queuedRecords)
            ..where((r) => r.accountId.equals(accountId) & r.result.isNull())
            ..orderBy([(r) => OrderingTerm.asc(r.id)]))
          .get();

  /// Bản ghi đã có kết quả đồng bộ, mới nhất trước (màn kết quả đồng bộ, 002 T060).
  Future<List<QueuedRecord>> synced(int accountId) =>
      (_db.select(_db.queuedRecords)
            ..where((r) => r.accountId.equals(accountId) & r.result.isNotNull())
            ..orderBy([(r) => OrderingTerm.asc(r.id)]))
          .get();

  /// Gửi một lô các bản ghi chưa đồng bộ của tài khoản. Mất mạng thì ném [ApiUnreachable], bản ghi giữ nguyên; gửi lại
  /// an toàn vì backend chống lặp theo `Idempotency-Key`.
  Future<SyncReport> sync(int accountId) async {
    final waiting = await unsynced(accountId);
    if (waiting.isEmpty) return const SyncReport();
    final response = await _api.post('/sync', {
      'lastConnectedAt': _lastConnectedAt(waiting).toIso8601String(),
      'records': [
        for (final r in waiting)
          {
            'idempotencyKey': r.idempotencyKey,
            'recordType': r.recordType,
            'deviceTime': r.deviceTime.toUtc().toIso8601String(),
            'content': jsonDecode(r.content),
          },
      ],
    });
    final body = response ?? const <String, dynamic>{};
    final results = [
      for (final item in (body['results'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>())
        RecordOutcome(
          idempotencyKey: item['idempotencyKey'] as String,
          result: SyncResult.of(item['result'] as String),
          reason: item['reason'] as String?,
          reference: item['reference'] as String?,
        ),
    ];
    final syncedAt = _clock().toUtc();
    await _db.batch((batch) {
      for (final r in results) {
        batch.update(
          _db.queuedRecords,
          QueuedRecordsCompanion(
            result: Value(r.result.code),
            reason: Value(r.reason),
            reference: Value(r.reference),
            syncedAt: Value(syncedAt),
          ),
          where: (t) => t.idempotencyKey.equals(r.idempotencyKey),
        );
      }
    });
    return SyncReport(
      results: results,
      sessionEnded: body['sessionEnded'] == true,
      residentsInScope: (body['residentsInScope'] as List<dynamic>?)?.map((e) => (e as num).toInt()).toList(),
    );
  }

  // Lần kết nối cuối trước bản ghi sớm nhất; chưa biết thì lấy thời điểm thiết bị (backend lấy giá trị nhỏ hơn giữa
  // số app gửi và lần kết nối máy chủ đã thấy, 002 research A-11)
  DateTime _lastConnectedAt(List<QueuedRecord> records) =>
      records.map((r) => (r.connectedBefore ?? r.deviceTime).toUtc()).reduce((a, b) => a.isBefore(b) ? a : b);

  static String _uuidV4() {
    final random = Random.secure();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final hex = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-'
        '${hex.substring(16, 20)}-${hex.substring(20)}';
  }
}
