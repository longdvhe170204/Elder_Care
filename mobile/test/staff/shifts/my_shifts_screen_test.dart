import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/shifts/my_shifts_screen.dart';

import '../../support/fake_backend.dart';

Map<String, dynamic> nightShift({String status = 'PUBLISHED', int? leadStaffId = 11}) => {
  'id': 21,
  'shiftTemplateId': 5,
  'startsAt': '2026-10-28T15:00:00Z',
  'endsAt': '2026-10-29T00:00:00Z',
  'status': status,
  'leadStaffId': leadStaffId,
  'missingLead': leadStaffId == null,
  'staff': [
    {'staffId': 11, 'onDutyDoctor': true, 'absent': false, 'approvedLeave': false},
    {'staffId': 12, 'onDutyDoctor': false, 'absent': true, 'approvedLeave': true},
  ],
  'staffingRatio': null,
  'allowedCommands': ['CANCEL'],
  'version': 3,
};

Future<void> pumpScreen(WidgetTester tester, ApiClient api, {bool dark = false, double scale = 1}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: dark ? AppTheme.dark() : AppTheme.light(),
      builder: (context, child) => MediaQuery(
        data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(scale)),
        child: child!,
      ),
      home: Scaffold(
        body: MyShiftsScreen(api: api, now: DateTime.utc(2026, 10, 28)),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;
  setUp(() {
    backend = FakeBackend()
      ..on('GET', '/me/shifts', 200, [nightShift()])
      ..on('GET', '/staff', 200, [
        {'id': 11, 'fullName': '[Họ tên NV 1]', 'phoneNumber': 'private-phone'},
        {'id': 12, 'fullName': '[Họ tên NV 2]', 'email': 'private-email'},
        {'id': 99, 'fullName': '[Người ngoài ca]'},
      ]);
  });

  testWidgets('FR_033_displaysOvernightDatesInVietnamTimeAndOnlyReadActions', (tester) async {
    await pumpScreen(tester, backend.client());
    expect(find.text('Tháng 10/2026'), findsOneWidget);
    expect(find.text('Từ 28/10/2026 22:00'), findsOneWidget);
    expect(find.text('Đến 29/10/2026 07:00'), findsOneWidget);
    expect(find.text('Đã công bố'), findsOneWidget);
    expect(find.text('Nhân viên trong ca: 2'), findsOneWidget);
    expect(find.text('Hủy ca'), findsNothing);
    expect(backend.requests.every((r) => r.method == 'GET'), isTrue);
  });

  testWidgets('FR_020_detailsExposeAllowedAbsenceCommandAndReloadServerStateAfterSuccess', (tester) async {
    final original = nightShift()..['allowedCommands'] = ['recordAbsence'];
    original['staff'] = [
      {'staffId': 11, 'onDutyDoctor': false, 'absent': false, 'approvedLeave': false},
      {'staffId': 12, 'onDutyDoctor': false, 'absent': false, 'approvedLeave': false},
    ];
    backend.on('GET', '/me/shifts', 200, [original]);
    backend.on('POST', '/shifts/21/record-absence', 200);
    await pumpScreen(tester, backend.client());
    await tester.tap(find.text('Chi tiết ca'));
    await tester.pumpAndSettle();
    final action = find.widgetWithText(OutlinedButton, 'Ghi nhận vắng ca');
    await tester.ensureVisible(action);
    await tester.tap(action);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('absent-staff')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('[Họ tên NV 2]').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const ValueKey('absence-reason')), 'Báo vắng trong ca');
    backend.on('GET', '/me/shifts', 200, [nightShift()]);
    final confirm = find.widgetWithText(FilledButton, 'Xác nhận ghi vắng');
    await tester.ensureVisible(confirm);
    await tester.tap(confirm);
    await tester.pumpAndSettle();
    expect(backend.calls('POST', '/shifts/21/record-absence'), hasLength(1));
    expect(backend.calls('GET', '/me/shifts'), hasLength(2));
    expect(find.text('Ghi nhận vắng ca'), findsNothing);
    await tester.tap(find.text('Chi tiết ca'));
    await tester.pumpAndSettle();
    expect(find.text('Vắng ca'), findsOneWidget);
  });

  testWidgets('FR_054_detailsShowLeadColleaguesAndBackendFlagsWithoutPrivateFields', (tester) async {
    await pumpScreen(tester, backend.client());
    await tester.tap(find.text('Chi tiết ca'));
    await tester.pumpAndSettle();
    expect(find.text('Người phụ trách ca'), findsOneWidget);
    expect(find.text('[Họ tên NV 1]'), findsNWidgets(2));
    expect(find.text('[Họ tên NV 2]'), findsOneWidget);
    expect(find.text('Bác sĩ trực'), findsOneWidget);
    expect(find.text('Vắng ca'), findsOneWidget);
    expect(find.text('Nghỉ được duyệt'), findsOneWidget);
    expect(find.text('private-phone'), findsNothing);
    expect(find.text('private-email'), findsNothing);
    expect(find.text('[Người ngoài ca]'), findsNothing);
    await tester.tap(find.byTooltip('Đóng chi tiết ca'));
    await tester.pumpAndSettle();
    expect(find.text('Người phụ trách ca'), findsNothing);
  });

  testWidgets('FR_054_filteredNamesAreNotInventedAndMissingLeadIsExplicit', (tester) async {
    backend.on('GET', '/me/shifts', 200, [nightShift(leadStaffId: null)]);
    backend.on('GET', '/staff', 200, []);
    await pumpScreen(tester, backend.client());
    await tester.tap(find.text('Chi tiết ca'));
    await tester.pumpAndSettle();
    expect(find.text('Chưa có người phụ trách'), findsOneWidget);
    expect(find.text('Chưa có thông tin được phép xem'), findsNWidgets(2));
    expect(find.textContaining('#11'), findsNothing);
  });

  testWidgets('FR_033_emptyMonthAndNavigationUseOwnDateRange', (tester) async {
    backend.on('GET', '/me/shifts', 200, []);
    await pumpScreen(tester, backend.client());
    expect(find.text('Bạn không có ca nào trong tháng này.'), findsOneWidget);
    await tester.tap(find.text('Tháng sau'));
    await tester.pumpAndSettle();
    expect(find.text('Tháng 11/2026'), findsOneWidget);
    expect(backend.calls('GET', '/me/shifts').last.url.queryParameters, {'from': '2026-11-01', 'to': '2026-12-01'});
    await tester.tap(find.text('Tháng trước'));
    await tester.pumpAndSettle();
    expect(find.text('Tháng 10/2026'), findsOneWidget);
  });

  testWidgets('FR_054_forbiddenRefreshHidesOldShiftsAndDisplaysBackendDetail', (tester) async {
    await pumpScreen(tester, backend.client());
    backend.on('GET', '/me/shifts', 403, {'detail': 'Bạn không có quyền xem lịch ca.'});
    await tester.tap(find.text('Làm mới'));
    await tester.pumpAndSettle();
    expect(find.text('Bạn không có quyền xem lịch ca.'), findsOneWidget);
    expect(find.text('Chi tiết ca'), findsNothing);
    backend.on('GET', '/me/shifts', 200, []);
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();
    expect(find.text('Bạn không có ca nào trong tháng này.'), findsOneWidget);
  });

  testWidgets('FR_033_offlineExplainsRetryAndDoesNotClaimCalendarIsQueued', (tester) async {
    backend.down = true;
    await pumpScreen(tester, backend.client());
    expect(find.text('Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.'), findsOneWidget);
    expect(find.textContaining('Bản ghi sẽ đồng bộ'), findsNothing);
    backend.down = false;
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();
    expect(find.text('Chi tiết ca'), findsOneWidget);
  });

  testWidgets('FR_033_showsLoadingUntilBackendReplies', (tester) async {
    final reply = Completer<http.Response>();
    final api = ApiClient(baseUrl: 'http://backend.test', httpClient: MockClient((_) => reply.future));
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(body: MyShiftsScreen(api: api)),
      ),
    );
    await tester.pump();
    expect(find.text('Đang tải lịch ca…'), findsOneWidget);
    reply.complete(http.Response('[]', 200));
    await tester.pumpAndSettle();
    expect(find.text('Đang tải lịch ca…'), findsNothing);
    expect(find.text('Bạn không có ca nào trong tháng này.'), findsOneWidget);
  });

  for (final (status, label) in [
    ('DRAFT', 'Nháp'),
    ('PUBLISHED', 'Đã công bố'),
    ('IN_PROGRESS', 'Đang diễn ra'),
    ('AWAITING_HANDOVER', 'Chờ bàn giao'),
    ('CLOSED', 'Đã đóng'),
    ('CANCELLED', 'Đã hủy'),
  ]) {
    testWidgets('FR_022_displaysBackendStatus_$status', (tester) async {
      backend.on('GET', '/me/shifts', 200, [nightShift(status: status)]);
      await pumpScreen(tester, backend.client());
      expect(find.text(label), findsOneWidget);
    });
  }

  for (final (width, dark) in [(320.0, false), (320.0, true), (960.0, true)]) {
    testWidgets('FR_054_layoutAt200PercentText_width${width}_dark$dark', (tester) async {
      tester.view.physicalSize = Size(width, 1000);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await pumpScreen(tester, backend.client(), dark: dark, scale: 2);
      expect(tester.takeException(), isNull);
      final details = find.text('Chi tiết ca');
      await tester.ensureVisible(details);
      await tester.pumpAndSettle();
      await tester.tap(details);
      await tester.pumpAndSettle();
      expect(find.byTooltip('Đóng chi tiết ca'), findsOneWidget);
      expect(tester.takeException(), isNull);
      await tester.ensureVisible(find.text('[Họ tên NV 2]'));
      await tester.pumpAndSettle();
      expect(find.text('[Họ tên NV 2]').hitTestable(), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }
}
