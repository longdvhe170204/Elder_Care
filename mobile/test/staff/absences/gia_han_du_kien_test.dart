import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/absences/absences_screen.dart';

import '../../support/fake_backend.dart';

// 004 T056 (US6; FR-048, FR-049): nhân viên xem lượt vắng của người cao tuổi và gia hạn dự kiến trở lại trên app (chỉ trực
// tuyến); gia hạn bắt buộc ngày giờ và lý do; 409 báo tại thẻ. Lượt đã đóng không có nút gia hạn.

Map<String, dynamic> absence({int id = 61, String status = 'OPEN'}) => {
  'id': id,
  'residentId': 5,
  'absenceType': 'HOME_VISIT',
  'departedAt': '2026-11-03T01:00:00Z',
  'expectedReturnAt': '2026-11-08T10:00:00Z',
  'status': status,
  if (status == 'CLOSED') 'returnedAt': '2026-11-07T10:00:00Z',
};

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  tester.view.physicalSize = const Size(800, 2400);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: AbsencesScreen(api: backend.client(), residentId: 5)),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> openForm(WidgetTester tester) async {
  await tester.tap(find.text('Gia hạn dự kiến trở lại'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_048_hienLuotVangCuaNguoiCaoTuoi_vaChiLuotMoCoNutGiaHan', (tester) async {
    final backend = FakeBackend()..on('GET', '/absences', 200, [absence(), absence(id: 62, status: 'CLOSED')]);
    await pumpScreen(tester, backend);

    expect(backend.calls('GET', '/absences').single.url.queryParameters['residentId'], '5');
    expect(find.text('Về nhà'), findsNWidgets(2));
    expect(find.text('Dự kiến trở lại 08/11/2026 17:00'), findsOneWidget);
    expect(find.text('Gia hạn dự kiến trở lại'), findsOneWidget);
    expect(find.text('Đã trở lại 07/11/2026 17:00'), findsOneWidget);
  });

  testWidgets('FR_049_giaHan_batBuocNgayGioVaLyDo', (tester) async {
    final backend = FakeBackend()..on('GET', '/absences', 200, [absence()]);
    await pumpScreen(tester, backend);
    await openForm(tester);

    await tester.tap(find.text('Lưu gia hạn'));
    await tester.pumpAndSettle();

    expect(find.text('Nhập ngày và giờ dự kiến trở lại'), findsOneWidget);
    expect(find.text('Nhập lý do'), findsOneWidget);
    expect(backend.calls('POST', '/absences/61/extend-expected-return'), isEmpty);
  });

  testWidgets('FR_049_giaHan_guiThoiDiemVaLyDo_roiTaiLaiDanhSach', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/absences', 200, [absence()])
      ..on('POST', '/absences/61/extend-expected-return', 200);
    await pumpScreen(tester, backend);
    await openForm(tester);

    await tester.enterText(find.widgetWithText(TextField, 'Ngày dự kiến trở lại (dd/MM/yyyy)'), '12/11/2026');
    await tester.enterText(find.widgetWithText(TextField, 'Giờ dự kiến trở lại (HH:mm)'), '10:00');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do gia hạn'), 'Bệnh của người thân');
    await tester.tap(find.text('Lưu gia hạn'));
    await tester.pumpAndSettle();

    final call = backend.calls('POST', '/absences/61/extend-expected-return').single;
    expect(jsonDecode(call.body), {'expectedReturnAt': '2026-11-12T10:00:00+07:00', 'reason': 'Bệnh của người thân'});
    expect(call.headers['Idempotency-Key'], matches(RegExp(r'^[0-9a-f-]{36}$')));
    expect(backend.calls('GET', '/absences'), hasLength(2));
  });

  testWidgets('FR_049_409_baoTaiThe', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/absences', 200, [absence()])
      ..on('POST', '/absences/61/extend-expected-return', 409, {
        'errorCode': 'ABSENCE_NOT_OPEN',
        'detail': 'Lượt vắng đã đóng',
      });
    await pumpScreen(tester, backend);
    await openForm(tester);
    await tester.enterText(find.widgetWithText(TextField, 'Ngày dự kiến trở lại (dd/MM/yyyy)'), '12/11/2026');
    await tester.enterText(find.widgetWithText(TextField, 'Giờ dự kiến trở lại (HH:mm)'), '10:00');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do gia hạn'), 'Bệnh của người thân');
    await tester.tap(find.text('Lưu gia hạn'));
    await tester.pumpAndSettle();

    expect(find.text('Lượt vắng đã đóng'), findsOneWidget);
  });
}
