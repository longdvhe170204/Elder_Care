import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/medication/supplied_intake_screen.dart';

import '../../support/fake_backend.dart';

// 006 T065 (FR-054, BR-M07-24; Q-410): app nhân viên ghi tiếp nhận thuốc cung ứng, cùng luật với web.

const _line = {
  'id': 42000001,
  'procurementRequestId': 42,
  'residentId': 5,
  'prescriptionId': 91,
  'medicationName': 'Amlodipin',
  'activeIngredient': 'Amlodipin',
  'strength': '5 mg',
  'dosageForm': 'Viên nén',
  'quantity': 30,
  'unit': 'viên',
};

FakeBackend backend({List<Object> lines = const [_line]}) => FakeBackend()
  ..on('GET', '/procurement-medication-lines', 200, lines)
  ..on('GET', '/residents', 200, {
    'items': [
      {
        'id': 5,
        'profileCode': 'NCT-005',
        'personalInfo': {'fullName': 'Nguyễn Thị Lan'},
      },
    ],
  })
  ..on('POST', '/supplied-medication-intakes', 201, {});

Future<void> pump(WidgetTester tester, FakeBackend b) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: SuppliedIntakeScreen(api: b.client())),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> fill(WidgetTester tester, String label, String value) async {
  final field = find.widgetWithText(TextField, label);
  await tester.ensureVisible(field);
  await tester.enterText(field, value);
}

void main() {
  testWidgets('FR_054_dat_guiDongVaIdempotencyKey', (tester) async {
    final b = backend();
    await pump(tester, b);
    expect(find.text('Amlodipin 5 mg, Viên nén'), findsOneWidget);
    expect(find.textContaining('Nguyễn Thị Lan'), findsOneWidget);

    await tester.tap(find.text('Amlodipin 5 mg, Viên nén'));
    await tester.pumpAndSettle();
    await fill(tester, 'Hạn dùng (yyyy-mm-dd)', '2027-11-01');
    await fill(tester, 'Tình trạng bao bì', 'Nguyên vỉ');
    await fill(tester, 'Người giao', 'Nhân viên mua A');
    await tester.ensureVisible(find.text('Ghi tiếp nhận'));
    await tester.tap(find.text('Ghi tiếp nhận'));
    await tester.pumpAndSettle();

    final post = b.calls('POST', '/supplied-medication-intakes').single;
    expect(jsonDecode(post.body), {
      'requestLineId': 42000001,
      'concentration': '5 mg',
      'dosageForm': 'Viên nén',
      'receivedQuantity': 30,
      'unit': 'viên',
      'expiryDate': '2027-11-01',
      'condition': 'Nguyên vỉ',
      'result': 'PASSED',
      'delivererName': 'Nhân viên mua A',
    });
    expect(post.headers['Idempotency-Key'], isNotEmpty);
  });

  testWidgets('BR_M07_24_khongDat_batBuocSaiLechVaLyDo', (tester) async {
    final b = backend();
    await pump(tester, b);
    await tester.tap(find.text('Amlodipin 5 mg, Viên nén'));
    await tester.pumpAndSettle();
    await fill(tester, 'Hạn dùng (yyyy-mm-dd)', '2027-11-01');
    await fill(tester, 'Tình trạng bao bì', 'Rách hộp');
    await fill(tester, 'Người giao', 'Nhân viên mua A');
    await tester.ensureVisible(find.text('Không đạt'));
    await tester.tap(find.text('Không đạt'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Ghi tiếp nhận'));
    await tester.tap(find.text('Ghi tiếp nhận'));
    await tester.pumpAndSettle();
    expect(find.text('Nhập sai lệch'), findsOneWidget);
    expect(find.text('Nhập lý do'), findsOneWidget);
    expect(b.calls('POST', '/supplied-medication-intakes'), isEmpty);

    await fill(tester, 'Sai lệch', 'Thiếu 10 viên');
    await fill(tester, 'Lý do', 'Nhà cung cấp giao thiếu');
    await tester.ensureVisible(find.text('Ghi tiếp nhận'));
    await tester.tap(find.text('Ghi tiếp nhận'));
    await tester.pumpAndSettle();
    final body = jsonDecode(b.calls('POST', '/supplied-medication-intakes').single.body) as Map<String, dynamic>;
    expect(body['result'], 'NOT_PASSED');
    expect(body['discrepancy'], 'Thiếu 10 viên');
    expect(body['reason'], 'Nhà cung cấp giao thiếu');
  });

  testWidgets('Q410_khongCoDong_baoTrong', (tester) async {
    await pump(tester, backend(lines: const []));
    expect(find.text('Không có thuốc mua về đang chờ tiếp nhận.'), findsOneWidget);
  });
}
