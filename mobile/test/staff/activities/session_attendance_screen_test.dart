import 'dart:convert';

import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/activities/session_attendance_screen.dart';

import '../../support/fake_backend.dart';

/// 014 T013, T014 (FR-027 → FR-030, SC-009; bố cục web Phase 5 duyệt 05/11): điểm danh buổi trên app, mỗi đăng ký một
/// dòng; lưu qua hàng đợi ngoại tuyến chung (một phong bì M04.SESSION_ATTENDANCE), mất mạng thì giữ trên máy.
void main() {
  late FakeBackend backend;
  late OfflineDatabase db;
  late OfflineQueue queue;

  setUp(() {
    backend = FakeBackend();
    db = OfflineDatabase(NativeDatabase.memory());
  });
  tearDown(() => db.close());

  Map<String, dynamic> session(int count) => {
    'id': 42,
    'activityId': 3,
    'startsAt': '2026-11-07T02:00:00Z',
    'endsAt': '2026-11-07T03:00:00Z',
    'status': 'SCHEDULED',
    'registrations': [
      for (var i = 0; i < count; i++)
        {'id': 900 + i, 'residentId': 700 + i, 'source': 'MANUAL', 'status': 'REGISTERED'},
    ],
  };

  Future<void> open(WidgetTester tester, int count) async {
    queue = OfflineQueue(db, backend.client(), newKey: () => 'attendance-key');
    backend.on('GET', '/activity-sessions/42', 200, session(count));
    backend.on('GET', '/residents', 200, {
      'items': [
        for (var i = 0; i < count; i++) {'id': 700 + i, 'fullName': 'Người cao tuổi ${i + 1}'},
      ],
    });
    tester.view.physicalSize = const Size(1080, 2400);
    addTearDown(tester.view.resetPhysicalSize);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: SessionAttendanceScreen(api: backend.client(), queue: queue, accountId: 7, sessionId: 42),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> present(WidgetTester tester, int index) async {
    final row = find.byKey(ValueKey('attendance-${900 + index}'));
    await tester.scrollUntilVisible(row, 300, scrollable: find.byType(Scrollable).first);
    await tester.tap(find.descendant(of: row, matching: find.text('Có mặt')));
    await tester.pump();
    final level = find.descendant(of: row, matching: find.text('Bình thường'));
    await tester.ensureVisible(level);
    await tester.pumpAndSettle();
    await tester.tap(level);
    await tester.pump();
  }

  testWidgets('SC_009_twentyResidentsAreRecordedInOneEnvelopeThroughTheSharedQueue', (tester) async {
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': 'attendance-key', 'result': 'ACCEPTED'},
      ],
    });
    await open(tester, 20);
    expect(find.text('Lưu điểm danh (0/20)'), findsOneWidget);
    for (var i = 0; i < 20; i++) {
      await present(tester, i);
    }
    await tester.tap(find.text('Lưu điểm danh (20/20)'));
    await tester.pumpAndSettle();

    expect(find.text('Đã lưu điểm danh'), findsOneWidget);
    expect(backend.calls('POST', '/activity-sessions/42/attendance'), isEmpty);
    final sync = jsonDecode(backend.calls('POST', '/sync').single.body) as Map<String, dynamic>;
    final record = (sync['records'] as List).single as Map<String, dynamic>;
    expect(record['recordType'], 'M04.SESSION_ATTENDANCE');
    final content = record['content'] as Map<String, dynamic>;
    expect(content['sessionId'], 42);
    expect(content['results'] as List, hasLength(20));
    expect((content['results'] as List).first, {
      'residentId': 700,
      'result': 'PRESENT',
      'participationLevel': 'NORMAL',
    });
  });

  testWidgets('FR_028_absentNeedsAReasonAndOfflineKeepsTheRecordOnTheDevice', (tester) async {
    await open(tester, 2);
    final row = find.byKey(const ValueKey('attendance-900'));
    await tester.tap(find.descendant(of: row, matching: find.text('Vắng')));
    await tester.pump();
    expect(find.text('Lưu điểm danh (0/2)'), findsOneWidget);
    await tester.tap(find.descendant(of: row, matching: find.text('Sức khỏe')));
    await tester.pump();
    backend.down = true;
    await tester.tap(find.text('Lưu điểm danh (1/2)'));
    await tester.pumpAndSettle();

    expect(find.text('Đã lưu trên máy, sẽ gửi khi có mạng'), findsOneWidget);
    final pending = await queue.unsynced(7);
    expect(pending.single.recordType, 'M04.SESSION_ATTENDANCE');
    expect((jsonDecode(pending.single.content) as Map<String, dynamic>)['results'], [
      {'residentId': 700, 'result': 'ABSENT', 'absenceReason': 'HEALTH'},
    ]);
  });
}
