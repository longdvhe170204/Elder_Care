import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/visits/visit_registration_screen.dart';

import '../../support/fake_backend.dart';

// 012 T025 (US4; FR-029 → FR-031, FR-035; SC-004 đăng ký trong 2 phút): người thân chọn ngày, khung còn chỗ, thêm người
// đi cùng rồi đăng ký; đạt thì Đã duyệt ngay, không đạt thì hiện mọi lý do, không tạo lượt. Lượt Đã duyệt của mình hủy
// được trước giờ bắt đầu. Chỉ trực tuyến.

const _slots = [
  {'startTime': '09:00', 'endTime': '11:00', 'capacity': 20, 'seatsLeft': 4},
  {'startTime': '15:00', 'endTime': '17:00', 'capacity': 20, 'seatsLeft': 0},
];

Map<String, dynamic> visit({int id = 81, String status = 'APPROVED'}) => {
  'id': id,
  'residentId': 5,
  'familyMemberId': 31,
  'date': '2026-11-06',
  'startTime': '09:00',
  'endTime': '11:00',
  'companions': <Object>[],
  'visitorCount': 1,
  'status': status,
};

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: VisitRegistrationScreen(
          api: api,
          residentId: 5,
          residentName: 'Nguyễn Thị Lan',
          today: DateTime(2026, 11, 5),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> tapText(WidgetTester tester, String text) async {
  await tester.ensureVisible(find.text(text).first);
  await tester.pumpAndSettle();
  await tester.tap(find.text(text).first);
  await tester.pumpAndSettle();
}

Future<void> enter(WidgetTester tester, String label, String value) async {
  final field = find.widgetWithText(TextField, label);
  await tester.ensureVisible(field);
  await tester.enterText(field, value);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_031_dangKyKhungConCho_daDuyetNgay', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/visit-slots', 200, _slots)
      ..on('GET', '/visits', 200, <Object>[])
      ..on('POST', '/visits', 201, visit());
    await pumpScreen(tester, fake.client());

    await tapText(tester, '06/11');
    expect(fake.calls('GET', '/visit-slots').last.url.queryParameters['date'], '2026-11-06');
    expect(find.text('09:00–11:00, còn 4 chỗ'), findsOneWidget);
    expect(find.text('15:00–17:00, hết chỗ'), findsOneWidget);

    await tapText(tester, 'Đăng ký thăm');
    expect(find.text('Chọn khung giờ'), findsOneWidget);
    expect(fake.calls('POST', '/visits'), isEmpty);

    await tapText(tester, '09:00–11:00, còn 4 chỗ');
    await tapText(tester, 'Thêm người đi cùng');
    await enter(tester, 'Họ tên người đi cùng 1', 'Trần Hoa');
    await enter(tester, 'Quan hệ người đi cùng 1', 'Cháu');
    await tapText(tester, 'Đăng ký thăm');

    final post = fake.calls('POST', '/visits').single;
    expect(jsonDecode(post.body), {
      'residentId': 5,
      'date': '2026-11-06',
      'startTime': '09:00',
      'endTime': '11:00',
      'companions': [
        {'fullName': 'Trần Hoa', 'relationship': 'Cháu'},
      ],
    });
    expect(post.headers['Idempotency-Key'], matches(RegExp(r'^[0-9a-f-]{36}$')));
    expect(find.text('Đã đăng ký, lượt thăm đã được duyệt'), findsOneWidget);
  });

  testWidgets('FR_030_khongDat_hienMoiLyDo', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/visit-slots', 200, _slots)
      ..on('GET', '/visits', 200, <Object>[])
      ..on('POST', '/visits', 422, {
        'status': 422,
        'errorCode': 'ISOLATION_ACTIVE',
        'unmetConditions': [
          {'code': 'ISOLATION_ACTIVE', 'description': 'Khu đang khoanh vùng'},
          {'code': 'RESIDENT_NOT_PRESENT', 'description': 'Người cao tuổi không có mặt tại viện'},
        ],
      });
    await pumpScreen(tester, fake.client());

    await tapText(tester, '06/11');
    await tapText(tester, '09:00–11:00, còn 4 chỗ');
    await tapText(tester, 'Đăng ký thăm');

    expect(find.text('Chưa đăng ký được:'), findsOneWidget);
    expect(find.text('Khu đang khoanh vùng'), findsOneWidget);
    expect(find.text('Người cao tuổi không có mặt tại viện'), findsOneWidget);
  });

  testWidgets('FR_035_huyLuotCuaMinh', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/visit-slots', 200, _slots)
      ..on('GET', '/visits', 200, [visit()])
      ..on('POST', '/visits/81/cancel', 200, {'id': 81});
    await pumpScreen(tester, fake.client());

    expect(find.text('06/11/2026, 09:00–11:00'), findsOneWidget);
    expect(find.text('Đã duyệt'), findsOneWidget);
    fake.on('GET', '/visits', 200, [visit(status: 'CANCELLED')]);
    await tapText(tester, 'Hủy lượt');

    expect(jsonDecode(fake.calls('POST', '/visits/81/cancel').single.body), <String, dynamic>{});
    expect(find.text('Hủy'), findsOneWidget);
  });
}
