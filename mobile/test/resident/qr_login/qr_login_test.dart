import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';
import 'package:mobile/resident/resident_root.dart';

import '../../support/fake_backend.dart';

// 002 T097 (tên logic dang_nhap_qr_test; FR-069, FR-070, FR-006, FR-013a; DESIGN.md 15.6 "Đăng nhập").

Widget fakeScanner(void Function(String code) onCode) =>
    Center(child: ElevatedButton(onPressed: () => onCode('qr-123'), child: const Text('Giả quét mã')));

Future<SessionController> pumpApp(WidgetTester tester, FakeBackend backend, MemoryStore store,
    {FakeBiometric? biometric}) async {
  final s = session(backend, store, biometric ?? FakeBiometric(available: false));
  await s.start();
  await tester.pumpWidget(ResidentRoot(session: s, scannerBuilder: fakeScanner));
  await tester.pumpAndSettle();
  return s;
}

void main() {
  late FakeBackend backend;
  late MemoryStore store;

  setUp(() {
    backend = FakeBackend();
    store = MemoryStore();
  });

  testWidgets('FR_069_quetMa_guiMaVaMaThietBi_luuToken_vaoHomNay', (tester) async {
    backend.on('POST', '/auth/qr-login', 200, loginResponse());
    await pumpApp(tester, backend, store);

    expect(find.text('Đặt mã QR nhân viên đưa vào trong khung.'), findsOneWidget);
    await tester.tap(find.text('Giả quét mã'));
    await tester.pumpAndSettle();

    final call = backend.calls('POST', '/auth/qr-login').single;
    expect(jsonDecode(call.body), {'code': 'qr-123'});
    expect(call.headers['X-Device-Id'], matches(RegExp(r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$')));
    expect(store.values[SessionController.tokenKey], 'session-1');
    expect(store.values[SessionController.deviceTokenKey], 'device-token-1');
    expect(find.textContaining('Hôm nay'), findsOneWidget);
  });

  testWidgets('FR_069_maDaDungHoacHetHan_baoLoi_oLaiManQuet', (tester) async {
    backend.on('POST', '/auth/qr-login', 401, {'status': 401, 'errorCode': 'INVALID_QR_CODE'});
    await pumpApp(tester, backend, store);

    await tester.tap(find.text('Giả quét mã'));
    await tester.pumpAndSettle();

    expect(find.text('Mã này không dùng được nữa. Nhờ nhân viên tạo mã mới.'), findsOneWidget);
    expect(find.text('Đặt mã QR nhân viên đưa vào trong khung.'), findsOneWidget);
    expect(store.values[SessionController.tokenKey], isNull);
  });

  testWidgets('FR_069_dungMatKhau_kenhAppNguoiCaoTuoi', (tester) async {
    backend.on('POST', '/auth/login', 200, loginResponse());
    await pumpApp(tester, backend, store);

    await tester.tap(find.text('Dùng mật khẩu'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Tên đăng nhập'), 'nct-41');
    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu'), 'Mat-khau-1');
    await tester.tap(find.text('Đăng nhập'));
    await tester.pumpAndSettle();

    expect(jsonDecode(backend.calls('POST', '/auth/login').single.body),
        {'username': 'nct-41', 'password': 'Mat-khau-1', 'channel': 'RESIDENT_APP'});
    expect(find.textContaining('Hôm nay'), findsOneWidget);
  });

  testWidgets('FR_006_buocDoiMatKhau_doiXong_dangNhapLai', (tester) async {
    backend.on('POST', '/auth/qr-login', 200, loginResponse(mustChangePassword: true));
    backend.on('POST', '/auth/change-password', 204);
    await pumpApp(tester, backend, store);

    await tester.tap(find.text('Giả quét mã'));
    await tester.pumpAndSettle();
    expect(find.text('Đặt mật khẩu mới'), findsOneWidget);

    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu nhân viên đưa'), 'Tam-123456');
    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu mới'), 'Moi-12345678');
    await tester.tap(find.text('Lưu mật khẩu mới'));
    await tester.pumpAndSettle();

    expect(jsonDecode(backend.calls('POST', '/auth/change-password').single.body),
        {'currentPassword': 'Tam-123456', 'newPassword': 'Moi-12345678'});
    expect(find.text('Đã đổi mật khẩu. Quét mã mới hoặc dùng mật khẩu mới để vào.'), findsOneWidget);
  });

  testWidgets('FR_013a_moLaiApp_phienCon_vaoThangHomNay', (tester) async {
    store.values[SessionController.tokenKey] = 'session-1';
    backend.on('GET', '/me', 200, residentUser());
    await pumpApp(tester, backend, store);

    expect(backend.calls('GET', '/me').single.headers['Authorization'], 'Bearer session-1');
    expect(find.textContaining('Hôm nay'), findsOneWidget);
    expect(backend.calls('POST', '/auth/qr-login'), isEmpty);
  });

  testWidgets('FR_013a_moAppKhiMatMang_phienCon_vanVaoHomNay', (tester) async {
    store.values[SessionController.tokenKey] = 'session-1';
    store.values[SessionController.userTypeKey] = 'resident';
    backend.down = true;
    await pumpApp(tester, backend, store);

    expect(find.textContaining('Hôm nay'), findsOneWidget);
    expect(store.values[SessionController.tokenKey], 'session-1');
  });

  testWidgets('FR_069_matMang_khiQuet_baoChuaKetNoi', (tester) async {
    await pumpApp(tester, backend, store);
    backend.down = true;

    await tester.tap(find.text('Giả quét mã'));
    await tester.pumpAndSettle();

    expect(find.text('Máy chưa kết nối mạng.'), findsOneWidget);
  });

  testWidgets('phienBiKetThuc_401_veManQuet', (tester) async {
    store.values[SessionController.tokenKey] = 'session-old';
    backend.on('GET', '/me', 401, {'status': 401});
    await pumpApp(tester, backend, store);

    expect(find.text('Đặt mã QR nhân viên đưa vào trong khung.'), findsOneWidget);
    expect(store.values[SessionController.tokenKey], isNull);
  });
}
