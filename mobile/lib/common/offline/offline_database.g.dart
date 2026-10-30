// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'offline_database.dart';

// ignore_for_file: type=lint
class $QueuedRecordsTable extends QueuedRecords with TableInfo<$QueuedRecordsTable, QueuedRecord> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $QueuedRecordsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
    'id',
    aliasedName,
    false,
    hasAutoIncrement: true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultConstraints: GeneratedColumn.constraintIsAlways('PRIMARY KEY AUTOINCREMENT'),
  );
  static const VerificationMeta _idempotencyKeyMeta = const VerificationMeta('idempotencyKey');
  @override
  late final GeneratedColumn<String> idempotencyKey = GeneratedColumn<String>(
    'idempotency_key',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
    defaultConstraints: GeneratedColumn.constraintIsAlways('UNIQUE'),
  );
  static const VerificationMeta _accountIdMeta = const VerificationMeta('accountId');
  @override
  late final GeneratedColumn<int> accountId = GeneratedColumn<int>(
    'account_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _recordTypeMeta = const VerificationMeta('recordType');
  @override
  late final GeneratedColumn<String> recordType = GeneratedColumn<String>(
    'record_type',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _deviceTimeMeta = const VerificationMeta('deviceTime');
  @override
  late final GeneratedColumn<DateTime> deviceTime = GeneratedColumn<DateTime>(
    'device_time',
    aliasedName,
    false,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _connectedBeforeMeta = const VerificationMeta('connectedBefore');
  @override
  late final GeneratedColumn<DateTime> connectedBefore = GeneratedColumn<DateTime>(
    'connected_before',
    aliasedName,
    true,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _contentMeta = const VerificationMeta('content');
  @override
  late final GeneratedColumn<String> content = GeneratedColumn<String>(
    'content',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _resultMeta = const VerificationMeta('result');
  @override
  late final GeneratedColumn<String> result = GeneratedColumn<String>(
    'result',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _reasonMeta = const VerificationMeta('reason');
  @override
  late final GeneratedColumn<String> reason = GeneratedColumn<String>(
    'reason',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _referenceMeta = const VerificationMeta('reference');
  @override
  late final GeneratedColumn<String> reference = GeneratedColumn<String>(
    'reference',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _syncedAtMeta = const VerificationMeta('syncedAt');
  @override
  late final GeneratedColumn<DateTime> syncedAt = GeneratedColumn<DateTime>(
    'synced_at',
    aliasedName,
    true,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: false,
  );
  @override
  List<GeneratedColumn> get $columns => [
    id,
    idempotencyKey,
    accountId,
    recordType,
    deviceTime,
    connectedBefore,
    content,
    result,
    reason,
    reference,
    syncedAt,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'queued_records';
  @override
  VerificationContext validateIntegrity(Insertable<QueuedRecord> instance, {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('idempotency_key')) {
      context.handle(
        _idempotencyKeyMeta,
        idempotencyKey.isAcceptableOrUnknown(data['idempotency_key']!, _idempotencyKeyMeta),
      );
    } else if (isInserting) {
      context.missing(_idempotencyKeyMeta);
    }
    if (data.containsKey('account_id')) {
      context.handle(_accountIdMeta, accountId.isAcceptableOrUnknown(data['account_id']!, _accountIdMeta));
    } else if (isInserting) {
      context.missing(_accountIdMeta);
    }
    if (data.containsKey('record_type')) {
      context.handle(_recordTypeMeta, recordType.isAcceptableOrUnknown(data['record_type']!, _recordTypeMeta));
    } else if (isInserting) {
      context.missing(_recordTypeMeta);
    }
    if (data.containsKey('device_time')) {
      context.handle(_deviceTimeMeta, deviceTime.isAcceptableOrUnknown(data['device_time']!, _deviceTimeMeta));
    } else if (isInserting) {
      context.missing(_deviceTimeMeta);
    }
    if (data.containsKey('connected_before')) {
      context.handle(
        _connectedBeforeMeta,
        connectedBefore.isAcceptableOrUnknown(data['connected_before']!, _connectedBeforeMeta),
      );
    }
    if (data.containsKey('content')) {
      context.handle(_contentMeta, content.isAcceptableOrUnknown(data['content']!, _contentMeta));
    } else if (isInserting) {
      context.missing(_contentMeta);
    }
    if (data.containsKey('result')) {
      context.handle(_resultMeta, result.isAcceptableOrUnknown(data['result']!, _resultMeta));
    }
    if (data.containsKey('reason')) {
      context.handle(_reasonMeta, reason.isAcceptableOrUnknown(data['reason']!, _reasonMeta));
    }
    if (data.containsKey('reference')) {
      context.handle(_referenceMeta, reference.isAcceptableOrUnknown(data['reference']!, _referenceMeta));
    }
    if (data.containsKey('synced_at')) {
      context.handle(_syncedAtMeta, syncedAt.isAcceptableOrUnknown(data['synced_at']!, _syncedAtMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  QueuedRecord map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return QueuedRecord(
      id: attachedDatabase.typeMapping.read(DriftSqlType.int, data['${effectivePrefix}id'])!,
      idempotencyKey: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}idempotency_key'],
      )!,
      accountId: attachedDatabase.typeMapping.read(DriftSqlType.int, data['${effectivePrefix}account_id'])!,
      recordType: attachedDatabase.typeMapping.read(DriftSqlType.string, data['${effectivePrefix}record_type'])!,
      deviceTime: attachedDatabase.typeMapping.read(DriftSqlType.dateTime, data['${effectivePrefix}device_time'])!,
      connectedBefore: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}connected_before'],
      ),
      content: attachedDatabase.typeMapping.read(DriftSqlType.string, data['${effectivePrefix}content'])!,
      result: attachedDatabase.typeMapping.read(DriftSqlType.string, data['${effectivePrefix}result']),
      reason: attachedDatabase.typeMapping.read(DriftSqlType.string, data['${effectivePrefix}reason']),
      reference: attachedDatabase.typeMapping.read(DriftSqlType.string, data['${effectivePrefix}reference']),
      syncedAt: attachedDatabase.typeMapping.read(DriftSqlType.dateTime, data['${effectivePrefix}synced_at']),
    );
  }

  @override
  $QueuedRecordsTable createAlias(String alias) {
    return $QueuedRecordsTable(attachedDatabase, alias);
  }
}

class QueuedRecord extends DataClass implements Insertable<QueuedRecord> {
  final int id;

  /// `Idempotency-Key` của bản ghi (UUID do app sinh); gửi lại cùng khóa không tạo bản ghi thứ hai.
  final String idempotencyKey;

  /// Tài khoản đã ghi; mọi bản ghi mang người thực hiện là tài khoản của phiên ghi (002 FR-014a).
  final int accountId;

  /// Mã loại bản ghi của module (`OfflineHandler.recordType`, ví dụ `M07.KET_QUA_LIEU`).
  final String recordType;

  /// Thời điểm trên thiết bị lúc ghi (`thoiDiemThietBi`).
  final DateTime deviceTime;

  /// Lần kết nối backend cuối mà app biết lúc ghi; rỗng khi chưa biết (mở app khi đã mất mạng).
  final DateTime? connectedBefore;

  /// Nội dung bản ghi (JSON) theo hợp đồng của module.
  final String content;

  /// `ACCEPTED`, `ACCEPTED_WITH_FLAG`, `PENDING_REVIEW`, `REJECTED`; rỗng khi chưa đồng bộ.
  final String? result;
  final String? reason;
  final String? reference;
  final DateTime? syncedAt;
  const QueuedRecord({
    required this.id,
    required this.idempotencyKey,
    required this.accountId,
    required this.recordType,
    required this.deviceTime,
    this.connectedBefore,
    required this.content,
    this.result,
    this.reason,
    this.reference,
    this.syncedAt,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['idempotency_key'] = Variable<String>(idempotencyKey);
    map['account_id'] = Variable<int>(accountId);
    map['record_type'] = Variable<String>(recordType);
    map['device_time'] = Variable<DateTime>(deviceTime);
    if (!nullToAbsent || connectedBefore != null) {
      map['connected_before'] = Variable<DateTime>(connectedBefore);
    }
    map['content'] = Variable<String>(content);
    if (!nullToAbsent || result != null) {
      map['result'] = Variable<String>(result);
    }
    if (!nullToAbsent || reason != null) {
      map['reason'] = Variable<String>(reason);
    }
    if (!nullToAbsent || reference != null) {
      map['reference'] = Variable<String>(reference);
    }
    if (!nullToAbsent || syncedAt != null) {
      map['synced_at'] = Variable<DateTime>(syncedAt);
    }
    return map;
  }

  QueuedRecordsCompanion toCompanion(bool nullToAbsent) {
    return QueuedRecordsCompanion(
      id: Value(id),
      idempotencyKey: Value(idempotencyKey),
      accountId: Value(accountId),
      recordType: Value(recordType),
      deviceTime: Value(deviceTime),
      connectedBefore: connectedBefore == null && nullToAbsent ? const Value.absent() : Value(connectedBefore),
      content: Value(content),
      result: result == null && nullToAbsent ? const Value.absent() : Value(result),
      reason: reason == null && nullToAbsent ? const Value.absent() : Value(reason),
      reference: reference == null && nullToAbsent ? const Value.absent() : Value(reference),
      syncedAt: syncedAt == null && nullToAbsent ? const Value.absent() : Value(syncedAt),
    );
  }

  factory QueuedRecord.fromJson(Map<String, dynamic> json, {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return QueuedRecord(
      id: serializer.fromJson<int>(json['id']),
      idempotencyKey: serializer.fromJson<String>(json['idempotencyKey']),
      accountId: serializer.fromJson<int>(json['accountId']),
      recordType: serializer.fromJson<String>(json['recordType']),
      deviceTime: serializer.fromJson<DateTime>(json['deviceTime']),
      connectedBefore: serializer.fromJson<DateTime?>(json['connectedBefore']),
      content: serializer.fromJson<String>(json['content']),
      result: serializer.fromJson<String?>(json['result']),
      reason: serializer.fromJson<String?>(json['reason']),
      reference: serializer.fromJson<String?>(json['reference']),
      syncedAt: serializer.fromJson<DateTime?>(json['syncedAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'idempotencyKey': serializer.toJson<String>(idempotencyKey),
      'accountId': serializer.toJson<int>(accountId),
      'recordType': serializer.toJson<String>(recordType),
      'deviceTime': serializer.toJson<DateTime>(deviceTime),
      'connectedBefore': serializer.toJson<DateTime?>(connectedBefore),
      'content': serializer.toJson<String>(content),
      'result': serializer.toJson<String?>(result),
      'reason': serializer.toJson<String?>(reason),
      'reference': serializer.toJson<String?>(reference),
      'syncedAt': serializer.toJson<DateTime?>(syncedAt),
    };
  }

  QueuedRecord copyWith({
    int? id,
    String? idempotencyKey,
    int? accountId,
    String? recordType,
    DateTime? deviceTime,
    Value<DateTime?> connectedBefore = const Value.absent(),
    String? content,
    Value<String?> result = const Value.absent(),
    Value<String?> reason = const Value.absent(),
    Value<String?> reference = const Value.absent(),
    Value<DateTime?> syncedAt = const Value.absent(),
  }) => QueuedRecord(
    id: id ?? this.id,
    idempotencyKey: idempotencyKey ?? this.idempotencyKey,
    accountId: accountId ?? this.accountId,
    recordType: recordType ?? this.recordType,
    deviceTime: deviceTime ?? this.deviceTime,
    connectedBefore: connectedBefore.present ? connectedBefore.value : this.connectedBefore,
    content: content ?? this.content,
    result: result.present ? result.value : this.result,
    reason: reason.present ? reason.value : this.reason,
    reference: reference.present ? reference.value : this.reference,
    syncedAt: syncedAt.present ? syncedAt.value : this.syncedAt,
  );
  QueuedRecord copyWithCompanion(QueuedRecordsCompanion data) {
    return QueuedRecord(
      id: data.id.present ? data.id.value : this.id,
      idempotencyKey: data.idempotencyKey.present ? data.idempotencyKey.value : this.idempotencyKey,
      accountId: data.accountId.present ? data.accountId.value : this.accountId,
      recordType: data.recordType.present ? data.recordType.value : this.recordType,
      deviceTime: data.deviceTime.present ? data.deviceTime.value : this.deviceTime,
      connectedBefore: data.connectedBefore.present ? data.connectedBefore.value : this.connectedBefore,
      content: data.content.present ? data.content.value : this.content,
      result: data.result.present ? data.result.value : this.result,
      reason: data.reason.present ? data.reason.value : this.reason,
      reference: data.reference.present ? data.reference.value : this.reference,
      syncedAt: data.syncedAt.present ? data.syncedAt.value : this.syncedAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('QueuedRecord(')
          ..write('id: $id, ')
          ..write('idempotencyKey: $idempotencyKey, ')
          ..write('accountId: $accountId, ')
          ..write('recordType: $recordType, ')
          ..write('deviceTime: $deviceTime, ')
          ..write('connectedBefore: $connectedBefore, ')
          ..write('content: $content, ')
          ..write('result: $result, ')
          ..write('reason: $reason, ')
          ..write('reference: $reference, ')
          ..write('syncedAt: $syncedAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
    id,
    idempotencyKey,
    accountId,
    recordType,
    deviceTime,
    connectedBefore,
    content,
    result,
    reason,
    reference,
    syncedAt,
  );
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is QueuedRecord &&
          other.id == this.id &&
          other.idempotencyKey == this.idempotencyKey &&
          other.accountId == this.accountId &&
          other.recordType == this.recordType &&
          other.deviceTime == this.deviceTime &&
          other.connectedBefore == this.connectedBefore &&
          other.content == this.content &&
          other.result == this.result &&
          other.reason == this.reason &&
          other.reference == this.reference &&
          other.syncedAt == this.syncedAt);
}

class QueuedRecordsCompanion extends UpdateCompanion<QueuedRecord> {
  final Value<int> id;
  final Value<String> idempotencyKey;
  final Value<int> accountId;
  final Value<String> recordType;
  final Value<DateTime> deviceTime;
  final Value<DateTime?> connectedBefore;
  final Value<String> content;
  final Value<String?> result;
  final Value<String?> reason;
  final Value<String?> reference;
  final Value<DateTime?> syncedAt;
  const QueuedRecordsCompanion({
    this.id = const Value.absent(),
    this.idempotencyKey = const Value.absent(),
    this.accountId = const Value.absent(),
    this.recordType = const Value.absent(),
    this.deviceTime = const Value.absent(),
    this.connectedBefore = const Value.absent(),
    this.content = const Value.absent(),
    this.result = const Value.absent(),
    this.reason = const Value.absent(),
    this.reference = const Value.absent(),
    this.syncedAt = const Value.absent(),
  });
  QueuedRecordsCompanion.insert({
    this.id = const Value.absent(),
    required String idempotencyKey,
    required int accountId,
    required String recordType,
    required DateTime deviceTime,
    this.connectedBefore = const Value.absent(),
    required String content,
    this.result = const Value.absent(),
    this.reason = const Value.absent(),
    this.reference = const Value.absent(),
    this.syncedAt = const Value.absent(),
  }) : idempotencyKey = Value(idempotencyKey),
       accountId = Value(accountId),
       recordType = Value(recordType),
       deviceTime = Value(deviceTime),
       content = Value(content);
  static Insertable<QueuedRecord> custom({
    Expression<int>? id,
    Expression<String>? idempotencyKey,
    Expression<int>? accountId,
    Expression<String>? recordType,
    Expression<DateTime>? deviceTime,
    Expression<DateTime>? connectedBefore,
    Expression<String>? content,
    Expression<String>? result,
    Expression<String>? reason,
    Expression<String>? reference,
    Expression<DateTime>? syncedAt,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (idempotencyKey != null) 'idempotency_key': idempotencyKey,
      if (accountId != null) 'account_id': accountId,
      if (recordType != null) 'record_type': recordType,
      if (deviceTime != null) 'device_time': deviceTime,
      if (connectedBefore != null) 'connected_before': connectedBefore,
      if (content != null) 'content': content,
      if (result != null) 'result': result,
      if (reason != null) 'reason': reason,
      if (reference != null) 'reference': reference,
      if (syncedAt != null) 'synced_at': syncedAt,
    });
  }

  QueuedRecordsCompanion copyWith({
    Value<int>? id,
    Value<String>? idempotencyKey,
    Value<int>? accountId,
    Value<String>? recordType,
    Value<DateTime>? deviceTime,
    Value<DateTime?>? connectedBefore,
    Value<String>? content,
    Value<String?>? result,
    Value<String?>? reason,
    Value<String?>? reference,
    Value<DateTime?>? syncedAt,
  }) {
    return QueuedRecordsCompanion(
      id: id ?? this.id,
      idempotencyKey: idempotencyKey ?? this.idempotencyKey,
      accountId: accountId ?? this.accountId,
      recordType: recordType ?? this.recordType,
      deviceTime: deviceTime ?? this.deviceTime,
      connectedBefore: connectedBefore ?? this.connectedBefore,
      content: content ?? this.content,
      result: result ?? this.result,
      reason: reason ?? this.reason,
      reference: reference ?? this.reference,
      syncedAt: syncedAt ?? this.syncedAt,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (idempotencyKey.present) {
      map['idempotency_key'] = Variable<String>(idempotencyKey.value);
    }
    if (accountId.present) {
      map['account_id'] = Variable<int>(accountId.value);
    }
    if (recordType.present) {
      map['record_type'] = Variable<String>(recordType.value);
    }
    if (deviceTime.present) {
      map['device_time'] = Variable<DateTime>(deviceTime.value);
    }
    if (connectedBefore.present) {
      map['connected_before'] = Variable<DateTime>(connectedBefore.value);
    }
    if (content.present) {
      map['content'] = Variable<String>(content.value);
    }
    if (result.present) {
      map['result'] = Variable<String>(result.value);
    }
    if (reason.present) {
      map['reason'] = Variable<String>(reason.value);
    }
    if (reference.present) {
      map['reference'] = Variable<String>(reference.value);
    }
    if (syncedAt.present) {
      map['synced_at'] = Variable<DateTime>(syncedAt.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('QueuedRecordsCompanion(')
          ..write('id: $id, ')
          ..write('idempotencyKey: $idempotencyKey, ')
          ..write('accountId: $accountId, ')
          ..write('recordType: $recordType, ')
          ..write('deviceTime: $deviceTime, ')
          ..write('connectedBefore: $connectedBefore, ')
          ..write('content: $content, ')
          ..write('result: $result, ')
          ..write('reason: $reason, ')
          ..write('reference: $reference, ')
          ..write('syncedAt: $syncedAt')
          ..write(')'))
        .toString();
  }
}

abstract class _$OfflineDatabase extends GeneratedDatabase {
  _$OfflineDatabase(QueryExecutor e) : super(e);
  $OfflineDatabaseManager get managers => $OfflineDatabaseManager(this);
  late final $QueuedRecordsTable queuedRecords = $QueuedRecordsTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables => allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [queuedRecords];
}

typedef $$QueuedRecordsTableCreateCompanionBuilder =
    QueuedRecordsCompanion Function({
      Value<int> id,
      required String idempotencyKey,
      required int accountId,
      required String recordType,
      required DateTime deviceTime,
      Value<DateTime?> connectedBefore,
      required String content,
      Value<String?> result,
      Value<String?> reason,
      Value<String?> reference,
      Value<DateTime?> syncedAt,
    });
typedef $$QueuedRecordsTableUpdateCompanionBuilder =
    QueuedRecordsCompanion Function({
      Value<int> id,
      Value<String> idempotencyKey,
      Value<int> accountId,
      Value<String> recordType,
      Value<DateTime> deviceTime,
      Value<DateTime?> connectedBefore,
      Value<String> content,
      Value<String?> result,
      Value<String?> reason,
      Value<String?> reference,
      Value<DateTime?> syncedAt,
    });

class $$QueuedRecordsTableFilterComposer extends Composer<_$OfflineDatabase, $QueuedRecordsTable> {
  $$QueuedRecordsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get idempotencyKey =>
      $composableBuilder(column: $table.idempotencyKey, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get accountId =>
      $composableBuilder(column: $table.accountId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get recordType =>
      $composableBuilder(column: $table.recordType, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get deviceTime =>
      $composableBuilder(column: $table.deviceTime, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get connectedBefore =>
      $composableBuilder(column: $table.connectedBefore, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get content =>
      $composableBuilder(column: $table.content, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get result =>
      $composableBuilder(column: $table.result, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get reason =>
      $composableBuilder(column: $table.reason, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get reference =>
      $composableBuilder(column: $table.reference, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get syncedAt =>
      $composableBuilder(column: $table.syncedAt, builder: (column) => ColumnFilters(column));
}

class $$QueuedRecordsTableOrderingComposer extends Composer<_$OfflineDatabase, $QueuedRecordsTable> {
  $$QueuedRecordsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get idempotencyKey =>
      $composableBuilder(column: $table.idempotencyKey, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get accountId =>
      $composableBuilder(column: $table.accountId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get recordType =>
      $composableBuilder(column: $table.recordType, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get deviceTime =>
      $composableBuilder(column: $table.deviceTime, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get connectedBefore =>
      $composableBuilder(column: $table.connectedBefore, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get content =>
      $composableBuilder(column: $table.content, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get result =>
      $composableBuilder(column: $table.result, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get reason =>
      $composableBuilder(column: $table.reason, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get reference =>
      $composableBuilder(column: $table.reference, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get syncedAt =>
      $composableBuilder(column: $table.syncedAt, builder: (column) => ColumnOrderings(column));
}

class $$QueuedRecordsTableAnnotationComposer extends Composer<_$OfflineDatabase, $QueuedRecordsTable> {
  $$QueuedRecordsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id => $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get idempotencyKey =>
      $composableBuilder(column: $table.idempotencyKey, builder: (column) => column);

  GeneratedColumn<int> get accountId => $composableBuilder(column: $table.accountId, builder: (column) => column);

  GeneratedColumn<String> get recordType => $composableBuilder(column: $table.recordType, builder: (column) => column);

  GeneratedColumn<DateTime> get deviceTime =>
      $composableBuilder(column: $table.deviceTime, builder: (column) => column);

  GeneratedColumn<DateTime> get connectedBefore =>
      $composableBuilder(column: $table.connectedBefore, builder: (column) => column);

  GeneratedColumn<String> get content => $composableBuilder(column: $table.content, builder: (column) => column);

  GeneratedColumn<String> get result => $composableBuilder(column: $table.result, builder: (column) => column);

  GeneratedColumn<String> get reason => $composableBuilder(column: $table.reason, builder: (column) => column);

  GeneratedColumn<String> get reference => $composableBuilder(column: $table.reference, builder: (column) => column);

  GeneratedColumn<DateTime> get syncedAt => $composableBuilder(column: $table.syncedAt, builder: (column) => column);
}

class $$QueuedRecordsTableTableManager
    extends
        RootTableManager<
          _$OfflineDatabase,
          $QueuedRecordsTable,
          QueuedRecord,
          $$QueuedRecordsTableFilterComposer,
          $$QueuedRecordsTableOrderingComposer,
          $$QueuedRecordsTableAnnotationComposer,
          $$QueuedRecordsTableCreateCompanionBuilder,
          $$QueuedRecordsTableUpdateCompanionBuilder,
          (QueuedRecord, BaseReferences<_$OfflineDatabase, $QueuedRecordsTable, QueuedRecord>),
          QueuedRecord,
          PrefetchHooks Function()
        > {
  $$QueuedRecordsTableTableManager(_$OfflineDatabase db, $QueuedRecordsTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () => $$QueuedRecordsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () => $$QueuedRecordsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () => $$QueuedRecordsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                Value<String> idempotencyKey = const Value.absent(),
                Value<int> accountId = const Value.absent(),
                Value<String> recordType = const Value.absent(),
                Value<DateTime> deviceTime = const Value.absent(),
                Value<DateTime?> connectedBefore = const Value.absent(),
                Value<String> content = const Value.absent(),
                Value<String?> result = const Value.absent(),
                Value<String?> reason = const Value.absent(),
                Value<String?> reference = const Value.absent(),
                Value<DateTime?> syncedAt = const Value.absent(),
              }) => QueuedRecordsCompanion(
                id: id,
                idempotencyKey: idempotencyKey,
                accountId: accountId,
                recordType: recordType,
                deviceTime: deviceTime,
                connectedBefore: connectedBefore,
                content: content,
                result: result,
                reason: reason,
                reference: reference,
                syncedAt: syncedAt,
              ),
          createCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                required String idempotencyKey,
                required int accountId,
                required String recordType,
                required DateTime deviceTime,
                Value<DateTime?> connectedBefore = const Value.absent(),
                required String content,
                Value<String?> result = const Value.absent(),
                Value<String?> reason = const Value.absent(),
                Value<String?> reference = const Value.absent(),
                Value<DateTime?> syncedAt = const Value.absent(),
              }) => QueuedRecordsCompanion.insert(
                id: id,
                idempotencyKey: idempotencyKey,
                accountId: accountId,
                recordType: recordType,
                deviceTime: deviceTime,
                connectedBefore: connectedBefore,
                content: content,
                result: result,
                reason: reason,
                reference: reference,
                syncedAt: syncedAt,
              ),
          withReferenceMapper: (p0) => p0.map((e) => (e.readTable(table), BaseReferences(db, table, e))).toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$QueuedRecordsTableProcessedTableManager =
    ProcessedTableManager<
      _$OfflineDatabase,
      $QueuedRecordsTable,
      QueuedRecord,
      $$QueuedRecordsTableFilterComposer,
      $$QueuedRecordsTableOrderingComposer,
      $$QueuedRecordsTableAnnotationComposer,
      $$QueuedRecordsTableCreateCompanionBuilder,
      $$QueuedRecordsTableUpdateCompanionBuilder,
      (QueuedRecord, BaseReferences<_$OfflineDatabase, $QueuedRecordsTable, QueuedRecord>),
      QueuedRecord,
      PrefetchHooks Function()
    >;

class $OfflineDatabaseManager {
  final _$OfflineDatabase _db;
  $OfflineDatabaseManager(this._db);
  $$QueuedRecordsTableTableManager get queuedRecords => $$QueuedRecordsTableTableManager(_db, _db.queuedRecords);
}
