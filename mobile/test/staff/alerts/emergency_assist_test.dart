import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/alerts/emergency_assist_screen.dart';

import '../../support/fake_backend.dart';

// 007 T102 (US13; FR-047i, FR-047j, FR-047k): đếm ngược 3 giây có Hủy, chạm lần hai gửi ngay; trạng thái người tiếp
// nhận; chi tiết một lần; ngoài ca hiện số của viện; Gọi 115 ghi lượt gọi rồi mở trình quay số.

final dialed = <String>[];

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  tester.view.physicalSize = const Size(800, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: EmergencyAssistScreen(
          api: backend.client(),
          dial: (phone) async {
            dialed.add(phone);
            return true;
          },
          pollInterval: const Duration(seconds: 5),
        ),
      ),
    ),
  );
  await tester.pump();
}

const sent = {'pressId': 1, 'alertId': 20, 'status': 'SENT'};

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    dialed.clear();
  });

  testWidgets('FR_047i_demNguoc3Giay_huyThiKhongGui', (tester) async {
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Gọi hỗ trợ khẩn cấp'));
    await tester.pump();
    expect(find.text('Gửi sau 3 giây, chạm để gửi ngay'), findsOneWidget);

    await tester.tap(find.text('Hủy'));
    await tester.pump(const Duration(seconds: 4));

    expect(backend.calls('POST', '/emergency-assist'), isEmpty);
    expect(find.text('Gọi hỗ trợ khẩn cấp'), findsOneWidget);
  });

  testWidgets('FR_047i_hetDemNguoc_guiKemIdempotencyKey_hienTrangThai', (tester) async {
    backend.on('POST', '/emergency-assist', 201, sent);
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Gọi hỗ trợ khẩn cấp'));
    await tester.pump(const Duration(seconds: 1));
    await tester.pump(const Duration(seconds: 1));
    await tester.pump(const Duration(seconds: 1));
    await tester.pump();

    final call = backend.calls('POST', '/emergency-assist').single;
    expect(call.headers['Idempotency-Key'], isNotEmpty);
    expect(find.text('Đã gửi, đang chờ người tiếp nhận'), findsOneWidget);

    backend.on('GET', '/emergency-assist/current', 200, {...sent, 'status': 'ARRIVED', 'receiverName': 'Trần Thu Hà'});
    await tester.pump(const Duration(seconds: 5));
    await tester.pump();
    expect(find.text('Trần Thu Hà đã tới'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('FR_047i_chamLanHai_guiNgay_chonChiTietMotLan', (tester) async {
    backend.on('POST', '/emergency-assist', 201, sent);
    backend.on('POST', '/emergency-assist/20/details', 204);
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Gọi hỗ trợ khẩn cấp'));
    await tester.pump();
    await tester.tap(find.text('Gửi sau 3 giây, chạm để gửi ngay'));
    await tester.pump();
    await tester.pump();
    expect(backend.calls('POST', '/emergency-assist'), hasLength(1));

    await tester.tap(find.text('Ngã'));
    await tester.pump();
    await tester.tap(find.text('Kích động, bạo lực'));
    await tester.pump();

    final details = backend.calls('POST', '/emergency-assist/20/details');
    expect(details, hasLength(1));
    expect(jsonDecode(details.single.body), {'eventType': 'FALL'});
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('BR_M05_29_ngoaiCa_hienLyDoVaSoCuaVien', (tester) async {
    backend.on('POST', '/emergency-assist', 409, {
      'errorCode': 'NOT_ON_SHIFT',
      'detail': 'Bạn không trong ca, hãy gọi số liên hệ của viện',
      'contacts': {'facilityPhone': '0281234567', 'emergencyNumber': '115'},
    });
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Gọi hỗ trợ khẩn cấp'));
    await tester.pump();
    await tester.tap(find.text('Gửi sau 3 giây, chạm để gửi ngay'));
    await tester.pump();
    await tester.pump();

    expect(find.text('Bạn không trong ca, hãy gọi số liên hệ của viện'), findsOneWidget);
    await tester.tap(find.text('Gọi số của viện 0281234567'));
    await tester.pump();
    expect(dialed, ['0281234567']);
  });

  testWidgets('BR_M05_31_goi115_ghiLuotGoiRoiQuaySo_chonKetQua', (tester) async {
    backend.on('POST', '/emergency-assist', 201, sent);
    backend.on('POST', '/alerts/20/call-115', 201, {'id': 7, 'pressedAt': '2026-11-06T03:00:00Z'});
    backend.on('POST', '/call-115-attempts/7/result', 200);
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Gọi hỗ trợ khẩn cấp'));
    await tester.pump();
    await tester.tap(find.text('Gửi sau 3 giây, chạm để gửi ngay'));
    await tester.pump();
    await tester.pump();

    await tester.tap(find.text('Gọi 115'));
    await tester.pump();
    await tester.pump();
    expect(backend.calls('POST', '/alerts/20/call-115'), hasLength(1));
    expect(dialed, ['115']);

    await tester.tap(find.text('Đã gọi được'));
    await tester.pump();
    await tester.pump();
    expect(jsonDecode(backend.calls('POST', '/call-115-attempts/7/result').single.body), {'result': 'CONNECTED'});
    expect(find.text('Đã ghi: đã gọi được cấp cứu 115.'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('FR_047k_matMang_van_quaySo115', (tester) async {
    backend.down = true;
    await pumpScreen(tester, backend);
    await tester.tap(find.text('Gọi 115'));
    await tester.pump();

    expect(dialed, ['115']);
  });
}
