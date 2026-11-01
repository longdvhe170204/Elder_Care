import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/notifications/push_coordinator.dart';

import '../../support/fake_backend.dart';
import '../../support/fake_push.dart';
import 'staff_notifications_support.dart';

// 009 T022 (tên logic xac_nhan_hai_thao_tac_test; SC-008, FR-026, FR-026a): từ hộp thông báo xác nhận một thao tác; từ thông báo
// đẩy bấm mở rồi xác nhận ngay trên màn chi tiết, đủ hai thao tác; "Xác nhận và tiếp nhận" chỉ hiện khi backend cho phép; bị
// 007 từ chối thì xác nhận vẫn còn.

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/residents', 403, {'errorCode': 'PERMISSION_DENIED'});
    backend.on('GET', '/me/call-reminders', 200, <Map<String, dynamic>>[]);
    backend.on(
      'GET',
      '/me/notifications',
      200,
      page([notification(id: 2, level: 'URGENT', title: 'Sự cố té ngã')], urgent: 1),
    );
    backend.on('POST', '/notifications/2/acknowledge', 200);
  });

  testWidgets('SC_008_fromInbox_oneTap', (tester) async {
    await pumpInbox(tester, backend.client());

    backend.on('GET', '/me/notifications', 200, page([]));
    await tapButton(tester, 'Xác nhận đã nhận');

    expect(backend.calls('POST', '/notifications/2/acknowledge').single.headers['Idempotency-Key'], isNotEmpty);
    expect(find.text('Đã xác nhận thông báo khẩn cấp'), findsOneWidget);
  });

  testWidgets('SC_008_fromPush_openThenAcknowledge_twoTaps', (tester) async {
    backend.on('GET', '/me/notifications/2', 200, notification(id: 2, level: 'URGENT', title: 'Sự cố té ngã'));
    final fake = FakePush();
    late PushCoordinator push;
    await tester.runAsync(() async => push = await startedPush(backend, fake, 'STAFF'));
    await pumpInbox(tester, backend.client(), push: push);

    await tester.runAsync(() async {
      fake.opened.add(2);
      await pumpEventQueue();
    });
    await tester.pumpAndSettle();

    backend.on(
      'GET',
      '/me/notifications/2',
      200,
      notification(id: 2, level: 'URGENT', title: 'Sự cố té ngã', acknowledgedAt: '2026-11-01T03:03:00Z'),
    );
    await tester.tap(button('Xác nhận đã nhận').last);
    await tester.pumpAndSettle();

    expect(backend.calls('POST', '/notifications/2/acknowledge'), hasLength(1));
    expect(find.text('Xác nhận lúc'), findsOneWidget);
    expect(find.text('Xác nhận đã nhận'), findsNothing);
  });

  testWidgets('FR_026_openingIsNotAcknowledging', (tester) async {
    backend.on('GET', '/me/notifications/2', 200, notification(id: 2, level: 'URGENT', title: 'Sự cố té ngã'));
    await pumpInbox(tester, backend.client());

    await tester.tap(find.text('Sự cố té ngã'));
    await tester.pumpAndSettle();

    expect(backend.calls('POST', '/notifications/2/acknowledge'), isEmpty);
  });

  testWidgets('FR_026a_noRight_noTakeChargeButton', (tester) async {
    await pumpInbox(tester, backend.client());

    expect(find.text('Xác nhận và tiếp nhận'), findsNothing);
  });

  testWidgets('FR_026a_takeCharge_succeeds', (tester) async {
    backend.on(
      'GET',
      '/me/notifications',
      200,
      page([notification(id: 2, level: 'URGENT', canTakeCharge: true)], urgent: 1),
    );
    backend.on('POST', '/notifications/2/acknowledge-and-take-charge', 200, {
      'acknowledgement': 'ACKNOWLEDGED',
      'takeCharge': {'result': 'SUCCEEDED'},
    });
    await pumpInbox(tester, backend.client());

    await tapButton(tester, 'Xác nhận và tiếp nhận');

    expect(backend.calls('POST', '/notifications/2/acknowledge-and-take-charge'), hasLength(1));
    expect(find.text('Đã xác nhận và tiếp nhận'), findsOneWidget);
  });

  testWidgets('FR_026a_takeChargeRejected_acknowledgementStands', (tester) async {
    backend.on(
      'GET',
      '/me/notifications',
      200,
      page([notification(id: 2, level: 'URGENT', canTakeCharge: true)], urgent: 1),
    );
    backend.on('POST', '/notifications/2/acknowledge-and-take-charge', 200, {
      'acknowledgement': 'ACKNOWLEDGED',
      'takeCharge': {'result': 'REJECTED', 'errorCode': 'ALREADY_TAKEN'},
    });
    await pumpInbox(tester, backend.client());

    await tapButton(tester, 'Xác nhận và tiếp nhận');

    expect(find.textContaining('Đã xác nhận. Chưa tiếp nhận được'), findsOneWidget);
  });
}
