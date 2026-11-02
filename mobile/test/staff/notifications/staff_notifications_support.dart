import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/notifications/push_coordinator.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/notifications/staff_inbox_screen.dart';

Map<String, dynamic> notification({
  int id = 1,
  String level = 'LOW',
  String title = 'Yêu cầu YC-2026-000050',
  String? acknowledgedAt,
  bool canTakeCharge = false,
  Map<String, dynamic>? sourceRecord,
}) => {
  'id': id,
  'level': level,
  'eventType': 'APPROVAL_REQUEST',
  'title': title,
  'content': const <Map<String, dynamic>>[],
  'hiddenParts': const <String>[],
  'identityOnly': false,
  'status': 'SENT',
  'sentAt': '2026-11-01T03:00:00Z',
  'viewedAt': null,
  'acknowledgedAt': acknowledgedAt,
  'sourceRecord': sourceRecord,
  'canAcknowledgeAndTakeCharge': canTakeCharge,
};

Map<String, dynamic> page(List<Map<String, dynamic>> items, {int unread = 0, int urgent = 0}) => {
  'items': items,
  'unreadCount': unread,
  'unacknowledgedUrgentCount': urgent,
  'nextCursor': null,
};

Map<String, dynamic> reminder({
  int id = 8,
  String status = 'AWAITING_RESULT',
  String? phone = '0901234567',
  bool canRecord = true,
  String? stopReason,
  List<Map<String, dynamic>> results = const [],
  List<Map<String, dynamic>> handoffs = const [],
}) => {
  'id': id,
  'callChainId': 3,
  'personToCall': {'type': 'FAMILY_MEMBER', 'fullName': 'Trần Thị Mai', 'relationship': null, 'phoneNumber': phone},
  'residentId': 12,
  'contentToReport': const [
    {'occurredAt': '2026-11-01T03:02:00Z', 'content': 'Té ngã ở phòng 205, đã sơ cứu'},
  ],
  'currentlyReminded': 'Lê Văn Hùng',
  'dueAt': '2026-11-01T03:15:00Z',
  'status': status,
  'stopReason': stopReason,
  'handoffs': handoffs,
  'results': results,
  'canRecordResult': canRecord,
};

Future<void> pumpInbox(
  WidgetTester tester,
  ApiClient api, {
  PushCoordinator? push,
  Future<bool> Function(String)? dial,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: StaffInboxScreen(api: api, push: push, dial: dial ?? (_) async => true),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Finder button(String label) =>
    find.ancestor(of: find.text(label), matching: find.byWidgetPredicate((w) => w is ButtonStyleButton));

Future<void> tapButton(WidgetTester tester, String label) async {
  await tester.ensureVisible(button(label).first);
  await tester.pumpAndSettle();
  await tester.tap(button(label).first);
  await tester.pumpAndSettle();
}
