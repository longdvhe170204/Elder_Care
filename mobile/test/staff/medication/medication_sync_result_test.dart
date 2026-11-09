import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';
import 'package:mobile/staff/medication/medication_round_cache.dart';
import 'package:mobile/staff/medication/medication_round_gateway.dart';
import 'package:mobile/staff/medication/medication_round_repository.dart';

import '../../support/fake_backend.dart';
import 'medication_fixtures.dart';

// 006 T028 (tên logic ket_qua_dong_bo_thuoc_test; BR-M15-20; quickstart Q15 BanGhiLieuBiTuChoiTest): kết quả liều ghi
// khi mất mạng hiện ở màn Đồng bộ với tên loại bản ghi và lý do bị từ chối (liều đã được thiết bị khác đóng).

Future<void> settle(WidgetTester tester) async {
  await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 100)));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('BR_M15_20_ketQuaLieuNgoaiTuyen_hienTenLoaiVaLyDoTuChoi', (tester) async {
    final backend = FakeBackend();
    final store = MemoryStore();
    serveRound(backend);
    backend.on('GET', '/me', 200, user('STAFF'));
    store.values[SessionController.tokenKey] = 'staff-1';
    final s = session(backend, store, FakeBiometric(available: false));
    await s.start();
    final queue = memoryQueue(s);
    final cache = MedicationRoundCache(NativeDatabase.memory());
    addTearDown(cache.close);
    final repository = MedicationRoundRepository(ApiMedicationRoundGateway(s.api), cache, queue);
    await tester.runAsync(() async {
      final load = await repository.load(7);
      backend.down = true;
      await repository.record(
        accountId: 7,
        item: load.round.items.first,
        outcome: DoseOutcome.administered,
        body: {'administeredAt': '2026-11-05T08:05:00+07:00', 'basis': 'REGULAR'},
        key: 'k1',
      );
      backend.down = false;
    });
    final key = (await tester.runAsync(() => queue.unsynced(7)))!.single.idempotencyKey;
    backend.on('POST', '/sync', 200, {
      'sessionEnded': false,
      'results': [
        {'idempotencyKey': key, 'result': 'REJECTED', 'reason': 'Liều đã được Trần Thu Hà xác nhận trước'},
      ],
    });

    await tester.pumpWidget(ElderCareApp(session: s, queue: queue));
    await settle(tester);
    await tester.tap(find.text('Đồng bộ'));
    await settle(tester);
    await tester.tap(find.text('Đồng bộ ngay'));
    await settle(tester);
    await settle(tester);

    expect(find.text('Kết quả liều thuốc'), findsOneWidget);
    expect(find.text('Bị từ chối'), findsOneWidget);
    expect(find.text('Lý do: Liều đã được Trần Thu Hà xác nhận trước'), findsOneWidget);
  });
}
