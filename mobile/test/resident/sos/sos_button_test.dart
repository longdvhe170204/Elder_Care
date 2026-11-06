import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/resident/sos/sos_button.dart';

import '../../support/fake_backend.dart';

// 012 T071, T074 (tên logic nut_sos_test; FR-085, quickstart K9 → K11; 007 FR-047d, FR-047f, FR-047h, FR-047j; DESIGN.md
// 15.6): chạm "Gọi người giúp" thì đếm ngược 3 giây có nút Hủy; Hủy không gửi; chạm lần nữa gửi ngay; hết giờ thì gửi
// bằng token thiết bị. Sau khi gửi: "Đã gửi…", "[họ tên] đang đến.", "[họ tên] đã tới." (hỏi lại mỗi 5 giây), sáu nút nơi
// đang ở, gửi vị trí khi backend yêu cầu và đã cho quyền. Không gửi được thì hiện số của viện đã lưu và 115, không xếp
// hàng gửi sau.

class FakeLocation implements SosLocationSource {
  FakeLocation({this.granted = true});

  bool granted;
  int asks = 0;

  @override
  Future<bool> requestPermission() async {
    asks++;
    return granted;
  }

  @override
  Future<SosPosition?> current() async =>
      SosPosition(latitude: 10.78, longitude: 106.7, accuracyMeters: 12, measuredAt: DateTime.utc(2026, 11, 6, 2));
}

Map<String, dynamic> status(String value, {String? receiver, bool sendLocation = false}) => {
  'pressId': 7,
  'alertId': 70,
  'status': value,
  'receiverName': receiver,
  'sendLocation': sendLocation,
  'locationIntervalSeconds': 10,
  'contacts': {'facilityPhone': '02838000001', 'emergencyNumber': '115'},
};

late FakeBackend backend;
late MemoryStore store;
late FakeLocation location;

Future<void> pumpButton(WidgetTester tester, {String? deviceToken = 'device-1'}) async {
  if (deviceToken != null) await store.write(SessionController.deviceTokenKey, deviceToken);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.resident(Brightness.light),
      home: Scaffold(
        body: const SizedBox.shrink(),
        bottomNavigationBar: SosButton(api: backend.client(), store: store, location: location),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> press(WidgetTester tester) async {
  await tester.tap(find.text('Gọi người giúp'));
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 400));
}

Future<void> backHome(WidgetTester tester) async {
  await tester.scrollUntilVisible(find.text('Về màn Hôm nay'), 200, scrollable: find.byType(Scrollable).first);
  await tester.tap(find.text('Về màn Hôm nay'));
}

void main() {
  setUp(() {
    backend = FakeBackend()
      ..on('POST', '/sos', 201, status('SENT'))
      ..on('GET', '/sos/current', 200, status('SENT'));
    store = MemoryStore();
    location = FakeLocation(granted: false);
    resetSosLocationAsked();
  });

  testWidgets('FR_085_cancelDuringCountdownSendsNothing', (tester) async {
    await pumpButton(tester);
    await press(tester);
    expect(find.textContaining('Đang gọi người giúp sau'), findsOneWidget);

    await tester.tap(find.text('Hủy'));
    await tester.pump();
    expect(find.text('Đã hủy, chưa gửi.'), findsOneWidget);
    await tester.pump(const Duration(seconds: 4));
    await tester.pumpAndSettle();
    expect(backend.calls('POST', '/sos'), isEmpty);
    expect(find.text('Gọi người giúp'), findsOneWidget);
  });

  testWidgets('FR_085_secondTapSendsNow_statusUpdates', (tester) async {
    await pumpButton(tester);
    await press(tester);
    await tester.tap(find.text('Gọi ngay'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));

    final post = backend.calls('POST', '/sos').single;
    expect(post.headers['X-Device-Token'], 'device-1');
    expect(post.headers['Idempotency-Key'], matches(RegExp(r'^[0-9a-f-]{36}$')));
    expect(find.text('Đã gửi. Nhân viên đang được báo.'), findsOneWidget);
    expect(await store.read(sosFacilityPhoneKey), '02838000001');

    backend.on('GET', '/sos/current', 200, status('RECEIVED', receiver: 'Phạm Thu Hà'));
    await tester.pump(const Duration(seconds: 5));
    await tester.pump();
    expect(find.text('Phạm Thu Hà đang đến.'), findsOneWidget);

    backend.on('GET', '/sos/current', 200, status('ARRIVED', receiver: 'Phạm Thu Hà'));
    await tester.pump(const Duration(seconds: 5));
    await tester.pump();
    expect(find.text('Phạm Thu Hà đã tới.'), findsOneWidget);
    expect(find.text('Hủy'), findsNothing);

    await backHome(tester);
    await tester.pumpAndSettle();
  });

  testWidgets('FR_085_countdownElapsesThenSends_placeOnce', (tester) async {
    backend.on('POST', '/sos/7/place', 204);
    await pumpButton(tester);
    await press(tester);
    expect(backend.calls('POST', '/sos'), isEmpty);
    await tester.pump(const Duration(seconds: 3));
    await tester.pump(const Duration(milliseconds: 400));
    expect(backend.calls('POST', '/sos'), hasLength(1));

    for (final place in ['Ở phòng', 'Nhà vệ sinh', 'Hành lang', 'Phòng sinh hoạt chung', 'Ngoài sân', 'Chỗ khác']) {
      expect(find.text(place), findsOneWidget, reason: place);
    }
    await tester.tap(find.text('Hành lang'));
    await tester.pump();
    expect(jsonDecode(backend.calls('POST', '/sos/7/place').single.body), {'place': 'CORRIDOR'});
    expect(find.text('Ở phòng'), findsNothing);
    await backHome(tester);
    await tester.pumpAndSettle();
  });

  testWidgets('K9_offlineShowsSavedNumberAnd115_noQueue', (tester) async {
    await store.write(sosFacilityPhoneKey, '02838000009');
    backend.down = true;
    await pumpButton(tester);
    await press(tester);
    await tester.tap(find.text('Gọi ngay'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));

    expect(find.text('Chưa gửi được.'), findsOneWidget);
    expect(find.text('Máy chưa kết nối mạng.'), findsOneWidget);
    expect(find.text('Gọi viện: 02838000009'), findsOneWidget);
    expect(find.text('Gọi cấp cứu 115'), findsOneWidget);

    backend.down = false;
    await tester.pump(const Duration(seconds: 10));
    expect(backend.calls('POST', '/sos').length, 1);
  });

  testWidgets('FR_085_refusedShowsReasonAndContacts', (tester) async {
    backend.on('POST', '/sos', 409, {
      'errorCode': 'NOT_AT_FACILITY',
      'detail': 'Đang tạm vắng',
      'contacts': {'facilityPhone': '02838000002', 'emergencyNumber': '115'},
    });
    await pumpButton(tester);
    await press(tester);
    await tester.tap(find.text('Gọi ngay'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));

    expect(find.text('Đang ghi là tạm vắng hoặc ở bệnh viện.'), findsOneWidget);
    expect(find.text('Gọi viện: 02838000002'), findsOneWidget);
    expect(await store.read(sosFacilityPhoneKey), '02838000002');
  });

  testWidgets('FR_085_unboundDeviceDoesNotCall', (tester) async {
    await pumpButton(tester, deviceToken: null);
    await press(tester);
    await tester.tap(find.text('Gọi ngay'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));

    expect(find.text('Máy này chưa được gắn với tài khoản.'), findsOneWidget);
    expect(backend.calls('POST', '/sos'), isEmpty);
  });

  testWidgets('K10_sendsLocationWhileRequested', (tester) async {
    location.granted = true;
    backend
      ..on('POST', '/sos', 201, status('SENT', sendLocation: true))
      ..on('GET', '/sos/current', 200, status('SENT', sendLocation: true))
      ..on('POST', '/sos/7/location', 204);
    await pumpButton(tester);
    await press(tester);
    await tester.tap(find.text('Gọi ngay'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    expect(backend.calls('POST', '/sos/7/location'), hasLength(1));
    expect(jsonDecode(backend.calls('POST', '/sos/7/location').single.body), {
      'latitude': 10.78,
      'longitude': 106.7,
      'accuracyMeters': 12,
      'measuredAt': '2026-11-06T02:00:00.000Z',
    });

    await tester.pump(const Duration(seconds: 10));
    await tester.pump();
    expect(backend.calls('POST', '/sos/7/location'), hasLength(2));

    backend.on('POST', '/sos/7/location', 409, {'errorCode': 'STOP_SENDING_LOCATION'});
    await tester.pump(const Duration(seconds: 10));
    await tester.pump();
    final sent = backend.calls('POST', '/sos/7/location').length;
    await tester.pump(const Duration(seconds: 20));
    await tester.pump();
    expect(backend.calls('POST', '/sos/7/location').length, sent);
    await backHome(tester);
    await tester.pumpAndSettle();
  });

  testWidgets('K11_permissionDeniedStillSends_notAskedAgain', (tester) async {
    backend
      ..on('POST', '/sos', 201, status('SENT', sendLocation: true))
      ..on('GET', '/sos/current', 200, status('SENT', sendLocation: true));
    await pumpButton(tester);
    for (var i = 0; i < 2; i++) {
      await press(tester);
      await tester.tap(find.text('Gọi ngay'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 400));
      expect(find.text('Đã gửi. Nhân viên đang được báo.'), findsOneWidget);
      await backHome(tester);
      await tester.pumpAndSettle();
    }
    expect(backend.calls('POST', '/sos'), hasLength(2));
    expect(backend.calls('POST', '/sos/7/location'), isEmpty);
    expect(location.asks, 1);
  });

  testWidgets('FR_085_firstUseIntroOnce', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.resident(Brightness.light),
        home: SosIntroGate(store: store, child: const Text('Hôm nay')),
      ),
    );
    await tester.pumpAndSettle();
    expect(
      find.text('Nút Gọi người giúp báo nhân viên đang trực. Nút không thay chuông gọi ở đầu giường.'),
      findsOneWidget,
    );
    await tester.tap(find.text('Đã hiểu'));
    await tester.pumpAndSettle();
    expect(find.text('Hôm nay'), findsOneWidget);
    expect(await store.read(sosIntroSeenKey), isNotNull);
  });
}
