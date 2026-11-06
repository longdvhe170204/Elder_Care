import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/belongings/family_belongings_screen.dart';

import '../../support/fake_backend.dart';

// 013 T028 (tên logic nguoi-than/do-gui; US6; FR-030, BR-M12-09): app người thân xem đồ gửi và lịch sử bàn giao, cùng
// chữ với cổng web; không vị trí lưu giữ, không tên nhân viên; không có quyền thì nói rõ.

Future<void> pump(WidgetTester tester, FakeBackend b) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: FamilyBelongingsScreen(api: b.client(), residentId: 7)),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_030_doGuiTrangThaiVaLichSu', (tester) async {
    final b = FakeBackend()
      ..on('GET', '/family-portal/residents/7/belongings', 200, [
        {
          'id': 90,
          'typeId': 2,
          'description': 'Điện thoại Nokia',
          'quantity': 1,
          'status': 'IN_USE',
          'holder': 'RESIDENT',
          'history': [
            {'id': 300, 'kind': 'INTAKE', 'targetStatus': 'HELD', 'occurredAt': '2026-11-06T02:00:00Z', 'quantity': 1},
            {'id': 301, 'kind': 'GIVE_FOR_USE', 'targetStatus': 'IN_USE', 'occurredAt': '2026-11-06T03:00:00Z'},
          ],
        },
      ]);
    await pump(tester, b);

    expect(find.text('Điện thoại Nokia'), findsOneWidget);
    expect(find.text('Đang được người cao tuổi sử dụng'), findsOneWidget);
    expect(find.textContaining('Tiếp nhận'), findsOneWidget);
    expect(find.textContaining('Giao sử dụng'), findsOneWidget);
    expect(find.textContaining('Vị trí'), findsNothing);
  });

  testWidgets('BR_M12_09_403_baoKhongCoQuyen', (tester) async {
    final b = FakeBackend()
      ..on('GET', '/family-portal/residents/7/belongings', 403, {'status': 403, 'errorCode': 'FORBIDDEN'});
    await pump(tester, b);
    expect(find.text('Chỉ người đại diện và người được phép đón đang hiệu lực xem được đồ gửi.'), findsOneWidget);
  });
}
