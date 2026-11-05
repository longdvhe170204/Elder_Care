import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';

import '../../support/fake_backend.dart';

Future<void> pumpStaff(WidgetTester tester, FakeBackend backend) async {
  backend.on('GET', '/me', 200, user('STAFF'));
  final store = MemoryStore()..values[SessionController.tokenKey] = 'test-session';
  final current = session(backend, store, FakeBiometric(available: false));
  await current.start();
  await tester.pumpWidget(ElderCareApp(session: current, queue: memoryQueue(current)));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_054_myShiftsRequiresViewPermissionAndIsReachableThroughMore', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/me/permissions', 200, [
        {'function': 'SHIFT_SCHEDULE', 'operation': 'VIEW'},
      ])
      ..on('GET', '/me/shifts', 200, [])
      ..on('GET', '/staff', 200, []);

    await pumpStaff(tester, backend);
    expect(find.byType(NavigationDestination), findsNWidgets(5));
    expect(find.text('Thêm'), findsOneWidget);
    expect(backend.calls('GET', '/me/shifts'), isEmpty);
    await tester.tap(find.text('Thêm'));
    await tester.pumpAndSettle();
    expect(find.text('Phê duyệt'), findsOneWidget);
    await tester.tap(find.text('Ca của tôi'));
    await tester.pumpAndSettle();

    expect(find.text('Bạn không có ca nào trong tháng này.'), findsOneWidget);
    expect(backend.calls('GET', '/me/shifts'), hasLength(1));
    expect(backend.calls('GET', '/me/permissions').single.headers['Authorization'], 'Bearer test-session');
    expect(tester.widget<NavigationBar>(find.byType(NavigationBar)).selectedIndex, 4);
    expect(tester.takeException(), isNull);
  });

  for (final grants in <List<Map<String, dynamic>>>[
    [],
    [
      {'function': 'SHIFT_SCHEDULE', 'operation': 'CREATE'},
    ],
  ]) {
    testWidgets('FR_054_doesNotExposeMyShiftsWithoutView_$grants', (tester) async {
      final backend = FakeBackend()..on('GET', '/me/permissions', 200, grants);
      await pumpStaff(tester, backend);
      expect(find.text('Ca của tôi'), findsNothing);
      expect(find.text('Thêm'), findsNothing);
      expect(find.byType(NavigationDestination), findsNWidgets(5));
      expect(backend.calls('GET', '/me/shifts'), isEmpty);
    });
  }

  testWidgets('FR_054_moreMenuWorksAt200PercentTextOnNarrowPhone', (tester) async {
    tester.view.physicalSize = const Size(320, 1000);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 2;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    final backend = FakeBackend()
      ..on('GET', '/me/permissions', 200, [
        {'function': 'SHIFT_SCHEDULE', 'operation': 'VIEW'},
      ])
      ..on('GET', '/me/shifts', 200, [])
      ..on('GET', '/staff', 200, []);

    await pumpStaff(tester, backend);
    await tester.tap(find.text('Thêm'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await tester.tap(find.text('Ca của tôi'));
    await tester.pumpAndSettle();
    expect(find.text('Bạn không có ca nào trong tháng này.'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
