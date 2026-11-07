// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'care_checklist_cache.dart';

// ignore_for_file: type=lint
class $CareChecklistSnapshotsTable extends CareChecklistSnapshots
    with TableInfo<$CareChecklistSnapshotsTable, CareChecklistSnapshot> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $CareChecklistSnapshotsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _accountIdMeta = const VerificationMeta('accountId');
  @override
  late final GeneratedColumn<int> accountId = GeneratedColumn<int>(
    'account_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _shiftIdMeta = const VerificationMeta('shiftId');
  @override
  late final GeneratedColumn<int> shiftId = GeneratedColumn<int>(
    'shift_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
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
  static const VerificationMeta _loadedAtMeta = const VerificationMeta('loadedAt');
  @override
  late final GeneratedColumn<DateTime> loadedAt = GeneratedColumn<DateTime>(
    'loaded_at',
    aliasedName,
    false,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: true,
  );
  @override
  List<GeneratedColumn> get $columns => [accountId, shiftId, content, loadedAt];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'care_checklist_snapshots';
  @override
  VerificationContext validateIntegrity(Insertable<CareChecklistSnapshot> instance, {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('account_id')) {
      context.handle(_accountIdMeta, accountId.isAcceptableOrUnknown(data['account_id']!, _accountIdMeta));
    } else if (isInserting) {
      context.missing(_accountIdMeta);
    }
    if (data.containsKey('shift_id')) {
      context.handle(_shiftIdMeta, shiftId.isAcceptableOrUnknown(data['shift_id']!, _shiftIdMeta));
    } else if (isInserting) {
      context.missing(_shiftIdMeta);
    }
    if (data.containsKey('content')) {
      context.handle(_contentMeta, content.isAcceptableOrUnknown(data['content']!, _contentMeta));
    } else if (isInserting) {
      context.missing(_contentMeta);
    }
    if (data.containsKey('loaded_at')) {
      context.handle(_loadedAtMeta, loadedAt.isAcceptableOrUnknown(data['loaded_at']!, _loadedAtMeta));
    } else if (isInserting) {
      context.missing(_loadedAtMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {accountId, shiftId};
  @override
  CareChecklistSnapshot map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return CareChecklistSnapshot(
      accountId: attachedDatabase.typeMapping.read(DriftSqlType.int, data['${effectivePrefix}account_id'])!,
      shiftId: attachedDatabase.typeMapping.read(DriftSqlType.int, data['${effectivePrefix}shift_id'])!,
      content: attachedDatabase.typeMapping.read(DriftSqlType.string, data['${effectivePrefix}content'])!,
      loadedAt: attachedDatabase.typeMapping.read(DriftSqlType.dateTime, data['${effectivePrefix}loaded_at'])!,
    );
  }

  @override
  $CareChecklistSnapshotsTable createAlias(String alias) {
    return $CareChecklistSnapshotsTable(attachedDatabase, alias);
  }
}

class CareChecklistSnapshot extends DataClass implements Insertable<CareChecklistSnapshot> {
  final int accountId;
  final int shiftId;
  final String content;
  final DateTime loadedAt;
  const CareChecklistSnapshot({
    required this.accountId,
    required this.shiftId,
    required this.content,
    required this.loadedAt,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['account_id'] = Variable<int>(accountId);
    map['shift_id'] = Variable<int>(shiftId);
    map['content'] = Variable<String>(content);
    map['loaded_at'] = Variable<DateTime>(loadedAt);
    return map;
  }

  CareChecklistSnapshotsCompanion toCompanion(bool nullToAbsent) {
    return CareChecklistSnapshotsCompanion(
      accountId: Value(accountId),
      shiftId: Value(shiftId),
      content: Value(content),
      loadedAt: Value(loadedAt),
    );
  }

  factory CareChecklistSnapshot.fromJson(Map<String, dynamic> json, {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return CareChecklistSnapshot(
      accountId: serializer.fromJson<int>(json['accountId']),
      shiftId: serializer.fromJson<int>(json['shiftId']),
      content: serializer.fromJson<String>(json['content']),
      loadedAt: serializer.fromJson<DateTime>(json['loadedAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'accountId': serializer.toJson<int>(accountId),
      'shiftId': serializer.toJson<int>(shiftId),
      'content': serializer.toJson<String>(content),
      'loadedAt': serializer.toJson<DateTime>(loadedAt),
    };
  }

  CareChecklistSnapshot copyWith({int? accountId, int? shiftId, String? content, DateTime? loadedAt}) =>
      CareChecklistSnapshot(
        accountId: accountId ?? this.accountId,
        shiftId: shiftId ?? this.shiftId,
        content: content ?? this.content,
        loadedAt: loadedAt ?? this.loadedAt,
      );
  CareChecklistSnapshot copyWithCompanion(CareChecklistSnapshotsCompanion data) {
    return CareChecklistSnapshot(
      accountId: data.accountId.present ? data.accountId.value : this.accountId,
      shiftId: data.shiftId.present ? data.shiftId.value : this.shiftId,
      content: data.content.present ? data.content.value : this.content,
      loadedAt: data.loadedAt.present ? data.loadedAt.value : this.loadedAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('CareChecklistSnapshot(')
          ..write('accountId: $accountId, ')
          ..write('shiftId: $shiftId, ')
          ..write('content: $content, ')
          ..write('loadedAt: $loadedAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(accountId, shiftId, content, loadedAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is CareChecklistSnapshot &&
          other.accountId == this.accountId &&
          other.shiftId == this.shiftId &&
          other.content == this.content &&
          other.loadedAt == this.loadedAt);
}

class CareChecklistSnapshotsCompanion extends UpdateCompanion<CareChecklistSnapshot> {
  final Value<int> accountId;
  final Value<int> shiftId;
  final Value<String> content;
  final Value<DateTime> loadedAt;
  final Value<int> rowid;
  const CareChecklistSnapshotsCompanion({
    this.accountId = const Value.absent(),
    this.shiftId = const Value.absent(),
    this.content = const Value.absent(),
    this.loadedAt = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  CareChecklistSnapshotsCompanion.insert({
    required int accountId,
    required int shiftId,
    required String content,
    required DateTime loadedAt,
    this.rowid = const Value.absent(),
  }) : accountId = Value(accountId),
       shiftId = Value(shiftId),
       content = Value(content),
       loadedAt = Value(loadedAt);
  static Insertable<CareChecklistSnapshot> custom({
    Expression<int>? accountId,
    Expression<int>? shiftId,
    Expression<String>? content,
    Expression<DateTime>? loadedAt,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (accountId != null) 'account_id': accountId,
      if (shiftId != null) 'shift_id': shiftId,
      if (content != null) 'content': content,
      if (loadedAt != null) 'loaded_at': loadedAt,
      if (rowid != null) 'rowid': rowid,
    });
  }

  CareChecklistSnapshotsCompanion copyWith({
    Value<int>? accountId,
    Value<int>? shiftId,
    Value<String>? content,
    Value<DateTime>? loadedAt,
    Value<int>? rowid,
  }) {
    return CareChecklistSnapshotsCompanion(
      accountId: accountId ?? this.accountId,
      shiftId: shiftId ?? this.shiftId,
      content: content ?? this.content,
      loadedAt: loadedAt ?? this.loadedAt,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (accountId.present) {
      map['account_id'] = Variable<int>(accountId.value);
    }
    if (shiftId.present) {
      map['shift_id'] = Variable<int>(shiftId.value);
    }
    if (content.present) {
      map['content'] = Variable<String>(content.value);
    }
    if (loadedAt.present) {
      map['loaded_at'] = Variable<DateTime>(loadedAt.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('CareChecklistSnapshotsCompanion(')
          ..write('accountId: $accountId, ')
          ..write('shiftId: $shiftId, ')
          ..write('content: $content, ')
          ..write('loadedAt: $loadedAt, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

abstract class _$CareChecklistCache extends GeneratedDatabase {
  _$CareChecklistCache(QueryExecutor e) : super(e);
  $CareChecklistCacheManager get managers => $CareChecklistCacheManager(this);
  late final $CareChecklistSnapshotsTable careChecklistSnapshots = $CareChecklistSnapshotsTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables => allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [careChecklistSnapshots];
}

typedef $$CareChecklistSnapshotsTableCreateCompanionBuilder =
    CareChecklistSnapshotsCompanion Function({
      required int accountId,
      required int shiftId,
      required String content,
      required DateTime loadedAt,
      Value<int> rowid,
    });
typedef $$CareChecklistSnapshotsTableUpdateCompanionBuilder =
    CareChecklistSnapshotsCompanion Function({
      Value<int> accountId,
      Value<int> shiftId,
      Value<String> content,
      Value<DateTime> loadedAt,
      Value<int> rowid,
    });

class $$CareChecklistSnapshotsTableFilterComposer extends Composer<_$CareChecklistCache, $CareChecklistSnapshotsTable> {
  $$CareChecklistSnapshotsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get accountId =>
      $composableBuilder(column: $table.accountId, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get shiftId =>
      $composableBuilder(column: $table.shiftId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get content =>
      $composableBuilder(column: $table.content, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get loadedAt =>
      $composableBuilder(column: $table.loadedAt, builder: (column) => ColumnFilters(column));
}

class $$CareChecklistSnapshotsTableOrderingComposer
    extends Composer<_$CareChecklistCache, $CareChecklistSnapshotsTable> {
  $$CareChecklistSnapshotsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get accountId =>
      $composableBuilder(column: $table.accountId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get shiftId =>
      $composableBuilder(column: $table.shiftId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get content =>
      $composableBuilder(column: $table.content, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get loadedAt =>
      $composableBuilder(column: $table.loadedAt, builder: (column) => ColumnOrderings(column));
}

class $$CareChecklistSnapshotsTableAnnotationComposer
    extends Composer<_$CareChecklistCache, $CareChecklistSnapshotsTable> {
  $$CareChecklistSnapshotsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get accountId => $composableBuilder(column: $table.accountId, builder: (column) => column);

  GeneratedColumn<int> get shiftId => $composableBuilder(column: $table.shiftId, builder: (column) => column);

  GeneratedColumn<String> get content => $composableBuilder(column: $table.content, builder: (column) => column);

  GeneratedColumn<DateTime> get loadedAt => $composableBuilder(column: $table.loadedAt, builder: (column) => column);
}

class $$CareChecklistSnapshotsTableTableManager
    extends
        RootTableManager<
          _$CareChecklistCache,
          $CareChecklistSnapshotsTable,
          CareChecklistSnapshot,
          $$CareChecklistSnapshotsTableFilterComposer,
          $$CareChecklistSnapshotsTableOrderingComposer,
          $$CareChecklistSnapshotsTableAnnotationComposer,
          $$CareChecklistSnapshotsTableCreateCompanionBuilder,
          $$CareChecklistSnapshotsTableUpdateCompanionBuilder,
          (
            CareChecklistSnapshot,
            BaseReferences<_$CareChecklistCache, $CareChecklistSnapshotsTable, CareChecklistSnapshot>,
          ),
          CareChecklistSnapshot,
          PrefetchHooks Function()
        > {
  $$CareChecklistSnapshotsTableTableManager(_$CareChecklistCache db, $CareChecklistSnapshotsTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () => $$CareChecklistSnapshotsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () => $$CareChecklistSnapshotsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () => $$CareChecklistSnapshotsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> accountId = const Value.absent(),
                Value<int> shiftId = const Value.absent(),
                Value<String> content = const Value.absent(),
                Value<DateTime> loadedAt = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) => CareChecklistSnapshotsCompanion(
                accountId: accountId,
                shiftId: shiftId,
                content: content,
                loadedAt: loadedAt,
                rowid: rowid,
              ),
          createCompanionCallback:
              ({
                required int accountId,
                required int shiftId,
                required String content,
                required DateTime loadedAt,
                Value<int> rowid = const Value.absent(),
              }) => CareChecklistSnapshotsCompanion.insert(
                accountId: accountId,
                shiftId: shiftId,
                content: content,
                loadedAt: loadedAt,
                rowid: rowid,
              ),
          withReferenceMapper: (p0) => p0.map((e) => (e.readTable(table), BaseReferences(db, table, e))).toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$CareChecklistSnapshotsTableProcessedTableManager =
    ProcessedTableManager<
      _$CareChecklistCache,
      $CareChecklistSnapshotsTable,
      CareChecklistSnapshot,
      $$CareChecklistSnapshotsTableFilterComposer,
      $$CareChecklistSnapshotsTableOrderingComposer,
      $$CareChecklistSnapshotsTableAnnotationComposer,
      $$CareChecklistSnapshotsTableCreateCompanionBuilder,
      $$CareChecklistSnapshotsTableUpdateCompanionBuilder,
      (
        CareChecklistSnapshot,
        BaseReferences<_$CareChecklistCache, $CareChecklistSnapshotsTable, CareChecklistSnapshot>,
      ),
      CareChecklistSnapshot,
      PrefetchHooks Function()
    >;

class $CareChecklistCacheManager {
  final _$CareChecklistCache _db;
  $CareChecklistCacheManager(this._db);
  $$CareChecklistSnapshotsTableTableManager get careChecklistSnapshots =>
      $$CareChecklistSnapshotsTableTableManager(_db, _db.careChecklistSnapshots);
}
