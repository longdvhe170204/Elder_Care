import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/incidents/incidents_screen.dart';

import '../../support/fake_backend.dart';

// 007 T037, T042 (US4, US5; FR-045, FR-047, FR-048): danh sách sự cố, lệnh theo allowedCommands, thẻ khẩn cấp cần
// trực tuyến.

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  tester.view.physicalSize = const Size(800, 2400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: IncidentsScreen(api: backend.client())),
    ),
  );
  await tester.pumpAndSettle();
}

const listPath = '/incidents';
Map<String, dynamic> incident(List<String> commands) => {
  'id': 9,
  'content': {'description': 'Ngã trong nhà vệ sinh'},
  'level': 'CRITICAL',
  'status': 'NEW',
  'wasEmergency': true,
  'handlers': ['Trần Thu Hà'],
  'allowedCommands': commands,
};

void main() {
  late FakeBackend backend;
  setUp(() => backend = FakeBackend());

  testWidgets('FR_045_danhSach_hienNoiDungMucTrangThai', (tester) async {
    backend.on('GET', listPath, 200, [incident([])]);
    await pumpScreen(tester, backend);
    expect(find.text('Ngã trong nhà vệ sinh'), findsOneWidget);
    expect(find.textContaining('Khẩn cấp · Mới'), findsOneWidget);
  });

  testWidgets('FR_045_tiepNhan_guiLenh', (tester) async {
    backend.on('GET', listPath, 200, [incident([])]);
    backend.on('GET', '/incidents/9', 200, incident(['receive']));
    backend.on('POST', '/incidents/9/receive', 200);
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Ngã trong nhà vệ sinh'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tiếp nhận'));
    await tester.pumpAndSettle();
    expect(backend.calls('POST', '/incidents/9/receive').single.headers['Idempotency-Key'], isNotEmpty);
  });

  testWidgets('FR_047_theKhanCap_hienNhanDangVaDiUng', (tester) async {
    backend.on('GET', listPath, 200, [incident([])]);
    backend.on('GET', '/incidents/9', 200, incident([]));
    backend.on('GET', '/incidents/9/emergency-card', 200, {
      'identification': {'fullName': 'Nguyễn Thị Hoa', 'roomBed': 'P101-G2'},
      'criticalFlag': false,
      'allergies': [
        {'name': 'Penicillin'},
      ],
      'primaryContact': {'fullName': 'Lê Văn An', 'phoneNumber': '0900000000'},
    });
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Ngã trong nhà vệ sinh'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xem thẻ thông tin khẩn cấp'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Nguyễn Thị Hoa'), findsOneWidget);
    expect(find.text('• Penicillin'), findsOneWidget);
    expect(find.textContaining('Lê Văn An'), findsOneWidget);
  });

  testWidgets('FR_047_theKhanCap_matKetNoi_baoLoi', (tester) async {
    backend.on('GET', listPath, 200, [incident([])]);
    backend.on('GET', '/incidents/9', 200, incident([]));
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Ngã trong nhà vệ sinh'));
    await tester.pumpAndSettle();
    backend.down = true;
    await tester.tap(find.text('Xem thẻ thông tin khẩn cấp'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Mất kết nối'), findsOneWidget);
  });
}
