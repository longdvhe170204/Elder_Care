import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/resident/today/today_screen.dart';

import '../../support/fake_backend.dart';

// 009 T042 (tên logic thong_bao_test; FR-022a; DESIGN.md 15.6): người cao tuổi xem thông báo từ thẻ ở màn "Hôm nay": chữ lớn,
// nút "Quay lại" có chữ, mỗi màn một việc, không có thao tác ẩn hay xác nhận. Nhận đẩy chờ cấu hình Firebase.

Map<String, dynamic> notification({int id = 1, String? viewedAt}) => {
  'id': id,
  'level': 'LOW',
  'eventType': 'VISIT_REMINDER',
  'title': '15:00 hôm nay [Họ tên 1] đến thăm',
  'content': [
    {'informationType': 'GENERAL', 'content': 'Gặp ở phòng khách tầng 1.'},
  ],
  'hiddenParts': <String>[],
  'identityOnly': false,
  'status': 'SENT',
  'sentAt': '2026-10-27T02:00:00Z',
  'viewedAt': viewedAt,
};

Map<String, dynamic> page(List<Map<String, dynamic>> items, {int unread = 0}) => {
  'items': items,
  'unreadCount': unread,
  'unacknowledgedUrgentCount': 0,
  'nextCursor': null,
};

Future<void> pumpToday(WidgetTester tester, ApiClient api, {double textScale = 1}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.resident(Brightness.light),
      builder: (context, child) => MediaQuery(
        data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(textScale)),
        child: child!,
      ),
      home: TodayScreen(api: api),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> openList(WidgetTester tester) async {
  await tester.tap(find.text('Thông báo'));
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/me/notifications', 200, page([notification()], unread: 1));
    backend.on('GET', '/me/notifications/1', 200, notification(viewedAt: '2026-10-27T03:00:00Z'));
  });

  testWidgets('T042_todayCard_showsUnreadCount', (tester) async {
    await pumpToday(tester, backend.client());

    expect(find.text('Thông báo'), findsOneWidget);
    expect(find.text('1 thông báo chưa xem'), findsOneWidget);
  });

  testWidgets('T042_todayCard_noNew', (tester) async {
    backend.on('GET', '/me/notifications', 200, page([]));
    await pumpToday(tester, backend.client());

    expect(find.text('Không có thông báo mới'), findsOneWidget);
  });

  testWidgets('T042_todayCard_stillOpensWhenCountFails', (tester) async {
    backend.down = true;
    await pumpToday(tester, backend.client());

    expect(find.text('Thông báo'), findsOneWidget);
    expect(find.text('1 thông báo chưa xem'), findsNothing);
  });

  testWidgets('T042_list_largeRows_backButtonHasText', (tester) async {
    await pumpToday(tester, backend.client());
    await openList(tester);

    expect(find.text('15:00 hôm nay [Họ tên 1] đến thăm'), findsOneWidget);
    final unread = tester.widget<Text>(find.text('Chưa xem'));
    expect(unread.style?.color, Theme.of(tester.element(find.text('Chưa xem'))).colorScheme.onSurface);
    final row = find.ancestor(of: find.text('15:00 hôm nay [Họ tên 1] đến thăm'), matching: find.byType(InkWell));
    expect(tester.getSize(row.first).height, greaterThanOrEqualTo(56));

    await tester.tap(find.text('Quay lại'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Hôm nay,'), findsOneWidget);
  });

  testWidgets('T042_backButton_fitsAtDoubleTextSize', (tester) async {
    await pumpToday(tester, backend.client(), textScale: 2);
    await openList(tester);

    final label = tester.getSize(find.text('Quay lại'));
    final button = tester.getSize(find.ancestor(of: find.text('Quay lại'), matching: find.byType(TextButton)));
    expect(button.width, greaterThanOrEqualTo(label.width));
    expect(label.height, lessThan(80));
  });

  testWidgets('FR_034_open_recordsView_showsContent_noHideOrAcknowledge', (tester) async {
    await pumpToday(tester, backend.client());
    await openList(tester);

    await tester.tap(find.text('15:00 hôm nay [Họ tên 1] đến thăm'));
    await tester.pumpAndSettle();

    expect(backend.calls('GET', '/me/notifications/1'), hasLength(1));
    expect(find.text('Gặp ở phòng khách tầng 1.'), findsOneWidget);
    expect(find.text('Ẩn khỏi danh sách'), findsNothing);
    expect(find.text('Xác nhận đã nhận'), findsNothing);
  });

  testWidgets('T042_empty', (tester) async {
    backend.on('GET', '/me/notifications', 200, page([]));
    await pumpToday(tester, backend.client());
    await openList(tester);

    expect(find.text('Chưa có thông báo nào.'), findsOneWidget);
  });

  testWidgets('T042_offline_shortSentence_retry', (tester) async {
    await pumpToday(tester, backend.client());
    backend.down = true;
    await openList(tester);

    expect(find.text('Máy chưa kết nối mạng.'), findsOneWidget);

    backend.down = false;
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(find.text('15:00 hôm nay [Họ tên 1] đến thăm'), findsOneWidget);
  });
}
