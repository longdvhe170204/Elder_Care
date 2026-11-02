import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_backend.dart';
import 'staff_notifications_support.dart';

// 009 T022 (tên logic xac_nhan_bat_buoc_truc_tuyen_test; Q-01, FR-026; DESIGN.md 15.5): mất mạng thì nút vẫn hiện, bấm thì báo
// rõ thao tác cần mạng, không đưa vào hàng đợi; có mạng lại bấm lần nữa dùng đúng khóa cũ.

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/residents', 403, {'errorCode': 'PERMISSION_DENIED'});
    backend.on('GET', '/me/call-reminders', 200, [reminder()]);
    backend.on('GET', '/call-reminders/8', 200, reminder());
    backend.on(
      'GET',
      '/me/notifications',
      200,
      page([notification(id: 2, level: 'URGENT', title: 'Sự cố té ngã')], urgent: 1),
    );
  });

  testWidgets('Q_01_acknowledgeOffline_saysNeedsNetwork_retryKeepsKey', (tester) async {
    await pumpInbox(tester, backend.client());

    backend.down = true;
    await tapButton(tester, 'Xác nhận đã nhận');

    expect(find.text('Xác nhận cần kết nối mạng. Kiểm tra mạng rồi thử lại.'), findsOneWidget);
    expect(button('Xác nhận đã nhận'), findsOneWidget);

    backend.down = false;
    backend.on('POST', '/notifications/2/acknowledge', 200);
    await tapButton(tester, 'Xác nhận đã nhận');

    final keys = backend.calls('POST', '/notifications/2/acknowledge').map((c) => c.headers['Idempotency-Key']).toSet();
    expect(keys, hasLength(1));
  });

  testWidgets('Q_01_recordResultOffline_saysNeedsNetwork_keepsForm', (tester) async {
    await pumpInbox(tester, backend.client());
    await tapButton(tester, 'Mở lượt nhắc gọi');
    await tapButton(tester, 'Ghi kết quả gọi');
    await tester.tap(find.text('Đã liên lạc được'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Ghi chú'), 'Đã báo tin');

    backend.down = true;
    await tapButton(tester, 'Lưu kết quả');

    expect(find.text('Ghi kết quả gọi cần kết nối mạng. Kiểm tra mạng rồi thử lại.'), findsOneWidget);
    expect(find.text('Đã báo tin'), findsOneWidget);
  });

  testWidgets('FR_033_inboxOffline_retry', (tester) async {
    backend.down = true;
    await pumpInbox(tester, backend.client());
    expect(find.textContaining('Kiểm tra mạng'), findsOneWidget);

    backend.down = false;
    await tapButton(tester, 'Thử lại');

    expect(find.text('Sự cố té ngã'), findsOneWidget);
  });
}
