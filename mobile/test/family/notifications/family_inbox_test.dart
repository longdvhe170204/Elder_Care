import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/notifications/notification_inbox.dart';
import 'package:mobile/common/notifications/push_coordinator.dart';
import 'package:mobile/common/theme/app_theme.dart';

import '../../support/fake_backend.dart';
import '../../support/fake_push.dart';

// 009 T031 (US4; FR-033, FR-034, FR-036, FR-026): hộp thông báo người thân trên app: Khẩn cấp chưa xác nhận lên đầu có nút
// xác nhận, lọc mức và chưa xem, mở thông báo theo quyền hiện hành, ẩn thông báo đã xem. Nhận đẩy chờ cấu hình Firebase.

Map<String, dynamic> notification({
  int id = 1,
  String level = 'LOW',
  String title = 'Bảng chi phí tháng 10 đã chốt',
  String? viewedAt,
  String? acknowledgedAt,
  List<Map<String, dynamic>> content = const [],
  List<String> hiddenParts = const [],
}) => {
  'id': id,
  'level': level,
  'eventType': 'COST_FINALIZED',
  'title': title,
  'content': content,
  'hiddenParts': hiddenParts,
  'identityOnly': false,
  'status': 'SENT',
  'sentAt': '2026-10-27T02:00:00Z',
  'viewedAt': viewedAt,
  'acknowledgedAt': acknowledgedAt,
};

Map<String, dynamic> page(List<Map<String, dynamic>> items, {int unread = 0, int urgent = 0, String? next}) => {
  'items': items,
  'unreadCount': unread,
  'unacknowledgedUrgentCount': urgent,
  'nextCursor': next,
};

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: NotificationInbox(api: api)),
    ),
  );
  await tester.pumpAndSettle();
}

Finder button(String label) =>
    find.ancestor(of: find.text(label), matching: find.byWidgetPredicate((w) => w is ButtonStyleButton));

Future<void> tapButton(WidgetTester tester, String label) async {
  await tester.ensureVisible(button(label));
  await tester.pumpAndSettle();
  await tester.tap(button(label));
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/residents', 403, {'errorCode': 'PERMISSION_DENIED'});
    backend.on('GET', '/me/notifications', 200, page([notification()], unread: 1));
  });

  testWidgets('FR_033_summaryAndRow_titleLevelStatus', (tester) async {
    await pumpScreen(tester, backend.client());

    expect(find.text('1 chưa xem, 0 khẩn cấp chưa xác nhận.'), findsOneWidget);
    expect(find.text('Bảng chi phí tháng 10 đã chốt'), findsOneWidget);
    expect(find.text('Nhẹ, 27/10/2026 09:00'), findsOneWidget);
    expect(find.text('Chưa xem'), findsWidgets);
  });

  testWidgets('FR_033_urgentUnacknowledged_onTopInOwnBlock', (tester) async {
    backend.on(
      'GET',
      '/me/notifications',
      200,
      page([notification(), notification(id: 2, level: 'URGENT', title: 'Người thân cần liên hệ gấp')], urgent: 1),
    );
    await pumpScreen(tester, backend.client());

    expect(find.text('Khẩn cấp chưa xác nhận'), findsOneWidget);
    final urgent = tester.getTopLeft(find.text('Người thân cần liên hệ gấp')).dy;
    final low = tester.getTopLeft(find.text('Bảng chi phí tháng 10 đã chốt')).dy;
    expect(urgent, lessThan(low));
  });

  testWidgets('FR_026_acknowledge_sendsIdempotencyKey_reloads', (tester) async {
    backend.on('GET', '/me/notifications', 200, page([notification(id: 2, level: 'URGENT', title: 'Khẩn')], urgent: 1));
    backend.on('POST', '/notifications/2/acknowledge', 200);
    await pumpScreen(tester, backend.client());

    backend.on('GET', '/me/notifications', 200, page([]));
    await tapButton(tester, 'Xác nhận đã nhận');

    final call = backend.calls('POST', '/notifications/2/acknowledge').single;
    expect(call.headers['Idempotency-Key'], isNotEmpty);
    expect(find.text('Không có thông báo nào'), findsOneWidget);
  });

  testWidgets('FR_026_acknowledgeOffline_saysNeedsNetwork', (tester) async {
    backend.on('GET', '/me/notifications', 200, page([notification(id: 2, level: 'URGENT', title: 'Khẩn')], urgent: 1));
    await pumpScreen(tester, backend.client());

    backend.down = true;
    await tapButton(tester, 'Xác nhận đã nhận');

    expect(find.textContaining('Kiểm tra mạng'), findsOneWidget);
  });

  testWidgets('FR_033_filters_levelAndUnread', (tester) async {
    await pumpScreen(tester, backend.client());

    await tester.tap(find.text('Trung bình'));
    await tester.pumpAndSettle();
    expect(backend.requests.last.url.queryParameters['level'], 'MEDIUM');

    final unreadChip = find.widgetWithText(FilterChip, 'Chưa xem');
    expect(tester.getTopLeft(unreadChip).dx, lessThan(tester.getTopLeft(find.text('Tất cả mức')).dx));
    await tester.tap(unreadChip);
    await tester.pumpAndSettle();
    expect(backend.requests.last.url.queryParameters['unread'], 'true');
    expect(backend.requests.last.url.queryParameters['level'], 'MEDIUM');
  });

  testWidgets('FR_033_moreFilters_eventType', (tester) async {
    await pumpScreen(tester, backend.client());

    await tester.tap(find.text('Lọc thêm'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Loại sự kiện'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('COST_FINALIZED').last);
    await tester.pumpAndSettle();

    expect(backend.requests.last.url.queryParameters['eventType'], 'COST_FINALIZED');
    expect(find.text('Lọc thêm (1)'), findsOneWidget);
  });

  testWidgets('SC_008_urgentDetail_acknowledgeOnSameScreen', (tester) async {
    final urgent = notification(id: 2, level: 'URGENT', title: 'Khẩn');
    backend.on('GET', '/me/notifications', 200, page([urgent], urgent: 1));
    backend.on('GET', '/me/notifications/2', 200, urgent);
    backend.on('POST', '/notifications/2/acknowledge', 200);
    await pumpScreen(tester, backend.client());

    await tester.tap(find.text('Khẩn'));
    await tester.pumpAndSettle();
    backend.on(
      'GET',
      '/me/notifications/2',
      200,
      notification(id: 2, level: 'URGENT', title: 'Khẩn', acknowledgedAt: '2026-10-27T03:05:00Z'),
    );
    await tapButton(tester, 'Xác nhận đã nhận');

    expect(backend.calls('POST', '/notifications/2/acknowledge'), hasLength(1));
    expect(find.text('Xác nhận lúc'), findsOneWidget);
  });

  testWidgets('FR_034_open_recordsView_currentRights_hiddenPartLocked', (tester) async {
    backend.on(
      'GET',
      '/me/notifications/1',
      200,
      notification(
        viewedAt: '2026-10-27T03:00:00Z',
        content: [
          {'informationType': 'GENERAL', 'content': 'Mời xem bảng chi phí'},
        ],
        hiddenParts: const ['HEALTH'],
      ),
    );
    await pumpScreen(tester, backend.client());

    await tester.tap(find.text('Bảng chi phí tháng 10 đã chốt'));
    await tester.pumpAndSettle();

    expect(backend.calls('GET', '/me/notifications/1'), hasLength(1));
    expect(find.text('Mời xem bảng chi phí'), findsOneWidget);
    expect(find.text('Bạn không còn quyền xem phần này.'), findsOneWidget);
  });

  testWidgets('FR_036_hide_removesRow', (tester) async {
    backend.on('GET', '/me/notifications/1', 200, notification(viewedAt: '2026-10-27T03:00:00Z'));
    backend.on('POST', '/me/notifications/1/hide', 200);
    await pumpScreen(tester, backend.client());
    await tester.tap(find.text('Bảng chi phí tháng 10 đã chốt'));
    await tester.pumpAndSettle();

    backend.on('GET', '/me/notifications', 200, page([]));
    await tapButton(tester, 'Ẩn khỏi danh sách');

    final call = backend.calls('POST', '/me/notifications/1/hide').single;
    expect(call.headers['Idempotency-Key'], isNotEmpty);
    expect(jsonDecode(call.body), isA<Map<String, dynamic>>());
    expect(find.text('Bảng chi phí tháng 10 đã chốt'), findsNothing);
  });

  testWidgets('T031_tappedPush_opensThatNotification', (tester) async {
    backend.on('GET', '/me/notifications/1', 200, notification(viewedAt: '2026-10-27T03:00:00Z'));
    final fake = FakePush();
    late PushCoordinator push;
    await tester.runAsync(() async => push = await startedPush(backend, fake, 'FAMILY_MEMBER'));
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: NotificationInbox(api: backend.client(), push: push),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.runAsync(() async {
      fake.opened.add(1);
      await pumpEventQueue();
    });
    await tester.pumpAndSettle();

    expect(find.text('Xem lần đầu'), findsOneWidget);
  });

  testWidgets('T031_foregroundPush_reloadsList', (tester) async {
    final fake = FakePush();
    late PushCoordinator push;
    await tester.runAsync(() async => push = await startedPush(backend, fake, 'FAMILY_MEMBER'));
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: NotificationInbox(api: backend.client(), push: push),
        ),
      ),
    );
    await tester.pumpAndSettle();

    backend.on('GET', '/me/notifications', 200, page([notification(id: 9, title: 'Thông báo vừa tới')]));
    await tester.runAsync(() async {
      fake.received.add(null);
      await pumpEventQueue();
    });
    await tester.pumpAndSettle();

    expect(find.text('Thông báo vừa tới'), findsOneWidget);
  });

  testWidgets('FR_033_offline_retry', (tester) async {
    backend.down = true;
    await pumpScreen(tester, backend.client());
    expect(find.textContaining('Kiểm tra mạng'), findsOneWidget);

    backend.down = false;
    await tapButton(tester, 'Thử lại');

    expect(find.text('Bảng chi phí tháng 10 đã chốt'), findsOneWidget);
  });

  testWidgets('FR_033_seeMore', (tester) async {
    backend.on('GET', '/me/notifications', 200, page([notification()], next: 'abc'));
    await pumpScreen(tester, backend.client());

    backend.on('GET', '/me/notifications', 200, page([notification(id: 3, title: 'Thông báo khác')]));
    await tapButton(tester, 'Xem thêm');

    expect(backend.requests.last.url.queryParameters['cursor'], 'abc');
    expect(find.text('Thông báo khác'), findsOneWidget);
  });
}
