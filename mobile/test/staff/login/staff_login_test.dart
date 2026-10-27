import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';

import '../../support/fake_backend.dart';

// 002 T046 (tên logic dang_nhap_thiet_bi_chung_test; FR-014a, FR-013): nhân viên đăng nhập trên máy dùng chung, kênh
// STAFF_APP kèm mã thiết bị; không hỏi sinh trắc học; đăng xuất về màn chọn để người sau đăng nhập.

Future<SessionController> pumpApp(WidgetTester tester, FakeBackend backend, MemoryStore store) async {
  final s = session(backend, store, FakeBiometric());
  await s.start();
  await tester.pumpWidget(ElderCareApp(session: s, queue: memoryQueue(s)));
  await tester.pumpAndSettle();
  return s;
}

Future<void> login(WidgetTester tester, String username, String password) async {
  await tester.tap(find.text('Nhân viên'));
  await tester.pumpAndSettle();
  await tester.enterText(find.widgetWithText(TextField, 'Tên đăng nhập'), username);
  await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu'), password);
  await tester.tap(find.widgetWithText(FilledButton, 'Đăng nhập'));
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;
  late MemoryStore store;

  setUp(() {
    backend = FakeBackend();
    store = MemoryStore();
  });

  testWidgets('FR_014a_nhanVienDangNhap_kenhStaffApp_maThietBi_vaoPhanNhanVien', (tester) async {
    backend.on('POST', '/auth/login', 200, {'token': 'staff-1', 'user': user('STAFF')});
    await pumpApp(tester, backend, store);

    expect(find.textContaining('kết thúc phiên của người trước'), findsNothing);
    await tester.tap(find.text('Nhân viên'));
    await tester.pumpAndSettle();
    expect(find.textContaining('kết thúc phiên của người trước'), findsOneWidget);
    await tester.enterText(find.widgetWithText(TextField, 'Tên đăng nhập'), ' dieu-duong-1 ');
    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu'), 'matkhau123');
    await tester.tap(find.widgetWithText(FilledButton, 'Đăng nhập'));
    await tester.pumpAndSettle();

    final call = backend.calls('POST', '/auth/login').single;
    expect(jsonDecode(call.body), {'username': 'dieu-duong-1', 'password': 'matkhau123', 'channel': 'STAFF_APP'});
    expect(call.headers['X-Device-Id'], isNotNull);
    expect(store.values[SessionController.tokenKey], 'staff-1');
    expect(find.byType(NavigationBar), findsOneWidget);
    expect(find.text('Mở bằng vân tay hoặc khuôn mặt?'), findsNothing);
  });

  testWidgets('FR_014_saiMatKhau_hienCauBackend_oLaiManDangNhap', (tester) async {
    backend.on('POST', '/auth/login', 401, {
      'status': 401,
      'errorCode': 'LOGIN_FAILED',
      'detail': 'Tên đăng nhập hoặc mật khẩu không đúng',
    });
    await pumpApp(tester, backend, store);

    await login(tester, 'dieu-duong-1', 'sai');

    expect(find.text('Tên đăng nhập hoặc mật khẩu không đúng'), findsOneWidget);
    expect(find.byType(NavigationBar), findsNothing);
  });

  testWidgets('FR_013_dangXuat_baoBackend_veManChon', (tester) async {
    backend.on('POST', '/auth/login', 200, {'token': 'staff-1', 'user': user('STAFF')});
    backend.on('POST', '/auth/logout', 204);
    await pumpApp(tester, backend, store);
    await login(tester, 'dieu-duong-1', 'matkhau123');

    await tester.tap(find.text('Đăng xuất'));
    await tester.pumpAndSettle();

    expect(backend.calls('POST', '/auth/logout'), hasLength(1));
    expect(store.values[SessionController.tokenKey], isNull);
    expect(find.text('Người thân'), findsOneWidget);
  });
}
