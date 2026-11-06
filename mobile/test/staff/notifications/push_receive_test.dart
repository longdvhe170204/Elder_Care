import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';
import 'package:mobile/common/notifications/push_coordinator.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/staff_root.dart';

import '../../support/fake_backend.dart';
import '../../support/fake_push.dart';
import 'staff_notifications_support.dart';

// 009 T022 (tên logic nhan_day_test; US1; FR-016): nhận đẩy ở phần nhân viên: tab Thông báo mở đầu, huy hiệu số Khẩn cấp chưa
// xác nhận; đang ở tab khác có Khẩn cấp thì hiện dải báo; bấm thông báo thì sang tab Thông báo và mở đúng thông báo.

void main() {
  late FakeBackend backend;
  late FakePush fake;
  late PushCoordinator push;
  late SessionController s;
  late OfflineQueue queue;

  setUp(() {
    backend = FakeBackend();
    fake = FakePush();
    backend.on('GET', '/residents', 403, {'errorCode': 'PERMISSION_DENIED'});
    backend.on('GET', '/me/call-reminders', 200, <Map<String, dynamic>>[]);
    backend.on('GET', '/me/notifications', 200, page([notification()]));
    backend.on('GET', '/sync/results', 200, {'items': <Map<String, dynamic>>[]});
  });

  Future<void> pumpRoot(WidgetTester tester) async {
    await tester.runAsync(() async {
      push = await startedPush(backend, fake, 'STAFF');
      final store = MemoryStore();
      await store.write(SessionController.tokenKey, 'session-1');
      s = session(backend, store, FakeBiometric(available: false));
      await s.start();
      queue = memoryQueue(s);
    });
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: StaffRoot(session: s, queue: queue, push: push),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> arrive(WidgetTester tester, String level) async {
    await tester.runAsync(() async {
      fake.received.add(level);
      await pumpEventQueue();
    });
    await tester.pumpAndSettle();
  }

  Future<void> toSyncTab(WidgetTester tester) async {
    await tester.tap(find.descendant(of: find.byType(NavigationBar), matching: find.text('Đồng bộ')));
    await tester.pumpAndSettle();
  }

  testWidgets('T022_notificationsTab_firstAndOpenedByDefault', (tester) async {
    await pumpRoot(tester);

    expect(find.descendant(of: find.byType(AppBar), matching: find.text('Thông báo')), findsOneWidget);
    expect(find.text('Yêu cầu YC-2026-000050'), findsOneWidget);
  });

  testWidgets('T022_badge_countsUnacknowledgedUrgent', (tester) async {
    backend.on('GET', '/me/notifications', 200, page([notification(level: 'URGENT', title: 'Sự cố')], urgent: 1));
    await pumpRoot(tester);

    expect(find.descendant(of: find.byType(Badge), matching: find.text('1')), findsOneWidget);
  });

  testWidgets('FR_016_urgentOnOtherTab_showsBand_openGoesToInbox', (tester) async {
    await pumpRoot(tester);
    await toSyncTab(tester);

    await arrive(tester, 'URGENT');
    expect(find.text('Có thông báo khẩn cấp mới'), findsOneWidget);

    await tapButton(tester, 'Mở hộp thông báo');

    expect(find.text('Có thông báo khẩn cấp mới'), findsNothing);
    expect(find.descendant(of: find.byType(AppBar), matching: find.text('Thông báo')), findsOneWidget);
  });

  testWidgets('FR_016_lowLevelOnOtherTab_noBand', (tester) async {
    await pumpRoot(tester);
    await toSyncTab(tester);

    await arrive(tester, 'LOW');

    expect(find.text('Có thông báo khẩn cấp mới'), findsNothing);
  });

  testWidgets('FR_016_urgentOnInboxTab_reloadsWithoutBand', (tester) async {
    await pumpRoot(tester);
    backend.on(
      'GET',
      '/me/notifications',
      200,
      page([notification(id: 2, level: 'URGENT', title: 'Sự cố vừa tới')], urgent: 1),
    );

    await arrive(tester, 'URGENT');

    expect(find.text('Có thông báo khẩn cấp mới'), findsNothing);
    expect(find.text('Sự cố vừa tới'), findsOneWidget);
  });

  testWidgets('T022_tappedPushOnOtherTab_opensThatNotification', (tester) async {
    backend.on('GET', '/me/notifications/1', 200, notification());
    await pumpRoot(tester);
    await toSyncTab(tester);

    await tester.runAsync(() async {
      fake.opened.add(1);
      await pumpEventQueue();
    });
    await tester.pumpAndSettle();

    expect(backend.calls('GET', '/me/notifications/1'), hasLength(1));
    expect(find.text('Xem lần đầu'), findsOneWidget);
  });
}
