import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/nutrition/meal_slip_screen.dart';

import '../../support/fake_backend.dart';

// 011 T031 (FR-047, FR-048, FR-048b): người nhận tại tầng nhận phiếu đã giao, báo sai lệch bắt buộc nội dung, nhận phần bổ sung.

Map<String, dynamic> slip(String status, {List<Map<String, dynamic>> extra = const []}) => {
  'id': 7,
  'floorArea': 'Tầng 1',
  'meal': 'Bữa trưa',
  'status': status,
  'portionsByDiet': [
    {'diet': 'Thường', 'portionCount': 4},
  ],
  'labels': <Map<String, dynamic>>[],
  'supplementaryPortions': extra,
};

FakeBackend backend(Map<String, dynamic> s) => FakeBackend()
  ..on('GET', '/daily-meals', 200, [
    {'dailyMealId': 9, 'mealId': 1, 'mealName': 'Bữa trưa', 'closed': true},
  ])
  ..on('GET', '/meal-slips', 200, [s]);

Future<void> open(WidgetTester tester, FakeBackend b) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: MealSlipScreen(api: b.client())),
    ),
  );
  await tester.pumpAndSettle();
  await tester.tap(find.text('Bữa trưa'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('phiếu đã giao: nhận phiếu gửi Idempotency-Key', (tester) async {
    final b = backend(slip('DELIVERED'))..on('POST', '/meal-slips/7/receive', 200, {});
    await open(tester, b);
    expect(find.text('Tầng 1: Đã giao'), findsOneWidget);
    await tester.tap(find.text('Nhận phiếu'));
    await tester.pumpAndSettle();
    final calls = b.calls('POST', '/meal-slips/7/receive');
    expect(calls, hasLength(1));
    expect(calls.single.headers['Idempotency-Key'], isNotEmpty);
  });

  testWidgets('báo sai lệch bắt buộc nội dung rồi gửi đúng thân', (tester) async {
    final b = backend(slip('DELIVERED'))..on('POST', '/meal-slips/7/report-discrepancy', 200, {});
    await open(tester, b);
    await tester.tap(find.text('Báo sai lệch'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Gửi báo sai lệch'));
    await tester.pumpAndSettle();
    expect(find.text('Nhập nội dung sai lệch.'), findsOneWidget);
    expect(b.calls('POST', '/meal-slips/7/report-discrepancy'), isEmpty);
    await tester.enterText(find.widgetWithText(TextField, 'Nội dung sai lệch'), 'Thiếu 1 suất mềm');
    await tester.tap(find.text('Gửi báo sai lệch'));
    await tester.pumpAndSettle();
    final calls = b.calls('POST', '/meal-slips/7/report-discrepancy');
    expect(calls, hasLength(1));
    expect(calls.single.body, contains('Thiếu 1 suất mềm'));
  });

  testWidgets('phiếu chưa giao không có nút nhận; phần bổ sung đã giao nhận được', (tester) async {
    final b = backend(
      slip(
        'PREPARED',
        extra: [
          {'id': 3, 'dietId': 1, 'portionCount': 2, 'deliveredAt': '2026-11-08T05:00:00Z', 'receivedAt': null},
        ],
      ),
    )..on('POST', '/supplementary-portions/3/receive', 200, {});
    await open(tester, b);
    expect(find.text('Nhận phiếu'), findsNothing);
    await tester.tap(find.text('Nhận phần bổ sung'));
    await tester.pumpAndSettle();
    expect(b.calls('POST', '/supplementary-portions/3/receive'), hasLength(1));
  });
}
