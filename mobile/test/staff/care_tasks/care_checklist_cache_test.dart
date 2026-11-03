import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/staff/care_tasks/care_checklist_cache.dart';
import 'package:mobile/staff/care_tasks/care_checklist_gateway.dart';
import 'package:mobile/staff/care_tasks/care_checklist_repository.dart';

class FixtureChecklistGateway implements CareChecklistGateway {
  Exception? failure;
  Future<Map<String, dynamic>> Function(int)? loader;
  Exception? commandFailure;
  final List<List<Object?>> attempts = [];
  final List<Map<String, dynamic>> recordings = [];
  @override
  Future<void> record(int taskId, Map<String, dynamic> body, String key) async {
    recordings.add(body);
    if (commandFailure != null) throw commandFailure!;
  }

  final body = <String, dynamic>{
    'shiftId': 9,
    'loadedAt': '2026-11-01T03:00:00Z',
    'tasks': <dynamic>[],
    'riskAgreements': <dynamic>[],
    'nonPerformanceReasonTypes': <dynamic>[],
  };
  @override
  Future<Map<String, dynamic>> load(int shiftId) async {
    if (loader != null) return loader!(shiftId);
    if (failure != null) throw failure!;
    return body;
  }

  @override
  Future<void> notPerform(int taskId, int reasonTypeId, String? note, String key) async {
    attempts.add([taskId, reasonTypeId, note, key]);
    if (commandFailure != null) throw commandFailure!;
  }

  @override
  Future<void> claim(int taskId, String key) async {}
}

void main() {
  late CareChecklistCache cache;
  late FixtureChecklistGateway gateway;
  late CareChecklistRepository repository;
  setUp(() {
    cache = CareChecklistCache(NativeDatabase.memory());
    gateway = FixtureChecklistGateway();
    repository = CareChecklistRepository(gateway, cache);
  });
  tearDown(() => cache.close());
  test('FR_035_luuChecklistTheoTaiKhoanVaCa', () async {
    final first = await repository.load(1, 9);
    expect(first.offline, isFalse);
    expect((await cache.read(1, 9))!.content, gateway.body);
    expect(await cache.read(2, 9), isNull);
    expect(await cache.read(1, 10), isNull);
  });
  test('FR_035_matMangDocBanDaTaiKemMocChuaCapNhat', () async {
    await repository.load(1, 9);
    gateway.failure = ApiUnreachable();
    final stale = await repository.load(1, 9);
    expect(stale.offline, isTrue);
    expect(stale.snapshot.loadedAt, DateTime.utc(2026, 11, 1, 3));
  });
  test('FR_035_khongGiaVoCoCacheKhiChuaTaiLanNao', () async {
    gateway.failure = ApiUnreachable();
    expect(() => repository.load(1, 9), throwsA(isA<ApiUnreachable>()));
  });
  test('BR_M15_20_matQuyenThiKhongHienLaiCacheCu', () async {
    await repository.load(1, 9);
    gateway.failure = ApiProblem(403, 'PERMISSION_DENIED', 'Không có quyền.');
    await expectLater(() => repository.load(1, 9), throwsA(isA<ApiProblem>()));
    expect(await cache.read(1, 9), isNull);
  });
}
