import 'dart:convert';

import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';

import '../../support/fake_backend.dart';

// 000 T100 (R-13; 002 research A-11, BR-M15-20): hàng đợi ngoại tuyến của phần nhân viên: phong bì Idempotency-Key,
// thời điểm thiết bị; gửi lô qua POST /sync, lưu kết quả từng bản ghi.

void main() {
  late FakeBackend backend;
  late ApiClient api;
  late OfflineDatabase db;
  late OfflineQueue queue;
  late DateTime now;

  setUp(() {
    backend = FakeBackend();
    api = backend.client();
    db = OfflineDatabase(NativeDatabase.memory());
    now = DateTime.utc(2026, 10, 27, 1, 0);
    var n = 0;
    queue = OfflineQueue(db, api, clock: () => now, newKey: () => 'key-${++n}');
  });

  tearDown(() => db.close());

  test('T100_ghiKhiMatMang_phongBiCoKhoaVaThoiDiemThietBi', () async {
    api.lastConnectedAt = DateTime.utc(2026, 10, 27, 0, 30);

    final key = await queue.enqueue(accountId: 5, recordType: 'M07.KET_QUA_LIEU', content: {'note': 'Uống hết'});

    expect(key, 'key-1');
    final waiting = await queue.unsynced(5);
    expect(waiting.single.idempotencyKey, 'key-1');
    expect(waiting.single.deviceTime.toUtc(), DateTime.utc(2026, 10, 27, 1, 0));
    expect(waiting.single.connectedBefore?.toUtc(), DateTime.utc(2026, 10, 27, 0, 30));
    expect(jsonDecode(waiting.single.content), {'note': 'Uống hết'});
  });

  test('T100_dongBo_guiLo_lastConnectedAtNhoNhat_luuKetQuaTungBanGhi', () async {
    api.lastConnectedAt = DateTime.utc(2026, 10, 27, 0, 30);
    await queue.enqueue(accountId: 5, recordType: 'M07.KET_QUA_LIEU', content: {'a': 1});
    api.lastConnectedAt = null;
    now = DateTime.utc(2026, 10, 27, 2, 0);
    await queue.enqueue(accountId: 5, recordType: 'M07.KET_QUA_LIEU', content: {'a': 2});
    backend.on('POST', '/sync', 200, {
      'sessionEnded': false,
      'residentsInScope': [3, 4],
      'results': [
        {'idempotencyKey': 'key-1', 'result': 'ACCEPTED', 'reference': 'dose:9'},
        {'idempotencyKey': 'key-2', 'result': 'REJECTED', 'reason': 'Không còn phân công'},
      ],
    });

    final report = await queue.sync(5);

    final body = jsonDecode(backend.calls('POST', '/sync').single.body) as Map<String, dynamic>;
    expect(body['lastConnectedAt'], '2026-10-27T00:30:00.000Z');
    expect(body['records'], [
      {
        'idempotencyKey': 'key-1',
        'recordType': 'M07.KET_QUA_LIEU',
        'deviceTime': '2026-10-27T01:00:00.000Z',
        'content': {'a': 1},
      },
      {
        'idempotencyKey': 'key-2',
        'recordType': 'M07.KET_QUA_LIEU',
        'deviceTime': '2026-10-27T02:00:00.000Z',
        'content': {'a': 2},
      },
    ]);
    expect(report.sessionEnded, isFalse);
    expect(report.residentsInScope, [3, 4]);
    expect(report.results.map((r) => (r.idempotencyKey, r.result, r.reason)), [
      ('key-1', SyncResult.accepted, null),
      ('key-2', SyncResult.rejected, 'Không còn phân công'),
    ]);
    expect(await queue.unsynced(5), isEmpty);
    expect((await queue.synced(5)).map((r) => r.result), ['ACCEPTED', 'REJECTED']);
  });

  test('T100_khongCoGiChoDongBo_khongGoiBackend', () async {
    final report = await queue.sync(5);

    expect(report.results, isEmpty);
    expect(backend.requests, isEmpty);
  });

  test('T100_vanMatMang_banGhiGiuNguyenChoLanSau', () async {
    await queue.enqueue(accountId: 5, recordType: 'M07.KET_QUA_LIEU', content: {'a': 1});
    backend.down = true;

    await expectLater(queue.sync(5), throwsA(isA<ApiUnreachable>()));
    expect(await queue.unsynced(5), hasLength(1));
  });

  test('T100_chiGuiBanGhiCuaTaiKhoanDangDangNhap_baoKetThucPhien', () async {
    await queue.enqueue(accountId: 5, recordType: 'M07.KET_QUA_LIEU', content: {'a': 1});
    await queue.enqueue(accountId: 6, recordType: 'M07.KET_QUA_LIEU', content: {'a': 2});
    backend.on('POST', '/sync', 200, {
      'sessionEnded': true,
      'results': [
        {'idempotencyKey': 'key-2', 'result': 'PENDING_REVIEW'},
      ],
    });

    final report = await queue.sync(6);

    final body = jsonDecode(backend.calls('POST', '/sync').single.body) as Map<String, dynamic>;
    expect(((body['records'] as List).single as Map<String, dynamic>)['idempotencyKey'], 'key-2');
    expect(report.sessionEnded, isTrue);
    expect(report.results.single.result, SyncResult.pendingReview);
    expect(await queue.unsynced(5), hasLength(1));
  });
}
