import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/alerts/alerts_screen.dart';

import '../../support/fake_backend.dart';

// 007 T031 (US3; FR-027, FR-029, FR-033, FR-036): danh sách cảnh báo đang mở, chi tiết theo allowedCommands, tiếp nhận,
// đóng cần kết quả, mất kết nối không gửi lệnh.

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  tester.view.physicalSize = const Size(800, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: AlertsScreen(api: backend.client())),
    ),
  );
  await tester.pumpAndSettle();
}

const listPath = '/alerts';
Map<String, dynamic> alert(String status, List<String> commands) => {
  'id': 5,
  'type': 'Sụt cân',
  'level': 'MEDIUM',
  'status': status,
  'sources': [
    {'sourceType': 'WEIGHT', 'level': 'MEDIUM'},
  ],
  'allowedCommands': commands,
};

void main() {
  late FakeBackend backend;
  setUp(() => backend = FakeBackend());

  testWidgets('FR_027_danhSach_hienLoaiMucTrangThai', (tester) async {
    backend.on('GET', listPath, 200, [alert('NEW', [])]);
    await pumpScreen(tester, backend);
    expect(find.text('Sụt cân'), findsOneWidget);
    expect(find.textContaining('Trung bình · Mới'), findsOneWidget);
  });

  testWidgets('FR_029_tiepNhan_guiKemIdempotencyKey', (tester) async {
    backend.on('GET', listPath, 200, [alert('NEW', [])]);
    backend.on('GET', '/alerts/5', 200, alert('NEW', ['receive']));
    backend.on('POST', '/alerts/5/receive', 200);
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Sụt cân'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tiếp nhận'));
    await tester.pumpAndSettle();

    expect(backend.calls('POST', '/alerts/5/receive').single.headers['Idempotency-Key'], isNotEmpty);
    expect(find.text('Tiếp nhận: đã thực hiện.'), findsOneWidget);
  });

  testWidgets('FR_029_dong_thieuKetQua_khongGui', (tester) async {
    backend.on('GET', listPath, 200, [alert('IN_PROGRESS', [])]);
    backend.on('GET', '/alerts/5', 200, alert('IN_PROGRESS', ['close']));
    backend.on('POST', '/alerts/5/close', 200);
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Sụt cân'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đóng cảnh báo'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xác nhận đóng'));
    await tester.pumpAndSettle();
    expect(find.text('Nhập kết quả xử lý.'), findsOneWidget);
    expect(backend.calls('POST', '/alerts/5/close'), isEmpty);

    await tester.enterText(find.byType(TextField), 'Đã kiểm tra');
    await tester.tap(find.text('Xác nhận đóng'));
    await tester.pumpAndSettle();
    expect(backend.calls('POST', '/alerts/5/close'), hasLength(1));
  });

  testWidgets('FR_050_matKetNoi_lenhKhongDuocGui', (tester) async {
    backend.on('GET', listPath, 200, [alert('NEW', [])]);
    backend.on('GET', '/alerts/5', 200, alert('NEW', ['activate-emergency']));
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Sụt cân'));
    await tester.pumpAndSettle();
    backend.down = true;
    await tester.tap(find.text('Kích hoạt khẩn cấp'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Mất kết nối'), findsOneWidget);
  });
}
