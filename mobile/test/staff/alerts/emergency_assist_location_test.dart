import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/resident/sos/sos_button.dart' show SosLocationSource, SosPosition;
import 'package:mobile/staff/alerts/emergency_assist_screen.dart';

import '../../support/fake_backend.dart';

// 007 T102 (FR-047h; BR-M05-28): sau khi gửi nút hỗ trợ, backend báo sendLocation thì app xin quyền một lần và gửi vị trí
// GPS theo chu kỳ; không có quyền thì không gửi; 409 thì dừng.

class _Location implements SosLocationSource {
  _Location({this.granted = true});

  final bool granted;
  int sent = 0;

  @override
  Future<bool> requestPermission() async => granted;

  @override
  Future<SosPosition?> current() async {
    sent++;
    return SosPosition(latitude: 10.77, longitude: 106.69, accuracyMeters: 12, measuredAt: DateTime.utc(2026, 11, 9, 3));
  }
}

const sent = {'pressId': 5, 'alertId': 20, 'status': 'SENT', 'sendLocation': true, 'locationIntervalSeconds': 30};

Future<void> press(WidgetTester tester, FakeBackend backend, SosLocationSource location) async {
  tester.view.physicalSize = const Size(800, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: EmergencyAssistScreen(api: backend.client(), location: location, dial: (_) async => true),
      ),
    ),
  );
  await tester.pump();
  await tester.tap(find.text('Gọi hỗ trợ khẩn cấp'));
  await tester.pump();
  await tester.tap(find.text('Gửi sau 3 giây, chạm để gửi ngay'));
  await tester.pump();
  await tester.pump();
  await tester.pump();
}

void main() {
  testWidgets('FR_047h_coQuyen_guiViTriNgayRoiTheoChuKy', (tester) async {
    final backend = FakeBackend()
      ..on('POST', '/emergency-assist', 201, sent)
      ..on('GET', '/emergency-assist/current', 200, sent)
      ..on('POST', '/emergency-assist/5/location', 204);
    final location = _Location();
    await press(tester, backend, location);

    final first = backend.calls('POST', '/emergency-assist/5/location');
    expect(first, hasLength(1));
    expect(jsonDecode(first.single.body), containsPair('latitude', 10.77));

    await tester.pump(const Duration(seconds: 30));
    expect(backend.calls('POST', '/emergency-assist/5/location'), hasLength(2));
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('FR_047h_tuChoiQuyen_khongGuiViTri', (tester) async {
    final backend = FakeBackend()
      ..on('POST', '/emergency-assist', 201, sent)
      ..on('GET', '/emergency-assist/current', 200, sent);
    final location = _Location(granted: false);
    await press(tester, backend, location);

    expect(backend.calls('POST', '/emergency-assist/5/location'), isEmpty);
    expect(location.sent, 0);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('BR_M05_28_409_dungGui', (tester) async {
    final backend = FakeBackend()
      ..on('POST', '/emergency-assist', 201, sent)
      ..on('GET', '/emergency-assist/current', 200, sent)
      ..on('POST', '/emergency-assist/5/location', 409, {'errorCode': 'STOP_SENDING_LOCATION', 'detail': 'Dừng'});
    await press(tester, backend, _Location());
    await tester.pump(const Duration(seconds: 90));

    expect(backend.calls('POST', '/emergency-assist/5/location'), hasLength(1));
    await tester.pumpWidget(const SizedBox());
  });
}
