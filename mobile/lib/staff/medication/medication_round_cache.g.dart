// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'medication_round_cache.dart';

// ignore_for_file: type=lint
class $MedicationRoundSnapshotsTable extends MedicationRoundSnapshots
    with TableInfo<$MedicationRoundSnapshotsTable, MedicationRoundSnapshot> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $MedicationRoundSnapshotsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _accountIdMeta = const VerificationMeta('accountId');
  @override
  late final GeneratedColumn<int> accountId = GeneratedColumn<int>(
    'account_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
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
  List<GeneratedColumn> get $columns => [accountId, content, loadedAt];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'medication_round_snapshots';
  @override
  VerificationContext validateIntegrity(Insertable<MedicationRoundSnapshot> instance, {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('account_id')) {
      context.handle(_accountIdMeta, accountId.isAcceptableOrUnknown(data['account_id']!, _accountIdMeta));
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
  Set<GeneratedColumn> get $primaryKey => {accountId};
  @override
  MedicationRoundSnapshot map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return MedicationRoundSnapshot(
      accountId: attachedDatabase.typeMapping.read(DriftSqlType.int, data['${effectivePrefix}account_id'])!,
      content: attachedDatabase.typeMapping.read(DriftSqlType.string, data['${effectivePrefix}content'])!,
      loadedAt: attachedDatabase.typeMapping.read(DriftSqlType.dateTime, data['${effectivePrefix}loaded_at'])!,
    );
  }

  @override
  $MedicationRoundSnapshotsTable createAlias(String alias) {
    return $MedicationRoundSnapshotsTable(attachedDatabase, alias);
  }
}

class MedicationRoundSnapshot extends DataClass implements Insertable<MedicationRoundSnapshot> {
  final int accountId;
  final String content;
  final DateTime loadedAt;
  const MedicationRoundSnapshot({required this.accountId, required this.content, required this.loadedAt});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['account_id'] = Variable<int>(accountId);
    map['content'] = Variable<String>(content);
    map['loaded_at'] = Variable<DateTime>(loadedAt);
    return map;
  }

  MedicationRoundSnapshotsCompanion toCompanion(bool nullToAbsent) {
    return MedicationRoundSnapshotsCompanion(
      accountId: Value(accountId),
      content: Value(content),
      loadedAt: Value(loadedAt),
    );
  }

  factory MedicationRoundSnapshot.fromJson(Map<String, dynamic> json, {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return MedicationRoundSnapshot(
      accountId: serializer.fromJson<int>(json['accountId']),
      content: serializer.fromJson<String>(json['content']),
      loadedAt: serializer.fromJson<DateTime>(json['loadedAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'accountId': serializer.toJson<int>(accountId),
      'content': serializer.toJson<String>(content),
      'loadedAt': serializer.toJson<DateTime>(loadedAt),
    };
  }

  MedicationRoundSnapshot copyWith({int? accountId, String? content, DateTime? loadedAt}) => MedicationRoundSnapshot(
    accountId: accountId ?? this.accountId,
    content: content ?? this.content,
    loadedAt: loadedAt ?? this.loadedAt,
  );
  MedicationRoundSnapshot copyWithCompanion(MedicationRoundSnapshotsCompanion data) {
    return MedicationRoundSnapshot(
      accountId: data.accountId.present ? data.accountId.value : this.accountId,
      content: data.content.present ? data.content.value : this.content,
      loadedAt: data.loadedAt.present ? data.loadedAt.value : this.loadedAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('MedicationRoundSnapshot(')
          ..write('accountId: $accountId, ')
          ..write('content: $content, ')
          ..write('loadedAt: $loadedAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(accountId, content, loadedAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is MedicationRoundSnapshot &&
          other.accountId == this.accountId &&
          other.content == this.content &&
          other.loadedAt == this.loadedAt);
}

class MedicationRoundSnapshotsCompanion extends UpdateCompanion<MedicationRoundSnapshot> {
  final Value<int> accountId;
  final Value<String> content;
  final Value<DateTime> loadedAt;
  const MedicationRoundSnapshotsCompanion({
    this.accountId = const Value.absent(),
    this.content = const Value.absent(),
    this.loadedAt = const Value.absent(),
  });
  MedicationRoundSnapshotsCompanion.insert({
    this.accountId = const Value.absent(),
    required String content,
    required DateTime loadedAt,
  }) : content = Value(content),
       loadedAt = Value(loadedAt);
  static Insertable<MedicationRoundSnapshot> custom({
    Expression<int>? accountId,
    Expression<String>? content,
    Expression<DateTime>? loadedAt,
  }) {
    return RawValuesInsertable({
      if (accountId != null) 'account_id': accountId,
      if (content != null) 'content': content,
      if (loadedAt != null) 'loaded_at': loadedAt,
    });
  }

  MedicationRoundSnapshotsCompanion copyWith({
    Value<int>? accountId,
    Value<String>? content,
    Value<DateTime>? loadedAt,
  }) {
    return MedicationRoundSnapshotsCompanion(
      accountId: accountId ?? this.accountId,
      content: content ?? this.content,
      loadedAt: loadedAt ?? this.loadedAt,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (accountId.present) {
      map['account_id'] = Variable<int>(accountId.value);
    }
    if (content.present) {
      map['content'] = Variable<String>(content.value);
    }
    if (loadedAt.present) {
      map['loaded_at'] = Variable<DateTime>(loadedAt.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('MedicationRoundSnapshotsCompanion(')
          ..write('accountId: $accountId, ')
          ..write('content: $content, ')
          ..write('loadedAt: $loadedAt')
          ..write(')'))
        .toString();
  }
}

abstract class _$MedicationRoundCache extends GeneratedDatabase {
  _$MedicationRoundCache(QueryExecutor e) : super(e);
  $MedicationRoundCacheManager get managers => $MedicationRoundCacheManager(this);
  late final $MedicationRoundSnapshotsTable medicationRoundSnapshots = $MedicationRoundSnapshotsTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables => allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [medicationRoundSnapshots];
}

typedef $$MedicationRoundSnapshotsTableCreateCompanionBuilder =
    MedicationRoundSnapshotsCompanion Function({
      Value<int> accountId,
      required String content,
      required DateTime loadedAt,
    });
typedef $$MedicationRoundSnapshotsTableUpdateCompanionBuilder =
    MedicationRoundSnapshotsCompanion Function({Value<int> accountId, Value<String> content, Value<DateTime> loadedAt});

class $$MedicationRoundSnapshotsTableFilterComposer
    extends Composer<_$MedicationRoundCache, $MedicationRoundSnapshotsTable> {
  $$MedicationRoundSnapshotsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get accountId =>
      $composableBuilder(column: $table.accountId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get content =>
      $composableBuilder(column: $table.content, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get loadedAt =>
      $composableBuilder(column: $table.loadedAt, builder: (column) => ColumnFilters(column));
}

class $$MedicationRoundSnapshotsTableOrderingComposer
    extends Composer<_$MedicationRoundCache, $MedicationRoundSnapshotsTable> {
  $$MedicationRoundSnapshotsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get accountId =>
      $composableBuilder(column: $table.accountId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get content =>
      $composableBuilder(column: $table.content, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get loadedAt =>
      $composableBuilder(column: $table.loadedAt, builder: (column) => ColumnOrderings(column));
}

class $$MedicationRoundSnapshotsTableAnnotationComposer
    extends Composer<_$MedicationRoundCache, $MedicationRoundSnapshotsTable> {
  $$MedicationRoundSnapshotsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get accountId => $composableBuilder(column: $table.accountId, builder: (column) => column);

  GeneratedColumn<String> get content => $composableBuilder(column: $table.content, builder: (column) => column);

  GeneratedColumn<DateTime> get loadedAt => $composableBuilder(column: $table.loadedAt, builder: (column) => column);
}

class $$MedicationRoundSnapshotsTableTableManager
    extends
        RootTableManager<
          _$MedicationRoundCache,
          $MedicationRoundSnapshotsTable,
          MedicationRoundSnapshot,
          $$MedicationRoundSnapshotsTableFilterComposer,
          $$MedicationRoundSnapshotsTableOrderingComposer,
          $$MedicationRoundSnapshotsTableAnnotationComposer,
          $$MedicationRoundSnapshotsTableCreateCompanionBuilder,
          $$MedicationRoundSnapshotsTableUpdateCompanionBuilder,
          (
            MedicationRoundSnapshot,
            BaseReferences<_$MedicationRoundCache, $MedicationRoundSnapshotsTable, MedicationRoundSnapshot>,
          ),
          MedicationRoundSnapshot,
          PrefetchHooks Function()
        > {
  $$MedicationRoundSnapshotsTableTableManager(_$MedicationRoundCache db, $MedicationRoundSnapshotsTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () => $$MedicationRoundSnapshotsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () => $$MedicationRoundSnapshotsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () => $$MedicationRoundSnapshotsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> accountId = const Value.absent(),
                Value<String> content = const Value.absent(),
                Value<DateTime> loadedAt = const Value.absent(),
              }) => MedicationRoundSnapshotsCompanion(accountId: accountId, content: content, loadedAt: loadedAt),
          createCompanionCallback:
              ({Value<int> accountId = const Value.absent(), required String content, required DateTime loadedAt}) =>
                  MedicationRoundSnapshotsCompanion.insert(accountId: accountId, content: content, loadedAt: loadedAt),
          withReferenceMapper: (p0) => p0.map((e) => (e.readTable(table), BaseReferences(db, table, e))).toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$MedicationRoundSnapshotsTableProcessedTableManager =
    ProcessedTableManager<
      _$MedicationRoundCache,
      $MedicationRoundSnapshotsTable,
      MedicationRoundSnapshot,
      $$MedicationRoundSnapshotsTableFilterComposer,
      $$MedicationRoundSnapshotsTableOrderingComposer,
      $$MedicationRoundSnapshotsTableAnnotationComposer,
      $$MedicationRoundSnapshotsTableCreateCompanionBuilder,
      $$MedicationRoundSnapshotsTableUpdateCompanionBuilder,
      (
        MedicationRoundSnapshot,
        BaseReferences<_$MedicationRoundCache, $MedicationRoundSnapshotsTable, MedicationRoundSnapshot>,
      ),
      MedicationRoundSnapshot,
      PrefetchHooks Function()
    >;

class $MedicationRoundCacheManager {
  final _$MedicationRoundCache _db;
  $MedicationRoundCacheManager(this._db);
  $$MedicationRoundSnapshotsTableTableManager get medicationRoundSnapshots =>
      $$MedicationRoundSnapshotsTableTableManager(_db, _db.medicationRoundSnapshots);
}
