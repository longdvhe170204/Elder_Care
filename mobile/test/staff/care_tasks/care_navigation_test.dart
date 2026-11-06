import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';

import '../../support/fake_backend.dart';

void main() {
  testWidgets('FR_049_careRemainsReachableThroughMoreAtLargeText', (tester) async {
    tester.view.physicalSize = const Size(320, 1000);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 2;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    final backend = FakeBackend()
      ..on('GET', '/me', 200, user('STAFF'))
      ..on('GET', '/me/permissions', 200, [
        {'function': 'TASK_CHECKLIST', 'operation': 'VIEW'},
      ]);
    final store = MemoryStore()..values[SessionController.tokenKey] = 'test-session';
    final current = session(backend, store, FakeBiometric(available: false));
    await current.start();
    await tester.pumpWidget(ElderCareApp(session: current, queue: memoryQueue(current)));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Thêm'));
    await tester.pumpAndSettle();
    expect(find.byType(NavigationDestination), findsNWidgets(5));
    await tester.tap(find.text('Chăm sóc hằng ngày'));
    await tester.pumpAndSettle();
    expect(find.text('Checklist ca'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
