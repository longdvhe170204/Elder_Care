import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/shifts/shift_absence_sheet.dart';

import '../../support/fake_backend.dart';

Map<String, dynamic> shift({bool allowed = true}) => {
  'id': 21,
  'leadStaffId': 11,
  'allowedCommands': [if (allowed) 'recordAbsence'],
  'staff': [
    {'staffId': 11, 'absent': false, 'approvedLeave': false},
    {'staffId': 12, 'absent': false, 'approvedLeave': false},
    {'staffId': 13, 'absent': true, 'approvedLeave': false},
    {'staffId': 14, 'absent': false, 'approvedLeave': true},
  ],
};

const names = {11: '[Người phụ trách]', 12: '[Nhân viên cùng ca]', 13: '[Người đã vắng]', 14: '[Người nghỉ có duyệt]'};

Future<void> openSheet(
  WidgetTester tester,
  ApiClient api, {
  bool allowed = true,
  double scale = 1,
  bool dark = false,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: dark ? AppTheme.dark() : AppTheme.light(),
      builder: (context, child) => MediaQuery(
        data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(scale)),
        child: child!,
      ),
      home: Builder(
        builder: (context) => Scaffold(
          body: TextButton(
            onPressed: () => showModalBottomSheet<bool>(
              context: context,
              isScrollControlled: true,
              useSafeArea: true,
              builder: (_) => ShiftAbsenceSheet(
                api: api,
                shift: shift(allowed: allowed),
                names: names,
              ),
            ),
            child: const Text('Mở ghi vắng'),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text('Mở ghi vắng'));
  await tester.pumpAndSettle();
}

Future<void> choose(WidgetTester tester, String field, String option) async {
  final widget = find.byKey(ValueKey(field));
  await tester.ensureVisible(widget);
  await tester.tap(widget);
  await tester.pumpAndSettle();
  await tester.tap(find.text(option).last);
  await tester.pumpAndSettle();
}

Future<void> submit(WidgetTester tester) async {
  final button = find.widgetWithText(FilledButton, 'Xác nhận ghi vắng');
  await tester.ensureVisible(button);
  await tester.tap(button);
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;
  setUp(() {
    backend = FakeBackend()..on('POST', '/shifts/21/record-absence', 200);
  });

  testWidgets('FR_020_requiresStaffAndReasonAndDoesNotOfferAlreadyAbsentOrOnLeave', (tester) async {
    await openSheet(tester, backend.client());
    await submit(tester);
    expect(backend.calls('POST', '/shifts/21/record-absence'), isEmpty);
    expect(find.text('Chọn nhân viên vắng.'), findsOneWidget);
    expect(find.text('Nhập lý do trước khi xác nhận.'), findsOneWidget);
    await tester.tap(find.byKey(const ValueKey('absent-staff')));
    await tester.pumpAndSettle();
    expect(find.text('[Người đã vắng]'), findsNothing);
    expect(find.text('[Người nghỉ có duyệt]'), findsNothing);
  });

  testWidgets('FR_020_sendsLeadReplacementReasonAndIdempotencyWithoutExtraHeaders', (tester) async {
    await openSheet(tester, backend.client());
    await choose(tester, 'absent-staff', '[Người phụ trách]');
    await choose(tester, 'replacement-lead', '[Nhân viên cùng ca]');
    await tester.enterText(find.byKey(const ValueKey('absence-reason')), '  Việc đột xuất  ');
    await submit(tester);
    final call = backend.calls('POST', '/shifts/21/record-absence').single;
    expect(jsonDecode(call.body), {'staffId': 11, 'reason': 'Việc đột xuất', 'replacementLeadStaffId': 12});
    expect(
      call.headers['Idempotency-Key'],
      matches(RegExp(r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$')),
    );
    expect(call.headers.containsKey('If-Match'), isFalse);
    expect(find.text('Ghi nhận vắng ca'), findsNothing);
  });

  testWidgets('FR_020_serverWindowRejectionPreservesInputAndHasNoSuccessMessage', (tester) async {
    backend.on('POST', '/shifts/21/record-absence', 409, {
      'errorCode': 'OUTSIDE_ABSENCE_REPORTING_WINDOW',
      'detail': 'Ngoài khoảng ghi nhận vắng ca.',
    });
    await openSheet(tester, backend.client());
    await choose(tester, 'absent-staff', '[Nhân viên cùng ca]');
    await tester.enterText(find.byKey(const ValueKey('absence-reason')), 'Đã báo vắng');
    await submit(tester);
    expect(find.text('Ngoài khoảng ghi nhận vắng ca.'), findsOneWidget);
    expect(find.text('Đã báo vắng'), findsOneWidget);
    expect(jsonDecode(backend.calls('POST', '/shifts/21/record-absence').single.body), {
      'staffId': 12,
      'reason': 'Đã báo vắng',
    });
  });

  testWidgets('FR_020_unknownNetworkOutcomeKeepsPayloadAndReusesKeyOnRetry', (tester) async {
    await openSheet(tester, backend.client());
    await choose(tester, 'absent-staff', '[Nhân viên cùng ca]');
    await tester.enterText(find.byKey(const ValueKey('absence-reason')), 'Lý do giữ lại');
    backend.down = true;
    await submit(tester);
    expect(find.text('Chưa nhận được xác nhận từ máy chủ. Giữ nguyên nội dung để thử lại.'), findsOneWidget);
    expect(find.text('Lý do giữ lại'), findsOneWidget);
    backend.down = false;
    await submit(tester);
    final calls = backend.calls('POST', '/shifts/21/record-absence');
    expect(calls, hasLength(2));
    expect(calls.first.headers['Idempotency-Key'], calls.last.headers['Idempotency-Key']);
    expect(calls.first.body, calls.last.body);
  });

  testWidgets('FR_053_lostPermissionClearsReasonAndStopsFurtherSubmissions', (tester) async {
    backend.on('POST', '/shifts/21/record-absence', 403, {
      'errorCode': 'PERMISSION_DENIED',
      'detail': 'Không còn quyền ghi vắng ca.',
    });
    await openSheet(tester, backend.client());
    await choose(tester, 'absent-staff', '[Nhân viên cùng ca]');
    await tester.enterText(find.byKey(const ValueKey('absence-reason')), 'Thông tin riêng');
    await submit(tester);
    expect(find.text('Không còn quyền ghi vắng ca.'), findsOneWidget);
    expect(find.text('Thông tin riêng'), findsNothing);
    expect(find.widgetWithText(FilledButton, 'Xác nhận ghi vắng'), findsNothing);
  });

  testWidgets('FR_020_deniedCommandHasNoFormAndLargeTextDoesNotOverflow', (tester) async {
    await openSheet(tester, backend.client(), allowed: false, scale: 2);
    expect(find.widgetWithText(FilledButton, 'Xác nhận ghi vắng'), findsNothing);
    expect(tester.takeException(), isNull);
    expect(backend.requests, isEmpty);
  });

  for (final dark in [false, true]) {
    testWidgets('FR_020_formSupportsLargeTextIn${dark ? "Dark" : "Light"}Theme', (tester) async {
      await openSheet(tester, backend.client(), scale: 2, dark: dark);
      expect(tester.takeException(), isNull);
      await choose(tester, 'absent-staff', '[Người phụ trách]');
      await tester.ensureVisible(find.byKey(const ValueKey('replacement-lead')));
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('FR_020_busySubmissionCannotBeSentTwice', (tester) async {
    final response = Completer<http.Response>();
    var count = 0;
    final api = ApiClient(
      baseUrl: 'http://backend.test',
      httpClient: MockClient((request) {
        count++;
        return response.future;
      }),
    );
    await openSheet(tester, api);
    await choose(tester, 'absent-staff', '[Nhân viên cùng ca]');
    await tester.enterText(find.byKey(const ValueKey('absence-reason')), 'Một lần');
    final button = find.widgetWithText(FilledButton, 'Xác nhận ghi vắng');
    await tester.ensureVisible(button);
    await tester.tap(button);
    await tester.pump();
    await tester.tap(button);
    await tester.pump();
    expect(count, 1);
    response.complete(http.Response('', 200));
    await tester.pumpAndSettle();
  });
}
