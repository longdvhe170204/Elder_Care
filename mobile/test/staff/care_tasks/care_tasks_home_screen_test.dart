import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/care_tasks/care_checklist_cache.dart';
import 'package:mobile/staff/care_tasks/care_tasks_home_screen.dart';

import '../../support/fake_backend.dart';

void main() {
  testWidgets('BR_M15_20_revokedChecklistPermissionClearsOnlyOwnCache', (tester) async {
    final backend = FakeBackend()..on('GET', '/me/permissions', 200, <dynamic>[]);
    final cache = CareChecklistCache(NativeDatabase.memory());
    addTearDown(cache.close);
    await cache.save(7, 9, {'loadedAt': '2026-11-06T08:00:00Z', 'tasks': <dynamic>[]});
    await cache.save(8, 10, {'loadedAt': '2026-11-06T08:00:00Z', 'tasks': <dynamic>[]});
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: CareTasksHomeScreen(api: backend.client(), accountId: 7, cache: cache),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(await cache.shiftIds(7), isEmpty);
    expect(await cache.shiftIds(8), [10]);
  });
  testWidgets('BR_M15_20_homeCanReopenOwnCachedChecklistWithoutNetwork', (tester) async {
    final backend = FakeBackend()..down = true;
    final cache = CareChecklistCache(NativeDatabase.memory());
    addTearDown(cache.close);
    await cache.save(7, 9, {'loadedAt': '2026-11-06T08:00:00Z', 'tasks': <dynamic>[]});
    await cache.save(8, 10, {'loadedAt': '2026-11-06T08:00:00Z', 'tasks': <dynamic>[]});
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: CareTasksHomeScreen(api: backend.client(), accountId: 7, cache: cache),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Checklist đã tải (ngoại tuyến)'));
    await tester.pumpAndSettle();
    expect(find.text('Ca 9'), findsOneWidget);
    expect(find.text('Ca 10'), findsNothing);
  });
  testWidgets('FR_049_homeShowsOnlyPermittedScreens', (tester) async {
    final backend = FakeBackend();
    final cache = CareChecklistCache(NativeDatabase.memory());
    addTearDown(cache.close);
    backend.on('GET', '/me/permissions', 200, [
      {'function': 'DAY_CARE_ATTENDANCE', 'operation': 'VIEW'},
    ]);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: CareTasksHomeScreen(api: backend.client(), accountId: 7, cache: cache),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Điểm danh bán trú'), findsOneWidget);
    expect(find.text('Lập công việc của ca'), findsNothing);
    expect(find.text('Checklist ca'), findsNothing);
  });
}
