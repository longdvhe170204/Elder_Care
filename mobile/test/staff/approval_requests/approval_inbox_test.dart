import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/approval_requests/approval_inbox_screen.dart';

import '../../support/fake_backend.dart';

// 000 T098 (tên logic hop_duyet_test; US5; FR-031 → FR-040): hộp duyệt trên app nhân viên, chỉ trực tuyến (R-13): ba hộp,
// mở chi tiết; nút theo allowedCommands; từ chối bắt buộc ý kiến; 422 báo tại ô; 409 người khác đã xử lý; 412 tải lại.

Map<String, dynamic> request({
  int id = 5,
  String status = 'PENDING_APPROVAL',
  List<String> commands = const ['approve', 'reject'],
  int version = 2,
}) => {
  'id': id,
  'displayCode': 'YC-$id',
  'type': 'M15.CORRECTION',
  'status': status,
  'content': {'field': 'Giá trị mới'},
  'requester': {'id': 3, 'type': 'ACCOUNT', 'fullName': '[Họ tên 2]'},
  'createdAt': '2026-10-27T00:10:00Z',
  'reason': 'Ghi nhầm',
  'requestedEffectiveDate': '2026-11-02',
  'allowedCommands': commands,
  'version': version,
};

Map<String, dynamic> page(List<Map<String, dynamic>> items, {String? next}) => {'items': items, 'nextCursor': next};

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: ApprovalInboxScreen(api: api)),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> openRow(WidgetTester tester) async {
  await tester.tap(find.text('Đính chính · YC-5'));
  await tester.pumpAndSettle();
}

/// Nút có chữ đúng `label`, mọi kiểu nút (`byType` chỉ khớp đúng lớp).
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
    backend.on('GET', '/approval-requests', 200, page([request()]));
    backend.on('GET', '/approval-requests/5', 200, request());
    backend.on('GET', '/audit-logs/entities/M15.APPROVAL_REQUEST/5', 200, {'items': <Object>[]});
  });

  testWidgets('T098_awaitingMyApproval_showsCodeTypeStatusRequester', (tester) async {
    await pumpScreen(tester, backend.client());

    expect(backend.requests.first.url.queryParameters['inbox'], 'AWAITING_MY_APPROVAL');
    expect(find.text('Đính chính · YC-5'), findsOneWidget);
    expect(find.text('Chờ duyệt'), findsOneWidget);
    expect(find.textContaining('[Họ tên 2]'), findsOneWidget);
    expect(find.textContaining('02/11/2026'), findsOneWidget);
  });

  testWidgets('T098_switchInbox_reloads', (tester) async {
    await pumpScreen(tester, backend.client());

    await tester.tap(find.text('Của tôi'));
    await tester.pumpAndSettle();

    expect(backend.requests.last.url.queryParameters['inbox'], 'MINE');
  });

  testWidgets('T098_awaitingCount_onChip_plusWhenMorePages', (tester) async {
    backend.on('GET', '/approval-requests', 200, page([request(), request(id: 6)], next: 'abc'));
    await pumpScreen(tester, backend.client());

    expect(find.text('Chờ tôi duyệt (2+)'), findsOneWidget);
  });

  testWidgets('T098_filters_sentToBackend_countKeptFromUnfilteredInbox', (tester) async {
    await pumpScreen(tester, backend.client());
    expect(find.text('Chờ tôi duyệt (1)'), findsOneWidget);

    backend.on('GET', '/approval-requests', 200, page([]));
    await tester.tap(find.byKey(const ValueKey('approval-status')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đã duyệt').last);
    await tester.pumpAndSettle();

    expect(backend.requests.last.url.queryParameters['status'], 'APPROVED');
    expect(backend.requests.last.url.queryParameters['inbox'], 'AWAITING_MY_APPROVAL');
    expect(find.text('Chờ tôi duyệt (1)'), findsOneWidget);

    await tester.tap(find.byKey(const ValueKey('approval-type')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đính chính').last);
    await tester.pumpAndSettle();

    expect(backend.requests.last.url.queryParameters['type'], 'M15.CORRECTION');
    expect(backend.requests.last.url.queryParameters['status'], 'APPROVED');
  });

  testWidgets('T098_twoCommands_shareTheWidth', (tester) async {
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    final approve = tester.getSize(button('Duyệt'));
    final reject = tester.getSize(button('Từ chối'));
    expect(approve.width, reject.width);
    expect(approve.width + reject.width + 12 + 32, tester.getSize(find.byType(Scaffold).last).width);
  });

  testWidgets('T098_detailHeader_isNightBlue', (tester) async {
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    final bar = tester.widget<AppBar>(find.byType(AppBar));
    expect(bar.backgroundColor, Theme.of(tester.element(find.byType(AppBar))).extension<Tones>()!.header);
  });

  testWidgets('T098_empty_saysSo', (tester) async {
    backend.on('GET', '/approval-requests', 200, page([]));
    await pumpScreen(tester, backend.client());

    expect(find.text('Không có yêu cầu nào'), findsOneWidget);
  });

  testWidgets('R_13_offline_saysNeedsNetwork_retryLoads', (tester) async {
    backend.down = true;
    await pumpScreen(tester, backend.client());

    expect(find.textContaining('Kiểm tra mạng'), findsOneWidget);

    backend.down = false;
    await tapButton(tester, 'Thử lại');

    expect(find.text('Đính chính · YC-5'), findsOneWidget);
  });

  testWidgets('T098_seeMore_appendsNextPage', (tester) async {
    backend.on('GET', '/approval-requests', 200, page([request()], next: 'abc'));
    await pumpScreen(tester, backend.client());

    backend.on('GET', '/approval-requests', 200, page([request(id: 6)]));
    await tapButton(tester, 'Xem thêm');

    expect(backend.requests.last.url.queryParameters['cursor'], 'abc');
    expect(find.text('Đính chính · YC-5'), findsOneWidget);
    expect(find.text('Đính chính · YC-6'), findsOneWidget);
  });

  testWidgets('FR_034_approve_sendsIfMatch_updatesStatus', (tester) async {
    backend.on(
      'POST',
      '/approval-requests/5/approve',
      200,
      request(status: 'APPROVED', commands: const [], version: 3),
    );
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    await tapButton(tester, 'Duyệt');

    final call = backend.calls('POST', '/approval-requests/5/approve').single;
    expect(call.headers['If-Match'], '"2"');
    expect(find.text('Đã duyệt'), findsOneWidget);
    expect(button('Duyệt'), findsNothing);
  });

  testWidgets('FR_035_reject_requiresComment_thenSends', (tester) async {
    backend.on('POST', '/approval-requests/5/reject', 200, request(status: 'REJECTED', commands: const []));
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    await tapButton(tester, 'Từ chối');
    expect(find.text('Nhập ý kiến khi từ chối'), findsOneWidget);
    expect(backend.calls('POST', '/approval-requests/5/reject'), isEmpty);

    await tester.enterText(find.byType(TextField), 'Thiếu chứng từ');
    await tapButton(tester, 'Từ chối');

    final body = jsonDecode(backend.calls('POST', '/approval-requests/5/reject').single.body) as Map<String, dynamic>;
    expect(body['comment'], 'Thiếu chứng từ');
  });

  testWidgets('T098_commandsFollowAllowedCommands', (tester) async {
    backend.on('GET', '/approval-requests/5', 200, request(commands: const ['cancel']));
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    expect(button('Duyệt'), findsNothing);
    expect(button('Từ chối'), findsNothing);
    expect(button('Hủy yêu cầu'), findsOneWidget);
  });

  testWidgets('FR_033_submitDraft', (tester) async {
    backend.on('GET', '/approval-requests/5', 200, request(status: 'DRAFT', commands: const ['submit']));
    backend.on('POST', '/approval-requests/5/submit', 200, request(commands: const []));
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    await tapButton(tester, 'Gửi duyệt');

    expect(backend.calls('POST', '/approval-requests/5/submit'), hasLength(1));
  });

  testWidgets('T098_cancel_requiresReason', (tester) async {
    backend.on('GET', '/approval-requests/5', 200, request(commands: const ['cancel']));
    backend.on('POST', '/approval-requests/5/cancel', 200, request(status: 'CANCELLED', commands: const []));
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    await tapButton(tester, 'Hủy yêu cầu');
    await tapButton(tester, 'Xác nhận hủy');
    expect(find.text('Nhập lý do hủy'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'Không cần nữa');
    await tapButton(tester, 'Xác nhận hủy');

    final body = jsonDecode(backend.calls('POST', '/approval-requests/5/cancel').single.body) as Map<String, dynamic>;
    expect(body['reason'], 'Không cần nữa');
  });

  testWidgets('US5_13_selfApprovalWithoutReason_422AtField', (tester) async {
    backend.on('POST', '/approval-requests/5/approve', 422, {'errorCode': 'REASON_REQUIRED', 'detail': 'Nhập lý do'});
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    await tapButton(tester, 'Duyệt');

    final field = tester.widget<TextField>(find.byType(TextField));
    expect(field.decoration?.errorText, 'Nhập lý do');
  });

  testWidgets('US5_12_alreadyProcessed_showsMessage_reloads', (tester) async {
    backend.on('POST', '/approval-requests/5/approve', 409, {
      'errorCode': 'ALREADY_PROCESSED',
      'detail': 'Yêu cầu đã được xử lý',
    });
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    backend.on('GET', '/approval-requests/5', 200, request(status: 'APPROVED', commands: const []));
    await tapButton(tester, 'Duyệt');

    expect(find.text('Yêu cầu đã được xử lý'), findsOneWidget);
    expect(find.text('Đã duyệt'), findsOneWidget);
  });

  testWidgets('R_13_commandOffline_notQueued_saysNeedsNetwork', (tester) async {
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    backend.down = true;
    await tapButton(tester, 'Duyệt');

    expect(find.textContaining('Kiểm tra mạng'), findsOneWidget);
    expect(find.text('Chờ duyệt'), findsOneWidget);
  });

  testWidgets('T098_timeline_hiddenWhenAuditForbidden', (tester) async {
    backend.on('GET', '/audit-logs/entities/M15.APPROVAL_REQUEST/5', 403, {'errorCode': 'PERMISSION_DENIED'});
    await pumpScreen(tester, backend.client());
    await openRow(tester);

    expect(find.text('Diễn biến'), findsNothing);
    expect(find.text('Lý do'), findsOneWidget);
  });
}
