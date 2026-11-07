import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/right_change_requests/right_change_requests_screen.dart';

import '../../support/fake_backend.dart';

// 012 T017 (US3; FR-015 a, FR-016, FR-020b): người đại diện xác nhận hoặc từ chối yêu cầu thay đổi quyền trên app người
// thân. Chỉ trực tuyến; từ chối bắt buộc lý do; yêu cầu trái mong muốn của người cao tuổi không có nút Xác nhận (chỉ
// Quản lý viện duyệt); 409 báo tại thẻ.

Map<String, dynamic> request({
  int id = 71,
  String right = 'VIEW_COSTS',
  bool newValue = true,
  bool applied = false,
  int? conflictingWishId,
  Map<String, dynamic>? newPicker,
}) => {
  'id': id,
  'residentId': 5,
  'type': 'RIGHT',
  'relationshipId': 12,
  'right': right,
  'newValue': newValue,
  'newPicker': ?newPicker,
  'reason': 'Gia đình đề nghị tại quầy',
  'status': 'PENDING_CONFIRMATION',
  'source': 'COUNTER',
  'appliedOnCreation': applied,
  'conflictingWishId': conflictingWishId,
};

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: RightChangeRequestsScreen(api: api)),
    ),
  );
  await tester.pumpAndSettle();
}

Finder button(String label) =>
    find.ancestor(of: find.text(label), matching: find.byWidgetPredicate((w) => w is ButtonStyleButton));

Future<void> tapButton(WidgetTester tester, String label) async {
  await tester.ensureVisible(button(label).first);
  await tester.pumpAndSettle();
  await tester.tap(button(label).first);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_015a_nguoiDaiDienXacNhan_yeuCauBienKhoiDanhSach', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/right-change-requests', 200, [
        request(),
        request(id: 72, right: 'PICKUP_ALLOWED', newValue: false, applied: true),
      ])
      ..on('POST', '/right-change-requests/71/confirm', 200, {...request(), 'status': 'EFFECTIVE'});
    await pumpScreen(tester, backend.client());

    final list = backend.calls('GET', '/right-change-requests').single;
    expect(list.url.queryParameters['status'], 'PENDING_CONFIRMATION');
    expect(find.text('Bật quyền Xem chi phí cho người thân'), findsOneWidget);
    expect(find.text('Tắt quyền Được phép đón cho người thân'), findsOneWidget);
    expect(find.text('Đã áp dụng khi lập'), findsOneWidget);

    await tapButton(tester, 'Xác nhận');

    final confirm = backend.calls('POST', '/right-change-requests/71/confirm').single;
    expect(jsonDecode(confirm.body), <String, dynamic>{});
    expect(confirm.headers['Idempotency-Key'], matches(RegExp(r'^[0-9a-f-]{36}$')));
    expect(find.text('Bật quyền Xem chi phí cho người thân'), findsNothing);
    expect(find.text('Đã xác nhận yêu cầu'), findsOneWidget);
  });

  testWidgets('FR_015_tuChoi_batBuocLyDo', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/right-change-requests', 200, [request()])
      ..on('POST', '/right-change-requests/71/reject', 200, {...request(), 'status': 'REJECTED'});
    await pumpScreen(tester, backend.client());

    await tapButton(tester, 'Từ chối');
    await tapButton(tester, 'Xác nhận từ chối');
    expect(find.text('Nhập lý do'), findsOneWidget);
    expect(backend.calls('POST', '/right-change-requests/71/reject'), isEmpty);

    await tester.enterText(find.widgetWithText(TextField, 'Lý do từ chối'), 'Chưa bàn với gia đình');
    await tapButton(tester, 'Xác nhận từ chối');

    final reject = backend.calls('POST', '/right-change-requests/71/reject').single;
    expect(jsonDecode(reject.body), {'reason': 'Chưa bàn với gia đình'});
    expect(find.text('Đã từ chối yêu cầu'), findsOneWidget);
  });

  testWidgets('FR_020b_traiMongMuon_khongCoNutXacNhan_vaNguoiDonMoiCoTen', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/right-change-requests', 200, [
        request(
          right: 'PICKUP_ALLOWED',
          conflictingWishId: 91,
          newPicker: {'fullName': 'Lê Văn X', 'relationship': 'OTHER'},
        ),
      ]);
    await pumpScreen(tester, backend.client());

    expect(find.text('Bật quyền Được phép đón cho Lê Văn X'), findsOneWidget);
    expect(find.text('Trái mong muốn của người cao tuổi, chờ Quản lý viện duyệt.'), findsOneWidget);
    expect(button('Xác nhận'), findsNothing);
    expect(button('Từ chối'), findsOneWidget);
  });

  testWidgets('FR_016_nguoiKhacDaXuLy_409BaoTaiThe', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/right-change-requests', 200, [request()])
      ..on('POST', '/right-change-requests/71/confirm', 409, {
        'status': 409,
        'errorCode': 'STATE_CHANGED',
        'title': 'Trạng thái đã đổi',
        'detail': 'Yêu cầu đã có kết quả',
      });
    await pumpScreen(tester, backend.client());

    await tapButton(tester, 'Xác nhận');

    expect(find.text('Yêu cầu đã có kết quả'), findsOneWidget);
    expect(find.text('Bật quyền Xem chi phí cho người thân'), findsOneWidget);
  });

  testWidgets('trong_khongCoYeuCauCho', (tester) async {
    final backend = FakeBackend()..on('GET', '/right-change-requests', 200, <Object>[]);
    await pumpScreen(tester, backend.client());

    expect(find.text('Không có yêu cầu nào chờ xác nhận.'), findsOneWidget);
  });
}
