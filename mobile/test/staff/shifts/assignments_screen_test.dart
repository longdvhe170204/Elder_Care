import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/shifts/assignments_screen.dart';
import 'assignments_controller_test.dart' show fixture;

void main() {
  testWidgets('FR_054_xem phân công, giờ ca qua đêm và không có lệnh quản lý', (tester) async {
    final b = fixture(manage: false)
      ..on('GET', '/floors', 200, [
        {'id': 2, 'name': '[Tầng kiểm thử]'},
      ])
      ..on('GET', '/shifts/12', 200, {
        'id': 12,
        'status': 'PUBLISHED',
        'startsAt': '2026-11-07T15:00:00Z',
        'endsAt': '2026-11-07T23:00:00Z',
        'allowedCommands': <String>[],
      })
      ..on('GET', '/shifts/12/assignments', 200, [
        {
          'id': 22,
          'staffId': 9,
          'targetType': 'FLOOR',
          'targetId': 2,
          'role': 'CAREGIVER',
          'assignmentKind': 'PRIMARY',
          'status': 'SCHEDULED',
          'startsAt': '2026-11-07T15:00:00Z',
          'endsAt': '2026-11-07T23:00:00Z',
        },
      ]);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: AssignmentsScreen(api: b.client(), initialMonth: DateTime(2026, 11)),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byType(DropdownButtonFormField<int>));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('07/11/2026 22:00').last);
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('[Nhân viên 1]'), 150);
    expect(find.text('[Tầng kiểm thử]'), findsOneWidget);
    expect(find.text('Tương lai'), findsOneWidget);
    expect(find.textContaining('08/11/2026 06:00'), findsWidgets);
    expect(find.text('Lập phân công'), findsNothing);
    expect(find.text('Xóa Nháp'), findsNothing);
    expect(tester.takeException(), isNull);
  });
  for (final dark in [false, true]) {
    testWidgets('FR_054_màn rỗng, cuộn ở 200 phần trăm, tối=$dark', (tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final b = fixture(manage: false);
      await tester.pumpWidget(
        MaterialApp(
          theme: dark ? AppTheme.dark() : AppTheme.light(),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(context).copyWith(textScaler: const TextScaler.linear(2)),
            child: child!,
          ),
          home: Scaffold(
            body: AssignmentsScreen(api: b.client(), initialMonth: DateTime(2026, 11)),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Chọn ca'), findsOneWidget);
      expect(find.text('Lập phân công'), findsNothing);
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets('FR_054_ẩn dữ liệu khi không có quyền xem và cho thử lại lỗi tải', (tester) async {
    final b = fixture()..on('GET', '/me/permissions', 403, {'detail': 'Không được phép'});
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(body: AssignmentsScreen(api: b.client())),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Không được phép'), findsOneWidget);
    expect(find.text('Thử lại'), findsOneWidget);
    expect(b.calls('GET', '/shift-schedules'), isEmpty);
  });
}
