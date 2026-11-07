import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/staff_root.dart';

import '../support/fake_backend.dart';

// Mục "Thêm" của StaffRoot theo quyền (DESIGN.md 15.4; team-plan mục 8, 2026-11-07): Phát thuốc (006 T028), Giường (003),
// Hoạt động ngoài viện (014 T018) chỉ hiện khi `/me/permissions` có khóa tương ứng; không tải được quyền thì không hiện.

const _titles = ['Phát thuốc', 'Giường', 'Hoạt động ngoài viện'];

void main() {
  Future<void> openMore(WidgetTester tester, FakeBackend backend) async {
    final s = session(backend, MemoryStore(), FakeBiometric());
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: StaffRoot(session: s, queue: memoryQueue(s)),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(NavigationDestination, 'Thêm'));
    await tester.pumpAndSettle();
  }

  testWidgets('DESIGN_15_4_mucTheoQuyen_khongCoQuyen_khongHien', (tester) async {
    await openMore(tester, FakeBackend()..on('GET', '/me/permissions', 200, <Object>[]));

    for (final title in _titles) {
      expect(find.text(title), findsNothing, reason: title);
    }
    expect(find.text('Chăm sóc hằng ngày'), findsOneWidget);
  });

  testWidgets('DESIGN_15_4_mucTheoQuyen_coQuyen_hienDuBaMuc', (tester) async {
    await openMore(
      tester,
      FakeBackend()..on('GET', '/me/permissions', 200, [
        {'function': 'MEDICATION_ADMINISTRATION', 'operation': 'PROFESSIONAL'},
        {'function': 'ROOM_BED_CONFIGURATION', 'operation': 'VIEW'},
        {'function': 'ACTIVITY_OUTING', 'operation': 'VIEW'},
      ]),
    );

    await tester.scrollUntilVisible(find.text('Hoạt động ngoài viện'), 200);
    for (final title in _titles) {
      expect(find.text(title), findsOneWidget, reason: title);
    }
  });
}
