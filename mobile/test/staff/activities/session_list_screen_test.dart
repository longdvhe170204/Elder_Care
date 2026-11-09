import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/activities/session_attendance_screen.dart';
import 'package:mobile/staff/activities/session_list_screen.dart';

import '../../support/fake_backend.dart';

/// 014 T013, T014 phần app (team-plan mục 8, dòng "SessionAttendanceScreen cần sessionId"): danh sách buổi trong viện
/// của ngày theo quyền ACTIVITY_OUTING, mở màn điểm danh; chuyến đi nằm ở màn chuyến riêng.
void main() {
  late FakeBackend backend;
  late OfflineDatabase db;

  setUp(() {
    backend = FakeBackend();
    db = OfflineDatabase(NativeDatabase.memory());
    backend.on('GET', '/me/permissions', 200, [
      {'function': 'ACTIVITY_OUTING', 'operation': 'VIEW'},
    ]);
    backend.on('GET', '/activity-sessions', 200, [
      {'id': 42, 'activityId': 3, 'isTrip': false, 'startsAt': '2026-11-07T02:00:00Z', 'status': 'SCHEDULED'},
      {'id': 43, 'activityId': 3, 'isTrip': false, 'startsAt': '2026-11-07T07:00:00Z', 'status': 'CANCELLED'},
      {'id': 41, 'activityId': 5, 'isTrip': true, 'startsAt': '2026-11-07T00:00:00Z', 'status': 'SCHEDULED'},
    ]);
    backend.on('GET', '/activities', 200, [
      {'id': 3, 'name': 'Cờ tướng'},
      {'id': 5, 'name': 'Tham quan'},
    ]);
    backend.on('GET', '/activity-sessions/42', 200, {
      'id': 42,
      'activityId': 3,
      'startsAt': '2026-11-07T02:00:00Z',
      'endsAt': '2026-11-07T03:00:00Z',
      'status': 'SCHEDULED',
      'registrations': <Object>[],
    });
    backend.on('GET', '/residents', 200, {'items': <Object>[]});
  });
  tearDown(() => db.close());

  Future<void> open(WidgetTester tester, {double textScale = 1}) async {
    final api = backend.client();
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(textScale)),
          child: child!,
        ),
        home: SessionListScreen(
          api: api,
          queue: OfflineQueue(db, api, newKey: () => 'k'),
          accountId: 7,
          date: '2026-11-07',
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('FR_027_danhSachBuoiTrongNgayMoDiemDanhKhongCoChuyen', (tester) async {
    await open(tester);
    expect(backend.calls('GET', '/activity-sessions').single.url.queryParameters, {
      'from': '2026-11-07',
      'to': '2026-11-07',
    });
    expect(find.text('Tham quan'), findsNothing);
    expect(find.text('Cờ tướng'), findsNWidgets(2));
    expect(find.text('Đã lên lịch'), findsOneWidget);
    expect(find.text('Đã hủy'), findsOneWidget);

    // Buổi đã hủy không mở điểm danh.
    await tester.tap(find.text('Đã hủy'));
    await tester.pumpAndSettle();
    expect(find.byType(SessionAttendanceScreen), findsNothing);

    await tester.tap(find.text('Đã lên lịch'));
    await tester.pumpAndSettle();
    expect(find.byType(SessionAttendanceScreen), findsOneWidget);
    expect(backend.calls('GET', '/activity-sessions/42'), hasLength(1));
  });

  testWidgets('FR_067_khongCoQuyenXemThiKhongTaiBuoi', (tester) async {
    backend.on('GET', '/me/permissions', 200, <Object>[]);
    await open(tester);
    expect(find.text('Bạn chưa có quyền xem buổi hoạt động.'), findsOneWidget);
    expect(backend.calls('GET', '/activity-sessions'), isEmpty);
  });

  testWidgets('NFR_10_chu200PhanTramVanMoDuocBuoi', (tester) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(360, 800);
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await open(tester, textScale: 2);
    expect(tester.takeException(), isNull);
    await tester.tap(find.text('Đã lên lịch'));
    await tester.pumpAndSettle();
    expect(find.byType(SessionAttendanceScreen), findsOneWidget);
  });
}
