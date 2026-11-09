import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/nutrition/family_food_screen.dart';

import '../../support/fake_backend.dart';

// 011 T035 (US7; FR-052 → FR-057, BR-M08-04; quickstart Q42 → Q44): nhân viên chọn người cao tuổi, ghi nhận đồ ăn gia đình
// (hệ thống nêu trạng thái và lý do), rồi xử lý theo trạng thái. Đồ ăn Không sử dụng không có lệnh cho dùng lại.

const _residents = {
  'items': [
    {
      'id': 5,
      'profileCode': 'NCT-0005',
      'personalInfo': {'fullName': 'Nguyễn Thị Lan'},
    },
  ],
  'nextCursor': null,
};

Map<String, dynamic> food(int id, String status, {List<String> reasons = const []}) => {
  'id': id,
  'senderName': 'Con gái',
  'type': 'Bánh quy',
  'description': 'Bánh',
  'ingredients': <int>[],
  'ingredientsUnknown': false,
  'expiresAt': '2026-12-25T11:00:00Z',
  'storageMethod': 'Tủ mát',
  'status': status,
  'systemReasons': reasons,
};

FakeBackend backend(List<Map<String, dynamic>> foods) => FakeBackend()
  ..on('GET', '/residents', 200, _residents)
  ..on('GET', '/residents/5/family-foods', 200, foods)
  ..on('GET', '/catalogs/allergens', 200, [
    {'id': 11, 'name': 'Đậu phộng', 'group': 'FOOD', 'status': 'ACTIVE'},
    {'id': 12, 'name': 'Sữa', 'group': 'FOOD', 'status': 'ACTIVE'},
  ]);

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: FamilyFoodScreen(api: api)),
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

Future<void> openResident(WidgetTester tester) async {
  await tapText(tester, 'Nguyễn Thị Lan');
  expect(find.text('Đồ ăn gia đình của Nguyễn Thị Lan'), findsOneWidget);
}

Future<void> fill(WidgetTester tester, String label, String value) async {
  await tester.ensureVisible(find.widgetWithText(TextField, label));
  await tester.enterText(find.widgetWithText(TextField, label), value);
}

void main() {
  testWidgets('Q42: hiện trạng thái và lý do hệ thống; Không sử dụng chỉ có lệnh ghi xử lý, không có lệnh cho dùng', (
    tester,
  ) async {
    final b = backend([
      food(1, 'NOT_ALLOWED', reasons: ['ALLERGY']),
    ]);
    await pumpScreen(tester, b.client());
    await openResident(tester);

    expect(find.textContaining('Không sử dụng'), findsOneWidget);
    expect(find.text('• Trùng dị ứng của người nhận'), findsOneWidget);
    expect(find.text('Ghi xử lý'), findsOneWidget);
    expect(find.text('Cho dùng'), findsNothing);
    expect(find.text('Kết thúc'), findsNothing);
  });

  testWidgets('ghi nhận đồ ăn mới: kiểm tra trường bắt buộc và hạn dùng, rồi gửi đúng thân', (tester) async {
    final b = backend([])..on('POST', '/residents/5/family-foods', 201, food(9, 'NEEDS_CONFIRMATION'));
    await pumpScreen(tester, b.client());
    await openResident(tester);
    expect(find.text('Chưa có đồ ăn gia đình nào được ghi nhận.'), findsOneWidget);

    await tapText(tester, 'Ghi nhận đồ ăn mới');
    await tapText(tester, 'Ghi nhận');
    expect(find.text('Nhập đủ người gửi, loại đồ ăn, mô tả và cách bảo quản.'), findsOneWidget);

    await fill(tester, 'Người gửi (họ tên, quan hệ)', 'Con gái');
    await fill(tester, 'Loại đồ ăn', 'Bánh quy');
    await fill(tester, 'Mô tả', 'Bánh tự làm');
    await fill(tester, 'Cách bảo quản', 'Tủ mát');
    await fill(tester, 'Hạn dùng (ngày/tháng/năm giờ:phút)', '32/13/2026');
    await tapText(tester, 'Ghi nhận');
    expect(find.textContaining('Hạn dùng theo dạng ngày/tháng/năm giờ:phút'), findsOneWidget);
    expect(b.calls('POST', '/residents/5/family-foods'), isEmpty);

    await fill(tester, 'Hạn dùng (ngày/tháng/năm giờ:phút)', '25/12/2026 18:00');
    await tapText(tester, 'Đậu phộng');
    await tapText(tester, 'Ghi nhận');

    final sent = jsonDecode(b.calls('POST', '/residents/5/family-foods').single.body) as Map;
    expect(sent['senderName'], 'Con gái');
    expect(sent['ingredients'], [11]);
    expect(sent['ingredientsUnknown'], false);
    expect(sent['expiresAt'], '2026-12-25T11:00:00.000Z');
    expect(find.text('Đã ghi nhận: Cần xác nhận.'), findsOneWidget);
  });

  testWidgets('Q42: Cần xác nhận thì cho dùng bắt buộc ghi chú, không cho dùng bắt buộc lý do', (tester) async {
    final b =
        backend([
            food(2, 'NEEDS_CONFIRMATION', reasons: ['INGREDIENTS_UNKNOWN']),
          ])
          ..on('POST', '/family-foods/2/approve-use', 200)
          ..on('POST', '/family-foods/2/reject-use', 200);
    await pumpScreen(tester, b.client());
    await openResident(tester);
    expect(find.text('• Thành phần không rõ'), findsOneWidget);

    await tapText(tester, 'Cho dùng');
    await tapText(tester, 'Xác nhận lệnh');
    expect(find.text('Nhập ghi chú xác nhận.'), findsOneWidget);
    expect(b.calls('POST', '/family-foods/2/approve-use'), isEmpty);

    await fill(tester, 'Ghi chú xác nhận', 'Đã hỏi gia đình');
    await tapText(tester, 'Xác nhận lệnh');
    expect(jsonDecode(b.calls('POST', '/family-foods/2/approve-use').single.body), {'note': 'Đã hỏi gia đình'});

    await tapText(tester, 'Không cho dùng');
    await tapText(tester, 'Xác nhận lệnh');
    expect(find.text('Nhập lý do không cho dùng.'), findsOneWidget);
    await fill(tester, 'Lý do không cho dùng', 'Không rõ nguồn gốc');
    await tapText(tester, 'Xác nhận lệnh');
    expect(jsonDecode(b.calls('POST', '/family-foods/2/reject-use').single.body), {'reason': 'Không rõ nguồn gốc'});
  });

  testWidgets('Q44: lệnh bị chặn thì hiện lý do của backend', (tester) async {
    final b = backend([food(3, 'NEEDS_CONFIRMATION')])
      ..on('POST', '/family-foods/3/approve-use', 409, {
        'errorCode': 'NOT_ALLOWED_TO_USE',
        'detail': 'Đồ ăn đã có xung đột hoặc hết hạn dùng, không cho dùng',
      });
    await pumpScreen(tester, b.client());
    await openResident(tester);

    await tapText(tester, 'Cho dùng');
    await fill(tester, 'Ghi chú xác nhận', 'Đã hỏi gia đình');
    await tapText(tester, 'Xác nhận lệnh');
    expect(find.text('Đồ ăn đã có xung đột hoặc hết hạn dùng, không cho dùng'), findsOneWidget);
  });

  testWidgets('ghi xử lý đồ ăn Không sử dụng: trả lại cần người nhận lại; hủy bỏ gửi cách xử lý', (tester) async {
    final b = backend([food(4, 'NOT_ALLOWED')])..on('POST', '/family-foods/4/record-disposal', 200);
    await pumpScreen(tester, b.client());
    await openResident(tester);

    await tapText(tester, 'Ghi xử lý');
    await tapText(tester, 'Xác nhận lệnh');
    expect(find.text('Chọn cách xử lý.'), findsOneWidget);
    expect(find.text('Đã dùng hết'), findsNothing);

    await tapText(tester, 'Trả lại người gửi');
    await tapText(tester, 'Xác nhận lệnh');
    expect(find.text('Nhập người nhận lại.'), findsOneWidget);

    await fill(tester, 'Người nhận lại', 'Con gái');
    await tapText(tester, 'Xác nhận lệnh');
    expect(jsonDecode(b.calls('POST', '/family-foods/4/record-disposal').single.body), {
      'method': 'RETURNED',
      'returnedTo': 'Con gái',
    });
  });

  testWidgets('kết thúc đồ ăn Được sử dụng: có cả cách đã dùng hết', (tester) async {
    final b = backend([food(5, 'ALLOWED')])..on('POST', '/family-foods/5/end', 200);
    await pumpScreen(tester, b.client());
    await openResident(tester);

    await tapText(tester, 'Kết thúc');
    await tapText(tester, 'Đã dùng hết');
    await tapText(tester, 'Xác nhận lệnh');
    expect(jsonDecode(b.calls('POST', '/family-foods/5/end').single.body), {'method': 'CONSUMED'});
  });
}
