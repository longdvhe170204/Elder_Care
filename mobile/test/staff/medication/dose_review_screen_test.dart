import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/medication/dose_review_screen.dart';
import 'package:mobile/staff/medication/medication_round_gateway.dart';

import '../../support/fake_backend.dart';

// 006 T028 (tên logic xem-lai-lieu; FR-029, SC-012): bản ghi thuốc kiểm soát đặc biệt "ghi sau mất kết nối" chờ Điều
// dưỡng khác xem lại; app chỉ gắn kết quả, quyền "khác người ghi" do backend kiểm.

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/dose-reviews', 200, [
      {
        'id': 31,
        'doseKey': '11-202611050730',
        'medicationName': 'Morphin',
        'administeredAt': '2026-11-05T00:35:00Z',
        'status': 'PENDING_REVIEW',
        'recordedBy': 'Trần Thu Hà',
      },
    ]);
  });

  Future<void> pump(WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: DoseReviewScreen(gateway: ApiMedicationRoundGateway(backend.client()), newKey: () => 'review-1'),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('FR_029_xemLai_chapNhan_ganKetQua', (tester) async {
    backend.on('POST', '/dose-reviews/31/result', 200);
    await pump(tester);

    expect(find.text('Morphin'), findsOneWidget);
    expect(find.text('Thời điểm dùng 05/11/2026 07:35'), findsOneWidget);
    expect(find.text('Người ghi: Trần Thu Hà'), findsOneWidget);
    expect(backend.calls('GET', '/dose-reviews').single.url.queryParameters, {'status': 'PENDING_REVIEW'});

    await tester.tap(find.text('Chấp nhận'));
    await tester.pumpAndSettle();

    final call = backend.calls('POST', '/dose-reviews/31/result').single;
    expect(jsonDecode(call.body), {'result': 'ACCEPTED'});
    expect(call.headers['Idempotency-Key'], 'review-1');
    expect(find.text('Đã chấp nhận bản ghi.'), findsOneWidget);
    expect(find.text('Morphin'), findsNothing);
  });

  testWidgets('FR_029_nguoiGhiTuXemLai_hienLoiCuaBackend', (tester) async {
    backend.on('POST', '/dose-reviews/31/result', 403, {
      'errorCode': 'TWO_PERSON_RULE',
      'detail': 'Người xem lại phải khác người ghi.',
    });
    await pump(tester);

    await tester.tap(find.text('Cần đính chính'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'Sai giờ dùng');
    await tester.tap(find.text('Ghi cần đính chính'));
    await tester.pumpAndSettle();

    expect(jsonDecode(backend.calls('POST', '/dose-reviews/31/result').single.body), {
      'result': 'REQUIRES_CORRECTION',
      'note': 'Sai giờ dùng',
    });
    expect(find.text('Người xem lại phải khác người ghi.'), findsOneWidget);
  });
}
