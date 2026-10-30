import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';

part 'offline_database.g.dart';

/// Bản ghi ghi khi mất mạng của phần nhân viên (000 R-13, T100; 002 research A-11): phong bì đồng bộ và kết quả
/// backend trả cho từng bản ghi (BR-M15-20). Kết quả rỗng là chưa đồng bộ.
class QueuedRecords extends Table {
  IntColumn get id => integer().autoIncrement()();

  /// `Idempotency-Key` của bản ghi (UUID do app sinh); gửi lại cùng khóa không tạo bản ghi thứ hai.
  TextColumn get idempotencyKey => text().unique()();

  /// Tài khoản đã ghi; mọi bản ghi mang người thực hiện là tài khoản của phiên ghi (002 FR-014a).
  IntColumn get accountId => integer()();

  /// Mã loại bản ghi của module (`OfflineHandler.recordType`, ví dụ `M07.KET_QUA_LIEU`).
  TextColumn get recordType => text()();

  /// Thời điểm trên thiết bị lúc ghi (`thoiDiemThietBi`).
  DateTimeColumn get deviceTime => dateTime()();

  /// Lần kết nối backend cuối mà app biết lúc ghi; rỗng khi chưa biết (mở app khi đã mất mạng).
  DateTimeColumn get connectedBefore => dateTime().nullable()();

  /// Nội dung bản ghi (JSON) theo hợp đồng của module.
  TextColumn get content => text()();

  /// `ACCEPTED`, `ACCEPTED_WITH_FLAG`, `PENDING_REVIEW`, `REJECTED`; rỗng khi chưa đồng bộ.
  TextColumn get result => text().nullable()();
  TextColumn get reason => text().nullable()();
  TextColumn get reference => text().nullable()();
  DateTimeColumn get syncedAt => dateTime().nullable()();
}

@DriftDatabase(tables: [QueuedRecords])
class OfflineDatabase extends _$OfflineDatabase {
  OfflineDatabase(super.executor);

  /// Cơ sở dữ liệu trên máy (SQLite qua `drift_flutter`).
  factory OfflineDatabase.onDevice() => OfflineDatabase(driftDatabase(name: 'offline_queue'));

  @override
  int get schemaVersion => 1;
}
