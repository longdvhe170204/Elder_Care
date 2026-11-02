import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/early_unlock/early_unlock_screen.dart';

import '../../support/fake_backend.dart';

// 002 T046 (tên logic mo_khoa_som_test; FR-019c): Trưởng tầng, Người phụ trách ca thấy tài khoản cùng tầng và ca đang
// Khóa tạm; phải xác nhận đã gặp trực tiếp và nhập lý do; lệnh gửi If-Match theo version.

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: EarlyUnlockScreen(api: backend.client())),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/early-unlock-candidates', 200, {
      'items': [
        {'accountId': 21, 'fullName': '[Họ tên NV 2]', 'lockedUntil': '2026-10-27T01:15:00Z', 'version': 3},
      ],
    });
  });

  testWidgets('FR_019c_danhSach_hienTen_khoaTamToi', (tester) async {
    await pumpScreen(tester, backend);

    expect(find.text('[Họ tên NV 2]'), findsOneWidget);
    expect(find.text('Khóa tạm tới 08:15'), findsOneWidget);
  });

  testWidgets('FR_019c_chuaGapTrucTiep_hoacThieuLyDo_khongGui', (tester) async {
    await pumpScreen(tester, backend);
    await tester.tap(find.text('[Họ tên NV 2]'));
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(FilledButton, 'Mở khóa sớm'));
    await tester.pumpAndSettle();

    expect(find.text('Cần gặp trực tiếp người này trước khi mở khóa.'), findsOneWidget);
    expect(find.text('Nhập lý do'), findsOneWidget);
    expect(backend.calls('POST', '/accounts/21/early-unlock'), isEmpty);
  });

  testWidgets('FR_019c_moKhoa_guiLyDoXacMinh_ifMatch_boThe', (tester) async {
    backend.on('POST', '/accounts/21/early-unlock', 200, {'id': 21, 'status': 'ACTIVE'});
    await pumpScreen(tester, backend);
    await tester.tap(find.text('[Họ tên NV 2]'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Tôi đã gặp trực tiếp người này'));
    await tester.enterText(find.widgetWithText(TextField, 'Lý do'), 'Gặp ở quầy trực');
    await tester.tap(find.widgetWithText(FilledButton, 'Mở khóa sớm'));
    await tester.pumpAndSettle();

    final call = backend.calls('POST', '/accounts/21/early-unlock').single;
    expect(call.headers['If-Match'], '"3"');
    expect(jsonDecode(call.body), {'reason': 'Gặp ở quầy trực', 'verifiedInPerson': true});
    expect(find.text('Đã mở khóa cho [Họ tên NV 2].'), findsOneWidget);
    expect(find.text('Khóa tạm tới 08:15'), findsNothing);
  });

  testWidgets('FR_019c_backendTuChoi_hienCauBackend', (tester) async {
    backend.on('POST', '/accounts/21/early-unlock', 403, {
      'status': 403,
      'errorCode': 'PERMISSION_DENIED',
      'detail': 'Chỉ Quản lý viện, Trưởng tầng hoặc Người phụ trách ca cùng tầng và ca mở khóa sớm (FR-019c)',
    });
    await pumpScreen(tester, backend);
    await tester.tap(find.text('[Họ tên NV 2]'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tôi đã gặp trực tiếp người này'));
    await tester.enterText(find.widgetWithText(TextField, 'Lý do'), 'Thử');
    await tester.tap(find.widgetWithText(FilledButton, 'Mở khóa sớm'));
    await tester.pumpAndSettle();

    expect(find.textContaining('cùng tầng và ca mở khóa sớm'), findsOneWidget);
  });
}
