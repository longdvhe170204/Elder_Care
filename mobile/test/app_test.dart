import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';

import 'support/fake_backend.dart';

// 000 T006, T007 (Q-346, Q-365): app chung mở đăng nhập rồi chuyển sang phần nhân viên, người thân hoặc người cao
// tuổi theo loại tài khoản của phiên; máy đã gắn tài khoản người cao tuổi vào thẳng màn quét mã (002 FR-014a).

Widget fakeScanner(void Function(String code) onCode) => const SizedBox.shrink();

Future<void> pumpSignedIn(WidgetTester tester, FakeBackend backend, MemoryStore store, String type) async {
  backend.on('GET', '/me', 200, user(type));
  store.values[SessionController.tokenKey] = 'session-1';
  final s = session(backend, store, FakeBiometric(available: false));
  await s.start();
  await tester.pumpWidget(ElderCareApp(session: s, queue: memoryQueue(s), scannerBuilder: fakeScanner));
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;
  late MemoryStore store;

  setUp(() {
    backend = FakeBackend();
    store = MemoryStore();
  });

  testWidgets('T006_taiKhoanNhanVien_vaoPhanNhanVien', (tester) async {
    await pumpSignedIn(tester, backend, store, 'STAFF');

    expect(find.byType(NavigationBar), findsOneWidget);
    expect(find.textContaining('Hôm nay,'), findsNothing);
  });

  testWidgets('T006_taiKhoanNguoiThan_vaoPhanNguoiThan', (tester) async {
    await pumpSignedIn(tester, backend, store, 'FAMILY_MEMBER');

    expect(find.text('Phần người thân: chưa có màn'), findsOneWidget);
  });

  testWidgets('T006_taiKhoanNguoiCaoTuoi_vaoHomNay', (tester) async {
    await pumpSignedIn(tester, backend, store, 'RESIDENT');

    expect(find.textContaining('Hôm nay,'), findsOneWidget);
    expect(find.byType(NavigationBar), findsNothing);
  });

  testWidgets('T006_chuaDangNhap_chonToiLa', (tester) async {
    final s = session(backend, store, FakeBiometric(available: false));
    await s.start();
    await tester.pumpWidget(ElderCareApp(session: s, queue: memoryQueue(s), scannerBuilder: fakeScanner));
    await tester.pumpAndSettle();

    expect(find.text('Nhân viên'), findsOneWidget);
    expect(find.text('Người thân'), findsOneWidget);
    expect(find.text('Người cao tuổi: quét mã QR'), findsOneWidget);
  });

  testWidgets('FR_014a_mayDaGanNguoiCaoTuoi_vaoThangManQuetMa', (tester) async {
    store.values[SessionController.deviceTokenKey] = 'device-token-1';
    final s = session(backend, store, FakeBiometric(available: false));
    await s.start();
    await tester.pumpWidget(ElderCareApp(session: s, queue: memoryQueue(s), scannerBuilder: fakeScanner));
    await tester.pumpAndSettle();

    expect(find.text('Đặt mã QR nhân viên đưa vào trong khung.'), findsOneWidget);
    expect(find.text('Nhân viên'), findsNothing);
  });
}
