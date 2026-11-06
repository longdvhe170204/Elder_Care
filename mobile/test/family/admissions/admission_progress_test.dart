import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/admissions/admission_progress_screen.dart';

import '../../support/fake_backend.dart';

// 004 T019 (US1; FR-003, FR-006): người đại diện xem tiến trình tiếp nhận của người mình đại diện; chỉ xem, chỉ trực tuyến.

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: AdmissionProgressScreen(api: backend.client(), applicationId: 7)),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_003_hienCacBuocVaTrangThaiTungBuoc', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/admission-applications/7/progress', 200, {
        'steps': [
          {'name': 'APPLICATION', 'done': true},
          {'name': 'ASSESSMENT', 'done': false},
        ],
      });
    await pumpScreen(tester, backend);

    expect(find.text('Đăng ký'), findsOneWidget);
    expect(find.text('Đánh giá'), findsOneWidget);
    expect(find.text('Đã xong'), findsOneWidget);
    expect(find.text('Chưa xong'), findsOneWidget);
  });

  testWidgets('FR_006_403_baoLoiVaTaiLai', (tester) async {
    final backend = FakeBackend()
      ..on('GET', '/admission-applications/7/progress', 403, {
        'errorCode': 'PERMISSION_DENIED',
        'detail': 'Bạn không có quyền xem tiến trình này',
      });
    await pumpScreen(tester, backend);

    expect(find.text('Bạn không có quyền xem tiến trình này'), findsOneWidget);
    expect(find.text('Tải lại'), findsOneWidget);
  });
}
