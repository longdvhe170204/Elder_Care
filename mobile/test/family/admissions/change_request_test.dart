import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/admissions/change_request_screen.dart';

import '../../support/fake_backend.dart';

// 004 T047 (US5; FR-037, FR-039): người đại diện gửi yêu cầu thay đổi mức chăm sóc hoặc loại lưu trú ở Nháp; thiếu trường
// báo tại ô; 422 của backend hiện tại màn; chỉ trực tuyến.

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  tester.view.physicalSize = const Size(800, 2400);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: ChangeRequestScreen(api: backend.client())),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> choose(WidgetTester tester, String field, String option) async {
  await tester.tap(find.text(field));
  await tester.pumpAndSettle();
  await tester.tap(find.text(option).last);
  await tester.pumpAndSettle();
}

Future<void> fillValid(WidgetTester tester) async {
  await choose(tester, 'Hợp đồng', 'Hợp đồng HD-031');
  await choose(tester, 'Nội dung thay đổi', 'Mức chăm sóc');
  await choose(tester, 'Giá trị mới', 'Đặc biệt');
  await tester.enterText(find.widgetWithText(TextField, 'Ngày hiệu lực mong muốn (dd/MM/yyyy)'), '20/11/2026');
  await tester.enterText(find.widgetWithText(TextField, 'Lý do'), 'Sau đợt đánh giá mới');
}

FakeBackend backendWithContract() => FakeBackend()
  ..on('GET', '/contracts', 200, [
    {'id': 31, 'contractNumber': 'HD-031', 'status': 'ACTIVE'},
  ]);

void main() {
  testWidgets('FR_037_thieuTruong_baoTaiO_vaChuaGui', (tester) async {
    final backend = backendWithContract();
    await pumpScreen(tester, backend);

    await tester.tap(find.text('Gửi yêu cầu'));
    await tester.pumpAndSettle();

    expect(find.text('Chọn hợp đồng'), findsOneWidget);
    expect(find.text('Chọn nội dung thay đổi'), findsOneWidget);
    expect(find.text('Nhập ngày hiệu lực mong muốn'), findsOneWidget);
    expect(find.text('Nhập lý do'), findsOneWidget);
    expect(backend.calls('POST', '/residency-change-requests'), isEmpty);
  });

  testWidgets('FR_039_guiYeuCauDoiMuc_taoONhap_vaBaoHanhChinhHoanThien', (tester) async {
    final backend = backendWithContract()..on('POST', '/residency-change-requests', 201, {'id': 12, 'status': 'DRAFT'});
    await pumpScreen(tester, backend);
    await fillValid(tester);

    await tester.tap(find.text('Gửi yêu cầu'));
    await tester.pumpAndSettle();

    final call = backend.calls('POST', '/residency-change-requests').single;
    expect(jsonDecode(call.body), {
      'contractId': 31,
      'changeTypes': ['CARE_LEVEL'],
      'newValue': {'careLevel': 'SPECIAL'},
      'requestedEffectiveDate': '2026-11-20',
      'reason': 'Sau đợt đánh giá mới',
    });
    expect(call.headers['Idempotency-Key'], matches(RegExp(r'^[0-9a-f-]{36}$')));
    expect(find.text('Đã gửi yêu cầu, Hành chính sẽ hoàn thiện và gửi duyệt.'), findsOneWidget);
  });

  testWidgets('FR_040_422_hienLoiCuaBackend', (tester) async {
    final backend = backendWithContract()
      ..on('POST', '/residency-change-requests', 422, {
        'errorCode': 'INVALID_VALUE',
        'detail': 'Ngày hiệu lực sớm hơn ngày bắt đầu hợp đồng',
      });
    await pumpScreen(tester, backend);
    await fillValid(tester);

    await tester.tap(find.text('Gửi yêu cầu'));
    await tester.pumpAndSettle();

    expect(find.text('Ngày hiệu lực sớm hơn ngày bắt đầu hợp đồng'), findsOneWidget);
  });
}
