import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/shift_swaps/shift_swaps_screen.dart';

import '../../support/fake_backend.dart';

Map<String, dynamic> swap({
  String status = 'AWAITING_RECIPIENT',
  List<String> commands = const ['accept', 'decline'],
}) => {
  'id': 7,
  'status': status,
  'kind': 'TAKE_OVER',
  'giverStaffId': 1,
  'recipientStaffId': 2,
  'shiftXId': 20,
  'reason': '[Lý do riêng tư]',
  'selfApproved': false,
  'allowedCommands': commands,
};

final myShifts = [
  {'id': 20, 'status': 'PUBLISHED', 'startsAt': '2026-11-20T00:00:00Z', 'endsAt': '2026-11-20T08:00:00Z'},
  {'id': 21, 'status': 'PUBLISHED', 'startsAt': '2026-11-21T00:00:00Z', 'endsAt': '2026-11-21T08:00:00Z'},
];

Future<void> open(WidgetTester tester, FakeBackend backend) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: ShiftSwapsScreen(api: backend.client()),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;
  setUp(() {
    backend = FakeBackend()
      ..on('GET', '/shift-swaps', 200, [swap()])
      ..on('GET', '/leave-requests', 200, [])
      ..on('GET', '/me/shifts', 200, myShifts)
      ..on('POST', '/shift-swaps/7/accept', 200, {})
      ..on('POST', '/shift-swaps/7/decline', 200, {})
      ..on('POST', '/shift-swaps', 201, {})
      ..on('POST', '/leave-requests', 201, {
        'requestIds': [9],
      });
  });

  testWidgets('FR_022_showsStatusAndOnlyCommandsTheBackendAllows', (tester) async {
    await open(tester, backend);
    expect(find.text('Chờ đồng ý'), findsOneWidget);
    expect(find.text('Đồng ý'), findsOneWidget);
    expect(find.text('Duyệt'), findsNothing);
    expect(find.textContaining('[Lý do riêng tư]'), findsOneWidget);
  });

  testWidgets('FR_022_acceptSendsCommandWithIdempotencyKey', (tester) async {
    await open(tester, backend);
    await tester.tap(find.byKey(const ValueKey('command-7-accept')));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Đồng ý'));
    await tester.pumpAndSettle();
    final call = backend.calls('POST', '/shift-swaps/7/accept').single;
    expect(call.headers['Idempotency-Key'], isNotEmpty);
    expect(jsonDecode(call.body), isEmpty);
  });

  testWidgets('FR_045_declineNeedsReasonBeforeSending', (tester) async {
    await open(tester, backend);
    await tester.tap(find.byKey(const ValueKey('command-7-decline')));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Từ chối'));
    await tester.pumpAndSettle();
    expect(find.text('Nhập lý do trước khi xác nhận.'), findsOneWidget);
    expect(backend.calls('POST', '/shift-swaps/7/decline'), isEmpty);
    await tester.enterText(find.byKey(const ValueKey('command-reason')), '  Bận việc nhà  ');
    await tester.tap(find.widgetWithText(FilledButton, 'Từ chối'));
    await tester.pumpAndSettle();
    expect(jsonDecode(backend.calls('POST', '/shift-swaps/7/decline').single.body), {'reason': 'Bận việc nhà'});
  });

  testWidgets('FR_021_approveShowsWarningConfirmationAfterConflict', (tester) async {
    backend
      ..on('GET', '/shift-swaps', 200, [
        swap(status: 'PENDING_APPROVAL', commands: ['approve', 'reject']),
      ])
      ..on('POST', '/shift-swaps/7/approve', 409, {
        'errorCode': 'WARNING_CONFIRMATION_REQUIRED',
        'detail': 'Chuỗi giờ làm liên tục vượt ngưỡng; cần xác nhận cảnh báo',
      });
    await open(tester, backend);
    await tester.tap(find.byKey(const ValueKey('command-7-approve')));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Duyệt'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Chuỗi giờ làm liên tục vượt ngưỡng'), findsWidgets);
    expect(find.byKey(const ValueKey('confirm-warning')), findsOneWidget);
    backend.on('POST', '/shift-swaps/7/approve', 200, {});
    await tester.tap(find.byKey(const ValueKey('confirm-warning')));
    await tester.enterText(find.byKey(const ValueKey('command-reason')), 'Cần người');
    await tester.tap(find.widgetWithText(FilledButton, 'Duyệt'));
    await tester.pumpAndSettle();
    final last = backend.calls('POST', '/shift-swaps/7/approve').last;
    expect(jsonDecode(last.body), {'reason': 'Cần người', 'confirmWarning': true});
  });

  testWidgets('FR_019_createTakeOverSendsContractBody', (tester) async {
    await open(tester, backend);
    await tester.tap(find.byKey(const ValueKey('create-request')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('swap-shift-x')));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('20/11/2026').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const ValueKey('swap-recipient')), '2');
    await tester.enterText(find.byKey(const ValueKey('swap-reason')), 'Việc gia đình');
    await tester.tap(find.widgetWithText(FilledButton, 'Gửi yêu cầu'));
    await tester.pumpAndSettle();
    expect(jsonDecode(backend.calls('POST', '/shift-swaps').single.body), {
      'kind': 'TAKE_OVER',
      'shiftXId': 20,
      'recipientId': 2,
      'reason': 'Việc gia đình',
    });
  });

  testWidgets('FR_026_createLeaveByShiftSendsChosenShiftsAndReason', (tester) async {
    await open(tester, backend);
    await tester.tap(find.text('Nghỉ đột xuất').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('create-request')).last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('leave-shift-21')));
    await tester.tap(find.byKey(const ValueKey('leave-shift-20')));
    await tester.enterText(find.byKey(const ValueKey('leave-reason')), 'Ốm');
    await tester.tap(find.widgetWithText(FilledButton, 'Gửi yêu cầu'));
    await tester.pumpAndSettle();
    expect(jsonDecode(backend.calls('POST', '/leave-requests').single.body), {
      'kind': 'BY_SHIFT',
      'shiftIds': [20, 21],
      'reason': 'Ốm',
    });
  });

  testWidgets('FR_026a_createLeaveByDayPicksDatesAndFloorFromCatalog', (tester) async {
    backend
      ..on('GET', '/floors', 200, [
        {'id': 5, 'name': 'Tầng 2'},
      ])
      ..on('GET', '/areas', 200, [
        {'id': 3, 'name': 'Khu A'},
      ]);
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await open(tester, backend);
    await tester.tap(find.text('Nghỉ đột xuất').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('create-request')).last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Nghỉ theo ngày'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('leave-add-date')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const ValueKey('leave-reason')), 'Việc gia đình');
    // Chưa chọn tầng thì không gửi, báo cần chọn phạm vi
    await tester.tap(find.widgetWithText(FilledButton, 'Gửi yêu cầu'));
    await tester.pumpAndSettle();
    expect(backend.calls('POST', '/leave-requests'), isEmpty);
    expect(find.byKey(const ValueKey('leave-error')), findsOneWidget);
    await tester.tap(find.byKey(const ValueKey('leave-scope-FLOOR')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tầng 2').last);
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Gửi yêu cầu'));
    await tester.pumpAndSettle();
    final now = DateTime.now();
    final first = DateTime(now.year, now.month + 1, 1);
    final day =
        '${first.year.toString().padLeft(4, '0')}-${first.month.toString().padLeft(2, '0')}-${first.day.toString().padLeft(2, '0')}';
    expect(jsonDecode(backend.calls('POST', '/leave-requests').single.body), {
      'kind': 'BY_DAY',
      'dates': [day],
      'scopeType': 'FLOOR',
      'scopeId': 5,
      'reason': 'Việc gia đình',
    });
  });

  testWidgets('FR_043_offlineShowsMessageAndKeepsInput', (tester) async {
    await open(tester, backend);
    await tester.tap(find.byKey(const ValueKey('command-7-decline')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const ValueKey('command-reason')), 'Bận');
    backend.down = true;
    await tester.tap(find.widgetWithText(FilledButton, 'Từ chối'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Không kết nối được máy chủ'), findsOneWidget);
    expect(find.text('Bận'), findsOneWidget);
  });
}
