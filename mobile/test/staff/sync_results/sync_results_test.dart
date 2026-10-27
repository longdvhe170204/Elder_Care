import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';

import '../../support/fake_backend.dart';

// 002 T060 (tên logic ket_qua_dong_bo_test; BR-M15-20, constitution XV; DESIGN.md 15.5): app hiển thị kết quả từng
// bản ghi sau đồng bộ; mất mạng thì dải báo ngoại tuyến; tài khoản không còn Hoạt động thì kết thúc phiên.

/// Nhân viên đã đăng nhập, hai bản ghi chờ đồng bộ; trả khóa của hai bản ghi.
Future<List<String>> pumpStaff(WidgetTester tester, FakeBackend backend, MemoryStore store) async {
  backend.on('GET', '/me', 200, user('STAFF'));
  store.values[SessionController.tokenKey] = 'staff-1';
  final s = session(backend, store, FakeBiometric(available: false));
  await s.start();
  final queue = memoryQueue(s);
  final keys = await tester.runAsync(
    () async => [
      await queue.enqueue(accountId: 7, recordType: 'M07.KET_QUA_LIEU', content: {'a': 1}),
      await queue.enqueue(accountId: 7, recordType: 'M07.KET_QUA_LIEU', content: {'a': 2}),
    ],
  );
  await tester.pumpWidget(ElderCareApp(session: s, queue: queue));
  await settle(tester);
  return keys!;
}

/// Chờ truy vấn SQLite (chạy ngoài đồng hồ giả của test) rồi vẽ lại.
Future<void> settle(WidgetTester tester) async {
  await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 100)));
  await tester.pumpAndSettle();
}

Future<void> syncNow(WidgetTester tester) async {
  await tester.tap(find.text('Đồng bộ ngay'));
  await settle(tester);
  await settle(tester);
}

void main() {
  late FakeBackend backend;
  late MemoryStore store;

  setUp(() {
    backend = FakeBackend();
    store = MemoryStore();
  });

  testWidgets('BR_M15_20_dongBo_hienKetQuaTungBanGhi_kemLyDoTuChoi', (tester) async {
    final keys = await pumpStaff(tester, backend, store);
    backend.on('POST', '/sync', 200, {
      'sessionEnded': false,
      'results': [
        {'idempotencyKey': keys[0], 'result': 'ACCEPTED'},
        {'idempotencyKey': keys[1], 'result': 'REJECTED', 'reason': 'Không còn phân công'},
      ],
    });
    expect(find.text('Chưa đồng bộ: 2 bản ghi'), findsOneWidget);

    await syncNow(tester);

    expect(find.text('Chưa đồng bộ: 0 bản ghi'), findsOneWidget);
    expect(find.text('Đã nhận'), findsOneWidget);
    expect(find.text('Bị từ chối'), findsOneWidget);
    expect(find.text('Lý do: Không còn phân công'), findsOneWidget);
    expect(find.text('Kết quả liều thuốc'), findsNWidgets(2));
  });

  testWidgets('15_5_matMang_hienDaiNgoaiTuyen_banGhiGiuNguyen', (tester) async {
    await pumpStaff(tester, backend, store);
    backend.down = true;

    await syncNow(tester);

    expect(find.text('Đang ngoại tuyến. Bản ghi sẽ đồng bộ khi có mạng.'), findsOneWidget);
    expect(find.text('Chưa đồng bộ: 2 bản ghi'), findsOneWidget);
  });

  testWidgets('FR_013_taiKhoanKhongConHoatDong_ketThucPhien', (tester) async {
    await pumpStaff(tester, backend, store);
    backend.on('POST', '/sync', 200, {'sessionEnded': true, 'results': <Object>[]});

    await syncNow(tester);

    expect(find.textContaining('phiên đã kết thúc'), findsOneWidget);
    expect(store.values[SessionController.tokenKey], isNull);
  });
}
