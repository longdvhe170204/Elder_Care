import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/admissions/contract_screen.dart';

import '../../support/fake_backend.dart';

// 004 T038 (US3; FR-028 → FR-031): người đại diện xem hợp đồng, phụ lục và trạng thái đặt cọc trên app người thân; không
// có thao tác sửa; lỗi tải báo rõ kèm nút Tải lại.

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: ContractScreen(api: backend.client())),
    ),
  );
  await tester.pumpAndSettle();
}

Map<String, dynamic> contract({int id = 31, String status = 'ACTIVE'}) => {
  'id': id,
  'contractNumber': 'HD-031',
  'residenceType': 'LONG_TERM_RESIDENTIAL',
  'startDate': '2026-11-10',
  'status': status,
};

void main() {
  testWidgets('FR_028_hienHopDong_trangThai_vaKhongCoNutSua', (tester) async {
    final backend = FakeBackend()..on('GET', '/contracts', 200, [contract()]);
    await pumpScreen(tester, backend);

    expect(find.text('Hợp đồng HD-031'), findsOneWidget);
    expect(find.text('Hiệu lực'), findsOneWidget);
    expect(find.text('Nội trú dài hạn, từ 10/11/2026'), findsOneWidget);
    expect(find.text('Sửa'), findsNothing);
  });

  testWidgets('FR_030_xemPhuLucVaDatCoc_chiDoc', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/contracts', 200, [contract()])
      ..on('GET', '/contracts/31/deposit', 200, {'status': 'NOT_SATISFIED', 'requiredAmount': 10000000})
      ..on('GET', '/contracts/31/addenda', 200, [
        {'id': 1, 'effectiveDate': '2026-12-01', 'status': 'PENDING_EFFECTIVE'},
      ]);
    await pumpScreen(tester, backend);

    await tester.tap(find.text('Xem phụ lục, đặt cọc'));
    await tester.pumpAndSettle();

    expect(find.text('Chưa đặt cọc'), findsOneWidget);
    expect(find.text('Khoản cần đặt cọc: 10.000.000 đ'), findsOneWidget);
    expect(find.text('Hiệu lực 01/12/2026, Chờ hiệu lực'), findsOneWidget);
    expect(find.textContaining('Thu cọc'), findsNothing);
  });

  testWidgets('FR_028_chuaCoHopDong_bao', (tester) async {
    final backend = FakeBackend()..on('GET', '/contracts', 200, <dynamic>[]);
    await pumpScreen(tester, backend);
    expect(find.text('Chưa có hợp đồng nào.'), findsOneWidget);
  });

  testWidgets('FR_028_matMang_baoVaTaiLai', (tester) async {
    final backend = FakeBackend()..down = true;
    await pumpScreen(tester, backend);
    expect(find.textContaining('Không kết nối được máy chủ'), findsOneWidget);

    backend
      ..down = false
      ..on('GET', '/contracts', 200, [contract()]);
    await tester.tap(find.text('Tải lại'));
    await tester.pumpAndSettle();
    expect(find.text('Hợp đồng HD-031'), findsOneWidget);
  });
}
