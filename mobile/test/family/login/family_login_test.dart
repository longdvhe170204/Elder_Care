import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';

import '../../support/fake_backend.dart';

// 002 T047 (tên logic dang_nhap_nguoi_than_test; FR-006, FR-050): người thân đăng nhập ở màn đầu chung, kênh MOBILE_APP
// (backend gán kênh FAMILY_APP theo loại tài khoản); lần đầu buộc đổi mật khẩu, đổi xong vào phần người thân (phiên giữ
// nguyên, khác người cao tuổi).

void main() {
  late FakeBackend backend;
  late MemoryStore store;

  setUp(() {
    backend = FakeBackend();
    store = MemoryStore();
  });

  testWidgets('FR_006_nguoiThanDangNhapLanDau_doiMatKhau_vaoPhanNguoiThan', (tester) async {
    backend.on('POST', '/auth/login', 200, {
      'token': 'family-1',
      'mustChangePassword': true,
      'user': user('FAMILY_MEMBER', mustChangePassword: true),
    });
    backend.on('POST', '/auth/change-password', 204);
    final s = session(backend, store, FakeBiometric());
    await s.start();
    await tester.pumpWidget(ElderCareApp(session: s, queue: memoryQueue(s)));
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextField, 'Tên đăng nhập'), 'nguoi-than-1');
    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu'), 'tam12345');
    await tester.tap(find.widgetWithText(FilledButton, 'Đăng nhập'));
    await tester.pumpAndSettle();

    expect(
      (jsonDecode(backend.calls('POST', '/auth/login').single.body) as Map<String, dynamic>)['channel'],
      'MOBILE_APP',
    );
    expect(find.text('Đặt mật khẩu mới'), findsOneWidget);
    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu nhân viên đưa'), 'tam12345');
    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu mới'), 'moi12345');
    await tester.tap(find.text('Lưu mật khẩu mới'));
    await tester.pumpAndSettle();

    expect(find.text('Phần người thân: chưa có màn'), findsOneWidget);
    expect(store.values[SessionController.tokenKey], 'family-1');
  });

  testWidgets('FR_006_matKhauMoiChuaDat_baoTaiO', (tester) async {
    backend.on('POST', '/auth/login', 200, {
      'token': 'family-1',
      'user': user('FAMILY_MEMBER', mustChangePassword: true),
    });
    backend.on('POST', '/auth/change-password', 422, {
      'status': 422,
      'errorCode': 'PASSWORD_POLICY_NOT_MET',
      'detail': 'Mật khẩu mới chưa đạt yêu cầu',
    });
    final s = session(backend, store, FakeBiometric());
    await s.start();
    await tester.pumpWidget(ElderCareApp(session: s, queue: memoryQueue(s)));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Tên đăng nhập'), 'nguoi-than-1');
    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu'), 'tam12345');
    await tester.tap(find.widgetWithText(FilledButton, 'Đăng nhập'));
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu nhân viên đưa'), 'tam12345');
    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu mới'), '123');
    await tester.tap(find.text('Lưu mật khẩu mới'));
    await tester.pumpAndSettle();

    expect(find.text('Mật khẩu mới chưa đạt yêu cầu'), findsOneWidget);
  });
}
