import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_backend.dart';
import 'staff_notifications_support.dart';

// 009 T022 (tên logic ghi_ket_qua_goi_test; US1; FR-027, FR-029a, FR-030, FR-031): khối "Cần gọi điện", chi tiết lượt nhắc,
// quay số, ghi kết quả gọi: Đã liên lạc được bắt buộc ghi chú, Không liên lạc được bắt buộc lý do; 422 báo tại ô; 409 đã kết
// thúc; lần gọi bổ sung; người không có quyền gọi không có nút.

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/residents', 403, {'errorCode': 'PERMISSION_DENIED'});
    backend.on('GET', '/me/notifications', 200, page([]));
    backend.on('GET', '/me/call-reminders', 200, [reminder()]);
    backend.on('GET', '/call-reminders/8', 200, reminder());
    backend.on('POST', '/call-reminders/8/results', 201);
  });

  Future<void> openReminder(WidgetTester tester, {Future<bool> Function(String)? dial}) async {
    await pumpInbox(tester, backend.client(), dial: dial);
    await tapButton(tester, 'Mở lượt nhắc gọi');
  }

  Future<void> openSheet(WidgetTester tester) async {
    await openReminder(tester);
    await tapButton(tester, 'Ghi kết quả gọi');
  }

  Future<void> choose(WidgetTester tester, String label) async {
    await tester.tap(find.widgetWithText(ChoiceChip, label));
    await tester.pumpAndSettle();
  }

  Map<String, dynamic> sent() =>
      jsonDecode(backend.calls('POST', '/call-reminders/8/results').single.body) as Map<String, dynamic>;

  testWidgets('FR_027_awaitingReminder_shownOnTop', (tester) async {
    await pumpInbox(tester, backend.client());

    expect(find.text('Cần gọi điện'), findsOneWidget);
    expect(find.text('Gọi Trần Thị Mai'), findsOneWidget);
    expect(find.text('Đang nhắc Lê Văn Hùng. Hạn 10:15'), findsOneWidget);
  });

  testWidgets('FR_027_noReminder_noBlock', (tester) async {
    backend.on('GET', '/me/call-reminders', 200, <Map<String, dynamic>>[]);
    await pumpInbox(tester, backend.client());

    expect(find.text('Cần gọi điện'), findsNothing);
  });

  testWidgets('FR_027_detail_showsContract_noResidentName', (tester) async {
    await openReminder(tester);

    expect(find.text('Chờ kết quả'), findsOneWidget);
    expect(find.text('Người thân'), findsOneWidget);
    expect(find.text('0901234567'), findsOneWidget);
    expect(find.text('Té ngã ở phòng 205, đã sơ cứu'), findsOneWidget);
  });

  testWidgets('FR_027_callButton_dialsCurrentNumber', (tester) async {
    final dialed = <String>[];
    await openReminder(
      tester,
      dial: (phone) async {
        dialed.add(phone);
        return true;
      },
    );

    await tapButton(tester, 'Gọi số này');

    expect(dialed, ['0901234567']);
  });

  testWidgets('FR_027_noPhone_noCallButton', (tester) async {
    backend.on('GET', '/call-reminders/8', 200, reminder(phone: null));
    await openReminder(tester);

    expect(find.text('Chưa có số điện thoại'), findsOneWidget);
    expect(find.text('Gọi số này'), findsNothing);
  });

  testWidgets('FR_030a_history_showsHandoffs', (tester) async {
    backend.on(
      'GET',
      '/call-reminders/8',
      200,
      reminder(
        handoffs: [
          {'reason': 'INITIAL', 'occurredAt': '2026-11-01T03:05:00Z'},
          {'reason': 'OVERDUE', 'occurredAt': '2026-11-01T03:10:00Z'},
        ],
      ),
    );
    await openReminder(tester);

    expect(find.text('10:05 Nhắc lần đầu'), findsOneWidget);
    expect(find.text('10:10 Chuyển nhắc, quá hạn'), findsOneWidget);
  });

  testWidgets('FR_029_noRight_noRecordButton', (tester) async {
    backend.on('GET', '/call-reminders/8', 200, reminder(canRecord: false));
    await openReminder(tester);

    expect(find.text('Ghi kết quả gọi'), findsNothing);
  });

  testWidgets('FR_030_stopped_showsReason', (tester) async {
    backend.on(
      'GET',
      '/call-reminders/8',
      200,
      reminder(status: 'STOPPED', canRecord: false, stopReason: 'Người nhận đã tự xác nhận'),
    );
    await openReminder(tester);

    expect(find.text('Đã dừng'), findsOneWidget);
    expect(find.text('Người nhận đã tự xác nhận'), findsOneWidget);
  });

  testWidgets('FR_030_mustChooseResult', (tester) async {
    await openSheet(tester);

    await tapButton(tester, 'Lưu kết quả');

    expect(find.text('Chọn kết quả gọi.'), findsOneWidget);
    expect(backend.calls('POST', '/call-reminders/8/results'), isEmpty);
  });

  testWidgets('FR_030_reached_requiresNote', (tester) async {
    await openSheet(tester);
    await choose(tester, 'Đã liên lạc được');

    await tapButton(tester, 'Lưu kết quả');

    expect(find.text('Cần ghi chú khi đã liên lạc được.'), findsOneWidget);
    expect(backend.calls('POST', '/call-reminders/8/results'), isEmpty);
  });

  testWidgets('FR_031_reached_sendsResult_key_phone_reloads', (tester) async {
    await openSheet(tester);
    await choose(tester, 'Đã liên lạc được');
    await tester.enterText(find.widgetWithText(TextField, 'Người nghe máy (nếu khác người cần gọi)'), 'Chồng bà Mai');
    await tester.enterText(find.widgetWithText(TextField, 'Ghi chú'), 'Đã báo tin');

    backend.on('GET', '/call-reminders/8', 200, reminder(status: 'RESULT_RECORDED', canRecord: false));
    await tapButton(tester, 'Lưu kết quả');

    final body = sent();
    expect(body['result'], 'REACHED');
    expect(body['calledPhoneNumber'], '0901234567');
    expect(body['answeredBy'], 'Chồng bà Mai');
    expect(body['note'], 'Đã báo tin');
    expect(body['calledAt'], endsWith('Z'));
    expect(backend.calls('POST', '/call-reminders/8/results').single.headers['Idempotency-Key'], isNotEmpty);
    expect(find.text('Đã có kết quả'), findsOneWidget);
  });

  testWidgets('FR_030_notReached_requiresReason_threeReasonsOfSpec', (tester) async {
    await openSheet(tester);
    await choose(tester, 'Không liên lạc được');

    expect(find.widgetWithText(ChoiceChip, 'Không nghe máy'), findsOneWidget);
    expect(find.widgetWithText(ChoiceChip, 'Sai số'), findsOneWidget);
    expect(find.widgetWithText(ChoiceChip, 'Không có số'), findsOneWidget);
    expect(find.text('Khác'), findsNothing);

    await tapButton(tester, 'Lưu kết quả');
    expect(find.text('Chọn lý do không liên lạc được.'), findsOneWidget);

    await choose(tester, 'Sai số');
    await tapButton(tester, 'Lưu kết quả');

    final body = sent();
    expect(body['result'], 'NOT_REACHED');
    expect(body['reason'], 'WRONG_NUMBER');
    expect(body.containsKey('answeredBy'), isFalse);
  });

  testWidgets('FR_030_invalid422_shownAtField', (tester) async {
    backend.on('POST', '/call-reminders/8/results', 422, {
      'errorCode': 'INVALID_VALUE',
      'detail': 'Đã liên lạc được cần ghi chú (FR-030)',
    });
    await openSheet(tester);
    await choose(tester, 'Đã liên lạc được');
    await tester.enterText(find.widgetWithText(TextField, 'Ghi chú'), 'Đã báo tin');

    await tapButton(tester, 'Lưu kết quả');

    expect(find.text('Đã liên lạc được cần ghi chú (FR-030)'), findsOneWidget);
  });

  testWidgets('FR_030_ended409_showsMessage', (tester) async {
    backend.on('POST', '/call-reminders/8/results', 409, {
      'errorCode': 'CALL_REMINDER_ENDED',
      'detail': 'Lượt nhắc gọi đã kết thúc',
    });
    await openSheet(tester);
    await choose(tester, 'Không liên lạc được');
    await choose(tester, 'Không nghe máy');

    await tapButton(tester, 'Lưu kết quả');

    expect(find.text('Lượt nhắc gọi đã kết thúc'), findsOneWidget);
  });

  testWidgets('FR_029a_afterNotReached_supplementaryCall', (tester) async {
    backend.on(
      'GET',
      '/call-reminders/8',
      200,
      reminder(
        status: 'RESULT_RECORDED',
        results: [
          {'result': 'NOT_REACHED', 'calledAt': '2026-11-01T03:09:00Z', 'reason': 'NO_ANSWER', 'supplementary': false},
        ],
      ),
    );
    await openReminder(tester);

    expect(find.text('Không liên lạc được, Không nghe máy'), findsOneWidget);
    expect(find.text('Ghi lần gọi bổ sung'), findsOneWidget);
    expect(find.text('Ghi kết quả gọi'), findsNothing);
  });

  testWidgets('T022_callReminderNotification_opensReminder', (tester) async {
    final notice = notification(
      id: 5,
      level: 'URGENT',
      title: 'Nhắc gọi điện',
      sourceRecord: {'type': 'M13.CALL_REMINDER', 'id': '8', 'canOpen': false},
    );
    backend.on('GET', '/me/call-reminders', 200, <Map<String, dynamic>>[]);
    backend.on('GET', '/me/notifications', 200, page([notice], urgent: 1));
    backend.on('GET', '/me/notifications/5', 200, notice);
    await pumpInbox(tester, backend.client());

    await tester.tap(find.text('Nhắc gọi điện'));
    await tester.pumpAndSettle();
    await tapButton(tester, 'Mở lượt nhắc gọi');

    expect(backend.calls('GET', '/call-reminders/8'), hasLength(1));
  });

  testWidgets('T022_otherNotification_noReminderButton', (tester) async {
    final notice = notification(id: 5, sourceRecord: {'type': 'M15.REQUEST', 'id': '8', 'canOpen': false});
    backend.on('GET', '/me/call-reminders', 200, <Map<String, dynamic>>[]);
    backend.on('GET', '/me/notifications', 200, page([notice]));
    backend.on('GET', '/me/notifications/5', 200, notice);
    await pumpInbox(tester, backend.client());

    await tester.tap(find.text('Yêu cầu YC-2026-000050'));
    await tester.pumpAndSettle();

    expect(find.text('Mở lượt nhắc gọi'), findsNothing);
  });
}
