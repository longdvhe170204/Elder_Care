import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';
import 'package:mobile/resident/resident_root.dart';

import '../../support/fake_backend.dart';

// 002 T097 (tên logic mo_khoa_sinh_trac_test; FR-074; DESIGN.md 15.6): sau lần đăng nhập đầu hỏi một lần có mở bằng
// vân tay hoặc khuôn mặt; bật thì lần mở app sau phải qua sinh trắc học; không có mã PIN.

Widget fakeScanner(void Function(String code) onCode) =>
    Center(child: ElevatedButton(onPressed: () => onCode('qr-123'), child: const Text('Giả quét mã')));

Future<void> pumpApp(WidgetTester tester, FakeBackend backend, MemoryStore store, FakeBiometric biometric) async {
  final s = session(backend, store, biometric);
  await s.start();
  await tester.pumpWidget(ResidentRoot(session: s, scannerBuilder: fakeScanner));
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;
  late MemoryStore store;

  setUp(() {
    backend = FakeBackend();
    store = MemoryStore();
    backend.on('POST', '/auth/qr-login', 200, loginResponse());
    backend.on('GET', '/me', 200, residentUser());
  });

  testWidgets('FR_074_lanDau_hoiMotLan_Co_batRoiMoLaiPhaiQuaSinhTrac', (tester) async {
    final biometric = FakeBiometric();
    await pumpApp(tester, backend, store, biometric);
    await tester.tap(find.text('Giả quét mã'));
    await tester.pumpAndSettle();

    expect(find.text('Mở bằng vân tay hoặc khuôn mặt?'), findsOneWidget);
    await tester.tap(find.text('Có'));
    await tester.pumpAndSettle();
    expect(store.values[SessionController.biometricKey], 'on');
    expect(find.textContaining('Hôm nay'), findsOneWidget);

    // Mở lại app
    await pumpApp(tester, backend, store, biometric);
    expect(find.textContaining('Hôm nay'), findsNothing);
    expect(find.byType(TextField), findsNothing, reason: 'không có mã PIN');
    await tester.tap(find.text('Mở bằng vân tay hoặc khuôn mặt'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Hôm nay'), findsOneWidget);
  });

  testWidgets('FR_074_DeSau_khongHoiLai_khongKhoa', (tester) async {
    final biometric = FakeBiometric();
    await pumpApp(tester, backend, store, biometric);
    await tester.tap(find.text('Giả quét mã'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Để sau'));
    await tester.pumpAndSettle();

    expect(store.values[SessionController.biometricKey], isNull);
    expect(biometric.prompts, 0);

    // Đăng nhập lại trên máy này (ví dụ sau khi gỡ thiết bị): không hỏi lại
    store.values.remove(SessionController.tokenKey);
    await pumpApp(tester, backend, store, biometric);
    await tester.tap(find.text('Giả quét mã'));
    await tester.pumpAndSettle();
    expect(find.text('Mở bằng vân tay hoặc khuôn mặt?'), findsNothing);
    expect(find.textContaining('Hôm nay'), findsOneWidget);
  });

  testWidgets('FR_074_mayKhongCoSinhTrac_khongHoi', (tester) async {
    await pumpApp(tester, backend, store, FakeBiometric(available: false));
    await tester.tap(find.text('Giả quét mã'));
    await tester.pumpAndSettle();

    expect(find.text('Mở bằng vân tay hoặc khuôn mặt?'), findsNothing);
    expect(find.textContaining('Hôm nay'), findsOneWidget);
  });

  testWidgets('FR_074_sinhTracKhongDung_vanKhoa', (tester) async {
    store.values[SessionController.tokenKey] = 'session-1';
    store.values[SessionController.biometricKey] = 'on';
    await pumpApp(tester, backend, store, FakeBiometric(succeeds: false));

    await tester.tap(find.text('Mở bằng vân tay hoặc khuôn mặt'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Hôm nay'), findsNothing);
  });
}
