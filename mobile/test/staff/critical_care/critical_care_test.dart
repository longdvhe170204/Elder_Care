import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/critical_care/critical_care_screen.dart';

import '../../support/fake_backend.dart';

// 007 T046 (US11; FR-050a → FR-050d): ghi dấu, xác nhận lại nguyện vọng, thực hiện lựa chọn trên app nhân viên.

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend, {int? alertId, int? flagId}) async {
  tester.view.physicalSize = const Size(800, 3000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: CriticalCareScreen(api: backend.client(), residentId: 5, alertId: alertId, criticalFlagId: flagId),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;

  setUp(() => backend = FakeBackend());

  testWidgets('FR_050a_thieuLoaiHoacNhanDinh_baoTaiO_khongGui', (tester) async {
    await pumpScreen(tester, backend);
    await tester.tap(find.widgetWithText(FilledButton, 'Ghi dấu nguy kịch'));
    await tester.pumpAndSettle();

    expect(find.text('Chọn loại dấu'), findsOneWidget);
    expect(find.text('Nhập nhận định'), findsOneWidget);
    expect(backend.calls('POST', '/residents/5/critical-flags'), isEmpty);
  });

  testWidgets('FR_050a_ghiDauChinhThuc_guiLoaiVaNhanDinh', (tester) async {
    backend.on('POST', '/residents/5/critical-flags', 201, {'flagId': 9, 'alertId': 20});
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Dấu chính thức'));
    await tester.enterText(find.widgetWithText(TextField, 'Nhận định'), 'Suy hô hấp nặng');
    await tester.tap(find.widgetWithText(FilledButton, 'Ghi dấu nguy kịch'));
    await tester.pumpAndSettle();

    final call = backend.calls('POST', '/residents/5/critical-flags').single;
    expect(jsonDecode(call.body), {'type': 'OFFICIAL', 'assessment': 'Suy hô hấp nặng'});
    expect(find.text('Đã ghi dấu nguy kịch và tạo cảnh báo Khẩn cấp.'), findsOneWidget);
  });

  testWidgets('FR_050a_xacNhanDauTam_goiDungMaDau', (tester) async {
    backend.on('POST', '/critical-flags/9/confirm', 200);
    await pumpScreen(tester, backend, alertId: 20, flagId: 9);
    await tester.tap(find.text('Xác nhận dấu tạm'));
    await tester.pumpAndSettle();

    expect(backend.calls('POST', '/critical-flags/9/confirm'), hasLength(1));
    expect(find.text('Đã xác nhận dấu.'), findsOneWidget);
  });

  testWidgets('FR_050c_xacNhanLai_guiNguoiLienHeCachVaKetQua', (tester) async {
    backend.on('POST', '/alerts/20/wish-reconfirmation', 201);
    await pumpScreen(tester, backend, alertId: 20, flagId: 9);
    await tester.enterText(find.widgetWithText(TextField, 'Người đã liên hệ'), 'Trần Thị B');
    await tester.tap(find.text('Gọi điện'));
    await tester.tap(find.text('Giữ nguyện vọng'));
    await tester.tap(find.widgetWithText(FilledButton, 'Ghi kết quả xác nhận lại'));
    await tester.pumpAndSettle();

    final body = jsonDecode(backend.calls('POST', '/alerts/20/wish-reconfirmation').single.body) as Map;
    expect(body['contactedPerson'], {'fullName': 'Trần Thị B'});
    expect(body['channel'], 'PHONE_CALL');
    expect(body['result'], 'KEPT');
  });

  testWidgets('FR_050d_chuyenBenhVien_canNoiDenVaNguoiDiCung_409HienCauBackend', (tester) async {
    backend.on('POST', '/alerts/20/execute-choice', 409, {
      'status': 409,
      'errorCode': 'CONDITION_NOT_MET',
      'detail': 'Chưa xác nhận lại nguyện vọng',
    });
    await pumpScreen(tester, backend, alertId: 20, flagId: 9);
    await tester.tap(find.text('Chuyển bệnh viện'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Thực hiện lựa chọn'));
    await tester.pumpAndSettle();
    expect(find.text('Nhập nơi chuyển đến'), findsOneWidget);
    expect(backend.calls('POST', '/alerts/20/execute-choice'), isEmpty);

    await tester.enterText(find.widgetWithText(TextField, 'Bệnh viện chuyển đến'), 'Bệnh viện X');
    await tester.enterText(find.widgetWithText(TextField, 'Người đi cùng (họ tên, quan hệ hoặc chức danh)'), 'Con gái');
    await tester.tap(find.widgetWithText(FilledButton, 'Thực hiện lựa chọn'));
    await tester.pumpAndSettle();

    final body = jsonDecode(backend.calls('POST', '/alerts/20/execute-choice').single.body) as Map;
    expect((body['transferOut'] as Map)['transferUnit'], 'Bệnh viện X');
    expect(find.text('Chưa xác nhận lại nguyện vọng'), findsOneWidget);
  });
}
